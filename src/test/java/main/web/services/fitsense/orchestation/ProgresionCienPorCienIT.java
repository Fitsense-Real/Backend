package main.web.services.fitsense.orchestation;

import main.web.services.fitsense.execution.domain.model.commands.FinishWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.model.commands.RecordSessionExerciseCommand;
import main.web.services.fitsense.execution.domain.model.commands.StartWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.services.WorkoutSessionCommandService;
import main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import main.web.services.fitsense.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import main.web.services.fitsense.orchestration.application.internal.WeeklyCycleService;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
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
 * PROGRESION ORDINARIA SOLO CON DOS SEMANAS AL 100 % REAL (27/10/2026), por el ciclo
 * completo con la progresion encendida en esta base aislada. La adaptacion sigue
 * con su 80 % (good_threshold_pct, sin tocar):
 * <pre>
 * &lt; 80 %            adaptacion (si el decisor la ordena), sin progresion
 * 80 % a &lt; 100 %    mantenimiento, sin progresion
 * 100 % una semana   mantenimiento, sin progresion
 * 100 % dos semanas  se evalua la progresion (por sesion: la que acaba de progresar espera)
 * </pre>
 * Toda la progresion es determinista: ai_calls = 0 en V32.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.adaptation.progression.enabled=true"
})
@DisplayName("Progresion al 100 % en dos semanas consecutivas; la adaptacion sigue en el 80 %")
class ProgresionCienPorCienIT {

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

    enum Dia { HECHO, PARCIAL, NADA, FATIGA }

    @BeforeEach
    void iaSimulada() {
        reset(ia);
        when(ia.isEnabled()).thenReturn(true);
        when(ia.modelName()).thenReturn("simulado");
        when(ia.pedirRespuesta(any(), any(), any(), any(), anyInt()))
                .thenReturn(new ReplicateSessionFixGenerator.RawFix("{}", "salida-simulada"));
        // Si llega a pedirse una adaptacion, la IA devuelve las senaladas tal cual.
        when(ia.interpretar(anyString(), any())).thenAnswer(inv -> igual(inv.getArgument(1)));
    }

