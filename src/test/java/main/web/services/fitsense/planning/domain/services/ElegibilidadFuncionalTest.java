package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams;
import main.web.services.fitsense.planning.domain.exceptions.InvalidPlanDraftException;
import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EF-0.1 con el VALIDADOR REAL. Reproduce la sesion PULL sin material de los casos del 46 %: un empuje con zona
 * espalda (775), un patron de tronco con zona espalda y una flexion de codo. Con la historica pasa VEF (no existe);
 * con una version EF se rechaza y se dice si el catalogo tiene o no alguna traccion.
 */
@DisplayName("EF-0.1 · elegibilidad funcional en el validador real")
class ElegibilidadFuncionalTest {

    static final LocalDate LUNES = LocalDate.of(2026, 10, 12);
    static final PrescriptionParams P = new PrescriptionParams(0, 3, 60, 5, 60, 20, null, null, 10, 180);
    static final PlanDraftValidator VALIDADOR = new PlanDraftValidator(new SessionDurationEstimator(),
            SlotCoverageEnforcement.OBSERVE, DoseEnforcement.OBSERVE);

    static final CandidateExercise EMPUJE_ESPALDA = new CandidateExercise(775L, "empuje etiquetado espalda", "back",
            "body weight", 1, PrescriptionType.SETS_REPS, "lats", "HORIZONTAL_PUSH");
    static final CandidateExercise TRONCO_ESPALDA = new CandidateExercise(1261L, "flexion de tronco", "back",
            "body weight", 1, PrescriptionType.SETS_REPS, "lats", "TRUNK_FLEXION");
    static final CandidateExercise CURL = new CandidateExercise(320L, "flexion de codo", "upper arms", "body weight", 1,
            PrescriptionType.SETS_REPS, "biceps", "ELBOW_FLEXION");
    static final CandidateExercise HIPEREXTENSION = new CandidateExercise(1066L, "hiperextension", "back",
            "body weight", 1, PrescriptionType.SETS_REPS, "spine", "TRUNK_EXTENSION");
    static final CandidateExercise REMO = new CandidateExercise(9001L, "remo", "back", "body weight", 1,
            PrescriptionType.SETS_REPS, "lats", "HORIZONTAL_PULL");
    static final CandidateExercise MANCUERNA = new CandidateExercise(315L, "curl con registro incoherente",
            "upper arms", "body weight", 1, PrescriptionType.SETS_REPS, "biceps", "ELBOW_FLEXION");

    static PlanGenerationContext ctx(PlanningPolicy politica, List<CandidateExercise> c) {
        var perfil = new PlanningProfile(1L, 30, "FEMALE", new BigDecimal("168"), new BigDecimal("68"), null,
                "INTERMEDIATE", "GAIN_MUSCLE", "x", "Ninguna", "HOME", 1, List.of((short) 1), 60,
                List.of("body weight"), List.of(), SafetyProfile.Declaradas.sinLimitaciones());
        return new PlanGenerationContext(1L, (short) 1, LUNES, LUNES.plusDays(6), perfil, PlanAdjustment.none(), null,
                c, SafetyProfile.none(), P, politica);
    }

    static PlanDraft pull(List<CandidateExercise> ejercicios) {
        var lista = ejercicios.stream().map(c -> new PlanDraft.DraftExercise(c.exerciseId(), PrescriptionType.SETS_REPS,
                (short) 3, (short) 10, null, null, (short) 90, null)).toList();
        return new PlanDraft(GenerationSource.RULE_ENGINE, "m", "p", null, "r",
                List.of(new PlanDraft.DraftWorkout(LUNES, WorkoutFocus.PULL, "s", 40, lista)));
    }

    static List<String> vef(PlanDraft d, PlanGenerationContext c) {
        try {
            VALIDADOR.validate(d, c, 30, ConservationReport.vacio());
            return List.of();
        } catch (InvalidPlanDraftException e) {
            return e.problems().stream().filter(p -> p.startsWith("VEF:")).toList();
        }
    }

    static final List<CandidateExercise> SIN_TRACCION = List.of(EMPUJE_ESPALDA, TRONCO_ESPALDA, CURL, HIPEREXTENSION);

