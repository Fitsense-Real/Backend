package main.web.services.fitsense.orchestation;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

/**
 * COMPARACION DE DOSIS POR COLOCACION entre la semana medida y la publicada (orden de 14/11/2026, fase 1).
 * <p>
 * Comparar el maximo semanal de cada ejercicio oculta subidas localizadas: con el mismo ejercicio el lunes (3x10 ->
 * 3x12) y el jueves (3x15 -> 3x13), el maximo baja (15 -> 13) aunque el lunes suba. Aqui se compara cada COLOCACION
 * con su colocacion equivalente, y solo cuando la correspondencia es fiable:
 * <ul>
 *   <li><b>CORRESPONDIDA</b>: mismo ejercicio y mismo dia de la semana (las dos semanas son consecutivas y el perfil
 *       entrena los mismos dias). Si el ejercicio aparece varias veces el mismo dia, se emparejan por orden de
 *       aparicion. Se comparan series, repeticiones, segundos y volumen (series x repeticiones, series x segundos):
 *       cualquier dimension que suba es un AUMENTO.</li>
 *   <li><b>SIN_CORRESPONDENCIA</b>: el ejercicio estaba en la semana medida, pero en otro dia (cambio de dia) o mas
 *       veces que las colocaciones correspondidas. No se declara ninguna equivalencia: para no ocultar una subida se
 *       compara, de forma conservadora, con la MENOR dosis medida de ese ejercicio. Si la supera en alguna dimension,
 *       es un AUMENTO_SIN_CORRESPONDENCIA.</li>
 *   <li><b>SUSTITUCION</b>: mismo dia de la semana y MISMA POSICION en la sesion que un ejercicio medido que se
 *       retiro (excluido por dolor o por seguridad, y ausente de la semana publicada). Es la colocacion que lo
 *       reemplaza: se compara con la dosis del retirado. Se intenta ANTES de la regla conservadora, porque un sustituto
 *       que tambien existe en otro dia de la semana medida no es una restauracion de esa otra colocacion.</li>
 *   <li><b>NUEVA</b>: el ejercicio no estaba en la semana medida (sustituto o ejercicio regenerado). No tiene con que
 *       compararse por colocacion: su dosis debe ser la BASE de su capacidad (lo comprueba V24 en la validacion
 *       independiente) y cuenta en el trabajo total.</li>
 * </ul>
 * El trabajo total (repeticiones y segundos, series x valor) se compara aparte: es la regla de P-03 nivel 2.
 */
final class ComparadorDosisPorColocacion {

    record Colocacion(LocalDate fecha, long ejercicio, int series, int reps, int segundos, int orden) {
        Colocacion(LocalDate fecha, long ejercicio, int series, int reps, int segundos) {
            this(fecha, ejercicio, series, reps, segundos, -1);
        }

        DayOfWeek dia() {
            return fecha.getDayOfWeek();
        }

        long volumenReps() {
            return (long) series * reps;
        }

        long volumenSegundos() {
            return (long) series * segundos;
        }

        String dosis() {
            return series + "x" + (segundos > 0 ? segundos + "s" : reps);
        }
    }

    enum Tipo { AUMENTO, AUMENTO_EN_SUSTITUCION, AUMENTO_SIN_CORRESPONDENCIA }

    record Hallazgo(Tipo tipo, Colocacion medida, Colocacion publicada, List<String> dimensiones) {
        @Override
        public String toString() {
            return tipo + " ejercicio " + publicada.ejercicio() + " " + publicada.dia() + ": " + medida.dosis() + " ("
                    + medida.fecha() + ") -> " + publicada.dosis() + " (" + publicada.fecha() + ") " + dimensiones;
        }
    }

    record Resultado(List<Hallazgo> aumentos, int correspondidas, List<Colocacion> sustituciones,
                     List<Colocacion> sinCorrespondencia,
                     List<Colocacion> nuevas, long repsMedida, long repsPublicada, long segundosMedida,
                     long segundosPublicada) {

        boolean sinAumentos() {
            return aumentos.isEmpty();
        }

        boolean trabajoTotalNoSube() {
            return repsPublicada <= repsMedida && segundosPublicada <= segundosMedida;
        }

        String describir() {
            return "correspondidas=" + correspondidas + " sustituciones=" + sustituciones.size()
                    + " sin_correspondencia=" + sinCorrespondencia.size()
                    + " nuevas=" + nuevas.size() + " trabajo " + repsMedida + ":" + segundosMedida + " -> "
                    + repsPublicada + ":" + segundosPublicada + " aumentos=" + aumentos;
        }
    }

