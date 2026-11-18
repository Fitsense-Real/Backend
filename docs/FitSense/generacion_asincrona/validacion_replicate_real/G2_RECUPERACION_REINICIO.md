# G2 · Recuperación tras reinicio con Replicate real

**Resultado: NO EJECUTADO con Replicate real (bloqueado). Validado en el ensayo sin coste tras corregir el defecto
HR-1.**

## 1. Por qué no se ejecutó

G2 necesita una predicción **aceptada y en curso** con su id ya guardado, para reiniciar mientras Replicate la procesa.
Con el proveedor fallando en 0,3 s (G1 y las sondas), ninguna predicción llega a estar en curso. Reiniciar así no
prueba nada y gasta.

Se aplicó la regla «no reiniciar antes de que el id esté guardado».

## 2. Lo que se validó sin coste (backend real + Replicate simulado)

Detalle en `ENSAYO_SIN_COSTO.md`. En los dos casos el id ya estaba guardado y la predicción seguía en curso.

| Caso | Resultado |
|---|---|
| **SIGTERM** (apagado ordenado, como en un redeploy), código base `65e1b7e` | **Defecto HR-1:** cancela en Replicate la predicción pagada y publica el plan del motor de reglas |
| SIGTERM, con la corrección `d1ac33f` | Vuelve a la cola (PENDING, `executions` 0), la fila sigue ENVIADA y hay 0 cancelaciones. Tras arrancar: `REPLICATE_REANUDADA` con el mismo id y COMPLETED. 3 POST para 3 filas, 1 plan. |
| **kill -9** | El lease vence (~90 s) y el barrido la recupera como huérfana. `REPLICATE_REANUDADA` con el mismo id y COMPLETED. 3 POST para 3 filas, 0 cancelaciones, 1 plan. |

Garantías comprobadas: ningún POST nuevo para la predicción en curso, ningún plan duplicado, la operación no se pierde
y los estados son coherentes.

## 3. Pendiente

Repetir G2 con Replicate real cuando el proveedor responda, con el jar de `d1ac33f` o posterior: SIGTERM con la
predicción en curso y comprobar que Replicate la termina y que el backend la recoge por su id. Coste esperado: el de
una generación (1–3 predicciones).

## Reanudación del 18/11/2026

**No ejecutado.** Se fusionó Fitsense-Real/Backend#76 (`a1ccf53`, CI verde) y el ensayo sin coste se repitió con
éxito sobre ese commit. Después, la sonda mínima de la fase 5 (`e23edtt1h9rmr0d1xh09hsd94c`) volvió a fallar con E002.
Como manda la orden, la campaña se detuvo antes de este escenario. 0 predicciones de este escenario.
