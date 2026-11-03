# Diagnóstico de las ocho adaptaciones originales (etapa C, PR #28)

**Fuente única:** la evidencia ya publicada de la etapa C, intento 3 (`experimento_final_20_usuarios/evidencia/C`):
- `predicciones_replicate/*.json`: el prompt exacto enviado y la respuesta del modelo;
- `ejecucion_maven.log`: el texto literal de cada rechazo;
- `decisiones.csv`: el estado V32.

**Nada se ha vuelto a ejecutar.** El experimento original no se ha modificado.

**Cálculo de duraciones.** Se replicó `SessionDurationEstimator` con los parámetros activos (`MVP-1.2`, V14):
- 3 s por repetición;
- 60 s de transición entre ejercicios;
- 5 min de calentamiento;
- descanso = (series − 1) × `rest_seconds`.

**Comprobación:** las 7 duraciones de rechazo que da el backend (2184, 1596, 1644, 2124, 2220, 1720 y 1840 s) se reproducen **exactas** con esa réplica. El script está en `evidencia/diagnostico/factib.py` y su salida en `factib.txt`.

**Mínimo V20:** `ceil(minutos × 60 × 70 / 100)`. La comparación es `segundos < mínimo`, así que **igualar el mínimo es válido**.

## Resumen

| Caso | Objetivo, nivel y lugar | Sesión: intervención | Duración / mínimo / margen | Intentos | Rechazos | V32 |
|---|---|---|---|---:|---|---|
| U03 | GAIN_MUSCLE, intermedio, gimnasio, 60 min | S15: REVISAR_CARGA | 2550 / 2520 / **30** | 1 | — | SIN_EFECTO |
| U04 | GAIN_MUSCLE, intermedio, gimnasio, 60 min | S20: VARIANTE<br>S21: INVESTIGAR | 2550 / 2520 / 30 (las dos) | 1 | — | APLICADA (S20) |
| U08 | INCREASE_STRENGTH, intermedio, gimnasio, 60 min | S45: REVISAR_CARGA | 2556 / 2520 / **36** | 2 | i1: V20 | SIN_EFECTO |
| U09 | INCREASE_STRENGTH, intermedio, gimnasio, 60 min | S50: VARIANTE<br>S51: INVESTIGAR | 2556 / 2520 / **36** (las dos) | 3 | i1: V20 ×2<br>i2: V20 ×2 | APLICADA (S50) |
| U13 | LOSE_WEIGHT, principiante, casa, 45 min | S75: REVISAR_CARGA | 2052 / 1890 / 162 | 1 | — | APLICADA |
| U14 | LOSE_WEIGHT, principiante, casa, 45 min | S80: VARIANTE<br>S81: INVESTIGAR | 2052 / 1890 / 162 (las dos) | 1 | — | APLICADA (S80 y S81) |
| U18 | GENERAL_FITNESS, principiante, casa, 45 min | S105: REVISAR_CARGA | 2040 / 1890 / **150** | 2 | i1: V20 | SIN_EFECTO |
| U19 | GENERAL_FITNESS, principiante, casa, 45 min | S110: VARIANTE<br>S111: INVESTIGAR | 2040 / 1890 / 150 (las dos) | 2 | i1: V20 ×2 | SIN_EFECTO |

**Totales:**
- 13 llamadas y 13 propuestas;
- 5 propuestas rechazadas, todas por V20 (7 mensajes V20, uno por sesión);
- 8 propuestas aceptadas: 4 con cambio efectivo (APLICADA) y 4 sin cambio efectivo (SIN_EFECTO).

## Lo que permite cada intervención (código vigente)

| Intervención | Sustituir | Retirar | Cambiar dosis | Carga | Fuente |
|---|---|---|---|---|---|
| REVISAR_CARGA_Y_RECUPERACION | No | No | Solo bajar (V8C, sin subir nada) | Bajar si había carga prescrita | `TipoIntervencion`, `ConservationValidator.revisarDosisDeLaIa` y `revisarCarga` |
| VARIANTE_MAS_SENCILLA_O_CARGA | Sí, mismo patrón (V8C) | Solo dentro de una sustitución | Solo bajar | Bajar si había carga prescrita | Ídem |
| INVESTIGAR_CAMBIO_MINIMO | No | No | Solo bajar | No | Ídem |

