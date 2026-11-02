package main.web.services.fitsense.orchestation;

import main.web.services.fitsense.FitsenseApplication;
import main.web.services.fitsense.execution.domain.model.commands.FinishWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.model.commands.RecordSessionExerciseCommand;
import main.web.services.fitsense.execution.domain.model.commands.StartWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.services.WorkoutSessionCommandService;
import main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import main.web.services.fitsense.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import main.web.services.fitsense.orchestration.application.internal.WeeklyCycleService;
import main.web.services.fitsense.orchestration.infrastructure.locking.CicloSemanalBloqueo;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.SessionDurationEstimator;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateSessionFixGenerator;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.planning.interfaces.rest.TrainingPlanController;
import main.web.services.fitsense.planning.interfaces.rest.resources.SkipWorkoutResource;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.domain.model.valueobjects.SkipReason;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import main.web.services.fitsense.shared.domain.model.valueobjects.TrainingWeek;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * CONCURRENCIA DEL CICLO SEMANAL: un bloqueo consultivo de sesion por
 * participante, en una conexion directa propia ({@link CicloSemanalBloqueo}).
 * <p>
 * Criterio: una sola decision procesada, un solo intento V32, una sola semana
 * publicada (ninguna version REPLACED por la carrera) y ninguna llamada a la
 * IA de mas. PostgreSQL efimero; IA simulada (con retraso para forzar el
 * solape); nunca Replicate.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("Ciclo semanal concurrente: bloqueo de sesion por participante")
class CicloConcurrenteIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @MockitoBean ReplicateSessionFixGenerator ia;

    @Autowired WeeklyCycleService ciclo;
    @Autowired PlanningContextFacade planning;
    @Autowired UserProfileCommandService perfiles;
    @Autowired WorkoutSessionCommandService sesiones;
    @Autowired TrainingPlanController planes;
    @Autowired UserRepository usuarios;
    @Autowired SessionDurationEstimator estimador;
    @Autowired JdbcTemplate jdbc;
    @Autowired main.web.services.fitsense.planning.application.internal.commandservices.AdaptationFailureRecorder registrador;

    static final AtomicInteger SECUENCIA = new AtomicInteger();
    static final LocalDate S1 = TrainingWeek.containing(LocalDate.now()).startDate().plusWeeks(1);
    static final List<String> GIMNASIO = List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine",
            "bench", "band");

    /** Retraso de la IA simulada, y si debe fallar. */
    final AtomicLong retrasoMs = new AtomicLong();
    volatile Throwable fallo;

    static ConfigurableApplicationContext segunda;

    @BeforeEach
    void iaSimulada() {
        reset(ia);
        retrasoMs.set(0);
        fallo = null;
        when(ia.isEnabled()).thenReturn(true);
        when(ia.modelName()).thenReturn("simulado");
        when(ia.pedirRespuesta(any(), any(), any(), any(), anyInt())).thenAnswer(inv -> {
            Thread.sleep(retrasoMs.get());
            if (fallo instanceof RuntimeException r) throw r;
            if (fallo instanceof Error e) throw e;
            return new ReplicateSessionFixGenerator.RawFix("{}", "salida-simulada");
        });
        when(ia.interpretar(anyString(), any())).thenAnswer(inv -> reducir(inv.getArgument(1)));
    }

    @AfterAll
    static void cerrarSegunda() {
        if (segunda != null) segunda.close();
    }

    // =======================================================================

    @Test
    @DisplayName("misma persona y semana a la vez, con la IA lenta: la segunda no hace nada (EN_CURSO), 1 llamada, 1 V32, 1 semana")
    void mismaPersonaIaLenta() throws Exception {
        long u = conFatiga();
        retrasoMs.set(3000);
        var pool = Executors.newFixedThreadPool(2);
        var primera = pool.submit(() -> ciclo.ejecutar(u, S1, S1.plusWeeks(1)));
        esperarBloqueo(u);
        clearInvocations(ia);
        var segundaLlamada = pool.submit(() -> ciclo.ejecutar(u, S1, S1.plusWeeks(1))).get(10, TimeUnit.SECONDS);
        assertEquals(CicloSemanalBloqueo.Estado.EN_CURSO, segundaLlamada.estado());
        assertTrue(segundaLlamada.planId().isEmpty(), "EN_CURSO no se presenta como publicado");
        var r1 = primera.get(60, TimeUnit.SECONDS);
        pool.shutdown();
        assertEquals(CicloSemanalBloqueo.Estado.EJECUTADO, r1.estado());
        assertEquals(1, llamadasIa(), "solo la primera llamo a la IA");
        comprobarUnico(u, "APLICADA");

        // Reintento posterior: el mismo plan, sin IA, sin evidencia nueva.
        clearInvocations(ia);
        var reintento = ciclo.ejecutar(u, S1, S1.plusWeeks(1));
        assertEquals(CicloSemanalBloqueo.Estado.EJECUTADO, reintento.estado());
        assertEquals(r1.planId(), reintento.planId());
        assertEquals(0, llamadasIa());
        comprobarUnico(u, "APLICADA");
        assertSinBloqueo(u);
    }

    @Test
    @DisplayName("sin IA (copia): cinco carreras simultaneas -> siempre 1 V32 y 1 semana, ninguna version reemplazada")
    void carrerasSinIa() throws Exception {
        for (int i = 0; i < 5; i++) {
            long u = cumplida();
            var listos = new CountDownLatch(1);
            var pool = Executors.newFixedThreadPool(2);
            var futuros = new ArrayList<Future<WeeklyCycleService.ResultadoCiclo>>();
            for (int t = 0; t < 2; t++)
                futuros.add(pool.submit(() -> {
                    listos.await();
                    return ciclo.ejecutar(u, S1, S1.plusWeeks(1));
                }));
            listos.countDown();
            var estados = new ArrayList<CicloSemanalBloqueo.Estado>();
            for (var f : futuros) estados.add(f.get(60, TimeUnit.SECONDS).estado());
            pool.shutdown();
            System.out.println("CONCURRENTE carrera " + i + " " + estados);
            assertTrue(estados.contains(CicloSemanalBloqueo.Estado.EJECUTADO), String.valueOf(estados));
            comprobarUnico(u, "SIN_ORDEN");
            assertSinBloqueo(u);
        }
        assertEquals(0, llamadasIa());
    }

    @Test
    @DisplayName("dos INSTANCIAS de la aplicacion sobre la misma base: la segunda ve EN_CURSO; despues devuelve el mismo plan")
    void dosInstancias() throws Exception {
        var otra = segundaInstancia().getBean(WeeklyCycleService.class);
        assertNotSame(ciclo, otra);
        long u = conFatiga();
        retrasoMs.set(3000);
        var pool = Executors.newSingleThreadExecutor();
        var primera = pool.submit(() -> ciclo.ejecutar(u, S1, S1.plusWeeks(1)));
        esperarBloqueo(u);
        var r2 = otra.ejecutar(u, S1, S1.plusWeeks(1));
        assertEquals(CicloSemanalBloqueo.Estado.EN_CURSO, r2.estado(), "la otra instancia no entra");
        var r1 = primera.get(60, TimeUnit.SECONDS);
        pool.shutdown();
        assertEquals(CicloSemanalBloqueo.Estado.EJECUTADO, r1.estado());
        comprobarUnico(u, "APLICADA");

        var r3 = otra.ejecutar(u, S1, S1.plusWeeks(1));
        assertEquals(CicloSemanalBloqueo.Estado.EJECUTADO, r3.estado());
        assertEquals(r1.planId(), r3.planId(), "la otra instancia devuelve el plan ya publicado");
        comprobarUnico(u, "APLICADA");
        assertSinBloqueo(u);
    }

    @Test
    @DisplayName("fallo durante el ciclo: el bloqueo se libera y un reintento procesa la semana una sola vez")
    void falloLiberaBloqueo() throws Exception {
        long u = conFatiga();
        // 1. Un fallo que el ciclo captura (la IA lanza): no se publica nada, el bloqueo se suelta.
        fallo = new IllegalStateException("caida simulada");
        var r = ciclo.ejecutar(u, S1, S1.plusWeeks(1));
        assertEquals(CicloSemanalBloqueo.Estado.EJECUTADO, r.estado());
        assertTrue(r.planId().isEmpty());
        assertSinBloqueo(u);
        // 2. Un fallo que atraviesa el ciclo (Error): sale la excepcion y el bloqueo se suelta igual.
        fallo = new AssertionError("fallo no capturado simulado");
        assertThrows(AssertionError.class, () -> ciclo.ejecutar(u, S1, S1.plusWeeks(1)));
        assertSinBloqueo(u);
        // 3. Reintento con la IA sana: una sola semana y una sola evidencia publicada.
        fallo = null;
        var ok = ciclo.ejecutar(u, S1, S1.plusWeeks(1));
        assertEquals(CicloSemanalBloqueo.Estado.EJECUTADO, ok.estado());
        assertTrue(ok.planId().isPresent());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM plan_adaptation_attempts WHERE user_id = ?"
                + " AND resulting_plan_id IS NOT NULL", Integer.class, u));
        assertEquals(1, versiones(u));
        assertSinBloqueo(u);
    }

    @Test
    @DisplayName("la conexion del bloqueo se cae a mitad del ciclo: el ciclo termina sin excepcion y el bloqueo queda libre")
    void conexionDelBloqueoPerdida() throws Exception {
        long u = conFatiga();
        retrasoMs.set(3000);
        var pool = Executors.newSingleThreadExecutor();
        var primera = pool.submit(() -> ciclo.ejecutar(u, S1, S1.plusWeeks(1)));
        esperarBloqueo(u);
        int cortadas = jdbc.queryForObject("SELECT count(pg_terminate_backend(pid)) FROM pg_stat_activity"
                + " WHERE application_name = 'fitsense-ciclo-semanal-bloqueo'", Integer.class);
        assertEquals(1, cortadas);
        var r = primera.get(60, TimeUnit.SECONDS);
        pool.shutdown();
        assertEquals(CicloSemanalBloqueo.Estado.EJECUTADO, r.estado());
        assertTrue(r.planId().isPresent());
        comprobarUnico(u, "APLICADA");
        assertSinBloqueo(u);
    }

    @Test
    @DisplayName("se pierde el bloqueo de A MIENTRAS ESPERA A LA IA y entra B: una decision, una evidencia, una semana")
    void bloqueoPerdidoDuranteLaIa() throws Exception {
        bloqueoPerdido(true);
    }

    @Test
    @DisplayName("se pierde el bloqueo de A AL EMPEZAR (aun decidiendo) y entra B: una decision, una evidencia, una semana")
    void bloqueoPerdidoAlEmpezar() throws Exception {
        bloqueoPerdido(false);
    }

    private void bloqueoPerdido(boolean esperarIa) throws Exception {
        long u = conFatiga();
        retrasoMs.set(4000);
        clearInvocations(ia);
        var pool = Executors.newFixedThreadPool(2);
        var a = pool.submit(() -> ciclo.ejecutar(u, S1, S1.plusWeeks(1)));
        esperarBloqueo(u);
        if (esperarIa) esperarLlamadaIa();
        assertEquals(1, (int) jdbc.queryForObject("SELECT count(pg_terminate_backend(pid)) FROM pg_stat_activity"
                + " WHERE application_name = 'fitsense-ciclo-semanal-bloqueo'", Integer.class));
        assertSinBloqueo(u);
        var b = pool.submit(() -> ciclo.ejecutar(u, S1, S1.plusWeeks(1)));   // B aprovecha el bloqueo libre
        var ra = resultado(a);
        var rb = resultado(b);
        pool.shutdown();
        var intentos = jdbc.queryForList("SELECT status || '/' || coalesce(resulting_plan_id::text, '-')"
                + " FROM plan_adaptation_attempts WHERE user_id = ? ORDER BY attempt_id", String.class, u);
        System.out.println("CONCURRENTE bloqueo_perdido(" + (esperarIa ? "durante la IA" : "al empezar") + ") A=" + ra
                + " B=" + rb + " intentos=" + intentos + " versiones=" + versiones(u) + " llamadas=" + llamadasIa());
        assertEquals(List.of("APLICADA"), intentos.stream().map(x -> x.substring(0, x.indexOf('/'))).toList(),
                "una sola evidencia V32: " + intentos);
        assertEquals(1, versiones(u), "una sola version de la semana (ninguna REPLACED)");
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM user_interventions WHERE user_id = ?",
                Integer.class, u), "una sola decision");
        long publicado = jdbc.queryForObject("SELECT resulting_plan_id FROM plan_adaptation_attempts WHERE user_id = ?",
                Long.class, u);
        for (var r : List.of(ra, rb))
            if (r instanceof WeeklyCycleService.ResultadoCiclo rc && rc.planId().isPresent())
                assertEquals(publicado, rc.planId().get(), "nadie devuelve un plan distinto del publicado");
        assertSinBloqueo(u);
    }

    @Test
    @DisplayName("un fallo TARDIO de la ejecucion que perdio el bloqueo no deja evidencia si otra ya publico")
    void falloTardioNoSeRegistra() {
        long u = conFatiga();
        retrasoMs.set(0);
        assertEquals(CicloSemanalBloqueo.Estado.EJECUTADO, ciclo.ejecutar(u, S1, S1.plusWeeks(1)).estado());
        comprobarUnico(u, "APLICADA");
        var fila = jdbc.queryForMap("SELECT weekly_metric_id, source_plan_id, new_week_start_date, decision,"
                + " targeted_sessions[1] AS senalada, targeted_interventions[1] AS intervencion"
                + " FROM plan_adaptation_attempts WHERE user_id = ?", u);
        long senalada = ((Number) fila.get("senalada")).longValue();
        // Lo que registraria la otra ejecucion si su IA acabara en NO_FACTIBLE despues.
        var tardio = new AdaptationAttempt(u, ((Number) fila.get("weekly_metric_id")).longValue(),
                ((Number) fila.get("source_plan_id")).longValue(),
                ((java.sql.Date) fila.get("new_week_start_date")).toLocalDate(), null,
                AdaptationDecision.valueOf((String) fila.get("decision")), AdaptationAttempt.Status.NO_FACTIBLE,
                null, (short) 1, List.of(senalada), List.of((String) fila.get("intervencion")), List.of(),
                List.of(), List.of(), "{}", "fallo tardio simulado", null, null);
        assertFalse(registrador.registrar(tardio), "no se registra: la medicion ya publico");
        comprobarUnico(u, "APLICADA");
    }

    /** El resultado, o la excepcion (que tambien es un desenlace aceptable si no publica nada). */
    private Object resultado(Future<WeeklyCycleService.ResultadoCiclo> f) throws Exception {
        try {
            return f.get(60, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            return "EXCEPCION " + e.getCause().getClass().getSimpleName();
        }
    }

    private void esperarLlamadaIa() throws InterruptedException {
        for (int i = 0; i < 400; i++) {
            if (llamadasIa() > 0) return;
            Thread.sleep(25);
        }
        fail("la IA no llego a llamarse");
    }

    @Test
    @DisplayName("personas distintas a la vez: no se bloquean entre si")
    void personasDistintas() throws Exception {
        long a = conFatiga(), b = conFatiga();
        retrasoMs.set(2500);
        var pool = Executors.newFixedThreadPool(2);
        var inicio = System.nanoTime();
        var fa = pool.submit(() -> ciclo.ejecutar(a, S1, S1.plusWeeks(1)));
        var fb = pool.submit(() -> ciclo.ejecutar(b, S1, S1.plusWeeks(1)));
        var ra = fa.get(60, TimeUnit.SECONDS);
        var rb = fb.get(60, TimeUnit.SECONDS);
        long ms = (System.nanoTime() - inicio) / 1_000_000;
        pool.shutdown();
        System.out.println("CONCURRENTE personas distintas en " + ms + " ms");
        assertEquals(CicloSemanalBloqueo.Estado.EJECUTADO, ra.estado());
        assertEquals(CicloSemanalBloqueo.Estado.EJECUTADO, rb.estado());
        assertTrue(ms < 2 * 2500 + 2000, "corrieron en paralelo, no en fila: " + ms + " ms");
        comprobarUnico(a, "APLICADA");
        comprobarUnico(b, "APLICADA");
    }

    // =======================================================================

    private ConfigurableApplicationContext segundaInstancia() {
        if (segunda == null)
            segunda = new SpringApplicationBuilder(FitsenseApplication.class)
                    .web(WebApplicationType.SERVLET)
                    // Como argumentos: el perfil de integracion fija spring.datasource.url como
                    // propiedad de sistema (base bloqueada), y solo la linea de comandos la supera.
                    .run("--spring.datasource.url=" + postgres.getJdbcUrl(),
                            "--spring.datasource.username=" + postgres.getUsername(),
                            "--spring.datasource.password=" + postgres.getPassword(),
                            "--spring.flyway.enabled=false",
                            "--authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
                            "--fitsense.ai.enabled=false",
                            "--fitsense.ai.api-token=",
                            "--spring.devtools.restart.enabled=false",
                            "--server.port=0");
        return segunda;
    }

    /** Una sola decision, un solo intento V32 con plan, una sola version de la semana nueva. */
    private void comprobarUnico(long u, String estado) {
        var intentos = jdbc.queryForList("SELECT status FROM plan_adaptation_attempts WHERE user_id = ?",
                String.class, u);
        assertEquals(List.of(estado), intentos, "un solo intento V32");
        assertEquals(1, versiones(u), "una sola version de la semana nueva (ninguna REPLACED)");
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM user_interventions WHERE user_id = ?",
                Integer.class, u), "una sola decision");
    }

    private int versiones(long u) {
        return jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ? AND week_start_date = ?",
                Integer.class, u, S1.plusWeeks(1));
    }

    private void esperarBloqueo(long u) throws InterruptedException {
        for (int i = 0; i < 200; i++) {
            if (bloqueos(u) > 0) return;
            Thread.sleep(25);
        }
        fail("el primer ciclo no tomo el bloqueo");
    }

    private void assertSinBloqueo(long u) throws InterruptedException {
        for (int i = 0; i < 40 && bloqueos(u) > 0; i++) Thread.sleep(25);
        assertEquals(0, bloqueos(u), "el bloqueo quedo tomado");
    }

    /** pg_locks: un bloqueo de bigint se ve como classid = 32 bits altos, objid = 32 bajos, objsubid = 1. */
    private int bloqueos(long u) {
        return jdbc.queryForObject("SELECT count(*) FROM pg_locks WHERE locktype = 'advisory' AND objsubid = 1"
                + " AND classid::bigint = ? AND objid::bigint = ?", Integer.class, 0x46534331L, u);
    }

    private int llamadasIa() {
        return (int) Mockito.mockingDetails(ia).getInvocations().stream()
                .filter(i -> i.getMethod().getName().equals("pedirRespuesta")).count();
    }

    /** Una semana con el viernes omitido por fatiga: el ciclo ordena adaptar (llama a la IA). */
    private long conFatiga() {
        long u = participante();
        long w1 = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        hacer(u, w1, true);
        return u;
    }

    /** Una semana cumplida: el ciclo copia sin IA. */
    private long cumplida() {
        long u = participante();
        long w1 = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        hacer(u, w1, false);
        return u;
    }

    private List<CorrectedSession> reducir(PlanGenerationContext ctx) {
        var salida = new ArrayList<CorrectedSession>();
        for (var senalada : ctx.adjustment().targetedSessions()) {
            if (senalada.intervencion() != TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION) continue;
            long id = senalada.plannedWorkoutId();
            var f = jdbc.queryForMap("SELECT scheduled_date, focus_code FROM planned_workouts WHERE planned_workout_id = ?", id);
            var lista = new ArrayList<>(jdbc.query("SELECT exercise_id, prescription_type, planned_sets, planned_reps,"
                            + " planned_duration_seconds, rest_seconds FROM planned_workout_exercises"
                            + " WHERE planned_workout_id = ? ORDER BY exercise_order",
                    (rs, k) -> new PlanDraft.DraftExercise(rs.getLong(1), PrescriptionType.valueOf(rs.getString(2)),
                            rs.getObject(3, Short.class), rs.getObject(4, Short.class), rs.getObject(5, Integer.class),
                            null, rs.getObject(6, Short.class), null), id));
            var limites = ctx.prescription() == null ? null : ctx.prescription().repLimits();
            for (int k = 0; k < lista.size(); k++) {
                var e = lista.get(k);
                if (e.prescriptionType() != PrescriptionType.SETS_REPS || e.plannedReps() == null) continue;
                var c = ctx.availableExercises().stream().filter(x -> x.exerciseId().equals(e.exerciseId())).findFirst();
                int minimo = limites == null || c.isEmpty() ? 0
                        : limites.minRepsFor(c.get().bodyPartCode(), ctx.profile().fitnessLevel());
                if (e.plannedReps() - 2 >= minimo) {
                    lista.set(k, new PlanDraft.DraftExercise(e.exerciseId(), e.prescriptionType(), e.plannedSets(),
                            (short) (e.plannedReps() - 2), null, null, e.restSeconds(), null));
                    break;
                }
            }
            var fecha = ((java.sql.Date) f.get("scheduled_date")).toLocalDate().plusWeeks(1);
            int min = estimador.estimateMinutes(new PlanDraft.DraftWorkout(fecha,
                    WorkoutFocus.valueOf((String) f.get("focus_code")), "x", 0, lista), ctx.prescription());
            salida.add(new CorrectedSession(id, fecha, min, lista, "reduccion simulada"));
        }
        return salida;
    }

    private long participante() {
        String email = "concurrente" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1992, 1, 15), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("70"), null, FitnessLevel.INTERMEDIATE,
                GoalType.GENERAL_FITNESS, "concurrencia", TrainingLocation.GYM, (short) 3,
                List.of((short) 1, (short) 3, (short) 5), (short) 45, GIMNASIO, List.of(), "Ninguna",
                true, true, true, true, true));
        return u;
    }

    private void hacer(long u, long planId, boolean fatigaViernes) {
        var ids = jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ? ORDER BY scheduled_date",
                Long.class, planId);
        for (int i = 0; i < ids.size(); i++) {
            long w = ids.get(i);
            if (fatigaViernes && i == ids.size() - 1) {
                planes.skip(UserDetailsImpl.build(usuarios.findById(u).orElseThrow()), w,
                        new SkipWorkoutResource(SkipReason.FATIGUE));
                continue;
            }
            long s = sesiones.handle(new StartWorkoutSessionCommand(u, w)).orElseThrow().getId();
            for (var f : jdbc.queryForList("SELECT planned_exercise_id, prescription_type, planned_sets, planned_reps,"
                    + " planned_duration_seconds FROM planned_workout_exercises WHERE planned_workout_id = ?", w)) {
                boolean duracion = "DURATION".equals(f.get("prescription_type"));
                int series = f.get("planned_sets") == null ? 1 : ((Number) f.get("planned_sets")).intValue();
                int total = series * ((Number) f.get(duracion ? "planned_duration_seconds" : "planned_reps")).intValue();
                sesiones.handle(new RecordSessionExerciseCommand(u, s, ((Number) f.get("planned_exercise_id")).longValue(),
                        (short) series, duracion ? null : total, duracion ? total : null, null, null));
            }
            sesiones.handle(new FinishWorkoutSessionCommand(u, s, (short) 6, (short) 4, (short) 45));
        }
    }
}
