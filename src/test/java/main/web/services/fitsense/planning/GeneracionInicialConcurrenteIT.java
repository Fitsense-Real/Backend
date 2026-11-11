package main.web.services.fitsense.planning;

import main.web.services.fitsense.planning.application.internal.commandservices.GeneracionInicialService;
import main.web.services.fitsense.planning.domain.exceptions.AiProviderUnavailableException;
import main.web.services.fitsense.planning.domain.exceptions.GeneracionEnCursoException;
import main.web.services.fitsense.planning.domain.exceptions.PlanAlreadyExistsException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateTrainingPlanGenerator;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * R-5 · DOS PETICIONES DEL PRIMER PLAN A LA VEZ NO PAGAN DOS VECES (resiliencia de Replicate, 11/11/2026).
 * <p>
 * Con PostgreSQL real y el bloqueo consultivo real: la primera peticion queda dentro de la llamada a la IA (simulada
 * y retenida con un cerrojo); la segunda, en otro hilo, recibe 409 al instante y no llama a la IA. Despues: una
 * peticion repetida no paga nada (ya hay plan) y, si el proceso que tenia el bloqueo muere (se cierra su conexion),
 * la siguiente peticion se atiende.
 * <p>
 * La IA es un doble: ninguna llamada a Replicate.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=true",
        "fitsense.ai.api-token=token-de-prueba-sin-uso"
})
@DisplayName("R-5 · generacion inicial concurrente: una sola llamada a la IA, un solo plan, y sin bloqueos huerfanos")
class GeneracionInicialConcurrenteIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @MockitoBean ReplicateTrainingPlanGenerator ia;
    @Autowired GeneracionInicialService generacion;
    @Autowired UserProfileCommandService perfiles;
    @Autowired JdbcTemplate jdbc;

    static final LocalDate SEMANA = LocalDate.of(2026, 11, 2);
    static final AtomicInteger N = new AtomicInteger();

    private final AtomicInteger llamadasIa = new AtomicInteger();
    private volatile CountDownLatch dentro = new CountDownLatch(0);
    private volatile CountDownLatch soltar = new CountDownLatch(0);

    @BeforeEach
    void ia() {
        when(ia.isEnabled()).thenReturn(true);
        when(ia.requestOutput(any(), any(), anyInt())).thenAnswer(inv -> {
            llamadasIa.incrementAndGet();
            dentro.countDown();
            assertTrue(soltar.await(30, TimeUnit.SECONDS), "la prueba no solto la llamada");
            // El proveedor falla: decide el motor de reglas, como siempre.
            throw new AiProviderUnavailableException("simulado");
        });
    }

    long usuario() {
        int n = N.incrementAndGet();
        String email = "concurrente" + n + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Concurrente', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1992, 3, 3), BiologicalSex.MALE,
                new BigDecimal("178"), new BigDecimal("76"), null, FitnessLevel.BEGINNER, GoalType.GAIN_MUSCLE,
                "masa", TrainingLocation.GYM, (short) 3, List.of((short) 1, (short) 3, (short) 5), (short) 45,
                List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine", "bench"), List.of(),
                "Ninguna", true, true, true, true, true));
        return u;
    }

    private int planes(long u) {
        return jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ?", Integer.class, u);
    }

    @Test
    @DisplayName("FALLABA ANTES · dos peticiones simultaneas: la segunda recibe 409 sin llamar a la IA; 1 llamada, 1 plan")
    void dosSimultaneas() throws Exception {
        long u = usuario();
        var cmd = GenerateWeeklyPlanCommand.firstPlan(u, SEMANA);
        dentro = new CountDownLatch(1);
        soltar = new CountDownLatch(1);

        var primera = CompletableFuture.supplyAsync(() -> generacion.generar(cmd));
        assertTrue(dentro.await(30, TimeUnit.SECONDS), "la primera no llego a la IA");

        var segunda = CompletableFuture.supplyAsync(() -> generacion.generar(cmd));
        var e = assertThrows(java.util.concurrent.ExecutionException.class, () -> segunda.get(30, TimeUnit.SECONDS));
        assertInstanceOf(GeneracionEnCursoException.class, e.getCause());

        soltar.countDown();
        assertTrue(primera.get(60, TimeUnit.SECONDS).isPresent(), "la primera publica con el motor de reglas");

        assertEquals(1, llamadasIa.get(), "una sola llamada a la IA para las dos peticiones");
        assertEquals(1, planes(u), "un solo plan");
    }

    @Test
    @DisplayName("peticiones repetidas tras publicar: se rechazan antes de generar, sin llamar a la IA")
    void repetidasTrasPublicar() {
        long u = usuario();
        var cmd = GenerateWeeklyPlanCommand.firstPlan(u, SEMANA);
        assertTrue(generacion.generar(cmd).isPresent());
        int antes = llamadasIa.get();

        for (int i = 0; i < 3; i++)
            assertThrows(PlanAlreadyExistsException.class, () -> generacion.generar(cmd));

        assertEquals(antes, llamadasIa.get(), "las repetidas no cuestan ninguna llamada");
        assertEquals(1, planes(u));
    }

    @Test
    @DisplayName("el proceso que tenia el bloqueo muere: PostgreSQL lo suelta y la siguiente peticion se atiende")
    void reinicioDelProceso() throws Exception {
        long u = usuario();
        var cmd = GenerateWeeklyPlanCommand.firstPlan(u, SEMANA);
        long clave = (0x46534331L << 32) | (u & 0xFFFFFFFFL);   // la de CicloSemanalBloqueo para esta persona

        var otroProceso = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(),
                postgres.getPassword());
        try (var ps = otroProceso.prepareStatement("SELECT pg_try_advisory_lock(?)")) {
            ps.setLong(1, clave);
            try (var rs = ps.executeQuery()) {
                assertTrue(rs.next() && rs.getBoolean(1));
            }
        }
        assertThrows(GeneracionEnCursoException.class, () -> generacion.generar(cmd),
                "mientras el otro proceso vive, su generacion sigue en curso");
        assertEquals(0, llamadasIa.get());

        otroProceso.close();   // el proceso muere: su sesion se cierra y el bloqueo se libera

        assertTrue(generacion.generar(cmd).isPresent());
        assertEquals(1, planes(u));
    }
}
