# Política de tiempos de la generación asíncrona (V59)

#71 trataba como un solo número tiempos que son distintos. V59 los separa y comprueba su coherencia **al arrancar**
(`PoliticaTiemposGeneracion`): una configuración incoherente con la IA encendida **no arranca**.

## Tabla

| # | Tiempo | Valor por omisión | Propiedad | Por qué este valor | Qué pasa al vencer |
|---|---|---|---|---|---|
| 1 | Respuesta HTTP a Flutter (`POST /plan/generation`) | milisegundos | — | Solo registra la operación; no llama a la IA | — |
| 2 | Arranque de la operación (`PENDING` → `PROCESSING`) | ≤ `poll-ms` (2 s) con worker libre | `fitsense.plan-generation.poll-ms` | Sondeo barato de la cola | — |
| 3 | Espera máxima en cola | 1.800 s | `max-queued-seconds` | Ningún worker la atendió (todas las instancias caídas) | `FAILED / TIEMPO_EXCEDIDO`, reintentable |
| 4 | Petición al proveedor (POST, sin `Prefer: wait`) | conectar 10 s, leer 90 s | `fitsense.ai.connect-timeout-seconds`, `read-timeout-seconds` | Replicate devuelve el id al crear la predicción; el POST es corto | Se clasifica según `IDEMPOTENCIA_PREDICCIONES.md` |
| 5 | Espera de la predicción (sondeo por id) | 150 s + último GET (≤ 41 s) | `fitsense.ai.timeout-seconds`, `poll-read-timeout-seconds` | Medido: un intento válido tarda unos 100 s | Cancelación pedida y estado `TIMEOUT` o `CANCELED` en el libro |
| 6 | Una predicción, cota total | **331 s** | `ReplicateProperties.esperaMaximaConCancelacionSegundos()` | Suma de 4 y 5 más la cancelación (10 + 30) | — |
| 7 | Pipeline, peor caso | **1.113 s** = 3 × 331 + 120 | `PlanGenerationPipeline.AI_ATTEMPTS`, `MARGEN_PIPELINE_S` | 3 intentos de IA más validación, respaldo y publicación (decenas de ms medidos; margen de 120 s) | — |
| 8 | Latido del worker | cada 20 s | `heartbeat-ms` | Hilo propio, nunca el planificador compartido de Spring | — |
| 9 | Lease | 90 s | `lease-seconds` | Al menos dos latidos por lease (se comprueba): un latido perdido por GC o red no basta para declarar muerto al worker | Huérfana: se reencola sin propietario |
| 10 | Operación, tiempo total | **1.800 s desde la primera ejecución** | `max-processing-seconds` | Red de seguridad. Se comprueba `≥ 7` al arrancar. Margen de unos 11 min sobre el peor caso | `FAILED / TIEMPO_EXCEDIDO`, reintentable; lo pagado queda en el libro |
| 11 | Ejecuciones (reinicios) | 3 | `max-executions` | Cada reanudación ya no repaga las predicciones (las lee del libro); 3 cubre dos despliegues seguidos | `FAILED / EJECUCION_INTERRUMPIDA`, reintentable |
| 12 | Recuperación tras reinicio | ≤ lease + barrido ≈ 90 + 60 s | — | El lease vence y el barrido la reencola | — |
| 13 | Ventana de reutilización entre operaciones | 24 h | `reuse-window-hours` | Un reintento del mismo usuario con la misma petición adopta la predicción ya pagada | — |

## Comprobaciones al arrancar

Lanzan `IllegalStateException`, con lo que la aplicación no arranca, en tres casos:

- `heartbeat-ms` ≤ 0 o `lease-seconds` ≤ 0.
- `heartbeat-ms × 2 > lease-seconds × 1000`: una generación viva se tomaría por huérfana.
- IA encendida y `max-processing-seconds < peor caso del pipeline`: se descartarían resultados válidos ya pagados.

Con la IA apagada (motor de reglas, milisegundos) la tercera no aplica. Si alguien sube `fitsense.ai.timeout-seconds`
sin subir el tiempo máximo, el despliegue falla al arrancar en lugar de descartar generaciones en silencio.

## Por qué 1.800 s y no 1.200 s

- 1.200 s apenas supera los 1.113 s. Cualquier ajuste del sondeo o un cuarto intento lo dejaría por debajo, y la
  comprobación lo impediría arrancar.
- 1.800 s coincide con la espera máxima en cola ya aprobada en #71, así que la app tiene una sola cota de 30 min.
- Una generación real dura unos 100–330 s. El límite solo actúa ante un defecto: el proveedor colgado más allá de
  sus propios límites o un worker zombi.
- Se mide desde `first_started_at`, que **no** se reinicia al reencolar. Un bucle de reinicios no puede extender la
  operación sin fin.

## Riesgos, recursos y costes

| Aspecto | Valoración |
|---|---|
| Espera del usuario | El caso normal no cambia (unos 100–330 s). El peor caso con defecto pasa de 10 a 30 min, y nunca es infinito. |
| Recursos | 2 generaciones a la vez por instancia (`max-concurrent`), 2 hilos de planificación propios y ninguna transacción abierta durante la IA. |
| Coste | No añade predicciones: tres como máximo por operación, sin repetir las ya anotadas. |
| Recuperación | Tras un reinicio, en unos 150 s la operación vuelve a la cola y continúa desde el libro. |
| Factibilidad | Todo es configurable por variable de entorno. No toca Railway; el valor por omisión vale tal cual. |
