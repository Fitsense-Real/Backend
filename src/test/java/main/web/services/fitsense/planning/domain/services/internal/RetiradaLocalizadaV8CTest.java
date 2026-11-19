package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static main.web.services.fitsense.planning.domain.services.V8CConservacionPatronH1Test.CAT;
import static org.junit.jupiter.api.Assertions.*;

/**
 * RETIRADA LOCALIZADA POR DOLOR (candidata) en V8C de continuidad. Semana medida: la sesion A (9001 y 72) y la sesion
 * B (580 con DOLOR identificado, 360 y 389 hechos). 580 (HORIZONTAL_PUSH, estructural) queda excluido por dolor
 * registrado. La semana nueva conserva A y retira SOLO 580 de B, con menos minutos. Datos del catalogo V44_2.
 */
@DisplayName("Retirada localizada por dolor: V8C solo la admite si es exactamente lo autorizado")
class RetiradaLocalizadaV8CTest {

    static final LocalDate L1 = LocalDate.of(2026, 11, 2), L2 = L1.plusWeeks(1);
    static final long A = 1L, B = 2L;

    static PreviousWeekSummary.PrescriptionOutcome presc(long w, long id, boolean dolor) {
        return new PreviousWeekSummary.PrescriptionOutcome(w, w == A ? L1 : L1.plusDays(2), id, "e" + id, "SETS_REPS",
                (short) 3, (short) 10, null, null, null, null, null, null,
                dolor ? BigDecimal.ZERO : new BigDecimal("100"), dolor ? "SKIPPED" : "COMPLETED",
                dolor ? "PAIN_OR_DISCOMFORT" : null, "COMPLETED", (short) 90);
    }

    static PreviousWeekSummary medida(List<Long> sesionA) {
        var p = new ArrayList<PreviousWeekSummary.PrescriptionOutcome>();
        for (var id : sesionA) p.add(presc(A, id, false));
        p.add(presc(B, 580L, true));
        p.add(presc(B, 360L, false));
        p.add(presc(B, 389L, false));
        return new PreviousWeekSummary(new BigDecimal("90"), null, 0, Map.of(), List.of(
                new PreviousWeekSummary.WorkoutOutcome(A, L1, "FULL_BODY", "COMPLETED", null, true, "COMPLETED",
                        (short) 6, new BigDecimal("100"), "A", (short) 45),
                new PreviousWeekSummary.WorkoutOutcome(B, L1.plusDays(2), "FULL_BODY", "COMPLETED", null, true,
                        "COMPLETED", (short) 6, new BigDecimal("67"), "B", (short) 45)), p, List.of());
    }

    static PlanDraft.DraftExercise ej(long id, int series) {
        return new PlanDraft.DraftExercise(id, PrescriptionType.SETS_REPS, (short) series, (short) 10, null, null,
                (short) 90, null);
    }

    static WeekDerivation semana(List<Long> sesionA, List<PlanDraft.DraftExercise> sesionB, int minutosB) {
        return new WeekDerivation(List.of(
                new DerivedWorkout(A, new PlanDraft.DraftWorkout(L2, WorkoutFocus.FULL_BODY, "A", 45,
                        sesionA.stream().map(id -> ej(id, 3)).toList())),
                new DerivedWorkout(B, new PlanDraft.DraftWorkout(L2.plusDays(2), WorkoutFocus.FULL_BODY, "B", minutosB,
                        sesionB))), List.of());
    }

    static ExclusionesDeSeguridad excluido(String motivo) {
        return new ExclusionesDeSeguridad(Map.of(580L, CAT.get(580L)), Map.of(580L, motivo));
    }

    static Map<Long, CandidateExercise> elegibles(boolean con580) {
        var m = new LinkedHashMap<>(CAT);
        if (!con580) m.remove(580L);
        return m;
    }

    static RetiradaSeguridad retirada(long sesion, String motivo, int minutosDespues) {
        return new RetiradaSeguridad(sesion, L2.plusDays(2), 580L, "HORIZONTAL_PUSH", motivo, "3x10 descanso 90", 45,
                minutosDespues);
    }

    static ConservationReport validar(List<Long> sesionA, WeekDerivation semana, ExclusionesDeSeguridad excl,
                                      Map<Long, CandidateExercise> elegibles, List<RetiradaSeguridad> retiradas) {
        return new ConservationValidator().validateContinuidad(semana, medida(sesionA), elegibles, excl, List.of(),
                retiradas, null, false);
    }

    static final List<Long> SESION_A = List.of(9001L, 72L);
    static final String DOLOR = SafetyConflictPolicy.DOLOR_REGISTRADO;

    @Test
    @DisplayName("retirar solo 580 de B, el resto identico y menos minutos: se admite y B queda conservada")
    void admitida() {
        var r = validar(SESION_A, semana(SESION_A, List.of(ej(360L, 3), ej(389L, 3)), 35), excluido(DOLOR),
                elegibles(false), List.of(retirada(B, DOLOR, 35)));
        assertTrue(r.problems().isEmpty(), r.problems()::toString);
        assertTrue(r.conservadas().contains(B));
    }

    @Test
    @DisplayName("sin declarar la retirada, V8C la rechaza como hoy (desaparece un ejercicio y bajan los minutos)")
    void sinDeclarar() {
        var r = validar(SESION_A, semana(SESION_A, List.of(ej(360L, 3), ej(389L, 3)), 35), excluido(DOLOR),
                elegibles(false), List.of());
        assertTrue(r.problems().stream().anyMatch(p -> p.contains("desaparecen los ejercicios [580]")),
                r.problems()::toString);
    }

