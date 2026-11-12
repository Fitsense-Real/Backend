# 04 · Lo que depende del proveedor

## La meta de 30 s

**Con la configuración evaluada no se alcanza, y no se presenta como alcanzada.** Es configuración del estudio, no del código: gpt-5-mini en `openai/gpt-5-structured`, PR-1.7, `reasoning_effort=medium` y el catálogo elegible en el prompt.

- **La predicción de generación más rápida medida tardó 35,7 s** y la mediana es de 67,6 s (REAL, GRA, 56 predicciones). El modelo produce de 3.800 a 13.500 tokens de salida, incluido el razonamiento.
- **Lo que queda en manos de FitSense ya es pequeño:** unos 0,1 s propios y unos 0,5 s de sondeo de media.
- **Un éxito al primer intento tarda hoy unos 60 s:** p50 real de 60,5 s, que con esta rama pasa a ~59,9 s según la reproducción de trazas.

## Qué acercaría la generación a 30 s

Todas estas palancas exigen una evaluación pagada nueva antes de adoptarse, porque cambian lo que propone el modelo:

| Palanca | Efecto esperado | Riesgo |
|---|---|---|
| `reasoning_effort=low` (`FITSENSE_AI_REASONING_EFFORT`) | Menos tokens de razonamiento: es la variable que más pesa en el tiempo | Más propuestas rechazadas (V15, V26), que suman reintentos. No está medido |
| Modelo más rápido (`FITSENSE_AI_MODEL`, p. ej. gpt-5-nano) | Predicciones más cortas | En campañas anteriores, peor validez al primer intento |
| Catálogo más pequeño en el prompt (entrada p95 de 78.700 tokens) | Menos tiempo de lectura | Cambia PR-1.7: no autorizado |
| Generación asíncrona (202 + consulta) | La espera deja de bloquear la petición HTTP; la percibida puede cubrirse con la interfaz | Cambia el contrato HTTP y la app |

## Límites de Replicate que el código no controla

- **Duración y varianza de la predicción:** de 35,7 s a 144,9 s con el mismo modelo y prompt.
- **Cuánto retiene el POST:** con `Prefer: wait=60` se observaron unos 20,5 s.
- **Disponibilidad:** las caídas siguen yendo al respaldo (R-1) y al cortacircuitos.
- **Cancelar no garantiza coste cero** (R-2). Una predicción cortada por el presupuesto puede facturar lo ya procesado.
- **Límites de peticiones de la cuenta:** el sondeo a 1 s hace unas 3 veces más GET por predicción (~59 frente a ~20). Con pocas generaciones simultáneas (límite de 5 por hora y persona, y exclusión por persona de R-5) no debería acercarse a los límites de la API. Conviene confirmarlos en la cuenta antes de abrir a más usuarios; si hiciera falta, `FITSENSE_AI_POLL_INTERVAL_MILLIS` vuelve a 3000 sin desplegar código.
- **Ninguna cifra de esta rama se ha medido contra Replicate real después del cambio** (USD 0). Las mejoras del sondeo y del presupuesto son simulaciones sobre tiempos reales.
- La primera ronda del entorno de pruebas debería revisar `GENERACION_TIEMPOS` y `REPLICATE_TIMING` para confirmarlas.
