# 07 · Operación, trazabilidad y monitoreo (Backend-B5)

- **Rama:** `preproduccion/b5-operacion-trazabilidad`, sobre `generation` (`77b9df3`).
- **Auditoría de referencia:** P1-01, P1-05, P1-07, P1-09 y P2-02.
- **Sin migraciones.** Lo que necesita tablas nuevas queda **propuesto** en §4, para numerarlo después de V66.

## 1. B5.1 · Monitorización (P1-01): **RESUELTO (alertas por log; sin servicio externo)**

| Componente | Qué hace |
|---|---|
| `IndicadoresOperativosService` | Recuentos desde la base: participantes activos; **participantes sin plan vigente** (sin plan `ACTIVE` que cubra hoy en su zona); **cierres pendientes** (pasado el lunes 04:00 local, tuvo plan la semana anterior y no tiene su medición); generaciones fallidas (24 h); adaptaciones `RECHAZADA` o `NO_FACTIBLE` (7 días); operaciones en curso y **huérfanas** (*lease* vencido); estado de los **dos cortacircuitos** (generación y adaptación) |
| `ContadoresOperativos` | Desde el arranque: correo enviado, fallido y descartado; login fallido; límites superados (429); **cierres que terminan sin plan** |
| `GET /api/v1/admin/operations` | ADMIN. Devuelve los indicadores. Solo recuentos: ningún correo, nombre, id de persona ni dato de salud |
| `VigilanteOperativo` | Cada 15 min (`fitsense.operations.alerts.*`) escribe `ALERTA_OPERATIVA tipo=… valor=… umbral=…` en WARN por cada umbral superado. Los avisos de correo y de cierres sin plan son **incrementales**, para no repetirse indefinidamente. También publica medidores Micrometer `fitsense.operaciones.*` |

**Tipos de alerta:**
- `PARTICIPANTES_SIN_PLAN`
- `CIERRE_SEMANAL_PENDIENTE`
- `GENERACIONES_FALLIDAS_24H` (≥ 3)
- `ADAPTACIONES_FALLIDAS_7D` (≥ 5)
- `OPERACIONES_HUERFANAS`
- `CORTACIRCUITOS_GENERACION_ABIERTO`
- `CORTACIRCUITOS_ADAPTACION_ABIERTO`
- `CORREO_FALLIDO`
- `CIERRES_SIN_PLAN`
- `VIGILANTE_FALLO`

**Protección:** no se abre ningún endpoint de métricas. `management.endpoints.web.exposure.include=health` sigue igual. Si en el futuro se expone `/actuator/prometheus`, tiene que ir detrás de autenticación.

**Pendiente de configurar (operación, no código):**
1. Una regla de alerta en Railway, o en la herramienta de logs que se elija, sobre `ALERTA_OPERATIVA`, `CICLO_SEMANAL_PENDIENTE` y `CICLO_SEMANAL_RECUPERADO` (B3), y `OPERACION_IA_*` (V66).
2. Un *uptime* externo sobre `/actuator/health`.

**Pruebas:** `OperacionYExportacionIT.indicadores` (ADMIN 200, USER 403, sin correos) y `alertas` (participante sin plan → `ALERTA_OPERATIVA`, sin sus datos).

## 2. B5.2 · Costos de IA (P1-07): **PARCIAL (límite por usuario resuelto; tope global propuesto)**

