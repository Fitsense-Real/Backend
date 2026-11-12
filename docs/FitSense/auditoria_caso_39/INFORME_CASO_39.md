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

   Con PLAN-REV-2.0, el mismo JSON tendría además 5 rechazos V26 y el respaldo sería otro plan. ~~La causa probable es el linaje del usuario~~ **Corregido en el cierre (§I.1):** la herencia por linaje **no** se aplicó. El log muestra `semana=1` y no contiene la línea `POLITICA_LINAJE`, de modo que el usuario no tenía planes anteriores. La política activa de ese proceso era PLAN-HIST-1, fijada por una configuración externa (variable de entorno o `application-local.properties`).
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

---

# I. Cierre de la auditoría y diseño de correcciones (orden de cierre)

**Mismas reglas que el resto del informe:** sin código de producción, sin Replicate, sin merge, sin tocar datos. Nivel de evidencia: [C] comprobado, [I] inferencia, [H] hipótesis.

Nuevas evidencias en `evidencia/cierre/`, producidas por:
- `Caso39ReproduccionIT`, con `-Dfitsense.auditoria.caso39.equipo=<lista exacta>`;
- `Caso39ContratoV15IT` (opt-in, `-Dfitsense.auditoria.caso39.contrato=true`).

### I.0 Datos disponibles y no disponibles

| Dato | Estado |
|---|---|
| Equipamiento exacto (29 códigos) | **Disponible** (orden de cierre). Usado en todas las reproducciones de esta sección |
| JSON completo del plan 78 | **No disponible en esta sesión** [C]. Revisé todos los mensajes de la conversación y la carpeta de subidas (`/mnt/user-data/uploads`, vacía). Solo constan los datos parciales de la primera orden: fechas y enfoques, 42–43 min, 6+1+1 por sesión, presencia del 1303 y el 910, plan `ACTIVE`, primera sesión `IN_PROGRESS`. **No afirmo una comparación ejercicio por ejercicio.** Dejo un script autoprobado para hacerla (§I.2) |
| Base de datos del usuario 39 (linaje, sesiones) | **Sin acceso** desde este entorno. Las consultas exactas están en §G y §I.4 |

## I.1 H1 · Política aplicada

**Cómo se selecciona [C]** (`WeeklyTrainingPlanCommandServiceImpl.java:208-212`). Se toma, por este orden:
1. La política del plan activo de **la misma semana** (`previousActive`), para una versión nueva de esa semana.
2. Si no lo hay, la del **linaje**: el plan más reciente del usuario **anterior** a esa semana (`politicaDelLinaje`, `:599-609`).
3. Si no hay ninguno, la **configuración activa**: `PlanningPolicySelector.activa()`, con `fitsense.planning.policy=${FITSENSE_PLANNING_POLICY:PLAN-REV-2.0}`.

Cuando se aplica el paso 2 y difiere de la configuración, se escribe en INFO `POLITICA_LINAJE usuario {u} semana {s}: sigue con {X} (la configuracion activa es {Y})`.

**Por qué el plan 78 no heredó nada [C]:**
- **Ningún plan previo:** `resolveWeekNumber` (`:611-616`) da `semana=1` solo si el usuario no tiene ningún plan. Todas las líneas del log real dicen `semana=1` o `week=1` (`CAPACIDADES_4A`, `REPLICATE_TIMING`, `GENERACION_TIEMPOS`).
- **Sin línea de linaje:** entre `SlotCoverageGate` (15:31:35.720) y `GoalFeasibilityObserver` (15:31:36.290), que es justo donde se resuelve la política, no hay ninguna línea `POLITICA_LINAJE`.
- **Sin plan activo de la misma semana:** era una generación inicial, y `GET /plan/current` devolvía 404.

**Conclusión [C + I fuerte]:**
- La política salió del paso 3: **el proceso local tenía activa PLAN-HIST-1.**
- `application.properties` declara PLAN-REV-2.0 por omisión. El perfil `local-dev` no toca la política (y además apaga la IA, que en tu ejecución estaba encendida).
- Por tanto, la fijó una configuración externa: la variable `FITSENSE_PLANNING_POLICY=PLAN-HIST-1` (es literalmente la reversión documentada en `application.properties:88`), `application-local.properties` (ignorado por git) o la configuración de ejecución del IDE.
- **Pendiente de confirmar** con cualquiera de estas comprobaciones:
  - la línea de arranque `POLITICA_PLANIFICACION PLAN-HIST-1` (INFO), frente a `POLITICA_PLANIFICACION candidata ACTIVA: PLAN-REV-2.0` (WARN);
  - `echo $FITSENSE_PLANNING_POLICY`;
  - la consulta 1 de §G, que debe mostrar el plan 78 como único plan, con `planning_policy = PLAN-HIST-1`.

**¿La herencia es intencional? [C]** Sí:
- está documentada en el propio código («Cierre de PLAN-REV-2.0, fase 7… la configuración decide solo la de los usuarios sin planes»);
- la cubre `TransicionesPoliticaIT`: HIST→REV2, REV2→HIST, P17 con candidatas desactivadas, mantenimiento e interruptor de progresión.

En este caso no actuó.

**Consecuencias de cada alternativa (sin recomendación automática):**

