package main.web.services.fitsense.planning.domain.services.internal;

import main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.PlanDraftValidator;
import main.web.services.fitsense.planning.domain.services.SessionDurationEstimator;
import main.web.services.fitsense.planning.domain.services.SlotCoverageEnforcement;
import main.web.services.fitsense.planning.domain.services.SlotCoveragePolicy;
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
 * El desglose por dia de los cuatro slots: cero dias, un dia y varios dias.
 * <p>
 * Lo que estas pruebas tienen que fijar, ademas de la aritmetica, es que el
 * desglose **no decide nada**: un slot concentrado en un solo dia se registra y
 * no produce ningun problema de validacion. Si alguien convierte eso en un
 * rechazo mas adelante, estas pruebas se caen, que es justo lo que se quiere.
 * <p>
 * Dominio puro: sin Spring, sin base y sin Replicate.
 */
class WeeklySlotDistributionTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 10, 12);
    private static final LocalDate MIERCOLES = LUNES.plusDays(2);
    private static final LocalDate VIERNES = LUNES.plusDays(4);

    // ------------------------------------------------------------- utiles

    private static CandidateExercise ej(long id, String zona, String patron) {
        return new CandidateExercise(id, "ejercicio " + id, zona, "body weight", 1,
                PrescriptionType.SETS_REPS, null, patron);
    }

    /** Conjunto con los cuatro slots bien cubiertos, por si hace falta exigirlos. */
    private static List<CandidateExercise> elegibles() {
        var lista = new ArrayList<CandidateExercise>();
        long id = 100;
        for (int i = 0; i < 4; i++) lista.add(ej(id++, "upper legs", "KNEE_DOMINANT"));
        for (int i = 0; i < 4; i++) lista.add(ej(id++, "upper legs", "HIP_HINGE"));
        for (int i = 0; i < 4; i++) lista.add(ej(id++, "chest", "HORIZONTAL_PUSH"));
        for (int i = 0; i < 4; i++) lista.add(ej(id++, "back", "HORIZONTAL_PULL"));
        for (int i = 0; i < 4; i++) lista.add(ej(id++, "upper arms", "ELBOW_FLEXION"));
        return lista;
    }

    // ids por slot: 100-103 rodilla · 104-107 cadera · 108-111 empuje
    //               112-115 traccion · 116-119 accesorio

    private static Map<Long, CandidateExercise> indice() {
        return elegibles().stream().collect(
                Collectors.toMap(CandidateExercise::exerciseId, c -> c, (a, b) -> a));
    }

    private static PlanDraft.DraftExercise prescrito(long id) {
        return new PlanDraft.DraftExercise(id, PrescriptionType.SETS_REPS,
                (short) 2, (short) 10, null, null, (short) 75, null);
    }

    /** Un borrador con una sesion por fecha dada. */
    private static PlanDraft borrador(List<LocalDate> fechas, List<List<Long>> porDia) {
        var entrenos = new ArrayList<PlanDraft.DraftWorkout>();
        for (int i = 0; i < fechas.size(); i++) {
            entrenos.add(new PlanDraft.DraftWorkout(fechas.get(i), WorkoutFocus.FULL_BODY,
                    "FULL_BODY - Sesion " + (i + 1), 45,
                    porDia.get(i).stream()
                            .map(WeeklySlotDistributionTest::prescrito).toList()));
        }
        return new PlanDraft(GenerationSource.AI, "gpt-5-nano", "plan de prueba",
                null, "prueba", entrenos);
    }

    private static WeeklySlotDistribution reparto(PlanDraft draft) {
        return SlotCoveragePolicy.distributionOf(draft, indice());
    }

    // ============================================================ cero dias

    @Nested
    @DisplayName("Cero dias: el slot no aparece en toda la semana")
    class CeroDias {

        @Test
        @DisplayName("colocaciones 0, dias 0, y una lista de ceros con un hueco por dia")
        void slotAusente() {
            // Tres sesiones sin un solo ejercicio de cadera.
            var d = reparto(borrador(List.of(LUNES, MIERCOLES, VIERNES),
                    List.of(List.of(100L, 108L), List.of(101L, 112L), List.of(109L, 113L))));

            assertEquals(0, d.placements(MandatorySlot.LOWER_HIP));
            assertEquals(0, d.distinctDays(MandatorySlot.LOWER_HIP));
            assertEquals(List.of(0, 0, 0), d.perDay(MandatorySlot.LOWER_HIP),
                    "un dia sin el slot es un 0, no una ausencia en la lista");
            assertEquals(List.of(MandatorySlot.LOWER_HIP), d.missingSlots());
        }

        @Test
        @DisplayName("un slot a cero NO cuenta como concentrado")
        void ceroNoEsConcentracion() {
            var d = reparto(borrador(List.of(LUNES, MIERCOLES, VIERNES),
                    List.of(List.of(100L, 108L), List.of(101L, 112L), List.of(109L, 113L))));

            assertFalse(d.concentratedSlots().contains(MandatorySlot.LOWER_HIP),
                    "no hay nada concentrado donde no hay nada");
        }

        @Test
        @DisplayName("los cuatro slots salen SIEMPRE, aunque los cuatro esten a cero")
        void losCuatroSiempre() {
            var d = reparto(borrador(List.of(LUNES), List.of(List.of(116L, 117L))));

            for (var slot : MandatorySlot.ALL) {
                assertEquals(0, d.placements(slot));
                assertEquals(List.of(0), d.perDay(slot), "falta la fila de " + slot);
            }
            assertEquals(MandatorySlot.ALL, d.missingSlots());
            var texto = d.describe((short) 1, "OBSERVE");
            for (var slot : MandatorySlot.ALL)
                assertTrue(texto.contains(slot.name()),
                        "el informe tiene que nombrar " + slot + " aunque este a cero");
        }

        @Test
        @DisplayName("un borrador sin sesiones no revienta")
        void sinSesiones() {
            var vacio = new PlanDraft(GenerationSource.RULE_ENGINE, null, "vacio",
                    null, "prueba", List.of());

            var d = reparto(vacio);
            assertEquals(List.of(), d.trainingDates());
            assertEquals(0, d.placements(MandatorySlot.LOWER_KNEE));
            assertEquals(List.of(), d.perDay(MandatorySlot.LOWER_KNEE));
        }
    }

    // ============================================================== un dia

    @Nested
    @DisplayName("Un dia: el caso del plan 4, que la cuenta de colocaciones escondia")
    class UnDia {

        @Test
        @DisplayName("2 colocaciones en la misma fecha son 1 dia, no 2")
        void dosEnElMismoDia() {
            var d = reparto(borrador(List.of(LUNES, MIERCOLES, VIERNES),
                    List.of(List.of(100L, 108L), List.of(101L, 112L),
                            List.of(104L, 105L))));          // las dos caderas el viernes

            assertEquals(2, d.placements(MandatorySlot.LOWER_HIP));
            assertEquals(1, d.distinctDays(MandatorySlot.LOWER_HIP),
                    "dos ejercicios el mismo dia son UN dia");
            assertEquals(List.of(0, 0, 2), d.perDay(MandatorySlot.LOWER_HIP));
            assertEquals(List.of(MandatorySlot.LOWER_HIP), d.concentratedSlots());
        }

        @Test
        @DisplayName("1 sola colocacion NO se marca como concentrada")
        void unaSolaNoEsConcentracion() {
            var d = reparto(borrador(List.of(LUNES, MIERCOLES),
                    List.of(List.of(100L, 104L), List.of(108L, 112L))));

            assertEquals(1, d.placements(MandatorySlot.LOWER_HIP));
            assertEquals(1, d.distinctDays(MandatorySlot.LOWER_HIP));
            assertEquals(List.of(), d.concentratedSlots(),
                    "concentrar exige 2 o mas en el mismo dia; una vez es poca "
                            + "cobertura, que es otra cosa");
        }

        @Test
        @DisplayName("el informe dice qué slot esta concentrado, por su nombre")
        void elInformeLoDice() {
            var texto = reparto(borrador(List.of(LUNES, MIERCOLES, VIERNES),
                    List.of(List.of(100L, 108L), List.of(101L, 112L),
                            List.of(104L, 105L))))
                    .describe((short) 1, "OBSERVE");

            assertTrue(texto.contains("LOWER_HIP concentrado en 1 dia"), texto);
            assertTrue(texto.contains("Cobertura estructural: COMPLETA"),
                    "los cuatro slots estan cubiertos: la concentracion es otra cosa");
            assertTrue(texto.contains("OBSERVE"));
        }
    }

    // ========================================================== varios dias

    @Nested
    @DisplayName("Varios dias: el reparto que se quiere")
    class VariosDias {

        @Test
        @DisplayName("mismo total de colocaciones, repartido, da mas dias distintos")
        void repartidoEnTres() {
            var d = reparto(borrador(List.of(LUNES, MIERCOLES, VIERNES),
                    List.of(List.of(100L, 104L, 108L, 112L),
                            List.of(101L, 105L, 109L, 113L),
                            List.of(102L, 106L, 110L, 114L))));

            for (var slot : MandatorySlot.ALL) {
                assertEquals(3, d.placements(slot), slot.name());
                assertEquals(3, d.distinctDays(slot), slot.name());
                assertEquals(List.of(1, 1, 1), d.perDay(slot), slot.name());
            }
            assertEquals(List.of(), d.concentratedSlots());
            assertEquals(List.of(), d.missingSlots());
        }

        @Test
        @DisplayName("las fechas salen en orden cronologico aunque el borrador no lo este")
        void fechasOrdenadas() {
            var d = reparto(borrador(List.of(VIERNES, LUNES, MIERCOLES),
                    List.of(List.of(100L), List.of(104L), List.of(108L))));

            assertEquals(List.of(LUNES, MIERCOLES, VIERNES), d.trainingDates());
            // El 100 estaba el viernes: tiene que caer en la tercera posicion.
            assertEquals(List.of(0, 0, 1), d.perDay(MandatorySlot.LOWER_KNEE));
            assertEquals(List.of(1, 0, 0), d.perDay(MandatorySlot.LOWER_HIP));
        }

        @Test
        @DisplayName("el reparto desigual se ve dia a dia")
        void repartoDesigual() {
            var d = reparto(borrador(List.of(LUNES, MIERCOLES, VIERNES),
                    List.of(List.of(100L, 101L, 108L), List.of(102L, 109L),
                            List.of(104L, 110L, 112L))));

            assertEquals(3, d.placements(MandatorySlot.LOWER_KNEE));
            assertEquals(2, d.distinctDays(MandatorySlot.LOWER_KNEE));
            assertEquals(List.of(2, 1, 0), d.perDay(MandatorySlot.LOWER_KNEE));
            assertEquals(List.of(3, 1, 1).size(), d.trainingDates().size());
        }

        @Test
        @DisplayName("un id que no esta en el conjunto elegible no suma a ningun dia")
        void idDesconocido() {
            var d = reparto(borrador(List.of(LUNES), List.of(List.of(777L, 100L))));

            assertEquals(1, d.placements(MandatorySlot.LOWER_KNEE));
            assertEquals(List.of(1), d.perDay(MandatorySlot.LOWER_KNEE));
        }
    }

    // ================================= y lo mas importante: no decide nada

    @Nested
    @DisplayName("El desglose NO cambia el criterio de aceptacion")
    class NoDecideNada {

        private static PlanGenerationContext contexto(List<CandidateExercise> elegibles) {
            var perfil = new PlanningProfile(1L, 30, "MALE", new BigDecimal("175.0"),
                    new BigDecimal("72.0"), new BigDecimal("75.0"), "BEGINNER",
                    "GAIN_MUSCLE", "Ganar masa muscular", "Ninguna", "HOME", 3,
                    List.of((short) 1, (short) 3, (short) 5), 45,
                    List.of("body weight", "dumbbell", "band"), List.of());
            return new PlanGenerationContext(1L, (short) 1, LUNES, LUNES.plusDays(6),
                    perfil, PlanAdjustment.none(), null, elegibles,
                    SafetyProfile.none(), (PrescriptionParams) null);
        }

        private static List<String> problemasV23(SlotCoverageEnforcement modo, PlanDraft draft) {
            var validador = new PlanDraftValidator(new SessionDurationEstimator(), modo);
            try {
                validador.validate(draft, contexto(elegibles()), 30);
                return List.of();
            } catch (InvalidPlanDraftException e) {
                return e.problems().stream().filter(p -> p.startsWith("V23:")).toList();
            }
        }

        @Test
        @DisplayName("un slot concentrado en un dia NO produce ningun problema, en ningun modo")
        void concentrarNoRechaza() {
            // Los cuatro slots cubiertos; la cadera, las dos veces el viernes.
            var draft = borrador(List.of(LUNES, MIERCOLES, VIERNES),
                    List.of(List.of(100L, 108L), List.of(101L, 112L),
                            List.of(104L, 105L)));

            assertEquals(List.of(MandatorySlot.LOWER_HIP),
                    reparto(draft).concentratedSlots(),
                    "el caso es el que se quiere probar");

            for (var modo : SlotCoverageEnforcement.values()) {
                assertEquals(List.of(), problemasV23(modo, draft),
                        "la concentracion se convirtio en rechazo en modo " + modo
                                + ". Eso es un cambio de criterio de 4C y no se "
                                + "decidio: primero se mide.");
            }
        }

        @Test
        @DisplayName("lo que SI sigue rechazando es un slot ausente, como antes")
        void ausenteSigueRechazando() {
            var draft = borrador(List.of(LUNES, MIERCOLES, VIERNES),
                    List.of(List.of(100L, 108L), List.of(101L, 112L),
                            List.of(109L, 113L)));          // sin cadera

            assertEquals(1, problemasV23(
                            SlotCoverageEnforcement.ENFORCE_WHEN_SATISFIABLE, draft).size(),
                    "el comportamiento anterior tiene que seguir intacto");
        }
    }
}
