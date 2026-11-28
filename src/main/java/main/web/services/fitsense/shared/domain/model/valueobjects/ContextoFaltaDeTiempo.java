package main.web.services.fitsense.shared.domain.model.valueobjects;

/**
 * Respuesta opcional a «¿sueles tener el tiempo previsto para este entrenamiento?» tras omitirlo
 * por {@link SkipReason#LACK_OF_TIME} (DISENO_ADAPTACION_CONTEXTUAL.md v0.3, §1).
 * <p>
 * La omision es un hecho; la causa y este contexto son informacion declarada. Sin respuesta
 * (null) no se infiere nada: la sesion se conserva (D1).
 */
public enum ContextoFaltaDeTiempo {
    /** Fue algo puntual: se conserva la sesion. */
    IMPREVISTO,
    /** Ese dia no hubo ningun minuto: recortar cinco minutos no lo resuelve; se conserva. */
    SIN_TIEMPO_ESE_DIA,
    /**
     * Suele tener menos tiempo para ESTE entrenamiento, con los minutos declarados M. Si la sesion
     * cabe en M se conserva; si no, se acorta hasta M como maximo.
     */
    MENOS_TIEMPO_HABITUAL
}
