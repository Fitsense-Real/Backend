package main.web.services.fitsense.shared.domain.model.valueobjects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("S-MAX: sesiones planificadas = min(dias pedidos, tope del nivel, dias disponibles)")
class FrecuenciaSemanalTest {

    @ParameterizedTest(name = "{0} pide {1} con {2} disponibles -> {3}")
    @CsvSource({
            "BEGINNER,1,1,1", "BEGINNER,2,2,2", "BEGINNER,3,3,3", "BEGINNER,4,4,4", "BEGINNER,5,5,4",
            "BEGINNER,6,6,4", "BEGINNER,7,7,4", "BEGINNER,3,7,3", "BEGINNER,5,7,4",
            "INTERMEDIATE,1,1,1", "INTERMEDIATE,4,4,4", "INTERMEDIATE,5,5,5", "INTERMEDIATE,6,6,6",
            "INTERMEDIATE,7,7,6", "INTERMEDIATE,2,7,2",
            "ADVANCED,1,1,1", "ADVANCED,5,5,5", "ADVANCED,6,6,6", "ADVANCED,7,7,6", "ADVANCED,4,7,4",
            // dias disponibles por debajo de lo pedido (el perfil no lo admite, pero la funcion no lo amplia)
            "ADVANCED,5,3,3"})
    void planificadas(String nivel, int pedidos, int disponibles, int esperadas) {
        assertEquals(esperadas, FrecuenciaSemanal.planificadas(pedidos, nivel, disponibles));
        assertTrue(FrecuenciaSemanal.planificadas(pedidos, nivel, disponibles) < 7, "nadie recibe 7 sesiones");
        assertEquals(pedidos > FrecuenciaSemanal.tope(nivel), FrecuenciaSemanal.recortaPorNivel(pedidos, nivel));
    }

    @ParameterizedTest(name = "tope {0} = {1}")
    @CsvSource({"BEGINNER,4", "INTERMEDIATE,6", "ADVANCED,6"})
    void topes(String nivel, int tope) {
        assertEquals(tope, FrecuenciaSemanal.tope(nivel), "los topes de S-MAX no cambian");
    }
}
