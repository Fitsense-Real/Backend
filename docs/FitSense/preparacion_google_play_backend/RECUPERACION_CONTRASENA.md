# Recuperación de contraseña por correo (PR C)

## Estado

- **Lo que ya existía** (`PasswordResetService`):
  - token de 256 bits de un solo uso, guardado solo como SHA-256;
  - caducidad de 30 minutos y como máximo 3 solicitudes por cuenta y hora;
  - la misma respuesta exista o no el correo;
  - la confirmación bloquea el token.
- **Lo que faltaba:** la entrega. Con `delivery=none` no salía nada.
- **Lo que añade esta PR:**
  - el envío real, independiente del proveedor;
  - la invalidación de sesiones;
  - un límite por IP.

**Sin proveedor configurado sigue sin salir nada.** No se ha contratado ningún servicio ni configurado ningún método de pago.

## Flujo

```
App/web ── POST /api/v1/auth/password-reset/request {email} ──► 202 (siempre el mismo cuerpo)
                      │ (si la cuenta existe, está ACTIVE y no superó 3/h)
                      ├─ guarda SHA-256(token), caduca en 30 min
                      └─ tras el COMMIT, en un hilo aparte ──► proveedor de correo ──► buzón
Persona ── abre el enlace o copia el código ──► POST /api/v1/auth/password-reset/confirm {token, newPassword}
                      ├─ 204: contraseña cambiada (BCrypt), token usado, resto de tokens invalidados,
                      │       users.token_version + 1 ⇒ todos los JWT anteriores dan 401
                      └─ 400 "El código de recuperación no es válido o ha caducado." (cualquier fallo)
```

## Contrato

**Solicitud.** La respuesta es 202 siempre, con o sin cuenta, y con el mismo cuerpo:

```http
POST /api/v1/auth/password-reset/request
Content-Type: application/json

{"email": "persona@ejemplo.invalid"}
```

```json
{"message": "Si el correo corresponde a una cuenta, recibiras un codigo para restablecer tu contrasena."}
```

**Confirmación:**

```http
POST /api/v1/auth/password-reset/confirm
Content-Type: application/json

{"token": "<43 caracteres base64url>", "newPassword": "<8-72 caracteres>"}
```

**Respuestas:**

| HTTP | Cuándo |
|---|---|
| 202 / 204 | Éxito (solicitud / confirmación) |
| 400 | Confirmación con token inválido, caducado, usado o de una cuenta no activa: mismo mensaje en todos los casos. También con contraseña fuera de 8–72 caracteres o con un cuerpo inválido |
| 429 `DEMASIADAS_SOLICITUDES` | Más de 20 peticiones por acción e IP en 15 minutos. Incluye la cabecera `Retry-After` |

## El correo

- **Asunto:** `Kora: restablece tu contrasena`. El nombre se cambia con `fitsense.mail.from-name`.
- **Cuerpo, solo texto:**
  - el enlace, si hay plantilla, o el código;
  - la caducidad en UTC;
  - "solo sirve una vez";
  - "si no lo has pedido, ignóralo";
  - "nunca te pediremos tu contraseña por correo".
- **Nunca lleva la contraseña:** el backend no la tiene en claro cuando envía el correo.
- `fitsense.password-reset.link-template`:
  - **vacía** (por omisión): el correo lleva el código y la app tiene que ofrecer una pantalla para pegarlo;
  - **con valor:** tiene que ser `https://…{token}…` (solo se admite `http://localhost` para pruebas).
- **No se inventa la URL pública:** la plantilla se rellenará cuando exista la página web o el *deep link* de la app.

## Garantías de seguridad

| Requisito | Cómo |
|---|---|
| Un solo uso | `used_at` más bloqueo de la fila. Al confirmar se invalidan los demás tokens pendientes |
| Caducidad | 30 min (configurable de 5 a 120) |
| Almacenamiento con hash | SHA-256. El token en claro solo existe en el correo |
| Sin enumeración | Siempre la misma respuesta. El correo sale **después del commit y en otro hilo**, así que el tiempo de respuesta no depende de que el envío ocurra. El límite por IP tampoco depende del correo |
| Límite de intentos | 3 solicitudes por cuenta y hora, y 20 por IP cada 15 min en cada endpoint. Un token de 256 bits no se puede adivinar por fuerza bruta |
| Hash de contraseña | BCrypt (`HashingService`, sin cambios) |
| Sesiones invalidadas | **V61** `users.token_version` y claim `tv` en el JWT. El filtro rechaza un token cuya versión no coincida. Los tokens anteriores a V61, sin `tv`, cuentan como 0 |
| Contraseña antigua rechazada | El hash se sustituye. Probado con un login |
| Logs | Solo `CORREO_ENVIADO tipo=RECUPERACION_CONTRASENA transporte=…` o `CORREO_FALLIDO … causa=HTTP 4xx`. Nunca el destinatario, el código ni la contraseña. El cuerpo de error del proveedor no se registra porque puede repetir el destinatario |
| Cuenta eliminada o no activa | No recibe código |

