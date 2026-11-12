# 04 · Auditoría de la PR #54: factibilidad, reintentos y presupuesto

**Pregunta:** ¿puede la optimización evitar llamadas a Replicate en perfiles donde la IA sí podría generar una rutina válida, también al segundo o tercer intento?

**Principio aplicado:** una generación lenta pero válida es preferible a descartar una solución.

**Ninguna llamada a Replicate (USD 0).** Cada cifra lleva su origen:
- **SINTÉTICO**: prueba unitaria con datos inventados.
- **VALIDADOR REAL**: catálogo, contexto y `PlanDraftValidator` reales, en PostgreSQL de Testcontainers.
- **HISTÓRICO**: trazas de campañas con IA real.

Evidencias en `evidencia/auditoria_pr54/`.

## 1. Cálculo exacto de la cota (`FactibilidadGeneracionIA`)

**Cuándo se evalúa.** Solo si se cumplen **todas** estas condiciones. En cualquier otro caso devuelve vacío y la IA se llama como siempre.
- La semana tiene **una sola sesión** (`effectiveDaysPerWeek == 1`).
- El objetivo se prescribe por capacidades, es decir, **no es GAIN_MUSCLE** (su V24 es otra, 4D v3).
- La política mide V4 en **segundos** (PLAN-REV-2.0).
- V24/V25 están en modo **ENFORCE** (`fitsense.capability.enforcement`).
- Están disponibles el perfil, la prescripción, la política y el conjunto elegible.

**Qué ejercicios considera.**
- **Todo el conjunto elegible** del participante (`context.availableExercises()`), sin repetidos. Es exactamente el mismo con el que la IA puede proponer y contra el que valida V1.
- Ese conjunto ya aplica nivel, equipamiento, ubicación, restricciones personales (impacto, suelo, rodillas, colgarse, por encima de la cabeza), ejercicios bloqueados y dolor. La comprobación **no filtra nada más**: si filtrara, la cota podría subir.

**Dosis: series, repeticiones y descansos.**
- Para cada ejercicio y cada capacidad admisible (`CapabilityPrescriptionPolicy.admisibles`), la dosis exacta que exige V24 extendida (`CapabilityPrescriptionPolicy.dosis`), con el número **mínimo** de series de su rango.
- V24 solo acepta una colocación cuya dosis coincida exactamente con una dosis admisible (`acreditada` → `Dosis.coincideCon`): mismo tipo, series dentro de [series, seriesMax], descanso idéntico y repeticiones o segundos idénticos.
- Por tanto, ninguna colocación aceptable puede durar menos que esa dosis con el mínimo de series.
- La duración sale del **mismo** `SessionDurationEstimator` que usa V4.

**Objetivo y nivel.** Entran a través de `admisibles` y `dosis` (las mismas funciones que V24) y de `CapabilityCoveragePolicy`, que da las principales factibles que V25 exige. No hay tablas propias.

**Patrones obligatorios.**
- Se usan los slots exigibles de V23 (`SlotCoveragePolicy.requiredOf(ofEligible(...))`) y el slot de cada ejercicio (`CoberturaDePatron.slot`), las dos funciones de V23.
- **V23 solo cuenta si bloquea a la IA**, con el modo real del validador:
  - con `ENFORCE_AI_ONLY` (configuración evaluada), exige los slots solo si la sesión tiene al menos tantas colocaciones como slots exigibles, igual que V23;
  - con `ENFORCE_ALWAYS`, siempre.

**Búsqueda.**
- Programación dinámica exacta (Dijkstra) sobre lo que falta por cubrir: capacidades de V25 y slots de V23, contando colocaciones para V5 (≥ 2) y para el umbral de V23.
- Las colocaciones se pueden repetir. Es una relajación: solo puede bajar la cota.

**Condición de omisión.**
- La cota supera el techo de V4 (`PlanningPolicy.excedeTecho`, el mismo predicado del validador), o ninguna combinación cumple V5 + V23 + V24 + V25.
- Entonces se registra `IA_NO_INVOCADA motivo=INVIABLE_PARA_IA` y decide el flujo determinista vigente.

**Supuestos y relajaciones** (todos bajan la cota o la dejan igual; ninguno la sube):

| Regla ignorada o relajada | Efecto sobre la cota |
|---|---|
| V1, V6 (dificultad), V7 (tipo de prescripción), V15, V18, V26, V27, tope de 8 colocaciones, CARDIO por sesión (LW), slots con fuerza (IS) | Ignorarlas solo puede bajarla |
| Colocaciones repetibles, mismo ejercicio varias veces | Solo puede bajarla |
| Series al mínimo de cada dosis | Es el mínimo exacto que acepta V24 |

