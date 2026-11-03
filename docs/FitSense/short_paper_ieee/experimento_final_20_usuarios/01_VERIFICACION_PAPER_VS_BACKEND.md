# Verificación: definiciones del artículo frente al backend

**Base:** `generation` `0974fb3` (`backend-mvp-1.0`). Lectura del código **anterior a cualquier ejecución**.

El archivo `Kora_Short_Paper_IEEE.docx` **no está disponible en el entorno de trabajo**: no aparece ni en el repositorio ni en el contenedor. Las definiciones del artículo se toman literalmente de la orden (§1.2). Cuando el documento esté disponible, conviene cotejar las secciones II.C–II.F con esta tabla.

**Criterio de la orden:** una regla del backend distinta de la del artículo **no es un defecto**. Se registra como discrepancia metodológica (categoría F) y no se cambia ninguna regla para fabricar coincidencias.

## 1. Base del experimento

| # | Elemento | Estado |
|---|---|---|
| 1 | Commit | `0974fb3eafc39e8b7397b4dbfe7c4405e4847440` |
| 2 | Política | `PLAN-HIST-1` (`PlanningPolicySelector`, `${fitsense.planning.policy:PLAN-HIST-1}`) |
| 3 | Migraciones y catálogo | V1–V44, V44_1 y V44_2. El catálogo es el de V9 y sus correcciones (V36–V38) |
| 4 | Indicadores experimentales | Apagados:<br>• `allow-candidates=false`;<br>• progresión `false`;<br>• IA `false` por defecto.<br>Nada del PR #17 |
| 5–10 | Fórmulas, reglas, respaldo e IA | §2 y §3 |

## 2. Concepto por concepto

### 2.1. Dosis y adherencia por ejercicio
- **Artículo:** dosis ejecutada / dosis prescrita.
- **Backend:**
  - Las clases son `WorkoutSessionExercise.computePercentage`, `ExerciseTarget.denominator` y `PlannedExerciseTarget`.
  - Dosis prescrita: series × reps, o series × segundos para DURATION. Dosis ejecutada: reps totales o segundos totales registrados.
  - Porcentaje = 100 × ejecutada / prescrita, con 2 decimales HALF_UP, **limitado a 0–100**.
  - `actualSets` no interviene en el cálculo.
- **Coincidencia:** total, con un tope del 100 % que el artículo no especifica.
- **Efecto en los 20 casos:** P40 depende de que 0,4 × dosis sea entero. Si no lo es, el redondeo produce un porcentaje distinto de 40,00 (§2.9).

### 2.2. Adherencia por sesión
- **Artículo:** promedio de sus ejercicios.
- **Backend:** `WorkoutSession.computeCompletion`, una media simple (sin ponderar por volumen).
  - Se divide entre el **número de ejercicios prescritos**, así que un ejercicio sin registro cuenta como 0.
  - Se promedian valores ya redondeados a 2 decimales, y el resultado se limita a 100.
- **Coincidencia:** total.
- **Efecto:** ninguno en H. En P40, ver §2.9.

### 2.3. Adherencia semanal oficial, Aw
- **Artículo:** promedio sobre todas las sesiones planificadas.
- **Backend:** `WeeklyMetricsCalculator.calculate`, guardado en `weekly_user_metrics.weighted_adherence_pct`.
  - Aw = Σ cumplimiento / n.º de sesiones previstas, con una sola versión por día (`WeekPlanInput.unoPorDia`).
  - Una sesión sin registro, o una omisión declarada, cuenta como 0.
  - Cada sesión pesa 1, aunque la columna se llame «ponderada». El comentario de `V30` habla de ponderar por minutos y **no coincide con el código**; el código manda.
- **Coincidencia:** total en la fórmula. Es una discrepancia documental interna, no un defecto funcional.
- **Efecto:** ninguno.

