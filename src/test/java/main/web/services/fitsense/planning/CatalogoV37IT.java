package main.web.services.fitsense.planning;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V37 (auditoria del catalogo, bloque A aprobado el 29/10/2026). Sobre una base
 * Flyway hasta la 36 ejecuta el archivo de V37 de db/migration y, despues, la
 * propuesta V37.1 (la copia historica de docs/FitSense/auditoria_catalogo/v37/separables, que
 * desde el 30/10/2026 es la V38 oficial con otros nombres de objetos temporales), y comprueba:
 * <ul>
 *   <li>cada fila de {@code correcciones_v37.tsv} queda con su valor «despues» (776 inactivo);</li>
 *   <li>ningun otro ejercicio ni requisito cambia;</li>
 *   <li>V27/V28 intactas: {@code exercise_difficulty_approvals} identica, con el evento de 776;</li>
 *   <li>suspension implica brazos arriba en todo el catalogo;</li>
 *   <li>si el estado anterior no es el esperado (reaplicar, o una edicion manual), aborta sin cambios.</li>
 * </ul>
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "spring.flyway.target=36"
})
class CatalogoV37IT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    static final Path DIR = Path.of("docs/FitSense/auditoria_catalogo/v37");
    static final Path A = Path.of("src/main/resources/db/migration/V37__catalog_safety_corrections.sql");
    static final Path B = DIR.resolve("separables/V37_1__catalog_classification_equipment.sql");

    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource ds;

    /** Una fila por ejercicio con todas las columnas que la propuesta puede tocar. */
    static final String HUELLA = """
            SELECT e.exercise_id, md5(concat_ws('|', e.requires_floor, e.requires_kneeling, e.requires_hanging,
                   e.requires_overhead, e.requires_inversion, e.high_impact, e.axial_load, e.difficulty_level,
                   e.movement_pattern, e.target_muscle, e.body_part_id, e.equipment_id, e.is_active, e.name_en,
                   e.name_es, e.instructions_es,
                   (SELECT string_agg(r.group_no || ':' || r.equipment_id, ',' ORDER BY r.group_no, r.equipment_id)
                      FROM exercise_equipment_requirements r WHERE r.exercise_id = e.exercise_id)))
              FROM exercises e ORDER BY 1""";

    private Map<Long, String> huella() {
        var m = new TreeMap<Long, String>();
        jdbc.query(HUELLA, rs -> { m.put(rs.getLong(1), rs.getString(2)); });
        return m;
    }

    private String aprobaciones() {
        return jdbc.queryForObject("SELECT md5(coalesce(string_agg(t::text, ',' ORDER BY t::text), ''))"
                + " FROM exercise_difficulty_approvals t", String.class);
    }

    /** Ejecuta el archivo en una transaccion, como Flyway. */
    private void ejecutar(Path sql) throws Exception {
        try (var c = ds.getConnection(); var st = c.createStatement()) {
            c.setAutoCommit(false);
            try {
                st.execute(Files.readString(sql));
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    private List<String[]> correcciones(String prefijoBloque) throws Exception {
        var out = new ArrayList<String[]>();
        for (var l : Files.readAllLines(DIR.resolve("correcciones_v37.tsv")).subList(1,
                Files.readAllLines(DIR.resolve("correcciones_v37.tsv")).size())) {
            var c = l.split("\t", -1);
            if (c[3].startsWith(prefijoBloque)) out.add(c);
        }
        return out;
    }

    private String valor(long id, String columna) {
        return jdbc.queryForObject("""
                SELECT CASE ? WHEN 'body_part' THEN bp.code WHEN 'equipment' THEN eq.code
                         WHEN 'requisitos' THEN coalesce((SELECT string_agg(r.group_no || ':' || q.code, ','
                                  ORDER BY r.group_no || ':' || q.code) FROM exercise_equipment_requirements r
                                  JOIN equipment_types q ON q.equipment_id = r.equipment_id
                                  WHERE r.exercise_id = e.exercise_id), '')
                         ELSE (to_jsonb(e) ->> ?) END
                  FROM exercises e JOIN body_parts bp ON bp.body_part_id = e.body_part_id
                  JOIN equipment_types eq ON eq.equipment_id = e.equipment_id WHERE e.exercise_id = ?""",
                String.class, columna, columna, id);
    }

    @Test
    @Order(1)
    @DisplayName("V37 aplica exactamente su matriz (776 inactivo), nada mas, y deja V28 intacta")
    void bloqueA() throws Exception {
        var antes = huella();
        var aprob = aprobaciones();
        var filas = correcciones("A");
        for (var f : filas) assertEquals(f[5], valor(Long.parseLong(f[0]), f[4]), "antes " + f[0] + " " + f[4]);
        ejecutar(A);
        for (var f : filas) assertEquals(f[6], valor(Long.parseLong(f[0]), f[4]), "despues " + f[0] + " " + f[4]);
        var tocados = filas.stream().map(f -> Long.parseLong(f[0])).collect(java.util.stream.Collectors.toSet());
        var despues = huella();
        var distintos = new TreeSet<Long>();
        antes.forEach((id, h) -> { if (!h.equals(despues.get(id))) distintos.add(id); });
        assertEquals(new TreeSet<>(tocados), distintos, "solo cambian los ejercicios de la matriz");
        assertEquals(aprob, aprobaciones(), "exercise_difficulty_approvals (V27/V28) no cambia");
        assertEquals(4, jdbc.queryForObject("SELECT count(*) FROM exercise_difficulty_approvals"
                + " WHERE exercise_id IN (122, 776, 872, 878) AND decision = 'APPROVED'", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM exercises WHERE requires_hanging AND NOT"
                + " requires_overhead", Integer.class));
        // 776: desactivado, con sus caracteristicas reales documentadas (ANEXO_776_V28.md)
        assertEquals(List.of(false, true, true, true, 3, "gymnastic rings"), jdbc.queryForObject(
                "SELECT e.is_active, e.requires_hanging, e.requires_overhead, e.requires_inversion, e.difficulty_level,"
                        + " q.code FROM exercises e JOIN equipment_types q ON q.equipment_id = e.equipment_id"
                        + " WHERE e.exercise_id = 776",
                (rs, n) -> List.of(rs.getBoolean(1), rs.getBoolean(2), rs.getBoolean(3), rs.getBoolean(4), rs.getInt(5),
                        rs.getString(6))));
    }

    @Test
    @Order(2)
    @DisplayName("reaplicar V37 aborta sin cambios")
    void reaplicarAborta() throws Exception {
        var antes = huella();
        var e = assertThrows(SQLException.class, () -> ejecutar(A));
        assertTrue(e.getMessage().contains("V37: estado anterior inesperado"), e.getMessage());
        assertEquals(antes, huella());
    }

    @Test
    @Order(3)
    @DisplayName("V37.1 (separable) aplica su matriz sobre V37 y una edicion previa la hace abortar")
    void bloqueB() throws Exception {
        var filas = correcciones("B");
        var bandera = filas.stream().filter(f -> f[4].startsWith("requires_")).findFirst().orElseThrow();
        String sql = "UPDATE exercises SET " + bandera[4] + " = NOT " + bandera[4] + " WHERE exercise_id = ?";
        long id = Long.parseLong(bandera[0]);
        // Una edicion manual de una fila esperada hace abortar sin cambios.
        jdbc.update(sql, id);
        var editada = huella();
        var e = assertThrows(SQLException.class, () -> ejecutar(B));
        assertTrue(e.getMessage().contains("V37.1: estado anterior inesperado"), e.getMessage());
        assertEquals(editada, huella());
        jdbc.update(sql, id);
        // Sin la edicion, aplica su matriz y deja V28 intacta.
        var aprob = aprobaciones();
        ejecutar(B);
        for (var f : filas) assertEquals(f[6], valor(Long.parseLong(f[0]), f[4]), "despues " + f[0] + " " + f[4]);
        assertEquals(aprob, aprobaciones());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM exercises WHERE requires_hanging AND NOT"
                + " requires_overhead", Integer.class));
    }
}
