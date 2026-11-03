# Seguridad, roles y endpoints

Referencia del sistema de roles (RBAC) del backend, del contrato de la API para Flutter y Angular, y de su puesta en marcha. Rama `seguridad-rbac`, que depende de `generation` (secuencia de fusión en §9).

**Alcance MVP:** dos roles, ADMIN hereda USER, USER solo ve lo suyo y ADMIN ve la analítica global. Se reutiliza la autenticación existente, se conservan los endpoints de Flutter y no hay gestión de cuentas desde la API.

## 1. Modelo de usuarios y roles

| Elemento | Definición |
|---|---|
| Roles | `USER` y `ADMIN` (`UserRole`). Columna `users.role VARCHAR(20) NOT NULL DEFAULT 'USER'`, con `ck_users_role` (V35) |
| Jerarquía | `ROLE_ADMIN > ROLE_USER` (`WebSecurityConfiguration.roleHierarchy`). ADMIN puede todo lo de USER y, además, `/api/v1/admin/**` |
| Alcance de USER | Solo sus propios datos: los servicios filtran por el usuario del token. Un recurso ajeno responde **404**, igual que uno inexistente, para no confirmar que existe |
| Origen del rol | La base de datos, en cada petición. El JWT solo lleva `sub` (user_id) y `email`. Un cambio de rol vale desde la petición siguiente, sin token nuevo |
| Estado de la cuenta | Solo una cuenta `ACTIVE` se autentica. Un JWT aún vigente de una cuenta `INACTIVE` o `DELETED` responde **401** (`BearerAuthorizationRequestFilter`) |
| Registro público | Siempre crea `USER`: el comando de registro no tiene rol y el constructor de `User` lo fija. Un campo `role` en el cuerpo se ignora |
| Creación de ADMIN | Solo con el procedimiento de ejecución única `docs/FitSense/operaciones/cambiar_rol.sql` (§7). Ninguna ruta ni ningún código de la aplicación escribe `users.role` |
| Revocación | El mismo script con `v_rol = 'USER'`. Vale desde la petición siguiente. Desactivar la cuenta (`status = 'INACTIVE'`) corta todo acceso de inmediato |

**Respuestas de error:**
- **401**: sin token, con un token inválido o vencido, o con una cuenta no activa.
- **403**: token válido sin el rol que pide la ruta, o una ruta fuera del contrato (se deniega por defecto).
- **404**: recurso ajeno o inexistente.

**Lo que ADMIN no puede.** Ser ADMIN no da acceso a credenciales, secretos ni operaciones de infraestructura. No hay endpoints para nada de eso, y ADMIN no puede cambiar roles desde la API.

**Tareas programadas.** `WeeklyCycleScheduler`, `OverdueWorkoutScheduler` y `StaleSessionScheduler` no pasan por HTTP. No necesitan sesión ni simulan un ADMIN; la prueba las invoca sin contexto de seguridad.

## 2. Matriz de endpoints y permisos

"Propio" significa que el usuario sale del token y nunca de la URL. Las rutas llevan el prefijo `/api/v1`.

### Públicos

| Método | Ruta | DTO de entrada → salida | App |
|---|---|---|---|
| POST | `/auth/register` | `SignUpResource` → `UserResource` (201) | Flutter |
| POST | `/auth/login` | `SignInResource` → `AuthenticatedUserResource` (incluye `role`) | Flutter, Angular |
| GET | `/actuator/health` | — | Railway |

### USER (ADMIN también, por la jerarquía), sobre datos propios

