package main.web.services.fitsense.planning;

import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.CapabilityCoveragePolicy;
import main.web.services.fitsense.planning.domain.services.CatalogEligibilityPolicy;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.domain.model.valueobjects.TrainingWeek;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * EVALUACION (no es una regresion): generacion inicial con el MOTOR DE REGLAS
 * para 4, 5 y 6 dias, y el impacto de exigir el 70 % exacto en V20. Sin IA y
 * sin Replicate; PostgreSQL efimero con V1-V35. No cambia ninguna regla: mide.
 * <p>
 * Las 72 referencias ({@link GeneracionReferenciaIT}) usan 3 dias separados y
 * solo producen FULL_BODY, asi que UPPER/LOWER y PUSH/PULL/LEGS nunca se habian
 * generado en una prueba. Aqui se generan y se registra, por sesion: enfoque,
 * ejercicios de trabajo y complementos, grupos, duracion en segundos frente al
 * 70 % exacto, cobertura de capacidades, validacion y distribuciones dudosas.
 * <p>
 * Opt-in: {@code -Dfitsense.evaluacion=true}. Escribe en {@code target/evaluacion/}
 * y, con {@code -Dfitsense.evaluacion.capturar=true}, en
 * {@code docs/FitSense/evidencia/frecuencia/}.
 */
