# Diseño · Adaptación mínima justificada por la causa

| | |
|---|---|
| Versión | **v0.3 · cerrada.** Incorpora las decisiones aprobadas y las verificaciones (`VERIFICACION_DISENO_V03.md`), sin rediseño. La v0.1 (`46fb26d`), la v0.2 (`6ffcf20`) y la v0.3 de revisión (`6120808`) quedan en el historial |
| Fecha | 27/10/2026 |
| Estado | Diseño cerrado. **ADAPT-2 no está autorizado para implementarse**: antes va la prueba de restauración (§8, caso 11). Solo se implementó la corrección D8 (`aeb09ab`) |

## Principio

> **FitSense ayuda a la persona a mantener y cumplir su rutina personalizada; no le cambia la rutina cada vez que tiene una dificultad.**

- **La personalización inicial** la hace la IA a partir del perfil.
- **La adaptación posterior** responde a un problema concreto de ejecución, con el **cambio mínimo** que su causa permite, sobre la sesión afectada. El resto del plan no cambia.
- **La omisión es un hecho; la causa es información declarada; la intervención es una decisión justificada por ambas.** Una omisión no implica por sí sola reducir el entrenamiento, y la falta de información tampoco autoriza un recorte.

**El MVP demuestra:**
1. un plan inicial personalizado con IA;
2. la medición de la adherencia y la detección de cuándo intervenir;
3. ajustes mínimos y justificados sobre el plan existente, con su resultado evaluado.

**Fuera del MVP:** regenerar la semana o la rutina, la progresión automática, los cambios globales del perfil y los cuestionarios adicionales.

### Cambios respecto de la v0.2

| Observación | Resolución en la v0.3 |
|---|---|
| D1–D4 | D1 y D4 aprobadas. D2 se precisa sin romper V32 y V33 (§6.1). D3 se aprueba con evidencia positiva (§3.3) |
| 1. V31 de un paso recupera la versión equivocada | Recorrido V31 + V32 hasta la última adaptación restaurable; inequívoco por las restricciones existentes (§3.2) |
| 2. Ausencia de causa ≠ recuperación | Se restaura solo con **ejecución registrada y completada, sin señales incompatibles**. Sin información, el ajuste se mantiene y la restauración queda pendiente (§3.3) |
| 3. Qué significan los minutos | M es la disponibilidad **para este entrenamiento**, no para todos (§1) |
| 4. Indicador | A_s entre 0 y 1; sesiones bajo el 30 %; n_r = 0 no calculable; tasas de finalización y de realización separadas (§4) |
| Verificación V32 | Conservar por causa nunca es `APLICADA`; se mantiene la equivalencia de V33 (§6.1) |
| Versionado | `adaptation_rule_version` en la evidencia V32, enlazada con la intervención (§6.2) |

---

## 1. ¿Cuándo adaptar?

**Sin cambios.** Decide el decisor actual, con sus umbrales:
- cobertura menor de 50 % → `INSUFFICIENT_DATA`;
- observada de 80 % o más → `NO_CHANGE`;
- de 50 % a menos de 80 % → `ADAPT_LOCALIZED`;
- menos de 50 % → `ADAPT_EXPANDED` (pospuesto).

La causa registrada de cada sesión señalada elige la intervención (§2).

**Lo único nuevo es una pregunta opcional, solo al omitir con «No tuve tiempo».** Si no responde, la omisión queda registrada igual.

> ¿Sueles tener el tiempo previsto para **este entrenamiento**?
> - **Sí, fue algo puntual** → `IMPREVISTO`
> - **No, suelo tener menos** → «¿Cuántos minutos sueles tener disponibles **para este entrenamiento**?» → `MENOS_TIEMPO_HABITUAL` + M

**Qué significa M:**
- La disponibilidad habitual para **esta sesión de la semana** (por ejemplo, la del viernes).
- **No** es la disponibilidad general: no cambia el perfil ni las demás sesiones.
- La escribe la persona; no se infiere de la sesión omitida.
- Su rango es el que ya admite el perfil, de 15 a 180 minutos.

Sesiones parciales: sin pregunta en el MVP (D4).

---

## 2. ¿Qué puede adaptarse?

