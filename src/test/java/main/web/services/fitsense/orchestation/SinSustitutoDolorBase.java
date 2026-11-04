package main.web.services.fitsense.orchestation;

import main.web.services.fitsense.execution.domain.model.commands.FinishWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.model.commands.RecordSessionExerciseCommand;
import main.web.services.fitsense.execution.domain.model.commands.StartWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.services.WorkoutSessionCommandService;
import main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import main.web.services.fitsense.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import main.web.services.fitsense.orchestration.application.internal.WeeklyCycleService;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.services.ConservationValidator;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateSessionFixGenerator;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.planning.interfaces.rest.TrainingPlanController;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.domain.model.valueobjects.SkipReason;
import main.web.services.fitsense.shared.domain.model.valueobjects.TrainingWeek;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * EL EJERCICIO 135 SIN SUSTITUTO, DE EXTREMO A EXTREMO (validacion del limite de dificultad, fase 2).
 * <p>
 * Los 24 casos del barrido son el 135 (<i>remo de deltoides traseros con banda</i>, dificultad 1) en sesiones PUSH
 * de perfiles intermedios y avanzados de casa con mancuernas y bandas, 6 o 7 dias. Aqui se reproduce el flujo REAL:
 * plan generado, sesion ejecutada con DOLOR IDENTIFICADO en el 135 (P-02 lo excluye), ciclo semanal sin IA
 * (respaldo P-03: SUST-SEG-1; si no hay sustituto, NO_FACTIBLE y regeneracion segura de nivel 2) y una semana mas.
 * <p>
 * Invariantes de seguridad comprobadas con el limite apagado y encendido: el 135 no vuelve a publicarse en ninguna
 * semana, su exclusion sigue activa y el ciclo termina (sin reintentos infinitos). Lo que cambia con el limite
 * se registra en {@code fitsense.validacion.salida} si se da.
 */
abstract class SinSustitutoDolorBase {

