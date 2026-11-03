# 10 · Revisión de catálogo: 580, 775, 780 y 862 (fase 6)

**Ficha para un profesional de entrenamiento.** **No se modifica el catálogo.**
- Lo que sigue son los datos tal como están y las dudas que plantean.
- **No hay correcciones** basadas en el nombre ni en la interpretación del desarrollador.
- V8C **no** exige el mismo músculo principal (H2 sigue abierto).

## Fuentes

- **Ficha original:** la semilla de ExerciseDB en `V9__catalog_full.sql` (código, nombre en inglés, zona, equipamiento, músculo, sinérgico, secundarios e instrucciones en inglés y español).
- **Estado vigente:** la base con las 46 migraciones (`evaluacion_calidad_adaptacion_v2/evidencia/catalogo_exercises_v44_2.csv`).
- **Clasificación de patrón:** `V26__movement_pattern.sql`. Equipamiento requerido: `V29`/`V37`.
- **Auditorías anteriores** (`docs/FitSense/auditoria_catalogo/`):
  - `fase1/hallazgos.tsv`;
  - `v37/DECISIONES_PENDIENTES.md`, que ya tenía 780 y 862 como PENDIENTE_CRITERIO.

## 1. Fichas

| Campo | 580 | 775 | 780 | 862 |
|---|---|---|---|---|
| Código ExerciseDB | 0316 | 1772 | 0437 | 0467 |
| Nombre (inglés) | *dumbbell incline breeding* | *elbow lift - reverse push-up* | *dumbbell upright row* | *gorilla chin* |
| Traducción española | «Curl inclinado con mancuerna» | «Elevación de codo y push-up invertido» | «Remo vertical con mancuerna» | «Dominada gorila» |
| Zona (`body_part`) | chest | back | shoulders | waist |
| Equipamiento | dumbbell (+ banco, V29) | body weight | dumbbell | pull-up bar |
| Músculo objetivo | pectorals | upper back | delts | **abs** |
| Sinérgico | shoulders | triceps | traps | forearms |
| Secundarios | shoulders, triceps | triceps, shoulders | traps, biceps | forearms, biceps |
| Patrón (V26) | HORIZONTAL_PUSH | HORIZONTAL_PUSH | HORIZONTAL_PULL | **VERTICAL_PULL** |
| Aislamiento | no | no | no | no |
| Dificultad | 1 | 1 | 2 | **1** |
| Prescripción | SETS_REPS | SETS_REPS | SETS_REPS | SETS_REPS |
| Activo | sí | sí | sí | sí |

**Instrucciones originales** (inglés, `V9`, resumidas sin interpretar):
- **580:** banco inclinado a 45°, mancuernas con agarre prono, brazos extendidos; bajar a los lados del pecho con los codos a 90° y empujar de vuelta. *(Lo que describe es un press inclinado.)*
- **775:** boca abajo en el suelo, manos bajo los hombros; empujar con las palmas para elevar el tronco con los codos pegados y «apretar la espalda alta» arriba; bajar.
- **780:** de pie, mancuernas delante de los muslos; elevarlas hacia la barbilla guiando con los codos; bajar.
- **862:** colgado de una barra de dominadas con agarre prono, core activado; subir el cuerpo hasta pasar la barbilla por encima de la barra; bajar.

## 2. Dudas de clasificación (para el profesional; no resueltas aquí)

