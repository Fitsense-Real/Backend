package main.web.services.fitsense.planning.infrastructure.generation.ai;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** El mismo snapshot de generacion que usa SessionFixPromptBuilderTest, para las pruebas del prompt V2. */
final class PlanInputSnapshotFixture {

    private static final LocalDate L1 = LocalDate.of(2026, 10, 12);
    private static final LocalDate L2 = LocalDate.of(2026, 10, 19);

    private PlanInputSnapshotFixture() {
    }

    static PlanInputSnapshot snapshot() {
        var ejercicios = List.of(
                catalogo(740L, "Dominadas", "BACK", "PULL"),
                catalogo(304L, "Press banca", "CHEST", "PUSH_H"),
                catalogo(592L, "Sentadilla", "LEGS", "SQUAT"));
        var previas = List.of(
                new PlanInputSnapshot.PreviousWorkout(L1, "FULL_BODY", "COMPLETED", null, true, (short) 7,
                        new BigDecimal("100.00"), List.of(previo(740L, "Dominadas", new BigDecimal("57.5")))));
        var constraints = new PlanInputSnapshot.Constraints(
                (short) 2, L2, L2.plusDays(6), 3,
                List.of((short) 1, (short) 3, (short) 5), 45, "GYM", 3,
                12, 10, 3, 31, 2, 120,
                List.of(new PlanInputSnapshot.SuggestedSession(L2, "FULL_BODY", List.of("BACK", "CHEST"), 2, 3)),
                List.of(new PlanInputSnapshot.ReduceVolumeCap(740L, 3, 10, "V19")),
                List.of(new PlanInputSnapshot.DosisPorClase("BASICO", 3, 10, 90, 3, 7.5)),
                4, 38.25, List.of("PULL", "PUSH_H", "SQUAT", "HINGE"));
        var adjustment = new PlanInputSnapshot.Adjustment(List.of("NONE"), null, null, null, 0.0, 0.0, 3,
                "baja adherencia", "LACK_OF_TIME", "reparte mejor la semana");
        var previousWeek = new PlanInputSnapshot.PreviousWeek(new BigDecimal("66.67"), new BigDecimal("6.5"), 480,
                Map.of("BACK", 1), previas);
        return new PlanInputSnapshot("GEN-IN-SCHEMA", "PRIN-1.0", "GEN-IN-1.6",
                new PlanInputSnapshot.User(30, "M", new BigDecimal("175"), new BigDecimal("75"),
                        new BigDecimal("72"), "INTERMEDIATE", "GAIN_MUSCLE", "ganar musculo", null,
                        List.of("body weight")),
                constraints, adjustment, previousWeek, ejercicios, null);
    }

    private static PlanInputSnapshot.AvailableExercise catalogo(long id, String nombre, String zona, String patron) {
        return new PlanInputSnapshot.AvailableExercise(id, nombre, zona, "body weight", 2,
                "SETS_REPS", nombre, 8, patron, patron, "BASICO");
    }

    private static PlanInputSnapshot.PreviousExercise previo(long id, String nombre, BigDecimal carga) {
        return new PlanInputSnapshot.PreviousExercise(id, nombre, "SETS_REPS",
                (short) 3, (short) 10, null, (short) 3, 30, null, carga,
                new BigDecimal("100.00"), "COMPLETED", null);
    }
}
