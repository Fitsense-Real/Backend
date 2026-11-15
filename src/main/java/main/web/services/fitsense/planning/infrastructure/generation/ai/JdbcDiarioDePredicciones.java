package main.web.services.fitsense.planning.infrastructure.generation.ai;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * V59: el libro de predicciones en PostgreSQL. Cada operacion es una sentencia con autocommit: nunca hay una
 * transaccion abierta mientras se espera a Replicate, y lo anotado sobrevive a un reinicio en cuanto se escribe.
 */
@Component
public class JdbcDiarioDePredicciones implements DiarioDePredicciones {

    private static final int MAX_ERROR = 500;

    private final JdbcTemplate jdbc;

    public JdbcDiarioDePredicciones(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Entrada> FILA = (rs, i) -> new Entrada(
            rs.getLong("prediction_row_id"),
            rs.getObject("operation_id", UUID.class),
            rs.getLong("user_id"),
            rs.getInt("attempt"),
            rs.getString("request_hash"),
            Estado.valueOf(rs.getString("status")),
            rs.getString("replicate_prediction_id"),
            rs.getString("output"),
            rs.getString("error"),
            (Long) rs.getObject("reused_from_row_id", Long.class));

    private static final String COLUMNAS = "prediction_row_id, operation_id, user_id, attempt, request_hash, status,"
            + " replicate_prediction_id, output, error, reused_from_row_id";

    @Override
    public Optional<Entrada> deIntento(UUID operationId, int attempt) {
        return jdbc.query("SELECT " + COLUMNAS + " FROM plan_generation_predictions"
                        + " WHERE operation_id = ? AND attempt = ? AND status <> 'DESCARTADA'", FILA, operationId, attempt)
                .stream().findFirst();
    }

    @Override
    public Optional<Entrada> reutilizable(long userId, String requestHash, UUID excepto, Instant desde) {
        // La mas reciente: una terminada con exito antes que una en curso de la misma hora.
        return jdbc.query("SELECT " + COLUMNAS + " FROM plan_generation_predictions"
                        + " WHERE user_id = ? AND request_hash = ? AND operation_id <> ?"
                        + " AND status IN ('SUCCEEDED', 'ENVIADA') AND requested_at >= ?"
                        + " ORDER BY (status = 'SUCCEEDED') DESC, requested_at DESC LIMIT 1",
                FILA, userId, requestHash, excepto, Timestamp.from(desde)).stream().findFirst();
    }

    @Override
    public Entrada adoptar(Entrada origen, UUID operationId, int attempt, Instant ahora) {
        var t = Timestamp.from(ahora);
        Long id = jdbc.queryForObject("INSERT INTO plan_generation_predictions (operation_id, user_id, attempt,"
                        + " request_hash, status, replicate_prediction_id, reused_from_row_id, output, error,"
                        + " requested_at, sent_at, finished_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                        + " RETURNING prediction_row_id", Long.class,
                operationId, origen.userId(), attempt, origen.requestHash(), origen.estado().name(),
                origen.predictionId(), origen.id(), origen.output(), origen.error(), t, t,
                origen.estado().terminal() ? t : null);
        return new Entrada(id, operationId, origen.userId(), attempt, origen.requestHash(), origen.estado(),
                origen.predictionId(), origen.output(), origen.error(), origen.id());
    }

    @Override
    public long solicitar(UUID operationId, long userId, int attempt, String requestHash, Instant ahora) {
        // Si habia una fila vigente para este intento con OTRA peticion, queda como historial.
        jdbc.update("UPDATE plan_generation_predictions SET status = 'DESCARTADA', finished_at = coalesce(finished_at, ?),"
                        + " error = coalesce(error, 'la peticion cambio') WHERE operation_id = ? AND attempt = ?"
                        + " AND status <> 'DESCARTADA'", Timestamp.from(ahora), operationId, attempt);
        return jdbc.queryForObject("INSERT INTO plan_generation_predictions (operation_id, user_id, attempt,"
                        + " request_hash, status, requested_at) VALUES (?, ?, ?, ?, 'SOLICITANDO', ?)"
                        + " RETURNING prediction_row_id", Long.class,
                operationId, userId, attempt, requestHash, Timestamp.from(ahora));
    }

    @Override
    public void enviada(long fila, String predictionId, Instant ahora) {
        // Una NO_CONFIRMADA cuyo id llega tarde (el proceso anterior seguia vivo) guarda el id como evidencia del
        // posible duplicado, pero no cambia de estado: no se vuelve reutilizable.
        jdbc.update("UPDATE plan_generation_predictions SET replicate_prediction_id = ?, sent_at = ?,"
                + " status = CASE WHEN status = 'SOLICITANDO' THEN 'ENVIADA' ELSE status END"
                + " WHERE prediction_row_id = ? AND status IN ('SOLICITANDO', 'NO_CONFIRMADA')"
                + " AND replicate_prediction_id IS NULL", predictionId, Timestamp.from(ahora), fila);
    }

    @Override
    public void consultada(long fila, int consultas, Instant ahora) {
        jdbc.update("UPDATE plan_generation_predictions SET last_polled_at = ?, polls = polls + ?"
                + " WHERE prediction_row_id = ?", Timestamp.from(ahora), Math.max(0, consultas), fila);
    }

    @Override
    public void terminada(long fila, Estado estado, String output, String error, Instant ahora) {
        var mensaje = error == null ? null : error.substring(0, Math.min(MAX_ERROR, error.length()));
        // Solo una prediccion abierta cambia de estado. Una ya cerrada (por ejemplo, NO_CONFIRMADA por otro worker)
        // conserva su estado y solo anota la salida tardia como evidencia.
        int n = jdbc.update("UPDATE plan_generation_predictions SET status = ?, output = coalesce(?, output), error = ?,"
                        + " finished_at = ? WHERE prediction_row_id = ? AND status IN ('SOLICITANDO', 'ENVIADA')",
                estado.name(), output, mensaje, Timestamp.from(ahora), fila);
        if (n == 0)
            jdbc.update("UPDATE plan_generation_predictions SET output = coalesce(output, ?),"
                    + " error = coalesce(error, ?), finished_at = coalesce(finished_at, ?)"
                    + " WHERE prediction_row_id = ? AND status <> 'SUCCEEDED'", output, mensaje, Timestamp.from(ahora), fila);
    }

    @Override
    public Recuento recuento(UUID operationId) {
        return jdbc.queryForObject("""
                SELECT count(*) FILTER (WHERE reused_from_row_id IS NULL) AS iniciadas,
                       count(*) FILTER (WHERE reused_from_row_id IS NOT NULL) AS reutilizadas,
                       count(*) FILTER (WHERE status = 'SUCCEEDED') AS completadas,
                       count(*) FILTER (WHERE status IN ('FAILED', 'ERROR_ENVIO')) AS fallidas,
                       count(*) FILTER (WHERE status = 'CANCELED') AS canceladas,
                       count(*) FILTER (WHERE status = 'TIMEOUT') AS vencidas,
                       count(*) FILTER (WHERE status = 'NO_CONFIRMADA') AS no_confirmadas,
                       count(*) FILTER (WHERE status = 'DESCARTADA') AS descartadas
                  FROM plan_generation_predictions WHERE operation_id = ?
                """, (rs, i) -> new Recuento(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getInt(5),
                rs.getInt(6), rs.getInt(7), rs.getInt(8)), operationId);
    }
}
