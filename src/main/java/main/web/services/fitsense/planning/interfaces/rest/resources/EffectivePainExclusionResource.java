package main.web.services.fitsense.planning.interfaces.rest.resources;

import java.time.LocalDate;

/**
 * O20: un ejercicio que HOY no se prescribe por dolor, y por que.
 *
 * @param origin                 {@code DIRECT}: tiene su propia exclusion (lo marco, o estaba en una sesion con dolor
 *                               sin identificar). {@code EQUIVALENT}: variante equivalente de uno con exclusion directa.
 *                               {@code PRECAUTION}: el mismo movimiento con otro angulo; no es equivalente, pero no se da
 *                               por seguro tras dolor.
 * @param confirmed              DIRECT: la persona marco o identifico ese ejercicio. EQUIVALENT: deriva de uno
 *                               confirmado.
 * @param provisional            EQUIVALENT o PRECAUTION de un dolor de sesion sin identificar (no confirmado). No
 *                               caduca sola. Siempre false en DIRECT.
 * @param confirmationRequired   Dolor de sesion sin identificar con dos semanas con plan cumplidas: sigue excluido y se
 *                               pide a la persona que confirme (identificar, declarar no relacionados o reactivar).
 * @param derivedFromExerciseId  EQUIVALENT: el ejercicio con exclusion directa del que deriva.
 * @param equivalenceLevel       EQUIVALENT: {@code VARIANTE_MISMO_EJERCICIO} o {@code MISMO_GESTO}; PRECAUTION:
 *                               {@code PRECAUCION_OTRO_ANGULO}.
 * @param equivalenceGroup       EQUIVALENT y MISMO_GESTO: el grupo de la lista explicita (p. ej. «G2-035 front raise»).
 * @param sourcePlannedWorkoutId DIRECT: la sesion de origen (para identificar despues que ejercicio fue).
 */
public record EffectivePainExclusionResource(Long exerciseId, String exerciseName, String origin, boolean confirmed,
                                             boolean provisional, boolean confirmationRequired,
                                             Long derivedFromExerciseId,
                                             String derivedFromExerciseName, String equivalenceLevel,
                                             String equivalenceGroup, String equivalenceJustification,
                                             Long sourcePlannedWorkoutId, LocalDate eventDate,
                                             boolean canIdentify, boolean canReactivate) {}
