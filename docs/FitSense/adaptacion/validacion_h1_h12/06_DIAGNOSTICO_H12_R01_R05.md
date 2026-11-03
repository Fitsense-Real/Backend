# 06 · Diagnóstico de H12: R01 y R05 (fases 3.1–3.3)

**Diagnóstico, sin cambios en el algoritmo por defecto.** Fuentes (solo lectura):
- corridas con Replicate real `RC1_r1` (prompt V1) y `RC2_r1` (prompt V2) del PR #30, de dos semanas;
- predicciones conservadas en `predicciones_replicate/`;
- conjunto elegible real de cada usuario (`available_exercises` de su prompt).

**Scripts:**
- `evidencia/h12/diagnostico_h12.py` → `causas_variante.csv` (las 38 sesiones TOO_DIFFICULT de las 7 corridas con IA real) y `r01_r05_detalle.csv`.

## 1. Los dos casos

| | R01 | R05 |
|---|---|---|
| Objetivo y nivel | GAIN_MUSCLE, principiante | LOSE_WEIGHT, principiante |
| Lugar, minutos y equipamiento | Casa, 45 min (mínimo V20 1890 s); peso corporal, mancuerna y banda | Gimnasio, 60 min (mínimo 2520 s); gimnasio completo |
| Motivo de omisión (S2), semanas 1 y 2 | TOO_DIFFICULT → VARIANTE_MAS_SENCILLA_O_CARGA | Igual |
| Dificultad máxima del perfil (`max_difficulty_level`) | **1** | **1** |
| Conjunto elegible | 168 ejercicios, **todos de dificultad 1** | 174 ejercicios, **todos de dificultad 1** |
| Dosis | **No reducible:** 4D en GAIN_MUSCLE | Reducible en teoría, pero **margen 0**: la S2 se generó justo en el mínimo (2520 s) |
| Margen V20 de la S2 | 228 s | **0 s** |

## 2. Qué cambió (detalle por ejercicio)

**Resultado V32:** APLICADA en todas las filas, salvo RC1_r1 R05 semana 2: SIN_EFECTO tras un rechazo V20 (el modelo bajó 10 → 8 repeticiones con margen 0) y un segundo intento sin cambios.

#### R01 · RC1_r1 · semana 1 · S2 (TOO_DIFFICULT) · duración 2118 s → 2118 s · mínimo V20 1890 s · margen 228 s

| # | Original (dif., patrón, músculo, dosis) | Sustituto (dif., músculo) | ¿Lo introdujo la semana 1? |
|---:|---|---|---|
| 1 | 933 *kneeling push-up (male)* (1, HORIZONTAL_PUSH, pectorals, 3x10 r90) | **1098** *push-up (wall) v. 2* (1, pectorals) |  |
| 2 | 143 *band underhand pulldown* (1, VERTICAL_PULL, lats, 3x10 r90) | **114** *band close-grip pulldown* (1, lats) |  |
| 3 | 1195 *self assisted inverse leg curl* (1, KNEE_DOMINANT, hamstrings, 3x10 r90) | **144** *band squat* (1, glutes) |  |
| 4 | 101 *band shoulder press* (1, VERTICAL_PUSH, delts, 3x10 r90) | **739** *dumbbell seated shoulder press* (1, delts) |  |
| 5 | 110 *band lying straight leg raise* (1, TRUNK_FLEXION, abs, 2x13 r90) | **57** *3/4 sit-up* (1, abs) |  |
| 6 | 1099 *push-up plus* (1, HORIZONTAL_PUSH, pectorals, 3x10 r90) | **460** *chest tap push-up (male)* (1, pectorals) |  |

#### R01 · RC1_r1 · semana 2 · S2 (TOO_DIFFICULT) · duración 2118 s → 2118 s · mínimo V20 1890 s · margen 228 s

