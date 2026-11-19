# U02 · 8 semanas (campaña MVP con IA real)

**Perfil:** LOSE_WEIGHT · BEGINNER · días [6, 7] (declarados 2) · casa, equipamiento limitado (peso corporal y banda) · 30 min.
Restricciones: impacto=false, suelo=true, rodillas=true, colgado=false, sobre la cabeza=true. Equipamiento: [body weight, band].

**Plan inicial:** RULE_ENGINE, 3 llamadas. Las 8 semanas tuvieron plan publicado.

Datos sintéticos y servicios reales del backend (PostgreSQL efímero). La adherencia es la calculada por el backend; no demuestra causalidad.

## Semana a semana

| S | Guion | Plan (origen) | Programadas | C / P / O / SR | Adherencia orden · backend · observada | Motivos | Decisión → estado | Intervención | IA | Paso | Retirados | Cambio de perfil |
|---|---|---|---:|---|---|---|---|---|---:|---|---|---|
| 1 | cumplimiento completo | 8 (RULE_ENGINE) | 2 | 2/0/0/0 | 100.00 · 100.00 · 100.00 | - | NO_CHANGE → SIN_ORDEN | {} | 0 | — | — | — |
| 2 | sesion parcial por fatiga | 9 (CONSERVED) | 2 | 1/1/0/0 | 75.00 · 66.66 · 66.66 | FATIGUE | ADAPT_LOCALIZED → SIN_EFECTO | {REVISAR_CARGA_Y_RECUPERACION} | 2 | — | — | — |
| 3 | omision por dificultad excesiva | 19 (CONSERVED) | 2 | 1/0/1/0 | 50.00 · 50.00 · 50.00 | TOO_DIFFICULT | ADAPT_LOCALIZED → RESPALDO | {VARIANTE_MAS_SENCILLA_O_CARGA} | 3 | — | — | — |
| 4 | recupera adherencia | 34 (CONSERVED) | 2 | 2/0/0/0 | 100.00 · 100.00 · 100.00 | - | NO_CHANGE → SIN_ORDEN | {} | 0 | — | — | — |
| 5 | falta de motivacion | 35 (CONSERVED) | 2 | 1/0/1/0 | 50.00 · 50.00 · 50.00 | LACK_OF_MOTIVATION | ADAPT_LOCALIZED → RESPALDO | {INVESTIGAR_CAMBIO_MINIMO} | 3 | — | — | — |
| 6 | cumple con RPE registrado (7) | 51 (CONSERVED) | 2 | 2/0/0/0 | 100.00 · 100.00 · 100.00 | - | NO_CHANGE → SIN_ORDEN | {} | 0 | — | — | — |
| 7 | imprevisto aislado de tiempo | 52 (CONSERVED) | 2 | 1/0/1/0 | 50.00 · 50.00 · 50.00 | LACK_OF_TIME/IMPREVISTO | ADAPT_LOCALIZED → SIN_EFECTO | {CONSERVAR} | 0 | — | — | — |
| 8 | cumple el plan vigente | 53 (CONSERVED) | 2 | 2/0/0/0 | 100.00 · 100.00 · 100.00 | - | NO_CHANGE → SIN_ORDEN | {} | 0 | — | — | — |

C/P/O/SR: completas (≥ 80 %), parciales (30–80 %), omitidas, sin registro. «Adherencia orden» = (C + 0,5·P)/programadas; «backend» = Σ A_s / m (`ADHERENCIA.md`).

## Progresión y reincorporación

Ningún paso publicado.

Reincorporaciones: **ninguna** en la campaña (ver la prueba focalizada en `SIMULACION_MVP_8_USUARIOS_8_SEMANAS.md` §7).

## Sesiones

| S | Sesión (origen) | Fecha | Foco | Min | Acción | Estado | Cumplimiento | Motivo | Contexto/minutos | RPE |
|---|---|---|---|---:|---|---|---:|---|---|---:|
| 1 | 10 (—) | 2026-11-07 | UPPER_BODY | 23 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 1 | 11 (—) | 2026-11-08 | LOWER_BODY | 23 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 2 | 12 (10) | 2026-11-14 | UPPER_BODY | 23 | PARCIAL | PARTIAL | 33.33 | FATIGUE | — | 8 |
| 2 | 13 (11) | 2026-11-15 | LOWER_BODY | 23 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 3 | 30 (12) | 2026-11-21 | UPPER_BODY | 23 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 3 | 31 (13) | 2026-11-22 | LOWER_BODY | 23 | OMITIDA | SKIPPED | — | TOO_DIFFICULT | — | — |
| 4 | 88 (30) | 2026-11-28 | UPPER_BODY | 23 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 4 | 89 (31) | 2026-11-29 | LOWER_BODY | 23 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 5 | 90 (88) | 2026-12-05 | UPPER_BODY | 23 | OMITIDA | SKIPPED | — | LACK_OF_MOTIVATION | — | — |
| 5 | 91 (89) | 2026-12-06 | LOWER_BODY | 23 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 6 | 182 (90) | 2026-12-12 | UPPER_BODY | 23 | HECHO | COMPLETED | 100.00 | — | — | 7 |
| 6 | 183 (91) | 2026-12-13 | LOWER_BODY | 23 | HECHO | COMPLETED | 100.00 | — | — | 7 |
| 7 | 184 (182) | 2026-12-19 | UPPER_BODY | 23 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 7 | 185 (183) | 2026-12-20 | LOWER_BODY | 23 | OMITIDA | SKIPPED | — | LACK_OF_TIME | IMPREVISTO | — |
| 8 | 186 (184) | 2026-12-26 | UPPER_BODY | 23 | HECHO | COMPLETED | 100.00 | — | — | 6 |
| 8 | 187 (185) | 2026-12-27 | LOWER_BODY | 23 | HECHO | COMPLETED | 100.00 | — | — | 6 |

**Violaciones de las comprobaciones independientes:** 0.

La dosis exacta de cada sesión está en `SESIONES_8_SEMANAS.csv`; cada llamada a la IA, en `evidencia/predicciones/` y `ADAPTACIONES_IA_REAL.csv`.
