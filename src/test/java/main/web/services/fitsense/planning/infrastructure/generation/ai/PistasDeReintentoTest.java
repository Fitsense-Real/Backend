package main.web.services.fitsense.planning.infrastructure.generation.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Fase D: cada motivo real de las 48 predicciones apunta a su dato, sin repetir pistas. */
class PistasDeReintentoTest {

    @Test
    @DisplayName("Los motivos reales de la campana reciben su pista concreta")
    void motivosReales() {
        var pistas = PistasDeReintento.para(List.of(
                "V12: hay dos entrenamientos el mismo dia (2026-11-16).",
                "V8C: la sesion 15 sube la dosis del ejercicio 507 (2x8r/120 -> 3x12r/60). La IA no puede subir dosis: solo el backend progresa.",
                "V24: el ejercicio 507 del 2026-12-12 no lleva la dosis de ninguna capacidad de GENERAL_FITNESS.",
                "V24: el ejercicio 878 del 2026-12-12 no lleva la dosis de ninguna capacidad de GENERAL_FITNESS.",
                "V18: el ejercicio 1054 se repite en dias consecutivos (2026-11-28 y 2026-11-29)."), true);

        assertEquals(4, pistas.size(), pistas::toString);
        assertTrue(pistas.get(0).contains("FECHAS LIBRES VALIDAS"));
        assertTrue(pistas.get(1).contains("antes de la flecha"));
        assertTrue(pistas.get(2).contains("rest_seconds como estaba"));
        assertTrue(pistas.get(3).contains("SUSTITUTOS ADMITIDOS"));
    }

    @Test
    @DisplayName("En la generacion, V20 apunta a los segundos por clase")
    void generacion() {
        var pistas = PistasDeReintento.para(List.of(
                "V20: el entrenamiento del 2026-11-04 dura unos 1884 s y el minimo es 1890 s"), false);
        assertEquals(List.of(pistas.get(0)), pistas);
        assertTrue(pistas.get(0).contains("DURACION CALCULADA"));
    }

    @Test
    @DisplayName("Un motivo sin pista no anade nada")
    void sinPista() {
        assertTrue(PistasDeReintento.para(List.of("V15: mas de la mitad en un grupo"), false).isEmpty());
    }
}
