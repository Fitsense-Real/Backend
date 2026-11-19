package main.web.services.fitsense.auditoria;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** O12 con la configuracion evaluada y la progresion apagada (todo lo demas por defecto). Ver {@link O12DolorBase}. */
@Testcontainers
@SpringBootTest(properties = {
        "authorization.jwt.secret=clave-solo-para-pruebas-de-integracion-0123456789abcdef",
        "fitsense.adaptation.progression.enabled=false"
})
@DisplayName("Auditoria O12 · dolor con la progresion apagada: lo que dolio no vuelve")
class O12DolorProgresionApagadaIT extends O12DolorBase {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Override
    String condicion() {
        return "progresion_apagada";
    }

    @Test
    @DisplayName("respuesta fija: una serie menos en el jueves del dolor")
    void menosSeries() {
        recorrer(Respuesta.MENOS_SERIES);
    }

    @Test
    @DisplayName("respuesta fija: la IA sube una serie (prohibido)")
    void subeSeries() {
        recorrer(Respuesta.SUBE_SERIES);
    }

    @Test
    @DisplayName("respuesta fija: la IA no propone ninguna sesion")
    void respuestaVacia() {
        recorrer(Respuesta.VACIA);
    }
}
