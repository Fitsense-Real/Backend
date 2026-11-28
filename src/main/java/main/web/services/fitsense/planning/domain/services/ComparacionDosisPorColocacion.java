package main.web.services.fitsense.planning.domain.services;

import java.time.DayOfWeek;
import java.util.*;

/**
 * P-03 POR COLOCACION (cierre premerge, 15/11/2026). Compara cada colocacion de una semana regenerada con su colocacion
 * equivalente de la semana medida, para que P-03 nivel 2 no acepte una semana cuyo trabajo TOTAL no sube pero en la que
 * una colocacion reducida (fatiga, mantenimiento, adaptacion) vuelve a una dosis mayor sin autorizacion.
 * <p>
 * Solo se declara una equivalencia cuando la correspondencia es fiable; si no lo es, se aplica la regla conservadora:
 * <ul>
 *   <li><b>MANTENIDA</b> (correspondida): mismo ejercicio y mismo dia de la semana. Si se repite el mismo dia, se
 *       emparejan por orden de aparicion. Ninguna dimension (series, repeticiones, segundos, series x repeticiones,
 *       series x segundos) puede subir.</li>
 *   <li><b>SUSTITUCION</b>: mismo dia y MISMA POSICION que un ejercicio medido RETIRADO (exclusion activa por dolor o
 *       seguridad) que ya no aparece. Se compara con la dosis del retirado. Se intenta antes de la regla conservadora,
 *       porque el sustituto puede existir otro dia sin ser una restauracion de esa otra colocacion.</li>
 *   <li><b>SIN_CORRESPONDENCIA</b>: el ejercicio estaba en la semana medida, pero otro dia o mas veces. No se declara
 *       equivalencia: se compara con la MENOR dosis medida de ese ejercicio en la semana (nunca con el maximo semanal,
 *       que ocultaria una restauracion).</li>
 *   <li><b>NUEVA</b>: el ejercicio no estaba en la semana medida. No hay colocacion con la que compararla: su dosis es
 *       la base de su capacidad (V24 en la validacion de la semana) y cuenta en el trabajo total, que P-03 sigue
 *       comprobando aparte.</li>
 *   <li><b>RETIRADA</b>: colocacion medida sin pareja en la regenerada (el ejercicio que dolio, o uno que la semana
 *       nueva ya no incluye). Solo baja trabajo.</li>
 * </ul>
 * Las progresiones autorizadas no se ven afectadas: P-03 nivel 2 solo actua ante dolor NO_FACTIBLE y regenera con dosis
 * base, que nunca superan una dosis progresada de la semana medida; la progresion sigue su propio camino (V31).
 */
public final class ComparacionDosisPorColocacion {

    /** Una colocacion: dia de la semana, posicion en la sesion (0..n-1), ejercicio y dosis. */
    public record Colocacion(DayOfWeek dia, int posicion, long ejercicio, int series, int reps, int segundos) {
        long volumenReps() {
            return (long) series * reps;
        }

        long volumenSegundos() {
            return (long) series * segundos;
        }

        public String dosis() {
            return series + "x" + (segundos > 0 ? segundos + "s" : String.valueOf(reps));
        }
    }

    public enum Tipo { MANTENIDA, SUSTITUCION, SIN_CORRESPONDENCIA, NUEVA }

    /** Una colocacion regenerada que sube respecto de la medida con la que se compara. */
    public record Aumento(Tipo tipo, Colocacion medida, Colocacion regenerada, List<String> dimensiones) {
        public String describir() {
            return "P-03 nivel 2 (por colocacion, " + tipo + "): el ejercicio " + regenerada.ejercicio() + " del "
                    + regenerada.dia() + " sube de " + medida.dosis() + " (ejercicio " + medida.ejercicio() + ") a "
                    + regenerada.dosis() + " " + dimensiones + "; ante dolor ninguna colocacion recupera dosis sin"
                    + " autorizacion.";
        }
    }

    public record Resultado(List<Aumento> aumentos, Map<Tipo, Integer> porTipo, int retiradas) {
        public boolean sinAumentos() {
            return aumentos.isEmpty();
        }
    }

    private ComparacionDosisPorColocacion() {
    }

    /**
     * @param retirados ejercicios con exclusion activa (dolor o seguridad): sus puestos son los que un sustituto ocupa.
     */
    public static Resultado comparar(List<Colocacion> medida, List<Colocacion> regenerada, Set<Long> retirados) {
        var porClave = new LinkedHashMap<String, Deque<Colocacion>>();
        var porEjercicio = new HashMap<Long, List<Colocacion>>();
        for (var c : medida) {
            porClave.computeIfAbsent(clave(c), k -> new ArrayDeque<>()).add(c);
            porEjercicio.computeIfAbsent(c.ejercicio(), k -> new ArrayList<>()).add(c);
        }
        var enRegenerada = new HashSet<Long>();
        regenerada.forEach(c -> enRegenerada.add(c.ejercicio()));
        var puestosRetirados = new HashMap<String, Colocacion>();
        for (var c : medida)
            if (retirados.contains(c.ejercicio()) && !enRegenerada.contains(c.ejercicio()))
                puestosRetirados.put(c.dia() + "#" + c.posicion(), c);
        var aumentos = new ArrayList<Aumento>();
        var porTipo = new EnumMap<Tipo, Integer>(Tipo.class);
        for (var t : Tipo.values()) porTipo.put(t, 0);
        int emparejadas = 0;
        for (var r : regenerada) {
            var cola = porClave.get(clave(r));
            Tipo tipo;
            Colocacion referencia = null;
            if (cola != null && !cola.isEmpty()) {
                tipo = Tipo.MANTENIDA;
                referencia = cola.poll();
                emparejadas++;
            } else if (puestosRetirados.containsKey(r.dia() + "#" + r.posicion())) {
                tipo = Tipo.SUSTITUCION;
                referencia = puestosRetirados.remove(r.dia() + "#" + r.posicion());
                emparejadas++;
            } else if (porEjercicio.containsKey(r.ejercicio())) {
                tipo = Tipo.SIN_CORRESPONDENCIA;
                referencia = porEjercicio.get(r.ejercicio()).stream()
                        .min(Comparator.comparingLong(Colocacion::volumenReps)
                                .thenComparingLong(Colocacion::volumenSegundos)
                                .thenComparingInt(Colocacion::series))
                        .orElseThrow();
            } else {
                tipo = Tipo.NUEVA;
            }
            porTipo.merge(tipo, 1, Integer::sum);
            if (referencia != null) {
                var dims = sube(referencia, r);
                if (!dims.isEmpty()) aumentos.add(new Aumento(tipo, referencia, r, List.copyOf(dims)));
            }
        }
        return new Resultado(List.copyOf(aumentos), Collections.unmodifiableMap(porTipo),
                Math.max(0, medida.size() - emparejadas));
    }

    private static String clave(Colocacion c) {
        return c.dia() + "|" + c.ejercicio();
    }

    private static List<String> sube(Colocacion m, Colocacion r) {
        var d = new ArrayList<String>();
        if (r.series() > m.series()) d.add("series " + m.series() + "->" + r.series());
        if (r.reps() > m.reps()) d.add("repeticiones " + m.reps() + "->" + r.reps());
        if (r.segundos() > m.segundos()) d.add("segundos " + m.segundos() + "->" + r.segundos());
        if (r.volumenReps() > m.volumenReps()) d.add("volumen " + m.volumenReps() + "->" + r.volumenReps());
        if (r.volumenSegundos() > m.volumenSegundos())
            d.add("volumen_segundos " + m.volumenSegundos() + "->" + r.volumenSegundos());
        return d;
    }
}
