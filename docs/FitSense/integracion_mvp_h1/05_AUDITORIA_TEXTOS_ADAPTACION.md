# 05 · Auditoría de los textos de adaptación

## 1. Qué texto ve hoy la persona (corrección de lo informado antes)

Se rastreó el flujo real en `generation`, con evidencia real (`docs/FitSense/evidencia/A01/evidencia.json`):

| Campo de la API | Endpoint | Valor real hoy (A01) |
|---|---|---|
| `adjustmentReason` (plan semanal) | `GET` del plan | «Adherencia observada 66.67 % (cobertura 100.00 %). Entre 50 % y 80 %: se corrige solo la peor sesion.» |
| `messageShown` (intervención) | `GET /api/v1/users/me/interventions` | El mismo texto |

**Origen:** `AdaptationOrderDecider` → `orden.reason()` → `sinAjustePorcentual(motivo)`. Ese motivo se usa a la vez como `reason` y como `message` de la decisión, y de ahí a `PlanAdjustment.reason()` y `UserIntervention.messageShown`.

**Corrección importante:**
- En el informe anterior (`decision_h1_h12/04`, PR #35) se dijo que la persona veía «…así que bajamos la exigencia». **No es así en el flujo actual.**
- Ese texto pertenece a `AdjustmentDecisionTable.message()`, la tabla porcentual que **ya no se llama**: `UserInterventionCommandServiceImpl` dice que se conserva solo para la comparación de la tesis.
- En `generation` y en la evidencia de #28–#33, «bajamos la exigencia» aparece **0** veces. Solo aparece en el documento del #35, que es el que se corrige aquí.

**El problema real es otro:**
- **(a)** La persona recibe un texto técnico, sin tildes y con jerga interna («se corrige solo la peor sesion»).
- **(b)** Ese texto describe la **decisión**, no lo que se publicó: dice «se corrige» aunque la sesión acabe SIN_EFECTO o conservada.
- **(c)** Los dos campos repiten lo mismo y ninguno informa del resultado.

## 2. ¿Puede publicarse una sustitución de dificultad mayor? Sí

| Pregunta (fase 4.1) | Respuesta | Prueba |
|---|---|---|
| 1. ¿Hay un escenario permitido en que una sustitución suba la dificultad? | **Sí.** En VARIANTE (TOO_DIFFICULT) y ante dolor, si el sustituto es elegible | `SustitucionDificultadMayorTest` 1 y 2 |
| 2. ¿V8C lo prohíbe? | **No.** En VARIANTE exige el mismo patrón (y, con H1, la misma distribución); no mira `difficulty_level`. Ante dolor ni siquiera exige el patrón | `SustitucionDificultadMayorTest` 1 (con las dos reglas) y 2 |
| 3. ¿Otro validador lo prohíbe? | **Solo** la elegibilidad (V1: el ejercicio tiene que estar en `available_exercises`, que ya filtra `max_difficulty_level` de la persona). Un intermedio (máx. 2) puede recibir 1 → 2. El prompt pide sustitutos de dificultad **≤**, pero es una instrucción, no un validador | Lectura de `ConservationValidator`, `SessionFixPromptBuilder` (filtro `difficulty() <= actual`) y el filtro de elegibilidad |
| 4. ¿El dolor tiene reglas distintas? | **Sí.** Ni el patrón ni la dificultad: sustituir o retirar, sin más ejercicios ni más volumen. El respaldo determinista **SUST-SEG-1** ordena por zona, patrón, músculo e id, **sin mirar la dificultad** | `SustitucionDificultadMayorTest` 3: elige 70002 (dif. 3) antes que 70003 (dif. 1) |
| 5. ¿Y si no hay comparación fiable? | Antes, la sustitución **no se mencionaba**. Ahora se dice «no tenemos un dato para comparar su dificultad». Pasa cuando falta la ficha de uno de los dos ejercicios en el catálogo de la semana (por ejemplo, el retirado ya no es elegible) | `DescripcionAdaptacionTest.sinComparacion` |
| 6. ¿Y con varias modificaciones de distinto tipo? | Cada pareja se clasifica por separado y cada clase lleva su frase. Antes, «retira 2 y añade 1» **no contaba la retirada sobrante** | `DescripcionAdaptacionTest.mixto` y `retiradaSobrante` |

En las 123 sustituciones reales (`validacion_h1_h12/04`) **ninguna** subió la dificultad. Es un caso posible, no observado.

**Prohibirlo sería un cambio funcional** (una regla nueva en V8C o en SUST-SEG-1), y **no se ha hecho**. Queda como decisión pendiente (`11`).

## 3. Los tres defectos del clasificador del PR #35, demostrados y corregidos en el PR B

| Defecto | Antes (#35) | Ahora (PR B) | Prueba |
|---|---|---|---|
| Dificultad mayor | Se contaba como **lateral**: «Cambiamos N ejercicios por otros **del mismo nivel de dificultad**» | `SUSTITUCION_MAYOR_DIFICULTAD`: «Cambiamos 1 ejercicio por otro registrado como de mayor dificultad.» | `mayorDificultad`, `mixto` (933 → 56) |
| Sin ficha | La sustitución **no se mencionaba** | `SUSTITUCION_SIN_COMPARACION` | `sinComparacion` |
| Retirada sobrante | Solo se contaba si no había añadidos | Lo retirado sin reemplazo es `RETIRADA` | `retiradaSobrante` |

La prueba `mixto` del #35 tenía justo el caso del primer defecto (933, dif. 1 → 56, dif. 2) y lo aceptaba como lateral.

## 4. Clasificación final (fase 4.2)

| Clase | Cuándo | Texto |
|---|---|---|
| Dificultad registrada menor | Mismo patrón, `difficulty_level` menor | «En N ejercicio(s) usamos una variante registrada como de menor dificultad.» |
| Dificultad registrada igual | Mismo patrón, igual | «Cambiamos N ejercicio(s) por otro(s) con la misma dificultad registrada: es una alternativa, no necesariamente más fácil.» |
| Dificultad registrada mayor | Mismo patrón, mayor | «Cambiamos N ejercicio(s) por otro(s) registrado(s) como de mayor dificultad.» |
| Sin comparación | Falta la ficha | «Cambiamos N ejercicio(s) por otro(s); no tenemos un dato para comparar su dificultad.» |
| Dosis reducida | Mismo ejercicio, menos series, repeticiones o segundos | «En N ejercicio(s) bajamos un poco el trabajo (menos series, repeticiones o tiempo).» |
| Seguridad | Sesión con SEGURIDAD_ANTE_DOLOR | «Por seguridad, cambiamos o retiramos N ejercicio(s). Si la molestia continúa, consulta a un profesional de la salud.» |
| Retirada | Retirado sin reemplazo | «Retiramos N ejercicio(s) de la sesión.» |
| Reprogramada | Otro día de la semana | «Movimos la sesión a otro día.» |
| Sin cambio efectivo | Señalada y no aplicada | «En una de las sesiones no aplicamos cambios esta semana: se mantiene como estaba.» |
| Conservada sin alternativa | Nota de factibilidad previa | «… no encontramos un cambio compatible con tu plan, así que por ahora se mantiene igual.» |

**Cómo se arma el texto:**
- Antes de las frases de cambio va una frase del motivo, sin promesa (por ejemplo, «Nos dijiste que una sesión te resultó difícil.»).
- Ante TOO_DIFFICULT, si **nada** baja lo registrado (ni dificultad ni dosis), se añade: «Si te sigue resultando difícil, indícalo de nuevo al registrar la sesión.»

**«Registrada» es deliberado:** el texto dice lo que dice el catálogo. Que un ejercicio tenga menos dificultad en el catálogo **no** significa que sea más fácil desde el punto de vista clínico o biomecánico (las fichas de 780 y 862 en #32 son ejemplos de dificultades dudosas).

## 5. Qué no cambia

El cambio solo toca dos textos:
- `adjustment_reason`, cuando se ordenó adaptar;
- `message_shown`, en las intervenciones nuevas.

**No cambian:**
- la orden ni la decisión: `orden.reason()` sigue siendo el `reason` técnico que cruza a planning y al snapshot;
- la adherencia;
- la propuesta de Replicate: el snapshot y el prompt no cambian, porque `WeeklyCycleService.toAdjustment` usa `order.reason()`, no `message`;
- el plan publicado, la dosis y los validadores;
- el esquema: sin migraciones; la columna `message_shown` sigue siendo VARCHAR(500);
- los textos ya guardados: no se reescriben.

Las pruebas que lo demuestran están en `06`.
