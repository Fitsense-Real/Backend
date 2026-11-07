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

    /** 4D: solo progresiones (la regla de siempre: sin politica de fatiga, ninguna reduccion en GAIN_MUSCLE). */
    public static boolean admite4D(MvpDosePolicy.Dosis exigida, PlanDraft.DraftExercise e,
                                   DosisAutorizada.Origen origen) {
        return admite4D(exigida, e, origen, false);
    }

    /**
     * 4D con la politica del contexto. Con {@code fatigaGM} (P-A1, solo {@code PLAN-REV-2.0}) se acepta ademas la
     * DOSIS DE MANTENIMIENTO de {@link #mantenimiento4D} cuando la autorizo V8C ante fatiga ({@code MANTENIMIENTO}) o
     * la hereda una copia con linaje ({@code HEREDADA}). Cualquier otra reduccion sigue rechazada, y la generacion
     * inicial (sin autorizaciones) no cambia.
     */
    public static boolean admite4D(MvpDosePolicy.Dosis exigida, PlanDraft.DraftExercise e,
                                   DosisAutorizada.Origen origen, boolean fatigaGM) {
        // Crear una dosis de mantenimiento (MANTENIMIENTO) exige la politica; HEREDAR una ya publicada con linaje no
        // (como P-P1: revertir la politica no deja la semana NO_FACTIBLE). Fuera de REV2 nunca se crea ninguna.
        if (((fatigaGM && origen == DosisAutorizada.Origen.MANTENIMIENTO) || origen == DosisAutorizada.Origen.HEREDADA)
                && mantenimiento4D(exigida, e)) return true;
        if (origen == DosisAutorizada.Origen.REDUCCION || origen == DosisAutorizada.Origen.MANTENIMIENTO) return false;
        if (e.plannedSets() == null || e.plannedReps() == null || e.restSeconds() == null) return false;
        if (e.restSeconds() != exigida.descansoSegundos()) return false;
        short s = e.plannedSets(), r = e.plannedReps();
        boolean series = exigida.relleno()
                ? s >= exigida.series() && s <= Math.max(exigida.series(), CapabilityPrescriptionPolicy.SERIES_MAX_AMPLIACION)
                : s == exigida.series();
        return series && r >= exigida.reps() && r <= exigida.reps() + REPS_EXTRA;
    }

    /**
     * P-A1 · LA DOSIS DE MANTENIMIENTO DE 4D ANTE FATIGA: una serie menos en una colocacion ESTRUCTURAL (3x10 -> 2x10,
     * un 33 %), con las mismas repeticiones y el mismo descanso. El relleno (2 series) no baja: por debajo de 2 series
     * lo prohibe V9. Las repeticiones no bajan: la evidencia revisada reduce series manteniendo el esfuerzo
     * (ciencia_rediseno §4: series -33 a -50 % una semana, mismos ejercicios y frecuencia). El tamano es PENDIENTE DE
     * VALIDACION PROFESIONAL.
     */
    public static boolean mantenimiento4D(MvpDosePolicy.Dosis exigida, PlanDraft.DraftExercise e) {
        if (exigida.relleno() || exigida.series() < 3) return false;
        if (e.plannedSets() == null || e.plannedReps() == null || e.restSeconds() == null) return false;
        return e.plannedSets() == exigida.series() - 1 && e.plannedReps() == exigida.reps()
                && e.restSeconds() == exigida.descansoSegundos();
    }

    /** Dosis por capacidad: progresion o reduccion, segun el origen. */
    public static boolean admiteCapacidad(CapabilityPrescriptionPolicy.Dosis d, PlanDraft.DraftExercise e,
                                          DosisAutorizada.Origen origen, int minReps, int maxReps) {
        if (e.restSeconds() == null || e.restSeconds() != d.descanso()) return false;
        if (e.prescriptionType() != d.tipo()) return false;
        short s = e.plannedSets() == null ? 1 : e.plannedSets();
        boolean progresion = origen != DosisAutorizada.Origen.REDUCCION
                && origen != DosisAutorizada.Origen.MANTENIMIENTO;
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
