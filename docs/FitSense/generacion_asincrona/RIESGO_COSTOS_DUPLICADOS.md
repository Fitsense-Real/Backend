# Riesgo de costes duplicados y contabilidad (V59)

No se usó Replicate real. Las cifras de predicciones vienen de pruebas con un Replicate simulado que cuenta los POST,
que son las predicciones que se facturarían.

## Antes y después

| Evento | #71 (predicciones pagadas) | V59 (predicciones pagadas) | Prueba |
|---|---|---|---|
| Generación normal (3 intentos rechazados y respaldo) | 3 | 3 | `caminoNormal` |
| El proceso muere antes de publicar y se reanuda | 6 | **3** | `reanudacionNoPagaOtraVez` |
| Dos reinicios seguidos | 6 y `FAILED` sin plan (máximo 2 ejecuciones) | **3** y plan publicado (máximo 3 ejecuciones) | `dosReiniciosSiguenSiendoTres` |
| Falso huérfano por latido sin hilo (D2) | 6 | 3 (no hay falso huérfano: hilo propio y lease) | `latidoAtrasadoConLeaseVigenteNoReencola` (E9) |
| Vence por tiempo con la generación viva y el usuario reintenta | 6 | **3** (adopción, ventana de 24 h) | `reintentoManualAdopta`, E8 |
| Replicate acepta y el id no llega, y luego se reinicia | hasta 4 (1 sin seguimiento más hasta 3 al reanudar) | **1** (`NO_CONFIRMADA`, sin reenvío) | `aceptadaSinIdNoSeReenvia`, E17 |

## Contabilidad por operación

Cada ejecución deja una línea de log, siempre (bloque `finally`):

```
GENERACION_ASINCRONA_PREDICCIONES operacion=<uuid> iniciadas=3 reutilizadas=0 completadas=3 fallidas=0
  canceladas=0 vencidas=0 no_confirmadas=0 descartadas=0
```

Cada evento de reanudación tiene su propia línea:

- `REPLICATE_REUTILIZADA`
- `REPLICATE_REANUDADA`
- `REPLICATE_ADOPTADA`
- `REPLICATE_REPETIDA`
- `REPLICATE_NO_CONFIRMADA`, con nivel WARN

Además se mantiene `REPLICATE_TIMING`.

Consulta de costes (solo lectura):

```sql
SELECT o.operation_id, o.status, o.executions,
       count(*) FILTER (WHERE p.reused_from_row_id IS NULL AND p.status <> 'ERROR_ENVIO') AS posibles_cobros,
       count(*) FILTER (WHERE p.reused_from_row_id IS NOT NULL)                           AS reutilizadas,
       count(*) FILTER (WHERE p.status = 'SUCCEEDED')                                     AS completadas,
       count(*) FILTER (WHERE p.status IN ('FAILED','ERROR_ENVIO'))                       AS fallidas,
       count(*) FILTER (WHERE p.status = 'CANCELED')                                      AS canceladas,
       count(*) FILTER (WHERE p.status = 'TIMEOUT')                                       AS vencidas,
       count(*) FILTER (WHERE p.status = 'NO_CONFIRMADA')                                 AS posibles_duplicados,
       sum(p.polls)                                                                       AS consultas,
       extract(epoch FROM (o.finished_at - coalesce(o.first_started_at, o.created_at)))   AS segundos_totales
  FROM plan_generation_operations o
  LEFT JOIN plan_generation_predictions p ON p.operation_id = o.operation_id
 GROUP BY o.operation_id
 ORDER BY o.created_at DESC;
```

## Cancelación ≠ gratis

`CANCELED` registra que se **pidió** cancelar y lo que Replicate respondió. No se asume que no se cobre:

- una predicción cancelada pudo consumir tiempo de cómputo;
- una cancelación que falla deja la predicción en `TIMEOUT` con su id para conciliarla.

En la consulta, toda fila nueva que no sea `ERROR_ENVIO` cuenta como posible cobro.

## Topes que siguen vigentes

- Como máximo 3 intentos de IA por ejecución, y las reanudaciones no añaden predicciones anotadas.
- Como máximo 3 ejecuciones por operación (`max-executions`).
- Como máximo 5 generaciones por persona y hora (R-5, en memoria y por instancia).
- Ningún reintento ilimitado: un `NO_CONFIRMADA` nunca se reenvía y un fallo repetido se repite sin llamar.
- El tope de gasto real de la cuenta solo puede fijarse en Replicate.
