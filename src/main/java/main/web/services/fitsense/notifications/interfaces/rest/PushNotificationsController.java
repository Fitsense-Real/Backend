package main.web.services.fitsense.notifications.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import main.web.services.fitsense.notifications.application.DispositivosPushService;
import main.web.services.fitsense.notifications.application.PreferenciasNotificacionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Notificaciones push de la cuenta autenticada: instalaciones (Firebase Installation ID) y preferencias.
 * Contrato: docs/FitSense/notificaciones_push/CONTRATO_PUSH_MOBILE.md. La autenticacion es la de siempre (JWT del
 * backend); Firebase solo transporta mensajes.
 */
@RestController
@RequestMapping(value = "/api/v1/users/me", produces = "application/json")
@Tag(name = "Notificaciones push", description = "Registro de instalaciones por FID y preferencias de notificacion")
public class PushNotificationsController {

    public record RegistroResource(
            @Schema(description = "Firebase Installation ID entregado por onRegistered (no un token FCM)",
                    example = "cR_9l0aBcD3fGh1jK2lM4n") @NotBlank @Size(max = 4096) String installationId,
            @Schema(example = "ANDROID") @NotBlank String platform,
            @Schema(example = "pe.fit.kora") @NotBlank @Size(max = 100) String appId,
            @Schema(example = "1.0.0+12") @Size(max = 40) String appVersion) {}

    public record DispositivoResource(long deviceId, String installationIdHint, String platform, String appVersion,
                                      String registeredAt, String lastSeenAt, boolean receivesNotifications) {
        static DispositivoResource de(DispositivosPushService.Dispositivo d) {
            return new DispositivoResource(d.deviceId(), d.installationIdHint(), d.platform(), d.appVersion(),
                    d.registeredAt().toString(), d.lastSeenAt().toString(), d.receivesNotifications());
        }
    }

    public record PreferenciasResource(@NotNull Boolean planUpdates, @NotNull Boolean accountNotices) {}

    private final DispositivosPushService dispositivos;
    private final PreferenciasNotificacionService preferencias;

    public PushNotificationsController(DispositivosPushService dispositivos,
                                       PreferenciasNotificacionService preferencias) {
        this.dispositivos = dispositivos;
        this.preferencias = preferencias;
    }

    @PutMapping(value = "/push-devices", consumes = "application/json")
    @Operation(summary = "Registra o renueva esta instalacion (idempotente). 201 si es nueva, 200 si ya estaba")
    public ResponseEntity<DispositivoResource> registrar(@AuthenticationPrincipal UserDetailsImpl principal,
                                                         @Valid @RequestBody RegistroResource r) {
        var reg = dispositivos.registrar(principal.getUserId(), r.installationId(), r.platform(), r.appId(),
                r.appVersion());
        return ResponseEntity.status(reg.nuevo() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(DispositivoResource.de(reg.dispositivo()));
    }

    @DeleteMapping("/push-devices/{installationId}")
    @Operation(summary = "Desvincula esta instalacion de la cuenta (cierre de sesion u onUnregistered). Idempotente")
    public ResponseEntity<Void> desvincular(@AuthenticationPrincipal UserDetailsImpl principal,
                                            @PathVariable String installationId) {
        dispositivos.desvincular(principal.getUserId(), installationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/push-devices")
    @Operation(summary = "Instalaciones activas de la cuenta (el FID se muestra ofuscado)")
    public List<DispositivoResource> listar(@AuthenticationPrincipal UserDetailsImpl principal) {
        return dispositivos.listar(principal.getUserId()).stream().map(DispositivoResource::de).toList();
    }

    @GetMapping("/notification-preferences")
    @Operation(summary = "Preferencias de notificacion; sin configurar, las dos categorias estan activadas")
    public PreferenciasResource leerPreferencias(@AuthenticationPrincipal UserDetailsImpl principal) {
        var p = preferencias.leer(principal.getUserId());
        return new PreferenciasResource(p.planUpdates(), p.accountNotices());
    }

    @PutMapping(value = "/notification-preferences", consumes = "application/json")
    @Operation(summary = "Guarda las preferencias; se aplican tambien a lo que estuviera pendiente de enviar")
    public PreferenciasResource guardarPreferencias(@AuthenticationPrincipal UserDetailsImpl principal,
                                                    @Valid @RequestBody PreferenciasResource r) {
        var p = preferencias.guardar(principal.getUserId(),
                new PreferenciasNotificacionService.Preferencias(r.planUpdates(), r.accountNotices()));
        return new PreferenciasResource(p.planUpdates(), p.accountNotices());
    }
}