@Testcontainers
@EnabledIfSystemProperty(named = "fitsense.evaluacion", matches = "true")
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("Evaluacion · generacion de 4-6 dias y V20 al 70 % exacto (motor de reglas, sin IA)")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EvaluacionFrecuenciaIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired PlanningContextFacade planning;
    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired UserProfileCommandService perfiles;
    @Autowired PlanDraftValidator validador;
    @Autowired JdbcTemplate jdbc;

    /** Los mismos equipos que las 72 referencias, para que los resultados sean comparables. */
    static final List<String> GIMNASIO = GeneracionReferenciaIT.GIMNASIO;
    static final List<String> CASA = GeneracionReferenciaIT.CASA;
    /** Ejercicios cuya clasificacion se revisa en la evaluacion del catalogo. */
    static final Set<Long> DUDOSOS = Set.of(1267L, 1296L, 1333L, 1059L, 872L);
    static final Set<String> PIERNAS = Set.of("upper legs", "lower legs");
    static final AtomicInteger SECUENCIA = new AtomicInteger();

    record Sesion(String perfil, LocalDate fecha, WorkoutFocus foco, int trabajo, int complementos,
                  Map<String, Long> grupos, long segundos, long pisoExacto, int estimadaMin, int pisoActualMin,
                  List<String> banderas, List<Long> dudosos) {
        double pct(int minutos) {
            return 100.0 * segundos / (minutos * 60.0);
        }
    }

    record Resultado(String perfil, int solicitados, int programados, List<WorkoutFocus> division,
                     String cobertura, String validacion, List<Sesion> sesiones) {}

    // ------------------------------------------------------------------ pruebas

    @Test
    @Order(2)
    @DisplayName("4, 5 y 6 dias: 4 objetivos x 3 niveles x gimnasio/casa, 45 min")
    void barridoCuatroASeisDias() throws Exception {
        var semana = TrainingWeek.containing(LocalDate.now()).startDate().plusWeeks(1);
        var resultados = new ArrayList<Resultado>();
        for (var objetivo : GoalType.values())
            for (var nivel : FitnessLevel.values())
                for (int dias : new int[]{4, 5, 6})
                    for (var lugar : List.of("GYM", "CASA")) {
                        var disponibles = new ArrayList<Short>();
                        for (short d = 1; d <= dias; d++) disponibles.add(d);
                        resultados.add(generar(objetivo + "/" + nivel + "/" + dias + "d/45/" + lugar,
                                objetivo, nivel, dias, disponibles, 45, lugar, semana));
                    }
        assertEquals(72, resultados.size());
        escribir("barrido_4_6_dias", resultados, 45);
    }

    /**
     * Va PRIMERO: el motor de reglas siembra con user_id y semana, asi que los
     * 72 perfiles reciben los ids 1-72, igual que en GeneracionReferenciaIT.
     */
    @Test
    @Order(1)
    @DisplayName("V20 al 70 % exacto: impacto sobre los 72 perfiles de referencia (3 dias)")
    void impactoV20EnReferencias() throws Exception {
        var semana = TrainingWeek.containing(LocalDate.now()).startDate().plusWeeks(1);
        var resultados = new ArrayList<Resultado>();
        for (var objetivo : GoalType.values())
            for (var nivel : FitnessLevel.values())
                for (int minutos : new int[]{30, 45, 60})
                    for (var lugar : List.of("GYM", "CASA"))
                        resultados.add(generar(objetivo + "/" + nivel + "/" + minutos + "/" + lugar,
                                objetivo, nivel, 3, List.of((short) 1, (short) 3, (short) 5), minutos, lugar,
                                semana));
        assertEquals(72, resultados.size());
        escribir("impacto_v20_referencias", resultados, 0);
    }

    // ------------------------------------------------------------------ medicion

    private Resultado generar(String etiqueta, GoalType objetivo, FitnessLevel nivel, int dias,
                              List<Short> disponibles, int minutos, String lugar, LocalDate semana) {
        String email = "evaluacion" + SECUENCIA.incrementAndGet() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1992, 1, 15), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("70"), null, nivel, objetivo, "evaluacion",
                "GYM".equals(lugar) ? TrainingLocation.GYM : TrainingLocation.HOME, (short) dias,
                disponibles, (short) minutos, "GYM".equals(lugar) ? GIMNASIO : CASA,
                List.of(), "Ninguna", true, true, true, true, true));

        var ctx = planComandos.prepare(new GenerateWeeklyPlanCommand(u, semana, null, true, null, null)).context();
        long plan;
        try {
            plan = planning.generateWeeklyPlan(u, semana, PlanAdjustment.none(), null, null).orElse(-1L);
        } catch (RuntimeException e) {
            String motivo = e instanceof InvalidPlanDraftException i
                    ? String.join(" ; ", i.problems()) : e.getClass().getSimpleName() + ": " + e.getMessage();
            return new Resultado(etiqueta, dias, 0, List.of(), "-", "SIN_PLAN " + motivo, List.of());
        }
        if (plan < 0) return new Resultado(etiqueta, dias, 0, List.of(), "-", "SIN_PLAN", List.of());

        var elegibles = ctx.availableExercises().stream()
                .collect(Collectors.toMap(CandidateExercise::exerciseId, c -> c, (a, b) -> a));
        var complementos = CatalogEligibilityPolicy.complementos(ctx.availableExercises(), objetivo.name());
        var p = ctx.prescription();

        var borrador = new ArrayList<PlanDraft.DraftWorkout>();
        var sesiones = new ArrayList<Sesion>();
        for (var f : jdbc.queryForList("SELECT planned_workout_id, scheduled_date, focus_code, workout_name,"
                + " expected_duration_minutes FROM planned_workouts WHERE plan_id = ? ORDER BY scheduled_date", plan)) {
            long id = ((Number) f.get("planned_workout_id")).longValue();
            var fecha = ((java.sql.Date) f.get("scheduled_date")).toLocalDate();
            var foco = WorkoutFocus.valueOf((String) f.get("focus_code"));
            var ejercicios = jdbc.query("SELECT exercise_id, prescription_type, planned_sets, planned_reps,"
                            + " planned_duration_seconds, target_load_kg, rest_seconds FROM planned_workout_exercises"
                            + " WHERE planned_workout_id = ? ORDER BY exercise_order",
                    (rs, k) -> new PlanDraft.DraftExercise(rs.getLong(1), PrescriptionType.valueOf(rs.getString(2)),
                            rs.getObject(3, Short.class), rs.getObject(4, Short.class), rs.getObject(5, Integer.class),
                            rs.getBigDecimal(6), rs.getObject(7, Short.class), null), id);
            var w = new PlanDraft.DraftWorkout(fecha, foco, (String) f.get("workout_name"),
                    ((Number) f.get("expected_duration_minutes")).intValue(), ejercicios);
            borrador.add(w);

            var trabajo = ejercicios.stream().filter(e -> !complementos.contains(e.exerciseId())).toList();
            var grupos = trabajo.stream()
                    .map(e -> elegibles.containsKey(e.exerciseId()) ? elegibles.get(e.exerciseId()).bodyPartCode() : "?")
                    .collect(Collectors.groupingBy(g -> g, TreeMap::new, Collectors.counting()));
            long segundos = segundos(w, p);
            long pisoExacto = minutos(ctx) * 60L * p.floorPct() / 100;
            var dudosos = ejercicios.stream().map(PlanDraft.DraftExercise::exerciseId).filter(DUDOSOS::contains).toList();
            sesiones.add(new Sesion(etiqueta, fecha, foco, trabajo.size(), ejercicios.size() - trabajo.size(), grupos,
                    segundos, pisoExacto, ESTIMADOR.estimateMinutes(w, p),
                    PlanDraftValidator.minimumSessionMinutes(ctx), banderas(foco, trabajo.size(), grupos,
                    segundos, pisoExacto, dudosos), dudosos));
        }

        var draft = new PlanDraft(GenerationSource.RULE_ENGINE, null, "evaluacion", null, "r", borrador);
        var cobertura = CapabilityCoveragePolicy.evaluar(objetivo.name(), nivel.name(), draft,
                ctx.availableExercises(), p.repLimits());
        String validacion;
        try {
            validador.validate(draft, ctx, 30);
            validacion = "VALIDA";
        } catch (InvalidPlanDraftException e) {
            validacion = String.join(" ; ", e.problems().stream().sorted().toList());
        }
        return new Resultado(etiqueta, dias, borrador.size(), borrador.stream().map(PlanDraft.DraftWorkout::focus).toList(),
                cobertura.resultado() + " " + cobertura.colocaciones(), validacion, sesiones);
    }

    private static final main.web.services.fitsense.planning.domain.services.SessionDurationEstimator ESTIMADOR =
            new main.web.services.fitsense.planning.domain.services.SessionDurationEstimator();

    private static int minutos(PlanGenerationContext ctx) {
        return ctx.effectiveSessionMinutes();
    }

    /**
     * La misma cuenta que SessionDurationEstimator, pero en segundos y sin
     * redondear: calentamiento + trabajo + descansos entre series + transiciones.
     */
    static long segundos(PlanDraft.DraftWorkout w,
                         main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams p) {
        if (w.exercises().isEmpty()) return 0;
        long total = (long) p.warmupMinutesOrDefault() * 60
                + (long) (w.exercises().size() - 1) * p.transitionSecondsOrDefault();
        for (var e : w.exercises()) {
            int series = e.plannedSets() == null ? 1 : e.plannedSets();
            total += e.prescriptionType() == PrescriptionType.DURATION
                    ? (long) series * (e.plannedDurationSeconds() == null ? 0 : e.plannedDurationSeconds())
                    : (long) series * (e.plannedReps() == null ? 0 : e.plannedReps()) * p.secondsPerRepOrDefault();
            int descanso = e.restSeconds() == null ? p.defaultRestSecondsOrDefault() : e.restSeconds();
            total += (long) Math.max(0, series - 1) * descanso;
        }
        return total;
    }

    /** Distribuciones que pasan los validadores pero merecen revision humana. */
    private static List<String> banderas(WorkoutFocus foco, int trabajo, Map<String, Long> grupos, long segundos,
                                         long pisoExacto, List<Long> dudosos) {
        var b = new ArrayList<String>();
        long piernas = grupos.entrySet().stream().filter(e -> PIERNAS.contains(e.getKey()))
                .mapToLong(Map.Entry::getValue).sum();
        if ((foco == WorkoutFocus.LOWER_BODY || foco == WorkoutFocus.LEGS) && piernas < 2)
            b.add("MENOS_DE_2_DE_PIERNA");
        if (foco == WorkoutFocus.LOWER_BODY && grupos.getOrDefault("waist", 0L) >= piernas)
            b.add("WAIST_IGUALA_O_SUPERA_PIERNA");
        if (foco.bodyPartCodes().size() >= 3 && trabajo < 4
                && grupos.values().stream().anyMatch(n -> n * 2 > trabajo))
            b.add("MAS_DE_LA_MITAD_SIN_V15(" + trabajo + "_de_trabajo)");
        if (trabajo < 3) b.add("MENOS_DE_3_DE_TRABAJO");
        if (segundos < pisoExacto) b.add("BAJO_70_EXACTO");
        if (!dudosos.isEmpty()) b.add("EJERCICIO_EN_REVISION" + dudosos);
        return b;
    }

    // ------------------------------------------------------------------ informe

    private void escribir(String nombre, List<Resultado> resultados, int minutosFijos) throws Exception {
        var tsv = new StringBuilder("perfil\tdias_solicitados\tsesiones\tdivision\tcobertura\tvalidacion\n");
        var sesionesTsv = new StringBuilder("perfil\tfecha\tenfoque\ttrabajo\tcomplementos\tgrupos\tsegundos"
                + "\tpct_configurado\testimada_min\tpiso_actual_min\tpiso_exacto_s\tbanderas\n");
        for (var r : resultados) {
            tsv.append(r.perfil()).append('\t').append(r.solicitados()).append('\t').append(r.programados())
                    .append('\t').append(r.division()).append('\t').append(r.cobertura()).append('\t')
                    .append(r.validacion()).append('\n');
            int minutos = minutosFijos > 0 ? minutosFijos : Integer.parseInt(r.perfil().split("/")[2]);
            for (var s : r.sesiones())
                sesionesTsv.append(s.perfil()).append('\t').append(s.fecha().getDayOfWeek()).append('\t')
                        .append(s.foco()).append('\t').append(s.trabajo()).append('\t').append(s.complementos())
                        .append('\t').append(s.grupos()).append('\t').append(s.segundos()).append('\t')
                        .append(String.format(Locale.ROOT, "%.1f", s.pct(minutos))).append('\t')
                        .append(s.estimadaMin()).append('\t').append(s.pisoActualMin()).append('\t')
                        .append(s.pisoExacto()).append('\t').append(String.join(",", s.banderas())).append('\n');
        }
        var dir = Boolean.getBoolean("fitsense.evaluacion.capturar")
                ? Path.of("docs/FitSense/evidencia/frecuencia") : Path.of("target/evaluacion");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(nombre + "_planes.tsv"), tsv.toString());
        Files.writeString(dir.resolve(nombre + "_sesiones.tsv"), sesionesTsv.toString());
    }
}
