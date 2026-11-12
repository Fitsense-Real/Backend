package main.web.services.fitsense.planning.application.internal.queryservices;

import main.web.services.fitsense.planning.domain.exceptions.InsufficientSlotCoverageException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.GoalCapabilityPolicy.Capability;
import main.web.services.fitsense.planning.domain.model.valueobjects.MandatorySlot;
import main.web.services.fitsense.planning.domain.services.GoalFeasibilityPolicy;
import main.web.services.fitsense.planning.domain.services.GoalFeasibilityPolicy.Veredicto;
import main.web.services.fitsense.planning.domain.services.SlotCoveragePolicy;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.interfaces.rest.resources.PlanLimitationsResource;
import main.web.services.fitsense.planning.interfaces.rest.resources.PlanLimitationsResource.Limitation;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Decision I (30/10/2026): una capacidad o un slot que no se puede cubrir con el equipamiento
 * declarado se ACEPTA como no factible (V23 no se toca para fabricar cobertura), pero el
 * participante tiene que saberlo.
 * <p>
 * Mide el mismo conjunto elegible que usara la generacion (prepare: equipamiento, dificultad,
 * restricciones declaradas y por edad) y no escribe nada. Los mensajes no obligan a comprar:
 * solo recuerdan que, si el participante ya tiene un implemento, puede declararlo.
 */
@Service
public class PlanLimitationsService {

    /** Implementos con los que una traccion es posible en casa sin maquinas. */
    static final List<String> TRACCION_EN_CASA = List.of("band", "wall bars", "anchor point");

    private static final Map<MandatorySlot, String[]> SLOTS = Map.of(
            MandatorySlot.UPPER_PULL, new String[]{"Ejercicios de tracción",
                    "Con el equipamiento que indicaste no hay ejercicios de tracción (remos o dominadas) "
                            + "seguros para ti. Tu plan trabaja empuje, piernas y core. Si ya tienes una banda "
                            + "elástica, una espaldera o un punto de anclaje, puedes añadirlo en tu perfil. "
                            + "No necesitas comprar nada."},
            MandatorySlot.UPPER_PUSH, new String[]{"Ejercicios de empuje",
                    "Con tu equipamiento y tus restricciones no hay ejercicios de empuje (flexiones o "
                            + "presses) seguros para ti. El plan trabaja el resto del cuerpo."},
            MandatorySlot.LOWER_KNEE, new String[]{"Ejercicios de rodilla",
                    "Con tu equipamiento y tus restricciones no hay sentadillas ni zancadas seguras para "
                            + "ti. El plan trabaja el resto del cuerpo."},
            MandatorySlot.LOWER_HIP, new String[]{"Ejercicios de cadera",
                    "Con tu equipamiento y tus restricciones no hay ejercicios de cadera (puentes o "
                            + "bisagras) seguros para ti. El plan trabaja el resto del cuerpo."});

    private static final Map<Capability, String> CAPACIDADES = Map.of(
            Capability.STRENGTH, "fuerza",
            Capability.HYPERTROPHY, "ganancia muscular",
            Capability.POWER, "potencia",
            Capability.MUSCULAR_ENDURANCE, "resistencia muscular",
            Capability.CARDIO, "cardio",
            Capability.MOBILITY, "movilidad",
            Capability.BALANCE, "equilibrio");

    private final WeeklyTrainingPlanCommandService commandService;
    private final main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories.WeeklyTrainingPlanRepository planes;
    private static final com.fasterxml.jackson.databind.ObjectMapper JSON = new com.fasterxml.jackson.databind.ObjectMapper();

    public PlanLimitationsService(WeeklyTrainingPlanCommandService commandService,
                                  main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories.WeeklyTrainingPlanRepository planes) {
        this.commandService = commandService;
        this.planes = planes;
    }

    static final Map<String, String> GRUPOS = Map.of("chest", "pecho", "back", "espalda", "shoulders", "hombros",
            "upper legs", "piernas", "upper arms", "brazos");

    /**
     * Cierre REV2, fase 5: la DOSIS de la semana publicada (traza {@code dosis}, solo politicas nuevas) llega a la
     * persona, no solo a la traza interna. Con DOSIS_LIMITADA se dice en que queda por debajo de la dosis orientativa,
     * sin prometer resultados y sin anadir volumen. Planes sin esa traza (historica): nada.
     */
    static java.util.Optional<Limitation> dosis(String trazaJson) {
        if (trazaJson == null || trazaJson.isBlank()) return java.util.Optional.empty();
        try {
            var d = JSON.readTree(trazaJson).path("dosis");
            if (!"DOSIS_LIMITADA".equals(d.path("estado").asText())) return java.util.Optional.empty();
            var partes = new ArrayList<String>();
            d.path("grupos_bajo_objetivo").forEach(g -> partes.add(GRUPOS.getOrDefault(g.asText(), g.asText())));
            long aer = d.path("aerobico_trabajo_minutos").asLong(0), obj = d.path("aerobico_objetivo_minutos").asLong(0);
            if (obj > 0 && aer < obj)
                partes.add("trabajo aeróbico (%d de %d minutos orientativos)".formatted(aer, obj));
            if (partes.isEmpty()) return java.util.Optional.empty();
            return java.util.Optional.of(new Limitation("DOSE", "DOSIS_LIMITADA", "Dosis semanal",
                    ("Con tus días, tu tiempo y tu equipamiento, el plan de esta semana queda por debajo de la dosis"
                            + " semanal orientativa en: %s. Es un plan válido y seguro; no añadimos volumen solo para"
                            + " compensar. Las cifras orientativas están pendientes de validación profesional.")
                            .formatted(String.join(", ", partes)),
                    List.of()));
        } catch (Exception e) {
            return java.util.Optional.empty();
        }
    }

