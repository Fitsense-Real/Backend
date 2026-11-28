package main.web.services.fitsense.analytics.infrastructure.persistence.jdbc;

import main.web.services.fitsense.analytics.domain.model.aggregates.WeeklyUserMetrics;
import main.web.services.fitsense.shared.infrastructure.json.JsonSupport;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * C4 (P1-09) · Registra cada RECALCULO de una medicion semanal que ya existia (V67, solo insercion), en la misma
 * transaccion que la sobrescribe: si el recalculo no se guarda, tampoco su registro, y al reves.
 * <p>
 * No interviene en el calculo: recibe los valores de antes y de despues que ya produjo la formula de siempre.
 * {@code calculated_at} de la medicion sigue siendo la fecha del primer calculo.
 */
@Component
public class HistorialDeRecalculos {

    /** El cierre semanal se volvio a ejecutar para la misma semana medida (relanzado, recuperado por B3 o V66). */
    public static final String CIERRE_REPETIDO = "CIERRE_REPETIDO";
    public static final String PROCESO_CIERRE_SEMANAL = "PROCESO:CIERRE_SEMANAL";
    /** La medicion sale de las sesiones registradas y del plan de esa semana. */
    public static final String SESIONES_REGISTRADAS = "SESIONES_REGISTRADAS";

    private final JdbcTemplate jdbc;
    private final JsonSupport json;

    public HistorialDeRecalculos(JdbcTemplate jdbc, JsonSupport json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(WeeklyUserMetrics medicion, Map<String, Object> antes, String versionAnterior,
                          String motivo, String actor) {
        var previo = normalizar(antes);
        var recalculado = normalizar(medicion.valoresMedidos());
        List<String> cambiados = WeeklyUserMetrics.camposCambiados(previo, recalculado);
        jdbc.update("""
                        INSERT INTO weekly_metric_recalculations (weekly_metric_id, user_id, week_start_date,
                            first_calculated_at, reason, actor, data_source, previous_calculation_version,
                            calculation_version, previous_values, recalculated_values, changed_fields)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?)
                        """, medicion.getId(), medicion.getUserId(), medicion.getWeekStartDate(),
                medicion.getCalculatedAt(), motivo, actor, SESIONES_REGISTRADAS, versionAnterior,
                medicion.getCalculationVersion(), json.write(previo), json.write(recalculado),
                cambiados.toArray(String[]::new));
    }

    /**
     * risk_factors es JSON: leido de la base viene normalizado por jsonb (otro orden y espaciado) y recien calculado,
     * no. Se compara y se guarda como estructura, no como texto: el mismo contenido no es un cambio.
     */
    private Map<String, Object> normalizar(Map<String, Object> valores) {
        var r = new java.util.LinkedHashMap<>(valores);
        if (r.get("risk_factors") instanceof String texto && !texto.isBlank())
            r.put("risk_factors", json.read(texto, Object.class));
        return r;
    }
}
