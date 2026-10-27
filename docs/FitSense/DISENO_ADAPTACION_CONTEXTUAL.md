# Diseño · Adaptación contextual y recuperación de la prescripción

| | |
|---|---|
| Versión | **v0.1 · borrador para revisión** |
| Fecha | 27/10/2026 |
| Base | rama `generation`, `8c5ab36` |
| Estado | Solo diseño: **sin código, migraciones, endpoints ni cambios en Flutter** hasta su aprobación |

Principio que gobierna todo el documento:
- **La omisión es un hecho.**
- **La causa es información declarada.**
- **La intervención es una decisión justificada por ambas.**

Una omisión nunca implica por sí sola que haya que reducir el entrenamiento.

---

## 1. Problema y justificación

**Lo que mostró A01** (`A01_ADAPTACION_REAL.md`):
- El perfil declaraba 45 minutos por sesión y el viernes previsto duraba 33.
- La persona lo omitió y declaró `LACK_OF_TIME`.
- FitSense decidió `ADAPT_LOCALIZED` y GPT-5-mini retiró un relleno: el viernes siguiente quedó en 28 minutos.

**La adaptación es técnicamente válida, pero no es pertinente:**
1. **No hay evidencia de que 28 minutos resuelvan algo.** La sesión omitida ya era 12 minutos más corta de lo que la persona dijo tener, así que el sistema no sabe si fue un imprevisto, si su disponibilidad real es menor o si el problema era el día.
2. **La reducción es permanente sin que nadie lo haya decidido.** Desde la segunda semana, cada semana se **deriva de la anterior** (`WeeklyCycleService`, P2-D) y nunca progresa sola. Si la persona completa el viernes de 28 minutos, la decisión siguiente es `NO_CHANGE` y el viernes queda en 28 minutos indefinidamente, aunque el motivo haya sido puntual.
3. **No se mide si la intervención funcionó en la sesión concreta.** Hoy solo existe `adherence_after_pct`, que es la adherencia de toda la semana siguiente.

A01 sigue siendo una **prueba técnica válida**: demuestra que la cadena funciona de punta a punta con IA real. Este diseño no cambia sus evidencias ni su conclusión.

**Lo que no cambia.** Se conserva la arquitectura validada:

> Registro → Medición de adherencia → Decisión → Intervención → IA generativa → Validación → Publicación → Evaluación posterior

Tampoco cambian los umbrales de adherencia (80 % y 50 %), la cobertura mínima (50 %), los umbrales de sesión (80 % completada, 30 % válida), V8C, V24, V25 ni el resto de reglas aprobadas. Lo nuevo **se agrega** donde hoy falta información; no reemplaza lo validado.

---

## 2. Comportamiento actual frente al propuesto

| Aspecto | Hoy (código) | Propuesto |
|---|---|---|
| Causa de la omisión | Una de 7 (`SkipReason`), obligatoria al omitir | Igual, y para `LACK_OF_TIME` una **subcausa opcional** |
| Causa → intervención | Fija, una por causa (`TipoIntervencion.deMotivo`) | La subcausa puede elegir otra intervención o ninguna (matriz, §3) |
| Disponibilidad real | No se pregunta | Solo si la persona dice que **normalmente** tiene menos tiempo, y siempre la indica ella |
| Alcance del cambio | Implícito: todo cambio persiste, porque la semana siguiente se copia | **Explícito:** `PUNTUAL`, `TEMPORAL` o `PERSISTENTE` (§5) |
| Volver a lo anterior | No existe | **Restauración** validada de la prescripción de referencia (§5) |
| Cambio del perfil | `PUT /profile` existe, pero el ciclo no lo propone | El ciclo lo **sugiere** y la persona lo **confirma** (§3, §6) |
| Medición del resultado | `adherence_after_pct` y `outcome` (IMPROVED, UNCHANGED, WORSENED) por semana | Además, el **resultado de cada sesión adaptada** (§6) |

---

## 3. Matriz de decisión

