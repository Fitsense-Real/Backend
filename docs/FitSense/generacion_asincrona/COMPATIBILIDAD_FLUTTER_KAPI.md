# Compatibilidad con Flutter, el recorrido de Kapi y Railway (V59)

## Flutter y Kapi

El contrato HTTP no cambia (ver `CONTRATO_API_ACTUALIZADO.md`): las mismas rutas, códigos, campos y estados. El
cliente de las PR Fitsense-Real/MobileApp#1 y #2, que guarda el `operationId`, consulta con `pollAfterSeconds` y
recupera con `/latest`, sigue valiendo sin cambios.

Esas PR están en otro repositorio, fuera del alcance de esta sesión. La compatibilidad se comprueba **contra el
contrato** con `MockMvc` y la seguridad real (E1/E2), no ejecutando la app.

| Requisito | Cómo queda |
|---|---|
| No reiniciar la generación al terminar el recorrido | La generación vive en el servidor. Acabar el recorrido no hace ninguna petición nueva, y un `POST` repetido devuelve la misma operación (200). |
| No reiniciarla al reabrir la app | `GET /latest` devuelve la misma operación y el mismo `planId` (`appCerradaYReabierta`). |
| No perder el `operationId` | Aunque Flutter lo pierda, `/latest` lo recupera. |
| No mostrar una rutina inexistente | `COMPLETED` siempre trae `planId` y solo se emite tras `persist`. Sin lease no se publica. |
| No mostrar porcentajes ni tiempos exactos falsos | El recurso no los tiene (`enCursoSeVeProcessing`). |
| No esperar para siempre | Cota de 30 min en cola y 30 min en proceso desde la primera ejecución, y 3 ejecuciones como máximo. |
| Perder la conexión no es un fallo definitivo | Un error de red del cliente no cambia nada en el servidor. Solo `FAILED` o `NO_FACTIBLE` son terminales. |
| Fallo definitivo frente a reintentable | `terminal=true` con `retryable=true` (`ERROR_TECNICO`, `EJECUCION_INTERRUMPIDA`, `TIEMPO_EXCEDIDO`) ofrece reintentar. `retryable=false` (`PERFIL_INCOMPLETO`, `NO_GENERABLE`, `NO_FACTIBLE`) no. |
| El recorrido termina antes que la rutina | La app sigue mostrando «preparando tu rutina» mientras el estado sea `PENDING`/`PROCESSING`. No se ha cambiado el diseño de Kapi. |

Lo que sí cambia para el usuario:

- Una generación lenta pero viva (600–1.113 s) ya **no** termina en `FAILED` a los 10 min.
- Un reintento tras `FAILED` puede ser casi inmediato, porque adopta las predicciones ya pagadas.

## Railway

No se ha tocado Railway. Se comprueba lo siguiente sobre el código:

| Requisito | Cómo se cumple |
|---|---|
| Reinicios de proceso y de contenedor | El estado está solo en PostgreSQL (operación, lease y libro). Con SIGTERM las operaciones vuelven a la cola; con SIGKILL vence el lease (90 s) y el barrido las recupera. |
| Varias instancias | `SKIP LOCKED` y un lease con propietario por proceso (`hostname-uuid8`). Fencing al publicar, cerrar, latir y enviar. El apagado solo toca lo propio. |
| Recursos limitados | `max-concurrent=2` por instancia, un hilo de ciclo y uno de latido, y el pool propio del worker. Nada crece sin límite. |
| Sin transacciones abiertas durante la IA | El pipeline sigue con `prepare` y `persist` en transacciones cortas. El libro usa autocommit por sentencia. |
| Sin estado solo en memoria | El libro sustituye a lo que antes vivía solo en el hilo (el id de la predicción). El límite de 5 por hora (R-5) sigue en memoria por instancia, igual que antes, y lo documenta. |
| Configuración | Todo tiene un valor por omisión válido. Las variables opcionales están documentadas en `application.properties`. Una incoherente impide arrancar. |

El número real de réplicas en Railway no se ha consultado (no se toca Railway). El diseño no depende de que haya
una sola.
