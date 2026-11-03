# 08 · Pruebas de H12: escenarios multisemana (fase 5)

## 1. Montaje

**Servicios reales del backend y PostgreSQL efímero** (Testcontainers), con el laboratorio `ExperimentoKora20IT` (*opt-in*):
- modo `GUION`, **sin red y sin Replicate**;
- **tres semanas** (ampliación del laboratorio: `semanas=3`).

**Respuestas controladas:** guion `H12`. Cada sesión señalada recibe en el **intento 1** la estrategia escrita en el perfil:

| Estrategia | Qué propone |
|---|---|
| `LATERAL_TODO` / `LATERAL_UNO` | Sustitutos de la misma dificultad y dosis (todos / uno) |
| `LATERAL_EQUIPO` | Lateral con otro equipamiento |
| `MENOR` / `MENOR_UNO` | Sustitutos de menor dificultad |
| `DOSIS` | Primera reducción de dosis, quepa o no |
| `CAMBIO_MUSCULO` | Mismo patrón, otro músculo |
| `VIOLA_H1` | #12b |
| `CUMPLIDOR` | Lo que cumple H12-A |

**Reintentos:** si el validador rechaza, el **intento 2** responde como un modelo que cumple H12-A (`CUMPLIDOR`: solo bajadas de dificultad y dosis que caben; si no hay nada, omite) y el **intento 3** omite.

**Perfiles congelados** (`evidencia/h12/PERFILES_ESCENARIOS_H12.csv`):
- R01–R08, **en el mismo orden** que la cohorte del PR #30, para reproducir sus planes con las mismas semillas;
- E09 (FU avanzado), E10 (GM intermedio), E11 (AG principiante en casa) y E12 (GM principiante, solo peso corporal);
- los **cuatro objetivos**; principiante, intermedio y avanzado; casa y gimnasio.

**Cuatro configuraciones**, mismo *commit* `5773f9a`:

| Corrida | `v8c.pattern-rule` | `variante.policy` | `variante.max-sustituciones` |
|---|---|---|---|
| `P0_HISTORICA` | conjunto | historica | 0 |
| `P1_H1` | multiconjunto | historica | 0 |
| `P2_H1_H12A` | multiconjunto | reduccion-demostrable | 0 |
| `P3_H1_H12C2` | multiconjunto | historica | 2 |

Cada corrida deja su `00_configuracion.txt` con las tres propiedades, más los planes, decisiones, V32, `trazas_ia.jsonl` (lo que propuso el guion en cada intento) y `ejecucion_maven.log`.
- Análisis: `evidencia/h12/analizar_escenarios.py` → `escenarios_resultado.csv` (164 filas: 41 sesiones señaladas por 4 corridas).

## 2. Los 15 escenarios

En cada celda:
- **estado de la sesión publicada:** APLICADA si cambió, SIN_EFECTO si se conservó;
- número de intentos y códigos de rechazo;
- lo que cambió: sustituciones (laterales / a menor dificultad) y cambios de dosis.

