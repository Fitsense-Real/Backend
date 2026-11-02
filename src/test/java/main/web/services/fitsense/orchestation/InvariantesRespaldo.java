package main.web.services.fitsense.orchestation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import main.web.services.fitsense.planning.domain.model.valueobjects.MandatorySlot;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.*;

/**
 * Invariantes de una semana CONSERVADA (continuidad o RESPALDO) frente a la semana medida de la que deriva, segun
 * las politicas vigentes (D1 de la orden de cierre del respaldo, 02/11/2026). Solo pruebas.
 * <p>
 * Una semana conservada es la semana medida, con las fechas +7, SALVO las correcciones de seguridad que el codigo
 * autoriza y deja trazadas en V32:
 * <ul>
 *   <li>la sustitucion SUST-SEG-1 de un ejercicio heredado que esta excluido por dolor (P-02, memoria ACTIVA): el
 *       que dolio esta misma semana (P-03 nivel 1, RESPALDO con dolor) o uno excluido antes;</li>
 *   <li>la carga sugerida que V11 ya no admite, a null (estas comprobaciones no comparan cargas).</li>
 * </ul>
 * Nada mas: ni variedad, ni semilla, ni otra sesion, ni otros minutos. Cada sustitucion: mismo tipo de prescripcion,
 * misma clase de slot (4C), en la misma posicion, sin subir series, repeticiones ni segundos, registrada en
 * {@code conservation_report.sustituciones} con la regla SUST-SEG-1; la sesion no sube su trabajo total; con
 * RESPALDO por dolor, la nota {@code REGENERACION_SEGURA} esta en V32.
 */
final class InvariantesRespaldo {

    private static final ObjectMapper JSON = new ObjectMapper();

    private InvariantesRespaldo() {}

    record Ej(long id, String tipo, Short series, Short reps, Integer segundos, Short descanso) {
        String dosis() {
            return "DURATION".equals(tipo) ? series + "x" + segundos + "s/" + descanso : series + "x" + reps + "/" + descanso;
        }
    }

    /**
     * Los incumplimientos de una semana conservada ({@code planDespues}) respecto de la medida ({@code planAntes}).
     * Vacio si se cumplen todas las invariantes.
     *
     * @param ev la fila V32 del ciclo (decision, status, generation_source, targeted_interventions,
     *           conservation_report)
     */
    static List<String> semanaConservada(JdbcTemplate jdbc, long planAntes, long planDespues, Map<String, Object> ev) {
        var problemas = new ArrayList<String>();
        var antes = sesiones(jdbc, planAntes);
        var despues = sesiones(jdbc, planDespues);
        var estructuraAntes = estructura(jdbc, planAntes, 7);
        var estructuraDespues = estructura(jdbc, planDespues, 0);
        if (antes.size() != despues.size()) {
            problemas.add("cambia el numero de sesiones " + antes.size() + " -> " + despues.size());
            return problemas;
        }
        long usuario = jdbc.queryForObject("SELECT user_id FROM weekly_training_plans WHERE plan_id = ?", Long.class,
                planDespues);
        var excluidos = new HashSet<>(jdbc.queryForList("SELECT exercise_id FROM user_safety_exclusions WHERE user_id = ?"
                + " AND status = 'ACTIVA'", Long.class, usuario));
        var informe = leer(String.valueOf(ev.get("conservation_report")));
        var sustituciones = new ArrayList<Map<String, String>>();
        for (var s : informe.path("sustituciones")) {
            var m = new HashMap<String, String>();
            s.fields().forEachRemaining(f -> m.put(f.getKey(), f.getValue().asText()));
            sustituciones.add(m);
        }
        boolean nota = String.valueOf(ev.get("conservation_report")).contains("REGENERACION_SEGURA");
        boolean hayCambios = false;

        for (int i = 0; i < antes.size(); i++) {
            if (!estructuraAntes.get(i).equals(estructuraDespues.get(i)))
                problemas.add("sesion %d: cambian fecha, enfoque o minutos (%s -> %s)"
                        .formatted(i, estructuraAntes.get(i), estructuraDespues.get(i)));
            var a = antes.get(i);
            var d = despues.get(i);
            if (a.size() != d.size()) {
                problemas.add("sesion %d: cambia el numero de ejercicios %d -> %d".formatted(i, a.size(), d.size()));
                continue;
            }
            long repsA = 0, repsD = 0, segA = 0, segD = 0;
            for (int p = 0; p < a.size(); p++) {
                var x = a.get(p);
                var y = d.get(p);
                repsA += (long) series(x) * nz(x.reps());
                repsD += (long) series(y) * nz(y.reps());
                segA += (long) series(x) * nz(x.segundos());
                segD += (long) series(y) * nz(y.segundos());
                if (x.equals(y)) continue;
                hayCambios = true;
                String donde = "sesion %d posicion %d (%d %s -> %d %s)".formatted(i, p, x.id(), x.dosis(), y.id(), y.dosis());
                if (x.id() == y.id()) {
                    problemas.add(donde + ": cambia la dosis sin sustitucion autorizada");
                    continue;
                }
                if (!excluidos.contains(x.id()))
                    problemas.add(donde + ": se sustituye un ejercicio que no esta excluido por seguridad");
                boolean trazada = sustituciones.stream().anyMatch(s -> String.valueOf(x.id()).equals(s.get("quitado"))
                        && String.valueOf(y.id()).equals(s.get("puesto")) && "SUST-SEG-1".equals(s.get("regla")));
                if (!trazada) problemas.add(donde + ": la sustitucion no esta en V32 con la regla SUST-SEG-1");
                if (!x.tipo().equals(y.tipo())) problemas.add(donde + ": cambia el tipo de prescripcion");
                if (!slot(jdbc, x.id()).equals(slot(jdbc, y.id()))) problemas.add(donde + ": cambia la clase de slot");
                if (mayor(y.series(), x.series()) || mayor(y.reps(), x.reps()) || mayor(y.segundos(), x.segundos()))
                    problemas.add(donde + ": el sustituto sube la dosis");
            }
            if (repsD > repsA || segD > segA)
                problemas.add("sesion %d: sube el trabajo (repeticiones %d -> %d, segundos %d -> %d)"
                        .formatted(i, repsA, repsD, segA, segD));
        }
        if (hayCambios && !"CONSERVED".equals(String.valueOf(ev.get("generation_source"))))
            problemas.add("una semana conservada con sustituciones debe tener origen CONSERVED, no " + ev.get("generation_source"));
        if (hayCambios && "RESPALDO".equals(String.valueOf(ev.get("status")))
                && String.valueOf(ev.get("targeted_interventions")).contains("SEGURIDAD_ANTE_DOLOR") && !nota)
            problemas.add("RESPALDO con dolor y sustitucion sin la nota REGENERACION_SEGURA en V32");
        return problemas;
    }

