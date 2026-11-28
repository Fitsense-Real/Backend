package main.web.services.fitsense;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V60-V63 sobre un esquema con cuentas existentes (V59): la secuencia de P-xxx continua tras el mayor codigo, las
 * cuentas conservan sus JWT (token_version 0) y los consentimientos ya registrados pasan a user_consents con su fecha,
 * version y retirada. Sin Spring, PostgreSQL efimero, datos sinteticos.
 */
@Testcontainers
@DisplayName("V60-V63 · migracion de cuentas existentes")
class MigracionCuentasV60aV63IT {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    private Flyway flyway(String target) {
        var c = Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration").placeholderReplacement(false);
        if (target != null) c = c.target(target);
        return c.load();
    }

    @Test
    @DisplayName("participantes, retirados y no participantes existentes")
    void conCuentasExistentes() {
        var jdbc = new JdbcTemplate(new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(),
                postgres.getPassword()));
        assertTrue(postgres.getJdbcUrl().startsWith("jdbc:postgresql://localhost:"), "solo base efimera local");
        flyway("59").migrate();
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, consent_version,"
                + " is_study_participant, participant_code, enrolled_at) VALUES ('a@pruebas.invalid', 'x', 'A',"
                + " '2026-10-01T10:00:00Z', 'MVP-1.0', true, 'P-003', '2026-10-01T10:00:00Z')");
        jdbc.update("INSERT INTO users (email, password_hash, first_name, consent_granted_at, consent_version,"
                + " is_study_participant, participant_code, enrolled_at, withdrawn_at) VALUES ('b@pruebas.invalid', 'x',"
                + " 'B', '2026-10-02T10:00:00Z', 'MVP-1.0', true, 'P-007', '2026-10-02T10:00:00Z',"
                + " '2026-10-20T10:00:00Z')");
        jdbc.update("INSERT INTO users (email, password_hash, first_name) VALUES ('c@pruebas.invalid', 'x', 'C')");

        // Hasta V63 exactamente: esta prueba es de V60-V63; las migraciones posteriores tienen la suya.
        var r = flyway("63").migrate();
        assertEquals("63", r.targetSchemaVersion);
        assertEquals(8L, jdbc.queryForObject("SELECT nextval('participant_code_seq')", Long.class),
                "V60: el siguiente codigo sigue al mayor existente (P-007)");
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM users WHERE token_version <> 0", Integer.class),
                "V61: los JWT emitidos antes siguen validos");
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM account_deletions", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM account_deletion_tokens", Integer.class));

        var filas = jdbc.queryForList("SELECT u.email, c.consent_type, c.document_version, c.granted_at, c.source,"
                + " c.affirmative_action, c.withdrawn_at FROM user_consents c JOIN users u USING (user_id) ORDER BY 1");
        assertEquals(2, filas.size(), "solo las dos participaciones: " + filas);
        assertEquals("a@pruebas.invalid", filas.get(0).get("email"));
        assertEquals("STUDY_PARTICIPATION", filas.get(0).get("consent_type"));
        assertEquals("MVP-1.0", filas.get(0).get("document_version"));
        assertEquals("MIGRACION", filas.get(0).get("source"));
        assertEquals("REGISTRO_ANTERIOR_A_V63", filas.get(0).get("affirmative_action"));
        assertNull(filas.get(0).get("withdrawn_at"));
        assertNotNull(filas.get(1).get("withdrawn_at"), "la retirada existente se conserva");

        var otra = flyway("63");
        assertEquals(0, otra.migrate().migrationsExecuted, "reinicio: nada que aplicar");
        assertDoesNotThrow(otra::validate);
    }
}
