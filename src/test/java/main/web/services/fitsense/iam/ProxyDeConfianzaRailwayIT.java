package main.web.services.fitsense.iam;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * B1.2 · P0-02, compatibilidad con el proxy de Railway: cuando la peticion llega de un proxy de CONFIANZA (los rangos
 * privados y 127/8 que Tomcat admite por omision, que es como llega el trafico de Railway al contenedor), la IP del
 * limite es la de {@code X-Forwarded-For}: dos clientes distintos detras del mismo proxy tienen cupos distintos y uno
 * no bloquea al otro. El caso contrario esta en {@link ProxyDeConfianzaLimitesIT}.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.ai.enabled=false",
        "fitsense.ai.api-token=",
        "server.forward-headers-strategy=native",
        "fitsense.auth-rate-limit.register-per-ip=3"
})
@DisplayName("P0-02 · detras de un proxy de confianza (Railway), el limite es por cliente real")
class ProxyDeConfianzaRailwayIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @LocalServerPort int puerto;

    @Test
    @DisplayName("cliente A agota su cupo (429); el cliente B, detras del mismo proxy, se registra")
    void porClienteReal() throws Exception {
        for (int i = 0; i < 3; i++) assertEquals(201, ProxyDeConfianzaLimitesIT.registrar(puerto, "198.51.100.7"));
        assertEquals(429, ProxyDeConfianzaLimitesIT.registrar(puerto, "198.51.100.7"));
        assertEquals(201, ProxyDeConfianzaLimitesIT.registrar(puerto, "198.51.100.8"));
    }
}
