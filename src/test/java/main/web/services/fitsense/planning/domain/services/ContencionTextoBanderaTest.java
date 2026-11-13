package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.ExerciseSafetyFacts;
import main.web.services.fitsense.planning.domain.model.valueobjects.SafetyProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * INC-CAT-C39 · contencion por texto del catalogo contra bandera (auditoria del caso 39).
 * Dominio puro: que ids se contienen para que restricciones declaradas, y como entran en las causas de seguridad.
 */
@DisplayName("INC-CAT-C39 · contencion por texto contra bandera")
class ContencionTextoBanderaTest {

    static final Set<Long> COLGARSE = Set.of(316L, 1301L, 1306L, 1153L, 1303L, 1304L, 1152L);
    static final Set<Long> CABEZA = Set.of(316L, 1301L, 131L, 400L, 448L, 1272L, 1309L, 1322L);
    static final Set<Long> SUELO = Set.of(55L, 224L, 637L, 741L, 875L, 1062L, 1063L, 1093L, 1095L, 1156L, 1181L,
            1193L, 1303L, 1304L, 1311L);

    /** (alto impacto, suelo, axial, rodillas, colgarse, brazos arriba, inversion) */
    static SafetyProfile seg(boolean suelo, boolean colgarse, boolean cabeza) {
        return new SafetyProfile(false, suelo, false, false, colgarse, cabeza, false);
    }

    static Set<Long> union(Set<Long> a, Set<Long> b) {
        var u = new TreeSet<>(a);
        u.addAll(b);
        return u;
    }

    @Test
    @DisplayName("sin restricciones: no se contiene nada (los demas perfiles reciben el ejercicio igual)")
    void sinRestricciones() {
        assertTrue(ContencionTextoBandera.contenidos(SafetyProfile.none()).isEmpty());
        assertTrue(ContencionTextoBandera.contenidos(seg(false, false, false)).isEmpty());
        assertFalse(ContencionTextoBandera.contiene(316L, seg(false, false, false)));
        assertFalse(ContencionTextoBandera.contiene(316L, null));
    }

    @Test
    @DisplayName("A · quien prohibe colgarse: exactamente los ids cuyo texto pide colgarse o apoyo suspendido")
    void prohibeColgarse() {
        assertEquals(new TreeSet<>(COLGARSE), ContencionTextoBandera.contenidos(seg(false, true, false)));
    }

    @Test
    @DisplayName("B · quien prohibe brazos sobre la cabeza: exactamente los ids cuyo texto los pide")
    void prohibeSobreLaCabeza() {
        assertEquals(new TreeSet<>(CABEZA), ContencionTextoBandera.contenidos(seg(false, false, true)));
    }

    @Test
    @DisplayName("C · quien prohibe ambas: la union, sin perder ninguno")
    void prohibeAmbas() {
        assertEquals(union(COLGARSE, CABEZA), ContencionTextoBandera.contenidos(seg(false, true, true)));
    }

    @Test
    @DisplayName("quien prohibe el suelo: los relajados en V38 cuyas instrucciones siguen en el suelo")
    void prohibeSuelo() {
        assertEquals(new TreeSet<>(SUELO), ContencionTextoBandera.contenidos(seg(true, false, false)));
    }

    @Test
    @DisplayName("restricciones que el registro no lee (impacto, axial, rodillas, inversion) no contienen nada")
    void otrasRestricciones() {
        assertTrue(ContencionTextoBandera.contenidos(new SafetyProfile(true, false, true, true, false, false, true))
                .isEmpty());
    }

    @Test
    @DisplayName("trazabilidad: cada entrada tiene restricciones y una contradiccion citada")
    void trazabilidad() {
        assertEquals(26, ContencionTextoBandera.registro().size(), "26 ejercicios documentados en el registro");
        ContencionTextoBandera.registro().forEach((id, c) -> {
            assertFalse(c.restricciones().isEmpty(), "sin restricciones: " + id);
            assertTrue(c.contradiccion() != null && c.contradiccion().length() > 20, "sin justificacion: " + id);
        });
        assertEquals(union(union(COLGARSE, CABEZA), SUELO), new TreeSet<>(ContencionTextoBandera.registro().keySet()));
    }

    @Test
    @DisplayName("no se solapa con INC-4C-03 (1298 ya retenido para todos) ni con INC-CAT-1374")
    void sinSolapes() {
        assertFalse(ContencionTextoBandera.registro().containsKey(1298L));
        assertFalse(ContencionTextoBandera.registro().containsKey(1374L));
    }

    @Test
    @DisplayName("causa de seguridad con nombre: autoriza bloqueo y sustitucion como INC-CAT-1374")
    void causaDeSeguridad() {
        var hechos = new ExerciseSafetyFacts(316L, true, false, true, false, false, false, false, false);
        var causas = SafetyConflictPolicy.causas(hechos, seg(false, true, false), false, false, true);
        assertEquals(java.util.List.of(SafetyConflictPolicy.CONTENIDO_INC_CAT_C39), causas);
        assertEquals("INC-CAT-C39", SafetyConflictPolicy.CONTENIDO_INC_CAT_C39);
        assertTrue(SafetyConflictPolicy.CAUSAS.contains("INC-CAT-C39"));
        assertTrue(SafetyConflictPolicy.esMotivoDeSeguridad("INC-CAT-C39"));
        assertTrue(SafetyConflictPolicy.esMotivoDeSustitucion("INC-CAT-C39"));
        // La firma anterior (cuatro argumentos) no cambia su resultado.
        assertTrue(SafetyConflictPolicy.causas(hechos, seg(false, true, false), false, false).isEmpty());
    }
}
