package main.web.services.fitsense.configuration.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import main.web.services.fitsense.configuration.domain.exceptions.NoActiveConfigurationException;
import main.web.services.fitsense.configuration.domain.model.aggregates.CalculationConfig;
import main.web.services.fitsense.configuration.domain.model.queries.GetActiveCalculationConfigQuery;
import main.web.services.fitsense.configuration.domain.model.valueobjects.CalculationParams;
import main.web.services.fitsense.configuration.domain.services.CalculationConfigQueryService;
import main.web.services.fitsense.configuration.interfaces.rest.resources.CalculationConfigResource;
import main.web.services.fitsense.configuration.interfaces.rest.resources.PublicCalculationConfigResource;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Solo lectura. Los umbrales se cambian por migracion, nunca por API: una fila
 * nueva es una version nueva y las metricas ya calculadas deben seguir apuntando
 * a la version con la que se produjeron.
 */
@RestController
@RequestMapping(produces = "application/json")
@Tag(name = "Configuracion", description = "Umbrales versionados del calculo")
public class CalculationConfigController {

    private final CalculationConfigQueryService calculationConfigQueryService;
    private final JsonSupport jsonSupport;

    public CalculationConfigController(CalculationConfigQueryService calculationConfigQueryService,
                                       JsonSupport jsonSupport) {
        this.calculationConfigQueryService = calculationConfigQueryService;
        this.jsonSupport = jsonSupport;
    }

    @GetMapping("/api/v1/config/active")
    @Operation(summary = "Lo que la app necesita de la configuracion vigente (USER)")
    public ResponseEntity<PublicCalculationConfigResource> active() {
        var config = activeConfig();
        var adherence = jsonSupport.read(config.getParams(), CalculationParams.class).adherence();
        return ResponseEntity.ok(new PublicCalculationConfigResource(
                config.getVersion(),
                config.getValidFrom(),
                adherence == null ? null : adherence.retroactiveDaysOrDefault(),
                adherence == null ? null : adherence.sessionCompletedThresholdPct()));
    }

    @GetMapping("/api/v1/admin/config/active")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Configuracion de calculo vigente completa, con sus parametros internos (ADMIN)")
    public ResponseEntity<CalculationConfigResource> activeForAdmin() {
        var config = activeConfig();
        return ResponseEntity.ok(new CalculationConfigResource(
                config.getId(),
                config.getVersion(),
                config.getDescription(),
                jsonSupport.read(config.getParams(), CalculationParams.class),
                config.getValidFrom()));
    }

    private CalculationConfig activeConfig() {
        return calculationConfigQueryService.handle(new GetActiveCalculationConfigQuery())
                .orElseThrow(NoActiveConfigurationException::new);
    }
}
