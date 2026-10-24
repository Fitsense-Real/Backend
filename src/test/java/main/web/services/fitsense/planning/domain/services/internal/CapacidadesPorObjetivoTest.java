package main.web.services.fitsense.planning.domain.services.internal;

import main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.model.valueobjects.GoalCapabilityPolicy.Capability;
import main.web.services.fitsense.planning.domain.services.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 4D v4 + 4A v6: la dosis por capacidad, la cobertura de la semana y las
 * validaciones V23/V24/V25 que las exigen. Datos sinteticos, sin base.
 */
class CapacidadesPorObjetivoTest {

    static final LocalDate LUNES = LocalDate.of(2026, 10, 26);
    static final PrescriptionParams.RepLimits LIMITES = new PrescriptionParams.RepLimits(6, 30,
            Map.of("waist", 10, "lower legs", 12));

    static CandidateExercise ej(long id, String zona, String patron, PrescriptionType tipo) {
        return new CandidateExercise(id, "ejercicio " + id, zona, "body weight", 1, tipo, null, patron);
    }

    static CandidateExercise sr(long id, String zona, String patron) {
        return ej(id, zona, patron, PrescriptionType.SETS_REPS);
    }

    static final CandidateExercise SENTADILLA = sr(1, "upper legs", "KNEE_DOMINANT");
    static final CandidateExercise PESO_MUERTO = sr(2, "upper legs", "HIP_HINGE");
    static final CandidateExercise PRESS = sr(3, "chest", "HORIZONTAL_PUSH");
    static final CandidateExercise REMO = sr(4, "back", "HORIZONTAL_PULL");
    static final CandidateExercise CRUNCH = sr(5, "waist", "TRUNK_FLEXION");
    static final CandidateExercise GEMELO = sr(6, "lower legs", "ANKLE_PLANTAR_FLEXION");
    static final CandidateExercise PLANCHA = ej(7, "waist", "CORE_STABILITY", PrescriptionType.DURATION);
    static final CandidateExercise BICI = new CandidateExercise(8L, "Bicicleta", "cardio", "stationary bike", 1,
            PrescriptionType.DURATION, null, "LOCOMOTION_CARDIO");
    static final CandidateExercise ESTIRAMIENTO = new CandidateExercise(9L, "Estiramiento de gemelo", "lower legs",
            "body weight", 1, PrescriptionType.DURATION, null, "MOBILITY");
    /** 1374: en revision de clasificacion. */
    static final CandidateExercise EN_REVISION = sr(1374, "waist", "LOCOMOTION_CARDIO");

    static final List<CandidateExercise> TODOS = List.of(SENTADILLA, PESO_MUERTO, PRESS, REMO, CRUNCH, GEMELO,
            PLANCHA, BICI, ESTIRAMIENTO, EN_REVISION);

    static CapabilityPrescriptionPolicy.Dosis dosis(Capability c, CandidateExercise e, String objetivo, String nivel) {
        return CapabilityPrescriptionPolicy.dosis(c, e, objetivo, nivel, LIMITES).orElseThrow();
    }

    @Nested
    @DisplayName("4D v4: la dosis depende de la capacidad, no hay un esquema unico")
    class Dosis {

        @Test
        @DisplayName("fuerza, hipertrofia y resistencia muscular: tres dosis distintas para el mismo ejercicio")
        void tresDosisDistintas() {
            var f = dosis(Capability.STRENGTH, SENTADILLA, "GENERAL_FITNESS", "INTERMEDIATE");
            var h = dosis(Capability.HYPERTROPHY, SENTADILLA, "GENERAL_FITNESS", "INTERMEDIATE");
            var r = dosis(Capability.MUSCULAR_ENDURANCE, SENTADILLA, "GENERAL_FITNESS", "INTERMEDIATE");
            assertEquals(List.of(3, 6, 150), List.of((int) f.series(), (int) f.reps(), (int) f.descanso()),
                    "3x5 sube al minimo general de 6 (V16)");
            assertEquals(List.of(3, 10, 90), List.of((int) h.series(), (int) h.reps(), (int) h.descanso()));
            assertEquals(List.of(3, 18, 45), List.of((int) r.series(), (int) r.reps(), (int) r.descanso()));
        }

