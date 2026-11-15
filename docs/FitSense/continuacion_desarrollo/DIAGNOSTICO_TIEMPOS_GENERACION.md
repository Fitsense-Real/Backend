# Diagnóstico de los tiempos de generación y de la espera en Flutter

## Problema

Generar el primer plan tarda minutos. La app deja de esperar a los 30 s mientras el backend sigue trabajando. El participante ve un error y vuelve a intentarlo. El segundo intento choca con el bloqueo por persona y recibe un 409.

## Evidencia

Toda la evidencia es real o medida, sin llamadas nuevas a Replicate.

| Fuente | Qué aporta |
|---|---|
| `docs/FitSense/rendimiento_generacion/01_DIAGNOSTICO_LATENCIA.md` | Etapas medidas: GRA (56 predicciones reales, PR-1.7, gpt-5-mini, `reasoning_effort=medium`) y `TiemposGeneracionIT` (local) |
| `docs/FitSense/rendimiento_generacion/05_LIMITACIONES_DEL_PROVEEDOR.md` | Lo que no controla FitSense |
| `INFORME_CASO_39.md` (rama `audit/caso-usuario-39`), H3 | La app (`dio_client.dart`) usa `receiveTimeout: 30 s` para todas las llamadas, incluida `POST /users/me/plan/generate`. Caso real: 154 s + 211 s = 366 s en el backend; el participante vio un error y reintentó |
| Código actual (`generation`) | `TrainingPlanController#generate` (síncrono, 201 con el plan), `GeneracionInicialService` (R-5: bloqueo por persona → 409 si hay otra en curso, 429 por frecuencia), `PlanGenerationPipeline` y `ReplicateTrainingPlanGenerator` |

## Dónde se consume el tiempo

| Etapa | Quién | Tiempo |
|---|---|---|
| Inicio de la solicitud (JWT, bloqueo R-5, límite) | FitSense | Milisegundos |
| Preparación del contexto y filtrado del catálogo | FitSense | p50 30–42 ms, p95 ≤ 65 ms |
| Construcción del prompt (58.000–225.000 caracteres) | FitSense | p50 2–18 ms |
| **Solicitud al proveedor y ejecución del modelo** | **Replicate** | **p50 67,6 s, p95 133,8 s, máximo 144,9 s por predicción**. Cola: 0,015 s |
| Sondeo hasta enterarse del final | FitSense ↔ Replicate | p50 1,9 s; con la optimización de la PR #54, ~0,5 s de retraso medio |
| Validación V1–V27 | FitSense | p50 3–6 ms |
| **Reintentos** | Replicate | **Cada uno es otra predicción completa (~70 s).** Los necesitaron 13 de 40 generaciones de GRA. Parte se debe a H2 |
| Respaldo determinista | FitSense | p50 8–21 ms |
| Persistencia | FitSense | p50 33–45 ms |
| Respuesta a Flutter | FitSense | Milisegundos, si la app sigue esperando |

**Por generación completa** (REAL, GRA, 40 generaciones):

| Caso | p50 |
|---|---|
| Todas | 74,1 s (p95 263,7 s, máximo 350,1 s) |
| Válida al primer intento | 60,5 s |
| 2 llamadas | 148,2 s |
| 3 llamadas | 346,9 s |

**Conclusión:** más del 99 % del tiempo es la ejecución del modelo en Replicate. FitSense gasta unos 0,1 s.

## Timeouts, cancelaciones y duplicados (estado actual)

| Aspecto | Hoy |
|---|---|
| **Timeouts del backend** | `fitsense.ai.timeout-seconds=150` (sondeo), `connect=10`, `read=90` y `poll-read=30` por llamada. **Peor caso por intento ≈ 291 s**; con cancelación, ≈ 331 s |
| **Timeout de la app** | 30 s (`receiveTimeout`), menor que la mediana de una sola predicción (67,6 s). **La espera síncrona casi nunca llega a la app** |
| **Cancelación** | El backend cancela la predicción si agota el sondeo (R-2). Si la app se cierra, el backend **no** se entera: sigue, persiste el plan y la respuesta se pierde |
| **Duplicados** | R-5: el bloqueo consultivo por persona hace que una segunda petición simultánea reciba **409** sin llamar a la IA; un plan ya creado hace que `prepare` la rechace. Una vez persistido, reintentar no paga nada, pero la app no sabe que el plan ya existe |
| **Recuperación al reabrir la app** | El plan acaba persistido y la app lo vería con `GET` del plan vigente, pero no hay un estado de la operación que consultar. Mientras tanto, solo se distingue «en curso» por el 409 |

## Causa

El contrato HTTP es **síncrono** (`POST` que devuelve el plan) y la duración real de la operación (60–350 s) es entre 2 y 12 veces el timeout de la app. Ninguna optimización del backend lo resuelve:

- la predicción más rápida medida tardó 35,7 s;
- bajar el razonamiento o cambiar de modelo exige una evaluación pagada y cambia lo evaluado.

## Solución

Ver `PROPUESTA_FLUJO_GENERACION.md`: un flujo asíncrono con estado persistente de la operación, comparado con las alternativas.

## Pruebas

Las mediciones citadas proceden de las campañas y pruebas documentadas en `rendimiento_generacion/`. No se han medido de nuevo contra Replicate (USD 0).

## Resultado

El diagnóstico está cerrado: el cuello de botella es el proveedor y el desajuste está en el contrato.

## Pendientes

- Decidir el flujo.
- Implementarlo en una rama independiente.
- Adaptar Flutter: en esta fase no se toca.
