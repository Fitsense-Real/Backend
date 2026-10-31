package main.web.services.fitsense.orchestration.infrastructure.locking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.function.Supplier;

/**
 * EXCLUSION DEL CICLO SEMANAL POR PARTICIPANTE, ENTRE HILOS E INSTANCIAS.
 * <p>
 * Un bloqueo consultivo de SESION de PostgreSQL ({@code pg_try_advisory_lock})
 * tomado en una CONEXION PROPIA Y DIRECTA, abierta solo para esto y mantenida
 * durante todo el ciclo. No sirve un bloqueo de transaccion
 * ({@code pg_advisory_xact_lock}): {@code WeeklyCycleService.runFor} va sin
 * {@code @Transactional} a proposito (cada paso abre su transaccion corta y la
 * IA puede tardar ~90 s), asi que se soltaria al acabar el primer paso. Tampoco
 * sirve una conexion del pool ni el endpoint {@code -pooler} de Neon: PgBouncer
 * en modo transaccion no conserva la sesion entre transacciones.
 * <p>
 * <ul>
 *   <li>Se toma ANTES de cerrar, medir, decidir o publicar la semana.</li>
 *   <li>Si ya lo tiene otro (otro hilo u otra instancia), NO se ejecuta nada:
 *       {@link Estado#EN_CURSO}. Es seguro reintentar despues: el ciclo
 *       repetido devuelve el plan ya publicado.</li>
 *   <li>Se suelta en {@code finally}, con {@code pg_advisory_unlock} en la MISMA
 *       conexion, y la conexion se cierra; si se hubiera perdido, el servidor
 *       ya libero el bloqueo al cerrar esa sesion.</li>
 *   <li>Sin poder abrir la conexion del bloqueo no se ejecuta el ciclo
 *       ({@link Estado#SIN_BLOQUEO}): ejecutar sin exclusion es justo lo que
 *       se quiere evitar. La tarea programada lo reintenta.</li>
 * </ul>
 * La clave identifica al participante (espacio propio del ciclo semanal), asi
 * que dos participantes distintos nunca se bloquean entre si.
 * <p>
 * LIMITE DECLARADO: si la conexion del bloqueo se cae A MITAD del ciclo, el
 * servidor suelta el bloqueo y otra ejecucion puede empezar mientras esta
 * termina. Se detecta al final ({@code isValid}) y queda en el log. Que aun asi
 * no se publique dos veces lo garantiza la publicacion, no este bloqueo: bloquea
 * la fila de la medicion y no publica si otro intento ya lo hizo
 * ({@code WeeklyTrainingPlanCommandServiceImpl.publishAdapted}). Lo que si puede
 * repetirse en ese caso es la llamada a la IA.
 */
@Component
public class CicloSemanalBloqueo {

    private static final Logger log = LoggerFactory.getLogger(CicloSemanalBloqueo.class);

    /** Espacio de claves del ciclo semanal: los 32 bits altos («FSC1»). */
    static final long ESPACIO = 0x46534331L << 32;

    public enum Estado { EJECUTADO, EN_CURSO, SIN_BLOQUEO }

    public record Resultado<T>(Estado estado, T valor) {}

    private final String url;
    private final String usuario;
    private final String clave;
    private final int segundosConexion;

    @org.springframework.beans.factory.annotation.Autowired
    public CicloSemanalBloqueo(JdbcConnectionDetails conexion,
                               @Value("${fitsense.cycle.lock.url:}") String urlBloqueo,
                               @Value("${fitsense.cycle.lock.connect-timeout-seconds:10}") int segundosConexion) {
        this(urlBloqueo == null || urlBloqueo.isBlank() ? directa(conexion.getJdbcUrl()) : urlBloqueo,
                conexion.getUsername(), conexion.getPassword(), segundosConexion);
    }

