package main.web.services.fitsense.adaptation.interfaces.acl;

import main.web.services.fitsense.adaptation.domain.model.aggregates.UserIntervention;
import main.web.services.fitsense.adaptation.domain.model.commands.DecideWeeklyAdjustmentCommand;
import main.web.services.fitsense.adaptation.domain.model.valueobjects.AdaptationOrder;
import main.web.services.fitsense.adaptation.domain.model.valueobjects.AdaptationOutcome;
import main.web.services.fitsense.adaptation.domain.model.valueobjects.AdjustmentDecision;
import main.web.services.fitsense.adaptation.domain.model.valueobjects.AdjustmentType;
import main.web.services.fitsense.adaptation.domain.services.UserInterventionCommandService;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * LA FRONTERA, ejercitada de verdad.
 * <p>
 * {@code WeeklyCycleAdjustmentTest} construye {@code AdjustmentOrderView} a
 * mano, asi que comprueba la traduccion a {@code PlanAdjustment} dando por
 * hecho que la vista llega poblada. Esta prueba comprueba lo otro: que
 * {@code AdaptationContextFacade} de verdad PROYECTA las sesiones que produce
 * el decisor, en vez de dejarlas caer como hacia hasta ahora.
 * <p>
 * Entre las dos cubren el recorrido entero de la orden:
 * {@code AdaptationOrder} -> {@code AdjustmentOrderView} -> {@code PlanAdjustment}.
 * <p>
 * Se entra por {@code decideForWeek}, que es el metodo publico, con el servicio
 * de dentro simulado. No hace falta base de datos.
 */
class AdaptationContextFacadeProyeccionTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 10, 19);
    private static final long SESION_A = 101L;
    private static final long SESION_B = 104L;

    private UserInterventionCommandService servicio;
    private AdaptationContextFacade facade;

    @BeforeEach
    void montar() {
        servicio = mock(UserInterventionCommandService.class);
        facade = new AdaptationContextFacade(servicio);
    }

    @Test
    @DisplayName("Las sesiones senaladas CRUZAN la frontera, con su intervencion")
    void lasSenaladasCruzan() {
        devuelve(ordenLocalizada());

        var vista = facade.decideForWeek(7L, LUNES).orElseThrow();

        assertTrue(vista.adapta(), "la proyeccion dejo caer las sesiones senaladas");
        assertEquals(List.of(SESION_A, SESION_B),
                vista.targetedSessions().stream()
                        .map(AdjustmentOrderView.TargetedSessionView::plannedWorkoutId).toList());
        assertEquals(TipoIntervencion.SEGURIDAD_ANTE_DOLOR,
                vista.targetedSessions().get(0).intervencion());
    }

    @Test
    @DisplayName("Y cruzan SOLO dos campos de los seis")
    void cruzanDosCamposDeSeis() {
        // AdaptationOrder.TargetedSession lleva ademas fecha, cumplimiento,
        // motivo y RPE. No viajan a proposito: planning los obtiene de
        // context.previousWeek() por el mismo plannedWorkoutId. Si algun dia se
        // anadieran a la vista, esta prueba obliga a justificarlo.
        devuelve(ordenLocalizada());

        var senalada = facade.decideForWeek(7L, LUNES).orElseThrow()
                .targetedSessions().get(0);

        assertEquals(2, AdjustmentOrderView.TargetedSessionView.class
                        .getRecordComponents().length,
                "la vista del ACL es de dos campos: identificador e intervencion");
        assertEquals(SESION_A, senalada.plannedWorkoutId());
    }

    @Test
    @DisplayName("El desenlace CRUZA, y no se deduce contando sesiones")
    void elDesenlaceCruza() {
        // ADAPT_EXPANDED con DOS sesiones senaladas. Contar sesiones acertaria
        // aqui por casualidad, asi que la prueba de abajo es la que vale: una
        // EXPANDED con UNA sola senalada quedaria registrada como LOCALIZED si
        // el dato se dedujera en vez de transportarse.
        devuelve(ordenLocalizada());
        assertEquals("ADAPT_EXPANDED", facade.decideForWeek(7L, LUNES).orElseThrow().outcome());

        devuelve(new AdaptationOrder(
                AdaptationOutcome.ADAPT_EXPANDED,
                List.of(new AdaptationOrder.TargetedSession(SESION_A, LUNES,
                        new BigDecimal("0.00"), "PAIN_OR_DISCOMFORT", (short) 8,
                        TipoIntervencion.SEGURIDAD_ANTE_DOLOR)),
                new BigDecimal("42.00"), new BigDecimal("100.00"), 3, 3,
                "Una sola sesion quedo senalada, pero se abarcan todas."));

        assertEquals("ADAPT_EXPANDED",
                facade.decideForWeek(7L, LUNES).orElseThrow().outcome(),
                "una ampliada con una sola sesion senalada NO es una localizada");
    }

    @Test
    @DisplayName("Sin orden, el desenlace es NO_CHANGE y no null")
    void sinOrdenElDesenlaceEsNoChange() {
        devuelve(AdaptationOrder.sinCambios(AdaptationOutcome.NO_CHANGE,
                new BigDecimal("88.00"), new BigDecimal("100.00"), 3, 3,
                "Adherencia adecuada."));

        assertEquals("NO_CHANGE", facade.decideForWeek(7L, LUNES).orElseThrow().outcome());
    }

    @Test
    @DisplayName("Una orden sin sesiones proyecta una lista vacia, no un null")
    void sinSesionesListaVacia() {
        devuelve(AdaptationOrder.sinCambios(AdaptationOutcome.NO_CHANGE,
                new BigDecimal("88.00"), new BigDecimal("100.00"), 3, 3,
                "Adherencia adecuada: no se adapta."));

        var vista = facade.decideForWeek(7L, LUNES).orElseThrow();

        assertFalse(vista.adapta());
        assertNotNull(vista.targetedSessions());
        assertTrue(vista.targetedSessions().isEmpty());
    }

    @Test
    @DisplayName("Sin intervencion que decidir, no hay vista")
    void sinIntervencion() {
        when(servicio.handle(any(DecideWeeklyAdjustmentCommand.class)))
                .thenReturn(Optional.empty());

        assertTrue(facade.decideForWeek(7L, LUNES).isEmpty());
    }

    // =======================================================================
    // Material
    // =======================================================================

    private void devuelve(AdaptationOrder orden) {
        var intervencion = mock(UserIntervention.class);
        when(intervencion.getId()).thenReturn(55L);
        when(intervencion.getTriggerSkipReason()).thenReturn(null);

        when(servicio.handle(any(DecideWeeklyAdjustmentCommand.class)))
                .thenReturn(Optional.of(new UserInterventionCommandService.AdjustmentResult(
                        intervencion, sinAjuste(orden.reason()), orden)));
    }

    private static AdaptationOrder ordenLocalizada() {
        return new AdaptationOrder(
                AdaptationOutcome.ADAPT_EXPANDED,
                List.of(
                        new AdaptationOrder.TargetedSession(SESION_A, LUNES,
                                new BigDecimal("0.00"), "PAIN_OR_DISCOMFORT", (short) 8,
                                TipoIntervencion.SEGURIDAD_ANTE_DOLOR),
                        new AdaptationOrder.TargetedSession(SESION_B, LUNES.plusDays(3),
                                new BigDecimal("40.00"), "LACK_OF_TIME", (short) 5,
                                TipoIntervencion.ACORTAR_O_REORGANIZAR)),
                new BigDecimal("42.00"), new BigDecimal("100.00"), 3, 3,
                "Por debajo de 60 %: se abarcan las 2 sesiones senaladas.");
    }

    /** Lo mismo que escribe el servicio desde la activacion: NONE / 0. */
    private static AdjustmentDecision sinAjuste(String motivo) {
        return new AdjustmentDecision(List.of(AdjustmentType.NONE), 0, 0, 0,
                0.0, 0.0, null, null, null, motivo, null, motivo);
    }
}
