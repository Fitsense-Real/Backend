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

## C-2 · V20 y D8 en una sesión acortada que se señala y vuelve igual de la IA (corregido en el cierre del PR #16)

| | |
|---|---|
| **Evidencia** | N03 S4: la sesión acortada (15 min, exenta por D8) se señala INVESTIGAR y la IA simulada la devuelve **igual**. V20 la rechaza 3 veces; después el respaldo publica esa misma sesión con D8 |
| **Causa** | En V8C, una sesión señalada solo heredaba D8 si su intervención permite reducir la duración |
| **Decisión** | Aprobada en la orden de cierre del PR #16: herencia limitada por identidad |
| **Corrección** | `ConservationValidator`: hereda la autorización de origen solo si la sesión queda SIN_EFECTO (idéntica, en el mismo orden), sin dolor, sin exclusiones, sin sustituciones, sin dosis rechazada y con una intervención que admite la copia. V20 no cambia |
| **Regresiones que fallan sin la corrección** | `HerenciaReduccionV20Test.identicaConservaD8` y `C2ReproduccionN03IT` (antes RESPALDO con 3 llamadas; ahora SIN_EFECTO con 1). Casos negativos: repetición, descanso, ejercicio, orden, sin D8, dolor y SEGURIDAD_ANTE_DOLOR |
| **Detalle** | `CIERRE_C2_D8_V8C.md` |

## C-3 · P-03 podía publicar, ante dolor, una sesión sustituida con más trabajo (corregido antes de la campaña)

| | |
|---|---|
| **Evidencia** | Al adaptar `CorreccionesReplicate*IT.c5DolorAnadeMas` a P-03: SUST-SEG-1 recalcula la dosis del sustituto (fuera de GAIN_MUSCLE ajusta las repeticiones a la zona del sustituto), y en continuidad V8C no aplica la regla «ante dolor no se añade trabajo» a una sesión no señalada |
| **Causa raíz** | El nivel 1 de P-03 reutiliza la continuidad, que no conoce la regla de dolor de las señaladas |
| **Corrección** | `AdaptedWeekProducer.masTrabajoQueLaMedida`: en el nivel 1, si una sesión del dolor sube repeticiones o segundos totales respecto de la medida, no se publica. Pasa al nivel 2 o queda NO_FACTIBLE |
| **Regresión** | `c5DolorAnadeMas`: mismo número de ejercicios y volumen ≤ el medido |

## C-6 · P-03 nivel 2 podía deshacer una reducción autorizada en plena semana de dolor (corregido en el cierre del PR #16)

| | |
|---|---|
| **Evidencia** | Auditoría pedida en la orden. LOSE_WEIGHT con reducción por fatiga y después dolor: la regeneración con dosis base subía la semana de 102 a 108 repeticiones. Tras un ACORTAR, de 450 a 486 |
| **Causa raíz** | El nivel 2 genera desde cero con las dosis base de 4D y no tiene linaje hacia las dosis reducidas ni hacia las retiradas de ACORTAR |
| **Corrección** | `SafeRegenerationService.masTrabajoQueLaMedida`: si la semana regenerada sube las repeticiones o los segundos totales de la semana medida, queda NO_FACTIBLE con el motivo («ante dolor no se añade trabajo»). No es una política nueva: aplica las reglas aprobadas |
| **Regresión** | `ProgresionIT.p03Nivel2TrasReduccion` (falla sin la corrección: 102 → 108), `p03Nivel2TrasProgresion`, `HerenciaD8IT.dolorNoAprovechaD8` |
| **Detalle** | `AUDITORIA_P03_NIVEL2.md` |

## C-7 · V43: integridad y concurrencia (corregido en el cierre del PR #16, antes de su primera aplicación)

| | |
|---|---|
| **Hallazgos** | Faltaban FK al ejercicio, al evento original y a la semana medida. El registro del dolor hacía «existe y luego inserta», así que una carrera omisión/ciclo podía violar la unicidad y abortar el ciclo. Dos reactivaciones simultáneas se pisaban |
| **Corrección** | FK e índice en V43; `INSERT … ON CONFLICT DO NOTHING`; reactivación con `FOR UPDATE` |
| **Pruebas** | `MigracionV43IT`, `PoliticasP123IT.reactivacionesSimultaneas` y `dolorNuevoTrasReactivar` |
| **Detalle** | `AUDITORIA_MIGRACION_V43.md` |

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
| `AdaptacionRegresionIT.cardioNoSustituyeAlTrabajoEstructural` | La propuesta inválida (V23) no publicaba nada | La propuesta sigue sin publicarse (V32 NO_FACTIBLE, 3 llamadas); si hay plan, es el de P-03 con reglas |
| `PerfilesLimiteIT` | Creaba perfiles de 15 min | P-01 definitiva (30–120) los rechaza; U07 se diagnostica a 30 min |
| `GeneracionPorObjetivoIT.principalesConsumenElTiempo` | Perfil de 20 min | Perfil de 30 min, el mínimo de P-01; **el resultado no cambia** (sin secundarias) |
| `HerenciaReduccionV20Test.intervencionNuevaInterrumpe` | Una señalada idéntica perdía D8 | Sustituida por los casos A–E de C-2 |
| `HerenciaD8IT.propuestaIdenticaConservaD8` | RESPALDO | SIN_EFECTO (C-2) |
| `HerenciaD8IT.dolorNoAprovechaD8` | Nivel 2 publicaba con reglas | NO_FACTIBLE: la regeneración subiría el trabajo (C-6) |
| `EsquemaVigenteSobreCatalogoAnterior` | Aplicaba V42 | Aplica también V43 (solo esquema) tras fijar el catálogo |

**No se eliminó ni se relajó ninguna validación del backend.**

## Batería final

`mvn verify -Pintegration` en verde:
- **792 pruebas unitarias**;
- **416 de integración**, con 30 opcionales omitidas (campañas con IA real y la campaña p123, que es opt-in).

Línea base en `generation`: 765 unitarias y 284 de integración.
