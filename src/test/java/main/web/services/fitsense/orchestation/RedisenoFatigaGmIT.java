package main.web.services.fitsense.orchestation;

import main.web.services.fitsense.execution.domain.model.commands.FinishWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.model.commands.RecordSessionExerciseCommand;
import main.web.services.fitsense.execution.domain.model.commands.StartWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.services.WorkoutSessionCommandService;
import main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import main.web.services.fitsense.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import main.web.services.fitsense.orchestration.application.internal.WeeklyCycleService;
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
 * REDISENO R/R' · P-A1 MULTISEMANA CON EL CICLO REAL: {@code PLAN-REV-2.0} y la progresion APAGADA (por defecto).
 * <p>
 * GAIN_MUSCLE, fatiga en una sesion: la IA simulada baja UNA serie a las estructurales de 3 series (la dosis de
 * mantenimiento); V8C la autoriza (MANTENIMIENTO) y V24 la acepta solo con esta politica. Despues:
 * <ul>
 *   <li>sin mas fatiga, la sesion se recupera de forma GRADUAL (pasos de RECUPERACION, uno por ciclo, nunca por
 *       encima de la dosis de antes y nunca una PROGRESION, porque la progresion sigue apagada);</li>
 *   <li>con fatiga persistente, la dosis no baja mas y el informe avisa (2 semanas: revisar habitos; 3: profesional).</li>
 * </ul>
 * Nada llega a Replicate. Cada semana se comprueba con la consulta de integridad del linaje.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.planning.policy=PLAN-REV-2.0",
        "fitsense.planning.policy.allow-candidates=true"
})
@DisplayName("Rediseno P-A1: fatiga en GAIN_MUSCLE con PLAN-REV-2.0 y la progresion apagada")
class RedisenoFatigaGmIT {

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

    static final AtomicInteger SECUENCIA = new AtomicInteger();
    static final LocalDate S1 = TrainingWeek.containing(LocalDate.now()).startDate().plusWeeks(1);
    static final List<String> EQUIPAMIENTO = List.of("dumbbell", "barbell", "cable", "leverage machine", "bench", "band");

    enum Dia { HECHO, FATIGA }

    record Ejercicio(long id, String tipo, Short series, Short reps, Integer segundos, Short descanso) {}

    record Sesion(long id, LocalDate fecha, String enfoque, int minutos, List<Ejercicio> ejercicios) {}

    @BeforeEach
    void iaSimulada() {
        reset(ia);
        when(ia.isEnabled()).thenReturn(true);
        when(ia.modelName()).thenReturn("simulado");
        when(ia.pedirRespuesta(any(), any(), any(), any(), anyInt()))
                .thenReturn(new ReplicateSessionFixGenerator.RawFix("{}", "salida-simulada"));
        when(ia.interpretar(anyString(), any())).thenAnswer(inv -> mantenimiento(inv.getArgument(1)));
    }

