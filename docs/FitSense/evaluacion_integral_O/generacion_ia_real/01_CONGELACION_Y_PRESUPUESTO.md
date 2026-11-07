# GRA · 01 · Congelación del protocolo y presupuesto (antes de la primera llamada pagada)

**Este documento se commitea antes de GRA-R1.** Nada de lo que aquí figura se cambia después de ver resultados.

## Versión

| Elemento | Valor |
|---|---|
| Código de producción | Versión A `f1c6c02`, sin cambios (`git diff f1c6c02 HEAD -- src/main` vacío). V46, PLAN-REV-2.0, **P15-C apagado**, aviso E2-a |
| Generación inicial con IA | Flujo existente del backend (`PlanGenerationPipeline` + `ReplicateTrainingPlanGenerator`, prompt **PR-1.7**). Hasta 3 propuestas validadas con `PlanDraftValidator` y respaldo por reglas identificado (`RULE_ENGINE`). Ver `00_ARQUITECTURA_Y_DISENO.md` |
| Adaptación con IA | Igual que en R_O y R′_O (prompt V2.1 `FIX-PR-2.1-EXP`, validadores + V8C) |
| Modelo | Replicate `openai/gpt-5-structured`, `gpt-5-mini`, razonamiento `medium`, tiempo máximo 150 s (`application.properties`, sin cambios). La versión no se puede fijar (endpoint del modelo): se registra antes de cada bloque y después del último |
| Arnés | `EvaluacionK20IT` (commit `c8ff1bf`) con `generacion-ia=SI`, `integral-replicate=SI`, `autorizacion-replicate=SI`, `credencial=proxy`, `sin-metricas=RESERVA_SIN_BLOQUEO` |
| Cohorte | O01–O20 (`9b996825…`, sin cambios) y O11-F35 (`1720227e…`). Guiones congelados |
| Oráculo | **v1.4** (`herramientas/oraculo_integral_O_v1_4.py`). Para la elección de v1.3 frente a v1.2, ver la tabla «Oráculo» más abajo |
| Corridas | **GRA-R1** y **GRA-R2**, independientes, cada una en bases efímeras limpias (bloque G, bloque P y O11-F35). No se reutiliza ningún plan, respuesta ni adaptación de R_O o R′_O |

## Oráculo

| | Contenido |
|---|---|
| Base | v1.3, que incluye D-OR-3 (rama de dolor), diagnosticado en R′_O |
| Añadido en v1.4 | R-2: un plan S1 con `generation_source = AI` exige una propuesta de generación válida; el respaldo nunca cuenta como IA |
| Pruebas | `pruebas/pruebas_oraculo_v1_4.csv`, todas pasan: identidad con v1.3; pruebas de v1.3 (19 negativas, R-1 y D1–D5); G-C, G-V, G-N y G-E |
| Identidad | v1.4 = v1.3 byte a byte en S_O, D_O, HIST, la repetición S, R_O y R′_O |
| Si no apruebas v1.3 | Se informará también con v1.2 |

## Comprobaciones gratuitas (arnés `c8ff1bf`, IA de generación apagada)

| Comprobación | Resultado |
|---|---|
| Unitarias | 1306, 0 fallos (BUILD SUCCESS) |
| S_O con IA simulada | Huella `7eb13353…`, igual: **el modo experimental apagado reproduce la generación original** |
| D_O | Huella `d46b3f50…`, igual |
| Regresión U/N y HIST | REV2 D U `a662cb4d` · REV2 D N `11f51d44` · HIST D U `f9f67973` · REV2 IA simulada U `84dfb6d2` · REV2 IA simulada N `da34db35`: **las 5 iguales** a la referencia |

## Presupuesto

| | Predicciones | USD |
|---|---:|---:|
| Autorizado (global) | 168 | 8,40 |
| Gastado en R_O y R′_O | 20 | 0,3033 |
| **Remanente** | **148** | **8,0967** |

**Estimación de llamadas:**

| Concepto | Fuente | Por corrida |
|---|---|---|
| Generación | 20 perfiles con llamada (O11 se detiene antes por `FUERZA_SIN_TIEMPO`, sin llamar), 1–3 propuestas cada uno. La campaña de estrés midió 17 llamadas en 7 usuarios (≈ 2,4 por usuario), unos USD 0,028 de media | **Esperado** ≈ 30–50 llamadas, USD 0,8–1,4. **Peor caso** 60 llamadas, ≤ USD 3 |
| Adaptación | R_O y R′_O: 10 ciclos con IA, 1 llamada cada uno, unos USD 0,014. Con planes nuevos el número de ciclos puede variar | **Esperado** ≈ 10–15 llamadas, USD 0,2. **Peor caso** ≈ 30 |
| **Total por corrida** | | **Esperado** ≈ 40–65 llamadas, USD 1–1,6. **Peor caso** ≈ 90 |

**Viabilidad:**
- Dos corridas esperadas: ≈ 80–130 predicciones y USD 2–3,2, dentro del remanente.
- El peor caso de dos corridas (≈ 180 predicciones) **no** cabría. Por eso:
  - **GRA-R1 tiene un tope propio de la mitad: 74 predicciones y USD 4,04.** Así GRA-R2 conserva siempre su parte.
  - GRA-R2 usa el resto.
  - Antes de cada predicción: gasto medido + reserva de USD 0,05 ≤ remanente del tramo.
  - Si se alcanza el tope, las llamadas siguientes **no se hacen**. El backend aplica entonces su respaldo, que queda registrado como `PRESUPUESTO_AGOTADO_SIN_LLAMADA` y **nunca** se cuenta como generación o adaptación de la IA.
  - Si el presupuesto no alcanza para completar GRA-R2, se informa lo ejecutado.

**Ejecución por etapas:** GRA-R1 bloque G, revisión de que la traza y el control de coste funcionan, después GRA-R1 bloques P y F35, y por último GRA-R2 completa. No es una repetición selectiva: cada bloque se ejecuta una sola vez.

## Qué se mide por separado

| Etapa | Medidas |
|---|---|
| **A. Generación con Replicate** | Llamadas, propuestas válidas al primer intento, rechazadas y motivos, reintentos, planes publicados desde la IA (`AI`), planes por respaldo (`RULE_ENGINE`), sin plan, dosis y patrones, restricciones, tiempo y coste |
| **B. Adaptación con Replicate** | Ciclos con IA, aceptadas, rechazadas, SIN_EFECTO (evitables o no), respaldos, pertinencia, volumen y dificultad, sesiones no señaladas, progresiones y reincorporaciones posteriores, tiempo y coste |

Una generación válida y una adaptación válida **nunca** se suman en un mismo indicador.