### 3.1 La pregunta de seguimiento por falta de tiempo (nueva)

**Cuándo se pregunta.** Justo después de que la persona elige «No tuve tiempo»:
- al omitir un entrenamiento (`POST /api/v1/workouts/{id}/skip` con `LACK_OF_TIME`);
- al cerrar una sesión parcial cuyo motivo dominante es `LACK_OF_TIME` (decisión pendiente D3).

**Es opcional.** Se puede cerrar sin responder, y la omisión ya queda registrada antes de la pregunta.

**Pregunta:** «¿Qué pasó con tu tiempo?»

| Opción en pantalla | Valor (`lack_of_time_context`) |
|---|---|
| «Fue algo puntual; normalmente sí tengo el tiempo que indiqué» | `IMPREVISTO` |
| «Normalmente tengo menos tiempo» → pide «¿Cuántos minutos sueles tener?» | `MENOS_TIEMPO_HABITUAL` + `declared_available_minutes` |
| «Podría haber entrenado otro día» | `OTRO_DIA` |
| (cierra sin responder) | `NULL` → **sin especificar** |

**Reglas de la respuesta:**
- **Los minutos los indica siempre la persona.** No se infieren de la duración de la sesión omitida ni de la sesión adaptada.
- Los minutos tienen el **mismo rango** que ya admite el perfil (15 a 180, `UserProfile.applyAvailability`). No se añade un umbral nuevo.
- Se puede responder hasta que cierre la semana (antes del ciclo del lunes). Después, la respuesta ya no influye en la decisión de esa semana.

**Cómo se registra:** en el entrenamiento previsto (`planned_workouts`), junto a `skip_reason`, que es donde ya vive la causa. Es una declaración sobre ese entrenamiento, no sobre el perfil.

### 3.2 Cómo se usa en la decisión

**El decisor no cambia** (`AdaptationOrderDecider`): con la observada y la cobertura decide `NO_CHANGE`, `ADAPT_LOCALIZED`, `ADAPT_EXPANDED` o `INSUFFICIENT_DATA`, y señala las sesiones por debajo del 80 %. Lo nuevo es **qué intervención recibe cada sesión señalada**: hoy depende solo de la causa; con el diseño, de la causa y la subcausa. Cuando la intervención no pide ningún cambio, la sesión **no se envía a la IA**.

**Clave de las columnas:**
- **«Regla»:** *A* es una regla actual, que se conserva; *N* es nueva, propuesta.
- **«Alcance»:** se define en §5.
- **«Confirmación»:** si la persona debe aceptar algo antes de que se aplique.

#### Falta de tiempo (`LACK_OF_TIME`)

| Subcausa | Datos necesarios | Decisión del backend | Intervención | Qué puede cambiar la IA | Qué queda intacto | Seguridad y factibilidad | Sin modificación válida | Alcance | Confirmación | Regla |
|---|---|---|---|---|---|---|---|---|---|---|
| `IMPREVISTO` | La subcausa | La sesión **se conserva** y no se pide nada a la IA | **N** `CONSERVAR_Y_OBSERVAR` | Nada (no se le llama) | Toda la sesión | No aplica | No aplica | `PUNTUAL` (nada que revertir) | No | N |
| `MENOS_TIEMPO_HABITUAL` | Subcausa y minutos M | (1) **Sugerir** cambiar el perfil a M. (2) Si **se confirma**: la semana siguiente se **regenera** con el perfil nuevo, sin P2-D (ver nota). (3) Si **no se confirma** o no responde a tiempo: `ACORTAR_O_REORGANIZAR` sobre esa sesión, con tope M | (3) `ACORTAR_O_REORGANIZAR` | (3) Bajar dosis o retirar **un** relleno, como hoy, **y que la sesión quepa en M** | Día, enfoque, ejercicios estructurales; dosis de lo que se queda si retira uno | V8C, V1–V25 y la exención de V20 de hoy. **Nueva:** la duración estimada no supera M | (3) `SIN_EFECTO`: se conserva y consta | (2) `PERSISTENTE`; (3) `TEMPORAL` | **Sí** para (2) | N (con la intervención A) |
| `OTRO_DIA` | La subcausa | `REPROGRAMAR_DIAS` sobre esa sesión | A `REPROGRAMAR_DIAS` | Solo la fecha | Ejercicios, dosis, enfoque, duración | V3 (un día de `available_days`, dentro de la semana), V12, V13, V18 | Sin día libre válido: `SIN_EFECTO`, y se sugiere ampliar los días disponibles (confirmación) | `PUNTUAL` | Solo para cambiar el perfil | A (intervención) + N (sugerencia) |
| Sin especificar | Nada más | Decisión pendiente **D1**. Recomendada: `ACORTAR_O_REORGANIZAR` como hoy, pero `TEMPORAL` | A `ACORTAR_O_REORGANIZAR` | Lo de hoy | Lo de hoy | Lo de hoy | `SIN_EFECTO` | `TEMPORAL` | No | A + alcance N |

