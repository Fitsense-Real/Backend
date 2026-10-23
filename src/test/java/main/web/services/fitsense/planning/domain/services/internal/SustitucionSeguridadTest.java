package main.web.services.fitsense.planning.domain.services.internal;

import main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sustitucion obligatoria por seguridad (P2-D): la politica, la excepcion de
 * V8C y el productor, sin base de datos. Los casos con catalogo real estan en
 * AdaptacionRegresionIT.
 */
class SustitucionSeguridadTest {

    static final LocalDate L1 = LocalDate.of(2026, 10, 12);
    static final LocalDate L2 = LocalDate.of(2026, 10, 19);

    static CandidateExercise ej(long id, String zona, String patron) {
        return new CandidateExercise(id, "ejercicio " + id, zona, "body weight", 1,
                PrescriptionType.SETS_REPS, null, patron);
    }

    // Semana medida: 101 (lunes) 740 rodilla + 1101 relleno de cintura;
    // 102 (miercoles) 304 empuje; 103 (viernes) 592 traccion.
    static final CandidateExercise RODILLA = ej(740, "upper legs", "KNEE_DOMINANT");
    static final CandidateExercise RELLENO_INSEGURO = ej(1101, "waist", "TRUNK_FLEXION");
    static final CandidateExercise EMPUJE = ej(304, "chest", "HORIZONTAL_PUSH");
    static final CandidateExercise TRACCION = ej(592, "back", "HORIZONTAL_PULL");
    // Sustitutos posibles.
    static final CandidateExercise RELLENO_CINTURA = ej(2001, "waist", "TRUNK_FLEXION");
    static final CandidateExercise RELLENO_OTRA_ZONA = ej(2002, "lower legs", "ANKLE_PLANTAR_FLEXION");
    static final CandidateExercise RODILLA_2 = ej(2003, "upper legs", "KNEE_DOMINANT");
    static final CandidateExercise EN_REVISION = ej(59, "waist", "TRUNK_FLEXION");

    static PreviousWeekSummary semanaMedida() {
        var workouts = List.of(outcome(101L, L1), outcome(102L, L1.plusDays(2)), outcome(103L, L1.plusDays(4)));
        var prescripciones = List.of(
                prescripcion(101L, L1, 740L, 3, 10, 90), prescripcion(101L, L1, 1101L, 2, 13, 90),
                prescripcion(102L, L1.plusDays(2), 304L, 3, 10, 90),
                prescripcion(103L, L1.plusDays(4), 592L, 3, 10, 90));
        return new PreviousWeekSummary(new BigDecimal("80.00"), new BigDecimal("6.0"), 480, Map.of(),
                workouts, prescripciones, List.of());
    }

    static PreviousWeekSummary.WorkoutOutcome outcome(long id, LocalDate fecha) {
        return new PreviousWeekSummary.WorkoutOutcome(id, fecha, "FULL_BODY", "COMPLETED", null, true,
                "COMPLETED", (short) 6, new BigDecimal("100.00"), "Sesion " + id, (short) 45);
    }

    static PreviousWeekSummary.PrescriptionOutcome prescripcion(long sesion, LocalDate fecha, long id,
                                                                int series, int reps, int descanso) {
        return new PreviousWeekSummary.PrescriptionOutcome(sesion, fecha, id, "ejercicio " + id, "SETS_REPS",
                (short) series, (short) reps, null, null, (short) series, series * reps, null, null,
                new BigDecimal("100.00"), "COMPLETED", null, "COMPLETED", (short) descanso);
    }

    static PlanDraft.DraftExercise dosis(long id, int series, int reps) {
        return new PlanDraft.DraftExercise(id, PrescriptionType.SETS_REPS, (short) series, (short) reps, null,
                null, (short) 90, null);
    }

    static PlanDraft.DraftWorkout sesion(LocalDate fecha, PlanDraft.DraftExercise... ejercicios) {
        return new PlanDraft.DraftWorkout(fecha, WorkoutFocus.FULL_BODY, "Sesion", 45, List.of(ejercicios));
    }

