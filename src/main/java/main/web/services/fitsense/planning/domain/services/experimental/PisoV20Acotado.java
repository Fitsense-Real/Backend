package main.web.services.fitsense.planning.domain.services.experimental;

import java.util.*;

/**
 * C2 CANDIDATA (EXPERIMENTAL, DESACTIVADA, SIN APROBAR). Evolucion minima de V20: el piso del 70 % deja de bloquear
 * un plan SOLO cuando V20 es la unica regla incumplida. No cambia 4A, 4C, 4D, V4, V24 ni ninguna otra validacion, y no
 * crea excepciones genericas: si aparece cualquier otro codigo (V4, V8C, V15, V18, V23, V24, V25, P-02, P-03...) la
 * politica no actua.
 * <p>
 * Modos:
 * <ul>
 *   <li>{@link Modo#REGENERAR}: se regenera con un piso menor, en pasos de {@code paso} puntos desde {@code inicial}
 *       hasta el minimo del objetivo (C2-A 50 %, C2-B 55 %, C2-C por objetivo).</li>
 *   <li>{@link Modo#MEJOR_ESFUERZO}: se conserva el piso del 70 % para generar y se acepta el MEJOR intento del
 *       constructor cuando la sesion esta SATURADA (no admite otro ejercicio ni otra serie sin romper una regla
 *       vigente). Es C2-D: el minimo lo fija el contenido, no un porcentaje.</li>
 * </ul>
 * No es un bean y la ruta de produccion no la referencia (prueba de aislamiento en {@code ModeloEExperimentalTest}).
 */
public record PisoV20Acotado(boolean habilitado, String version, Modo modo, int inicial, int paso,
                             Map<String, Integer> minimoPorObjetivo, int minimoPorDefecto) {

    public enum Modo { REGENERAR, MEJOR_ESFUERZO }

    public static final String VERSION = "C2-0.1-CANDIDATA-SIN-APROBAR";

    public PisoV20Acotado {
        if (inicial < 0 || inicial > 100 || paso <= 0) throw new IllegalArgumentException("piso o paso invalido");
        minimoPorObjetivo = Map.copyOf(minimoPorObjetivo);
    }

    /** C2-A: piso general minimo del 50 %. */
    public static PisoV20Acotado c2a() {
        return new PisoV20Acotado(false, VERSION + "/C2-A", Modo.REGENERAR, 70, 5, Map.of(), 50);
    }

    /** C2-B: piso general minimo del 55 %. */
    public static PisoV20Acotado c2b() {
        return new PisoV20Acotado(false, VERSION + "/C2-B", Modo.REGENERAR, 70, 5, Map.of(), 55);
    }

    /** C2-C: GAIN_MUSCLE 55 %; INCREASE_STRENGTH, LOSE_WEIGHT y GENERAL_FITNESS 65 %. */
    public static PisoV20Acotado c2c() {
        return new PisoV20Acotado(false, VERSION + "/C2-C", Modo.REGENERAR, 70, 5,
                Map.of("GAIN_MUSCLE", 55, "INCREASE_STRENGTH", 65, "LOSE_WEIGHT", 65, "GENERAL_FITNESS", 65), 65);
    }

    /** C2-D: el 70 % sigue siendo el objetivo; solo deja de bloquear en sesiones saturadas. */
    public static PisoV20Acotado c2d() {
        return new PisoV20Acotado(false, VERSION + "/C2-D", Modo.MEJOR_ESFUERZO, 70, 5, Map.of(), 0);
    }

    public int minimo(String objetivo) {
        return minimoPorObjetivo.getOrDefault(objetivo, minimoPorDefecto);
    }

    /** Pisos a probar despues del inicial, en orden: 65, 60, ... hasta el minimo del objetivo. */
    public List<Integer> secuencia(String objetivo) {
        if (modo == Modo.MEJOR_ESFUERZO) return List.of();
        var out = new ArrayList<Integer>();
        for (int p = inicial - paso; p >= minimo(objetivo); p -= paso) out.add(p);
        return List.copyOf(out);
    }

    /**
     * Condicion estricta: hay al menos un problema y TODOS son de V20. Se reconoce por el codigo al inicio del mensaje
     * del validador ("V20: ..."); cualquier otro codigo, o un problema sin codigo, la desactiva.
     */
    public static boolean soloV20(Collection<String> problemas) {
        return !problemas.isEmpty() && problemas.stream().allMatch(p -> p != null && p.trim().startsWith("V20:"));
    }

    /** No actua con una orden de volumen activa (reduccion D8/ACORTAR ya exime de V20) ni deshabilitada en produccion. */
    public boolean puedeActuar(Collection<String> problemas, boolean ajusteActivo, boolean laboratorio) {
        if (!habilitado && !laboratorio) return false;
        return !ajusteActivo && soloV20(problemas);
    }
}
