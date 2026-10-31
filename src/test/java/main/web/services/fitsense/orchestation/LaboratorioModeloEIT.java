package main.web.services.fitsense.orchestation;

import main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams;
import main.web.services.fitsense.execution.domain.model.commands.FinishWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.model.commands.RecordSessionExerciseCommand;
import main.web.services.fitsense.execution.domain.model.commands.StartWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.services.WorkoutSessionCommandService;
import main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import main.web.services.fitsense.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import main.web.services.fitsense.orchestration.application.internal.WeeklyCycleService;
import main.web.services.fitsense.planning.FactibilidadDuracionIT;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.*;
import main.web.services.fitsense.planning.domain.services.experimental.ParametrosModeloE;
import main.web.services.fitsense.planning.domain.services.experimental.ValidadorModeloE;
import main.web.services.fitsense.planning.domain.services.experimental.VolumenSemanal;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateSessionFixGenerator;
import main.web.services.fitsense.planning.infrastructure.generation.experimental.ConstructorSemanaModeloE;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.planning.interfaces.rest.TrainingPlanController;
import main.web.services.fitsense.planning.interfaces.rest.resources.SkipWorkoutResource;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.*;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.shared.domain.model.valueobjects.SkipReason;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * LABORATORIO REVISADO (auditoria cientifica, etapas 7, 8 y 11). Compara los tres caminos sobre PostgreSQL EFIMERO, sin
 * IA y sin tocar la ruta de produccion:
 * <ul>
 *   <li><b>C1_VIGENTE</b>: motor y validador de produccion (V20 70 %, topes 6/7/8, 4A/4C/4D).</li>
 *   <li><b>C2_MINIMA</b> (evolucion minima): todo vigente; SOLO si el unico rechazo es V20, el piso se acota a lo que la
 *       estructura vigente puede dar (70 -> 65 -> ... -> 50 %, copia del contexto en memoria). Los planes vigentes
 *       validos salen identicos.</li>
 *   <li><b>C3_MODELO_E</b>: {@link ConstructorSemanaModeloE} con {@link ParametrosModeloE#desactivado()} en modo
 *       laboratorio; se valida con el validador vigente SIN piso (V20 es la regla que E sustituye) y con
 *       {@link ValidadorModeloE}. Lo que solo incumple el tope de V24 o el presupuesto 4D es EXPERIMENTAL.</li>
 * </ul>
 * Correcciones del laboratorio anterior: el porcentaje de tiempo se mide POR SESION (maximo) y no solo en promedio, y
 * se marca aparte {@code V4_ESTRICTO} cuando una sesion supera {@code M x 60 x 1,15} segundos, porque el validador
 * redondea ({@code Math.round(M x 1,15)} y minutos declarados redondeados): ver AUDITORIA_LABORATORIO_E.md.
 * Opt-in: {@code -Dfitsense.laboratorio.e=true}.
 */
@Testcontainers
@EnabledIfSystemProperty(named = "fitsense.laboratorio.e", matches = "true")
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.adaptation.progression.enabled=true"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Laboratorio revisado · tres caminos (vigente, evolucion minima, modelo E)")
class LaboratorioModeloEIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @MockitoBean ReplicateSessionFixGenerator ia;

    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired UserProfileCommandService perfiles;
    @Autowired PlanDraftValidator validador;
    @Autowired @Qualifier("ruleBasedTrainingPlanGenerator") TrainingPlanGenerator reglas;
    @Autowired PlanningContextFacade planning;
    @Autowired WeeklyCycleService ciclo;
    @Autowired WorkoutSessionCommandService sesiones;
    @Autowired TrainingPlanController planes;
    @Autowired UserRepository usuarios;
    @Autowired JdbcTemplate jdbc;

    static final SessionDurationEstimator ESTIMADOR = new SessionDurationEstimator();
    static final LocalDate SEMANA = FactibilidadDuracionIT.SEMANA;
    static final Pattern CODIGO = Pattern.compile("^(V\\d+[A-Z]?)");
    static final ParametrosModeloE E = ParametrosModeloE.desactivado();
    static int secuencia;

    @BeforeEach
    void antes() {
        when(ia.isEnabled()).thenReturn(false);
        assertTrue(postgres.getJdbcUrl().startsWith("jdbc:postgresql://localhost:"), "solo base efimera");
    }

    record Perfil(String id, GoalType objetivo, FitnessLevel nivel, String equipo, int dias, int minutos,
                  boolean restringido, String grupo) {}

    // ============================================================================================ matriz

    @Test
    @Order(1)
    void matrizRevisada() throws Exception {
        var lista = new ArrayList<Perfil>();
        int n = 0;
        for (var equipo : FactibilidadDuracionIT.EQUIPOS.keySet())
            for (int dias : new int[]{1, 2, 3, 4, 6, 7})
                for (var o : GoalType.values())
                    for (var nv : FitnessLevel.values())
                        for (int m : new int[]{30, 45, 60, 75})
                            lista.add(new Perfil("R%04d".formatted(++n), o, nv, equipo, dias, m, false, "BASE"));
        for (var o : GoalType.values())
            for (var nv : FitnessLevel.values())
                for (int m : new int[]{30, 45, 60, 75}) {
                    lista.add(new Perfil("R%04d".formatted(++n), o, nv, "GIMNASIO", 3, m, true, "RESTRICCIONES"));
                    lista.add(new Perfil("R%04d".formatted(++n), o, nv, "GIMNASIO", 3, m, false, "DOLOR_P02"));
                }
        var csv = new StringBuilder(String.join(",", "perfil", "grupo", "user_id", "objetivo", "nivel", "equipo",
                "restricciones", "dias", "sesiones_efectivas", "minutos", "camino", "resultado", "reglas_vigentes_incumplidas",
                "hallazgos_modelo_e", "piso_v20_usado_pct", "v4_estricto_excedido", "ejercicios_max", "seg_sesion_max",
                "pct_minutos_max_sesion", "pct_minutos_prom", "series_sesion_max_grupo", "series_sem_min_grupo_entrenable",
                "grupos_entrenables_bajo_objetivo", "objetivo_semanal_escalado", "series_totales_sem", "cardio_min_sem",
                "identico_a_vigente", "ms")).append('\n');
        var resumen = new TreeMap<String, Integer>();
        var constructor = new ConstructorSemanaModeloE(reglas);
        for (var p : lista) {
            long u = crear(p);
            var prepared = planComandos.prepare(new GenerateWeeklyPlanCommand(u, SEMANA, null, true, null, null));
            var ctx = prepared.context();
            if ("DOLOR_P02".equals(p.grupo())) ctx = conExclusionesDeDolor(ctx, prepared.divisor());
            int div = prepared.divisor();

            // C1
            long t0 = System.nanoTime();
            PlanDraft c1 = generar(ctx);
            long ms1 = (System.nanoTime() - t0) / 1_000_000;
            var r1 = problemas(c1, ctx, div);
            fila(csv, p, u, ctx, "C1_VIGENTE", c1, r1, Set.of(), 70, ms1, "-");
            resumen.merge("C1:" + clase(c1, r1, Set.of()), 1, Integer::sum);

            // C2
            int piso = 70;
            PlanDraft c2 = c1;
            List<String> r2 = r1;
            var ctx2 = ctx;
            t0 = System.nanoTime();
            while (c2 != null && !r2.isEmpty() && r2.stream().allMatch(x -> codigo(x).equals("V20")) && piso > 50) {
                piso -= 5;
                ctx2 = conPiso(ctx, piso);
                c2 = generar(ctx2);
                r2 = problemas(c2, ctx2, div);
            }
            long ms2 = (System.nanoTime() - t0) / 1_000_000 + ms1;
            fila(csv, p, u, ctx2, "C2_MINIMA", c2, r2, Set.of(), piso, ms2, String.valueOf(firma(c2).equals(firma(c1))));
            resumen.merge("C2:" + clase(c2, r2, Set.of()), 1, Integer::sum);

            // C3
            t0 = System.nanoTime();
            PlanDraft c3;
            try {
                c3 = constructor.construir(ctx, div, E, true);
            } catch (RuntimeException ex) {
                c3 = null;
            }
            long ms3 = (System.nanoTime() - t0) / 1_000_000;
            var ctx3 = ConstructorSemanaModeloE.sinPisoV20(ctx);
            var r3 = problemas(c3, ctx3, div);
            var enEstudio = Set.of("V24_TOPE", "V24_PRESUPUESTO");
            fila(csv, p, u, ctx3, "C3_MODELO_E", c3, r3, enEstudio, 0, ms3, String.valueOf(firma(c3).equals(firma(c1))));
            resumen.merge("C3:" + clase(c3, r3, enEstudio), 1, Integer::sum);
        }
        escribir("MATRIZ_EXPERIMENTOS_REVISADA.csv", csv.toString());
        System.out.println("LAB_E_RESUMEN " + lista.size() + " perfiles " + resumen);
    }

    /** P-02 simulado en memoria: se quitan del conjunto elegible los 6 ejercicios que el plan vigente usa mas. */
    PlanGenerationContext conExclusionesDeDolor(PlanGenerationContext c, int div) {
        var d = generar(c);
        if (d == null) return c;
        var usos = new HashMap<Long, Integer>();
        d.workouts().forEach(w -> w.exercises().forEach(e -> usos.merge(e.exerciseId(), 1, Integer::sum)));
        var fuera = usos.entrySet().stream().sorted(Map.Entry.<Long, Integer>comparingByValue().reversed()
                .thenComparing(Map.Entry.comparingByKey())).limit(6).map(Map.Entry::getKey).collect(Collectors.toSet());
        var elegibles = c.availableExercises().stream().filter(x -> !fuera.contains(x.exerciseId())).toList();
        return new PlanGenerationContext(c.userId(), c.weekNumber(), c.weekStartDate(), c.weekEndDate(), c.profile(),
                c.adjustment(), c.previousWeek(), elegibles, c.safety(), c.prescription());
    }

    static PlanGenerationContext conPiso(PlanGenerationContext c, int pct) {
        var x = c.prescription();
        var p = new PrescriptionParams(pct, x.secondsPerRep(), x.transitionSeconds(), x.warmupMinutes(),
                x.defaultRestSeconds(), x.durationTolerancePct(), x.byGoal(), x.repLimits(), x.maxLoadIncreasePct(),
                x.maxRestSeconds());
        return new PlanGenerationContext(c.userId(), c.weekNumber(), c.weekStartDate(), c.weekEndDate(), c.profile(),
                c.adjustment(), c.previousWeek(), c.availableExercises(), c.safety(), p);
    }

    PlanDraft generar(PlanGenerationContext ctx) {
        try {
            return reglas.generate(ctx, List.of());
        } catch (RuntimeException e) {
            return null;
        }
    }

    List<String> problemas(PlanDraft d, PlanGenerationContext ctx, int div) {
        if (d == null) return List.of("SIN_BORRADOR");
        try {
            validador.validate(d, ctx, div);
            return List.of();
        } catch (InvalidPlanDraftException e) {
            return e.problems();
        }
    }

    static String clase(PlanDraft d, List<String> problemas, Set<String> enEstudio) {
        if (d == null) return "SIN_BORRADOR";
        if (problemas.isEmpty()) return "VALIDO";
        return problemas.stream().allMatch(x -> enEstudio.contains(codigo(x))) ? "EXPERIMENTAL" : "INVALIDO";
    }

    static String codigo(String problema) {
        var m = CODIGO.matcher(problema.trim());
        String c = m.find() ? m.group(1) : "OTRO";
        if ("V24".equals(c)) {
            if (problema.contains("colocaciones y el maximo")) return "V24_TOPE";
            if (problema.contains("presupuesto")) return "V24_PRESUPUESTO";
        }
        return c;
    }

    void fila(StringBuilder csv, Perfil p, long u, PlanGenerationContext ctx, String camino, PlanDraft d,
              List<String> problemas, Set<String> enEstudio, int piso, long ms, String identico) {
        var porId = ctx.availableExercises().stream()
                .collect(Collectors.toMap(CandidateExercise::exerciseId, c -> c, (a, b) -> a));
        var complementos = CatalogEligibilityPolicy.complementos(ctx.availableExercises(), ctx.profile().goalType());
        int m = ctx.effectiveSessionMinutes();
        String hall = "-";
        int ejMax = 0, sesMax = 0, minEntrenable = 0, bajo = 0, total = 0;
        long segMax = 0;
        double pctProm = 0, cardio = 0;
        boolean v4 = false;
        int objetivo = E.objetivoSemanal(ctx.profile().goalType(), ctx.effectiveDaysPerWeek());
        if (d != null) {
            var h = ValidadorModeloE.evaluar(d, ctx.profile().goalType(), m, ctx.prescription(), porId, complementos, E);
            hall = h.stream().map(ValidadorModeloE.Hallazgo::codigo).distinct().sorted().collect(Collectors.joining(" "));
            bajo = (int) h.stream().filter(x -> x.codigo().equals("E4")).count();
            for (var w : d.workouts()) {
                long s = ESTIMADOR.estimateSeconds(w, ctx.prescription());
                segMax = Math.max(segMax, s);
                pctProm += 100.0 * s / (m * 60.0);
                ejMax = Math.max(ejMax, w.exercises().size());
                if (s > m * 60.0 * 1.15) v4 = true;
            }
            pctProm /= Math.max(1, d.workouts().size());
            var vol = VolumenSemanal.medir(d, porId, complementos);
            sesMax = vol.maximoPorGrupoYSesion();
            cardio = vol.minutosCardio();
            total = vol.porSemana().values().stream().mapToInt(i -> i).sum();
            var entrenables = new TreeSet<String>();
            for (var w : d.workouts())
                for (var g : E.gruposGrandes())
                    if (w.focus() == WorkoutFocus.FULL_BODY || w.focus().accepts(g)) entrenables.add(g);
            minEntrenable = entrenables.stream().mapToInt(vol::semanal).min().orElse(0);
        }
        var codigos = problemas.stream().map(LaboratorioModeloEIT::codigo).distinct().sorted().collect(Collectors.joining(" "));
        csv.append(String.join(",", p.id(), p.grupo(), String.valueOf(u), p.objetivo().name(), p.nivel().name(), p.equipo(),
                p.restringido() ? "SI" : "NO", String.valueOf(p.dias()), String.valueOf(ctx.effectiveDaysPerWeek()),
                String.valueOf(p.minutos()), camino, clase(d, problemas, enEstudio), "\"" + codigos + "\"", "\"" + hall + "\"",
                String.valueOf(piso), String.valueOf(v4), String.valueOf(ejMax), String.valueOf(segMax),
                String.format(Locale.ROOT, "%.1f", d == null ? 0 : 100.0 * segMax / (m * 60.0)),
                String.format(Locale.ROOT, "%.1f", pctProm), String.valueOf(sesMax), String.valueOf(minEntrenable),
                String.valueOf(bajo), String.valueOf(objetivo), String.valueOf(total),
                String.format(Locale.ROOT, "%.1f", cardio), identico, String.valueOf(ms))).append('\n');
    }

    static String firma(PlanDraft d) {
        return d == null ? "null" : d.workouts().stream().map(w -> w.scheduledDate() + ":" + w.exercises().stream()
                .map(e -> e.exerciseId() + "x" + e.plannedSets() + "x" + e.plannedReps() + "x" + e.plannedDurationSeconds())
                .collect(Collectors.joining(","))).collect(Collectors.joining("|"));
    }

    // ============================================================================================ longitudinal

    /**
     * 8 semanas por el ciclo REAL con la progresion ENCENDIDA (solo en este contexto de prueba). Guion: S1-S2
     * completas (progresion), S3 dolor (P-02), S4 falta de tiempo en dos sesiones (ACORTAR), S5 RPE 9 (fatiga),
     * S6-S8 completas. Camino vigente (piso 70) y evolucion minima aplicada solo donde hace falta (GAIN_MUSCLE 75:
     * piso 55 en la configuracion de la base efimera durante su caso). El modelo E no esta cableado en el ciclo.
     */
    @Test
    @Order(2)
    void longitudinalConProgresion() throws Exception {
        var casos = List.of(
                new Perfil("P1", GoalType.GAIN_MUSCLE, FitnessLevel.INTERMEDIATE, "GIMNASIO", 3, 60, false, "70"),
                new Perfil("P2", GoalType.GAIN_MUSCLE, FitnessLevel.INTERMEDIATE, "GIMNASIO", 3, 75, false, "70"),
                new Perfil("P2m", GoalType.GAIN_MUSCLE, FitnessLevel.INTERMEDIATE, "GIMNASIO", 3, 75, false, "55"),
                new Perfil("P3", GoalType.INCREASE_STRENGTH, FitnessLevel.BEGINNER, "GIMNASIO", 3, 45, false, "70"),
                new Perfil("P4", GoalType.LOSE_WEIGHT, FitnessLevel.INTERMEDIATE, "CASA_MANCUERNAS_BANDAS", 3, 45, false, "70"),
                new Perfil("P5", GoalType.GENERAL_FITNESS, FitnessLevel.ADVANCED, "GIMNASIO", 4, 60, false, "70"));
        var csv = new StringBuilder("caso,piso_v20,objetivo,nivel,minutos,semana,guion,plan_id,origen,sesiones,"
                + "series_totales_sem,reps_totales_sem,exclusiones_activas,intento_status,intervenciones,regeneracion_p03\n");
        for (var c : casos) {
            pisoBase(Integer.parseInt(c.grupo()));
            long u = crear(c);
            Long plan;
            try {
                plan = planning.generateWeeklyPlan(u, SEMANA, PlanAdjustment.none(), null, null).orElse(null);
            } catch (RuntimeException ex) {
                plan = null;
            }
            for (int s = 1; s <= 8; s++) {
                var inicio = SEMANA.plusWeeks(s - 1);
                String guion = switch (s) {
                    case 3 -> "DOLOR";
                    case 4 -> "FALTA_DE_TIEMPO";
                    case 5 -> "FATIGA_RPE9";
                    default -> "COMPLETA";
                };
                linea(csv, c, s, guion, u, plan);
                if (plan != null) guion(u, plan, guion);
                try {
                    plan = ciclo.runFor(u, inicio, inicio.plusWeeks(1)).orElse(null);
                } catch (RuntimeException ex) {
                    plan = null;
                }
            }
        }
        pisoBase(70);
        escribir("SIMULACIONES_LONGITUDINALES_REVISADAS.csv", csv.toString());
    }

    void pisoBase(int pct) {
        assertTrue(postgres.getJdbcUrl().startsWith("jdbc:postgresql://localhost:"), "solo base efimera");
        jdbc.update("UPDATE calculation_configs SET params = jsonb_set(params, '{prescription,session_minutes_floor_pct}',"
                + " to_jsonb(?::int), true) WHERE is_active", pct);
    }

    void guion(long u, long plan, String guion) {
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
                case "FALTA_DE_TIEMPO" -> {
                    if (k < 2) planes.skip(user, w, new SkipWorkoutResource(SkipReason.LACK_OF_TIME));
                    else hacer(u, w, (short) 6);
                }
                case "FATIGA_RPE9" -> hacer(u, w, (short) 9);
                default -> hacer(u, w, (short) 6);
            }
        }
    }

    void hacer(long u, long w, short rpe) {
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

    void linea(StringBuilder csv, Perfil c, int s, String guion, long u, Long plan) {
        String origen = "-", st = "-", inter = "-", regen = "-";
        int n = 0, series = 0, reps = 0;
        if (plan != null) {
            origen = jdbc.queryForObject("SELECT generation_source FROM weekly_training_plans WHERE plan_id = ?",
                    String.class, plan);
            n = jdbc.queryForObject("SELECT count(*) FROM planned_workouts WHERE plan_id = ?", Integer.class, plan);
            var t = jdbc.queryForMap("SELECT COALESCE(SUM(COALESCE(pwe.planned_sets,1)),0) s, COALESCE(SUM(COALESCE("
                    + "pwe.planned_sets,1) * COALESCE(pwe.planned_reps,0)),0) r FROM planned_workout_exercises pwe JOIN"
                    + " planned_workouts pw ON pw.planned_workout_id = pwe.planned_workout_id WHERE pw.plan_id = ?", plan);
            series = ((Number) t.get("s")).intValue();
            reps = ((Number) t.get("r")).intValue();
        }
        var a = jdbc.queryForList("SELECT status, array_to_string(targeted_interventions, ' ') i FROM"
                + " plan_adaptation_attempts WHERE user_id = ? ORDER BY attempt_id DESC LIMIT 1", u);
        if (!a.isEmpty()) {
            st = String.valueOf(a.get(0).get("status"));
            inter = String.valueOf(a.get(0).get("i"));
        }
        var r = jdbc.queryForList("SELECT outcome FROM safe_regeneration_attempts WHERE user_id = ? ORDER BY"
                + " regeneration_id DESC LIMIT 1", String.class, u);
        if (!r.isEmpty()) regen = r.get(0);
        int excl = jdbc.queryForObject("SELECT count(*) FROM user_safety_exclusions WHERE user_id = ? AND status ="
                + " 'ACTIVA'", Integer.class, u);
        csv.append(String.join(",", c.id(), c.grupo(), c.objetivo().name(), c.nivel().name(), String.valueOf(c.minutos()),
                "S" + s, guion, plan == null ? "SIN_PLAN" : String.valueOf(plan), origen, String.valueOf(n),
                String.valueOf(series), String.valueOf(reps), String.valueOf(excl), st, "\"" + inter + "\"", regen))
                .append('\n');
    }

    long crear(Perfil p) {
        String email = "labe" + (++secuencia) + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'LabE', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        boolean libre = !p.restringido();
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 6, 15), BiologicalSex.FEMALE,
                new BigDecimal("168"), new BigDecimal("68"), null, p.nivel(), p.objetivo(), "laboratorio e",
                "GIMNASIO".equals(p.equipo()) ? TrainingLocation.GYM : TrainingLocation.HOME, (short) p.dias(),
                FactibilidadDuracionIT.DIAS.get(p.dias()), (short) p.minutos(),
                FactibilidadDuracionIT.EQUIPOS.get(p.equipo()), List.of(), "Ninguna", libre, libre, libre, libre, libre));
        return u;
    }

    void escribir(String nombre, String contenido) throws Exception {
        var dir = Boolean.getBoolean("fitsense.laboratorio.capturar")
                ? Path.of("docs/FitSense/auditoria_cientifica_entrenamiento") : Path.of("target/laboratorio_e");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(nombre), contenido);
    }
}
