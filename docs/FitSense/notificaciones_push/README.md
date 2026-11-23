# Notificaciones push (Firebase Cloud Messaging)

**Rama:** `feature/notificaciones-push-fcm`. **Estado:** PR independiente, **sin merge** y sin desplegar.

| Documento | Contenido |
|---|---|
| `CONTRATO_PUSH_MOBILE.md` | Contrato de integración que ofrece el backend: identificador FID, ciclo de vida, endpoints, mensaje, pantallas y lo que se espera de la app |
| `EVENTOS_Y_PROCESOS.md` | Verificación de los cuatro eventos frente a los procesos reales, antiduplicado y estados |
| `EJEMPLOS_REALES_HTTP.md` | Solicitudes y respuestas reales de todos los endpoints, generadas por `ContratoPushHttpIT` |
| `ARRANQUE_FIREBASE.md` | Causa y solución del `NoClassDefFoundError: JacksonFactory`, prueba de regresión y arranque real en los tres escenarios |
| `CONFIGURACION_RAILWAY.md` | Cómo se entregan las credenciales en Railway sin pasar por GitHub, y qué ocurre sin credenciales válidas |
| `SEGURIDAD_Y_CONFIGURACION.md` | Credenciales, variables, garantías de la orden con su prueba, riesgos y privacidad |
| `PRUEBAS.md` | Pruebas y regresión |

## Resumen

- **Identificador:** solo Firebase Installation ID.
  - Flutter `firebase_messaging` ≥ 17.0.0, con `register()` / `onRegistered`.
  - Admin SDK Java **9.12.0** (`setFid` desde 9.10.0; 9.11.0 no arranca sin Firestore, ver `ARRANQUE_FIREBASE.md`).
  - No se aceptan tokens FCM.
- **Migración V64**, con cuatro tablas nuevas:
  - `push_devices`;
  - `notification_preferences`;
  - `notification_events` (bandeja de salida);
  - `notification_deliveries`.
- **Endpoints autenticados** con el JWT de siempre:
  - `PUT` / `GET` / `DELETE /api/v1/users/me/push-devices` (el `DELETE` acepta el FID en la ruta o en el cuerpo);
  - `GET` / `PUT /api/v1/users/me/notification-preferences`.
- **Eventos.**
  - El detector programado crea PLAN_GENERATED, PLAN_ADAPTED y WEEKLY_PLAN_READY leyendo filas ya confirmadas. Esto **no modifica** la generación, la adaptación, la progresión ni la reincorporación.
  - ACCOUNT_NOTICE (PASSWORD_CHANGED) se escribe en la misma transacción que el cambio de contraseña.
- **Emisor** en su propio hilo, fuera de las transacciones de negocio:
  - comprueba la cuenta activa, las preferencias, el plan vigente y los dispositivos válidos;
  - reintenta con espera creciente;
  - `UNREGISTERED` desvincula el dispositivo.
- **Apagado por omisión** (`FITSENSE_PUSH_ENABLED=false`). Las credenciales van solo como secreto del entorno.
