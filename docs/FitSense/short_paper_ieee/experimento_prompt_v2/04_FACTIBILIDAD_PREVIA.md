# Factibilidad previa de cada sesión señalada

**Implementación:** `FactibilidadAdaptacion` (`planning/infrastructure/generation/ai`). Solo la usa el prompt V2, es decir, con `fitsense.ai.adaptation.prompt-version=v2`.

**No es una regla ni un validador.** No decide, no filtra y no modifica nada: **describe**, para el modelo, lo que el backend ya sabe. El validador final (V8C + V1–V25) sigue siendo obligatorio y es el único que decide.

## 1. Qué calcula, por sesión señalada

| Dato | Cómo se obtiene (código existente, sin fórmulas nuevas) | Grado |
|---|---|---|
| Duración actual | `SessionDurationEstimator.estimateSeconds`, el mismo cálculo de V17/V20 | **Comprobado** |
| Mínimo | `PlanDraftValidator.minimumSessionSeconds`, el piso de V20 | **Comprobado** |
| Margen | Duración − mínimo. `null` si la intervención exime del mínimo (ACORTAR_O_REORGANIZAR) o si V20 no aplica | **Comprobado** |
| Qué permite la intervención | `TipoIntervencion`: sustituir, cambiar dosis, bajar carga. Es lo mismo que juzga V8C | Lectura de la regla |
| ¿Se puede bajar la dosis? | Sí, si la intervención lo permite y el objetivo **no** es GAIN_MUSCLE. En GAIN_MUSCLE, 4D no admite reducciones (`ProgresionDosisPolicy.admite4D` con REDUCCION → `false`) | Lectura de la regla |
| Opciones de dosis (una por línea) | Cada **cambio unitario** permitido se aplica sobre una **copia temporal** de la sesión y se vuelve a estimar con el estimador real:<br>• SETS_REPS: 1 repetición menos (sin bajar de `max(V9 = 6, RepLimits.minRepsFor(zona, nivel))`) y 1 serie menos (sin bajar de V9 = 2);<br>• DURATION: −10 s (sin bajar de V10 = 20 s) y 1 serie menos (sin bajar de 2) | **Comprobado** (segundos y [cabe]/[NO cabe] frente a V20) |
| Sustitutos | Los **mismos** que V1 imprime en «SUSTITUTOS ADMITIDOS»: mismo patrón, elegibles, sin los de la sesión, los de días contiguos ni los que dolieron, dificultad no mayor, hasta 6 | La prueba 15 comprueba que coinciden |
| ¿El sustituto admite la misma dosis? | **GAIN_MUSCLE:** la dosis 4D de su slot (`MvpDosePolicy.dosisPara`) coincide en series, repeticiones y descanso.<br>**Resto:** alguna de sus capacidades admisibles (`CapabilityPrescriptionPolicy.admisibles` → `dosis`) coincide en tipo, series, repeticiones o segundos, y descanso. Son las dosis que acepta V24 | Lectura de la regla. GUION: aceptado por el backend en U04, U09, U14 y U19 |
| Cargas reducibles | Ejercicios de la sesión con carga prescrita (`loadKg > 0`) en una intervención que permite bajarla | Lectura de la regla |
| ¿Hay algún cambio factible? | Alguna opción [cabe], algún sustituto de la misma dosis o alguna carga reducible | Derivado |

**Lo que no comprueba, y se declara en el propio prompt** («el validador final sigue decidiendo»):
- la combinación de varios cambios frente a V24/V25;
- la semana completa (V12, V18 y el resto de V1–V25);
- la adecuación clínica.

El texto de V2 dice que las opciones se pueden **sumar** mientras no pasen del margen. Es correcto porque el estimador es aditivo por ejercicio: calentamiento + Σ ejercicio + transiciones.

## 2. Valores de los 8 casos (los mismos que en la etapa C)

