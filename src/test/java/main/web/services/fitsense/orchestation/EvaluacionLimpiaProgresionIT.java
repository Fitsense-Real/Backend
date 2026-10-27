package main.web.services.fitsense.orchestation;

import main.web.services.fitsense.execution.domain.model.commands.FinishWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.model.commands.RecordSessionExerciseCommand;
import main.web.services.fitsense.execution.domain.model.commands.StartWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.services.WorkoutSessionCommandService;
import main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import main.web.services.fitsense.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import main.web.services.fitsense.orchestration.application.internal.WeeklyCycleService;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
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
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.domain.model.valueobjects.SkipReason;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import main.web.services.fitsense.shared.domain.model.valueobjects.TrainingWeek;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * EVALUACION LIMPIA: rutinas generadas HOY, diez semanas de ciclo real con la
 * progresion encendida (y con ella la V24 por capacidades y V25 en la
 * adaptacion). Base PostgreSQL efimera; la IA de correccion, simulada (solo
 * hace lo que su intervencion permite). Nada llega a Neon ni a Replicate.
 * <p>
 * Los 72 perfiles de referencia (4 objetivos x 3 niveles x 30/45/60 min x
 * gimnasio/casa), con el guion:
 * <pre>
 * s1-s3 todo hecho        copia y primeros pasos
 * s4    viernes: fatiga   adaptacion (reduccion de la IA)
 * s5    todo hecho        copia de la reduccion
 * s6    viernes: tiempo   acortar (retirada de un relleno)
 * s7-10 todo hecho        recuperacion, progresion, D-P7, reincorporacion
 * </pre>
 * Toda semana PUBLICADA paso V24 (dosis por capacidad o 4D, con solo las dosis
 * autorizadas), la evidencia de hipertrofia y V25, porque son parte de la
 * validacion del camino de adaptacion con la progresion encendida. La semana
 * inicial se revalida ademas con el validador de la generacion.
 * <p>
 * Larga (unos minutos): solo con {@code -Dfitsense.evaluacion.limpia=true}.
 * Informe: {@code target/evaluacion-limpia/informe.md}.
 */
