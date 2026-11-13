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

    /*
     * Contencion reducida a 8 sobre el catalogo integral (V47-V50): solo el MOVIMIENTO pendiente de decision
     * profesional. Los 18 liberados por V49 y la parte de 1301 (cabeza) y 1303/1304 (suelo) ya no se contienen.
     */
    static final Set<Long> COLGARSE = Set.of(1153L, 1301L, 1303L, 1304L, 1306L);
    static final Set<Long> CABEZA = Set.of(131L, 400L, 448L);
    static final Set<Long> SUELO = Set.of();
    /** Los 26 de la PR #55 original. */
    static final Set<Long> ORIGINAL_26 = Set.of(316L, 1301L, 1306L, 1153L, 1303L, 1304L, 1152L, 131L, 400L, 448L,
            1272L, 1309L, 1322L, 55L, 224L, 637L, 741L, 875L, 1062L, 1063L, 1093L, 1095L, 1156L, 1181L, 1193L, 1311L);

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
        assertFalse(ContencionTextoBandera.contiene(1303L, seg(false, false, false)));
        assertFalse(ContencionTextoBandera.contiene(1303L, null));
    }

    @Test
    @DisplayName("A · quien prohibe colgarse: exactamente los ids de apoyo suspendido pendientes de decision")
    void prohibeColgarse() {
        assertEquals(new TreeSet<>(COLGARSE), ContencionTextoBandera.contenidos(seg(false, true, false)));
    }

    @Test
    @DisplayName("B · quien prohibe brazos sobre la cabeza: exactamente los tres curl altos")
    void prohibeSobreLaCabeza() {
        assertEquals(new TreeSet<>(CABEZA), ContencionTextoBandera.contenidos(seg(false, false, true)));
    }

    @Test
    @DisplayName("C · quien prohibe ambas: la union, sin perder ninguno")
    void prohibeAmbas() {
        assertEquals(union(COLGARSE, CABEZA), ContencionTextoBandera.contenidos(seg(false, true, true)));
    }

    @Test
    @DisplayName("quien prohibe el suelo: nada (V49 alineo con el GIF todos los textos de suelo contenidos)")
    void prohibeSuelo() {
        assertEquals(new TreeSet<>(SUELO), ContencionTextoBandera.contenidos(seg(true, false, false)));
        assertFalse(ContencionTextoBandera.contiene(1303L, seg(true, false, false)), "1303 ya no por suelo");
        assertFalse(ContencionTextoBandera.contiene(1093L, seg(true, false, false)), "1093 liberado");
    }

    @Test
    @DisplayName("1301 se contiene solo por colgarse: brazos sobre la cabeza ya no aplica (V49)")
    void parcial1301() {
        assertTrue(ContencionTextoBandera.contiene(1301L, seg(false, true, false)));
        assertFalse(ContencionTextoBandera.contiene(1301L, seg(false, false, true)));
    }

    @Test
    @DisplayName("liberados por V49: 18 ids que no se contienen para ninguna combinacion de restricciones")
    void liberados() {
        assertEquals(18, ContencionTextoBandera.LIBERADOS_POR_V49.size());
        var todos = seg(true, true, true);
        for (var id : ContencionTextoBandera.LIBERADOS_POR_V49) {
            assertFalse(ContencionTextoBandera.registro().containsKey(id), "liberado en el registro: " + id);
            assertFalse(ContencionTextoBandera.contiene(id, todos), "liberado contenido: " + id);
        }
        // 18 liberados + 8 contenidos = los 26 originales; nada nuevo entra en el registro.
        assertEquals(new TreeSet<>(ORIGINAL_26),
                union(ContencionTextoBandera.LIBERADOS_POR_V49, ContencionTextoBandera.registro().keySet()));
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
        assertEquals(8, ContencionTextoBandera.registro().size(), "8 ejercicios documentados en el registro");
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
        var hechos = new ExerciseSafetyFacts(1303L, true, false, true, false, false, false, false, false);
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