| Método | Ruta | DTO de entrada → salida | App |
|---|---|---|---|
| GET | `/users/me` | → `UserResource` (incluye `role`) | Flutter, Angular |
| POST | `/users/me/withdraw` | → `UserResource` | Flutter |
| GET, POST, PUT | `/users/me/profile` | `CreateUserProfileResource` / `UpdateUserProfileResource` → `UserProfileResource` | Flutter |
| GET | `/users/me/plan/current` | → `WeeklyTrainingPlanResource` | Flutter |
| POST | `/users/me/plan/generate` | → `WeeklyTrainingPlanResource` | Flutter |
| GET | `/users/me/plan/{planId}` | → `WeeklyTrainingPlanResource` (ajeno: 404) | Flutter |
| GET | `/users/me/plans` | → lista de `PlanSummaryResource` | Flutter |
| POST | `/workouts/{plannedWorkoutId}/skip` | `SkipWorkoutResource` (ajeno: 404) | Flutter |
| POST | `/workouts/{plannedWorkoutId}/start` | → `WorkoutSessionResource` (ajeno: 404) | Flutter |
| POST | `/workouts/{plannedWorkoutId}/report` | `ReportWorkoutResource` → `WorkoutSessionResource` (ajeno: 404) | Flutter |
| GET | `/sessions/current` | → `WorkoutSessionResource` | Flutter |
| GET | `/sessions/{sessionId}` | → `WorkoutSessionResource` (ajena: 404) | Flutter |
| PUT | `/sessions/{sessionId}/exercises/{plannedExerciseId}` | `RecordExerciseResource` → `WorkoutSessionResource` (sesión ajena: 404; ejercicio de otro entrenamiento: 422) | Flutter |
| POST | `/sessions/{sessionId}/finish` | `FinishSessionResource` → `WorkoutSessionResource` (ajena: 404) | Flutter |
| POST | `/sessions/{sessionId}/abandon` | → `WorkoutSessionResource` (ajena: 404) | Flutter |
| GET | `/users/me/interventions` | → lista de `InterventionResource` | Flutter |
| GET | `/users/me/metrics/weekly` | → lista de `WeeklyMetricsResource` | Flutter |
| GET | `/users/me/metrics/weekly/{weekStartDate}` | → `WeeklyMetricsResource` | Flutter |
| GET | `/config/active` | → `CalculationConfigResource` (sin cambios) | Flutter |
| GET | `/catalog/body-parts`, `/catalog/equipment-types` | → listas | Flutter |
| GET | `/catalog/exercises?bodyPart&equipment&maxDifficulty&search&limit` | → lista | Flutter |
| GET | `/catalog/exercises/{exerciseId}` | → `ExerciseDetailResource` | Flutter |

### Solo ADMIN (`/api/v1/admin/**`): regla de ruta + `@PreAuthorize`, cada petición auditada

| Método | Ruta | Filtros | DTO de salida | App |
|---|---|---|---|---|
| GET | `/admin/analytics/overview` | `from`, `to` | `AnalyticsOverviewResource` | Angular |
| GET | `/admin/analytics/adherence` | `from`, `to`, `goalType`, `fitnessLevel`, `trainingLocation` | `AdherenceDistributionResource` | Angular |
| GET | `/admin/analytics/adherence/trends` | igual que la anterior | `AdherenceTrendResource` | Angular |
| GET | `/admin/analytics/segments` | `by` (obligatorio: `GOAL_TYPE`, `FITNESS_LEVEL`, `TRAINING_LOCATION`, `RISK_LEVEL`) + los de adherencia | `SegmentsResource` | Angular |
| GET | `/admin/participants` | `status` (`ACTIVE`, `INACTIVE`, `DELETED`), `studyStatus` (`ENROLLED`, `WITHDRAWN`, `NOT_PARTICIPANT`), `page` (desde 0), `size` (1–200, 50 por defecto) | `ParticipantPageResource` | Angular |
| GET | `/admin/participants/{participantId}` | — | `ParticipantDetailResource` (nombre y correo; acceso registrado aparte) | Angular |
| GET | `/admin/participants/{participantId}/adherence` | `from`, `to` | `ParticipantAdherenceResource` | Angular |

**Por qué estos siete y no más.** Son exactamente los del contrato pedido y cada uno alimenta una vista del panel:
- `overview`: la portada;
- `adherence`: la distribución de cada indicador;
- `adherence/trends`: la gráfica semanal;
- `segments`: la comparación por grupos;
- `participants`, `participants/{id}` y `participants/{id}/adherence`: el listado, la ficha y la serie de una persona.