| Id | Duda | Por qué importa en la adaptación | Antecedente |
|---|---|---|---|
| 580 | La traducción española («Curl…») no corresponde a la ficha. Las instrucciones describen un **press inclinado**, y «breeding» parece un error de la fuente. El patrón y el músculo (HORIZONTAL_PUSH, pectorales) son coherentes con un press | La persona ve un nombre que no corresponde al ejercicio. Las sustituciones del mismo patrón lo cambian por fondos asistidos o flexiones de tríceps | H7 (PR #30) |
| 775 | Zona *back* y músculo *upper back*, pero patrón **HORIZONTAL_PUSH** (empuje desde el suelo con los codos pegados). Mezcla una extensión de espalda con un empuje | Sus sustitutos de empuje (73, 313) son de pecho o tríceps: el músculo pasa de espalda alta a pectoral o tríceps | H7; `fase1/hallazgos.tsv`: G1 (patrón y zona incoherentes) y G3 (nombre y zona incoherentes) |
| 780 | El remo al mentón es una **elevación vertical** del hombro, clasificada como **HORIZONTAL_PULL**, así que cuenta como tracción horizontal (UPPER_PULL) | Se sustituye por remos de espalda (119, 300): de deltoides a espalda | H7; `v37/DECISIONES_PENDIENTES.md`: «SHOULDER_RAISE o SCAPULAR_ELEVATION», PENDIENTE_CRITERIO |
| 862 | Una dominada colgada (barra de dominadas) con **músculo objetivo abdominales**, zona *waist*, **dificultad 1** y patrón VERTICAL_PULL. Sinérgico antebrazos, sin dorsal | 1) Las sustituciones del mismo patrón la cambian por dominadas o jalones asistidos (74, 75): de abdominales a dorsal (H13). 2) Una dominada colgada con dificultad 1 aparece en planes de **principiantes** | H13 (PR #30); `fase1/hallazgos.tsv`: D2 (suspensión con dificultad 1) y G1; `v37/DECISIONES_PENDIENTES.md`: dificultad 3 «probable», PENDIENTE_CRITERIO |

## 3. Sustituciones históricas afectadas (`04`)

| Id | Sustituciones | Detalle | Cambio de músculo |
|---|---:|---|---:|
| 580 | 5 | U04 (GM), PR #28 C y PR #29: → 102 (×2), 73, 313, 129 | 2 (→ tríceps) |
| 775 | 2 | U09 (FU), PR #28 C → 73; C2_r1 → 313 | 2 |
| 780 | 3 | U09, PR #28 C → 300; C2_r1 y C2_r2 → 119 | 3 |
| 862 | 4 | R05 y R08, RC1_r1 y RC2_r1 → 74 / 75 | 4 |
| **Total** | **14 de 123** | | **11 de los 23 cambios de músculo** observados |

**Casi la mitad de los cambios de músculo** (11 de 23) salen de **4 ejercicios** cuya clasificación es dudosa. Por eso:
- antes de cualquier regla de músculo en V8C (H2), conviene **revisar estas fichas**;
- una regla de músculo absoluta, con el catálogo actual, protegería clasificaciones posiblemente erróneas (por ejemplo, exigir que 862 se sustituya por otro ejercicio de «abdominales»).

## 4. Preguntas para el profesional

1. **580:** ¿es un press inclinado con mancuernas? ¿Qué nombre español corresponde? ¿HORIZONTAL_PUSH es adecuado para un press inclinado a 45°, o el ángulo justifica otro tratamiento?
2. **775:** ¿qué movimiento es realmente? ¿Empuje (HORIZONTAL_PUSH, tríceps) o extensión de espalda? ¿Qué músculo objetivo tiene?
3. **780:** ¿el remo al mentón debe contar como tracción horizontal (cobertura UPPER_PULL) o como elevación del hombro?
4. **862:** ¿qué músculo principal trabaja la *gorilla chin*? ¿Qué dificultad tiene? ¿Debe estar disponible para principiantes?
5. **En general:** cuando una persona dice «demasiado difícil», ¿qué cambio de músculo dentro del mismo patrón es aceptable (por ejemplo, pectoral → tríceps en un empuje horizontal)?

**Ningún dato del catálogo se ha cambiado en esta orden.** Cualquier corrección iría en una migración nueva, con revisión, aprobación e impacto simulado (960 perfiles y REPLAY), como en V36–V38.