    @Test
    @DisplayName("fatiga -> dosis de mantenimiento (MANTENIMIENTO) -> copia -> recuperacion gradual hasta la dosis de antes")
    void mantenimientoYRecuperacionGradual() throws Exception {
        long u = participante();
        long w1 = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        assertEquals("PLAN-REV-2.0", politica(w1), "premisa: la semana se genera con REV2");
        var referencia = semana(w1);
        int fatigada = referencia.size() - 1;

        ejecutar(u, w1, dias(referencia.size(), fatigada, Dia.FATIGA));
        long w2 = ciclo(u, 0);
        var ev2 = ultimoIntento(u);
        System.out.println("P-A1 w2 " + ev2.get("status") + " " + ev2.get("targeted_interventions") + " "
                + referencia.get(fatigada).ejercicios() + " -> " + semana(w2).get(fatigada).ejercicios());
        assertEquals("{REVISAR_CARGA_Y_RECUPERACION}", ev2.get("targeted_interventions"), String.valueOf(ev2));
        assertEquals("APLICADA", ev2.get("status"), "con REV2 la dosis de mantenimiento se publica: " + ev2);
        // (las dosis autorizadas no se serializan en conservation_report: lo prueba que V24 la acepte, APLICADA)
        var reducidas = reducidas(referencia.get(fatigada), semana(w2).get(fatigada));
        assertFalse(reducidas.isEmpty(), "premisa: alguna estructural bajo una serie");
        for (var par : reducidas) {
            assertEquals(par[0].series() - 1, (int) par[1].series(), "una serie menos: " + Arrays.toString(par));
            assertEquals(par[0].reps(), par[1].reps(), "mismas repeticiones");
            assertEquals(par[0].descanso(), par[1].descanso(), "mismo descanso");
        }
        assertSinViolaciones();

        // Sin mas fatiga: pasos de RECUPERACION, uno por ciclo, sin pasar de la dosis de antes; nunca PROGRESION.
        long plan = w2;
        var tipos = new ArrayList<String>();
        var semanasConPaso = new ArrayList<Integer>();
        for (int k = 1; k <= 2 * reducidas.size() + 2; k++) {
            ejecutar(u, plan, dias(referencia.size(), -1, Dia.HECHO));
            plan = ciclo(u, k);
            var ev = ultimoIntento(u);
            String inf = informe(ev);
            if (inf.contains("\"tipo\":\"RECUPERACION\"")) {
                tipos.add("RECUPERACION");
                semanasConPaso.add(k + 2);
            }
            assertFalse(inf.contains("\"tipo\":\"PROGRESION\""), "la progresion esta apagada: " + inf);
            assertFalse(inf.contains("\"tipo\":\"REINCORPORACION\""), inf);
            var actual = semana(plan).get(fatigada);
            for (var e : actual.ejercicios()) {
                var antes = referencia.get(fatigada).ejercicios().stream().filter(x -> x.id() == e.id()).findFirst();
                antes.ifPresent(a -> assertTrue(e.series() <= a.series() && Objects.equals(e.reps(), a.reps()),
                        "nunca por encima de la dosis de antes: " + a + " -> " + e));
            }
            System.out.println("P-A1 semana " + (k + 2) + " " + ev.get("status") + " progresadas="
                    + ev.get("progressed_sessions") + " " + actual.ejercicios());
            assertSinViolaciones();
            if (actual.ejercicios().equals(referencia.get(fatigada).ejercicios())) break;
        }
        assertEquals(referencia.get(fatigada).ejercicios(), semana(plan).get(fatigada).ejercicios(),
                "tras " + tipos.size() + " pasos de recuperacion la sesion vuelve a la dosis de antes");
        assertEquals(reducidas.size(), tipos.size(), "un paso de RECUPERACION por serie quitada (uno por ciclo)");
        // LIMITACION MEDIDA (no se fuerza): cada paso exige dos semanas cumplidas desde el anterior, asi que con k
        // series quitadas la vuelta completa lleva 2k semanas (3 series: pasos en las semanas 4, 6 y 8). La evidencia
        // revisada vuelve en dos pasos (80-85 % -> 100 %); queda documentado en el informe.
        for (int i = 1; i < semanasConPaso.size(); i++)
            assertEquals(2, semanasConPaso.get(i) - semanasConPaso.get(i - 1), "un paso cada dos semanas: " + semanasConPaso);
        System.out.println("P-A1 RECUPERACION en las semanas " + semanasConPaso + " (" + reducidas.size() + " series)");
    }

