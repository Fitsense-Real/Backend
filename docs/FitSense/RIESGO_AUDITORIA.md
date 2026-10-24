# Auditoría de la detección de riesgo de baja adherencia

La auditoría parte del commit `ef472ee`, en la rama `generation`. No se cambiaron umbrales, reglas ni código de producción. No se tocaron la generación, P2-D ni Replicate.

## Dónde vive

`WeeklyMetricsCalculator.scoreRisk` calcula el riesgo al medir cada semana cerrada, en el paso 2 del ciclo semanal (`WeeklyCycleService`). El resultado se guarda en `weekly_user_metrics` (`risk_score`, `risk_level`, `risk_factors`, `is_dropout`) y se expone en `GET /api/v1/users/me/metrics/weekly[/{lunes}]`.

## Reglas vigentes (configuración MVP-1.8)

| Señal | Qué mide | Puntos |
|---|---|---|
| `low_adherence` | Adherencia semanal **oficial** (A_w, sin pesos) | 80 o más → 0 · 60 o más → 10 · 40 o más → 20 · menos de 40 → 30 |
| `consecutive_skips` | Entrenamientos seguidos sin sesión válida (30 % o más) **al final** de la semana, contados hacia atrás desde el último | 1 → 5 · 2 → 15 · 3 o más → 25 |
| `adherence_drop` | Caída en puntos frente a la A_w **guardada** de la semana anterior | 15 o más → 5 · 25 o más → 15. Una subida no puntúa |
| `overexertion` | RPE medio de las sesiones que cuentan | 8 o más → 10 |

**Nivel según el puntaje:** 25 o más es MODERATE, 50 o más es HIGH, 70 o más es CRITICAL; por debajo de 25 es LOW.

**Abandono** (`is_dropout`): se marca cuando pasan 14 días o más sin una **sesión válida**. Es una señal operativa, no la prueba de que la persona dejó el ejercicio, y no suma al puntaje.
- Sesión válida: cuenta para la adherencia y llega al 30 % (el mismo umbral de 17.3).
- Los días se cuentan en la fecha **local** del participante (`users.timezone`), hasta el domingo de cierre incluido. Si la zona no se encuentra, se usa la de referencia del MVP, `America/Lima`.
- Quien nunca tuvo una sesión válida no tiene días ni se marca.

| Última sesión | ¿Reinicia los 14 días? |
|---|---|
| 100 %, 80 %, 60 % o 30 % | Sí |
| 25 % o 10 % | No: aporta su porcentaje a la adherencia, pero no reinicia |
| Entrenamiento omitido | No |

**Semana sin plan:** no tiene puntaje ni nivel; solo puede llevar la marca de abandono.

## Qué decide una intervención

**El nivel de riesgo no decide la adaptación.** `AdaptationOrderDecider` decide con la adherencia **observada** y la cobertura de registro:

| Situación | Decisión |
|---|---|
| Cobertura menor de 50 % | `INSUFFICIENT_DATA`: se conserva el plan |
| Observada de 80 % o más | `NO_CHANGE` |
| Observada de 50 % a menos de 80 % | `ADAPT_LOCALIZED`: se corrige la peor sesión señalada |
| Observada menor de 50 % | `ADAPT_EXPANDED`: se corrigen todas las sesiones señaladas |

El riesgo es un indicador descriptivo que se guarda, se muestra y queda en la exportación, en paralelo a la decisión. Ninguna regla ni ningún mensaje al participante depende de él.

En la tesis conviene decir esto así. La disminución de adherencia se **detecta** con la oficial y se refleja en el riesgo. La **intervención** se dispara con la observada, para no adaptar por falta de registro.

## Escenarios comprobados

Los valores esperados están calculados a mano en las pruebas.

**Integración** (`AdherenciaRiesgoIT`, 5 pruebas, PostgreSQL temporal, dos semanas seguidas):

