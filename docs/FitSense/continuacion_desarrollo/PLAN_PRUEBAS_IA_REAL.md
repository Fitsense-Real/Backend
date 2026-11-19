# Plan de pruebas con la IA real (Replicate): pendiente de autorización

**No se ha llamado a Replicate.** Este documento presenta el número de casos, el máximo de predicciones, el coste estimado, los criterios de aceptación y la evidencia que se recogería. Nada se ejecuta sin tu autorización expresa.

## 1. Comprobaciones sin coste (hechas, sobre `generation`)

| Aspecto | Estado verificado |
|---|---|
| **Generador real configurado** | `openai/gpt-5-structured` en Replicate, modelo `gpt-5-mini`, `reasoning_effort=medium` (`application.properties`; variables `FITSENSE_AI_MODEL` y `FITSENSE_AI_REASONING_EFFORT`) |
| **Prompts** | Generación PR-1.7 (`ConfiguracionEvaluadaTest`). Adaptación V2.1 (`fitsense.ai.adaptation.prompt-version=v2.1`). Política PLAN-REV-2.0 |
| **Contrato de entrada y salida** | Instantánea `PlanInputSnapshot` (se guarda en `input_snapshot`) y esquema JSON `PlanJsonSchema` / `SessionFixJsonSchema`. Pruebas: `ReplicateSchemaRoutingTest`, `SessionFix*Test` |
| **Timeouts** | `connect=10`, `read=90`, `poll-read=30` y sondeo total de 150 s por intento; `Prefer: wait=60`; sondeo cada 1 s. Pruebas: `ReplicateTimeoutsHttpTest` y `ReplicateSondeoRendimientoTest` |
| **Reintentos** | Hasta 3 intentos solo tras una propuesta **rechazada**. Un fallo del proveedor va directo al respaldo (R-1). Hay una comprobación previa de factibilidad que evita llamadas imposibles (H-G9). Pruebas: `PistasDeReintentoTest` y `ReplicateRespuestasInvalidasTest` |
| **Respuestas inválidas** | Un JSON roto, un esquema incumplido o una salida vacía no se publican: se reintentan o van al respaldo (R-1). `ReplicateRespuestasInvalidasTest` |
| **Ningún control se omite con el proveedor real** | La propuesta de la IA pasa por el mismo `PlanDraftValidator` (V1–V27) y V8C que el respaldo. El respaldo también se valida. Pruebas: `ConfiguracionEvaluadaIT` y campañas anteriores (GRA y BASE) |
| **Cancelación y gasto** | La predicción se cancela al agotar el sondeo (R-2); hay exclusión por persona y un límite de 5 por hora (R-5). Pruebas: `ReplicateCancelacionTest` y `AirCircuitBreakerTest` |
| **Registros y métricas** | `GENERACION_TIEMPOS`, `REPLICATE_TIMING`, `COBERTURA_SEMANA`, `V32` (intentos de adaptación) y `safe_regeneration_attempts` |

**Pendiente sin coste:** que la primera ronda del entorno de pruebas confirme `GENERACION_TIEMPOS` y `REPLICATE_TIMING` con el sondeo de 1 s. Ver `rendimiento_generacion/05`.

## 2. Matriz propuesta

### Bloque A · Generación inicial con la configuración evaluada (PR-1.7)

- **Casos:** 16 perfiles, con los cuatro objetivos × principiante/intermedio × gimnasio/casa.
- **Variación:** 3–4 días y 45–60 min.
- **Inclusiones:** las sesiones donde aplica la regla de la mitad de V15, y el perfil del caso 39 como control de linaje.

### Bloque B · Adaptación real (V2.1)

- **Casos:** 8 escenarios sobre semanas medidas: fatiga, falta de tiempo, dolor con sustituto, dolor sin sustituto (NO_FACTIBLE esperado), progresión y desmotivación.
- **Objetivos:** los cuatro, dos por objetivo.

### Bloque C · Evaluación pareada de H2 (solo si se autoriza)

- **Casos:** 30 perfiles. 24 de la rejilla donde aplica la regla (GENERAL_FITNESS/LOSE_WEIGHT e INCREASE_STRENGTH; UPPER, LOWER y FULL; principiante e intermedio) en PLAN-REV-2.0, más 6 de PLAN-HIST-1, incluido el 39.
- **Brazos:** PR-1.7 (control) frente a PR-1.9 (`fitsense.ai.generacion.contrato-v15=true`).
- **Diseño:** un intento por brazo y orden alterno.

## 3. Predicciones y coste

**Referencia:** USD 0,018–0,023 por predicción (GRA, gpt-5-mini, PR-1.7).

| Bloque | Casos | Previstas (media observada de 1,35–1,4 por caso) | Máximo (3 por caso) | Coste previsto | Coste máximo |
|---|---|---|---|---|---|
| A · Generación | 16 | ~22 | 48 | USD 0,40–0,51 | USD 1,10 |
| B · Adaptación | 8 | ~11 | 24 | USD 0,20–0,25 | USD 0,55 |
| C · H2 pareada | 30 × 2 | 60 (un intento por brazo) | 180 (si se mide el desenlace con 3 intentos) | USD 1,08–1,38 | USD 4,14 |
| **Total** | | **~93** | **252** | **≈ USD 1,7–2,1** | **≈ USD 5,8** |

**Tope propuesto: USD 8**, con un corte automático del arnés si se alcanza. Los precios reales los fija Replicate y pueden variar.

## 4. Criterios de aceptación

| Bloque | Criterio |
|---|---|
| A | 0 planes publicados con violaciones (validador independiente); 0 ejercicios contenidos; validez al primer intento ≥ la de GRA (27/40); mediana de latencia por predicción en el rango de GRA (±20 %) |
| B | 0 aumentos de dosis no autorizados (V8C, P-03); lo que dolió no reaparece; NO_FACTIBLE solo donde el oráculo lo espera |
| C (H2) | 0 rechazos V15 por la regla de la mitad en el tratamiento; ninguna otra categoría (V20, V23, V24, V26, V27) sube más de 1 frente al control; validez al primer intento ≥ la del control; latencia no peor en más del 10 % |

## 5. Evidencia que se recogería

Por cada predicción:

- el id de Replicate;
- el modelo y la versión del prompt;
- la instantánea (`input_snapshot`) y el prompt;
- la salida cruda;
- los motivos de rechazo y el desenlace;
- `REPLICATE_TIMING` (`predict_time`, cola y sondeo) y `GENERACION_TIEMPOS`;
- el coste según Replicate.

También el validador independiente (el oráculo en Python de campañas anteriores) y el resumen por bloque y por objetivo. Todo en `docs/FitSense/evaluacion_ia_real/`, como en GRA.

**No se usará ninguna salida simulada como prueba de que funciona el proveedor real.**

## Pendientes

- **Tu autorización**, con el tope USD 8 y los bloques que apruebes.
- Rama y arnés: reutilizar el arnés GRA (`generacion-ia`) con REPLICATE_API_TOKEN en el entorno de pruebas.
