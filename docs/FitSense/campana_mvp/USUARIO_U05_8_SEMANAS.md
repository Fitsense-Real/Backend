# U05 · 8 semanas (campaña MVP con IA real)

**Perfil:** GENERAL_FITNESS · INTERMEDIATE · días [1, 2, 3, 4, 5, 6, 7] (declarados 7) · gimnasio con restricciones (sin impacto, sin rodillas, sin sobre la cabeza) · 30 min.
Restricciones: impacto=false, suelo=true, rodillas=false, colgado=true, sobre la cabeza=false. Equipamiento: [body weight, dumbbell, barbell, cable, leverage machine, bench, band, stationary bike, treadmill, kettlebell, ez barbell, smith machine].

**Plan inicial:** AI, 1 llamadas. Las 8 semanas tuvieron plan publicado.

Datos sintéticos y servicios reales del backend (PostgreSQL efímero). La adherencia es la calculada por el backend; no demuestra causalidad.

## Semana a semana

| S | Guion | Plan (origen) | Programadas | C / P / O / SR | Adherencia orden · backend · observada | Motivos | Decisión → estado | Intervención | IA | Paso | Retirados | Cambio de perfil |
|---|---|---|---:|---|---|---|---|---|---:|---|---|---|
| 1 | cumple las programadas | 25 (AI) | 6 | 6/0/0/0 | 100.00 · 100.00 · 100.00 | - | NO_CHANGE → SIN_ORDEN | {} | 0 | — | — | — |
| 2 | cumple las programadas | 26 (CONSERVED) | 6 | 6/0/0/0 | 100.00 · 100.00 · 100.00 | - | NO_CHANGE → SIN_ORDEN | {} | 0 | PROGRESION | — | — |
| 3 | dolor en una sesion y falta de tiempo en otra | 27 (CONSERVED) | 6 | 5/0/1/0 | 83.33 · 80.00 · 80.00 | PAIN_OR_DISCOMFORT + LACK_OF_TIME/MENOS_TIEMPO_HABITUAL(22 min) | ADAPT_LOCALIZED → APLICADA | {SEGURIDAD_ANTE_DOLOR} | 2 | — | 177@58 | — |
| 4 | cumplimiento parcial de varias sesiones | 38 (AI) | 6 | 3/3/0/0 | 75.00 · 70.00 · 70.00 | FATIGUE + LACK_OF_TIME + FATIGUE | ADAPT_LOCALIZED → APLICADA | {REVISAR_CARGA_Y_RECUPERACION} | 2 | — | — | — |
| 5 | intenta cambiar restricciones (sin suelo) | 46 (AI) | 6 | 6/0/0/0 | 100.00 · 100.00 · 100.00 | - | NO_CHANGE → SIN_ORDEN | {} | 0 | — | — | RECHAZADO_422 (allowFloorExercises=false): perfil fijo durante el estudio |
| 6 | fatiga recurrente | 47 (CONSERVED) | 6 | 4/2/0/0 | 83.33 · 80.00 · 80.00 | FATIGUE + FATIGUE | NO_CHANGE → SIN_ORDEN | {} | 0 | RECUPERACION | — | — |
| 7 | recupera el cumplimiento | 48 (CONSERVED) | 6 | 6/0/0/0 | 100.00 · 100.00 · 100.00 | - | NO_CHANGE → SIN_ORDEN | {} | 0 | — | — | — |
| 8 | recupera el cumplimiento (progresion o mantenimiento) | 49 (CONSERVED) | 6 | 6/0/0/0 | 100.00 · 100.00 · 100.00 | - | NO_CHANGE → SIN_ORDEN | {} | 0 | RECUPERACION | — | — |

C/P/O/SR: completas (≥ 80 %), parciales (30–80 %), omitidas, sin registro. «Adherencia orden» = (C + 0,5·P)/programadas; «backend» = Σ A_s / m (`ADHERENCIA.md`).

## Progresión y reincorporación

| S | Tipo | Ejercicio | Antes → después | Volumen antes → después (base, techo) | Incremento |
|---|---|---|---|---|---:|
| 2 | PROGRESION | 508 | `{"reps":18,"series":3,"descanso":45,"segundos":null}` → `{"reps":19,"series":3,"descanso":45,"segundos":null}` | 106 → 109 (106, 116) | 2.83 % |
| 6 | RECUPERACION | 508 | `{"reps":18,"series":3,"descanso":45,"segundos":null}` → `{"reps":19,"series":3,"descanso":45,"segundos":null}` | 98 → 101 (106, 116) | 3.06 % |
| 8 | RECUPERACION | 709 | `{"reps":8,"series":2,"descanso":90,"segundos":null}` → `{"reps":10,"series":2,"descanso":90,"segundos":null}` | 101 → 105 (106, 116) | 3.96 % |

