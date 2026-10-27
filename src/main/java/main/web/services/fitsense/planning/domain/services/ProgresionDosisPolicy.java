package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.DosisAutorizada;
import main.web.services.fitsense.planning.domain.model.valueobjects.MvpDosePolicy;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;

/**
 * D-P3 · LOS RANGOS DENTRO DE LOS QUE V24 PUEDE ACEPTAR UNA DOSIS AUTORIZADA.
 * <p>
 * No cambia ninguna dosis exacta: la generacion sigue exigiendo la de siempre.
 * Esto solo responde, para una dosis que YA esta autorizada
 * ({@link DosisAutorizada}), si ademas cae dentro de lo aprobado. Una dosis
 * autorizada fuera de rango se rechaza igual.
 * <ul>
 *   <li>Progresion ({@code PASO} y {@code HEREDADA}): hasta
 *       {@value #REPS_EXTRA} repeticiones sobre la exacta; series hasta
 *       {@link CapabilityPrescriptionPolicy#SERIES_MAX_AMPLIACION} (ya
 *       aprobado), salvo las estructurales de {@code GAIN_MUSCLE}, que
 *       conservan sus series. El descanso no cambia.</li>
 *   <li>Reduccion ({@code REDUCCION}, {@code HEREDADA} y el {@code PASO} de una
 *       recuperacion que aun no llega a la base), solo en los objetivos
 *       por capacidades: nunca por encima de la exacta, ni por debajo del minimo
 *       de repeticiones de V16 para su zona y nivel, ni de una serie, ni de
 *       {@value #SEGUNDOS_MIN} s (min_duration_seconds). El descanso no cambia.
 *       En {@code GAIN_MUSCLE} no hay reducciones autorizadas: 4D sigue igual.</li>
 * </ul>
 */
public final class ProgresionDosisPolicy {

    /** D-P3: repeticiones por encima de la dosis exacta que puede alcanzar una progresion. */
    public static final short REPS_EXTRA = 2;
    /** min_duration_seconds de la configuracion vigente (V7). */
    public static final int SEGUNDOS_MIN = 20;

    private ProgresionDosisPolicy() {
    }

    /** 4D: solo progresiones. */
    public static boolean admite4D(MvpDosePolicy.Dosis exigida, PlanDraft.DraftExercise e,
                                   DosisAutorizada.Origen origen) {
        if (origen == DosisAutorizada.Origen.REDUCCION) return false;
        if (e.plannedSets() == null || e.plannedReps() == null || e.restSeconds() == null) return false;
        if (e.restSeconds() != exigida.descansoSegundos()) return false;
        short s = e.plannedSets(), r = e.plannedReps();
        boolean series = exigida.relleno()
                ? s >= exigida.series() && s <= Math.max(exigida.series(), CapabilityPrescriptionPolicy.SERIES_MAX_AMPLIACION)
                : s == exigida.series();
        return series && r >= exigida.reps() && r <= exigida.reps() + REPS_EXTRA;
    }

    /** Dosis por capacidad: progresion o reduccion, segun el origen. */
    public static boolean admiteCapacidad(CapabilityPrescriptionPolicy.Dosis d, PlanDraft.DraftExercise e,
                                          DosisAutorizada.Origen origen, int minReps, int maxReps) {
        if (e.restSeconds() == null || e.restSeconds() != d.descanso()) return false;
        if (e.prescriptionType() != d.tipo()) return false;
        short s = e.plannedSets() == null ? 1 : e.plannedSets();
        boolean progresion = origen != DosisAutorizada.Origen.REDUCCION;
        // Un PASO de recuperacion puede quedar aun por debajo de la base: cae en
        // el rango de reduccion, que es el mismo que el de una reduccion aprobada.
        boolean reduccion = true;
        boolean subeBien = s >= d.series()
                && s <= Math.max(d.seriesMax(), CapabilityPrescriptionPolicy.SERIES_MAX_AMPLIACION)
                && dentro(e, d, 0, REPS_EXTRA, maxReps);
        boolean bajaBien = s >= 1 && s <= d.series() && bajo(e, d, minReps);
        return (progresion && subeBien) || (reduccion && bajaBien);
    }

    private static boolean dentro(PlanDraft.DraftExercise e, CapabilityPrescriptionPolicy.Dosis d, int bajo,
                                  int sobre, int maxReps) {
        if (d.reps() != null)
            return e.plannedReps() != null && e.plannedReps() >= d.reps() - bajo
                    && e.plannedReps() <= Math.min(maxReps, d.reps() + sobre);
        return e.plannedDurationSeconds() != null && e.plannedDurationSeconds().equals(d.segundos());
    }

    private static boolean bajo(PlanDraft.DraftExercise e, CapabilityPrescriptionPolicy.Dosis d, int minReps) {
        if (d.reps() != null)
            return e.plannedReps() != null && e.plannedReps() <= d.reps() && e.plannedReps() >= minReps;
        return e.plannedDurationSeconds() != null && d.segundos() != null
                && e.plannedDurationSeconds() <= d.segundos() && e.plannedDurationSeconds() >= SEGUNDOS_MIN;
    }
}