    /** La continuidad: la semana medida copiada a la semana nueva. */
    static WeekDerivation copia() {
        return new WeekDerivation(List.of(
                new DerivedWorkout(101L, sesion(L2, dosis(740, 3, 10), dosis(1101, 2, 13))),
                new DerivedWorkout(102L, sesion(L2.plusDays(2), dosis(304, 3, 10))),
                new DerivedWorkout(103L, sesion(L2.plusDays(4), dosis(592, 3, 10)))), List.of());
    }

    static Map<Long, CandidateExercise> elegibles(CandidateExercise... extra) {
        return Stream.concat(Stream.of(RODILLA, EMPUJE, TRACCION), Stream.of(extra))
                .collect(Collectors.toMap(CandidateExercise::exerciseId, c -> c, (a, b) -> a, LinkedHashMap::new));
    }

    static ExclusionesDeSeguridad excluido(CandidateExercise c, String motivo) {
        return new ExclusionesDeSeguridad(Map.of(c.exerciseId(), c), Map.of(c.exerciseId(), motivo));
    }

    static PlanGenerationContext contexto(String objetivo, Map<Long, CandidateExercise> elegibles) {
        var perfil = new PlanningProfile(7L, 35, "FEMALE", new BigDecimal("165"), new BigDecimal("70"), null,
                "INTERMEDIATE", objetivo, "x", "Ninguna", "HOME", 3, List.of((short) 1, (short) 3, (short) 5), 45,
                List.of("body weight"), List.of(), SafetyProfile.Declaradas.sinLimitaciones());
        return new PlanGenerationContext(7L, (short) 2, L2, L2.plusDays(6), perfil, PlanAdjustment.none(),
                semanaMedida(), List.copyOf(elegibles.values()), SafetyProfile.none(), (PrescriptionParams) null);
    }

    static SafetySubstitutionPolicy.Resultado reparar(WeekDerivation d, ExclusionesDeSeguridad ex,
                                                      Map<Long, CandidateExercise> elegibles, String objetivo) {
        return SafetySubstitutionPolicy.reparar(d.workouts(), semanaMedida(), ex, elegibles,
                contexto(objetivo, elegibles));
    }

    static List<Long> ids(DerivedWorkout d) {
        return d.workout().exercises().stream().map(PlanDraft.DraftExercise::exerciseId).toList();
    }

    // =====================================================================

    @Nested
    @DisplayName("la politica de sustitucion")
    class Politica {

        @Test
        @DisplayName("sustituye SOLO el heredado inseguro, en su posicion; el resto queda identico")
        void soloElHeredadoInseguro() {
            var r = reparar(copia(), excluido(RELLENO_INSEGURO, "INC-CAT-1374"),
                    elegibles(RELLENO_CINTURA, RELLENO_OTRA_ZONA), "GENERAL_FITNESS");
            assertTrue(r.problemas().isEmpty(), r.problemas().toString());
            assertEquals(List.of(740L, 2001L), ids(r.semana().get(0)));
            assertSame(copia().workouts().get(1).workout().getClass(), r.semana().get(1).workout().getClass());
            assertEquals(copia().workouts().get(1), r.semana().get(1));
            assertEquals(copia().workouts().get(2), r.semana().get(2));
            var s = r.sustituciones().get(0);
            assertEquals(101L, s.sesionOrigen());
            assertEquals(1101L, s.quitado());
            assertEquals(2001L, s.puesto());
            assertEquals("INC-CAT-1374", s.motivo());
            assertEquals(SafetySubstitutionPolicy.REGLA, s.regla());
            assertEquals("TRUNK_FLEXION", s.patronQuitado());
            // Misma dia, enfoque y duracion declarada.
            var antes = copia().workouts().get(0).workout();
            var despues = r.semana().get(0).workout();
            assertEquals(antes.scheduledDate(), despues.scheduledDate());
            assertEquals(antes.focus(), despues.focus());
            assertEquals(antes.expectedDurationMinutes(), despues.expectedDurationMinutes());
        }

