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

## 5. Qué hace P2-D con una semana que contiene el 1374

**Antes de SUST-SEG-1** (diagnóstico del 23/10, sustituido): la continuidad con el 1374 se rechazaba por V1 y quien no tenía barra, o no podía levantar los brazos, se quedaba sin semana (`NO_FACTIBLE`).

**Ahora** (desarrollo y pruebas, no desplegado): el 1374 heredado se sustituye por un relleno elegible con dosis recalculada, la semana pasa por V8C (excepción explícita) y por V1–V24, y la evidencia V32 registra la sustitución. Si no hay sustituto, sigue en `NO_FACTIBLE` con un motivo `SEGURIDAD:`. Con barra y sin restricciones, la copia sigue siendo exacta.

Detalle, reglas y pruebas: [`P2D_SUSTITUCION_SEGURIDAD.md`](P2D_SUSTITUCION_SEGURIDAD.md).

## 6. Pendiente

- **Planes ya publicados** (sección 4): identificarlos, bloquear en la vista la sesión afectada o repararla en una versión nueva, sin tocar el histórico.
- **V34** sigue fuera de `db/migration` hasta tener lo anterior y las comprobaciones de despliegue.
