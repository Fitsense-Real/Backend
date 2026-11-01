package main.web.services.fitsense.planning.domain.services.experimental;

import main.web.services.fitsense.planning.domain.model.valueobjects.*;
import main.web.services.fitsense.planning.domain.services.SessionDurationEstimator;
import main.web.services.fitsense.configuration.domain.model.valueobjects.PrescriptionParams;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Politicas candidatas (C2, V4 versionado, versionado de politica, metrica aerobica): desactivadas y exactas")
class PoliticasCandidatasTest {

    // ------------------------------------------------------------------------------------------------ V4

    /**
     * Bordes de V4 en segundos estimados. Limite exacto L = M x 69 s (M x 60 x 1,15): 30 -> 2.070, 45 -> 3.105,
     * 60 -> 4.140, 75 -> 5.175. Columnas: minutos, segundos, HISTORICO, TRUNCADO, SEGUNDOS.
     */
    @ParameterizedTest(name = "{0} min, {1} s -> historico {2}, truncado {3}, segundos {4}")
    @CsvSource({
            "30,2069,true,true,true", "30,2070,true,false,true", "30,2071,true,false,false",
            "30,2129,true,false,false", "30,2130,false,false,false",
            "45,3089,true,true,true", "45,3090,true,false,true", "45,3104,true,false,true", "45,3105,true,false,true",
            "45,3106,true,false,false", "45,3149,true,false,false", "45,3150,false,false,false",
            "60,4139,true,true,true", "60,4140,true,true,true", "60,4141,true,true,false", "60,4169,true,true,false",
            "60,4170,false,false,false",
            "75,5174,true,true,true", "75,5175,true,true,true", "75,5176,true,true,false", "75,5189,true,true,false",
            "75,5190,false,false,false"})
    void bordesV4(int minutos, long segundos, boolean historico, boolean truncado, boolean estricto) {
        assertEquals(historico, TechoV4Versionado.HISTORICO.admite(segundos, minutos), "historico");
        assertEquals(truncado, TechoV4Versionado.TRUNCADO.admite(segundos, minutos), "truncado");
        assertEquals(estricto, TechoV4Versionado.SEGUNDOS.admite(segundos, minutos), "segundos");
        if (estricto) assertTrue(segundos <= TechoV4Versionado.limiteSegundosExacto(minutos));
    }

    @ParameterizedTest(name = "{0} min: maximos historico {1}, truncado {2}, segundos {3}")
    @CsvSource({"30,2129,2069,2070", "45,3149,3089,3105", "60,4169,4169,4140", "75,5189,5189,5175"})
    void maximosAdmitidos(int minutos, long historico, long truncado, long estricto) {
        assertEquals(historico, TechoV4Versionado.HISTORICO.maximoAdmitido(minutos));
        assertEquals(truncado, TechoV4Versionado.TRUNCADO.maximoAdmitido(minutos));
        assertEquals(estricto, TechoV4Versionado.SEGUNDOS.maximoAdmitido(minutos));
    }

    @Test
    @DisplayName("V4: el historico es el del validador (round) y el truncado el del perfil (floor)")
    void v4CoincideConProduccion() {
        for (int m = 30; m <= 75; m++) {
            assertEquals(Math.round(m * 1.15), (long) TechoV4Versionado.HISTORICO.techoMinutos(m));
            assertEquals((long) Math.floor(m * 1.15), (long) TechoV4Versionado.TRUNCADO.techoMinutos(m));
        }
        // Mismo redondeo del estimador para los minutos declarados.
        var est = new SessionDurationEstimator();
        var p = new PrescriptionParams(70, 3, 60, 5, 60, 20, null, null, 10, 180);
        var w = new PlanDraft.DraftWorkout(LocalDate.of(2026, 11, 2), WorkoutFocus.FULL_BODY, "x", 0,
                List.of(new PlanDraft.DraftExercise(1L, PrescriptionType.SETS_REPS, (short) 3, (short) 10, null, null,
                        (short) 90, null)));
        assertEquals(est.estimateMinutes(w, p), TechoV4Versionado.minutosDeclarados(est.estimateSeconds(w, p)));
    }

    // ------------------------------------------------------------------------------------------------ C2

