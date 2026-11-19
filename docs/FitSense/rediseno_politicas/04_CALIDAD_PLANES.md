# 04 · Calidad de los planes: PLAN-HIST-1 frente a PLAN-REV-2.0

**Fuente:** barrido de generación inicial sobre 3.780 perfiles (4 objetivos × 3 niveles × 3 equipamientos × 1–7 días × 15 duraciones), con el generador y el validador reales.
- Testcontainers, sin IA y sin Replicate.
- Evidencia en `evidencia/barridos/{HIST,REV2}`: `barrido_generacion.csv` con una fila por perfil y `planes.csv.gz` con todos los planes, ejercicio a ejercicio. La comparación está en `evidencia/barridos/comparacion_HIST_REV2/`.
- La repetición del barrido sobre el commit final está en `06_VERIFICACION.md`.

**Regla:** las métricas no cambian de criterio entre políticas. Un plan cuenta como «con PULL sin tracción», «con más de 24 series» o «con sesión de menos de 3 ejercicios» con la misma definición en las dos.

## 1. Totales

| Métrica | HIST | REV2 | Lectura |
|---|---|---|---|
| Perfiles con plan | 3.641 | **3.776** | 139 rescatados (todos los que HIST perdía por V20); 4 perdidos (§4) |
| Planes válidos idénticos a HIST | — | 117 de 3.637 | REV2 cambia casi todos los planes: es un rediseño, no un ajuste |
| Algún grupo > 24 series directas/semana | 446 (máx. **48**) | **0** | — |
| Algún grupo > 20 series/semana | 953 | 4 (avanzados; techo 24) | — |
| Sesiones PULL sin ningún ejercicio de tracción | 271 planes | **0** | P-G3 |
| Planes con alguna sesión de menos de 3 ejercicios | 42 | **0** | — |
| Planes con alguna sesión «corta razonable» (< 50 % de M y no insuficiente) | — | 1.063 | Consecuencia buscada de P-G1: la sesión acaba antes cuando la dosis está cubierta |
| Planes con alguna sesión insuficiente (< 3 ejercicios o < 6 series) | — | 0 | — |
| Estado de la dosis semanal | — | DOSIS_OBJETIVO 1.391 · DOSIS_LIMITADA 2.385 | DOSIS_LIMITADA: algún grupo o el aeróbico no llega al objetivo (casi siempre por el catálogo o por el número de días); queda en la traza |
| Planes con algún ejercicio de alto impacto | 1.835 | 1.082 | La preferencia de menor impacto en los complementos (P-C2) lo reduce sobre todo en LW y GF |
| Planes con algún ejercicio de impacto moderado | 1.915 | 1.007 | — |
| Dificultad máxima 1 / 2 / 3 (planes) | 1.290 / 1.423 / 928 | 1.373 / 1.459 / 944 | Sin cambio de criterio: V6 limita la dificultad por nivel en las dos |

## 2. Por objetivo y nivel (medianas; HIST → REV2)