    @Test
    @DisplayName("fatiga persistente: la dosis no baja de la de mantenimiento; aviso a las 2 semanas y a las 3 (profesional)")
    void fatigaPersistente() throws Exception {
        long u = participante();
        long w1 = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        var referencia = semana(w1);
        int fatigada = referencia.size() - 1;
        long plan = w1;
        var avisos = new ArrayList<String>();
        for (int k = 0; k < 3; k++) {
            ejecutar(u, plan, dias(referencia.size(), fatigada, Dia.FATIGA));
            var r = ciclo.runFor(u, S1.plusWeeks(k), S1.plusWeeks(k + 1));
            var ev = ultimoIntento(u);
            String inf = informe(ev);
            System.out.println("P-A1 persistente semana " + (k + 2) + " " + ev.get("status") + " "
                    + r.map(p -> semana(p).get(fatigada).ejercicios()).orElse(null));
            assertTrue(r.isPresent(), "la semana se publica: " + ev);
            plan = r.get();
            for (var e : semana(plan).get(fatigada).ejercicios())
                assertTrue(e.series() == null || e.series() >= 2, "nunca por debajo de 2 series (V9): " + e);
            avisos.add(inf.contains("FATIGA_PERSISTENTE") ? "PERSISTENTE" : inf.contains("FATIGA_REPETIDA")
                    ? "REPETIDA" : "-");
            assertSinViolaciones();
        }
        assertEquals(List.of("-", "REPETIDA", "PERSISTENTE"), avisos);
    }

    // =======================================================================
    // La IA simulada: la dosis de mantenimiento en las estructurales (3 -> 2 series), nada mas
    // =======================================================================

    List<CorrectedSession> mantenimiento(PlanGenerationContext ctx) {
        var salida = new ArrayList<CorrectedSession>();
        for (var senalada : ctx.adjustment().targetedSessions()) {
            var s = sesion(senalada.plannedWorkoutId());
            var ejercicios = new ArrayList<PlanDraft.DraftExercise>();
            for (var e : s.ejercicios()) {
                Short series = e.series();
                if (senalada.intervencion() == TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION
                        && "SETS_REPS".equals(e.tipo()) && series != null && series >= 3 && estructural(e.id()))
                    series = (short) (series - 1);
                ejercicios.add(new PlanDraft.DraftExercise(e.id(), PrescriptionType.valueOf(e.tipo()), series,
                        e.reps(), e.segundos(), null, e.descanso(), null));
            }
            var fecha = s.fecha().plusWeeks(1);
            int minutos = estimador.estimateMinutes(new PlanDraft.DraftWorkout(fecha,
                    WorkoutFocus.valueOf(s.enfoque()), "x", 0, ejercicios), ctx.prescription());
            salida.add(new CorrectedSession(s.id(), fecha, minutos, ejercicios, "mantenimiento simulado"));
        }
        return salida;
    }

    boolean estructural(long exerciseId) {
        String patron = jdbc.queryForObject("SELECT movement_pattern FROM exercises WHERE exercise_id = ?",
                String.class, exerciseId);
        return MandatorySlot.ofMovementPattern(String.valueOf(patron)).isPresent();
    }

    // =======================================================================
    // Material
    // =======================================================================

    static Dia[] dias(int n, int marcada, Dia marca) {
        var d = new Dia[n];
        Arrays.fill(d, Dia.HECHO);
        if (marcada >= 0) d[marcada] = marca;
        return d;
    }

    /** Pares [antes, despues] de los ejercicios cuya dosis cambio. */
    static List<Ejercicio[]> reducidas(Sesion antes, Sesion despues) {
        var pares = new ArrayList<Ejercicio[]>();
        for (var d : despues.ejercicios())
            antes.ejercicios().stream().filter(a -> a.id() == d.id() && !a.equals(d)).findFirst()
                    .ifPresent(a -> pares.add(new Ejercicio[]{a, d}));
        return pares;
    }

    long ciclo(long u, int k) {
        var r = ciclo.runFor(u, S1.plusWeeks(k), S1.plusWeeks(k + 1));
        assertTrue(r.isPresent(), "sin semana publicada en la " + (k + 2) + ": " + ultimoIntento(u));
        return r.get();
    }

    String politica(long plan) {
        return jdbc.queryForObject("SELECT planning_policy FROM weekly_training_plans WHERE plan_id = ?",
                String.class, plan);
    }

