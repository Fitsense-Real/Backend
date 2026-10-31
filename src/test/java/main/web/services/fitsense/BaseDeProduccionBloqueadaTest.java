package main.web.services.fitsense;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BaseDeProduccionBloqueadaTest {

    @Test
    void leeElHostDeProduccionDelApplicationPropertiesDeLaAplicacion() {
        var host = BaseDeProduccionBloqueada.hostDeProduccion().orElseThrow();
        assertTrue(host.endsWith("neon.tech"), host);
    }

    @Test
    void rechazaCualquierUrlConElHostDeProduccion() {
        var host = BaseDeProduccionBloqueada.hostDeProduccion().orElseThrow();
        var error = assertThrows(IllegalStateException.class, () -> BaseDeProduccionBloqueada
                .comprobar(host, "spring.flyway.url",
                        "jdbc:postgresql://" + host.toUpperCase() + ":5432/otra?sslmode=require"));
        assertTrue(error.getMessage().startsWith("BASE DE PRODUCCION BLOQUEADA"));
    }

    @Test
    void dejaPasarOtrasBasesYValoresNulos() {
        var host = BaseDeProduccionBloqueada.hostDeProduccion().orElseThrow();
        assertDoesNotThrow(() -> BaseDeProduccionBloqueada.comprobar(host, "x",
                "jdbc:postgresql://localhost:54321/test"));
        assertDoesNotThrow(() -> BaseDeProduccionBloqueada.comprobar(host, "x",
                "jdbc:postgresql://otro-host-de-pruebas.neon.tech/neondb"));
        assertDoesNotThrow(() -> BaseDeProduccionBloqueada.comprobar(host, "x", null));
    }

    /**
     * La via de beans, sin ninguna propiedad: es la que cubre @ServiceConnection
     * y cualquier DataSource construido en codigo. Hikari no abre conexion hasta
     * el primer getConnection, asi que esto no sale de la JVM.
     */
    @Test
    void abortaElContextoSiUnDataSourceApuntaAProduccion() {
        var host = BaseDeProduccionBloqueada.hostDeProduccion().orElseThrow();
        try (var context = new org.springframework.context.support.GenericApplicationContext()) {
            new BaseDeProduccionBloqueada().initialize(context);
            context.registerBean("dataSource", javax.sql.DataSource.class, () -> {
                var ds = new com.zaxxer.hikari.HikariDataSource();
                ds.setJdbcUrl("jdbc:postgresql://" + host + "/neondb");
                return ds;
            });
            var error = assertThrows(org.springframework.beans.factory.BeanCreationException.class,
                    context::refresh);
            assertTrue(error.getMostSpecificCause().getMessage().contains("DataSource 'dataSource'"));
        }
    }

    @Test
    void abortaElContextoSiFlywayApuntaAProduccion() {
        var host = BaseDeProduccionBloqueada.hostDeProduccion().orElseThrow();
        try (var context = new org.springframework.context.support.GenericApplicationContext()) {
            new BaseDeProduccionBloqueada().initialize(context);
            context.registerBean("flyway", org.flywaydb.core.Flyway.class, () -> org.flywaydb.core.Flyway
                    .configure().dataSource("jdbc:postgresql://" + host + "/neondb", "u", "p").load());
            var error = assertThrows(org.springframework.beans.factory.BeanCreationException.class,
                    context::refresh);
            assertTrue(error.getMostSpecificCause().getMessage().contains("Flyway"));
        }
    }
}
