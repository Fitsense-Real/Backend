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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Catalogo integral · V50: clasificacion (9) y equipamiento (17) verificados con el GIF.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("Catalogo integral · V50 clasificacion y equipamiento")
class CatalogoIntegralClasificacionIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired JdbcTemplate jdbc;

    String requisitos(long id) {
        return jdbc.queryForObject("""
                SELECT coalesce(string_agg(r.group_no || ':' || t.code, ',' ORDER BY r.group_no, t.code), '')
                  FROM exercise_equipment_requirements r JOIN equipment_types t ON t.equipment_id = r.equipment_id
                 WHERE r.exercise_id = ?""", String.class, id);
    }

    @ParameterizedTest(name = "{0} -> {1}/{2}/{3}")
    @CsvSource({
            "300, back, lats, VERTICAL_PULL", "322, shoulders, delts, SHOULDER_RAISE",
            "353, back, lats, SHOULDER_EXTENSION", "382, chest, pectorals, HORIZONTAL_ADDUCTION",
            "407, upper arms, triceps, ELBOW_EXTENSION", "574, shoulders, delts, SHOULDER_RAISE",
            "875, upper legs, glutes, HIP_EXTENSION", "1070, back, lats, MOBILITY", "1254, lower legs, calves, MOBILITY"})
    void clasificacion(long id, String zona, String objetivo, String patron) {
        var r = jdbc.queryForMap("SELECT bp.code AS zona, e.target_muscle AS objetivo, e.movement_pattern AS patron"
                + " FROM exercises e JOIN body_parts bp ON bp.body_part_id = e.body_part_id WHERE e.exercise_id = ?", id);
        assertEquals(zona, r.get("zona"));
        assertEquals(objetivo, r.get("objetivo"));
        assertEquals(patron, r.get("patron"));
    }

    @Test
    @DisplayName("1254 pasa a estiramiento por tiempo, con su nombre y el cierre de las instrucciones coherentes")
    void estiramientoDeGemelos() {
        var r = jdbc.queryForMap("SELECT name_es, default_prescription, instructions_es,"
                + " instruction_steps_es::text AS pasos FROM exercises WHERE exercise_id = 1254");
        assertEquals("Estiramiento de gemelos inclinado contra la pared", r.get("name_es"));
        assertEquals("DURATION", r.get("default_prescription"));
        assertTrue(((String) r.get("instructions_es")).endsWith("Mantén la duración indicada."));
        assertFalse(((String) r.get("pasos")).contains("repeticiones"));
    }

    @ParameterizedTest(name = "{0}: {1}")
    @CsvSource(delimiter = ';', value = {
            "316;1:pull-up bar,1:smith machine", "1306;1:captains chair,1:parallel bars",
            "1307;1:gymnastic rings,1:suspension straps", "912;1:bench,2:pull-up bar,2:smith machine",
            "212;1:barbell,2:bench,3:squat rack", "496;1:bench", "820;1:stability ball,2:dumbbell",
            "1195;1:ghd bench,1:leverage machine", "1196;1:bench", "1201;1:bench",
            "61;1:pull-up bar,2:other", "62;1:pull-up bar,2:other", "871;1:pull-up bar,2:other",
            "368;1:cable,2:bench", "532;1:stationary bike", "997;1:landmine,2:barbell", "908;1:hyperextension bench"})
    void equipamiento(long id, String esperado) {
        assertEquals(esperado, requisitos(id));
    }

    @Test
    @DisplayName("implemento principal de los sustituidos")
    void implementoPrincipal() {
        assertEquals("stationary bike", principal(532));
        assertEquals("landmine", principal(997));
        assertEquals("hyperextension bench", principal(908));
    }

    String principal(long id) {
        return jdbc.queryForObject("SELECT t.code FROM exercises e JOIN equipment_types t"
                + " ON t.equipment_id = e.equipment_id WHERE e.exercise_id = ?", String.class, id);
    }
}
