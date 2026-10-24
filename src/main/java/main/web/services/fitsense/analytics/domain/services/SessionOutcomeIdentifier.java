package main.web.services.fitsense.analytics.domain.services;

import main.web.services.fitsense.analytics.domain.model.valueobjects.SessionOutcome;
import main.web.services.fitsense.analytics.domain.model.valueobjects.WeekPlanInput;
import main.web.services.fitsense.analytics.domain.model.valueobjects.WeekSessionInput;
import main.web.services.fitsense.configuration.domain.model.valueobjects.CalculationParams;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Cual de las sesiones de la semana fue un problema, y por que.
 * <p>
 * Servicio de dominio PURO, igual que {@link WeeklyMetricsCalculator}: recibe el
 * plan, las sesiones y los umbrales, y no consulta nada. Eso es lo que permite
 * recalcular una semana con otra version de umbrales y comparar.
 * <p>
 * POR QUE VIVE EN ANALYTICS Y NO EN ADAPTACION. Decidir si una sesion "fue un
 * problema" es aplicar el instrumento de medicion -los mismos umbrales de
 * completada, valida y completada-por-ejercicio que ya usa el calculo semanal-,
 * y eso es medir. Decidir QUE HACER con ese problema -localizada o ampliada, y
 * que se permite cambiar- es adaptar, y eso vive en el modulo de adaptacion.
 * <p>
 * NO INVENTA UMBRALES. Reutiliza los de la configuracion, que estan sembrados
 * desde V7 y pendientes de calibrar tras el piloto:
 * <ul>
 *   <li>{@code session_completed_threshold_pct} = 80: por debajo, la sesion no
 *       se da por completada, y eso es lo que la vuelve senalable.</li>
 *   <li>{@code session_valid_threshold_pct} = 30: por debajo, no cuenta como
 *       valida. Sirve para ordenar de peor a mejor, no para senalar.</li>
 * </ul>
 * El RPE NO entra en el criterio. Una sesion completada con RPE 8 no es un
 * fallo; el RPE viaja como contexto para que la IA lo tenga en cuenta.
 */
@Service
public class SessionOutcomeIdentifier {

    /**
     * @param plan     los entrenamientos previstos de la semana.
     * @param sessions las sesiones que CUENTAN -el repositorio ya filtra por
     *                 counts_toward_adherence, asi que las que estan en curso o
     *                 fueron desplazadas no llegan aqui-.
     */
    public List<SessionOutcome> identify(WeekPlanInput plan, List<WeekSessionInput> sessions,
                                         CalculationParams params) {
        if (plan == null || plan.workouts() == null) return List.of();

        var byWorkout = sessions == null ? java.util.Map.<Long, WeekSessionInput>of()
                : sessions.stream().collect(Collectors.toMap(
                WeekSessionInput::plannedWorkoutId, s -> s, (a, b) -> b));

        double umbralCompletada = params.adherence().sessionCompletedThresholdPct();
        plan = plan.unoPorDia(byWorkout.keySet());

        return plan.workouts().stream()
                .sorted(Comparator.comparing(WeekPlanInput.ScheduledWorkout::scheduledDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(workout -> {
                    var session = byWorkout.get(workout.plannedWorkoutId());
                    boolean omisionDeclarada = "SKIPPED".equals(workout.status());
                    boolean conRegistro = session != null || omisionDeclarada;

                    // Sin registro el cumplimiento es un HUECO, no un cero. La
                    // diferencia es toda la razon de ser de V30.
                    BigDecimal cumplimiento = session != null ? session.completionPercentage()
                            : omisionDeclarada ? BigDecimal.ZERO : null;

                    // Se senala lo que CONSTA que no se completo. Lo que no
                    // consta no se senala: no se puede corregir lo que no se
                    // sabe que fallo.
                    boolean senalada = conRegistro
                            && (cumplimiento == null
                            || cumplimiento.doubleValue() < umbralCompletada);

                    // El motivo de la sesion manda sobre el del previsto: es el
                    // que registro la persona al cerrarla.
                    String motivo = session != null && session.dominantSkipReason() != null
                            ? session.dominantSkipReason() : workout.skipReason();

                    return new SessionOutcome(
                            workout.plannedWorkoutId(), workout.scheduledDate(),
                            workout.status(), conRegistro,
                            cumplimiento, motivo,
                            session == null ? null : session.sessionRpe(), senalada);
                })
                .toList();
    }

    /**
     * La peor sesion senalada, que es el objetivo de la adaptacion LOCALIZADA.
     * <p>
     * Peor = menor cumplimiento. A igual cumplimiento, la mas antigua: si el
     * problema se repite, se corrige donde empezo. Las sesiones sin registro
     * nunca son candidatas, por mucho que parezcan las peores.
     */
    public Optional<SessionOutcome> worst(List<SessionOutcome> outcomes) {
        if (outcomes == null) return Optional.empty();
        return outcomes.stream()
                .filter(SessionOutcome::problematic)
                .min(Comparator
                        .comparing((SessionOutcome o) -> o.completionPct() == null
                                ? BigDecimal.ZERO : o.completionPct())
                        .thenComparing(SessionOutcome::scheduledDate,
                                Comparator.nullsLast(Comparator.naturalOrder())));
    }
}
