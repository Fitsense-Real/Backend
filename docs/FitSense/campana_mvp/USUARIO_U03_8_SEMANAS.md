# U03 · 8 semanas (campaña MVP con IA real)

**Perfil:** INCREASE_STRENGTH · ADVANCED · días [1, 2, 3, 4] (declarados 4) · gimnasio completo · 60 min.
Restricciones: impacto=true, suelo=true, rodillas=true, colgado=true, sobre la cabeza=true. Equipamiento: [body weight, dumbbell, barbell, cable, leverage machine, bench, band, stationary bike, treadmill, kettlebell, ez barbell, smith machine].

**Plan inicial:** AI, 3 llamadas. Las 8 semanas tuvieron plan publicado.

Datos sintéticos y servicios reales del backend (PostgreSQL efímero). La adherencia es la calculada por el backend; no demuestra causalidad.

## Semana a semana

| S | Guion | Plan (origen) | Programadas | C / P / O / SR | Adherencia orden · backend · observada | Motivos | Decisión → estado | Intervención | IA | Paso | Retirados | Cambio de perfil |
|---|---|---|---:|---|---|---|---|---|---:|---|---|---|
| 1 | cumplimiento sostenido | 20 (AI) | 4 | 4/0/0/0 | 100.00 · 100.00 · 100.00 | - | NO_CHANGE → SIN_ORDEN | {} | 0 | — | — | — |
| 2 | cumplimiento sostenido (evaluar progresion) | 21 (CONSERVED) | 4 | 4/0/0/0 | 100.00 · 100.00 · 100.00 | - | NO_CHANGE → SIN_ORDEN | {} | 0 | PROGRESION | — | — |
| 3 | dolor en un ejercicio | 22 (CONSERVED) | 4 | 4/0/0/0 | 100.00 · 95.83 · 95.83 | PAIN_OR_DISCOMFORT | ADAPT_LOCALIZED → APLICADA | {SEGURIDAD_ANTE_DOLOR} | 2 | — | 517@42 | — |
| 4 | cumple el plan compatible con seguridad | 28 (AI) | 4 | 4/0/0/0 | 100.00 · 100.00 · 100.00 | - | NO_CHANGE → SIN_ORDEN | {} | 0 | — | — | — |
| 5 | intenta cambiar disponibilidad a mar, jue, sab y dom | 29 (CONSERVED) | 4 | 4/0/0/0 | 100.00 · 100.00 · 100.00 | - | NO_CHANGE → SIN_ORDEN | {} | 0 | PROGRESION | — | RECHAZADO_422 (dias 2,4,6,7): perfil fijo durante el estudio |
| 6 | intenta cambiar equipamiento (sin barra ni smith) | 30 (CONSERVED) | 4 | 4/0/0/0 | 100.00 · 100.00 · 100.00 | - | NO_CHANGE → SIN_ORDEN | {} | 0 | PROGRESION | — | RECHAZADO_422 (sin barbell ni smith machine): perfil fijo durante el estudio |
| 7 | ejecucion parcial de una sesion | 31 (CONSERVED) | 4 | 3/1/0/0 | 87.50 · 85.72 · 85.72 | FATIGUE | NO_CHANGE → SIN_ORDEN | {} | 0 | — | — | — |
| 8 | cumple (verificar linaje) | 32 (CONSERVED) | 4 | 4/0/0/0 | 100.00 · 100.00 · 100.00 | - | NO_CHANGE → SIN_ORDEN | {} | 0 | — | — | — |

C/P/O/SR: completas (≥ 80 %), parciales (30–80 %), omitidas, sin registro. «Adherencia orden» = (C + 0,5·P)/programadas; «backend» = Σ A_s / m (`ADHERENCIA.md`).

## Progresión y reincorporación

