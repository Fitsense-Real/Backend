package main.web.services.fitsense.planning;

import main.web.services.fitsense.catalog.domain.exceptions.EmptyEligibleSetException;
import main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import main.web.services.fitsense.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import main.web.services.fitsense.planning.application.internal.generation.FirstPlanGenerationRunner;
import main.web.services.fitsense.planning.application.internal.generation.GenerationFailureClassifier;
import main.web.services.fitsense.planning.application.internal.generation.PlanGenerationOperationService;
import main.web.services.fitsense.planning.domain.model.valueobjects.GenerationOperationStatus;
import main.web.services.fitsense.planning.domain.model.valueobjects.NoFactibleReason;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.domain.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.junit.jupiter.api.BeforeEach;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * V58 · generacion inicial asincrona contra una base PostgreSQL de pruebas
 * migrada desde cero (V1..V58), con la IA APAGADA: el pipeline real corre con
 * el motor de reglas y todos los validadores, sin llamar a Replicate.
 * <p>
 * Como las IT de P2-D, solo corre si se le da una base explicita:
 * <pre>
 *   ./mvnw -Pintegration verify -Dit.test=GeneracionAsincronaIT \
 *     -Dfitsense.pruebas.url=jdbc:postgresql://localhost:5433/fitsense_v58 \
 *     -Dfitsense.pruebas.usuario=postgres -Dfitsense.pruebas.clave=x
 * </pre>
 * El worker programado se apaga: cada prueba lo conduce paso a paso
 * (claimNext/execute/recoverStale) para observar cada estado.
 */
@EnabledIfSystemProperty(named = "fitsense.pruebas.url", matches = ".+")
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.plan-generation.worker.enabled=false"
})
@DisplayName("V58 · generacion inicial asincrona")
class GeneracionAsincronaIT {

    private static final AtomicInteger SECUENCIA = new AtomicInteger();
    private static final List<String> EQUIPAMIENTO = List.of("body weight", "dumbbell", "barbell", "cable",
            "leverage machine", "bench", "band", "kettlebell", "ez barbell", "smith machine");

    @DynamicPropertySource
    static void apuntarAPruebas(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", () -> System.getProperty("fitsense.pruebas.url"));
        registro.add("spring.datasource.username", () -> System.getProperty("fitsense.pruebas.usuario"));
        registro.add("spring.datasource.password", () -> System.getProperty("fitsense.pruebas.clave"));
        registro.add("spring.flyway.url", () -> System.getProperty("fitsense.pruebas.url"));
        registro.add("spring.flyway.user", () -> System.getProperty("fitsense.pruebas.usuario"));
        registro.add("spring.flyway.password", () -> System.getProperty("fitsense.pruebas.clave"));
        registro.add("spring.flyway.enabled", () -> "true");
        registro.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired PlanGenerationOperationService service;
    @Autowired UserProfileCommandService perfiles;
    @Autowired UserRepository usuarios;
    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext contexto;
    MockMvc mvc;
    @MockitoSpyBean FirstPlanGenerationRunner runner;

    @BeforeEach
    void http() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        // claimNext toma la PENDING mas antigua de CUALQUIER usuario (es lo correcto en produccion):
        // se cierran las que dejaron pruebas anteriores para que cada prueba reclame la suya.
        jdbc.update("UPDATE plan_generation_operations SET status = 'FAILED', reason_code = 'LIMPIEZA_DE_PRUEBA',"
                + " finished_at = now(), updated_at = now() WHERE status IN ('PENDING', 'PROCESSING')");
    }

