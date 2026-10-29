package main.web.services.fitsense.planning.domain.services.internal;

import main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.DoseEnforcement;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.SessionDurationEstimator;
import main.web.services.fitsense.planning.domain.services.SlotCoverageEnforcement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V15 · un grupo cuenta como DISPONIBLE solo si es prescribible para el objetivo.
 * <p>
 * Antes, V15 exigia dos grupos en una sesion si el conjunto elegible tenia
 * ejercicios de ambos, aunque la politica de capacidades del objetivo no
 * permitiera prescribir ninguno de uno de ellos. En LOSE_WEIGHT la unica
 * capacidad resistida es la fuerza, con dosis solo para estructurales, y un
 * gemelo (flexion plantar, RESISTIDO) no tiene ninguna: V15 exigia un grupo
 * que V24/V25 no dejan poner (CORRECCION_RESPALDO_5_6_DIAS.md).
 * <p>
 * Se prueba el validador tal como va en produccion para la generacion
 * (capacidades en ENFORCE) y tal como lo construye la adaptacion (sin
 * capacidades), y con propuestas sinteticas de la IA: V15 conserva su
 * proteccion en todo lo demas.
 */
@DisplayName("V15 · cuenta como disponible solo lo que el objetivo puede prescribir")
class V15GruposPrescribiblesTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 11, 2);
    private static final PrescriptionParams PARAMETROS =
            new PrescriptionParams(null, null, null, null, null, null, null, null, null, null);

    // Catalogo minimo: estructurales de muslo, gemelos (RESISTIDO), espalda, biceps y cardio.
    static final CandidateExercise SENTADILLA = ej(1, "upper legs", "KNEE_DOMINANT", null);
    static final CandidateExercise ZANCADA = ej(2, "upper legs", "KNEE_DOMINANT", null);
    static final CandidateExercise PESO_MUERTO = ej(3, "upper legs", "HIP_HINGE", null);
    static final CandidateExercise SENTADILLA_2 = ej(4, "upper legs", "KNEE_DOMINANT", null);
    static final CandidateExercise GEMELO = ej(11, "lower legs", "ANKLE_PLANTAR_FLEXION", null);
    static final CandidateExercise GEMELO_2 = ej(12, "lower legs", "ANKLE_PLANTAR_FLEXION", null);
    static final CandidateExercise ABDOMINAL = ej(13, "waist", "TRUNK_FLEXION", null);
    static final CandidateExercise REMO = ej(21, "back", "HORIZONTAL_PULL", null);
    static final CandidateExercise JALON = ej(22, "back", "VERTICAL_PULL", null);
    static final CandidateExercise DOMINADA_SUPINA = ej(31, "upper arms", "VERTICAL_PULL", "biceps");
    static final CandidateExercise CURL = ej(32, "upper arms", "ELBOW_FLEXION", "biceps");
    static final CandidateExercise PRESS = ej(41, "chest", "HORIZONTAL_PUSH", null);
    static final CandidateExercise CARDIO = new CandidateExercise(91L, "trote", "cardio", "body weight", 1,
            PrescriptionType.DURATION, "cardiovascular system", "LOCOMOTION_CARDIO");

    static CandidateExercise ej(long id, String zona, String patron, String musculo) {
        return new CandidateExercise(id, "ejercicio " + id, zona, "body weight", 1, PrescriptionType.SETS_REPS,
                musculo, patron);
    }

    /** Como en produccion para la generacion: dosis y capacidades bloquean. */
    static PlanDraftValidator produccion() {
        return new PlanDraftValidator(new SessionDurationEstimator(), SlotCoverageEnforcement.OFF,
                DoseEnforcement.OFF, DoseEnforcement.ENFORCE);
    }

    /** Como lo construye la adaptacion P2-D (copia semanal): sin capacidades. */
    static PlanDraftValidator sinCapacidades() {
        return new PlanDraftValidator(new SessionDurationEstimator(), SlotCoverageEnforcement.OFF,
                DoseEnforcement.OFF);
    }

    static PlanGenerationContext contexto(String objetivo, List<CandidateExercise> elegibles) {
        var perfil = new PlanningProfile(7L, 35, "FEMALE", new BigDecimal("165"), new BigDecimal("70"), null,
                "INTERMEDIATE", objetivo, "prueba", "Ninguna", "GYM", 1, List.of((short) 1), 45,
                List.of("body weight"), List.of(), SafetyProfile.Declaradas.sinLimitaciones());
        return new PlanGenerationContext(7L, (short) 1, LUNES, LUNES.plusDays(6), perfil, PlanAdjustment.none(),
                PreviousWeekSummary.empty(), elegibles, SafetyProfile.none(), PARAMETROS);
    }

    static PlanDraft sesion(GenerationSource origen, WorkoutFocus enfoque, List<CandidateExercise> ejercicios) {
        var lista = new ArrayList<PlanDraft.DraftExercise>();
        for (var c : ejercicios)
            lista.add(c.defaultPrescription() == PrescriptionType.DURATION
                    ? new PlanDraft.DraftExercise(c.exerciseId(), PrescriptionType.DURATION, (short) 3, null, 120,
                    null, (short) 60, null)
                    : new PlanDraft.DraftExercise(c.exerciseId(), PrescriptionType.SETS_REPS, (short) 3, (short) 8,
                    null, null, (short) 120, null));
        return new PlanDraft(origen, null, "prueba", null, "r",
                List.of(new PlanDraft.DraftWorkout(LUNES, enfoque, enfoque.name(), 45, lista)));
    }

    static List<String> problemas(PlanDraftValidator v, String prefijo, String objetivo, List<CandidateExercise> elegibles,
                                  GenerationSource origen, WorkoutFocus enfoque, List<CandidateExercise> ejercicios) {
        try {
            v.validate(sesion(origen, enfoque, ejercicios), contexto(objetivo, elegibles), 30);
            return List.of();
        } catch (InvalidPlanDraftException e) {
            return e.problems().stream().filter(p -> p.startsWith(prefijo)).toList();
        }
    }

    static List<String> v15(PlanDraftValidator v, String objetivo, List<CandidateExercise> elegibles,
                            GenerationSource origen, WorkoutFocus enfoque, List<CandidateExercise> ejercicios) {
        return problemas(v, "V15", objetivo, elegibles, origen, enfoque, ejercicios);
    }

    static final List<CandidateExercise> CATALOGO = List.of(SENTADILLA, ZANCADA, PESO_MUERTO, SENTADILLA_2, GEMELO,
            GEMELO_2, ABDOMINAL, REMO, JALON, DOMINADA_SUPINA, CURL, PRESS, CARDIO);

    // ----------------------------------------------------------------- el defecto

    @Nested
    @DisplayName("El defecto: LOSE_WEIGHT no puede prescribir gemelos")
    class Defecto {

        @ParameterizedTest(name = "origen {0}")
        @ValueSource(strings = {"RULE_ENGINE", "AI"})
        @DisplayName("piernas solo con muslo ya no se rechaza por un grupo imposible")
        void piernasSinGemeloImposible(GenerationSource origen) {
            assertEquals(List.of(), v15(produccion(), "LOSE_WEIGHT", CATALOGO, origen, WorkoutFocus.LEGS,
                    List.of(SENTADILLA, PESO_MUERTO, CARDIO)));
        }

        @Test
        @DisplayName("tren inferior de LOSE_WEIGHT: el abdomen RESISTIDO tampoco es prescribible")
        void trenInferiorSoloMuslo() {
            assertEquals(List.of(), v15(produccion(), "LOSE_WEIGHT", CATALOGO, GenerationSource.RULE_ENGINE,
                    WorkoutFocus.LOWER_BODY, List.of(SENTADILLA, PESO_MUERTO, CARDIO)));
        }

        @Test
        @DisplayName("traccion de LOSE_WEIGHT con solo curl (RESISTIDO) disponible en brazos: basta la espalda")
        void traccionSinBicepsEstructural() {
            var sinDominada = CATALOGO.stream().filter(c -> c != DOMINADA_SUPINA).toList();
            assertEquals(List.of(), v15(produccion(), "LOSE_WEIGHT", sinDominada, GenerationSource.RULE_ENGINE,
                    WorkoutFocus.PULL, List.of(REMO, JALON, CARDIO)));
        }
    }

    // ----------------------------------------------------- V15 conserva su proteccion

    @Nested
    @DisplayName("V15 conserva su proteccion")
    class Proteccion {

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"GAIN_MUSCLE", "GENERAL_FITNESS", "INCREASE_STRENGTH"})
        @DisplayName("si el objetivo SI puede prescribir gemelos, piernas solo con muslo sigue rechazandose")
        void gemeloPrescribibleSigueExigido(String objetivo) {
            var p = v15(produccion(), objetivo, CATALOGO, GenerationSource.AI, WorkoutFocus.LEGS,
                    List.of(SENTADILLA, PESO_MUERTO));
            assertEquals(1, p.size(), p.toString());
            assertTrue(p.get(0).contains("Debe cubrir al menos 2"), p.get(0));
        }

        @Test
        @DisplayName("LOSE_WEIGHT: traccion solo de espalda con un biceps estructural disponible se rechaza")
        void bicepsEstructuralSigueExigido() {
            var p = v15(produccion(), "LOSE_WEIGHT", CATALOGO, GenerationSource.AI, WorkoutFocus.PULL,
                    List.of(REMO, JALON, CARDIO));
            assertEquals(1, p.size(), p.toString());
            assertTrue(p.get(0).contains("Debe cubrir al menos 2"), p.get(0));
        }

        @Test
        @DisplayName("propuesta de IA: un grupo ajeno al enfoque sigue rechazandose")
        void grupoAjeno() {
            var p = v15(produccion(), "LOSE_WEIGHT", CATALOGO, GenerationSource.AI, WorkoutFocus.LEGS,
                    List.of(SENTADILLA, PRESS, CARDIO));
            assertTrue(p.stream().anyMatch(x -> x.contains("incluye ejercicios de [chest]")), p.toString());
        }

        @Test
        @DisplayName("propuesta de IA: un grupo por encima de la mitad sigue rechazandose")
        void masDeLaMitad() {
            var p = v15(produccion(), "GENERAL_FITNESS", CATALOGO, GenerationSource.AI, WorkoutFocus.LOWER_BODY,
                    List.of(SENTADILLA, ZANCADA, PESO_MUERTO, SENTADILLA_2, GEMELO, ABDOMINAL));
            assertTrue(p.stream().anyMatch(x -> x.contains("dedica 4 de 6 ejercicios a upper legs")), p.toString());
        }

        @Test
        @DisplayName("copia semanal (validador sin capacidades): el mismo criterio que la generacion")
        void copiaSemanalMismoCriterio() {
            // Antes, este validador exigia gemelos a LOSE_WEIGHT y la semana 2 quedaba sin plan.
            var sesion = List.of(SENTADILLA, PESO_MUERTO, CARDIO);
            assertEquals(List.of(), v15(sinCapacidades(), "LOSE_WEIGHT", CATALOGO, GenerationSource.RULE_ENGINE,
                    WorkoutFocus.LEGS, sesion));
            assertEquals(v15(produccion(), "LOSE_WEIGHT", CATALOGO, GenerationSource.RULE_ENGINE, WorkoutFocus.LEGS,
                    sesion), v15(sinCapacidades(), "LOSE_WEIGHT", CATALOGO, GenerationSource.RULE_ENGINE,
                    WorkoutFocus.LEGS, sesion));
        }

        @org.junit.jupiter.params.ParameterizedTest(name = "{0}, generacion={1}")
        @org.junit.jupiter.params.provider.CsvSource({
                "GAIN_MUSCLE, true", "GAIN_MUSCLE, false",
                "INCREASE_STRENGTH, true", "INCREASE_STRENGTH, false",
                "GENERAL_FITNESS, true", "GENERAL_FITNESS, false"})
        @DisplayName("los demas objetivos, en generacion y en copia, siguen exigiendo los dos grupos prescribibles")
        void demasObjetivosSiguenExigiendo(String objetivo, boolean generacion) {
            var v = generacion ? produccion() : sinCapacidades();
            var p = v15(v, objetivo, CATALOGO, GenerationSource.RULE_ENGINE, WorkoutFocus.LEGS,
                    List.of(SENTADILLA, PESO_MUERTO, SENTADILLA_2));
            assertEquals(1, p.size(), p.toString());
            assertTrue(p.get(0).contains("Debe cubrir al menos 2"), p.get(0));
        }

        @Test
        @DisplayName("copia semanal: un grupo ajeno y un grupo por encima de la mitad se siguen rechazando")
        void copiaSemanalConservaLasOtrasReglas() {
            var ajeno = v15(sinCapacidades(), "LOSE_WEIGHT", CATALOGO, GenerationSource.RULE_ENGINE,
                    WorkoutFocus.LEGS, List.of(SENTADILLA, PESO_MUERTO, REMO));
            assertTrue(ajeno.stream().anyMatch(x -> x.contains("incluye ejercicios de [back]")), ajeno.toString());
            var tiron = v15(sinCapacidades(), "LOSE_WEIGHT", CATALOGO, GenerationSource.RULE_ENGINE,
                    WorkoutFocus.PULL, List.of(REMO, JALON));
            assertTrue(tiron.stream().anyMatch(x -> x.contains("Debe cubrir al menos 2")), tiron.toString());
        }

        @Test
        @DisplayName("la sesion admitida sigue sometida a V25: sin la capacidad principal (cardio) se rechaza")
        void sinCapacidadPrincipal() {
            var p = problemas(produccion(), "V25", "LOSE_WEIGHT", CATALOGO, GenerationSource.AI,
                    WorkoutFocus.LEGS, List.of(SENTADILLA, PESO_MUERTO));
            assertFalse(p.isEmpty(), "V25 debe exigir el cardio de LOSE_WEIGHT");
        }

        @Test
        @DisplayName("un gemelo puesto en LOSE_WEIGHT lo sigue rechazando V24 (sin capacidad del objetivo)")
        void gemeloForzadoRechazado() {
            var p = problemas(produccion(), "V24", "LOSE_WEIGHT", CATALOGO, GenerationSource.AI,
                    WorkoutFocus.LEGS, List.of(SENTADILLA, GEMELO, CARDIO));
            assertFalse(p.isEmpty(), "poner un gemelo para cumplir V15 no debe ser una salida");
        }
    }
}
