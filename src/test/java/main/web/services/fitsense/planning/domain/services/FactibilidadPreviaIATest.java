package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams;
import main.web.services.fitsense.planning.domain.model.valueobjects.AdjustmentType;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.DerivedWorkout;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanningProfile;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.planning.domain.model.valueobjects.SafetyProfile;
import main.web.services.fitsense.planning.domain.model.valueobjects.WeekDerivation;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * La factibilidad previa solo evita la llamada cuando DEMUESTRA que ninguna propuesta cabe
 * (orden de optimizacion del 31/10/2026, fase C). Ante la duda, la IA se llama.
 */
class FactibilidadPreviaIATest {

    private static final LocalDate LUNES = LocalDate.of(2026, 10, 19);
    private static final PrescriptionParams PARAMS =
            new PrescriptionParams(null, null, null, null, null, null, null, null, null, null);

    // Tres estructurales de muslo con slot y dos rellenos sin slot (como U01 S2).
    private static final List<CandidateExercise> CATALOGO = List.of(
            ej(1, "upper legs", "KNEE_DOMINANT"), ej(2, "upper legs", "HIP_HINGE"),
            ej(3, "upper legs", "KNEE_DOMINANT"), ej(4, "lower legs", null), ej(5, "waist", null));

    @Test
    @DisplayName("GAIN_MUSCLE: acortar a 10 min una sesion de cinco es imposible y no se llama")
    void gainMuscleImposible() {
        var motivo = FactibilidadPreviaIA.imposibilidad(
                contexto("GAIN_MUSCLE", acortar(102L, 10), List.of((short) 1, (short) 3, (short) 5)), semana());

        assertTrue(motivo.isPresent(), "con 10 min y solo retirar un relleno no hay propuesta posible");
        assertTrue(motivo.get().contains("solo retirar un relleno"), motivo.get());
    }

    @Test
    @DisplayName("GAIN_MUSCLE: si retirar un relleno cabe, la IA se llama")
    void gainMuscleFactible() {
        int conCinco = new SessionDurationEstimator().estimateMinutes(sesion(LUNES.plusDays(2)), PARAMS);
        var motivo = FactibilidadPreviaIA.imposibilidad(
                contexto("GAIN_MUSCLE", acortar(102L, conCinco - 1), List.of((short) 1, (short) 3, (short) 5)),
                semana());

        assertTrue(motivo.isEmpty(), () -> "retirar un relleno ahorra mas de un minuto: " + motivo);
    }

    @Test
    @DisplayName("Por capacidades, bajar la dosis tambien cuenta: el limite que eso alcanza no se bloquea")
    void capacidadesBajandoDosis() {
        // Con 3x10 a 2x10 en las cinco la sesion baja mucho mas que retirando una sola.
        var bajada = new PlanDraft.DraftWorkout(LUNES.plusDays(2), WorkoutFocus.FULL_BODY, "B", 45,
                List.of(ej2x10(1), ej2x10(2), ej2x10(3), ej2x10(4), ej2x10(5)));
        int minutos = new SessionDurationEstimator().estimateMinutes(bajada, PARAMS);
        var motivo = FactibilidadPreviaIA.imposibilidad(
                contexto("GENERAL_FITNESS", acortar(102L, minutos), List.of((short) 1, (short) 3, (short) 5)),
                semana());

        assertTrue(motivo.isEmpty(), () -> "la reduccion de dosis cabe: " + motivo);
    }

    @Test
    @DisplayName("Sin minutos declarados no hay nada que demostrar: se llama")
    void sinMinutos() {
        var orden = orden(new PlanAdjustment.TargetedSession(102L, TipoIntervencion.ACORTAR_O_REORGANIZAR));
        assertTrue(FactibilidadPreviaIA.imposibilidad(
                contexto("GAIN_MUSCLE", orden, List.of((short) 1, (short) 3, (short) 5)), semana()).isEmpty());
    }

