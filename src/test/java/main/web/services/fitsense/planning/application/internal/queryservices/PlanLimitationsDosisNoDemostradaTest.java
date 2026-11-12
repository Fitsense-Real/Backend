package main.web.services.fitsense.planning.application.internal.queryservices;

import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Auditoria v46 · E2-a: aviso cuando la dosis de espalda u hombro solo sale de ejercicios no demostrados")
class PlanLimitationsDosisNoDemostradaTest {

    static CandidateExercise c(long id, String nombre, String grupo, String patron) {
        return new CandidateExercise(id, nombre, grupo, "body weight", 1, PrescriptionType.SETS_REPS, "x", patron);
    }

    static final List<CandidateExercise> CATALOGO = List.of(
            c(775, "Extensión de espalda en banco", "back", "HORIZONTAL_PUSH"),
            c(973, "Gancho de izquierda (boxeo)", "shoulders", "HORIZONTAL_PUSH"),
            c(1261, "Tronco 1261", "back", "TRUNK_ROTATION"),
            c(10, "Remo invertido", "back", "HORIZONTAL_PULL"),
            c(11, "Pino contra la pared", "shoulders", "VERTICAL_PUSH"),
            c(12, "Movilidad de hombro", "shoulders", "MOBILITY"),
            c(13, "Sentadilla", "upper legs", "KNEE_DOMINANT"));

    @Test
    @DisplayName("espalda y hombro solo con 775/973: un aviso con ambos grupos, sin prometer ni afirmar trabajo")
    void ambos() {
        var l = PlanLimitationsService.dosisNoDemostrada(List.of(775L, 973L, 13L, 775L), CATALOGO);
        assertTrue(l.isPresent());
        assertEquals("DOSE", l.get().type());
        assertEquals("DOSIS_NO_DEMOSTRADA", l.get().code());
        var m = l.get().message();
        assertTrue(m.contains("espalda") && m.contains("hombros"), m);
        assertTrue(m.contains("Gancho de izquierda (boxeo)") && m.contains("Extensión de espalda en banco"), m);
        assertTrue(m.contains("no lo contamos como trabajo demostrado"), m);
        assertFalse(m.toLowerCase().contains("perder") || m.toLowerCase().contains("ganar"), "no promete resultados");
    }

    @Test
    @DisplayName("con un ejercicio demostrado del grupo, ese grupo no avisa; la movilidad no cuenta como dosis")
    void mezcla() {
        assertTrue(PlanLimitationsService.dosisNoDemostrada(List.of(775L, 10L, 973L, 11L), CATALOGO).isEmpty());
        var l = PlanLimitationsService.dosisNoDemostrada(List.of(775L, 10L, 973L, 12L), CATALOGO);
        assertTrue(l.isPresent());
        assertTrue(l.get().message().contains("hombros") && !l.get().message().contains("espalda y"), l.get().message());
        assertTrue(PlanLimitationsService.dosisNoDemostrada(List.of(1261L), CATALOGO).isPresent(), "1261 tambien");
    }

    @Test
    @DisplayName("sin ejercicios del grupo, sin plan o sin catalogo: no se inventa un aviso")
    void nada() {
        assertTrue(PlanLimitationsService.dosisNoDemostrada(List.of(13L, 12L), CATALOGO).isEmpty());
        assertTrue(PlanLimitationsService.dosisNoDemostrada(List.of(), CATALOGO).isEmpty());
        assertTrue(PlanLimitationsService.dosisNoDemostrada(null, CATALOGO).isEmpty());
        assertTrue(PlanLimitationsService.dosisNoDemostrada(List.of(775L), null).isEmpty());
        assertTrue(PlanLimitationsService.dosisNoDemostrada(List.of(999L), CATALOGO).isEmpty(), "fuera del catalogo");
    }
}
