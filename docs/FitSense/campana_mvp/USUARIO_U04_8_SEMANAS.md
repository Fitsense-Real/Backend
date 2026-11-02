# U04 · 8 semanas (campaña MVP con IA real)

**Perfil:** GENERAL_FITNESS · BEGINNER · días [6] (declarados 1) · casa, peso corporal · 30 min.
Restricciones: impacto=false, suelo=true, rodillas=true, colgado=false, sobre la cabeza=true. Equipamiento: [body weight].

**Plan inicial:** RULE_ENGINE, 3 llamadas. Las 8 semanas tuvieron plan publicado.

Datos sintéticos y servicios reales del backend (PostgreSQL efímero). La adherencia es la calculada por el backend; no demuestra causalidad.

## Semana a semana

| S | Guion | Plan (origen) | Programadas | C / P / O / SR | Adherencia orden · backend · observada | Motivos | Decisión → estado | Intervención | IA | Paso | Retirados | Cambio de perfil |
|---|---|---|---:|---|---|---|---|---|---:|---|---|---|
| 1 | completa su sesion | 1 (RULE_ENGINE) | 1 | 1/0/0/0 | 100.00 · 100.00 · 100.00 | - | NO_CHANGE → SIN_ORDEN | {} | 0 | — | — | — |
| 2 | completa su sesion | 2 (CONSERVED) | 1 | 1/0/0/0 | 100.00 · 100.00 · 100.00 | - | NO_CHANGE → SIN_ORDEN | {} | 0 | PROGRESION | — | — |
| 3 | completa (elegibilidad para progresar) | 3 (CONSERVED) | 1 | 1/0/0/0 | 100.00 · 100.00 · 100.00 | - | NO_CHANGE → SIN_ORDEN | {} | 0 | — | — | — |
| 4 | semana sin registro | 4 (CONSERVED) | 1 | 0/0/0/1 | 0.00 · 0.00 · — | - | INSUFFICIENT_DATA → SIN_ORDEN | {} | 0 | — | — | — |
| 5 | falta recurrente de tiempo con minutos | 5 (CONSERVED) | 1 | 0/0/1/0 | 0.00 · 0.00 · 0.00 | LACK_OF_TIME/MENOS_TIEMPO_HABITUAL(20 min) | ADAPT_EXPANDED → RESPALDO | {ACORTAR_O_REORGANIZAR} | 3 | — | — | — |
| 6 | ejecuta la sesion resultante | 10 (CONSERVED) | 1 | 1/0/0/0 | 100.00 · 100.00 · 100.00 | - | NO_CHANGE → SIN_ORDEN | {} | 0 | — | — | — |
| 7 | falta de motivacion | 11 (CONSERVED) | 1 | 0/0/1/0 | 0.00 · 0.00 · 0.00 | LACK_OF_MOTIVATION | ADAPT_EXPANDED → APLICADA | {INVESTIGAR_CAMBIO_MINIMO} | 2 | — | — | — |
| 8 | cumplimiento final | 23 (AI) | 1 | 1/0/0/0 | 100.00 · 100.00 · 100.00 | - | NO_CHANGE → SIN_ORDEN | {} | 0 | — | — | — |

C/P/O/SR: completas (≥ 80 %), parciales (30–80 %), omitidas, sin registro. «Adherencia orden» = (C + 0,5·P)/programadas; «backend» = Σ A_s / m (`ADHERENCIA.md`).

## Progresión y reincorporación

| S | Tipo | Ejercicio | Antes → después | Volumen antes → después (base, techo) | Incremento |
|---|---|---|---|---|---:|
| 2 | PROGRESION | 1262 | `{"reps":15,"series":2,"descanso":45,"segundos":null}` → `{"reps":16,"series":2,"descanso":45,"segundos":null}` | 89 → 91 (89, 97) | 2.25 % |

Reincorporaciones: **ninguna** en la campaña (ver la prueba focalizada en `SIMULACION_MVP_8_USUARIOS_8_SEMANAS.md` §7).

## Sesiones

| S | Sesión (origen) | Fecha | Foco | Min | Acción | Estado | Cumplimiento | Motivo | Contexto/minutos | RPE |
|---|---|---|---|---:|---|---|---:|---|---|---:|
| 1 | 1 (—) | 2026-11-07 | FULL_BODY | 28 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 2 | 2 (1) | 2026-11-14 | FULL_BODY | 28 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 3 | 3 (2) | 2026-11-21 | FULL_BODY | 29 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 4 | 4 (3) | 2026-11-28 | FULL_BODY | 29 | SIN_REGISTRO | SCHEDULED | — | — | — | — |
| 5 | 5 (4) | 2026-12-05 | FULL_BODY | 29 | OMITIDA | SKIPPED | — | LACK_OF_TIME | MENOS_TIEMPO_HABITUAL 20 min | — |
| 6 | 14 (5) | 2026-12-12 | FULL_BODY | 29 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 7 | 15 (14) | 2026-12-19 | FULL_BODY | 29 | OMITIDA | SKIPPED | — | LACK_OF_MOTIVATION | — | — |
| 8 | 44 (15) | 2026-12-26 | FULL_BODY | 30 | HECHO | COMPLETED | 100.00 | — | — | 6 |

**Violaciones de las comprobaciones independientes:** 0.

La dosis exacta de cada sesión está en `SESIONES_8_SEMANAS.csv`; cada llamada a la IA, en `evidencia/predicciones/` y `ADAPTACIONES_IA_REAL.csv`.