    @Test
    @DisplayName("dos semanas al 100 % -> PROGRESION con ai_calls 0; la sesion que progreso espera dos semanas suyas")
    void dosSemanasPerfectasProgresa() throws Exception {
        long u = participante();
        long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();

        ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.HECHO);
        plan = ciclo(u, 0);
        assertEquals("{}", ultimo(u).get("progressed_sessions"), "100 % una semana: no progresa");

        ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.HECHO);
        plan = ciclo(u, 1);
        var ev = ultimo(u);
        assertEquals("SIN_ORDEN", ev.get("status"));
        assertEquals(0, ((Number) ev.get("ai_calls")).intValue(), "la progresion no llama a la IA");
        String primera = String.valueOf(ev.get("progressed_sessions"));
        assertNotEquals("{}", primera, "100 % dos semanas: progresa");
        assertTrue(informe(ev).contains("\"tipo\":\"PROGRESION\""), informe(ev));
        long progresadaEn = derivadaDe(plan, primera);

        // Semana siguiente al 100 %: la sesion que acaba de progresar no puede volver a hacerlo
        // (sus dos semanas tienen que ser posteriores al aumento); otra si.
        ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.HECHO);
        plan = ciclo(u, 2);
        ev = ultimo(u);
        assertNotEquals("{" + progresadaEn + "}", String.valueOf(ev.get("progressed_sessions")),
                "la sesion recien progresada no reutiliza semanas anteriores a su aumento");
        assertEquals(0, ((Number) ev.get("ai_calls")).intValue());
        assertSinViolaciones();
    }

    @Test
    @DisplayName("una semana entre el 80 % y el 99 %: mantenimiento sin progresion, y despues hacen falta dos semanas nuevas al 100 %")
    void semanaParcialBloqueaDosSemanas() throws Exception {
        long u = participante();
        long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.HECHO);
        plan = ciclo(u, 0);

        ejecutar(u, plan, Dia.PARCIAL, Dia.HECHO, Dia.HECHO);
        plan = ciclo(u, 1);
        var ev = ultimo(u);
        double observada = ((Number) ev.get("observed_adherence_pct")).doubleValue();
        assertTrue(observada >= 80 && observada < 100, "premisa: entre 80 y 99,99 %: " + observada);
        assertEquals("NO_CHANGE", ev.get("decision"), "con 80 % o mas no se adapta");
        assertEquals("{}", ev.get("progressed_sessions"), "con " + observada + " % no se progresa (antes, con el 80 %, si)");
        assertEquals(0, ((Number) ev.get("ai_calls")).intValue());

        ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.HECHO);
        plan = ciclo(u, 2);
        assertEquals("{}", ultimo(u).get("progressed_sessions"), "una sola semana al 100 % tras la parcial: no progresa");

        ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.HECHO);
        ciclo(u, 3);
        assertNotEquals("{}", ultimo(u).get("progressed_sessions"), "dos semanas nuevas al 100 %: progresa");
        assertSinViolaciones();
    }

    @Test
    @DisplayName("100 % de lo registrado pero una sesion sin registro: no progresa")
    void registroIncompletoNoProgresa() throws Exception {
        long u = participante();
        long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.HECHO);
        plan = ciclo(u, 0);

        ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.NADA);
        var r = ciclo.runFor(u, S1.plusWeeks(1), S1.plusWeeks(2));
        assertTrue(r.isPresent(), String.valueOf(ultimo(u)));
        var ev = ultimo(u);
        assertEquals("{}", ev.get("progressed_sessions"), "sin registro completo no hay progresion: " + ev);
        assertEquals(0, ((Number) ev.get("ai_calls")).intValue());
    }

    @Test
    @DisplayName("menos del 80 %: la adaptacion de siempre y ninguna progresion en ese ciclo")
    void bajoOchentaAdaptaSinProgresar() throws Exception {
        long u = participante();
        long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.HECHO);
        plan = ciclo(u, 0);

        ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.FATIGA);
        clearInvocations(ia);
        assertTrue(ciclo.runFor(u, S1.plusWeeks(1), S1.plusWeeks(2)).isPresent());
        var ev = ultimo(u);
        assertTrue(((Number) ev.get("observed_adherence_pct")).doubleValue() < 80, String.valueOf(ev));
        assertTrue(String.valueOf(ev.get("decision")).startsWith("ADAPT_"), "con menos del 80 % se adapta: " + ev);
        assertEquals("{}", ev.get("progressed_sessions"), "una modificacion por ciclo: con orden no se progresa");
    }

    @Test
    @DisplayName("reejecutar el ciclo de una semana con progresion no la duplica")
    void reejecucionNoDuplica() throws Exception {
        long u = participante();
        long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.HECHO);
        plan = ciclo(u, 0);
        ejecutar(u, plan, Dia.HECHO, Dia.HECHO, Dia.HECHO);
        long progresado = ciclo(u, 1);
        int filas = intentos(u);
        assertNotEquals("{}", ultimo(u).get("progressed_sessions"));

        var otra = ciclo.runFor(u, S1.plusWeeks(1), S1.plusWeeks(2));
        assertEquals(Optional.of(progresado), otra, "el mismo plan");
        assertEquals(filas, intentos(u), "sin evidencia nueva");
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ?"
                + " AND week_start_date = ? AND status = 'ACTIVE'", Integer.class, u, S1.plusWeeks(2)));
    }

    // =======================================================================

    private long ciclo(long u, int k) {
        var r = ciclo.runFor(u, S1.plusWeeks(k), S1.plusWeeks(k + 1));
        assertTrue(r.isPresent(), "sin semana publicada en la " + (k + 2) + ": " + ultimo(u));
        return r.get();
    }

    /** La sesion del plan nuevo que deriva de la que progreso. */
    private long derivadaDe(long plan, String progresadas) {
        long origen = Long.parseLong(progresadas.replaceAll("[{}]", ""));
        return jdbc.queryForObject("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ?"
                + " AND source_planned_workout_id = ?", Long.class, plan, origen);
    }

    private List<CorrectedSession> igual(PlanGenerationContext ctx) {
        var salida = new ArrayList<CorrectedSession>();
        for (var senalada : ctx.adjustment().targetedSessions()) {
            long id = senalada.plannedWorkoutId();
            var f = jdbc.queryForMap("SELECT scheduled_date, expected_duration_minutes FROM planned_workouts"
                    + " WHERE planned_workout_id = ?", id);
            var lista = jdbc.query("SELECT exercise_id, prescription_type, planned_sets, planned_reps,"
                            + " planned_duration_seconds, target_load_kg, rest_seconds FROM planned_workout_exercises"
                            + " WHERE planned_workout_id = ? ORDER BY exercise_order",
                    (rs, i) -> new PlanDraft.DraftExercise(rs.getLong(1), PrescriptionType.valueOf(rs.getString(2)),
                            rs.getObject(3, Short.class), rs.getObject(4, Short.class), rs.getObject(5, Integer.class),
                            rs.getBigDecimal(6), rs.getObject(7, Short.class), null), id);
            salida.add(new CorrectedSession(id, ((java.sql.Date) f.get("scheduled_date")).toLocalDate().plusWeeks(1),
                    ((Number) f.get("expected_duration_minutes")).intValue(), lista, "igual"));
        }
        return salida;
    }

    private long participante() {
        String email = "cien-" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1992, 1, 15), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("70"), null, FitnessLevel.INTERMEDIATE, GoalType.LOSE_WEIGHT,
                "progresion al 100", TrainingLocation.GYM, (short) 3, List.of((short) 1, (short) 3, (short) 5),
                (short) 45, GIMNASIO, List.of(), "Ninguna", true, true, true, true, true));
        return u;
    }

    private void ejecutar(long u, long planId, Dia... dias) {
        var ids = jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ?"
                + " ORDER BY scheduled_date", Long.class, planId);
        for (int i = 0; i < ids.size(); i++) {
            long w = ids.get(i);
            switch (dias[i]) {
                case HECHO, PARCIAL -> {
                    long s = sesiones.handle(new StartWorkoutSessionCommand(u, w)).orElseThrow().getId();
                    boolean primero = dias[i] == Dia.PARCIAL;
                    for (var f : jdbc.queryForList("SELECT planned_exercise_id, prescription_type, planned_sets,"
                            + " planned_reps, planned_duration_seconds FROM planned_workout_exercises"
                            + " WHERE planned_workout_id = ? ORDER BY exercise_order", w)) {
                        boolean duracion = "DURATION".equals(f.get("prescription_type"));
                        int series = f.get("planned_sets") == null ? 1 : ((Number) f.get("planned_sets")).intValue();
                        int unidad = ((Number) f.get(duracion ? "planned_duration_seconds" : "planned_reps")).intValue();
                        // PARCIAL: el primer ejercicio de repeticiones con una serie menos (falta de tiempo).
                        boolean recorta = primero && !duracion && series > 1;
                        int hechas = recorta ? series - 1 : series;
                        sesiones.handle(new RecordSessionExerciseCommand(u, s,
                                ((Number) f.get("planned_exercise_id")).longValue(), (short) hechas,
                                duracion ? null : hechas * unidad, duracion ? hechas * unidad : null, null,
                                recorta ? SkipReason.LACK_OF_TIME : null));
                        if (recorta) primero = false;
                    }
                    sesiones.handle(new FinishWorkoutSessionCommand(u, s, (short) 6, (short) 4, (short) 45));
                }
                case NADA -> { }
                case FATIGA -> planes.skip(UserDetailsImpl.build(usuarios.findById(u).orElseThrow()), w,
                        new SkipWorkoutResource(SkipReason.FATIGUE));
            }
        }
    }

    private int intentos(long u) {
        return jdbc.queryForObject("SELECT count(*) FROM plan_adaptation_attempts WHERE user_id = ?", Integer.class, u);
    }

    private Map<String, Object> ultimo(long u) {
        return jdbc.queryForMap("SELECT decision, status, ai_calls, observed_adherence_pct, progressed_sessions::text,"
                + " conservation_report::text FROM plan_adaptation_attempts WHERE user_id = ?"
                + " ORDER BY attempt_id DESC LIMIT 1", u);
    }

    private static String informe(Map<String, Object> ev) {
        return String.valueOf(ev.get("conservation_report")).replace(" ", "");
    }

    private void assertSinViolaciones() throws Exception {
        var texto = Files.readString(Path.of("docs/FitSense/consultas/linaje_v31_v32.sql")).lines()
                .filter(l -> !l.stripLeading().startsWith("--")).collect(Collectors.joining("\n"));
        assertEquals(List.of(), jdbc.queryForList(texto.split(";")[0].strip()));
    }
}