Solo la sesión señalada, y solo el cambio mínimo que su intervención permite. Las demás sesiones se conservan idénticas (V8C).

| Causa (y respuesta) | Intervención | ¿Se llama a la IA? | Qué puede cambiar | Si no es factible | Tras la adaptación | Regla |
|---|---|---|---|---|---|---|
| Falta de tiempo · imprevisto | **`CONSERVAR`** | No | Nada | No aplica | Nada que restaurar | Nueva |
| Falta de tiempo · menos tiempo, con M **≥** la duración prevista | **`CONSERVAR`** | No | Nada: la sesión ya cabe en M | No aplica | Nada que restaurar | Nueva |
| Falta de tiempo · menos tiempo, con M **<** la duración prevista | `ACORTAR_O_REORGANIZAR` con la sesión ≤ M | Sí | Bajar dosis o retirar **un** complemento; conservar los estructurales y la dosis de los que se quedan | `SIN_EFECTO`: se conserva y consta | **Se conserva**: se basa en una disponibilidad declarada para ese entrenamiento | Actual + tope M (nuevo) |
| Falta de tiempo · sin respuesta (D1) | **`CONSERVAR`** | No | Nada | No aplica | Nada que restaurar | **Cambia** la regla actual (hoy acorta) |
| Cambio de horario | `REPROGRAMAR_DIAS` | Sí | Solo la fecha, dentro de los días disponibles y de la semana | `SIN_EFECTO` | **Restaurable** (§3) | Actual + restauración |
| Dolor o molestia | `SEGURIDAD_ANTE_DOLOR` | Sí | Retirar o sustituir el ejercicio que dolió | Sin IA: `NO_FACTIBLE` | **Nunca** se restaura el ejercicio | Actual |
| Fatiga | `REVISAR_CARGA_Y_RECUPERACION` | Sí | Solo la dosis | `SIN_EFECTO` | **Restaurable** (§3) | Actual + restauración |
| Muy difícil | `VARIANTE_MAS_SENCILLA_O_CARGA` | Sí | Variante del mismo patrón, o la carga | `SIN_EFECTO` | Se conserva: volver sería subir la dificultad | Actual |
| Falta de motivación | `INVESTIGAR_CAMBIO_MINIMO` | Sí | Cambios pequeños de dosis | `SIN_EFECTO` | **Restaurable** (§3), con evidencia positiva (D3) | Actual + restauración |
| Otra, o sin causa | `CAUSA_NO_REGISTRADA` | Hoy sí; propuesto **no** | Nada | No aplica | Nada que restaurar | Actual (sin llamada) |

**Lo que se mantiene sin cambios:** V8C, V1–V25 con 4D en ENFORCE, la sustitución por seguridad de lo heredado, la exención de V20 solo para la sesión que se acorta, `SIN_EFECTO` y `NO_FACTIBLE`, y `RESPALDO` cuando no hay IA.

**Única validación nueva:** con `ACORTAR_O_REORGANIZAR` y un M declarado, la duración estimada no supera M.

**Sin IA cuando no hay nada que pedir.** Hoy toda sesión señalada se envía a la IA, incluida `CAUSA_NO_REGISTRADA`, que no puede cambiar nada. Con `CONSERVAR` y `CAUSA_NO_REGISTRADA` no se hace la llamada, y la sesión queda registrada como se describe en §6.1.

---

## 3. ¿Cómo se conserva la rutina?

### 3.1 Qué se restaura

Solo los cambios **restaurables**:
- el día reprogramado (`REPROGRAMAR_DIAS`);
- la dosis ajustada por fatiga (`REVISAR_CARGA_Y_RECUPERACION`);
- la dosis ajustada por motivación (`INVESTIGAR_CAMBIO_MINIMO`).

Todo lo demás se conserva. Restaurar es volver **exactamente** a la prescripción de referencia, con los mismos ejercicios, dosis y día. No se generan ejercicios ni combinaciones nuevas, y nunca se supera la referencia: restaurar **no es progresar**.

### 3.2 Encontrar la referencia: V31 + V32, sin columna nueva

**El problema de la v0.2.** El origen inmediato no sirve:

```
Semana 1 · referencia      3×12
Semana 2 · fatiga          2×12   origen V31: semana 1
Semana 3 · copia           2×12   origen V31: semana 2   ← su origen ya está reducido
```

