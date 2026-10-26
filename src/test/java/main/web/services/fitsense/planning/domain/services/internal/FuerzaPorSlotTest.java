package main.web.services.fitsense.planning.domain.services.internal;

import main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.model.valueobjects.GoalCapabilityPolicy.Capability;
import main.web.services.fitsense.planning.domain.services.CapabilityPrescriptionPolicy;
import main.web.services.fitsense.planning.domain.services.DoseEnforcement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static main.web.services.fitsense.planning.domain.services.internal.CapacidadesPorObjetivoTest.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * V25 · INCREASE_STRENGTH: LOWER_KNEE, LOWER_HIP, UPPER_PUSH y UPPER_PULL con
 * dosis de STRENGTH en la semana, cuando sea factible (decision del 26/10/2026,
 * tras el plan 58). Sin cantidades por sesion ni proporciones.
 */
class FuerzaPorSlotTest {

    static final String F = "INCREASE_STRENGTH";

    static PlanDraft.DraftExercise d(Capability c, CandidateExercise e, String objetivo) {
        return CapabilityPrescriptionPolicy.dosis(c, e, objetivo, "INTERMEDIATE", null).orElseThrow()
                .prescribir(e.exerciseId(), null);
    }

    static PlanDraft.DraftWorkout dia(LocalDate fecha, PlanDraft.DraftExercise... ejercicios) {
        return new PlanDraft.DraftWorkout(fecha, WorkoutFocus.FULL_BODY, "s", 45, List.of(ejercicios));
    }

    static PlanGenerationContext contexto(String objetivo, List<CandidateExercise> elegibles, int minutos) {
        var perfil = new PlanningProfile(7L, 35, "MALE", new BigDecimal("177"), new BigDecimal("76"), null,
                "INTERMEDIATE", objetivo, "x", "Ninguna", "GYM", 3, List.of((short) 1, (short) 3, (short) 5), minutos,
                List.of("body weight"), List.of(), SafetyProfile.Declaradas.sinLimitaciones());
        return new PlanGenerationContext(7L, (short) 1, LUNES, LUNES.plusDays(6), perfil, PlanAdjustment.none(),
                PreviousWeekSummary.empty(), elegibles, SafetyProfile.none(), (PrescriptionParams) null);
    }

    static List<String> v25(PlanGenerationContext ctx, PlanDraft.DraftWorkout... dias) {
        var draft = new PlanDraft(GenerationSource.AI, "m", "p", null, "r", List.of(dias));
        return problemas(draft, ctx, DoseEnforcement.ENFORCE).stream().filter(p -> p.startsWith("V25")).toList();
    }

    @Test
    @DisplayName("como el plan 58: la bisagra solo con dosis de hipertrofia -> se rechaza por LOWER_HIP, y solo por el")
    void bisagraConHipertrofia() {
        var p = v25(contexto(F, TODOS, 60),
                dia(LUNES, d(Capability.STRENGTH, SENTADILLA, F), d(Capability.HYPERTROPHY, PRESS, F)),
                dia(LUNES.plusDays(2), d(Capability.STRENGTH, PRESS, F), d(Capability.HYPERTROPHY, PESO_MUERTO, F)),
                dia(LUNES.plusDays(4), d(Capability.STRENGTH, REMO, F), d(Capability.HYPERTROPHY, SENTADILLA, F)));
        assertEquals(1, p.size(), p.toString());
        assertTrue(p.get(0).contains("LOWER_HIP") && p.get(0).contains("no HYPERTROPHY"), p.toString());
    }

    @Test
    @DisplayName("los cuatro slots con dosis de fuerza en la semana, mas hipertrofia: pasa")
    void cuatroSlots() {
        var p = v25(contexto(F, TODOS, 60),
                dia(LUNES, d(Capability.STRENGTH, SENTADILLA, F), d(Capability.STRENGTH, PRESS, F),
                        d(Capability.HYPERTROPHY, REMO, F)),
                dia(LUNES.plusDays(2), d(Capability.STRENGTH, PESO_MUERTO, F), d(Capability.STRENGTH, REMO, F)));
        assertTrue(p.isEmpty(), p.toString());
    }

    @Test
    @DisplayName("sin ejercicio de bisagra con dosis de fuerza en el conjunto elegible: LOWER_HIP no se exige")
    void slotNoFactible() {
        var sinBisagra = TODOS.stream().filter(e -> e != PESO_MUERTO).toList();
        var p = v25(contexto(F, sinBisagra, 60),
                dia(LUNES, d(Capability.STRENGTH, SENTADILLA, F), d(Capability.STRENGTH, PRESS, F),
                        d(Capability.STRENGTH, REMO, F)));
        assertTrue(p.isEmpty(), p.toString());
    }

    @Test
    @DisplayName("factibilidad conjunta: una sesion corta (menos de 30 min) aloja 3 de fuerza; cubrir 3 de 4 basta")
    void capacidadDeLaSemana() {
        var ctx = contexto(F, TODOS, 25);
        var tres = v25(ctx, dia(LUNES, d(Capability.STRENGTH, SENTADILLA, F), d(Capability.STRENGTH, PRESS, F),
                d(Capability.STRENGTH, REMO, F)));
        assertTrue(tres.isEmpty(), "no caben los cuatro: no se rechaza " + tres);
        var dos = v25(ctx, dia(LUNES, d(Capability.STRENGTH, SENTADILLA, F), d(Capability.STRENGTH, PRESS, F),
                d(Capability.HYPERTROPHY, REMO, F)));
        assertFalse(dos.isEmpty(), "cabian tres y hay solo dos: se rechaza " + dos);
    }

    @Test
    @DisplayName("los demas objetivos no cambian: GENERAL_FITNESS con la bisagra en hipertrofia no se rechaza por slot")
    void otrosObjetivos() {
        var g = "GENERAL_FITNESS";
        var p = v25(contexto(g, TODOS, 60),
                dia(LUNES, d(Capability.STRENGTH, SENTADILLA, g), d(Capability.STRENGTH, PRESS, g),
                        d(Capability.STRENGTH, REMO, g), d(Capability.HYPERTROPHY, PESO_MUERTO, g),
                        d(Capability.MUSCULAR_ENDURANCE, REMO, g), d(Capability.CARDIO, BICI, g),
                        d(Capability.MOBILITY, ESTIRAMIENTO, g)));
        assertTrue(p.stream().noneMatch(x -> x.contains("dosis de STRENGTH en la semana")), p.toString());
    }

    @Test
    @DisplayName("con las reglas de capacidad en OFF (validador de P2-D) no se mira")
    void apagado() {
        var draft = new PlanDraft(GenerationSource.AI, "m", "p", null, "r", List.of(
                dia(LUNES, d(Capability.STRENGTH, SENTADILLA, F), d(Capability.HYPERTROPHY, PESO_MUERTO, F))));
        var p = problemas(draft, contexto(F, TODOS, 60), DoseEnforcement.OFF);
        assertTrue(p.stream().noneMatch(x -> x.startsWith("V25")), p.toString());
    }
}
