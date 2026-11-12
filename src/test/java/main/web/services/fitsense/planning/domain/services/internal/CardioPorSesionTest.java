package main.web.services.fitsense.planning.domain.services.internal;

import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.model.valueobjects.GoalCapabilityPolicy.Capability;
import main.web.services.fitsense.planning.domain.services.CapabilityPrescriptionPolicy;
import main.web.services.fitsense.planning.domain.services.DoseEnforcement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static main.web.services.fitsense.planning.domain.services.internal.CapacidadesPorObjetivoTest.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * V25 · LOSE_WEIGHT: CARDIO en cada sesion en la que sea factible (decision del
 * 26/10/2026, tras el plan 55). Los demas objetivos siguen con la regla semanal.
 */
class CardioPorSesionTest {

    static final LocalDate MIERCOLES = LUNES.plusDays(2);
    static final LocalDate VIERNES = LUNES.plusDays(4);

    /** Cardio cuya zona tiene recuperacion entre dias seguidos (escalera). */
    static final CandidateExercise ESCALERA = new CandidateExercise(30L, "Escalera", "upper legs", "stepmill machine",
            1, PrescriptionType.DURATION, null, "LOCOMOTION_CARDIO");

    static PlanDraft.DraftExercise d(Capability c, CandidateExercise e, String objetivo) {
        return CapabilityPrescriptionPolicy.dosis(c, e, objetivo, "INTERMEDIATE", null).orElseThrow()
                .prescribir(e.exerciseId(), null);
    }

    static PlanDraft.DraftWorkout dia(LocalDate fecha, WorkoutFocus enfoque, PlanDraft.DraftExercise... ejercicios) {
        return new PlanDraft.DraftWorkout(fecha, enfoque, "s", 45, List.of(ejercicios));
    }

    static List<String> v25(String objetivo, List<CandidateExercise> elegibles, PlanDraft.DraftWorkout... dias) {
        var draft = new PlanDraft(GenerationSource.AI, "m", "p", null, "r", List.of(dias));
        return problemas(draft, contexto(objetivo, elegibles), DoseEnforcement.ENFORCE).stream()
                .filter(p -> p.startsWith("V25")).toList();
    }

    @Test
    @DisplayName("el plan 55: cardio solo el lunes -> se rechaza el miercoles y el viernes, no el lunes")
    void plan55() {
        var p = v25("LOSE_WEIGHT", TODOS,
                dia(LUNES, WorkoutFocus.FULL_BODY, d(Capability.CARDIO, BICI, "LOSE_WEIGHT"),
                        d(Capability.STRENGTH, SENTADILLA, "LOSE_WEIGHT")),
                dia(MIERCOLES, WorkoutFocus.FULL_BODY, d(Capability.STRENGTH, PRESS, "LOSE_WEIGHT"),
                        d(Capability.STRENGTH, REMO, "LOSE_WEIGHT")),
                dia(VIERNES, WorkoutFocus.FULL_BODY, d(Capability.STRENGTH, PESO_MUERTO, "LOSE_WEIGHT"),
                        d(Capability.STRENGTH, PRESS, "LOSE_WEIGHT")));
        assertEquals(2, p.size(), p.toString());
        assertTrue(p.get(0).contains(MIERCOLES.toString()) && p.get(1).contains(VIERNES.toString()), p.toString());
        assertTrue(p.stream().noneMatch(x -> x.contains(LUNES.toString())));
        assertTrue(p.get(0).contains("puede repetirse"), "le dice a la IA como resolverlo");
    }

    @Test
    @DisplayName("tres sesiones con cardio, reutilizando el unico ejercicio de cardio en dias no consecutivos: valido")
    void reutilizacion() {
        var p = v25("LOSE_WEIGHT", TODOS,
                dia(LUNES, WorkoutFocus.FULL_BODY, d(Capability.CARDIO, BICI, "LOSE_WEIGHT"),
                        d(Capability.STRENGTH, SENTADILLA, "LOSE_WEIGHT")),
                dia(MIERCOLES, WorkoutFocus.FULL_BODY, d(Capability.CARDIO, BICI, "LOSE_WEIGHT"),
                        d(Capability.STRENGTH, PRESS, "LOSE_WEIGHT")),
                dia(VIERNES, WorkoutFocus.FULL_BODY, d(Capability.CARDIO, BICI, "LOSE_WEIGHT"),
                        d(Capability.STRENGTH, REMO, "LOSE_WEIGHT")));
        assertTrue(p.isEmpty(), p.toString());
    }

    @Test
    @DisplayName("una colocacion de fuerza no cuenta como cardio aunque el ejercicio admita las dos")
    void laDosisDecide() {
        // La bici con una dosis que no es la de CARDIO no acredita cardio (V24 la rechaza aparte).
        var otraDosis = new PlanDraft.DraftExercise(8L, PrescriptionType.DURATION, (short) 2, null, 30, null,
                (short) 15, null);
        var p = v25("LOSE_WEIGHT", TODOS,
                dia(LUNES, WorkoutFocus.FULL_BODY, otraDosis, d(Capability.STRENGTH, SENTADILLA, "LOSE_WEIGHT")));
        assertTrue(p.stream().anyMatch(x -> x.contains(LUNES.toString())), p.toString());
    }

