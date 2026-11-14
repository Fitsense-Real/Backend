package main.web.services.fitsense.planning.domain.model.valueobjects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V53-B · resolucion del apoyo suspendido (propuesta, apagada por defecto).
 * <p>
 * La tabla fija, para cada modo, que hace el permiso propio {@code allow_suspended_support} con y sin «no colgarse».
 * «Efecto» es lo que de verdad cambia el conjunto elegible y los validadores: el componente
 * {@code excludeSuspendedSupport} solo tiene efecto si la fachada del catalogo entrega el hecho, es decir, con la
 * opcion A o la B encendida.
 */
@DisplayName("V53-B · resolucion del permiso «apoyo suspendido»")
class PermisoApoyoSuspendidoResolucionTest {

    private static Boolean tri(String v) {
        return "null".equals(v) ? null : Boolean.valueOf(v);
    }

    private static SafetyProfile.Declaradas declaradas(boolean colgarse, Boolean apoyo) {
        return new SafetyProfile.Declaradas(true, true, true, colgarse, true, apoyo);
    }

    @ParameterizedTest(name = "{0} colgarse={1} permiso={2} -> componente={3} efecto={4}")
    @CsvSource(delimiter = ';', value = {
            // APAGADO: el permiso propio se ignora y el atributo no tiene efecto en ningun caso.
            "APAGADO;true;null;false;false",
            "APAGADO;true;true;false;false",
            "APAGADO;true;false;false;false",
            "APAGADO;false;null;true;false",
            "APAGADO;false;true;true;false",
            "APAGADO;false;false;true;false",
            // OPCION A: el apoyo suspendido sigue a «no colgarse»; el permiso propio se ignora.
            "NO_COLGARSE;true;null;false;false",
            "NO_COLGARSE;true;true;false;false",
            "NO_COLGARSE;true;false;false;false",
            "NO_COLGARSE;false;null;true;true",
            "NO_COLGARSE;false;true;true;true",
            "NO_COLGARSE;false;false;true;true",
            // OPCION B: sin responder hereda «no colgarse»; respondido, manda el permiso en los dos sentidos.
            "PERMISO_PROPIO;true;null;false;false",
            "PERMISO_PROPIO;true;true;false;false",
            "PERMISO_PROPIO;true;false;true;true",
            "PERMISO_PROPIO;false;null;true;true",
            "PERMISO_PROPIO;false;true;false;false",
            "PERMISO_PROPIO;false;false;true;true"
    })
    void tablaDeResolucion(ModoApoyoSuspendido modo, boolean colgarse, String permiso, boolean componente,
                           boolean efecto) {
        var s = SafetyProfile.forAgeLevelAndDeclared(30, "ADVANCED", declaradas(colgarse, tri(permiso)), modo);
        assertEquals(componente, s.excludeSuspendedSupport());
        assertEquals(efecto, modo.activo() && s.excludeSuspendedSupport());
        // Ningun otro componente depende del modo ni del permiso: B no levanta ni anade otra restriccion.
        var base = SafetyProfile.forAgeLevelAndDeclared(30, "ADVANCED",
                new SafetyProfile.Declaradas(true, true, true, colgarse, true));
        assertEquals(base.excludeHanging(), s.excludeHanging());
        assertEquals(base.excludeHighImpact(), s.excludeHighImpact());
        assertEquals(base.excludeFloorWork(), s.excludeFloorWork());
        assertEquals(base.excludeAxialLoad(), s.excludeAxialLoad());
        assertEquals(base.excludeKneeling(), s.excludeKneeling());
        assertEquals(base.excludeOverhead(), s.excludeOverhead());
        assertEquals(base.excludeInversion(), s.excludeInversion());
        assertEquals(base.describe(), s.describe(), "el texto del prompt no cambia (decision documentada)");
    }

    @Test
    @DisplayName("la opcion B tampoco relaja la politica por edad ni las demas declaraciones")
    void bNoRelajaNadaMas() {
        var todo = new SafetyProfile.Declaradas(false, false, false, false, false, true);
        var conB = SafetyProfile.forAgeLevelAndDeclared(72, "BEGINNER", todo, ModoApoyoSuspendido.PERMISO_PROPIO);
        var sinB = SafetyProfile.forAgeLevelAndDeclared(72, "BEGINNER", todo.sinPermisoApoyoSuspendido());
        assertFalse(conB.excludeSuspendedSupport());
        assertTrue(sinB.excludeSuspendedSupport());
        assertEquals(new SafetyProfile(sinB.excludeHighImpact(), sinB.excludeFloorWork(), sinB.excludeAxialLoad(),
                sinB.excludeKneeling(), sinB.excludeHanging(), sinB.excludeOverhead(), sinB.excludeInversion(),
                false), conB);
    }

