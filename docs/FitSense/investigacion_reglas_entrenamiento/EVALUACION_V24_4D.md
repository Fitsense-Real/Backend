# Evaluación de V24 / 4D: límites de ejercicios, dosis y volumen

## 1. Lo que limita hoy el número de ejercicios

| Límite | Valor | Origen | Valoración |
|---|---|---|---|
| Tope de GAIN_MUSCLE (4D) | 6 (< 60 min) / 7 (≥ 60) | 6: spec «4D v2 §5», no está en el repositorio. 7: parche de la adenda 4D v3.1 para que 60 min llegue a V20 | El 7 no es un criterio de volumen, es una consecuencia de V20 |
| Tope de capacidades | 8 | Constante técnica del motor original (`MAX_EXERCISES_PER_SESSION`), convertida en validación | Sin origen funcional |
| Presupuesto 4D | 85 % del tiempo | Spec externa | No auditable en el repositorio |

**Evidencia** (`REVISION_CIENTIFICA_REGLAS.md` §3 a–d):
- ninguna guía fija un **máximo** de ejercicios por sesión;
- el «8–10 ejercicios» del ACSM 1998 era un **mínimo** para cubrir los grandes grupos;
- la variable con dosis-respuesta es el **número de series por grupo y semana** (Pelland 2026; Schoenfeld 2017; Baz-Valle 2021);
- por sesión, el único techo útil encontrado es preliminar: ~11 series fraccionales por músculo para hipertrofia (Remmert 2025, preprint).

**Conclusión.** El número de ejercicios es una **variable derivada**: series por semana ÷ (series por ejercicio × sesiones). Un tope fijo es, como mucho, una protección de UX y tiempo. No es un control de seguridad ni de dosis.

## 2. Lo que el tope no controla

- **No limita el volumen por grupo en una sesión.** Con la regla vigente hay sesiones de 15–21 series de un mismo grupo (`MATRIZ_EXPERIMENTOS.csv`, `series_sesion_max_grupo`; INCREASE_STRENGTH y GAIN_MUSCLE con 6–7 días). Esas sesiones respetan el tope de 7/8 colocaciones porque el volumen va en las series, no en el número de ejercicios.
- **No garantiza la dosis semanal.** Solo 25 de 163 planes válidos de GAIN_MUSCLE llegan a ≥ 10 series por semana en los cuatro grupos grandes (pecho, espalda, hombros, pierna).

## 3. Dosis de 4D y de capacidades frente a la evidencia

| Dosis | Evidencia | Valoración |
|---|---|---|
| Hipertrofia 3×10, descanso 90 s (zona pequeña 3×13) | ACSM 2009: 6–12 RM, 1–2 min. Singer 2024: sin mejora apreciable más allá de 90 s | Coherente. **Conservar** como dosis por defecto |
| Fuerza 3×5, descanso 150 s (principiante 2×8/120) | ACSM 2009: 3–5 min con cargas pesadas. Grgic 2018: 60–120 s bastan en no entrenados | Coherente para principiantes. **Discutible** para intermedios y avanzados (por debajo de 3 min) |
| Resistencia 3×18, descanso 45 s | ACSM 2009: > 15 repeticiones, < 90 s | Coherente |
| Cardio 3 × 90–120 s, descanso 60 s | OMS: 150–300 min/semana moderado o 75–150 vigoroso | **Insuficiente como dosis semanal**: los planes dan 27–40 min/semana |
| Series máx. 3 «solo para V20» | ACSM 2009: 1–3 series | La dosis es coherente; el **motivo** (rellenar tiempo) no lo es |
| Carga +10 % (V11) | ACSM 2009: +2–10 % | Fundamentada |

## 4. Qué conservar de 4D

**Conservar:**
- la dosis por capacidad (series, repeticiones, descanso), que es coherente con ACSM 2009 y Singer 2024;
- la regla de no alargar descansos para rellenar (V20 / `max_rest_seconds` = 180);
- la evidencia de hipertrofia (capacidad acreditada por colocación);
- la progresión de carga del 2–10 %.

**Replantear:**
- el **tope fijo** de colocaciones (6/7/8) como criterio de dosis;
- el **presupuesto del 85 %**, sin origen auditable, que en la práctica es otra forma de techo de tiempo y se solapa con V4.

**Añadir (laguna):**
- un control de **series por grupo y semana** (mínimo y máximo por objetivo y nivel);
- un control de **series por grupo y sesión** (techo blando);
- un control de **minutos aeróbicos semanales** en LOSE_WEIGHT y GENERAL_FITNESS.

## 5. Experimentos sobre el tope

| Alternativa | Perfiles con plan (de 912) | GAIN_MUSCLE 75 | Observaciones |
|---|---:|---:|---|
| A: tope fijo vigente | 830 | 0/57 | Volumen por grupo y sesión sin control (hasta 21) |
| B: tope dinámico (por encima del tope solo si falta el piso; ≤ 10 series por grupo y sesión y ≤ 20 por semana) | 863 | 30/57 | Rescata la mitad de GAIN_MUSCLE 75; el resto choca con el presupuesto 4D, V15 o la falta de grupos en el enfoque |
| C: sin tope (solo semana y tiempo) | 863 | 30/57 | En esta matriz, igual que B: el límite por sesión no llegó a actuar porque B parte del borrador vigente |
| E: sin tope fijo, con mínimo de volumen semanal y sin V20 | 896 | 57/57 | 4 % de sesiones con > 11 series de un grupo. Pero en 80 perfiles con **1–3 días** (sobre todo GAIN_MUSCLE con 1–2) la sesión llega a 13–21 colocaciones y al 113–115 % de M. Es decir: **con 1–2 días, el objetivo de 10 series por semana y grupo no cabe sin sesiones excesivas**. El objetivo semanal debe escalar con la frecuencia disponible (mínimo de ~4 series, Iversen 2021) y debe existir un techo por sesión (ejercicios o series) |

Las colocaciones de B, C y E por encima del tope son **experimentales**: incumplen V24 (tope) y a veces el presupuesto 4D. No son válidas con la política vigente.
