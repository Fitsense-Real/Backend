package main.web.services.fitsense.planning.interfaces.acl;

import java.math.BigDecimal;

/**
 * Lo que planning necesita saber de la semana MEDIDA para registrar la
 * evidencia de una adaptacion.
 * <p>
 * POR QUE ENTRA POR AQUI Y NO LO CONSULTA PLANNING. Estos cuatro datos viven en
 * analytics, y planning no tiene ACL hacia analytics: sus salientes son
 * catalog, configuration, execution, iam y profiling, y esa ausencia es
 * deliberada. Orchestration ya los tiene -son el {@code WeeklyMetricsView} del
 * paso 2 del ciclo-, asi que los pasa.
 *
 * @param weeklyMetricId           la fila de metricas de la semana medida. V32
 *                                 la referencia por clave ajena.
 * @param measuredPlanId           el plan de esa semana: del que PROVIENE la
 *                                 adaptacion, no el que la sustituye.
 * @param observedAdherencePct     adherencia sobre lo que se registro.
 * @param registrationCoveragePct  que proporcion de la semana llego a
 *                                 registrarse. Sin esto, una adherencia baja no
 *                                 se distingue de una semana sin registrar.
 * @param decision                 el desenlace que decidio el contexto de
 *                                 adaptacion: NO_CHANGE, INSUFFICIENT_DATA,
 *                                 ADAPT_LOCALIZED o ADAPT_EXPANDED. Viaja como
 *                                 texto porque los dos contextos tienen su
 *                                 propio enum con los mismos cuatro nombres, y
 *                                 compartir el tipo los ataria.
 */
public record MeasuredWeekView(Long weeklyMetricId,
                               Long measuredPlanId,
                               BigDecimal observedAdherencePct,
                               BigDecimal registrationCoveragePct,
                               String decision) {

    public MeasuredWeekView {
        if (weeklyMetricId == null)
            throw new IllegalArgumentException(
                    "La evidencia de una adaptacion referencia la medicion de la semana.");
        if (measuredPlanId == null)
            throw new IllegalArgumentException(
                    "Una semana adaptada declara de que plan proviene.");
        if (decision == null || decision.isBlank())
            throw new IllegalArgumentException(
                    "Sin decision no se puede registrar la evidencia del intento.");
    }
}
