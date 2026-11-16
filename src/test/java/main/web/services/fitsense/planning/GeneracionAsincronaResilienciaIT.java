package main.web.services.fitsense.planning;

import main.web.services.fitsense.catalog.domain.exceptions.EmptyEligibleSetException;
import main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import main.web.services.fitsense.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import main.web.services.fitsense.planning.application.internal.generation.FirstPlanGenerationRunner;
import main.web.services.fitsense.planning.application.internal.generation.GenerationFailureClassifier;
import main.web.services.fitsense.planning.application.internal.generation.PlanGenerationOperationService;
import main.web.services.fitsense.planning.domain.model.valueobjects.GenerationOperationStatus;
import main.web.services.fitsense.planning.infrastructure.generation.ai.DiarioDePredicciones;
import main.web.services.fitsense.planning.infrastructure.generation.ai.JdbcDiarioDePredicciones;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.*;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * V59 · RESILIENCIA DE LA GENERACION ASINCRONA sobre PostgreSQL 16 efimero (Testcontainers, migrado desde cero
 * V1..V59). Sin IA (motor de reglas real) y sin worker: cada prueba ejecuta a mano el reclamo, el latido y el barrido,
 * y fija los tiempos en la base para simular minutos sin esperarlos. Corre en el CI.
 * <p>
 * Lo de las predicciones de Replicate esta en DiarioDePrediccionesReplicateTest (HTTP simulado) y en
 * ReanudacionConReplicateSimuladoIT (pipeline real con un Replicate falso local).
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.plan-generation.worker.enabled=false"
})
@DisplayName("V59 · generacion asincrona: tiempos, lease, fencing, recuperacion, idempotencia y libro")
class GeneracionAsincronaResilienciaIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    static final AtomicInteger SECUENCIA = new AtomicInteger();
    static final List<String> EQUIPAMIENTO = List.of("body weight", "dumbbell", "barbell", "bench", "cable",
            "leverage machine", "band", "pull-up bar");

    @Autowired PlanGenerationOperationService service;
    @Autowired UserProfileCommandService perfiles;
    @Autowired UserRepository usuarios;
    @Autowired JdbcTemplate jdbc;
    @Autowired JdbcDiarioDePredicciones libro;
    @Autowired WebApplicationContext contexto;
    @Autowired org.springframework.scheduling.TaskScheduler planificadorDeSpring;
    @MockitoSpyBean FirstPlanGenerationRunner runner;
    MockMvc mvc;

    @BeforeEach
    void limpiar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        jdbc.update("UPDATE plan_generation_operations SET status = 'FAILED', reason_code = 'LIMPIEZA_DE_PRUEBA',"
                + " finished_at = now(), updated_at = now(), lease_owner = NULL, lease_expires_at = NULL"
                + " WHERE status IN ('PENDING', 'PROCESSING')");
    }

    long participante() {
        String email = "v59-" + SECUENCIA.incrementAndGet() + "-" + UUID.randomUUID() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'V59', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 3, 10), BiologicalSex.FEMALE,
                new BigDecimal("164"), new BigDecimal("63"), null, FitnessLevel.INTERMEDIATE, GoalType.GENERAL_FITNESS,
                "Generacion asincrona V59", TrainingLocation.GYM, (short) 2, List.of((short) 2, (short) 4), (short) 55,
                EQUIPAMIENTO, List.of(), "Ninguna", true, true, true, false, false));
        return u;
    }

    private String estado(UUID id) {
        return jdbc.queryForObject("SELECT status FROM plan_generation_operations WHERE operation_id = ?",
                String.class, id);
    }

    private int planes(long u) {
        return jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ?", Integer.class, u);
    }

    /** Una generacion VIVA (latiendo, lease vigente) que empezo hace {@code segundos}. */
    private void viva(UUID id, int segundos) {
        jdbc.update("UPDATE plan_generation_operations SET first_started_at = now() - make_interval(secs => ?),"
                + " started_at = now() - make_interval(secs => ?), updated_at = now(),"
                + " lease_expires_at = now() + interval '60 seconds' WHERE operation_id = ?", segundos, segundos, id);
    }

    /** El proceso que la ejecutaba murio: su lease vencio. */
    private void leaseVencido(UUID id) {
        jdbc.update("UPDATE plan_generation_operations SET lease_expires_at = now() - interval '1 second',"
                + " updated_at = now() - interval '5 minutes' WHERE operation_id = ?", id);
    }

    private UserDetailsImpl principal(long u) {
        return UserDetailsImpl.build(usuarios.findById(u).orElseThrow());
    }

    private static void esperar(CountDownLatch l) {
        try {
            l.await(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ===================================================================================== tiempos (fase 3)
    @Test
    @DisplayName("generaciones VIVAS de 30, 90, 300, 600, 900 y 1500 s no se dan por vencidas (antes: FAILED a los 600 s)")
    void generacionesLargasVivasNoVencen() {
        var ids = new ArrayList<UUID>();
        for (int s : new int[]{30, 90, 300, 600, 900, 1500}) {
            long u = participante();
            var id = service.request(u).operation().getId();
            assertThat(service.claimNext()).contains(id);
            viva(id, s);
            ids.add(id);
        }
        service.recoverStale();
        for (var id : ids) assertThat(estado(id)).as("viva: sigue PROCESSING").isEqualTo("PROCESSING");
    }

    @Test
    @DisplayName("por encima del tiempo maximo de la OPERACION (1.800 s): FAILED TIEMPO_EXCEDIDO reintentable y sin lease")
    void superaElLimiteDeLaOperacion() {
        long u = participante();
        var id = service.request(u).operation().getId();
        service.claimNext();
        viva(id, 1801);
        service.recoverStale();
        var op = service.find(u, id);
        assertThat(op.getStatus()).isEqualTo(GenerationOperationStatus.FAILED);
        assertThat(op.getReasonCode()).isEqualTo(GenerationFailureClassifier.TIEMPO_EXCEDIDO);
        assertThat(op.isRetryable()).isTrue();
        assertThat(op.getLeaseOwner()).isNull();
    }

    @Test
    @DisplayName("el tiempo maximo se mide desde la PRIMERA ejecucion: reencolar no lo reinicia")
    void elLimiteNoSeReiniciaAlReencolar() {
        long u = participante();
        var id = service.request(u).operation().getId();
        service.claimNext();
        jdbc.update("UPDATE plan_generation_operations SET first_started_at = now() - interval '1700 seconds'"
                + " WHERE operation_id = ?", id);
        leaseVencido(id);
        service.recoverStale();
        assertThat(estado(id)).isEqualTo("PENDING");
        service.claimNext();
        assertThat(jdbc.queryForObject("SELECT first_started_at < now() - interval '1600 seconds'"
                + " FROM plan_generation_operations WHERE operation_id = ?", Boolean.class, id)).isTrue();
        jdbc.update("UPDATE plan_generation_operations SET first_started_at = now() - interval '1801 seconds',"
                + " lease_expires_at = now() + interval '60 seconds' WHERE operation_id = ?", id);
        service.recoverStale();
        assertThat(estado(id)).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("E9 · latido atrasado 181 s con el lease vigente: NO se reencola (antes: reencolada y ejecutada dos veces)")
    void latidoAtrasadoConLeaseVigenteNoReencola() {
        long u = participante();
        var id = service.request(u).operation().getId();
        service.claimNext();
        jdbc.update("UPDATE plan_generation_operations SET updated_at = now() - interval '181 seconds',"
                + " lease_expires_at = now() + interval '30 seconds' WHERE operation_id = ?", id);
        service.recoverStale();
        assertThat(estado(id)).isEqualTo("PROCESSING");
        assertThat(service.claimNext()).isEmpty();
    }

    // ===================================================================================== reinicio (fase 4)
    @Test
    @DisplayName("E3 · reinicio del backend: lease vencido -> PENDING sin propietario -> se reclama y se completa una vez")
    void reinicioRecuperaYCompleta() {
        long u = participante();
        var id = service.request(u).operation().getId();
        service.claimNext();                // el proceso muere aqui
        leaseVencido(id);
        service.recoverStale();
        assertThat(estado(id)).isEqualTo("PENDING");
        assertThat(service.find(u, id).getLeaseOwner()).isNull();
        assertThat(service.claimNext()).contains(id);
        assertThat(service.find(u, id).getLeaseOwner()).isEqualTo(service.owner());
        service.execute(id);
        assertThat(estado(id)).isEqualTo("COMPLETED");
        assertThat(planes(u)).isEqualTo(1);
    }

    @Test
    @DisplayName("interrupciones repetidas: se agota max-executions (3) y se cierra FAILED EJECUCION_INTERRUMPIDA, nunca colgada")
    void interrupcionesRepetidasTerminan() {
        long u = participante();
        var id = service.request(u).operation().getId();
        for (int i = 0; i < 3; i++) {
            assertThat(service.claimNext()).contains(id);
            leaseVencido(id);
            service.recoverStale();
        }
        var op = service.find(u, id);
        assertThat(op.getStatus()).isEqualTo(GenerationOperationStatus.FAILED);
        assertThat(op.getReasonCode()).isEqualTo(GenerationFailureClassifier.EJECUCION_INTERRUMPIDA);
        assertThat(op.getExecutions()).isEqualTo((short) 3);
    }

    @Test
    @DisplayName("E11 · el plan ya se publico y el proceso murio antes de cerrar: la reanudacion la cierra con ESE plan, sin duplicar")
    void planYaPublicadoAntesDeCerrar() {
        long u = participante();
        var id = service.request(u).operation().getId();
        service.claimNext();
        service.execute(id);
        Long plan = service.find(u, id).getPlanId();
        // Se simula la muerte entre persist y el cierre: la operacion vuelve a PROCESSING de un proceso muerto.
        jdbc.update("UPDATE plan_generation_operations SET status = 'PROCESSING', plan_id = NULL, finished_at = NULL,"
                + " lease_owner = 'proceso-muerto', lease_expires_at = now() - interval '1 second'"
                + " WHERE operation_id = ?", id);
        service.recoverStale();
        assertThat(service.claimNext()).contains(id);
        service.execute(id);
        assertThat(service.find(u, id).getStatus()).isEqualTo(GenerationOperationStatus.COMPLETED);
        assertThat(service.find(u, id).getPlanId()).isEqualTo(plan);
        assertThat(planes(u)).isEqualTo(1);
    }

    @Test
    @DisplayName("apagado ordenado: solo devuelve a la cola SUS operaciones, no las de otra instancia")
    void apagadoSoloLasPropias() {
        long u = participante();
        var id = service.request(u).operation().getId();
        service.claimNext();
        jdbc.update("UPDATE plan_generation_operations SET lease_owner = 'otra-instancia' WHERE operation_id = ?", id);
        assertThat(service.requeueOnShutdown(List.of(id))).isZero();
        assertThat(estado(id)).isEqualTo("PROCESSING");
    }

    // ===================================================================================== fencing y concurrencia
    @Test
    @DisplayName("E5 · fencing: si otra instancia tiene el lease, este worker no publica, no cierra y su latido no la renueva")
    void sinLeaseNoPublica() {
        long u = participante();
        var id = service.request(u).operation().getId();
        service.claimNext();
        jdbc.update("UPDATE plan_generation_operations SET lease_owner = 'otra-instancia',"
                + " lease_expires_at = now() + interval '60 seconds' WHERE operation_id = ?", id);
        assertThat(service.heartbeat(List.of(id))).isZero();
        service.execute(id);
        assertThat(planes(u)).as("no publica sin ser el propietario").isZero();
        assertThat(estado(id)).isEqualTo("PROCESSING");
    }

    @Test
    @DisplayName("E5 · dos workers reclaman a la vez la misma PENDING: exactamente uno la obtiene (SKIP LOCKED)")
    void dosWorkersUnaOperacion() throws Exception {
        long u = participante();
        var id = service.request(u).operation().getId();
        var salida = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(4);
        var futuros = new ArrayList<Future<Optional<UUID>>>();
        for (int i = 0; i < 4; i++) futuros.add(pool.submit(() -> { esperar(salida); return service.claimNext(); }));
        salida.countDown();
        int ganadores = 0;
        for (var f : futuros) if (f.get(30, TimeUnit.SECONDS).isPresent()) ganadores++;
        pool.shutdown();
        assertThat(ganadores).isEqualTo(1);
        assertThat(service.find(u, id).getExecutions()).isEqualTo((short) 1);
    }

    @Test
    @DisplayName("E6 · ocho solicitudes simultaneas del mismo usuario: una sola operacion")
    void solicitudesSimultaneas() throws Exception {
        long u = participante();
        var salida = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(8);
        var futuros = new ArrayList<Future<UUID>>();
        for (int i = 0; i < 8; i++)
            futuros.add(pool.submit(() -> { esperar(salida); return service.request(u).operation().getId(); }));
        salida.countDown();
        var ids = new java.util.HashSet<UUID>();
        for (var f : futuros) ids.add(f.get(30, TimeUnit.SECONDS));
        pool.shutdown();
        assertThat(ids).hasSize(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM plan_generation_operations WHERE user_id = ?",
                Integer.class, u)).isEqualTo(1);
    }

    @Test
    @DisplayName("E16 · una generacion viva alcanza el maximo: se cierra y su resultado tardio NO se publica (fencing)")
    void vivaQueAlcanzaElMaximoNoPublica() throws Exception {
        long u = participante();
        var liberar = new CountDownLatch(1);
        var empezo = new CountDownLatch(1);
        doAnswer(inv -> { empezo.countDown(); esperar(liberar); return inv.callRealMethod(); })
                .when(runner).run(eq(u), any());
        var id = service.request(u).operation().getId();
        service.claimNext();
        var ejecucion = CompletableFuture.runAsync(() -> service.execute(id));
        assertThat(empezo.await(30, TimeUnit.SECONDS)).isTrue();
        viva(id, 1801);
        service.recoverStale();
        liberar.countDown();
        ejecucion.get(60, TimeUnit.SECONDS);
        assertThat(estado(id)).isEqualTo("FAILED");
        assertThat(planes(u)).isZero();
    }

    // ===================================================================================== desenlaces
    @Test
    @DisplayName("E12 · NO_FACTIBLE: terminal, no reintentable, sin plan")
    void noFactible() {
        long u = participante();
        doThrow(new EmptyEligibleSetException(u)).when(runner).run(eq(u), any());
        var id = service.request(u).operation().getId();
        service.claimNext();
        service.execute(id);
        var op = service.find(u, id);
        assertThat(op.getStatus()).isEqualTo(GenerationOperationStatus.NO_FACTIBLE);
        assertThat(op.isRetryable()).isFalse();
        assertThat(op.getLeaseOwner()).isNull();
        assertThat(planes(u)).isZero();
    }

    @Test
    @DisplayName("E13/E15 · error tecnico: FAILED reintentable; el reintento manual crea UNA operacion nueva y se completa")
    void errorTecnicoYReintentoManual() {
        long u = participante();
        doThrow(new IllegalStateException("proveedor caido")).doCallRealMethod().when(runner).run(eq(u), any());
        var primera = service.request(u).operation().getId();
        service.claimNext();
        service.execute(primera);
        assertThat(service.find(u, primera).isRetryable()).isTrue();
        var reintento = service.request(u);
        assertThat(reintento.created()).isTrue();
        assertThat(reintento.operation().getId()).isNotEqualTo(primera);
        service.claimNext();
        service.execute(reintento.operation().getId());
        assertThat(estado(reintento.operation().getId())).isEqualTo("COMPLETED");
        assertThat(planes(u)).isEqualTo(1);
    }

    @Test
    @DisplayName("E9 · PENDING sin worker durante mas de max-queued: FAILED, nunca espera infinita")
    void pendienteSinWorkerTermina() {
        long u = participante();
        var id = service.request(u).operation().getId();
        jdbc.update("UPDATE plan_generation_operations SET created_at = now() - interval '1801 seconds'"
                + " WHERE operation_id = ?", id);
        service.recoverStale();
        assertThat(estado(id)).isEqualTo("FAILED");
    }

    // ===================================================================================== Flutter (fase 6)
    @Test
    @DisplayName("E1/E2 · la app se cierra y vuelve: la generacion sigue sin consultas, latest devuelve LA MISMA y un POST no crea otra")
    void appCerradaYReabierta() throws Exception {
        long u = participante();
        var resp = mvc.perform(post("/api/v1/users/me/plan/generation").with(user(principal(u))))
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        String id = com.jayway.jsonpath.JsonPath.read(resp, "$.operationId");
        // la app se cierra: nadie consulta; el backend procesa igual
        service.claimNext();
        service.execute(UUID.fromString(id));
        // la app se reabre y recupera por latest; el tour termino antes o despues, da igual
        mvc.perform(get("/api/v1/users/me/plan/generation/latest").with(user(principal(u))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.operationId").value(id))
                .andExpect(jsonPath("$.status").value("COMPLETED")).andExpect(jsonPath("$.terminal").value(true))
                .andExpect(jsonPath("$.planId").isNumber());
        mvc.perform(post("/api/v1/users/me/plan/generation").with(user(principal(u))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.operationId").value(id));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM plan_generation_operations WHERE user_id = ?",
                Integer.class, u)).isEqualTo(1);
        assertThat(planes(u)).isEqualTo(1);
    }

    @Test
    @DisplayName("E1 · mientras se genera, la app ve PROCESSING con pollAfterSeconds, sin porcentajes ni tiempos prometidos")
    void enCursoSeVeProcessing() throws Exception {
        long u = participante();
        var id = service.request(u).operation().getId();
        service.claimNext();
        mvc.perform(get("/api/v1/users/me/plan/generation/" + id).with(user(principal(u))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.terminal").value(false)).andExpect(jsonPath("$.pollAfterSeconds").value(3))
                .andExpect(jsonPath("$.planId").doesNotExist());
    }

    // ===================================================================================== libro (JDBC real)
    @Test
    @DisplayName("libro JDBC: SOLICITANDO -> ENVIADA -> SUCCEEDED, un intento vigente, DESCARTADA, recuento")
    void libroJdbc() {
        long u = participante();
        var op = service.request(u).operation().getId();
        var ahora = Instant.now();
        String hash = "a".repeat(64);
        long f1 = libro.solicitar(op, u, 1, hash, ahora);
        assertThat(libro.deIntento(op, 1).orElseThrow().estado()).isEqualTo(DiarioDePredicciones.Estado.SOLICITANDO);
        libro.enviada(f1, "p1", ahora);
        libro.consultada(f1, 3, ahora);
        libro.terminada(f1, DiarioDePredicciones.Estado.SUCCEEDED, "{\"ok\":1}", null, ahora);
        var e = libro.deIntento(op, 1).orElseThrow();
        assertThat(e.estado()).isEqualTo(DiarioDePredicciones.Estado.SUCCEEDED);
        assertThat(e.predictionId()).isEqualTo("p1");
        assertThat(e.output()).isEqualTo("{\"ok\":1}");
        // misma operacion e intento con otra peticion: la anterior queda DESCARTADA, sigue habiendo una vigente
        long f2 = libro.solicitar(op, u, 1, "b".repeat(64), ahora);
        assertThat(libro.deIntento(op, 1).orElseThrow().id()).isEqualTo(f2);
        libro.terminada(f2, DiarioDePredicciones.Estado.NO_CONFIRMADA, null, "corte", ahora);
        var r = libro.recuento(op);
        assertThat(r.iniciadas()).isEqualTo(2);
        assertThat(r.completadas()).isZero(); // la SUCCEEDED quedo DESCARTADA
        assertThat(r.descartadas()).isEqualTo(1);
        assertThat(r.noConfirmadas()).isEqualTo(1);
    }

    @Test
    @DisplayName("libro JDBC: la BD no admite un SUCCEEDED sin salida ni ENVIADA sin id (resultado incompleto)")
    void libroRechazaIncompletos() {
        long u = participante();
        var op = service.request(u).operation().getId();
        long f = libro.solicitar(op, u, 1, "c".repeat(64), Instant.now());
        assertThatThrownBy(() -> libro.terminada(f, DiarioDePredicciones.Estado.SUCCEEDED, null, null, Instant.now()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE plan_generation_predictions SET status = 'ENVIADA'"
                + " WHERE prediction_row_id = ?", f)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("E8/E15 · libro JDBC: reutilizable solo del MISMO usuario y MISMA peticion; adoptar deja la traza")
    void libroReutilizable() {
        long ana = participante();
        long beto = participante();
        var vieja = service.request(ana).operation().getId();
        jdbc.update("UPDATE plan_generation_operations SET status = 'FAILED', reason_code = 'TIEMPO_EXCEDIDO',"
                + " finished_at = now() WHERE operation_id = ?", vieja);
        String hash = "d".repeat(64);
        long f = libro.solicitar(vieja, ana, 1, hash, Instant.now());
        libro.enviada(f, "pAna", Instant.now());
        libro.terminada(f, DiarioDePredicciones.Estado.SUCCEEDED, "{\"ana\":1}", null, Instant.now());
        var nueva = service.request(ana).operation().getId();
        var desde = Instant.now().minusSeconds(3600);
        assertThat(libro.reutilizable(beto, hash, nueva, desde)).as("nunca de otro usuario").isEmpty();
        assertThat(libro.reutilizable(ana, "e".repeat(64), nueva, desde)).as("nunca otra peticion").isEmpty();
        var origen = libro.reutilizable(ana, hash, nueva, desde).orElseThrow();
        var adoptada = libro.adoptar(origen, nueva, 1, Instant.now());
        assertThat(adoptada.reusedFrom()).isEqualTo(f);
        assertThat(libro.deIntento(nueva, 1).orElseThrow().output()).isEqualTo("{\"ana\":1}");
        assertThat(libro.recuento(nueva).reutilizadas()).isEqualTo(1);
        assertThat(libro.recuento(nueva).iniciadas()).isZero();
    }

    // ===================================================================================== worker real (R02, R15, R20)

    private Instant leaseDe(UUID id) {
        return jdbc.queryForObject("SELECT lease_expires_at FROM plan_generation_operations WHERE operation_id = ?",
                java.sql.Timestamp.class, id).toInstant();
    }

    private static void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void esperarTerminal(java.util.Collection<UUID> ids, int segundos) {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(segundos);
        while (ids.stream().anyMatch(id -> List.of("PENDING", "PROCESSING").contains(estado(id)))) {
            if (System.nanoTime() > limite) throw new AssertionError("operaciones sin terminar: " + ids);
            dormir(50);
        }
    }

    @Test
    @DisplayName("R02 · el cierre semanal ocupa el UNICO hilo del planificador de Spring: el latido del worker (hilo"
            + " propio) sigue renovando el lease; uno en ese planificador, como en #71, no correria")
    void elCierreSemanalNoBloqueaElLatido() throws Exception {
        long u = participante();
        var id = service.request(u).operation().getId();
        var liberar = new CountDownLatch(1);
        var empezo = new CountDownLatch(1);
        doAnswer(inv -> { empezo.countDown(); esperar(liberar); return inv.callRealMethod(); })
                .when(runner).run(eq(u), any());
        // Ciclo cada 50 ms, latido cada 100 ms (en produccion 2 s y 20 s): todo escalado, la misma logica.
        var worker = new main.web.services.fitsense.planning.infrastructure.scheduling.PlanGenerationWorker(
                service, 2, 0, 50, 100, 60_000);
        worker.start();
        try {
            assertThat(empezo.await(30, TimeUnit.SECONDS)).as("el worker reclamo y ejecuta").isTrue();
            if (planificadorDeSpring instanceof org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler p)
                assertThat(p.getPoolSize()).as("Spring comparte un solo hilo entre todas las @Scheduled").isEqualTo(1);

            // El «cierre semanal» (WeeklyCycleScheduler, llamadas a la IA usuario por usuario): 3 s aqui, minutos alli.
            var cierreEmpezo = new CountDownLatch(1);
            var cierreTermino = new CountDownLatch(1);
            planificadorDeSpring.schedule(() -> { cierreEmpezo.countDown(); dormir(3000); cierreTermino.countDown(); },
                    Instant.now());
            assertThat(cierreEmpezo.await(10, TimeUnit.SECONDS)).isTrue();
            var latidoComoEn71 = new java.util.concurrent.atomic.AtomicBoolean();
            planificadorDeSpring.schedule(() -> latidoComoEn71.set(true), Instant.now());

            var antes = leaseDe(id);
            dormir(1500);
            var despues = leaseDe(id);
            assertThat(despues).as("el latido propio siguio renovando durante el cierre").isAfter(antes);
            assertThat(latidoComoEn71.get()).as("un latido en el planificador de Spring seguiria esperando")
                    .isFalse();
            assertThat(cierreTermino.await(10, TimeUnit.SECONDS)).isTrue();
            service.recoverStale();
            assertThat(estado(id)).as("viva durante el cierre: nunca se reencola").isEqualTo("PROCESSING");
        } finally {
            liberar.countDown();
            esperarTerminal(List.of(id), 60);
            worker.stop();
        }
        assertThat(estado(id)).isEqualTo("COMPLETED");
        assertThat(planes(u)).isEqualTo(1);
        assertThat(service.find(u, id).getExecutions()).isEqualTo((short) 1);
    }

    @Test
    @DisplayName("R15/R20 · diez usuarios a la vez (con doble toque) y el worker real: 10 operaciones aisladas, 10 planes,"
            + " una ejecucion cada una y nunca mas de max-concurrent (2) en paralelo")
    void variosUsuariosConElWorkerReal() throws Exception {
        int n = 10;
        var usuarios = new ArrayList<Long>();
        for (int i = 0; i < n; i++) usuarios.add(participante());
        var enCurso = new AtomicInteger();
        var maximo = new AtomicInteger();
        var llamadas = new AtomicInteger();
        doAnswer(inv -> {
            llamadas.incrementAndGet();
            maximo.accumulateAndGet(enCurso.incrementAndGet(), Math::max);
            try {
                dormir(150);
                return inv.callRealMethod();
            } finally {
                enCurso.decrementAndGet();
            }
        }).when(runner).run(any(), any());

        var hilos = Executors.newFixedThreadPool(n);
        var ids = new java.util.concurrent.ConcurrentHashMap<Long, java.util.Set<UUID>>();
        var tareas = new ArrayList<Future<?>>();
        for (long u : usuarios)
            for (int toque = 0; toque < 2; toque++)
                tareas.add(hilos.submit(() -> ids.computeIfAbsent(u, k -> ConcurrentHashMap.newKeySet())
                        .add(service.request(u).operation().getId())));
        for (var f : tareas) f.get(60, TimeUnit.SECONDS);
        hilos.shutdown();

        var worker = new main.web.services.fitsense.planning.infrastructure.scheduling.PlanGenerationWorker(
                service, 2, 0, 50, 100, 60_000);
        long t0 = System.nanoTime();
        worker.start();
        try {
            esperarTerminal(ids.values().stream().flatMap(java.util.Set::stream).toList(), 120);
        } finally {
            worker.stop();
        }
        long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);

        for (long u : usuarios) {
            assertThat(ids.get(u)).as("doble toque: la misma operacion").hasSize(1);
            var id = ids.get(u).iterator().next();
            assertThat(estado(id)).isEqualTo("COMPLETED");
            assertThat(planes(u)).isEqualTo(1);
            assertThat(service.find(u, id).getExecutions()).isEqualTo((short) 1);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM plan_generation_operations WHERE user_id = ?",
                    Integer.class, u)).isEqualTo(1);
        }
        assertThat(llamadas.get()).as("una ejecucion del pipeline por usuario").isEqualTo(n);
        assertThat(maximo.get()).as("nunca mas de max-concurrent a la vez").isBetween(1, 2);
        System.out.printf("R20 · %d operaciones terminadas en %d ms con 2 en paralelo (maximo observado %d)%n",
                n, ms, maximo.get());
    }

    @Test
    @DisplayName("R17 · libro JDBC: dos ejecuciones anotan el MISMO intento con la MISMA peticion: la segunda no puede"
            + " (ux_pgp_intento_vigente) y no se descarta la primera")
    void libroNoAdmiteDosVigentesDeLaMismaPeticion() {
        long u = participante();
        var op = service.request(u).operation().getId();
        String hash = "f".repeat(64);
        long f1 = libro.solicitar(op, u, 1, hash, Instant.now());
        assertThatThrownBy(() -> libro.solicitar(op, u, 1, hash, Instant.now()))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
        var vigente = libro.deIntento(op, 1).orElseThrow();
        assertThat(vigente.id()).isEqualTo(f1);
        assertThat(vigente.estado()).isEqualTo(DiarioDePredicciones.Estado.SOLICITANDO);
        assertThat(libro.recuento(op).iniciadas()).isEqualTo(1);
    }
}
