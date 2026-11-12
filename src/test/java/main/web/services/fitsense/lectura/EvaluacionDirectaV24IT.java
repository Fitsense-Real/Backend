package main.web.services.fitsense.lectura;

import main.web.services.fitsense.execution.infrastructure.scheduling.StaleSessionScheduler;
import main.web.services.fitsense.orchestration.infrastructure.scheduling.WeeklyCycleScheduler;
import main.web.services.fitsense.planning.infrastructure.scheduling.OverdueWorkoutScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * LA MISMA EVALUACION QUE {@link EvaluacionLecturaV24IT}, SIN DOCKER: directa
 * contra la base (Neon), con la conexion en SOLO LECTURA impuesta por el
 * servidor.
 * <p>
 * Tres cerrojos:
 * <ul>
 *   <li>la URL lleva {@code options=-c default_transaction_read_only=on}: cada
 *       transaccion de la sesion es de solo lectura en PostgreSQL, y cualquier
 *       INSERT, UPDATE o DELETE falla;</li>
 *   <li>{@code readOnlyMode=always} y Hikari {@code read-only}: el driver tambien
 *       la marca de solo lectura;</li>
 *   <li>Flyway apagado (no migra ni valida escribiendo) y las tareas programadas
 *       sustituidas por simulacros: el ciclo semanal no corre.</li>
 * </ul>
 * La primera comprobacion del test es que el servidor confirma
 * {@code transaction_read_only = on}; si no, se detiene sin evaluar. Requiere la
 * V34 ya aplicada en la base (el modelo la lee). No llama a Replicate.
 * <p>
 * Uso (sin Docker):
 * <pre>
 * mvnw verify -Pintegration -Dtest=NoUnit -Dsurefire.failIfNoSpecifiedTests=false \
 *   -Dit.test=EvaluacionDirectaV24IT \
 *   -Dfitsense.lectura.directa.url='jdbc:postgresql://HOST/DB?sslmode=require' \
 *   -Dfitsense.lectura.usuario=USUARIO -Dfitsense.lectura.clave=CLAVE
 * </pre>
 */
@EnabledIfSystemProperty(named = "fitsense.lectura.directa.url", matches = "jdbc:postgresql://.+")
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "spring.flyway.enabled=false",
        "spring.datasource.hikari.read-only=true",
        "spring.datasource.hikari.maximum-pool-size=2",
        // Solo para que el ensamblador calcule las dosis con linaje; los productores
        // A y B se construyen en la evaluacion con sus propios interruptores.
        "fitsense.adaptation.progression.enabled=true"
})
@DisplayName("Lectura directa (sin Docker) · planes existentes frente a la V24 por capacidades")
class EvaluacionDirectaV24IT extends EvaluacionV24Base {

    @DynamicPropertySource
    static void base(DynamicPropertyRegistry r) {
        String url = System.getProperty("fitsense.lectura.directa.url");
        String sep = url.contains("?") ? "&" : "?";
        r.add("spring.datasource.url", () -> url + sep
                + "readOnly=true&readOnlyMode=always&options=-c%20default_transaction_read_only%3Don");
        r.add("spring.datasource.username", () -> System.getProperty("fitsense.lectura.usuario"));
        r.add("spring.datasource.password", () -> System.getProperty("fitsense.lectura.clave"));
    }

    @MockitoBean WeeklyCycleScheduler cicloProgramado;
    @MockitoBean OverdueWorkoutScheduler vencidas;
    @MockitoBean StaleSessionScheduler sesionesAbandonadas;

    @Test
    void evaluar() throws Exception {
        // Antes de nada: el servidor confirma que esta sesion no puede escribir.
        assertEquals("on", jdbc.queryForObject("SHOW default_transaction_read_only", String.class),
                "la sesion no quedo en solo lectura: no se evalua nada");
        assertEquals("on", jdbc.queryForObject("SHOW transaction_read_only", String.class));
        String version = jdbc.queryForObject("SELECT version FROM flyway_schema_history WHERE success"
                + " AND version IS NOT NULL ORDER BY installed_rank DESC LIMIT 1", String.class);
        var filas = evaluarTodos();
        escribir(filas, "Fuente (directa, solo lectura): Flyway " + version);
    }
}
