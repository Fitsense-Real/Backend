package main.web.services.fitsense.planning;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import main.web.services.fitsense.planning.application.internal.generation.FirstPlanGenerationRunner;
import main.web.services.fitsense.planning.application.internal.generation.PipelineFirstPlanGenerationRunner;
import main.web.services.fitsense.planning.application.internal.generation.PlanGenerationOperationService;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.infrastructure.generation.ai.JdbcDiarioDePredicciones;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.*;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

/**
 * V59 · REANUDACION DE EXTREMO A EXTREMO con el pipeline REAL (IA encendida, validadores, respaldo y publicacion)
 * contra un Replicate FALSO local (com.sun.net.httpserver en un puerto efimero). No sale nada a la red y no se gasta
 * ningun credito: el token es ficticio y la URL es localhost.
 * <p>
 * El falso cuenta los POST (= predicciones que Replicate cobraria) y devuelve una salida que los validadores
 * rechazan, para que el pipeline haga todos sus intentos y termine en el respaldo determinista. El «reinicio» se
 * simula quitandole el lease a la operacion justo antes de publicar (el worker ya no es el propietario: no publica)
 * y reencolandola con el lease vencido, igual que tras la muerte del proceso.
 * <p>
 * En #71 cada reanudacion volvia a pagar todas las predicciones. Aqui se comprueba que no.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=true",
        "fitsense.ai.api-token=token-ficticio-de-prueba",
        "fitsense.ai.poll-interval-millis=250",
        "fitsense.ai.generate.max-per-hour=100",
        "fitsense.plan-generation.worker.enabled=false"
})
@DisplayName("V59 · reanudacion con Replicate simulado: el pipeline real no paga dos veces")
class ReanudacionConReplicateSimuladoIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    // ------------------------------------------------------------ Replicate falso
    static final HttpServer REPLICATE;
    static final AtomicInteger POSTS = new AtomicInteger();
    static final AtomicInteger GETS = new AtomicInteger();
    /** Acepta la prediccion (la «cobra») y corta la conexion sin responder: el id nunca llega al backend. */
    static final AtomicBoolean CORTAR_TRAS_ACEPTAR = new AtomicBoolean();

    static {
        try {
            REPLICATE = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        REPLICATE.createContext("/v1/models/", ReanudacionConReplicateSimuladoIT::crear);
        REPLICATE.createContext("/v1/predictions/", ReanudacionConReplicateSimuladoIT::consultar);
        REPLICATE.start();
    }

    static String base() {
        return "http://127.0.0.1:" + REPLICATE.getAddress().getPort();
    }

    private static void crear(HttpExchange x) throws IOException {
        x.getRequestBody().readAllBytes();
        if (!"POST".equals(x.getRequestMethod())) {
            responder(x, 405, "{}");
            return;
        }
        int n = POSTS.incrementAndGet();
        if (CORTAR_TRAS_ACEPTAR.get()) {
            x.close();          // aceptada y cobrada, pero sin respuesta
            return;
        }
        responder(x, 201, prediccion("p" + n, "starting", null));
    }

    private static void consultar(HttpExchange x) throws IOException {
        GETS.incrementAndGet();
        String id = x.getRequestURI().getPath().substring("/v1/predictions/".length());
        if (id.endsWith("/cancel")) {
            responder(x, 200, prediccion(id.substring(0, id.length() - "/cancel".length()), "canceled", null));
            return;
        }
        // Salida que el contrato rechaza: el pipeline agota sus intentos y publica el respaldo validado.
        responder(x, 200, prediccion(id, "succeeded", "\"{}\""));
    }

    private static String prediccion(String id, String estado, String salida) {
        return "{\"id\":\"" + id + "\",\"status\":\"" + estado + "\",\"output\":" + (salida == null ? "null" : salida)
                + ",\"error\":null,\"urls\":{\"get\":\"" + base() + "/v1/predictions/" + id + "\",\"cancel\":\""
                + base() + "/v1/predictions/" + id + "/cancel\"}}";
    }

    private static void responder(HttpExchange x, int codigo, String cuerpo) throws IOException {
        byte[] b = cuerpo.getBytes(StandardCharsets.UTF_8);
        x.getResponseHeaders().add("Content-Type", "application/json");
        x.sendResponseHeaders(codigo, b.length);
        try (var o = x.getResponseBody()) {
            o.write(b);
        }
    }

    @DynamicPropertySource
    static void replicateLocal(DynamicPropertyRegistry r) {
        r.add("fitsense.ai.base-url", () -> base() + "/v1/models/openai/gpt-5-structured/predictions");
    }

    @AfterAll
    static void parar() {
        REPLICATE.stop(0);
    }

    // ------------------------------------------------------------ contexto
    static final AtomicInteger SECUENCIA = new AtomicInteger();

    @Autowired PlanGenerationOperationService service;
    @Autowired UserProfileCommandService perfiles;
    @Autowired WeeklyTrainingPlanCommandService planes;
    @Autowired JdbcTemplate jdbc;
    @Autowired JdbcDiarioDePredicciones libro;
    @MockitoSpyBean FirstPlanGenerationRunner runner;

    /** Cuantas ejecuciones pierden el lease justo antes de publicar (el proceso «muere» ahi). */
    final AtomicInteger morirAntesDePublicar = new AtomicInteger();

    @BeforeEach
    void preparar() {
        POSTS.set(0);
        GETS.set(0);
        CORTAR_TRAS_ACEPTAR.set(false);
        morirAntesDePublicar.set(0);
        jdbc.update("UPDATE plan_generation_operations SET status = 'FAILED', reason_code = 'LIMPIEZA_DE_PRUEBA',"
                + " finished_at = now(), updated_at = now(), lease_owner = NULL, lease_expires_at = NULL"
                + " WHERE status IN ('PENDING', 'PROCESSING')");
        var real = new PipelineFirstPlanGenerationRunner(planes);
        doAnswer(inv -> {
            Long u = inv.getArgument(0);
            Runnable antesDePublicar = inv.getArgument(1);
            return real.run(u, () -> {
                if (morirAntesDePublicar.getAndDecrement() > 0)
                    jdbc.update("UPDATE plan_generation_operations SET lease_owner = 'otro-proceso'"
                            + " WHERE user_id = ? AND status = 'PROCESSING'", u);
                antesDePublicar.run();
            });
        }).when(runner).run(any(), any());
    }

    long participante() {
        String email = "v59e2e-" + SECUENCIA.incrementAndGet() + "-" + UUID.randomUUID() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'V59', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 3, 10), BiologicalSex.FEMALE,
                new BigDecimal("164"), new BigDecimal("63"), null, FitnessLevel.INTERMEDIATE, GoalType.GENERAL_FITNESS,
                "Generacion asincrona V59", TrainingLocation.GYM, (short) 2, List.of((short) 2, (short) 4), (short) 55,
                List.of("body weight", "dumbbell", "barbell", "bench", "cable", "leverage machine", "band",
                        "pull-up bar"), List.of(), "Ninguna", true, true, true, false, false));
        return u;
    }

    private String estado(UUID id) {
        return jdbc.queryForObject("SELECT status FROM plan_generation_operations WHERE operation_id = ?",
                String.class, id);
    }

    private int planesDe(long u) {
        return jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ?", Integer.class, u);
    }

    private int ejecuciones(UUID id) {
        return jdbc.queryForObject("SELECT executions FROM plan_generation_operations WHERE operation_id = ?",
                Integer.class, id);
    }

    /** El proceso murio: lease vencido; el barrido la devuelve a la cola y se vuelve a reclamar. */
    private void reiniciar(UUID id) {
        jdbc.update("UPDATE plan_generation_operations SET lease_expires_at = now() - interval '1 second'"
                + " WHERE operation_id = ?", id);
        service.recoverStale();
        assertThat(estado(id)).isEqualTo("PENDING");
        assertThat(service.claimNext()).contains(id);
    }

    @Test
    @DisplayName("E3/E4/E10 · muere antes de publicar con todas las predicciones pagadas: la reanudacion no crea ninguna")
    void reanudacionNoPagaOtraVez() {
        long u = participante();
        var id = service.request(u).operation().getId();
        morirAntesDePublicar.set(1);
        assertThat(service.claimNext()).contains(id);
        service.execute(id);

        int pagadas = POSTS.get();
        assertThat(pagadas).as("la primera ejecucion hace todos los intentos de IA").isEqualTo(3);
        assertThat(estado(id)).as("perdio el lease: no publica ni cierra").isEqualTo("PROCESSING");
        assertThat(planesDe(u)).isZero();

        reiniciar(id);
        service.execute(id);

        assertThat(POSTS.get()).as("la reanudacion reutiliza las 3 salidas del libro: 0 POST nuevos").isEqualTo(3);
        assertThat(estado(id)).isEqualTo("COMPLETED");
        assertThat(planesDe(u)).isEqualTo(1);
        assertThat(ejecuciones(id)).isEqualTo(2);
        var r = libro.recuento(id);
        assertThat(r.iniciadas()).isEqualTo(3);
        assertThat(r.completadas()).isEqualTo(3);
        assertThat(r.reutilizadas()).isZero();
    }

    @Test
    @DisplayName("dos reinicios seguidos: siguen siendo 3 predicciones y 1 plan (en #71 serian 9)")
    void dosReiniciosSiguenSiendoTres() {
        long u = participante();
        var id = service.request(u).operation().getId();
        morirAntesDePublicar.set(2);
        assertThat(service.claimNext()).contains(id);
        service.execute(id);
        reiniciar(id);
        service.execute(id);
        assertThat(estado(id)).isEqualTo("PROCESSING");
        reiniciar(id);
        service.execute(id);

        assertThat(POSTS.get()).isEqualTo(3);
        assertThat(estado(id)).isEqualTo("COMPLETED");
        assertThat(planesDe(u)).isEqualTo(1);
        assertThat(ejecuciones(id)).isEqualTo(3);
    }

    @Test
    @DisplayName("E15 · reintento manual tras un fallo con la misma peticion: adopta las salidas, 0 POST nuevos")
    void reintentoManualAdopta() {
        long u = participante();
        var primera = service.request(u).operation().getId();
        morirAntesDePublicar.set(1);
        assertThat(service.claimNext()).contains(primera);
        service.execute(primera);
        assertThat(POSTS.get()).isEqualTo(3);
        // La operacion se cierra sin plan (por ejemplo, el barrido por tiempo maximo) y el usuario reintenta.
        jdbc.update("UPDATE plan_generation_operations SET status = 'FAILED', reason_code = 'TIEMPO_EXCEDIDO',"
                + " finished_at = now(), lease_owner = NULL, lease_expires_at = NULL WHERE operation_id = ?", primera);

        var segunda = service.request(u).operation().getId();
        assertThat(segunda).isNotEqualTo(primera);
        assertThat(service.claimNext()).contains(segunda);
        service.execute(segunda);

        assertThat(POSTS.get()).as("las 3 predicciones ya pagadas se adoptan").isEqualTo(3);
        assertThat(estado(segunda)).isEqualTo("COMPLETED");
        assertThat(planesDe(u)).isEqualTo(1);
        assertThat(libro.recuento(segunda).reutilizadas()).isEqualTo(3);
        assertThat(libro.recuento(segunda).iniciadas()).isZero();
    }

    @Test
    @DisplayName("E17 · Replicate acepta pero el id no llega: NO_CONFIRMADA, no se reenvia, respaldo validado")
    void aceptadaSinIdNoSeReenvia() {
        long u = participante();
        var id = service.request(u).operation().getId();
        CORTAR_TRAS_ACEPTAR.set(true);
        morirAntesDePublicar.set(1);
        assertThat(service.claimNext()).contains(id);
        service.execute(id);

        int aceptadas = POSTS.get();
        var r = libro.recuento(id);
        // Un fallo del proveedor no se reintenta con la IA (R-1): una sola prediccion aceptada, NO_CONFIRMADA.
        assertThat(aceptadas).as("el POST aceptado sin id queda NO_CONFIRMADA y no se reenvia").isEqualTo(1);
        assertThat(r.noConfirmadas()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM plan_generation_predictions WHERE operation_id = ?"
                + " AND replicate_prediction_id IS NOT NULL", Integer.class, id)).isZero();

        // Reinicio: la reanudacion repite NO_CONFIRMADA sin otro POST, y publica el respaldo validado.
        reiniciar(id);
        service.execute(id);
        assertThat(POSTS.get()).as("ningun POST nuevo tras el reinicio").isEqualTo(aceptadas);
        assertThat(estado(id)).isEqualTo("COMPLETED");
        assertThat(planesDe(u)).isEqualTo(1);
        assertThat(libro.recuento(id).noConfirmadas()).isEqualTo(aceptadas);
    }

    @Test
    @DisplayName("sin reinicios: la generacion normal hace sus 3 intentos una sola vez y publica")
    void caminoNormal() {
        long u = participante();
        var id = service.request(u).operation().getId();
        assertThat(service.claimNext()).contains(id);
        service.execute(id);
        assertThat(POSTS.get()).isEqualTo(3);
        assertThat(estado(id)).isEqualTo("COMPLETED");
        assertThat(planesDe(u)).isEqualTo(1);
        assertThat(ejecuciones(id)).isEqualTo(1);
    }
}
