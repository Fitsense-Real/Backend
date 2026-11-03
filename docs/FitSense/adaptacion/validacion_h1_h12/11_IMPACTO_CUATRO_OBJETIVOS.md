# 11 · Impacto sobre los cuatro objetivos (fase 7)

## 1. Cobertura

| Objetivo | Histórico con IA real (sesiones TOO_DIFFICULT / sustituciones) | Escenarios H12 (perfil: nivel, lugar) |
|---|---|---|
| GAIN_MUSCLE | 9 / 56 (U04 intermedio, gimnasio; R01 principiante, casa) | R01 (principiante, casa), R02 (intermedio, gimnasio, 75 min: NO_FACTIBLE de control), E10 (intermedio, gimnasio, 60 min), E12 (principiante, casa, solo peso corporal) |
| INCREASE_STRENGTH | 11 / 26 (U09 intermedio, gimnasio; R03 principiante, casa; R04 intermedio, gimnasio) | R03 (principiante, casa), R04 (intermedio, gimnasio), E09 (**avanzado**, gimnasio) |
| LOSE_WEIGHT | 9 / 32 (U14 principiante, casa; R05 principiante, gimnasio) | R05 (principiante, gimnasio), R06 (intermedio, casa, solo peso corporal) |
| GENERAL_FITNESS | 9 / 9 (U19 principiante, casa; R08 intermedio, gimnasio) | R07 y E11 (principiantes, casa), R08 (intermedio, gimnasio) |

Cada objetivo tiene principiante e intermedio, casa y gimnasio. GM intermedio a 75 min sigue siendo NO_FACTIBLE en la generación (H6 del PR #30). **No se cambia la política para hacerlo factible.**

## 2. H1 (multiconjunto) por objetivo

| Objetivo | Sustituciones históricas válidas | Escenarios: cambio frente a la histórica |
|---|---:|---|
| GAIN_MUSCLE | **56 / 56** | E10 (#12b): rechazado; el reintento hace 3 bajadas del mismo patrón |
| INCREASE_STRENGTH | **26 / 26** | Ninguno |
| LOSE_WEIGHT | **32 / 32** | Ninguno |
| GENERAL_FITNESS | **9 / 9** | Ninguno |

**H1 no cambia la dosis, V20, V24 ni la generación**, en ningún objetivo (`05`, `12`).

## 3. Alternativas H12 por objetivo

**Escenarios** (3 semanas; sesiones señaladas que cambiaron / laterales / bajadas / cambios de dosis):

| Objetivo | P0 histórica | P1 H1 | P2 H1 + H12-A | P3 H1 + H12-C (2) |
|---|---|---|---|---|
| GAIN_MUSCLE (12 señaladas) | 5 / 20 / 3 / 0 | 4 / 18 / 3 / 0 | **1 / 0 / 3 / 0** | 0 / 0 / 0 / 0 |
| INCREASE_STRENGTH (12) | 7 / 3 / 5 / 2 | 7 / 3 / 5 / 2 | 6 / 0 / 5 / 4 | 6 / 3 / **2** / 2 |
| LOSE_WEIGHT (8) | 3 / 14 / 0 / 2 | 3 / 14 / 0 / 2 | **1 / 0 / 0 / 2** | 1 / 0 / 0 / 2 |
| GENERAL_FITNESS (9) | 8 / 5 / 1 / 4 | 8 / 5 / 1 / 4 | 6 / 0 / 1 / **11** | 8 / 5 / 1 / 4 |

**Histórico** (estimación con un modelo que cumpliera H12-A; `07` §2):

| Objetivo | Sesiones TOO_DIFFICULT | Cambiadas hoy | H12-A rechaza en el 1.er intento | → SIN_EFECTO (sin bajada ni dosis) | H12-C (2) rechaza |
|---|---:|---:|---:|---:|---:|
| GAIN_MUSCLE | 9 | 9 | 9 | **4** (R01 principiante; U04 se resuelve con bajadas) | 9 |
| INCREASE_STRENGTH | 11 | 10 | 6 | 1 | 5 |
| LOSE_WEIGHT | 9 | 8 | 6 | 3 (R05, margen 0) | 5 |
| GENERAL_FITNESS | 9 | 6 | 2 | 0 | 2 |

## 4. Comprobaciones específicas pedidas

**GAIN_MUSCLE:**
- **No se rompe 4D ni V24.** Ninguna alternativa autoriza reducir la dosis. En los escenarios, E12 propone una repetición menos y V24 la rechaza en las cuatro configuraciones.
- **Las bajadas de dificultad se aceptan** (E10, U04) porque la sustitución 4D usa la misma dosis del ejercicio (`admiteMismaDosis`).
- **Principiante en GM:** sin variante menor y sin dosis. Con H12-A, **conservar** es la única salida (R01 y E12: SIN_EFECTO). Lo que hoy hace la IA (lateral 6 de 6) no es una reducción observable.

**INCREASE_STRENGTH:**
- Las sustituciones de menor dificultad (E09: 3 → 2 y 3 → 1; R04: 2 → 1) se aceptan con H1 y con H12-A.
- **H12-C (2) bloquea E09** (3 bajadas).
- Las reducciones autorizadas (R03 S3: −1 repetición) se mantienen. En el principiante, H12-A cambia el lateral por una repetición menos.

**LOSE_WEIGHT:**
- Ejercicios por duración (R05: cardio 3 × 90 s; R06: 3 × 120 s).
- **V20** rechaza lo que no cabe en las cuatro configuraciones (R05 con margen 0 y R06 en la semana 3).
- Con H12-A, R05 (sin margen y todo de dificultad 1) queda conservada en vez de cambiar 7 de 8 ejercicios cada semana.

**GENERAL_FITNESS:**
- La movilidad y el cardio (DURATION) siguen admitiendo reducciones de segundos o series cuando caben (R07: tres semanas; R06).
- Las sustituciones de movilidad (1260 → 66 en PR #30) son del mismo patrón: H1 las acepta, y H12-A solo si bajan la dificultad.
- **Continuidad:** las sesiones que llegan al mínimo se conservan después (R07 semana 3, E11 semana 3 con H12-A).

**Generación inicial y adherencia:**
- En las cuatro corridas de escenarios, `generacion.csv`, el plan inicial (195 filas), `adherencia.csv` y las métricas Aw, Aobs y C de las semanas 2 y 3 son **idénticos** (comparados byte a byte y por columnas).
- La comparación de 960 perfiles está en `12`.

**No se ha cambiado ninguna política para hacer factibles los perfiles.**
