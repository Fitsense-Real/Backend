# Implementación de las políticas P-01, P-02 y P-03

**Fecha:** 31/10/2026.
**Rama:** `politicas-p123`, creada desde `generation` en `7af6ad8` (PR #15), con CI verde en ese commit.
**Actualizada** con la orden de cierre del PR #16:
- P-01 definitiva (30–120 min);
- C-2 (herencia limitada de D8);
- el guardia del nivel 2 de P-03;
- la auditoría de V43 y de la API.
**Entorno:** PostgreSQL efímero (Testcontainers).
- No se tocaron Neon, Railway, Flutter ni `main`.
- No se desplegó nada.
- No se hizo ninguna llamada a Replicate.

## 0. Punto de partida y matriz de impacto

### Línea base

Ejecuté la batería completa (`mvn verify -Pintegration`) sobre `generation` antes de cambiar nada:
- 765 pruebas unitarias, todas en verde;
- 284 pruebas de integración en verde, más 28 opcionales omitidas (campañas con IA real).

### Matriz de impacto

| Pieza | P-01 | P-02 | P-03 | Qué cambia |
|---|:-:|:-:|:-:|---|
| `GoalType` y `UserProfile` (profiling) | ✔ | | | El mínimo por objetivo se valida en el alta y en la edición previa al primer plan |
| `DuracionPorObjetivoPolicy` y `ProfileOutsideGoalDurationException` (planning) | ✔ | | | Los perfiles históricos fuera de rango no generan plan |
| `WeeklyTrainingPlanCommandServiceImpl.prepare` | ✔ | ✔ | | Comprueba P-01 y resta del conjunto elegible lo excluido por dolor |
| `WeeklyTrainingPlanCommandServiceImpl` (omitir) | | ✔ | | Omitir por dolor registra la exclusión |
| **V43** `user_safety_exclusions` y `UserSafetyExclusion` | | ✔ | | Tabla y entidad nuevas |
| `PainMemoryService` | | ✔ | | Registra, consulta y reactiva |
| `SafetyConflictPolicy` | | ✔ | | Nueva causa verificada `dolor_registrado` |
| `SafetyExclusionResolver` | | ✔ | | Lo heredado que dolió entra en la sustitución SUST-SEG-1 |
| `PainExclusionController` | | ✔ | | `GET` y `POST /users/me/pain-exclusions…` |
| `AdaptedWeekProducer.respaldo` | | | ✔ | Nivel 1: sustitución determinista |
| **V43** `safe_regeneration_attempts`, `SafeRegenerationService` y `PlanGenerationPipeline.runDeterministic` | | | ✔ | Nivel 2: regeneración con el motor de reglas |
| `PlanningContextFacade.adaptWeeklyPlan` | | ✔ | ✔ | Registra el dolor antes de preparar; idempotencia de P-03 |
| `AdaptationOutcome` y `AdaptationOutcomeQueryService` | | | ✔ | Nuevos resultados en la API |

### Lo que no se modificó

- Los cuatro objetivos.
- V1–V25 (V20 y V4 incluidos) y 4D v3.2/v3.3.
- V8C, salvo **C-2**: la única modificación, aprobada en el cierre del PR #16. Es una herencia limitada de D8 por identidad (`CIERRE_C2_D8_V8C.md`).
- ADAPT-2.
- Los límites de progresión y las reglas de reincorporación.
- La fórmula de adherencia.
- Los máximos de sesiones por nivel.
- La inmutabilidad del perfil.
- P-04: no hay ningún mecanismo para cambiar los minutos ni M.

> **Actualización (orden de fusión del PR #16).** P-01 queda en **30–75 minutos para los cuatro objetivos**. Fuera de rango, 422 con «La duración por sesión debe estar entre 30 y 75 minutos (objetivo X); indicaste N.». V4 se mantiene: con 75 min, el techo es 86. Los perfiles históricos fuera de 30–75 no se modifican: no generan plan y reciben `PERFIL_FUERA_DE_P01`. Limitación conocida, no oculta: GAIN_MUSCLE a 75 min sigue sin plan posible con V20 y 4D (la sesión más larga posible es de unos 46 min y el piso de 75 min es 52,5). Se estudia en `docs/FitSense/investigacion_reglas_entrenamiento/`.

## 1. P-01: duración por sesión, versión definitiva (30–120 min)

**Actualización del cierre del PR #16:** sustituye a la primera versión de P-01 (15/30 según el objetivo y 180 de máximo).

| Objetivo | Mínimo | Máximo |
|---|---:|---:|
| GAIN_MUSCLE | 30 | 120 |
| INCREASE_STRENGTH | 30 | 120 |
| LOSE_WEIGHT | 30 | 120 |
| GENERAL_FITNESS | 30 | 120 |

No queda ninguna distinción entre objetivos (`GoalType.MIN_SESSION_MINUTES = 30` y `MAX_SESSION_MINUTES = 120`; `DuracionPorObjetivoPolicy` repite la tabla en planificación).

**Dónde se aplica:**
- en el alta del perfil y en la edición permitida antes de congelarlo (`UserProfile`), antes del rango general;
- en la admisión de duración;
- en la verificación de perfiles históricos (`prepare()`).

**Qué pasa fuera de rango:** HTTP 422, el contrato de error de siempre (`DomainRuleViolationException`), con un mensaje único: *«La duración por sesión debe estar entre 30 y 120 minutos (objetivo GENERAL_FITNESS); indicaste 15.»* El valor declarado nunca se corrige.

**Perfiles históricos** congelados fuera de 30–120 (por ejemplo 15, 20, 150 o 180):
- no se modifican ni se sobrescriben, y sus planes anteriores no cambian;
- no se genera un plan nuevo para ellos: `ProfileOutsideGoalDurationException`, código `PERFIL_FUERA_DE_P01`, HTTP 422, antes de generar y sin llamar a la IA;
- en el ciclo semanal, esa semana queda sin plan y el motivo consta en el log;
- no hay ningún mecanismo para editar un perfil congelado.

**La restricción de la base no cambia:** `ck_profile_session_minutes` sigue en `15..180` (V1). Así los perfiles históricos siguen siendo legibles. P-01 es una regla de dominio y no necesita una migración.

**V4 intacta:** 30–120 es la `session_minutes` del perfil, no un máximo de la duración real. El techo sigue siendo `session_minutes × 1,15` con el redondeo vigente (`floor`):

| Perfil | Techo |
|---:|---:|
| 30 | 34 |
| 60 | 69 |
| 120 | 138 |

**D8 intacta:**
- una sesión acortada legítimamente por ACORTAR puede durar menos de 30 min (N03: 15 min con un perfil de 30);
- la generación inicial no hereda D8: ninguna sesión inicial baja del piso de V20.

**Pruebas:**
- `PoliticaP01DuracionMinimaTest` (41 casos):
  - 15, 29, 30, 60, 119, 120, 121 y 180 para los cuatro objetivos;
  - techo V4 con 30, 60 y 120;
  - edición previa (29 y 121 rechazados sin dejar el perfil a medias);
  - las dos tablas coinciden.
- `PoliticasP123IT.cuatroObjetivosEnLosLimites`: cada objetivo en 30 y en 120. Si hay plan, ninguna sesión supera el techo ni baja del piso.
- `PoliticasP123IT.perfilHistoricoFueraDelNuevoRango`: 15, 20, 150 y 180 no generan, no llaman a la IA y no se modifican.
- `C2ReproduccionN03IT`: ACORTAR deja una sesión de menos de 30 min con D8 válida.

## 2. P-02: memoria persistente del dolor

### Persistencia

No había una estructura reutilizable:
- `blocked_exercise_ids` es parte del perfil congelado;
- V32 registra intentos, no exclusiones vigentes.

V43 crea `user_safety_exclusions`, con estas columnas:
- usuario y ejercicio;
- motivo (`DOLOR_IDENTIFICADO` o `DOLOR_SESION_SIN_IDENTIFICAR`);
- el evento original: plan, sesión y ejercicio planificado;
- fecha del evento y vía de registro (`OMISION`, `CICLO_SEMANAL` o `SESION`);
- `recorded_at`;
- estado (`ACTIVA` o `REACTIVADA`);
- `reactivated_at`, `reactivation_reason` y `reactivation_evidence`.

Restricciones:
- **Unicidad por evento** (usuario, ejercicio, sesión de origen): registrar dos veces no duplica, y releer una semana no resucita una exclusión reactivada.
- Un CHECK impide reactivar sin evidencia.

### Registro

- **Al omitir una sesión por dolor**, en la misma transacción: todos los ejercicios de la sesión, porque no se sabe cuál dolió (la regla de V8C).
- **Al empezar cada ciclo**, sobre la semana medida, para lo que llega por ejecución:
  - ejercicio marcado con `PAIN_OR_DISCOMFORT`: solo ese ejercicio;
  - sesión con dolor sin identificar: todos sus ejercicios.

### Aplicación

`prepare()` resta las exclusiones activas del conjunto elegible. Todos los caminos parten de ese conjunto, y V1 rechaza cualquier borrador que traiga algo no elegible:
- generación inicial;
- regeneración;
- adaptación;
- progresión (`cumple` exige que el ejercicio sea elegible);
- reincorporación (`elegibles.get(retirado)` es nulo, así que la reserva queda DESCARTADA).

En la semana derivada, un ejercicio heredado que dolió tiene una causa verificada (`dolor_registrado`). Por eso entra en la sustitución obligatoria SUST-SEG-1, cuya excepción ya está en V8C; V8C no se modificó.

**Lo que nunca levanta una exclusión:**
- el paso de las semanas;
- cumplir entrenamientos;
- que no haya nuevos reportes.

### Reactivación explícita (contrato para Flutter, no implementado en la app)

| | |
|---|---|
| Ver | `GET /api/v1/users/me/pain-exclusions` → `[{exclusionId, exerciseId, exerciseName, reason, eventDate, sourcePlannedWorkoutId, status, recordedAt, reactivatedAt}]` |
| Reactivar | `POST /api/v1/users/me/pain-exclusions/{exerciseId}/reactivation`, cuerpo `{"confirmNoPain": true, "comment": "opcional, ≤ 300"}` |
| 200 | `{exerciseId, exclusionesReactivadas, reactivadaEn, candidatoHoy, nota}` |
| 401 | Sin token |
| 404 | No existe una exclusión activa de ese ejercicio para ESTE usuario (tampoco revela las de otros) |
| 422 | `confirmNoPain` no es true, o `REACTIVACION_RECHAZADA_POR_SEGURIDAD`: el ejercicio choca hoy con una restricción vigente (banderas del catálogo frente a lo declarado, INC-4C-03 o INC-CAT-1374). La confirmación no la levanta |

**Lo que no se pide ni se guarda:** ninguna afirmación médica. Solo la confirmación del usuario y un comentario libre opcional. La evidencia guardada es la acción, el usuario, el ejercicio, la vía y la hora, y que el control de seguridad no encontró conflicto.

**Reactivar no es reincorporar.** El ejercicio vuelve a ser candidato y sigue sujeto a todos los validadores. No se añade automáticamente: `candidatoHoy` y `nota` lo explican.

### Pruebas

| Caso | Prueba |
|---|---|
| Dolor en una sesión, con ejercicio no identificado | `AdaptacionCompletaIT.dolorSinIa`, `PoliticasP123IT.antesDelReinicio` |
| Dolor con ejercicio identificado (solo ese) | `PoliticasP123IT.dolorIdentificadoPersisteCincoSemanas` |
| Semana siguiente y cinco semanas después | La misma, ciclo a ciclo |
| Nueva generación | La misma (`generateWeeklyPlan` a 20 semanas) |
| Adaptación | `dolorSinIa`, `iaInvalidaSustitucionDeterminista` |
| Progresión | `ProgresionIT.p02ProgresionSinElExcluido` |
| Reincorporación | `ProgresionIT.p02ReincorporacionDescartadaPorDolor` |
| Reinicio de la aplicación | `antesDelReinicio` y `despuesDelReinicio` (contexto Spring cerrado con `@DirtiesContext`) |
| Otro usuario no la hereda | `dolorIdentificadoPersisteCincoSemanas` |
| Reactivación sin autorización, de otro usuario y sin confirmar | `PoliticasP123IT.reactivacion` (401, 404 y 422) |
| Reactivación válida, sin reincorporar automáticamente | `PoliticasP123IT.reactivacion` |
| Reactivación rechazada por una restricción incompatible | `PoliticasP123IT.reactivacionRechazadaPorSeguridad` |
| El reactivado sigue sujeto a los validadores | Vuelve al conjunto elegible: V1–V25 y V8C como cualquier candidato |

## 3. P-03: regeneración segura en el mismo ciclo

### Secuencia

1. El usuario reporta dolor.
2. P-02 registra la exclusión.
3. Se evalúa la adaptación permitida (V8C, ADAPT-2) con la IA, si está encendida.
4. **Nivel 1: sustitución determinista.** Si la IA no está disponible o no da una propuesta válida, y todo lo que dolió está excluido por P-02, el respaldo ya no termina en NO_FACTIBLE.
   - Aplica SUST-SEG-1: mismo día, enfoque, minutos y clase de slot; dosis recalculada sin subir.
   - Después pasan V8C y la validación estricta (V1–V25 con 4D).
   - Si es válida, se publica: V32 `RESPALDO`, nota `REGENERACION_SEGURA:` y API `REGENERACION_SEGURA_SUSTITUCION`.
5. **Nivel 2: regeneración con reglas.** Si el nivel 1 tampoco produce una semana válida, la adaptación queda NO_FACTIBLE en V32, como antes, y en el mismo ciclo:
   - se genera la semana con el **motor de reglas solamente**;
   - con el conjunto elegible sin lo que dolió;
   - con el mismo perfil, los mismos días y el mismo número de sesiones (V2);
   - sin M y sin progresiones;
   - con la validación completa;
   - con una comprobación explícita de que nada excluido aparece;
   - **sin más trabajo que la semana medida** (auditoría del cierre del PR #16, `AUDITORIA_P03_NIVEL2.md`): si la semana regenerada sube las repeticiones o los segundos totales planificados, no se publica.

   Este guardia existe porque las dosis base podían deshacer una reducción autorizada por fatiga o un ACORTAR en plena semana de dolor.

   Si pasa, el resultado es API `REGENERACION_SEGURA_REGLAS`, con el plan regenerado.
6. Si tampoco hay semana válida, `safe_regeneration_attempts` registra NO_FACTIBLE con el motivo y no se publica nada. La API muestra `NO_FACTIBLE_POR_SEGURIDAD` y el motivo `P-03: sin alternativa segura válida: …`.

### Restricciones respetadas

- No se modifica el perfil ni M.
- Mismas sesiones y mismos días.
- No sube la dosis:
  - el nivel 1 recalcula la dosis del sustituto, y además `masTrabajoQueLaMedida` impide que una sesión del dolor suba sus repeticiones o segundos totales (C-3 de `ERRORES_Y_CORRECCIONES.md`);
  - el nivel 2 usa las dosis base de 4D.
- Las sesiones ejecutadas no se tocan (lo comprueban las pruebas sobre el historial).
- La semana no se duplica:
  - el nivel 2 no regenera si ya hay un plan activo;
  - `ux_plan_active`;
  - `UNIQUE (weekly_metric_id)`.

### Idempotencia

`safe_regeneration_attempts` tiene una fila por medición.
- **Ciclo repetido:** devuelve el mismo plan.
- **Plan y traza:** se escriben en **una** transacción (`TransactionTemplate`). Si la escritura cae, no queda nada a medias y el reintento publica una sola vez.

### Trazabilidad

| Dato | Dónde |
|---|---|
| Evento de dolor original | `user_safety_exclusions` (sesión y ejercicio planificado) |
| Exclusiones | `user_safety_exclusions` y `safe_regeneration_attempts.excluded_exercise_ids` |
| Resultado de la adaptación y motivo del rechazo | V32 (`status` y `rejection_reason`), y `adaptation_rejection` |
| Intento, origen y resultado de validación | `safe_regeneration_attempts` (`source = RULES`, `outcome` y `validation_problems`) |
| Plan publicado o NO_FACTIBLE | `resulting_plan_id`, o `outcome = NO_FACTIBLE` |
| Linaje | Nivel 1: V31 y V32 completos (sustituciones en el informe). Nivel 2: una generación nueva, sin V31 hacia la semana medida, enlazada por `adaptation_attempt_id` al V32 NO_FACTIBLE |

### Consecuencia conocida

La semana del nivel 2 parte de dosis base. Es lo mismo que antes ocurría una semana después, cuando el ciclo siguiente generaba desde cero.

### Pruebas

| Caso | Prueba |
|---|---|
| Dolor con sustituto elegible | `dolorSinIa` (7 sustituciones, IA apagada) |
| IA apagada | `dolorSinIa` |
| IA con respuesta inválida simulada | `iaInvalidaSustitucionDeterminista` (3 llamadas, ni una más) |
| Sin sustituto, con regeneración determinista válida | `sinSustitutoRegeneraYEsIdempotente` |
| Regeneración inviable | `regeneracionInviable` |
| Doble ejecución del ciclo | `sinSustitutoRegeneraYEsIdempotente` |
| Interrupción y reintento | `interrupcionYReintento` |
| Sesiones ya ejecutadas | `dolorIdentificadoPersisteCincoSemanas` y `sinSustituto…` (historial idéntico) |
| Restricciones múltiples y ejercicio ya excluido por P-02 | `restriccionesMultiplesYDolorAnterior` |
| Sin cambios de frecuencia ni publicación insegura | En todas |

**Cómo se simula «sin sustituto»:** en `sinSustituto…`, `interrupcionYReintento` y `regeneracionInviable`, el resolutor de exclusiones devuelve «ninguna», y en el caso inviable el motor de reglas falla. Es la única forma determinista de forzar ese camino con el catálogo actual. En la campaña se observan los caminos reales.

## 4. Migración V43

- **Qué crea:** dos tablas y dos índices. No modifica datos existentes.
- **Ajustada antes de su primera aplicación** (no se ha aplicado en ningún entorno compartido):
  - claves foráneas al ejercicio, al evento original (plan, sesión y ejercicio planificado) y a la semana medida;
  - índice por usuario en la traza de P-03.
- **Concurrencia:**
  - el registro del dolor usa `INSERT … ON CONFLICT DO NOTHING`;
  - la reactivación bloquea la fila.
- **Detalle:** `AUDITORIA_MIGRACION_V43.md`.
- **Probada:**
  - desde una base vacía (todas las IT);
  - sobre un esquema V42 con datos históricos y con un reinicio posterior (`MigracionV43IT`);
  - sobre catálogos fijados (V36–V41) más V42 y V43 (`EsquemaVigenteSobreCatalogoAnterior`).
- **No** se aplicó a Neon ni a Railway.
- **El PR no se fusiona automáticamente.**

## 5. Compatibilidad del contrato

Detalle y requisitos para Flutter en `REVISION_CONTRATO_API_P123.md`.


- `AdaptationOutcome.resultado` admite dos valores nuevos: `REGENERACION_SEGURA_SUSTITUCION` y `REGENERACION_SEGURA_REGLAS`.
- Los demás campos no cambian. Un cliente que no conozca los valores nuevos los verá como un texto más.
- Los endpoints de P-02 son nuevos.
- El error de P-01 usa el formato de error de siempre (422).
