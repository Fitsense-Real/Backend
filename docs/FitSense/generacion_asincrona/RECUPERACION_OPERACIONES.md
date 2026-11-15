# Recuperación de operaciones (V59)

## Operación de FitSense frente a predicción de Replicate

| | Operación (`plan_generation_operations`) | Predicción (`plan_generation_predictions`, nueva en V59) |
|---|---|---|
| Qué es | La petición del usuario: «genera mi primera rutina» | Una llamada facturable a Replicate, que es un intento del pipeline |
| Cuántas | Una activa por usuario (índice único parcial) | Hasta 3 vigentes por operación (`ux_pgp_intento_vigente (operation_id, attempt)`) |
| Identificador | `operation_id` (UUID), el que guarda Flutter | `replicate_prediction_id` (el id de Replicate) y `prediction_row_id` |
| Estados | `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`, `NO_FACTIBLE` | `SOLICITANDO`, `ENVIADA`, `SUCCEEDED`, `FAILED`, `CANCELED`, `TIMEOUT`, `NO_CONFIRMADA`, `ERROR_ENVIO`, `DESCARTADA` |
| Tiempos | `created_at`, `first_started_at`, `started_at`, `lease_expires_at`, `finished_at` | `requested_at`, `sent_at`, `last_polled_at`, `finished_at`, `polls` |
| Resultado | `plan_id` | `output` (salida cruda de la IA) y `error` (≤ 500 caracteres) |
| Evidencia de cancelación | — | estado `CANCELED` y `error` con el estado devuelto |
| Secretos | ninguno | ninguno: ni token ni cabeceras. El prompt no se guarda; solo su huella SHA-256 (`request_hash`). |

## Lease con propietario

- **Reclamo.** Con `FOR UPDATE SKIP LOCKED` se fijan:
  - `lease_owner` = `<hostname>-<uuid8>`, uno por proceso;
  - `lease_expires_at = now + 90 s`;
  - `first_started_at`, solo la primera vez.
- **Latido.** Cada 20 s, desde el hilo `generacion-inicial-latido`, se renueva `lease_expires_at` solo de las
  operaciones **propias**.
- **Huérfana = lease vencido.** Un `updated_at` antiguo con lease vigente ya no basta: lo prueba un test con un
  retraso de latido de 181 s.
- **Fencing.** Publicar (`ensureStillProcessing` antes de `persist`), cerrar (`finish`), latir y reencolar al apagar
  exigen `lease_owner = este proceso`. Además, antes de cada predicción **nueva** se comprueba lo mismo. Un worker
  que perdió su lease ni publica ni paga otra predicción.
- **`ck_pgo_lease`.** `PROCESSING` ⇔ hay propietario y vencimiento. La base rechaza cualquier transición que olvide
  soltar el lease.

## Barrido (`recoverStale`, cada 60 s)

1. `PROCESSING` con `coalesce(first_started_at, started_at) < now - 1800 s`, o `PENDING` con
   `created_at < now - 1800 s`: pasa a `FAILED / TIEMPO_EXCEDIDO`.
2. `PROCESSING` con el lease vencido y `executions < 3`: vuelve a `PENDING` sin propietario.
3. `PROCESSING` con el lease vencido y `executions ≥ 3`: pasa a `FAILED / EJECUCION_INTERRUMPIDA`.

Todas las transiciones sueltan el lease.

## Reanudación tras un reinicio

La operación reencolada vuelve a ejecutar el pipeline **desde el principio**. Es lo que garantiza que se repitan los
validadores, el respaldo y la publicación sin atajos. Cada intento de IA consulta primero el libro:

| Fila del intento | Qué se hace | Llamadas a Replicate |
|---|---|---|
| `SUCCEEDED` | Se reutiliza su salida. Se valida otra vez con los validadores actuales. | 0 |
| `ENVIADA` (hay id) | Se sigue **esa** predicción por `GET /v1/predictions/{id}` | solo GET |
| `SOLICITANDO` (sin id confirmado) | Pasa a `NO_CONFIRMADA`. **No se reenvía** (ver `IDEMPOTENCIA_PREDICCIONES.md`). Cuenta como fallo del proveedor y el pipeline sigue con su respaldo validado. | 0 |
| `FAILED`, `CANCELED`, `TIMEOUT`, `ERROR_ENVIO`, `NO_CONFIRMADA` | Se repite el mismo desenlace | 0 |
| `request_hash` distinto (cambió el perfil) | La fila pasa a `DESCARTADA` y se pide una nueva | 1 POST |
| No hay fila | Se busca una predicción reutilizable de **otra** operación del **mismo** usuario con la **misma** huella (24 h). Si no hay, se anota y se envía. | 0 o 1 POST |

El reintento del pipeline se construye con la propuesta y los motivos del intento anterior, y es determinista. Por
eso, al repetirlo con la salida guardada, salen las mismas peticiones y las mismas huellas.
`ReanudacionConReplicateSimuladoIT` lo demuestra con el pipeline real:

- un reinicio antes de publicar se queda en 3 POST en total (antes, 6);
- dos reinicios seguidos, también 3 (antes, 9).

## Resultados tardíos

- **Si una operación vence** por tiempo máximo con su predicción en marcha, el worker antiguo, si sigue vivo, anota
  la salida en el libro (`SUCCEEDED`), pero no publica. Si el usuario reintenta en menos de 24 h, la nueva operación
  la **adopta** sin otra llamada (prueba E8 y `reintentoManualAdopta`).
- **Si una fila ya está cerrada** (por ejemplo, `NO_CONFIRMADA` porque otro worker la reanudó) y llega tarde el id o
  la salida, se guardan como evidencia del posible duplicado, pero la fila **no cambia de estado** y no se vuelve
  reutilizable.

## Apagado ordenado

Sigue como en #71: las operaciones propias vuelven a `PENDING` sin gastar una ejecución. Con V59 solo las
**propias** (filtro por `lease_owner`), y al retomarlas se leen del libro.
