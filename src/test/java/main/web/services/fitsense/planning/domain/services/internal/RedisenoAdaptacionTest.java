package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams;
import main.web.services.fitsense.planning.application.internal.outboundservices.acl.ExternalConfigurationService;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.infrastructure.validation.StrictValidationAdapter;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * REDISENO R/R' (orden del 05/11/2026) · P-A1 (reduccion por fatiga en GAIN_MUSCLE) y P-A2 (una sustitucion ante
 * desmotivacion), con el productor, V8C y la validacion estricta reales (dominio puro, sin Spring ni base de datos).
 * Cada cambio se prueba con {@code PLAN-REV-2.0} (lo autoriza) y con la politica historica (todo como antes), y con
 * negativas: lo que la politica no autoriza sigue rechazado.
 */
@DisplayName("Rediseno R/R': P-A1 (fatiga en GM) y P-A2 (desmotivacion) solo con la politica que lo autoriza")
class RedisenoAdaptacionTest {

    private static final long USUARIO = 9L;
    private static final LocalDate LUNES = LocalDate.of(2026, 11, 2);
    private static final int SESIONES = 3;
    private static final int DIVISOR = 30;
    private static final SessionDurationEstimator ESTIMADOR = new SessionDurationEstimator();
    private static final long S0 = 701L;
    /** Estructurales del lunes: 100 KNEE_DOMINANT, 101 HORIZONTAL_PUSH, 102 HORIZONTAL_PULL, 103 HIP_HINGE. */
    private static final long EJ = 100L;
    /** Sustitutos de 101 (HORIZONTAL_PUSH, chest, dificultad 1). */
    private static final long EQUIVALENTE = 900L, MAS_DIFICIL = 901L, MAS_IMPACTO = 902L, OTRO_PATRON = 903L,
            OTRO_GRUPO = 904L, EQUIVALENTE_2 = 905L;

    private final AdaptedWeekProducer productor = new AdaptedWeekProducer(
            new SessionFixApplier(), new ConservationValidator(), estricta());

    // =======================================================================
    // P-A1
    // =======================================================================

    @Nested
    @DisplayName("P-A1: dosis de mantenimiento ante fatiga en GAIN_MUSCLE")
    class Fatiga {

        @Test
        @DisplayName("REV2: los 4 estructurales 3x10 -> 2x10 se publican como MANTENIMIENTO (aplicada, -33 % de series)")
        void mantenimientoAceptado() {
            var r = corregir(PlanningPolicy.REV2, TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION,
                    conSeries(Map.of(100L, 2, 101L, 2, 102L, 2, 103L, 2)));
            assertTrue(r.publicable(), r.problemas().toString());
            assertEquals(List.of(S0), r.conservacion().aplicadas());
            assertEquals(Optional.of(DosisAutorizada.Origen.MANTENIMIENTO),
                    r.conservacion().autorizacionDe(LUNES.plusWeeks(1), ej(EJ, 2, 10)));
        }

        @Test
        @DisplayName("HISTORICA: la misma reduccion la rechaza V24 (4D no se reduce): comportamiento de siempre")
        void historicaRechaza() {
            var r = corregir(PlanningPolicy.HISTORICA, TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION,
                    conSeries(Map.of(100L, 2)));
            assertFalse(r.publicable());
            assertTrue(r.problemas().stream().anyMatch(p -> p.startsWith("V24: el ejercicio 100 ")), r.problemas().toString());
        }

        @Test
        @DisplayName("REV2 (negativas): reps bajadas, 1 serie, relleno bajado o descanso cambiado siguen rechazados")
        void reduccionesArbitrarias() {
            var reps = new ArrayList<>(ejercicios(0, 6));
            reps.set(0, ej(100L, 3, 8));
            assertFalse(corregir(PlanningPolicy.REV2, TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION, reps).publicable(),
                    "bajar repeticiones no es la dosis de mantenimiento");
            assertFalse(corregir(PlanningPolicy.REV2, TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION,
                    conSeries(Map.of(100L, 1))).publicable(), "una serie no");
            assertFalse(corregir(PlanningPolicy.REV2, TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION,
                    conSeries(Map.of(104L, 1))).publicable(), "el relleno (2 series) no baja");
            var descanso = new ArrayList<>(ejercicios(0, 6));
            descanso.set(0, new PlanDraft.DraftExercise(100L, PrescriptionType.SETS_REPS, (short) 2, (short) 10, null,
                    null, (short) 60, null));
            assertFalse(corregir(PlanningPolicy.REV2, TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION, descanso)
                    .publicable(), "el descanso no cambia");
        }

