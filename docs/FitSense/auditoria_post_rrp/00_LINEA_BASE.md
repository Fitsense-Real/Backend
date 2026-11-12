# 00 · Línea base de la auditoría post R/R′

**Fecha:** 05/11/2026.
**Rama de trabajo:** `investigacion/k20-post-rrp-auditoria`, creada desde `2dc4abe`. Se publica en `origin` y no se fusiona.
**Restricciones vigentes:**
- sin merge ni despliegue;
- sin tocar Neon, el short paper ni la evidencia histórica del PR #41;
- sin Replicate.

## 1. Lo que se preserva sin modificar

| Elemento | Referencia | Hash |
|---|---|---|
| Versión congelada de la evaluación K20 | commit `89a1e4f` («congelacion de V2.1») | árbol `src/` = `08e69206d594fe9ae32a9db29fab618c67915371`; árbol completo = `41ab6f4e54b8d671416ab218b7fdd37af3bea685` |
| Evidencia final R/R′ (PR #41, borrador) | commit `2dc4abe`; rama `eval/final-ieee-k20-v2` en `origin` = `2dc4abe94c71dc17212e13f7556c4aa3bd30e765` | árbol completo = `9a093af087055cc6c8d483293d8e670ff51bc516` |
| Rama base del producto | `generation` en `origin` = `76e15c57be2213a3dec1ea5b78c65d082886d853` | — |

Comprobaciones:
- `git rev-parse 89a1e4f:src` sigue dando `08e69206…`.
- La rama `eval/final-ieee-k20-v2` no recibe commits desde esta auditoría.
- Todo el trabajo nuevo está en `docs/FitSense/auditoria_post_rrp/` y en pruebas *opt-in* nuevas. Ninguna corrección se aplica sobre la versión congelada.

## 2. Interruptores reales (valores por omisión en `application.properties`)

| Propiedad | Valor por omisión | Observación |
|---|---|---|
| `fitsense.ai.enabled` | `false` | En K20, el arnés lo activa por propiedades de prueba. |
| `fitsense.ai.adaptation.prompt-version` | `v1` | K20 usó V2.1 (FIX-PR-2.1-EXP) por propiedades de prueba (`08_CONGELACION.md`). |
| `fitsense.adaptation.progression.enabled` | `false` | K20 lo fija explícitamente a `false` (`EvaluacionK20IT.java:115`). |
| `fitsense.adaptation.capability-validation.enabled` | `false` | En la adaptación, V24-ext y V25 no se validan por capacidades. |
| `fitsense.adaptation.difficulty-guard` | `off` | K20 lo fija a `preferencia` (`EvaluacionK20IT.java:116`), es decir, con el límite de dificultad candidato activo. En producción, por omisión, está apagado. |
| `fitsense.coverage.pregeneration` | `OBSERVE` | — |
| `fitsense.coverage.enforcement` | `ENFORCE_AI_ONLY` | El comentario de la propiedad dice «se entrega en OBSERVE» (discrepancia D-CFG, documento 01). |
| `fitsense.approvals.filter-enabled` | `false` | — |

**Nota:** lo desplegado en Railway o Neon (variables de entorno y `calculation_configs` activa) **no se verificó** porque está fuera del alcance autorizado.

## 3. Resultados históricos que sirven de referencia (PR #41, sin cambios)

**D/D′ (determinista):**
- U: 20/20.
- J: 15/16. J03 falla porque no hay plan inicial válido (V20).

**R/R′ definitivos con Replicate (versión congelada):**
- U: 20/20 en cada corrida y B-1 20/20.
- J: 15/16 en cada corrida. A-1 NO CUMPLE por J03. B-2 36/36, con límite inferior de Wilson del 90,4 %.
- 0 incumplimientos de seguridad, 0 rechazos, 0 reintentos.
- 58 predicciones; coste real USD 0,763 (contabilizado USD 0,863).

**Sesiones no factibles (sonda), sin adaptación posible:**
- U03 S3, U04 S3, J01 S4, J09 S4, J13 S3.
- U07 S3 y U12 S3 se conservan sin IA (CAUSA_NO_REGISTRADA).

**Corridas con incidencia (R_U y Rp_J):** se conservan como evidencia (`EVALUACION_CON_INCIDENCIAS`, `INCIDENCIA_R_U.md` e `INCIDENCIA_Rp_J.md`). No se usan como resultado.

## 4. Herramientas nuevas de esta auditoría (solo miden)

| Prueba | Activación | Qué hace |
|---|---|---|
| `DiagnosticoGeneracionJ03IT` | `-Dfitsense.diagnostico.generacion=true` (+ objetivo, nivel, equipo, días y minutos) | Genera con el motor real y valida con el validador de producción. Busca exhaustivamente modificaciones de un paso (añadir, +1 serie, sustituir) y redistribuciones entre días. |
| `BarridoGeneracionInicialIT` | `-Dfitsense.barrido.generacion=true` | 3.780 perfiles (4 objetivos × 3 niveles × 3 equipos × 1–7 días × 15 duraciones). Desde la versión 2, repite cada inválido con 10 semillas más. |

Las dos usan Testcontainers (`postgres:16-alpine`) y una base efímera. No tocan ninguna base persistente.
