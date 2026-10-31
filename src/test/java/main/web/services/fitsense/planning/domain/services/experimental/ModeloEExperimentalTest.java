package main.web.services.fitsense.planning.domain.services.experimental;

import main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Modelo E experimental: parametros, volumen, validador candidato y aislamiento de produccion")
class ModeloEExperimentalTest {

    static final LocalDate L = LocalDate.of(2026, 11, 2);
    static final PrescriptionParams P = new PrescriptionParams(70, 3, 60, 5, 60, 20, null, null, 10, 180);

    static CandidateExercise c(long id, String grupo, PrescriptionType t) {
        return new CandidateExercise(id, "e" + id, grupo, "body weight", 1, t, null, null);
    }

    static PlanDraft.DraftExercise sr(long id, int series, int reps) {
        return new PlanDraft.DraftExercise(id, PrescriptionType.SETS_REPS, (short) series, (short) reps, null, null,
                (short) 90, null);
    }

    @Test
    @DisplayName("desactivado por defecto, version marcada como sin aprobar")
    void desactivado() {
        var p = ParametrosModeloE.desactivado();
        assertFalse(p.habilitado());
        assertTrue(p.version().contains("SIN-APROBAR"));
        assertEquals(1.0, p.techoTiempoFraccion(), "la tolerancia de V4 no es un objetivo");
    }

    @Test
    @DisplayName("objetivo semanal escalado por dias: GAIN_MUSCLE 4/8/10, resto 4")
    void escalado() {
        var p = ParametrosModeloE.desactivado();
        assertEquals(4, p.objetivoSemanal("GAIN_MUSCLE", 1));
        assertEquals(8, p.objetivoSemanal("GAIN_MUSCLE", 2));
        assertEquals(10, p.objetivoSemanal("GAIN_MUSCLE", 3));
        assertEquals(10, p.objetivoSemanal("GAIN_MUSCLE", 6));
        assertEquals(4, p.objetivoSemanal("LOSE_WEIGHT", 1));
        assertEquals(4, p.objetivoSemanal("GENERAL_FITNESS", 6));
        assertEquals(1800, p.techoSegundos(30));
    }

    @Test
    @DisplayName("volumen: series directas por grupo y sesion/semana; cardio en minutos; sin complementos")
    void volumen() {
        var e = Map.of(1L, c(1, "chest", PrescriptionType.SETS_REPS), 2L, c(2, "chest", PrescriptionType.SETS_REPS),
                3L, c(3, "upper legs", PrescriptionType.SETS_REPS), 9L, c(9, "waist", PrescriptionType.SETS_REPS));
        var w1 = new PlanDraft.DraftWorkout(L, WorkoutFocus.FULL_BODY, "a", 30, List.of(sr(1, 3, 10), sr(2, 3, 10), sr(9, 2, 13)));
        var w2 = new PlanDraft.DraftWorkout(L.plusDays(2), WorkoutFocus.FULL_BODY, "b", 30, List.of(sr(3, 3, 10)));
        var v = VolumenSemanal.medir(new PlanDraft(GenerationSource.RULE_ENGINE, null, "x", null, null, List.of(w1, w2)), e,
                Set.of(9L));
        assertEquals(6, v.semanal("chest"));
        assertEquals(3, v.semanal("upper legs"));
        assertEquals(0, v.semanal("waist"), "los complementos no cuentan");
        assertEquals(6, v.maximoPorGrupoYSesion());
    }

    @Test
    @DisplayName("validador candidato: E1 tiempo, E2 sesion, E4 objetivo semanal")
    void validador() {
        var e = new HashMap<Long, CandidateExercise>();
        var ejercicios = new ArrayList<PlanDraft.DraftExercise>();
        for (long i = 1; i <= 5; i++) {
            e.put(i, c(i, "chest", PrescriptionType.SETS_REPS));
            ejercicios.add(sr(i, 3, 10));
        }
        var w = new PlanDraft.DraftWorkout(L, WorkoutFocus.FULL_BODY, "a", 30, ejercicios);
        var d = new PlanDraft(GenerationSource.RULE_ENGINE, null, "x", null, null, List.of(w));
        var h = ValidadorModeloE.evaluar(d, "GAIN_MUSCLE", 20, P, e, Set.of(), ParametrosModeloE.desactivado());
        var codigos = h.stream().map(ValidadorModeloE.Hallazgo::codigo).toList();
        assertTrue(codigos.contains("E1"), "5x(3x10/90) + 5 min + transiciones > 20 min: " + h);
        assertTrue(codigos.contains("E2"), "15 series de pecho > techo blando 10: " + h);
        assertTrue(codigos.contains("E4"), "espalda, hombros y pierna sin series con 1 dia (objetivo 4): " + h);
    }

    @Test
    @DisplayName("aislamiento: ninguna clase de produccion fuera de 'experimental' usa el modelo E")
    void aislamiento() throws IOException {
        var raiz = Path.of("src/main/java");
        assertTrue(Files.isDirectory(raiz), "se ejecuta desde la raiz del modulo");
        try (Stream<Path> fuentes = Files.walk(raiz)) {
            var infractores = fuentes.filter(f -> f.toString().endsWith(".java"))
                    .filter(f -> !f.toString().contains("/experimental/"))
                    .filter(f -> {
                        try {
                            String s = Files.readString(f);
                            return s.contains("ModeloE") || s.contains(".experimental.");
                        } catch (IOException ex) {
                            return true;
                        }
                    }).toList();
            assertEquals(List.of(), infractores, "el modelo E no puede estar cableado en la ruta de produccion");
        }
    }
}
