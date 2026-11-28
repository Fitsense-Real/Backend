package main.web.services.fitsense.planning.domain.model.valueobjects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * INC-4C-03: que lo declarado por el participante llegue al filtro.
 * <p>
 * El defecto no era que {@code forAgeLevelAndDeclared} estuviera mal: estaba
 * bien y <b>nadie lo llamaba</b>. Estas pruebas fijan su comportamiento y, la
 * ultima, fija tambien el del metodo que se usaba antes, para que quede escrito
 * por que no basta.
 */
class SafetyProfileDeclaradasTest {

    private static final int EDAD_MVP = 30;          // el perfil del piloto
    private static final String NIVEL_MVP = "BEGINNER";

    private static SafetyProfile con(SafetyProfile.Declaradas declaradas) {
        return SafetyProfile.forAgeLevelAndDeclared(EDAD_MVP, NIVEL_MVP, declaradas);
    }

    private static SafetyProfile.Declaradas d(boolean impacto, boolean suelo, boolean arrodillarse,
                                              boolean suspension, boolean cabeza) {
        return new SafetyProfile.Declaradas(impacto, suelo, arrodillarse, suspension, cabeza);
    }

    // -----------------------------------------------------------------------
    // 1. Cada bandera corta lo suyo
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("No poder suspenderse excluye suspension, y solo eso")
    void suspension() {
        var s = con(d(true, true, true, false, true));
        assertTrue(s.excludeHanging());
        assertFalse(s.excludeKneeling());
        assertFalse(s.excludeOverhead());
        assertFalse(s.excludeFloorWork());
    }

    @Test
    @DisplayName("No poder arrodillarse excluye arrodillarse, y solo eso")
    void arrodillarse() {
        var s = con(d(true, true, false, true, true));
        assertTrue(s.excludeKneeling());
        assertFalse(s.excludeHanging());
        assertFalse(s.excludeOverhead());
    }

    @Test
    @DisplayName("No poder llevar los brazos por encima de la cabeza excluye eso, y solo eso")
    void porEncimaDeLaCabeza() {
        var s = con(d(true, true, true, true, false));
        assertTrue(s.excludeOverhead());
        assertFalse(s.excludeKneeling());
        assertFalse(s.excludeHanging());
    }

    @Test
    @DisplayName("No poder trabajar en el suelo excluye el suelo a los 30, donde la edad no lo hace")
    void suelo() {
        // La politica por edad solo retira el suelo a partir de los 70: a los 30
        // esta bandera es la UNICA que puede excluirlo.
        assertFalse(SafetyProfile.forAgeAndLevel(EDAD_MVP, NIVEL_MVP).excludeFloorWork());
        assertTrue(con(d(true, false, true, true, true)).excludeFloorWork());
    }

    @Test
    @DisplayName("No poder hacer alto impacto se suma a la exclusion por nivel")
    void altoImpacto() {
        // A los 30 INTERMEDIATE la edad y el nivel no excluyen el impacto.
        assertFalse(SafetyProfile.forAgeAndLevel(30, "INTERMEDIATE").excludeHighImpact());
        assertTrue(SafetyProfile.forAgeLevelAndDeclared(30, "INTERMEDIATE",
                d(false, true, true, true, true)).excludeHighImpact());
    }

    // -----------------------------------------------------------------------
    // 2. Los limites: lo declarado SUMA, nunca resta
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Declarar que puede NO levanta una restriccion de edad")
    void loDeclaradoNoLevantaLaEdad() {
        var mayor = SafetyProfile.forAgeLevelAndDeclared(72, NIVEL_MVP,
                SafetyProfile.Declaradas.sinLimitaciones());

        assertTrue(mayor.excludeFloorWork(), "A los 72 el suelo se retira por edad");
        assertTrue(mayor.excludeAxialLoad());
        assertTrue(mayor.excludeInversion());
        assertTrue(mayor.excludeHighImpact());
    }

    @Test
    @DisplayName("Sin declaraciones (cliente antiguo) se comporta como la politica de edad y nivel")
    void nullEsSinLimitaciones() {
        var sinDeclarar = SafetyProfile.forAgeLevelAndDeclared(EDAD_MVP, NIVEL_MVP, null);
        var soloPolitica = SafetyProfile.forAgeAndLevel(EDAD_MVP, NIVEL_MVP);

        assertEquals(soloPolitica.excludeHighImpact(), sinDeclarar.excludeHighImpact());
        assertEquals(soloPolitica.excludeFloorWork(), sinDeclarar.excludeFloorWork());
        assertEquals(soloPolitica.excludeAxialLoad(), sinDeclarar.excludeAxialLoad());
        assertFalse(sinDeclarar.excludeKneeling());
        assertFalse(sinDeclarar.excludeHanging());
        assertFalse(sinDeclarar.excludeOverhead());
    }

    // -----------------------------------------------------------------------
    // 3. El defecto, fijado para que no vuelva
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("forAgeAndLevel ignora lo declarado: por eso no se puede usar en prepare()")
    void elDefectoQueSeCorrigio() {
        // Hasta el 16/10, WeeklyTrainingPlanCommandServiceImpl llamaba a este
        // metodo. Pasa false LITERAL en las tres ultimas: da igual lo que el
        // participante haya declarado.
        var ignorando = SafetyProfile.forAgeAndLevel(EDAD_MVP, NIVEL_MVP);
        assertFalse(ignorando.excludeKneeling());
        assertFalse(ignorando.excludeHanging());
        assertFalse(ignorando.excludeOverhead());

        // El mismo participante, diciendo que no puede ninguna de las tres.
        var escuchando = con(d(true, true, false, false, false));
        assertTrue(escuchando.excludeKneeling());
        assertTrue(escuchando.excludeHanging());
        assertTrue(escuchando.excludeOverhead());
    }

    @Test
    @DisplayName("El perfil del MVP a los 30: solo una de las banderas de edad le protege")
    void elPerfilDelPiloto() {
        var s = SafetyProfile.forAgeLevelAndDeclared(EDAD_MVP, NIVEL_MVP,
                SafetyProfile.Declaradas.sinLimitaciones());

        assertTrue(s.excludeHighImpact(), "por ser principiante, no por la edad");
        assertFalse(s.excludeFloorWork());
        assertFalse(s.excludeAxialLoad());
        // Arrodillarse, suspenderse y por encima de la cabeza NO tienen corte
        // por edad, por decision de Juan del 15/10: no se inventan
        // prohibiciones. Para este perfil, lo declarado es lo unico que puede
        // excluirlos, y hasta hoy no llegaba.
        assertFalse(s.excludeKneeling());
        assertFalse(s.excludeHanging());
        assertFalse(s.excludeOverhead());
    }
}
