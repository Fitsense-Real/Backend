package main.web.services.fitsense.planning.domain.model.valueobjects;

import java.util.List;
import java.util.Map;

/**
 * El resultado de valorar la cobertura de los cuatro slots de 4C.
 * <p>
 * Se usa en dos momentos distintos con el mismo tipo:
 * <ul>
 *   <li>ANTES de generar, sobre el conjunto elegible: cuantos ejercicios hay
 *       disponibles por slot. Responde «¿se puede?».</li>
 *   <li>DESPUES de generar, sobre el borrador: cuantos ejercicios prescritos
 *       caen en cada slot. Responde «¿se hizo?».</li>
 * </ul>
 * El umbral de §6 solo tiene sentido en el primero. En el segundo lo que
 * importa es si el recuento es cero.
 */
public record SlotCoverage(
        /** Ejercicios por slot, los cuatro slots siempre presentes, cero incluido. */
        Map<MandatorySlot, Integer> countsBySlot,
        /**
         * Ejercicios que no caen en ningun slot obligatorio: accesorios, core,
         * cardio, movilidad. No es un problema: la mayoria del catalogo esta
         * aqui y las sesiones los necesitan.
         */
        int outsideSlots,
        /**
         * Ejercicios cuyo {@code movement_pattern} es nulo. En la base, la V26
         * lo prohibe en los activos ({@code ck_exercises_activo_clasificado}),
         * asi que esto deberia ser cero siempre. Si no lo es, el dato se degrado
         * y la cobertura calculada vale menos de lo que parece: se cuenta aparte
         * en vez de desaparecer dentro de outsideSlots.
         */
        int unclassified
) {

    /** Umbral de 4C v4 §6. Es el unico sitio donde viven estos dos numeros. */
    public static final int MINIMO_ROBUSTO = 3;

    public enum Verdict {
        /** Algun slot a cero: con este conjunto no hay semana que cumpla 4C §5. */
        NO_FACTIBLE,
        /** Todos los slots cubiertos, alguno con 1 o 2 opciones. */
        FRAGIL,
        /** Los cuatro slots con 3 o mas opciones. */
        ROBUSTO
    }

    public int count(MandatorySlot slot) {
        return countsBySlot.getOrDefault(slot, 0);
    }

    /** Slots con cero ejercicios, en el orden fijo de {@link MandatorySlot#ALL}. */
    public List<MandatorySlot> emptySlots() {
        return MandatorySlot.ALL.stream().filter(slot -> count(slot) == 0).toList();
    }

    /** Slots con 1 o 2 ejercicios: cubiertos, pero sin margen. */
    public List<MandatorySlot> fragileSlots() {
        return MandatorySlot.ALL.stream()
                .filter(slot -> count(slot) > 0 && count(slot) < MINIMO_ROBUSTO)
                .toList();
    }

    /** Slots con al menos un ejercicio. Es el conjunto exigible al borrador. */
    public List<MandatorySlot> coveredSlots() {
        return MandatorySlot.ALL.stream().filter(slot -> count(slot) > 0).toList();
    }

    /**
     * El veredicto de §6: 0 = NO FACTIBLE, 1 o 2 = FRAGIL, 3 o mas = ROBUSTO.
     * <p>
     * Manda el slot PEOR cubierto, no el promedio: cuatro slots con 20, 20, 20 y
     * 1 opcion son fragiles, porque el hueco que falla es el de 1.
     */
    public Verdict verdict() {
        int peor = MandatorySlot.ALL.stream().mapToInt(this::count).min().orElse(0);
        if (peor == 0) return Verdict.NO_FACTIBLE;
        if (peor < MINIMO_ROBUSTO) return Verdict.FRAGIL;
        return Verdict.ROBUSTO;
    }

    /** Una linea para log y para el mensaje de rechazo. */
    public String describe() {
        var sb = new StringBuilder();
        MandatorySlot.ALL.forEach(slot -> sb.append(slot).append('=').append(count(slot)).append(' '));
        sb.append("fuera_de_slot=").append(outsideSlots);
        if (unclassified > 0) sb.append(" SIN_PATRON=").append(unclassified);
        sb.append(" veredicto=").append(verdict());
        return sb.toString();
    }
}
