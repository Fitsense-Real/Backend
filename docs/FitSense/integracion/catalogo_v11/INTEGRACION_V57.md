# Integración de V57 (#68): constancia de las omisiones impuestas por el sistema

## Problema

Un ejercicio marcado por seguridad en una sesión publicada solo puede registrarse como omitido. Las causas posibles son:

- dolor registrado (P-DOLOR-PUB);
- una restricción del perfil;
- una contención.

El motivo de la fila (`skip_reason`) lo elige el participante, así que no distingue esa omisión de una voluntaria.

**Antes de V57:**

- solo la omisión por dolor se podía reconstruir, a partir de las exclusiones, y dependía de la configuración del momento;
- V54 solo dejaba la causa en la sesión que **vence** bloqueada;
- una sesión bloqueada que el participante **omite a mano** no dejaba ninguna constancia.

## Solución (rama `propuesta/traza-omision-sistema`)

| Pieza | Qué hace |
|---|---|
| **Migración V57** | `ALTER TABLE workout_session_exercises ADD COLUMN system_block_cause VARCHAR(200)`. Las filas existentes quedan a NULL: no se reclasifica nada sin evidencia |
| **Ejercicio** (`WorkoutSessionCommandServiceImpl.anotarBloqueo`) | Al registrar un ejercicio, sea uno a uno o al terminar la sesión, guarda en su fila la causa de seguridad vigente. Es la misma cadena que devuelve la API, por ejemplo `dolor_registrado` o `requires_floor+dolor_registrado`. Es NULL si no había marca |
| **Sesión** (`WeeklyTrainingPlanCommandServiceImpl.anotarBloqueoDeSistema`) | Guarda `planned_workouts.system_block_cause` (`ejercicio=causa; …`) en dos momentos: al **vencer** (V54, comportamiento sin cambios, solo refactorizado) y, nuevo, al **omitir a mano** una sesión que tiene marcas de seguridad. Se anota antes de `markSkipped` |
| **Lo que no cambia** | El estado, `skip_reason`, la fórmula de adherencia, los pesos, el denominador, los umbrales, la cobertura, el riesgo y las reglas de adaptación. Ningún cálculo lee las columnas nuevas |
| **`SOLO_ESQUEMA`** | Añade la línea «57» después de V54 y V55, para la comprobación del esquema vigente sobre el catálogo anterior |

### Compatibilidad con V54

- V57 es la migración siguiente a V56. No altera V54 y reutiliza la columna `planned_workouts.system_block_cause` que V54 creó.
- El camino de vencimiento sigue produciendo la misma cadena. `RegeneracionPorColocacionRev2IT` y `Plan78ContencionReducidaIT` lo comprueban.

## Clasificación que permiten los registros

| Caso | Cómo se reconoce |
|---|---|
| Ejercicio omitido voluntariamente | `workout_session_exercises.status = SKIPPED` y `system_block_cause IS NULL` |
| Ejercicio que el sistema impidió hacer por dolor | `system_block_cause` contiene `dolor_registrado` |
| Ejercicio bloqueado por otra restricción | `system_block_cause` con una causa distinta, como `requires_floor`, `requires_hanging`, `overhead` o la contención |
| Sesión entera bloqueada | `planned_workouts.system_block_cause` con alguna causa distinta de `dolor_registrado`. `blockedForSafety` bloquea la sesión ante cualquier conflicto que no sea dolor |
| Sesión que vence después de quedar bloqueada | `status = SKIPPED`, `skip_reason IS NULL` y `system_block_cause` no nulo (V54) |
| Sesión omitida a mano estando bloqueada | `status = SKIPPED`, `skip_reason` declarado y `system_block_cause` no nulo (V57) |
| Sesión con marcas solo de dolor, vencida u omitida | `system_block_cause` contiene únicamente `…=dolor_registrado`. La sesión **no** estaba bloqueada: el resto de ejercicios se podía hacer |

**Si la restricción desaparece después,** la causa se conserva, porque es la vigente en el momento del registro. `Plan78ContencionReducidaIT#omisionManualDeSesionBloqueada` lo comprueba: tras la omisión permite colgarse y los brazos arriba en el perfil, y la causa sigue en la fila.

**Limitación documentada:** si el participante **vuelve a registrar** el mismo ejercicio, la fila refleja el último registro, porque el registro sobrescribe. Si para entonces la restricción ya no existe, la causa pasa a NULL. Es coherente: en ese momento el ejercicio ya podía hacerse.

## Pruebas

