package main.web.services.fitsense.demo;

import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Coloca en el calendario de cada semana las horas de lo que el generador ejecuto «ahora» (SOLO en la base de prueba
 * del generador, antes de exportar). No cambia ejercicios, dosis, estados, fechas programadas ni metricas: solo las
 * marcas de tiempo, para que el historial se lea como lo habria dejado la app semana a semana.
 * <ul>
 *   <li>Perfil y rutina inicial: el domingo anterior a W-3 (alta del usuario), 19:00 y 19:05 en Lima.</li>
 *   <li>Planes del cierre semanal, metricas, intentos de adaptacion e intervenciones: el lunes de la semana nueva a
 *   las 03:00 (la hora del cierre programado).</li>
 *   <li>Sesiones: el dia programado a las 18:30; fin = inicio + minutos activos + 5; cada ejercicio registrado a
 *   intervalos regulares dentro de la sesion. Omisiones a mano: el dia programado a las 08:00.</li>
 *   <li>{@code expires_at}: el original (23:59 del dia programado), que el generador alargo para poder ejecutar.</li>
 *   <li>Exclusiones por dolor: la hora en que se registro el ejercicio con dolor.</li>
 * </ul>
 */
class CalendarioDemo {
    final JdbcTemplate jdbc; final long u; final ZoneId zona;
    CalendarioDemo(JdbcTemplate jdbc, long u, ZoneId zona) { this.jdbc = jdbc; this.u = u; this.zona = zona; }

    Timestamp en(LocalDate dia, int h, int m) {
        return Timestamp.from(dia.atTime(LocalTime.of(h, m)).atZone(zona).toInstant());
    }

