# Migraciones V58 y V59

Ninguna migración aplicada se ha modificado: V58 es la de #71, intacta, y V59 es nueva. Las correcciones de esta
orden son de código y no necesitaron otra migración.

## V58 (`plan_generation_operations`, #71)

| Elemento | Qué garantiza |
|---|---|
| PK `operation_id` UUID; FK `user_id` → users; FK `plan_id` → weekly_training_plans | Relaciones |
| `ck_pgo_status`, `ck_pgo_kind` | Estados válidos |
| `ck_pgo_completed_has_plan` | `COMPLETED` siempre con plan |
| `ck_pgo_finished` | Terminal ⇔ `finished_at` |
| `ck_pgo_reason` | `reason_code` solo en `FAILED` o `NO_FACTIBLE` |
| `ck_pgo_executions` | `executions ≥ 0` |
| `ux_pgo_una_activa_por_usuario` (parcial, `PENDING`/`PROCESSING`) | Una operación activa por usuario: doble toque y peticiones simultáneas |
| `ix_pgo_pendientes` (parcial, `PENDING`) | El reclamo `ORDER BY created_at … SKIP LOCKED` |
| `ix_pgo_usuario_reciente` | `/latest` |

## V59

| Elemento | Qué garantiza |
|---|---|
| Columnas nuevas: `lease_owner`, `lease_expires_at`, `first_started_at` | Lease con propietario; límite desde la primera ejecución |
| `ck_pgo_lease` (`NOT VALID`) | `PROCESSING` ⇔ hay propietario y vencimiento, en toda escritura nueva |
| `ix_pgo_lease_vencido` | Barrido de huérfanas |
| Tabla nueva `plan_generation_predictions`, con FK a la operación y al usuario y una autorreferencia `reused_from_row_id` | Libro de predicciones |
| `ck_pgp_*` (estado, `ENVIADA`/`SUCCEEDED` con id, `SUCCEEDED` con salida) | No se guardan resultados incompletos como válidos |
| `ux_pgp_intento_vigente (operation_id, attempt) WHERE status <> 'DESCARTADA'` | Un intento vigente por operación. Si dos ejecuciones anotan el mismo intento, la segunda falla (`DuplicateKeyException`) y sigue la de la primera. |
| `ix_pgp_reutilizable (user_id, request_hash, requested_at DESC)` parcial | Adopción entre operaciones del mismo usuario |
| `ix_pgp_operacion` | Recuento por operación |

**Tabla de planes** (V4, sin cambios): `ux_plan_active (user_id, week_start_date) WHERE status = 'ACTIVE'`. Es la
garantía final de que no hay dos planes activos.

## Transacciones

- **Reclamo:** una sentencia (`UPDATE … (SELECT … FOR UPDATE SKIP LOCKED)`).
- **Latido y barrido:** sentencias sueltas e indexadas.
- **Libro:** autocommit. **Ninguna transacción abierta mientras se espera a Replicate.**
- **Publicación:** una transacción corta con `SELECT … FOR UPDATE` de la operación (propiedad), `persist` del plan y
  sus sesiones. Si algo falla, rollback completo (R16).
- **Cierre:** `finish`, en una transacción corta, solo si la operación sigue `PROCESSING` y es del propietario.

## Pruebas

| Comprobación | Cómo | Resultado |
|---|---|---|
| Flyway desde una PostgreSQL **vacía** (V1..V59) | Testcontainers en todas las IT, en el CI y en local; además `GeneracionAsincronaIT` contra una base local vacía | PASS |
| **Actualización desde V58 con datos** | `MigracionV58aV59IT`: Flyway hasta V58, se cargan operaciones `PROCESSING` (sin lease, de un proceso muerto), `PENDING` y `FAILED`, y la aplicación aplica V59 al arrancar | PASS |
| V59 no reescribe filas | La `PROCESSING` heredada sigue sin lease ni `first_started_at` | PASS |
| `ck_pgo_lease` `NOT VALID` y vigente para escrituras nuevas | `pg_constraint.convalidated = false`; `PROCESSING` sin lease se rechaza | PASS |
| **La `PROCESSING` heredada se recupera** | El barrido la devuelve a `PENDING`; después se reclama con lease y `first_started_at` y termina `COMPLETED` con 1 plan | PASS |
| `VALIDATE CONSTRAINT ck_pgo_lease` tras recuperar | Se valida sin error | PASS |
| La base rechaza resultados incompletos | `libroRechazaIncompletos` | PASS |
| Un intento vigente por operación | `libroNoAdmiteDosVigentesDeLaMismaPeticion` | PASS |
| Catálogo fijado con V59 solo de esquema | `EsquemaVigenteSobreCatalogoAnterior` (V59 añadida a `SOLO_ESQUEMA`) | PASS, en la regresión |

### Defecto encontrado y corregido

Antes de esta orden, una `PROCESSING` heredada de V58 tenía `lease_expires_at` nulo, y
`lease_expires_at < now` es falso con un nulo. Se quedaba `PROCESSING` hasta vencer a los 30 min como
`TIEMPO_EXCEDIDO`, sin reencolarse.

- **Corrección:** el barrido trata el lease nulo como vencido.
- **Evidencia:** con la corrección retirada, `MigracionV58aV59IT` falla (`expected "PENDING" but was "PROCESSING"`).
  Con ella, pasa.

## Para un despliegue real (no ejecutado)

V59 es aditiva y no bloquea durante mucho tiempo: un `ALTER TABLE ADD COLUMN` sin valor por omisión, un `CHECK`
`NOT VALID`, una tabla nueva e índices sobre una tabla pequeña.

Tras el primer barrido sin filas heredadas, puede validarse la restricción:
`ALTER TABLE plan_generation_operations VALIDATE CONSTRAINT ck_pgo_lease`. Esto no se ha hecho en ninguna base real.
