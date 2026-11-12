# 02 · Optimizaciones implementadas

Las tres actúan sobre lo que controla FitSense: cuántas predicciones se piden, cuánto se tarda en enterarse de que terminaron y cuánto se espera en total. Ninguna cambia prompts, reglas, validadores, modelo ni lo que se publica en los casos evaluados.

## A. Llamadas que no pueden salir bien (H-G9)

**Problema** (`auditoria_activacion_ia/03_DIAGNOSTICO_HG9.md`):
- Perfil: GENERAL_FITNESS intermedio o avanzado con una sesión de 30–35 min.
- Una sesión única tiene que llevar:
  - los cuatro slots de 4C (V23);
  - STRENGTH, MUSCULAR_ENDURANCE, CARDIO y MOBILITY (V25);
  - cada una con su dosis exacta (V24).
- Eso dura al menos 2265 s y el techo de V4 es 1800 o 2100 s.
- Se pedían tres predicciones, todas rechazadas (~210 s y ~USD 0,07), y después publicaba el motor de reglas.

**Cambio** (`FactibilidadGeneracionIA`, invocada por `PlanGenerationPipeline` antes del primer intento):
- **No es una regla nueva:** usa las comprobaciones que el validador ya aplica a un borrador de la IA, con sus mismas funciones:
  - V2 (número de sesiones);
  - V24 extendida (`CapabilityPrescriptionPolicy.acreditada`);
  - V25 (`CapabilityCoveragePolicy`);
  - V23 (`SlotCoveragePolicy`, solo si bloquea a la IA);
  - V5;
  - V4 (`PlanningPolicy.excedeTecho` con el `SessionDurationEstimator`).
- Calcula de forma exacta la **cota inferior** de duración: la combinación más barata que cumple esas reglas, por programación dinámica sobre el conjunto elegible.
- Todas las demás reglas se ignoran. Solo podrían subir la duración mínima, así que **no puede marcar como imposible un caso posible**.
- **Solo se aplica con una sesión semanal**, en objetivos con V24 por capacidades (no GAIN_MUSCLE), con V4 en segundos y con V24/V25 bloqueando. Si falta cualquier dato, la IA se llama como siempre.
- Si la cota supera el techo de V4:
  - se registra `IA_NO_INVOCADA motivo=INVIABLE_PARA_IA` con la cota y el techo;
  - publica el **mismo motor de reglas con la misma validación** (los motivos de rechazo que recibía no cambian su salida);
  - no cuenta para el cortacircuitos.
- **Interruptor de emergencia:** `fitsense.ai.generate.feasibility-precheck` (`FITSENSE_AI_GENERATE_FEASIBILITY_PRECHECK`, `true` por omisión).

**Efecto medido** (`FactibilidadGeneracionIAIT`, catálogo y validador reales):
- **Rejilla H-G9:**
  - imposibles exactamente en intermedio y avanzado con 30 y 35 min, con una cota de **2265 s**, igual que la auditoría;
  - en esos perfiles: 0 llamadas (antes 3) y `generation_attempts` 1 (antes 4);
  - los perfiles parecidos factibles (40 y 45 min, principiante) siguen llamando a la IA.
- **240 perfiles de un día** (cuatro objetivos × tres niveles × gimnasio/casa × 30–75 min):
  - **6 inviables**, todos de GENERAL_FITNESS intermedio o avanzado (gimnasio 30/35 min, casa 30 min);
  - **ningún falso positivo:** en los 227 perfiles para los que existe una propuesta válida conocida (el plan determinista o sus variantes, validado como si fuera de la IA), la comprobación no marcó ninguno.

## B. Sondeo

**Problema:**
- Replicate retiene el POST unos 20,5 s (REAL) y devuelve la predicción en curso.
- Después se consultaba cada **3 s**: de media, el backend se enteraba **1,5 s** después de que la predicción terminara (máximo 3 s).

**Cambio:**
- Pausa configurable: `fitsense.ai.poll-interval-millis` (`FITSENSE_AI_POLL_INTERVAL_MILLIS`).
- **1000 ms por omisión**, acotada a 250–5000 ms.
- R-2 se mantiene íntegra: cancelación única, `REPLICATE_CANCEL`, sin respuestas tardías.

**Efecto** (SIMULADO con los 56 `predict_time` REALES de GRA y el POST de 20,5 s, `ReplicateSondeoRendimientoTest`):

| Pausa | Retraso medio | p95 | Máx. | Consultas por predicción |
|---|---|---|---|---|
| 3000 ms (antes) | 1555 ms | 2894 ms | 2941 ms | 19,9 |
| **1000 ms** | **501 ms** | **941 ms** | **965 ms** | 58,6 |

