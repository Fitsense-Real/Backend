# Impacto sobre planes publicados (matriz)

Esta auditoría no lee la base real: no hay acceso a Neon por orden. No modifica planes reales, no reescribe historial ni cambia dosis ni adherencia. Esta matriz describe qué hace cada migración con un plan ya publicado, a partir del código vigente. La consulta del §3, de solo lectura, permite medir la exposición real.

## 1. Cómo se referencia el catálogo

- `planned_workout_exercises` y `workout_session_exercises` guardan solo el `exercise_id`. Nombre, instrucciones, GIF y banderas se leen siempre del catálogo.
- El contexto que vio la IA (`plan_input_snapshot`, JSON) es evidencia inmutable y conserva los nombres antiguos.
- `SafetyConflictChecker` evalúa las banderas actuales del catálogo contra el perfil cuando se consulta o se inicia una sesión (P2D). Una bandera nueva que el perfil excluye bloquea las sesiones aún no iniciadas (`safetyBlocked`) y activa la sustitución por SUST-SEG-1 en la semana siguiente.

## 2. Matriz por migración

| Migración | Planes ACTIVE (sesiones SCHEDULED) | Sesión IN_PROGRESS | Sesiones completadas, registros, adherencia | Planes REPLACED / historial | Adaptación y progresión |
|---|---|---|---|---|---|
| **V47** banderas (26 ej.) | Si el perfil excluye la bandera nueva, la sesión queda bloqueada al consultarla (mecanismo P2D) y no se puede iniciar. No se repara sola: SUST-SEG-1 la sustituye en la semana siguiente. | No se interrumpe ni se sustituye: se puede omitir el ejercicio. La reparación devuelve SESION_EN_CURSO. | Sin cambio: lo registrado se conserva. Una sesión bloqueada que caduca pasa a SKIPPED, el riesgo ya documentado en la PR #55 §6. | Sin cambio. | La adaptación de la semana siguiente no puede proponer el ejercicio: es causa de seguridad y lo excluye V8C. |
| **V48** dificultad (12 ej.) | **No bloquea.** La dificultad no es causa de seguridad en `SafetyConflictPolicy`. Un principiante con 916 (curl nórdico) en su semana actual lo conserva hasta el cambio de semana. | Sin cambio. | Sin cambio. | Sin cambio. | La generación siguiente ya no lo elige por encima del nivel. La adaptación con límite de dificultad lo trata como «más difícil». |
| **V49** textos (408 filas) | El nombre y las instrucciones se ven corregidos de inmediato en la app y en la API, por `exercise_id`. Dosis y estructura no cambian. | Igual: el texto cambia durante la sesión, que es lo deseado (deja de pedir colgarse o suelo cuando no corresponde). | Sin cambio en los datos. El historial muestra el nombre nuevo, que describe lo que el usuario hizo realmente según el GIF. | Los snapshots de la IA conservan el texto antiguo como evidencia. | El 1093 pasa de impacto MODERADO a BAJO por nombre, lo que solo afecta a sustituciones futuras. |
| **V50** clasificación (9) | Sin efecto sobre lo ya publicado: los validadores se aplican al generar, no se revalidan planes existentes. | Sin cambio. | Sin cambio. | Sin cambio. | Las semanas siguientes cuentan esos ejercicios en su nuevo patrón: V15, 4C y cobertura de capacidades. |
| **V50** equipamiento (17) | Sin efecto sobre lo publicado: la elegibilidad por equipo se decide al generar. | Sin cambio. | Sin cambio. | Sin cambio. | Un usuario sin el implemento ya no recibe esos ejercicios en semanas futuras. |

**Qué afecta solo a selecciones futuras:** V48 y V50 completas, además del impacto por nombre del 1093.

**Qué actúa sobre planes ya publicados:**
- V47, mediante el bloqueo de seguridad vigente. No se ha añadido ningún mecanismo nuevo.
- V49, que solo cambia el texto mostrado.

## 3. Consulta de exposición (solo lectura, para ejecutar en la base real por quien tenga acceso)

```sql
-- Sesiones aún no iniciadas que V47 bloquearía: ejercicio con bandera nueva + perfil que la excluye.
SELECT pw.planned_workout_id, pw.scheduled_date, wp.user_id, pwe.exercise_id
  FROM planned_workout_exercises pwe
  JOIN planned_workouts pw ON pw.planned_workout_id = pwe.planned_workout_id
  JOIN weekly_training_plans wp ON wp.plan_id = pw.plan_id
  JOIN user_profiles up ON up.user_id = wp.user_id
 WHERE wp.status = 'ACTIVE' AND pw.status = 'SCHEDULED'
   AND (   (pwe.exercise_id IN (66,206,474,497,503,594,1300) AND up.allow_overhead_exercises = FALSE)
        OR (pwe.exercise_id IN (275,685,805,895)             AND up.allow_floor_exercises = FALSE)
        OR (pwe.exercise_id IN (460,496)                     AND up.allow_high_impact = FALSE)
        OR (pwe.exercise_id IN (685,696,805,995,1005)        AND up.allow_kneeling_exercises = FALSE)
        OR  pwe.exercise_id IN (149,220,227,276,819,889,999,1000,1213,1214)); -- AX: politica de edad (65+), revisar edad
-- Principiantes con un ejercicio que V48 sube de nivel (no se bloquea; solo informativo):
-- ... AND pwe.exercise_id IN (464,1108,1191,862,874,868,880,915,916,1192,821,1201) AND up.fitness_level = 'BEGINNER'
```

Las reparaciones operativas, es decir, reparar ya las sesiones futuras afectadas, quedan para una PR posterior, como pide la orden.
