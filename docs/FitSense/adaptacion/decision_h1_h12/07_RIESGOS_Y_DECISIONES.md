# 07 · Riesgos y decisiones pendientes

## 1. Estado de cada línea

| Línea | Estado | Rama / PR | Activo por defecto |
|---|---|---|---|
| **H1** multiconjunto | **Candidata lista** (`01`, `03`) | `candidate/h1-multiconjunto` (PR borrador nuevo, base #31) | **Sí, solo en la rama candidata**. En `generation` sigue `conjunto` hasta que fusiones |
| Textos de justificación | Implementados (solo presentación) | `feature/justificacion-adaptacion-verificable` (PR borrador nuevo, base la candidata) | Sí, en esa rama |
| Cambios de prompt y de `message_shown` | **Propuesta pendiente** (`04` §3) | — | No |
| **H12-A** «reducción demostrable» | **No aprobada**. Instrumento de revisión profesional (`05`, `05b`) | #33 | **No** |
| H12-B/C/D | Apagadas | #33 | No |
| V2 (prompt con factibilidad previa) | Apagado | #29 | No |
| Catálogo 580/775/780/862 | Análisis abierto; fichas (`06`) | #32 | Sin cambios |

## 2. Riesgos

| # | Riesgo | Probabilidad / impacto | Mitigación |
|---|---|---|---|
| R1 | Con H1, una VARIANTE que hoy se publicaría (2 añadidos de un patrón por 1 retirado) pasa a reintento o conservación: menos adaptaciones aplicadas | Baja: 0 de 123 casos reales. Impacto bajo | Es el cambio buscado. Reversión inmediata con `FITSENSE_V8C_PATTERN_RULE=conjunto` (`01` §3) |
| R2 | Error de configuración: alguien fija `conjunto` sin saberlo | Baja | El arranque registra un `WARN` explícito con la regla histórica activa |
| R3 | La IT permanente depende de encontrar un participante generado con la forma adecuada (busca hasta 6) | Baja: los perfiles son deterministas | Si un cambio de catálogo o de generación la dejara sin caso, falla con un mensaje explícito, nunca en silencio |
| R4 | Los textos nuevos describen el cambio con la dificultad del catálogo. Si el catálogo está mal clasificado (por ejemplo 862 con dificultad 1, o 780 con 2), el texto hereda el error | Media en casos concretos (`06`) | El texto nunca dice más que el catálogo. La revisión del catálogo lo corrige en origen |
| R5 | El texto ya no promete «bajamos la exigencia». Una persona que esperaba un cambio puede percibir que «no se hizo nada» cuando hubo lateral o conservación | Media. Impacto en la experiencia | Es el comportamiento honesto. El texto invita a volver a indicarlo. Lo valora P2 del instrumento (`05`) |
| R6 | `message_shown` (intervención) y el prompt siguen afirmando cosas que el resultado puede contradecir | Ya existe hoy | Propuestas P-T1 y P-T2 (`04` §3), pendientes de tu aprobación |
| R7 | Pilas de PR (#31 → candidata → textos; #31 → #32 → #33): fusionarlas fuera de orden produce diferencias confusas | Media | Fusionar en orden, o rebasar sobre `generation` tras cada fusión. **No se fusiona nada automáticamente** |
| R8 | La IA real con la regla nueva no se ha medido (solo IA simulada y repetición de propuestas reales registradas) | — | Las 123 propuestas reales registradas pasan igual. Una corrida con Replicate necesitaría tu autorización y presupuesto |

## 3. Decisiones pendientes (tuyas)

1. **Fusionar la candidata H1** después de revisar la integración final (`01`–`03`). Orden: #31 y después la candidata, o la candidata directamente sobre `generation` si se rebasa.
2. **Fusionar los textos de justificación** (solo presentación), después de la candidata.
3. **P-T1:** corregir la frase del prompt «change_reason y rationale se le muestran al usuario» y pedir que no llame «más sencillo» a un cambio sin bajada de dificultad. Cambia instrucciones del modelo.
4. **P-T2/P-T3:** redacción de `AdjustmentDecisionTable.message` (texto de la decisión, guardado en `message_shown`).
5. **H12-A:** después de la revisión profesional (`05`): activar, activar con la tabla H12-B o descartar.
6. **Catálogo 580/775/780/862:** las respuestas del profesional (`06`) decidirían una migración nueva, con 960 perfiles y REPLAY.
7. **H2** (músculo en V8C): esperar a la revisión del catálogo.

## 4. Lo que no se ha hecho (restricciones de la orden)

- H12-A no se ha activado por defecto, ni V2.
- No se ha tocado V20, V24, 4D, las fórmulas de adherencia, el catálogo ni sus migraciones.
- No se ha fusionado ni desplegado ningún PR (#17, #28–#33 ni los nuevos).
- No se ha accedido a Railway ni a Neon, no se ha trabajado en Flutter y no se ha modificado el short paper IEEE: el CSV del catálogo solo se leyó.
- No se ha llamado a Replicate.
- No se ha sobrescrito ninguna evidencia anterior: la REPLAY se descartó con `git checkout` y los documentos nuevos están en `decision_h1_h12/`.
