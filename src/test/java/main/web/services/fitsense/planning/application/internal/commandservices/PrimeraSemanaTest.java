package main.web.services.fitsense.planning.application.internal.commandservices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** La semana del primer plan: la actual solo si ninguna de sus sesiones ya paso. */
class PrimeraSemanaTest {

    static final LocalDate LUNES = LocalDate.of(2026, 10, 19);
    static final LocalDate LUNES_SIGUIENTE = LUNES.plusWeeks(1);
    static final List<Short> LMV = List.of((short) 1, (short) 3, (short) 5);

    private static LocalDate semana(LocalDate hoy, List<Short> dias, int porSemana) {
        return WeeklyTrainingPlanCommandServiceImpl.primeraSemana(hoy, dias, porSemana, "INTERMEDIATE");
    }

    @Test
    @DisplayName("sabado con lunes, miercoles y viernes: el lunes que viene (el caso del plan 53)")
    void sabado() {
        assertEquals(LUNES_SIGUIENTE, semana(LUNES.plusDays(5), LMV, 3));
    }

    @Test
    @DisplayName("el mismo lunes: la semana actual; la sesion de hoy vence a las 23:59")
    void lunes() {
        assertEquals(LUNES, semana(LUNES, LMV, 3));
    }

    @Test
    @DisplayName("martes con lunes, miercoles y viernes: el lunes ya paso, la siguiente")
    void martes() {
        assertEquals(LUNES_SIGUIENTE, semana(LUNES.plusDays(1), LMV, 3));
    }

    @Test
    @DisplayName("martes con miercoles, viernes y domingo: todo por delante, la actual")
    void martesConDiasPorDelante() {
        assertEquals(LUNES, semana(LUNES.plusDays(1), List.of((short) 3, (short) 5, (short) 7), 3));
    }

    @Test
    @DisplayName("cuenta los dias que el generador elegiria, no todos los disponibles")
    void soloLosElegidos() {
        // Lunes a sabado disponibles, 2 por semana: el generador elige lunes y jueves.
        var todos = List.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6);
        assertEquals(LUNES_SIGUIENTE, semana(LUNES.plusDays(1), todos, 2));
        assertEquals(LUNES, semana(LUNES, todos, 2));
    }
}
