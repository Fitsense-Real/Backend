package main.web.services.fitsense.catalog.domain.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static main.web.services.fitsense.catalog.domain.services.FamiliaDeMovimientoEquivalente.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("O20 opcion A: familia de movimiento equivalente (sin base de datos)")
class FamiliaDeMovimientoEquivalenteTest {

    @Test
    @DisplayName("Nivel 1: variantes v. N, mayusculas, espacios y duplicados de nombre identico")
    void variantes() {
        assertEquals("dumbbell front raise", clave("dumbbell front raise v. 2"));
        assertEquals("dumbbell front raise", clave("  Dumbbell   Front Raise  "));
        assertEquals("lever shoulder press", clave("lever shoulder press v.3"));
        assertEquals("barbell upright row", clave("barbell upright row V. 10"));
        assertTrue(mismaFamilia("dumbbell front raise", "dumbbell front raise v. 2"));
        assertTrue(mismaFamilia("self assisted inverse leg curl", "self assisted inverse leg curl"));
        assertFalse(mismaFamilia("dumbbell front raise", "dumbbell lateral raise"));
        // «v.» dentro del nombre no es un sufijo de variante.
        assertEquals("v. sit hold", clave("v. sit hold"));
        assertNull(clave(null));
        assertNull(clave("   "));
        assertFalse(mismaFamilia(null, null), "dos ejercicios sin nombre no son de la misma familia");
    }

    @Test
    @DisplayName("Nivel 2: el mismo gesto con otro material o postura, solo con el mismo patron y musculo")
    void mismoGestoConOtroMaterial() {
        String p = "SHOULDER_RAISE", m = "delts";
        assertEquals("front raise", nucleo("dumbbell seated alternate front raise"));
        assertEquals("front raise", nucleo("band front raise"));
        assertTrue(mismoGesto("dumbbell front raise", p, m, "dumbbell seated front raise", p, m));
        assertTrue(mismoGesto("dumbbell front raise", p, m, "cable front raise", p, m));
        assertTrue(mismoGesto("dumbbell front raise", p, m, "barbell front raise", p, m));
        // Otro movimiento del mismo patron: NO es el mismo gesto (sustituto legitimo).
        assertFalse(mismoGesto("dumbbell front raise", p, m, "dumbbell lateral raise", p, m));
        assertFalse(mismoGesto("dumbbell front raise", p, m, "dumbbell full can lateral raise", p, m));
        assertFalse(mismoGesto("dumbbell front raise", p, m, "dumbbell lateral to front raise", p, m));
        // El mismo nucleo con OTRO patron o musculo no se agrupa (salvo que sea variante de nivel 1).
        assertFalse(mismoGesto("dumbbell front raise", p, m, "cable front raise", "VERTICAL_PUSH", m));
        assertFalse(mismoGesto("dumbbell front raise", p, m, "cable front raise", p, "traps"));
        assertFalse(mismoGesto("dumbbell front raise", null, null, "cable front raise", null, null));
        // Nivel 1 no exige patron: las variantes de upright row con patrones distintos siguen siendo equivalentes.
        assertTrue(equivalentes("barbell upright row", "SHOULDER_RAISE", m, "barbell upright row v. 3",
                "HORIZONTAL_PULL", m));
        // Un nombre hecho solo de modificadores no agrupa nada.
        assertNull(nucleo("dumbbell"));
    }
}
