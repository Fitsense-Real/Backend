package main.web.services.fitsense.planning.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import main.web.services.fitsense.planning.application.internal.commandservices.PainMemoryService;
import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalCatalogService;
import main.web.services.fitsense.planning.interfaces.rest.resources.EffectivePainExclusionResource;
import main.web.services.fitsense.planning.interfaces.rest.resources.IdentifyPainExclusionResource;
import main.web.services.fitsense.planning.interfaces.rest.resources.PainConfirmationRequestResource;
import main.web.services.fitsense.planning.interfaces.rest.resources.PainExclusionResource;
import main.web.services.fitsense.planning.interfaces.rest.resources.ReactivatePainExclusionResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * P-02 · memoria persistente del dolor, del lado del usuario: ver sus exclusiones y reactivar una de forma
 * explicita. Siempre sobre {@code /users/me}: nadie ve ni reactiva las de otro.
 */
@RestController
@RequestMapping(value = "/api/v1", produces = "application/json")
@Tag(name = "Pain exclusions", description = "Ejercicios excluidos por dolor (P-02)")
public class PainExclusionController {

    private final PainMemoryService painMemory;
    private final ExternalCatalogService catalog;

    public PainExclusionController(PainMemoryService painMemory, ExternalCatalogService catalog) {
        this.painMemory = painMemory;
        this.catalog = catalog;
    }

    @GetMapping("/users/me/pain-exclusions")
    @Operation(summary = "Ejercicios excluidos por dolor, activos y reactivados")
    public ResponseEntity<List<PainExclusionResource>> list(@AuthenticationPrincipal UserDetailsImpl principal) {
        var filas = painMemory.historial(principal.getUserId());
        var nombres = catalog.fetchNames(filas.stream().map(x -> x.getExerciseId()).collect(Collectors.toSet()));
        return ResponseEntity.ok(filas.stream().map(x -> new PainExclusionResource(x.getId(), x.getExerciseId(),
                nombres.get(x.getExerciseId()), x.getReason().name(), x.getEventDate(), x.getSourcePlannedWorkoutId(),
                x.getStatus().name(), x.getRecordedAt(), x.getReactivatedAt(), x.confirmada(), x.getIdentifiedAt()))
                .toList());
    }

    /**
     * O20: lo que hoy no se prescribe por dolor, con su origen. Distingue lo registrado para ese ejercicio
     * ({@code DIRECT}) de las variantes equivalentes que se excluyen con el ({@code EQUIVALENT}), y un dolor de sesion
     * sin identificar (no confirmado) de uno confirmado.
     */
    @GetMapping("/users/me/pain-exclusions/effective")
    @Operation(summary = "Ejercicios que hoy no se prescriben por dolor: los registrados y sus variantes equivalentes")
    public ResponseEntity<List<EffectivePainExclusionResource>> effective(
            @AuthenticationPrincipal UserDetailsImpl principal) {
        var efectivas = painMemory.exclusionesEfectivas(principal.getUserId());
        var ids = new java.util.TreeSet<Long>();
        efectivas.forEach(x -> {
            ids.add(x.exerciseId());
            if (x.derivaDe() != null) ids.add(x.derivaDe());
        });
        var nombres = catalog.fetchNames(ids);
        return ResponseEntity.ok(efectivas.stream().map(x -> {
            boolean directa = x.origen() == PainMemoryService.Origen.DIRECTA;
            return new EffectivePainExclusionResource(x.exerciseId(), nombres.get(x.exerciseId()),
                    switch (x.origen()) { case DIRECTA -> "DIRECT"; case EQUIVALENTE -> "EQUIVALENT";
                        case PRECAUCION -> "PRECAUTION"; }, x.confirmada(), x.provisional(), x.requiereConfirmacion(),
                    x.derivaDe(),
                    x.derivaDe() == null ? null : nombres.get(x.derivaDe()), x.nivel(), x.grupo(), x.justificacion(),
                    x.sourcePlannedWorkoutId(), x.eventDate(), directa && !x.confirmada(), directa);
        }).toList());
    }

