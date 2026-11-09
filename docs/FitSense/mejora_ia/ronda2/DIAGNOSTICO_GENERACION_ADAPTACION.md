# Diagnóstico de la generación y la adaptación con IA (MEJ ronda 2, fase 1)

Este documento complementa `AUDITORIA_REGLAS_FITSENSE.md`. Responde a las preguntas concretas de la orden con datos y código, sin pagar ninguna predicción.

## 1. ¿El backend es correcto, o el problema está en lo que recibe Replicate?

**El backend es correcto en lo que decide.** No se publica nada que no pase sus validadores, y V24 no se relaja en la generación inicial. La IA falla sobre todo por **información que no recibe o que recibe contradictoria**:

| Rechazos de MEJ-C (PR-1.7, versión vigente) | Número | Causa |
|---|---:|---|
| V15 | 12 | `max_per_body_part` llega `null` con REV2: la regla de la mitad no viaja con su número |
| V26 | 8 | Los techos por grupo no están en PR-1.7 |
| V23 | 3 | En GF no hay ningún texto sobre los slots |

Con PR-1.8 (MEJ-G1), que añade los números de V15 y V26, esos hallazgos bajaron de 20 a 5. La única regresión fue la línea de dosis orientativa, que en GAIN_MUSCLE empujó al modelo a subir series fijas: 13 hallazgos V24 en una sola propuesta, resueltos en el reintento.

**Conclusión:** la parte de **información** se resuelve con el prompt (G1/G2/G3). La parte de **interpretación** (que el modelo aproveche el tiempo, que responda a la causa) es la que se mide con G3 y A2.

## 2. O10: dos ejercicios y seis series

| Pregunta | Respuesta |
|---|---|
| Objetivo y estructura | LOSE_WEIGHT, ADVANCED, casa, peso corporal, 1 sesión × 30 min (máximo 1800 s). Principal CARDIO (3×120 s, descanso 60 s); secundaria STRENGTH (estructural 3×6, descanso 150 s); máximo 8 colocaciones |
| ¿Cumplía la política? | **Sí.** V5 (≥ 2 ejercicios), V24 (dosis exactas), V25 (CARDIO en la sesión), V4 (1320 s ≤ 1800 s) y V26. V15 no aplica: los cardios son complementos y el mínimo de grupos queda en 0. REV2 no tiene mínimo de ejercicios ni de duración |
| ¿Defecto técnico u observación? | **Observación de calidad.** `CalidadDosis` la clasifica como INSUFICIENTE (< 3 ejercicios de trabajo o < 6 series), pero esa clase **solo se escribe en la traza**: ningún validador ni aviso la usa (`CalidadDosis` dice «No rechaza nada: informa») |
| ¿El oráculo usa otro criterio? | **No.** Su comprobación QINS (`len(ej) < 3 or series < 6`) es el mismo umbral de `CalidadDosis`. El oráculo lo marca como FALLO de calidad y el backend lo acepta por diseño. No se cambia el oráculo ni se añade un mínimo de ejercicios |
| ¿Se podía hacer mejor con las reglas actuales? | **Sí.** Dos cardios suman 300 + 2×480 + 60 = 1320 s. Una colocación de fuerza más cabe: ≈ 1734 s ≤ 1800 s. El motor de reglas la pone (29 min, 3 ejercicios). PR-1.7 dice «completa el tiempo con las secundarias», pero solo da costes por dosis en GAIN_MUSCLE, así que el modelo no puede saber que cabía |
| Qué se prueba | G3 (pr-1.8c) le da al modelo el coste en segundos de cada dosis (`CARDIO_1=480 s`, `STRENGTH_1≈354 s`) y el tiempo útil de la sesión, todo con datos que ya existen. O10 es caso diagnóstico, pero el criterio se mide en los cuatro objetivos |

## 3. Otros casos

- **O04 (oráculo):** artefacto del emparejamiento posicional del oráculo; V8C acepta el cambio por identidad (detalle en la auditoría §4). En la ronda 2 la vigilancia de cambios no autorizados se calcula por identidad.
- **O18:** retirar un relleno está permitido. V2 y V2.1 lo contradecían; V2.2 y v2.2a lo ofrecen. Con la versión vigente del modelo, V2.1 también acierta (MEJ-A0), así que el efecto esperado de A1 es la coherencia, no un cambio medible.
- **O12:** técnicamente válido; la política del dolor sigue pendiente de revisión profesional. No se añade ninguna restricción.
- **V24 en GAIN_MUSCLE con PR-1.8:** fue el prompt, no el validador. V24 lo rechazó como debía y no se publicó nada con series subidas. pr-1.8b lo reformula: la dosis de cada ejercicio no se toca y la dosis semanal se busca con la selección y el reparto.

## 4. Regresión (recuento real)

| Commit | Unitarias | Integración | Fallos |
|---|---|---|---|
| `42d9dc1` (sin las variantes de la ronda 2) | 1310 (0 fallos, 1 omitida) | 593 (1 fallo, 2 errores, 44 omitidas) | `CatalogoV36IT.semanaSiguiente`; `ProgresionIT.p02ProgresionSinElExcluido` y su versión REV2 |
| `dce7158` (con las variantes, apagadas) | **1316** (1310 + 6 nuevas; 0 fallos, 1 omitida) | 593 (los **mismos** 3) | Idénticos |

**Los 3 fallos dependen de la fecha y ya existían.** Las dos pruebas fijan su semana con `LocalDate.now()`. Con el mismo commit `42d9dc1` y `faketime`:

| Fecha simulada | CatalogoV36IT | ProgresionRev2IT |
|---|---|---|
| 05/11/2026 | 7/7 pasan | 24/24 pasan |
| 09/11/2026 | 1 fallo («1296 no está en available_exercises», plan sintético V35) | 1 error (`Optional.empty` en la semana 2) |

No están relacionadas con la ronda 2 ni con ninguna regla de planificación. Se documentan aquí y en `INCIDENCIAS_MEJ_R2.md` sin cambiar las pruebas, que quedan fuera del encargo. Recomendación aparte: fijar la fecha con el reloj de pruebas.

**La regresión cubre:**
- los cuatro objetivos, V1–V27, V24, V25 y V8C;
- PLAN-REV-2.0, la adaptación, la progresión, la recuperación y la reincorporación;
- las pruebas de las variantes: `MejoraIaPromptsTest`, 10 pruebas, que comprueban que PR-1.7, V2.1 y V2.2 no cambian.

**El referente anterior de 1310** era solo el número de pruebas unitarias.

## 5. Decisión

**Se puede pasar a la fase pagada.** Ningún defecto invalida la medición:
- los huecos de V8C (H-A1, H-A2) se vigilan por identidad;
- los fallos de regresión no tienen relación con lo que se mide.