        @Test
        @DisplayName("un estructural se sustituye por uno del MISMO slot, aunque haya relleno de la misma zona")
        void estructuralMismoSlot() {
            var r = reparar(copia(), excluido(RODILLA, "suelo"),
                    elegibles(RELLENO_CINTURA, RODILLA_2, ej(2010, "upper legs", "HIP_HINGE")), "GAIN_MUSCLE");
            // 740 ya no es elegible: se quita del conjunto.
            assertEquals(List.of(2003L, 1101L), ids(r.semana().get(0)));
        }

        @Test
        @DisplayName("prefiere la misma zona y nunca elige uno en revision de clasificacion")
        void mismaZonaYNoEnRevision() {
            var r = reparar(copia(), excluido(RELLENO_INSEGURO, "x"),
                    elegibles(EN_REVISION, RELLENO_OTRA_ZONA, RELLENO_CINTURA), "GENERAL_FITNESS");
            assertEquals(2001L, r.sustituciones().get(0).puesto());
            var soloRevision = reparar(copia(), excluido(RELLENO_INSEGURO, "x"),
                    elegibles(EN_REVISION), "GENERAL_FITNESS");
            assertFalse(soloRevision.problemas().isEmpty());
        }

        @Test
        @DisplayName("GAIN_MUSCLE: la dosis del sustituto es la de 4D para su clase, no la copiada")
        void dosis4D() {
            var r = reparar(copia(), excluido(RODILLA, "suelo"), elegibles(RODILLA_2), "GAIN_MUSCLE");
            var puesto = r.semana().get(0).workout().exercises().get(0);
            var esperada = MvpDosePolicy.dosisPara(MandatorySlot.of(RODILLA_2), "upper legs").orElseThrow();
            assertEquals(esperada.series(), puesto.plannedSets());
            assertEquals(esperada.reps(), puesto.plannedReps());
            assertEquals(esperada.descansoSegundos(), puesto.restSeconds());
            assertNull(puesto.targetLoadKg(), "sin registro de carga del sustituto no se sugiere peso");
        }

        @Test
        @DisplayName("sin sustituto valido: lo dice con su motivo y no toca la semana")
        void sinSustituto() {
            var r = reparar(copia(), excluido(RELLENO_INSEGURO, "INC-CAT-1374"), elegibles(RODILLA_2),
                    "GENERAL_FITNESS");
            assertTrue(r.sustituciones().isEmpty());
            assertEquals(1, r.problemas().size());
            assertTrue(r.problemas().get(0).startsWith("SEGURIDAD: el ejercicio 1101 de la sesion 101"),
                    r.problemas().get(0));
            assertTrue(r.problemas().get(0).contains("INC-CAT-1374"));
            assertEquals(copia().workouts(), r.semana());
        }

        @Test
        @DisplayName("lo que NO es heredado de esa sesion no se repara: un error de la IA sigue siendo error")
        void noReparaLoNoHeredado() {
            // 1101 aparece en la sesion 102, donde no estaba en la semana medida.
            var d = new WeekDerivation(List.of(
                    new DerivedWorkout(101L, sesion(L2, dosis(740, 3, 10))),
                    new DerivedWorkout(102L, sesion(L2.plusDays(2), dosis(304, 3, 10), dosis(1101, 2, 13))),
                    new DerivedWorkout(103L, sesion(L2.plusDays(4), dosis(592, 3, 10)))), List.of());
            var r = reparar(d, excluido(RELLENO_INSEGURO, "x"), elegibles(RELLENO_CINTURA), "GENERAL_FITNESS");
            assertTrue(r.sustituciones().isEmpty());
            assertEquals(d.workouts(), r.semana());
        }

        @Test
        @DisplayName("no elige un ejercicio que ya esta en la sesion ni en un dia contiguo")
        void noRepiteNiContiguo() {
            var d = new WeekDerivation(List.of(
                    new DerivedWorkout(101L, sesion(L2, dosis(740, 3, 10), dosis(1101, 2, 13))),
                    new DerivedWorkout(102L, sesion(L2.plusDays(1), dosis(2001, 2, 13)))), List.of());
            var r = SafetySubstitutionPolicy.reparar(d.workouts(), semanaMedida(), excluido(RELLENO_INSEGURO, "x"),
                    elegibles(RELLENO_CINTURA, ej(2004, "waist", "TRUNK_ROTATION")), contexto("GENERAL_FITNESS", elegibles()));
            assertEquals(2004L, r.sustituciones().get(0).puesto(), "2001 esta el dia siguiente");
        }

