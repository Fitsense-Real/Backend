package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.model.valueobjects.CandidateExercise;
import main.web.services.fitsense.planning.domain.model.valueobjects.ConservationReport;
import main.web.services.fitsense.planning.domain.model.valueobjects.ExclusionesDeSeguridad;
import main.web.services.fitsense.planning.domain.model.valueobjects.PrescriptionType;
import main.web.services.fitsense.planning.domain.services.ConservationValidator.ReglaPatron;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static main.web.services.fitsense.planning.domain.services.V8CConservacionPatronH1Test.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * LIMITE DE DIFICULTAD (candidato, {@code fitsense.adaptation.difficulty-guard}, APAGADO por defecto). Ante
 * TOO_DIFFICULT (VARIANTE) y dolor (SEGURIDAD y SUST-SEG-1), un sustituto no puede tener dificultad registrada mayor
 * que la del ejercicio que reemplaza. Datos del catalogo V44_2 (ver V8CConservacionPatronH1Test).
 */
@DisplayName("Limite de dificultad (candidato, apagado por defecto): ningun sustituto mas dificil ante TOO_DIFFICULT o dolor")
class LimiteDificultadTest {

    static ConservationReport validar(boolean limite, TipoIntervencion t, String motivo, List<Long> antes,
                                      List<Long> despues) {
        return new ConservationValidator(ReglaPatron.MULTICONJUNTO, limite).validate(
                derivacion(despues.stream().map(id -> ej(id, 3, 10)).toList(), List.of(ej(9001L, 3, 10))),
                semana(motivo, antes), orden(t), CAT, ExclusionesDeSeguridad.ninguna(), List.of(), false, 30);
    }

    static ConservationReport variante(boolean limite, List<Long> antes, List<Long> despues) {
        return validar(limite, TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA, "TOO_DIFFICULT", antes, despues);
    }

    @Test
    @DisplayName("0. Por defecto el limite esta APAGADO; 'on' lo enciende; otro valor es un error")
    void configuracion() {
        assertFalse(new ConservationValidator().limitaDificultad());
        assertFalse(new ConservationValidator("multiconjunto", "off").limitaDificultad());
        assertFalse(new ConservationValidator("multiconjunto", "").limitaDificultad());
        assertTrue(new ConservationValidator("multiconjunto", " ON ").limitaDificultad());
        assertThrows(IllegalArgumentException.class, () -> new ConservationValidator("multiconjunto", "si"));
        assertEquals(ReglaPatron.MULTICONJUNTO, new ConservationValidator("multiconjunto", "on").reglaPatron());
        assertEquals(SafetySubstitutionPolicy.Estrategia.HISTORICA, new ConservationValidator().estrategiaSustitucion());
        assertEquals(SafetySubstitutionPolicy.Estrategia.LIMITE,
                new ConservationValidator("multiconjunto", "on").estrategiaSustitucion());
        var c = new ConservationValidator("multiconjunto", "preferencia");
        assertEquals(SafetySubstitutionPolicy.Estrategia.LIMITE_Y_PREFERENCIA, c.estrategiaSustitucion());
        assertTrue(c.limitaDificultad(), "C tambien limita en V8C");
    }

    @Test
    @DisplayName("9. Estrategia C: entre candidatos <= elige la dificultad mas cercana (igual antes que menor); B, por id")
    void estrategiaC() {
        var original = cand(70001L, 2);
        Map<Long, CandidateExercise> e = new LinkedHashMap<>();
        e.put(70002L, cand(70002L, 1));
        e.put(70003L, cand(70003L, 2));
        e.put(70004L, cand(70004L, 3));
        assertEquals(70002L, SafetySubstitutionPolicy.elegir(original, null, e, Set.of(), Set.of(), null,
                SafetySubstitutionPolicy.Estrategia.LIMITE).orElseThrow().exerciseId(), "B: id mas bajo de los <=");
        assertEquals(70003L, SafetySubstitutionPolicy.elegir(original, null, e, Set.of(), Set.of(), null,
                SafetySubstitutionPolicy.Estrategia.LIMITE_Y_PREFERENCIA).orElseThrow().exerciseId(), "C: igual");
        assertEquals(70002L, SafetySubstitutionPolicy.elegir(original, null, e, Set.of(), Set.of(), null,
                SafetySubstitutionPolicy.Estrategia.HISTORICA).orElseThrow().exerciseId(), "A: id, sin filtro");
    }

