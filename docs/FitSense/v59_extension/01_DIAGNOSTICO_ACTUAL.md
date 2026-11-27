# 01 · Diagnóstico actual: qué caminos de IA estaban protegidos

**Orden:** «ORDEN DE TRABAJO — BACKEND: AMPLIACIÓN V59 Y CONTROL DE PREDICCIONES REPLICATE».
**Base:** `generation` en `77b9df3d` (PR #88 fusionada). Prompt PR-1.7 y PR-2.2 inactiva: en `generation` no existe y `PlanPromptBuilder.VERSION = "PR-1.7"`. Sin credenciales en el repositorio.

## 1. Qué es V59

V58 y V59 (`plan_generation_operations`, `plan_generation_predictions`) dan a la **generación inicial asíncrona** (`POST /api/v1/users/me/plan/generation`) tres piezas:

| Pieza | Qué hace |
|---|---|
| Operación persistente | `PENDING` → `PROCESSING` → `COMPLETED`, `FAILED` o `NO_FACTIBLE`. La reclama un worker con `FOR UPDATE SKIP LOCKED` |
| Propietario con lease | `lease_owner` y `lease_expires_at`. Un hilo propio renueva el lease (latido). El **fencing** impide enviar o publicar a un worker que ya no es el propietario |
| Libro de predicciones | `DiarioDePredicciones`. Cada intento se anota **antes** de enviarse (`SOLICITANDO`) y se confirma con el id (`ENVIADA`) |

**Qué pasa con cada intento al reanudar:**

| Estado del intento | Al reanudar |
|---|---|
| `SUCCEEDED` | Se reutiliza la salida |
| `ENVIADA` | Se sigue la predicción por su id |
| `SOLICITANDO` | Pasa a `NO_CONFIRMADA`: no se reenvía |
| Otro final | Se repite el desenlace |

El barrido (`recoverStale`) devuelve a la cola las huérfanas con el lease vencido, hasta `max-executions`.

**Desde PR #88:** el transporte recupera una predicción **solo por el id de su propia respuesta**, nunca por contenido.

## 2. Los caminos que llaman a Replicate

| # | Camino | Entrada | ¿Operación V59? | ¿Libro? | ¿Sobrevive a un reinicio? |
|---|---|---|---|---|---|
| 1 | Generación inicial **asíncrona** | `POST /api/v1/users/me/plan/generation` → `PlanGenerationOperationService` → `FirstPlanGenerationRunner` | **Sí** (`FIRST_PLAN`) | **Sí** | **Sí** |
| 2 | Generación inicial **síncrona** | `POST /users/me/plan/generate` → `GeneracionInicialService` → `prepare`, `generate`, `persist` | No | **No** | **No**: si el proceso muere con una predicción en curso, no queda rastro. La siguiente petición paga otra |
| 3 | **Cierre semanal**, generación | `WeeklyCycleScheduler` (lunes 03:00 local) → `WeeklyCycleService` → `PlanningContextFacade.generateWeeklyPlan` (pipeline) | No | **No** | **No**. Además, **el ciclo no se relanza**: la tarea solo corre a las 03:00 del lunes |
| 4 | **Cierre semanal**, adaptación | `WeeklyCycleService` → `adaptWeeklyPlan` → `AdaptedWeekCommandService` → `ReplicateSessionFixGenerator` (correcciones de sesión) | No | **No** | **No**: igual que el 3 |
| 5 | Regeneración segura (P-03) | `SafeRegenerationService` → `pipeline.runDeterministic` | — | — | **No llama a la IA**: motor de reglas |
| 6 | Relanzamiento manual del ciclo | `WeeklyCycleController` → `WeeklyCycleService` | Como el 3 y el 4 | | |

**Todo pasa por un único transporte,** `ReplicateTrainingPlanGenerator.call`:
- la adaptación entra por `transporte.call(...)`;
- la generación, por `requestOutput`.

El libro solo actúa si hay un ámbito abierto (`DiarioDePredicciones.actual()`). Solo el camino 1 lo abría.

## 3. Protecciones que ya tenían los caminos 2, 3 y 4

| Protección | Dónde | Qué cubre | Qué no cubre |
|---|---|---|---|
| Bloqueo consultivo de sesión por persona (`CicloSemanalBloqueo`) | Camino 2 y ciclo semanal | Dos ejecuciones **simultáneas** de la misma persona, entre hilos e instancias | Un reinicio: el bloqueo se suelta y la siguiente ejecución vuelve a pagar |
| `prepare` rechaza si ya hay plan | Camino 2 | Un primer plan duplicado | — |
| «Ciclo semanal repetido» (`resultingPlanOf`) y `publishAdapted`, que bloquea la medición y no publica dos veces | Ciclo | Una **publicación** duplicada | Predicciones duplicadas. Como dice su propio comentario: «Lo que si puede repetirse en ese caso es la llamada a la IA» |
| Límite de frecuencia (`LimiteDeGeneraciones`) | Camino 2 | Abuso | — |

## 4. Identificadores de predicción

- El id solo se guardaba en `plan_generation_predictions.replicate_prediction_id`, y solo en el camino 1.
- En los caminos 2, 3 y 4 vivía únicamente en memoria (`Medicion`) y en los registros (`REPLICATE_TIMING`).

## 5. Cancelación y respaldo

| Mecanismo | Comportamiento |
|---|---|
| Cancelación (R-2) | Una predicción que se deja de esperar **con su id** se cancela una vez |
| Sondeo (PR #88) | Tolera 3 fallos seguidos |
| Respaldo `RULE_ENGINE` | Tras un fallo del proveedor, o tras 3 propuestas inválidas |
| Adaptación | Su respaldo es la copia conservada (`RESPALDO_IA`) y su propia evidencia V32 |

## 6. Conclusión

Faltaba **el mismo seguimiento V59** en los caminos 2, 3 y 4, y un **relanzamiento** del ciclo semanal interrumpido.

Los componentes de V59 se pueden reutilizar tal cual:
- la tabla de operaciones;
- el lease;
- el fencing;
- el libro.

No hace falta otra arquitectura (`02`).
