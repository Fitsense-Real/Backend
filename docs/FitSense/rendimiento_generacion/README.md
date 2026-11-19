# Rendimiento de la generación con IA

**Objetivo:** que Kora entregue las rutinas antes, sin tocar su seguridad, su personalización ni el comportamiento evaluado.

**Base:**
- `generation` con R-1 a R-5 fusionadas.
- Rama: `perf/reduce-ai-generation-latency`.

**Sin cambios en:**
- Prompts PR-1.7 y V2.1.
- PLAN-REV-2.0, V1–V27, V24 ni V8C.
- Progresión ni reincorporación.
- Guardas de dificultad y dolor.
- Modelo y `reasoning_effort`.
- Railway, Neon y Flutter.

**Sin llamadas a Replicate (USD 0).**

| Documento | Contenido |
|---|---|
| `01_DIAGNOSTICO_LATENCIA.md` | Dónde se va el tiempo, por etapa. Separa FitSense de Replicate. p50/p95 reales. Qué significan los ~293 s |
| `02_OPTIMIZACIONES.md` | A: llamadas imposibles (H-G9). B: sondeo. C: presupuesto total. Más observabilidad |
| `03_PRUEBAS_Y_RESULTADOS.md` | Escenarios, antes y después (real frente a simulado), regresión |
| `04_AUDITORIA_FACTIBILIDAD_Y_REINTENTOS.md` | **Auditoría de la PR #54:** cota inferior verificada contra el validador real, históricos, H-G9, reintentos, presupuestos y decisión por optimización |
| `05_LIMITACIONES_DEL_PROVEEDOR.md` | Lo que no depende de FitSense, por qué 30 s no es alcanzable hoy y qué lo acercaría |
| `evidencia/` | Trazas reales, simulaciones, script de reproducción y salidas de los laboratorios |

**Resumen:**
- **Más del 99 % del tiempo es la ejecución del modelo en Replicate.**
  - Mediana de 67,6 s por predicción con gpt-5-mini, PR-1.7 y razonamiento `medium`.
  - FitSense gasta unos 0,1 s por generación.
- **Lo que FitSense controla y se ha optimizado:**
  - **Llamadas imposibles evitadas:** 3 predicciones (~210 s) en los perfiles H-G9.
  - **Retraso del sondeo:** de 1,5 s a 0,5 s de media por predicción.
  - **Presupuesto total:** desactivado por omisión tras la auditoría (`04`). Es configurable, y 480 s no cambió ninguno de los 48 desenlaces observados.
- **La meta de 30 s no se alcanza con el modelo actual.** La predicción de generación más rápida medida tardó 35,7 s.