    @Test
    @DisplayName("10. Estrategia C no pasa la dificultad por delante de la zona: un candidato de otra zona y misma dificultad pierde")
    void preferenciaDetrasDeLaZona() {
        var original = cand(70001L, 2);
        Map<Long, CandidateExercise> e = new LinkedHashMap<>();
        e.put(70002L, new CandidateExercise(70002L, "e", "chest", "body weight", 1, PrescriptionType.SETS_REPS,
                "pectorals", "HORIZONTAL_PUSH"));
        e.put(70003L, new CandidateExercise(70003L, "e", "upper arms", "body weight", 2, PrescriptionType.SETS_REPS,
                "triceps", "HORIZONTAL_PUSH"));
        assertEquals(70002L, SafetySubstitutionPolicy.elegir(original, null, e, Set.of(), Set.of(), null,
                SafetySubstitutionPolicy.Estrategia.LIMITE_Y_PREFERENCIA).orElseThrow().exerciseId(),
                "misma zona (chest) antes que dificultad igual en otra zona");
    }

    @Test
    @DisplayName("11. Explicar un descarte: excluido en la sesion, mas dificil con limite, o el mismo")
    void descartes() {
        var original = cand(70001L, 1);
        assertEquals(SafetySubstitutionPolicy.Etapa.ES_EL_MISMO, SafetySubstitutionPolicy.descarte(original, original,
                null, Set.of(), Set.of(), null, true));
        assertEquals(SafetySubstitutionPolicy.Etapa.NO_EN_LA_SESION_NI_CONTIGUO, SafetySubstitutionPolicy.descarte(
                cand(70002L, 1), original, null, Set.of(70002L), Set.of(), null, true));
        assertEquals(SafetySubstitutionPolicy.Etapa.DIFICULTAD_NO_MAYOR, SafetySubstitutionPolicy.descarte(
                cand(70003L, 2), original, null, Set.of(), Set.of(), null, true));
        assertNull(SafetySubstitutionPolicy.descarte(cand(70003L, 2), original, null, Set.of(), Set.of(), null, false));
    }

    @Test
    @DisplayName("1. VARIANTE 580 (1) -> 56 (2): apagado se acepta (hoy); encendido se RECHAZA con el motivo")
    void varianteSube() {
        assertTrue(variante(false, List.of(580L, 360L), List.of(56L, 360L)).aceptable());
        var r = variante(true, List.of(580L, 360L), List.of(56L, 360L));
        assertFalse(r.aceptable());
        assertTrue(r.problems().stream().anyMatch(p -> p.contains("sustituye por el ejercicio 56, de dificultad"
                + " registrada 2, cuando el retirado que le corresponde (580) tiene 1")), r.problems()::toString);
    }

    @Test
    @DisplayName("2. VARIANTE que baja (380 (2) -> 72 (1)) o iguala (933 (1) -> 129 (1)): se acepta con el limite")
    void varianteBajaOIgual() {
        assertTrue(variante(true, List.of(380L, 360L), List.of(72L, 360L)).aceptable());
        assertTrue(variante(true, List.of(933L, 360L), List.of(129L, 360L)).aceptable());
    }