**Clasificar cada paso.** Cada paso `origen → derivada` del linaje queda clasificado por la evidencia V32 del intento que publicó la derivada. Ese intento cumple dos condiciones:
- su `resulting_plan_id` es el plan de la derivada;
- todos sus arreglos contienen identificadores de sesiones del plan de origen (trigger de V32).

| Tipo de paso | Condición en V32 |
|---|---|
| **Adaptado por I** | origen ∈ `applied_sessions`; I = `targeted_interventions[k]` en la posición k del origen dentro de `targeted_sessions` |
| **Restaurado** | origen ∈ `restored_sessions` (arreglo nuevo, §6.1) |
| **Sustituido por seguridad** | el origen aparece en las sustituciones de `conservation_report` |
| **Copia** | origen ∈ `conserved_sessions` o `no_effect_sessions`, sin sustitución |

**El algoritmo.** Para una sesión S de la semana medida, se recorre el linaje hacia atrás:
- mientras los pasos sean **copias**, se sigue retrocediendo;
- el primer paso que no es una copia decide el resultado:

| Primer paso que no es una copia | Resultado |
|---|---|
| Adaptado por una intervención **restaurable** | **Referencia = el origen de ese paso** (la versión anterior a la adaptación) |
| Adaptado por una intervención no restaurable, restaurado o sustituido por seguridad | **No hay nada restaurable**: se conserva S |
| No hay ningún paso así (llega a la generación inicial) | **No hay nada restaurable**: se conserva S |

En el ejemplo: semana 3 ← semana 2 es una copia; semana 2 ← semana 1 está adaptada por fatiga; la referencia es la **semana 1** (3×12), sin importar cuántas semanas de copia haya en medio.

**Por qué el recorrido es inequívoco.** Lo garantizan restricciones que ya existen en la base:
- **Un solo origen por sesión** (`source_planned_workout_id` es una columna), nunca ella misma (`ck_pw_source_not_self`), del **mismo usuario** y de una semana **estrictamente anterior** (`fn_derivacion_coherente`). El camino hacia atrás es único y termina.
- **Una sola derivada por origen y plan** (`uq_pw_source_per_plan`).
- **Clasificación única de cada paso:**
  - `applied_sessions` y `no_effect_sessions` son disjuntos (`ck_paa_aplicada_o_sin_efecto`);
  - ambos están contenidos en `targeted_sessions` (`ck_paa_desenlace_subconjunto`);
  - `conserved_sessions` es disjunto de las señaladas (`ck_paa_conservadas_disjuntas`);
  - `targeted_interventions` es paralelo a `targeted_sessions` (`ck_paa_pareja`), sin repetidos (`ck_paa_senaladas_unicas`).
  - El arreglo nuevo `restored_sessions` debe ser disjunto de los cuatro (§6.1).
- **Varios intentos para la misma semana medida.** Por ejemplo, un `NO_FACTIBLE` y luego un éxito. Solo cuenta el intento cuyo `resulting_plan_id` es el plan de la derivada; un intento sin plan no publica derivadas.

**Lo que falta demostrar con datos.** Las restricciones prueban que el camino es único. Que la clasificación sea **completa** depende de que toda derivada publicada tenga su intento V32: P2-D lo escribe en la misma transacción que publica. La consulta de verificación (§7) lo comprueba sobre datos reales, y la reparación por seguridad (`PublishedPlanSafetyRepairService`, hoy sin uso) debe revisarse antes de activarla. **Un paso que no se pueda clasificar no se restaura.**

### 3.3 Cuándo se restaura: evidencia positiva, no ausencia de causa

La restauración se evalúa al derivar la semana siguiente, **solo** si la sesión adaptada tiene ejecución registrada y completada en la semana medida.

| Ejecución de la sesión adaptada en la semana medida | Resultado |
|---|---|
| **Completada** (A_s ≥ 0,80) **y sin señales incompatibles** | **Se evalúa la restauración**: revalidación (§3.4); si pasa, se restaura |
| Completada **con** señal incompatible | No se restaura; el ajuste se mantiene |
| Parcial (A_s < 0,80) | No se restaura. Si quedó señalada, la decisión nueva parte de la versión adaptada |
| Omitida (con cualquier causa) | No se restaura; la decisión nueva parte de la versión adaptada |
| **Sin información** (sin sesión ni omisión declarada) | **No se restaura**: el ajuste se mantiene y la restauración queda **pendiente** para el ciclo siguiente |