**No hay una copia de las políticas.** Cada número sale de las funciones que usa el validador. Si una política cambia, la comprobación cambia con ella.

**Lo que sí ata la comprobación al validador.** Si alguien cambiara el validador de forma que aceptara una colocación sin dosis exacta, la cota dejaría de ser válida. Lo vigilan:
- `AuditoriaFactibilidadIT`, que comprueba que una dosis perturbada no pasa;
- `FactibilidadGeneracionIAIT`, que corre en CI.

Ver §8.

## 2. Verificación independiente: el validador real como único juez

**Prueba:** `AuditoriaFactibilidadIT`, laboratorio opt-in: `-Dfitsense.auditoria.factibilidad=true`. Usa VALIDADOR REAL.

**Perfiles: 294.**
- Rejilla de una sesión: 4 objetivos × 3 niveles × gimnasio/casa × 30–75 min.
- GENERAL_FITNESS intermedio y avanzado, minuto a minuto entre 30 y 45 (el umbral).
- Los 6 perfiles históricos de una sesión.

**Borradores generados, 400 por perfil (117.200 en total).** Todos «como de la IA», y el juez es `PlanDraftValidator.validate`:
- el plan del motor de reglas y sus 4 variantes deterministas;
- construcciones aleatorias que cubren lo exigido, a veces sin los slots, para explorar el caso en que V23 no aplica;
- mutaciones de los aceptados: cambiar, quitar o añadir una colocación;
- dosis perturbadas fuera de las exactas: repeticiones −1, segundos −5, descanso −15.

**Condición comprobada en cada borrador aceptado:** `cota_inferior <= duracion(P)`.

| Resultado | Valor |
|---|---|
| Borradores aceptados por el validador | **26.988** |
| Perfiles con al menos uno aceptado | 250 |
| **Violaciones de `cota <= duración(P)`** | **0** |
| **Perfiles marcados inviables con un borrador aceptado** | **0** |
| Dosis fuera de las exactas aceptadas por el validador | **0** (confirma el supuesto de dosis de §1) |
| Holgura (duración mínima aceptada − cota), 190 perfiles | mínimo **0 s**, mediana 102 s |

**La cota no está inflada.** En varios perfiles, la propuesta válida más corta encontrada **coincide exactamente** con la cota (holgura de 0 s).

**Sin propuesta válida conocida.** En 21 perfiles del alcance la búsqueda no encontró ningún borrador aceptado y la cota cabe en el techo. Su factibilidad **no está demostrada** en ningún sentido, y por eso **se llama a la IA** (caso 7). La comprobación solo omite cuando la imposibilidad está demostrada.

**Pruebas sintéticas** (`FactibilidadGeneracionIATest`, SINTÉTICO):
- La cota coincide con una búsqueda exhaustiva por fuerza bruta en **300 + 600 casos**.
- Los 600 nuevos son casos límite: hasta 4 capacidades y 4 slots, V23 siempre activa o por posiciones, y opciones escasas.

**Alcance de la evidencia.** Ninguna muestra demuestra la ausencia universal de falsos positivos. La seguridad se apoya en el argumento de §1: cada regla usada es exactamente una que el validador aplica y todo lo demás está relajado. Las pruebas buscan contraejemplos y no encontraron ninguno.

## 3. Perfiles inviables y umbral

**22 perfiles demostrados inviables**, todos de GENERAL_FITNESS intermedio o avanzado:

| Lugar | Inviable | Cota | Primer minutaje que cabe |
|---|---|---|---|
| Gimnasio | 30–37 min | **2265 s** (37 min 45 s) | 38 min (2280 s): se llama a la IA |
| Casa (peso corporal) | 30–32 min | **1953 s** (32 min 33 s) | 33 min (1980 s): se llama a la IA |

Ningún otro objetivo ni nivel resulta inviable.

## 4. Perfiles históricos (HISTÓRICO)

`evidencia/auditoria_pr54/HISTORICO_FRENTE_A_COMPROBACION.csv`

**Fuentes: 191 generaciones iniciales con IA real.**
- GRA R1 y R2 (PR-1.7).
- CONF-IA C1–C3: PR-1.7, pr-1.8c y pr-1.8a (rama `experimento/mejora-ia`).
- Campaña MVP y campaña de estrés.

| Concepto | Generaciones | Descartadas por la comprobación |
|---|---|---|
| Propuesta válida al **primer** intento | 123 | **0** |
| Propuesta válida al **segundo** intento | 36 | **0** |
| Propuesta válida al **tercer** intento | 21 | **0** |
| Respaldo / sin plan | 6 / 4 | — |
| **Total descartadas** | — | **0** |