        @Test
        @DisplayName("REV2 (negativa): la dosis de mantenimiento es SOLO ante fatiga; con desmotivacion se rechaza")
        void soloFatiga() {
            var r = corregir(PlanningPolicy.REV2, TipoIntervencion.INVESTIGAR_CAMBIO_MINIMO, conSeries(Map.of(100L, 2)));
            assertFalse(r.publicable());
            assertTrue(r.problemas().stream().anyMatch(p -> p.startsWith("V24: el ejercicio 100 ")), r.problemas().toString());
        }

        @Test
        @DisplayName("la copia de una dosis de mantenimiento con linaje se hereda: REV2 la publica; HISTORICA no la rechaza por V24")
        void copiaHereda() {
            var ctx = contexto(PlanAdjustment.none(), medida(Map.of(100L, (short) 2), Set.of(),
                    Map.of(S0, Set.of(100L)), Map.of(), Map.of()), elegibles()).conPolitica(PlanningPolicy.REV2);
            var r = productor.produce(ctx, copia(Map.of(100L, (short) 2)), List.of(), elegibles());
            assertTrue(r.publicable(), r.problemas().toString());
            assertEquals(Optional.of(DosisAutorizada.Origen.HEREDADA),
                    r.conservacion().autorizacionDe(LUNES.plusWeeks(1), ej(100L, 2, 10)));
            // Al volver a HISTORICA, V24 acepta la dosis heredada (no queda NO_FACTIBLE por 4D). El piso del 70 % de
            // V20 SI sigue rigiendo: en esta sesion, al limite de 45 min, faltan 30 s (limitacion documentada; D8 solo
            // concede la exencion de V20 a un ACORTAR).
            var hist = productor.produce(ctx.conPolitica(PlanningPolicy.HISTORICA), copia(Map.of(100L, (short) 2)),
                    List.of(), elegibles());
            assertTrue(hist.problemas().stream().noneMatch(p -> p.startsWith("V24")), hist.problemas().toString());
            assertTrue(hist.problemas().stream().allMatch(p -> p.startsWith("V20")), hist.problemas().toString());
        }

        @Test
        @DisplayName("sin linaje, la copia de 2x10 sigue rechazada: heredar exige una autorizacion publicada")
        void copiaSinLinaje() {
            var ctx = contexto(PlanAdjustment.none(), medida(Map.of(100L, (short) 2), Set.of(), Map.of(), Map.of(),
                    Map.of()), elegibles()).conPolitica(PlanningPolicy.REV2);
            assertFalse(productor.produce(ctx, copia(Map.of(100L, (short) 2)), List.of(), elegibles()).publicable());
        }

        @Test
        @DisplayName("fatiga persistente: 2 semanas -> aviso de revision; 3 o mas -> recomendar un profesional")
        void fatigaPersistente() {
            var r2 = corregir(PlanningPolicy.REV2, TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION,
                    conSeries(Map.of(100L, 2)), Map.of(S0, 1), Map.of());
            assertTrue(r2.conservacion().avisos().stream().anyMatch(a -> a.startsWith("FATIGA_REPETIDA sesion " + S0)),
                    r2.conservacion().avisos().toString());
            var r3 = corregir(PlanningPolicy.REV2, TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION,
                    conSeries(Map.of(100L, 2)), Map.of(S0, 2), Map.of());
            assertTrue(r3.conservacion().avisos().stream().anyMatch(a -> a.startsWith("FATIGA_PERSISTENTE sesion " + S0)
                    && a.contains("profesional sanitario")), r3.conservacion().avisos().toString());
            var r1 = corregir(PlanningPolicy.REV2, TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION,
                    conSeries(Map.of(100L, 2)));
            assertTrue(r1.conservacion().avisos().isEmpty(), "una semana de fatiga no avisa");
        }

