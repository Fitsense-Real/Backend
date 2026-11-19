# Solicitud de eliminación desde la web (PR B)

Este es el backend de la página pública que Google Play exige: tiene que permitir pedir la eliminación de la cuenta **sin la app**. La página en sí no forma parte de esta PR, y **no hay URL pública definitiva**: los ejemplos usan `https://<dominio-web>/…`.

**Depende de la PR C** (servicio de correo). Sin un transporte de correo configurado:
- la solicitud responde igual;
- no se envía nada, así que el flujo no es operativo.

## Flujo

```
Página web (sin login)                         Backend
1. Formulario: correo ───────────────► POST /api/v1/account-deletion/requests {email}
                                       ◄── 202 (siempre igual)
                                       └─ si es una cuenta ACTIVE: token 256 bits (SHA-256 en la base, 30 min,
                                          un solo uso), correo tras el commit con
                                          https://<dominio-web>/eliminar-cuenta?token=…
2. La persona abre el enlace: la página muestra el aviso de irreversibilidad y un botón.
   (Abrir el enlace no borra nada: no hay confirmación por GET.)
3. Botón "Eliminar definitivamente" ─► POST /api/v1/account-deletion/confirm {token, confirm:true}
                                       ├─ 200 DELETED {deletionId, completedAt}
                                       ├─ 202 PENDING {deletionId}  (se completa sola)
                                       └─ 400 (token inválido/caducado/usado; mismo mensaje)
4. "Consultar estado" ───────────────► GET /api/v1/account-deletion/requests/{deletionId}
                                       └─ 200 {status, requestedAt, completedAt} | 404
```

El paso 3 ejecuta **el mismo procedimiento** que `DELETE /api/v1/users/me` (`EliminacionDeCuentaService`), con origen `WEB`:
- la cuenta pasa a DELETED y sus sesiones de la app quedan revocadas;
- las generaciones en curso se cancelan;
- se hace el borrado completo, que se reanuda si se interrumpe.

## Contrato

**Solicitud**

```http
POST /api/v1/account-deletion/requests
Content-Type: application/json

{"email": "persona@ejemplo.invalid"}
```

```json
{"message": "Si el correo corresponde a una cuenta, recibiras un enlace para confirmar la eliminacion."}
```

Siempre responde 202 con el mismo cuerpo, exista o no la cuenta, y con `Cache-Control: no-store`.

**Confirmación**

```http
POST /api/v1/account-deletion/confirm
Content-Type: application/json

{"token": "<43 caracteres base64url del enlace>", "confirm": true}
```

```json
{"status": "DELETED", "deletionId": "9b2e…", "requestedAt": "2026-11-19T21:00:00Z", "completedAt": "2026-11-19T21:00:00Z"}
```

**Estado**

```http
GET /api/v1/account-deletion/requests/9b2e…
```

```json
{"status": "PENDING", "deletionId": "9b2e…", "requestedAt": "2026-11-19T21:00:00Z"}
```

La referencia es un UUID aleatorio que solo conoce quien confirmó. La respuesta no incluye correo, nombre ni código.

**Errores.** El cuerpo es el `ApiErrorResource` de siempre.

| HTTP | `details[0]` | Cuándo |
|---|---|---|
| 400 | `CONFIRMACION_REQUERIDA` | Falta `confirm=true` |
| 400 | — | Token inválido, caducado o usado, o cuenta no activa: siempre *"El enlace de eliminacion no es valido o ha caducado."* También un cuerpo inválido o una referencia que no es un UUID |
| 404 | — | Referencia desconocida |
| 429 | `DEMASIADAS_SOLICITUDES` | 20 peticiones por acción e IP cada 15 minutos (configurable); cabecera `Retry-After` |
| 503 | `ELIMINACION_NO_REGISTRADA` | No se pudo registrar la eliminación. El token ya se gastó: hay que pedir otro enlace |

## Garantías