**Límites comunes:**
- el descanso no cambia;
- mínimo 2 series (V9);
- repeticiones ≥ `min_reps` (V16/V24);
- duración ≥ 20 s.

**En GAIN_MUSCLE la dosis 4D no admite reducciones.** `ProgresionDosisPolicy.admite4D` devuelve `false` para `Origen.REDUCCION`, y V24 exige la dosis exacta.

**Carga:** ninguna de las 8 sesiones tenía carga prescrita (`loadKg` nulo en la semana medida), así que bajar la carga no era posible: V8C rechaza poner una carga donde no la había.

## Ficha por caso (15 elementos)

### U03 · S15 · GAIN_MUSCLE · REVISAR_CARGA_Y_RECUPERACION

| # | Elemento | Valor |
|---|---|---|
| 1 | Plan | Motor de reglas, 3 sesiones FULL_BODY de 7 ejercicios, 3×10 con 90 s |
| 2 | Sesiones señaladas | S15 (era el 06/11, ahora 13/11) |
| 3 | Motivo e intervención | FATIGUE → REVISAR_CARGA_Y_RECUPERACION |
| 4 | Ejercicios y dosis | 380, 351, 637, 949, 1073, 496 y 436, todos con 3×10 y 90 s |
| 5 | Duración actual | 2550 s |
| 6 | Mínimo | 2520 s |
| 7 | Margen | 30 s |
| 8 | Prompt | 192.986 caracteres; 54.721 *tokens* de entrada |
| 9 | Propuestas | i1: la sesión devuelta **idéntica** y con notas («Si estás fatigado…») |
| 10 | Validaciones | Aprobada |
| 11 | Rechazos | — |
| 12 | Retroalimentación | — |
| 13 | Propuesta aceptada | i1 |
| 14 | Cambios efectivos | Ninguno |
| 15 | V32 | SIN_EFECTO (CONSERVED) |

### U04 · S20 (VARIANTE) y S21 (INVESTIGAR) · GAIN_MUSCLE

| # | Elemento | Valor |
|---|---|---|
| 1 | Plan | Motor de reglas, 3×10 con 90 s |
| 2 | Sesiones señaladas | S20 (13/11 → 11/11) y S21 (13/11) |
| 3 | Motivo e intervención | **S20:** TOO_DIFFICULT → VARIANTE.<br>**S21:** LACK_OF_MOTIVATION → INVESTIGAR |
| 4 | Ejercicios y dosis | **S20:** 380, 389, 360, 779, 1236, 580 y 392 (3×10, 90 s).<br>**S21:** 588, 1001, 142, 1154, 1042, 580 y 77 (3×10, 90 s) |
| 5–7 | Duración / mínimo / margen | 2550 / 2520 / 30 en las dos |
| 8 | Prompt | 194.831 caracteres; 55.347 *tokens* |
| 9 | Propuestas | **S20:** los 7 ejercicios sustituidos por 72, 77, 148, 101, 144, 102 y 75, con la misma dosis.<br>**S21:** omitida |
| 10–12 | Validación / rechazos / retroalimentación | Aprobada / — / — |
| 13 | Aceptada | i1 |
| 14 | Cambios efectivos | **S20:** 7 sustituciones (duración igual, 2550 s).<br>**S21:** ninguno |
| 15 | V32 | APLICADA (S20 aplicada, S21 sin efecto) |

### U08 · S45 · INCREASE_STRENGTH · REVISAR_CARGA_Y_RECUPERACION

