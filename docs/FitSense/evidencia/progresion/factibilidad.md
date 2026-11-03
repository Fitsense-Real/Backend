# Factibilidad de la progresion de volumen (simulacion)

Perfiles sin plan: 0

Sesiones analizadas: 216

## Por objetivo: incremento minimo y que lo permite

| Objetivo | Sesiones | Paso minimo ≤ 5 % | ≤ 10 % | Algun paso ≤ 10 % valido HOY | Valido si V24 admitiera la dosis (solo V24-dosis bloquea) | Mediana del paso minimo |
|---|---:|---:|---:|---:|---:|---:|
| LOSE_WEIGHT | 54 | 54 (100 %) | 54 (100 %) | 6 (11 %) | 6 (11 %) | 1.4 % |
| GAIN_MUSCLE | 54 | 54 (100 %) | 54 (100 %) | 0 (0 %) | 54 (100 %) | 1.4 % |
| INCREASE_STRENGTH | 54 | 54 (100 %) | 54 (100 %) | 18 (33 %) | 18 (33 %) | 2.3 % |
| GENERAL_FITNESS | 54 | 54 (100 %) | 54 (100 %) | 14 (26 %) | 14 (26 %) | 0.8 % |

## Por tipo de paso: cuantas veces lo bloquea cada regla

| Objetivo | Paso | Intentos | Validos | Fuera del rango de series | Bloqueos (regla: veces) |
|---|---|---:|---:|---:|---|
| GAIN_MUSCLE | +1 rep/serie | 306 | 0 | 0 | {V24-dosis=306} |
| GAIN_MUSCLE | +1 serie | 306 | 0 | 257 | {V24-dosis=306} |
| GAIN_MUSCLE | +2 rep/serie | 306 | 0 | 0 | {V24-dosis=306} |
| GENERAL_FITNESS | +1 rep/serie | 195 | 0 | 0 | {V24-otro=195} |
| GENERAL_FITNESS | +1 serie | 327 | 54 | 219 | {V24-otro=273, V4=20} |
| GENERAL_FITNESS | +2 rep/serie | 195 | 0 | 0 | {V24-otro=195} |
| GENERAL_FITNESS | +5 s/serie | 80 | 0 | 0 | {V24-otro=80} |
| INCREASE_STRENGTH | +1 rep/serie | 294 | 0 | 0 | {V24-otro=294, V25=6} |
| INCREASE_STRENGTH | +1 serie | 294 | 102 | 192 | {V24-otro=192, V25=5} |
| INCREASE_STRENGTH | +2 rep/serie | 294 | 0 | 0 | {V24-otro=294, V25=6} |
| LOSE_WEIGHT | +1 rep/serie | 120 | 0 | 0 | {V24-otro=120} |
| LOSE_WEIGHT | +1 serie | 228 | 60 | 168 | {V24-otro=168} |
| LOSE_WEIGHT | +2 rep/serie | 120 | 0 | 0 | {V24-otro=120} |
| LOSE_WEIGHT | +5 s/serie | 108 | 0 | 0 | {V24-otro=108} |

## Por objetivo y minutos: paso minimo valido hoy

| Objetivo | 30 min | 45 min | 60 min |
|---|---:|---:|---:|
| LOSE_WEIGHT | 0 (0 %) | 0 (0 %) | 6 (33 %) |
| GAIN_MUSCLE | 0 (0 %) | 0 (0 %) | 0 (0 %) |
| INCREASE_STRENGTH | 6 (33 %) | 6 (33 %) | 6 (33 %) |
| GENERAL_FITNESS | 2 (11 %) | 6 (33 %) | 6 (33 %) |
