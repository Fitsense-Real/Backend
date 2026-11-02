package main.web.services.fitsense.iam.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import main.web.services.fitsense.iam.application.internal.commandservices.PasswordResetService;
import main.web.services.fitsense.iam.interfaces.rest.resources.PasswordResetConfirmResource;
import main.web.services.fitsense.iam.interfaces.rest.resources.PasswordResetRequestResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Recuperacion de contrasena (publica, bajo /api/v1/auth). Ver {@link PasswordResetService}.
 * <p>
 * {@code POST /password-reset/request} responde SIEMPRE 202 con el mismo cuerpo. {@code POST /password-reset/confirm}
 * responde 204, o 400 con el mismo mensaje para cualquier codigo no valido.
 */
@RestController
@SecurityRequirements
@RequestMapping(value = "/api/v1/auth/password-reset", produces = "application/json")
@Tag(name = "Autenticacion", description = "Registro e inicio de sesion")
public class PasswordResetController {

    static final String RESPUESTA = "Si el correo corresponde a una cuenta, recibiras un codigo para restablecer"
            + " tu contrasena.";

    private final PasswordResetService servicio;

    public PasswordResetController(PasswordResetService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/request")
    @Operation(summary = "Solicita un codigo de recuperacion (misma respuesta exista o no el correo)")
    public ResponseEntity<Map<String, String>> solicitar(@Valid @RequestBody PasswordResetRequestResource r) {
        servicio.solicitar(r.email());
        return ResponseEntity.accepted().body(Map.of("message", RESPUESTA));
    }

    @PostMapping("/confirm")
    @Operation(summary = "Cambia la contrasena con el codigo recibido (un solo uso)")
    public ResponseEntity<Void> confirmar(@Valid @RequestBody PasswordResetConfirmResource r) {
        servicio.confirmar(r.token(), r.newPassword());
        return ResponseEntity.noContent().build();
    }
}
