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

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ReplicateTrainingPlanGenerator implements TrainingPlanGenerator {

    private static final Logger log = LoggerFactory.getLogger(ReplicateTrainingPlanGenerator.class);

    /** Espera inicial de la peticion. Replicate no admite mas de 60. */
    private static final int PREFER_WAIT_SECONDS = 60;

    /** Cada cuanto se vuelve a preguntar por una prediccion en curso. */
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(3);

    private final ReplicateProperties properties;
    private final PlanPromptBuilder promptBuilder;
    private final PlanDraftFromJsonAssembler assembler;
    private final JsonSupport jsonSupport;
    private final RestClient restClient;

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
        this(properties, promptBuilder, assembler, jsonSupport, RestClient.builder());
    }

    /**
     * Costura para pruebas: con un {@code RestClient.Builder} se puede atar un
     * {@code MockRestServiceServer} y medir los tramos sin llamar a Replicate.
     * <p>
     * ES PUBLICO, y dos constructores publicos NO son un problema mientras el
     * otro lleve {@code @Autowired}, igual que en {@code PlanDraftValidator}.
     * <p>
     * El cliente se sigue construyendo aqui dentro, igual que antes: lo unico
     * que cambia es de donde sale el constructor.
     */
    public ReplicateTrainingPlanGenerator(ReplicateProperties properties,
                                          PlanPromptBuilder promptBuilder,
                                          PlanDraftFromJsonAssembler assembler,
                                          JsonSupport jsonSupport,
                                          RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.promptBuilder = promptBuilder;
        this.assembler = assembler;
        this.jsonSupport = jsonSupport;
        this.restClient = restClientBuilder.build();
    }

    @Override
    public GenerationSource source() { return GenerationSource.AI; }

    @Override
    public String modelName() { return properties.model(); }

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
        var snapshot = PlanInputSnapshot.of(context);
        var snapshotJson = jsonSupport.write(snapshot);
        var prompt = promptBuilder.build(context, snapshotJson, feedback);
        return call(prompt, context.effectiveDaysPerWeek(),
                context.userId(), context.weekNumber(), attempt);
    }

    /** Convierte la salida del modelo en borrador. Lanza InvalidPlanDraftException si no se puede. */
    public PlanDraft toDraft(String output) {
        return assembler.toDraft(output, properties.model());
    }

    /**
     * Envoltura de medicion. Es fina a proposito: lo unico que hace es abrir el
     * cronometro monotonico y garantizar que la linea REPLICATE_TIMING se
     * escriba por TODAS las salidas, incluidas las que lanzan excepcion.
     * <p>
     * Publico para que la prueba de instrumentacion pueda medir los tramos sin
     * montar un {@code PlanGenerationContext} completo. No lo llama nadie mas:
     * el pipeline entra por {@link #requestOutput}.
     */
    public String call(String prompt, int workoutCount,
                       Long userId, short weekNumber, int attempt) {
        var medicion = new Medicion();
        try {
            return callMedido(prompt, workoutCount, medicion);
        } finally {
            log.info("{}", medicion.timing(userId, weekNumber, attempt).describe());
        }
    }

    private String callMedido(String prompt, int workoutCount, Medicion medicion) {
        var input = new LinkedHashMap<String, Object>();
        input.put("model", properties.model());
        input.put("prompt", prompt);
        input.put("reasoning_effort", properties.reasoningEffort());
        input.put("json_schema", PlanJsonSchema.format(workoutCount));

        String rawBody;
        try {
            rawBody = post(input);
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

        var response = jsonSupport.read(rawBody, ReplicateResponse.class);
        medicion.predictionId(response == null ? null : response.id());

        // Prefer: wait tiene un techo duro de 60 s del lado de Replicate. Con el
        // catalogo completo el modelo tarda mas, y la respuesta llegaba con
        // estado "starting": no era un fallo, era una prediccion aun en curso
        // que se contabilizaba como intento perdido.
        var desenlace = Sondeo.Desenlace.RESPONDIO;
        if (response != null && response.pending()) {
            // Copia final: la lambda no puede capturar una variable que se reasigna.
            final var enCurso = response;
            var sondeo = medicion.sondeando(() -> awaitCompletion(enCurso));
            desenlace = sondeo.desenlace();
            response = sondeo.response();
            medicion.predictionId(response == null ? null : response.id());
        }

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

        // Evidencia de cada intento, tambien de los rechazados: la proporcion de
        // planes validos al primer intento es un resultado de la tesis y esta
        // linea es lo que permite revisar QUE propuso el modelo cada vez.
        log.info("Salida de la IA (prediccion {}, prompt {} {}, {} caracteres de prompt): {}",
                response.id(), PlanPromptBuilder.VERSION, PrescriptionPrinciples.VERSION,
                prompt.length(), output);
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
    private Sondeo awaitCompletion(ReplicateResponse pending) {
        var pollUrl = pending.pollUrl();
        if (pollUrl == null) {
            log.warn("La prediccion sigue en curso y no trae urls.get: no se puede consultar");
            return new Sondeo(pending, 0, Sondeo.Desenlace.FALLO_LA_CONSULTA);
        }

        var limite = Instant.now().plusSeconds(properties.timeoutSeconds());
        var actual = pending;
        int consultas = 0;

        while (actual != null && actual.pending() && Instant.now().isBefore(limite)) {
            try {
                Thread.sleep(POLL_INTERVAL.toMillis());
            } catch (InterruptedException e) {
                // Restaurar la marca: alguien esta parando la aplicacion y no
                // queremos dejar el hilo en un estado inconsistente.
                Thread.currentThread().interrupt();
                return new Sondeo(actual, consultas, Sondeo.Desenlace.INTERRUMPIDO);
            }

            try {
                var raw = restClient.get()
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

        if (actual != null && actual.pending())
            log.warn("La prediccion {} seguia en curso tras {} s de sondeo "
                            + "(el POST ya habia esperado hasta {} s con Prefer: wait)",
                    pending.id(), properties.timeoutSeconds(), PREFER_WAIT_SECONDS);

        return new Sondeo(actual, consultas,
                actual != null && actual.pending()
                        ? Sondeo.Desenlace.LIMITE_AGOTADO
                        : Sondeo.Desenlace.RESPONDIO);
    }

    private String post(Map<String, Object> input) {
        try {
            return restClient.post()
                    .uri(properties.baseUrl())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiToken())
                    .header("Prefer", "wait=" + PREFER_WAIT_SECONDS)
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