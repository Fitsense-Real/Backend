# Auditoría integral del caso real · usuario 39

**Código auditado:** `generation` en `623c4faab5561e8f8f8150a7d6655e813e75d265`. Rama de auditoría: `audit/caso-usuario-39`.

**Alcance:**
- Sin llamadas a Replicate y sin gasto.
- Sin cambios en el producto: solo una prueba de investigación opt-in, scripts y documentación.

**Nivel de evidencia de cada afirmación:**
- **[C] COMPROBADO:** reproducido o leído en el código o en el log.
- **[I] INFERENCIA:** deducción técnica sólida, no reproducida.
- **[H] HIPÓTESIS:** necesita un experimento, en algunos casos pagado.

**Reproducción:** `Caso39ReproduccionIT` (`src/test/java/.../auditoria/caso39/`), con:
- PostgreSQL de Testcontainers;
- catálogo, validador, normalizador, motor de reglas y pipeline reales;
- el **JSON exacto** de la predicción `jhxmsrkj61rmr0d1t5trg6znj4`, que estaba íntegro en el log real;
- el mismo error del proveedor en el intento 2;
- `user_id = 39` y semana 2026-11-16, porque la semilla del motor de reglas depende de ellos.

```
./mvnw verify -Pintegration -Dit.test=Caso39ReproduccionIT -Dtest=NoUnit -Dsurefire.failIfNoSpecifiedTests=false \
  -Dfitsense.auditoria.caso39=true -Dfitsense.planning.policy=PLAN-HIST-1 \
  "-Dfitsense.auditoria.caso39.equipo-excluido=stability ball,medicine ball,wall bars,balance board,ab wheel"
```

Las evidencias están en `evidencia/`. La tabla final del documento lista cada archivo.

---

## 0. Resumen ejecutivo

1. **[C] Tu generación local NO se ejecutó con PLAN-REV-2.0, sino con PLAN-HIST-1.** Solo esa política reproduce a la vez:
   - el rechazo exacto del intento 1 (solo las dos V15);
   - las colocaciones del respaldo (`HYPERTROPHY=12, STRENGTH=4, MUSCULAR_ENDURANCE=8, CARDIO=4, MOBILITY=4`);
   - la distribución 4C del plan 78 y sus sesiones de 42–43 min (suelo V20 del 70 % de 60 min).

   Con PLAN-REV-2.0, el mismo JSON tendría además 5 rechazos V26 y el respaldo sería otro plan. La causa probable es el linaje del usuario: la política se hereda de su plan anterior. También podría ser una variable de entorno. Está pendiente de confirmar con una consulta SQL (§G).
2. **[C] V15 rechazó las dos sesiones de piernas** porque cada una ponía 4 de sus 6 ejercicios de trabajo en `upper legs`, y V15 no admite más de la mitad (3).
   - **Causa raíz [C]:** el prompt le dio al modelo un tope distinto, `max_per_body_part = 6`, calculado como la mitad de `min_exercises_per_session = 12`.
   - El modelo cumplió el número que se le dio y el validador aplicó otro.
3. **[C] El segundo intento recibió exactamente el JSON rechazado y los dos motivos.** No llegó a responder: seguía procesándose a los 211 s. Un timeout del proveedor corta los intentos por diseño y pasa al respaldo.
4. **[C] `goalText` solo llega al prompt**, como dato y con una frase genérica. Ningún componente determinista lo usa: ni el planificador, ni la elegibilidad, ni las dosis, ni los validadores, ni el motor de reglas. El plan 78, del motor de reglas, **no puede** reflejar «correr un maratón».
5. **[C] El plan 78 respeta las restricciones tal como el catálogo las define.** Los 32 ejercicios tienen `requires_hanging = false` y `requires_overhead = false`, y los filtros se aplicaron en la consulta.
   - **[I] Hay dos ejercicios cuyas instrucciones en español contradicen su equipo o su GIF** (1303 y 1152), y uno usa una estación de fondos con los pies sin apoyo.
   - No se ha demostrado una violación, pero necesita revisión profesional (§D.3).
6. **[C] La primera sesión no nace `IN_PROGRESS`.** Todas nacen `SCHEDULED` (comprobado en la reproducción). Solo `POST /workouts/{id}/start` pone ese estado, y el backend permite iniciar una sesión futura.
7. **[C] La reparación localizada es técnicamente viable en este caso** con PLAN-HIST-1:
   - sesiones superiores de la IA + inferiores del motor de reglas: VÁLIDA;
   - un cambio de un ejercicio por sesión inferior: VÁLIDA.

   Con PLAN-REV-2.0, una sesión que V15 no marcó incumple V26. Ninguna sesión puede darse por aprobada sola.
8. **[C] La app corta `POST /plan/generate` a los 30 s** (`receiveTimeout`), mientras el backend tardó 6 min. El usuario no ve los 366 s como una espera, sino como un error.
9. **No se encontró ningún hallazgo crítico con evidencia.** Los hallazgos altos son H1, H2, H3 y H4 (§F).

---

## A. Reconstrucción del caso

### A.1 Cronología (log real, hora -05:00)