| Alternativa | Ventajas | Riesgos | Adaptación y progresión | Historial y análisis |
|---|---|---|---|---|
| **1. Mantener el linaje** (comportamiento actual) | Nadie cambia de reglas a mitad de su programa. Las copias y adaptaciones se validan con las mismas reglas con que se generaron | Los usuarios antiguos nunca reciben las mejoras de REV-2.0. Las cohortes quedan mezcladas | Continuidad garantizada (probado en `TransicionesPoliticaIT`) | Cada plan guarda su `planning_policy`: el análisis por política es posible, pero hay que estratificar |
| **2. Política nueva solo para usuarios nuevos** | En el código actual **es el mismo mecanismo que la 1**: «usuario nuevo» significa «sin planes». La diferencia solo existiría si se definiera por fecha de alta, lo que requiere código nuevo | Un usuario reinscrito o con planes antiguos sigue en HIST | Igual que 1 | Igual que 1 |
| **3. Migrar usuarios antiguos a REV-2.0** | Una sola regla vigente y comparabilidad futura | El propio código advierte que, sin migración explícita, «un cambio de configuración revalidaba la copia con otras reglas y dejaba al usuario sin plan en silencio». HIST→REV2 falla por V4 al 100 % o V27. V26 tiene tolerancia para semanas derivadas (no obliga a recortar lo heredado, solo impide subir) | La progresión heredada se conserva (`HEREDADA`), pero las dosis y duraciones cambian de criterio (PISO_70 → DOSIS_SEMANAL): las semanas pueden acortarse o alargarse de golpe | Rompe la serie temporal de esos usuarios. Exige marcar la semana de transición y excluirla o estratificarla en el análisis del estudio |

**Recomendación limitada al diagnóstico:**
- No tocar la herencia: en este caso no fue la causa.
- **Sí** conviene hacer visible la política efectiva: añadir `politica=` a `GENERACION_TIEMPOS`, que es solo observabilidad. Así una prueba local no se confunde con la configuración evaluada.

## I.2 Plan 78 y equipamiento exacto

### Elegibilidad con los 29 códigos reales [C] (`cierre/RESUMEN_equipo_exacto_PLAN-HIST-1.txt`)

| Medida | Log real | Reproducción con el equipo exacto | ¿Coincide? |
|---|---|---|---|
| Elegibles | 350 | **350** | Sí |
| LOWER_KNEE / LOWER_HIP / UPPER_PUSH / UPPER_PULL / fuera | 27 / 18 / 62 / 33 / 210 | **27 / 18 / 62 / 33 / 210** | Sí |
| Ejercicios del intento 1 fuera del conjunto | 0 (el validador no marcó V1) | **0** | Sí |
| Ejercicios del plan reproducido fuera del conjunto | — | **0** | — |
| Motivos del intento 1 (PLAN-HIST-1) | 2 × V15 | **idénticos, texto exacto** | Sí |
| Longitud del prompt del intento 1 | 114.998 caracteres | **115.000** | Diferencia de 2. **[H]** Compatible con `health_notes = ""` en lugar de `null`, sin efecto semántico |
| Ejercicios enviados al prompt | — | 336 de 350. No se envían 14: 55, 59, 123, 126, 298, 312, 417, 465, 902, 903, 1158, 1194, 1257 y 1310 | — |

El plan de respaldo reproducido con el equipo exacto es **idéntico** al de la reproducción anterior. También lo son los experimentos de reparación localizada.

### Comparación con el plan 78 real

La comparación es parcial: faltan los 32 ejercicios con su dosis.

| Elemento | Original (datos que diste) | Reproducción | Coincide |
|---|---|---|---|
| Fechas y enfoques | Lun 16 superior, jue 19 inferior, vie 20 superior, dom 22 inferior | Idéntico | Sí |
| Duración | ~42–43 min | 43 / 42 / 43 / 43 | Sí |
| Estructura | 6 de fuerza/acondicionamiento + 1 cardio + 1 movilidad | Idéntica en las 4 sesiones | Sí |
| Ejercicios señalados | 1303 (parallel bars) y 910 (pull-up bar) | 1303 el 19 (orden 4) y 910 el 20 (orden 5) | Sí |
| Estado del plan | `ACTIVE` | `ACTIVE` | Sí |
| Estado de las sesiones | La primera `IN_PROGRESS` | Todas `SCHEDULED` al publicar | Esperado: el cambio es posterior a la creación (§I.4) |
| 32 ids, orden, series, repeticiones, segundos y descansos | **No disponible** | `plan78_respaldo_reproducido.csv` | **Pendiente** |

**Para cerrarla:** `python3 -I -B evidencia/comparar_plan78.py plan78_real.json` sobre la respuesta de `GET /api/v1/users/me/plan/current`, o con la consulta 2 de §G.
- **Autoprueba [C]:** devuelve «COINCIDE» con un JSON construido desde la reproducción, y detecta un cambio de 910 → 912.

## I.3 Seguridad

**Contexto clave [C]: la app solo muestra el nombre, el equipo y la dosis.**
- `workout_detail_page.dart:100-105` y `session_page.dart` pintan `exerciseName` y `equipment`.
- `gifPath` e `instructions` llegan en el JSON pero **no se muestran**.

Por tanto, lo que el usuario cree que debe hacer sale del nombre y del equipo. La IA también ve el nombre, no las instrucciones.

### Los cinco ejercicios priorizados (`cierre/seguridad_5_ejercicios.csv`)

El «movimiento del GIF» procede de la revisión visual documentada en `auditoria_catalogo/revision_visual`. **No la he vuelto a verificar en esta tarea:** los GIF no están accesibles desde este entorno.

