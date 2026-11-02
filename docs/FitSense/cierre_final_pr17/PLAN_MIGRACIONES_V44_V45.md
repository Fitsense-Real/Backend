# Plan de migraciones V44 y V45

**Estado:**
- **No se ha ejecutado en ningún entorno remoto.**
- No hubo conexión a Neon ni a Railway.
- Las dos migraciones solo se han aplicado en PostgreSQL 16 efímero (Testcontainers), en `MigracionV44IT`, `MigracionV45IT` y en todas las pruebas de integración, que migran desde una base vacía.

## 1. Revisión del esquema

| Migración | Cambios | Datos |
|---|---|---|
| **V44** `planning_policy_version` | `ALTER TABLE weekly_training_plans ADD COLUMN planning_policy VARCHAR(32) NULL, ADD COLUMN planning_policy_trace JSONB NULL`, más la restricción `ck_weekly_training_plans_planning_policy` (lista cerrada de 4 versiones) y los comentarios | Ninguno. Los planes existentes quedan con `NULL`, que se lee como `PLAN-HIST-1` |
| **V45** `planning_policy_elegibilidad_funcional` | `DROP CONSTRAINT` y `ADD CONSTRAINT` con la misma lista más `PLAN-EF-0.1`, `PLAN-C2D-EF-0.1` y `PLAN-C2D-V4-EF-0.1` | Ninguno |

**Comprobado en `MigracionV44IT` y `MigracionV45IT`:**
- columnas, restricciones y filas anteriores sin cambios;
- versiones conocidas aceptadas y desconocidas rechazadas, `PLAN-C2-0.1` incluida;
- un reinicio no vuelve a aplicar nada;
- `validate()` limpio.

## 2. Compatibilidad histórica

| Escenario | Resultado |
|---|---|
| Planes anteriores a V44 | `planning_policy = NULL` → `PlanningPolicy.deRegistro(null) = PLAN-HIST-1`. Se revalidan con sus reglas originales |
| Código nuevo con la configuración por defecto | Guarda `PLAN-HIST-1` y su traza; genera y valida igual que antes (960 de 960 perfiles idénticos; campaña REPLAY idéntica) |
| **Vuelta atrás del código** con V44/V45 ya aplicadas | **Segura:** el código anterior no conoce las columnas y Hibernate (`ddl-auto=validate`) solo comprueba las columnas que mapea; las columnas de más no se validan |
| Vuelta atrás del esquema | Flyway Community no tiene migraciones de vuelta. Haría falta SQL manual (§6) |

## 3. Riesgo de Flyway automático en el despliegue

`spring.flyway.enabled=true`: **las migraciones se aplican al arrancar la aplicación**, antes de que JPA valide.

| Riesgo | Detalle | Mitigación |
|---|---|---|
| Bloqueo de `weekly_training_plans` | `ADD COLUMN ... NULL` es instantáneo (solo metadatos), pero toma un bloqueo `ACCESS EXCLUSIVE` breve. `ADD CONSTRAINT CHECK` **recorre la tabla** con el bloqueo puesto. V45 repite el recorrido | En tablas pequeñas son milisegundos. Medir antes las filas (§4). Si fueran muchas, aplicar la restricción con `NOT VALID` y luego `VALIDATE CONSTRAINT`, como migración nueva y nunca editando V44 o V45 |
| Varias réplicas arrancando a la vez | Flyway usa un bloqueo consultivo: una réplica migra y las demás esperan | Desplegar con una réplica, o confirmar el bloqueo |
| Peticiones durante la migración | Una generación que escriba en la tabla espera al bloqueo | Ventana de bajo uso |
| Fallo a mitad | Cada migración va en su propia transacción (PostgreSQL DDL transaccional): o se aplica entera o no se aplica | Revisar `flyway_schema_history` |
| Checksum | Editar V44 o V45 después de aplicarlas rompe el arranque (incidente de V18 y V19) | **No editarlas nunca.** Cualquier cambio va en una V46 |
| Arranque con un código que no tenga las versiones EF | Si se despliega un código anterior a V45 sobre una base con V45, nada falla (la restricción es más permisiva) | — |

## 4. Plan en staging (propuesta; requiere autorización)

1. **Copia de staging** (`pg_dump`) y anotar `SELECT max(installed_rank), max(version) FROM flyway_schema_history` (debe ser 43).
2. **Comprobaciones previas:**
   - `SELECT count(*) FROM weekly_training_plans` (tamaño, para estimar el bloqueo);
   - `SELECT column_name FROM information_schema.columns WHERE table_name='weekly_training_plans' AND column_name LIKE 'planning_policy%'` (debe estar vacío);
   - `SELECT conname FROM pg_constraint WHERE conname='ck_weekly_training_plans_planning_policy'` (vacío);
   - huella de columnas, restricciones y filas como en `MigracionV44IT.huella`.
3. **Desplegar el código** con `fitsense.planning.policy=PLAN-HIST-1` y `allow-candidates=false` (por defecto). Flyway aplica V44 y V45.
4. **Comprobaciones posteriores:**
   - `flyway_schema_history`: V44 y V45 con `success = true`;
   - la huella del paso 2 sin cambios, salvo las dos columnas y la restricción;
   - `SELECT count(*) FROM weekly_training_plans WHERE planning_policy IS NOT NULL` = 0 justo tras migrar;
   - generar un plan de prueba → `planning_policy = 'PLAN-HIST-1'` y traza con `politica`, `parametros` y `sesiones`;
   - intentar `UPDATE ... SET planning_policy='PLAN-X'` en una transacción con `ROLLBACK` → debe fallar;
   - arranque sin errores de validación de JPA.
5. **Observación:** una semana con la política histórica; comparar adherencia y generación con la semana anterior.

**Producción:** el mismo procedimiento, **solo después de staging y con autorización expresa**.

## 5. Lo que NO hace el despliegue de V44/V45

- **No activa** C2-D, V4-C, EF ni LW-PREF-0.2: la política la decide `fitsense.planning.policy`, y las candidatas exigen `allow-candidates=true`.
- No toca planes ya publicados.
- No cambia el catálogo.

## 6. Recuperación ante fallos

| Situación | Acción |
|---|---|
| V44 o V45 falla al migrar | La transacción se revierte sola. La app no arranca: volver a desplegar el código anterior (no necesita las columnas) y analizar el registro |
| La app falla después de migrar | Volver al código anterior: es compatible con las columnas nuevas (§2) |
| Hace falta retirar el esquema (excepcional) | SQL manual, revisado y en una ventana: `ALTER TABLE weekly_training_plans DROP CONSTRAINT IF EXISTS ck_weekly_training_plans_planning_policy; ALTER TABLE weekly_training_plans DROP COLUMN IF EXISTS planning_policy_trace, DROP COLUMN IF EXISTS planning_policy; DELETE FROM flyway_schema_history WHERE version IN ('44','45');`. **Se pierde la traza** de los planes generados mientras tanto |
| Restaurar todo | Restaurar el `pg_dump` del paso 1 |
