# Revisión de clasificaciones: los 8 ejercicios `LOCOMOTION_CARDIO` en revisión

**Fecha:** 23/10/2026 · **Alcance:** auditoría de datos. **No se crea ninguna migración, no se cambian filtros ni reglas y no se toca ningún dato real.**

## Fuentes y método

Para cada ejercicio se revisaron:

- **Registro del catálogo:** el estado tras aplicar V1–V33 en un PostgreSQL efímero, con nombre original, instrucciones en inglés y español, banderas, zona, implemento, dificultad y prescripción.
- **GIF:** el `gif_path` del catálogo servido desde `https://raw.githubusercontent.com/hasaneyldrm/exercises-dataset/refs/heads/main/<gif_path>`, la ruta indicada por el equipo; © Gym visual. Se examinaron 8 fotogramas repartidos por cada animación.
- **Precedentes de V26:** cómo clasificó V26 ejercicios equivalentes, para que cada propuesta siga el criterio ya aplicado y no uno nuevo.

**Criterio.** Un salto o un movimiento rápido **no** acredita cardio por sí mismo. `LOCOMOTION_CARDIO` se reserva a la locomoción cíclica y continua; la capacidad `CARDIO` dependerá además de la dosis y la organización (Paso 4).

**Veredicto (sobre la etiqueta ACTUAL):**

- `CORRECTO`: la etiqueta es la adecuada.
- `CORREGIR`: la etiqueta es errónea; el patrón destino se indica con su confianza.
- `INCIERTO`: la evidencia no basta para decidir.

## Fichas

### 59 · *air bike* · «Bicicleta de aire» · **CORREGIR**

| Campo | Valor |
|---|---|
| Registro | `waist`, peso corporal, `abs` (+ flexores de cadera), dificultad 1, `SETS_REPS`, suelo = sí, impacto = no |
| GIF (`videos/0003-1ZFqTDN.gif`) | Tumbado boca arriba, manos tras la cabeza, codo hacia la rodilla contraria alternando con la pierna extendida: un crunch de bicicleta. No hay desplazamiento |
| Instrucciones | Coinciden con el GIF («túmbate sobre tu espalda… movimiento de pedaleo») |
| Patrón actual → propuesto | `LOCOMOTION_CARDIO` → **`TRUNK_FLEXION`** (confianza alta; precedente: 106 *band bicycle crunch* = `TRUNK_FLEXION`) |
| Justificación | Es un ejercicio abdominal. El «air bike» del nombre no es la bicicleta estática |

### 93 · *backward jump* · «Salto hacia atrás» · **CORREGIR**

| Campo | Valor |
|---|---|
| Registro | `upper legs`, peso corporal, `quads` (+ isquios, glúteos, gemelos), dificultad 2, `SETS_REPS`, impacto = sí |
| GIF (`videos/1473-SaDOwk7.gif`) | Desde semisentadilla, salto vertical/hacia atrás con extensión completa y aterrizaje en semisentadilla. Saltos discretos, no locomoción continua |
| Instrucciones | Coinciden («salta hacia atrás… aterriza suavemente») |
| Patrón actual → propuesto | `LOCOMOTION_CARDIO` → **`KNEE_DOMINANT`** (confianza media; precedentes: 918/919 *jump squat* = `KNEE_DOMINANT`) |
| Justificación | Pliometría de tren inferior: explosiva, no cardiovascular. Alto impacto bien marcado |
| Consecuencia | Pasa a contar para `LOWER_KNEE` (4C) y a dosis estructural en 4D |

### 298 · *bridge - mountain climber (cross body)* · «Puente con escalador cruzado» · **INCIERTO**

| Campo | Valor |
|---|---|
| Registro | `waist`, peso corporal, `abs`, dificultad 1, `DURATION`, suelo = sí, impacto = no |
| GIF (`videos/2466-9c6T1YX.gif`) | Plancha alta; rodilla hacia el codo contrario alternando. Es la familia del escalador |
| Instrucciones | Piden expresamente «ritmo controlado» y caderas niveladas: énfasis de core |
| Patrón actual → propuesto | `LOCOMOTION_CARDIO` → sin propuesta. Candidatos: mantener (precedente 1054 *mountain climber* = `LOCOMOTION_CARDIO`) o `CORE_STABILITY`/`TRUNK_ROTATION_LATERAL_FLEXION` |
| Justificación | El movimiento es el del escalador, pero lo que lo haría cardiovascular es el ritmo y la duración, y las instrucciones piden lo contrario. Decidirlo es de la dosis (Paso 4). Mientras tanto **no cuenta como evidencia de cardio** |
| Consecuencia | Exige suelo: no está en ninguno de los 38 perfiles sin cardio |

