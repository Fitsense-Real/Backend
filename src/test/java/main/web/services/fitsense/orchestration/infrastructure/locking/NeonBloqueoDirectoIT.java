package main.web.services.fitsense.orchestration.infrastructure.locking;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VERIFICACION DE SOLO LECTURA DEL BLOQUEO DEL CICLO SEMANAL CONTRA NEON.
 * <p>
 * No escribe NADA: no levanta Spring (asi no corre Flyway), no inserta, no actualiza
 * y no crea tablas. Un bloqueo consultivo no toca datos. Solo con
 * {@code -Dfitsense.neon.verificar=true} y la variable {@code DB_PASSWORD}; la URL y el
 * usuario salen de {@code application.properties}. No imprime la contrasena ni la URL
 * completa.
 * <pre>
 * DB_PASSWORD=... mvn -o verify -Pintegration -Dtest=NoUnit -Dsurefire.failIfNoSpecifiedTests=false \
 *     -Dit.test=NeonBloqueoDirectoIT -Dfitsense.neon.verificar=true
 * </pre>
 * Comprueba:
 * <ol>
 *   <li>el endpoint del bloqueo es el directo (sin {@code -pooler}) y acepta conexion;</li>
 *   <li>el bloqueo se adquiere, se MANTIENE entre transacciones de la misma sesion y se
 *       libera;</li>
 *   <li>mientras esta tomado, una segunda ejecucion del mismo participante ve EN_CURSO;</li>
 *   <li>la conexion del bloqueo es una sesion estable (el mismo backend en sucesivas
 *       sentencias), no una conexion del pool transaccional.</li>
 * </ol>
 * La clave es de un participante que no existe (el ultimo identificador de 32 bits).
 */
@EnabledIfSystemProperty(named = "fitsense.neon.verificar", matches = "true")
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
@DisplayName("Neon · el bloqueo del ciclo usa el endpoint directo y una sesion estable (solo lectura)")
class NeonBloqueoDirectoIT {

    private static final long PARTICIPANTE_INEXISTENTE = 0xFFFFFFFFL;

    @Test
    void bloqueoContraNeon() throws Exception {
        var props = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/application.properties")) {
            props.load(in);
        }
        String url = props.getProperty("spring.datasource.url");
        String usuario = props.getProperty("spring.datasource.username");
        String clave = System.getenv("DB_PASSWORD");
        String directa = CicloSemanalBloqueo.directa(url);

        System.out.println("NEON host configurado: " + host(url));
        System.out.println("NEON host del bloqueo: " + host(directa));
        assertFalse(directa.contains("-pooler"), "el bloqueo no puede ir por el pooler");

        var bloqueo = new CicloSemanalBloqueo(directa, usuario, clave, 15);
        var dentro = new AtomicReference<String>();
        var resultado = bloqueo.ejecutar(PARTICIPANTE_INEXISTENTE, () -> {
            try (Connection otra = conectar(directa, usuario, clave)) {
                long k = CicloSemanalBloqueo.clave(PARTICIPANTE_INEXISTENTE);
                // Lo ve una conexion distinta: el bloqueo esta tomado en el servidor.
                int tomados = contar(otra, k);
                // Una segunda ejecucion del mismo participante no entra.
                var segunda = new CicloSemanalBloqueo(directa, usuario, clave, 15)
                        .ejecutar(PARTICIPANTE_INEXISTENTE, () -> "no deberia ejecutarse");
                // Y sigue tomado despues de varias transacciones de otra conexion.
                for (int i = 0; i < 3; i++) otra.createStatement().execute("SELECT 1");
                int sigue = contar(otra, k);
                dentro.set("tomados=" + tomados + " segunda=" + segunda.estado() + " sigue=" + sigue);
                return "ok";
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
        System.out.println("NEON durante el bloqueo: " + dentro.get() + " · resultado=" + resultado.estado());
        assertEquals(CicloSemanalBloqueo.Estado.EJECUTADO, resultado.estado());
        assertTrue(dentro.get().contains("tomados=1"), dentro.get());
        assertTrue(dentro.get().contains("segunda=EN_CURSO"), dentro.get());
        assertTrue(dentro.get().contains("sigue=1"), dentro.get());

        try (Connection c = conectar(directa, usuario, clave)) {
            assertEquals(0, contar(c, CicloSemanalBloqueo.clave(PARTICIPANTE_INEXISTENTE)), "liberado al terminar");
            // Sesion estable: el mismo backend en sentencias sucesivas (con PgBouncer en modo
            // transaccion podria cambiar).
            long pid1 = pid(c), pid2 = pid(c), pid3 = pid(c);
            System.out.println("NEON backend estable en la conexion directa: " + (pid1 == pid2 && pid2 == pid3));
            assertTrue(pid1 == pid2 && pid2 == pid3, "la conexion directa tiene que ser una sesion estable");
        }
    }

    private static Connection conectar(String url, String usuario, String clave) throws Exception {
        var p = new Properties();
        p.setProperty("user", usuario);
        p.setProperty("password", clave);
        p.setProperty("ApplicationName", "fitsense-verificacion-neon");
        p.setProperty("connectTimeout", "15");
        var c = DriverManager.getConnection(url, p);
        c.setAutoCommit(true);
        c.setReadOnly(true);
        return c;
    }

    private static int contar(Connection c, long k) throws Exception {
        try (var ps = c.prepareStatement("SELECT count(*) FROM pg_locks WHERE locktype = 'advisory'"
                + " AND ((classid::bigint << 32) | objid::bigint) = ? AND granted")) {
            ps.setLong(1, k);
            try (var rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static long pid(Connection c) throws Exception {
        try (var rs = c.createStatement().executeQuery("SELECT pg_backend_pid()")) {
            rs.next();
            return rs.getLong(1);
        }
    }

    /** Solo el host, sin usuario, contrasena ni parametros. */
    private static String host(String jdbcUrl) {
        var m = java.util.regex.Pattern.compile("//([^/:?]+)").matcher(jdbcUrl);
        return m.find() ? m.group(1) : "?";
    }
}
