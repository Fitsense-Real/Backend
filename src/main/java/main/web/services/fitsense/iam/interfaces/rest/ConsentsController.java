package main.web.services.fitsense.iam.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import main.web.services.fitsense.iam.application.internal.consent.ConsentimientoService;
import main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Consentimientos de la persona autenticada (V63). Ver CONSENTIMIENTO_Y_EDAD.md. */
@RestController
@RequestMapping(value = "/api/v1/users/me/consents", produces = "application/json")
@Tag(name = "Consentimientos", description = "Historial, aceptacion y retirada de consentimientos versionados")
public class ConsentsController {

    public record AceptacionResource(@NotNull ConsentimientoService.Tipo type,
                                     @NotBlank @Size(max = 40) String documentVersion) {}

    public record ConsentimientoResource(String type, String documentVersion, String grantedAt,
                                         String affirmativeAction, String source, String withdrawnAt) {
        static ConsentimientoResource de(ConsentimientoService.Consentimiento c) {
            return new ConsentimientoResource(c.tipo().name(), c.version(), c.aceptadoEn().toString(), c.accion(),
                    c.origen(), c.retiradoEn() == null ? null : c.retiradoEn().toString());
        }
    }

    /**
     * {@code approvedDocuments}: si la version vigente de cada tipo corresponde a un documento aprobado (B2.1). Una
     * participacion registrada sin documento aprobado ({@code CASILLA_SIN_DOCUMENTO_APROBADO}) no es prueba valida.
     */
    public record EstadoResource(Map<String, String> currentVersions, Map<String, Boolean> approvedDocuments,
                                 boolean enforcedOnSignup, List<ConsentimientoResource> history) {}

    private final ConsentimientoService servicio;

    public ConsentsController(ConsentimientoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Versiones vigentes y el historial de consentimientos propios")
    public EstadoResource estado(@AuthenticationPrincipal UserDetailsImpl principal) {
        var vigentes = new LinkedHashMap<String, String>();
        var aprobados = new LinkedHashMap<String, Boolean>();
        Arrays.stream(ConsentimientoService.Tipo.values()).forEach(t -> {
            vigentes.put(t.name(), servicio.versionVigente(t).orElse(null));
            aprobados.put(t.name(), servicio.documentoAprobado(t));
        });
        return new EstadoResource(vigentes, aprobados, servicio.exigirEnRegistro(),
                servicio.historial(principal.getUserId()).stream().map(ConsentimientoResource::de).toList());
    }

    @PostMapping
    @Operation(summary = "Acepta la version vigente de las condiciones o de la politica de privacidad")
    public ResponseEntity<ConsentimientoResource> aceptar(@AuthenticationPrincipal UserDetailsImpl principal,
                                                          @Valid @RequestBody AceptacionResource r) {
        var c = servicio.aceptar(principal.getUserId(), r.type(), r.documentVersion());
        return ResponseEntity.status(HttpStatus.CREATED).body(ConsentimientoResource.de(c));
    }

    @PostMapping("/{type}/withdraw")
    @Operation(summary = "Retira el consentimiento de investigacion (equivale a retirarse del estudio)")
    public ResponseEntity<Void> retirar(@AuthenticationPrincipal UserDetailsImpl principal,
                                        @PathVariable ConsentimientoService.Tipo type) {
        servicio.retirar(principal.getUserId(), type);
        return ResponseEntity.noContent().build();
    }
}
