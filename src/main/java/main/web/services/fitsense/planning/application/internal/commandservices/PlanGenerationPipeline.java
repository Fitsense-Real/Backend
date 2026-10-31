package main.web.services.fitsense.planning.application.internal.commandservices;

import main.web.services.fitsense.planning.domain.exceptions.AiProviderUnavailableException;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.exceptions.PlanGenerationFailedException;
import main.web.services.fitsense.planning.domain.services.CapabilityCoveragePolicy;
import main.web.services.fitsense.planning.domain.services.PlanDraftNormalizer;
import main.web.services.fitsense.planning.domain.model.valueobjects.GoalCapabilityPolicy;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.TrainingPlanGenerator;
import main.web.services.fitsense.planning.infrastructure.generation.ai.AirCircuitBreaker;
import main.web.services.fitsense.planning.infrastructure.generation.ai.AiRetryFeedback;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateTrainingPlanGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * La politica de fallo de 19.4, literal:
 * <pre>
 *   Intento 1 falla -> se reenvia con su propuesta y los motivos de rechazo.
 *   Intentos 2 y 3   -> igual; 3 desde el plan 36, donde el segundo intento
 *                       quedo a un solo campo de ser valido.
 *   Los 3 fallan    -> generador de reglas.
 *   Regla falla     -> no se activa la semana.
 * </pre>
 * El respaldo por reglas no es opcional: sin el, una caida del proveedor deja
 * participantes sin plan y produce perdida de datos irrecuperable.
 * <p>
 * Devuelve tambien cuantos intentos hicieron falta, porque la proporcion de
 * planes validos al primer intento es un resultado reportable de la tesis.
 */
@Component
public class PlanGenerationPipeline {

    private static final Logger log = LoggerFactory.getLogger(PlanGenerationPipeline.class);
    private static final int AI_ATTEMPTS = 3;

    private final ReplicateTrainingPlanGenerator aiGenerator;
    private final TrainingPlanGenerator ruleGenerator;
    private final PlanDraftValidator validator;
    private final PlanDraftNormalizer normalizer;

    /**
     * El cortacircuitos, antes inline aqui. Se extrajo el 13/10/2026 para poder
     * PROBARLO y, sobre todo, para poder VERLO: el aviso de que una generacion
     * se salta la IA estaba en {@code log.debug} y el nivel por omision es INFO,
     * asi que diez generaciones podian salir por reglas sin dejar rastro. La
     * politica -3 fallos seguidos, 10 minutos- no cambia.
     */
    private final AirCircuitBreaker breaker;

    /**
     * ESTE {@code @Autowired} NO ES DECORATIVO. Al haber dos constructores,
     * Spring pierde el atajo de "si hay uno solo con argumentos, usalo" y el
     * contexto no arranca sin marcar cual es el bueno. Ya costo un incidente
     * (INC-4C-01) en {@code PlanDraftValidator}.
     */
    @org.springframework.beans.factory.annotation.Autowired
    public PlanGenerationPipeline(ReplicateTrainingPlanGenerator aiGenerator,
                                  @Qualifier("ruleBasedTrainingPlanGenerator")
                                  TrainingPlanGenerator ruleGenerator,
                                  PlanDraftValidator validator,
                                  PlanDraftNormalizer normalizer) {
        this(aiGenerator, ruleGenerator, validator, normalizer, new AirCircuitBreaker());
    }

    /**
     * Para pruebas: permite inyectar un cortacircuitos con reloj falso y
     * comprobar la ventana de diez minutos sin esperar diez minutos. Es de
     * paquete a proposito, para no dejar dos constructores publicos.
     */
    PlanGenerationPipeline(ReplicateTrainingPlanGenerator aiGenerator,
                           TrainingPlanGenerator ruleGenerator,
                           PlanDraftValidator validator,
                           PlanDraftNormalizer normalizer,
                           AirCircuitBreaker breaker) {
        this.aiGenerator = aiGenerator;
        this.ruleGenerator = ruleGenerator;
        this.validator = validator;
        this.normalizer = normalizer;
        this.breaker = breaker;
    }

    /** @param attempts numero de intentos consumidos, para generation_attempts. */
    public record Result(PlanDraft draft, short attempts) {}