    @Test
    @DisplayName("los dos interruptores encendidos: configuracion ambigua, falla con un mensaje claro")
    void dosInterruptoresFallan() {
        var e = assertThrows(IllegalStateException.class, () -> ModoApoyoSuspendido.desdeInterruptores(true, true));
        assertTrue(e.getMessage().contains(ModoApoyoSuspendido.PROPIEDAD_OPCION_A), e.getMessage());
        assertTrue(e.getMessage().contains(ModoApoyoSuspendido.PROPIEDAD_OPCION_B), e.getMessage());
        assertTrue(e.getMessage().contains("excluyentes"), e.getMessage());
        assertEquals(ModoApoyoSuspendido.APAGADO, ModoApoyoSuspendido.desdeInterruptores(false, false));
        assertEquals(ModoApoyoSuspendido.NO_COLGARSE, ModoApoyoSuspendido.desdeInterruptores(true, false));
        assertEquals(ModoApoyoSuspendido.PERMISO_PROPIO, ModoApoyoSuspendido.desdeInterruptores(false, true));
        assertFalse(ModoApoyoSuspendido.APAGADO.activo());
    }

    @Test
    @DisplayName("compatibilidad: el constructor de siete componentes equivale a excludeSuspendedSupport = excludeHanging")
    void constructorDeCompatibilidad() {
        for (int m = 0; m < 128; m++) {
            boolean[] b = new boolean[7];
            for (int i = 0; i < 7; i++) b[i] = (m & (1 << i)) != 0;
            var siete = new SafetyProfile(b[0], b[1], b[2], b[3], b[4], b[5], b[6]);
            var ocho = new SafetyProfile(b[0], b[1], b[2], b[3], b[4], b[5], b[6], b[4]);
            assertEquals(ocho, siete);
            assertEquals(b[4], siete.excludeSuspendedSupport());
        }
        assertFalse(SafetyProfile.none().excludeSuspendedSupport());
        assertFalse(SafetyProfile.forAge(80).excludeSuspendedSupport());
    }

    @Test
    @DisplayName("compatibilidad: Declaradas de cinco = permiso propio null; los modos A y apagado dan lo de siempre")
    void declaradasDeCompatibilidad() {
        boolean[] v = {true, false};
        for (boolean a : v) for (boolean b : v) for (boolean c : v) for (boolean d : v) for (boolean e : v) {
            var cinco = new SafetyProfile.Declaradas(a, b, c, d, e);
            assertEquals(new SafetyProfile.Declaradas(a, b, c, d, e, null), cinco);
            assertNull(cinco.allowSuspendedSupport());
            for (int edad : new int[]{30, 61, 66, 72})
                for (String nivel : new String[]{"BEGINNER", "INTERMEDIATE", "ADVANCED"}) {
                    var hoy = SafetyProfile.forAgeLevelAndDeclared(edad, nivel, cinco);
                    assertEquals(hoy.excludeHanging(), hoy.excludeSuspendedSupport());
                    for (var modo : ModoApoyoSuspendido.values())
                        assertEquals(hoy, SafetyProfile.forAgeLevelAndDeclared(edad, nivel, cinco, modo),
                                "sin permiso propio todos los modos resuelven igual: " + modo);
                    // Con permiso propio, apagado y opcion A lo ignoran.
                    for (Boolean permiso : new Boolean[]{true, false}) {
                        var seis = new SafetyProfile.Declaradas(a, b, c, d, e, permiso);
                        assertEquals(hoy, SafetyProfile.forAgeLevelAndDeclared(edad, nivel, seis,
                                ModoApoyoSuspendido.APAGADO));
                        assertEquals(hoy, SafetyProfile.forAgeLevelAndDeclared(edad, nivel, seis,
                                ModoApoyoSuspendido.NO_COLGARSE));
                    }
                }
        }
        assertEquals(SafetyProfile.forAgeLevelAndDeclared(30, "ADVANCED", null),
                SafetyProfile.forAgeLevelAndDeclared(30, "ADVANCED", null, ModoApoyoSuspendido.PERMISO_PROPIO));
    }
}
