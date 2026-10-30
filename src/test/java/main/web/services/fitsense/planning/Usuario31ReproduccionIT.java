package main.web.services.fitsense.planning;

import main.web.services.fitsense.EsquemaVigenteSobreCatalogoAnterior;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Usuario 31 con el catalogo V36 (Flyway hasta la 36), conservado como evidencia:
 *   1. el catalogo V36 tal cual: el plan 63 era valido y daba 776 y 1320;
 *   2. con las correcciones confirmadas de 776, 1320, 1103, 1185 y 309 aplicadas
 *      solo a esta base desechable: los rechazos de la IA no cambian.
 * El contenedor es compartido: el escenario corregido va despues del actual.
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Import(EsquemaVigenteSobreCatalogoAnterior.class)
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "spring.flyway.target=36"
})
class Usuario31ReproduccionIT extends Usuario31Base {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    @Order(1)
    @DisplayName("catalogo actual: se reproducen los tres rechazos, el plan 63 es valido y 776 y 1320 siguen elegibles")
    void catalogoActual() {
        var r = informe("actual");
        assertEquals("actual elegibles: 776=si 1320=si 1185=si 309=si 1103=si 301=si", r.get(0));
        rechazosDeLaIa(r, 13);
        assertEquals("actual plan 63: VALIDA", r.get(4));
        contiene(r.get(5), " 776[back/HIP_HINGE]", " 1320[upper arms/HORIZONTAL_PUSH]");
    }

    @Test
    @Order(2)
    @DisplayName("catalogo corregido: mismos rechazos de la IA; el plan 63 ya no es publicable; el motor sigue publicando")
    void catalogoCorregido() {
        // 776: GIF = suspension invertida en anillas.  1320: sentado en el suelo.
        // 1103: sentadilla (upper legs/quads).  1185: brazos por encima de la cabeza.
        // 309: una rodilla apoyada en el banco.
        jdbc.update("UPDATE exercises SET requires_hanging = true, requires_overhead = true,"
                + " requires_inversion = true, difficulty_level = 3 WHERE exercise_id = 776");
        jdbc.update("UPDATE exercises SET requires_floor = true WHERE exercise_id = 1320");
        jdbc.update("UPDATE exercises SET body_part_id = (SELECT body_part_id FROM body_parts WHERE code = 'upper legs'),"
                + " target_muscle = 'quads' WHERE exercise_id = 1103");
        jdbc.update("UPDATE exercises SET requires_overhead = true WHERE exercise_id = 1185");
        jdbc.update("UPDATE exercises SET requires_kneeling = true WHERE exercise_id = 309");
        var r = informe("corregido");
        assertEquals("corregido elegibles: 776=NO 1320=NO 1185=si 309=si 1103=si 301=si", r.get(0));
        rechazosDeLaIa(r, 12);
        contiene(r.get(4), "V1: el ejercicio 776 no esta en available_exercises",
                "V1: el ejercicio 1320 no esta en available_exercises",
                "V15: el entrenamiento del " + DOMINGO + " dedica 4 de 7 ejercicios a upper legs");
        assertTrue(r.get(5).startsWith("corregido RULE_ENGINE: | SATURDAY UPPER_BODY"), r.get(5));
        assertFalse(r.get(5).contains(" 776[") || r.get(5).contains(" 1320["), r.get(5));
    }
}