| # | Original (dif., patrón, músculo, dosis) | Sustituto (dif., músculo) | ¿Lo introdujo la semana 1? |
|---:|---|---|---|
| 1 | 1098 *push-up (wall) v. 2* (1, HORIZONTAL_PUSH, pectorals, 3x10 r90) | **129** *band close-grip push-up* (1, triceps) | sí |
| 2 | 114 *band close-grip pulldown* (1, VERTICAL_PULL, lats, 3x10 r90) | — (sin cambio) |  |
| 3 | 144 *band squat* (1, KNEE_DOMINANT, glutes, 3x10 r90) | — (sin cambio) |  |
| 4 | 739 *dumbbell seated shoulder press* (1, VERTICAL_PUSH, delts, 3x10 r90) | **136** *band twisting overhead press* (1, delts) | sí |
| 5 | 57 *3/4 sit-up* (1, TRUNK_FLEXION, abs, 2x13 r90) | — (sin cambio) |  |
| 6 | 460 *chest tap push-up (male)* (1, HORIZONTAL_PUSH, pectorals, 3x10 r90) | **494** *clock push-up* (1, pectorals) | sí |

#### R01 · RC2_r1 · semana 1 · S2 (TOO_DIFFICULT) · duración 2118 s → 2118 s · mínimo V20 1890 s · margen 228 s

| # | Original (dif., patrón, músculo, dosis) | Sustituto (dif., músculo) | ¿Lo introdujo la semana 1? |
|---:|---|---|---|
| 1 | 933 *kneeling push-up (male)* (1, HORIZONTAL_PUSH, pectorals, 3x10 r90) | **129** *band close-grip push-up* (1, triceps) |  |
| 2 | 143 *band underhand pulldown* (1, VERTICAL_PULL, lats, 3x10 r90) | **115** *band fixed back close grip pulldown* (1, lats) |  |
| 3 | 1195 *self assisted inverse leg curl* (1, KNEE_DOMINANT, hamstrings, 3x10 r90) | **144** *band squat* (1, glutes) |  |
| 4 | 101 *band shoulder press* (1, VERTICAL_PUSH, delts, 3x10 r90) | **704** *dumbbell seated alternate press* (1, delts) |  |
| 5 | 110 *band lying straight leg raise* (1, TRUNK_FLEXION, abs, 2x13 r90) | **63** *arms overhead full sit-up (male)* (1, abs) |  |
| 6 | 1099 *push-up plus* (1, HORIZONTAL_PUSH, pectorals, 3x10 r90) | **460** *chest tap push-up (male)* (1, pectorals) |  |

#### R01 · RC2_r1 · semana 2 · S2 (TOO_DIFFICULT) · duración 2118 s → 2118 s · mínimo V20 1890 s · margen 228 s

| # | Original (dif., patrón, músculo, dosis) | Sustituto (dif., músculo) | ¿Lo introdujo la semana 1? |
|---:|---|---|---|
| 1 | 129 *band close-grip push-up* (1, HORIZONTAL_PUSH, triceps, 3x10 r90) | **509** *close-grip push-up (on knees)* (1, triceps) | sí |
| 2 | 115 *band fixed back close grip pulldown* (1, VERTICAL_PULL, lats, 3x10 r90) | **117** *band kneeling one arm pulldown* (1, lats) | sí |
| 3 | 144 *band squat* (1, KNEE_DOMINANT, glutes, 3x10 r90) | **1076** *march sit (wall)* (1, glutes) | sí |
| 4 | 704 *dumbbell seated alternate press* (1, VERTICAL_PUSH, delts, 3x10 r90) | **739** *dumbbell seated shoulder press* (1, delts) | sí |
| 5 | 63 *arms overhead full sit-up (male)* (1, TRUNK_FLEXION, abs, 2x13 r90) | **57** *3/4 sit-up* (1, abs) | sí |
| 6 | 460 *chest tap push-up (male)* (1, HORIZONTAL_PUSH, pectorals, 3x10 r90) | **318** *body-up* (1, triceps) | sí |

#### R05 · RC1_r1 · semana 1 · S2 (TOO_DIFFICULT) · duración 2520 s → 2520 s · mínimo V20 2520 s · margen 0 s

