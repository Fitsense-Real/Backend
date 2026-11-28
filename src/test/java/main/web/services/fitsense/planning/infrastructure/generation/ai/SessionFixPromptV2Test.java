package main.web.services.fitsense.planning.infrastructure.generation.ai;

import main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.valueobjects.AdjustmentType;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.ConservationReport;
import main.web.services.fitsense.planning.domain.model.valueobjects.DerivedWorkout;
import main.web.services.fitsense.planning.domain.model.valueobjects.GenerationSource;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanningProfile;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.planning.domain.model.valueobjects.PreviousWeekSummary;
import main.web.services.fitsense.planning.domain.model.valueobjects.SafetyProfile;
import main.web.services.fitsense.planning.domain.model.valueobjects.WeekDerivation;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;
import main.web.services.fitsense.planning.domain.services.DoseEnforcement;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.SessionDurationEstimator;
import main.web.services.fitsense.planning.domain.services.SlotCoverageEnforcement;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PROMPT DE ADAPTACION V2 (experimental, experimento C1 frente a C2). Sin Replicate.
 * <p>
 * Lo que se comprueba: (1) apagado, V1 es exactamente V1; (2) V2 solo ANADE texto; (3) los numeros de V2 son los del
 * backend (estimador y piso de V20); (4) lo que V2 llama «cabe» lo acepta V20 en el validador real y lo que llama
 * «NO cabe» lo rechaza; (5) las sesiones sin cambio posible (GAIN_MUSCLE con dosis solamente) se declaran como tales;
 * (6) el reintento de V20 cambia de pista solo en V2. Las sesiones reproducen las condiciones de U03, U04, U08, U09,
 * U18 y U19 de la etapa C (experimento_prompt_v2/01).
 */
@DisplayName("Prompt de adaptacion V2 (experimental): factibilidad previa y compatibilidad con V1")
class SessionFixPromptV2Test {

    static final LocalDate S = LocalDate.of(2026, 11, 9);
    static final JsonSupport JSON = new JsonSupport();
    static final SessionFixPromptBuilder V1 = new SessionFixPromptBuilder(JSON);
    static final SessionFixPromptBuilder V1_EXPLICITO = new SessionFixPromptBuilder(JSON, "v1");
    static final SessionFixPromptBuilder V2 = new SessionFixPromptBuilder(JSON, "v2");
    /** Los parametros de MVP-1.2 (V14): 70 %, 3 s por repeticion, 60 s de transicion, 5 min, 60 s de descanso. */
    static final PrescriptionParams P = new PrescriptionParams(70, 3, 60, 5, 60, 20, null,
            new PrescriptionParams.RepLimits(6, 30, Map.of()), 10, 180);
    static final PrescriptionParams P_PRINCIPIANTE = new PrescriptionParams(70, 3, 60, 5, 60, 20, null,
            new PrescriptionParams.RepLimits(6, 30, Map.of()), 10, 180);
    static final PlanDraftValidator VALIDADOR = new PlanDraftValidator(new SessionDurationEstimator(),
            SlotCoverageEnforcement.OBSERVE, DoseEnforcement.OBSERVE);

    // ======================================================== escenarios (los de la etapa C)

    record Escenario(PlanGenerationContext ctx, WeekDerivation der) {}

    static PlanDraft.DraftExercise sr(long id, int series, int reps, int descanso) {
        return new PlanDraft.DraftExercise(id, PrescriptionType.SETS_REPS, (short) series, (short) reps, null, null,
                (short) descanso, null);
    }

    static PlanDraft.DraftExercise dur(long id, int series, int segundos, int descanso) {
        return new PlanDraft.DraftExercise(id, PrescriptionType.DURATION, (short) series, null, segundos, null,
                (short) descanso, null);
    }

    static CandidateExercise cand(long id, String zona, PrescriptionType tipo, String musculo, String patron, int dif) {
        return new CandidateExercise(id, "ejercicio " + id, zona, "body weight", dif, tipo, musculo, patron);
    }

