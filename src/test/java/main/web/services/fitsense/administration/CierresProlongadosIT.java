package main.web.services.fitsense.administration;

import com.fasterxml.jackson.databind.ObjectMapper;
import main.web.services.fitsense.administration.application.internal.operations.IndicadoresOperativosService;
import main.web.services.fitsense.administration.application.internal.operations.VigilanteOperativo;
import main.web.services.fitsense.analytics.interfaces.acl.AnalyticsContextFacade;
import main.web.services.fitsense.iam.infrastructure.tokens.jwt.services.TokenServiceImpl;
import main.web.services.fitsense.iam.interfaces.acl.IamContextFacade;
import main.web.services.fitsense.orchestration.application.internal.WeeklyCycleService;
import main.web.services.fitsense.orchestration.infrastructure.scheduling.CiclosSemanalesPendientes;
import main.web.services.fitsense.orchestration.infrastructure.scheduling.WeeklyCycleScheduler;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.*;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.RelojPruebas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * C3 · INTERRUPCIONES DE MAS DE 24 HORAS. La recuperacion automatica de B3 (y la de V66) cubre la ventana de
 * {@code catch-up-hours}; pasada esa ventana, a proposito, NO se cierra ni se publica tarde (cambiaria la semantica
 * temporal del estudio). Lo que se anade es que esas semanas no queden en silencio:
 * <ul>
 *   <li>se identifican todas las semanas sin cerrar de las ultimas 13, no solo la anterior;</li>
 *   <li>se distingue EN_VENTANA (se recupera sola) de FUERA_DE_VENTANA (necesita la decision del equipo);</li>
 *   <li>alerta en ERROR, lista seudonimizada para ADMIN y auditada; nada de esto escribe.</li>
 * </ul>
 * PostgreSQL de Testcontainers, IA apagada, usuarios sinteticos.
 */
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(properties = {
        "authorization.jwt.secret=" + CierresProlongadosIT.SECRETO,
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.adaptation.progression.enabled=false",
        "fitsense.operations.alerts.enabled=false"
})
@DisplayName("C3 · cierres semanales de mas de 24 h: identificados, alertados, sin cierres tardios ni duplicados")
class CierresProlongadosIT {

    static final String SECRETO = "clave-solo-para-pruebas-de-integracion-0123456789abcdef";
    static final String ZONA = "America/Lima";

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @MockitoBean main.web.services.fitsense.planning.infrastructure.scheduling.OverdueWorkoutScheduler vencidas;

    @Autowired WebApplicationContext contexto;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserProfileCommandService perfiles;
    @Autowired PlanningContextFacade planning;
    @Autowired AnalyticsContextFacade analitica;
    @Autowired IndicadoresOperativosService indicadores;
    @Autowired VigilanteOperativo vigilante;
    @Autowired IamContextFacade iam;
    @Autowired WeeklyCycleService ciclo;
    @Autowired CiclosSemanalesPendientes pendientes;
    MockMvc http;

    static final ObjectMapper JSON = new ObjectMapper();

    /** Semanas relativas a HOY (la deteccion usa el reloj de la base, como en produccion). */
    static final LocalDate LUNES_ACTUAL = LocalDate.now(ZoneId.of(ZONA)).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    static final LocalDate W1 = LUNES_ACTUAL.minusWeeks(1);
    static final LocalDate W2 = LUNES_ACTUAL.minusWeeks(2);

    record Cuenta(long id, String codigo, String email) {}

    @BeforeEach
    void preparar() {
        http = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        // Las cuentas de otras pruebas de la clase quedan fuera (la deteccion recorre todos los participantes).
        jdbc.update("UPDATE users SET status = 'INACTIVE' WHERE email LIKE 'c3-%'");
    }