### 2.4. Adherencia observada, Aobs
- **Artículo:** sobre las sesiones con registro.
- **Backend:** `observed_adherence_pct` = Σ cumplimiento de las sesiones con registro / n.º con registro.
  - Son registro: una sesión terminada (con cualquier porcentaje) y una **omisión declarada con motivo**, que cuenta al 0 %.
  - No son registro: una sesión sin nada, una sesión abandonada, o una vencida por el barrido (SKIPPED sin motivo).
- **Coincidencia:** total.
- **Efecto:** E5 → Aobs = 100.

### 2.5. Cobertura, C
- **Artículo:** sesiones con registro / sesiones planificadas.
- **Backend:** `registration_coverage_pct` = con registro / previstas × 100.
- **Coincidencia:** total.

### 2.6. Umbral de cobertura del 50 %
- **Artículo:** cobertura menor del 50 % → datos insuficientes.
- **Backend:** `EvidenceRule.enoughEvidence`, con `fitsense.adaptation.min-registration-coverage-pct=50`. Hay evidencia si C ≥ 50 y Aobs no es nulo; si no, `INSUFFICIENT_DATA`.
- **Coincidencia:** total.
- **Efecto:** E5 (C = 33,33) → datos insuficientes.

### 2.7. Variable de la decisión: **Aobs, no Aw**
- **Artículo:** las franjas (≥ 80, 50–80, < 50) se aplican a la adherencia semanal (Aw).
- **Backend:** `AdaptationOrderDecider.decide` usa **`observedAdherencePct` (Aobs)**. Aw solo se guarda y se informa.
- **Coincidencia:** **parcial (discrepancia F)**.
- **Efecto en los 20 casos: ninguno.**
  - En E1–E4 la cobertura es del 100 %, así que Aw = Aobs.
  - En E5 decide antes la regla de cobertura.
  - La diferencia aparecería con cobertura entre el 50 % y el 100 %, por ejemplo H/H/N: Aw = 66,7 y Aobs = 100, y el backend no adaptaría.

### 2.8. Franjas de decisión
Clase: `AdaptationOrderDecider`, con los umbrales `good_threshold_pct` = 80 y `moderate_threshold_pct` = 50 de `calculation_configs` (V7).

**Orden de evaluación:**
1. **Dolor.** Una omisión o un ejercicio dejado por dolor fuerza `ADAPT_LOCALIZED` (SEGURIDAD_ANTE_DOLOR), aunque la adherencia sea buena o falten datos.
2. **Cobertura insuficiente.** Da `INSUFFICIENT_DATA`.
3. **Aobs ≥ 80.** Da `NO_CHANGE`.
4. **Sin sesión problemática con registro.** Da `INSUFFICIENT_DATA` («no se adapta a ciegas»).
5. **Aobs < 50.** Da `ADAPT_EXPANDED`, que señala **todas** las sesiones con registro por debajo del 80 %.
6. **50 ≤ Aobs < 80.** Da `ADAPT_LOCALIZED`, que señala **solo la peor** sesión con registro (o todas las de dolor).

**Coincidencia:** total en umbrales y alcance. Tiene además reglas que el artículo no menciona: la prioridad del dolor y que una sesión sin registro nunca se señala.

**Efecto:** E3 señala S3; E4 señala S2 y S3. Ningún patrón incluye dolor.

### 2.9. Sesiones parciales (P40)
- **Backend:** el porcentaje real del ejercicio se limita a 0–100.
  - Con 40 % la sesión queda PARTIAL (≥ 30).
  - Por debajo del 30 % la sesión sigue contando con su porcentaje real.
- No hay forma de registrar «el 40 % de la sesión» como tal: se registran dosis por ejercicio.
- **Efecto:** hay que comprobar por caso si sale exactamente 40,00.

### 2.10. Ausencia de registro frente a omisión declarada
- **Backend:** sí las distingue.
  - La omisión declarada exige un motivo y cuenta como registro al 0 %.
  - La ausencia no cuenta como registro.
  - No existe el estado «vencida»: el barrido marca SKIPPED sin motivo, lo que tampoco es registro.

