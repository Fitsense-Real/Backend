package main.web.services.fitsense.catalogo;

import org.junit.jupiter.api.DisplayName;
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
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FITSENSE-CATALOGO-TECNICO-CONSERVADOR-v1 · congelacion (opt-in, -Dfitsense.catalogo.congelar=true). Reconstruye la
 * base desde PostgreSQL vacio aplicando todas las migraciones en orden y vuelca, en un orden estable, el catalogo
 * completo (exercises y su equipamiento) y el historial de Flyway, con su SHA-256. No modifica nada.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@EnabledIfSystemProperty(named = "fitsense.catalogo.congelar", matches = "true")
@DisplayName("Catalogo tecnico conservador v1 · volcado congelado desde una base nueva")
class CatalogoCongeladoIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired JdbcTemplate jdbc;

    private String volcado(String sql) {
        var filas = jdbc.queryForList(sql);
        if (filas.isEmpty()) return "";
        var cabecera = String.join("\t", filas.get(0).keySet());
        return cabecera + "\n" + filas.stream().map(f -> f.values().stream()
                .map(v -> v == null ? "" : String.valueOf(v).replace("\t", " ").replace("\n", "\\n"))
                .collect(Collectors.joining("\t"))).collect(Collectors.joining("\n")) + "\n";
    }

    @Test
    void volcar() throws Exception {
        var salida = Path.of(System.getProperty("fitsense.catalogo.salida", "target/catalogo_congelado"));
        Files.createDirectories(salida);
        var ficheros = Map.of(
                "exercises.tsv", "SELECT * FROM exercises ORDER BY exercise_id",
                "exercise_equipment_requirements.tsv", "SELECT * FROM exercise_equipment_requirements ORDER BY 1, 2, 3",
                "exercise_difficulty_approvals.tsv", "SELECT * FROM exercise_difficulty_approvals ORDER BY 1",
                "flyway.tsv", "SELECT version, description, type, script, checksum, success FROM flyway_schema_history"
                        + " ORDER BY installed_rank");
        var huellas = new StringBuilder();
        for (var e : new java.util.TreeMap<>(ficheros).entrySet()) {
            var texto = volcado(e.getValue());
            Files.writeString(salida.resolve(e.getKey()), texto);
            var sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(texto.getBytes(
                    java.nio.charset.StandardCharsets.UTF_8)));
            huellas.append(sha).append("  ").append(e.getKey()).append('\n');
        }
        Files.writeString(salida.resolve("SHA256SUMS"), huellas.toString());
        assertEquals(1219, jdbc.queryForObject("SELECT count(*) FROM exercises WHERE is_active", Integer.class));
        assertEquals(1376, jdbc.queryForObject("SELECT count(*) FROM exercises", Integer.class));
    }
}