**Nota sobre la regeneración.** Si la persona confirma menos minutos, la semana siguiente se **regenera** y no se adapta con P2-D. P2-D corrige solo las sesiones señaladas y conserva las demás. Con un tope de duración menor para toda la semana, las sesiones conservadas incumplirían V24 y la semana quedaría `NO_FACTIBLE`; ya está demostrado con `forcedSessionMinutes` (`ADENDA_4D_v3_1.md` §6, `AdaptacionCompletaIT.ordenDeSesionesMasCortas`). Un cambio de perfil confirmado cambia **todas** las sesiones, así que corresponde una semana nueva generada con el perfil vigente, validada con V1–V25 y registrada con su motivo.

#### Las otras causas

| Causa | Intervención (A) | Qué cambia la IA (A) | Alcance propuesto (N) | Sin modificación válida | Confirmación | Observación |
|---|---|---|---|---|---|---|
| `PAIN_OR_DISCOMFORT` | `SEGURIDAD_ANTE_DOLOR` | Quitar el ejercicio que dolió, o reemplazarlo tras revisar la restricción | **`PERSISTENTE` para el ejercicio:** nunca se restaura automáticamente | Sin IA: `NO_FACTIBLE` (no se republica lo que dolió); así sigue | No (decisión pendiente **D6**: ¿preguntar si bloquear el ejercicio en el perfil?) | Conservar no es neutro: es la única causa en que la sesión **no** puede quedar igual |
| `FATIGUE` | `REVISAR_CARGA_Y_RECUPERACION` | Ajustar dosis; no sustituir, retirar ni mover | `TEMPORAL` | `SIN_EFECTO` | No | La fatiga no autoriza mover el día ni bajar del piso de V20 |
| `TOO_DIFFICULT` | `VARIANTE_MAS_SENCILLA_O_CARGA` | Variante del mismo patrón, o carga | **`PERSISTENTE`** | `SIN_EFECTO` | No | Volver a la variante difícil sería **aumentar la dificultad**, que no es restaurar: queda para una política de progresión, fuera de alcance |
| `SCHEDULE_CHANGE` | `REPROGRAMAR_DIAS` | Solo la fecha | `PUNTUAL` | `SIN_EFECTO` + sugerir días disponibles | Solo para cambiar el perfil | Igual que `OTRO_DIA` |
| `LACK_OF_MOTIVATION` | `INVESTIGAR_CAMBIO_MINIMO` | Cambios pequeños de dosis | `TEMPORAL` | `SIN_EFECTO` | No | Sin pregunta de seguimiento en esta versión (decisión pendiente **D5**) |
| `OTHER` o sin causa | `CAUSA_NO_REGISTRADA` | Nada | `PUNTUAL` | La sesión se conserva | No | No se inventa la causa |
| Vencido sin causa | No es un registro | No se señala | No aplica | No aplica | No | Igual que hoy: cuenta 0 en la oficial, no entra en la observada |

