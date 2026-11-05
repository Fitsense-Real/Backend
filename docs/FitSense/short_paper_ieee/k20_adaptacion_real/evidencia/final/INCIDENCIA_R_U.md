# Incidencia técnica en R_U (registrada antes de pasar el oráculo a R_U)

**Registro:** 2026-11-05T03:20:32Z. Commit `89a1e4f` (árbol `src` `08e69206`).

## Qué pasó

- U13/ciclo1, intento 1: predicción `1enbdbrr65rmw0d1n6tv0p534w`.
  - Replicate la mantuvo en `processing` (creada a las 03:16:20Z, sin completar al consultarla).
  - El backend agotó su espera (175.669 ms) y registró `ERROR_PROVEEDOR`.
- Sin métricas, el control de gasto del arnés (`A2` §2) cargó la reserva de 0,05 USD y bloqueó las llamadas siguientes.
  - U14, U18 y U19 recibieron `PRESUPUESTO_AGOTADO_SIN_LLAMADA` y no consultaron a la IA.
- Consumo de R_U: 5 predicciones (4 medidas + 1 sin métricas) y USD 0,1226 con la reserva.

## Clasificación y decisión

- **Fallo técnico** del proveedor (red o tiempo), que el protocolo prevé (`08` §6).
- **No** se ha mirado el resultado del oráculo de R_U antes de decidir.
- Se **conserva R_U íntegra** y se **repite completa una sola vez** como `R_U_rep`, con la misma versión, los mismos perfiles, el mismo prompt y el mismo modelo.

## Tope de la repetición

- **19 predicciones / USD 1,07**, es decir 24 − 5 y 1,20 − 0,1226.
- Así el total de la evaluación no supera las **144 predicciones / USD 7,20** autorizadas.
- Con 8 ciclos y un intento por ciclo en DEV-R, el tope no se espera que limite.
- Si la repetición vuelve a fallar por causas técnicas, **no** se repite más: se informan las dos.

## Desenlace de la predicción atascada (consultado después)

- `1enbdbrr65rmw0d1n6tv0p534w` terminó en **`failed`** a las 03:26:41Z, tras 621 s de `predict_time`.
- Replicate **no registró tokens** de entrada ni de salida (`metrics` solo con tiempos).
- La contabilidad conservadora del arnés le cargó la reserva de 0,05 USD; el coste real por tokens fue 0.