| # | Original (dif., patrón, músculo, dosis) | Sustituto (dif., músculo) | ¿Lo introdujo la semana 1? |
|---:|---|---|---|
| 1 | 862 *gorilla chin* (1, VERTICAL_PULL, abs, 2x10 r120) | **74** *assisted parallel close grip pull-up* (1, lats) |  |
| 2 | 988 *lever chest press* (1, HORIZONTAL_PUSH, pectorals, 2x8 r120) | **72** *assisted chest dip (kneeling)* (1, pectorals) |  |
| 3 | 1191 *side-to-side chin* (1, VERTICAL_PULL, lats, 2x8 r120) | **75** *assisted pull-up* (1, lats) |  |
| 4 | 1046 *lever seated leg curl* (1, KNEE_DOMINANT, hamstrings, 2x8 r120) | **327** *cable assisted inverse leg curl* (1, hamstrings) |  |
| 5 | 987 *lever military press* (1, VERTICAL_PUSH, delts, 2x8 r120) | **101** *band shoulder press* (1, delts) |  |
| 6 | 1092 *machine inner chest press* (1, HORIZONTAL_PUSH, pectorals, 2x8 r120) | **313** *bench dip (knees bent)* (1, triceps) |  |
| 7 | 1054 *mountain climber* (1, LOCOMOTION_CARDIO, cardiovascular system, 3x90s r60) | — (sin cambio) |  |
| 8 | 253 *bear crawl* (1, LOCOMOTION_CARDIO, cardiovascular system, 3x90s r60) | — (sin cambio) |  |

#### R05 · RC1_r1 · semana 2 · S2 (TOO_DIFFICULT) · duración 2520 s → 2520 s · mínimo V20 2520 s · margen 0 s

| # | Original (dif., patrón, músculo, dosis) | Sustituto (dif., músculo) | ¿Lo introdujo la semana 1? |
|---:|---|---|---|
| 1 | 74 *assisted parallel close grip pull-up* (1, VERTICAL_PULL, lats, 2x10 r120) | — (sin cambio) |  |
| 2 | 72 *assisted chest dip (kneeling)* (1, HORIZONTAL_PUSH, pectorals, 2x8 r120) | — (sin cambio) |  |
| 3 | 75 *assisted pull-up* (1, VERTICAL_PULL, lats, 2x8 r120) | — (sin cambio) |  |
| 4 | 327 *cable assisted inverse leg curl* (1, KNEE_DOMINANT, hamstrings, 2x8 r120) | — (sin cambio) |  |
| 5 | 101 *band shoulder press* (1, VERTICAL_PUSH, delts, 2x8 r120) | — (sin cambio) |  |
| 6 | 313 *bench dip (knees bent)* (1, HORIZONTAL_PUSH, triceps, 2x8 r120) | — (sin cambio) |  |
| 7 | 1054 *mountain climber* (1, LOCOMOTION_CARDIO, cardiovascular system, 3x90s r60) | — (sin cambio) |  |
| 8 | 253 *bear crawl* (1, LOCOMOTION_CARDIO, cardiovascular system, 3x90s r60) | — (sin cambio) |  |

#### R05 · RC2_r1 · semana 1 · S2 (TOO_DIFFICULT) · duración 2520 s → 2520 s · mínimo V20 2520 s · margen 0 s

| # | Original (dif., patrón, músculo, dosis) | Sustituto (dif., músculo) | ¿Lo introdujo la semana 1? |
|---:|---|---|---|
| 1 | 862 *gorilla chin* (1, VERTICAL_PULL, abs, 2x10 r120) | **74** *assisted parallel close grip pull-up* (1, lats) |  |
| 2 | 988 *lever chest press* (1, HORIZONTAL_PUSH, pectorals, 2x8 r120) | **72** *assisted chest dip (kneeling)* (1, pectorals) |  |
| 3 | 1191 *side-to-side chin* (1, VERTICAL_PULL, lats, 2x8 r120) | **75** *assisted pull-up* (1, lats) |  |
| 4 | 1046 *lever seated leg curl* (1, KNEE_DOMINANT, hamstrings, 2x8 r120) | **327** *cable assisted inverse leg curl* (1, hamstrings) |  |
| 5 | 987 *lever military press* (1, VERTICAL_PUSH, delts, 2x8 r120) | **514** *dumbbell bench seated press* (1, delts) |  |
| 6 | 1092 *machine inner chest press* (1, HORIZONTAL_PUSH, pectorals, 2x8 r120) | **73** *assisted wide-grip chest dip (kneeling)* (1, pectorals) |  |
| 7 | 1054 *mountain climber* (1, LOCOMOTION_CARDIO, cardiovascular system, 3x90s r60) | **1117** *quick feet v. 2* (1, cardiovascular system) |  |
| 8 | 253 *bear crawl* (1, LOCOMOTION_CARDIO, cardiovascular system, 3x90s r60) | **853** *half knee bends (male)* (1, cardiovascular system) |  |