    Cuenta participante(String rol, boolean participa) {
        String email = "c3-" + UUID.randomUUID() + "@pruebas.invalid";
        String codigo = participa ? "P-C3-" + UUID.randomUUID().toString().substring(0, 6) : null;
        jdbc.update("INSERT INTO users (email, password_hash, first_name, role, timezone, is_study_participant,"
                + " participant_code, consent_granted_at, enrolled_at) VALUES (?, 'x', 'Sintetico', ?, ?, ?, ?, now(),"
                + " now())", email, rol, ZONA, participa, codigo);
        long id = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        if (participa)
            perfiles.handle(new CreateUserProfileCommand(id, LocalDate.of(1990, 3, 10), BiologicalSex.FEMALE,
                    new BigDecimal("164"), new BigDecimal("63"), null, FitnessLevel.BEGINNER, GoalType.GENERAL_FITNESS,
                    "Sintetico", TrainingLocation.HOME, (short) 3, List.of((short) 1, (short) 3, (short) 5),
                    (short) 45, List.of("body weight"), List.of(), null, true, true, true, false, false));
        return new Cuenta(id, codigo, email);
    }

    void plan(Cuenta c, LocalDate semana) {
        planning.generateWeeklyPlan(c.id(), semana, PlanAdjustment.none(), null, null).orElseThrow();
    }

    /** Lo que debe decir la deteccion de W1 segun la hora real (puede ser lunes de madrugada al correr la prueba). */
    static Optional<String> estadoEsperadoW1() {
        var ahora = ZonedDateTime.now(ZoneId.of(ZONA));
        var cierre = LUNES_ACTUAL.atTime(3, 0).atZone(ZoneId.of(ZONA));
        if (!ahora.isAfter(cierre)) return Optional.empty();
        return Optional.of(Duration.between(cierre, ahora).toHours() < 24 ? IndicadoresOperativosService.EN_VENTANA
                : IndicadoresOperativosService.FUERA_DE_VENTANA);
    }

    List<IndicadoresOperativosService.CierreAtrasado> de(Cuenta c) {
        return indicadores.cierresAtrasados().stream().filter(x -> c.codigo().equals(x.participantCode())).toList();
    }