    long participante() {
        String email = "rediseno-gm" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long userId = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(userId, LocalDate.of(1992, 5, 10), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("68"), null, FitnessLevel.INTERMEDIATE,
                GoalType.GAIN_MUSCLE, "rediseno P-A1", TrainingLocation.GYM, (short) 3,
                List.of((short) 1, (short) 3, (short) 5), (short) 45, EQUIPAMIENTO, List.of(), "Ninguna",
                true, true, true, true, true));
        return userId;
    }

    List<Sesion> semana(long planId) {
        return jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ?"
                + " ORDER BY scheduled_date", Long.class, planId).stream().map(this::sesion).toList();
    }

    Sesion sesion(long id) {
        var f = jdbc.queryForMap("SELECT scheduled_date, focus_code, expected_duration_minutes FROM planned_workouts"
                + " WHERE planned_workout_id = ?", id);
        var ejercicios = jdbc.query("SELECT exercise_id, prescription_type, planned_sets, planned_reps,"
                        + " planned_duration_seconds, rest_seconds FROM planned_workout_exercises"
                        + " WHERE planned_workout_id = ? ORDER BY exercise_order",
                (rs, k) -> new Ejercicio(rs.getLong(1), rs.getString(2), rs.getObject(3, Short.class),
                        rs.getObject(4, Short.class), rs.getObject(5, Integer.class), rs.getObject(6, Short.class)), id);
        return new Sesion(id, ((java.sql.Date) f.get("scheduled_date")).toLocalDate(), (String) f.get("focus_code"),
                ((Number) f.get("expected_duration_minutes")).intValue(), ejercicios);
    }

    void ejecutar(long u, long planId, Dia... dias) {
        var principal = UserDetailsImpl.build(usuarios.findById(u).orElseThrow());
        var ids = jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ?"
                + " ORDER BY scheduled_date", Long.class, planId);
        for (int i = 0; i < ids.size(); i++) {
            long w = ids.get(i);
            if (dias[i] == Dia.FATIGA) {
                planes.skip(principal, w, new SkipWorkoutResource(SkipReason.FATIGUE));
                continue;
            }
            long sesion = sesiones.handle(new StartWorkoutSessionCommand(u, w)).orElseThrow().getId();
            for (var f : jdbc.queryForList("SELECT planned_exercise_id, prescription_type, planned_sets,"
                    + " planned_reps, planned_duration_seconds FROM planned_workout_exercises"
                    + " WHERE planned_workout_id = ? ORDER BY exercise_order", w)) {
                boolean duracion = "DURATION".equals(f.get("prescription_type"));
                int series = f.get("planned_sets") == null ? 1 : ((Number) f.get("planned_sets")).intValue();
                int total = series * ((Number) f.get(duracion ? "planned_duration_seconds" : "planned_reps")).intValue();
                sesiones.handle(new RecordSessionExerciseCommand(u, sesion,
                        ((Number) f.get("planned_exercise_id")).longValue(), (short) series,
                        duracion ? null : total, duracion ? total : null, null, null));
            }
            sesiones.handle(new FinishWorkoutSessionCommand(u, sesion, (short) 6, (short) 4, (short) 45));
        }
    }

    Map<String, Object> ultimoIntento(long u) {
        return jdbc.queryForMap("SELECT decision, status, ai_calls, resulting_plan_id, targeted_interventions::text,"
                + " applied_sessions::text, progressed_sessions::text, conservation_report::text, rejection_reason"
                + " FROM plan_adaptation_attempts WHERE user_id = ? ORDER BY attempt_id DESC LIMIT 1", u);
    }

    static String informe(Map<String, Object> ev) {
        return String.valueOf(ev.get("conservation_report")).replace(" ", "");
    }

    void assertSinViolaciones() throws Exception {
        var texto = Files.readString(Path.of("docs/FitSense/consultas/linaje_v31_v32.sql")).lines()
                .filter(l -> !l.stripLeading().startsWith("--")).collect(Collectors.joining("\n"));
        assertEquals(List.of(), jdbc.queryForList(texto.split(";")[0].strip()));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM plan_adaptation_attempts WHERE"
                + " cardinality(progressed_sessions) > 1 OR (cardinality(progressed_sessions) = 1 AND"
                + " (status <> 'SIN_ORDEN' OR resulting_plan_id IS NULL))", Integer.class));
    }
}
