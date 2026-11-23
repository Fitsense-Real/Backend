# Arranque con Firebase activo: `NoClassDefFoundError: JacksonFactory`

## Problema

El agente Mobile arrancó Spring Boot con `FITSENSE_PUSH_ENABLED=true` y una credencial, y el arranque falló con:

```
java.lang.NoClassDefFoundError: com/google/api/client/json/jackson2/JacksonFactory
```

Las pruebas anteriores no lo detectaban. Usaban un transporte simulado, o credenciales inválidas que fallaban antes de llegar al punto del error.

## Causa (verificada en el bytecode del SDK)

| | firebase-admin 9.11.0 | firebase-admin 9.12.0 |
|---|---|---|
| `ApiClientUtils.getDefaultJsonFactory()` | `com.google.api.client.json.jackson2.JacksonFactory.getDefaultInstance()` | `GsonFactory.getDefaultInstance()` (vía `JsonFactoryInstanceHolder`) |
| Quién aporta esa clase | `google-http-client-jackson2`. El SDK no la declara: llegaba **solo** de forma transitiva con `google-cloud-firestore` | `google-http-client-gson`, dependencia **directa** del SDK |

El backend excluye `google-cloud-firestore` y `google-cloud-storage` porque solo usa mensajería. Con 9.11.0, `FirebaseOptions.build()` llama a `getDefaultJsonFactory()` y no encuentra la clase.

`NoClassDefFoundError` es un `Error`, no una `RuntimeException`. Se escapaba del `catch (IOException | RuntimeException)` de `PushConfiguracion` y tumbaba la creación del bean y el arranque.

## Solución

1. **`firebase-admin` 9.11.0 → 9.12.0.** Es la corrección del propio SDK. Se mantienen la exclusión de Firestore y Storage, `setFid()` (presente en 9.12.0, comprobado con `javap`), el contrato HTTP y V64. Árbol resuelto:
   - `firebase-admin` 9.12.0;
   - `google-auth-library-oauth2-http` 1.57.0;
   - `google-api-client` y `google-api-client-gson` 2.9.2;
   - `google-http-client` y `google-http-client-gson` 2.2.0;
   - `google-http-client-apache-v2` 2.0.0;
   - `grpc-context` y `grpc-api` 1.83.1.

   **Sin** Firestore, Storage ni `google-http-client-jackson2`.

2. **Defensa en profundidad.** `PushConfiguracion.crear` también captura `LinkageError`. Si en el futuro faltara una clase del SDK, la aplicación arranca, el envío queda apagado de hecho (sin eventos ni avisos acumulados) y el registro dice:

   > Notificaciones push activadas pero el SDK de Firebase no se pudo cargar (NoClassDefFoundError: …): no se envía nada.

   La prueba de regresión falla en ese caso: el error no queda oculto.

3. **Transporte HTTP inyectable solo en pruebas.** Si existe un bean `HttpTransport`, se usa para la API de FCM **y** para el refresco OAuth de la credencial (`GoogleCredentials.fromStream(in, HttpTransportFactory)`). En producción no hay ninguno y se usa el transporte por omisión.

## Prueba que impide que reaparezca

`ArranqueFirebaseTest` (unitaria) recorre el camino real del SDK:
- usa una **credencial sintética** con estructura válida (cuenta de servicio con una clave RSA de 2048 bits generada en la prueba, sin ningún secreto real);
- intercepta todas las llamadas con **Google simulado** (`GoogleSimulado`, un `MockHttpTransport`): ninguna sale de la JVM.

| Caso | Con 9.11.0 | Con 9.12.0 |
|---|---|---|
| `arrancaConCredencialSintetica` | **FALLA**: `NoClassDefFoundError: com/google/api/client/json/jackson2/JacksonFactory` | Pasa: transporte Firebase disponible y 0 llamadas a Google al arrancar |
| `envioPorFidConElSdkReal` | **FALLA** | Pasa: token OAuth y `POST https://fcm.googleapis.com/v1/projects/kora-fitpe/messages:send` con `"fid":"<FID>"`, sin `"token"`, con `restricted_package_name` `pe.fit.kora`, `eventType` y `channel_id` |
| `unregisteredConElSdkReal` | **FALLA** | Pasa: el 404 `UNREGISTERED` de FCM se clasifica con el SDK real como `NO_REGISTRADO` |

Las ejecuciones con 9.11.0 se hicieron antes de la actualización, para confirmar que la prueba detecta el fallo.

## Arranque real de Spring Boot en los tres escenarios

| Prueba | Configuración | Resultado |
|---|---|---|
| `ArranquePushDesactivadoIT` | Push desactivado (valor por omisión) | Arranca. Transporte `apagado`. El registro de FID funciona (`201`). Sin eventos ni envíos |
| `ArranquePushSinCredencialesIT` | `enabled=true` con un Base64 que no es una cuenta de servicio completa | Arranca. Transporte `sin credenciales`. El registro de FID funciona. Con una operación COMPLETED, `detectar()` = 0, `procesar()` = 0 y **0 filas** en `notification_events`: no se acumulan eventos imposibles de enviar |
| `ArranquePushActivoIT` | `enabled=true` con credencial sintética válida y Google simulado | Arranca **sin `NoClassDefFoundError`**. Transporte `FirebaseTransportePush` y 0 llamadas a Google al arrancar. De punta a punta: registro del FID → PLAN_GENERATED → emisor → token OAuth → FCM v1 con `fid`. El evento queda `ENVIADO` con `fcm_message_id` |

Registro de arranque en `ArranquePushActivoIT`:

```
INFO  PushConfiguracion -- Notificaciones push activas: proyecto kora-fitpe, paquete pe.fit.kora, destino por FID.
```

## Lo que esto no demuestra

- **Entrega real.** Ninguna prueba usó credenciales reales de `kora-fitpe` ni un dispositivo Android. Google está simulado y la respuesta de FCM es la documentada, no una observada en producción.
- **Que FCM acepte el FID de un dispositivo concreto.** Eso solo lo prueba un envío real.

**Prueba real.** Según la orden de merge del 23/11/2026, la prueba con Firebase `kora-fitpe` y un dispositivo resultó satisfactoria. Sus registros no forman parte de este repositorio (ver `PRUEBAS.md`).
