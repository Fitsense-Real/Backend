package main.web.services.fitsense.profiling.domain.model.aggregates;

import main.web.services.fitsense.planning.domain.services.DuracionPorObjetivoPolicy;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.commands.UpdateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.valueobjects.*;
import main.web.services.fitsense.shared.domain.exceptions.DomainRuleViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

    @ParameterizedTest(name = "{0} {1} min -> {2}")
    @CsvSource({
            "GAIN_MUSCLE,14,false", "GAIN_MUSCLE,15,true", "GAIN_MUSCLE,16,true",
            "INCREASE_STRENGTH,14,false", "INCREASE_STRENGTH,15,true", "INCREASE_STRENGTH,16,true",
            "LOSE_WEIGHT,29,false", "LOSE_WEIGHT,30,true", "LOSE_WEIGHT,31,true",
            "GENERAL_FITNESS,29,false", "GENERAL_FITNESS,30,true", "GENERAL_FITNESS,31,true",
            "GAIN_MUSCLE,180,true", "GAIN_MUSCLE,181,false",
            "INCREASE_STRENGTH,180,true", "INCREASE_STRENGTH,181,false",
            "LOSE_WEIGHT,180,true", "LOSE_WEIGHT,181,false",
            "GENERAL_FITNESS,180,true", "GENERAL_FITNESS,181,false",
            "LOSE_WEIGHT,15,false", "GENERAL_FITNESS,15,false"
    })
    @DisplayName("alta del perfil: admite o rechaza segun el objetivo, sin modificar los minutos")
    void alta(GoalType objetivo, int minutos, boolean admitido) {
        if (admitido) {
            var perfil = new UserProfile(1L, alta(objetivo, minutos));
            assertEquals((short) minutos, perfil.getSessionMinutes(), "nunca se corrige lo declarado");
        } else {
            var e = assertThrows(DomainRuleViolationException.class, () -> new UserProfile(1L, alta(objetivo, minutos)));
            assertTrue(e.getMessage().contains(objetivo.name()), e.getMessage());
            assertTrue(e.getMessage().contains("indicaste " + minutos), e.getMessage());
        }
        assertEquals(admitido, DuracionPorObjetivoPolicy.admite(objetivo.name(), minutos),
                "la tabla de planificacion coincide con la del perfil");
    }

    @ParameterizedTest
    @EnumSource(GoalType.class)
    @DisplayName("la tabla de planificacion (perfiles historicos) es la misma que la de admision")
    void mismasTablas(GoalType objetivo) {
        assertEquals(objetivo.minSessionMinutes(), DuracionPorObjetivoPolicy.minimo(objetivo.name()));
        assertEquals(GoalType.MAX_SESSION_MINUTES, DuracionPorObjetivoPolicy.MAXIMO);
    }

    @Test
    @DisplayName("la edicion previa al primer plan tambien respeta P-01 (no es una via para saltarla)")
    void edicion() {
        var perfil = new UserProfile(1L, alta(GoalType.GAIN_MUSCLE, 15));
        assertThrows(DomainRuleViolationException.class, () -> perfil.update(edicion(GoalType.GENERAL_FITNESS, 15)));
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
