# Pruebas de las notificaciones push

**Condiciones de todas las pruebas:**
- PostgreSQL 16 efímero (Testcontainers);
- un **transporte simulado** en lugar de Firebase: **ninguna prueba llama a Google**;
- sin Replicate y sin datos reales.

## Pruebas nuevas

| Prueba | Casos | Qué demuestra |
|---|---:|---|
| `NotificacionesPushIT.registroDeDispositivos` | 1 | Registro idempotente por FID (`201` y luego `200`). Se rechazan con código un token FCM, otra app y otra plataforma. Sin JWT: 401/403. El FID completo nunca vuelve en la API. Desvincular es idempotente |
| `…cambioDeCuenta` | 1 | El mismo FID en otra cuenta revoca el vínculo anterior (`CAMBIO_DE_CUENTA`). Nadie desvincula un FID ajeno |
| `…preferencias` | 1 | Por omisión, las dos categorías están activadas. Desactivar PLANES descarta también lo pendiente |
| `…planGenerado` | 1 | Una operación asíncrona COMPLETED produce un solo `PLAN_GENERATED`. El detector y el emisor son idempotentes. El destino es el FID. El mensaje tiene exactamente las 7 claves del contrato |
| `…cierreSemanal` | 1 | El ciclo semanal real publica la semana 2 (`SIN_ORDEN`). Si un segundo registro APLICADA cita el mismo plan, hay **un** evento y es `PLAN_ADAPTED` |
| `…sinDispositivoActivo` | 1 | PLAN_GENERATED sin dispositivo activo, porque nunca se registró o porque se desvinculó: el evento queda `SIN_DESTINO/SIN_DISPOSITIVOS_ACTIVOS`, sin envíos ni reintentos |
| `…planNoVigenteYCaducado` | 1 | Un plan reemplazado antes del envío se descarta. Un evento de más de 24 h caduca |
| `…cuentaEliminada` | 1 | Durante la eliminación no se envía nada. Una vez eliminada la cuenta, no quedan filas en las cuatro tablas |
| `…fallosDeFcm` | 1 | `UNREGISTERED` desvincula el dispositivo. `UNAVAILABLE` se reintenta en su turno, sin repetir el FID desvinculado |
| `…fcmCaido` | 1 | Los reintentos se agotan (`FALLIDO`). El plan sigue publicado y el ciclo semanal funciona con FCM caído |
| `…cambioDeContrasena` | 1 | La recuperación de contraseña crea `ACCOUNT_NOTICE/PASSWORD_CHANGED`. El aviso va al dispositivo de antes. Después ese dispositivo no recibe hasta un nuevo registro |
| `…soloEventosReales` | 1 | Las operaciones FAILED y NO_FACTIBLE y la generación síncrona no notifican. El esquema impide que un intento RECHAZADA o NO_FACTIBLE cite un plan. Un plan publicado produce un evento y ninguno más |
| `ContratoPushHttpIT.contrato` | 1 | **Contrato HTTP exacto**: rutas `/api/v1/...` y cabecera JWT; campos exactos de `Dispositivo`, `Preferencias` y `ApiErrorResource`; FID aceptado y token FCM rechazado; `appVersion` opcional; 401 sin JWT; `DELETE` por ruta y por cuerpo; `DELETE` sin FID da 400; cambio de cuenta; eliminación de cuenta (JWT 401 y sin dispositivos). Genera `EJEMPLOS_REALES_HTTP.md` |
| `ArranqueFirebaseTest` | 3 | **Regresión del arranque con Firebase activo**: credencial sintética y Google simulado (`GoogleSimulado`), sin salida de la JVM. Falla con 9.11.0 (`NoClassDefFoundError: JacksonFactory`) y pasa con 9.12.0. Prueba el envío por `fid` con el SDK real y la clasificación de `UNREGISTERED` |
| `ArranquePushDesactivadoIT` | 1 | Spring Boot real con el push desactivado: arranca y no hay eventos ni envíos. El registro de FID funciona |
| `ArranquePushSinCredencialesIT` | 1 | Spring Boot real con el push activado y una credencial inválida: arranca, el envío queda apagado y **0 eventos** acumulados |
| `ArranquePushActivoIT` | 1 | Spring Boot real con el push activado y una credencial sintética: arranca sin `NoClassDefFoundError` y hace de punta a punta FID → PLAN_GENERATED → FCM v1 con `fid` → `ENVIADO` |
| `MensajePushTest` | 6 | Las claves del contrato, el aviso de cuenta, la ausencia de datos sensibles en los cuatro tipos, el mensaje FCM con `fid` (y sin `token`), `restrictedPackageName`, el TTL, la clasificación de errores de FCM y el FID ofuscado en los registros |
| `PushConfiguracionTest` | 3 | Apagado por omisión. Sin credenciales, con Base64 inválido o con un archivo inexistente, el transporte queda no disponible y la aplicación no falla. Encendido sin credenciales, ni el detector ni los avisos de cuenta escriben en la base |

## Pruebas existentes ajustadas

| Prueba | Cambio |
|---|---|
| `EsquemaVigenteSobreCatalogoAnterior` | Se añade V64, que es solo esquema, para las pruebas que fijan un catálogo anterior |
| `MigracionCuentasV60aV63IT` | Migra hasta `target=63` en lugar de hasta la última. Fijaba «63» como la última versión y V64 la superaba. Las comprobaciones de V60–V63 no cambian |
| `EliminacionDeCuentaIT.ningunaTablaSinCubrir` | Sin cambios. Pasa porque las cuatro tablas nuevas están en `TABLAS_EN_ORDEN` |

## Regresión

| Ejecución | Resultado |
|---|---|
| **Regresión completa sobre `18b415c0`** (`mvn verify -Pintegration`, último commit de código: firebase-admin 9.12.0 y pruebas de arranque) | Unitarias: **1.598, 0 fallos**, 1 omitida. Integración: **866, 0 fallos**, 72 omitidas (opt-in). BUILD SUCCESS |
| CI de GitHub (`verify`) sobre `ead2a682` (el mismo código más documentación) | Verde. El run de `18b415c0` quedó cancelado porque lo reemplazó el push siguiente |
| Antecedentes | `785bedc4`: 1.595 y 863, 0 fallos. `c40dd4b`: el único fallo, `MigracionCuentasV60aV63IT`, se corrigió en `1c433e8` |

Cubiertos en esta regresión:
- registro y desvinculación de FID (`ContratoPushHttpIT`, `NotificacionesPushIT`);
- persistencia en PostgreSQL (V64);
- los cuatro eventos y la prevención de duplicados (`planGenerado`, `cierreSemanal`, `soloEventosReales`, `cambioDeContrasena`);
- los reintentos (`fallosDeFcm`, `fcmCaido`);
- la seguridad y la eliminación de cuenta (`cuentaEliminada`, `EliminacionDeCuentaIT`);
- el arranque en los tres escenarios.

## Prueba con Firebase real (reportada)

La orden de merge del responsable del proyecto (23/11/2026) indica que la **prueba real** de notificaciones con Firebase `kora-fitpe` resultó **satisfactoria**.

- Esa prueba se hizo fuera del entorno de desarrollo del backend.
- Este repositorio **no contiene** sus registros, credenciales, identificadores de dispositivo ni mensajes, y no se incorporan aquí.
- La evidencia automatizada que sí contiene el repositorio es la de este documento: SDK real con Google simulado y credencial sintética.

## Pendiente

- **Prueba integral repetible** con la app Mobile ajustada al contrato (`CONTRATO_PUSH_MOBILE.md`). El repositorio Mobile no se modificó desde el backend.
