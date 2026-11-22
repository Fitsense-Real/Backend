# Notificaciones push (Firebase Cloud Messaging)

**Rama:** `feature/notificaciones-push-fcm`. **Estado:** PR independiente, **sin merge** y sin desplegar.

| Documento | Contenido |
|---|---|
| `CONTRATO_PUSH_MOBILE.md` | Contrato con Mobile: identificador FID, ciclo de vida, endpoints, mensaje, pantallas y lo que Mobile debe confirmar |
| `EVENTOS_Y_PROCESOS.md` | Verificación de los cuatro eventos frente a los procesos reales, antiduplicado y estados |
| `SEGURIDAD_Y_CONFIGURACION.md` | Credenciales, variables, garantías de la orden con su prueba, riesgos y privacidad |
| `PRUEBAS.md` | Pruebas y regresión |

## Resumen

- **Identificador:** solo Firebase Installation ID.
  - Flutter `firebase_messaging` ≥ 17.0.0, con `register()` / `onRegistered`.
  - Admin SDK Java ≥ 9.10.0, con `setFid`.
  - No se aceptan tokens FCM.
- **Migración V64**, con cuatro tablas nuevas:
  - `push_devices`;
  - `notification_preferences`;
  - `notification_events` (bandeja de salida);
  - `notification_deliveries`.
- **Endpoints autenticados** con el JWT de siempre:
  - `PUT` / `GET` / `DELETE /api/v1/users/me/push-devices`;
  - `GET` / `PUT /api/v1/users/me/notification-preferences`.
- **Eventos.**
  - El detector programado crea PLAN_GENERATED, PLAN_ADAPTED y WEEKLY_PLAN_READY leyendo filas ya confirmadas. Esto **no modifica** la generación, la adaptación, la progresión ni la reincorporación.
  - ACCOUNT_NOTICE (PASSWORD_CHANGED) se escribe en la misma transacción que el cambio de contraseña.
- **Emisor** en su propio hilo, fuera de las transacciones de negocio:
  - comprueba la cuenta activa, las preferencias, el plan vigente y los dispositivos válidos;
  - reintenta con espera creciente;
  - `UNREGISTERED` desvincula el dispositivo.
- **Apagado por omisión** (`FITSENSE_PUSH_ENABLED=false`). Las credenciales van solo como secreto del entorno.
