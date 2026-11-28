package main.web.services.fitsense.planning.application.internal.commandservices;

import main.web.services.fitsense.RelojPruebas;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * RIESGO RESIDUAL DE P-03 NIVEL 2 (cierre tecnico conservador, fase 6). Contraejemplo determinista de la comprobacion
 * de trabajo de {@link SafeRegenerationService}: la semana medida lleva una colocacion REDUCIDA (2x10, como un
 * mantenimiento por fatiga); la regenerada la devuelve a la base (3x10) el mismo dia y quita otra colocacion, asi que el
 * trabajo TOTAL no sube.
 * <ul>
 *   <li>Politica vigente (P-03 compara solo el total): la comprobacion la ACEPTA. Esta prueba documenta ese
 *       comportamiento; no lo corrige.</li>
 *   <li>P-03-COLOCACION ({@code fitsense.regeneracion.p03-por-colocacion}, ComparacionDosisPorColocacion): la rechaza
 *       nombrando la colocacion. Aprobada por el propietario para la candidata premerge y sus pruebas (encendida en
 *       src/test/resources/config/application.properties); apagada en produccion hasta autorizacion expresa.</li>
 * </ul>
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("P-03 nivel 2: el total no detecta una colocacion que vuelve a la base; la regla por colocacion si")
class P03ComparacionPorColocacionIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired SafeRegenerationService p03;
    @Autowired PlanningContextFacade planning;
    @Autowired UserProfileCommandService perfiles;
    @Autowired JdbcTemplate jdbc;

    record Fila(long pwe, LocalDate fecha, String foco, long ejercicio, short series, short reps, short descanso) {}

    @Test
    @DisplayName("contraejemplo: 2x10 medido -> 3x10 regenerado el mismo dia, total sin subir")
    void contraejemplo() {
        long u = 9_850;
        jdbc.update("INSERT INTO users (user_id, email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " OVERRIDING SYSTEM VALUE VALUES (?, ?, 'x', 'Prueba', now(), now())", u, "p03-" + u + "@pruebas.invalid");
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 3, 3), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("65"), null, FitnessLevel.INTERMEDIATE, GoalType.GAIN_MUSCLE,
                "p03", TrainingLocation.GYM, (short) 3, List.of((short) 1, (short) 3, (short) 5), (short) 45,
                List.of("dumbbell", "barbell", "cable", "leverage machine", "bench"), List.of(), "Ninguna",
                true, true, true, true, true));
        long plan = planning.generateWeeklyPlan(u, RelojPruebas.SEMANA_REFERENCIA, PlanAdjustment.none(), null, null)
                .orElseThrow();
        var filas = jdbc.query("SELECT pwe.planned_exercise_id, pw.scheduled_date, pw.focus_code, pwe.exercise_id,"
                        + " pwe.planned_sets, pwe.planned_reps, pwe.rest_seconds FROM planned_workouts pw JOIN"
                        + " planned_workout_exercises pwe ON pwe.planned_workout_id = pw.planned_workout_id WHERE pw.plan_id = ?"
                        + " AND pwe.prescription_type = 'SETS_REPS' AND pwe.planned_sets >= 3"
                        + " ORDER BY pw.scheduled_date, pwe.exercise_order", (rs, k) -> new Fila(rs.getLong(1),
                        rs.getDate(2).toLocalDate(), rs.getString(3), rs.getLong(4), rs.getShort(5), rs.getShort(6),
                        rs.getShort(7)), plan);
        assertTrue(filas.size() >= 2, "premisa: al menos dos colocaciones estructurales de 3 series");
        var reducida = filas.get(0);
        // La semana medida: la colocacion 'reducida' a una serie menos (como el mantenimiento P-A1).
        jdbc.update("UPDATE planned_workout_exercises SET planned_sets = planned_sets - 1 WHERE planned_exercise_id = ?",
                reducida.pwe());
        // La regenerada: la semana medida tal cual, con la colocacion reducida de nuevo en la base (3 series) y sin
        // otra colocacion (la que dolio), de modo que el total no sube.
        var quitada = filas.get(1);
        var todas = jdbc.query("SELECT pwe.planned_exercise_id, pw.scheduled_date, pw.focus_code, pwe.exercise_id,"
                + " pwe.planned_sets, coalesce(pwe.planned_reps, 0), coalesce(pwe.rest_seconds, 60) FROM planned_workouts pw JOIN"
                + " planned_workout_exercises pwe ON pwe.planned_workout_id = pw.planned_workout_id WHERE pw.plan_id = ?"
                + " AND pwe.prescription_type = 'SETS_REPS' ORDER BY pw.scheduled_date, pwe.exercise_order",
                (rs, k) -> new Fila(rs.getLong(1), rs.getDate(2).toLocalDate(), rs.getString(3), rs.getLong(4),
                        rs.getShort(5), rs.getShort(6), rs.getShort(7)), plan);
        var porDia = new TreeMap<LocalDate, List<PlanDraft.DraftExercise>>();
        var foco = new HashMap<LocalDate, String>();
        for (var f : todas) {
            if (f.pwe() == quitada.pwe()) continue;
            short series = f.pwe() == reducida.pwe() ? (short) (f.series() + 1) : f.series();
            porDia.computeIfAbsent(f.fecha().plusWeeks(1), k -> new ArrayList<>()).add(new PlanDraft.DraftExercise(
                    f.ejercicio(), PrescriptionType.SETS_REPS, series, f.reps(), null, null, f.descanso(), null));
            foco.put(f.fecha().plusWeeks(1), f.foco());
        }
        var sesiones = porDia.entrySet().stream().map(e -> new PlanDraft.DraftWorkout(e.getKey(),
                WorkoutFocus.valueOf(foco.get(e.getKey())), "regenerada", 45, e.getValue())).toList();
        var regenerada = new PlanDraft(null, null, null, null, "contraejemplo", sesiones);

        boolean configurada = configuradaEnElContexto();
        assertTrue(configurada, "la candidata habilita P-03 por colocacion en sus pruebas (config/application.properties)");
        p03.setPorColocacion(false);
        assertNull(p03.masTrabajoQueLaMedida(plan, regenerada),
                "politica vigente: el total no sube, P-03 nivel 2 aceptaria la semana aunque una colocacion suba");
        p03.setPorColocacion(true);
        try {
            var motivo = p03.masTrabajoQueLaMedida(plan, regenerada);
            assertNotNull(motivo, "con la regla por colocacion, se rechaza");
            assertTrue(motivo.contains("por colocacion") && motivo.contains(String.valueOf(reducida.ejercicio())), motivo);
        } finally {
            p03.setPorColocacion(configurada);
        }
    }

    /**
     * Caso 5 (ejercicio repetido o cambiado de dia): la colocacion reducida (2 series) no vuelve a su dia, sino que
     * aparece OTRO dia a 3 series. No hay correspondencia fiable (otro dia), asi que se compara con la MENOR dosis
     * medida del ejercicio en la semana (la reducida) y se rechaza, aunque el total no suba.
     */
    @Test
    @DisplayName("cambio de dia: la reducida reaparece otro dia a la base; sin correspondencia, se compara con la menor y se rechaza")
    void cambioDeDia() {
        long u = 9_851;
        jdbc.update("INSERT INTO users (user_id, email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " OVERRIDING SYSTEM VALUE VALUES (?, ?, 'x', 'Prueba', now(), now())", u, "p03-" + u + "@pruebas.invalid");
        perfiles.handle(new CreateUserProfileCommand(u, LocalDate.of(1990, 3, 3), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("65"), null, FitnessLevel.INTERMEDIATE, GoalType.GAIN_MUSCLE,
                "p03", TrainingLocation.GYM, (short) 3, List.of((short) 1, (short) 3, (short) 5), (short) 45,
                List.of("dumbbell", "barbell", "cable", "leverage machine", "bench"), List.of(), "Ninguna",
                true, true, true, true, true));
        long plan = planning.generateWeeklyPlan(u, RelojPruebas.SEMANA_REFERENCIA, PlanAdjustment.none(), null, null)
                .orElseThrow();
        var todas = jdbc.query("SELECT pwe.planned_exercise_id, pw.scheduled_date, pw.focus_code, pwe.exercise_id,"
                + " pwe.planned_sets, coalesce(pwe.planned_reps, 0), coalesce(pwe.rest_seconds, 60) FROM planned_workouts pw JOIN"
                + " planned_workout_exercises pwe ON pwe.planned_workout_id = pw.planned_workout_id WHERE pw.plan_id = ?"
                + " AND pwe.prescription_type = 'SETS_REPS' ORDER BY pw.scheduled_date, pwe.exercise_order",
                (rs, k) -> new Fila(rs.getLong(1), rs.getDate(2).toLocalDate(), rs.getString(3), rs.getLong(4),
                        rs.getShort(5), rs.getShort(6), rs.getShort(7)), plan);
        // La reducida: una estructural de 3 series del primer dia cuyo ejercicio no se repite en la semana.
        var reducida = todas.stream().filter(f -> f.series() >= 3 && f.fecha().equals(todas.get(0).fecha())
                && todas.stream().filter(g -> g.ejercicio() == f.ejercicio()).count() == 1).findFirst().orElseThrow();
        var quitada = todas.stream().filter(f -> f.series() >= 3 && f.pwe() != reducida.pwe()).findFirst().orElseThrow();
        var otroDia = todas.stream().map(Fila::fecha).filter(d -> !d.equals(reducida.fecha())).findFirst().orElseThrow();
        jdbc.update("UPDATE planned_workout_exercises SET planned_sets = planned_sets - 1 WHERE planned_exercise_id = ?",
                reducida.pwe());
        var porDia = new TreeMap<LocalDate, List<PlanDraft.DraftExercise>>();
        var foco = new HashMap<LocalDate, String>();
        for (var f : todas) {
            foco.put(f.fecha().plusWeeks(1), f.foco());
            if (f.pwe() == quitada.pwe() || f.pwe() == reducida.pwe()) continue;
            porDia.computeIfAbsent(f.fecha().plusWeeks(1), k -> new ArrayList<>()).add(new PlanDraft.DraftExercise(
                    f.ejercicio(), PrescriptionType.SETS_REPS, f.series(), f.reps(), null, null, f.descanso(), null));
        }
        porDia.computeIfAbsent(otroDia.plusWeeks(1), k -> new ArrayList<>()).add(new PlanDraft.DraftExercise(
                reducida.ejercicio(), PrescriptionType.SETS_REPS, reducida.series(), reducida.reps(), null, null,
                reducida.descanso(), null));
        var sesiones = porDia.entrySet().stream().map(e -> new PlanDraft.DraftWorkout(e.getKey(),
                WorkoutFocus.valueOf(foco.get(e.getKey())), "regenerada", 45, e.getValue())).toList();
        var regenerada = new PlanDraft(null, null, null, null, "cambio de dia", sesiones);

        boolean configurada = configuradaEnElContexto();
        try {
            p03.setPorColocacion(false);
            assertNull(p03.masTrabajoQueLaMedida(plan, regenerada), "premisa: el total no sube");
            p03.setPorColocacion(true);
            var motivo = p03.masTrabajoQueLaMedida(plan, regenerada);
            assertNotNull(motivo, "la reducida no puede reaparecer a la base en otro dia");
            assertTrue(motivo.contains("SIN_CORRESPONDENCIA") && motivo.contains(String.valueOf(reducida.ejercicio())),
                    motivo);
        } finally {
            p03.setPorColocacion(configurada);
        }
    }

    /** El valor de fitsense.regeneracion.p03-por-colocacion en el contexto de pruebas (el bean, no el proxy). */
    private boolean configuradaEnElContexto() {
        Object destino = org.springframework.test.util.AopTestUtils.getUltimateTargetObject(p03);
        return (Boolean) org.springframework.test.util.ReflectionTestUtils.getField(destino, "porColocacion");
    }
}
