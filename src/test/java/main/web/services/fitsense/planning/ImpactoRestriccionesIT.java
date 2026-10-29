package main.web.services.fitsense.planning;

import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.MandatorySlot;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.services.GoalFeasibilityPolicy;
import main.web.services.fitsense.planning.domain.services.SlotCoveragePolicy;
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
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Impacto de un catalogo sobre perfiles con restricciones (auditoria del
 * catalogo, fase 4). Opt-in: {@code -Dfitsense.impacto.dir=<carpeta>}.
 * <p>
 * Para 4 objetivos x 3 niveles x gimnasio/casa x 8 combinaciones de
 * restricciones (192 perfiles, 3 dias, 45 min) escribe {@code impacto_restricciones.tsv}:
 * elegibles, cobertura 4C previa, veredicto de capacidades, elegibles de
 * LOWER_HIP, resultado del motor de reglas y las VIOLACIONES: ejercicios con
 * un error de seguridad CONFIRMADO en la revision visual (bloque A de
 * {@code correcciones_v37.tsv}) que el perfil excluye y aun asi recibe como
 * elegibles o en su plan. Con el catalogo V36 deben aparecer; con la propuesta
 * V37 deben ser cero. No relaja ninguna restriccion: un perfil sin plan se
 * registra como SIN_PLAN.
 * <p>
 * El catalogo se elige con {@code -Dspring.flyway.locations=...} (ver
 * {@link GeneracionReferenciaCandidataIT}).
 */
