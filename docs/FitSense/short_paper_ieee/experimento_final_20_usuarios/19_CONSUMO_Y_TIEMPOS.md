# Consumo y tiempos

**Fuente de los tokens y del tiempo de cómputo:** las métricas de cada predicción en Replicate (`token_input_count`, `token_output_count`, `predict_time`), descargadas en `evidencia/*/predicciones_replicate/`.

**El coste es una estimación** con la tarifa pública de GPT-5 mini (US$ 0,25 por millón de tokens de entrada y US$ 2,00 por millón de salida). **La cifra oficial está en el panel de facturación de Replicate.**

## Consumo total del día (todas las predicciones creadas en la cuenta)

| Intento | Predicciones | Tokens de entrada | Tokens de salida | Coste estimado |
|---|---:|---:|---:|---:|
| 1 (error del laboratorio) | 0 | 0 | 0 | US$ 0 |
| 2 (502 del proxy) | 12 | ~493.000 | ~52.000 | ~US$ 0,23 |
| **3 (válido)** | **35** | **1.289.410** | **220.935** | **US$ 0,76** |
| **Total** | **47** (límite: 90) | 1.782.129 | 273.203 | **~US$ 0,99** (límite: US$ 10) |

Las 47 predicciones se completaron en Replicate (`succeeded`). En el intento 2, 7 respuestas no llegaron al backend, pero se cobran igual.

## Tiempos del intento válido

| Operación | Llamadas | Espera vista por el backend: mín / mediana / máx | `predict_time` mediano (Replicate) | Tokens de entrada / salida | Coste estimado |
|---|---:|---|---:|---|---:|
| Generación inicial | 22 | 62,9 / 97,8 / 139,8 s | 96,3 s | 788.372 / 180.159 | US$ 0,56 |
| Adaptación | 13 | 10,6 / 35,1 / 63,5 s | 32,9 s | 501.038 / 40.776 | US$ 0,21 |

**Duración de la ejecución completa:** unos 40 minutos para los 20 usuarios y los 20 gemelos.

**Lectura:** la generación con IA tarda alrededor de 1,5 minutos por plan y cuesta unos 2,8 centavos de dólar por gemelo. La adaptación tarda alrededor de 35 s y cuesta unos 2,6 centavos por operación elegible.
