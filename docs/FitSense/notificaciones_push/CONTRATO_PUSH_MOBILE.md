# Contrato de notificaciones push: backend → app

**Estado:** contrato exacto que **ofrece el backend** (PR #82). La app lo implementa por su lado. Los ejemplos salen de `ContratoPushHttpIT`, que llama a los endpoints reales sobre PostgreSQL 16; están completos en `EJEMPLOS_REALES_HTTP.md`.

| | |
|---|---|
| Proyecto Firebase | `kora-fitpe` |
| App Android (`applicationId`) | `pe.fit.kora`. Es el **único** `appId` que admite el backend y el `restrictedPackageName` de los mensajes |
| Autenticación | `Authorization: Bearer <JWT>`: el JWT del login actual (`POST /api/v1/auth/login`). Firebase **no** autentica |
| Prefijo de todas las rutas | `/api/v1` |

## 0. Compatibilidad con la propuesta actual de Mobile

Mobile propone `PUT` y `DELETE /users/me/push-devices`. No se asume compatibilidad. Comparación exacta con el backend:

| Propuesta Mobile | Backend | ¿Coincide? |
|---|---|---|
| `PUT /users/me/push-devices` | `PUT /api/v1/users/me/push-devices` | **Solo si la URL base de Mobile ya incluye `/api/v1`**, como en el resto de llamadas al backend. Sin el prefijo, la respuesta es `404` |
| `DELETE /users/me/push-devices` | `DELETE /api/v1/users/me/push-devices` con cuerpo `{"installationId": "<FID>"}`, **o bien** `DELETE /api/v1/users/me/push-devices/{installationId}` | **Sí, si envía el FID en el cuerpo JSON.** Un `DELETE` sin FID (ni en la ruta ni en el cuerpo) da `400`. El backend nunca adivina qué dispositivo desvincular |
| Cuerpo del `PUT` | `installationId`, `platform`, `appId` obligatorios; `appVersion` opcional (§3) | **Por verificar en Mobile:** los nombres de los campos tienen que ser exactamente estos. Un campo con otro nombre se ignora y da `400` por el campo obligatorio que falta |
| Identificador | **FID**, nunca un token FCM (§1) | **Por verificar en Mobile:** debe usar `register()` / `onRegistered`, no `getToken()` |

## 1. Identificador esperado: Firebase Installation ID (FID)

| Pieza | Versión mínima | API |
|---|---|---|
| Flutter `firebase_messaging` | **17.0.0** | `FirebaseMessaging.instance.register()`, `onRegistered` (Stream con el FID), `unregister()`, `onUnregistered`. `getToken()`, `onTokenRefresh` y `deleteToken()` están marcados `@Deprecated` |
| Android | BoM 35, minSdk 24 (requisitos de `firebase_messaging` 17) | `FirebaseMessagingService.onRegistered` / `onUnregistered` (el plugin los usa) |
| Backend: Firebase Admin SDK Java | **9.10.0** (se usa 9.11.0) | `Message.Builder.setFid(fid)` |

**Formato aceptado:** `^[A-Za-z0-9_-]{16,64}$`. Un FID mide 22 caracteres; ejemplo: `cR_9l0aBcD3fGh1jK2lM4n`.

**Rechazos:** un token de registro FCM (lleva `:` y mide más de 150 caracteres) recibe `400 IDENTIFICADOR_PUSH_INVALIDO`. FID y tokens no se mezclan: la base solo admite `identifier_type = 'FID'`.

## 2. Endpoints

| Método y ruta | Uso | Éxito |
|---|---|---|
| `PUT /api/v1/users/me/push-devices` | Registrar o renovar esta instalación (idempotente) | `201` si es nueva, `200` si ya existía, con un `Dispositivo` |
| `GET /api/v1/users/me/push-devices` | Dispositivos activos de la cuenta | `200`, lista de `Dispositivo` |
| `DELETE /api/v1/users/me/push-devices` con cuerpo `{"installationId": "<FID>"}` | Desvincular (cierre de sesión u `onUnregistered`) | `204`, sin cuerpo |
| `DELETE /api/v1/users/me/push-devices/{installationId}` | Lo mismo, con el FID en la ruta | `204`, sin cuerpo |
| `GET /api/v1/users/me/notification-preferences` | Leer preferencias | `200`, `Preferencias` |
| `PUT /api/v1/users/me/notification-preferences` | Guardar preferencias | `200`, `Preferencias` |

Los dos `DELETE` son idempotentes. Desvincular un FID inexistente o de otra cuenta también responde `204`, no revela nada y no cambia vínculos ajenos.

## 3. Campos JSON

### `PUT /push-devices`: solicitud (`Content-Type: application/json`)

| Campo | Obligatorio | Tipo | Regla |
|---|---|---|---|
| `installationId` | sí | string | FID (§1) |
| `platform` | sí | string | Exactamente `"ANDROID"` |
| `appId` | sí | string | Exactamente `"pe.fit.kora"` |
| `appVersion` | no | string ≤ 40 | P. ej. `"1.0.0+12"`. Se guarda para diagnóstico |

### `Dispositivo`: respuesta

| Campo | Tipo | Significado |
|---|---|---|
| `deviceId` | number | Identificador interno del vínculo |
| `installationIdHint` | string | Los 4 primeros caracteres del FID y `…`. El FID completo **nunca** vuelve |
| `platform` | string | `"ANDROID"` |
| `appVersion` | string \| null | La última versión enviada |
| `registeredAt`, `lastSeenAt` | string ISO-8601 UTC | Alta y último `PUT` |
| `receivesNotifications` | boolean | `false` si el dispositivo se registró con credenciales anteriores a un cambio de contraseña: hay que repetir el `PUT` tras el nuevo login |

### `DELETE /push-devices`: solicitud con cuerpo

| Campo | Obligatorio |
|---|---|
| `installationId` | sí |

### `Preferencias`: solicitud y respuesta

| Campo | Obligatorio en el `PUT` | Por omisión | Cubre |
|---|---|---|---|
| `planUpdates` | sí | `true` | PLAN_GENERATED, PLAN_ADAPTED, WEEKLY_PLAN_READY |
| `accountNotices` | sí | `true` | ACCOUNT_NOTICE |

Se aplican **al enviar**: desactivar una categoría descarta también lo que estuviera pendiente.

## 4. Respuestas de error

**Formato de los errores 400, 404 y 409** (`ApiErrorResource`). El código de máquina va en `details[0]`:

```json
{ "timestamp": "2026-11-22T23:34:00.552730548Z", "status": 400, "error": "Bad Request",
  "message": "El identificador debe ser un Firebase Installation ID (no un token de registro FCM).",
  "details": [ "IDENTIFICADOR_PUSH_INVALIDO" ] }
```

| Estado | `details` | Cuándo |
|---|---|---|
| 400 | `["IDENTIFICADOR_PUSH_INVALIDO"]` | El identificador no es un FID (p. ej. un token FCM) |
| 400 | `["APLICACION_NO_ADMITIDA"]` | `appId` ≠ `pe.fit.kora` (p. ej. `com.example.fitsense_mobile`) |
| 400 | `["PLATAFORMA_NO_ADMITIDA"]` | `platform` ≠ `ANDROID` |
| 400 | `["VERSION_APP_INVALIDA"]` | `appVersion` de más de 40 caracteres |
| 400 | `["CUENTA_NO_ACTIVA"]` | La cuenta está en eliminación o inactiva |
| 400 | `["<campo>: must not be blank"]`, … | Falta un campo obligatorio. Se lista cada uno, en orden alfabético |
| 400 | `[]` con `"El cuerpo de la solicitud no es valido."` | JSON mal formado, o `DELETE /push-devices` sin cuerpo |
| 401 | — | Sin JWT, o JWT inválido o revocado (contraseña cambiada, cuenta eliminada). **Este cuerpo lo produce el filtro de seguridad y no trae `details`:** `{"timestamp", "message": "Se requiere un token valido.", "error": "Unauthorized", "status": 401}` |
| 409 | — | Dos `PUT` simultáneos del mismo FID: reintentar una vez |

## 5. Comportamiento en el ciclo de vida

| Situación | Qué hace la app | Qué hace el backend | Probado en |
|---|---|---|---|
| **Primer registro** | Tras el login, con `POST_NOTIFICATIONS` concedido: `register()`. En `onRegistered(fid)`: `PUT` | Crea el vínculo (`201`) | `ContratoPushHttpIT` §1 |
| **Renovación o arranque** | En cada arranque con sesión, y en cada `onRegistered`: `PUT` | `200` con el mismo `deviceId` (actualiza `lastSeenAt` y `appVersion`). Un FID distinto es otro dispositivo. Máximo 5 activos por cuenta; el menos reciente deja su lugar | §2 |
| **Cierre de sesión** | **Antes** de borrar el JWT: `DELETE` con el FID (cuerpo o ruta) | Revoca el vínculo. El teléfono deja de recibir para esa cuenta | §7 |
| **Cambio de cuenta** | Tras el login de la cuenta B: `PUT` con el mismo FID | El FID activo pertenece a una sola cuenta. El vínculo con A se revoca (`CAMBIO_DE_CUENTA`) y A deja de recibir en ese teléfono, aunque la app no haya llamado al `DELETE` | §6; `NotificacionesPushIT.cambioDeCuenta` |
| **Cambio de contraseña** | Tras el nuevo login: `PUT` con el FID | Los dispositivos de antes reciben `ACCOUNT_NOTICE/PASSWORD_CHANGED` y luego quedan con `receivesNotifications=false` hasta el nuevo `PUT`. El JWT anterior devuelve `401` | `NotificacionesPushIT.cambioDeContrasena` |
| **Eliminación de cuenta** | `DELETE /api/v1/users/me` (o la eliminación web). Nada más | Durante la eliminación no se envía nada. Al completarse se borran dispositivos, eventos, entregas y preferencias, y el JWT da `401` | §8; `NotificacionesPushIT.cuentaEliminada` |
| **`onUnregistered(fid)`** | `DELETE` con ese FID | Revoca el vínculo | §7 |
| **Desinstalación** | — | FCM responde `UNREGISTERED` y el backend desvincula el FID | `NotificacionesPushIT.fallosDeFcm` |

## 6. Mensaje push

Lleva un bloque `notification` (texto genérico) y un bloque `data`, con todos los valores como cadenas:

| Clave | Siempre | Ejemplo |
|---|---|---|
| `schemaVersion` | sí | `"1"` |
| `eventType` | sí | `"PLAN_GENERATED"`, `"PLAN_ADAPTED"`, `"WEEKLY_PLAN_READY"` o `"ACCOUNT_NOTICE"` |
| `eventId` | sí | UUID, único por evento. La app debe descartar duplicados con él |
| `resourceType` | eventos de plan | `"PLAN"` |
| `resourceId` | eventos de plan | `"42"` (`planId`) |
| `targetScreen` | sí | `"WEEKLY_PLAN"` o `"ACCOUNT_SECURITY"` |
| `createdAt` | sí | `"2026-11-22T14:05:00Z"` |
| `noticeCode` | solo ACCOUNT_NOTICE | `"PASSWORD_CHANGED"` |

**Android:**
- `restrictedPackageName = pe.fit.kora`, prioridad `NORMAL` y TTL de 24 h.
- `collapseKey` / `tag`: `plan_semana` para los eventos de plan, `aviso_cuenta` para los avisos.
- Canales que la app debe crear: `kora_planes` y `kora_cuenta`.

**Al tocar la notificación**, la app pide el dato al backend con su JWT. El mensaje nunca trae contenido del plan ni datos de salud.

| `eventType` | Título · cuerpo |
|---|---|
| PLAN_GENERATED | «Tu plan está listo» · «Ya puedes ver tu plan de entrenamiento.» |
| PLAN_ADAPTED | «Ajustamos tu plan» · «Revisa los cambios de tu plan de esta semana.» |
| WEEKLY_PLAN_READY | «Tu nueva semana está lista» · «Ya puedes ver tu plan de la semana.» |
| ACCOUNT_NOTICE | «Aviso de seguridad» · «La contraseña de tu cuenta se cambió. Si no fuiste tú, recupérala desde la app.» |

Cuándo se genera cada evento y cómo se evitan los duplicados: `EVENTOS_Y_PROCESOS.md`.

## 7. Lo que el backend espera de la app

1. `firebase_messaging` ≥ 17.0.0, con `register()` / `onRegistered`; nunca `getToken()`.
2. `applicationId` `pe.fit.kora`.
3. URL base con `/api/v1` y exactamente los nombres de campo del §3.
4. `DELETE` con el FID antes de borrar el JWT al cerrar sesión.
5. Deduplicación por `eventId`, y creación de los canales `kora_planes` y `kora_cuenta`.
6. Traducción de `targetScreen` a sus rutas.

Un cambio de este contrato requiere un `schemaVersion` nuevo o un acuerdo explícito. Nunca se pasa en silencio a tokens.
