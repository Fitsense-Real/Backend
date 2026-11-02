package main.web.services.fitsense.planning.domain.services.internal;

import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.GoalCapabilityPolicy.Capability;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.planning.domain.services.CatalogEligibilityPolicy;
import main.web.services.fitsense.planning.domain.services.CatalogEligibilityPolicy.Decision;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static main.web.services.fitsense.planning.domain.services.CatalogEligibilityPolicy.Decision.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Las familias que aparecen en el catalogo real (V1..V33 aplicadas), con un
 * ejemplo de cada una. Los ids y zonas son los del catalogo.
 */
class CatalogEligibilityPolicyTest {

    private static CandidateExercise ej(long id, String nombre, String zona, String patron) {
        return new CandidateExercise(id, nombre, zona, "body weight", 1,
                PrescriptionType.DURATION, null, patron);
    }

    /** 20 en el catalogo: zona cardio, patron LOCOMOTION_CARDIO. */
    static final CandidateExercise CARDIO_ZONA_CARDIO = ej(901, "Saltos de tijera", "cardio", "LOCOMOTION_CARDIO");
    /** 37: todos los que dicen «estiramiento» son MOBILITY. */
    static final CandidateExercise ESTIRAMIENTO = ej(902, "Estiramiento de cuadriceps", "upper legs", "MOBILITY");
    /** 4 MOBILITY de neck y lower arms. */
    static final CandidateExercise MOVILIDAD_CUELLO = ej(903, "Estiramiento lateral de cuello", "neck", "MOBILITY");
    /** 4 KNEE_DOMINANT y 1 de tronco tienen zona cardio: no son ruta de cardio. */
    static final CandidateExercise FUERZA_ZONA_CARDIO = ej(904, "Sentadilla con salto", "cardio", "KNEE_DOMINANT");
    /** 31 WRIST_FOREARM en lower arms. */
    static final CandidateExercise ANTEBRAZO = ej(905, "Curl de muneca", "lower arms", "WRIST_FOREARM");
    /** Ya elegibles antes del Paso 2: 8 LOCOMOTION_CARDIO fuera de la zona cardio. */
    static final CandidateExercise LOCOMOCION_YA_ELEGIBLE = ej(906, "Escaladores", "waist", "LOCOMOTION_CARDIO");
    /** Ya elegibles antes del Paso 2: 8 MOBILITY sin «estiramiento» en el nombre. */
    static final CandidateExercise MOVILIDAD_YA_ELEGIBLE = ej(55, "Circulos de tobillo", "lower legs", "MOBILITY");
    static final CandidateExercise FUERZA = ej(907, "Sentadilla", "upper legs", "KNEE_DOMINANT");

    static Stream<Arguments> decisiones() {
        return Stream.of(
                // objetivo, candidato, decision esperada
                Arguments.of("GAIN_MUSCLE", CARDIO_ZONA_CARDIO, EXCLUIDO_FUERA_DE_ENFOQUE),
                Arguments.of("INCREASE_STRENGTH", CARDIO_ZONA_CARDIO, EXCLUIDO_FUERA_DE_ENFOQUE),
                Arguments.of("LOSE_WEIGHT", CARDIO_ZONA_CARDIO, ADMITIDO_POR_CAPACIDAD),
                Arguments.of("GENERAL_FITNESS", CARDIO_ZONA_CARDIO, ADMITIDO_POR_CAPACIDAD),

                Arguments.of("GAIN_MUSCLE", ESTIRAMIENTO, EXCLUIDO_ESTIRAMIENTO),
                Arguments.of("INCREASE_STRENGTH", ESTIRAMIENTO, EXCLUIDO_ESTIRAMIENTO),
                Arguments.of("LOSE_WEIGHT", ESTIRAMIENTO, EXCLUIDO_ESTIRAMIENTO),
                Arguments.of("GENERAL_FITNESS", ESTIRAMIENTO, ADMITIDO_POR_CAPACIDAD),

                Arguments.of("GAIN_MUSCLE", MOVILIDAD_CUELLO, EXCLUIDO_FUERA_DE_ENFOQUE),
                Arguments.of("LOSE_WEIGHT", MOVILIDAD_CUELLO, EXCLUIDO_FUERA_DE_ENFOQUE),
                Arguments.of("GENERAL_FITNESS", MOVILIDAD_CUELLO, ADMITIDO_POR_CAPACIDAD),

                Arguments.of("LOSE_WEIGHT", FUERZA_ZONA_CARDIO, EXCLUIDO_FUERA_DE_ENFOQUE),
                Arguments.of("GENERAL_FITNESS", FUERZA_ZONA_CARDIO, EXCLUIDO_FUERA_DE_ENFOQUE),
                Arguments.of("GENERAL_FITNESS", ANTEBRAZO, EXCLUIDO_FUERA_DE_ENFOQUE),

                Arguments.of("GAIN_MUSCLE", LOCOMOCION_YA_ELEGIBLE, ADMITIDO),
                Arguments.of("LOSE_WEIGHT", LOCOMOCION_YA_ELEGIBLE, ADMITIDO),
                Arguments.of("GAIN_MUSCLE", MOVILIDAD_YA_ELEGIBLE, ADMITIDO),
                Arguments.of("GENERAL_FITNESS", MOVILIDAD_YA_ELEGIBLE, ADMITIDO),
                Arguments.of("INCREASE_STRENGTH", FUERZA, ADMITIDO),
                Arguments.of("GENERAL_FITNESS", FUERZA, ADMITIDO));
    }