## 3. Qué ocurre después de la decisión: respaldo, NO_FACTIBLE e IA

Clases: `AdaptedWeekCommandService` y `AdaptedWeekProducer`.

**`NO_CHANGE` / `INSUFFICIENT_DATA`:**
- se **copia** la semana medida (validada por V8C y la revisión estricta);
- **nunca** se llama a la IA;
- V32 = `SIN_ORDEN`.

**`ADAPT_*`:**
- Cada sesión señalada recibe la intervención de su motivo (`TipoIntervencion.deMotivo`):

| Motivo | Intervención | ¿Necesita IA? |
|---|---|---|
| FATIGUE | REVISAR_CARGA_Y_RECUPERACION | Sí |
| TOO_DIFFICULT | VARIANTE_MAS_SENCILLA_O_CARGA | Sí |
| LACK_OF_MOTIVATION | INVESTIGAR_CAMBIO_MINIMO | Sí |
| CONSERVAR / CAUSA_NO_REGISTRADA | — | No (dan `SIN_EFECTO`) |

- **IA apagada o no disponible:** `RESPALDO` (la semana medida, copiada) o `NO_FACTIBLE` si la copia no valida. Con dolor, P-02/P-03.
- **IA encendida:**
  - hasta **3 intentos** (`AI_ATTEMPTS = 3`), cada uno validado (V8C + estricta) y con la respuesta del rechazo como retroalimentación;
  - un fallo del proveedor corta el bucle;
  - si fallan los 3: `RESPALDO`;
  - si hay al menos un cambio aplicado: `APLICADA` (origen AI). Sin ningún cambio: `SIN_EFECTO`.
- **Antes de llamar** se comprueba la factibilidad previa (`FactibilidadPreviaIA`).
- **Disyuntor:** 3 fallos seguidos del proveedor lo abren durante 10 minutos.

**Generación inicial** (`PlanGenerationPipeline`):
- con IA: hasta 3 intentos con IA y después el motor de reglas;
- sin IA: el motor de reglas;
- se valida siempre con `PlanDraftValidator` (V1–V25, 4C, 4D y V24/V25).

**Replicate:**

| Elemento | Valor |
|---|---|
| Clases | `ReplicateTrainingPlanGenerator` (transporte y generación) y `ReplicateSessionFixGenerator` (correcciones) |
| Endpoint | `openai/gpt-5-structured` |
| Modelo | `gpt-5-mini` |
| Esfuerzo | `medium` |
| Espera | Sondeo de hasta 150 s |
| Token | `REPLICATE_API_TOKEN` |

## 4. Consecuencia para los 20 escenarios (antes de ejecutar)

| Patrón | Decisión según el artículo | Decisión según el backend | Sin IA (etapa A) | Con IA (etapa C) |
|---|---|---|---|---|
| E1 | Sin cambio | `NO_CHANGE` | Copia (`SIN_ORDEN`) | Igual, sin llamada |
| E2 | Sin cambio | `NO_CHANGE` si P40 da exactamente 40 | Copia | Igual |
| E3 | Localizada | `ADAPT_LOCALIZED` sobre S3 | **`RESPALDO`: se decide adaptar, pero no se aplica** | Hasta 3 llamadas sobre S3 |
| E4 | Ampliada | `ADAPT_EXPANDED` sobre S2 y S3 | **`RESPALDO`** | Hasta 3 llamadas sobre S2 y S3 |
| E5 | Datos insuficientes | `INSUFFICIENT_DATA` | Copia | Igual, sin llamada |

**Sin IA, una decisión de adaptar no produce ninguna adaptación aplicada:** termina en `RESPALDO`. Así está diseñado (sin IA solo se aplican sustituciones de seguridad ante dolor), y la orden pide distinguirlo y no contarlo como adaptación generativa.
