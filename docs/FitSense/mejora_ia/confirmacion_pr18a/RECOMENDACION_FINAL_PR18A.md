# Recomendación final · ¿sustituir PR-1.7 por pr-1.8a?

**Respuesta: no. Hay que mantener PR-1.7.** Es la decisión **B** del protocolo congelado, que se fijó antes de la primera llamada pagada.

## En pocas palabras

- **Validez a la primera:** con la versión actual del modelo, pr-1.8a **no consigue más planes válidos** que PR-1.7 (13 de 18 con cada uno en A2).
- **Lo que pr-1.8a reparte de otra manera:**
  - **mejora fuerza** (3/4 frente a 1/4) gracias a menos errores V15 y V26;
  - **empeora pérdida de peso**: vuelven los rechazos por duración (V4), y en O06 el plan publicado tiene la mitad de trabajo aeróbico;
  - **empeora condición general**: aparece el único hallazgo V24 y un V23 más.
- **Fiabilidad:** pr-1.8a **tarda más** (88 s frente a 79 s de mediana). En O09 superó el límite de 150 s **cuatro veces de cuatro**, mientras que PR-1.7 no tuvo ningún timeout. En producción, cada timeout es un plan de respaldo.
- **Por qué se cierra aquí:** la ventaja de A1 (16/19 frente a 12/19) se obtuvo con otra versión del modelo y **no se reprodujo** con la actual.

## Resultado frente a los criterios (A2, 18 perfiles, misma versión del modelo)

| # | Criterio fijado antes | PR-1.7 | pr-1.8a | ¿Cumple? |
|---|---|---|---|---|
| 1 | Validez a la primera, Wilcoxon p < 0,05 | 13/18 (IC 95 % 49–88 %) | 13/18 (49–88 %) | **No**: 2 perfiles mejor, 2 peor y 14 igual; p = 1,0; McNemar 2 a 2, p = 1,0 |
| 2 | V24 ≤ control | 0 | **1** | **No** |
| 3 | Estructurales ≤ control y respaldos por rechazos ≤ control | 2 · 0 | **3** · 0 | **No** (V23) |
| 4 | Ningún plan publicado con violaciones | 0 | 0 | Sí |
| 5 | Dosis y tiempo sin empeorar de forma significativa | — | % de M p = 0,32; mejorables p = 0,84 | Sí, aunque en GAIN_MUSCLE el % de M baja de 57,7 a 52,5 |
| 6 | **LOSE_WEIGHT**: válidos a la primera ≥ control | 5/5 | **4/5** (3 rechazos V4) | **No: regresión relevante** |
| 7 | Sesiones INSUFICIENTE ≤ control | 1 | 0 | Sí |
| 8 | Consistencia (diferencia ≥ 0 en cada ronda) | — | 0 en A2; A3 no evaluable | No se puede demostrar |

**Decisión del protocolo:** fallan los criterios 2, 3 y 6, y la suma de diferencias es 0, así que el resultado es **B, mantener PR-1.7**.

## Qué pasó con A3
- A3 empezó con USD 1,527, por encima del mínimo de 1,45, pero **quedó incompleta**:
  - pr-1.8a tuvo 2 timeouts, 2 fallos del proveedor y 1 error de E/S (O19, recuperado y válido);
  - el saldo se agotó antes de PR-1.7 O17–O20.
- Según el protocolo (§4), **no entra en la decisión ni se presenta como comparación**.
- Solo como sensibilidad: en los 9 perfiles con resultado en las dos condiciones, 5 válidos con PR-1.7 frente a 6 con pr-1.8a. Una diferencia de un perfil **no cambiaría la decisión**: seguirían fallando los criterios 2, 3 y 6, y sumando A2 serían 18 frente a 19 de 27, lejos de la significación.

## Evidencia complementaria (versión anterior del modelo `045f8102…`, no se junta)

| Ronda | PR-1.7 | pr-1.8a | Nota |
|---|---|---|---|
| R2 (ronda 2, selección) | 10/19 | 15/19 | pr-1.8a se eligió con esta evidencia |
| R3 (ronda 2) | 11/19 | 12/19 | — |
| A1 (CONF-IA) | 12/19 | 16/19 | Favorable no concluyente, p = 0,28 |
| **A2 (versión actual)** | **13/18** | **13/18** | **Sin ventaja** |

**Lectura:**
- Con la versión anterior, pr-1.8a tendía a dar más planes válidos.
- Con la actual, PR-1.7 ha mejorado por sí solo (13/18) y la ventaja desaparece.
- Además, el patrón de V4 en pérdida de peso se repite en las dos versiones.

## Otras respuestas

| Pregunta | Respuesta |
|---|---|
| ¿Respeta V24, V4, V15, V26 y las estructuras? | V15 y V26: mejor (5 hallazgos frente a 19). V4: peor en LOSE_WEIGHT. V24: un hallazgo más. Estructuras: un V23 más en GENERAL_FITNESS. **Ningún plan publicado incumple nada**: el backend lo impide con los dos prompts |
| ¿Se repiten los errores de duración en LOSE_WEIGHT? | **Sí**, en O06 (2148 s frente a un techo de 2100 s). Es la segunda campaña en que pasa con pr-1.8a y nunca con PR-1.7 |
| ¿Coste por plan válido? | A2: PR-1.7 USD 0,050 (0,652 / 13) y pr-1.8a USD 0,056 (0,725 / 13, incluidas las reservas de los timeouts) |
| ¿Hay que seguir probando? | **No.** Con el resultado neutro y las regresiones, no se justifica gastar más. pr-1.8a sigue en la rama experimental, apagada |
| ¿Se activó algo? | **No.** pr-1.8a solo existe en `experimento/confirmacion-pr18a-final`, apagada por defecto. `generation` no se toca. Sin merge ni despliegue |

## Coste
- **Esta campaña:** USD 2,6115, en 99 predicciones.
- **Bloque de USD 10:** gastado USD 9,7065; quedan USD 0,2935.
- No se tocaron los presupuestos anteriores.

## Limitaciones
- Una sola ronda completa (A2), 18 perfiles sintéticos, con la versión del modelo publicada hoy.
- O09 queda fuera de A2 por timeouts de pr-1.8a. Contándolo como «no válido a la primera» con las dos condiciones, el resultado sería 13/19 frente a 13/19: la decisión no cambia.
