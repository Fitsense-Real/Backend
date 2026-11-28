# Seguridad, privacidad y configuración de las notificaciones push

## Credenciales: solo en el servidor

El repositorio **no contiene** ninguna credencial de Firebase. `.gitignore` excluye `*firebase-adminsdk*.json`, `*service-account*.json` y `firebase-credentials*.json`.

**Orden de búsqueda** (`PushConfiguracion`):

1. `FITSENSE_FIREBASE_CREDENTIALS_BASE64`: el JSON de la cuenta de servicio, codificado en Base64, como **variable secreta** del entorno (en Railway, una variable marcada como secreta).
2. `FITSENSE_FIREBASE_CREDENTIALS_FILE`: ruta a un archivo montado **fuera** del repositorio.
3. Application Default Credentials (`GOOGLE_APPLICATION_CREDENTIALS`).

**Recomendaciones para la cuenta de servicio:**
- crearla en `kora-fitpe` con el **mínimo** permiso para enviar mensajes (rol de administración de Firebase Cloud Messaging);
- no darle permisos de Firestore, Storage ni Auth;
- rotar la clave si se expone.

**Si las credenciales faltan o fallan:**
- la aplicación **arranca igual**;
- el registro deja un error sin el detalle de la excepción, que podría citar parte de la credencial;
- el envío queda **apagado de hecho**: no se detectan eventos, no se registran avisos de cuenta y no se envía nada;
- nada del entrenamiento se ve afectado.

Guía para Railway: `CONFIGURACION_RAILWAY.md`.

## Variables

| Variable | Por omisión | Uso |
|---|---|---|
| `FITSENSE_PUSH_ENABLED` | `false` | Interruptor general: detector y emisor |
| `FITSENSE_FIREBASE_PROJECT_ID` | `kora-fitpe` | Proyecto |
| `FITSENSE_PUSH_ANDROID_PACKAGE` | `pe.fit.kora` | Único `appId` admitido y `restrictedPackageName` |
| `FITSENSE_FIREBASE_CREDENTIALS_BASE64` / `_FILE` | vacío | Credenciales (secreto) |
| `FITSENSE_PUSH_TTL_HOURS` | 24 | Caducidad del evento y TTL de Android |
| `FITSENSE_PUSH_DETECTION_WINDOW_HOURS` | 6 | Ventana del detector (1–48) |
| `FITSENSE_PUSH_MAX_ATTEMPTS` | 5 | Reintentos ante errores transitorios. Espera de 30 s, 60 s, 2 min… con tope de 30 min |
| `FITSENSE_PUSH_MAX_DEVICES_PER_USER` | 5 | Dispositivos activos por cuenta |

## Garantías

| Requisito de la orden | Cómo se cumple | Prueba |
|---|---|---|
| Enviar solo tras confirmar la persistencia | El detector lee filas confirmadas. El aviso de cuenta va en la misma transacción que el cambio. El emisor solo lee la bandeja | `planGenerado`, `cierreSemanal`, `cambioDeContrasena` |
| Si Firebase falla, no revertir ni interrumpir | La generación y la adaptación no conocen Firebase. El emisor corre en su propio hilo y fuera de transacciones de negocio | `fcmCaido`: el plan sigue `ACTIVE` y el ciclo semanal publica con FCM caído |
| No enviar a dispositivos desvinculados | Solo `revoked_at IS NULL`. `UNREGISTERED` desvincula | `registroDeDispositivos`, `fallosDeFcm` |
| No enviar a cuentas eliminadas | Se comprueba `users.status = 'ACTIVE'` en el momento del envío. La eliminación borra las tablas | `cuentaEliminada` y la guarda `EliminacionDeCuentaIT.ningunaTablaSinCubrir` |
| No enviar a usuarios no autorizados | Solo se registra con JWT. El FID pertenece a una sola cuenta. Al cambiar la contraseña, los dispositivos viejos dejan de recibir. `restrictedPackageName` | `cambioDeCuenta`, `cambioDeContrasena` |
| Sin datos sensibles de salud | Textos genéricos y fijos. `data` solo lleva identificadores. Los registros muestran 4 caracteres del FID | `MensajePushTest.sinDatosSensibles`, `mensajeFcm` |
| Respetar las preferencias | Se consultan **al enviar** | `preferencias` |
| No sustituir la autenticación | Ninguna dependencia ni uso de Firebase Auth. El JWT del backend sigue igual | Revisión del diff |

## Riesgos residuales

- **Un FID conocido podría registrarse desde otra cuenta.** Los FID no son secretos, pero son difíciles de obtener. El efecto se limita a avisos genéricos y a desvincular el teléfono de la cuenta original, que vuelve a vincularse en el siguiente arranque. No expone datos.
- **Instancia única recomendada.** El reclamo usa `FOR UPDATE SKIP LOCKED` y lease, así que admite varias instancias. Aun así, el resto del backend sigue la recomendación de instancia única (límites en memoria).
- **Sin validación con Firebase real.** El envío real no se probó: no hay credenciales en el entorno de desarrollo y la orden no autoriza desplegar. El transporte se probó con un simulado. La construcción del mensaje FCM se probó sin red.

## Privacidad (Google Play)

El FID es un **identificador de dispositivo**. En la sección de seguridad de datos de Play Console hay que declararlo:
- categoría «Identificadores de dispositivo u otros identificadores»;
- finalidad «Funcionalidad de la app» (notificaciones);
- sin compartir con terceros, salvo Google como encargado del transporte.

Se borra con la cuenta. Ver `../preparacion_google_play_backend/INVENTARIO_PRIVACIDAD_GOOGLE_PLAY.md`.