**Factibilidad, en todos los casos:**
- Sin IA (solo motor de reglas), el resultado es el de hoy: `RESPALDO`, o `NO_FACTIBLE` ante dolor.
- Ninguna fila añade umbrales: los números que aparecen son los ya aprobados o los minutos que declara la persona.

---

## 4. Flujo funcional y estados

```
Omisión registrada (causa obligatoria)
   └─ si LACK_OF_TIME → pregunta opcional (subcausa, minutos)
        └─ si MENOS_TIEMPO_HABITUAL → sugerencia de actualizar el perfil (confirmar o descartar)
Cierre semanal (lunes 03:00)
   1. Cerrar y medir (sin cambios)
   2. Puntuar la intervención anterior (sin cambios) + resultado de cada sesión adaptada (N, §6)
   3. Decidir (sin cambios en umbrales)
   4. Por sesión señalada: intervención = f(causa, subcausa)   (N)
      Por sesión adaptada la semana anterior con alcance TEMPORAL o PUNTUAL: ¿restaurar? (N, §5)
   5. ¿Perfil cambiado y confirmado desde la última semana?
        sí → regenerar la semana con el perfil vigente (N)
        no → derivar (P2-D) · IA solo para las señaladas con cambio pedido · validar · publicar
   6. Enlazar intervención, evidencia V32 (+ alcance y restauraciones, N)
```

**Estados de una sesión señalada en V32.** Hoy existen `APLICADA`, `SIN_EFECTO`, `SIN_ORDEN`, `RESPALDO` y `NO_FACTIBLE` por intento. Se añadiría por sesión:

| Estado por sesión | Significado | Regla |
|---|---|---|
| `aplicada` | La IA la corrigió y V8C lo aceptó | A (`applied_sessions`) |
| `sin_efecto` | Se pidió un cambio y ninguno era válido | A (`no_effect_sessions`) |
| `conservada_por_causa` | La subcausa pide no cambiar (por ejemplo, `IMPREVISTO`) y no se llamó a la IA | **N** |
| `restaurada` | Se devolvió a su prescripción de referencia | **N** |
| `restauracion_no_factible` | Debía restaurarse y la referencia ya no es válida (§5.3) | **N** |

---

## 5. Temporalidad y restauración

### 5.1 Tres alcances

| Alcance | Qué significa | Ejemplo | Semana siguiente |
|---|---|---|---|
| `PUNTUAL` | Vale solo para la semana adaptada | Mover el viernes al sábado por un cambio de horario | Se deriva de la **referencia**: vuelve el día original |
| `TEMPORAL` | Vale mientras dure la causa | Acortar por falta de tiempo sin subcausa; bajar la carga por fatiga | Se **restaura** la referencia si la causa no se repitió (§5.2) |
| `PERSISTENTE` | Pasa a ser la nueva base | Variante más sencilla; ejercicio que dolió; perfil actualizado y confirmado | No se restaura nunca automáticamente |

### 5.2 La prescripción de referencia

**Definición.** La referencia de una sesión adaptada es la versión de esa sesión **antes** de la primera adaptación `TEMPORAL` o `PUNTUAL` de la cadena. En A01, la referencia del viernes del 13/11 es el viernes del 06/11: 6 colocaciones y 33 minutos.

**Cómo se conserva.** No se copian datos: la referencia es un entrenamiento previsto que ya existe en la base. Basta con saber cuál es:
- **Opción recomendada (decisión D7):** una columna nueva `reference_planned_workout_id` en `planned_workouts`, nula salvo en sesiones adaptadas con alcance `TEMPORAL` o `PUNTUAL`.
  - Se escribe al publicar.
  - Si la referencia ya era una sesión adaptada, se hereda su referencia, de modo que la cadena no crece.
- **Alternativa sin columna:** recorrer el linaje V31 (`source_planned_workout_id`) hacia atrás, consultando el alcance en V32 de cada eslabón. Es posible, pero frágil con varias semanas encadenadas.