### 876 · *forward jump* · «Salto hacia adelante» · **CORREGIR**

| Campo | Valor |
|---|---|
| Registro | `upper legs`, peso corporal, `quads`, dificultad 2, `SETS_REPS`, impacto = sí |
| GIF (`videos/1472-uZKq7lo.gif`) | Salto horizontal (salto largo) con braceo desde sentadilla y aterrizaje en sentadilla |
| Instrucciones | Coinciden, con saltos encadenados |
| Patrón actual → propuesto | `LOCOMOTION_CARDIO` → **`KNEE_DOMINANT`** (confianza media; mismos precedentes que 93) |
| Justificación | Pliometría horizontal. Encadenar saltos no la convierte en cardio |
| Consecuencia | La misma que 93 |

### 902 · *inchworm* · «Gusano caminando» · **CORREGIR** (destino incierto)

| Campo | Valor |
|---|---|
| Registro | `waist`, peso corporal, `abs` (+ hombros, isquios), dificultad 1, `SETS_REPS`, suelo = sí |
| GIF (`videos/1471-ZgsNQ6d.gif`) | De pie → manos al suelo → plancha → desciende hasta casi tocar el suelo (abajo de una flexión) → vuelve caminando con las manos → de pie |
| Instrucciones | Iguales salvo la bajada al suelo, que **el GIF muestra y las instrucciones no mencionan** |
| Patrón actual → propuesto | `LOCOMOTION_CARDIO` → **no cardio**; destino `MOBILITY` o `CORE_STABILITY` (incierto) |
| Justificación | Movimiento lento de movilidad de cadena posterior y estabilidad de hombro y core. No es locomoción cíclica |

### 903 · *inchworm v. 2* · «Inchworm v. 2» · **CORREGIR** (destino incierto)

| Campo | Valor |
|---|---|
| Registro | Igual que 902 |
| GIF (`videos/3698-TV87DNB.gif`) | Plancha alta ↔ los pies caminan hacia las manos (posición en V invertida). Repite sin volver a ponerse de pie |
| Instrucciones | Coinciden |
| Diferencia con 902 | No vuelve a la posición de pie ni baja al suelo; avanza a cuatro apoyos |
| Patrón actual → propuesto | `LOCOMOTION_CARDIO` → **no cardio**; destino `MOBILITY` o `CORE_STABILITY` (incierto) |
| Justificación | Igual que 902 |

### 1117 · *quick feet v. 2* · «Pies rápidos v. 2» · **CORRECTO**

| Campo | Valor |
|---|---|
| Registro | `upper legs`, peso corporal, `quads`, dificultad 1, `SETS_REPS`, impacto = no, suelo = no |
| GIF (`videos/3552-mweqJin.gif`) | De pie, ligeramente flexionado: apoyos rápidos alternos de baja amplitud, como correr en el sitio, sin fase de vuelo apreciable |
| Instrucciones | Coinciden («como si estuvieras corriendo en el sitio… durante el tiempo o el número de repeticiones») |
| Patrón | `LOCOMOTION_CARDIO` (se mantiene) |
| Justificación | Locomoción cíclica continua, sin impacto, sin suelo: la ruta directa de cardio más accesible del catálogo |
| Observación de datos | Prescripción `SETS_REPS` para un ejercicio continuo: candidato a `DURATION`, a decidir con 4D. Que el patrón sea correcto **no** acredita `CARDIO`; eso depende de la dosis |

### 1374 · *wind sprints* · «Sprints con viento» · **CORREGIR (seguridad)**

