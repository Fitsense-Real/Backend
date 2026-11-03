package main.web.services.fitsense.analytics.domain.services;

import main.web.services.fitsense.analytics.domain.model.valueobjects.*;
import main.web.services.fitsense.configuration.domain.model.valueobjects.CalculationParams;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Las cuatro metricas de 17.3, el volumen ejecutado y el riesgo. Servicio de
 * dominio puro: recibe el denominador, las sesiones y los umbrales, y no
 * consulta nada.
 * <p>
 * Que sea puro no es purismo: es lo que permite recalcular una semana meses
 * despues con otra version de umbrales y comparar, que es exactamente lo que
 * pide la calibracion posterior al piloto.
 */
@Service
public class WeeklyMetricsCalculator {

    public MetricsCalculation calculate(WeekPlanInput plan,
                                        List<WeekSessionInput> sessions,
                                        BigDecimal previousWeightedAdherencePct,
                                        Short daysSinceLastWorkout,
                                        int plannedWeekVolume,
                                        int plannedReps,
                                        int plannedSeconds,
                                        CalculationParams params) {

        if (!plan.hasActivePlan()) return withoutPlan(plan, daysSinceLastWorkout, params);

        // 17.4 punto 3: por cada entrenamiento, UNA sesion. El repositorio ya
        // filtra por counts_toward_adherence, asi que aqui solo se indexa.
        var byWorkout = sessions.stream().collect(Collectors.toMap(
                WeekSessionInput::plannedWorkoutId, session -> session, (a, b) -> b));
        // Y por cada dia, UN previsto aunque la semana combine versiones.
        plan = plan.unoPorDia(byWorkout.keySet());

        var adherence = params.adherence();
        int scheduled = plan.scheduledWorkouts();

        int valid = 0;
        int completed = 0;
        int skipped = 0;
        int recorded = 0;
        int completedExercises = 0;
        int trainingMinutes = 0;
        int executedVolume = 0;
        int executedReps = 0;
        int executedSeconds = 0;
        // ADHERENCIA SEMANAL: A_w = sum(A_s / 100) / m x 100, con m los
        // previstos. Cada previsto pesa UNO: ni su duracion, ni su volumen, ni
        // su carga, ni el RPE o la satisfaccion cambian cuanto vale. A_s es el
        // cumplimiento de la sesion que cuenta (A_s = sum(A_e / 100) / n x 100,
        // calculado en execution), o 0 si no la hay.
        double adherenceNumerator = 0.0;
        int adherenceDenominator = 0;
        // V30: lo observado lleva su propio denominador, que solo suma los
        // entrenamientos con registro. Misma regla de pesos.
        double observedNumerator = 0.0;
        int observedDenominator = 0;

        var rpes = new ArrayList<Short>();
        var satisfactions = new ArrayList<Short>();
        var skipReasons = new ArrayList<String>();

        for (var workout : plan.workouts()) {
            var session = byWorkout.get(workout.plannedWorkoutId());
            // Tope de 100: superar un entrenamiento no compensa otro.
            double completion = session == null ? 0.0 : Math.min(100.0, Math.max(0.0, session.completion()));

            // V30. HAY REGISTRO si existe sesion, O si la persona declaro la
            // omision (SKIPPED con causa): eso es informacion -nos dijo que no
            // entreno-. Un previsto sin sesion no informa de nada, siga
            // SCHEDULED o lo haya cerrado como SKIPPED la tarea de vencimiento.
            boolean omisionDeclarada = workout.omisionDeclarada();
            boolean conRegistro = session != null || omisionDeclarada;

            // La metrica primaria tiene el denominador de TODOS los previstos:
            // lo que no tiene registro entra como 0. Una sesion por debajo del
            // 30 % (entrenamiento SKIPPED) aporta su cumplimiento real.
            adherenceDenominator++;
            adherenceNumerator += completion;

            if (conRegistro) {
                recorded++;
                observedDenominator++;
                observedNumerator += completion;
                // valid y skipped pasan a contar SOLO lo que tiene registro,
                // asi que su suma es recorded. Antes sumaban scheduled porque
                // lo desconocido caia en skipped: ese era el defecto.
                if (completion >= adherence.sessionValidThresholdPct()) valid++;
                else skipped++;
            }

            if (session != null) {
                if (completion >= adherence.sessionCompletedThresholdPct()) completed++;
                completedExercises += session.completedExercises();

                // Volumen realmente ejecutado, SIN TOPE. Se suma solo dentro del
                // recorrido del denominador: una sesion que no corresponde a un
                // entrenamiento de la semana no cuenta, igual que en 17.4.
                executedVolume += session.executedVolume();
                executedReps += session.executedReps();
                executedSeconds += session.executedSeconds();

                if (session.activeMinutes() != null) trainingMinutes += session.activeMinutes();
                if (session.sessionRpe() != null) rpes.add(session.sessionRpe());
                if (session.satisfaction() != null) satisfactions.add(session.satisfaction());
                if (session.dominantSkipReason() != null) skipReasons.add(session.dominantSkipReason());
            }

            if (workout.skipReason() != null) skipReasons.add(workout.skipReason());
        }

        // Sin previstos evaluables no hay adherencia: NULL, nunca 100.
        var weighted = adherenceDenominator <= 0 ? null
                : percentage(adherenceNumerator / adherenceDenominator);
        // V30. Sin un solo registro no hay denominador observado: NULL, no 0.
        // Es la diferencia entre "no sabemos" y "no entreno".
        var observed = observedDenominator <= 0 ? null
                : percentage(observedNumerator / observedDenominator);
        var coverage = scheduled <= 0 ? null : percentage(ratio(recorded, scheduled) * 100);
        var frequency = percentage(ratio(valid, scheduled) * 100);
        var workoutAdherence = percentage(ratio(completed, scheduled) * 100);
        var exerciseAdherence = plan.assignedExercises() <= 0 ? null
                : percentage(ratio(completedExercises, plan.assignedExercises()) * 100);

        int consecutiveSkips = trailingSkipStreak(plan, byWorkout, adherence.sessionValidThresholdPct());
        var averageRpe = average(rpes);

        var riskFactors = new LinkedHashMap<String, Object>();
        var riskScore = scoreRisk(weighted, consecutiveSkips, previousWeightedAdherencePct,
                averageRpe, params, riskFactors);

        boolean dropout = daysSinceLastWorkout != null
                && daysSinceLastWorkout >= params.dropout().daysWithoutWorkout();
        if (dropout) riskFactors.put("dropout_days", daysSinceLastWorkout);

        return new MetricsCalculation(
                true, plan.planId(), plan.weekNumber(),
                scheduled, valid, completed, skipped,
                plan.assignedExercises(), completedExercises,
                weighted, frequency, workoutAdherence, exerciseAdherence,
                plannedWeekVolume, executedVolume,
                plannedReps, plannedSeconds, executedReps, executedSeconds,
                trainingMinutes, averageRpe, average(satisfactions),
                consecutiveSkips, daysSinceLastWorkout, dominant(skipReasons),
                riskScore, levelOf(riskScore, params.risk().levels()), riskFactors, dropout,
                recorded, observed, coverage);
    }