| Hora | Evento | Fuente |
|---|---|---|
| ≤ 15:31:35.720 | Petición de generación inicial. Su inicio exacto no está en el log | — |
| 15:31:35.720 | Conjunto elegible: 350 (`LOWER_KNEE=27 LOWER_HIP=18 UPPER_PUSH=62 UPPER_PULL=33 fuera=210`) | `SlotCoverageGate` |
| 15:31:36.291 | Empieza el pipeline (`CAPACIDADES_4A`) | `PlanGenerationPipeline` |
| 15:34:10.579 | Intento 1 terminado: POST 61.288 ms + sondeo 92.901 ms (65 consultas) = 154.189 ms. `SUCCEEDED` | `REPLICATE_TIMING` |
| 15:34:10.597 | El normalizador corrige la duración declarada del día 20 (47 → 46 min) | `PlanDraftNormalizer` |
| 15:34:10.664 | Intento 1 rechazado: 2 × V15 | `PlanGenerationPipeline` |
| 15:36:43–46 | La app consulta el plan (404). Un segundo POST se rechaza con `GENERACION_EN_CURSO` sin llamar a la IA (R-5) | `GeneracionInicialService`, `CicloSemanalBloqueo` |
| 15:37:41.760 | El intento 2 sigue en curso tras 150 s de sondeo (POST 60.602 + sondeo 150.416, 119 consultas) | `ReplicateTrainingPlanGenerator` |
| 15:37:42.034 | Cancelación: `CANCELADA`, estado `canceled` (R-2) | `REPLICATE_CANCEL` |
| 15:37:42.036 | Fallo del proveedor, 1 de 3 para el cortacircuitos; pasa al respaldo | `PlanGenerationPipeline` |
| 15:37:42.222 | `GENERACION_TIEMPOS`: `ia_ms=[154286, 211371] validacion_ms=84 respaldo_ms=184 fitsense_ms=272 total_ms=365929` | idem |
| 15:37:47.109 | «Plan 78 generado … por RULE_ENGINE en 3 intento(s)» | `WeeklyTrainingPlanCommandServiceImpl` |

### A.2 En qué se fue el tiempo [C]

| Tramo | ms | % | Qué incluye |
|---|---|---|---|
| Intento 1 en Replicate | 154.189 | 41,5 | Predicción completa: el modelo razonando y escribiendo 8.361 caracteres de JSON |
| Prompt e instantánea del intento 1 | 97 | 0,0 | `ia_ms` menos `REPLICATE_TIMING` |
| Validación del intento 1 | 84 | 0,0 | Normalizador + V1–V27 |
| Intento 2 en Replicate | 211.018 | 56,8 | 60,6 s de POST + 150,4 s de sondeo, sin respuesta |
| Cancelación + prompt del intento 2 | 353 | 0,1 | `REPLICATE_CANCEL` tardó 274 ms |
| Respaldo (motor de reglas + validación) | 184 | 0,0 | |
| **Total del pipeline** | **365.929** | | |
| Antes del pipeline (preparar contexto, visible en el log) | ≥ 571 | | En la reproducción: 11 ms |
| Después del pipeline (persistir el plan) | 4.887 | | En la reproducción: 95 ms |
| **Visible de extremo a extremo** | **≥ 371.389** | | |

**Qué miden y qué no miden los tiempos registrados [C]:**
- `total_ms` mide solo el pipeline.
- `fitsense_ms = total − ia_ms` excluye la construcción del prompt, que va dentro de `ia_ms`.
- Ninguno de los dos incluye preparar el contexto ni persistir.
- **[I]** Los ~5,5 s fuera del pipeline, frente a 106 ms en local, apuntan a una base de datos lejana. Hay muchas idas y vueltas al guardar 4 sesiones, 32 ejercicios e instantáneas JSON de unos 100 KB.

### A.3 Preguntas de la fase 1

| # | Pregunta | Respuesta | Evidencia |
|---|---|---|---|
| 1 | ¿Qué consumió el tiempo? | El 98,3 % fue Replicate (dos predicciones), lo propio de FitSense es 0,27 s dentro del pipeline, y hay ~5,5 s fuera | §A.2 |
| 2 | ¿Qué incluyen los tiempos? | Ver §A.2 | `PlanGenerationPipeline.Tiempos` |
| 3 | ¿El intento 1 recibió el catálogo y las restricciones esperadas? | **[C] Sí, con PLAN-HIST-1.** Los 32 ejercicios del JSON están en el conjunto elegible reproducido (0 fuera). La huella de elegibilidad coincide exactamente con la real (350; 27/18/62/33/210). El prompt reproducido mide 115.051 caracteres frente a 114.998 reales: 53 de diferencia, compatibles con algún código de equipo sin efecto en la elegibilidad que también viaja en el prompt [I] | `RESUMEN_reproduccion_PLAN-HIST-1.txt`, `busqueda_equipo_resultado.txt` |
| 4 | ¿El intento 2 recibió el JSON y los errores? | **[C] Sí, exactos.** `feedback.rejectedOutput()` es igual al JSON del intento 1 y `rejectionProblems` a los 2 motivos V15. El prompt 2 = prompt 1 + bloque «INTENTO ANTERIOR RECHAZADO» (+9.743 caracteres) | Aserciones de la prueba, `prompt_intento2_reproducido.txt` |
| 5 | ¿Por qué no hubo tercer intento? | **[C] Por diseño.** `AiProviderUnavailableException` hace `break` (`PlanGenerationPipeline.java`, «El proveedor está caído: no tiene sentido gastar el segundo intento»). La predicción que sigue en curso se trata como fallo del proveedor y cuenta para el cortacircuitos (1 de 3) | Código, log 15:37:42.036 |
| 6 | ¿Por qué dice «3 intentos»? | **[C]** `generation_attempts` cuenta 2 intentos de IA + 1 del respaldo, que se suma a propósito «para reflejar el coste real» (`PlanGenerationPipeline`, `attempts++` antes del respaldo) | Reproducido: `generation_attempts=3` |
| 7 | ¿Algo inesperado en R-1…R-5? | Ver abajo | |

**Detalle de R-1 a R-5 [C]:**
- **R-1:** no intervino. El JSON era válido sintácticamente.
- **R-2:** correcto. Una sola cancelación, confirmada, y sin respuesta tardía aprovechada.
- **R-3:** correcto. El POST esperó hasta 60 s y el sondeo hasta 150 s.
- **R-4:** no intervino.
- **R-5:** correcto. El POST concurrente se rechazó sin coste.

Hay dos observaciones de diseño:
- **El timeout del intento 2 cuenta como fallo del proveedor** para un cortacircuitos de 3 fallos y 10 min de descanso (`AirCircuitBreaker`). Tres generaciones lentas seguidas abrirían el corte para todas las peticiones durante 10 min. **[I]** Esto supone que el bean es único, que es lo habitual.
- **La app no espera la respuesta (H3).** Usa `receiveTimeout: 30 s` en `dio_client.dart` para todas las llamadas, incluida `POST /users/me/plan/generate`. **[I]** La consulta y el reintento de las 15:36 encajan con un usuario que vio un error y volvió a intentarlo.

