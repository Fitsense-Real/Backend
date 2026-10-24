package main.web.services.fitsense.analytics.domain.model.valueobjects;

import main.web.services.fitsense.shared.domain.model.valueobjects.EvidenceRule;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Resultado del calculo semanal antes de persistirse.
 * <p>
 * Los porcentajes son nullables a proposito: NULL significa "no hubo
 * denominador", y ese matiz separa a un participante que no cumplio de uno al
 * que el sistema no llego a proponerle nada. ck_wum_null_when_no_plan lo exige
 * en la base.
 */
public record MetricsCalculation(
        boolean hasActivePlan,
        Long planId,
        short weekNumber,
        int scheduledWorkouts,
        int validWorkouts,
        int completedWorkouts,
        int skippedWorkouts,
        int assignedExercises,
        int completedExercises,
        BigDecimal weightedAdherencePct,
        BigDecimal frequencyAdherencePct,
        BigDecimal workoutAdherencePct,
        BigDecimal exerciseAdherencePct,
        Integer plannedWeekVolume,
        int executedVolume,
        Integer plannedReps,
        Integer plannedSeconds,
        int executedReps,
        int executedSeconds,
        int totalTrainingMinutes,
        BigDecimal averageSessionRpe,
        BigDecimal averageSatisfaction,
        int consecutiveSkips,
        Short daysSinceLastWorkout,
        String dominantSkipReason,
        BigDecimal riskScore,
        RiskLevel riskLevel,
        Map<String, Object> riskFactors,
        boolean dropout,

        // ---- V30 (17/10/2026). Van al final a proposito: anadirlos en medio
        // obliga a recolocar una llamada de treinta argumentos posicionales y
        // un error ahi no lo detecta el compilador.
        //
        // weightedAdherencePct es la metrica primaria del estudio, con el
        // denominador de TODOS los entrenamientos previstos. Pese al nombre,
        // desde la correccion de adherencia no pondera: cada previsto pesa uno
        // (docs/FitSense/ADHERENCIA.md). Estas tres son medidas al lado.
        /** Previstos que tienen registro: hay sesion, o el previsto quedo SKIPPED. */
        int recordedWorkouts,
        /** Cumplimiento SOLO sobre los que tienen registro, sin pesos. NULL si ninguno. */
        BigDecimal observedAdherencePct,
        /** recordedWorkouts / scheduledWorkouts. Decide si hay evidencia para adaptar. */
        BigDecimal registrationCoveragePct
) {

    /** V30. La misma regla que usan la entidad y la vista ACL. */
    public boolean hasEnoughEvidence(double minimumCoveragePct) {
        return hasActivePlan && EvidenceRule.enoughEvidence(
                observedAdherencePct, registrationCoveragePct, minimumCoveragePct);
    }
}