    static Escenario escenario(String objetivo, String nivel, int minutos, List<CandidateExercise> catalogo,
                               PreviousWeekSummary semana, Object... sesiones) {
        var senaladas = new ArrayList<PlanAdjustment.TargetedSession>();
        var derivadas = new ArrayList<DerivedWorkout>();
        // una sesion conservada (no senalada) el lunes, siempre
        derivadas.add(new DerivedWorkout(900L, new PlanDraft.DraftWorkout(S, WorkoutFocus.FULL_BODY, "A", minutos,
                List.of(sr(9001L, 3, 10, 90), sr(9002L, 3, 10, 90)))));
        for (int i = 0; i < sesiones.length; i += 3) {
            long id = (Long) sesiones[i];
            var tipo = (TipoIntervencion) sesiones[i + 1];
            @SuppressWarnings("unchecked") var ej = (List<PlanDraft.DraftExercise>) sesiones[i + 2];
            senaladas.add(new PlanAdjustment.TargetedSession(id, tipo));
            derivadas.add(new DerivedWorkout(id, new PlanDraft.DraftWorkout(S.plusDays(2L * (derivadas.size())),
                    WorkoutFocus.FULL_BODY, "S" + id, minutos, ej)));
        }
        var orden = new PlanAdjustment(List.of(AdjustmentType.NONE), null, null, null, 0.0, 0.0, null, null, null,
                "baja adherencia", null, null, senaladas);
        var perfil = new PlanningProfile(1L, 33, "FEMALE", new BigDecimal("166"), new BigDecimal("63"), null, nivel,
                objetivo, "x", "Ninguna", "GYM", 3, List.of((short) 1, (short) 3, (short) 5), minutos,
                List.of("body weight"), List.of(), SafetyProfile.Declaradas.sinLimitaciones());
        var todos = new ArrayList<>(catalogo);
        todos.add(cand(9001L, "back", PrescriptionType.SETS_REPS, "lats", "VERTICAL_PULL", 1));
        todos.add(cand(9002L, "chest", PrescriptionType.SETS_REPS, "pectorals", "HORIZONTAL_PUSH", 1));
        var ctx = new PlanGenerationContext(1L, (short) 2, S, S.plusDays(6), perfil, orden, semana, todos,
                SafetyProfile.none(), "BEGINNER".equals(nivel) ? P_PRINCIPIANTE : P);
        return new Escenario(ctx, new WeekDerivation(derivadas, List.of()));
    }

    /** U08 S45 (y U09 S51): fuerza 3x6 con 150 s y dos de hipertrofia 3x10 con 90 s, 60 min. 2556 s, minimo 2520. */
    static List<PlanDraft.DraftExercise> sesionFuerza() {
        return List.of(sr(1106L, 3, 6, 150), sr(994L, 3, 6, 150), sr(1265L, 3, 6, 150), sr(1154L, 3, 6, 150),
                sr(323L, 3, 10, 90), sr(487L, 3, 10, 90));
    }

    static List<CandidateExercise> catalogoFuerza() {
        return List.of(cand(1106L, "chest", PrescriptionType.SETS_REPS, "pectorals", "HORIZONTAL_PUSH", 2),
                cand(994L, "back", PrescriptionType.SETS_REPS, "upper back", "HORIZONTAL_PULL", 1),
                cand(1265L, "upper legs", PrescriptionType.SETS_REPS, "quads", "KNEE_DOMINANT", 1),
                cand(1154L, "shoulders", PrescriptionType.SETS_REPS, "delts", "VERTICAL_PUSH", 1),
                cand(323L, "chest", PrescriptionType.SETS_REPS, "pectorals", "HORIZONTAL_PUSH", 2),
                cand(487L, "back", PrescriptionType.SETS_REPS, "lats", "VERTICAL_PULL", 2),
                cand(72L, "chest", PrescriptionType.SETS_REPS, "pectorals", "HORIZONTAL_PUSH", 1),
                cand(119L, "back", PrescriptionType.SETS_REPS, "upper back", "HORIZONTAL_PULL", 1));
    }

    /** U03 S15 / U04: GAIN_MUSCLE, 7 ejercicios 3x10 con 90 s, 60 min. 2550 s, minimo 2520. */
    static List<PlanDraft.DraftExercise> sesionGm() {
        return List.of(sr(380L, 3, 10, 90), sr(351L, 3, 10, 90), sr(637L, 3, 10, 90), sr(949L, 3, 10, 90),
                sr(1073L, 3, 10, 90), sr(496L, 3, 10, 90), sr(436L, 3, 10, 90));
    }