**Cuándo se restaura (TEMPORAL).** Al derivar la semana N+1, una sesión adaptada en la semana N se restaura si se cumplen las tres condiciones:
1. su alcance es `TEMPORAL`;
2. en la semana N **no** volvió a fallar por la misma causa (no fue señalada con la misma causa);
3. la referencia supera la revalidación (§5.3).

Si volvió a fallar por la misma causa, no se restaura, y la nueva decisión parte de la versión adaptada.

**Duración.** Por defecto, una semana del ciclo; es la unidad natural, no un umbral nuevo (decisión **D2**).

**`PUNTUAL`:** se restaura siempre en la semana siguiente, con la misma revalidación.

### 5.3 Revalidación: nunca restaurar algo inseguro

La sesión restaurada se valida como cualquier sesión publicada, con el perfil y el catálogo **vigentes**, no los de entonces:
- **Seguridad:** exclusiones de seguridad (SUST-SEG-1), ejercicios bloqueados por la persona (`blocked_exercise_ids`), restricciones declaradas, y ningún ejercicio retirado por dolor en cualquier semana desde la referencia.
- **Elegibilidad y equipo:** V1 y el catálogo funcional vigente.
- **Dosis y cobertura:** V20 (sin exención: la sesión restaurada ya no está acortada), V24 y V25.

Si la referencia no pasa, **no se restaura**: la sesión queda como estaba, en estado `restauracion_no_factible` y con el motivo. Si el problema es un solo ejercicio inseguro, se le aplica la sustitución de seguridad de siempre (SUST-SEG-1) o se conserva la versión adaptada; nunca se reintroduce.

### 5.4 La restauración no es progresión

- **Restaurar** devuelve **exactamente** la prescripción de referencia, con los mismos ejercicios, series, repeticiones, descanso y día, que la persona ya tuvo. Nunca la supera.
- **Progresar** (subir carga, volumen o dificultad por encima de la referencia) no existe en FitSense y este diseño no lo introduce.
- Por eso `TOO_DIFFICULT` es `PERSISTENTE`: volver a la variante difícil sería subir la dificultad, no restaurar.

### 5.5 Registro

- **V32** (`plan_adaptation_attempts`): se añaden por intento los arreglos `restored_sessions` y `restoration_blocked_sessions` (mismo formato que `applied_sessions`). El motivo de cada bloqueo va en `conservation_report`, como hoy van las sustituciones.
- **V8C:** la restauración es un cambio **autorizado** de una sesión no señalada. Se acepta solo si la sesión resultante es idéntica a la referencia, salvo las sustituciones de seguridad.

---

## 6. Indicador de resultado por sesión adaptada

**Pregunta:** ¿la persona realizó la sesión concreta que FitSense modificó?

**Unidad de análisis.** Cada **sesión adaptada publicada**: el entrenamiento previsto del plan resultante (`resulting_plan_id`) cuyo origen V31 está en `applied_sessions` del intento V32. Las sesiones restauradas se miden aparte con la misma fórmula. Las `conservada_por_causa` también se miden aparte, porque permiten comparar.

**Momento.** Al cerrar la semana de esa sesión, en el paso 2 del ciclo siguiente: el mismo momento en que hoy se calcula `adherence_after_pct`.

**Clasificación.** Usa los umbrales que ya existen:

| Resultado | Condición |
|---|---|
| `COMPLETADA` | Sesión que cuenta con A_s ≥ 80 % (`session_completed_threshold_pct`) |
| `PARCIAL` | Sesión que cuenta con 0 < A_s < 80 % |
| `OMITIDA` | Omitida **con** causa, o sesión con A_s = 0. Se guarda si la causa es **la misma** que motivó la adaptación |
| `SIN_INFORMACION` | Sin sesión y sin omisión declarada (vencida sin causa) |
| `NO_APLICA` | El intento fue `SIN_EFECTO`, `RESPALDO` o `NO_FACTIBLE`: no hubo sesión adaptada |

**Fórmulas** (mismo criterio que la adherencia oficial y la observada; sin pesos):

