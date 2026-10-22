package main.web.services.fitsense.orchestration.application.internal;

import main.web.services.fitsense.adaptation.interfaces.acl.AdjustmentOrderView;
import main.web.services.fitsense.planning.domain.model.valueobjects.PlanAdjustment;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LA TRADUCCION QUE HACE ALCANZABLE LA ADAPTACION.
 * <p>
 * Hasta el 21/10/2026 el camino de adaptacion localizada estaba construido
 * entero y era INALCANZABLE desde el ciclo semanal: {@code AdaptationOrderDecider}
 * calculaba las sesiones senaladas, pero {@code AdjustmentOrderView} no las
 * llevaba y {@code toAdjustment} llamaba al constructor de doce argumentos de
 * {@code PlanAdjustment}, que deja {@code targetedSessions} vacio. Y
 * {@code adapta()} es literalmente {@code !targetedSessions.isEmpty()}.
 * <p>
 * Estas pruebas fijan las dos mitades de la regla:
 * <ul>
 *   <li>una orden localizada llega con {@code adapta() == true} y
 *       {@code isActive() == false} -los dos mecanismos no coexisten-;
 *   <li>sin orden no se dispara NADA: ni adaptacion ni la progresion
 *       porcentual heredada.
 * </ul>
 * Se entra por {@code toAdjustment}, que es de paquete, y no por
 * {@code runFor}: lo que se comprueba es la traduccion, y montar los tres
 * facades para eso probaria otra cosa.
 */
class WeeklyCycleAdjustmentTest {

    private static final long SESION_A = 101L;
    private static final long SESION_B = 104L;

    // =======================================================================
    // Orden localizada
    // =======================================================================

    @Test
    @DisplayName("Una orden localizada llega con adapta() y SIN objetivo porcentual")
    void ordenLocalizadaLlegaCompleta() {
        var ajuste = WeeklyCycleService.toAdjustment(localizada());

        assertTrue(ajuste.adapta(), "las sesiones senaladas no cruzaron la frontera");
        assertFalse(ajuste.isActive(),
                "isActive() mira targetVolume: una orden localizada no lleva objetivo porcentual");
        assertNull(ajuste.targetVolume());
        assertNull(ajuste.targetVolumeMin());
        assertNull(ajuste.targetVolumeMax());
    }

    @Test
    @DisplayName("Y lleva QUE sesion y QUE se permite hacer en ella")
    void laOrdenConservaIdentificadorEIntervencion() {
        var ajuste = WeeklyCycleService.toAdjustment(localizada());

        assertEquals(2, ajuste.targetedSessions().size());
        assertEquals(List.of(SESION_A, SESION_B),
                ajuste.targetedSessions().stream()
                        .map(PlanAdjustment.TargetedSession::plannedWorkoutId).toList());
        assertEquals(TipoIntervencion.SEGURIDAD_ANTE_DOLOR,
                ajuste.targetedSessions().get(0).intervencion(),
                "la intervencion decide que se le permite cambiar a la IA en esa sesion");
    }

    @Test
    @DisplayName("Construir el ajuste localizado NO lanza")
    void noLanza() {
        // Es la prueba del autoboxing. AdjustmentOrderView.targetVolume() es un
        // int primitivo: pasarlo tal cual a un parametro Integer lo convierte en
        // no nulo aunque valga cero, y PlanAdjustment rechaza targetVolume no
        // nulo junto a sesiones senaladas. Sin la condicion de toAdjustment,
        // esto lanzaria SIEMPRE y la adaptacion nunca llegaria al generador.
        assertDoesNotThrow(() -> WeeklyCycleService.toAdjustment(localizada()));
    }

    // =======================================================================
    // Sin orden: no se dispara nada
    // =======================================================================

    @Test
    @DisplayName("Sin sesiones senaladas no hay adaptacion NI progresion heredada")
    void sinOrdenNoSeDisparaNada() {
        // El caso de adherencia adecuada y el de evidencia insuficiente llegan
        // los dos asi: NONE y cero. Lo que se vigila es que ese cero no se
        // convierta en un ajuste activo.
        var ajuste = WeeklyCycleService.toAdjustment(sinSesiones());

        assertFalse(ajuste.adapta(), "no hay sesiones senaladas: no se adapta");
        assertFalse(ajuste.isActive(),
                "targetVolume = 0 autoboxeado daria isActive() cierto y marcaria el plan"
                        + " con adjustment_applied = NONE todas las semanas");
        assertNull(ajuste.targetVolume());
        assertEquals(0.0, ajuste.targetVolumeChangePct());
        assertTrue(ajuste.targetedSessions().isEmpty());
    }

    @Test
    @DisplayName("Si el mecanismo porcentual actuara, su objetivo SI viaja")
    void elMecanismoPorcentualSigueFuncionando() {
        // AdjustmentDecisionTable salio del flujo, no del codigo: se conserva
        // porque es el mecanismo contra el que se compara en la tesis. Si
        // volviera a decidir, la traduccion tiene que seguir siendo correcta, y
        // sin esta prueba la condicion de toAdjustment podria anular el camino
        // viejo sin que nadie se enterara.
        var ajuste = WeeklyCycleService.toAdjustment(porcentual());

        assertTrue(ajuste.isActive());
        assertEquals(120, ajuste.targetVolume());
        assertEquals(-15.0, ajuste.targetVolumeChangePct());
        assertFalse(ajuste.adapta());
    }

    // =======================================================================
    // Material
    // =======================================================================

    /** Lo que produce el ciclo desde la activacion: NONE / 0 mas las senaladas. */
    private static AdjustmentOrderView localizada() {
        return new AdjustmentOrderView(
                7L, List.of("NONE"), "NONE",
                0, 0, 0, 0.0, 0.0,
                null, null, null,
                "Por debajo del umbral: se senala la sesion peor cumplida.",
                "PAIN_OR_DISCOMFORT", null, null,
                "ADAPT_EXPANDED",
                List.of(new AdjustmentOrderView.TargetedSessionView(
                                SESION_A, TipoIntervencion.SEGURIDAD_ANTE_DOLOR),
                        new AdjustmentOrderView.TargetedSessionView(
                                SESION_B, TipoIntervencion.ACORTAR_O_REORGANIZAR)));
    }

    /** Adherencia adecuada o evidencia insuficiente: el mismo material. */
    private static AdjustmentOrderView sinSesiones() {
        return new AdjustmentOrderView(
                7L, List.of("NONE"), "NONE",
                0, 0, 0, 0.0, 0.0,
                null, null, null,
                "Adherencia adecuada: no se adapta.",
                null, null, null,
                "NO_CHANGE",
                List.of());
    }

    /** El mecanismo viejo, por si volviera. */
    private static AdjustmentOrderView porcentual() {
        return new AdjustmentOrderView(
                7L, List.of("REDUCE_VOLUME"), "REDUCE_VOLUME",
                120, 110, 130, -15.0, 0.0,
                null, null, null,
                "Reduccion del 15 %.",
                "LACK_OF_TIME", null, null,
                // NO_CHANGE y no un ADAPT_*: el mecanismo porcentual no es un
                // desenlace de adaptacion, y decir lo contrario sugeriria que
                // los dos mecanismos pueden coexistir.
                "NO_CHANGE",
                List.of());
    }
}
