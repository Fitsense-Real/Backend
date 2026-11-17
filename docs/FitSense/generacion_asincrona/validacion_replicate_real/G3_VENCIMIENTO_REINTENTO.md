# G3 · Vencimiento y reintento controlado con Replicate real

**Resultado: NO EJECUTADO con Replicate real (bloqueado). Validado en el ensayo sin coste.**

## 1. Por qué no se ejecutó

G3 fuerza el vencimiento **con una predicción en curso**, para comprobar dos cosas:

- que la salida tardía se queda en el libro;
- que el reintento la adopta sin pagar otra.

Sin predicciones que lleguen a estar en curso, el reintento solo podría «adoptar» un FAILED. No probaría la
reutilización de un resultado pagado y gastaría otra predicción.

## 2. Procedimiento validado sin coste

Se hizo con el backend real y el jar `d1ac33f`, contra el Replicate simulado:

1. Con la fila del intento 1 ENVIADA, se movió `first_started_at` 1.801 s atrás en la base de pruebas. No se bajó
   ningún tiempo de la configuración.
2. El barrido (cada 30 s) la cerró a los 21 s como **FAILED `TIEMPO_EXCEDIDO`, reintentable**, sin plan. Es el estado
   terminal previsto por la política.
3. El worker vivo recibió la salida tardía y la dejó **SUCCEEDED** en el libro. El fencing le impidió enviar el
   intento 2 y publicar.
4. El reintento por la API (`POST /plan/generation`) creó otra operación:
   - `REPLICATE_ADOPTADA` y `REPLICATE_REUTILIZADA` del intento 1, con `reused_from_row_id` y **sin POST** para él;
   - los únicos POST fueron los intentos 2 y 3, porque la salida simulada no pasa los validadores;
   - 1 plan.

Con una salida real válida en el intento 1, el reintento habría publicado **sin ninguna predicción nueva**.

## 3. Pendiente

Ejecutarlo con Replicate real cuando el proveedor responda. Coste esperado: 1 predicción, más 0 en el reintento si la
salida es válida.
