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
import main.web.services.fitsense.planning.infrastructure.generation.ai.PlazoDeGeneracion;
import main.web.services.fitsense.planning.domain.services.FactibilidadGeneracionIA;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateTrainingPlanGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
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
     * RENDIMIENTO (12/11/2026) · PRESUPUESTO TOTAL DE LA FASE DE IA. Antes solo habia limites por llamada (unos
     * 290-330 s cada una, R-3/R-2), y con tres intentos una generacion podia esperar mas de 15 minutos antes del
     * respaldo. Ahora: no se empieza un intento si quedan menos de {@link #minimoPorIntento} (la prediccion de
     * generacion mas corta medida tardo 35,7 s: con menos no puede terminar), y la que esta en curso deja de esperarse
     * al agotarse (se cancela, R-2). Los valores por omision (360 s / 40 s) cubren la generacion mas larga medida con
     * Replicate real (350 s, tres intentos, GRA): no cambian ningun desenlace observado, solo acotan la cola.
     */
    private final Duration presupuesto;
    private final Duration minimoPorIntento;

    /** RENDIMIENTO: evitar las llamadas que {@link FactibilidadGeneracionIA} demuestra inutiles (H-G9). */
    private final boolean factibilidadPrevia;

    private final Clock reloj;

    public static final int PRESUPUESTO_POR_OMISION_S = 360;
    public static final int MINIMO_POR_INTENTO_POR_OMISION_S = 40;

    /**
     * ESTE {@code @Autowired} NO ES DECORATIVO. Al haber varios constructores,
     * Spring pierde el atajo de "si hay uno solo con argumentos, usalo" y el
     * contexto no arranca sin marcar cual es el bueno. Ya costo un incidente
     * (INC-4C-01) en {@code PlanDraftValidator}.
     */
    @org.springframework.beans.factory.annotation.Autowired
    public PlanGenerationPipeline(ReplicateTrainingPlanGenerator aiGenerator,
                                  @Qualifier("ruleBasedTrainingPlanGenerator")
                                  TrainingPlanGenerator ruleGenerator,
                                  PlanDraftValidator validator,
                                  PlanDraftNormalizer normalizer,
                                  @org.springframework.beans.factory.annotation.Value(
                                          "${fitsense.ai.generate.total-budget-seconds:360}") int presupuestoSegundos,
                                  @org.springframework.beans.factory.annotation.Value(
                                          "${fitsense.ai.generate.min-attempt-seconds:40}") int minimoPorIntentoSegundos,
                                  @org.springframework.beans.factory.annotation.Value(
                                          "${fitsense.ai.generate.feasibility-precheck:true}") boolean factibilidadPrevia) {
        this(aiGenerator, ruleGenerator, validator, normalizer, new AirCircuitBreaker(), Clock.systemUTC(),
                presupuestoSegundos, minimoPorIntentoSegundos, factibilidadPrevia);
        log.info("Generacion con IA: presupuesto total {} s, sin empezar un intento con menos de {} s,"
                        + " comprobacion previa de factibilidad {}", presupuesto.toSeconds(),
                minimoPorIntento.toSeconds(), this.factibilidadPrevia ? "activa" : "apagada");
    }

    /** Con los valores por omision (pruebas). */
    public PlanGenerationPipeline(ReplicateTrainingPlanGenerator aiGenerator,
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
        this(aiGenerator, ruleGenerator, validator, normalizer, breaker, Clock.systemUTC(),
                PRESUPUESTO_POR_OMISION_S, MINIMO_POR_INTENTO_POR_OMISION_S, true);
    }

    /** Para pruebas: reloj falso y presupuesto propio. */
    PlanGenerationPipeline(ReplicateTrainingPlanGenerator aiGenerator,
                           TrainingPlanGenerator ruleGenerator,
                           PlanDraftValidator validator,
                           PlanDraftNormalizer normalizer,
                           AirCircuitBreaker breaker,
                           Clock reloj,
                           int presupuestoSegundos,
                           int minimoPorIntentoSegundos,
                           boolean factibilidadPrevia) {
        this.aiGenerator = aiGenerator;
        this.ruleGenerator = ruleGenerator;
        this.validator = validator;
        this.normalizer = normalizer;
        this.breaker = breaker;
        this.reloj = reloj == null ? Clock.systemUTC() : reloj;
        // Acotados, no rechazados: 60 s - 30 min; el minimo por intento, 10 s - 150 s y nunca mayor que el total.
        int total = presupuestoSegundos <= 0 ? PRESUPUESTO_POR_OMISION_S : Math.max(60, Math.min(presupuestoSegundos, 1800));
        int minimo = minimoPorIntentoSegundos <= 0 ? MINIMO_POR_INTENTO_POR_OMISION_S
                : Math.max(10, Math.min(minimoPorIntentoSegundos, 150));
        this.presupuesto = Duration.ofSeconds(total);
        this.minimoPorIntento = Duration.ofSeconds(Math.min(minimo, total));
        this.factibilidadPrevia = factibilidadPrevia;
    }

    public Duration presupuesto() { return presupuesto; }

    public Duration minimoPorIntento() { return minimoPorIntento; }

    /** @param attempts numero de intentos consumidos, para generation_attempts. */
    public record Result(PlanDraft draft, short attempts) {}

    public Result run(PlanGenerationContext context, int durationToRepsDivisor) {
        registrarCapacidades(context);
        var tiempos = new Tiempos(context);
        try {
            return run(context, durationToRepsDivisor, tiempos);
        } finally {
            tiempos.registrar();
        }
    }

    private Result run(PlanGenerationContext context, int durationToRepsDivisor, Tiempos tiempos) {
        short attempts = 0;
        // Todos los motivos de rechazo, en orden y sin repetir: los recibe el
        // motor de reglas y, en el reintento, la parte que no es del ultimo.
        var acumulados = new java.util.LinkedHashSet<String>();
        String ultimaPropuesta = null;
        List<String> ultimosMotivos = List.of();
        List<String> problems = List.of();

        tiempos.motivo = aiGenerator.isEnabled() ? "CIRCUIT_BREAKER" : "IA_APAGADA";
        var inviable = aiGenerator.isEnabled() && factibilidadPrevia
                ? FactibilidadGeneracionIA.evaluar(context, validator.coverageEnforcement(),
                        validator.capabilityEnforcement())
                : java.util.Optional.<FactibilidadGeneracionIA.Inviabilidad>empty();
        if (inviable.isPresent()) {
            // RENDIMIENTO (H-G9): ninguna propuesta puede pasar V4 con V23 + V24 + V25. Pedirla eran hasta tres
            // predicciones rechazadas antes de este mismo motor de reglas. Ni cuenta para el corte ni es un fallo.
            tiempos.motivo = "INVIABLE_PARA_IA";
            log.warn("IA_NO_INVOCADA user={} motivo=INVIABLE_PARA_IA {} destino=RULE_ENGINE",
                    context.userId(), inviable.get().describe());
        } else if (aiGenerator.isEnabled() && proveedorDisponible(context.userId())) {
            tiempos.motivo = "RECHAZOS";
            var plazo = reloj.instant().plus(presupuesto);
            for (int attempt = 1; attempt <= AI_ATTEMPTS; attempt++) {
                var restante = Duration.between(reloj.instant(), plazo);
                if (restante.compareTo(minimoPorIntento) < 0) {
                    // RENDIMIENTO: con menos de lo que tarda la prediccion mas corta medida, el intento no puede
                    // terminar dentro del presupuesto. Decide el motor de reglas, como tras un rechazo.
                    tiempos.motivo = "PRESUPUESTO";
                    log.warn("IA_PRESUPUESTO_AGOTADO user={} intento={} quedan_s={} minimo_s={} presupuesto_s={}"
                                    + " destino=RULE_ENGINE", context.userId(), attempt, restante.toSeconds(),
                            minimoPorIntento.toSeconds(), presupuesto.toSeconds());
                    break;
                }
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
                    final int n = attempt;
                    final var fb = feedback;
                    long t0 = System.nanoTime();
                    try {
                        output = PlazoDeGeneracion.durante(plazo,
                                () -> aiGenerator.requestOutput(context, fb, n));
                    } finally {
                        tiempos.ia(System.nanoTime() - t0);
                    }
                    // El tipo de prescripcion se corrige antes de validar: es un
                    // dato del catalogo, no una decision del modelo. Todo lo
                    // demas se valida tal cual lo propuso.
                    long t1 = System.nanoTime();
                    try {
                        var draft = normalizer.normalize(aiGenerator.toDraft(output), context).draft();
                        validator.validate(draft, context, durationToRepsDivisor);
                        breaker.registrarRespuestaDelProveedor();
                        registrarCobertura(draft, context);
                        tiempos.motivo = null;
                        tiempos.origen = "IA";
                        return new Result(draft, attempts);
                    } finally {
                        tiempos.validacion(System.nanoTime() - t1);
                    }

                } catch (AiProviderUnavailableException e) {
                    // El proveedor esta caido: no tiene sentido gastar el segundo
                    // intento, va a fallar igual.
                    if (!reloj.instant().isBefore(plazo)) {
                        // RENDIMIENTO: la prediccion se dejo de esperar porque se agoto el presupuesto total, no
                        // porque el proveedor fallara: no cuenta para el cortacircuitos.
                        tiempos.motivo = "PRESUPUESTO";
                        log.warn("IA_PRESUPUESTO_AGOTADO user={} intento={} durante la prediccion; destino=RULE_ENGINE",
                                context.userId(), attempt);
                    } else {
                        tiempos.motivo = "PROVEEDOR";
                        registrarFalloDeProveedor(context.userId());
                    }
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

                } catch (RuntimeException e) {
                    // R-1: cualquier otro error en el camino de la IA (un defecto propio, un dato inesperado) NO deja
                    // la semana sin plan: se registra en ERROR, no se gastan mas intentos ni cuenta para el corte, y
                    // decide el motor de reglas, validado como siempre. Nunca se publica lo que lanzo.
                    tiempos.motivo = "ERROR_INESPERADO";
                    log.error("IA_ERROR_INESPERADO usuario {} intento {}: {}; la semana la hace el motor de reglas",
                            context.userId(), attempt, e.toString(), e);
                    acumulados.add("Error inesperado en la generacion con IA (" + e.getClass().getSimpleName() + ")");
                    break;
                }
            }
        }
        problems = List.copyOf(acumulados);

        // Respaldo determinista. Se cuenta como intento propio para que
        // generation_attempts refleje el coste real de producir la semana.
        attempts++;
        tiempos.origen = "RESPALDO";
        long t2 = System.nanoTime();
        try {
            var draft = ruleGenerator.generate(context, problems);
            validator.validate(draft, context, durationToRepsDivisor);
            registrarCobertura(draft, context);
            return new Result(draft, attempts);
        } catch (InvalidPlanDraftException e) {
            tiempos.origen = "RESPALDO_ALTERNATIVO";
            // D-G3 (rediseno, solo con alternativasDeterministas): antes de dejar la semana sin plan, alternativas
            // DETERMINISTAS y acotadas. Mismo usuario y semana => misma semana publicada.
            if (context.policy() != null && context.policy().alternativasDeterministas()) {
                var alternativa = alternativaDeterminista(context, problems, durationToRepsDivisor);
                if (alternativa != null) {
                    registrarCobertura(alternativa, context);
                    return new Result(alternativa, attempts);
                }
            }
            // El motor de reglas fallando indica que el conjunto elegible o el
            // perfil hacen imposible cualquier plan valido. Es un dato, no un
            // error tecnico: la semana queda sin plan y su adherencia sera NULL.
            tiempos.origen = "SIN_PLAN";
            log.error("El motor de reglas tampoco produjo un plan valido para el usuario {}: {}",
                    context.userId(), e.problems());
            throw new PlanGenerationFailedException(context.userId(),
                    "Ultimos problemas: " + String.join(" | ", e.problems()));
        } finally {
            tiempos.respaldo(System.nanoTime() - t2);
        }
    }

    /**
     * RENDIMIENTO (12/11/2026) · DONDE SE VA EL TIEMPO DE UNA GENERACION, una linea por generacion:
     * {@code GENERACION_TIEMPOS}. Separa lo que espera a Replicate (ia_ms, por intento; el detalle de POST y sondeo
     * sigue en REPLICATE_TIMING) de lo que gasta FitSense (validacion y respaldo). Solo observa: no decide nada.
     */
    private static final class Tiempos {
        private final Long userId;
        private final short semana;
        private final long inicio = System.nanoTime();
        private final List<Long> iaMs = new ArrayList<>();
        private long validacionNs;
        private long respaldoNs;
        String origen = "SIN_PLAN";
        String motivo;

        Tiempos(PlanGenerationContext context) {
            this.userId = context.userId();
            this.semana = context.weekNumber();
        }

        void ia(long ns) { iaMs.add(ns / 1_000_000); }

        void validacion(long ns) { validacionNs += ns; }

        void respaldo(long ns) { respaldoNs += ns; }

        void registrar() {
            long total = (System.nanoTime() - inicio) / 1_000_000;
            long ia = iaMs.stream().mapToLong(Long::longValue).sum();
            log.info("GENERACION_TIEMPOS user={} semana={} origen={} motivo_respaldo={} intentos_ia={} ia_ms={}"
                            + " validacion_ms={} respaldo_ms={} fitsense_ms={} total_ms={}",
                    userId, semana, origen, motivo == null ? "-" : motivo, iaMs.size(), iaMs,
                    validacionNs / 1_000_000, respaldoNs / 1_000_000, Math.max(0, total - ia), total);
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

    /** D-G3: numero de semillas derivadas que se prueban (k = 1..N). */
    static final int VARIANTES_D_G3 = 4;

    /**
     * D-G3 · alternativa determinista acotada al borrador invalido del motor de reglas. Modo por omision SEMILLAS:
     * k = 1..{@link #VARIANTES_D_G3} semillas derivadas ({@code seedFor + k x 7919}); la primera valida. Modo
     * REPARACION (laboratorio, {@code -Dfitsense.lab.dg3-modo=REPARACION}): sustitucion de UNA colocacion de una
     * sesion que falla por un candidato elegible del mismo grupo y tipo de prescripcion, con la misma dosis, en orden
     * de id, con un maximo de validaciones. Nunca se publica algo que no pase la validacion completa.
     */
    private main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft alternativaDeterminista(
            PlanGenerationContext context, List<String> problems, int divisor) {
        return alternativaDeterminista(context, problems, divisor, System.getProperty("fitsense.lab.dg3-modo", "SEMILLAS"));
    }

    /** D-G3 con el modo explicito (SEMILLAS o REPARACION); publico solo para medirlo en el laboratorio. */
    public main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft alternativaDeterminista(
            PlanGenerationContext context, List<String> problems, int divisor, String modo) {
        if ("REPARACION".equals(modo))
            return reparacionLocal(context, problems, divisor);
        if (!(ruleGenerator instanceof main.web.services.fitsense.planning.infrastructure.generation.rules
                .RuleBasedTrainingPlanGenerator reglas)) return null;
        for (int k = 1; k <= VARIANTES_D_G3; k++) {
            try {
                var d = reglas.generarVariante(context, problems, k);
                validator.validate(d, context, divisor);
                log.info("D-G3 user={} semana={} variante_semilla={} VALIDA", context.userId(),
                        context.weekStartDate(), k);
                return d;
            } catch (InvalidPlanDraftException ignorado) {
                // siguiente variante
            }
        }
        return null;
    }

    static final int MAX_VALIDACIONES_REPARACION = 300;

    private main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft reparacionLocal(
            PlanGenerationContext context, List<String> problems, int divisor) {
        main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft base;
        List<String> fallos;
        try {
            base = ruleGenerator.generate(context, problems);
            validator.validate(base, context, divisor);
            return base;
        } catch (InvalidPlanDraftException e) {
            fallos = e.problems();
            base = ruleGenerator.generate(context, problems);
        }
        var porId = new java.util.HashMap<Long, main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise>();
        context.availableExercises().forEach(c -> porId.put(c.exerciseId(), c));
        var candidatos = context.availableExercises().stream()
                .sorted(java.util.Comparator.comparing(
                        main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise::exerciseId))
                .toList();
        int validaciones = 0;
        for (int i = 0; i < base.workouts().size(); i++) {
            var w = base.workouts().get(i);
            String fecha = w.scheduledDate().toString();
            if (fallos.stream().noneMatch(p -> p.contains(fecha))) continue;
            var usados = new java.util.HashSet<Long>();
            w.exercises().forEach(e -> usados.add(e.exerciseId()));
            for (int k = 0; k < w.exercises().size(); k++) {
                var sale = w.exercises().get(k);
                var cs = porId.get(sale.exerciseId());
                if (cs == null) continue;
                for (var c : candidatos) {
                    if (usados.contains(c.exerciseId()) || !java.util.Objects.equals(c.bodyPartCode(), cs.bodyPartCode())
                            || c.defaultPrescription() != cs.defaultPrescription()) continue;
                    if (++validaciones > MAX_VALIDACIONES_REPARACION) return null;
                    var ej = new java.util.ArrayList<>(w.exercises());
                    ej.set(k, new main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft.DraftExercise(
                            c.exerciseId(), sale.prescriptionType(), sale.plannedSets(), sale.plannedReps(),
                            sale.plannedDurationSeconds(), null, sale.restSeconds(), sale.notes()));
                    var ws = new java.util.ArrayList<>(base.workouts());
                    ws.set(i, new main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft.DraftWorkout(
                            w.scheduledDate(), w.focus(), w.name(), w.expectedDurationMinutes(), ej));
                    var d = new main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft(base.source(),
                            base.modelName(), base.planName(), base.declaredTotalVolume(), base.rationale(), ws);
                    try {
                        validator.validate(d, context, divisor);
                        log.info("D-G3 user={} semana={} reparacion_local VALIDA tras {} validaciones",
                                context.userId(), context.weekStartDate(), validaciones);
                        return d;
                    } catch (InvalidPlanDraftException ignorado) {
                        // siguiente
                    }
                }
            }
        }
        return null;
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