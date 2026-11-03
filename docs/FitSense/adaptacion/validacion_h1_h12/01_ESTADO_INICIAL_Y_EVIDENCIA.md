# 01 · Estado inicial y evidencia (fase 1)

Verificado el 03/11/2026, antes de modificar nada.

## 1. Ramas, *commits* y bases reales

| PR | Rama | Cabeza | Base | CI (`verify`, `mvn -B -ntp verify -Pintegration`) |
|---|---|---|---|---|
| — | `generation` (backend estable) | `0974fb3eafc39e8b7397b4dbfe7c4405e4847440` = referencia histórica de la orden | — | — |
| #28 | `lab/experimento-kora-20usuarios` | `0f17ac770a94dda2b31fc4ec384e36cff390a117` | `generation` (`0974fb3`) | [41570491767](https://github.com/Fitsense-Real/Backend/actions/runs/41570491767): **success** |
| #29 | `experiment/adaptation-prompt-v2` | `51c145b2887e621acc52fc81605f58e64edd0a5c` | rama de #28 (`0f17ac7`) | [41577492912](https://github.com/Fitsense-Real/Backend/actions/runs/41577492912): **success** |
| #30 | `experiment/adaptation-v2-quality-validation` | `7580f08acebd57adb94bac9e74b6d783fa9863ac` | rama de #29 (`51c145b`) | [41637134939](https://github.com/Fitsense-Real/Backend/actions/runs/41637134939): **success** |

- Los tres están **abiertos, en borrador y sin fusionar**. `mergeable_state = clean`.
- `generation` es antecesor de #28; #28, de #29; y #29, de #30.

**Rama de este trabajo:** `experiment/adaptation-functional-safety-h1-h12`, creada desde `7580f08`, la cabeza verificada de #30. Es la base de evaluación más reciente.

## 2. Evidencias intactas

Los árboles git de las carpetas de evidencia son **idénticos** en su *commit* de origen y en el punto de partida de esta rama. Ningún archivo difiere.

| Carpeta | Árbol en su PR | Árbol aquí | Archivos | Diferencias |
|---|---|---|---:|---:|
| `short_paper_ieee/experimento_final_20_usuarios` (#28) | `6b44c720…` @ `0f17ac7` | `6b44c720…` | 143 | 0 |
| `short_paper_ieee/experimento_prompt_v2` (#29) | `efa9c274…` @ `51c145b` | `efa9c274…` | 216 | 0 |
| `short_paper_ieee/evaluacion_calidad_adaptacion_v2` (#30) | `fc2f3ba8…` @ `7580f08` | `fc2f3ba8…` | 255 | 0 |

**Ninguna evidencia anterior se modifica en esta rama.** Lo nuevo va solo en `docs/FitSense/adaptacion/validacion_h1_h12/`.

## 3. Hallazgos de partida (de `evaluacion_calidad_adaptacion_v2/08`)

| # | Hallazgo | Estado al empezar |
|---|---|---|
| H1 | V8C comprueba el patrón **por conjunto**: una sesión puede perder un patrón y publicarse (U04 S2, #12b) | Hueco demostrado, no observado con Replicate |
| H2 | V8C no compara el músculo principal: 8 de 57 sustituciones lo cambian (15 más en la cohorte R) | Pendiente de revisión profesional |
| H3 | Sustituciones de VARIANTE con la misma dificultad (la original ya tiene la mínima) | Decisión de política |
| H7 | Clasificación discutible de 580, 775 y 780 | Catálogo, revisión profesional |
| H12 | Sustitución lateral de casi toda la sesión, repetida cada semana (R01, R05) | Decisión de política |
| H13 | 862 (*gorilla chin*, abdominales) clasificado como VERTICAL_PULL | Catálogo, revisión profesional |

## 4. Versiones de los validadores y configuración

- **Sin cambios en `src/main` desde #29** (`51c145b..7580f08`: 0 archivos).
- Respecto a `generation`, solo cambian los 7 archivos del prompt V2 de #29, detrás de su indicador.

**V8C (`ConservationValidator`) al empezar:**
- Sustituir solo lo autorizan `VARIANTE_MAS_SENCILLA_O_CARGA` y `SEGURIDAD_ANTE_DOLOR`; el resto de intervenciones lo rechaza. `ACORTAR_O_REORGANIZAR` solo puede **retirar** una colocación de relleno.
- **Dolor:** rama propia. No exige patrón, prohíbe más añadidos que retirados y no permite que suba el volumen.
- **Dificultad:** cada añadido tiene que tener un patrón que esté **entre** los de los retirados (conjunto).
- No compara músculo, dificultad relativa, número de sustituciones ni historial.

**V20, V24 y el resto de V1–V25:** sin cambios. La duración mínima es `ceil(min × 60 × 70 / 100)`, con igualdad permitida. V24 aplica 4D (sin reducciones en GAIN_MUSCLE).

**Configuración por defecto** (`application.properties` y valores `@Value`):

| Propiedad | Valor |
|---|---|
| `fitsense.ai.adaptation.prompt-version` | `v1` → **V2 apagado** |
| `fitsense.planning.policy` | `PLAN-HIST-1` |
| `fitsense.planning.policy.allow-candidates` | `false` → **ninguna política experimental (PR #17) activa** |
| `fitsense.adaptation.progression.enabled` | `false` |
| `fitsense.adaptation.capability-validation.enabled` | `false` |
| `fitsense.coverage.enforcement` / `pregeneration` | `OBSERVE` |
| `fitsense.ai.enabled` | `false` (variable de entorno) |

**Replicate:** no se usa en esta fase. Todas las ejecuciones de esta carpeta son sin red (determinista o guion).
