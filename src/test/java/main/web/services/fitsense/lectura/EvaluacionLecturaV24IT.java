package main.web.services.fitsense.lectura;

import main.web.services.fitsense.planning.application.internal.commandservices.SafetyExclusionResolver;
import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalConfigurationService;
import main.web.services.fitsense.planning.domain.model.commands.GenerateWeeklyPlanCommand;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.*;
import main.web.services.fitsense.planning.infrastructure.generation.ai.ReplicateSessionFixGenerator;
import main.web.services.fitsense.planning.infrastructure.validation.StrictValidationAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * EVALUACION EN SOLO LECTURA DE LOS PLANES EXISTENTES FRENTE A LA V24 POR
 * CAPACIDADES (antes de encender la progresion en Neon).
 * <p>
 * <b>No escribe en la fuente.</b> {@link CopiaDeLectura} la abre en solo
 * lectura (comprobado) y copia los datos a un PostgreSQL efimero local; todo lo
 * demas ocurre en esa copia. No llama a Replicate.
 * <p>
 * Para cada participante, su ultima semana publicada es la semana medida y se
 * simula la COPIA del ciclo siguiente ({@code NO_CHANGE}, sin paso) con:
 * <ul>
 *   <li><b>A</b> · los interruptores apagados: la validacion de hoy;</li>
 *   <li><b>B</b> · progresion encendida: V24 por capacidades en P2-D con las
 *       dosis heredadas de linaje y las reducciones autorizadas.</li>
 * </ul>
 * Lo que importa son las semanas que <b>A publica y B no</b>: encender la
 * progresion las dejaria {@code NO_FACTIBLE}. El informe solo lleva
 * identificadores, objetivo y codigos de regla; ningun dato personal.
 * <p>
 * Uso (lo ejecuta la persona responsable, con credenciales de solo lectura):
 * <pre>
 * mvn -o verify -Pintegration -Dtest=NoUnit -Dsurefire.failIfNoSpecifiedTests=false \
 *   -Dit.test=EvaluacionLecturaV24IT \
 *   -Dfitsense.lectura.url='jdbc:postgresql://HOST/DB?sslmode=require' \
 *   -Dfitsense.lectura.usuario=USUARIO -Dfitsense.lectura.clave=CLAVE
 * </pre>
 * Informe: {@code target/lectura-v24/informe.md}.
 */
@EnabledIfSystemProperty(named = "fitsense.lectura.url", matches = ".+")
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        // Solo para que el ensamblador calcule las dosis con linaje en la copia; los
        // productores A y B se construyen aqui con sus propios interruptores.
        "fitsense.adaptation.progression.enabled=true"
})
@DisplayName("Lectura · planes existentes frente a la V24 por capacidades (A: hoy, B: progresion encendida)")
class EvaluacionLecturaV24IT extends EvaluacionV24Base {

    @ServiceConnection
    static PostgreSQLContainer copia = new PostgreSQLContainer("postgres:16-alpine");

    static CopiaDeLectura.Resultado copiado;

    static {
        String url = System.getProperty("fitsense.lectura.url");
        if (url != null && !url.isBlank()) {
            copia.start();
            try {
                copiado = CopiaDeLectura.copiar(url, System.getProperty("fitsense.lectura.usuario"),
                        System.getProperty("fitsense.lectura.clave"), copia.getJdbcUrl(), copia.getUsername(),
                        copia.getPassword());
            } catch (Exception e) {
                throw new IllegalStateException("No se pudo copiar la fuente en solo lectura: " + e, e);
            }
        }
    }

    @Test
    void evaluar() throws Exception {
        assertFalse(copiado.filas().isEmpty(), "la copia esta vacia");
        var filas = evaluarTodos();
        escribir(filas, "Fuente (copia): Flyway " + copiado.versionFuente() + " · tablas copiadas: "
                + copiado.filas().size() + " · filas de planes: " + copiado.filas().get("weekly_training_plans"));
        // Comprobacion de la propia copia: mismas filas que se leyeron.
        for (var t : List.of("weekly_training_plans", "planned_workouts", "planned_workout_exercises"))
            assertEquals(copiado.filas().get(t), jdbc.queryForObject("SELECT count(*) FROM " + t, Integer.class), t);
    }
}
