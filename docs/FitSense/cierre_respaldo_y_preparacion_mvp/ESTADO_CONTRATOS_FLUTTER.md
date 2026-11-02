# Estado de los contratos para Flutter

**Referencia de comportamiento:** `docs/FitSense/preparacion_integracion_pr17/ESPECIFICACION_FLUTTER.md` (casos A–E).

**Alcance de este documento:** no se ha modificado Flutter. Es un inventario de la API real, sacado de los `@RestController` del código:
- `generation` 8f112d5;
- PR #17 cc97045, con los indicadores **apagados** por defecto.

**Todas las rutas** cuelgan de `/api/v1` y requieren JWT (`Authorization: Bearer`), salvo las de `/auth`.

## 1. Contratos disponibles hoy (en `generation`, sin indicadores)

| Necesidad del MVP | Endpoint | Notas |
|---|---|---|
| Registro e inicio de sesión | `POST /auth/register`, `POST /auth/login` | Devuelve el token |
| Usuario y retirada del estudio | `GET /users/me`, `POST /users/me/withdraw` | |
| **Perfil y disponibilidad** | `GET` / `POST` / `PUT /users/me/profile` | Días disponibles, minutos por sesión (P-01: 30–75 según el objetivo; un valor fuera de rango da un error de dominio con el rango), equipamiento, nivel, objetivo y restricciones |
| Catálogo (para elegir equipamiento y mostrar ejercicios) | `GET /catalog/body-parts`, `/catalog/equipment-types`, `/catalog/exercises`, `/catalog/exercises/{id}` | |
| Limitaciones del plan (por qué falta algo) | `GET /users/me/plan/limitations` | Solo lectura; explica, p. ej., «tracción sin banda» |
| **Generación inicial** | `POST /users/me/plan/generate` | IA y, si falla, el motor de reglas. Sin semana válida, error de dominio con texto (sin NOFACT-0.1) |
| **Plan semanal** | `GET /users/me/plan/current`, `GET /users/me/plan/{planId}`, `GET /users/me/plans` | Sesiones, ejercicios, dosis, descansos, minutos, guía de esfuerzo y bloque de adaptación |
| **Iniciar y registrar** | `POST /workouts/{plannedWorkoutId}/start`, `GET /sessions/current`, `GET /sessions/{sessionId}`, `PUT /sessions/{sessionId}/exercises/{plannedExerciseId}`, `POST /sessions/{sessionId}/finish`, `POST /sessions/{sessionId}/abandon` | Una sesión solo se abre antes de `expires_at` (23:59 local del día) |
| Informe posterior y omisión | `POST /workouts/{plannedWorkoutId}/report`, `POST /workouts/{plannedWorkoutId}/skip` | `skip` con motivo: falta de tiempo (con contexto: imprevisto, sin tiempo ese día, menos tiempo habitual y sus minutos), fatiga, dificultad, motivación, horario, **dolor**, otro |
| **Dolor por ejercicio** | `PUT /sessions/{id}/exercises/{peId}` con `skipReason = PAIN_OR_DISCOMFORT` | Registra la exclusión P-02 al cerrar el ciclo |
| **Exclusiones por dolor** | `GET /users/me/pain-exclusions`, `POST /users/me/pain-exclusions/{exerciseId}/reactivation` | La reactivación es la **única** vía de reincorporación: confirmación explícita, nunca automática |
| **Adherencia** | `GET /users/me/metrics/weekly`, `GET /users/me/metrics/weekly/{weekStartDate}` | Adherencia ponderada, observada y cobertura de registro |
| **Adaptación localizada y progresión** | `GET /users/me/plan/adaptations` (`AdaptationOutcome`), `GET /users/me/interventions` | `resultado`: `ADAPTACION_APLICADA`, `CONSERVACION_DELIBERADA`, `SIN_CAMBIOS_VALIDOS`, `RESPALDO_PROPUESTA_IA_INVALIDA`, `RESPALDO_SIN_IA`, `REGENERACION_SEGURA_SUSTITUCION`, `REGENERACION_SEGURA_REGLAS`, `ACORTAMIENTO_IMPOSIBLE`/`REPROGRAMACION_IMPOSIBLE`/`ADAPTACION_IMPOSIBLE`, `NO_FACTIBLE_POR_SEGURIDAD`/`NO_FACTIBLE`, `PROGRESION_DEL_BACKEND`, `SIN_ADAPTACION`. Por sesión: intervención y resultado |
| **Estados NO_FACTIBLE (adaptación)** | `GET /users/me/plan/adaptations` y la **ausencia** de plan activo en `GET /plan/current` | `motivos` en texto (técnico). Ver §3 |
| Ciclo semanal | Automático: `WeeklyCycleScheduler` revisa cada hora las zonas horarias activas y cierra la semana a las 03:00 hora local | `POST /cycle/run-for-me` existe para demostración y solo actúa sobre quien llama |

