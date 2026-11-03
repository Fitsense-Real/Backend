# 06 · Fichas pendientes de revisión: 580, 775, 780 y 862

**El análisis sigue abierto.** Este documento **no modifica el catálogo ni sus migraciones**.
- Completa la ficha de `validacion_h1_h12/10_REVISION_CATALOGO_580_775_780_862.md` con las **instrucciones originales completas**, los **sustitutos afectados** y lo que cada respuesta cambiaría.
- Las dudas se formulan; no se resuelven aquí.

**Fuentes:**
- `V9__catalog_full.sql`: semilla de ExerciseDB, nombre, músculos e instrucciones en inglés y español;
- `V11`: nombre español;
- `V24` y `V29`: equipamiento requerido;
- `V25`: banderas de movimiento;
- `V26`: patrón;
- estado vigente con las 46 migraciones: `short_paper_ieee/evaluacion_calidad_adaptacion_v2/evidencia/catalogo_exercises_v44_2.csv`, solo leído;
- sustituciones: `validacion_h1_h12/04_IMPACTO_H1_SUSTITUCIONES_HISTORICAS.csv` y los escenarios de `validacion_h1_h12/evidencia/h12`.

**Imágenes y GIF:** `images/<código>-*.jpg` y `videos/<código>-*.gif`, © Gym visual, en el almacenamiento de medios del backend. Se recomienda que el profesional los vea: las instrucciones solas no bastan para clasificar.

---

## Ficha 580

| Campo | Valor vigente |
|---|---|
| Código ExerciseDB | 0316 |
| Nombre original | *dumbbell incline breeding* |
| Traducción (V11) | «Curl inclinado con mancuerna» |
| Zona | chest |
| Equipamiento | dumbbell (+ bench, V29) |
| Músculo objetivo / sinérgico / secundarios | pectorals / shoulders / shoulders, triceps |
| Patrón (V26) | HORIZONTAL_PUSH |
| Aislamiento | no |
| Dificultad | 1 |
| Prescripción | SETS_REPS |
| Activo | sí |

**Instrucciones originales (EN):**
> Set up an incline bench at a 45-degree angle. Sit on the bench with your back against the pad and feet flat on the ground. Hold a dumbbell in each hand with an overhand grip, palms facing forward. Start with your arms fully extended, perpendicular to the ground. Lower the dumbbells slowly to the sides of your chest, keeping your elbows at a 90-degree angle. Pause for a moment at the bottom, then push the dumbbells back up to the starting position. Repeat for the desired number of repetitions.

**Instrucciones que ve la persona (ES):**
> Coloca un banco inclinado a un ángulo de 45 grados. Siéntate en el banco con la espalda apoyada en el respaldo y los pies planos sobre el suelo. Sostén una mancuerna en cada mano con agarre prono, con las palmas hacia adelante. Comienza con los brazos completamente extendidos, perpendiculares al suelo. Baja lentamente las mancuernas hacia los lados del pecho, manteniendo los codos en un ángulo de 90 grados. Haz una pausa por un momento en la parte inferior, luego empuja las mancuernas de nuevo hacia arriba hasta la posición inicial. Repite el número de repeticiones deseado.

**Sustituciones afectadas (TOO_DIFFICULT, todas aceptadas por H1):**

| Origen | Persona | Sustituto (dif.) | Músculo |
|---|---|---|---|
| PR #28 C, C1_r2 | U04 GM | 102 *band bench press*, «Press de banca con banda» (1) | pectorals → pectorals |
| C2_r1 | U04 GM | 73 *assisted wide-grip chest dip (kneeling)*, máquina (1) | pectorals → pectorals |
| C1_r1; laboratorio U04 | U04 GM | 313 *bench dip (knees bent)*, «Fondos en banco con rodillas flexionadas» (1) | pectorals → **triceps** |
| C2_r2 | U04 GM | 129 *band close-grip push-up*, «Flexión con agarre cerrado» (1) | pectorals → **triceps** |

**Dudas:**
- **(a)** El nombre español dice «curl», pero las instrucciones describen un **press inclinado**. ¿Qué nombre corresponde?
- **(b)** ¿Un press a 45° es HORIZONTAL_PUSH o merece otro tratamiento por el ángulo?
- **(c)** ¿Es aceptable cambiarlo por fondos o flexiones cerradas, de predominio tríceps, cuando la persona dice «difícil»?

**Lo que cambiaría cada respuesta:**
- (a) solo cambia el texto visible;
- (b) cambiaría la cobertura 4C y los sustitutos elegibles: requiere migración, 960 perfiles y REPLAY;
- (c) alimenta H2 (músculo).

---