| Id | Nombre (es / en) | Equipo y requisitos | Grupo / patrón / dificultad | Banderas | Instrucciones | Movimiento según la revisión del GIF | Contradicción verificada [C] | Posible efecto en un principiante sin colgarse ni sobre la cabeza [I] |
|---|---|---|---|---|---|---|---|---|
| 1303 | Elevación de piernas torsionada / twisted leg raise | parallel bars (requisito: captains chair **o** parallel bars, V38) | waist / TRUNK_ROTATION_LATERAL_FLEXION / 1 | Colgarse no, sobre la cabeza no, suelo no | Tumbado boca arriba en el suelo | «Apoyo en la estación de fondos; de pie en la estación» | Las instrucciones (suelo) contradicen el equipo y el GIF (estación de fondos) | La app muestra «parallel bars»: hará el ejercicio sostenido con los brazos y los pies en el aire. Es apoyo suspendido, con carga alta para hombros y agarre en 100 kg. Dificultad 1 discutible |
| 1152 | Fondos invertidos / reverse dip | body weight (sin requisitos desde V38) | upper arms / HORIZONTAL_PUSH / 1 | Suelo sí | «Colócate entre dos barras paralelas con los brazos extendidos» (fondos en paralelas) | «Tumbado y sentado en el suelo; no usa paralelas» | Las instrucciones describen otro ejercicio | Por el nombre, el usuario puede hacer fondos en paralelas o en banco: extensión profunda de hombro con todo el peso corporal |
| 853 | Media sentadilla / half knee bends (male) | body weight | cardio / LOCOMOTION_CARDIO / 1 | — | Media sentadilla (bajar como para sentarse) | «Marcha con rodillas altas» (V38 lo pasó a cardio). La misma nota dice «cuenta como LOWER_KNEE», una anotación interna incoherente | Nombre e instrucciones no corresponden al GIF ni al patrón | El usuario hace 3 × 90 s de **sentadillas** como «cardio». No es peligroso en sí, pero cambia el estímulo y suma volumen de rodilla no contabilizado |
| 1093 | Empujar para correr / push to run | body weight | cardio / LOCOMOTION_CARDIO / 1 | Suelo no (V38) | Flexión + rodillas al pecho (desde el suelo) | «De pie elevando rodillas» | Instrucciones y nombre distintos del GIF | Puede hacer flexiones + escaladores en el suelo (permitido para este perfil). Estímulo distinto |
| 910 | Remo invertido / inverted row | pull-up bar (requisito: pull-up bar **o** smith **o** suspension straps) | back / HORIZONTAL_PULL / 1 | Colgarse no, suelo sí (V37) | Barra a la cintura o TRX, talones en el suelo | «Tumbado bajo la barra con talones en el suelo» | Ninguna en seguridad. Equipo práctico ambiguo | Con una barra de dominadas alta no se puede hacer bien. Riesgo bajo |

### Hallazgo sistemático nuevo (H4-b) [C]

En V37/V38 se **relajaron** banderas de seguridad por lo que se veía en el GIF («restricción de más»), **sin actualizar nombre ni instrucciones**. Esos textos siguen describiendo el movimiento restringido. Hay 13 relajaciones de `true` → `false` (`cierre/relajaciones_banderas_V37_V38.csv`). En 6 de ellas, el texto describe el movimiento restringido:

| Id | Nombre visible (es) | Bandera relajada | Texto | ¿Elegible para el 39? | ¿Llega a la IA? |
|---|---|---|---|---|---|
| **316** | **Dominadas con agarre estrecho** | colgarse y sobre la cabeza (V38) | «Cuélgate de una barra de dominadas…» | **Sí** | **Sí, va en el prompt** |
| 1301 | Crunch invertido suspendido | colgarse (V38) | «Cuélgate de una barra de dominadas…» | Sí | Sí: **la IA lo usó en el intento 1** (día 22) |
| 1309 | Estiramiento de espalda alta | sobre la cabeza (V38) | «Eleva los brazos por encima de la cabeza» | Sí | Sí: está en la lista de MOBILITY del prompt |
| 1322 | Estiramiento de tríceps | sobre la cabeza (V38) | «Extiende un brazo por encima de la cabeza» | Sí | Sí: está en la lista de MOBILITY |
| 131 | Curl de bíceps con banda sobre la cabeza a una mano | sobre la cabeza (V38) | El nombre dice «sobre la cabeza» | No (dificultad 2) | — |
| 1298 | Maltés en apertura | colgarse (V38) | «colgándote de un par de anillas» | No (dificultad 3) | — |

**Fuera de las relajaciones, con la misma contradicción:**
- **1306** «Elevación de piernas vertical en paralelas»: instrucciones «Cuélgate de las barras paralelas»; GIF «silla romana: apoyo en antebrazos». Elegible.
- **448** «Curl por encima de la cabeza sentado en cable»: el nombre dice *por encima*, el GIF muestra brazos a la altura del hombro y la bandera es «no». Elegible.

Un barrido amplio por texto (`cierre/catalogo_contradicciones_texto_bandera.csv`, 69 filas) encuentra más candidatos. **Incluye falsos positivos** (correas TRX «suspended», máquinas con «pull-up» en el nombre) y es material de triaje, no hallazgos confirmados.

**Clasificación:**
- **316: CRÍTICO (comunicación de seguridad).** El sistema puede proponer a quien prohibió colgarse un ejercicio que la app nombra «Dominadas» y cuyo texto dice «Cuélgate». La bandera sigue la lectura del GIF, pero el usuario no ve el GIF.
  - No hay evidencia de que se haya prescrito: el plan 78 no lo incluye.
  - La vía existe para todo usuario con barra de dominadas que haya restringido colgarse.
- **1301, 1306, 1309, 1322, 448, 1303, 1152: ALTO**, pendientes de revisión profesional con el GIF delante.
- **853 y 1093: MEDIO** (estímulo y nombre).

**Corrección específica propuesta (no aplicada):**
1. **Contención inmediata**, con el mismo patrón que `INC-CAT-1374` y `INC-4C-03` en `ExternalCatalogService` («sin interruptor: es seguridad»): retener 316, 1301 y 1306 para perfiles con `allowHanging=false`, y 1309, 1322 y 448 para `allowOverhead=false`, hasta la revisión.
   - No cambia banderas ni políticas: solo resta elegibles.
   - Efecto sobre el usuario 39: −6 elegibles (350 → 344). El 316 cuenta en UPPER_PULL (33 → 32).
2. **Revisión profesional** de los 13 relajados y de los candidatos del barrido, con GIF, nombre e instrucciones a la vista.
3. **Migración de datos auditada** (nombres e instrucciones en español) para cada ejercicio cuyo GIF se confirme. Y lo contrario: devolver la bandera a `true` donde el texto sea el correcto.

### Apoyo suspendido: el vacío conceptual

**Definiciones vigentes [C]** (V25):
- **Colgarse:** «el cuerpo cuelga del agarre, pies sin apoyo, incluye asistidas».
- **Sobre la cabeza:** «brazos por encima de la cabeza en alguna fase».