    @PostMapping("/users/me/pain-exclusions/{exerciseId}/identification")
    @Operation(summary = "Indica despues que este ejercicio de una sesion con dolor fue el que dolio. Su exclusion "
            + "queda confirmada, con sus variantes equivalentes. No reactiva ningun otro ejercicio de la sesion.")
    public ResponseEntity<PainMemoryService.Identificacion> identify(
            @AuthenticationPrincipal UserDetailsImpl principal, @PathVariable Long exerciseId,
            @Valid @RequestBody IdentifyPainExclusionResource resource) {
        return ResponseEntity.ok(painMemory.identificar(principal.getUserId(), exerciseId, resource.plannedWorkoutId(),
                Boolean.TRUE.equals(resource.confirmPain()), resource.unrelatedExerciseIds() == null ? java.util.Set.of()
                        : new java.util.TreeSet<>(resource.unrelatedExerciseIds())));
    }

    /**
     * O20 (cierre final): las sesiones con dolor sin identificar que esperan la confirmacion de la persona. Con
     * {@code confirmationRequired} true ya pasaron dos semanas con plan: Mobile debe pedirla de forma activa. Nada de lo
     * listado vuelve a prescribirse mientras no conteste.
     */
    @GetMapping("/users/me/pain-exclusions/confirmation-requests")
    @Operation(summary = "Sesiones con dolor sin identificar que esperan tu confirmacion: cual fue y cuales no")
    public ResponseEntity<List<PainConfirmationRequestResource>> confirmationRequests(
            @AuthenticationPrincipal UserDetailsImpl principal) {
        var userId = principal.getUserId();
        var pedir = new java.util.TreeSet<Long>();
        painMemory.exclusionesEfectivas(userId).forEach(x -> {
            if (x.origen() == PainMemoryService.Origen.DIRECTA && x.requiereConfirmacion()) pedir.add(x.exerciseId());
        });
        var porSesion = new java.util.TreeMap<Long, java.util.List<main.web.services.fitsense.planning.domain.model
                .aggregates.UserSafetyExclusion>>();
        painMemory.historial(userId).stream().filter(x -> x.activa() && !x.confirmada())
                .forEach(x -> porSesion.computeIfAbsent(x.getSourcePlannedWorkoutId(), k -> new java.util.ArrayList<>())
                        .add(x));
        var ids = new java.util.TreeSet<Long>();
        porSesion.values().forEach(l -> l.forEach(x -> ids.add(x.getExerciseId())));
        var nombres = catalog.fetchNames(ids);
        return ResponseEntity.ok(porSesion.entrySet().stream().map(e -> new PainConfirmationRequestResource(e.getKey(),
                e.getValue().get(0).getEventDate(),
                e.getValue().stream().anyMatch(x -> pedir.contains(x.getExerciseId())),
                e.getValue().stream().map(x -> new PainConfirmationRequestResource.Exercise(x.getExerciseId(),
                        nombres.get(x.getExerciseId()))).distinct().toList())).toList());
    }

    @PostMapping("/users/me/pain-exclusions/{exerciseId}/reactivation")
    @Operation(summary = "Pide volver a considerar un ejercicio excluido por dolor. No lo reincorpora: "
            + "vuelve a ser candidato y sigue sujeto a todas las reglas. Se rechaza si choca con una restriccion "
            + "de seguridad vigente.")
    public ResponseEntity<PainMemoryService.Reactivacion> reactivate(
            @AuthenticationPrincipal UserDetailsImpl principal, @PathVariable Long exerciseId,
            @Valid @RequestBody ReactivatePainExclusionResource resource) {
        return ResponseEntity.ok(painMemory.reactivar(principal.getUserId(), exerciseId,
                Boolean.TRUE.equals(resource.confirmNoPain()), resource.comment()));
    }
}
