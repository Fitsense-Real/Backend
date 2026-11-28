## Catalogo v37

| Variante | Refs validas | Refs que cambian vs antes | Sesiones ref < 70 % | Barrido: planes validos | Barrido: perfiles sin plan | Sesiones barrido < 70 % | Robustez validas | 192 publicados | 192 con violaciones |
|---|---|---|---|---|---|---|---|---|---|
| antes (redondeo) | 72/72 | — | 20 | 72/72 | 0 | 64 | 288/288 | 192/192 | 0 |
| 70 % exacto sin correcciones | 72/72 | {'LOSE_WEIGHT': 2, 'GAIN_MUSCLE': 4, 'GENERAL_FITNESS': 7} | 0 | 72/72 | 0 | 0 | 288/288 | 192/192 | 0 |
| final (exacto + 2 correcciones) | 72/72 | {'LOSE_WEIGHT': 2, 'GAIN_MUSCLE': 4, 'GENERAL_FITNESS': 7} | 0 | 72/72 | 0 | 0 | 288/288 | 192/192 | 0 |

- **antes (redondeo)**: barrido sin plan ninguno; robustez sin plan ninguno; sesiones rechazadas por regla en robustez —
- **70 % exacto sin correcciones**: barrido sin plan ninguno; robustez sin plan ninguno; sesiones rechazadas por regla en robustez —
- **final (exacto + 2 correcciones)**: barrido sin plan ninguno; robustez sin plan ninguno; sesiones rechazadas por regla en robustez —

Referencias que cambian (final frente a antes, v37): LOSE_WEIGHT/BEGINNER/45/GYM, LOSE_WEIGHT/BEGINNER/45/CASA, GAIN_MUSCLE/BEGINNER/45/GYM, GAIN_MUSCLE/INTERMEDIATE/45/GYM, GAIN_MUSCLE/ADVANCED/45/GYM, GAIN_MUSCLE/ADVANCED/45/CASA, GENERAL_FITNESS/BEGINNER/45/GYM, GENERAL_FITNESS/BEGINNER/45/CASA, GENERAL_FITNESS/BEGINNER/60/GYM, GENERAL_FITNESS/INTERMEDIATE/60/GYM, GENERAL_FITNESS/INTERMEDIATE/60/CASA, GENERAL_FITNESS/ADVANCED/60/GYM, GENERAL_FITNESS/ADVANCED/60/CASA

- antes (redondeo): GAIN_MUSCLE: 138 sesiones, 5.75 ejercicios de media, max 88.2 %; GENERAL_FITNESS: 138 sesiones, 5.78 ejercicios de media, max 116.2 %; INCREASE_STRENGTH: 138 sesiones, 5.35 ejercicios de media, max 107.3 %; LOSE_WEIGHT: 138 sesiones, 4.35 ejercicios de media, max 80.9 %
- 70 % exacto sin correcciones: GAIN_MUSCLE: 138 sesiones, 5.75 ejercicios de media, max 88.2 %; GENERAL_FITNESS: 138 sesiones, 6.18 ejercicios de media, max 116.2 %; INCREASE_STRENGTH: 138 sesiones, 5.43 ejercicios de media, max 107.3 %; LOSE_WEIGHT: 138 sesiones, 4.46 ejercicios de media, max 80.9 %
- final (exacto + 2 correcciones): GAIN_MUSCLE: 138 sesiones, 5.75 ejercicios de media, max 88.2 %; GENERAL_FITNESS: 138 sesiones, 6.18 ejercicios de media, max 116.2 %; INCREASE_STRENGTH: 138 sesiones, 5.43 ejercicios de media, max 107.3 %; LOSE_WEIGHT: 138 sesiones, 4.46 ejercicios de media, max 80.9 %

## Catalogo v37b

| Variante | Refs validas | Refs que cambian vs antes | Sesiones ref < 70 % | Barrido: planes validos | Barrido: perfiles sin plan | Sesiones barrido < 70 % | Robustez validas | 192 publicados | 192 con violaciones |
|---|---|---|---|---|---|---|---|---|---|
| antes (redondeo) | 72/72 | — | 16 | 70/72 | 2 | 54 | 280/288 | 192/192 | 0 |
| 70 % exacto sin correcciones | 72/72 | {'LOSE_WEIGHT': 2, 'GAIN_MUSCLE': 3, 'GENERAL_FITNESS': 7} | 0 | 68/72 | 4 | 0 | 275/288 | 192/192 | 0 |
| final (exacto + 2 correcciones) | 72/72 | {'LOSE_WEIGHT': 2, 'GAIN_MUSCLE': 3, 'GENERAL_FITNESS': 7} | 0 | 70/72 | 2 | 0 | 280/288 | 192/192 | 0 |

