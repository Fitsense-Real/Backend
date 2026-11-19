# U01 · 8 semanas (campaña MVP con IA real)

**Perfil:** GAIN_MUSCLE · INTERMEDIATE · días [2, 3] (declarados 2) · casa, mancuernas y bandas · 45 min.
Restricciones: impacto=true, suelo=true, rodillas=true, colgado=true, sobre la cabeza=true. Equipamiento: [body weight, dumbbell, band].

**Plan inicial:** RULE_ENGINE, 3 llamadas. Las 8 semanas tuvieron plan publicado.

Datos sintéticos y servicios reales del backend (PostgreSQL efímero). La adherencia es la calculada por el backend; no demuestra causalidad.

## Semana a semana

| S | Guion | Plan (origen) | Programadas | C / P / O / SR | Adherencia orden · backend · observada | Motivos | Decisión → estado | Intervención | IA | Paso | Retirados | Cambio de perfil |
|---|---|---|---:|---|---|---|---|---|---:|---|---|---|
| 1 | cumple todas | 6 (RULE_ENGINE) | 2 | 2/0/0/0 | 100.00 · 100.00 · 100.00 | - | NO_CHANGE → SIN_ORDEN | {} | 0 | — | — | — |
| 2 | omite una por menos tiempo habitual (minutos reales) | 7 (CONSERVED) | 2 | 1/0/1/0 | 50.00 · 50.00 · 50.00 | LACK_OF_TIME/MENOS_TIEMPO_HABITUAL(22 min) | ADAPT_LOCALIZED → SIN_EFECTO | {ACORTAR_O_REORGANIZAR} | 2 | — | — | — |
| 3 | intenta cumplir la sesion adaptada | 12 (CONSERVED) | 2 | 2/0/0/0 | 100.00 · 100.00 · 100.00 | - | NO_CHANGE → SIN_ORDEN | {} | 0 | — | — | — |
| 4 | cumple todas | 13 (CONSERVED) | 2 | 2/0/0/0 | 100.00 · 100.00 · 100.00 | - | NO_CHANGE → SIN_ORDEN | {} | 0 | PROGRESION | — | — |
| 5 | cumple todas (evaluar progresion) | 14 (CONSERVED) | 2 | 2/0/0/0 | 100.00 · 100.00 · 100.00 | - | NO_CHANGE → SIN_ORDEN | {} | 0 | PROGRESION | — | — |
| 6 | dolor en un ejercicio | 15 (CONSERVED) | 2 | 2/0/0/0 | 100.00 · 90.00 · 90.00 | PAIN_OR_DISCOMFORT | ADAPT_LOCALIZED → APLICADA | {SEGURIDAD_ANTE_DOLOR} | 1 | — | 1152@22 | — |
| 7 | cumple (seguridad y elegibilidad de progresion) | 16 (AI) | 2 | 2/0/0/0 | 100.00 · 100.00 · 100.00 | - | NO_CHANGE → SIN_ORDEN | {} | 0 | — | — | — |
| 8 | cumple (posible reincorporacion) | 17 (CONSERVED) | 2 | 2/0/0/0 | 100.00 · 100.00 · 100.00 | - | NO_CHANGE → SIN_ORDEN | {} | 0 | PROGRESION | — | — |

C/P/O/SR: completas (≥ 80 %), parciales (30–80 %), omitidas, sin registro. «Adherencia orden» = (C + 0,5·P)/programadas; «backend» = Σ A_s / m (`ADHERENCIA.md`).

## Progresión y reincorporación

| S | Tipo | Ejercicio | Antes → después | Volumen antes → después (base, techo) | Incremento |
|---|---|---|---|---|---:|
| 4 | PROGRESION | 1152 | `{"reps":10,"series":3,"descanso":90,"segundos":null}` → `{"reps":11,"series":3,"descanso":90,"segundos":null}` | 150 → 153 (150, 165) | 2.00 % |
| 5 | PROGRESION | 134 | `{"reps":13,"series":2,"descanso":90,"segundos":null}` → `{"reps":14,"series":2,"descanso":90,"segundos":null}` | 168 → 170 (168, 184) | 1.19 % |
| 8 | PROGRESION | 1155 | `{"reps":10,"series":3,"descanso":90,"segundos":null}` → `{"reps":11,"series":3,"descanso":90,"segundos":null}` | 150 → 153 (150, 165) | 2.00 % |

Reincorporaciones: **ninguna** en la campaña (ver la prueba focalizada en `SIMULACION_MVP_8_USUARIOS_8_SEMANAS.md` §7).

## Sesiones

| S | Sesión (origen) | Fecha | Foco | Min | Acción | Estado | Cumplimiento | Motivo | Contexto/minutos | RPE |
|---|---|---|---|---:|---|---|---:|---|---|---:|
| 1 | 6 (—) | 2026-11-03 | UPPER_BODY | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 1 | 7 (—) | 2026-11-04 | LOWER_BODY | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 2 | 8 (6) | 2026-11-10 | UPPER_BODY | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 2 | 9 (7) | 2026-11-11 | LOWER_BODY | 32 | OMITIDA | SKIPPED | — | LACK_OF_TIME | MENOS_TIEMPO_HABITUAL 22 min | — |
| 3 | 16 (8) | 2026-11-17 | UPPER_BODY | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 3 | 17 (9) | 2026-11-18 | LOWER_BODY | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 4 | 18 (16) | 2026-11-24 | UPPER_BODY | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 4 | 19 (17) | 2026-11-25 | LOWER_BODY | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 5 | 20 (18) | 2026-12-01 | UPPER_BODY | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 5 | 21 (19) | 2026-12-02 | LOWER_BODY | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 6 | 22 (20) | 2026-12-08 | UPPER_BODY | 32 | HECHO_CON_DOLOR | COMPLETED | 80.00 | PAIN_OR_DISCOMFORT | — | 6 |
| 6 | 23 (21) | 2026-12-09 | LOWER_BODY | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 7 | 24 (22) | 2026-12-15 | UPPER_BODY | 33 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 7 | 25 (23) | 2026-12-16 | LOWER_BODY | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 8 | 26 (24) | 2026-12-22 | UPPER_BODY | 33 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 8 | 27 (25) | 2026-12-23 | LOWER_BODY | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |

**Violaciones de las comprobaciones independientes:** 0.

La dosis exacta de cada sesión está en `SESIONES_8_SEMANAS.csv`; cada llamada a la IA, en `evidencia/predicciones/` y `ADAPTACIONES_IA_REAL.csv`.
