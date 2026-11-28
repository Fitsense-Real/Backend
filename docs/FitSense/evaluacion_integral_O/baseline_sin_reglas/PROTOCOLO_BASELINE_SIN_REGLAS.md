# Línea base sin reglas · protocolo congelado (antes de la primera llamada pagada)

**Este documento, los prompts y el instrumento de conteo se commitean antes de BASE-R1.** Nada de lo que aquí figura se cambia después de ver resultados. Si apareciera un defecto en el instrumento, se corrige solo el instrumento, se documenta y se conservan las respuestas y el conteo original.

**Finalidad:** medir, no corregir. El flujo es:

**perfil + catálogo filtrado → Replicate sin reglas → respuesta original guardada → identificación de incumplimientos → conteo.**

Ninguna respuesta se corrige, se reintenta por su contenido, se publica ni se aplica.

## 1. Versión y condiciones

| Elemento | Valor |
|---|---|
| Comparación (condición A) | GRA-R1 y GRA-R2, commit `3883054`. Sus registros no se modifican |
| Condición B | `BASELINE_SIN_REGLAS`, corridas **BASE-R1** y **BASE-R2** |
| Código de producción | Sin cambios. Solo se añadió un modo de lectura al arnés de pruebas (`6a507f8`, `src/test`) |
| Cohorte | O01–O20 (`O_PERFILES.csv`, sha256 `9b996825…`), **O11 incluido**. O11-F35 no participa |
| Catálogo | V46. **Conjunto elegible que el backend recupera de PostgreSQL** para cada perfil (`PlanGenerationContext.availableExercises`), volcado en solo lectura: `protocolo/CATALOGO_FILTRADO_O01_O20.csv.gz` (sha256 del CSV `f54eaa7f…`) |
| Modelo | Replicate `openai/gpt-5-structured`, `model = gpt-5-mini`, `reasoning_effort = medium`, `json_schema`; `Prefer: wait=25`, sondeo cada 3 s, límite de 150 s. Son los parámetros del backend |
| Versión | GRA usó `9f4cd9ec…`. El 06/11/2026 a las 18:46 UTC el endpoint pasó a `045f8102…`. Se **intenta fijar `9f4cd9ec…`** con `POST /v1/predictions`. Si el proveedor la rechaza sin crear predicción, se usa la versión vigente del endpoint del modelo. La versión de cada predicción se registra |
| Política de evaluación | PLAN-REV-2.0, la misma con la que corrieron GRA-R1 y GRA-R2 |

### Verificación del catálogo

El volcado coincide con los `available_exercises` que GRA recibió en ids y en atributos (nombre, grupo, material, dificultad, tipo de prescripción, músculo y patrón), en los 19 perfiles con prompt de GRA: 0 diferencias de atributos.

Sin embargo, el prompt de GRA **no enviaba el conjunto completo**. Por ejemplo, O02 recibía 49 de 127 ejercicios y O06, 332 de 698. `PlanInputSnapshot` retira dos grupos:
- los ejercicios sin dosis admisible, porque «V24 lo rechazaría»;
- en GAIN_MUSCLE, los que no pueden demostrar hipertrofia (regla B1).

Las dos son reglas de planificación (para satisfacer V24). Por eso **la línea base recibe el conjunto elegible completo de PostgreSQL**, sin ese recorte. V1 del validador comprueba contra ese mismo conjunto completo.

## 2. Qué recibe cada condición

