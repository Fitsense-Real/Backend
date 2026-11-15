package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.ExerciseSafetyFacts;
import main.web.services.fitsense.planning.domain.model.valueobjects.ModoApoyoSuspendido;
import main.web.services.fitsense.planning.domain.model.valueobjects.SafetyProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V53-B · la causa {@code apoyo_suspendido} sale del componente propio {@code excludeSuspendedSupport}. Con todo
 * SafetyProfile construido como antes (constructor de siete o permiso propio null) ese componente vale lo mismo que
 * {@code excludeHanging}: las causas son identicas a las de V53.
 */
@DisplayName("V53-B · causa apoyo_suspendido con el permiso propio")
class PermisoApoyoSuspendidoCausaTest {

    /** 457 chest dip: apoyo suspendido, sin colgarse. Con el hecho como lo entrega la fachada con A o B encendida. */
    private static final ExerciseSafetyFacts FONDO = new ExerciseSafetyFacts(457L, true, false, false, false, false,
            false, false, false, true);
    /** 1069 muscle-up: cuelga y apoya. */
    private static final ExerciseSafetyFacts MUSCLE_UP = new ExerciseSafetyFacts(1069L, true, false, false, false,
            false, true, false, false, true);
    /** Apagado: la fachada entrega el hecho en false. */
    private static final ExerciseSafetyFacts FONDO_APAGADO = new ExerciseSafetyFacts(457L, true, false, false, false,
            false, false, false, false, false);

    private static SafetyProfile b(boolean colgarse, Boolean apoyo) {
        return SafetyProfile.forAgeLevelAndDeclared(30, "ADVANCED",
                new SafetyProfile.Declaradas(true, true, true, colgarse, true, apoyo), ModoApoyoSuspendido.PERMISO_PROPIO);
    }

    private static List<String> causas(ExerciseSafetyFacts f, SafetyProfile s) {
        return SafetyConflictPolicy.causas(f, s, false, false, false);
    }

    @Test
    @DisplayName("igual que V53 con los perfiles de siempre")
    void identicoConLosPerfilesDeSiempre() {
        var sinColgarse = new SafetyProfile(false, false, false, false, true, false, false);
        var libre = SafetyProfile.none();
        assertEquals(List.of(SafetyConflictPolicy.APOYO_SUSPENDIDO), causas(FONDO, sinColgarse));
        assertEquals(List.of(), causas(FONDO, libre));
        assertEquals(List.of(), causas(FONDO_APAGADO, sinColgarse), "apagado: sin hecho no hay causa");
        assertEquals(List.of(SafetyConflictPolicy.SUSPENSION, SafetyConflictPolicy.APOYO_SUSPENDIDO),
                causas(MUSCLE_UP, sinColgarse));
    }

    @Test
    @DisplayName("opcion B: la causa sigue al permiso propio; «no colgarse» sigue dando suspension")
    void opcionB() {
        // Sin responder: hereda «no colgarse».
        assertEquals(List.of(SafetyConflictPolicy.APOYO_SUSPENDIDO), causas(FONDO, b(false, null)));
        assertEquals(List.of(), causas(FONDO, b(true, null)));
        // Confirmado que si: el fondo vuelve; el muscle-up sigue fuera por colgarse.
        assertEquals(List.of(), causas(FONDO, b(false, true)));
        assertEquals(List.of(SafetyConflictPolicy.SUSPENSION), causas(MUSCLE_UP, b(false, true)));
        // Puede colgarse pero no apoyo suspendido.
        assertEquals(List.of(SafetyConflictPolicy.APOYO_SUSPENDIDO), causas(FONDO, b(true, false)));
        assertEquals(List.of(SafetyConflictPolicy.APOYO_SUSPENDIDO), causas(MUSCLE_UP, b(true, false)));
        assertTrue(SafetyConflictPolicy.esMotivoDeSeguridad(SafetyConflictPolicy.APOYO_SUSPENDIDO));
        assertTrue(SafetyConflictPolicy.esMotivoDeSustitucion(SafetyConflictPolicy.APOYO_SUSPENDIDO));
    }

    @Test
    @DisplayName("opcion B no toca INC-CAT-C39: 1153 sigue contenido para «no colgarse» aunque confirme el permiso")
    void c39NoCambia() {
        for (Boolean apoyo : new Boolean[]{null, true, false}) {
            assertTrue(ContencionTextoBandera.contiene(1153L, b(false, apoyo)));
            assertTrue(ContencionTextoBandera.contiene(1303L, b(false, apoyo)));
            assertFalse(ContencionTextoBandera.contiene(1153L, b(true, apoyo)));
        }
    }
}
