<!-- Generado por ContratoPushHttpIT (target/contrato_push_ejemplos.md) sobre PostgreSQL 16 real. No editar a mano: regenerar con la prueba. -->
# Ejemplos reales del contrato push (generados por ContratoPushHttpIT)

## 1. Registro de dispositivo

```http
PUT /api/v1/users/me/push-devices
Authorization: Bearer <JWT>
Content-Type: application/json

{"installationId":"cR_9l0aBcD3fGh1jK2lM4n","platform":"ANDROID","appId":"pe.fit.kora","appVersion":"1.0.0+12"}
```

```http
HTTP 201

{
  "deviceId" : 1,
  "installationIdHint" : "cR_9…",
  "platform" : "ANDROID",
  "appVersion" : "1.0.0+12",
  "registeredAt" : "2026-11-22T23:34:00.505315Z",
  "lastSeenAt" : "2026-11-22T23:34:00.505315Z",
  "receivesNotifications" : true
}
```

## 2. Registro repetido (renovacion o arranque): idempotente

```http
PUT /api/v1/users/me/push-devices
Authorization: Bearer <JWT>
Content-Type: application/json

{"installationId":"cR_9l0aBcD3fGh1jK2lM4n","platform":"ANDROID","appId":"pe.fit.kora"}
```

```http
HTTP 200

{
  "deviceId" : 1,
  "installationIdHint" : "cR_9…",
  "platform" : "ANDROID",
  "appVersion" : null,
  "registeredAt" : "2026-11-22T23:34:00.505315Z",
  "lastSeenAt" : "2026-11-22T23:34:00.534500Z",
  "receivesNotifications" : true
}
```

## 3. Errores del registro

```http
PUT /api/v1/users/me/push-devices
Authorization: Bearer <JWT>
Content-Type: application/json

{"installationId":"dXy7:APA91bHPRgkF3JUikC4ENAHEeMrd41Zxv3hVZjC9KtT8OvPVGJ-hQMRKRrZuJAEcl7B338qju59zJMjw2DELjzEvxwYv7hH5Ynpc1ODQ0aT4U4OFEeco8ohsN5PjL1iC2dNtk2BAokeMCg2ZXKqpc8FXKmhX94kIxQ","platform":"ANDROID","appId":"pe.fit.kora"}
```

```http
HTTP 400

{
  "timestamp" : "2026-11-22T23:34:00.552730548Z",
  "status" : 400,
  "error" : "Bad Request",
  "message" : "El identificador debe ser un Firebase Installation ID (no un token de registro FCM).",
  "details" : [ "IDENTIFICADOR_PUSH_INVALIDO" ]
}
```

```http
PUT /api/v1/users/me/push-devices
Authorization: Bearer <JWT>
Content-Type: application/json

{"installationId":"cR_9l0aBcD3fGh1jK2lM4n","platform":"ANDROID","appId":"com.example.fitsense_mobile"}
```

```http
HTTP 400

{
  "timestamp" : "2026-11-22T23:34:00.593298669Z",
  "status" : 400,
  "error" : "Bad Request",
  "message" : "La aplicacion no corresponde a esta configuracion de Firebase.",
  "details" : [ "APLICACION_NO_ADMITIDA" ]
}
```

```http
PUT /api/v1/users/me/push-devices
Authorization: Bearer <JWT>
Content-Type: application/json

{"installationId":"cR_9l0aBcD3fGh1jK2lM4n","platform":"IOS","appId":"pe.fit.kora"}
```

```http
HTTP 400

{
  "timestamp" : "2026-11-22T23:34:00.607202752Z",
  "status" : 400,
  "error" : "Bad Request",
  "message" : "Solo se admite la plataforma ANDROID.",
  "details" : [ "PLATAFORMA_NO_ADMITIDA" ]
}
```

```http
PUT /api/v1/users/me/push-devices
Authorization: Bearer <JWT>
Content-Type: application/json

{"installationId":"cR_9l0aBcD3fGh1jK2lM4n"}
```

```http
HTTP 400

{
  "timestamp" : "2026-11-22T23:34:00.634060964Z",
  "status" : 400,
  "error" : "Bad Request",
  "message" : "La solicitud tiene campos invalidos.",
  "details" : [ "appId: must not be blank", "platform: must not be blank" ]
}
```

```http
PUT /api/v1/users/me/push-devices
Content-Type: application/json

{"installationId":"cR_9l0aBcD3fGh1jK2lM4n","platform":"ANDROID","appId":"pe.fit.kora"}
```

```http
HTTP 401

{
  "timestamp" : "2026-11-22T23:34:00.649483532Z",
  "message" : "Se requiere un token valido.",
  "error" : "Unauthorized",
  "status" : 401
}
```

## 4. Consulta

```http
GET /api/v1/users/me/push-devices
Authorization: Bearer <JWT>
```