        @Test
        @DisplayName("principiante: fuerza 2x8 y resistencia 2x15; nunca bajo el minimo de zona")
        void principiante() {
            var f = dosis(Capability.STRENGTH, SENTADILLA, "INCREASE_STRENGTH", "BEGINNER");
            assertEquals(List.of(2, 8, 120), List.of((int) f.series(), (int) f.reps(), (int) f.descanso()));
            var r = dosis(Capability.MUSCULAR_ENDURANCE, GEMELO, "GENERAL_FITNESS", "BEGINNER");
            assertEquals(Short.valueOf((short) 15), r.reps());
            var fuerzaGemelo = CapabilityPrescriptionPolicy.dosis(Capability.STRENGTH, GEMELO, "GENERAL_FITNESS",
                    "INTERMEDIATE", LIMITES);
            assertTrue(fuerzaGemelo.isEmpty(), "la fuerza solo se prescribe en ejercicios estructurales");
        }

        @Test
        @DisplayName("cardio, movilidad y equilibrio son de duracion; el cardio no tiene dosis de repeticiones")
        void duracion() {
            var c = dosis(Capability.CARDIO, BICI, "LOSE_WEIGHT", "INTERMEDIATE");
            assertEquals(PrescriptionType.DURATION, c.tipo());
            assertEquals(120, c.segundos());
            assertNotNull(c.nota(), "la intensidad se indica, aunque no se valide");
            assertEquals(Integer.valueOf(90), dosis(Capability.CARDIO, BICI, "LOSE_WEIGHT", "BEGINNER").segundos());
            assertEquals(Integer.valueOf(30), dosis(Capability.MOBILITY, ESTIRAMIENTO, "GENERAL_FITNESS", "BEGINNER").segundos());
            assertTrue(CapabilityPrescriptionPolicy.dosis(Capability.CARDIO, sr(20, "cardio", "LOCOMOTION_CARDIO"),
                    "LOSE_WEIGHT", "INTERMEDIATE", LIMITES).isEmpty());
        }

        @Test
        @DisplayName("GAIN_MUSCLE: la dosis de 4D v3 tal cual y solo HYPERTROPHY asignable")
        void ganarMusculoSinCambios() {
            var h = dosis(Capability.HYPERTROPHY, SENTADILLA, "GAIN_MUSCLE", "BEGINNER");
            var v3 = MvpDosePolicy.dosisPara(MandatorySlot.of(SENTADILLA), "upper legs").orElseThrow();
            assertEquals(List.of(v3.series(), v3.reps(), v3.descansoSegundos()), List.of(h.series(), h.reps(), h.descanso()));
            assertEquals(h.series(), h.seriesMax(), "sin ampliacion en 4D v3");
            assertEquals(Set.of(Capability.HYPERTROPHY), CapabilityPrescriptionPolicy.asignables("GAIN_MUSCLE", "INTERMEDIATE"));
        }

        @Test
        @DisplayName("POWER nunca es asignable: no hay forma verificable de acreditarla")
        void potencia() {
            assertFalse(CapabilityPrescriptionPolicy.asignables("INCREASE_STRENGTH", "ADVANCED").contains(Capability.POWER));
        }

        @Test
        @DisplayName("la capacidad la acredita la dosis exacta; en revision o sin capacidad no acredita nada")
        void acreditacion() {
            var fuerza = dosis(Capability.STRENGTH, PRESS, "GENERAL_FITNESS", "INTERMEDIATE").prescribir(3L, null);
            assertEquals(Optional.of(Capability.STRENGTH),
                    CapabilityPrescriptionPolicy.acreditada(fuerza, PRESS, "GENERAL_FITNESS", "INTERMEDIATE", LIMITES));
            var generica = new PlanDraft.DraftExercise(3L, PrescriptionType.SETS_REPS, (short) 3, (short) 12, null,
                    null, (short) 60, null);
            assertTrue(CapabilityPrescriptionPolicy.acreditada(generica, PRESS, "GENERAL_FITNESS", "INTERMEDIATE", LIMITES)
                    .isEmpty(), "el 3x12/60 generico no demuestra ninguna capacidad");
            assertTrue(CapabilityPrescriptionPolicy.admisibles(EN_REVISION, "LOSE_WEIGHT", "INTERMEDIATE").isEmpty());
            assertTrue(CapabilityPrescriptionPolicy.admisibles(CRUNCH, "LOSE_WEIGHT", "INTERMEDIATE").isEmpty(),
                    "un abdominal no aporta CARDIO ni STRENGTH");
            // La ampliacion a 3 series para llegar al minimo de duracion se acepta.
            var ampliada = dosis(Capability.HYPERTROPHY, CRUNCH, "GENERAL_FITNESS", "INTERMEDIATE")
                    .conSeries((short) 3).orElseThrow().prescribir(5L, null);
            assertTrue(CapabilityPrescriptionPolicy.acreditada(ampliada, CRUNCH, "GENERAL_FITNESS", "INTERMEDIATE",
                    LIMITES).isPresent());
        }
    }