        @Test
        @DisplayName("fatiga persistente sobre una sesion ya en mantenimiento: no se puede bajar mas (2x10 -> 1x10 rechazado)")
        void noBajaMas() {
            var lista = conSeries(Map.of(100L, 1));
            var ctx = contexto(orden(TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION),
                    medida(Map.of(100L, (short) 2), Set.of(), Map.of(S0, Set.of(100L)), Map.of(S0, 1), Map.of()),
                    elegibles()).conPolitica(PlanningPolicy.REV2);
            var correccion = new CorrectedSession(S0, LUNES.plusWeeks(1), minutos(lista), lista, "bajar");
            var r = productor.produce(ctx, copia(Map.of(100L, (short) 2)), List.of(correccion), elegibles());
            assertFalse(r.publicable());
        }

        @Test
        @DisplayName("fatiga NO es dolor: si consta dolor en un ejercicio, bajarle la dosis no basta (la seguridad manda)")
        void dolorNoEsFatiga() {
            var r = corregir(PlanningPolicy.REV2, TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION,
                    conSeries(Map.of(100L, 2)), Map.of(), Map.of(100L, "PAIN_OR_DISCOMFORT"));
            assertFalse(r.publicable());
            assertTrue(r.problemas().stream().anyMatch(p -> p.contains("100") && p.toLowerCase(Locale.ROOT).contains("dol")),
                    r.problemas().toString());
        }

        @Test
        @DisplayName("generacion inicial: una dosis 2x10 en un estructural sigue rechazada por V24 tambien con REV2")
        void generacionIntacta() {
            var validador = new PlanDraftValidator(ESTIMADOR, SlotCoverageEnforcement.ENFORCE_AI_ONLY,
                    DoseEnforcement.ENFORCE);
            var semana = semanaNueva(Map.of(100L, (short) 2));
            var ctx = contexto(PlanAdjustment.none(), medida(Map.of(), Set.of(), Map.of(), Map.of(), Map.of()),
                    elegibles()).conPolitica(PlanningPolicy.REV2);
            var e = assertThrows(InvalidPlanDraftException.class, () -> validador.validate(
                    new PlanDraft(GenerationSource.RULE_ENGINE, null, "g", null, "r", semana), ctx, DIVISOR));
            assertTrue(e.problems().stream().anyMatch(p -> p.startsWith("V24: el ejercicio 100 ")), e.problems().toString());
        }
    }

    // =======================================================================
    // P-A2
    // =======================================================================

    @Nested
    @DisplayName("P-A2: una sustitucion lateral ante desmotivacion")
    class Desmotivacion {

        @Test
        @DisplayName("REV2: sustituir 101 por un equivalente (mismo patron y grupo, dif e impacto no superiores) se publica")
        void sustitucionEquivalente() {
            var r = corregir(PlanningPolicy.REV2, TipoIntervencion.INVESTIGAR_CAMBIO_MINIMO, sustituir(101L, EQUIVALENTE));
            assertTrue(r.publicable(), r.problemas().toString());
            assertEquals(List.of(S0), r.conservacion().aplicadas());
        }

        @Test
        @DisplayName("HISTORICA: la misma sustitucion se rechaza (INVESTIGAR no sustituye): comportamiento de siempre")
        void historicaRechaza() {
            var r = corregir(PlanningPolicy.HISTORICA, TipoIntervencion.INVESTIGAR_CAMBIO_MINIMO,
                    sustituir(101L, EQUIVALENTE));
            assertFalse(r.publicable());
            assertTrue(r.problemas().stream().anyMatch(p -> p.contains("no autoriza sustituir")), r.problemas().toString());
        }

        @Test
        @DisplayName("REV2 (negativas): mas dificil, mas impacto, otro patron u otro grupo se rechazan con su motivo")
        void sustitucionesNoEquivalentes() {
            var casos = Map.of(MAS_DIFICIL, "mas dificil", MAS_IMPACTO, "mas impacto", OTRO_PATRON, "otro patron",
                    OTRO_GRUPO, "otro grupo");
            casos.forEach((id, motivo) -> {
                var r = corregir(PlanningPolicy.REV2, TipoIntervencion.INVESTIGAR_CAMBIO_MINIMO, sustituir(101L, id));
                assertFalse(r.publicable(), "sustituto " + id);
                assertTrue(r.problemas().stream().anyMatch(p -> p.contains(motivo)), id + " " + r.problemas());
            });
        }

