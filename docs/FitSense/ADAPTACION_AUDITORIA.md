# Auditoría integral de la adaptación personalizada

La auditoría parte del commit `b8b06b8`, en la rama `generation`. No se cambiaron umbrales, la generación, P2-D (las clases del productor y de los validadores), V34 ni el catálogo. No hubo llamadas a Replicate.

## El recorrido

`WeeklyCycleService.runFor` es el ciclo de producción, que el programador lanza el lunes a las 03:00 hora local:

| Paso | Qué hace | Dónde |
|---|---|---|
| 1. Cerrar | La semana medida deja de admitir sesiones | `PlanningContextFacade.closeWeek` |
| 2. Medir | A_w oficial, observada, cobertura y riesgo | `AnalyticsContextFacade.calculateWeek` |
| 3. Puntuar | `adherence_after_pct` de la intervención que produjo el plan medido | `recordOutcome` |
| 4. Decidir | `NO_CHANGE`, `ADAPT_LOCALIZED`, `ADAPT_EXPANDED` o `INSUFFICIENT_DATA`, más las sesiones señaladas y su intervención | `AdaptationOrderDecider` |
| 5. Adaptar | Deriva la semana medida +7 días; la IA corrige solo las señaladas; se valida y se publica | `AdaptedWeekCommandService` |
| 6. Enlazar | La intervención apunta al plan que produjo | `linkResultingPlan` |

**Cómo se decide** (con la adherencia **observada**, sin pesos):

| Situación | Decisión |
|---|---|
| Cobertura menor de 50 % | `INSUFFICIENT_DATA` |
| 80 % o más | `NO_CHANGE` |
| De 50 % a menos de 80 % | `ADAPT_LOCALIZED`: la peor sesión señalada |
| Menos de 50 % | `ADAPT_EXPANDED`: todas las señaladas |

**Cómo se valida** (`AdaptedWeekProducer`):
- V8C comprueba que cambie solo lo señalado y solo lo que su intervención permite.
- La validación estricta aplica V1 a V25, con 4D en ENFORCE.
- La sustitución por seguridad (SUST-SEG-1) se aplica a lo heredado.
- La semana se publica en una sola transacción, con la evidencia V32 en `plan_adaptation_attempts`.

**Sin IA (solo motor de reglas)** no hay corrección por reglas, y es una decisión de diseño:
- Con una orden de adaptar, la semana se **conserva** (`RESPALDO`).
- Si una sesión señalada se omitió por dolor, **no se publica** (`NO_FACTIBLE`): conservarla volvería a publicar el ejercicio que dolió. Esa semana queda sin plan.

## Defecto encontrado y corregido

**Repetir el ciclo semanal publicaba otra versión de la semana nueva.**
- Ocurre con un segundo `runFor` sobre la misma semana medida: otra instancia de la aplicación, un relanzamiento manual o un reinicio dentro de la hora del cierre.
- El ciclo volvía a adaptar con `replaceExisting = true`: la versión 1 pasaba a `REPLACED`, se publicaba una versión 2 y se escribía una segunda fila de evidencia para la misma decisión.
- Con el código anterior, 16 de los 16 casos del recorrido fallan en ese punto.

**Corrección:**
- `WeeklyCycleService` pregunta a adaptación si la intervención de esa semana medida ya produjo un plan, con `AdaptationContextFacade.resultingPlanOf`. Si lo produjo, devuelve ese plan y no adapta otra vez.
- Las métricas y la puntuación de la intervención se recalculan igual, porque son idempotentes.
- Un intento que no publicó (`NO_FACTIBLE`) no tiene plan enlazado, así que se puede reintentar.

## Pruebas

`AdaptacionCompletaIT` tiene 18 pruebas sobre PostgreSQL temporal. Es el ciclo de producción completo: la semana medida la genera el motor de reglas y se ejecuta de verdad, y la adherencia y la decisión salen del cálculo real. Solo se simula la IA de corrección, igual que en `AdaptacionRegresionIT`. Ante una sesión omitida por falta de tiempo (`ACORTAR_O_REORGANIZAR`), retira una colocación de relleno.

**Escenarios de la semana medida** (H = hecho, O = omitido por falta de tiempo, N = sin registro):

| Semana medida | Observada | Cobertura | Decisión esperada | Sesiones que cambian |
|---|---:|---:|---|---|
| H H H | 100 | 100 | `NO_CHANGE` | Ninguna |
| H H O | 66,67 | 100 | `ADAPT_LOCALIZED` | La omitida |
| H O O | 33,33 | 100 | `ADAPT_EXPANDED` | Las dos omitidas |
| H N N | 100 | 33,33 | `INSUFFICIENT_DATA` | Ninguna |

Los cuatro escenarios se probaron con cada objetivo: `GAIN_MUSCLE`, `INCREASE_STRENGTH`, `LOSE_WEIGHT` y `GENERAL_FITNESS`.

