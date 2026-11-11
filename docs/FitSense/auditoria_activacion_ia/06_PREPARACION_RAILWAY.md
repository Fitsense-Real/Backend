# 06 · Preparación para Railway (solo verificación)

**Qué se hizo y qué no:**
- No se desplegó nada ni se modificaron variables de Railway.
- No se ejecutaron migraciones contra ninguna base de datos remota, ni se imprimieron o guardaron credenciales.
- Todo lo que sigue sale del código de la rama `generation` (con la PR #45) y de pruebas locales.
- **No se han verificado las variables reales de ningún entorno desplegado.**

## 1. Precedencia de configuración en Spring Boot

- Cada propiedad de `application.properties` con la forma `${VAR:defecto}` toma la variable de entorno `VAR` si existe, y el valor por defecto si no.
- Además, Spring da a las variables de entorno con forma relajada (`SPRING_DATASOURCE_URL` → `spring.datasource.url`) **más prioridad** que el propio `application.properties`, aunque la propiedad esté escrita como literal.
- Railway inyecta las variables del servicio como variables de entorno del proceso.
- **Perfil:** `application.properties` no fija `spring.profiles.active`. El perfil `local-dev` solo se activa a mano (`SPRING_PROFILES_ACTIVE=local-dev`) y **no debe usarse en Railway**: trae un secreto JWT de desarrollo y habilita el ciclo manual.

## 2. Variables

| Variable | Propiedad | Por defecto | Para un entorno de prueba con IA real |
|---|---|---|---|
| `DB_PASSWORD` | `spring.datasource.password` | **sin valor** (obligatoria) | Contraseña de la base de **prueba** |
| `SPRING_DATASOURCE_URL` | `spring.datasource.url` | **literal: el host Neon del proyecto** | **Obligatoria en un entorno de prueba** (ver §3) |
| `SPRING_DATASOURCE_USERNAME` | `spring.datasource.username` | literal `neondb_owner` | Usuario de la base de prueba |
| `JWT_SECRET` | `authorization.jwt.secret` | **sin valor** (obligatoria) | Secreto propio del entorno, de al menos 32 bytes |
| `REPLICATE_API_TOKEN` | `fitsense.ai.api-token` | vacío | Token de una cuenta de Replicate **con límite de gasto** (§5) |
| `FITSENSE_AI_ENABLED` | `fitsense.ai.enabled` | `true` | `true`. **Interruptor de emergencia:** `false` apaga la IA sin redesplegar código |
| `FITSENSE_AI_MODEL` | `fitsense.ai.model` | `gpt-5-mini` | Sin cambios (el evaluado) |
| `FITSENSE_AI_REASONING_EFFORT` | `fitsense.ai.reasoning-effort` | `medium` | Sin cambios |
| `FITSENSE_AI_PREFER_WAIT_SECONDS` | `fitsense.ai.prefer-wait-seconds` | `60` | Sin cambios (el arnés usó 25; es transporte) |
| `FITSENSE_AI_ADAPTATION_PROMPT_VERSION` | adaptación | `v2.1` | Sin cambios |
| `FITSENSE_PLANNING_POLICY` / `..._ALLOW_CANDIDATES` | política | `PLAN-REV-2.0` / `true` | Sin cambios |
| `FITSENSE_PROGRESSION_ENABLED` | progresión | `true` | Sin cambios |
| `FITSENSE_DIFFICULTY_GUARD` | límite de dificultad | `preferencia` | Sin cambios |
| `FITSENSE_PAIN_LOCALIZED_REMOVAL` | retirada localizada | `on` | Sin cambios |
| `FITSENSE_V8C_PATTERN_RULE` | V8C | `multiconjunto` | Sin cambios |
| `FITSENSE_COVERAGE_ENFORCEMENT` / `FITSENSE_DOSE_ENFORCEMENT` / `FITSENSE_CAPABILITY_ENFORCEMENT` | V23 / V24 / V25 | `ENFORCE_AI_ONLY` / `ENFORCE` / `ENFORCE` | Sin cambios |
| `FITSENSE_CORS_ORIGINS` | CORS | vacío | Origen de la app de prueba |
| `FITSENSE_MANUAL_CYCLE_ENABLED` | ciclo manual | `false` | `false` |
| `FITSENSE_API_DOCS_ENABLED` | Swagger | `false` | `false` |
| `FITSENSE_PASSWORD_RESET_DELIVERY` | recuperación de contraseña | `none` | Según el entorno |

Esta configuración la comprueban `ConfiguracionEvaluadaTest` y `ConfiguracionEvaluadaIT` (PR #45). El comportamiento sin token lo comprueban `SinTokenReplicateTest` (esta auditoría) y `CorreccionesReplicateApagadoIT`.

## 3. Riesgo principal: la URL de la base de datos está fijada en el código

- `spring.datasource.url` y `spring.datasource.username` son **literales** en `application.properties`, y apuntan al proyecto Neon del repositorio.
- `spring.flyway.enabled=true`: al arrancar, Flyway migra la base a la que se conecte.
- **Consecuencia:** un servicio de prueba que solo defina `DB_PASSWORD` y comparta la contraseña de esa base **se conectará a ella y la migrará**, y los schedulers escribirán ciclos en ella (§4).
- **Condición antes de desplegar:** definir `SPRING_DATASOURCE_URL` y `SPRING_DATASOURCE_USERNAME` hacia una base de prueba separada, o una rama de Neon dedicada. Después, comprobar en el primer arranque, en el registro de Flyway, que el host es el esperado.
- **No se modifica el código:** cambiar la URL por defecto es una decisión operativa, fuera de esta auditoría.

## 4. Migraciones y tareas programadas

- **Flyway:** la última migración es `V46__catalogo_traducciones_objetivas.sql`. PLAN-REV-2.0 exige `V44_3__planning_policy_rediseno.sql`, que amplía la restricción de `weekly_training_plans`.
  - En una base nueva, Flyway aplica V1–V46 al arrancar.
  - En una base existente, aplica las que falten; sin V44_3, los planes PLAN-REV-2.0 violarían la restricción.
  - Las pruebas de integración aplican todas las migraciones sobre PostgreSQL en contenedores.
- **Schedulers activos sin propiedad que los apague:**

| Scheduler | Cron (UTC del proceso) | Efecto con la IA activa |
|---|---|---|
| `WeeklyCycleScheduler` | `0 0 * * * *` (cada hora) | Cierra semanas, adapta y progresa. **Puede llamar a Replicate** en las adaptaciones con señal |
| `OverdueWorkoutScheduler` | `0 30 * * * *` | Marca sesiones vencidas. Sin IA |
| `StaleSessionScheduler` | `0 35 * * * *` | Cierra sesiones abandonadas. Sin IA |

## 5. Coste y controles

- **Dentro de la aplicación no hay un tope de gasto en dólares.** Lo que limita el gasto:
  - **por petición:** como máximo 3 intentos de generación y 3 de adaptación por sesión señalada; después, el respaldo determinista;
  - **circuit breaker:** tras 3 fallos seguidos, la IA queda apagada 10 minutos;
  - **solo la generación inicial y las adaptaciones con señal llaman a la IA.** La progresión, la recuperación y la reincorporación no la llaman.
- **Coste de referencia (GRA):** unos USD 0,023 por llamada de generación y unos USD 0,013 por llamada de adaptación. H-G9 añade unos USD 0,07 por generación inicial de esos perfiles (`03`).
- **Controles que hay que poner fuera de la aplicación (condiciones):**
  1. un **límite de gasto y alertas** en la cuenta de Replicate del token;
  2. un token **exclusivo del entorno de prueba**, para poder revocarlo sin tocar otros;
  3. `FITSENSE_AI_ENABLED=false` como interruptor de emergencia;
  4. un número de usuarios de prueba acotado.
- **Sin token:**
  - `ReplicateProperties.isUsable()` es falso, y la IA no se llama nunca;
  - la generación usa el motor de reglas, y las adaptaciones el respaldo determinista;
  - la aplicación arranca y funciona;
  - comprobado con `SinTokenReplicateTest` y con toda la regresión de integración, que corre sin token.

## 6. Rama de despliegue

- Según `docs/FitSense/CATALOGO_V37.md`, Railway no despliega desde `generation`.
- Para un entorno de prueba hay que crear un servicio que apunte a `generation`, o a una rama de entrega que la contenga, **después de fusionar la PR de la corrección O12**.
- No se ha verificado la configuración de ningún servicio existente.

## 7. Lista de comprobación antes de desplegar (no ejecutada)

| # | Condición | Estado |
|---|---|---|
| 1 | Fusionar la PR de la corrección O12 (la IA no sube dosis a través de un sustituto) | Pendiente de revisión |
| 2 | Base de prueba separada: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` y `DB_PASSWORD` | Pendiente de configuración |
| 3 | `JWT_SECRET` propio | Pendiente de configuración |
| 4 | `REPLICATE_API_TOKEN` exclusivo, con límite de gasto y alertas en Replicate | Pendiente de configuración |
| 5 | Sin `SPRING_PROFILES_ACTIVE=local-dev` | Pendiente de configuración |
| 6 | Revisar en el primer arranque el host al que se conecta Flyway y que la versión 46 queda aplicada; en el primer plan generado, comprobar la política (`weekly_training_plans`) PLAN-REV-2.0 | Pendiente |
| 7 | Usuarios de prueba controlados, no usuarios reales | Pendiente |
| 8 | Plan de reversión: `FITSENSE_AI_ENABLED=false`, o los interruptores de §2 | Documentado |
