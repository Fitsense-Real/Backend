package main.web.services.fitsense.planning;

import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.SessionDurationEstimator;
import main.web.services.fitsense.planning.domain.services.TrainingPlanGenerator;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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

/**
 * AUDITORIA POST R/R' · BARRIDO DE LA GENERACION INICIAL (solo mide). Para cada perfil admitido por P-01 en la rejilla
 * objetivo (4) x nivel (3) x equipamiento (3) x dias (1-7) x minutos (30-75 cada 5, mas 53, 59, 61, 68 y 69) ejecuta el
 * MOTOR DE REGLAS con el contexto real y valida con el validador de produccion. Registra resultado, reglas que fallan y,
 * por sesion, los segundos estimados frente al piso de V20. Opt-in: {@code -Dfitsense.barrido.generacion=true};
 * salida {@code -Dfitsense.barrido.salida}.
 */
@Testcontainers
@EnabledIfSystemProperty(named = "fitsense.barrido.generacion", matches = "true")
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
class BarridoGeneracionInicialIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired UserProfileCommandService perfiles;
    @Autowired PlanDraftValidator validador;
    @Autowired @Qualifier("ruleBasedTrainingPlanGenerator") TrainingPlanGenerator reglas;
    @Autowired JdbcTemplate jdbc;

    static final SessionDurationEstimator ESTIMADOR = new SessionDurationEstimator();
    static final LocalDate SEMANA = LocalDate.of(2026, 11, 2);
    static final Map<String, List<String>> EQUIPOS = new LinkedHashMap<>();
    static {
        EQUIPOS.put("GIMNASIO", List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine", "bench",
                "band", "stationary bike", "treadmill", "kettlebell", "ez barbell", "smith machine"));
        EQUIPOS.put("CASA_MANCUERNAS_BANDAS", List.of("body weight", "dumbbell", "band"));
        EQUIPOS.put("CASA_PESO_CORPORAL", List.of("body weight"));
    }
    static final int[] MINUTOS = {30, 35, 40, 45, 50, 53, 55, 59, 60, 61, 65, 68, 69, 70, 75};
    static final List<Short> SEMANA_DIAS = List.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6,
            (short) 7);
    static final Map<Integer, List<Short>> DIAS = Map.of(
            1, List.of((short) 1), 2, List.of((short) 1, (short) 4), 3, List.of((short) 1, (short) 3, (short) 5),
            4, List.of((short) 1, (short) 2, (short) 4, (short) 5),
            5, List.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5),
            6, SEMANA_DIAS.subList(0, 6), 7, SEMANA_DIAS);

    int n;

    @Test
    void barrido() throws Exception {
        var salida = Path.of(System.getProperty("fitsense.barrido.salida", "target/barrido-generacion"));
        Files.createDirectories(salida);
        var csv = new StringBuilder("objetivo,nivel,equipo,dias,minutos,sesiones,resultado,codigos,seg_min_sesion,"
                + "seg_max_sesion,piso_v20,deficit_max_s,semillas_extra,semillas_extra_validas," + CAB_METRICAS
                + ",primer_problema\n");
        var planes = new StringBuilder("perfil,fecha,enfoque,ejercicio,tipo,series,reps,segundos,descanso\n");
        // 1.a pasada: los mismos usuarios (y por tanto las mismas semillas) que el barrido original.
        record Fila(GoalType o, FitnessLevel nv, String eq, int dias, int m, Evaluacion r) {}
        var filas = new ArrayList<Fila>();
        for (var objetivo : GoalType.values())
            for (var nivel : FitnessLevel.values())
                for (var equipo : EQUIPOS.keySet())
                    for (int dias = 1; dias <= 7; dias++)
                        for (int m : MINUTOS)
                            filas.add(new Fila(objetivo, nivel, equipo, dias, m, evaluar(objetivo, nivel, equipo, dias, m)));
        // 2.a pasada: cada perfil INVALIDO con SEMILLAS_EXTRA usuarios nuevos (otras semillas del motor).
        for (var f : filas) {
            var r = f.r();
            int semillasValidas = 0, semillasProbadas = 0;
            if (!"VALIDO".equals(r.resultado()))
                for (int k = 0; k < SEMILLAS_EXTRA; k++) {
                    semillasProbadas++;
                    if ("VALIDO".equals(evaluar(f.o(), f.nv(), f.eq(), f.dias(), f.m()).resultado())) semillasValidas++;
                }
            String primero = r.primero().replace("\"", "'").replace("\n", " ");
            if (primero.length() > 300) primero = primero.substring(0, 300);
            csv.append(String.join(",", f.o().name(), f.nv().name(), f.eq(), "" + f.dias(), "" + f.m(),
                    "" + r.sesiones(), r.resultado(), r.codigos(), "" + r.smin(), "" + r.smax(),
                    "" + r.piso(), "" + Math.max(0, r.deficit()), "" + semillasProbadas,
                    "" + semillasValidas, r.metricas().isEmpty() ? ",,,,,,,,,,,," : r.metricas(),
                    "\"" + primero + "\"")).append('\n');
            planes.append(r.plan());
        }
        Files.writeString(salida.resolve("barrido_generacion.csv"), csv);
        Files.writeString(salida.resolve("planes.csv"), planes);
        System.out.println("BARRIDO_GENERACION perfiles=" + n);
    }

    /** Semillas adicionales (otros usuarios: otra semilla del motor) para cada perfil INVALIDO. */
    static final int SEMILLAS_EXTRA = Integer.getInteger("fitsense.barrido.semillas", 10);

    record Evaluacion(String resultado, String codigos, String primero, long smin, long smax, long piso, long deficit,
                      int sesiones, String metricas, String plan) {}

    /** Cabecera de las metricas de calidad (rediseno P-G1): mismas columnas en todas las politicas. */
    static final String CAB_METRICAS = "politica,ej_min,ej_max,seg_medio,pct_tiempo_medio,series_grupo_max,grupo_max,"
            + "aerobico_trabajo_min,alto_impacto,moderado_impacto,pull_sin_traccion,dificultad_max,grupos_bajo_objetivo";

    String metricas(main.web.services.fitsense.planning.domain.model.valueobjects.PlanGenerationContext ctx,
                    main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft d) {
        var porId = new HashMap<Long, main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise>();
        ctx.availableExercises().forEach(c -> porId.put(c.exerciseId(), c));
        var comp = main.web.services.fitsense.planning.domain.services.CatalogEligibilityPolicy.complementos(
                ctx.availableExercises(), ctx.profile().goalType());
        int ejMin = Integer.MAX_VALUE, ejMax = 0, alto = 0, moderado = 0, pullSin = 0, difMax = 0;
        long segTotal = 0, aerobico = 0;
        var semana = new TreeMap<String, Integer>();
        for (var w : d.workouts()) {
            ejMin = Math.min(ejMin, w.exercises().size());
            ejMax = Math.max(ejMax, w.exercises().size());
            segTotal += ESTIMADOR.estimateSeconds(w, ctx.prescription());
            boolean traccion = false;
            for (var e : w.exercises()) {
                var c = porId.get(e.exerciseId());
                if (c == null) continue;
                difMax = Math.max(difMax, c.difficulty());
                var imp = main.web.services.fitsense.planning.domain.services.ImpactoPolicy.nivel(c);
                if (imp == main.web.services.fitsense.planning.domain.services.ImpactoPolicy.Nivel.ALTO) alto++;
                if (imp == main.web.services.fitsense.planning.domain.services.ImpactoPolicy.Nivel.MODERADO) moderado++;
                if ("HORIZONTAL_PULL".equals(c.movementPattern()) || "VERTICAL_PULL".equals(c.movementPattern()))
                    traccion = true;
                var clase = main.web.services.fitsense.planning.domain.services.CapabilityPrescriptionPolicy.claseDe(c);
                if (clase == main.web.services.fitsense.planning.domain.services.CapabilityPrescriptionPolicy.Clase.CARDIO
                        && e.plannedSets() != null && e.plannedDurationSeconds() != null)
                    aerobico += (long) e.plannedSets() * e.plannedDurationSeconds();
                boolean resistido = !comp.contains(c.exerciseId()) && (clase
                        == main.web.services.fitsense.planning.domain.services.CapabilityPrescriptionPolicy.Clase.ESTRUCTURAL
                        || clase == main.web.services.fitsense.planning.domain.services.CapabilityPrescriptionPolicy.Clase.RESISTIDO);
                if (resistido && e.plannedSets() != null && c.bodyPartCode() != null)
                    semana.merge(c.bodyPartCode(), (int) e.plannedSets(), Integer::sum);
            }
            if (w.focus() == main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus.PULL && !traccion)
                pullSin++;
        }
        String grupoMax = semana.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("");
        int seriesMax = semana.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        String nivel = ctx.profile().fitnessLevel(), obj = ctx.profile().goalType();
        long bajo = main.web.services.fitsense.planning.domain.services.DosisSemanalPolicy.GRUPOS_GRANDES.stream()
                .filter(g -> ctx.availableExercises().stream().anyMatch(c -> g.equals(c.bodyPartCode())))
                .filter(g -> semana.getOrDefault(g, 0)
                        < main.web.services.fitsense.planning.domain.services.DosisSemanalPolicy.objetivoSeriesSemana(obj, nivel, g))
                .count();
        int n = Math.max(1, d.workouts().size());
        long medio = segTotal / n;
        return String.join(",", ctx.policy().id(), "" + (ejMin == Integer.MAX_VALUE ? 0 : ejMin), "" + ejMax, "" + medio,
                String.format(java.util.Locale.ROOT, "%.1f", 100.0 * medio / (ctx.profile().sessionMinutes() * 60.0)),
                "" + seriesMax, grupoMax, "" + (aerobico / 60), "" + alto, "" + moderado, "" + pullSin, "" + difMax,
                "" + bajo);
    }

    static String plan(String clave, main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft d) {
        var sb = new StringBuilder();
        for (var w : d.workouts())
            for (var e : w.exercises())
                sb.append(String.join(",", clave, "" + w.scheduledDate(), "" + w.focus(), "" + e.exerciseId(),
                        "" + e.prescriptionType(), "" + e.plannedSets(), "" + e.plannedReps(),
                        "" + e.plannedDurationSeconds(), "" + e.restSeconds())).append('\n');
        return sb.toString();
    }

    Evaluacion evaluar(GoalType objetivo, FitnessLevel nivel, String equipo, int dias, int m) {
        n++;
        String email = "barrido" + n + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at,"
                + " enrolled_at) VALUES (?, 'x', 'Barrido', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 6, 15),
                BiologicalSex.FEMALE, new BigDecimal("168"), new BigDecimal("68"), null, nivel,
                objetivo, "barrido", "GIMNASIO".equals(equipo) ? TrainingLocation.GYM
                : TrainingLocation.HOME, (short) dias, DIAS.get(dias), (short) m,
                EQUIPOS.get(equipo), List.of(), "Ninguna", true, true, true, true, true));
        String resultado, codigos = "", primero = "", met = "", planTxt = "";
        long smin = -1, smax = -1, piso = -1, deficit = 0;
        int sesiones = 0;
        try {
            var prep = planComandos.prepare(new GenerateWeeklyPlanCommand(u, SEMANA, null, true, null, null));
            var ctx = prep.context();
            piso = PlanDraftValidator.minimumSessionSeconds(ctx);
            var d = reglas.generate(ctx, List.of());
            sesiones = d.workouts().size();
            met = metricas(ctx, d);
            planTxt = plan(String.join("|", objetivo.name(), nivel.name(), equipo, "" + dias, "" + m), d);
            smin = Long.MAX_VALUE;
            for (var w : d.workouts()) {
                long s = ESTIMADOR.estimateSeconds(w, ctx.prescription());
                smin = Math.min(smin, s);
                smax = Math.max(smax, s);
                deficit = Math.max(deficit, piso - s);
            }
            try {
                validador.validate(d, ctx, prep.divisor());
                resultado = "VALIDO";
            } catch (InvalidPlanDraftException e) {
                resultado = "INVALIDO";
                var cs = new TreeSet<String>();
                e.problems().forEach(x -> cs.add(x.contains(":") ? x.substring(0, x.indexOf(':')) : "OTRO"));
                codigos = String.join(" ", cs);
                primero = e.problems().get(0);
            }
        } catch (RuntimeException e) {
            resultado = "SIN_BORRADOR";
            primero = e.getClass().getSimpleName() + ": " + e.getMessage();
        }
        return new Evaluacion(resultado, codigos, primero, smin, smax, piso, deficit, sesiones, met, planTxt);
    }
}
