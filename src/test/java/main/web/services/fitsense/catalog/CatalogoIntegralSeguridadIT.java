package main.web.services.fitsense.catalog;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Catalogo integral · V47 (banderas de seguridad que el GIF exige) y V48 (contencion conservadora de dificultad).
 * Comprueba el estado final de cada fila, que las migraciones solo endurecen (nunca relajan) y que no tocan nada mas.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("Catalogo integral · V47 banderas de seguridad y V48 dificultad conservadora")
class CatalogoIntegralSeguridadIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired JdbcTemplate jdbc;

    static final String MIG = "src/main/resources/db/migration/";

    boolean bandera(long id, String columna) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT " + columna + " FROM exercises WHERE exercise_id = ?", Boolean.class, id));
    }

    @ParameterizedTest(name = "{0} {1} = true")
    @CsvSource({
            "66, requires_overhead", "206, requires_overhead", "474, requires_overhead", "497, requires_overhead",
            "594, requires_overhead", "1300, requires_overhead",
            "149, axial_load", "220, axial_load", "227, axial_load", "276, axial_load", "819, axial_load",
            "889, axial_load", "999, axial_load", "1000, axial_load", "1213, axial_load", "1214, axial_load",
            "275, requires_floor", "685, requires_floor", "805, requires_floor", "895, requires_floor",
            "460, high_impact", "496, high_impact",
            "685, requires_kneeling", "696, requires_kneeling", "805, requires_kneeling", "995, requires_kneeling",
            "1005, requires_kneeling"})
    void v47BanderaQueElGifExige(long id, String columna) {
        assertTrue(bandera(id, columna), id + " " + columna);
    }

    /**
     * 503 (dead bug): V47 le anadio requires_overhead y V52 se lo quita. Conflicto resuelto con cuatro miradas al GIF
     * completo: segunda revision (lote L14), verificacion independiente (W03, ALTA) y el coordinador. Tumbado boca arriba,
     * los brazos apuntan al techo, unos 90 grados, en los 12 fotogramas y nunca pasan de la cabeza
     * (docs/FitSense/catalogo_integral/VALIDACION_SEGURIDAD.md). V47 se aplico, como prueba soloEndurecen.
     */
    @Test
    @DisplayName("503: la bandera OH de V47 la retira V52 con verificacion independiente")
    void v47Superado503() throws Exception {
        assertFalse(bandera(503, "requires_overhead"));
        assertTrue(Files.readString(Path.of(MIG + "V52__catalogo_cierre_correcciones_verificadas.sql"))
                .contains("-- 503 dead bug: requires_overhead:"));
    }

    @ParameterizedTest(name = "{0} -> d{1}")
    @CsvSource({"464, 3", "1108, 3", "1191, 3", "862, 3", "874, 3", "868, 3", "880, 3", "915, 3", "916, 3",
            "1192, 3", "821, 2", "1201, 2"})
    void v48DificultadConservadora(long id, int nivel) {
        assertEquals(nivel, jdbc.queryForObject("SELECT difficulty_level FROM exercises WHERE exercise_id = ?",
                Integer.class, id));
    }

    @Test
    @DisplayName("V47 solo pone banderas a TRUE y V48 solo sube niveles: ninguna relaja, ninguna toca otra columna")
    void soloEndurecen() throws Exception {
        String v47 = Files.readString(Path.of(MIG + "V47__catalogo_integral_banderas_seguridad.sql"));
        String v48 = Files.readString(Path.of(MIG + "V48__catalogo_integral_dificultad_conservadora.sql"));
        var set = Pattern.compile("(?is)UPDATE\\s+exercises\\s+SET\\s+(.*?)\\s+WHERE\\s+(.*?);");
        int n47 = 0, n48 = 0;
        for (var m = set.matcher(v47); m.find(); n47++) {
            for (var asignacion : m.group(1).split(","))
                assertTrue(asignacion.trim().toLowerCase(Locale.ROOT).matches(
                        "(high_impact|requires_floor|axial_load|requires_kneeling|requires_overhead)\\s*=\\s*true"),
                        asignacion);
            assertTrue(m.group(2).contains("= FALSE"), "sin guarda del estado anterior: " + m.group(2));
        }
        for (var m = set.matcher(v48); m.find(); n48++) {
            assertTrue(m.group(1).trim().matches("difficulty_level = [23]"), m.group(1));
            assertTrue(m.group(2).contains("difficulty_level = 1"), m.group(2));
        }
        assertEquals(26, n47);
        assertEquals(12, n48);
        for (var sql : new String[]{v47, v48})
            // Sin comentarios ni literales (un nombre de la fuente puede contener «drop»).
            assertFalse(sql.toLowerCase(Locale.ROOT).replaceAll("--[^\n]*", "").replaceAll("'[^']*'", "''")
                    .matches("(?s).*\\b(insert|delete|alter|drop|is_active)\\b.*"));
    }

    @Test
    @DisplayName("ningun ejercicio activo pierde una bandera de seguridad respecto a V46 (las de V47 solo se suman)")
    void sinRelajaciones() {
        // Recuentos de banderas activas tras V46 (linea base de la auditoria) + las que suma V47.
        assertEquals(Integer.valueOf(0), jdbc.queryForObject("""
                SELECT count(*) FROM exercises
                 WHERE exercise_id IN (66, 206, 474, 497, 594, 1300) AND NOT requires_overhead""", Integer.class));
        // 503: V52 retira la OH de V47 con verificacion independiente (v47Superado503).
        assertEquals(Integer.valueOf(1219), jdbc.queryForObject(
                "SELECT count(*) FROM exercises WHERE is_active", Integer.class), "V47/V48 no activan ni desactivan");
    }
}