| # | Escenario | Caso (usuario, semana, sesión, estrategia del intento 1) | P0 histórica | P1 H1 | P2 H1 + H12-A | P3 H1 + H12-C (N = 2) |
|---:|---|---|---|---|---|---|
| 1 | Dificultad 3 con alternativa de dificultad 2 | E09 (FU avanzado, gimnasio) sem. 1 S2, MENOR | **APLICADA** · 1 int. · 3 sust. (0 lat., 3 a menor) | **APLICADA** · 1 int. · 3 sust. (0 lat., 3 a menor) | **APLICADA** · 1 int. · 3 sust. (0 lat., 3 a menor) | **SIN_EFECTO** · 3 int.; rechazos V8C y V8C · sin cambio |
| 2 | Dificultad 2 con alternativa de dificultad 1 | R08 (AG intermedio, gimnasio) sem. 1 S2, MENOR | **APLICADA** · 1 int. · 1 sust. (0 lat., 1 a menor) | **APLICADA** · 1 int. · 1 sust. (0 lat., 1 a menor) | **APLICADA** · 1 int. · 1 sust. (0 lat., 1 a menor) | **APLICADA** · 1 int. · 1 sust. (0 lat., 1 a menor) |
| 2 | (íd.) | R04 (FU intermedio) sem. 2 S2, MENOR, margen 6 s | **APLICADA** · 2 int.; rechazo V20 · 2 sust. (0 lat., 2 a menor) | **APLICADA** · 2 int.; rechazo V20 · 2 sust. (0 lat., 2 a menor) | **APLICADA** · 2 int.; rechazo V20 · 2 sust. (0 lat., 2 a menor) | **APLICADA** · 2 int.; rechazo V20 · 2 sust. (0 lat., 2 a menor) |
| 3 | Dificultad 1 sin alternativa inferior | R03 (FU principiante, casa) sem. 1 S2, LATERAL_UNO | **APLICADA** · 1 int. · 1 sust. (1 lat., 0 a menor) | **APLICADA** · 1 int. · 1 sust. (1 lat., 0 a menor) | **APLICADA** · 2 int.; rechazo V8C · 1 dosis | **APLICADA** · 1 int. · 1 sust. (1 lat., 0 a menor) |
| 4 | Mismo nivel, distinta demanda (otro equipamiento) | E11 (AG principiante, casa) sem. 1 S2, LATERAL_EQUIPO | **APLICADA** · 1 int. · 1 sust. (1 lat., 0 a menor) | **APLICADA** · 1 int. · 1 sust. (1 lat., 0 a menor) | **APLICADA** · 2 int.; rechazo V8C · 3 dosis | **APLICADA** · 1 int. · 1 sust. (1 lat., 0 a menor) |
| 5 | Sesión sin margen de reducción | R05 (PP principiante) sem. 1 S2, DOSIS, margen 0 | **SIN_EFECTO** · 2 int.; rechazo V20 · sin cambio | **SIN_EFECTO** · 2 int.; rechazo V20 · sin cambio | **SIN_EFECTO** · 2 int.; rechazo V20 · sin cambio | **SIN_EFECTO** · 2 int.; rechazo V20 · sin cambio |
| 6 | Sesión con margen pequeño | R04 sem. 1 S3 (fatiga), DOSIS, margen 6 s | **SIN_EFECTO** · 2 int.; rechazo V20 · sin cambio | **SIN_EFECTO** · 2 int.; rechazo V20 · sin cambio | **SIN_EFECTO** · 2 int.; rechazo V20 · sin cambio | **SIN_EFECTO** · 2 int.; rechazo V20 · sin cambio |
| 7 | Reducción de dosis permitida | R03 sem. 1 S3 (desmotivación), dosis que cabe | **APLICADA** · 1 int. · 1 dosis | **APLICADA** · 1 int. · 1 dosis | **APLICADA** · 2 int.; rechazo V8C · 1 dosis | **APLICADA** · 1 int. · 1 dosis |
| 8 | Reducción de dosis prohibida por 4D | E12 (GM principiante, solo peso corporal) sem. 1 S2, DOSIS | **SIN_EFECTO** · 2 int.; rechazo V24 · sin cambio | **SIN_EFECTO** · 2 int.; rechazo V24 · sin cambio | **SIN_EFECTO** · 2 int.; rechazo V24 · sin cambio | **SIN_EFECTO** · 2 int.; rechazo V24 · sin cambio |
| 9 | Sustitución de un ejercicio | R08 sem. 1 S2 (1 a menor) y R03 sem. 1 S2 (1 lateral) | **APLICADA** · 1 int. · 1 sust. (0 lat., 1 a menor) | **APLICADA** · 1 int. · 1 sust. (0 lat., 1 a menor) | **APLICADA** · 1 int. · 1 sust. (0 lat., 1 a menor) | **APLICADA** · 1 int. · 1 sust. (0 lat., 1 a menor) |
| 10 | Sustitución de casi toda la sesión | R01 (GM principiante) sem. 1 S2, LATERAL_TODO (6 de 6) | **APLICADA** · 1 int. · 6 sust. (6 lat., 0 a menor) | **APLICADA** · 1 int. · 6 sust. (6 lat., 0 a menor) | **SIN_EFECTO** · 2 int.; rechazo V8C · sin cambio | **SIN_EFECTO** · 2 int.; rechazo V8C · sin cambio |
| 10 | (íd.) | R05 sem. 2 S2, LATERAL_TODO (7 de 8), margen 0 | **APLICADA** · 1 int. · 7 sust. (7 lat., 0 a menor) | **APLICADA** · 1 int. · 7 sust. (7 lat., 0 a menor) | **SIN_EFECTO** · 2 int.; rechazo V8C · sin cambio | **SIN_EFECTO** · 2 int.; rechazo V8C · sin cambio |
| 11 | Segunda semana con nueva omisión por dificultad | R01 sem. 2 S2, LATERAL_TODO | **APLICADA** · 1 int. · 6 sust. (6 lat., 0 a menor) | **APLICADA** · 1 int. · 6 sust. (6 lat., 0 a menor) | **SIN_EFECTO** · 2 int.; rechazo V8C · sin cambio | **SIN_EFECTO** · 2 int.; rechazo V8C · sin cambio |
| 12 | Tercera solicitud sobre una sesión ya modificada | R01 sem. 3 S2, LATERAL_TODO | **APLICADA** · 1 int. · 6 sust. (6 lat., 0 a menor) | **APLICADA** · 1 int. · 6 sust. (6 lat., 0 a menor) | **SIN_EFECTO** · 2 int.; rechazo V8C · sin cambio | **SIN_EFECTO** · 2 int.; rechazo V8C · sin cambio |
| 12 | (íd.) | R05 sem. 3 S2, LATERAL_TODO | **APLICADA** · 1 int. · 7 sust. (7 lat., 0 a menor) | **APLICADA** · 1 int. · 7 sust. (7 lat., 0 a menor) | **SIN_EFECTO** · 2 int.; rechazo V8C · sin cambio | **SIN_EFECTO** · 2 int.; rechazo V8C · sin cambio |
| 13 | Conserva el patrón y cambia el músculo | R08 sem. 2 S2, CAMBIO_MUSCULO | **APLICADA** · 1 int. · 1 sust. (1 lat., 0 a menor) | **APLICADA** · 1 int. · 1 sust. (1 lat., 0 a menor) | **APLICADA** · 2 int.; rechazo V8C · 3 dosis | **APLICADA** · 1 int. · 1 sust. (1 lat., 0 a menor) |
| 14 | Incumple la regla H1 | E10 (GM intermedio, 60 min) sem. 1 S2, VIOLA_H1 | **APLICADA** · 1 int. · 2 sust. (2 lat., 0 a menor) | **APLICADA** · 2 int.; rechazo V8C · 3 sust. (0 lat., 3 a menor) | **APLICADA** · 2 int.; rechazo V8C · 3 sust. (0 lat., 3 a menor) | **SIN_EFECTO** · 3 int.; rechazos V8C y V8C · sin cambio |
| 15 | Sin alternativas | E12 sem. 2 S2, CUMPLIDOR (no hay nada que ofrecer) | **SIN_EFECTO** · 1 int. · sin cambio | **SIN_EFECTO** · 1 int. · sin cambio | **SIN_EFECTO** · 1 int. · sin cambio | **SIN_EFECTO** · 1 int. · sin cambio |

