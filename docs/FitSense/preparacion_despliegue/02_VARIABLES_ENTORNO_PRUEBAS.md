# 02 · Variables para un entorno de pruebas en Railway

**Alcance:**
- Lista técnica para un despliegue **posterior**, todavía no realizado.
- Los nombres están comprobados en `src/main/resources/application.properties` y en las anotaciones `@Value` de `src/main/java`.
- **No contiene valores secretos.** Los secretos se definen solo en el panel de Railway, nunca en Markdown, registros ni commits.

## 1. Obligatorias

| Variable | Propiedad | Valor por defecto en el código | Qué poner en pruebas |
|---|---|---|---|
| `SPRING_DATASOURCE_URL` | `spring.datasource.url` | **Literal Neon** (ver `01`) | `jdbc:postgresql://<host-de-prueba>:<puerto>/<base>?sslmode=require`, de una base **separada** o una rama de Neon dedicada a pruebas |
| `SPRING_DATASOURCE_USERNAME` | `spring.datasource.username` | Literal `neondb_owner` | Usuario de la base de prueba |
| `DB_PASSWORD` | `spring.datasource.password` | **Ninguno**: sin ella no arranca | Contraseña de la base de prueba |
| `JWT_SECRET` | `authorization.jwt.secret` | **Ninguno**: sin ella no arranca | Secreto propio del entorno, al menos 32 bytes aleatorios, distinto de cualquier otro entorno |

Hoy solo `DB_PASSWORD` y `JWT_SECRET` son obligatorias **para el código**: son las únicas sin valor por defecto. `SPRING_DATASOURCE_URL` y `SPRING_DATASOURCE_USERNAME` son obligatorias **por seguridad operativa**: sin ellas se usa Neon.

## 2. IA (Replicate)

| Variable | Propiedad | Por defecto | En pruebas |
|---|---|---|---|
| `REPLICATE_API_TOKEN` | `fitsense.ai.api-token` | Vacío: la IA no se usa | Token **exclusivo** del entorno de pruebas, de una cuenta con límite de gasto (`03`). Sin él: motor de reglas y respaldo determinista |
| `FITSENSE_AI_ENABLED` | `fitsense.ai.enabled` | `true` | `true` para probar la IA; `false` como interruptor de emergencia |
| `FITSENSE_AI_MODEL` | `fitsense.ai.model` | `gpt-5-mini` | Sin cambios (el evaluado) |
| `FITSENSE_AI_REASONING_EFFORT` | `fitsense.ai.reasoning-effort` | `medium` | Sin cambios |
| `FITSENSE_AI_PREFER_WAIT_SECONDS` | `fitsense.ai.prefer-wait-seconds` | `60` (se limita a 1–60) | Sin cambios |
| `FITSENSE_AI_ADAPTATION_PROMPT_VERSION` | `fitsense.ai.adaptation.prompt-version` | `v2.1` (= FIX-PR-2.1-EXP) | Sin cambios |

- El prompt de generación (PR-1.7) no tiene variable: es `PlanPromptBuilder.VERSION`.
- `fitsense.ai.timeout-seconds` (150) y `fitsense.ai.base-url` no tienen variable propia. Se podrían cambiar con el nombre relajado (`FITSENSE_AI_TIMEOUT_SECONDS`), pero no hace falta.

## 3. Política y adaptación

| Variable | Propiedad | Por defecto | En pruebas |
|---|---|---|---|
| `FITSENSE_PLANNING_POLICY` | `fitsense.planning.policy` | `PLAN-REV-2.0` | Sin cambios |
| `FITSENSE_PLANNING_POLICY_ALLOW_CANDIDATES` | `fitsense.planning.policy.allow-candidates` | `true` | Sin cambios: la autorización para usar la política candidata REV2 |
| `FITSENSE_PROGRESSION_ENABLED` | `fitsense.adaptation.progression.enabled` | `true` | Sin cambios |
| `FITSENSE_DIFFICULTY_GUARD` | `fitsense.adaptation.difficulty-guard` | `preferencia` | Sin cambios |
| `FITSENSE_PAIN_LOCALIZED_REMOVAL` | `fitsense.adaptation.pain-localized-removal` | `on` | Sin cambios |
| `FITSENSE_V8C_PATTERN_RULE` | `fitsense.adaptation.v8c.pattern-rule` | `multiconjunto` | Sin cambios |
| `FITSENSE_COVERAGE_ENFORCEMENT` | `fitsense.coverage.enforcement` (V23) | `ENFORCE_AI_ONLY` | Sin cambios |
| `FITSENSE_DOSE_ENFORCEMENT` | `fitsense.dose.enforcement` (V24) | `ENFORCE` | Sin cambios |
| `FITSENSE_CAPABILITY_ENFORCEMENT` | `fitsense.capability.enforcement` (V25) | `ENFORCE` | Sin cambios |
| `FITSENSE_COVERAGE_PREGENERATION` | `fitsense.coverage.pregeneration` | `OBSERVE` | Sin cambios |
| `FITSENSE_ADAPTATION_CAPABILITY_VALIDATION` | `fitsense.adaptation.capability-validation.enabled` | `false` | Sin cambios: la progresión ya activa la V24 por capacidades |

**Nota:** los valores por defecto de las anotaciones `@Value` del código son los históricos (por ejemplo, `PLAN-HIST-1`, `v1`, progresión `false`). Solo se usarían si faltara `application.properties`, que va dentro del JAR. Lo que manda son los valores de esta tabla. Los comprueban `ConfiguracionEvaluadaTest` y `ConfiguracionEvaluadaIT`.

## 4. Operación

| Variable | Propiedad | Por defecto | En pruebas |
|---|---|---|---|
| `FITSENSE_CORS_ORIGINS` | `fitsense.cors.allowed-origins` | Vacío: sin orígenes web | El origen de la app de prueba, si es web |
| `FITSENSE_MANUAL_CYCLE_ENABLED` | `fitsense.cycle.manual-run.enabled` | `false` | `false` |
| `FITSENSE_API_DOCS_ENABLED` | `fitsense.api-docs.enabled` y Swagger | `false` | `false` |
| `FITSENSE_PASSWORD_RESET_DELIVERY` | `fitsense.password-reset.delivery` | `none` | `none` mientras no haya proveedor de correo |
| `FITSENSE_PASSWORD_RESET_TTL_MINUTES`, `FITSENSE_PASSWORD_RESET_MAX_PER_HOUR` | recuperación de contraseña | `30`, `3` | Sin cambios |
| `fitsense.cycle.lock.url` (`FITSENSE_CYCLE_LOCK_URL`) | URL del bloqueo del ciclo | Vacío: la del pool sin `-pooler` | Vacío, salvo que la base de prueba use un *pooler* en modo transacción |

## 5. Que **no** debe definirse

- `SPRING_PROFILES_ACTIVE=local` o `local-dev`. Esos perfiles traen un secreto JWT de desarrollo, exigen una base local y la IA apagada.
- Variables copiadas del servicio de producción, en especial `DB_PASSWORD` sin `SPRING_DATASOURCE_URL` propia (`01` §1.4).
- `SPRING_FLYWAY_ENABLED=false` en una base nueva: quedaría sin esquema.