| Información | GRA (con reglas) | BASE (sin reglas) |
|---|---|---|
| Perfil: objetivo, nivel, edad, sexo, altura, peso, peso objetivo, lugar, material, días disponibles, días/semana, minutos/sesión, notas de salud | Sí (JSON) | Sí (texto) |
| Restricciones declaradas y ejercicios bloqueados | Aplicadas como filtro del catálogo | Mismo filtro **y** además escritas como dato |
| Catálogo filtrado: id, nombre, grupo, material, dificultad, tipo de prescripción, músculo, patrón | Sí, recortado por V24/B1 | **Sí, completo** |
| `min_reps`, `slot`, `clase_dosis`, `dosis` por ejercicio | Sí | **No** |
| División sugerida (fechas, enfoques, cuotas por grupo), `max_difficulty_level`, `max_reps`, `target_sets/reps`, `max_rest_seconds`, `dosis_por_clase`, `max_colocaciones_dia`, `minutos_utiles_dia`, `slots_exigibles`, `max_session_seconds` | Sí | **No** |
| Reglas 1–20, principios P-1.3, fórmula de duración, grupos por enfoque, 4C | Sí | **No** |
| Esquema JSON | `PlanJsonSchema` («Exactamente N entrenamientos…», «sin repetir exercise_id») | Mismo esquema con descripciones neutras («Los entrenamientos de la semana.», «Los ejercicios de esa sesión.») |
| Retroalimentación de validadores y reintentos guiados | Hasta 3 propuestas | **No: una sola propuesta** |
| Normalizador, respaldo determinista, bloqueo de factibilidad (O11) | Sí | **No** |
| **Adaptación:** plan vigente de la semana medida | Sesiones señaladas con candidatos, permisos e instrucciones (FIX-PR-2.1-EXP) y V8C | Plan completo (ids, nombres, dosis) **sin** señalar sesiones, intervenciones, candidatos, permisos ni reglas de conservación |
| **Adaptación:** lo ocurrido | Estado, porcentaje, motivo y marcas por ejercicio | Lo mismo, en texto |

Los textos completos están en `PROMPTS_BASELINE.md` y las entradas enviadas, con sus huellas, en `protocolo/`.

## 3. Ejecución y presupuesto

- **Llamadas:** BASE-R1 = 20 de generación + 10 de adaptación (los 10 ciclos con IA de GRA-R1). BASE-R2 = 20 + 10 (los de GRA-R2). Total previsto: **60**.
- **Un solo intento por elemento.** Solo se reintenta **una vez** un fallo del proveedor (predicción fallida o cancelada sin salida, o error HTTP), igual para todos. Una respuesta inválida o incompleta **no** se repite.
- **Presupuesto global verificado:**

  | Concepto | Predicciones | USD |
  |---|---:|---:|
  | Autorizado | 168 | 8,40 |
  | R_O y R′_O | 20 | 0,3033 |
  | GRA (suma de los `99_resumen.txt` de los seis bloques) | 76 | 1,5368 |
  | **Gastado** | **96** | **1,8401** |
  | **Remanente** | **72** | **6,5599** |

- **Control antes de cada predicción:** gastado + 1 ≤ tope y USD medido + reserva de 0,05 ≤ tope, con un registro compartido.
- **Topes por corrida:** BASE-R1 tiene un tope propio de la mitad del remanente (36 predicciones, USD 3,28), para que BASE-R2 conserve su parte. BASE-R2 usa el resto.
- **Si el presupuesto se agota,** las llamadas siguientes no se hacen y se informa exactamente lo completado.

## 4. Conteo en generación (solo lectura)

**Instrumento:** el `PlanDraftValidator` real (V1–V27, incluidos V24 y V25) con el **mismo contexto** de la generación de la semana 1 de cada perfil. El contexto se obtiene con `prepare`, de solo lectura, en una base efímera; no se persiste nada. Arnés `EvaluacionK20IT`, modo `baseline-evaluar`.

**Dos etapas, sin modificar el archivo guardado:**
- **CRUDO (medida principal):** la propuesta tal cual.
- **NORMALIZADO:** tras el normalizador mecánico del backend (tipo de prescripción y duración calculada). Es la etapa en la que GRA validaba sus propuestas y la que permite comparar etapas equivalentes.

**Verificación del instrumento:** las 54 propuestas de generación de GRA (todos los intentos, extraídos de `trazas_ia.jsonl`) se pasaron por el mismo modo. En NORMALIZADO reprodujo la decisión del backend en las 54 (16 rechazadas y 38 aceptadas, 0 discrepancias).

**Hallazgo por regla:** cada mensaje del validador (código Vn, objeto y texto).

**Infracción única:** hallazgos de una misma familia de causa sobre el mismo objeto (sesión, ejercicio, grupo o semana). El solapamiento queda registrado en la columna `solapamiento`.