#### R05 · RC2_r1 · semana 2 · S2 (TOO_DIFFICULT) · duración 2520 s → 2520 s · mínimo V20 2520 s · margen 0 s

| # | Original (dif., patrón, músculo, dosis) | Sustituto (dif., músculo) | ¿Lo introdujo la semana 1? |
|---:|---|---|---|
| 1 | 74 *assisted parallel close grip pull-up* (1, VERTICAL_PULL, lats, 2x10 r120) | **76** *assisted standing chin-up* (1, lats) | sí |
| 2 | 72 *assisted chest dip (kneeling)* (1, HORIZONTAL_PUSH, pectorals, 2x8 r120) | **102** *band bench press* (1, pectorals) | sí |
| 3 | 75 *assisted pull-up* (1, VERTICAL_PULL, lats, 2x8 r120) | **113** *band assisted pull-up* (1, lats) | sí |
| 4 | 327 *cable assisted inverse leg curl* (1, KNEE_DOMINANT, hamstrings, 2x8 r120) | **915** *inverse leg curl (bench support)* (1, hamstrings) | sí |
| 5 | 514 *dumbbell bench seated press* (1, VERTICAL_PUSH, delts, 2x8 r120) | **101** *band shoulder press* (1, delts) | sí |
| 6 | 73 *assisted wide-grip chest dip (kneeling)* (1, HORIZONTAL_PUSH, pectorals, 2x8 r120) | **313** *bench dip (knees bent)* (1, triceps) | sí |
| 7 | 1117 *quick feet v. 2* (1, LOCOMOTION_CARDIO, cardiovascular system, 3x90s r60) | **1054** *mountain climber* (1, cardiovascular system) | sí |
| 8 | 853 *half knee bends (male)* (1, LOCOMOTION_CARDIO, cardiovascular system, 3x90s r60) | **253** *bear crawl* (1, cardiovascular system) | sí |

## 3. Respuesta de Replicate (justificación que vio la persona)

| Corrida | Usuario | Semana | Predicción | Justificación (extracto literal) |
|---|---|---:|---|---|
| RC1_r1 | R01 | 1 | `5zwwcgxtn5rmy0d1m56bj4m7yg` | «sustituí ejercicios por **variantes más sencillas** del mismo patrón … con la misma dosis» |
| RC1_r1 | R01 | 2 | `409qf13b81rmt0d1m56t3521f0` | «opciones con banda/agarre **más conservador** para reducir la dificultad percibida» |
| RC1_r1 | R05 | 1 | `8qxa99hv1hrmt0d1m58vggfnm0` | «variantes **asistidas y menos exigentes** … manteniendo series y repeticiones» |
| RC1_r1 | R05 | 2 | `q6qatbq92srmr0d1m58sq4gcvg` → `gysn4fbgxnrmr0d1m5991rmdj8` | Intento 1: «reduzco … (10 → 8)» (V20 lo rechaza); intento 2: «no corrijo … ya están en la mínima dosis» |
| RC2_r1 | R01 | 1 | `xbexxtxng1rmy0d1m5ca854dsc` | «variantes **más sencillas** del mismo patrón … manteniendo exactamente la dosis» |
| RC2_r1 | R01 | 2 | `2h1fp21za1rmw0d1m5crhacv1r` | «variantes **más sencillas** en la sesión señalada» |
| RC2_r1 | R05 | 1 | `hf49te2hdhrmw0d1m5dvyxj5dm` | «variantes **asistidas** del mismo patrón» |
| RC2_r1 | R05 | 2 | `xgs76yeee9rmt0d1m5dstpemz0` | «variantes **más asistidas y conservadoras**» |