| Requisito | Cómo |
|---|---|
| No hace falta la app ni la contraseña | Rutas públicas (`/api/v1/account-deletion/**` en `PUBLIC_PATHS`) |
| Verificar que el correo es suyo | Solo quien recibe el correo tiene el token |
| No revelar qué correos existen | Misma respuesta siempre. El correo sale después del commit y en otro hilo. El límite por IP no depende de la cuenta |
| Tokens temporales, de un solo uso y guardados de forma segura | `account_deletion_tokens` (V62): solo el SHA-256, 30 min, `used_at`. Al confirmar se gastan todos los pendientes de la cuenta. La fila se bloquea (`FOR UPDATE`) |
| Límites y protección contra abuso | <ul><li>Como mucho 3 correos por cuenta y hora: evita llenar de correos el buzón de un tercero.</li><li>20 peticiones por IP y acción.</li><li>Un token de 256 bits no se puede adivinar.</li></ul> |
| Sin borrados accidentales | Solo `POST` con `confirm=true` borra. Abrir el enlace, una vista previa o el antivirus que lo escanea no borran nada |
| La misma lógica de eliminación | `EliminacionDeCuentaService.eliminar(userId, WEB)` |
| Logs | Solo `CORREO_ENVIADO tipo=ELIMINACION_CUENTA_WEB` y el id de la eliminación. Nunca el correo ni el token |

## Configuración

| Variable | Por omisión | Notas |
|---|---|---|
| `FITSENSE_MAIL_TRANSPORT`, `FITSENSE_MAIL_FROM`, `FITSENSE_MAIL_API_KEY` | `none` | Las mismas que la PR C |
| `FITSENSE_ACCOUNT_DELETION_WEB_LINK_TEMPLATE` | vacía | `https://<dominio-web>/eliminar-cuenta?token={token}`. Si está vacía, el correo lleva el código y la página tiene que pedirlo |
| `FITSENSE_ACCOUNT_DELETION_WEB_TTL_MINUTES` | 30 | Entre 5 y 120 |
| `fitsense.account-deletion.web.max-requests-per-hour` | 3 | Por cuenta |

## Qué tiene que hacer la página web

Pendiente, fuera de esta PR:

1. **Una URL pública estable** que se declara en Google Play Console. Tiene que explicar:
   - qué se borra;
   - qué no alcanza el borrado (ver [POLITICA_ELIMINACION_PROPUESTA](POLITICA_ELIMINACION_PROPUESTA.md), apartado 4);
   - el plazo;
   - la diferencia con retirarse del estudio.
2. **El formulario del correo** y el mensaje neutro de respuesta.
3. **La ruta `/eliminar-cuenta?token=…`**:
   - lee el token de la URL y lo guarda en memoria;
   - lo quita de la barra de direcciones (`history.replaceState`) para que no quede en el historial ni en el `Referer`;
   - muestra el aviso de irreversibilidad;
   - al pulsar el botón, hace el `POST /confirm`.
4. **Mostrar el estado** con el `deletionId` devuelto: "Eliminada" o "En proceso, se completará automáticamente".
5. **Configuración del servidor de la página:**
   - servirla por HTTPS;
   - sin analítica de terceros en la ruta del token;
   - `Referrer-Policy: no-referrer`.
6. **CORS:** si la página está en otro origen, hay que añadirlo a `FITSENSE_CORS_ALLOWED_ORIGINS`. Esta PR no abre CORS.

## Pruebas

`EliminacionWebIT`, con GreenMail, PostgreSQL efímero y sin Replicate:

| Caso | Qué comprueba |
|---|---|
| Flujo completo | <ul><li>Llega el correo con el enlace y en la base solo está el hash.</li><li>Un GET no borra y, sin `confirm`, responde 400.</li><li>Con confirmación, responde 200 DELETED con origen WEB.</li><li>La cuenta desaparece: el login falla y el JWT de la app da 401.</li><li>El estado se consulta sin datos personales.</li><li>El token no se puede reutilizar.</li><li>Los logs no contienen el correo ni el token.</li></ul> |
| Correo inexistente | <ul><li>Misma respuesta y ningún correo enviado.</li><li>Referencia desconocida: 404; malformada: 400.</li></ul> |
| Token caducado o inventado | 400 con el mismo mensaje en los dos casos |
| Límite por cuenta | 4 solicitudes producen 3 correos |
| Límite por IP | 429 en la solicitud y en la confirmación (corta la fuerza bruta) |

Además, `EliminacionDeCuentaIT.ningunaTablaSinCubrir` cubre la tabla nueva.
