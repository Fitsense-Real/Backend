# 09 · Informe final de la evaluación K20 con Replicate real (R y R′)

**Versión evaluada:**
- Commit de congelación **`89a1e4f`**, árbol `src` `08e69206…`; CI verde con el mismo árbol (run 41957848419).
- Prompt **V2.1** (`FIX-PR-2.1-EXP`), `gpt-5-mini` sobre `openai/gpt-5-structured` (versión `9f4cd9ec…`), oráculo v2.
- Perfiles U (`584e2204…`) y J (`2f793c9f…`) originales. Configuración y hashes en `08`.

**Autorización:** «Autorizo ejecutar la evaluación definitiva R y R′ con Replicate real […] hasta 144 predicciones y USD 7,20».

**Separación de cohortes y fases:**
- Los resultados de **desarrollo** (DEV-R, `A3`; H01–H16; `evidencia/desarrollo/`) **no** forman parte de esta evaluación.
- U01–U20 se usó en el desarrollo: mide compatibilidad, no generalización.
- **J01–J16 es la cohorte independiente:** no se ejecutó nada sobre ella hasta D (`08` §8), y nada se ajustó después.

## 1. Corridas

| Corrida | Carpeta (`evidencia/final/`) | Predicciones | USD medido | Estado |
|---|---|---:|---:|---|
| D, D′ (sin IA) | `D_U`, `Dp_U`, `D_J`, `Dp_J` | 0 | 0 | D = D′ en U (`f9f67973…`) y en J (`276d535d…`) |
| R sobre U (1.ª) | `R_U` | 5 | 0,1226* | **Incidencia técnica** (`INCIDENCIA_R_U.md`): conservada, repetida una vez |
| **R sobre U** | **`R_U_rep`** | 8 | 0,1140 | **Válida** |
| **R sobre J** | **`R_J`** | 15 | 0,1899 | **Válida** |
| **R′ sobre U** | **`Rp_U`** | 8 | 0,1181 | **Válida** |
| R′ sobre J (1.ª) | `Rp_J` | 7 | 0,1251* | **Incidencia técnica** (`INCIDENCIA_Rp_J.md`): conservada, repetida una vez |
| **R′ sobre J** | **`Rp_J_rep`** | 15 | 0,1933 | **Válida** |
| **Total** | | **58** de 144 | **0,763 real por tokens** (0,863 contabilizado*) | |

\* Incluye la reserva de 0,05 USD cargada a una predicción sin tokens. Replicate dejó una en `failed` (621 s) y otra en `canceled` (4,8 s), las dos sin tokens; su coste real es 0. La cifra oficial es la del panel de Replicate.

**Incidencias (procedimiento de `08` §6, decidido antes de ver el oráculo):**
- En R_U (U13) y en Rp_J (J08), una predicción **falló en el proveedor**.
- Sin métricas, el control de gasto del arnés **bloqueó las llamadas siguientes**, incluido el reintento propio del sistema.
- Las dos corridas no reflejan el sistema congelado: se **conservan** y se **repitieron completas una vez**.
- Los topes de las repeticiones (19 / 1,07 y 41 / 2,27) mantienen la suma dentro de 144 predicciones / USD 7,20.
- **No hubo ninguna otra repetición.** Las dos corridas con incidencia se evalúan también en §6.

## 2. Criterios de aceptación aprobados (`07` §3; sin cambios)

Corridas válidas: `R_U_rep`, `Rp_U`, `R_J`, `Rp_J_rep` (`EVALUACION_DEFINITIVA/CRITERIOS_FINALES.csv`).

