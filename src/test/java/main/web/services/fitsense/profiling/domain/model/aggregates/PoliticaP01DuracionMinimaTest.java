package main.web.services.fitsense.profiling.domain.model.aggregates;

import main.web.services.fitsense.planning.domain.services.DuracionPorObjetivoPolicy;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.commands.UpdateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.*;
import main.web.services.fitsense.shared.domain.exceptions.DomainRuleViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * P-01 · duracion minima por objetivo en la ADMISION del perfil. Limites inmediatamente inferior, igual y
 * superior al minimo, y 180/181 para los cuatro objetivos. El perfil nunca corrige el valor: o lo admite tal
 * cual o lo rechaza con un mensaje comprensible (HTTP 422 por DomainRuleViolationException).
 */
class PoliticaP01DuracionMinimaTest {

    /** P-01 definitiva: 30-120 para los cuatro objetivos; la matriz de la orden de cierre del PR #16. */
    static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> matriz() {
        var casos = new java.util.ArrayList<org.junit.jupiter.params.provider.Arguments>();
        int[] minutos = {15, 29, 30, 60, 119, 120, 121, 180};
        boolean[] admitido = {false, false, true, true, true, true, false, false};
        for (var objetivo : GoalType.values())
            for (int i = 0; i < minutos.length; i++)
                casos.add(org.junit.jupiter.params.provider.Arguments.of(objetivo, minutos[i], admitido[i]));
        return casos.stream();
    }

    @ParameterizedTest(name = "{0} {1} min -> {2}")
    @org.junit.jupiter.params.provider.MethodSource("matriz")
    @DisplayName("alta del perfil: 30-120 para los cuatro objetivos, sin modificar los minutos")
    void alta(GoalType objetivo, int minutos, boolean admitido) {
        if (admitido) {
            var perfil = new UserProfile(1L, alta(objetivo, minutos));
            assertEquals((short) minutos, perfil.getSessionMinutes(), "nunca se corrige lo declarado");
        } else {
            var e = assertThrows(DomainRuleViolationException.class, () -> new UserProfile(1L, alta(objetivo, minutos)));
            assertTrue(e.getMessage().contains("entre 30 y 120 minutos"), e.getMessage());
            assertTrue(e.getMessage().contains("indicaste " + minutos), e.getMessage());
        }
        assertEquals(admitido, DuracionPorObjetivoPolicy.admite(objetivo.name(), minutos),
                "la tabla de planificacion coincide con la del perfil");
    }

    @ParameterizedTest
    @EnumSource(GoalType.class)
    @DisplayName("V4 intacta: con 120 min el techo sigue siendo 120 x 1,15 = 138 (y con 30, 34)")
    void toleranciaV4(GoalType objetivo) {
        assertEquals(138, new UserProfile(1L, alta(objetivo, 120)).maxSessionMinutesAllowed());
        assertEquals(69, new UserProfile(1L, alta(objetivo, 60)).maxSessionMinutesAllowed());
        assertEquals(34, new UserProfile(1L, alta(objetivo, 30)).maxSessionMinutesAllowed());
    }

    @ParameterizedTest
    @EnumSource(GoalType.class)
    @DisplayName("la tabla de planificacion (perfiles historicos) es la misma que la de admision")
    void mismasTablas(GoalType objetivo) {
        assertEquals(30, objetivo.minSessionMinutes());
        assertEquals(objetivo.minSessionMinutes(), DuracionPorObjetivoPolicy.minimo(objetivo.name()));
        assertEquals(120, GoalType.MAX_SESSION_MINUTES);
        assertEquals(GoalType.MAX_SESSION_MINUTES, DuracionPorObjetivoPolicy.MAXIMO);
    }

    @Test
    @DisplayName("la edicion previa al primer plan tambien respeta P-01 (no es una via para saltarla)")
    void edicion() {
        var perfil = new UserProfile(1L, alta(GoalType.GAIN_MUSCLE, 30));
        assertThrows(DomainRuleViolationException.class, () -> perfil.update(edicion(GoalType.GAIN_MUSCLE, 29)));
        assertThrows(DomainRuleViolationException.class, () -> perfil.update(edicion(GoalType.GENERAL_FITNESS, 121)));
        assertEquals((short) 30, perfil.getSessionMinutes(), "un rechazo no deja el perfil a medias");
    }

    private static CreateUserProfileCommand alta(GoalType objetivo, int minutos) {
        return new CreateUserProfileCommand(1L, LocalDate.of(1990, 1, 1), BiologicalSex.FEMALE,
                new BigDecimal("165"), new BigDecimal("65"), null, FitnessLevel.INTERMEDIATE, objetivo, "objetivo",
                TrainingLocation.GYM, (short) 3, List.of((short) 1, (short) 3, (short) 5), (short) minutos,
                List.of("dumbbell"), List.of(), null, true, true, true, true, true);
    }

    private static UpdateUserProfileCommand edicion(GoalType objetivo, int minutos) {
        return new UpdateUserProfileCommand(1L, BiologicalSex.FEMALE, new BigDecimal("165"), new BigDecimal("65"),
                null, FitnessLevel.INTERMEDIATE, objetivo, "objetivo", TrainingLocation.GYM, (short) 3,
                List.of((short) 1, (short) 3, (short) 5), (short) minutos, List.of("dumbbell"), List.of(), null,
                true, true, true, true, true);
    }
}
