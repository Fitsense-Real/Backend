# Copia semanal y mantenimiento sin IA · informe

**Fecha:** 27/10/2026 · **Cambios en producción:** ninguno (solo pruebas y documentación) · Neon sin tocar · progresión apagada por defecto · ninguna llamada pagada a Replicate (IA simulada).

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
| 4 | Reincorporación autorizada | 0 | 0 | `LOSE_WEIGHT`, 2 semanas tras acortar |
| 5 | Mantenimiento (sin paso posible y en el techo) | 0 | 0 | La misma rutina; `INCREASE_STRENGTH`: base [84, 84, 72] → [90, 90, 78] y se detiene (techo 92/92/79, el siguiente paso lo supera) |
| 6 | Datos insuficientes | 0 | 0 | `INSUFFICIENT_DATA`, copia idéntica, sin paso |
| 7 | Adaptación por fatiga | 1 | 1 | `APLICADA`, `ai_calls = 1` en V32; acortar por falta de tiempo: también 1 |
| 8 | Repetir el mismo ciclo, **en secuencia** (copia y adaptación) | 0 | 0 | Mismo plan, 1 plan vigente, sin intento V32 nuevo |
| 8b | Dos ciclos **simultáneos** de la misma semana (`PendientesProgresionIT`) | — | — | 1 semana vigente. **La evidencia V32 puede duplicarse** (hallazgo abierto, §6.3) |
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

576 ciclos deterministas (copias, 312 pasos de progresión, 14 recuperaciones, 6 reincorporaciones y mantenimiento): **0 llamadas**. Solo las 144 adaptaciones llaman a la IA: **246 llamadas simuladas** en total, porque una adaptación puede usar hasta 3 intentos. En V32, `ai_calls` cuenta solo llamadas a la IA (0 en una copia), mientras que `generation_attempts` registra también el intento determinista de publicar. Evidencia: `evidencia/progresion/evaluacion_limpia.md`.

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
- **Ciclos simultáneos: HALLAZGO ABIERTO.** La repetición **secuencial** devuelve el mismo plan, sin duplicar nada. Con dos ejecuciones **simultáneas** de la misma semana, el resultado depende de quién gane la carrera. Se observaron tres:
  1. la segunda falla con un error de base de datos (`uq_wum_user_week`);
  2. la segunda devuelve vacío;
  3. **las dos publican:** queda una sola semana vigente (la otra versión pasa a `REPLACED`), pero hay **dos intentos V32 `SIN_ORDEN` para la misma decisión** (regresión del 27/10).

  Lo garantizado hoy es una sola semana vigente; la evidencia puede duplicarse. No es un efecto de la progresión: es el ciclo existente. La tarea programada recorre a los participantes uno a uno, así que solo ocurre si se lanza el ciclo dos veces a la vez (dos instancias o un relanzamiento manual simultáneo). **Corrección propuesta, sin implementar:** un bloqueo por participante al empezar `runFor` (`pg_advisory_xact_lock`, solo código) o una restricción única de intentos publicados por decisión (migración nueva). Necesita tu decisión.
- **`fitsense.ai.enabled`:** en el repositorio vale `${FITSENSE_AI_ENABLED:false}`. Un `true` local viene del entorno de ejecución.

### 6.4 Regresión tras las decisiones 1(a) y 2(a)

- **720 unitarias y 191 de integración.** En la primera pasada falló solo `ciclosSimultaneos`, que destapó el hallazgo de §6.3; ajustada la prueba, vuelve a pasar (6/6). 8 omitidas, todas opcionales.
- `GeneracionReferenciaIT`: **72/72 idénticos.** `ProgresionIT`: 17/17. `CopiaSinIaIT`: 5/5. `PendientesProgresionIT`: 6/6. `CargaV8CTest`: 10/10. Pruebas de V8C (`ConservationValidator*`, `ProgresionV24V8CTest`): 75/75.
- Evaluación de 10 semanas (72 perfiles): 720/720 publicadas, 0 llamadas a la IA en los 576 ciclos sin orden; las reincorporaciones bajan de 6 a 0 por la regla 1(a).
