# Copia semanal y mantenimiento sin IA · informe

**Fecha:** 27/10/2026 · Neon sin tocar · progresión apagada por defecto · ninguna llamada pagada a Replicate (IA simulada).

**Cambios en producción** (por orden):
1. Copia semanal sin IA (§1–§5): **ninguno**; el comportamiento ya existía y solo se añadieron pruebas.
2. Decisión 1(a), reincorporación tras al menos una progresión: `ProgresionResolver` (§6.1).
3. Decisión 2(a), reducción de carga como adaptación aplicada: `ConservationValidator` y `TipoIntervencion` (§6.2).
4. Exclusión del ciclo semanal por participante: `CicloSemanalBloqueo`, `WeeklyCycleService`, `WeeklyCycleScheduler` y `WeeklyCycleController` (§7).

Sin migraciones. V24, V25 y V31–V34 sin cambios.

## 1. Comportamiento encontrado (antes de cambiar nada)

Revisión del flujo real (`WeeklyCycleService.runFor` → `AdaptedWeekCommandService.adaptar`):

| Situación | Qué hace el código hoy | ¿IA? |
|---|---|---|
| `NO_CHANGE` / `INSUFFICIENT_DATA` | `continuidad`: copia la semana medida con fechas +7 (V31), valida (V8C + V1–V25) y publica `CONSERVED` / `SIN_ORDEN` | No |
| Progresión, recuperación, reincorporación, vuelta atrás, techo | Dentro de `continuidad`, solo con `NO_CHANGE` y el interruptor encendido: `ProgresionResolver` es puro backend; sin paso válido, la copia | No |
| `ADAPT_LOCALIZED` / `ADAPT_EXPANDED` | `corregir`: hasta 3 intentos de la IA de corrección, validados dentro del bucle | Sí |
| Respaldo (IA agotada o fallo de contrato) | Se decide sin propuesta, sin llamadas adicionales; con el proveedor caído se corta antes del tercer intento | Ninguna extra |
| Ciclo repetido | `resultingPlanOf` devuelve el plan ya publicado: ni semana nueva ni intento V32 nuevo | No |
| Sin semana medida con plan (primera semana o un hueco) | Generación inicial (`generateWeeklyPlan`), que puede usar la IA de generación | Sí (generación inicial, fuera del alcance) |

La copia crea sesiones **nuevas** (`SCHEDULED`, sin registros de ejecución) con su linaje V31; la adherencia de cada semana se mide sobre sus propias sesiones. La API expone `generationSource = CONSERVED`, que distingue una copia de una rutina generada.

**Conclusión:** lo pedido ya existía. Por eso no se tocó producción; el trabajo se limita a demostrarlo con pruebas.

## 2. Matriz de escenarios y llamadas a la IA (`CopiaSinIaIT`, 5/5)

El contador cuenta `pedirRespuesta` (la única llamada del ciclo semanal) y la prueba falla si no coincide exactamente.

| # | Escenario | Llamadas esperadas | Medidas | Además |
|---|---|---:|---:|---|
| 1 | Semana conservada | 0 | 0 | `NO_CHANGE`, `CONSERVED`, sin paso |
| 2 | Progresión autorizada | 0 | 0 | `PROGRESION` en V32 |
| 3 | Recuperación autorizada | 0 | 0 | `RECUPERACION` en V32 |
| 4 | Reincorporación autorizada | 0 | 0 | `LOSE_WEIGHT` / intermedio / 45: reincorpora solo después de al menos un paso del viernes tras acortar (1(a), §6.1) |
| 5 | Mantenimiento (sin paso posible y en el techo) | 0 | 0 | La misma rutina; `INCREASE_STRENGTH`: base [84, 84, 72] → [90, 90, 78] y se detiene (techo 92/92/79, el siguiente paso lo supera) |
| 6 | Datos insuficientes | 0 | 0 | `INSUFFICIENT_DATA`, copia idéntica, sin paso |
| 7 | Adaptación por fatiga | 1 | 1 | `APLICADA`, `ai_calls = 1` en V32; acortar por falta de tiempo: también 1 |
| 8 | Repetir el mismo ciclo, **en secuencia** (copia y adaptación) | 0 | 0 | Mismo plan, 1 plan vigente, sin intento V32 nuevo |
| 8b | Dos ciclos **simultáneos** de la misma semana | 1 (solo el primero) | 1 | Con el bloqueo (§7): 1 decisión, 1 intento V32, 1 versión de la semana; el segundo devuelve `EN_CURSO` sin llamar a la IA |
| 9 | La copia conserva | — | — | Mismos ejercicios, dosis y orden; fecha +7, mismo enfoque y minutos; sesiones nuevas sin registros; historial de la semana medida intacto |
| 10 | Invariantes | — | — | Consulta `linaje_v31_v32.sql` y restricciones de V34: sin violaciones |