| Control | Estado |
|---|---|
| Límite por usuario en la ruta **síncrona** | Ya existía (`LimiteDeGeneraciones`, 5 por hora) |
| Límite por usuario en la ruta **asíncrona** | **Nuevo.** Se consume solo al crear una operación nueva y **dentro de su transacción**: un doble toque que pierde la carrera no gasta cupo (`GeneracionAsincronaResilienciaIT.solicitudesSimultaneas` lo vigila; un primer diseño fuera de la transacción lo rompía y se corrigió) y, sin cupo, no queda ninguna operación. Prueba: `OperacionYExportacionIT.limiteAsincrono` |
| Generaciones duplicadas por concurrencia | Asíncrona: una operación activa por usuario (V58). Síncrona y ciclo: `CicloSemanalBloqueo`. **Asíncrona frente a síncrona:** las cubre la ampliación V59 (#89): las dos rutas tienen libro y la segunda reutiliza la predicción de la misma persona con la misma huella en 24 h |
| Reintentar un POST de creación incierta | **No se reintenta.** Así es desde PR #88: G08, `NO_CONFIRMADA`. #89 lo extiende a la ruta síncrona y al ciclo |
| **Tope global de gasto** | **PROPUESTA, no implementada** (ver abajo) |

**Propuesta de tope global:**
- Un contador diario de llamadas a Replicate, el número que ya mide `REPLICATE_CONSUMO`.
- Al superar `FITSENSE_AI_DAILY_CALL_CAP`, la generación pasa al motor de reglas con el motivo `TOPE_DIARIO` y salta `ALERTA_OPERATIVA tipo=TOPE_IA`.
- En memoria basta con una réplica. Para que sobreviva a reinicios necesita una fila por día, lo que exige una **migración posterior a V66**.
- Valor orientativo, con los datos de `03` de la auditoría: unas 3 llamadas por generación y ~0,02 USD por llamada en *medium*. Para 30 participantes, 12 semanas y ~1,5 llamadas por semana, son ~540 llamadas y ~11 USD. Un tope de 100 llamadas al día deja margen.
- **La cifra la decide el equipo con el presupuesto aprobado.**

## 3. B5.3 · Trazabilidad del estudio (P1-05, P1-09, P2-02)

### 3.1 Qué se necesita para reconstruir las 12 semanas y dónde está

| Dato | Fuente | ¿Reconstruible? |
|---|---|---|
| Plan publicado por semana | `weekly_training_plans`: inmutable, versiones con `parent_plan_id` y `REPLACED` | Sí |
| Origen `AI`, `RULE_ENGINE` o `CONSERVED` | `weekly_training_plans.generation_source`, `generation_attempts` | Sí. **El motivo del respaldo no** (§4, P2-02) |
| Adaptaciones y sus motivos | `plan_adaptation_attempts` (V32), `user_interventions` | Sí |
| Ejecución de sesiones | `workout_sessions`, `workout_session_exercises` | Sí, pero **se editan en el mismo registro**: sin historial de cambios (§4) |
| Métricas semanales | `weekly_user_metrics` (una fila por semana) | Sí; un recálculo **sobrescribe** (§3.2 y §4) |
| Recálculos | `calculation_version`; `calculated_at` es el **primer** cálculo (`ADHERENCIA.md`) | No se ve cuándo se recalculó ni el valor anterior (§4) |
| Abandono y retirada | `weekly_user_metrics.is_dropout`, `users.withdrawn_at`, `user_consents.withdrawn_at` | Sí |
| Datos exportados | `GET /api/v1/admin/exports/weekly-metrics` (§3.3), auditado | Sí |

### 3.2 Implementado sin migración

- **`calculated_at` NO cambia** (revisión de integración del 27/11/2026). Una primera versión de B5 lo renovaba en cada recálculo, pero `ADHERENCIA.md` documenta que es la fecha del **primer** cálculo y los análisis se apoyan en ello. Se conserva esa semántica (`OperacionYExportacionIT.calculadoEnSeConserva`). Registrar cuándo se recalcula necesita una columna propia, `recalculated_at`, con migración posterior a V66 (§4).
- **Detalle de administración solo de participantes.** `GET /admin/participants/{id}` y `/{id}/adherence` dan 404 para una cuenta que no participa. Antes mostraban nombre y correo de cualquier cuenta USER. Prueba: `OperacionYExportacionIT.detalleSoloParticipantes`. `SeguridadRolesIT.datosPersonales` sigue en verde.

### 3.3 Exportación seudonimizada (P1-05): **RESUELTO**

`GET /api/v1/admin/exports/weekly-metrics?from=&to=` (ADMIN, `text/csv`):

- **De solo lectura.** Transacción `readOnly`; no modifica ningún registro operativo.
- **Una fila por participante y semana medida.** Identidad: **solo** `participant_code` y `study_status`. Nunca correo, nombre, id interno, fecha de nacimiento ni notas de salud.
- **Columnas:** las de `weekly_user_metrics` tal como están, el origen y la versión del plan medido, `calculation_version` y `calculated_at`.
- **Solo participantes.** Los retirados salen con `WITHDRAWN`: qué hacer con sus datos es la decisión P-2.
- **CSV seguro.** Escape RFC 4180 y prefijo `'` contra la inyección de fórmulas (`AdminExportControllerTest`).
- **Auditada.** Cada exportación deja `ADMIN_EXPORT adminId=… export=… filas=…` en `fitsense.audit`.

**Prueba:** `OperacionYExportacionIT.exportacion`. Comprueba que no hay correos ni nombres, que el no participante no aparece, que se audita y que USER recibe 403.

## 4. Propuestas que necesitan migración (no implementadas; numerar después de V66)

| Propuesta | Para qué | Esquema propuesto |
|---|---|---|
| Historial de métricas y `recalculated_at` | No perder el valor anterior a un recálculo ni cuándo ocurrió | `weekly_user_metrics_history` (copia de la fila, más `superseded_at` y `calculation_version`). Se rellena con un *trigger* `BEFORE UPDATE`, sin cambiar el código de cálculo |
| Historial de sesiones | Ver qué se editó y cuándo | `workout_session_changes` (`session_id`, `planned_exercise_id`, valor anterior y nuevo, `changed_at`), solo de inserción |
| Motivo del respaldo (P2-02) | Saber por qué un plan es `RULE_ENGINE` (IA apagada, cortacircuitos, proveedor, rechazos, inviable, tope) | Columna `weekly_training_plans.fallback_reason`, o una clave en `input_snapshot` (sin migración, pero requiere pasar el motivo por `GeneratedPlan`, lo que toca muchas pruebas) |
| Rol de base de datos de solo lectura | Analizar sin acceso de escritura | `CREATE ROLE fitsense_analisis` con `GRANT SELECT` sobre las tablas del estudio. Es una operación de base, no de Flyway |
| Auditoría persistente | Que los accesos de ADMIN sobrevivan a la retención de los logs | `admin_audit_events` (solo inserción) |
| Tope diario de IA persistente | §2 | `ai_daily_usage(day, calls)` |

## 5. Pendiente

- Configurar las reglas de alerta y el *uptime* (operación).
- Decidir el tope global y las migraciones de §4.
- Panel web: pantalla de `/admin/operations` y botón de exportación (no es Mobile).