**La justificación afirma una simplificación** («más sencillas», «menos exigentes») **que el catálogo no demuestra**: la dificultad pasa de 1 a 1 en las 59 sustituciones de R01 y R05.
- En R05 el sustituto suele ser una máquina asistida (72–75: *assisted … (kneeling)*, *assisted pull-up*). Puede ser más fácil en la práctica, pero el dato que lo diría no existe en el catálogo (§5).

## 4. Qué cambió en la semana 1 y qué volvió a cambiar en la semana 2

**R01:**
- **Semana 1** (las dos condiciones): **6 de 6** ejercicios sustituidos, todos 1 → 1, y misma dosis (GM no puede bajarla).
- **Semana 2:**
  - C1 cambia 3 de los 6 que acababa de introducir (1098 → 129, 739 → 136 y 460 → 494);
  - C2 cambia **los 6** (129 → 509, 115 → 117, 144 → 1076, 704 → 739, 63 → 57 y 460 → 318).
- La sesión de la semana 3 sería la cuarta versión de la misma sesión en tres semanas, sin ninguna bajada de dificultad.

**R05:**
- **Semana 1:**
  - C1 cambia 6 de 8, y los dos de cardio quedan;
  - C2 cambia **8 de 8**, todos 1 → 1, y la duración queda en 2520 s = mínimo.
- **Semana 2:**
  - C1 intenta bajar repeticiones; V20 lo rechaza (margen 0) y queda SIN_EFECTO;
  - C2 vuelve a cambiar **los 8**, y **dos vuelven a los ejercicios originales** de la semana 1: 1117 → **1054** (*mountain climber*) y 853 → **253** (*bear crawl*). Son ejercicios de la sesión que la persona había declarado demasiado difícil una semana antes.

**La rotación existe con la IA real** (18 sustituciones en 4 sesiones de la semana 2: R01 C1, R01 C2, R03 C1 y R05 C2), y puede ser **circular** (A → B → A).
- En los escenarios controlados (`08`) la rotación de R01 vuelve en la semana 3 exactamente a los sustitutos de la semana 1.

## 5. Causas (fase 3.3), con las 38 sesiones TOO_DIFFICULT de las corridas con IA real

| # | Pregunta | Resultado | Fuente |
|---|---|---|---|
| 1 | Ejercicios que ya tenían la dificultad mínima | **187 de 239** (78 %). En principiantes, **146 de 146** | `causas_variante.csv` |
| 2 | Ejercicios con una alternativa de menor dificultad (mismo patrón y tipo de prescripción, en el elegible del usuario) | **52 de 239**. **Todos en intermedios**: en principiantes, 0 (el elegible solo tiene dificultad 1) | Íd. |
| 3 | Sesiones con alguna reducción de dosis que cabe en el margen | **22 de 38** | Íd. (réplica del estimador V20 y de los pisos de V2) |
| 4 | Sesiones con carga prescrita que reducir | **0**: ninguna propuesta ni plan llevó carga en kg, y los bloques V2 de RC2 y C2 nunca listaron «cargas reducibles» («No hay carga prescrita que bajar») | Trazas y prompts |
| 5 | Sesiones cerca del mínimo V20 | Margen 0: **4**; margen < 30 s: **6** | Íd. |
| — | Dosis bloqueada por 4D (GAIN_MUSCLE) | **9 de 38** sesiones | Íd. |
| 6 | Sesiones donde solo cabía sustituir sin bajar la dificultad (sin alternativa menor ni dosis) | Las de **GM principiante** (R01 en C1 y C2: 4) y las de **margen 0** (R05: 4) | Íd. |
| 7 | Sustituciones repetidas en la semana siguiente | **18** (en 4 sesiones de la semana 2) | Íd. |
| 8 | Ejercicios cambiados por sesión (25 sesiones con alguna sustitución) | 1: 2 · 2: 2 · 3: 2 · 4: 5 · 5: 2 · **6: 5 · 7: 5 · 8: 2**. En 14 de 25 se cambia ≥ 5 | Íd. |
| 9 | ¿Se reutilizan ejercicios ya sustituidos? | **Sí:** R05 C2 vuelve a 1054 y 253 en la semana 2; R01 vuelve en la semana 3 del escenario controlado | §4; `08` |
| 10 | ¿El backend guarda historial para detectar la rotación? | **Solo parcial** (§6) | Código |