    public CicloSemanalBloqueo(String url, String usuario, String clave, int segundosConexion) {
        this.url = url;
        this.usuario = usuario;
        this.clave = clave;
        this.segundosConexion = segundosConexion;
        if (url != null && url.contains("-pooler"))
            log.warn("CICLO_BLOQUEO la URL del bloqueo usa el pooler de Neon ({}); un bloqueo de sesion no se"
                    + " conserva a traves de PgBouncer en modo transaccion.", url.replaceAll("//[^@/]*@", "//"));
    }

    /** El endpoint directo de Neon: el mismo host sin {@code -pooler}. Otras URL, tal cual. */
    static String directa(String jdbcUrl) {
        return jdbcUrl == null ? null : jdbcUrl.replace("-pooler.", ".");
    }

    static long clave(long userId) {
        return ESPACIO | (userId & 0xFFFFFFFFL);
    }

    /** Ejecuta {@code cuerpo} con el ciclo de {@code userId} bloqueado, o no lo ejecuta. */
    public <T> Resultado<T> ejecutar(long userId, Supplier<T> cuerpo) {
        Connection c;
        try {
            c = abrir();
        } catch (SQLException e) {
            log.error("CICLO_BLOQUEO usuario {}: no se pudo abrir la conexion del bloqueo; el ciclo no se ejecuta: {}",
                    userId, e.getMessage());
            return new Resultado<>(Estado.SIN_BLOQUEO, null);
        }
        long k = clave(userId);
        try (c) {
            boolean adquirido;
            try (var ps = c.prepareStatement("SELECT pg_try_advisory_lock(?)")) {
                ps.setLong(1, k);
                try (var rs = ps.executeQuery()) {
                    adquirido = rs.next() && rs.getBoolean(1);
                }
            }
            if (!adquirido) {
                log.info("CICLO_BLOQUEO usuario {}: otro ciclo ya esta en curso; no se ejecuta este.", userId);
                return new Resultado<>(Estado.EN_CURSO, null);
            }
            try {
                return new Resultado<>(Estado.EJECUTADO, cuerpo.get());
            } finally {
                liberar(c, userId, k);
            }
        } catch (SQLException e) {
            log.error("CICLO_BLOQUEO usuario {}: fallo de la conexion del bloqueo antes de ejecutar: {}",
                    userId, e.getMessage());
            return new Resultado<>(Estado.SIN_BLOQUEO, null);
        }
    }

    private Connection abrir() throws SQLException {
        var p = new Properties();
        if (usuario != null) p.setProperty("user", usuario);
        if (clave != null) p.setProperty("password", clave);
        p.setProperty("tcpKeepAlive", "true");
        p.setProperty("ApplicationName", "fitsense-ciclo-semanal-bloqueo");
        p.setProperty("connectTimeout", String.valueOf(segundosConexion));
        p.setProperty("loginTimeout", String.valueOf(segundosConexion));
        var c = DriverManager.getConnection(url, p);
        c.setAutoCommit(true);
        return c;
    }

    /** Suelta el bloqueo en la misma conexion. Nunca lanza: el cuerpo ya termino. */
    private static void liberar(Connection c, long userId, long k) {
        try {
            if (!c.isValid(5)) {
                log.warn("CICLO_BLOQUEO usuario {}: la conexion del bloqueo se perdio durante el ciclo; el servidor"
                        + " ya lo libero y la exclusion pudo no cubrir el ciclo entero.", userId);
                return;
            }
            try (var ps = c.prepareStatement("SELECT pg_advisory_unlock(?)")) {
                ps.setLong(1, k);
                try (var rs = ps.executeQuery()) {
                    if (!rs.next() || !rs.getBoolean(1))
                        log.warn("CICLO_BLOQUEO usuario {}: el bloqueo ya no estaba tomado al soltarlo.", userId);
                }
            }
        } catch (SQLException e) {
            log.warn("CICLO_BLOQUEO usuario {}: no se pudo soltar explicitamente ({}); se cierra la conexion y el"
                    + " servidor lo libera.", userId, e.getMessage());
        }
    }
}