| Campo | Valor |
|---|---|
| Registro | `waist`, implemento principal **peso corporal**, `abs`, dificultad 1, `SETS_REPS`, impacto = **sí**, colgarse = **no**, brazos arriba = **no**, sin requisito de barra |
| GIF (`videos/0858-Qoujh3Q.gif`) | **Colgado de una barra de dominadas**, elevación alterna de rodillas. No hay carrera |
| Instrucciones | **Contradicen el GIF:** describen un sprint en pista. En gimnasio, *wind sprints* es la elevación alterna de rodillas colgado, que es lo que muestra el GIF; las instrucciones son las erróneas |
| Patrón actual → propuesto | `LOCOMOTION_CARDIO` → **`TRUNK_FLEXION`** (confianza alta; precedentes 61/62 *arm slingers hanging* = `TRUNK_FLEXION`) |
| Banderas propuestas | `requires_hanging = true`, `requires_overhead = true` (lo exige `ck_exercises_hanging_is_overhead`), `high_impact = false`, implemento principal y requisito `pull-up bar`. Son los mismos valores que 61, 62 y 867 |
| Instrucciones | Reescribir según el GIF |
| **Riesgo actual** | Hoy es elegible en 64 combinaciones perfil-objetivo de la matriz. En **32** de ellas el participante declaró **no** poder trabajar con los brazos por encima de la cabeza, en casa y sin barra. Los filtros de seguridad no lo ven porque el registro dice que no exige colgarse |

## Impacto (simulación en base efímera, 432 perfiles; nada se aplicó a datos reales)

La simulación aplica la propuesta en una copia de la base:

| Ejercicio | Cambio simulado |
|---|---|
| 59 | `TRUNK_FLEXION` |
| 93, 876 | `KNEE_DOMINANT` |
| 902, 903 | `CORE_STABILITY` (para 902/903 no hay destino decidido: la simulación toma uno, el que no da cobertura ni de cardio ni de movilidad, y no es una propuesta) |
| 1374 | Banderas y barra según su ficha |
| 1117 | Sin cambios; sale de revisión |
| 298 | Sin cambios; sigue en revisión |

La simulación usa las mismas reglas de elegibilidad que el oráculo del Paso 2.

SQL de la simulación, aplicado **solo** a una copia efímera (`CREATE DATABASE fs_sim TEMPLATE fs`):

```sql
UPDATE exercises SET movement_pattern = 'TRUNK_FLEXION'  WHERE exercise_id = 59;
UPDATE exercises SET movement_pattern = 'KNEE_DOMINANT'  WHERE exercise_id IN (93, 876);
UPDATE exercises SET movement_pattern = 'CORE_STABILITY' WHERE exercise_id IN (902, 903);
UPDATE exercises SET movement_pattern = 'TRUNK_FLEXION', high_impact = false,
       requires_hanging = true, requires_overhead = true, equipment_id = 19   -- pull-up bar
 WHERE exercise_id = 1374;
INSERT INTO exercise_equipment_requirements (exercise_id, group_no, equipment_id) VALUES (1374, 1, 19);
```

El disparador `fn_principal_en_requisitos` rechaza el requisito de barra si el implemento principal sigue siendo «peso corporal»: una corrección real de 1374 tiene que cambiar ambos.

### Los 38 perfiles sin cardio verificado

| Resultado | Perfiles | Detalle |
|---|---|---|
| **Seguirían sin candidatos** | 0 | — |
| **Mejorarían** (de `NO_VERIFICABLE` a `FRAGIL`) | **38** | **Todos dependen de un único ejercicio: 1117.** Ninguno tenía otro candidato en revisión |
| Dependerían de un incierto | 0 | 298, el único incierto, exige suelo y no aparece en ninguno |

Los 38 están en `HOME_PESO_CORPORAL` (19) y `HOME_BASICO` (19). Por nivel: `BEGINNER` 14, `INTERMEDIATE` 12, `ADVANCED` 12. Por edad: 30 → 8, 62 → 12, 72 → 18. Por restricciones: ninguna 6; sin suelo ni brazos arriba 14; todas 18.

**Corrección a lo dicho antes:** la dependencia no era del 59 sino del 1117.

### Cardio de `LOSE_WEIGHT` / `GENERAL_FITNESS` (108 perfiles cada uno)

| | 0 | 1–2 | ≥3 |
|---|---|---|---|
| Hoy (los 8 en revisión no cuentan) | 38 | 26 | 44 |
| Con la propuesta (1117 confirmado; 298 sigue sin contar) | **0** | **52** | **56** |

- Ningún perfil pierde cardio verificado.
- De los ocho, **solo 1117** aporta cardio verificado. Los otros siete no eran cardio o siguen sin acreditarse.
- Los 38 perfiles quedan con **un solo** candidato: el pool es frágil y no es una semana válida.

### Otros efectos

