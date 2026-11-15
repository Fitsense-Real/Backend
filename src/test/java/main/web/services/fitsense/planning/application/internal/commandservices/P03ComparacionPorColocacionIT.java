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
 *   <li>Propuesta P-03-COLOCACION (apagada, {@code fitsense.regeneracion.p03-por-colocacion}): la rechaza nombrando la
 *       colocacion. Activarla es una decision del propietario.</li>
 * </ul>
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("P-03 nivel 2: el total no detecta una colocacion que vuelve a la base; la regla por colocacion (apagada) si")
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

        p03.setPorColocacion(false);
        assertNull(p03.masTrabajoQueLaMedida(plan, regenerada),
                "politica vigente: el total no sube, P-03 nivel 2 aceptaria la semana aunque una colocacion suba");
        p03.setPorColocacion(true);
        try {
            var motivo = p03.masTrabajoQueLaMedida(plan, regenerada);
            assertNotNull(motivo, "con la regla por colocacion, se rechaza");
            assertTrue(motivo.contains("por colocacion") && motivo.contains(String.valueOf(reducida.ejercicio())), motivo);
        } finally {
            p03.setPorColocacion(false);
        }
    }
}