| Criterio | Cohorte | Resultado | Veredicto |
|---|---|---|---|
| **A-1** Recorridos PASS 20/20 | U | R **20/20** · R′ **20/20** | **CUMPLE** |
| **A-1** Objetivo 16/16 | J | R **15/16** · R′ **15/16** (**J03 FAIL**) | **NO CUMPLE** |
| **A-2/C** 0 incumplimientos de seguridad | U | R 0 · R′ 0 | **CUMPLE** |
| **A-2/C** 0 incumplimientos de seguridad | J | R 0 · R′ 0. J03 no publica ningún plan: su fallo es de generación y cuenta en A-1 | **CUMPLE** |
| **B-1** R + R′ ≥ 18/20 efectivas y pertinentes | U | **20/20** | **CUMPLE** |
| **B-1** Ninguna corrida < 9/10 | U | R **10/10** · R′ **10/10** | **CUMPLE** |
| **B-2** R + R′ ≥ 80 % de las factibles | J | **36/36 = 100 %** | **CUMPLE** |
| **B-2** Wilson 95 % inferior ≥ 63 % | J | **90,4 %** | **CUMPLE** |
| **B-2** Ninguna corrida < 75 % | J | R **18/18** · R′ **18/18** | **CUMPLE** |
| **B-3** Proporción de ACC (informativa) | U / J | **20/20** / **36/36** ACC | — |
| **B-4** 0 desmotivación con > 2 cambios | U / J | 0 / 0 | **CUMPLE** |

**Lectura:**
- **Se cumplen todos los criterios salvo A-1 en J**, que **no se cumple**: J03 no obtiene plan inicial.
- Superar B-1 y B-2 **no** equivale a un 100 % global de recorridos. En J el resultado es 15/16 recorridos, y el 100 % de B-2 se refiere a las **sesiones factibles** de los 15 perfiles con plan.

## 3. U01–U20 (matriz: `EVALUACION_DEFINITIVA/MATRIZ_U01_U20.csv`)

| | R | R′ |
|---|---:|---:|
| Recorridos PASS | 20/20 | 20/20 |
| Sesiones señaladas | 14 | 14 |
| … **factibles** | **10** | **10** |
| … técnicamente no factibles (U03 S3, U04 S3; GAIN_MUSCLE con dosis fija; CONS-J) | 2 | 2 |
| … conservadas por causa no registrada, sin IA (U07 S3, U12 S3; CONS-J) | 2 | 2 |
| **Adaptaciones efectivas publicadas por Replicate** | **10/10** | **10/10** |
| ACC / ALT / INC | 10 / 0 / 0 | 10 / 0 / 0 |
| Respaldos en sesiones factibles | 0 | 0 |
| Propuestas rechazadas | 0 de 8 | 0 de 8 |
| Reintentos | 0 (8 ciclos, 1 intento cada uno) | 0 |
| «Demasiado difícil» (4) | 2 con variante menos difícil y 2 con menos dosis; 0 más difíciles | igual |
| Sustituciones con el mismo músculo | 4/4 | 4/4 |
| Cambio ante desmotivación (3 con cambio) | 1, 1 y 1 ejercicio | 1, 1 y 1 |

**Usuarios:**
- **Con adaptación de IA publicada en las dos corridas:** U04, U08, U09, U13, U14, U18 y U19.
- **Sin orden de adaptación**, correctamente: U01, U02, U05, U06, U10, U11, U15, U16, U17 y U20. Son patrones completos, de datos insuficientes o P40 sin omisión.
- **Conservaciones justificadas:** U03 y U07 (U04 S3 y U12 S3 también, dentro de usuarios que sí se adaptan).

Detalle por sesión, con los ejercicios: `EVALUACION_DEFINITIVA/ADAPTACIONES_IA_DETALLE.csv`.

## 4. J01–J16 (matriz: `EVALUACION_DEFINITIVA/MATRIZ_J01_J16.csv`)

| | R | R′ |
|---|---:|---:|
| Recorridos PASS | **15/16** | **15/16** |
| **J03** | **FAIL de generación**: sin plan inicial; V20, faltan 234 s (`08` §8) | igual |
| Sesiones señaladas (sin J03) | 21 | 21 |
| … **factibles** | **18** | **18** |
| … técnicamente no factibles (J01 S4, J09 S4, J13 S3; GAIN_MUSCLE con fatiga o desmotivación; CONS-J) | 3 | 3 |
| **Adaptaciones efectivas publicadas por Replicate** | **18/18** | **18/18** |
| ACC / ALT / INC | 18 / 0 / 0 | 18 / 0 / 0 |
| Respaldos en sesiones factibles · rechazos · reintentos | 0 · 0 de 15 · 0 | 0 · 0 de 15 · 0 |
| «Demasiado difícil» (10) | 6 con variante menos difícil y 4 con menos dosis; 0 más difíciles | 7 y 3; 0 más difíciles |
| Sustituciones con el mismo músculo | 10/10 | 12/12 |
| Cambio ante desmotivación (6 con cambio) | 1 ejercicio en las 6 | 1 en las 6 |

