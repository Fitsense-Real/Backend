# 22 · Auditoría de coherencia: informe de cierre, F8, oráculos y evidencia congelada

**Alcance:**
- Se recalcula todo **desde la evidencia archivada**, no desde los documentos.
- Ningún resultado anterior se modifica: las precisiones de §2 se recogen aquí, sin reescribir `17` ni `18`.

**Herramientas:**
- `herramientas/auditar_coherencia.py`: re-extrae los `.tar.gz` y re-ejecuta los oráculos.
- `pre_replicate/clasificar_resultados.py`: PASS / ESPERADO_SIN_PLAN / DEFECTO.

## 1. Evidencia archivada (`evidencia/k20_cierre/`, 26 corridas)

**Resultado: 0 discrepancias.**

| Comprobación | Resultado |
|---|---|
| Huellas de los tres oráculos iguales entre sí en cada corrida | 26/26 |
| Huella = la de `oraculos_f8.txt` | 26/26 |
| D = D′ en REV2 (U, J, K, L, M) | 5/5 |
| HIST = huellas congeladas (U `f9f67973`, J `276d535d`, K `54a5bda0`, L `a1379048`) | 4/4 |
| `llamadas_proveedor = 0` | 26/26 |
| Clasificaciones Hist / REV2 / REV2+DIAS / HIST+DIAS = tabla de `17` §3 | Todas coinciden |
| Barridos `HIST_cierre` / `REV2_cierre` | HIST idéntico byte a byte a `HIST_final` (3.641 válidos). REV2: 3.776 válidos; DOSIS_LIMITADA 1.898 y DOSIS_OBJETIVO 1.878; grupos > 24 series 0; PULL sin tracción 0; sesiones < 3 ejercicios 0; impacto moderado 500. **Todo igual a `17` §2** |
| Evidencia histórica (short paper, `auditoria_post_rrp`, `evidencia/k20`, barridos `HIST`/`REV2`/`*_final`, `cohorte_L`) | Ningún fichero modificado ni borrado desde `d4556b1`; el short paper, tampoco desde `2dc4abe` |

## 2. Precisiones (no son errores de resultado; no se reescribe nada)

| # | Dónde | Qué dice | Qué muestra la evidencia | Efecto |
|---|---|---|---|---|
| 1 | `17`, título y §3 | «código final `cc76bd1`» | 21 de las 26 corridas de F8 se ejecutaron sobre `b6a4718`; solo las 5 *postfix* son de `cc76bd1` | **Resuelto en F10 (§3):** las 23 corridas, repetidas sobre el código final, dan las mismas huellas |
| 2 | `12` §4 y `18` P6/P15 | «775 está en 1.468 planes»; «hoy 194 planes REV2» | Las dos cifras son del barrido `REV2_final` (`d4556b1`), no del código final | En `REV2_cierre`: 775 en **1.441** planes; aislados con fuerza en IS/GF: **194** (igual). HIST no cambia. Las cifras vigentes están en `21` |
| 3 | `17` §1, fila 9 | `TransicionesPoliticaIT` 6/6 | Era cierto en `7a8f00b`. Desde `c2fbab3` son **7/7** (prueba P17) | Ninguno |
| 4 | `18` §4 | Gasto esperado de USD 2–5 | Con el coste real medido (0,014–0,017 USD por predicción): **USD 0,7–1,9** | Se actualiza en `23` §6; el tope no cambia (8,40) |
| 5 | `17` §3 | M29 «FAIL», según el oráculo | Correcto como salida del oráculo, que no se toca. La clasificación nueva lo separa (§4) | Ninguno |

## 3. V24 y la política HIST siguen intactas (código final)

**Repetición completa sobre el código final (F10):**
- Commit `c2fbab3`; `src/main` es idéntico a `cc76bd1`.
- Las 23 corridas K20 (D, D′, HIST D e IA simulada en U, J, K, L y M; el fallo del proveedor; V2.1 en U y M) y los dos barridos.

| Comprobación | Resultado |
|---|---|
| Código de V24 (`infrastructure/validation`, `StrictValidationAdapter`) | **Sin cambios** desde `d4556b1` (`git diff` vacío) |
| Prompt de generación (`PlanPromptBuilder`) | **Sin cambios** desde `d4556b1` |
| K20 HIST D (U, J, K, L, M) | Huellas **idénticas** a las congeladas (`f9f67973`, `276d535d`, `54a5bda0`, `a1379048`) y a F8 (`0e010d07` en M) |
| K20 REV2 D = D′ | U `a662cb4d`, J `97228f04`, K `73f28d97`, L `83bd199a`, M `662e19cf`: **iguales a F8** |
| K20 REV2 con IA simulada (y V2.1, y con fallo del proveedor) | U `84dfb6d2`, J `dafe6034`, K `8ad60c9b`, L `dc0ed02d`, M `e0cc3474`, fallo del proveedor `472de2bd`: **iguales a F8** |
| Llamadas al proveedor | 0 en las 23 |
| Barrido HIST (3.780 perfiles) | BARRIDO_HIST |
| Barrido REV2 | BARRIDO_REV2 |
| Regresión completa | 1.301 unitarias y 588 de integración, 0 fallos, sobre `cc76bd1` (`17` §1). `src/main` no ha cambiado desde entonces; la única prueba nueva (`p17CandidatasDesactivadas`) pasa, 7/7 en su clase |

**Cambios de generación desde `d4556b1`:** `CapabilityWeekBuilder`, `CalidadDosis`, `FrecuenciaSemanal` y `PlanGenerationContext`. O solo actúan en el modo de dosis de REV2, o son refactorizaciones sin efecto. Lo demuestra que HIST sea idéntico byte a byte y que las huellas HIST no cambien.

**Nada de lo decidido (P12, P17, P18, catálogo) cambia la generación ni la adaptación.** Por eso no hace falta repetir pruebas antes de congelar N, más allá de esta verificación.

## 4. M29: resultado esperado sin plan, separado de los defectos

`clasificar_resultados.py` **no** cambia el oráculo ni sus salidas. Usa la columna `esperado`, fijada en el manifiesto de M antes de ejecutar:
- solo admite ESPERADO_SIN_PLAN si el oráculo falla **únicamente** por «0 planes vigentes en S1»;
- y si `incidencias.log` registra el **mismo código** previsto.

| Corrida de M (oráculo vigente) | Commit | PASS | ESPERADO_SIN_PLAN | DEFECTO | M29 |
|---|---|---:|---:|---:|---|
| REV2 D, D′ e IA simulada (1.ª ejecución, la no vista) | `b6a4718` | 29 | 0 | **1** | **DEFECTO**: llegaba el fallo técnico de V25, no `FUERZA_SIN_TIEMPO` |
| REV2 D, IA simulada y V2.1 (*postfix* y F10) | `cc76bd1` / `c2fbab3` | 29 | **1** | 0 | ESPERADO_SIN_PLAN (`FUERZA_SIN_TIEMPO`) |
| HIST D (oráculo HIST+DIAS-1) | todos | 30 | 0 | 0 | PASS: con HIST hay plan, como preveía el manifiesto |

**Lectura:**
- La 1.ª ejecución de M **conserva su defecto**: no se reclasifica a posteriori.
- La corrección `cc76bd1` lo resolvió y la ejecución posterior es de regresión, no de evaluación no vista (`17` §3).
- El clasificador **no oculta fallos**. En `HIST_D_J`, J03 sale DEFECTO: es el fallo conocido de R/R′ (A-1 NO CUMPLE), y J no tiene columna `esperado`.
