package main.web.services.fitsense.administration.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import main.web.services.fitsense.administration.application.internal.operations.IndicadoresOperativosService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * B5.1 (P1-01): estado operativo del backend para el panel (ADMIN): participantes sin plan vigente, cierres
 * pendientes, generaciones y adaptaciones fallidas, operaciones huerfanas, cortacircuitos y contadores de correo y
 * autenticacion. Solo recuentos: ningun dato de una persona.
 */
@RestController
@RequestMapping(value = "/api/v1/admin/operations", produces = "application/json")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administracion · operacion", description = "Indicadores operativos (ADMIN)")
public class AdminOperationsController {

    private final IndicadoresOperativosService indicadores;

    public AdminOperationsController(IndicadoresOperativosService indicadores) {
        this.indicadores = indicadores;
    }

    @GetMapping
    @Operation(summary = "Indicadores operativos (recuentos, sin datos personales)")
    public ResponseEntity<IndicadoresOperativosService.Indicadores> estado() {
        return ResponseEntity.ok(indicadores.calcular());
    }

    public record CierresAtrasadosResource(java.time.Instant calculatedAt, long catchUpHours,
                                           java.util.List<IndicadoresOperativosService.CierreAtrasado> items) {}

    /**
     * C3 · Que semanas se quedaron sin cerrar y de quien (codigo seudonimo P-xxx, zona y semana; ni correo ni datos de
     * salud). Solo lectura. Las {@code FUERA_DE_VENTANA} no se recuperan solas: el procedimiento esta en
     * 04_CONTINUIDAD_CIERRE_SEMANAL.md. Queda auditado como una consulta de datos de participantes.
     */
    @GetMapping("/weekly-closures/overdue")
    @Operation(summary = "Cierres semanales sin completar (EN_VENTANA se recuperan solos; FUERA_DE_VENTANA, no)")
    public ResponseEntity<CierresAtrasadosResource> cierresAtrasados(
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl admin) {
        var items = indicadores.cierresAtrasados();
        main.web.services.fitsense.administration.infrastructure.audit.AdminAudit.exported(
                admin == null ? null : admin.getUserId(), "weekly-closures-overdue", items.size());
        return ResponseEntity.ok(new CierresAtrasadosResource(java.time.Instant.now(), indicadores.horasDeRecuperacion(),
                items));
    }
}
