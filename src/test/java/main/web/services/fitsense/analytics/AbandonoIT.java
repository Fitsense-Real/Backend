package main.web.services.fitsense.analytics;

import main.web.services.fitsense.analytics.interfaces.acl.AnalyticsContextFacade;
import main.web.services.fitsense.execution.domain.model.commands.FinishWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.model.commands.RecordSessionExerciseCommand;
import main.web.services.fitsense.execution.domain.model.commands.StartWorkoutSessionCommand;
import main.web.services.fitsense.execution.domain.services.WorkoutSessionCommandService;
import main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import main.web.services.fitsense.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
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
import main.web.services.fitsense.shared.domain.model.valueobjects.TrainingWeek;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ABANDONO: 14 dias sin una sesion VALIDA (cuenta para la adherencia y llega al
 * 30 %), contados en el dia local del participante. Una sesion por debajo del
 * 30 % aporta su porcentaje pero no reinicia el contador.
 * <p>
 * PostgreSQL 16 efimero con V1-V33, motor de reglas, sin Replicate. Las
 * sesiones se fechan por SQL en hora local para controlar los dias exactos.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
class AbandonoIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired PlanningContextFacade planning;
    @Autowired AnalyticsContextFacade analytics;
    @Autowired UserProfileCommandService perfiles;
    @Autowired WorkoutSessionCommandService sesiones;
    @Autowired TrainingPlanController planes;
    @Autowired UserRepository usuarios;
    @Autowired JdbcTemplate jdbc;

    static final AtomicInteger SECUENCIA = new AtomicInteger();
    static final LocalDate S1 = TrainingWeek.containing(LocalDate.now()).startDate().plusWeeks(1);
    /** Un domingo fijo de referencia; las sesiones se fechan alrededor de el. */
    static final LocalDate DOMINGO = S1.plusDays(6);
    static final List<String> EQUIPAMIENTO = List.of("dumbbell", "barbell", "cable", "leverage machine", "bench", "band");

    record Participante(long userId, List<Long> dias, List<Long> ejercicios, String zona) {}

    private Participante participante(String zona) {
        int n = SECUENCIA.incrementAndGet();
        String email = "abandono" + n + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at, timezone)"
                + " VALUES (?, 'x', 'Prueba', now(), now(), ?)", email, zona);
        long userId = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(userId, LocalDate.of(1992, 5, 10), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("68"), null, FitnessLevel.INTERMEDIATE, GoalType.GAIN_MUSCLE,
                "prueba de abandono", TrainingLocation.GYM, (short) 3,
                List.of((short) 1, (short) 3, (short) 5), (short) 45, EQUIPAMIENTO,
                List.of(), "Ninguna", true, true, true, true, true));
        long plan = planning.generateWeeklyPlan(userId, S1, PlanAdjustment.none(), null, null).orElseThrow();
        var dias = jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ?"
                + " ORDER BY scheduled_date", Long.class, plan);
        // Cada entrenamiento con un solo ejercicio de 2x10 = 20 repeticiones.
        var ejercicios = dias.stream().map(pw -> {
            var ids = jdbc.queryForList("SELECT planned_exercise_id FROM planned_workout_exercises"
                    + " WHERE planned_workout_id = ? ORDER BY exercise_order", Long.class, pw);
            for (int k = 1; k < ids.size(); k++)
                jdbc.update("DELETE FROM planned_workout_exercises WHERE planned_exercise_id = ?", ids.get(k));
            jdbc.update("UPDATE planned_workout_exercises SET prescription_type = 'SETS_REPS', planned_sets = 2,"
                    + " planned_reps = 10, planned_duration_seconds = NULL WHERE planned_exercise_id = ?", ids.get(0));
            return ids.get(0);
        }).toList();
        return new Participante(userId, dias, ejercicios, zona);
    }

    /**
     * Ejecuta el entrenamiento {@code i} con {@code reps} de 20 y fecha la sesion
     * en hora LOCAL del participante.
     */
    private BigDecimal sesion(Participante p, int i, int reps, String fechaHoraLocal) {
        long s = sesiones.handle(new StartWorkoutSessionCommand(p.userId(), p.dias().get(i))).orElseThrow().getId();
        sesiones.handle(new RecordSessionExerciseCommand(p.userId(), s, p.ejercicios().get(i), (short) 2, reps,
                null, null, reps < 20 ? SkipReason.FATIGUE : null));
        sesiones.handle(new FinishWorkoutSessionCommand(p.userId(), s, (short) 6, null, (short) 30));
        jdbc.update("UPDATE workout_sessions SET started_at = (timestamp '" + fechaHoraLocal + "' AT TIME ZONE ?),"
                + " ended_at = (timestamp '" + fechaHoraLocal + "' AT TIME ZONE ?) + interval '30 minutes'"
                + " WHERE session_id = ?", p.zona(), p.zona(), s);
        return jdbc.queryForObject("SELECT completion_percentage FROM workout_sessions WHERE session_id = ?",
                BigDecimal.class, s);
    }

    /** Mide la semana que termina en ese domingo y devuelve dias sin entrenar y abandono. */
    private Map<String, Object> medirHasta(Participante p, LocalDate domingo) {
        assertEquals(DayOfWeek.SUNDAY, domingo.getDayOfWeek());
        var lunes = TrainingWeek.containing(domingo).startDate();
        analytics.calculateWeek(p.userId(), lunes);
        return jdbc.queryForMap("SELECT days_since_last_workout, is_dropout FROM weekly_user_metrics"
                + " WHERE user_id = ? AND week_start_date = ?", p.userId(), lunes);
    }

    private static Integer dias(Map<String, Object> fila) {
        var d = fila.get("days_since_last_workout");
        return d == null ? null : ((Number) d).intValue();
    }

    /**
     * Sesion al 100 % el domingo de referencia y otra al X % siete dias despues.
     * Medido catorce dias despues de la primera: si la segunda es valida, 7 dias
     * y sin abandono; si no, 14 dias y abandono.
     */
    @ParameterizedTest(name = "{0} de 20 repeticiones = {1} % -> {2} dias, abandono {3}")
    @CsvSource({
            "16, 80.00, 7,  false",
            " 6, 30.00, 7,  false",
            " 5, 25.00, 14, true",
            " 2, 10.00, 14, true"})
    @DisplayName("solo una sesion de 30 % o mas reinicia el contador")
    void reiniciaSoloConSesionValida(int reps, String pct, int diasEsperados, boolean abandono) {
        var p = participante("America/Lima");
        assertEquals(0, new BigDecimal("100.00").compareTo(sesion(p, 0, 20, DOMINGO + " 10:00")));
        assertEquals(0, new BigDecimal(pct).compareTo(sesion(p, 1, reps, DOMINGO.plusDays(7) + " 10:00")));

        var fila = medirHasta(p, DOMINGO.plusDays(14));
        assertEquals(diasEsperados, dias(fila));
        assertEquals(abandono, fila.get("is_dropout"));

        // La sesion por debajo del 30 % sigue en el historial y cuenta para la adherencia.
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM workout_sessions WHERE user_id = ?"
                + " AND counts_toward_adherence", Integer.class, p.userId()));
    }

    @Test
    @DisplayName("un entrenamiento omitido no reinicia el contador")
    void omitidoNoReinicia() {
        var p = participante("America/Lima");
        sesion(p, 0, 20, DOMINGO + " 10:00");
        planes.skip(UserDetailsImpl.build(usuarios.findById(p.userId()).orElseThrow()), p.dias().get(1),
                new SkipWorkoutResource(SkipReason.LACK_OF_TIME));
        var fila = medirHasta(p, DOMINGO.plusDays(14));
        assertEquals(14, dias(fila));
        assertEquals(true, fila.get("is_dropout"));
    }

    @Test
    @DisplayName("13 dias no es abandono; 14 si")
    void limites13y14() {
        // Ultima sesion valida el lunes siguiente al domingo de referencia: 13 dias al domingo D+14.
        var p13 = participante("America/Lima");
        sesion(p13, 0, 20, DOMINGO.plusDays(1) + " 10:00");
        var f13 = medirHasta(p13, DOMINGO.plusDays(14));
        assertEquals(13, dias(f13));
        assertEquals(false, f13.get("is_dropout"));

        var p14 = participante("America/Lima");
        sesion(p14, 0, 20, DOMINGO + " 10:00");
        var f14 = medirHasta(p14, DOMINGO.plusDays(14));
        assertEquals(14, dias(f14));
        assertEquals(true, f14.get("is_dropout"));
    }

    @Test
    @DisplayName("cerca de la medianoche cuenta el dia local: domingo 23:50 en Lima son 14 dias, no 13")
    void medianoche() {
        // 23:50 en Lima es 04:50 UTC del lunes: con la fecha UTC salian 13 dias y no habia abandono.
        var p = participante("America/Lima");
        sesion(p, 0, 20, DOMINGO + " 23:50");
        var fila = medirHasta(p, DOMINGO.plusDays(14));
        assertEquals(14, dias(fila));
        assertEquals(true, fila.get("is_dropout"));

        // Diez minutos despues ya es lunes en Lima: 13 dias.
        var q = participante("America/Lima");
        sesion(q, 0, 20, DOMINGO.plusDays(1) + " 00:10");
        assertEquals(13, dias(medirHasta(q, DOMINGO.plusDays(14))));
    }

    @Test
    @DisplayName("se usa la zona del participante: lunes 00:30 en Tokio es domingo en UTC, y cuenta como lunes")
    void zonaDelParticipante() {
        var p = participante("Asia/Tokyo");
        sesion(p, 0, 20, DOMINGO.plusDays(1) + " 00:30");
        assertEquals(13, dias(medirHasta(p, DOMINGO.plusDays(14))));
    }

    @Test
    @DisplayName("una sesion posterior al cierre de la semana medida no cuenta para esa semana")
    void posteriorAlCierre() {
        var p = participante("America/Lima");
        sesion(p, 0, 20, DOMINGO + " 10:00");
        sesion(p, 1, 20, DOMINGO.plusDays(15) + " 10:00");
        var fila = medirHasta(p, DOMINGO.plusDays(14));
        assertEquals(14, dias(fila));
        assertEquals(true, fila.get("is_dropout"));
    }

    @Test
    @DisplayName("nunca entreno, o solo por debajo del 30 %: sin dias ni abandono")
    void sinSesionValida() {
        var nunca = participante("America/Lima");
        var f = medirHasta(nunca, DOMINGO.plusDays(14));
        assertNull(dias(f));
        assertEquals(false, f.get("is_dropout"));

        var pobre = participante("America/Lima");
        sesion(pobre, 0, 2, DOMINGO + " 10:00");
        var g = medirHasta(pobre, DOMINGO.plusDays(14));
        assertNull(dias(g), "sin sesion valida no se inventa una fecha");
        assertEquals(false, g.get("is_dropout"));
    }
}
