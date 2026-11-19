# U07 · 8 semanas (campaña MVP con IA real)

**Perfil:** LOSE_WEIGHT · INTERMEDIATE · días [1, 2, 3, 5, 6, 7] (declarados 6) · gimnasio sin barra, smith, kettlebell ni ez · 45 min.
Restricciones: impacto=true, suelo=true, rodillas=true, colgado=true, sobre la cabeza=true. Equipamiento: [body weight, dumbbell, cable, leverage machine, bench, treadmill, stationary bike].

**Plan inicial:** AI, 1 llamadas. Las 8 semanas tuvieron plan publicado.

Datos sintéticos y servicios reales del backend (PostgreSQL efímero). La adherencia es la calculada por el backend; no demuestra causalidad.

## Semana a semana

| S | Guion | Plan (origen) | Programadas | C / P / O / SR | Adherencia orden · backend · observada | Motivos | Decisión → estado | Intervención | IA | Paso | Retirados | Cambio de perfil |
|---|---|---|---:|---|---|---|---|---|---:|---|---|---|
| 1 | cumplimiento completo | 36 (AI) | 6 | 6/0/0/0 | 100.00 · 100.00 · 100.00 | - | NO_CHANGE → SIN_ORDEN | {} | 0 | — | — | — |
| 2 | omite algunas por cambio de horario | 37 (CONSERVED) | 6 | 4/0/2/0 | 66.67 · 66.67 · 66.67 | SCHEDULE_CHANGE + SCHEDULE_CHANGE | ADAPT_LOCALIZED → SIN_EFECTO | {REPROGRAMAR_DIAS} | 2 | — | — | — |
| 3 | fatiga y sesiones parciales | 39 (CONSERVED) | 6 | 4/2/0/0 | 83.33 · 83.33 · 83.33 | FATIGUE + FATIGUE | NO_CHANGE → SIN_ORDEN | {} | 0 | — | — | — |
| 4 | cumple la planificacion resultante | 40 (CONSERVED) | 6 | 6/0/0/0 | 100.00 · 100.00 · 100.00 | - | NO_CHANGE → SIN_ORDEN | {} | 0 | — | — | — |
| 5 | omite una por falta de motivacion | 41 (CONSERVED) | 6 | 5/0/1/0 | 83.33 · 83.33 · 83.33 | LACK_OF_MOTIVATION | NO_CHANGE → SIN_ORDEN | {} | 0 | — | — | — |
| 6 | intenta cambiar equipamiento (sin cable) | 42 (CONSERVED) | 6 | 6/0/0/0 | 100.00 · 100.00 · 100.00 | - | NO_CHANGE → SIN_ORDEN | {} | 0 | — | — | RECHAZADO_422 (sin cable): perfil fijo durante el estudio |
| 7 | recupera el cumplimiento | 43 (CONSERVED) | 6 | 6/0/0/0 | 100.00 · 100.00 · 100.00 | - | NO_CHANGE → SIN_ORDEN | {} | 0 | PROGRESION | — | — |
| 8 | cumple (progresion, mantenimiento y cardio) | 44 (CONSERVED) | 6 | 6/0/0/0 | 100.00 · 100.00 · 100.00 | - | NO_CHANGE → SIN_ORDEN | {} | 0 | PROGRESION | — | — |

C/P/O/SR: completas (≥ 80 %), parciales (30–80 %), omitidas, sin registro. «Adherencia orden» = (C + 0,5·P)/programadas; «backend» = Σ A_s / m (`ADHERENCIA.md`).

## Progresión y reincorporación

| S | Tipo | Ejercicio | Antes → después | Volumen antes → después (base, techo) | Incremento |
|---|---|---|---|---|---:|
| 7 | PROGRESION | 341 | `{"reps":6,"series":3,"descanso":150,"segundos":null}` → `{"reps":7,"series":3,"descanso":150,"segundos":null}` | 96 → 99 (96, 105) | 3.13 % |
| 8 | PROGRESION | 354 | `{"reps":6,"series":3,"descanso":150,"segundos":null}` → `{"reps":7,"series":3,"descanso":150,"segundos":null}` | 96 → 99 (96, 105) | 3.13 % |

Reincorporaciones: **ninguna** en la campaña (ver la prueba focalizada en `SIMULACION_MVP_8_USUARIOS_8_SEMANAS.md` §7).

## Sesiones