    long participante() {
        String email = "v58-" + SECUENCIA.incrementAndGet() + "-" + UUID.randomUUID() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'V58', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 3, 10), BiologicalSex.FEMALE,
                new BigDecimal("164"), new BigDecimal("63"), null, FitnessLevel.INTERMEDIATE, GoalType.GENERAL_FITNESS,
                "Generacion asincrona", TrainingLocation.GYM, (short) 2, List.of((short) 2, (short) 4), (short) 55,
                EQUIPAMIENTO, List.of(), "Ninguna", true, true, true, false, false));
        return u;
    }

    private UUID procesarSiguiente() {
        var id = service.claimNext().orElseThrow();
        service.execute(id);
        return id;
    }

    private int filas(long u) {
        return jdbc.queryForObject("SELECT count(*) FROM plan_generation_operations WHERE user_id = ?", Integer.class, u);
    }

    private UserDetailsImpl principal(long u) {
        return UserDetailsImpl.build(usuarios.findById(u).orElseThrow());
    }

    // 1, 10 · usuario nuevo: se registra, se procesa y queda un plan valido publicado.
    @Test
    void laPrimeraRutinaSeGeneraEnSegundoPlanoConElPipelineReal() {
        long u = participante();
        var pedida = service.request(u);
        assertThat(pedida.created()).isTrue();
        assertThat(pedida.operation().getStatus()).isEqualTo(GenerationOperationStatus.PENDING);

        var id = service.claimNext().orElseThrow();
        assertThat(id).isEqualTo(pedida.operation().getId());
        assertThat(service.find(u, id).getStatus()).isEqualTo(GenerationOperationStatus.PROCESSING);

        service.execute(id);
        var hecha = service.find(u, id);
        assertThat(hecha.getStatus()).isEqualTo(GenerationOperationStatus.COMPLETED);
        assertThat(hecha.getPlanId()).isNotNull();
        assertThat(hecha.getFinishedAt()).isNotNull();
        assertThat(jdbc.queryForObject("SELECT user_id FROM weekly_training_plans WHERE plan_id = ?", Long.class,
                hecha.getPlanId())).isEqualTo(u);
        assertThat(jdbc.queryForObject("SELECT status FROM weekly_training_plans WHERE plan_id = ?", String.class,
                hecha.getPlanId())).isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM planned_workouts WHERE plan_id = ?", Integer.class,
                hecha.getPlanId())).isPositive();
        verify(runner, times(1)).run(eq(u), any());
    }

    // 8, 18 · pedir dos veces: misma operacion, una sola generacion; despues de terminar, la misma COMPLETED.
    @Test
    void pedirDosVecesNoGeneraDosRutinas() {
        long u = participante();
        var primera = service.request(u);
        var segunda = service.request(u);
        assertThat(segunda.created()).isFalse();
        assertThat(segunda.operation().getId()).isEqualTo(primera.operation().getId());

        procesarSiguiente();
        var tercera = service.request(u);
        assertThat(tercera.created()).isFalse();
        assertThat(tercera.operation().getId()).isEqualTo(primera.operation().getId());
        assertThat(tercera.operation().getStatus()).isEqualTo(GenerationOperationStatus.COMPLETED);
        assertThat(filas(u)).isEqualTo(1);
        verify(runner, times(1)).run(eq(u), any());
    }

    @Test
    void peticionesSimultaneasDevuelvenLaMismaOperacion() throws Exception {
        long u = participante();
        var salida = new CountDownLatch(1);
        var a = CompletableFuture.supplyAsync(() -> { await(salida); return service.request(u); });
        var b = CompletableFuture.supplyAsync(() -> { await(salida); return service.request(u); });
        salida.countDown();
        var ra = a.get(30, TimeUnit.SECONDS);
        var rb = b.get(30, TimeUnit.SECONDS);
        assertThat(ra.operation().getId()).isEqualTo(rb.operation().getId());
        assertThat(ra.created() ^ rb.created()).as("exactamente una la creo").isTrue();
        assertThat(filas(u)).isEqualTo(1);
    }

    // 9, 18 · generacion de varios minutos: se consulta PROCESSING y un nuevo POST no lanza otra.
    @Test
    void unaGeneracionLargaSeConsultaSinDuplicarse() throws Exception {
        long u = participante();
        var liberar = new CountDownLatch(1);
        var empezo = new CountDownLatch(1);
        doAnswer(inv -> { empezo.countDown(); await(liberar); return inv.callRealMethod(); }).when(runner).run(eq(u), any());

        var id = service.request(u).operation().getId();
        var claimed = service.claimNext().orElseThrow();
        var ejecucion = CompletableFuture.runAsync(() -> service.execute(claimed));
        assertThat(empezo.await(30, TimeUnit.SECONDS)).isTrue();

        assertThat(service.find(u, id).getStatus()).isEqualTo(GenerationOperationStatus.PROCESSING);
        var otra = service.request(u);
        assertThat(otra.created()).isFalse();
        assertThat(otra.operation().getId()).isEqualTo(id);

        liberar.countDown();
        ejecucion.get(120, TimeUnit.SECONDS);
        assertThat(service.find(u, id).getStatus()).isEqualTo(GenerationOperationStatus.COMPLETED);
        verify(runner, times(1)).run(eq(u), any());
    }

    // 11 · NO_FACTIBLE: sin plan, con el codigo estable y el texto para el participante.
    @Test
    void sinSemanaGenerableTerminaNoFactible() {
        long u = participante();
        doThrow(new EmptyEligibleSetException(u)).when(runner).run(eq(u), any());
        var id = service.request(u).operation().getId();
        procesarSiguiente();
        var op = service.find(u, id);
        assertThat(op.getStatus()).isEqualTo(GenerationOperationStatus.NO_FACTIBLE);
        assertThat(op.getReasonCode()).isEqualTo(NoFactibleReason.SIN_SEMANA_GENERABLE.name());
        assertThat(op.getMessage()).isEqualTo(NoFactibleReason.SIN_SEMANA_GENERABLE.texto());
        assertThat(op.isRetryable()).isFalse();
        assertThat(op.getPlanId()).isNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ?", Integer.class, u))
                .isZero();
    }

    // 12 · error tecnico: FAILED reintentable; el reintento es una operacion nueva que si publica.
    @Test
    void unErrorTecnicoPermiteReintentarDeFormaSegura() {
        long u = participante();
        doThrow(new IllegalStateException("proveedor caido")).doCallRealMethod().when(runner).run(eq(u), any());
        var primera = service.request(u).operation().getId();
        procesarSiguiente();
        var fallida = service.find(u, primera);
        assertThat(fallida.getStatus()).isEqualTo(GenerationOperationStatus.FAILED);
        assertThat(fallida.getReasonCode()).isEqualTo(GenerationFailureClassifier.ERROR_TECNICO);
        assertThat(fallida.isRetryable()).isTrue();
        assertThat(fallida.getMessage()).doesNotContain("proveedor caido");

        var reintento = service.request(u);
        assertThat(reintento.created()).isTrue();
        assertThat(reintento.operation().getId()).isNotEqualTo(primera);
        procesarSiguiente();
        assertThat(service.find(u, reintento.operation().getId()).getStatus()).isEqualTo(GenerationOperationStatus.COMPLETED);
    }

    // 14 · reinicio del backend: una PROCESSING huerfana se recupera una vez y despues se cierra, nunca queda colgada.
    @Test
    void unaEjecucionInterrumpidaSeRecuperaYNoQuedaColgada() {
        long u = participante();
        var id = service.request(u).operation().getId();
        assertThat(service.claimNext()).contains(id);          // el proceso "muere" aqui
        envejecer(id);
        service.recoverStale();
        assertThat(service.find(u, id).getStatus()).isEqualTo(GenerationOperationStatus.PENDING);

        assertThat(service.claimNext()).contains(id);          // segunda ejecucion, que tambien "muere"
        envejecer(id);
        service.recoverStale();
        var cerrada = service.find(u, id);
        assertThat(cerrada.getStatus()).isEqualTo(GenerationOperationStatus.FAILED);
        assertThat(cerrada.getReasonCode()).isEqualTo(GenerationFailureClassifier.EJECUCION_INTERRUMPIDA);
        assertThat(cerrada.isRetryable()).isTrue();
        assertThat(cerrada.getExecutions()).isEqualTo((short) 2);
        verify(runner, times(0)).run(anyLong(), any());
    }

    // 13 · apagado ordenado (despliegue): la generacion interrumpida NO se registra como fallo y vuelve a la cola.
    @Test
    void unApagadoOrdenadoDevuelveLaOperacionALaColaYSeRetoma() throws Exception {
        long u = participante();
        var empezo = new CountDownLatch(1);
        var apagar = new CountDownLatch(1);
        doAnswer(inv -> { empezo.countDown(); await(apagar); throw new IllegalStateException("I/O interrumpida por el apagado"); })
                .doCallRealMethod().when(runner).run(eq(u), any());
        var id = service.request(u).operation().getId();
        var claimed = service.claimNext().orElseThrow();
        var detenido = new java.util.concurrent.atomic.AtomicBoolean();
        var ejecucion = CompletableFuture.runAsync(() -> service.execute(claimed, detenido::get));
        assertThat(empezo.await(30, TimeUnit.SECONDS)).isTrue();

        detenido.set(true);
        apagar.countDown();
        ejecucion.get(30, TimeUnit.SECONDS);
        assertThat(service.find(u, id).getStatus()).as("no es un fallo de la generacion")
                .isEqualTo(GenerationOperationStatus.PROCESSING);

        assertThat(service.requeueOnShutdown(List.of(id))).isEqualTo(1);
        var enCola = service.find(u, id);
        assertThat(enCola.getStatus()).isEqualTo(GenerationOperationStatus.PENDING);
        assertThat(enCola.getExecutions()).as("el apagado no gasta un intento").isEqualTo((short) 0);

        procesarSiguiente();                                 // la siguiente instancia la retoma
        var hecha = service.find(u, id);
        assertThat(hecha.getStatus()).isEqualTo(GenerationOperationStatus.COMPLETED);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ?", Integer.class, u))
                .isEqualTo(1);
        assertThat(filas(u)).isEqualTo(1);
    }

    @Test
    void unaRecuperacionNoPisaUnaOperacionQueTermino() {
        long u = participante();
        var id = service.request(u).operation().getId();
        procesarSiguiente();
        envejecer(id);
        service.recoverStale();
        assertThat(service.find(u, id).getStatus()).isEqualTo(GenerationOperationStatus.COMPLETED);
    }

    // 19 · dos usuarios a la vez: cada uno su operacion y su plan.
    @Test
    void dosUsuariosSimultaneosNoCompartenEstados() throws Exception {
        long ana = participante();
        long beto = participante();
        var opAna = service.request(ana).operation().getId();
        var opBeto = service.request(beto).operation().getId();
        var a = service.claimNext().orElseThrow();
        var b = service.claimNext().orElseThrow();
        CompletableFuture.allOf(CompletableFuture.runAsync(() -> service.execute(a)),
                CompletableFuture.runAsync(() -> service.execute(b))).get(180, TimeUnit.SECONDS);

        var hechaAna = service.find(ana, opAna);
        var hechaBeto = service.find(beto, opBeto);
        assertThat(hechaAna.getPlanId()).isNotEqualTo(hechaBeto.getPlanId());
        assertThat(jdbc.queryForObject("SELECT user_id FROM weekly_training_plans WHERE plan_id = ?", Long.class,
                hechaAna.getPlanId())).isEqualTo(ana);
        assertThat(jdbc.queryForObject("SELECT user_id FROM weekly_training_plans WHERE plan_id = ?", Long.class,
                hechaBeto.getPlanId())).isEqualTo(beto);
        assertThatThrownBy(() -> service.find(beto, opAna)).isInstanceOf(ResourceNotFoundException.class);
        verify(runner, times(1)).run(eq(ana), any());
        verify(runner, times(1)).run(eq(beto), any());
    }

    // Contrato HTTP: 202 y luego 200 con el mismo id, 404 a otro usuario, 401 sin sesion.
    @Test
    void contratoHttp() throws Exception {
        long u = participante();
        long otro = participante();
        var respuesta = mvc.perform(post("/api/v1/users/me/plan/generation").with(user(principal(u))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.terminal").value(false))
                .andExpect(jsonPath("$.pollAfterSeconds").value(3))
                .andReturn().getResponse().getContentAsString();
        var id = respuesta.replaceAll(".*\"operationId\":\"([0-9a-f-]+)\".*", "$1");

        mvc.perform(post("/api/v1/users/me/plan/generation").with(user(principal(u))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.operationId").value(id));
        mvc.perform(get("/api/v1/users/me/plan/generation/" + id).with(user(principal(u))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING"));
        mvc.perform(get("/api/v1/users/me/plan/generation/latest").with(user(principal(u))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.operationId").value(id));
        mvc.perform(get("/api/v1/users/me/plan/generation/" + id).with(user(principal(otro))))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/users/me/plan/generation/latest").with(user(principal(otro))))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/users/me/plan/generation/no-es-un-uuid").with(user(principal(u))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/users/me/plan/generation"))
                .andExpect(status().isUnauthorized());

        procesarSiguiente();
        mvc.perform(get("/api/v1/users/me/plan/generation/" + id).with(user(principal(u))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.terminal").value(true))
                .andExpect(jsonPath("$.planId").isNumber())
                .andExpect(jsonPath("$.pollAfterSeconds").doesNotExist());
    }

    @Test
    void sinPerfilSeRechazaAntesDeRegistrar() throws Exception {
        String email = "v58-sin-perfil-" + UUID.randomUUID() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'V58', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        mvc.perform(post("/api/v1/users/me/plan/generation").with(user(principal(u))))
                .andExpect(status().isUnprocessableContent());
        assertThat(filas(u)).isZero();
    }

    // 3, 4, 5 · una generacion lenta pero viva sigue latiendo: el barrido no la reencola ni la cierra.
    @Test
    void unaGeneracionLentaQueLateNoSeTomaPorHuerfana() {
        long u = participante();
        var id = service.request(u).operation().getId();
        assertThat(service.claimNext()).contains(id);
        envejecer(id);
        assertThat(service.heartbeat(List.of(id))).isEqualTo(1);
        service.recoverStale();
        var op = service.find(u, id);
        assertThat(op.getStatus()).isEqualTo(GenerationOperationStatus.PROCESSING);
        assertThat(op.getExecutions()).isEqualTo((short) 1);
    }

    // 14 · tiempo maximo superado en ejecucion: FAILED TIEMPO_EXCEDIDO y la rutina tardia NO se publica.
    @Test
    void superarElTiempoMaximoCierraLaOperacionSinPublicarTarde() throws Exception {
        long u = participante();
        var liberar = new CountDownLatch(1);
        var empezo = new CountDownLatch(1);
        doAnswer(inv -> { empezo.countDown(); await(liberar); return inv.callRealMethod(); })
                .doCallRealMethod().when(runner).run(eq(u), any());

        var id = service.request(u).operation().getId();
        var claimed = service.claimNext().orElseThrow();
        var ejecucion = CompletableFuture.runAsync(() -> service.execute(claimed));
        assertThat(empezo.await(30, TimeUnit.SECONDS)).isTrue();

        jdbc.update("UPDATE plan_generation_operations SET started_at = now() - interval '11 minutes' WHERE operation_id = ?", id);
        service.heartbeat(List.of(id));                     // sigue viva: lo que la cierra es el tiempo maximo
        service.recoverStale();
        var vencida = service.find(u, id);
        assertThat(vencida.getStatus()).isEqualTo(GenerationOperationStatus.FAILED);
        assertThat(vencida.getReasonCode()).isEqualTo(GenerationFailureClassifier.TIEMPO_EXCEDIDO);
        assertThat(vencida.isRetryable()).isTrue();
        assertThat(vencida.getFinishedAt()).isNotNull();

        liberar.countDown();
        ejecucion.get(180, TimeUnit.SECONDS);
        assertThat(service.find(u, id).getStatus()).as("el resultado tardio no la reabre")
                .isEqualTo(GenerationOperationStatus.FAILED);
        assertThat(service.find(u, id).getPlanId()).isNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ?", Integer.class, u))
                .as("no se publico la rutina generada fuera de tiempo").isZero();

        var reintento = service.request(u);
        assertThat(reintento.created()).isTrue();
        procesarSiguiente();
        assertThat(service.find(u, reintento.operation().getId()).getStatus()).isEqualTo(GenerationOperationStatus.COMPLETED);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ?", Integer.class, u))
                .isEqualTo(1);
    }

    // 14 · una PENDING que ningun worker atiende tampoco espera para siempre.
    @Test
    void unaPendienteQueNadieAtiendeVence() {
        long u = participante();
        var id = service.request(u).operation().getId();
        jdbc.update("UPDATE plan_generation_operations SET created_at = now() - interval '31 minutes' WHERE operation_id = ?", id);
        service.recoverStale();
        var op = service.find(u, id);
        assertThat(op.getStatus()).isEqualTo(GenerationOperationStatus.FAILED);
        assertThat(op.getReasonCode()).isEqualTo(GenerationFailureClassifier.TIEMPO_EXCEDIDO);
        assertThat(op.isRetryable()).isTrue();
        verify(runner, times(0)).run(anyLong(), any());
    }

    /** Sin latido desde hace 5 min (stale 3 min), pero dentro del tiempo maximo (10 min). */
    private void envejecer(UUID id) {
        jdbc.update("UPDATE plan_generation_operations SET started_at = now() - interval '5 minutes',"
                + " updated_at = now() - interval '5 minutes' WHERE operation_id = ?", id);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(60, TimeUnit.SECONDS)) throw new IllegalStateException("timeout");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