    @Test
    @DisplayName("una causa que no es dolor (brazos_arriba) no autoriza retirar sin sustituto")
    void otraCausa() {
        var r = validar(SESION_A, semana(SESION_A, List.of(ej(360L, 3), ej(389L, 3)), 35),
                excluido(SafetyConflictPolicy.BRAZOS_ARRIBA), elegibles(false),
                List.of(retirada(B, SafetyConflictPolicy.BRAZOS_ARRIBA, 35)));
        assertTrue(r.problems().stream().anyMatch(p -> p.contains("solo el dolor autoriza")), r.problems()::toString);
    }

    @Test
    @DisplayName("si 580 sigue siendo elegible, no hay motivo de seguridad: se rechaza")
    void sigueElegible() {
        var r = validar(SESION_A, semana(SESION_A, List.of(ej(360L, 3), ej(389L, 3)), 35), excluido(DOLOR),
                elegibles(true), List.of(retirada(B, DOLOR, 35)));
        assertTrue(r.problems().stream().anyMatch(p -> p.contains("sigue siendo elegible")), r.problems()::toString);
    }

    @Test
    @DisplayName("declarar la retirada y seguir prescribiendo 580: se rechaza (lo que dolio no vuelve)")
    void siguePrescrito() {
        var r = validar(SESION_A, semana(SESION_A, List.of(ej(580L, 3), ej(360L, 3), ej(389L, 3)), 45),
                excluido(DOLOR), elegibles(false), List.of(retirada(B, DOLOR, 45)));
        assertFalse(r.problems().isEmpty());
    }

    @Test
    @DisplayName("retirar 580 y ademas bajar la dosis de 360: se rechaza (lo demas se conserva tal cual)")
    void ademasDosis() {
        var r = validar(SESION_A, semana(SESION_A, List.of(ej(360L, 2), ej(389L, 3)), 33), excluido(DOLOR),
                elegibles(false), List.of(retirada(B, DOLOR, 33)));
        assertTrue(r.problems().stream().anyMatch(p -> p.contains("lo demas se conserva tal cual")),
                r.problems()::toString);
    }

    @Test
    @DisplayName("retirar 580 y ademas 389: se rechaza")
    void ademasOtraRetirada() {
        var r = validar(SESION_A, semana(SESION_A, List.of(ej(360L, 3)), 25), excluido(DOLOR), elegibles(false),
                List.of(retirada(B, DOLOR, 25)));
        assertFalse(r.problems().isEmpty());
        assertTrue(r.problems().stream().anyMatch(p -> p.contains("389")), r.problems()::toString);
    }

    @Test
    @DisplayName("retirar 580 y anadir 56: se rechaza (no se incorporan ejercicios nuevos)")
    void ademasAnade() {
        var r = validar(SESION_A, semana(SESION_A, List.of(ej(360L, 3), ej(389L, 3), ej(56L, 3)), 45),
                excluido(DOLOR), elegibles(false), List.of(retirada(B, DOLOR, 45)));
        assertFalse(r.problems().isEmpty());
    }

    @Test
    @DisplayName("los minutos no pueden subir al retirar")
    void minutosSuben() {
        var r = validar(SESION_A, semana(SESION_A, List.of(ej(360L, 3), ej(389L, 3)), 50), excluido(DOLOR),
                elegibles(false), List.of(retirada(B, DOLOR, 50)));
        assertTrue(r.problems().stream().anyMatch(p -> p.contains("mas que los 45")), r.problems()::toString);
    }

    @Test
    @DisplayName("580 es estructural (HORIZONTAL_PUSH): si ningun otro ejercicio de la semana cubre su slot, se rechaza")
    void slotSinCobertura() {
        var sinCobertura = List.of(9001L);
        var r = validar(sinCobertura, semana(sinCobertura, List.of(ej(360L, 3), ej(389L, 3)), 35), excluido(DOLOR),
                elegibles(false), List.of(retirada(B, DOLOR, 35)));
        assertTrue(r.problems().stream().anyMatch(p -> p.contains("colocacion estructural")), r.problems()::toString);
    }

    @Test
    @DisplayName("una retirada declarada en una sesion que no esta en la semana nueva es un problema")
    void sesionInexistente() {
        var r = validar(SESION_A, semana(SESION_A, List.of(ej(360L, 3), ej(389L, 3)), 35), excluido(DOLOR),
                elegibles(false), List.of(retirada(B, DOLOR, 35), retirada(77L, DOLOR, 35)));
        assertTrue(r.problems().stream().anyMatch(p -> p.contains("sesion 77")), r.problems()::toString);
    }

    @Test
    @DisplayName("la sesion A (no senalada, sin retirada) sigue sin poder cambiar")
    void noSenaladaIntacta() {
        var r = validar(SESION_A, semana(List.of(9001L), List.of(ej(360L, 3), ej(389L, 3)), 35), excluido(DOLOR),
                elegibles(false), List.of(retirada(B, DOLOR, 35)));
        assertTrue(r.problems().stream().anyMatch(p -> p.contains("sesion 1 no estaba senalada")),
                r.problems()::toString);
    }

    @Test
    @DisplayName("configuracion: off por defecto, on la enciende, otro valor es un error")
    void configuracion() {
        assertFalse(AdaptedWeekProducer.retiradaDesde(null));
        assertFalse(AdaptedWeekProducer.retiradaDesde("off"));
        assertTrue(AdaptedWeekProducer.retiradaDesde(" ON "));
        assertThrows(IllegalArgumentException.class, () -> AdaptedWeekProducer.retiradaDesde("si"));
    }
}