| Familia | Códigos | Categoría de la orden |
|---|---|---|
| CATÁLOGO | V1, subclasificado con el catálogo | Inexistente o no identificable · Equipamiento · Restricciones personales · Ejercicios bloqueados · Dificultad · Fuera del catálogo filtrado (otro filtro) |
| DIFICULTAD | V6 | Dificultad |
| PRESCRIPCIÓN | V7 | Series y repeticiones |
| DOSIS_EJERCICIO | V9, V10, V16, V19 y V24 de dosis | Series y repeticiones (V9/V10/V16/V19) · Dosis (V24) |
| CARGA | V11 | Carga |
| DESCANSO | V21 | Descansos |
| DURACIÓN | V4, V17, V20 y V24 de coste | Duración |
| N_EJERCICIOS | V5 y V24 de colocaciones | Cantidad de ejercicios por sesión |
| FRECUENCIA | V2 | Frecuencia semanal |
| DÍAS | V3, V12 | Estructura del plan (días) |
| DISTRIB_SESIONES | V13, V18 | Distribución de sesiones |
| DISTRIB_EJERCICIOS | V15, V22 | Distribución de ejercicios |
| VOLUMEN_GRUPO | V26 | Volumen por grupo muscular |
| VOLUMEN_SEMANAL | V8 (rango) | Dosis (volumen semanal) |
| ESTRUCTURA | V8 (`total_volume` declarado) | Estructura del plan |
| PATRONES | V23, V25, V27 | Patrones obligatorios |

**Otras reglas de conteo:**
- Una propuesta sin JSON válido o que el backend no puede leer es **NO_EVALUABLE**. Se cuenta aparte y nunca como válida.
- Un fallo del proveedor se registra aparte.
- **Denominadores:**
  - 20 perfiles originales;
  - 19 factibles (O11 aparte, por ser inviabilidad esperada);
  - planes afectados y total de infracciones, siempre por separado.
- **Comparación con GRA con el mismo instrumento:**
  - primera propuesta de GRA frente a la única de BASE (etapas equivalentes);
  - todas las propuestas de GRA;
  - planes publicados por GRA (validados por construcción).

## 5. Conteo en adaptación (solo lectura)

Los validadores de adaptación (V8C / `ConservationValidator`) necesitan el estado de la base de cada corrida GRA, que era efímera. Por eso el conteo usa un **oráculo independiente** (`herramientas/contar_baseline.py`). El oráculo codifica la política técnica vigente: `TipoIntervencion`, `PermisosIntervencion` y las reglas de V8C con PLAN-REV-2.0.

**Datos de entrada por ciclo:**
- el plan publicado de la semana medida;
- las sesiones señaladas y las intervenciones del backend (`plan_adaptation_attempts`);
- las marcas de dolor;
- el catálogo filtrado;
- los minutos del perfil.

Las sesiones se emparejan por día de la semana. Las comparaciones de ejercicios son por id.

| Cat. | Cambio no autorizado | Regla de FitSense |
|---|---|---|
| C1 | Cualquier cambio en una sesión **no señalada** (ejercicios, dosis o descanso) | Las no señaladas se conservan |
| C2 | Más trabajo en una sesión señalada | La IA nunca aumenta el trabajo |
| C3 | Subida de series, repeticiones o segundos de un ejercicio | La IA nunca sube dosis |
| C4 | Ejercicio nuevo más difícil que el retirado (o que el máximo de la sesión) | Dificultad no superior |
| C5 | Ejercicio excluido por equipamiento, restricción o bloqueo | Catálogo filtrado |
| C6 | Ejercicio con dolor que sigue en la semana, cuando la intervención es SEGURIDAD_ANTE_DOLOR | Retirada por dolor |
| C7 | Ejercicio inexistente o no elegible por otro filtro | Catálogo filtrado |
| C8 | Sesión modificada que supera M × 60 s (V4 con PLAN-REV-2.0) | Duración disponible |
| C9 | Sustitución con otro patrón donde la política exige el mismo (P-A2) | Conservación de patrones |
| C10 | Cambio de un tipo que la intervención no permite | Permisos por intervención (tabla de abajo) |
| C11 | Ante dolor, más ejercicios añadidos que retirados | Rama de dolor (D-OR-3) |
| C12 | Sesión añadida, eliminada (salvo REPROGRAMAR_DIAS), duplicada o con fecha fuera de la semana | Estructura |

**Permisos de C10:**
- INVESTIGAR: solo dosis, **o** una sustitución equivalente estricta (P-A2), nunca las dos.
- ACORTAR: dosis y retirar **una** colocación de relleno; no añadir.
- REVISAR_CARGA: solo dosis.
- VARIANTE: sustituir sin subir el impacto (P-C2).
- REPROGRAMAR, CAUSA_NO_REGISTRADA y CONSERVAR: ningún cambio de contenido.