**J05** (GAIN_MUSCLE a 59 min, la franja de C-8) genera, se adapta y pasa en las dos corridas.

## 5. R frente a R′

- **Categoría:** idéntica en **todas** las sesiones (14/14 en U y 21/21 en J).
- **Recorridos, factibilidad, origen** (IA o conservación) y **ausencia de rechazos:** idénticos.
- **Cambio concreto:** igual en 11/14 sesiones de U y en 11/21 de J. Donde difiere, el modelo eligió otro ejercicio o dosis, siempre ACC:
  - U: U04 S2, U18 S3, U19 S3;
  - J: J04 S3, J06 S4, J07 S3, J08 S4, J13 S2, J14 S2/S3/S4, J16 S2/S4.
- **Lectura:** el resultado funcional es estable entre corridas; la **forma** de la adaptación varía. Con 2 corridas, la variabilidad queda **descrita, no estimada**.

`EVALUACION_DEFINITIVA/DIFERENCIAS_R_RP.csv`

## 6. Las corridas con incidencia (se informan, no se ocultan)

Si se evaluasen `R_U` y `Rp_J` tal como quedaron (`EVALUACION_CON_INCIDENCIAS/`):

| | R_U (1.ª) | Rp_J (1.ª) |
|---|---|---|
| Recorridos PASS | 20/20 | 15/16 |
| Efectivas en factibles | 4/8 | 5/7 = 71,4 % |
| Respaldos (copia segura) | 6 sesiones | 15 sesiones |
| Incumplimientos de seguridad | **0** | **0** |

**Con ellas, B-1 no se cumpliría (14/18) y Rp_J quedaría por debajo del 75 %.**

Son dos hechos que deben constar:
1. **El sistema es seguro ante un fallo del proveedor:** cuando la IA no responde, publica el respaldo determinista sin ningún incumplimiento y el recorrido se completa.
2. **El fallo del proveedor fue frecuente:** 2 de 58 predicciones, con un efecto grande en la efectividad, porque el arnés de evaluación corta las llamadas siguientes por prudencia de gasto. En producción, el sistema reintentaría hasta 3 veces.

## 7. Seguridad y regresión

- **Seguridad** (oráculo v2, en las 4 corridas válidas y en las 2 con incidencia):
  - 0 planes inválidos publicados;
  - 0 modificaciones no autorizadas;
  - 0 aumentos de dosis o de dificultad;
  - 0 sesiones no señaladas alteradas;
  - 0 incoherencias de texto.
- **Regresión del árbol congelado:**
  - unitarias 1.218 ejecutadas, 0 fallidas;
  - integración 523 ejecutadas, 0 fallidas, 41 omitidas (`evidencia/c8/regresion_completa_totales.txt`);
  - rejilla GM 312/312; matriz de 960 y REPLAY idénticas (`11`).
- **CI verde** con el mismo árbol `src`.

## 8. Qué funciona, qué no, y qué se puede afirmar

### Funciona (con evidencia)

- **Recorrido completo de extremo a extremo con IA real:** 20/20 en U en las dos corridas, y 15/15 en los perfiles de J que tienen plan.
- **Adaptación efectiva y pertinente con IA real:** 56 de 56 sesiones factibles en las 4 corridas válidas (U 20/20, J 36/36), todas ACC según el oráculo v2.
  - Sin propuestas rechazadas.
  - Sin reintentos.
  - Sin sustitutos más difíciles.
  - Con el músculo objetivo conservado.
  - Con un único cambio ante desmotivación.
- **Generalización a casos no vistos** (J, independiente): la IA adapta el 100 % de las 36 sesiones factibles; el límite de Wilson al 95 % es del 90,4 %.
- **Seguridad:** ningún incumplimiento en ninguna corrida, ni siquiera con fallos del proveedor.
- **Reproducibilidad del núcleo determinista:** D = D′, y U es idéntica a la evaluación congelada anterior.
- **Coste bajo:** unos 0,11–0,19 USD por corrida de cohorte. Latencia mediana por predicción de 29–35 s.

### No funciona o queda limitado