- **antes (redondeo)**: barrido sin plan [('LOSE_WEIGHT/BEGINNER/4d/45/CASA', {'V20': 2}), ('LOSE_WEIGHT/BEGINNER/5d/45/CASA', {'V20': 2})]; robustez sin plan {'LOSE_WEIGHT/BEGINNER/4d/45/CASA': 4, 'LOSE_WEIGHT/BEGINNER/5d/45/CASA': 4}; sesiones rechazadas por regla en robustez {'V20': 16}
- **70 % exacto sin correcciones**: barrido sin plan [('LOSE_WEIGHT/BEGINNER/4d/45/CASA', {'V20': 2}), ('LOSE_WEIGHT/BEGINNER/5d/45/CASA', {'V20': 2}), ('LOSE_WEIGHT/BEGINNER/6d/45/CASA', {'V25': 1}), ('GENERAL_FITNESS/INTERMEDIATE/4d/45/GYM', {'V15': 1})]; robustez sin plan {'LOSE_WEIGHT/BEGINNER/4d/45/CASA': 4, 'LOSE_WEIGHT/BEGINNER/5d/45/CASA': 4, 'LOSE_WEIGHT/BEGINNER/6d/45/CASA': 4, 'GENERAL_FITNESS/ADVANCED/4d/45/GYM': 1}; sesiones rechazadas por regla en robustez {'V20': 16, 'V25': 4, 'V15': 1}
- **final (exacto + 2 correcciones)**: barrido sin plan [('LOSE_WEIGHT/BEGINNER/4d/45/CASA', {'V20': 2}), ('LOSE_WEIGHT/BEGINNER/5d/45/CASA', {'V20': 2})]; robustez sin plan {'LOSE_WEIGHT/BEGINNER/4d/45/CASA': 4, 'LOSE_WEIGHT/BEGINNER/5d/45/CASA': 4}; sesiones rechazadas por regla en robustez {'V20': 16}

Referencias que cambian (final frente a antes, v37b): LOSE_WEIGHT/BEGINNER/45/GYM, LOSE_WEIGHT/BEGINNER/45/CASA, GAIN_MUSCLE/INTERMEDIATE/45/GYM, GAIN_MUSCLE/INTERMEDIATE/45/CASA, GAIN_MUSCLE/ADVANCED/45/CASA, GENERAL_FITNESS/BEGINNER/45/GYM, GENERAL_FITNESS/BEGINNER/60/GYM, GENERAL_FITNESS/BEGINNER/60/CASA, GENERAL_FITNESS/INTERMEDIATE/60/GYM, GENERAL_FITNESS/INTERMEDIATE/60/CASA, GENERAL_FITNESS/ADVANCED/60/GYM, GENERAL_FITNESS/ADVANCED/60/CASA

- antes (redondeo): GAIN_MUSCLE: 138 sesiones, 5.75 ejercicios de media, max 88.2 %; GENERAL_FITNESS: 138 sesiones, 5.86 ejercicios de media, max 116.2 %; INCREASE_STRENGTH: 138 sesiones, 5.35 ejercicios de media, max 107.3 %; LOSE_WEIGHT: 130 sesiones, 4.28 ejercicios de media, max 80.9 %
- 70 % exacto sin correcciones: GAIN_MUSCLE: 138 sesiones, 5.75 ejercicios de media, max 88.2 %; GENERAL_FITNESS: 134 sesiones, 6.18 ejercicios de media, max 116.2 %; INCREASE_STRENGTH: 138 sesiones, 5.43 ejercicios de media, max 107.3 %; LOSE_WEIGHT: 126 sesiones, 4.31 ejercicios de media, max 84.7 %
- final (exacto + 2 correcciones): GAIN_MUSCLE: 138 sesiones, 5.75 ejercicios de media, max 88.2 %; GENERAL_FITNESS: 138 sesiones, 6.16 ejercicios de media, max 116.2 %; INCREASE_STRENGTH: 138 sesiones, 5.43 ejercicios de media, max 107.3 %; LOSE_WEIGHT: 130 sesiones, 4.36 ejercicios de media, max 84.7 %