    static List<CandidateExercise> catalogoGm() {
        return List.of(cand(380L, "chest", PrescriptionType.SETS_REPS, "pectorals", "HORIZONTAL_PUSH", 2),
                cand(351L, "back", PrescriptionType.SETS_REPS, "upper back", "HORIZONTAL_PULL", 1),
                cand(637L, "upper legs", PrescriptionType.SETS_REPS, "hamstrings", "KNEE_DOMINANT", 2),
                cand(949L, "shoulders", PrescriptionType.SETS_REPS, "delts", "VERTICAL_PUSH", 2),
                cand(1073L, "upper legs", PrescriptionType.SETS_REPS, "glutes", "HIP_EXTENSION", 1),
                cand(496L, "chest", PrescriptionType.SETS_REPS, "pectorals", "HORIZONTAL_PUSH", 1),
                cand(436L, "back", PrescriptionType.SETS_REPS, "upper back", "HORIZONTAL_PULL", 1),
                cand(72L, "chest", PrescriptionType.SETS_REPS, "pectorals", "HORIZONTAL_PUSH", 1));
    }

    /** U18 S105 / U19: GENERAL_FITNESS principiante, casa, 45 min. 2040 s, minimo 1890. */
    static List<PlanDraft.DraftExercise> sesionAg() {
        return List.of(sr(101L, 2, 10, 90), sr(1097L, 2, 10, 90), sr(775L, 2, 10, 90), sr(1334L, 2, 15, 45),
                sr(1308L, 2, 15, 45), sr(1076L, 2, 15, 45), dur(1054L, 3, 90, 60), dur(1188L, 2, 30, 15));
    }

    static List<CandidateExercise> catalogoAg() {
        return List.of(cand(101L, "shoulders", PrescriptionType.SETS_REPS, "delts", "VERTICAL_PUSH", 1),
                cand(1097L, "chest", PrescriptionType.SETS_REPS, "pectorals", "HORIZONTAL_PUSH", 1),
                cand(775L, "chest", PrescriptionType.SETS_REPS, "upper back", "HORIZONTAL_PUSH", 1),
                cand(1334L, "chest", PrescriptionType.SETS_REPS, "pectorals", "HORIZONTAL_PUSH", 1),
                cand(1308L, "waist", PrescriptionType.SETS_REPS, "spine", "TRUNK_FLEXION", 1),
                cand(1076L, "upper legs", PrescriptionType.SETS_REPS, "glutes", "KNEE_DOMINANT", 1),
                cand(1054L, "cardio", PrescriptionType.DURATION, "cardiovascular system", "LOCOMOTION_CARDIO", 1),
                cand(1188L, "back", PrescriptionType.DURATION, "lats", "MOBILITY", 1));
    }

    static Escenario u08() {
        return escenario("INCREASE_STRENGTH", "INTERMEDIATE", 60, catalogoFuerza(), null,
                45L, TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION, sesionFuerza());
    }

    static Escenario u09() {
        return escenario("INCREASE_STRENGTH", "INTERMEDIATE", 60, catalogoFuerza(), null,
                50L, TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA, sesionFuerza(),
                51L, TipoIntervencion.INVESTIGAR_CAMBIO_MINIMO, sesionFuerza());
    }

    static Escenario u03() {
        return escenario("GAIN_MUSCLE", "INTERMEDIATE", 60, catalogoGm(), null,
                15L, TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION, sesionGm());
    }

    static Escenario u04() {
        return escenario("GAIN_MUSCLE", "INTERMEDIATE", 60, catalogoGm(), null,
                20L, TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA, sesionGm(),
                21L, TipoIntervencion.INVESTIGAR_CAMBIO_MINIMO, sesionGm());
    }

    static Escenario u18() {
        return escenario("GENERAL_FITNESS", "BEGINNER", 45, catalogoAg(), null,
                105L, TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION, sesionAg());
    }

    static Escenario u19() {
        return escenario("GENERAL_FITNESS", "BEGINNER", 45, catalogoAg(), null,
                110L, TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA, sesionAg(),
                111L, TipoIntervencion.INVESTIGAR_CAMBIO_MINIMO, sesionAg());
    }

