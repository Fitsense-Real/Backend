package main.web.services.fitsense.planning.domain.model.valueobjects;

/**
 * La decision que tomo el modulo de adaptacion, vista desde {@code planning}.
 * <p>
 * EXISTE PORQUE {@code planning} NO IMPORTA DE {@code adaptation}. El enum
 * equivalente de alla -{@code adaptation...AdaptationOutcome}- no puede cruzar
 * el limite, y el puente entre los dos modulos es {@link PlanAdjustment}, que
 * lleva las sesiones senaladas pero no la decision. Quien llama la traduce una
 * vez, en el borde.
 * <p>
 * Los nombres son LITERALMENTE los valores que admite {@code ck_paa_decision}
 * en V32. No es casualidad ni comodidad: si alguien anade aqui un valor que la
 * restriccion no admite, la insercion falla en el primer intento real en vez de
 * en una revision.
 */
public enum AdaptationDecision {

    /** Hubo datos y no hubo baja adherencia. No se adapta nada. */
    NO_CHANGE,

    /**
     * No hay cobertura de registro suficiente para afirmar que hubo baja
     * adherencia. NO es un visto bueno: es que no se sabe.
     */
    INSUFFICIENT_DATA,

    /** Baja adherencia localizada: se senalan sesiones concretas. */
    ADAPT_LOCALIZED,

    /** Baja adherencia extendida: se senalan mas sesiones, en una sola pasada. */
    ADAPT_EXPANDED;

    /** Si esta decision ORDENA adaptar. V32 ata el estado a esto. */
    public boolean ordenaAdaptar() {
        return this == ADAPT_LOCALIZED || this == ADAPT_EXPANDED;
    }
}