## Ficha 775

| Campo | Valor vigente |
|---|---|
| Código ExerciseDB | 1772 |
| Nombre original | *elbow lift - reverse push-up* |
| Traducción (V11) | «Elevación de codo y push-up invertido» |
| Zona | back |
| Equipamiento | body weight |
| Músculo objetivo / sinérgico / secundarios | upper back / triceps / triceps, shoulders |
| Patrón (V26) | **HORIZONTAL_PUSH** |
| Aislamiento | no |
| Dificultad | 1 |
| Prescripción | SETS_REPS |
| Activo | sí |

**Instrucciones originales (EN):**
> Start by lying face down on the ground with your legs extended and your hands placed directly under your shoulders. Engage your core and press through your palms to lift your upper body off the ground, keeping your elbows close to your sides. Pause at the top for a moment, squeezing your upper back muscles. Slowly lower your body back down to the starting position. Repeat for the desired number of repetitions.

**Instrucciones (ES):**
> Comienza tumbado boca abajo en el suelo con las piernas extendidas y las manos colocadas justo debajo de los hombros. Activa el core y presiona con las palmas para levantar la parte superior del cuerpo del suelo, manteniendo los codos cerca de los costados. Haz una pausa por un momento en la parte superior, apretando los músculos de la espalda alta. Baja lentamente el cuerpo de vuelta a la posición inicial. Repite el número de repeticiones deseado.

**Sustituciones afectadas:**

| Origen | Persona | Sustituto (dif.) | Músculo |
|---|---|---|---|
| PR #28 C | U09 FU | 73 *assisted wide-grip chest dip (kneeling)* (1) | upper back → **pectorals** |
| C2_r1 | U09 FU | 313 *bench dip (knees bent)* (1) | upper back → **triceps** |
| Escenario E11 (GF principiante, casa) | — | 318 *body-up* (1) → 460 *chest tap push-up* (1) → 318 otra vez | upper back → triceps → pectorals → triceps |

**Dudas:**
- **(a)** ¿Qué movimiento es: una flexión de brazos desde el suelo con los codos pegados (empuje, tríceps) o una extensión de espalda (tipo cobra)?
- **(b)** ¿Qué músculo objetivo tiene?
- **(c)** Si es una extensión de espalda, ¿debe salir de HORIZONTAL_PUSH? Sus sustitutos actuales serían entonces incorrectos.

**Lo que cambiaría:**
- (c) cambiaría el patrón: migración, 4C, 960 perfiles y REPLAY;
- (a) y (b) solo cambian la ficha y H2.

---

## Ficha 780

| Campo | Valor vigente |
|---|---|
| Código ExerciseDB | 0437 |
| Nombre original | *dumbbell upright row* |
| Traducción (V11) | «Remo vertical con mancuerna» |
| Zona | shoulders |
| Equipamiento | dumbbell |
| Músculo objetivo / sinérgico / secundarios | delts / traps / traps, biceps |
| Patrón (V26) | **HORIZONTAL_PULL** |
| Aislamiento | no |
| Dificultad | **2** |
| Prescripción | SETS_REPS |
| Activo | sí |

**Instrucciones originales (EN):**
> Stand with your feet shoulder-width apart, holding a dumbbell in each hand with an overhand grip. Let the dumbbells hang in front of your thighs, with your arms fully extended. Keeping your back straight and your core engaged, exhale and lift the dumbbells straight up towards your chin, leading with your elbows. Pause for a moment at the top, then inhale and slowly lower the dumbbells back down to the starting position. Repeat for the desired number of repetitions.

**Instrucciones (ES):**
> Ponte de pie con los pies separados a la altura de los hombros, sujetando una mancuerna en cada mano con un agarre prono. Deja que las mancuernas cuelguen frente a tus muslos, con los brazos completamente extendidos. Manteniendo la espalda recta y el core activado, exhala y levanta las mancuernas rectas hacia tu barbilla, guiando el movimiento con los codos. Haz una pausa por un momento en la parte superior, luego inhala y baja lentamente las mancuernas de vuelta a la posición inicial. Repite el número de repeticiones deseado.

**Sustituciones afectadas** (dificultad 2 → 1, así que son bajadas registradas):

| Origen | Persona | Sustituto (dif.) | Músculo |
|---|---|---|---|
| PR #28 C | U09 FU | 300 *bench pull-ups*, «Dominadas en banco» (1) | delts → **lats** |
| C2_r1, C2_r2 | U09 FU | 119 *band one arm twisting seated row* (1) | delts → **upper back** |