    // =====================================================================

    static PlanDraft semana(PlanDraft.DraftExercise... ejercicios) {
        return new PlanDraft(GenerationSource.AI, "m", "p", null, "r",
                List.of(new PlanDraft.DraftWorkout(LUNES, WorkoutFocus.FULL_BODY, "s", 45, List.of(ejercicios))));
    }

    static PlanDraft.DraftExercise con(Capability c, CandidateExercise e, String objetivo) {
        return dosis(c, e, objetivo, "INTERMEDIATE").prescribir(e.exerciseId(), null);
    }

    @Nested
    @DisplayName("cobertura: COMPLETO, PARCIAL o NO_FACTIBLE, solo por las principales")
    class Cobertura {

        @Test
        @DisplayName("GENERAL_FITNESS con las cuatro principales acreditadas: COMPLETO")
        void completo() {
            var c = CapabilityCoveragePolicy.evaluar("GENERAL_FITNESS", "INTERMEDIATE", semana(
                    con(Capability.STRENGTH, SENTADILLA, "GENERAL_FITNESS"),
                    con(Capability.MUSCULAR_ENDURANCE, REMO, "GENERAL_FITNESS"),
                    con(Capability.CARDIO, BICI, "GENERAL_FITNESS"),
                    con(Capability.MOBILITY, ESTIRAMIENTO, "GENERAL_FITNESS")), TODOS, LIMITES);
            assertEquals(CapabilityCoveragePolicy.Resultado.COMPLETO, c.resultado(), c.describe());
            assertTrue(c.secundariasIncorporadas().isEmpty());
        }

        @Test
        @DisplayName("sin cardio en el conjunto elegible: PARCIAL, cardio no factible y nombrado")
        void parcial() {
            var sinCardio = TODOS.stream().filter(e -> e != BICI).toList();
            var c = CapabilityCoveragePolicy.evaluar("GENERAL_FITNESS", "INTERMEDIATE", semana(
                    con(Capability.STRENGTH, SENTADILLA, "GENERAL_FITNESS"),
                    con(Capability.MUSCULAR_ENDURANCE, REMO, "GENERAL_FITNESS"),
                    con(Capability.MOBILITY, ESTIRAMIENTO, "GENERAL_FITNESS"),
                    con(Capability.HYPERTROPHY, PRESS, "GENERAL_FITNESS")), sinCardio, LIMITES);
            assertEquals(CapabilityCoveragePolicy.Resultado.PARCIAL, c.resultado());
            assertTrue(c.noCubiertas().get(Capability.CARDIO).startsWith("no factible"), c.describe());
            assertTrue(c.omitidas().isEmpty(), "no es una omision: no habia con que");
            assertEquals(Set.of(Capability.HYPERTROPHY), c.secundariasIncorporadas());
            assertNotNull(c.mensaje());
        }

        @Test
        @DisplayName("con cardio factible y una semana sin el: es una OMISION, no un parcial legitimo")
        void omision() {
            var c = CapabilityCoveragePolicy.evaluar("LOSE_WEIGHT", "INTERMEDIATE", semana(
                    con(Capability.STRENGTH, SENTADILLA, "LOSE_WEIGHT")), TODOS, LIMITES);
            assertEquals(Set.of(Capability.CARDIO), c.omitidas());
            assertEquals(CapabilityCoveragePolicy.Resultado.NO_FACTIBLE, c.resultado(),
                    "solo secundarias: no cumple el objetivo");
        }

        @Test
        @DisplayName("los ejercicios en revision no hacen factible ni cubren una capacidad")
        void enRevisionNoCuenta() {
            var f = CapabilityCoveragePolicy.factibilidad("LOSE_WEIGHT", "INTERMEDIATE",
                    List.of(SENTADILLA, EN_REVISION), LIMITES);
            assertFalse(f.get(Capability.CARDIO).factible(), f.get(Capability.CARDIO).motivo());
        }
    }

    // =====================================================================

    static PlanGenerationContext contexto(String objetivo, List<CandidateExercise> elegibles) {
        var perfil = new PlanningProfile(7L, 35, "FEMALE", new BigDecimal("165"), new BigDecimal("70"), null,
                "INTERMEDIATE", objetivo, "x", "Ninguna", "GYM", 1, List.of((short) 1), 45,
                List.of("body weight"), List.of(), SafetyProfile.Declaradas.sinLimitaciones());
        return new PlanGenerationContext(7L, (short) 1, LUNES, LUNES.plusDays(6), perfil, PlanAdjustment.none(),
                PreviousWeekSummary.empty(), elegibles, SafetyProfile.none(), (PrescriptionParams) null);
    }