    /**
     * Auditoria del catalogo v46, decision E2-a (adoptada provisionalmente): ejercicios cuya contribucion a la dosis de
     * espalda u hombro NO esta demostrada. 775 (extension de tronco en banco contada como espalda), 973 (gancho de boxeo
     * contado como hombro) y 1261, 1308, 1259 y 1146 (trabajo de tronco contado como espalda). No se reclasifican ni se
     * retiran (tampoco de HIST): solo se avisa cuando son la UNICA fuente de dosis de ese grupo en el plan publicado.
     */
    static final java.util.Set<Long> DOSIS_NO_DEMOSTRADA = java.util.Set.of(775L, 973L, 1261L, 1308L, 1259L, 1146L);
    private static final java.util.Set<String> SIN_DOSIS_RESISTIDA = java.util.Set.of("LOCOMOTION_CARDIO", "MOBILITY",
            "BALANCE_CONTROL", "OTHER");

    /**
     * Aviso E2-a: para espalda y hombros, si todos los ejercicios resistidos del plan que se cuentan para ese grupo son
     * de {@link #DOSIS_NO_DEMOSTRADA}, se dice que esa dosis no esta demostrada. No cambia el plan, no anade volumen y
     * no afirma que el grupo este trabajado. Un ejercicio del plan que ya no esta en el catalogo elegible no se cuenta.
     */
    static java.util.Optional<Limitation> dosisNoDemostrada(List<Long> ejerciciosDelPlan, List<CandidateExercise> catalogo) {
        if (ejerciciosDelPlan == null || ejerciciosDelPlan.isEmpty() || catalogo == null) return java.util.Optional.empty();
        var porId = new java.util.HashMap<Long, CandidateExercise>();
        catalogo.forEach(c -> porId.putIfAbsent(c.exerciseId(), c));
        var grupos = new ArrayList<String>();
        var nombres = new java.util.TreeSet<String>();
        for (var grupo : List.of("back", "shoulders")) {
            var fuentes = ejerciciosDelPlan.stream().distinct().map(porId::get).filter(java.util.Objects::nonNull)
                    .filter(c -> grupo.equals(c.bodyPartCode()))
                    .filter(c -> c.movementPattern() == null || !SIN_DOSIS_RESISTIDA.contains(c.movementPattern()))
                    .toList();
            if (fuentes.isEmpty() || !fuentes.stream().allMatch(c -> DOSIS_NO_DEMOSTRADA.contains(c.exerciseId())))
                continue;
            grupos.add(GRUPOS.getOrDefault(grupo, grupo));
            fuentes.forEach(c -> nombres.add(c.name()));
        }
        if (grupos.isEmpty()) return java.util.Optional.empty();
        return java.util.Optional.of(new Limitation("DOSE", "DOSIS_NO_DEMOSTRADA", "Trabajo de " + String.join(" y ", grupos),
                ("En tu plan, el trabajo de %s procede solo de ejercicios cuya clasificación está en revisión (%s). No "
                        + "podemos afirmar que aporten la dosis orientativa a ese grupo, así que no lo contamos como trabajo "
                        + "demostrado. Tu plan sigue siendo válido y seguro; la revisión profesional del catálogo está "
                        + "pendiente.").formatted(String.join(" y ", grupos), String.join(", ", nombres)),
                List.of()));
    }

