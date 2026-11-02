# Contratos finales para Flutter

**Referencia congelada propuesta:** `generation` **después de fusionar #22–#26**, con la etiqueta `backend-mvp-1.0` (a crear cuando los fusiones).
- Hasta entonces, la referencia es el commit de integración local que se probó: `generation` 7c19703 más los 5 PR.
- Todos los cambios son **aditivos o correcciones de estado**: la app ya desarrollada (`feature/mvp-integracion-api`) no se rompe.

**Convenciones:**
- Base: `{API_BASE_URL}` = `http(s)://host:puerto/api/v1`.
- JWT: `Authorization: Bearer <token>` en todo salvo `/auth/**`. Los tokens caducan a los 7 días.
- JSON en UTF-8. Fechas `YYYY-MM-DD`; instantes ISO-8601 con zona.

## 1. Errores (todos con `ApiErrorResource`)

`{timestamp, status, error, message, details[]}`. `message` está en español y está pensado para mostrarse tal cual.

| Código | Cuándo |
|---|---|
| 400 | Validación (`details` con `campo: mensaje`); id no numérico; JSON mal formado (#23); código de recuperación no válido (#25) |
| 401 | Sin token, token caducado o no válido; **login fallido** (#23; antes 500) |
| 403 | Ruta de administración o fuera de contrato |
| 404 | Recurso inexistente **o de otro usuario** (no se distingue) |
| 409 | Conflicto de escritura concurrente |
| 422 | Regla de dominio (rango de minutos por objetivo, perfil congelado, plan ya activo, sesión caducada…) |

## 2. Endpoints del MVP

| Flujo | Endpoint | Respuesta |
|---|---|---|
| Registro | `POST /auth/register` `{email, password(8–72), firstName, lastName?, timezone? (IANA; por defecto America/Lima), acceptsStudyParticipation}` | 201 `UserResource` (**sin token**) |
| Login | `POST /auth/login` `{email, password}` | 200 `{userId, email, fullName, token, role}`; 401 |
| Recuperación (#25) | `POST /auth/password-reset/request` `{email}` | **Siempre 202** `{message}` |
| | `POST /auth/password-reset/confirm` `{token, newPassword}` | 204; 400. **No operativa sin proveedor de correo** |
| Usuario | `GET /users/me` · `POST /users/me/withdraw` | `UserResource` |
| Perfil | `GET` / `POST` (201) / `PUT /users/me/profile` | 404 sin perfil; 422 con el rango por objetivo; `PUT` → 422 tras el primer plan |
| Catálogo | `GET /catalog/equipment-types`, `/catalog/body-parts`, `/catalog/exercises[/{id}]` | |
| Plan | `POST /users/me/plan/generate` (201) · `GET /users/me/plan/current` · `/plan/{planId}` · `/plans` · `/plan/limitations` | `current`: **día local del usuario** (#24); 404 sin plan |
| Adaptación | `GET /users/me/plan/adaptations` y `plan.adaptation` | `AdaptationOutcome` (§3) |
| Sesiones | `POST /workouts/{id}/start` (201) · `GET /sessions/current` (204 sin sesión) · `GET /sessions/{id}` · `PUT /sessions/{id}/exercises/{plannedExerciseId}` · `POST /sessions/{id}/finish` `{sessionRpe 1–10, satisfaction?, activeMinutes?}` · `POST /sessions/{id}/abandon` | Caducidad: 23:59 locales del día (`expiresAt`) |
| Informe y omisión | `POST /workouts/{id}/report` · `POST /workouts/{id}/skip` `{reason, lackOfTimeContext?, availableMinutes? 15–180}` (204) | |
| Dolor | `PUT …/exercises/{peId}` con `skipReason=PAIN_OR_DISCOMFORT`; `GET /users/me/pain-exclusions`; `POST /users/me/pain-exclusions/{exerciseId}/reactivation` `{confirmNoPain: true, comment?}` | La reincorporación **solo** se hace con la confirmación explícita |
| Adherencia | `GET /users/me/metrics/weekly[/{weekStartDate}]` | Solo cuando la semana ha cerrado |
| Intervenciones | `GET /users/me/interventions` | |

## 3. `AdaptationOutcome` (#22)

**Campos de siempre (no cambian):** `attemptId`, `weekStartDate`, `resultingPlanId`, `resultado`, `estado`, `decision`, `llamadasIa`, `motivos[]` (texto técnico) y `sesiones[{origenPlannedWorkoutId, intervencion, resultado}]`.

**Campos nuevos (aditivos):**

| Campo | Valores |
|---|---|
| `estadoPublicacion` | `PUBLICADO` \| `NO_PUBLICADO` |
| `motivoCodigo` | `NoFactibleReason` o `null` (filas anteriores o publicado). Catálogo cerrado: `SIN_SUSTITUTO_SEGURO`, `TRABAJO_SUPERIOR_AL_PERMITIDO`, `DOLOR_SIN_RESOLUCION_SEGURA`, `SEMANA_NO_VALIDA`, `SEMANA_SIN_SESIONES`, `EXCLUIDO_REAPARECE`, `PLAN_YA_ACTIVO`, `SIN_SEMANA_GENERABLE`, `SEMANA_MEDIDA_NO_ENCONTRADA`, `DISPONIBILIDAD_NO_CUMPLIBLE`. **Un código desconocido se trata como genérico** |
| `motivoTexto` | Texto neutro del backend (o `null`) |
| `codigos[]` | Todos los códigos, el de fondo primero |
| `sesionAfectada` | `{plannedWorkoutId, fecha, enfoque}` o `null` |
| `restricciones[]` | `[{tipo: "EXCLUSION_DOLOR", exerciseId}]` |

**Mensajes de la app** (ya implementados en la rama móvil), según `resultado`:

| `resultado` | Mensaje |
|---|---|
| `CONSERVACION_DELIBERADA`, `SIN_CAMBIOS_VALIDOS`, `*_IMPOSIBLE`, `RESPALDO_*`, `SIN_ADAPTACION` | «Tu plan se mantiene esta semana» |
| `ADAPTACION_APLICADA` | «Se ajustó una sesión debido a una dificultad registrada» |
| `REGENERACION_SEGURA_*` | «Se sustituyó un ejercicio por una restricción de seguridad» |
| `NO_FACTIBLE*` | «No fue posible preparar una nueva rutina que cumpla todas tus restricciones». Con #22, además `motivoTexto` |

## 4. Fuera del contrato del MVP (no usar todavía)

| Elemento | Situación |
|---|---|
| Duración por contenido, métrica aeróbica, NOFACT-0.1 de la generación | PR #17, apagados |
| URL de medios | Decisión pendiente; hoy solo hay rutas relativas: no construirlas |
| Cambio de zona horaria después del registro | No existe |
| `/cycle/run-for-me` | Solo en local o en pruebas |
