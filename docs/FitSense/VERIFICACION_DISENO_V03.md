# Verificación de las premisas del diseño v0.3

| | |
|---|---|
| Fecha | 27/10/2026 |
| Diseño | `DISENO_ADAPTACION_CONTEXTUAL.md` v0.3 (`6120808`), que **no se modifica** |
| Alcance | Comprobaciones sobre el código y PostgreSQL. **Sin cambios de código de producción**: solo una prueba de verificación y una consulta de solo lectura |
| Base de datos | PostgreSQL 16 real (Testcontainers) con todas las migraciones, y datos producidos por el **ciclo de producción** a lo largo de varias semanas. **No se consultó Neon:** no hay credenciales en este entorno y está fuera de los límites acordados. La consulta queda lista para ejecutarla allí en solo lectura |

## Resumen

| Comprobación | Resultado |
|---|---|
| B. ¿`NO_CHANGE` publica? | **Sí**: el ciclo publica una versión nueva (`SIN_ORDEN`) también con `NO_CHANGE` e `INSUFFICIENT_DATA`. Una restauración puede publicarse ahí |
| C. Integridad de V31 + V32 | **0 violaciones**; los 18 pasos de linaje producidos por el ciclo se clasifican en un solo tipo. El recorrido de 3 semanas llega a la referencia correcta. Una derivación sin evidencia **se detecta** |
| D. Sesión al 25 % | Aporta **0,25** a la adherencia oficial (`counts_toward_adherence = true`), pero su entrenamiento queda en estado `SKIPPED`. El indicador debe clasificar por la sesión, no por el estado |
| D5. RPE ≥ 8 | **No es apropiado** como bloqueo: es un RPE global y opcional de la sesión, y el umbral se aplica al **promedio semanal**, solo como factor descriptivo de riesgo. Recomendación: no usarlo |
| **Defecto nuevo (comportamiento actual)** | **Un acortamiento aplicado deja la semana siguiente sin plan** cuando se copia la sesión acortada (V20). Se reproduce en la prueba. Corrección mínima propuesta, **sin aplicar** |

Prueba: `LinajeIntegridadIT` (orchestation). Consulta: `docs/FitSense/consultas/linaje_v31_v32.sql`.

---

## A. Restauración simultánea con una adaptación

**Hallazgo.** Coincide con la observación de la revisión: la v0.3 no fija qué pasa si en el mismo ciclo hay una sesión señalada que se adapta y otra que cumple para restaurarse.

**Corrección mínima propuesta: una modificación funcional por ciclo publicado.**

| Desenlace de la sesión señalada en el ciclo | ¿Restauración? |
|---|---|
| `APLICADA` (al menos una sesión cambió) | **No.** Las restauraciones elegibles quedan pendientes: siguen copiándose y el recorrido las vuelve a encontrar |
| `SIN_EFECTO` (incluida la conservación por causa) | **Sí, una**, validada |
| `SIN_ORDEN` (`NO_CHANGE` o `INSUFFICIENT_DATA`) | **Sí, una**, validada |
| `RESPALDO` (sin IA) | **No**: `RESPALDO` es por definición una copia exacta; se pospone |
| `NO_FACTIBLE` | No hay publicación |

- **Si hay varias elegibles:** se restaura la de adaptación más antigua y, a igualdad, la de fecha más temprana. Es una regla de orden, no un umbral.
- **Invariantes que se mantienen:**
  - una restauración nunca entra en `applied_sessions`, así que `APLICADA` ⇔ `applied_sessions` no vacío (V33) no cambia;
  - `restored_sessions` es disjunto de los demás arreglos;
  - V8C debe autorizar el cambio en una sesión no señalada solo si coincide exactamente con la referencia.

## B. Publicación con `NO_CHANGE`

**Verificado en el código y en la prueba.** Con una semana medida, el ciclo **siempre** deriva y publica (`WeeklyCycleService.runFor` → `adaptWeeklyPlan`). Con `NO_CHANGE` e `INSUFFICIENT_DATA` no llama a la IA y registra `SIN_ORDEN`, con origen `CONSERVED` y un plan nuevo. En la prueba, el participante A publicó tres semanas seguidas: `APLICADA`, `SIN_ORDEN` y `SIN_ORDEN`.

