# 04 · Infraestructura, seguridad y preparación para el despliegue

**Rama auditada:** `generation` en `77b9df3`. **Fecha:** 27/11/2026.
**Alcance:** código, configuración (`application*.properties`), migraciones, documentación de despliegue y pruebas. **No se ha accedido a Railway, Neon ni Firebase.** Todo lo que depende de sus consolas se marca como **SIN VERIFICAR**.

> **Conclusión:** el backend **no está listo para producción con participantes reales**. Que la suite pase no basta: hay cinco bloqueos operativos o de privacidad (S-1, login sin límite, cierre semanal sin recuperación, copias no verificadas, monitorización inexistente). El detalle y la prioridad de cada uno están en `06_PLAN_DE_CIERRE_BACKEND.md`.

---

## 1. Arquitectura de despliegue prevista

Fuente: `docs/FitSense/preparacion_google_play_backend/PREPARACION_HTTPS_RAILWAY.md`.

- Un contenedor en Railway (`java -jar target/fitsense-backend-0.0.1-SNAPSHOT.jar`), **una sola réplica**. No hay `Dockerfile`, `railway.json` ni `Procfile` en el repositorio, así que la construcción depende de la detección automática de Railway. **SIN VERIFICAR**: cómo está configurado hoy el servicio.
- TLS terminado en el proxy de Railway; `server.forward-headers-strategy=native`.
- **PostgreSQL:** la configuración base y la documentación apuntan a **Neon** (`application.properties:3`, `MIGRACIONES_Y_DESPLIEGUE.md`). Existe una rama remota `docs/propuesta-base-datos-railway` (no integrada y no revisada en esta auditoría). **Hay que confirmar qué base usará el piloto**, porque de eso dependen las copias de seguridad (§9).
- Replicate (`gpt-5-mini`), Firebase (FCM) y el proveedor de correo son servicios externos.

## 2. Matriz de seguridad y despliegue