| Situación | ¿La cubre alguna bandera? | Ejemplos en el catálogo |
|---|---|---|
| Colgarse de una barra o anillas (tracción, pies sin apoyo) | Sí (`requires_hanging`) | 865–871 «hanging leg raise…» |
| Sostener el peso sobre paralelas o silla romana (compresión, pies sin apoyo) | **No** | 1303, 1304, 1306, 1184 (paralelas), 462 (captains chair), 777 «elbow dips» |
| Apoyo parcial con los pies (remo invertido, TRX) | No, y es razonable | 910, 316 según el GIF, 914, 1307, 1297, 1300, 1312 |
| Carga alta de hombro sin elevar los brazos (fondos) | No | 777, 1152 si se sigue el texto, 458 (dificultad 3) |

**Alternativas, para revisión profesional (no se crea ninguna restricción):**

| Alternativa | Qué cambia | Efecto en la elegibilidad del usuario 39 | Riesgo |
|---|---|---|---|
| A. Mantener la definición y aclarar en la app que «colgarse» significa colgar de una barra | Solo texto | 0 | El vacío sigue: quien no puede cargar el hombro con todo su peso no tiene cómo declararlo |
| B. Ampliar `requires_hanging` a «sostener el peso corporal con los brazos, con los pies sin apoyo» | Reclasificar 6 elegibles (777, 462, 1184, 1303, 1304, 1306) | 350 → 344 (UPPER_PUSH 62 → 61) | Cambia el significado de una restricción existente: perfiles ya generados verían ejercicios retirados (V8C y P-02 deben respetarlo) |
| C. Atributo nuevo `requires_arm_support` («apoyo suspendido»), aplicado a quien prohíba colgarse o a una restricción nueva | Esquema y migración | −6 si se vincula a `allowHanging=false` | Es una restricción nueva: fuera del alcance de esta tarea |
| D. Atributo de carga de hombro, usado por las políticas de dolor y limitación (V8C) y no por el perfil | Esquema | 0 por omisión | Más fino, pero exige criterios profesionales de clasificación |

## I.4 H6 · Sesiones futuras en `IN_PROGRESS`

**Comprobado [C]:**

| Pregunta | Respuesta | Evidencia |
|---|---|---|
| 1. ¿Cuándo se inició la primera sesión del plan 78? | **No determinable sin la base de datos.** Ver la consulta de abajo | — |
| 2. ¿Fue por el endpoint de inicio? | **[I] Sí:** es la única ruta que escribe `IN_PROGRESS` en un entrenamiento | `PlannedWorkout.markInProgress()` (`:139`) ← `WorkoutSessionCommandServiceImpl.handle(StartWorkoutSessionCommand)` (`:47-70`) |
| 3. ¿Qué evento la dejó en `IN_PROGRESS`? | El inicio. Si después se abandonó, o la cerró `StaleSessionScheduler` (sesiones de más de 6 h, cada hora en el minuto 35), **el entrenamiento sigue `IN_PROGRESS`** | `handle(AbandonWorkoutSessionCommand)` (`:107-115`, «abandonar no cierra el entrenamiento»); `StaleSessionScheduler.java:36-45` solo abandona la sesión |
| 4. ¿Se puede iniciar cualquier sesión futura? | **Sí.** Solo se comprueba abierta, no vencida, no bloqueada por seguridad y sin otra sesión abierta. **No hay límite inferior de fecha** | `WorkoutSessionCommandServiceImpl.java:51-63` |
| 5. ¿Fuera de orden? | **Sí:** no hay comprobación de orden | Idem |
| 6. ¿Qué pasa al abandonar? | La sesión pasa a `ABANDONED`; el entrenamiento sigue `IN_PROGRESS` y se puede volver a iniciar (intento + 1) hasta que venza | `:107-115`; `isOpen` incluye `IN_PROGRESS` (`PlannedWorkout.java:237`) |
| 7. ¿Puede quedar `IN_PROGRESS` sin ejecución activa? | **Sí**, tras un abandono manual o automático | Idem |
| 8. Adherencia | `IN_PROGRESS` sin resultado cuenta como no realizado, igual que `SCHEDULED`. Si se completa, cuenta para el día programado aunque se haga antes | Subagente: `WeekPlanInput.java:38-40`, `WorkoutSession.java:202` |
| 9. Caducidad | La tarea de las 00:30 pasa `SCHEDULED` e `IN_PROGRESS` vencidos a `SKIPPED` | `PlannedWorkoutRepository.java:24-32` |
| 10. Reparación de seguridad y adaptación | La reparación de seguridad se **bloquea** si hay algún entrenamiento `IN_PROGRESS`, aunque no haya sesión abierta. La adaptación lo trata igual que «sin registro» | `PublishedPlanSafetyRepairService.java:225-227`; `PreviousWeekAssembler.java:196-198` (subagente) |

**Consulta para cerrar 1–3 (solo lectura):**

```sql
SELECT s.session_id, s.planned_workout_id, pw.scheduled_date, s.status, s.attempt_number, s.started_at, s.ended_at
  FROM workout_sessions s JOIN planned_workouts pw ON pw.planned_workout_id = s.planned_workout_id
 WHERE pw.plan_id = 78 ORDER BY s.started_at;
```

**Corrección mínima propuesta (no implementada):**
1. **Guarda de fecha en `start`:** rechazar si `hoy(zona del usuario) < scheduledDate`.
   - «Hoy» se calcula con la **misma** zona con que se fija `expires_at` (`externalIamService.timezoneOf`, por omisión `America/Lima`).
   - Así la ventana permitida es exactamente [00:00, 23:59] local del día programado.
   - Error nuevo en la familia `WorkoutNotAvailableException` («todavía no es el día»), 409 con un código estable.
