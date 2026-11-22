# Configuración de las notificaciones push en Railway

**No se ha desplegado nada.** Esta guía describe cómo se hará cuando se autorice. Las credenciales de Firebase **nunca** entran en GitHub.

## 1. Obtener la credencial (una sola vez, fuera del repositorio)

1. En Firebase Console, abrir el proyecto `kora-fitpe` y ir a **Configuración del proyecto → Cuentas de servicio → Generar nueva clave privada**. Se descarga un JSON.
2. Recomendado: usar una cuenta de servicio con el permiso mínimo para enviar mensajes (rol de administración de Firebase Cloud Messaging), sin acceso a Firestore, Storage ni Auth.
3. Codificar el JSON en Base64 en una sola línea, en la máquina de quien administra:

   ```bash
   base64 -w0 kora-fitpe-xxxx.json     # macOS: base64 -i kora-fitpe-xxxx.json | tr -d '\n'
   ```

4. **Borrar el archivo local** cuando la variable esté guardada. No se adjunta a correos, tickets, chats ni commits. `.gitignore` excluye `*firebase-adminsdk*.json`, `*service-account*.json` y `firebase-credentials*.json` como salvaguarda.

## 2. Variables del servicio backend en Railway

| Variable | Valor | Tipo |
|---|---|---|
| `FITSENSE_FIREBASE_CREDENTIALS_BASE64` | La línea Base64 del paso 1.3 | **Secreta**: marcarla como sellada (*sealed*) si el plan lo permite, para que no se pueda volver a leer desde el panel |
| `FITSENSE_PUSH_ENABLED` | `true` cuando se autorice el envío; `false` o ausente hasta entonces | Normal |
| `FITSENSE_FIREBASE_PROJECT_ID` | `kora-fitpe` (valor por omisión: no hace falta definirla) | Normal |
| `FITSENSE_PUSH_ANDROID_PACKAGE` | `pe.fit.kora` (valor por omisión) | Normal |

**No usar** `FITSENSE_FIREBASE_CREDENTIALS_FILE` en Railway: obligaría a montar un archivo con el secreto en un volumen.

**No definir** `GOOGLE_APPLICATION_CREDENTIALS`: la vía recomendada es la variable Base64.

**Rotación:**
1. generar una clave nueva;
2. reemplazar la variable y redesplegar;
3. revocar la clave anterior en la consola de Google Cloud.

## 3. Qué hace el backend según la configuración

Comprobado en `PushConfiguracionTest` y `NotificacionesPushIT`:

| `FITSENSE_PUSH_ENABLED` | Credencial | Resultado |
|---|---|---|
| ausente o `false` | cualquiera | **Apagado.** No se detectan eventos, no se registran avisos de cuenta y no se envía nada. Los endpoints de dispositivos y preferencias funcionan: la app puede registrarse antes de que se active el envío |
| `true` | ausente | **Apagado de hecho.** El registro dice «activadas pero sin credenciales válidas: no se envía nada». Igual que la fila anterior: ni eventos, ni avisos, ni envíos. La aplicación arranca con normalidad |
| `true` | Base64 inválido, JSON que no es una cuenta de servicio o archivo inexistente | **Apagado de hecho**, igual que la fila anterior. Por seguridad, el registro no incluye el detalle de la excepción |
| `true` | válida | **Activo.** El registro dice «Notificaciones push activas: proyecto kora-fitpe, paquete pe.fit.kora, destino por FID» |

Ningún proceso de entrenamiento depende de Firebase. Con FCM caído o mal configurado, la generación, la adaptación y el ciclo semanal siguen igual (`NotificacionesPushIT.fcmCaido`).

## 4. Requisitos del despliegue

- **Migración V64.** La aplica Flyway al arrancar. Es solo esquema y usa `gen_random_uuid()`, que viene integrada en PostgreSQL 13 o superior.
- **Instancia única.** Es la recomendación vigente del backend. El emisor ya reclama los eventos con `FOR UPDATE SKIP LOCKED` y un *lease*, y admitiría varias instancias.
- **Salida HTTPS** hacia `fcm.googleapis.com` y `oauth2.googleapis.com`. Railway la permite por omisión.

## 5. Verificación tras el despliegue (cuando se autorice)

1. Buscar en el registro de arranque la línea «Notificaciones push activas…».
2. Con una cuenta de prueba y un teléfono con la app: hacer `PUT /api/v1/users/me/push-devices` y generar el primer plan en modo asíncrono. Debe llegar «Tu plan está listo».
3. Comprobar en la base:

   ```sql
   SELECT status, status_reason FROM notification_events ORDER BY recorded_at DESC LIMIT 5;
   SELECT status, error_code FROM notification_deliveries ORDER BY attempted_at DESC LIMIT 5;
   ```

   Lo esperado es `ENVIADO`, con `fcm_message_id` informado en la entrega.

Estas pruebas con Firebase real y con la app **siguen pendientes**.