## 3. Simulación de 10 semanas (`EvaluacionLimpiaProgresionIT`, 72 perfiles)

Los mismos 72 perfiles de referencia y el mismo guion (§14 de `IMPLEMENTACION_PROGRESION_V34.md`), ahora contando las llamadas a la IA por ciclo:

| Objetivo | Semanas publicadas | Ciclos sin orden | **Llamadas IA en ellos** | Ciclos con orden | Llamadas IA en ellos |
|---|---:|---:|---:|---:|---:|
| LOSE_WEIGHT | 180/180 | 144 | **0** | 36 | 64 |
| GAIN_MUSCLE | 180/180 | 144 | **0** | 36 | 72 |
| INCREASE_STRENGTH | 180/180 | 144 | **0** | 36 | 64 |
| GENERAL_FITNESS | 180/180 | 144 | **0** | 36 | 46 |

576 ciclos deterministas (copias, 312 pasos de progresión, 14 recuperaciones, 0 reincorporaciones y mantenimiento): **0 llamadas**. Solo las 144 adaptaciones llaman a la IA: **246 llamadas simuladas** en total, porque una adaptación puede usar hasta 3 intentos. En V32, `ai_calls` cuenta solo llamadas a la IA (0 en una copia), mientras que `generation_attempts` registra también el intento determinista de publicar. Evidencia: `evidencia/progresion/evaluacion_limpia.md`.

Las reincorporaciones eran 6 antes de la decisión 1(a); con ella son 0, porque esos viernes no habían dado ningún paso propio después del acortamiento (§6.1).

## 4. Regresión

- **710 unitarias y 185 de integración, 0 fallos.** 8 omitidas, todas opcionales: simulaciones de factibilidad (2), corridas pagadas de Replicate (2), lectura de Neon (3) y la evaluación limpia (1), que se ejecutó aparte con su propiedad (1/1).
- `GeneracionReferenciaIT`: **72/72 idénticos.**
- `ProgresionIT`: 17/17. `LinajeIntegridadIT`: 1/1. `CopiaSinIaIT`: 5/5.

## 5. Riesgos y diferencias pendientes

- **Adaptaciones que no pueden salir bien siguen gastando 3 llamadas.** En `GAIN_MUSCLE`, 4D rechaza cualquier reducción de dosis por fatiga (respaldo tras 3 intentos); en `INCREASE_STRENGTH`, acortar no tiene relleno que quitar. Es el flujo existente (la IA podría proponer otra cosa válida, como bajar la carga), así que no se cambió. Si se quiere ahorrar esas llamadas, hace falta una decisión aparte.
- **La generación inicial** (primera semana, o una semana sin plan medido) sigue usando la IA de generación. Está fuera del alcance de esta orden.
- **Pendiente de decisión (no tocado):** la política de recuperación de series (`IMPLEMENTACION_PROGRESION_V34.md` §14).
- **Disponibilidad M:** sigue sin existir en el backend (§11).

## 6. Revisión posterior y decisiones (27/10/2026)

### 6.1 Reincorporación: decisión 1(a), implementada

Orden: adaptación → cumplimiento sostenido → **al menos un paso de progresión (o recuperación) de esa sesión después del acortamiento** → evaluación de la reincorporación. Si la sesión nunca pudo dar un paso válido, se mantiene acortada.

