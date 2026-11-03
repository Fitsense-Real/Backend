package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.ExclusionesDeSeguridad;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.planning.domain.services.ConservationValidator.ReglaPatron;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static main.web.services.fitsense.planning.domain.services.V8CConservacionPatronH1Test.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ¿PUEDE PUBLICARSE UNA SUSTITUCION DE MAYOR DIFICULTAD? (integracion MVP, fase 4.1). Documenta el comportamiento
 * REAL, sin cambiarlo:
 * <ol>
 *   <li>V8C no compara la dificultad: en VARIANTE exige el mismo patron (y con H1, la misma distribucion), y ante
 *       dolor ni siquiera el patron. Una sustitucion 1 -> 2 del mismo patron pasa con las dos reglas.</li>
 *   <li>El unico limite de dificultad es la elegibilidad (V1: el ejercicio esta en {@code available_exercises}, que
 *       ya filtra el nivel maximo de la persona). Lo comprueban otras pruebas; aqui no se repite.</li>
 *   <li>El prompt ofrece a la IA sustitutos de dificultad MENOR O IGUAL (SessionFixPromptBuilder), pero es una
 *       instruccion, no un validador: si la IA propone otro ejercicio elegible, nada lo rechaza por dificultad.</li>
 *   <li>La sustitucion determinista por seguridad (SUST-SEG-1) ordena por zona, patron, musculo e id: no mira la
 *       dificultad, asi que puede elegir uno mas dificil que el original.</li>
 * </ol>
 * Por eso el texto de la adaptacion (DescripcionAdaptacion) distingue SUSTITUCION_MAYOR_DIFICULTAD y no la llama
 * «del mismo nivel». Prohibirla seria un cambio funcional y queda como decision pendiente.
 */
@DisplayName("Sustitucion de mayor dificultad: lo que hoy permite el backend (sin cambiarlo)")
class SustitucionDificultadMayorTest {

    @ParameterizedTest(name = "VARIANTE con regla {0}: 580 (dif 1) -> 56 (dif 2), mismo patron, se ACEPTA")
    @EnumSource(ReglaPatron.class)
    @DisplayName("1. V8C acepta en VARIANTE una sustitucion del mismo patron que SUBE la dificultad registrada")
    void varianteSubeDificultad(ReglaPatron regla) {
        assertTrue(CAT.get(56L).difficulty() > CAT.get(580L).difficulty());
        var r = new ConservationValidator(regla).validate(
                derivacion(List.of(ej(56L, 3, 10), ej(360L, 3, 10)), List.of(ej(9001L, 3, 10))),
                semana("TOO_DIFFICULT", List.of(580L, 360L)), orden(TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA),
                CAT, ExclusionesDeSeguridad.ninguna(), List.of(), false, 30);
        assertTrue(r.aceptable(), r.problems()::toString);
        assertEquals(List.of(SENALADA), r.aplicadas());
    }

    @Test
    @DisplayName("2. V8C acepta ante dolor una sustitucion que sube la dificultad (la rama de dolor no la compara)")
    void dolorSubeDificultad() {
        var r = new ConservationValidator().validate(
                // En el ayudante, cada ejercicio de la sesion senalada lleva el motivo: con dolor, todos «dolieron»,
                // asi que la sesion solo lleva el que se sustituye.
                derivacion(List.of(ej(56L, 3, 10)), List.of(ej(9001L, 3, 10))),
                semana("PAIN_OR_DISCOMFORT", List.of(580L)), orden(TipoIntervencion.SEGURIDAD_ANTE_DOLOR),
                CAT, ExclusionesDeSeguridad.ninguna(), List.of(), false, 30);
        assertTrue(r.aceptable(), r.problems()::toString);
    }

    @Test
    @DisplayName("3. SUST-SEG-1 (respaldo por seguridad) no mira la dificultad: elige 70002 (dif 3) antes que 70003 (dif 1)")
    void sustitucionSeguridadNoMiraDificultad() {
        var original = cand(70001L, 1);
        Map<Long, CandidateExercise> elegibles = new LinkedHashMap<>();
        elegibles.put(70002L, cand(70002L, 3));
        elegibles.put(70003L, cand(70003L, 1));
        var elegido = SafetySubstitutionPolicy.elegir(original, null, elegibles, Set.of(), Set.of(), null);
        assertTrue(elegido.isPresent());
        assertEquals(70002L, elegido.get().exerciseId());
        assertTrue(elegido.get().difficulty() > original.difficulty());
    }

    static CandidateExercise cand(long id, int dificultad) {
        return new CandidateExercise(id, "e" + id, "chest", "body weight", dificultad, PrescriptionType.SETS_REPS,
                "pectorals", "HORIZONTAL_PUSH");
    }
}