**Señales incompatibles.** Todas salen de datos que ya existen, con umbrales aprobados:

| Intervención restaurable | Señal incompatible |
|---|---|
| Fatiga | Un ejercicio de la sesión registrado con causa fatiga o dolor. **El RPE no se usa** (D5) |
| Motivación | Un ejercicio de la sesión registrado con causa falta de motivación |
| Cambio de horario | El día original ya no está en los días disponibles **vigentes** (V3 en la revalidación) |
| Todas | Dolor registrado en la sesión, o cualquier fallo de la revalidación (§3.4) |

**Qué se mantiene mientras la restauración está pendiente.** La sesión ajustada sigue copiándose. El recorrido de §3.2 la sigue encontrando, porque las copias no cortan el camino. No hay plazo ni umbral nuevo: se reevalúa en cada ciclo con la misma regla.

**Qué demuestra y qué no.** Es una condición operativa **observable** para el MVP: sesión completada, sin fatiga ni dolor declarados y revalidada. No demuestra la recuperación fisiológica.

**Una modificación funcional por ciclo publicado.**
- Si la sesión señalada termina `APLICADA`, las restauraciones elegibles **esperan**: siguen copiándose y el recorrido las vuelve a encontrar.
- Con `SIN_EFECTO` o `SIN_ORDEN` se permite **una** restauración validada: la de adaptación más antigua y, a igualdad, la de fecha más temprana.
- Con `RESPALDO` no se restaura, porque `RESPALDO` es por definición una copia exacta.

**Día de la semana.** Un día reprogramado se restaura al **día de la semana** de la referencia dentro de la semana nueva, nunca a su fecha absoluta. Se revalida con V3 (días disponibles vigentes), V12, V13 y V18. Si el día está ocupado o ya no está disponible, no se restaura.

### 3.4 Revalidación: nunca restaurar algo inseguro

La sesión restaurada se valida con el perfil y el catálogo **vigentes**:
- exclusiones de seguridad (SUST-SEG-1), ejercicios bloqueados y restricciones declaradas;
- ningún ejercicio que haya dolido desde la referencia;
- días disponibles vigentes;
- V1–V25 (V20 sin exención: la sesión restaurada ya no está acortada).

Si no pasa, no se restaura: la sesión se conserva como estaba y el motivo queda en `conservation_report`.

---

## 4. Indicador de resultado por sesión adaptada

**Pregunta:** ¿la persona realizó la sesión concreta que FitSense modificó?

| | |
|---|---|
| Unidad | Cada sesión **adaptada publicada**: su origen V31 está en `applied_sessions`. Las restauradas y las conservadas por su causa se miden aparte, con las mismas fórmulas, como comparación |
| Momento | Al cerrar la semana de esa sesión (en el ciclo siguiente, junto a `adherence_after_pct`) |
| A_s | **Proporción entre 0 y 1**: `completion_percentage` / 100 de la sesión que cuenta (`counts_toward_adherence`). Sin sesión, A_s = 0 |
| Fuente | V32, el linaje V31, `planned_workouts` y `workout_sessions`. Consulta o vista de solo lectura, **sin tabla nueva** |

**Clasificación** (umbrales existentes: 0,80 completada y 0,30 válida):

| Resultado | Condición | ¿Tiene registro? |
|---|---|---|
| Completada | A_s ≥ 0,80 | Sí |
| Parcial válida | 0,30 ≤ A_s < 0,80 | Sí |
| Parcial no válida | 0 < A_s < 0,30: cuenta con su A_s real, como en la adherencia, pero no es una sesión válida | Sí |
| Omitida | Omitida con causa, o sesión registrada con A_s = 0. Se marca si la causa es **la misma** que motivó la adaptación | Sí |

