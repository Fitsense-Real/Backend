package main.web.services.fitsense.administration.infrastructure.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registro de las operaciones administrativas sensibles, en el logger
 * {@code fitsense.audit} (una linea por evento, clave=valor). Solo identificadores
 * internos: nunca correos, nombres, tokens ni parametros de consulta.
 */
public final class AdminAudit {

    private static final Logger AUDIT = LoggerFactory.getLogger("fitsense.audit");

    private AdminAudit() {}

    public static void request(Long adminId, String method, String path, int status, long millis) {
        AUDIT.info("ADMIN_REQUEST adminId={} method={} path={} status={} ms={}",
                adminId, method, path, status, millis);
    }

    public static void personalDataViewed(Long adminId, Long participantId) {
        AUDIT.info("ADMIN_PERSONAL_DATA_VIEWED adminId={} participantId={}", adminId, participantId);
    }

    /** B5.3: cada exportacion (que, cuantas filas). El contenido no se registra. */
    public static void exported(Long adminId, String que, int filas) {
        AUDIT.info("ADMIN_EXPORT adminId={} export={} filas={}", adminId, que, filas);
    }

    public static void denied(Long userId, String method, String path) {
        AUDIT.warn("ADMIN_ACCESS_DENIED userId={} method={} path={}", userId, method, path);
    }
}
