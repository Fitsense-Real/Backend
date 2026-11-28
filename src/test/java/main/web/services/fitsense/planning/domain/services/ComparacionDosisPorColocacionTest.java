package main.web.services.fitsense.planning.domain.services;

import main.web.services.fitsense.planning.domain.services.ComparacionDosisPorColocacion.Colocacion;
import main.web.services.fitsense.planning.domain.services.ComparacionDosisPorColocacion.Tipo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.time.DayOfWeek.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * P-03 POR COLOCACION · los 10 casos obligatorios de la orden de cierre premerge (VALIDACION_P03_POR_COLOCACION.md).
 * Semana medida tipica: lunes A 3x10, B 3x10 (que dolio); miercoles C 3x10; viernes A 2x10 (REDUCIDA, mantenimiento),
 * D 3x10.
 */
@DisplayName("P-03 por colocacion: ninguna colocacion recupera dosis sin autorizacion; el total se sigue comprobando aparte")
class ComparacionDosisPorColocacionTest {

    static final long A = 100, B = 200, C = 300, D = 400, E = 500;

    static Colocacion c(java.time.DayOfWeek dia, int pos, long ej, int series, int reps) {
        return new Colocacion(dia, pos, ej, series, reps, 0);
    }

    static final List<Colocacion> MEDIDA = List.of(
            c(MONDAY, 0, A, 3, 10), c(MONDAY, 1, B, 3, 10),
            c(WEDNESDAY, 0, C, 3, 10),
            c(FRIDAY, 0, A, 2, 10), c(FRIDAY, 1, D, 3, 10));

    @Test
    @DisplayName("1-2 · contraejemplo: el viernes A vuelve de 2x10 a 3x10 y se quita B; el total baja, pero se detecta la restauracion")
    void contraejemplo() {
        var regenerada = List.of(c(MONDAY, 0, A, 3, 10), c(WEDNESDAY, 0, C, 3, 10),
                c(FRIDAY, 0, A, 3, 10), c(FRIDAY, 1, D, 3, 10));
        assertTrue(total(regenerada) <= total(MEDIDA), "premisa: el total no sube (lo que aceptaba P-03 solo por total)");
        var r = ComparacionDosisPorColocacion.comparar(MEDIDA, regenerada, Set.of(B));
        assertEquals(1, r.aumentos().size(), r.toString());
        var a = r.aumentos().get(0);
        assertEquals(Tipo.MANTENIDA, a.tipo());
        assertEquals(FRIDAY, a.regenerada().dia());
        assertEquals(A, a.regenerada().ejercicio());
        assertTrue(a.describir().contains("2x10") && a.describir().contains("3x10"), a.describir());
    }

    @Test
    @DisplayName("3 · reduccion legitima: bajar dosis nunca es un aumento")
    void reduccionLegitima() {
        var regenerada = List.of(c(MONDAY, 0, A, 2, 10), c(WEDNESDAY, 0, C, 2, 8),
                c(FRIDAY, 0, A, 2, 10), c(FRIDAY, 1, D, 2, 10));
        assertTrue(ComparacionDosisPorColocacion.comparar(MEDIDA, regenerada, Set.of(B)).sinAumentos());
    }

    @Test
    @DisplayName("4 · progresion autorizada en la medida (3x12): la regenerada con dosis base (3x10) no la viola ni la bloquea")
    void progresionAutorizada() {
        var progresada = new ArrayList<>(MEDIDA);
        progresada.set(2, c(WEDNESDAY, 0, C, 3, 12));   // paso de progresion autorizado (V31) en la semana medida
        var regenerada = List.of(c(MONDAY, 0, A, 3, 10), c(WEDNESDAY, 0, C, 3, 10), c(FRIDAY, 0, A, 2, 10),
                c(FRIDAY, 1, D, 3, 10));
        assertTrue(ComparacionDosisPorColocacion.comparar(progresada, regenerada, Set.of(B)).sinAumentos(),
                "P-03 regenera con dosis base: nunca supera una dosis progresada");
    }

    @Test
    @DisplayName("5 · ejercicio repetido en distintos dias: se empareja por dia; otro dia, contra la MENOR dosis medida")
    void repetidoEnDistintosDias() {
        // A sigue 3x10 el lunes (bien) y 2x10 el viernes (bien): sin aumentos aunque el maximo semanal de A sea 3x10.
        var igual = List.of(c(MONDAY, 0, A, 3, 10), c(WEDNESDAY, 0, C, 3, 10), c(FRIDAY, 0, A, 2, 10),
                c(FRIDAY, 1, D, 3, 10));
        assertTrue(ComparacionDosisPorColocacion.comparar(MEDIDA, igual, Set.of(B)).sinAumentos());
        // A aparece ademas el miercoles a 3x10: sin correspondencia fiable, se compara con la menor (2x10) y sube.
        var otroDia = List.of(c(MONDAY, 0, A, 3, 10), c(WEDNESDAY, 0, C, 3, 10), c(WEDNESDAY, 1, A, 3, 10),
                c(FRIDAY, 0, D, 3, 10));
        var r = ComparacionDosisPorColocacion.comparar(MEDIDA, otroDia, Set.of(B));
        assertEquals(1, r.aumentos().size(), r.toString());
        assertEquals(Tipo.SIN_CORRESPONDENCIA, r.aumentos().get(0).tipo());
        assertEquals(2, r.aumentos().get(0).medida().series(), "contra la menor dosis medida, no contra el maximo");
    }

