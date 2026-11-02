package main.web.services.fitsense.planning.domain.model.valueobjects;

/**
 * De donde salio el plan de la semana.
 * <p>
 * La proporcion de RULE_ENGINE frente a AI es un dato reportable de la tesis
 * (20.6): si es alta, indica un modelo que incumple mucho las validaciones y un
 * sistema que depende del respaldo determinista.
 * <p>
 * CONSERVED (V32) existe precisamente para no contaminar esa cifra. Una semana
 * copiada de la anterior con fechas nuevas -el respaldo seguro, y los desenlaces
 * NO_CHANGE e INSUFFICIENT_DATA- no la genero el modelo ni el motor de reglas.
 * Contarla como RULE_ENGINE inflaria la cifra con semanas en las que el motor de
 * reglas no hizo nada.
 */
public enum GenerationSource {
    AI,
    RULE_ENGINE,
    CONSERVED
}