2. **Restituir el estado al abandonar:** si la sesión abandonada era la única abierta y el entrenamiento no tiene resultado, volver a `SCHEDULED`. Igual en `StaleSessionScheduler`.
3. **Reparación de seguridad:** comprobar si hay una **sesión abierta** (`IN_PROGRESS` en `workout_sessions`) en vez del estado del entrenamiento. Con el punto 2 queda redundante, pero es más robusto.
4. **App (fuera de esta tarea, sin tocar Flutter):** mostrar «Comenzar» solo si es hoy, para alinearla con el backend.

**Efectos de la corrección sobre los casos legítimos:**
- **Inicio en la fecha programada:** permitido todo el día local.
- **Reanudar una sesión ya abierta:** no pasa por `start` (sigue igual).
- **Reiniciar tras abandonar el mismo día:** permitido.
- **Entrenamientos reprogramados:** no existe un endpoint de reprogramación [C]. Las adaptaciones crean versiones con fechas propias y la guarda usa la fecha de cada entrenamiento.
- **Sesiones parciales:** `PARTIAL` está cerrado y no se reinicia (sin cambios).
- **Sesiones vencidas:** siguen rechazadas por `isExpired`. El registro retroactivo (`report`) no pasa por `start` y no cambia.
- **Datos ya existentes:** la guarda no altera datos. Una migración opcional podría devolver a `SCHEDULED` los entrenamientos futuros en `IN_PROGRESS` sin sesión abierta, previa consulta y con autorización.

**Pruebas necesarias:**
1. `start` el día anterior a las 23:59 (Lima) → rechazado.
2. `start` el día a las 00:00 → permitido.
3. `start` el día a las 23:58 → permitido.
4. `start` al día siguiente → rechazado por vencido.
5. Usuario con otra zona horaria: los límites siguen su zona.
6. Inicio fuera de orden el mismo día con dos sesiones programadas ese día: permitido.
7. Abandonar la única sesión → el entrenamiento vuelve a `SCHEDULED`; volver a iniciar → intento 2.
8. Abandono por `StaleSessionScheduler` → `SCHEDULED`.
9. Abandonar con otra sesión del mismo entrenamiento completada → no se restituye.
10. Reparación de seguridad con un entrenamiento restituido → no bloqueada.
11. La adherencia de la semana no cambia con la corrección (regresión de `AdherenciaIT` y `EjecucionEntrenamientosIT`; esta última fija hoy el `IN_PROGRESS` tras abandonar, en la línea 394, y **habrá que actualizarla de forma consciente**).
12. Caducidad de las 00:30 sin cambios.
13. `report` retroactivo sin cambios.

## I.5 H2 · Contrato prompt–V15

### Causa raíz [C]

- **El tope que se comunica:** `PlanInputSnapshot.suggestedSession` (`:677-688`) envía `max_per_body_part = min_exercises_per_session / 2`, solo si el enfoque tiene 3 o más grupos y el mínimo es 4 o más.
- **De dónde sale el mínimo:** `min_exercises_per_session` sale de `minExercisesOrNull` (`:726-744`).
  - Usa el suelo de minutos de V20 (solo en PLAN-HIST-1, PISO_70) y la **dosis objetivo del `TargetPrescription`** (series y repeticiones del objetivo).
  - **No** usa las dosis por capacidad que V24 exige a GENERAL_FITNESS, LOSE_WEIGHT e INCREASE_STRENGTH.
  - No está acotado por el máximo de colocaciones.
  - GAIN_MUSCLE sí usa la dosis 4D (GEN-IN-1.10b), y por eso no tiene contradicciones.
- **El máximo de colocaciones:** `max_colocaciones_dia` sale de `CapabilityPrescriptionPolicy.MAX_COLOC_DIA = 8` (`:60`) para los objetivos por capacidades, o de `MvpDosePolicy.maxColocaciones(minutos)` para GAIN_MUSCLE.
- **Lo que verifica V15** (`PlanDraftValidator.java:856-868`):
  - excluye los complementos de capacidad (`CatalogEligibilityPolicy.complementos`, `esComplemento` `:127`): cardio y movilidad que entran por 4A v6;
  - rechaza si un grupo tiene más de la mitad de los ejercicios de trabajo (`cuantos > deTrabajo.size()/2`), cuando el enfoque tiene 3 o más grupos y hay 4 o más ejercicios de trabajo.
- **Con PLAN-REV-2.0** no hay suelo V20 → `min_exercises_per_session = null` → `max_per_body_part = null`. La regla se aplica, pero **no se comunica**.

### La sonda del validador real [C] (`cierre/sonda_v15_*.csv`)

- **Cómo se hizo:** sobre el contexto real del usuario 39, se sustituyó una sesión por composiciones con n = 2…6 ejercicios de trabajo, k = 1…n del mismo grupo y 2 complementos (cardio y movilidad), y se validó la semana completa.
- **Resultado:** 80 combinaciones por política y 32 rechazos por la regla de la mitad.
- **Concordancia:** `topeV15(foco, n) = (grupos del foco ≥ 3 y n ≥ 4) ? floor(n/2) : sin regla` predice **exactamente** el resultado de V15 en las 80, en UPPER_BODY y LOWER_BODY, con las dos políticas. La prueba falla si no coincide.

### Contradicciones en la rejilla [C] (`cierre/contrato_v15_rejilla_*.csv`)

La rejilla tiene 384 perfiles: 4 objetivos × 3 niveles × gimnasio/casa × 2–5 días × 30/45/60/75 min. Son 1.312 sesiones, 1.184 de ellas con la regla de la mitad.

| Contradicción | PLAN-HIST-1 | PLAN-REV-2.0 | Diseño propuesto |
|---|---|---|---|
| C1 · `min_exercises_per_session` > `max_colocaciones_dia` | 320 sesiones | 0 | **0** |
| C2 · el tope comunicado admite una concentración que V15 rechaza (con el máximo de ejercicios de trabajo que permite el prompt) | 400 | 0 | **0** |
| C3 · V15 aplica la mitad y el prompt no da tope | 48 | **1.184** (todas) | **0** |

