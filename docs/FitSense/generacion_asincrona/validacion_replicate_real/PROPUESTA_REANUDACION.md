# Propuesta para reanudar G1, G2 y G3

Se aplica cuando el proveedor vuelva a funcionar. **Nada de esto se ejecuta sin una nueva autorización.**

## 0. Condiciones previas (sin coste)

| # | Condición | Quién |
|---|---|---|
| C1 | Se identificó y se resolvió la causa del E002: el panel (Billing) o la respuesta de soporte (`SOLICITUD_SOPORTE_REPLICATE.md`) | Usuario |
| C2 | Se confirmó en el panel el cargo real de las 4 predicciones fallidas (esperado: USD 0,00) | Usuario |
| C3 | Se sabe si la cuenta tiene un límite de gasto y cuál es | Usuario |
| C4 | Fitsense-Real/Backend#76 (HR-1) aprobada, o al menos la campaña corre sobre su cabeza, para que G2 use la corrección | Usuario |
| C5 | `GET /v1/models/openai/gpt-5-structured`: la versión servida es `81cb6179…` u otra. Si cambió, se anota en el protocolo antes de gastar. | Claude |

## 1. Puerta de entrada: una sola sonda mínima

Es la misma sonda del diagnóstico: `gpt-5-mini`, razonamiento `minimal`, prompt de una línea y esquema de una
propiedad. Coste < USD 0,001.

- **`succeeded`:** se pasa a G1.
- **`failed`:** se detiene todo y se vuelve a soporte. No hay más sondas ese día.

## 2. Escenarios

El protocolo es el mismo de `PROTOCOLO_CONGELADO.md`:

- el mismo modelo, variante, prompt y parámetros;
- base de pruebas nueva;
- usuarios sintéticos nuevos;
- una sola instancia.

El commit ejecutado es la cabeza de #76, o `generation` si ya se fusionó.

| Paso | Procedimiento | Predicciones (peor caso) | Coste esperado | Criterio de aceptación |
|---|---|---|---|---|
| G1 | `POST /plan/generation` y espera hasta el estado terminal | 1–3 | USD 0,02–0,10 | Plan `AI` publicado. Libro = Replicate. `/plan/current` devuelve el plan. |
| G2 | Nuevo usuario. En cuanto la fila del intento 1 está ENVIADA **con id**, SIGTERM y nuevo arranque | 1–3 (0 extra por el reinicio) | USD 0,02–0,10 | Tras arrancar, `REPLICATE_REANUDADA` con el mismo id. 0 cancelaciones. 0 POST extra para ese intento. 1 plan. |
| G3 | Nuevo usuario. Con la predicción en curso, `first_started_at` −1.801 s en la base de pruebas → barrido → reintento por la API | 1 (+0 si la salida es válida) | USD 0,02–0,03 | FAILED `TIEMPO_EXCEDIDO` sin publicar. La salida tardía queda SUCCEEDED en el libro. El reintento la adopta (`REPLICATE_ADOPTADA`, `reused_from_row_id`) sin POST. 1 plan `AI`. |

**Total previsto: 3–7 predicciones y USD 0,06–0,25.** El tope operativo sigue en 12 predicciones o USD 1,50.

Opcional, solo si sobra margen y lo autorizas: G2 con **kill -9** en lugar de SIGTERM. Ya se validó sin coste, así que
no es necesario.

## 3. Reglas de parada (las mismas)

Se detiene ante cualquiera de estas situaciones:

- un E002 u otro fallo del proveedor;
- una duplicación;
- una predicción sin identificar;
- una NO_CONFIRMADA inesperada;
- un plan inválido;
- un resultado inesperado.

Ningún escenario superado se repite. Si G1 falla por rechazos de validadores en los 3 intentos, es un resultado válido
de la IA, no del flujo asíncrono: se documenta y G1 se repite como mucho una vez.

## 4. Conciliación tras cada paso

`GET /v1/predictions` (las nuevas) frente a `plan_generation_predictions`, con los tokens y el coste de cada predicción
en `PREDICCIONES_Y_COSTOS.csv`. Al final, contraste con el cargo del panel.

## 5. Entregables al terminar

Se actualizan `G1_*`, `G2_*`, `G3_*`, `PREDICCIONES_Y_COSTOS.csv`, `CONCILIACION_REPLICATE.md`,
`RESULTADOS_VALIDADORES.md`, `RIESGOS_RESIDUALES.md` y `CONCLUSION_VALIDACION_REAL.md` en la rama
`validacion/replicate-real`, sin fusionar.