**La clase se decide primero por la sesión** (la que cuenta para la adherencia) y, solo si no hay sesión, por la omisión declarada. Una sesión cerrada por debajo del 30 % deja su entrenamiento en estado `SKIPPED`, pero es «parcial no válida» con su A_s real (por ejemplo, 0,25), nunca «omitida». Es la misma semántica de la adherencia oficial (`AdherenciaIT`).
| Sin información | Sin sesión y sin omisión declarada | No |
| No aplica | No hubo sesión adaptada (`SIN_EFECTO`, `RESPALDO`, `NO_FACTIBLE`) | Fuera de la unidad |

**Fórmulas.** n = sesiones adaptadas; n_r = sesiones adaptadas con registro.

| Indicador | Fórmula | Si el denominador es 0 |
|---|---|---|
| Cumplimiento oficial | Σ A_s / n × 100 (sin información cuenta 0) | No calculable (`NULL`) |
| Cumplimiento observado | Σ A_s / n_r × 100 | No calculable (`NULL`) |
| Tasa de finalización | completadas / n_r × 100 | No calculable (`NULL`) |
| Tasa de realización | (completadas + parciales válidas) / n_r × 100 (sesiones con A_s ≥ 0,30) | No calculable (`NULL`) |
| Repetición de la dificultad | omitidas por la misma causa / n_r × 100 | No calculable (`NULL`) |
| Cobertura | n_r / n × 100 | No calculable (`NULL`) |

**No es una prueba causal.** Que una sesión adaptada se complete es un **resultado observado**: no demuestra que la IA o la intervención lo hayan producido, porque no hay grupo de control.

---

## 5. El ejemplo de A01 con este diseño

**Viernes del 06/11:** 6 ejercicios, 33 minutos, omitido por falta de tiempo.

| Caso | Entrada | Decisión | Viernes siguiente | Lunes y miércoles |
|---|---|---|---|---|
| A | «Fue algo puntual» | `CONSERVAR`, sin IA | Igual: 6 ejercicios, 33 min | Iguales |
| B | «Suelo tener menos», M = 30 para este entrenamiento | `ACORTAR_O_REORGANIZAR`, ≤ 30 min | Retira el complemento 1256 → 28 min (lo que hizo A01). Si no fuera posible sin tocar los estructurales: `SIN_EFECTO` | Iguales |
| B' | «Suelo tener menos», M = 40 | `CONSERVAR` (33 ya cabe en 40) | Igual | Iguales |
| C | No responde | `CONSERVAR`, sin IA | Igual | Iguales |
| D | Fatiga: dosis ajustada; la semana siguiente completada sin fatiga ni dolor declarados | Restaurar (revalidada) | Vuelve a la referencia | Iguales |

**A01 sigue siendo la prueba técnica histórica** de la regla anterior (`ADAPT-1`, §6.2), con sus evidencias intactas.

---

## 6. Verificaciones técnicas

### 6.1 V32: conservar no es aplicar

**Invariantes que se mantienen:**
- **V33:** `APLICADA` ⇔ `applied_sessions` no está vacío.
- `applied_sessions` y `no_effect_sessions` ⊆ `targeted_sessions`, disjuntos entre sí.
- `conserved_sessions` es disjunto de las señaladas.
- `RESPALDO` y `SIN_ORDEN` tienen origen `CONSERVED`.

**Cómo se registra lo nuevo:**

| Situación | Registro |
|---|---|
| Sesión conservada por su causa (`CONSERVAR` o `CAUSA_NO_REGISTRADA`) | Sigue **señalada**, con su intervención en `targeted_interventions`. Va en **`no_effect_sessions`**, nunca en `applied_sessions`. Si es la única señalada: `status = SIN_EFECTO`, `ai_calls = 0`, `generation_source = CONSERVED` |
| Mezcla, en `ADAPT_EXPANDED` (pospuesto) | Las conservadas por su causa van en `no_effect_sessions`; las corregidas en `applied_sessions`. `APLICADA` si al menos una se aplicó |
| Sesión restaurada | Arreglo nuevo **`restored_sessions`** (con el origen, como los demás), disjunto de los cuatro. No es una adaptación aplicada ni una conservada |
| Restauración bloqueada | La sesión sigue en `conserved_sessions`; el motivo va en `conservation_report` |
| Semana `NO_CHANGE` con una restauración | `status = SIN_ORDEN`, origen `CONSERVED`: restaurar es volver a la prescripción anterior, no una generación de la IA. Verificado: el ciclo publica una versión nueva con `NO_CHANGE` |
| Señalada `SIN_EFECTO` con una restauración en otra sesión | `status = SIN_EFECTO`; la restaurada va en `restored_sessions`, no en `applied_sessions` (V33 intacta) |
| Señalada `APLICADA` | Sin restauración ese ciclo (§3.3) |

