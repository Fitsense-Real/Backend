# H2-v3 · PR-2.1: jerarquía de restricciones del prompt de generación inicial

**Orden:** «ORDEN DE TRABAJO — CIERRE H2-v2 Y DESARROLLO DE H2-v3 (PR-2.1)».
**Autorización:** investigar, implementar, probar y crear una PR nueva. **NO MERGE. NO GASTOS ADICIONALES.**

**Rama:** `feature/h2-v3-pr21`.
- Sale de la cabeza de la H2-v2 (`971596e1`), así que conserva su evidencia y sus arneses.
- Se compara contra `generation`.
- PR-2.1 está **desactivada por omisión** (`fitsense.ai.generacion.prompt-version=PR-1.7`).

## Estado en una línea

**PR-2.1 corrige en pruebas controladas las cuatro contradicciones que explicaban las regresiones de PR-2.0** (C10–C13, `01`):
- se reproducen los rechazos reales de G02, G03, G15, G18 y G19;
- seguir PR-2.1 al pie de la letra los convierte en semanas válidas, y lo mismo pasa con las 51 salidas reales y las 26 rutinas del motor de reglas;
- su reintento ya no pide recortar dosis.

**No se ha hecho ninguna llamada pagada. H2 no se declara resuelta:** falta la evaluación con gpt-5-mini real (`04`), que necesita autorización y presupuesto nuevos.

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
| `mvn verify -Pintegration` completo, local | Pendiente de anotar al terminar |
| CI de GitHub (`verify`) | Ver la PR |
