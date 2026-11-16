package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutFocus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("H2 · RepartoV15: la regla de la mitad de V15 en una sola funcion")
class RepartoV15Test {

    @Test
    @DisplayName("aplica con 3 o mas grupos y 4 o mas ejercicios de trabajo: floor(n/2)")
    void tope() {
        assertNull(RepartoV15.topeMismoGrupo(2, 8), "con 2 grupos no hay regla de la mitad");
        assertNull(RepartoV15.topeMismoGrupo(3, 3), "con menos de 4 ejercicios de trabajo no hay regla");
        assertEquals(2, RepartoV15.topeMismoGrupo(3, 4));
        assertEquals(2, RepartoV15.topeMismoGrupo(3, 5));
        assertEquals(3, RepartoV15.topeMismoGrupo(3, 6));
        assertEquals(3, RepartoV15.topeMismoGrupo(3, 7));
        assertEquals(4, RepartoV15.topeMismoGrupo(5, 8));
    }

    @Test
    @DisplayName("usuario 39 (LOWER_BODY, 8 colocaciones): {4:2, 5:2, 6:3, 7:3, 8:4}; nunca el 6 de PR-1.7")
    void tablaUsuario39() {
        assertTrue(WorkoutFocus.LOWER_BODY.bodyPartCodes().size() >= 3, "premisa: LOWER_BODY tiene 3 grupos");
        assertEquals(Map.of("4", 2, "5", 2, "6", 3, "7", 3, "8", 4),
                RepartoV15.tabla(WorkoutFocus.LOWER_BODY.bodyPartCodes().size(), 8));
        assertEquals(Map.of(), RepartoV15.tabla(2, 8), "sin regla, sin tabla");
        assertEquals(Map.of(), RepartoV15.tabla(3, 3));
    }
}
