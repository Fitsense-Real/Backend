package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.ConservationReport;
import main.web.services.fitsense.planning.domain.model.valueobjects.ExclusionesDeSeguridad;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.planning.domain.services.ConservationValidator.ReglaPatron;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static main.web.services.fitsense.planning.domain.services.V8CConservacionPatronH1Test.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * VALIDACION FINAL DEL LIMITE DE DIFICULTAD · TOO_DIFFICULT (VARIANTE_MAS_SENCILLA_O_CARGA), escenarios 1-12 contra
 * V8C real, con H1 (MULTICONJUNTO, la regla por defecto) y las tres estrategias del interruptor
 * {@code fitsense.adaptation.difficulty-guard}: {@code off} (A, la de hoy), {@code on} (B) y {@code preferencia} (C).
 * <p>
 * La comparacion es por EMPAREJAMIENTO valido dentro de cada patron: no por el maximo ni por el promedio. La IGUALDAD
 * sigue permitida (no es H12-A). Ids del catalogo V44_2 (V8CConservacionPatronH1Test.CAT) mas tres fichas sinteticas de
 * dificultad 3 (7030x) para los escenarios que la necesitan; los ids reales no se modifican.
 */
@DisplayName("Limite de dificultad · TOO_DIFFICULT: escenarios 1-12 con A/B/C (V8C real, H1 multiconjunto)")
class LimiteDificultadTooDifficultTest {

    static final Map<Long, CandidateExercise> CATALOGO = new LinkedHashMap<>(CAT);

    static {
        CATALOGO.put(70301L, new CandidateExercise(70301L, "e70301", "chest", "barbell", 3, PrescriptionType.SETS_REPS,
                "pectorals", "HORIZONTAL_PUSH"));
        CATALOGO.put(70302L, new CandidateExercise(70302L, "e70302", "back", "barbell", 3, PrescriptionType.SETS_REPS,
                "upper back", "HORIZONTAL_PULL"));
    }

    static ConservationReport variante(String guard, List<Long> antes, List<PlanDraft.DraftExercise> despues) {
        return new ConservationValidator("multiconjunto", guard).validate(
                derivacion(despues, List.of(ej(9001L, 3, 10))), semana("TOO_DIFFICULT", antes),
                orden(TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA), CATALOGO, ExclusionesDeSeguridad.ninguna(),
                List.of(), false, 30);
    }

    static ConservationReport variante(String guard, List<Long> antes, Long... despues) {
        return variante(guard, antes, Stream.of(despues).map(id -> ej(id, 3, 10)).toList());
    }

    static boolean poDificultad(ConservationReport r) {
        return r.problems().stream().anyMatch(p -> p.contains("limite de dificultad"));
    }

