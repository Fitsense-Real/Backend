package main.web.services.fitsense.iam.application.internal.deletion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Eliminacion completa de una cuenta (alternativa A de POLITICA_ELIMINACION_PROPUESTA.md): se borran todas las filas
 * de la persona en todas las tablas. Es el unico procedimiento; lo usan la app ({@code DELETE /api/v1/users/me}) y,
 * cuando exista, la solicitud web. No hay anonimizacion ni conservacion de datos bajo P-xxx: esa alternativa espera la
 * decision del estudio y no se activa aqui.
 * <p>
 * Dos fases, cada una en su propia transaccion:
 * <ol>
 *   <li><b>Registro</b> (corta, bloquea la fila de {@code users}): la cuenta pasa a {@code DELETED}, lo que corta en el
 *   acto el inicio de sesion y todo JWT emitido (el filtro recarga al usuario en cada peticion y exige ACTIVE); las
 *   generaciones en curso se cierran como FAILED/CUENTA_ELIMINADA, asi que el worker no envia mas predicciones ni
 *   publica (fencing de V59); y queda una fila PENDIENTE en {@code account_deletions}. Solo despues de esta fase se
 *   responde algo distinto de un error.</li>
 *   <li><b>Borrado</b> (bloquea la fila de {@code users} y la de la eliminacion): borra en el orden de las FK y por
 *   ultimo la fila de {@code users}. Si algo falla (un bloqueo, una escritura concurrente que llego tarde), la
 *   transaccion entera se deshace, la eliminacion sigue PENDIENTE y la reanuda {@link #reanudarPendientes()}.</li>
 * </ol>
 * Nunca se registra el correo, el nombre ni el codigo de participante: los logs llevan solo el identificador de la
 * eliminacion y el recuento de filas.
 */
@Service
public class EliminacionDeCuentaService {

    private static final Logger log = LoggerFactory.getLogger(EliminacionDeCuentaService.class);

    /**
     * Tablas con datos de la persona, en el orden en que se borran (de las que referencian a las referenciadas). Las
     * que dependen de estas con ON DELETE CASCADE (user_profiles, planned_workouts, planned_workout_exercises,
     * workout_session_exercises) caen con ellas. {@code EliminacionDeCuentaIT} comprueba contra el esquema real que no
     * queda ninguna tabla sin cubrir: una tabla nueva con user_id o con una FK hacia estas hace fallar la prueba hasta
     * que se anada aqui.
     */
    public static final List<String> TABLAS_EN_ORDEN = List.of(
            "user_safety_exclusions",
            "safe_regeneration_attempts",
            "user_interventions",
            "plan_adaptation_attempts",
            "plan_generation_predictions",
            "plan_generation_operations",
            "workout_sessions",
            "weekly_user_metrics",
            "weekly_training_plans",
            "password_reset_tokens",
            "account_deletion_tokens",
            "user_consents",
            // V64 (notificaciones push): entregas, eventos, instalaciones y preferencias.
            "notification_deliveries",
            "notification_events",
            "push_devices",
            "notification_preferences",
            "users");

    /** Tablas con user_id que NO son datos de la persona y no se borran (el propio registro de eliminaciones). */
    public static final List<String> TABLAS_EXCLUIDAS = List.of("account_deletions");

    public enum Origen { APP, WEB }

    public enum Estado { PENDIENTE, COMPLETADA }

    /** Lo que se puede decir de una eliminacion sin exponer datos de la persona. */
    public record Eliminacion(UUID deletionId, Estado estado, Instant solicitadaEn, Instant completadaEn,
                              boolean yaRegistrada) {
        public boolean completada() {
            return estado == Estado.COMPLETADA;
        }
    }

    /** La cuenta no existe (o ya se elimino por completo): no hay nada que registrar. */
    public static class CuentaInexistenteException extends RuntimeException {
        public CuentaInexistenteException() {
            super("No existe una cuenta que eliminar.");
        }
    }

    /** La fase de registro no pudo completarse: no se ha cambiado nada y la peticion se puede repetir. */
    public static class EliminacionNoRegistradaException extends RuntimeException {
        public EliminacionNoRegistradaException(Throwable causa) {
            super("No se pudo registrar la eliminacion. No se ha cambiado nada; vuelve a intentarlo.", causa);
        }
    }

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final Clock clock;

    public EliminacionDeCuentaService(JdbcTemplate jdbc, PlatformTransactionManager transacciones,
                                      Optional<Clock> clock) {
        this.jdbc = jdbc;
        this.tx = new TransactionTemplate(transacciones);
        this.clock = clock.orElse(Clock.systemUTC());
    }

    /**
     * Registra la eliminacion y trata de completarla en el acto. Idempotente: si ya hay una PENDIENTE para la cuenta,
     * devuelve esa (con {@code yaRegistrada}) y vuelve a intentar el borrado.
     *
     * @throws CuentaInexistenteException       si no hay fila en users
     * @throws EliminacionNoRegistradaException si la fase de registro falla (nada cambio)
     */
    public Eliminacion eliminar(long userId, Origen origen) {
        Eliminacion registrada;
        try {
            registrada = tx.execute(s -> registrar(userId, origen));
        } catch (CuentaInexistenteException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("ELIMINACION_CUENTA_NO_REGISTRADA causa={}", e.getClass().getSimpleName());
            throw new EliminacionNoRegistradaException(e);
        }
        var resultado = borrar(registrada.deletionId());
        return new Eliminacion(resultado.deletionId(), resultado.estado(), resultado.solicitadaEn(),
                resultado.completadaEn(), registrada.yaRegistrada());
    }

    public Optional<Eliminacion> consultar(UUID deletionId) {
        return jdbc.query("SELECT deletion_id, status, requested_at, completed_at FROM account_deletions"
                        + " WHERE deletion_id = ?",
                (rs, i) -> new Eliminacion(rs.getObject(1, UUID.class), Estado.valueOf(rs.getString(2)),
                        rs.getTimestamp(3).toInstant(),
                        rs.getTimestamp(4) == null ? null : rs.getTimestamp(4).toInstant(), true),
                deletionId).stream().findFirst();
    }

    /** Fase 1. Dentro de una transaccion. */
    private Eliminacion registrar(long userId, Origen origen) {
        jdbc.execute("SET LOCAL lock_timeout = '5s'");
        var fila = jdbc.queryForList("SELECT status FROM users WHERE user_id = ? FOR UPDATE", userId);
        if (fila.isEmpty()) throw new CuentaInexistenteException();

        var pendiente = jdbc.query("SELECT deletion_id, requested_at FROM account_deletions"
                        + " WHERE user_id = ? AND status = 'PENDIENTE'",
                (rs, i) -> new Eliminacion(rs.getObject(1, UUID.class), Estado.PENDIENTE,
                        rs.getTimestamp(2).toInstant(), null, true), userId);
        if (!pendiente.isEmpty()) return pendiente.get(0);

        var ahora = Timestamp.from(clock.instant());
        jdbc.update("UPDATE users SET status = 'DELETED', updated_at = ? WHERE user_id = ?", ahora, userId);
        int canceladas = jdbc.update("""
                UPDATE plan_generation_operations
                   SET status = 'FAILED', reason_code = 'CUENTA_ELIMINADA',
                       message = 'La cuenta se elimino; la generacion no se completa.', retryable = FALSE,
                       finished_at = ?, updated_at = ?, lease_owner = NULL, lease_expires_at = NULL
                 WHERE user_id = ? AND status IN ('PENDING', 'PROCESSING')
                """, ahora, ahora, userId);
        var id = UUID.randomUUID();
        jdbc.update("INSERT INTO account_deletions (deletion_id, user_id, origin, status, requested_at)"
                + " VALUES (?, ?, ?, 'PENDIENTE', ?)", id, userId, origen.name(), ahora);
        log.info("ELIMINACION_CUENTA_REGISTRADA eliminacion={} origen={} generaciones_canceladas={}",
                id, origen, canceladas);
        return new Eliminacion(id, Estado.PENDIENTE, ahora.toInstant(), null, false);
    }

    /**
     * Fase 2. Si otra ejecucion ya esta borrando esta misma eliminacion (SKIP LOCKED), no espera: devuelve el estado
     * actual. Un fallo deja la eliminacion PENDIENTE con el tipo de error (sin mensaje: podria contener datos).
     */
    Eliminacion borrar(UUID deletionId) {
        try {
            var hecho = tx.execute(s -> borrarEnTransaccion(deletionId));
            if (hecho != null) return hecho;
        } catch (RuntimeException e) {
            anotarFallo(deletionId, e);
        }
        return consultar(deletionId).orElseThrow();
    }

    private Eliminacion borrarEnTransaccion(UUID deletionId) {
        jdbc.execute("SET LOCAL lock_timeout = '5s'");
        var filas = jdbc.queryForList("SELECT user_id, status, requested_at FROM account_deletions"
                + " WHERE deletion_id = ? FOR UPDATE SKIP LOCKED", deletionId);
        if (filas.isEmpty() || !"PENDIENTE".equals(filas.get(0).get("status"))) return null;
        long userId = ((Number) filas.get(0).get("user_id")).longValue();

        // Con la fila de users bloqueada, ninguna escritura nueva que la referencie (FK) puede confirmarse hasta que
        // termine esta transaccion: lo que se inserte despues falla por la FK en lugar de quedar huerfano.
        jdbc.queryForList("SELECT 1 FROM users WHERE user_id = ? FOR UPDATE", userId);
        var borradas = new LinkedHashMap<String, Integer>();
        for (var tabla : TABLAS_EN_ORDEN) {
            int n = jdbc.update("DELETE FROM " + tabla + " WHERE user_id = ?", userId);
            borradas.put(tabla, n);
        }
        var ahora = Timestamp.from(clock.instant());
        jdbc.update("UPDATE account_deletions SET status = 'COMPLETADA', completed_at = ?, user_id = NULL,"
                        + " attempts = attempts + 1, last_error = NULL, rows_deleted = CAST(? AS jsonb)"
                        + " WHERE deletion_id = ?",
                ahora, json(borradas), deletionId);
        log.info("ELIMINACION_CUENTA_COMPLETADA eliminacion={} filas={}", deletionId, borradas);
        return new Eliminacion(deletionId, Estado.COMPLETADA,
                ((Timestamp) filas.get(0).get("requested_at")).toInstant(), ahora.toInstant(), false);
    }

    private void anotarFallo(UUID deletionId, RuntimeException e) {
        String causa = e.getClass().getSimpleName();
        if (e instanceof DataAccessException dae && dae.getMostSpecificCause() != null)
            causa = causa + "/" + dae.getMostSpecificCause().getClass().getSimpleName();
        log.warn("ELIMINACION_CUENTA_PENDIENTE eliminacion={} causa={} (se reintentara)", deletionId, causa);
        try {
            jdbc.update("UPDATE account_deletions SET attempts = attempts + 1, last_error = ?"
                    + " WHERE deletion_id = ? AND status = 'PENDIENTE'", causa, deletionId);
        } catch (RuntimeException ignorada) {
            log.warn("ELIMINACION_CUENTA_SIN_ANOTAR eliminacion={}", deletionId);
        }
    }

    /** Reanuda las eliminaciones que quedaron PENDIENTE (fallo, reinicio del proceso). */
    @Scheduled(initialDelayString = "${fitsense.account-deletion.retry-initial-delay-ms:60000}",
            fixedDelayString = "${fitsense.account-deletion.retry-delay-ms:300000}")
    public void reanudarPendientes() {
        reanudarPendientes(Duration.ofMinutes(1));
    }

    /** @param antiguedad solo las registradas hace al menos esto (no compite con la peticion que la acaba de crear) */
    public int reanudarPendientes(Duration antiguedad) {
        var limite = Timestamp.from(clock.instant().minus(antiguedad));
        var ids = jdbc.queryForList("SELECT deletion_id FROM account_deletions WHERE status = 'PENDIENTE'"
                + " AND requested_at <= ? ORDER BY requested_at LIMIT 50", UUID.class, limite);
        int completadas = 0;
        for (var id : ids)
            if (borrar(id).completada()) completadas++;
        if (!ids.isEmpty())
            log.info("ELIMINACION_CUENTA_REANUDACION pendientes={} completadas={}", ids.size(), completadas);
        return completadas;
    }

    private static String json(Map<String, Integer> m) {
        return m.entrySet().stream().map(e -> "\"%s\":%d".formatted(e.getKey(), e.getValue()))
                .collect(Collectors.joining(",", "{", "}"));
    }
}
