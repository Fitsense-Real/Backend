package main.web.services.fitsense.orchestation;

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
import main.web.services.fitsense.planning.infrastructure.generation.ai.SessionFixPromptBuilder;
import main.web.services.fitsense.planning.infrastructure.generation.rules.RuleBasedTrainingPlanGenerator;
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
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;

/**
 * A01 · PRIMERA ADAPTACION REAL CON GPT-5-MINI. Llama a Replicate y cuesta
 * dinero: solo corre con {@code -Dfitsense.replicate.real=true}, nunca en la
 * suite normal.
 * <p>
 * Base PostgreSQL temporal (Testcontainers), nunca Neon. Nada simulado en la
 * decision, la correccion ni la validacion: el ciclo es
 * {@link WeeklyCycleService#runFor}. Los dos espias solo copian lo que
 * devuelven el constructor del prompt y el generador de correcciones reales,
 * para la evidencia.
 * <p>
 * La semana medida la produce el motor de reglas por los servicios de
 * produccion (preparar, generar, validar, persistir) para no pagar la
 * generacion inicial. Semanas: la siguiente a la actual y la posterior (el
 * lunes de hace 8 dias ya no admite reporte retroactivo, V16).
 */
@Testcontainers
@EnabledIfSystemProperty(named = "fitsense.replicate.real", matches = "true")
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=true",
        // El proxy del entorno pone el token real; el backend solo exige que no este vacio.
        "fitsense.ai.api-token=proxy",
        "fitsense.ai.model=gpt-5-mini",
        "fitsense.ai.reasoning-effort=medium"
})
class A01AdaptacionRealIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @org.springframework.test.context.DynamicPropertySource
    static void ensayo(org.springframework.test.context.DynamicPropertyRegistry registro) {
        if (SECO) registro.add("fitsense.ai.enabled", () -> "false");
    }

    @MockitoSpyBean SessionFixPromptBuilder prompts;
    @MockitoSpyBean ReplicateSessionFixGenerator ia;

    @Autowired WeeklyCycleService ciclo;
    @Autowired PlanningContextFacade planning;
    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired RuleBasedTrainingPlanGenerator reglas;
    @Autowired PlanDraftValidator validador;
    @Autowired SessionDurationEstimator estimador;
    @Autowired UserProfileCommandService perfiles;
    @Autowired WorkoutSessionCommandService sesiones;
    @Autowired TrainingPlanController planes;
    @Autowired UserRepository usuarios;
    @Autowired JsonSupport json;
    @Autowired JdbcTemplate jdbc;

    static final LocalDate MEDIDA = TrainingWeek.containing(LocalDate.now()).startDate().plusWeeks(1);
    static final LocalDate NUEVA = MEDIDA.plusWeeks(1);
    static final List<String> EQUIPAMIENTO = List.of("dumbbell", "barbell", "cable", "leverage machine", "bench", "band");
    /** Ensayo gratuito del arnes: la IA queda apagada y el ciclo conserva (RESPALDO). */
    static final boolean SECO = Boolean.getBoolean("fitsense.a01.seco");
    static final Path SALIDA = Path.of(System.getProperty("fitsense.a01.salida", "target/a01"));

    record Ejercicio(long id, String tipo, Short series, Short reps, Integer segundos, Short descanso) {
        PlanDraft.DraftExercise borrador() {
            return new PlanDraft.DraftExercise(id, PrescriptionType.valueOf(tipo), series, reps, segundos,
                    null, descanso, null);
        }
    }

    record Sesion(long id, LocalDate fecha, String enfoque, int minutos, String nombre, String estado,
                  List<Ejercicio> ejercicios) {}

    @Test
    @DisplayName("A01: ADAPT_LOCALIZED de extremo a extremo con GPT-5-mini real")
    void adaptacionReal() throws Exception {
        Files.createDirectories(SALIDA);
        var evidencia = new LinkedHashMap<String, Object>();
        var promptsUsados = new ArrayList<Map<String, Object>>();
        var respuestas = new ArrayList<Map<String, Object>>();
        doAnswer(inv -> {
            var p = (SessionFixPromptBuilder.SessionFixPrompt) inv.callRealMethod();
            promptsUsados.add(Map.of("texto", p.text(), "snapshot", p.snapshotJson()));
            return p;
        }).when(prompts).build(any(), any(), any(), any());
        doAnswer(inv -> {
            var r = (ReplicateSessionFixGenerator.RawFix) inv.callRealMethod();
            respuestas.add(Map.of("intento", inv.getArgument(4), "salida", r.output()));
            return r;
        }).when(ia).pedirRespuesta(any(), any(), any(), any(), anyInt());

        // 1. Preparacion: configuracion, perfil y semana medida.
        assertEquals(!SECO, ia.isEnabled(), "Replicate habilitado");
        assertEquals("gpt-5-mini", ia.modelName());
        long u = participante();
        evidencia.put("configuracion", Map.of("modelo", ia.modelName(), "reasoning_effort", "medium",
                "replicate_habilitado", ia.isEnabled(), "forced_session_minutes", "null (lo decide el ciclo)"));
        evidencia.put("perfil", jdbc.queryForMap("SELECT user_id, birth_date, biological_sex, height_cm, current_weight_kg,"
                + " fitness_level, goal_type, training_location, days_per_week, available_days::text,"
                + " session_minutes, equipment_codes::text, blocked_exercise_ids::text, health_notes FROM user_profiles WHERE user_id = ?", u));

        var preparada = planComandos.prepare(GenerateWeeklyPlanCommand.firstPlan(u, MEDIDA));
        var borrador = reglas.generate(preparada.context(), List.of());
        validador.validate(borrador, preparada.context(), preparada.divisor());   // lanza si no pasa V1-V25
        long planMedido = planComandos.persist(preparada, new GeneratedPlan(borrador, (short) 1))
                .orElseThrow().getId();
        var medida = semana(planMedido);
        evidencia.put("plan_inicial", Map.of("plan_id", planMedido, "semana", MEDIDA.toString(),
                "origen", borrador.source().name(), "validacion", "V1-V25 aprobada (ENFORCE)", "sesiones", medida));
        assertEquals(List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                medida.stream().map(s -> s.fecha().getDayOfWeek()).toList(), "premisa: lunes, miercoles y viernes");

        // Factibilidad: el viernes tiene un relleno, y la semana sin el pasa la validacion estricta.
        var viernes = medida.get(2);
        var relleno = relleno(viernes).orElseThrow(() -> new AssertionError("el viernes no tiene relleno que retirar"));
        var ctxNueva = planComandos.prepare(new GenerateWeeklyPlanCommand(u, NUEVA, null, false, null, null)).context();
        var factible = new ArrayList<PlanDraft.DraftWorkout>();
        for (var s : medida) {
            var ej = s.ejercicios().stream().filter(e -> s != viernes || e != relleno).map(Ejercicio::borrador).toList();
            var w = new PlanDraft.DraftWorkout(s.fecha().plusWeeks(1), WorkoutFocus.valueOf(s.enfoque()), s.nombre(), 0, ej);
            factible.add(new PlanDraft.DraftWorkout(w.scheduledDate(), w.focus(), w.name(),
                    estimador.estimateMinutes(w, ctxNueva.prescription()), ej));
        }
        // Con la misma exencion que aplica P2-D: la sesion autorizada a acortarse no tiene minimo de V20.
        validador.validate(new PlanDraft(GenerationSource.RULE_ENGINE, null, borrador.planName(), null,
                "factibilidad", factible), ctxNueva, preparada.divisor(), new ConservationReport(List.of(),
                List.of(), List.of(), List.of(), Set.of(viernes.fecha().plusWeeks(1))));
        evidencia.put("factibilidad", Map.of("relleno_del_viernes", relleno.id(),
                "semana_sin_relleno", "V1-V25 aprobada, con el viernes exento de V20 como en P2-D;"
                        + " V8C admite retirar UNA colocacion de relleno en ACORTAR_O_REORGANIZAR"));

        // 2. Ejecucion por los servicios reales.
        var historial = new ArrayList<Map<String, Object>>();
        ejecutar(u, medida.get(0));
        ejecutar(u, medida.get(1));
        planes.skip(UserDetailsImpl.build(usuarios.findById(u).orElseThrow()), viernes.id(),
                new SkipWorkoutResource(SkipReason.LACK_OF_TIME));
        var ejecucion = jdbc.queryForList("SELECT pw.planned_workout_id, pw.scheduled_date, pw.status, pw.skip_reason,"
                + " ws.session_id, ws.status AS session_status, ws.completion_percentage, ws.counts_toward_adherence"
                + " FROM planned_workouts pw LEFT JOIN workout_sessions ws ON ws.planned_workout_id = pw.planned_workout_id"
                + " WHERE pw.plan_id = ? ORDER BY pw.scheduled_date", planMedido);
        evidencia.put("ejecucion", ejecucion);
        historial.addAll(historia(u, planMedido));

        // 3. El ciclo real.
        var publicado = ciclo.runFor(u, MEDIDA, NUEVA);

        // 5. Evidencia antes de comprobar nada: un fallo tambien se reporta.
        evidencia.put("metricas", jdbc.queryForMap("SELECT weekly_metric_id, weighted_adherence_pct,"
                + " observed_adherence_pct, registration_coverage_pct, risk_score, risk_level"
                + " FROM weekly_user_metrics WHERE user_id = ? AND week_start_date = ?", u, MEDIDA));
        var intento = jdbc.queryForMap("SELECT attempt_id, weekly_metric_id, source_plan_id, new_week_start_date,"
                + " resulting_plan_id, decision, status, generation_source, ai_calls, targeted_sessions::text,"
                + " targeted_interventions::text, applied_sessions::text, no_effect_sessions::text,"
                + " conserved_sessions::text, conservation_report::text, rejection_reason, observed_adherence_pct,"
                + " registration_coverage_pct FROM plan_adaptation_attempts WHERE user_id = ?", u);
        evidencia.put("plan_adaptation_attempts", intento);
        evidencia.put("intervencion", jdbc.queryForList("SELECT row_to_json(i)::text FROM user_interventions i"
                + " WHERE user_id = ?", String.class, u));
        evidencia.put("prompts", promptsUsados);
        evidencia.put("respuestas_replicate", respuestas);
        var nueva = publicado.map(this::semana).orElse(List.of());
        evidencia.put("plan_resultante", publicado.map(id -> Map.of("plan_id", id, "semana", NUEVA.toString(),
                "plan", jdbc.queryForMap("SELECT plan_id, week_start_date, plan_version, parent_plan_id, status,"
                        + " generation_source, model_name, generation_attempts, adjustment_applied, adjustment_reason,"
                        + " output_snapshot::text FROM weekly_training_plans WHERE plan_id = ?", id),
                "sesiones", nueva)).orElse(null));
        evidencia.put("diferencias", diferencias(medida, nueva));
        Files.writeString(SALIDA.resolve("evidencia.json"), json.write(evidencia));
        if (SECO) return;   // ensayo sin IA: solo comprueba que la evidencia se escribe

        // 4. Comprobaciones.
        assertEquals("ADAPT_LOCALIZED", intento.get("decision"), String.valueOf(intento));
        assertEquals("{" + viernes.id() + "}", intento.get("targeted_sessions"), "solo el viernes");
        assertEquals("{ACORTAR_O_REORGANIZAR}", intento.get("targeted_interventions"));
        assertFalse(respuestas.isEmpty(), "no hubo respuesta de GPT-5-mini");
        assertEquals("APLICADA", intento.get("status"), "resultado: " + intento);
        assertEquals("AI", intento.get("generation_source"));
        long planNuevo = publicado.orElseThrow();
        assertEquals(planNuevo, ((Number) intento.get("resulting_plan_id")).longValue());
        assertEquals(planMedido, ((Number) intento.get("source_plan_id")).longValue());

        for (int i = 0; i < 2; i++) {
            assertEquals(medida.get(i).ejercicios(), nueva.get(i).ejercicios(), "cambio " + medida.get(i).fecha());
            assertEquals(medida.get(i).minutos(), nueva.get(i).minutos());
            assertEquals(medida.get(i).enfoque(), nueva.get(i).enfoque());
        }
        var idsMedida = medida.stream().flatMap(s -> s.ejercicios().stream()).map(Ejercicio::id).toList();
        assertTrue(nueva.stream().flatMap(s -> s.ejercicios().stream()).allMatch(e -> idsMedida.contains(e.id())),
                "entro un ejercicio que no estaba en la semana medida");
        assertNotEquals(viernes.ejercicios(), nueva.get(2).ejercicios(), "el viernes no cambio");
        assertEquals(viernes.fecha().plusWeeks(1), nueva.get(2).fecha(), "ACORTAR no cambia el dia");
        for (var s : nueva) {
            assertFalse(planning.fetchPlannedWorkout(s.id()).orElseThrow().blockedForSafety());
            assertNotNull(jdbc.queryForObject("SELECT source_planned_workout_id FROM planned_workouts"
                    + " WHERE planned_workout_id = ?", Long.class, s.id()), "linaje V31");
        }
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ?"
                + " AND week_start_date = ? AND status = 'ACTIVE'", Integer.class, u, NUEVA));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM user_interventions WHERE user_id = ?"
                + " AND resulting_plan_id = ?", Integer.class, u, planNuevo));
        assertEquals(historial, historia(u, planMedido), "cambio el historial de la semana medida");
        assertEquals(0, new BigDecimal("66.67").compareTo((BigDecimal) ((Map<?, ?>) evidencia.get("metricas"))
                .get("observed_adherence_pct")));
        assertEquals(0, new BigDecimal("100.00").compareTo((BigDecimal) ((Map<?, ?>) evidencia.get("metricas"))
                .get("registration_coverage_pct")));
    }

    private long participante() {
        String email = "a01-" + System.nanoTime() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long userId = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(userId, LocalDate.of(1992, 5, 10), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("68"), null, FitnessLevel.INTERMEDIATE, GoalType.GAIN_MUSCLE,
                "prueba de adaptacion", TrainingLocation.GYM, (short) 3,
                List.of((short) 1, (short) 3, (short) 5), (short) 45, EQUIPAMIENTO,
                List.of(), "Ninguna", true, true, true, true, true));
        return userId;
    }

    private List<Sesion> semana(long planId) {
        var filas = jdbc.queryForList("SELECT planned_workout_id, scheduled_date, focus_code, workout_name, status,"
                + " expected_duration_minutes FROM planned_workouts WHERE plan_id = ? ORDER BY scheduled_date", planId);
        var lista = new ArrayList<Sesion>();
        for (var f : filas) {
            long id = ((Number) f.get("planned_workout_id")).longValue();
            var ejercicios = jdbc.query("SELECT exercise_id, prescription_type, planned_sets, planned_reps,"
                            + " planned_duration_seconds, rest_seconds FROM planned_workout_exercises"
                            + " WHERE planned_workout_id = ? ORDER BY exercise_order",
                    (rs, k) -> new Ejercicio(rs.getLong(1), rs.getString(2), rs.getObject(3, Short.class),
                            rs.getObject(4, Short.class), rs.getObject(5, Integer.class),
                            rs.getObject(6, Short.class)), id);
            lista.add(new Sesion(id, ((java.sql.Date) f.get("scheduled_date")).toLocalDate(),
                    (String) f.get("focus_code"), ((Number) f.get("expected_duration_minutes")).intValue(),
                    (String) f.get("workout_name"), (String) f.get("status"), ejercicios));
        }
        return lista;
    }

    /** Toda la sesion, al 100 %: lo indicado de cada ejercicio. */
    private void ejecutar(long u, Sesion s) {
        long sesion = sesiones.handle(new StartWorkoutSessionCommand(u, s.id())).orElseThrow().getId();
        var pes = jdbc.queryForList("SELECT planned_exercise_id FROM planned_workout_exercises"
                + " WHERE planned_workout_id = ? ORDER BY exercise_order", Long.class, s.id());
        for (int k = 0; k < pes.size(); k++) {
            var e = s.ejercicios().get(k);
            boolean duracion = "DURATION".equals(e.tipo());
            int series = e.series() == null ? 1 : e.series();
            sesiones.handle(new RecordSessionExerciseCommand(u, sesion, pes.get(k), (short) series,
                    duracion ? null : series * e.reps(), duracion ? series * e.segundos() : null, null, null));
        }
        sesiones.handle(new FinishWorkoutSessionCommand(u, sesion, (short) 6, (short) 4, (short) 45));
    }

    /** El relleno: la ultima colocacion cuyo patron no es un slot de 4C (igual que AdaptacionCompletaIT). */
    private Optional<Ejercicio> relleno(Sesion s) {
        for (int k = s.ejercicios().size() - 1; k >= 0; k--) {
            var e = s.ejercicios().get(k);
            var patron = jdbc.queryForObject("SELECT movement_pattern FROM exercises WHERE exercise_id = ?",
                    String.class, e.id());
            if (MandatorySlot.ofMovementPattern(patron).isEmpty()) return Optional.of(e);
        }
        return Optional.empty();
    }

    private List<Map<String, Object>> historia(long u, long planMedido) {
        var filas = new ArrayList<Map<String, Object>>();
        filas.addAll(jdbc.queryForList("SELECT session_id, planned_workout_id, attempt_number, status,"
                + " completion_percentage, counts_toward_adherence, session_rpe FROM workout_sessions"
                + " WHERE user_id = ? ORDER BY session_id", u));
        filas.addAll(jdbc.queryForList("SELECT wse.* FROM workout_session_exercises wse JOIN workout_sessions ws"
                + " ON ws.session_id = wse.session_id WHERE ws.user_id = ? ORDER BY wse.session_exercise_id", u));
        filas.addAll(jdbc.queryForList("SELECT pw.planned_workout_id, pw.scheduled_date, pwe.exercise_id,"
                + " pwe.planned_sets, pwe.planned_reps, pwe.planned_duration_seconds FROM planned_workouts pw"
                + " JOIN planned_workout_exercises pwe ON pwe.planned_workout_id = pw.planned_workout_id"
                + " WHERE pw.plan_id = ? ORDER BY pw.scheduled_date, pwe.exercise_order", planMedido));
        return filas;
    }

    /** Ejercicio por ejercicio, de la sesion medida a su derivada. */
    private List<Map<String, Object>> diferencias(List<Sesion> medida, List<Sesion> nueva) {
        var salida = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < medida.size(); i++) {
            var antes = medida.get(i);
            var despues = i < nueva.size() ? nueva.get(i) : null;
            var cambios = new ArrayList<String>();
            if (despues == null) cambios.add("sin sesion derivada");
            else {
                var porId = new LinkedHashMap<Long, Ejercicio>();
                despues.ejercicios().forEach(e -> porId.put(e.id(), e));
                for (var e : antes.ejercicios()) {
                    var d = porId.remove(e.id());
                    if (d == null) cambios.add("retirado " + e);
                    else if (!d.equals(e)) cambios.add("dosis " + e + " -> " + d);
                }
                porId.values().forEach(e -> cambios.add("anadido " + e));
                if (antes.minutos() != despues.minutos())
                    cambios.add("minutos " + antes.minutos() + " -> " + despues.minutos());
            }
            salida.add(Map.of("origen", antes.id(), "fecha", antes.fecha().toString(),
                    "derivada", despues == null ? "-" : despues.id(), "cambios", cambios.isEmpty() ? List.of("identica") : cambios));
        }
        return salida;
    }
}