    static String texto(SessionFixPromptBuilder b, Escenario e, AiRetryFeedback f) {
        return b.build(e.ctx(), e.der(), PlanInputSnapshotFixture.snapshot(), f).text();
    }

    static String plano(String s) {
        return s.replaceAll("\\s+", " ");
    }

    // ======================================================== 1-4: apagado y compatibilidad

    @Test
    @DisplayName("1. Apagado (constructor por defecto o v1): el mismo texto y la version FIX-PR-1.3")
    void apagadoEsV1() {
        for (var e : List.of(u03(), u04(), u08(), u09(), u18(), u19())) {
            assertEquals(texto(V1, e, null), texto(V1_EXPLICITO, e, null));
            assertFalse(texto(V1, e, null).contains("FACTIBILIDAD"));
        }
        assertEquals(SessionFixPromptBuilder.VERSION, V1.version());
        assertEquals("FIX-PR-1.3", SessionFixPromptBuilder.VERSION, "V1 no cambia de version");
    }

    @Test
    @DisplayName("2. Un valor desconocido del indicador no arranca (no cae en silencio a ninguna version)")
    void valorDesconocido() {
        assertThrows(IllegalArgumentException.class, () -> new SessionFixPromptBuilder(JSON, "v3"));
        assertEquals(SessionFixPromptBuilder.VERSION_V2, new SessionFixPromptBuilder(JSON, " V2 ").version());
    }

    @Test
    @DisplayName("3. V2 solo ANADE: quitando sus bloques y su version queda V1 byte a byte (sin y con reintento)")
    void v2SoloAnade() {
        var fb = new AiRetryFeedback("{\"sessions\":[]}", List.of("V20: el entrenamiento del 2026-11-11 dura unos 2184 s y el minimo es 2520 s"
                + " (70 % de 60 minutos): le faltan 336 s. Agrega un ejercicio del enfoque o una serie a los que ya tiene,"
                + " no descanso."), List.of());
        for (var e : List.of(u03(), u04(), u08(), u09(), u18(), u19()))
            for (var f : java.util.Arrays.asList(null, fb)) {
                String v1 = texto(V1, e, f), v2 = texto(V2, e, f);
                String reducido = v2;
                int i = reducido.indexOf("\nCOMO USAR FACTIBILIDAD");
                int j = reducido.indexOf("\nSESIONES SENALADAS");
                reducido = reducido.substring(0, i) + reducido.substring(j);
                for (var s : FactibilidadAdaptacion.calcular(e.ctx(), e.der()))
                    reducido = reducido.replace(FactibilidadAdaptacion.texto(s), "");
                reducido = reducido.replace(SessionFixPromptBuilder.VERSION_V2, SessionFixPromptBuilder.VERSION)
                        .replace(SessionFixPromptBuilder.PISTA_V20_V2, PistasDeReintento.para(
                                List.of("V20: x"), true).get(0));
                assertEquals(v1, reducido);
            }
    }

    @Test
    @DisplayName("4. La version viaja en el snapshot y en la etiqueta de la peticion (V1 intacta)")
    void versionEnSnapshotYPeticion() {
        var e = u08();
        assertTrue(V1.build(e.ctx(), e.der(), PlanInputSnapshotFixture.snapshot(), null).snapshotJson()
                .contains("\"FIX-PR-1.3\""));
        assertTrue(V2.build(e.ctx(), e.der(), PlanInputSnapshotFixture.snapshot(), null).snapshotJson()
                .contains("\"FIX-PR-2.0-EXP\""));
        assertEquals(ReplicateTrainingPlanGenerator.Peticion.deCorrecciones(1).promptVersion(),
                ReplicateTrainingPlanGenerator.Peticion.deCorrecciones(1, SessionFixPromptBuilder.VERSION).promptVersion());
        assertTrue(ReplicateTrainingPlanGenerator.Peticion.deCorrecciones(1, SessionFixPromptBuilder.VERSION_V2)
                .promptVersion().startsWith("FIX-PR-2.0-EXP "));
    }

    // ======================================================== 5-10: valores reales por caso

