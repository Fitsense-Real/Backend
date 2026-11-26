# H2-v3 · PR-2.1: jerarquía de restricciones del prompt de generación inicial

**Orden:** «ORDEN DE TRABAJO — CIERRE H2-v2 Y DESARROLLO DE H2-v3 (PR-2.1)».
**Autorización:** investigar, implementar, probar y crear una PR nueva. **NO MERGE. NO GASTOS ADICIONALES.**

**Rama:** `feature/h2-v3-pr21`.
- Sale de la cabeza de la H2-v2 (`971596e1`), así que conserva su evidencia y sus arneses.
- Se compara contra `generation`.
- PR-2.1 está **desactivada por omisión** (`fitsense.ai.generacion.prompt-version=PR-1.7`).

## Estado en una línea

**Evaluación con gpt-5-mini real (opción A): RECHAZADA** (`06`). Falla el bloqueante **K2** (latencia).

| Brazo | Mediana de `predict_time` |
|---|---:|
| PR-1.7 | 54,4 s |
| PR-2.1 | **61,0 s** |
| Máximo permitido (PR-1.7 × 1,10) | 59,8 s |

El resto de criterios se cumple:
- válidas al primer intento: **23 frente a 18**;
- casos con V15 de la mitad o V26: **1 frente a 4**;
- V26 de 5 rechazos a 0, y V24/V25 de 1 caso a 0.

Consecuencias:
- **se mantiene PR-1.7** y PR-2.1 sigue desactivada;
- la etapa 2 no se ejecutó, porque estaba condicionada a aprobar la etapa 1;
- **presupuesto:** USD 3,1327 de 7,60 reservados;
- **H2 no se declara resuelta.**

Antes de la evaluación, en pruebas controladas sin coste, PR-2.1 corrigió las cuatro contradicciones de PR-2.0 (C10–C13, `01`–`03`).

## Índice

| Entregable de la orden | Documento |
|---|---|
| 1. Cierre de la PR #85 sin merge | `01` §1 |
| 2. Diagnóstico técnico de las regresiones (G02, G03, G15, G18, G19) y revisión del contrato completo | `01` §2 |
| 3–4. PR-2.1 y la jerarquía de restricciones | `02` |
| 5. Pruebas específicas de G02, G03, G15, G18 y G19 | `03` §1 |
| 6–7. Regresión completa y CI | §Regresión, abajo |
| 8. Comparación preliminar sin coste | `03` §3 |
| 9–10. Diseño experimental con gpt-5-mini real y presupuesto | `04` |
| 11. PR nueva e independiente | La PR de esta rama |
| 12. Riesgos y limitaciones | `05` |
| Evaluación real (opción A), informe y decisión | `06`; datos en `evaluacion_real/` |

## Evidencia (`evidencia/`), toda sin coste

| Archivo | Contenido |
|---|---|
| `diagnostico_G02_G03_G15_G18_G19.txt` y `diagnostico_reparto.py` | Ejercicio por ejercicio: grupo, clase y dosis de las salidas reales de A y B |
| `lector_literal_PR21.csv` | Para las 51 salidas reales y las 26 rutinas del motor: rechazos antes, rechazos tras aplicar PR-2.1 y cambios hechos |
| `tras_PR21/` | Las cinco salidas de la regresión, ya corregidas siguiendo PR-2.1 |
| `prompts_y_cotas_26_casos_PR17_PR21.csv` | Modo `PROMPTS`: tamaño y cota de las 52 peticiones de la evaluación propuesta |
| `etapa2_simulado_PR21/` | El arnés de la etapa 2 con Replicate simulado y PR-2.1 |
| `analizar_h2v3.py` (en esta carpeta) | Los criterios de `04`, programados antes de ejecutar |

## Regresión

| Ejecución | Resultado |
|---|---|
| `mvn verify -Pintegration` completo, local, sobre el código de `4dcd3773` | Unitarias: **1.635, 0 fallos**, 1 omitida. Integración: **927, 0 fallos**, 74 omitidas (las opcionales, entre ellas los arneses pagados). **BUILD SUCCESS** (31 min 4 s). Incluye `H2v3RegresionesIT`, `H2v3PipelineSimuladoIT`, `ContratoH2v2IT` y `H2v2PipelineSimuladoIT` |
| `mvn verify -Pintegration` completo, local, tras la evaluación (código de `5d29e302` en adelante; después solo cambiaron datos y documentación) | Unitarias: **1.636, 0 fallos**, 1 omitida. Integración: **927, 0 fallos**, 74 omitidas. **BUILD SUCCESS** (33 min 15 s) |
| CI de GitHub (`verify`) | En verde en `a29a66a4` (resultados de la evaluación); ver la cabeza de la PR |
