package main.web.services.fitsense.planning.interfaces.rest.transform;

import main.web.services.fitsense.catalog.interfaces.acl.EligibleExerciseView;
import main.web.services.fitsense.planning.domain.model.aggregates.WeeklyTrainingPlan;
import main.web.services.fitsense.planning.domain.services.MetricaAerobica;
import main.web.services.fitsense.planning.interfaces.rest.resources.AerobicActivityResource;
import main.web.services.fitsense.planning.interfaces.rest.resources.DurationInfoResource;
import main.web.services.fitsense.planning.interfaces.rest.resources.PlannedWorkoutResource;
import main.web.services.fitsense.planning.interfaces.rest.resources.WeeklyTrainingPlanResource;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;

import java.util.*;

/**
 * V44 · Campos OPCIONALES y retrocompatibles del plan: disponibilidad frente a duracion estimada por sesion y
 * metrica aerobica de la semana. Cada uno con su indicador; desactivados, el recurso sale identico al de siempre.
 * <p>
 * La duracion sale de la traza de la politica ({@code planning_policy_trace}), que guarda los segundos estimados con
 * la misma formula que V17/V20. Un plan anterior a V44 no tiene traza: {@code reason = SIN_DATOS} y no se inventa
 * la disponibilidad.
 */
public final class DuracionYAerobicaAssembler {

    private DuracionYAerobicaAssembler() {}

    public static WeeklyTrainingPlanResource enriquecer(WeeklyTrainingPlanResource r, WeeklyTrainingPlan plan,
                                                        Map<Long, EligibleExerciseView> catalogo, JsonSupport json,
                                                        boolean duracion, boolean aerobica) {
        if (!duracion && !aerobica) return r;
        var workouts = r.workouts();
        String politica = null;
        if (duracion) {
            var traza = traza(plan, json);
            politica = plan.politica().id();
            var porFecha = new HashMap<String, Map<String, Object>>();
            if (traza != null && traza.get("sesiones") instanceof List<?> l)
                for (var x : l)
                    if (x instanceof Map<?, ?> m) porFecha.put(String.valueOf(m.get("fecha")), cast(m));
            Integer disponibles = traza == null ? null : entero(traza.get("minutos_disponibles"));
            String finalPolitica = politica;
            workouts = workouts.stream().map(w -> w.conDuracion(duracion(w, porFecha.get(String.valueOf(
                    w.scheduledDate())), disponibles, finalPolitica, plan.getAdjustmentApplied()))).toList();
        }
        AerobicActivityResource aero = null;
        if (aerobica) {
            var bloques = new ArrayList<MetricaAerobica.Bloque>();
            for (var w : plan.workoutsView())
                for (var e : w.exercisesView()) {
                    var v = catalogo.get(e.getExerciseId());
                    if (v == null || !"LOCOMOTION_CARDIO".equalsIgnoreCase(String.valueOf(v.movementPattern()))) continue;
                    bloques.add(new MetricaAerobica.Bloque(e.getPlannedSets() == null ? 1 : e.getPlannedSets(),
                            e.getPlannedDurationSeconds() == null ? 0 : e.getPlannedDurationSeconds(),
                            e.getRestSeconds() == null ? 0 : e.getRestSeconds()));
                }
            var m = MetricaAerobica.programada(bloques);
            aero = new AerobicActivityResource(m.minutosTrabajoProgramados(), m.minutosBloqueProgramados(),
                    m.minutosEjecutados(), m.minutosExternos(), m.intensidad().name(), m.minutosEquivalentes(),
                    m.aviso());
        }
        return r.con(workouts, politica, aero);
    }

    static DurationInfoResource duracion(PlannedWorkoutResource w, Map<String, Object> sesion, Integer disponibles,
                                         String politica, String ajuste) {
        if (sesion == null || disponibles == null)
            return new DurationInfoResource(null, (int) w.expectedDurationMinutes(), null, politica, "SIN_DATOS",
                    "Duracion estimada: %d minutos.".formatted(w.expectedDurationMinutes()));
        long seg = sesion.get("segundos_estimados") instanceof Number n ? n.longValue() : w.expectedDurationMinutes() * 60L;
        int estimados = (int) Math.max(1, Math.round(seg / 60.0));
        String motivo;
        if (Boolean.TRUE.equals(sesion.get("excepcion_c2d"))) motivo = "ESTRUCTURA_COMPLETA";
        else if (Boolean.TRUE.equals(sesion.get("exenta_v8c")))
            motivo = ajuste != null && ajuste.toUpperCase(Locale.ROOT).contains("ACORTAR") ? "SESION_ACORTADA"
                    : "REDUCCION_AUTORIZADA";
        else motivo = "DENTRO_DEL_TIEMPO";
        var texto = new StringBuilder("Tu rutina tiene una duracion estimada de %d minutos. Dispones de hasta %d minutos"
                .formatted(estimados, disponibles)).append(" para entrenar.");
        switch (motivo) {
            // Neutral: no afirma que el contenido sea suficiente ni completo; solo de donde sale la duracion.
            case "ESTRUCTURA_COMPLETA" -> texto.append(" La duracion se calculo segun los ejercicios disponibles y las"
                    + " restricciones de tu plan.");
            case "REDUCCION_AUTORIZADA" -> texto.append(" Esta semana la sesion es mas corta porque tu plan se adapto.");
            case "SESION_ACORTADA" -> texto.append(" Se acorto para ajustarse al tiempo que indicaste.");
            default -> { }
        }
        return new DurationInfoResource(disponibles, estimados, disponibles - estimados, politica, motivo,
                texto.toString());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> traza(WeeklyTrainingPlan plan, JsonSupport json) {
        if (plan.getPlanningPolicyTrace() == null || json == null) return null;
        try {
            return json.read(plan.getPlanningPolicyTrace(), Map.class);
        } catch (RuntimeException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> cast(Map<?, ?> m) {
        return (Map<String, Object>) m;
    }

    private static Integer entero(Object o) {
        return o instanceof Number n ? n.intValue() : null;
    }
}
