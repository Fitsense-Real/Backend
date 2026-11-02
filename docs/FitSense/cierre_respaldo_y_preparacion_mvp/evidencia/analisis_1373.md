# Analisis de sustitucion SUST-SEG-1 del ejercicio 1373

Casos analizados: 2; sin sustituto SUST-SEG-1: 2

## Caso C1: usuario 2, sesion 2026-11-06 (FULL_BODY)

- Desenlace del ciclo: publicado=true, estado V32=NO_FACTIBLE, origen=null
- Motivo V32: SEGURIDAD: el ejercicio 1373 de la sesion 6 (2026-11-13) dejo de ser compatible por seguridad (dolor_registrado) y no hay sustituto elegible con la regla SUST-SEG-1: mismo slot (relleno), tipo SETS_REPS y zona que admita el enfoque FULL_BODY. Requiere intervencion; no se publica una sustitucion insegura. | V8C: la sesion 6 registro dolor y no se senalo para adaptarla y vuelve a prescribir [1373]. Un ejercicio que dolio no se repropone porque estuviera en el plan anterior: o se sustituye por una alternativa segura, o se retira. | V1: el ejercicio 1373 no esta en available_exercises.
- Sustituciones V32: "sustituciones": []
- Ejercicios de la sesion: [1221, 117, 1237, 950, 1324, 1373]; contiguos: []
- `SafetySubstitutionPolicy.elegir` con el contexto real: **sin sustituto**

- **P-03 nivel 2 (semana entera con reglas):** repeticiones 416 -> 416, segundos 1200 -> 1200 (dentro del trabajo medido)
- **Variante localizada (solo 2026-11-13 regenerada):** V1-V25: ninguno; sin el 1373: true; sesion: repeticiones 212 -> 212, segundos 360 -> 360; semana: repeticiones 416 -> 416, segundos 1200 -> 1200; ejercicios de la sesion: 376 3x10, 473 3x18, 355 3x18, 1157 3x18, 1135 3x120s, 884 2x10

**Original:** 1373 «Círculos de muñeca»: zona lower arms, equipo body weight, dificultad 1, tipo SETS_REPS, patron MOBILITY, slot relleno, complemento true, musculo forearms

### Embudo sobre los elegibles del perfil (731)

| Filtro | Quedan |
|---|---|
| F1 distinto del original | 731 |
| F2 misma clase de slot (relleno) | 418 |
| F3 mismo tipo (SETS_REPS) | 351 |
| F4 misma condicion de complemento (true) | 0 |
| F5 zona que admite FULL_BODY o la misma (lower arms) | 0 |
| F6 brazo admitido (V22) | 0 |
| F7 no en revision | 0 |
| F8 no en la sesion ni en un dia contiguo | 0 |

**Candidatos que fallan UN solo filtro:**

- F3 mismo tipo (SETS_REPS): 21 (67 Estiramiento de cuadriceps en cuatro apoyos [upper legs, DURATION, MOBILITY]; 881 Estiramiento de isquiotibiales [upper legs, DURATION, MOBILITY]; 917 Estiramiento en cruz [upper legs, DURATION, MOBILITY]; 978 Estiramiento de isquiotibiales con pierna elevada [upper legs, DURATION, MOBILITY]; 1075 Estiramiento de cuádriceps acostado de lado [upper legs, DURATION, MOBILITY]; 1149 Estiramiento rana oscilante [upper legs, DURATION, MOBILITY]; 1150 Estiramiento para corredores [upper legs, DURATION, MOBILITY]; 1151 Estiramiento de glúteos sentado [upper legs, DURATION, MOBILITY]; 1193 Estiramiento sentado del piriforme [upper legs, DURATION, MOBILITY]; 1375 El mejor estiramiento del mundo [upper legs, DURATION, MOBILITY]; 66 Estiramiento de espalda y pectorales [back, DURATION, MOBILITY]; 938 Estiramiento de espalda alta de rodillas [back, DURATION, MOBILITY]; ...)
- F4 misma condicion de complemento (true): 197 (68 Toque de dedos circular con brazos abiertos [upper legs, SETS_REPS, TRUNK_FLEXION]; 123 Rotación interna de cadera tumbado con banda [upper legs, SETS_REPS, OTHER]; 126 Rotación interna de cadera sentado con banda [upper legs, SETS_REPS, OTHER]; 254 Toque de dedos básico [upper legs, SETS_REPS, TRUNK_FLEXION]; 310 Giro acostado con rodillas flexionadas [upper legs, SETS_REPS, TRUNK_ROTATION_LATERAL_FLEXION]; 875 Patadas flutter [upper legs, SETS_REPS, TRUNK_FLEXION]; 882 Toque de dedos circular con manos entrelazadas [upper legs, SETS_REPS, TRUNK_FLEXION]; 883 Toque circular con manos entrelazadas [upper legs, SETS_REPS, TRUNK_FLEXION]; 884 Abrazar rodillas al pecho [upper legs, SETS_REPS, MOBILITY]; 1044 Abducción de cadera sentado [upper legs, SETS_REPS, HIP_ABDUCTION_ADDUCTION]; 1045 Aducción de cadera sentado [upper legs, SETS_REPS, HIP_ABDUCTION_ADDUCTION]; 1158 Extensión de piernas con banda [upper legs, SETS_REPS, OTHER]; ...)

