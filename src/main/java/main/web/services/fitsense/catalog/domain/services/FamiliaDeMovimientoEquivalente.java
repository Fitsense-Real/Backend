package main.web.services.fitsense.catalog.domain.services;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * FAMILIA DE MOVIMIENTO EQUIVALENTE (O20, opcion A, orden de seguridad del 24/11/2026).
 * <p>
 * Tras dolor en un ejercicio, ninguna variante equivalente que pueda reproducir el mismo gesto debe volver a un plan.
 * Dos ejercicios son EQUIVALENTES si cumplen cualquiera de los dos niveles:
 * <ol>
 *   <li><b>Variante del mismo ejercicio</b> ({@link #clave}): su nombre en ingles ({@code name_en}), sin mayusculas,
 *   sin espacios sobrantes y sin el sufijo final {@code v. N}, es identico. Es el caso de O20: «dumbbell front raise»
 *   (0310) y «dumbbell front raise v. 2» (0309); tambien los repetidos con el mismo nombre y los duplicados historicos.
 *   No exige el mismo patron registrado: en «barbell upright row», v. 2 y v. 3 tienen patrones distintos y siguen
 *   siendo el mismo ejercicio con nombre propio.</li>
 *   <li><b>Mismo gesto con otro material o postura</b> ({@link #nucleo}): el mismo patron de movimiento, el mismo
 *   musculo objetivo Y el mismo NUCLEO del nombre, que es el nombre sin el material (dumbbell, barbell, cable, band,
 *   lever, smith, kettlebell...) ni la postura (seated, standing, incline, alternate, one arm...). «dumbbell seated
 *   front raise», «band front raise» o «cable front raise» reproducen el gesto de «dumbbell front raise»; «dumbbell
 *   lateral raise» o «dumbbell full can lateral raise» no (otro nucleo: es otro movimiento del mismo patron, y
 *   sustituir dentro del patron es justo lo que debe poder hacer SUST-SEG-1).</li>
 * </ol>
 * Es una regla conservadora y basada en datos del catalogo, no un criterio clinico: agrupa de mas antes que de menos.
 * Dominio puro: sin base de datos.
 */
public final class FamiliaDeMovimientoEquivalente {

    /** «v. 2», «v.3», «V. 10» al final del nombre. */
    private static final Pattern SUFIJO_VARIANTE = Pattern.compile("\\s+v\\.\\s*\\d+\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ESPACIOS = Pattern.compile("\\s+");

    /**
     * Material y postura que NO cambian el gesto (nivel 2). Los de varias palabras van primero: se quitan antes que
     * sus partes. Lista cerrada y revisable; cualquier cambio cambia que se excluye tras dolor.
     */
    static final List<String> MODIFICADORES = List.of(
            "ez barbell", "ez-bar", "ez bar", "smith machine", "leverage machine", "resistance band", "body weight",
            "single arm", "one arm", "two arm",
            "lever", "barbell", "dumbbells", "dumbbell", "kettlebell", "cable", "band", "weighted", "bodyweight",
            "machine", "smith",
            "seated", "standing", "kneeling", "lying", "incline", "decline", "alternate", "alternating", "single", "one");

    private static final List<Pattern> PATRONES_MODIFICADORES = MODIFICADORES.stream()
            .sorted(Comparator.comparingInt(String::length).reversed())
            .map(m -> Pattern.compile("(?<=\\s)" + Pattern.quote(m) + "(?=\\s)"))
            .toList();

    private FamiliaDeMovimientoEquivalente() {}

    /** Nivel 1. La clave de variante de un nombre en ingles; {@code null} si el nombre esta vacio. */
    public static String clave(String nombreEn) {
        if (nombreEn == null) return null;
        var normalizado = ESPACIOS.matcher(nombreEn.strip().toLowerCase(Locale.ROOT)).replaceAll(" ");
        normalizado = SUFIJO_VARIANTE.matcher(normalizado).replaceAll("").strip();
        return normalizado.isEmpty() ? null : normalizado;
    }

    /** Nivel 2. El nucleo del movimiento: la clave sin material ni postura; {@code null} si no queda nada. */
    public static String nucleo(String nombreEn) {
        var c = clave(nombreEn);
        if (c == null) return null;
        var n = " " + c.replace('(', ' ').replace(')', ' ') + " ";
        for (var p : PATRONES_MODIFICADORES) n = p.matcher(n).replaceAll(" ");
        n = ESPACIOS.matcher(n).replaceAll(" ").strip();
        return n.isEmpty() ? null : n;
    }

    /** Nivel 1: el mismo ejercicio con nombre propio (variante {@code v. N} o nombre identico). */
    public static boolean mismaFamilia(String nombreA, String nombreB) {
        var a = clave(nombreA);
        return a != null && a.equals(clave(nombreB));
    }

    /** Nivel 2: el mismo gesto (mismo nucleo, mismo patron, mismo musculo objetivo; ninguno nulo). */
    public static boolean mismoGesto(String nombreA, String patronA, String musculoA,
                                     String nombreB, String patronB, String musculoB) {
        if (patronA == null || musculoA == null || !Objects.equals(patronA, patronB) || !Objects.equals(musculoA, musculoB))
            return false;
        var a = nucleo(nombreA);
        return a != null && a.equals(nucleo(nombreB));
    }

    /** Equivalentes por cualquiera de los dos niveles. */
    public static boolean equivalentes(String nombreA, String patronA, String musculoA,
                                       String nombreB, String patronB, String musculoB) {
        return mismaFamilia(nombreA, nombreB) || mismoGesto(nombreA, patronA, musculoA, nombreB, patronB, musculoB);
    }

    /**
     * Patron LIKE (en minusculas) que encuentra en base de datos las variantes {@code v. N} de una clave. Sin escapar:
     * un comodin del nombre solo amplia los candidatos; el filtro final es {@link #clave}.
     */
    public static String patronVariantes(String clave) {
        return clave + " v.%";
    }
}
