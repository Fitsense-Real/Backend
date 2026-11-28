package main.web.services.fitsense.orchestation;

import main.web.services.fitsense.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.planning.domain.services.AdaptedWeekProducer;
import main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel;
import main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * RETIRADA LOCALIZADA ANTE DOLOR (candidata), de extremo a extremo: PostgreSQL efimero, servicios reales, IA apagada.
 * Los perfiles, la semana (2026-11-02) y los ids son los CONGELADOS del laboratorio de 22 perfiles: el perfil Rnnnn es
 * el usuario nnnn, porque la semilla del motor de reglas es userId x 1.000.003 + semana. Cada subclase fija la
 * estrategia del limite y el interruptor de la retirada; dentro de una clase los perfiles se crean en orden de id.
 */
abstract class RetiradaLocalizadaBase extends SinSustitutoDolorBase {

    static final LocalDate MED = SinSustitutoDolor24CasosIT.SEMANA_DEL_BARRIDO, NUE = MED.plusWeeks(1),
            SIG = MED.plusWeeks(2);

    @Autowired AdaptedWeekProducer producer;

    record Escenario(long usuario, long planMedido, List<Long> sesiones135, Map<String, Object> v32, Optional<Long> plan,
                     List<Map<String, Object>> p03) {}

    /** El perfil congelado: id alineado, plan real de la semana del barrido, dolor en el 135 y ciclo semanal. */
    Escenario escenario(String perfil, GoalType objetivo, FitnessLevel nivel, int dias, int minutos) {
        when(ia.isEnabled()).thenReturn(false);
        long objetivoId = Long.parseLong(perfil.substring(1));
        long ultimo = jdbc.queryForObject("SELECT COALESCE(MAX(user_id), 0) FROM users", Long.class);
        assertTrue(ultimo < objetivoId, "los perfiles de una clase se crean en orden de id");
        while (ultimo < objetivoId - 1)
            ultimo = jdbc.queryForObject("INSERT INTO users (email, password_hash, first_name, consent_granted_at,"
                    + " enrolled_at) VALUES (?, 'x', 'Relleno', now(), now()) RETURNING user_id", Long.class,
                    "relleno" + UUID.randomUUID() + "@pruebas.invalid");
        long u = participante(objetivo, nivel, dias, minutos);
        assertEquals(objetivoId, u, "el usuario tiene el id congelado");
        long plan = planning.generateWeeklyPlan(u, MED, PlanAdjustment.none(), null, null).orElseThrow();
        var con135 = jdbc.queryForList("SELECT DISTINCT pw.planned_workout_id FROM planned_workouts pw JOIN"
                + " planned_workout_exercises pwe ON pwe.planned_workout_id = pw.planned_workout_id WHERE pw.plan_id = ?"
                + " AND pwe.exercise_id = ? ORDER BY 1", Long.class, plan, EJ);
        assertFalse(con135.isEmpty(), "premisa: el plan congelado lleva el 135");
        ejecutar(u, plan, con135.get(0));
        ciclo.runFor(u, MED, NUE);
        return new Escenario(u, plan, con135, intento(u, NUE), planActivo(u, NUE),
                jdbc.queryForList("SELECT outcome, resulting_plan_id, reason_code FROM safe_regeneration_attempts"
                        + " WHERE user_id = ?", u));
    }

    /** Sesiones de un plan: origen -> lista de (ejercicio, series, reps, segundos, descanso) en orden. */
    Map<Long, List<List<Object>>> contenido(long plan, boolean porOrigen) {
        var m = new LinkedHashMap<Long, List<List<Object>>>();
        for (var f : jdbc.queryForList("SELECT pw.planned_workout_id, pw.source_planned_workout_id, pwe.exercise_id,"
                + " pwe.planned_sets, pwe.planned_reps, pwe.planned_duration_seconds, pwe.rest_seconds FROM planned_workouts"
                + " pw JOIN planned_workout_exercises pwe ON pwe.planned_workout_id = pw.planned_workout_id WHERE"
                + " pw.plan_id = ? ORDER BY pw.scheduled_date, pwe.exercise_order", plan)) {
            Object clave = porOrigen ? f.get("source_planned_workout_id") : f.get("planned_workout_id");
            m.computeIfAbsent(clave == null ? -1L : ((Number) clave).longValue(), k -> new ArrayList<>())
                    .add(Arrays.asList(f.get("exercise_id"), f.get("planned_sets"), f.get("planned_reps"),
                            f.get("planned_duration_seconds"), f.get("rest_seconds")));
        }
        return m;
    }

    int minutos(long plannedWorkout) {
        return jdbc.queryForObject("SELECT expected_duration_minutes FROM planned_workouts WHERE planned_workout_id = ?",
                Integer.class, plannedWorkout);
    }

    Integer minutosDeOrigen(long plan, long origen) {
        return jdbc.queryForObject("SELECT expected_duration_minutes FROM planned_workouts WHERE plan_id = ? AND"
                + " source_planned_workout_id = ?", Integer.class, plan, origen);
    }

