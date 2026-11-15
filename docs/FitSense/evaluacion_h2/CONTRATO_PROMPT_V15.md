# Contrato prompt–V15

## Problema

Lo que se le dice a la IA y lo que se verifica tienen que ser la misma regla, y una prueba tiene que impedir que se separen en silencio.

## La regla (una sola función)

`planning/domain/services/RepartoV15.java`:

| Función | Devuelve |
|---|---|
| `topeMismoGrupo(grupos, nTrabajo)` | `null` si `grupos < 3` o `nTrabajo < 4` (la regla no aplica); si no, `nTrabajo / 2` |
| `tabla(grupos, maxTrabajo)` | `{"4": 2, "5": 2, "6": 3, "7": 3, "8": 4, …}` hasta `maxTrabajo`; vacía si no aplica |

V15 sigue con su propio código, que no cambia. La prueba permanente `ContratoPromptV15IT#sondaV15` lo comprueba con el **validador real**:

- recorre n = 2…6 y k = 1…n del mismo grupo, con 2 complementos;
- la función predice exactamente cuándo rechaza V15.

Si alguien cambia V15 o la función, la prueba falla.

## Qué recibe la IA

| Campo de la instantánea (`input_snapshot`) | PR-1.7 (A, apagado) | PR-1.9 (B, encendido) |
|---|---|---|
| `prompt_version` | `PR-1.7` | `PR-1.9` |
| `suggested_split[].max_per_body_part` | `min_exercises / 2` o `null` | `null` |
| `suggested_split[].max_per_body_part_por_n_trabajo` | (no se serializa) | Tabla de `RepartoV15` |
| `constraints.complementos_no_cuentan` | (no se serializa) | Ids de `CatalogEligibilityPolicy.complementos`, ordenados |
| `constraints.min_exercises_per_session` | Fórmula histórica | `null` con capacidades; si no, `min(histórico, max_colocaciones_dia)` |
| Regla 15 del prompt | «no pongas más de `max_per_body_part` ejercicios del mismo body_part» | «sin contar `complementos_no_cuentan`, ningún body_part puede tener más de la mitad (división entera) de los ejercicios de trabajo; con n ejercicios de trabajo, el tope es el valor de n en `max_per_body_part_por_n_trabajo`» |
| Comprobación final | La frase histórica | La misma regla |

**Fallo rápido:** si el texto histórico cambia y la sustitución no lo encuentra, `aplicarContratoV15` lanza `IllegalStateException`. La variante nunca queda a medias.

## Coherencia generación–validación

| Pieza | Lee el interruptor | Efecto |
|---|---|---|
| `PlanPromptBuilder` | Sí | Texto y versión |
| `ReplicateTrainingPlanGenerator` | Sí (`promptBuilder.contratoV15()`) | Instantánea enviada y etiqueta de la petición |
| `WeeklyTrainingPlanCommandServiceImpl` | Sí | La instantánea persistida es la que recibió la IA |
| `AdaptedWeekCommandService` (adaptación V2.1) | **No** | Sin cambios (esta orden) |
| `PlanDraftValidator` (V15) | No | Sin cambios |

## Resultado

Con el interruptor encendido no queda ninguna contradicción conocida. Con el interruptor apagado, todo es idéntico byte a byte a PR-1.7.

## Pendientes

Ninguno técnico.
