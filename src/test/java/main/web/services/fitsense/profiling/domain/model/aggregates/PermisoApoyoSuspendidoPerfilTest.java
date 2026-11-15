package main.web.services.fitsense.profiling.domain.model.aggregates;

import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.commands.UpdateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V53-B · el permiso propio «apoyo suspendido» en el perfil: crear y actualizar NO lo tocan (sin cambio de API, sin
 * reinterpretar «no colgarse»); solo la confirmacion explicita lo fija, con su fecha.
 */
@DisplayName("V53-B · permiso «apoyo suspendido» en el agregado del perfil")
class PermisoApoyoSuspendidoPerfilTest {

    private static UserProfile nuevo(Boolean colgarse) {
        return new UserProfile(1L, new CreateUserProfileCommand(1L, LocalDate.of(1990, 1, 1), BiologicalSex.MALE,
                new BigDecimal("180"), new BigDecimal("80"), null, FitnessLevel.ADVANCED, GoalType.GENERAL_FITNESS,
                "x", TrainingLocation.GYM, (short) 3, List.of((short) 1, (short) 3, (short) 5), (short) 45,
                List.of("parallel bars"), List.of(), null, true, true, true, colgarse, true));
    }

    @Test
    @DisplayName("al crear y al actualizar queda sin responder y pendiente de confirmar, con o sin «no colgarse»")
    void crearYActualizarNoLoTocan() {
        for (Boolean colgarse : new Boolean[]{null, true, false}) {
            var p = nuevo(colgarse);
            assertNull(p.getAllowSuspendedSupport());
            assertNull(p.getSuspendedSupportConfirmedAt());
            assertTrue(p.apoyoSuspendidoPendienteDeConfirmar());
            p.update(new UpdateUserProfileCommand(1L, BiologicalSex.MALE, new BigDecimal("180"),
                    new BigDecimal("80"), null, FitnessLevel.ADVANCED, GoalType.GENERAL_FITNESS, "x", TrainingLocation.GYM,
                    (short) 3, List.of((short) 1, (short) 3, (short) 5), (short) 45, List.of("parallel bars"),
                    List.of(), null, null, null, null, false, null));
            assertNull(p.getAllowSuspendedSupport(), "cambiar «no colgarse» no escribe el permiso propio");
            assertTrue(p.apoyoSuspendidoPendienteDeConfirmar());
        }
    }

    @Test
    @DisplayName("la confirmacion explicita fija el valor y la fecha, y no toca «no colgarse»")
    void confirmacion() {
        var p = nuevo(false);
        p.confirmarApoyoSuspendido(true);
        assertEquals(Boolean.TRUE, p.getAllowSuspendedSupport());
        assertNotNull(p.getSuspendedSupportConfirmedAt());
        assertFalse(p.apoyoSuspendidoPendienteDeConfirmar());
        assertFalse(p.allowancesOrDefault().allowHangingExercises());
        p.confirmarApoyoSuspendido(false);
        assertEquals(Boolean.FALSE, p.getAllowSuspendedSupport());
    }
}
