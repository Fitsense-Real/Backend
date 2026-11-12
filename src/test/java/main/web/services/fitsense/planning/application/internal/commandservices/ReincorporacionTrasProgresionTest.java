package main.web.services.fitsense.planning.application.internal.commandservices;

import main.web.services.fitsense.planning.application.internal.commandservices.EvidenciaV32.Clase;
import main.web.services.fitsense.planning.application.internal.commandservices.EvidenciaV32.Informe;
import main.web.services.fitsense.planning.application.internal.commandservices.ProgresionResolver.HechoDeLinaje;
import main.web.services.fitsense.shared.domain.model.valueobjects.TipoIntervencion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 1(a), 27/10/2026: SOLO UNA PROGRESION de la misma sesion, posterior al ultimo
 * acortamiento, habilita evaluar la reincorporacion. Los hechos van del mas
 * reciente al mas antiguo, como el linaje.
 */
@DisplayName("1(a) · solo una PROGRESION posterior al acortamiento, de la misma sesion, habilita la reincorporacion")
class ReincorporacionTrasProgresionTest {

    // Cada semana el viernes es otra fila (el linaje): 30 acortado, 31, 32, 33...
    private static final long OTRA_SESION = 999L;

    @Test
    @DisplayName("una RECUPERACION sin progresion posterior no habilita")
    void recuperacionNoHabilita() {
        assertEquals(0, ProgresionResolver.progresionesTrasAcortar(List.of(
                paso(32, "RECUPERACION"),
                sinPaso(31),
                acortar(30))));
    }

    @Test
    @DisplayName("una PROGRESION ANTERIOR al acortamiento no cuenta")
    void progresionAnteriorNoCuenta() {
        assertEquals(0, ProgresionResolver.progresionesTrasAcortar(List.of(
                sinPaso(32),
                acortar(31),
                paso(30, "PROGRESION"))));
    }

    @Test
    @DisplayName("se cuenta desde el ULTIMO acortamiento: una progresion entre dos acortamientos no cuenta")
    void desdeElUltimoAcortamiento() {
        assertEquals(0, ProgresionResolver.progresionesTrasAcortar(List.of(
                acortar(33),
                paso(32, "PROGRESION"),
                acortar(31))));
    }

    @Test
    @DisplayName("una PROGRESION de OTRA sesion, aunque venga en el mismo informe, no cuenta")
    void otraSesionNoCuenta() {
        var deOtra = new HechoDeLinaje(32L, Clase.PROGRESADA, null, new Informe(List.of(),
                List.of(new Informe.Paso(OTRA_SESION, "PROGRESION", 7L, null, null, 100, 104, 100, 110, null))));
        assertEquals(0, ProgresionResolver.progresionesTrasAcortar(List.of(deOtra, sinPaso(31), acortar(30))));
    }

    @Test
    @DisplayName("ni una REINCORPORACION ni una VUELTA_ATRAS cuentan")
    void otrosTiposNoCuentan() {
        assertEquals(0, ProgresionResolver.progresionesTrasAcortar(List.of(
                paso(33, "VUELTA_ATRAS"),
                paso(32, "REINCORPORACION"),
                acortar(31))));
    }

    @Test
    @DisplayName("una PROGRESION valida posterior al acortamiento SI habilita evaluar la reincorporacion")
    void progresionPosteriorHabilita() {
        assertEquals(1, ProgresionResolver.progresionesTrasAcortar(List.of(
                sinPaso(33),
                paso(32, "PROGRESION"),
                paso(31, "RECUPERACION"),
                acortar(30))));
    }

    @Test
    @DisplayName("sin acortamiento en el linaje se cuentan solo las progresiones")
    void sinAcortamiento() {
        assertEquals(2, ProgresionResolver.progresionesTrasAcortar(List.of(
                paso(33, "PROGRESION"), paso(32, "RECUPERACION"), paso(31, "PROGRESION"))));
    }

    // =======================================================================

    private static HechoDeLinaje acortar(long sesion) {
        return new HechoDeLinaje(sesion, Clase.APLICADA, TipoIntervencion.ACORTAR_O_REORGANIZAR,
                new Informe(List.of(), List.of()));
    }

    private static HechoDeLinaje sinPaso(long sesion) {
        return new HechoDeLinaje(sesion, Clase.CONSERVADA, null, new Informe(List.of(), List.of()));
    }

    private static HechoDeLinaje paso(long sesion, String tipo) {
        return new HechoDeLinaje(sesion, Clase.PROGRESADA, null, new Informe(List.of(),
                List.of(new Informe.Paso(sesion, tipo, 7L, null, null, 100, 104, 100, 110, "PENDIENTE"))));
    }
}
