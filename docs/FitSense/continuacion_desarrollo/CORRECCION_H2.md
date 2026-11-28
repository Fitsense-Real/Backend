# Corrección H2 · contrato prompt–V15 (PR-1.9, apagada por defecto)

| | |
|---|---|
| Rama | `fix/h2-contrato-prompt-v15` |
| Base | `generation` |
| Merge | **Sin fusionar**: PR borrador. La orden no autoriza fusionar automáticamente las PR funcionales |
| Interruptor | `fitsense.ai.generacion.contrato-v15` (variable de entorno `FITSENSE_AI_GENERACION_CONTRATO_V15`), por defecto **false** |

## Problema

Ver `DIAGNOSTICO_H2.md`: la IA recibe una cuota por grupo que no es la que verifica V15.

## Solución

Corresponde al diseño H2-B del expediente:

1. **`RepartoV15`** (dominio, nueva): la regla de la mitad en una sola función, `topeMismoGrupo(grupos, nTrabajo)`, y la tabla `tabla(grupos, maxTrabajo)`. V15 no cambia: la función la replica, y una prueba con el validador real fija que coinciden.
2. **`PlanInputSnapshot.of(context, contratoV15)`.** Con el interruptor encendido:
   - cada sesión de `suggested_split` lleva `max_per_body_part_por_n_trabajo`. Por ejemplo, LOWER_BODY con 8 colocaciones: `{"4":2,"5":2,"6":3,"7":3,"8":4}`. `max_per_body_part` pasa a `null`;
   - `constraints.complementos_no_cuentan` lleva los `exercise_id` que V15 no cuenta (`CatalogEligibilityPolicy.complementos`);
   - `min_exercises_per_session` nunca supera `max_colocaciones_dia` (C1) y es `null` cuando gobiernan las capacidades, porque se calculaba con `target_sets × target_reps` en lugar de las dosis de V24;
   - `prompt_version` = `PR-1.9`.
3. **`PlanPromptBuilder`.** Con el interruptor encendido sustituye dos frases: la regla 15 y la línea de la comprobación final. El nuevo texto dice la regla tal como la aplica V15: sin contar los complementos, ningún body_part puede tener más de la mitad de los ejercicios de trabajo de la sesión, según la tabla. Si no encuentra el texto histórico, **falla**, para que la variante no quede a medias.
4. **Coherencia:** la misma bandera llega a `ReplicateTrainingPlanGenerator`, a `WeeklyTrainingPlanCommandServiceImpl` (la instantánea guardada en `input_snapshot`) y a `AdaptedWeekCommandService`. La instantánea que se guarda es la que recibió la IA.

**Apagado**, el prompt y la instantánea son **idénticos byte a byte** a PR-1.7. Es la configuración evaluada (`ConfiguracionEvaluadaTest` e IT).

## Lo que no cambia

- V15, V20, V24, V25, V8C, D8, P-03, la progresión, la reincorporación y el catálogo.
- El motor de reglas, que no lee la instantánea.
- El esquema JSON de respuesta.
- No se hace ninguna llamada a Replicate.

## Pruebas

| Prueba | Qué fija | Resultado |
|---|---|---|
| `RepartoV15Test` (2) | Los topes y la tabla; usuario 39: `{4:2, 5:2, 6:3, 7:3, 8:4}`, nunca el 6 de PR-1.7 | ✅ |
| `ContratoV15PromptTest#apagadoIdentico` | Apagado: instantánea y prompt idénticos a PR-1.7 en los cuatro objetivos, sin campos nuevos | ✅ |
| `#encendidoSinContradicciones` | Encendido: C1 = C2 = C3 = 0. La tabla coincide con `RepartoV15` para cada n y los complementos con V15 | ✅ |
| `#sinMinimoConCapacidades` | Encendido: no se comunica el mínimo mal calculado cuando gobiernan las capacidades | ✅ |
| `#textoEncendido` | Encendido: el prompt ya no dice la regla histórica y sí la de V15 | ✅ |
| `#falloRapido` | Si el texto histórico cambia, la variante falla | ✅ |
| **`ContratoPromptV15IT`** (permanente) | Sonda con el **validador real**: la función predice exactamente cuándo V15 rechaza (n = 2…6, k = 1…n, con 2 complementos) | ✅ |
| Prompts y configuración existentes: `PromptCapacidadesTest`, `PromptRev2Test`, `PromptHistoricoHuellaTest` (huella de adaptación), `SessionFix*Test`, `ConfiguracionEvaluadaTest`/`IT`, `MinExercisesPerSessionTest`, `PlanInputSnapshotDosisTest`, `ContratoV4CIATest`, `PistasDeReintentoTest` | Nada cambia con el interruptor apagado | ✅ 161 unitarias + 4 IT, 0 fallos |

**Negativas:**

- con el interruptor apagado no aparece ningún campo nuevo;
- la variante falla si falta el texto histórico.

**Positivas:**

- la tabla coincide con V15;
- C1, C2 y C3 dan 0.

## Resultado

La contradicción se elimina por construcción cuando el interruptor está encendido, y la configuración evaluada no cambia mientras está apagado.

**Contradicción residual con el interruptor encendido: ninguna conocida.** La tabla cubre todos los n de 4 a `max_colocaciones_dia`.

## Decisión pendiente (propietario)

Activar PR-1.9 cambia lo que recibe la IA y, por tanto, la configuración evaluada de la tesis. Antes hace falta una evaluación pareada con la IA real, ya diseñada en el expediente §I.5:

- **Muestra:** 30 perfiles.
- **Brazos:** control PR-1.7 frente a tratamiento PR-1.9.
- **Coste:** USD 1,1–1,4 (tope USD 5).
- **Criterios de aceptación:**
  - 0 rechazos V15 por la regla de la mitad;
  - ninguna otra categoría de rechazo sube más de 1;
  - validez al primer intento no inferior al control;
  - latencia no peor en más del 10 %.

El protocolo está en `PLAN_PRUEBAS_IA_REAL.md`. **Necesita autorización expresa antes de gastar.**

## Pendientes

- Autorización y ejecución de la evaluación pareada.
- Si se adopta: activar la variable en el despliegue y actualizar la configuración evaluada.
- Más adelante, de forma opcional: que V15 llame a `RepartoV15`. Sería un cambio sin efecto en el comportamiento, con su propia regresión.