        @Test
        @DisplayName("una zona que el enfoque no admite no sirve de sustituto")
        void zonaFueraDelEnfoque() {
            var r = reparar(copia(), excluido(RELLENO_INSEGURO, "x"), elegibles(RELLENO_OTRA_ZONA), "GENERAL_FITNESS");
            assertTrue(r.sustituciones().isEmpty());
            assertEquals(1, r.problemas().size());
        }

        @Test
        @DisplayName("sin exclusiones no cambia nada")
        void sinExclusiones() {
            var r = reparar(copia(), ExclusionesDeSeguridad.ninguna(), elegibles(RELLENO_CINTURA), "GAIN_MUSCLE");
            assertEquals(copia().workouts(), r.semana());
            assertTrue(r.sustituciones().isEmpty() && r.problemas().isEmpty());
        }
    }

    @Nested
    @DisplayName("la excepcion de V8C: solo la sustitucion obligatoria por seguridad, comprobada")
    class V8C {

        final ConservationValidator v8c = new ConservationValidator();

        @Test
        @DisplayName("admitida: la sesion cuenta como conservada y la sustitucion consta en el informe")
        void admitida() {
            var el = elegibles(RELLENO_CINTURA);
            var r = reparar(copia(), excluido(RELLENO_INSEGURO, "INC-CAT-1374"), el, "GENERAL_FITNESS");
            var informe = v8c.validateContinuidad(new WeekDerivation(r.semana(), List.of()), semanaMedida(), el,
                    r.sustituciones());
            assertTrue(informe.problems().isEmpty(), informe.problems().toString());
            assertEquals(List.of(101L, 102L, 103L), informe.conservadas());
            assertEquals(r.sustituciones(), informe.sustituciones());
        }

        @Test
        @DisplayName("sin autorizacion, la misma sustitucion es un cambio no permitido, como siempre")
        void sinAutorizacion() {
            var el = elegibles(RELLENO_CINTURA);
            var r = reparar(copia(), excluido(RELLENO_INSEGURO, "x"), el, "GENERAL_FITNESS");
            var informe = v8c.validateContinuidad(new WeekDerivation(r.semana(), List.of()), semanaMedida(), el);
            assertTrue(informe.problems().stream().anyMatch(p -> p.contains("aparecen los ejercicios [2001]")),
                    informe.problems().toString());
        }

        @Test
        @DisplayName("si el quitado sigue siendo elegible, no hay motivo de seguridad: se rechaza")
        void quitadoTodaviaElegible() {
            var el = elegibles(RELLENO_CINTURA, RELLENO_INSEGURO);
            var s = new SustitucionSeguridad(101L, L2, 1101L, "TRUNK_FLEXION", 2001L, "x", "R", "a", "b");
            var semana = new WeekDerivation(List.of(
                    new DerivedWorkout(101L, sesion(L2, dosis(740, 3, 10), dosis(2001, 2, 13))),
                    copia().workouts().get(1), copia().workouts().get(2)), List.of());
            var informe = v8c.validateContinuidad(semana, semanaMedida(), el, List.of(s));
            assertTrue(informe.problems().stream().anyMatch(p -> p.contains("sigue siendo elegible")),
                    informe.problems().toString());
            assertTrue(informe.sustituciones().isEmpty());
        }

        @Test
        @DisplayName("un relleno no puede sustituir a un estructural: se rechaza por clase de slot")
        void otraClaseDeSlot() {
            var el = elegibles(RELLENO_CINTURA);
            el.remove(740L);
            var s = new SustitucionSeguridad(101L, L2, 740L, "KNEE_DOMINANT", 2001L, "x", "R", "a", "b");
            var semana = new WeekDerivation(List.of(
                    new DerivedWorkout(101L, sesion(L2, dosis(2001, 3, 10), dosis(1101, 2, 13))),
                    copia().workouts().get(1), copia().workouts().get(2)), List.of());
            var informe = v8c.validateContinuidad(semana, semanaMedida(), el, List.of(s));
            assertTrue(informe.problems().stream().anyMatch(p -> p.contains("misma clase de slot")),
                    informe.problems().toString());
        }

