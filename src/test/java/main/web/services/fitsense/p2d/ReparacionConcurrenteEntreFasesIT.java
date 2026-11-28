package main.web.services.fitsense.p2d;

import main.web.services.fitsense.RelojPruebas;
import main.web.services.fitsense.planning.application.internal.commandservices.PreviousWeekAssembler;
import main.web.services.fitsense.planning.application.internal.commandservices.PublishedPlanSafetyRepairService.Estado;
import main.web.services.fitsense.planning.application.internal.commandservices.PublishedPlanSafetyRepairService.Resultado;
import main.web.services.fitsense.planning.domain.model.valueobjects.MandatorySlot;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;

/**
 * PD-16: la reparacion de seguridad trabaja por fases, cada una en su transaccion. Si OTRA reparacion publica una
 * version nueva de la semana entre la fase 1 (leer la version activa) y la lectura de la semana publicada de la fase 2,
 * la reparacion en curso trabaja con informacion mezclada: la v1 que leyo y la v2 como referencia. Debe responder
 * CAMBIO_CONCURRENTE, nunca un NO_VALIDA causado por esa mezcla.
 * <p>
 * Determinista: el cambio concurrente se fuerza en ese punto exacto. El espia de {@link PreviousWeekAssembler} ejecuta
 * la otra reparacion completa, en otro hilo, justo antes de la primera lectura de la semana publicada. No depende del
 * planificador del sistema operativo, como si dependia {@code ProteccionPlanesPublicadosIT.reparacionesSimultaneas}.
 * Semana fija ({@link RelojPruebas#SEMANA_REFERENCIA}); motor de reglas, sin IA ni Replicate.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("PD-16 · reparacion con la version activa cambiada entre fases: CAMBIO_CONCURRENTE, nunca NO_VALIDA")
class ReparacionConcurrenteEntreFasesIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired PlanningContextFacade planning;
    @Autowired UserProfileCommandService perfiles;
    @Autowired JdbcTemplate jdbc;
    @MockitoSpyBean PreviousWeekAssembler assembler;

    static final AtomicInteger SECUENCIA = new AtomicInteger();
    static final LocalDate SEMANA = RelojPruebas.SEMANA_REFERENCIA;
    /** La vispera: la semana no ha terminado ni empezado; todas sus sesiones estan abiertas. */
    static final LocalDate HOY = SEMANA.minusDays(1);
    static final String SIN_SLOT = " AND x.movement_pattern NOT IN ('KNEE_DOMINANT','HIP_HINGE','HIP_EXTENSION',"
            + "'HORIZONTAL_PUSH','VERTICAL_PUSH','HORIZONTAL_PULL','VERTICAL_PULL')";

    record Participante(long userId, long planId) {}

    private Participante participante() {
        int n = SECUENCIA.incrementAndGet();
        String email = "entrefases" + n + "@pruebas.invalid";
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, enrolled_at)"
                + " VALUES (?, 'x', 'Prueba', now(), now())", email);
        long userId = jdbc.queryForObject("SELECT user_id FROM users WHERE email = ?", Long.class, email);
        perfiles.handle(new CreateUserProfileCommand(userId, LocalDate.of(1992, 5, 10), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("68"), null, FitnessLevel.INTERMEDIATE, GoalType.GAIN_MUSCLE,
                "prueba PD-16", TrainingLocation.GYM, (short) 3,
                List.of((short) 1, (short) 3, (short) 5), (short) 45,
                List.of("dumbbell", "barbell", "cable", "leverage machine", "bench", "band"),
                List.of(), "Ninguna", true, true, true, true, true));
        long plan = planning.generateWeeklyPlan(userId, SEMANA, PlanAdjustment.none(), null, null)
                .orElseThrow(() -> new AssertionError("no se genero la semana"));
        return new Participante(userId, plan);
    }

    /** Un relleno (de cintura si hay) pasa a ser el 1374, como si se hubiera publicado antes de contenerlo. */
    private void con1374(Participante p) {
        long pe = jdbc.queryForObject("SELECT pwe.planned_exercise_id FROM planned_workout_exercises pwe"
                + " JOIN planned_workouts pw ON pw.planned_workout_id = pwe.planned_workout_id"
                + " JOIN exercises x ON x.exercise_id = pwe.exercise_id"
                + " JOIN body_parts bp ON bp.body_part_id = x.body_part_id"
                + " WHERE pw.plan_id = ? AND pwe.prescription_type = 'SETS_REPS'" + SIN_SLOT
                + " ORDER BY (bp.code = 'waist') DESC, pw.scheduled_date, pwe.exercise_order LIMIT 1",
                Long.class, p.planId());
        String patron = jdbc.queryForObject("SELECT x.movement_pattern FROM planned_workout_exercises pwe JOIN"
                + " exercises x ON x.exercise_id = pwe.exercise_id WHERE pwe.planned_exercise_id = ?", String.class, pe);
        assertTrue(MandatorySlot.ofMovementPattern(patron).isEmpty(), "premisa: un relleno");
        jdbc.update("UPDATE planned_workout_exercises SET exercise_id = 1374 WHERE planned_exercise_id = ?", pe);
    }

    private Resultado reparar(Participante p) {
        return planning.repairPublishedPlanForSafety(p.userId(), SEMANA, HOY);
    }

    private int versiones(Participante p) {
        return jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ? AND week_start_date = ?",
                Integer.class, p.userId(), SEMANA);
    }

    private int activas(Participante p) {
        return jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ? AND week_start_date = ?"
                + " AND status = 'ACTIVE'", Integer.class, p.userId(), SEMANA);
    }

    private long activa(Participante p) {
        return jdbc.queryForObject("SELECT plan_id FROM weekly_training_plans WHERE user_id = ? AND week_start_date = ?"
                + " AND status = 'ACTIVE'", Long.class, p.userId(), SEMANA);
    }

    @Test
    @DisplayName("control: sin cambio concurrente, el espia no altera nada y la reparacion se publica")
    void sinCambioConcurrente() {
        var p = participante();
        con1374(p);
        var r = reparar(p);
        assertEquals(Estado.REPARADA, r.estado(), r.toString());
        assertEquals(2, versiones(p));
        assertEquals(1, activas(p));
    }

    @Test
    @DisplayName("otra reparacion publica entre la fase 1 y la lectura de la semana publicada: CAMBIO_CONCURRENTE")
    void versionCambiadaEntreFases() throws Exception {
        var p = participante();
        con1374(p);

        // La primera lectura de la semana publicada de la reparacion A dispara, antes de leer, la reparacion B
        // completa en otro hilo. Las lecturas de B (y las de despues) son normales.
        var armado = new AtomicBoolean(true);
        var otra = new AtomicReference<Resultado>();
        doAnswer(inv -> {
            if (armado.compareAndSet(true, false)) {
                var hilo = Executors.newSingleThreadExecutor();
                try {
                    otra.set(hilo.submit(() -> reparar(p)).get(120, TimeUnit.SECONDS));
                } finally {
                    hilo.shutdownNow();
                }
            }
            return inv.callRealMethod();
        }).when(assembler).assemble(anyLong(), any(), anyInt(), any(), any());

        var r = reparar(p);

        assertFalse(armado.get(), "premisa: el cambio concurrente se forzo entre las fases de A");
        assertNotNull(otra.get());
        assertEquals(Estado.REPARADA, otra.get().estado(), "premisa: la otra reparacion publica la v2: " + otra.get());
        assertEquals(Estado.CAMBIO_CONCURRENTE, r.estado(),
                "A leyo la v1 y la v2 ya es la activa: es un cambio concurrente, no una semana invalida: " + r);
        assertNull(r.planNuevo(), "A no publica nada");
        assertEquals(2, versiones(p), "solo la version de B");
        assertEquals(1, activas(p));
        assertEquals(otra.get().planNuevo(), activa(p), "la activa es la que publico B");
    }
}