- **`LOWER_KNEE` (93 y 876 a `KNEE_DOMINANT`):** +2 candidatos en 16 de 108 perfiles por objetivo (`INTERMEDIATE`/`ADVANCED`, 30 años, sin prohibir impacto). Todos tenían ya 21 o más, así que ningún veredicto 4C cambia.
- **1374 en `GAIN_MUSCLE`:** hoy 16 perfiles → 18 con la propuesta.
  - **Gana 14:** casa con barra declarada o gimnasio, sin restricciones. Antes lo excluía un «alto impacto» que no tiene.
  - **Pierde 12:** sin barra, o con brazos arriba prohibido. Es exactamente la exposición indebida actual.

## Efecto sobre P2-D (sin cambios aplicados)

| Cambio | Efecto en una semana conservada que contenga el ejercicio |
|---|---|
| 59 y 902/903 pasan a otro patrón no estructural | Ninguno: siguen siendo «relleno» para 4D y siguen siendo elegibles |
| 93/876 pasan a `KNEE_DOMINANT` | En `GAIN_MUSCLE` pasan a estructurales: una dosis de relleno conservada incumple V24 y la semana sale `NO_FACTIBLE`. Es el caso que demuestra `AdaptacionRegresionIT` (reclasificación). Hay que decidir cómo represcribir antes de aplicar |
| 1374 con banderas y barra | Para quien no tenga barra o prohíba brazos arriba deja de ser elegible: V1 → `NO_FACTIBLE`, la semana no se conserva. Es el tratamiento correcto como problema de seguridad, pero deja esa semana sin plan si no hay sustitución |
| `GoalFeasibilityPolicy.CLASIFICACION_EN_REVISION` | Habría que actualizar la lista: 1117 sale; 59, 93, 876, 902, 903 y 1374 dejan de ser cardio; 298 sigue |

**Planes existentes.** Esta revisión no accede a la base real. Consulta de **solo lectura** para ejecutar en la base de pruebas o en producción con un usuario de lectura:

```sql
-- Semanas activas o futuras con alguno de los 8
SELECT p.user_id, p.plan_id, p.week_start_date, p.status, pwe.exercise_id, pw.scheduled_date
  FROM planned_workout_exercises pwe
  JOIN planned_workouts pw ON pw.planned_workout_id = pwe.planned_workout_id
  JOIN weekly_training_plans p ON p.plan_id = pw.plan_id
 WHERE pwe.exercise_id IN (59, 93, 298, 876, 902, 903, 1117, 1374)
   AND p.week_end_date >= current_date
 ORDER BY pwe.exercise_id, p.week_start_date;

-- 1374 prescrito a quien declaró no colgarse / no brazos arriba / sin barra
SELECT p.user_id, p.week_start_date, up.allow_hanging_exercises, up.allow_overhead_exercises,
       'pull-up bar' = ANY (up.equipment_codes) AS tiene_barra, up.training_location
  FROM planned_workout_exercises pwe
  JOIN planned_workouts pw ON pw.planned_workout_id = pwe.planned_workout_id
  JOIN weekly_training_plans p ON p.plan_id = pw.plan_id
  JOIN user_profiles up ON up.user_id = p.user_id
 WHERE pwe.exercise_id = 1374
   AND (up.allow_hanging_exercises IS FALSE OR up.allow_overhead_exercises IS FALSE
        OR NOT ('pull-up bar' = ANY (up.equipment_codes)));
```

## Decisiones que se proponen

1. **1374, prioridad de seguridad.** Corregir banderas, implemento y requisito de barra, patrón e instrucciones en una migración versionada. Antes hay que decidir qué hace P2-D con una semana conservada que lo contenga.
2. **59, 93, 876, 902, 903:** sacarlos de `LOCOMOTION_CARDIO`.
   - 59 → `TRUNK_FLEXION`; 93 y 876 → `KNEE_DOMINANT`.
   - 902 y 903: elegir entre `MOBILITY` y `CORE_STABILITY`. `MOBILITY` los convertiría en ruta de movilidad para `GENERAL_FITNESS`.
3. **1117:** confirmar `LOCOMOTION_CARDIO` y sacarlo de revisión. Valorar `DURATION` como prescripción con 4D.
4. **298:** queda incierto hasta 4D.

**Limitaciones de esta revisión:**

- **Por qué no es definitiva:** la revisión de GIF e instrucciones es de una sola persona, sin segundo evaluador. Para la tesis convendría una segunda revisión con acuerdo registrado, como se hizo con la dificultad.
- **Qué no cubre:** quedan 13 ejercicios en revisión, los 8 de movilidad y los 5 de zona cardio con patrón de fuerza.
- **Qué falta antes de aplicar:** la consulta de planes existentes en la base real y la decisión sobre P2-D.
