# 06 · Versión final propuesta y presupuesto (pendiente de aprobación)

## Versión

| Elemento | Propuesta |
|---|---|
| Rama | `eval/final-ieee-k20-v2`: **candidata posterior**, nueva respecto de la evaluación congelada `a31c36b`, que se conserva íntegra |
| Base | `eval/final-ieee-k20` (`generation` `76e15c5` + #38 + #39 + P3) |
| Cambios en `src/main` | El prompt (V2 portado, `109c3a3`, y V2.1) y **4D v3.4** (aprobada): tope de colocaciones de GAIN_MUSCLE derivado de V20 por encima de 60 min (`ADENDA_4D_v3_4.md`). **Ningún otro cambio de regla, validador, política, migración ni catálogo.** C-8 (53–59 min) **no** está integrado |
| Configuración | Límite `preferencia` · retirada `on` · progresión `off` · V8C multiconjunto · PLAN-HIST-1 · `MVP-1.8` · **`fitsense.ai.adaptation.prompt-version=v2.1`** (o `v2`, según el diagnóstico de desarrollo) |
| Modelo | `gpt-5-mini` sobre `openai/gpt-5-structured`, esfuerzo `medium`, 3 intentos, `prefer-wait=25` |
| Oráculo | v2 (`k20_adaptacion_real/herramientas`, 23 pruebas). Igual que el congelado, salvo la regla del músculo objetivo (ALT) |
| SHA | Se fija en `07` al congelar, **después** del diagnóstico de desarrollo y de tu elección de prompt |

## Etapas con Replicate (ninguna ejecutada)

| Etapa | Qué | Cohorte | Predicciones máximas | Tope USD (a 0,05 por predicción) | Gasto esperado (observado ≈ 0,015 por predicción en C2) |
|---|---|---|---:|---:|---:|
| **DEV-R** (desarrollo, no es evaluación) | Una corrida con **V2** y otra con **V2.1** sobre U01–U20 (8 ciclos con IA, hasta 3 intentos) para elegir el prompt | U01–U20 | 2 × 24 = **48** | **2,40** | ≈ 0,25 |
| **R** | Evaluación congelada | U01–U20 + **J01–J16** (8 + 16 ciclos con IA). H01–H16 ya no: están expuestos | 24 + 48 = **72** | **3,60** | ≈ 0,40 |
| **R′** | Repetición completa de R | Ídem | **72** | **3,60** | ≈ 0,40 |
| **Total** | | | **192** | **9,60** | **≈ 1,05** |

**Reglas del presupuesto:**
- Cada predicción se cuenta **antes** de hacerse.
- Al llegar al tope por corrida, no hay más llamadas. La corrida termina con respaldo y se registra.
- R y R′ exigen `autorizacion-replicate=SI`.
- Hay una comprobación gratuita previa (`GET /v1/account` = 200).
- Las corridas no se repiten de forma selectiva.
- Una corrida que falle por causas técnicas se conserva y se repite completa una sola vez, con registro.

**Opcional:** la generación inicial con IA (20 gemelos, ≤ 60 predicciones / USD 3) como bloque separado. Recomiendo **no** incluirla en esta fase: el objetivo es la adaptación.

## Lo que necesito que apruebes

1. **DEV-R:** 48 predicciones / USD 2,40, V2 frente a V2.1, sobre U01–U20 (desarrollo).
2. **R + R′:** 144 predicciones / USD 7,20, sobre U01–U20 + J01–J16, **después** de congelar.
3. **Criterios del `07`**, antes de DEV-R.
4. **C-4 a C-6 y C-8** (`04`): por defecto **no** se aplican. C-7 ya está aprobado e integrado (4D v3.4).

El plan detallado de DEV-R (comandos, tope y dimensiones del diagnóstico) está en `A2_PLAN_DEV_R.md`.
