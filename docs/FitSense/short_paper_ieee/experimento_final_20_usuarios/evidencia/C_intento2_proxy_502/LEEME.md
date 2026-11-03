# Etapa C, intento 2: 7 de 12 respuestas perdidas por el proxy del entorno (502 a los 30 s)

| Campo | Valor |
|---|---|
| Fecha | 2026-11-03, de 01:42 a 01:48 UTC |
| Laboratorio | `fbfa723`. Credencial inyectada por el proxy; `prefer-wait-seconds` = 60, el valor por defecto del backend |
| Predicciones creadas en Replicate | **12**: las 12 terminaron bien en Replicate (una seguía procesándose al consultarla). Registro de cada una en `predicciones_replicate/` |
| Respuestas recibidas por el backend | **5** (de 7 a 27 s) |
| Respuestas perdidas | **7**. El proxy del entorno devolvió `502 Bad Gateway: "upstream request failed"` a los ~30 s, mientras Replicate tardaba entre 32 y 93 s. El backend lo trata como proveedor no disponible |
| Disyuntor del backend | Tras 3 fallos seguidos dejó de llamar durante 10 minutos, como está diseñado:<br>• en la adaptación, U19 = `RESPALDO_SIN_IA`;<br>• en la generación, U04G–U20G usaron el motor de reglas sin llamar |
| Tokens (métricas de Replicate) | 437.900 de entrada y 41.784 de salida, en 11 predicciones terminadas |
| Coste estimado | **US$ 0,19** con la tarifa pública de GPT-5 mini (0,25 / 2,00 USD por millón). La cifra oficial está en el panel de Replicate |
| Categoría | **E (integración), por el entorno.** La espera síncrona de 60 s supera el límite de 30 s del proxy de esta sesión. No es un fallo de Replicate ni de la lógica del backend |
| Qué se hizo después | Anexo 2 del protocolo: repetir la etapa C **completa** con `fitsense.ai.prefer-wait-seconds=25`. El POST responde antes de 30 s y el backend sigue con su sondeo. No cambia ni el modelo, ni el *prompt*, ni la política, ni los validadores. Límite: 78 predicciones (90 − 12) |

Se conserva sin editar. Los datos de U01–U20 de este intento son válidos como observación del comportamiento ante un proveedor que no responde (respaldos y disyuntor), pero **no** como medida de la calidad de las propuestas de la IA.
