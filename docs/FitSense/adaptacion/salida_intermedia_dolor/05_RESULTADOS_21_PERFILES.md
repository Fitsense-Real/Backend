# 05 · Resultados de los 21 perfiles con A, B, C y D

Laboratorio `SinSustitutoDolor24CasosIT`:
- mismos perfiles, semana (2026-11-02), `user_id` y semillas congelados;
- flujo real de extremo a extremo con PostgreSQL efímero, servicios reales y sin Replicate.

El detalle por perfil está en `05_RESULTADOS_21_PERFILES.csv`:

| Grupo de columnas | Contenido |
|---|---|
| Desenlace | desenlace en A, B, C, D y D con B |
| Respaldo | sustituto con A |
| Trabajo y dosis | trabajo antes y después, dosis cambiadas |
| Registro | estado V32, linaje, API |
| Continuidad | semana siguiente, excluidos y motivo cuando D no retira |

Las filas en bruto están en `evidencia/flujo_135_22_perfiles_{A,B,C,D,DB}.csv`.

| Configuración | Comportamiento |
|---|---|
| A | política histórica (`difficulty-guard=off`) |
| B | límite (`on`) |
| C | límite + preferencia (`preferencia`) |
| **D** | **C + retirada localizada** (`preferencia` + `pain-localized-removal=on`) |
| D con B | `on` + `pain-localized-removal=on`, para comprobar la retirada también con B |

## Recuento

| Desenlace | A | B | C | **D** | D con B |
|---|---:|---:|---:|---:|---:|
| Sustitución directa SUST-SEG-1 (780 dif 2 / 663 dif 3) | 21 | 0 | 0 | 0 | 0 |
| **Retirada localizada válida** | — | — | — | **3** | **3** |
| Regeneración completa P-03 | 0 | 14 | 14 | 13 | 13 |
| **Semana sin plan** | 0 | 7 | 7 | **5** | 5 |
| Plan inicial no válido (R0548, V20, independiente) | 1 | 1 | 1 | 1 | 1 |

Por grupo:

| Grupo | Con B y C | Con D |
|---|---|---|
| **Los 7 sin plan** | 7 sin plan | **2 recuperados** por retirada (R0521 y R0573); 5 siguen sin plan (R0524, R0527, R0570, R0574 por V20; R0545 por sesión no señalada) |
| **Los 14 regenerados** | 14 regenerados | **1** pasa a retirada (R0561); 13 siguen regenerados |

D y D con B son **idénticos**: la retirada se comporta igual con B que con C.

## Los tres casos recuperados con D

| Perfil | Sesión | Minutos | Trabajo semanal | Dosis cambiadas | Estado V32 | Linaje | API | Semana siguiente |
|---|---|---|---|---:|---|---|---|---|
| R0521 (GF · intermedio · 6 · 30) | 135 retirado | 34 → 27 | 774 → 756 reps · 2.640 → 2.640 s | 0 | RESPALDO / CONSERVED + `RETIRADA_SEGURA` | 6/6 | RESPALDO_SIN_IA | NO_CHANGE, plan, sin el 135 |
| R0573 (GF · avanzado · 7 · 30) | 135 retirado | 34 → 27 | 848 → 830 reps · 2.460 → 2.460 s | 0 | ídem | 6/6 | RESPALDO_SIN_IA | ídem |
| R0561 (FU · avanzado · 7 · 30) | 135 retirado | 32 → 25 | 452 → 434 reps | 0 | ídem | 6/6 | RESPALDO_SIN_IA | ídem |

## Los 14 regenerados: ¿la retirada conserva más entrenamiento?

**Solo en 1 de los 14 (R0561) la retirada es válida.** En los 13 restantes la sesión sin el 135 incumple V20, así que se mantiene la regeneración P-03. Hay dos maneras de mirar R0561:

| | P-03 (B o C) | Retirada (D) |
|---|---|---|
| Plan | semana nueva generada por reglas desde dosis base | **la misma semana medida** sin el 135 |
| Linaje V31 | 0/6 | **6/6** |
| Repeticiones semanales | 452 (igual a la medida) | 434 (−18, las del 135) |
| Continuidad con lo que la persona ya hacía | se pierde | **se conserva** (mismos ejercicios, mismas dosis) |

Con la retirada, la persona sigue la semana que ya conocía y pierde solo el ejercicio que le dolió. Con P-03 recibe una semana distinta, aunque con el mismo volumen total.
