# Trazabilidad del ejercicio retirado (480)

**Escenario:** el de `REINCORPORACION_CON_IA_REAL.md`. Es la sesión del jueves de un plan GAIN_MUSCLE de 30 min y 2 días.

**Fuentes:**
- `evidencia/1_ia_real_GAIN_MUSCLE_30min_2dias/TRAZA_EJERCICIO_RETIRADO.csv`: una fila por ejercicio del jueves y semana, con lo registrado.
- `HISTORIAL_REINCORPORACION.csv`.

## 1. Antes de retirarse (semana 1, plan 1, sesión 2)

| Posición | Ejercicio | Dosis | Clase 4D | Registro |
|---:|---|---|---|---|
| 1 | 72 | 3×10 / 90 s | estructural | COMPLETED (30 repeticiones) |
| 2 | **480** | **2×10 / 90 s** | **relleno estándar (sin slot)** | PARTIAL (8 de 20) |
| 3 | 715 | 3×10 / 90 s | estructural | SKIPPED, falta de tiempo |
| 4 | 513 | 3×10 / 90 s | estructural | SKIPPED, falta de tiempo |

- **La sesión:** cerrada al 35 % (PARTIAL), por falta de tiempo, sin pregunta de M.
- **Volumen de la sesión:** 110. Es la base V_base del linaje.

## 2. La retirada (ciclo 1 → plan 2, sesión 4, V31 origen 2)

**Replicate (`4rckyzeapdrmw0d1j02atkdqf4`):** «Quité la colocación de relleno para ajustar la sesión a tu tiempo disponible sin reducir la dosis de los ejercicios estructurales».

**El jueves publicado:** 72, 715 y 513 a 3×10 / 90 s, con duración de 18 min, calculada por el backend.

**V32:**

| Campo | Valor |
|---|---|
| Decisión | ADAPT_LOCALIZED |
| Intervención | ACORTAR_O_REORGANIZAR |
| Estado | APLICADA |
| Sesiones aplicadas | la 2 |
| Llamadas (`ai_calls`) | 1 |

**V31:** la sesión 4 declara `source_planned_workout_id = 2`.

## 3. Mientras está ausente (semanas 2 a 10)

| Semana | Plan | Jueves | 480 | Paso del jueves en el ciclo |
|---:|---:|---|---|---|
| 2 | 2 | 72 3×10, 715 3×10, 513 3×10 | AUSENTE | — (mantenimiento) |
| 3 | 3 | igual | AUSENTE | — (el paso fue del lunes) |
| 4 | 4 | igual | AUSENTE | 72 → 3×11, con la reserva PENDIENTE |
| 5 | 5 | 72 3×11 | AUSENTE | — |
| 6 | 6 | 72 3×11 | AUSENTE | 72 → 3×12 |
| 7 | 7 | 72 3×12 | AUSENTE | — |
| 8 | 8 | 72 3×12 | AUSENTE | 715 → 3×11 |
| 9 | 9 | 72 3×12, 715 3×11, 513 3×10 | AUSENTE | — |
| 10 | 10 | igual | AUSENTE | **REINCORPORACION 480** |

**Cada semana:** todas sus sesiones se registran completas, la adherencia observada es del 100 % y la cobertura del 100 %.

**Sin restauración automática:** 480 no vuelve hasta que lo decide la regla de reincorporación.

## 4. Después de reincorporarse (semanas 11 y 12)

| Semana | Plan | Posición de 480 | Dosis | Registro |
|---:|---:|---:|---|---|
| 11 | 11 | 4 (al final) | **2×10 / 90 s** (la inicial) | **COMPLETED: 2 series, 20 repeticiones** |
| 12 | 12 | 4 | 2×10 / 90 s | publicada; el ciclo 11 la mantiene, sin duplicado y sin otro paso |

**V32 del ciclo 10:** paso `REINCORPORACION`, `exercise_id` 480, `antes` = null, `despues` = 2×10 / 90 s, volumen 99 → 119, base 110, techo 121, `estado_reserva` PENDIENTE.

## 5. Las 12 condiciones de la orden en el ciclo 10

| # | Condición | Evidencia |
|---:|---|---|
| 1 | Retirada legítima por ACORTAR | V32 APLICADA ACORTAR_O_REORGANIZAR, una sola retirada y nada añadido |
| 2 | Linaje que identifica el ejercicio | V31 de 2 a 4, a 6, … a 20, todas copias o pasos (`retirado()` en `ProgresionResolver`) |
| 3 | Progresión previa | 3 pasos de PROGRESION en el jueves después del acortamiento (1(a) exige al menos uno) |
| 4 | Adherencia | Las dos semanas del criterio, observada ≥ 80 % (fueron del 100 %) |
| 5 | Sin dolor ni fatiga | Ningún registro con PAIN o FATIGUE en S ni en O |
| 6 | Sigue permitido | 480 elegible y sin exclusión de seguridad |
| 7 | Duración suficiente | La sesión revalidada con V4 y V20 al publicarse |
| 8 | Límite M | **No hubo M** (sesión parcial). Con M, `conMNoSeReincorpora` prueba que no vuelve |
| 9 | Volumen autorizado | 99 + 20 = 119 ≤ 121 (1,10 × 110) |
| 10 | Máximo de colocaciones | 4 de 7 |
| 11 | Dosis inicial | 2×10 / 90 s, la RELLENO_ESTANDAR de 4D (`exacta`, D-P8) |
| 12 | Sin progresión en el mismo ciclo | `progressed_sessions` con una sola sesión y un solo paso, y la consulta de linaje sin violaciones |