**Lectura por escenario:**
- **1–2 (bajadas reales).**
  - Las tres primeras configuraciones las aceptan.
  - **H12-C con N = 2 rechaza la de E09**, que tiene 3 bajadas legítimas, y la deja SIN_EFECTO. Un límite numérico no distingue lo lateral de lo simplificado.
- **3–4 (dificultad 1, laterales).**
  - Histórica y H1 publican el lateral.
  - **H12-A lo rechaza**, y el reintento resuelve con una **reducción de dosis** que cabe (R03: 1; E11: 3).
- **5–6 (margen 0 o pequeño).**
  - En las cuatro configuraciones V20 rechaza la reducción que no cabe, y la sesión se **conserva**.
  - H12 no interviene: ni añade ni quita salidas de dosis.
- **7 (dosis permitida).** Igual en las cuatro.
- **8 (4D).**
  - V24 rechaza la reducción de dosis en GM en las cuatro.
  - **Ninguna alternativa autoriza dosis que hoy están prohibidas.**
- **9 (una sustitución).** La bajada se acepta siempre; la lateral solo sin H12-A.
- **10–12 (sesión casi entera y semanas 2–3).**
  - **P0 y P1:** R01 sustituye **6 de 6 en cada una de las 3 semanas**, y R05 **7 de 8** en las semanas 2 y 3.
  - **P2 (H12-A):** la sesión queda **SIN_EFECTO, conservada**, las tres semanas, porque el lateral se rechaza y no hay bajada ni dosis.
  - **P3 (H12-C, N = 2):** también **SIN_EFECTO**: 6 > 2 se rechaza, y el reintento no encuentra ninguna bajada.
  - **Rotación observada en P0:**
    - R01, semana 1: 933 → 129, 143 → 114, 1195 → 144, 101 → 136, 110 → 57, 1099 → 318;
    - semana 2: 129 → 460, 114 → 115, 144 → 145, 136 → **101**, 57 → 60, 318 → 494;
    - semana 3: 460 → **129**, 115 → **114**, 145 → **144**, 101 → **136**, 60 → **57**, 494 → **318**.

    Vuelve **exactamente** a los ejercicios de la semana 1, y en la semana 2 había reintroducido el **101** original. Es una rotación circular A → B → A, sin ninguna bajada de dificultad.
