# Cierre de pendientes: gpt-5-mini y H2

**Orden:** «ORDEN DE TRABAJO — CIERRE DE PENDIENTES GPT-5 MINI Y H2», 23/11/2026.
**Autorización:** diagnóstico, correcciones justificadas y pruebas. **NO MERGE.**

**PR principal:** [Fitsense-Real/Backend#83](https://github.com/Fitsense-Real/Backend/pull/83), en la rama `feature/replicate-gpt-5-mini`.
**PR relacionada:** [Fitsense-Real/Backend#69](https://github.com/Fitsense-Real/Backend/pull/69). Sin fusionar y sin cambios.

**Rama de evaluación:** `eval/h2-gpt5-mini` (#83 + #69). Solo para la evaluación de H2: no es una PR y no se fusiona.

## Qué se ha comprobado y cómo

| Tipo de evidencia | Qué cubre |
|---|---|
| **Pruebas reales con gpt-5-mini** (Replicate, 26 predicciones nuevas) | H2: comparación pareada PR-1.7 frente a PR-1.9 (`01`) |
| **Corridas reales anteriores** (evaluación G5M, sin repetirlas) | Evidencia de O04, O20 y O06, y tiempos de generación (`02`, `03`) |
| **Pruebas automatizadas** (sin IA, PostgreSQL 16) | O04 por la API, O06, reanudación asíncrona con la petición directa y la regresión completa (`05`) |
| **Lectura de código** (Mobile, sin modificarlo; backend) | Compatibilidad con Flutter, la pantalla de 30 s y el defecto D-M1 (`03`, `04`) |
| **Conclusiones inferidas** | Se marcan como tales en cada documento (p. ej., la causa común V26 en H2, o la valoración clínica de O20) |

## Entregables

| # | Entregable | Documento | Resumen |
|---|---|---|---|
| 1 | **H2** | `01_H2_PR17_VS_PR19.md` | 13 parejas reales. Válidas a la primera: 9 con PR-1.7 y 10 con PR-1.9. Regla de la mitad: 2 casos frente a 1, sin potencia. **V26 sube de 1 a 3 casos y la latencia sube 14 s: desfavorable.** **Mantener H2 desactivada**, corregirla (H2-v2, con los techos de V26 en el prompt) y volver a evaluar con otro presupuesto |
| 2 | **O04** | `02_O04_O20.md` | DURATION sin series: **válido** (contrato V4/V9/V8C) y **una sola interpretación** en el backend (una tanda). Reducción real de 696 a 344 s. Es una **discrepancia del oráculo**, con propuesta para v1.5. En Mobile se muestra y se registra bien, pero hay un **defecto aparte**, D-M1 |
| 3 | **O20** | `02_O04_O20.md` | `SUST-SEG-1` sigue la política vigente y respeta las restricciones. Es una **limitación del oráculo** (A6), con propuesta para v1.5. **Riesgo de seguridad abierto:** la sustituta es una variante del mismo gesto que dolió (25 familias). Hay que decidir entre A, B y C |
| 4 | **Rendimiento** | `03_RENDIMIENTO_Y_MOBILE.md` | Mediana de 81 s por llamada con `medium`. Con el cliente asíncrono de Mobile (rama `generation`), **compatible**: «pendiente» a los 30 s, sin duplicados, sin falsos fallos y sin estados inconsistentes. Riesgo solo con la rama síncrona `main` (D-M2). No se aumentó ningún límite |
| 5 | **Regresiones** | `05_REGRESIONES_PRESUPUESTO_PENDIENTES.md` §1 | Regresión completa, pruebas nuevas, rama de evaluación y CI |
| 6 | **Presupuesto** | `05` §2 | 131 predicciones. **USD 7,9438 de 8** cargados por cota y saldo de USD 0,0562. No se amplió |
| 7 | **Código** | Más abajo | Solo pruebas y documentación en #83. **Sin cambios en el código de producción, las reglas ni los validadores** |
| 8 | **Pendientes** | `05` §4 | P-1 (seguridad de `SUST-SEG-1`) bloquea el merge. P-2 y P-4 son de Mobile. P-3 es H2 |

## Código (entregable 7)

**En #83** (`feature/replicate-gpt-5-mini`, a partir de `4a1e0a21`):

| Archivo | Cambio |
|---|---|
| `src/test/java/.../integral/DuracionSinSeriesHttpIT.java` | **Nuevo.** O04 por la API: se muestra, el volumen cuenta una tanda, se registra, y el contrato de `actualDurationSeconds` es el total (D-M1) |
| `src/test/java/.../planning/O06LimiteDuracionIT.java` | **Nuevo.** Tiempo solicitado, autorizado y publicado |
| `src/test/java/.../planning/ReanudacionConReplicateSimuladoIT.java` | Endpoint simulado: `gpt-5-structured` → `gpt-5-mini`, que es el formato de producción |
| `docs/FitSense/cierre_gpt5_mini_h2/**` | Este informe y su evidencia |

**En la rama de evaluación** `eval/h2-gpt5-mini`:
- la fusión de #69 sobre #83;
- `H2PrimeraPropuestaIT` (opt-in; modos `PROMPTS`, `REAL` y `VALIDAR`).

**No hay commits en `generation` ni en `main`. No se tocó #69. No se modificó `Fitsense-Real/MobileApp`.**
