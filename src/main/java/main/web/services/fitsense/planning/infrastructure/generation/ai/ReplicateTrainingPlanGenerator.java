package main.web.services.fitsense.planning.infrastructure.generation.ai;

import main.web.services.fitsense.planning.domain.exceptions.AiProviderUnavailableException;
import main.web.services.fitsense.planning.domain.model.valueobjects.GenerationSource;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;
import main.web.services.fitsense.planning.domain.services.TrainingPlanGenerator;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.util.regex.Pattern;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ReplicateTrainingPlanGenerator implements TrainingPlanGenerator {

    private static final Logger log = LoggerFactory.getLogger(ReplicateTrainingPlanGenerator.class);

    private final ReplicateProperties properties;
    private final PlanPromptBuilder promptBuilder;
    private final PlanDraftFromJsonAssembler assembler;
    private final JsonSupport jsonSupport;
    private final RestClient restClient;
    /** R-3: el sondeo usa su propio cliente, con un limite de lectura corto. */
    private final RestClient pollClient;

    /** R-2: reloj y pausa del sondeo, sustituibles en las pruebas para no esperar 150 s de verdad. */
    @FunctionalInterface
    interface Durmiente {
        void dormir(Duration d) throws InterruptedException;
    }

    private Clock reloj = Clock.systemUTC();
    private Durmiente durmiente = d -> Thread.sleep(d.toMillis());

    /** Solo para pruebas: reloj y pausa controlados. */
    ReplicateTrainingPlanGenerator conTiempoDePrueba(Clock reloj, Durmiente durmiente) {
        this.reloj = reloj;
        this.durmiente = durmiente;
        return this;
    }

    /**
     * R-2: como termino la peticion de cancelar una prediccion abandonada. Se registra en una linea propia,
     * {@code REPLICATE_CANCEL}, para no cambiar las claves de {@code REPLICATE_TIMING}.
     */
    enum Cancelacion {
        /** Replicate confirmo el estado canceled. */
        CANCELADA,
        /** Replicate acepto la peticion y la prediccion aun no estaba en canceled (en transicion). */
        SOLICITADA,
        /** La prediccion ya habia terminado (succeeded o failed) cuando llego la cancelacion. */
        YA_TERMINADA,
        /** Replicate rechazo la cancelacion (4xx/5xx). */
        RECHAZADA,
        /** No hubo respuesta a la peticion de cancelar (red, limite de lectura). */
        ERROR_DE_RED,
        /** No hay identificador ni urls.cancel: no hay a quien pedirselo. */
        SIN_IDENTIFICADOR
    }

    /** Los identificadores de Replicate son alfanumericos: nada mas entra en una URL derivada. */
    private static final Pattern ID_VALIDO = Pattern.compile("[A-Za-z0-9]{1,64}");

    /**
     * ESTE {@code @Autowired} NO ES DECORATIVO. Al haber dos constructores,
     * Spring pierde el atajo de "si hay uno solo con argumentos, usalo" y el
     * contexto no arranca sin marcar cual es el bueno (INC-4C-01).
     */
    @org.springframework.beans.factory.annotation.Autowired
    public ReplicateTrainingPlanGenerator(ReplicateProperties properties,
                                          PlanPromptBuilder promptBuilder,
                                          PlanDraftFromJsonAssembler assembler,
                                          JsonSupport jsonSupport) {
        // R-3: limites HTTP explicitos (antes, el cliente del JDK sin limite de conexion ni de lectura).
        this(properties, promptBuilder, assembler, jsonSupport,
                ReplicateHttp.post(properties), ReplicateHttp.sondeo(properties));
        log.info("Replicate HTTP: conectar {} s, leer POST {} s (Prefer: wait {} s), leer sondeo {} s, sondeo {} s"
                        + " consultando cada {} ms; espera maxima por llamada ~{} s (~{} s si hay que cancelarla)",
                properties.connectTimeoutSeconds(), properties.readTimeoutSeconds(), properties.preferWaitSeconds(),
                properties.pollReadTimeoutSeconds(), properties.timeoutSeconds(), properties.pollIntervalMillis(),
                properties.esperaMaximaSegundos(), properties.esperaMaximaConCancelacionSegundos());
        // G5M: que modelo y que forma de peticion quedan activos (sin token ni URL con credenciales).
        if (properties.formato() == ReplicateProperties.Formato.DIRECT)
            log.info("Replicate: modelo {} en formato direct (reasoning_effort={}, verbosity={},"
                            + " max_completion_tokens={}); el esquema viaja en el system_prompt y la salida se valida",
                    properties.modeloEfectivo(), properties.reasoningEffort(), properties.verbosity(),
                    properties.maxCompletionTokens());
        else
            log.warn("Replicate: formato structured (modelo {} via gpt-5-structured). Ese endpoint respondio E002 para"
                    + " esta cuenta desde el 17/11/2026: solo debe usarse para comparar o revertir, a proposito.",
                    properties.modeloEfectivo());
    }

    /**
     * Costura para pruebas: con un {@code RestClient.Builder} se puede atar un
     * {@code MockRestServiceServer} y medir los tramos sin llamar a Replicate.
     * Es de paquete para no dejar dos constructores publicos.
     * <p>
     * El cliente se sigue construyendo aqui dentro, igual que antes: lo unico
     * que cambia es de donde sale el constructor.
     */
    ReplicateTrainingPlanGenerator(ReplicateProperties properties,
                                   PlanPromptBuilder promptBuilder,
                                   PlanDraftFromJsonAssembler assembler,
                                   JsonSupport jsonSupport,
                                   RestClient.Builder restClientBuilder) {
        this(properties, promptBuilder, assembler, jsonSupport, restClientBuilder, restClientBuilder);
    }

    /** Con un cliente para el POST y otro para el sondeo (R-3). */
    ReplicateTrainingPlanGenerator(ReplicateProperties properties,
                                   PlanPromptBuilder promptBuilder,
                                   PlanDraftFromJsonAssembler assembler,
                                   JsonSupport jsonSupport,
                                   RestClient.Builder postBuilder,
                                   RestClient.Builder pollBuilder) {
        this.properties = properties;
        this.promptBuilder = promptBuilder;
        this.assembler = assembler;
        this.jsonSupport = jsonSupport;
        this.restClient = postBuilder.build();
        this.pollClient = pollBuilder == postBuilder ? this.restClient : pollBuilder.build();
    }

    @Override
    public GenerationSource source() { return GenerationSource.AI; }

    @Override
    public String modelName() { return properties.modeloEfectivo(); }

    public boolean isEnabled() { return properties.isUsable(); }

    @Override
    public PlanDraft generate(PlanGenerationContext context, List<String> previousProblems) {
        return toDraft(requestOutput(context, AiRetryFeedback.ofProblems(previousProblems)));
    }

    /**
     * Pide una propuesta y devuelve el JSON tal cual lo entrego el modelo. Va
     * separado de {@link #toDraft(String)} para que el pipeline conserve la
     * propuesta aunque luego se rechace: el reintento la necesita para
     * corregirla (AiRetryFeedback).
     */
    public String requestOutput(PlanGenerationContext context, AiRetryFeedback feedback) {
        return requestOutput(context, feedback, 0);
    }

    /**
     * @param attempt intento dentro de ESTA generacion, para que la linea
     *                REPLICATE_TIMING se pueda atar a su plan. Un mismo plan
     *                puede provocar varios intentos y el id de prediccion solo
     *                no basta para distinguirlos.
     */
    public String requestOutput(PlanGenerationContext context, AiRetryFeedback feedback,
                                int attempt) {
        var snapshot = PlanInputSnapshot.of(context, promptBuilder.contratoV15());
        var snapshotJson = jsonSupport.write(snapshot);
        var prompt = promptBuilder.build(context, snapshotJson, feedback);
        return call(prompt, Peticion.deSemana(context.effectiveDaysPerWeek(), promptBuilder.version()),
                context.userId(), context.weekNumber(), attempt);
    }

    /** Convierte la salida del modelo en borrador. Lanza InvalidPlanDraftException si no se puede. */
    public PlanDraft toDraft(String output) {
        return assembler.toDraft(output, properties.modeloEfectivo());
    }

    /**
     * QUE SE LE PIDE AL PROVEEDOR, ademas del prompt: el esquema que tiene que
     * cumplir la respuesta y la etiqueta con que se registra la peticion.
     * <p>
     * Los dos campos van juntos a proposito. Son la unica diferencia entre una
     * generacion inicial y una adaptacion, y si viajaran por separado nada
     * impediria mandar el esquema de una con la etiqueta de la otra, que es
     * justo el error que hacia mentir a la traza.
     *
     * @param jsonSchema    lo que acepta Replicate en {@code json_schema}.
     * @param promptVersion para la linea "Salida de la IA". Identifica QUE se
     *                      pidio: esa linea es la evidencia de la tesis sobre
     *                      que propuso el modelo en cada intento, y con la
     *                      etiqueta equivocada no vale para comparar nada.
     */
    record Peticion(Map<String, Object> jsonSchema, String promptVersion) {

        /** Generacion inicial: una semana entera, con sus principios. */
        static Peticion deSemana(int workoutCount) {
            return deSemana(workoutCount, PlanPromptBuilder.VERSION);
        }

        /** H2: con la version efectiva del texto de reglas (PR-1.7 o PR-1.9). */
        static Peticion deSemana(int workoutCount, String versionReglas) {
            return new Peticion(PlanJsonSchema.format(workoutCount),
                    versionReglas + " " + PrescriptionPrinciples.VERSION);
        }

        /** Adaptacion localizada: correcciones de sesiones concretas. */
        static Peticion deCorrecciones(int sessionCount) {
            return deCorrecciones(sessionCount, SessionFixPromptBuilder.VERSION);
        }

        /** Con la version de prompt que de verdad se envia (V1 o V2 experimental). */
        static Peticion deCorrecciones(int sessionCount, String fixPromptVersion) {
            return new Peticion(SessionFixJsonSchema.format(sessionCount),
                    fixPromptVersion + " " + SessionFixJsonSchema.VERSION);
        }
    }

    /**
     * Envoltura de medicion. Es fina a proposito: lo unico que hace es abrir el
     * cronometro monotonico y garantizar que la linea REPLICATE_TIMING se
     * escriba por TODAS las salidas, incluidas las que lanzan excepcion.
     * <p>
     * Visibilidad de paquete para que la prueba de instrumentacion pueda medir
     * los tramos sin montar un {@code PlanGenerationContext} completo. No la
     * llama nadie mas: el pipeline entra por {@link #requestOutput}.
     */
    String call(String prompt, int workoutCount,
                Long userId, short weekNumber, int attempt) {
        return call(prompt, Peticion.deSemana(workoutCount), userId, weekNumber, attempt);
    }

    /**
     * La misma llamada medida, con la peticion ya armada.
     * <p>
     * Existe porque la ADAPTACION manda otro esquema y otra etiqueta. Lo que no
     * cambia -el POST, el sondeo de predicciones en curso, la linea
     * REPLICATE_TIMING- se comparte, que es la parte que no conviene duplicar.
     */
    String call(String prompt, Peticion peticion,
                Long userId, short weekNumber, int attempt) {
        // RENDIMIENTO (12/11/2026): el plazo del presupuesto total de la generacion, si lo hay
        // (PlazoDeGeneracion). Acota el Prefer: wait del POST y el sondeo; si se agota con la prediccion en curso,
        // se cancela como cualquier otro limite (R-2). Sin plazo (la adaptacion no lo fija), todo sigue como antes.
        return call(prompt, peticion, userId, weekNumber, attempt, PlazoDeGeneracion.actual());
    }

    /** La misma, con el plazo del presupuesto total (null = sin plazo). */
    String call(String prompt, Peticion peticion,
                Long userId, short weekNumber, int attempt, java.time.Instant plazo) {
        var medicion = new Medicion();
        try {
            // V59: dentro de una operacion asincrona, cada prediccion pasa por el libro (reanudar sin pedir otra).
            var ambito = DiarioDePredicciones.actual();
            if (diario != null && ambito.isPresent())
                return callConDiario(prompt, peticion, medicion, plazo, ambito.get(), attempt);
            return callMedido(prompt, peticion, medicion, plazo);
        } finally {
            log.info("{}", medicion.timing(userId, weekNumber, attempt).describe());
            // G5M: modelo, formato, tokens y coste de la misma llamada (sin prompt, sin token, sin datos personales).
            log.info("{}", medicion.consumo(userId, weekNumber, attempt, properties, tarifaEntradaUsdMillon,
                    tarifaSalidaUsdMillon));
        }
    }

    // =====================================================================================================
    // V59 · LIBRO DE PREDICCIONES (solo dentro de DiarioDePredicciones.durante: la generacion asincrona)
    // =====================================================================================================

    private DiarioDePredicciones diario;

    /*
     * G5M: tarifa de Replicate para estimar el coste (USD por millon de tokens; gpt-5-mini: 0,25 entrada y 2,00
     * salida, la misma que usa el control de presupuesto de la evaluacion K20). Solo alimenta la linea
     * REPLICATE_CONSUMO: no limita ni decide nada.
     */
    private double tarifaEntradaUsdMillon = 0.25;
    private double tarifaSalidaUsdMillon = 2.00;

    @org.springframework.beans.factory.annotation.Value("${fitsense.ai.cost.input-usd-per-million:0.25}")
    void setTarifaEntradaUsdMillon(double usd) { this.tarifaEntradaUsdMillon = usd; }

    @org.springframework.beans.factory.annotation.Value("${fitsense.ai.cost.output-usd-per-million:2.00}")
    void setTarifaSalidaUsdMillon(double usd) { this.tarifaSalidaUsdMillon = usd; }
    private Duration ventanaReutilizacion = Duration.ofHours(24);

    /** Opcional: sin libro (pruebas unitarias del cliente), todo sigue como antes. */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setDiario(DiarioDePredicciones diario) {
        this.diario = diario;
    }

    @org.springframework.beans.factory.annotation.Value("${fitsense.plan-generation.reuse-window-hours:24}")
    void setVentanaReutilizacionHoras(long horas) {
        this.ventanaReutilizacion = Duration.ofHours(Math.max(0, horas));
    }

    /** Huella estable entre arranques: claves ordenadas (Map.of no garantiza el orden entre JVM). */
    private static final com.fasterxml.jackson.databind.ObjectMapper HUELLA = new com.fasterxml.jackson.databind.ObjectMapper()
            .configure(com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    static String huella(Map<String, Object> input) {
        try {
            var bytes = HUELLA.writeValueAsBytes(new java.util.TreeMap<>(input));
            var digest = java.security.MessageDigest.getInstance("SHA-256").digest(bytes);
            return java.util.HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo calcular la huella de la peticion", e);
        }
    }

    /** Para pruebas: la huella de la peticion de una semana de {@code workoutCount} sesiones con este prompt. */
    String huellaDe(String prompt, int workoutCount) {
        return huella(entrada(prompt, Peticion.deSemana(workoutCount)));
    }

    /**
     * Lo que se envia como {@code input}, segun el formato (G5M, 23/11/2026).
     * <ul>
     *   <li><b>direct</b> (openai/gpt-5-mini): {@code system_prompt} con el esquema, {@code prompt},
     *   {@code reasoning_effort}, {@code verbosity} y {@code max_completion_tokens}. Ni {@code model} ni
     *   {@code json_schema}: ese modelo no los admite (su esquema de entrada en Replicate: prompt, system_prompt,
     *   messages, image_input, reasoning_effort, verbosity, max_completion_tokens). El esquema deja de ser una
     *   restriccion del proveedor y pasa a ser una instruccion: por eso la salida se sigue validando entera
     *   (ensamblador + V1-V27/V8C) y un texto que no sea el JSON pedido es un intento rechazado, no un plan.</li>
     *   <li><b>structured</b> (openai/gpt-5-structured): la peticion de siempre, sin cambios.</li>
     * </ul>
     * No se usa {@code messages}: segun Replicate, si se envia se ignoran prompt y system_prompt, y no aporta nada a
     * una peticion de un solo turno.
     */
    private Map<String, Object> entrada(String prompt, Peticion peticion) {
        var input = new LinkedHashMap<String, Object>();
        if (properties.formato() == ReplicateProperties.Formato.STRUCTURED) {
            input.put("model", properties.model());
            input.put("prompt", prompt);
            input.put("reasoning_effort", properties.reasoningEffort());
            input.put("json_schema", peticion.jsonSchema());
            return input;
        }
        input.put("system_prompt", instruccionDeSistema(peticion));
        input.put("prompt", prompt);
        input.put("reasoning_effort", properties.reasoningEffort());
        input.put("verbosity", properties.verbosity());
        input.put("max_completion_tokens", properties.maxCompletionTokens());
        return input;
    }

    /** Version de la instruccion de sistema del formato direct, para la traza. */
    static final String VERSION_INSTRUCCION_DIRECTA = "SYS-JSON-1.0";

    /**
     * La instruccion de sistema del formato direct: responder SOLO con el JSON del esquema de la peticion. El esquema
     * es el MISMO objeto que structured enviaba en json_schema (PlanJsonSchema / SessionFixJsonSchema), serializado
     * con las claves ordenadas para que la peticion -y su huella del libro V59- sea identica entre arranques.
     */
    static String instruccionDeSistema(Peticion peticion) {
        @SuppressWarnings("unchecked")
        var format = (Map<String, Object>) peticion.jsonSchema().get("format");
        String esquema;
        try {
            esquema = HUELLA.writeValueAsString(format.get("schema"));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo serializar el esquema de la peticion", e);
        }
        return ("Eres el componente de IA de FitSense. Responde UNICAMENTE con un objeto JSON valido: sin texto antes"
                + " ni despues, sin bloques de codigo (```), sin comentarios y sin explicaciones. El objeto debe cumplir"
                + " exactamente el JSON Schema \"%s\" que sigue: incluye todas las propiedades de cada \"required\","
                + " ninguna propiedad adicional, los tipos indicados (los enteros sin comillas ni decimales) y solo los"
                + " valores de cada \"enum\". Las \"description\" del esquema son instrucciones obligatorias."
                + " Las reglas de las instrucciones del usuario prevalecen y no se relajan para poder responder."
                + "\n\nJSON Schema:\n%s").formatted(format.get("name"), esquema);
    }

    private String callConDiario(String prompt, Peticion peticion, Medicion medicion, java.time.Instant plazo,
                                 DiarioDePredicciones.Ambito ambito, int attempt) {
        // Fencing ANTES de todo: un worker que ya no es el propietario de la operacion no envia, no consulta y no toca
        // el libro (no puede, por ejemplo, dar por NO_CONFIRMADA la prediccion que el propietario esta enviando).
        ambito.antesDeEnviar().run();
        var input = entrada(prompt, peticion);
        medicion.entrada(input);
        String hash = huella(input);
        var propia = diario.deIntento(ambito.operationId(), attempt);
        DiarioDePredicciones.Entrada previa = null;
        if (propia.isPresent() && propia.get().requestHash().equals(hash)) {
            previa = propia.get();
        } else if (propia.isEmpty()) {
            var otra = diario.reutilizable(ambito.userId(), hash, ambito.operationId(),
                    reloj.instant().minus(ventanaReutilizacion));
            if (otra.isPresent()) {
                previa = diario.adoptar(otra.get(), ambito.operationId(), attempt, reloj.instant());
                log.info("REPLICATE_ADOPTADA operacion={} intento={} prediction_id={} de_operacion={} estado={}",
                        ambito.operationId(), attempt, otra.get().predictionId(), otra.get().operationId(),
                        otra.get().estado());
            }
        }
        if (previa != null) return reanudar(previa, peticion, prompt, medicion, plazo, ambito);

        // Nueva: se anota ANTES de enviar. Si el proceso muere entre el envio y la confirmacion, la fila queda en
        // SOLICITANDO y la reanudacion NO reenvia (Replicate pudo aceptarla).
        long fila;
        try {
            fila = diario.solicitar(ambito.operationId(), ambito.userId(), attempt, hash, reloj.instant());
        } catch (org.springframework.dao.DuplicateKeyException carrera) {
            // Otra ejecucion de esta operacion anoto este intento entre la consulta y la anotacion: se sigue la suya.
            var suya = diario.deIntento(ambito.operationId(), attempt).orElseThrow(() -> carrera);
            log.warn("REPLICATE_CARRERA operacion={} intento={}: otra ejecucion ya lo anoto; se reanuda la suya",
                    ambito.operationId(), attempt);
            return reanudar(suya, peticion, prompt, medicion, plazo, ambito);
        }
        String rawBody;
        try {
            rawBody = postSinEspera(input);
        } catch (EnvioNoAceptado e) {
            medicion.postFallo();
            diario.terminada(fila, DiarioDePredicciones.Estado.ERROR_ENVIO, null, e.getMessage(), reloj.instant());
            throw new AiProviderUnavailableException(e.getMessage());
        } catch (EnvioIncierto e) {
            medicion.postFallo();
            return noConfirmada(fila, ambito, attempt, e.getMessage());
        }
        medicion.postTerminado();

        ReplicateResponse response;
        try {
            response = jsonSupport.read(rawBody, ReplicateResponse.class);
        } catch (RuntimeException e) {
            response = null;
        }
        if (response == null || response.id() == null || !ID_VALIDO.matcher(response.id()).matches()) {
            // Un 2xx sin identificador legible: Replicate pudo crearla. Mismo trato conservador.
            medicion.resultado(ReplicateTiming.Resultado.INVALID_RESPONSE);
            return noConfirmada(fila, ambito, attempt, "respuesta 2xx sin identificador de prediccion legible");
        }
        diario.enviada(fila, response.id(), reloj.instant());
        medicion.predictionId(response.id());
        return seguir(fila, response, peticion, prompt, medicion, plazo);
    }

    private String noConfirmada(long fila, DiarioDePredicciones.Ambito ambito, int attempt, String motivo) {
        diario.terminada(fila, DiarioDePredicciones.Estado.NO_CONFIRMADA, null, motivo, reloj.instant());
        log.warn("REPLICATE_NO_CONFIRMADA operacion={} intento={} motivo={}: Replicate pudo aceptar la prediccion sin"
                + " que se guardara su id; NO se reenvia (posible prediccion facturada sin aprovechar)",
                ambito.operationId(), attempt, motivo);
        throw new AiProviderUnavailableException("PREDICCION_NO_CONFIRMADA: " + motivo);
    }

    /** Reanuda un intento ya anotado sin crear otra prediccion. */
    private String reanudar(DiarioDePredicciones.Entrada e, Peticion peticion, String prompt, Medicion medicion,
                            java.time.Instant plazo, DiarioDePredicciones.Ambito ambito) {
        medicion.predictionId(e.predictionId());
        medicion.reanudada();
        switch (e.estado()) {
            case SUCCEEDED -> {
                medicion.postTerminado();
                medicion.resultado(ReplicateTiming.Resultado.SUCCEEDED);
                log.info("REPLICATE_REUTILIZADA operacion={} intento={} prediction_id={}: salida ya recibida, sin"
                        + " llamar a Replicate", ambito.operationId(), e.attempt(), e.predictionId());
                return e.output();
            }
            case ENVIADA -> {
                medicion.postTerminado();
                log.info("REPLICATE_REANUDADA operacion={} intento={} prediction_id={}: se sigue la misma prediccion",
                        ambito.operationId(), e.attempt(), e.predictionId());
                var url = urlDeConsulta(e.predictionId());
                var enCurso = new ReplicateResponse(e.predictionId(), "processing", null, null, null,
                        new ReplicateResponse.Urls(url, null, null));
                return seguir(e.id(), enCurso, peticion, prompt, medicion, plazo);
            }
            case SOLICITANDO -> {
                return noConfirmada(e.id(), ambito, e.attempt(), "envio sin confirmar al reanudar");
            }
            default -> {
                log.info("REPLICATE_REPETIDA operacion={} intento={} estado={}: se repite el desenlace registrado sin"
                        + " otra prediccion", ambito.operationId(), e.attempt(), e.estado());
                throw new AiProviderUnavailableException("desenlace registrado de la prediccion: " + e.estado()
                        + (e.error() == null ? "" : " (" + e.error() + ")"));
            }
        }
    }

    /** Sondeo, cancelacion y registro del final: lo mismo que callMedido, anotado en el libro. */
    private String seguir(long fila, ReplicateResponse response, Peticion peticion, String prompt,
                          Medicion medicion, java.time.Instant plazo) {
        var desenlace = Sondeo.Desenlace.RESPONDIO;
        if (response.pending()) {
            final var enCurso = response;
            var sondeo = medicion.sondeando(() -> awaitCompletion(enCurso, plazo));
            desenlace = sondeo.desenlace();
            diario.consultada(fila, sondeo.requests(), reloj.instant());
            if (sondeo.response() != null) response = sondeo.response();
        }
        if (response.pending() && (desenlace == Sondeo.Desenlace.INTERRUMPIDO || Thread.currentThread().isInterrupted())) {
            // Apagado (SIGTERM): la prediccion sigue viva en Replicate y su id ya esta en el libro. No se cancela ni se
            // cierra la fila (queda ENVIADA): la instancia siguiente la reanuda por su id sin pagar otra.
            medicion.resultado(ReplicateTiming.Resultado.INTERRUPTED);
            log.warn("REPLICATE_INTERRUMPIDA prediction_id={}: el proceso se esta apagando; se deja ENVIADA para"
                    + " reanudarla por su id (no se cancela)", response.id());
            throw new GeneracionInterrumpidaException("apagado con la prediccion " + response.id() + " en curso");
        }
        if (response.pending()) {
            var trasCancelar = cancelar(response, desenlace);
            if (trasCancelar != null && trasCancelar.succeeded()) {
                response = trasCancelar;
                desenlace = Sondeo.Desenlace.RESPONDIO;
            } else if (trasCancelar != null && "canceled".equals(trasCancelar.status())) {
                response = trasCancelar;
            }
        }
        medicion.metricas(response.metrics());
        if (!response.succeeded()) {
            String detail = response.status() + " " + response.error();
            var estado = "canceled".equals(response.status()) ? DiarioDePredicciones.Estado.CANCELED
                    : response.pending() ? DiarioDePredicciones.Estado.TIMEOUT : DiarioDePredicciones.Estado.FAILED;
            diario.terminada(fila, estado, null, detail, reloj.instant());
            medicion.resultado(clasificar(response, desenlace));
            log.warn("Replicate devolvio un estado no exitoso: {}", detail);
            throw new AiProviderUnavailableException(detail);
        }
        String output = response.joinedOutput();
        medicion.salida(output.length());
        diario.terminada(fila, DiarioDePredicciones.Estado.SUCCEEDED, output, null, reloj.instant());
        medicion.resultado(ReplicateTiming.Resultado.SUCCEEDED);
        log.info("Salida de la IA (prediccion {}, prompt {}, {} caracteres de prompt): {}",
                response.id(), peticion.promptVersion(), prompt.length(), output);
        return output;
    }

    /** GET /v1/predictions/{id} en el mismo servidor que la base. Solo ids alfanumericos. */
    String urlDeConsulta(String id) {
        if (id == null || !ID_VALIDO.matcher(id).matches()) return null;
        var base = URI.create(properties.baseUrl());
        return base.getScheme() + "://" + base.getRawAuthority() + "/v1/predictions/" + id;
    }

    /** El envio NO llego a Replicate o Replicate lo rechazo: no hay prediccion. */
    static final class EnvioNoAceptado extends RuntimeException {
        EnvioNoAceptado(String m) { super(m); }
    }

    /** El envio pudo llegar y aceptarse sin que conozcamos su id (corte de lectura, 5xx, conexion reiniciada). */
    static final class EnvioIncierto extends RuntimeException {
        EnvioIncierto(String m) { super(m); }
    }

    /**
     * POST SIN {@code Prefer: wait}: Replicate devuelve el identificador en cuanto crea la prediccion, y se guarda
     * antes de esperar. Asi la ventana sin identificador es la de una peticion corta, no los 60 s de la espera.
     */
    private String postSinEspera(Map<String, Object> input) {
        try {
            return restClient.post()
                    .uri(properties.baseUrl())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("input", input))
                    .retrieve()
                    .body(String.class);
        } catch (org.springframework.web.client.RestClientResponseException e) {
            int codigo = e.getStatusCode().value();
            if (codigo >= 400 && codigo < 500)
                throw new EnvioNoAceptado("Replicate rechazo la peticion (http " + codigo + ")");
            throw new EnvioIncierto("Replicate respondio http " + codigo);
        } catch (org.springframework.web.client.ResourceAccessException e) {
            if (noLlegoAEnviarse(e)) throw new EnvioNoAceptado("no se pudo conectar con Replicate: " + e.getMessage());
            throw new EnvioIncierto("se corto la respuesta de Replicate: " + e.getMessage());
        } catch (RuntimeException e) {
            throw new EnvioIncierto(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /** Solo un fallo de CONEXION (antes de enviar el cuerpo) garantiza que no hay prediccion. */
    static boolean noLlegoAEnviarse(Throwable e) {
        for (var c = e; c != null; c = c.getCause())
            if (c instanceof java.net.ConnectException || c instanceof java.net.UnknownHostException
                    || c instanceof java.net.http.HttpConnectTimeoutException
                    || c instanceof java.net.NoRouteToHostException)
                return true;
        return false;
    }

    private String callMedido(String prompt, Peticion peticion, Medicion medicion, java.time.Instant plazo) {
        // G5M: la misma entrada que el libro V59 (antes, una copia a mano que habria podido divergir del formato).
        var input = entrada(prompt, peticion);
        medicion.entrada(input);

        String rawBody;
        try {
            rawBody = post(input, esperaDelPost(plazo));
        } catch (RuntimeException e) {
            // ANTES SALIA post_ms=0 SIEMPRE. Un POST que revienta a los 50 s y
            // uno que revienta al instante se registraban igual, y por tanto ese
            // campo no media nada en el caso en que mas falta hace. La excepcion
            // se relanza TAL CUAL: el pipeline sigue viendo exactamente lo mismo.
            medicion.postFallo();
            throw e;
        }
        medicion.postTerminado();

        // Respuesta inicial de Replicate: repite el prompt entero (unos 160 KB con
        // el catalogo) y casi siempre llega con status "starting" y output null,
        // asi que no muestra el plan. Queda en DEBUG; la salida real del modelo
        // se registra en INFO mas abajo, cuando la prediccion termina.
        log.debug("Replicate respondio (crudo): {}", rawBody);

        // R-1 (resiliencia de Replicate, 11/11/2026): el SOBRE de Replicate se leia fuera de todo manejo de errores.
        // Un 2xx que no era su JSON (pagina de un proxy, cuerpo vacio, JSON cortado) lanzaba IllegalStateException,
        // que nadie capturaba, y la semana se quedaba sin plan aunque el motor de reglas pudiera hacerla. Es un fallo
        // del PROVEEDOR -el modelo no llego a proponer nada-, no del modelo: va por el mismo camino que un 5xx. El
        // cuerpo crudo no se copia al mensaje ni al log (repite el prompt entero).
        ReplicateResponse response;
        try {
            response = jsonSupport.read(rawBody, ReplicateResponse.class);
        } catch (RuntimeException e) {
            medicion.resultado(ReplicateTiming.Resultado.INVALID_RESPONSE);
            log.warn("Replicate devolvio un cuerpo que no es una respuesta valida ({} caracteres)",
                    rawBody == null ? 0 : rawBody.length());
            throw new AiProviderUnavailableException("la respuesta de Replicate no se pudo interpretar");
        }
        medicion.predictionId(response == null ? null : response.id());

        // Prefer: wait tiene un techo duro de 60 s del lado de Replicate. Con el
        // catalogo completo el modelo tarda mas, y la respuesta llegaba con
        // estado "starting": no era un fallo, era una prediccion aun en curso
        // que se contabilizaba como intento perdido.
        var desenlace = Sondeo.Desenlace.RESPONDIO;
        if (response != null && response.pending()) {
            // Copia final: la lambda no puede capturar una variable que se reasigna.
            final var enCurso = response;
            var sondeo = medicion.sondeando(() -> awaitCompletion(enCurso, plazo));
            desenlace = sondeo.desenlace();
            response = sondeo.response();
            medicion.predictionId(response == null ? null : response.id());
        }

        // R-2 (resiliencia de Replicate, 11/11/2026): una prediccion que SEGUIA EN CURSO cuando dejamos de esperar
        // (limite agotado, sondeo fallido o sin urls.get, hilo interrumpido) seguia ejecutandose -y pudiendo
        // facturarse- en Replicate sin que nadie la leyera. Ahora se pide su cancelacion, una vez. Cancelar no
        // garantiza coste cero: lo que ya proceso puede cobrarse. Si al cancelar resulta que YA HABIA TERMINADO con
        // exito, se usa esa salida: no se ha publicado nada todavia, asi que no es una respuesta tardia.
        if (response != null && response.pending()) {
            var trasCancelar = cancelar(response, desenlace);
            if (trasCancelar != null && trasCancelar.succeeded()) {
                response = trasCancelar;
                desenlace = Sondeo.Desenlace.RESPONDIO;
            }
        }

        if (response != null) medicion.metricas(response.metrics());
        if (response == null || !response.succeeded()) {
            String detail = response == null
                    ? "respuesta vacia"
                    : response.status() + " " + response.error();
            // Separar estos casos es justo lo que permite decidir si conviene
            // ampliar el limite: solo TIMEOUT dice algo sobre el tiempo de espera.
            medicion.resultado(clasificar(response, desenlace));
            log.warn("Replicate devolvio un estado no exitoso: {}", detail);

            // Excepcion de proveedor, no de plan invalido: el modelo no llego a
            // proponer nada. El cortacircuitos solo cuenta estas, y en el
            // analisis hay que poder separar "el modelo no supo" de "el
            // proveedor estaba caido".
            throw new AiProviderUnavailableException(detail);
        }

        medicion.resultado(ReplicateTiming.Resultado.SUCCEEDED);
        String output = response.joinedOutput();
        medicion.salida(output.length());

        // Evidencia de cada intento, tambien de los rechazados: la proporcion de
        // planes validos al primer intento es un resultado de la tesis y esta
        // linea es lo que permite revisar QUE propuso el modelo cada vez.
        // La etiqueta sale de la peticion, no de PlanPromptBuilder. Estaba fija
        // y una adaptacion habria quedado registrada como si fuera generacion
        // inicial. La linea rendida no cambia para la generacion inicial:
        // Peticion.deSemana une las dos versiones con un espacio, igual que los
        // dos marcadores de antes.
        log.info("Salida de la IA (prediccion {}, prompt {}, {} caracteres de prompt): {}",
                response.id(), peticion.promptVersion(), prompt.length(), output);
        return output;
    }

    /**
     * Que resultado registrar cuando la llamada no termino en exito.
     * <p>
     * La prediccion en estado "pending" NO implica timeout: puede ser que
     * dejaramos de esperar, que no pudieramos preguntar, o que nos estuvieran
     * apagando. Hasta el 13/10/2026 los tres se escribian como TIMEOUT.
     */
    private static ReplicateTiming.Resultado clasificar(ReplicateResponse response,
                                                        Sondeo.Desenlace desenlace) {
        if (response == null || !response.pending())
            return ReplicateTiming.Resultado.NOT_SUCCEEDED;
        return switch (desenlace) {
            case FALLO_LA_CONSULTA -> ReplicateTiming.Resultado.POLL_ERROR;
            case INTERRUMPIDO -> ReplicateTiming.Resultado.INTERRUPTED;
            case LIMITE_AGOTADO, RESPONDIO -> ReplicateTiming.Resultado.TIMEOUT;
        };
    }

    /**
     * El cronometro de una llamada. Mutable y de un solo uso, vive dentro de
     * {@link #call}.
     * <p>
     * TIEMPO MONOTONICO: {@code System.nanoTime()}, no el reloj de pared. Un
     * ajuste de hora del sistema no debe producir una duracion negativa ni
     * inflada en una medicion que se va a publicar.
     * <p>
     * NO GUARDA PROMPT, NI TOKEN, NI DATO DEL PARTICIPANTE.
     */
    private static final class Medicion {

        private final long inicio = System.nanoTime();
        private long postMs;
        private long pollingMs;
        private int pollingRequests;
        private String predictionId;
        // G5M: consumo. Solo longitudes y contadores: nunca el texto del prompt ni de la salida.
        private long caracteresEntrada = -1;
        private long bytesEntrada = -1;
        private long caracteresSalida = -1;
        private boolean reanudada;
        private ReplicateResponse.Metrics metricas;

        /**
         * Por omision POST_ERROR: si el POST lanza, nunca se llega a cambiarlo y
         * esa es exactamente la verdad. Deja de serlo en cuanto el POST vuelve.
         */
        private ReplicateTiming.Resultado resultado = ReplicateTiming.Resultado.POST_ERROR;

        void postTerminado() {
            postMs = ReplicateTiming.msDesde(inicio, System.nanoTime());
            // Ya respondio: lo que falle a partir de aqui no es un fallo del POST.
            resultado = ReplicateTiming.Resultado.NOT_SUCCEEDED;
        }

        /** El POST lanzo. Se mide igual: cuanto tardo en fallar tambien es dato. */
        void postFallo() {
            postMs = ReplicateTiming.msDesde(inicio, System.nanoTime());
        }

        /** Mide el tramo de sondeo y se queda con cuantas consultas costo. */
        Sondeo sondeando(java.util.function.Supplier<Sondeo> sondeo) {
            long inicioSondeo = System.nanoTime();
            try {
                var hecho = sondeo.get();
                pollingRequests = hecho.requests();
                return hecho;
            } finally {
                pollingMs = ReplicateTiming.msDesde(inicioSondeo, System.nanoTime());
            }
        }

        /** Solo se queda con el id si lo hay: nunca sobreescribe uno bueno con null. */
        void predictionId(String id) {
            if (id != null && !id.isBlank()) this.predictionId = id;
        }

        void resultado(ReplicateTiming.Resultado r) { this.resultado = r; }

        /** Caracteres (y bytes UTF-8) de texto enviados al modelo (prompt y, en direct, system_prompt). */
        void entrada(Map<String, Object> input) {
            long n = 0, b = 0;
            for (var clave : List.of("prompt", "system_prompt"))
                if (input.get(clave) instanceof String t) {
                    n += t.length();
                    b += t.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
                }
            caracteresEntrada = n;
            bytesEntrada = b;
        }

        /** Margen de la cota por los tokens de formato del chat (roles, separadores), muy por encima de lo real. */
        static final int MARGEN_TOKENS_FORMATO = 256;

        void salida(int caracteres) { caracteresSalida = caracteres; }

        /** El intento ya estaba en el libro V59: no se creo una prediccion nueva en esta llamada. */
        void reanudada() { reanudada = true; }

        void metricas(ReplicateResponse.Metrics m) { if (m != null) metricas = m; }

        /**
         * Linea REPLICATE_CONSUMO. Coste:
         * <ul>
         *   <li>{@code coste_usd}: con los tokens que informa Replicate; "no_reportado" si no los informa (gpt-5-mini
         *   directo solo informa predict_time).</li>
         *   <li>{@code coste_cota_usd}: cota SUPERIOR sin depender de Replicate, solo en direct (hay tope de salida):
         *   un token por BYTE UTF-8 de entrada (un tokenizador BPE por bytes no produce mas) mas un margen de formato,
         *   y max_completion_tokens de salida (razonamiento incluido).
         *   "sin_cota" en structured, que no fija tope de salida.</li>
         * </ul>
         * Una prediccion reanudada del libro no se vuelve a cobrar: su coste ya se registro en la llamada que la creo.
         */
        String consumo(Long userId, short weekNumber, int attempt, ReplicateProperties p,
                       double tarifaEntrada, double tarifaSalida) {
            var tokensEntrada = metricas == null ? null : metricas.tokenInputCount();
            var tokensSalida = metricas == null ? null : metricas.tokenOutputCount();
            String coste = tokensEntrada == null || tokensSalida == null ? "no_reportado"
                    : usd(tokensEntrada / 1e6 * tarifaEntrada + tokensSalida / 1e6 * tarifaSalida);
            boolean directo = p.formato() == ReplicateProperties.Formato.DIRECT;
            String cota = !directo ? "sin_cota" : caracteresEntrada < 0 ? "-"
                    : usd((bytesEntrada + MARGEN_TOKENS_FORMATO) / 1e6 * tarifaEntrada
                            + p.maxCompletionTokens() / 1e6 * tarifaSalida);
            return ("REPLICATE_CONSUMO user=%s week=%d attempt=%d modelo=%s formato=%s prediction_id=%s resultado=%s"
                    + " origen=%s predict_time_s=%s tokens_entrada=%s tokens_salida=%s coste_usd=%s coste_cota_usd=%s"
                    + " caracteres_entrada=%d caracteres_salida=%d reasoning_effort=%s verbosity=%s"
                    + " max_completion_tokens=%s").formatted(
                    userId == null ? "-" : userId, weekNumber, attempt, p.modeloEfectivo(), p.formato().etiqueta(),
                    predictionId == null ? "desconocido" : predictionId, resultado,
                    reanudada ? "reanudada" : "nueva",
                    metricas == null || metricas.predictTime() == null ? "no_reportado"
                            : String.format(java.util.Locale.ROOT, "%.3f", metricas.predictTime()),
                    tokensEntrada == null ? "no_reportado" : tokensEntrada,
                    tokensSalida == null ? "no_reportado" : tokensSalida,
                    reanudada && !"no_reportado".equals(coste) ? "0_reanudada" : coste,
                    reanudada ? "0_reanudada" : cota,
                    caracteresEntrada, caracteresSalida, p.reasoningEffort(),
                    directo ? p.verbosity() : "-", directo ? p.maxCompletionTokens() : "-");
        }

        private static String usd(double v) { return String.format(java.util.Locale.ROOT, "%.6f", v); }

        ReplicateTiming timing(Long userId, short weekNumber, int attempt) {
            return new ReplicateTiming(userId, weekNumber, attempt, predictionId,
                    postMs, pollingMs, pollingRequests, resultado);
        }
    }

    /** Lo que devuelve el sondeo: la respuesta, cuanto costo y COMO termino. */
    record Sondeo(ReplicateResponse response, int requests, Desenlace desenlace) {

        /**
         * Como termino el sondeo. Existe para que "dejamos de esperar" no se
         * confunda con "no pudimos preguntar": los dos dejan la prediccion en
         * pending y hasta ahora se registraban igual.
         */
        enum Desenlace {
            /** Llego un estado final: succeeded, failed, canceled. */
            RESPONDIO,
            /** Se agoto fitsense.ai.timeout-seconds con la prediccion viva. */
            LIMITE_AGOTADO,
            /** Un GET de sondeo fallo, o no habia urls.get al que preguntar. */
            FALLO_LA_CONSULTA,
            /** Se interrumpio el hilo: la aplicacion se esta parando. */
            INTERRUMPIDO
        }
    }

    /**
     * Consulta la prediccion hasta que termina. El limite lo marca
     * fitsense.ai.timeout-seconds, que ya NO esta atado a los 60 s de la
     * cabecera Prefer.
     */
    private Sondeo awaitCompletion(ReplicateResponse pending, java.time.Instant plazo) {
        var pollUrl = pending.pollUrl();
        if (pollUrl == null) {
            log.warn("La prediccion sigue en curso y no trae urls.get: no se puede consultar");
            return new Sondeo(pending, 0, Sondeo.Desenlace.FALLO_LA_CONSULTA);
        }

        var limite = reloj.instant().plusSeconds(properties.timeoutSeconds());
        // RENDIMIENTO: el presupuesto total de la generacion puede acabar antes que el limite propio del sondeo.
        boolean porPresupuesto = plazo != null && plazo.isBefore(limite);
        if (porPresupuesto) limite = plazo;
        var pausa = Duration.ofMillis(properties.pollIntervalMillis());
        var actual = pending;
        int consultas = 0;

        while (actual != null && actual.pending() && reloj.instant().isBefore(limite)) {
            try {
                durmiente.dormir(pausa);
            } catch (InterruptedException e) {
                // Restaurar la marca: alguien esta parando la aplicacion y no
                // queremos dejar el hilo en un estado inconsistente.
                Thread.currentThread().interrupt();
                return new Sondeo(actual, consultas, Sondeo.Desenlace.INTERRUMPIDO);
            }

            try {
                var raw = pollClient.get()
                        .uri(pollUrl)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiToken())
                        .retrieve()
                        .body(String.class);
                consultas++;
                actual = jsonSupport.read(raw, ReplicateResponse.class);
            } catch (RuntimeException e) {
                consultas++;
                // NO es un timeout: puede haber pasado a los tres segundos.
                log.warn("Fallo al consultar la prediccion {} tras {} consulta(s): {}",
                        pending.id(), consultas, e.getMessage());
                return new Sondeo(actual, consultas, Sondeo.Desenlace.FALLO_LA_CONSULTA);
            }
        }

        if (actual != null && actual.pending() && porPresupuesto)
            log.warn("REPLICATE_PRESUPUESTO la prediccion {} seguia en curso al agotarse el presupuesto total de la"
                    + " generacion ({} consulta(s)); se deja de esperar y se cancela", pending.id(), consultas);
        else if (actual != null && actual.pending())
            log.warn("La prediccion {} seguia en curso tras {} s de sondeo "
                            + "(el POST ya habia esperado hasta {} s con Prefer: wait)",
                    pending.id(), properties.timeoutSeconds(), properties.preferWaitSeconds());

        return new Sondeo(actual, consultas,
                actual != null && actual.pending()
                        ? Sondeo.Desenlace.LIMITE_AGOTADO
                        : Sondeo.Desenlace.RESPONDIO);
    }

    /**
     * R-2: pide a Replicate que cancele una prediccion que dejamos de esperar. Una sola vez, nunca lanza: un fallo al
     * cancelar no cambia el desenlace (el respaldo sigue igual), solo queda registrado.
     *
     * @return la prediccion tal como la devolvio la cancelacion, o null si no hubo respuesta legible
     */
    private ReplicateResponse cancelar(ReplicateResponse pendiente, Sondeo.Desenlace desenlace) {
        var url = urlDeCancelacion(pendiente);
        if (url == null) {
            registrarCancelacion(pendiente, desenlace, Cancelacion.SIN_IDENTIFICADOR, null);
            return null;
        }
        try {
            var raw = pollClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiToken())
                    .retrieve()
                    .body(String.class);
            ReplicateResponse r;
            try {
                r = raw == null || raw.isBlank() ? null : jsonSupport.read(raw, ReplicateResponse.class);
            } catch (RuntimeException e) {
                r = null;
            }
            String estado = r == null ? null : r.status();
            var resultado = "canceled".equals(estado) ? Cancelacion.CANCELADA
                    : r != null && (r.succeeded() || "failed".equals(estado)) ? Cancelacion.YA_TERMINADA
                    : Cancelacion.SOLICITADA;
            registrarCancelacion(pendiente, desenlace, resultado, estado);
            return r;
        } catch (org.springframework.web.client.RestClientResponseException e) {
            registrarCancelacion(pendiente, desenlace, Cancelacion.RECHAZADA, "http_" + e.getStatusCode().value());
            return null;
        } catch (RuntimeException e) {
            registrarCancelacion(pendiente, desenlace, Cancelacion.ERROR_DE_RED, null);
            return null;
        }
    }

    /**
     * La URL de cancelacion: {@code urls.cancel} si Replicate la dio; si no, la de la API publica
     * ({@code POST /v1/predictions/{id}/cancel}) en el mismo servidor que {@code fitsense.ai.base-url}.
     */
    String urlDeCancelacion(ReplicateResponse r) {
        if (r.urls() != null && r.urls().cancel() != null && !r.urls().cancel().isBlank()) return r.urls().cancel();
        if (r.id() == null || !ID_VALIDO.matcher(r.id()).matches()) return null;
        try {
            var base = URI.create(properties.baseUrl());
            return base.getScheme() + "://" + base.getRawAuthority() + "/v1/predictions/" + r.id() + "/cancel";
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static void registrarCancelacion(ReplicateResponse pendiente, Sondeo.Desenlace desenlace,
                                             Cancelacion resultado, String estado) {
        log.warn("REPLICATE_CANCEL prediction_id={} estado_previo={} motivo={} resultado={} estado_tras_cancelar={}",
                pendiente.id() == null || pendiente.id().isBlank() ? "desconocido" : pendiente.id(),
                pendiente.status(), desenlace, resultado, estado == null ? "desconocido" : estado);
    }

    /**
     * Cuanto pedir a Replicate que retenga el POST: lo configurado o, si queda menos presupuesto, lo que quede (al
     * menos 1 s). Asi el POST tampoco se come el presupuesto.
     */
    int esperaDelPost(java.time.Instant plazo) {
        int configurada = properties.preferWaitSeconds();
        if (plazo == null) return configurada;
        long quedan = Duration.between(reloj.instant(), plazo).toSeconds();
        return (int) Math.max(1, Math.min(configurada, quedan));
    }

    private String post(Map<String, Object> input, int esperaSegundos) {
        try {
            return restClient.post()
                    .uri(properties.baseUrl())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiToken())
                    .header("Prefer", "wait=" + esperaSegundos)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("input", input))
                    .retrieve()
                    .body(String.class);
        } catch (RuntimeException e) {
            log.warn("Replicate no respondio correctamente: {}", e.getMessage());
            throw new AiProviderUnavailableException(e.getMessage());
        }
    }
}