    @Test
    @DisplayName("6 · sustitucion por dolor: E ocupa el puesto de B (retirado) con su dosis; con mas, es un aumento")
    void sustitucionPorDolor() {
        var conSustituto = List.of(c(MONDAY, 0, A, 3, 10), c(MONDAY, 1, E, 3, 10), c(WEDNESDAY, 0, C, 3, 10),
                c(FRIDAY, 0, A, 2, 10), c(FRIDAY, 1, D, 3, 10));
        var r = ComparacionDosisPorColocacion.comparar(MEDIDA, conSustituto, Set.of(B));
        assertTrue(r.sinAumentos(), r.toString());
        assertEquals(1, r.porTipo().get(Tipo.SUSTITUCION));
        var conMas = List.of(c(MONDAY, 0, A, 3, 10), c(MONDAY, 1, E, 3, 12), c(WEDNESDAY, 0, C, 3, 10),
                c(FRIDAY, 0, A, 2, 10), c(FRIDAY, 1, D, 3, 10));
        var r2 = ComparacionDosisPorColocacion.comparar(MEDIDA, conMas, Set.of(B));
        assertEquals(Tipo.SUSTITUCION, r2.aumentos().get(0).tipo(), r2.toString());
        // Si B NO es un retirado (sin exclusion), E en su puesto es NUEVA: sin colocacion con la que compararla.
        var sinExclusion = ComparacionDosisPorColocacion.comparar(MEDIDA, conMas, Set.of());
        assertTrue(sinExclusion.sinAumentos());
        assertEquals(1, sinExclusion.porTipo().get(Tipo.NUEVA));
    }

    @Test
    @DisplayName("7 · adaptacion localizada: solo cambia una colocacion, a la baja; el resto identico")
    void adaptacionLocalizada() {
        var localizada = new ArrayList<>(MEDIDA);
        localizada.set(1, c(MONDAY, 1, B, 2, 10));
        var r = ComparacionDosisPorColocacion.comparar(MEDIDA, localizada, Set.of());
        assertTrue(r.sinAumentos());
        assertEquals(5, r.porTipo().get(Tipo.MANTENIDA));
    }

    @Test
    @DisplayName("8 · adaptacion expandida: varias sesiones a la baja y una colocacion retirada; ningun aumento")
    void adaptacionExpandida() {
        var expandida = List.of(c(MONDAY, 0, A, 2, 10), c(WEDNESDAY, 0, C, 2, 10), c(FRIDAY, 0, A, 2, 8));
        var r = ComparacionDosisPorColocacion.comparar(MEDIDA, expandida, Set.of(B));
        assertTrue(r.sinAumentos());
        assertEquals(2, r.retiradas(), "B y D no aparecen");
    }

    @Test
    @DisplayName("9 · regeneracion NO_FACTIBLE: si una colocacion sube, el motivo existe (P-03 no publica)")
    void regeneracionNoFactible() {
        var sube = List.of(c(MONDAY, 0, A, 3, 12), c(WEDNESDAY, 0, C, 2, 10), c(FRIDAY, 0, A, 2, 10),
                c(FRIDAY, 1, D, 2, 10));
        var r = ComparacionDosisPorColocacion.comparar(MEDIDA, sube, Set.of(B));
        assertFalse(r.sinAumentos());
        assertTrue(r.aumentos().get(0).describir().startsWith("P-03 nivel 2 (por colocacion, MANTENIDA)"));
    }

    @Test
    @DisplayName("10 · semana sin cambios: todo mantenido, ningun aumento")
    void sinCambios() {
        var r = ComparacionDosisPorColocacion.comparar(MEDIDA, MEDIDA, Set.of());
        assertTrue(r.sinAumentos());
        assertEquals(5, r.porTipo().get(Tipo.MANTENIDA));
        assertEquals(0, r.retiradas());
    }

    @Test
    @DisplayName("segundos y volumen: subir segundos o series x repeticiones tambien es un aumento")
    void segundosYVolumen() {
        var medida = List.of(new Colocacion(MONDAY, 0, A, 3, 0, 60));
        assertFalse(ComparacionDosisPorColocacion.comparar(medida, List.of(new Colocacion(MONDAY, 0, A, 3, 0, 90)),
                Set.of()).sinAumentos());
        assertFalse(ComparacionDosisPorColocacion.comparar(List.of(c(MONDAY, 0, A, 3, 10)),
                List.of(c(MONDAY, 0, A, 4, 8)), Set.of()).sinAumentos(), "4x8 sube series aunque baje repeticiones");
    }

    private static long total(List<Colocacion> s) {
        return s.stream().mapToLong(x -> (long) x.series() * x.reps()).sum();
    }
}
