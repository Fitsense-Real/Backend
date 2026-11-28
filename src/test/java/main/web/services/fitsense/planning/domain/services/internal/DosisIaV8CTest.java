package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V8C · LA IA NUNCA SUBE DOSIS, CON CUALQUIER INTERRUPTOR (correccion 1, 27/10/2026).
 * <p>
 * Los 16 pares son los que la campana con Replicate real publico con la
 * progresion y la V24 por capacidades apagadas (la configuracion de produccion):
 * uno por ciclo, con su intervencion. Cada uno se prueba con
 * {@code autorizarDosis} en false (produccion) y en true (progresion o
 * capacidades): en los dos se rechaza.
 */
@DisplayName("V8C · la IA no sube series, repeticiones ni segundos en una sesion senalada, con o sin interruptores")
class DosisIaV8CTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 11, 2);
    private static final long S = 901L;       // la senalada
    private static final long OTRA = 902L;    // no senalada
    private static final long B = 11L;        // otro ejercicio de la senalada, sin cambios

    private final ConservationValidator v8c = new ConservationValidator();

    record Caso(String perfil, TipoIntervencion t, long id, String tipo, int series, int valor, int descanso,
                int seriesN, int valorN, int descansoN) {
        @Override
        public String toString() {
            return perfil + " " + t + " " + id + ": " + series + "x" + valor + "/" + descanso + " -> "
                    + seriesN + "x" + valorN + "/" + descansoN;
        }
    }

    static final TipoIntervencion CARGA = TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION;
    static final TipoIntervencion VARIANTE = TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA;
    static final TipoIntervencion INVESTIGAR = TipoIntervencion.INVESTIGAR_CAMBIO_MINIMO;

    /** Los 16 ciclos de la campana 1 (evidencia/replicate_real/adaptacion/ciclos.jsonl). */
    static Stream<Caso> observados() {
        return Stream.of(
                new Caso("LW/INT/45 s4", TipoIntervencion.ACORTAR_O_REORGANIZAR, 755, "R", 3, 6, 150, 2, 8, 120),
                new Caso("LW/INT/45 s6", CARGA, 755, "R", 2, 8, 120, 3, 8, 150),
                new Caso("LW/INT/45 s7", VARIANTE, 1324, "D", 3, 120, 60, 2, 180, 60),
                new Caso("LW/ADV/60 s2", CARGA, 1096, "R", 3, 6, 150, 2, 8, 180),
                new Caso("LW/ADV/60 s6", CARGA, 1135, "D", 2, 60, 30, 3, 60, 30),
                new Caso("LW/ADV/60 s7", VARIANTE, 1092, "R", 3, 6, 150, 3, 8, 90),
                new Caso("LW/ADV/60 s8", INVESTIGAR, 1096, "R", 2, 6, 150, 3, 6, 150),
                new Caso("IS/INT/45 s6", CARGA, 418, "R", 2, 6, 120, 3, 6, 150),
                new Caso("IS/INT/45 s8", INVESTIGAR, 992, "R", 3, 6, 150, 4, 6, 150),
                new Caso("IS/ADV/60 s2", CARGA, 183, "R", 3, 6, 150, 4, 6, 180),
                new Caso("IS/ADV/60 s6", CARGA, 183, "R", 3, 6, 150, 6, 6, 180),
                new Caso("IS/ADV/60 s7", VARIANTE, 975, "R", 2, 13, 90, 3, 13, 90),
                new Caso("IS/ADV/60 s8", INVESTIGAR, 186, "R", 2, 6, 180, 3, 6, 180),
                new Caso("GF/BEG/30 s2", CARGA, 147, "R", 2, 8, 120, 3, 8, 120),
                new Caso("GF/ADV/60 s6", CARGA, 1104, "D", 2, 40, 60, 3, 30, 60),
                new Caso("GF/ADV/60 s8", INVESTIGAR, 446, "R", 3, 18, 45, 4, 18, 45));
    }

    static Stream<Arguments> observadosEnLosDosEstados() {
        return observados().flatMap(c -> Stream.of(Arguments.of(c, false), Arguments.of(c, true)));
    }

    @ParameterizedTest(name = "{0} · autorizarDosis={1}")
    @MethodSource("observadosEnLosDosEstados")
    @DisplayName("los 16 incrementos observados con Replicate real se rechazan")
    void incrementoObservado(Caso c, boolean autorizarDosis) {
        var r = validar(c.t(), c.id(), c.tipo(), c.series(), c.valor(), c.descanso(),
                c.seriesN(), c.valorN(), c.descansoN(), autorizarDosis);
        assertFalse(r.aceptable(), c + ": se acepto");
        assertTrue(r.problems().stream().anyMatch(p -> p.contains("sube la dosis del ejercicio " + c.id())),
                c + ": " + r.problems());
        assertTrue(r.aplicadas().isEmpty(), "una sesion rechazada no cuenta como aplicada");
    }

    @ParameterizedTest(name = "autorizarDosis={0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("control: bajar series, sin subir nada, sigue siendo una adaptacion aplicada")
    void reduccionSigueAceptada(boolean autorizarDosis) {
        var r = validar(CARGA, 755, "R", 3, 6, 150, 2, 6, 150, autorizarDosis);
        assertTrue(r.aceptable(), r.problems().toString());
        assertEquals(List.of(S), r.aplicadas());
    }

    @ParameterizedTest(name = "autorizarDosis={0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("control: bajar segundos de un ejercicio por duracion tambien se acepta")
    void reduccionDeDuracionAceptada(boolean autorizarDosis) {
        var r = validar(CARGA, 1324, "D", 3, 120, 60, 3, 90, 60, autorizarDosis);
        assertTrue(r.aceptable(), r.problems().toString());
        assertEquals(List.of(S), r.aplicadas());
    }

    @ParameterizedTest(name = "autorizarDosis={0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("solo con autorizarDosis se emiten dosis autorizadas para V24; la prohibicion no depende de ello")
    void autorizacionesSoloConInterruptor(boolean autorizarDosis) {
        var r = validar(CARGA, 755, "R", 3, 6, 150, 2, 6, 150, autorizarDosis);
        assertEquals(autorizarDosis, !r.dosisAutorizadas().isEmpty(), r.dosisAutorizadas().toString());
    }

    @ParameterizedTest(name = "autorizarDosis={0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("una sustitucion por dificultad no se compara con el ejercicio retirado (otra prescripcion valida)")
    void sustitucionNoSeComparaConElRetirado(boolean autorizarDosis) {
        var catalogo = Map.of(755L, candidato(755L, "HORIZONTAL_PUSH"), 756L, candidato(756L, "HORIZONTAL_PUSH"),
                B, candidato(B, "VERTICAL_PULL"));
        var derivada = new WeekDerivation(List.of(
                new DerivedWorkout(S, sesion(S, 0, List.of(ej(756L, "R", 4, 10, 90), ej(B, "R", 3, 10, 90)))),
                new DerivedWorkout(OTRA, sesion(OTRA, 2, List.of(ej(B, "R", 3, 10, 90))))), List.of());
        var r = v8c.validate(derivada, previa(755L, "R", 2, 8, 90), orden(VARIANTE), catalogo,
                ExclusionesDeSeguridad.ninguna(), List.of(), autorizarDosis);
        assertTrue(r.problems().stream().noneMatch(p -> p.contains("sube la dosis")), r.problems().toString());
    }

    // =======================================================================

    private ConservationReport validar(TipoIntervencion t, long id, String tipo, int series, int valor, int descanso,
                                       int seriesN, int valorN, int descansoN, boolean autorizarDosis) {
        var derivada = new WeekDerivation(List.of(
                new DerivedWorkout(S, sesion(S, 0, List.of(ej(id, tipo, seriesN, valorN, descansoN),
                        ej(B, "R", 3, 10, 90)))),
                new DerivedWorkout(OTRA, sesion(OTRA, 2, List.of(ej(B, "R", 3, 10, 90))))), List.of());
        return v8c.validate(derivada, previa(id, tipo, series, valor, descanso), orden(t), Map.of(),
                ExclusionesDeSeguridad.ninguna(), List.of(), autorizarDosis);
    }

    private static PlanDraft.DraftExercise ej(long id, String tipo, int series, int valor, int descanso) {
        boolean d = tipo.equals("D");
        return new PlanDraft.DraftExercise(id, d ? PrescriptionType.DURATION : PrescriptionType.SETS_REPS,
                (short) series, d ? null : (short) valor, d ? valor : null, null, (short) descanso, null);
    }

    private static PlanDraft.DraftWorkout sesion(long id, int dia, List<PlanDraft.DraftExercise> l) {
        return new PlanDraft.DraftWorkout(LUNES.plusDays(dia).plusWeeks(1), WorkoutFocus.FULL_BODY, "Sesion " + id, 40, l);
    }

    private static PreviousWeekSummary previa(long id, String tipo, int series, int valor, int descanso) {
        var sesiones = List.of(
                new PreviousWeekSummary.WorkoutOutcome(S, LUNES, "FULL_BODY", "SKIPPED", "FATIGUE", true, "SKIPPED",
                        null, BigDecimal.ZERO, "Sesion " + S, (short) 40),
                new PreviousWeekSummary.WorkoutOutcome(OTRA, LUNES.plusDays(2), "FULL_BODY", "COMPLETED", null, true,
                        "COMPLETED", (short) 6, new BigDecimal("100.00"), "Sesion " + OTRA, (short) 40));
        var prescritas = List.of(
                presc(S, LUNES, id, tipo, series, valor, descanso),
                presc(S, LUNES, B, "R", 3, 10, 90),
                presc(OTRA, LUNES.plusDays(2), B, "R", 3, 10, 90));
        return new PreviousWeekSummary(new BigDecimal("66.67"), new BigDecimal("6.0"), null, Map.of(), sesiones,
                prescritas, List.of());
    }

    private static PreviousWeekSummary.PrescriptionOutcome presc(long sesion, LocalDate dia, long id, String tipo,
                                                                 int series, int valor, int descanso) {
        boolean d = tipo.equals("D");
        return new PreviousWeekSummary.PrescriptionOutcome(sesion, dia, id, "ejercicio " + id,
                d ? "DURATION" : "SETS_REPS", (short) series, d ? null : (short) valor, d ? valor : null, null,
                (short) series, d ? null : series * valor, d ? series * valor : null, null, new BigDecimal("100.00"),
                "COMPLETED", null, "COMPLETED", (short) descanso);
    }

    private static CandidateExercise candidato(long id, String patron) {
        return new CandidateExercise(id, "ej" + id, "chest", "body weight", 1, PrescriptionType.SETS_REPS, "pectorals", patron);
    }

    private static PlanAdjustment orden(TipoIntervencion t) {
        return new PlanAdjustment(List.of(AdjustmentType.NONE), null, null, null, 0.0, 0.0, null, null, null,
                "causa", null, null, List.of(new PlanAdjustment.TargetedSession(S, t)));
    }
}
