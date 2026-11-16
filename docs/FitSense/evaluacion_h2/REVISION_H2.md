# Revisión de H2 (fase 1)

## Problema

Las instrucciones que recibe la IA para generar la primera semana (prompt PR-1.7) no dicen la misma regla que verifica V15. La IA puede obedecer el prompt y aun así ver su propuesta rechazada. Eso cuesta un reintento (otra predicción de unos 70 s) y, si vuelve a fallar, se publica el plan del motor de reglas.

## Qué se revisó

| Fuente | Versión |
|---|---|
| Expediente original | `docs/FitSense/continuacion_desarrollo/DIAGNOSTICO_H2.md` y `INFORME_CASO_39.md` (rama `audit/caso-usuario-39`) |
| Prompt histórico | `PlanPromptBuilder`, `VERSION = "PR-1.7"`, regla 15 y comprobación final |
| V15 | `PlanDraftValidator.validateFocusCoverage` en `generation` (`2578719`) |
| PR #69 | `fix/h2-contrato-prompt-v15`, actualizada con `generation` (`ff61681`) y con los cambios de esta orden |
| Configuración activa | `application.properties`: `fitsense.planning.policy=PLAN-REV-2.0` y `fitsense.ai.generacion.contrato-v15=false` |

## Qué hace V15 realmente

Lo verifica el código, no la documentación:

1. **Grupos ajenos:** salvo en FULL_BODY, ningún ejercicio de trabajo puede ser de un grupo fuera del enfoque.
2. **Cobertura:** debe cubrir al menos `min(2, grupos con material prescribible, n)` grupos del enfoque.
3. **Regla de la mitad:** solo si el enfoque tiene **3 grupos o más** y la sesión tiene **4 ejercicios de trabajo o más**. En ese caso, ningún grupo puede pasar de `n / 2` (división entera).
4. **Complementos:** los complementos de capacidad (`CatalogEligibilityPolicy.complementos`: cardio y movilidad que solo entran por la capacidad) **no cuentan** en `n` ni en los grupos.

## Las tres contradicciones, una por una

| Punto de la orden (códigos del expediente) | Contradicción | PR-1.7 (histórico) | V15 | ¿Sigue presente con el interruptor apagado? |
|---|---|---|---|---|
| **1 · Límite por grupo** (C2: tope que admite más de lo verificado; C3: regla aplicada y no comunicada) | El tope comunicado no es el verificado | `max_per_body_part = min_exercises_per_session / 2`: un solo número, calculado con el mínimo de la política y no con los `n` ejercicios que la IA decide poner | `n_trabajo / 2`, que depende de `n` | **Sí.** Con PLAN-HIST-1 se comunica un tope que puede superar el real (caso 39: tope 6, real 3). Con **PLAN-REV-2.0**, la política vigente, en los 16 perfiles medidos el tope llega `null`: **no se comunica ninguno**, aunque V15 aplica la regla en 51 sesiones (prueba `iaSimuladaCuatroObjetivos`) |
| **2 · Complementos** | El prompt no dice que no cuentan | Nada | No cuentan en `n` ni en los grupos | **Sí** |
| **3 · Mínimo con capacidades** (C1: mínimo mayor que `max_colocaciones_dia`) | Se pide más de lo que cabe | `min_exercises_per_session` se calcula con `target_sets × target_reps`, aunque gobiernen las dosis por capacidad (V24), y no se acota por `max_colocaciones_dia` | V15 no exige ese mínimo; V20 y V24 gobiernan la dosis | **Sí** |

**Conclusión de la fase 1:** H2 **sigue presente** en la configuración histórica. Con la política vigente, la forma dominante es la **omisión**: la regla de la mitad se aplica y el prompt no la comunica.

## Qué corrige #69

La corrección se aplica **solo en la generación** y **solo con el interruptor encendido**:

| Contradicción | Corrección (PR-1.9) |
|---|---|
| Límite por grupo | Cada sesión de `suggested_split` lleva `max_per_body_part_por_n_trabajo`, la tabla de `RepartoV15.topeMismoGrupo` para n = 4…`max_colocaciones_dia`; `max_per_body_part` pasa a `null`. La regla 15 y la comprobación final dicen la regla tal como la aplica V15 |
| Complementos | `constraints.complementos_no_cuentan` con los ids exactos de `CatalogEligibilityPolicy.complementos` |
| Mínimo con capacidades | `min_exercises_per_session` es `null` cuando gobiernan las capacidades; en los demás casos se acota por `max_colocaciones_dia` |

**Cambio de esta orden:** la adaptación **ya no lee el interruptor**.

- Antes, al encenderlo, la adaptación V2.1 habría recibido otro `min_exercises_per_session` sin haberse evaluado. Era un cambio accidental.
- H2 queda limitado a la generación inicial y a la de cada semana nueva.
- Lo fija `ContratoV15PromptTest#adaptacionNoLeeElInterruptor`.

## Lo que no cambia

- V15, V20, V24, V25, V8C, D8 y P-03.
- Los límites de progresión y de reincorporación.
- El catálogo, la fórmula de adherencia y el esquema JSON de respuesta.
- El motor de reglas, que no lee la instantánea.

**No se modifica V15 para aceptar propuestas que incumplen la regla:** se corrige la instrucción.

## Pendientes

- Evaluación pareada con la IA real: `DISENO_EVALUACION_PAREADA.md`, que **necesita autorización**.
