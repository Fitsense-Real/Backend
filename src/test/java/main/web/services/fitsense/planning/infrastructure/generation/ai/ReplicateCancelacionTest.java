package main.web.services.fitsense.planning.infrastructure.generation.ai;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams;
import main.web.services.fitsense.planning.application.internal.commandservices.PlanGenerationPipeline;
import main.web.services.fitsense.planning.domain.exceptions.AiProviderUnavailableException;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.PlanDraftNormalizer;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.TrainingPlanGenerator;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * R-2 · UNA PREDICCION QUE DEJAMOS DE ESPERAR SE CANCELA (resiliencia de Replicate, 11/11/2026).
 * <p>
 * Antes, al agotar el limite de sondeo (150 s) la prediccion seguia ejecutandose en Replicate, sin que nadie la
 * leyera: {@code urls.cancel} se recibia y nunca se usaba. Ahora se pide la cancelacion UNA vez y se registra el
 * resultado ({@code REPLICATE_CANCEL}). El desenlace no cambia: fallo del proveedor y respaldo.
 * <p>
 * Reloj y pausa controlados: ninguna prueba espera 150 s de verdad. HTTP simulado: ninguna llamada a Replicate.
 */
@DisplayName("R-2 · predicciones con timeout: cancelacion registrada, una sola vez, y sin respuestas tardias")
class ReplicateCancelacionTest {

    private static final String URL = "https://api.replicate.test/v1/models/openai/gpt-5-structured/predictions";
    private static final String GET = "https://api.replicate.test/v1/predictions/p1";
    private static final String CANCEL = "https://api.replicate.test/v1/predictions/p1/cancel";
    private static final long USUARIO = 7L;
    private static final LocalDate LUNES = LocalDate.of(2026, 10, 12);

