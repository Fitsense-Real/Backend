# Simulación V36 / V37 / V37 + V37.1

Bases PostgreSQL 16 aisladas (Testcontainers). V36 se fija con `spring.flyway.target=36`; V37 es el catálogo vigente (`db/migration`); V37.1 (pendiente) se añade con `spring.flyway.locations`, sin estar en `db/migration`. Motor de reglas, sin IA ni Replicate.

## 1. Generación de referencia (72 perfiles)

- V36 simulado frente a la referencia de V36 (`generacion_referencia_v36.txt`): **idéntico** (72 perfiles).

| Catálogo | Perfiles | Válidos | Sin plan | Perfiles distintos de V36 | Sesiones distintas de V36 |
|---|---:|---:|---:|---:|---:|
| V36 | 72 | 72 | 0 | 0 | 0 |
| V37 (vigente) | 72 | 72 | 0 | 68 | 180 |
| V37 + V37.1 (pendiente) | 72 | 72 | 0 | 70 | 207 |

**Ejercicios que desaparecen de las referencias (V36 a V37)** (sesiones en que salen):

776 (43), 1098 (9), 308 (9), 895 (8), 307 (7), 1334 (7), 302 (7), 1096 (7), 494 (6), 115 (6), 117 (6), 118 (6), 114 (5), 896 (5), 1097 (5), 1099 (5), 1095 (5), 1103 (5), 1138 (5), 1156 (4), 301 (4), 1094 (4), 495 (4), 142 (4), 143 (4), 496 (4), 994 (3), 1072 (3), 515 (3), 1100 (3), 1040 (3), 580 (3), 461 (3), 1180 (3), 323 (3), 1058 (3), 1102 (3), 421 (3), 750 (3), 119 (2), 542 (2), 540 (2), 1037 (2), 300 (2), 325 (2), 482 (2), 1146 (2), 136 (2), 908 (2), 120 (2), 774 (2), 178 (2), 1179 (2), 1026 (2), 102 (2), 58 (2), 389 (2), 933 (2), 1284 (2), 177 (2), 704 (2), 73 (2), 1265 (2), 223 (2), 1220 (2), 340 (1), 1223 (1), 673 (1), 103 (1), 594 (1), 110 (1), 1063 (1), 935 (1), 1256 (1), 139 (1), 995 (1), 304 (1), 912 (1), 880 (1), 899 (1), 547 (1), 394 (1), 360 (1), 433 (1), 875 (1), 950 (1), 545 (1), 1046 (1), 536 (1), 63 (1), 749 (1), 116 (1), 518 (1), 147 (1), 778 (1), 710 (1), 474 (1), 387 (1), 624 (1), 355 (1), 713 (1), 435 (1), 674 (1), 150 (1), 324 (1), 1032 (1), 122 (1), 575 (1), 1064 (1), 991 (1), 1304 (1), 479 (1), 957 (1), 393 (1), 1110 (1), 1282 (1), 1222 (1), 928 (1), 889 (1), 780 (1), 1092 (1), 75 (1), 56 (1), 878 (1), 1023 (1), 77 (1), 144 (1), 911 (1), 1043 (1), 357 (1), 327 (1), 101 (1), 990 (1), 1008 (1), 592 (1), 395 (1), 1114 (1), 988 (1), 179 (1), 552 (1), 632 (1), 341 (1), 987 (1), 548 (1), 483 (1), 1024 (1), 124 (1), 513 (1), 775 (1), 705 (1), 1059 (1), 1155 (1), 989 (1), 272 (1), 636 (1), 339 (1), 959 (1), 190 (1), 152 (1), 535 (1), 667 (1), 1197 (1), 512 (1), 434 (1), 913 (1), 1002 (1), 1308 (1), 380 (1), 1031 (1), 992 (1), 422 (1), 586 (1), 549 (1), 887 (1), 432 (1), 151 (1), 1038 (1), 1285 (1), 232 (1), 306 (1)

**Los que entran** (sesiones): 142 (44), 898 (10), 1097 (8), 308 (7), 515 (7), 896 (7), 143 (7), 1099 (6), 1094 (6), 1180 (6), 116 (6), 1155 (6), 1334 (5), 119 (5), 775 (5), 673 (5), 1103 (5), 302 (4), 117 (4), 103 (4), 1179 (4), 1138 (4), 1096 (4), 301 (4), 990 (3)

**Sustituciones más frecuentes en la misma posición** (sale → entra: sesiones): 776→142: 39, 896→898: 5, 1096→1097: 4, 1098→1099: 3, 895→896: 3, 1100→1180: 3, 1103→1102: 3, 1097→1098: 3, 1138→1155: 3, 494→1334: 2, 1334→1099: 2, 515→1094: 2, 302→308: 2, 115→116: 2, 461→898: 2, 178→1180: 2, 307→395: 2, 142→673: 2, 1095→1094: 2, 302→143: 2

## 2. Barrido de 4 a 6 días (72 perfiles) y robustez (288 generaciones)

| Catálogo | Barrido: válidos | Barrido: no válidos | Robustez: válidas | Robustez: sin plan |
|---|---:|---:|---:|---:|
| V36 | 72 | 0 | 288 | 0 |
| V37 (vigente) | 72 | 0 | 288 | 0 |
| V37 + V37.1 (pendiente) | 70 | 2 | 280 | 8 |

