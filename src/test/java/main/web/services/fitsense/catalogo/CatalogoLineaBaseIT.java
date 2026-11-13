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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

/**
 * AUDITORIA INTEGRAL DEL CATALOGO · LINEA BASE (opt-in, solo lectura sobre una base efimera).
 * <p>
 * Aplica TODAS las migraciones vigentes sobre PostgreSQL 16 desechable y exporta el catalogo resultante, una fila por
 * ejercicio (activos e inactivos), con los atributos que la auditoria compara antes/despues. El texto se normaliza a
 * una linea (tabuladores y saltos de linea -> espacio). Tambien exporta el historial de Flyway aplicado, para que la
 * linea base quede asociada a un estado exacto de migraciones.
 * <p>
 * {@code -Dfitsense.catalogo.exportar=true -Dfitsense.catalogo.salida=<dir>}
 */
@Testcontainers
@EnabledIfSystemProperty(named = "fitsense.catalogo.exportar", matches = "true")
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("Catalogo integral · exportacion de la linea base (opt-in)")
class CatalogoLineaBaseIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired JdbcTemplate jdbc;

    static final String CATALOGO = """
            SELECT e.exercise_id, e.source_code, e.is_active, e.name_en, coalesce(e.name_es, ''),
                   bp.code, e.target_muscle, coalesce(e.synergist_muscle, ''),
                   coalesce(array_to_string(e.secondary_muscles, ','), ''), eq.code,
                   coalesce((SELECT string_agg(r.group_no || ':' || q.code, ',' ORDER BY r.group_no, q.code)
                               FROM exercise_equipment_requirements r
                               JOIN equipment_types q ON q.equipment_id = r.equipment_id
                              WHERE r.exercise_id = e.exercise_id), ''),
                   e.difficulty_level, e.default_prescription, coalesce(e.movement_pattern, ''),
                   coalesce(e.is_isolation::text, ''), e.high_impact, e.requires_floor, e.axial_load,
                   e.requires_kneeling, e.requires_hanging, e.requires_overhead, e.requires_inversion,
                   coalesce(e.gif_path, ''), coalesce(e.image_path, ''), coalesce(e.media_attribution, ''),
                   regexp_replace(coalesce(e.instructions_en, ''), '[\\t\\r\\n]+', ' ', 'g'),
                   regexp_replace(coalesce(e.instructions_es, ''), '[\\t\\r\\n]+', ' ', 'g'),
                   regexp_replace(coalesce(e.instruction_steps_es::text, ''), '[\\t\\r\\n]+', ' ', 'g')
              FROM exercises e
              JOIN body_parts bp ON bp.body_part_id = e.body_part_id
              JOIN equipment_types eq ON eq.equipment_id = e.equipment_id
             ORDER BY e.exercise_id
            """;

    static final String CABECERA = String.join("\t", "id", "source", "active", "name_en", "name_es", "bp", "target",
            "synergist", "secondary", "equip", "reqs", "dif", "presc", "pattern", "iso", "hi", "floor", "axial",
            "kneel", "hang", "overhead", "inv", "gif", "image", "attribution", "instr_en", "instr_es", "steps_es");

    @Test
    void exportar() throws Exception {
        var dir = Path.of(System.getProperty("fitsense.catalogo.salida", "target/catalogo_integral"));
        Files.createDirectories(dir);
        var filas = jdbc.query(CATALOGO, (rs, n) -> {
            int cols = rs.getMetaData().getColumnCount();
            var sb = new StringBuilder();
            for (int c = 1; c <= cols; c++) {
                if (c > 1) sb.append('\t');
                Object v = rs.getObject(c);
                String s = v instanceof Boolean b ? (b ? "t" : "f") : String.valueOf(v);
                sb.append(s.replace('\t', ' ').replace('\n', ' ').replace('\r', ' '));
            }
            return sb.toString();
        });
        Files.writeString(dir.resolve("catalogo.tsv"), CABECERA + "\n" + String.join("\n", filas) + "\n",
                StandardCharsets.UTF_8);
        var flyway = jdbc.queryForList("SELECT version || '\t' || description || '\t' || checksum || '\t' || success"
                        + " FROM flyway_schema_history WHERE version IS NOT NULL ORDER BY installed_rank", String.class)
                .stream().collect(Collectors.joining("\n"));
        Files.writeString(dir.resolve("flyway.tsv"), "version\tdescription\tchecksum\tsuccess\n" + flyway + "\n",
                StandardCharsets.UTF_8);
        var equipo = jdbc.queryForList("SELECT equipment_id || '\t' || code || '\t' || requires_gym"
                + " FROM equipment_types ORDER BY equipment_id", String.class).stream().collect(Collectors.joining("\n"));
        Files.writeString(dir.resolve("equipment_types.tsv"), "id\tcode\trequires_gym\n" + equipo + "\n",
                StandardCharsets.UTF_8);
        var partes = jdbc.queryForList("SELECT body_part_id || '\t' || code FROM body_parts ORDER BY body_part_id",
                String.class).stream().collect(Collectors.joining("\n"));
        Files.writeString(dir.resolve("body_parts.tsv"), "id\tcode\n" + partes + "\n", StandardCharsets.UTF_8);
    }
}