Reincorporaciones: **ninguna** en la campaña (ver la prueba focalizada en `SIMULACION_MVP_8_USUARIOS_8_SEMANAS.md` §7).

## Sesiones

| S | Sesión (origen) | Fecha | Foco | Min | Acción | Estado | Cumplimiento | Motivo | Contexto/minutos | RPE |
|---|---|---|---|---:|---|---|---:|---|---|---:|
| 1 | 46 (—) | 2026-11-02 | PUSH | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 1 | 47 (—) | 2026-11-03 | PULL | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 1 | 48 (—) | 2026-11-04 | LEGS | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 1 | 49 (—) | 2026-11-06 | PUSH | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 1 | 50 (—) | 2026-11-07 | PULL | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 1 | 51 (—) | 2026-11-08 | LEGS | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 2 | 52 (46) | 2026-11-09 | PUSH | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 2 | 53 (47) | 2026-11-10 | PULL | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 2 | 54 (48) | 2026-11-11 | LEGS | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 2 | 55 (49) | 2026-11-13 | PUSH | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 2 | 56 (50) | 2026-11-14 | PULL | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 2 | 57 (51) | 2026-11-15 | LEGS | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 3 | 58 (52) | 2026-11-16 | PUSH | 32 | HECHO_CON_DOLOR | COMPLETED | 80.00 | PAIN_OR_DISCOMFORT | — | 6 |
| 3 | 59 (53) | 2026-11-17 | PULL | 32 | OMITIDA | SKIPPED | — | LACK_OF_TIME | MENOS_TIEMPO_HABITUAL 22 min | — |
| 3 | 60 (54) | 2026-11-18 | LEGS | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 3 | 61 (55) | 2026-11-20 | PUSH | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 3 | 62 (56) | 2026-11-21 | PULL | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 3 | 63 (57) | 2026-11-22 | LEGS | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 4 | 104 (58) | 2026-11-23 | PUSH | 30 | PARCIAL | PARTIAL | 40.00 | FATIGUE | — | 8 |
| 4 | 105 (59) | 2026-11-24 | PULL | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 4 | 106 (60) | 2026-11-25 | LEGS | 32 | PARCIAL | PARTIAL | 40.00 | LACK_OF_TIME | — | 8 |
| 4 | 107 (61) | 2026-11-27 | PUSH | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 4 | 108 (62) | 2026-11-28 | PULL | 32 | PARCIAL | PARTIAL | 40.00 | FATIGUE | — | 8 |
| 4 | 109 (63) | 2026-11-29 | LEGS | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 5 | 152 (104) | 2026-11-30 | PUSH | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 5 | 153 (105) | 2026-12-01 | PULL | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 5 | 154 (106) | 2026-12-02 | LEGS | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 5 | 155 (107) | 2026-12-04 | PUSH | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 5 | 156 (108) | 2026-12-05 | PULL | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 5 | 157 (109) | 2026-12-06 | LEGS | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 6 | 158 (152) | 2026-12-07 | PUSH | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 6 | 159 (153) | 2026-12-08 | PULL | 32 | PARCIAL | PARTIAL | 40.00 | FATIGUE | — | 8 |
| 6 | 160 (154) | 2026-12-09 | LEGS | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 6 | 161 (155) | 2026-12-11 | PUSH | 32 | PARCIAL | PARTIAL | 40.00 | FATIGUE | — | 8 |
| 6 | 162 (156) | 2026-12-12 | PULL | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 6 | 163 (157) | 2026-12-13 | LEGS | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 7 | 164 (158) | 2026-12-14 | PUSH | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 7 | 165 (159) | 2026-12-15 | PULL | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 7 | 166 (160) | 2026-12-16 | LEGS | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 7 | 167 (161) | 2026-12-18 | PUSH | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 7 | 168 (162) | 2026-12-19 | PULL | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 7 | 169 (163) | 2026-12-20 | LEGS | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 8 | 170 (164) | 2026-12-21 | PUSH | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 8 | 171 (165) | 2026-12-22 | PULL | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 8 | 172 (166) | 2026-12-23 | LEGS | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 8 | 173 (167) | 2026-12-25 | PUSH | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 8 | 174 (168) | 2026-12-26 | PULL | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 8 | 175 (169) | 2026-12-27 | LEGS | 32 | HECHO | COMPLETED | 100.00 | — | — | 6 |

**Violaciones de las comprobaciones independientes:** 0.

La dosis exacta de cada sesión está en `SESIONES_8_SEMANAS.csv`; cada llamada a la IA, en `evidencia/predicciones/` y `ADAPTACIONES_IA_REAL.csv`.
