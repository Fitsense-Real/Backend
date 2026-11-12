package main.web.services.fitsense.auditoria.caso39;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import main.web.services.fitsense.planning.application.internal.commandservices.GeneracionInicialService;
import main.web.services.fitsense.planning.domain.exceptions.AiProviderUnavailableException;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext;
import main.web.services.fitsense.planning.domain.services.PlanDraftNormalizer;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.infrastructure.generation.ai.AiRetryFeedback;
import main.web.services.fitsense.planning.infrastructure.generation.ai.PlanInputSnapshot;
import main.web.services.fitsense.planning.infrastructure.generation.ai.PlanPromptBuilder;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateTrainingPlanGenerator;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.*;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AUDITORIA DEL CASO REAL DEL USUARIO 39 (12/11/2026). SOLO INVESTIGACION: no cambia el producto.
 * <p>
 * Reproduce, sin Replicate y con el codigo de {@code generation} 623c4fa, la generacion inicial real:
 * <ol>
 *   <li>mismo perfil (userId 39, GENERAL_FITNESS, principiante, gimnasio, 4 dias [1,4,5,7], 60 min, sin colgarse ni
 *       brazos arriba) y misma semana (2026-11-16);</li>
 *   <li>intento 1: el JSON EXACTO de la prediccion jhxmsrkj61rmr0d1t5trg6znj4, copiado del log real;</li>
 *   <li>intento 2: el mismo error que dio el proveedor (sigue en curso tras 150 s, se cancela);</li>
 *   <li>respaldo: motor de reglas y validador reales. Como la semilla depende de userId y semana, el usuario se crea
 *       con user_id = 39 para obtener la MISMA semana que el plan 78.</li>
 * </ol>
 * El equipo exacto del perfil real no se conoce: se usa el de un gimnasio completo (todos los codigos del catalogo) y
 * se comprueba contra la huella del log real (350 elegibles; LOWER_KNEE=27 LOWER_HIP=18 UPPER_PUSH=62 UPPER_PULL=33).
 * <p>
 * Opt-in: {@code -Dfitsense.auditoria.caso39=true}. Escribe sus evidencias en
 * {@code -Dfitsense.auditoria.caso39.salida} (por omision target/caso39).
 */