    long[] trabajo(long plan) {
        var f = jdbc.queryForMap("SELECT COALESCE(SUM(COALESCE(pwe.planned_sets,1) * COALESCE(pwe.planned_reps,0)),0) AS r,"
                + " COALESCE(SUM(COALESCE(pwe.planned_sets,1) * COALESCE(pwe.planned_duration_seconds,0)),0) AS s FROM"
                + " planned_workouts pw JOIN planned_workout_exercises pwe ON pwe.planned_workout_id = pw.planned_workout_id"
                + " WHERE pw.plan_id = ?", plan);
        return new long[]{((Number) f.get("r")).longValue(), ((Number) f.get("s")).longValue()};
    }

    int activos(long u, LocalDate semana) {
        return jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ? AND week_start_date = ?"
                + " AND status = 'ACTIVE'", Integer.class, u, semana);
    }

    String resultadoApi(long u) {
        var vistos = planes.adaptations(UserDetailsImpl.build(usuarios.findById(u).orElseThrow())).getBody();
        return vistos == null || vistos.isEmpty() ? "-" : vistos.get(vistos.size() - 1).resultado();
    }

    String textoPublicado(long plan) {
        return jdbc.queryForObject("SELECT adjustment_reason FROM weekly_training_plans WHERE plan_id = ?", String.class,
                plan);
    }

    /**
     * La retirada localizada publicada: SOLO falta el 135 en su sesion, todo lo demas (las demas sesiones y el resto de
     * esa sesion, con sus dosis y descansos) es identico a la semana medida; el trabajo no sube; un solo plan activo;
     * no hizo falta P-03; V32 lo registra como RESPALDO conservado con la nota de la retirada.
     */
    void assertRetiradaPublicada(Escenario e) {
        assertTrue(e.plan().isPresent(), "se publica la semana con la retirada: " + e.v32());
        long nuevo = e.plan().get();
        assertEquals("RESPALDO", e.v32().get("status"), e.v32()::toString);
        assertEquals("CONSERVED", e.v32().get("generation_source"));
        assertTrue(String.valueOf(e.v32().get("informe")).contains(AdaptedWeekProducer.NOTA_RETIRADA_SEGURA.trim()),
                String.valueOf(e.v32().get("informe")));
        assertTrue(e.p03().isEmpty(), "no hizo falta regenerar: " + e.p03());
        assertEquals(1, activos(e.usuario(), NUE));
        assertFalse(ejerciciosDe(nuevo).contains(EJ), "el 135 no vuelve");

        var antes = contenido(e.planMedido(), false);
        var despues = contenido(nuevo, true);
        assertEquals(antes.keySet(), despues.keySet(), "linaje: cada sesion nueva deriva de su sesion medida");
        for (var origen : antes.keySet()) {
            var esperado = new ArrayList<>(antes.get(origen));
            if (origen.equals(e.sesiones135().get(0))) esperado.removeIf(x -> ((Number) x.get(0)).longValue() == EJ);
            assertEquals(esperado, despues.get(origen), "sesion " + origen + ": solo cambia la retirada del 135");
        }
        long s = e.sesiones135().get(0);
        assertTrue(minutosDeOrigen(nuevo, s) < minutos(s), "la sesion retirada dura menos");
        long[] t0 = trabajo(e.planMedido()), t1 = trabajo(nuevo);
        assertTrue(t1[0] <= t0[0] && t1[1] <= t0[1], "el trabajo no sube");
        assertEquals("RESPALDO_SIN_IA", resultadoApi(e.usuario()), "API: resultado vigente para un respaldo sin IA");
        var texto = textoPublicado(nuevo);
        assertTrue(texto.contains("Por seguridad, cambiamos o retiramos 1 ejercicio"), texto);
        assertFalse(texto.contains("no aplicamos cambios"), texto);
    }

    /** La retirada no se publico: NO_FACTIBLE con el motivo y P-03 siguio su camino. */
    void assertRetiradaRechazada(Escenario e, String motivo) {
        assertEquals("NO_FACTIBLE", e.v32().get("status"), e.v32()::toString);
        String rechazo = String.valueOf(e.v32().get("rejection_reason"));
        assertTrue(rechazo.contains("RETIRADA_LOCALIZADA no aplicable") && rechazo.contains(motivo), rechazo);
        assertFalse(e.p03().isEmpty(), "P-03 se intento");
        e.plan().ifPresent(p -> assertFalse(ejerciciosDe(p).contains(EJ)));
        assertTrue(activos(e.usuario(), NUE) <= 1);
    }

    /** Semana siguiente sin dolor: el 135 sigue fuera y, si hubo retirada, el resto sigue igual. */
    void assertSemanaSiguiente(Escenario e) {
        ejecutar(e.usuario(), e.plan().orElseThrow(), null);
        ciclo.runFor(e.usuario(), NUE, SIG);
        var siguiente = planActivo(e.usuario(), SIG);
        assertTrue(siguiente.isPresent());
        assertFalse(ejerciciosDe(siguiente.get()).contains(EJ), "la exclusion no caduca");
        assertEquals(1, activos(e.usuario(), SIG));
    }
}
