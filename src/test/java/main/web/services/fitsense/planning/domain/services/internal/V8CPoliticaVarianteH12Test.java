package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.ConservationReport;
import main.web.services.fitsense.planning.domain.model.valueobjects.ExclusionesDeSeguridad;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanDraft;
import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.planning.domain.services.ConservationValidator.PoliticaVariante;
import main.web.services.fitsense.planning.domain.services.ConservationValidator.ReglaPatron;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static main.web.services.fitsense.planning.domain.services.V8CConservacionPatronH1Test.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * H12 (PROTOTIPOS, apagados por defecto): politica de VARIANTE en V8C. H12-A (reduccion demostrable de dificultad,
 * por emparejamiento) y H12-C (maximo de sustituciones por sesion). Datos reales del catalogo V44_2.
 */
@DisplayName("H12 · V8C: politica de VARIANTE (H12-A reduccion demostrable, H12-C limite) - prototipos")
class V8CPoliticaVarianteH12Test {

    /** Catalogo de H1 mas un ejercicio de dificultad 3 y su variante de dificultad 2 (KNEE_DOMINANT). */
    static final Map<Long, CandidateExercise> CAT3 = new LinkedHashMap<>(CAT);

    static {
        CAT3.put(1000L, new CandidateExercise(1000L, "e1000", "upper legs", "barbell", 3, PrescriptionType.SETS_REPS,
                "quads", "KNEE_DOMINANT"));
        CAT3.put(1001L, new CandidateExercise(1001L, "e1001", "upper legs", "dumbbell", 2, PrescriptionType.SETS_REPS,
                "quads", "KNEE_DOMINANT"));
    }

    static ConservationReport var(ConservationValidator v, List<Long> antes, List<Long> despues) {
        return v.validate(derivacion(despues.stream().map(id -> ej(id, 3, 10)).toList(), List.of(ej(9001L, 3, 10))),
                semana("TOO_DIFFICULT", antes), orden(TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA), CAT3,
                ExclusionesDeSeguridad.ninguna(), List.of(), false, 30);
    }

    static final ConservationValidator HIST = new ConservationValidator();
    static final ConservationValidator A = new ConservationValidator(ReglaPatron.MULTICONJUNTO,
            PoliticaVariante.REDUCCION_DEMOSTRABLE, 0);
    static final ConservationValidator C2 = new ConservationValidator(ReglaPatron.MULTICONJUNTO,
            PoliticaVariante.HISTORICA, 2);

    @Test
    @DisplayName("0. Por defecto: HISTORICA y sin limite; valores invalidos rechazados")
    void porDefecto() {
        assertEquals(PoliticaVariante.HISTORICA, HIST.politicaVariante());
        assertEquals(0, HIST.maxSustituciones());
        assertEquals(PoliticaVariante.REDUCCION_DEMOSTRABLE, PoliticaVariante.desde(" reduccion-demostrable "));
        assertThrows(IllegalArgumentException.class, () -> PoliticaVariante.desde("lateral"));
        assertThrows(IllegalArgumentException.class,
                () -> new ConservationValidator(ReglaPatron.CONJUNTO, PoliticaVariante.HISTORICA, -1));
    }

    @Test
    @DisplayName("1. Dificultad 3 -> 2 del mismo patron: aceptada con HISTORICA y con H12-A")
    void tresADos() {
        assertTrue(var(HIST, List.of(1000L, 9001L), List.of(1001L, 9001L)).aceptable());
        assertTrue(var(A, List.of(1000L, 9001L), List.of(1001L, 9001L)).aceptable());
    }

    @Test
    @DisplayName("2. Dificultad 2 -> 1 (380 -> 72): aceptada con las dos")
    void dosAUno() {
        assertTrue(var(HIST, List.of(380L, 9001L), List.of(72L, 9001L)).aceptable());
        assertTrue(var(A, List.of(380L, 9001L), List.of(72L, 9001L)).aceptable());
    }

    @Test
    @DisplayName("3. Lateral (dificultad 1 -> 1, 580 -> 73): HISTORICA acepta; H12-A rechaza con su mensaje")
    void lateral() {
        assertTrue(var(HIST, List.of(580L, 9001L), List.of(73L, 9001L)).aceptable());
        var r = var(A, List.of(580L, 9001L), List.of(73L, 9001L));
        assertFalse(r.aceptable());
        assertTrue(r.problems().get(0).contains("no reducen la dificultad del catalogo"), r.problems()::toString);
        assertTrue(r.aplicadas().isEmpty());
    }

    @Test
    @DisplayName("4. Lateral 2 -> 2 (380 -> 56): H12-A rechaza")
    void lateralDos() {
        assertFalse(var(A, List.of(380L, 9001L), List.of(56L, 9001L)).aceptable());
    }

