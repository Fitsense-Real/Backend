# Diagnóstico del límite de 600 s en la generación asíncrona (PR #71, V58)

Auditoría del código de #71 en su cabeza `12a01c6` (rama `claude/async-first-plan-generation`), con pruebas
reproducibles sin Replicate. La corrección está en la rama dependiente `fix/async-generacion-resiliente` (V59).

## 1. Dónde está el límite y qué significa

| Elemento | En #71 |
|---|---|
| Propiedad | `fitsense.plan-generation.max-processing-seconds=600` |
| Dónde se aplica | `PlanGenerationOperationService.recoverStale()`, que el worker de #71 ejecutaba en cada ciclo (cada 2 s) |
| Desde cuándo se mide | `started_at` de la ejecución **actual**, que se reinicia en cada reencolado |
| Qué hace | `PROCESSING` con `started_at < now - 600 s` pasa a `FAILED / TIEMPO_EXCEDIDO` (reintentable) |
| Qué pretendía | Una red de seguridad para que la app no esperase para siempre |
| Justificación declarada | «Mayor que el peor caso del pipeline con IA, 150 s por intento» |

Esa justificación **no se cumple**. Una predicción no está acotada por los 150 s del sondeo, sino por toda su
cadena HTTP (`ReplicateProperties.esperaMaximaConCancelacionSegundos()`):

```
conectar 10 + leer POST 90 + sondeo 150 + pausa 1 + conectar 10 + leer GET 30 + cancelar (10 + 30) = 331 s
```

El pipeline hace hasta 3 intentos de IA (`PlanGenerationPipeline.AI_ATTEMPTS`) más la validación, el respaldo y la
publicación:

```
3 × 331 + 120 = 1.113 s  >  600 s
```

`fitsense.ai.generate.total-budget-seconds` está en 0 (sin presupuesto total), así que nada acota los tres intentos
por debajo de esa cifra.

## 2. Qué pasaba al superar los 600 s

Prueba D1 (temporal `DiagnosticoV58IT`, PostgreSQL efímera, pipeline bloqueado a mitad):

```
D1 tras barrido:              FAILED TIEMPO_EXCEDIDO exec=1
D1 al terminar el pipeline:   FAILED TIEMPO_EXCEDIDO exec=1 planes=0
```

| Pregunta | Respuesta en #71 |
|---|---|
| La predicción de Replicate | **Sigue** corriendo y cobrándose. Nada la cancela y nada guarda su id. |
| La operación | `FAILED / TIEMPO_EXCEDIDO`, aunque la generación esté viva y vaya a terminar bien. |
| El worker | Sigue ejecutando el pipeline hasta el final; el hilo no se interrumpe. |
| El resultado | Se calcula, se valida y **se descarta** antes de `persist`, porque la operación ya no está en `PROCESSING`. |
| Flutter | Ve `FAILED` reintentable. Si reintenta, la nueva operación **paga otra vez** todas las predicciones. |

Es una pérdida silenciosa de un resultado válido ya pagado.

## 3. Defecto independiente: el latido se quedaba sin hilo

`PlanGenerationWorker` latía con `@Scheduled`. Spring Boot usa por omisión **un solo hilo** para todas las tareas
`@Scheduled`, y no hay configuración `spring.task.scheduling.*`. Ese hilo lo comparten:

- `WeeklyCycleScheduler.runHourly()` (cron horario). El lunes, a la hora de cierre, adapta a cada usuario con
  llamadas síncronas a la IA. Son minutos de hilo ocupado.
- `OverdueWorkoutScheduler`.
- `StaleSessionScheduler`.

Mientras ese hilo está ocupado, no late nadie. A los 180 s sin `updated_at`, el barrido toma por **huérfana** una
generación viva y la reencola. Prueba D2:

```
D2 tras barrido:     PENDING - exec=1
D2 reclamada otra vez: PROCESSING - exec=2
D2 ejecuciones del pipeline (cada una pagaría sus predicciones): 2 estado=COMPLETED planes=1
```

El plan no se duplica, porque lo protege el plan único y `PlanAlreadyExistsException`, pero **las predicciones se
pagan dos veces**.

## 4. Reinicio del backend

En #71, cuando muere el proceso:

1. La operación deja de latir.
2. Se reencola (una vez).
3. El pipeline empieza **de cero**.

Las predicciones que el proceso muerto había lanzado siguen en Replicate. Nadie conoce su id, nadie las lee y se
facturan. La nueva ejecución pide todas otra vez. Con dos interrupciones, la operación acaba en
`FAILED / EJECUCION_INTERRUMPIDA` habiendo pagado hasta 2 × 3 predicciones sin publicar nada.

## 5. Predicciones duplicadas posibles en #71

| Causa | Duplicados por evento |
|---|---|
| Reinicio o caída durante la generación | hasta 3 |
| Falso huérfano por latido sin hilo | hasta 3 |
| Reintento manual tras `TIEMPO_EXCEDIDO` con la generación viva | hasta 3 |
| POST con `Prefer: wait` cortado antes de recibir el id (hasta 60 s de ventana) | 1, sin seguimiento |

## 6. Qué no está mal en #71 y se conserva

- La operación persistente con un índice único parcial: una sola `PENDING`/`PROCESSING` por usuario.
- El reclamo con `FOR UPDATE SKIP LOCKED`.
- El pipeline idéntico al síncrono, sin transacciones abiertas durante la IA.
- La comprobación antes de `persist`.
- El reencolado en el apagado ordenado.
- La clasificación de fallos.
- El contrato HTTP.

## 7. Conclusión

Subir el límite a 1.200 s no corrige nada de lo anterior. El latido sin hilo y el reinicio seguirían duplicando
predicciones, y una configuración distinta de la IA volvería a dejar el límite por debajo del peor caso. La
corrección (ver `VALIDACION_TIMEOUTS.md`, `VALIDACION_LEASE_HEARTBEAT.md` y `RECUPERACION_PREDICCIONES.md`):

- separa los tiempos;
- da al worker hilos propios y un lease con propietario;
- anota cada predicción antes de enviarla y la reanuda por su id;
- comprueba al arrancar que el tiempo máximo de la operación no queda por debajo del peor caso.
