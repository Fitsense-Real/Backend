# Línea base sin reglas · resumen de resultados

**Experimento:** medición, no corrección. El mismo modelo de Replicate, con los mismos 20 perfiles O01–O20 y el mismo catálogo filtrado de PostgreSQL, **sin** las reglas de planificación ni de adaptación de FitSense. Dos corridas, BASE-R1 y BASE-R2.

**Tratamiento de las respuestas:**
- se guardaron íntegras, sin corregir, reintentar por contenido, publicar ni aplicar;
- se contaron después, en solo lectura, con el validador real del backend (V1–V27) y con el oráculo de adaptación, que codifica V8C y los permisos de PLAN-REV-2.0.

**Comparación:** con GRA-R1 y GRA-R2 (commit `3883054`), medidos con el mismo instrumento. Protocolo y criterios congelados en `ba2f16e`, antes de la primera llamada pagada.

## 1. Ejecución

| | BASE-R1 | BASE-R2 | Total |
|---|---:|---:|---:|
| Predicciones de generación (O01–O20, O11 incluido) | 20 | 20 | 40 |
| Predicciones de adaptación (ciclos de GRA) | 10 | 10 | 20 |
| `succeeded` | 30/30 | 30/30 | **60/60** |
| Fallos del proveedor / no evaluables | 0 / 0 | 0 / 0 | 0 / 0 |
| Coste medido (USD) | 0,4763 | 0,4831 | **0,9594** |

**Presupuesto global:** 156 de 168 predicciones y USD 2,7995 de 8,40. Ninguno de los dos topes se superó y no hubo llamadas adicionales.

**Versión:** se solicitó fijada a la de GRA (`9f4cd9ec…`) y el proveedor la aceptó en las 60. Replicate la informa como `hidden`, así que no se puede confirmar qué versión ejecutó.

## 2. Generación inicial: violaciones (Tabla A de `TABLAS_IEEE_BASELINE.md`)

**Etapa comparable:** primera propuesta de cada condición, tras el normalizador mecánico del backend (que es como GRA validaba). 19 perfiles factibles por corrida.

| | FitSense con reglas (GRA-R1 + R2) | Sin reglas (BASE-R1 + R2) |
|---|---|---|
| Planes con ≥ 1 violación | 13/38 (34,2 %) | **38/38 (100 %)** |
| Planes sin violaciones | 25/38 (65,8 %) | 0/38 (0 %) |
| Infracciones únicas | 41 (1,1 por plan) | **1 296 (34,1 por plan; máximo 83)** |
| Infracciones sin normalizador (CRUDO) | 45 | 1 413 |
| Planes publicados con violaciones | **0/38**: los 13 rechazos se resolvieron en el reintento | no se publica |

**Por corrida:**
- GRA-R1 frente a BASE-R1: 7/19 planes con violaciones (22 infracciones) frente a 19/19 (637).
- GRA-R2 frente a BASE-R2: 6/19 (19) frente a 19/19 (659).

**Infracciones sin reglas por tipo (BASE-R1 + R2):**

| Tipo | Infracciones |
|---|---:|
| Dosis (V24) | 783 |
| Patrones obligatorios (V23/V25/V27) | 117 |
| Carga sugerida sin historial (V11) | 109 |
| Series y repeticiones | 107 |
| Volumen por grupo muscular (V26) | 87 |
| Distribución de ejercicios (V15/V22) | 47 |
| Distribución de sesiones (V13/V18) | 24 |
| Ejercicios por sesión | 12 |
| Frecuencia semanal (V2) | 6 |
| Duración | 3 |
| Descansos | 1 |

**Catálogo:** 0 ejercicios inexistentes, de equipamiento no disponible, restringidos o bloqueados. El modelo eligió siempre de la lista filtrada que recibió.

**Comparación pareada por perfil:**

| Corrida | Violan solo sin reglas | Violan solo con reglas | Violan en ambas | McNemar exacta |
|---|---:|---:|---:|---|
| 1 | 12 | 0 | 7 | p = 0,00049 |
| 2 | 13 | 0 | 6 | p = 0,00024 |

La prueba se aplica a 19 pares por corrida; no se generaliza más allá de esta cohorte sintética.

**O11** (avanzado, fuerza, una sesión de 30 min), reportado aparte:
- FitSense lo declara inviable antes de llamar al modelo.
- Sin reglas, el modelo generó un plan en las dos corridas, con 19 y 17 infracciones.

## 3. Adaptación: cambios no autorizados (Tabla B)

Los mismos 10 ciclos con IA de cada corrida GRA, partiendo de los planes válidos publicados por GRA.

| | FitSense con reglas (GRA-R1 + R2, publicadas) | Sin reglas (BASE-R1 + R2) |
|---|---|---|
| Adaptaciones con ≥ 1 cambio no autorizado | **0/20** | **20/20 (100 %)** |
| Cambios no autorizados | 0 | **107** (59 + 48) |

