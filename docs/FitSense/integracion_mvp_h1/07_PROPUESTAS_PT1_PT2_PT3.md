# 07 · P-T1, P-T2 y P-T3

## P-T1: la instrucción incorrecta del prompt (solo propuesta; no se ha tocado el prompt)

### Flujo real, comprobado en el código de `generation`

| Campo de la IA | Adaptación (`SessionFixPromptBuilder`) | Generación (`PlanPromptBuilder`) |
|---|---|---|
| `rationale` | `SessionFixFromJsonAssembler` exige que exista y luego **se descarta**: no está en `CorrectedSession` ni en el `output_snapshot` | Va a `PlanDraft.rationale`, que queda en `output_snapshot`. Se copia a `adjustment_reason` **solo si** `adjustment.isActive()` (`targetVolume != null`), lo que ya **no ocurre**: el mecanismo porcentual está fuera del flujo y `WeeklyCycleService.toAdjustment` deja `targetVolume` en null |
| `change_reason` (por sesión) | Va a `CorrectedSession.rationale` («para la traza; no se valida»). El plan publicado guarda `derived_workouts` (`DerivedWorkout`), que **no** lo incluye: **se descarta** | — |

**Lo que recibe Flutter:**
- `WeeklyTrainingPlanResource`: `adjustmentApplied`, `adjustmentReason` y `adaptation` (códigos por sesión), entre otros;
- `InterventionResource.messageShown`: `GET /api/v1/users/me/interventions`.

**Ninguno lleva el texto de la IA.**

**El prompt dice, sin embargo:**
- `SessionFixPromptBuilder` (V1): «change_reason y rationale **se le muestran al usuario**: escríbelos en español, en segunda persona…».
- `PlanPromptBuilder`: «rationale se le muestra al usuario…».

Las dos afirmaciones son falsas hoy.

### Propuesta (no aplicada)

**Sustituir en V1 y V2 por:**

> change_reason y rationale quedan en la traza del sistema para auditoría; no se le muestran al usuario. Escríbelos en español, breves y concretos: qué cambiaste y por qué. No califiques un cambio como «más sencillo», «más fácil» o «menos exigente» si el ejercicio nuevo no tiene una dificultad registrada menor (difficulty) en available_exercises; en ese caso descríbelo como una alternativa del mismo patrón.

**Cómo evaluarla, por separado y con autorización:**
- **(1)** Las pruebas de igualdad del prompt fallarían a propósito: hay que actualizarlas.
- **(2)** Una corrida con IA real C1/C1′ para medir que no cambian las tasas de aceptación ni de rechazo. Requiere Replicate, que no está autorizado.
- **(3)** Mientras no se mida, **no se aplica**: cambiar el texto del prompt puede cambiar las propuestas.

## P-T2: un mensaje de intervención que expresa intención (preparado y probado en el PR B)

**Antes:** `message_shown` = el motivo técnico de la orden («Adherencia observada 66.67 % (cobertura 100.00 %). Entre 50 % y 80 %: se corrige solo la peor sesion.»). Se escribe **al decidir**, antes de saber si la IA propondrá algo válido.

**Ahora:** `MensajeDeIntervencion.intencion(orden)`, en palabras de la persona y en futuro de intención:

| Orden | `message_shown` (nuevo) |
|---|---|
| ADAPT, TOO_DIFFICULT | «La semana pasada cumpliste el 67 % de lo previsto. Nos dijiste que una sesión te resultó difícil. Revisaremos qué ajustes son compatibles con tu entrenamiento. Si ningún cambio es compatible con tu entrenamiento, la mantendremos como está.» |
| ADAPT, dolor | «… Reportaste molestias. Revisaremos por seguridad los ejercicios de esa sesión. Si el dolor continúa, consulta a un profesional de la salud. …» |
| ADAPT, falta de tiempo | «… Nos dijiste que te faltó tiempo. Revisaremos si esa sesión puede acortarse sin cambiar tus días. …» |
| ADAPT, fatiga | «… Nos contaste que llegabas con cansancio. Revisaremos la carga de esa sesión. …» |
| ADAPT, desmotivación | «… Nos dijiste que te costó encontrar motivación. Buscaremos un cambio pequeño en esa sesión. …» |
| ADAPT, horario | «… Nos dijiste que cambió tu horario. Revisaremos el día de esa sesión. …» |
| ADAPT, sin causa | «… Una sesión no se completó y no registraste el motivo: si nos lo cuentas, podremos ajustarla. …» |
| Ampliada (varias) | «… Revisaremos las N sesiones que nos indicaste.» + una frase por motivo, sin repetir |
| NO_CHANGE | «La semana pasada cumpliste el 100 % de lo previsto. Es un buen cumplimiento: mantenemos tu plan.» |
| INSUFFICIENT_DATA (registro) | «No tenemos registros suficientes para ajustar tu plan, así que lo mantenemos. Registra tus sesiones para que podamos adaptarlo.» (sin porcentaje, porque no es fiable) |
| INSUFFICIENT_DATA (sin sesión señalada) | «Mantenemos tu plan: no encontramos una sesión concreta que revisar.» |

**Garantías, con pruebas en `06`:**
- **El `reason` técnico no cambia:** `sinAjustePorcentual(motivo, mensaje)` conserva `reason = motivo` y solo cambia `message`. Planning solo lee `order.reason()` (`WeeklyCycleService.toAdjustment`), así que **ni el snapshot ni la entrada de Replicate ni ninguna decisión cambian**.
- **Ninguna frase promete un resultado:** ni «bajamos», «acortamos», «subimos», «reducimos» o «cambiamos», ni «más fácil» o «sencillo». Se prueba para los 9 tipos de intervención y para su combinación.
- **Cabe en `message_shown`:** máximo 500 caracteres. Sin migración.
- **Las intervenciones ya guardadas no se reescriben:** solo cambian las nuevas.

## P-T3: decisión, procesamiento y resultado

**Tres momentos, tres textos, sin contradicción:**

| Momento | Campo | Contenido | Quién lo escribe |
|---|---|---|---|
| **Decisión** | `user_interventions.message_shown` (API: `/interventions`) | Intención: «Revisaremos…», «Mantenemos…» | `MensajeDeIntervencion` (PR B) |
| **Procesamiento** | `plan_adaptation_attempts` (V32: `decision`, `status`, `applied/no_effect/conserved_sessions`, `rejection_reason`, `conservation_report`) y el `reason` técnico | Traza técnica para auditoría, que no ve la persona | Sin cambios |
| **Resultado** | `weekly_training_plans.adjustment_reason` (API del plan) | Lo que se publicó: «usamos una variante registrada como de menor dificultad», «no aplicamos cambios»… | `DescripcionAdaptacion` (PR B) |

**Redacciones revisadas por motivo:**
- **Fatiga, falta de tiempo, dolor, dificultad y desmotivación** tienen una frase de intención en P-T2 y una de motivo y resultado en `DescripcionAdaptacion`. Ninguna promete: «Nos contaste que llegabas con cansancio.» + lo que cambió.
- **Los textos de `AdjustmentDecisionTable.message()`** («bajamos el volumen y la carga», «acortamos las sesiones», «bajamos la exigencia», «subimos un poco el volumen») sí prometían. Pero esa tabla **no está en el flujo**, así que no llegan a nadie. **No se han tocado:** es el mecanismo de comparación de la tesis. Si algún día vuelve al flujo, habría que aplicarle el mismo criterio (`11`).

**Contradicción entre campos:** antes, los dos campos repetían «se corrige solo la peor sesion» aunque el resultado fuera SIN_EFECTO. Ahora uno dice «revisaremos» y el otro lo que pasó (por ejemplo, «no aplicamos cambios esta semana»). La IT `AdaptacionCompletaIT.cicloCompleto` lo comprueba con el ciclo real (`06`).