    @Test
    @DisplayName("C2: desactivada, secuencias exactas por variante y condicion estricta 'solo V20'")
    void c2() {
        for (var c : List.of(PisoV20Acotado.c2a(), PisoV20Acotado.c2b(), PisoV20Acotado.c2c(), PisoV20Acotado.c2d())) {
            assertFalse(c.habilitado());
            assertTrue(c.version().contains("SIN-APROBAR"));
            assertFalse(c.puedeActuar(List.of("V20: x"), false, false), "deshabilitada fuera del laboratorio");
        }
        assertEquals(List.of(65, 60, 55, 50), PisoV20Acotado.c2a().secuencia("LOSE_WEIGHT"));
        assertEquals(List.of(65, 60, 55), PisoV20Acotado.c2b().secuencia("INCREASE_STRENGTH"));
        assertEquals(List.of(65, 60, 55), PisoV20Acotado.c2c().secuencia("GAIN_MUSCLE"));
        assertEquals(List.of(65), PisoV20Acotado.c2c().secuencia("INCREASE_STRENGTH"));
        assertEquals(List.of(65), PisoV20Acotado.c2c().secuencia("LOSE_WEIGHT"));
        assertEquals(List.of(65), PisoV20Acotado.c2c().secuencia("GENERAL_FITNESS"));
        assertEquals(List.of(), PisoV20Acotado.c2d().secuencia("GAIN_MUSCLE"));

        var c = PisoV20Acotado.c2c();
        assertTrue(c.puedeActuar(List.of("V20: a", "V20: b"), false, true));
        assertFalse(c.puedeActuar(List.of(), false, true), "sin problemas no hay nada que acotar");
        for (var otro : List.of("V4: x", "V8C: x", "V15: x", "V18: x", "V23: x", "V24: x", "V25: x", "P-02 x", "sin codigo"))
            assertFalse(c.puedeActuar(List.of("V20: a", otro), false, true), "no se salta " + otro);
        assertFalse(c.puedeActuar(List.of("V20: a"), true, true), "con orden de volumen activa no actua");
    }

    // ------------------------------------------------------------------------------------------------ versionado

    @Test
    @DisplayName("versionado: solo la historica esta activa; un plan sin version se lee como historico")
    void versionado() {
        assertTrue(VersionPoliticaPlanificacion.HISTORICA.activable());
        for (var v : List.of(VersionPoliticaPlanificacion.C2_CANDIDATA, VersionPoliticaPlanificacion.V4_CORREGIDA,
                VersionPoliticaPlanificacion.MODELO_E))
            assertFalse(v.activable(), v.id());
        assertSame(VersionPoliticaPlanificacion.HISTORICA, VersionPoliticaPlanificacion.deUnPlan(null));
        assertSame(VersionPoliticaPlanificacion.HISTORICA, VersionPoliticaPlanificacion.deUnPlan(" "));
        assertSame(VersionPoliticaPlanificacion.C2_CANDIDATA, VersionPoliticaPlanificacion.deUnPlan("PLAN-C2-0.1"));
        assertThrows(IllegalArgumentException.class, () -> VersionPoliticaPlanificacion.deUnPlan("PLAN-X"));
        assertEquals("HISTORICO", VersionPoliticaPlanificacion.HISTORICA.v4());
        var t = VersionPoliticaPlanificacion.C2_CANDIDATA.traza(55, 86, "MVP-1.x", "laboratorio");
        assertEquals(55, t.get("piso_v20_usado_pct"));
        assertEquals("PLAN-C2-0.1", t.get("politica"));
    }

    // ------------------------------------------------------------------------------------------------ aerobica

    @Test
    @DisplayName("metrica aerobica: programado != ejecutado; sin intensidad no hay equivalentes; aviso sin juicio")
    void aerobica() {
        var cardio = new CandidateExercise(7L, "bici", "cardio", "stationary bike", 1, PrescriptionType.DURATION, null,
                "LOCOMOTION_CARDIO");
        var w = new PlanDraft.DraftWorkout(LocalDate.of(2026, 11, 2), WorkoutFocus.FULL_BODY, "x", 30,
                List.of(new PlanDraft.DraftExercise(7L, PrescriptionType.DURATION, (short) 3, null, 120, null,
                        (short) 60, null)));
        var d = new PlanDraft(GenerationSource.RULE_ENGINE, null, "p", null, null, List.of(w, w));
        var m = MetricaAerobica.programada(d, Map.of(7L, cardio));
        assertEquals(12.0, m.minutosTrabajoProgramados(), 1e-9, "2 sesiones x 3 x 120 s");
        assertEquals(16.0, m.minutosBloqueProgramados(), 1e-9, "mas 2 pausas de 60 s por sesion");
        assertEquals(MetricaAerobica.Intensidad.DESCONOCIDA, m.intensidad());
        assertNull(m.minutosEquivalentes());
        assertNull(m.conRegistro(10.0, 30.0, MetricaAerobica.Intensidad.DESCONOCIDA).minutosEquivalentes(),
                "con intensidad desconocida no se suponen equivalentes");
        assertEquals(80.0, m.conRegistro(10.0, 30.0, MetricaAerobica.Intensidad.VIGOROSA).minutosEquivalentes(), 1e-9);
        assertEquals(40.0, m.conRegistro(10.0, 30.0, MetricaAerobica.Intensidad.MODERADA).minutosEquivalentes(), 1e-9);
        String aviso = m.aviso().toLowerCase();
        assertTrue(aviso.contains("fuera de fitsense"));
        for (var prohibido : List.of("incumpl", "no cumples", "perder", "perderas", "garantiza"))
            assertFalse(aviso.contains(prohibido), prohibido);
    }
}