    public PlanLimitationsResource limitationsFor(Long userId) {
        LocalDate semana = commandService.firstPlanWeekStart(userId);
        List<MandatorySlot> vacios;
        List<CandidateExercise> elegibles;
        String objetivo;
        String nivel;
        int diasPedidos;
        java.util.Optional<Limitation> fuerzaTiempo;
        try {
            var ctx = commandService.prepare(
                    new GenerateWeeklyPlanCommand(userId, semana, null, true, null, null)).context();
            elegibles = ctx.availableExercises();
            vacios = SlotCoveragePolicy.ofEligible(elegibles).emptySlots();
            objetivo = ctx.profile().goalType();
            nivel = ctx.profile().fitnessLevel();
            diasPedidos = ctx.profile().daysPerWeek();
            fuerzaTiempo = main.web.services.fitsense.planning.domain.services.CoberturaFuerzaTiempo
                    .evaluar(ctx, new main.web.services.fitsense.planning.domain.services.SessionDurationEstimator())
                    .map(d -> new Limitation("TIME", main.web.services.fitsense.planning.domain.exceptions
                            .FuerzaSinTiempoException.CODIGO, "Tiempo para la fuerza",
                            main.web.services.fitsense.planning.domain.exceptions.FuerzaSinTiempoException
                                    .mensaje(d, ctx.effectiveSessionMinutes()).replaceFirst("^[A-Z_]+: ", ""),
                            List.of()));
        } catch (InsufficientSlotCoverageException e) {
            // Modo que bloquea: no hay contexto, pero si los slots vacios.
            return new PlanLimitationsResource(semana, e.emptySlots().stream().map(this::deSlot).toList());
        }

        var limitaciones = new ArrayList<Limitation>();
        frecuencia(diasPedidos, nivel).ifPresent(limitaciones::add);
        if (planes != null) {
            var ultimo = planes.findFirstByUserIdOrderByWeekNumberDescPlanVersionDesc(userId);
            ultimo.flatMap(p -> dosis(p.getPlanningPolicyTrace())).ifPresent(limitaciones::add);
            ultimo.flatMap(p -> dosisNoDemostrada(planes.exerciseIdsOfPlan(p.getId()), elegibles))
                    .ifPresent(limitaciones::add);
        }
        fuerzaTiempo.ifPresent(limitaciones::add);
        vacios.stream().map(this::deSlot).forEach(limitaciones::add);
        GoalFeasibilityPolicy.evaluarPool(objetivo, nivel, elegibles).ifPresent(pool -> pool.principales().stream()
                .filter(c -> c.aplica() && c.veredicto() == Veredicto.NO_FACTIBLE)
                // Una capacidad que no se cubre solo porque falta la traccion ya la explica el slot.
                .filter(c -> !(vacios.contains(MandatorySlot.UPPER_PULL) && soloFaltaTraccion(c)))
                .map(c -> new Limitation("CAPABILITY", c.capacidad().name(),
                        "Objetivo: " + CAPACIDADES.getOrDefault(c.capacidad(), c.capacidad().name()),
                        "Tu objetivo incluye " + CAPACIDADES.getOrDefault(c.capacidad(), c.capacidad().name())
                                + ", pero con el equipamiento y las restricciones que indicaste no hay "
                                + "ejercicios suficientes para trabajarla con seguridad. El plan se centra en "
                                + "lo que sí puedes hacer. Si ya tienes otro equipamiento, puedes añadirlo en "
                                + "tu perfil.",
                        List.of()))
                .forEach(limitaciones::add));
        return new PlanLimitationsResource(semana, List.copyOf(limitaciones));
    }

    /**
     * S-MAX (contrato de frecuencia, FrecuenciaSemanal): si el tope del nivel recorta los dias pedidos, el participante
     * tiene que saber cuantas sesiones se programan y que completarlas es el 100 %. No cambia el tope.
     */
    static java.util.Optional<Limitation> frecuencia(int diasPedidos, String nivel) {
        if (!main.web.services.fitsense.shared.domain.model.valueobjects.FrecuenciaSemanal
                .recortaPorNivel(diasPedidos, nivel)) return java.util.Optional.empty();
        int sesiones = main.web.services.fitsense.shared.domain.model.valueobjects.FrecuenciaSemanal.tope(nivel);
        String n = switch (nivel == null ? "" : nivel) {
            case "BEGINNER" -> "principiante";
            case "ADVANCED" -> "avanzado";
            default -> "intermedio";
        };
        return java.util.Optional.of(new Limitation("FREQUENCY", "S_MAX", "Sesiones por semana",
                ("Indicaste %d días para entrenar. Con tu nivel (%s), tu plan programa %d sesiones por semana y deja "
                        + "los demás días para recuperarte. Completar esas %d sesiones cuenta como el 100 %% de tu "
                        + "plan.").formatted(diasPedidos, n, sesiones, sesiones),
                List.of()));
    }

    private Limitation deSlot(MandatorySlot slot) {
        var t = SLOTS.get(slot);
        return new Limitation("SLOT", slot.name(), t[0], t[1],
                slot == MandatorySlot.UPPER_PULL ? TRACCION_EN_CASA : List.of());
    }

    private static boolean soloFaltaTraccion(GoalFeasibilityPolicy.CapacidadEvaluada c) {
        return c.rutas().stream().allMatch(r -> r.ruta() == GoalFeasibilityPolicy.Ruta.SLOTS_RESISTIDOS
                && r.porSlot() != null && r.porSlot().entrySet().stream()
                .allMatch(e -> e.getKey() == MandatorySlot.UPPER_PULL || e.getValue() > 0));
    }
}