```http
HTTP 200

[ {
  "deviceId" : 1,
  "installationIdHint" : "cR_9…",
  "platform" : "ANDROID",
  "appVersion" : null,
  "registeredAt" : "2026-11-22T23:34:00.505315Z",
  "lastSeenAt" : "2026-11-22T23:34:00.534500Z",
  "receivesNotifications" : true
} ]
```

## 5. Preferencias

```http
GET /api/v1/users/me/notification-preferences
Authorization: Bearer <JWT>
```

```http
HTTP 200

{
  "planUpdates" : true,
  "accountNotices" : true
}
```

```http
PUT /api/v1/users/me/notification-preferences
Authorization: Bearer <JWT>
Content-Type: application/json

{"planUpdates":false,"accountNotices":true}
```

```http
HTTP 200

{
  "planUpdates" : false,
  "accountNotices" : true
}
```

```http
PUT /api/v1/users/me/notification-preferences
Authorization: Bearer <JWT>
Content-Type: application/json

{"planUpdates":false}
```

```http
HTTP 400

{
  "timestamp" : "2026-11-22T23:34:00.698617911Z",
  "status" : 400,
  "error" : "Bad Request",
  "message" : "La solicitud tiene campos invalidos.",
  "details" : [ "accountNotices: must not be null" ]
}
```

## 6. Cambio de cuenta en el mismo telefono

```http
PUT /api/v1/users/me/push-devices
Authorization: Bearer <JWT>
Content-Type: application/json

{"installationId":"cR_9l0aBcD3fGh1jK2lM4n","platform":"ANDROID","appId":"pe.fit.kora","appVersion":"1.0.0+12"}
```

```http
HTTP 201

{
  "deviceId" : 2,
  "installationIdHint" : "cR_9…",
  "platform" : "ANDROID",
  "appVersion" : "1.0.0+12",
  "registeredAt" : "2026-11-22T23:34:00.704481Z",
  "lastSeenAt" : "2026-11-22T23:34:00.704481Z",
  "receivesNotifications" : true
}
```

```http
GET /api/v1/users/me/push-devices
Authorization: Bearer <JWT>
```

```http
HTTP 200

[ ]
```

## 7. Cierre de sesion: desvinculacion por ruta y por cuerpo (las dos son validas e idempotentes)

```http
DELETE /api/v1/users/me/push-devices/cR_9l0aBcD3fGh1jK2lM4n
Authorization: Bearer <JWT>
```

```http
HTTP 204
```

```http
DELETE /api/v1/users/me/push-devices
Authorization: Bearer <JWT>
Content-Type: application/json

{"installationId":"cR_9l0aBcD3fGh1jK2lM4n"}
```

```http
HTTP 204
```

```http
GET /api/v1/users/me/push-devices
Authorization: Bearer <JWT>
```

```http
HTTP 200

[ ]
```

```http
DELETE /api/v1/users/me/push-devices
Authorization: Bearer <JWT>
Content-Type: application/json

{}
```

```http
HTTP 400

{
  "timestamp" : "2026-11-22T23:34:00.76400353Z",
  "status" : 400,
  "error" : "Bad Request",
  "message" : "La solicitud tiene campos invalidos.",
  "details" : [ "installationId: must not be blank" ]
}
```

```http
DELETE /api/v1/users/me/push-devices
Authorization: Bearer <JWT>
Content-Type: application/json


```

```http
HTTP 400

{
  "timestamp" : "2026-11-22T23:34:00.770223499Z",
  "status" : 400,
  "error" : "Bad Request",
  "message" : "El cuerpo de la solicitud no es valido.",
  "details" : [ ]
}
```

## 8. Eliminacion de cuenta: el JWT deja de valer y no quedan dispositivos

```http
PUT /api/v1/users/me/push-devices
Authorization: Bearer <JWT>
Content-Type: application/json

{"installationId":"cR_9l0aBcD3fGh1jK2lM4n","platform":"ANDROID","appId":"pe.fit.kora"}
```

```http
HTTP 201

{
  "deviceId" : 3,
  "installationIdHint" : "cR_9…",
  "platform" : "ANDROID",
  "appVersion" : null,
  "registeredAt" : "2026-11-22T23:34:00.775134Z",
  "lastSeenAt" : "2026-11-22T23:34:00.775134Z",
  "receivesNotifications" : true
}
```

```http
DELETE /api/v1/users/me
Authorization: Bearer <JWT>
Content-Type: application/json

{"password":"clave-sintetica-123","confirm":true}
```

```http
HTTP 200

{
  "status" : "DELETED",
  "deletionId" : "aba8cd59-9a53-43bf-a5e4-04c4dca56a91",
  "requestedAt" : "2026-11-22T23:34:00.867908Z",
  "completedAt" : "2026-11-22T23:34:00.897066597Z"
}
```

```http
GET /api/v1/users/me/push-devices
Authorization: Bearer <JWT>
```

```http
HTTP 401

{
  "timestamp" : "2026-11-22T23:34:00.912684804Z",
  "message" : "Se requiere un token valido.",
  "error" : "Unauthorized",
  "status" : 401
}
```
