package main.web.services.fitsense.planning.interfaces.rest.resources;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/** P-02: una exclusion por dolor tal como la ve el propio usuario. Sin datos clinicos. */
public record PainExclusionResource(Long exclusionId, Long exerciseId, String exerciseName, String reason,
                                    LocalDate eventDate, Long sourcePlannedWorkoutId, String status,
                                    OffsetDateTime recordedAt, OffsetDateTime reactivatedAt) {}
