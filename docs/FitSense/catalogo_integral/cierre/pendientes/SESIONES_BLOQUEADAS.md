# Sesiones bloqueadas que vencen como SKIPPED sin motivo (orden de 14/11/2026, fase 9)

## 1. Diagnóstico (código vigente)

| Pregunta | Respuesta |
|---|---|
| **¿Cuándo se bloquea una sesión?** | Cuando un ejercicio de una sesión abierta (SCHEDULED o IN_PROGRESS) de un plan activo choca **hoy** con la seguridad del participante. `PublishedPlanSafetyService` usa las mismas causas que el filtro del conjunto elegible: banderas, INC-4C-03, INC-CAT-1374 e INC-CAT-C39. La lectura del plan marca `safetyBlocked`, y execution se niega a iniciarla (`WorkoutNotAvailableException`) |
| **¿Cómo se registra la causa?** | **No se guarda.** El bloqueo se calcula al leer. Solo queda en el registro de intentos de reparación, si alguien la intentó |
| **¿Qué pasa al vencer?** | La tarea diaria (`ExpireOverdueWorkoutsCommand`) la cierra como `SKIPPED` con `skip_reason = NULL`, igual que cualquier sesión no hecha. La causa se pierde |
| **¿Podía el usuario hacerla?** | **No.** El inicio se rechaza. Tampoco puede registrarla a posteriori: `conflictosParaRegistrar` también la bloquea |
| **Efecto en la adherencia** (`WeeklyMetricsCalculator`, sin cambios) | **Adherencia primaria:** el denominador incluye todos los previstos, así que cuenta como 0. En el plan 78 (4 sesiones), la semana no puede pasar del 75 % aunque haga todo lo demás. **Cobertura de registro:** baja, porque no hay registro (motivo NULL); 1 de 4 son 25 puntos menos, y puede quedar por debajo del mínimo de evidencia (`EvidenceRule`), con lo que no se adapta por falta de evidencia. **Racha de omisiones** (`consecutiveSkips`) y **puntuación de riesgo**: suben si es de las últimas de la semana |
| **¿Provoca adaptaciones equivocadas?** | **Puede.** `PreviousWeekAssembler` entrega la sesión a la adaptación como `SKIPPED` sin motivo: no hay forma de distinguir «no quiso» de «el sistema no le dejó». Una adaptación por baja adherencia o un aumento del riesgo pueden nacer de un bloqueo del sistema |

## 2. Solución propuesta, por etapas

| Etapa | Qué | ¿Cambia la metodología? | Estado |
|---|---|---|---|
| **S1 · Constancia** | V54 añade `planned_workouts.system_block_cause`. Al vencer, la tarea guarda la causa vigente (`ejercicio=causa; ...`, p. ej. `1303=INC-CAT-C39`). El estado, el motivo (NULL), la adherencia, la cobertura, las rachas, el riesgo y la adaptación **no cambian** | No | **Preparada** en `propuesta/reparacion-dificultad-publicada`. Prueba: `Plan78ContencionReducidaIT.semanaEmpezada` |
| **S2 · Prevención** | Reparar antes de que venza: la reparación localizada ya existe y, con el interruptor del plan 78, sí publica. Se lanzaría al detectar el bloqueo en una semana sin empezar, y se avisaría al usuario | No (la reparación ya está aprobada); el disparo automático es una decisión de producto | Propuesta |
| **S3a · Adaptación informada** | `PreviousWeekAssembler` diría a la adaptación que la sesión no se ofreció por un bloqueo del sistema, para que no la trate como falta de adherencia | **Sí**: cambia la entrada de la adaptación (los umbrales no) | Necesita aprobación metodológica |
| **S3b · Denominador** | Excluir de la adherencia primaria las sesiones con `system_block_cause` («no ofrecidas») | **Sí**: cambia la fórmula oficial | **No se propone aplicar.** Se documenta como opción para el propietario; sin recalcular el historial |

**Lo que no se hace:**
- no se modifica la fórmula de adherencia ni los umbrales;
- no se recalcula el historial;
- no se rellena `skip_reason` (sería inventar un motivo que la persona no dio);
- nada de esto se activa sin aprobación.

**Datos reales:** el número de sesiones que vencieron bloqueadas antes de V54 no se puede reconstruir con exactitud, porque la causa no se guardaba. Se puede estimar con la consulta de solo lectura de `PLANES_PUBLICADOS_C39.md` §2, aplicada a sesiones `SKIPPED` con motivo NULL. No se ha ejecutado contra datos reales.
