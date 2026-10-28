package main.web.services.fitsense.configuration.interfaces.rest.resources;

import java.time.OffsetDateTime;

/**
 * Lo que la app movil necesita de la configuracion vigente: la version (para
 * etiquetar lo que muestra), desde cuando rige, cuantos dias atras admite un
 * reporte retroactivo y a partir de que cumplimiento una sesion cuenta como
 * completada. Los parametros de riesgo, adaptacion y prescripcion son internos y
 * solo los ve ADMIN en /api/v1/admin/config/active.
 */
public record PublicCalculationConfigResource(
        String version,
        OffsetDateTime validFrom,
        Integer retroactiveReportDays,
        Double sessionCompletedThresholdPct
) {}