| Indicador | Fórmula | Denominador |
|---|---|---|
| Cumplimiento oficial de sesiones adaptadas | Σ A_s / n × 100 | Todas las sesiones adaptadas (n); las `SIN_INFORMACION` cuentan 0 |
| Cumplimiento observado | Σ A_s / n_r × 100 | Solo las que tienen registro (n_r) |
| Tasa de realización | `COMPLETADA` / n_r | Solo las que tienen registro |
| Repetición de la dificultad | `OMITIDA` con la misma causa / n_r | Solo las que tienen registro |

**Sin duplicar datos.** Todo se obtiene de lo que ya existe:
- V32 (`applied_sessions`, `resulting_plan_id`);
- el linaje V31;
- `planned_workouts` (estado y causa) y `workout_sessions` (A_s, `counts_toward_adherence`).

Propuesta: una **vista** de solo lectura o una consulta de analítica, como `consultas/adherencia_recalculada.sql`, sin tabla nueva (decisión **D8**). Si hiciera falta guardarlo, iría en la evidencia V32 o en `user_interventions` como un resumen, nunca copiando sesiones.

**Lo que este indicador no demuestra:**
- Que una sesión adaptada se complete **no prueba** que la IA o la intervención lo hayan causado. No hay grupo de control: la persona pudo tener más tiempo esa semana, o cumplir por estar observada.
- `outcome` = `IMPROVED` compara dos cifras; no es un efecto causal.

En la tesis se presentan como **resultados observados**, con su denominador y su cobertura.

---

## 7. Contrato Flutter–backend propuesto

**Omitir** (`POST /api/v1/workouts/{plannedWorkoutId}/skip`). Hoy el cuerpo es `{ "reason": "LACK_OF_TIME" }`. Se proponen dos campos **opcionales**:

```json
{ "reason": "LACK_OF_TIME",
  "lackOfTimeContext": "MENOS_TIEMPO_HABITUAL",
  "availableMinutes": 25 }
```

- `lackOfTimeContext` solo se admite con `reason = LACK_OF_TIME`; con otra causa → 400.
- `availableMinutes` solo con `MENOS_TIEMPO_HABITUAL`, entre 15 y 180 → si no, 400.
- Responder después de omitir: `PATCH /api/v1/workouts/{plannedWorkoutId}/skip-context` con los mismos dos campos, mientras la semana no esté cerrada (después → 409).

**Sesión parcial:** si se aprueba D3, el cierre de sesión acepta los mismos dos campos opcionales.

**Sugerencia de perfil.** No hace falta un endpoint nuevo:
- Tras `MENOS_TIEMPO_HABITUAL`, Flutter muestra «¿Quieres que tus sesiones duren M minutos a partir de la próxima semana?».
- Si acepta, usa el `PUT /profile` que **ya existe** (`UpdateUserProfileCommand.sessionMinutes`).
- Si rechaza, no se envía nada y rige la fila (3) de la matriz.
- Lo mismo para ampliar días con `OTRO_DIA` o `SCHEDULE_CHANGE`, con `availableDays`.

**Lectura.** `GET` del plan vigente añade, por sesión:
- `adaptationScope`: `PUNTUAL`, `TEMPORAL`, `PERSISTENTE` o nulo;
- `restoredFrom`: el id de la referencia, si fue restaurada.

Flutter puede mostrar «Sesión acortada esta semana».

---

## 8. Impacto por componente

