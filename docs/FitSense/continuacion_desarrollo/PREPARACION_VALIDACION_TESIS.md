# Preparación de la validación de la tesis (objetivo 4)

**Todavía no se recogen datos con participantes.** Este documento fija qué debe estar identificado y ser reproducible antes de empezar, y qué falta.

## Problema

La validación del objetivo 4 (adherencia) necesita una versión estable, identificada y reproducible del sistema, y una medición de adherencia que no confunda el comportamiento del participante con las restricciones que impone FitSense.

## Inventario de versiones

| # | Elemento | Estado | Cómo se identifica | Falta |
|---|---|---|---|---|
| 1 | **Backend** | `generation` = `0d11278` (catálogo v1.1 integrado en `50849d0` + V57; ver `docs/FitSense/integracion/catalogo_v11/`) | SHA de `generation` y etiqueta propuesta `backend-tesis-v1` | Crear la etiqueta al congelar. **No se ha desplegado** |
| 2 | **Catálogo** | `FITSENSE-CATALOGO-TECNICO-CONSERVADOR-v1.1`: A 919, B 273, C 27, D 157 | Huella normalizada (`exercises` `20d6a795…`) y Flyway V1…V56 (V57 añade solo constancia de omisiones) | Nada técnico. Siguen abiertas **312 decisiones profesionales** |
| 3 | **Flutter** | Fuera de este repositorio; no se ha tocado en esta fase | Commit o etiqueta del repositorio móvil | Identificar la versión; adaptar el flujo asíncrono (`PROPUESTA_FLUJO_GENERACION.md`) y el timeout de 30 s |
| 4 | **Proveedor de IA** | `gpt-5-mini`, `reasoning_effort=medium`, PR-1.7 y V2.1 (configuración evaluada) | `ConfiguracionEvaluadaTest`/`IT`, y `prompt_version` en cada `input_snapshot` | Decidir H2 (PR-1.9) antes de congelar; si cambia, actualizar la configuración evaluada |
| 5 | **Registro de generaciones** | `weekly_training_plans` (fuente, intentos, modelo, `input_snapshot`), `GENERACION_TIEMPOS` y `REPLICATE_TIMING` | Tablas y registros | Recoger los registros del entorno (Railway) en un almacenamiento fijo |
| 6 | **Registro de adaptaciones** | `plan_adaptation_attempts` (V32), `safe_regeneration_attempts`, `adaptation_reason_codes` | Tablas | Nada |
| 7 | **Registro de sesiones** | `planned_workouts`, `workout_sessions`, `workout_session_exercises` | Tablas | Nada |
| 8 | **Cálculo de adherencia** | Fórmula oficial **sin cambios** (`ADHERENCIA.md`) | Código y documento | Nada; no se modifica |
| 9 | **Causas de omisión** | V54 (sesión que vence bloqueada), V57 (ejercicio impuesto y omisión manual de una sesión bloqueada) | `planned_workouts.system_block_cause`, `workout_session_exercises.system_block_cause` | **Decisión metodológica documentada** (abajo) |
| 10 | **Indicadores de seguridad y factibilidad** | NO_FACTIBLE estructurado, `safe_regeneration_attempts`, exclusiones de dolor (`user_safety_exclusions`) y P-DOLOR-PUB | Tablas | Definir el tablero de indicadores para la tesis |

## Reproducibilidad

Una ejecución es reproducible si se fijan:

- el SHA del backend;
- la huella del catálogo;
- la versión del prompt (`prompt_version`) y del modelo;
- la política (`PLAN-REV-2.0`);
- los interruptores (`ESTADO_INTERRUPT0RES.md`).

Cada plan guarda la instantánea exacta que recibió la IA (`input_snapshot`), así que la entrada se puede reconstruir. La salida de la IA no es determinista. Se reproduce **el contexto y la validación**, no la respuesta del modelo.

## Separación de las pruebas

| Tipo | Qué demuestra | Qué **no** demuestra |
|---|---|---|
| **Funcionales** (regresión: 1540 unitarias + 754 IT) | Que el sistema aplica sus reglas | Que la adherencia mejore |
| **Técnicas** (rejillas 168, 312 y 72; huella; HALL-GM-01) | La factibilidad y la seguridad de la generación y la adaptación con perfiles sintéticos | El efecto en personas |
| **Con perfiles sintéticos e IA real** (GRA, BASE; `PLAN_PRUEBAS_IA_REAL.md`) | Que el proveedor real produce planes válidos con la configuración evaluada | La adherencia real |
| **Evaluación de adherencia con participantes** | El objetivo 4 | — (no iniciada) |

**Las pruebas sintéticas no demuestran una mejora real de la adherencia**, y no se presentan así.

## Tratamiento de las omisiones impuestas (decisión metodológica pendiente)

Con V57 integrado, los registros distinguen de forma persistente:

| Caso | Cómo se reconoce |
|---|---|
| Ejercicio omitido por el participante | `workout_session_exercises.system_block_cause IS NULL` |
| Ejercicio que el sistema impidió hacer por dolor | `system_block_cause` contiene `dolor_registrado` |
| Ejercicio bloqueado por otra restricción | `system_block_cause` con otra causa |
| Sesión entera bloqueada | `planned_workouts.system_block_cause` con alguna causa distinta de `dolor_registrado` |
| Sesión que vence tras quedar bloqueada | `status = SKIPPED`, `skip_reason IS NULL` y `system_block_cause` no nulo |
| Sesión omitida a mano estando bloqueada | `status = SKIPPED`, `skip_reason` declarado y `system_block_cause` no nulo |

**El indicador oficial no cambia.** Hay que decidir, antes de recoger datos, cuál de estas opciones se usa en la tesis:

| Opción | Qué implica |
|---|---|
| **A** (recomendada) | Informar el indicador oficial **y**, como análisis secundario preregistrado, el mismo indicador con las omisiones impuestas fuera del denominador. Las dos cifras salen de los mismos registros |
| **B** | Solo el indicador oficial, declarando la limitación |
| **C** | Cambiar el indicador oficial. **No autorizado**: modifica la metodología |

## Pendientes antes de recoger datos

1. Decidir A o B y dejarlo documentado y firmado.
2. Decidir H2 (evaluación pareada).
3. Implementar el flujo asíncrono y adaptar Flutter, o aceptar la limitación del timeout.
4. Etiquetar la versión (`backend-tesis-v1`) y desplegarla en un entorno de evaluación. **Requiere autorización.**
5. Cerrar la revisión profesional de las decisiones de seguridad abiertas, o declararla como limitación.
6. Proceso ético con participantes: consentimiento y aprobación. Está fuera del alcance técnico.
