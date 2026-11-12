package main.web.services.fitsense.analytics.domain.model.valueobjects;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Lo que paso con UNA sesion de la semana. Es lo minimo que el decisor necesita
 * para senalar cual fue el problema, y nada mas.
 * <p>
 * NO lleva el detalle por ejercicio a proposito. El prompt ya se lo manda a la
 * IA -planificado, real, porcentaje, estado y motivo de cada ejercicio-, asi que
 * duplicarlo aqui seria mantener dos copias del mismo dato para que una decida
 * lo que la otra ya sabe. El backend senala la SESION y el porque; la IA mira
 * dentro.
 * <p>
 * El RPE viaja pero NO senala: una sesion completada con RPE 8 no es un fallo.
 * Es contexto que explica un problema cuando coincide con baja ejecucion.
 *
 * @param recorded     hay registro: existe sesion, o el previsto quedo en
 *                     SKIPPED. Un previsto en SCHEDULED sin sesion no informa de
 *                     nada, y confundir las dos cosas era el defecto de V30.
 * @param completionPct cumplimiento de la sesion. NULL si no hay registro: sin
 *                     registro no hay un cero, hay un hueco.
 * @param problematic  no alcanzo el umbral de completada del instrumento, o se
 *                     omito. Lo decide el identificador con los umbrales de la
 *                     configuracion, no un numero escrito aqui.
 */
public record SessionOutcome(
        Long plannedWorkoutId,
        LocalDate scheduledDate,
        String status,
        boolean recorded,
        BigDecimal completionPct,
        String skipReason,
        Short sessionRpe,
        boolean problematic
) {

    /** Sin registro no sabemos si fallo: no se puede senalar lo que no consta. */
    public boolean unknown() {
        return !recorded;
    }
}
