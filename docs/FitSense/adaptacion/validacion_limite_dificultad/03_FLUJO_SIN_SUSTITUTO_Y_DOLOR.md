# 03 · Flujo sin sustituto y ante dolor

**Rama:** `experiment/difficulty-guard` (PR #38, borrador). **Límite:** apagado por defecto (`off`).

Este documento trata qué ocurre, de extremo a extremo y hasta el plan publicado, cuando SUST-SEG-1 no encuentra sustituto para un ejercicio que dolió.

Se apoya en el código y en pruebas ejecutadas con PostgreSQL efímero y servicios reales, sin Replicate:

| Prueba | Qué cubre |
|---|---|
| `SinSustitutoDolorLimiteApagadoIT` | Límite apagado (A) |
| `SinSustitutoDolorLimiteEncendidoIT` | Límite encendido (B) |
| `SinSustitutoDolorLimitePreferenciaIT` | Límite con preferencia de dificultad (C) |
| `SinSustitutoDolor24CasosIT` (opt-in) | Los 22 perfiles de los 24 casos, con A, B y C |
| `RespaldoConDolorIT` | Caso C: el 1373 sin sustituto |
| `LimiteDificultadDolorTest` | SUST-SEG-1 real |

## 1. La cadena que ya existe

Esta cadena estaba ya en `generation`; no se ha creado ninguna política nueva.

| Paso | Componente | Sin sustituto |
|---|---|---|
| 0 | **P-02** `PainMemoryService` | El ejercicio con `PAIN_OR_DISCOMFORT` y 0 hecho queda **excluido** (`user_safety_exclusions`, `DOLOR_IDENTIFICADO`, `ACTIVA`). Sale del conjunto elegible de todas las semanas siguientes (`WeeklyTrainingPlanCommandServiceImpl:171`): no lo ve la IA ni el motor de reglas, y V1 rechaza cualquier borrador que lo traiga. |
| 1 | IA (si está activa) + **V8C** | Una propuesta que lo vuelve a prescribir se rechaza: `V8C: ... vuelve a prescribir [id]`. |
| 2 | **RESPALDO / P-03 nivel 1** `AdaptedWeekProducer.respaldo` → `SafetySubstitutionPolicy.reparar` | Si no hay candidato, la sesión **no se sustituye**. El problema se declara (`SEGURIDAD: ... no hay sustituto elegible con la regla SUST-SEG-1 ...`). `revisarYCerrar` devuelve **NO_FACTIBLE** con `SIN_SUSTITUTO_SEGURO` y `SEMANA_NO_VALIDA`. **No se publica nada por esta vía.** |
| 3 | **P-03 nivel 2** `SafeRegenerationService.regenerar` | Regenera la semana con el motor de reglas sobre el conjunto elegible **sin** lo excluido. Antes de publicar comprueba de forma explícita que ningún excluido reaparece (`EXCLUIDO_REAPARECE`) y que la semana no supera el trabajo de la medida (`TRABAJO_SUPERIOR_AL_PERMITIDO`). Si todo cumple, publica (`safe_regeneration_attempts.outcome = PUBLICADO`, plan resultante obligatorio por `ck_sra_plan`). Si no, queda **NO_FACTIBLE** con motivo. |
| 4 | Plan publicado con un excluido (`PublishedPlanSafetyRepairService`) | Si no hay sustituto, el estado es `SIN_SUSTITUTO` y no se crea versión nueva. La sesión insegura queda **bloqueada** para lectura y ejecución (`PublishedPlanSafetyService`). |

La única vía que conserva una sesión sin tocarla es el respaldo **sin** dolor. Con dolor, el respaldo nunca conserva el ejercicio excluido:
- o lo sustituye;
- o declara la sesión no factible.

`InvariantesRespaldo` y `RespaldoConDolorIT` lo comprueban.

## 2. Las 12 preguntas (fase 2.2)

Las respuestas valen para el 135 sin sustituto con el límite encendido (B o C). Están comprobadas en `SinSustitutoDolorLimiteEncendidoIT`, `SinSustitutoDolorLimitePreferenciaIT` y el laboratorio de los 22 perfiles (ver 08).

| # | Pregunta | Respuesta | Evidencia |
|---|---|---|---|
| 1 | ¿Se retira el ejercicio que provocó dolor? | **Sí.** No aparece en ningún plan publicado de la semana nueva ni de la siguiente. | Invariantes 1 y 2 de `SinSustitutoDolorBase.recorrer` |
| 2 | ¿Se conserva ese mismo ejercicio? | **No.** El respaldo no lo conserva: NO_FACTIBLE. La regeneración no lo incluye: está fuera del elegible y hay comprobación explícita. | `flujo_135_ABC.csv`: `{SIN_SUSTITUTO_SEGURO,SEMANA_NO_VALIDA}` y luego `PUBLICADO ... excluidos={135}` |
| 3 | ¿Se conserva la sesión completa? | **No.** La semana se **regenera** entera por reglas (P-03 nivel 2), con dosis base. En 7 de 21 perfiles ni siquiera eso: esa semana queda sin plan (08). | `resultado_api = REGENERACION_SEGURA_REGLAS` |
| 4 | ¿Se intenta otra alternativa de seguridad? | **Sí:**<br>1. IA, si está activa;<br>2. SUST-SEG-1;<br>3. regeneración P-03 nivel 2.<br>No hay más intentos: el ciclo termina. | Mismo CSV; un solo intento V32 y un único intento P-03 |
| 5 | ¿Se devuelve `SIN_EFECTO`? | **No.** El estado V32 es `NO_FACTIBLE`. `SIN_EFECTO` es para propuestas que no cambian nada, y aquí no hay propuesta publicable. | `estado_v32 = NO_FACTIBLE` |
| 6 | ¿Se utiliza `RESPALDO`? | **Se intenta y no basta.** RESPALDO con sustitución solo se publica si SUST-SEG-1 encuentra sustituto. Con A (off) sí lo encuentra: 780, de dificultad 2. | Fila `off` de `flujo_135_ABC.csv`: `RESPALDO / CONSERVED / [780] / 2` |
| 7 | ¿El resultado puede ser `NO_FACTIBLE`? | **Sí.** Es el desenlace de la adaptación localizada. Si además P-03 nivel 2 no pudiera publicar (excluido que reaparece o trabajo superior), la semana quedaría NO_FACTIBLE con motivo. | `NoFactibleReason`, `SafeRegenerationService` |
| 8 | ¿Se publica algún plan nuevo? | **Depende del perfil.** En 14 de los 21 perfiles reproducidos se publica el plan de P-03 nivel 2, sin el 135. En **7 no se publica nada esa semana**: la regeneración supera el trabajo de la semana medida (`TRABAJO_SUPERIOR_AL_PERMITIDO`), la API devuelve `NO_FACTIBLE_POR_SEGURIDAD` y la semana siguiente el ciclo genera un plan nuevo sin el 135. | `plan_semana_nueva = true` y `PUBLICADO plan=…` |
| 9 | ¿Quedan registradas las exclusiones por dolor? | **Sí:** `user_safety_exclusions` en `ACTIVA` / `DOLOR_IDENTIFICADO`, y `excluded_exercise_ids` en `safe_regeneration_attempts`. | Aserción P-02 de la base; columna `excluidos={135}` |
| 10 | ¿Puede el ejercicio excluido reaparecer posteriormente? | **No sin reactivación explícita** del participante: `POST /api/v1/users/me/pain-exclusions/{id}/reactivation`, que pasa el control de seguridad. La semana siguiente, sin dolor, tampoco lo trae. | Invariante 2; `semana_siguiente = NO_CHANGE/SIN_ORDEN/plan` |
| 11 | ¿Cómo se informa a la aplicación móvil? | Por tres vías:<br>• `GET /api/v1/users/me/plan/adaptations`: `resultado = REGENERACION_SEGURA_REGLAS`, con el plan regenerado.<br>• `message_shown` de la intervención: la intención, del tipo «Reportaste molestias. Revisaremos por seguridad…».<br>• El plan activo nuevo en `GET /users/me/plan/current`.<br>Con A el resultado es `REGENERACION_SEGURA_SUSTITUCION`. | `AdaptationOutcomeQueryService`; columna `resultado_api` |
| 12 | ¿Se conserva correctamente el linaje? | **Parcialmente, y es el comportamiento documentado de P-03 nivel 2.** La semana regenerada **no lleva linaje V31** hacia la medida (`source_planned_workout_id` nulo), porque parte de las dosis base. La trazabilidad queda en otras tablas:<br>• `safe_regeneration_attempts`: `attempt_id` V32, plan medido, plan resultante y excluidos;<br>• `plan_adaptation_attempts`.<br>Con A sí hay linaje: copia sustituida. | Comentario de `SafeRegenerationService` (líneas 42 y 120); 14 R-3 |

## 3. ¿Puede el respaldo reintroducir un excluido por dolor?

**No se ha encontrado ningún camino.** Se comprobó en cuatro capas independientes:

1. **Conjunto elegible:** P-02 filtra lo excluido antes de preparar la semana. Ni SUST-SEG-1, ni la IA, ni las reglas lo ven.
2. **V1 (estricta):** rechaza cualquier ejercicio que no esté en `available_exercises`.
3. **V8C:** rechaza «vuelve a prescribir». Lo comprueba `LimiteDificultadDolorTest.casoF` con off, on y preferencia.
4. **P-03 nivel 2:** comprobación explícita `EXCLUIDO_REAPARECE`, además de V1.

Además, en `SafetySubstitutionPolicy.reparar` la sesión sin sustituto **conserva** el ejercicio en la derivada. Pero lo **declara** como problema, y `revisarYCerrar` impide publicarla (NO_FACTIBLE). `LimiteDificultadDolorTest.invarianteNoReintroduce` lo comprueba en cada caso.

Hay una salvedad que va al 14 (R-2):
- La cadena depende de que **P-02 haya registrado el dolor**: un ejercicio con `PAIN_OR_DISCOMFORT`, o una sesión omitida por dolor, que generaliza a todos sus ejercicios.
- Un dolor que el participante no registra no puede excluirse.
- Esto no lo introduce el límite.

**Conclusión:** no hay problema de seguridad que corregir en esta fase, así que **no se prepara corrección**.

## 4. Salidas autorizadas existentes (fase 2.3)

| Operación pedida | ¿Existe? | Dónde |
|---|---|---|
| Retirar el ejercicio afectado | **Sí, como sustitución o regeneración, no como retirada simple.** Ante dolor, V8C acepta retirar sin añadir (`dolorBajaOIgualORetira`). Pero la retirada sin sustituto **no** la hace el respaldo determinista: la hace P-03 nivel 2 regenerando. | V8C rama de dolor; `SafeRegenerationService` |
| Conservar solo el resto de la sesión | **No existe como operación determinista** del respaldo y **no se crea**: la orden lo prohíbe sin demostrar antes que hace falta. **El laboratorio sí lo demuestra para una parte de los casos:**<br>• en 14/21 perfiles, P-03 nivel 2 cubre el caso;<br>• en **7/21**, la semana queda sin plan.<br>La propuesta («retirar solo el ejercicio sin sustituto y conservar el resto, sin añadir trabajo») queda en el 14 (R-3) y el 15 como cambio separado, **pendiente de autorización**. | 08 |
| Buscar otra sustitución compatible | **Sí:** SUST-SEG-1. | `SafetySubstitutionPolicy` |
| Declarar que no hay alternativa segura | **Sí:** `NO_FACTIBLE` / `SIN_SUSTITUTO_SEGURO` y `SIN_SUSTITUTO` en planes publicados. | `AdaptedWeekProducer`; `PublishedPlanSafetyRepairService` |
| Solicitar revisión de la prescripción | **Parcial:** el texto `SEGURIDAD: ... Requiere intervención` queda en V32 y en el resultado de la API. No hay una cola de revisión profesional. | V32 `rejection_reason` |

**No se modifica el respaldo ni se añade ningún mecanismo.** La necesidad de una salida intermedia queda demostrada para 7 de 21 perfiles; decidirla corresponde al responsable (15).

## 5. Diferencia funcional A → B/C en el flujo

| | A (off, hoy) | B / C (límite) |
|---|---|---|
| SUST-SEG-1 para el 135 | 780 (dif 2) o 663 (dif 3) | ninguno |
| V32 | `RESPALDO` / `CONSERVED` | `NO_FACTIBLE` (`SIN_SUSTITUTO_SEGURO`) |
| Publicado | copia de la semana con 135 → 780 (dif 2) o 663 (dif 3), con linaje V31, en 21/21 perfiles | semana regenerada por reglas sin el 135 (sin linaje V31) en 14/21; **ninguno** en 7/21: semana sin plan |
| Dificultad del trabajo que sustituye al 135 | sube | no hay sustituto 1:1; la semana se rehace con dosis base |
| API | `REGENERACION_SEGURA_SUSTITUCION` | `REGENERACION_SEGURA_REGLAS` (14) o `NO_FACTIBLE_POR_SEGURIDAD` (7) |
