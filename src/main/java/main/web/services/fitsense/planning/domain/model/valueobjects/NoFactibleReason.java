package main.web.services.fitsense.planning.domain.model.valueobjects;

/**
 * CODIGO ESTABLE de por que una adaptacion semanal no pudo publicar una semana valida (orden de cierre definitivo
 * del backend, 02/11/2026, fase 2.1).
 * <p>
 * Lo asigna el codigo que TOMA la decision, en el mismo punto en que se escribe el texto tecnico, y se guarda en la
 * evidencia (V32 {@code reason_codes}, P-03 {@code reason_code}). Nunca se deduce despues leyendo el texto. No cambia
 * ninguna regla: solo nombra la que ya decidio.
 * <p>
 * El texto para la persona es neutro: no es un diagnostico ni afirma una causa medica. Los nombres son contrato de la
 * API: se pueden anadir valores, no renombrar ni quitar.
 */
public enum NoFactibleReason {

    /** P-03 nivel 1: un ejercicio excluido por dolor no tiene sustituto con la regla SUST-SEG-1. */
    SIN_SUSTITUTO_SEGURO("No encontramos un ejercicio seguro para sustituir uno que te causó molestias."),

    /** La alternativa (sustitucion determinista o semana regenerada) sube el trabajo de la semana medida. */
    TRABAJO_SUPERIOR_AL_PERMITIDO("La alternativa disponible aumentaba tu carga de trabajo, y tras una molestia no la aumentamos."),

    /** Habia dolor y no se pudo resolver de forma segura (sin propuesta valida ni sustitucion determinista). */
    DOLOR_SIN_RESOLUCION_SEGURA("No pudimos preparar una alternativa segura para la sesión en la que registraste molestias."),

    /** La semana propuesta no supera las validaciones del plan. */
    SEMANA_NO_VALIDA("La semana propuesta no cumplía todas las condiciones de tu plan."),

    /** La semana propuesta no tiene sesiones. */
    SEMANA_SIN_SESIONES("No fue posible preparar ninguna sesión para la semana."),

    /** P-03 nivel 2: el borrador regenerado volvia a traer un ejercicio excluido por dolor. */
    EXCLUIDO_REAPARECE("La alternativa disponible incluía un ejercicio que te causó molestias."),

    /** P-03 nivel 2: ya existe un plan activo para esa semana. */
    PLAN_YA_ACTIVO("Ya tienes un plan para esa semana."),

    /** P-03 nivel 2: con tu perfil y tus restricciones el motor de reglas no puede generar una semana. */
    SIN_SEMANA_GENERABLE("Con tus restricciones actuales no fue posible generar una semana válida."),

    /** P-03 nivel 2: no se encontro la semana medida con la que comparar el trabajo. */
    SEMANA_MEDIDA_NO_ENCONTRADA("No fue posible comparar con tu semana anterior."),

    /**
     * Factibilidad previa: ninguna adaptacion valida cabia en el tiempo o los dias declarados (se conserva la semana).
     * Se deriva del resultado ACORTAMIENTO_IMPOSIBLE / REPROGRAMACION_IMPOSIBLE / ADAPTACION_IMPOSIBLE.
     */
    DISPONIBILIDAD_NO_CUMPLIBLE("No fue posible ajustar la semana al tiempo o a los días disponibles que registraste.");

    private final String texto;

    NoFactibleReason(String texto) {
        this.texto = texto;
    }

    /** Texto comprensible y neutro para la persona. */
    public String texto() {
        return texto;
    }

    /** El codigo guardado, o null si es nulo o desconocido (una fila escrita por una version posterior). */
    public static NoFactibleReason of(String codigo) {
        if (codigo == null) return null;
        for (var r : values()) if (r.name().equals(codigo)) return r;
        return null;
    }
}
