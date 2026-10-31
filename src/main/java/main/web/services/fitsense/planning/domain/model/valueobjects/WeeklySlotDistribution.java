package main.web.services.fitsense.planning.domain.model.valueobjects;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Como se reparten los cuatro slots de 4C A LO LARGO DE LA SEMANA, no solo
 * cuantas veces aparecen.
 * <p>
 * EXISTE PORQUE CONTAR COLOCACIONES ENGANA. En el plan 4 (12/10/2026)
 * {@code LOWER_HIP} salio con 2 colocaciones y la cobertura dio por buena la
 * semana; las dos estaban el MISMO dia, asi que el participante entrenaba
 * cadena posterior una sola vez. Cubrir los cuatro patrones y repartirlos bien
 * son dos cosas distintas, y hasta ahora el log solo sabia medir la primera.
 * <p>
 * ES SOLO OBSERVACION. No cambia el criterio de aceptacion de 4C: que un slot
 * se concentre en un dia NO es un incumplimiento, es un dato que hay que reunir
 * antes de decidir si merece una regla. Nada de esta clase entra en la lista de
 * problemas del validador.
 * <p>
 * El dia se cuenta por FECHA DISTINTA: dos ejercicios de cadera el miercoles
 * son un dia, no dos.
 */
public record WeeklySlotDistribution(
        /** Fechas de entrenamiento de la semana, en orden y sin repetir. */
        List<LocalDate> trainingDates,
        /**
         * Colocaciones por slot y por dia, en el mismo orden que
         * {@link #trainingDates}. Los cuatro slots estan SIEMPRE, con su lista
         * completa de ceros si hace falta: un slot que falta no debe
         * desaparecer del informe, que es el fallo que ya costo una correccion
         * en el SQL de verificacion.
         */
        Map<MandatorySlot, List<Integer>> placementsByDay
) {

    /** Veces que el slot aparece en toda la semana. */
    public int placements(MandatorySlot slot) {
        return perDay(slot).stream().mapToInt(Integer::intValue).sum();
    }

    /** Fechas distintas en las que el slot aparece al menos una vez. */
    public int distinctDays(MandatorySlot slot) {
        return (int) perDay(slot).stream().filter(n -> n > 0).count();
    }

    /** Colocaciones dia a dia, en orden cronologico. Nunca null. */
    public List<Integer> perDay(MandatorySlot slot) {
        var lista = placementsByDay.get(slot);
        if (lista != null) return lista;
        var ceros = new ArrayList<Integer>();
        trainingDates.forEach(fecha -> ceros.add(0));
        return List.copyOf(ceros);
    }

    /** Slots presentes en la semana pero en un unico dia, con 2 o mas veces. */
    public List<MandatorySlot> concentratedSlots() {
        return MandatorySlot.ALL.stream()
                .filter(slot -> distinctDays(slot) == 1 && placements(slot) >= 2)
                .toList();
    }

    /** Slots que no aparecen ningun dia. */
    public List<MandatorySlot> missingSlots() {
        return MandatorySlot.ALL.stream()
                .filter(slot -> placements(slot) == 0)
                .toList();
    }

    /**
     * El bloque que va al log. Varias lineas a proposito: es un informe para
     * leer mientras se reunen generaciones, no un registro para agregar.
     * <p>
     * La linea de una sola linea que ya existia NO se sustituye: esta se suma.
     */
    public String describe(short weekNumber, String modo) {
        var sb = new StringBuilder();
        sb.append("Cobertura 4C posterior - semana ").append(weekNumber)
                .append(" (").append(trainingDates.size()).append(" dias: ")
                .append(trainingDates).append(")\n");

        for (var slot : MandatorySlot.ALL) {
            int dias = distinctDays(slot);
            int veces = placements(slot);
            sb.append(String.format("  %-11s -> %d %-13s | %d %-5s| %s%n",
                    slot, veces, veces == 1 ? "colocacion" : "colocaciones",
                    dias, dias == 1 ? "dia" : "dias", perDay(slot)));
        }

        var faltan = missingSlots();
        sb.append("  Cobertura estructural: ")
                .append(faltan.isEmpty() ? "COMPLETA" : "INCOMPLETA, faltan " + faltan)
                .append('\n');

        var concentrados = concentratedSlots();
        sb.append("  Distribucion semanal: ");
        if (concentrados.isEmpty()) {
            sb.append("sin slots concentrados en un solo dia");
        } else {
            sb.append(concentrados.stream().map(Enum::name)
                            .collect(java.util.stream.Collectors.joining(", ")))
                    .append(concentrados.size() == 1
                            ? " concentrado en 1 dia" : " concentrados en 1 dia");
        }
        sb.append('\n');

        sb.append("  Modo: ").append(modo)
                .append(" (la distribucion NO decide nada: solo se mide)");
        return sb.toString();
    }

    /** Mapa vacio con los cuatro slots y una lista de ceros por dia. */
    public static WeeklySlotDistribution empty(List<LocalDate> fechas) {
        var mapa = new LinkedHashMap<MandatorySlot, List<Integer>>();
        for (var slot : MandatorySlot.ALL) {
            var ceros = new ArrayList<Integer>();
            fechas.forEach(fecha -> ceros.add(0));
            mapa.put(slot, List.copyOf(ceros));
        }
        return new WeeklySlotDistribution(List.copyOf(fechas), Map.copyOf(mapa));
    }
}