### A.4 Política realmente usada (H1) [C]

| Huella | Log real | PLAN-HIST-1 reproducido | PLAN-REV-2.0 reproducido |
|---|---|---|---|
| Motivos del intento 1 | 2 × V15 | **2 × V15 (idénticos)** | 2 × V15 + 5 × V26 |
| Colocaciones del respaldo | H12 S4 ME8 C4 M4 | **H12 S4 ME8 C4 M4** | H2 S4 ME8 C8 M4 B1 |
| Distribución 4C del respaldo | K[0,2,0,2] H[0,1,0,0] P[3,0,3,0] T[3,0,2,0] | **idéntica** | otra |
| Duración de las sesiones | 42–43 min | **42–43 min** | otra |

**Cómo se elige la política (`WeeklyTrainingPlanCommandServiceImpl.java:208-212`):**
- La configuración solo decide para usuarios **sin planes previos**.
- Una semana nueva hereda la política del plan anterior del usuario (su linaje).

Si el usuario 39 tenía planes de antes con PLAN-HIST-1, heredó esa política aunque la configuración diga PLAN-REV-2.0. **Pendiente de confirmar con la consulta 1 de §G.**

**Consecuencia:** las pruebas locales con usuarios antiguos no evalúan la configuración que se presenta como vigente.

---

## B. Auditoría de V15

### B.1 Qué ejercicios provocaron el rechazo [C]

Datos de `V15_desglose_intento1.csv`, con el catálogo real.

| Sesión | Ejercicios de trabajo (6) | `upper legs` | Complementos fuera de V15 |
|---|---|---|---|
| 19-nov LOWER_BODY | 1004 Peso muerto con palanca · 864 Abdominal medio (waist) · 144 Sentadilla con banda · 122 Elevación de cadera con banda · 294 Elevación de talones (lower legs) · 1112 Inclinación pélvica hacia puente | **1004, 144, 122, 1112 = 4** | 1325 cinta (CARDIO), 978 estiramiento (MOBILITY) |
| 22-nov LOWER_BODY | 1005 Extensión de cadera con palanca · 1301 Crunch invertido suspendido (waist) · 1073 Puente de glúteos · 1114 Sentadilla con apoyo · 1253 Gemelos en escalón (lower legs) · 1199 Abducción de cadera | **1005, 1073, 1114, 1199 = 4** | 1117 pies rápidos (CARDIO), 1151 estiramiento (MOBILITY) |

**Qué exige V15 (`PlanDraftValidator.validateFocusCoverage`, ~líneas 762-870):**
- `LOWER_BODY` admite `upper legs, lower legs, waist` (`WorkoutFocus.java:12`).
- Hay que cubrir al menos 2 grupos.
- Ningún grupo puede superar la mitad de los ejercicios de trabajo (`cuantos > deTrabajo.size() / 2`), cuando el enfoque tiene 3 o más grupos y hay 4 o más ejercicios.

**Por qué evalúa 6 de 8:** los complementos de capacidad (cardio y movilidad que entran por 4A v6, `CatalogEligibilityPolicy.complementos`) no cuentan ni como grupo propio ni como grupo ajeno. Por tanto, el máximo es 3 de 6.

**Clasificación de esos ejercicios [C]:**
- Los ocho de `upper legs` tienen `body_part = upper legs` en el catálogo.
- 1004/1005 (palanca de cadera), 144 (sentadilla) y 122/1073/1112 (puentes y elevaciones de cadera) son trabajo de muslo y cadera. La clasificación es coherente.
- El rechazo no se debe a un error de catálogo.

**Reproducibilidad [C]:** con PLAN-HIST-1, el validador actual devuelve exactamente los dos mensajes del log (`motivos_intento1_identicos_al_log_real=true`).

### B.2 ¿El prompt permitía cumplir V15? Causa raíz (H2) [C]

`constraints.suggested_split` del prompt real reproducido, para LOWER_BODY:

```json
{"focus_code":"LOWER_BODY","body_parts":["upper legs","lower legs","waist"],"min_body_parts":2,"max_per_body_part":6}
```

- **El prompt da un tope de 6.** La lista de comprobación final dice «ningún body_part pasa del max_per_body_part de su sesión».
  - Ese tope sale de `PlanInputSnapshot.suggestedSession`: `min_exercises_per_session / 2 = 12 / 2 = 6`.
  - El comentario del código supone que «si la IA pone más ejercicios, V15 admitirá algo más, nunca menos». Aquí es al revés.
- **Por qué es al revés:**
  - `max_colocaciones_dia = 8`, de modo que nunca puede haber 12 ejercicios.
  - V15 descuenta los dos complementos.
  - El tope real era 3, no 6.
- **El mismo prompt pide cosas incompatibles:** `min_exercises_per_session = 12` junto a `max_colocaciones_dia = 8`.
- **Con PLAN-REV-2.0** `max_per_body_part` llega `null`: no se da ningún número, y la regla de la mitad sigue aplicándose.

**Conclusión [C]:** el modelo cumplió la cuota que se le comunicó (4 ≤ 6) e incumplió la que se verifica (4 > 3). Esto, y no el catálogo ni el validador, es lo que convirtió una semana corregible en el reintento de 211 s y en el respaldo.

### B.3 Feedback del reintento [C]

- **Correcto y accionable en el texto.** Lleva la propuesta completa, los dos motivos («dedica 4 de 6 ejercicios a upper legs. Reparte entre los grupos del enfoque LOWER_BODY») y la instrucción explícita «Si sobran de un grupo muscular, REEMPLÁZALOS por otros del enfoque».
- **Limitación:** el mensaje no dice el tope numérico (3) y el bloque de restricciones sigue diciendo 6. **[H]** Es posible que el modelo hubiera repetido el error; no se puede saber, porque la predicción no terminó.

### B.4 Viabilidad de la reparación localizada (sin implementarla) [C]