    @Test
    @DisplayName("5. U08: 2556 s, minimo 2520, margen 36; reps -1 en 3x10 cabe (9 s); series -1 no cabe (120 y 168 s)")
    void u08Valores() {
        var t = plano(texto(V2, u08(), null));
        assertTrue(t.contains("C. LIMITES: dura 2556 s; minimo 2520 s; puedes retirar como MAXIMO 36 s"), t);
        assertTrue(t.contains("- 323 (3x10): 3x9 retira 9 s [cabe]; 2x10 retira 120 s [NO cabe]"), t);
        assertTrue(t.contains("- 1106 (3x6): 2x6 retira 168 s [NO cabe] (repeticiones minimas 6)"), t);
        assertTrue(t.contains("F. SI NO HAY SOLUCION: hay opciones validas en D."), t);
        assertTrue(t.contains("No sustituyas ni retires ejercicios."), t);
    }

    @Test
    @DisplayName("6. U03 (GAIN_MUSCLE + REVISAR_CARGA): ninguna opcion y la instruccion explicita de omitir")
    void u03SinSolucion() {
        var f = FactibilidadAdaptacion.calcular(u03().ctx(), u03().der());
        assertEquals(1, f.size());
        assertFalse(f.get(0).hayCambioFactible());
        assertEquals(30L, f.get(0).margen());
        var t = plano(texto(V2, u03(), null));
        assertTrue(t.contains("D. OPCIONES: ninguna. No se puede bajar la dosis: en GAIN_MUSCLE la dosis 4D no admite"
                + " reducciones."), t);
        assertTrue(t.contains("OMITELA del arreglo: es la respuesta correcta."), t);
    }

    @Test
    @DisplayName("7. U04: S21 (GM + INVESTIGAR) sin solucion; S20 (GM + VARIANTE) con sustitutos de la misma dosis 4D")
    void casoU04() {
        var f = FactibilidadAdaptacion.calcular(u04().ctx(), u04().der());
        var s20 = f.stream().filter(x -> x.plannedWorkoutId() == 20L).findFirst().orElseThrow();
        var s21 = f.stream().filter(x -> x.plannedWorkoutId() == 21L).findFirst().orElseThrow();
        assertFalse(s21.hayCambioFactible());
        assertTrue(s20.hayCambioFactible(), s20.toString());
        assertTrue(s20.sustitutos().stream().anyMatch(x -> x.exerciseId() == 72L && x.mismaDosis()), s20.toString());
        assertTrue(s20.opciones().isEmpty(), "en GM no se ofrece ninguna reduccion de dosis");
    }

    @Test
    @DisplayName("8. U09: S50 VARIANTE con sustituto de la misma dosis (72 por 1106/323) y S51 con reps -1 que caben")
    void casoU09() {
        var f = FactibilidadAdaptacion.calcular(u09().ctx(), u09().der());
        assertEquals(2, f.size());
        assertTrue(f.stream().allMatch(FactibilidadAdaptacion.Sesion::hayCambioFactible));
        assertTrue(f.get(1).opciones().stream().anyMatch(o -> o.exerciseId() == 117L || o.exerciseId() == 323L));
        assertEquals(36L, f.get(0).margen());
    }

    @Test
    @DisplayName("9. U18: margen 150; una serie menos de 1054 retira exactamente 150 s y cabe (igualar el minimo vale)")
    void u18Igualdad() {
        var s = FactibilidadAdaptacion.calcular(u18().ctx(), u18().der()).get(0);
        assertEquals(2040L, s.duracion());
        assertEquals(1890L, s.minimo());
        var serieMenos = s.opciones().stream().filter(o -> o.exerciseId() == 1054L
                && o.despues().equals("2 series de 90 s")).findFirst().orElseThrow();
        assertEquals(150L, serieMenos.segundosRetirados());
        assertTrue(serieMenos.cabe());
        assertTrue(plano(texto(V2, u18(), null)).contains("- 1054 (3 series de 90 s): 3 series de 80 s retira 30 s [cabe];"
                + " 2 series de 90 s retira 150 s [cabe]"));
    }

