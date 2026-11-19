package main.web.services.fitsense.catalog;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
 * V46 (auditoria del catalogo, rama aislada): correcciones OBJETIVAS de traduccion. Comprueba los nombres corregidos,
 * que la migracion solo cambia {@code name_es} y que los nombres nuevos no activan ninguna regla por nombre (impacto).
 * La seleccion no cambia: lo demuestran los barridos de regresion (evidencia de la auditoria).
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("V46: traducciones objetivas (973, 1263) sin efecto en la seleccion")
class CatalogoV46IT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired JdbcTemplate jdbc;

    String nombre(long id) {
        return jdbc.queryForObject("SELECT name_es FROM exercises WHERE exercise_id = ?", String.class, id);
    }

    @Test
    void nombresCorregidos() {
        assertEquals("Gancho de izquierda (boxeo)", nombre(973));
        assertEquals("Sentadilla dividida (zancada estática)", nombre(1263));
        assertEquals("left hook. boxing", jdbc.queryForObject("SELECT name_en FROM exercises WHERE exercise_id = 973", String.class));
    }

    @Test
    void laMigracionSoloCambiaNameEs() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/V46__catalogo_traducciones_objetivas.sql"));
        var m = Pattern.compile("(?is)UPDATE\\s+exercises\\s+SET\\s+(.*?)\\s+WHERE").matcher(sql);
        int n = 0;
        while (m.find()) {
            n++;
            assertTrue(m.group(1).trim().toLowerCase(Locale.ROOT).matches("name_es\\s*=\\s*'[^']*'"), m.group(1));
        }
        assertEquals(2, n);
        assertFalse(sql.toLowerCase(Locale.ROOT).matches("(?s).*\\b(insert|delete|alter|drop)\\b.*"));
    }

    @Test
    void sinReglasPorNombre() {
        // Los mismos terminos que ImpactoPolicy (MODERADO y ALTO_POR_NOMBRE): ninguno aparece en los nombres nuevos.
        var impacto = Pattern.compile("\\b(trote|trotar|correr|carrera|skipping|rodillas altas|jumping|comba|cuerda|tijeras|"
                + "escaladores?|salto|saltos|saltando|pliom|burpee|sprint|esprint|caja|rebote|patinador)\\b");
        for (long id : new long[]{973, 1263})
            assertFalse(impacto.matcher(nombre(id).toLowerCase(Locale.ROOT)).find(), nombre(id));
    }
}
