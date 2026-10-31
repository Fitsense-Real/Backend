package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.aggregates.WeeklyTrainingPlan;
import main.web.services.fitsense.planning.domain.model.commands.*;
import main.web.services.fitsense.planning.domain.model.valueobjects.AdaptedPublication;
import main.web.services.fitsense.planning.domain.model.valueobjects.GeneratedPlan;
import main.web.services.fitsense.planning.domain.model.valueobjects.PreparedPlanGeneration;

import java.time.LocalDate;
import java.util.Optional;

/**
 * La generacion de un plan esta partida en tres fases a proposito. Ver el
 * javadoc de generate(): la del medio hace llamadas HTTP largas y no puede
 * ejecutarse dentro de una transaccion.
 * <p>
 * No existe un handle(GenerateWeeklyPlanCommand) de una sola pieza: mientras
 * existiera, cualquier llamador podria reintroducir el fallo sin darse cuenta.
 */
public interface WeeklyTrainingPlanCommandService {

    /**
     * El lunes de la semana del PRIMER plan, con la fecha local del
     * participante: la semana actual si ninguna de sus sesiones ya paso, la
     * siguiente si no. Generar la actual un sabado dejaba todas las sesiones
     * vencidas al nacer (plan 53).
     */
    LocalDate firstPlanWeekStart(Long userId);

    /** Fase 1: lee todo lo necesario para generar. Transaccional y corta. */
    PreparedPlanGeneration prepare(GenerateWeeklyPlanCommand command);

    /** Fase 2: llama al generador. SIN transaccion: tarda ~90 s de HTTP. */
    GeneratedPlan generate(PreparedPlanGeneration prepared);

    /** Fase 3: materializa y guarda. Transaccional y corta. */
    Optional<WeeklyTrainingPlan> persist(PreparedPlanGeneration prepared, GeneratedPlan generated);

    /**
     * FASE 3 DE LA ADAPTACION. Publica la semana adaptada y registra su
     * evidencia EN LA MISMA TRANSACCION.
     * <p>
     * Separada de {@link #persist} a proposito: aquella recibe un
     * {@code PlanDraft}, que ya no sabe de que sesion viene cada una. Esta
     * recibe la semana con su linaje intacto, sesion por sesion.
     * <p>
     * SOLO PARA RESULTADOS PUBLICABLES. Un NO_FACTIBLE no pasa por aqui: no hay
     * plan que publicar, y su evidencia va en una transaccion aparte, por
     * {@code AdaptationFailureRecorder}, porque la de publicacion no existe o se
     * esta deshaciendo.
     */
    Optional<WeeklyTrainingPlan> publishAdapted(PreparedPlanGeneration prepared,
                                                AdaptedPublication publicacion);

    void handle(StartPlannedWorkoutCommand command);
    void handle(RecordWorkoutOutcomeCommand command);
    void handle(SkipPlannedWorkoutCommand command);
    void handle(ClosePlanWeekCommand command);
    int handle(ExpireOverdueWorkoutsCommand command);
}