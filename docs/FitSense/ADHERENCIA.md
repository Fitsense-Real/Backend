# Adherencia: definición operacional

Este documento es la referencia de la tesis y del backend. Se corrigió a partir de `11b5aae`, en la rama `generation`. Hay **una sola** adherencia: cuánto de lo planificado se ejecutó, sin ponderaciones.

## Fórmulas

| Nivel | Fórmula | Dónde se calcula |
|---|---|---|
| Ejercicio | A_e = min(E_e / P_e, 1) × 100 | `WorkoutSessionExercise.record` (execution) |
| Entrenamiento | A_s = Σ(A_e / 100) / n × 100, con n = ejercicios **indicados** | `WorkoutSession.finish` (execution) |
| Semana | A_w = Σ(A_s / 100) / m × 100, con m = entrenamientos previstos | `WeeklyMetricsCalculator` (analytics) |

**Ejercicio:**
- Con prescripción por repeticiones, E y P son repeticiones: P = series × repeticiones.
- Con prescripción por duración, E y P son segundos: P = series × segundos.
- Superar lo indicado no suma: el aporte se limita al 100 %.

**Entrenamiento:** todos los ejercicios pesan lo mismo, sin importar sus repeticiones o su duración. Un ejercicio no registrado aporta 0.

**Semana:**
- Cada entrenamiento previsto vale **uno**.
- La duración, el volumen, la carga, el RPE y la satisfacción no lo ponderan.
- A_s es el cumplimiento de la sesión que cuenta, o 0 si no hay ninguna.

### Ejemplo completo

**Lunes**, 45 minutos:

| Ejercicio | Dosis | Hecho | A_e |
|---|---|---|---:|
| Sentadilla | 3×10 = 30 reps | 30 | 100 |
| Remo | 3×10 = 30 reps | 30 | 100 |

A_s = (1 + 1) / 2 × 100 = **100**

**Miércoles**, 45 minutos:

| Ejercicio | Dosis | Hecho | A_e |
|---|---|---|---:|
| Press | 3×10 = 30 reps | 30 | 100 |
| Plancha | 2×30 s = 60 s | 0 s | 0 |

A_s = (1 + 0) / 2 × 100 = **50**

**Viernes**, 30 minutos: omitido con causa, sin sesión. A_s = **0**

**Semana:** A_w = (1 + 0,5 + 0) / 3 × 100 = **50,00 %**

Con la fórmula anterior la semana daba (45×100 + 45×50 + 30×0) / 120 = 56,25 %.

## Reglas de contabilización

| Caso | Aporte |
|---|---|
| Completado (80 % o más) | Su A_s real, con un máximo de 100 |
| Parcial (de 30 % a menos de 80 %) | Su A_s real |
| Por debajo de 30 % | Su A_s real, por ejemplo 0,25, aunque el entrenamiento quede `SKIPPED` |
| Omitido, o sin sesión | 0 |
| Abandonado | No cuenta: vale el último intento cerrado (`counts_toward_adherence`) |
| Reporte retroactivo | Una sola vez, en la semana de **su entrenamiento** |
| Versiones reemplazadas | Un previsto por día entre versiones |
| Semana sin plan | No calculable (`NULL`), nunca 100 |

**Versiones reemplazadas.** Los entrenamientos `REPLACED` no cuentan. Si dos versiones traen el mismo día, queda uno solo, por este orden de preferencia:
1. el que tiene una sesión que cuenta;
2. el que ya está cerrado;
3. el de la versión más reciente.

Los umbrales del 80 % y el 30 % **clasifican** entrenamientos (estado, frecuencia, señalamiento); no reemplazan su porcentaje.

**Cuándo se calcula.** La semana se mide al cerrarse, en el ciclo semanal, con todos sus previstos. No existe un cálculo de semana en curso; la regla temporal no cambió.

**Oficial y observada: dos indicadores distintos.** Ninguno de los dos pondera; lo que cambia es el denominador.

| | Adherencia semanal oficial | Adherencia observada |
|---|---|---|
| Columna | `weighted_adherence_pct` | `observed_adherence_pct` |
| Denominador | **Todos** los previstos; lo no registrado cuenta 0 | Solo los previstos con registro (sesión u omisión declarada) |
| Uso | Resultado de la tesis, riesgo, intervención (`trigger_adherence_pct`, `adherence_after_pct`) | Solo interno: decidir si se adapta y con qué alcance |