    public Result run(PlanGenerationContext context, int durationToRepsDivisor) {
        registrarCapacidades(context);
        short attempts = 0;
        // Todos los motivos de rechazo, en orden y sin repetir: los recibe el
        // motor de reglas y, en el reintento, la parte que no es del ultimo.
        var acumulados = new java.util.LinkedHashSet<String>();
        String ultimaPropuesta = null;
        List<String> ultimosMotivos = List.of();
        List<String> problems = List.of();

        if (aiGenerator.isEnabled() && proveedorDisponible(context.userId())) {
            for (int attempt = 1; attempt <= AI_ATTEMPTS; attempt++) {
                attempts++;
                String output = null;
                try {
                    // Se ACUMULAN los problemas de todos los intentos. En la prueba
                    // el segundo intento solo recibio el error del primero,
                    // corrigio ese y cometio otro del mismo tipo. Ademas, desde el
                    // plan 29 el reintento recibe su propuesta rechazada: sin ella
                    // generaba un plan nuevo y no corregia el anterior.
                    var anteriores = new java.util.ArrayList<>(acumulados);
                    anteriores.removeAll(ultimosMotivos);
                    var feedback = attempt == 1 ? AiRetryFeedback.none()
                            : new AiRetryFeedback(ultimaPropuesta, ultimosMotivos, anteriores);

                    // Se pasa el numero de intento para que la linea
                    // REPLICATE_TIMING se pueda atar a ESTA generacion: un mismo
                    // plan puede provocar hasta tres, y el id de prediccion solo
                    // no los distingue.
                    output = aiGenerator.requestOutput(context, feedback, attempt);
                    // El tipo de prescripcion se corrige antes de validar: es un
                    // dato del catalogo, no una decision del modelo. Todo lo
                    // demas se valida tal cual lo propuso.
                    var draft = normalizer.normalize(aiGenerator.toDraft(output), context).draft();
                    validator.validate(draft, context, durationToRepsDivisor);
                    breaker.registrarRespuestaDelProveedor();
                    registrarCobertura(draft, context);
                    return new Result(draft, attempts);

                } catch (AiProviderUnavailableException e) {
                    // El proveedor esta caido: no tiene sentido gastar el segundo
                    // intento, va a fallar igual.
                    registrarFalloDeProveedor(context.userId());
                    acumulados.addAll(e.problems());
                    break;

                } catch (InvalidPlanDraftException e) {
                    // El modelo respondio pero incumplio validaciones. Eso NO
                    // cuenta para el cortocircuito: es su capacidad, no una caida.
                    breaker.registrarRespuestaDelProveedor();
                    ultimaPropuesta = output;
                    ultimosMotivos = List.copyOf(e.problems());
                    acumulados.addAll(e.problems());
                    log.warn("Intento {} de IA rechazado para el usuario {}: {}",
                            attempt, context.userId(), e.problems());
                }
            }
        }
        problems = List.copyOf(acumulados);

        // Respaldo determinista. Se cuenta como intento propio para que
        // generation_attempts refleje el coste real de producir la semana.
        attempts++;
        try {
            var draft = ruleGenerator.generate(context, problems);
            validator.validate(draft, context, durationToRepsDivisor);
            registrarCobertura(draft, context);
            return new Result(draft, attempts);
        } catch (InvalidPlanDraftException e) {
            // El motor de reglas fallando indica que el conjunto elegible o el
            // perfil hacen imposible cualquier plan valido. Es un dato, no un
            // error tecnico: la semana queda sin plan y su adherencia sera NULL.
            log.error("El motor de reglas tampoco produjo un plan valido para el usuario {}: {}",
                    context.userId(), e.problems());
            throw new PlanGenerationFailedException(context.userId(),
                    "Ultimos problemas: " + String.join(" | ", e.problems()));
        }
    }

