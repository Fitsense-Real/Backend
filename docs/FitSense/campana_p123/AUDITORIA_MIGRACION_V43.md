# Auditoría de la migración V43

**Archivo:** `src/main/resources/db/migration/V43__pain_memory_and_safe_regeneration.sql`.

**Estado de los entornos:** V43 **no se ha aplicado en ningún entorno compartido** (ni Neon ni Railway; el PR #16 no está fusionado ni desplegado). Por eso los ajustes de esta auditoría se hicieron **en la propia V43, antes de su primera aplicación**, sin crear una migración V44. Ninguna migración ya aplicada se modificó.

## 1. Estructura

### `user_safety_exclusions` (P-02)

| Columna | Tipo | Nulo | Restricción |
|---|---|---|---|
| `exclusion_id` | BIGINT IDENTITY | no | PK |
| `user_id` | BIGINT | no | FK → `users` |
| `exercise_id` | BIGINT | no | **FK → `exercises`** (nueva en la auditoría) |
| `reason` | VARCHAR(40) | no | CHECK `DOLOR_IDENTIFICADO` / `DOLOR_SESION_SIN_IDENTIFICAR` |
| `source_plan_id` | BIGINT | no | **FK → `weekly_training_plans`** (nueva) |
| `source_planned_workout_id` | BIGINT | no | **FK → `planned_workouts`** (nueva) |
| `source_planned_exercise_id` | BIGINT | sí | **FK → `planned_workout_exercises`** (nueva); nulo cuando el dolor es de sesión |
| `event_date` | DATE | no | |
| `recorded_by` | VARCHAR(30) | no | CHECK `OMISION` / `CICLO_SEMANAL` / `SESION` |
| `recorded_at` | TIMESTAMPTZ | no | DEFAULT now() |
| `status` | VARCHAR(20) | no | CHECK `ACTIVA` / `REACTIVADA`, DEFAULT `ACTIVA` |
| `reactivated_at`, `reactivation_reason`, `reactivation_evidence` | | sí | CHECK `ck_use_reactivacion`: ACTIVA sin fecha ni evidencia; REACTIVADA con las dos |

- Unicidad por evento original: `ux_use_evento (user_id, exercise_id, source_planned_workout_id)`.
- Índice parcial `ix_use_user_activas (user_id) WHERE status = 'ACTIVA'`, para la consulta de cada `prepare()`.

### `safe_regeneration_attempts` (P-03)

| Columna | Restricción |
|---|---|
| `regeneration_id` | PK |
| `user_id` | FK → `users`; **índice `ix_sra_user`** (nuevo, para la API por usuario) |
| `weekly_metric_id` | FK → `weekly_user_metrics`; **UNIQUE**: una regeneración por medición, que es la llave de idempotencia |
| `measured_plan_id` | **FK → `weekly_training_plans`** (nueva) |
| `adaptation_attempt_id` | FK → `plan_adaptation_attempts` (el V32 NO_FACTIBLE) |
| `resulting_plan_id` | FK → `weekly_training_plans` |
| `source` | CHECK `RULES` |
| `outcome` | CHECK `PUBLICADO` / `NO_FACTIBLE`; CHECK `ck_sra_plan`: PUBLICADO ⇔ hay plan |

**Comportamiento ante borrados:** las claves foráneas son RESTRICT por omisión. FitSense no borra planes, sesiones ni ejercicios (se reemplazan o desactivan), y `workout_sessions` ya referencia planes y sesiones igual. Un borrado manual de historial quedaría bloqueado, que es lo deseado.

## 2. Seguridad y concurrencia

| Riesgo | Tratamiento | Evidencia |
|---|---|---|
| Una exclusión de otro usuario | Todas las consultas filtran por `user_id`; la API solo opera sobre `/users/me` | `PoliticasP123IT.reactivacion` (404 con el token de otro usuario) |
| Otro usuario la reactiva | Imposible: se busca por el usuario autenticado | Ídem |
| Doble reporte del mismo evento | `INSERT … ON CONFLICT (user_id, exercise_id, source_planned_workout_id) DO NOTHING`. Antes era «existe y luego inserta», y una carrera omisión/ciclo podía violar la unicidad y abortar el ciclo | `MigracionV43IT` (segundo insert = 0 filas), `PoliticasP123IT.dolorNuevoTrasReactivar` |
| Dos reactivaciones simultáneas | `SELECT … FOR UPDATE` (`bloquearActivas`): la segunda espera y ya no ve nada activo, así que da 404 | `PoliticasP123IT.reactivacionesSimultaneas` (una OK y otra 404; una sola fila REACTIVADA) |
| Reactivación y nuevo dolor | Un dolor posterior es un evento nuevo (otra fila), queda ACTIVA y prevalece. La reactivación anterior sigue constando | `dolorNuevoTrasReactivar` |
| Releer una semana ya procesada | El evento original no se reinserta: no resucita una exclusión reactivada | `PoliticasP123IT.reactivacion` |
| Operación repetida | Registro idempotente; la regeneración es única por medición | `sinSustitutoRegeneraYEsIdempotente` |
| Fallo a mitad | El plan regenerado y su traza van en una transacción; el registro del dolor, en la suya propia (`REQUIRES_NEW` o la de la omisión) | `interrupcionYReintento` |
| Una exclusión desaparece por una regeneración | Imposible: la regeneración solo lee exclusiones, nunca las borra ni las cambia | `regeneracionInviable` y la auditoría del nivel 2 (exclusión activa tras NO_FACTIBLE) |

## 3. Compatibilidad

| Caso | Prueba | Resultado |
|---|---|---|
| PostgreSQL vacío, secuencia Flyway completa | Todas las IT (`@ServiceConnection` con Flyway) | ✔ |
| Esquema hasta V42 más datos históricos representativos y después V43 | `MigracionV43IT` | ✔ Solo se aplica V43. Las columnas, las restricciones y el número de filas de **todas** las tablas anteriores no cambian. Los perfiles congelados de 15 y 180 min se conservan. Las tablas nuevas quedan vacías |
| Reinicio con V43 ya aplicada | `MigracionV43IT` | ✔ 0 migraciones pendientes; `validate()` pasa |
| Catálogos fijados (V36–V41) más V42 y V43 | `EsquemaVigenteSobreCatalogoAnterior` (IT de referencia) | ✔ |
| Restricciones activas sobre datos reales | `MigracionV43IT` | ✔ La FK al ejercicio rechaza uno inexistente; `ck_use_reactivacion` rechaza reactivar sin evidencia; `ck_sra_plan` rechaza PUBLICADO sin plan |

## 4. Prohibiciones respetadas

- V43 no se ejecutó en Neon ni en Railway.
- No se tocaron datos reales ni se limpiaron bases compartidas.
- No hubo despliegue ni migraciones automáticas contra bases remotas.
- Todas las pruebas usaron PostgreSQL efímero local (Testcontainers).

## 5. Riesgos pendientes

| Riesgo | Valoración |
|---|---|
| Al desplegar, V43 se aplicará con Flyway en el arranque | Solo crea tablas: no hay bloqueos largos sobre tablas existentes; las FK nuevas apuntan desde tablas vacías. **Decidir cuándo desplegar es del usuario** |
| `ck_profile_session_minutes` sigue en 15..180 | Intencionado: P-01 (30–120) es de dominio y así se pueden leer los perfiles históricos. Llevar la restricción a la base exigiría decidir antes qué hacer con esos perfiles |
| Volumen de `user_safety_exclusions` | Pocas filas por usuario (una por ejercicio y evento con dolor); índice parcial en las activas |
