package main.web.services.fitsense.catalog.domain.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * FAMILIA DE MOVIMIENTO EQUIVALENTE (O20, opcion A; ajuste final del 24/11/2026).
 * <p>
 * Tras dolor en un ejercicio, ninguna variante que reproduzca el mismo gesto debe volver a un plan. Dos ejercicios son
 * EQUIVALENTES solo si lo dice uno de estos dos niveles, y nada mas:
 * <ol>
 *   <li><b>Variante del mismo ejercicio</b> ({@link #clave}, {@link #NIVEL_VARIANTE}): su nombre en ingles
 *   ({@code name_en}), sin mayusculas, sin espacios sobrantes y sin el sufijo final {@code v. N}, es identico. Es el
 *   caso de O20: «dumbbell front raise» y «dumbbell front raise v. 2». No exige el mismo patron registrado.</li>
 *   <li><b>Mismo gesto con otro nombre, material o postura</b> ({@link #NIVEL_MISMO_GESTO}): los dos nombres estan en
 *   el mismo grupo de la LISTA EXPLICITA {@value #RECURSO}, revisada y versionada, cada grupo con su justificacion.
 *   «dumbbell seated front raise», «band front raise» o «cable front raise» reproducen el gesto de «dumbbell front
 *   raise» (grupo «front raise»).</li>
 * </ol>
 * Coincidir en patron de movimiento y musculo objetivo NO basta: «dumbbell lateral raise» comparte patron y musculo
 * con «dumbbell front raise» y es otro movimiento. Lo dudoso (cambios de angulo, nucleos genericos, posibles
 * sinonimos) no esta en la lista y por tanto NO es equivalente; queda en el inventario de pendientes
 * ({@code docs/FitSense/seguridad_o20/evidencia/equivalencias_pendientes.csv}). No es un criterio clinico.
 * <p>
 * <p>
 * <b>Precaucion por angulo</b> ({@link #NIVEL_PRECAUCION_ANGULO}, cierre final): el mismo gesto con otro angulo o apoyo
 * del cuerpo (incline, decline, lying, kneeling) NO es equivalente, pero tampoco se da por seguro tras dolor. La lista
 * {@value #RECURSO_PRECAUCIONES} (las relaciones pendientes por angulo del inventario) se aplica como precaucion: tras
 * dolor en «lever incline chest press», «lever chest press» no se prescribe ni se usa como sustituto mientras dure la
 * exclusion. Solo esas relaciones; los nucleos genericos y los posibles sinonimos siguen pendientes sin efecto.
 * <p>
 * Dominio puro: sin base de datos. Las listas se leen una vez del classpath.
 */
public final class FamiliaDeMovimientoEquivalente {

    public static final String NIVEL_VARIANTE = "VARIANTE_MISMO_EJERCICIO";
    public static final String NIVEL_MISMO_GESTO = "MISMO_GESTO";
    public static final String RECURSO = "catalog/equivalencias_mismo_gesto.csv";
    public static final String NIVEL_PRECAUCION_ANGULO = "PRECAUCION_OTRO_ANGULO";
    public static final String RECURSO_PRECAUCIONES = "catalog/precauciones_angulo.csv";

    /** «v. 2», «v.3», «V. 10» al final del nombre. */
    private static final Pattern SUFIJO_VARIANTE = Pattern.compile("\\s+v\\.\\s*\\d+\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ESPACIOS = Pattern.compile("\\s+");

    /** Un grupo confirmado de la lista explicita: sus miembros son claves de nivel 1. */
    public record GrupoMismoGesto(String id, Set<String> claves, String justificacion) {}

    /** Una fila de precaucion por angulo: subgrupos de claves, uno por angulo, del mismo gesto. */
    public record PrecaucionAngulo(String id, java.util.List<Set<String>> subgrupos, String justificacion) {}

    private static volatile Map<String, GrupoMismoGesto> porClave;
    private static volatile Map<String, java.util.List<PrecaucionAngulo>> precaucionesPorClave;

    private FamiliaDeMovimientoEquivalente() {}

    /** Nivel 1. La clave de variante de un nombre en ingles; {@code null} si el nombre esta vacio. */
    public static String clave(String nombreEn) {
        if (nombreEn == null) return null;
        var normalizado = ESPACIOS.matcher(nombreEn.strip().toLowerCase(Locale.ROOT)).replaceAll(" ");
        normalizado = SUFIJO_VARIANTE.matcher(normalizado).replaceAll("").strip();
        return normalizado.isEmpty() ? null : normalizado;
    }

    /** Nivel 1: el mismo ejercicio con nombre propio (variante {@code v. N} o nombre identico). */
    public static boolean mismaFamilia(String nombreA, String nombreB) {
        var a = clave(nombreA);
        return a != null && a.equals(clave(nombreB));
    }

    /** Nivel 2: el grupo confirmado al que pertenece un nombre, si lo hay. */
    public static Optional<GrupoMismoGesto> grupo(String nombreEn) {
        var c = clave(nombreEn);
        return c == null ? Optional.empty() : Optional.ofNullable(lista().get(c));
    }

    /** Nivel 2: los dos nombres estan en el mismo grupo confirmado (y no son el mismo ejercicio). */
    public static boolean mismoGesto(String nombreA, String nombreB) {
        var g = grupo(nombreA);
        return g.isPresent() && !mismaFamilia(nombreA, nombreB) && g.equals(grupo(nombreB));
    }

    /** Equivalentes por cualquiera de los dos niveles. */
    public static boolean equivalentes(String nombreA, String nombreB) {
        return mismaFamilia(nombreA, nombreB) || mismoGesto(nombreA, nombreB);
    }

    /**
     * La relacion de seguridad entre dos nombres: {@link #NIVEL_VARIANTE} o {@link #NIVEL_MISMO_GESTO} (equivalentes),
     * {@link #NIVEL_PRECAUCION_ANGULO} (el mismo gesto con otro angulo: no equivalente, precaucion tras dolor) o
     * {@code null} (sin relacion).
     */
    public static String nivel(String nombreA, String nombreB) {
        if (mismaFamilia(nombreA, nombreB)) return NIVEL_VARIANTE;
        if (mismoGesto(nombreA, nombreB)) return NIVEL_MISMO_GESTO;
        return precaucionPorAngulo(nombreA, nombreB).isPresent() ? NIVEL_PRECAUCION_ANGULO : null;
    }

    /** La fila de precaucion por angulo que relaciona dos nombres de subgrupos (angulos) distintos, si la hay. */
    public static Optional<PrecaucionAngulo> precaucionPorAngulo(String nombreA, String nombreB) {
        var a = clave(nombreA);
        var b = clave(nombreB);
        if (a == null || b == null || a.equals(b)) return Optional.empty();
        for (var fila : precauciones().getOrDefault(a, java.util.List.of()))
            for (var sub : fila.subgrupos())
                if (sub.contains(a)) {
                    if (sub.contains(b)) break; // mismo angulo: no es precaucion por angulo
                    if (fila.subgrupos().stream().anyMatch(o -> o != sub && o.contains(b))) return Optional.of(fila);
                }
        return Optional.empty();
    }

    /** Las claves relacionadas por precaucion de angulo con un nombre (los otros subgrupos de su fila). */
    public static Set<String> clavesPrecaucionAngulo(String nombreEn) {
        var c = clave(nombreEn);
        var r = new TreeSet<String>();
        if (c == null) return r;
        for (var fila : precauciones().getOrDefault(c, java.util.List.of()))
            for (var sub : fila.subgrupos())
                if (!sub.contains(c)) r.addAll(sub);
        return r;
    }

    /** Las filas de precaucion por angulo, para el inventario. */
    public static Collection<PrecaucionAngulo> filasPrecaucion() {
        var porId = new LinkedHashMap<String, PrecaucionAngulo>();
        precauciones().values().forEach(l -> l.forEach(f -> porId.putIfAbsent(f.id(), f)));
        return Collections.unmodifiableCollection(porId.values());
    }

    /** Los grupos confirmados, para el inventario. */
    public static Collection<GrupoMismoGesto> grupos() {
        var porId = new LinkedHashMap<String, GrupoMismoGesto>();
        lista().values().forEach(g -> porId.putIfAbsent(g.id(), g));
        return Collections.unmodifiableCollection(porId.values());
    }

    /**
     * Patron LIKE (en minusculas) que encuentra en base de datos las variantes {@code v. N} de una clave. Sin escapar:
     * un comodin del nombre solo amplia los candidatos; el filtro final es {@link #clave}.
     */
    public static String patronVariantes(String clave) {
        return clave + " v.%";
    }

    private static Map<String, java.util.List<PrecaucionAngulo>> precauciones() {
        var l = precaucionesPorClave;
        if (l == null) {
            synchronized (FamiliaDeMovimientoEquivalente.class) {
                if (precaucionesPorClave == null) {
                    var in = FamiliaDeMovimientoEquivalente.class.getClassLoader()
                            .getResourceAsStream(RECURSO_PRECAUCIONES);
                    if (in == null)
                        throw new IllegalStateException("Falta la lista de precauciones " + RECURSO_PRECAUCIONES);
                    try (var r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                        precaucionesPorClave = leerPrecauciones(r);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
                l = precaucionesPorClave;
            }
        }
        return l;
    }

    /**
     * Lee las precauciones: {@code fila;patron;musculo;subgrupos;justificacion}, con subgrupos
     * {@code angulo=clave|clave^angulo=clave}. Falla si una fila tiene menos de dos subgrupos, un miembro sin
     * normalizar, un miembro en dos subgrupos de la misma fila o le falta la justificacion.
     */
    public static Map<String, java.util.List<PrecaucionAngulo>> leerPrecauciones(Reader fuente) throws IOException {
        var resultado = new LinkedHashMap<String, java.util.List<PrecaucionAngulo>>();
        var ids = new TreeSet<String>();
        try (var br = new BufferedReader(fuente)) {
            String linea;
            int n = 0;
            while ((linea = br.readLine()) != null) {
                n++;
                if (linea.isBlank() || linea.startsWith("#") || linea.startsWith("fila;")) continue;
                var c = linea.split(";", -1);
                if (c.length != 5) throw new IllegalStateException("Linea %d: se esperaban 5 columnas".formatted(n));
                var id = c[0].strip();
                var just = c[4].strip();
                if (id.isEmpty() || just.isEmpty() || !ids.add(id))
                    throw new IllegalStateException("Linea %d: fila sin id, repetida o sin justificacion".formatted(n));
                var subgrupos = new java.util.ArrayList<Set<String>>();
                var vistos = new TreeSet<String>();
                for (var sub : c[3].split("\\^")) {
                    var partes = sub.split("=", 2);
                    if (partes.length != 2) throw new IllegalStateException("Linea %d: subgrupo sin angulo".formatted(n));
                    var claves = new TreeSet<String>();
                    for (var m : partes[1].split("\\|")) {
                        var k = clave(m);
                        if (k == null || !k.equals(m.strip()) || !vistos.add(k))
                            throw new IllegalStateException("Linea %d: miembro no valido «%s»".formatted(n, m));
                        claves.add(k);
                    }
                    subgrupos.add(Collections.unmodifiableSet(claves));
                }
                if (subgrupos.size() < 2) throw new IllegalStateException("Linea %d: menos de dos angulos".formatted(n));
                var fila = new PrecaucionAngulo(id, java.util.List.copyOf(subgrupos), just);
                for (var k : vistos) resultado.computeIfAbsent(k, x -> new java.util.ArrayList<>()).add(fila);
            }
        }
        resultado.replaceAll((k, v) -> java.util.List.copyOf(v));
        return Collections.unmodifiableMap(resultado);
    }

    private static Map<String, GrupoMismoGesto> lista() {
        var l = porClave;
        if (l == null) {
            synchronized (FamiliaDeMovimientoEquivalente.class) {
                if (porClave == null) {
                    var in = FamiliaDeMovimientoEquivalente.class.getClassLoader().getResourceAsStream(RECURSO);
                    if (in == null) throw new IllegalStateException("Falta la lista de equivalencias " + RECURSO);
                    try (var r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                        porClave = leer(r);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
                l = porClave;
            }
        }
        return l;
    }

    /**
     * Lee la lista: {@code grupo;patron;musculo;miembros;justificacion}, miembros separados por {@code |}; las lineas
     * con {@code #} y la cabecera se ignoran. Falla si un miembro no es una clave normalizada, si esta en dos grupos,
     * si un grupo tiene menos de dos miembros o si le falta la justificacion: una lista ambigua no se aplica a medias.
     */
    public static Map<String, GrupoMismoGesto> leer(Reader fuente) throws IOException {
        var resultado = new LinkedHashMap<String, GrupoMismoGesto>();
        var ids = new TreeSet<String>();
        try (var br = new BufferedReader(fuente)) {
            String linea;
            int n = 0;
            while ((linea = br.readLine()) != null) {
                n++;
                if (linea.isBlank() || linea.startsWith("#") || linea.startsWith("grupo;")) continue;
                var c = linea.split(";", -1);
                if (c.length != 5) throw new IllegalStateException("Linea %d: se esperaban 5 columnas".formatted(n));
                var id = c[0].strip();
                var just = c[4].strip();
                if (id.isEmpty() || just.isEmpty() || !ids.add(id))
                    throw new IllegalStateException("Linea %d: grupo sin id, repetido o sin justificacion".formatted(n));
                var claves = new TreeSet<String>();
                for (var m : c[3].split("\\|")) {
                    var k = clave(m);
                    if (k == null || !k.equals(m.strip()))
                        throw new IllegalStateException("Linea %d: miembro no normalizado «%s»".formatted(n, m));
                    claves.add(k);
                }
                if (claves.size() < 2) throw new IllegalStateException("Linea %d: grupo de un miembro".formatted(n));
                var g = new GrupoMismoGesto(id, Collections.unmodifiableSet(claves), just);
                for (var k : claves)
                    if (resultado.putIfAbsent(k, g) != null)
                        throw new IllegalStateException("Linea %d: «%s» esta en dos grupos".formatted(n, k));
            }
        }
        return Collections.unmodifiableMap(resultado);
    }
}