No hay endpoints de gestión de cuentas ni de roles. El panel puede leer los umbrales vigentes en `GET /config/active`, que ADMIN también alcanza por la jerarquía.

`participantId` es el `user_id`. Es un identificador interno, no un dato personal. El listado y la analítica usan `participantCode` cuando hace falta un seudónimo visible.

### Deshabilitados en producción

| Ruta | Estado | Cómo se habilita (solo desarrollo o pruebas) |
|---|---|---|
| POST `/cycle/run-for-me` | 403 para todos | `FITSENSE_MANUAL_CYCLE_ENABLED=true`: vuelve a USER |
| `/swagger-ui/**`, `/v3/api-docs/**` | 401 sin token, 403 con token | `FITSENSE_API_DOCS_ENABLED=true`: pública |
| Cualquier otra ruta | 403 | — |

## 3. Analítica de ADMIN: definiciones

**Sin indicadores nuevos.** Cada cifra es una columna de `weekly_user_metrics`, con su fórmula en `ADHERENCIA.md`, o una estadística descriptiva de esa columna (media, mediana con `percentile_cont`, cuartiles, mínimo, máximo). No se recalcula nada. El SQL es de solo lectura, en `AdminAnalyticsQueryService`.

| Campo de la API | Columna | Definición |
|---|---|---|
| `officialAdherence…`, `OFFICIAL_ADHERENCE` | `weighted_adherence_pct` | A_w, la adherencia oficial de la tesis (sin pesos, pese al nombre heredado) |
| `FREQUENCY_ADHERENCE` | `frequency_adherence_pct` | Sesiones válidas / previstos |
| `WORKOUT_ADHERENCE` | `workout_adherence_pct` | Sesiones completadas / previstos |
| `EXERCISE_ADHERENCE` | `exercise_adherence_pct` | Ejercicios completados / asignados |
| `REGISTRATION_COVERAGE`, `…CoveragePct` | `registration_coverage_pct` | Previstos con registro / previstos |
| `riskLevels` | `risk_level` | LOW, MODERATE, HIGH o CRITICAL; NONE es una semana sin nivel (sin plan) |
| `dropouts` | `is_dropout` | Abandono según `dropout.days_without_workout` de la configuración |

**La adherencia observada** (`observed_adherence_pct`) **no se expone.** Es interna de la adaptación y no es la adherencia de la tesis.

**Población y filtros:**
- **Población:** cuentas `USER` con `is_study_participant = true`, como exige el comentario de V1.
  - Las cuentas ADMIN y las del equipo sin consentimiento no entran.
  - Los retirados sí: sus semanas anteriores al retiro son datos del estudio.
- **Periodo:** `from` y `to` acotan `week_start_date`, ambos incluidos. Con `from > to` responde 400.
- **Segmentos:** objetivo, nivel y lugar salen del perfil actual, porque el perfil no se versiona por semana. Un segmento de un solo participante lo identifica, así que la interfaz debe tratarlo con cuidado.
- **Semana sin plan:** todas las adherencias son `NULL` y no entran en medias ni medianas, nunca como 0 ni como 100.
- **Sin datos:** los contadores son 0, las medias y medianas `null`, y las listas van vacías. Nunca se inventa un valor.
- **Semanas históricas:** las calculadas con la fórmula anterior a `ef472ee` no se distinguen por columna (ver `ADHERENCIA.md`, «Datos históricos»). Para evaluar la tesis, usar `from` desde el despliegue de la fórmula vigente.

**Datos personales:**
- La analítica y el listado no llevan nombre ni correo.
- El detalle (`/admin/participants/{id}`) sí los lleva, y cada consulta deja `ADMIN_PERSONAL_DATA_VIEWED`.
- El detalle no incluye notas de salud, fecha de nacimiento ni medidas: ninguna función administrativa actual las usa.

**Auditoría.** Va en el logger `fitsense.audit`, una línea por evento, solo con identificadores internos:
- `ADMIN_REQUEST adminId method path status ms`: cada petición que llega a un controlador de `/admin`.
- `ADMIN_ACCESS_DENIED userId method path`: un USER intentó entrar.
- `ADMIN_PERSONAL_DATA_VIEWED adminId participantId`.

