package main.web.services.fitsense;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Arranque REAL de Spring Boot: contexto completo, Flyway aplicando V1..V33
 * sobre un PostgreSQL vacio y Hibernate validando las entidades contra el
 * esquema que resulta ({@code ddl-auto=validate}).
 * <p>
 * Antes era {@code FitsenseApplicationTests} y corria dentro de {@code mvn test}
 * con la base de {@code application.properties}: Neon de PRODUCCION, con Flyway
 * activo. Bastaba tener {@code DB_PASSWORD} en el entorno para que una prueba
 * unitaria migrara produccion.
 * <p>
 * Ahora la base es un contenedor efimero de Testcontainers y solo se ejecuta de
 * forma explicita: {@code mvn verify -Pintegration}. Sin Docker FALLA, no se
 * salta: un arranque no comprobado no cuenta como verde.
 * <p>
 * {@code @ServiceConnection} gana a {@code spring.datasource.*}, asi que ni
 * application.properties ni la URL bloqueada que impone Maven deciden la base.
 * Aun asi se comprueba al final: que la conexion sea la del contenedor.
 */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token="
})
class FitsenseApplicationIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    DataSource dataSource;

    @Autowired
    Flyway flyway;

    @Test
    void contextLoads() throws Exception {
        try (var connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();
            assertFalse(url.contains("neon.tech"), "La prueba no debe tocar Neon: " + url);
            assertEquals(postgres.getJdbcUrl(), url);
        }

        var info = flyway.info();
        assertTrue(info.applied().length > 0, "Flyway no aplico ninguna migracion");
        assertEquals(0, info.pending().length, "Quedaron migraciones pendientes");
        for (var migracion : info.applied())
            assertTrue(migracion.getState().isApplied() && !migracion.getState().isFailed(),
                    "Migracion fallida: V" + migracion.getVersion());
    }
}