Experimentos con el validador completo, en `reparacion_localizada_*.csv`. Ninguna de estas semanas se publica ni se presenta como de la IA.

| Experimento | PLAN-HIST-1 | PLAN-REV-2.0 |
|---|---|---|
| E1 JSON de la IA tal cual | Rechazado (2 × V15) | Rechazado (2 × V15 + 5 × V26) |
| E2 Superiores de la IA + inferiores del motor de reglas, reensambladas | **VÁLIDA** | Rechazada: V26 en la sesión **superior** del 20 (9 series de upper arms, techo 6) y techo semanal de upper arms (15 > 12) |
| E3 La IA con 1 cambio por sesión inferior (1112 → 1141 2×13; 1199 → 134 3×15) | **VÁLIDA** | Rechazada (V26 en piernas y brazos) |
| E4 Motor de reglas (plan 78) | VÁLIDA | VÁLIDA |
| E5 Solo las 2 sesiones superiores de la IA | Rechazada (V2: se esperaban 4; V23: faltan LOWER_KNEE y LOWER_HIP) | Rechazada (V2, V23, V26) |

**Conclusiones:**
1. **Técnicamente viable [C].** Un cambio mínimo y determinista habría dado en este caso un plan de IA válido **sin segunda predicción**: unos 211 s menos [I].
2. **Una sesión no queda aprobada porque V15 no la marque [C].** E2 con REV-2.0 lo demuestra: la sesión del 20 tenía V26. Las reglas semanales (V2, V23, V25, V26 semanal y V8 si hay ajuste) solo se pueden comprobar sobre la semana completa (E5).
3. **Requisitos de una reparación segura:**
   - reensamblar y pasar el validador completo sobre la semana entera;
   - reparar como mucho las sesiones con motivos, sin suponer que las demás son válidas;
   - publicar solo si la semana completa pasa;
   - si no, seguir el camino actual (reintento o respaldo).
4. **Efectos por regla:**
   - **V15** es por sesión.
   - **V23** (4C) y **V25** (capacidades) son semanales: cambiar una sesión puede descubrir un slot o una capacidad.
   - **V24**: las dosis del ejercicio nuevo deben ser de su capacidad (E3 usa las dosis del motor de reglas).
   - **V26** (solo REV-2.0) suma por sesión y por semana.
   - **V27** (PULL real, REV-2.0) es por sesión PULL.
   - **V13**: los enfoques no cambian si se conservan fecha y foco.

---

## C. Auditoría del objetivo libre (`goalText`)

### C.1 Trazabilidad [C]

Leído por un subagente y verificado en los puntos clave.

| Componente | ¿Lo recibe? | ¿Lo usa? | Qué puede cambiar |
|---|---|---|---|
| DTO de entrada (`CreateUserProfileResource`, `UpdateUserProfileResource`) | Sí. `@NotBlank`, máx. 500 caracteres | No: solo lo valida | Nada |
| Persistencia (`TrainingGoal`, `user_profiles.goal_text`, V1) | Sí | No | Nada. El comentario de la columna dice «Va al prompt de la IA» |
| `GeneracionInicialService` | No | No | Nada |
| `WeeklyTrainingPlanCommandServiceImpl` | Va dentro de `PlanningProfile` | No: solo usa `goalType` (P-01) | Nada |
| PLAN-REV-2.0 / PLAN-HIST-1 (`PlanningPolicy`, `GoalCapabilityPolicy`, `DosisSemanalPolicy`) | No | No | Capacidades, dosis y minutos dependen **solo de `goalType`** |
| Contexto y elegibilidad (`ExternalCatalogService`, `CatalogEligibilityPolicy.decidir(c, goalType)`) | En el perfil | No | Nada |
| Prompt PR-1.7 (`PlanInputSnapshot` → `user.goal_text`; `PlanPromptBuilder.java:230-232`) | Sí | **Solo como contexto**: «elige pensando en el objetivo del participante (user.goal_type y user.goal_text)…» | Lo que el modelo quiera elegir **dentro** de lo que el resto del sistema permite. No dice qué hacer si contradice `goal_type` ni lo marca como dato no confiable |
| Petición a Replicate | Solo dentro del prompt | — | — |
| Validación (V1–V27, V8C) | No | No | **Ninguna regla comprueba que el plan se adecúe al objetivo escrito** |
| Motor de reglas (respaldo) | No | No | Nada: decide por `goalType` (`RuleBasedTrainingPlanGenerator`, `CapabilityWeekBuilder`) |
| Plan guardado y API | Copia literal en `input_snapshot` como evidencia | No | Solo `plan_name` y `rationale`, si el modelo lo parafrasea |
| Adaptación V2.1 | Sí, en el JSON | No: ninguna instrucción lo menciona | Nada |

**Respuesta principal [C]:** FitSense personaliza por `goalType` y por los campos numéricos y de seguridad del perfil. `goalText` es **contexto descriptivo para el modelo**, sin ninguna garantía de influencia. En el respaldo no influye nada.

**Evidencia de que el modelo sí lo leyó [C]:** el intento 1 se llamó «Semana 1 - Fuerza y resistencia orientada a running» y su `rationale` habla de capacidad aeróbica. Aun así, su contenido fue una semana general de fuerza con un cardio de 3 × 90 s por sesión. La dosis de cardio la fija el backend para principiante (`CapabilityPrescriptionPolicy`), así que el modelo **no podía** pautar carreras continuas largas.

**Riesgo de inyección en el prompt [I]:**
- Hasta 500 caracteres de texto libre llegan al modelo sin la advertencia «esto es un dato, no una instrucción».
- Jackson escapa el JSON, así que no se puede romper la estructura.
- Todo lo que sale pasa los validadores deterministas. Lo peor que puede pasar es que sesgue la elección dentro del espacio válido o cambie el nombre y la justificación.
- Riesgo medio-bajo.

### C.2 GENERAL_FITNESS frente a preparación de un maratón [C] [I]

