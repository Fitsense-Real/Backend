package main.web.services.fitsense.orchestation;

import main.web.services.fitsense.planning.domain.exceptions.PlanGenerationFailedException;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static main.web.services.fitsense.profiling.domain.model.valueobjects.FitnessLevel.INTERMEDIATE;
import static main.web.services.fitsense.profiling.domain.model.valueobjects.GoalType.GAIN_MUSCLE;
import static org.junit.jupiter.api.Assertions.*;

/**
 * REGISTRO (opt-in, {@code -Dfitsense.registro.r0548=true}) del caso R0548, SEPARADO del limite de dificultad: un
 * perfil GAIN_MUSCLE intermedio, casa con mancuernas y bandas, 7 dias, 75 minutos, semana 2026-11-02, usuario 548 (la
 * semilla del motor de reglas es userId x 1.000.003 + semana). La GENERACION INICIAL no consigue un plan valido: V20
 * (las sesiones duran unos 2.550 s y el minimo es 3.150 s, el 70 % de 75 minutos).
 * <p>
 * No es una correccion: fija el comportamiento actual para poder investigarlo aparte. No se toca V20 ni el catalogo.
 */
@Testcontainers
@EnabledIfSystemProperty(named = "fitsense.registro.r0548", matches = "true")
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
@DisplayName("Registro R0548: la generacion inicial (GM intermedio, casa, 7 dias, 75 min) no pasa V20")
class GeneracionR0548V20IT extends SinSustitutoDolorBase {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    void generacionInicialNoPasaV20() {
        long ultimo = jdbc.queryForObject("SELECT COALESCE(MAX(user_id), 0) FROM users", Long.class);
        while (ultimo < 547)
            ultimo = jdbc.queryForObject("INSERT INTO users (email, password_hash, first_name, consent_granted_at,"
                    + " enrolled_at) VALUES (?, 'x', 'Relleno', now(), now()) RETURNING user_id", Long.class,
                    "relleno" + UUID.randomUUID() + "@pruebas.invalid");
        long u = participante(GAIN_MUSCLE, INTERMEDIATE, 7, 75);
        assertEquals(548L, u, "el usuario tiene el id congelado");
        var e = assertThrows(PlanGenerationFailedException.class, () -> planning.generateWeeklyPlan(u,
                SinSustitutoDolor24CasosIT.SEMANA_DEL_BARRIDO, PlanAdjustment.none(), null, null));
        System.out.println("R0548 " + e.getMessage());
        assertTrue(e.getMessage().contains("V20"), e.getMessage());
    }
}