@Testcontainers
@EnabledIfSystemProperty(named = "fitsense.impacto.dir", matches = ".+")
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
class ImpactoRestriccionesIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired PlanningContextFacade planning;
    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired UserProfileCommandService perfiles;
    @Autowired JdbcTemplate jdbc;

    static final Path CORRECCIONES = Path.of("docs/FitSense/auditoria_catalogo/v37/correcciones_v37.tsv");

    /** allowHighImpact, allowFloor, allowKneeling, allowHanging, allowOverhead. */
    record Restriccion(String nombre, boolean impacto, boolean suelo, boolean rodillas, boolean colgar,
                       boolean brazosArriba) {}

    static final List<Restriccion> RESTRICCIONES = List.of(
            new Restriccion("NINGUNA", true, true, true, true, true),
            new Restriccion("SIN_SUELO", true, false, true, true, true),
            new Restriccion("SIN_RODILLAS", true, true, false, true, true),
            new Restriccion("SIN_SUSPENSION", true, true, true, false, true),
            new Restriccion("SIN_BRAZOS_ARRIBA", true, true, true, true, false),
            new Restriccion("SIN_IMPACTO", false, true, true, true, true),
            new Restriccion("COMO_USUARIO_31", false, false, true, false, true),
            new Restriccion("TODAS", false, false, false, false, false));

    /** Ejercicios del bloque A por bandera que pasa a true, y los que pasan a nivel 3 o a un aparato. */
    private Map<String, Set<Long>> confirmados() throws Exception {
        var m = new HashMap<String, Set<Long>>();
        for (var l : Files.readAllLines(CORRECCIONES).subList(1, Files.readAllLines(CORRECCIONES).size())) {
            var c = l.split("\t");
            if (!c[3].startsWith("A")) continue;
            long id = Long.parseLong(c[0]);
            if (c[4].startsWith("requires_") && c[6].equals("true")) m.computeIfAbsent(c[4], k -> new TreeSet<>()).add(id);
            if (c[4].equals("difficulty_level")) m.computeIfAbsent("nivel_3", k -> new TreeSet<>()).add(id);
            if (c[4].equals("requisitos")) m.computeIfAbsent("aparato", k -> new TreeSet<>()).add(id);
        }
        return m;
    }

    @Test
    @DisplayName("192 perfiles con restricciones: elegibles, 4C, capacidades, plan y violaciones confirmadas")
    void impacto() throws Exception {
        var conf = confirmados();
        var semana = TrainingWeek.containing(LocalDate.now()).startDate().plusWeeks(1);
        var sb = new StringBuilder("perfil\telegibles\tcobertura_4c\tcapacidades\tlower_hip\tplan\tviolaciones_elegibles"
                + "\tviolaciones_plan\tejercicios_plan\n");
        int n = 0;
        for (var objetivo : GoalType.values())
            for (var nivel : FitnessLevel.values())
                for (var lugar : List.of("GYM", "CASA"))
                    for (var r : RESTRICCIONES) {
                        n++;
                        String perfil = objetivo + "/" + nivel + "/" + lugar + "/" + r.nombre();
                        String email = "impacto" + n + "@pruebas.invalid";
                        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
                        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
                        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1995, 3, 10), BiologicalSex.MALE,
                                new BigDecimal("175"), new BigDecimal("75"), null, nivel, objetivo, "impacto",
                                "GYM".equals(lugar) ? TrainingLocation.GYM : TrainingLocation.HOME, (short) 3,
                                List.of((short) 1, (short) 3, (short) 5), (short) 45,
                                "GYM".equals(lugar) ? GeneracionReferenciaBase.GIMNASIO : GeneracionReferenciaBase.CASA,
                                List.of(), "Ninguna", r.impacto(), r.suelo(), r.rodillas(), r.colgar(), r.brazosArriba()));
                        var ctx = planComandos.prepare(new GenerateWeeklyPlanCommand(u, semana, null, true, null, null))
                                .context();
                        List<CandidateExercise> eleg = ctx.availableExercises();
                        var ids = eleg.stream().map(CandidateExercise::exerciseId).collect(Collectors.toSet());
                        var cobertura = SlotCoveragePolicy.ofEligible(eleg);
                        var pool = GoalFeasibilityPolicy.evaluarPool(objetivo.name(), nivel.name(), eleg)
                                .map(p -> p.veredicto().name()).orElse("-");
                        var hip = eleg.stream().filter(c -> MandatorySlot.LOWER_HIP.movementPatterns()
                                .contains(c.movementPattern())).map(CandidateExercise::exerciseId).sorted().toList();
                        var prohibidos = prohibidos(conf, r, nivel, lugar);
                        var violElegibles = new TreeSet<>(ids);
                        violElegibles.retainAll(prohibidos);
                        String plan;
                        List<Long> enPlan = List.of();
                        try {
                            long p = planning.generateWeeklyPlan(u, semana, PlanAdjustment.none(), null, null).orElse(-1L);
                            if (p < 0) plan = "SIN_PLAN";
                            else {
                                plan = "PUBLICADO";
                                enPlan = jdbc.queryForList("SELECT pwe.exercise_id FROM planned_workout_exercises pwe"
                                        + " JOIN planned_workouts pw ON pw.planned_workout_id = pwe.planned_workout_id"
                                        + " WHERE pw.plan_id = ? ORDER BY pw.scheduled_date, pwe.exercise_order", Long.class, p);
                            }
                        } catch (RuntimeException e) {
                            plan = "SIN_PLAN " + e.getClass().getSimpleName();
                        }
                        var violPlan = new TreeSet<>(enPlan);
                        violPlan.retainAll(prohibidos);
                        sb.append(perfil).append('\t').append(eleg.size()).append('\t').append(cobertura.describe())
                                .append('\t').append(pool).append('\t').append(hip).append('\t').append(plan)
                                .append('\t').append(violElegibles).append('\t').append(violPlan).append('\t')
                                .append(enPlan).append('\n');
                    }
        assertEquals(192, n);
        var dir = Path.of(System.getProperty("fitsense.impacto.dir"));
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("impacto_restricciones.tsv"), sb.toString());
    }

    /** Lo que el perfil excluye segun la revision visual (no segun las banderas del catalogo cargado). */
    private static Set<Long> prohibidos(Map<String, Set<Long>> conf, Restriccion r, FitnessLevel nivel, String lugar) {
        var s = new TreeSet<Long>();
        if (!r.suelo()) s.addAll(conf.getOrDefault("requires_floor", Set.of()));
        if (!r.rodillas()) s.addAll(conf.getOrDefault("requires_kneeling", Set.of()));
        if (!r.colgar()) s.addAll(conf.getOrDefault("requires_hanging", Set.of()));
        if (!r.brazosArriba()) s.addAll(conf.getOrDefault("requires_overhead", Set.of()));
        if (nivel != FitnessLevel.ADVANCED) s.addAll(conf.getOrDefault("nivel_3", Set.of()));
        if ("CASA".equals(lugar)) s.addAll(conf.getOrDefault("aparato", Set.of()));
        return s;
    }
}
