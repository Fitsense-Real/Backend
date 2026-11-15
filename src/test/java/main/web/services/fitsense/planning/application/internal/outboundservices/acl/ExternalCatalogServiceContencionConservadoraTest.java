package main.web.services.fitsense.planning.application.internal.outboundservices.acl;

import main.web.services.fitsense.catalog.interfaces.acl.CatalogContextFacade;
import main.web.services.fitsense.catalog.interfaces.acl.EligibleExerciseView;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanningProfile;
import main.web.services.fitsense.planning.domain.model.valueobjects.SafetyProfile;
import main.web.services.fitsense.planning.domain.services.ContencionConservadora;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * INC-CAT-CONS-v1 en el conjunto elegible (cierre tecnico conservador): identidad no determinada fuera para todos,
 * apoyo suspendido fuera salvo un TRUE explicito del permiso propio (NULL no habilita), 1113 fuera para principiantes.
 * Encendida por defecto; apagada devuelve el conjunto anterior.
 */
@DisplayName("INC-CAT-CONS-v1 · contencion conservadora en el conjunto elegible")
class ExternalCatalogServiceContencionConservadoraTest {

    private static EligibleExerciseView vista(long id) {
        return new EligibleExerciseView(id, "ejercicio " + id, "chest", "body weight", "pectorals",
                (short) 1, "SETS_REPS", null, null, null, "HORIZONTAL_PUSH");
    }

    private static PlanningProfile perfil(String nivel, Boolean apoyoSuspendido) {
        return new PlanningProfile(9L, 41, "MALE", new BigDecimal("180"), new BigDecimal("80"), null,
                nivel, "GENERAL_FITNESS", "x", "Ninguna", "GYM", 3, List.of((short) 1, (short) 3, (short) 5), 60,
                List.of("body weight", "parallel bars"), List.of(),
                new SafetyProfile.Declaradas(true, true, true, true, true, apoyoSuspendido));
    }

    private static List<Long> elegibles(PlanningProfile perfil, boolean contencion) {
        var fachada = Mockito.mock(CatalogContextFacade.class);
        Mockito.when(fachada.fetchEligibleExercises(anyList(), anyBoolean(), anyInt(), anyList(),
                        anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(),
                        anyBoolean(), anyBoolean(), anyBoolean()))
                .thenReturn(List.of(vista(320), vista(457), vista(1113), vista(72), vista(1311)));
        var servicio = new ExternalCatalogService(fachada);
        servicio.setContencionConservadora(contencion);
        var normal = servicio.fetchEligibleFor(perfil, 3, SafetyProfile.none()).stream().map(c -> c.exerciseId()).toList();
        var silenciosa = servicio.fetchEligibleForExplanation(perfil, 3, SafetyProfile.none()).stream()
                .map(c -> c.exerciseId()).toList();
        assertEquals(normal, silenciosa, "la consulta de escasez aplica la misma contencion");
        return normal;
    }

    @Test
    @DisplayName("principiante sin confirmacion: solo queda el 72")
    void principiante() {
        assertEquals(List.of(72L), elegibles(perfil("BEGINNER", null), true));
    }

    @Test
    @DisplayName("intermedio: vuelve el 1113; el apoyo suspendido sigue fuera con NULL y con false")
    void intermedio() {
        assertEquals(List.of(1113L, 72L), elegibles(perfil("INTERMEDIATE", null), true));
        assertEquals(List.of(1113L, 72L), elegibles(perfil("INTERMEDIATE", false), true));
    }

    @Test
    @DisplayName("solo un TRUE explicito (V53-B confirmado) habilita el apoyo suspendido; la identidad sigue fuera")
    void confirmado() {
        assertEquals(List.of(457L, 1113L, 72L), elegibles(perfil("INTERMEDIATE", true), true));
    }

    @Test
    @DisplayName("apagada: el conjunto elegible es el anterior, sin ninguna contencion conservadora")
    void apagada() {
        assertEquals(List.of(320L, 457L, 1113L, 72L, 1311L), elegibles(perfil("BEGINNER", null), false));
    }

    @Test
    @DisplayName("listas: 19 de identidad, 15 de apoyo suspendido, 1113; sin solapes con INC-4C-03")
    void listas() {
        assertEquals(19, ContencionConservadora.IDENTIDAD_NO_DETERMINADA.size());
        assertFalse(ContencionConservadora.IDENTIDAD_NO_DETERMINADA.contains(853L), "853 recuperado por V56");
        assertEquals(15, ContencionConservadora.APOYO_SUSPENDIDO.size());
        assertEquals(java.util.Set.of(1113L), ContencionConservadora.DIFICULTAD_EN_DUDA);
        for (long retenido : List.of(813L, 858L, 861L, 873L, 885L, 1192L, 1294L, 1298L))
            assertTrue(ExternalCatalogService.retenidoInc4C03(retenido), "sigue retenido: " + retenido);
        assertTrue(java.util.Collections.disjoint(ContencionConservadora.IDENTIDAD_NO_DETERMINADA,
                ContencionConservadora.APOYO_SUSPENDIDO));
    }

    @Test
    @DisplayName("las causas son de seguridad: autorizan la sustitucion obligatoria SUST-SEG-1 en la semana derivada")
    void causas() {
        for (var c : List.of(ContencionConservadora.CAUSA_IDENTIDAD, ContencionConservadora.CAUSA_APOYO_SUSPENDIDO,
                ContencionConservadora.CAUSA_DIFICULTAD)) {
            assertTrue(main.web.services.fitsense.planning.domain.services.SafetyConflictPolicy.esMotivoDeSeguridad(c), c);
            assertTrue(main.web.services.fitsense.planning.domain.services.SafetyConflictPolicy.esMotivoDeSustitucion(c), c);
        }
    }
}
