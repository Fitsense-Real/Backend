# 09 · Trazabilidad

Cada cifra principal de esta documentación remite a su fuente original.

**Abreviaturas de rama:**
- **G** = `generation` `543932a`;
- **M** = `experimento/mejora-ia` `1d5c28f`;
- **P** = `experimento/confirmacion-pr18a-final` `ffca1f4`.

**Abreviaturas de carpeta:**
- `EIO` = `docs/FitSense/evaluacion_integral_O/`;
- `MIA` = `docs/FitSense/mejora_ia/`.

## 1. Cronología de commits

| Fecha (UTC) | Commit | Rama | Hecho |
|---|---|---|---|
| 06/11/2026 | `f1c6c02` | G (historia) | Código de producción evaluado en GRA (versión A) |
| 06/11/2026 | `c8ff1bf` | G (historia) | Arnés de GRA |
| 06/11/2026 | `b60e5ea` | G (historia) | Congelación de GRA, antes de pagar |
| 06/11/2026 | `3883054` | G (historia) | Entregables de GRA |
| 07/11/2026 | `6a507f8` | G (historia) | Modo de solo lectura del arnés para BASE (`src/test`) |
| 07/11/2026 21:24 | `ba2f16e` | G (historia) | Congelación de BASE |
| 07/11/2026 22:09 | `e0a6441` | G (historia) | Resultados de BASE |
| 07/11/2026 23:48 | `e851de7` | M | Protocolo MEJ congelado; corridas A1, A2 y G1 |
| 08/11/2026 | `c7b4b9d`, `ca4ef1f` | M | Corridas MEJ A0, C, G1C, G1R y CC |
| 08/11/2026 01:27 | `42d9dc1` | M | Resultados de MEJ; base de la auditoría de la ronda 2 |
| 09/11/2026 | `a57abd2` | M | Protocolo de MEJ ronda 2 congelado |
| 09/11/2026 | `1aac853` | M | Código ejecutado en MEJ ronda 2 |
| 09/11/2026 | `0878f85` | M | Cierre de MEJ ronda 2 |
| 10/11/2026 | `5bea428` | M | Corrección de V8C (H-A1/H-A2) y pruebas |
| 10/11/2026 | `957a08d` | M | Protocolo CONF-IA congelado |
| 10/11/2026 | `ab04e67` | M | Código evaluado en CONF-IA (V8C + pruebas de fecha) |
| 10/11/2026 | `1d5c28f` | M | Entregables de CONF-IA (punta actual de M) |
| 10/11/2026 | `685161a` → `d8c925f` | G | PR #43 (pruebas de fecha), fusionada |
| 10/11/2026 | `5b709dd`, `4ca5cd2` → `543932a` | G | PR #44 (V8C), fusionada |
| 10/11/2026 | `f95b218` | P | Código ejecutado de pr-1.8a (traslado exacto, apagado por defecto) |
| 10/11/2026 | `5f48dd5` | P | Protocolo pr-1.8a congelado |
| 10/11/2026 23:35 | `ffca1f4` | P | Entregables de pr-1.8a (punta actual de P) |
| 11/11/2026 | `8b4bb4d` → **`13b75f2`** | G | PR #45: configuración evaluada por defecto en `generation`, fusionada |

## 2. Matriz de afirmaciones

