# Ensayo sin coste (fase 1)

Fecha: 17/11/2026. **0 predicciones pagadas.** No salió nada hacia Replicate: el proveedor era simulado.

## 1. Pruebas automáticas con proveedor simulado

Base de partida `65e1b7e`, con un PostgreSQL efímero para la IT opt-in:

| Prueba | Resultado |
|---|---|
| `GeneracionAsincronaIT` (opt-in) | 15/15 |
| `GeneracionAsincronaResilienciaIT` (R01–R20) | 23/23 |
| `ReanudacionConReplicateSimuladoIT` | 11/11 |
| `MigracionV58aV59IT` | 1/1 |

Estas pruebas cubren las operaciones, el libro, los estados, la reanudación, el vencimiento, la publicación única, el
recuento de predicciones (`GENERACION_ASINCRONA_PREDICCIONES`) y el límite de 3 intentos.

## 2. Ensayo de extremo a extremo (backend real + Replicate simulado)

Las pruebas automáticas no ejercitan el proceso real. Por eso se ensayaron **los mismos guiones** que se usarían en
G1, G2 y G3, sobre el backend real:

- `java -jar`, una sola instancia;
- PostgreSQL de pruebas (`fitsense_ensayo`, `fitsense_ensayo2`);
- usuarios sintéticos creados por la API;
- `fitsense.ai.base-url` apuntando a un Replicate simulado local (127.0.0.1:8899).

El simulado registra cada POST, GET y cancelación en un fichero propio. Cada predicción tarda 40 s y devuelve una
salida que el contrato rechaza, así que cada generación ejercita los 3 intentos y el respaldo.

| Ensayo | Qué se hizo | Resultado |
|---|---|---|
| E1 · normal | POST `/plan/generation` → consulta hasta el estado terminal | COMPLETED (respaldo por RECHAZOS, como se esperaba con salidas inválidas). 3 POST en el simulado = 3 filas SUCCEEDED en el libro. |
| E2a · SIGTERM | Con la fila del intento 1 ENVIADA **y su id guardado**, apagado ordenado y nuevo arranque | **Fallo en `65e1b7e` (HR-1, §3).** Con la corrección: PENDING tras el apagado (`executions` 0), fila ENVIADA, 0 cancelaciones. Tras arrancar: `REPLICATE_REANUDADA` con el mismo id y COMPLETED. 3 POST = 3 filas, 1 plan. |
| E2b · kill -9 | La misma situación, con muerte abrupta | El lease vence (~90 s) y el barrido la recupera («1 huérfana en cola de nuevo»). `REPLICATE_REANUDADA` con el mismo id y COMPLETED. 3 POST = 3 filas, `executions` 2, 0 cancelaciones, 1 plan. |
| E3 · vencimiento + reintento | Con la predicción en curso, `first_started_at` se movió 1.801 s atrás en la base de pruebas | El barrido la cierra en 21 s (FAILED `TIEMPO_EXCEDIDO`, reintentable, sin plan). El worker vivo recibe la salida tardía, la deja SUCCEEDED en el libro y **no envía el intento 2** (fencing). El reintento por la API crea otra operación: `REPLICATE_ADOPTADA` + `REPLICATE_REUTILIZADA` del intento 1 (`reused_from_row_id`), sin POST para él. Los 2 POST nuevos son los intentos 2 y 3. Resultado: 1 plan. |

Recuento del segundo ensayo: 9 POST en el simulado y 10 filas en el libro (9 ids distintos + 1 adoptada con el mismo
id). Ninguna predicción quedó sin identificar ni se duplicó.

## 3. Hallazgos

### HR-1 · El apagado ordenado cancelaba la predicción pagada y publicaba el respaldo (defecto, corregido)

**Reproducción.** En `65e1b7e`, con la predicción del intento 1 en curso y su id en el libro, un SIGTERM provocó lo
siguiente:

1. `PlanGenerationWorker.stop()` interrumpe el hilo, y el sondeo termina como `INTERRUMPIDO`.
2. `seguir()` trata ese desenlace como «dejamos de esperar»:
   - **cancela la predicción en Replicate**, con `REPLICATE_CANCEL`;
   - marca la fila CANCELED;
   - lanza `AiProviderUnavailableException`.
3. El pipeline lo toma por un fallo del proveedor, genera el respaldo del motor de reglas y, como la operación seguía
   siendo de ese worker, **lo publica**.
4. La operación queda COMPLETED con un plan `RULE_ENGINE` en 1 s. `requeueOnShutdown` no llega a actuar.

**Consecuencias en producción.** Cada despliegue o reinicio con una generación en curso habría tenido tres efectos:

- se pagaba una predicción que después se cancelaba;
- el participante recibía el plan de respaldo en vez del de la IA;
- se incumplía la garantía documentada de «una predicción identificada se recupera tras un reinicio».

El kill -9 no tenía el problema: el lease vence y se reanuda.

**Por qué no lo detectaban las pruebas.** Simulaban el reinicio por la base (lease vencido) o con un fallo del
`runner`. Ninguna interrumpía el hilo durante el sondeo real.

**Corrección** (`d1ac33f`, rama `validacion/replicate-real`). Es mínima:

- `ReplicateTrainingPlanGenerator.seguir`: si el hilo se interrumpe con la predicción en curso, **no la cancela ni
  cierra la fila**. La fila queda ENVIADA y se lanza `GeneracionInterrumpidaException`, que no cuenta para el
  cortacircuitos.
- `PlanGenerationOperationService.execute`: la guarda que se comprueba antes de cada predicción y antes de publicar
  también falla si el proceso se está apagando. Un proceso que se apaga no envía ni publica, tampoco el respaldo, y la
  operación vuelve a la cola.

**Prueba determinista.** R21 (`ReanudacionConReplicateSimuladoIT.apagadoOrdenadoConPrediccionEnCurso`):

- **Falla sin la corrección:** «la predicción pagada no se cancela por el apagado: expected 0 but was 1».
- **Pasa con ella**, y comprueba que:
  - no hay cancelaciones ni POST nuevos y no se publica ningún plan;
  - la fila queda ENVIADA y la operación pasa a PENDING;
  - la reanudación termina con 3 POST, no 4.

**Ajuste de R16.** El indicador «se está apagando» se activa ahora en el momento de la muerte simulada. Antes era
verdadero desde el inicio, y la nueva guarda habría adelantado la prueba antes del primer envío. La intención de la
prueba no cambia: la muerte a mitad del commit se deshace entera y la reanudación publica una vez.

**Verificación tras la corrección:**

| Ejecución | Resultado |
|---|---|
| IT asíncronas | 15 + 23 + 12 + 1 = **51/51** |
| Unitarias | **1576, 0 fallos** (1 omitida, opt-in) |
| E2a, E2b y E3 de extremo a extremo con el jar corregido | conformes (tabla del §2) |

### HR-2 · El simulado del ensayo no leía cuerpos *chunked* (defecto del arnés, no del backend)

El primer E1 dio NO_CONFIRMADA. El simulado respondía antes de leer el cuerpo, que el cliente envía *chunked*, y el
cliente informó «Error writing request body».

El backend actuó como manda R-2:

- dio la predicción por NO_CONFIRMADA, ya que Replicate pudo haberla aceptado;
- **no la reenvió**;
- publicó el respaldo.

Se corrigió el simulado y no se tocó el backend. Queda como evidencia adicional del trato conservador de un envío
incierto.

## 4. Decisión

Con HR-1 corregido, verificado y subido (`d1ac33f`), el ensayo cumple. Se autoriza internamente pasar a G1 con el
protocolo congelado.
