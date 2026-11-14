package main.web.services.fitsense.orchestation;

import main.web.services.fitsense.orchestation.ComparadorDosisPorColocacion.Colocacion;
import main.web.services.fitsense.orchestation.ComparadorDosisPorColocacion.Tipo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * El comparador por colocacion (orden de 14/11/2026, fase 1): detecta las subidas que el maximo semanal oculta, no
 * inventa equivalencias y no confunde una sustitucion con una subida.
 */
@DisplayName("Comparador de dosis por colocacion: ninguna subida queda oculta por una agregacion semanal")
class ComparadorDosisPorColocacionTest {

    static final LocalDate LUN = LocalDate.of(2026, 11, 16), MIE = LUN.plusDays(2), JUE = LUN.plusDays(3);
    static final LocalDate LUN2 = LUN.plusWeeks(1), MIE2 = MIE.plusWeeks(1), JUE2 = JUE.plusWeeks(1);

    static Colocacion c(LocalDate f, long e, int s, int r) {
        return new Colocacion(f, e, s, r, 0);
    }

    static Colocacion d(LocalDate f, long e, int s, int seg) {
        return new Colocacion(f, e, s, 0, seg);
    }

    @Test
    @DisplayName("el ejemplo de la orden: sube el lunes (3x10 -> 3x12) aunque el maximo semanal baje (3x15 -> 3x13)")
    void maximoSemanalOcultaLaSubida() {
        var medida = List.of(c(LUN, 100, 3, 10), c(JUE, 100, 3, 15));
        var publicada = List.of(c(LUN2, 100, 3, 12), c(JUE2, 100, 3, 13));
        // El criterio anterior (maximo por ejercicio) no la veia: max 15 -> 13.
        int maxMedida = medida.stream().mapToInt(Colocacion::reps).max().orElseThrow();
        int maxPublicada = publicada.stream().mapToInt(Colocacion::reps).max().orElseThrow();
        assertTrue(maxPublicada <= maxMedida, "premisa: el maximo semanal baja");

        var r = ComparadorDosisPorColocacion.comparar(medida, publicada);
        assertEquals(2, r.correspondidas());
        assertEquals(1, r.aumentos().size(), r.describir());
        var a = r.aumentos().get(0);
        assertEquals(Tipo.AUMENTO, a.tipo());
        assertEquals(LUN2, a.publicada().fecha());
        assertTrue(a.dimensiones().contains("repeticiones 10->12"), a.dimensiones().toString());
        assertTrue(r.trabajoTotalNoSube(), "el total incluso baja (75 -> 75): por eso hace falta la colocacion");
    }

    @Test
    @DisplayName("el mismo ejercicio en dos sesiones, sin cambios: cada colocacion con la suya y ningun aumento")
    void mismoEjercicioEnDosSesiones() {
        var r = ComparadorDosisPorColocacion.comparar(List.of(c(LUN, 7, 3, 10), c(JUE, 7, 2, 13)),
                List.of(c(LUN2, 7, 3, 10), c(JUE2, 7, 2, 13)));
        assertTrue(r.sinAumentos(), r.describir());
        assertEquals(2, r.correspondidas());
    }

    @Test
    @DisplayName("varias veces el mismo dia: se emparejan por orden de aparicion y la segunda sube")
    void repetidoElMismoDia() {
        var r = ComparadorDosisPorColocacion.comparar(List.of(c(LUN, 7, 3, 10), c(LUN, 7, 2, 10)),
                List.of(c(LUN2, 7, 3, 10), c(LUN2, 7, 3, 10)));
        assertEquals(1, r.aumentos().size(), r.describir());
        assertTrue(r.aumentos().get(0).dimensiones().contains("series 2->3"));
    }

    @Test
    @DisplayName("cambio de dia: sin correspondencia fiable no se declara equivalencia; se compara con la menor dosis medida")
    void cambioDeDia() {
        var medida = List.of(c(LUN, 9, 3, 10), c(JUE, 9, 3, 15));
        var sube = ComparadorDosisPorColocacion.comparar(medida, List.of(c(MIE2, 9, 3, 12)));
        assertEquals(0, sube.correspondidas());
        assertEquals(1, sube.sinCorrespondencia().size());
        assertEquals(1, sube.aumentos().size(), sube.describir());
        assertEquals(Tipo.AUMENTO_SIN_CORRESPONDENCIA, sube.aumentos().get(0).tipo());

        var igual = ComparadorDosisPorColocacion.comparar(medida, List.of(c(MIE2, 9, 3, 10)));
        assertTrue(igual.sinAumentos(), igual.describir());
        assertEquals(1, igual.sinCorrespondencia().size(), "se informa la falta de correspondencia aunque no suba");
    }

    @Test
    @DisplayName("sustitucion por dolor: el sustituto es NUEVO (sin equivalencia por colocacion) y cuenta en el total")
    void sustitucionPorDolor() {
        var r = ComparadorDosisPorColocacion.comparar(List.of(c(LUN, 50, 3, 10), c(LUN, 51, 2, 13)),
                List.of(c(LUN2, 60, 3, 10), c(LUN2, 51, 2, 13)));
        assertTrue(r.sinAumentos(), r.describir());
        assertEquals(1, r.nuevas().size());
        assertEquals(60, r.nuevas().get(0).ejercicio());
        assertTrue(r.trabajoTotalNoSube());
        // Un sustituto con mas trabajo que lo que sustituye no se ve por colocacion, pero si en el total.
        var mas = ComparadorDosisPorColocacion.comparar(List.of(c(LUN, 50, 3, 10)), List.of(c(LUN2, 60, 3, 12)));
        assertTrue(mas.sinAumentos());
        assertFalse(mas.trabajoTotalNoSube(), "el total lo detecta: 30 -> 36");
    }

