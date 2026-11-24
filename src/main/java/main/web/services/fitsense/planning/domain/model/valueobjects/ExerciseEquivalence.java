package main.web.services.fitsense.planning.domain.model.valueobjects;

/**
 * O20, del lado de planning: un ejercicio equivalente a otro que dolio y por que. {@code level} es
 * {@code VARIANTE_MISMO_EJERCICIO} (mismo nombre salvo {@code v. N}) o {@code MISMO_GESTO} (grupo {@code groupId} de la
 * lista explicita del catalogo, con su {@code justification}).
 */
public record ExerciseEquivalence(Long exerciseId, String level, String groupId, String justification) {}
