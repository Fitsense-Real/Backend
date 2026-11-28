# 05 · Correo y recuperación de contraseña (Backend-B4.1, P0-09)

**Estado: PREPARADO SIN ACTIVAR · BLOQUEADO POR DECISIÓN** (proveedor, titular y dominio: P-10).

**La recuperación de contraseña no se ha vuelto a programar: ya existe.** Esta entrega prepara su operación con un proveedor real. No se ha contratado ningún proveedor ni se ha usado ninguna credencial real.

## 1. Lo que ya existe y se reutiliza

| Pieza | Dónde | Verificado por |
|---|---|---|
| Endpoints | `POST /api/v1/auth/password-reset/request` (siempre 202) y `/confirm` (204, o 400 con un mensaje único) | `RecuperacionContrasenaIT` |
| Token | 32 bytes de `SecureRandom`; se guarda **solo su SHA-256**; un solo uso; 30 min (5–120); invalida los demás | `RecuperacionContrasenaIT.unSoloUso`, `caducadoEInventado` |
| Revocación de sesiones | `token_version` (V61): el JWT anterior da 401 | `RecuperacionPorCorreoIT.flujoCompleto` (SMTP simulado con GreenMail) |
| Límites | 3 por hora y cuenta; 20 por IP cada 15 min | `RecuperacionContrasenaIT.limite`, `RecuperacionPorCorreoIT.limitePorIp` |
| Envío | `EnvioDeCorreo`: después del commit y fuera del hilo de la petición; cola de 200 en memoria y un reintento | — |
| Transportes | `smtp`, `resend` y `brevo` (HTTPS, sin SDK) | `CorreoConfiguracion` |
| Plantilla | `MensajeDeRecuperacion`: código o enlace; caducidad en UTC; aviso de que nunca se pide la contraseña | `MensajeDeRecuperacionTest` |
| Simulación de 12 semanas | E12-17: recuperación a mitad del estudio, con el JWT anterior revocado | `Estudio12SemanasIT` (B6) |

## 2. Configuración del proveedor (cuando se apruebe)

Railway Hobby bloquea el SMTP saliente. Recomendación (`COMPARATIVA_PROVEEDORES_CORREO.md`): **Resend por HTTPS**; alternativa, Brevo.

| Variable en Railway | Valor | Notas |
|---|---|---|
| `FITSENSE_MAIL_TRANSPORT` | `resend` (o `brevo`) | Sin ella, `none`: no se envía nada |
| `FITSENSE_MAIL_FROM` | `no-responder@<dominio verificado>` | Tiene que pertenecer al dominio verificado en el proveedor. Si no es válida, la aplicación no arranca |
| `FITSENSE_MAIL_FROM_NAME` | `Kora` (o el nombre aprobado) | — |
| `FITSENSE_MAIL_API_KEY` | **Secreto** | Solo como variable de Railway; nunca en el repositorio ni en el chat |
| `FITSENSE_MAIL_API_BASE_URL` | Vacía | Por omisión, `https://api.resend.com` o `https://api.brevo.com` |
| `FITSENSE_PASSWORD_RESET_DELIVERY` | `email` | Exige un transporte distinto de `none`; si no, la aplicación no arranca |
| `FITSENSE_PASSWORD_RESET_TTL_MINUTES` | `30` | — |
| `FITSENSE_PASSWORD_RESET_LINK_TEMPLATE` | Vacía | **Flujo con código** (decisión de esta orden para Mobile). Un enlace profundo será una mejora posterior (P-11) |

**Dominio:**
1. Verificarlo en el proveedor.
2. Publicar en el DNS los registros **SPF** (`include:` del proveedor) y **DKIM** (las claves que da el proveedor) y, si se puede, **DMARC** (`p=none` al principio).
3. **Sin SPF y DKIM, el correo acaba en spam.**

**Errores:**
- Un fallo del proveedor se registra como `CORREO_FALLIDO tipo=… transporte=… intento=… causa=…`, sin el correo de nadie, y se reintenta una vez.
- Una cola llena se registra como `CORREO_DESCARTADO`.
- La petición siempre responde 202, así que no se revela nada.
- Con B5, los dos sucesos cuentan en `ContadoresOperativos` y disparan `ALERTA_OPERATIVA tipo=CORREO_FALLIDO`.

## 3. Procedimiento de verificación con un buzón sintético (cuando haya credenciales de pruebas)

Se hace en **preproducción**, con una cuenta de prueba (`@<dominio de pruebas>`), **nunca con una persona real**.

1. **Arranque.** En el log de arranque aparecen `CORREO transporte=resend remitente configurado` y ningún `ABORTADO`.
2. **Solicitud.**

   ```bash
   curl -s -o /dev/null -w '%{http_code}\n' -X POST https://<preproduccion>/api/v1/auth/password-reset/request -H 'Content-Type: application/json' -d '{"email":"<buzon-sintetico>"}'
   ```

   Respuesta esperada: **202**. Después, `CORREO_ENVIADO tipo=RECUPERACION_CONTRASENA` en el log.
3. **Entrega.** El correo llega al buzón en menos de 2 minutos, **no** en spam, con el código y la caducidad.
4. **Confirmación.**

   ```bash
   curl -s -o /dev/null -w '%{http_code}\n' -X POST https://<preproduccion>/api/v1/auth/password-reset/confirm -H 'Content-Type: application/json' -d '{"token":"<codigo>","newPassword":"<nueva>"}'
   ```

   Respuesta esperada: **204**.
5. **Reutilización.** Repetir el paso 4 con el mismo código: **400**.
6. **Revocación de sesiones.** `GET /api/v1/users/me` con un JWT obtenido **antes** del paso 4: **401**.
7. **Vencimiento.** Pedir otro código, esperar más de 30 min y confirmar: **400**.
8. **Sin enumeración.** Solicitar con un correo inexistente: **202** idéntico y ningún correo enviado.
9. **Acta.** Registrar fecha, proveedor, dominio, tiempos de entrega y resultados.

**No se ha ejecutado:** no hay credenciales ni proveedor aprobados.

## 4. Contrato para Mobile (flujo con código)

| Paso | Llamada | Respuestas |
|---|---|---|
| "Olvidé mi contraseña" | `POST /api/v1/auth/password-reset/request` `{"email"}` | **202** siempre, con el mismo mensaje. **429** con `Retry-After` |
| Introducir el código y la contraseña nueva (8–72) | `POST /api/v1/auth/password-reset/confirm` `{"token","newPassword"}` | **204**. **400** con el mismo mensaje para un código no válido o caducado, o para una contraseña fuera de rango. **429** |
| Después del 204 | Descartar el JWT guardado (ya no sirve) e iniciar sesión con la contraseña nueva | — |

El código tiene 43 caracteres Base64url: conviene un campo de texto que admita pegarlo.

## 5. Pendiente

- **P-10:** proveedor, cuenta (titular) y dominio remitente.
- Ejecutar §3 en preproducción.
- **P-11:** enlace profundo (mejora posterior).
- **Mobile:** pantallas.
