package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.ExclusionesDeSeguridad;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.planning.domain.services.SafetySubstitutionPolicy.Estrategia;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;

import static main.web.services.fitsense.planning.domain.services.V8CConservacionPatronH1Test.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * VALIDACION FINAL DEL LIMITE DE DIFICULTAD · DOLOR, casos A-G e I, sobre la reparacion REAL de SUST-SEG-1
 * ({@link SafetySubstitutionPolicy#reparar}) y V8C ante SEGURIDAD_ANTE_DOLOR, con las tres estrategias:
 * A = HISTORICA (off, la de hoy), B = LIMITE (on), C = LIMITE_Y_PREFERENCIA (preferencia).
 * <p>
 * El ejercicio que dolio es 70001 (dificultad 2, chest, HORIZONTAL_PUSH, SETS_REPS) en la sesion senalada. Como en
 * produccion, el conjunto elegible ya NO contiene ningun ejercicio excluido por dolor
 * (WeeklyTrainingPlanCommandServiceImpl: P-02 los filtra antes de preparar la semana; SafetyExclusionResolver solo
 * declara exclusiones fuera del conjunto elegible). Los casos H (tres intentos invalidos de la IA, respaldo completo)
 * y J (adaptaciones previas e historial) son de extremo a extremo: RespaldoConDolorIT, SinSustitutoDolor*IT y el
 * REPLAY de la campana P123 (N08).
 */
@DisplayName("Limite de dificultad · dolor: casos A-G e I con A/B/C (SUST-SEG-1 real y V8C)")
class LimiteDificultadDolorTest {

    static final long DOLIO = 70001L;

    static CandidateExercise c(long id, int dif) {
        return new CandidateExercise(id, "e" + id, "chest", "body weight", dif, PrescriptionType.SETS_REPS,
                "pectorals", "HORIZONTAL_PUSH");
    }

    static ExclusionesDeSeguridad excluido(long... ids) {
        var o = new LinkedHashMap<Long, CandidateExercise>();
        var m = new LinkedHashMap<Long, String>();
        for (long id : ids) {
            o.put(id, id == DOLIO ? c(DOLIO, 2) : c(id, 2));
            m.put(id, SafetyConflictPolicy.DOLOR_REGISTRADO);
        }
        return new ExclusionesDeSeguridad(o, m);
    }

    /** Repara la semana (sesion senalada con 70001 y 9001; sesion conservada con 9001) y devuelve el resultado. */
    static SafetySubstitutionPolicy.Resultado reparar(Estrategia e, ExclusionesDeSeguridad excl,
                                                      Map<Long, CandidateExercise> elegibles) {
        var semana = derivacion(List.of(ej(DOLIO, 3, 10), ej(360L, 3, 10)), List.of(ej(9001L, 3, 10)));
        return SafetySubstitutionPolicy.reparar(semana.workouts(), semana("PAIN_OR_DISCOMFORT", List.of(DOLIO, 360L)),
                excl, elegibles, null, e);
    }

    static Optional<Long> sustituto(SafetySubstitutionPolicy.Resultado r) {
        assertTrue(r.sustituciones().size() <= 1, r.sustituciones()::toString);
        return r.sustituciones().stream().map(s -> s.puesto()).findFirst();
    }

    static Map<Long, CandidateExercise> elegibles(CandidateExercise... cs) {
        var m = new LinkedHashMap<Long, CandidateExercise>();
        for (var x : cs) m.put(x.exerciseId(), x);
        return m;
    }

    /** Lo que dolio no queda en la sesion senalada reparada, con o sin sustituto publicado. */
    static void invarianteNoReintroduce(SafetySubstitutionPolicy.Resultado r) {
        var senalada = r.semana().stream().filter(d -> d.sourcePlannedWorkoutId() == SENALADA).findFirst().orElseThrow();
        boolean sigue = senalada.workout().exercises().stream().anyMatch(x -> x.exerciseId() == DOLIO);
        // Sin sustituto, la reparacion lo deja en la sesion y lo DECLARA (problema): revisarYCerrar lo convierte en
        // NO_FACTIBLE/SIN_SUSTITUTO_SEGURO y la semana no se publica. Con sustituto, ya no esta.
        assertEquals(r.sustituciones().isEmpty(), sigue);
        if (sigue) assertFalse(r.problemas().isEmpty(), "sin sustituto tiene que quedar declarado");
    }

    @Test
    @DisplayName("A: hay sustituto del mismo nivel: B y C lo eligen; A elige el primero por id aunque sea mas dificil")
    void casoA() {
        var e = elegibles(c(70002L, 3), c(70003L, 2), c(70004L, 1));
        assertEquals(Optional.of(70002L), sustituto(reparar(Estrategia.HISTORICA, excluido(DOLIO), e)), "A: sube a 3");
        assertEquals(Optional.of(70003L), sustituto(reparar(Estrategia.LIMITE, excluido(DOLIO), e)), "B: el <= de id menor");
        assertEquals(Optional.of(70003L), sustituto(reparar(Estrategia.LIMITE_Y_PREFERENCIA, excluido(DOLIO), e)),
                "C: el del mismo nivel");
        for (var s : Estrategia.values()) invarianteNoReintroduce(reparar(s, excluido(DOLIO), e));
    }

    @Test
    @DisplayName("A2: con un menor de id mas bajo, B elige el menor y C el del mismo nivel (la unica diferencia B/C)")
    void casoA2() {
        var e = elegibles(c(70002L, 1), c(70003L, 2));
        assertEquals(Optional.of(70002L), sustituto(reparar(Estrategia.LIMITE, excluido(DOLIO), e)));
        assertEquals(Optional.of(70003L), sustituto(reparar(Estrategia.LIMITE_Y_PREFERENCIA, excluido(DOLIO), e)));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Estrategia.class)
    @DisplayName("B: solo hay sustitutos de menor dificultad: todas las estrategias eligen el mismo")
    void casoB(Estrategia s) {
        var r = reparar(s, excluido(DOLIO), elegibles(c(70004L, 1), c(70005L, 1)));
        assertEquals(Optional.of(70004L), sustituto(r));
        invarianteNoReintroduce(r);
    }

    @Test
    @DisplayName("C: solo hay sustitutos MAS dificiles: A sube; B y C no sustituyen y lo declaran (no se publica)")
    void casoC() {
        var e = elegibles(c(70002L, 3));
        assertEquals(Optional.of(70002L), sustituto(reparar(Estrategia.HISTORICA, excluido(DOLIO), e)));
        for (var s : List.of(Estrategia.LIMITE, Estrategia.LIMITE_Y_PREFERENCIA)) {
            var r = reparar(s, excluido(DOLIO), e);
            assertTrue(sustituto(r).isEmpty());
            assertTrue(r.problemas().get(0).contains("no hay sustituto elegible"), r.problemas()::toString);
            invarianteNoReintroduce(r);
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Estrategia.class)
    @DisplayName("D: no hay ningun sustituto (ni del slot): ninguna estrategia inventa uno; queda declarado")
    void casoD(Estrategia s) {
        var otroSlot = new CandidateExercise(70006L, "e", "upper legs", "body weight", 1, PrescriptionType.SETS_REPS,
                "quads", "KNEE_DOMINANT");
        var r = reparar(s, excluido(DOLIO), elegibles(otroSlot));
        assertTrue(sustituto(r).isEmpty());
        invarianteNoReintroduce(r);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Estrategia.class)
    @DisplayName("E: el sustituto del mismo nivel TAMBIEN dolio (excluido, fuera del elegible): se elige otro, nunca el excluido")
    void casoE(Estrategia s) {
        // 70003 (dif 2) dolio antes: P-02 lo saco del conjunto elegible. Queda 70004 (dif 1) y 70002 (dif 3).
        var e = elegibles(c(70002L, 3), c(70004L, 1));
        var r = reparar(s, excluido(DOLIO, 70003L), e);
        var elegido = sustituto(r).orElseThrow();
        assertNotEquals(70003L, elegido);
        assertEquals(s.limita() ? 70004L : 70002L, elegido);
        invarianteNoReintroduce(r);
    }

    @ParameterizedTest(name = "guard={0}")
    @ValueSource(strings = {"off", "on", "preferencia"})
    @DisplayName("F: el original ya retirado: una propuesta que lo vuelve a prescribir se rechaza con cualquier estrategia")
    void casoF(String guard) {
        var r = new ConservationValidator("multiconjunto", guard).validate(
                derivacion(List.of(ej(580L, 3, 10)), List.of(ej(9001L, 3, 10))), semana("PAIN_OR_DISCOMFORT",
                        List.of(580L)), orden(TipoIntervencion.SEGURIDAD_ANTE_DOLOR), CAT,
                ExclusionesDeSeguridad.ninguna(), List.of(), false, 30);
        assertFalse(r.aceptable());
        assertTrue(r.problems().stream().anyMatch(p -> p.contains("vuelve a prescribir [580]")), r.problems()::toString);
    }

    @ParameterizedTest(name = "guard={0}")
    @ValueSource(strings = {"off", "on", "preferencia"})
    @DisplayName("G: la IA propone un sustituto mas dificil ante dolor (580 dif 1 -> 56 dif 2): solo B y C lo rechazan")
    void casoG(String guard) {
        var r = new ConservationValidator("multiconjunto", guard).validate(
                derivacion(List.of(ej(56L, 3, 10)), List.of(ej(9001L, 3, 10))), semana("PAIN_OR_DISCOMFORT",
                        List.of(580L)), orden(TipoIntervencion.SEGURIDAD_ANTE_DOLOR), CAT,
                ExclusionesDeSeguridad.ninguna(), List.of(), false, 30);
        assertEquals("off".equals(guard), r.aceptable(), r.problems()::toString);
        if (!"off".equals(guard))
            assertTrue(r.problems().stream().anyMatch(p -> p.contains("limite de dificultad")), r.problems()::toString);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Estrategia.class)
    @DisplayName("I: un solo candidato valido: la eleccion es determinista (100 ordenes de insercion distintos)")
    void casoI(Estrategia s) {
        var base = new ArrayList<>(List.of(c(70004L, 1), c(70002L, 3),
                new CandidateExercise(70006L, "e", "upper legs", "body weight", 1, PrescriptionType.SETS_REPS, "quads",
                        "KNEE_DOMINANT")));
        var esperado = s.limita() ? 70004L : null;
        var rnd = new Random(20261103L);
        Set<Long> vistos = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            Collections.shuffle(base, rnd);
            var r = reparar(s, excluido(DOLIO), elegibles(base.toArray(CandidateExercise[]::new)));
            vistos.add(sustituto(r).orElse(-1L));
        }
        assertEquals(1, vistos.size(), "siempre el mismo: " + vistos);
        if (esperado != null) assertEquals(Set.of(esperado), vistos);
        else assertEquals(Set.of(70002L), vistos, "A: el primero por id (70002, que sube)");
    }

    @Test
    @DisplayName("Dosis del sustituto: la de la posicion original (no sube), con cualquier estrategia")
    void dosisNoSube() {
        for (var s : Estrategia.values()) {
            var r = reparar(s, excluido(DOLIO), elegibles(c(70004L, 1), c(70003L, 2)));
            var senalada = r.semana().stream().filter(d -> d.sourcePlannedWorkoutId() == SENALADA).findFirst()
                    .orElseThrow();
            PlanDraft.DraftExercise puesto = senalada.workout().exercises().get(0);
            assertEquals((short) 3, puesto.plannedSets());
            assertEquals((short) 10, puesto.plannedReps());
        }
    }
}
