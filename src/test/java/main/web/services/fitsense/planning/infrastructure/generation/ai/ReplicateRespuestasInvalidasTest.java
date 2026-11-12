package main.web.services.fitsense.planning.infrastructure.generation.ai;

import main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams;
import main.web.services.fitsense.planning.application.internal.commandservices.PlanGenerationPipeline;
import main.web.services.fitsense.planning.domain.exceptions.AiProviderUnavailableException;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.exceptions.PlanGenerationFailedException;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.PlanDraftNormalizer;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.TrainingPlanGenerator;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * R-1 · UNA RESPUESTA ILEGIBLE DE REPLICATE NO DEJA AL USUARIO SIN PLAN (resiliencia de Replicate, 11/11/2026).
 * <p>
 * Antes, el sobre de la respuesta del POST se leia fuera de todo manejo de errores: un cuerpo 2xx que no era JSON
 * (una pagina de un proxy, un cuerpo vacio, un JSON cortado) lanzaba {@code IllegalStateException}, que ni el
 * pipeline ni la adaptacion capturaban. La semana se quedaba sin plan aunque el motor de reglas pudiera producirlo.
 * <p>
 * Ahora es un fallo del PROVEEDOR ({@link AiProviderUnavailableException}): cuenta para el cortacircuitos, no se
 * reintenta (como cualquier fallo del proveedor) y la semana la hace el motor de reglas, validado como siempre.
 * Un JSON del MODELO mal formado sigue siendo lo que era: un fallo de contrato que se reintenta con realimentacion.
 * <p>
 * Todo con HTTP simulado: ninguna llamada a Replicate.
 */
@DisplayName("R-1 · respuestas invalidas de Replicate: siempre al mecanismo de reintento o respaldo, nunca al vacio")
class ReplicateRespuestasInvalidasTest {

    private static final String URL = "https://api.replicate.test/predictions";
    private static final long USUARIO = 7L;
    private static final LocalDate LUNES = LocalDate.of(2026, 10, 12);

    private MockRestServiceServer server;

    private ReplicateTrainingPlanGenerator generador() {
        var builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        var properties = new ReplicateProperties(true, "token-de-prueba", URL, "gpt-5-nano", "medium", 150, 0);
        return new ReplicateTrainingPlanGenerator(properties, null, null, new JsonSupport(), builder);
    }

    // ------------------------------------------------------------------ el sobre de Replicate, ilegible

    @ParameterizedTest(name = "cuerpo 2xx: {0}")
    @ValueSource(strings = {
            "<html><body>502 Bad Gateway</body></html>",
            "texto que no es JSON",
            "{\"id\":\"p1\",\"status\":\"succeeded\",\"output\":{\"json_output\":",
            "[]",
            "\"una cadena\""})
    @DisplayName("un sobre que no es la respuesta de Replicate es un fallo del proveedor, no una excepcion suelta")
    void sobreIlegible(String cuerpo) {
        var g = generador();
        server.expect(requestTo(URL)).andRespond(withSuccess(cuerpo, MediaType.APPLICATION_JSON));

        var e = assertThrows(AiProviderUnavailableException.class, () -> g.call("prompt", 1, USUARIO, (short) 1, 1));
        assertTrue(e.getMessage().contains("no se pudo interpretar"), e.getMessage());
        assertFalse(e.getMessage().contains("502 Bad Gateway"), "no se copia el cuerpo crudo al mensaje");
        server.verify();
    }

    @Test
    @DisplayName("cuerpo 2xx vacio: fallo del proveedor")
    void sobreVacio() {
        var g = generador();
        server.expect(requestTo(URL)).andRespond(withSuccess("", MediaType.APPLICATION_JSON));
        assertThrows(AiProviderUnavailableException.class, () -> g.call("prompt", 1, USUARIO, (short) 1, 1));
    }

