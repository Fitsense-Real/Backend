# P2-D: regresión de la adaptación ante reclasificaciones, complementos y bloqueos

**Fecha:** 23/10/2026 · **Prueba:** `src/test/java/main/web/services/fitsense/p2d/AdaptacionRegresionIT.java` · **Ejecución:** `mvn verify -Pintegration` (requiere Docker).

**Montaje:**

- PostgreSQL 16 efímero (Testcontainers) con V1–V33 aplicadas por Flyway.
- Flujo de producción `PlanningContextFacade.adaptWeeklyPlan`.
- Validadores reales: V8C, validación estricta con 4D en `ENFORCE`, V1–V24 y V23 en `ENFORCE_AI_ONLY`, la configuración por defecto.
- Persistencia y evidencia V32 reales.

**Solo se simula** `ReplicateSessionFixGenerator` (`@MockitoBean`), que devuelve la corrección de cada caso. No hay llamadas a Replicate ni autorización de pago. **Ninguna clase productiva de P2-D se modificó.**

En cada caso, la semana medida la produce el motor de reglas para un participante nuevo (GYM, `INTERMEDIATE`, lunes/miércoles/viernes, 45 min). Se registra su medición y se adapta la semana siguiente.

## 1. Reclasificación: no se altera en silencio una sesión conservada

| Caso | Resultado |
|---|---|
| Control (`GAIN_MUSCLE`, sin cambios de catálogo, IA sin propuesta) | Se publica una **copia exacta**, sesión por sesión, con ejercicios y dosis; origen `CONSERVED` |
| Un relleno de la semana medida se reclasifica a `HIP_HINGE` (pasa a estructural) | **No se publica.** V32 registra `NO_FACTIBLE` con `V24: el ejercicio N …`: la dosis conservada ya no corresponde a su clase. La semana medida no se toca |
| Un ejercicio de la semana medida pasa a exigir suelo y el participante no lo permite | **No se publica.** `NO_FACTIBLE` con `V1: el ejercicio N no esta en available_exercises`: se trata como incompatibilidad de seguridad real, no se conserva |

## 2. Complementos de cardio y movilidad

Las sesiones señaladas usan `SEGURIDAD_ANTE_DOLOR` sin ejercicio identificado. V8C exige entonces que no reaparezca ninguno de sus ejercicios, y la corrección simulada las reemplaza enteras con alternativas de la misma zona, tipo y clase de slot.

| Caso | Resultado |
|---|---|
| `GENERAL_FITNESS`: un cardio complementario en una posición de relleno | **Se publica** con origen `AI`. El cardio está en la sesión corregida, ningún ejercicio de la sesión con dolor reaparece y las sesiones no señaladas quedan idénticas |
| `GENERAL_FITNESS`: cardios en **todas** las colocaciones de un slot | **No se publica.** Los tres intentos se rechazan con `V23: la semana no cubre <ese slot>`: un complemento no pasa por trabajo estructural. Sin propuesta usable, una sesión con dolor no se conserva: `NO_FACTIBLE`, `ai_calls = 3` |

**Hallazgo relacionado.** La semana de `GENERAL_FITNESS` que produce el motor de reglas puede no cubrir un slot (pasó con `LOWER_KNEE`). Con V23 en `ENFORCE_AI_ONLY`, cualquier corrección de la IA sobre esa semana se rechaza mientras no añada el slot que falta. La prueba lo tiene en cuenta: la corrección simulada cubre los slots ausentes.

## 3. Bloqueos individuales

| Caso | Resultado |
|---|---|
| Control (`GAIN_MUSCLE`): la IA sustituye un estructural por una variante del mismo patrón y zona | Se publica con origen `AI` y la variante |
| La misma variante, **bloqueada** por el participante | Los tres intentos se rechazan con `V1: el ejercicio Z …`. El respaldo publica la semana conservada, **sin el bloqueado** y idéntica a la medida; origen `CONSERVED`, `ai_calls = 3` |
| Se bloquea un ejercicio que **estaba** en la semana medida | **No se publica.** `NO_FACTIBLE` con `V1: el ejercicio Y …`: el conflicto con la sesión anterior se detecta, no se conserva |

## 4. Defecto abierto encontrado (no corregido: P2-D intocable en este paso)

**Una continuidad sin llamadas a la IA no se puede publicar.** Afecta a:

- `NO_CHANGE` e `INSUFFICIENT_DATA`;
- el respaldo con la IA apagada o el cortacircuitos abierto.

`publishAdapted` guarda `generation_attempts = aiCalls = 0`, y `ck_plan_attempts` (V4) exige `>= 1`. **Consecuencias en producción:**

- `WeeklyCycleService.runFor` captura la excepción y **la semana queda sin plan**;
- **tampoco se registra evidencia** en `plan_adaptation_attempts` (0 filas);
- `NO_CHANGE` es lo que corresponde a un participante con buena adherencia, así que el caso es frecuente.

`PublicacionAtomicaIT` no lo detectaba porque siempre publica con `aiCalls = 1`, y `PublishAdaptedTest` usa simulaciones sin base.

El defecto queda caracterizado en `defectoAbiertoNoChangeSinLlamadasViolaCkPlanAttempts`. Esa prueba **comprueba el fallo actual y fallará a propósito cuando se corrija**; entonces hay que sustituirla por la de copia exacta con `NO_CHANGE`.

**Corrección propuesta, pendiente de autorización.** En `WeeklyTrainingPlanCommandServiceImpl.publishAdapted`, pasar a `firstVersion`/`nextVersionOf` `max(1, aiCalls)` como `generation_attempts`, porque producir la semana conservada es un intento de generación. `ai_calls` de V32 seguiría en 0, que V32 admite.

## 5. Lo que estas pruebas no cubren

- **Dolor identificado por ejercicio:** el registro de ejecución con motivo de dolor no está sembrado; se usa el caso preventivo.
- **`CorreccionReplicateEvidenciaIT`:** sigue pendiente de una ejecución pagada.
- **Movilidad como complemento:** la regla es la misma que para el cardio, pero no tiene caso propio.
