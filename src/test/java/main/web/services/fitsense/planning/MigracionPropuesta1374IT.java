package main.web.services.fitsense.planning;

import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalCatalogService;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanningProfile;
import main.web.services.fitsense.planning.domain.model.valueobjects.SafetyProfile;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * La migracion PROPUESTA de INC-CAT-1374, ejecutada sobre PostgreSQL efimero
 * con V1..V33 aplicadas por Flyway, igual que la aplicaria Flyway: el script
 * entero en una transaccion, con los disparadores diferidos comprobados al
 * COMMIT. No esta en db/migration: esta prueba la lee de
 * docs/FitSense/migraciones_propuestas/.
 * <p>
 * Se ejecuta con Flyway hasta la 36: V37 ya corrige 1374 (colgado de la barra,
 * TRUNK_FLEXION), y esta propuesta historica se escribio para el estado anterior.
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "spring.flyway.target=36"
})
class MigracionPropuesta1374IT {

    static final Path SCRIPT = Path.of("docs", "FitSense", "migraciones_propuestas",
            "V34__correccion_inc_cat_1374.sql");

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager tm;
    @Autowired ExternalCatalogService catalogo;

    private void aplicar() throws Exception {
        var sql = Files.readString(SCRIPT);
        new TransactionTemplate(tm).executeWithoutResult(s -> jdbc.execute(sql));
    }

    private Map<String, Object> registro() {
        return jdbc.queryForMap("SELECT e.movement_pattern, e.requires_hanging, e.requires_overhead, e.high_impact,"
                + " t.code AS principal, e.difficulty_level, e.default_prescription, e.is_active, e.body_part_id,"
                + " e.instructions_es, (SELECT string_agg(r.group_no || ':' || q.code, ',') FROM"
                + " exercise_equipment_requirements r JOIN equipment_types q ON q.equipment_id = r.equipment_id"
                + " WHERE r.exercise_id = e.exercise_id) AS requisitos"
                + " FROM exercises e JOIN equipment_types t ON t.equipment_id = e.equipment_id"
                + " WHERE e.exercise_id = 1374");
    }

    private static PlanningProfile perfil(String nivel, List<String> equipamiento, SafetyProfile.Declaradas d) {
        return new PlanningProfile(99L, 30, "FEMALE", new BigDecimal("165"), new BigDecimal("70"), null, nivel,
                "GAIN_MUSCLE", "x", "Ninguna", "GYM", 3, List.of((short) 1, (short) 3, (short) 5), 45,
                equipamiento, List.of(), d);
    }

    private boolean recibe1374(String nivel, List<String> equipamiento, SafetyProfile.Declaradas d) {
        var p = perfil(nivel, equipamiento, d);
        return catalogo.fetchEligibleFor(p, p.maxDifficultyLevel(),
                        SafetyProfile.forAgeLevelAndDeclared(p.age(), nivel, d)).stream()
                .map(CandidateExercise::exerciseId).anyMatch(id -> id == 1374L);
    }

    static final List<String> CON_BARRA = List.of("pull-up bar", "dumbbell");
    static final List<String> SIN_BARRA = List.of("dumbbell", "band");
    static final SafetyProfile.Declaradas LIBRE = SafetyProfile.Declaradas.sinLimitaciones();
    static final SafetyProfile.Declaradas SIN_BRAZOS_ARRIBA = new SafetyProfile.Declaradas(true, true, true, true, false);
    static final SafetyProfile.Declaradas SIN_COLGARSE = new SafetyProfile.Declaradas(true, true, true, false, true);

    @Test
    @Order(1)
    void antesDeLaMigracionLaContencionYaProtege() {
        assertEquals("LOCOMOTION_CARDIO", registro().get("movement_pattern"));
        assertFalse(recibe1374("INTERMEDIATE", SIN_BARRA, LIBRE), "sin barra");
        assertFalse(recibe1374("INTERMEDIATE", CON_BARRA, SIN_BRAZOS_ARRIBA), "sin brazos arriba");
        assertFalse(recibe1374("INTERMEDIATE", CON_BARRA, SIN_COLGARSE), "sin colgarse");
        assertTrue(recibe1374("INTERMEDIATE", CON_BARRA, LIBRE), "con barra y sin restricciones");
        // El dato aun dice alto impacto: un principiante no lo recibe.
        assertFalse(recibe1374("BEGINNER", CON_BARRA, LIBRE));
    }

    @Test
    @Order(2)
    void siElEjercicioNoEsElRevisadoAbortaSinTocarNada() {
        var antes = registro();
        var error = assertThrows(Exception.class, () -> new TransactionTemplate(tm).executeWithoutResult(s -> {
            jdbc.update("UPDATE exercises SET name_en = 'otro' WHERE exercise_id = 1374");
            try {
                jdbc.execute(Files.readString(SCRIPT));
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
        }));
        assertTrue(error.getMessage().contains("no es wind sprints"), error.getMessage());
        assertEquals(antes, registro(), "el guardia tenia que dejarlo intacto");
        assertEquals("wind sprints", jdbc.queryForObject("SELECT name_en FROM exercises WHERE exercise_id = 1374",
                String.class));
    }

    @Test
    @Order(3)
    void aplicaPasaLosDisparadoresYEsIdempotente() throws Exception {
        var antes = registro();
        aplicar();
        var despues = registro();
        assertEquals("TRUNK_FLEXION", despues.get("movement_pattern"));
        assertEquals(true, despues.get("requires_hanging"));
        assertEquals(true, despues.get("requires_overhead"));
        assertEquals(false, despues.get("high_impact"));
        assertEquals("pull-up bar", despues.get("principal"));
        assertEquals("1:pull-up bar", despues.get("requisitos"));
        assertTrue(String.valueOf(despues.get("instructions_es")).startsWith("Cuélgate de una barra"));
        // Lo que NO cambia.
        for (var campo : List.of("difficulty_level", "default_prescription", "is_active", "body_part_id"))
            assertEquals(antes.get(campo), despues.get(campo), campo);
        // Igual que los colgados equivalentes 61 y 62.
        var precedente = jdbc.queryForMap("SELECT movement_pattern, requires_hanging, requires_overhead, high_impact,"
                + " equipment_id FROM exercises WHERE exercise_id = 61");
        var corregido = jdbc.queryForMap("SELECT movement_pattern, requires_hanging, requires_overhead, high_impact,"
                + " equipment_id FROM exercises WHERE exercise_id = 1374");
        assertEquals(precedente, corregido);

        aplicar();
        assertEquals(despues, registro(), "reaplicarla no cambia nada");
    }

    @Test
    @Order(4)
    void despuesDeLaMigracionLosFiltrosDeSeguridadLoVen() {
        assertFalse(recibe1374("INTERMEDIATE", SIN_BARRA, LIBRE), "sin barra");
        assertFalse(recibe1374("INTERMEDIATE", CON_BARRA, SIN_BRAZOS_ARRIBA), "sin brazos arriba");
        assertFalse(recibe1374("INTERMEDIATE", CON_BARRA, SIN_COLGARSE), "sin colgarse");
        assertTrue(recibe1374("INTERMEDIATE", CON_BARRA, LIBRE));
        // Ya no es de alto impacto: con barra y sin restricciones, tambien para principiantes.
        assertTrue(recibe1374("BEGINNER", CON_BARRA, LIBRE));
    }
}