    /**
     * Caso C: no hay semana segura. Se exige que no se publique nada para la semana nueva, que V32 diga NO_FACTIBLE
     * con un motivo y que las exclusiones por dolor sigan activas (no se levantan restricciones para publicar).
     */
    static List<String> sinPublicar(JdbcTemplate jdbc, long usuario, LocalDate semanaNueva, Map<String, Object> ev,
                                    Collection<Long> dolieron) {
        var problemas = new ArrayList<String>();
        if (!"NO_FACTIBLE".equals(String.valueOf(ev.get("status"))))
            problemas.add("sin semana publicada, el estado V32 debe ser NO_FACTIBLE y es " + ev.get("status"));
        if (ev.get("rejection_reason") == null || String.valueOf(ev.get("rejection_reason")).isBlank())
            problemas.add("NO_FACTIBLE sin motivo en V32");
        int activos = jdbc.queryForObject("SELECT count(*) FROM weekly_training_plans WHERE user_id = ?"
                + " AND week_start_date = ? AND status = 'ACTIVE'", Integer.class, usuario, semanaNueva);
        if (activos != 0) problemas.add("hay " + activos + " plan(es) activo(s) para la semana " + semanaNueva);
        var activas = new HashSet<>(jdbc.queryForList("SELECT exercise_id FROM user_safety_exclusions WHERE user_id = ?"
                + " AND status = 'ACTIVA'", Long.class, usuario));
        for (var e : dolieron)
            if (!activas.contains(e)) problemas.add("la exclusion por dolor del ejercicio " + e + " no esta activa");
        return problemas;
    }

    static List<List<Ej>> sesiones(JdbcTemplate jdbc, long planId) {
        return jdbc.queryForList("SELECT planned_workout_id FROM planned_workouts WHERE plan_id = ? ORDER BY scheduled_date",
                Long.class, planId).stream().map(id -> jdbc.query("SELECT exercise_id, prescription_type, planned_sets,"
                        + " planned_reps, planned_duration_seconds, rest_seconds FROM planned_workout_exercises"
                        + " WHERE planned_workout_id = ? ORDER BY exercise_order",
                (rs, k) -> new Ej(rs.getLong(1), rs.getString(2), rs.getObject(3, Short.class), rs.getObject(4, Short.class),
                        rs.getObject(5, Integer.class), rs.getObject(6, Short.class)), id)).toList();
    }

    private static List<String> estructura(JdbcTemplate jdbc, long planId, int dias) {
        return jdbc.queryForList("SELECT (scheduled_date + ?) || '|' || focus_code || '|' || expected_duration_minutes"
                + " FROM planned_workouts WHERE plan_id = ? ORDER BY scheduled_date", String.class, dias, planId);
    }

    private static String slot(JdbcTemplate jdbc, long exerciseId) {
        var patron = jdbc.queryForObject("SELECT movement_pattern FROM exercises WHERE exercise_id = ?", String.class, exerciseId);
        return MandatorySlot.ofMovementPattern(patron).map(Enum::name).orElse("relleno");
    }

    private static JsonNode leer(String texto) {
        try {
            return JSON.readTree(texto == null || "null".equals(texto) ? "{}" : texto);
        } catch (Exception e) {
            throw new IllegalStateException("conservation_report ilegible: " + texto, e);
        }
    }

    private static int series(Ej e) { return e.series() == null ? 1 : e.series(); }

    private static long nz(Number n) { return n == null ? 0 : n.longValue(); }

    private static boolean mayor(Number nuevo, Number antes) {
        return nuevo != null && antes != null && nuevo.longValue() > antes.longValue();
    }
}
