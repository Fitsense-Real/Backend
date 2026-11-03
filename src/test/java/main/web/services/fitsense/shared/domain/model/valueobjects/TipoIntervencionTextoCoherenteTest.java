package main.web.services.fitsense.shared.domain.model.valueobjects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * `FIX-PR-1.2` · EL TEXTO QUE RECIBE EL MODELO CONCUERDA CON LOS PERMISOS.
 *
 * <h2>Que comprueba, y que no</h2>
 *
 * NO comprueba que el texto este bien escrito: eso no se automatiza. Comprueba
 * que <b>no prometa ni prohiba lo contrario de lo que las banderas permiten</b>,
 * que es el defecto que causo el {@code SIN_EFECTO} de la corrida real: el texto
 * de {@code ACORTAR_O_REORGANIZAR} decia «conservando los ejercicios cuando sea
 * posible» —una preferencia con excepcion— cuando la bandera era un no rotundo, y
 * no decia nada del dia ni del minimo de duracion.
 * <p>
 * ESTOS TEXTOS NO SON SOLO PARA EL MODELO. {@code ConservationValidator} los
 * concatena a sus mensajes de rechazo, asi que un texto que prometa un permiso
 * inexistente acaba en la evidencia que lee la tesis.
 *
 * <h2>Por que esta prueba no es fragil</h2>
 *
 * No afirma frases literales. Afirma una correspondencia entre el texto y
 * {@code permiteSustituirEjercicio()}, {@code permiteCambiarDosis()},
 * {@code permiteReprogramar()} y {@code permiteQuitarEjercicio()}. Si alguien
 * cambia una bandera sin tocar el texto, o al reves, falla aqui y no en una
 * corrida de pago.
 *
 * <h2>El detalle que casi se me escapa</h2>
 *
 * RETIRAR SIN ANADIR YA ESTABA PERMITIDO en las intervenciones que pueden
 * sustituir. {@code ConservationValidator.revisarSustitucion} vuelve antes de
 * comprobar nada cuando no hay anadidos —{@code if (d.anadidos().isEmpty())
 * return;}—, asi que {@code VARIANTE_MAS_SENCILLA_O_CARGA} y
 * {@code SEGURIDAD_ANTE_DOLOR} podian retirar desde antes de INC-P2D-01. Sus
 * textos no pueden decir «no retirar», y de ahi {@link #puedeRetirar}.
 * <p>
 * Dominio puro: sin Spring, sin base de datos y sin Replicate.
 */
@DisplayName("FIX-PR-1.2 · el texto de cada intervencion concuerda con sus permisos")
class TipoIntervencionTextoCoherenteTest {

    /**
     * Retirar sin anadir esta permitido si la intervencion puede sustituir —el
     * camino vuelve antes— o si tiene el permiso propio de INC-P2D-01.
     */
    private static boolean puedeRetirar(TipoIntervencion tipo) {
        return tipo.permiteSustituirEjercicio() || tipo.permiteQuitarEjercicio();
    }

    private static String texto(TipoIntervencion tipo) {
        return tipo.instruccion().toLowerCase(Locale.ROOT);
    }

    @Test
    @DisplayName("Ninguna instruccion esta vacia ni se queda sin prohibiciones")
    void todasDicenAlgoYProhibenAlgo() {
        for (var tipo : TipoIntervencion.values()) {
            var t = texto(tipo);
            assertFalse(t.isBlank(), tipo + " no tiene instruccion");
            assertTrue(t.contains(" no "),
                    tipo + " no enuncia ninguna prohibicion, y todas tienen alguna."
                            + " Era el defecto del texto anterior de"
                            + " ACORTAR_O_REORGANIZAR: " + tipo.instruccion());
        }
    }

    @Test
    @DisplayName("Solo REPROGRAMAR_DIAS promete cambiar el dia; las demas lo prohiben")
    void elDiaSoloDondeSePuede() {
        for (var tipo : TipoIntervencion.values()) {
            var t = texto(tipo);
            if (tipo.permiteReprogramar())
                assertTrue(t.contains("reprogramar el dia"),
                        tipo + " puede cambiar el dia y su texto no lo dice: "
                                + tipo.instruccion());
            else
                assertTrue(t.contains("no cambiar el dia") || t.contains("ni el dia"),
                        tipo + " NO puede cambiar el dia y su texto no lo prohibe."
                                + " Es el error que movio la sesion de lunes a"
                                + " miercoles en la corrida real: " + tipo.instruccion());
        }
    }

    @Test
    @DisplayName("Solo las que pueden retirar lo mencionan; las que no, lo prohiben")
    void retirarSoloDondeSePuede() {
        for (var tipo : TipoIntervencion.values()) {
            var t = texto(tipo);
            if (puedeRetirar(tipo))
                assertFalse(t.contains("no sustituir ni retirar")
                                || t.contains("no retirar"),
                        tipo + " SI puede retirar sin anadir y su texto lo prohibe: "
                                + tipo.instruccion());
            else
                assertTrue(t.contains("retirar"),
                        tipo + " no puede retirar y su texto no lo dice. El modelo"
                                + " no tiene otra forma de saberlo: " + tipo.instruccion());
        }
    }

    @Test
    @DisplayName("Solo ACORTAR_O_REORGANIZAR habla del relleno y del minimo de duracion")
    void elPermisoNuevoSoloEnSuIntervencion() {
        for (var tipo : TipoIntervencion.values()) {
            var t = texto(tipo);
            boolean esLaDelPermiso = tipo == TipoIntervencion.ACORTAR_O_REORGANIZAR;

            // El permiso nuevo de INC-P2D-01: una colocacion de relleno.
            assertTrue(t.contains("relleno") == esLaDelPermiso,
                    tipo + ": solo ACORTAR_O_REORGANIZAR puede retirar relleno, y el"
                            + " texto tiene que decirlo exactamente ahi. "
                            + tipo.instruccion());

            // La exencion de V20, que es la causa directa del SIN_EFECTO de anoche.
            assertTrue(t.contains("duracion minima") == esLaDelPermiso,
                    tipo + ": la exencion del minimo de duracion es por sesion y solo"
                            + " la tiene ACORTAR_O_REORGANIZAR. " + tipo.instruccion());
        }
    }

    @Test
    @DisplayName("El permiso de retirar relleno sigue siendo de una sola intervencion")
    void soloUnaTieneElPermisoDeQuitar() {
        for (var tipo : TipoIntervencion.values())
            assertTrue(tipo.permiteQuitarEjercicio()
                            == (tipo == TipoIntervencion.ACORTAR_O_REORGANIZAR),
                    "permiteQuitarEjercicio() mal para " + tipo);
    }

    @Test
    @DisplayName("Las que no pueden cambiar la dosis lo dicen")
    void laDosisSoloDondeSePuede() {
        for (var tipo : TipoIntervencion.values()) {
            if (tipo.permiteCambiarDosis()) continue;
            var t = texto(tipo);
            assertTrue(t.contains("no cambiar la dosis") || t.contains("ni la dosis")
                            || t.contains("no cambiar la dosis,"),
                    tipo + " no puede cambiar la dosis y su texto no lo prohibe: "
                            + tipo.instruccion());
        }
    }

    @Test
    @DisplayName("La sustitucion respeta el permiso de cada intervencion")
    void sustituirSoloDondeSePuede() {
        for (var tipo : TipoIntervencion.values()) {
            var t = texto(tipo);

            boolean prohibe = t.contains("no sustituir")
                    || t.contains("no inventar una sustitucion");

            assertTrue(prohibe != tipo.permiteSustituirEjercicio(),
                    tipo + " tiene una instruccion incompatible con su permiso "
                            + "de sustitucion: " + tipo.instruccion());
        }
    }
}