    @Test
    @DisplayName("sin cardio en el conjunto elegible (equipamiento o seguridad): ninguna sesion se exige")
    void noFactible() {
        var sinCardio = TODOS.stream().filter(e -> e != BICI).toList();
        var p = v25("LOSE_WEIGHT", sinCardio,
                dia(LUNES, WorkoutFocus.FULL_BODY, d(Capability.STRENGTH, SENTADILLA, "LOSE_WEIGHT")),
                dia(MIERCOLES, WorkoutFocus.FULL_BODY, d(Capability.STRENGTH, PRESS, "LOSE_WEIGHT")));
        assertTrue(p.isEmpty(), p.toString());
    }

    @Test
    @DisplayName("escasez: el unico cardio es de piernas; un dia de tren superior no lo admite y no se exige")
    void enfoqueQueNoAdmite() {
        var soloEscalera = List.of(SENTADILLA, PRESS, REMO, ESCALERA);
        var p = v25("LOSE_WEIGHT", soloEscalera,
                dia(LUNES, WorkoutFocus.LOWER_BODY, d(Capability.CARDIO, ESCALERA, "LOSE_WEIGHT"),
                        d(Capability.STRENGTH, SENTADILLA, "LOSE_WEIGHT")),
                dia(MIERCOLES, WorkoutFocus.UPPER_BODY, d(Capability.STRENGTH, PRESS, "LOSE_WEIGHT"),
                        d(Capability.STRENGTH, REMO, "LOSE_WEIGHT")));
        assertTrue(p.isEmpty(), "UPPER_BODY no admite upper legs: " + p);
    }

    @Test
    @DisplayName("escasez: el unico cardio es de piernas y el dia anterior fue de piernas: recuperacion, no se exige")
    void recuperacion() {
        var soloEscalera = List.of(SENTADILLA, PRESS, REMO, ESCALERA);
        var martes = LUNES.plusDays(1);
        var p = v25("LOSE_WEIGHT", soloEscalera,
                dia(LUNES, WorkoutFocus.FULL_BODY, d(Capability.CARDIO, ESCALERA, "LOSE_WEIGHT"),
                        d(Capability.STRENGTH, SENTADILLA, "LOSE_WEIGHT")),
                dia(martes, WorkoutFocus.FULL_BODY, d(Capability.STRENGTH, PRESS, "LOSE_WEIGHT"),
                        d(Capability.STRENGTH, REMO, "LOSE_WEIGHT")));
        assertTrue(p.isEmpty(), "el lunes trabajo upper legs: " + p);
        // Sin piernas en un dia vecino, el lunes sin cardio si se exige.
        var q = v25("LOSE_WEIGHT", soloEscalera,
                dia(LUNES, WorkoutFocus.FULL_BODY, d(Capability.STRENGTH, PRESS, "LOSE_WEIGHT")),
                dia(MIERCOLES, WorkoutFocus.FULL_BODY, d(Capability.CARDIO, ESCALERA, "LOSE_WEIGHT"),
                        d(Capability.STRENGTH, REMO, "LOSE_WEIGHT")));
        assertTrue(q.stream().anyMatch(x -> x.contains(LUNES.toString())), q.toString());
    }

    @Test
    @DisplayName("los demas objetivos no cambian: GENERAL_FITNESS con cardio solo el lunes no se rechaza por sesion")
    void otrosObjetivos() {
        var p = v25("GENERAL_FITNESS", TODOS,
                dia(LUNES, WorkoutFocus.FULL_BODY, d(Capability.CARDIO, BICI, "GENERAL_FITNESS"),
                        d(Capability.STRENGTH, SENTADILLA, "GENERAL_FITNESS"),
                        d(Capability.MUSCULAR_ENDURANCE, REMO, "GENERAL_FITNESS"),
                        d(Capability.MOBILITY, ESTIRAMIENTO, "GENERAL_FITNESS")),
                dia(MIERCOLES, WorkoutFocus.FULL_BODY, d(Capability.STRENGTH, PRESS, "GENERAL_FITNESS")));
        assertTrue(p.stream().noneMatch(x -> x.contains("no incluye CARDIO")), p.toString());
    }

    @Test
    @DisplayName("con las reglas de capacidad en OFF (validador de P2-D) no se mira")
    void apagado() {
        var draft = new PlanDraft(GenerationSource.AI, "m", "p", null, "r", List.of(
                dia(LUNES, WorkoutFocus.FULL_BODY, d(Capability.CARDIO, BICI, "LOSE_WEIGHT")),
                dia(MIERCOLES, WorkoutFocus.FULL_BODY, d(Capability.STRENGTH, PRESS, "LOSE_WEIGHT"))));
        var p = problemas(draft, contexto("LOSE_WEIGHT", TODOS), DoseEnforcement.OFF);
        assertTrue(p.stream().noneMatch(x -> x.startsWith("V25")), p.toString());
    }
}