    static Colocacion o(LocalDate f, long e, int s, int r, int orden) {
        return new Colocacion(f, e, s, r, 0, orden);
    }

    @Test
    @DisplayName("sustituto en el puesto del retirado, que existe reducido otro dia: se compara con el retirado, no es una restauracion")
    void sustitutoQueExisteOtroDia() {
        // Medida: el lunes 50 (dolio) 3x10 en la posicion 2; el viernes 375 a 2x10 (mantenimiento por fatiga).
        var medida = List.of(o(LUN, 50, 3, 10, 2), o(JUE, 375, 2, 10, 1));
        // Publicada: el lunes 375 a 3x10 en la posicion 2 (sustituye a 50); el viernes 375 sigue a 2x10.
        var publicada = List.of(o(LUN2, 375, 3, 10, 2), o(JUE2, 375, 2, 10, 1));
        var sinRetirados = ComparadorDosisPorColocacion.comparar(medida, publicada);
        assertEquals(1, sinRetirados.aumentos().size(), "sin saber que 50 se retiro, la regla conservadora lo marca");
        var r = ComparadorDosisPorColocacion.comparar(medida, publicada, java.util.Set.of(50L));
        assertTrue(r.sinAumentos(), r.describir());
        assertEquals(1, r.sustituciones().size());
        assertEquals(1, r.correspondidas(), "el viernes reducido se corresponde y no sube");
    }

    @Test
    @DisplayName("sustituto con mas dosis que el retirado en su puesto: AUMENTO_EN_SUSTITUCION")
    void sustitutoConMasDosis() {
        var r = ComparadorDosisPorColocacion.comparar(List.of(o(LUN, 50, 2, 10, 0)), List.of(o(LUN2, 60, 3, 10, 0)),
                java.util.Set.of(50L));
        assertEquals(1, r.aumentos().size(), r.describir());
        assertEquals(Tipo.AUMENTO_EN_SUSTITUCION, r.aumentos().get(0).tipo());
    }

    @Test
    @DisplayName("restauracion: la colocacion reducida (2x10) vuelve a 3x10 el mismo dia; se detecta aunque el total baje")
    void restauracionDeUnaReduccion() {
        var r = ComparadorDosisPorColocacion.comparar(List.of(o(JUE, 375, 2, 10, 1), o(JUE, 400, 3, 12, 2)),
                List.of(o(JUE2, 375, 3, 10, 1)));
        assertTrue(r.trabajoTotalNoSube(), "premisa: 56 -> 30");
        assertEquals(1, r.aumentos().size(), r.describir());
        assertEquals(Tipo.AUMENTO, r.aumentos().get(0).tipo());
    }

    @Test
    @DisplayName("la regeneracion mantiene el trabajo: ningun aumento y total igual")
    void mantiene() {
        var semana = List.of(c(LUN, 1, 3, 10), c(MIE, 2, 2, 13), d(JUE, 3, 2, 30));
        var r = ComparadorDosisPorColocacion.comparar(semana, List.of(c(LUN2, 1, 3, 10), c(MIE2, 2, 2, 13),
                d(JUE2, 3, 2, 30)));
        assertTrue(r.sinAumentos());
        assertTrue(r.trabajoTotalNoSube());
        assertEquals(3, r.correspondidas());
    }

    @Test
    @DisplayName("la regeneracion reduce el trabajo: ningun aumento y total menor")
    void reduce() {
        var r = ComparadorDosisPorColocacion.comparar(List.of(c(LUN, 1, 3, 10), d(JUE, 3, 3, 30)),
                List.of(c(LUN2, 1, 2, 10), d(JUE2, 3, 2, 30)));
        assertTrue(r.sinAumentos());
        assertTrue(r.repsPublicada() < r.repsMedida() && r.segundosPublicada() < r.segundosMedida());
    }

    @Test
    @DisplayName("la regeneracion intenta aumentar: series, repeticiones y segundos se detectan cada uno")
    void intentaAumentar() {
        var r = ComparadorDosisPorColocacion.comparar(List.of(c(LUN, 1, 3, 10), c(MIE, 2, 2, 13), d(JUE, 3, 2, 30)),
                List.of(c(LUN2, 1, 4, 10), c(MIE2, 2, 2, 15), d(JUE2, 3, 2, 40)));
        assertEquals(3, r.aumentos().size(), r.describir());
        assertFalse(r.trabajoTotalNoSube());
    }

    @Test
    @DisplayName("subida compensada: una colocacion sube y otra baja; el total no sube, pero la subida se detecta")
    void subidaCompensada() {
        var r = ComparadorDosisPorColocacion.comparar(List.of(c(LUN, 1, 3, 10), c(JUE, 2, 3, 12)),
                List.of(c(LUN2, 1, 3, 12), c(JUE2, 2, 3, 10)));
        assertTrue(r.trabajoTotalNoSube(), "premisa: 66 -> 66");
        assertEquals(1, r.aumentos().size(), r.describir());
    }
}
