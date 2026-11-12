package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.MandatorySlot;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;
import main.web.services.fitsense.planning.domain.model.valueobjects.GoalCapabilityPolicy;

import java.util.*;

/**
 * CIERRE PLAN-REV-2.0 · FASE 2 (alternativa C): INCREASE_STRENGTH con el tiempo disponible demasiado corto para los
 * slots de fuerza que V25 exige. No cambia V25, la dosis de fuerza ni V4: solo detecta ANTES de generar que la
 * combinacion pedida no permite la prescripcion completa, para decirlo con claridad y ofrecer alternativas de
 * disponibilidad, en lugar de un fallo tecnico de generacion.
 * <p>
 * Calculo (cota inferior, nunca marca un caso que si cabe): para cada slot 4C con un elegible que admite dosis de
 * STRENGTH, el ejercicio mas corto con esa dosis (SessionDurationEstimator, los mismos parametros que V4); V25 exige
 * {@code min(slots factibles, sesiones x por_sesion)} slots (su propia regla). Repartidos en las sesiones de la
 * semana, la sesion mas cargada lleva {@code ceil(exigidos / sesiones)} colocaciones; si incluso con los ejercicios mas
 * cortos supera el limite de V4 de la politica activa, no hay plan posible.
 */
public final class CoberturaFuerzaTiempo {

    private CoberturaFuerzaTiempo() {
    }

    public record Deficit(int slotsExigidos, int sesiones, long segundosMinimos, long limiteSegundos,
                          int minutosNecesarios) {
    }

    public static Optional<Deficit> evaluar(PlanGenerationContext ctx, SessionDurationEstimator estimador) {
        if (ctx == null || ctx.profile() == null || !"INCREASE_STRENGTH".equals(ctx.profile().goalType())
                || ctx.availableExercises() == null || ctx.prescription() == null) return Optional.empty();
        String objetivo = ctx.profile().goalType();
        String nivel = ctx.profile().fitnessLevel();
        var limites = ctx.prescription().repLimits();
        var fuerza = GoalCapabilityPolicy.Capability.STRENGTH;
        var masCorto = new EnumMap<MandatorySlot, Long>(MandatorySlot.class);
        for (var c : ctx.availableExercises()) {
            if (!CapabilityPrescriptionPolicy.admisibles(c, objetivo, nivel).contains(fuerza)) continue;
            var slot = CoberturaDePatron.slot(c);
            var d = CapabilityPrescriptionPolicy.dosis(fuerza, c, objetivo, nivel, limites);
            if (slot.isEmpty() || d.isEmpty()) continue;
            long s = segundosDe(c, d.get(), ctx, estimador);
            masCorto.merge(slot.get(), s, Math::min);
        }
        if (masCorto.isEmpty()) return Optional.empty();
        int sesiones = ctx.suggestedSplit().size();
        if (sesiones == 0) return Optional.empty();
        int porSesion = ctx.effectiveSessionMinutes() < 30 ? 3 : 4;   // la misma regla que V25
        int exigidos = Math.min(masCorto.size(), sesiones * porSesion);
        int enLaMasCargada = (exigidos + sesiones - 1) / sesiones;
        var cortos = masCorto.values().stream().sorted().limit(enLaMasCargada).toList();
        long warmup = (long) ctx.prescription().warmupMinutesOrDefault() * 60;
        long transicion = ctx.prescription().transitionSecondsOrDefault();
        long minimo = warmup + cortos.stream().mapToLong(Long::longValue).sum()
                + (long) Math.max(0, cortos.size() - 1) * transicion;
        long limite = ctx.policy().limiteV4SegundosDe(ctx.effectiveSessionMinutes());
        if (minimo <= limite) return Optional.empty();
        int minutos = (int) Math.ceil(minimo * 100.0 / (60.0 * (100 + ctx.policy().toleranciaV4Pct())));
        return Optional.of(new Deficit(exigidos, sesiones, minimo, limite, minutos));
    }

    /** Segundos de trabajo y descanso de un ejercicio con esa dosis (sin calentamiento ni transicion). */
    private static long segundosDe(CandidateExercise c, CapabilityPrescriptionPolicy.Dosis d, PlanGenerationContext ctx,
                                   SessionDurationEstimator estimador) {
        var e = new PlanDraft.DraftExercise(c.exerciseId(), d.tipo(), d.series(), d.reps(), d.segundos(), null,
                d.descanso(), null);
        var w = new PlanDraft.DraftWorkout(ctx.weekStartDate(), WorkoutFocus.FULL_BODY, "x", 0, List.of(e));
        return estimador.estimateSeconds(w, ctx.prescription())
                - (long) ctx.prescription().warmupMinutesOrDefault() * 60;
    }
}
