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
}