    @Test
    @DisplayName("REPROGRAMAR sin otra fecha disponible libre es imposible")
    void reprogramarSinFecha() {
        var orden = orden(new PlanAdjustment.TargetedSession(102L, TipoIntervencion.REPROGRAMAR_DIAS));
        var motivo = FactibilidadPreviaIA.imposibilidad(
                contexto("GAIN_MUSCLE", orden, List.of((short) 1, (short) 3, (short) 5)), semana());

        assertTrue(motivo.isPresent(), "lunes y viernes estan ocupados y el miercoles es la suya");
    }

    @Test
    @DisplayName("REPROGRAMAR con un dia disponible libre llama a la IA")
    void reprogramarConFecha() {
        var orden = orden(new PlanAdjustment.TargetedSession(102L, TipoIntervencion.REPROGRAMAR_DIAS));
        assertTrue(FactibilidadPreviaIA.imposibilidad(
                contexto("GAIN_MUSCLE", orden, List.of((short) 1, (short) 3, (short) 5, (short) 6)),
                semana()).isEmpty());
    }

    @Test
    @DisplayName("Si una sola de las senaladas puede salir, la IA se llama")
    void unaPosibleBasta() {
        var orden = orden(new PlanAdjustment.TargetedSession(102L, TipoIntervencion.ACORTAR_O_REORGANIZAR, 10),
                new PlanAdjustment.TargetedSession(103L, TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA));
        assertTrue(FactibilidadPreviaIA.imposibilidad(
                contexto("GAIN_MUSCLE", orden, List.of((short) 1, (short) 3, (short) 5)), semana()).isEmpty());
    }

    private static PlanAdjustment acortar(long id, int minutos) {
        return orden(new PlanAdjustment.TargetedSession(id, TipoIntervencion.ACORTAR_O_REORGANIZAR, minutos));
    }

    private static PlanAdjustment orden(PlanAdjustment.TargetedSession... sesiones) {
        return new PlanAdjustment(List.of(AdjustmentType.NONE), null, null, null, 0.0, 0.0, null, null, null,
                "falta de tiempo", null, null, List.of(sesiones));
    }

    private static PlanGenerationContext contexto(String objetivo, PlanAdjustment orden, List<Short> dias) {
        var perfil = new PlanningProfile(1L, 30, "M", new BigDecimal("175"), new BigDecimal("75"),
                new BigDecimal("72"), "INTERMEDIATE", objetivo, "objetivo", null,
                "GYM", dias.size(), dias, 45, List.of("body weight"), List.of(),
                SafetyProfile.Declaradas.sinLimitaciones());
        return new PlanGenerationContext(1L, (short) 2, LUNES, LUNES.plusDays(6), perfil, orden, null,
                CATALOGO, null, PARAMS);
    }

    private static WeekDerivation semana() {
        return new WeekDerivation(List.of(
                new DerivedWorkout(101L, sesion(LUNES)),
                new DerivedWorkout(102L, sesion(LUNES.plusDays(2))),
                new DerivedWorkout(103L, sesion(LUNES.plusDays(4)))), List.of());
    }

    private static PlanDraft.DraftWorkout sesion(LocalDate fecha) {
        return new PlanDraft.DraftWorkout(fecha, WorkoutFocus.FULL_BODY, "Sesion", 45,
                List.of(ej3x10(1), ej3x10(2), ej3x10(3), ej3x10(4), ej3x10(5)));
    }

    private static PlanDraft.DraftExercise ej3x10(long id) {
        return new PlanDraft.DraftExercise(id, PrescriptionType.SETS_REPS, (short) 3, (short) 10, null, null,
                (short) 90, null);
    }

    private static PlanDraft.DraftExercise ej2x10(long id) {
        return new PlanDraft.DraftExercise(id, PrescriptionType.SETS_REPS, (short) 2, (short) 10, null, null,
                (short) 90, null);
    }

    private static CandidateExercise ej(long id, String grupo, String patron) {
        return new CandidateExercise(id, "ejercicio " + id, grupo, "body weight", 1, PrescriptionType.SETS_REPS,
                null, patron);
    }
}