| Objetivo · nivel | % del tiempo | seg. por sesión | ej. sesión más corta | series máx./grupo/semana (mediana · máximo) | min aeróbicos/semana | alto impacto (ejercicios) | PULL sin tracción |
|---|---|---|---|---|---|---|---|
| GAIN_MUSCLE B | 72 → 58 | 2539 → 1808 | 7 → 5 | 18 · 36 → 12 · 12 | 0 → 0 | 0 → 0 | 0 → 0 |
| GAIN_MUSCLE I | 73 → 66 | 2550 → 1981 | 7 → 6 | 18 · 48 → 15 · 20 | 0 → 0 | 177 → 124 | 15 → 0 |
| GAIN_MUSCLE A | 73 → 66 | 2550 → 1999 | 7 → 6 | 18 · 48 → 15 · 21 | 0 → 0 | 378 → 267 | 15 → 0 |
| INCREASE_STRENGTH B | 72 → **37** | 2550 → 1204 | 8 → 4 | 18 · 36 → 8 · 8 | 0 → 0 | 0 → 0 | 0 → 0 |
| INCREASE_STRENGTH I | 74 → 56 | 2556 → 1792 | 6 → 4 | 18 · 42 → 12 · 18 | 0 → 0 | 208 → 156 | 50 → 0 |
| INCREASE_STRENGTH A | 74 → 56 | 2556 → 1792 | 6 → 4 | 18 · 42 → 12 · 18 | 0 → 0 | 355 → 236 | 50 → 0 |
| LOSE_WEIGHT B | 74 → 70 | 2508 → 2160 | 7 → 4 | 12 · 24 → 4 · 6 | 36 → 45 | 0 → 0 | 0 → 0 |
| LOSE_WEIGHT I | 73 → **92** | 2562 → 3228 | 5 → 6 | 12 · 30 → 6 · 12 | 48 → 72 | 1203 → 101 | 86 → 0 |
| LOSE_WEIGHT A | 73 → **92** | 2562 → 3228 | 5 → 6 | 12 · 30 → 6 · 12 | 48 → 72 | 1276 → 144 | 85 → 0 |
| GENERAL_FITNESS B | 72 → 63 | 2533 → 2008 | 8 → 6 | 14 · 22 → 6 · 9 | 18 → 36 | 0 → 0 | 0 → 0 |
| GENERAL_FITNESS I | 77 → 85 | 2687 → 2573 | 7 → 6 | 15 · 30 → 9 · 15 | 30 → 48 | 738 → 158 | 75 → 0 |
| GENERAL_FITNESS A | 77 → 84 | 2680 → 2587 | 7 → 6 | 15 · 30 → 9 · 15 | 30 → 48 | 829 → 232 | 75 → 0 |

## 3. Diferencias relevantes y su justificación

1. **Volumen.**
   - HIST llenaba hasta el 70 % del tiempo, con principiantes de hasta 36 series semanales por grupo y avanzados de hasta 48. REV2 respeta el techo [HP][PRO] de 12, 20 o 24 en todos los planes.
   - En GAIN_MUSCLE la mediana queda en 12–15 series, dentro de la banda que la evidencia asocia a hipertrofia (~10–20; `02_EVIDENCIA_DOSIS.md` §1).
   - No se ha recortado volumen útil para cumplir una cifra: el objetivo semanal de A está en el inicio de esa banda.
2. **Tiempo.**
   - REV2 usa menos tiempo en fuerza y en GAIN_MUSCLE, porque la dosis ya está cubierta.
   - Usa más en LOSE_WEIGHT intermedio y avanzado, porque el objetivo aeróbico lo pide: la mediana pasa de 48 a 72 min de trabajo por semana. Sigue **por debajo de 150**: es una limitación del catálogo y de V18 (§4).
   - El caso extremo es INCREASE_STRENGTH principiante: ~20 min con 4 ejercicios de fuerza de 2–3 series. Es **corto pero razonable** según la evidencia (3–5 multiarticulares con 6–12 series efectivas; Iversen 2021) y queda clasificado como `CORTA_RAZONABLE` en la traza. Está **pendiente de validación profesional**.
3. **Composición.**
   - No hay sesiones PULL sin tracción.
   - Ninguna sesión baja de 3 ejercicios: en HIST había 42 planes con una sesión de 2.
   - La sesión más corta de REV2 tiene menos ejercicios (4–6) que la de HIST (5–8), porque HIST añadía ejercicios para llegar al 70 %.
4. **Impacto.**
   - En LW y GF intermedios y avanzados, el alto impacto baja de ~1.200 ejercicios a ~100–230.
   - P-C2 no prohíbe el alto impacto a quien lo admite (avanzados con `allowHighImpact`): solo prefiere el de menos impacto cuando el complemento es intercambiable.