| S | Tipo | Ejercicio | Antes → después | Volumen antes → después (base, techo) | Incremento |
|---|---|---|---|---|---:|
| 2 | PROGRESION | 887 | `{"reps":10,"series":2,"descanso":90,"segundos":null}` → `{"reps":11,"series":2,"descanso":90,"segundos":null}` | 132 → 134 (132, 145) | 1.52 % |
| 5 | PROGRESION | 1018 | `{"reps":13,"series":3,"descanso":90,"segundos":null}` → `{"reps":14,"series":3,"descanso":90,"segundos":null}` | 210 → 213 (210, 231) | 1.43 % |
| 6 | PROGRESION | 556 | `{"reps":10,"series":3,"descanso":90,"segundos":null}` → `{"reps":11,"series":3,"descanso":90,"segundos":null}` | 120 → 123 (132, 145) | 2.50 % |

Reincorporaciones: **ninguna** en la campaña (ver la prueba focalizada en `SIMULACION_MVP_8_USUARIOS_8_SEMANAS.md` §7).

## Sesiones

| S | Sesión (origen) | Fecha | Foco | Min | Acción | Estado | Cumplimiento | Motivo | Contexto/minutos | RPE |
|---|---|---|---|---:|---|---|---:|---|---|---:|
| 1 | 32 (—) | 2026-11-02 | UPPER_BODY | 42 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 1 | 33 (—) | 2026-11-03 | LOWER_BODY | 49 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 1 | 34 (—) | 2026-11-04 | UPPER_BODY | 43 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 1 | 35 (—) | 2026-11-05 | LOWER_BODY | 49 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 2 | 36 (32) | 2026-11-09 | UPPER_BODY | 42 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 2 | 37 (33) | 2026-11-10 | LOWER_BODY | 49 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 2 | 38 (34) | 2026-11-11 | UPPER_BODY | 43 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 2 | 39 (35) | 2026-11-12 | LOWER_BODY | 49 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 3 | 40 (36) | 2026-11-16 | UPPER_BODY | 42 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 3 | 41 (37) | 2026-11-17 | LOWER_BODY | 49 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 3 | 42 (38) | 2026-11-18 | UPPER_BODY | 43 | HECHO_CON_DOLOR | COMPLETED | 83.33 | PAIN_OR_DISCOMFORT | — | 6 |
| 3 | 43 (39) | 2026-11-19 | LOWER_BODY | 49 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 4 | 64 (40) | 2026-11-23 | UPPER_BODY | 42 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 4 | 65 (41) | 2026-11-24 | LOWER_BODY | 49 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 4 | 66 (42) | 2026-11-25 | UPPER_BODY | 48 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 4 | 67 (43) | 2026-11-26 | LOWER_BODY | 49 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 5 | 68 (64) | 2026-11-30 | UPPER_BODY | 42 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 5 | 69 (65) | 2026-12-01 | LOWER_BODY | 49 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 5 | 70 (66) | 2026-12-02 | UPPER_BODY | 48 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 5 | 71 (67) | 2026-12-03 | LOWER_BODY | 49 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 6 | 72 (68) | 2026-12-07 | UPPER_BODY | 42 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 6 | 73 (69) | 2026-12-08 | LOWER_BODY | 49 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 6 | 74 (70) | 2026-12-09 | UPPER_BODY | 48 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 6 | 75 (71) | 2026-12-10 | LOWER_BODY | 49 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 7 | 76 (72) | 2026-12-14 | UPPER_BODY | 42 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 7 | 77 (73) | 2026-12-15 | LOWER_BODY | 49 | PARCIAL | PARTIAL | 42.86 | FATIGUE | — | 8 |
| 7 | 78 (74) | 2026-12-16 | UPPER_BODY | 48 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 7 | 79 (75) | 2026-12-17 | LOWER_BODY | 49 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 8 | 80 (76) | 2026-12-21 | UPPER_BODY | 42 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 8 | 81 (77) | 2026-12-22 | LOWER_BODY | 49 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 8 | 82 (78) | 2026-12-23 | UPPER_BODY | 48 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 8 | 83 (79) | 2026-12-24 | LOWER_BODY | 49 | HECHO | COMPLETED | 100.00 | — | — | 6 |

**Violaciones de las comprobaciones independientes:** 0.

La dosis exacta de cada sesión está en `SESIONES_8_SEMANAS.csv`; cada llamada a la IA, en `evidencia/predicciones/` y `ADAPTACIONES_IA_REAL.csv`.
