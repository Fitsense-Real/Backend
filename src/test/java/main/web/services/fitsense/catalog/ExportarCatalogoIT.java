package main.web.services.fitsense.catalog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

/**
 * Auditoria del catalogo (rama aislada, SOLO LECTURA): exporta el catalogo de la base efimera tras todas las
 * migraciones, con la consulta de {@code docs/FitSense/auditoria_catalogo/exportar_catalogo.sql} (sin la transaccion).
 * Opcional {@code -Dfitsense.catalogo.sql-previo=<fichero>}: variante contrafactual aplicada antes de exportar.
 * Opt-in: {@code -Dfitsense.catalogo.exportar=<salida.tsv>}.
 */
@Testcontainers
@EnabledIfSystemProperty(named = "fitsense.catalogo.exportar", matches = ".+")
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
class ExportarCatalogoIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired JdbcTemplate jdbc;

    @Test
    void exportar() throws Exception {
        String previo = System.getProperty("fitsense.catalogo.sql-previo", "");
        if (!previo.isBlank()) jdbc.execute(Files.readString(Path.of(previo)));
        String sql = Files.readString(Path.of("docs/FitSense/auditoria_catalogo/exportar_catalogo.sql"))
                .replace("BEGIN TRANSACTION READ ONLY;", "").replace("ROLLBACK;", "")
                .lines().filter(l -> !l.startsWith("--")).collect(Collectors.joining("\n")).trim();
        if (sql.endsWith(";")) sql = sql.substring(0, sql.length() - 1);
        var filas = jdbc.query(sql, (rs, i) -> {
            var b = new StringBuilder();
            int n = rs.getMetaData().getColumnCount();
            for (int c = 1; c <= n; c++) {
                if (c > 1) b.append('\t');
                Object v = rs.getObject(c);
                String t = v == null ? "" : (v instanceof Boolean bo ? (bo ? "t" : "f") : v.toString());
                b.append(t.replace('\t', ' ').replace('\n', ' '));
            }
            return b.toString();
        });
        Path salida = Path.of(System.getProperty("fitsense.catalogo.exportar"));
        Files.createDirectories(salida.toAbsolutePath().getParent());
        Files.writeString(salida, String.join("\n", filas) + "\n");
    }
}
