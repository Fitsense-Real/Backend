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
| `MensajePushTest` | 6 | Las claves del contrato, el aviso de cuenta, la ausencia de datos sensibles en los cuatro tipos, el mensaje FCM con `fid` (y sin `token`), `restrictedPackageName`, el TTL, la clasificación de errores de FCM y el FID ofuscado en los registros |
| `PushConfiguracionTest` | 2 | Apagado por omisión. Sin credenciales, con Base64 inválido o con un archivo inexistente, el transporte queda no disponible y la aplicación no falla |

## Pruebas existentes ajustadas

| Prueba | Cambio |
|---|---|
| `EsquemaVigenteSobreCatalogoAnterior` | Se añade V64, que es solo esquema, para las pruebas que fijan un catálogo anterior |
| `MigracionCuentasV60aV63IT` | Migra hasta `target=63` en lugar de hasta la última. Fijaba «63» como la última versión y V64 la superaba. Las comprobaciones de V60–V63 no cambian |
| `EliminacionDeCuentaIT.ningunaTablaSinCubrir` | Sin cambios. Pasa porque las cuatro tablas nuevas están en `TABLAS_EN_ORDEN` |

## Regresión

| Ejecución | Resultado |
|---|---|
| Regresión completa (`mvnw verify -Pintegration`, árbol del primer commit) | Unitarias: **1.592, 0 fallos**, 1 omitida. Integración: **861, 1 fallo** (`MigracionCuentasV60aV63IT`, la versión fija «63»), 72 omitidas (opt-in) |
| Tras corregirla, con la configuración final | `NotificacionesPushIT` 10/10, `MigracionCuentasV60aV63IT` 1/1, `EliminacionDeCuentaIT` 7/7, `RecuperacionContrasenaIT` 5/5, `RecuperacionPorCorreoIT` 4/4, `MensajePushTest` 6/6 y `PushConfiguracionTest` 2/2 |

## Lo que no se probó

- **Envío real a Firebase con credenciales de `kora-fitpe`.** No hay credenciales en el entorno de desarrollo y la orden no autoriza desplegar.
- **La app Mobile.** No hay agente Mobile disponible y el contrato está pendiente de su confirmación.
