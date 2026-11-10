# Resultados por objetivo · pr-1.8a frente a PR-1.7 (ronda A2, la que decide)

**Versión del modelo:** `81cb6179…`, la misma en todos los bloques.
- **Perfiles:** 18 de los 19 factibles. O09 queda fuera porque pr-1.8a superó el tiempo de espera tres veces (I-P-4). O11 es la inviabilidad esperada.
- **A3** quedó incompleta y no entra en la decisión (§4 del protocolo). Sus datos descriptivos están en `COMPARACION_PR17_VS_PR18A.xlsx`.

## Resumen

| Objetivo (perfiles) | Válidos a la primera, PR-1.7 → pr-1.8a | Rechazos por código, PR-1.7 → pr-1.8a | DOSIS_LIMITADA, PR-1.7 → pr-1.8a | % de M medio, PR-1.7 → pr-1.8a |
|---|---|---|---|---|
| GAIN_MUSCLE (5) | 5/5 → 5/5 | ninguno → ninguno | 4/5 → **5/5** | 57,7 → **52,5** |
| GENERAL_FITNESS (4) | 2/4 → **1/4** | V23 2 → V23 3 y **V24 1** | 4/4 → 4/4 | 65,8 → 64,3 |
| INCREASE_STRENGTH (4) | 1/4 → **3/4** | V15 11 y V26 8 → V15 2 y V26 3 | 3/4 → 1/4 | 63,1 → 62,3 |
| LOSE_WEIGHT (5) | 5/5 → **4/5** | ninguno → **V4 3** | 5/5 → 5/5 | 79,3 → 80,4 |
| **Total (18)** | **13/18 → 13/18** | 21 → 12 | 16 → 15 | 65,4 → 63,2 |

## GAIN_MUSCLE
- Los dos prompts aciertan siempre a la primera, y ninguno tiene hallazgos V24, V15 ni V26 en este objetivo.
- **La dosis fija por clase no se altera** con ninguno de los dos: no hay rechazos V24 en GAIN_MUSCLE.
- Con pr-1.8a, **los planes se quedan algo más cortos de volumen**:
  - DOSIS_LIMITADA en 5 de 5 (frente a 4 de 5);
  - % de M medio de 52,5 (frente a 57,7).
- No es significativo en el conjunto (Wilcoxon de % de M p = 0,32), pero va en contra.

## INCREASE_STRENGTH
- **Es donde pr-1.8a ayuda:** 3 de 4 válidos a la primera, frente a 1 de 4.
- Los errores de reparto (V15) y de techo semanal (V26), que con PR-1.7 se acumulan en fuerza (11 y 8), bajan a 2 y 3.
- Lo mismo se vio en A1 y en la ronda 2, con la otra versión del modelo.
- Los patrones exigidos, la capacidad STRENGTH, la distribución, las dosis exactas y la duración los comprueban V1–V27. Ningún plan publicado los incumple.

## LOSE_WEIGHT · atención especial
- **La regresión por duración (V4) vuelve a aparecer con pr-1.8a**, y solo con pr-1.8a.
  - En **O06**, el primer intento propuso una sesión de unos 2148 s con un techo de 2100 s (35 min).
  - Hubo 3 rechazos V4 en ese intento; el segundo fue válido.
  - Con PR-1.7, 0 rechazos V4.
- **El plan que acabó publicándose para O06 también es peor:**
  - 18 min de trabajo aeróbico en la semana, frente a los 36 de PR-1.7;
  - el objetivo aeróbico es de 150.
- Válidos a la primera: **4/5 con pr-1.8a frente a 5/5 con PR-1.7.** Según el criterio 6 del protocolo, **es una regresión relevante**.
- Lo positivo: con PR-1.7, O10 sale con una sesión INSUFICIENTE (2 colocaciones). Con pr-1.8a sale COMPLETA (3 colocaciones).
- Los límites de duración no se tocaron.

## GENERAL_FITNESS
- **pr-1.8a empeora:** 1 de 4 válidos a la primera, frente a 2 de 4.
- **Aparece el único hallazgo V24 de la ronda (O04):** el ejercicio 1107 no llevaba la dosis de ninguna capacidad del objetivo («usa exactamente una de: HYPERTROPHY 2-3x13…»). El backend lo rechazó y el reintento fue válido.
- **Cobertura de capacidades (V23):** 3 rechazos con pr-1.8a, frente a 2 con PR-1.7.
- No se aplicaron estructuras de fuerza o hipertrofia ajenas al objetivo en ningún plan publicado: V25 sin hallazgos.

## Conclusión por objetivo
pr-1.8a **traslada errores de un objetivo a otro**, pero no los reduce en conjunto (13 frente a 13):
- **Mejora:** fuerza.
- **Empeora:** pérdida de peso (V4) y condición general (V24 y V23).
- **Queda igual:** ganancia muscular, en validez, aunque con algo menos de volumen.