**Así se distingue «conservada por su causa» de «la IA no encontró un cambio permitido»**, sin un estado nuevo: por la intervención (`CONSERVAR` o `CAUSA_NO_REGISTRADA`) y por `ai_calls`.

**Cambios de esquema (una migración nueva, V35; V34 no se toca):**
- añadir `CONSERVAR` a `ck_paa_intervenciones`;
- añadir la columna `restored_sessions`, con sus restricciones de disjunción y pertenencia al plan de origen (trigger de V32);
- añadir `adaptation_rule_version` (§6.2).

### 6.2 Versionado de la regla de adaptación

**Hoy:** `user_interventions.rule_version` y `weekly_user_metrics.calculation_version` guardan la versión de la **configuración del cálculo** (MVP-1.8): umbrales y parámetros. La lógica de adaptación no cambia esos umbrales, así que subir esa versión mezclaría dos cosas distintas.

**Propuesta:**
- Columna `plan_adaptation_attempts.adaptation_rule_version`, obligatoria, escrita por P2-D en cada intento.
- Valores:
  - `ADAPT-1`: la lógica vigente, la de A01; es el valor para las filas existentes;
  - `ADAPT-2`: este diseño.
- **Enlace con la intervención:** el intento y la intervención comparten `weekly_metric_id`, la semana medida. Con varios intentos para la misma semana, el que vale es el que publicó (`resulting_plan_id` = el `resulting_plan_id` de la intervención) o, si ninguno publicó, el último por `attempt_id`.
- **A01** consta como `ADAPT-1` en su expediente. No se modifica: corrió en una base temporal que ya no existe.

---

## 7. Impacto, mínimo

| Componente | Cambio |
|---|---|
| Flutter | La pregunta opcional tras «No tuve tiempo» y el campo de minutos «para este entrenamiento» |
| Backend: contrato | `POST /api/v1/workouts/{id}/skip`: `lackOfTimeContext` y `availableMinutes` opcionales (este, solo con `MENOS_TIEMPO_HABITUAL`, de 15 a 180; si no, 400) |
| Backend: decisión | Intervención según causa y respuesta; `CONSERVAR` sin llamada a la IA; recorrido y política de restauración (§3) |
| PostgreSQL | `planned_workouts`: `lack_of_time_context`, `declared_available_minutes`. V35: `CONSERVAR` en `ck_paa_intervenciones`, `restored_sessions` y `adaptation_rule_version` |
| Prompt | Para la sesión con M: «la sesión debe durar como máximo M minutos». Las sesiones que se conservan no se envían |
| Validaciones | V8C acepta una restauración solo si es idéntica a la referencia. Nueva: sesión ≤ M. **Sin cambios:** V1–V25 y sus umbrales |
| Analítica | Consulta del indicador (§4) y **consulta de verificación del linaje**: toda derivada publicada tiene exactamente un intento V32 con su plan, y cada paso se clasifica en un solo tipo |

**No cambia:** la generación inicial, el decisor, los umbrales, V24, V25 ni V31.

---

## 8. Casos de aceptación

