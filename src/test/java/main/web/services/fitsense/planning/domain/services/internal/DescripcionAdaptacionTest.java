package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.DescripcionAdaptacion.Cambio;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Textos de justificacion verificables (solo presentacion): el mensaje describe lo que cambio de verdad, con los
 * datos reales del catalogo V44_2 de U04, R01, R03 y R05.
 */
@DisplayName("Justificacion de la adaptacion: solo afirma lo que el sistema registra")
class DescripcionAdaptacionTest {

    static final LocalDate L1 = LocalDate.of(2026, 11, 2), L2 = L1.plusWeeks(1);
    static final long S = 20L;
    static final String MENSAJE = "Cumpliste el 33 % de tu plan. Nos dijiste que resultaba dificil, asi que bajamos la exigencia.";

    static CandidateExercise c(long id, String patron, int dif) {
        return new CandidateExercise(id, "e" + id, "x", "body weight", dif, PrescriptionType.SETS_REPS, "m", patron);
    }

    static final List<CandidateExercise> CAT = List.of(
            c(380, "HORIZONTAL_PUSH", 2), c(389, "VERTICAL_PULL", 2), c(72, "HORIZONTAL_PUSH", 1),
            c(74, "VERTICAL_PULL", 1), c(56, "HORIZONTAL_PUSH", 2), c(360, "HIP_HINGE", 2),
            // R01 (todo dificultad 1)
            c(933, "HORIZONTAL_PUSH", 1), c(129, "HORIZONTAL_PUSH", 1), c(143, "VERTICAL_PULL", 1),
            c(114, "VERTICAL_PULL", 1), c(1195, "KNEE_DOMINANT", 1), c(144, "KNEE_DOMINANT", 1),
            c(1097, "HORIZONTAL_PUSH", 1));

    static PlanDraft.DraftExercise ej(long id, int s, int r) {
        return new PlanDraft.DraftExercise(id, PrescriptionType.SETS_REPS, (short) s, (short) r, null, null, (short) 90, null);
    }

    static PreviousWeekSummary previa(String motivo, List<PlanDraft.DraftExercise> antes) {
        var presc = new ArrayList<PreviousWeekSummary.PrescriptionOutcome>();
        for (var e : antes)
            presc.add(new PreviousWeekSummary.PrescriptionOutcome(S, L1, e.exerciseId(), "e", "SETS_REPS", e.plannedSets(),
                    e.plannedReps(), null, null, null, null, null, null, BigDecimal.ZERO, "SKIPPED", motivo, "SKIPPED",
                    (short) 90));
        return new PreviousWeekSummary(new BigDecimal("33"), null, 0, Map.of(), List.of(
                new PreviousWeekSummary.WorkoutOutcome(S, L1, "FULL_BODY", "SKIPPED", motivo, true, "SKIPPED", null,
                        BigDecimal.ZERO, "S", (short) 45)), presc, List.of());
    }

    static PlanAdjustment orden(TipoIntervencion t) {
        return new PlanAdjustment(List.of(AdjustmentType.NONE), null, null, null, 0.0, 0.0, null, null, null, MENSAJE,
                null, null, List.of(new PlanAdjustment.TargetedSession(S, t)));
    }

    static AdaptedWeekResult resultado(List<PlanDraft.DraftExercise> despues, boolean aplicada, List<String> notas) {
        var semana = List.of(new DerivedWorkout(S, new PlanDraft.DraftWorkout(L2, WorkoutFocus.FULL_BODY, "S", 45, despues)));
        var informe = new ConservationReport(List.of(), aplicada ? List.of(S) : List.of(), aplicada ? List.of() : List.of(S),
                List.of(), Set.of(), List.of(), List.of(), List.of(), Set.of(), List.of(), notas);
        return new AdaptedWeekResult(AdaptedWeekResult.Estado.PUBLICABLE,
                aplicada ? AdaptedWeekResult.Motivo.CORRECCION_IA : AdaptedWeekResult.Motivo.SIN_EFECTO, semana,
                aplicada ? GenerationSource.AI : GenerationSource.CONSERVED, informe, List.of());
    }

    static String texto(TipoIntervencion t, String motivo, List<PlanDraft.DraftExercise> antes,
                        List<PlanDraft.DraftExercise> despues, boolean aplicada, List<String> notas) {
        var o = orden(t);
        var conteo = DescripcionAdaptacion.clasificar(resultado(despues, aplicada, notas), previa(motivo, antes), CAT, o);
        return DescripcionAdaptacion.texto(MENSAJE, o, conteo);
    }

    @Test
    @DisplayName("U04: dos bajadas 2 -> 1 del mismo patron -> 'variante de menor dificultad'; nunca 'mismo nivel'")
    void u04Bajadas() {
        var t = texto(TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA, "TOO_DIFFICULT",
                List.of(ej(380, 3, 10), ej(389, 3, 10), ej(360, 3, 10)), List.of(ej(72, 3, 10), ej(74, 3, 10), ej(360, 3, 10)),
                true, List.of());
        assertEquals("Cumpliste el 33 % de tu plan. Nos dijiste que una sesión te resultó difícil. En 2 ejercicios usamos"
                + " una variante de menor dificultad.", t);
    }

