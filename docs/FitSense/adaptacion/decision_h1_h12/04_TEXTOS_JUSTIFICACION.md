# 04 · Textos de justificación de la adaptación

**Rama:** `feature/justificacion-adaptacion-verificable`, sobre la candidata H1.
**Tipo de cambio:** solo presentación.
- No cambia decisiones, validadores, permisos, la dosis, el plan ni el prompt.
- H12-A/B/C/D siguen apagadas.

## 1. El problema

El texto que ve la persona en el plan publicado (`adjustment_reason`, campo `adjustmentReason` de la API del plan semanal) se fija **antes** de la adaptación, en la tabla de decisión:

> «Cumpliste el NN % de tu plan. Nos dijiste que resultaba difícil, así que **bajamos la exigencia**.»

Se publica igual en todos estos casos:
- cuando la IA cambió 6 ejercicios de dificultad 1 por otros de dificultad 1 (R01, R05);
- cuando la sesión no cambió (SIN_EFECTO);
- cuando se conservó por falta de alternativas (E12).

**En esos casos el texto afirma una reducción que el sistema no puede demostrar.**

## 2. Lo implementado: el texto describe lo que cambió de verdad

`DescripcionAdaptacion.clasificar` compara, para cada sesión señalada:
- la sesión medida (`previousWeek`);
- la sesión publicada;
- el catálogo elegible (`availableExercises`).

Asigna una sola categoría por cambio, sin inferir nada del nombre:

| Categoría | Cuándo | Texto (por categoría con N > 0) |
|---|---|---|
| Reducción de dificultad registrada | Un ejercicio se cambia por otro **del mismo patrón con menor `difficulty_level`** | «En N ejercicio(s) usamos una variante de menor dificultad.» |
| Reducción de dosis registrada | El mismo ejercicio con **menos** series, repeticiones o segundos | «En N ejercicio(s) bajamos un poco el trabajo (menos series, repeticiones o tiempo).» |
| Sustitución lateral | Otro ejercicio del mismo patrón y **la misma dificultad** (o mayor) | «Cambiamos N ejercicio(s) por otro(s) del mismo nivel de dificultad: es una alternativa, no necesariamente más fácil.» |
| Sustitución por seguridad (dolor) | Cambio en una sesión con SEGURIDAD_ANTE_DOLOR | «Por seguridad, cambiamos o retiramos N ejercicio(s). Si la molestia continúa, consulta a un profesional de la salud.» |
| Retirada | Se quita sin añadir | «Retiramos N ejercicio(s) de la sesión.» |
| Reprogramada | La sesión cambia de día | «Movimos la sesión a otro día.» |
| Adaptación no aplicada | La sesión señalada se publica igual (SIN_EFECTO o CONSERVADA) | «En una de las sesiones no aplicamos cambios esta semana: se mantiene como estaba.» |
| Conservada por falta de alternativa compatible | Igual que la anterior, pero con nota de factibilidad previa (`FACTIBILIDAD_PREVIA:`) | «En una de las sesiones no encontramos un cambio compatible con tu plan, así que por ahora se mantiene igual.» |

**Estructura del texto:**
1. Se conserva el prefijo de adherencia «Cumpliste el NN % de tu plan.».
2. Una frase de motivo **sin promesa**. Por ejemplo, TOO_DIFFICULT da «Nos dijiste que una sesión te resultó difícil.».
3. Las frases de cada categoría.
4. Si el motivo es TOO_DIFFICULT y solo hubo laterales, no aplicadas o conservadas, se añade «Si te sigue resultando difícil, indícalo de nuevo al registrar la sesión.».

El texto nunca supera 500 caracteres, el límite de la columna.

### Ejemplos, con los casos reales

El porcentaje es ilustrativo (el de las pruebas). Las frases son exactamente las que produce el código, y las pruebas unitarias las fijan.

| Caso | Texto anterior | Texto nuevo |
|---|---|---|
| **R01** (6 laterales 1 → 1) | «Cumpliste el 33 % de tu plan. Nos dijiste que resultaba difícil, así que bajamos la exigencia.» | «Cumpliste el 33 % de tu plan. Nos dijiste que una sesión te resultó difícil. Cambiamos 6 ejercicios por otros del mismo nivel de dificultad: es una alternativa, no necesariamente más fácil. Si te sigue resultando difícil, indícalo de nuevo al registrar la sesión.» |
| **U04** (5 bajadas + 2 laterales) | igual que el anterior | «… Nos dijiste que una sesión te resultó difícil. En 5 ejercicios usamos una variante de menor dificultad. Cambiamos 2 ejercicios por otros del mismo nivel de dificultad: es una alternativa, no necesariamente más fácil.» |
| **R03** (2 × 10 → 2 × 9) | igual | «… En 1 ejercicio bajamos un poco el trabajo (menos series, repeticiones o tiempo).» |
| **E12** (SIN_EFECTO, sin alternativas) | igual | «… En una de las sesiones no encontramos un cambio compatible con tu plan, así que por ahora se mantiene igual. Si te sigue resultando difícil, indícalo de nuevo al registrar la sesión.» |