**Capacidades y duración del barrido** (sesiones con su duración estimada respecto a los minutos configurados):

| Catálogo | Planes con cobertura COMPLETO | Sesiones | % mínimo (estimación de la evaluación) | % medio | Sesiones bajo el 70 % en la estimación (V20 las aprueba con su propia medida) |
|---|---:|---:|---:|---:|---:|
| V36 | 72 de 72 | 336 | 67.8 | 74.2 | 59 |
| V37 (vigente) | 72 de 72 | 336 | 67.8 | 74.1 | 64 |
| V37 + V37.1 (pendiente) | 70 de 72 | 328 | 67.8 | 74.1 | 54 |

Sin plan en robustez con V37 + V37.1 (pendiente) (perfil: semillas de 4): LOSE_WEIGHT/BEGINNER/4d/45/CASA=4, LOSE_WEIGHT/BEGINNER/5d/45/CASA=4

## 3. Continuidad semanal (8 LOSE_WEIGHT de 5-6 días y 96 perfiles)

| Catálogo | Pruebas | Fallos | Detalle |
|---|---:|---:|---|
| V36 | 2 | 0 | — |
| V37 (vigente) | 2 | 0 | — |
| V37 + V37.1 (pendiente) | 2 | 1 | — |

## 4. Perfiles con restricciones (192: 4 objetivos x 3 niveles x gimnasio/casa x 8 restricciones)

| Catálogo | Perfiles con violaciones entre sus elegibles | Violaciones elegibles | Planes con un ejercicio prohibido | Sin plan | 4C NO_FACTIBLE | 4C FRAGIL |
|---|---:|---:|---:|---:|---:|---:|
| V36 | 184 | 2332 | 124 | 0 | 0 | 0 |
| V37 (vigente) | 0 | 0 | 0 | 0 | 0 | 0 |
| V37 + V37.1 (pendiente) | 0 | 0 | 0 | 0 | 0 | 0 |

**Por restricción** (planes con un ejercicio prohibido / sin plan / media de elegibles de LOWER_HIP):

| Restricción | V36 | V37 (vigente) | V37 + V37.1 (pendiente) |
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
| V36 | 96 | 6 | 20.3 | 0 | 0 |
| V37 (vigente) | 96 | 5 | 18.4 | 0 | 0 |
| V37 + V37.1 (pendiente) | 96 | 5 | 18.4 | 0 | 0 |

### Cumplimiento por dimensión (planes generados de los 192 perfiles)

Se cuentan los planes que contienen al menos un ejercicio que el perfil no puede hacer **según la revisión visual**, no según las banderas del catálogo cargado.

| Catálogo | Banderas de seguridad | Dificultad (nivel 3 en principiante o intermedio) | Equipamiento real observado en el GIF | 4C robusta | Capacidades: pool ROBUSTO |
|---|---:|---:|---:|---:|---:|
| V36 | 82 | 74 | **147** | 192 de 192 | 170 de 192 |
| V37 (vigente) | 0 | 0 | **93** | 192 de 192 | 170 de 192 |
| V37 + V37.1 (pendiente) | 0 | 0 | **0** | 192 de 192 | 170 de 192 |

El equipamiento real es el problema que V37 **no** corrige: lo corrige V37.1 (bloque B2), pendiente de la corrección de V20.

## 5. Usuario 31 (respuestas guardadas de Replicate y plan 63)

**V36**

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

**V37 (vigente)**

- `v37 elegibles: 776=NO 1320=NO 1185=si 309=si 1103=si 301=si`
- `v37 intento 1: DIVISION: usa estas fechas y enfoques, que cumplen V13 y V18: 2026-11-07 UPPER_BODY, 2026-11-08 LOWER_BODY. ; V18: el ejercicio 1325 se repite en dias consecutivos (2026-11-07 y 2026-11-08). ; V23: la semana no cubre LOWER_HIP. Step 4C exige que la semana toque los cuatro patrones obligatorios cuando hay ejercicios para ellos. Hay LOWER_HIP=11 elegibl`
- `v37 intento 2: V15: el entrenamiento del 2026-11-08 dedica 4 de 6 ejercicios a upper legs. Reparte entre los grupos del enfoque LOWER_BODY.`
- `v37 intento 3: V15: el entrenamiento del 2026-11-08 dedica 3 de 5 ejercicios a upper legs. Reparte entre los grupos del enfoque LOWER_BODY.`
- `v37 plan 63: V1: el ejercicio 1320 no esta en available_exercises. ; V1: el ejercicio 776 no esta en available_exercises.`
- `v37 RULE_ENGINE: | SATURDAY UPPER_BODY 301[back/KNEE_DOMINANT] 142[back/HIP_HINGE] 1155[chest/HORIZONTAL_PUSH] 135[shoulders/HORIZONTAL_PULL] 314[upper arms/HORIZONTAL_PUSH] 1325[cardio/LOCOMOTION_CARDIO] 1173[neck/MOBILITY] | SUNDAY LOWER_BODY 1264[upper legs/KNEE_DOMINANT] 1102[waist/KNEE_DOMINANT] 147[upper legs/HIP_HINGE] 1017[lower legs/ANKLE_PLANTAR_FLEXION] 882[upper legs/TRUNK_FLEXION] 984`

**V37 + V37.1 (pendiente)**