**Dudas:**
- **(a)** ¿El remo al mentón debe contar como tracción horizontal (cobertura UPPER_PULL) o como elevación del hombro? `v37/DECISIONES_PENDIENTES.md` proponía SHOULDER_RAISE o SCAPULAR_ELEVATION: PENDIENTE_CRITERIO.
- **(b)** ¿Su dificultad 2 es adecuada?

**Lo que cambiaría:**
- (a) Si deja de ser HORIZONTAL_PULL, no cubriría UPPER_PULL, y sus sustitutos de espalda dejarían de ser del mismo patrón. Requiere migración, 4C, 960 perfiles y REPLAY.
- (b) Si fuera 1, sus sustituciones actuales pasarían de «bajada» a «lateral» (H12-A, textos de `04`).

---

## Ficha 862

| Campo | Valor vigente |
|---|---|
| Código ExerciseDB | 0467 |
| Nombre original | *gorilla chin* |
| Traducción (V11) | «Dominada gorila» |
| Zona | waist |
| Equipamiento | pull-up bar (V24; la semilla decía *body weight*) |
| Músculo objetivo / sinérgico / secundarios | **abs** / forearms / forearms, biceps |
| Patrón (V26) | VERTICAL_PULL |
| Banderas V25 | colgado y por encima de la cabeza (`ck_exercises_hanging_is_overhead`) |
| Aislamiento | no |
| Dificultad | **1** |
| Prescripción | SETS_REPS |
| Activo | sí |

**Instrucciones originales (EN):**
> Stand with your feet shoulder-width apart and your knees slightly bent. Grasp a pull-up bar with an overhand grip, slightly wider than shoulder-width apart. Hang from the bar with your arms fully extended and your palms facing away from you. Engage your core and pull your body up towards the bar, bringing your chin above the bar. Pause for a moment at the top, then slowly lower your body back down to the starting position. Repeat for the desired number of repetitions.

**Instrucciones (ES):**
> Ponte de pie con los pies separados a la altura de los hombros y las rodillas ligeramente flexionadas. Sujeta una barra de dominadas con agarre prono, ligeramente más ancho que la altura de los hombros. Cuélgate de la barra con los brazos completamente extendidos y las palmas hacia afuera. Activa el core y tira del cuerpo hacia la barra, llevando la barbilla por encima de la barra. Haz una pausa por un momento en la parte superior, luego baja lentamente el cuerpo de vuelta a la posición inicial. Repite el número de repeticiones deseado.

**Sustituciones afectadas:**

| Origen | Persona | Sustituto (dif.) | Músculo |
|---|---|---|---|
| PR #30 RC1_r1, RC2_r1 | R05 LW principiante, gimnasio | 74 *assisted parallel close grip pull-up*, máquina (1) | **abs** → lats |
| PR #30 RC1_r1, RC2_r1 | R08 GF intermedio, gimnasio | 75 *assisted pull-up*, máquina (1) | **abs** → lats |

**Dudas:**
- **(a)** ¿Qué músculo principal trabaja? Las instrucciones describen una dominada prona completa.
- **(b)** ¿Qué dificultad tiene? `v37` proponía 3 como «probable»: PENDIENTE_CRITERIO.
- **(c)** ¿Debe estar disponible para principiantes? Hoy entra en planes de principiante por tener dificultad 1, como en R05.
- **(d)** Si la persona la encuentra difícil, ¿una dominada asistida en máquina es una simplificación legítima? Es la pregunta P4 de `05_INSTRUMENTO_REVISION_H12A.md`.

**Lo que cambiaría:**
- (b) y (c) Si la dificultad sube, 862 desaparece de los planes de principiante: cambia la generación (960 perfiles y REPLAY), y sus sustituciones por 74 y 75 pasarían a ser **bajadas registradas**.
- (a) Cambiar el músculo solo afecta a la ficha y a H2.

---

## Resumen

| Id | Sustituciones afectadas | Cambio de músculo | Duda principal | ¿Afecta a H12-A? |
|---|---:|---:|---|---|
| 580 | 5 (+ laboratorio) | 2 | nombre; ángulo | No (dificultad 1 → 1: lateral) |
| 775 | 2 (+ E11) | 2 (+ E11) | ¿empuje o extensión? | No (lateral) |
| 780 | 3 | 3 | patrón; dificultad 2 | **Sí**: si la dificultad fuera 1, sus bajadas serían laterales |
| 862 | 4 | 4 | músculo; dificultad 1 | **Sí**: si la dificultad fuera 3, sus sustituciones serían bajadas y saldría de los planes de principiante |

**Nada de esto se ha cambiado.** Cualquier corrección iría en una migración nueva, revisada y aprobada, con impacto medido (960 perfiles y REPLAY), como en V36–V38.
