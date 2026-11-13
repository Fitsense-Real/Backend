package main.web.services.fitsense.p2d;

import main.web.services.fitsense.execution.domain.exceptions.WorkoutNotAvailableException;
import main.web.services.fitsense.execution.domain.model.commands.StartWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.services.WorkoutSessionCommandService;
import main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import main.web.services.fitsense.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import main.web.services.fitsense.planning.application.internal.commandservices.PublishedPlanSafetyRepairService.Estado;
import main.web.services.fitsense.planning.domain.model.commands.ExpireOverdueWorkoutsCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.SafetyProfile;
import main.web.services.fitsense.planning.domain.services.ContencionTextoBandera;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.planning.interfaces.rest.TrainingPlanController;
import main.web.services.fitsense.planning.interfaces.rest.resources.WeeklyTrainingPlanResource;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.domain.model.valueobjects.TrainingWeek;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * INC-CAT-C39 REDUCIDA · IMPACTO SOBRE EL PLAN 78 (usuario 39), con el catalogo V1-V50 y PostgreSQL efimero, sin
 * Replicate. Los planes se manipulan SOLO en esta base aislada (copia controlada), nunca en datos reales.
 * <p>
 * El contenido es el plan 78 REPRODUCIDO por la auditoria del caso 39 ({@code src/test/resources/caso39/plan78.csv}):
 * coincide con el real en fechas, enfoques, duraciones, estructura, 1303 y 910; la identidad ejercicio a ejercicio
 * esta pendiente de una consulta sobre los datos reales. El perfil es el real: GENERAL_FITNESS principiante, gimnasio
 * completo (29 codigos), lunes/jueves/viernes/domingo, sin colgarse y sin brazos sobre la cabeza. La semana es la
 * siguiente a hoy con el mismo patron de dias, para que la prueba no caduque.
 * <p>
 * Escribe la evidencia en {@code -Dfitsense.plan78.salida} (por omision target/plan78_contencion.csv) ANTES de
 * comprobar nada, para poder ejecutarla tambien sobre otras variantes de la contencion y comparar.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("INC-CAT-C39 reducida · plan 78 del usuario 39 (copia controlada)")