- **Fuera de alcance por código:** 175 de 191 tienen dos o más sesiones semanales, y la comprobación devuelve vacío para ellas.
- **Las 16 de una sesión** son O10 (×9), O11 (×2), O11-F35 (×2), U04 de la campaña MVP, U05 y U07 de la de estrés:
  - **todas se envían a la IA** hoy, salvo U07, cuyos 15 min ya no admite el perfil (P-01);
  - las de una sesión con propuesta válida (O10, O11-F35, U05) siguen llamando a la IA;
  - U04 de la campaña MVP, que acabó en respaldo tras 3 rechazos por V23, también.
- **Ningún perfil histórico con propuesta de IA válida, en cualquier intento, queda descartado.**

## 5. H-G9: por qué el motor de reglas publica y la IA no puede

`evidencia/auditoria_pr54/HG9_MOTOR_DE_REGLAS.txt`. VALIDADOR REAL, en los 22 perfiles inviables:

| Pregunta | Respuesta |
|---|---|
| ¿El plan determinista pasa el validador? | **Sí, como RULE_ENGINE** |
| ¿Pasaría si lo hubiera propuesto la IA? | **No: lo rechaza V23**, y solo V23 |
| ¿Por qué? | **V23 está en `ENFORCE_AI_ONLY`**, una decisión documentada en `application.properties`: rechaza el borrador de la IA, pero no el del motor de reglas, para que una caída del proveedor no deje participantes sin plan |
| ¿Qué publica el motor de reglas? | 4–6 colocaciones, 1641–2217 s, **sin cubrir uno o dos slots** (LOWER_HIP o UPPER_PULL) |
| ¿Sin V23 sería inviable? | **No.** La cota sin V23 es **1599 s**, por debajo del techo |
| ¿Usa autorizaciones u otros límites de dosis? | No. Mismas dosis (V24) y mismo V4; la única diferencia es V23 |

**No hay contradicción.**
- El motor de reglas y la IA **no** están bajo las mismas restricciones: el validador exige V23 solo a la IA.
- La imposibilidad demostrada es exacta: ninguna propuesta **de la IA** puede pasar el validador tal como está configurado.
- La comprobación no se justifica por el respaldo, sino porque ninguna de las tres propuestas podría publicarse nunca.
- **No se confunde una estructura concreta con cualquier plan.** La cota recorre todas las combinaciones del conjunto elegible que el validador podría aceptar.

**Decisión pendiente, que no es de esta PR:** si V23 debería aplicarse igual a ambos orígenes. Cambiarlo sería una regla deportiva y no se toca.

## 6. Reintentos (casos 1–7)

**Prueba:** `ReintentosYFactibilidadIT`, que corre en CI.
- Pipeline, validador V1–V27 y motor de reglas reales.
- La IA es un doble que devuelve, por intento, una propuesta inválida (V2) o la válida conocida.

| Caso | Resultado |
|---|---|
| 1 · Válida al primer intento | Publica IA, 1 llamada |
| 2 · Inválida y luego válida | Publica IA en el 2.º intento. El reintento recibe su propuesta rechazada y los motivos (V2) |
| 3 · Dos inválidas y luego válida | Publica IA en el 3.º intento, con feedback |
| 4 · Tres inválidas | Respaldo determinista (`generation_attempts` = 4) |
| 5 · Inviable demostrado (H-G9, 30 min) | 0 llamadas; flujo determinista (`generation_attempts` = 1) |
| 6 · Aparentemente inviable (una sesión, 30 min, avanzado) con solución conocida (O10) | Se llama a la IA y publica IA |
| 7 · Factibilidad no demostrada (GENERAL_FITNESS intermedio, 38 min, junto al umbral) | Se llama a la IA, con sus 3 intentos |

**Sin cambios:**
- Ni el número de intentos (3) ni las condiciones de reintento.
- El feedback (`AiRetryFeedback` con la propuesta rechazada y los motivos acumulados) sigue igual.

## 7. Presupuesto global

`evidencia/auditoria_pr54/PRESUPUESTOS_SOBRE_TRAZAS.csv`. HISTÓRICO, reproducido aritméticamente, con el sondeo de 1 s en todas las configuraciones:

