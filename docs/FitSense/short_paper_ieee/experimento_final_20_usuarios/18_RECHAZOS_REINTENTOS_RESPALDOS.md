# Rechazos, reintentos y respaldos (etapa C, intento válido)

**Fuentes:** `15_GENERACION_REPLICATE.csv`, `16_ADAPTACION_REPLICATE.csv`, `14_TRAZAS_IA_REAL.jsonl` y `evidencia/C/ejecucion_maven.log`.

## Generación inicial con IA (20 gemelos)

| Concepto | Valor |
|---|---:|
| Operaciones | 20 |
| Llamadas | 22 |
| Propuestas aceptadas al primer intento | 18 |
| Propuestas rechazadas por los validadores | **2**:<br>• U05G, V24: «el ejercicio 548 lleva 3x10 con 120 s de descanso y 4D v2 exige 90 s»;<br>• U14G, V15: «dedica 4 de 6 ejercicios a upper legs» |
| Reintentos | 2, y los dos aceptados en el intento 2 |
| Respaldos al motor de reglas | **0** |
| Planes publicados | **20/20, con origen `AI`** |

## Adaptación (8 operaciones elegibles: E3 y E4)

| Concepto | Valor |
|---|---:|
| Llamadas | 13 |
| Propuestas recibidas | 13 |
| Propuestas rechazadas por los validadores | **5**. Las 5 por **V20** (la sesión propuesta queda por debajo del 70 % de los minutos): U08 (intento 1), U09 (intentos 1 y 2), U18 (intento 1) y U19 (intento 1) |
| Propuestas aceptadas | 8, una por operación |
| … con cambios efectivos (`APLICADA`) | **4**: U04, U09, U13 y U14 |
| … sin ningún cambio efectivo (`SIN_EFECTO`, API `SIN_CAMBIOS_VALIDOS`) | **4**: U03, U08, U18 y U19 |
| Reintentos | 5 |
| Respaldos (`RESPALDO`) | **0** |
| NO_FACTIBLE | 0 |
| Operaciones que la política no envía a la IA | 12: E1, E2 y E5. U07 y U12 por «causa no registrada» |

## Lo que protegieron los validadores

- **V20, duración mínima:** las 5 propuestas de adaptación rechazadas acortaban la sesión por debajo del mínimo. El backend devolvió el motivo al modelo («le faltan N s…») y en el siguiente intento el modelo propuso una semana válida o sin cambios.
- **V24, dosis por capacidad, y V15, reparto por grupos:** los 2 rechazos de la generación.
- **V8C, conservación:** ninguna sesión no señalada cambió (0 de 46).
- **Linaje V31/V32:** 0 violaciones.

**Los respaldos de la etapa A (8) no son comparables con estos:** en A la IA estaba apagada a propósito.