    @Test
    @DisplayName("10. ACORTAR_O_REORGANIZAR: sin maximo (V20 no obliga en esa sesion)")
    void acortarSinMinimo() {
        var e = escenario("INCREASE_STRENGTH", "INTERMEDIATE", 60, catalogoFuerza(), null,
                45L, TipoIntervencion.ACORTAR_O_REORGANIZAR, sesionFuerza());
        var s = FactibilidadAdaptacion.calcular(e.ctx(), e.der()).get(0);
        assertNull(s.margen());
        assertTrue(s.opciones().stream().allMatch(FactibilidadAdaptacion.Opcion::cabe));
        assertTrue(plano(texto(V2, e, null)).contains("en esta sesion el minimo de duracion no obliga"));
    }

    // ======================================================== 11-14: lo que V2 dice frente al validador real

    /** Aplica UNA opcion a una copia de la sesion y devuelve los problemas V20 del validador real. */
    static List<String> v20(Escenario e, long sesion, FactibilidadAdaptacion.Opcion o) {
        var der = e.der().from(sesion).orElseThrow().workout();
        var ej = new ArrayList<PlanDraft.DraftExercise>();
        for (var x : der.exercises())
            ej.add(!x.exerciseId().equals(o.exerciseId()) ? x : aplicar(x, o.despues()));
        var draft = new PlanDraft(GenerationSource.AI, "m", "p", null, "r", List.of(new PlanDraft.DraftWorkout(
                der.scheduledDate(), der.focus(), der.name(), der.expectedDurationMinutes(), ej)));
        try {
            VALIDADOR.validate(draft, e.ctx(), 30, ConservationReport.vacio());
            return List.of();
        } catch (InvalidPlanDraftException ex) {
            return ex.problems().stream().filter(p -> p.startsWith("V20")).toList();
        }
    }

    static PlanDraft.DraftExercise aplicar(PlanDraft.DraftExercise x, String dosis) {
        var m = java.util.regex.Pattern.compile("(\\d+)x(\\d+)").matcher(dosis);
        if (m.matches())
            return new PlanDraft.DraftExercise(x.exerciseId(), x.prescriptionType(), Short.valueOf(m.group(1)),
                    Short.valueOf(m.group(2)), null, null, x.restSeconds(), null);
        var d = java.util.regex.Pattern.compile("(\\d+) series? de (\\d+) s").matcher(dosis);
        assertTrue(d.matches(), dosis);
        return new PlanDraft.DraftExercise(x.exerciseId(), x.prescriptionType(), Short.valueOf(d.group(1)), null,
                Integer.valueOf(d.group(2)), null, x.restSeconds(), null);
    }

    @Test
    @DisplayName("11. Coherencia con V20 REAL: cada opcion [cabe] pasa V20 y cada [NO cabe] la rechaza (6 casos)")
    void coherenciaConV20() {
        int cabe = 0, noCabe = 0;
        for (var e : List.of(u03(), u04(), u08(), u09(), u18(), u19()))
            for (var s : FactibilidadAdaptacion.calcular(e.ctx(), e.der()))
                for (var o : s.opciones()) {
                    var problemas = v20(e, s.plannedWorkoutId(), o);
                    assertEquals(o.cabe(), problemas.isEmpty(), s.plannedWorkoutId() + " " + o + " " + problemas);
                    if (o.cabe()) cabe++;
                    else noCabe++;
                }
        assertTrue(cabe >= 10 && noCabe >= 10, "cabe=" + cabe + " noCabe=" + noCabe);
    }

    @Test
    @DisplayName("12. Los segundos que V2 atribuye a cada opcion son los del estimador del backend")
    void segundosDelEstimador() {
        var est = new SessionDurationEstimator();
        for (var e : List.of(u08(), u18(), u19()))
            for (var s : FactibilidadAdaptacion.calcular(e.ctx(), e.der())) {
                var w = e.der().from(s.plannedWorkoutId()).orElseThrow().workout();
                assertEquals(est.estimateSeconds(w, e.ctx().prescription()), s.duracion());
                assertEquals(PlanDraftValidator.minimumSessionSeconds(e.ctx()), s.minimo());
                for (var o : s.opciones()) {
                    var ej = new ArrayList<PlanDraft.DraftExercise>();
                    for (var x : w.exercises()) ej.add(!x.exerciseId().equals(o.exerciseId()) ? x : aplicar(x, o.despues()));
                    var copia = new PlanDraft.DraftWorkout(w.scheduledDate(), w.focus(), w.name(),
                            w.expectedDurationMinutes(), ej);
                    assertEquals(s.duracion() - est.estimateSeconds(copia, e.ctx().prescription()), o.segundosRetirados());
                }
            }
    }