Los cambios de rol quedan registrados en la salida del script (`NOTICE ROL_CAMBIADO`), que el responsable guarda.

## 4. Compatibilidad con Flutter y Angular

**Qué se revisó:** la app Flutter accesible, `FitSense-2-0/MobileApp` (rama `main`, último commit `82105e3` del 28/08/2026).
- Solo llama a `/auth/login`, `/auth/register` y `/auth/me`, con su propio modelo (`id`, `firstName`, `lastName`, `email`, `role`).
- No usa `/config/active` ni `run-for-me`.
- Su contrato no coincide del todo con este backend: espera `id` en lugar de `userId` y llama a `/auth/me`, que aquí no existe.
- Por eso **no** se toma como prueba concluyente del consumidor real. La decisión fue no cambiar ningún contrato existente.

| Punto | Estado | Efecto en Flutter |
|---|---|---|
| `GET /config/active` | **Sin cambios** (se revirtió la versión reducida) | Ninguno |
| `role` en el login y en `/users/me` | Campo **añadido**; los demás campos no cambian | Compatible: un cliente que no lo lee lo ignora. El modelo de `MobileApp` ya declara `role` |
| `POST /cycle/run-for-me` | 403 en producción (pedido explícito) | Solo afecta si la app lo llamaba; la app revisada no lo hace |
| Token vigente de una cuenta INACTIVE | 401 (corrección pequeña de un hueco existente) | Tratar el 401 como sesión cerrada, igual que un token vencido |
| CORS restringido | Solo navegadores | Ninguno: una app nativa no envía `Origin` |
| Swagger apagado | Solo documentación | Ninguno |

**Angular:**
- No guarda secretos: solo la URL del backend y el token del usuario que inicia sesión.
- No hay claves privadas en el cliente.

## 5. Configuración por entorno

| Variable | Por defecto | Desarrollo | Producción (Railway) |
|---|---|---|---|
| `FITSENSE_CORS_ORIGINS` | vacío (ningún origen) | `http://localhost:4200` | Los orígenes HTTPS exactos de Vercel, cuando estén definidos |
| `FITSENSE_API_DOCS_ENABLED` | `false` | `true` | sin definir |
| `FITSENSE_MANUAL_CYCLE_ENABLED` | `false` | `true` si hace falta | sin definir |

Todo es seguro por defecto: una variable olvidada deja la opción cerrada, no abierta.

### Procedimiento CORS para Vercel y Railway

1. Desplegar Angular en Vercel y anotar el dominio de **producción**, por ejemplo `https://<proyecto>.vercel.app`, y el dominio propio si se configura uno.
2. En Railway, en las variables del servicio del backend, poner `FITSENSE_CORS_ORIGINS` con esos orígenes separados por comas:
   - exactos: esquema, host y puerto opcional;
   - sin barra final ni ruta;
   - sin `*`.
3. Redesplegar el backend. Un valor inválido o con comodín impide arrancar y queda en el log (`Origen CORS invalido`): un error de configuración nunca termina abriendo el acceso.
4. **Vistas previas de Vercel:** cada despliegue tiene una URL distinta y no se autorizan automáticamente. Para probarlas, añadir explícitamente un alias estable de rama, o usarlas contra un backend de desarrollo.
5. CORS no es autorización. Aunque un origen esté autorizado, cada petición sigue necesitando un token de ADMIN para `/admin`.

## 6. Plan de migración (V35)

V35 es el siguiente número libre:
- `db/migration` llega a V34 y ninguna rama remota usa el 35.
- Las dos V35 anteriores del historial (`b6fd9ae`, `4801e58`) se retiraron antes de aplicarse en Neon (Flyway en V33 el 27/10).

**Qué hace.** Solo agrega la columna `role`, con valor por defecto `USER`, y el CHECK `ck_users_role` que admite solo USER y ADMIN. En PostgreSQL 11 o posterior es una operación de metadatos, sin reescribir la tabla.

