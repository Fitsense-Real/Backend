package main.web.services.fitsense.planning;

import main.web.services.fitsense.planning.domain.exceptions.PlanGenerationFailedException;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.MandatorySlot;
import main.web.services.fitsense.planning.domain.model.valueobjects.MvpDosePolicy;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;
import main.web.services.fitsense.planning.domain.services.CapabilityPrescriptionPolicy;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.SessionDurationEstimator;
import main.web.services.fitsense.planning.domain.services.WeeklyTrainingPlanCommandService;
import main.web.services.fitsense.planning.infrastructure.generation.rules.RuleBasedTrainingPlanGenerator;
import main.web.services.fitsense.planning.interfaces.acl.PlanningContextFacade;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.BiologicalSex;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import main.web.services.fitsense.profiling.domain.model.valueobjects.TrainingLocation;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RESPALDO DE 60 MINUTOS CON 5 Y 6 DIAS (campana MVP del 30/10/2026, usuario U06).
 * <p>
 * U06 (GAIN_MUSCLE avanzado, gimnasio, lunes, miercoles, jueves, sabado y domingo, 60 min) se
 * quedo SIN PLAN INICIAL: la IA agoto sus intentos (un timeout del proveedor) y el motor de reglas,
 * que es el respaldo, no supero V15 (dia LEGS solo con upper legs) ni V20 (domingo corto). Los
 * barridos previos solo cubrian 45 min y dias seguidos. Motor de reglas, sin IA, semana de la
 * campana (2/11/2026).
 * <p>
 * DIAGNOSTICO: no es un fallo del motor, es un CONFLICTO DE POLITICAS aprobadas en GAIN_MUSCLE a
 * 60 min con 5-6 dias. 4D v3.1 fija la dosis (estructural 3x10/90 ~364 s; resistido de zona
 * pequena 2x13/90 ~262 s) y un tope de 7 colocaciones; V20 pide 2520 s; V15 pide dos grupos y, en
 * LOWER_BODY, ninguno por encima de la mitad. LEGS/PULL con dos grupos: 6 x 364 + 262 = 2448 s.
 * LOWER_BODY con la mitad: 3 x 364 + 4 x 262 = 2142 s. Ninguna composicion valida llega al piso.
 * <p>
 * 4D v3.2 (decision del 31/10/2026, opcion B, ADENDA_4D_v3_2.md): el relleno de zona pequena
 * puede pasar de 2x13 a 3x13 solo cuando hace falta para V20. Resuelve 16 de las 24 combinaciones
 * (U06 incluido), que pasan a ser regresion. Quedan 8 (GAIN_MUSCLE intermedio y avanzado EN CASA:
 * el dia PULL es espalda 3x10 x 6 + biceps de relleno ESTANDAR 2x10, 2430 s de 2520) que la
 * opcion B no cubre; siguen como reproduccion con {@code -Dfitsense.pendientes=true}.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("Respaldo de reglas: 5 y 6 dias de 60 minutos publican plan")
class RespaldoSesentaMinutosIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired PlanningContextFacade planning;
    @Autowired WeeklyTrainingPlanCommandService planComandos;
    @Autowired RuleBasedTrainingPlanGenerator motor;
    @Autowired SessionDurationEstimator estimador;
    @Autowired UserProfileCommandService perfiles;
    @Autowired JdbcTemplate jdbc;

    static final LocalDate S1 = LocalDate.of(2026, 11, 2);
    private int secuencia;

    record Caso(GoalType objetivo, FitnessLevel nivel, List<Short> dias, int minutos, String lugar) {
        @Override public String toString() { return objetivo + "/" + nivel + "/" + dias + "/" + minutos + "/" + lugar; }
    }

    @Test
    @DisplayName("U06: GAIN_MUSCLE avanzado, gimnasio, 5 dias no seguidos, 60 min")
    void usuarioU06() {
        var fallos = new ArrayList<String>();
        generar(new Caso(GoalType.GAIN_MUSCLE, FitnessLevel.ADVANCED, dias(1, 3, 4, 6, 7), 60, "GYM"), fallos);
        assertEquals(List.of(), fallos);
    }

    @Test
    @DisplayName("Barrido de 60 min sin GAIN_MUSCLE: 3 objetivos x 3 niveles x 5 y 6 dias (seguidos y como U06/U07) x gimnasio y casa")
    void barridoSesentaMinutos() {
        barrer(List.of(GoalType.LOSE_WEIGHT, GoalType.INCREASE_STRENGTH, GoalType.GENERAL_FITNESS));
    }

    @Test
    @DisplayName("4D v3.2 y v3.3: GAIN_MUSCLE a 60 min con 5 y 6 dias publica plan en las 24 combinaciones")
    void gainMuscleSesentaMinutos() {
        barrer(List.of(GoalType.GAIN_MUSCLE));
    }

    /**
     * 4D v3.3, verificacion previa (orden del 31/10/2026, fase E): en casa, intermedio, el dia PULL
     * no tiene traccion estructural de brazo; con las dosis de 4D v3.2 la mejor composicion (6
     * estructurales de espalda + biceps de relleno 2x10) no llega al piso de V20, y con el biceps a
     * 3x10 si. Es el unico cambio posible de menor impacto: 3x13 no aplica (no es zona pequena) y un
     * segundo biceps 2x10 en lugar de un estructural acorta la sesion.
     */
    @Test
    @DisplayName("4D v3.3: en casa el PULL no tiene traccion estructural de brazo y solo llega al piso con el biceps a 3x10")
    void casaPullNecesitaLaSerieDelBiceps() {
        long u = participante(new Caso(GoalType.GAIN_MUSCLE, FitnessLevel.INTERMEDIATE, dias(1, 2, 3, 4, 5), 60, "CASA"));
        var ctx = planComandos.prepare(new GenerateWeeklyPlanCommand(u, S1, null, true, null, null)).context();
        var brazoEstructural = ctx.availableExercises().stream()
                .filter(c -> "upper arms".equals(c.bodyPartCode()) && esBicepsDePull(c))
                .filter(c -> MandatorySlot.of(c).isPresent()).map(CandidateExercise::exerciseId).toList();
        assertEquals(List.of(), brazoEstructural, "en casa no hay traccion estructural de upper arms");
        long piso = PlanDraftValidator.minimumSessionSeconds(ctx);
        var biceps = ctx.availableExercises().stream()
                .filter(c -> "upper arms".equals(c.bodyPartCode()) && esBicepsDePull(c))
                .findFirst().orElseThrow();
        var espalda = ctx.availableExercises().stream().filter(c -> "back".equals(c.bodyPartCode()))
                .filter(c -> MandatorySlot.of(c).isPresent()).limit(6).toList();
        assertEquals(6, espalda.size());
        for (short series : new short[]{2, 3}) {
            var ej = new ArrayList<PlanDraft.DraftExercise>();
            espalda.forEach(c -> ej.add(new PlanDraft.DraftExercise(c.exerciseId(), PrescriptionType.SETS_REPS,
                    (short) 3, (short) 10, null, null, (short) 90, null)));
            ej.add(new PlanDraft.DraftExercise(biceps.exerciseId(), PrescriptionType.SETS_REPS, series, (short) 10,
                    null, null, (short) 90, null));
            long segundos = estimador.estimateSeconds(new PlanDraft.DraftWorkout(S1.plusDays(1), WorkoutFocus.PULL,
                    "PULL", 60, ej), ctx.prescription());
            if (series == 2) assertTrue(segundos < piso, segundos + " s con el biceps 2x10, piso " + piso);
            else assertTrue(segundos >= piso, segundos + " s con el biceps 3x10, piso " + piso);
        }
    }

    private void barrer(List<GoalType> objetivos) {
        barrer(objetivos, c -> true);
    }

    private void barrer(List<GoalType> objetivos, java.util.function.Predicate<Caso> incluir) {
        var fallos = new ArrayList<String>();
        int n = 0;
        for (var objetivo : objetivos)
            for (var nivel : FitnessLevel.values())
                for (var d : List.of(dias(1, 2, 3, 4, 5), dias(1, 3, 4, 6, 7), dias(1, 2, 3, 4, 5, 6), dias(1, 2, 3, 5, 6, 7)))
                    for (var lugar : List.of("GYM", "CASA")) {
                        var caso = new Caso(objetivo, nivel, d, 60, lugar);
                        if (!incluir.test(caso)) continue;
                        generar(caso, fallos);
                        n++;
                    }
        System.out.println("BARRIDO_60 " + (n - fallos.size()) + "/" + n + " con plan");
        fallos.forEach(f -> System.out.println("BARRIDO_60_FALLO " + f));
        assertEquals(List.of(), fallos);
    }

    /** V22: en PULL, de upper arms solo el biceps. */
    private static boolean esBicepsDePull(CandidateExercise c) {
        return WorkoutFocus.PULL.accepts(c.bodyPartCode()) && c.targetMuscle() != null
                && c.targetMuscle().toLowerCase(java.util.Locale.ROOT).contains("biceps");
    }

    private static List<Short> dias(int... d) {
        var l = new ArrayList<Short>();
        for (int x : d) l.add((short) x);
        return List.copyOf(l);
    }

    private void generar(Caso caso, List<String> fallos) {
        long u = participante(caso);
        try {
            planning.generateWeeklyPlan(u, S1, PlanAdjustment.none(), null, null)
                    .orElseThrow(() -> new AssertionError("sin plan: " + caso));
        } catch (PlanGenerationFailedException e) {
            fallos.add(caso + " -> " + e.getMessage() + borrador(u));
            return;
        }
        if (caso.objetivo() == GoalType.GAIN_MUSCLE) seriesAdicionales(caso, u, fallos);
    }

    /**
     * 4D v3.2, independiente del validador: en el plan publicado, toda colocacion con mas series que
     * su dosis 4D es relleno de zona pequena con 3x13, y es NECESARIA (con 2x13 la sesion baja del
     * piso de V20). Las demas llevan exactamente su dosis.
     */
    private void seriesAdicionales(Caso caso, long u, List<String> fallos) {
        var ctx = planComandos.prepare(new GenerateWeeklyPlanCommand(u, S1, null, true, null, null)).context();
        Map<Long, CandidateExercise> elegibles = ctx.availableExercises().stream()
                .collect(Collectors.toMap(CandidateExercise::exerciseId, Function.identity(), (a, b) -> a));
        long piso = PlanDraftValidator.minimumSessionSeconds(ctx);
        long plan = jdbc.queryForObject("SELECT plan_id FROM weekly_training_plans WHERE user_id = ?", Long.class, u);
        for (var w : jdbc.queryForList("SELECT planned_workout_id, scheduled_date, focus_code,"
                + " expected_duration_minutes FROM planned_workouts WHERE plan_id = ?", plan)) {
            long id = ((Number) w.get("planned_workout_id")).longValue();
            var ejercicios = jdbc.query("SELECT exercise_id, planned_sets, planned_reps, rest_seconds"
                            + " FROM planned_workout_exercises WHERE planned_workout_id = ? ORDER BY exercise_order",
                    (rs, i) -> new PlanDraft.DraftExercise(rs.getLong(1), PrescriptionType.SETS_REPS,
                            rs.getShort(2), rs.getShort(3), null, null, rs.getShort(4), null), id);
            var fecha = ((java.sql.Date) w.get("scheduled_date")).toLocalDate();
            var foco = WorkoutFocus.valueOf((String) w.get("focus_code"));
            for (int k = 0; k < ejercicios.size(); k++) {
                var e = ejercicios.get(k);
                var c = elegibles.get(e.exerciseId());
                var d = MvpDosePolicy.dosisPara(MandatorySlot.of(c), c.bodyPartCode()).orElseThrow();
                if (e.plannedSets() == d.series()) continue;
                if (e.plannedSets() != MvpDosePolicy.seriesMaxEn(d, foco, c.bodyPartCode())
                        || MvpDosePolicy.seriesMaxEn(d, foco, c.bodyPartCode()) == d.series()) {
                    fallos.add(caso + " " + fecha + ": " + e.exerciseId() + " lleva " + e.plannedSets()
                            + " series y su dosis es " + d.describe());
                    continue;
                }
                var sin = new ArrayList<>(ejercicios);
                sin.set(k, new PlanDraft.DraftExercise(e.exerciseId(), e.prescriptionType(), d.series(),
                        e.plannedReps(), null, null, e.restSeconds(), null));
                long segundos = estimador.estimateSeconds(new PlanDraft.DraftWorkout(fecha, foco, "s",
                        ((Number) w.get("expected_duration_minutes")).intValue(), sin), ctx.prescription());
                if (segundos >= piso)
                    fallos.add(caso + " " + fecha + ": la serie adicional de " + e.exerciseId()
                            + " sobra (" + segundos + " s >= " + piso + " s)");
            }
        }
    }

    /** El borrador del motor, sesion a sesion, para leer un fallo. */
    private String borrador(long u) {
        var ctx = planComandos.prepare(new GenerateWeeklyPlanCommand(u, S1, null, true, null, null)).context();
        Map<Long, CandidateExercise> elegibles = ctx.availableExercises().stream()
                .collect(Collectors.toMap(CandidateExercise::exerciseId, Function.identity(), (a, b) -> a));
        var sb = new StringBuilder();
        for (var w : motor.generate(ctx, List.of()).workouts()) {
            sb.append("\n  ").append(w.scheduledDate()).append(' ').append(w.focus()).append(' ')
                    .append(estimador.estimateSeconds(w, ctx.prescription())).append(" s:");
            for (var e : w.exercises()) {
                var c = elegibles.get(e.exerciseId());
                sb.append(' ').append(e.exerciseId()).append('[').append(c == null ? "?" : c.bodyPartCode() + "/"
                        + c.movementPattern() + "/" + CapabilityPrescriptionPolicy.claseDe(c)).append("] ")
                        .append(e.plannedSets()).append('x')
                        .append(e.plannedReps() != null ? e.plannedReps() + "r" : e.plannedDurationSeconds() + "s")
                        .append('/').append(e.restSeconds());
            }
        }
        return sb.toString();
    }

    private long participante(Caso caso) {
        String email = "r60-" + (++secuencia) + "-" + System.nanoTime() + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long u = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        boolean gym = "GYM".equals(caso.lugar());
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1992, 1, 15), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("70"), null, caso.nivel(), caso.objetivo(), "r60",
                gym ? TrainingLocation.GYM : TrainingLocation.HOME, (short) caso.dias().size(), caso.dias(),
                (short) caso.minutos(), gym ? GeneracionReferenciaBase.GIMNASIO : GeneracionReferenciaBase.CASA,
                List.of(), "Ninguna", true, true, true, true, true));
        return u;
    }
}
