package main.web.services.fitsense.orchestation;

import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.*;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateSessionFixGenerator;
import main.web.services.fitsense.planning.infrastructure.generation.rules.RuleBasedTrainingPlanGenerator;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.*;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.BufferedWriter;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * COMPARACION PERFIL A PERFIL. Vuelca, para los 960 perfiles de la matriz (mismo orden, mismos user_id y semillas),
 * el plan COMPLETO de cada politica: resultado, reglas, excepcion C2-D, exclusiones por dolor y cada colocacion
 * (fecha, enfoque, segundos de la sesion, orden, ejercicio, patron, clase, tipo, series, repeticiones, segundos,
 * descanso). Se ejecuta igual sobre versiones distintas del codigo para comparar los archivos.
 * Opt-in: {@code -Dfitsense.comparacion.perfiles=true}; salida en {@code -Dfitsense.comparacion.salida}.
 */
@Testcontainers
@EnabledIfSystemProperty(named = "fitsense.comparacion.perfiles", matches = "true")
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "fitsense.adaptation.progression.enabled=true"
})
@DisplayName("Comparacion perfil a perfil: volcado de planes completos")
class ComparacionPerfilesIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @MockitoBean ReplicateSessionFixGenerator ia;
    @Autowired main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService planComandos;
    @Autowired UserProfileCommandService perfiles;
    @Autowired PlanDraftValidator validador;
    @Autowired RuleBasedTrainingPlanGenerator reglas;
    @Autowired JdbcTemplate jdbc;

    static final SessionDurationEstimator EST = new SessionDurationEstimator();
    static final LocalDate SEMANA = LocalDate.of(2026, 11, 2);
    static final Map<String, List<String>> EQUIPOS = new LinkedHashMap<>();
    static final Map<Integer, List<Short>> DIAS = Map.of(
            1, List.of((short) 1), 2, List.of((short) 1, (short) 4), 3, List.of((short) 1, (short) 3, (short) 5),
            4, List.of((short) 1, (short) 2, (short) 4, (short) 5),
            6, List.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6),
            7, List.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6, (short) 7));

    static {
        EQUIPOS.put("GIMNASIO", List.of("body weight", "dumbbell", "barbell", "cable", "leverage machine", "bench",
                "band", "stationary bike", "treadmill", "kettlebell", "ez barbell", "smith machine"));
        EQUIPOS.put("CASA_MANCUERNAS_BANDAS", List.of("body weight", "dumbbell", "band"));
        EQUIPOS.put("CASA_PESO_CORPORAL", List.of("body weight"));
    }

    record Perfil(String id, GoalType objetivo, FitnessLevel nivel, String equipo, int dias, int minutos,
                  boolean restringido, String grupo) {}

    static int secuencia;

    List<String> politicas() {
        return List.of("PLAN-HIST-1", "PLAN-C2D-0.1", "PLAN-C2D-V4-0.1", "PLAN-C2D-EF-0.1", "PLAN-C2D-V4-EF-0.1");
    }

    PlanGenerationContext conPolitica(PlanGenerationContext c, String id) {
        return c.conPolitica(PlanningPolicy.deRegistro(id));
    }

    PlanGenerationContext conElegibles(PlanGenerationContext c, List<CandidateExercise> e) {
        return new PlanGenerationContext(c.userId(), c.weekNumber(), c.weekStartDate(), c.weekEndDate(), c.profile(),
                c.adjustment(), c.previousWeek(), e, c.safety(), c.prescription(), c.policy());
    }

    /** Estado de C2-D (como en las matrices): solo si la version tiene C2-D y sin ella todo es V20. */
    String[] saturacion(PlanDraft d, PlanGenerationContext ctx, String id, int div, List<String> prob) {
        var pol = ctx.policy();
        if (!pol.c2d() || d == null) return null;
        var sin = ctx.conPolitica(new PlanningPolicy(pol.id(), pol.candidata(), false, pol.v4Segundos(),
                pol.elegibilidadFuncional(), pol.descripcion()));
        var p = problemas(d, sin, div);
        if (p.isEmpty() || !p.stream().allMatch(x -> x.startsWith("V20:"))) return null;
        var r = validador.evaluarC2D(d, ctx, div, ConservationReport.vacio());
        return new String[]{r.estado().name(), r.motivo()};
    }

    @Test
    void volcar() throws Exception {
        when(ia.isEnabled()).thenReturn(false);
        assertTrue(postgres.getJdbcUrl().startsWith("jdbc:postgresql://localhost:"));
        var dir = Path.of(System.getProperty("fitsense.comparacion.salida", "target/comparacion"));
        Files.createDirectories(dir);
        try (BufferedWriter planes = Files.newBufferedWriter(dir.resolve("planes.csv"));
             BufferedWriter coloc = Files.newBufferedWriter(dir.resolve("colocaciones.csv"))) {
            planes.write("perfil,politica,resultado,reglas,c2d_estado,c2d_motivo,excluidos_dolor,sesiones,seg_total\n");
            coloc.write("perfil,politica,fecha,enfoque,seg_sesion,orden,exercise_id,patron,clase,tipo,series,reps,segundos,descanso\n");
            for (var p : perfiles960()) {
                long u = crear(p);
                var prep = planComandos.prepare(new GenerateWeeklyPlanCommand(u, SEMANA, null, true, null, null));
                var base = prep.context();
                Set<Long> fuera = Set.of();
                if ("DOLOR_P02".equals(p.grupo())) {
                    var d = generar(base);
                    if (d != null) {
                        var usos = new HashMap<Long, Integer>();
                        d.workouts().forEach(w -> w.exercises().forEach(e -> usos.merge(e.exerciseId(), 1, Integer::sum)));
                        fuera = usos.entrySet().stream().sorted(Map.Entry.<Long, Integer>comparingByValue().reversed()
                                .thenComparing(Map.Entry.comparingByKey())).limit(6).map(Map.Entry::getKey)
                                .collect(Collectors.toCollection(TreeSet::new));
                        final var f = fuera;
                        base = conElegibles(base, base.availableExercises().stream()
                                .filter(x -> !f.contains(x.exerciseId())).toList());
                    }
                }
                var porId = base.availableExercises().stream()
                        .collect(Collectors.toMap(CandidateExercise::exerciseId, c -> c, (a, b) -> a));
                for (var etiqueta : politicas()) {
                    var ctx = conPolitica(base, etiqueta);
                    var d = generar(ctx);
                    var prob = problemas(d, ctx, prep.divisor());
                    String estado = "-", motivo = "-";
                    var sat = saturacion(d, ctx, etiqueta, prep.divisor(), prob);
                    if (sat != null) { estado = sat[0]; motivo = sat[1]; }
                    var codigos = prob.stream().map(x -> x.contains(":") ? x.substring(0, x.indexOf(':')) : "OTRO")
                            .distinct().sorted().collect(Collectors.joining(" "));
                    long total = 0;
                    if (d != null)
                        for (var w : d.workouts()) {
                            long s = EST.estimateSeconds(w, ctx.prescription());
                            total += s;
                            int k = 0;
                            for (var e : w.exercises()) {
                                var c = porId.get(e.exerciseId());
                                coloc.write(String.join(",", p.id(), etiqueta, String.valueOf(w.scheduledDate()),
                                        String.valueOf(w.focus()), String.valueOf(s), String.valueOf(++k),
                                        String.valueOf(e.exerciseId()), c == null ? "?" : String.valueOf(c.movementPattern()),
                                        c == null ? "?" : CapabilityPrescriptionPolicy.claseDe(c).name(),
                                        String.valueOf(e.prescriptionType()), String.valueOf(e.plannedSets()),
                                        String.valueOf(e.plannedReps()), String.valueOf(e.plannedDurationSeconds()),
                                        String.valueOf(e.restSeconds())) + "\n");
                            }
                        }
                    planes.write(String.join(",", p.id(), etiqueta,
                            d == null ? "SIN_BORRADOR" : prob.isEmpty() ? "VALIDO" : "INVALIDO", "\"" + codigos + "\"",
                            estado, "\"" + motivo.replace("\"", "'") + "\"",
                            "\"" + fuera.stream().map(String::valueOf).collect(Collectors.joining(" ")) + "\"",
                            String.valueOf(d == null ? 0 : d.workouts().size()), String.valueOf(total)) + "\n");
                }
            }
        }
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

    List<Perfil> perfiles960() {
        var lista = new ArrayList<Perfil>();
        int n = 0;
        for (var equipo : EQUIPOS.keySet())
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
        return lista;
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
                DIAS.get(p.dias()), (short) p.minutos(), EQUIPOS.get(p.equipo()), List.of(), "Ninguna", libre, libre,
                libre, libre, libre));
        return u;
    }
}
