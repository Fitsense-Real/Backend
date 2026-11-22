# Contrato de notificaciones push: backend ↔ Mobile

**Estado:** propuesta del backend, **pendiente de confirmación por Mobile**. No hubo un agente Mobile disponible en la sesión en que se escribió. El contrato se basa en las APIs publicadas de las dos librerías y se verificó en sus artefactos (§1).

**Proyecto Firebase:** `kora-fitpe`.
**App Android:** `pe.fit.kora`.
**Autenticación:** la de siempre (JWT del backend). Firebase solo transporta mensajes; **no** se usa Firebase Authentication.

## 1. Identificador: Firebase Installation ID (FID), solo FID

| Pieza | Versión mínima | API | Verificado en |
|---|---|---|---|
| Flutter `firebase_messaging` | **17.0.0** (última: 17.1.0) | `register()`, `onRegistered` (Stream<String> con el FID), `unregister()`, `onUnregistered` | Código del paquete 17.1.0 (`lib/src/messaging.dart`). `getToken()`, `onTokenRefresh` y `deleteToken()` están marcados `@Deprecated('Use register()/onRegistered…')`. |
| Android nativo (lo usa el plugin) | BoM 35 / minSdk 24 (requisito de `firebase_messaging` 17) | `FirebaseMessagingService.onRegistered(installationId)` y `onUnregistered(installationId)` | `FlutterFirebaseMessagingService.java` del plugin 17.1.0 |
| Firebase Admin SDK Java | **9.10.0** (el backend usa 9.11.0) | `Message.Builder.setFid(String)` | `javap` sobre el jar: 9.9.0 no lo tiene, 9.10.0 y 9.11.0 sí. El SDK exige exactamente uno de `token`, `fid`, `topic` o `condition`. |

**Decisión:** solo FID.
- El backend **rechaza** tokens de registro FCM (`400 IDENTIFICADOR_PUSH_INVALIDO`). Un token lleva `:` y mide más de 64 caracteres.
- El patrón aceptado es `^[A-Za-z0-9_-]{16,64}$`. Un FID mide 22 caracteres.
- No se mezclan tokens y FID: la columna `identifier_type` solo admite `FID` (CHECK de V64).

> No se encontró una fecha oficial de retirada de los tokens. Las páginas de firebase.google.com no eran accesibles desde el entorno de desarrollo. Lo verificado es que las dos librerías ya ofrecen FID y que la de Flutter depreca los tokens.

## 2. Ciclo de vida del identificador

| Momento | Mobile | Backend |
|---|---|---|
| **Registro** | Tras iniciar sesión y con permiso de notificaciones concedido (Android 13+: `POST_NOTIFICATIONS`): `FirebaseMessaging.instance.register()`. Al recibir `onRegistered(fid)`: `PUT /api/v1/users/me/push-devices`. | Crea el vínculo: `201` si es nuevo, `200` si ya existía. |
| **Renovación** | Cada `onRegistered` con un FID distinto se envía igual (`PUT`). En cada arranque con sesión se vuelve a hacer `PUT` con el FID vigente, que es idempotente y actualiza la actividad. | Un FID nuevo es otro dispositivo. El anterior se desvincula cuando Mobile avisa o cuando FCM responde `UNREGISTERED`. Hay como máximo 5 activos por cuenta; el menos reciente deja su lugar. |
| **Desvinculación** | Al recibir `onUnregistered(fid)`: `DELETE /api/v1/users/me/push-devices/{fid}`. | Revoca el vínculo. Es idempotente. |
| **Cierre de sesión** | **Antes** de borrar el JWT: `DELETE /push-devices/{fid}`. Opcionalmente, `unregister()`. | Ese teléfono deja de recibir para esa cuenta. |
| **Cambio de cuenta** | Tras el login de la cuenta B: `PUT` con el mismo FID. | El FID activo pertenece a una sola cuenta. El vínculo con A se revoca (`CAMBIO_DE_CUENTA`) y A deja de recibir en ese teléfono. |
| **Cambio de contraseña** | Tras el nuevo login: `PUT` con el FID. | Los dispositivos registrados con las credenciales anteriores reciben el aviso `PASSWORD_CHANGED` y luego dejan de recibir (`receivesNotifications=false`) hasta el nuevo `PUT`. |
| **Eliminación de cuenta** | Nada adicional. | `DELETE /api/v1/users/me` y la eliminación web borran dispositivos, eventos, entregas y preferencias (V64 en `TABLAS_EN_ORDEN`). Durante la eliminación no se envía nada. |
| **Desinstalación** | — | FCM responde `UNREGISTERED` y el backend desvincula el FID (`NO_REGISTRADO_EN_FCM`). |

## 3. Endpoints (todos con `Authorization: Bearer <JWT>`)

### `PUT /api/v1/users/me/push-devices`

```json
{ "installationId": "cR_9l0aBcD3fGh1jK2lM4n", "platform": "ANDROID", "appId": "pe.fit.kora", "appVersion": "1.0.0+12" }
```

**Respuesta** `201` (nuevo) o `200` (ya existía):

```json
{ "deviceId": 17, "installationIdHint": "cR_9…", "platform": "ANDROID", "appVersion": "1.0.0+12",
  "registeredAt": "2026-11-22T14:05:00Z", "lastSeenAt": "2026-11-22T14:05:00Z", "receivesNotifications": true }
```

El FID completo **nunca** vuelve en la API.