## Configuración

Todas las claves se leen de variables de entorno. Ninguna está en el repositorio.

| Variable | Valor para usarlo | Notas |
|---|---|---|
| `FITSENSE_PASSWORD_RESET_DELIVERY` | `email` | Con `email` la aplicación **no arranca** si el transporte es `none` |
| `FITSENSE_MAIL_TRANSPORT` | `resend` \| `brevo` \| `smtp` \| `none` | Ver [COMPARATIVA_PROVEEDORES_CORREO](COMPARATIVA_PROVEEDORES_CORREO.md) |
| `FITSENSE_MAIL_FROM` | Remitente del dominio verificado en el proveedor | Obligatorio si el transporte no es `none` |
| `FITSENSE_MAIL_FROM_NAME` | `Kora` | |
| `FITSENSE_MAIL_API_KEY` | Clave del proveedor (resend o brevo) | Secreto de Railway |
| `FITSENSE_MAIL_API_BASE_URL` | vacía | Solo para pruebas. Fuera de `localhost` tiene que ser https |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` | | Solo con `smtp`. **Railway Hobby bloquea el SMTP saliente** |
| `FITSENSE_PASSWORD_RESET_LINK_TEMPLATE` | vacía o `https://…{token}` | |
| `FITSENSE_PUBLIC_RATE_LIMIT` | 20 | Peticiones por acción e IP en 15 min |

**Detrás del proxy de Railway**, la IP del límite solo es la del cliente con `server.forward-headers-strategy=native` (PR E). Sin ese ajuste, todas las peticiones comparten la IP del proxy y el límite se vuelve global.

## Pruebas

Ninguna prueba toca un proveedor real.

| Prueba | Tipo | Qué comprueba |
|---|---|---|
| `RecuperacionPorCorreoIT` | GreenMail como SMTP simulado en un puerto local | <ul><li>Llega el correo con el código y sin la contraseña.</li><li>La base guarda solo el hash.</li><li>Al confirmar, el JWT anterior da 401, la contraseña antigua no entra y la nueva sí.</li><li>El código no se puede reutilizar.</li><li>Un correo inexistente da la misma respuesta y no genera correo.</li><li>Una cuenta eliminada no recibe código.</li><li>Límite por IP: 429 con `Retry-After`.</li><li>El log no contiene el correo, el código ni las contraseñas.</li></ul> |
| `TransportesDeCorreoTest` | Servidor HTTP local | <ul><li>Formato exacto de Resend y de Brevo: ruta, cabecera de la clave y JSON escapado.</li><li>Un rechazo devuelve solo el código HTTP.</li><li>Un proveedor inalcanzable.</li><li>Una configuración incompleta impide arrancar.</li></ul> |
| `MensajeDeRecuperacionTest` | Unitaria | Enlace o código, caducidad en UTC y validación de la plantilla |
| `RecuperacionContrasenaIT` | Ya existía, sin cambios | Uso único, caducidad, límite por cuenta y cuenta no activa |

## Lo que falta para activarlo (requiere autorización)

1. Elegir el proveedor y crear la cuenta en el plan gratuito.
   - No se ha contratado nada.
   - Verificar un dominio propio en el proveedor (registros SPF y DKIM).
   - Sin dominio propio, los proveedores solo permiten enviar a la propia cuenta.
2. Guardar `FITSENSE_MAIL_API_KEY` como variable secreta en Railway.
3. Decidir entre enlace y código.
   - Con enlace, la página web o el *deep link* tienen que existir antes de rellenar `LINK_TEMPLATE`.
4. Pantallas de la app (Flutter):
   - "olvidé mi contraseña", con el correo;
   - "introduce el código y la nueva contraseña";
   - tras un 204, volver al login. Los tokens guardados en otros dispositivos ya no sirven.