**Unos 1,05 s menos por predicción** (aprox. 1,5 % de la mediana), a cambio de unas 3 veces más consultas GET.

**No hay otro retraso evitable** entre el final de la predicción y su recepción:
- la cola de Replicate es de 0,015 s;
- la diferencia restante es red y lectura del JSON.

## C. Presupuesto total de la fase de IA

**Problema:**
- Solo había límites por llamada (§4 de `01`).
- Con tres intentos, una petición podía esperar hasta unos 919 s antes del respaldo.

**Cambio** (`PlanGenerationPipeline` + `PlazoDeGeneracion`):
- `fitsense.ai.generate.total-budget-seconds`: **360 por omisión**, acotado a 60–1800.
- `fitsense.ai.generate.min-attempt-seconds`: **40 por omisión**, acotado a 10–150.
- **Antes de cada intento:** si queda menos del mínimo, no se empieza (`IA_PRESUPUESTO_AGOTADO`) y decide el motor de reglas. 40 s porque la predicción de generación más corta medida tardó 35,7 s: con menos no puede terminar.
- **Durante el intento:**
  - el `Prefer: wait` del POST se acota a lo que queda;
  - el sondeo para al llegar el plazo (`REPLICATE_PRESUPUESTO`) y **se cancela** como en R-2.
  - Agotar el presupuesto **no cuenta** como fallo del proveedor para el cortacircuitos.
- **El plazo viaja en el hilo** (`PlazoDeGeneracion`), sin cambiar la firma de `requestOutput`, que simulan decenas de pruebas y arneses de evaluación. La adaptación no lo fija y sigue igual.

**Por qué 360 s** (REAL, reproducción de trazas, `evidencia/BARRIDO_PRESUPUESTO.csv`):

| Presupuesto | Desenlaces que cambian en GRA (de 40, PR-1.7) | En la campaña de estrés (de 8, prompt anterior) | Espera máxima GRA |
|---|---|---|---|
| 240 s | 4 | 3 | 240 s |
| 300 s | 2 | 2 | 300 s |
| **360 s** | **0** | 2 (394 s y 460 s, tercer intento) | 345 s |
| 420 s | 0 | 1 | 345 s |
| 480 s | 0 | 0 | 345 s |

- 360 s **no cambia ningún desenlace** de la configuración evaluada (GRA) y acota la cola.
- Si se quisiera conservar también las colas del prompt anterior: `FITSENSE_AI_GENERATE_TOTAL_BUDGET_SECONDS=480`.

**Peor caso nuevo:**
- 360 s más lo que tarde en devolver error una lectura HTTP ya en curso (≤ 90 s si fuera el POST) más la cancelación (≤ 40 s): ≤ ~490 s teóricos, frente a ~919 s.
- En la práctica, el desfase medido en simulación es de una pausa del sondeo (≤ 1 s).

**Contrato HTTP sin cambios:** el respaldo publica la semana como siempre (201 con el plan). No hay códigos nuevos.

## D. Observabilidad

| Línea | Para qué |
|---|---|
| `GENERACION_TIEMPOS` (una por generación) | `origen`, `motivo_respaldo` (`INVIABLE_PARA_IA` / `PRESUPUESTO` / `PROVEEDOR` / `RECHAZOS` / `CIRCUIT_BREAKER` / `IA_APAGADA` / `ERROR_INESPERADO`), `intentos_ia`, `ia_ms` por intento, `validacion_ms`, `respaldo_ms`, `fitsense_ms` y `total_ms`. Permite obtener p50/p95 reales en el entorno de pruebas sin instrumentar nada más |
| `IA_NO_INVOCADA motivo=INVIABLE_PARA_IA` | Llamada evitada, con su cota |
| `IA_PRESUPUESTO_AGOTADO` / `REPLICATE_PRESUPUESTO` | Intento no empezado o predicción cortada por el presupuesto |
| Arranque | Pausa, espera máxima por llamada (con y sin cancelación), presupuesto, mínimo por intento y estado de la comprobación previa |

## Descartado (fuera de alcance o sin justificación)

- **Bajar `reasoning_effort`, cambiar de modelo o reducir el catálogo del prompt:** es lo que más acortaría la predicción, pero cambia la calidad evaluada y exige medirlo con llamadas pagadas. Ver `04`.
- **Generación asíncrona** (202 y consulta desde la app): cambia el contrato HTTP y la app.
- **Webhooks de Replicate:** ahorrarían como mucho los 0,5 s que quedan de sondeo y necesitan un punto de entrada público.
- **Cortar reintentos por rechazos corregibles (V15, V26…):** el reintento es lo que convierte un tercio de las propuestas rechazadas en planes de IA válidos (GRA: 20/20 publicados desde la IA).