| Prueba | Qué fija |
|---|---|
| `Plan78ContencionReducidaIT` (5) | Incluye `omisionManualDeSesionBloqueada`, nueva: la sesión 1 está bloqueada (`1303=INC-CAT-C39`) y la 2 libre. Las dos se omiten con OTHER: la 1 queda SKIPPED, OTHER y con la causa; la 2, SKIPPED, OTHER y NULL. La causa se conserva al cambiar el perfil |
| `RegeneracionPorColocacionRev2IT` | El ejercicio con dolor registrado y omitido en curso guarda `dolor_registrado`. Ninguna otra fila de la sesión lleva causa |
| `ProteccionPlanesPublicadosIT`, `EjecucionEntrenamientosIT`, `Usuario31ReproduccionIT`, `RetiradaLocalizadaLimiteIT` y `CatalogoCalidadIT` | El registro, la ejecución, la adherencia y el catálogo no cambian |
| Regresión completa (CI) y `EsquemaVigenteSobreCatalogoAnterior` | El esquema con V57 aplicado sobre el catálogo anterior |

## Resultado

**Integrada.** [#68](https://github.com/Fitsense-Real/Backend/pull/68) se fusionó en `generation` → **`0d112785a5df70d686da36c80e814b40ca077cd6`**. El árbol es idéntico al de su cabeza probada `b69af7f`.

Condiciones que se cumplían antes de fusionar:

1. Rama actualizada con `generation` `50849d0` mediante un merge sin conflictos.
2. Regresión del catálogo integrado en verde (`REGRESION_POST_MERGE.md`).
3. CI de la cabeza en verde: la suite completa sobre `generation` + V57.
4. GitHub la daba por fusionable sin conflictos.

| Verificación | Resultado |
|---|---|
| CI de GitHub en `b69af7f` | **Verde** (run 43724462786, 14:27–14:43 UTC) |
| Pruebas de integración dirigidas, en local sobre `0d11278` | **114 pruebas, 0 fallos, 1 omitida (opt-in)**. Clases: `Plan78ContencionReducidaIT` (5), `RegeneracionPorColocacionRev2IT` (6), `ProteccionPlanesPublicadosIT`, `EjecucionEntrenamientosIT`, `Usuario31ReproduccionIT`, `RetiradaLocalizadaLimiteIT`, `CatalogoCalidadIT`, `ContencionTextoBanderaIT`, `PoliticasP123IT` (39) y `FitsenseApplicationIT` |
| Migraciones | 60, de V1 a V57. Cada IT las aplica desde una base vacía. V57 sobre el esquema anterior: comprobación `SOLO_ESQUEMA` |
| Catálogo | Sin cambios. `CatalogoCalidadIT` pasa con la huella permanente |
| Adherencia | Sin cambios. Ningún cálculo lee las columnas nuevas. «5 · lo que recibe la adherencia» de `EjecucionEntrenamientosIT` pasa |

**Suficiencia:** con V57, las cinco situaciones que pide la orden se distinguen en los registros persistidos, y la causa se conserva aunque la restricción desaparezca después. La única limitación es la de volver a registrar un ejercicio, descrita arriba.

## Propuesta metodológica

**Se necesita una decisión antes de recoger datos reales.**

El indicador oficial de adherencia **no cambia**. Con V57, las omisiones impuestas se pueden separar a partir de los mismos registros. Las opciones son:

| Opción | Qué implica | Recomendación |
|---|---|---|
| **A** | Informar el indicador oficial **y**, como análisis secundario **preregistrado**, el mismo indicador sin las omisiones impuestas en el denominador. Son las filas o sesiones con `system_block_cause` no nulo y una causa distinta de un dolor que el propio participante registró en esa sesión | **Recomendada** |
| **B** | Solo el indicador oficial, declarando la limitación | Aceptable |
| **C** | Cambiar el indicador oficial | **No autorizada**: modifica la metodología |

**Para la opción A hay que decidir y dejar escrito antes de recoger datos:**

1. Si el dolor cuenta como impuesto. Recomendación: un ejercicio marcado por un dolor **registrado antes** de la sesión es una omisión impuesta. El dolor que se declara en la propia sesión es un dato del participante.
2. Si una sesión con marcas solo de dolor y el resto de ejercicios disponibles cuenta como omitida voluntaria. Recomendación: sí, excepto los ejercicios marcados.
3. Si se informan por separado los registros anteriores a V57, que tienen la causa a NULL y no se pueden clasificar. Recomendación: excluirlos del análisis secundario y decirlo.

## Pendientes

- La decisión A/B, firmada antes de recoger datos.
- Flutter no muestra la causa. No hace falta para el análisis, porque está en la base de datos.
