## R1. 72 referencias: oficial (antes de V36) frente a candidata (V36)

| Medida | Oficial | Candidata |
|---|---:|---:|
| Perfiles con plan VALIDA | 72/72 | 72/72 |
| Sesiones | 216 | 216 |
| Sesiones con otro dia o enfoque | — | 0 |
| Sesiones con otros minutos estimados | — | 26 (de -4 a +4 min) |
| Colocaciones (ejercicios) | 1155 | 1153 |
| Sesiones con algun ejercicio o dosis distinta | — | 213 (72 perfiles) |
| · directas (contienen alguno de los 9, antes o despues) | — | 77 |
| · indirectas (seleccion determinista) | — | 136 |

Distribucion de dosis (series x reps o segundos / descanso), las 10 mas frecuentes:

| Dosis | Oficial | Candidata |
|---|---:|---:|
| 3x10r/90 | 263 | 270 |
| 3x6r/150 | 187 | 185 |
| 3x120s/60 | 108 | 108 |
| 2x8r/120 | 99 | 99 |
| 2x10r/90 | 92 | 88 |
| 3x10r/150 | 62 | 64 |
| 3x90s/60 | 54 | 54 |
| 3x18r/45 | 53 | 53 |
| 2x30s/15 | 52 | 52 |
| 3x13r/90 | 41 | 39 |

Patrones de movimiento de las colocaciones (cada plan con su catalogo):

| Patron | Oficial | Candidata |
|---|---:|---:|
| HORIZONTAL_PUSH | 308 | 297 |
| KNEE_DOMINANT | 195 | 208 |
| LOCOMOTION_CARDIO | 162 | 162 |
| HORIZONTAL_PULL | 140 | 142 |
| HIP_HINGE | 88 | 102 |
| VERTICAL_PUSH | 61 | 69 |
| MOBILITY | 54 | 54 |
| VERTICAL_PULL | 41 | 36 |
| HIP_EXTENSION | 43 | 29 |
| SHOULDER_RAISE | 20 | 17 |
| TRUNK_FLEXION | 18 | 11 |
| TRUNK_ROTATION_LATERAL_FLEXION | 7 | 9 |
| HORIZONTAL_ADDUCTION | 4 | 6 |
| SCAPULAR_ELEVATION | 5 | 5 |
| HIP_ABDUCTION_ADDUCTION | 4 | 2 |
| CORE_STABILITY | 2 | 2 |
| TRUNK_EXTENSION | 2 | 2 |
| SHOULDER_EXTENSION | 1 | 0 |

Colocaciones que la clasificacion CORRECTA no admite (se juzgan los dos planes con los datos de V36):

| Exposicion | Oficial | Candidata |
|---|---:|---:|
| alto_impacto_en_principiante | 3 | 0 |
| dificultad>BEGINNER | 3 | 0 |
| dificultad>INTERMEDIATE | 3 | 0 |

## R2. Barrido de 72 perfiles de 4-6 dias y robustez (288 generaciones)

| Medida | Base sin V36 | Base + V36 | PR #3 sin V36 | PR #3 + V36 |
|---|---:|---:|---:|---:|
| Perfiles con plan VALIDA | 60/72 | 60/72 | 72/72 | 72/72 |
| Perfiles sin plan | 12 | 12 | 0 | 0 |
| Planes con tantas sesiones como dias pedidos | 44/60 | 44/60 | 56/72 | 56/72 |
| Sesiones | 270 | 270 | 336 | 336 |
| Sesiones por debajo del 70 % exacto (barrido) | 55 | 48 | 66 | 59 |
| Sesiones por debajo del 70 % exacto (72 referencias) | 17 | 18 | 17 | 18 |
| Robustez: generaciones | 288 | 288 | 288 | 288 |
| Robustez: VALIDA | 236 | 236 | 288 | 288 |
| Robustez: sin plan | 52 | 52 | 0 | 0 |
| Bandera BAJO_70_EXACTO | 55 | 48 | 66 | 59 |
| Bandera EJERCICIO_EN_REVISION | 96 | 15 | 104 | 19 |
| Bandera MAS_DE_LA_MITAD_SIN_V15 | 10 | 10 | 10 | 10 |
| Bandera MENOS_DE_2_DE_PIERNA | 8 | 8 | 12 | 12 |
| Bandera MENOS_DE_3_DE_TRABAJO | 16 | 16 | 60 | 60 |
| Bandera WAIST_IGUALA_O_SUPERA_PIERNA | 12 | 12 | 16 | 16 |
| Colocaciones que V36 no admite: 1267_inactivo | 44 | 0 | 51 | 0 |
| Colocaciones que V36 no admite: alto_impacto_principiante | 4 | 0 | 4 | 0 |
| Colocaciones que V36 no admite: dificultad_superior_al_nivel | 6 | 0 | 7 | 0 |
| Colocaciones que V36 no admite: trabajo_fuera_de_grupo_en_PULL | 21 | 0 | 31 | 0 |

Sesiones del barrido con otros ejercicios: base 270/270, PR #3 336/336; con otro enfoque: base 0, PR #3 0.

Apariciones de los nueve en el barrido, por enfoque (PR #3 sin V36 -> PR #3 + V36):

| Ejercicio | Antes | Despues |
|---|---|---|
| 527 | PULL 10, UPPER_BODY 7 | LEGS 1, LOWER_BODY 1 |
| 609 | PULL 5, UPPER_BODY 2 | LOWER_BODY 2 |
| 725 | PULL 9, UPPER_BODY 6 | PUSH 1, UPPER_BODY 3 |
| 760 | PULL 7, UPPER_BODY 1 | 0 |
| 872 | LOWER_BODY 27 | LOWER_BODY 1 |
| 1059 | LOWER_BODY 25 | LEGS 4, LOWER_BODY 4 |
| 1267 | PUSH 17, UPPER_BODY 34 | 0 |
| 1296 | PUSH 3, UPPER_BODY 5 | PUSH 1 |
| 1333 | 0 | LOWER_BODY 2, PULL 2, UPPER_BODY 5 |

Base: perfiles sin plan antes = despues: GENERAL_FITNESS/ADVANCED/5d/45/CASA, GENERAL_FITNESS/ADVANCED/6d/45/CASA, GENERAL_FITNESS/INTERMEDIATE/5d/45/CASA, GENERAL_FITNESS/INTERMEDIATE/6d/45/CASA, LOSE_WEIGHT/ADVANCED/5d/45/CASA, LOSE_WEIGHT/ADVANCED/5d/45/GYM, LOSE_WEIGHT/ADVANCED/6d/45/CASA, LOSE_WEIGHT/ADVANCED/6d/45/GYM, LOSE_WEIGHT/INTERMEDIATE/5d/45/CASA, LOSE_WEIGHT/INTERMEDIATE/5d/45/GYM, LOSE_WEIGHT/INTERMEDIATE/6d/45/CASA, LOSE_WEIGHT/INTERMEDIATE/6d/45/GYM