    @Test
    @DisplayName("13. Las propuestas originales rechazadas por V20 siguen rechazandose; sus subconjuntos validos pasan")
    void propuestasOriginales() {
        // U08 i1: 1106 y 994 a 2x6 + 323 y 487 a 3x8 -> 2184 s (rechazo V20 original)
        var e = u08();
        assertEquals(List.of(2184L), duraciones(e, 45L, Map.of(1106L, "2x6", 994L, "2x6", 323L, "3x8", 487L, "3x8")));
        assertFalse(v20Con(e, 45L, Map.of(1106L, "2x6", 994L, "2x6", 323L, "3x8", 487L, "3x8")).isEmpty());
        // su subconjunto 323 y 487 a 3x8: 2520 s, igual al minimo -> V20 lo acepta
        assertEquals(List.of(2520L), duraciones(e, 45L, Map.of(323L, "3x8", 487L, "3x8")));
        assertTrue(v20Con(e, 45L, Map.of(323L, "3x8", 487L, "3x8")).isEmpty());
        // U18 i1 (1720 s) rechazada; sin el cambio de 1054 (1930 s) pasa
        var u18 = u18();
        var i1 = new LinkedHashMap<Long, String>(Map.of(101L, "2x8", 1097L, "2x8", 775L, "2x8", 1334L, "2x12",
                1308L, "2x12", 1076L, "2x12", 1188L, "2 series de 20 s"));
        assertTrue(v20Con(u18, 105L, i1).isEmpty());
        assertEquals(List.of(1930L), duraciones(u18, 105L, i1));
        i1.put(1054L, "2 series de 60 s");
        assertEquals(List.of(1720L), duraciones(u18, 105L, i1));
        assertFalse(v20Con(u18, 105L, i1).isEmpty());
    }

    static List<Long> duraciones(Escenario e, long sesion, Map<Long, String> cambios) {
        var w = conCambios(e, sesion, cambios);
        return List.of(new SessionDurationEstimator().estimateSeconds(w, e.ctx().prescription()));
    }

    static PlanDraft.DraftWorkout conCambios(Escenario e, long sesion, Map<Long, String> cambios) {
        var w = e.der().from(sesion).orElseThrow().workout();
        var ej = new ArrayList<PlanDraft.DraftExercise>();
        for (var x : w.exercises()) ej.add(cambios.containsKey(x.exerciseId()) ? aplicar(x, cambios.get(x.exerciseId())) : x);
        return new PlanDraft.DraftWorkout(w.scheduledDate(), w.focus(), w.name(), w.expectedDurationMinutes(), ej);
    }

    static List<String> v20Con(Escenario e, long sesion, Map<Long, String> cambios) {
        var draft = new PlanDraft(GenerationSource.AI, "m", "p", null, "r", List.of(conCambios(e, sesion, cambios)));
        try {
            VALIDADOR.validate(draft, e.ctx(), 30, ConservationReport.vacio());
            return List.of();
        } catch (InvalidPlanDraftException ex) {
            return ex.problems().stream().filter(p -> p.startsWith("V20")).toList();
        }
    }

    @Test
    @DisplayName("14. V2 nunca ofrece bajar de los pisos (series 2, min_reps, 20 s) ni toca el descanso")
    void respetaPisos() {
        for (var e : List.of(u08(), u09(), u18(), u19()))
            for (var s : FactibilidadAdaptacion.calcular(e.ctx(), e.der()))
                for (var o : s.opciones()) {
                    var x = aplicar(e.der().from(s.plannedWorkoutId()).orElseThrow().workout().exercises().stream()
                            .filter(y -> y.exerciseId().equals(o.exerciseId())).findFirst().orElseThrow(), o.despues());
                    assertTrue(x.plannedSets() >= PlanDraftValidator.MIN_SETS, o.toString());
                    if (x.plannedReps() != null) assertTrue(x.plannedReps() >= 6, o.toString());
                    if (x.plannedDurationSeconds() != null)
                        assertTrue(x.plannedDurationSeconds() >= PlanDraftValidator.MIN_DURATION_SECONDS, o.toString());
                }
    }

