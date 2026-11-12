package main.web.services.fitsense.planning.application.internal.queryservices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("S-MAX en /plan/limitations: aviso solo cuando el tope del nivel recorta lo pedido")
class PlanLimitationsFrecuenciaTest {

    @ParameterizedTest(name = "{0} pide {1}: aviso={2}")
    @CsvSource({"BEGINNER,4,false", "BEGINNER,5,true", "BEGINNER,7,true", "INTERMEDIATE,6,false",
            "INTERMEDIATE,7,true", "ADVANCED,5,false", "ADVANCED,7,true"})
    void aviso(String nivel, int pedidos, boolean aviso) {
        var l = PlanLimitationsService.frecuencia(pedidos, nivel);
        assertEquals(aviso, l.isPresent());
        l.ifPresent(x -> {
            assertEquals("FREQUENCY", x.type());
            assertEquals("S_MAX", x.code());
            int sesiones = "BEGINNER".equals(nivel) ? 4 : 6;
            assertTrue(x.message().contains(pedidos + " días") && x.message().contains(sesiones + " sesiones")
                    && x.message().contains("100 %"), x.message());
        });
    }
}
