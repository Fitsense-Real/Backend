# U08 · 8 semanas (campaña MVP con IA real)

**Perfil:** INCREASE_STRENGTH · BEGINNER · días [1, 3, 5] (declarados 3) · casa, mancuernas ajustables, bandas y banco · 45 min.
Restricciones: impacto=true, suelo=true, rodillas=true, colgado=true, sobre la cabeza=true. Equipamiento: [body weight, dumbbell, band, bench].

**Plan inicial:** AI, 1 llamadas. Las 8 semanas tuvieron plan publicado.

Datos sintéticos y servicios reales del backend (PostgreSQL efímero). La adherencia es la calculada por el backend; no demuestra causalidad.

## Semana a semana

| S | Guion | Plan (origen) | Programadas | C / P / O / SR | Adherencia orden · backend · observada | Motivos | Decisión → estado | Intervención | IA | Paso | Retirados | Cambio de perfil |
|---|---|---|---:|---|---|---|---|---|---:|---|---|---|
| 1 | cumple todas | 55 (AI) | 3 | 3/0/0/0 | 100.00 · 100.00 · 100.00 | - | NO_CHANGE → SIN_ORDEN | {} | 0 | — | — | — |
| 2 | cumple (elegibilidad para progresar) | 56 (CONSERVED) | 3 | 3/0/0/0 | 100.00 · 100.00 · 100.00 | - | NO_CHANGE → SIN_ORDEN | {} | 0 | PROGRESION | — | — |
| 3 | sesion demasiado dificil | 57 (CONSERVED) | 3 | 2/1/0/0 | 83.33 · 83.33 · 83.33 | TOO_DIFFICULT | NO_CHANGE → SIN_ORDEN | {} | 0 | — | — | — |
| 4 | ejecuta el plan modificado | 58 (CONSERVED) | 3 | 3/0/0/0 | 100.00 · 100.00 · 100.00 | - | NO_CHANGE → SIN_ORDEN | {} | 0 | — | — | — |
| 5 | dolor en un ejercicio | 59 (CONSERVED) | 3 | 3/0/0/0 | 100.00 · 95.83 · 95.83 | PAIN_OR_DISCOMFORT | ADAPT_LOCALIZED → APLICADA | {SEGURIDAD_ANTE_DOLOR} | 2 | — | 1103@202 | — |
| 6 | cumple las compatibles | 60 (AI) | 3 | 3/0/0/0 | 100.00 · 100.00 · 100.00 | - | NO_CHANGE → SIN_ORDEN | {} | 0 | — | — | — |
| 7 | menos tiempo habitual para una sesion | 61 (CONSERVED) | 3 | 2/0/1/0 | 66.67 · 66.67 · 66.67 | LACK_OF_TIME/MENOS_TIEMPO_HABITUAL(24 min) | ADAPT_LOCALIZED → APLICADA | {ACORTAR_O_REORGANIZAR} | 2 | — | 864@210 | — |
| 8 | cumple (continuidad y progresion real) | 62 (AI) | 3 | 3/0/0/0 | 100.00 · 100.00 · 100.00 | - | NO_CHANGE → SIN_ORDEN | {} | 0 | — | — | — |

C/P/O/SR: completas (≥ 80 %), parciales (30–80 %), omitidas, sin registro. «Adherencia orden» = (C + 0,5·P)/programadas; «backend» = Σ A_s / m (`ADHERENCIA.md`).

## Progresión y reincorporación

| S | Tipo | Ejercicio | Antes → después | Volumen antes → después (base, techo) | Incremento |
|---|---|---|---|---|---:|
| 2 | PROGRESION | 500 | `{"reps":13,"series":2,"descanso":90,"segundos":null}` → `{"reps":14,"series":2,"descanso":90,"segundos":null}` | 150 → 152 (150, 165) | 1.33 % |

Reincorporaciones: **ninguna** en la campaña (ver la prueba focalizada en `SIMULACION_MVP_8_USUARIOS_8_SEMANAS.md` §7).

## Sesiones

| S | Sesión (origen) | Fecha | Foco | Min | Acción | Estado | Cumplimiento | Motivo | Contexto/minutos | RPE |
|---|---|---|---|---:|---|---|---:|---|---|---:|
| 1 | 190 (—) | 2026-11-02 | FULL_BODY | 34 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 1 | 191 (—) | 2026-11-04 | FULL_BODY | 34 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 1 | 192 (—) | 2026-11-06 | FULL_BODY | 34 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 2 | 193 (190) | 2026-11-09 | FULL_BODY | 34 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 2 | 194 (191) | 2026-11-11 | FULL_BODY | 34 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 2 | 195 (192) | 2026-11-13 | FULL_BODY | 34 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 3 | 196 (193) | 2026-11-16 | FULL_BODY | 34 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 3 | 197 (194) | 2026-11-18 | FULL_BODY | 34 | PARCIAL | PARTIAL | 50.00 | TOO_DIFFICULT | — | 9 |
| 3 | 198 (195) | 2026-11-20 | FULL_BODY | 34 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 4 | 199 (196) | 2026-11-23 | FULL_BODY | 34 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 4 | 200 (197) | 2026-11-25 | FULL_BODY | 34 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 4 | 201 (198) | 2026-11-27 | FULL_BODY | 34 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 5 | 202 (199) | 2026-11-30 | FULL_BODY | 34 | HECHO_CON_DOLOR | COMPLETED | 87.50 | PAIN_OR_DISCOMFORT | — | 6 |
| 5 | 203 (200) | 2026-12-02 | FULL_BODY | 34 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 5 | 204 (201) | 2026-12-04 | FULL_BODY | 34 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 6 | 205 (202) | 2026-12-07 | FULL_BODY | 36 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 6 | 206 (203) | 2026-12-09 | FULL_BODY | 34 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 6 | 207 (204) | 2026-12-11 | FULL_BODY | 34 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 7 | 208 (205) | 2026-12-14 | FULL_BODY | 36 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 7 | 209 (206) | 2026-12-16 | FULL_BODY | 34 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 7 | 210 (207) | 2026-12-18 | FULL_BODY | 34 | OMITIDA | SKIPPED | — | LACK_OF_TIME | MENOS_TIEMPO_HABITUAL 24 min | — |
| 8 | 211 (208) | 2026-12-21 | FULL_BODY | 36 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 8 | 212 (209) | 2026-12-23 | FULL_BODY | 34 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 8 | 213 (210) | 2026-12-25 | FULL_BODY | 24 | HECHO | COMPLETED | 100.00 | — | — | 6 |

**Violaciones de las comprobaciones independientes:** 0.

La dosis exacta de cada sesión está en `SESIONES_8_SEMANAS.csv`; cada llamada a la IA, en `evidencia/predicciones/` y `ADAPTACIONES_IA_REAL.csv`.