| # | Elemento | Valor |
|---|---|---|
| 1 | Plan | Motor de reglas: 4 ejercicios de fuerza (3×6, 150 s) y 2 de hipertrofia (3×10, 90 s) |
| 2–3 | Sesión y motivo | S45 (13/11), FATIGUE |
| 4 | Ejercicios | 1106, 994, 1265 y 1154 (3×6, 150 s); 323 y 487 (3×10, 90 s) |
| 5–7 | Duración / mínimo / margen | 2556 / 2520 / **36** |
| 8 | Prompt | 189.083 caracteres; 55.873 *tokens* |
| 9 | Propuestas | **i1:** 1106 y 994 de 3×6 a 2×6, y 323 y 487 de 3×10 a 3×8.<br>**i2:** sesión omitida |
| 10 | Validaciones | **i1:** rechazada.<br>**i2:** aprobada |
| 11 | Rechazos | **i1:** «V20: … dura unos 2184 s y el mínimo es 2520 s: le faltan 336 s» |
| 12 | Retroalimentación | El texto de V20 («Agrega un ejercicio del enfoque o una serie…»), más la pista «si no puedes llegar al mínimo con lo que la intervención permite, OMÍTELA».<br>La IA respondió: «no puedes aumentar series ni añadir ejercicios según las reglas, y reducir la dosis bajaría aún más la duración» → omitió |
| 13 | Aceptada | i2 (vacía) |
| 14 | Cambios efectivos | Ninguno |
| 15 | V32 | SIN_EFECTO (CONSERVED) |

### U09 · S50 (VARIANTE) y S51 (INVESTIGAR) · INCREASE_STRENGTH

