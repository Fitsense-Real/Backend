# 01 · Diagnóstico de la latencia de generación

Cada cifra dice de dónde sale:
- **REAL**: trazas de Replicate de campañas anteriores, guardadas en el repositorio.
- **LOCAL**: medición en este contenedor con PostgreSQL de Testcontainers.
- **SIMULADO**: cálculo o prueba con reloj falso a partir de tiempos reales.

No se hizo ninguna llamada nueva a Replicate. Las cifras están en `evidencia/LATENCIAS_REALES_REPLICATE.txt` y `evidencia/TIEMPOS_FITSENSE_LOCAL.txt`.

## 1. Fuentes de datos

| Fuente | Qué es | Representatividad |
|---|---|---|
| **GRA** (`evaluacion_integral_O/generacion_ia_real/entregables`) | 56 predicciones de generación y 20 de adaptación, gpt-5-mini, **PR-1.7**, `reasoning_effort=medium` | **La configuración activada hoy.** Fuente principal |
| Campaña de estrés (`campana_estres`) | 17 predicciones de generación con desglose POST / sondeo / número de consultas | Prompt anterior a PR-1.7. Solo se usa para el comportamiento del POST y del sondeo |
| JSON de predicciones (estrés + MVP) | `created_at` / `started_at` / `completed_at` | Para medir la cola de Replicate |
| `TiemposGeneracionIT` (opt-in) | Etapas de FitSense con IA apagada: cuatro objetivos + H-G9, 15 repeticiones | LOCAL: depende de la máquina |

## 2. Etapas: FitSense frente a Replicate

| # | Etapa | Quién | Tiempo | Origen |
|---|---|---|---|---|
| 1 | Recepción de la solicitud (JWT, bloqueo R-5, límite) | FitSense | Milisegundos (no se midió aparte) | — |
| 2 | Recuperación del perfil | FitSense | Junto con la 3: **p50 30–42 ms**, p95 ≤ 65 ms | LOCAL |
| 3 | Filtrado del catálogo (conjunto elegible) | FitSense | Incluido en la 2 | LOCAL |
| 4 | Construcción del prompt (58.000–225.000 caracteres) | FitSense | **p50 2–18 ms**, p95 ≤ 47 ms | LOCAL |
| 5 | Envío a Replicate (POST con `Prefer: wait`) | Replicate | El POST queda retenido **~20,5 s** (p50; p95 21,3 s) y vuelve con la predicción en curso | REAL (estrés) |
| 6 | Arranque y ejecución de la predicción | Replicate | Cola **0,015 s** (p50 de 65). Ejecución **p50 67,6 s, p95 133,8 s, máx. 144,9 s** | REAL (GRA) |
| 7 | Sondeo hasta enterarse del final | FitSense ↔ Replicate | Espera del backend − `predict_time`: **p50 1,9 s, p95 3,1 s** (red + retraso del sondeo). Con pausas de 3 s, el retraso puro es de 1,5 s de media | REAL / SIMULADO |
| 8 | Validación de una propuesta (V1–V27) | FitSense | **p50 3–6 ms**, p95 ≤ 17 ms | LOCAL |
| 9 | Reintentos | Replicate | **Cada uno es otra predicción completa** (~70 s). Los necesitaron 13 de 40 generaciones de GRA | REAL |
| 10 | Respaldo determinista (motor de reglas + validación) | FitSense | **p50 8–21 ms**, p95 ≤ 31 ms | LOCAL |
| 11 | Persistencia | FitSense | **p50 33–45 ms**, p95 ≤ 72 ms | LOCAL |
| 12 | Respuesta HTTP | FitSense | Milisegundos (no se midió aparte) | — |

**Conclusión:**
- **FitSense suma unos 0,1 s por generación.**
- **Replicate suma de 35 s a 145 s por cada predicción.**
- La latencia la deciden la ejecución del modelo y el número de predicciones (reintentos), no el backend.

Por qué tarda el modelo (REAL, GRA):
- **Tokens de salida:** p50 7.000, p95 10.200 (incluyen el razonamiento de `reasoning_effort=medium`).
- **Tokens de entrada:** p50 26.700, p95 78.700 (el catálogo elegible va en el prompt).

## 3. p50 y p95 por generación completa (REAL, GRA, 40 generaciones con IA)

| Generaciones | n | p50 | p95 | Máx. |
|---|---|---|---|---|
| Todas | 40 | 74,1 s | 263,7 s | 350,1 s |
| Válida al primer intento (1 llamada) | 27 | **60,5 s** | 121,6 s | 148,0 s |
| 2 llamadas | 10 | 148,2 s | 243,3 s | 246,0 s |
| 3 llamadas | 3 | 346,9 s | 349,8 s | 350,1 s |

Adaptación (REAL, GRA, 20 predicciones):
- `predict_time`: p50 25,1 s, p95 44,0 s.
- La adaptación se ejecuta en el ciclo semanal, no en la petición HTTP.

## 4. ¿Qué son los ~293 s?

**Es la espera máxima de UNA llamada (un intento), no de toda la operación.** Así se calcula `ReplicateProperties.esperaMaximaSegundos()` (R-3):

| Tramo | s |
|---|---|
| Conectar (POST) | 10 |
| Leer el POST (`Prefer: wait` 60 + 30) | 90 |
| Sondeo (`timeout-seconds`) | 150 |
| Una pausa más (antes 3 s; ahora 1 s) | 3 → 1 |
| Conectar y leer el último GET | 10 + 30 |
| **Total por intento** | **293 → 291** |
| Cancelación de R-2, si la predicción sigue en curso (conectar + leer) | +40 → **333 / 331** |

La cancelación de R-2 se añadió después de R-3, así que los 293 s no la incluían. Ahora se registra al arrancar con y sin cancelación.

## 5. ¿Cuánto podía tardar una generación con tres intentos y respaldo? (antes de esta rama)

- **Peor caso teórico: unos 919 s (15,3 min).**
  - Solo se reintenta tras una propuesta RECHAZADA, que es una respuesta. Un fallo del proveedor va directo al respaldo.
  - Por eso los intentos 1 y 2 son respuestas: ≤ 293 s cada uno.
  - El 3.º puede agotar el sondeo y cancelarse: ≤ 333 s.
  - Después, el respaldo (~0,02 s) y la persistencia (~0,04 s).
- **Típico con tres intentos (REAL):** 265–350 s (3 generaciones de GRA).
- **Con la mediana por intento (SIMULADO):** 3 × 69,5 s ≈ 208 s + 0,1 s.
- **No había presupuesto de la operación completa:** solo límites por llamada.