    /**
     * Semana sin plan. Todas las adherencias van NULL: sin denominador no hay
     * porcentaje, y un cero aqui contaminaria los promedios del estudio con
     * semanas en las que el sistema simplemente no propuso nada (19.4).
     * <p>
     * Lo prescrito va NULL por la misma razon —volumen equivalente y sus dos
     * componentes crudos—, y asi lo exigen ck_wum_planned_volume_null_when_no_plan
     * y ck_wum_components_null_when_no_plan.
     * <p>
     * Lo ejecutado va 0 y no NULL: sin plan no puede haber sesiones que cuenten,
     * de modo que el cero es el valor real y no un hueco.
     */
    private MetricsCalculation withoutPlan(WeekPlanInput plan, Short daysSinceLastWorkout,
                                           CalculationParams params) {
        boolean dropout = daysSinceLastWorkout != null
                && daysSinceLastWorkout >= params.dropout().daysWithoutWorkout();

        var factors = new LinkedHashMap<String, Object>();
        factors.put("no_active_plan", true);
        if (dropout) factors.put("dropout_days", daysSinceLastWorkout);

        return new MetricsCalculation(
                false, null, plan.weekNumber(),
                0, 0, 0, 0, 0, 0,
                null, null, null, null,
                null, 0,
                null, null, 0, 0,
                0, null, null,
                0, daysSinceLastWorkout, null,
                null, null, factors, dropout,
                // Sin plan no hay previstos: cero registros y ningun porcentaje,
                // por la misma razon que las otras adherencias van NULL (19.4).
                0, null, null);
    }

