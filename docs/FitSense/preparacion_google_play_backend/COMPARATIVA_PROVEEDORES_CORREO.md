# Comparativa de proveedores de correo (para Railway)

**Consultada el 19/11/2026.** Los precios y los límites cambian: hay que confirmarlos en la página de cada proveedor antes de crear la cuenta. **No se ha contratado ningún servicio ni configurado ningún método de pago.**

## Restricción de Railway

- En los planes Free, Trial y Hobby, el **SMTP saliente (puertos 25, 465 y 587) está bloqueado**. Solo está disponible desde Pro, que cuesta USD 20 al mes según el personal de Railway en su foro.
- Railway recomienda usar una API HTTPS para el correo, sea cual sea el plan.
- Son respuestas del personal en el foro de la comunidad, no la documentación oficial: comprobarlo en la documentación de *outbound networking* antes de decidir.

**Conclusión:** sin pasar a Pro, solo sirve un proveedor con **API HTTPS**. Por eso el backend implementa `resend` y `brevo` sobre HTTPS (puerto 443), con el cliente del JDK y sin SDK. Además mantiene `smtp` para no depender de un proveedor.

## Comparativa

| Proveedor | Transporte en FitSense | Plan gratuito (según su web y reseñas, a confirmar) | Ventajas | Inconvenientes |
|---|---|---|---|---|
| **Resend** | `resend` (HTTPS) | 100 correos al día, unos 3.000 al mes, sin tarjeta y permanente | API mínima, documentación clara, verificación de dominio sencilla. Railway la cita como alternativa | Límite diario bajo, aunque suficiente para un estudio de decenas de participantes. Empresa joven |
| **Brevo** (antes Sendinblue) | `brevo` (HTTPS) | 300 correos al día, sin tarjeta | Más volumen gratis, con API y SMTP. Proveedor europeo | El plan gratuito añade su marca a los correos. Panel orientado a marketing |
| Amazon SES | No implementado (necesitaría una firma AWS SigV4 o su SDK) | De pago por uso; la capa gratuita depende de la cuenta de AWS | Muy barato a escala | Hay que salir del *sandbox* con una solicitud, y la cuenta de AWS lleva facturación. Configuración más pesada |
| Postmark, Mailgun, SendGrid | No implementados. Se añadirían con un adaptador pequeño como los de Resend y Brevo | Planes gratuitos muy limitados o de prueba (sin verificar hoy) | Buena entregabilidad | Los planes gratuitos cambian a menudo |
| SMTP de un buzón personal (Gmail u otro) | `smtp` | — | Sin cuenta nueva | Bloqueado en Railway Hobby. Contraseñas de aplicación y límites de envío. **No recomendado** para correo transaccional |

## Recomendación

1. **Resend por HTTPS** (`FITSENSE_MAIL_TRANSPORT=resend`):
   - funciona en Railway Hobby sin SMTP;
   - el plan gratuito cubre de sobra la recuperación de contraseña y las solicitudes de eliminación de un estudio pequeño;
   - la integración es la más simple.
2. **Alternativa: Brevo** (`FITSENSE_MAIL_TRANSPORT=brevo`), si hace falta más volumen gratis o un proveedor europeo. Hay que aceptar la marca en el pie del correo.
3. En cualquiera de los dos casos, **verificar un dominio propio** (SPF y DKIM) antes de usarlo con participantes. Sin eso, el correo no llega o acaba en spam.

**Cambiar de proveedor** solo requiere variables de entorno:
- `FITSENSE_MAIL_TRANSPORT`
- `FITSENSE_MAIL_API_KEY`
- `FITSENSE_MAIL_FROM`

El mismo servicio (`CorreoSaliente` / `EnvioDeCorreo`) lo usa la solicitud web de eliminación (PR B): no hay un segundo canal de correo.

## Decisiones pendientes

En [PENDIENTES_APROBACION](PENDIENTES_APROBACION.md):
- proveedor y titular de la cuenta;
- dominio remitente;
- si el proveedor procesa los datos fuera del país del estudio. El correo electrónico es un dato personal y hay que declararlo en la política de privacidad.

## Fuentes

- [Resend · precios](https://resend.com/pricing.md)
- [Resend · qué incluye cada plan](https://resend.com/docs/knowledge-base/what-is-resend-pricing)
- [Resend · nuevo plan gratuito](https://resend.com/blog/new-free-tier.md)
- [Brevo · API de correo, 300 al día](https://www.brevo.com/features/email-api/?rtype=n2go)
- [Brevo · correo transaccional](https://www.brevo.com/bulk-send-transactional-emails)
- [Railway Station · SMTP saliente bloqueado en Hobby](https://station.railway.com/questions/smtp-blocked-for-n8n-password-recovery-i-8fd0a3e3)
- [Railway Station · SMTP tras pasar a Pro](https://station.railway.com/questions/outbound-smtp-not-working-despite-pro-pl-87b04b89)