| S | Sesión (origen) | Fecha | Foco | Min | Acción | Estado | Cumplimiento | Motivo | Contexto/minutos | RPE |
|---|---|---|---|---:|---|---|---:|---|---|---:|
| 1 | 92 (—) | 2026-11-02 | PUSH | 51 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 1 | 93 (—) | 2026-11-03 | PULL | 51 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 1 | 94 (—) | 2026-11-04 | LEGS | 51 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 1 | 95 (—) | 2026-11-06 | PUSH | 51 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 1 | 96 (—) | 2026-11-07 | PULL | 51 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 1 | 97 (—) | 2026-11-08 | LEGS | 51 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 2 | 98 (92) | 2026-11-09 | PUSH | 51 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 2 | 99 (93) | 2026-11-10 | PULL | 51 | OMITIDA | SKIPPED | — | SCHEDULE_CHANGE | — | — |
| 2 | 100 (94) | 2026-11-11 | LEGS | 51 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 2 | 101 (95) | 2026-11-13 | PUSH | 51 | OMITIDA | SKIPPED | — | SCHEDULE_CHANGE | — | — |
| 2 | 102 (96) | 2026-11-14 | PULL | 51 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 2 | 103 (97) | 2026-11-15 | LEGS | 51 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 3 | 110 (98) | 2026-11-16 | PUSH | 51 | PARCIAL | PARTIAL | 50.00 | FATIGUE | — | 8 |
| 3 | 111 (99) | 2026-11-17 | PULL | 51 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 3 | 112 (100) | 2026-11-18 | LEGS | 51 | PARCIAL | PARTIAL | 50.00 | FATIGUE | — | 8 |
| 3 | 113 (101) | 2026-11-20 | PUSH | 51 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 3 | 114 (102) | 2026-11-21 | PULL | 51 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 3 | 115 (103) | 2026-11-22 | LEGS | 51 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 4 | 116 (110) | 2026-11-23 | PUSH | 51 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 4 | 117 (111) | 2026-11-24 | PULL | 51 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 4 | 118 (112) | 2026-11-25 | LEGS | 51 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 4 | 119 (113) | 2026-11-27 | PUSH | 51 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 4 | 120 (114) | 2026-11-28 | PULL | 51 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 4 | 121 (115) | 2026-11-29 | LEGS | 51 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 5 | 122 (116) | 2026-11-30 | PUSH | 51 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 5 | 123 (117) | 2026-12-01 | PULL | 51 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 5 | 124 (118) | 2026-12-02 | LEGS | 51 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 5 | 125 (119) | 2026-12-04 | PUSH | 51 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 5 | 126 (120) | 2026-12-05 | PULL | 51 | OMITIDA | SKIPPED | — | LACK_OF_MOTIVATION | — | — |
| 5 | 127 (121) | 2026-12-06 | LEGS | 51 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 6 | 128 (122) | 2026-12-07 | PUSH | 51 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 6 | 129 (123) | 2026-12-08 | PULL | 51 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 6 | 130 (124) | 2026-12-09 | LEGS | 51 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 6 | 131 (125) | 2026-12-11 | PUSH | 51 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 6 | 132 (126) | 2026-12-12 | PULL | 51 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 6 | 133 (127) | 2026-12-13 | LEGS | 51 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 7 | 134 (128) | 2026-12-14 | PUSH | 51 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 7 | 135 (129) | 2026-12-15 | PULL | 51 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 7 | 136 (130) | 2026-12-16 | LEGS | 51 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 7 | 137 (131) | 2026-12-18 | PUSH | 51 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 7 | 138 (132) | 2026-12-19 | PULL | 51 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 7 | 139 (133) | 2026-12-20 | LEGS | 51 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 8 | 140 (134) | 2026-12-21 | PUSH | 51 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 8 | 141 (135) | 2026-12-22 | PULL | 51 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 8 | 142 (136) | 2026-12-23 | LEGS | 51 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 8 | 143 (137) | 2026-12-25 | PUSH | 51 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 8 | 144 (138) | 2026-12-26 | PULL | 51 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 8 | 145 (139) | 2026-12-27 | LEGS | 51 | HECHO | COMPLETED | 100.00 | — | — | 6 |

**Violaciones de las comprobaciones independientes:** 0.

La dosis exacta de cada sesión está en `SESIONES_8_SEMANAS.csv`; cada llamada a la IA, en `evidencia/predicciones/` y `ADAPTACIONES_IA_REAL.csv`.
