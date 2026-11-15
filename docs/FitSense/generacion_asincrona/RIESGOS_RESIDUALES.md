# Riesgos residuales (V59)

Lo que la corrección **no** elimina, con su cota y su mitigación. No se declara exactamente-una-vez.

| # | Riesgo | Cuándo | Cota | Mitigación y evidencia |
|---|---|---|---|---|
| R1 | Predicción facturada sin aprovechar (`NO_CONFIRMADA`) | Replicate acepta el POST y la respuesta con el id no llega: 5xx, lectura cortada o el proceso muere en esos milisegundos | 1 por intento afectado; no se reenvía | Se cuenta como `no_confirmadas`. Si el id llega tarde se guarda. El usuario recibe el respaldo validado. |
| R2 | Sin clave de idempotencia verificada en Replicate | Siempre | — | No se reenvía ante la duda. Si se confirma una clave, puede enviarse `prediction_row_id` sin cambiar el esquema. |
| R3 | Retención de salidas en Replicate | Se reanuda una `ENVIADA` horas después y Replicate ya no la sirve | 1 intento como fallo del proveedor | `seguir` la trata como fallo: respaldo validado, sin otro POST. La retención exacta no está verificada (sin acceso a la documentación). |
| R4 | Reintento manual tras `NO_CONFIRMADA` | El usuario reintenta y la operación nueva no tiene salida que adoptar | hasta 3 predicciones; 5 generaciones por hora (R-5) | La política es conservadora a favor del usuario. Queda contabilizado. |
| R5 | Predicción tardía de un worker con fencing que nadie reclama | El usuario no reintenta en 24 h | 1–3 predicciones | Queda `SUCCEEDED` en el libro como evidencia. La ventana es configurable (`reuse-window-hours`). |
| R6 | Cancelar no es gratis | Toda `CANCELED` o `TIMEOUT` | cómputo consumido hasta la cancelación | Se registra que se pidió y su respuesta. En la consulta de costes cuenta como posible cobro. |
| R7 | Guarda de fencing dentro del pipeline registrada como `IA_ERROR_INESPERADO` | Un worker pierde el lease a mitad del pipeline | cosmético (log) | No publica: lo detiene `ensureStillProcessing` antes de `persist`. El log puede confundir; está anotado. |
| R8 | Límite R-5 en memoria por instancia | Varias réplicas o reinicios | N × 5 por hora | Igual que antes de V59. Documentado en `LimiteDeGeneraciones`. |
| R9 | Reloj de las instancias | Varias réplicas con relojes muy desfasados | lease de 90 s | Los leases se comparan con el reloj de cada instancia. Un desfase mayor que unos 60 s podría adelantar un reencolado, aunque el fencing impide publicar dos veces y el libro impide pagar dos veces. |
| R10 | `ck_pgo_lease` es `NOT VALID` | Filas `PROCESSING` anteriores a V59 en una base real | — | Se aplica a todas las escrituras nuevas. Validarla (`VALIDATE CONSTRAINT`) queda para cuando no haya filas antiguas; no se toca ninguna base real. |
| R11 | Sin prueba con Replicate real | Latencias, formatos y retención reales | — | Requiere la prueba controlada que se propone en el informe final, autorizada aparte. |
| R12 | App Flutter no ejecutada en esta sesión | Las PR del cliente están en otro repositorio | — | El contrato no cambia y la compatibilidad se comprueba con `MockMvc` (E1/E2). |

## Sin cambios (comprobado)

- Validadores V15, V20, V24, V25 y V8C, y D8.
- P-03, incluido P-03 por colocación, que sigue desactivado.
- Progresión y reincorporación.
- Catálogo v1.1 y V57.
- H2 y la PR #69.
- La fórmula de adherencia.

El `git diff` respecto de #71 solo toca:

- la generación asíncrona;
- el generador de Replicate, en el camino nuevo con ámbito;
- `PlanGenerationPipeline.AI_ATTEMPTS`, que solo pasa de privada a pública;
- `application.properties`, con un bloque nuevo;
- V59;
- pruebas y documentación.