    @Test
    @DisplayName("R01: sustituciones 1 -> 1 -> 'mismo nivel de dificultad, no necesariamente más fácil'; sin 'bajamos la exigencia'")
    void r01Laterales() {
        var t = texto(TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA, "TOO_DIFFICULT",
                List.of(ej(933, 3, 10), ej(143, 3, 10), ej(1195, 3, 10)), List.of(ej(129, 3, 10), ej(114, 3, 10), ej(144, 3, 10)),
                true, List.of());
        assertTrue(t.contains("Cambiamos 3 ejercicios por otros del mismo nivel de dificultad: es una alternativa, no"
                + " necesariamente más fácil."), t);
        assertTrue(t.contains("Si te sigue resultando difícil, indícalo de nuevo"), t);
        assertFalse(t.contains("menor dificultad"), t);
        assertFalse(t.toLowerCase().contains("bajamos la exigencia"), t);
        assertFalse(t.toLowerCase().contains("más sencill"), t);
    }

    @Test
    @DisplayName("U04 mixto: una bajada y una lateral -> las dos frases, cada una con su numero")
    void mixto() {
        var conteo = DescripcionAdaptacion.clasificar(resultado(List.of(ej(72, 3, 10), ej(56, 3, 10)), true, List.of()),
                previa("TOO_DIFFICULT", List.of(ej(380, 3, 10), ej(933, 3, 10))), CAT,
                orden(TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA));
        // 380 (dif 2) se empareja con 72 (dif 1, baja); 933 (dif 1) con 56 (dif 2): no baja -> lateral (no se afirma)
        assertEquals(1, conteo.get(Cambio.REDUCCION_DIFICULTAD));
        assertEquals(1, conteo.get(Cambio.SUSTITUCION_LATERAL));
    }

    @Test
    @DisplayName("R03: una repeticion menos -> 'bajamos un poco el trabajo'")
    void r03Dosis() {
        var t = texto(TipoIntervencion.INVESTIGAR_CAMBIO_MINIMO, "LACK_OF_MOTIVATION", List.of(ej(1097, 2, 10)),
                List.of(ej(1097, 2, 9)), true, List.of());
        assertEquals("Cumpliste el 33 % de tu plan. Nos dijiste que te costó encontrar motivación. En 1 ejercicio bajamos"
                + " un poco el trabajo (menos series, repeticiones o tiempo).", t);
    }

    @Test
    @DisplayName("R05 sin cambio: 'no aplicamos cambios'; nunca dice que se bajo la exigencia")
    void noAplicada() {
        var antes = List.of(ej(933, 2, 8));
        var t = texto(TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA, "TOO_DIFFICULT", antes, antes, false, List.of());
        assertTrue(t.contains("En una de las sesiones no aplicamos cambios esta semana: se mantiene como estaba."), t);
        assertTrue(t.contains("indícalo de nuevo"), t);
        assertFalse(t.toLowerCase().contains("bajamos"), t);
    }

    @Test
    @DisplayName("Factibilidad previa sin opcion -> 'no encontramos un cambio compatible'")
    void conservadaSinAlternativa() {
        var antes = List.of(ej(933, 2, 8));
        var t = texto(TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA, "TOO_DIFFICULT", antes, antes, false,
                List.of(AdaptationOutcome.NOTA_FACTIBILIDAD + "sin opcion"));
        assertTrue(t.contains("no encontramos un cambio compatible con tu plan, así que por ahora se mantiene igual."), t);
    }

    @Test
    @DisplayName("Dolor: 'por seguridad' + consulta profesional")
    void dolor() {
        var t = texto(TipoIntervencion.SEGURIDAD_ANTE_DOLOR, "PAIN_OR_DISCOMFORT", List.of(ej(380, 3, 10)),
                List.of(ej(360, 3, 10)), true, List.of());
        assertTrue(t.startsWith("Cumpliste el 33 % de tu plan. Reportaste molestias. Por seguridad, cambiamos o retiramos 1"
                + " ejercicio."), t);
    }

    @Test
    @DisplayName("Sin sesiones senaladas el mensaje original no cambia")
    void sinSenaladas() {
        var o = new PlanAdjustment(List.of(AdjustmentType.NONE), null, null, null, 0.0, 0.0, null, null, null, MENSAJE,
                null, null, List.of());
        assertEquals(MENSAJE, DescripcionAdaptacion.texto(MENSAJE, o, Map.of()));
    }

    @Test
    @DisplayName("Si el mensaje no trae el prefijo de adherencia, el texto empieza por el motivo")
    void sinPrefijo() {
        var o = orden(TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA);
        assertEquals("Nos dijiste que una sesión te resultó difícil. En 1 ejercicio usamos una variante de menor dificultad.",
                DescripcionAdaptacion.texto("otro texto", o, Map.of(Cambio.REDUCCION_DIFICULTAD, 1)));
    }
}