- **Código:** `ProgresionResolver.pasosTrasRetirada`. Cuenta los pasos `PROGRESION`/`RECUPERACION` del linaje de la sesión posteriores al `ACORTAR` aplicado; con 0 no se propone la reincorporación. R2, el techo, la duración, la elegibilidad, la seguridad y V24/V25 siguen exigiéndose igual.
- **Pruebas:**
  - `PendientesProgresionIT.sinProgresionNoReincorpora` (`LOSE_WEIGHT` / principiante / 30, GYM y CASA): 8 semanas sin pasos tras acortar, ninguna reincorporación, el viernes se mantiene igual y 0 llamadas.
  - `CopiaSinIaIT` (escenario 4, `LOSE_WEIGHT` / intermedio / 45): mantenimiento, 6 pasos del viernes, mantenimiento y reincorporación en la semana 11.
  - `ProgresionIT` (`GAIN_MUSCLE`): reincorpora en la semana 21 tras sus pasos.
- **Efecto en la evaluación de 10 semanas:** las reincorporaciones de `LOSE_WEIGHT` pasan de 6 a 0. Esos viernes no habían dado ningún paso propio tras el acortamiento; los pasos de esas semanas fueron en otras sesiones.

### 6.2 Carga en una sesión señalada: decisión 2(a), implementada

V8C (`ConservationValidator.revisarCarga`) compara la carga prescrita de la semana medida con la propuesta **solo en sesiones señaladas** (en las demás sigue sin compararse):

| Cambio de la IA | Resultado |
|---|---|
| Bajar desde una carga prescrita, con `REVISAR_CARGA_Y_RECUPERACION` o `VARIANTE_MAS_SENCILLA_O_CARGA` (`TipoIntervencion.permiteReducirCarga`) | Cambio efectivo: la sesión cuenta como **aplicada** (APLICADA, `applied_sessions`, origen AI; cumple V33) |
| Subirla | Rechazado |
| Poner carga donde no había prescrita | Rechazado |
| Bajarla en otra intervención | Rechazado |
| Dejarla en 0 | Rechazado |
| Quitar la sugerencia (pasar a null) | Ni efecto ni rechazo |
| Cualquier otro cambio sin permiso | Rechazado por V8C como siempre |

V11 sigue juzgando aparte la carga frente al último peso anotado.

- **Pruebas:**
  - `CargaV8CTest` (10 unitarias).
  - `PendientesProgresionIT.adaptacionSoloDeCarga`: 20 → 16 kg publicado como APLICADA / AI, `applied_sessions` = la sesión, 1 llamada.
  - `subidaDeCargaRechazada`: 20 → 24 kg rechazado tras 3 intentos (respaldo) y nunca publicado.
- **Cambio de comportamiento a tener en cuenta:** antes, cualquier cambio de carga de la IA en una sesión señalada pasaba sin comprobarse (V8C la ignoraba). Ahora una subida o una carga nueva hacen que esa propuesta se rechace y la IA reintente. No depende de los interruptores de progresión: es una corrección de V8C. V24 y V25 no se tocan.

### 6.3 Otros puntos

- **Opción 2 de recuperación:** confirmada en el código y en el diseño (§3.2); prueba `serieNoRecuperableYProgresionDeOtros`.
- **Ciclos simultáneos:** antes del bloqueo, dos ejecuciones simultáneas podían publicar las dos: una versión `REPLACED` y **dos intentos V32** para la misma decisión. **Resuelto en §7.**
- **`fitsense.ai.enabled`:** en el repositorio vale `${FITSENSE_AI_ENABLED:false}`. Un `true` local viene del entorno de ejecución.

### 6.4 Regresión tras las decisiones 1(a) y 2(a)

- **720 unitarias y 191 de integración.** En la primera pasada falló solo `ciclosSimultaneos`, que destapó el hallazgo de §6.3; ajustada la prueba, vuelve a pasar (6/6). 8 omitidas, todas opcionales.
- `GeneracionReferenciaIT`: **72/72 idénticos.** `ProgresionIT`: 17/17. `CopiaSinIaIT`: 5/5. `PendientesProgresionIT`: 6/6. `CargaV8CTest`: 10/10. Pruebas de V8C (`ConservationValidator*`, `ProgresionV24V8CTest`): 75/75.
- Evaluación de 10 semanas (72 perfiles): 720/720 publicadas, 0 llamadas a la IA en los 576 ciclos sin orden; las reincorporaciones bajan de 6 a 0 por la regla 1(a).

