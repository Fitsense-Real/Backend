# Rendimiento de la generación con IA

**Fuentes:** `GENERACIONES_REPLICATE.csv` y, en `evidencia/real/`, `TIEMPOS_GENERACION.csv`, `prompts_generacion.log` y `tiempos.log`.

## 1. Medidas (17 llamadas de generación y 3 de adaptación)

| | Generación | Adaptación |
|---|---:|---:|
| Llamadas | 17 | 3 |
| Tiempo por llamada, media / mediana | 96 s / 87 s | 29 s / 30 s |
| Mínimo / máximo | 46 s / 171 s | 27 s / 30 s |
| POST bloqueante (`prefer-wait` = 20 s) | 20,7 s de media | 20,6 s |
| Polling hasta el resultado | 75,6 s de media (cada 3 s) | 8,4 s |
| Tamaño del prompt | 34 000 – 219 000 caracteres | 193 000 – 212 000 |
| Tokens de entrada | 9 400 – 64 000 | 55 000 – 64 000 |
| Tokens de salida | 3 750 – 15 900 | 2 700 – 3 300 |
| Coste | US$ 0,473 | US$ 0,063 |

## 2. Tiempo por usuario: dónde se va

Generación inicial completa, con todos sus intentos.

| Usuario | Total | prepare | generate (IA y respaldo) | validar | normalizar | persist |
|---|---:|---:|---:|---:|---:|---:|
| U01 | 96 s | 0,65 s | 95,5 s | 33 ms | 2 ms | 49 ms |
| U02 | 394 s | 0,07 s | 394,1 s | 15 ms | 1 ms | 81 ms |
| U03 | 260 s | 0,54 s | 259,6 s | 18 ms | 0 | 44 ms |
| U04 | 79 s | 0,68 s | 77,9 s | 12 ms | 0 | 69 ms |
| U05 | 60 s | 0,54 s | 59,2 s | 6 ms | 0 | 90 ms |
| U06 | 462 s | 0,76 s | 460,7 s | 22 ms | 0 | 82 ms |
| U08 | 97 s | 0,70 s | 96,5 s | 16 ms | 0 | 72 ms |

**El trabajo del backend suma menos de 1 s por generación.** Más del 99 % del tiempo es del proveedor: el POST de 20 s más el polling hasta que el modelo termina.

Los usuarios lentos (U02, U06 y U03) lo son porque necesitaron **3 llamadas**, no porque una llamada sea anómala.

## 3. Qué explica la latencia de una llamada

| Factor | Correlación con el polling |
|---|---:|
| Tamaño del prompt (caracteres) | 0,40 |
| **Tokens de salida** | **0,96** |

**El tamaño del prompt no es la causa principal.**
- Los prompts de unos 200 000 caracteres (U01, U08) dieron 46–96 s.
- U02, con 96 000 caracteres, tardó 149 s porque produjo 12 600 tokens de salida.

El tiempo lo marca **cuánto genera el modelo**: la longitud del JSON con razonamiento y notas, más el razonamiento interno de esfuerzo `medium`.

**El caso de 183 s y 209 000 caracteres de la ejecución local** es coherente con esto: U06 hizo 3 llamadas de 137–171 s con unos 210 000 caracteres y 14 000–16 000 tokens de salida.

## 4. Mejoras posibles sin cambiar políticas

Documentadas, **no aplicadas**: primero hay que medir su efecto en la calidad.

| Mejora | Efecto esperado | Riesgo |
|---|---|---|
| Menos reintentos evitables (los rechazos de los §6 de la campaña) | Es lo que más tiempo ahorra: U02 y U06 tardaron 3 veces más por 2 reintentos | Ninguno: la factibilidad previa y el contrato del PR #13 ya van en esa dirección |
| Pedir menos texto de salida (`notes` vacías, `rationale` corto) | Menos tokens de salida, la variable dominante | Bajo. La app muestra el `rationale`, que habría que mantener útil |
| `prefer-wait` más alto (hasta 60 s) | Menos peticiones de polling; la latencia apenas cambia | Muy bajo |
| Prompts por objetivo sin catálogo redundante (las 219 000 caracteres de U06 son sobre todo candidatos) | Menos tokens de entrada (coste) y algo de latencia | Medio: la IA necesita los candidatos; recortarlos exige pruebas |
| Esfuerzo de razonamiento `low` | Menos latencia | **Alto:** puede bajar la tasa de propuestas válidas. Cambiar el modelo o su configuración no está autorizado |

**Rendimiento después de corrección y seguridad:** el respaldo determinista sigue garantizando un plan en menos de 1 s cuando la IA falla.
