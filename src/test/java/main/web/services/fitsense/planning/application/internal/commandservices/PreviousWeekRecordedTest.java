package main.web.services.fitsense.planning.application.internal.commandservices;

import main.web.services.fitsense.planning.domain.model.valueobjects.WorkoutStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@code recorded} alineado con V30.
 * <p>
 * La regla es: hay informacion de una sesion si se ejecuto O si quedo declarada
 * como omitida. Lo que antes se perdia era el segundo caso, y es justo la
 * distincion sobre la que se decide adaptar: una omision declarada no es una
 * sesion desconocida.
 * <p>
 * La tabla se recorre ENTERA, incluidos los estados que hoy no se dan en esta
 * ruta, para que anadir un estado al enum no cambie el significado sin que nadie
 * lo note.
 */
class PreviousWeekRecordedTest {

    // =======================================================================
    // El caso que estaba mal
    // =======================================================================

    @Test
    @DisplayName("Omitida y declarada, sin ejecucion: SI hay registro")
    void omisionDeclarada() {
        assertTrue(PreviousWeekAssembler.hayRegistro(false, WorkoutStatus.SKIPPED),
                "la persona nos dijo que no entreno: eso es informacion, y decirle"
                        + " al modelo «sin registro» le oculta el motivo");
    }

    // =======================================================================
    // Y los que ya estaban bien, que no deben cambiar
    // =======================================================================

    @Test
    @DisplayName("Prevista y nunca tocada: NO hay registro")
    void previstaSinTocar() {
        assertFalse(PreviousWeekAssembler.hayRegistro(false, WorkoutStatus.SCHEDULED),
                "esta si es una sesion desconocida");
    }

    @Test
    @DisplayName("Con ejecucion registrada: SI hay registro, cualquiera sea el estado")
    void conEjecucion() {
        for (var estado : WorkoutStatus.values()) {
            assertTrue(PreviousWeekAssembler.hayRegistro(true, estado),
                    () -> "con resultado de ejecucion hay registro, y el estado "
                            + estado + " no puede quitarselo");
        }
    }

    @Test
    @DisplayName("Sin ejecucion, solo SKIPPED cuenta como registro")
    void tablaCompleta() {
        for (var estado : WorkoutStatus.values()) {
            boolean esperado = estado == WorkoutStatus.SKIPPED;
            assertEquals(esperado, PreviousWeekAssembler.hayRegistro(false, estado),
                    () -> "sin resultado, " + estado + " deberia dar " + esperado
                            + ". Si se anadio un estado al enum, decide a proposito"
                            + " si cuenta como informacion antes de tocar esta prueba");
        }
    }

    @Test
    @DisplayName("Es la definicion de V30, no una parecida")
    void mismaDefinicionQueV30() {
        // V30, en WeeklyMetricsCalculator:
        //     boolean omisionDeclarada = "SKIPPED".equals(workout.status());
        //     boolean conRegistro = session != null || omisionDeclarada;
        for (var estado : WorkoutStatus.values()) {
            for (boolean conResultado : new boolean[] {true, false}) {
                boolean v30 = conResultado || "SKIPPED".equals(estado.name());
                assertEquals(v30, PreviousWeekAssembler.hayRegistro(conResultado, estado),
                        () -> "difiere de V30 en (" + conResultado + ", " + estado + ")");
            }
        }
    }
}
