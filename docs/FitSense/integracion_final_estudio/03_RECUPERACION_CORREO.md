# 03 · Recuperación de contraseña con correo real

**PR:** [#97](https://github.com/Fitsense-Real/Backend/pull/97) (Backend-C2), solo el texto del correo, **fusionada** (`09a92028`). Además, [#102](https://github.com/Fitsense-Real/Backend/pull/102) (C6, abierta y sin fusionar) trata el hallazgo de §8.
**Antecedentes:** `cierre_preproduccion/05_CORREO_Y_RECUPERACION.md` (configuración y procedimiento) y `preparacion_google_play_backend/COMPARATIVA_PROVEEDORES_CORREO.md` (proveedores). No se repiten aquí.

**Estado: el correo NO está operativo.** No hay proveedor contratado ni credenciales, y no se ha hecho ninguna prueba de entrega real.

## 1. Lo que se reutiliza sin cambios

Todo lo que pide la orden ya existe y no se ha tocado:
- **Solicitud.** `POST /api/v1/auth/password-reset/request` responde siempre 202 con el mismo cuerpo, exista o no la cuenta.
- **Confirmación.** `POST /api/v1/auth/password-reset/confirm` responde 204, o 400 con un único mensaje para código incorrecto, usado o caducado.
- **Token.**
  - 256 bits aleatorios, en base64url de 43 caracteres;
  - de un solo uso; confirmar invalida los demás pendientes;
  - en la base solo se guarda su **SHA-256**;
  - la fila se bloquea, para que dos confirmaciones simultáneas no lo usen dos veces.
- **Caducidad.** `FITSENSE_PASSWORD_RESET_TTL_MINUTES`, 30 por omisión (entre 5 y 120).
- **Límites.**
  - 3 solicitudes por hora y cuenta (`FITSENSE_PASSWORD_RESET_MAX_PER_HOUR`); por encima, la respuesta no cambia y no se envía nada;
  - por IP, 429 con `Retry-After` en la séptima solicitud.
- **Revocación de sesiones.** Cambiar la contraseña incrementa la versión del token: los JWT anteriores reciben 401. Queda un aviso de seguridad en la misma transacción (V64).
- **Envío.** Después del commit y fuera del hilo de la petición, para que el tiempo de respuesta no revele si la cuenta existe.

## 2. Transportes compatibles

| Transporte | Requisitos técnicos para Kora | Valoración |
|---|---|---|
| `resend` (API HTTPS) | Clave de API; dominio verificado con SPF y DKIM (y DMARC recomendado); remitente de ese dominio | **Recomendado.** Funciona en Railway, que en el plan Hobby bloquea el SMTP saliente. El plan gratuito publicado (100 al día, unos 3.000 al mes; **a confirmar al contratar**) cubre de sobra un estudio de decenas de personas |
| `brevo` (API HTTPS) | Clave de API; remitente o dominio verificado | Alternativa. Más volumen gratuito, pero el plan gratuito añade su marca a los correos |
| `smtp` (Spring Mail) | Servidor, puerto, usuario y clave SMTP | Solo fuera de Railway Hobby. No recomendado para correo transaccional desde un buzón personal |

### 2.1 Configuración recomendada (sin secretos)

```properties
FITSENSE_MAIL_TRANSPORT=resend
FITSENSE_MAIL_FROM=no-responder@<dominio verificado del estudio>
FITSENSE_MAIL_FROM_NAME=Kora
FITSENSE_MAIL_API_KEY=<SECRETO: solo en las variables de Railway>
FITSENSE_PASSWORD_RESET_DELIVERY=email
FITSENSE_PASSWORD_RESET_TTL_MINUTES=30
# Vacía: modalidad de CÓDIGO (no hay App Links nuevos en esta orden)
FITSENSE_PASSWORD_RESET_LINK_TEMPLATE=
```

Si la configuración es incoherente, la aplicación **no arranca**: `delivery=email` sin transporte, un remitente inválido o `dev-log` fuera de los perfiles locales.

## 3. El correo (C2)

Antes el texto no tenía tildes («contrasena») y solo daba la caducidad en UTC. Ahora:

```text
Asunto: Kora: código para restablecer tu contraseña

Hola:

Recibimos una solicitud para restablecer la contraseña de tu cuenta de Kora.

Para elegir una contraseña nueva, copia este código y pégalo en la app, en «Restablecer contraseña»:

<código de 43 caracteres>

Caduca en 30 minutos (el 28/11/2026 a las 14:30 UTC) y solo sirve una vez. Al cambiar la contraseña se cierran las sesiones
abiertas en otros dispositivos.

Si no lo pediste tú, ignora este mensaje: tu contraseña no cambia.
Nunca te pediremos tu contraseña ni este código por correo, teléfono o mensaje.
```

Características:
- texto plano en UTF-8: los tres transportes lo envían así, y Spring Mail usa UTF-8 por omisión;
- no lleva la contraseña, el correo del destinatario en el cuerpo ni ningún otro dato de la cuenta;
- el código es largo (43 caracteres) a propósito: es lo que hace inútil probar códigos. Por eso el correo dice que se **copie y pegue**. Acortarlo exigiría limitar los intentos de confirmación por código, y **no se ha cambiado**.

## 4. Seguridad operativa

| Punto | Comprobación |
|---|---|
| Ningún código en los logs de producción | `dev-log`, que lo escribe, **no arranca** fuera de los perfiles `local` y `local-dev`. Con `none` y `email` solo se registra `CORREO_ENVIADO tipo=RECUPERACION_CONTRASENA transporte=…`, sin destinatario. Lo prueba `RecuperacionPorCorreoIT.flujoCompleto`: ni el código, ni el correo, ni las contraseñas aparecen en el log |
| Fallos de entrega | Dos intentos con 2 s de pausa. Luego `CORREO_FALLIDO … causa=…`, sin el cuerpo de la respuesta del proveedor (que repite el destinatario), el contador `CORREO_FALLIDO` y `ALERTA_OPERATIVA tipo=CORREO_FALLIDO` |
| Cola en memoria | Un hilo y una cola de 200. Si se llena: `CORREO_DESCARTADO`, el contador y la alerta |
| **Riesgo de reinicio** | La cola vive en memoria: **un reinicio o un despliegue pierde los correos pendientes**. El token sigue en la base, pero la persona no lo recibe y tiene que pedirlo otra vez (hasta 3 por hora). Para un estudio pequeño se acepta. Mitigación: no desplegar en horas de uso y mirar `CORREO_FALLIDO`. **No se cambia en esta orden**; una cola persistente necesitaría una migración |
| Compatibilidad con Mobile | Modalidad de código con las pantallas ya existentes: pedir el código con el correo, y después código más nueva contraseña. Respuestas: 202; 204 / 400 / 429. Tras el 204, Mobile descarta el JWT guardado, porque ya no vale. Ejemplos 11 y 12 en `02` §4 |

## 5. Pruebas

Con correo **simulado** (GreenMail para SMTP; un servidor HTTP local para Resend y Brevo):

| Caso que pide la orden | Prueba |
|---|---|
| Solicitud | `RecuperacionPorCorreoIT.flujoCompleto`, `RecuperacionContrasenaIT.sinEnumeracion` |
| Código correcto | `RecuperacionPorCorreoIT.flujoCompleto` |
| Código incorrecto | `RecuperacionContrasenaIT.caducadoEInventado` |
| Código ya utilizado | `RecuperacionContrasenaIT.unSoloUso`, `RecuperacionPorCorreoIT.flujoCompleto` |
| Caducidad | `RecuperacionContrasenaIT.caducadoEInventado` |
| Límites y 429 | `RecuperacionContrasenaIT.limite` (por cuenta), `RecuperacionPorCorreoIT.limitePorIp` (429 con `Retry-After`) |
| Contraseña actualizada | `RecuperacionPorCorreoIT.flujoCompleto` (login con la nueva) |
| JWT anterior revocado | `RecuperacionPorCorreoIT.flujoCompleto` (401 con el antiguo) |
| Texto del correo | `MensajeDeRecuperacionTest` |
| Transportes | `TransportesDeCorreoTest` (Resend y Brevo contra un servidor simulado: formato, errores sin datos) |

Resultado en la rama C2: **11 unitarias y 14 de integración, 0 fallos**.

## 6. Prueba de entrega real (preparada, NO ejecutada)

Se hace cuando haya un **proveedor autorizado** y credenciales de pruebas, **solo** en preproducción y **solo** con un buzón sintético del equipo. Se sigue el procedimiento de `cierre_preproduccion/05_CORREO_Y_RECUPERACION.md` §3, midiendo además:

| Medida | Cómo | Criterio |
|---|---|---|
| Tiempo de entrega | Hora del `CORREO_ENVIADO` en el log frente a la hora de llegada al buzón | < 2 min en 10 envíos |
| Bandeja | Bandeja de entrada, no spam, en al menos dos proveedores de buzón (p. ej. Gmail y Outlook) | 10/10 en bandeja de entrada |
| Autenticación | Cabeceras `Authentication-Results`: SPF, DKIM y DMARC | `pass` en los tres |
| Texto | Tildes, código completo y caducidad legibles en Android | Revisión visual |

**No se declara operativo el correo sin esta prueba y su acta.**

## 7. Estado

| Punto | Clasificación |
|---|---|
| Flujo de recuperación (código, límites, revocación) | **RESUELTO** (ya existía; probado) |
| Texto del correo apto para uso real | **RESUELTO** (integrado: #97) |
| Configuración del proveedor | **PREPARADO SIN ACTIVAR** |
| Contratación y dominio verificado | **BLOQUEADO POR APROBACIÓN** (P-10) |
| Prueba de entrega real | **BLOQUEADO POR INFRAESTRUCTURA** (sin proveedor ni buzón) |
| Cola de correo persistente | **PENDIENTE** (riesgo aceptado y documentado) |
| Solo vale el código más reciente | **PREPARADO SIN ACTIVAR** (#102, abierta; §8) |

## 8. Hallazgo de Mobile: el código anterior seguía valiendo

**Observación:** al pedir un código nuevo, el anterior seguía siendo válido hasta que se usaba uno de los dos.

**Diagnóstico** sobre el código de `generation`:
- `solicitar` creaba el token sin tocar los pendientes; solo `confirmar` invalidaba los demás.
- **No era una decisión del contrato:** ningún documento ni prueba lo exigía. `RecuperacionContrasenaIT.unSoloUso` confirma el segundo y solo después comprueba que el primero no vale.
- **Era una debilidad menor:** varios códigos válidos a la vez para la misma cuenta.

**Corrección**, en [#102](https://github.com/Fitsense-Real/Backend/pull/102) y **no fusionada**: esta orden no lo autoriza y no se mezcla con #97.

| Punto | Comportamiento con #102 |
|---|---|
| Política | **Solo vale el código más reciente.** Una solicitud nueva invalida los pendientes |
| Límite | Por encima de 3 por hora no se emite código ni se invalida nada: el último enviado sigue valiendo |
| Concurrencia | Solicitar y confirmar bloquean primero la cuenta y después los tokens: sin cruce de bloqueos, límite exacto, nunca dos códigos vigentes. Un código nunca sirve dos veces |
| Reenvíos | Si un correo anterior llega tarde, su código da 400. El correo nuevo lo avisa: «Si pediste más de un código, solo vale el del correo más reciente» |
| Disponibilidad | Si el correo más reciente se pierde (cola en memoria y reinicio), hay que pedir otro (§4) |
| Contrato HTTP | Sin cambios (202/204/400/429). Sin migración |
| Pruebas | `soloElMasReciente`, `porEncimaDelLimiteNoInvalida`, `simultaneas` (5 rondas en paralelo, sin 5xx) y las de recuperación existentes: 0 fallos |

**Mientras #102 no se fusione**, el comportamiento sigue siendo el anterior.
