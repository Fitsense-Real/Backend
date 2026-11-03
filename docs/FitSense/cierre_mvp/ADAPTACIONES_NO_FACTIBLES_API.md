# Adaptaciones no factibles: qué ve el cliente de la API

**Fecha:** 31/10/2026, §11–12 de la orden.
**Flutter no se modifica:** este documento le dice cómo interpretar la respuesta.

## 1. Antes de la corrección

| Lo que el cliente veía | Qué significaba |
|---|---|
| `WeeklyTrainingPlanResource.generationSource` | AI (la IA cambió algo), RULE_ENGINE o CONSERVED |
| `WeeklyTrainingPlanResource.adjustmentApplied` | El **nombre de la decisión** (ADAPT_LOCALIZED o ADAPT_EXPANDED), **también cuando no se cambió nada** |
| `adjustmentReason`, y `messageShown` en `/users/me/interventions` | El motivo del decisor, escrito **antes** de conocer el resultado (por ejemplo, «se corrige solo la peor sesión») |
| El estado V32 (APLICADA, SIN_EFECTO, RESPALDO, NO_FACTIBLE) | **No se exponía** |

**Consecuencia:** con CONSERVED y ADAPT_LOCALIZED, el cliente no podía distinguir entre:
- conservación deliberada;
- acortamiento imposible;
- reprogramación imposible;
- propuesta de la IA rechazada;
- IA no disponible.

Además, `adjustmentApplied = ADAPT_LOCALIZED` sobre la rutina de siempre **podía leerse como «ajustada»**: una sesión de 28 min presentada como ajustada a 22.

Un NO_FACTIBLE no publica semana, y el cliente solo veía que faltaba el plan.

## 2. La corrección mínima

Es compatible: solo añade.

**1. Campo nuevo `adaptation` en `WeeklyTrainingPlanResource`.**
- Lo devuelven `GET /users/me/plan/current` y `/users/me/plan/{id}`.
- Es `null` en una generación inicial.
- **No cambia ni quita** ningún campo existente.

**2. Endpoint nuevo `GET /users/me/plan/adaptations`.**
- Es de solo lectura y devuelve un resultado por ciclo.
- **Incluye los NO_FACTIBLE**, que no tienen plan.

**3. Solo traduce lo que V32 ya registra** (`AdaptationOutcome`), salvo un dato:
- **El motivo de la factibilidad previa.** Antes solo iba al log; ahora queda en `correcciones_del_backend` del informe V32.
- Es un campo JSON existente: **sin migración** y sin cambiar la semana publicada.
- Es lo único que no estaba persistido. Sin él, «imposible» y «la IA no estaba disponible» eran indistinguibles.

**Forma de `adaptation`** (y de cada elemento de `/adaptations`):

```json
{
  "attemptId": 17, "weekStartDate": "2026-11-09", "resultingPlanId": 18,
  "resultado": "ACORTAMIENTO_IMPOSIBLE", "estado": "RESPALDO", "decision": "ADAPT_LOCALIZED",
  "llamadasIa": 0,
  "motivos": ["ACORTAR de la sesion 15: con lo que V8C permite (en GAIN_MUSCLE, solo retirar un relleno) no baja de 28 min y la persona declaro 22"],
  "sesiones": [{ "origenPlannedWorkoutId": 15, "intervencion": "ACORTAR_O_REORGANIZAR", "resultado": "SIN_EFECTO" }]
}
```

## 3. Las seis situaciones de la orden

| Situación | `resultado` | `estado` (V32) | Plan publicado | Cómo debe presentarlo la app |
|---|---|---|---|---|
| 1 · Adaptación aplicada | `ADAPTACION_APLICADA` | APLICADA | Sí, con el cambio (`generationSource` = AI) | «Ajustamos tu sesión…» |
| 2 · Conservación deliberada | `CONSERVACION_DELIBERADA` | SIN_EFECTO | Sí, la misma (CONSERVED) | Sin mensaje de ajuste: se mantiene |
| 3 · Imposible por duración | `ACORTAMIENTO_IMPOSIBLE` (+ `motivos` con la cota) | RESPALDO, 0 llamadas | Sí, **la misma** | «No es posible acortarla a tus minutos sin quitar trabajo esencial; se mantiene». **Nunca** como ajustada |
| 4 · Reprogramación imposible | `REPROGRAMACION_IMPOSIBLE` | RESPALDO, 0 llamadas | Sí, la misma | «No hay otro día libre esta semana» |
| 5 · Respaldo por propuesta inválida de la IA | `RESPALDO_PROPUESTA_IA_INVALIDA` | RESPALDO, con 1 a 3 llamadas | Sí, la misma | «Se mantiene tu rutina» |
| 6 · No factible por seguridad | `NO_FACTIBLE_POR_SEGURIDAD` (+ `motivos`) | NO_FACTIBLE | **No** | Solo en `/adaptations`: «Esta semana necesita revisión por el dolor declarado» |

**Otros resultados posibles:**

| `resultado` | Cuándo |
|---|---|
| `SIN_CAMBIOS_VALIDOS` | La IA respondió sin cambios aplicables |
| `RESPALDO_SIN_IA` | IA apagada o cortacircuitos |
| `ADAPTACION_IMPOSIBLE` | Imposibilidad con intervenciones mezcladas |
| `NO_FACTIBLE` | Validación fallida, sin dolor |
| `PROGRESION_DEL_BACKEND` | Un paso de progresión del backend |
| `SIN_ADAPTACION` | Copia sin orden de adaptar |

**El resultado por sesión** (`sesiones[].resultado`): APLICADA, SIN_EFECTO, CONSERVADA, PROGRESADA o NO_PUBLICADA.

**Regla para el cliente:** **solo `ADAPTACION_APLICADA` y `PROGRESION_DEL_BACKEND` son cambios.** `adjustmentApplied` sigue existiendo por compatibilidad, pero no indica que algo cambiara.

## 4. Seguridad

Con dolor y sin alternativa segura, el resultado sigue siendo **NO_FACTIBLE**:
- **No** se conserva ni se publica como segura la sesión con el ejercicio problemático.
- La API no lo oculta: lo devuelve `/adaptations` con su motivo.

## 5. Pruebas

| Prueba | Qué comprueba |
|---|---|
| `AdaptationOutcomeTest` | Las seis situaciones y el resto de resultados, sin ambigüedad |
| `AdaptacionPorCausasIT` (por el controlador real) | CONSERVAR da CONSERVACION_DELIBERADA; ACORTAR aplicado da ADAPTACION_APLICADA; el acortamiento inviable da ACORTAMIENTO_IMPOSIBLE con la cota y la sesión SIN_EFECTO, igual en `/adaptations` |
| `AdaptacionCompletaIT.dolorSinIa` | NO_FACTIBLE_POR_SEGURIDAD en `/adaptations`, sin plan |

## 6. Lo que queda fuera (propuesta, no implementada)

- **`messageShown` de `/users/me/interventions`:** se escribe antes del resultado. Cambiarlo afecta a la tabla `user_interventions` (V6) y a la interpretación de la tesis, así que la app debe usar `adaptation.resultado`. Si se aprueba, se podría añadir el resultado a ese recurso.
- **Textos para la persona:** son decisión de producto. La API da códigos estables.
- **Una M nueva mayor** (para permitir reincorporar tras acortar con M) solo puede declararse hoy con otra omisión por falta de tiempo. Ofrecer una forma de actualizarla sin omitir sería una función nueva.