    long filas(String tabla) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tabla, Long.class);
    }

    @Test
    @DisplayName("interrupcion de dos semanas: las dos semanas sin cerrar aparecen; la antigua, FUERA_DE_VENTANA")
    void dosSemanasSinCerrar() {
        var c = participante("USER", true);
        plan(c, W2);
        plan(c, W1);
        var lista = de(c);
        var w2 = lista.stream().filter(x -> x.measuredWeekStart().equals(W2)).findFirst().orElseThrow();
        assertEquals(IndicadoresOperativosService.FUERA_DE_VENTANA, w2.status());
        assertTrue(w2.hoursSinceDue() >= 7 * 24, "su cierre fue hace mas de una semana");
        assertFalse(w2.newWeekWithoutPlan(), "W1 si tuvo plan");
        assertEquals(estadoEsperadoW1(), lista.stream().filter(x -> x.measuredWeekStart().equals(W1)).findFirst()
                .map(IndicadoresOperativosService.CierreAtrasado::status));
        assertEquals(ZONA, w2.timezone());
        assertTrue(indicadores.calcular().cierresFueraDeVentana() >= 1);
    }

    @Test
    @DisplayName("semanas cerradas, cuentas retiradas o que no participan: no aparecen")
    void soloLoQueFaltaDeVerdad() {
        var cerrada = participante("USER", true);
        plan(cerrada, W2);
        analitica.calculateWeek(cerrada.id(), W2);
        var retirada = participante("USER", true);
        plan(retirada, W2);
        jdbc.update("UPDATE users SET withdrawn_at = now() WHERE user_id = ?", retirada.id());

        assertTrue(de(cerrada).isEmpty(), "con su medicion, la semana esta cerrada");
        assertTrue(de(retirada).isEmpty(), "tras la retirada no se esperan cierres");
        assertEquals(0, indicadores.cierresAtrasados().stream().filter(x -> x.participantCode() == null).count(),
                "nunca una cuenta que no participa");
    }

    @Test
    @DisplayName("la deteccion es solo lectura e idempotente: repetirla no cierra, no mide y no genera nada")
    void soloLectura() {
        var c = participante("USER", true);
        plan(c, W2);
        long metricas = filas("weekly_user_metrics"), planes = filas("weekly_training_plans"),
                operaciones = filas("plan_generation_operations"), intervenciones = filas("user_interventions");
        var primera = indicadores.cierresAtrasados();
        var segunda = indicadores.cierresAtrasados();
        vigilante.revisar();
        vigilante.revisar();
        assertEquals(primera, segunda);
        assertEquals(metricas, filas("weekly_user_metrics"));
        assertEquals(planes, filas("weekly_training_plans"));
        assertEquals(operaciones, filas("plan_generation_operations"));
        assertEquals(intervenciones, filas("user_interventions"));
    }

    @Test
    @DisplayName("alerta CIERRE_SEMANAL_FUERA_DE_VENTANA en ERROR, con el recuento y sin datos de la persona")
    void alerta(CapturedOutput salida) {
        var c = participante("USER", true);
        plan(c, W2);
        assertTrue(vigilante.revisar().contains("CIERRE_SEMANAL_FUERA_DE_VENTANA"));
        var linea = salida.getAll().lines().filter(l -> l.contains("CIERRE_SEMANAL_FUERA_DE_VENTANA")).findFirst()
                .orElseThrow();
        assertTrue(linea.contains("ERROR"), linea);
        assertFalse(linea.contains(c.codigo()) || linea.contains(c.email()), "solo recuentos en el log");
    }

    @Test
    @DisplayName("GET /admin/operations/weekly-closures/overdue: ADMIN ve codigos P-xxx (sin correos) y queda auditado;"
            + " USER, 403")
    void endpoint(CapturedOutput salida) throws Exception {
        var c = participante("USER", true);
        plan(c, W2);
        var admin = participante("ADMIN", false);
        var token = new TokenServiceImpl(SECRETO, 7).generateToken(admin.id(), admin.email(), 0);
        var r = http.perform(get("/api/v1/admin/operations/weekly-closures/overdue")
                .header("Authorization", "Bearer " + token)).andReturn().getResponse();
        assertEquals(200, r.getStatus(), r.getContentAsString());
        var cuerpo = JSON.readTree(r.getContentAsString());
        assertEquals(24, cuerpo.get("catchUpHours").asLong());
        assertTrue(r.getContentAsString().contains(c.codigo()));
        assertFalse(r.getContentAsString().contains("@"), "ningun correo");
        assertTrue(salida.getAll().contains("weekly-closures-overdue"), "consulta auditada");

        var usuario = new TokenServiceImpl(SECRETO, 7).generateToken(c.id(), c.email(), 0);
        assertEquals(403, http.perform(get("/api/v1/admin/operations/weekly-closures/overdue")
                .header("Authorization", "Bearer " + usuario)).andReturn().getResponse().getStatus());
    }

    @Test
    @DisplayName("el planificador, pasadas 24 h del cierre: no cierra, no mide ni publica la semana tarde (ni una vez)")
    void fueraDeVentanaNoSeRecuperaSolo() {
        LocalDate s1 = RelojPruebas.SEMANA_REFERENCIA, s2 = s1.plusWeeks(1);
        String zona = "America/Bogota";
        var c = participante("USER", true);
        jdbc.update("UPDATE users SET timezone = ? WHERE user_id = ?", zona, c.id());
        plan(c, s1);
        for (int horas : new int[]{25, 30, 48, 72}) {
            var tick = s2.atTime(3, 0).atZone(ZoneId.of(zona)).plusHours(horas).toInstant();
            new WeeklyCycleScheduler(iam, ciclo, pendientes, 24, 3, Optional.of(Clock.fixed(tick, ZoneOffset.UTC)))
                    .runHourly();
        }
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM weekly_user_metrics WHERE user_id = ?", Integer.class,
                c.id()), "sin medicion tardia");
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ? AND"
                + " week_start_date = ?", Integer.class, c.id(), s2), "sin plan publicado tarde");
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ? AND status ="
                + " 'ACTIVE'", Integer.class, c.id()), "un solo plan activo, el de S1, intacto");
    }
}