- **Por objetivo (HIST):** GENERAL_FITNESS y LOSE_WEIGHT tienen 134 C1 y 174 C2 cada uno; INCREASE_STRENGTH, 52 C1, 52 C2 y 48 C3; GAIN_MUSCLE, 0.
- **Ejemplo genérico:** GENERAL_FITNESS principiante, gimnasio, 3 días, 45 min, LOWER_BODY. Se comunica `min=9, max_coloc=8, max_per_body_part=4`; con 6 de trabajo, V15 admite 3.

### Frecuencia histórica [C]

Las cifras proceden de las campañas reales archivadas, con prompts anteriores a PR-1.7 en parte.
- **Llamadas de generación:** 112, con 47 reintentos con motivos. **5 incluían la V15 de la mitad y 2 fueron solo por ella.**
- **Auditoría de 48 predicciones:** 15 rechazadas; 5 con V15 de la mitad, **3 solo por ella**.
- **Lectura:** es una causa minoritaria pero recurrente. Este caso es uno de ellos.

### Diseño de la corrección (sin tocar V15)

Hay dos variantes. Las dos usan **una sola fuente de verdad**:
- una función pura `RepartoV15.topeMismoGrupo(WorkoutFocus, int nTrabajo)`, idéntica a la de la sonda;
- `CatalogEligibilityPolicy.complementos` (ya pública) para decidir qué cuenta como trabajo.

Una prueba de contrato fija que V15 y la función coinciden (la sonda, convertida en prueba permanente). Más adelante, en un cambio aparte y con la regresión completa, V15 podría llamar a esa función sin cambiar su comportamiento.

| | **H2-A · solo datos** (sin tocar el texto de PR-1.7) | **H2-B · regla y tabla** (cambia el texto: requiere una versión nueva de prompt) |
|---|---|---|
| `min_exercises_per_session` | Calculado con las mismas dosis que exige V24 y **acotado** a `max_colocaciones_dia`, o `null` cuando gobiernan las capacidades | Igual |
| `max_per_body_part` | `topeMismoGrupo(foco, max_colocaciones_dia − complementos obligatorios)`. Usuario 39: 8 − 2 = 6 → **3** | Se sustituye por `max_per_body_part_por_n_trabajo` (usuario 39: `{4:2, 5:2, 6:3, 7:3, 8:4}`) y `complementos_no_cuentan` (ids de `CatalogEligibilityPolicy.complementos`) |
| Texto | Sin cambios | La regla dicha tal como la aplica V15: «sin contar los complementos de cardio y movilidad, ningún body_part puede pasar de la mitad de los ejercicios de trabajo de la sesión; usa la tabla» |
| Contradicciones residuales | **Sí:** si el modelo usa menos ejercicios de trabajo de los previstos (por ejemplo 5 con 3 iguales, cuando V15 admite 2). Es menos frecuente, pero existe. Con REV-2.0 pasaría de `null` a un número: es un cambio de comportamiento evaluado | **Ninguna** por construcción (0 en la rejilla) |
| Coste de adopción | Bajo, pero cambia los datos de PR-1.7 en PLAN-HIST-1 y PLAN-REV-2.0 | Versión nueva de prompt y evaluación pareada |

**Antes y después, usuario 39 (PLAN-HIST-1, LOWER_BODY):**

```json
// antes
{"min_exercises_per_session":12, "max_colocaciones_dia":8,
 "suggested_split":[{"focus_code":"LOWER_BODY","min_body_parts":2,"max_per_body_part":6}]}
// después (H2-B)
{"min_exercises_per_session":null, "max_colocaciones_dia":8,
 "suggested_split":[{"focus_code":"LOWER_BODY","min_body_parts":2,
   "max_per_body_part_por_n_trabajo":{"4":2,"5":2,"6":3,"7":3,"8":4}}],
 "complementos_no_cuentan":[66,67,253,453,454,455,773,853,881,917,978,1053,1054,1075,1093,1117,1136,1137,
   1149,1150,1151,1173,1178,1193,1255,1258,1260,1309,1322,1325,1373,1375]}
```

Con PLAN-REV-2.0 el «antes» es `min=null, max_per_body_part=null`, y el «después» es el mismo.

**Invariantes que se conservan:**
- V15, V23, V24, V25, V26, V27 y V8C no cambian (validador intacto).
- Las dosis de V24 no cambian (`constraints.capacidades` intacto).
- PLAN-HIST-1 y PLAN-REV-2.0 no cambian.
- El motor de reglas no cambia (no lee la instantánea).
- Solo cambian campos de la instantánea de la IA y, en H2-B, el texto del prompt.

**Pruebas de contrato (sin Replicate):**
1. Rejilla completa con ambas políticas: `min ≤ max_coloc` siempre que ambos existan (C1 = 0).
2. Para cada sesión y cada n de la tabla, tope comunicado = `topeMismoGrupo` (C2 = 0), y la regla siempre comunicada cuando aplica (C3 = 0).
3. Sonda permanente: la función predice a V15 en las combinaciones de UPPER, LOWER y FULL_BODY. Si alguien cambia V15, la prueba avisa.
4. `complementos_no_cuentan` = `CatalogEligibilityPolicy.complementos(available, objetivo)`.
5. La instantánea no cambia fuera de los campos de H2 (comparación JSON campo a campo con la base).
6. Regresión completa: 960 perfiles, REPLAY y pruebas de V24, V8C, progresión y transiciones.

### Evaluación

**Sin coste:** la comparación anterior (rejilla y sonda) ya muestra que el contrato actual es contradictorio y el propuesto no.