La observada **no** es la adherencia de la tesis y no debe presentarse como tal.

**`weighted_adherence_pct` es un nombre técnico heredado.** No describe una fórmula ponderada: desde `ef472ee` contiene A_w sin pesos. No se renombra porque haría falta una migración que no aporta al MVP (decisión del 24/10/2026).

## Indicadores que se conservan aparte

Los siguientes indicadores se guardan en `weekly_user_metrics` y alimentan el riesgo y la adaptación como factores propios. **No** entran en A_w:
- RPE medio y satisfacción;
- minutos entrenados;
- volumen ejecutado, repeticiones y segundos;
- frecuencia de sesiones válidas;
- causa dominante de omisión.

## Quién consume la adherencia

Todos leen la misma fila de `weekly_user_metrics`:

| Consumidor | Qué usa |
|---|---|
| Riesgo (`WeeklyMetricsCalculator.scoreRisk`) | Baja adherencia y caída frente a la semana anterior, ambas sobre A_w |
| Intervención (`UserIntervention`) | `trigger_adherence_pct` = A_w de la semana medida; `adherence_after_pct` = A_w de la semana siguiente |
| Decisión de adaptación (`AdaptationOrderDecider`) | La observada y la cobertura; la evidencia de V32 guarda las mismas cifras |
| Generación (`PreviousWeekSummary`, `PlanInputSnapshot`) | A_w de la semana anterior como contexto |
| API para Flutter y administración | `GET /api/v1/users/me/metrics/weekly[/{lunes}]` |

## Qué cambió en el código

| Antes | Ahora |
|---|---|
| A_w = Σ(duración × A_s) / Σ duración. La observada se ponderaba igual | Cada previsto pesa uno |
| Las sesiones se atribuían a la semana por `started_at`, con corte a medianoche UTC. Una sesión del domingo después de las 19:00 en UTC−5, o un entrenamiento hecho antes de su semana, quedaba fuera | Se atribuyen por `planned_workout_id` (`ExecutionContextFacade.fetchCountedSessionsOf`) |
| Un día cerrado en la versión 1 y repetido en la versión 2 contaba dos veces | Un previsto por día entre versiones (`WeekPlanInput.unoPorDia`) |
| Se podía omitir un entrenamiento `IN_PROGRESS` con la sesión abierta | Se rechaza: primero hay que cerrarla o abandonarla |

A_e y A_s no cambiaron: execution ya los calculaba así.

## Datos históricos

Los registros de ejecución no se modifican. Las filas de `weekly_user_metrics` calculadas antes de esta corrección conservan el valor con pesos.

`docs/FitSense/consultas/adherencia_recalculada.sql` es de solo lectura. Rehace cada semana con esta definición y la compara con la cifra guardada.

Una semana sin cambios en la cifra cumple tres condiciones: todos sus entrenamientos duran lo mismo (como en los planes del motor de reglas, que copian `session_minutes`), todas sus sesiones empiezan dentro de su semana en UTC y no tiene días repetidos entre versiones.

Recalcular las filas guardadas reescribiría `weekly_user_metrics` (el cálculo es idempotente), pero no `user_interventions`: las intervenciones ya decididas conservarían la cifra con la que se decidieron.

**Decisión (24/10/2026):**
- No se recalculan las semanas históricas por ahora. No se modifica Neon.
- Las semanas calculadas con la fórmula anterior **no se mezclan** con las nuevas para evaluar los resultados de la tesis.
- Antes del piloto se ejecuta `adherencia_recalculada.sql` en modo solo lectura para saber si hay datos reales que requieran recalcularse.

**Cómo distinguir las filas.** La fila no guarda qué fórmula la calculó. `calculation_version` es la versión de configuración (MVP-1.8), que no cambió con esta corrección, y `calculated_at` es la fecha del **primer** cálculo, porque la columna no se actualiza al recalcular. Por eso una fecha anterior al despliegue de `ef472ee` no prueba que la cifra sea la vieja. La consulta de comparación, que rehace cada semana desde los datos de ejecución, es la forma fiable de separarlas.

Las migraciones V13 y V14 describen `expected_duration_minutes` como el peso de la adherencia. Son historia y no se modifican; esa descripción quedó superada por este documento.