        @Test
        @DisplayName("REV2 (negativa): dos sustituciones en la misma sesion se rechazan")
        void dosSustituciones() {
            var lista = new ArrayList<>(sustituir(101L, EQUIVALENTE));
            lista.set(lista.indexOf(ej(102L, 3, 10)), ej(EQUIVALENTE_2, 3, 10));
            var r = corregir(PlanningPolicy.REV2, TipoIntervencion.INVESTIGAR_CAMBIO_MINIMO, lista);
            assertFalse(r.publicable());
            assertTrue(r.problemas().stream().anyMatch(p -> p.contains("UNA sustitucion")), r.problemas().toString());
        }

        @Test
        @DisplayName("REV2 (negativa): sustitucion + cambio de dosis en la misma sesion se rechaza")
        void sustitucionMasDosis() {
            var lista = new ArrayList<>(sustituir(101L, EQUIVALENTE));
            lista.set(lista.indexOf(ej(100L, 3, 10)), ej(100L, 3, 9));
            var r = corregir(PlanningPolicy.REV2, TipoIntervencion.INVESTIGAR_CAMBIO_MINIMO, lista);
            assertFalse(r.publicable());
            assertTrue(r.problemas().stream().anyMatch(p -> p.contains("Una cosa o la otra")), r.problemas().toString());
        }

        @Test
        @DisplayName("REV2 (negativa): sustituir en una sesion NO senalada se rechaza")
        void noSenalada() {
            var ctx = contexto(orden(TipoIntervencion.INVESTIGAR_CAMBIO_MINIMO),
                    medida(Map.of(), Set.of(), Map.of(), Map.of(), Map.of()), elegibles()).conPolitica(PlanningPolicy.REV2);
            var otra = new ArrayList<>(ejercicios(1, 6));
            otra.set(1, ej(EQUIVALENTE, 3, 10));
            var correccion = new CorrectedSession(S0 + 1, LUNES.plusWeeks(1).plusDays(2), minutos(otra), otra, "otra");
            var e = assertThrows(IllegalArgumentException.class,
                    () -> productor.produce(ctx, copia(Map.of()), List.of(correccion), elegibles()));
            assertTrue(e.getMessage().contains("NO senalo"), e.getMessage());
        }

        @Test
        @DisplayName("P-C2 con REV2: en VARIANTE, sustituir por un ejercicio de mas impacto se rechaza; HISTORICA lo admitia")
        void varianteNoSubeImpacto() {
            var rev2 = corregir(PlanningPolicy.REV2, TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA,
                    sustituir(101L, MAS_IMPACTO));
            assertFalse(rev2.publicable());
            assertTrue(rev2.problemas().stream().anyMatch(p -> p.contains("no puede aumentar el impacto")),
                    rev2.problemas().toString());
            var hist = corregir(PlanningPolicy.HISTORICA, TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA,
                    sustituir(101L, MAS_IMPACTO));
            assertTrue(hist.problemas().stream().noneMatch(p -> p.contains("impacto")), hist.problemas().toString());
        }
    }

    // =======================================================================
    // H14
    // =======================================================================

    @Nested
    @DisplayName("H14: una dosis heredada de GAIN_MUSCLE no vale con otro objetivo (V24 revalida con el objetivo vigente)")
    class CambioDeObjetivo {

        @Test
        @DisplayName("la progresion 4D (3x11/90 s) con linaje, revalidada con LOSE_WEIGHT, no se acepta por herencia")
        void progresionGmConOtroObjetivo() {
            for (var politica : List.of(PlanningPolicy.HISTORICA, PlanningPolicy.REV2)) {
                var gm = contexto(PlanAdjustment.none(), medida(lunesCon(EJ, ej(EJ, 3, 11)), Set.of(),
                        Map.of(S0, Set.of(EJ)), Map.of(), Map.of()), elegibles()).conPolitica(politica);
                var lw = conObjetivo(gm, "LOSE_WEIGHT");
                var copia = copiaCon(EJ, ej(EJ, 3, 11));
                var enGm = productor.produce(gm, copia, List.of(), elegibles());
                assertTrue(enGm.problemas().stream().noneMatch(p -> p.startsWith("V24: el ejercicio " + EJ + " ")),
                        politica.id() + " " + enGm.problemas());
                // Con la dosis por capacidad activa (capability-validation), V24 revalida la dosis heredada contra
                // el rango del objetivo VIGENTE: la autorizacion HEREDADA no la salva.
                var conCapacidad = new AdaptedWeekProducer(new SessionFixApplier(), new ConservationValidator(),
                        estricta(DoseEnforcement.ENFORCE));
                var enLw = conCapacidad.produce(lw, copia, List.of(), elegibles());
                assertFalse(enLw.publicable(), politica.id() + ": con otro objetivo la dosis de GM no se hereda");
                assertTrue(enLw.problemas().stream().anyMatch(p -> p.startsWith("V24: el ejercicio " + EJ + " ")),
                        politica.id() + " " + enLw.problemas());
            }
        }
    }

