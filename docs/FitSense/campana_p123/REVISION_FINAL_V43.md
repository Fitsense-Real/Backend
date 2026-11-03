# Revisión final de V43 (`V43__pain_memory_and_safe_regeneration.sql`)

Revisión directa del SQL real frente a las entidades JPA, las consultas y los servicios. Todo se ejecutó sobre PostgreSQL efímero (Testcontainers). **No se ha aplicado V43 en Neon ni en Railway, ni se ha conectado a ellos.**

## 1. ¿Se aplicó V43 en algún entorno?

- V43 solo existe en la rama `politicas-p123` (PR #16, sin fusionar).
  - `origin/generation` termina en **V42**.
  - `origin/main` termina en **V24**.
- Ningún despliegue hecho desde esas ramas puede haberla aplicado.
- Por eso las modificaciones de V43 durante el PR (las FK y el índice añadidos en la orden de cierre) no alteran una migración ya aplicada.
- **Antes de fusionar no debe cambiar nada más en V43.** Después de fusionar, cualquier cambio irá en una V44.

## 2. SQL frente a entidades

`spring.jpa.hibernate.ddl-auto=validate`: Hibernate valida el esquema contra las entidades en cada arranque. Todas las IT (sobre V1–V43) arrancan, así que la correspondencia está comprobada en ejecución.

### `user_safety_exclusions` ↔ `UserSafetyExclusion`

| Columna | SQL | Entidad | ✔ |
|---|---|---|:-:|
| exclusion_id | BIGINT IDENTITY PK | `Long`, IDENTITY | ✔ |
| user_id | BIGINT NOT NULL FK users | `Long`, nullable=false | ✔ |
| exercise_id | BIGINT NOT NULL FK exercises | `Long`, nullable=false | ✔ |
| reason | VARCHAR(40) CHECK (DOLOR_IDENTIFICADO, DOLOR_SESION_SIN_IDENTIFICAR) | `enum Reason`, length 40, STRING | ✔ (mismos valores) |
| source_plan_id | BIGINT NOT NULL FK weekly_training_plans | `Long`, nullable=false | ✔ |
| source_planned_workout_id | BIGINT NOT NULL FK planned_workouts | `Long`, nullable=false | ✔ |
| source_planned_exercise_id | BIGINT FK planned_workout_exercises | `Long` | ✔ |
| event_date | DATE NOT NULL | `LocalDate` | ✔ |
| recorded_by | VARCHAR(30) CHECK (OMISION, CICLO_SEMANAL, SESION) | `enum RecordedBy`, length 30 | ✔ |
| recorded_at | TIMESTAMPTZ DEFAULT now() | `OffsetDateTime`, insertable/updatable=false | ✔ |
| status | VARCHAR(20) DEFAULT 'ACTIVA' CHECK (ACTIVA, REACTIVADA) | `enum Status`, length 20, por defecto ACTIVA | ✔ |
| reactivated_at / reactivation_reason (300) / reactivation_evidence (500) | | `OffsetDateTime` / length 300 / length 500 | ✔ |

### `safe_regeneration_attempts` ↔ `SafeRegenerationAttempt`

| Columna | SQL | Entidad | ✔ |
|---|---|---|:-:|
| regeneration_id | BIGINT IDENTITY PK | `Long`, IDENTITY | ✔ |
| user_id, weekly_metric_id, measured_plan_id | BIGINT NOT NULL con FK | `Long`, nullable=false | ✔ |
| new_week_start_date | DATE NOT NULL | `LocalDate` | ✔ |
| adaptation_attempt_id | BIGINT FK plan_adaptation_attempts | `Long` | ✔ |
| adaptation_rejection, validation_problems | TEXT | `String` | ✔ |
| excluded_exercise_ids | BIGINT[] NOT NULL | `Long[]`, `@JdbcTypeCode(ARRAY)`, `bigint[]` | ✔ |
| source | VARCHAR(20) DEFAULT 'RULES' CHECK = 'RULES' | `String`, "RULES" | ✔ |
| outcome | VARCHAR(20) CHECK (PUBLICADO, NO_FACTIBLE) | `enum Outcome`, length 20 | ✔ |
| resulting_plan_id | BIGINT FK | `Long` | ✔ |
| created_at | TIMESTAMPTZ DEFAULT now() | `OffsetDateTime`, insertable/updatable=false | ✔ |

### Restricciones y consultas

| Restricción | Consulta o servicio que la usa |
|---|---|
| `ux_use_evento` UNIQUE (user_id, exercise_id, source_planned_workout_id) | `registrarSiNoExiste`: `INSERT … ON CONFLICT (user_id, exercise_id, source_planned_workout_id) DO NOTHING`. Releer la misma semana no duplica ni reactiva |
| `ix_use_user_activas` (parcial, `status = 'ACTIVA'`) | `excluidosActivos`, `bloquearActivas` (`PESSIMISTIC_WRITE`) |
| `ck_use_reactivacion` | `reactivar` siempre escribe la fecha y la evidencia juntas |
| `ux_sra_medicion` UNIQUE (weekly_metric_id) | `yaResuelta` (`findByWeeklyMetricId`); llave de idempotencia de P-03 |
| `ck_sra_plan` (PUBLICADO ⇔ plan) | `regenerar` / `registrarNoFactible` |
| `ix_sra_user` | `adaptations` (GET por usuario) |

### Claves foráneas y borrados

Las FK de V43 son RESTRICT (por omisión). El código no borra nada de lo que referencian:
- no hay `delete…` ni `DELETE` sobre planes, sesiones, ejercicios, métricas, intentos ni usuarios;
- los planes se reemplazan, no se borran;
- el catálogo se desactiva con `is_active` (V37: 776 desactivado), no se borra;
- los agregados con `orphanRemoval` (plan → sesiones → ejercicios) no tienen ningún método que quite elementos.

**Efecto.** Un borrado **manual** en la base de un plan con evidencia de dolor quedaría bloqueado. Es intencional: la evidencia del estudio no puede desaparecer en silencio.

## 3. Concurrencia

| Escenario | Prueba | Resultado |
|---|---|---|
| Dos registros simultáneos del **mismo** dolor | `PoliticasP123IT.dolorSimultaneoMismoEventoYOtroUsuario` (nueva) | Una sola fila (`ON CONFLICT DO NOTHING`) |
| Dos **usuarios distintos** a la vez | ídem | Cada uno la suya y solo la suya |
| Dos reactivaciones simultáneas | `reactivacionesSimultaneas` | Una reactiva y la otra recibe 404; una sola fila REACTIVADA (bloqueo `PESSIMISTIC_WRITE`) |
| Dolor nuevo tras reactivar | `dolorNuevoTrasReactivar` | Vuelve a excluir; registrar dos veces no duplica |
| Fallo a mitad de la transacción y reintento | `interrupcionYReintento` | Nada a medias (plan y traza en la misma transacción); el reintento publica una sola vez |
| Dos regeneraciones simultáneas de la **misma semana** | `regeneracionesSimultaneasMismaSemana` (nueva) | Un plan y una traza PUBLICADO que lo nombra; el cierre posterior devuelve el mismo y no duplica (el plan y su traza se guardan en la misma transacción y `ux_sra_medicion` admite una sola traza por medición: la segunda transacción no puede dejar un plan sin traza) |
| Doble ejecución del ciclo | `sinSustitutoRegeneraYEsIdempotente` | Mismo plan; una traza y un intento |

**Nota de la prueba nueva de regeneraciones simultáneas.** La primera versión usaba un perfil GAIN_MUSCLE cuya regeneración resultó NO_FACTIBLE: P-03 nivel 2 detectó más trabajo que la semana medida (486 → 502 repeticiones). No es un fallo: es la guarda de nivel 2. Se usa el escenario que publica (INCREASE_STRENGTH, el de `interrupcionYReintento`).

## 4. Compatibilidad

| Comprobación | Prueba | Resultado |
|---|---|---|
| Base vacía con todas las migraciones | Las 458 IT | ✔ |
| V42 con datos históricos, después V43 | `MigracionV43IT` | ✔ Solo V43; ni tablas, ni columnas, ni restricciones, ni filas anteriores cambian; los perfiles congelados de 15 y 180 min no se corrigen |
| Reinicio y segunda ejecución de Flyway | `MigracionV43IT` | ✔ 0 migraciones; `validate` pasa |
| Consultas de adaptación, generación y seguridad sobre el esquema nuevo | `PoliticasP123IT`, `ProgresionIT`, `HerenciaD8IT`, `C2ReproduccionN03IT`, campaña | ✔ |

## 5. Veredicto

**V43 está lista para integrarse sin comprometer datos.**
- Solo crea tablas nuevas.
- Su mapeo está validado en cada arranque.
- Su idempotencia y su concurrencia están probadas.
- No se ha aplicado en ningún entorno compartido.

**Condiciones:**
- aplicarla solo con el procedimiento de despliegue aprobado (no en esta orden);
- no modificarla después de fusionar.
