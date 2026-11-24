package main.web.services.fitsense.planning.interfaces.rest.resources;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * P-02: una exclusion por dolor tal como la ve el propio usuario. Sin datos clinicos.
 * <p>
 * O20 (campos anadidos al final, compatibles): {@code confirmed} es false en un dolor de sesion que aun no se ha
 * identificado (un reporte general) y true si la persona marco ese ejercicio o lo identifico despues
 * ({@code identifiedAt}).
 */
public record PainExclusionResource(Long exclusionId, Long exerciseId, String exerciseName, String reason,
                                    LocalDate eventDate, Long sourcePlannedWorkoutId, String status,
                                    OffsetDateTime recordedAt, OffsetDateTime reactivatedAt,
                                    Boolean confirmed, OffsetDateTime identifiedAt) {}