| # | Elemento | Valor |
|---|---|---|
| 1 | Plan | Motor de reglas, fuerza 3×6 con 150 s e hipertrofia 3×10 con 90 s |
| 2–3 | Sesiones y motivos | **S50:** TOO_DIFFICULT → VARIANTE.<br>**S51:** LACK_OF_MOTIVATION → INVESTIGAR |
| 4 | Ejercicios | **S50:** 540, 775, 1315 y 780 (3×6); 380 y 1039 (3×10).<br>**S51:** 73, 1228, 1196 y 705 (3×6); 1284 y 117 (3×10) |
| 5–7 | Duración / mínimo / margen | 2556 / 2520 / **36** en las dos (**confirmado**) |
| 8 | Prompt | 190.805 caracteres; 56.458 *tokens*.<br>El de S50 trae la lista de SUSTITUTOS ADMITIDOS |
| 9 | Propuestas | **i1:**<br>• S50: 2 sustituciones y 4 series menos (retira 960 s);<br>• S51: series −1 en los 6 (retira 912 s).<br>**i2:**<br>• S50: 6 sustituciones con su dosis de catálogo (retira 432 s);<br>• S51: series −1 en 73 y 1196 (retira 336 s).<br>**i3:**<br>• S50: las 6 sustituciones con la duración original;<br>• S51: devuelta sin cambios |
| 10 | Validaciones | i1: rechazada. i2: rechazada. i3: aprobada |
| 11 | Rechazos | **i1:** V20, 1596 / 2520 (11/11) y 1644 / 2520 (13/11).<br>**i2:** V20, 2124 / 2520 y 2220 / 2520 |
| 12 | Retroalimentación | La misma pista de V20 en los dos reintentos |
| 13 | Aceptada | i3 |
| 14 | Cambios efectivos | **S50:** 6 sustituciones del mismo patrón y músculo (H10 de PR #28).<br>**S51:** ninguno |
| 15 | V32 | APLICADA (S50 aplicada, S51 sin efecto) |

### U13 · S75 · LOSE_WEIGHT · REVISAR_CARGA_Y_RECUPERACION

| # | Elemento | Valor |
|---|---|---|
| 1 | Plan | Motor de reglas, casa: 4 ejercicios de 2×8 con 120 s y 2 de cardio de 3×90 s con 60 s |
| 2–3 | Sesión y motivo | S75 (13/11), FATIGUE |
| 4 | Ejercicios | 1113, 116, 1059 y 704 (2×8, `min_reps` 8: no se pueden bajar); 1117 y 853 (3×90 s) |
| 5–7 | Duración / mínimo / margen | 2052 / 1890 / 162 |
| 8 | Prompt | 30.958 caracteres; 8.950 *tokens* |
| 9 | Propuestas | i1: 1117 de 3×90 s a 2×90 s (retira 150 s) |
| 10–12 | Validación / rechazos / retroalimentación | Aprobada / — / — |
| 13–14 | Aceptada / cambios | i1; una serie menos en 1117 (duración 1902 s) |
| 15 | V32 | APLICADA |

### U14 · S80 (VARIANTE) y S81 (INVESTIGAR) · LOSE_WEIGHT

| # | Elemento | Valor |
|---|---|---|
| 1–7 | Plan, sesiones, motivos, ejercicios y margen | El mismo plan tipo que U13, con margen de 162 s en las dos sesiones |
| 8 | Prompt | 32.701 caracteres; 9.536 *tokens* |
| 9 | Propuestas | **S80:** 1117 de 3×90 s a 2×90 s.<br>**S81:** 1093 de 3×90 s a 2×90 s |
| 10–14 | Validación y cambios | Aprobada en i1; una serie menos de cardio en cada sesión (1902 s) |
| 15 | V32 | APLICADA (S80 y S81) |

### U18 · S105 · GENERAL_FITNESS · REVISAR_CARGA_Y_RECUPERACION

| # | Elemento | Valor |
|---|---|---|
| 1 | Plan | Motor de reglas, casa: 2×10 (90 s), 2×15 (45 s), cardio 3×90 s y movilidad 2×30 s |
| 2–3 | Sesión y motivo | S105 (13/11), FATIGUE |
| 4 | Ejercicios | 101, 1097 y 775 (2×10); 1334, 1308 y 1076 (2×15); 1054 (3×90 s); 1188 (2×30 s) |
| 5–7 | Duración / mínimo / margen | 2040 / 1890 / **150** |
| 8 | Prompt | 71.764 caracteres; 21.270 *tokens* |
| 9 | Propuestas | **i1:** bajó las repeticiones de los 6 ejercicios, 1054 de 3×90 s a 2×60 s y 1188 de 2×30 s a 2×20 s (retira 320 s).<br>**i2:** omitida |
| 10–11 | Validación y rechazos | **i1:** V20, 1720 / 1890 («le faltan 170 s»).<br>**i2:** aprobada |
| 12 | Retroalimentación | La misma pista de V20 → la IA omitió |
| 13–15 | Aceptada / cambios / V32 | i2 / ninguno / **SIN_EFECTO** |

### U19 · S110 (VARIANTE) y S111 (INVESTIGAR) · GENERAL_FITNESS

| # | Elemento | Valor |
|---|---|---|
| 1–7 | Plan, sesiones, motivos, ejercicios y margen | El mismo plan tipo que U18, con margen de 150 s en las dos sesiones |
| 8 | Prompt | 73.777 caracteres; 21.952 *tokens* |
| 9 | Propuestas | **i1:**<br>• S110: 2 sustituciones y reducciones (retira 200 s);<br>• S111: reducciones en los 8 ejercicios (retira 200 s).<br>**i2:** las dos omitidas («para alcanzar el mínimo habría que aumentar series/segundos o añadir ejercicio… no permiten subir volumen») |
| 10–11 | Validación y rechazos | **i1:** V20, 1840 / 1890 en las dos sesiones («le faltan 50 s»).<br>**i2:** aprobada |
| 13–15 | Aceptada / cambios / V32 | i2 / ninguno / **SIN_EFECTO** |

## Lectura conjunta

1. **El modelo sí respeta los permisos.**
   - En las 13 propuestas no hubo ningún rechazo por V8C (sustituir donde no se puede, subir la dosis, cambiar el día), V24, V1 ni V6.
   - **Todos** los rechazos fueron de duración (V20).
2. **El modelo no calcula la duración.**
   - El prompt da la duración actual y el mínimo, pero no cuánto retira cada cambio.
   - Con márgenes de 30–36 s (GM y FU), una sola serie menos ya retira 120–168 s.
3. **El reintento empuja a omitir.**
   - El mensaje de V20 está escrito para la generación («Agrega un ejercicio… o una serie»).
   - En una sesión que solo puede bajar la dosis, el modelo lo lee como una orden imposible: no puede subir. La pista de reintento añade «OMÍTELA».
   - En los 3 casos con un intento rechazado y otro aceptado sin cambios (U08, U18 y U19), el modelo **omitió**. No probó a reducir menos, que habría sido válido (ver `02`).
   - En U09, el modelo no omitió S50, pero sus reintentos siguieron retirando de más hasta el tercero.
4. **Hay sesiones sin ninguna modificación posible** con las reglas vigentes: U03 S15 y U04 S21 (ver `03`).
