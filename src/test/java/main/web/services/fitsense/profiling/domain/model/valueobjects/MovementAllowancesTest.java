package main.web.services.fitsense.profiling.domain.model.valueobjects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** INC-4C-03: las cinco banderas declaradas. Dominio puro. */
class MovementAllowancesTest {

    @Test
    @DisplayName("Al crear, lo que no se declara se admite (como el DEFAULT TRUE de V25)")
    void alCrearNullEsPermitido() {
        var todas = MovementAllowances.declaradas(null, null, null, null, null);
        assertEquals(MovementAllowances.sinLimitaciones(), todas);
        assertFalse(todas.algunaLimitacion());
    }

    @Test
    @DisplayName("Al crear, solo false prohibe")
    void alCrearSoloFalseProhibe() {
        var sinSuspension = MovementAllowances.declaradas(null, null, null, false, null);
        assertTrue(sinSuspension.allowHighImpact());
        assertFalse(sinSuspension.allowHangingExercises());
        assertTrue(sinSuspension.algunaLimitacion());
    }

    @Test
    @DisplayName("Al actualizar, lo que no se envia SE CONSERVA")
    void alActualizarNullConserva() {
        var actual = MovementAllowances.declaradas(true, true, true, false, false);

        // Un cliente antiguo manda el formulario sin estos campos.
        var tras = actual.conCambios(null, null, null, null, null);

        assertEquals(actual, tras,
                "Si null reiniciara a true, una restriccion declarada se borraria sin que nadie lo note");
        assertFalse(tras.allowHangingExercises());
        assertFalse(tras.allowOverheadExercises());
    }

    @Test
    @DisplayName("Al actualizar se puede levantar y volver a poner una restriccion")
    void alActualizarSePuedeCambiar() {
        var actual = MovementAllowances.declaradas(true, true, true, false, true);

        var levantada = actual.conCambios(null, null, null, true, null);
        assertTrue(levantada.allowHangingExercises());
        assertFalse(levantada.algunaLimitacion());

        var otraVez = levantada.conCambios(null, null, null, false, null);
        assertFalse(otraVez.allowHangingExercises());
    }

    @Test
    @DisplayName("Las cinco banderas son independientes")
    void lasCincoSonIndependientes() {
        assertFalse(MovementAllowances.declaradas(false, null, null, null, null).allowHighImpact());
        assertFalse(MovementAllowances.declaradas(null, false, null, null, null).allowFloorExercises());
        assertFalse(MovementAllowances.declaradas(null, null, false, null, null).allowKneelingExercises());
        assertFalse(MovementAllowances.declaradas(null, null, null, false, null).allowHangingExercises());
        assertFalse(MovementAllowances.declaradas(null, null, null, null, false).allowOverheadExercises());

        // y ninguna arrastra a las demas
        var soloSuelo = MovementAllowances.declaradas(null, false, null, null, null);
        assertTrue(soloSuelo.allowHighImpact());
        assertTrue(soloSuelo.allowKneelingExercises());
        assertTrue(soloSuelo.allowHangingExercises());
        assertTrue(soloSuelo.allowOverheadExercises());
    }
}
