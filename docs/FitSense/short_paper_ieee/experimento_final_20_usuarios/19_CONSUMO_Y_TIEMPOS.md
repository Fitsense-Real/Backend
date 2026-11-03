# Consumo y tiempos

| Concepto | Valor | Fuente |
|---|---|---|
| Llamadas reales a Replicate | **0** | `evidencia/*/99_resumen.txt` (`llamadas_proveedor=0`) |
| Consumo del proveedor | **US$ 0** | No hubo llamadas |
| Límite autorizado | USD 10 o 90 solicitudes | Orden §10.3 |
| Consumo previsto de la etapa C | ≤ 84 llamadas. Unos US$ 1,7 a la tasa observada en la campaña MVP (~US$ 0,02 por llamada, estimación por tokens) | `13_CONFIGURACION_REPLICATE.md` |

## Tiempos de la etapa determinista

| Ejecución | Duración de la prueba (20 usuarios) | Fuente |
|---|---|---|
| A0 | 42,3 s | `evidencia/A0/ejecucion_maven.log` |
| A1 | Ver el registro | `evidencia/A1/ejecucion_maven.log` |
| A1, repetición | Ver el registro | `evidencia/A1_repeticion/ejecucion_maven.log` |

La duración incluye arrancar el contexto de Spring y migrar 46 versiones en la base efímera. Los tiempos de respuesta del modelo solo existirán con la etapa C.