    // ------------------------------------------------------------------- riesgo

    private BigDecimal scoreRisk(BigDecimal weighted, int consecutiveSkips,
                                 BigDecimal previousWeighted, BigDecimal averageRpe,
                                 CalculationParams params, Map<String, Object> factors) {
        var risk = params.risk();
        int score = 0;

        if (weighted != null) {
            int points = pointsFor(risk.adherencePoints(), weighted.doubleValue(),
                    entry -> entry.minPct(), entry -> entry.points());
            if (points > 0) factors.put("low_adherence", points);
            score += points;
        }

        int skipPoints = pointsFor(risk.consecutiveSkipsPoints(), consecutiveSkips,
                entry -> (double) entry.minCount(), entry -> entry.points());
        if (skipPoints > 0) factors.put("consecutive_skips", skipPoints);
        score += skipPoints;

        if (weighted != null && previousWeighted != null) {
            double drop = previousWeighted.doubleValue() - weighted.doubleValue();
            if (drop > 0) {
                int dropPoints = pointsFor(risk.dropPoints(), drop,
                        entry -> entry.minDropPp(), entry -> entry.points());
                if (dropPoints > 0) factors.put("adherence_drop", dropPoints);
                score += dropPoints;
            }
        }

        if (averageRpe != null && risk.rpeThreshold() != null
                && averageRpe.doubleValue() >= risk.rpeThreshold()) {
            factors.put("overexertion", risk.overexertionPoints());
            score += risk.overexertionPoints();
        }

        return BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
    }

    /** Toma el tramo mas alto que el valor alcanza. Las tablas viven en la configuracion. */
    private <T> int pointsFor(List<T> table, double value,
                              java.util.function.ToDoubleFunction<T> threshold,
                              java.util.function.ToIntFunction<T> points) {
        if (table == null) return 0;
        return table.stream()
                .sorted(Comparator.comparingDouble(threshold).reversed())
                .filter(entry -> value >= threshold.applyAsDouble(entry))
                .mapToInt(points)
                .findFirst()
                .orElse(0);
    }

    private RiskLevel levelOf(BigDecimal score, CalculationParams.Risk.Levels levels) {
        if (score == null || levels == null) return null;
        double value = score.doubleValue();
        if (levels.critical() != null && value >= levels.critical()) return RiskLevel.CRITICAL;
        if (levels.high() != null && value >= levels.high()) return RiskLevel.HIGH;
        if (levels.moderate() != null && value >= levels.moderate()) return RiskLevel.MODERATE;
        return RiskLevel.LOW;
    }

    // ------------------------------------------------------------------ helpers

    /**
     * Racha de entrenamientos seguidos sin sesion valida al FINAL de la semana.
     * <p>
     * Se cuenta desde el ultimo hacia atras: tres saltos al cierre predicen
     * abandono; tres saltos al inicio con recuperacion posterior, no.
     */
    private int trailingSkipStreak(WeekPlanInput plan, Map<Long, WeekSessionInput> byWorkout,
                                   double validThreshold) {
        var ordered = plan.workouts().stream()
                .sorted(Comparator.comparing(WeekPlanInput.ScheduledWorkout::scheduledDate))
                .toList();

        int streak = 0;
        for (int i = ordered.size() - 1; i >= 0; i--) {
            var session = byWorkout.get(ordered.get(i).plannedWorkoutId());
            if (session != null && session.completion() >= validThreshold) break;
            streak++;
        }
        return streak;
    }

    private String dominant(List<String> values) {
        return values.stream()
                .collect(Collectors.groupingBy(value -> value, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private BigDecimal average(List<Short> values) {
        if (values.isEmpty()) return null;
        double sum = values.stream().mapToInt(Short::intValue).sum();
        return BigDecimal.valueOf(sum / values.size()).setScale(2, RoundingMode.HALF_UP);
    }

    private double ratio(int numerator, int denominator) {
        return denominator <= 0 ? 0.0 : (double) numerator / denominator;
    }

    /** MIN(x, 1) x 100 de 17.3: superar lo indicado no compensa otra sesion no hecha. */
    private BigDecimal percentage(double value) {
        return BigDecimal.valueOf(Math.max(0.0, Math.min(100.0, value)))
                .setScale(2, RoundingMode.HALF_UP);
    }
}