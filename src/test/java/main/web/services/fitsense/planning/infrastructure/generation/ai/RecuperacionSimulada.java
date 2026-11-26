package main.web.services.fitsense.planning.infrastructure.generation.ai;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * RT · Las consultas GRATUITAS con que el cliente busca una prediccion cuyo envio quedo incierto
 * ({@code GET /v1/predictions}, una vez por ronda). Las pruebas que antes esperaban "incierto = fallar sin mas" las
 * declaran ahora: Replicate no tiene ninguna prediccion con esa entrada y el desenlace sigue siendo el de antes (fallo
 * del proveedor, sin reenviar).
 */
final class RecuperacionSimulada {

    private RecuperacionSimulada() {
    }

    /** El listado, vacio, en las {@link ReplicateTrainingPlanGenerator#RECUPERACION_RONDAS} rondas. */
    static void sinCoincidencia(MockRestServiceServer server, String listado) {
        server.expect(ExpectedCount.times(ReplicateTrainingPlanGenerator.RECUPERACION_RONDAS), requestTo(listado))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"next\":null,\"results\":[]}", MediaType.APPLICATION_JSON));
    }
}