**Pagada, propuesta y no ejecutada (requiere autorización):**
- **Muestra:** 24 perfiles de la rejilla donde aplica la regla. Mitad GENERAL_FITNESS y LOSE_WEIGHT y mitad INCREASE_STRENGTH, con UPPER, LOWER y FULL, principiante e intermedio, en PLAN-REV-2.0 (la evaluada). Más 6 perfiles PLAN-HIST-1 como control de linaje, incluido el 39.
- **Diseño:** pareado (control con PR-1.7 frente a H2-B), un intento por brazo, orden alterno.
- **Coste:**
  - 30 × 2 = 60 llamadas × USD 0,018–0,023 (referencia GRA) ≈ **USD 1,1–1,4**;
  - midiendo el desenlace final, con hasta 3 intentos: ≤ USD 4,2;
  - **tope propuesto: USD 5**.
- **Criterios de aceptación:**
  - rechazos V15 por la mitad en el tratamiento = 0;
  - ninguna categoría de rechazo (V20, V23, V24, V26, V27) sube en más de 1 caso frente al control;
  - validez al primer intento no inferior al control;
  - mediana de latencia no peor en más del 10 %;
  - tamaño del prompt: + ~0,5 % (lista de complementos y tablas, unos 600 caracteres sobre 115.000), calculado sin coste.

## I.6 Priorización

Criterios en orden: seguridad, integridad de datos y estados, corrección funcional, conservación de políticas, experiencia de usuario, rendimiento, coste.

| Orden | Cambio | Problema | Archivos | Riesgo de regresión | Pruebas | ¿Replicate? | ¿Altera lo evaluado? | ¿Aislable? | Condiciones para el merge |
|---|---|---|---|---|---|---|---|---|---|
| 1 | **H4-b contención** (316, 1301, 1306 con colgarse prohibido; 1309, 1322, 448 con sobre la cabeza prohibido) | Ejercicios con texto restringido llegan a quien los prohibió | `ExternalCatalogService` (patrón INC-CAT-1374) + prueba | Bajo: solo resta elegibles | Elegibilidad por perfil; factibilidad de la rejilla y de los 4 objetivos (que ninguno quede NO_FACTIBLE nuevo); 960/REPLAY | No | Sí, mínimamente: menos elegibles con esas restricciones | Sí | Regresión verde; lista revisada por ti; 0 perfiles NO_FACTIBLE nuevos |
| 2 | **H4 revisión profesional + migración de textos** (1303, 1152, 853, 1093, 13 relajados, triaje del barrido) | Nombres e instrucciones que no coinciden con el GIF ni con las banderas | Migración de datos de catálogo (como V36/V37), sin tocar banderas salvo decisión profesional | Bajo en código; medio en datos | Pruebas de catálogo existentes; IT de nombres e instrucciones; regresión | No | No (solo textos) | Sí | Revisión profesional firmada; migración idempotente; prueba de no regresión del catálogo |
| 3 | **H6 guardas de estado** (fecha en `start`, restituir al abandonar, reparación por sesión abierta) | Inicio anticipado, estado pegado, reparación bloqueada | `WorkoutSessionCommandServiceImpl`, `PlannedWorkout`, `StaleSessionScheduler`, `PublishedPlanSafetyRepairService` + pruebas | Medio: toca ejecución y adherencia | Los 13 casos de §I.4; `EjecucionEntrenamientosIT`, `AdherenciaIT` y caducidad | No | Sí, en ejecución, no en planificación | Sí | Todos los casos pasan; ajuste consciente de la aserción del abandono; app sin cambios obligatorios |
| 4 | **H2** (H2-B preferente; H2-A si el texto de PR-1.7 debe quedar congelado) | Contrato prompt–V15 contradictorio: rechazos evitables y reintentos de ~3,5 min | `PlanInputSnapshot`, `PlanPromptBuilder` (solo H2-B), una función compartida + pruebas de contrato | Medio: cambia lo que ve la IA | Contrato (§I.5) + regresión completa | **Sí** (≤ USD 5) | **Sí** (contrato de la IA) | Sí | C1 = C2 = C3 = 0; evaluación pareada aceptada; regresión verde |
| 5 | **H1 observabilidad** (`politica=` en `GENERACION_TIEMPOS`). Decisión sobre migrar usuarios antiguos: aparte | Pruebas locales con una política distinta de la evaluada sin que se vea | `PlanGenerationPipeline.Tiempos` | Muy bajo | Unitaria de la línea de log | No | No | Sí | Verde |
| 6 | **Respaldo inmediato y sustitución posterior** (D) o generación asíncrona | La espera de minutos frente al corte de 30 s de la app (H3) | Pipeline, servicio de generación inicial, versionado, contrato HTTP y app | Alto | Versionado, prohibiciones de sustitución, adherencia, R-5, concurrencia, app | Recomendable | Sí (flujo de publicación) | Parcial | Diseño aprobado; pruebas de versionado; app adaptada |
| 7 | **H3 timeout de Flutter** | La app corta a los 30 s | App (`dio_client.dart`); depende de 6 | Bajo en sí, pero subirlo solo traslada la espera | Prueba de la app | No | No | Sí, aunque solo tiene sentido con 6 | Decidir junto con 6 |
| 8 | **Reparación localizada** (B, primero determinista) | Reintentos completos por fallos en 1–2 sesiones | Pipeline + reensamblado + validación completa | Medio-alto | E1–E5 generalizados, ambas políticas, cohortes O y GRA | Para la versión con IA | Sí | Sí | Después de H2: con el contrato corregido, el beneficio debe medirse de nuevo |

**Tabla resumen solicitada:**

| Hallazgo | Severidad | Confirmado | Solución propuesta | Riesgo | Prioridad |
|---|---|---|---|---|---|
| H4-b (316 y otros textos de colgarse o sobre la cabeza con bandera relajada) | **Crítico** (316) / Alto (resto) | Sí, el texto y las banderas. Pendiente: la revisión profesional del GIF | Contención inmediata + revisión + migración de textos | Bajo | **1** |
| H4 (1303, 1152, 853, 1093, 910; apoyo suspendido) | Alto | Parcial (contradicciones de texto [C]; movimiento real según la revisión documentada) | Revisión profesional, migración de textos y decisión A/B/C/D sobre el apoyo | Bajo / medio | 2 |
| H6 | Medio | Sí (mecanismo). Pendiente: el hecho concreto del plan 78 (SQL) | Guarda de fecha + restitución al abandonar + reparación por sesión abierta | Medio | 3 |
| H2 | Alto | Sí (causa, sonda y rejilla) | H2-B (o H2-A) con función compartida y pruebas de contrato | Medio | 4 |
| H1 | Alto (impacto en la validez de las pruebas locales) | Sí: no hubo linaje. La política fue la configuración externa PLAN-HIST-1. Pendiente: la fuente exacta (variable o archivo local) | Observabilidad; decisión sobre migración aparte | Muy bajo | 5 |
| H3 | Alto | Sí | Junto con respaldo inmediato o asíncrono | Alto (por el flujo) | 6–7 |