class Plan78ContencionReducidaIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired PlanningContextFacade planning;
    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired UserProfileCommandService perfiles;
    @Autowired WorkoutSessionCommandService sesiones;
    @Autowired TrainingPlanController planes;
    @Autowired UserRepository usuarios;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transacciones;

    static final AtomicInteger SECUENCIA = new AtomicInteger();
    static final LocalDate HOY = LocalDate.now();
    static final LocalDate SEMANA = TrainingWeek.containing(HOY).startDate().plusWeeks(1);
    static final SafetyProfile SEGURIDAD_39 = new SafetyProfile(false, false, false, false, true, true, false);

    static final List<String> GIMNASIO = List.of("assisted", "band", "barbell", "bench", "body weight", "cable",
            "captains chair", "dumbbell", "ez barbell", "ghd bench", "gymnastic rings", "hyperextension bench",
            "landmine", "leverage machine", "olympic barbell", "other", "parallel bars", "preacher bench",
            "pull-up bar", "rope", "sled machine", "smith machine", "squat rack", "stationary bike", "step platform",
            "suspension straps", "treadmill", "vertical pole", "weighted");

    /** Enfoque y minutos declarados de cada sesion del plan 78 (lunes, jueves, viernes, domingo). */
    static final List<String> ENFOQUE = List.of("UPPER_BODY", "LOWER_BODY", "UPPER_BODY", "LOWER_BODY");
    static final List<Integer> MINUTOS = List.of(43, 42, 43, 43);

    /** Copia literal de la consulta de PLANES_PUBLICADOS_C39.md §2 (si cambia alli, cambia aqui). */
    static final String CONSULTA_IDENTIFICACION = """
            SELECT p.user_id, p.plan_id, p.week_start_date, pw.planned_workout_id, pw.scheduled_date, pw.status,
                   pwe.exercise_id, x.name_es,
                   CASE WHEN pwe.exercise_id IN (1153,1301,1303,1304,1306) THEN 'colgarse' ELSE 'brazos sobre la cabeza' END AS restriccion,
                   EXISTS (SELECT 1 FROM planned_workouts o WHERE o.plan_id = p.plan_id
                           AND o.status IN ('COMPLETED','PARTIAL','SKIPPED')) AS semana_empezada
            FROM weekly_training_plans p
            JOIN user_profiles up            ON up.user_id = p.user_id
            JOIN planned_workouts pw         ON pw.plan_id = p.plan_id AND pw.status IN ('SCHEDULED','IN_PROGRESS')
            JOIN planned_workout_exercises pwe ON pwe.planned_workout_id = pw.planned_workout_id
            JOIN exercises x                 ON x.exercise_id = pwe.exercise_id
            WHERE p.status = 'ACTIVE'
              AND (   (pwe.exercise_id IN (1153,1301,1303,1304,1306) AND up.allow_hanging_exercises  = false)
                   OR (pwe.exercise_id IN (131,400,448)              AND up.allow_overhead_exercises = false))
            ORDER BY pw.scheduled_date, p.user_id
            """;

    record Fila(int sesion, int orden, long ejercicio, String tipo, Integer series, Integer reps, Integer duracion,
                Integer descanso) {}

    static List<Fila> plan78() throws IOException {
        try (InputStream in = Plan78ContencionReducidaIT.class.getResourceAsStream("/caso39/plan78.csv")) {
            var filas = new ArrayList<Fila>();
            for (var linea : new String(Objects.requireNonNull(in).readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                if (linea.isBlank() || linea.startsWith("#")) continue;
                var c = linea.split(";", -1);
                filas.add(new Fila(Integer.parseInt(c[0]), Integer.parseInt(c[1]), Long.parseLong(c[2]), c[3],
                        entero(c[4]), entero(c[5]), entero(c[6]), entero(c[7])));
            }
            return filas;
        }
    }

    private static Integer entero(String s) {
        return s == null || s.isBlank() ? null : Integer.valueOf(s.trim());
    }

    record Copia(long userId, long planId, List<Long> sesiones) {}

    /** Perfil real del usuario 39 y una semana publicada cuyo contenido pasa a ser el del plan 78. */
    private Copia copiaDelPlan78() throws IOException {
        int n = SECUENCIA.incrementAndGet();
        String email = "plan78-" + n + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'P78', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        // allowHighImpact, allowFloor, allowKneeling, allowHanging, allowOverhead
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1985, 6, 1), BiologicalSex.MALE,
                new BigDecimal("180"), new BigDecimal("100"), new BigDecimal("80"), FitnessLevel.BEGINNER,
                GoalType.GENERAL_FITNESS, "Quiero Correr un maraton", TrainingLocation.GYM, (short) 4,
                List.of((short) 1, (short) 4, (short) 5, (short) 7), (short) 60, GIMNASIO, List.of(), null, true,
                true, true, false, false));
        long plan = planning.generateWeeklyPlan(u, SEMANA, PlanAdjustment.none(), null, null)
                .orElseThrow(() -> new AssertionError("no se genero la semana"));
        var ids = jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ?"
                + " ORDER BY scheduled_date", Long.class, plan);
        assertEquals(4, ids.size(), "lunes, jueves, viernes y domingo, como el plan 78");
        for (int i = 0; i < ids.size(); i++)
            jdbc.update("UPDATE planned_workouts SET focus_code = ?, expected_duration_minutes = ? WHERE planned_workout_id = ?",
                    ENFOQUE.get(i), MINUTOS.get(i), ids.get(i));
        jdbc.update("DELETE FROM planned_workout_exercises WHERE planned_workout_id IN"
                + " (SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ?)", plan);
        for (var f : plan78())
            jdbc.update("INSERT INTO planned_workout_exercises (planned_workout_id, exercise_id, exercise_order,"
                            + " prescription_type, planned_sets, planned_reps, planned_duration_seconds, rest_seconds)"
                            + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)", ids.get(f.sesion()), f.ejercicio(), f.orden(),
                    f.tipo(), f.series(), f.reps(), f.duracion(), f.descanso());
        return new Copia(u, plan, ids);
    }

    private WeeklyTrainingPlanResource leer(long userId, long planId) {
        var principal = UserDetailsImpl.build(usuarios.findById(userId).orElseThrow());
        return new TransactionTemplate(transacciones).execute(tx -> planes.byId(principal, planId).getBody());
    }

    /** Por sesion (0..3): los ejercicios bloqueados con su causa. */
    private Map<Integer, Map<Long, String>> bloqueos(Copia c) {
        var plan = leer(c.userId(), c.planId());
        var r = new TreeMap<Integer, Map<Long, String>>();
        for (int i = 0; i < c.sesiones().size(); i++) {
            long id = c.sesiones().get(i);
            var w = plan.workouts().stream().filter(x -> x.plannedWorkoutId() == id).findFirst().orElseThrow();
            var m = new TreeMap<Long, String>();
            w.exercises().stream().filter(e -> e.safetyBlockReason() != null)
                    .forEach(e -> m.put(e.exerciseId(), e.safetyBlockReason()));
            if (w.safetyBlocked() != !m.isEmpty()) throw new AssertionError("bloqueo de sesion incoherente " + i);
            if (!m.isEmpty()) r.put(i, m);
        }
        return r;
    }

    private List<Map<String, Object>> contenido(long planId) {
        return jdbc.queryForList("SELECT pw.scheduled_date, pwe.exercise_order, pwe.exercise_id, pwe.prescription_type,"
                + " pwe.planned_sets, pwe.planned_reps, pwe.planned_duration_seconds, pwe.rest_seconds"
                + " FROM planned_workouts pw JOIN planned_workout_exercises pwe ON pwe.planned_workout_id = pw.planned_workout_id"
                + " WHERE pw.plan_id = ? ORDER BY pw.scheduled_date, pwe.exercise_order", planId);
    }

    private static int num(Object o) {
        return o == null ? 0 : ((Number) o).intValue();
    }

    @Test
    @DisplayName("semana sin empezar: solo el jueves (1303) se bloquea; la reparacion no publica por el 916 (V48)")
    void semanaSinEmpezar() throws IOException {
        var c = copiaDelPlan78();
        var b = bloqueos(c);

        // Evidencia primero (se compara entre variantes de la contencion).
        var sb = new StringBuilder("variante;registro;sesion;ejercicio;causa\n");
        b.forEach((s, m) -> m.forEach((id, causa) -> sb.append(ContencionTextoBandera.registro().size()).append(';')
                .append(ContencionTextoBandera.registro().keySet().stream().sorted().toList()).append(';')
                .append(List.of("lun", "jue", "vie", "dom").get(s)).append(';').append(id).append(';').append(causa)
                .append('\n')));
        var salida = Path.of(System.getProperty("fitsense.plan78.salida", "target/plan78_contencion.csv"));
        Files.createDirectories(salida.toAbsolutePath().getParent());
        Files.writeString(salida, sb.toString(), StandardCharsets.UTF_8);

        // Con la contencion reducida: el jueves por el 1303 (apoyo suspendido); el viernes (1152 liberado, 910 nunca
        // contenido) y el lunes (1093 liberado) ya no.
        assertEquals(Map.of(1, Map.of(1303L, "INC-CAT-C39")), b);
        // La consulta de identificacion de PLANES_PUBLICADOS_C39.md §2 (solo lectura) encuentra lo mismo.
        var consulta = jdbc.queryForList(CONSULTA_IDENTIFICACION).stream()
                .filter(f -> ((Number) f.get("user_id")).longValue() == c.userId()).toList();
        assertEquals(1, consulta.size(), consulta.toString());
        assertEquals(1303L, ((Number) consulta.get(0).get("exercise_id")).longValue());
        assertEquals(c.sesiones().get(1), ((Number) consulta.get(0).get("planned_workout_id")).longValue());
        assertEquals(false, consulta.get(0).get("semana_empezada"));
        var error = assertThrows(WorkoutNotAvailableException.class,
                () -> sesiones.handle(new StartWorkoutSessionCommand(c.userId(), c.sesiones().get(1))));
        assertTrue(error.getMessage().contains("INC-CAT-C39"), error.getMessage());
        assertTrue(sesiones.handle(new StartWorkoutSessionCommand(c.userId(), c.sesiones().get(2))).isPresent(),
                "la sesion del viernes (1152, 910) se puede iniciar");
        jdbc.update("UPDATE planned_workouts SET status = 'SCHEDULED' WHERE planned_workout_id = ?", c.sesiones().get(2));
        jdbc.update("DELETE FROM workout_sessions WHERE planned_workout_id = ?", c.sesiones().get(2));

        // Reparacion explicita. LIMITACION ACTUAL, documentada: la reparacion revalida la semana ENTERA contra el
        // catalogo de hoy, y la misma sesion del jueves lleva el 916 (curl de piernas invertido en polea), que V48
        // subio a dificultad 3 y ya no es elegible para un principiante. V1 rechaza la version nueva y no se publica
        // nada: el jueves sigue bloqueado y la version 1 queda intacta. La dificultad no es causa de seguridad, asi
        // que el 916 por si solo no bloquea ninguna sesion publicada.
        assertEquals(3, jdbc.queryForObject("SELECT difficulty_level FROM exercises WHERE exercise_id = 916", Integer.class));
        var antes = contenido(c.planId());
        var r = planning.repairPublishedPlanForSafety(c.userId(), SEMANA, HOY);
        assertEquals(Estado.NO_VALIDA, r.estado(), r.toString());
        assertEquals(List.of("V1: el ejercicio 916 no esta en available_exercises."), r.problemas(), r.toString());
        assertEquals(antes, contenido(c.planId()), "la version 1 no se toca");
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ?",
                Integer.class, c.userId()), "no se publica ninguna version nueva");
        assertEquals(Map.of(1, Map.of(1303L, "INC-CAT-C39")), bloqueos(c), "el jueves sigue bloqueado");
    }

    /**
     * La misma semana SIN el 916 (sustituido por el curl de piernas sentado 1046, nivel 1, misma familia y sin banderas, antes de aplicar la
     * restriccion): la reparacion publica una version nueva que solo cambia el 1303, sin aumentar dosis (V8C).
     * Aisla el mecanismo de reparacion del efecto de V48 sobre el plan 78.
     */
    @Test
    @DisplayName("sin el 916: la reparacion sustituye solo el 1303, sin aumentar la dosis (V8C)")
    void reparacionLocalizada() throws IOException {
        var c = copiaDelPlan78();
        jdbc.update("UPDATE planned_workout_exercises SET exercise_id = 1046 WHERE exercise_id = 916 AND planned_workout_id = ?",
                c.sesiones().get(1));
        var antes = contenido(c.planId());
        var r = planning.repairPublishedPlanForSafety(c.userId(), SEMANA, HOY);
        assertEquals(Estado.REPARADA, r.estado(), r.toString());
        assertEquals(antes, contenido(c.planId()), "la version 1 no se toca");
        long v2 = jdbc.queryForObject("SELECT plan_id FROM weekly_training_plans WHERE user_id = ? AND status = 'ACTIVE'",
                Long.class, c.userId());
        var despues = contenido(v2);
        assertEquals(antes.size(), despues.size());
        int cambios = 0;
        for (int i = 0; i < antes.size(); i++) {
            var a = antes.get(i);
            var d = despues.get(i);
            if (Objects.equals(a, d)) continue;
            cambios++;
            assertEquals(1303L, ((Number) a.get("exercise_id")).longValue(), "solo cambia la fila del 1303: " + a);
            long sustituto = ((Number) d.get("exercise_id")).longValue();
            assertFalse(ContencionTextoBandera.contiene(sustituto, SEGURIDAD_39), "sustituto contenido: " + sustituto);
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM exercises WHERE exercise_id = ? AND"
                    + " (requires_hanging OR requires_overhead)", Integer.class, sustituto), "sustituto restringido");
            assertEquals(a.get("scheduled_date"), d.get("scheduled_date"));
            assertTrue(num(d.get("planned_sets")) <= num(a.get("planned_sets")), "sin mas series: " + a + " -> " + d);
            if (a.get("planned_reps") != null && d.get("planned_reps") != null)
                assertTrue(num(d.get("planned_reps")) <= num(a.get("planned_reps")), "sin mas repeticiones: " + a + " -> " + d);
            if (a.get("planned_duration_seconds") != null && d.get("planned_duration_seconds") != null)
                assertTrue(num(d.get("planned_duration_seconds")) <= num(a.get("planned_duration_seconds")),
                        "sin mas duracion: " + a + " -> " + d);
        }
        assertEquals(1, cambios, "una sola sustitucion localizada");
        assertTrue(bloqueos(new Copia(c.userId(), v2, jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts"
                + " WHERE plan_id = ? ORDER BY scheduled_date", Long.class, v2))).isEmpty(), "la version 2 no bloquea nada");
    }

    /**
     * LIMITACION ACTUAL, documentada (no es el comportamiento deseado): con una sesion ya terminada la reparacion no
     * versiona la semana (SEMANA_EMPEZADA). La sesion del jueves sigue bloqueada, no se puede iniciar y, al vencer,
     * la tarea de vencimiento la cierra como SKIPPED SIN MOTIVO: cuenta en la adherencia como no hecha aunque el
     * participante no pudo hacerla. Ver docs/FitSense/contencion_c39_reducida/PLANES_PUBLICADOS_C39.md.
     */
    @Test
    @DisplayName("semana empezada: el jueves sigue bloqueado y al vencer queda SKIPPED sin motivo (limitacion actual)")
    void semanaEmpezada() throws IOException {
        var c = copiaDelPlan78();
        jdbc.update("UPDATE planned_workouts SET status = 'COMPLETED' WHERE planned_workout_id = ?", c.sesiones().get(0));
        var antes = contenido(c.planId());

        var r = planning.repairPublishedPlanForSafety(c.userId(), SEMANA, HOY);
        assertEquals(Estado.SEMANA_EMPEZADA, r.estado(), r.toString());
        assertEquals(antes, contenido(c.planId()), "nada se sustituye en silencio");
        assertEquals(Map.of(1, Map.of(1303L, "INC-CAT-C39")), bloqueos(c));
        assertThrows(WorkoutNotAvailableException.class,
                () -> sesiones.handle(new StartWorkoutSessionCommand(c.userId(), c.sesiones().get(1))));

        var vence = jdbc.queryForObject("SELECT expires_at FROM planned_workouts WHERE planned_workout_id = ?",
                OffsetDateTime.class, c.sesiones().get(1));
        planComandos.handle(new ExpireOverdueWorkoutsCommand(vence.plusMinutes(1)));
        var fila = jdbc.queryForMap("SELECT status, skip_reason FROM planned_workouts WHERE planned_workout_id = ?",
                c.sesiones().get(1));
        assertEquals("SKIPPED", fila.get("status"));
        assertNull(fila.get("skip_reason"), "no existe un motivo de seguridad: se cuenta como no hecha");
    }
}