| Conjunto | Configuración | Planes de IA | Perdidos | Cortadas y canceladas | p50 | p95 | Máx. |
|---|---|---|---|---|---|---|---|
| GRA (40, PR-1.7) | Sin presupuesto | 40 | 0 | 0 | 74,3 s | 267,6 s | 345,1 s |
| GRA | 360 s | 40 | 0 | 0 | 74,3 s | 267,6 s | 345,1 s |
| GRA | 420 s | 40 | 0 | 0 | 74,3 s | 267,6 s | 345,1 s |
| GRA | 480 s | 40 | 0 | 0 | 74,3 s | 267,6 s | 345,1 s |
| Estrés (8, prompt anterior) | Sin presupuesto | 6 | 0 | 0 | 144,2 s | 432,5 s | 455,3 s |
| Estrés | **360 s** | 4 | **2** (U06, 460 s; U02, 394 s) | 2 | 144,2 s | 360 s | 360 s |
| Estrés | 420 s | 5 | **1** (U06) | 1 | 144,2 s | 409,5 s | 420 s |
| Estrés | 480 s | 6 | 0 | 0 | 144,2 s | 432,5 s | 455,3 s |

**Respuestas a la orden:**
- **Tiempo de cada intento y de la validación:** cada intento es una predicción de 35–145 s. La validación tarda 3–6 ms.
- **Margen antes de un nuevo intento:** se comprueba el presupuesto restante justo antes de cada intento. Ningún intento queda «no empezado» en estas trazas.
- **Una predicción a punto de terminar:** se corta al llegar el plazo y se pide la cancelación (R-2). Si al cancelar ya había terminado con éxito, se usa su salida.
- **Los 40 s mínimos:** la predicción de generación más corta medida tardó 35,7 s.
- **Al agotarse el presupuesto:** decide el respaldo determinista, que publica un plan **distinto**, validado y sin IA. **No es funcionalmente equivalente** a la propuesta de IA que se pierde.
- **360 s cambia éxitos de IA por respaldos** en colas observadas (prompt anterior).

**Decisión: desactivado por omisión** (`fitsense.ai.generate.total-budget-seconds=0`) y configurable.
- 480 s fue el menor valor que no cambió ninguno de los 48 desenlaces observados.
- Pero es una muestra pequeña, con solo 20 s de margen sobre el máximo observado (455–460 s). No se activa hasta que se decida con más datos, por ejemplo con `GENERACION_TIEMPOS` en el entorno de pruebas.
- Sin presupuesto, cada intento sigue acotado por R-3/R-2: ≤ ~291 s por intento, ≤ ~331 s si hay que cancelar.

## 8. Decisión sobre cada optimización

| Optimización | Decisión | Por qué |
|---|---|---|
| **Comprobación previa** | **Se conserva, activa, dentro de su alcance** (una sesión, no GAIN_MUSCLE, V4 en segundos, V24/V25 en ENFORCE) | Construida solo con reglas del validador, todo lo demás relajado. 0 violaciones en 26.988 propuestas válidas, 0 inviables con propuesta válida, 0 descartes en 191 generaciones históricas. Lo no demostrado llama a la IA. Interruptor: `FITSENSE_AI_GENERATE_FEASIBILITY_PRECHECK=false` |
| **Sondeo cada 1 s** | **Se conserva** (configurable: `FITSENSE_AI_POLL_INTERVAL_MILLIS`) | No cambia desenlaces ni cancelaciones (las pruebas de R-2 siguen en verde). Retraso medio de 1,5 a 0,5 s. Riesgo: unas 3 veces más consultas GET; volver a 3000 no exige desplegar código |
| **Presupuesto global** | **Desactivado por omisión** (configurable) | 360 s perdería 2 de 8 éxitos de IA de la campaña de estrés; 480 s preservó los 48, con poca muestra |
| **Instrumentación** (`GENERACION_TIEMPOS`) | **Se conserva** | Solo observa |

## 9. Riesgos y limitaciones

- **Dependencia del validador.** La comprobación es tan correcta como la equivalencia entre sus funciones y las del validador. Si V23, V24, V25 o V4 cambian de semántica, hay que repetir `AuditoriaFactibilidadIT`.
  - Por ejemplo, si V24 empezara a aceptar dosis no exactas o V23 cambiara su umbral de posiciones.
  - `FactibilidadGeneracionIAIT` y `ReintentosYFactibilidadIT` corren en CI.
- **La búsqueda no es exhaustiva** sobre todos los planes posibles. Encuentra propuestas válidas y comprueba la desigualdad; la prueba de seguridad es el argumento de §1.
- **H-G9 existe por una asimetría de configuración** (V23 solo para la IA). Si se decide aplicar V23 también al motor de reglas, esos perfiles quedarían sin plan; y si se relaja para la IA, dejarían de ser inviables. Ambas son decisiones de política, fuera de esta PR.
- **La evidencia de presupuestos es HISTÓRICA y pequeña:** 48 generaciones, 8 de ellas con un prompt anterior.
- **El sondeo de 1 s no se ha medido contra Replicate real.**
