package main.web.services.fitsense.planning.domain.services;

import java.util.Locale;

/**
 * Como se trata un incumplimiento de la dosis de 4D v2 en el primer perfil del
 * MVP. Se configura con {@code fitsense.dose.enforcement}.
 * <p>
 * Mismo patron que {@link SlotCoverageEnforcement}, y por el mismo motivo: la
 * implementacion entra entera y empezar a rechazar planes es una decision del
 * estudio, no un efecto de haber escrito la cuenta.
 * <p>
 * PERO SIN ASIMETRIA POR ORIGEN. {@code ENFORCE_AI_ONLY} existe en cobertura
 * porque el motor de reglas no conoce patrones y rechazarlo dejaria semanas sin
 * plan. Aqui no hace falta: el motor de reglas SI puede cumplir la dosis, y la
 * condicion de Juan es que se adapte ANTES de activar el rechazo. Por eso solo
 * hay un modo que bloquea, y bloquea a los dos generadores.
 */
public enum DoseEnforcement {

    /** No mide ni rechaza. Para no pagar el calculo donde no interesa. */
    OFF,

    /**
     * Mide y registra, no rechaza. ES EL VALOR POR OMISION, y es con el que hay
     * que medir la primera ronda: dice si el motor de reglas ya cumple la dosis
     * antes de que su incumplimiento deje a alguien sin plan.
     */
    OBSERVE,

    /**
     * Rechaza cualquier borrador que incumpla, sea de IA o de reglas.
     * <p>
     * NO ACTIVAR hasta comprobar en OBSERVE que el motor de reglas produce un
     * plan conforme. Un borrador de reglas rechazado termina en
     * {@code PlanGenerationFailedException}: semana sin plan y adherencia NULL.
     * Con el proveedor caido, eso le pasaria a todo el mundo.
     */
    ENFORCE;

    public boolean mide()    { return this != OFF; }
    public boolean bloquea() { return this == ENFORCE; }

    /**
     * Tolerante con el valor de la propiedad. Ante la duda NO bloquea: ni un
     * valor ausente ni uno mal escrito deben empezar a rechazar planes por su
     * cuenta. Quien quiera bloquear lo escribe.
     */
    public static DoseEnforcement parse(String valor) {
        if (valor == null || valor.isBlank()) return OBSERVE;
        try {
            return valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return OBSERVE;
        }
    }
}