    static final long EJ = 135L;
    static final AtomicInteger SECUENCIA = new AtomicInteger();
    static final LocalDate MEDIDA = TrainingWeek.containing(LocalDate.now()).startDate().plusWeeks(1);
    static final LocalDate NUEVA = MEDIDA.plusWeeks(1);
    static final LocalDate SIGUIENTE = NUEVA.plusWeeks(1);
    static final List<String> CASA = List.of("body weight", "dumbbell", "band");
    static final Map<Integer, List<Short>> DIAS = Map.of(
            6, List.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6),
            7, List.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6, (short) 7));

    /** Las configuraciones de los 22 perfiles (objetivo, nivel, dias, minutos) del barrido. */
    static final List<Object[]> CONFIGURACIONES = List.of(
            new Object[]{GoalType.LOSE_WEIGHT, FitnessLevel.INTERMEDIATE, 6, 60},
            new Object[]{GoalType.GAIN_MUSCLE, FitnessLevel.ADVANCED, 6, 45},
            new Object[]{GoalType.INCREASE_STRENGTH, FitnessLevel.ADVANCED, 6, 60},
            new Object[]{GoalType.GENERAL_FITNESS, FitnessLevel.INTERMEDIATE, 6, 30},
            new Object[]{GoalType.LOSE_WEIGHT, FitnessLevel.INTERMEDIATE, 7, 75},
            new Object[]{GoalType.GAIN_MUSCLE, FitnessLevel.INTERMEDIATE, 7, 30},
            new Object[]{GoalType.INCREASE_STRENGTH, FitnessLevel.INTERMEDIATE, 7, 60},
            new Object[]{GoalType.GENERAL_FITNESS, FitnessLevel.ADVANCED, 7, 30},
            new Object[]{GoalType.LOSE_WEIGHT, FitnessLevel.INTERMEDIATE, 6, 75},
            new Object[]{GoalType.GAIN_MUSCLE, FitnessLevel.ADVANCED, 6, 60},
            new Object[]{GoalType.INCREASE_STRENGTH, FitnessLevel.ADVANCED, 7, 45},
            new Object[]{GoalType.GENERAL_FITNESS, FitnessLevel.INTERMEDIATE, 7, 45});

    @MockitoBean ReplicateSessionFixGenerator ia;
    @Autowired WeeklyCycleService ciclo;
    @Autowired PlanningContextFacade planning;
    @Autowired UserProfileCommandService perfiles;
    @Autowired WorkoutSessionCommandService sesiones;
    @Autowired TrainingPlanController planes;
    @Autowired UserRepository usuarios;
    @Autowired ConservationValidator v8c;
    @Autowired JdbcTemplate jdbc;

    record Caso(long usuario, String config, long planMedido, long sesion135) {}

    /** El primer perfil de las configuraciones cuyo plan real lleva el 135. */
    Caso caso() {
        var probados = new ArrayList<String>();
        for (var c : CONFIGURACIONES) {
            long u = participante((GoalType) c[0], (FitnessLevel) c[1], (int) c[2], (int) c[3]);
            long plan = planning.generateWeeklyPlan(u, MEDIDA, PlanAdjustment.none(), null, null).orElseThrow();
            var sesion = jdbc.queryForList("SELECT pw.planned_workout_id FROM planned_workouts pw JOIN"
                    + " planned_workout_exercises pwe ON pwe.planned_workout_id = pw.planned_workout_id WHERE"
                    + " pw.plan_id = ? AND pwe.exercise_id = ? ORDER BY pw.scheduled_date LIMIT 1", Long.class, plan, EJ);
            String etiqueta = Arrays.toString(c);
            if (!sesion.isEmpty()) return new Caso(u, etiqueta, plan, sesion.get(0));
            probados.add(etiqueta);
        }
        throw new AssertionError("ningun plan real lleva el 135 en " + probados
                + "; el caso no se puede reproducir con estas configuraciones");
    }

    long participante(GoalType objetivo, FitnessLevel nivel, int dias, int minutos) {
        String email = "sinsust" + getClass().getSimpleName().toLowerCase(Locale.ROOT) + SECUENCIA.incrementAndGet()
                + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'SinSust', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 6, 15), BiologicalSex.FEMALE,
                new BigDecimal("168"), new BigDecimal("68"), null, nivel, objetivo, "validacion limite",
                TrainingLocation.HOME, (short) dias, DIAS.get(dias), (short) minutos, CASA, List.of(), "Ninguna",
                true, true, true, true, true));
        return u;
    }

    /** Todas las sesiones hechas; en la del 135 (si se pide), el 135 con dolor identificado y 0 hecho. */
    void ejecutar(long u, long plan, Long sesionConDolor) {
        var ws = jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ?"
                + " ORDER BY scheduled_date", Long.class, plan);
        for (long w : ws) {
            long s = sesiones.handle(new StartWorkoutSessionCommand(u, w)).orElseThrow().getId();
            var filas = jdbc.queryForList("SELECT planned_exercise_id, exercise_id, prescription_type, planned_sets,"
                    + " planned_reps, planned_duration_seconds FROM planned_workout_exercises WHERE planned_workout_id = ?"
                    + " ORDER BY exercise_order", w);
            for (var f : filas) {
                long pe = ((Number) f.get("planned_exercise_id")).longValue();
                boolean duracion = "DURATION".equals(f.get("prescription_type"));
                int series = f.get("planned_sets") == null ? 1 : ((Number) f.get("planned_sets")).intValue();
                int total = series * ((Number) f.get(duracion ? "planned_duration_seconds" : "planned_reps")).intValue();
                boolean dolor = sesionConDolor != null && w == sesionConDolor
                        && ((Number) f.get("exercise_id")).longValue() == EJ;
                sesiones.handle(new RecordSessionExerciseCommand(u, s, pe, (short) (dolor ? 0 : series),
                        duracion ? null : (dolor ? 0 : total), duracion ? (dolor ? 0 : total) : null, null,
                        dolor ? SkipReason.PAIN_OR_DISCOMFORT : null));
            }
            sesiones.handle(new FinishWorkoutSessionCommand(u, s, (short) 6, (short) 4, (short) 45));
        }
    }

    Map<String, Object> intento(long u, LocalDate semana) {
        var filas = jdbc.queryForList("SELECT decision, status, generation_source, ai_calls, resulting_plan_id,"
                + " rejection_reason, reason_codes::text AS codigos, conservation_report::text AS informe"
                + " FROM plan_adaptation_attempts WHERE user_id = ? AND new_week_start_date = ? ORDER BY attempt_id",
                u, semana);
        return filas.isEmpty() ? Map.of() : filas.get(filas.size() - 1);
    }

    List<Long> ejerciciosDe(long plan) {
        return jdbc.queryForList("SELECT pwe.exercise_id FROM planned_workout_exercises pwe JOIN planned_workouts pw"
                + " ON pw.planned_workout_id = pwe.planned_workout_id WHERE pw.plan_id = ?", Long.class, plan);
    }

    Optional<Long> planActivo(long u, LocalDate semana) {
        var l = jdbc.queryForList("SELECT plan_id FROM weekly_training_plans WHERE user_id = ? AND week_start_date = ?"
                + " AND status = 'ACTIVE'", Long.class, u, semana);
        return l.isEmpty() ? Optional.empty() : Optional.of(l.get(0));
    }

    int dificultad(long id) {
        return jdbc.queryForObject("SELECT difficulty_level FROM exercises WHERE exercise_id = ?", Integer.class, id);
    }

    /**
     * El flujo completo y sus invariantes. Devuelve una linea de evidencia (configuracion, desenlace de cada semana,
     * sustituto y su dificultad) que cada subclase compara con lo que espera de su configuracion.
     */
    Map<String, Object> recorrer() throws Exception {
        when(ia.isEnabled()).thenReturn(false);
        return recorrer(caso());
    }

    /** El mismo flujo para un caso dado (los 22 perfiles del barrido: SinSustitutoDolor24CasosIT). */
    Map<String, Object> recorrer(Caso c) throws Exception {
        when(ia.isEnabled()).thenReturn(false);
        ejecutar(c.usuario(), c.planMedido(), c.sesion135());

        // P-02 registra el dolor identificado del 135 al cerrar la semana (via ciclo).
        var nuevo = ciclo.runFor(c.usuario(), MEDIDA, NUEVA);
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM user_safety_exclusions WHERE user_id = ? AND"
                + " exercise_id = ? AND status = 'ACTIVA' AND reason = 'DOLOR_IDENTIFICADO'", Integer.class,
                c.usuario(), EJ), "P-02: el 135 queda excluido por dolor identificado");
        var semana1 = intento(c.usuario(), NUEVA);
        var activo1 = planActivo(c.usuario(), NUEVA);
        // INVARIANTE 1: lo que dolio no se vuelve a publicar, haya o no sustituto.
        activo1.ifPresent(p -> assertFalse(ejerciciosDe(p).contains(EJ), "el 135 reaparece en la semana nueva"));
        // Una sola version activa como mucho (sin duplicar la semana).
        assertTrue(jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ? AND"
                + " week_start_date = ? AND status = 'ACTIVE'", Integer.class, c.usuario(), NUEVA) <= 1);
        var regeneracion = jdbc.queryForList("SELECT outcome, resulting_plan_id, reason_code, excluded_exercise_ids::text AS excluidos"
                + " FROM safe_regeneration_attempts WHERE user_id = ?", c.usuario());
        var visto = planes.adaptations(UserDetailsImpl.build(usuarios.findById(c.usuario()).orElseThrow())).getBody();
        String resultadoApi = visto == null || visto.isEmpty() ? "-" : visto.get(visto.size() - 1).resultado();

        // Segunda semana: si hubo plan, se ejecuta entera (sin dolor) y se corre otro ciclo.
        String desenlace2 = "-";
        if (activo1.isPresent()) {
            ejecutar(c.usuario(), activo1.get(), null);
            ciclo.runFor(c.usuario(), NUEVA, SIGUIENTE);
            var activo2 = planActivo(c.usuario(), SIGUIENTE);
            // INVARIANTE 2: la exclusion no caduca entre semanas.
            activo2.ifPresent(p -> assertFalse(ejerciciosDe(p).contains(EJ), "el 135 reaparece en la semana 3"));
            var i2 = intento(c.usuario(), SIGUIENTE);
            desenlace2 = i2.getOrDefault("decision", "-") + "/" + i2.getOrDefault("status", "-") + "/"
                    + activo2.map(x -> "plan").orElse("sin_plan");
        }

        // El sustituto del 135 en la semana nueva, si la semana es la copia sustituida (mismo linaje).
        String sustituto = "-";
        Integer difSustituto = null;
        if (activo1.isPresent()) {
            var linaje = jdbc.queryForList("SELECT pwe.exercise_id FROM planned_workout_exercises pwe JOIN"
                    + " planned_workouts pw ON pw.planned_workout_id = pwe.planned_workout_id WHERE pw.plan_id = ? AND"
                    + " pw.source_planned_workout_id = ?", Long.class, activo1.get(), c.sesion135());
            var antes = new HashSet<>(jdbc.queryForList("SELECT exercise_id FROM planned_workout_exercises WHERE"
                    + " planned_workout_id = ?", Long.class, c.sesion135()));
            var puestos = linaje.stream().filter(x -> !antes.contains(x)).toList();
            if (!puestos.isEmpty()) {
                sustituto = puestos.toString();
                difSustituto = puestos.stream().mapToInt(this::dificultad).max().orElse(0);
            }
        }
        var linea = new LinkedHashMap<String, Object>();
        linea.put("limite", v8c.limitaDificultad() ? "on" : "off");
        linea.put("estrategia", v8c.estrategiaSustitucion().name());
        linea.put("config", c.config());
        linea.put("decision", semana1.get("decision"));
        linea.put("estado_v32", semana1.get("status"));
        linea.put("origen", semana1.get("generation_source"));
        linea.put("codigos", semana1.get("codigos"));
        linea.put("resultado_api", resultadoApi);
        linea.put("plan_semana_nueva", activo1.isPresent());
        linea.put("regeneracion_p03", regeneracion.isEmpty() ? "-" : regeneracion.get(0).get("outcome") + " "
                + "plan=" + regeneracion.get(0).get("resulting_plan_id") + " motivo=" + regeneracion.get(0).get("reason_code") + " excluidos=" + regeneracion.get(0).get("excluidos"));
        linea.put("sustituto_del_135", sustituto);
        linea.put("dificultad_sustituto", difSustituto);
        linea.put("semana_siguiente", desenlace2);
        linea.put("rechazo", String.valueOf(semana1.get("rejection_reason")).replace('\n', ' '));
        escribir(linea);
        return linea;
    }

    static void escribir(Map<String, Object> linea) throws Exception {
        String dir = System.getProperty("fitsense.validacion.salida");
        if (dir == null) return;
        var p = Path.of(dir, "flujo_135.csv");
        Files.createDirectories(p.getParent());
        if (!Files.exists(p))
            Files.writeString(p, String.join(";", linea.keySet()) + "\n", StandardCharsets.UTF_8);
        Files.writeString(p, String.join(";", linea.values().stream().map(String::valueOf).toList()) + "\n",
                StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }
}
