# 10 · Fase 1: contrato de disponibilidad y frecuencia (S-MAX)

**Estado:** CORREGIDO (consistencia técnica y comunicación) y COMPROBADO (contrato en el ciclo real).
**Commit:** `f95e1f7`. Los topes **no cambian**.

## 1. Semántica (código actual)

| Concepto | Dónde | Significado |
|---|---|---|
| `available_days` | Perfil (`UserProfile`) | Días en que la persona **puede** entrenar |
| `days_per_week` | Perfil | Cuántas veces **quiere** entrenar. El perfil exige `days_per_week ≤ |available_days|` (`UserProfile`, "Marcaste N días disponibles pero quieres entrenar M") |
| Tope por nivel (S-MAX) | `FrecuenciaSemanal` (nuevo, fuente única) | Principiante **4**; intermedio y avanzado **6**; nadie recibe 7 |
| Sesiones programadas | `PlanGenerationContext.effectiveDaysPerWeek()` + `suggestedSplit()` | `min(days_per_week, tope del nivel, |available_days|)` |
| Frecuencia menor legítima | `PlanAdjustment.forcedDaysPerWeek` | Solo una orden REDUCE_DAYS de la adaptación (V8C la controla). Ninguna otra vía publica menos sesiones: V2 exige exactamente `effectiveDaysPerWeek()` |
| Fechas | `WeeklySplitPlanner.chooseDates` | Primeros N días disponibles, repartidos. Separa los días cuando puede, pero **no lo garantiza**: con 4 de 7 elige lunes, miércoles, viernes y sábado |
| Recuperación entre sesiones | V13 + división | En días consecutivos nunca el mismo enfoque. Si la frecuencia lo permite, los días pegados alternan superior e inferior |
| Adherencia | `WeeklyMetricsCalculator` | Denominador = **sesiones programadas** (`plan.workouts()`), no días disponibles |
| API | `GET /users/me/plan` (sesiones programadas); `GET /users/me/profile` (`daysPerWeek` pedido); `GET /users/me/plan/limitations` | Desde `f95e1f7`, `limitations` incluye `FREQUENCY / S_MAX` cuando el tope recorta lo pedido |

**Inconsistencia corregida:** el tope estaba triplicado. Las constantes de `PlanGenerationContext`, la primera semana de `WeeklyTrainingPlanCommandServiceImpl` y la base de REDUCE_DAYS de `UserInterventionCommandServiceImpl` (`? 4 : 6` escrito a mano) usan ahora `FrecuenciaSemanal`. El comportamiento no cambia: los barridos y K20 dan las mismas huellas (F8).

## 2. Pruebas

| Prueba | Qué fija | Resultado |
|---|---|---|
| `FrecuenciaSemanalTest` (24) | `planificadas = min(pedidos, tope, disponibles)`; topes 4/6/6; nunca 7 | 24/24 |
| `PlanLimitationsFrecuenciaTest` (7) | Aviso solo cuando el tope recorta; el texto incluye días pedidos, sesiones y «100 %» | 7/7 |
| `AdherenciaSesionesProgramadasTest` (2) | 4 programadas (de 5 disponibles) y las 4 hechas = **100 %**; 3 de 4 = **75 %** (no 60 %) | 2/2 |
| `FrecuenciaPorNivelIT` (ciclo real: generación y persistencia) | 4 objetivos × 3 niveles × 1–7 días pedidos × 3 patrones de disponibilidad (exactos, toda la semana, no consecutivos): **216 planes**. Comprueba recuento, días disponibles, una sesión por día, días pegados nunca con el mismo enfoque ni con dos cuerpos completos, y aviso FREQUENCY | **HIST 216/216 y REV2 216/216.** Principiante 5, 6 o 7 → 4; intermedio y avanzado 7 → 6. 248 pares de días pegados, todos con enfoque alterno |

**Observación sin cambio:** `chooseDates` no separa al máximo; con 4 de 7 da viernes y sábado seguidos. La recuperación sí la garantiza la alternancia de enfoques. Cambiar la elección de fechas alteraría todos los planes históricos, así que no se toca; queda propuesto (P11 de `18_INFORME_CIERRE.md`).

## 3. Oráculo: contrato de días versionado (`+DIAS-1`)

- El oráculo histórico **no se modifica**.
- `oraculo_k20_politica.py <POLÍTICA>+DIAS-1` **sustituye solo** la comprobación histórica «sesiones == days_per_week», que ignoraba S-MAX, por:
  - **rechaza:** más sesiones que el tope del nivel;
  - **rechaza:** más sesiones que las esperadas;
  - **rechaza:** menos sesiones que las esperadas sin justificación (REDUCE_DAYS en `user_interventions` para ese plan; ni siquiera así se admite una semana vacía);
  - **rechaza:** `planned_days_count` distinto de las sesiones publicadas.
- Días no disponibles, fuera de semana o repetidos: los rechazan las comprobaciones de siempre.
- Pruebas: `herramientas/test_oraculo_dias.py`, 7 positivas y 6 negativas, OK.

### L05 con cada oráculo (evidencia original sin modificar: `evidencia/k20/*_L.tar.gz`)

| Corrida L | Histórico | REV2 | **REV2 + DIAS-1** | HIST + DIAS-1 |
|---|---|---|---|---|
| REV2 D / D′ | 10 · 6 (V20) | 15 · **1 (L05)** | **16 · 0** | — |
| REV2 IA simulada | 9 · 7 (V20) | 15 · **1 (L05)** | **16 · 0** | — |
| HIST D | 15 · **1 (L05)** | 8 · 8 | 9 · 7 | **16 · 0** |

- **L05 conserva su clasificación histórica (FAIL).**
- Con el contrato corregido pasa: es una **reevaluación**, no un cambio del experimento.
- No cambia ninguna otra clasificación en U, J, K ni L (13 corridas re-evaluadas).
- **Advertencia de protocolo:** la secuencia de L05 (H/H/H/H/O) preveía una omisión «demasiado difícil» en la 5.ª sesión, que nunca existió. El escenario de adaptación de L05 **no se ejercitó**: completó 4/4, Aw 100 % y NO_CHANGE. La cohorte M (`cohorte_M/M_MANIFIESTO.md`) genera las secuencias sobre las sesiones programadas, no sobre los días pedidos.

## 4. Aplicación móvil (repositorio `mobileapp`, fuera de esta sesión: solo contrato)

| Observado (`lib/features/profile/data/models/user_profile.dart`) | Consecuencia | Cambio necesario |
|---|---|---|
| Envía `daysPerWeek = days.length` | Frecuencia pedida = disponibilidad, siempre. Un principiante que marca 5 días pide 5 y recibe 4 sin explicación | Preguntar por separado «¿qué días puedes?» y «¿cuántas veces quieres?» (o explicar el tope) |
| No consume `GET /users/me/plan/limitations` | La persona no ve ni el aviso de tracción ni FREQUENCY ni TIME | Mostrar la lista (type, title, message, optionalEquipmentCodes) en el plan y en el alta |
| Adherencia / progreso | Si la app calcula porcentajes con los días del perfil, mostraría 80 % con 4/4 | Usar las sesiones del plan o las métricas del backend (`weighted_adherence_pct`) |
| `POST /users/me/plan/generate` → 422 | Con `FUERZA_SIN_TIEMPO` (F2) debe mostrar el mensaje y las alternativas | Tratar `code`/`message` del `ApiErrorResource` |
