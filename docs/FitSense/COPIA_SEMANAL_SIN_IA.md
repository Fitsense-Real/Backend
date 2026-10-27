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
| 8b | Dos ciclos **simultáneos** de la misma semana (`PendientesProgresionIT`) | — | — | 1 plan vigente y 1 evidencia publicada. La ejecución que pierde la carrera falla con una excepción (`uq_wum_user_week`, la métrica semanal) antes de publicar; la tarea programada la captura por participante y sigue con los demás |
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

## 6. Revisión posterior (27/10/2026)

- **Adaptación que solo baja la carga** (`PendientesProgresionIT.adaptacionSoloDeCarga`): viernes parcial por fatiga, con 20 kg anotados; la IA propone 16 kg sin tocar la dosis. **Se publica** (la sesión sale con 16 kg), pero el intento queda como `SIN_EFECTO` / `CONSERVED`, porque V8C excluye la carga al clasificar. La semana publicada no coincide con lo que dice la evidencia. **Pendiente de decisión** (no se tocó código).
- **Reincorporación sin progresión previa:** `LOSE_WEIGHT` / principiante / 30 min reincorpora el relleno sin haber dado ningún paso, porque el menor paso supera el 5 % y la reserva no admite ninguno. **Pendiente de decisión** sobre la regla (diseño §5, condición 2).
- **`fitsense.ai.enabled`:** en el repositorio vale `${FITSENSE_AI_ENABLED:false}`. Un `true` local viene del entorno de ejecución, no del código.