    @Test
    @DisplayName("3. Emparejamiento, no orden: retira 380 (2) y 933 (1), anade 72 (1) y 56 (2) -> 56<->380, 72<->933: se acepta")
    void emparejamiento() {
        var r = variante(true, List.of(380L, 933L, 360L), List.of(72L, 56L, 360L));
        assertTrue(r.aceptable(), r.problems()::toString);
    }

    @Test
    @DisplayName("4. Dolor (IA) 580 (1) -> 56 (2): apagado se acepta (hoy); encendido se rechaza")
    void dolorSube() {
        assertTrue(validar(false, TipoIntervencion.SEGURIDAD_ANTE_DOLOR, "PAIN_OR_DISCOMFORT", List.of(580L),
                List.of(56L)).aceptable());
        var r = validar(true, TipoIntervencion.SEGURIDAD_ANTE_DOLOR, "PAIN_OR_DISCOMFORT", List.of(580L), List.of(56L));
        assertFalse(r.aceptable());
        assertTrue(r.problems().stream().anyMatch(p -> p.contains("SEGURIDAD_ANTE_DOLOR")), r.problems()::toString);
    }

    @Test
    @DisplayName("5. Dolor (IA) que baja o iguala: se acepta con el limite; retirar sin anadir tambien")
    void dolorBajaOIgualORetira() {
        assertTrue(validar(true, TipoIntervencion.SEGURIDAD_ANTE_DOLOR, "PAIN_OR_DISCOMFORT", List.of(380L),
                List.of(72L)).aceptable());
        assertTrue(validar(true, TipoIntervencion.SEGURIDAD_ANTE_DOLOR, "PAIN_OR_DISCOMFORT", List.of(933L),
                List.of(129L)).aceptable());
    }

    @Test
    @DisplayName("6. Fuera de VARIANTE y dolor el limite no actua (no hay sustitucion permitida que limitar)")
    void otrasIntervenciones() {
        // ACORTAR solo retira: nada que comparar, mismo resultado con y sin limite.
        var sin = validar(false, TipoIntervencion.ACORTAR_O_REORGANIZAR, "LACK_OF_TIME", List.of(580L, 360L), List.of(360L));
        var con = validar(true, TipoIntervencion.ACORTAR_O_REORGANIZAR, "LACK_OF_TIME", List.of(580L, 360L), List.of(360L));
        assertEquals(sin.aceptable(), con.aceptable());
        assertEquals(sin.problems(), con.problems());
    }

    @Test
    @DisplayName("7. SUST-SEG-1: sin limite elige 70002 (dif 3) por id; con limite, 70003 (dif 1)")
    void sustSeg1Elige() {
        var original = cand(70001L, 1);
        Map<Long, CandidateExercise> e = new LinkedHashMap<>();
        e.put(70002L, cand(70002L, 3));
        e.put(70003L, cand(70003L, 1));
        assertEquals(70002L, SafetySubstitutionPolicy.elegir(original, null, e, Set.of(), Set.of(), null, false)
                .orElseThrow().exerciseId());
        assertEquals(70003L, SafetySubstitutionPolicy.elegir(original, null, e, Set.of(), Set.of(), null, true)
                .orElseThrow().exerciseId());
    }

    @Test
    @DisplayName("8. SUST-SEG-1 con limite y solo sustitutos mas dificiles: no hay sustituto (se informa, no se publica)")
    void sustSeg1SinSustituto() {
        var original = cand(70001L, 1);
        Map<Long, CandidateExercise> e = Map.of(70002L, cand(70002L, 2));
        assertTrue(SafetySubstitutionPolicy.elegir(original, null, e, Set.of(), Set.of(), null, false).isPresent());
        assertTrue(SafetySubstitutionPolicy.elegir(original, null, e, Set.of(), Set.of(), null, true).isEmpty());
    }

    static CandidateExercise cand(long id, int dificultad) {
        return new CandidateExercise(id, "e" + id, "chest", "body weight", dificultad, PrescriptionType.SETS_REPS,
                "pectorals", "HORIZONTAL_PUSH");
    }
}
