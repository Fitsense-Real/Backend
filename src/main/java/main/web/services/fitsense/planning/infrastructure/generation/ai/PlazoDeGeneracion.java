package main.web.services.fitsense.planning.infrastructure.generation.ai;

import java.time.Instant;
import java.util.function.Supplier;

/**
 * RENDIMIENTO (12/11/2026) · EL PLAZO DEL PRESUPUESTO TOTAL, PARA LA LLAMADA EN CURSO.
 * <p>
 * {@code PlanGenerationPipeline} fija el instante a partir del cual la generacion deja de esperar a la IA y
 * {@link ReplicateTrainingPlanGenerator} lo lee al hacer el POST y el sondeo. Va en el hilo, no en la firma de
 * {@code requestOutput}, para no cambiar el contrato del generador (lo simulan decenas de pruebas y arneses de
 * evaluacion). La llamada es sincrona y en el mismo hilo; el plazo se retira siempre al terminar.
 */
public final class PlazoDeGeneracion {

    private static final ThreadLocal<Instant> PLAZO = new ThreadLocal<>();

    private PlazoDeGeneracion() {
    }

    /** Ejecuta {@code accion} con ese plazo (null = sin plazo) y deja el anterior al salir. */
    public static <T> T durante(Instant plazo, Supplier<T> accion) {
        var anterior = PLAZO.get();
        PLAZO.set(plazo);
        try {
            return accion.get();
        } finally {
            if (anterior == null) PLAZO.remove();
            else PLAZO.set(anterior);
        }
    }

    /** El plazo de la generacion en curso en este hilo, o null. */
    public static Instant actual() {
        return PLAZO.get();
    }
}