**Dónde se aplica:**
- `WeeklyTrainingPlanCommandServiceImpl.publishAdapted`, solo cuando la decisión ordena adaptar y hay ajuste y contexto.
- En cualquier otro caso (sin adaptación, progresión, regeneración) el texto es **el mismo de siempre**.

**Pruebas:**
- `DescripcionAdaptacionTest`: 9 unitarias (U04, R01, mixto, R03, no aplicada, conservada sin alternativa, dolor, sin sesiones señaladas, sin prefijo).
- `ConservacionPatronH1IT.justificacionVerificable`: integración. Comprueba que el `adjustment_reason` **publicado** ya no dice «bajamos la exigencia» y describe la bajada real.
- Resultados en §5.

## 3. Lo que NO se ha cambiado y queda como propuesta pendiente

Estos tres puntos cambian instrucciones del modelo o textos de la decisión, así que **no se implementan sin tu aprobación**.

### P-T1. El prompt dice a la IA algo falso sobre lo que ve la persona
`SessionFixPromptBuilder` (V1 y V2) dice:

> «change_reason y rationale se le muestran al usuario: escríbelos en español, en segunda persona…»

**No es cierto:**
- `rationale` y `change_reason` **no** se guardan ni se muestran;
- la persona solo ve `adjustment_reason`.

Además, ahí es donde la IA escribe «variantes más sencillas» cuando la dificultad no cambia (R01).

**Propuesta:**
- **(a)** Corregir la frase: «change_reason y rationale quedan en la traza para auditoría; no se muestran al usuario».
- **(b)** Pedir que no se califique un cambio como «más sencillo» si la dificultad del catálogo no baja.

**Efecto:** cambia el texto del prompt, lo que puede alterar las respuestas de la IA. Requiere aprobación y, si se quiere medir, una corrida con IA real (no autorizada en esta orden).

### P-T2. El mensaje de la intervención (`message_shown`) se fija antes de saber el resultado
`UserIntervention.messageShown` (tabla de V6, expuesto en `InterventionResource`) guarda `decision.message()`, es decir, el mismo «así que bajamos la exigencia». Se guarda en el momento de decidir, cuando aún no se sabe si habrá cambio.

**Propuesta:** que la tabla de decisión diga, para TOO_DIFFICULT, «Nos dijiste que resultaba difícil: revisaremos esa sesión para la próxima semana».

**Por qué no se implementa:**
- es el texto de la **decisión** (`AdjustmentDecisionTable`);
- es un registro persistido de la intervención;
- tiene pruebas propias.

Lo dejo para tu aprobación. No afecta a decisiones ni a la adherencia.

### P-T3. Textos de los demás motivos
Los mensajes de FATIGUE («bajamos el volumen y la carga»), LACK_OF_TIME («acortamos las sesiones») y PAIN tienen el mismo problema potencial: prometen un cambio antes de publicarlo.

En el plan publicado ya se corrigen con las frases de §2. Corregir su redacción en la tabla de decisión es la misma decisión que P-T2.

## 4. Cómo volver atrás

- Revertir el commit de esta rama, o no fusionarla. No hay migración, interruptor ni dato nuevo.
- Los planes ya publicados conservan el texto con el que se publicaron.

## 5. Regresión de esta rama

Ejecución local de `mvnw -o -B -ntp verify -Pintegration` sobre `1e01832`: código `4bd511a` sobre la candidata `79ec110`, con PostgreSQL efímero.

**Una omitida no es una superada.**

| Bloque | Ejecutadas | Superadas | Fallidas | Errores | Omitidas (*opt-in*) | Frente a la candidata |
|---|---:|---:|---:|---:|---:|---|
| Unitarias | 1.114 | 1.113 | **0** | **0** | 1 | +9 (`DescripcionAdaptacionTest`) |
| Integración | 496 | 459 | **0** | **0** | 37 | +1 (`ConservacionPatronH1IT.justificacionVerificable`) |

- Las omitidas son las mismas *opt-in* que en la candidata (`03` §1).
- El cambio solo afecta al texto `adjustment_reason`: no cambia el plan publicado, la decisión ni la adherencia.
- Por eso la comparación de 960 perfiles (solo generación) y la REPLAY (sin adaptación por IA) no dependen de él, y no se repitieron aquí.
- **CI:** ver el PR de esta rama.
