package main.web.services.fitsense.planning.application.internal.commandservices;

import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalCatalogService;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanningProfile;
import main.web.services.fitsense.planning.domain.model.valueobjects.SafetyProfile;
import main.web.services.fitsense.planning.domain.services.GoalFeasibilityPolicy;
import main.web.services.fitsense.planning.domain.services.GoalFeasibilityPolicy.CapacidadEvaluada;
import main.web.services.fitsense.planning.domain.services.GoalFeasibilityPolicy.EvaluacionPool;
import main.web.services.fitsense.planning.domain.services.GoalFeasibilityPolicy.Ruta;
import main.web.services.fitsense.planning.domain.services.GoalFeasibilityPolicy.Veredicto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Paso 3 · OBSERVACION de la factibilidad por objetivo en la generacion
 * inicial. No rechaza, no filtra, no cambia el contexto ni el borrador.
 * <ul>
 *   <li>{@code FACTIBILIDAD_POOL}: nivel A, antes de llamar a cualquier
 *       generador.</li>
 *   <li>{@code ESCASEZ}: si alguna principal no es robusta, que restriccion la
 *       provoca. Se recuenta levantando UNA restriccion cada vez; esos
 *       conjuntos son hipoteticos y NUNCA llegan a un generador.</li>
 *   <li>{@code COBERTURA_SEMANAL}: nivel B, sobre el borrador aceptado.</li>
 *   <li>{@code DIVERGENCIA_V23}: el objetivo no exige los cuatro slots y V23
 *       (4C) si.</li>
 * </ul>
 * Un fallo aqui se registra y se traga: la observacion no puede dejar a nadie
 * sin plan.
 */
@Component
public class GoalFeasibilityObserver {

    private static final Logger log = LoggerFactory.getLogger(GoalFeasibilityObserver.class);

    private final ExternalCatalogService catalog;

    public GoalFeasibilityObserver(ExternalCatalogService catalog) {
        this.catalog = catalog;
    }

    public void antesDeGenerar(PlanGenerationContext context) {
        try {
            var perfil = context.profile();
            var pool = GoalFeasibilityPolicy.evaluarPool(perfil.goalType(), perfil.fitnessLevel(),
                    context.availableExercises());
            if (pool.isEmpty()) {
                log.warn("FACTIBILIDAD_POOL user={} objetivo={} sin_matriz modo=OBSERVE",
                        context.userId(), perfil.goalType());
                return;
            }
            log.info("FACTIBILIDAD_POOL user={} semana={} {} modo=OBSERVE",
                    context.userId(), context.weekNumber(), pool.get().describe());
            if (pool.get().veredicto() != Veredicto.ROBUSTO)
                log.warn("ESCASEZ user={} objetivo={} motivos={} causas={} (recuentos hipoteticos, "
                                + "no se aplica ninguno) modo=OBSERVE",
                        context.userId(), perfil.goalType(), pool.get().motivos(),
                        causasDeEscasez(context, pool.get()));
            if (!GoalFeasibilityPolicy.exigeSlotsResistidos(perfil.goalType()))
                log.info("DIVERGENCIA_V23 user={} objetivo={}: 4A v6 no exige los cuatro slots "
                                + "resistidos a este objetivo y V23 (4C) si. Sin cambios en V23 hasta "
                                + "que sean coherentes. modo=OBSERVE",
                        context.userId(), perfil.goalType());
        } catch (RuntimeException e) {
            log.warn("FACTIBILIDAD_POOL user={} no se pudo observar: {}", context.userId(), e.toString());
        }
    }

    public void despuesDeGenerar(PlanGenerationContext context, PlanDraft borrador) {
        try {
            var perfil = context.profile();
            GoalFeasibilityPolicy.evaluarSemana(perfil.goalType(), perfil.fitnessLevel(), borrador,
                            context.availableExercises())
                    .ifPresent(semana -> log.info("COBERTURA_SEMANAL user={} semana={} origen={} {} modo=OBSERVE",
                            context.userId(), context.weekNumber(),
                            borrador == null ? null : borrador.source(), semana.describe()));
        } catch (RuntimeException e) {
            log.warn("COBERTURA_SEMANAL user={} no se pudo observar: {}", context.userId(), e.toString());
        }
    }

