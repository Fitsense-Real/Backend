# Simulación V36 / V37 / V37 + V37.1

Bases PostgreSQL 16 aisladas (Testcontainers). V37 y V37.1 se cargan con `spring.flyway.locations`, sin estar en `db/migration`. Motor de reglas, sin IA ni Replicate.

## 1. Generación de referencia (72 perfiles)

- V36 simulado frente a la referencia oficial: **idéntico** (72 perfiles).

| Catálogo | Perfiles | Válidos | Sin plan | Perfiles distintos de V36 | Sesiones distintas de V36 |
|---|---:|---:|---:|---:|---:|
| V36 (vigente) | 72 | 72 | 0 | 0 | 0 |
| V37 (bloque A) | 72 | 72 | 0 | 68 | 180 |
| V37 + V37.1 | 72 | 72 | 0 | 70 | 207 |

## 2. Barrido de 4 a 6 días (72 perfiles) y robustez (288 generaciones)

| Catálogo | Barrido: válidos | Barrido: no válidos | Robustez: válidas | Robustez: sin plan |
|---|---:|---:|---:|---:|
| V36 (vigente) | 72 | 0 | 288 | 0 |
| V37 (bloque A) | 72 | 0 | 288 | 0 |
| V37 + V37.1 | 70 | 2 | 280 | 8 |

Sin plan en robustez con V37 + V37.1 (perfil: semillas de 4): LOSE_WEIGHT/BEGINNER/4d/45/CASA=4, LOSE_WEIGHT/BEGINNER/5d/45/CASA=4

## 3. Continuidad semanal (8 LOSE_WEIGHT de 5-6 días y 96 perfiles)

| Catálogo | Pruebas | Fallos | Detalle |
|---|---:|---:|---|
| V36 (vigente) | 2 | 0 | — |
| V37 (bloque A) | 2 | 0 | — |
| V37 + V37.1 | 2 | 1 | — |

## 4. Perfiles con restricciones (192: 4 objetivos x 3 niveles x gimnasio/casa x 8 restricciones)

| Catálogo | Perfiles con violaciones entre sus elegibles | Violaciones elegibles | Planes con un ejercicio prohibido | Sin plan | 4C NO_FACTIBLE | 4C FRAGIL |
|---|---:|---:|---:|---:|---:|---:|
| V36 (vigente) | 184 | 2332 | 124 | 0 | 0 | 0 |
| V37 (bloque A) | 0 | 0 | 0 | 0 | 0 | 0 |
| V37 + V37.1 | 0 | 0 | 0 | 0 | 0 | 0 |

**Por restricción** (planes con un ejercicio prohibido / sin plan / media de elegibles de LOWER_HIP):

| Restricción | V36 (vigente) | V37 (bloque A) | V37 + V37.1 |
|---|---|---|---|
| COMO_USUARIO_31 | 19 / 0 / 17.8 | 0 / 0 / 15.8 | 0 / 0 / 15.8 |
| NINGUNA | 11 / 0 / 26.2 | 0 / 0 / 25.2 | 0 / 0 / 25.2 |
| SIN_BRAZOS_ARRIBA | 13 / 0 / 26.2 | 0 / 0 / 25.2 | 0 / 0 / 25.2 |
| SIN_IMPACTO | 13 / 0 / 24.7 | 0 / 0 / 23.7 | 0 / 0 / 23.7 |
| SIN_RODILLAS | 17 / 0 / 25.2 | 0 / 0 / 23.7 | 0 / 0 / 23.7 |
| SIN_SUELO | 19 / 0 / 19.3 | 0 / 0 / 17.3 | 0 / 0 / 17.3 |
| SIN_SUSPENSION | 12 / 0 / 26.2 | 0 / 0 / 25.2 | 0 / 0 / 25.2 |
| TODAS | 20 / 0 / 17.8 | 0 / 0 / 15.3 | 0 / 0 / 15.3 |

**LOWER_HIP en perfiles sin suelo o sin suspensión** (elegibles mínimo / medio, perfiles con 0, 1 o 2):

