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

    public PlanLimitationsService(WeeklyTrainingPlanCommandService commandService) {
        this.commandService = commandService;
    }

    public PlanLimitationsResource limitationsFor(Long userId) {
        LocalDate semana = commandService.firstPlanWeekStart(userId);
        List<MandatorySlot> vacios;
        List<CandidateExercise> elegibles;
        String objetivo;
        String nivel;
        try {
            var ctx = commandService.prepare(
                    new GenerateWeeklyPlanCommand(userId, semana, null, true, null, null)).context();
            elegibles = ctx.availableExercises();
            vacios = SlotCoveragePolicy.ofEligible(elegibles).emptySlots();
            objetivo = ctx.profile().goalType();
            nivel = ctx.profile().fitnessLevel();
        } catch (InsufficientSlotCoverageException e) {
            // Modo que bloquea: no hay contexto, pero si los slots vacios.
            return new PlanLimitationsResource(semana, e.emptySlots().stream().map(this::deSlot).toList());
        }

        var limitaciones = new ArrayList<Limitation>();
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
