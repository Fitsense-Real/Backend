package main.web.services.fitsense.execution;

import main.web.services.fitsense.execution.application.internal.commandservices.WorkoutSessionCommandServiceImpl;
import main.web.services.fitsense.execution.application.internal.outboundservices.acl.ExternalConfigurationService;
import main.web.services.fitsense.execution.application.internal.outboundservices.acl.ExternalPlanningService;
import main.web.services.fitsense.execution.domain.exceptions.WorkoutNotAvailableException;
import main.web.services.fitsense.execution.domain.model.aggregates.WorkoutSession;
import main.web.services.fitsense.execution.domain.model.commands.RecordSessionExerciseCommand;
import main.web.services.fitsense.execution.domain.model.commands.ReportWorkoutCommand;
import main.web.services.fitsense.execution.domain.model.valueobjects.CompletionThresholds;
import main.web.services.fitsense.execution.domain.model.valueobjects.ExerciseTarget;
import main.web.services.fitsense.execution.infrastructure.persistence.jpa.repositories.WorkoutSessionRepository;
import main.web.services.fitsense.planning.domain.model.entities.PlannedWorkout;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutStatus;
import main.web.services.fitsense.planning.interfaces.acl.PlannedExerciseTarget;
import main.web.services.fitsense.planning.interfaces.acl.PlannedWorkoutView;
import main.web.services.fitsense.shared.domain.exceptions.DomainRuleViolationException;
import main.web.services.fitsense.shared.domain.model.valueobjects.SkipReason;
import main.web.services.fitsense.shared.interfaces.rest.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Reglas de la ejecucion corregidas en la auditoria, sin base de datos. */
class EjecucionReglasTest {

    static final CompletionThresholds UMBRALES = new CompletionThresholds(80, 30, 80, 100);

    @Nested
    @DisplayName("omitir un entrenamiento")
    class Omitir {

        private PlannedWorkout entrenamiento() {
            return new PlannedWorkout(null, LocalDate.now(), WorkoutFocus.values()[0], "Prueba", 45, (short) 1,
                    OffsetDateTime.now().plusDays(1));
        }

        @Test
        @DisplayName("SCHEDULED, IN_PROGRESS y SKIPPED sin causa admiten omitirse")
        void admitidos() {
            var w = entrenamiento();
            w.markSkipped(SkipReason.FATIGUE);
            assertEquals(WorkoutStatus.SKIPPED, w.getStatus());

            var enCurso = entrenamiento();
            enCurso.markInProgress();
            enCurso.markSkipped(SkipReason.LACK_OF_TIME);
            assertEquals(WorkoutStatus.SKIPPED, enCurso.getStatus());

            var vencido = entrenamiento();
            vencido.markOutcome(WorkoutStatus.SKIPPED, null);
            vencido.markSkipped(SkipReason.SCHEDULE_CHANGE);
            assertEquals(SkipReason.SCHEDULE_CHANGE, vencido.getSkipReason());
        }

        @Test
        @DisplayName("COMPLETED, PARTIAL y REPLACED no: tienen una sesion que cuenta o ya no rigen")
        void rechazados() {
            for (var cerrado : List.of(WorkoutStatus.COMPLETED, WorkoutStatus.PARTIAL)) {
                var w = entrenamiento();
                w.markOutcome(cerrado, null);
                assertThrows(DomainRuleViolationException.class, () -> w.markSkipped(SkipReason.FATIGUE));
                assertEquals(cerrado, w.getStatus());
                assertNull(w.getSkipReason());
            }
            var reemplazado = entrenamiento();
            reemplazado.markReplaced();
            assertThrows(DomainRuleViolationException.class, () -> reemplazado.markSkipped(SkipReason.FATIGUE));
            assertEquals(WorkoutStatus.REPLACED, reemplazado.getStatus());
        }
    }

    @Nested
    @DisplayName("ejercicio inseguro y reporte")
    class Servicio {

