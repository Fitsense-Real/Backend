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
                  List<Ejercicio> ejercicios) {
        Sesion withEjercicios(List<Ejercicio> otros) {
            return new Sesion(id, fecha, enfoque, minutos, nombre, estado, otros);
        }
    }

    @Test
    @DisplayName("A01: ADAPT_LOCALIZED de extremo a extremo con GPT-5-mini real")
    void adaptacionReal() throws Exception {
        Files.createDirectories(SALIDA);
        var evidencia = new LinkedHashMap<String, Object>();
        var promptsUsados = new ArrayList<SessionFixPromptBuilder.SessionFixPrompt>();
        var respuestas = new ArrayList<String>();
        doAnswer(inv -> {
            var p = (SessionFixPromptBuilder.SessionFixPrompt) inv.callRealMethod();
            promptsUsados.add(p);
            return p;
        }).when(prompts).build(any(), any(), any(), any());
        doAnswer(inv -> {
            var r = (ReplicateSessionFixGenerator.RawFix) inv.callRealMethod();
            respuestas.add(r.output());
            return r;
        }).when(ia).pedirRespuesta(any(), any(), any(), any(), anyInt());

        var comprobaciones = new ArrayList<Map<String, Object>>();
        evidencia.put("modo", SECO ? "ENSAYO SIN IA (no es A01)" : "A01 REAL");
        evidencia.put("naturaleza", "Ejecucion experimental: registros de ejecucion simulados por los servicios"
                + " reales del backend, con fechas de prueba; no es una semana entrenada por una participante."
                + " La correccion la hace GPT-5-mini real.");
        try {
            // 1. Preparacion: configuracion, perfil y semana medida.
            long u = participante();
            evidencia.put("configuracion", Map.of("modelo", ia.modelName(), "reasoning_effort", "medium",
                    "replicate_habilitado", ia.isEnabled(), "forced_session_minutes", "null (lo decide el ciclo)",
                    "semana_medida", MEDIDA.toString(), "semana_resultante", NUEVA.toString()));
            comprobar(comprobaciones, "configuracion", ia.isEnabled() == !SECO && "gpt-5-mini".equals(ia.modelName()),
                    "habilitado=" + ia.isEnabled() + " modelo=" + ia.modelName());
            evidencia.put("perfil", jdbc.queryForObject("SELECT row_to_json(p)::text FROM user_profiles p"
                    + " WHERE user_id = ?", String.class, u));
            guardar(evidencia);

            var preparada = planComandos.prepare(GenerateWeeklyPlanCommand.firstPlan(u, MEDIDA));
            var borrador = reglas.generate(preparada.context(), List.of());
            validador.validate(borrador, preparada.context(), preparada.divisor());   // lanza si no pasa V1-V25
            long planMedido = planComandos.persist(preparada, new GeneratedPlan(borrador, (short) 1))
                    .orElseThrow().getId();
            var medida = semana(planMedido);
            evidencia.put("plan_inicial", Map.of("plan_id", planMedido, "semana", MEDIDA.toString(),
                    "origen", borrador.source().name(), "validacion", "V1-V25 aprobada (ENFORCE)", "sesiones", medida));
            guardar(evidencia);
            assertEquals(List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                    medida.stream().map(s -> s.fecha().getDayOfWeek()).toList(), "premisa: lunes, miercoles y viernes");

            // Factibilidad: el viernes tiene un relleno, y la semana sin el pasa la validacion estricta.
            var viernes = medida.get(2);
            var relleno = relleno(viernes).orElseThrow(() -> new AssertionError("el viernes no tiene relleno"));
            var ctxNueva = planComandos.prepare(new GenerateWeeklyPlanCommand(u, NUEVA, null, false, null, null)).context();
            var exentoV20 = new ConservationReport(List.of(), List.of(), List.of(), List.of(),
                    Set.of(viernes.fecha().plusWeeks(1)));
            var factible = new ArrayList<PlanDraft.DraftWorkout>();
            for (var s : medida)
                factible.add(borradorDe(s, s.fecha().plusWeeks(1),
                        s.ejercicios().stream().filter(e -> s != viernes || e != relleno).toList(), ctxNueva));
            // Con la misma exencion que aplica P2-D: la sesion autorizada a acortarse no tiene minimo de V20.
            validador.validate(new PlanDraft(GenerationSource.RULE_ENGINE, null, borrador.planName(), null,
                    "factibilidad", factible), ctxNueva, preparada.divisor(), exentoV20);
            evidencia.put("factibilidad", Map.of("relleno_del_viernes", relleno.id(),
                    "semana_sin_relleno", "V1-V25 aprobada, con el viernes exento de V20 como en P2-D;"
                            + " V8C admite retirar UNA colocacion de relleno en ACORTAR_O_REORGANIZAR"));
            evidencia.put("clasificacion_viernes", clasificacion(viernes, preparada.context()));
            guardar(evidencia);

            // 2. Ejecucion por los servicios reales.
            ejecutar(u, medida.get(0));
            ejecutar(u, medida.get(1));
            planes.skip(UserDetailsImpl.build(usuarios.findById(u).orElseThrow()), viernes.id(),
                    new SkipWorkoutResource(SkipReason.LACK_OF_TIME));
            evidencia.put("ejecucion", jdbc.queryForList("SELECT row_to_json(x)::text FROM (SELECT"
                    + " pw.planned_workout_id, pw.scheduled_date, pw.status, pw.skip_reason, ws.session_id,"
                    + " ws.status AS session_status, ws.completion_percentage, ws.counts_toward_adherence,"
                    + " ws.session_rpe FROM planned_workouts pw LEFT JOIN workout_sessions ws"
                    + " ON ws.planned_workout_id = pw.planned_workout_id WHERE pw.plan_id = ?"
                    + " ORDER BY pw.scheduled_date) x", String.class, planMedido));
            evidencia.put("ejercicios_ejecutados", jdbc.queryForList("SELECT row_to_json(x)::text FROM (SELECT"
                    + " wse.* FROM workout_session_exercises wse JOIN workout_sessions ws ON ws.session_id ="
                    + " wse.session_id WHERE ws.user_id = ? ORDER BY wse.session_exercise_id) x", String.class, u));
            var historial = historia(u, planMedido);
            guardar(evidencia);

            // 3. El ciclo real.
            var publicado = ciclo.runFor(u, MEDIDA, NUEVA);

            // 5. Evidencia.
            evidencia.put("metricas", jdbc.queryForObject("SELECT row_to_json(m)::text FROM weekly_user_metrics m"
                    + " WHERE user_id = ? AND week_start_date = ?", String.class, u, MEDIDA));
            var intento = jdbc.queryForMap("SELECT attempt_id, weekly_metric_id, source_plan_id, new_week_start_date,"
                    + " resulting_plan_id, decision, status, generation_source, ai_calls, targeted_sessions::text,"
                    + " targeted_interventions::text, applied_sessions::text, no_effect_sessions::text,"
                    + " conserved_sessions::text, conservation_report::text, rejection_reason,"
                    + " observed_adherence_pct, registration_coverage_pct FROM plan_adaptation_attempts"
                    + " WHERE user_id = ?", u);
            evidencia.put("plan_adaptation_attempts", intento);
            evidencia.put("intervencion", jdbc.queryForList("SELECT row_to_json(i)::text FROM user_interventions i"
                    + " WHERE user_id = ?", String.class, u));
            evidencia.put("predicciones", respuestas.size());
            var nueva = publicado.map(this::semana).orElse(List.of());
            evidencia.put("plan_resultante", publicado.map(id -> Map.of("plan_id", id, "semana", NUEVA.toString(),
                    "plan", jdbc.queryForObject("SELECT row_to_json(w)::text FROM weekly_training_plans w"
                            + " WHERE plan_id = ?", String.class, id),
                    "sesiones", nueva)).orElse(null));
            evidencia.put("diferencias", diferencias(medida, nueva));
            guardar(evidencia);

            // Revalidacion de la semana publicada con el validador de produccion (V1-V25, 4D en ENFORCE).
            if (!nueva.isEmpty()) {
                var ctxRevision = planComandos.prepare(new GenerateWeeklyPlanCommand(u, NUEVA, null, true, null,
                        null)).context();
                var publicada = new PlanDraft(GenerationSource.AI, null, borrador.planName(), null, "revision",
                        nueva.stream().map(s -> borradorDe(s, s.fecha(), s.ejercicios(), null)).toList());
                List<String> problemas;
                try {
                    validador.validate(publicada, ctxRevision, preparada.divisor(), exentoV20);
                    problemas = List.of();
                } catch (main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException e) {
                    problemas = e.problems();
                }
                evidencia.put("revalidacion_semana_publicada", Map.of("validador", "V1-V25 de produccion,"
                        + " viernes exento de V20 como en P2-D", "problemas", problemas));
                comprobar(comprobaciones, "revalidacion V1-V25 de la semana publicada", problemas.isEmpty(),
                        problemas.toString());
            }

            // 4. Comprobaciones: todas se evaluan y quedan en la evidencia.
            comprobar(comprobaciones, "decision ADAPT_LOCALIZED", "ADAPT_LOCALIZED".equals(intento.get("decision")),
                    String.valueOf(intento.get("decision")));
            comprobar(comprobaciones, "solo el viernes senalado",
                    ("{" + viernes.id() + "}").equals(intento.get("targeted_sessions")),
                    String.valueOf(intento.get("targeted_sessions")));
            comprobar(comprobaciones, "intervencion ACORTAR_O_REORGANIZAR",
                    "{ACORTAR_O_REORGANIZAR}".equals(intento.get("targeted_interventions")),
                    String.valueOf(intento.get("targeted_interventions")));
            comprobar(comprobaciones, "GPT-5-mini devolvio propuesta", !respuestas.isEmpty(),
                    respuestas.size() + " prediccion(es)");
            comprobar(comprobaciones, "como mucho tres predicciones", respuestas.size() <= 3,
                    respuestas.size() + " prediccion(es)");
            comprobar(comprobaciones, "estado APLICADA, origen AI (V8C y estricta aprobadas)",
                    "APLICADA".equals(intento.get("status")) && "AI".equals(intento.get("generation_source")),
                    intento.get("status") + " / " + intento.get("generation_source") + " / "
                            + intento.get("rejection_reason"));
            comprobar(comprobaciones, "plan publicado y enlazado al de origen", publicado.isPresent()
                            && publicado.get().equals(numero(intento.get("resulting_plan_id")))
                            && Long.valueOf(planMedido).equals(numero(intento.get("source_plan_id"))),
                    "publicado=" + publicado + " origen=" + intento.get("source_plan_id"));
            var metricas = jdbc.queryForMap("SELECT observed_adherence_pct, registration_coverage_pct,"
                    + " weighted_adherence_pct FROM weekly_user_metrics WHERE user_id = ? AND week_start_date = ?",
                    u, MEDIDA);
            comprobar(comprobaciones, "adherencia observada 66,67 % y cobertura 100 %",
                    new BigDecimal("66.67").compareTo((BigDecimal) metricas.get("observed_adherence_pct")) == 0
                            && new BigDecimal("100.00").compareTo((BigDecimal) metricas.get("registration_coverage_pct")) == 0,
                    metricas.toString());
            if (nueva.size() == 3) {
                for (int i = 0; i < 2; i++) {
                    var antes = medida.get(i);
                    var despues = nueva.get(i);
                    comprobar(comprobaciones, antes.fecha().getDayOfWeek() + " intacto",
                            antes.ejercicios().equals(despues.ejercicios()) && antes.minutos() == despues.minutos()
                                    && antes.enfoque().equals(despues.enfoque())
                                    && antes.fecha().plusWeeks(1).equals(despues.fecha()),
                            antes.ejercicios() + " -> " + despues.ejercicios());
                }
                var nuevoViernes = nueva.get(2);
                var retirados = viernes.ejercicios().stream()
                        .filter(e -> nuevoViernes.ejercicios().stream().noneMatch(d -> d.id() == e.id())).toList();
                var mantenidos = viernes.ejercicios().stream().filter(e -> !retirados.contains(e)).toList();
                comprobar(comprobaciones, "viernes: mismo dia", viernes.fecha().plusWeeks(1).equals(nuevoViernes.fecha()),
                        String.valueOf(nuevoViernes.fecha()));
                comprobar(comprobaciones, "viernes: una sola colocacion retirada y es de relleno",
                        retirados.size() == 1 && relleno(viernes.withEjercicios(retirados)).isPresent(),
                        "retirados=" + retirados);
                comprobar(comprobaciones, "viernes: sin ejercicios anadidos", nuevoViernes.ejercicios().stream()
                        .allMatch(d -> viernes.ejercicios().stream().anyMatch(e -> e.id() == d.id())),
                        String.valueOf(nuevoViernes.ejercicios()));
                comprobar(comprobaciones, "viernes: las que se quedan conservan su dosis",
                        mantenidos.equals(nuevoViernes.ejercicios()), mantenidos + " -> " + nuevoViernes.ejercicios());
            } else {
                comprobar(comprobaciones, "semana resultante con tres sesiones", false, "sesiones=" + nueva.size());
            }
            for (var s : nueva)
                comprobar(comprobaciones, "sesion " + s.id() + " no bloqueada y con linaje V31",
                        !planning.fetchPlannedWorkout(s.id()).orElseThrow().blockedForSafety()
                                && jdbc.queryForObject("SELECT source_planned_workout_id FROM planned_workouts"
                                + " WHERE planned_workout_id = ?", Long.class, s.id()) != null, "");
            comprobar(comprobaciones, "una sola version vigente", jdbc.queryForObject("SELECT count(*) FROM"
                    + " weekly_training_plans WHERE user_id = ? AND week_start_date = ? AND status = 'ACTIVE'",
                    Integer.class, u, NUEVA) == 1, "");
            comprobar(comprobaciones, "intervencion enlazada al plan nuevo", publicado.isPresent()
                    && jdbc.queryForObject("SELECT count(*) FROM user_interventions WHERE user_id = ?"
                    + " AND resulting_plan_id = ?", Integer.class, u, publicado.get()) == 1, "");
            comprobar(comprobaciones, "historial de la semana medida intacto",
                    historial.equals(historia(u, planMedido)), "");
        } finally {
            evidencia.put("comprobaciones", comprobaciones);
            guardar(evidencia);
            for (int i = 0; i < promptsUsados.size(); i++) {
                Files.writeString(SALIDA.resolve("prompt_intento_" + (i + 1) + ".txt"), promptsUsados.get(i).text());
                Files.writeString(SALIDA.resolve("snapshot_intento_" + (i + 1) + ".json"),
                        promptsUsados.get(i).snapshotJson());
            }
            for (int i = 0; i < respuestas.size(); i++)
                Files.writeString(SALIDA.resolve("respuesta_intento_" + (i + 1) + ".json"), respuestas.get(i));
        }
        var fallidas = comprobaciones.stream().filter(c -> !(Boolean) c.get("ok")).toList();
        assertTrue(fallidas.isEmpty(), "comprobaciones fallidas: " + fallidas);
    }

    private static void comprobar(List<Map<String, Object>> lista, String nombre, boolean ok, String detalle) {
        lista.add(Map.of("comprobacion", nombre, "ok", ok, "detalle", detalle));
    }

    private static Long numero(Object valor) {
        return valor == null ? null : ((Number) valor).longValue();
    }

    /** Escribe la evidencia ya reunida. Una parte que no se pueda serializar no tumba las demas. */
    private void guardar(Map<String, Object> evidencia) throws java.io.IOException {
        var seguro = new LinkedHashMap<String, Object>();
        evidencia.forEach((clave, valor) -> {
            try {
                json.write(valor);
                seguro.put(clave, valor);
            } catch (RuntimeException e) {
                seguro.put(clave, "NO SERIALIZABLE: " + e.getMessage() + " · " + valor);
            }
        });
        Files.writeString(SALIDA.resolve("evidencia.json"), json.write(seguro));
    }

    private PlanDraft.DraftWorkout borradorDe(Sesion s, LocalDate fecha, List<Ejercicio> ejercicios,
                                              PlanGenerationContext ctx) {
        var ej = ejercicios.stream().map(Ejercicio::borrador).toList();
        int minutos = ctx == null ? s.minutos()
                : estimador.estimateMinutes(new PlanDraft.DraftWorkout(fecha, WorkoutFocus.valueOf(s.enfoque()),
                s.nombre(), 0, ej), ctx.prescription());
        return new PlanDraft.DraftWorkout(fecha, WorkoutFocus.valueOf(s.enfoque()), s.nombre(), minutos, ej);
    }

    /** Cada colocacion del viernes con su ficha de catalogo, su slot de 4C y su clase de 4D. */
    private List<Map<String, Object>> clasificacion(Sesion viernes, PlanGenerationContext ctx) {
        var salida = new ArrayList<Map<String, Object>>();
        for (var e : viernes.ejercicios()) {
            var candidato = ctx.availableExercises().stream().filter(c -> c.exerciseId() == e.id()).findFirst();
            var patron = jdbc.queryForObject("SELECT movement_pattern FROM exercises WHERE exercise_id = ?",
                    String.class, e.id());
            salida.add(Map.of(
                    "colocacion", e.toString(),
                    "catalogo", jdbc.queryForObject("SELECT row_to_json(x)::text FROM exercises x"
                            + " WHERE exercise_id = ?", String.class, e.id()),
                    "slot_4C", MandatorySlot.ofMovementPattern(patron).map(Object::toString).orElse("ninguno (relleno)"),
                    "clase_4D", candidato.map(c -> String.valueOf(main.web.services.fitsense.planning.domain.services
                            .CapabilityPrescriptionPolicy.claseDe(c))).orElse("no elegible"),
                    "dosis_4D_exigida", candidato.flatMap(c -> MvpDosePolicy.dosisPara(MandatorySlot.of(c),
                            c.bodyPartCode())).map(MvpDosePolicy.Dosis::describe).orElse("-")));
        }
        return salida;
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