- **GENERAL_FITNESS** (matriz `GoalCapabilityPolicy`):
  - principales: STRENGTH, MUSCULAR_ENDURANCE, CARDIO y MOBILITY;
  - secundarias: HYPERTROPHY y BALANCE;
  - cardio: 1 bloque por sesión en el motor de reglas (LOSE_WEIGHT tiene 2) de 3 × 90 s para principiante;
  - objetivo aeróbico semanal de 30 min, solo en PLAN-REV-2.0 (`DosisSemanalPolicy`), y es un objetivo de llenado, no una regla de rechazo.
- **Lo que no existe en FitSense [C]:**
  - carrera continua o progresión de distancia o tiempo;
  - tirada larga, zonas de intensidad o periodización hacia una fecha;
  - control del volumen de impacto semanal de la carrera;
  - tests de partida.

  Ningún objetivo soportado, ni siquiera LOSE_WEIGHT, es entrenamiento específico de resistencia de carrera.

---

## D. Auditoría del plan 78

El plan 78 se reproduce con PLAN-HIST-1 y el equipo inferido. Tiene las mismas sesiones, duraciones (43/42/43/43), huellas y los ejercicios 1303 y 910 que describes. El detalle está en `plan78_respaldo_reproducido.csv`.

**[I] Reserva:** no tengo el JSON del plan 78 real. La identidad se apoya en cuatro huellas independientes que coinciden. La consulta 2 de §G permite comparar ejercicio a ejercicio.

### D.1 Evaluación estructural [C]

- **Distribución:** lunes y viernes UPPER_BODY, jueves y domingo LOWER_BODY. Cada sesión tiene 6 ejercicios resistidos, 1 cardio por duración y 1 movilidad.
- **Validez técnica:** cumple V1–V27 según la política del linaje (PLAN-HIST-1).
- **4C:** cubre los cuatro slots, con UPPER_PULL concentrado el lunes y el viernes.
- **Duración:** 42–43 min, frente a los 60 pedidos. PLAN-HIST-1 llena hasta el suelo V20 (70 % de 60 = 42), no hasta 60. **No es un defecto de esta semana:** es la política histórica.

### D.2 Adecuación al objetivo

| Dimensión | ¿Qué hay? | Valoración |
|---|---|---|
| Condición física general | Fuerza de tren superior e inferior, core, gemelos, movilidad | **[C] Adecuada** a GENERAL_FITNESS principiante |
| Relación directa con correr | Gemelos (493, 134), unilateral y rodilla (507 sentadilla curtsy, 1315 zancadas), estiramientos de gemelo (1255, 1136, 454), 1325 caminata en cinta inclinada | **[I] Indirecta y por coincidencia:** salen de la plantilla, no del objetivo |
| Estímulo cardiovascular | 4 colocaciones × 3 × 90 s = **18 min de trabajo a la semana**. Solo 1325 es locomoción en cinta, y es caminando | **[C] Cobertura mínima de CARDIO**, no un estímulo de preparación de carrera |
| ¿El motor de reglas tiene en cuenta el texto? | No | [C] |
| ¿La distribución se diseñó según el texto? | No: sale de `WeeklySplitPlanner` por días y nivel | [C] |
| ¿Los validadores comprueban la adecuación al texto? | No | [C] |

Lo que FitSense garantiza y lo que no:
- **Validez técnica: SÍ.** Restricciones del sistema, dosis, duración, cobertura y seguridad declarada.
- **Adecuación funcional: SÍ, para GENERAL_FITNESS.**
- **Personalización semántica: NO garantizada.** Solo la IA ve el texto y nada lo verifica.
- **Especialización deportiva: NO existe.** No hay ningún modelo de preparación de un maratón.

Que el plan no prepare un maratón no lo hace inválido. Que cumpla V1–V27 tampoco significa que responda a ese objetivo.

### D.3 Seguridad y restricciones

Restricciones del perfil: `allowHanging=false` y `allowOverhead=false`. El resto está permitido.

**Definiciones vigentes [C]** (V25, comentarios de columna):
- `requires_hanging`: «El cuerpo cuelga del agarre, pies sin apoyo. Incluye asistidas.»
- `requires_overhead`: «Brazos por encima de la cabeza en alguna fase.»
- Los filtros se aplican en la consulta del catálogo (`ExerciseRepository.findEligible`): la IA y el motor de reglas no ven lo excluido.

**Resultado [C]:** 32 de 32 ejercicios tienen `requires_hanging = false`, `requires_overhead = false` y `high_impact = false`.

| Id | Catálogo actual | Revisión visual previa (GIF) | Instrucciones `instructions_es` | Valoración |
|---|---|---|---|---|
| **1303** Elevación de piernas torsionada | equipo `parallel bars`, colgarse = no, sobre la cabeza = no, suelo = no, dificultad 1 | B10: «Apoyo en la estación de fondos», «de pie en la estación» | «**Túmbate sobre tu espalda**… manos debajo de los glúteos» | **[C] Las instrucciones contradicen el equipo y el GIF.** **[I]** En la estación de fondos el cuerpo se sostiene con los brazos y los pies sin apoyo. No es «colgar del agarre» según la definición, pero es una carga en suspensión sobre los hombros que ninguna restricción contempla. Para un principiante de 100 kg que declaró «no colgarse», necesita revisión profesional |
| **1152** Fondos invertidos | `body weight`, suelo = sí | B06: «Tumbado y sentado en el suelo; no usa paralelas» | «**Colócate entre dos barras paralelas** con los brazos completamente extendidos» (fondos en paralelas) | **[C] Las instrucciones describen otro ejercicio**, el fondo en paralelas: apoyo suspendido y extensión profunda del hombro. Si el usuario sigue el texto en vez del GIF, hace un ejercicio más exigente que el clasificado |
| **910** Remo invertido | `pull-up bar`, colgarse = no, suelo = sí | B01: «Tumbado bajo la barra con talones en el suelo» | «barra a la altura de la cintura… talones en el suelo» | **[C] Compatible con la definición:** los pies están apoyados. **[I]** Problema práctico: una barra de dominadas alta no sirve. Hace falta una barra baja (Smith o rack) |
| 314 Fondos en banco | `bench` | B: «Manos en un banco» | Coherentes | Compatible. El hombro va en extensión, no sobre la cabeza |
| 707 Elevación frontal sentado | `dumbbell` | B05: sin error | «hasta la altura de los hombros» | Compatible |
| **853** «Media sentadilla» (cardio) | `LOCOMOTION_CARDIO` | Grupo: «Marcha con rodillas altas» | «baja el cuerpo como si te fueras a sentar» (sentadilla) | **[C] El nombre y las instrucciones no corresponden al GIF.** El usuario puede hacer sentadillas en vez de cardio |
| 1093 «Empujar para correr» (cardio) | `body weight` | B02: «De pie elevando rodillas; el nombre no coincide» | Flexión + rodillas al pecho | **[C] Instrucciones distintas del GIF** (ya conocido, gravedad baja) |