**Por qué solo eso:** las columnas de rastro (`role_updated_at`, `role_updated_by`) y su CHECK se retiraron. No son necesarias para un RBAC seguro: el rol solo lo cambia quien tiene las credenciales de la base, mediante el script de §7, y la salida del script es el registro.
- Las cuentas existentes quedan USER y no se modifica ningún otro dato.
- Un backend anterior funciona sobre una base con V35, porque `ddl-auto=validate` ignora columnas extra. Volver de versión no exige deshacer la migración.

**Pasos, ninguno ejecutado:**
1. **Lectura en Neon, de solo lectura:**
   ```sql
   SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 3;
   ```
   Confirmar que no existe una V35 y que V34 está o va a estar aplicada (PR de `generation`).
2. Seguir la secuencia de fusión de §9.
3. Con autorización explícita, desplegar el backend. Flyway aplica V35 al arrancar, como las migraciones anteriores. **Ese despliegue es la ejecución de la migración en Neon.**
4. Comprobar:
   ```sql
   SELECT role, count(*) FROM users GROUP BY role;
   ```
   Debe dar todas las cuentas como USER.

## 7. Creación del primer ADMIN

Se eligió un procedimiento de ejecución única, `docs/FitSense/operaciones/cambiar_rol.sql`, en lugar de una promoción al arrancar.
- **Por qué es más seguro:** la aplicación no tiene ningún camino de elevación de privilegios, ni siquiera desactivado.
- **Quién puede ejecutarlo:** solo quien ya tiene las credenciales de la base. El script no concede ningún acceso nuevo.

**Pasos:**
1. La persona se registra con la app e inicia sesión al menos una vez. Eso prueba que controla la cuenta y su contraseña.
2. El responsable del despliegue confirma por un canal propio que la cuenta es suya.
3. En el editor SQL de Neon, leer su `user_id`:
   ```sql
   SELECT user_id, status, role, last_login_at FROM users WHERE email = lower('<correo>');
   ```
4. Copiar el script y rellenar sus cuatro constantes:
   - `v_email` y `v_user_id`;
   - `v_rol = 'ADMIN'`;
   - `v_confirma = 'CAMBIAR <user_id> A ADMIN'`.
5. Ejecutarlo y guardar el `NOTICE` `ROL_CAMBIADO …`, junto con quién lo ejecutó.
6. No guardar la copia rellenada. El archivo del repositorio queda con las constantes vacías y, tal cual, no hace nada.
7. Verificar: el ADMIN inicia sesión en Angular y `GET /api/v1/users/me` devuelve `"role":"ADMIN"`.

**El script se niega, sin cambiar nada:**
- si falta la confirmación o no coincide;
- si el correo y el `user_id` no son de la misma cuenta;
- si la cuenta no está ACTIVE;
- si nunca inició sesión.

**Revocar:** el mismo script con `v_rol = 'USER'` y `v_confirma = 'CAMBIAR <user_id> A USER'`.

## 8. Pruebas

- **`SeguridadRolesIT`:** 25 casos. Usa registro, login, JWT y filtro reales sobre PostgreSQL 16, con V1–V35.
- **`CorsOriginsTest`:** 10 casos.