| Catálogo | Perfiles | Mínimo | Media | Con 0 | Con 1-2 (FRAGIL) |
|---|---:|---:|---:|---:|---:|
| V36 (vigente) | 96 | 6 | 20.3 | 0 | 0 |
| V37 (bloque A) | 96 | 5 | 18.4 | 0 | 0 |
| V37 + V37.1 | 96 | 5 | 18.4 | 0 | 0 |

## 5. Usuario 31 (respuestas guardadas de Replicate y plan 63)

**V36 (vigente)**

- `actual elegibles: 776=si 1320=si 1185=si 309=si 1103=si 301=si`
- `actual intento 1: DIVISION: usa estas fechas y enfoques, que cumplen V13 y V18: 2026-11-07 UPPER_BODY, 2026-11-08 LOWER_BODY. ; V18: el ejercicio 1325 se repite en dias consecutivos (2026-11-07 y 2026-11-08). ; V23: la semana no cubre LOWER_HIP. Step 4C exige que la semana toque los cuatro patrones obligatorios cuando hay ejercicios para ellos. Hay LOWER_HIP=13 elegibl`
- `actual intento 2: V15: el entrenamiento del 2026-11-08 dedica 4 de 6 ejercicios a upper legs. Reparte entre los grupos del enfoque LOWER_BODY.`
- `actual intento 3: V15: el entrenamiento del 2026-11-08 dedica 3 de 5 ejercicios a upper legs. Reparte entre los grupos del enfoque LOWER_BODY.`
- `actual plan 63: VALIDA`
- `actual RULE_ENGINE: | SATURDAY UPPER_BODY 301[back/KNEE_DOMINANT] 776[back/HIP_HINGE] 1098[chest/HORIZONTAL_PUSH] 135[shoulders/HORIZONTAL_PULL] 1320[upper arms/HORIZONTAL_PUSH] 1325[cardio/LOCOMOTION_CARDIO] 1322[upper arms/MOBILITY] | SUNDAY LOWER_BODY 1043[upper legs/HIP_HINGE] 1102[waist/KNEE_DOMINANT] 144[upper legs/KNEE_DOMINANT] 1017[lower legs/ANKLE_PLANTAR_FLEXION] 1205[upper legs/KNEE_DO`
- `corregido elegibles: 776=NO 1320=NO 1185=si 309=si 1103=si 301=si`
- `corregido intento 1: DIVISION: usa estas fechas y enfoques, que cumplen V13 y V18: 2026-11-07 UPPER_BODY, 2026-11-08 LOWER_BODY. ; V18: el ejercicio 1325 se repite en dias consecutivos (2026-11-07 y 2026-11-08). ; V23: la semana no cubre LOWER_HIP. Step 4C exige que la semana toque los cuatro patrones obligatorios cuando hay ejercicios para ellos. Hay LOWER_HIP=12 elegibl`
- `corregido intento 2: V15: el entrenamiento del 2026-11-08 dedica 4 de 6 ejercicios a upper legs. Reparte entre los grupos del enfoque LOWER_BODY.`
- `corregido intento 3: V15: el entrenamiento del 2026-11-08 dedica 3 de 5 ejercicios a upper legs. Reparte entre los grupos del enfoque LOWER_BODY.`
- `corregido plan 63: V15: el entrenamiento del 2026-11-08 dedica 4 de 7 ejercicios a upper legs. Reparte entre los grupos del enfoque LOWER_BODY. ; V1: el ejercicio 1320 no esta en available_exercises. ; V1: el ejercicio 776 no esta en available_exercises. ; V24: el ejercicio 1103 del 2026-11-08 no lleva la dosis de ninguna capacidad de GENERAL_FITNESS. Usa exactamente una de: [STRENGTH 2-3x8, desca`
- `corregido RULE_ENGINE: | SATURDAY UPPER_BODY 302[back/KNEE_DOMINANT] 142[back/HIP_HINGE] 580[chest/HORIZONTAL_PUSH] 135[shoulders/HORIZONTAL_PULL] 686[upper arms/HORIZONTAL_PUSH] 1325[cardio/LOCOMOTION_CARDIO] 1178[lower arms/MOBILITY] | SUNDAY LOWER_BODY 1264[upper legs/KNEE_DOMINANT] 1102[waist/KNEE_DOMINANT] 144[upper legs/KNEE_DOMINANT] 985[lower legs/ANKLE_PLANTAR_FLEXION] 1005[upper legs/HIP`

