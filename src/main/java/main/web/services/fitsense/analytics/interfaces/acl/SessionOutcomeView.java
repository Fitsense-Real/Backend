package main.web.services.fitsense.analytics.interfaces.acl;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Lo que paso con una sesion, cruzando hacia el modulo de adaptacion.
 * <p>
 * Es lo minimo para senalar: identificador persistente, cuando, de que era,
 * si hay registro, cuanto se cumplio, por que se omitio y con que esfuerzo.
 * <b>Sin detalle por ejercicio</b>, que el prompt ya le manda a la IA.
 * <p>
 * {@code plannedWorkoutId} es el identificador por el que hay que reconocer la
 * sesion. La fecha puede cambiar legitimamente -con {@code SCHEDULE_CHANGE} la
 * IA puede mover el entrenamiento de dia-, asi que validar por fecha rechazaria
 * un cambio correcto.
 */
public record SessionOutcomeView(
        Long plannedWorkoutId,
        LocalDate scheduledDate,
        String status,
        boolean recorded,
        BigDecimal completionPct,
        String skipReason,
        Short sessionRpe,
        boolean problematic
) {}
