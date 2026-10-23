# P2-D: regresión de la adaptación ante reclasificaciones, complementos y bloqueos

**Fecha:** 23/10/2026 · **Prueba:** `src/test/java/main/web/services/fitsense/p2d/AdaptacionRegresionIT.java` · **Ejecución:** `mvn verify -Pintegration` (requiere Docker).

**Montaje:**

- PostgreSQL 16 efímero (Testcontainers) con V1–V33 aplicadas por Flyway.
- Flujo de producción `PlanningContextFacade.adaptWeeklyPlan`.
- Validadores reales: V8C, validación estricta con 4D en `ENFORCE`, V1–V24 y V23 en `ENFORCE_AI_ONLY`, la configuración por defecto.
- Persistencia y evidencia V32 reales.

**Solo se simula** `ReplicateSessionFixGenerator` (`@MockitoBean`), que devuelve la corrección de cada caso. No hay llamadas a Replicate ni autorización de pago. La única clase productiva modificada es `publishAdapted`, con la corrección autorizada de la sección 4.

En cada caso, la semana medida la produce el motor de reglas para un participante nuevo (GYM, `INTERMEDIATE`, lunes/miércoles/viernes, 45 min). Se registra su medición y se adapta la semana siguiente.

## 1. Reclasificación: no se altera en silencio una sesión conservada

| Caso | Resultado |
|---|---|
| Control (`GAIN_MUSCLE`, sin cambios de catálogo, IA sin propuesta) | Se publica una **copia exacta**, sesión por sesión, con ejercicios y dosis; origen `CONSERVED` |
| Un relleno de la semana medida se reclasifica a `HIP_HINGE` (pasa a estructural) | **No se publica.** V32 registra `NO_FACTIBLE` con `V24: el ejercicio N …`: la dosis conservada ya no corresponde a su clase. La semana medida no se toca |
| Un ejercicio de la semana medida pasa a exigir suelo y el participante no lo permite | **Se sustituye solo ese ejercicio** por uno de su slot, sin suelo y con dosis 4D (SUST-SEG-1); el resto queda idéntico. Sin sustituto seguro: `NO_FACTIBLE` con `SEGURIDAD: …`. Ver [`P2D_SUSTITUCION_SEGURIDAD.md`](P2D_SUSTITUCION_SEGURIDAD.md). *Hasta el 23/10 este caso terminaba en `NO_FACTIBLE` por V1* |

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
| Se bloquea un ejercicio que **estaba** en la semana medida | **No se publica.** `NO_FACTIBLE` con `V1: el ejercicio Y …`: el conflicto con la sesión anterior se detecta, no se conserva. Un bloqueo es una preferencia, no seguridad: SUST-SEG-1 no lo sustituye |

## 4. Defecto encontrado y CORREGIDO: publicar una continuidad sin IA

**Defecto.** Una continuidad sin llamadas a la IA se publicaba con `generation_attempts = aiCalls = 0`, y `ck_plan_attempts` (V4) exige `>= 1`. Afectaba a:

- `NO_CHANGE` e `INSUFFICIENT_DATA`;
- el respaldo con la IA apagada o el cortacircuitos abierto.

`WeeklyCycleService.runFor` capturaba la excepción y **la semana quedaba sin plan y sin evidencia** en `plan_adaptation_attempts`. `NO_CHANGE` es la decisión que corresponde a un participante con buena adherencia. `PublicacionAtomicaIT` no lo detectaba porque siempre publica con `aiCalls = 1`, y `PublishAdaptedTest` usa simulaciones sin base.

**Corrección autorizada (23/10/2026).** En `WeeklyTrainingPlanCommandServiceImpl.publishAdapted`:

```java
short generationAttempts = (short) Math.max(1, publicacion.aiCalls());
```

- Se usa **solo** para `generation_attempts` en `firstVersion`/`nextVersionOf`: producir la semana conservada es un intento determinista.
- `ai_calls` de la evidencia V32 sigue siendo el número **real** de llamadas, 0 incluido: no se afirma que la IA intervino.
- No toca ningún validador: V1, V8C, V23, V24 y la seguridad deciden igual que antes. La corrección solo actúa sobre semanas que ya eran publicables.

**Pruebas de aceptación** (PostgreSQL efímero, sin llamadas pagadas):

| Escenario | Prueba | Resultado |
|---|---|---|
| `NO_CHANGE` sin IA | `noChangePublicaLaCopiaExacta` | Copia exacta, `generation_attempts = 1`, `ai_calls = 0`, `SIN_ORDEN`, IA no invocada |
| `INSUFFICIENT_DATA` | `insufficientDataPublicaSinIa` | Igual |
| IA desactivada con orden de adaptar | `iaDesactivadaPublicaElRespaldo` | Respaldo publicado, `RESPALDO`, `ai_calls = 0`, `generation_attempts = 1` |
| Cortacircuitos abierto | `conElCortacircuitosAbiertoSePublicaElRespaldoSinLlamar` | Tres caídas simuladas lo abren (cada una publica su respaldo con `ai_calls = 1`); la cuarta adaptación no invoca a la IA y publica `RESPALDO` con `ai_calls = 0`, `generation_attempts = 1` |
| IA llamada una vez | `sinCambiosSePublicaLaCopiaExacta` | `ai_calls = 1`, `generation_attempts = 1` |
| IA llamada tres veces | `unBloqueadoNoEntraEnUnaAdaptacionNueva` | `ai_calls = 3`, `generation_attempts = 3` |
| Fallo real de publicación (métrica inexistente, `fk_paa_metric`) | `falloDePublicacionHaceRollback` | Excepción, 0 planes y 0 intentos: sin plan ni evidencia a medias; la semana medida intacta |
| Plan original incompatible con seguridad | `reclasificadoConRestriccionDeSeguridad` | Sigue `NO_FACTIBLE` con V1: no se fuerza ninguna publicación |

- Unitarias en `PublishAdaptedTest`: `generation_attempts = 1` con `ai_calls = 0` en `NO_CHANGE`, y `3`/`3` con tres llamadas.
- **Mutación de control:** sin la corrección fallan exactamente los cuatro casos de continuidad sin IA, con `ck_plan_attempts`, y la unitaria.
- **La prueba que caracterizaba el defecto se eliminó**; ninguna prueba lo da por aceptable.

**Regresión de las IT de P2-D existentes**, sin modificarlas, en un PostgreSQL 16 temporal con V1–V33:

| Clase | Resultado |
|---|---|
| `InvariantesV33IT` | 3/3 |
| `PublicacionAtomicaIT` | 2/2 |
| `DisparadorDiferidoIT` | 2/2 |
| `CorreccionReplicateEvidenciaIT` | saltada (de pago) |

0 filas residuales.

## 5. Lo que estas pruebas no cubren

- **Dolor identificado por ejercicio:** el registro de ejecución con motivo de dolor no está sembrado; se usa el caso preventivo.
- **`CorreccionReplicateEvidenciaIT`:** sigue pendiente de una ejecución pagada.
- **Movilidad como complemento:** la regla es la misma que para el cardio, pero no tiene caso propio.