    private static PlanGenerationContext conObjetivo(PlanGenerationContext c, String objetivo) {
        var p = c.profile();
        var perfil = new PlanningProfile(p.userId(), p.age(), p.biologicalSex(), p.heightCm(), p.weightKg(),
                p.targetWeightKg(), p.fitnessLevel(), objetivo, p.goalText(), p.healthNotes(), p.trainingLocation(),
                p.daysPerWeek(), p.availableDays(), p.sessionMinutes(), p.equipmentCodes(), p.blockedExerciseIds(),
                p.declaradas());
        return new PlanGenerationContext(c.userId(), c.weekNumber(), c.weekStartDate(), c.weekEndDate(), perfil,
                c.adjustment(), c.previousWeek(), c.availableExercises(), c.safety(), c.prescription(), c.policy());
    }

    private static WeekDerivation copiaCon(long id, PlanDraft.DraftExercise dosis) {
        var semana = semanaNueva(Map.of());
        var derivadas = new ArrayList<DerivedWorkout>();
        for (int i = 0; i < SESIONES; i++) {
            var w = semana.get(i);
            if (i == 0) {
                var lista = new ArrayList<>(w.exercises());
                lista.replaceAll(e -> e.exerciseId() == id ? dosis : e);
                w = new PlanDraft.DraftWorkout(w.scheduledDate(), w.focus(), w.name(), minutos(lista), lista);
            }
            derivadas.add(new DerivedWorkout(S0 + i, w));
        }
        return new WeekDerivation(derivadas, List.of());
    }

    // =======================================================================
    // Material
    // =======================================================================

    private AdaptedWeekResult corregir(PlanningPolicy politica, TipoIntervencion intervencion,
                                       List<PlanDraft.DraftExercise> lunes) {
        return corregir(politica, intervencion, lunes, Map.of(), Map.of());
    }

    private AdaptedWeekResult corregir(PlanningPolicy politica, TipoIntervencion intervencion,
                                       List<PlanDraft.DraftExercise> lunes, Map<Long, Integer> fatiga,
                                       Map<Long, String> motivos) {
        var ctx = contexto(orden(intervencion), medida(Map.of(), Set.of(), Map.of(), fatiga, motivos), elegibles())
                .conPolitica(politica);
        var correccion = new CorrectedSession(S0, LUNES.plusWeeks(1), minutos(lunes), lunes, "correccion");
        return productor.produce(ctx, copia(Map.of()), List.of(correccion), elegibles());
    }

    private static List<PlanDraft.DraftExercise> conSeries(Map<Long, Integer> series) {
        var lista = new ArrayList<PlanDraft.DraftExercise>();
        for (var e : ejercicios(0, 6))
            lista.add(series.containsKey(e.exerciseId()) ? ej(e.exerciseId(), series.get(e.exerciseId()), e.plannedReps())
                    : e);
        return lista;
    }

    private static List<PlanDraft.DraftExercise> sustituir(long quitado, long puesto) {
        var lista = new ArrayList<PlanDraft.DraftExercise>();
        for (var e : ejercicios(0, 6)) lista.add(e.exerciseId() == quitado ? ej(puesto, 3, 10) : e);
        return lista;
    }

    private static StrictValidationAdapter estricta() {
        return estricta(DoseEnforcement.OFF);
    }

