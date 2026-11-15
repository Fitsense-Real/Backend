package main.web.services.fitsense.catalogo;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.*;

/**
 * CIERRE DEL CATALOGO · PROCEDENCIA DE CADA ATRIBUTO (opt-in, solo lectura sobre una base efimera).
 * <p>
 * Aplica las migraciones UNA A UNA sobre PostgreSQL 16 desechable y, tras cada version, compara el catalogo con el de la
 * version anterior. Escribe un cambio por fila: version, ejercicio, atributo, valor anterior y valor nuevo. Asi cada
 * valor final queda atribuido a la migracion que lo escribio (dataset, regla, traduccion, revision o auditoria), sin
 * depender de lo que dicen los comentarios de las migraciones.
 * <p>
 * Atributos: todas las columnas de {@code exercises} (via {@code row_to_json}, sin fechas tecnicas), el codigo de zona
 * y de equipo, y los requisitos de equipamiento (V23+). El texto se normaliza a una linea.
 * <p>
 * {@code -Dfitsense.catalogo.procedencia=true -Dfitsense.catalogo.salida=<dir>}
 */
@Testcontainers
@EnabledIfSystemProperty(named = "fitsense.catalogo.procedencia", matches = "true")
@DisplayName("Catalogo · procedencia de cada atributo por migracion (opt-in)")
class ProcedenciaCatalogoIT {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    static final Set<String> TECNICAS = Set.of("created_at", "updated_at");

    @Test
    void procedencia() throws Exception {
        var dir = Path.of(System.getProperty("fitsense.catalogo.salida", "target/procedencia"));
        Files.createDirectories(dir);
        var base = Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration").placeholderReplacement(false);
        var versiones = Arrays.stream(base.load().info().all()).map(MigrationInfo::getVersion).toList();
        var cambios = new StringBuilder("version\tid\tatributo\tanterior\tnuevo\n");
        var resumen = new StringBuilder("version\tdescripcion\tejercicios\tcambios\n");
        Map<Long, Map<String, String>> previo = new HashMap<>();
        for (var v : versiones) {
            var info = base.target(v).load();
            info.migrate();
            String desc = Arrays.stream(info.info().applied()).filter(m -> m.getVersion().equals(v))
                    .map(MigrationInfo::getDescription).findFirst().orElse("");
            Map<Long, Map<String, String>> actual;
            try (Connection c = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(),
                    postgres.getPassword())) {
                actual = foto(c);
            }
            int n = 0;
            var tocados = new HashSet<Long>();
            for (var e : actual.entrySet()) {
                var antes = previo.getOrDefault(e.getKey(), Map.of());
                for (var a : e.getValue().entrySet()) {
                    String viejo = antes.get(a.getKey());
                    if (!Objects.equals(viejo, a.getValue())) {
                        cambios.append(v).append('\t').append(e.getKey()).append('\t').append(a.getKey()).append('\t')
                                .append(viejo == null ? "∅" : viejo).append('\t').append(a.getValue()).append('\n');
                        n++;
                        tocados.add(e.getKey());
                    }
                }
            }
            resumen.append(v).append('\t').append(desc).append('\t').append(tocados.size()).append('\t').append(n)
                    .append('\n');
            previo = actual;
        }
        Files.writeString(dir.resolve("procedencia_cambios.tsv"), cambios.toString(), StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("procedencia_resumen.tsv"), resumen.toString(), StandardCharsets.UTF_8);
    }

    private static Map<Long, Map<String, String>> foto(Connection c) throws Exception {
        var r = new TreeMap<Long, Map<String, String>>();
        if (!existe(c, "exercises")) return r;
        boolean reqs = existe(c, "exercise_equipment_requirements");
        String sql = "SELECT e.exercise_id, row_to_json(e)::text, bp.code, eq.code"
                + (reqs ? ", coalesce((SELECT string_agg(q.group_no || ':' || t.code, ',' ORDER BY q.group_no, t.code)"
                + " FROM exercise_equipment_requirements q JOIN equipment_types t ON t.equipment_id = q.equipment_id"
                + " WHERE q.exercise_id = e.exercise_id), '')" : ", NULL")
                + " FROM exercises e LEFT JOIN body_parts bp ON bp.body_part_id = e.body_part_id"
                + " LEFT JOIN equipment_types eq ON eq.equipment_id = e.equipment_id";
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        try (var st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                var m = new TreeMap<String, String>();
                var json = mapper.readTree(rs.getString(2));
                json.fieldNames().forEachRemaining(k -> {
                    if (TECNICAS.contains(k)) return;
                    var val = json.get(k);
                    m.put(k, val.isNull() ? "" : (val.isTextual() ? val.asText() : val.toString())
                            .replaceAll("[\\t\\r\\n]+", " "));
                });
                m.put("body_part_code", Objects.toString(rs.getString(3), ""));
                m.put("equipment_code", Objects.toString(rs.getString(4), ""));
                if (reqs) m.put("requisitos", Objects.toString(rs.getString(5), ""));
                r.put(rs.getLong(1), m);
            }
        }
        return r;
    }

    private static boolean existe(Connection c, String tabla) throws Exception {
        try (var ps = c.prepareStatement("SELECT to_regclass(?) IS NOT NULL")) {
            ps.setString(1, "public." + tabla);
            try (var rs = ps.executeQuery()) {
                rs.next();
                return rs.getBoolean(1);
            }
        }
    }
}
