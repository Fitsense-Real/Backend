package main.web.services.fitsense.catalog.domain.model.valueobjects;

/**
 * O20: un ejercicio equivalente a otro y POR QUE lo es: {@code level} es
 * {@code VARIANTE_MISMO_EJERCICIO} (mismo nombre salvo {@code v. N}) o {@code MISMO_GESTO} (grupo {@code groupId} de
 * la lista explicita, con su {@code justification}).
 */
public record EquivalentExercise(Long exerciseId, String level, String groupId, String justification) {}
