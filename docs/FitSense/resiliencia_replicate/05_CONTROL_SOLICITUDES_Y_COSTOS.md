# 05 · R-5: control de solicitudes y costes

## Reglas que ya existían (revisadas antes de cambiar nada)

- `prepare` y `persist` rechazan si ya hay un plan activo para la semana (`PlanAlreadyExistsException`, 422). Una petición repetida **después** de publicar ya no generaba.
- El ciclo semanal ya estaba excluido por persona entre hilos e instancias (`CicloSemanalBloqueo`: bloqueo consultivo de sesión de PostgreSQL, en una conexión directa) y publica a lo sumo una semana.
- **Lo que faltaba:** la petición de primer plan no usaba ese bloqueo y no tenía límite de frecuencia.

## Protecciones añadidas (`GeneracionInicialService`)

| Caso | Antes | Ahora |
|---|---|---|
| Solicitud normal | Genera | Genera (igual) |
| Dos idénticas **consecutivas** | La 2.ª: 422, sin coste | Igual |
| Dos **simultáneas** | Las dos pagaban IA (hasta 3 + 3 predicciones); la 2.ª fallaba al persistir | La 2.ª: **409 al instante, sin llamar a la IA**. 1 plan |
| Petición mientras corre el **ciclo semanal** de esa persona | Sin exclusión | **409**: misma clave de bloqueo que el ciclo |
| Diez repetidas con plan existente | 10 × 422, sin coste | Igual, y **no gastan cupo** |
| Diez repetidas que fallan al generar | 10 × hasta 3 predicciones | **5 llegan a generar**, el resto **429** con `Retry-After` |
| Error durante una generación | — | El bloqueo se suelta (`finally`) y la siguiente se atiende |
| **Reinicio del proceso** durante una solicitud | — | PostgreSQL suelta el bloqueo al cerrarse la conexión de esa sesión; la siguiente se atiende |
| Base no disponible para el bloqueo | — | **503**: no se genera sin exclusión |
| Nueva semana (ciclo) o regeneración P-03 | — | Sin cambios: el ciclo tiene su propia exclusión y no cuenta para el límite |

**Exclusión (válida con varias instancias):**
- Se reutiliza el bloqueo del ciclo; **no se añade ninguna tabla ni migración**.
- Mientras dura una generación inicial se mantiene **una conexión directa** abierta, del orden de minutos en el peor caso.
- Con muchas generaciones iniciales simultáneas, hay que vigilar el número de conexiones de la base.

**Límite de frecuencia:**
- `fitsense.ai.generate.max-per-hour` (`FITSENSE_AI_GENERATE_MAX_PER_HOUR`), 5 por omisión, válido entre 1 y 100.
- **Justificación:** el flujo normal necesita 1, y 5 deja margen para corregir el perfil y reintentar. Acota el peor caso a 15 predicciones por persona y hora.
- **Límite declarado:** la ventana vive en memoria y **es por instancia**. Con N instancias el límite efectivo es N × 5, y un reinicio la vacía.
  - Un límite común a todas las instancias exigiría una tabla (migración), que esta orden no permite. Queda propuesto.

## Supervisión del gasto (sin código nuevo)

| Qué | Dónde | Cómo |
|---|---|---|
| Predicciones y su resultado | Log `REPLICATE_TIMING` (una línea por llamada: `resultado=SUCCEEDED/TIMEOUT/NOT_SUCCEEDED/POST_ERROR/INVALID_RESPONSE/POLL_ERROR/INTERRUPTED`, `prediction_id`, tiempos) | Contar líneas por `resultado` |
| Cancelaciones | Log `REPLICATE_CANCEL` (PR #50) | Contar por `resultado` |
| IA no invocada (cortacircuitos) | Log `IA_NO_INVOCADA` | Contar |
| Errores inesperados | Log `IA_ERROR_INESPERADO` (PR #49) | Contar |
| Reintentos y respaldos de generación | `weekly_training_plans.generation_attempts`, `generation_source` | SQL de abajo |
| Llamadas de adaptación | `plan_adaptation_attempts.ai_calls`, `status` | SQL de abajo |
| Peticiones rechazadas por R-5 | Log `GENERACION_EN_CURSO`; respuestas HTTP 409 y 429 | Contar |
| **Gasto real** | **Solo el panel de facturación de Replicate** | — |

```sql
-- Generaciones iniciales y semanales: intentos de IA (generation_attempts - 1 cuando hubo respaldo) y origen.
SELECT date_trunc('day', created_at) AS dia, generation_source,
       count(*) AS planes, sum(generation_attempts) AS intentos
FROM weekly_training_plans GROUP BY 1, 2 ORDER BY 1;

-- Adaptaciones: llamadas hechas por estado.
SELECT date_trunc('day', created_at) AS dia, status, count(*) AS adaptaciones, sum(ai_calls) AS llamadas
FROM plan_adaptation_attempts GROUP BY 1, 2 ORDER BY 1;
```

**Estimación frente a gasto confirmado:**
- La aplicación **no recibe el coste** de cada predicción. Cualquier cifra calculada aquí (predicciones × coste de referencia de GRA, unos USD 0,023 por generación y USD 0,013 por adaptación con versiones anteriores del modelo) es una **estimación**.
- El gasto confirmado solo lo da Replicate.

**Tope monetario dentro del backend:** no se implementa. Para que fuera fiable haría falta:
1. un contador **común a todas las instancias** (tabla, con migración);
2. contar **antes** de cada predicción, incluidas las que siguen en curso;
3. un precio por predicción que la API no devuelve.

Aun así, no garantizaría el gasto de la cuenta, por las predicciones que quedan en curso tras un corte o que se crean fuera de la aplicación. **El tope real es el límite de gasto y las alertas de la cuenta de Replicate.**