| Componente | Cambio necesario | Reutiliza |
|---|---|---|
| **Flutter** | Pregunta opcional tras «No tuve tiempo»; campo de minutos; sugerencia de perfil; etiqueta de alcance | `PUT /profile` existente |
| **Backend: contratos** | Dos campos opcionales en omitir (y en cerrar sesión, si D3); `PATCH skip-context`; dos campos de lectura | `SkipWorkoutResource`, `GlobalExceptionHandler` (400 y 409) |
| **Backend: decisión** | `TipoIntervencion.deMotivo(causa, subcausa)`; una intervención nueva `CONSERVAR_Y_OBSERVAR`; alcance por intervención; regla de restauración; regenerar si el perfil cambió | `AdaptationOrderDecider` sin cambios en umbrales; `SessionOutcomeIdentifier` (lleva la subcausa) |
| **PostgreSQL** (mínimo) | `planned_workouts`: `lack_of_time_context`, `declared_available_minutes`, `reference_planned_workout_id` (D7). `plan_adaptation_attempts`: `restored_sessions`, `restoration_blocked_sessions` y el alcance por sesión aplicada (arreglo paralelo, o dentro de `conservation_report`) | Linaje V31; arreglos de V32 |
| **Prompt** (`SessionFixPromptBuilder`) | Por sesión señalada: alcance y, si existe, «la sesión debe caber en M minutos». Las `conservada_por_causa` no se envían | Las instrucciones de `TipoIntervencion` |
| **Validaciones** | V8C: aceptar `restaurada` solo si es idéntica a la referencia (más SUST-SEG-1); **nueva:** sesión con M declarado no supera M. **Sin cambios:** V1–V25 y sus umbrales | `ConservationValidator`, `PlanDraftValidator` |
| **Trazabilidad V31/V32** | V31 sin cambios; la referencia es un puntero, no una copia. V32 gana dos arreglos y el alcance | `plan_adaptation_attempts` |
| **Analítica** | Vista del resultado por sesión adaptada (§6) | `weekly_user_metrics`, `workout_sessions` |
| **Pruebas** | Los 8 casos de §9 como pruebas de integración (sin IA, con la IA simulada como en `AdaptacionCompletaIT`) y, al final, una corrida real como A01 para los casos 2 y 4 | Arnés de A01 (`A01AdaptacionRealIT`) |

**Fuera de alcance:** progresión de carga o dificultad; nuevas causas de omisión; `ADAPT_EXPANDED` (se retoma después de aprobar esto); calibración de umbrales.

---

## 9. Casos de aceptación

| # | Entrada | Decisión esperada | Modificación permitida | Resultado esperado | Evidencia verificable |
|---|---|---|---|---|---|
| 1 | Escenario A01 (H H O): viernes omitido por `LACK_OF_TIME` + `IMPREVISTO` | `ADAPT_LOCALIZED`; viernes `CONSERVAR_Y_OBSERVAR` | Ninguna; 0 llamadas a la IA | Viernes siguiente idéntico (6 colocaciones, 33 min) | V32: estado por sesión `conservada_por_causa`, `ai_calls = 0`; sesión idéntica a la medida |
| 2 | Igual, + `MENOS_TIEMPO_HABITUAL`, M = 25, **confirma** el perfil | Perfil a 25; semana siguiente **regenerada** | Semana nueva completa con el perfil de 25 | Todas las sesiones ≤ tope de 25 min (V24 con la duración efectiva, V20) | Perfil actualizado; plan con motivo de regeneración; V1–V25 aprobada |
| 2b | Igual, M = 25, **no confirma** | `ACORTAR_O_REORGANIZAR` `TEMPORAL` con tope 25 | Bajar dosis o retirar un relleno, ≤ 25 min | Si cabe: aplicada; si no: `SIN_EFECTO` | V32 con alcance `TEMPORAL`; validación «no supera M» |
| 3 | Viernes omitido + `OTRO_DIA`, perfil con días [1, 3, 5, 6] | `REPROGRAMAR_DIAS` `PUNTUAL` | Solo la fecha (sábado) | Sábado con los mismos ejercicios; la semana siguiente vuelve al viernes | V8C acepta solo el cambio de día; V3, V12, V13, V18; restauración registrada la semana siguiente |
| 3b | Igual con días [1, 3, 5] | `REPROGRAMAR_DIAS` | — | `SIN_EFECTO` (no hay día libre) + sugerencia de días | V32 `no_effect_sessions`; sin cambios |
| 4 | Semana N: viernes acortado `TEMPORAL`. Semana N: viernes **completado** | Restaurar la referencia | Volver exactamente a la referencia | Viernes de N+1 = viernes de N−1 (+14 días) | V32 `restored_sessions`; sesión idéntica a la referencia; V20 sin exención |
| 5 | Viernes adaptado completado al 100 % | — | — | Resultado por sesión `COMPLETADA` | Vista §6: unidad = sesión adaptada; A_s = 100 |
| 6 | Viernes adaptado **omitido otra vez** por `LACK_OF_TIME` | Nueva decisión, sin restaurar | Según la subcausa nueva | Resultado `OMITIDA`, con la marca de **misma causa** | Vista §6: «la intervención no resolvió la dificultad observada» |
| 7 | `LACK_OF_TIME` **sin** responder la pregunta | Decisión D1 (recomendada: `ACORTAR` `TEMPORAL`) | Lo de hoy | No se inventa disponibilidad: no hay M ni cambio de perfil | V32 alcance `TEMPORAL`; `declared_available_minutes` nulo |
| 8 | Viernes acortado `TEMPORAL`; en la semana N la persona registra **dolor** con un ejercicio de la referencia (o lo bloquea en el perfil) | Restauración **bloqueada** para ese ejercicio | Ninguna reintroducción | Sesión sin ese ejercicio: `restauracion_no_factible` o SUST-SEG-1 | V32 `restoration_blocked_sessions` con el motivo; ningún ejercicio que dolió en la semana publicada |

