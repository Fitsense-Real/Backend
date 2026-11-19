# E · Decisiones deportivas pendientes (no se aplica ninguna sin tu aprobación)

**Cifras:** salen de los barridos contrafactuales (`C`), en REV2 / HIST.

## E1 · P15: ¿puede un ejercicio aislado acreditar por sí solo un patrón principal?

**Hoy (A):**
- 13 ejercicios con `is_isolation=true` y patrón `KNEE_DOMINANT`: 11 curl femorales, 1 extensión de piernas y 1 curl de pie.
- Cubren el patrón de rodilla (el de sentadilla o zancada) igual que un multiarticular.
- `is_isolation` no llega a planning: el motor no puede distinguirlos.

**Alternativas medidas:**

| | A · Actual | B · Los aislados no cubren el patrón ni reciben dosis de fuerza (simulado con datos, CF2) | C · No cubren el patrón, pero conservan su dosis completa |
|---|---|---|---|
| Planes válidos REV2 / HIST | 3.776 / 3.641 | **3.776 / 3.641 (0 perdidos)** | Igual que B en la cobertura (misma regla de patrón) |
| Rodilla cubierta solo por aislados (REV2) | 324 | **0** | 0 |
| Planes sin patrón de rodilla (REV2, LOSE_WEIGHT) | 94 | 99 (+5) | Igual que B |
| Planes con algún aislado de rodilla (REV2 / HIST) | 984 / 1.445 | 176 / 145 | Más que B (conservan la fuerza) |
| DOSIS_LIMITADA (REV2) | 1.898 | 1.899 | ≈ igual |
| Sustitutos que más suben (REV2) | — | Sissy squat (+105), sentadilla con alcance (+69), marcha en sentadilla isométrica contra la pared (+65), zancada caminando (+62), sentadilla cruzada (+43) | — |
| Principiantes con alto impacto o dificultad ≥ 3 | 0 | 0 | — |
| Cambio de código | — | Ninguno: basta con datos (un patrón no estructural para los aislados) | `is_isolation` tiene que llegar a `CandidateExercise`, y `MandatorySlot.of` tiene que excluirlos, sin tocar su clase de dosis |

**Riesgos de B y C:**
- El sustituto más frecuente, el **sissy squat (1205)**, figura con **dificultad 1** y recibiría 62 planes más de principiante. Su dificultad también debe revisarse (E3).
- «March sit (wall)» (1076) como patrón de rodilla también merece revisión.

**Recomendación: C** (no B) si un profesional confirma que un aislado no sustituye al patrón:
- separa lo que ya hoy está mezclado: cobertura de patrón por un lado, contribución de dosis muscular por otro;
- no quita a los curl femorales la dosis de isquios que sí aportan;
- B consigue el mismo efecto de cobertura sin código, pero penaliza su dosis de fuerza.

**En ambos casos, antes de aplicarlo:** decidir E3 (dificultad del sissy squat y del curl nórdico), para no cambiar un problema por otro.

## E2 · Ejercicios de tronco contados como dosis de un grupo (775, 973, 1261, 1308, 1259, 1146)

**Medido:**

| Variante | REV2 | HIST |
|---|---|---|
| CF1: 775 y 973 sin dosis | 0 planes perdidos. DOSIS_LIMITADA 1.898 → 2.340. **Sin dosis de hombro: GAIN_MUSCLE 29 → 332, INCREASE_STRENGTH 16 → 327, GENERAL_FITNESS 57 → 353.** La espalda se desplaza a 1261 (+302), 1308 (+291) y 1259 (+225), igual de dudosos | **110 planes válidos se pierden** (V20 de 139 → 247): HIST usa 775 y 973 para alcanzar el piso de duración |
| CF1b: los 6 sin dosis | 0 planes perdidos. La espalda se desplaza a 1066 «lower back curl» (extensión de tronco, +659) | **146 planes se pierden** |

**Hallazgo de fondo: en casa sin material no existe ningún ejercicio de hombro con dosis**, salvo el 973. Hay un estiramiento, y el handstand push-up (dificultad 3, clasificado como brazos). Quitar la dosis al 973 no deja «otro hombro»: deja los planes **sin hombro** (1.260 de casa sin material, frente a 346 hoy).

**Opciones:**

| Opción | Qué supone |
|---|---|
| E2-a | Mantener el estado actual, pero declarar en la API que la dosis de hombro y espalda en casa sin material es aproximada (DOSE) |
| E2-b | Reclasificar (sin dosis) y aceptar DOSIS_LIMITADA de hombro en casa sin material. **Solo en REV2:** en HIST rompe 110–146 planes |
| E2-c | Añadir al catálogo ejercicios reales de hombro sin material (por ejemplo, flexión en pica). Requiere contenido nuevo con revisión profesional |
| E2-d | Regla de dosis por patrón coherente con el grupo (cambio en `CalidadDosis`). Sistémico; afecta a la clasificación de dosis de muchos planes |

**Recomendación:** E2-c + E2-b, solo en REV2, cuando E2-c exista. Mientras tanto, E2-a. **No aplicar E2-b a HIST.**

## E3 · Dificultad

| Ejercicios | Dificultad actual | Hallazgo | Planes de principiante (REV2 / HIST) | Propuesta |
|---|---|---|---|---|
| Curl nórdico: 1195, 1196, 1197, 327, 915, 916 | 1 | Excéntrico exigente; instrucciones de otro movimiento; anclaje no declarado | **289 / 468** | Dificultad 3 (o no para principiantes), con instrucciones correctas y requisito de anclaje |
| 1205 sissy squat | 1 | Ejercicio exigente para la rodilla | REV2: 62 más con P15 | Revisar |
| 1262, 1115 | 1 | Flexión con gesto añadido | — | 2 |
| Toda la dificultad 1 del catálogo | — | Sale de una regla de palabras clave (V10); solo hay 4 aprobaciones humanas | — | Revisión profesional por muestreo |

**Esto afecta también a HIST, la política por defecto:**
- **468 planes de principiante incluyen hoy un curl nórdico.**
- Es el hallazgo de más riesgo para usuarios reales y no depende de REV2.

## E4 · Movimientos que hay que identificar (revisión visual humana)

| Ejercicio | Lo que hay que decidir |
|---|---|
| 853 | ¿Media sentadilla o marcha? Fija el nombre y las instrucciones, y si sigue siendo cardio |
| 580 | ¿Press o apertura? Si es apertura: `HORIZONTAL_ADDUCTION`, aislado, sin dosis de fuerza |
| 775 | ¿Qué es realmente (extensión de tronco en banco, movilidad)? ¿Requiere banco? |
| 1093 | Movimiento real y nombre |
| 1195 y 1196 | ¿Requisito de anclaje? |

## E5 · Ejercicios sin equivalente fiable

Sin equivalente fiable en casa sin material:
- **hombro con dosis:** no hay ninguno (E2);
- **isquios multiarticular sin anclaje:** el curl nórdico necesita anclaje.

**Recomendación:**
- No desactivar ninguno hasta que exista el sustituto.
- Mientras tanto, la limitación debe declararse (DOSE/SLOT en `/plan/limitations`), no ocultarse.

## E6 · Asiento no modelado (S1, sistémico)

78 ejercicios usados piden sentarse en un banco o silla, que el equipamiento no declara.

**Decisión de producto:**
- o se asume que siempre hay una silla;
- o se añade «silla» al perfil de equipamiento.

## E7 · Instrucciones nuevas (texto)

Proponer textos para 1320 y para el curl nórdico (`B` §1) requiere tu revisión o la de un profesional antes de escribirlos en la base de datos.