**V37 (bloque A)**

- `v37 elegibles: 776=NO 1320=NO 1185=si 309=si 1103=si 301=si`
- `v37 intento 1: DIVISION: usa estas fechas y enfoques, que cumplen V13 y V18: 2026-11-07 UPPER_BODY, 2026-11-08 LOWER_BODY. ; V18: el ejercicio 1325 se repite en dias consecutivos (2026-11-07 y 2026-11-08). ; V23: la semana no cubre LOWER_HIP. Step 4C exige que la semana toque los cuatro patrones obligatorios cuando hay ejercicios para ellos. Hay LOWER_HIP=11 elegibl`
- `v37 intento 2: V15: el entrenamiento del 2026-11-08 dedica 4 de 6 ejercicios a upper legs. Reparte entre los grupos del enfoque LOWER_BODY.`
- `v37 intento 3: V15: el entrenamiento del 2026-11-08 dedica 3 de 5 ejercicios a upper legs. Reparte entre los grupos del enfoque LOWER_BODY.`
- `v37 plan 63: V1: el ejercicio 1320 no esta en available_exercises. ; V1: el ejercicio 776 no esta en available_exercises.`
- `v37 RULE_ENGINE: | SATURDAY UPPER_BODY 301[back/KNEE_DOMINANT] 142[back/HIP_HINGE] 1155[chest/HORIZONTAL_PUSH] 135[shoulders/HORIZONTAL_PULL] 314[upper arms/HORIZONTAL_PUSH] 1325[cardio/LOCOMOTION_CARDIO] 1173[neck/MOBILITY] | SUNDAY LOWER_BODY 1264[upper legs/KNEE_DOMINANT] 1102[waist/KNEE_DOMINANT] 147[upper legs/HIP_HINGE] 1017[lower legs/ANKLE_PLANTAR_FLEXION] 882[upper legs/TRUNK_FLEXION] 984`

**V37 + V37.1**

- `v37b elegibles: 776=NO 1320=NO 1185=si 309=si 1103=si 301=si`
- `v37b intento 1: DIVISION: usa estas fechas y enfoques, que cumplen V13 y V18: 2026-11-07 UPPER_BODY, 2026-11-08 LOWER_BODY. ; V18: el ejercicio 1325 se repite en dias consecutivos (2026-11-07 y 2026-11-08). ; V23: la semana no cubre LOWER_HIP. Step 4C exige que la semana toque los cuatro patrones obligatorios cuando hay ejercicios para ellos. Hay LOWER_HIP=11 elegibl`
- `v37b intento 2: V15: el entrenamiento del 2026-11-08 dedica 4 de 6 ejercicios a upper legs. Reparte entre los grupos del enfoque LOWER_BODY.`
- `v37b intento 3: V15: el entrenamiento del 2026-11-08 dedica 3 de 5 ejercicios a upper legs. Reparte entre los grupos del enfoque LOWER_BODY.`
- `v37b plan 63: V15: el entrenamiento del 2026-11-08 dedica 4 de 7 ejercicios a upper legs. Reparte entre los grupos del enfoque LOWER_BODY. ; V1: el ejercicio 1320 no esta en available_exercises. ; V1: el ejercicio 776 no esta en available_exercises. ; V24: el ejercicio 1103 del 2026-11-08 no lleva la dosis de ninguna capacidad de GENERAL_FITNESS. Usa exactamente una de: [STRENGTH 2-3x8, descanso 1`
- `v37b RULE_ENGINE: | SATURDAY UPPER_BODY 301[back/KNEE_DOMINANT] 314[upper arms/HORIZONTAL_PUSH] 994[back/HORIZONTAL_PULL] 547[chest/HORIZONTAL_PUSH] 135[shoulders/HORIZONTAL_PULL] 1325[cardio/LOCOMOTION_CARDIO] 1173[neck/MOBILITY] | SUNDAY LOWER_BODY 125[upper legs/HIP_HINGE] 1102[waist/KNEE_DOMINANT] 1303[waist/TRUNK_ROTATION_LATERAL_FLEXION] 145[upper legs/KNEE_DOMINANT] 1016[lower legs/ANKLE_PL`

