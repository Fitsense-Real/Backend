package main.web.services.fitsense.orchestation;

import main.web.services.fitsense.RelojPruebas;
import main.web.services.fitsense.execution.domain.model.commands.FinishWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.model.commands.RecordSessionExerciseCommand;
import main.web.services.fitsense.execution.domain.model.commands.StartWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.services.WorkoutSessionCommandService;
import main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import main.web.services.fitsense.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import main.web.services.fitsense.orchestration.application.internal.WeeklyCycleService;
import main.web.services.fitsense.planning.application.internal.commandservices.PainMemoryService;
import main.web.services.fitsense.planning.application.internal.commandservices.PlanningPolicySelector;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanningPolicy;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateSessionFixGenerator;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.planning.interfaces.rest.TrainingPlanController;
import main.web.services.fitsense.planning.interfaces.rest.resources.SkipWorkoutResource;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.*;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.domain.model.valueobjects.ContextoFaltaDeTiempo;
import main.web.services.fitsense.shared.domain.model.valueobjects.SkipReason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * CICLO REAL con una version de politica configurada por propiedades (cada subclase la suya y su propia base
 * efimera, de modo que los user_id y las semillas coinciden entre politicas). Perfil -> generacion -> publicacion ->
 * ejecucion -> dolor -> exclusiones P-02 -> adaptacion -> P-03 -> validacion -> publicacion o rechazo, sin tocar
 * calculation_configs: es la PROPIA politica la que decide.
 * <p>
 * 10 semanas: S1-S2 completas; S3 dolor en la primera sesion; S4 falta de tiempo habitual (M/2) en la primera
 * (ACORTAR); S5 RPE 9 (fatiga, D8); S6 baja adherencia (una sesion); S7 reactivacion explicita de una exclusion por
 * dolor (reincorporacion legitima como candidato) y semana completa; S8-S10 completas. Tras cada ciclo se repite
 * el mismo ciclo para comprobar la idempotencia (mismo plan, un solo plan activo por semana).
 */
abstract class CicloRealPoliticaBase {

    @MockitoBean ReplicateSessionFixGenerator ia;
    /** El reloj lo controla la prueba: sin barrido horario de entrenamientos vencidos (ver RelojPruebas). */
    @MockitoBean main.web.services.fitsense.planning.infrastructure.scheduling.OverdueWorkoutScheduler vencidas;

    @Autowired UserProfileCommandService perfiles;
    @Autowired PlanningContextFacade planning;
    @Autowired WeeklyCycleService ciclo;
    @Autowired WorkoutSessionCommandService sesiones;
    @Autowired TrainingPlanController planes;
    @Autowired UserRepository usuarios;
    @Autowired PainMemoryService dolor;
    @Autowired PlanningPolicySelector selector;
    @Autowired JdbcTemplate jdbc;

    static final LocalDate SEMANA = RelojPruebas.SEMANA_REFERENCIA;

    abstract String jdbcUrl();

    abstract PlanningPolicy esperada();

    abstract String archivo();

    record Caso(String id, GoalType objetivo, FitnessLevel nivel, String equipo, int dias, int minutos) {}

    static final List<Caso> CASOS = List.of(
            new Caso("L1", GoalType.GAIN_MUSCLE, FitnessLevel.INTERMEDIATE, "GIMNASIO", 3, 60),
            new Caso("L2", GoalType.GAIN_MUSCLE, FitnessLevel.INTERMEDIATE, "GIMNASIO", 3, 75),
            new Caso("L4", GoalType.INCREASE_STRENGTH, FitnessLevel.INTERMEDIATE, "CASA_PESO_CORPORAL", 6, 75),
            new Caso("L6", GoalType.LOSE_WEIGHT, FitnessLevel.BEGINNER, "CASA_MANCUERNAS_BANDAS", 4, 75),
            new Caso("L7", GoalType.GENERAL_FITNESS, FitnessLevel.ADVANCED, "GIMNASIO", 4, 60),
            new Caso("L8", GoalType.GAIN_MUSCLE, FitnessLevel.ADVANCED, "CASA_PESO_CORPORAL", 6, 75));