**En cada caso se comprueba:**
- **Decisión y trazabilidad:** la decisión, el estado (`APLICADA`, `SIN_EFECTO` o `SIN_ORDEN`), el origen (`AI` o `CONSERVED`), una sola petición a la IA cuando hay que adaptar y ninguna cuando no, las sesiones aplicadas iguales a las señaladas con algo que corregir, y la intervención enlazada al plan.
- **Una sola versión vigente** de la semana nueva.
- **Estructura:** las fechas avanzan 7 días, se conserva el enfoque y el linaje V31, cada señalada tiene un relleno menos con el resto idéntico, y las no señaladas quedan idénticas, con la misma duración.
- **Seguridad y objetivo:** no entra ningún ejercicio que no estuviera en la semana medida, ninguna sesión queda bloqueada y el objetivo del perfil no cambia.
- **Historial:** las sesiones, los ejercicios ejecutados y lo indicado en la semana medida quedan idénticos.
- **Reintento:** repetir el ciclo no publica otra versión ni otra evidencia ni otra intervención.

**Dos casos más:**
- Sin IA y con una orden de adaptar: `RESPALDO`, copia exacta, ninguna llamada.
- Omisión por dolor sin IA: `NO_FACTIBLE` con el motivo, sin plan.

**Ya cubierto por P2-D** (`AdaptacionRegresionIT`, que se volvió a ejecutar):
- un ejercicio bloqueado por el participante que la IA propone y no entra;
- la sustitución por seguridad de lo heredado (INC-CAT-1374);
- los complementos de cardio y movilidad de `GENERAL_FITNESS`;
- las reclasificaciones que rompen 4D, que se rechazan con el identificador del ejercicio.

## Duración forzada por la orden

V24 de `GAIN_MUSCLE` mide con la duración efectiva, la de la orden si fija minutos, igual que el prompt. Antes medía con el perfil: con un perfil de 60 y una orden de 45, P2-D publicaba sesiones de siete colocaciones cuando el tope de 45 minutos es seis. Detalle y pruebas en `ADENDA_4D_v3_1.md`, sección 6.

El decisor actual no fija minutos, así que en el ciclo vigente no ocurre. Si una orden los fijara, P2-D no podría cumplirla con una corrección local y la semana quedaría `NO_FACTIBLE`.

## Observación: fuerza sin relleno

En `INCREASE_STRENGTH` las sesiones del motor de reglas tienen cuatro ejercicios, todos de los slots de 4C y con la dosis mínima de fuerza (3×6). Con `ACORTAR_O_REORGANIZAR` no hay relleno que retirar ni dosis que bajar sin romper 4D. La semana se publica como `SIN_EFECTO` (`CONSERVED`): la decisión se tomó y la sesión no cambió, y la evidencia lo dice.

No es un defecto: es el límite de lo que esa intervención permite sobre una sesión mínima. Lo mismo valdría para una IA real.

## Ejemplo completo: `GAIN_MUSCLE`, de la ejecución a la publicación

**1. Semana medida** (plan 5, motor de reglas, 3 sesiones FULL_BODY):

| Día | Sesión | Ejercicios (series × reps) | Qué hizo |
|---|---|---|---|
| Lun 26/10 | 13 | 1103 (3×13), 776 (3×10), 580 (3×10), 696 (3×10), 1296 (3×10) | Hecho al 100 % |
| Mié 28/10 | 14 | 586, 325, 327, 1023 (3×10), 1182 (2×13, relleno), 632 (3×10) | Hecho al 100 % |
| Vie 30/10 | 15 | 344 (2×10), 999 (2×10), 1115 (3×10), 745 (2×10, relleno), 1103 (3×13), 495 (3×10) | Omitido: falta de tiempo |

**2. Medición:**
- A_w oficial = (1 + 1 + 0) / 3 = **66,67 %**; observada 66,67 %; cobertura 100 %.
- Riesgo: 10 puntos por adherencia + 5 por una omisión al final = **15, LOW**.

**3. Decisión:**
- `ADAPT_LOCALIZED`, porque la observada está entre 50 % y 80 %.
- Sesión señalada: la 15, con intervención `ACORTAR_O_REORGANIZAR` (la causa fue la falta de tiempo).
- `user_interventions.trigger_adherence_pct` = 66,67.

**4. Adaptación:**
- Se deriva la semana +7 días.
- Una sola petición a la IA (simulada), que retira el relleno 745 de la sesión 15.
- V8C confirma que solo cambió la señalada y que el cambio está permitido. La validación estricta aprueba la semana.

**5. Publicación** (plan 6, versión vigente de la semana del 02/11):

| Día | Sesión nueva | Origen | Ejercicios | Duración |
|---|---|---|---|---|
| Lun 02/11 | 16 | 13 | Idénticos | 32 min |
| Mié 04/11 | 17 | 14 | Idénticos | 35 min |
| Vie 06/11 | 18 | 15 | 344, 999, 1115, 1103, 495: sin el 745 | 31 → 28 min |

**Evidencia V32:**

| Campo | Valor |
|---|---|
| `decision` | `ADAPT_LOCALIZED` |
| `status` | `APLICADA` |
| `generation_source` | `AI` |
| `ai_calls` | 1 |
| `targeted_sessions` | {15} |
| `applied_sessions` | {15} |
| `conserved_sessions` | {13, 14} |
| `source_plan_id` | 5 |
| `resulting_plan_id` | 6 |

La intervención queda enlazada al plan 6. La semana medida queda intacta. Repetir el ciclo devuelve el plan 6 sin publicar nada más.
