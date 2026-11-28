# 01 · Diagnóstico de las adaptaciones (12 sesiones, 8 usuarios)

**Fuentes:**
- `03_FACTIBILIDAD_12_SESIONES.csv`, generado a partir de la evidencia;
- la sonda de factibilidad y la coherencia V2, en `evidencia/desarrollo/DEV3_S21_U`, sobre el backend de la candidata (H1, límite de dificultad C, retirada, P3);
- el histórico de V1 (etapa C, PR #28) y de V2 (C1/C2, `experimento_prompt_v2`).

**No se ha hecho ninguna llamada pagada.**

## Respuestas por sesión

| Sesión | 1. Qué necesita | 2. Qué autoriza | 3. ¿Hay modificación válida? | 4. Restricciones | 5. Alternativas válidas | 6. Por qué se rechazó antes (V1) | 7. ¿Prompt? | 8. ¿Política? | 9. ¿Restricción legítima? | 10. Mejora sin romper reglas |
|---|---|---|---|---|---|---|---|---|---|---|
| U03 S3 (GM, fatiga) | Bajar el trabajo | Solo dosis | **No** (0/21) | 4D/V24 fija 3×10 con 90 s; margen V20 30 s y una serie menos retira ≥ 120 s; sin carga | Ninguna | No hubo rechazo: SIN_EFECTO | No | Sí: la intervención solo permite dosis y 4D la fija | **Sí** | Ninguna sin cambiar 4D, V20 o la intervención (`04`, C-5) |
| U04 S2 (GM, difícil) | Más fácil | Sustituir y bajar dosis | Sí (259/307) | 4D fija la dosis: solo sustitutos con la misma dosis; dificultad no mayor | 109 más fáciles; 150 laterales | — (APLICADA) | No | No | — | V2.1: preferir los de menor dificultad del mismo músculo |
| U04 S3 (GM, motivación) | Cambio mínimo | Solo dosis | **No** (0/21) | Igual que U03 | Ninguna | — | No | Sí | **Sí** | Igual que U03 |
| U08 S3 (FU, fatiga) | Bajar el trabajo | Solo dosis | Sí (4/18) | Margen 36 s: solo −1/−2 repeticiones en 323/487; las series no caben; V9 | 4 reducciones de repeticiones | **V20**: V1 retiraba más que el margen | **Sí (V1)** | No | — | V2: margen y coste de cada cambio (en C2: 0 rechazos) |
| U09 S2 (FU, difícil) | Más fácil | Sustituir y bajar dosis | Sí (197/264) | Margen 36 s; límite de dificultad | 78 más fáciles; 115 laterales | **V20** ×2 | **Sí (V1)** | No | — | V2 + V2.1 |
| U09 S3 (FU, motivación) | Cambio mínimo | Solo dosis | Sí (4/18) | Margen 36 s | 4 reducciones de repeticiones | **V20** ×2 | **Sí (V1)** | No | — | V2 + V2.1 (una sola opción) |
| U13 S3 (LW, fatiga) | Bajar el trabajo | Solo dosis | Sí (6/18) | Margen 162 s; V16 | 2 series y 4 segundos | — (APLICADA) | No | No | — | — |
| U14 S2 (LW principiante, difícil) | Más fácil | Sustituir y bajar dosis | Sí (55/67) | **No hay nada más fácil** (dificultad 1) | 49 laterales y reducciones de dosis | — (APLICADA, con laterales) | **Sí, de pertinencia**: un lateral no la hace más fácil | No | — | V2.1: «no hay variante más fácil: baja la dosis» |
| U14 S3 (LW, motivación) | Cambio mínimo | Solo dosis | Sí (6/18) | Margen 162 s | Reducciones | — | No | No | — | V2.1: una sola opción |
| U18 S3 (GF, fatiga) | Bajar el trabajo | Solo dosis | Sí (17/24) | Margen 150 s; V9 | 12 de repeticiones, 4 de segundos, 1 de series | **V20** | **Sí (V1)** | No | — | V2 |
| U19 S2 (GF principiante, difícil) | Más fácil | Sustituir y bajar dosis | Sí (77/84) | Nada más fácil | 60 laterales y reducciones | **V20** ×2 | **Sí (V1) y de pertinencia** | No | — | V2 + V2.1 |
| U19 S3 (GF, motivación) | Cambio mínimo | Solo dosis | Sí (17/24) | Margen 150 s | Reducciones | **V20** | **Sí (V1)**. **V2: pertinencia**, porque una corrida cambió 8 ejercicios | No | — | **V2.1: una sola opción sobre un ejercicio** |

## Causas concretas y corregibles

| # | Causa | Sesiones afectadas | Corrección | Tipo |
|---|---|---|---|---|
| K1 | **V1 no da al modelo ni el margen de V20 ni el coste en segundos de cada cambio**: el modelo retira de más y V20 rechaza | U08, U09 (×2), U18, U19 (×2) | Bloque de factibilidad de V2 (margen, opciones [cabe] o [NO cabe], pisos y sustitutos con la misma dosis). En C2: **0 rechazos y 10/10 cambiadas en las 2 corridas** | Prompt |
| K2 | Tras un rechazo de V20, el mensaje («añade un ejercicio o una serie») está pensado para la generación y **no se puede cumplir** en una corrección que solo puede bajar | Mismas | Pista de reintento de V20 de V2 | Prompt |
| K3 | El cambio mínimo no está acotado: V2 invita a «combinar cambios» y el modelo puede tocar 8 ejercicios | U19 S3 (V2 r2); riesgo en U09, U14 | V2.1: «una sola opción sobre un solo ejercicio» | Prompt (no es una regla nueva; ver `04` C-4 para hacerlo regla) |
| K4 | «Demasiado difícil» sin dificultad registrada en la lista y sin aviso de que no hay nada más fácil: el modelo elige laterales (ALT, texto «no necesariamente más fácil») | U14 S2, U19 S2 (principiantes); U04/U09 si eligen laterales | V2.1: sustitutos de menor dificultad **del mismo músculo** o, si no hay, bajar la dosis | Prompt |
| K5 | Sustituciones que cambian el músculo objetivo (P4 del PR #28): V8C solo exige el patrón | Posible en U04, U09, U14, U19 | V2.1 lo indica; el oráculo v2 las clasifica como ALT y no como ACC | Prompt y criterio |
| K6 | **U03 S3 y U04 S3 son imposibles con las reglas vigentes** (demostrado, no solo para cambios simples) | U03 S3, U04 S3 | Ninguna sin tu decisión (`04`, C-5) | Restricción legítima |

## U03 S3 y U04 S3: imposibilidad demostrada

**El razonamiento no se limita a cambios simples:**
1. La intervención (fatiga o motivación) **no permite sustituir ni retirar**. Solo permite bajar series, repeticiones, segundos o carga. El descanso no cambia.
2. En GAIN_MUSCLE, 4D/V24 exige la dosis exacta del slot (3×10, 90 s) en **cada** ejercicio. Cualquier cambio de repeticiones o series en cualquier ejercicio incumple V24 por sí solo, sea cual sea el resto de la propuesta. La sonda: 14/14 cambios de repeticiones rechazados por V24.
3. Aunque se ignorara 4D, quitar una serie retira al menos 3 s × 10 repeticiones + 90 s de descanso = **120 s**, y el margen sobre V20 es de **30 s**. Ninguna combinación de quitas de series cabe; 7/7 rechazadas por V20.
4. No hay carga prescrita que bajar (gimnasio sin `target_load_kg`), y las sesiones no tienen ejercicios por tiempo.

Por tanto, **el conjunto de modificaciones autorizadas y válidas es vacío**:
- conservar es lo correcto (**CONS-J**);
- no cuenta como adaptación de la IA;
- V2 lo comunica al modelo («F: OMÍTELA»): en C2, omitidas 2/2 con una sola llamada.

**Lo que lo cambiaría**, todo pendiente de tu decisión y sin implementar (`04`, C-5):
- admitir en 4D una reducción ante fatiga;
- eximir de V20 a las sesiones de fatiga, como ya se hace con la falta de tiempo;
- permitir sustituir con fatiga.

**No lo recomiendo** para el artículo: se debilitarían reglas de dosis o de duración solo para subir la tasa.
