# Riesgos residuales de la generación asíncrona (#71 + #72)

Esto es lo que la corrección **no** elimina, con su cota y su mitigación. No se declara exactamente-una-vez.

| # | Riesgo | Cuándo | Cota | Mitigación y evidencia |
|---|---|---|---|---|
| R1 | Una predicción facturada sin aprovechar (`NO_CONFIRMADA`) | Replicate acepta el POST y la respuesta con el id no llega: 5xx, lectura cortada o un proceso que muere en esos milisegundos | 1 por intento afectado; nunca se reenvía | Contada (`no_confirmadas`) y registrada con hora. Si el id llega tarde, se guarda. El usuario recibe el respaldo validado (R07). |
| R2 | No hay una clave de idempotencia ni una conciliación automática verificadas en Replicate | Siempre | — | No se reenvía ante la duda. La conciliación es manual (`RECUPERACION_PREDICCIONES.md` §3). Si se confirma una clave, puede enviarse `prediction_row_id` sin cambiar el esquema. |
| R3 | Retención de las salidas en Replicate | Se reanuda una `ENVIADA` horas después y Replicate ya no la sirve | 1 intento como fallo del proveedor | `seguir` lo trata como un fallo: respaldo validado, sin otro POST. La retención exacta no está verificada. |
| R4 | Reintento manual tras un `NO_CONFIRMADA` con el respaldo también fallido | El usuario reintenta una operación `FAILED` | hasta 3 predicciones; 5 generaciones por hora (R-5) | Requiere una acción del usuario. Queda contabilizado. |
| R5 | Una predicción tardía de una operación vencida que nadie reclama | El usuario no reintenta en 24 h | 1–3 predicciones | Queda `SUCCEEDED` en el libro como evidencia. La ventana es configurable. |
| R6 | Cancelar no es gratis | Toda `CANCELED` o `TIMEOUT` | Cómputo consumido hasta la cancelación | Se registra que se pidió y la respuesta. La consulta de costes la cuenta como posible cobro. |
| R7 | Un worker que pierde el lease a mitad del pipeline registra `IA_ERROR_INESPERADO` y calcula un respaldo que no publicará | Pausa larga, partición de red o vencimiento | Unos ms de CPU y una línea de log engañosa | No publica, no paga y no toca el libro (R04 y R17). Solo cosmético; el pipeline no se ha tocado para no alterar el camino síncrono. |
| R8 | Una predicción en curso cuando se pierde el lease sigue sondeándose hasta terminar | Lo mismo que R7 | ≤ 331 s de GET, sin crear otra | Su resultado se anota en el libro y lo aprovecha el propietario actual o un reintento. |
| R9 | El límite R-5 vive en memoria y es por instancia | Varias réplicas o reinicios | N × 5 por hora | Igual que antes de #71. |
| R10 | Desfase de reloj entre instancias | Varias réplicas con relojes muy distintos | Lease de 90 s | Los leases se comparan con el reloj de la instancia. Un desfase grande podría adelantar un reencolado, pero el fencing impide publicar y el libro impide pagar dos veces. |
| R11 | `ck_pgo_lease` es `NOT VALID` | Filas `PROCESSING` anteriores a V59 en una base real | — | Las recupera el barrido (lease nulo = vencido, `MigracionV58aV59IT`). Después se puede validar. No se ha tocado ninguna base real. |
| R12 | Espera máxima visible | Un defecto que mantiene la operación en cola y luego en proceso | ≤ 60 min (30 + 30) | Nunca infinita. Lo típico, medido, es 75–350 s. |
| R13 | Sin prueba con Replicate real | Latencias, formatos y retención reales | — | Prueba controlada propuesta en `DECISION_PREPARACION_IA_REAL.md` |
| R14 | La app Flutter no se ejecutó en esta sesión | Las PR del cliente están en otro repositorio | — | El código de MobileApp#2 se leyó y es compatible. El contrato no cambia (`COMPATIBILIDAD_FLUTTER.md`). |
| R15 | Número de réplicas en Railway desconocido | — | — | El diseño admite varias instancias (R17). No se ha tocado Railway. |

## Defectos encontrados en esta validación y corregidos en #72

| Defecto | Efecto | Corrección | Prueba de regresión |
|---|---|---|---|
| Una `PROCESSING` de V58 (lease nulo) no se reencolaba | Tras desplegar #72 con una generación en curso, esa operación esperaba 30 min y terminaba en `TIEMPO_EXCEDIDO` | Lease nulo = vencido | `MigracionV58aV59IT`: falla sin la corrección y pasa con ella |
| La propiedad se comprobaba antes de publicar, fuera de la transacción | Una ventana mínima para que un worker sin lease publicara (la frenaba `ux_plan_active`) | `SELECT … FOR UPDATE` y `persist` en una sola transacción | `muereDuranteLaPublicacion`, `dosInstanciasNoDuplicanElTrabajo` |
| Carrera al anotar el mismo intento: `solicitar` descartaba cualquier fila vigente | Dos ejecuciones simultáneas podían enviar dos POST para el mismo intento | Solo se descarta otra petición; la misma choca con `ux_pgp_intento_vigente` y se sigue la existente | `carreraConOtraEjecucion*`, `libroNoAdmiteDosVigentesDeLaMismaPeticion` |
| Un worker sin lease podía reanudar y dar por `NO_CONFIRMADA` la fila del propietario | El libro podía quedar mal (sin coste añadido) | El fencing se comprueba al empezar cada intento | `sinPropiedadNoSeTocaElLibro`, R17 (0 `NO_CONFIRMADA`) |
| El barrido corría en cada ciclo (cada 2 s por instancia) | Carga innecesaria en la base | `recover-ms` = 30 s | `elBarridoNoCorreEnCadaCiclo` |

## Sin cambios (comprobado)

No se han modificado:

- los validadores V15, V20, V24, V25 y V8C, ni D8;
- P-03, incluido P-03 por colocación, que sigue apagado por omisión;
- la progresión y la reincorporación;
- el catálogo conservador v1.1 y V57;
- H2 y la PR #69;
- la fórmula de adherencia;
- `PlanGenerationPipeline`, salvo `AI_ATTEMPTS`, que pasa de privada a pública.

Ver `REGRESION_FINAL.md`.
