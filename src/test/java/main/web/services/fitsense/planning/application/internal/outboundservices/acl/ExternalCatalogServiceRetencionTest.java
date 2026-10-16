package main.web.services.fitsense.planning.application.internal.outboundservices.acl;

import main.web.services.fitsense.catalog.interfaces.acl.CatalogContextFacade;
import main.web.services.fitsense.catalog.interfaces.acl.EligibleExerciseView;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanningProfile;
import main.web.services.fitsense.planning.domain.model.valueobjects.SafetyProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;

/**
 * INC-4C-03. Comprueba que los siete ejercicios retenidos NO salen del conjunto
 * elegible, y que la retencion no se lleva nada mas por delante.
 * <p>
 * Es la prueba que convierte la retencion en algo que ocurre en el backend y no
 * solo en un documento: si alguien quita un id de la constante sin revisarlo,
 * esto se pone rojo.
 */
class ExternalCatalogServiceRetencionTest {

    /** Los tres de la auditoria del bloque 7 y los cuatro de 4D v2 §4. */
    private static final List<Long> RETENIDOS =
            List.of(813L, 858L, 861L, 873L, 885L, 1192L, 1298L);

    private static EligibleExerciseView vista(long id, String nombre) {
        return new EligibleExerciseView(id, nombre, "back", "body weight", "lats",
                (short) 1, "SETS_REPS", null, null, null, "VERTICAL_PULL");
    }

    private static PlanningProfile perfil() {
        return new PlanningProfile(9L, 30, "MALE",
                new BigDecimal("175"), new BigDecimal("72"), new BigDecimal("75"),
                "BEGINNER", "GAIN_MUSCLE", "Ganar masa", "Ninguna", "HOME",
                3, List.of((short) 1, (short) 3, (short) 5), 45,
                List.of("body weight", "dumbbell", "band"), List.of());
    }

    private static ExternalCatalogService conCatalogo(List<EligibleExerciseView> vistas) {
        var fachada = Mockito.mock(CatalogContextFacade.class);
        Mockito.when(fachada.fetchEligibleExercises(anyList(), anyBoolean(), anyInt(), anyList(),
                        anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(),
                        anyBoolean(), anyBoolean(), anyBoolean()))
                .thenReturn(vistas);
        return new ExternalCatalogService(fachada);
    }

    private static List<Long> idsElegibles(List<EligibleExerciseView> vistas) {
        return conCatalogo(vistas)
                .fetchEligibleFor(perfil(), 1, SafetyProfile.none())
                .stream()
                .map(c -> c.exerciseId())
                .toList();
    }

    @Test
    @DisplayName("ninguno de los siete retenidos llega al conjunto elegible")
    void retieneLosSiete() {
        var catalogo = RETENIDOS.stream().map(id -> vista(id, "retenido " + id)).toList();

        var resultado = idsElegibles(catalogo);

        assertTrue(resultado.isEmpty(),
                "el catalogo solo tenia retenidos y el resultado deberia ir vacio, y fue " + resultado);
    }

    @Test
    @DisplayName("cada id retenido se comprueba por separado")
    void retieneCadaUnoPorSeparado() {
        for (var retenido : RETENIDOS) {
            var resultado = idsElegibles(List.of(
                    vista(retenido, "retenido " + retenido),
                    vista(500L, "ejercicio normal")));

            assertFalse(resultado.contains(retenido),
                    "el ejercicio " + retenido + " esta retenido por INC-4C-03 y no debe ser elegible");
            assertTrue(resultado.contains(500L),
                    "la retencion de " + retenido + " no debe arrastrar a otros ejercicios");
        }
    }

    @Test
    @DisplayName("un ejercicio no retenido pasa igual que antes")
    void noRetieneLoDemas() {
        var resultado = idsElegibles(List.of(
                vista(500L, "Sentadilla"),
                vista(501L, "Remo con banda"),
                vista(502L, "Puente de glúteo")));

        assertEquals(List.of(500L, 501L, 502L), resultado);
    }

    @Test
    @DisplayName("la retencion convive con los dos filtros que ya habia")
    void convivicConLosFiltrosAnteriores() {
        var catalogo = List.of(
                vista(1192L, "Skin the cat"),                       // retenido
                new EligibleExerciseView(600L, "Elevacion de cuello", "neck",
                        "body weight", "traps", (short) 1, "SETS_REPS",
                        null, null, null, "OTHER"),                 // fuera de foco
                vista(601L, "Estiramiento de isquiotibiales"),      // nombre con estiramiento
                vista(500L, "Sentadilla"));                         // el unico que sobrevive

        assertEquals(List.of(500L), idsElegibles(catalogo));
    }

    @Test
    @DisplayName("el catalogo vacio no rompe la retencion")
    void catalogoVacio() {
        assertTrue(idsElegibles(List.of()).isEmpty());
    }

    @Test
    @DisplayName("un nombre nulo no rompe el filtro de estiramiento")
    void nombreNulo() {
        var resultado = idsElegibles(List.of(vista(500L, null), vista(1192L, null)));

        assertEquals(List.of(500L), resultado,
                "el nombre nulo debe pasar el filtro de estiramiento, y 1192 seguir retenido");
    }
}