    private ComparadorDosisPorColocacion() {}

    static Resultado comparar(List<Colocacion> medida, List<Colocacion> publicada) {
        return comparar(medida, publicada, Set.of());
    }

    /**
     * @param retirados ejercicios retirados de la semana medida por dolor o por seguridad (las exclusiones activas): sus
     *                  colocaciones son las que un sustituto puede ocupar.
     */
    static Resultado comparar(List<Colocacion> medida, List<Colocacion> publicada, Set<Long> retirados) {
        // Colocaciones medidas por (dia, ejercicio), en orden de aparicion.
        var porClave = new LinkedHashMap<String, Deque<Colocacion>>();
        var porEjercicio = new HashMap<Long, List<Colocacion>>();
        for (var c : medida) {
            porClave.computeIfAbsent(clave(c), k -> new ArrayDeque<>()).add(c);
            porEjercicio.computeIfAbsent(c.ejercicio(), k -> new ArrayList<>()).add(c);
        }
        var publicados = new HashSet<Long>();
        publicada.forEach(c -> publicados.add(c.ejercicio()));
        // Puestos de los retirados: (dia, posicion) -> colocacion medida, si el ejercicio ya no esta en la publicada.
        var puestosRetirados = new HashMap<String, Colocacion>();
        for (var c : medida)
            if (c.orden() >= 0 && retirados.contains(c.ejercicio()) && !publicados.contains(c.ejercicio()))
                puestosRetirados.put(c.dia() + "#" + c.orden(), c);
        var aumentos = new ArrayList<Hallazgo>();
        var sustituciones = new ArrayList<Colocacion>();
        var sinCorrespondencia = new ArrayList<Colocacion>();
        var nuevas = new ArrayList<Colocacion>();
        int correspondidas = 0;
        for (var p : publicada) {
            var cola = porClave.get(clave(p));
            if (cola != null && !cola.isEmpty()) {
                var m = cola.poll();
                correspondidas++;
                var dims = sube(m, p);
                if (!dims.isEmpty()) aumentos.add(new Hallazgo(Tipo.AUMENTO, m, p, dims));
            } else if (p.orden() >= 0 && puestosRetirados.containsKey(p.dia() + "#" + p.orden())) {
                var m = puestosRetirados.remove(p.dia() + "#" + p.orden());
                sustituciones.add(p);
                var dims = sube(m, p);
                if (!dims.isEmpty()) aumentos.add(new Hallazgo(Tipo.AUMENTO_EN_SUSTITUCION, m, p, dims));
            } else if (porEjercicio.containsKey(p.ejercicio())) {
                sinCorrespondencia.add(p);
                var menor = porEjercicio.get(p.ejercicio()).stream()
                        .min(Comparator.comparingLong(Colocacion::volumenReps)
                                .thenComparingLong(Colocacion::volumenSegundos)
                                .thenComparingInt(Colocacion::series))
                        .orElseThrow();
                var dims = sube(menor, p);
                if (!dims.isEmpty()) aumentos.add(new Hallazgo(Tipo.AUMENTO_SIN_CORRESPONDENCIA, menor, p, dims));
            } else {
                nuevas.add(p);
            }
        }
        return new Resultado(List.copyOf(aumentos), correspondidas, List.copyOf(sustituciones), List.copyOf(sinCorrespondencia),
                List.copyOf(nuevas), medida.stream().mapToLong(Colocacion::volumenReps).sum(),
                publicada.stream().mapToLong(Colocacion::volumenReps).sum(),
                medida.stream().mapToLong(Colocacion::volumenSegundos).sum(),
                publicada.stream().mapToLong(Colocacion::volumenSegundos).sum());
    }

    private static String clave(Colocacion c) {
        return c.dia() + "|" + c.ejercicio();
    }

    /** Las dimensiones en las que la publicada supera a la medida. */
    private static List<String> sube(Colocacion m, Colocacion p) {
        var d = new ArrayList<String>();
        if (p.series() > m.series()) d.add("series " + m.series() + "->" + p.series());
        if (p.reps() > m.reps()) d.add("repeticiones " + m.reps() + "->" + p.reps());
        if (p.segundos() > m.segundos()) d.add("segundos " + m.segundos() + "->" + p.segundos());
        if (p.volumenReps() > m.volumenReps()) d.add("volumen " + m.volumenReps() + "->" + p.volumenReps());
        if (p.volumenSegundos() > m.volumenSegundos())
            d.add("volumen_segundos " + m.volumenSegundos() + "->" + p.volumenSegundos());
        return d;
    }
}
