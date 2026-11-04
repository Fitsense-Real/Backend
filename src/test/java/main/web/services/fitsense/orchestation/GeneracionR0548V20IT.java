package main.web.services.fitsense.orchestation;

import main.web.services.fitsense.planning.domain.model.valueobjects.MvpDosePolicy;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel.INTERMEDIATE;
import static main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType.GAIN_MUSCLE;
import static org.junit.jupiter.api.Assertions.*;

/**
 * R0548: GAIN_MUSCLE intermedio, casa con mancuernas y bandas, 7 dias, 75 minutos, semana 2026-11-02, usuario 548 (la
 * semilla del motor de reglas es userId x 1.000.003 + semana). PRUEBA PERMANENTE DE GENERACION EXITOSA desde 4D v3.4.
 * <p>
 * REGISTRO HISTORICO DEL FALLO (no se borra): hasta 4D v3.4 esta prueba era un registro opt-in que AFIRMABA el fallo:
 * {@code PlanGenerationFailedException} por V20, 2.550 s frente a 3.150 s (70 % de 75 minutos), en 6 sesiones
 * (docs/FitSense/adaptacion/salida_intermedia_dolor/11_CIERRE_PR39.md). Causa raiz:
 * docs/FitSense/short_paper_ieee/k20_adaptacion_real/A1_DIAGNOSTICO_H05_R0548.md (el tope de 7 colocaciones de 4D
 * v3.1 no llegaba al piso de V20 por encima de 60 minutos). Corregido por docs/FitSense/ADENDA_4D_v3_4.md sin tocar
 * V20, V24, la dosis ni el catalogo.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("R0548 (GM intermedio, casa, 7 dias, 75 min): la generacion inicial publica un plan valido (4D v3.4)")
class GeneracionR0548V20IT extends SinSustitutoDolorBase {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    void generacionInicialPublicaUnPlanValido() {
        long ultimo = jdbc.queryForObject("SELECT COALESCE(MAX(user_id), 0) FROM users", Long.class);
        while (ultimo < 547)
            ultimo = jdbc.queryForObject("INSERT INTO users (email, password_hash, first_name, consent_granted_at,"
                    + " enrolled_at) VALUES (?, 'x', 'Relleno', now(), now()) RETURNING user_id", Long.class,
                    "relleno" + UUID.randomUUID() + "@pruebas.invalid");
        long u = participante(GAIN_MUSCLE, INTERMEDIATE, 7, 75);
        assertEquals(548L, u, "el usuario tiene el id congelado");
        long plan = planning.generateWeeklyPlan(u, SinSustitutoDolor24CasosIT.SEMANA_DEL_BARRIDO, PlanAdjustment.none(),
                null, null).orElseThrow();
        var sesiones = jdbc.queryForList("SELECT pw.planned_workout_id, count(e.*) AS n FROM planned_workouts pw JOIN"
                + " planned_workout_exercises e ON e.planned_workout_id = pw.planned_workout_id WHERE pw.plan_id = ?"
                + " GROUP BY pw.planned_workout_id", plan);
        assertEquals(7, sesiones.size(), "7 sesiones");
        for (var s : sesiones)
            assertTrue(((Number) s.get("n")).intValue() <= MvpDosePolicy.maxColocaciones(75),
                    "como mucho el tope de 4D v3.4 (9 colocaciones a 75 minutos): " + s);
        // La dosis no cambia: 4D (2-3 series, 10 o 13 repeticiones, 90 s), sin ejercicios por tiempo.
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM planned_workouts pw JOIN planned_workout_exercises e"
                + " ON e.planned_workout_id = pw.planned_workout_id WHERE pw.plan_id = ? AND (e.prescription_type <> 'SETS_REPS'"
                + " OR e.planned_sets NOT IN (2, 3) OR e.planned_reps NOT IN (10, 13) OR e.rest_seconds <> 90)", Integer.class,
                plan));
    }
}
