package main.web.services.fitsense.planning.domain.services.internal;

import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.model.valueobjects.GoalCapabilityPolicy.Capability;
import main.web.services.fitsense.planning.domain.services.GoalFeasibilityPolicy;
import main.web.services.fitsense.planning.domain.services.GoalFeasibilityPolicy.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class GoalFeasibilityPolicyTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 10, 26);

    private static CandidateExercise ej(long id, String zona, String patron) {
        return new CandidateExercise(id, "ejercicio " + id, zona, "body weight", 1,
                PrescriptionType.SETS_REPS, null, patron);
    }

    /** n ids distintos por slot resistido, a partir de base. */
    private static List<CandidateExercise> slots(int n, long base) {
        var l = new ArrayList<CandidateExercise>();
        for (int i = 0; i < n; i++) {
            l.add(ej(base + i, "upper legs", "KNEE_DOMINANT"));
            l.add(ej(base + 100 + i, "upper legs", "HIP_HINGE"));
            l.add(ej(base + 200 + i, "chest", "HORIZONTAL_PUSH"));
            l.add(ej(base + 300 + i, "back", "HORIZONTAL_PULL"));
        }
        return l;
    }

    private static List<CandidateExercise> patron(String patron, int n, long base) {
        return LongStream.range(0, n).mapToObj(i -> ej(base + i, "cardio", patron)).toList();
    }

    private static List<CandidateExercise> juntar(List<CandidateExercise>... listas) {
        return Stream.of(listas).flatMap(List::stream).toList();
    }

    private static EvaluacionPool pool(String objetivo, String nivel, List<CandidateExercise> elegibles) {
        return GoalFeasibilityPolicy.evaluarPool(objetivo, nivel, elegibles).orElseThrow();
    }

    private static CapacidadEvaluada principal(EvaluacionPool p, Capability c) {
        return p.principales().stream().filter(x -> x.capacidad() == c).findFirst().orElseThrow();
    }

    private static CapacidadEvaluada secundaria(EvaluacionPool p, Capability c) {
        return p.secundarias().stream().filter(x -> x.capacidad() == c).findFirst().orElseThrow();
    }

    // ------------------------------------------------------------ nivel A

    /** Umbral 0 / 1-2 / 3+ sobre la ruta directa de cardio. Con 0, el circuito no la salva ni la condena. */
    @ParameterizedTest(name = "{0} cardio -> {1}")
    @CsvSource({"0,NO_VERIFICABLE", "1,FRAGIL", "2,FRAGIL", "3,ROBUSTO", "7,ROBUSTO"})
    void umbralDeCardioParaLoseWeight(int n, Veredicto esperado) {
        var p = pool("LOSE_WEIGHT", "INTERMEDIATE", patron("LOCOMOTION_CARDIO", n, 5000));
        assertEquals(esperado, p.veredicto());
        var cardio = principal(p, Capability.CARDIO);
        var directa = cardio.rutas().get(0);
        assertEquals(Ruta.LOCOMOCION_DIRECTA, directa.ruta());
        assertEquals(n, directa.verificados());
        assertEquals(n == 0 ? Veredicto.NO_FACTIBLE : esperado, directa.veredicto());
        assertEquals(Veredicto.NO_VERIFICABLE, cardio.rutas().get(1).veredicto(), "el circuito nunca acredita");
    }

    @Test
    void filasRepetidasNoInflanElPool() {
        var mismo = ej(5000, "cardio", "LOCOMOTION_CARDIO");
        var p = pool("LOSE_WEIGHT", "INTERMEDIATE", List.of(mismo, mismo, mismo));
        assertEquals(Veredicto.FRAGIL, p.veredicto());
        assertEquals(1, principal(p, Capability.CARDIO).rutas().get(0).verificados());
    }

    @Test
    void lasClasificacionesEnRevisionNoSonEvidencia() {
        var enRevision = List.of(ej(59, "waist", "LOCOMOTION_CARDIO"), ej(93, "upper legs", "LOCOMOTION_CARDIO"),
                ej(298, "waist", "LOCOMOTION_CARDIO"));
        var p = pool("LOSE_WEIGHT", "INTERMEDIATE", enRevision);
        assertEquals(Veredicto.NO_VERIFICABLE, p.veredicto());
        var directa = principal(p, Capability.CARDIO).rutas().get(0);
        assertEquals(0, directa.verificados());
        assertEquals(3, directa.enRevision());
        assertTrue(directa.motivo().contains("3 en revision"), directa.motivo());
    }

    @Test
    void loseWeightNoExigeLosCuatroSlots() {
        var soloCardio = patron("LOCOMOTION_CARDIO", 4, 5000);
        assertEquals(Veredicto.ROBUSTO, pool("LOSE_WEIGHT", "BEGINNER", soloCardio).veredicto());
        assertEquals(Veredicto.NO_FACTIBLE, pool("GAIN_MUSCLE", "BEGINNER", soloCardio).veredicto());
        assertFalse(GoalFeasibilityPolicy.exigeSlotsResistidos("LOSE_WEIGHT"));
        assertTrue(GoalFeasibilityPolicy.exigeSlotsResistidos("GAIN_MUSCLE"));
        assertTrue(GoalFeasibilityPolicy.exigeSlotsResistidos("INCREASE_STRENGTH"));
        assertTrue(GoalFeasibilityPolicy.exigeSlotsResistidos("GENERAL_FITNESS"));
    }

    @Test
    void unSlotVacioHaceNoFactibleAlObjetivoQueLoExigeYDiceCual() {
        var sinCadera = slots(3, 1000).stream().filter(c -> !"HIP_HINGE".equals(c.movementPattern())).toList();
        var p = pool("GAIN_MUSCLE", "BEGINNER", sinCadera);
        assertEquals(Veredicto.NO_FACTIBLE, p.veredicto());
        assertEquals(List.of("HYPERTROPHY/SLOTS_RESISTIDOS: slots sin candidatos: [LOWER_HIP]"), p.motivos());
    }

    @Test
    void unaSecundariaAusenteNoCambiaElVeredicto() {
        // LOSE_WEIGHT: cardio robusto, ninguna resistencia: STRENGTH (secundaria) no factible.
        var p = pool("LOSE_WEIGHT", "INTERMEDIATE", patron("LOCOMOTION_CARDIO", 3, 5000));
        assertEquals(Veredicto.NO_FACTIBLE, secundaria(p, Capability.STRENGTH).veredicto());
        assertEquals(Veredicto.ROBUSTO, p.veredicto());
        assertTrue(p.motivos().isEmpty());
    }

    @Test
    void powerNoAplicaAPrincipiantesYNuncaSeVerificaPorSuposicion() {
        var elegibles = slots(3, 1000);
        var principiante = pool("INCREASE_STRENGTH", "BEGINNER", elegibles);
        assertFalse(secundaria(principiante, Capability.POWER).aplica());
        var intermedio = pool("INCREASE_STRENGTH", "INTERMEDIATE", elegibles);
        assertEquals(Veredicto.NO_VERIFICABLE, secundaria(intermedio, Capability.POWER).veredicto());
        assertEquals(Veredicto.ROBUSTO, intermedio.veredicto());
    }

    @Test
    void balanceOportunistaAusenteNoInvalidaGeneralFitness() {
        var p = pool("GENERAL_FITNESS", "INTERMEDIATE", juntar(slots(3, 1000),
                patron("LOCOMOTION_CARDIO", 3, 5000), patron("MOBILITY", 3, 6000)));
        assertEquals(Veredicto.NO_FACTIBLE, secundaria(p, Capability.BALANCE).veredicto());
        assertEquals(Veredicto.ROBUSTO, p.veredicto());
    }

    /** La misma combinacion de ejercicios: robusta, fragil y no factible segun el objetivo. */
    @Test
    void mismoPoolTresVeredictosSegunElObjetivo() {
        var elegibles = juntar(slots(3, 1000), patron("LOCOMOTION_CARDIO", 1, 5000));
        assertEquals(Veredicto.ROBUSTO, pool("GAIN_MUSCLE", "INTERMEDIATE", elegibles).veredicto());
        assertEquals(Veredicto.FRAGIL, pool("LOSE_WEIGHT", "INTERMEDIATE", elegibles).veredicto());
        var general = pool("GENERAL_FITNESS", "INTERMEDIATE", elegibles);
        assertEquals(Veredicto.NO_FACTIBLE, general.veredicto());
        assertEquals(Veredicto.NO_FACTIBLE, principal(general, Capability.MOBILITY).veredicto());
    }

    @Test
    void elVeredictoEsElPeorDeLasPrincipales() {
        var p = pool("GENERAL_FITNESS", "INTERMEDIATE", juntar(slots(3, 1000),
                patron("LOCOMOTION_CARDIO", 2, 5000), patron("MOBILITY", 5, 6000)));
        assertEquals(Veredicto.FRAGIL, p.veredicto());
        assertEquals(Veredicto.ROBUSTO, principal(p, Capability.STRENGTH).veredicto());
        assertEquals(Veredicto.ROBUSTO, principal(p, Capability.MUSCULAR_ENDURANCE).veredicto());
        assertEquals(Veredicto.FRAGIL, principal(p, Capability.CARDIO).veredicto());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "ENDURANCE")
    void objetivoSinMatrizNoSeEvalua(String objetivo) {
        assertTrue(GoalFeasibilityPolicy.evaluarPool(objetivo, "BEGINNER", slots(3, 1000)).isEmpty());
    }

    // ------------------------------------------------------------ nivel B

    private static PlanDraft semana(List<List<CandidateExercise>> porDia) {
        var w = new ArrayList<PlanDraft.DraftWorkout>();
        for (int i = 0; i < porDia.size(); i++)
            w.add(new PlanDraft.DraftWorkout(LUNES.plusDays(2L * i), WorkoutFocus.FULL_BODY, "s", 45,
                    porDia.get(i).stream().map(c -> new PlanDraft.DraftExercise(c.exerciseId(),
                            PrescriptionType.SETS_REPS, (short) 3, (short) 10, null, null, (short) 60, null)).toList()));
        return new PlanDraft(GenerationSource.AI, "m", "p", null, "r", w);
    }

    /** El ejemplo de la revision: pool de cardio robusto y semana sin cardio. */
    @Test
    void poolRobustoNoImplicaSemanaConCardio() {
        var cardio = patron("LOCOMOTION_CARDIO", 5, 5000);
        var fuerza = slots(3, 1000);
        var elegibles = juntar(cardio, fuerza);
        assertEquals(Veredicto.ROBUSTO, pool("LOSE_WEIGHT", "INTERMEDIATE", elegibles).veredicto());

        var sinCardio = GoalFeasibilityPolicy.evaluarSemana("LOSE_WEIGHT", "INTERMEDIATE",
                semana(List.of(fuerza.subList(0, 4), fuerza.subList(4, 8))), elegibles).orElseThrow();
        assertEquals(VeredictoSemana.PRINCIPAL_AUSENTE, sinCardio.veredicto());

        var conCardio = GoalFeasibilityPolicy.evaluarSemana("LOSE_WEIGHT", "INTERMEDIATE",
                semana(List.of(List.of(fuerza.get(0), cardio.get(0)), List.of(fuerza.get(1), cardio.get(1)))),
                elegibles).orElseThrow();
        assertEquals(VeredictoSemana.PRINCIPALES_PRESENTES_PENDIENTE_4D, conCardio.veredicto());
        var c = conCardio.principales().get(0);
        assertEquals(2, c.colocaciones());
        assertEquals(2, c.dias());
    }

    @Test
    void cardioSoloEnRevisionNoSeAcreditaEnLaSemana() {
        var dudoso = ej(59, "waist", "LOCOMOTION_CARDIO");
        var fuerza = slots(1, 1000);
        var elegibles = juntar(List.of(dudoso), fuerza);
        var s = GoalFeasibilityPolicy.evaluarSemana("LOSE_WEIGHT", "INTERMEDIATE",
                semana(List.of(List.of(dudoso, fuerza.get(0)))), elegibles).orElseThrow();
        assertEquals(VeredictoSemana.PRINCIPAL_NO_VERIFICABLE, s.veredicto());
    }

    @Test
    void semanaSinUnSlotNoCumpleHipertrofia() {
        var fuerza = slots(1, 1000);
        var sinTraccion = fuerza.subList(0, 3);
        var s = GoalFeasibilityPolicy.evaluarSemana("GAIN_MUSCLE", "BEGINNER",
                semana(List.of(sinTraccion)), fuerza).orElseThrow();
        assertEquals(VeredictoSemana.PRINCIPAL_AUSENTE, s.veredicto());
        assertTrue(s.principales().get(0).detalle().contains("UPPER_PULL"));
    }

    @Test
    void enLaSemanaUnaSecundariaAusenteTampocoCambiaElVeredicto() {
        var cardio = patron("LOCOMOTION_CARDIO", 1, 5000);
        var s = GoalFeasibilityPolicy.evaluarSemana("LOSE_WEIGHT", "INTERMEDIATE",
                semana(List.of(List.of(cardio.get(0)))), cardio).orElseThrow();
        assertEquals(VeredictoSemana.PRINCIPALES_PRESENTES_PENDIENTE_4D, s.veredicto());
        assertEquals(Presencia.AUSENTE, s.secundarias().get(0).presencia());
    }

    @Test
    void unaSesionSoloDeComplementosNoAcreditaElObjetivoCompleto() {
        // GENERAL_FITNESS con una sesion de cardio y movilidad: dos principales
        // presentes, las de fuerza ausentes. No se rechaza la sesion; la semana
        // no se declara completa.
        var cardio = patron("LOCOMOTION_CARDIO", 1, 5000);
        var movilidad = patron("MOBILITY", 1, 6000);
        var elegibles = juntar(cardio, movilidad, slots(3, 1000));
        var s = GoalFeasibilityPolicy.evaluarSemana("GENERAL_FITNESS", "INTERMEDIATE",
                semana(List.of(List.of(cardio.get(0), movilidad.get(0)))), elegibles).orElseThrow();
        assertEquals(VeredictoSemana.PRINCIPAL_AUSENTE, s.veredicto());
    }

    @Test
    void unIdFueraDelConjuntoElegibleNoAcreditaNada() {
        var cardio = patron("LOCOMOTION_CARDIO", 1, 5000);
        var s = GoalFeasibilityPolicy.evaluarSemana("LOSE_WEIGHT", "INTERMEDIATE",
                semana(List.of(cardio)), List.of()).orElseThrow();
        assertEquals(VeredictoSemana.PRINCIPAL_AUSENTE, s.veredicto());
    }
}
