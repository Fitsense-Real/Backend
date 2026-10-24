package main.web.services.fitsense.planning.domain.services.internal;

import main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.CatalogEligibilityPolicy;
import main.web.services.fitsense.planning.domain.services.DoseEnforcement;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.SessionDurationEstimator;
import main.web.services.fitsense.planning.domain.services.SlotCoverageEnforcement;
import main.web.services.fitsense.planning.infrastructure.generation.rules.RuleBasedTrainingPlanGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Paso 2: lo que el catalogo admite como complemento de capacidad, V15 no lo
 * rechaza ni lo cuenta, y el motor de reglas no lo elige. Para lo que ya era
 * elegible, nada cambia.
 */
class CatalogoV15CoherenciaTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 10, 12);

    private static CandidateExercise ej(long id, String nombre, String zona, String patron,
                                        PrescriptionType tipo) {
        return new CandidateExercise(id, nombre, zona, "body weight", 1, tipo, null, patron);
    }

    private static CandidateExercise fuerza(long id, String zona, String patron) {
        return ej(id, "ejercicio " + id, zona, patron, PrescriptionType.SETS_REPS);
    }

    /** Material de fuerza para todos los enfoques: tres por zona. */
    private static final List<CandidateExercise> BASE = Stream.of(
            fuerza(1, "chest", "HORIZONTAL_PUSH"), fuerza(2, "chest", "HORIZONTAL_PUSH"), fuerza(3, "chest", "HORIZONTAL_PUSH"),
            fuerza(11, "back", "HORIZONTAL_PULL"), fuerza(12, "back", "VERTICAL_PULL"), fuerza(13, "back", "HORIZONTAL_PULL"),
            fuerza(21, "upper legs", "KNEE_DOMINANT"), fuerza(22, "upper legs", "HIP_HINGE"), fuerza(23, "upper legs", "HIP_EXTENSION"),
            fuerza(31, "lower legs", "ANKLE_PLANTAR_FLEXION"), fuerza(32, "lower legs", "ANKLE_PLANTAR_FLEXION"), fuerza(33, "lower legs", "ANKLE_PLANTAR_FLEXION"),
            fuerza(41, "shoulders", "VERTICAL_PUSH"), fuerza(42, "shoulders", "SHOULDER_RAISE"), fuerza(43, "shoulders", "SHOULDER_RAISE"),
            fuerza(51, "upper arms", "ELBOW_EXTENSION"), fuerza(52, "upper arms", "ELBOW_FLEXION"), fuerza(53, "upper arms", "ELBOW_EXTENSION"),
            fuerza(61, "waist", "TRUNK_FLEXION"), fuerza(62, "waist", "CORE_STABILITY"), fuerza(63, "waist", "TRUNK_FLEXION")
    ).toList();

    private static final CandidateExercise CARDIO =
            ej(901, "Saltos de tijera", "cardio", "LOCOMOTION_CARDIO", PrescriptionType.DURATION);
    private static final CandidateExercise CARDIO_2 =
            ej(902, "Trote en el sitio", "cardio", "LOCOMOTION_CARDIO", PrescriptionType.DURATION);
    private static final CandidateExercise ESTIRAMIENTO_MUSLO =
            ej(903, "Estiramiento de cuadriceps", "upper legs", "MOBILITY", PrescriptionType.DURATION);
    private static final CandidateExercise ESTIRAMIENTO_GEMELO =
            ej(904, "Estiramiento de gemelo", "lower legs", "MOBILITY", PrescriptionType.DURATION);
    private static final CandidateExercise MOVILIDAD_YA_ELEGIBLE =
            ej(55, "Circulos de tobillo", "lower legs", "MOBILITY", PrescriptionType.DURATION);

    private static final List<CandidateExercise> CANDIDATOS_NUEVOS =
            List.of(CARDIO, CARDIO_2, ESTIRAMIENTO_MUSLO, ESTIRAMIENTO_GEMELO);

    /** Lo que el ACL dejaria pasar para ese objetivo. */
    private static List<CandidateExercise> elegiblesPara(String objetivo, List<CandidateExercise> extra) {
        var todos = new ArrayList<>(BASE);
        todos.addAll(extra);
        return todos.stream()
                .filter(c -> CatalogEligibilityPolicy.decidir(c, objetivo).admite())
                .toList();
    }

    private static PlanningProfile perfil(String objetivo, int dias, List<Short> diasDisponibles) {
        return new PlanningProfile(7L, 35, "FEMALE", new BigDecimal("165"), new BigDecimal("70"),
                null, "INTERMEDIATE", objetivo, "prueba", "Ninguna", "HOME", dias,
                diasDisponibles, 45, List.of("body weight"), List.of(),
                SafetyProfile.Declaradas.sinLimitaciones());
    }

    private static PlanGenerationContext contexto(String objetivo, List<CandidateExercise> elegibles,
                                                  int dias, List<Short> diasDisponibles) {
        return new PlanGenerationContext(7L, (short) 1, LUNES, LUNES.plusDays(6),
                perfil(objetivo, dias, diasDisponibles), PlanAdjustment.none(),
                PreviousWeekSummary.empty(), elegibles, SafetyProfile.none(), (PrescriptionParams) null);
    }

    private static PlanDraft.DraftExercise prescrito(CandidateExercise c) {
        return c.defaultPrescription() == PrescriptionType.DURATION
                ? new PlanDraft.DraftExercise(c.exerciseId(), PrescriptionType.DURATION,
                (short) 2, null, 30, null, (short) 30, null)
                : new PlanDraft.DraftExercise(c.exerciseId(), PrescriptionType.SETS_REPS,
                (short) 3, (short) 12, null, null, (short) 60, null);
    }

    private static PlanDraft unaSesion(WorkoutFocus enfoque, List<CandidateExercise> ejercicios) {
        var sesion = new PlanDraft.DraftWorkout(LUNES, enfoque, enfoque.name(), 45,
                ejercicios.stream().map(CatalogoV15CoherenciaTest::prescrito).toList());
        return new PlanDraft(GenerationSource.AI, "modelo", "plan", null, "prueba", List.of(sesion));
    }

    private static PlanDraftValidator validador() {
        return new PlanDraftValidator(new SessionDurationEstimator(), SlotCoverageEnforcement.OFF,
                DoseEnforcement.OFF);
    }

    private static List<String> problemasV15(String objetivo, WorkoutFocus enfoque,
                                             List<CandidateExercise> sesion) {
        var ctx = contexto(objetivo, elegiblesPara(objetivo, CANDIDATOS_NUEVOS), 1, List.of((short) 1));
        try {
            validador().validate(unaSesion(enfoque, sesion), ctx, 30);
            return List.of();
        } catch (InvalidPlanDraftException e) {
            return e.problems().stream().filter(p -> p.startsWith("V15")).toList();
        }
    }

    private static List<CandidateExercise> dosDelEnfoque(WorkoutFocus enfoque) {
        return BASE.stream().filter(c -> enfoque.accepts(c.bodyPartCode()))
                .collect(java.util.stream.Collectors.toMap(CandidateExercise::bodyPartCode, c -> c,
                        (a, b) -> a, java.util.LinkedHashMap::new))
                .values().stream().limit(2).toList();
    }

    /**
     * La propiedad central: todo complemento que el catalogo admite para un
     * objetivo cabe en cualquier enfoque sin que V15 proteste.
     */
    @ParameterizedTest(name = "{0}")
    @EnumSource(WorkoutFocus.class)
    void cadaComplementoAdmitidoEsAceptadoPorV15EnCualquierEnfoque(WorkoutFocus enfoque) {
        for (var objetivo : List.of("LOSE_WEIGHT", "GENERAL_FITNESS")) {
            var admitidos = elegiblesPara(objetivo, CANDIDATOS_NUEVOS).stream()
                    .filter(c -> CatalogEligibilityPolicy.esComplemento(c, objetivo)).toList();
            assertFalse(admitidos.isEmpty(), objetivo);
            for (var complemento : admitidos) {
                var sesion = new ArrayList<>(dosDelEnfoque(enfoque));
                sesion.add(complemento);
                assertEquals(List.of(), problemasV15(objetivo, enfoque, sesion),
                        objetivo + " " + enfoque + " + " + complemento.name());
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"GAIN_MUSCLE", "INCREASE_STRENGTH"})
    void enObjetivosSinCardioElCatalogoNoLoAdmite(String objetivo) {
        var elegibles = elegiblesPara(objetivo, CANDIDATOS_NUEVOS);
        assertTrue(elegibles.stream().noneMatch(c -> CANDIDATOS_NUEVOS.contains(c)), objetivo);
        assertEquals(BASE, elegibles);
    }

    @Test
    void loseWeightRecibeCardioPeroNoEstiramientos() {
        var elegibles = elegiblesPara("LOSE_WEIGHT", CANDIDATOS_NUEVOS);
        assertTrue(elegibles.contains(CARDIO) && elegibles.contains(CARDIO_2));
        assertFalse(elegibles.contains(ESTIRAMIENTO_MUSLO) || elegibles.contains(ESTIRAMIENTO_GEMELO));
    }

    @Test
    void generalFitnessRecibeCardioYMovilidad() {
        assertTrue(elegiblesPara("GENERAL_FITNESS", CANDIDATOS_NUEVOS).containsAll(CANDIDATOS_NUEVOS));
    }

    /** Un estiramiento no pasa por trabajo de piernas: no cuenta para cubrir el enfoque. */
    @Test
    void unComplementoNoCuentaParaCubrirElEnfoque() {
        var calf1 = BASE.stream().filter(c -> c.exerciseId() == 31).findFirst().orElseThrow();
        var calf2 = BASE.stream().filter(c -> c.exerciseId() == 32).findFirst().orElseThrow();
        var problemas = problemasV15("GENERAL_FITNESS", WorkoutFocus.LEGS,
                List.of(ESTIRAMIENTO_MUSLO, ESTIRAMIENTO_GEMELO, calf1, calf2));
        assertEquals(1, problemas.size(), problemas.toString());
        assertTrue(problemas.get(0).contains("Debe cubrir al menos 2"), problemas.get(0));
    }

    /** Un ejercicio que ya era elegible se sigue contando como antes, tambien en GENERAL_FITNESS. */
    @ParameterizedTest
    @ValueSource(strings = {"GAIN_MUSCLE", "GENERAL_FITNESS"})
    void loYaElegibleSigueContandoComoAntes(String objetivo) {
        var muslo = BASE.stream().filter(c -> c.exerciseId() == 21).findFirst().orElseThrow();
        var ctx = contexto(objetivo, Stream.concat(BASE.stream(), Stream.of(MOVILIDAD_YA_ELEGIBLE)).toList(),
                1, List.of((short) 1));
        assertDoesNotThrow(() -> validador().validate(
                unaSesion(WorkoutFocus.LEGS, List.of(muslo, MOVILIDAD_YA_ELEGIBLE)), ctx, 30));
    }

    /** Fuera de los complementos admitidos, V15 sigue rechazando zonas ajenas como antes. */
    @Test
    void v15SigueRechazandoZonasAjenas() {
        var pecho = BASE.get(0);
        var muslo = BASE.stream().filter(c -> c.exerciseId() == 21).findFirst().orElseThrow();
        var problemas = problemasV15("LOSE_WEIGHT", WorkoutFocus.LEGS, List.of(muslo, pecho));
        assertTrue(problemas.stream().anyMatch(p -> p.contains("incluye ejercicios de [chest]")), problemas.toString());
    }

    /**
     * Objetivos sin cardio ni movilidad en su matriz: el motor de reglas no
     * elige complementos y, con la misma semilla, produce EXACTAMENTE la misma
     * semana con y sin ellos en el conjunto elegible.
     */
    @ParameterizedTest
    @ValueSource(strings = {"GAIN_MUSCLE", "INCREASE_STRENGTH"})
    void elMotorDeReglasProduceLaMismaSemana(String objetivo) {
        var generador = new RuleBasedTrainingPlanGenerator(30, new SessionDurationEstimator());
        var dias = List.of((short) 1, (short) 3, (short) 5);
        var sin = generador.generate(contexto(objetivo, BASE, 3, dias), List.of());
        var con = generador.generate(contexto(objetivo, elegiblesPara(objetivo, CANDIDATOS_NUEVOS), 3, dias),
                List.of());
        assertEquals(sin, con, objetivo);
        var ids = con.workouts().stream().flatMap(w -> w.exercises().stream())
                .map(PlanDraft.DraftExercise::exerciseId).toList();
        CANDIDATOS_NUEVOS.forEach(c -> assertFalse(ids.contains(c.exerciseId()), c.name()));
    }

    /**
     * 4A v6 + 4D v4 (sustituye a la caracterizacion del Paso 2 para estos dos
     * objetivos): CARDIO es principal de LOSE_WEIGHT y GENERAL_FITNESS, y
     * MOVILIDAD de GENERAL_FITNESS. El motor de reglas YA NO los omite: usa los
     * complementos verificados, y nunca los que estan en revision de
     * clasificacion (902, 903).
     */
    @ParameterizedTest
    @ValueSource(strings = {"LOSE_WEIGHT", "GENERAL_FITNESS"})
    void elMotorDeReglasIncluyeLasPrincipalesVerificadas(String objetivo) {
        var generador = new RuleBasedTrainingPlanGenerator(30, new SessionDurationEstimator());
        var dias = List.of((short) 1, (short) 3, (short) 5);
        var semana = generador.generate(contexto(objetivo, elegiblesPara(objetivo, CANDIDATOS_NUEVOS), 3, dias),
                List.of());
        var ids = semana.workouts().stream().flatMap(w -> w.exercises().stream())
                .map(PlanDraft.DraftExercise::exerciseId).toList();
        assertTrue(ids.contains(CARDIO.exerciseId()), objetivo + ": falta el cardio verificado " + ids);
        if ("GENERAL_FITNESS".equals(objetivo))
            assertTrue(ids.contains(ESTIRAMIENTO_GEMELO.exerciseId()), "falta la movilidad verificada " + ids);
        assertFalse(ids.contains(CARDIO_2.exerciseId()), "902 esta en revision de clasificacion");
        assertFalse(ids.contains(ESTIRAMIENTO_MUSLO.exerciseId()), "903 esta en revision de clasificacion");
    }
}