## 7. Exclusión del ciclo semanal por participante

**Por qué no un bloqueo de transacción.** `WeeklyCycleService.runFor` va sin `@Transactional` a propósito: cada paso abre su transacción corta y la IA puede tardar ~90 s. Un `pg_advisory_xact_lock` se soltaría al terminar la primera transacción, y envolver el ciclo en una transacción larga reintroduciría los cortes por inactividad. El endpoint `-pooler` de Neon (PgBouncer en modo transacción) tampoco conserva un bloqueo de sesión.

**Cómo funciona** (`orchestration/infrastructure/locking/CicloSemanalBloqueo`):
1. **Conexión propia y directa**, abierta solo para el bloqueo con `DriverManager` (fuera del pool de Hikari) contra el endpoint directo: el `spring.datasource.url` sin `-pooler`, o `fitsense.cycle.lock.url` si se configura. Con `tcpKeepAlive`, tiempo de conexión limitado y `ApplicationName=fitsense-ciclo-semanal-bloqueo`.
2. **`pg_try_advisory_lock(clave)`**, con una clave propia del ciclo (32 bits altos «FSC1») y el participante (32 bits bajos). Personas distintas nunca se bloquean entre sí.
3. Se toma **antes** de cerrar, medir, decidir o publicar.
4. **Ya ocupado:** no se ejecuta nada; resultado explícito `EN_CURSO`. `runFor` devuelve vacío, porque no se publicó nada en esa llamada; `ejecutar` devuelve el estado; la tarea programada lo registra y reintenta, y el endpoint `run-for-me` añade `cycleStatus`.
5. **Dentro del bloqueo**, el ciclo vuelve a comprobar si la decisión ya produjo su plan y lo devuelve (`resultingPlanOf`).
6. **`finally`:** `pg_advisory_unlock` en la **misma** conexión y cierre. Si la conexión se perdió, el servidor ya soltó el bloqueo al cerrarse la sesión; se registra y no se lanza nada.
7. **Sin conexión para el bloqueo:** `SIN_BLOQUEO`; el ciclo **no** se ejecuta (no se corre sin exclusión) y se reintenta.

**Bloqueo perdido: lo que el bloqueo solo no garantiza.** Si la conexión del bloqueo se cae a mitad del ciclo, el servidor suelta el bloqueo y otra ejecución puede empezar mientras la primera sigue. La prueba lo provoca (`pg_terminate_backend` de la conexión del bloqueo de A mientras A espera a la IA lenta, y entonces arranca B). **Sin más protección, falló:** las dos ejecuciones publicaron (`intentos=[APLICADA/4, APLICADA/5]`, 2 versiones de la semana, 2 llamadas). Con el corte al empezar, en cambio, ya era seguro (B chocaba con `uq_ui_metric`).

**Corrección, sin migración: coordinación persistente en la publicación.** La transacción que publica (`publishAdapted`) empieza bloqueando la fila de la medición (`SELECT … FROM weekly_user_metrics WHERE weekly_metric_id = ? FOR UPDATE`) y comprueba si algún intento de esa medición ya publicó un plan (`resulting_plan_id IS NOT NULL` en `plan_adaptation_attempts`). Si lo hay, no escribe nada y devuelve ese plan. Dos publicaciones de la misma medición quedan en fila en la base, y la segunda ve lo que confirmó la primera; no depende de que la conexión del bloqueo siga viva. El registrador de fallos (`AdaptationFailureRecorder`) hace lo mismo en su transacción: un `NO_FACTIBLE` tardío de la ejecución que perdió el bloqueo no se registra si la otra ya publicó. El bloqueo de sesión sigue siendo la primera barrera (evita el trabajo y la llamada duplicados en el caso normal); la fila bloqueada es la que garantiza una sola publicación.

