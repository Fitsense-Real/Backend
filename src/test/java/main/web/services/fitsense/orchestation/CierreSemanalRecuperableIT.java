package main.web.services.fitsense.orchestation;

import main.web.services.fitsense.RelojPruebas;
import main.web.services.fitsense.iam.interfaces.acl.IamContextFacade;
import main.web.services.fitsense.orchestration.application.internal.WeeklyCycleService;
import main.web.services.fitsense.orchestration.infrastructure.scheduling.CiclosSemanalesPendientes;
import main.web.services.fitsense.orchestration.infrastructure.scheduling.WeeklyCycleScheduler;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.*;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * B3 · P0-05: el cierre semanal se RECUPERA aunque el tick del lunes 03:00 se pierda, sin publicar dos veces. El
 * planificador real se construye con un reloj fijo (la hora del tick) y los servicios reales: PostgreSQL de
 * Testcontainers, IA apagada (motor de reglas), sin llamadas a Replicate.
 * <p>
 * Semana medida: {@link RelojPruebas#SEMANA_REFERENCIA} (S1); semana nueva: S2. Cada prueba usa zonas propias para no
 * pisarse (el planificador recorre todas las zonas con cuentas activas).
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.adaptation.progression.enabled=false"
})
@DisplayName("P0-05 · cierre semanal recuperable: tick perdido, zonas, ventana, reintentos acotados y concurrencia")
class CierreSemanalRecuperableIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    /** El reloj de los entrenamientos lo controla la prueba. */
    @MockitoBean main.web.services.fitsense.planning.infrastructure.scheduling.OverdueWorkoutScheduler vencidas;

    @Autowired IamContextFacade iam;
    @Autowired WeeklyCycleService ciclo;
    @Autowired CiclosSemanalesPendientes pendientes;
    @Autowired PlanningContextFacade planning;
    @Autowired UserProfileCommandService perfiles;
    @Autowired JdbcTemplate jdbc;

    static final LocalDate S1 = RelojPruebas.SEMANA_REFERENCIA;
    static final LocalDate S2 = S1.plusWeeks(1);

    @BeforeEach
    void soloEstaPrueba() {
        // Las cuentas de otras pruebas de esta clase quedan fuera del planificador (otra zona inactiva).
        jdbc.update("UPDATE users SET status = 'INACTIVE' WHERE email LIKE 'cierre-b3-%'");
    }

    /** Reloj que la prueba mueve: un mismo planificador recorre varios ticks (como el bean unico de produccion). */
    static final class RelojMovil extends Clock {
        volatile Instant ahora;
        RelojMovil(Instant ahora) { this.ahora = ahora; }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(ahora, zone); }
        @Override public Instant instant() { return ahora; }
    }

    /** Un planificador real con el reloj en {@code instante}. */
    WeeklyCycleScheduler planificador(Instant instante, WeeklyCycleService servicio) {
        return new WeeklyCycleScheduler(iam, servicio, pendientes, 24, 3,
                Optional.of(Clock.fixed(instante, ZoneOffset.UTC)));
    }

    static Instant lunes(LocalDate lunes, int hora, int minuto, String zona) {
        return lunes.atTime(hora, minuto).atZone(ZoneId.of(zona)).toInstant();
    }

    long participante(String zona, boolean conPerfil) {
        String email = "cierre-b3-" + UUID.randomUUID() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, timezone, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Sintetico', ?, now(), now())", email, zona);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        if (conPerfil)
            perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 3, 10), BiologicalSex.FEMALE,
                    new BigDecimal("164"), new BigDecimal("63"), null, FitnessLevel.BEGINNER, GoalType.GENERAL_FITNESS,
                    "Sintetico", TrainingLocation.HOME, (short) 3, List.of((short) 1, (short) 3, (short) 5),
                    (short) 45, List.of("body weight"), List.of(), null, true, true, true, false, false));
        return u;
    }

    /** Participante con el plan de S1 publicado (la semana que se medira). */
    long conPlanS1(String zona) {
        long u = participante(zona, true);
        planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        return u;
    }

    int activos(long u, LocalDate semana) {
        return jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ? AND week_start_date = ?"
                + " AND status = 'ACTIVE'", Integer.class, u, semana);
    }

    int versiones(long u, LocalDate semana) {
        return jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ? AND week_start_date = ?",
                Integer.class, u, semana);
    }

    int mediciones(long u, LocalDate semana) {
        return jdbc.queryForObject("SELECT count(*) FROM weekly_user_metrics WHERE user_id = ? AND week_start_date = ?",
                Integer.class, u, semana);
    }

    @Test
    @DisplayName("a las 03:00 locales se cierra como siempre: medicion de S1 y plan de S2")
    void ticknormal() {
        long a = conPlanS1("America/Lima");
        planificador(lunes(S2, 3, 0, "America/Lima"), ciclo).runHourly();
        assertEquals(1, mediciones(a, S1));
        assertEquals(1, activos(a, S2));
    }

    @Test
    @DisplayName("tick de las 03:00 perdido (reinicio a las 02:59): el de las 04:00 lo recupera; el de las 05:00 no repite")
    void tickPerdido() {
        long a = conPlanS1("America/Guayaquil");
        planificador(lunes(S2, 2, 0, "America/Guayaquil"), ciclo).runHourly();
        assertEquals(0, mediciones(a, S1), "premisa: a las 02:00 todavia no toca");

        planificador(lunes(S2, 4, 0, "America/Guayaquil"), ciclo).runHourly();
        assertEquals(1, mediciones(a, S1));
        assertEquals(1, activos(a, S2));
        int antes = versiones(a, S2);

        planificador(lunes(S2, 5, 0, "America/Guayaquil"), ciclo).runHourly();
        assertEquals(antes, versiones(a, S2), "un cierre completo no se vuelve a ejecutar");
        assertEquals(1, activos(a, S2));
    }

    @Test
    @DisplayName("cohorte lenta: dos zonas con la misma diferencia horaria evaluadas a las 04:40 se cierran las dos")
    void dosZonasMismaHora() {
        long lima = conPlanS1("America/Panama");
        long bogota = conPlanS1("America/Cancun");   // UTC-5 todo el ano, como Panama
        planificador(lunes(S2, 4, 40, "America/Panama"), ciclo).runHourly();
        assertEquals(1, activos(lima, S2));
        assertEquals(1, activos(bogota, S2));
    }

    @Test
    @DisplayName("zonas distintas: cada una se cierra a su hora local, no a la del servidor")
    void zonasDistintas() {
        long madrid = conPlanS1("Europe/Madrid");
        long tokio = conPlanS1("Asia/Tokyo");
        // 03:00 en Tokio = domingo 19:00 en Madrid: solo Tokio.
        planificador(lunes(S2, 3, 0, "Asia/Tokyo"), ciclo).runHourly();
        assertEquals(1, activos(tokio, S2));
        assertEquals(0, mediciones(madrid, S1), "en Madrid todavia es domingo");
        planificador(lunes(S2, 3, 0, "Europe/Madrid"), ciclo).runHourly();
        assertEquals(1, activos(madrid, S2));
    }

    @Test
    @DisplayName("fuera de la ventana (martes 04:00, mas de 24 h) no se publica una semana tarde")
    void fueraDeLaVentana() {
        long a = conPlanS1("America/La_Paz");
        planificador(S2.plusDays(1).atTime(4, 0).atZone(ZoneId.of("America/La_Paz")).toInstant(), ciclo).runHourly();
        assertEquals(0, mediciones(a, S1));
        assertEquals(0, activos(a, S2));
    }

    @Test
    @DisplayName("un participante cuyo cierre falla siempre (sin perfil) se recupera como mucho 3 veces por semana")
    void reintentosAcotados() {
        long sinPerfil = participante("America/Caracas", false);
        var espia = Mockito.spy(ciclo);
        var reloj = new RelojMovil(lunes(S2, 4, 0, "America/Caracas"));
        var unico = new WeeklyCycleScheduler(iam, espia, pendientes, 24, 3, Optional.of(reloj));
        for (int h = 4; h <= 12; h++) {
            reloj.ahora = lunes(S2, h, 0, "America/Caracas");
            unico.runHourly();
        }
        verify(espia, times(3)).ejecutar(eq(sinPerfil), any(), any());
    }

    @Test
    @DisplayName("la semana nueva ya tenia plan y falta la medicion: se completa sin un segundo plan activo")
    void yaTeniaPlan() {
        long a = conPlanS1("America/Asuncion");
        planning.generateWeeklyPlan(a, S2, PlanAdjustment.none(), null, null).orElseThrow();
        planificador(lunes(S2, 6, 0, "America/Asuncion"), ciclo).runHourly();
        assertEquals(1, mediciones(a, S1));
        assertEquals(1, activos(a, S2));
    }

    @Test
    @DisplayName("dos instancias recuperan a la vez el mismo cierre: una medicion y un solo plan activo")
    void dosInstancias() throws Exception {
        long a = conPlanS1("America/Montevideo");
        var hora = lunes(S2, 7, 0, "America/Montevideo");
        var salida = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        for (int i = 0; i < 2; i++)
            pool.submit(() -> {
                salida.await();
                planificador(hora, ciclo).runHourly();
                return null;
            });
        salida.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(3, TimeUnit.MINUTES));
        // La que llego segunda pudo encontrarlo EN_CURSO: el siguiente tick termina lo que falte.
        planificador(lunes(S2, 8, 0, "America/Montevideo"), ciclo).runHourly();
        assertEquals(1, mediciones(a, S1));
        assertEquals(1, activos(a, S2));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ?"
                + " AND week_start_date = ? AND plan_version = 1", Integer.class, a, S2), "ninguna version duplicada");
    }
}
