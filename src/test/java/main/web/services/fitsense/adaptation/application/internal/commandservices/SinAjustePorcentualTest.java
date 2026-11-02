package main.web.services.fitsense.adaptation.application.internal.commandservices;

import main.web.services.fitsense.adaptation.domain.model.aggregates.UserIntervention;
import main.web.services.fitsense.shared.domain.model.valueobjects.SkipReason;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LA FILA QUE SE ESCRIBE EN {@code user_interventions} CUANDO EL MECANISMO
 * PORCENTUAL YA NO ACTUA.
 * <p>
 * Esta prueba existe por un fallo concreto: la primera version de
 * {@code sinAjustePorcentual} pasaba {@code null} como mensaje. Las cinco
 * pruebas de traduccion del ciclo semanal pasaban igual, porque ninguna
 * construia una intervencion — y {@code UserIntervention} hace
 * {@code this.messageShown = decision.message()} contra una columna
 * {@code VARCHAR(500) NOT NULL}. El fallo no habria salido hasta que el ciclo
 * semanal intentara guardar su primera fila, en produccion.
 * <p>
 * La leccion, que es la de siempre: una traduccion correcta no demuestra que lo
 * traducido se pueda persistir.
 * <p>
 * NO TOCA LA BASE. Comprueba que el agregado se construye y que lleva los
 * valores que la base exige; que Postgres los acepte es P2-D.
 */
class SinAjustePorcentualTest {

    @Test
    @DisplayName("La decision sin ajuste porcentual SI trae mensaje, que es NOT NULL en V6")
    void traeMensaje() {
        var decision = UserInterventionCommandServiceImpl
                .sinAjustePorcentual("Por debajo de 70 %: se corrige solo la peor sesion.");

        assertNotNull(decision.message(), "message_shown es VARCHAR(500) NOT NULL");
        assertFalse(decision.message().isBlank());
        assertTrue(decision.message().length() <= 500,
                () -> "no cabe en message_shown: " + decision.message().length());
    }

    @Test
    @DisplayName("Con un motivo vacio hay respaldo, no un null")
    void motivoVacio() {
        for (String motivo : new String[]{null, "", "   "}) {
            var decision = UserInterventionCommandServiceImpl.sinAjustePorcentual(motivo);
            assertNotNull(decision.message(), () -> "motivo <" + motivo + "> dejo el mensaje en null");
            assertFalse(decision.message().isBlank());
        }
    }

    @Test
    @DisplayName("Y la intervencion SE CONSTRUYE con ella: es lo que no se comprobaba")
    void laIntervencionSeConstruye() {
        var decision = UserInterventionCommandServiceImpl
                .sinAjustePorcentual("Adherencia observada 42 % por debajo del umbral.");

        var intervencion = assertDoesNotThrow(() -> new UserIntervention(
                7L, 300L, 400L,
                new BigDecimal("42.00"), SkipReason.PAIN_OR_DISCOMFORT,
                decision, 120, 3, 45, 2, "v6"));

        // Lo que acaba en la fila. NONE y cero son la verdad: no se aplico
        // ningun ajuste porcentual, y la adaptacion localizada se registra en
        // plan_adaptation_attempts, no aqui.
        assertEquals(decision.message(), intervencion.getMessageShown());
        assertNotNull(intervencion.getMessageShown());
        assertArrayEquals(new String[]{"NONE"}, intervencion.getAdjustmentTypes());
        assertEquals(0, intervencion.getTargetVolumeChangePct().compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("NONE va solo y es uno: lo exigen ck_ui_none_alone y ck_ui_types_size")
    void noneVaSoloYEsUno() {
        var tipos = UserInterventionCommandServiceImpl.sinAjustePorcentual("motivo").typeNames();

        assertEquals(1, tipos.size(), "ck_ui_types_size exige entre 1 y 4");
        assertEquals("NONE", tipos.get(0),
                "ck_ui_none_alone exige que NONE no conviva con otro tipo");
    }
}
