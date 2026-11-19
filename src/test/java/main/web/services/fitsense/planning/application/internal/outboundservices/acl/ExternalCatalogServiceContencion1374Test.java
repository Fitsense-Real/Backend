package main.web.services.fitsense.planning.application.internal.outboundservices.acl;

import main.web.services.fitsense.catalog.interfaces.acl.CatalogContextFacade;
import main.web.services.fitsense.catalog.interfaces.acl.EligibleExerciseView;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanningProfile;
import main.web.services.fitsense.planning.domain.model.valueobjects.SafetyProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;

/**
 * INC-CAT-1374: el 1374 solo llega a quien cumple los requisitos REALES del
 * ejercicio del GIF -barra declarada, colgarse y brazos arriba permitidos-,
 * aunque su registro todavia no los declare.
 */
class ExternalCatalogServiceContencion1374Test {

    private static EligibleExerciseView vista(long id) {
        return new EligibleExerciseView(id, "ejercicio " + id, "waist", "body weight", "abs",
                (short) 1, "SETS_REPS", null, null, null, "LOCOMOTION_CARDIO");
    }

    private static PlanningProfile perfil(List<String> equipamiento) {
        return new PlanningProfile(9L, 30, "MALE", new BigDecimal("175"), new BigDecimal("72"), null,
                "INTERMEDIATE", "GAIN_MUSCLE", "x", "Ninguna", "GYM", 3,
                List.of((short) 1, (short) 3, (short) 5), 45, equipamiento, List.of(),
                SafetyProfile.Declaradas.sinLimitaciones());
    }

    private static SafetyProfile seguridad(boolean sinColgarse, boolean sinBrazosArriba) {
        return new SafetyProfile(false, false, false, false, sinColgarse, sinBrazosArriba, false);
    }

    private static List<Long> elegibles(List<String> equipamiento, SafetyProfile seguridad) {
        var fachada = Mockito.mock(CatalogContextFacade.class);
        Mockito.when(fachada.fetchEligibleExercises(anyList(), anyBoolean(), anyInt(), anyList(),
                        anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(),
                        anyBoolean(), anyBoolean(), anyBoolean()))
                .thenReturn(List.of(vista(1374), vista(1117), vista(61)));
        var servicio = new ExternalCatalogService(fachada);
        var normal = servicio.fetchEligibleFor(perfil(equipamiento), 2, seguridad).stream()
                .map(c -> c.exerciseId()).toList();
        // La consulta silenciosa de los recuentos de ESCASEZ aplica la misma contencion.
        var silenciosa = servicio.fetchEligibleForExplanation(perfil(equipamiento), 2, seguridad).stream()
                .map(c -> c.exerciseId()).toList();
        assertEquals(normal, silenciosa);
        return normal;
    }

    @ParameterizedTest(name = "barra={0} sinColgarse={1} sinBrazosArriba={2} -> 1374 {3}")
    @CsvSource({
            "false, false, false, CONTENIDO",
            "false, true,  true,  CONTENIDO",
            "true,  true,  false, CONTENIDO",
            "true,  false, true,  CONTENIDO",
            "true,  true,  true,  CONTENIDO",
            "true,  false, false, ADMITIDO"
    })
    void soloLlegaAQuienCumpleLosRequisitosReales(boolean barra, boolean sinColgarse,
                                                  boolean sinBrazosArriba, String esperado) {
        var equipamiento = barra ? List.of("body weight", "pull-up bar") : List.of("body weight", "dumbbell");
        var ids = elegibles(equipamiento, seguridad(sinColgarse, sinBrazosArriba));
        assertEquals("ADMITIDO".equals(esperado), ids.contains(1374L), ids.toString());
    }

    @Test
    @DisplayName("la contencion solo toca al 1374")
    void noTocaANingunOtro() {
        var ids = elegibles(List.of("body weight"), seguridad(true, true));
        assertEquals(List.of(1117L, 61L), ids);
    }

    @Test
    @DisplayName("el codigo de equipamiento se compara sin mayusculas ni espacios")
    void codigoTolerante() {
        assertTrue(elegibles(List.of(" Pull-Up Bar "), seguridad(false, false)).contains(1374L));
    }

    @Test
    @DisplayName("sin equipamiento declarado, contenido")
    void sinEquipamiento() {
        var fachada = Mockito.mock(CatalogContextFacade.class);
        Mockito.when(fachada.fetchEligibleExercises(Mockito.any(), anyBoolean(), anyInt(), anyList(),
                        anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(),
                        anyBoolean(), anyBoolean(), anyBoolean()))
                .thenReturn(List.of(vista(1374)));
        var ids = new ExternalCatalogService(fachada)
                .fetchEligibleFor(perfil(null), 2, seguridad(false, false));
        assertTrue(ids.isEmpty());
    }
}
