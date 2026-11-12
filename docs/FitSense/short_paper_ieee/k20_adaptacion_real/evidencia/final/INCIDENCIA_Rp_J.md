# Incidencia técnica en Rp_J (registrada antes de pasar el oráculo a Rp_J)

**Registro:** 2026-11-05T03:39:15Z. Commit `89a1e4f` (árbol `src` `08e69206`).

## Qué pasó

- J08/ciclo1, intento 1: predicción `hzc2287xj5rmw0d1n74agyp30g`.
  - Replicate la terminó en **`canceled`** a los 4,8 s, **sin tokens registrados**.
  - El backend registró `ERROR_PROVEEDOR` (5.423 ms).
- Sin métricas de tokens, el control de gasto del arnés cargó la reserva y **bloqueó todas las llamadas siguientes**.
  - Se bloqueó también el **reintento propio del sistema** (hasta 3 intentos por ciclo): J08 intento 2, J09 y siguientes no consultaron a la IA.
- Consumo de Rp_J: 7 predicciones (6 medidas + 1 sin tokens) y USD 0,1251 con la reserva; el coste real de la cancelada es 0.

## Clasificación y decisión

- **Fallo técnico del proveedor**, agravado por la regla de bloqueo del arnés, más estricta que lo autorizado: «detener si una predicción **supera la reserva**».
- La corrida no refleja el comportamiento del sistema congelado.
- **No** se ha mirado el oráculo de Rp_J antes de decidir.
- Según `08` §6: se **conserva Rp_J** y se **repite completa una sola vez** como `Rp_J_rep`, sin cambiar nada.
  - Tope: **41 predicciones / USD 2,27**, es decir 48 − 7 y 2,40 − 0,1251.
  - Con ese tope, la suma de todos los topes sigue siendo 144 predicciones / USD 7,20.
- Si la repetición vuelve a fallar por causas técnicas, no se repite más: se informan las dos.
