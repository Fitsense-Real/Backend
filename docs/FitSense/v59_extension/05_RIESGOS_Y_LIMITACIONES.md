# 05 · Riesgos y limitaciones

1. **Predicciones no confirmadas.** Si Replicate recibió el POST y el id no llegó (ni entero ni al principio de un cuerpo cortado, PR #88), la fila queda `NO_CONFIRMADA`.
   - **No se reenvía, no se busca por contenido** y se publica el respaldo.
   - Esa predicción **puede haberse ejecutado y cobrado**: sin id no se puede seguir ni cancelar.
   - V66 evita el duplicado, no el coste de la primera. Con `Cancel-After` se acotaría en el servidor (`06`).

2. **La vía síncrona solo se reanuda si la persona repite la petición** (`02` §7).
   - Si no repite, la operación se cierra a los 30 min (`TIEMPO_EXCEDIDO`).
   - Su libro queda, y una petición posterior reutiliza una predicción ya terminada o en curso: misma persona, misma huella, menos de 24 h.
   - Lo que **no** reutiliza es una `NO_CONFIRMADA` de una operación ya cerrada. Una petición explícita nueva, pasada la ventana, puede pagar otra predicción. Es una petición nueva de la persona, no un reenvío automático.

3. **El ciclo semanal se relanza durante 24 h** (`weekly-recovery-hours`). Después, `TIEMPO_EXCEDIDO` y la semana queda como hoy cuando un ciclo falla: sin plan nuevo, con adherencia `NULL`. No se publica una semana tarde.

4. **Adopciones limitadas** (`max-executions` = 3). Tres caídas seguidas en la misma operación la cierran como `EJECUCION_INTERRUMPIDA`.

5. **Publicación del ciclo sin fencing propio.** El ciclo publica con `publishAdapted` y `persist`, que no comprueban el propietario de la operación.
   - Un zombi del ciclo que siga vivo no **envía** predicciones: el libro comprueba el fencing antes de cada una.
   - Lo que impide que **publique dos veces** es la idempotencia de siempre: «ciclo repetido», y `publishAdapted`, que bloquea la medición. No lo impide el fencing.
   - Está probado en V59-02/09 y V59-03b.

6. **Contexto cambiado durante la caída.** Si lo que se pide a la IA cambió, la predicción antigua queda `DESCARTADA` y no se aplica; se pide otra con el estado actual. Eso **es** una predicción nueva, porque la petición es distinta (V59-10).

7. **Datos expirados en Replicate.** La entrada y la salida de las predicciones de la API se borran (`data_removed`).
   - Una `ENVIADA` reanudada mucho después puede no tener salida: se tratará como fallo del proveedor y se publicará el respaldo.
   - Una `SUCCEEDED` ya tiene su salida en el libro, así que no depende de Replicate.

8. **Reinicio simulado.** El «reinicio» de las pruebas es el estado que deja un proceso muerto: lease vencido, otro propietario y la conexión del bloqueo cerrada con `pg_terminate_backend`.
   - Hay **un** reinicio real: un segundo contexto de Spring, otro proceso lógico con memoria vacía (V59-03b).
   - No se mató la JVM: la JVM de las pruebas es la misma.

9. **Sin pruebas con Replicate real.** El transporte se prueba contra un Replicate falso con el cliente de producción. El comportamiento real de los cortes ya se analizó en PR #88.

10. **Recuperaciones imposibles.** No se pueden recuperar:
    - una predicción creada sin id;
    - una huérfana con las adopciones agotadas;
    - un ciclo fuera de su ventana;
    - una predicción cuyos datos borró Replicate antes de leerlos.

    En todas, el resultado es **incierto y seguro**: no se reenvía, se publica el respaldo y queda registrado.
