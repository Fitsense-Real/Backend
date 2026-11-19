# 02 · Generación inicial del plan con IA

**Estado:** se mantiene el prompt **PR-1.7**, el único que existe en `generation` (`PlanPromptBuilder.VERSION = "PR-1.7"`). Las variantes PR-1.8 y pr-1.8a/b/c no se adoptaron; ver `05_EXPERIMENTOS_MEJORA_PROMPTS.md`.

Referencias de código: `generation` `543932a`. Abreviaturas:
- `P` = `src/main/java/main/web/services/fitsense/planning`;
- `props` = `src/main/resources/application.properties`.

## 1. Configuración

| Elemento | Histórica (hasta `543932a`) | Campañas evaluadas (GRA, MEJ, CONF-IA, pr-1.8a) | Incorporada a `generation` (PR #45) | Desplegada |
|---|---|---|---|---|
| IA | Apagada | Encendida (arnés, con token) | **Encendida por defecto** (`FITSENSE_AI_ENABLED`); sin `REPLICATE_API_TOKEN` genera el motor de reglas | No verificada |
| Generación | PR-1.7 | PR-1.7 (control) | **PR-1.7** | No verificada |
| Adaptación | v1 = FIX-PR-1.3 | V2.1 (control) | **V2.1 = FIX-PR-2.1-EXP** | No verificada |
| Política | PLAN-HIST-1 | PLAN-REV-2.0 | **PLAN-REV-2.0** (con `allow-candidates=true`) | No verificada |
| Progresión | Apagada | Solo en el bloque P (O15–O20) | **Encendida** (`FITSENSE_PROGRESSION_ENABLED`) | No verificada |
| Límite de dificultad | `off` | `preferencia` | **`preferencia`** | No verificada |
| Retirada localizada ante dolor | `off` | `on` | **`on`** | No verificada |
| V8C | `multiconjunto` (H-A1/H-A2 desde `543932a`) | GRA/MEJ/R2: anterior a la corrección; CONF-IA: corregido | `multiconjunto`, corregido | No verificada |

- **Modelo:** `openai/gpt-5-structured`, campo `model` = `gpt-5-mini`, razonamiento `medium`, límite de 150 s (sin cambios).
- **Antes del 11/11/2026**, los resultados de las campañas describían una configuración distinta de la que el código cargaba por defecto. Con la PR #45 coinciden.
- **Planes ya guardados:** conservan su política por linaje (PLAN-HIST-1 o la que tuvieran). REV2 solo se aplica a planes nuevos.
- **Desplegada:**
  - según la documentación del repositorio, Railway no despliega desde `generation` (`docs/FitSense/CATALOGO_V37.md`);
  - las variables de entorno de cualquier entorno desplegado **no se han verificado**;
  - incorporar la configuración a `generation` **no** significa que la IA funcione ya en ningún entorno.

## 2. Flujo

```
Perfil
  → comprobaciones (elegibilidad, P-01 minutos dentro del rango del objetivo, SafetyProfile)
  → catálogo filtrado (equipamiento, restricciones, bloqueos y lo que dolió, P-02)
  → contexto (PlanGenerationContext + política del linaje; snapshot guardado con el plan)
  → bloqueo previo de factibilidad (p. ej., FUERZA_SIN_TIEMPO en INCREASE_STRENGTH)
  → Replicate (prompt PR-1.7) → propuesta JSON
  → normalización mecánica → PlanDraftValidator V1–V27
      ├─ válida → plan publicado (origen IA)
      └─ inválida → retroalimentación con los motivos exactos → reintento (hasta 3 propuestas)
            └─ ninguna válida o proveedor caído → respaldo determinista (motor de reglas,
               mismo validador) → plan publicado (origen RULE_ENGINE)
                  └─ si también falla → la semana queda sin plan (PlanGenerationFailedException)
```

### Piezas
- **Preparación** (`WeeklyTrainingPlanCommandServiceImpl.prepare`):
  - perfil, P-01 y `SafetyProfile`;
  - catálogo elegible filtrado en la propia consulta;
  - exclusión de ejercicios con dolor;
  - comprobación previa 4C (por defecto solo la registra);
  - política heredada del linaje;
  - `PlanGenerationContext`.
- **Llamada:** `ReplicateTrainingPlanGenerator`, límite de 150 s.
- **Pipeline** (`PlanGenerationPipeline`):
  - 3 intentos de IA, con retroalimentación (`AiRetryFeedback`: propuesta rechazada, motivos, errores anteriores y pistas);
  - corte inmediato si cae el proveedor (cortacircuitos: 3 fallos seguidos, 10 min);
  - respaldo `RuleBasedTrainingPlanGenerator` validado con el mismo validador;
  - con REV2, alternativas deterministas acotadas (D-G3).
- **Validadores principales:**
  - V1: ejercicio elegible;
  - V2/V3: sesiones y días;
  - **V4: duración** (REV2: segundos estimados ≤ M × 60, sin tolerancia);
  - V5–V13: mínimos, dificultad, campos, carga, días y enfoque;
  - **V15: reparto por grupos**;
  - V16/V17: repeticiones y duración declarada;
  - V18: recuperación;
  - V21: descanso ≤ 180 s;
  - **V23: cobertura semanal de los 4 slots** (salvo LOSE_WEIGHT; por defecto `ENFORCE_AI_ONLY`, es decir, rechaza propuestas de la IA);
  - **V24: dosis** por clase (GAIN_MUSCLE) o por capacidad (resto), en `ENFORCE`;
  - **V25: capacidades principales factibles** (CARDIO en LOSE_WEIGHT, los 4 slots con STRENGTH en INCREASE_STRENGTH);
  - **V26: techos semanales de series por grupo** (12 / 20 / 24 según nivel; solo con políticas de volumen semanal como REV2);
  - V27: tracción real en PULL (REV2).
  - No existe V14.
- **La generación inicial no pasa por V8C.** Exige dosis exactas y no lleva autorizaciones.

## 3. Los cuatro objetivos

| Objetivo | Capacidades principales | Exigencias específicas |
|---|---|---|
| GAIN_MUSCLE | HYPERTROPHY | Dosis fija por clase (`MvpDosePolicy`): estructural 3 × 10 con 90 s, relleno con 2 series; 6–7 colocaciones por día. V23 (4 slots) |
| INCREASE_STRENGTH | STRENGTH | Los 4 slots con dosis de fuerza (V25); **3 × 6 / 150 s efectivas** (principiante 2 × 8 / 120 s). En el código (`CapabilityPrescriptionPolicy`) la base es 3 × 5, pero las repeticiones nunca bajan del mínimo de V16 de la zona y el nivel; con el mínimo general de 6, la dosis real es 3 × 6, y 3 × 10 en cintura. Bloqueo FUERZA_SIN_TIEMPO si no cabe |
| LOSE_WEIGHT | CARDIO | CARDIO en cada sesión factible (V25); cardio 3 × 120 s (principiante 3 × 90 s). V23 no se aplica |
| GENERAL_FITNESS | STRENGTH, MUSCULAR_ENDURANCE, CARDIO, MOBILITY | Ninguna principal factible puede faltar (V25); V23 (4 slots) |

Las capacidades secundarias nunca hacen fallar un plan (`GoalCapabilityPolicy`).

## 4. Resultados con IA real (referencia: GRA-R1 y GRA-R2)

**Identificación:**
- fecha: 06/11/2026, de 05:13 a 06:44 UTC;
- código: producción `f1c6c02`, sin cambios; congelación `b60e5ea`;
- versión del modelo: `9f4cd9ec…`;
- perfiles: O01–O20 + O11-F35;
- coste: 76 predicciones (generación + adaptación), USD 1,5368.

Fuente: `docs/FitSense/evaluacion_integral_O/generacion_ia_real/entregables/INFORME_INTEGRAL_GENERACION_ADAPTACION_IA_REAL.md` (INF), tablas 2 y 7.

### 4.1 Propuestas originales de la IA frente a planes aceptados

| | GRA-R1 | GRA-R2 |
|---|---:|---:|
| Perfiles con llamada | 20 (19 + O11-F35) | 20 |
| Llamadas de generación | 30 | 26 |
| **Propuestas originales válidas a la primera** | **13/20** (12/19 sin O11-F35) | **14/20** (13/19) |
| Necesitaron 2 / 3 propuestas | 4 / 3 | 6 / 0 |
| **Propuestas rechazadas** por V1–V27 | **10** | **6** |
| Motivos de rechazo | V15 × 6, V26 × 6, V23 × 2, V24 × 1, V4 × 1 | V26 × 5, V15 × 4, V13 × 1, V18 × 1 |
| **Planes aceptados y publicados desde la IA** | **20/20** | **20/20** |
| Planes por respaldo determinista | 0 | 0 |
| Sin plan (O11, 30 min, FUERZA_SIN_TIEMPO, sin llamada) | 1 | 1 |
| Planes publicados con infracciones | **0** | **0** |
| Estado de dosis del plan aceptado | 17 DOSIS_LIMITADA, 3 DOSIS_OBJETIVO | 17 / 3 |
| Latencia mediana (máx.) | 75,5 s (145,2) | 66,6 s (148,0) |
| Coste de generación | USD 0,7031 | USD 0,5659 |

**Lectura:**
- **La IA propone y el backend decide:**
  - un tercio de las propuestas originales tiene algún fallo, sobre todo de reparto (V15) y de techos (V26);
  - el backend las rechaza con el motivo exacto y el reintento corrige;
  - ninguna propuesta inválida llega al usuario.
- **Los planes aceptados son conservadores en dosis:** DOSIS_LIMITADA en 16 de los 19 de la cohorte, frente a 9 de 19 con el motor de reglas (R_O). Lo avisa `/plan/limitations`.
- **Reproducibilidad:**
  - planes idénticos entre R1 y R2: 0/20;
  - Jaccard de ejercicios: mediana 0,36 (rango 0,00–0,65);
  - todos son válidos.
  - La variabilidad es del modelo; la parte determinista (número de sesiones, decisiones) se reproduce exactamente.

### 4.2 Por objetivo
- **GRA** no desglosa por objetivo en el informe principal; el detalle por perfil está en la tabla 2 de INF.
- **Desglose por objetivo** con la misma cohorte y PR-1.7 en campañas posteriores (versión `045f8102…`, CONF-IA C1–C3, válidos a la primera):
  - LOSE_WEIGHT **14/15**;
  - GAIN_MUSCLE **12/15**;
  - GENERAL_FITNESS **5/15**;
  - INCREASE_STRENGTH **3/12**.
- **Patrón con PR-1.7:**
  - el objetivo con más rechazos es **fuerza** (V15/V26);
  - le sigue **condición general** (V23, cobertura de los 4 patrones);
  - pérdida de peso y ganancia muscular rara vez fallan a la primera.
- Con `81cb6179…` (A2), PR-1.7: LOSE_WEIGHT 5/5, GAIN_MUSCLE 5/5, GENERAL_FITNESS 2/4, INCREASE_STRENGTH 1/4.

### 4.3 Comparación con la línea base sin reglas
- **Mismo modelo, sin las reglas de FitSense (BASE):** 19/19 propuestas con infracciones en cada corrida.
- **Con reglas (GRA):** 7/19 y 6/19, con 0 planes publicados con infracciones.
- **Dos diferencias que hay que tener presentes:**
  - BASE recibió el catálogo elegible completo, sin el recorte V24/B1 del prompt de GRA (83 de sus 783 infracciones V24 son de ejercicios que GRA no habría ofrecido);
  - la versión del modelo de BASE no es verificable: se pidió `9f4cd9ec…` y el proveedor la devuelve como `hidden`.
- Ver `04_LINEA_BASE_SIN_REGLAS.md`.

## 5. Qué no demuestran estos resultados

- **Que los planes sean deportivamente óptimos:** los validadores verifican las políticas de FitSense, no el criterio de un entrenador. INF marca 6–7 planes por corrida como «requiere revisión» por observaciones del catálogo.
- **Que mejoren la adherencia de personas reales:** los perfiles y la adherencia son sintéticos (guiones congelados).

## 6. Evidencia
- **INF y tablas:** `generacion_ia_real/entregables/` (`TABLAS_GRA.md`, `RESULTADOS_GENERACION_REPLICATE.csv`, `SOLICITUDES_Y_RESPUESTAS_REPLICATE.csv`, `EVALUACION_20_PERFILES_IA_REAL.xlsx`).
- **ZIP:** `generacion_ia_real/entregables/GRA_EVIDENCIA_COMPLETA.zip`.
- **Integridad:**
  - `generacion_ia_real/SHA256SUMS.txt` (idéntico a `entregables/SHA256SUMS.txt`) tiene 65 entradas y se comprueba desde `generacion_ia_real/`;
  - en el repositorio dan **62 OK**. Las otras 3 son los registros `*.log` del ejecutor, que `.gitignore` excluye; sus copias dentro de `GRA_EVIDENCIA_COMPLETA.zip` coinciden con las huellas, así que el total es **65/65**;
  - `SHA256SUMS_CONGELACION.txt` 5/5 OK.
- **Versión del modelo:** `generacion_ia_real/evidencia/versiones.txt` (`9f4cd9ec…` antes de cada bloque y después del último).