**Conclusión [C] [I]:**
- **Violaciones:** no se demuestra ninguna de las restricciones declaradas tal como el sistema las define.
- **Riesgos de seguridad reales (H4):**
  1. Instrucciones contradictorias en 1303 y 1152, de las que una describe un ejercicio en paralelas.
  2. Un hueco de definición: el apoyo suspendido con los pies sin apoyo no queda cubierto por «colgarse».

**Propuesta, no aplicada:**
- revisión profesional del 1303 y el 1152 con el GIF delante;
- corregir `instructions_es` con una migración de datos auditada, como V36/V37;
- decidir de forma explícita si «no colgarse» debe excluir también el apoyo suspendido en paralelas o en una silla romana.

No se cambia ninguna clasificación basándose solo en el nombre.

### D.4 Estado `IN_PROGRESS` antes de la fecha [C]

- **Al crear el plan todas las sesiones son `SCHEDULED`:**
  - lo fija el constructor (`PlannedWorkout.java:119`);
  - la reproducción lo comprueba (`DISTINCT status = [SCHEDULED]` tras publicar).
- **Solo una ruta escribe `IN_PROGRESS`:** `POST /workouts/{id}/start` → `markInProgress` (`PlannedWorkout.java:143`).
- **El inicio no tiene límite inferior de fecha.** `WorkoutSessionCommandServiceImpl.handle(StartWorkoutSessionCommand)` comprueba abierto, no vencido, no bloqueado por seguridad y sin otra sesión abierta. **Nunca comprueba `scheduledDate`.**
- **La app** muestra «Comenzar entrenamiento» en la próxima sesión pendiente sin mirar la fecha (`workout_detail_page.dart`).
- **No depende de la zona horaria:** el estado inicial es constante.

Para plan 78: **[I]** alguien pulsó «Comenzar» en la sesión del lunes 16 antes de esa fecha. La consulta 3 de §G lo confirma.

**Consecuencias (lectura de código del subagente, verificada en la guarda de inicio):**

| Área | Efecto |
|---|---|
| Adherencia | `IN_PROGRESS` cuenta como no terminado, igual que `SCHEDULED`. Si se completa, cuenta para el lunes aunque se haga el jueves |
| Caducidad | El trabajo de las 00:30 trata `IN_PROGRESS` igual que `SCHEDULED`: pasa a `SKIPPED` |
| Ciclo semanal | Sin resultado registrado, es lo mismo que «sin registro» |
| Reparación de seguridad | **Queda bloqueada** mientras haya una sesión `IN_PROGRESS`, aunque no haya ninguna abierta (`PublishedPlanSafetyRepairService`) |
| Abandono | Abandonar una sesión no devuelve el estado a `SCHEDULED`: el `IN_PROGRESS` se queda pegado |
| App | Muestra «En curso» y «Continuar», y oculta «No podré hacerla» |

**Valoración:** no es un estado intencional. Es un defecto de las guardas (H8, gravedad media).

---

## E. Comparación de alternativas (sin implementar)

| Alternativa | Viabilidad | Beneficio esperado | Riesgo | Cambios necesarios |
|---|---|---|---|---|
| **A. Generación semanal actual** | Ya está en uso | Planes de IA validados cuando el primer intento sale bien | Latencia: 154 s + 211 s en este caso [C]; p50 ~60–75 s y p95 ~270 s en GRA [C]. Con la app cortando a los 30 s, **la espera síncrona no llega al usuario** (H3) [C] | Ninguno, pero H2 seguiría provocando rechazos evitables |
| **B. Reparación localizada** | **Alta en este caso** (E2 y E3 válidas con HIST) [C]. Con REV-2.0 depende de V26 [C] | Determinista (E3): 0 predicciones extra, ~0,1 s [I]. Pedir a la IA solo 2 sesiones: 1 predicción con la mitad de salida, **[H]** ~35–70 s, sin medir | Romper reglas semanales (V23, V25, V26, V8). Exige validar la semana completa (E5). Un cambio determinista elige ejercicios «a mano», con riesgo de calidad | Reensamblado y validación completa. Para la versión con IA, un contrato nuevo («corrige solo las sesiones X»), que cambia el prompt y requiere autorización y evaluación pagada |
| **C. Generación por día** | Media-baja [I] | **[H]** 4 predicciones en paralelo con menos salida cada una; latencia ≈ la de la más lenta | Las reglas semanales (V2, V13, V23, V25, V26, V8) obligan a planificar antes cuotas por día y a validar después la semana. Coste de entrada ×4: cada predicción lleva ~97 K caracteres de catálogo [C]. Concurrencia y límites de Replicate. Mostrar una sesión antes puede quedar invalidado por la semana | Rediseño del pipeline, del prompt, de la persistencia y de la adaptación. Es el cambio más grande |
| **D. Respaldo inmediato y mejora posterior** | Media-alta [I]. La infraestructura de versiones ya existe (`plan_version`, `parent_plan_id`, `REPLACED`) [C] | Respuesta en ~0,3 s + persistencia [C], compatible con el timeout de la app. La IA mejora en segundo plano | Sustituir un plan ya visto o empezado. `markReplaced` convierte `IN_PROGRESS` en `REPLACED` y puede dejar una sesión abierta huérfana [I]. Adherencia y progresión deben apuntar a la versión vigente. El respaldo es válido pero genérico e ignora `goalText` [C] | **Prohibir sustituir** si: hay alguna sesión iniciada o con registro; ha llegado la fecha de la primera sesión; han pasado más de N minutos; o el usuario no lo acepta. Aviso en la app. Pruebas de versionado y adherencia. Cambia el flujo del producto |
| **E. Reducir razonamiento o contexto** | Alta técnicamente; requiere evaluación pagada [C] | Prompt de 115.051 caracteres [C]: instrucciones 13.145 (11 %); `available_exercises` 97.440 (85 %; 336 ejercicios × ~290); restricciones 2.469; usuario 684. Por campo del catálogo: `dosis` 18 %, `name` 14 %, `movement_pattern` 13 %, `prescription_type` 11 %. Reintento +9.743 (el JSON rechazado son 8.361). **[I]** La latencia la dominan la salida y el razonamiento (GRA: ~7.000 tokens de salida de mediana), así que reducir la entrada da una ganancia moderada; bajar `reasoning_effort` pesa más [H] | Menos calidad o más rechazos (V15, V26), sin medir | Configuración (`FITSENSE_AI_REASONING_EFFORT`) o filtrado previo del catálogo. Cambia lo que ve el modelo: requiere autorización y evaluación |

