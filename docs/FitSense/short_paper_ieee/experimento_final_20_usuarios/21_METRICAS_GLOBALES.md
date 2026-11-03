# Métricas globales (calculadas con los datos observados)

**Fuentes:** `08_RESULTADOS_20_A0.csv` y `11_RESULTADOS_20_A1.csv`. A0 y A1 son idénticas (`12_COMPARACION_A0_A1.md`). La etapa C no se ejecutó (`13_CONFIGURACION_REPLICATE.md`).

## 14.1. Generación inicial

| Indicador | A0 / A1 | C |
|---|---:|---|
| Planes iniciales válidos y publicados | **20/20** | No ejecutada |
| Rechazados por los validadores | 0 | — |
| NO_FACTIBLE | 0 | — |
| Errores técnicos | 0 | — |
| Origen | Motor de reglas (`RULE_ENGINE`), 1 intento por usuario | — |

## 14.2. Adherencia (tolerancia ±0,01 pp)

| Indicador | Coinciden con el oráculo | Error absoluto máximo frente al oráculo | Coinciden con el recálculo independiente |
|---|---:|---:|---:|
| Aw | 18/20 | 0,56 pp (U12: 79,44 frente a 80) | **20/20 (error 0,00)** |
| Aobs | 18/20 | 0,56 pp (U12) | 20/20 (0,00) |
| C | 20/20 | 0,00 | 20/20 (0,00) |

**Las dos diferencias** (U07: 79,75; U12: 79,44) vienen de la entrada P40 con dosis enteras, no del cálculo (`09_HALLAZGOS_Y_CAUSAS_RAIZ.md`, H1).

**Error medio absoluto frente al oráculo, en los 20 casos:**
- Aw = (0,25 + 0,56) / 20 = **0,04 pp**;
- Aobs = 0,04 pp;
- C = 0.

**Diferencia metodológica:** la decisión usa Aobs, no Aw. No afecta a estos 20 casos (H2).

## 14.3. Decisiones

| Indicador | A0 / A1 |
|---|---:|
| Decisiones que coinciden con el oráculo del artículo | **18/20** |
| Decisiones distintas | 2 (U07, U12): `ADAPT_LOCALIZED` frente a «sin cambio», porque Aobs = 79,75 o 79,44 < 80 |
| Diferencias explicadas por las reglas vigentes | 2/2. El backend aplica su umbral estricto sobre el porcentaje realmente registrado |
| No evaluables por falta de plan | 0 |
| `NO_CHANGE` | 6 (E1 ×4, más U02 y U17 de E2) |
| `ADAPT_LOCALIZED` | 6 (E3 ×4, más U07 y U12) |
| `ADAPT_EXPANDED` | 4 (E4 ×4) |
| `INSUFFICIENT_DATA` (conserva el plan) | 4 (E5 ×4) |
| Sesiones señaladas correctamente | E3 → S3 (la omitida); E4 → S2 y S3 (las dos omitidas); U07 y U12 → S3 (la parcial) |

## 14.4. Aplicación de cambios

| Indicador | A0 / A1 |
|---|---:|
| Adaptaciones efectivamente publicadas (cambios aplicados) | **0** (no hay IA en la etapa A) |
| Planes siguientes publicados | 20/20 |
| Sesiones cambiadas en los planes siguientes | **0 de 60** |
| Sesiones no señaladas que se conservaron intactas | **46 de 46**: las 60 menos las 14 señaladas (4 × 1 en E3, 4 × 2 en E4 y 1 × 2 en U07/U12). Las 14 señaladas tampoco cambiaron (respaldo o conservación) |
| Respaldos (`RESPALDO`) | 8 (E3 y E4) |
| Conservaciones deliberadas (`SIN_EFECTO`) | 2 (U07, U12) |
| Copias sin orden (`SIN_ORDEN`) | 10 (E1, E5, U02 y U17) |
| NO_FACTIBLE | 0 |
| Violaciones de restricciones o de linaje (V31/V32) | **0** |

## 14.5. Replicate

**No ejecutado.** No hay operaciones con llamada, propuestas, rechazos, reintentos, respaldos por fallo de la IA, tiempos ni costes. Lo único que se puede adelantar con los datos de A es el número de operaciones que **habrían sido elegibles** para la IA:
- **8 adaptaciones** (E3 y E4), con un máximo de 24 llamadas;
- **20 generaciones iniciales** en los gemelos, con un máximo de 60.

**No se usa el 100 % de planes publicados como tasa de éxito de nada relacionado con la IA:** todos salieron del motor de reglas o de la copia determinista.