    /** Un escenario hipotetico: el perfil con UNA restriccion levantada. */
    record Hipotesis(String levanta, PlanningProfile perfil, int maxDificultad, SafetyProfile seguridad) {}

    static List<Hipotesis> hipotesis(PlanGenerationContext context) {
        var p = context.profile();
        var s = context.safety() == null ? SafetyProfile.none() : context.safety();
        int dif = context.effectiveMaxDifficulty();
        var lista = new ArrayList<Hipotesis>();
        boolean[] f = {s.excludeHighImpact(), s.excludeFloorWork(), s.excludeAxialLoad(), s.excludeKneeling(),
                s.excludeHanging(), s.excludeOverhead(), s.excludeInversion()};
        String[] nombres = {"alto_impacto", "suelo", "carga_axial", "rodillas", "suspension",
                "brazos_arriba", "inversion"};
        for (int i = 0; i < f.length; i++) {
            if (!f[i]) continue;
            var g = f.clone();
            g[i] = false;
            lista.add(new Hipotesis(nombres[i], p, dif,
                    new SafetyProfile(g[0], g[1], g[2], g[3], g[4], g[5], g[6])));
        }
        if (dif < 3) lista.add(new Hipotesis("nivel_" + (dif + 1), p, dif + 1, s));
        if (p.blockedExerciseIds() != null && !p.blockedExerciseIds().isEmpty())
            lista.add(new Hipotesis("bloqueos_individuales", conBloqueos(p, List.of()), dif, s));
        if (p.excludesGymOnlyEquipment())
            lista.add(new Hipotesis("equipamiento_de_gimnasio", enGimnasio(p), dif, s));
        return lista;
    }

    /**
     * Que restriccion, levantada sola, mejoraria cada principal. Solo explica:
     * los conjuntos hipoteticos no salen de este metodo.
     */
    public List<String> causasDeEscasez(PlanGenerationContext context, EvaluacionPool actual) {
        var causas = new ArrayList<String>();
        var perfil = context.profile();
        for (var h : hipotesis(context)) {
            var elegibles = catalog.fetchEligibleForExplanation(h.perfil(), h.maxDificultad(), h.seguridad());
            var otro = GoalFeasibilityPolicy.evaluarPool(perfil.goalType(), perfil.fitnessLevel(), elegibles);
            if (otro.isEmpty()) continue;
            for (var antes : actual.principales()) {
                if (!antes.aplica()) continue;
                var despues = otro.get().principales().stream()
                        .filter(c -> c.capacidad() == antes.capacidad()).findFirst().orElse(null);
                if (despues == null || despues.veredicto().ordinal() <= antes.veredicto().ordinal()
                        && medida(despues) <= medida(antes)) continue;
                causas.add("%s:%s(%d)->%s(%d) si se levantara %s".formatted(antes.capacidad(),
                        antes.veredicto(), medida(antes), despues.veredicto(), medida(despues), h.levanta()));
            }
        }
        return causas;
    }

    /** Candidatos verificados de la mejor ruta medible (el peor slot, en los slots). */
    static int medida(CapacidadEvaluada c) {
        return c.rutas().stream().filter(r -> r.ruta() != Ruta.CIRCUITO)
                .mapToInt(GoalFeasibilityPolicy.RutaEvaluada::verificados).max().orElse(0);
    }

    private static PlanningProfile conBloqueos(PlanningProfile p, List<Long> bloqueos) {
        return new PlanningProfile(p.userId(), p.age(), p.biologicalSex(), p.heightCm(), p.weightKg(),
                p.targetWeightKg(), p.fitnessLevel(), p.goalType(), p.goalText(), p.healthNotes(),
                p.trainingLocation(), p.daysPerWeek(), p.availableDays(), p.sessionMinutes(),
                p.equipmentCodes(), bloqueos, p.declaradas());
    }

    private static PlanningProfile enGimnasio(PlanningProfile p) {
        return new PlanningProfile(p.userId(), p.age(), p.biologicalSex(), p.heightCm(), p.weightKg(),
                p.targetWeightKg(), p.fitnessLevel(), p.goalType(), p.goalText(), p.healthNotes(),
                "GYM", p.daysPerWeek(), p.availableDays(), p.sessionMinutes(),
                p.equipmentCodes(), p.blockedExerciseIds(), p.declaradas());
    }
}
