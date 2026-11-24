package main.web.services.fitsense.planning.interfaces.rest.resources;

import java.time.LocalDate;

/**
 * O20: un ejercicio que HOY no se prescribe por dolor, y por que.
 *
 * @param origin                 {@code DIRECT}: tiene su propia exclusion (lo marco, o estaba en una sesion con dolor
 *                               sin identificar). {@code EQUIVALENT}: variante equivalente de uno con exclusion directa.
 * @param confirmed              DIRECT: la persona marco o identifico ese ejercicio. EQUIVALENT: deriva de uno
 *                               confirmado.
 * @param provisional            EQUIVALENT de un dolor de sesion sin identificar: deja de aplicarse sola tras dos
 *                               semanas con plan. Siempre false en DIRECT.
 * @param derivedFromExerciseId  EQUIVALENT: el ejercicio con exclusion directa del que deriva.
 * @param equivalenceLevel       EQUIVALENT: {@code VARIANTE_MISMO_EJERCICIO} o {@code MISMO_GESTO}.
 * @param equivalenceGroup       EQUIVALENT y MISMO_GESTO: el grupo de la lista explicita (p. ej. «G2-035 front raise»).
 * @param sourcePlannedWorkoutId DIRECT: la sesion de origen (para identificar despues que ejercicio fue).
 */
public record EffectivePainExclusionResource(Long exerciseId, String exerciseName, String origin, boolean confirmed,
                                             boolean provisional, Long derivedFromExerciseId,
                                             String derivedFromExerciseName, String equivalenceLevel,
                                             String equivalenceGroup, String equivalenceJustification,
                                             Long sourcePlannedWorkoutId, LocalDate eventDate,
                                             boolean canIdentify, boolean canReactivate) {}