@Testcontainers
@EnabledIfSystemProperty(named = "fitsense.auditoria.caso39", matches = "true")
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=true",
        "fitsense.ai.api-token=token-de-prueba-sin-uso"
})
@DisplayName("Auditoria caso real usuario 39 · reproduccion sin Replicate")
class Caso39ReproduccionIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @MockitoSpyBean ReplicateTrainingPlanGenerator ia;
    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired GeneracionInicialService generacionInicial;
    @Autowired UserProfileCommandService perfiles;
    @Autowired PlanDraftValidator validador;
    @Autowired PlanDraftNormalizer normalizador;
    @Autowired PlanPromptBuilder promptBuilder;
    @Autowired JsonSupport jsonSupport;
    @Autowired JdbcTemplate jdbc;
    @Autowired @org.springframework.beans.factory.annotation.Qualifier("ruleBasedTrainingPlanGenerator")
    main.web.services.fitsense.planning.domain.services.TrainingPlanGenerator reglas;

    static final long USUARIO = 39L;
    static final LocalDate SEMANA = LocalDate.of(2026, 11, 16);
    static final Path SALIDA = Path.of(System.getProperty("fitsense.auditoria.caso39.salida", "target/caso39"));

    static final List<String> V15_REAL = List.of(
            "V15: el entrenamiento del 2026-11-19 dedica 4 de 6 ejercicios a upper legs. Reparte entre los grupos del enfoque LOWER_BODY.",
            "V15: el entrenamiento del 2026-11-22 dedica 4 de 6 ejercicios a upper legs. Reparte entre los grupos del enfoque LOWER_BODY.");

    @Test
    @DisplayName("caso 39 completo: V15 exacto, feedback del intento 2, respaldo, plan publicado y sus evidencias")
    void caso39() throws Exception {
        Files.createDirectories(SALIDA);
        var logs = capturarLogs();
        String intento1 = recurso("/caso39/intento1_jhxmsrkj61rmr0d1t5trg6znj4.json");

        // ---- perfil real ----------------------------------------------------------------------------------------
        // Equipo: todo el catalogo menos lo indicado en -Dfitsense.auditoria.caso39.equipo-excluido (separado por comas)
        var excluido = Arrays.stream(System.getProperty("fitsense.auditoria.caso39.equipo-excluido", "").split(","))
                .map(String::trim).filter(x -> !x.isEmpty()).collect(Collectors.toSet());
        // -Dfitsense.auditoria.caso39.equipo: lista EXACTA del perfil real (tiene prioridad sobre la exclusion)
        var exacto = System.getProperty("fitsense.auditoria.caso39.equipo", "").trim();
        List<String> equipo = !exacto.isEmpty()
                ? Arrays.stream(exacto.split(",")).map(String::trim).filter(x -> !x.isEmpty()).toList()
                : jdbc.queryForList("SELECT code FROM equipment_types ORDER BY display_order", String.class)
                        .stream().filter(x -> !excluido.contains(x)).toList();
        var desconocidos = equipo.stream().filter(x -> jdbc.queryForObject(
                "SELECT count(*) FROM equipment_types WHERE code = ?", Integer.class, x) == 0).toList();
        assertTrue(desconocidos.isEmpty(), "codigos de equipo que el catalogo no conoce: " + desconocidos);
        crearUsuario39(equipo);

        // ---- contexto: el mismo que recibe el pipeline ------------------------------------------------------------
        var preparado = planComandos.prepare(GenerateWeeklyPlanCommand.firstPlan(USUARIO, SEMANA));
        PlanGenerationContext ctx = preparado.context();
        var elegibles = ctx.availableExercises().stream()
                .collect(Collectors.toMap(CandidateExercise::exerciseId, c -> c, (a, b) -> a, LinkedHashMap::new));
        var resumen = new StringBuilder();
        resumen.append("equipo=").append(equipo).append('\n');
        resumen.append("elegibles=").append(elegibles.size()).append('\n');
        resumen.append("primera_semana_calculada=").append(planComandos.firstPlanWeekStart(USUARIO)).append('\n');
        resumen.append("politica=").append(ctx.policy()).append('\n');
        resumen.append("perfil=").append(ctx.profile()).append('\n');

        // Conjunto elegible con TODO el equipo: slot y requisitos de equipo por ejercicio (para buscar fuera de la
        // prueba el subconjunto de equipo que da la huella real de 350 elegibles).
        var volcado = new StringBuilder("exercise_id;body_part;equipment;slot;sin_patron;requisitos\n");
        var requisitos = new HashMap<Long, String>();
        jdbc.query("""
                SELECT r.exercise_id, string_agg(r.group_no || ':' || eq.code, '|' ORDER BY r.group_no, eq.code) req
                  FROM exercise_equipment_requirements r JOIN equipment_types eq ON eq.equipment_id = r.equipment_id
                 GROUP BY r.exercise_id""", rs -> { requisitos.put(rs.getLong(1), rs.getString(2)); });
        for (var c : elegibles.values()) {
            boolean sinPatron = c.movementPattern() == null || c.movementPattern().isBlank();
            var slot = sinPatron ? "" : main.web.services.fitsense.planning.domain.services.CoberturaDePatron.slot(c)
                    .map(Enum::name).orElse("FUERA");
            volcado.append(c.exerciseId()).append(';').append(c.bodyPartCode()).append(';').append(c.equipmentCode())
                    .append(';').append(slot).append(';').append(sinPatron).append(';')
                    .append(requisitos.getOrDefault(c.exerciseId(), "")).append('\n');
        }
        Files.writeString(SALIDA.resolve("elegibles_todo_el_equipo.csv"), volcado.toString(), StandardCharsets.UTF_8);

        var compl = main.web.services.fitsense.planning.domain.services.CatalogEligibilityPolicy
                .complementos(elegibles.values(), ctx.profile().goalType());
        resumen.append("complementos_4A=").append(new TreeSet<>(compl)).append('\n');
        var snapshotIds = PlanInputSnapshot.of(ctx).availableExercises().stream()
                .map(PlanInputSnapshot.AvailableExercise::exerciseId).collect(Collectors.toSet());
        resumen.append("ejercicios_en_prompt=").append(snapshotIds.size()).append('\n');
        var elegiblesNoEnviados = elegibles.keySet().stream().filter(id -> !snapshotIds.contains(id)).sorted().toList();
        resumen.append("elegibles_no_enviados_al_prompt=").append(elegiblesNoEnviados).append('\n');

        // Huella del log real (SlotCoverageGate)
        var previa = lineas(logs, "Cobertura 4C previa del usuario 39");
        resumen.append("huella_previa_reproducida=").append(previa).append('\n');

        // ---- intento 1: el JSON real con el validador real ---------------------------------------------------------
        var idsIa = new ArrayList<Long>();
        var borradorIa = ia.toDraft(intento1);
        borradorIa.workouts().forEach(w -> w.exercises().forEach(e -> idsIa.add(e.exerciseId())));
        var fueraDelConjunto = idsIa.stream().filter(id -> !elegibles.containsKey(id)).distinct().toList();
        resumen.append("ids_ia_fuera_del_conjunto_elegible=").append(fueraDelConjunto).append('\n');

        var normalizado = normalizador.normalize(borradorIa, ctx).draft();
        var rechazo = assertThrows(InvalidPlanDraftException.class,
                () -> validador.validate(normalizado, ctx, 30), "el JSON real debe ser rechazado");
        resumen.append("motivos_intento1_reproducidos=").append(rechazo.problems()).append('\n');
        assertTrue(rechazo.problems().containsAll(V15_REAL), "los dos motivos V15 del log real");
        resumen.append("motivos_intento1_identicos_al_log_real=").append(V15_REAL.equals(rechazo.problems())).append('\n');

        // Desglose V15 por sesion (grupo, complemento o trabajo)
        escribirDesgloseV15(normalizado, elegibles, ctx);

        // ---- reparacion localizada: viabilidad SIN Replicate (experimentos deterministas) ---------------------------
        // Ninguna de estas semanas se publica ni se presenta como de la IA: solo se pasan por el validador completo
        // para saber si reensamblar sesiones es tecnicamente viable y que reglas semanales intervienen.
        var reparacion = new StringBuilder("experimento;resultado;motivos\n");
        reparacion.append("E1_ia_tal_cual;").append(validar(normalizado, ctx)).append('\n');
        var deReglas = reglas.generate(ctx, List.of());
        reparacion.append("E4_motor_de_reglas;").append(validar(deReglas, ctx)).append('\n');
        var hibrida = new ArrayList<main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft.DraftWorkout>();
        for (int i = 0; i < normalizado.workouts().size(); i++) {
            var w = normalizado.workouts().get(i);
            hibrida.add(w.focus().name().equals("LOWER_BODY") ? deReglas.workouts().get(i) : w);
        }
        var e2 = new main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft(normalizado.source(),
                normalizado.modelName(), normalizado.planName(), null, normalizado.rationale(), hibrida);
        reparacion.append("E2_superior_ia_mas_inferior_reglas;").append(validar(normalizador.normalize(e2, ctx).draft(), ctx)).append('\n');
        // E3: cambio minimo en las dos sesiones rechazadas: un ejercicio de upper legs por otro de waist / lower legs
        // con la dosis con la que el motor de reglas los prescribe en el plan 78 (1112 -> 1141 2x13 r90;
        // 1199 -> 134 3x15 r45).
        var e3w = new ArrayList<main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft.DraftWorkout>();
        for (var w : normalizado.workouts()) {
            var ej = new ArrayList<main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft.DraftExercise>();
            for (var e : w.exercises()) {
                if (e.exerciseId() == 1112L) ej.add(new main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft.DraftExercise(
                        1141L, e.prescriptionType(), (short) 2, (short) 13, null, null, (short) 90, e.notes()));
                else if (e.exerciseId() == 1199L) ej.add(new main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft.DraftExercise(
                        134L, e.prescriptionType(), (short) 3, (short) 15, null, null, (short) 45, e.notes()));
                else ej.add(e);
            }
            e3w.add(new main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft.DraftWorkout(
                    w.scheduledDate(), w.focus(), w.name(), w.expectedDurationMinutes(), ej));
        }
        var e3 = new main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft(normalizado.source(),
                normalizado.modelName(), normalizado.planName(), null, normalizado.rationale(), e3w);
        reparacion.append("E3_ia_con_un_cambio_por_sesion_inferior;").append(validar(normalizador.normalize(e3, ctx).draft(), ctx)).append('\n');
        // E5: solo las dos sesiones superiores de la IA (sin las inferiores): el validador es semanal
        var e5 = new main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft(normalizado.source(),
                normalizado.modelName(), normalizado.planName(), null, normalizado.rationale(),
                normalizado.workouts().stream().filter(w -> w.focus().name().equals("UPPER_BODY")).toList());
        reparacion.append("E5_solo_sesiones_superiores_ia;").append(validar(e5, ctx)).append('\n');
        Files.writeString(SALIDA.resolve("reparacion_localizada_experimentos.csv"), reparacion.toString(), StandardCharsets.UTF_8);

        // ---- pipeline completo con el doble del proveedor -------------------------------------------------------
        var feedbacks = new ArrayList<AiRetryFeedback>();
        var prompts = new ArrayList<String>();
        doAnswer(inv -> {
            PlanGenerationContext c = inv.getArgument(0);
            AiRetryFeedback fb = inv.getArgument(1);
            int intento = inv.getArgument(2);
            feedbacks.add(fb);
            String prompt = promptBuilder.build(c, jsonSupport.write(PlanInputSnapshot.of(c)), fb);
            prompts.add(prompt);
            Files.writeString(SALIDA.resolve("prompt_intento" + intento + ".txt"), prompt, StandardCharsets.UTF_8);
            if (intento == 1) return intento1;
            // Lo que lanzo el generador real tras 150 s de sondeo y la cancelacion (log 15:37:42.035)
            throw new AiProviderUnavailableException("Replicate devolvio un estado no exitoso: processing null");
        }).when(ia).requestOutput(any(), any(), anyInt());

        var plan = generacionInicial.generar(GenerateWeeklyPlanCommand.firstPlan(USUARIO, SEMANA)).orElseThrow();
        long planId = plan.getId();

        verify(ia, times(2)).requestOutput(any(), any(), anyInt());
        assertEquals(2, feedbacks.size());
        assertTrue(feedbacks.get(0).isEmpty(), "el intento 1 no lleva feedback");
        assertEquals(intento1, feedbacks.get(1).rejectedOutput(), "el intento 2 recibe el JSON rechazado EXACTO");
        assertEquals(rechazo.problems(), feedbacks.get(1).rejectionProblems(), "y los motivos del rechazo");
        resumen.append("feedback_intento2_motivos=").append(feedbacks.get(1).rejectionProblems()).append('\n');
        assertTrue(feedbacks.get(1).earlierProblems().isEmpty());
        resumen.append("prompt1_caracteres=").append(prompts.get(0).length()).append('\n');
        resumen.append("prompt2_caracteres=").append(prompts.get(1).length()).append('\n');

        var fila = jdbc.queryForMap("SELECT plan_id, status, generation_source, generation_attempts, planning_policy,"
                + " week_start_date FROM weekly_training_plans WHERE plan_id = ?", planId);
        resumen.append("plan=").append(fila).append('\n');
        assertEquals("RULE_ENGINE", fila.get("generation_source"));
        assertEquals(3, ((Number) fila.get("generation_attempts")).intValue(), "2 predicciones + el respaldo");

        // Huellas del validador sobre el respaldo (comparables con el log real)
        resumen.append("huella_respaldo_reproducida=").append(lineas(logs, "COBERTURA_CAPACIDADES usuario 39 semana 1 origen RULE_ENGINE")).append('\n');
        resumen.append("distribucion_respaldo_reproducida=").append(lineas(logs, "Cobertura 4C posterior")).append('\n');
        resumen.append("tiempos=").append(lineas(logs, "GENERACION_TIEMPOS")).append('\n');

        escribirPlan(planId);
        Files.writeString(SALIDA.resolve("RESUMEN.txt"), resumen.toString(), StandardCharsets.UTF_8);
        Files.writeString(SALIDA.resolve("log_reproduccion.txt"),
                logs.list.stream().map(e -> e.getLevel() + " " + e.getLoggerName() + " : " + e.getFormattedMessage())
                        .collect(Collectors.joining("\n")), StandardCharsets.UTF_8);

        // Todas las sesiones nacen SCHEDULED (el IN_PROGRESS real no puede venir de la creacion)
        var estados = jdbc.queryForList("SELECT DISTINCT status FROM planned_workouts WHERE plan_id = ?", String.class, planId);
        assertEquals(List.of("SCHEDULED"), estados);
    }

    // ------------------------------------------------------------------------------------------------------------

    private String validar(main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft d,
                           PlanGenerationContext ctx) {
        try {
            validador.validate(d, ctx, 30);
            return "VALIDO;";
        } catch (InvalidPlanDraftException e) {
            return "RECHAZADO;" + String.join(" | ", e.problems()).replace(';', ',');
        }
    }

    private void crearUsuario39(List<String> equipo) {
        jdbc.update("INSERT INTO users (user_id, email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " OVERRIDING SYSTEM VALUE VALUES (?, 'caso39@pruebas.invalid', 'x', 'Caso39', now(), now())", USUARIO);
        perfiles.handle(new CreateUserProfileCommand(USUARIO, LocalDate.of(1985, 6, 1), BiologicalSex.MALE,
                new BigDecimal("180"), new BigDecimal("100"), new BigDecimal("80"), FitnessLevel.BEGINNER,
                GoalType.GENERAL_FITNESS, "Quiero Correr un maraton UWU", TrainingLocation.GYM, (short) 4,
                List.of((short) 1, (short) 4, (short) 5, (short) 7), (short) 60, equipo, List.of(), null,
                true, true, true, false, false));
    }

    private void escribirDesgloseV15(main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft d,
                                     Map<Long, CandidateExercise> elegibles, PlanGenerationContext ctx) throws Exception {
        var complementos = main.web.services.fitsense.planning.domain.services.CatalogEligibilityPolicy
                .complementos(elegibles.values(), ctx.profile().goalType());
        var sb = new StringBuilder("fecha;enfoque;orden;exercise_id;nombre;body_part;equipo;complemento_4A;cuenta_V15;"
                + "grupo_del_enfoque\n");
        for (var w : d.workouts()) {
            var esperados = w.focus().bodyPartCodes();
            int orden = 0;
            for (var e : w.exercises()) {
                orden++;
                var c = elegibles.get(e.exerciseId());
                boolean comp = complementos.contains(e.exerciseId());
                sb.append(w.scheduledDate()).append(';').append(w.focus()).append(';').append(orden).append(';')
                        .append(e.exerciseId()).append(';').append(c == null ? "?" : c.name()).append(';')
                        .append(c == null ? "?" : c.bodyPartCode()).append(';')
                        .append(c == null ? "?" : c.equipmentCode()).append(';').append(comp).append(';')
                        .append(!comp).append(';').append(c != null && esperados.contains(c.bodyPartCode())).append('\n');
            }
        }
        Files.writeString(SALIDA.resolve("V15_desglose_intento1.csv"), sb.toString(), StandardCharsets.UTF_8);
    }

    private void escribirPlan(long planId) throws Exception {
        var filas = jdbc.queryForList("""
                SELECT pw.scheduled_date, pw.focus_code, pw.status AS workout_status, pw.expected_duration_minutes,
                       pw.expires_at, pe.exercise_order, pe.exercise_id, e.name_es, e.name_en, bp.code AS body_part,
                       eq.code AS equipment, e.target_muscle, e.difficulty_level, e.movement_pattern,
                       e.requires_hanging, e.requires_overhead, e.requires_floor, e.requires_kneeling,
                       e.requires_inversion, e.high_impact, e.axial_load, pe.prescription_type, pe.planned_sets, pe.planned_reps,
                       pe.planned_duration_seconds, pe.rest_seconds, e.gif_path, e.instructions_es
                  FROM planned_workouts pw
                  JOIN planned_workout_exercises pe ON pe.planned_workout_id = pw.planned_workout_id
                  JOIN exercises e ON e.exercise_id = pe.exercise_id
                  JOIN body_parts bp ON bp.body_part_id = e.body_part_id
                  JOIN equipment_types eq ON eq.equipment_id = e.equipment_id
                 WHERE pw.plan_id = ?
                 ORDER BY pw.scheduled_date, pe.exercise_order""", planId);
        Files.writeString(SALIDA.resolve("plan_respaldo_reproducido.csv"), csv(filas), StandardCharsets.UTF_8);
    }

    static String csv(List<Map<String, Object>> filas) {
        if (filas.isEmpty()) return "";
        var sb = new StringBuilder(String.join(";", filas.get(0).keySet())).append('\n');
        for (var f : filas)
            sb.append(f.values().stream().map(v -> v == null ? "" : String.valueOf(v).replace(';', ','))
                    .collect(Collectors.joining(";"))).append('\n');
        return sb.toString();
    }

    private static ListAppender<ILoggingEvent> capturarLogs() {
        var root = ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger(Logger.ROOT_LOGGER_NAME);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        root.addAppender(appender);
        return appender;
    }

    private static List<String> lineas(ListAppender<ILoggingEvent> logs, String contiene) {
        return logs.list.stream().map(ILoggingEvent::getFormattedMessage).filter(m -> m.contains(contiene)).toList();
    }

    private static String recurso(String ruta) throws Exception {
        try (var in = Caso39ReproduccionIT.class.getResourceAsStream(ruta)) {
            return new String(Objects.requireNonNull(in, ruta).readAllBytes(), StandardCharsets.UTF_8).trim();
        }
    }
}