        @Test
        @DisplayName("la excepcion no abre la sesion: cualquier otro cambio en ella sigue rechazado")
        void noAbreLaSesion() {
            var el = elegibles(RELLENO_CINTURA);
            var r = reparar(copia(), excluido(RELLENO_INSEGURO, "x"), el, "GENERAL_FITNESS");
            // Ademas de la sustitucion, se cambia la dosis del 740.
            var tocada = r.semana().get(0).workout();
            var ejercicios = new ArrayList<>(tocada.exercises());
            ejercicios.set(0, dosis(740, 2, 10));
            var semana = new WeekDerivation(List.of(new DerivedWorkout(101L, new PlanDraft.DraftWorkout(
                    tocada.scheduledDate(), tocada.focus(), tocada.name(), tocada.expectedDurationMinutes(),
                    ejercicios)), r.semana().get(1), r.semana().get(2)), List.of());
            var informe = v8c.validateContinuidad(semana, semanaMedida(), el, r.sustituciones());
            assertTrue(informe.problems().stream().anyMatch(p -> p.contains("cambia la dosis de [740]")),
                    informe.problems().toString());
        }
    }

    @Nested
    @DisplayName("el productor")
    class Productor {

        static final class Estricta implements StrictPlanValidation {
            @Override
            public List<String> revisar(PlanDraft draft, PlanGenerationContext context, ConservationReport c) {
                // V1: todo ejercicio tiene que ser elegible.
                var elegibles = context.availableExercises().stream().map(CandidateExercise::exerciseId)
                        .collect(Collectors.toSet());
                return draft.workouts().stream().flatMap(w -> w.exercises().stream())
                        .map(PlanDraft.DraftExercise::exerciseId).filter(id -> !elegibles.contains(id))
                        .map(id -> "V1: el ejercicio " + id + " no esta en available_exercises.").toList();
            }

            @Override
            public boolean bloqueaDosis4D() {
                return true;
            }
        }

        final AdaptedWeekProducer producer = new AdaptedWeekProducer(new SessionFixApplier(),
                new ConservationValidator(), new Estricta());

        @Test
        @DisplayName("continuidad con un heredado inseguro y sustituto: PUBLICABLE, CONSERVED, sustitucion en el informe")
        void publicaConSustitucion() {
            var el = elegibles(RELLENO_CINTURA);
            var r = producer.produce(contexto("GENERAL_FITNESS", el), copia(), List.of(), el,
                    excluido(RELLENO_INSEGURO, "INC-CAT-1374"));
            assertTrue(r.publicable(), r.problemas().toString());
            assertEquals(GenerationSource.CONSERVED, r.source());
            assertEquals(List.of(740L, 2001L), ids(r.semana().get(0)));
            assertEquals(1, r.conservacion().sustituciones().size());
        }

        @Test
        @DisplayName("sin sustituto: NO_FACTIBLE, el motivo de seguridad primero")
        void noFactibleSinSustituto() {
            var el = elegibles();
            var r = producer.produce(contexto("GENERAL_FITNESS", el), copia(), List.of(), el,
                    excluido(RELLENO_INSEGURO, "INC-CAT-1374"));
            assertFalse(r.publicable());
            assertTrue(r.problemas().get(0).startsWith("SEGURIDAD: el ejercicio 1101"), r.problemas().toString());
        }

        @Test
        @DisplayName("sin exclusiones, el mismo heredado no elegible sigue siendo NO_FACTIBLE por V1, como antes")
        void sinExclusionesComoAntes() {
            var el = elegibles(RELLENO_CINTURA);
            var r = producer.produce(contexto("GENERAL_FITNESS", el), copia(), List.of(), el);
            assertFalse(r.publicable());
            assertTrue(r.problemas().stream().anyMatch(p -> p.startsWith("V1: el ejercicio 1101")));
        }
    }
}