| Semana 1 → semana 2 | A_w | Puntaje | Nivel | Adaptación |
|---|---:|---:|---|---|
| 100 → 100/100/100 | 100 | 0 | LOW | NO_CHANGE |
| 100 → 100/omitido/omitido | 33,33 | 30 + 15 + 15 = 60 | HIGH | ADAPT_EXPANDED (2 sesiones) |
| 100 → 100/100/omitido | 66,67 | 10 + 5 + 15 = 30 | MODERATE | ADAPT_LOCALIZED (1 sesión) |
| 100 → 100/sin registro/sin registro | 33,33 | 60 | HIGH | INSUFFICIENT_DATA (cobertura 33 %) |
| Semana 2 medida antes que la 1 | 33,33 | 45, sin caída | MODERATE | Al recalcular después de la 1: 60 |

**Unitarias** (`RiesgoEscenariosTest`, 16 pruebas):
- los bordes 80,00 y 79,67;
- la racha de omisiones al final frente a la del principio;
- una sesión al 25 %, que abre racha;
- caídas de 10, 15 y 36,67 puntos;
- una subida y una semana sin anterior;
- RPE medio de 8 y de 7,67;
- CRITICAL con 70 puntos y HIGH con 50;
- abandono con 14 días y con 13, y sin plan;
- **independencia de la duración**: 100/0/0 con 60, 45 y 30 minutos da 45 puntos. Con la fórmula anterior daba 35.

## Hallazgos

No hay un defecto demostrado en el puntaje: todas las reglas se comportan como están escritas y usan la adherencia nueva. Los hallazgos 1 y 2 se corrigieron después (ver "Corrección del abandono"); el 3 y el 4 siguen como están.

1. **El abandono cuenta sesiones por debajo del 30 %.** *(Corregido.)*
   - `ExternalExecutionService.daysSinceLastWorkout` está documentado como "días sin ninguna sesión válida (17.5)".
   - Usa `findLastCountedSessionDate`, que toma cualquier sesión que cuente, incluidas las de menos del 30 %, que no son válidas según 17.3.
   - Consecuencia: alguien que registra sesiones del 10 % nunca se marca como abandono. La racha de omisiones, en cambio, sí exige el 30 %.
   - Requiere decidir cuál es la regla (ver "Decisiones pendientes").
2. **Corte UTC en los días sin entrenar.** *(Corregido.)*
   - Los días se cuentan con la fecha UTC de la sesión. Una sesión después de las 19:00 en Lima cuenta como del día siguiente.
   - Consecuencia: la marca de abandono puede llegar un día tarde.
   - Es menor y no se corrigió.
3. **Semana de transición de la fórmula.**
   - La caída compara con la A_w **guardada** de la semana anterior.
   - La primera semana medida con `ef472ee` se compararía con una semana calculada con pesos, si existiera.
   - Antes del piloto, la consulta `adherencia_recalculada.sql` dice si hay datos así. Esto es parte de la condición de no mezclar fórmulas.
4. **Dependencia del orden.**
   - Si la semana anterior no está calculada, no hay caída.
   - El ciclo semanal calcula las semanas en orden, así que en la operación normal no ocurre. Un recálculo manual fuera de orden sí lo produce.

## Corrección del abandono

Decisión del 24/10/2026: el abandono considera solo sesiones válidas (30 % o más), con el umbral de 14 días sin cambios.

**Cambios:**
- `ExternalExecutionService.daysSinceLastWorkout` toma la última sesión que cuenta con un cumplimiento de 30 % o más (`findLastValidSessionDate`) y anterior al fin del domingo de cierre en la zona del participante.
- Los días se cuentan con la fecha local de esa sesión.
- La zona horaria llega por una ACL nueva hacia IAM (`analytics/.../ExternalIamService`).
- Las sesiones por debajo del 30 % no se tocan: siguen en el historial y aportan su porcentaje.

Además, una sesión posterior al cierre ya no cuenta para la semana medida. Antes daba 0 días al recalcular una semana pasada.

**Pruebas:**
- `AbandonoIT` (10, PostgreSQL temporal):
  - sesiones al 10, 25, 30 y 80 %;
  - un entrenamiento omitido;
  - los límites de 13 y 14 días;
  - domingo 23:50 y lunes 00:10 en Lima;
  - un participante en Tokio;
  - una sesión posterior al cierre;
  - quien nunca entrenó, y quien solo entrenó por debajo del 30 %.

  Con el código anterior fallan 6 de las 10.
- `DiasSinEntrenarTest` (4 pruebas unitarias del conteo de días por fecha local).