    private static StrictValidationAdapter estricta(DoseEnforcement capacidad) {
        var configuracion = mock(ExternalConfigurationService.class);
        when(configuracion.durationToRepsDivisor()).thenReturn(DIVISOR);
        return new StrictValidationAdapter(new PlanDraftValidator(ESTIMADOR, SlotCoverageEnforcement.ENFORCE_AI_ONLY,
                DoseEnforcement.ENFORCE, capacidad), configuracion);
    }

    private static WeekDerivation copia(Map<Long, Short> seriesLunes) {
        var semana = semanaNueva(seriesLunes);
        var derivadas = new ArrayList<DerivedWorkout>();
        for (int i = 0; i < SESIONES; i++) derivadas.add(new DerivedWorkout(S0 + i, semana.get(i)));
        return new WeekDerivation(derivadas, List.of());
    }

    private static List<PlanDraft.DraftWorkout> semanaNueva(Map<Long, Short> seriesLunes) {
        var semana = new ArrayList<PlanDraft.DraftWorkout>();
        for (int i = 0; i < SESIONES; i++) {
            var lista = i == 0 ? conSeriesCortas(seriesLunes) : ejercicios(i, 6);
            semana.add(new PlanDraft.DraftWorkout(dia(i).plusWeeks(1), WorkoutFocus.FULL_BODY, "Sesion " + (i + 1),
                    minutos(lista), lista));
        }
        return semana;
    }

    private static List<PlanDraft.DraftExercise> conSeriesCortas(Map<Long, Short> series) {
        var lista = new ArrayList<PlanDraft.DraftExercise>();
        for (var e : ejercicios(0, 6))
            lista.add(series.containsKey(e.exerciseId()) ? ej(e.exerciseId(), series.get(e.exerciseId()), 10) : e);
        return lista;
    }

    private static PreviousWeekSummary medida(Map<Long, Short> seriesLunes, Set<Long> reduccion,
                                              Map<Long, Set<Long>> linaje, Map<Long, Integer> fatiga,
                                              Map<Long, String> motivos) {
        return medida(conSeriesCortas(seriesLunes), reduccion, linaje, fatiga, motivos);
    }

    private static List<PlanDraft.DraftExercise> lunesCon(long id, PlanDraft.DraftExercise dosis) {
        var lista = new ArrayList<>(ejercicios(0, 6));
        lista.replaceAll(e -> e.exerciseId() == id ? dosis : e);
        return lista;
    }

    private static PreviousWeekSummary medida(List<PlanDraft.DraftExercise> lunes, Set<Long> reduccion,
                                              Map<Long, Set<Long>> linaje, Map<Long, Integer> fatiga,
                                              Map<Long, String> motivos) {
        var sesiones = new ArrayList<PreviousWeekSummary.WorkoutOutcome>();
        var prescripciones = new ArrayList<PreviousWeekSummary.PrescriptionOutcome>();
        for (int i = 0; i < SESIONES; i++) {
            var lista = i == 0 ? lunes : ejercicios(i, 6);
            sesiones.add(new PreviousWeekSummary.WorkoutOutcome(S0 + i, dia(i), "FULL_BODY", "COMPLETED", null, true,
                    "COMPLETED", (short) 6, new BigDecimal("100.00"), "Sesion " + (i + 1), (short) minutos(lista)));
            for (var e : lista) {
                String motivo = i == 0 ? motivos.get(e.exerciseId()) : null;
                prescripciones.add(new PreviousWeekSummary.PrescriptionOutcome(S0 + i, dia(i), e.exerciseId(),
                        "ejercicio " + e.exerciseId(), "SETS_REPS", e.plannedSets(), e.plannedReps(), null, null,
                        e.plannedSets(), e.plannedSets() * e.plannedReps(), null, null, new BigDecimal("100.00"),
                        motivo == null ? "COMPLETED" : "SKIPPED", motivo, motivo == null ? "COMPLETED" : "SKIPPED",
                        e.restSeconds()));
            }
        }
        return new PreviousWeekSummary(new BigDecimal("100.00"), new BigDecimal("6.0"), null, Map.of(),
                sesiones, prescripciones, List.of(), reduccion, linaje, fatiga);
    }

    private static List<PlanDraft.DraftExercise> ejercicios(int sesion, int colocaciones) {
        var lista = new ArrayList<PlanDraft.DraftExercise>();
        for (int j = 0; j < colocaciones; j++) lista.add(ej(100L + sesion * 10L + j, j >= 4 ? 2 : 3, 10));
        return List.copyOf(lista);
    }

