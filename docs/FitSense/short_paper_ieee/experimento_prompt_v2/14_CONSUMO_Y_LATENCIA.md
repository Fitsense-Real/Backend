# Consumo y latencia

**Fuentes:**
- `evidencia/<corrida>/predicciones_replicate/indice.json`: métricas de Replicate por predicción (`token_input_count`, `token_output_count` y `predict_time`), descargadas después con GET gratuitos;
- `REPLICATE_TIMING` en `ejecucion_maven.log`: tiempo de extremo a extremo del backend.

**Coste:** se calcula con el precio público de GPT-5 mini: US$ 0,25 por millón de *tokens* de entrada y US$ 2,00 por millón de salida.

## Por corrida

| Corrida | Predicciones | Rechazadas | *Tokens* de entrada | *Tokens* de salida | Coste (US$) | Tiempo de proveedor, suma (s) | Mediana por llamada (s) | Máximo (s) | `predict_time`, mediana (s) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| C1 r1 | 15 | 7 | 521.982 | 46.022 | 0,222 | 408,8 | 31,7 | 44,4 | 31,0 |
| C2 r1 | 8 | 0 | 291.118 | 24.181 | 0,121 | 209,1 | 28,6 | 35,2 | 26,5 |
| C2 r2 | 8 | 0 | 291.118 | 23.223 | 0,119 | 213,3 | 25,7 | 50,6 | 24,7 |
| C1 r2 | 15 | 8 | 488.931 | 54.395 | 0,231 | 481,4 | 34,9 | 50,8 | 32,3 |
| **Total** | **46** | **15** | **1.593.149** | **147.821** | **0,694** | 1.312,6 | — | — | — |

| Condición (media de 2 corridas) | Llamadas | Coste (US$) | Tiempo de proveedor (s) |
|---|---:|---:|---:|
| C1 | 15,0 | 0,227 | 445,1 |
| C2 | 8,0 | 0,120 | 211,2 |

## Presupuesto

| | Límite | Consumido |
|---|---:|---:|
| Predicciones | 120 | **46** (38 %) |
| Coste estimado (US$) | 5,00 | **0,69** (14 %) |

**Otros datos:**
- 0 predicciones perdidas: las 46 están en el registro y en la ventana de cada corrida, y todas en estado `succeeded`.
- No se alcanzó ninguna guarda de presupuesto.
- No hubo reintentos del laboratorio.

## Lectura

- **C2 usa la mitad de llamadas, coste y tiempo de proveedor.**
  - No es por un prompt más corto: C2 es entre un 0,7 % y un 12 % más largo (`05`).
  - Es porque **resolvió cada caso en el primer intento**: 8 de 8 en las dos corridas, frente a 15 llamadas con 7–8 rechazos en C1.
  - La mediana de *tokens* de entrada por llamada es mayor en C2 (39.207 frente a unos 23.200) porque C1 reparte más llamadas entre los casos pequeños con reintentos.
- **Por llamada, la latencia es parecida:** mediana de 26–29 s en C2 frente a 32–35 s en C1.
- **Determinismo de la entrada:** los *tokens* de entrada de C2 r1 y C2 r2 coinciden exactamente (291.118), porque los prompts son idénticos. La salida varía (24.181 frente a 23.223): **el modelo no es determinista.**
