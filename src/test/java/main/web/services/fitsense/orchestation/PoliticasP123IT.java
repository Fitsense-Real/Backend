package main.web.services.fitsense.orchestation;

import main.web.services.fitsense.execution.domain.model.commands.FinishWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.model.commands.RecordSessionExerciseCommand;
import main.web.services.fitsense.execution.domain.model.commands.StartWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.services.WorkoutSessionCommandService;
import main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import main.web.services.fitsense.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import main.web.services.fitsense.orchestration.application.internal.WeeklyCycleService;
import main.web.services.fitsense.planning.application.internal.commandservices.PainMemoryService;
import main.web.services.fitsense.planning.application.internal.commandservices.PlanGenerationPipeline;
import main.web.services.fitsense.planning.application.internal.commandservices.SafetyExclusionResolver;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.exceptions.PlanGenerationFailedException;
import main.web.services.fitsense.planning.domain.exceptions.ProfileOutsideGoalDurationException;
import main.web.services.fitsense.planning.domain.model.aggregates.UserSafetyExclusion;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.ExclusionesDeSeguridad;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateSessionFixGenerator;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateTrainingPlanGenerator;
import main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories.SafeRegenerationAttemptRepository;
import main.web.services.fitsense.planning.infrastructure.persistence.jpa.repositories.UserSafetyExclusionRepository;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.planning.interfaces.rest.TrainingPlanController;
import main.web.services.fitsense.planning.interfaces.rest.resources.SkipWorkoutResource;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.*;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.domain.exceptions.DomainRuleViolationException;
import main.web.services.fitsense.shared.domain.exceptions.ResourceNotFoundException;
import main.web.services.fitsense.shared.domain.model.valueobjects.SkipReason;
import main.web.services.fitsense.shared.domain.model.valueobjects.TrainingWeek;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P-01, P-02 y P-03 (politicas aprobadas el 31/10/2026) por el ciclo semanal de produccion, sobre PostgreSQL
 * efimero. La IA de correccion es un doble (nada llega a Replicate); la generacion inicial es el motor de reglas.
 * <ul>
 *   <li>P-01: un perfil historico fuera del rango no genera ni llama a la IA.</li>
 *   <li>P-02: dolor identificado y sin identificar; persistencia entre semanas y tras reiniciar; aislamiento por
 *       usuario; nueva generacion; reactivacion explicita (sin autorizacion, valida, rechazada por seguridad) y
 *       reactivar no es reincorporar.</li>
 *   <li>P-03: IA invalida -&gt; sustitucion determinista; sin sustituto -&gt; regeneracion con reglas; regeneracion
 *       inviable -&gt; NO_FACTIBLE sin publicar; doble ejecucion; interrupcion y reintento; sesiones ejecutadas
 *       intactas; restricciones multiples; misma frecuencia.</li>
 * </ul>
 * La progresion y la reincorporacion con exclusiones estan en {@code ProgresionIT} (progresion encendida).
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PoliticasP123IT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @MockitoBean ReplicateSessionFixGenerator ia;
    @MockitoSpyBean ReplicateTrainingPlanGenerator iaGeneracion;
    @MockitoSpyBean SafetyExclusionResolver exclusionesSeguridad;
    @MockitoSpyBean PlanGenerationPipeline pipeline;
    @MockitoSpyBean SafeRegenerationAttemptRepository regeneraciones;

    @Autowired WeeklyCycleService ciclo;
    @Autowired PlanningContextFacade planning;
    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired UserProfileCommandService perfiles;
    @Autowired WorkoutSessionCommandService sesiones;
    @Autowired TrainingPlanController planes;
    @Autowired UserRepository usuarios;
    @Autowired PainMemoryService dolor;
    @Autowired UserSafetyExclusionRepository exclusiones;
    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext contexto;

    static final AtomicInteger SECUENCIA = new AtomicInteger();
    static final LocalDate S1 = TrainingWeek.containing(LocalDate.now()).startDate().plusWeeks(1);
    static final List<String> GIMNASIO = List.of("dumbbell", "barbell", "cable", "leverage machine", "bench", "band");
    static final String CLAVE = "Clave-de-prueba-123";
    /** Para la prueba de reinicio: lo registrado antes de cerrar el contexto. */
    static long usuarioReinicio;
    static Set<Long> excluidosReinicio = Set.of();

    @BeforeEach
    void iaApagada() {
        reset(ia, exclusionesSeguridad, pipeline, regeneraciones);
        when(ia.isEnabled()).thenReturn(false);
    }

    // =====================================================================
    // P-01
    // =====================================================================

    @Test
    @Order(1)
    @DisplayName("P-01: un perfil historico de 15 min en LOSE_WEIGHT no genera, no llama a la IA y no se modifica")
    void perfilHistoricoFueraDeP01() {
        long u = usuario();
        // Un perfil congelado ANTES de P-01: se inserta tal cual estaba (la base admite 15-180).
        jdbc.update("INSERT INTO user_profiles (user_id, birth_date, height_cm, current_weight_kg, fitness_level,"
                + " goal_type, goal_text, training_location, days_per_week, available_days, session_minutes,"
                + " equipment_codes, blocked_exercise_ids) VALUES (?, '1990-01-01', 170, 70, 'BEGINNER', 'LOSE_WEIGHT',"
                + " 'historico', 'HOME', 3, '{1,3,5}', 15, '{}', '{}')", u);
        var e = assertThrows(ProfileOutsideGoalDurationException.class,
                () -> planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null));
        assertTrue(e.getMessage().contains(ProfileOutsideGoalDurationException.CODIGO), e.getMessage());
        verify(pipeline, never()).run(any(), anyInt());
        verify(iaGeneracion, never()).requestOutput(any(), any(), anyInt());
        assertEquals(15, jdbc.queryForObject("SELECT session_minutes FROM user_profiles WHERE user_id = ?",
                Integer.class, u), "el perfil no se corrige");
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ?",
                Integer.class, u));
    }

    @ParameterizedTest(name = "{0} {1} min")
    @CsvSource({"GAIN_MUSCLE,30", "INCREASE_STRENGTH,30", "LOSE_WEIGHT,30", "GENERAL_FITNESS,30",
            "GAIN_MUSCLE,60", "INCREASE_STRENGTH,60", "LOSE_WEIGHT,60", "GENERAL_FITNESS,60",
            "GAIN_MUSCLE,90", "INCREASE_STRENGTH,90", "LOSE_WEIGHT,90", "GENERAL_FITNESS,90",
            "GAIN_MUSCLE,105", "INCREASE_STRENGTH,105", "LOSE_WEIGHT,105", "GENERAL_FITNESS,105",
            "GAIN_MUSCLE,120", "INCREASE_STRENGTH,120", "LOSE_WEIGHT,120", "GENERAL_FITNESS,120"})
    @Order(2)
    @DisplayName("P-01: los cuatro objetivos de 30 a 120: o plan valido dentro del techo V4 (x 1,15), o no hay plan")
    void cuatroObjetivosEnLosLimites(GoalType objetivo, int minutos) {
        long u = participante(objetivo, FitnessLevel.INTERMEDIATE, minutos, GIMNASIO, true);
        int techo = (int) Math.floor(minutos * 1.15);
        int piso = (int) Math.ceil(minutos * 0.70);
        try {
            long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
            for (var d : jdbc.queryForList("SELECT expected_duration_minutes FROM planned_workouts WHERE plan_id = ?",
                    Integer.class, plan)) {
                assertTrue(d <= techo, objetivo + ": sesion de " + d + " min con techo " + techo);
                // La generacion inicial no hereda D8: ninguna sesion por debajo del piso de V20.
                assertTrue(d >= piso - 1, objetivo + ": sesion inicial de " + d + " min bajo el piso " + piso);
            }
            System.out.println("P01_LIMITE " + objetivo + " " + minutos + " PLAN " + plan);
        } catch (DomainRuleViolationException e) {
            System.out.println("P01_LIMITE " + objetivo + " " + minutos + " SIN_PLAN " + e.getMessage());
        }
    }

    @ParameterizedTest(name = "historico {0} {1} min")
    @CsvSource({"GAIN_MUSCLE,15", "INCREASE_STRENGTH,20", "LOSE_WEIGHT,150", "GENERAL_FITNESS,180"})
    @Order(3)
    @DisplayName("P-01: un perfil historico congelado fuera de 30-120 no genera, no llama a la IA y no se modifica")
    void perfilHistoricoFueraDelNuevoRango(GoalType objetivo, int minutos) {
        long u = usuario();
        jdbc.update("INSERT INTO user_profiles (user_id, birth_date, height_cm, current_weight_kg, fitness_level,"
                + " goal_type, goal_text, training_location, days_per_week, available_days, session_minutes,"
                + " equipment_codes, blocked_exercise_ids) VALUES (?, '1990-01-01', 170, 70, 'INTERMEDIATE', ?,"
                + " 'historico', 'GYM', 3, '{1,3,5}', ?, '{dumbbell}', '{}')", u, objetivo.name(), minutos);
        var e = assertThrows(ProfileOutsideGoalDurationException.class,
                () -> planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null));
        assertTrue(e.getMessage().contains("admite de 30 a 120"), e.getMessage());
        verify(pipeline, never()).run(any(), anyInt());
        verify(iaGeneracion, never()).requestOutput(any(), any(), anyInt());
        assertEquals(minutos, jdbc.queryForObject("SELECT session_minutes FROM user_profiles WHERE user_id = ?",
                Integer.class, u), "el perfil congelado no se corrige");
    }

    // =====================================================================
    // P-02
    // =====================================================================

    @Test
    @Order(10)
    @DisplayName("P-02: dolor en UN ejercicio identificado -> solo ese queda excluido; ausente 5 semanas y en una generacion nueva")
    void dolorIdentificadoPersisteCincoSemanas() {
        long u = participante(GoalType.GAIN_MUSCLE, FitnessLevel.INTERMEDIATE, 45, GIMNASIO, true);
        long w1 = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        var sesionesW1 = workouts(w1);
        long doloroso = ejercicios(sesionesW1.get(0)).get(0);
        hacer(u, sesionesW1.get(0), doloroso);
        hacer(u, sesionesW1.get(1), null);
        hacer(u, sesionesW1.get(2), null);
        var historialAntes = historial(u, w1);

        long w2 = ciclo.runFor(u, S1, S1.plusWeeks(1)).orElseThrow();
        var filas = exclusiones.findByUserIdOrderByIdAsc(u);
        assertEquals(1, filas.size(), "solo el ejercicio marcado: " + filas.stream().map(x -> x.getExerciseId()).toList());
        assertEquals(doloroso, filas.get(0).getExerciseId());
        assertEquals(UserSafetyExclusion.Reason.DOLOR_IDENTIFICADO, filas.get(0).getReason());
        assertEquals(UserSafetyExclusion.RecordedBy.CICLO_SEMANAL, filas.get(0).getRecordedBy());
        assertEquals(sesionesW1.get(0), filas.get(0).getSourcePlannedWorkoutId());
        assertFalse(contiene(w2, doloroso), "semana siguiente");
        assertEquals(historialAntes, historial(u, w1), "las sesiones ejecutadas no se reescriben");
        assertEquals(workouts(w1).size(), workouts(w2).size(), "misma frecuencia");

        long plan = w2;
        for (int k = 1; k <= 5; k++) {
            for (var w : workouts(plan)) hacer(u, w, null);
            plan = ciclo.runFor(u, S1.plusWeeks(k), S1.plusWeeks(k + 1)).orElseThrow();
            assertFalse(contiene(plan, doloroso), "semana " + (k + 2));
        }
        assertEquals(UserSafetyExclusion.Status.ACTIVA, exclusiones.findById(filas.get(0).getId()).orElseThrow().getStatus(),
                "cumplir cinco semanas no levanta la exclusion");

        // Una generacion NUEVA (sin semana medida) tampoco la ofrece.
        var ctx = planComandos.prepare(new GenerateWeeklyPlanCommand(u, S1.plusWeeks(20), null, false, null, null))
                .context();
        assertTrue(ctx.availableExercises().stream().noneMatch(c -> c.exerciseId() == doloroso));
        long nueva = planning.generateWeeklyPlan(u, S1.plusWeeks(20), PlanAdjustment.none(), null, null).orElseThrow();
        assertFalse(contiene(nueva, doloroso), "generacion nueva");

        // Otro usuario con el mismo perfil no hereda la exclusion.
        long otro = participante(GoalType.GAIN_MUSCLE, FitnessLevel.INTERMEDIATE, 45, GIMNASIO, true);
        assertTrue(dolor.excluidosActivos(otro).isEmpty());
        var suyo = planComandos.prepare(new GenerateWeeklyPlanCommand(otro, S1, null, false, null, null)).context();
        assertTrue(suyo.availableExercises().stream().anyMatch(c -> c.exerciseId() == doloroso),
                "para otro usuario sigue siendo candidato");
    }

    @Test
    @Order(20)
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    @DisplayName("P-02 (1/2): dolor de sesion sin identificar -> todos sus ejercicios; se cierra el contexto")
    void antesDelReinicio() {
        long u = participante(GoalType.INCREASE_STRENGTH, FitnessLevel.INTERMEDIATE, 45, GIMNASIO, true);
        long w1 = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        var ws = workouts(w1);
        omitirPorDolor(u, ws.get(1));
        usuarioReinicio = u;
        excluidosReinicio = new TreeSet<>(ejercicios(ws.get(1)));
        assertEquals(excluidosReinicio, dolor.excluidosActivos(u));
        assertTrue(exclusiones.findByUserIdOrderByIdAsc(u).stream().allMatch(x ->
                x.getReason() == UserSafetyExclusion.Reason.DOLOR_SESION_SIN_IDENTIFICAR
                        && x.getRecordedBy() == UserSafetyExclusion.RecordedBy.OMISION));
    }

    @Test
    @Order(21)
    @DisplayName("P-02 (2/2): tras reiniciar la aplicacion la exclusion sigue y se aplica")
    void despuesDelReinicio() {
        assertTrue(usuarioReinicio > 0, "premisa: corrio la parte 1");
        assertEquals(excluidosReinicio, dolor.excluidosActivos(usuarioReinicio));
        var ctx = planComandos.prepare(new GenerateWeeklyPlanCommand(usuarioReinicio, S1.plusWeeks(5), null, false,
                null, null)).context();
        assertTrue(ctx.availableExercises().stream().noneMatch(c -> excluidosReinicio.contains(c.exerciseId())));
    }

    @Test
    @Order(30)
    @DisplayName("P-02: reactivacion sin token (401), de otro usuario (404), sin confirmar (422), valida (200) y no reincorpora")
    void reactivacion() throws Exception {
        var http = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        var a = cuenta(http);
        var b = cuenta(http);
        perfil(a, GoalType.GAIN_MUSCLE, FitnessLevel.INTERMEDIATE, 45, GIMNASIO, true);
        long w1 = planning.generateWeeklyPlan(a, S1, PlanAdjustment.none(), null, null).orElseThrow();
        var ws = workouts(w1);
        long doloroso = ejercicios(ws.get(0)).get(0);
        hacer(a, ws.get(0), doloroso);
        hacer(a, ws.get(1), null);
        hacer(a, ws.get(2), null);
        long w2 = ciclo.runFor(a, S1, S1.plusWeeks(1)).orElseThrow();
        assertEquals(Set.of(doloroso), dolor.excluidosActivos(a));
        String ruta = "/api/v1/users/me/pain-exclusions/" + doloroso + "/reactivation";
        String cuerpo = "{\"confirmNoPain\":true,\"comment\":\"ya no molesta\"}";

        http.perform(post(ruta).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isUnauthorized());
        // GET: sin token 401; un usuario sin exclusiones recibe una lista vacia; cada uno ve solo las suyas.
        http.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/v1/users/me/pain-exclusions")).andExpect(status().isUnauthorized());
        http.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/v1/users/me/pain-exclusions").header("Authorization", "Bearer " + token(http, b)))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().json("[]"));
        http.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/v1/users/me/pain-exclusions").header("Authorization", "Bearer " + token(http, a)))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$[0].exerciseId").value(doloroso))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$[0].status").value("ACTIVA"));
        http.perform(post(ruta).header("Authorization", "Bearer " + token(http, b))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andExpect(status().isNotFound());
        http.perform(post(ruta).header("Authorization", "Bearer " + token(http, a))
                .contentType(MediaType.APPLICATION_JSON).content("{\"confirmNoPain\":false}"))
                .andExpect(status().isUnprocessableEntity());
        assertEquals(Set.of(doloroso), dolor.excluidosActivos(a), "nada de lo anterior la levanta");

        http.perform(post(ruta).header("Authorization", "Bearer " + token(http, a))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andExpect(status().isOk());
        var fila = exclusiones.findByUserIdOrderByIdAsc(a).get(0);
        assertEquals(UserSafetyExclusion.Status.REACTIVADA, fila.getStatus());
        assertNotNull(fila.getReactivatedAt());
        assertTrue(fila.getReactivationEvidence().startsWith("CONFIRMACION_EXPLICITA_DEL_USUARIO user=" + a));
        assertEquals("ya no molesta", fila.getReactivationReason());
        assertTrue(dolor.excluidosActivos(a).isEmpty());
        var ctx = planComandos.prepare(new GenerateWeeklyPlanCommand(a, S1.plusWeeks(2), null, true, null, null))
                .context();
        assertTrue(ctx.availableExercises().stream().anyMatch(c -> c.exerciseId() == doloroso),
                "reactivado: vuelve a ser candidato");

        // Reactivar NO es reincorporar: la semana siguiente se conserva y no lo trae de vuelta.
        for (var w : workouts(w2)) hacer(a, w, null);
        long w3 = ciclo.runFor(a, S1.plusWeeks(1), S1.plusWeeks(2)).orElseThrow();
        assertFalse(contiene(w3, doloroso), "reactivar no lo anade automaticamente");
        // Y releer la semana del dolor no resucita la exclusion (el evento original ya se proceso).
        dolor.registrarSemana(a, S1);
        assertTrue(dolor.excluidosActivos(a).isEmpty());
    }

    @Test
    @Order(31)
    @DisplayName("P-02: la reactivacion se rechaza si el ejercicio choca con una restriccion de seguridad vigente")
    void reactivacionRechazadaPorSeguridad() {
        long u = participante(GoalType.GAIN_MUSCLE, FitnessLevel.INTERMEDIATE, 45, GIMNASIO, false);
        long w1 = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        long sesion = workouts(w1).get(0);
        // Un ejercicio que exige los brazos por encima de la cabeza; el perfil no lo permite.
        long porEncima = jdbc.queryForObject("SELECT exercise_id FROM exercises WHERE requires_overhead AND is_active"
                + " ORDER BY exercise_id LIMIT 1", Long.class);
        // Fixture: un dolor registrado sobre ese ejercicio (su evento de origen es una sesion real del usuario).
        exclusiones.save(new UserSafetyExclusion(u, porEncima, UserSafetyExclusion.Reason.DOLOR_IDENTIFICADO, w1,
                sesion, null, S1, UserSafetyExclusion.RecordedBy.SESION));
        var e = assertThrows(DomainRuleViolationException.class, () -> dolor.reactivar(u, porEncima, true, "ok"));
        assertTrue(e.getMessage().startsWith("REACTIVACION_RECHAZADA_POR_SEGURIDAD"), e.getMessage());
        assertTrue(dolor.excluidosActivos(u).contains(porEncima), "sigue excluido");
        assertThrows(ResourceNotFoundException.class, () -> dolor.reactivar(u, 999_999L, true, null));
    }

    @Test
    @Order(32)
    @DisplayName("P-02 concurrencia: dos reactivaciones simultaneas -> una reactiva, la otra no encuentra nada (404)")
    void reactivacionesSimultaneas() throws Exception {
        long u = participante(GoalType.GAIN_MUSCLE, FitnessLevel.INTERMEDIATE, 45, GIMNASIO, true);
        long w1 = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        long sesion = workouts(w1).get(0);
        long ejercicio = ejercicios(sesion).get(0);
        exclusiones.save(new UserSafetyExclusion(u, ejercicio, UserSafetyExclusion.Reason.DOLOR_IDENTIFICADO, w1,
                sesion, null, S1, UserSafetyExclusion.RecordedBy.SESION));
        var salida = new java.util.concurrent.CountDownLatch(1);
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.Callable<String> tarea = () -> {
            salida.await();
            try {
                dolor.reactivar(u, ejercicio, true, null);
                return "OK";
            } catch (ResourceNotFoundException e) {
                return "404";
            }
        };
        var a = pool.submit(tarea);
        var b = pool.submit(tarea);
        salida.countDown();
        var resultados = new java.util.ArrayList<>(List.of(a.get(), b.get()));
        pool.shutdown();
        java.util.Collections.sort(resultados);
        assertEquals(List.of("404", "OK"), resultados);
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM user_safety_exclusions WHERE user_id = ? AND"
                + " status = 'REACTIVADA'", Integer.class, u));
    }

    @Test
    @Order(34)
    @DisplayName("V43 concurrencia: el mismo dolor registrado dos veces a la vez -> una fila; otro usuario a la vez -> la suya")
    void dolorSimultaneoMismoEventoYOtroUsuario() throws Exception {
        long u = participante(GoalType.GAIN_MUSCLE, FitnessLevel.INTERMEDIATE, 45, GIMNASIO, true);
        long v = participante(GoalType.INCREASE_STRENGTH, FitnessLevel.INTERMEDIATE, 45, GIMNASIO, true);
        long wu = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        long wv = planning.generateWeeklyPlan(v, S1, PlanAdjustment.none(), null, null).orElseThrow();
        long su = workouts(wu).get(0), sv = workouts(wv).get(0);
        long eu = ejercicios(su).get(0), ev = ejercicios(sv).get(0);
        hacer(u, su, eu);
        hacer(v, sv, ev);
        // El cierre de la sesion ya registra; se borra para que las tres llamadas compitan por la PRIMERA insercion.
        jdbc.update("DELETE FROM user_safety_exclusions WHERE user_id IN (?, ?)", u, v);

        var salida = new java.util.concurrent.CountDownLatch(1);
        var pool = java.util.concurrent.Executors.newFixedThreadPool(3);
        java.util.function.Function<Long, java.util.concurrent.Callable<Integer>> tarea = usuario -> () -> {
            salida.await();
            return dolor.registrarSemana(usuario, S1).size();
        };
        var a = pool.submit(tarea.apply(u));
        var b = pool.submit(tarea.apply(u));
        var c = pool.submit(tarea.apply(v));
        salida.countDown();
        a.get();
        b.get();
        c.get();
        pool.shutdown();
        assertEquals(List.of(eu), jdbc.queryForList("SELECT exercise_id FROM user_safety_exclusions WHERE user_id = ?",
                Long.class, u), "una sola fila por evento aunque se registre dos veces a la vez");
        assertEquals(List.of(ev), jdbc.queryForList("SELECT exercise_id FROM user_safety_exclusions WHERE user_id = ?",
                Long.class, v), "el otro usuario tiene la suya y solo la suya");
        assertEquals(Set.of(eu), dolor.excluidosActivos(u));
        assertEquals(Set.of(ev), dolor.excluidosActivos(v));
    }

    @Test
    @Order(33)
    @DisplayName("P-02 orden: un dolor NUEVO despues de reactivar vuelve a excluir; registrar dos veces no duplica")
    void dolorNuevoTrasReactivar() {
        long u = participante(GoalType.GAIN_MUSCLE, FitnessLevel.INTERMEDIATE, 45, GIMNASIO, true);
        long w1 = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        var ws = workouts(w1);
        long ejercicio = ejercicios(ws.get(0)).get(0);
        hacer(u, ws.get(0), ejercicio);
        dolor.registrarSemana(u, S1);
        dolor.registrarSemana(u, S1);   // idempotente
        assertEquals(1, exclusiones.findByUserIdOrderByIdAsc(u).size());
        dolor.reactivar(u, ejercicio, true, null);
        assertTrue(dolor.excluidosActivos(u).isEmpty());
        // Nuevo evento sobre el mismo ejercicio despues de reactivar (otra sesion real del usuario como origen).
        exclusiones.save(new UserSafetyExclusion(u, ejercicio, UserSafetyExclusion.Reason.DOLOR_IDENTIFICADO, w1,
                ws.get(1), null, S1.plusDays(2), UserSafetyExclusion.RecordedBy.SESION));
        assertEquals(java.util.Set.of(ejercicio), dolor.excluidosActivos(u), "el dolor posterior prevalece");
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM user_safety_exclusions WHERE user_id = ? AND"
                + " status = 'REACTIVADA'", Integer.class, u), "la reactivacion anterior sigue constando");
    }

    // =====================================================================
    // P-03
    // =====================================================================

    @Test
    @Order(40)
    @DisplayName("P-03: la IA responde invalido tres veces -> sustitucion determinista en el mismo ciclo, sin lo que dolio")
    void iaInvalidaSustitucionDeterminista() {
        when(ia.isEnabled()).thenReturn(true);
        when(ia.modelName()).thenReturn("simulado");
        when(ia.pedirRespuesta(any(), any(), any(), any(), anyInt()))
                .thenReturn(new ReplicateSessionFixGenerator.RawFix("{}", "respuesta simulada invalida"));
        when(ia.interpretar(anyString(), any())).thenThrow(new InvalidPlanDraftException(List.of("JSON simulado invalido")));
        long u = participante(GoalType.LOSE_WEIGHT, FitnessLevel.INTERMEDIATE, 45, GIMNASIO, true);
        long w1 = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        var ws = workouts(w1);
        hacer(u, ws.get(0), null);
        omitirPorDolor(u, ws.get(1));
        hacer(u, ws.get(2), null);
        var excluidos = dolor.excluidosActivos(u);

        long w2 = ciclo.runFor(u, S1, S1.plusWeeks(1)).orElseThrow();
        var ev = ultimoIntento(u);
        assertEquals("RESPALDO", ev.get("status"), String.valueOf(ev));
        assertEquals(3, ((Number) ev.get("ai_calls")).intValue(), "los tres intentos, ni uno mas");
        assertNoContieneNinguno(w2, excluidos);
        assertEquals(workouts(w1).size(), workouts(w2).size());
        var api = planes.adaptations(UserDetailsImpl.build(usuarios.findById(u).orElseThrow())).getBody();
        assertEquals("REGENERACION_SEGURA_SUSTITUCION", api.get(api.size() - 1).resultado());
        verify(pipeline, never()).runDeterministic(any(), anyInt());
    }

    @Test
    @Order(41)
    @DisplayName("P-03: sin sustituto -> regeneracion determinista valida en el mismo ciclo; repetir el ciclo no duplica")
    void sinSustitutoRegeneraYEsIdempotente() {
        long u = participante(GoalType.GAIN_MUSCLE, FitnessLevel.INTERMEDIATE, 45, GIMNASIO, true);
        // Simula que SUST-SEG-1 no encuentra sustituto: sin exclusiones verificadas el respaldo no puede resolver.
        doReturn(ExclusionesDeSeguridad.ninguna()).when(exclusionesSeguridad).resolver(argThat(c -> c.userId() == u));
        long w1 = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        var ws = workouts(w1);
        hacer(u, ws.get(0), null);
        omitirPorDolor(u, ws.get(1));
        hacer(u, ws.get(2), null);
        var historialAntes = historial(u, w1);
        var excluidos = dolor.excluidosActivos(u);

        long w2 = ciclo.runFor(u, S1, S1.plusWeeks(1)).orElseThrow();
        assertEquals("NO_FACTIBLE", ultimoIntento(u).get("status"), "la adaptacion no pudo; consta");
        var reg = jdbc.queryForMap("SELECT * FROM safe_regeneration_attempts WHERE user_id = ?", u);
        assertEquals("PUBLICADO", reg.get("outcome"));
        assertEquals("RULES", reg.get("source"));
        assertEquals(w2, ((Number) reg.get("resulting_plan_id")).longValue());
        assertEquals("RULE_ENGINE", jdbc.queryForObject("SELECT generation_source FROM weekly_training_plans"
                + " WHERE plan_id = ?", String.class, w2));
        assertNoContieneNinguno(w2, excluidos);
        assertEquals(ws.size(), workouts(w2).size(), "mismas sesiones (V2)");
        assertEquals(historialAntes, historial(u, w1), "lo ejecutado no se toca");
        var api = planes.adaptations(UserDetailsImpl.build(usuarios.findById(u).orElseThrow())).getBody();
        assertEquals("REGENERACION_SEGURA_REGLAS", api.get(api.size() - 1).resultado(), String.valueOf(api));
        assertEquals(w2, api.get(api.size() - 1).resultingPlanId());
        verify(iaGeneracion, never()).requestOutput(any(), any(), anyInt());

        // Doble ejecucion del ciclo: mismo plan, nada duplicado.
        assertEquals(w2, ciclo.runFor(u, S1, S1.plusWeeks(1)).orElseThrow());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ? AND"
                + " week_start_date = ?", Integer.class, u, S1.plusWeeks(1)));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM safe_regeneration_attempts WHERE user_id = ?",
                Integer.class, u));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM plan_adaptation_attempts WHERE user_id = ?",
                Integer.class, u));
    }

    @Test
    @Order(45)
    @DisplayName("V43 concurrencia: dos cierres simultaneos de la misma semana con regeneracion -> un plan y una traza")
    void regeneracionesSimultaneasMismaSemana() throws Exception {
        long u = participante(GoalType.INCREASE_STRENGTH, FitnessLevel.INTERMEDIATE, 45, GIMNASIO, true);
        doReturn(ExclusionesDeSeguridad.ninguna()).when(exclusionesSeguridad).resolver(argThat(c -> c.userId() == u));
        long w1 = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        var ws = workouts(w1);
        hacer(u, ws.get(0), null);
        hacer(u, ws.get(1), null);
        omitirPorDolor(u, ws.get(2));
        var excluidos = dolor.excluidosActivos(u);

        var salida = new java.util.concurrent.CountDownLatch(1);
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.Callable<Optional<Long>> tarea = () -> {
            salida.await();
            return ciclo.runFor(u, S1, S1.plusWeeks(1));
        };
        var a = pool.submit(tarea);
        var b = pool.submit(tarea);
        salida.countDown();
        var ra = a.get();
        var rb = b.get();
        pool.shutdown();

        // Nada duplicado ni a medias, gane quien gane: una traza por medicion (ux_sra_medicion) y, si publica, un
        // plan y solo uno, el que la traza nombra.
        var trazas = jdbc.queryForList("SELECT * FROM safe_regeneration_attempts WHERE user_id = ?", u);
        assertEquals(1, trazas.size(), "una sola traza de P-03 por medicion");
        assertEquals("PUBLICADO", trazas.get(0).get("outcome"), "premisa del escenario: la regeneracion es valida");
        var planes = jdbc.queryForList("SELECT plan_id FROM weekly_training_plans WHERE user_id = ? AND"
                + " week_start_date = ?", Long.class, u, S1.plusWeeks(1));
        assertEquals(1, planes.size(), "una sola semana publicada");
        long publicado = planes.get(0);
        assertEquals(publicado, ((Number) trazas.get(0).get("resulting_plan_id")).longValue(),
                "la traza apunta al plan que quedo");
        for (var r : List.of(ra, rb)) r.ifPresent(id -> assertEquals(publicado, id));
        assertNoContieneNinguno(publicado, excluidos);
        // Un cierre posterior devuelve lo mismo y no duplica.
        assertEquals(publicado, ciclo.runFor(u, S1, S1.plusWeeks(1)).orElseThrow());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM safe_regeneration_attempts WHERE user_id = ?",
                Integer.class, u));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ? AND"
                + " week_start_date = ?", Integer.class, u, S1.plusWeeks(1)));
    }

    @Test
    @Order(42)
    @DisplayName("P-03: regeneracion determinista inviable -> NO_FACTIBLE con motivo y sin publicar nada")
    void regeneracionInviable() {
        long u = participante(GoalType.GAIN_MUSCLE, FitnessLevel.INTERMEDIATE, 45, GIMNASIO, true);
        doReturn(ExclusionesDeSeguridad.ninguna()).when(exclusionesSeguridad).resolver(argThat(c -> c.userId() == u));
        doThrow(new PlanGenerationFailedException(u, "simulado: V15 sin composicion valida"))
                .when(pipeline).runDeterministic(argThat(c -> c.userId() == u), anyInt());
        long w1 = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        var ws = workouts(w1);
        omitirPorDolor(u, ws.get(0));
        hacer(u, ws.get(1), null);
        hacer(u, ws.get(2), null);

        assertTrue(ciclo.runFor(u, S1, S1.plusWeeks(1)).isEmpty());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ? AND"
                + " week_start_date = ?", Integer.class, u, S1.plusWeeks(1)), "sin publicacion insegura");
        var reg = jdbc.queryForMap("SELECT * FROM safe_regeneration_attempts WHERE user_id = ?", u);
        assertEquals("NO_FACTIBLE", reg.get("outcome"));
        assertTrue(String.valueOf(reg.get("validation_problems")).contains("V15"), String.valueOf(reg));
        var api = planes.adaptations(UserDetailsImpl.build(usuarios.findById(u).orElseThrow())).getBody();
        var ultimo = api.get(api.size() - 1);
        assertEquals("NO_FACTIBLE_POR_SEGURIDAD", ultimo.resultado());
        assertNull(ultimo.resultingPlanId());
        assertTrue(ultimo.motivos().stream().anyMatch(m -> m.startsWith("P-03: sin alternativa")), ultimo.motivos().toString());
        // El ciclo siguiente sigue recordando el dolor.
        assertFalse(dolor.excluidosActivos(u).isEmpty());
    }

    @Test
    @Order(43)
    @DisplayName("P-03: interrupcion al registrar la regeneracion -> nada a medias; el reintento publica una sola vez")
    void interrupcionYReintento() {
        long u = participante(GoalType.INCREASE_STRENGTH, FitnessLevel.INTERMEDIATE, 45, GIMNASIO, true);
        doReturn(ExclusionesDeSeguridad.ninguna()).when(exclusionesSeguridad).resolver(argThat(c -> c.userId() == u));
        long w1 = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        var ws = workouts(w1);
        hacer(u, ws.get(0), null);
        hacer(u, ws.get(1), null);
        omitirPorDolor(u, ws.get(2));
        doThrow(new IllegalStateException("caida simulada")).when(regeneraciones).save(any());

        assertTrue(ciclo.runFor(u, S1, S1.plusWeeks(1)).isEmpty(), "la caida corta el ciclo");
        reset(regeneraciones);   // la caida no se repite
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ? AND"
                + " week_start_date = ?", Integer.class, u, S1.plusWeeks(1)), "el plan no queda sin su traza");
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM safe_regeneration_attempts WHERE user_id = ?",
                Integer.class, u));

        long w2 = ciclo.runFor(u, S1, S1.plusWeeks(1)).orElseThrow();
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ? AND"
                + " week_start_date = ?", Integer.class, u, S1.plusWeeks(1)));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM safe_regeneration_attempts WHERE user_id = ?"
                + " AND outcome = 'PUBLICADO'", Integer.class, u));
        assertNoContieneNinguno(w2, dolor.excluidosActivos(u));
    }

    @Test
    @Order(44)
    @DisplayName("P-03 con restricciones multiples y un dolor anterior (P-02): nada inseguro, nada excluido, misma frecuencia")
    void restriccionesMultiplesYDolorAnterior() {
        long u = participante(GoalType.GENERAL_FITNESS, FitnessLevel.BEGINNER, 45, List.of("dumbbell", "band"), false);
        long w1 = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        var ws = workouts(w1);
        long primero = ejercicios(ws.get(0)).get(0);
        hacer(u, ws.get(0), primero);
        hacer(u, ws.get(1), null);
        hacer(u, ws.get(2), null);
        var w2 = ciclo.runFor(u, S1, S1.plusWeeks(1));
        assertTrue(dolor.excluidosActivos(u).contains(primero));
        if (w2.isEmpty()) return;   // sin alternativa: NO_FACTIBLE registrado, nada publicado
        var ws2 = workouts(w2.get());
        omitirPorDolor(u, ws2.get(1));
        hacer(u, ws2.get(0), null);
        hacer(u, ws2.get(2), null);
        var w3 = ciclo.runFor(u, S1.plusWeeks(1), S1.plusWeeks(2));
        var excluidos = dolor.excluidosActivos(u);
        assertTrue(excluidos.contains(primero), "el dolor anterior sigue");
        assertTrue(excluidos.containsAll(ejercicios(ws2.get(1))), "y se suma el nuevo");
        w3.ifPresent(p -> {
            assertNoContieneNinguno(p, excluidos);
            assertEquals(ws.size(), workouts(p).size());
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM planned_workout_exercises pwe JOIN"
                    + " planned_workouts pw ON pw.planned_workout_id = pwe.planned_workout_id JOIN exercises e ON"
                    + " e.exercise_id = pwe.exercise_id WHERE pw.plan_id = ? AND (e.high_impact OR e.requires_floor"
                    + " OR e.requires_kneeling OR e.requires_overhead OR e.requires_hanging)", Integer.class, p),
                    "ninguna restriccion declarada se viola");
        });
    }

    // =====================================================================
    // Apoyo
    // =====================================================================

    private long usuario() {
        String email = "p123_" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        return jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
    }

    private long participante(GoalType objetivo, FitnessLevel nivel, int minutos, List<String> equipo, boolean libre) {
        long u = usuario();
        perfil(u, objetivo, nivel, minutos, equipo, libre);
        return u;
    }

    private void perfil(long u, GoalType objetivo, FitnessLevel nivel, int minutos, List<String> equipo, boolean libre) {
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1991, 3, 3), BiologicalSex.MALE,
                new BigDecimal("175"), new BigDecimal("75"), null, nivel, objetivo, "politicas p123",
                TrainingLocation.GYM, (short) 3, List.of((short) 1, (short) 3, (short) 5), (short) minutos, equipo,
                List.of(), null, libre, libre, libre, libre, libre));
    }

    private long cuenta(MockMvc http) throws Exception {
        String email = "p123http" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
        http.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"%s","password":"%s","firstName":"Prueba","lastName":"P123",
                 "timezone":"America/Lima","acceptsStudyParticipation":true}""".formatted(email, CLAVE)))
                .andExpect(status().isCreated());
        return jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
    }

    private String token(MockMvc http, long u) throws Exception {
        String email = jdbc.queryForObject("SELECT email FROM users WHERE user_id = ?", String.class, u);
        var res = http.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, CLAVE)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var m = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"").matcher(res);
        assertTrue(m.find(), res);
        return m.group(1);
    }

    private List<Long> workouts(long planId) {
        return jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ?"
                + " ORDER BY scheduled_date", Long.class, planId);
    }

    private List<Long> ejercicios(long plannedWorkoutId) {
        return jdbc.queryForList("SELECT exercise_id FROM planned_workout_exercises WHERE planned_workout_id = ?"
                + " ORDER BY exercise_order", Long.class, plannedWorkoutId);
    }

    private boolean contiene(long planId, long exerciseId) {
        return jdbc.queryForObject("SELECT count(*) FROM planned_workout_exercises pwe JOIN planned_workouts pw"
                + " ON pw.planned_workout_id = pwe.planned_workout_id WHERE pw.plan_id = ? AND pwe.exercise_id = ?",
                Integer.class, planId, exerciseId) > 0;
    }

    private void assertNoContieneNinguno(long planId, Collection<Long> excluidos) {
        assertFalse(excluidos.isEmpty(), "premisa: hay exclusiones");
        for (var id : excluidos) assertFalse(contiene(planId, id), "plan " + planId + " publica el excluido " + id);
    }

    /** Ejecuta la sesion completa; si {@code conDolor} no es null, ese ejercicio se marca con dolor y no se hace. */
    private void hacer(long u, long plannedWorkoutId, Long conDolor) {
        long sesion = sesiones.handle(new StartWorkoutSessionCommand(u, plannedWorkoutId)).orElseThrow().getId();
        var filas = jdbc.queryForList("SELECT planned_exercise_id, exercise_id, prescription_type, planned_sets,"
                + " planned_reps, planned_duration_seconds FROM planned_workout_exercises WHERE planned_workout_id = ?"
                + " ORDER BY exercise_order", plannedWorkoutId);
        for (var f : filas) {
            long pe = ((Number) f.get("planned_exercise_id")).longValue();
            long ex = ((Number) f.get("exercise_id")).longValue();
            int series = f.get("planned_sets") == null ? 1 : ((Number) f.get("planned_sets")).intValue();
            boolean duracion = "DURATION".equals(f.get("prescription_type"));
            if (conDolor != null && conDolor == ex) {
                sesiones.handle(new RecordSessionExerciseCommand(u, sesion, pe, (short) 0, duracion ? null : 0,
                        duracion ? 0 : null, null, SkipReason.PAIN_OR_DISCOMFORT));
                continue;
            }
            int reps = f.get("planned_reps") == null ? 0 : ((Number) f.get("planned_reps")).intValue();
            int segundos = f.get("planned_duration_seconds") == null ? 0
                    : ((Number) f.get("planned_duration_seconds")).intValue();
            sesiones.handle(new RecordSessionExerciseCommand(u, sesion, pe, (short) series,
                    duracion ? null : series * reps, duracion ? series * segundos : null, null, null));
        }
        sesiones.handle(new FinishWorkoutSessionCommand(u, sesion, (short) 6, (short) 4, (short) 45));
    }

    private void omitirPorDolor(long u, long plannedWorkoutId) {
        planes.skip(UserDetailsImpl.build(usuarios.findById(u).orElseThrow()), plannedWorkoutId,
                new SkipWorkoutResource(SkipReason.PAIN_OR_DISCOMFORT));
    }

    private Map<String, Object> ultimoIntento(long u) {
        return jdbc.queryForList("SELECT * FROM plan_adaptation_attempts WHERE user_id = ? ORDER BY attempt_id DESC",
                u).get(0);
    }

    private List<Map<String, Object>> historial(long u, long planMedido) {
        var filas = new ArrayList<Map<String, Object>>();
        filas.addAll(jdbc.queryForList("SELECT session_id, planned_workout_id, status, completion_percentage,"
                + " session_rpe FROM workout_sessions WHERE user_id = ? ORDER BY session_id", u));
        filas.addAll(jdbc.queryForList("SELECT pw.planned_workout_id, pw.status, pw.skip_reason, pwe.exercise_id,"
                + " pwe.planned_sets, pwe.planned_reps FROM planned_workouts pw JOIN planned_workout_exercises pwe"
                + " ON pwe.planned_workout_id = pw.planned_workout_id WHERE pw.plan_id = ?"
                + " ORDER BY pw.scheduled_date, pwe.exercise_order", planMedido));
        return filas;
    }
}
