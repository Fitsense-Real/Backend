# Eventos push y procesos del backend

## Problema

La orden pide mantener PLAN_GENERATED, PLAN_ADAPTED, WEEKLY_PLAN_READY y ACCOUNT_NOTICE. Antes de implementarlos había que verificar que cada uno corresponde a un proceso que **existe** en el backend.

## Verificación

| Evento | Proceso | Fila confirmada que lo prueba | Resultado |
|---|---|---|---|
| PLAN_GENERATED | Generación inicial asíncrona (V58/V59, `PlanGenerationOperationService`) | `plan_generation_operations.status = 'COMPLETED'`. Siempre lleva `plan_id` (CHECK `ck_pgo_completed_has_plan`) | **Existe** |
| PLAN_ADAPTED | Cierre semanal (`WeeklyCycleService`) con una adaptación aplicada | `plan_adaptation_attempts.status = 'APLICADA'` con `resulting_plan_id` | **Existe** |
| WEEKLY_PLAN_READY | Cierre semanal que publica la semana siguiente sin aplicar adaptación | `plan_adaptation_attempts` con `resulting_plan_id` y estado `SIN_ORDEN`, `SIN_EFECTO` o `RESPALDO`. La progresión publica por este camino | **Existe** |
| ACCOUNT_NOTICE | Recuperación de contraseña (`PasswordResetService.confirmar`), que sube `token_version` | La fila del aviso se escribe en la **misma transacción** que el cambio | **Existe** (solo `PASSWORD_CHANGED`) |

## Lo que NO se notifica, y por qué

| Caso | Motivo |
|---|---|
| Generación **síncrona** del primer plan | La app ya está esperando la respuesta HTTP con el plan. Un push sería un duplicado |
| Adaptación `RECHAZADA` o `NO_FACTIBLE` | No publica ningún plan. Notificar un fracaso por push necesitaría un texto y un flujo acordados (propuesta futura, fuera de esta orden) |
| Regeneración en el mismo ciclo (P-03) | Está **apagada** en producción. Si se activara, haría falta añadir su origen al detector |
| Eliminación de cuenta | Los dispositivos se borran con la cuenta. No queda destino legítimo |
| Retiro del estudio y consentimientos | No hay texto aprobado. Los documentos de consentimiento aún no están aprobados |

## Cómo se crean los eventos sin modificar la generación ni la adaptación

La orden prohíbe modificar generación, adaptación, progresión y reincorporación.

**Detector programado** (`DetectorEventosPush`, cada 30 s y solo con `fitsense.push.enabled=true`):
- lee las filas **ya confirmadas** de `plan_generation_operations` y `plan_adaptation_attempts`;
- inserta un evento por plan en `notification_events`.

**Por qué este diseño:**
- **Solo después de la persistencia.** El detector solo ve lo que ya tiene `COMMIT`.
- **Firebase no puede interrumpir el entrenamiento.** La generación y la adaptación no llaman a ningún componente push. Si FCM falla, solo se reintenta el envío.
- **Ventana acotada.** El detector mira 6 h hacia atrás (`fitsense.push.detection-window-hours`). Así un reinicio no pierde eventos y, al activar la función, no se notifica el historial.
- **Planes reemplazados.** Un plan ya `REPLACED` no genera evento. Si se reemplaza antes del envío, el emisor lo descarta (`PLAN_NO_VIGENTE`).

**El aviso de cuenta sí se escribe desde su proceso.** Es la recuperación de contraseña (IAM), que la orden no protege. Va en la misma transacción que el cambio: si el cambio se revierte, el aviso no existe.

## Antiduplicado

**Por plan.** La clave es `dedupe_key = 'PLAN:' || plan_id`, con restricción UNIQUE. Con varias filas para el mismo plan:
- se inserta una sola (`DISTINCT ON`, `ON CONFLICT DO NOTHING`);
- si alguna es `APLICADA`, el tipo es `PLAN_ADAPTED`.

**Por aviso de contraseña.** La clave es `ACCOUNT:PASSWORD_CHANGED:<user>:<versión nueva>`.

**Por dispositivo.** El índice único `ux_nd_enviado (event_id, device_id) WHERE status = 'ENVIADO'` impide repetir un envío que FCM ya aceptó. Esto vale también en los reintentos.

**En el teléfono.**
- `collapseKey` / `tag` = `plan_semana`: el aviso nuevo reemplaza al anterior;
- `eventId` permite deduplicar en la app.

## Estados de un evento

| Estado | Significado |
|---|---|
| `PENDIENTE` | Esperando turno (`next_attempt_at`) |
| `EN_ENVIO` | Reclamado con lease de 120 s. Si el proceso muere, se reclama de nuevo al vencer |
| `ENVIADO` | Al menos un dispositivo lo aceptó |
| `SIN_DESTINO` | No había dispositivos activos y válidos |
| `DESCARTADO` | Cuenta no activa, preferencia desactivada o plan no vigente (`status_reason`) |
| `FALLIDO` | Reintentos agotados (`REINTENTOS_AGOTADOS`) o todos los destinos rechazados (`DESTINOS_RECHAZADOS`) |
| `CADUCADO` | Más antiguo que el TTL (24 h) |