- **13 (otro músculo, mismo patrón).**
  - Histórica, H1 y H12-C lo publican (H2 sigue abierto: ninguna alternativa compara el músculo).
  - Con H12-A se rechaza porque **es lateral**, no por el músculo; el reintento baja la dosis (3).
- **14 (H1).**
  - **Solo la histórica publica** la pérdida del tirón vertical.
  - Con H1, el reintento hace 3 **bajadas** de dificultad del mismo patrón.
  - Con H12-C (N = 2) ni siquiera las bajadas caben, y queda SIN_EFECTO.
- **15 (sin alternativas).** Las cuatro **conservan** la sesión: la conservación sigue siendo una respuesta legítima.

## 3. Resultado agregado (3 semanas, 12 perfiles, 41 sesiones señaladas)

| | P0 histórica | P1 H1 | P2 H1 + H12-A | P3 H1 + H12-C (2) |
|---|---:|---:|---:|---:|
| Sesiones señaladas que cambiaron | 23 | 22 | **14** | 15 |
| Sustituciones | 51 | 49 | **9** | 11 |
| …laterales (misma dificultad) | **42** | 40 | **0** | 8 |
| …a menor dificultad | 9 | 9 | 9 | **3** |
| Cambios de dosis | 8 | 8 | **17** | 8 |
| Distribución de patrones rota | **1** (E10) | 0 | 0 | 0 |
| Intentos de IA (guion) | 54 | 55 | 75 | 72 |
| Rechazos del validador | 13 | 14 | 34 | 31 |
| Filas sesión × semana con SIN_EFECTO | 11 | 12 | **26** | 24 |
| Sesiones no señaladas cambiadas | 0 | 0 | 0 | 0 |
| Dosis subidas | 0 | 0 | 0 | 0 |
| Descansos cambiados | 0 | 0 | 0 | 0 |
| Publicadas bajo el mínimo V20 | 0 | 0 | 0 | 0 |
| Violaciones de linaje | 0 | 0 | 0 | 0 |

**Lectura:**
- **H12-A elimina los 42 laterales** y conserva las 9 bajadas reales. Una parte de lo que antes era lateral se resuelve con **dosis** (de 8 a 17 cambios), donde la política lo permite. Lo que no tiene ni bajada ni dosis queda **conservado** (SIN_EFECTO, de 11 a 26).
- **H12-C (2)** reduce laterales (de 42 a 8), pero también **las bajadas legítimas** (de 9 a 3).
- **Ninguna configuración** produjo cambios fuera de alcance, dosis no autorizadas, descansos cambiados, sesiones bajo el mínimo ni violaciones de linaje.

**Coste de las alternativas:**
- más intentos, porque el guion propone primero lo que la política rechaza;
- con H12-A, el prompt **ya no ofrecería** laterales (`PromptVarianteH12Test`), así que con un modelo real el número de rechazos debería ser menor. **No se ha medido:** la orden excluye Replicate en esta fase.

## 4. Pruebas unitarias de las alternativas

| Clase | Pruebas | Qué fija |
|---|---:|---|
| `V8CPoliticaVarianteH12Test` | 10 | H12-A: 3 → 2 y 2 → 1 aceptadas; lateral 1 → 1 y 2 → 2 rechazadas; **el emparejamiento es necesario** (el multiconjunto acepta {2, 1} → {1, 1}, H12-A no); reasignación; no toca dosis, retirada, fatiga ni dolor. H12-C: 2 sí y 3 no; el dolor no se limita. Cambio de músculo con bajada aceptado (H2 abierto) |
| `PromptVarianteH12Test` | 4 | Con la política histórica, el prompt V1/V2 es idéntico byte a byte; con H12-A solo aparecen sustitutos de menor dificultad; GM principiante → V2 «OMÍTELA»; INVESTIGAR y FATIGA no cambian |

Las 14 pasan. La regresión completa está en `12`.