## 2. Contratos implementados detrás de indicadores (PR #17, **apagados**)

| Contrato | Indicador | Qué añade | Estado |
|---|---|---|---|
| Duración estimada por sesión | `fitsense.api.duracion-estimada.enabled` | `DurationInfoResource` en cada sesión del plan (`DURACION_POR_CONTENIDO`) | En el PR #17, **sin fusionar**, apagado |
| Actividad aeróbica semanal | `fitsense.metricas.aerobica.enabled` | `AerobicActivityResource` en el plan semanal (informativo) | En el PR #17, apagado |
| NO_FACTIBLE estructurado (NOFACT-0.1) | `fitsense.api.no-factible-estructurado.enabled` | 422 con `diagnosis` estructurado (`NoFactibleExceptionHandler`) en la generación | En el PR #17, apagado |
| Políticas C2-D, EF, V4-C, LW-PREF, REP-SUST | `fitsense.planning.policy` + `allow-candidates` | Cambian la generación, no el contrato | Apagadas; pendientes de la evaluación profesional |

**Desactivados, el recurso sale idéntico al de siempre** (lo verifican `DuracionYAerobicaAssemblerTest` y los 960 perfiles).

## 3. Lo que falta para el MVP

| Necesidad | Estado | Qué haría falta |
|---|---|---|
| Explicar un NO_FACTIBLE de **adaptación** con un mensaje comprensible | **Falta** | Un código de motivo (`SIN_SUSTITUTO_SEGURO`, `TRABAJO_SUPERIOR`, `SIN_DIAS`…), la sesión afectada y el estado de publicación en `AdaptationOutcome`. NOFACT-0.1 solo cubre la generación. Ver `ANALISIS_NO_FACTIBLE_1373.md` §6 |
| Distinguir «tu semana se mantiene» de «se cambió un ejercicio por dolor» | **Disponible** | `RESPALDO_*` frente a `REGENERACION_SEGURA_SUSTITUCION` (con la sustitución) |
| Mostrar la duración estimada | Detrás de un indicador | Activarlo requiere fusionar el PR #17 (o portar solo ese contrato) |
| Textos de la app | No están en el backend | Los textos de §3.3 de `ESPECIFICACION_FLUTTER.md` |
| Lista de equipamiento unificada | Pendiente de aprobación | `estabilizacion_ci_y_respaldo/DECISIONES_PENDIENTES.md` |

## 4. Flujo mínimo de la app (con lo disponible hoy)

1. **Registro e inicio de sesión:** `auth/register`, `auth/login`.
2. **Perfil:** `POST /users/me/profile`. Si el plan no puede cubrir algo, se explica con `GET /plan/limitations`.
3. **Generación:** `POST /users/me/plan/generate` → `GET /plan/current`.
4. **Entrenamiento:**
   - `POST /workouts/{id}/start`;
   - un `PUT …/exercises/{peId}` por ejercicio (con `PAIN_OR_DISCOMFORT` si algo duele);
   - `POST /sessions/{id}/finish`.
   - Si no se entrena: `POST /workouts/{id}/skip` con su motivo.
5. **Cierre semanal (automático):** `GET /users/me/metrics/weekly` y `GET /plan/adaptations` para explicar qué cambió y por qué; `GET /plan/current` para la semana nueva.
6. **Dolor:** `GET /users/me/pain-exclusions`; reincorporación solo con `POST …/reactivation`.
7. **Sin plan nuevo:** NO_FACTIBLE en `GET /plan/adaptations` (texto técnico hasta que exista el contrato de §3).