**Qué falta para restaurar ahí.** Hoy V8C exige que en `SIN_ORDEN` toda sesión sea **idéntica** a la medida. La restauración necesita la autorización ya prevista en la v0.3 (§6.1).

**Día de la semana.** Al restaurar un día reprogramado se recupera el **día de la semana** de la referencia dentro de la semana nueva (el lunes de la semana más el día de la referencia menos uno), nunca su fecha absoluta. Después se revalida con los días disponibles vigentes (V3) y con V12, V13 y V18 frente a las demás sesiones de esa semana. Si el día está ocupado o ya no está disponible, no se restaura.

## C. Integridad del recorrido V31 + V32

**Escenario.** Ciclo de producción y PostgreSQL real, con la IA de corrección simulada:

| Participante | Semanas | Desenlaces |
|---|---|---|
| A (`GAIN_MUSCLE`, días [1, 3, 5, 6]) | 4 | `APLICADA` (reprogramada) → `SIN_ORDEN` (`NO_CHANGE`) → `SIN_ORDEN` (`INSUFFICIENT_DATA`) |
| B (`INCREASE_STRENGTH`) | 2 | `SIN_EFECTO` |
| E (`GAIN_MUSCLE`) | 3 | `APLICADA` (acortada) → `NO_FACTIBLE` (defecto, ver más abajo) |
| C (`LOSE_WEIGHT`, sin IA) | 2 | `RESPALDO` |
| D (`GAIN_MUSCLE`, dolor, sin IA) | 1 | `NO_FACTIBLE` (sin plan) |

**Resultados:**

| Premisa | Resultado |
|---|---|
| 1. Cada plan derivado publicado tiene **exactamente un** intento V32 con su `resulting_plan_id` | Sí: 0 violaciones |
| Un solo plan de origen por plan derivado, y coincide con el `source_plan_id` del intento | Sí |
| Ningún plan publicado por un intento tiene sesiones sin origen V31 | Sí |
| 2. Cada sesión derivada cae en **una sola** clase de V32 | Sí: 18 pasos = 14 `COPIA` + 2 `SIN_EFECTO` + 1 `ADAPTADA:REPROGRAMAR_DIAS` + 1 `ADAPTADA:ACORTAR_O_REORGANIZAR` |
| Recorrido desde la semana 4 de A | `COPIA` → `COPIA` → `ADAPTADA:REPROGRAMAR_DIAS` → **referencia en la semana 1** |
| 3. Una derivación **sin evidencia** se detecta | Sí: al borrar el intento de la semana 3 de A, la consulta informa `PLAN_DERIVADO_SIN_INTENTO_UNICO` para ese plan |

**Qué demuestra:** con los datos que produce el sistema actual, el recorrido es inequívoco, y la regla «si no se puede clasificar, no se restaura» tiene cómo detectarlo.

**Qué no demuestra:** el estado de **Neon**. Antes de activar la restauración hay que ejecutar `linaje_v31_v32.sql` allí, en solo lectura, y esperar 0 violaciones. También hay que revisar la reparación por seguridad (`PublishedPlanSafetyRepairService`, hoy sin uso) si llega a activarse.

## D. El indicador y la sesión al 25 %

**Verificado:**
- Toda sesión **cerrada** cuenta (`WorkoutSession.finish` pone `counts_toward_adherence = true`, sea cual sea el porcentaje). Solo las abandonadas y las desplazadas por un reintento dejan de contar.
- En la adherencia oficial aporta su A_s real: una sesión al 25 % suma 0,25. Lo prueba `AdherenciaIT` («por debajo del 30 %: el entrenamiento queda SKIPPED pero aporta su 25 %»).
- **Pero** su entrenamiento previsto queda en `SKIPPED`.

**Corrección del indicador (§4 de la v0.3):**
- A_s es **el mismo valor** que usa la adherencia oficial: la sesión que cuenta, o 0 sin sesión.
- La clase se decide **primero por la sesión** (completada, parcial válida o parcial no válida) y **solo sin sesión** por la omisión declarada.
- Una sesión al 25 % es «parcial no válida» con A_s = 0,25, nunca «omitida».

Así, el cumplimiento oficial del indicador usa exactamente la semántica de A_w, restringida a las sesiones adaptadas.

## D5. ¿RPE ≥ 8 como bloqueo de la restauración por fatiga?

