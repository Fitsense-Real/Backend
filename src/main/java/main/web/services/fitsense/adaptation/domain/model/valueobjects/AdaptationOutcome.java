package main.web.services.fitsense.adaptation.domain.model.valueobjects;

/**
 * El desenlace de la decision de adaptacion.
 * <p>
 * Es un campo propio y no "un objetivo de volumen igual al anterior", y la
 * diferencia importa: {@code PlanAdjustment.isActive()} es
 * {@code targetVolume != null}, asi que un ajuste activo con variacion cero
 * seguiria disparando V8 con su banda del cinco por ciento. El desenlace separa
 * "no hay que cambiar nada" de "hay que cambiar un cero por ciento", que no es
 * lo mismo.
 */
public enum AdaptationOutcome {

    /**
     * Adherencia adecuada: se conserva la prescripcion anterior. Sin IA y
     * <b>sin progresion automatica</b>: la tabla antigua subia el volumen por
     * encima del umbral bueno, y eso contradice la regla de que solo se adapta
     * cuando hay baja adherencia.
     */
    NO_CHANGE,

    /** Baja adherencia: se corrige SOLO la peor sesion registrada. */
    ADAPT_LOCALIZED,

    /** Adherencia severamente baja: se abarcan todas las sesiones senaladas. */
    ADAPT_EXPANDED,

    /**
     * No hay evidencia suficiente: una parte demasiado grande de la semana no
     * tiene registro. <b>No se adapta, y no es un cero por ciento.</b> Era el
     * defecto que V30 hizo visible: la adherencia nula entraba como 0,0 y caia
     * en el tramo mas severo, de modo que quien no registro sus entrenamientos
     * recibia el mismo trato que quien los omitio todos.
     */
    INSUFFICIENT_DATA
}