## I.7 Respuestas finales (comprobado o pendiente)

1. **¿Plan 78 con PLAN-HIST-1?** Prácticamente confirmado [C+I]: dos huellas exactas, la elegibilidad exacta y las colocaciones del respaldo. La confirmación definitiva es la columna `planning_policy` del plan 78 (SQL §G-1), **pendiente**.
2. **¿Por qué esa política?** **No fue el linaje** [C]: `semana=1`, sin `POLITICA_LINAJE`. Fue la política activa del proceso, PLAN-HIST-1, fijada por una configuración externa [I]. **Pendiente:** qué fuente exacta (variable `FITSENSE_PLANNING_POLICY`, `application-local.properties` o IDE), que se ve en la línea de arranque `POLITICA_PLANIFICACION`.
3. **¿El plan 78 coincide ejercicio por ejercicio?** **Pendiente:** el JSON no está en esta sesión. Coincide en todo lo disponible: fechas, enfoques, duraciones, estructura, 1303, 910 y huellas. Hay script para cerrarlo.
4. **¿La elegibilidad coincide con los 350?** **Sí** [C], exacta, también por slot, con el equipo real.
5. **¿Hay instrucciones incompatibles con la ejecución real?** **Sí** [C], frente a la revisión documentada de los GIF:
   - 1303 y 1152 (instrucciones de otro ejercicio);
   - 853 y 1093 (nombre e instrucciones distintos del GIF);
   - y de forma sistemática en los relajados de V38: 316, 1301, 1309, 1322 (más 1306 y 448).
6. **¿Hay que redefinir alguna restricción?** Hay un vacío [C]: el apoyo suspendido no lo cubre ninguna bandera. Las alternativas A–D quedan para revisión profesional. No se ha creado ninguna restricción.
7. **¿Por qué `IN_PROGRESS`?** Porque se pulsó «Comenzar» antes de la fecha y el backend lo permite sin límite inferior [C del mecanismo]. Si luego se abandonó, se quedó pegado [C]. **Pendiente:** el cuándo y cómo concreto, con el SQL de sesiones.
8. **¿Corrección mínima?** Guarda de fecha en la zona del usuario + volver a `SCHEDULED` al abandonar + reparación de seguridad por sesión abierta, con las 13 pruebas de §I.4.
9. **¿H2 sin tocar V15?** Comunicando a la IA el umbral que V15 aplica, con la misma función y la misma definición de complementos. H2-B: tabla por número de ejercicios de trabajo y lista de complementos; H2-A: solo datos. Además, un `min_exercises_per_session` nunca mayor que `max_colocaciones_dia`. La sonda permanente impide que se separen.
10. **¿Qué pruebas demuestran que H2 no crea contradicciones?** Las de §I.5:
    - rejilla con C1 = C2 = C3 = 0 en ambas políticas (hoy: HIST 320/400/48; REV 0/0/1.184);
    - sonda permanente contra el validador real;
    - igualdad de la lista de complementos;
    - instantánea sin cambios fuera de H2;
    - regresión completa.
11. **¿Qué implementar primero?** La **contención de seguridad H4-b (316 y compañía)**. Después la revisión y migración de textos, luego las guardas de H6, luego H2 con su evaluación. Respaldo inmediato y reparación localizada, más adelante.
12. **¿Qué necesita autorización de gasto en Replicate?**
    - **H2** (evaluación pareada, tope de USD 5);
    - recomendable para el respaldo inmediato y la reparación localizada con IA;
    - cualquier cambio de razonamiento o de modelo.

    No lo necesitan: H4, H6, H1 (observabilidad) ni la parte determinista de la reparación localizada.

**Cierre:** la auditoría del caso 39 queda cerrada en lo comprobable desde este entorno. Lo pendiente depende de datos que no tengo: el JSON del plan 78, la base de datos y la línea de arranque. Quedo a la espera de autorización antes de implementar cualquier corrección.

### Archivos de evidencia del cierre (`evidencia/cierre/` y raíz)

| Archivo | Contenido |
|---|---|
| `cierre/RESUMEN_equipo_exacto_PLAN-HIST-1.txt` / `..._PLAN-REV-2.0.txt` | Reproducción con los 29 códigos reales: elegibilidad, complementos, prompt, motivos, plan |
| `cierre/plan78_reproducido_equipo_exacto.csv`, `cierre/prompt_intento1_equipo_exacto.txt` | Plan y prompt con el equipo exacto |
| `cierre/sonda_v15_PLAN-HIST-1.csv`, `..._PLAN-REV-2.0.csv` | Sonda del validador real (80 combinaciones por política) |
| `cierre/contrato_v15_rejilla_*.csv`, `cierre/contrato_v15_resumen_*.txt` | Contradicciones del contrato actual y del propuesto (384 perfiles por política) |
| `cierre/seguridad_5_ejercicios.csv` | Ficha de 1303, 1152, 853, 1093 y 910 |
| `cierre/relajaciones_banderas_V37_V38.csv` | Las 13 relajaciones `true` → `false`, con su texto |
| `cierre/catalogo_contradicciones_texto_bandera.csv` | Barrido de triaje (69 filas, incluye falsos positivos) |
| `comparar_plan78.py` | Comparación ejercicio por ejercicio contra el JSON real (autoprobada) |
