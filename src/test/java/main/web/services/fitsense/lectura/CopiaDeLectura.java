package main.web.services.fitsense.lectura;

import org.flywaydb.core.Flyway;

import java.sql.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * COPIA DE SOLO LECTURA de una base FitSense (Neon) a una base local efimera.
 * <p>
 * La fuente se abre con {@code setReadOnly(true)} y una transaccion
 * {@code REPEATABLE READ, READ ONLY}: PostgreSQL rechaza cualquier escritura,
 * y si {@code transaction_read_only} no es {@code on} se aborta antes de leer.
 * Solo se ejecutan SELECT. La copia (destino) se crea con las migraciones del
 * repositorio hasta V33, la version de Neon, y se rellena tabla a tabla con las
 * columnas comunes; despues Spring aplica la V34 sobre la copia, nunca sobre
 * la fuente.
 */
public final class CopiaDeLectura {

    /** Lo que se copio, para el informe. */
    public record Resultado(String versionFuente, Map<String, Integer> filas, List<String> omitidas) {}

    private CopiaDeLectura() {
    }

    public static Resultado copiar(String urlFuente, String usuarioFuente, String claveFuente,
                                   String urlDestino, String usuarioDestino, String claveDestino) throws SQLException {
        Flyway.configure().dataSource(urlDestino, usuarioDestino, claveDestino)
                .locations("classpath:db/migration").placeholderReplacement(false).target("33").load().migrate();

        try (var fuente = DriverManager.getConnection(urlFuente, usuarioFuente, claveFuente);
             var destino = DriverManager.getConnection(conStringtype(urlDestino), usuarioDestino, claveDestino)) {
            soloLectura(fuente);
            String version = escalar(fuente, "SELECT version FROM flyway_schema_history WHERE success AND version IS NOT NULL ORDER BY installed_rank DESC LIMIT 1");

            destino.setAutoCommit(false);
            try (var st = destino.createStatement()) {
                st.execute("SET session_replication_role = replica");   // sin disparadores ni FK al copiar
            }
            // La copia refleja los datos de la fuente, incluidos catalogo y configuracion
            // (que las migraciones tambien siembran): se vacia antes de llenarla.
            var todas = tablas(destino);
            try (var st = destino.createStatement()) {
                st.execute("TRUNCATE " + String.join(", ", todas.stream().map(t -> "\"" + t + "\"").toList())
                        + " RESTART IDENTITY CASCADE");
            }
            var filas = new LinkedHashMap<String, Integer>();
            var omitidas = new ArrayList<String>();
            for (var tabla : todas) {
                var columnas = columnasComunes(fuente, destino, tabla);
                if (columnas.isEmpty()) {
                    omitidas.add(tabla);
                    continue;
                }
                filas.put(tabla, copiarTabla(fuente, destino, tabla, columnas));
            }
            reiniciarIdentidades(destino);
            destino.commit();
            fuente.rollback();
            return new Resultado(version, filas, omitidas);
        }
    }

    /** Fuente en solo lectura, comprobado. Si no se puede garantizar, no se lee nada. */
    static void soloLectura(Connection fuente) throws SQLException {
        fuente.setReadOnly(true);
        fuente.setAutoCommit(false);
        try (var st = fuente.createStatement()) {
            st.execute("SET SESSION CHARACTERISTICS AS TRANSACTION ISOLATION LEVEL REPEATABLE READ, READ ONLY");
            st.execute("SET TRANSACTION ISOLATION LEVEL REPEATABLE READ, READ ONLY");
        }
        if (!"on".equals(escalar(fuente, "SHOW transaction_read_only")))
            throw new IllegalStateException("La conexion a la fuente no quedo en solo lectura: no se lee nada.");
    }

    private static String conStringtype(String url) {
        return url + (url.contains("?") ? "&" : "?") + "stringtype=unspecified";
    }

    private static String escalar(Connection c, String sql) throws SQLException {
        try (var st = c.createStatement(); var rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    private static List<String> tablas(Connection destino) throws SQLException {
        var lista = new ArrayList<String>();
        try (var st = destino.createStatement(); var rs = st.executeQuery(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'"
                        + " AND table_type = 'BASE TABLE' AND table_name <> 'flyway_schema_history' ORDER BY 1")) {
            while (rs.next()) lista.add(rs.getString(1));
        }
        return lista;
    }

    private static List<String> columnasComunes(Connection fuente, Connection destino, String tabla)
            throws SQLException {
        var enFuente = new ArrayList<String>();
        try (var ps = fuente.prepareStatement("SELECT column_name FROM information_schema.columns"
                + " WHERE table_schema = 'public' AND table_name = ?")) {
            ps.setString(1, tabla);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) enFuente.add(rs.getString(1));
            }
        }
        var comunes = new ArrayList<String>();
        try (var ps = destino.prepareStatement("SELECT column_name FROM information_schema.columns"
                + " WHERE table_schema = 'public' AND table_name = ? AND is_generated = 'NEVER'"
                + " ORDER BY ordinal_position")) {
            ps.setString(1, tabla);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) if (enFuente.contains(rs.getString(1))) comunes.add(rs.getString(1));
            }
        }
        return comunes;
    }

    private static int copiarTabla(Connection fuente, Connection destino, String tabla, List<String> columnas)
            throws SQLException {
        String lista = columnas.stream().map(c -> "\"" + c + "\"").reduce((a, b) -> a + ", " + b).orElseThrow();
        String marcas = columnas.stream().map(c -> "?").reduce((a, b) -> a + ", " + b).orElseThrow();
        int n = 0;
        try (var st = fuente.createStatement(); var rs = st.executeQuery("SELECT " + lista + " FROM \"" + tabla + "\"");
             var ins = destino.prepareStatement("INSERT INTO \"" + tabla + "\" (" + lista + ") OVERRIDING SYSTEM VALUE"
                     + " VALUES (" + marcas + ")")) {
            while (rs.next()) {
                for (int i = 1; i <= columnas.size(); i++) {
                    Object v = rs.getObject(i);
                    if (v instanceof Array a) v = destino.createArrayOf(a.getBaseTypeName(), (Object[]) a.getArray());
                    ins.setObject(i, v);
                }
                ins.addBatch();
                if (++n % 500 == 0) ins.executeBatch();
            }
            ins.executeBatch();
        }
        return n;
    }

    private static void reiniciarIdentidades(Connection destino) throws SQLException {
        var columnas = new ArrayList<String[]>();
        try (var st = destino.createStatement(); var rs = st.executeQuery(
                "SELECT table_name, column_name FROM information_schema.columns WHERE table_schema = 'public'"
                        + " AND (is_identity = 'YES' OR column_default LIKE 'nextval%')")) {
            while (rs.next()) columnas.add(new String[]{rs.getString(1), rs.getString(2)});
        }
        for (var tc : columnas)
            try (var st = destino.createStatement()) {
                st.execute(("SELECT setval(pg_get_serial_sequence('\"%1$s\"', '%2$s'),"
                        + " coalesce((SELECT max(\"%2$s\") FROM \"%1$s\"), 0) + 1, false)").formatted(tc[0], tc[1]));
            }
    }
}
