# INC-CAT-1374: «wind sprints» es un ejercicio colgado y el catálogo no lo sabe

**Fecha:** 23/10/2026 · **Origen:** `REVISION_CATALOGO_CARDIO.md` · **Estado:**

- **Contención:** aplicada en el código.
- **Migración:** propuesta, **no desplegada**.
- **Planes existentes:** pendientes de medir.

## 1. El problema

El GIF del 1374 muestra una elevación alterna de rodillas **colgado de una barra de dominadas**. Su registro dice:

- que no exige colgarse ni brazos arriba;
- que no exige barra (su implemento es peso corporal);
- que es de alto impacto;
- y sus instrucciones describen un sprint en pista.

Los filtros de seguridad trabajan con ese registro, así que **no pueden excluirlo** a quien declaró que no puede colgarse o tener los brazos arriba, ni a quien no tiene barra.

En la matriz de 432 perfiles es elegible en 64 combinaciones perfil-objetivo; en 32 de ellas el participante tenía prohibidos los brazos arriba y estaba en casa sin barra. **Son combinaciones simuladas, no participantes reales.** La exposición real se mide con las consultas de la sección 4.

## 2. Contención (aplicada)

`ExternalCatalogService`, en el mismo punto que la retención INC-4C-03: antes de cualquier generador, del prompt y de la adaptación.

**El 1374 solo es elegible si:**

1. el perfil declara `pull-up bar`;
2. no tiene restringido colgarse;
3. no tiene restringidos los brazos arriba.

La exclusión por alto impacto del dato actual se mantiene, porque es más conservadora que la corregida.

**Por qué no un bloqueo total.** Bloquearlo para todos dejaría también sin semana conservada, por V1, a quien sí puede hacerlo. La contención condicional corta toda la exposición indebida sin quitar el ejercicio a quien cumple sus requisitos reales. Si se prefiere el bloqueo total, basta con que `admite1374` devuelva `false`.

- Sin interruptor, como INC-4C-03: es seguridad, no medición.
- Cuando se excluye, se escribe `EJERCICIOS_CONTENIDOS user=… motivo=INC-CAT-1374`.
- La consulta silenciosa de los recuentos de `ESCASEZ` aplica la misma regla.

**Pruebas:**

- `ExternalCatalogServiceContencion1374Test` (9): cada combinación de barra, colgarse y brazos arriba; que solo afecta al 1374; códigos tolerantes; perfil sin equipamiento.
- `CatalogoElegibleIT`: el oráculo de 432 perfiles incluye la regla. Mutación de control: sin la contención, el oráculo falla con «sobran (incompatibles) = [1374]».

## 3. Migración correctiva (propuesta, no desplegada)

`docs/FitSense/migraciones_propuestas/V34__correccion_inc_cat_1374.sql`. Está **fuera** de `db/migration` para que Flyway no la aplique.

**Qué cambia:** deja el 1374 igual que los colgados equivalentes 61 y 62.

| Campo | Valor |
|---|---|
| Patrón | `TRUNK_FLEXION` |
| Exige colgarse | sí |
| Exige brazos arriba | sí |
| Alto impacto | no |
| Implemento principal | `pull-up bar` |
| Requisito | grupo 1: `pull-up bar` |
| Instrucciones (es/en, pasos) | Reescritas según el GIF |

**Qué no cambia:** dificultad, zona, músculo, prescripción y actividad.

**Cómo está construida:**

- Identifica el ejercicio por id, `source_code` (0858) y `name_en`, y aborta sin tocar nada si no coinciden.
- Es idempotente.
- Termina con una verificación que aborta si el resultado no es el previsto.

**`MigracionPropuesta1374IT`** (4, PostgreSQL efímero con V1–V33; el script en una transacción, como Flyway):

1. antes de la migración, la contención ya protege;
2. con otro `name_en`, el guardia aborta y no cambia nada;
3. se aplica, pasa el disparador diferido `tg_exercises_principal`, queda igual que el 61 y reaplicarla no cambia nada;
4. después, los filtros de seguridad lo excluyen solos. Además lo reciben los principiantes con barra, porque ya no es de alto impacto.

**Para desplegarla:**

- copiarla a `db/migration` con el siguiente número libre;
- después, retirar la contención.

Ambas cosas, después de las secciones 4 y 5.

## 4. Planes existentes: medir antes de desplegar

**La contención no corrige lo ya publicado.** Una semana activa publicada antes de esta corrección sigue mostrando el 1374 a quien lo tenga: la app lee el plan guardado, no el catálogo. La contención solo actúa en las semanas que se generan o adaptan desde ahora.