---

## 10. Decisiones pendientes de aprobación

| # | Decisión | Recomendación |
|---|---|---|
| D1 | `LACK_OF_TIME` sin responder: ¿acortar `TEMPORAL` o conservar? | Acortar `TEMPORAL`: es reversible y mantiene el camino validado en A01 |
| D2 | Cuánto dura una adaptación `TEMPORAL` | Una semana del ciclo; se prolonga solo si la causa se repite |
| D3 | ¿Preguntar también al cerrar una sesión parcial por falta de tiempo? | Sí, con la misma pregunta opcional |
| D4 | Nombre y lugar del estado `conservada_por_causa` en V32 | Arreglo nuevo `conserved_by_cause_sessions`, con el mismo patrón que los demás |
| D5 | ¿Pregunta de seguimiento para `LACK_OF_MOTIVATION`? | No en esta versión |
| D6 | Ante dolor, ¿ofrecer bloquear el ejercicio en el perfil? | Ofrecerlo con confirmación, usando `blocked_exercise_ids` |
| D7 | ¿Columna `reference_planned_workout_id` o recorrer el linaje? | Columna |
| D8 | Indicador por sesión: ¿vista o columna? | Vista de solo lectura |
| D9 | Un cambio de perfil confirmado regenera la semana: ¿también si cambian los días disponibles? | Sí, por la misma razón que los minutos |

---

## 11. Implicaciones y limitaciones para la tesis

**Qué se puede sostener:**
- La adaptación responde a la causa **declarada** y a su contexto, no solo a la omisión.
- Ningún cambio se vuelve permanente sin una base (`PERSISTENTE`) o sin confirmación de la persona.
- Cada sesión adaptada tiene un resultado observable.

**Qué no se puede sostener:**
- Que una mejor adherencia posterior se deba a la IA: no hay diseño experimental con control.
- Que la disponibilidad declarada sea la real.
- Que la subcausa sea exacta: es autodeclarada, y la pregunta es opcional.

**Datos de la tesis:**
- A01 se conserva como prueba técnica histórica, con sus evidencias sin cambios, y documenta el comportamiento **anterior** a este diseño.
- Las adaptaciones medidas con el diseño nuevo deben identificarse por versión de regla (`rule_version`), para **no mezclarlas** con las anteriores, igual que se decidió con la fórmula de adherencia.

**Limitación que se mantiene:** sin IA (solo motor de reglas) no hay corrección. Este diseño mejora **qué** se le pide a la IA y **cuándo no** pedirle nada, pero no sustituye a la IA.
