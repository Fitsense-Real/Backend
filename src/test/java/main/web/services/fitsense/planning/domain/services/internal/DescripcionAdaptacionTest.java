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
    /** El motivo REAL que llega como adjustment.reason(): el de la orden (AdaptationOrderDecider), tecnico. */
    static final String MENSAJE = "Adherencia observada 33.33 % (cobertura 100.00 %). Entre 50 % y 80 %: se corrige solo la peor sesion.";

    static CandidateExercise c(long id, String patron, int dif) {
        return new CandidateExercise(id, "e" + id, "x", "body weight", dif, PrescriptionType.SETS_REPS, "m", patron);
    }

    static final List<CandidateExercise> CAT = List.of(
            c(380, "HORIZONTAL_PUSH", 2), c(389, "VERTICAL_PULL", 2), c(72, "HORIZONTAL_PUSH", 1),
            c(74, "VERTICAL_PULL", 1), c(56, "HORIZONTAL_PUSH", 2), c(360, "HIP_HINGE", 2),
            // R01 (todo dificultad 1)
            c(933, "HORIZONTAL_PUSH", 1), c(129, "HORIZONTAL_PUSH", 1), c(143, "VERTICAL_PULL", 1),
            c(114, "VERTICAL_PULL", 1), c(1195, "KNEE_DOMINANT", 1), c(144, "KNEE_DOMINANT", 1),
            c(1097, "HORIZONTAL_PUSH", 1), c(580, "HORIZONTAL_PUSH", 1));

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
    @DisplayName("U04: dos bajadas 2 -> 1 del mismo patron -> 'registrada como de menor dificultad'; sin el motivo tecnico")
    void u04Bajadas() {
        var t = texto(TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA, "TOO_DIFFICULT",
                List.of(ej(380, 3, 10), ej(389, 3, 10), ej(360, 3, 10)), List.of(ej(72, 3, 10), ej(74, 3, 10), ej(360, 3, 10)),
                true, List.of());
        assertEquals("Nos dijiste que una sesión te resultó difícil. En 2 ejercicios usamos una variante registrada como"
                + " de menor dificultad.", t);
    }

    @Test
    @DisplayName("R01: sustituciones 1 -> 1 -> 'misma dificultad registrada, no necesariamente más fácil' + invitacion")
    void r01Laterales() {
        var t = texto(TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA, "TOO_DIFFICULT",
                List.of(ej(933, 3, 10), ej(143, 3, 10), ej(1195, 3, 10)), List.of(ej(129, 3, 10), ej(114, 3, 10), ej(144, 3, 10)),
                true, List.of());
        assertEquals("Nos dijiste que una sesión te resultó difícil. Cambiamos 3 ejercicios por otros con la misma"
                + " dificultad registrada: es una alternativa, no necesariamente más fácil. Si te sigue resultando"
                + " difícil, indícalo de nuevo al registrar la sesión.", t);
        assertFalse(t.toLowerCase().contains("más sencill"), t);
    }

    @Test
    @DisplayName("DIFICULTAD MAYOR: 580 (1) -> 56 (2) del mismo patron se dice tal cual, nunca 'mismo nivel'")
    void mayorDificultad() {
        var o = orden(TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA);
        var conteo = DescripcionAdaptacion.clasificar(resultado(List.of(ej(56, 3, 10)), true, List.of()),
                previa("TOO_DIFFICULT", List.of(ej(580, 3, 10))), CAT, o);
        assertEquals(Map.of(Cambio.SUSTITUCION_MAYOR_DIFICULTAD, 1), conteo);
        var t = DescripcionAdaptacion.texto(MENSAJE, o, conteo);
        assertEquals("Nos dijiste que una sesión te resultó difícil. Cambiamos 1 ejercicio por otro registrado como de"
                + " mayor dificultad. Si te sigue resultando difícil, indícalo de nuevo al registrar la sesión.", t);
        assertFalse(t.contains("misma dificultad"), t);
    }

    @Test
    @DisplayName("Mixto: 380 (2) -> 72 (1) baja y 933 (1) -> 56 (2) sube: una frase para cada cosa, sin invitacion")
    void mixto() {
        var o = orden(TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA);
        var conteo = DescripcionAdaptacion.clasificar(resultado(List.of(ej(72, 3, 10), ej(56, 3, 10)), true, List.of()),
                previa("TOO_DIFFICULT", List.of(ej(380, 3, 10), ej(933, 3, 10))), CAT, o);
        assertEquals(Map.of(Cambio.REDUCCION_DIFICULTAD, 1, Cambio.SUSTITUCION_MAYOR_DIFICULTAD, 1), conteo);
        assertEquals("Nos dijiste que una sesión te resultó difícil. En 1 ejercicio usamos una variante registrada como"
                + " de menor dificultad. Cambiamos 1 ejercicio por otro registrado como de mayor dificultad.",
                DescripcionAdaptacion.texto(MENSAJE, o, conteo));
    }

    @Test
    @DisplayName("Sin ficha del retirado (ya no elegible): 'no tenemos un dato para comparar', y no se cuenta como retirada")
    void sinComparacion() {
        var o = orden(TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA);
        var conteo = DescripcionAdaptacion.clasificar(resultado(List.of(ej(72, 3, 10)), true, List.of()),
                previa("TOO_DIFFICULT", List.of(ej(9999, 3, 10))), CAT, o);
        assertEquals(Map.of(Cambio.SUSTITUCION_SIN_COMPARACION, 1), conteo);
        assertTrue(DescripcionAdaptacion.texto(MENSAJE, o, conteo)
                .contains("Cambiamos 1 ejercicio por otro; no tenemos un dato para comparar su dificultad."));
    }

    @Test
    @DisplayName("Retira 2 y anade 1: la sustitucion y la retirada sobrante se cuentan las dos")
    void retiradaSobrante() {
        var conteo = DescripcionAdaptacion.clasificar(resultado(List.of(ej(72, 3, 10), ej(360, 3, 10)), true, List.of()),
                previa("TOO_DIFFICULT", List.of(ej(380, 3, 10), ej(56, 3, 10), ej(360, 3, 10))), CAT,
                orden(TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA));
        assertEquals(Map.of(Cambio.REDUCCION_DIFICULTAD, 1, Cambio.RETIRADA, 1), conteo);
    }

    @Test
    @DisplayName("R03: una repeticion menos -> 'bajamos un poco el trabajo'")
    void r03Dosis() {
        var t = texto(TipoIntervencion.INVESTIGAR_CAMBIO_MINIMO, "LACK_OF_MOTIVATION", List.of(ej(1097, 2, 10)),
                List.of(ej(1097, 2, 9)), true, List.of());
        assertEquals("Nos dijiste que te costó encontrar motivación. En 1 ejercicio bajamos un poco el trabajo (menos"
                + " series, repeticiones o tiempo).", t);
    }

    @Test
    @DisplayName("R05 sin cambio: 'no aplicamos cambios'; nunca dice que se bajo nada")
    void noAplicada() {
        var antes = List.of(ej(933, 2, 8));
        var t = texto(TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA, "TOO_DIFFICULT", antes, antes, false, List.of());
        assertEquals("Nos dijiste que una sesión te resultó difícil. En una de las sesiones no aplicamos cambios esta"
                + " semana: se mantiene como estaba. Si te sigue resultando difícil, indícalo de nuevo al registrar la"
                + " sesión.", t);
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
    @DisplayName("Dolor: 'por seguridad' + consulta profesional; la dificultad no se compara aunque suba (580 -> 56)")
    void dolor() {
        var o = orden(TipoIntervencion.SEGURIDAD_ANTE_DOLOR);
        var conteo = DescripcionAdaptacion.clasificar(resultado(List.of(ej(56, 3, 10)), true, List.of()),
                previa("PAIN_OR_DISCOMFORT", List.of(ej(580, 3, 10))), CAT, o);
        assertEquals(Map.of(Cambio.SUSTITUCION_SEGURIDAD, 1), conteo);
        assertEquals("Reportaste molestias. Por seguridad, cambiamos o retiramos 1 ejercicio. Si la molestia continúa,"
                + " consulta a un profesional de la salud.", DescripcionAdaptacion.texto(MENSAJE, o, conteo));
    }

    @Test
    @DisplayName("Sin sesiones senaladas el mensaje original no cambia")
    void sinSenaladas() {
        var o = new PlanAdjustment(List.of(AdjustmentType.NONE), null, null, null, 0.0, 0.0, null, null, null, MENSAJE,
                null, null, List.of());
        assertEquals(MENSAJE, DescripcionAdaptacion.texto(MENSAJE, o, Map.of()));
    }
}