    private static PlanDraft.DraftExercise ej(long id, int series, int reps) {
        return new PlanDraft.DraftExercise(id, PrescriptionType.SETS_REPS, (short) series, (short) reps, null, null,
                (short) 90, null);
    }

    private static LocalDate dia(int i) {
        return LUNES.plusDays(i * 2L);
    }

    private static int minutos(List<PlanDraft.DraftExercise> lista) {
        return ESTIMADOR.estimateMinutes(new PlanDraft.DraftWorkout(LUNES, WorkoutFocus.FULL_BODY, "m", 1, lista),
                parametros());
    }

    private static final List<String> PATRONES = List.of("KNEE_DOMINANT", "HORIZONTAL_PUSH", "HORIZONTAL_PULL",
            "HIP_HINGE", "ISOMETRIC_HOLD", "CARRY");
    private static final List<String> PARTES = List.of("upper legs", "chest", "back", "upper legs", "shoulders",
            "chest");

    private static Map<Long, CandidateExercise> elegibles() {
        var porId = new LinkedHashMap<Long, CandidateExercise>();
        for (int s = 0; s < SESIONES; s++)
            for (int j = 0; j < PATRONES.size(); j++) {
                long id = 100L + s * 10L + j;
                porId.put(id, new CandidateExercise(id, "ejercicio " + id, PARTES.get(j), "body weight", 1,
                        PrescriptionType.SETS_REPS, null, PATRONES.get(j), Boolean.FALSE));
            }
        porId.put(EQUIVALENTE, cand(EQUIVALENTE, "Flexion inclinada", "chest", 1, "HORIZONTAL_PUSH", false));
        porId.put(EQUIVALENTE_2, cand(EQUIVALENTE_2, "Remo con banda", "back", 1, "HORIZONTAL_PULL", false));
        porId.put(MAS_DIFICIL, cand(MAS_DIFICIL, "Flexion arquero", "chest", 2, "HORIZONTAL_PUSH", false));
        porId.put(MAS_IMPACTO, cand(MAS_IMPACTO, "Flexion con palmada", "chest", 1, "HORIZONTAL_PUSH", true));
        porId.put(OTRO_PATRON, cand(OTRO_PATRON, "Sentadilla", "chest", 1, "KNEE_DOMINANT", false));
        porId.put(OTRO_GRUPO, cand(OTRO_GRUPO, "Press de hombro", "shoulders", 1, "HORIZONTAL_PUSH", false));
        return porId;
    }

    private static CandidateExercise cand(long id, String nombre, String parte, int dificultad, String patron,
                                          boolean impacto) {
        return new CandidateExercise(id, nombre, parte, "body weight", dificultad, PrescriptionType.SETS_REPS, null,
                patron, impacto);
    }

    private static PrescriptionParams parametros() {
        return new PrescriptionParams(null, null, null, null, null, null, null, null, null, null);
    }

    private static PlanAdjustment orden(TipoIntervencion intervencion) {
        return new PlanAdjustment(List.of(AdjustmentType.NONE), null, null, null, 0.0, 0.0, null, null, null,
                "causa", null, null, List.of(new PlanAdjustment.TargetedSession(S0, intervencion)));
    }

    private static PlanGenerationContext contexto(PlanAdjustment ajuste, PreviousWeekSummary previa,
                                                  Map<Long, CandidateExercise> elegibles) {
        return new PlanGenerationContext(USUARIO, (short) 2, LUNES.plusWeeks(1), LUNES.plusWeeks(1).plusDays(6),
                new PlanningProfile(USUARIO, 30, "MALE", new BigDecimal("175.0"), new BigDecimal("72.0"),
                        new BigDecimal("75.0"), "BEGINNER", "GAIN_MUSCLE", "Ganar masa", "Ninguna", "HOME", SESIONES,
                        List.of((short) 1, (short) 3, (short) 5), 45, List.of("body weight", "dumbbell", "band"),
                        List.of(), SafetyProfile.Declaradas.sinLimitaciones()),
                ajuste, previa, List.copyOf(elegibles.values()), SafetyProfile.none(), parametros());
    }
}