        final WorkoutSessionRepository repositorio = mock(WorkoutSessionRepository.class);
        final ExternalPlanningService planning = mock(ExternalPlanningService.class);
        final ExternalConfigurationService configuracion = mock(ExternalConfigurationService.class);
        final WorkoutSessionCommandServiceImpl servicio =
                new WorkoutSessionCommandServiceImpl(repositorio, planning, configuracion);

        static final long USUARIO = 7L, ENTRENAMIENTO = 70L, INSEGURO = 701L, SESION = 9L;

        private PlannedWorkoutView vista(String estado, Map<Long, String> conflictos) {
            return new PlannedWorkoutView(ENTRENAMIENTO, 1L, USUARIO, LocalDate.now(), "FULL_BODY", "Prueba",
                    (short) 45, estado, OffsetDateTime.now().plusDays(1),
                    List.of(new PlannedExerciseTarget(INSEGURO, 1374L, "SETS_REPS", (short) 3, (short) 10, null)),
                    conflictos);
        }

        private void preparar(String estado) {
            var v = vista(estado, Map.of(INSEGURO, "INC-CAT-1374"));
            when(planning.fetchPlannedWorkout(ENTRENAMIENTO)).thenReturn(Optional.of(v));
            when(planning.targetsOf(any())).thenReturn(List.of(new ExerciseTarget(INSEGURO, 1374L, false, 30, null)));
            when(configuracion.thresholds()).thenReturn(UMBRALES);
            when(configuracion.retroactiveReportDays()).thenReturn(7);
            var sesion = WorkoutSession.start(USUARIO, ENTRENAMIENTO, 1L, (short) 1);
            ReflectionTestUtils.setField(sesion, "id", SESION);
            when(repositorio.findByIdForUpdate(SESION)).thenReturn(Optional.of(sesion));
            when(repositorio.save(any())).thenAnswer(i -> i.getArgument(0));
        }

        @Test
        @DisplayName("con causa y trabajo se rechaza; sin causa tambien; con causa y cero, se acepta")
        void registroDelInseguro() {
            preparar("IN_PROGRESS");
            for (var trabajo : List.of(new Object[]{(short) 3, 30, null}, new Object[]{(short) 0, 0, 20},
                    new Object[]{(short) 1, null, null}))
                assertThrows(WorkoutNotAvailableException.class, () -> servicio.handle(new RecordSessionExerciseCommand(
                        USUARIO, SESION, INSEGURO, (Short) trabajo[0], (Integer) trabajo[1], (Integer) trabajo[2],
                        null, SkipReason.PAIN_OR_DISCOMFORT)));
            assertThrows(WorkoutNotAvailableException.class, () -> servicio.handle(new RecordSessionExerciseCommand(
                    USUARIO, SESION, INSEGURO, (short) 0, 0, null, null, null)));

            var s = servicio.handle(new RecordSessionExerciseCommand(USUARIO, SESION, INSEGURO, (short) 0, 0, null,
                    null, SkipReason.PAIN_OR_DISCOMFORT)).orElseThrow();
            assertEquals("SKIPPED", s.findExercise(INSEGURO).orElseThrow().getStatus().name());
        }

        @Test
        @DisplayName("reportar sobre un entrenamiento REPLACED se rechaza antes de crear nada")
        void reporteSobreReemplazado() {
            preparar("REPLACED");
            assertThrows(WorkoutNotAvailableException.class, () -> servicio.handle(new ReportWorkoutCommand(
                    USUARIO, ENTRENAMIENTO, OffsetDateTime.now(), (short) 7, null, null,
                    List.of(new ReportWorkoutCommand.ReportedExercise(INSEGURO, (short) 0, 0, null, null,
                            SkipReason.OTHER)))));
            verify(repositorio, never()).saveAndFlush(any());
        }
    }

    @Test
    @DisplayName("una restriccion de la base por peticiones simultaneas responde 409, no 500")
    void conflictoEs409() {
        var r = new GlobalExceptionHandler().handleConflict(new DataIntegrityViolationException("ux_session_active"));
        assertEquals(HttpStatus.CONFLICT, r.getStatusCode());
        assertFalse(r.getBody().message().contains("ux_session_active"), "no expone la restriccion");
    }
}
