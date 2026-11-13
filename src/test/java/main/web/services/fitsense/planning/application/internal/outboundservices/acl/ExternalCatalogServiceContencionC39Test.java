package main.web.services.fitsense.planning.application.internal.outboundservices.acl;

import main.web.services.fitsense.catalog.interfaces.acl.CatalogContextFacade;
import main.web.services.fitsense.catalog.interfaces.acl.EligibleExerciseView;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanningProfile;
import main.web.services.fitsense.planning.domain.model.valueobjects.SafetyProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * INC-CAT-C39 en el conjunto elegible: el mismo filtro para la consulta normal (motor de reglas e IA) y para la
 * silenciosa de los recuentos de escasez, y solo para los perfiles que declaran la restriccion.
 */
@DisplayName("INC-CAT-C39 · filtro en el conjunto elegible")
class ExternalCatalogServiceContencionC39Test {

    private static EligibleExerciseView vista(long id) {
        return new EligibleExerciseView(id, "ejercicio " + id, "back", "body weight", "lats",
                (short) 1, "SETS_REPS", null, null, null, "HORIZONTAL_PULL");
    }

    private static PlanningProfile perfil() {
        return new PlanningProfile(9L, 41, "MALE", new BigDecimal("180"), new BigDecimal("100"), null,
                "BEGINNER", "GENERAL_FITNESS", "x", "Ninguna", "GYM", 4,
                List.of((short) 1, (short) 4, (short) 5, (short) 7), 60, List.of("body weight", "pull-up bar"),
                List.of(), SafetyProfile.Declaradas.sinLimitaciones());
    }

    private static List<Long> elegibles(SafetyProfile seguridad) {
        var fachada = Mockito.mock(CatalogContextFacade.class);
        Mockito.when(fachada.fetchEligibleExercises(anyList(), anyBoolean(), anyInt(), anyList(),
                        anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(),
                        anyBoolean(), anyBoolean(), anyBoolean()))
                .thenReturn(List.of(vista(1301), vista(400), vista(1093), vista(910), vista(61)));
        var servicio = new ExternalCatalogService(fachada);
        var normal = servicio.fetchEligibleFor(perfil(), 1, seguridad).stream().map(c -> c.exerciseId()).toList();
        var silenciosa = servicio.fetchEligibleForExplanation(perfil(), 1, seguridad).stream()
                .map(c -> c.exerciseId()).toList();
        assertEquals(normal, silenciosa, "la consulta de escasez aplica la misma contencion");
        return normal;
    }

    @ParameterizedTest(name = "suelo={0} colgarse={1} cabeza={2} -> {3}")
    @CsvSource(delimiter = ';', value = {
            "false;false;false;1301,400,1093,910,61",
            "false;true;false;400,1093,910,61",
            "false;false;true;1301,1093,910,61",
            "false;true;true;1093,910,61",
            "true;false;false;1301,400,1093,910,61",
            "true;true;true;1093,910,61"
    })
    void soloParaQuienDeclaraLaRestriccion(boolean suelo, boolean colgarse, boolean cabeza, String esperado) {
        var seguridad = new SafetyProfile(false, suelo, false, false, colgarse, cabeza, false);
        var ids = elegibles(seguridad);
        assertEquals(java.util.Arrays.stream(esperado.split(",")).map(Long::valueOf).toList(), ids);
        // 910 (remo invertido, talones en el suelo) no esta en el registro: sin incidencia, nunca se contiene.
        assertTrue(ids.contains(910L));
        // 1093 (liberado por V49: su texto ya describe la elevacion de rodilla de pie del GIF) tampoco.
        assertTrue(ids.contains(1093L));
    }

    @ParameterizedTest(name = "contenidoIncCatC39({0}) con colgarse prohibido = {1}")
    @CsvSource({"1301, true", "1303, true", "1304, true", "1306, true", "1153, true", "316, false", "1152, false",
            "853, false", "910, false", "1093, false", "400, false"})
    void fachadaEstatica(long id, boolean esperado) {
        assertEquals(esperado, ExternalCatalogService.contenidoIncCatC39(id,
                new SafetyProfile(false, false, false, false, true, false, false)));
    }
}
