package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LA IA NUNCA SUBE DOSIS, TAMPOCO A TRAVES DE UN SUSTITUTO DE SEGURIDAD (auditoria de activacion, O12).
 * <p>
 * {@link AdaptedWeekProducer#subidasPorSustitucion} compara la dosis publicada de cada sustituto SUST-SEG-1 con la que
 * {@link SafetySubstitutionPolicy#dosis} le daria partiendo de la semana MEDIDA. Cubre las tres magnitudes (series,
 * repeticiones y segundos) y comprueba que una bajada o la misma dosis siguen valiendo.
 */
@DisplayName("O12 · un sustituto de seguridad no puede subir series, repeticiones ni segundos")
class SubidaPorSustitucionTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 11, 2);
    private static final long S = 901L;       // sesion con dolor
    private static final long QUITADO = 991L; // ejercicio que dolio
    private static final long PUESTO = 72L;   // sustituto

    @Test
    @DisplayName("sube series (3x18 -> 4x18): se rechaza")
    void subeSeries() {
        var p = revisar("R", 3, 18, "R", 4, 18);
        assertEquals(1, p.size(), p.toString());
        assertTrue(p.get(0).contains("sube la dosis a traves del sustituto 72 de 991"), p.toString());
        assertTrue(p.get(0).contains("4x18") && p.get(0).contains("3x18"), p.toString());
    }

    @Test
    @DisplayName("sube repeticiones (3x18 -> 3x20): se rechaza")
    void subeRepeticiones() {
        var p = revisar("R", 3, 18, "R", 3, 20);
        assertEquals(1, p.size(), p.toString());
        assertTrue(p.get(0).contains("sube la dosis"), p.toString());
    }

    @Test
    @DisplayName("sube segundos de trabajo (3x60s -> 3x90s): se rechaza")
    void subeSegundos() {
        var p = revisar("D", 3, 60, "D", 3, 90);
        assertEquals(1, p.size(), p.toString());
        assertTrue(p.get(0).contains("3x90s") && p.get(0).contains("3x60s"), p.toString());
    }

    @Test
    @DisplayName("control: la misma dosis que la medida es valida")
    void mismaDosis() {
        assertEquals(List.of(), revisar("R", 3, 18, "R", 3, 18));
        assertEquals(List.of(), revisar("D", 3, 60, "D", 3, 60));
    }

    @Test
    @DisplayName("control: bajar series, repeticiones o segundos en el sustituto sigue siendo valido")
    void bajadasValidas() {
        assertEquals(List.of(), revisar("R", 3, 18, "R", 2, 18));
        assertEquals(List.of(), revisar("R", 3, 18, "R", 3, 15));
        assertEquals(List.of(), revisar("D", 3, 60, "D", 3, 45));
    }

    @Test
    @DisplayName("control: sin dosis medida del ejercicio quitado no se inventa un rechazo")
    void sinDosisMedida() {
        var semana = List.of(new DerivedWorkout(S, sesion(ej(PUESTO, "R", 4, 18))));
        var sust = List.of(sustitucion());
        var ctx = contexto(new PreviousWeekSummary(new BigDecimal("50.00"), new BigDecimal("6.0"), null, Map.of(),
                List.of(), List.of(), List.of()));
        assertEquals(List.of(), AdaptedWeekProducer.subidasPorSustitucion(semana, sust, ctx, catalogo("R")));
    }

    @Test
    @DisplayName("control: sin sustituciones no hay nada que revisar")
    void sinSustituciones() {
        var semana = List.of(new DerivedWorkout(S, sesion(ej(PUESTO, "R", 4, 18))));
        assertEquals(List.of(), AdaptedWeekProducer.subidasPorSustitucion(semana, List.of(),
                contexto(previa("R", 3, 18)), catalogo("R")));
    }

    // =======================================================================

    private static List<String> revisar(String tipoMedido, int seriesMedidas, int valorMedido,
                                        String tipoPublicado, int seriesPublicadas, int valorPublicado) {
        var semana = List.of(new DerivedWorkout(S,
                sesion(ej(PUESTO, tipoPublicado, seriesPublicadas, valorPublicado))));
        return AdaptedWeekProducer.subidasPorSustitucion(semana, List.of(sustitucion()),
                contexto(previa(tipoMedido, seriesMedidas, valorMedido)), catalogo(tipoPublicado));
    }

    private static SustitucionSeguridad sustitucion() {
        return new SustitucionSeguridad(S, LUNES.plusWeeks(1), QUITADO, "CORE", PUESTO, "dolor",
                "SUST-SEG-1", null, null);
    }

    private static Map<Long, CandidateExercise> catalogo(String tipo) {
        var pt = tipo.equals("D") ? PrescriptionType.DURATION : PrescriptionType.SETS_REPS;
        return Map.of(PUESTO, new CandidateExercise(PUESTO, "ej" + PUESTO, "waist", "body weight", 1, pt,
                "abs", "CORE"));
    }

    /** Sin perfil ni parametros de prescripcion: SUST-SEG-1 conserva series y repeticiones (o segundos). */
    private static PlanGenerationContext contexto(PreviousWeekSummary previa) {
        return new PlanGenerationContext(1L, (short) 2, LUNES.plusWeeks(1), LUNES.plusWeeks(1).plusDays(6),
                null, null, previa, List.of(), null, null);
    }

    private static PlanDraft.DraftExercise ej(long id, String tipo, int series, int valor) {
        boolean d = tipo.equals("D");
        return new PlanDraft.DraftExercise(id, d ? PrescriptionType.DURATION : PrescriptionType.SETS_REPS,
                (short) series, d ? null : (short) valor, d ? valor : null, null, (short) 45, null);
    }

    private static PlanDraft.DraftWorkout sesion(PlanDraft.DraftExercise e) {
        return new PlanDraft.DraftWorkout(LUNES.plusWeeks(1), WorkoutFocus.FULL_BODY, "Sesion " + S, 40, List.of(e));
    }

    private static PreviousWeekSummary previa(String tipo, int series, int valor) {
        boolean d = tipo.equals("D");
        var sesiones = List.of(new PreviousWeekSummary.WorkoutOutcome(S, LUNES, "FULL_BODY", "SKIPPED",
                "PAIN_OR_DISCOMFORT", true, "SKIPPED", null, BigDecimal.ZERO, "Sesion " + S, (short) 40));
        var prescritas = List.of(new PreviousWeekSummary.PrescriptionOutcome(S, LUNES, QUITADO, "ejercicio " + QUITADO,
                d ? "DURATION" : "SETS_REPS", (short) series, d ? null : (short) valor, d ? valor : null, null,
                (short) 0, null, null, null, BigDecimal.ZERO, "SKIPPED", null, "SKIPPED", (short) 45));
        return new PreviousWeekSummary(new BigDecimal("0.00"), new BigDecimal("6.0"), null, Map.of(), sesiones,
                prescritas, List.of());
    }
}
