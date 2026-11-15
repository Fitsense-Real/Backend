# Idempotencia de operaciones y predicciones (V59)

**No se declara exactamente-una-vez.** Lo que se demuestra con pruebas es «como máximo una predicción nueva por
intento y operación mientras su envío conste en la base», y «ninguna predicción reenviada cuando el envío quedó
incierto». El caso en que Replicate acepta una predicción y su id nunca llega es un riesgo residual acotado y
contabilizado, no eliminado.

## Escenarios

| Escenario | Mecanismo | Resultado demostrado |
|---|---|---|
| Doble toque en Flutter | Índice único parcial `ux_pgo_una_activa_por_usuario`. La segunda petición recibe la misma operación (200). | 1 operación (`GeneracionAsincronaIT`) |
| Reintento HTTP del cliente | Ídem. `POST` después de `COMPLETED` devuelve 200 con el mismo `planId`. | 1 operación y 1 plan (E1/E2) |
| Flutter se cierra y se reabre | La operación vive en la base. `GET /latest` la devuelve. | mismo `operationId` y `planId` (E1/E2) |
| Pérdida de conexión | Ídem. El cliente reintenta el GET. | sin efecto en el servidor |
| Reinicio del backend | Lease vencido, reencolado y libro de predicciones | 0 POST nuevos (`ReanudacionConReplicateSimuladoIT`) |
| Dos workers | `FOR UPDATE SKIP LOCKED`, más fencing por `lease_owner` | 1 reclamo entre 4 workers (E5). El que perdió el lease no publica, no late y no paga una predicción nueva. |
| Dos peticiones simultáneas | Índice único y captura de la carrera | 1 operación entre 8 peticiones (E6) |
| Resultado válido recibido dos veces | `terminada` solo cierra filas abiertas, y `SUCCEEDED` es inmutable | 0 HTTP al reutilizar (E10) |
| Publicación | `ensureStillProcessing` (propietario) antes de `persist`, más plan único por usuario y semana, más `PlanAlreadyExistsException`, que cierra `COMPLETED` con el plan existente | 1 plan (E11 y todas las IT) |

## La ventana «Replicate aceptó y el id no se guardó»

### Qué cambió en el envío

1. **Antes del POST** se inserta la fila en estado `SOLICITANDO` (autocommit).
2. El POST va **sin `Prefer: wait`**. Replicate responde en cuanto crea la predicción, con su id, así que la ventana
   ya no son hasta 60 s de espera retenida, sino lo que dura una petición corta.
3. Con el id en la mano, la fila pasa a `ENVIADA` antes de empezar a sondear.

### Clasificación del fallo del POST

| Fallo | ¿Pudo crearse la predicción? | Estado | ¿Se reenvía? |
|---|---|---|---|
| HTTP 4xx | No (Replicate la rechazó) | `ERROR_ENVIO` | Lo decide el pipeline como fallo del proveedor; no hay nada que duplicar |
| Conexión rechazada, host desconocido, *connect timeout*, sin ruta | No (el cuerpo no llegó) | `ERROR_ENVIO` | Ídem |
| HTTP 5xx | **Quizá** | `NO_CONFIRMADA` | **No** |
| Lectura cortada o reiniciada, *read timeout* | **Quizá** | `NO_CONFIRMADA` | **No** |
| 2xx sin un id legible | **Quizá** | `NO_CONFIRMADA` | **No** |
| El proceso muere entre el POST y la confirmación | **Quizá** | `SOLICITANDO`, que pasa a `NO_CONFIRMADA` al reanudar | **No** |

`NO_CONFIRMADA` se trata como fallo del proveedor. El pipeline hace lo de siempre ante un proveedor caído (R-1):
publicar el respaldo determinista validado. El usuario recibe una rutina válida y no se paga una segunda predicción
por la duda.

### ¿Hay clave de idempotencia en Replicate?

No se pudo verificar: `api.replicate.com` y `replicate.com` no son accesibles desde este entorno, y la
documentación pública consultada antes no describe ninguna cabecera de idempotencia para crear predicciones. Por
eso el diseño es **conservador**: no se reenvía. Si en el futuro se confirma una clave, se podrá enviar el
`prediction_row_id` como clave sin cambiar el esquema.

### Qué no se puede garantizar

- Una predicción en `NO_CONFIRMADA` pudo ejecutarse y facturarse sin aprovecharse. Queda contada (`no_confirmadas`)
  y, si llegó a conocerse su id, guardada.
- Si después el usuario reintenta manualmente, la nueva operación no adopta una `NO_CONFIRMADA` (no hay salida que
  leer) y puede pagar otra. Está acotado por el límite de 5 generaciones por hora (R-5) y el máximo de 3 intentos.

## Huella de la petición

`request_hash` = SHA-256 del modelo, el prompt, el esfuerzo de razonamiento y el esquema JSON, serializados con
las claves ordenadas (`ORDER_MAP_ENTRIES_BY_KEYS`), porque el orden de `Map.of` varía entre JVM. La huella:

- impide reutilizar la salida de **otra** petición, por ejemplo si cambió el perfil;
- impide reutilizar la de **otro** usuario (filtro por `user_id`).

Ambos casos tienen prueba.