### Embudo sobre el catalogo activo entero, sin filtro de perfil (1219)

| Filtro | Quedan |
|---|---|
| F1 distinto del original | 1218 |
| F2 misma clase de slot (relleno) | 674 |
| F3 mismo tipo (SETS_REPS) | 597 |
| F4 misma condicion de complemento (true) | 0 |
| F5 zona que admite FULL_BODY o la misma (lower arms) | 0 |
| F6 brazo admitido (V22) | 0 |
| F7 no en revision | 0 |
| F8 no en la sesion ni en un dia contiguo | 0 |

**Candidatos que fallan UN solo filtro:**

- F4 misma condicion de complemento (true): 332 (57 Abdominal 3/4 [waist, SETS_REPS, TRUNK_FLEXION]; 58 Inclinación lateral a 45° [waist, SETS_REPS, TRUNK_ROTATION_LATERAL_FLEXION]; 60 Toques de talón alternos [waist, SETS_REPS, TRUNK_FLEXION]; 61 Brazos sueltos colgantes con rodillas flexionadas [waist, SETS_REPS, TRUNK_FLEXION]; 62 Cables sujetando antebrazo con piernas rectas [waist, SETS_REPS, TRUNK_FLEXION]; 63 Sit-up completo con brazos elevados [waist, SETS_REPS, TRUNK_FLEXION]; 65 Plancha trasera [back, SETS_REPS, CORE_STABILITY]; 68 Toque de dedos circular con brazos abiertos [upper legs, SETS_REPS, TRUNK_FLEXION]; 69 Extensión de espalda en balón de ejercicio [back, SETS_REPS, TRUNK_EXTENSION]; 70 Giro ruso asistido [waist, SETS_REPS, TRUNK_ROTATION_LATERAL_FLEXION]; 95 Curl de antebrazo invertido con banda [lower arms, SETS_REPS, WRIST_FOREARM]; 98 Elevación frontal con banda [shoulders, SETS_REPS, SHOULDER_RAISE]; ...)
- F3 mismo tipo (SETS_REPS): 27 (66 Estiramiento de espalda y pectorales [back, DURATION, MOBILITY]; 67 Estiramiento de cuadriceps en cuatro apoyos [upper legs, DURATION, MOBILITY]; 456 Estiramiento de pecho y hombro delantero [chest, DURATION, MOBILITY]; 466 Estiramiento de pecho con balón medicinal [chest, DURATION, MOBILITY]; 773 Estiramiento dinámico de pecho [chest, DURATION, MOBILITY]; 810 Estiramiento lateral en balón [back, DURATION, MOBILITY]; 811 Estiramiento de flexor de cadera con balón [upper legs, DURATION, MOBILITY]; 822 Estiramiento de espalda baja en balón (pirámide) [back, DURATION, MOBILITY]; 823 Estiramiento lateral en balón acostado [back, DURATION, MOBILITY]; 827 Estiramiento de isquiotibiales sentado con balón [upper legs, DURATION, MOBILITY]; 881 Estiramiento de isquiotibiales [upper legs, DURATION, MOBILITY]; 917 Estiramiento en cruz [upper legs, DURATION, MOBILITY]; ...)

## Caso C2: usuario 32, sesion 2026-11-06 (FULL_BODY)