Consultas de **solo lectura**, para la base de pruebas o para producción con un usuario de lectura:

```sql
-- Exposición: semanas activas o futuras con el 1374 prescrito a quien no cumple sus requisitos reales
SELECT p.user_id, p.plan_id, p.week_start_date, pw.scheduled_date, pw.status,
       up.allow_hanging_exercises, up.allow_overhead_exercises,
       'pull-up bar' = ANY (up.equipment_codes) AS tiene_barra, up.training_location
  FROM planned_workout_exercises pwe
  JOIN planned_workouts pw ON pw.planned_workout_id = pwe.planned_workout_id
  JOIN weekly_training_plans p ON p.plan_id = pw.plan_id
  JOIN user_profiles up ON up.user_id = p.user_id
 WHERE pwe.exercise_id = 1374
   AND p.status = 'ACTIVE'
   AND p.week_end_date >= current_date
   AND (up.allow_hanging_exercises IS FALSE OR up.allow_overhead_exercises IS FALSE
        OR NOT ('pull-up bar' = ANY (up.equipment_codes)))
 ORDER BY pw.scheduled_date;

-- Histórico: cuántas veces se prescribió y cuántas se ejecutó
SELECT count(*) AS prescripciones, count(DISTINCT p.user_id) AS participantes
  FROM planned_workout_exercises pwe
  JOIN planned_workouts pw ON pw.planned_workout_id = pwe.planned_workout_id
  JOIN weekly_training_plans p ON p.plan_id = pw.plan_id
 WHERE pwe.exercise_id = 1374;
```

## 5. Qué hace hoy P2-D con una semana que contiene el 1374 (diagnóstico, probado)

`AdaptacionRegresionIT` › `0c` (PostgreSQL efímero, flujo real, validadores reales): el 1374 se inyecta en la semana medida como si fuera anterior a la contención.

| Participante | Resultado de la adaptación |
|---|---|
| Sin barra | **No se conserva.** `NO_FACTIBLE` con «V1: el ejercicio 1374 …», evidencia registrada, sin plan; la semana medida intacta |
| Con barra pero brazos arriba prohibidos | Igual: `NO_FACTIBLE` |
| Con barra y sin restricciones | Copia exacta con el 1374; `generation_attempts = 1`, `ai_calls = 0` |

**Por caminos:**

- **`NO_CHANGE` / `INSUFFICIENT_DATA` / respaldo.** La continuidad se revalida entera; V1 rechaza la sesión con el 1374 y la semana sale `NO_FACTIBLE`. **No se publica nada inseguro, pero el participante se queda sin semana** hasta una intervención.
- **Adaptación con IA.**
  - El prompt ya no ofrece el 1374.
  - Si la sesión que lo contiene **no** está señalada, V8C prohíbe cambiarla: la IA no puede arreglarla, los intentos fallan por V1 y el respaldo termina en `NO_FACTIBLE`.
  - Si está señalada con una intervención que permite sustituir, la IA puede reemplazarlo y la semana pasa por todas las validaciones.
- **Generación inicial:** no le afecta (contenido).

## 6. Propuesta: sustitución determinista segura (NO implementada; requiere autorización)

Para no dejar sin semana a quien cumple el plan, la continuidad podría sustituir **solo** los ejercicios que dejaron de ser elegibles por **seguridad**:

1. Alternativa elegible para el perfil actual, en este orden de preferencia:
   1. misma clase de slot (estructural sigue estructural en el mismo slot; relleno sigue relleno);
   2. misma zona, o en su defecto una zona que el enfoque admita;
   3. mismo tipo de prescripción;
   4. misma clase de dosis 4D, copiando la dosis.

   Además: que no esté ya en la sesión, que no se repita en días consecutivos, que no esté bloqueado y que no esté en revisión de clasificación. Desempate por id.
2. La semana resultante pasa por **todas** las validaciones (V1–V24, V8C).
3. **Si no hay alternativa válida:** `NO_FACTIBLE` con un motivo específico («sin sustituto seguro para N»), nunca una sustitución insegura.
4. Evidencia V32 que diga qué se sustituyó y por qué. Hoy V8C rechaza cualquier cambio en una sesión no señalada, así que haría falta una excepción explícita y trazable, del tipo «sustitución por seguridad de catálogo».

Toca `AdaptedWeekProducer`, `ConservationValidator` y la evidencia, es decir, clases productivas de P2-D. Por eso queda **propuesta**, con su prueba a escribir antes de implementarla.

**Lo ya publicado** (sección 4) necesitaría la misma sustitución aplicada una sola vez a las sesiones afectadas, o retirarlas de la vista con aviso. Se decide después de medir.
