package main.web.services.fitsense.planning.domain.services.internal;

import main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams;
import main.web.services.fitsense.planning.domain.exceptions.InsufficientSlotCoverageException;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Los cinco casos de cobertura 4C que pidio Juan, mas los bordes del mapa y la
 * comprobacion de que nada de esto toca la dosis.
 * <p>
 * Son pruebas de DOMINIO PURO: ni Spring, ni base de datos, ni Replicate.
 * Ejercitan el {@link PlanDraftValidator} y el {@link SlotCoverageGate} reales,
 * no una reimplementacion suya, que es la unica forma de que probar signifique
 * algo. Corren con {@code mvn -Dtest=SlotCoverage4CTest test} en segundos.
 * <p>
 * Hay un arnes equivalente en {@code arnes/Cobertura4C.java} que corre con solo
 * {@code javac} y {@code java}, para cuando Maven no esta disponible.
 */
class SlotCoverage4CTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 10, 12);
    private static final long USUARIO = 1L;

    // ------------------------------------------------------------- utiles

    /** Candidato con patron de movimiento. La dificultad y el tipo no importan. */
    private static CandidateExercise ej(long id, String zona, String patron) {
        return new CandidateExercise(id, "ejercicio " + id, zona, "body weight", 1,
                PrescriptionType.SETS_REPS, null, patron);
    }

    /** El perfil HOME/BEGINNER/GAIN_MUSCLE del plan 3, que es el medido. */
    private static PlanningProfile perfil(int dias, int minutos) {
        return new PlanningProfile(USUARIO, 30, "MALE", new BigDecimal("175.0"),
                new BigDecimal("72.0"), new BigDecimal("75.0"), "BEGINNER",
                "GAIN_MUSCLE", "Ganar masa muscular entrenando en casa", "Ninguna",
                "HOME", dias, List.of((short) 1, (short) 3, (short) 5), minutos,
                List.of("body weight", "dumbbell", "band"), List.of());
    }

    private static PlanGenerationContext contexto(List<CandidateExercise> elegibles,
                                                  int dias, int minutos) {
        return new PlanGenerationContext(USUARIO, (short) 1, LUNES, LUNES.plusDays(6),
                perfil(dias, minutos), PlanAdjustment.none(), null, elegibles,
                SafetyProfile.none(), (PrescriptionParams) null);
    }

    private static PlanDraft.DraftExercise prescrito(long id) {
        return new PlanDraft.DraftExercise(id, PrescriptionType.SETS_REPS,
                (short) 2, (short) 10, null, null, (short) 75, null);
    }

    private static PlanDraft borrador(GenerationSource origen, List<List<Long>> porDia) {
        var entrenos = new ArrayList<PlanDraft.DraftWorkout>();
        for (int i = 0; i < porDia.size(); i++) {
            entrenos.add(new PlanDraft.DraftWorkout(LUNES.plusDays(i * 2L),
                    WorkoutFocus.FULL_BODY, "FULL_BODY - Sesion " + (i + 1), 45,
                    porDia.get(i).stream()
                            .map(SlotCoverage4CTest::prescrito).toList()));
        }
        return new PlanDraft(origen, origen == GenerationSource.AI ? "gpt-5-nano" : null,
                "plan de prueba", null, "prueba", entrenos);
    }

    private static PlanDraft borradorIa(List<List<Long>> porDia) {
        return borrador(GenerationSource.AI, porDia);
    }

    private static Map<Long, CandidateExercise> indice(List<CandidateExercise> elegibles) {
        return elegibles.stream().collect(
                Collectors.toMap(CandidateExercise::exerciseId, c -> c, (a, b) -> a));
    }

    /**
     * Solo los problemas de cobertura. El borrador de prueba incumple a
     * proposito otras validaciones -no es un plan completo- y filtrar por el
     * prefijo mantiene cada prueba sobre una sola cosa.
     */
    private static List<String> problemasV23(PlanDraftValidator validador,
                                             PlanDraft draft,
                                             PlanGenerationContext ctx) {
        try {
            validador.validate(draft, ctx, 30);
            return List.of();
        } catch (InvalidPlanDraftException e) {
            return e.problems().stream().filter(p -> p.startsWith("V23:")).toList();
        }
    }

    private static PlanDraftValidator validador(SlotCoverageEnforcement modo) {
        return new PlanDraftValidator(new SessionDurationEstimator(), modo);
    }

    // --------------------------------------------------------- conjuntos

    private static List<CandidateExercise> elegiblesCompleto() {
        var lista = new ArrayList<CandidateExercise>();
        long id = 100;
        for (int i = 0; i < 4; i++) lista.add(ej(id++, "upper legs", "KNEE_DOMINANT"));
        for (int i = 0; i < 4; i++) lista.add(ej(id++, "upper legs", "HIP_HINGE"));
        for (int i = 0; i < 4; i++) lista.add(ej(id++, "chest", "HORIZONTAL_PUSH"));
        for (int i = 0; i < 4; i++) lista.add(ej(id++, "back", "HORIZONTAL_PULL"));
        for (int i = 0; i < 6; i++) lista.add(ej(id++, "upper arms", "ELBOW_FLEXION"));
        for (int i = 0; i < 3; i++) lista.add(ej(id++, "waist", "CORE_STABILITY"));
        return lista;
    }

    private static List<CandidateExercise> elegiblesSinPull() {
        return elegiblesCompleto().stream()
                .filter(c -> !"HORIZONTAL_PULL".equals(c.movementPattern()))
                .toList();
    }

    private static List<CandidateExercise> elegiblesFragil() {
        var lista = new ArrayList<CandidateExercise>();
        long id = 300;
        for (int i = 0; i < 5; i++) lista.add(ej(id++, "upper legs", "KNEE_DOMINANT"));
        lista.add(ej(id++, "upper legs", "HIP_EXTENSION"));            // uno solo
        for (int i = 0; i < 5; i++) lista.add(ej(id++, "chest", "HORIZONTAL_PUSH"));
        for (int i = 0; i < 5; i++) lista.add(ej(id++, "back", "VERTICAL_PULL"));
        return lista;
    }

    // ============================================================ CASO 1

    @Nested
    @DisplayName("Caso 1: un slot sin ningun ejercicio elegible")
    class SlotVacio {

        @Test
        @DisplayName("el veredicto de 4C §6 es NO FACTIBLE y nombra el slot vacio")
        void veredictoNoFactible() {
            var cobertura = SlotCoveragePolicy.ofEligible(elegiblesSinPull());

            assertEquals(SlotCoverage.Verdict.NO_FACTIBLE, cobertura.verdict());
            assertEquals(List.of(MandatorySlot.UPPER_PULL), cobertura.emptySlots());
            assertEquals(4, cobertura.count(MandatorySlot.UPPER_PUSH),
                    "los otros slots se siguen contando");
            assertEquals(9, cobertura.outsideSlots(),
                    "accesorios y core no son un problema, pero se cuentan");
            assertEquals(0, cobertura.unclassified());
        }

        @Test
        @DisplayName("la comprobacion previa lanza en los modos ENFORCE_*")
        void previaLanza() {
            var e = assertThrows(InsufficientSlotCoverageException.class,
                    () -> SlotCoverageGate.evaluateBeforeGeneration(USUARIO,
                            elegiblesSinPull(),
                            SlotCoverageEnforcement.ENFORCE_WHEN_SATISFIABLE));

            assertEquals(List.of(MandatorySlot.UPPER_PULL), e.emptySlots());
            assertTrue(e.getMessage().contains("4C"),
                    "el mensaje tiene que decir de que criterio viene");
        }

        @Test
        @DisplayName("en OBSERVE mide y no lanza; en OFF no mide")
        void previaNoLanza() {
            var cobertura = assertDoesNotThrow(() ->
                    SlotCoverageGate.evaluateBeforeGeneration(USUARIO, elegiblesSinPull(),
                            SlotCoverageEnforcement.OBSERVE));
            assertNotNull(cobertura);
            assertEquals(SlotCoverage.Verdict.NO_FACTIBLE, cobertura.verdict());

            assertNull(SlotCoverageGate.evaluateBeforeGeneration(USUARIO,
                            elegiblesSinPull(), SlotCoverageEnforcement.OFF),
                    "en OFF no se ha medido nada, y devolver ceros seria mentir");
        }

        @Test
        @DisplayName("el validador NO exige un slot que el conjunto elegible no tiene")
        void validadorNoExigeLoImposible() {
            var elegibles = elegiblesSinPull();
            var draft = borradorIa(List.of(
                    List.of(100L, 104L, 108L), List.of(101L, 108L, 118L)));

            assertEquals(List.of(),
                    problemasV23(validador(SlotCoverageEnforcement.ENFORCE_WHEN_SATISFIABLE),
                            draft, contexto(elegibles, 2, 45)),
                    "rechazar aqui dejaria la semana sin plan por algo que el "
                            + "generador no puede arreglar");
        }
    }

    // ============================================================ CASO 2

    @Nested
    @DisplayName("Caso 2: cobertura fragil (1 o 2 opciones en algun slot)")
    class CoberturaFragil {

        @Test
        @DisplayName("el veredicto es FRAGIL y los cuatro slots estan cubiertos")
        void veredictoFragil() {
            var cobertura = SlotCoveragePolicy.ofEligible(elegiblesFragil());

            assertEquals(SlotCoverage.Verdict.FRAGIL, cobertura.verdict());
            assertEquals(1, cobertura.count(MandatorySlot.LOWER_HIP));
            assertEquals(List.of(MandatorySlot.LOWER_HIP), cobertura.fragileSlots());
            assertEquals(List.of(), cobertura.emptySlots());
        }

        @Test
        @DisplayName("FRAGIL no bloquea en NINGUN modo: 4C §6 lo declara factible")
        void fragilNuncaBloquea() {
            for (var modo : SlotCoverageEnforcement.values()) {
                assertDoesNotThrow(() -> SlotCoverageGate.evaluateBeforeGeneration(
                                USUARIO, elegiblesFragil(), modo),
                        "FRAGIL bloqueo en modo " + modo);
            }
        }

        @Test
        @DisplayName("el slot peor cubierto manda, no el promedio")
        void mandaElPeor() {
            var lista = new ArrayList<CandidateExercise>();
            long id = 900;
            for (int i = 0; i < 20; i++) lista.add(ej(id++, "upper legs", "KNEE_DOMINANT"));
            for (int i = 0; i < 20; i++) lista.add(ej(id++, "chest", "HORIZONTAL_PUSH"));
            for (int i = 0; i < 20; i++) lista.add(ej(id++, "back", "HORIZONTAL_PULL"));
            lista.add(ej(id, "upper legs", "HIP_HINGE"));

            assertEquals(SlotCoverage.Verdict.FRAGIL,
                    SlotCoveragePolicy.ofEligible(lista).verdict(),
                    "61 ejercicios con un solo HIP_HINGE siguen siendo fragiles");
        }
    }

    // ============================================================ CASO 3

    @Nested
    @DisplayName("Caso 3: cobertura completa")
    class CoberturaCompleta {

        @Test
        @DisplayName("ROBUSTO, y una semana que toca los cuatro slots no da problemas")
        void semanaBuenaPasa() {
            var elegibles = elegiblesCompleto();
            assertEquals(SlotCoverage.Verdict.ROBUSTO,
                    SlotCoveragePolicy.ofEligible(elegibles).verdict());

            var draft = borradorIa(List.of(
                    List.of(100L, 104L, 108L), List.of(101L, 105L, 112L)));

            assertEquals(List.of(),
                    problemasV23(validador(SlotCoverageEnforcement.ENFORCE_WHEN_SATISFIABLE),
                            draft, contexto(elegibles, 2, 45)));
        }

        @Test
        @DisplayName("si la semana se salta un slot que SI habia, se rechaza y se explica")
        void semanaIncompletaSeRechaza() {
            var elegibles = elegiblesCompleto();
            // Tres de rodilla, dos de empuje, uno de traccion: falta LOWER_HIP.
            var draft = borradorIa(List.of(
                    List.of(100L, 101L, 108L), List.of(102L, 109L, 112L)));

            var problemas = problemasV23(
                    validador(SlotCoverageEnforcement.ENFORCE_WHEN_SATISFIABLE),
                    draft, contexto(elegibles, 2, 45));

            assertEquals(1, problemas.size());
            var mensaje = problemas.get(0);
            assertTrue(mensaje.contains("LOWER_HIP"), "tiene que nombrar el slot");
            assertTrue(mensaje.contains("sin tocar series"),
                    "el mensaje que recibe la IA tiene que prohibir tocar la dosis");
            assertTrue(mensaje.contains("LOWER_HIP=4"),
                    "y decir cuantos elegibles hay para ese slot");
        }

        @Test
        @DisplayName("en OBSERVE y en OFF la misma semana mala no se rechaza")
        void observeNoRechaza() {
            var elegibles = elegiblesCompleto();
            var draft = borradorIa(List.of(
                    List.of(100L, 101L, 108L), List.of(102L, 109L, 112L)));

            assertEquals(List.of(), problemasV23(validador(SlotCoverageEnforcement.OBSERVE),
                    draft, contexto(elegibles, 2, 45)));
            assertEquals(List.of(), problemasV23(validador(SlotCoverageEnforcement.OFF),
                    draft, contexto(elegibles, 2, 45)));
        }
    }

    // ============================================================ CASO 4

    @Nested
    @DisplayName("Caso 4: reintento de la IA")
    class ReintentoIa {

        @Test
        @DisplayName("el motivo viaja en la excepcion, que es lo que recibe el reintento")
        void elMotivoLlegaAlReintento() {
            var elegibles = elegiblesCompleto();
            // Falta UPPER_PULL.
            var intento1 = borradorIa(List.of(
                    List.of(100L, 104L, 108L), List.of(101L, 105L, 109L)));
            var ctx = contexto(elegibles, 2, 45);
            var validador = validador(SlotCoverageEnforcement.ENFORCE_AI_ONLY);

            var e = assertThrows(InvalidPlanDraftException.class,
                    () -> validador.validate(intento1, ctx, 30));

            var cobertura = e.problems().stream()
                    .filter(p -> p.startsWith("V23:")).toList();
            assertEquals(1, cobertura.size());
            assertTrue(cobertura.get(0).contains("UPPER_PULL"));

            // El pipeline pasa e.problems() a AiRetryFeedback tal cual: el texto
            // que se comprueba aqui es literalmente el que lee el modelo.
            var intento2 = borradorIa(List.of(
                    List.of(100L, 104L, 108L), List.of(112L, 105L, 109L)));

            assertEquals(List.of(), problemasV23(validador, intento2, ctx),
                    "el intento corregido pasa la cobertura");
            assertEquals(intento1.volume(30), intento2.volume(30),
                    "y corregir la cobertura no cambia el volumen total");
        }
    }

    // ============================================================ CASO 5

    @Nested
    @DisplayName("Caso 5: el borrador del motor de reglas")
    class FalloDelRespaldo {

        @Test
        @DisplayName("ENFORCE_AI_ONLY no rechaza al respaldo: no tiene reintento")
        void aiOnlyPerdonaAlRespaldo() {
            var elegibles = elegiblesCompleto();
            var draft = borrador(GenerationSource.RULE_ENGINE, List.of(
                    List.of(100L, 101L, 108L), List.of(102L, 109L, 112L)));

            assertEquals(List.of(),
                    problemasV23(validador(SlotCoverageEnforcement.ENFORCE_AI_ONLY),
                            draft, contexto(elegibles, 2, 45)),
                    "rechazar el respaldo deja la semana sin plan: medido, el motor "
                            + "de reglas cubre los cuatro slots en 30 de 90 semanas");
        }

        @Test
        @DisplayName("ENFORCE_WHEN_SATISFIABLE si lo rechaza, y por eso no se entrega activo")
        void whenSatisfiableSiRechazaAlRespaldo() {
            var elegibles = elegiblesCompleto();
            var draft = borrador(GenerationSource.RULE_ENGINE, List.of(
                    List.of(100L, 101L, 108L), List.of(102L, 109L, 112L)));

            assertEquals(1,
                    problemasV23(validador(SlotCoverageEnforcement.ENFORCE_WHEN_SATISFIABLE),
                            draft, contexto(elegibles, 2, 45)).size());
        }

        @Test
        @DisplayName("una semana sin posiciones para los cuatro slots no se rechaza")
        void loQueNoCabeNoSeRechaza() {
            var elegibles = elegiblesCompleto();
            // Dos sesiones de dos y uno: tres posiciones para cuatro slots. Es lo
            // que deja REDUCE_VOLUME con REDUCE_DAYS.
            var draft = borrador(GenerationSource.RULE_ENGINE,
                    List.of(List.of(100L, 104L), List.of(108L)));
            assertEquals(3, SlotCoveragePolicy.exercisePositions(draft));

            assertEquals(List.of(),
                    problemasV23(validador(SlotCoverageEnforcement.ENFORCE_WHEN_SATISFIABLE),
                            draft, contexto(elegibles, 2, 30)),
                    "no se puede cumplir por aritmetica, no por mala eleccion");

            assertEquals(1,
                    problemasV23(validador(SlotCoverageEnforcement.ENFORCE_ALWAYS),
                            draft, contexto(elegibles, 2, 30)).size(),
                    "ENFORCE_ALWAYS si lo rechaza, y por eso no es el valor por omision");
        }

        @Test
        @DisplayName("una semana corta que SI cubre los cuatro slots pasa")
        void semanaCortaCompletaPasa() {
            var elegibles = elegiblesCompleto();
            var draft = borrador(GenerationSource.RULE_ENGINE,
                    List.of(List.of(100L, 104L), List.of(108L, 112L)));

            assertEquals(List.of(),
                    problemasV23(validador(SlotCoverageEnforcement.ENFORCE_WHEN_SATISFIABLE),
                            draft, contexto(elegibles, 2, 30)));
        }
    }

    // ================================================== bordes del mapa

    @Nested
    @DisplayName("El mapa de patrones y los datos degradados")
    class BordesDelMapa {

        @Test
        @DisplayName("los dos patrones de cada slot cuentan para el mismo slot")
        void patronesPorSlot() {
            assertEquals(MandatorySlot.LOWER_HIP,
                    MandatorySlot.ofMovementPattern("HIP_EXTENSION").orElseThrow());
            assertEquals(MandatorySlot.LOWER_HIP,
                    MandatorySlot.ofMovementPattern("HIP_HINGE").orElseThrow());
            assertEquals(MandatorySlot.UPPER_PUSH,
                    MandatorySlot.ofMovementPattern("VERTICAL_PUSH").orElseThrow());
            assertEquals(MandatorySlot.UPPER_PULL,
                    MandatorySlot.ofMovementPattern("VERTICAL_PULL").orElseThrow());
        }

        @Test
        @DisplayName("las 17 clases que no son de slot no pertenecen a ninguno")
        void clasesFueraDeSlot() {
            assertTrue(MandatorySlot.ofMovementPattern("ELBOW_FLEXION").isEmpty());
            assertTrue(MandatorySlot.ofMovementPattern("CORE_STABILITY").isEmpty());
            assertTrue(MandatorySlot.ofMovementPattern("LOCOMOTION_CARDIO").isEmpty());
            assertTrue(MandatorySlot.ofMovementPattern("MOBILITY").isEmpty());
        }

        @Test
        @DisplayName("patron nulo o vacio no es ningun slot, y se tolera el formato")
        void patronNuloYFormato() {
            assertTrue(MandatorySlot.ofMovementPattern(null).isEmpty());
            assertTrue(MandatorySlot.ofMovementPattern("  ").isEmpty());
            assertEquals(MandatorySlot.LOWER_KNEE,
                    MandatorySlot.ofMovementPattern(" knee_dominant ").orElseThrow());
        }

        @Test
        @DisplayName("los ejercicios sin patron se cuentan aparte, no dentro de fuera_de_slot")
        void sinPatronSeCuentaAparte() {
            var lista = new ArrayList<>(elegiblesCompleto());
            lista.add(ej(9999L, "waist", null));

            var cobertura = SlotCoveragePolicy.ofEligible(lista);
            assertEquals(1, cobertura.unclassified());
            assertEquals(9, cobertura.outsideSlots());
            assertTrue(cobertura.describe().contains("SIN_PATRON"),
                    "el dato degradado tiene que verse en el log");
        }

        @Test
        @DisplayName("conjunto vacio o nulo: NO FACTIBLE, sin reventar")
        void conjuntoVacio() {
            assertEquals(SlotCoverage.Verdict.NO_FACTIBLE,
                    SlotCoveragePolicy.ofEligible(List.of()).verdict());
            assertEquals(SlotCoverage.Verdict.NO_FACTIBLE,
                    SlotCoveragePolicy.ofEligible(null).verdict());
        }

        @Test
        @DisplayName("un modo mal escrito, vacio o nulo cae en OBSERVE y no bloquea")
        void modoTolerante() {
            assertEquals(SlotCoverageEnforcement.OBSERVE,
                    SlotCoverageEnforcement.parse("ENFORZAR"));
            assertEquals(SlotCoverageEnforcement.OBSERVE,
                    SlotCoverageEnforcement.parse(""));
            assertEquals(SlotCoverageEnforcement.OBSERVE,
                    SlotCoverageEnforcement.parse(null));
            assertFalse(SlotCoverageEnforcement.OBSERVE.bloquea());
            assertTrue(SlotCoverageEnforcement.OBSERVE.mide());
            assertFalse(SlotCoverageEnforcement.OFF.mide());
        }

        @Test
        @DisplayName("el que se entrega por omision mide y no rechaza")
        void porOmisionNoRechaza() {
            var porOmision = new PlanDraftValidator(new SessionDurationEstimator(),
                    (SlotCoverageEnforcement) null).coverageEnforcement();

            assertEquals(SlotCoverageEnforcement.OBSERVE, porOmision);
            assertFalse(porOmision.bloquea(),
                    "la implementacion entra; activarla es una decision aparte");
        }
    }

    // ============================================ no se toca la dosis

    @Nested
    @DisplayName("La cobertura no toca la dosis")
    class NoTocaLaDosis {

        @Test
        @DisplayName("ningun modo cambia series, repeticiones, descanso ni volumen")
        void dosisIntacta() {
            var elegibles = elegiblesCompleto();
            var draft = borradorIa(List.of(
                    List.of(100L, 104L, 108L), List.of(101L, 105L, 112L)));
            int volumenAntes = draft.volume(30);
            var seriesAntes = draft.workouts().stream()
                    .flatMap(w -> w.exercises().stream())
                    .map(PlanDraft.DraftExercise::plannedSets).toList();

            for (var modo : SlotCoverageEnforcement.values()) {
                problemasV23(validador(modo), draft, contexto(elegibles, 2, 45));
            }

            assertEquals(volumenAntes, draft.volume(30));
            assertEquals(seriesAntes, draft.workouts().stream()
                    .flatMap(w -> w.exercises().stream())
                    .map(PlanDraft.DraftExercise::plannedSets).toList());
        }

        @Test
        @DisplayName("la cuenta del borrador cuenta posiciones, no ejercicios distintos")
        void cuentaPosiciones() {
            var elegibles = elegiblesCompleto();
            // El mismo 100 el lunes y el miercoles.
            var draft = borradorIa(List.of(List.of(100L, 108L), List.of(100L, 112L)));

            var cobertura = SlotCoveragePolicy.ofDraft(draft, indice(elegibles));
            assertEquals(2, cobertura.count(MandatorySlot.LOWER_KNEE));
            assertEquals(4, SlotCoveragePolicy.exercisePositions(draft));
        }

        @Test
        @DisplayName("un id que no esta en el conjunto elegible no finge cobertura")
        void idDesconocidoNoFingeCobertura() {
            var draft = borradorIa(List.of(List.of(777L)));

            var cobertura = SlotCoveragePolicy.ofDraft(draft, indice(elegiblesCompleto()));
            assertEquals(1, cobertura.unclassified());
            assertEquals(0, cobertura.count(MandatorySlot.LOWER_KNEE));
        }
    }
}