**Por nivel y objetivo:**

| Nivel y objetivo | Sesiones | Sustituciones | A menor dificultad | Laterales (= dificultad) | Con alternativa menor disponible |
|---|---:|---:|---:|---:|---:|
| Principiante GAIN_MUSCLE | 4 | 21 | 0 | 21 | 0 |
| Principiante LOSE_WEIGHT | 9 | 32 | 0 | 32 | 0 |
| Principiante INCREASE_STRENGTH | 4 | 6 | 0 | 6 | 0 |
| Principiante GENERAL_FITNESS | 5 | 0 (15 cambios de dosis) | — | — | 0 |
| Intermedio GAIN_MUSCLE | 5 | 35 | **29** | 6 | 30 ejercicios |
| Intermedio INCREASE_STRENGTH | 7 | 20 | 11 | 9 | 19 |
| Intermedio GENERAL_FITNESS | 4 | 9 | 1 | 8 | 3 |
| **Total** | **38** | **123** | **41** | **82** | 52 |

**Lectura:**
- **Las 59 sustituciones de principiantes son laterales**, porque no existe ninguna alternativa de menor dificultad.
- **En intermedios** la IA sí baja la dificultad cuando puede: 41 de 64.

## 6. ¿De dónde viene H12? (combinación de factores)

| Factor | ¿Contribuye? | Evidencia |
|---|---|---|
| **Dificultad mínima del perfil** | **Sí, el principal.** Un principiante solo tiene ejercicios de dificultad 1: «una variante más sencilla» no existe en el catálogo | §1 y §5 |
| Disponibilidad limitada | Parcial: hay muchos laterales (168–174 elegibles), pero ninguno más fácil según el catálogo | §1 |
| Política de dosis (4D) | **Sí en GM:** cierra la otra salida (bajar dosis) | R01 |
| Duración mínima (V20) | **Sí cuando el plan se genera en el mínimo:** cierra la dosis también fuera de GM | R05 (margen 0) |
| Permisos de VARIANTE | **Sí:** autoriza sustituir por el mismo patrón sin exigir menor dificultad ni limitar el número | `TipoIntervencion`, V8C |
| **Prompt** | **Sí, el que lo hace visible.** Los dos prompts ofrecen «SUSTITUTOS ADMITIDOS» con dificultad **≤** (no <), ordenados por id y hasta 6. V2 además los presenta como «opciones válidas» y pide «omite solo si ninguna sirve». La IA elige lo ofrecido y lo describe como «más sencillo» | `SessionFixPromptBuilder`, `FactibilidadAdaptacion` |
| Ninguna política limita cambios innecesarios | **Sí:** nada impide cambiar 8 de 8 ni volver a cambiar la semana siguiente | V8C |
| Historial | **No hay** registro de qué ejercicio entró por una sustitución, así que no se puede detectar la rotación de forma fiable | §6.1 |

**Conclusión:** H12 es la combinación de cinco factores:
1. el catálogo no tiene «más fácil» para principiantes;
2. la dosis está bloqueada (4D o margen 0);
3. la regla permite el lateral;
4. el prompt lo ofrece;
5. nada lo limita entre semanas.

**No es un defecto de implementación.**

### 6.1 ¿Hay historial suficiente para detectar la rotación?

**V8C** solo recibe la semana medida (`PreviousWeekSummary`) y la derivada. No sabe qué ejercicios de la semana medida entraron por una sustitución.

**La base guarda:**
- los planes de cada semana (`weekly_training_plans` y sus sesiones);
- los intentos de adaptación (`plan_adaptation_attempts`), con decisión, intervención, sesiones aplicadas y `conservation_report`.

Pero `conservation_report.sustituciones` solo registra las sustituciones **de seguridad** (dolor), no las de VARIANTE.

**Consecuencia:** detectar «este ejercicio entró la semana pasada por una sustitución lateral» exige comparar el plan medido con el anterior, mediante la correspondencia de sesiones que guarda la derivación. **Hoy ese dato no llega a V8C ni al prompt.**
- Por eso H12-D necesita un cambio de datos o de contexto (`07`).
- El laboratorio solo puede detectarla a posteriori, comparando planes.
