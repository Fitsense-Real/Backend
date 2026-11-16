# Decisión: preparación para una prueba controlada con IA real

## Estado

Las garantías de la generación asíncrona están demostradas **con simulación determinista**: un Replicate falso
local, sin red y sin créditos. No se ha ejecutado nada contra Replicate real, y la decisión de hacerlo es del equipo.

| Garantía | ¿Demostrada sin IA real? | Qué falta confirmar con IA real |
|---|---|---|
| No se pierde una generación válida larga | Sí (R01, R18) | Latencias reales del mismo orden que las medidas (75–350 s) |
| No se repiten predicciones al reanudar | Sí (R05, R06, R16, R17) | Que `GET /v1/predictions/{id}` devuelva la salida tras un reinicio real |
| Sin reenvío ante la incertidumbre | Sí (R07) | — (no se provoca a propósito con IA real) |
| Una sola rutina publicada | Sí (R08, R09, R16, R19) | — |
| El latido no se bloquea | Sí (R02, escalado) | Que en Railway no haya reencolados falsos con 20 s / 90 s |
| Compatibilidad con Flutter | Sí, contra el código de MobileApp#2 y con `MockMvc` | Un recorrido de la app contra un backend de pruebas |

## Recomendación

La generación asíncrona está **lista para una prueba controlada con IA real**, una vez aprobadas las PR para su
revisión. Sin merge ni despliegue hasta esa decisión. Propuesta, pendiente de autorización y presupuesto:

1. **Entorno aislado:** base de pruebas nueva y backend local o de staging. **Nunca** la base real.
2. **Tres escenarios** con un usuario de prueba cada uno (unas 3–9 predicciones en total):
   - G1: una generación normal (medir la latencia real y comprobar 1–3 predicciones en el libro);
   - G2: un reinicio provocado con la predicción `ENVIADA` (comprobar que se reanuda por id sin otro POST);
   - G3: un vencimiento forzado y un reintento (comprobar la adopción).
3. **Criterios de aceptación:**
   - `iniciadas` en el libro igual al número de predicciones en el panel de Replicate;
   - 0 planes duplicados;
   - 0 `NO_CONFIRMADA` en G1 y G2;
   - `COMPLETED` en todas.
4. **Requisitos del entorno:** acceso de red a `api.replicate.com` (hoy bloqueado en esta sesión) y un token con un
   tope de gasto fijado **en la cuenta de Replicate**.

Me detengo aquí: no paso al Paso 2 del backend ni uso Replicate real sin su autorización.