**No son cambios no autorizados:**
- las reducciones, retiradas y sustituciones que la política permite;
- un cambio distinto del de GRA pero permitido;
- la conservación de una sesión señalada, que se registra aparte como **SIN_EFECTO**.

**Comparación:** las adaptaciones publicadas por GRA se miden con el mismo oráculo.

**Verificación del instrumento:**
- GRA-R1: 0 cambios no autorizados en 10 ciclos;
- GRA-R2: 0 en 10, y detecta el SIN_EFECTO de O18;
- 4 de 4 pruebas negativas (`herramientas/pruebas_contar_baseline.py`).

**Corrección del instrumento antes de pagar (documentada):** la primera versión del oráculo usaba la tabla de permisos histórica y el techo de V4 + 15 %. Contra GRA marcaba 2 sustituciones de O05 (INVESTIGAR) que V8C había aceptado por P-A2. Se ajustó a PLAN-REV-2.0 antes de cualquier respuesta de la línea base.

## 6. Prohibiciones (de la orden)

No se modifican:
- el backend de producción, V24, V25, V8C, HIST ni los perfiles;
- las evidencias de GRA;
- Neon ni mobileapp.

No hay merge ni despliegues. No se publican los planes ni las adaptaciones de la línea base. No hay reintentos guiados, respaldo, simulación de semanas, progresión ni reincorporación. No se supera el presupuesto ni se inventan resultados.

## 7. Incidencia de ejecución (antes de la primera predicción)

El primer arranque de BASE-R1 se detuvo en la consulta gratuita de versión: HTTP 403 con «error code: 1010». Cloudflare bloquea el User-Agent por defecto de Python-urllib.

- **Predicciones creadas:** ninguna. El registro siguió en 96 / USD 1,8401.
- **Corrección:** cabecera `User-Agent: fitsense-baseline/1.0` en el cliente.
- **Lo que no cambia:** prompts, esquema, parámetros y criterios.

## 8. Versión fijada y forma de la salida (observado en la primera predicción)

- **Versión:** `POST /v1/predictions` con la versión de GRA (`9f4cd9ec…`) fue **aceptada** y la predicción se creó. Replicate devuelve `"version": "hidden"`, así que el proveedor no permite confirmar qué versión ejecutó. Se registra como «versión fijada solicitada, no verificable».
- **Forma de la salida:** en este modo llega como objeto `{"json_output": {…}}`. Es una de las formas que el backend ya maneja en `ReplicateResponse.joinedOutput()`, donde el plan es el contenido de `json_output`.
- **Cómo se trata:** el contador (`salida_backend`) aplica la misma regla antes de evaluar. La respuesta original se guarda íntegra en `respuestas/` y no se modifica.

## 9. Incidencia de ejecución en BASE-R2 (corte de red)

**Qué pasó:** tras 15 predicciones correctas, el POST de **O16/generación** recibió `ConnectionResetError` al leer la respuesta. El cliente no capturaba esa excepción y se detuvo.

**Investigación:**
- La predicción sí se había creado y había terminado en Replicate (`2nwmx17ta5rmy0d1q00rxhyrgg`, `succeeded`).
- Se recuperó por su id con GET, sin coste y **sin repetir la llamada**. Su prompt y su `json_schema` son idénticos byte a byte a los congelados de O16.
- Queda marcada como `recuperada` en `respuestas/O16_generacion.json`.

**Defecto de contabilidad encontrado:**
- El `break` tras una predicción correcta saltaba la escritura del registro de presupuesto, que solo se guardaba al final.
- El control antes de cada predicción usaba el contador en memoria y era correcto; lo único que no se escribía era el archivo.
- BASE-R1 terminó y quedó registrada. El registro de BASE-R2 se **reconstruyó** con las 16 respuestas guardadas: 126 → 142 predicciones, USD 2,3164 → 2,5838.

**Corrección del cliente (sin tocar prompts, esquema, parámetros ni criterios):**
- el registro se escribe tras cada elemento;
- un corte de conexión es un fallo del proveedor;
- antes del único reintento permitido, el cliente busca si la predicción ya se creó con el mismo prompt y la recupera, para no pagarla dos veces.

**Continuación:** BASE-R2 se reanuda en O17. Los elementos con respuesta guardada no se vuelven a pedir.