| Requisito | Caso |
|---|---|
| USER accede a lo suyo | `Usuario.susFunciones` |
| USER recibe 403 en cada ruta ADMIN | `Usuario.prohibidoEnAdmin` (las 7 rutas) |
| Sin token: 401 | `Usuario.anonimo` |
| Se deniega por defecto | `Usuario.denegadoPorDefecto` |
| USER no ve recursos de otro | `Aislamiento.recursosAjenos` (plan, entrenamiento, sesión: 404) |
| Propiedad del ejercicio | `Aislamiento.propiedadDelEjercicio` (de otro entrenamiento o de otra persona: 422; sesión ajena: 404; nada queda registrado) |
| ADMIN accede a lo de USER (rutas) | `Administrador.heredaUsuario`, `loginConRol` |
| Jerarquía en `@PreAuthorize` | `JerarquiaPorMetodo.metodos`: ADMIN pasa un método `hasRole('USER')`, USER no pasa `hasRole('ADMIN')`, y sin autenticación se rechaza |
| ADMIN consulta la analítica global | `Administrador.rutasAdmin`, `Analitica.cifras` (medias, medianas, niveles y abandonos comprobados contra datos sembrados; excluye no participantes y ADMIN) |
| Seudonimización | `Analitica.datosPersonales` |
| JWT válido de cuenta INACTIVE o DELETED | `CuentasYRoles.cuentaInactiva` (401) |
| El registro público nunca crea ADMIN | `CuentasYRoles.registroSinAdmin` |
| El cambio de rol vale en la petición siguiente | `CuentasYRoles.cambioDeRol` (promover y revocar con el mismo token) |
| La promoción exige autorización explícita | `Promocion.*` (script vacío, confirmación, identidad, titularidad, estado; la base solo admite USER y ADMIN) |
| Ciclo manual bloqueado | `Endurecimiento.cicloManual` (USER y ADMIN: 403) |
| Swagger cerrado | `Endurecimiento.documentacion` |
| `config/active` sin cambios de contrato | `Endurecimiento.configuracion` |
| CORS por origen | `Endurecimiento.cors`, `CorsOriginsTest` |
| Tareas programadas | `Endurecimiento.tareasProgramadas` |
| Generación, adaptación y progresión intactas | Regresión completa y `GeneracionReferenciaIT` (72/72) |

### Regresión (28/10/2026, commit `9988af3`, alcance MVP)

| Fase | Resultado |
|---|---|
| Unitarias | 809 de 809 |
| Integración | 245 ejecutadas, 0 fallos y 10 omitidas (las opcionales: Replicate real, Neon y lectura) |
| `GeneracionReferenciaIT` | 72 de 72 planes iniciales idénticos |
| CI en GitHub (`verify`) | En verde sobre `9988af3`, 6 min 21 s |

**Las 7 pruebas P2-D sobre la réplica preparada** (`InvariantesV33IT`, `PublicacionAtomicaIT`, `DisparadorDiferidoIT`) usan una base sin Flyway que valida el esquema.
- En local pasan con la V35 mínima aplicada a la réplica `fs-p2d` (no a Neon).
- En CI se omiten: necesitan `fitsense.pruebas.url`.
- Para correrlas contra la rama de pruebas de Neon (FitSenseTests), esa rama necesitará V35 cuando se autorice.

## 9. Secuencia de fusión

**El problema:**
- Fitsense-Real/Backend#1 es `generation` → `main`.
- Fitsense-Real/Backend#2 es `seguridad-rbac` → `generation`.
- Fusionar #2 en `generation` **después** de #1 deja RBAC en `generation`, pero no en `main`.

**Secuencia correcta.** Ninguno de estos pasos está autorizado todavía:
1. Revisar y fusionar #1 en `main`.
2. Cambiar la base de #2 a `main`. Como #1 ya estará dentro, el diff de #2 queda reducido a la seguridad. Comprobar el diff.
3. Si `main` avanzó, traer `main` a `seguridad-rbac` (merge, sin reescribir historia). Esperar a que el CI quede en verde.
4. Revisar y fusionar #2 en `main`.

**No** fusionar #2 en `generation` antes que #1: obligaría a revisar la seguridad dentro del diff funcional de #1.

## 10. CI

`.github/workflows/ci.yml` se ejecuta en cada PR y en cada push a `main` y `generation`:
- `mvn -B -ntp verify -Pintegration` en Ubuntu con JDK 21: compilación, pruebas unitarias e integración sobre PostgreSQL 16 efímero (Testcontainers).
- No usa secretos y no toca Neon ni Replicate.
- Las pruebas que los necesitan son opcionales y se omiten solas: Replicate real, Neon y la réplica P2-D, que exige `fitsense.pruebas.url`.
- La guardia `BaseDeProduccionBloqueada` impide conectarse a producción.