- Desenlace del ciclo: publicado=false, estado V32=NO_FACTIBLE, origen=null
- Motivo V32: SEGURIDAD: el ejercicio 1373 de la sesion 99 (2026-11-13) dejo de ser compatible por seguridad (dolor_registrado) y no hay sustituto elegible con la regla SUST-SEG-1: mismo slot (relleno), tipo SETS_REPS y zona que admita el enfoque FULL_BODY. Requiere intervencion; no se publica una sustitucion insegura. | V8C: la sesion 99 registro dolor y no se senalo para adaptarla y vuelve a prescribir [1373]. Un ejercicio que dolio no se repropone porque estuviera en el plan anterior: o se sustituye por una alternativa segura, o se retira. | V1: el ejercicio 1373 no esta en available_exercises.
- Sustituciones V32: "sustituciones": []
- Ejercicios de la sesion: [1155, 233, 121, 1177, 1373, 574]; contiguos: []
- `SafetySubstitutionPolicy.elegir` con el contexto real: **sin sustituto**

- **P-03 nivel 2 (semana entera con reglas):** repeticiones 416 -> 396, segundos 1200 -> 1260 (**sube el trabajo: rechazada**)
- **Variante localizada (solo 2026-11-13 regenerada):** V1-V25: ninguno; sin el 1373: true; sesion: repeticiones 212 -> 192, segundos 360 -> 420; semana: repeticiones 416 -> 396, segundos 1200 -> 1260; ejercicios de la sesion: 743 3x10, 340 3x18, 1032 3x18, 310 3x18, 1117 3x120s, 455 2x30s

### Embudo sobre los elegibles del perfil (731)

| Filtro | Quedan |
|---|---|
| F1 distinto del original | 731 |
| F2 misma clase de slot (relleno) | 418 |
| F3 mismo tipo (SETS_REPS) | 351 |
| F4 misma condicion de complemento (true) | 0 |
| F5 zona que admite FULL_BODY o la misma (lower arms) | 0 |
| F6 brazo admitido (V22) | 0 |
| F7 no en revision | 0 |
| F8 no en la sesion ni en un dia contiguo | 0 |

**Candidatos que fallan UN solo filtro:**

- F3 mismo tipo (SETS_REPS): 21 (67 Estiramiento de cuadriceps en cuatro apoyos [upper legs, DURATION, MOBILITY]; 881 Estiramiento de isquiotibiales [upper legs, DURATION, MOBILITY]; 917 Estiramiento en cruz [upper legs, DURATION, MOBILITY]; 978 Estiramiento de isquiotibiales con pierna elevada [upper legs, DURATION, MOBILITY]; 1075 Estiramiento de cuádriceps acostado de lado [upper legs, DURATION, MOBILITY]; 1149 Estiramiento rana oscilante [upper legs, DURATION, MOBILITY]; 1150 Estiramiento para corredores [upper legs, DURATION, MOBILITY]; 1151 Estiramiento de glúteos sentado [upper legs, DURATION, MOBILITY]; 1193 Estiramiento sentado del piriforme [upper legs, DURATION, MOBILITY]; 1375 El mejor estiramiento del mundo [upper legs, DURATION, MOBILITY]; 66 Estiramiento de espalda y pectorales [back, DURATION, MOBILITY]; 938 Estiramiento de espalda alta de rodillas [back, DURATION, MOBILITY]; ...)
- F4 misma condicion de complemento (true): 197 (68 Toque de dedos circular con brazos abiertos [upper legs, SETS_REPS, TRUNK_FLEXION]; 123 Rotación interna de cadera tumbado con banda [upper legs, SETS_REPS, OTHER]; 126 Rotación interna de cadera sentado con banda [upper legs, SETS_REPS, OTHER]; 254 Toque de dedos básico [upper legs, SETS_REPS, TRUNK_FLEXION]; 310 Giro acostado con rodillas flexionadas [upper legs, SETS_REPS, TRUNK_ROTATION_LATERAL_FLEXION]; 875 Patadas flutter [upper legs, SETS_REPS, TRUNK_FLEXION]; 882 Toque de dedos circular con manos entrelazadas [upper legs, SETS_REPS, TRUNK_FLEXION]; 883 Toque circular con manos entrelazadas [upper legs, SETS_REPS, TRUNK_FLEXION]; 884 Abrazar rodillas al pecho [upper legs, SETS_REPS, MOBILITY]; 1044 Abducción de cadera sentado [upper legs, SETS_REPS, HIP_ABDUCTION_ADDUCTION]; 1045 Aducción de cadera sentado [upper legs, SETS_REPS, HIP_ABDUCTION_ADDUCTION]; 1158 Extensión de piernas con banda [upper legs, SETS_REPS, OTHER]; ...)