**Lo que queda (declarado):**
- Tras perder el bloqueo, **la llamada a la IA puede repetirse** (la prueba registra 2): las dos ejecuciones llegan a pedir propuesta, pero solo una publica. Con Replicate real sería un coste duplicado, no un dato duplicado.
- Si las **dos** ejecuciones acaban en `NO_FACTIBLE` (ninguna publica), quedan dos filas de fallo, igual que un reintento posterior; V32 no distingue una de otro sin un identificador de ejecución, que exigiría migración.
- La regla «una publicación por medición» se aplica a las semanas adaptadas por el ciclo; la generación inicial no pasa por aquí y no cambia.

**Pruebas** (`CicloConcurrenteIT`, 9/9, PostgreSQL efímero, IA simulada con retraso para forzar el solape):

| Prueba | Resultado |
|---|---|
| Misma persona y semana a la vez, IA lenta (3 s) | La segunda: `EN_CURSO` inmediato, 0 llamadas. Total: 1 llamada, 1 decisión, 1 intento V32 (`APLICADA`), 1 versión de la semana |
| Cinco carreras simultáneas sin IA (copia) | Siempre `[EJECUTADO, EN_CURSO]`: 1 intento `SIN_ORDEN`, 1 versión, ninguna `REPLACED` |
| **Dos instancias** de la aplicación (otro contexto Spring completo, en otro puerto) sobre la misma base | La segunda instancia ve `EN_CURSO` mientras la primera trabaja; después devuelve el mismo plan, sin evidencia nueva |
| Fallo durante el ciclo (la IA lanza) y fallo que atraviesa el ciclo (`Error`) | El bloqueo se libera en los dos casos; el reintento publica una sola vez |
| La conexión del bloqueo se corta a mitad (`pg_terminate_backend`) | El ciclo termina sin excepción, publica una vez y el bloqueo queda libre |
| **Bloqueo de A perdido mientras A espera a la IA (4 s), y B entra** | A y B: `EJECUTADO` con el **mismo** plan. 1 decisión, 1 intento V32 (`APLICADA`), 1 versión de la semana; 2 llamadas a la IA (la residual) |
| Bloqueo de A perdido al empezar (A aún decidiendo), y B entra | Igual: el mismo plan para los dos, 1 decisión, 1 intento, 1 versión |
| Fallo tardío (`NO_FACTIBLE`) de una ejecución cuando otra ya publicó | No se registra; sigue habiendo 1 intento V32 |
| Reintento después del primer ciclo | El mismo plan, 0 llamadas, sin intento V32 nuevo |
| Personas distintas a la vez | Las dos se ejecutan en paralelo (2,8 s con 2,5 s de IA cada una) |

Además: `PendientesProgresionIT.ciclosSimultaneos` vuelve a exigir 1 evidencia y 0 versiones `REPLACED`, y `CicloSemanalBloqueoTest` (3 unitarias) cubre el endpoint directo de Neon, la clave por participante y que sin conexión no se ejecuta el ciclo.

**Para desplegar:** el endpoint directo de Neon debe aceptar conexiones con las mismas credenciales (se deriva solo de la URL actual). Es una conexión más por ciclo en curso; la tarea programada procesa a los participantes de uno en uno.

### 7.1 Regresión tras el bloqueo

- **723 unitarias y 197 de integración, 0 fallos.** 8 omitidas, todas opcionales.
- `GeneracionReferenciaIT`: **72/72 idénticos.** `CicloConcurrenteIT`: 6/6. `PendientesProgresionIT`: 6/6. `CopiaSinIaIT`: 5/5. `ProgresionIT`: 17/17.

### 7.2 Regresión tras la guarda de publicación (bloqueo perdido)

- **723 unitarias y 200 de integración, 0 fallos.** 8 omitidas, todas opcionales.
- `GeneracionReferenciaIT`: **72/72 idénticos** (la generación inicial no pasa por la guarda). `CicloConcurrenteIT`: **9/9**, con las tres pruebas nuevas de bloqueo perdido. `PendientesProgresionIT`: 6/6. `CopiaSinIaIT`: 5/5. `ProgresionIT`: 17/17.
- `PublishAdaptedTest.reemplazarSinMotivoSeRechaza` comprobaba «ninguna interacción» con el repositorio de evidencia; ahora la guarda lo consulta antes, así que la prueba exige lo que de verdad importaba: que no se escriba ninguna evidencia.