    @Test
    @DisplayName("sobre JSON vacio ({}): sin estado de exito, fallo del proveedor (ya era asi)")
    void sobreJsonVacio() {
        var g = generador();
        server.expect(requestTo(URL)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertThrows(AiProviderUnavailableException.class, () -> g.call("prompt", 1, USUARIO, (short) 1, 1));
    }

    @Test
    @DisplayName("sobre valido con exito: la salida del modelo pasa tal cual (control)")
    void sobreValido() {
        var g = generador();
        server.expect(requestTo(URL)).andRespond(withSuccess(
                "{\"id\":\"p1\",\"status\":\"succeeded\",\"output\":{\"json_output\":{\"workouts\":[]}}}",
                MediaType.APPLICATION_JSON));
        assertEquals("{\"workouts\":[]}", g.call("prompt", 1, USUARIO, (short) 1, 1));
    }

    // ------------------------------------------------------------------ el pipeline completo

    /**
     * El generador real con HTTP simulado, sin plantilla de prompt ni ensamblador: la peticion sale por
     * {@code call}, y la salida del modelo se convierte en borrador con una regla de prueba (JSON con
     * {@code "ok"} vale; cualquier otra cosa incumple el contrato).
     */
    private static final class SoloHttp extends ReplicateTrainingPlanGenerator {
        final AtomicInteger peticiones = new AtomicInteger();

        SoloHttp(ReplicateProperties p, RestClient.Builder b) {
            super(p, null, null, new JsonSupport(), b);
        }

        @Override
        public String requestOutput(PlanGenerationContext context, AiRetryFeedback feedback, int attempt) {
            peticiones.incrementAndGet();
            return call("prompt", 1, context.userId(), context.weekNumber(), attempt);
        }

        @Override
        public PlanDraft toDraft(String output) {
            if (output.contains("\"ok\"")) return borrador(GenerationSource.AI);
            throw new InvalidPlanDraftException(List.of("la salida del modelo no cumple el contrato"));
        }
    }

    private SoloHttp ia;
    private TrainingPlanGenerator reglas;
    private PlanGenerationPipeline pipeline;

    private void montarPipeline(boolean reglasValidas) {
        var builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        ia = new SoloHttp(new ReplicateProperties(true, "token-de-prueba", URL, "gpt-5-nano", "medium", 150, 0),
                builder);
        reglas = mock(TrainingPlanGenerator.class);
        var validador = mock(PlanDraftValidator.class);
        var normalizador = mock(PlanDraftNormalizer.class);
        when(normalizador.normalize(any(), any()))
                .thenAnswer(inv -> new PlanDraftNormalizer.Result(inv.getArgument(0), List.of()));
        if (reglasValidas) when(reglas.generate(any(), anyList())).thenReturn(borrador(GenerationSource.RULE_ENGINE));
        else when(reglas.generate(any(), anyList()))
                .thenThrow(new InvalidPlanDraftException(List.of("V4: el motor de reglas tampoco cabe")));
        pipeline = new PlanGenerationPipeline(ia, reglas, validador, normalizador);
    }

    private void responder(String cuerpo, int veces) {
        server.expect(ExpectedCount.times(veces), requestTo(URL))
                .andRespond(withSuccess(cuerpo, MediaType.APPLICATION_JSON));
    }

    private static String exito(String salida) {
        return "{\"id\":\"p1\",\"status\":\"succeeded\",\"output\":{\"json_output\":" + salida + "}}";
    }

    @Test
    @DisplayName("FALLABA ANTES · sobre ilegible: el motor de reglas publica la semana (RULE_ENGINE), 1 llamada")
    void sobreIlegibleLlegaAlRespaldo() {
        montarPipeline(true);
        responder("<html>proxy</html>", 1);

        var r = pipeline.run(contexto(), 30);

        assertEquals(GenerationSource.RULE_ENGINE, r.draft().source());
        assertEquals(1, ia.peticiones.get(), "un fallo del proveedor no gasta los otros intentos");
        assertEquals(2, r.attempts(), "1 de IA + el respaldo");
        verify(reglas).generate(any(), argThat(l -> l.stream().anyMatch(p -> p.contains("no se pudo interpretar"))));
        server.verify();
    }

    @Test
    @DisplayName("salida del modelo mal formada en los 3 intentos: 3 llamadas, sin bucle, y respaldo")
    void tresSalidasInvalidas() {
        montarPipeline(true);
        responder(exito("{\"incompleto\":true}"), 3);

        var r = pipeline.run(contexto(), 30);

        assertEquals(GenerationSource.RULE_ENGINE, r.draft().source());
        assertEquals(3, ia.peticiones.get(), "nunca mas de 3 intentos");
        assertEquals(4, r.attempts());
        server.verify();
    }

    @Test
    @DisplayName("salida invalida y luego valida: publica la IA en el segundo intento")
    void validaEnElSegundo() {
        montarPipeline(true);
        server.expect(requestTo(URL)).andRespond(withSuccess(exito("{\"mal\":1}"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(URL)).andRespond(withSuccess(exito("{\"ok\":1}"), MediaType.APPLICATION_JSON));

        var r = pipeline.run(contexto(), 30);

        assertEquals(GenerationSource.AI, r.draft().source());
        assertEquals(2, r.attempts());
        verifyNoInteractions(reglas);
        server.verify();
    }

    @Test
    @DisplayName("valida a la primera: 1 llamada, sin respaldo (control)")
    void validaALaPrimera() {
        montarPipeline(true);
        responder(exito("{\"ok\":1}"), 1);

        var r = pipeline.run(contexto(), 30);

        assertEquals(GenerationSource.AI, r.draft().source());
        assertEquals(1, r.attempts());
        verifyNoInteractions(reglas);
    }

    @Test
    @DisplayName("sobre ilegible y el respaldo tampoco vale: error explicito, ningun plan")
    void fallanLaIaYElRespaldo() {
        montarPipeline(false);
        responder("no es json", 1);

        var e = assertThrows(PlanGenerationFailedException.class, () -> pipeline.run(contexto(), 30));
        assertTrue(e.getMessage().contains("V4"), e.getMessage());
    }

    @Test
    @DisplayName("un error inesperado en el camino de la IA tampoco deja sin plan: se registra y publica el respaldo")
    void errorInesperadoDeLaIa() {
        var reglasMock = mock(TrainingPlanGenerator.class);
        when(reglasMock.generate(any(), anyList())).thenReturn(borrador(GenerationSource.RULE_ENGINE));
        var normalizador = mock(PlanDraftNormalizer.class);
        when(normalizador.normalize(any(), any()))
                .thenAnswer(inv -> new PlanDraftNormalizer.Result(inv.getArgument(0), List.of()));
        var iaMock = mock(ReplicateTrainingPlanGenerator.class);
        when(iaMock.isEnabled()).thenReturn(true);
        when(iaMock.requestOutput(any(), any(), anyInt())).thenThrow(new IllegalStateException("fallo interno"));
        var p = new PlanGenerationPipeline(iaMock, reglasMock, mock(PlanDraftValidator.class), normalizador);

        var r = p.run(contexto(), 30);

        assertEquals(GenerationSource.RULE_ENGINE, r.draft().source());
        verify(iaMock, times(1)).requestOutput(any(), any(), anyInt());
    }

    // ------------------------------------------------------------------ ayudantes

    private static PlanGenerationContext contexto() {
        return new PlanGenerationContext(USUARIO, (short) 1, LUNES, LUNES.plusDays(6),
                perfil(), PlanAdjustment.none(), null, List.of(ej(1L), ej(2L)),
                SafetyProfile.none(), (PrescriptionParams) null);
    }

    private static PlanDraft borrador(GenerationSource origen) {
        var ejercicios = new ArrayList<PlanDraft.DraftExercise>();
        ejercicios.add(new PlanDraft.DraftExercise(1L, PrescriptionType.SETS_REPS,
                (short) 2, (short) 10, null, null, (short) 75, null));
        return new PlanDraft(origen, origen == GenerationSource.AI ? "gpt-5-nano" : null,
                "plan de prueba", null, "prueba",
                List.of(new PlanDraft.DraftWorkout(LUNES, WorkoutFocus.FULL_BODY,
                        "FULL_BODY - Sesion 1", 45, ejercicios)));
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
