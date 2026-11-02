package main.web.services.fitsense.planning;

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
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Auditoria C4/C6 (criterio aprobado el 30/10/2026): el grupo principal y el slot que acredita el
 * patron deben ser coherentes. Un patron de pierna (LOWER_KNEE, LOWER_HIP) con un grupo que no es
 * de pierna, o un patron de tronco superior (UPPER_PUSH, UPPER_PULL) con un grupo que no es de
 * tronco superior, es un CRUCE.
 * <p>
 * No es una restriccion sobre los existentes: los 24 cruces de V39 estan registrados en
 * {@code evidencia/c4_c6/cruces_grupo_slot_v39.tsv} y solo se informan como advertencia (algunos
 * pueden ser excepciones justificadas). La prueba falla unicamente si aparece un cruce NUEVO, que
 * tendria que corregirse o documentarse como excepcion anadiendolo al registro.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("C4/C6: auditoria de cruces entre grupo y slot")
class CoherenciaGrupoSlotIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    static final Path REGISTRO = Path.of("docs/FitSense/evidencia/c4_c6/cruces_grupo_slot_v39.tsv");

    @Autowired JdbcTemplate jdbc;

    @Test
    @DisplayName("los cruces registrados se advierten; uno nuevo hace fallar la prueba")
    void sinCrucesNuevos() throws Exception {
        List<String> cruces = jdbc.queryForList("""
                SELECT e.exercise_id || ' ' || e.name_en || ' (' || bp.code || '/' || e.movement_pattern || ')'
                  FROM exercises e JOIN body_parts bp ON bp.body_part_id = e.body_part_id
                 WHERE e.is_active
                   AND ((e.movement_pattern IN ('KNEE_DOMINANT', 'HIP_HINGE', 'HIP_EXTENSION')
                         AND bp.code NOT IN ('upper legs', 'lower legs'))
                     OR (e.movement_pattern IN ('HORIZONTAL_PUSH', 'VERTICAL_PUSH', 'HORIZONTAL_PULL', 'VERTICAL_PULL')
                         AND bp.code NOT IN ('chest', 'back', 'shoulders', 'upper arms', 'lower arms', 'neck')))
                 ORDER BY e.exercise_id""", String.class);
        Set<String> registrados = Files.readAllLines(REGISTRO).stream().skip(1).filter(l -> !l.isBlank())
                .map(l -> l.split("\t")[0]).collect(Collectors.toCollection(TreeSet::new));
        var actuales = cruces.stream().map(c -> c.split(" ")[0]).collect(Collectors.toCollection(TreeSet::new));
        var nuevos = cruces.stream().filter(c -> !registrados.contains(c.split(" ")[0])).toList();
        var resueltos = new TreeSet<>(registrados);
        resueltos.removeAll(actuales);

        System.out.println("C4/C6 ADVERTENCIA: " + (cruces.size() - nuevos.size()) + " cruces registrados siguen abiertos:");
        cruces.stream().filter(c -> !nuevos.contains(c)).forEach(c -> System.out.println("  " + c));
        System.out.println("C4/C6: resueltos respecto al registro inicial de V39: " + resueltos);
        assertEquals(List.of(), nuevos, "cruce nuevo entre grupo y slot: corregirlo o documentarlo en " + REGISTRO);
    }
}