    /**
     * P-03 · SOLO EL MOTOR DE REGLAS, con la validacion completa (V1-V25). Para la regeneracion segura
     * dentro del ciclo: determinista, sin llamadas a la IA (P-05 no autoriza gasto) y con el conjunto
     * elegible que ya excluye lo que dolio (P-02). Lanza {@code PlanGenerationFailedException} si el
     * borrador no pasa: nunca devuelve un plan invalido.
     */
    public Result runDeterministic(PlanGenerationContext context, int durationToRepsDivisor) {
        registrarCapacidades(context);
        try {
            var draft = ruleGenerator.generate(context, List.of());
            validator.validate(draft, context, durationToRepsDivisor);
            registrarCobertura(draft, context);
            return new Result(draft, (short) 1);
        } catch (InvalidPlanDraftException e) {
            throw new PlanGenerationFailedException(context.userId(),
                    "Regeneracion determinista: " + String.join(" | ", e.problems()));
        }
    }

    /** Solo para el log: que capacidades del objetivo lleva la semana. No decide nada. */
    private void registrarCobertura(PlanDraft draft, PlanGenerationContext context) {
        var profile = context.profile();
        if (profile == null || GoalCapabilityPolicy.forGoal(profile.goalType()).isEmpty()) return;
        var cobertura = CapabilityCoveragePolicy.evaluar(profile.goalType(), profile.fitnessLevel(), draft,
                context.availableExercises(), context.prescription() == null ? null : context.prescription().repLimits());
        log.info("COBERTURA_SEMANA user={} semana={} origen={} {}", context.userId(), context.weekNumber(),
                draft.source(), cobertura.describe());
    }

    /**
     * Paso 1 · 4A v6, SOLO OBSERVACION. Deja en el log que capacidades exige el
     * objetivo antes de generar; no decide nada ni entra en el prompt. Va aqui
     * y no en {@code prepare()} para no tocar el camino de adaptacion (P2-D).
     */
    private void registrarCapacidades(PlanGenerationContext context) {
        var profile = context.profile();
        String goal = profile == null ? null : profile.goalType();
        String level = profile == null ? null : profile.fitnessLevel();
        GoalCapabilityPolicy.forGoal(goal).ifPresentOrElse(
                capacidades -> log.info("CAPACIDADES_4A user={} semana={} {} modo=OBSERVE",
                        context.userId(), context.weekNumber(), capacidades.describe(level)),
                () -> log.warn("CAPACIDADES_4A user={} semana={} version={} objetivo={} "
                                + "sin_matriz modo=OBSERVE",
                        context.userId(), context.weekNumber(), GoalCapabilityPolicy.VERSION, goal));
    }

    /**
     * Cortacircuitos. Sin el, con Replicate caido cada participante del cierre
     * semanal espera su timeout completo: con diez participantes y 90 segundos
     * son quince minutos, y todos acaban en el motor de reglas igualmente.
     * <p>
     * Es una politica del estudio, no una preocupacion generica de red, por eso
     * no se trae una libreria: el respaldo por reglas ya existe y lo unico que
     * hace falta es dejar de esperar.
     */
    private boolean proveedorDisponible(Long userId) {
        if (breaker.abierto()) {
            // ANTES ERA log.debug Y ASI NO SE VEIA. Es el unico aviso de que esta
            // generacion no paso por la IA; sin el, una comparacion entre
            // perfiles puede estar hecha toda con el motor de reglas y nadie se
            // entera. Va en WARN porque no es el camino previsto.
            log.warn("IA_NO_INVOCADA user={} motivo=CIRCUIT_BREAKER_OPEN {} destino=RULE_ENGINE",
                    userId == null ? "desconocido" : userId, breaker.describe());
            return false;
        }
        return true;
    }

    private void registrarFalloDeProveedor(Long userId) {
        if (breaker.registrarFalloDeProveedor()) {
            log.error("El proveedor de IA fallo {} veces seguidas (usuario {}). Se usa el "
                            + "motor de reglas durante {} minutos, hasta {}.",
                    breaker.fallosQueAbrieron(), userId,
                    AirCircuitBreaker.DESCANSO.toMinutes(), breaker.reabrirDespuesDe());
        } else {
            // Nuevo: el contador intermedio tambien se ve. En la medicion del
            // 13/10 quedo en 2 de 3 y no habia forma de saberlo desde el log.
            log.warn("Fallo del proveedor de IA para el usuario {}: {} de {} seguidos",
                    userId, breaker.fallosSeguidos(), AirCircuitBreaker.UMBRAL);
        }
    }
}