- **J03:** un principiante de LOSE_WEIGHT en casa, con 70 min, no obtiene plan inicial (V20). **El objetivo de 16/16 en J no se cumple.** Es una limitación del catálogo de bajo impacto frente a V20 con sesiones largas, y afecta a una familia de perfiles (`13`).
- **GAIN_MUSCLE con fatiga o desmotivación** no se puede adaptar (5 sesiones: U03 S3, U04 S3, J01 S4, J09 S4, J13 S3), porque la dosis fija de 4D no admite reducción. Se conservan (CONS-J) y **no** cuentan como éxito.
- **Dependencia del proveedor:** 2 de 58 predicciones fallaron en Replicate. Un fallo deja la sesión en el respaldo seguro, sin adaptar.
- **Variabilidad:** el ejercicio o la dosis concretos cambian entre corridas (13 de 35 sesiones).

### Afirmaciones defendibles en el artículo

1. «Con IA real (gpt-5-mini vía Replicate) y un prompt con información de factibilidad, FitSense completó los 20 recorridos de la cohorte de desarrollo en dos corridas independientes.»
2. «Sobre 16 casos independientes, generados con una regla fija y no usados en el desarrollo, 15 completaron el recorrido; el restante no obtuvo plan inicial por una limitación determinista del catálogo, ajena a la IA.»
3. «En las sesiones donde existía una adaptación válida, la IA produjo una adaptación efectiva y pertinente en 56 de 56 casos (dos corridas, dos cohortes), sin aumentar dosis ni dificultad y conservando el músculo objetivo, según un oráculo independiente.»
4. «Ningún plan inválido se publicó en ninguna corrida, incluidas dos con fallos del proveedor, en las que el sistema recurrió a un respaldo determinista.»

### Afirmaciones que NO se pueden defender

- Que FitSense adapte el 100 % de los recorridos o de las sesiones: hay sesiones imposibles y un perfil sin plan.
- Que la pertinencia sea **profesional**: es la regla operativa del oráculo. Falta la revisión de un profesional (§9).
- Que la variabilidad del modelo esté **estimada**: 2 corridas solo la describen.
- Que los resultados se extiendan a usuarios reales, dolor, falta de tiempo o cambio de horario: J y U son sintéticos y cubren fatiga, demasiado difícil y desmotivación.
- Que el volumen semanal de GAIN_MUSCLE con 61–75 min sea adecuado (`13`).

## 9. Para la revisión profesional (no cambian ninguna cifra)

- **Fatiga:** a veces se acorta un estiramiento (U18 S3 en DEV-R; J04 S3 en R′, de 30 a 20 s).
- **«Demasiado difícil»:** en algunas sesiones se reducen muchos ejercicios a la vez (J08 S4 en R′). La regla de cambio mínimo solo se exige ante desmotivación.
- **Etiquetas del catálogo:** «Pase de banda» (125) figura con músculo objetivo «glúteos» (J14 S2 en R; J16 S2 y S4 en R′). La comprobación del músculo depende de esas etiquetas.

## 10. Recomendación

1. **Usar R y R′ válidas como evidencia del artículo**, con las dos corridas con incidencia informadas en un párrafo o en el material suplementario.
2. **Declarar expresamente que A-1 en J no se cumple (15/16)**, con su causa.
3. Para una versión posterior, **no para este artículo**, porque exigirían una nueva evaluación:
   - la familia de principiantes con sesiones largas (J03, `13`);
   - que el arnés de evaluación no bloquee las llamadas por una predicción fallida sin tokens. Puede reintentar como el sistema y mantener el bloqueo solo cuando se supere la reserva;
   - una revisión profesional de las adaptaciones (§9).
4. **No fusionar, no desplegar y no editar el short paper** hasta tu decisión.

## Evidencia

- `evidencia/final/` (todas las corridas, cada una con `00_configuracion.txt`, `oraculo/`, `predicciones_metricas.csv`, `trazas_ia.jsonl`, `prompts/` y `tablas/`);
- `EVALUACION_DEFINITIVA/` y `EVALUACION_CON_INCIDENCIAS/`;
- `INCIDENCIA_*.md`;
- `registro_ejecucion_R.txt`;
- herramientas `evaluar_final.py` y `comparar_dev_r.py`.