    /**
     * escenario, retirados, anadidos, aceptable con el limite (B y C). Con A (off) todos son aceptables: el limite es
     * lo unico que cambia.
     */
    static Stream<Arguments> escenarios() {
        return Stream.of(
                Arguments.of("01 dificultad 3 -> 2 (70301 -> 56)", List.of(70301L, 360L), List.of(56L, 360L), true, Set.of()),
                Arguments.of("02 dificultad 2 -> 1 (380 -> 72)", List.of(380L, 360L), List.of(72L, 360L), true, Set.of()),
                Arguments.of("03 dificultad 2 -> 2 (380 -> 56): la igualdad sigue permitida (no H12-A)",
                        List.of(380L, 360L), List.of(56L, 360L), true, Set.of()),
                Arguments.of("04 dificultad 1 -> 1 (933 -> 73)", List.of(933L, 360L), List.of(73L, 360L), true, Set.of()),
                Arguments.of("05 dificultad 1 -> 2 (580 -> 56): rechazado", List.of(580L, 360L), List.of(56L, 360L),
                        false, Set.of(56L)),
                Arguments.of("06 dificultad 2 -> 3 (380 -> 70301): rechazado", List.of(380L, 360L),
                        List.of(70301L, 360L), false, Set.of(70301L)),
                Arguments.of("07 varias sustituciones validas del mismo patron: {3,1} -> {2,1}",
                        List.of(70301L, 933L, 360L), List.of(56L, 73L, 360L), true, Set.of()),
                Arguments.of("08 dos retirados de patrones distintos, {HP 2, HPULL 1} -> {HP 1, HPULL 2}: maximo y"
                                + " promedio iguales, pero el HORIZONTAL_PULL sube: rechazado (emparejamiento por patron)",
                        List.of(380L, 300L, 360L), List.of(72L, 780L, 360L), false, Set.of(780L)),
                Arguments.of("09 dos sustitutos del mismo patron, {3,1} -> {2,2}: promedio igual, pero no hay"
                                + " emparejamiento valido: rechazado", List.of(70301L, 933L, 360L),
                        List.of(56L, 380L, 360L), false, Set.of(380L, 56L)),
                Arguments.of("10 un sustituto valido y otro mas dificil, {2,1} -> {1,3}: rechazado solo por el de 3",
                        List.of(380L, 933L, 360L), List.of(72L, 70301L, 360L), false, Set.of(70301L)),
                Arguments.of("07b emparejamiento correcto aunque el orden de la lista no coincida, {2,1} -> {1,2}",
                        List.of(380L, 933L, 360L), List.of(72L, 56L, 360L), true, Set.of()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("escenarios")
    @DisplayName("A (off) acepta; B (on) y C (preferencia) aplican el limite por emparejamiento")
    void escenario(String nombre, List<Long> antes, List<Long> despues, boolean aceptableConLimite, Set<Long> culpables) {
        var a = variante("off", antes, despues.toArray(Long[]::new));
        assertTrue(a.aceptable(), () -> "A (off) debe aceptar como hoy: " + a.problems());
        for (var guard : List.of("on", "preferencia")) {
            var r = variante(guard, antes, despues.toArray(Long[]::new));
            assertEquals(aceptableConLimite, r.aceptable(), () -> guard + ": " + r.problems());
            if (!aceptableConLimite) {
                // Solo por el limite (sin el, se acepta) y el mensaje nombra exactamente al sustituto que sube.
                var porLimite = r.problems().stream().filter(p -> p.contains("limite de dificultad")).toList();
                assertEquals(r.problems(), porLimite, "solo el limite rechaza: " + r.problems());
                for (var id : despues) {
                    boolean nombrado = porLimite.stream().anyMatch(p -> p.contains("por el ejercicio " + id + ","));
                    if (culpables.contains(id) && culpables.size() == 1) assertTrue(nombrado, porLimite::toString);
                    if (!culpables.contains(id)) assertFalse(nombrado, () -> id + " no sube: " + porLimite);
                }
            } else assertFalse(poDificultad(r), r.problems()::toString);
        }
    }

    @ParameterizedTest(name = "guard={0}")
    @ValueSource(strings = {"off", "on", "preferencia"})
    @DisplayName("12 reduccion de dosis sin sustituir (series 3 -> 2): permitida con cualquier estrategia")
    void reduccionDeDosis(String guard) {
        var r = variante(guard, List.of(380L, 360L), List.of(ej(380L, 2, 10), ej(360L, 3, 10)));
        assertTrue(r.aceptable(), r.problems()::toString);
    }

    @ParameterizedTest(name = "guard={0}")
    @ValueSource(strings = {"off", "on", "preferencia"})
    @DisplayName("12b sustituto igual de dificil y ademas menos series: el limite no lo impide")
    void sustitucionYReduccion(String guard) {
        var r = variante(guard, List.of(380L, 360L), List.of(ej(56L, 2, 10), ej(360L, 3, 10)));
        assertFalse(poDificultad(r), r.problems()::toString);
        assertEquals(variante("off", List.of(380L, 360L), List.of(ej(56L, 2, 10), ej(360L, 3, 10))).problems(),
                r.problems(), "la estrategia no cambia el veredicto de las demas reglas");
    }

    @ParameterizedTest(name = "guard={0}")
    @ValueSource(strings = {"off", "on", "preferencia"})
    @DisplayName("H1 sigue actuando con el limite: dos del patron de A en lugar de A y B se rechaza por patron")
    void h1SigueActuando(String guard) {
        // 380 (HP 2) y 389 (VPULL 2) -> 72 (HP 1) y 56 (HP 2): la dificultad no sube, pero se pierde VERTICAL_PULL.
        var r = variante(guard, List.of(380L, 389L, 360L), 72L, 56L, 360L);
        assertFalse(r.aceptable());
        assertFalse(poDificultad(r), "no es el limite quien rechaza: " + r.problems());
    }
}
