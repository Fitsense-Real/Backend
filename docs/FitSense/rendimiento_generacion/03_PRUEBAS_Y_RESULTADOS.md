# 03 · Pruebas y resultados

**Ninguna llamada a Replicate (USD 0).** Cada cifra lleva su origen:
- **REAL**: trazas medidas en campañas anteriores.
- **SIMULADO**: cálculo o reloj falso sobre tiempos reales.
- **LOCAL**: medición en este contenedor.

## 1. Escenarios

| Escenario | Antes | Ahora | Prueba |
|---|---|---|---|
| Válida al primer intento | 1 predicción; p50 60,5 s (REAL) | 1 predicción; ~59,9 s (SIMULADO: −1 s de sondeo) | `PresupuestoGeneracionTest.validaALaPrimera`, `ReplicateSondeoRendimientoTest` |
| Válida tras un rechazo | 2 predicciones; p50 148,2 s (REAL) | 2 predicciones; ~146 s (SIMULADO) | `PlanGenerationPipelineCircuitBreakerTest`, `ReplicateRespuestasInvalidasTest` |
| Timeout de una predicción | Sondeo hasta 150 s, cancelación (R-2) y respaldo | Igual. Si además se agota el presupuesto: se corta antes, se cancela y no cuenta para el corte | `ReplicateSondeoRendimientoTest.sinPlazoComoAntes` / `plazoCortaYCancela`, `ReplicateCancelacionTest` |
| JSON inválido | Fallo del proveedor y respaldo (R-1) | Igual | `ReplicateRespuestasInvalidasTest` (14) |
| Proveedor caído | Respaldo inmediato; cuenta para el corte | Igual | `PresupuestoGeneracionTest.presupuestoNoAbreElCorte`, `PlanGenerationPipelineCircuitBreakerTest` |
| **H-G9** | **3 predicciones rechazadas** (~3 × 69,5 s ≈ 210 s) y respaldo | **0 predicciones**; respaldo en ~0,1 s | `FactibilidadGeneracionIAIT.rejillaHG9` |
| Perfil factible parecido a H-G9 | IA | **IA** (sin cambios) | `FactibilidadGeneracionIAIT` (principiante; 40 y 45 min) |
| Respaldo determinista | Motor de reglas + V1–V27 | Igual (8–21 ms LOCAL) | Las mismas |
| Generaciones repetidas | 422 sin coste (R-5) | Igual | `GeneracionInicialConcurrenteIT.repetidasTrasPublicar`, `GeneracionInicialServiceTest` |
| Solicitudes concurrentes | 409 sin llamar a la IA (R-5) | Igual | `GeneracionInicialConcurrenteIT.dosSimultaneas` |
| Tercer intento sin tiempo (predicciones de 170 s) | 3 intentos: 510 s | **2 intentos: 340 s**, después el respaldo | `PresupuestoGeneracionTest.tercerIntentoSinTiempo` |

## 2. Antes y después

| Indicador | Antes | Después | Origen |
|---|---|---|---|
| Tiempo total, GRA (40 generaciones con IA) | p50 75,8 s · p95 268,7 s · máx. 350,1 s | p50 74,3 s · p95 267,6 s · máx. 345,1 s | SIMULADO sobre trazas REALES (`REPRODUCCION_TRAZAS.csv`) |
| Tiempo total, válidas al primer intento | p50 60,5 s | p50 59,9 s | ídem |
| Tiempo total, H-G9 | ~210 s (3 × p50 real) | ~0,1 s | SIMULADO + `FactibilidadGeneracionIAIT` |
| Peor caso teórico de una petición | ~919 s | 360 s + una lectura en curso + cancelación (≤ ~490 s) | Cálculo (`01` §5, `02` C) |
| Retraso del sondeo por predicción | media 1555 ms · p95 2894 ms | media 501 ms · p95 941 ms | SIMULADO con los 56 `predict_time` REALES |
| Intentos, GRA | 56 | 56 (0 desenlaces cambian) | SIMULADO sobre trazas REALES |
| Intentos, H-G9 (por generación) | 3 + respaldo | 0 + respaldo | `FactibilidadGeneracionIAIT` |
| Llamadas evitadas | — | 3 por generación inicial H-G9. 6 de 240 perfiles de un día. 0 en los demás | `FactibilidadGeneracionIAIT` |
| Uso de respaldo | H-G9: respaldo tras 3 rechazos | H-G9: respaldo directo. GRA: igual (0 → 0). Colas > 360 s (prompt anterior): 2 de 8 pasan a respaldo | ídem y `BARRIDO_PRESUPUESTO.csv` |
| Planes inválidos publicados | 0 | **0** | Validación completa en todos los caminos; regresión |
| Regresiones V24 / V8C | — | **0** | Regresión completa (§3) |

## 3. Regresión

Ver la PR de rendimiento: regresión completa (unitarias e integración) sobre `generation` con R-1 a R-5, escenarios O06, O12 y O20 con IA simulada, y CI.