**Qué existe hoy:**
- `session_rpe` es un **RPE global de la sesión**, de 1 a 10 y **opcional** (`ck_session_rpe`). No hay RPE por ejercicio ni por serie.
- El umbral `rpe_threshold` = 8 de MVP-1.8 se aplica al **promedio semanal** de las sesiones que cuentan y solo suma 10 puntos al riesgo descriptivo (`overexertion`). No decide ninguna adaptación.

**Por qué no sirve como bloqueo:**
1. Es otro uso: pasaría de un promedio semanal a una sesión individual, y de describir a decidir.
2. Un RPE de sesión alto es esperable en un entrenamiento de hipertrofia exigente y no indica fatiga persistente. Bloquear con él podría mantener reducida **indefinidamente** la dosis de alguien que ya cumple.
3. Al ser opcional, su ausencia no informa nada.

**Recomendación (cierra D5):** **no usar el RPE.** Las señales incompatibles para la fatiga quedan solo en lo **declarado** en esa sesión: algún ejercicio registrado con causa fatiga o dolor. A eso se suma la exigencia de completarla (A_s ≥ 0,80). No hay umbral nuevo.

---

## Defecto encontrado: un acortamiento aplicado deja la semana siguiente sin plan

**Comportamiento actual (ADAPT-1), reproducido en `LinajeIntegridadIT` (participante E):**
1. Viernes omitido por falta de tiempo. `ACORTAR_O_REORGANIZAR` retira un relleno y la sesión queda en unos 30 minutos, por debajo del piso de V20 (31 = 70 % de 45). Se publica porque V8C la exime de V20 **solo esa semana** (`fechas_con_duracion_reducible`).
2. La persona completa toda la semana adaptada (100 %, `NO_CHANGE`).
3. El ciclo copia la semana. La copia del viernes **ya no está exenta**, V20 la rechaza y el resultado es `NO_FACTIBLE`: «V20: el entrenamiento del 2026-11-20 dura unos 30 minutos y el minimo es 31». **La semana queda sin plan.**

En el escenario de A01 pasaría lo mismo: su viernes adaptado dura 28 minutos, así que la semana posterior a la adaptada no se habría publicado.

**Impacto:**
- Toda adaptación que acorta por debajo del piso de V20 hace fallar la publicación siguiente, cualquiera que sea la adherencia: un participante que cumple el 100 % se queda sin plan.
- No ocurre sin IA (`RESPALDO` no acorta), y no se ha visto en producción porque la adaptación con IA real solo se ejecutó en pruebas (A01).

**Corrección mínima propuesta (sin aplicar):**
- La exención de V20 se extiende a la **copia** de una sesión acortada con autorización. Se reconoce con el mismo recorrido V31 + V32: retroceder por copias hasta un paso `ADAPTADA:ACORTAR_O_REORGANIZAR`.
- Va en el informe de V8C (`fechas_con_duracion_reducible`), que ya es el mecanismo de la exención. No cambia el umbral de V20 ni V24, V25 o V8C para el resto.
- **Coherencia con la v0.3:** el acortamiento por una disponibilidad declarada se **conserva**, así que su copia debe poder publicarse.
- **Si se clasifica mal:** si el recorrido no puede clasificarse, no hay exención, y la semana queda `NO_FACTIBLE` como hoy. Falla de forma segura, sin publicar algo no validado.
- **Pruebas exigidas:** el escenario E debe publicar la semana 3; una copia de una sesión que no fue acortada sigue sujeta a V20; y una derivación sin evidencia no recibe la exención.

**Decisión pendiente:** autorizar esta corrección. Es un defecto del comportamiento actual, previo al diseño v0.3, y afecta a A01 y a cualquier prueba nueva con `ACORTAR`.

---

## Estado de las decisiones

| # | Estado |
|---|---|
| D1, D4 | Aprobadas |
| D2 | Aprobada condicionada. Las invariantes se mantienen con la política de §A |
| D3 | Aprobada con la condición de evidencia positiva |
| D5 | **Recomendación: cerrar sin RPE** (§D5) |
| D6 | Aprobada |
| D7 | Aprobada; va en la migración V35 junto con lo demás |
| **D8** (nueva) | Corrección del defecto de V20 en la copia de una sesión acortada: **pendiente de autorización** |
