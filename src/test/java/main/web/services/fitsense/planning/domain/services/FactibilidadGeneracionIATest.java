package main.web.services.fitsense.planning.domain.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RENDIMIENTO · la cota inferior de {@link FactibilidadGeneracionIA}, con colocaciones sinteticas: es exacta y nunca
 * mayor que una sesion valida (si lo fuera, marcaria como imposible un caso posible y se perderia la IA).
 * <p>
 * Bits: capacidades exigidas primero (0..bitsCap-1), slots exigidos despues. 300 s de calentamiento, 15 s de
 * transicion.
 */
@DisplayName("Rendimiento · cota inferior de la comprobacion previa (V5 + V23 + V24 + V25)")
class FactibilidadGeneracionIATest {

    private static final long W = 300, T = 15;

    private static long cota(Map<Integer, Long> m, int caps, int slots, boolean v23) {
        return FactibilidadGeneracionIA.cotaInferior(m, caps, slots, v23, false, W, T);
    }

    @Test
    @DisplayName("una colocacion por capacidad: calentamiento + suma + transiciones")
    void sumaExacta() {
        var m = new HashMap<Integer, Long>();
        m.put(0b01, 100L);
        m.put(0b10, 200L);
        assertEquals(W + 100 + 200 + T, cota(m, 2, 0, false));
    }

    @Test
    @DisplayName("elige la mas corta de cada aportacion y aprovecha la que cubre capacidad y slot a la vez")
    void combinaAportaciones() {
        // 2 capacidades (bits 0,1) y 2 slots (bits 2,3).
        var m = new HashMap<Integer, Long>();
        m.put(0b0001, 100L);          // capacidad A sin slot
        m.put(0b0010, 100L);          // capacidad B sin slot
        m.put(0b0100, 150L);          // slot 1, capacidad secundaria
        m.put(0b1000, 150L);          // slot 2, capacidad secundaria
        m.put(0b0101, 180L);          // capacidad A en el slot 1
        m.put(0b1010, 180L);          // capacidad B en el slot 2
        // Con V23: A+slot1 (180) y B+slot2 (180) = 360 < 100+100+150+150.
        assertEquals(W + 360 + T, cota(m, 2, 2, true));
    }

    @Test
    @DisplayName("V23 solo cuenta si hay tantas colocaciones como slots: con menos, no se exigen")
    void v23ConPocasPosiciones() {
        // 1 capacidad, 4 slots exigidos. Sin colocaciones de slot baratas, lo minimo son 2 colocaciones (V5)
        // sin cubrir slots (2 < 4: V23 no bloquea).
        var m = new HashMap<Integer, Long>();
        m.put(0b00001, 60L);
        m.put(0b00000, 30L);
        m.put(0b00010, 500L);
        m.put(0b00100, 500L);
        m.put(0b01000, 500L);
        m.put(0b10000, 500L);
        assertEquals(W + 60 + 30 + T, cota(m, 1, 4, true));
        // Con ENFORCE_ALWAYS se exigen siempre.
        assertEquals(W + 60 + 4 * 500 + 4 * T,
                FactibilidadGeneracionIA.cotaInferior(m, 1, 4, true, true, W, T));
    }

    @Test
    @DisplayName("V5: con una sola capacidad exigida hacen falta dos colocaciones igualmente")
    void dosEjerciciosComoMinimo() {
        var m = new HashMap<Integer, Long>();
        m.put(0b1, 100L);
        assertEquals(W + 100 + 100 + T, cota(m, 1, 0, false), "la segunda puede repetir la mas corta (relajacion)");
    }

    @Test
    @DisplayName("si una capacidad exigida no tiene ninguna colocacion posible, no hay sesion valida: -1")
    void sinSolucion() {
        var m = new HashMap<Integer, Long>();
        m.put(0b01, 100L);
        assertEquals(-1, cota(m, 2, 0, false));
    }

    @Test
    @DisplayName("propiedad: la cota nunca supera ninguna combinacion valida enumerada por fuerza bruta")
    void nuncaMayorQueUnaSesionValida() {
        var r = new java.util.Random(20261112L);
        for (int caso = 0; caso < 300; caso++) {
            int caps = 1 + r.nextInt(3), slots = r.nextInt(4);
            boolean v23 = r.nextBoolean();
            int bits = caps + slots;
            var m = new HashMap<Integer, Long>();
            for (int k = 0; k < 6; k++) m.merge(r.nextInt(1 << bits), 30L + r.nextInt(400), Math::min);
            long c = cota(m, caps, slots, v23);
            long minimo = fuerzaBruta(m, caps, slots, v23);
            assertEquals(minimo, c, "caso " + caso + " " + m);
        }
    }

    /** Todas las multiconjuntas de hasta 6 colocaciones. */
    private static long fuerzaBruta(Map<Integer, Long> m, int caps, int slots, boolean v23) {
        var opciones = m.entrySet().stream().toList();
        long[] mejor = {Long.MAX_VALUE};
        recorrer(opciones, 0, 0, 0, 0, caps, slots, v23, mejor);
        return mejor[0] == Long.MAX_VALUE ? -1 : mejor[0];
    }

    private static void recorrer(java.util.List<Map.Entry<Integer, Long>> o, int desde, int mask, int n, long suma,
                                 int caps, int slots, boolean v23, long[] mejor) {
        int todasCap = (1 << caps) - 1, todosSlots = ((1 << slots) - 1) << caps;
        if (n >= 2 && (mask & todasCap) == todasCap
                && (!v23 || n < slots || (mask & todosSlots) == todosSlots))
            mejor[0] = Math.min(mejor[0], W + suma + (long) (n - 1) * T);
        if (n == 6) return;
        for (int i = desde; i < o.size(); i++)
            recorrer(o, i, mask | o.get(i).getKey(), n + 1, suma + o.get(i).getValue(), caps, slots, v23, mejor);
    }
}
