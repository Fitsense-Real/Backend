package main.web.services.fitsense.orchestation;

import main.web.services.fitsense.RelojPruebas;
import main.web.services.fitsense.execution.domain.model.commands.FinishWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.model.commands.RecordSessionExerciseCommand;
import main.web.services.fitsense.execution.domain.model.commands.StartWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.services.WorkoutSessionCommandService;
import main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import main.web.services.fitsense.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import main.web.services.fitsense.orchestration.application.internal.WeeklyCycleService;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.SessionDurationEstimator;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateSessionFixGenerator;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.planning.interfaces.rest.TrainingPlanController;
import main.web.services.fitsense.planning.interfaces.rest.resources.SkipWorkoutResource;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.*;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.domain.model.valueobjects.SkipReason;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
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
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * C-2 · REPRODUCCION DEL CASO N03 S4 de la campana p123 (orden de cierre del PR #16).
 * <p>
 * N03 (LOSE_WEIGHT principiante, casa, bandas y peso corporal, sin impacto, 7 dias, 30 min) se recrea con el MISMO
 * {@code user_id} (4) en una base limpia, asi que el motor de reglas usa la misma semilla y genera la misma semana.
 * Se ejecuta el guion S1-S4 con la misma IA simulada de la campana:
 * <ul>
 *   <li>S3: dos sesiones parciales -&gt; ACORTAR retira el relleno 1117: el lunes queda en 15 min con D8;</li>
 *   <li>S4: esa sesion se omite por motivacion -&gt; INVESTIGAR_CAMBIO_MINIMO; la IA la devuelve igual.</li>
 * </ul>
 * Antes de C-2: tres rechazos de V20 (sin D8) y despues el respaldo publicaba esa misma sesion. Ahora: la propuesta
 * identica hereda D8, se publica en la PRIMERA llamada como SIN_EFECTO y la validacion independiente la da por buena.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.adaptation.progression.enabled=true"
})
class C2ReproduccionN03IT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @MockitoBean ReplicateSessionFixGenerator ia;
    /** El reloj lo controla la prueba: sin barrido horario de entrenamientos vencidos. */
    @MockitoBean main.web.services.fitsense.planning.infrastructure.scheduling.OverdueWorkoutScheduler vencidas;

    @Autowired WeeklyCycleService ciclo;
    @Autowired PlanningContextFacade planning;
    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired UserProfileCommandService perfiles;
    @Autowired WorkoutSessionCommandService sesiones;
    @Autowired TrainingPlanController planes;
    @Autowired UserRepository usuarios;
    @Autowired SessionDurationEstimator estimador;
    @Autowired PlanDraftValidator validador;
    @Autowired JdbcTemplate jdbc;

    static final LocalDate S1 = LocalDate.of(2026, 11, 2);

    @Test
    @DisplayName("N03 S4: la sesion acortada senalada y devuelta igual hereda D8 y se publica sin rechazos de V20")
    void n03s4() {
        iaSimulada();
        // Mismo user_id que en la campana (4): N07_NEG, N01 y N02 ocupaban 1-3.
        for (int i = 1; i <= 3; i++) usuario("relleno" + i);
        long u = usuario("n03");
        assertEquals(4L, u, "premisa: mismo user_id y misma semilla que N03 en la campana");
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 6, 15), BiologicalSex.FEMALE,
                new BigDecimal("166"), new BigDecimal("68"), null, FitnessLevel.BEGINNER, GoalType.LOSE_WEIGHT,
                "c2 n03", TrainingLocation.HOME, (short) 7,
                List.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6, (short) 7), (short) 30,
                List.of("body weight", "band"), List.of(), null, false, true, true, true, true));

        long plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null).orElseThrow();
        // S1 completa, S2 dificil (todo hecho), S3 dos parciales, S4 dos omisiones por motivacion.
        ejecutarTodo(u, plan, null);
        plan = ciclo.runFor(u, S1, S1.plusWeeks(1)).orElseThrow();
        ejecutarTodo(u, plan, SkipReason.TOO_DIFFICULT);
        plan = ciclo.runFor(u, S1.plusWeeks(1), S1.plusWeeks(2)).orElseThrow();
        var ws = workouts(plan);
        parcial(u, ws.get(0));
        parcial(u, ws.get(1));
        for (int i = 2; i < ws.size(); i++) hecho(u, ws.get(i), null);
        plan = ciclo.runFor(u, S1.plusWeeks(2), S1.plusWeeks(3)).orElseThrow();
        var s3 = ultimo(u);
        assertEquals("APLICADA", s3.get("status"), "premisa: ACORTAR aplicado en S3 " + s3);
        long acortada = workouts(plan).get(0);
        int minutosAcortada = jdbc.queryForObject("SELECT expected_duration_minutes FROM planned_workouts WHERE"
                + " planned_workout_id = ?", Integer.class, acortada);
        assertTrue(minutosAcortada < 21, "premisa: la sesion acortada esta bajo el piso de V20 (21): " + minutosAcortada);
        assertTrue(minutosAcortada < 30, "D8 sigue admitiendo una sesion acortada por debajo del minimo de P-01");

        ws = workouts(plan);
        var principal = UserDetailsImpl.build(usuarios.findById(u).orElseThrow());
        planes.skip(principal, ws.get(0), new SkipWorkoutResource(SkipReason.LACK_OF_MOTIVATION));
        planes.skip(principal, ws.get(1), new SkipWorkoutResource(SkipReason.LACK_OF_MOTIVATION));
        for (int i = 2; i < ws.size(); i++) hecho(u, ws.get(i), null);
        clearInvocations(ia);
        long s5 = ciclo.runFor(u, S1.plusWeeks(3), S1.plusWeeks(4)).orElseThrow();
        var s4 = ultimo(u);

        assertEquals("{INVESTIGAR_CAMBIO_MINIMO}", s4.get("targeted_interventions"), String.valueOf(s4));
        assertEquals("SIN_EFECTO", s4.get("status"), "antes: RESPALDO tras tres rechazos de V20 · " + s4);
        assertEquals(1, ((Number) s4.get("ai_calls")).intValue(), "una sola llamada (antes: tres)");
        verify(ia, times(1)).pedirRespuesta(any(), any(), any(), any(), anyInt());
        assertTrue(String.valueOf(s4.get("conservation_report")).replace(" ", "")
                .contains("\"fechas_con_duracion_reducible\":[\"" + S1.plusWeeks(4) + "\""), "D8 heredada: " + s4);
        // La sesion publicada es la misma copia y la validacion independiente la reconoce.
        long copia = workouts(s5).get(0);
        assertEquals(ejercicios(acortada), ejercicios(copia));
        var ctx = planComandos.prepare(new GenerateWeeklyPlanCommand(u, S1.plusWeeks(4), null, true, null, null)).context();
        var independiente = new ValidacionIndependiente(jdbc).validar(s5, ctx, validador, 30);
        assertEquals("VALIDA_CON_AUTORIZACIONES", independiente.estado(), independiente.texto());
    }

    // ------------------------------------------------------------------ IA simulada (politica de la campana)

    private void iaSimulada() {
        when(ia.isEnabled()).thenReturn(true);
        when(ia.modelName()).thenReturn("SIMULADA-c2");
        when(ia.pedirRespuesta(any(), any(), any(), any(), anyInt()))
                .thenReturn(new ReplicateSessionFixGenerator.RawFix("{}", "SIMULADA"));
        when(ia.interpretar(anyString(), any())).thenAnswer(inv -> corregir(inv.getArgument(1)));
    }

    private List<CorrectedSession> corregir(PlanGenerationContext ctx) {
        var salida = new ArrayList<CorrectedSession>();
        for (var s : ctx.adjustment().sesionesParaIA()) {
            long id = s.plannedWorkoutId();
            var lista = new ArrayList<>(ejercicios(id));
            if (s.intervencion() == TipoIntervencion.ACORTAR_O_REORGANIZAR)
                for (int k = lista.size() - 1; k >= 0; k--)
                    if (MandatorySlot.ofMovementPattern(String.valueOf(jdbc.queryForObject("SELECT movement_pattern"
                            + " FROM exercises WHERE exercise_id = ?", String.class, lista.get(k).exerciseId()))).isEmpty()) {
                        lista.remove(k);
                        break;
                    }
            var w = jdbc.queryForMap("SELECT scheduled_date, focus_code FROM planned_workouts WHERE planned_workout_id = ?", id);
            var fecha = ((java.sql.Date) w.get("scheduled_date")).toLocalDate().plusWeeks(1);
            int min = estimador.estimateMinutes(new PlanDraft.DraftWorkout(fecha,
                    WorkoutFocus.valueOf((String) w.get("focus_code")), "x", 0, lista), ctx.prescription());
            salida.add(new CorrectedSession(id, fecha, min, lista, "SIMULADA " + s.intervencion()));
        }
        return salida;
    }

    // ------------------------------------------------------------------ apoyo

    private long usuario(String nombre) {
        String email = "c2_" + nombre + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', ?, now(), now())", email, nombre);
        return jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
    }

    private List<Long> workouts(long planId) {
        return jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ? ORDER BY scheduled_date",
                Long.class, planId);
    }

    private List<PlanDraft.DraftExercise> ejercicios(long w) {
        return jdbc.query("SELECT exercise_id, prescription_type, planned_sets, planned_reps, planned_duration_seconds,"
                        + " rest_seconds FROM planned_workout_exercises WHERE planned_workout_id = ? ORDER BY exercise_order",
                (rs, k) -> new PlanDraft.DraftExercise(rs.getLong(1), PrescriptionType.valueOf(rs.getString(2)),
                        rs.getObject(3, Short.class), rs.getObject(4, Short.class), rs.getObject(5, Integer.class), null,
                        rs.getObject(6, Short.class), null), w);
    }

    private void ejecutarTodo(long u, long plan, SkipReason marca) {
        for (var w : workouts(plan)) hecho(u, w, marca);
    }

    /** Sesion completa; con {@code marca}, el primer ejercicio a la mitad con ese motivo (como la campana). */
    private void hecho(long u, long w, SkipReason marca) {
        registrar(u, w, 1.0, null, marca, marca == null ? 6 : 9);
    }

    private void parcial(long u, long w) {
        registrar(u, w, 0.35, SkipReason.LACK_OF_TIME, null, 6);
    }

    private void registrar(long u, long w, double fraccion, SkipReason motivoParcial, SkipReason marca, int rpe) {
        RelojPruebas.abrirVentanaDeEjecucion(jdbc, u);
        long s = sesiones.handle(new StartWorkoutSessionCommand(u, w)).orElseThrow().getId();
        var filas = jdbc.queryForList("SELECT planned_exercise_id, prescription_type, planned_sets, planned_reps,"
                + " planned_duration_seconds FROM planned_workout_exercises WHERE planned_workout_id = ?"
                + " ORDER BY exercise_order", w);
        double objetivo = fraccion * filas.size();
        for (int k = 0; k < filas.size(); k++) {
            var f = filas.get(k);
            boolean duracion = "DURATION".equals(f.get("prescription_type"));
            int series = f.get("planned_sets") == null ? 1 : ((Number) f.get("planned_sets")).intValue();
            int total = series * ((Number) f.get(duracion ? "planned_duration_seconds" : "planned_reps")).intValue();
            double parte = Math.max(0, Math.min(1, objetivo - k));
            SkipReason motivo = parte >= 1 ? null : motivoParcial;
            if (k == 0 && marca != null) {
                motivo = marca;
                parte = 0.5;
            }
            int hecho = (int) Math.round(total * parte);
            sesiones.handle(new RecordSessionExerciseCommand(u, s, ((Number) f.get("planned_exercise_id")).longValue(),
                    (short) (hecho == 0 ? 0 : series), duracion ? null : hecho, duracion ? hecho : null, null, motivo));
        }
        sesiones.handle(new FinishWorkoutSessionCommand(u, s, (short) rpe, (short) 4, (short) 30));
    }

    private Map<String, Object> ultimo(long u) {
        return jdbc.queryForMap("SELECT status, ai_calls, targeted_interventions::text, conservation_report::text"
                + " FROM plan_adaptation_attempts WHERE user_id = ? ORDER BY attempt_id DESC LIMIT 1", u);
    }
}