**Cambios por tipo (BASE-R1 + R2):**

| Código | Cambio no autorizado | Cambios |
|---|---|---:|
| C1 | Sesión no señalada modificada | 44 |
| C3 | Subida de dosis de un ejercicio | 22 |
| C10 | Cambio que la intervención no permite | 18 |
| C2 | Más trabajo en la sesión señalada | 9 |
| C6 | Ejercicio excluido por dolor que sigue en el plan | 5 |
| C4 | Más dificultad | 4 |
| C8 | Duración excedida | 4 |
| C11 | Ante dolor, más añadidos que retirados | 1 |

**SIN_EFECTO** (conservación de una sesión señalada, que se registra aparte y no se cuenta como cambio):
- línea base: 0;
- GRA-R2: 1 (O18, ya informado como evitable).

**Cambios distintos pero permitidos** (reducciones, sustituciones autorizadas, retirada de un relleno en ACORTAR) **no** se contaron.

## 4. Investigación de discrepancias (clasificación de la orden)

| Clase | Hallazgo | Tratamiento |
|---|---|---|
| **Violación del modelo, por reglas que no recibió** | Casi toda la diferencia. Las 783 infracciones de dosis son V24: el ejercicio no lleva la dosis de ninguna capacidad, o no aporta capacidad o hipertrofia. Es la tabla interna de dosis de FitSense, que la línea base no recibe por diseño. Igual con 4C, V25, V26 y `min_reps` | Es exactamente lo que mide el experimento: el efecto de esas reglas. Se informa separado de los datos que sí recibió |
| **Violación del modelo, por datos que sí recibió** | Generación: 6 planes con más sesiones que los días/semana pedidos (V2) y 3 sesiones por encima de los minutos declarados (V4). Adaptación: 5 casos que mantienen un ejercicio con dolor del que se le informó | Se cuentan como infracciones |
| **Diferencia de contexto** | La línea base recibió el conjunto elegible completo. El prompt de GRA retiraba los ejercicios sin dosis admisible (V24) y, en GAIN_MUSCLE, los que no acreditan hipertrofia (B1). De las 783 infracciones V24, 83 son sobre ejercicios que el prompt de GRA no habría ofrecido | Es una regla de planificación (protocolo §1). Las otras 700 son sobre ejercicios que GRA también veía |
| **Defecto del evaluador (I-1)** | El validador añade una sugerencia sin código («DIVISION: usa estas fechas…») cuando hay V13/V18, y el contador la contaba como infracción | Corregido solo el instrumento: −5 hallazgos por etapa en BASE-R1 y −1 en GRA-R2 (O14). Conteo original conservado en la evidencia (`conteo_r1_instrumento_v1_ORIGINAL`) |
| **Defecto del evaluador, antes de pagar** | Tabla de permisos histórica y techo V4 + 15 % en el oráculo de adaptación: marcaba 2 cambios de GRA (O05, P-A2) que V8C acepta | Corregido antes de la primera llamada; el oráculo da 0/0 en GRA |
| **Normalización** | Con la versión fijada, la salida llega como `{"json_output": …}` | Se desenvuelve igual que `ReplicateResponse.joinedOutput()` del backend. El contenido no se modifica |
| **Proveedor** | BASE-R2, O16: corte de conexión al leer el POST. La predicción se había creado y había terminado | Recuperada por su id, sin repetir la llamada (prompt idéntico byte a byte). Registro de presupuesto reconstruido |
| **Variabilidad legítima** | BASE-R1 frente a R2: 637 frente a 659 infracciones y 59 frente a 48 cambios | Mismo patrón cualitativo en ambas corridas |

**Validación del instrumento:**
- el validador en etapa NORMALIZADO reproduce las 54 decisiones del backend en las propuestas de GRA;
- el oráculo de adaptación da 0 cambios en las 20 adaptaciones publicadas por GRA;
- 4 de 4 pruebas negativas.

## 5. Límites

- **Cohorte:** sintética y pequeña (19 + 1 perfiles; 10 ciclos por corrida).
- **Pruebas:** solo la de McNemar indicada; no se afirma nada fuera de esta cohorte.
- **Peso de V24:** la mayor parte de las infracciones de generación son de la política interna de dosis (V24). Una línea base que no la conoce no puede cumplirla; el resultado mide el efecto de tenerla, no un defecto general del modelo.
- **Adaptación:** se evaluó con un oráculo independiente que codifica V8C. `ConservationValidator` no se pudo ejecutar porque requiere el estado efímero de las bases de GRA.
- **Calidad deportiva:** «sin violaciones» no implica calidad deportiva, que sigue pendiente de revisión profesional.
