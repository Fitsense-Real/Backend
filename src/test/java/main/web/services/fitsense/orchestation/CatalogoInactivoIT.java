package main.web.services.fitsense.orchestation;

import main.web.services.fitsense.execution.domain.model.commands.FinishWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.model.commands.RecordSessionExerciseCommand;
import main.web.services.fitsense.execution.domain.model.commands.StartWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.services.WorkoutSessionCommandService;
import main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import main.web.services.fitsense.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import main.web.services.fitsense.orchestration.application.internal.WeeklyCycleService;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
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
import main.web.services.fitsense.shared.domain.model.valueobjects.TrainingWeek;
import org.junit.jupiter.api.AfterEach;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FASE 4 del cierre de PLAN-REV-2.0: un ejercicio se DESACTIVA en el catalogo mientras esta en un plan publicado.
 * <p>
 * Ciclo real sobre PostgreSQL efimero, sin Replicate. Con {@code PLAN-REV-2.0} ({@code reparaCatalogoInactivo}):
 * <ul>
 *   <li>con sustituto equivalente estricto: la semana siguiente sale con SOLO esa colocacion sustituida (SUST-CAT-1,
 *       motivo {@code catalogo_inactivo}), el sustituto cumple patron, zona, musculo, dificultad e impacto no
 *       superiores; el resto de la semana identico; la semana medida (historia) intacta;</li>
 *   <li>sin sustituto: la semana no sale, el motivo empieza por «CATALOGO» (no queda en silencio) y no se publica
 *       nada inactivo;</li>
 *   <li>con IA (otra sesion senalada): la reparacion tambien se aplica a la sesion no senalada.</li>
 * </ul>
 * {@link CatalogoInactivoHistIT}: lo mismo con la historica, que conserva su comportamiento (V1, NO_FACTIBLE).
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.planning.policy=PLAN-REV-2.0",
        "fitsense.planning.policy.allow-candidates=true"
})
@DisplayName("Fase 4 · ejercicio desactivado en el catalogo con un plan publicado (REV2)")
class CatalogoInactivoIT {

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
    @Autowired JdbcTemplate jdbc;