5. **Dificultad y equipamiento.** Mismo criterio que HIST (V6 y equipamiento del perfil) y sin diferencias relevantes.
6. **Recuperación.** Las reglas de recuperación entre sesiones (P1/V15 y AUD-G1) no cambian. Con REV2, ningún perfil del barrido falla por V15.
7. **Adecuación al objetivo.**
   - GM queda en su banda de volumen.
   - IS lleva los 4 patrones de fuerza con la dosis de fuerza.
   - LW lleva fuerza mínima (4 series por grupo y semana) además del aeróbico.
   - GF combina las dos cosas.
   - Todas las cifras son [HP][PRO].

## 4. Lo que no mejora o empeora (limitaciones, no ocultadas)

| # | Limitación | Alcance | Clasificación |
|---|---|---|---|
| L1 | INCREASE_STRENGTH intermedio y avanzado, gimnasio y casa con mancuernas, **1 día de 30 min**: sin plan por V25. Los 4 patrones con la dosis de fuerza suman unos 1.896 s, por encima de los 1.800 s máximos | 4 perfiles de 3.780 | Justificada. Alternativas (a), (b) y (c) en `01_DISENOS_DURACION.md` §4, **pendientes de decisión** |
| L2 | `DOSIS_LIMITADA` en 2.385 planes: algún grupo o el aeróbico no alcanza el objetivo semanal | 63 % | Información en la traza. No se rellena con volumen inventado |
| L3 | LOSE_WEIGHT intermedio y avanzado: 72 min de trabajo aeróbico por semana (mediana) frente al objetivo de 150 [SP] | LW I/A | Catálogo (pocos ejercicios aeróbicos prescribibles en casa) y V18 |
| L4 | Sin tracción con peso corporal en el catálogo: en casa sin material no hay PULL, y la división pasa a superior/inferior (P-G3) | Casa con peso corporal | Limitación del catálogo. **No se inventan** ejercicios ni se asume que una mesa o una toalla sean seguras |
| L5 | 1261, 1308 y 775 figuran como `back` con patrón HORIZONTAL_PUSH (clasificación dudosa del catálogo) | Composición de UPPER/PULL | Catálogo: propuesta de revisión profesional |
| L6 | GAIN_MUSCLE A/I: 267 y 124 ejercicios de alto impacto | Perfiles que lo admiten | Coherente con el perfil; sin cambio |

## 5. J03 (LOSE_WEIGHT principiante, casa sin material, 4 días de 70 min)

**Con HIST:** sin plan. La rejilla chocaba entre V20 (70 %) y V15, y la única salida era llenar la sesión LOWER con seis ejercicios de pierna.

**Con REV2** (pre-ejecución K20 en 9f42767, idéntica en el commit final):

| Sesión | Enfoque | Contenido | Estimado |
|---|---|---|---|
| Lun | UPPER_BODY | 1262 flexión araña, 775 push-up invertido, 1320 dip en suelo (2×8, 120 s) + 853, 253 y 1054 (3×90 s de cardio) | 2.274 s (54 %) |
| Mar | LOWER_BODY | 1073 puente de glúteo (2×8) + 1117 y 1093 (3×90 s) | 1.368 s (33 %) |
| Jue | UPPER_BODY | 1115, 775, 509 (2×8) + 853, 1054 y 1117 (3×90 s) | 2.274 s (54 %) |
| Vie | LOWER_BODY | 1263 sentadilla búlgara (2×8) + 253 y 1093 (3×90 s) | 1.368 s (33 %) |

**Lectura:**
- Se genera sin tocar V15.
- No hay seis ejercicios de pierna: la pierna lleva 4 series en la semana, la dosis mínima [HP][PRO].
- Hay 45 min de trabajo aeróbico, el objetivo para principiante [SEG/HP].
- Las sesiones LOWER son cortas (23 min de 70). Con la dosis de REV2 es lo pertinente, pero conviene revisarlo profesionalmente: ¿añadir movilidad o un segundo ejercicio de pierna?
- La sesión superior no tiene tracción (L4) y es UPPER_BODY, no PULL.
- **Resuelto en lo que la orden pedía:** plan válido sin relleno.