**Duplicaciones detectadas en el prompt [C]:**
- Las listas de ids de CARDIO (6) y MOBILITY (27) de la cabecera repiten ejercicios que ya están en `available_exercises`. Pesan poco.
- El JSON ya va compacto: el formato solo ahorraría 3 caracteres.
- Se envían 336 de 350 elegibles para usar 32 posiciones.

---

## F. Hallazgos y recomendación

### F.1 Hallazgos clasificados

| Id | Gravedad | Hallazgo | Evidencia |
|---|---|---|---|
| H1 | **Alto** | La generación real usó PLAN-HIST-1, heredada del linaje, en lugar de la PLAN-REV-2.0 configurada. Las pruebas locales con usuarios antiguos no evalúan la configuración vigente | [C] §A.4. Causa por confirmar (§G-1) |
| H2 | **Alto** | Contrato prompt–validador incoherente en V15. Con HIST, `max_per_body_part = 6` frente al 3 real. Con REV, `null`. Además `min_exercises_per_session = 12` con `max_colocaciones_dia = 8`. Es la causa directa del rechazo, del reintento de 211 s y del respaldo | [C] §B.2 |
| H3 | **Alto** | La app corta la generación a los 30 s y el backend tarda minutos. El usuario ve un error y reintenta (409) | [C] `dio_client.dart`, log. Flutter no se ha tocado |
| H4 | **Alto** (seguridad, pendiente de revisión profesional) | Instrucciones contradictorias con el GIF o el equipo en 1303 (paralelas frente a suelo) y 1152 (texto de fondos en paralelas). Hueco de definición: el apoyo suspendido no lo cubre «colgarse» | [C] §D.3. No hay violación demostrada |
| H5 | Medio | `goalText` sin influencia garantizada. El respaldo lo ignora. No hay validación de adecuación ni advertencia contra la inyección. El producto pide un objetivo libre que solo la IA puede leer | [C] §C |
| H6 | Medio | Se pueden iniciar sesiones futuras. `IN_PROGRESS` se queda pegado tras abandonar y bloquea la reparación de seguridad | [C] §D.4 |
| H7 | Medio | 853 «Media sentadilla» y 1093: nombre e instrucciones de un ejercicio distinto al del GIF, en el bloque de cardio | [C] §D.3 |
| H8 | Medio | Un timeout del proveedor cuenta para un cortacircuitos global (3 fallos, 10 min) | [C] código, [I] efecto |
| H9 | Bajo | ~5,5 s fuera de `GENERACION_TIEMPOS` (preparación 0,57 s + persistencia 4,9 s), frente a 0,1 s en local | [C] log, [I] causa |
| H10 | Bajo | 910 con equipo `pull-up bar` cuando necesita una barra baja | [C] §D.3 |
| — | Informativo | `generation_attempts = 3` incluye el respaldo por diseño. El reintento se corta tras un timeout por diseño | [C] §A.3 |

**Crítico:** ninguno con evidencia.

### F.2 Recomendación

1. **Qué resolver primero, con coste cero y sin tocar el producto:**
   - confirmar H1 con las consultas de §G;
   - verificar la sesión iniciada (§G-3);
   - encargar la revisión profesional de H4 (1303 y 1152), que es seguridad.
2. **Para el MVP: D + la corrección de H2.**
   - **D** porque la app ya no espera más de 30 s (H3). Ninguna optimización del modelo baja la generación de 30 s de forma fiable. Opciones en orden de preferencia:
     - publicar el respaldo validado al instante;
     - o generación asíncrona;
     - y como mucho una sustitución por el plan de IA con las prohibiciones de §E.
   - **H2** porque cuesta poco y ataca la causa de este rechazo. Consiste en que `max_per_body_part` y `min_exercises_per_session` digan los mismos números que V15 verifica: la mitad de los ejercicios de trabajo, con como mucho `max_colocaciones_dia` colocaciones menos los complementos.
   - **Ojo:** cambia los datos que ve el modelo (contrato PR-1.7). Requiere tu autorización y una evaluación pareada antes de adoptarse.
3. **Para una etapa posterior:**
   - **B**: primero la versión determinista, que solo publica tras validar la semana completa; después la versión con IA sobre sesiones concretas;
   - **E**: evaluación pagada de razonamiento bajo y filtrado previo del catálogo;
   - **C**: generación por día, solo si B y D no bastan, porque es el rediseño más caro y arriesgado.
   - **El objetivo deportivo específico** (maratón) es una funcionalidad nueva: un objetivo soportado con su matriz de capacidades, progresión y validadores. Mientras no exista, conviene que la app **no prometa** personalización por texto libre y lo diga explícitamente.