    @Test
    void historicaNoAplicaVEF() {
        assertEquals(List.of(), vef(pull(SIN_TRACCION), ctx(PlanningPolicy.HISTORICA, SIN_TRACCION)));
        assertEquals(List.of(), vef(pull(SIN_TRACCION), ctx(PlanningPolicy.C2D, SIN_TRACCION)),
                "C2-D sin EF no cambia la interpretacion");
    }

    @Test
    void pullSinTraccionYCatalogoSinTraccionEsNoFactible() {
        var p = vef(pull(SIN_TRACCION), ctx(PlanningPolicy.EF, SIN_TRACCION));
        assertTrue(p.stream().anyMatch(x -> x.contains("ejercicio 775") && x.contains("HORIZONTAL_PUSH")), p.toString());
        assertTrue(p.stream().anyMatch(x -> x.contains("ejercicio 1261") && x.contains("ZONA_INCOHERENTE")), p.toString());
        assertFalse(p.stream().anyMatch(x -> x.contains("ejercicio 320")), "la flexion de codo es compatible con PULL");
        assertFalse(p.stream().anyMatch(x -> x.contains("ejercicio 1066")), "la extension de tronco es compatible");
        assertTrue(p.stream().anyMatch(x -> x.contains("NO_FACTIBLE por catalogo o equipamiento insuficiente")),
                p.toString());
    }

    @Test
    void pullSinTraccionConTraccionDisponibleLoDice() {
        var c = new java.util.ArrayList<>(SIN_TRACCION);
        c.add(REMO);
        var p = vef(pull(List.of(CURL, HIPEREXTENSION)), ctx(PlanningPolicy.EF, c));
        assertEquals(1, p.size(), p.toString());
        assertTrue(p.get(0).contains("hay 1 candidatos elegibles"), p.get(0));
    }

    @Test
    void pullConTraccionPasa() {
        var c = List.of(REMO, CURL, HIPEREXTENSION);
        assertEquals(List.of(), vef(pull(c), ctx(PlanningPolicy.EF, c)));
    }

    @Test
    void registroIncoherenteNoCuenta() {
        var c = List.of(REMO, MANCUERNA);
        var p = vef(pull(c), ctx(PlanningPolicy.EF, c));
        assertEquals(1, p.size(), p.toString());
        assertTrue(p.get(0).contains("DATOS_INCOHERENTES"), p.get(0));
    }

    @Test
    void c2dNoEximeVEF() {
        var problemas = List.of("V20: corto", "VEF: sin traccion");
        assertFalse(PlanDraftValidator.aplicaC2D(ctx(PlanningPolicy.C2D_EF, SIN_TRACCION), ConservationReport.vacio(),
                problemas), "C2-D solo actua si todo es V20: una sesion funcionalmente invalida no se publica");
    }

    @Test
    void reglaDeLaIACoincideConElValidador() {
        var texto = ElegibilidadFuncional.reglaParaIA();
        for (var p : ElegibilidadFuncional.FAMILIA_PULL) assertTrue(texto.contains(p), p);
        for (var p : ElegibilidadFuncional.FAMILIA_PUSH) assertTrue(texto.contains(p), p);
        for (var p : ElegibilidadFuncional.FAMILIA_PIERNA) assertTrue(texto.contains(p), p);
        assertFalse(ElegibilidadFuncional.compatible(WorkoutFocus.PULL, EMPUJE_ESPALDA));
        assertTrue(ElegibilidadFuncional.compatible(WorkoutFocus.FULL_BODY, EMPUJE_ESPALDA),
                "en FULL_BODY el empuje cuenta como empuje (su slot es UPPER_PUSH)");
        assertFalse(ElegibilidadFuncional.compatible(WorkoutFocus.FULL_BODY, TRONCO_ESPALDA));
    }

    @Test
    void versionesEFSonPropias() {
        for (var p : PlanningPolicy.CATALOGO.values())
            assertEquals(p.id().contains("-EF-"), p.elegibilidadFuncional(), p.id());
        assertTrue(PlanningPolicy.EF.candidata() && PlanningPolicy.C2D_EF.candidata()
                && PlanningPolicy.C2D_V4C_EF.candidata());
    }
}
