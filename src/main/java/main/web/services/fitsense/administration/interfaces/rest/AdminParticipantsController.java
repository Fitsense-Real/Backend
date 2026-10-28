package main.web.services.fitsense.administration.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import main.web.services.fitsense.administration.application.internal.queryservices.AdminAnalyticsQueryService;
import main.web.services.fitsense.administration.infrastructure.audit.AdminAudit;
import main.web.services.fitsense.administration.interfaces.rest.resources.ParticipantAdherenceResource;
import main.web.services.fitsense.administration.interfaces.rest.resources.ParticipantDetailResource;
import main.web.services.fitsense.administration.interfaces.rest.resources.ParticipantPageResource;
import main.web.services.fitsense.iam.domain.model.valueobjects.UserStatus;
import main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import main.web.services.fitsense.shared.domain.exceptions.ResourceNotFoundException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Participantes para el panel Angular (ADMIN). El listado y la serie de adherencia
 * van seudonimizados; nombre y correo solo aparecen en el detalle, y cada consulta
 * del detalle se registra como acceso a datos personales.
 */
@RestController
@RequestMapping(value = "/api/v1/admin/participants", produces = "application/json")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administracion · participantes", description = "Consulta de participantes (ADMIN)")
public class AdminParticipantsController {

    static final int MAX_PAGE_SIZE = 200;

    private final AdminAnalyticsQueryService queryService;

    public AdminParticipantsController(AdminAnalyticsQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    @Operation(summary = "Listado paginado y seudonimizado, con la ultima semana medida")
    public ResponseEntity<ParticipantPageResource> list(
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String studyStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        if (page < 0) throw new IllegalArgumentException("page no puede ser negativo.");
        if (size < 1 || size > MAX_PAGE_SIZE)
            throw new IllegalArgumentException("size debe estar entre 1 y " + MAX_PAGE_SIZE + ".");
        return ResponseEntity.ok(queryService.participants(
                status == null ? null : status.name(), studyStatus, page, size));
    }

    @GetMapping("/{participantId}")
    @Operation(summary = "Detalle con nombre y correo (acceso auditado)")
    public ResponseEntity<ParticipantDetailResource> detail(
            @AuthenticationPrincipal UserDetailsImpl admin,
            @PathVariable Long participantId) {
        var detalle = queryService.participant(participantId)
                .orElseThrow(() -> new ResourceNotFoundException("Participante", participantId));
        AdminAudit.personalDataViewed(admin.getUserId(), participantId);
        return ResponseEntity.ok(detalle);
    }

    @GetMapping("/{participantId}/adherence")
    @Operation(summary = "Semanas medidas de un participante, sin datos personales")
    public ResponseEntity<ParticipantAdherenceResource> adherence(
            @PathVariable Long participantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return queryService.participantAdherence(participantId, from, to)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Participante", participantId));
    }
}