    static List<String> problemas(PlanDraft d, PlanGenerationContext ctx, DoseEnforcement modo) {
        var v = new PlanDraftValidator(new SessionDurationEstimator(), SlotCoverageEnforcement.ENFORCE_ALWAYS,
                DoseEnforcement.ENFORCE, modo);
        try {
            v.validate(d, ctx, 30);
            return List.of();
        } catch (InvalidPlanDraftException e) {
            return e.problems();
        }
    }

    @Nested
    @DisplayName("validadores: V23, V24 extendida y V25 dicen lo mismo que el generador")
    class Validadores {

        // Sin prescription params (null): la dosis se compara sin limites de V16.
        static PlanDraft.DraftExercise sinLimites(Capability c, CandidateExercise e, String objetivo) {
            return CapabilityPrescriptionPolicy.dosis(c, e, objetivo, "INTERMEDIATE", null).orElseThrow()
                    .prescribir(e.exerciseId(), null);
        }

        @Test
        @DisplayName("V25: LOSE_WEIGHT sin cardio, con cardio factible, se rechaza; V23 no le exige slots")
        void perderPesoSinCardio() {
            var ctx = contexto("LOSE_WEIGHT", TODOS);
            var p = problemas(semana(sinLimites(Capability.STRENGTH, SENTADILLA, "LOSE_WEIGHT"),
                    sinLimites(Capability.STRENGTH, PRESS, "LOSE_WEIGHT")), ctx, DoseEnforcement.ENFORCE);
            assertTrue(p.stream().anyMatch(x -> x.startsWith("V25") && x.contains("CARDIO")), p.toString());
            assertTrue(p.stream().noneMatch(x -> x.startsWith("V23")), "4A v6: no se exigen slots a LOSE_WEIGHT " + p);
        }

        @Test
        @DisplayName("LOSE_WEIGHT con cardio y fuerza acreditados: sin problemas de capacidad")
        void perderPesoCorrecto() {
            var ctx = contexto("LOSE_WEIGHT", TODOS);
            var p = problemas(semana(sinLimites(Capability.STRENGTH, SENTADILLA, "LOSE_WEIGHT"),
                    sinLimites(Capability.CARDIO, BICI, "LOSE_WEIGHT")), ctx, DoseEnforcement.ENFORCE);
            assertTrue(p.stream().noneMatch(x -> x.startsWith("V24") || x.startsWith("V25") || x.startsWith("V23")),
                    p.toString());
        }

        @Test
        @DisplayName("V24: dosis generica en INCREASE_STRENGTH, o un abdominal en LOSE_WEIGHT, se rechazan")
        void dosisIncorrecta() {
            var generica = new PlanDraft.DraftExercise(1L, PrescriptionType.SETS_REPS, (short) 4, (short) 6, null,
                    null, (short) 120, null);
            var p = problemas(semana(generica, sinLimites(Capability.STRENGTH, PRESS, "INCREASE_STRENGTH")),
                    contexto("INCREASE_STRENGTH", TODOS), DoseEnforcement.ENFORCE);
            assertTrue(p.stream().anyMatch(x -> x.startsWith("V24: el ejercicio 1") && x.contains("STRENGTH")),
                    p.toString());
            var crunch = new PlanDraft.DraftExercise(5L, PrescriptionType.SETS_REPS, (short) 3, (short) 15, null,
                    null, (short) 45, null);
            var q = problemas(semana(crunch, sinLimites(Capability.CARDIO, BICI, "LOSE_WEIGHT")),
                    contexto("LOSE_WEIGHT", TODOS), DoseEnforcement.ENFORCE);
            assertTrue(q.stream().anyMatch(x -> x.contains("no aporta ninguna capacidad de LOSE_WEIGHT")), q.toString());
        }

        @Test
        @DisplayName("inseguro (fuera del conjunto elegible): V1, aunque la dosis sea correcta")
        void inseguro() {
            var sinPress = TODOS.stream().filter(e -> e != PRESS).toList();
            var p = problemas(semana(sinLimites(Capability.STRENGTH, PRESS, "INCREASE_STRENGTH"),
                    sinLimites(Capability.STRENGTH, SENTADILLA, "INCREASE_STRENGTH")),
                    contexto("INCREASE_STRENGTH", sinPress), DoseEnforcement.ENFORCE);
            assertTrue(p.stream().anyMatch(x -> x.startsWith("V1: el ejercicio 3")), p.toString());
        }

