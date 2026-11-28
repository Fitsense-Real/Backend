# FITSENSE-CATALOGO-TECNICO-CONSERVADOR-v1.1 — INTEGRADO

## Problema

Hay que dejar constancia formal de la versión del catálogo que queda en `generation`.

## Registro

| | |
|---|---|
| **Identificador** | `FITSENSE-CATALOGO-TECNICO-CONSERVADOR-v1.1 — INTEGRADO` |
| **Fecha** | 15/11/2026 |
| **Commit de integración** | **`50849d0f4b5c979cd980bdaceea1dfd7dd550ebc`** (merge de #67, 14:26:28 UTC) |
| **Código** | Idéntico a la candidata probada `12c6897` (`git diff 12c6897 50849d0 -- src pom.xml .github` vacío; árbol de `src` `5599aacf…`) |
| **`generation` tras V57** | `0d112785a5df70d686da36c80e814b40ca077cd6` (merge de #68). V57 no toca el catálogo: añade una columna de constancia en `workout_session_exercises` |
| **PR integradas** | #62, #56, #57, #58, #59, #55, #60, #63, #65, #61, #64, #66 y #67, en este orden (`PR_INTEGRADAS.md`) |
| **Migraciones del catálogo** | V1…V56, 59 migraciones (1–44, 44.1, 44.2, 44.3, 46–52, 52.1, 53, 54, 55, 56). V57 queda después, ajena al catálogo |
| **Despliegue** | **Ninguno.** Railway, Neon, `main` (`7b8a6d3`) y Flutter sin tocar |

## Catálogo

| Categoría | Cantidad |
|---|---|
| Ejercicios | 1.376 |
| Activos | 1.219 |
| **A** · elegible sin restricción adicional | **919** |
| **B** · elegible con restricciones (B1 252 + B2 21) | **273** |
| **C** · excluido temporalmente de toda planificación nueva | **27** |
| **D** · inactivo | **157** |
| **Utilizables (A + B)** | **1.192** |

Las listas, motivos y GIF están en `../../catalogo_integral/cierre_conservador/VERSION_CONGELADA.md` §2 y en `catalogo_clasificado.tsv`. No cambiaron de v1 a v1.1 ni con el merge: la huella es la misma.

## Huella normalizada

Catálogo reconstruido dos veces desde PostgreSQL vacío sobre `50849d0`, con resultado **REPRODUCIBLE** e idéntico a v1 y v1.1:

```
20d6a79531c733124f744c12b94746dee412f58b54f296542ef062e9c49b220e  exercises.tsv  filas=1376  sin=created_at
a23c68431d02052053c346687c322d79ee078a8c01881a8fb35c6cb393cd9ad0  exercise_equipment_requirements.tsv  filas=1522
f79df277101533bfcb26f051bea665e9f99c1ac79f03c81c26405f30177f3606  exercise_difficulty_approvals.tsv  filas=4  sin=recorded_at
57c239d333486f659fc82c25999d162f25c2a75379f3d13bf6427f5aac085025  flyway.tsv  filas=59
```

## Regresión

| | |
|---|---|
| Regresión completa | 1540 unitarias + 754 de integración, 0 fallos (`REGRESION_POST_MERGE.md`) |
| Rejillas 168 (on/off) y 312 | Idénticas byte a byte a v1.1 |
| HALL-GM-01 (40) | Idéntico byte a byte a v1.1 |
| Referencia de 72 | Pasa |

## Interruptores en producción

| Interruptor | Estado |
|---|---|
| Contención conservadora | **Encendida** |
| P-DOLOR-PUB | **Encendida** |
| P-03 por colocación | Apagada |
| V53 opción A | Apagada |
| V53-B | Apagada |
| Reparación tolerante #64 | Apagada |

El detalle está en `ESTADO_INTERRUPT0RES.md`.

## Riesgos y limitaciones para la tesis

`RIESGOS_RESIDUALES.md` y `../../catalogo_integral/premerge_final/LIMITACIONES_PARA_TESIS.md`.

1. **No hay validación profesional ni clínica.** Esta versión no la tiene y no garantiza la seguridad. Siguen abiertas **312 decisiones** que corresponden a profesionales, y se declaran como limitación.
2. Las funciones opcionales apagadas (P-03 por colocación, V53 A, V53-B y #64) no forman parte de lo evaluado. No se les puede atribuir ningún resultado.
3. Todo se ha medido con perfiles sintéticos y con el validador real. No con personas.
4. Hay 14 perfiles de casa, con solo peso corporal, sin plan (V20, 775). Es un NO_FACTIBLE legítimo.

## Regla de cierre

**El catálogo no se modifica después de este registro.** Cualquier cambio constituye una versión nueva, con su identificador, su regresión y su huella: datos, migraciones, listas de contención o interruptores del catálogo.

## Resultado

`FITSENSE-CATALOGO-TECNICO-CONSERVADOR-v1.1` queda **INTEGRADO** en `generation`.