    /** Reloj que solo avanza cuando la pausa del sondeo lo pide: cada pausa de 3 s cuenta 60 s. */
    private static final class Reloj extends Clock {
        Instant ahora = Instant.parse("2026-11-11T12:00:00Z");
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return ahora; }
    }

    private MockRestServiceServer server;
    private Reloj reloj;
    private ListAppender<ILoggingEvent> logs;
    private ch.qos.logback.classic.Logger logger;

    @BeforeEach
    void capturar() {
        logger = ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger(ReplicateTrainingPlanGenerator.class);
        logs = new ListAppender<>();
        logs.start();
        logger.addAppender(logs);
        logger.setLevel(Level.INFO);
    }

    @AfterEach
    void soltar() {
        logger.detachAppender(logs);
    }

    private ReplicateTrainingPlanGenerator generador() {
        var builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        reloj = new Reloj();
        var p = new ReplicateProperties(true, "token-de-prueba", URL, "gpt-5-nano", "medium", 150, 60);
        return new ReplicateTrainingPlanGenerator(p, null, null, new JsonSupport(), builder)
                .conTiempoDePrueba(reloj, d -> reloj.ahora = reloj.ahora.plusSeconds(60));
    }

    private static String prediccion(String status, boolean conCancel) {
        return "{\"id\":\"p1\",\"status\":\"" + status + "\",\"output\":"
                + ("succeeded".equals(status) ? "{\"json_output\":{\"ok\":1}}" : "null")
                + ",\"urls\":{\"get\":\"" + GET + "\",\"cancel\":" + (conCancel ? "\"" + CANCEL + "\"" : "null") + "}}";
    }

    private void postEnCurso(boolean conCancel) {
        server.expect(requestTo(URL)).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(prediccion("starting", conCancel), MediaType.APPLICATION_JSON));
    }

    /** Con 150 s de limite y 60 s por pausa: 3 consultas y se agota. */
    private void tresConsultasEnCurso() {
        server.expect(ExpectedCount.times(3), requestTo(GET)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(prediccion("processing", true), MediaType.APPLICATION_JSON));
    }

    private List<String> cancelaciones() {
        return logs.list.stream().map(ILoggingEvent::getFormattedMessage)
                .filter(m -> m.startsWith("REPLICATE_CANCEL")).toList();
    }

    @Test
    @DisplayName("termina normalmente: no se cancela nada (control)")
    void terminaNormalmente() {
        var g = generador();
        postEnCurso(true);
        server.expect(requestTo(GET))
                .andRespond(withSuccess(prediccion("succeeded", true), MediaType.APPLICATION_JSON));

        assertEquals("{\"ok\":1}", g.call("prompt", 1, USUARIO, (short) 1, 1));
        server.verify();
        assertTrue(cancelaciones().isEmpty());
    }

    @Test
    @DisplayName("FALLABA ANTES · supera los 150 s: se pide la cancelacion una vez y se registra CANCELADA")
    void superaElLimiteYSeCancela() {
        var g = generador();
        postEnCurso(true);
        tresConsultasEnCurso();
        server.expect(ExpectedCount.once(), requestTo(CANCEL)).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(prediccion("canceled", true), MediaType.APPLICATION_JSON));

        assertThrows(AiProviderUnavailableException.class, () -> g.call("prompt", 1, USUARIO, (short) 1, 1));
        server.verify();
        var c = cancelaciones();
        assertEquals(1, c.size(), c.toString());
        assertTrue(c.get(0).contains("prediction_id=p1") && c.get(0).contains("motivo=LIMITE_AGOTADO")
                && c.get(0).contains("resultado=CANCELADA"), c.get(0));
    }

    @Test
    @DisplayName("cancelacion rechazada por Replicate: queda registrada, una sola peticion, y el desenlace no cambia")
    void cancelacionRechazada() {
        var g = generador();
        postEnCurso(true);
        tresConsultasEnCurso();
        server.expect(ExpectedCount.once(), requestTo(CANCEL)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThrows(AiProviderUnavailableException.class, () -> g.call("prompt", 1, USUARIO, (short) 1, 1));
        server.verify();
        assertTrue(cancelaciones().get(0).contains("resultado=RECHAZADA"), cancelaciones().toString());
    }

    @Test
    @DisplayName("error de red al cancelar: registrado como ERROR_DE_RED, sin reintentar la cancelacion")
    void errorDeRedAlCancelar() {
        var g = generador();
        postEnCurso(true);
        tresConsultasEnCurso();
        server.expect(ExpectedCount.once(), requestTo(CANCEL)).andRespond(withException(new IOException("reset")));

        assertThrows(AiProviderUnavailableException.class, () -> g.call("prompt", 1, USUARIO, (short) 1, 1));
        server.verify();
        assertTrue(cancelaciones().get(0).contains("resultado=ERROR_DE_RED"), cancelaciones().toString());
    }

    @Test
    @DisplayName("termino justo antes de cancelar: YA_TERMINADA y se usa su salida (no se ha publicado nada aun)")
    void terminoAntesDeCancelar() {
        var g = generador();
        postEnCurso(true);
        tresConsultasEnCurso();
        server.expect(ExpectedCount.once(), requestTo(CANCEL))
                .andRespond(withSuccess(prediccion("succeeded", true), MediaType.APPLICATION_JSON));

        assertEquals("{\"ok\":1}", g.call("prompt", 1, USUARIO, (short) 1, 1));
        server.verify();
        assertTrue(cancelaciones().get(0).contains("resultado=YA_TERMINADA"), cancelaciones().toString());
    }

    @Test
    @DisplayName("sin urls.cancel: se deriva la URL publica de cancelacion a partir del identificador")
    void urlDerivadaDelIdentificador() {
        var g = generador();
        postEnCurso(false);
        server.expect(ExpectedCount.times(3), requestTo(GET))
                .andRespond(withSuccess(prediccion("processing", false), MediaType.APPLICATION_JSON));
        server.expect(ExpectedCount.once(), requestTo(CANCEL))
                .andRespond(withSuccess(prediccion("canceled", false), MediaType.APPLICATION_JSON));

        assertThrows(AiProviderUnavailableException.class, () -> g.call("prompt", 1, USUARIO, (short) 1, 1));
        server.verify();
    }

    @Test
    @DisplayName("el sondeo falla: tambien se cancela, porque la prediccion sigue viva y nadie la va a leer")
    void sondeoFallidoTambienCancela() {
        var g = generador();
        postEnCurso(true);
        server.expect(requestTo(GET)).andRespond(withException(new IOException("timeout de lectura")));
        server.expect(ExpectedCount.once(), requestTo(CANCEL))
                .andRespond(withSuccess(prediccion("canceled", true), MediaType.APPLICATION_JSON));

        assertThrows(AiProviderUnavailableException.class, () -> g.call("prompt", 1, USUARIO, (short) 1, 1));
        server.verify();
        assertTrue(cancelaciones().get(0).contains("motivo=FALLO_LA_CONSULTA"), cancelaciones().toString());
    }

    @Test
    @DisplayName("una prediccion que ya fallo no se cancela (no estaba activa)")
    void prediccionFallidaNoSeCancela() {
        var g = generador();
        postEnCurso(true);
        server.expect(requestTo(GET)).andRespond(withSuccess(prediccion("failed", true), MediaType.APPLICATION_JSON));

        assertThrows(AiProviderUnavailableException.class, () -> g.call("prompt", 1, USUARIO, (short) 1, 1));
        server.verify();
        assertTrue(cancelaciones().isEmpty());
    }

    @Test
    @DisplayName("identificador con caracteres raros: no se deriva ninguna URL; SIN_IDENTIFICADOR")
    void identificadorInvalido() {
        var g = generador();
        var r = new ReplicateResponse("../../otra", "processing", null, null, null, null);
        assertNull(g.urlDeCancelacion(r));
    }

    // ------------------------------------------------------------------ respuesta tardia y pipeline

    @Test
    @DisplayName("tras el timeout y la cancelacion, el respaldo publica UNA semana y nadie vuelve a leer la prediccion")
    void sinRespuestaTardia() {
        var g = generador();
        // Un solo intento de IA (fallo del proveedor), su cancelacion y nada mas: si alguien volviera a consultar
        // la prediccion, MockRestServiceServer fallaria por peticion inesperada.
        postEnCurso(true);
        tresConsultasEnCurso();
        server.expect(ExpectedCount.once(), requestTo(CANCEL))
                .andRespond(withSuccess(prediccion("canceled", true), MediaType.APPLICATION_JSON));

        var ia = new SoloHttp(g);
        var reglas = mock(TrainingPlanGenerator.class);
        when(reglas.generate(any(), anyList())).thenReturn(borrador(GenerationSource.RULE_ENGINE));
        var normalizador = mock(PlanDraftNormalizer.class);
        when(normalizador.normalize(any(), any()))
                .thenAnswer(inv -> new PlanDraftNormalizer.Result(inv.getArgument(0), List.of()));
        var pipeline = new PlanGenerationPipeline(ia, reglas, mock(PlanDraftValidator.class), normalizador);

        var r = pipeline.run(contexto(), 30);

        assertEquals(GenerationSource.RULE_ENGINE, r.draft().source());
        verify(reglas, times(1)).generate(any(), anyList());
        server.verify();
    }

    /** Delega el HTTP en el generador de la prueba, sin plantilla de prompt ni ensamblador. */
    private static final class SoloHttp extends ReplicateTrainingPlanGenerator {
        private final ReplicateTrainingPlanGenerator http;

        SoloHttp(ReplicateTrainingPlanGenerator http) {
            super(new ReplicateProperties(true, "t", URL, "gpt-5-nano", "medium", 150, 60), null, null,
                    new JsonSupport(), RestClient.builder());
            this.http = http;
        }

        @Override
        public boolean isEnabled() { return true; }

        @Override
        public String requestOutput(PlanGenerationContext context, AiRetryFeedback feedback, int attempt) {
            return http.call("prompt", 1, context.userId(), context.weekNumber(), attempt);
        }

        @Override
        public PlanDraft toDraft(String output) {
            throw new InvalidPlanDraftException(List.of("no deberia llegar aqui"));
        }
    }

    private static PlanGenerationContext contexto() {
        return new PlanGenerationContext(USUARIO, (short) 1, LUNES, LUNES.plusDays(6),
                perfil(), PlanAdjustment.none(), null, List.of(ej(1L), ej(2L)),
                SafetyProfile.none(), (PrescriptionParams) null);
    }

    private static PlanDraft borrador(GenerationSource origen) {
        var ejercicios = new ArrayList<PlanDraft.DraftExercise>();
        ejercicios.add(new PlanDraft.DraftExercise(1L, PrescriptionType.SETS_REPS,
                (short) 2, (short) 10, null, null, (short) 75, null));
        return new PlanDraft(origen, null, "plan de prueba", null, "prueba",
                List.of(new PlanDraft.DraftWorkout(LUNES, WorkoutFocus.FULL_BODY, "FULL_BODY - Sesion 1", 45,
                        ejercicios)));
    }

    private static CandidateExercise ej(long id) {
        return new CandidateExercise(id, "ejercicio " + id, "chest", "body weight", 1,
                PrescriptionType.SETS_REPS, null, "HORIZONTAL_PUSH");
    }

    private static PlanningProfile perfil() {
        return new PlanningProfile(USUARIO, 30, "MALE", new BigDecimal("175.0"),
                new BigDecimal("72.0"), new BigDecimal("75.0"), "BEGINNER",
                "GAIN_MUSCLE", "Ganar masa muscular entrenando en casa", "Ninguna",
                "HOME", 2, List.of((short) 1, (short) 3), 45,
                List.of("body weight", "dumbbell", "band"), List.of(), SafetyProfile.Declaradas.sinLimitaciones());
    }
}