        @Test
        @DisplayName("una secundaria nunca se exige: INCREASE_STRENGTH sin hipertrofia es valido")
        void secundariaNoObligatoria() {
            var ctx = contexto("INCREASE_STRENGTH", TODOS);
            var p = problemas(semana(sinLimites(Capability.STRENGTH, SENTADILLA, "INCREASE_STRENGTH"),
                    sinLimites(Capability.STRENGTH, PESO_MUERTO, "INCREASE_STRENGTH"),
                    sinLimites(Capability.STRENGTH, PRESS, "INCREASE_STRENGTH"),
                    sinLimites(Capability.STRENGTH, REMO, "INCREASE_STRENGTH")), ctx, DoseEnforcement.ENFORCE);
            assertTrue(p.stream().noneMatch(x -> x.startsWith("V24") || x.startsWith("V25")), p.toString());
        }

        @Test
        @DisplayName("con las reglas de capacidad en OFF (validador de la adaptacion P2-D) no se mira nada de esto")
        void apagado() {
            var p = problemas(semana(sinLimites(Capability.STRENGTH, SENTADILLA, "LOSE_WEIGHT")),
                    contexto("LOSE_WEIGHT", TODOS), DoseEnforcement.OFF);
            assertTrue(p.stream().noneMatch(x -> x.startsWith("V24") || x.startsWith("V25")), p.toString());
        }
    }

    @Nested
    @DisplayName("4D v3.1 · GAIN_MUSCLE")
    class Hipertrofia31 {

        @Test
        @DisplayName("tope de colocaciones: 6 hasta 59 minutos, 7 desde 60; 4D v3 se conserva como referencia")
        void tope() {
            assertEquals(6, MvpDosePolicy.maxColocaciones(30));
            assertEquals(6, MvpDosePolicy.maxColocaciones(59));
            assertEquals(7, MvpDosePolicy.maxColocaciones(60));
            assertEquals(6, MvpDosePolicy.MAX_COLOC_DIA);
        }

        static final CandidateExercise OTRO = sr(1070, "chest", "OTHER");
        static final CandidateExercise ISOMETRICO = ej(900, "chest", "HORIZONTAL_ADDUCTION", PrescriptionType.DURATION);

        @Test
        @DisplayName("movilidad, OTHER, duracion o en revision no acreditan hipertrofia y se rechazan; con el interruptor en OFF (P2-D), no")
        void soloEvidenciaDeHipertrofia() {
            var elegibles = Stream.concat(TODOS.stream(), Stream.of(OTRO, ISOMETRICO)).toList();
            var ctx = contexto("GAIN_MUSCLE", elegibles);
            var d = MvpDosePolicy.dosisPara(MandatorySlot.of(PRESS), "chest").orElseThrow();
            var press = new PlanDraft.DraftExercise(3L, PrescriptionType.SETS_REPS, d.series(), d.reps(), null, null,
                    d.descansoSegundos(), null);
            var relleno = MvpDosePolicy.dosisPara(Optional.empty(), "chest").orElseThrow();
            var otro = new PlanDraft.DraftExercise(1070L, PrescriptionType.SETS_REPS, relleno.series(), relleno.reps(),
                    null, null, relleno.descansoSegundos(), null);
            var movilidad = new PlanDraft.DraftExercise(9L, PrescriptionType.DURATION, (short) 2, null, 30, null,
                    (short) 15, null);
            var iso = new PlanDraft.DraftExercise(900L, PrescriptionType.DURATION, (short) 3, null, 40, null,
                    (short) 45, null);
            var p = problemas(semana(press, otro, movilidad, iso), ctx, DoseEnforcement.ENFORCE);
            for (long id : List.of(1070L, 9L, 900L))
                assertTrue(p.stream().anyMatch(x -> x.startsWith("V24: el ejercicio " + id) && x.contains("no acredita hipertrofia")),
                        id + " " + p);
            assertTrue(p.stream().noneMatch(x -> x.startsWith("V24: el ejercicio 3 ")), p.toString());
            var apagado = problemas(semana(press, otro, movilidad, iso), ctx, DoseEnforcement.OFF);
            assertTrue(apagado.stream().noneMatch(x -> x.contains("no acredita hipertrofia")), apagado.toString());
        }
    }
}