| Código | `details[0]` | Causa |
|---|---|---|
| 400 | `IDENTIFICADOR_PUSH_INVALIDO` | No tiene forma de FID (p. ej. un token FCM) |
| 400 | `PLATAFORMA_NO_ADMITIDA` | `platform` distinto de `ANDROID` |
| 400 | `APLICACION_NO_ADMITIDA` | `appId` distinto de `pe.fit.kora` |
| 400 | `VERSION_APP_INVALIDA` | `appVersion` de más de 40 caracteres |
| 400 | `installationId: …` | Campo obligatorio ausente |
| 401 / 403 | — | Sin JWT, o JWT revocado |
| 409 | — | Dos registros simultáneos del mismo FID: reintentar |

### `DELETE /api/v1/users/me/push-devices/{installationId}`

Responde `204` siempre, también si el FID no existe o es de otra cuenta. No revela nada y no cambia vínculos ajenos.

### `GET /api/v1/users/me/push-devices`

Lista los dispositivos activos, con el mismo formato que la respuesta del `PUT`.

### `GET` y `PUT /api/v1/users/me/notification-preferences`

```json
{ "planUpdates": true, "accountNotices": true }
```

- Sin configurar, las dos son `true`.
- `planUpdates` cubre `PLAN_GENERATED`, `PLAN_ADAPTED` y `WEEKLY_PLAN_READY`.
- `accountNotices` cubre `ACCOUNT_NOTICE`.
- Se aplican **al enviar**: desactivar una categoría también descarta lo que estaba pendiente.

## 4. Mensaje

Cada mensaje lleva un bloque `notification` (título y cuerpo genéricos, ver §5) y un bloque `data`. Todos los valores son cadenas.

| Clave | Siempre | Ejemplo | Uso en Mobile |
|---|---|---|---|
| `schemaVersion` | sí | `"1"` | Ignorar mensajes con una versión mayor que la conocida |
| `eventType` | sí | `"PLAN_ADAPTED"` | Tipo de evento (§5) |
| `eventId` | sí | `"0b6f3c2e-…"` (UUID) | Identificador único: descartar duplicados en la app |
| `resourceType` | si aplica | `"PLAN"` | Tipo del recurso relacionado |
| `resourceId` | si aplica | `"42"` | `planId` del plan publicado |
| `targetScreen` | sí | `"WEEKLY_PLAN"` | Pantalla de destino (§6) |
| `createdAt` | sí | `"2026-11-22T14:05:00Z"` | Fecha del evento, ISO-8601 UTC |
| `noticeCode` | solo `ACCOUNT_NOTICE` | `"PASSWORD_CHANGED"` | Subtipo del aviso de cuenta |

**Android:**
- `restrictedPackageName = pe.fit.kora`;
- prioridad `NORMAL`;
- TTL de 24 h;
- `collapseKey` y `tag`: `plan_semana` para los eventos de plan, `aviso_cuenta` para los avisos. Una notificación de plan reemplaza a la anterior en la bandeja.

**Canales que Mobile debe crear al arrancar:**

| Canal | Contenido |
|---|---|
| `kora_planes` | Plan listo, plan ajustado, nueva semana |
| `kora_cuenta` | Avisos de seguridad |

**Al tocar la notificación** (`getInitialMessage` / `onMessageOpenedApp`):
1. leer `targetScreen` y `resourceId`;
2. si no hay sesión, ir al login y después a la pantalla;
3. **pedir siempre el dato al backend** con la API autenticada. El mensaje nunca trae el contenido del plan.

## 5. Eventos

| `eventType` | Proceso real del backend | Texto visible |
|---|---|---|
| `PLAN_GENERATED` | La generación inicial **asíncrona** termina en `COMPLETED` (`plan_generation_operations`) | «Tu plan está listo» · «Ya puedes ver tu plan de entrenamiento.» |
| `PLAN_ADAPTED` | El cierre semanal publica un plan con una adaptación `APLICADA` | «Ajustamos tu plan» · «Revisa los cambios de tu plan de esta semana.» |
| `WEEKLY_PLAN_READY` | El cierre semanal publica la semana siguiente sin adaptación aplicada (continuidad, progresión, sin efecto o respaldo) | «Tu nueva semana está lista» · «Ya puedes ver tu plan de la semana.» |
| `ACCOUNT_NOTICE` | `PASSWORD_CHANGED`: confirmación de la recuperación de contraseña | «Aviso de seguridad» · «La contraseña de tu cuenta se cambió. Si no fuiste tú, recupérala desde la app.» |

**Antiduplicado:** un plan se notifica **una sola vez**, aunque varios registros lo citen. La clave única es `PLAN:<planId>`, y `PLAN_ADAPTED` gana a `WEEKLY_PLAN_READY`.

Detalle y lo que **no** se notifica: `EVENTOS_Y_PROCESOS.md`.

## 6. Pantallas de destino

| `targetScreen` | Pantalla de Mobile (a confirmar) | Dato que pide después |
|---|---|---|
| `WEEKLY_PLAN` | Plan semanal | `GET` del plan activo; `resourceId` sirve para resaltar la versión |
| `ACCOUNT_SECURITY` | Ajustes, cuenta y seguridad | — |

## 7. Lo que pedimos a Mobile que confirme

1. Que la app usa `firebase_messaging` **≥ 17.0.0** con `register()` / `onRegistered`, y **no** `getToken()`.
2. Que el `applicationId` es `pe.fit.kora`. El clon del repositorio Mobile disponible al escribir esto todavía tiene `com.example.fitsense_mobile` y no incluye Firebase.
3. Los nombres de las pantallas de destino y de los canales `kora_planes` / `kora_cuenta`.
4. Que el cierre de sesión llama al `DELETE` **antes** de borrar el JWT.
5. Que se deduplica por `eventId` en la app.
6. Que se pide el permiso `POST_NOTIFICATIONS` antes de `register()`.

Cualquier cambio a este contrato se hace con una versión nueva de `schemaVersion` o con un acuerdo explícito. Nunca se pasa de forma silenciosa a usar tokens.
