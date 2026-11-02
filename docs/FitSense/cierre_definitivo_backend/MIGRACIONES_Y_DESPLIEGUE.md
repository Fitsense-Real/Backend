# Migraciones y despliegue (fases 3.3 y 7)

**No se ha desplegado nada ni se ha aplicado ninguna migración en Neon ni en Railway.** Todas las pruebas usan bases aisladas (Testcontainers o PostgreSQL local).

## 1. Migraciones

| Rama | Migraciones | Total |
|---|---|---:|
| `generation` 7c19703 | V1–V44 | 44 |
| + Fitsense-Real/Backend#22 | + **V44_1** (`reason_codes`, `reason_code`) | 45 |
| + Fitsense-Real/Backend#25 | + **V44_2** (`password_reset_tokens`) | 46 |
| PR #17 (candidata) | + **V45** (elegibilidad funcional) | 47 |

| Comprobación | Resultado | Evidencia |
|---|---|---|
| **Orden** | V44 → V44_1 (44.1) → V44_2 (44.2) → V45. La numeración 44.x evita chocar con la V45 del PR #17 | Combinación real de los 5 PR con `generation` y con el PR #17, sin conflictos |
| **Datos existentes** | V44_1 solo añade columnas NULL (las filas anteriores quedan sin código). V44_2 solo crea una tabla. V45 está revisada en el PR #17 | `MigracionV44_1IT` y `MigracionV44_2IT`: sobre un esquema histórico con datos, **ninguna columna, restricción ni fila anterior cambia** |
| **Integridad** | `ck_paa_reason_codes_solo_sin_plan` (solo hay códigos sin plan publicado), `ck_sra_reason_code_solo_no_factible`, `uq_prt_token_hash`, `ck_prt_hash_sha256`, `ck_prt_caducidad`, `ck_prt_uso`, y FK `password_reset_tokens.user_id → users` | Las mismas pruebas; `RecuperacionContrasenaIT` |
| **Índices** | `ix_prt_user_created (user_id, created_at)` para el límite de solicitudes. `reason_codes` no necesita índice (se lee por `attempt_id`) | — |
| **Arranque repetido** | Un segundo `migrate` no aplica nada y `validate` es correcto | `MigracionV43IT`, `MigracionV44IT`, `MigracionV44_1IT`, `MigracionV44_2IT`; arranque real del *jar* local (44 migraciones) |
| **Dependencias** | V44_1 depende de V32 (`plan_adaptation_attempts`) y V43 (`safe_regeneration_attempts`). V44_2 depende de V1 (`users`) | — |
| **Validación de Hibernate** | `ddl-auto=validate` arranca con las 46/47 | Regresión completa en `REGRESION_CIERRE_BACKEND.md` |

**Orden de fusión recomendado:** #22 antes que #25. Si se invierte, no pasa nada mientras **ninguna base compartida** haya aplicado V44_2 antes de V44_1. Flyway rechaza una versión anterior a la última aplicada (`outOfOrder=false`), así que:
- se fusionan los dos;
- después se despliega una sola vez.

## 2. Procedimiento de despliegue en Railway (documentado; **no ejecutado**)

**Antes de nada: confirmar en el panel de Railway la rama real de despliegue.** No consta en el repositorio. Ninguna operación se hace sin esa confirmación ni sin tu autorización expresa.

| # | Paso | Detalle |
|---|---|---|
| 1 | **Rama** | Confirmar en el panel de Railway qué rama se despliega (`generation`, `main` u otra) y que no hay despliegue automático pendiente de commits no aprobados |
| 2 | **Variables y secretos** (en Railway, nunca en el repositorio) | `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (≥ 32 bytes, distinta de la local), `FITSENSE_AI_ENABLED` (decisión), `REPLICATE_API_TOKEN` (solo si se autoriza la IA), `FITSENSE_CORS_ORIGINS`, `FITSENSE_API_DOCS_ENABLED=false`, `FITSENSE_MANUAL_CYCLE_ENABLED=false`, `FITSENSE_PROGRESSION_ENABLED` (decisión), `FITSENSE_PASSWORD_RESET_DELIVERY=none` (hasta tener proveedor), `TZ=UTC`. **No definir** `SPRING_PROFILES_ACTIVE=local` ni `local-dev` |
| 3 | **Base de destino** | Neon (proyecto `ep-dark-brook-…`, base `neondb`): confirmar si es la base del estudio o una de pruebas. Para pruebas, mejor una **rama de Neon** separada |
| 4 | **Migraciones que se aplicarían** | Depende del estado real. Antes, **consulta de solo lectura**: `SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 10;`. Si la última es V43: se aplicarían V44, V44_1 y V44_2 (y V45 solo si se fusiona el PR #17) |
| 5 | **Copia de seguridad** | Inmediatamente antes: rama o instantánea de Neon (restauración a un instante) **y** `pg_dump --format=custom` guardado fuera de Railway. Verificar que la copia se puede restaurar en una base aislada |
| 6 | **Verificaciones posteriores** | Arranque sin errores de Flyway ni de `validate`; `flyway_schema_history` con las versiones esperadas y `success = true`; comprobación de salud (Actuator no existe: ver la decisión); prueba de humo equivalente a `scripts/smoke-local.sh` con un usuario de prueba en la base de pruebas; `FITSENSE_PLANNING_POLICY` y `FITSENSE_PLANNING_POLICY_ALLOW_CANDIDATES` **sin definir** (valores por defecto `PLAN-HIST-1` y `false`); IA según lo decidido |
| 7 | **Recuperación ante fallos** | Flyway no tiene migraciones de retroceso. Si una migración falla, la transacción de PostgreSQL la deshace y la aplicación no arranca: se vuelve a desplegar el *build* anterior. Si falla después: se restaura la rama o instantánea de Neon del paso 5 y se vuelve a desplegar el *build* anterior. Las columnas y la tabla nuevas son aditivas: el *build* anterior funciona con ellas presentes |

## 3. Datos mínimos de prueba (local)

No se necesita un *seed*: las migraciones cargan el catálogo y la configuración. Con `scripts/smoke-local.sh`:
- se crea un usuario de prueba (`@pruebas.invalid`);
- se recorre la primera ruta.

Ver `GUIA_EJECUCION_LOCAL.md`.
