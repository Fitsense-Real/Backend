# Implementación de las políticas P-01, P-02 y P-03

**Fecha:** 31/10/2026.
**Rama:** `politicas-p123`, creada desde `generation` en `7af6ad8` (PR #15), con CI verde en ese commit.
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
- V8C, V1–V25 y 4D v3.2/v3.3.
- ADAPT-2.
- Los límites de progresión y las reglas de reincorporación.
- La fórmula de adherencia.
- Los máximos de sesiones por nivel.
- La inmutabilidad del perfil.
- P-04: no hay ningún mecanismo para cambiar los minutos ni M.

## 1. P-01: duración mínima por objetivo

| Objetivo | Mínimo | Máximo |
|---|---:|---:|
| GAIN_MUSCLE e INCREASE_STRENGTH | 15 | 180 |
| LOSE_WEIGHT y GENERAL_FITNESS | 30 | 180 |

**Dónde se aplica:** en el constructor y en `update` de `UserProfile`, antes del rango general.

**Qué pasa fuera de rango:** se rechaza con HTTP 422, el contrato de error de siempre (`DomainRuleViolationException`). El mensaje nombra el objetivo y el rango, por ejemplo: *«Para el objetivo GENERAL_FITNESS la duración por sesión debe estar entre 30 y 180 minutos; indicaste 15.»*

El valor declarado nunca se corrige.

**Perfiles históricos:** los congelados antes de P-01 no se modifican. `prepare()` lanza `ProfileOutsideGoalDurationException` (código `PERFIL_FUERA_DE_P01`, HTTP 422) antes de generar, así que tampoco llega nada a Replicate. En el ciclo semanal, esa semana queda sin plan y el motivo consta en el log.

**Pruebas:**
- `PoliticaP01DuracionMinimaTest` (27 casos):
  - 14/15/16 y 29/30/31 según el objetivo;
  - 180/181 para los cuatro objetivos;
  - la edición previa al primer plan;
  - que la tabla de planificación coincide con la del perfil.
- `PoliticasP123IT.perfilHistoricoFueraDeP01`: no genera, no llama a la IA y no modifica el perfil.
- `PoliticasP123IT.cuatroObjetivosEnSuMinimo`.

**Hallazgo:** el mínimo es un límite de admisión, no una garantía de plan.
- INCREASE_STRENGTH intermedio de 15 min en gimnasio **no tiene plan**: V4 rechaza sesiones de 18 min con un techo de 17.
- Es la misma situación de U07 en la campaña anterior, ahora en un objetivo cuyo mínimo aprobado es 15.
- Se registra como pendiente de decisión y no se relaja V4.

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
   - con una comprobación explícita de que nada excluido aparece.

   El resultado es API `REGENERACION_SEGURA_REGLAS`, con el plan regenerado.
6. Si tampoco hay semana válida, `safe_regeneration_attempts` registra NO_FACTIBLE con el motivo y no se publica nada. La API muestra `NO_FACTIBLE_POR_SEGURIDAD` y el motivo `P-03: sin alternativa segura válida: …`.

### Restricciones respetadas

- No se modifica el perfil ni M.
- Mismas sesiones y mismos días.
- No sube la dosis:
  - el nivel 1 recalcula la dosis del sustituto, y V8C rechaza que suba el volumen ante dolor;
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

- Solo crea dos tablas y un índice. No modifica datos existentes.
- Se probó:
  - desde una base vacía (todas las IT);
  - sobre esquemas con historial previo, porque las IT que fijan el catálogo en V36–V41 aplican después V42 y V43 con `EsquemaVigenteSobreCatalogoAnterior`.
- **No** se aplicó a Neon ni a Railway.
- Por ser una migración, el PR **no se fusiona automáticamente**: queda para revisión.

## 5. Compatibilidad del contrato

- `AdaptationOutcome.resultado` admite dos valores nuevos: `REGENERACION_SEGURA_SUSTITUCION` y `REGENERACION_SEGURA_REGLAS`.
- Los demás campos no cambian. Un cliente que no conozca los valores nuevos los verá como un texto más.
- Los endpoints de P-02 son nuevos.
- El error de P-01 usa el formato de error de siempre (422).
