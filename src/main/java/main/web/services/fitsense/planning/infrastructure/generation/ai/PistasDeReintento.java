package main.web.services.fitsense.planning.infrastructure.generation.ai;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * Orden de optimizacion (31/10/2026), fase D: cada motivo de rechazo apunta al dato del prompt que
 * lo resuelve. No anade reintentos ni relaja nada; solo dice DONDE esta la respuesta, que en las
 * 48 predicciones reales la IA no encontraba (V12 sin fechas libres, V20 sin segundos, V24 bajando
 * el descanso, V8C subiendo dosis por los principios de progresion).
 */
final class PistasDeReintento {

    private PistasDeReintento() {
    }

    /** Una pista por regla presente en los motivos, sin repetir y en el orden en que aparecen. */
    static List<String> para(List<String> motivos, boolean correccion) {
        var pistas = new LinkedHashSet<String>();
        for (var m : motivos) {
            if (m == null) continue;
            var pista = pista(m.strip(), correccion);
            if (pista != null) pistas.add(pista);
        }
        return List.copyOf(pistas);
    }

    private static String pista(String m, boolean correccion) {
        if (m.startsWith("V12"))
            return correccion ? "V12: pon la sesion en una de sus FECHAS LIBRES VALIDAS; ninguna otra fecha vale."
                    : "V12: una sola sesion por fecha.";
        if (m.startsWith("V18") && correccion)
            return "V18: elige el sustituto de SUSTITUTOS ADMITIDOS, que ya excluye lo de los dias pegados.";
        if (m.startsWith("V8C") && m.contains("sube"))
            return "V8C: deja esa dosis exactamente como estaba (lo que va antes de la flecha). En una correccion no se progresa.";
        if (m.startsWith("V8C") && correccion)
            return "V8C: sustituye solo por un ejercicio de SUSTITUTOS ADMITIDOS y no toques lo que la intervencion no permite.";
        if (m.startsWith("V24"))
            return correccion
                    ? "V24: usa la dosis de la linea DOSIS de esa sesion y deja rest_seconds como estaba."
                    : "V24: usa exactamente la dosis de clase_dosis del ejercicio (series, repeticiones y descanso); sin clase de hipertrofia, no lo uses en GAIN_MUSCLE.";
        if (m.startsWith("V20"))
            return correccion
                    ? "V20: mira DURACION de esa sesion; si no puedes llegar al minimo con lo que la intervencion permite, OMITELA."
                    : "V20: suma los segundos de DURACION CALCULADA (por clase de dosis) hasta pasar el minimo en segundos de cada sesion.";
        return null;
    }
}