| # | Entrada | Esperado | Evidencia |
|---|---|---|---|
| 1 | Viernes omitido · imprevisto | Viernes siguiente idéntico; 0 llamadas a la IA | V32 `SIN_EFECTO`, viernes en `no_effect_sessions` con `CONSERVAR`, `ai_calls = 0`, `CONSERVED` |
| 2 | Viernes omitido · M = 30 (factible) | Viernes ≤ 30 min; solo se retira un complemento; lunes y miércoles idénticos | V32 `APLICADA`; V8C y V1–V25 aprobadas; duración ≤ 30 |
| 3 | M que solo se alcanza tocando estructurales | `SIN_EFECTO`; viernes conservado | V32 `no_effect_sessions` |
| 4 | Viernes omitido · sin respuesta | Igual que el caso 1 | Igual que el caso 1 |
| 5 | Fatiga en la semana 2; copias en la 3 y la 4 **sin información**; completada en la 5 sin fatiga ni dolor declarados | Se mantiene en las semanas 3 y 4; en la 6 se restaura la **semana 1** | Recorrido V31 + V32 hasta la semana 1; `restored_sessions`; sesión idéntica a la referencia |
| 6 | Fatiga; la semana siguiente, completada pero con un ejercicio registrado con causa fatiga | No se restaura | Señal incompatible registrada |
| 7 | Cambio de horario; después la persona quita el día original de sus días disponibles | No se restaura | Revalidación V3; motivo en `conservation_report` |
| 8 | Fatiga; la referencia contiene un ejercicio bloqueado después | No se restaura ese contenido | Motivo en `conservation_report`; ningún ejercicio bloqueado publicado |
| 10 | En el mismo ciclo: una sesión señalada se aplica y otra cumple para restaurarse | Solo se modifica la señalada; la restauración espera al ciclo siguiente | V32 `APLICADA` con `restored_sessions` vacío; la otra sesión es copia |
| 11 | **Prioritaria antes de ADAPT-2.** Restauración real por fatiga en un ciclo `NO_CHANGE` | Se publica la semana con la sesión restaurada | Publicación `SIN_ORDEN`, V8C que solo acepta la sesión idéntica a la referencia, V31, V32 (`restored_sessions`) y V33 |
| 9 | Sesión adaptada: completada / al 25 % / omitida por la misma causa / sin registro | Completada / parcial no válida / omitida con la marca / sin información | Consulta §4; con n_r = 0, indicadores `NULL` |

---

## 9. Decisiones

| # | Decisión | Estado |
|---|---|---|
| D1 | Falta de tiempo sin respuesta: conservar sin IA | **Aprobada** (27/10) |
| D2 | Registro en V32 (§6.1): `no_effect_sessions` + `CONSERVAR` y el arreglo nuevo `restored_sessions`, sin estados nuevos | **Aprobada**, condicionada a las pruebas de invariantes (casos 10 y 11) |
| D3 | Motivación: restaurable, solo con evidencia positiva (§3.3) | **Aprobada con condición**, incorporada |
| D4 | Sin pregunta en sesiones parciales | **Aprobada** |
| D5 | El RPE **no** bloquea la restauración: es un RPE global de la sesión, opcional, y su umbral se aplica al promedio semanal como riesgo descriptivo | **Cerrada sin RPE** (27/10) |
| D6 | `CAUSA_NO_REGISTRADA` deja de llamar a la IA | **Aprobada** |
| D7 | `adaptation_rule_version` en V32, con `ADAPT-1` para lo existente (§6.2) | **Aprobada**, para V35 |
| D8 | La copia idéntica de una sesión acortada con autorización hereda la exención de V20 (defecto del comportamiento actual) | **Implementada** (`aeb09ab`). Local cerrado; en Neon, 0 violaciones y sin historial (`VERIFICACION_DISENO_V03.md`) |

## 10. Verificaciones

`VERIFICACION_DISENO_V03.md` recoge las comprobaciones contra el código y PostgreSQL. En resumen:
- `NO_CHANGE` publica una versión nueva.
- El linaje V31 + V32 es inequívoco: 0 violaciones y una derivación sin evidencia se detecta (`LinajeIntegridadIT`, `consultas/linaje_v31_v32.sql`).
- Una sesión al 25 % aporta 0,25.
- El RPE no es apropiado como bloqueo.
- Se encontró y corrigió el defecto de V20 (D8).

## 11. Para la tesis

**Qué se puede sostener:** la adaptación es **mínima** (una sesión, un cambio permitido), **justificada** (por la causa y, en la falta de tiempo, por la disponibilidad que declara la persona para ese entrenamiento), **reversible** cuando corresponde y hay evidencia (§3.3) y **trazable** (V31, V32 y `adaptation_rule_version`).

**Qué no se puede sostener:** que una mejor adherencia posterior se deba a la IA, ni que la disponibilidad declarada sea la real.

**Datos:** se comparan por `adaptation_rule_version`: A01 corresponde a `ADAPT-1` y las pruebas nuevas a `ADAPT-2`.
