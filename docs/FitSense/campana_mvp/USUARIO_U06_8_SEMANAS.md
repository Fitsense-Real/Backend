# U06 · 8 semanas (campaña MVP con IA real)

**Perfil:** GAIN_MUSCLE · ADVANCED · días [1, 3, 4, 6, 7] (declarados 5) · gimnasio completo · 60 min.
Restricciones: impacto=true, suelo=true, rodillas=true, colgado=true, sobre la cabeza=true. Equipamiento: [body weight, dumbbell, barbell, cable, leverage machine, bench, band, stationary bike, treadmill, kettlebell, ez barbell, smith machine].

**Plan inicial:** SIN_PLAN. **Corte:** sin plan inicial (SIN_PLAN: PlanGenerationFailedException: No se pudo generar un plan valido para el usuario 6. Ultimos problemas: V15: el entrenamiento del 2026-11-05 tiene enfoque LEGS y solo trabaja [upper legs]. Debe cubrir al menos 2 grupos distintos de: [upper legs, lower legs]. | V20: el entrenamiento del 2026-11-08 dura unos 2142 s y el minimo es 2520 s (70 % de 60 minutos): le faltan 378 s. Agrega un ejercicio del enfoque o una serie a los que ya tiene, no descanso.).

Datos sintéticos y servicios reales del backend (PostgreSQL efímero). La adherencia es la calculada por el backend; no demuestra causalidad.

## Semana a semana

| S | Guion | Plan (origen) | Programadas | C / P / O / SR | Adherencia orden · backend · observada | Motivos | Decisión → estado | Intervención | IA | Paso | Retirados | Cambio de perfil |
|---|---|---|---:|---|---|---|---|---|---:|---|---|---|
| 1 | cumplimiento sostenido | — (-) | 0 | 0/0/0/0 | — · — · — | - | - → NO_EJECUTADA | - | 0 | — | — | — |
| 2 | cumplimiento sostenido | — (-) | 0 | 0/0/0/0 | — · — · — | - | - → NO_EJECUTADA | - | 0 | — | — | — |
| 3 | cumple (posible progresion gradual) | — (-) | 0 | 0/0/0/0 | — · — · — | - | - → NO_EJECUTADA | - | 0 | — | — | — |
| 4 | cumple con RPE normal (6) | — (-) | 0 | 0/0/0/0 | — · — · — | - | - → NO_EJECUTADA | - | 0 | — | — | — |
| 5 | omision localizada por menos tiempo habitual | — (-) | 0 | 0/0/0/0 | — · — · — | - | - → NO_EJECUTADA | - | 0 | — | — | — |
| 6 | cumple el plan adaptado | — (-) | 0 | 0/0/0/0 | — · — · — | - | - → NO_EJECUTADA | - | 0 | — | — | — |
| 7 | cumple (progresion o reincorporacion) | — (-) | 0 | 0/0/0/0 | — · — · — | - | - → NO_EJECUTADA | - | 0 | — | — | — |
| 8 | cumple (limites de volumen y mantenimiento) | — (-) | 0 | 0/0/0/0 | — · — · — | - | - → NO_EJECUTADA | - | 0 | — | — | — |

C/P/O/SR: completas (≥ 80 %), parciales (30–80 %), omitidas, sin registro. «Adherencia orden» = (C + 0,5·P)/programadas; «backend» = Σ A_s / m (`ADHERENCIA.md`).

## Progresión y reincorporación

Ningún paso publicado.

Reincorporaciones: **ninguna** en la campaña (ver la prueba focalizada en `SIMULACION_MVP_8_USUARIOS_8_SEMANAS.md` §7).

## Sesiones

| S | Sesión (origen) | Fecha | Foco | Min | Acción | Estado | Cumplimiento | Motivo | Contexto/minutos | RPE |
|---|---|---|---|---:|---|---|---:|---|---|---:|

**Violaciones de las comprobaciones independientes:** 0.

La dosis exacta de cada sesión está en `SESIONES_8_SEMANAS.csv`; cada llamada a la IA, en `evidencia/predicciones/` y `ADAPTACIONES_IA_REAL.csv`.
