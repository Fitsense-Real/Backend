package main.web.services.fitsense.planning.application.internal.queryservices;

import main.web.services.fitsense.planning.application.internal.commandservices.PainMemoryService;
import main.web.services.fitsense.planning.application.internal.commandservices.SafetyConflictChecker;
import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalProfilingService;
import main.web.services.fitsense.planning.domain.model.entities.PlannedWorkout;
import main.web.services.fitsense.planning.domain.model.entities.PlannedWorkoutExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanningProfile;
import main.web.services.fitsense.planning.domain.model.valueobjects.SafetyProfile;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutStatus;
import main.web.services.fitsense.planning.interfaces.acl.PlannedWorkoutView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * P-DOLOR-PUB · casos de la proteccion por dolor en planes publicados que no dependen de la base (cierre tecnico
 * conservador, VALIDACION_DOLOR_PUBLICADO.md, casos 2, 4, 9 y 10).
 */
@DisplayName("P-DOLOR-PUB: el dolor se suma a las otras causas, no las sustituye, y solo en sesiones abiertas")
class PublishedPlanSafetyServiceDolorTest {

    static final long USUARIO = 7L;
    static final long CON_DOLOR = 100L, CON_DOLOR_Y_SUELO = 200L, SIN_NADA = 300L;

    final ExternalProfilingService profiling = mock(ExternalProfilingService.class);
    final SafetyConflictChecker checker = mock(SafetyConflictChecker.class);
    final PainMemoryService dolor = mock(PainMemoryService.class);

    PublishedPlanSafetyService servicio(boolean encendida) {
        when(profiling.fetchProfile(USUARIO)).thenReturn(Optional.of(perfil()));
        // 200 ya choca hoy con una restriccion declarada (sin suelo); 100 y 300 no.
        when(checker.motivos(any(), any(), any())).thenReturn(Map.of(CON_DOLOR_Y_SUELO, "requires_floor"));
        when(dolor.excluidosActivos(USUARIO)).thenReturn(Set.of(CON_DOLOR, CON_DOLOR_Y_SUELO));
        var s = new PublishedPlanSafetyService(profiling, checker);
        s.setPainMemory(dolor);
        org.springframework.test.util.ReflectionTestUtils.setField(s, "dolorEnPlanesPublicados", encendida);
        return s;
    }

    @Test
    @DisplayName("caso 9: el ya bloqueado por otra causa conserva esa causa y sigue bloqueando la sesion; el resto, intacto")
    void seSumaALaOtraCausa() {
        var conflictos = servicio(true).conflictos(USUARIO, sesion(WorkoutStatus.SCHEDULED));
        assertEquals(Map.of(1L, "dolor_registrado", 2L, "requires_floor+dolor_registrado"), conflictos,
                "caso 2: el ejercicio sin dolor ni restriccion (3) no se marca");
        assertTrue(vista(conflictos).blockedForSafety(), "la otra causa sigue bloqueando la sesion entera");
        assertFalse(vista(Map.of(1L, "dolor_registrado")).blockedForSafety(), "solo por dolor no se bloquea entera");
    }

    @Test
    @DisplayName("apagada: exactamente las causas de antes (la proteccion no quita ni anade otras restricciones)")
    void apagada() {
        var conflictos = servicio(false).conflictos(USUARIO, sesion(WorkoutStatus.SCHEDULED));
        assertEquals(Map.of(2L, "requires_floor"), conflictos);
        verifyNoInteractions(dolor);
    }

    @ParameterizedTest
    @EnumSource(value = WorkoutStatus.class, names = {"COMPLETED", "PARTIAL", "REPLACED"})
    @DisplayName("caso 4: una sesion terminada es historia: no se marca ni admite registros nuevos")
    void terminadas(WorkoutStatus estado) {
        var s = servicio(true);
        assertEquals(Map.of(), s.conflictos(USUARIO, sesion(estado)));
        assertEquals(Map.of(), s.conflictosParaRegistrar(USUARIO, sesion(estado)));
    }

    @Test
    @DisplayName("caso 5: en curso se marca igual que programada; un SKIPPED se marca para el reporte retroactivo")
    void enCursoYOmitida() {
        var s = servicio(true);
        var programada = s.conflictos(USUARIO, sesion(WorkoutStatus.SCHEDULED));
        assertEquals(programada, s.conflictos(USUARIO, sesion(WorkoutStatus.IN_PROGRESS)));
        assertEquals(Map.of(), s.conflictos(USUARIO, sesion(WorkoutStatus.SKIPPED)), "la lectura del plan no la marca");
        assertEquals(programada, s.conflictosParaRegistrar(USUARIO, sesion(WorkoutStatus.SKIPPED)),
                "registrar a posteriori no puede anotar trabajo en lo que dolio");
    }

    @Test
    @DisplayName("caso 10: leer varias veces da lo mismo y no escribe nada")
    void idempotente() {
        var s = servicio(true);
        var primera = s.conflictos(USUARIO, sesion(WorkoutStatus.SCHEDULED));
        for (int i = 0; i < 3; i++) assertEquals(primera, s.conflictos(USUARIO, sesion(WorkoutStatus.SCHEDULED)));
        verify(dolor, times(4)).excluidosActivos(USUARIO);
        verifyNoMoreInteractions(dolor);
    }

    private static PlannedWorkout sesion(WorkoutStatus estado) {
        var w = mock(PlannedWorkout.class);
        when(w.getStatus()).thenReturn(estado);
        var ejercicios = List.of(ejercicio(1L, CON_DOLOR), ejercicio(2L, CON_DOLOR_Y_SUELO), ejercicio(3L, SIN_NADA));
        when(w.exercisesView()).thenReturn(ejercicios);
        return w;
    }

    private static PlannedWorkoutExercise ejercicio(long prescripcion, long exerciseId) {
        var e = mock(PlannedWorkoutExercise.class);
        when(e.getId()).thenReturn(prescripcion);
        when(e.getExerciseId()).thenReturn(exerciseId);
        return e;
    }

    private static PlannedWorkoutView vista(Map<Long, String> conflictos) {
        return new PlannedWorkoutView(1L, 1L, USUARIO, LocalDate.of(2026, 11, 16), "FULL_BODY", "sesion", (short) 45,
                "SCHEDULED", null, List.of(), conflictos);
    }

    private static PlanningProfile perfil() {
        return new PlanningProfile(USUARIO, 30, "MALE", new BigDecimal("175.0"), new BigDecimal("72.0"),
                new BigDecimal("75.0"), "BEGINNER", "GAIN_MUSCLE", "objetivo", "Ninguna", "HOME", 2,
                List.of((short) 1, (short) 3), 45, List.of("body weight"), List.of(),
                SafetyProfile.Declaradas.sinLimitaciones());
    }
}
