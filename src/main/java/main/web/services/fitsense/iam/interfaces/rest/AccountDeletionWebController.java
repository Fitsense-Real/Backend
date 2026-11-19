package main.web.services.fitsense.iam.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import main.web.services.fitsense.iam.application.internal.deletion.EliminacionDeCuentaService;
import main.web.services.fitsense.iam.application.internal.deletion.SolicitudEliminacionWebService;
import main.web.services.fitsense.iam.domain.exceptions.ConfirmationRequiredException;
import main.web.services.fitsense.iam.interfaces.rest.resources.AccountDeletionResource;
import main.web.services.fitsense.shared.domain.exceptions.ResourceNotFoundException;
import main.web.services.fitsense.shared.infrastructure.ratelimit.LimitePorIp;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Backend de la pagina publica de eliminacion de cuenta (SOLICITUD_ELIMINACION_WEB.md). Sin autenticacion: la
 * propiedad del correo se demuestra con el token del correo. Todas las rutas tienen limite por IP.
 */
@RestController
@SecurityRequirements
@RequestMapping(value = "/api/v1/account-deletion", produces = "application/json")
@Tag(name = "Eliminacion de cuenta (web)", description = "Solicitud publica de eliminacion verificada por correo")
public class AccountDeletionWebController {

    static final String RESPUESTA = "Si el correo corresponde a una cuenta, recibiras un enlace para confirmar la"
            + " eliminacion.";

    public record SolicitudResource(@NotBlank @Size(max = 254) String email) {}

    public record ConfirmacionResource(@NotBlank @Size(max = 200) String token, Boolean confirm) {
        @Override
        public String toString() {
            return "ConfirmacionResource[token=***, confirm=" + confirm + "]";
        }
    }

    private final SolicitudEliminacionWebService web;
    private final EliminacionDeCuentaService eliminacion;
    private final LimitePorIp limite;

    public AccountDeletionWebController(SolicitudEliminacionWebService web, EliminacionDeCuentaService eliminacion,
                                        LimitePorIp limite) {
        this.web = web;
        this.eliminacion = eliminacion;
        this.limite = limite;
    }

    @PostMapping("/requests")
    @Operation(summary = "Pide el enlace de eliminacion (misma respuesta exista o no el correo)")
    public ResponseEntity<Map<String, String>> solicitar(@Valid @RequestBody SolicitudResource r,
                                                         HttpServletRequest peticion) {
        limite.comprobar("account-deletion-request", peticion);
        web.solicitar(r.email());
        return ResponseEntity.accepted().header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(Map.of("message", RESPUESTA));
    }

    @PostMapping("/confirm")
    @Operation(summary = "Confirma con el token del correo y elimina la cuenta (irreversible)")
    public ResponseEntity<AccountDeletionResource> confirmar(@Valid @RequestBody ConfirmacionResource r,
                                                             HttpServletRequest peticion) {
        limite.comprobar("account-deletion-confirm", peticion);
        if (!Boolean.TRUE.equals(r.confirm()))
            throw new ConfirmationRequiredException(
                    "Falta la confirmacion: envia confirm=true para eliminar la cuenta de forma irreversible.");
        return respuesta(web.confirmar(r.token()));
    }

    @GetMapping("/requests/{reference}")
    @Operation(summary = "Estado de una eliminacion por su referencia (no expone datos de la cuenta)")
    public ResponseEntity<AccountDeletionResource> estado(@PathVariable UUID reference, HttpServletRequest peticion) {
        limite.comprobar("account-deletion-status", peticion);
        return eliminacion.consultar(reference).map(AccountDeletionWebController::respuestaEstado)
                .orElseThrow(() -> new ResourceNotFoundException("No hay ninguna eliminacion con esa referencia."));
    }

    private static ResponseEntity<AccountDeletionResource> respuesta(EliminacionDeCuentaService.Eliminacion e) {
        var recurso = new AccountDeletionResource(e.completada() ? "DELETED" : "PENDING", e.deletionId(),
                e.solicitadaEn(), e.completadaEn(), null);
        return ResponseEntity.status(e.completada() ? HttpStatus.OK : HttpStatus.ACCEPTED)
                .header(HttpHeaders.CACHE_CONTROL, "no-store").body(recurso);
    }

    private static ResponseEntity<AccountDeletionResource> respuestaEstado(EliminacionDeCuentaService.Eliminacion e) {
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(new AccountDeletionResource(e.completada() ? "DELETED" : "PENDING", e.deletionId(),
                        e.solicitadaEn(), e.completadaEn(), null));
    }
}