| Afirmación | Cifra | Fuente | Rama |
|---|---|---|---|
| GRA: válidos a la primera | 13/20 y 14/20 (12/19 y 13/19) | `EIO/generacion_ia_real/entregables/INFORME_INTEGRAL_GENERACION_ADAPTACION_IA_REAL.md` §1 y §3 | G |
| GRA: rechazos y motivos | 10 (V15 × 6, V26 × 6, V23 × 2, V24 × 1, V4 × 1) y 6 (V26 × 5, V15 × 4, V13 × 1, V18 × 1) | ídem §3 | G |
| GRA: publicados desde la IA, respaldos | 20/20, 0 | ídem §1 | G |
| GRA: adaptación | 10/10 y 9/10; ACC/ALT/CONS-J/INC 11/0/1/0 y 9/1/2/0 | ídem §5 | G |
| GRA: oráculo | 150/150 y 148/148 | ídem §7 | G |
| GRA: progresión | 26/30, 3/3, 1/0 | ídem §6 | G |
| GRA: coste | 76 predicciones, USD 1,5368 | ídem §9 | G |
| GRA: versión | `9f4cd9ec…` | `EIO/generacion_ia_real/evidencia/versiones.txt` | G |
| BASE: infracciones | 19/19 frente a 7/19 y 6/19; normalizado 637/659; crudo 693/720 | `EIO/baseline_sin_reglas/TABLAS_IEEE_BASELINE.md`, `metricas.json` | G |
| BASE: adaptación | 10/10 ciclos con cambios no autorizados (59/48) | `EIO/baseline_sin_reglas/RESUMEN_RESULTADOS_BASELINE.md` §3 | G |
| BASE: coste y versión | 60 predicciones, USD 0,9594; `hidden` | ídem §1; `PROTOCOLO_BASELINE_SIN_REGLAS.md` §1 y §8 | G |
| MEJ: PR-1.8 | 15/19 frente a 12/19, p = 0,45; V24 13 | `MIA/RESULTADOS_MEJ.md` §3 | M |
| MEJ: coste | 102 predicciones, USD 2,00 | ídem §2 | M |
| MEJ R2: confirmatoria | 21/38, 27/38, 28/38; p = 0,11 y 0,039 | `MIA/ronda2/RECOMENDACION_FINAL.md` §3.2 | M |
| MEJ R2: adaptación | 35/36, 36/36, 36/36 | ídem §4 | M |
| MEJ R2: coste | 362 predicciones, USD 7,9051 | ídem §8 | M |
| MEJ R2: H-A1 y H-A2 | Hallazgos de la auditoría | `MIA/ronda2/AUDITORIA_REGLAS_FITSENSE.md` §7 | M |
| CONF-IA: pr-1.8c | 34/57 frente a 41/57, p = 0,29; V24 0 frente a 2 | `MIA/confirmacion/RECOMENDACION_FINAL_ADOPCION.md` §2 | M |
| CONF-IA: pr-1.8a | 12/19 frente a 16/19, p = 0,28 | ídem §2 | M |
| CONF-IA: adaptación | 58/60 frente a 60/60, p = 0,50 | ídem §4 | M |
| CONF-IA: flujo conjunto | 148/148, 12/12 | `MIA/confirmacion/RESULTADOS_VALIDACION_INTEGRAL.md` | M |
| CONF-IA: coste | 336 predicciones, USD 7,0950 | `MIA/confirmacion/INCIDENCIAS_CONF_IA.md` (tabla de costes) | M |
| pr-1.8a A2 | 13/18 frente a 13/18, p = 1,0; V24 0 frente a 1; V4 0 frente a 3 | `MIA/confirmacion_pr18a/RECOMENDACION_FINAL_PR18A.md`; `ANALISIS_V4_V24.md` | P |
| pr-1.8a: por objetivo | INCREASE_STRENGTH 1/4 → 3/4; LOSE_WEIGHT 5/5 → 4/5… | `MIA/confirmacion_pr18a/RESULTADOS_POR_OBJETIVO.md` | P |
| pr-1.8a: coste | 99 predicciones, USD 2,6115 | `RECOMENDACION_FINAL_PR18A.md` (Coste) | P |
| V8C: pruebas | 25 nuevas, 9 fallos sin la corrección | `MIA/confirmacion/CORRECCION_V8C_HA1_HA2.md`; `PRUEBAS_Y_REGRESIONES.md` | M |
| Regresión final | 1331/0/1 unitarias; 593/0/44 integración; CI verde (run 218) | Ejecución local sobre `4ca5cd2`; CI de `generation` en `543932a` | G |
| Prompt de generación vigente | `PlanPromptBuilder.VERSION = "PR-1.7"` | `src/main/java/main/web/services/fitsense/planning/infrastructure/generation/ai/PlanPromptBuilder.java` l. 93 | G |
| Prompt de adaptación por defecto | Hasta `543932a`: `v1` (`FIX-PR-1.3`). Desde la PR #45: `v2.1` = `FIX-PR-2.1-EXP` | `application.properties`; `SessionFixPromptBuilder.java` l. 57 y 77 | G |
| Política por defecto | Hasta `543932a`: `PLAN-HIST-1` (`@Value` de `PlanningPolicySelector.java` l. 28). Desde la PR #45: `PLAN-REV-2.0` en `application.properties`, con `allow-candidates=true` | `application.properties`; `PlanningPolicySelector.java` | G |
| Progresión | Encendida por defecto desde la PR #45 (antes, apagada); techo 1,10; umbral 100 %; 5 % | `application.properties`; `ProgresionResolver.java` l. 74 y 122; `V7__config_data.sql` l. 25 | G |

## 3. Comprobaciones hechas para esta documentación (11/11/2026, sin llamadas a Replicate)

| Comprobación | Resultado |
|---|---|
| `sha256sum -c` GRA (`generacion_ia_real/SHA256SUMS.txt`) | 62 OK + 3 `*.log` (excluidos por `.gitignore`) OK contra la copia del ZIP = 65/65 |
| `sha256sum -c` GRA congelación | 5/5 OK |
| `sha256sum -c` BASE | 24/24 OK; ZIP sin errores (197 archivos) |
| `sha256sum -c` MEJ | 5/5 OK; tar legible (908 entradas) |
| MEJ ronda 2 | Sin manifiesto; huellas del ejecutor y del análisis coinciden; ZIP sin errores (2978 archivos) |
| CONF-IA congelación | 8 archivos OK + 3 blobs `ab04e67:` OK + 1 registro informativo; ZIP sin errores |
| pr-1.8a congelación | 10/11 archivos OK; `correr_PR18A.sh` difiere (I-P-2, documentado); ZIP sin errores |
| Valores por defecto en `generation` | Verificados en `application.properties`, `PlanningPolicySelector`, `PlanPromptBuilder`, `SessionFixPromptBuilder` y `ProgresionResolver`. Desde la PR #45 los comprueban `ConfiguracionEvaluadaTest` (valores efectivos e interruptores) y `ConfiguracionEvaluadaIT` (cableado de Spring y los cuatro objetivos con PLAN-REV-2.0) |
| Puntas remotas de las ramas | G `543932a`, M `1d5c28f`, P `ffca1f4` |

## 4. PR relacionadas

- https://github.com/Fitsense-Real/Backend/pull/43: pruebas dependientes de la fecha, fusionada.
- https://github.com/Fitsense-Real/Backend/pull/44: V8C H-A1/H-A2, fusionada.
- https://github.com/Fitsense-Real/Backend/pull/45: activación de la configuración evaluada en `generation`.