    @Test
    @DisplayName("5. Emparejamiento: retirados 380 (dif 2) y 580 (dif 1), anadidos 72 y 73 (dif 1): solo uno baja -> rechazada")
    void emparejamientoNecesario() {
        // El multiconjunto lo acepta (2 HPUSH por 2 HPUSH): hace falta el emparejamiento.
        assertTrue(var(new ConservationValidator(ReglaPatron.MULTICONJUNTO), List.of(380L, 580L, 9001L),
                List.of(72L, 73L, 9001L)).aceptable());
        var r = var(A, List.of(380L, 580L, 9001L), List.of(72L, 73L, 9001L));
        assertFalse(r.aceptable());
        assertTrue(r.problems().get(0).contains("73") || r.problems().get(0).contains("72"), r.problems()::toString);
    }

    @Test
    @DisplayName("6. Emparejamiento con reasignacion: 380 y 389 (dif 2) -> 72 (HPUSH 1) y 74 (VPULL 1): aceptada")
    void emparejamientoCompleto() {
        assertTrue(var(A, List.of(380L, 389L, 9001L), List.of(72L, 74L, 9001L)).aceptable());
    }

    @Test
    @DisplayName("7. H12-A no toca la reduccion de dosis sin sustituir, ni retirar, ni FATIGA, ni el dolor")
    void noToca() {
        var dosis = A.validate(derivacion(List.of(ej(580L, 3, 8), ej(9001L, 3, 10)), List.of(ej(9001L, 3, 10))),
                semana("TOO_DIFFICULT", List.of(580L, 9001L)), orden(TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA),
                CAT3, ExclusionesDeSeguridad.ninguna(), List.of(), false, 30);
        assertTrue(dosis.aceptable(), dosis.problems()::toString);
        assertTrue(var(A, List.of(580L, 389L, 9001L), List.of(580L, 9001L)).aceptable(), "retirar");
        var dolor = A.validate(derivacion(List.of(ej(360L, 3, 10), ej(1236L, 3, 10)), List.of(ej(9001L, 3, 10))),
                semana("PAIN_OR_DISCOMFORT", List.of(380L, 389L)), orden(TipoIntervencion.SEGURIDAD_ANTE_DOLOR), CAT3,
                ExclusionesDeSeguridad.ninguna(), List.of(), false, 30);
        assertTrue(dolor.aceptable(), dolor.problems()::toString);
        var fatiga = A.validate(derivacion(List.of(ej(580L, 2, 10), ej(9001L, 3, 10)), List.of(ej(9001L, 3, 10))),
                semana("FATIGUE", List.of(580L, 9001L)), orden(TipoIntervencion.REVISAR_CARGA_Y_RECUPERACION), CAT3,
                ExclusionesDeSeguridad.ninguna(), List.of(), false, 30);
        assertTrue(fatiga.aceptable(), fatiga.problems()::toString);
    }

    @Test
    @DisplayName("8. H12-C (max 2): 2 sustituciones aceptadas; 3 rechazadas; el dolor no se limita")
    void limite() {
        assertTrue(var(C2, List.of(380L, 389L, 9001L), List.of(72L, 74L, 9001L)).aceptable());
        var r = var(C2, List.of(380L, 389L, 580L, 9001L), List.of(56L, 74L, 73L, 9001L));
        assertFalse(r.aceptable());
        assertTrue(r.problems().get(0).contains("como maximo 2 por sesion"), r.problems()::toString);
        assertTrue(var(HIST, List.of(380L, 389L, 580L, 9001L), List.of(56L, 74L, 73L, 9001L)).aceptable());
        var dolor = C2.validate(derivacion(List.of(ej(360L, 3, 10), ej(1236L, 3, 10), ej(779L, 3, 10)),
                        List.of(ej(9001L, 3, 10))), semana("PAIN_OR_DISCOMFORT", List.of(380L, 389L, 580L)),
                orden(TipoIntervencion.SEGURIDAD_ANTE_DOLOR), CAT3, ExclusionesDeSeguridad.ninguna(), List.of(), false, 30);
        assertTrue(dolor.aceptable(), dolor.problems()::toString);
    }

    @Test
    @DisplayName("9. H12-A con cambio de musculo dentro de una reduccion de dificultad: aceptada (H2 sigue abierto)")
    void musculo() {
        // 389 (lats, dif 2) -> 862 (abs, dif 1, VERTICAL_PULL por catalogo): baja la dificultad y cambia el musculo.
        assertTrue(var(A, List.of(389L, 9001L), List.of(862L, 9001L)).aceptable());
    }
}
