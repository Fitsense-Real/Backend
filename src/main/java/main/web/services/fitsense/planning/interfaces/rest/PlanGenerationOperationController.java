package main.web.services.fitsense.planning.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import main.web.services.fitsense.planning.application.internal.generation.PlanGenerationOperationService;
import main.web.services.fitsense.planning.interfaces.rest.resources.PlanGenerationOperationResource;
import main.web.services.fitsense.shared.domain.exceptions.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/users/me/plan/generation", produces = "application/json")
@Tag(name = "Planes", description = "Generacion asincrona del primer plan")
public class PlanGenerationOperationController {

    private final PlanGenerationOperationService service;

    public PlanGenerationOperationController(PlanGenerationOperationService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Pide la generacion del primer plan. 202 si se registro una operacion nueva; 200 si ya habia "
            + "una en curso o el plan ya existe (idempotente: no llama dos veces a la IA).")
    public ResponseEntity<PlanGenerationOperationResource> request(@AuthenticationPrincipal UserDetailsImpl principal) {
        var requested = service.request(principal.getUserId());
        return ResponseEntity.status(requested.created() ? HttpStatus.ACCEPTED : HttpStatus.OK)
                .body(PlanGenerationOperationResource.of(requested.operation()));
    }

    @GetMapping("/latest")
    @Operation(summary = "La ultima operacion del usuario, para recuperarla si la app perdio su id")
    public ResponseEntity<PlanGenerationOperationResource> latest(@AuthenticationPrincipal UserDetailsImpl principal) {
        return service.latest(principal.getUserId())
                .map(PlanGenerationOperationResource::of)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Operacion de generacion", principal.getUserId()));
    }

    @GetMapping("/{operationId}")
    @Operation(summary = "Estado de una operacion. 404 si no existe o es de otro usuario.")
    public ResponseEntity<PlanGenerationOperationResource> find(@AuthenticationPrincipal UserDetailsImpl principal,
                                                                @PathVariable UUID operationId) {
        return ResponseEntity.ok(PlanGenerationOperationResource.of(service.find(principal.getUserId(), operationId)));
    }
}