    @ParameterizedTest(name = "{0} · {1} -> {2}")
    @MethodSource("decisiones")
    void decisionPorObjetivo(String objetivo, CandidateExercise candidato, Decision esperada) {
        assertEquals(esperada, CatalogEligibilityPolicy.decidir(candidato, objetivo));
        assertEquals(esperada == ADMITIDO_POR_CAPACIDAD,
                CatalogEligibilityPolicy.esComplemento(candidato, objetivo));
    }

    /**
     * Sin regresion: todo lo que el filtro anterior admitia se sigue admitiendo
     * como trabajo normal, en los cuatro objetivos, y nunca es complemento.
     */
    @ParameterizedTest
    @ValueSource(strings = {"GAIN_MUSCLE", "INCREASE_STRENGTH", "LOSE_WEIGHT", "GENERAL_FITNESS"})
    void loQueYaEraElegibleNoCambia(String objetivo) {
        for (var c : List.of(LOCOMOCION_YA_ELEGIBLE, MOVILIDAD_YA_ELEGIBLE, FUERZA)) {
            assertTrue(CatalogEligibilityPolicy.exclusionHeredada(c).isEmpty());
            assertEquals(ADMITIDO, CatalogEligibilityPolicy.decidir(c, objetivo));
        }
    }

    /** Los objetivos sin cardio ni movilidad quedan exactamente como antes. */
    @ParameterizedTest
    @ValueSource(strings = {"GAIN_MUSCLE", "INCREASE_STRENGTH"})
    void objetivosSinCardioNiMovilidadConservanElFiltroAnterior(String objetivo) {
        for (var c : List.of(CARDIO_ZONA_CARDIO, ESTIRAMIENTO, MOVILIDAD_CUELLO, FUERZA_ZONA_CARDIO,
                ANTEBRAZO, LOCOMOCION_YA_ELEGIBLE, MOVILIDAD_YA_ELEGIBLE, FUERZA)) {
            boolean antes = CatalogEligibilityPolicy.exclusionHeredada(c).isEmpty();
            assertEquals(antes, CatalogEligibilityPolicy.decidir(c, objetivo).admite(), c.name());
        }
    }

    @Test
    void laAdmisionLaDecideElPatronNoElNombreNiLaZona() {
        // Nombre de cardio, zona cardio, patron de fuerza: no entra.
        var nombreEnganoso = ej(910, "Carrera de cardio", "cardio", "KNEE_DOMINANT");
        assertEquals(EXCLUIDO_FUERA_DE_ENFOQUE,
                CatalogEligibilityPolicy.decidir(nombreEnganoso, "LOSE_WEIGHT"));
        // «Estiramiento» en el nombre pero patron de fuerza: tampoco.
        var estiramientoQueNoEsMovilidad = ej(911, "Estiramiento con press", "chest", "HORIZONTAL_PUSH");
        assertEquals(EXCLUIDO_ESTIRAMIENTO,
                CatalogEligibilityPolicy.decidir(estiramientoQueNoEsMovilidad, "GENERAL_FITNESS"));
        // Sin patron: no hay ruta.
        assertEquals(EXCLUIDO_FUERA_DE_ENFOQUE,
                CatalogEligibilityPolicy.decidir(ej(912, "x", "cardio", null), "LOSE_WEIGHT"));
    }

    @Test
    void lasRutasSonLasDeLaMatriz4A() {
        assertEquals(Map.of("LOCOMOTION_CARDIO", Capability.CARDIO, "MOBILITY", Capability.MOBILITY),
                CatalogEligibilityPolicy.RUTAS);
        assertTrue(CatalogEligibilityPolicy.capacidadesDe("LOSE_WEIGHT").contains(Capability.CARDIO));
        assertFalse(CatalogEligibilityPolicy.capacidadesDe("LOSE_WEIGHT").contains(Capability.MOBILITY));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"ENDURANCE"})
    void objetivoDesconocidoUsaSoloElFiltroAnterior(String objetivo) {
        assertEquals(EXCLUIDO_FUERA_DE_ENFOQUE, CatalogEligibilityPolicy.decidir(CARDIO_ZONA_CARDIO, objetivo));
        assertEquals(EXCLUIDO_ESTIRAMIENTO, CatalogEligibilityPolicy.decidir(ESTIRAMIENTO, objetivo));
    }

    @Test
    void informeCuentaPorDecisionYCapacidad() {
        var informe = CatalogEligibilityPolicy.informe(List.of(CARDIO_ZONA_CARDIO, ESTIRAMIENTO,
                MOVILIDAD_CUELLO, FUERZA_ZONA_CARDIO, FUERZA), "GENERAL_FITNESS");
        assertEquals(1, informe.porDecision().get(ADMITIDO));
        assertEquals(3, informe.porDecision().get(ADMITIDO_POR_CAPACIDAD));
        assertEquals(1, informe.porDecision().get(EXCLUIDO_FUERA_DE_ENFOQUE));
        assertEquals(Map.of(Capability.CARDIO, 1, Capability.MOBILITY, 2), informe.porCapacidad());
    }
}