    void colocar(Map<Long, Timestamp> expira, LocalDate w3) {
        // Planes por semana, en orden.
        var planes = jdbc.queryForList("SELECT plan_id, week_start_date, replaced_at FROM weekly_training_plans"
                + " WHERE user_id = ? ORDER BY week_start_date, plan_id", u);
        var creado = new HashMap<Long, Timestamp>();
        for (int i = 0; i < planes.size(); i++) {
            long id = ((Number) planes.get(i).get("plan_id")).longValue();
            LocalDate semana = ((java.sql.Date) planes.get(i).get("week_start_date")).toLocalDate();
            Timestamp t = i == 0 ? en(w3.minusDays(1), 19, 5) : en(semana, 3, 0);
            creado.put(id, t);
            Timestamp siguiente = en(semana.plusDays(7), 3, 0);
            jdbc.update("UPDATE weekly_training_plans SET created_at = ?, activated_at = CASE WHEN activated_at IS NULL"
                    + " THEN NULL ELSE ?::timestamptz END, replaced_at = CASE WHEN replaced_at IS NULL THEN NULL ELSE ?::timestamptz END"
                    + " WHERE plan_id = ?", t, t, siguiente, id);
            jdbc.update("UPDATE planned_workouts SET created_at = ?, updated_at = ? WHERE plan_id = ?", t, t, id);
            jdbc.update("UPDATE planned_workout_exercises pwe SET created_at = ? FROM planned_workouts pw"
                    + " WHERE pw.planned_workout_id = pwe.planned_workout_id AND pw.plan_id = ?", t, id);
        }
        expira.forEach((pw, t) -> jdbc.update("UPDATE planned_workouts SET expires_at = ? WHERE planned_workout_id = ?", t, pw));

        // Omisiones a mano (sin sesion): por la manana del dia programado.
        for (var f : jdbc.queryForList("SELECT pw.planned_workout_id, pw.scheduled_date FROM planned_workouts pw"
                + " JOIN weekly_training_plans p ON p.plan_id = pw.plan_id WHERE p.user_id = ? AND pw.status = 'SKIPPED'"
                + " AND NOT EXISTS (SELECT 1 FROM workout_sessions s WHERE s.planned_workout_id = pw.planned_workout_id)", u))
            jdbc.update("UPDATE planned_workouts SET updated_at = ? WHERE planned_workout_id = ?",
                    en(((java.sql.Date) f.get("scheduled_date")).toLocalDate(), 8, 0), f.get("planned_workout_id"));

        // Sesiones y ejercicios registrados.
        var registroDolor = new HashMap<Long, Timestamp>();
        for (var s : jdbc.queryForList("SELECT s.session_id, s.planned_workout_id, s.active_minutes, pw.scheduled_date"
                + " FROM workout_sessions s JOIN planned_workouts pw ON pw.planned_workout_id = s.planned_workout_id"
                + " WHERE s.user_id = ? ORDER BY s.session_id", u)) {
            long sid = ((Number) s.get("session_id")).longValue();
            LocalDate dia = ((java.sql.Date) s.get("scheduled_date")).toLocalDate();
            int min = s.get("active_minutes") == null ? 45 : ((Number) s.get("active_minutes")).intValue();
            Timestamp ini = en(dia, 18, 30);
            Timestamp fin = new Timestamp(ini.getTime() + (min + 5) * 60_000L);
            jdbc.update("UPDATE workout_sessions SET started_at = ?, ended_at = CASE WHEN ended_at IS NULL THEN NULL"
                    + " ELSE ?::timestamptz END, created_at = ?, updated_at = ? WHERE session_id = ?", ini, fin, ini, fin, sid);
            jdbc.update("UPDATE planned_workouts SET updated_at = ? WHERE planned_workout_id = ?", fin, s.get("planned_workout_id"));
            var ejercicios = jdbc.queryForList("SELECT wse.session_exercise_id, wse.planned_exercise_id, wse.skip_reason"
                    + " FROM workout_session_exercises wse JOIN planned_workout_exercises pwe"
                    + " ON pwe.planned_exercise_id = wse.planned_exercise_id WHERE wse.session_id = ?"
                    + " ORDER BY pwe.exercise_order, wse.session_exercise_id", sid);
            long paso = (fin.getTime() - 5 * 60_000L - ini.getTime()) / (ejercicios.size() + 1);
            for (int i = 0; i < ejercicios.size(); i++) {
                Timestamp t = new Timestamp(ini.getTime() + (i + 1) * paso);
                jdbc.update("UPDATE workout_session_exercises SET created_at = ?, updated_at = ? WHERE session_exercise_id = ?",
                        t, t, ejercicios.get(i).get("session_exercise_id"));
                registroDolor.put(((Number) ejercicios.get(i).get("planned_exercise_id")).longValue(), t);
            }
        }

        // Metricas: al cierre del lunes siguiente. Intentos e intervenciones: al cierre que publico la semana nueva.
        for (var m : jdbc.queryForList("SELECT weekly_metric_id, week_start_date FROM weekly_user_metrics WHERE user_id = ?", u))
            jdbc.update("UPDATE weekly_user_metrics SET calculated_at = ? WHERE weekly_metric_id = ?",
                    en(((java.sql.Date) m.get("week_start_date")).toLocalDate().plusDays(7), 3, 0), m.get("weekly_metric_id"));
        for (var a : jdbc.queryForList("SELECT attempt_id, new_week_start_date FROM plan_adaptation_attempts WHERE user_id = ?", u))
            jdbc.update("UPDATE plan_adaptation_attempts SET created_at = ? WHERE attempt_id = ?",
                    en(((java.sql.Date) a.get("new_week_start_date")).toLocalDate(), 3, 0), a.get("attempt_id"));
        jdbc.update("UPDATE safe_regeneration_attempts SET created_at = (SELECT a.created_at FROM plan_adaptation_attempts a"
                + " WHERE a.attempt_id = safe_regeneration_attempts.adaptation_attempt_id) WHERE user_id = ?"
                + " AND adaptation_attempt_id IS NOT NULL", u);
        for (var i : jdbc.queryForList("SELECT ui.intervention_id, p.week_start_date FROM user_interventions ui"
                + " JOIN weekly_training_plans p ON p.plan_id = ui.resulting_plan_id WHERE ui.user_id = ?", u)) {
            Timestamp t = en(((java.sql.Date) i.get("week_start_date")).toLocalDate(), 3, 0);
            jdbc.update("UPDATE user_interventions SET created_at = ?, applied_at = CASE WHEN applied_at IS NULL THEN NULL"
                    + " ELSE ?::timestamptz END WHERE intervention_id = ?", t, t, i.get("intervention_id"));
        }

        // Exclusiones por dolor: cuando se registro ese ejercicio.
        for (var x : jdbc.queryForList("SELECT exclusion_id, source_planned_exercise_id FROM user_safety_exclusions"
                + " WHERE user_id = ?", u)) {
            Object pe = x.get("source_planned_exercise_id");
            Timestamp t = pe == null ? null : registroDolor.get(((Number) pe).longValue());
            if (t == null) continue;
            jdbc.update("UPDATE user_safety_exclusions SET recorded_at = ?, identified_at = CASE WHEN identified_at IS NULL"
                    + " THEN NULL ELSE ?::timestamptz END WHERE exclusion_id = ?", t, t, x.get("exclusion_id"));
        }

        // Perfil: el mismo domingo del alta.
        Timestamp alta = en(w3.minusDays(1), 19, 0);
        jdbc.update("UPDATE user_profiles SET created_at = ?, updated_at = ?, suspended_support_confirmed_at ="
                + " CASE WHEN suspended_support_confirmed_at IS NULL THEN NULL ELSE ?::timestamptz END WHERE user_id = ?", alta, alta, alta, u);
    }
}