    static final AtomicInteger SECUENCIA = new AtomicInteger();
    static final LocalDate S1 = TrainingWeek.containing(LocalDate.now()).startDate().plusWeeks(1);
    static final List<String> GIMNASIO = List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine",
            "bench", "band", "stationary bike", "treadmill", "kettlebell", "ez barbell", "smith machine");

    final List<Long> desactivados = new ArrayList<>();

    @BeforeEach
    void iaSimulada() {
        reset(ia);
        when(ia.isEnabled()).thenReturn(true);
        when(ia.modelName()).thenReturn("simulado");
        when(ia.pedirRespuesta(any(), any(), any(), any(), anyInt()))
                .thenReturn(new ReplicateSessionFixGenerator.RawFix("{}", "salida-simulada"));
        // La IA simulada no propone nada: lo que se mide es lo que hace el backend con lo heredado.
        when(ia.interpretar(anyString(), any())).thenReturn(List.of());
    }

    @AfterEach
    void reactivar() {
        desactivados.forEach(id -> jdbc.update("UPDATE exercises SET is_active = true WHERE exercise_id = ?", id));
        desactivados.clear();
    }

    record Fila(long pw, long ejercicio, String patron, int zona, String musculo, int dificultad, boolean impacto,
                String tipo) {}

    // =======================================================================
    // Escenarios
    // =======================================================================

    @Test
    @DisplayName("con equivalente estricto: SUST-CAT-1 solo en esa colocacion; resto identico; historia intacta")
    void conSustituto() throws Exception {
        long u = participante(GoalType.GENERAL_FITNESS, FitnessLevel.INTERMEDIATE, 45);
        long w1 = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        var x = conEquivalentes(w1, 2).orElseThrow(() -> new IllegalStateException("premisa: un ejercicio con equivalentes"));
        var antes = contenido(w1);
        desactivar(x.ejercicio());
        ejecutar(u, w1);
        var w2 = ciclo.runFor(u, S1, S1.plusWeeks(1));
        var ultimo = ultimo(u);
        System.out.println("CATALOGO_REV2 con_sustituto x=" + x + " -> " + ultimo);
        esperadoConSustituto(w2, ultimo, x, antes, w1);
    }

    void esperadoConSustituto(Optional<Long> w2, Map<String, Object> ultimo, Fila x, List<List<Long>> antes, long w1)
            throws Exception {
        assertTrue(w2.isPresent(), "con un equivalente estricto la semana sale: " + ultimo);
        String informe = String.valueOf(ultimo.get("informe")).replace(" ", "");
        assertTrue(informe.contains("\"regla\":\"SUST-CAT-1\"") && informe.contains("\"motivo\":\"catalogo_inactivo\""),
                informe);
        var despues = contenido(w2.get());
        assertEquals(antes.size(), despues.size());
        for (int i = 0; i < antes.size(); i++) {
            var a = antes.get(i);
            var d = despues.get(i);
            if (!a.contains(x.ejercicio())) {
                assertEquals(a, d, "una sesion sin el inactivo no cambia");
                continue;
            }
            assertEquals(a.size(), d.size(), "se sustituye, no se quita ni se anade");
            int k = a.indexOf(x.ejercicio());
            long puesto = d.get(k);
            assertNotEquals(x.ejercicio(), puesto);
            for (int j = 0; j < a.size(); j++) if (j != k) assertEquals(a.get(j), d.get(j), "solo esa colocacion");
            var y = fila(puesto);
            assertEquals(x.patron(), y.patron(), "mismo patron");
            assertEquals(x.zona(), y.zona(), "misma zona");
            assertTrue(y.dificultad() <= x.dificultad(), "dificultad no mayor");
            assertTrue(!y.impacto() || x.impacto(), "impacto no mayor");
            assertEquals(x.tipo(), y.tipo());
        }
        // La semana medida (historia) no se toca: sigue con el inactivo, como se hizo.
        assertTrue(contenido(w1).stream().anyMatch(s -> s.contains(x.ejercicio())), "la historia se conserva");
        sinInactivosPublicados();
        assertSinViolaciones();
    }

    @Test
    @DisplayName("sin equivalente estricto: no se publica; el motivo lo dice (CATALOGO); nada inactivo publicado")
    void sinSustituto() throws Exception {
        long u = participante(GoalType.INCREASE_STRENGTH, FitnessLevel.INTERMEDIATE, 45);
        long w1 = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        // Un ejercicio sin ningun posible equivalente (mismo patron y zona, dificultad no mayor) DENTRO del plan: si lo
        // hubiera en otra sesion no contigua seria un sustituto legitimo (la regla solo excluye la misma sesion y las
        // contiguas) y no se podria desactivar sin tocar el plan.
        var enPlan = contenido(w1).stream().flatMap(List::stream).collect(Collectors.toSet());
        var x = candidatos(w1).stream().filter(f -> jdbc.queryForList("SELECT exercise_id FROM exercises WHERE"
                        + " movement_pattern = ? AND body_part_id = ? AND difficulty_level <= ? AND exercise_id <> ?",
                Long.class, f.patron(), f.zona(), f.dificultad(), f.ejercicio()).stream().noneMatch(enPlan::contains))
                .findFirst().orElseThrow(() -> new IllegalStateException("premisa: un ejercicio sin equivalentes en el plan"));
        desactivar(x.ejercicio());
        // Tambien todos sus posibles equivalentes (mismo patron y zona, dificultad no mayor), que no estan en el plan.
        jdbc.queryForList("SELECT exercise_id FROM exercises WHERE is_active AND movement_pattern = ? AND body_part_id = ?"
                + " AND difficulty_level <= ?", Long.class, x.patron(), x.zona(), x.dificultad()).stream()
                .filter(id -> !enPlan.contains(id)).forEach(this::desactivar);
        ejecutar(u, w1);
        var w2 = ciclo.runFor(u, S1, S1.plusWeeks(1));
        var ultimo = ultimo(u);
        System.out.println("CATALOGO_REV2 sin_sustituto x=" + x + " desactivados=" + desactivados.size() + " -> " + ultimo);
        esperadoSinSustituto(w2, ultimo);
    }

    void esperadoSinSustituto(Optional<Long> w2, Map<String, Object> ultimo) throws Exception {
        assertTrue(w2.isEmpty(), "sin equivalente no se publica una sustitucion que no cumple la regla: " + ultimo);
        assertEquals("NO_FACTIBLE", ultimo.get("status"), String.valueOf(ultimo));
        assertTrue(String.valueOf(ultimo.get("rejection_reason")).startsWith("CATALOGO"),
                "el motivo es explicito: " + ultimo.get("rejection_reason"));
        sinInactivosPublicados();
    }

    @Test
    @DisplayName("con IA (otra sesion senalada por fatiga): la colocacion inactiva de la sesion no senalada tambien se repara")
    void conIaOtraSesionSenalada() throws Exception {
        long u = participante(GoalType.GENERAL_FITNESS, FitnessLevel.INTERMEDIATE, 45);
        long w1 = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        var ids = jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ? ORDER BY scheduled_date",
                Long.class, w1);
        // El inactivo en una sesion que NO se va a senalar (la primera); la ultima se omite por fatiga.
        var x = conEquivalentes(w1, 2, ids.get(0)).orElseThrow(() -> new IllegalStateException("premisa"));
        desactivar(x.ejercicio());
        hecho(u, ids.get(0));
        hecho(u, ids.get(1));
        planes.skip(UserDetailsImpl.build(usuarios.findById(u).orElseThrow()), ids.get(2),
                new SkipWorkoutResource(SkipReason.FATIGUE));
        var w2 = ciclo.runFor(u, S1, S1.plusWeeks(1));
        var ultimo = ultimo(u);
        System.out.println("CATALOGO_REV2 con_ia x=" + x + " -> " + ultimo);
        esperadoConIa(w2, ultimo, x);
    }

    void esperadoConIa(Optional<Long> w2, Map<String, Object> ultimo, Fila x) throws Exception {
        assertTrue(w2.isPresent(), String.valueOf(ultimo));
        assertTrue(String.valueOf(ultimo.get("informe")).replace(" ", "").contains("\"regla\":\"SUST-CAT-1\""),
                String.valueOf(ultimo));
        sinInactivosPublicados();
        assertSinViolaciones();
    }

    // =======================================================================
    // Material
    // =======================================================================

    /** Un ejercicio de fuerza (con patron) del plan con al menos {@code n} equivalentes estrictos activos fuera del plan. */
    Optional<Fila> conEquivalentes(long plan, int n) {
        return conEquivalentes(plan, n, null);
    }

    /** Las colocaciones de fuerza (con patron) del plan, en orden. */
    List<Fila> candidatos(long plan) {
        return jdbc.query("SELECT pw.planned_workout_id, e.exercise_id, e.movement_pattern, e.body_part_id,"
                        + " coalesce(e.target_muscle, ''), e.difficulty_level, e.high_impact, e.default_prescription"
                        + " FROM planned_workout_exercises pwe JOIN planned_workouts pw ON pw.planned_workout_id ="
                        + " pwe.planned_workout_id JOIN exercises e ON e.exercise_id = pwe.exercise_id WHERE pw.plan_id = ?"
                        + " AND e.movement_pattern IN ('HORIZONTAL_PUSH','HORIZONTAL_PULL','VERTICAL_PUSH','VERTICAL_PULL',"
                        + "'KNEE_DOMINANT','HIP_DOMINANT') ORDER BY pw.scheduled_date, pwe.exercise_order",
                (rs, k) -> new Fila(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getInt(4), rs.getString(5),
                        rs.getInt(6), rs.getBoolean(7), rs.getString(8)), plan);
    }

    Optional<Fila> conEquivalentes(long plan, int n, Long soloSesion) {
        var enPlan = contenido(plan).stream().flatMap(List::stream).collect(Collectors.toSet());
        var filas = jdbc.query("SELECT pw.planned_workout_id, e.exercise_id, e.movement_pattern, e.body_part_id,"
                        + " coalesce(e.target_muscle, ''), e.difficulty_level, e.high_impact, e.default_prescription"
                        + " FROM planned_workout_exercises pwe JOIN planned_workouts pw ON pw.planned_workout_id ="
                        + " pwe.planned_workout_id JOIN exercises e ON e.exercise_id = pwe.exercise_id WHERE pw.plan_id = ?"
                        + " AND e.movement_pattern IN ('HORIZONTAL_PUSH','HORIZONTAL_PULL','VERTICAL_PUSH','VERTICAL_PULL',"
                        + "'KNEE_DOMINANT','HIP_DOMINANT') ORDER BY pw.scheduled_date, pwe.exercise_order",
                (rs, k) -> new Fila(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getInt(4), rs.getString(5),
                        rs.getInt(6), rs.getBoolean(7), rs.getString(8)), plan);
        // Que no este en dos sesiones (asi la comprobacion «solo esa colocacion» es directa).
        var veces = filas.stream().collect(Collectors.groupingBy(Fila::ejercicio, Collectors.counting()));
        return filas.stream().filter(f -> veces.get(f.ejercicio()) == 1)
                .filter(f -> soloSesion == null || f.pw() == soloSesion)
                .filter(f -> !f.impacto())
                .filter(f -> jdbc.queryForList("SELECT exercise_id FROM exercises WHERE is_active AND movement_pattern = ?"
                                + " AND body_part_id = ? AND lower(coalesce(target_muscle, '')) = lower(?) AND difficulty_level <= ?"
                                + " AND NOT high_impact AND default_prescription = ? AND exercise_id <> ?", Long.class,
                        f.patron(), f.zona(), f.musculo(), f.dificultad(), f.tipo(), f.ejercicio()).stream()
                        .filter(id -> !enPlan.contains(id)).count() >= n)
                .findFirst();
    }

    Fila fila(long ejercicio) {
        return jdbc.queryForObject("SELECT 0, exercise_id, movement_pattern, body_part_id, coalesce(target_muscle, ''),"
                        + " difficulty_level, high_impact, default_prescription FROM exercises WHERE exercise_id = ?",
                (rs, k) -> new Fila(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getInt(4), rs.getString(5),
                        rs.getInt(6), rs.getBoolean(7), rs.getString(8)), ejercicio);
    }

    void desactivar(long id) {
        assertEquals(1, jdbc.update("UPDATE exercises SET is_active = false WHERE exercise_id = ?", id));
        desactivados.add(id);
    }

    /** Ningun plan con semana posterior a la medida contiene un ejercicio inactivo. */
    void sinInactivosPublicados() {
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM planned_workout_exercises pwe JOIN planned_workouts pw"
                + " ON pw.planned_workout_id = pwe.planned_workout_id JOIN weekly_training_plans p ON p.plan_id = pw.plan_id"
                + " JOIN exercises e ON e.exercise_id = pwe.exercise_id WHERE p.week_start_date > ? AND NOT e.is_active",
                Integer.class, java.sql.Date.valueOf(S1)), "se publico un ejercicio inactivo");
    }

    long participante(GoalType objetivo, FitnessLevel nivel, int minutos) {
        String email = "catinac" + getClass().getSimpleName().length() + "x" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1992, 1, 15), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("70"), null, nivel, objetivo, "catalogo", TrainingLocation.GYM,
                (short) 3, List.of((short) 1, (short) 3, (short) 5), (short) minutos, GIMNASIO,
                List.of(), "Ninguna", true, true, true, true, true));
        return u;
    }

    List<List<Long>> contenido(long planId) {
        return jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ? ORDER BY scheduled_date",
                Long.class, planId).stream().map(id -> jdbc.queryForList("SELECT exercise_id FROM planned_workout_exercises"
                + " WHERE planned_workout_id = ? ORDER BY exercise_order", Long.class, id)).toList();
    }

    Map<String, Object> ultimo(long u) {
        return jdbc.queryForMap("SELECT decision, status, rejection_reason, conservation_report::text AS informe"
                + " FROM plan_adaptation_attempts WHERE user_id = ? ORDER BY attempt_id DESC LIMIT 1", u);
    }

    void ejecutar(long u, long planId) {
        jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ? ORDER BY scheduled_date",
                Long.class, planId).forEach(w -> hecho(u, w));
    }

    void hecho(long u, long w) {
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

    void assertSinViolaciones() throws Exception {
        var texto = Files.readString(Path.of("docs/FitSense/consultas/linaje_v31_v32.sql")).lines()
                .filter(l -> !l.stripLeading().startsWith("--")).collect(Collectors.joining("\n"));
        assertEquals(List.of(), jdbc.queryForList(texto.split(";")[0].strip()));
    }
}