    @BeforeEach
    void antes() {
        when(ia.isEnabled()).thenReturn(false);
        assertTrue(jdbcUrl().startsWith("jdbc:postgresql://localhost:"), "solo base efimera");
        assertSame(esperada(), selector.activa(), "la politica la decide la configuracion de esta clase");
        var piso = jdbc.queryForObject("SELECT params->'prescription'->>'session_minutes_floor_pct' FROM"
                + " calculation_configs WHERE is_active", String.class);
        assertEquals("70", piso, "no se toca calculation_configs: V20 sigue al 70 %");
    }

    @Test
    void cicloDiezSemanas() throws Exception {
        var csv = new StringBuilder("caso,politica_configurada,objetivo,nivel,equipo,dias,minutos,semana,guion,plan_id,"
                + "politica_plan,origen,contexto_traza,excepciones_c2d,sesiones,series,reps,seg_duracion,seg_sesion_min,"
                + "seg_sesion_max,pct_min,exclusiones_activas,excluidos_en_plan,planes_activos_semana,idempotente,"
                + "intento_status,intervenciones,regeneracion_p03,motivo_p03\n");
        int n = 0;
        for (var c : CASOS) {
            long u = crear(c, ++n);
            Long plan;
            try {
                plan = planning.generateWeeklyPlan(u, SEMANA, PlanAdjustment.none(), null, null).orElse(null);
            } catch (RuntimeException ex) {
                plan = null;
            }
            boolean idem = true;
            for (int s = 1; s <= 10; s++) {
                var inicio = SEMANA.plusWeeks(s - 1);
                int activos = jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ? AND"
                        + " week_start_date = ? AND status = 'ACTIVE'", Integer.class, u, inicio);
                linea(csv, c, s, guionDe(s), u, plan, activos, idem);
                if (s == 7) reactivarUna(u);
                if (plan != null) guion(u, plan, guionDe(s), c.minutos());
                Long siguiente, repetido;
                try {
                    siguiente = ciclo.runFor(u, inicio, inicio.plusWeeks(1)).orElse(null);
                } catch (RuntimeException ex) {
                    siguiente = null;
                }
                // Idempotencia: repetir el mismo ciclo no publica otro plan.
                try {
                    repetido = ciclo.runFor(u, inicio, inicio.plusWeeks(1)).orElse(null);
                } catch (RuntimeException ex) {
                    repetido = null;
                }
                int tras = jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ? AND"
                        + " week_start_date = ? AND status = 'ACTIVE'", Integer.class, u, inicio.plusWeeks(1));
                idem = tras <= 1 && (repetido == null || Objects.equals(siguiente, repetido));
                plan = siguiente;
            }
        }
        escribir(archivo(), csv.toString());
        // Detalle de todos los planes del ciclo (para el anexo de la muestra profesional y la auditoria).
        var det = new StringBuilder("caso_user_id,plan_id,semana_inicio,politica,origen,fecha,enfoque,orden,exercise_id,"
                + "nombre,body_part,patron,tipo,series,reps,segundos,descanso\n");
        for (var f : jdbc.queryForList("SELECT p.user_id, p.plan_id, p.week_start_date, coalesce(p.planning_policy,"
                + " 'NULL') pol, p.generation_source, pw.scheduled_date, pw.focus_code, pwe.exercise_order,"
                + " pwe.exercise_id, replace(coalesce(e.name_es, e.name_en), ',', ' ') nombre, bp.code bp,"
                + " coalesce(e.movement_pattern, '-') patron, pwe.prescription_type, pwe.planned_sets, pwe.planned_reps,"
                + " pwe.planned_duration_seconds, pwe.rest_seconds FROM weekly_training_plans p JOIN planned_workouts pw"
                + " ON pw.plan_id = p.plan_id JOIN planned_workout_exercises pwe ON pwe.planned_workout_id ="
                + " pw.planned_workout_id JOIN exercises e ON e.exercise_id = pwe.exercise_id JOIN body_parts bp ON"
                + " bp.body_part_id = e.body_part_id ORDER BY p.plan_id, pw.scheduled_date, pwe.exercise_order"))
            det.append(String.join(",", f.values().stream().map(String::valueOf).toList())).append('\n');
        escribir(archivo().replace("LONGITUDINAL_", "PLANES_CICLO_"), det.toString());
        // Opciones experimentales registradas en la traza de cada plan (LW-PREF, REP-SUST), si las hay.
        var op = new StringBuilder("caso_user_id,plan_id,semana_inicio,politica,elegibilidad_funcional,opciones\n");
        for (var f : jdbc.queryForList("SELECT user_id, plan_id, week_start_date, coalesce(planning_policy, 'NULL') pol,"
                + " coalesce(planning_policy_trace->'parametros'->>'elegibilidad_funcional', '-') ef,"
                + " coalesce(replace((planning_policy_trace->'opciones')::text, ',', ';'), '-') op"
                + " FROM weekly_training_plans ORDER BY plan_id"))
            op.append(String.join(",", f.values().stream().map(String::valueOf).toList())).append('\n');
        escribir(archivo().replace("LONGITUDINAL_", "TRAZA_OPCIONES_"), op.toString());
    }

    static String guionDe(int s) {
        return switch (s) {
            case 3 -> "DOLOR";
            case 4 -> "ACORTAR";
            case 5 -> "FATIGA_RPE9";
            case 6 -> "BAJA_ADHERENCIA";
            case 7 -> "REACTIVACION";
            case 11 -> "FIN";
            default -> "COMPLETA";
        };
    }

    void reactivarUna(long u) {
        var ids = jdbc.queryForList("SELECT exercise_id FROM user_safety_exclusions WHERE user_id = ? AND status ="
                + " 'ACTIVA' ORDER BY exercise_id LIMIT 1", Long.class, u);
        if (!ids.isEmpty()) {
            try {
                dolor.reactivar(u, ids.get(0), true, "laboratorio: confirmo que ya no siento molestias (simulado)");
            } catch (RuntimeException ignorada) {
                // una restriccion de seguridad vigente puede impedirla; queda registrado por la ausencia del cambio
            }
        }
    }

    void guion(long u, long plan, String guion, int minutos) {
        var ws = jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ? ORDER BY"
                + " scheduled_date", Long.class, plan);
        var user = UserDetailsImpl.build(usuarios.findById(u).orElseThrow());
        for (int k = 0; k < ws.size(); k++) {
            long w = ws.get(k);
            switch (guion) {
                case "DOLOR" -> {
                    if (k == 0) planes.skip(user, w, new SkipWorkoutResource(SkipReason.PAIN_OR_DISCOMFORT));
                    else hacer(u, w, (short) 6);
                }
                case "ACORTAR" -> {
                    if (k == 0) planes.skip(user, w, new SkipWorkoutResource(SkipReason.LACK_OF_TIME,
                            ContextoFaltaDeTiempo.MENOS_TIEMPO_HABITUAL, Math.max(15, minutos / 2)));
                    else hacer(u, w, (short) 6);
                }
                case "FATIGA_RPE9" -> hacer(u, w, (short) 9);
                case "BAJA_ADHERENCIA" -> {
                    if (k == 0) hacer(u, w, (short) 6);
                    else planes.skip(user, w, new SkipWorkoutResource(SkipReason.LACK_OF_MOTIVATION));
                }
                default -> hacer(u, w, (short) 6);
            }
        }
    }

    void hacer(long u, long w, short rpe) {
        RelojPruebas.abrirVentanaDeEjecucion(jdbc, u);
        long sesion = sesiones.handle(new StartWorkoutSessionCommand(u, w)).orElseThrow().getId();
        for (var f : jdbc.queryForList("SELECT planned_exercise_id, prescription_type, planned_sets, planned_reps,"
                + " planned_duration_seconds FROM planned_workout_exercises WHERE planned_workout_id = ?"
                + " ORDER BY exercise_order", w)) {
            long pe = ((Number) f.get("planned_exercise_id")).longValue();
            int series = f.get("planned_sets") == null ? 1 : ((Number) f.get("planned_sets")).intValue();
            boolean dur = "DURATION".equals(f.get("prescription_type"));
            int reps = f.get("planned_reps") == null ? 0 : ((Number) f.get("planned_reps")).intValue();
            int seg = f.get("planned_duration_seconds") == null ? 0 : ((Number) f.get("planned_duration_seconds")).intValue();
            sesiones.handle(new RecordSessionExerciseCommand(u, sesion, pe, (short) series, dur ? null : series * reps,
                    dur ? series * seg : null, null, null));
        }
        sesiones.handle(new FinishWorkoutSessionCommand(u, sesion, rpe, (short) 4, (short) 45));
    }

    @SuppressWarnings("unchecked")
    void linea(StringBuilder csv, Caso c, int s, String guion, long u, Long plan, int activos, boolean idem) {
        String origen = "-", st = "-", inter = "-", regen = "-", motivo = "-", pol = "-", ctxTraza = "-";
        int n = 0, series = 0, reps = 0, seg = 0, exc = 0, enPlan = 0;
        long segMin = 0, segMax = 0;
        double pctMin = 0;
        if (plan != null) {
            var p = jdbc.queryForMap("SELECT generation_source, coalesce(planning_policy, 'NULL') pol,"
                    + " planning_policy_trace::text traza FROM weekly_training_plans WHERE plan_id = ?", plan);
            origen = String.valueOf(p.get("generation_source"));
            pol = String.valueOf(p.get("pol"));
            n = jdbc.queryForObject("SELECT count(*) FROM planned_workouts WHERE plan_id = ?", Integer.class, plan);
            var t = jdbc.queryForMap("SELECT COALESCE(SUM(COALESCE(pwe.planned_sets,1)),0) s, COALESCE(SUM(CASE WHEN"
                    + " pwe.prescription_type = 'SETS_REPS' THEN COALESCE(pwe.planned_sets,1) * COALESCE(pwe.planned_reps,0)"
                    + " ELSE 0 END),0) r, COALESCE(SUM(CASE WHEN pwe.prescription_type = 'DURATION' THEN"
                    + " COALESCE(pwe.planned_sets,1) * COALESCE(pwe.planned_duration_seconds,0) ELSE 0 END),0) d"
                    + " FROM planned_workout_exercises pwe JOIN planned_workouts pw ON pw.planned_workout_id ="
                    + " pwe.planned_workout_id WHERE pw.plan_id = ?", plan);
            series = ((Number) t.get("s")).intValue();
            reps = ((Number) t.get("r")).intValue();
            seg = ((Number) t.get("d")).intValue();
            enPlan = jdbc.queryForObject("SELECT count(*) FROM planned_workout_exercises pwe JOIN planned_workouts pw ON"
                    + " pw.planned_workout_id = pwe.planned_workout_id JOIN user_safety_exclusions x ON x.user_id = ? AND"
                    + " x.exercise_id = pwe.exercise_id AND x.status = 'ACTIVA' AND x.recorded_at <= now() WHERE"
                    + " pw.plan_id = ?", Integer.class, u, plan);
            var traza = p.get("traza") == null ? null : String.valueOf(p.get("traza"));
            if (traza != null) {
                var m = new main.web.services.fitsense.shared.infrastructure.json.JsonSupport().read(traza, Map.class);
                ctxTraza = String.valueOf(m.get("contexto"));
                exc = m.get("excepciones_c2d") instanceof Number x ? x.intValue() : 0;
                segMin = Long.MAX_VALUE;
                int disp = m.get("minutos_disponibles") instanceof Number x ? x.intValue() : c.minutos();
                for (var o : (List<Map<String, Object>>) m.get("sesiones")) {
                    long v = ((Number) o.get("segundos_estimados")).longValue();
                    segMin = Math.min(segMin, v);
                    segMax = Math.max(segMax, v);
                }
                if (segMin == Long.MAX_VALUE) segMin = 0;
                pctMin = 100.0 * segMin / (disp * 60.0);
            }
        }
        var a = jdbc.queryForList("SELECT status, array_to_string(targeted_interventions, ' ') i FROM"
                + " plan_adaptation_attempts WHERE user_id = ? ORDER BY attempt_id DESC LIMIT 1", u);
        if (!a.isEmpty()) {
            st = String.valueOf(a.get(0).get("status"));
            inter = String.valueOf(a.get(0).get("i"));
        }
        var r = jdbc.queryForList("SELECT outcome, coalesce(validation_problems, '') reason FROM safe_regeneration_attempts WHERE"
                + " user_id = ? ORDER BY regeneration_id DESC LIMIT 1", u);
        if (!r.isEmpty()) {
            regen = String.valueOf(r.get(0).get("outcome"));
            motivo = String.valueOf(r.get(0).get("reason")).replace(',', ';').replace('"', '\'');
            if (motivo.length() > 140) motivo = motivo.substring(0, 140);
        }
        int excl = jdbc.queryForObject("SELECT count(*) FROM user_safety_exclusions WHERE user_id = ? AND status ="
                + " 'ACTIVA'", Integer.class, u);
        csv.append(String.join(",", c.id(), esperada().id(), c.objetivo().name(), c.nivel().name(), c.equipo(),
                String.valueOf(c.dias()), String.valueOf(c.minutos()), "S" + s, guion,
                plan == null ? "SIN_PLAN" : String.valueOf(plan), pol, origen, ctxTraza, String.valueOf(exc),
                String.valueOf(n), String.valueOf(series), String.valueOf(reps), String.valueOf(seg),
                String.valueOf(segMin), String.valueOf(segMax), String.format(Locale.ROOT, "%.1f", pctMin),
                String.valueOf(excl), String.valueOf(enPlan), String.valueOf(activos), String.valueOf(idem), st,
                "\"" + inter + "\"", regen, "\"" + motivo + "\"")).append('\n');
    }

    long crear(Caso c, int n) {
        String email = "ciclo" + n + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Ciclo', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 6, 15), BiologicalSex.FEMALE,
                new BigDecimal("168"), new BigDecimal("68"), null, c.nivel(), c.objetivo(), "ciclo real",
                "GIMNASIO".equals(c.equipo()) ? TrainingLocation.GYM : TrainingLocation.HOME, (short) c.dias(),
                ImplementacionC2DV4CMatrizIT.DIAS.get(c.dias()), (short) c.minutos(),
                ImplementacionC2DV4CMatrizIT.EQUIPOS.get(c.equipo()), List.of(), "Ninguna", true, true, true, true, true));
        return u;
    }

    /** Las versiones con EF escriben en la carpeta de la correccion funcional; las demas, donde siempre. */
    void escribir(String nombre, String contenido) throws Exception {
        if (Boolean.getBoolean("fitsense.cierre.pr17")) CierrePR17LabIT.escribir(nombre, contenido);
        else if (esperada().elegibilidadFuncional() || Boolean.getBoolean("fitsense.correccion.ef"))
            CorreccionFuncionalMatrizIT.escribir(nombre, contenido);
        else ImplementacionC2DV4CMatrizIT.escribir(nombre, contenido);
    }
}
