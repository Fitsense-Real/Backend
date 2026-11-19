package main.web.services.fitsense.administration.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import main.web.services.fitsense.administration.application.internal.queryservices.AdminAnalyticsQueryService;
import main.web.services.fitsense.administration.application.internal.queryservices.AdminAnalyticsQueryService.Segmentacion;
import main.web.services.fitsense.administration.interfaces.rest.resources.*;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Analitica global para el panel Angular. Solo ADMIN (la ruta y el metodo) y
 * solo cifras agregadas: ninguna respuesta lleva nombre, correo ni participantCode.
 */
@RestController
@RequestMapping(value = "/api/v1/admin/analytics", produces = "application/json")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administracion · analitica", description = "Adherencia agregada del estudio (ADMIN)")
public class AdminAnalyticsController {

    private final AdminAnalyticsQueryService queryService;

    public AdminAnalyticsController(AdminAnalyticsQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/overview")
    @Operation(summary = "Inscripcion, semanas medidas, adherencia oficial y riesgo de la ultima semana")
    public ResponseEntity<AnalyticsOverviewResource> overview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(queryService.overview(AnalyticsFilters.period(from, to)));
    }

    @GetMapping("/adherence")
    @Operation(summary = "Distribucion de cada indicador semanal guardado")
    public ResponseEntity<AdherenceDistributionResource> adherence(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) GoalType goalType,
            @RequestParam(required = false) FitnessLevel fitnessLevel,
            @RequestParam(required = false) TrainingLocation trainingLocation) {
        return ResponseEntity.ok(queryService.adherence(
                new AnalyticsFilters(from, to, goalType, fitnessLevel, trainingLocation)));
    }

    @GetMapping("/adherence/trends")
    @Operation(summary = "Serie semanal de la adherencia oficial, la cobertura y el riesgo")
    public ResponseEntity<AdherenceTrendResource> trends(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) GoalType goalType,
            @RequestParam(required = false) FitnessLevel fitnessLevel,
            @RequestParam(required = false) TrainingLocation trainingLocation) {
        return ResponseEntity.ok(queryService.trends(
                new AnalyticsFilters(from, to, goalType, fitnessLevel, trainingLocation)));
    }

    @GetMapping("/segments")
    @Operation(summary = "Adherencia oficial por objetivo, nivel, lugar o nivel de riesgo")
    public ResponseEntity<SegmentsResource> segments(
            @RequestParam Segmentacion by,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) GoalType goalType,
            @RequestParam(required = false) FitnessLevel fitnessLevel,
            @RequestParam(required = false) TrainingLocation trainingLocation) {
        return ResponseEntity.ok(queryService.segments(
                new AnalyticsFilters(from, to, goalType, fitnessLevel, trainingLocation), by));
    }
}
