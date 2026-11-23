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
| `…planNoVigenteYCaducado` | 1 | Un plan reemplazado antes del envío se descarta. Un evento de más de 24 h caduca |
| `…cuentaEliminada` | 1 | Durante la eliminación no se envía nada. Una vez eliminada la cuenta, no quedan filas en las cuatro tablas |
| `…fallosDeFcm` | 1 | `UNREGISTERED` desvincula el dispositivo. `UNAVAILABLE` se reintenta en su turno, sin repetir el FID desvinculado |
| `…fcmCaido` | 1 | Los reintentos se agotan (`FALLIDO`). El plan sigue publicado y el ciclo semanal funciona con FCM caído |
| `…cambioDeContrasena` | 1 | La recuperación de contraseña crea `ACCOUNT_NOTICE/PASSWORD_CHANGED`. El aviso va al dispositivo de antes. Después ese dispositivo no recibe hasta un nuevo registro |
| `…soloEventosReales` | 1 | Las operaciones FAILED y NO_FACTIBLE y la generación síncrona no notifican. El esquema impide que un intento RECHAZADA o NO_FACTIBLE cite un plan. Un plan publicado produce un evento y ninguno más |
| `ContratoPushHttpIT.contrato` | 1 | **Contrato HTTP exacto**: rutas `/api/v1/...` y cabecera JWT; campos exactos de `Dispositivo`, `Preferencias` y `ApiErrorResource`; FID aceptado y token FCM rechazado; `appVersion` opcional; 401 sin JWT; `DELETE` por ruta y por cuerpo; `DELETE` sin FID da 400; cambio de cuenta; eliminación de cuenta (JWT 401 y sin dispositivos). Genera `EJEMPLOS_REALES_HTTP.md` |
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
| **Regresión completa sobre `785bedc4`** (`mvn verify -Pintegration`, el último commit de código) | Unitarias: **1.595, 0 fallos**, 1 omitida. Integración: **863, 0 fallos**, 72 omitidas (opt-in). BUILD SUCCESS |
| CI de GitHub (`verify`) sobre `785bedc4` | Verde |
| Antecedente: regresión sobre `c40dd4b` | 1.592 y 861. El único fallo, `MigracionCuentasV60aV63IT` (fijaba la versión 63), quedó corregido en `1c433e8` |

## Lo que no se probó

Pendientes:

- **Envío real a Firebase con credenciales de `kora-fitpe`.** No hay credenciales en el entorno de desarrollo y la orden no autoriza desplegar. Procedimiento en `CONFIGURACION_RAILWAY.md` §5.
- **Prueba integral con la app Mobile.** La app tiene que ajustarse al contrato (`CONTRATO_PUSH_MOBILE.md`); el repositorio Mobile no se modificó.
