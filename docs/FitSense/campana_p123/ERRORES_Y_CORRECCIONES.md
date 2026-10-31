# Errores y correcciones

## C-1 · El instrumento de validación independiente cortaba el linaje D8 en una señalada sin efecto (corregido)

| | |
|---|---|
| **Evidencia original** | Primera ejecución de la campaña: N03 S4–S10 = INVALIDA, por «V20: el entrenamiento del … dura unos 918 s y el mínimo es 1260 s». El backend las había publicado como válidas |
| **Reproducción** | N03 S3: ACORTAR retira 1117 (sesión de 15 min, exenta de V20 por D8). S4: esa sesión se omite y se señala INVESTIGAR; queda en `no_effect_sessions` del V32. Las copias siguientes heredan la exención |
| **Causa raíz** | `ValidacionIndependiente.reducible()` solo seguía `conserved_sessions` y `progressed_sessions`. Una señalada **sin efecto** es una copia: D8 hereda la exención «tras una señalada sin cambios» (`HerenciaD8IT`) |
| **Tipo** | Defecto del **instrumento de prueba**. El backend era correcto |
| **Corrección** | `no_effect_sessions` cuenta como copia en el recorrido del linaje |
| **Regresión que falla sin la corrección** | `HerenciaD8IT.respaldoIdenticoConservaD8` y `propuestaIdenticaConservaD8` validan ahora con el instrumento la copia tras una señalada SIN_EFECTO. Sin la corrección dan INVALIDA (comprobado); con ella pasan |
| **Repetición** | Campaña de nuevo: 0 INVALIDA |

## C-2 · V20 y D8 en una sesión acortada que se señala y vuelve igual de la IA (pendiente de decisión: no se modificó V8C)

| | |
|---|---|
| **Evidencia** | N03 S4: la sesión acortada (15 min, exenta) se omite y se señala INVESTIGAR_CAMBIO_MINIMO. La IA simulada la devuelve **igual** y V8C/V20 la rechazan 3 veces («le faltan 342 s»). Después, el respaldo publica **esa misma sesión**, válida y con la exención |
| **Causa** | En V8C (`ConservationValidator`), una sesión señalada solo recibe la exención D8 si su intervención permite reducir la duración (ACORTAR). Una conservada, sí |
| **Impacto** | Seguridad: ninguno (se publica lo mismo y es válido). **Coste:** con IA real, hasta 3 llamadas pagadas desperdiciadas en un caso que acaba igual |
| **Por qué no se corrigió** | Exige cambiar V8C, que la orden prohíbe. Además, «una señalada sin cambios hereda D8» solo está aprobado para las sesiones **no** señaladas |
| **Decisión necesaria** | ¿Una sesión acortada que se señala por otra causa y vuelve sin cambios conserva la exención D8 en el camino de la IA? Si la respuesta es sí: es un cambio de una línea en V8C, más una regresión |

## C-3 · P-03 podía publicar, ante dolor, una sesión sustituida con más trabajo (corregido antes de la campaña)

| | |
|---|---|
| **Evidencia** | Al adaptar `CorreccionesReplicate*IT.c5DolorAnadeMas` a P-03: SUST-SEG-1 recalcula la dosis del sustituto (fuera de GAIN_MUSCLE ajusta las repeticiones a la zona del sustituto), y en continuidad V8C no aplica la regla «ante dolor no se añade trabajo» a una sesión no señalada |
| **Causa raíz** | El nivel 1 de P-03 reutiliza la continuidad, que no conoce la regla de dolor de las señaladas |
| **Corrección** | `AdaptedWeekProducer.masTrabajoQueLaMedida`: en el nivel 1, si una sesión del dolor sube repeticiones o segundos totales respecto de la medida, no se publica. Pasa al nivel 2 o queda NO_FACTIBLE |
| **Regresión** | `c5DolorAnadeMas`: mismo número de ejercicios y volumen ≤ el medido |

## C-4 · Dobles de IA que leían lo que dolió del conjunto elegible (pruebas, corregido)

`CopiaSinIaIT` y `ValidacionMultisemanaIT` buscaban el ejercicio que dolió en `availableExercises` para copiar su patrón. Con P-02 ya no está ahí, y el doble lanzaba `NoSuchElementException`. Ahora el patrón y el grupo se leen del catálogo.

## C-5 · Arnés de la campaña (pruebas, corregido)

El detector de la ejecución del reincorporado usaba una expresión regular que fallaba con JSON anidado. Ahora lo lee con Jackson.

## Pruebas existentes actualizadas al comportamiento aprobado (no son defectos)

| Prueba | Antes | Ahora |
|---|---|---|
| `AdaptacionCompletaIT.dolorSinIa` | NO_FACTIBLE, semana sin plan | P-02 registra; P-03 publica sin lo que dolió |
| `LinajeIntegridadIT` (caso D) | `NO_FACTIBLE` | `RESPALDO` con el plan de P-03; el linaje se audita igual |
| `HerenciaD8IT.dolorNoAprovechaD8` | Sin plan | Nivel 1 o nivel 2, nunca lo que dolió |
| `CorreccionesReplicate*IT.c5DolorAnadeMas` | Sin plan | Nivel 1 sin más trabajo |
| `PerfilesLimiteIT` | Creaba perfiles de 15 min en LOSE_WEIGHT y GENERAL_FITNESS | P-01 los rechaza; U07 se diagnostica a 30 min |
| `EsquemaVigenteSobreCatalogoAnterior` | Aplicaba V42 | Aplica también V43 (solo esquema) tras fijar el catálogo |

**No se eliminó ni se relajó ninguna validación del backend.**