@Testcontainers
@EnabledIfSystemProperty(named = "fitsense.evaluacion.limpia", matches = "true")
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.adaptation.progression.enabled=true"
})
@DisplayName("Evaluacion limpia · 72 perfiles nuevos, 10 semanas con la progresion encendida")
class EvaluacionLimpiaProgresionIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @MockitoBean ReplicateSessionFixGenerator ia;

    @Autowired WeeklyCycleService ciclo;
    @Autowired PlanningContextFacade planning;
    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired PlanDraftValidator validador;
    @Autowired UserProfileCommandService perfiles;
    @Autowired WorkoutSessionCommandService sesiones;
    @Autowired TrainingPlanController planes;
    @Autowired UserRepository usuarios;
    @Autowired SessionDurationEstimator estimador;
    @Autowired JdbcTemplate jdbc;

    static final LocalDate S1 = TrainingWeek.containing(LocalDate.now()).startDate().plusWeeks(1);
    static final List<String> GIMNASIO = List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine",
            "bench", "band", "stationary bike", "treadmill", "kettlebell", "ez barbell", "smith machine");
    static final List<String> CASA = List.of("body weight", "dumbbell", "band");

    enum Dia { HECHO, FATIGA, TIEMPO }

    /** El guion: que pasa el ULTIMO dia de cada semana (los demas, hechos). */
    static final List<Dia> GUION = List.of(Dia.HECHO, Dia.HECHO, Dia.HECHO, Dia.FATIGA, Dia.HECHO, Dia.TIEMPO,
            Dia.HECHO, Dia.HECHO, Dia.HECHO, Dia.HECHO);

    record Semana(int n, String decision, String status, String tipoPaso, String motivo) {}

    record Perfil(String nombre, GoalType objetivo, String inicial, List<Semana> semanas, String corte) {}

    @Test
    void evaluar() throws Exception {
        when(ia.isEnabled()).thenReturn(true);
        when(ia.modelName()).thenReturn("simulado");
        when(ia.pedirRespuesta(any(), any(), any(), any(), anyInt()))
                .thenReturn(new ReplicateSessionFixGenerator.RawFix("{}", "salida-simulada"));
        when(ia.interpretar(anyString(), any())).thenAnswer(inv -> corregir(inv.getArgument(1)));

        var resultados = new ArrayList<Perfil>();
        int n = 0;
        for (var objetivo : GoalType.values())
            for (var nivel : FitnessLevel.values())
                for (int minutos : new int[]{30, 45, 60})
                    for (var lugar : List.of("GYM", "CASA"))
                        resultados.add(recorrer(++n, objetivo, nivel, minutos, lugar));
        escribir(resultados);
    }

    private Perfil recorrer(int n, GoalType objetivo, FitnessLevel nivel, int minutos, String lugar) {
        String nombre = objetivo + "/" + nivel + "/" + minutos + "/" + lugar;
        long u = participante(n, objetivo, nivel, minutos, lugar);
        var semanas = new ArrayList<Semana>();
        Optional<Long> plan;
        try {
            plan = planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null);
        } catch (RuntimeException e) {
            return new Perfil(nombre, objetivo, "SIN_PLAN " + e.getClass().getSimpleName(), semanas, null);
        }
        if (plan.isEmpty()) return new Perfil(nombre, objetivo, "SIN_PLAN", semanas, null);
        String inicial = validarInicial(u, plan.get());

        for (int k = 0; k < GUION.size(); k++) {
            ejecutar(u, plan.get(), GUION.get(k));
            var semana = S1.plusWeeks(k);
            Optional<Long> siguiente;
            try {
                siguiente = ciclo.runFor(u, semana, semana.plusWeeks(1));
            } catch (RuntimeException e) {
                semanas.add(new Semana(k + 2, "-", "EXCEPCION", null, e.getClass().getSimpleName() + ": " + e.getMessage()));
                return new Perfil(nombre, objetivo, inicial, semanas, "excepcion en la semana " + (k + 2));
            }
            var ev = jdbc.queryForMap("SELECT decision, status, conservation_report::text AS informe, rejection_reason"
                    + " FROM plan_adaptation_attempts WHERE user_id = ? ORDER BY attempt_id DESC LIMIT 1", u);
            String informe = String.valueOf(ev.get("informe")).replace(" ", "");
            var m = Pattern.compile("\"tipo\":\"([A-Z_]+)\"").matcher(informe);
            semanas.add(new Semana(k + 2, (String) ev.get("decision"), (String) ev.get("status"),
                    m.find() ? m.group(1) : null, (String) ev.get("rejection_reason")));
            if (siguiente.isEmpty())
                return new Perfil(nombre, objetivo, inicial, semanas, "sin semana publicada en la " + (k + 2));
            plan = siguiente;
        }
        return new Perfil(nombre, objetivo, inicial, semanas, null);
    }

    /** La semana inicial con el validador de la generacion (V24 y V25 completas). */
    private String validarInicial(long u, long plan) {
        var sesionesPlan = new ArrayList<PlanDraft.DraftWorkout>();
        for (var f : jdbc.queryForList("SELECT planned_workout_id, scheduled_date, focus_code, workout_name,"
                + " expected_duration_minutes FROM planned_workouts WHERE plan_id = ? ORDER BY scheduled_date", plan)) {
            long id = ((Number) f.get("planned_workout_id")).longValue();
            var ejercicios = jdbc.query("SELECT exercise_id, prescription_type, planned_sets, planned_reps,"
                            + " planned_duration_seconds, target_load_kg, rest_seconds FROM planned_workout_exercises"
                            + " WHERE planned_workout_id = ? ORDER BY exercise_order",
                    (rs, i) -> new PlanDraft.DraftExercise(rs.getLong(1), PrescriptionType.valueOf(rs.getString(2)),
                            rs.getObject(3, Short.class), rs.getObject(4, Short.class), rs.getObject(5, Integer.class),
                            rs.getBigDecimal(6), rs.getObject(7, Short.class), null), id);
            sesionesPlan.add(new PlanDraft.DraftWorkout(((java.sql.Date) f.get("scheduled_date")).toLocalDate(),
                    WorkoutFocus.valueOf((String) f.get("focus_code")), (String) f.get("workout_name"),
                    ((Number) f.get("expected_duration_minutes")).intValue(), ejercicios));
        }
        var ctx = planComandos.prepare(new GenerateWeeklyPlanCommand(u, S1, null, true, null, null)).context();
        try {
            validador.validate(new PlanDraft(GenerationSource.RULE_ENGINE, null, "inicial", null, "r", sesionesPlan),
                    ctx, 30);
            return "VALIDA";
        } catch (InvalidPlanDraftException e) {
            return "INVALIDA " + codigos(e.problems());
        }
    }

    // =======================================================================
    // Informe
    // =======================================================================

    private static String codigos(Collection<String> problemas) {
        return problemas.stream().map(p -> p.contains(":") ? p.substring(0, p.indexOf(':')) : p)
                .collect(Collectors.toCollection(TreeSet::new)).toString();
    }

    private void escribir(List<Perfil> perfiles) throws Exception {
        var sb = new StringBuilder("# Evaluación limpia · progresión encendida, 10 semanas\n\n");
        sb.append("| Objetivo | Perfiles | Inicial válida (V24+V25) | Semanas publicadas | Sin publicar | Pasos: progresión | recuperación | reincorporación | vuelta atrás | Adaptación aplicada | Respaldo | Sin efecto |\n")
                .append("|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|\n");
        for (var objetivo : GoalType.values()) {
            var l = perfiles.stream().filter(p -> p.objetivo() == objetivo).toList();
            var s = l.stream().flatMap(p -> p.semanas().stream()).toList();
            sb.append("| ").append(objetivo).append(" | ").append(l.size())
                    .append(" | ").append(l.stream().filter(p -> "VALIDA".equals(p.inicial())).count())
                    .append(" | ").append(s.stream().filter(x -> !List.of("NO_FACTIBLE", "RECHAZADA", "EXCEPCION")
                            .contains(x.status())).count())
                    .append(" | ").append(s.stream().filter(x -> List.of("NO_FACTIBLE", "RECHAZADA", "EXCEPCION")
                            .contains(x.status())).count());
            for (var tipo : List.of("PROGRESION", "RECUPERACION", "REINCORPORACION", "VUELTA_ATRAS"))
                sb.append(" | ").append(s.stream().filter(x -> tipo.equals(x.tipoPaso())).count());
            for (var st : List.of("APLICADA", "RESPALDO", "SIN_EFECTO"))
                sb.append(" | ").append(s.stream().filter(x -> st.equals(x.status())).count());
            sb.append(" |\n");
        }
        var fallos = perfiles.stream().filter(p -> p.corte() != null || !"VALIDA".equals(p.inicial())).toList();
        sb.append("\n## Perfiles con algún fallo (").append(fallos.size()).append(")\n\n");
        if (fallos.isEmpty()) sb.append("Ninguno.\n");
        for (var p : fallos) {
            sb.append("- **").append(p.nombre()).append("** · inicial ").append(p.inicial());
            if (p.corte() != null) sb.append(" · ").append(p.corte());
            p.semanas().stream().filter(x -> x.motivo() != null).forEach(x -> sb.append("\n  - semana ").append(x.n())
                    .append(" ").append(x.decision()).append("/").append(x.status()).append(": ").append(x.motivo()));
            sb.append("\n");
        }
        sb.append("\n## Detalle por perfil (estado de cada ciclo, semanas 2–11)\n\n");
        for (var p : perfiles)
            sb.append("- ").append(p.nombre()).append(": ").append(p.semanas().stream()
                    .map(x -> x.status() + (x.tipoPaso() != null ? "+" + x.tipoPaso() : "")).toList()).append("\n");
        var salida = Path.of("target", "evaluacion-limpia", "informe.md");
        Files.createDirectories(salida.getParent());
        Files.writeString(salida, sb.toString());
        System.out.println(sb);
    }

    // =======================================================================
    // La IA simulada: solo lo que permite cada intervencion
    // =======================================================================

    /** REVISAR_CARGA: -2 repeticiones sin bajar del minimo de V16 (si no, -1 serie). ACORTAR: el ultimo relleno. */
    private List<CorrectedSession> corregir(PlanGenerationContext ctx) {
        var salida = new ArrayList<CorrectedSession>();
        for (var senalada : ctx.adjustment().targetedSessions()) {
            long id = senalada.plannedWorkoutId();
            var f = jdbc.queryForMap("SELECT scheduled_date, focus_code FROM planned_workouts WHERE planned_workout_id = ?", id);
            var actuales = jdbc.query("SELECT pwe.exercise_id, pwe.prescription_type, pwe.planned_sets, pwe.planned_reps,"
                            + " pwe.planned_duration_seconds, pwe.rest_seconds, e.movement_pattern FROM planned_workout_exercises pwe"
                            + " JOIN exercises e ON e.exercise_id = pwe.exercise_id WHERE pwe.planned_workout_id = ?"
                            + " ORDER BY pwe.exercise_order",
                    (rs, k) -> Map.entry(new PlanDraft.DraftExercise(rs.getLong(1), PrescriptionType.valueOf(rs.getString(2)),
                            rs.getObject(3, Short.class), rs.getObject(4, Short.class), rs.getObject(5, Integer.class), null,
                            rs.getObject(6, Short.class), null), String.valueOf(rs.getString(7))), id);
            var lista = new ArrayList<PlanDraft.DraftExercise>();
            int quitar = -1;
            if (senalada.intervencion() == TipoIntervencion.ACORTAR_O_REORGANIZAR)
                for (int k = actuales.size() - 1; k >= 0 && quitar < 0; k--)
                    if (MandatorySlot.ofMovementPattern(actuales.get(k).getValue()).isEmpty()) quitar = k;
            boolean hecho = false;
            for (int k = 0; k < actuales.size(); k++) {
                if (k == quitar) continue;
                var e = actuales.get(k).getKey();
                Short series = e.plannedSets(), reps = e.plannedReps();
                if (senalada.intervencion() == TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION && !hecho
                        && e.prescriptionType() == PrescriptionType.SETS_REPS && reps != null) {
                    if (reps - 2 >= minimo(ctx, e.exerciseId())) {
                        reps = (short) (reps - 2);
                        hecho = true;
                    } else if (series != null && series > 1) {
                        series = (short) (series - 1);
                        hecho = true;
                    }
                }
                lista.add(new PlanDraft.DraftExercise(e.exerciseId(), e.prescriptionType(), series, reps,
                        e.plannedDurationSeconds(), null, e.restSeconds(), null));
            }
            var fecha = ((java.sql.Date) f.get("scheduled_date")).toLocalDate().plusWeeks(1);
            int min = estimador.estimateMinutes(new PlanDraft.DraftWorkout(fecha,
                    WorkoutFocus.valueOf((String) f.get("focus_code")), "x", 0, lista), ctx.prescription());
            salida.add(new CorrectedSession(id, fecha, min, lista, "ajuste simulado"));
        }
        return salida;
    }

    private static int minimo(PlanGenerationContext ctx, long exerciseId) {
        var limites = ctx.prescription() == null ? null : ctx.prescription().repLimits();
        var c = ctx.availableExercises().stream().filter(x -> x.exerciseId() == exerciseId).findFirst();
        return limites == null || c.isEmpty() ? 0 : limites.minRepsFor(c.get().bodyPartCode(), ctx.profile().fitnessLevel());
    }

    // =======================================================================
    // Material
    // =======================================================================

    private long participante(int n, GoalType objetivo, FitnessLevel nivel, int minutos, String lugar) {
        String email = "limpia" + n + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1992, 1, 15), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("70"), null, nivel, objetivo, "evaluacion limpia",
                "GYM".equals(lugar) ? TrainingLocation.GYM : TrainingLocation.HOME, (short) 3,
                List.of((short) 1, (short) 3, (short) 5), (short) minutos, "GYM".equals(lugar) ? GIMNASIO : CASA,
                List.of(), "Ninguna", true, true, true, true, true));
        return u;
    }

    /** Todas las sesiones hechas, salvo la ULTIMA de la semana, que sigue el guion. */
    private void ejecutar(long u, long planId, Dia ultimo) {
        var ids = jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ?"
                + " ORDER BY scheduled_date", Long.class, planId);
        for (int i = 0; i < ids.size(); i++) {
            long w = ids.get(i);
            var dia = i == ids.size() - 1 ? ultimo : Dia.HECHO;
            if (dia != Dia.HECHO) {
                planes.skip(UserDetailsImpl.build(usuarios.findById(u).orElseThrow()), w,
                        new SkipWorkoutResource(dia == Dia.FATIGA ? SkipReason.FATIGUE : SkipReason.LACK_OF_TIME));
                continue;
            }
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
    }
}