    // ======================================================== 15-18: sustitutos, reintento, carga

    @Test
    @DisplayName("15. Los sustitutos de V2 son exactamente los que V1 imprime en SUSTITUTOS ADMITIDOS")
    void mismosSustitutosQueV1() {
        for (var e : List.of(u04(), u09(), u19())) {
            var v1 = texto(V1, e, null);
            for (var s : FactibilidadAdaptacion.calcular(e.ctx(), e.der())) {
                if (!s.intervencion().permiteSustituirEjercicio()) continue;
                var propia = e.der().from(s.plannedWorkoutId()).orElseThrow().workout();
                FactibilidadAdaptacion.sustitutosAdmitidos(e.ctx(), e.der(), s.plannedWorkoutId(), propia)
                        .forEach((id, lista) -> assertTrue(v1.contains("    " + id + " (") && v1.contains(
                                (lista.isEmpty() ? "ninguno" : lista.toString()) + "\n"), id + " " + lista));
            }
        }
    }

    @Test
    @DisplayName("16. Reintento por V20: V2 cambia la pista (devolver trabajo); V1 conserva la suya")
    void reintentoV20() {
        var fb = new AiRetryFeedback("{\"sessions\":[]}", List.of("V20: el entrenamiento del 2026-11-13 dura unos 2184 s y el minimo es 2520 s"
                + " (70 % de 60 minutos): le faltan 336 s. Agrega un ejercicio del enfoque o una serie a los que ya"
                + " tiene, no descanso."), List.of());
        var v1 = plano(texto(V1, u08(), fb));
        var v2 = plano(texto(V2, u08(), fb));
        assertTrue(v1.contains("V20: mira DURACION de esa sesion; si no puedes llegar al minimo con lo que la"
                + " intervencion permite, OMITELA."));
        assertFalse(v2.contains("V20: mira DURACION de esa sesion"));
        assertTrue(v2.contains("Devuelve trabajo que TU quitaste"));
        assertTrue(v2.contains("le faltan 336 s"), "el mensaje del validador viaja igual");
    }

    @Test
    @DisplayName("17. Con carga prescrita, REVISAR_CARGA puede bajarla (aun en GAIN_MUSCLE); sin ella, no se ofrece")
    void carga() {
        var semana = new PreviousWeekSummary(new BigDecimal("50"), null, 0, Map.of(), List.of(), List.of(
                new PreviousWeekSummary.PrescriptionOutcome(15L, S.minusDays(3), 380L, "e", "SETS_REPS", (short) 3,
                        (short) 10, null, new BigDecimal("20.0"), null, null, null, null, null, "SKIPPED", "FATIGUE",
                        "SKIPPED", (short) 90)), List.of());
        var e = escenario("GAIN_MUSCLE", "INTERMEDIATE", 60, catalogoGm(), semana,
                15L, TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION, sesionGm());
        var s = FactibilidadAdaptacion.calcular(e.ctx(), e.der()).get(0);
        assertEquals(List.of(380L), s.cargasReducibles());
        assertTrue(s.hayCambioFactible());
        assertTrue(plano(texto(V2, e, null)).contains("carga: puedes bajar target_load_kg (no cambia la duracion) en [380]"));
        assertTrue(FactibilidadAdaptacion.calcular(u03().ctx(), u03().der()).get(0).cargasReducibles().isEmpty());
    }

    @Test
    @DisplayName("18. Sin sesiones para la IA, V2 no anade nada por sesion y el error de siempre se conserva")
    void sinSenaladas() {
        var e = u08();
        var sin = new PlanGenerationContext(1L, (short) 2, S, S.plusDays(6), e.ctx().profile(), PlanAdjustment.none(),
                null, e.ctx().availableExercises(), SafetyProfile.none(), P);
        assertTrue(FactibilidadAdaptacion.calcular(sin, e.der()).isEmpty());
        assertThrows(IllegalArgumentException.class,
                () -> V2.build(sin, e.der(), PlanInputSnapshotFixture.snapshot(), null));
    }
}