| # | Aspecto | Estado | Evidencia | Problema / riesgo |
|---|---|---|---|---|
| I1 | Variables de entorno | IMPLEMENTADA SIN VALIDACIÓN INTEGRAL | En `prod`, `SPRING_DATASOURCE_URL`/`USERNAME` no tienen valor por omisión: si faltan, la aplicación no arranca (`application-prod.properties`; `DespliegueHttpsIT` arranca con `prod`). La tabla de variables está en `PREPARACION_HTTPS_RAILWAY.md` §3. | Las variables reales de Railway están **SIN VERIFICAR**. |
| I2 | Secretos en el repositorio | PARCIAL | No hay contraseñas ni claves (`DB_PASSWORD`, `JWT_SECRET`, `REPLICATE_API_TOKEN`, `FITSENSE_MAIL_API_KEY`, Firebase por variable; `.gitignore:62-64` excluye los JSON de credenciales). | **S-3:** la URL del *pooler* de Neon y el usuario `neondb_owner` están como valores por omisión en `application.properties:3-4`, también en el historial. Sin el perfil `prod`, cualquier arranque con `DB_PASSWORD` definido se conecta a Neon. |
| I3 | Contraseñas | VERIFICADA | BCrypt (`BCryptHashingService`); 8–72 caracteres. | No hay política de complejidad. |
| I4 | JWT | VERIFICADA con observaciones | HS256 con secreto de al menos 32 bytes (si es más corto, no arranca); 7 días; revocación por `token_version` y por estado de la cuenta (`BearerAuthorizationRequestFilter`). | Sin *logout* ni *refresh*; **S-2** (el token lleva el `email`). Un único secreto para todos los entornos si no se separa (I20). |
| I5 | HTTPS y proxy | IMPLEMENTADA SIN VALIDACIÓN INTEGRAL | `HttpsRequeridoFilter` (403 `HTTPS_REQUERIDO`), HSTS, CSP, `nosniff`, `X-Frame-Options`; `DespliegueHttpsIT` (`httpRechazado`, `httpsTrasProxy`, `ipYEsquemaDelProxy`). | Que Tomcat confíe en la IP real del proxy de Railway está **SIN VERIFICAR** (procedimiento de diagnóstico en `PREPARACION_HTTPS_RAILWAY.md` §6). Si no confía, el limitador por IP vería la IP del proxy y bloquearía a todo el mundo a la vez. |
| I6 | CORS | VERIFICADA | Solo orígenes exactos; un comodín impide arrancar; sin credenciales de navegador (`WebSecurityConfiguration.corsConfigurationSource`, `CorsOrigins`; `CorsOriginsTest`, `DespliegueHttpsIT.cors`). | Hay que poner en `FITSENSE_CORS_ORIGINS` el dominio del panel web. |
| I7 | Rate limiting | **PARCIAL** | `LimitePorIp` en memoria (20 peticiones cada 15 min) solo en la recuperación de contraseña y la eliminación web; `fitsense.ai.generate.max-per-hour=5` en la generación. | **Login, registro y `POST /users/me/consents` sin límite.** Límites en memoria (S-4): se pierden al reiniciar y no se comparten entre réplicas. |
| I8 | Validación de entradas | VERIFICADA | Bean Validation en los recursos (`SignUpResource`, `CreateUserProfileResource`…); 400 homogéneos (`ContratoErroresApiIT.entradaMalFormada`, `SeguridadRolesIT.entradaInvalida`). | — |
| I9 | Autorización por roles | VERIFICADA | Denegación por omisión; `/api/v1/admin/**` exige ADMIN (por ruta y con `@PreAuthorize`); el rol se lee de la base en cada petición; ADMIN solo se asigna con `docs/FitSense/operaciones/cambiar_rol.sql`. `SeguridadRolesIT` (`prohibidoEnAdmin`, `anonimo`, `denegadoPorDefecto`, `rutasAdmin`, `registroSinAdmin`, `cambioDeRol`). | — |
| I10 | Acceso a datos entre usuarios (IDOR) | VERIFICADA (con huecos de prueba) | Todos los recursos `/users/me/**`, `/sessions/{id}`, `/workouts/{id}`, `/plan/{planId}` y `/plan/generation/{id}` filtran por el usuario del token. `SeguridadRolesIT.recursosAjenos`, `propiedadDelEjercicio`; `ContratoErroresApiIT.recursosAjenos`; `GeneracionAsincronaIT` (opt-in). | Sin prueba explícita entre usuarios para `/workouts/{id}/skip`, `/workouts/{id}/report` y `/sessions/{id}/finish`. Por lectura del código, la comprobación existe (`WeeklyTrainingPlanCommandServiceImpl:508-512`, `WorkoutSessionCommandServiceImpl:226-258`). |
| I11 | Manejo de errores | VERIFICADA | `GlobalExceptionHandler` (401/400/404/409/422/429/503); en `prod`, `server.error.include-*=never`. | No hay manejador genérico de `Exception`: el error 500 lo genera Spring, sin traza en `prod`. El 422 de un correo duplicado revela que la cuenta existe (decisión D14). |
| I12 | Logs sin datos sensibles | **CON ERROR (S-1)** | `ReplicateTrainingPlanGenerator.java:536` y `:824`: `log.info("Salida de la IA (prediccion {}, prompt {}, …): {}", …)` escribe en INFO el plan completo de la persona. A eso se suman los registros detallados de adaptación y dolor (p. ej. `PainMemoryService`: `DOLOR_FAMILIA usuario N: …`, visibles en la ejecución de las pruebas). `PRUEBAS_SEGURIDAD.md` S-1 lo declara **bloqueante para participantes reales**. Los correos no se registran (`grep` de `log.*email` sin resultados). | Datos de salud y entrenamiento vinculados al `user_id` en los logs de Railway, con una retención **SIN VERIFICAR** (P-14). |
| I13 | Monitorización | **NO IMPLEMENTADA** | Actuator solo expone `health`, sin detalles (`management.endpoints.web.exposure.include=health`). No hay Micrometer/Prometheus, Sentry, OpenTelemetry ni alertas (`pom.xml`). La auditoría de administración solo va al logger `fitsense.audit` (`AdminAuditInterceptor`), no se guarda y no tiene pruebas. | Nadie se entera de un cierre semanal fallido, de que Replicate caiga ni de que el correo se descarte, salvo leyendo los logs a mano. |
| I14 | Copias de seguridad | **SIN VERIFICAR** | Solo hay documentación: `MIGRACIONES_Y_DESPLIEGUE.md:38-42` (rama de Neon o `pg_dump --format=custom` fuera de Railway). En `scripts/` solo está `smoke-local.sh`. | Retención real desconocida (P-14). |
| I15 | Restauración | **NO VERIFICADA** | No hay procedimiento probado ni registro de un ensayo de restauración. | No se puede prometer que un registro perdido se recupere. |
| I16 | Migraciones | VERIFICADA con riesgos | Flyway V1–V65 (falta V45, reservada para el PR #17); `validate-on-migrate` y `outOfOrder=false` por omisión de Spring Boot; pruebas `MigracionV43IT`…`MigracionV65IT`, `MigracionV58aV59IT`, `MigracionCuentasV60aV63IT`. | (a) Si V45 se fusionara ahora, la validación fallaría en las bases que ya están en V65: habría que renumerarla. (b) `ck_pgo_lease` de V59 se creó `NOT VALID` y no se ha validado en ninguna base real. (c) La ampliación V59 en curso (rama `feature/v59-recovery-all-ai-flows`) añade **V66**: hay que coordinar su número con cualquier otra migración. (d) Flyway no tiene vuelta atrás: la única es restaurar la copia previa (I15). |
| I17 | Disponibilidad | PARCIAL | Una réplica a propósito (límites, reautenticación y cortacircuitos en memoria); health check en `/actuator/health`. | Punto único de fallo. Cada despliegue interrumpe el servicio. |
| I18 | Reinicios | PARCIAL | La generación asíncrona se recupera (lease, latido, `recoverStale`, `requeueOnShutdown`; `GeneracionAsincronaResilienciaIT`, `ReanudacionConReplicateSimuladoIT`). Las eliminaciones pendientes se reanudan cada 5 min (`EliminacionDeCuentaIT.interrumpidaYReanudada`). | **El cierre semanal no se recupera** (ver `03` §5 y `05` R-01). La cola de correo es en memoria (200 mensajes): lo pendiente se pierde al reiniciar. El endpoint síncrono, el ciclo semanal y la adaptación no tienen libro de predicciones: lo cubre la ampliación V59, todavía **pendiente**. |
| I19 | Configuración de Railway | SIN VERIFICAR | Guía en `PREPARACION_HTTPS_RAILWAY.md` §4. | Hay que comprobar en la consola: perfil `prod`, réplicas = 1, *healthcheck*, variables, dominio, retención de logs y región. |
| I20 | Aislamiento dev / pruebas / producción | PARCIAL | Perfil `local-dev` con su propia base y la guardia `BaseDeProduccionBloqueada` (las pruebas no pueden tocar la base real; `BaseDeProduccionBloqueadaTest`); `dev-log` prohibido fuera de local; IA apagada en local. | No hay un perfil `staging` ni la separación documentada de secretos (JWT, Replicate, Firebase) por entorno. La configuración base apunta a Neon (S-3). No consta un entorno de pruebas desplegado (pendiente 9 de `IMPLEMENTACIONES_PENDIENTES_MVP.md`). |

## 3. Interruptores de comportamiento en producción (valores por omisión)

| Propiedad | Valor por omisión | Observación para el piloto |
|---|---|---|
| `fitsense.ai.enabled` | `true` (sin token, se usa el motor de reglas) | Decidir junto con el presupuesto de Replicate. |
| `fitsense.ai.adaptation.prompt-version` | `v2.1` | Ver `03` §2. |
| `fitsense.planning.policy` | `PLAN-REV-2.0` | — |
| `fitsense.adaptation.progression.enabled` | `true` | — |
| `fitsense.consent.enforce-on-signup` | `false` | **Hay que encenderlo con los documentos aprobados** (`02`). |
| `fitsense.password-reset.delivery` / `fitsense.mail.transport` | `none` / `none` | La recuperación no funciona. |
| `fitsense.push.enabled` | `false` | Sin push. |
| `fitsense.cycle.manual-run.enabled` | `false` (forzado en `prod`) | Correcto. |
| `fitsense.api-docs.enabled` | `false` (forzado en `prod`) | Correcto. |
| `fitsense.security.require-https` | `true` en `prod` | Correcto. |
| `spring.task.scheduling.pool.size` | sin definir (1 hilo) | Todas las tareas `@Scheduled` comparten un único hilo (ver `03` §5). |

## 4. Protección de datos personales y de salud

| Dato | Dónde | Riesgo |
|---|---|---|
| `health_notes`, `goal_text` (texto libre) | `user_profiles`; se envían a Replicate (`PlanInputSnapshot:59-60,519`) | P-13: base legal, información a las personas y retención en Replicate sin decidir. |
| Fecha de nacimiento completa | `user_profiles.birth_date` | P-8 (minimización). La IA solo recibe la edad. |
| Salida de la IA, detalle de adaptación y dolor | Logs INFO | **S-1, bloqueante.** |
| Salida de la IA | `plan_generation_predictions.output` | Se borra al eliminar la cuenta. No hay plazo de retención. |
| Nombre y correo en el panel | `GET /api/v1/admin/participants/{id}` | Se muestran para **cualquier** cuenta USER, también de no participantes y retirados (`AdminAnalyticsQueryService.participant`, `WHERE role='USER'`). La consulta queda en el log `fitsense.audit`. |
| Consentimientos | `user_consents` | Se borran al eliminar la cuenta (P-4). |

No hay tareas de retención ni de purga para las operaciones, las predicciones, las métricas, los eventos push ni los tokens caducados. Solo existe la eliminación de la cuenta.

## 5. Notificaciones y Firebase (soporte del backend)

| # | Funcionalidad | Estado | Evidencia |
|---|---|---|---|
| N1 | Registro de tokens FCM (FID de instalación) | VERIFICADA | `PUT /api/v1/users/me/push-devices` (`DispositivosPushService.registrar`): FID `^[A-Za-z0-9_-]{16,64}$`, solo ANDROID, `appId` igual al paquete; `NotificacionesPushIT`, `ContratoPushHttpIT`. |
| N2 | Asociación con el usuario | VERIFICADA | Siempre por el usuario del JWT; si el mismo FID estaba en otra cuenta, se revoca allí (`CAMBIO_DE_CUENTA`). Índice `ux_pd_fid_activo` (V64). |
| N3 | Actualización del token | VERIFICADA | Un PUT con el mismo FID actualiza `last_seen_at`, `token_version` y `app_version`. Como máximo 5 dispositivos por usuario (desplaza el menos reciente). |
| N4 | Desvinculación al cerrar sesión | PARCIAL | **Depende de Mobile:** no hay *logout* en el servidor; Mobile debe llamar a `DELETE /push-devices/{installationId}` antes de borrar el JWT (`CONTRATO_PUSH_MOBILE.md`). |
| N5 | Eliminación de tokens | VERIFICADA | `DELETE` (revocación suave); FCM `UNREGISTERED`/`SENDER_ID_MISMATCH` → revocado; al eliminar la cuenta se borra todo. Un FID rotado no se revoca de forma automática. |
| N6 | Envío | IMPLEMENTADA SIN VALIDACIÓN INTEGRAL | Cuatro eventos del servidor: `PLAN_GENERATED`, `PLAN_ADAPTED`, `WEEKLY_PLAN_READY` y `ACCOUNT_NOTICE/PASSWORD_CHANGED` (`TipoEventoPush`, `DetectorEventosPush` cada 30 s, `EmisorPush` cada 15 s). Probado con un Google simulado (`ArranquePush*IT`). **Nunca se ha enviado a un dispositivo real desde este repositorio.** Apagado por omisión. |
| N7 | Manejo de errores | VERIFICADA (simulado) | Errores temporales con reintentos y espera creciente (de 30 s a 30 min, 5 intentos); `INVALID_ARGUMENT` permanente sin desvincular. TTL de 24 h. Sin credenciales, `TransporteNoDisponible`: la aplicación no se cae. |
| N8 | Protección de credenciales | VERIFICADA | Base64, ruta de fichero o `GOOGLE_APPLICATION_CREDENTIALS` por variable; nunca en el repositorio. |
| N9 | Recordatorios y avisos de inactividad | NO APLICA al backend / **no implementados** | No hay ningún recordatorio en el servidor. Si el estudio los quiere, tienen que ser **notificaciones locales de Mobile** o una funcionalidad nueva. |

**Qué dispara cada notificación:** las tres de plan las produce el detector, que lee filas confirmadas de `plan_generation_operations` y `plan_adaptation_attempts`. La de contraseña la escribe la transacción de recuperación. El resto (recordatorios de entrenamiento, avisos de semana) **no lo envía el backend**.

## 6. Panel administrativo y seguimiento del estudio

| # | Funcionalidad | Estado | Evidencia / problema |
|---|---|---|---|
| P1 | Consulta de usuarios | VERIFICADA | `GET /api/v1/admin/participants` (paginado ≤ 200, filtros `status`/`studyStatus`), seudonimizado (`participantCode`); `SeguridadRolesIT.datosPersonales`. |
| P2 | Identificación de participantes | PARCIAL | Usa `users.is_study_participant` (la casilla). No cruza con `user_consents` ni con la versión aprobada. Los retirados se incluyen en la analítica. |
| P3 | Visualización de adherencia | VERIFICADA | `/participants/{id}/adherence`, `/analytics/adherence`, `/trends`, `/segments`; `SeguridadRolesIT.cifras`, `AdherenciaIT`, `AdherenciaRiesgoIT`. Un segmento con n=1 identifica a una persona (lo advierte el propio recurso). |
| P4 | Historial de entrenamientos | **NO IMPLEMENTADA** en el panel | Solo `weekly_user_metrics`. No hay ningún endpoint de administración para sesiones, planes, ejercicios ni adaptaciones. |
| P5 | Adaptaciones en el panel | **NO IMPLEMENTADA** | Las adaptaciones solo se ven en `/users/me/plan/adaptations` (lo ve la propia persona). |
| P6 | Métricas | VERIFICADA | `/analytics/overview`. |
| P7 | Exportación | **NO IMPLEMENTADA** | No hay ningún endpoint de exportación. Hay consultas SQL de solo lectura en `docs/FitSense/consultas/*.sql` (`BEGIN TRANSACTION READ ONLY` … `ROLLBACK`). No existe un rol de base de datos de solo lectura ni vistas de análisis. |
| P8 | Roles y permisos | VERIFICADA | §2, I9. |
| P9 | Acceso restringido a datos de participantes | **PARCIAL** | Cualquier ADMIN ve el nombre y el correo de cualquier cuenta USER. No hay un rol de investigador seudonimizado ni una auditoría persistente. |
| P10 | Análisis sin modificar registros operativos | PARCIAL | Se puede con las consultas de solo lectura y `AdminAnalyticsQueryService` (`@Transactional(readOnly=true)`). Falta una réplica o un rol de solo lectura y una exportación reproducible. |
| P11 | Trazabilidad temporal | PARCIAL | Todo en `TIMESTAMPTZ`; los planes no se modifican (versión nueva y la anterior pasa a `REPLACED`). Pero: (a) `weekly_user_metrics` **se sobrescribe** al recalcular y `calculated_at` conserva la fecha del **primer** cálculo (`WeeklyUserMetrics:181-182`, `insertable=false, updatable=false`); (b) las sesiones y los ejercicios se actualizan en el mismo registro (un PUT sobrescribe); (c) `plan_adaptation_attempts` no está protegida para que solo se añadan filas. Para el estudio se pierde el "qué se sabía en cada momento". |