| Caso y sesión | Intervención | Duración / mínimo / margen | Opciones que caben solas (s) | Sustitutos de misma dosis | ¿Factible? |
|---|---|---|---|---|---|
| U03 S15 (GM) | REVISAR_CARGA | 2550 / 2520 / 30 | Ninguna: 4D no admite reducciones | — (no puede sustituir) | **No** → «omítela» |
| U04 S20 (GM) | VARIANTE | 2550 / 2520 / 30 | Ninguna de dosis | Sí | Sí |
| U04 S21 (GM) | INVESTIGAR | 2550 / 2520 / 30 | Ninguna | — | **No** → «omítela» |
| U08 S45 (FU) | REVISAR_CARGA | 2556 / 2520 / 36 | Repetición −1 en 323 y en 487: 9 cada una | — | Sí |
| U09 S50 (FU) | VARIANTE | 2556 / 2520 / 36 | Repetición −1 en los 3×10: 9 | Sí | Sí |
| U09 S51 (FU) | INVESTIGAR | 2556 / 2520 / 36 | Repetición −1 en 1284 y en 117: 9 | — | Sí |
| U13 S75 (PP) | REVISAR_CARGA | 2052 / 1890 / 162 | −10 s de cardio: 30; una serie de cardio menos: 150 | — | Sí |
| U14 S80 y S81 (PP) | VARIANTE e INVESTIGAR | 2052 / 1890 / 162 | Ídem | S80: sí | Sí |
| U18 S105 (AG) | REVISAR_CARGA | 2040 / 1890 / 150 | Repetición −1: 6; −10 s: 20–30; una serie de cardio menos: 150 (igual al margen) | — | Sí |
| U19 S110 y S111 (AG) | VARIANTE e INVESTIGAR | 2040 / 1890 / 150 | Ídem | S110: sí | Sí |

Los valores salen del prompt V2 real de cada caso (`evidencia/GUION_v2/prompts/*_i1.txt`) y coinciden con el diagnóstico (`01`–`03`).

## 3. Cómo se comprobó que V2 no llama válido a lo que el backend rechaza

1. **Unitarias:** `SessionFixPromptV2Test`, pruebas 11–14.
   - Cada opción [cabe] se aplica a una copia y pasa V20 en el `PlanDraftValidator` real.
   - Cada [NO cabe] la rechaza V20.
   - Esto se comprueba en las condiciones de U03, U04, U08, U09, U18 y U19.
   - Los segundos coinciden con el estimador y no se ofrece nada por debajo de los pisos.
2. **Integración sin red** (modo GUION del laboratorio, con la base efímera y los servicios reales; `evidencia/GUION_v2`). El «modelo» responde con lo que V2 describe:
   - **intento 1:** una opción [NO cabe] o, si V2 dice que no hay solución, una reducción prohibida;
   - **intento 2:** lo que V2 llama válido.

   **Resultado:**

   | Caso | Intento 1 | Intento 2 | V32 |
   |---|---|---|---|
   | U03 | Repetición −1 (prohibida en GM) → **rechazado por V24** | Omitida | SIN_EFECTO |
   | U04 | S21: repetición −1 → **V24**; S20: 2 sustitutos de la misma dosis | S20 igual; S21 omitida | APLICADA (S20) |
   | U08 | Serie −1 en 1106 ([NO cabe]) → **V20, 2388 s** | 323 y 487 → 3×9 | APLICADA |
   | U09 | Serie −1 en S50 y S51 → **V20 ×2** | Sustitutos + repetición −1 en las dos | APLICADA (S50 y S51) |
   | U13, U14, U18 y U19 | Solo lo válido | — | APLICADA (todas sus sesiones) |

   **7 de 8 aplicadas.** El máximo teórico de `03` era 7, porque U03 es F3. Ninguna afirmación de V2 contradijo a los validadores.
3. **Con los mismos guiones, V1 y V2 producen el mismo resultado** en la base (`decisiones`, `plan_siguiente` y `resultados_20` iguales; ver `06`). El backend no depende de la versión del prompt.
