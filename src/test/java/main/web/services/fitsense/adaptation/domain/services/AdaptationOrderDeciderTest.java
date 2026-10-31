package main.web.services.fitsense.adaptation.domain.services;

import main.web.services.fitsense.adaptation.domain.model.valueobjects.AdaptationOutcome;
import main.web.services.fitsense.adaptation.domain.model.valueobjects.AdjustmentContext;
import main.web.services.fitsense.analytics.interfaces.acl.SessionOutcomeView;
import main.web.services.fitsense.configuration.domain.model.valueobjects.CalculationParams;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * La decision de adaptacion localizada y ampliada.
 * <p>
 * Fija cuatro cosas que la logica anterior no cumplia: que la falta de registro
 * no se trate como adherencia baja, que no haya progresion automatica, que la
 * orden no exija mover el volumen, y que el motivo limite lo que la IA puede
 * cambiar dentro de una sesion senalada.
 */
class AdaptationOrderDeciderTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 10, 12);
    private static final double COBERTURA_MINIMA = 50.0;   // PROVISIONAL

    private final AdaptationOrderDecider decider = new AdaptationOrderDecider();

    // -----------------------------------------------------------------------
    // 1. Evidencia
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Cobertura por debajo del minimo: INSUFFICIENT_DATA y se conserva")
    void coberturaInsuficiente() {
        var orden = decidir(contexto(cifra("100.00"), cifra("33.33"), 1, 3, List.of()));

        assertEquals(AdaptationOutcome.INSUFFICIENT_DATA, orden.outcome());
        assertTrue(orden.conserva());
        assertFalse(orden.adapta());
        assertTrue(orden.sessions().isEmpty());
    }

    @Test
    @DisplayName("Sin un solo registro no hay evidencia, aunque el minimo sea 0 %")
    void sinRegistrosNoHayEvidencia() {
        var orden = decider.decide(
                contexto(null, cifra("0.00"), 0, 3, List.of()), params(), 0.0);

        assertEquals(AdaptationOutcome.INSUFFICIENT_DATA, orden.outcome());
    }

    @Test
    @DisplayName("Tres omitidas declaradas NO son falta de datos: son adherencia 0 real")
    void omitidasDeclaradasSiSonEvidencia() {
        var orden = decidir(contexto(cifra("0.00"), cifra("100.00"), 3, 3, List.of(
                sesion(1, 0, "LACK_OF_TIME", true),
                sesion(2, 2, "FATIGUE", true),
                sesion(3, 4, "LACK_OF_TIME", true))));

        assertEquals(AdaptationOutcome.ADAPT_EXPANDED, orden.outcome());
        assertEquals(3, orden.sessions().size());
    }

    // -----------------------------------------------------------------------
    // 2. Adherencia adecuada: conservar, sin progresion
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Adherencia observada adecuada: NO_CHANGE y NINGUNA progresion")
    void adherenciaAdecuadaConserva() {
        var orden = decidir(contexto(cifra("100.00"), cifra("66.67"), 2, 3, List.of()));

        assertEquals(AdaptationOutcome.NO_CHANGE, orden.outcome());
        assertTrue(orden.conserva());
        assertFalse(orden.adapta(), "no se llama a la IA cuando la adherencia es adecuada");
        assertTrue(orden.sessions().isEmpty());
    }

    @Test
    @DisplayName("El caso que justifica usar lo observado y no la ponderada")
    void observadaContraPonderada() {
        // Dos sesiones al 100 % y una sin registro. La ponderada da 66,67 %, que
        // cae en el tramo de baja adherencia; lo observado da 100 %.
        var contexto = new AdjustmentContext(
                cifra("66.67"), null, 480, 480, 3, 45, 2, null, cifra("6.5"),
                List.of(), 2, 3, cifra("100.00"), cifra("66.67"));

        assertEquals(AdaptationOutcome.NO_CHANGE, decidir(contexto).outcome(),
                "cumplio el 100 % de lo que registro: no hay nada que adaptar");
    }

    // -----------------------------------------------------------------------
    // 3. Alcance
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Adherencia moderada: localizada, UNA sesion, la peor")
    void localizadaTocaUnaSola() {
        var orden = decidir(contexto(cifra("65.00"), cifra("100.00"), 3, 3, List.of(
                sesion(1, 0, null, false),
                sesion(2, 2, "LACK_OF_TIME", true),
                sesion(3, 4, null, false))));

        assertEquals(AdaptationOutcome.ADAPT_LOCALIZED, orden.outcome());
        assertEquals(1, orden.sessions().size());
        assertEquals(2L, orden.sessions().get(0).plannedWorkoutId());
        assertTrue(orden.adapta());
    }

    @Test
    @DisplayName("Localizada elige la de peor cumplimiento entre varias senaladas")
    void localizadaEligeLaPeor() {
        var orden = decidir(contexto(cifra("55.00"), cifra("100.00"), 3, 3, List.of(
                sesionCon(1, 0, 70, "LACK_OF_TIME"),
                sesionCon(2, 2, 20, "TOO_DIFFICULT"),
                sesionCon(3, 4, 60, "LACK_OF_TIME"))));

        assertEquals(AdaptationOutcome.ADAPT_LOCALIZED, orden.outcome());
        assertEquals(2L, orden.sessions().get(0).plannedWorkoutId(), "la del 20 %");
    }

    @Test
    @DisplayName("Adherencia severa: ampliada, todas las senaladas")
    void ampliadaTocaTodas() {
        var orden = decidir(contexto(cifra("35.00"), cifra("100.00"), 3, 3, List.of(
                sesionCon(1, 0, 40, "LACK_OF_TIME"),
                sesionCon(2, 2, 0, "FATIGUE"),
                sesionCon(3, 4, 90, null))));

        assertEquals(AdaptationOutcome.ADAPT_EXPANDED, orden.outcome());
        assertEquals(2, orden.sessions().size(), "las dos senaladas, no la del 90 %");
        assertFalse(orden.esSesionSenalada(3L));
    }

    @Test
    @DisplayName("Adherencia baja sin ninguna sesion senalada: no se adapta a ciegas")
    void bajaSinSenaladasNoAdapta() {
        var orden = decidir(contexto(cifra("60.00"), cifra("100.00"), 3, 3, List.of(
                sesion(1, 0, null, false), sesion(2, 2, null, false))));

        assertEquals(AdaptationOutcome.INSUFFICIENT_DATA, orden.outcome());
        assertTrue(orden.conserva());
    }

    // -----------------------------------------------------------------------
    // 4. El motivo limita lo que la IA puede cambiar
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Problema de horario: se reprograma el dia, NO se sustituyen ejercicios")
    void horarioNoPermiteSustituir() {
        var orden = decidir(contexto(cifra("60.00"), cifra("100.00"), 3, 3,
                List.of(sesionCon(2, 2, 0, "SCHEDULE_CHANGE"))));

        assertEquals(TipoIntervencion.REPROGRAMAR_DIAS, orden.sessions().get(0).intervencion());
        assertFalse(orden.permiteSustituirEjercicioEn(2L),
                "si el problema era el dia, cambiar ejercicios no esta permitido");
    }

    @Test
    @DisplayName("Falta de tiempo: acortar o reorganizar, sin sustituir")
    void faltaDeTiempoNoPermiteSustituir() {
        var orden = decidir(contexto(cifra("60.00"), cifra("100.00"), 3, 3,
                List.of(sesionCon(2, 2, 0, "LACK_OF_TIME"))));

        assertEquals(TipoIntervencion.ACORTAR_O_REORGANIZAR, orden.sessions().get(0).intervencion());
        assertFalse(orden.permiteSustituirEjercicioEn(2L));
    }

    @Test
    @DisplayName("Demasiado dificil: si permite variante del mismo patron")
    void demasiadoDificilPermiteSustituir() {
        var orden = decidir(contexto(cifra("60.00"), cifra("100.00"), 3, 3,
                List.of(sesionCon(2, 2, 30, "TOO_DIFFICULT"))));

        assertEquals(TipoIntervencion.VARIANTE_MAS_SENCILLA_O_CARGA,
                orden.sessions().get(0).intervencion());
        assertTrue(orden.permiteSustituirEjercicioEn(2L));
        assertFalse(orden.sessions().get(0).intervencion().exigeRevisionDeSeguridad());
    }

    @Test
    @DisplayName("Dolor: permite sustituir pero exige revisar la restriccion declarada")
    void dolorExigeRevisionDeSeguridad() {
        var orden = decidir(contexto(cifra("60.00"), cifra("100.00"), 3, 3,
                List.of(sesionCon(2, 2, 20, "PAIN_OR_DISCOMFORT"))));

        var tipo = orden.sessions().get(0).intervencion();
        assertEquals(TipoIntervencion.SEGURIDAD_ANTE_DOLOR, tipo);
        assertTrue(tipo.permiteSustituirEjercicio());
        assertTrue(tipo.exigeRevisionDeSeguridad(),
                "no se reemplaza automaticamente sin evaluar la restriccion");
    }

    @Test
    @DisplayName("Fatiga y falta de motivacion: no sustituyen de entrada")
    void fatigaYMotivacion() {
        assertFalse(decidir(contexto(cifra("60.00"), cifra("100.00"), 3, 3,
                List.of(sesionCon(2, 2, 10, "FATIGUE")))).permiteSustituirEjercicioEn(2L));
        assertFalse(decidir(contexto(cifra("60.00"), cifra("100.00"), 3, 3,
                List.of(sesionCon(2, 2, 10, "LACK_OF_MOTIVATION")))).permiteSustituirEjercicioEn(2L));
    }

    @Test
    @DisplayName("Sin motivo registrado: no se inventa una sustitucion")
    void sinMotivoNoSeInventa() {
        var orden = decidir(contexto(cifra("60.00"), cifra("100.00"), 3, 3,
                List.of(sesionCon(2, 2, 0, null))));

        assertEquals(TipoIntervencion.CAUSA_NO_REGISTRADA, orden.sessions().get(0).intervencion());
        assertFalse(orden.permiteSustituirEjercicioEn(2L));
    }

    @Test
    @DisplayName("Una sesion NO senalada no admite ningun cambio")
    void laNoSenaladaNoAdmiteCambios() {
        var orden = decidir(contexto(cifra("60.00"), cifra("100.00"), 3, 3, List.of(
                sesionCon(1, 0, 100, null), sesionCon(2, 2, 30, "TOO_DIFFICULT"))));

        assertTrue(orden.esSesionSenalada(2L));
        assertFalse(orden.esSesionSenalada(1L));
        assertFalse(orden.permiteSustituirEjercicioEn(1L));
        assertFalse(orden.permiteSustituirEjercicioEn(999L), "ni una que no existe");
    }

    @Test
    @DisplayName("Seguridad aparte: adherencia buena con un ejercicio dejado por dolor -> solo esa sesion, SEGURIDAD_ANTE_DOLOR")
    void dolorConAdherenciaBuena() {
        var orden = decidir(contexto(cifra("93.33"), cifra("100.00"), 3, 3, List.of(
                sesionCon(1, 0, 100, null), sesionCon(2, 2, 80, "PAIN_OR_DISCOMFORT"), sesionCon(3, 4, 100, null))));
        assertEquals(AdaptationOutcome.ADAPT_LOCALIZED, orden.outcome());
        assertEquals(1, orden.sessions().size());
        assertEquals(2L, orden.sessions().get(0).plannedWorkoutId());
        assertEquals(main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion.SEGURIDAD_ANTE_DOLOR,
                orden.sessions().get(0).intervencion());
    }

    @Test
    @DisplayName("Localizada con dolor en otra sesion: se corrige primero la del dolor, no la de peor cumplimiento")
    void dolorAntesQueLaPeor() {
        var orden = decidir(contexto(cifra("60.00"), cifra("100.00"), 3, 3, List.of(
                sesionCon(1, 0, 100, null), sesionCon(2, 2, 80, "PAIN_OR_DISCOMFORT"), sesion(3, 4, "LACK_OF_TIME", true))));
        assertEquals(AdaptationOutcome.ADAPT_LOCALIZED, orden.outcome());
        assertEquals(List.of(2L), orden.sessions().stream().map(s -> s.plannedWorkoutId()).toList());
    }

    @Test
    @DisplayName("Sin dolor, adherencia buena: NO_CHANGE (una observacion de fatiga no dispara nada)")
    void fatigaConAdherenciaBuena() {
        var orden = decidir(contexto(cifra("93.33"), cifra("100.00"), 3, 3, List.of(
                sesionCon(1, 0, 100, null), sesionCon(2, 2, 80, "FATIGUE"), sesionCon(3, 4, 100, null))));
        assertEquals(AdaptationOutcome.NO_CHANGE, orden.outcome());
    }

    // -----------------------------------------------------------------------

    private main.web.services.fitsense.adaptation.domain.model.valueobjects.AdaptationOrder
    decidir(AdjustmentContext contexto) {
        return decider.decide(contexto, params(), COBERTURA_MINIMA);
    }

    private static AdjustmentContext contexto(BigDecimal observada, BigDecimal cobertura,
                                              int registradas, int previstas,
                                              List<SessionOutcomeView> sesiones) {
        return new AdjustmentContext(
                observada, null, 480, 480, 3, 45, 2, null, cifra("6.5"),
                sesiones, registradas, previstas, observada, cobertura);
    }

    /** Senalada o no, con cumplimiento 0 si lo esta. */
    private static SessionOutcomeView sesion(long id, int dia, String motivo, boolean senalada) {
        return sesionCon(id, dia, senalada ? 0 : 100, motivo);
    }

    private static SessionOutcomeView sesionCon(long id, int dia, int cumplimiento, String motivo) {
        return new SessionOutcomeView(id, LUNES.plusDays(dia),
                cumplimiento == 0 ? "SKIPPED" : "PARTIAL", true,
                BigDecimal.valueOf(cumplimiento), motivo, (short) 6, cumplimiento < 80);
    }

    private static BigDecimal cifra(String v) {
        return new BigDecimal(v);
    }

    private static CalculationParams params() {
        var adherence = new CalculationParams.Adherence(80.0, 30.0, 80.0, 100.0,
                "weighted_adherence_pct", "LAST_FINISHED", 7);
        var adjustment = new CalculationParams.Adjustment(80.0, 50.0, 20.0, 30.0, 10.0,
                5.0, 5.0, 15.0, 20.0, 40.0, 5.0, 1, 2, 20, 2, 2, 6, 20, 1, 1, 20.0, 8.0);
        return new CalculationParams(adherence, adjustment, null,
                new CalculationParams.Dropout(14), null);
    }
}