4. **Pruebas necesarias antes de autorizar cambios:**
   - **H2:** regresión completa, más una prueba del contrato (para cada perfil de la rejilla 960, el `max_per_body_part` del prompt coincide con el umbral real de V15 para `max_colocaciones_dia`), más una evaluación pagada pareada pequeña (validez al primer intento, antes y después).
   - **D:** pruebas de integración de versionado:
     - sustitución permitida sin sesiones;
     - sustitución prohibida con una sesión iniciada, completada o con fecha alcanzada;
     - adherencia y progresión sobre la versión vigente;
     - concurrencia con el ciclo semanal y R-5.
   - **B:** experimentos E2/E3 generalizados a la cohorte O y a GRA, con ambas políticas. Validación completa obligatoria.
   - **H6:** prueba de que `start` rechaza una fecha futura y de que el abandono restituye `SCHEDULED`.
5. **Mejoras sin perder las propiedades verificadas:**
   - **H2:** el validador no cambia. Solo se informa mejor al modelo.
   - **Instrumentación:** medir preparación y persistencia en `GENERACION_TIEMPOS` (H9).
   - **H6:** guarda de fecha en `start` y restitución del estado al abandonar. No toca políticas deportivas.
   - **H4:** corrección de instrucciones con una migración auditada, sin tocar banderas.

   Nada de esto relaja V15, V24 ni V8C, ni cambia PLAN-REV-2.0 ni PR-1.7.

---

## G. Consultas de verificación (solo lectura, para ejecutar en tu base)

```sql
-- 1) Linaje de política del usuario 39 (H1)
SELECT plan_id, week_start_date, plan_version, status, generation_source, planning_policy, created_at
  FROM weekly_training_plans WHERE user_id = 39 ORDER BY plan_id;

-- 2) Contenido real del plan 78, para comparar con evidencia/plan78_respaldo_reproducido.csv
SELECT pw.scheduled_date, pw.focus_code, pw.status, pw.expected_duration_minutes, pe.exercise_order, pe.exercise_id,
       pe.planned_sets, pe.planned_reps, pe.planned_duration_seconds, pe.rest_seconds
  FROM planned_workouts pw JOIN planned_workout_exercises pe ON pe.planned_workout_id = pw.planned_workout_id
 WHERE pw.plan_id = 78 ORDER BY pw.scheduled_date, pe.exercise_order;

-- 3) ¿Quién puso IN_PROGRESS? (H6)
SELECT session_id, planned_workout_id, status, started_at, ended_at FROM workout_sessions WHERE plan_id = 78;

-- 4) Equipo real del perfil (cierra la inferencia de §A.3)
SELECT equipment_codes FROM user_profiles WHERE user_id = 39;
```

## H. Respuestas a los criterios de aceptación

1. **V15:** 4 de 6 ejercicios de trabajo en `upper legs` el 19 y el 22 (1004, 144, 122, 1112 y 1005, 1073, 1114, 1199), cuando el máximo es la mitad (3). El prompt había dicho 6. [C]
2. **¿El segundo intento recibió los errores?** Sí: el JSON exacto y los dos motivos, comprobado con aserciones. [C]
3. **¿Por qué hubo respaldo tras dos predicciones?** El intento 2 no terminó en 150 s de sondeo. Un timeout se trata como fallo del proveedor y corta los intentos por diseño. [C]
4. **`goalText`:** solo es contexto en el prompt. No influye de forma determinista y en el respaldo no influye nada. [C]
5. **¿Por qué una rutina general?** El plan lo hizo el motor de reglas, que decide solo por `goalType = GENERAL_FITNESS`. Ni el sistema ni los validadores tienen un modelo de entrenamiento de maratón. [C]
6. **Seguridad:** el plan cumple las restricciones tal como están definidas (32/32). Hay riesgo por instrucciones contradictorias (1303, 1152) y un hueco de definición en el apoyo suspendido, pendientes de revisión profesional. [C] [I]
7. **`IN_PROGRESS`:** no viene de la creación. Viene de un `start` sobre una sesión futura, que el backend permite por no tener límite de fecha inferior. [C] El hecho concreto está pendiente de §G-3.
8. **¿Se pueden corregir solo las sesiones rechazadas?** Sí, en este caso con HIST (E2, E3), siempre que se valide la semana completa. Con REV-2.0, no sin tocar también la sesión superior (V26). [C]
9. **Mejor relación tiempo, coste, personalización y seguridad:** para el MVP, D (respaldo inmediato y validado) + H2. Después, B determinista. C es la más cara y arriesgada. E necesita evaluación pagada. [I]
10. **¿Qué cambiar primero sin romper el backend evaluado?** Confirmar H1 y revisar H4 (coste cero). Después, H2 (coherencia de datos del prompt, con evaluación) y D (flujo de publicación), sin tocar validadores ni políticas. [I]

---

### Archivos de evidencia (`evidencia/`)

| Archivo | Contenido |
|---|---|
| `log_real_usuario39.txt` | Log real proporcionado (fuente primaria) |
| `intento1_jhxmsrkj61rmr0d1t5trg6znj4.json` | JSON exacto del intento 1, extraído del log |
| `V15_desglose_intento1.csv` | Grupo, complemento y recuento V15 por ejercicio |
| `plan78_respaldo_reproducido.csv` | Plan de respaldo reproducido, con banderas de seguridad, equipo, GIF e instrucciones |
| `reparacion_localizada_PLAN-HIST-1.csv`, `..._PLAN-REV-2.0.csv` | Experimentos E1–E5 |
| `RESUMEN_reproduccion_PLAN-HIST-1.txt`, `..._PLAN-REV-2.0.txt` | Huellas reproducidas por política |
| `prompt_intento1_reproducido.txt`, `prompt_intento2_reproducido.txt` | Prompts reconstruidos con el código real (HIST, equipo inferido) |
| `elegibles_todo_el_equipo.csv`, `buscar_equipo_huella.py`, `busqueda_equipo_resultado.txt` | Inferencia del equipo a partir de la huella de 350 elegibles |
