# FITSENSE-CATALOGO-TECNICO-CONSERVADOR-v1.1 · versión congelada (propuesta)

Se propone congelarla con este contenido. Se queda en **propuesta** hasta que el propietario autorice el merge.

**Cualquier cambio posterior** en el catálogo, en sus migraciones, en las listas de contención o en los interruptores es una versión nueva.

## 1. Identificación

| | |
|---|---|
| Identificador | `FITSENSE-CATALOGO-TECNICO-CONSERVADOR-v1.1` |
| Commit del código | **`12c6897028fa1af183596d973961b8c39768a981`** (rama `cierre/catalogo-premerge-final`). Los commits posteriores solo añaden documentos y evidencias: `git diff 12c6897 HEAD -- src` vacío |
| Árbol | `475703a028ea316b116ae6fed3fab107d3c024bd`: el mismo que da la simulación de la integración sobre `generation` (`ORDEN_MERGE_CONTROLADO.md`) |
| Versión anterior | `FITSENSE-CATALOGO-TECNICO-CONSERVADOR-v1` (`836a1f5`), intacta en `cierre/catalogo-conservador-v1` |
| Fecha | 15/11/2026 |
| Base | `generation` (`623c4fa`) + las 12 PR #55–#66 (HALL-PROG-01 una sola vez) + los commits propios de la candidata |
| PR miembro | #62, #56, #57, #58, #59, #55, #60, #63, #65, #61, #64 y #66, en este orden, y la PR final |
| Migraciones | Flyway **V1…V56**, 59 migraciones (1–44, 44.1, 44.2, 44.3, 46–52, 52.1, 53, 54, 55, 56). **Ninguna nueva respecto a v1.** Lista en `evidencia/congelado/flyway.tsv` |
| Estado | **No fusionado ni desplegado.** No se ha tocado Railway, Neon, Flutter ni ningún dato real |

## 2. Catálogo (idéntico a v1)

| Categoría | Cantidad |
|---|---|
| Ejercicios | 1.376 |
| Activos | 1.219 |
| **A** · elegible | **919** |
| **B** · con restricciones (B1 252 + B2 21) | **273** |
| **C** · excluido temporalmente | **27** |
| **D** · inactivo | **157** |

Las listas y sus motivos son los de v1 (`../cierre_conservador/VERSION_CONGELADA.md` §2, `catalogo_clasificado.tsv`). El código de las listas de contención no cambió de v1 a v1.1.

## 3. Interruptores

| Interruptor | Producción | Candidata y sus pruebas |
|---|---|---|
| `fitsense.catalogo.contencion-conservadora` | **true** | true |
| `fitsense.seguridad.dolor-en-planes-publicados` | **true** | true |
| `fitsense.regeneracion.p03-por-colocacion` | **false** (`${FITSENSE_P03_POR_COLOCACION:false}`) | **true** (`src/test/resources/config/application.properties`) |
| `fitsense.seguridad.apoyo-suspendido-en-no-colgarse` (V53 A) | false | false |
| `fitsense.seguridad.permiso-apoyo-suspendido` (V53-B, #66) | false | false |
| `fitsense.reparacion.tolerar-dificultad-de-publicacion` (#64) | false | false |

- V53 A y V53-B no pueden estar encendidas a la vez: la configuración lanza `IllegalStateException`.
- **Sin relajar:** V15, V20, V24, V25, V8C, D8, los límites de progresión y de reincorporación, y la fórmula de adherencia.

## 4. Huella

Normalizada, sin las columnas de hora de la reconstrucción (`evidencia/congelado/HUELLA_NORMALIZADA.txt`). Dos reconstrucciones independientes desde PostgreSQL vacío dan **REPRODUCIBLE**, y coinciden con v1:

```
20d6a79531c733124f744c12b94746dee412f58b54f296542ef062e9c49b220e  exercises.tsv  filas=1376  sin=created_at
a23c68431d02052053c346687c322d79ee078a8c01881a8fb35c6cb393cd9ad0  exercise_equipment_requirements.tsv  filas=1522
f79df277101533bfcb26f051bea665e9f99c1ac79f03c81c26405f30177f3606  exercise_difficulty_approvals.tsv  filas=4  sin=recorded_at
57c239d333486f659fc82c25999d162f25c2a75379f3d13bf6427f5aac085025  flyway.tsv  filas=59
```

La huella permanente del catálogo (`src/test/resources/catalogo/huella_catalogo.tsv`, `76ad282c…`) no cambia, y `CatalogoCalidadIT` pasa.

**Reconstrucción:** como en v1 (`../cierre_conservador/VERSION_CONGELADA.md` §5), con `git checkout 12c6897`.

## 5. Resultados con los que se propone congelar

| | Resultado |
|---|---|
| Regresión completa | 1540 unitarias + 754 de integración, **0 fallos** (`REGRESION_INTEGRADA_FINAL.md`) |
| Rejillas 168 (con y sin contención) y 312 | Idénticas a v1 |
| Referencia de 72 | Pasa |
| HALL-GM-01 (40) | Idéntico a v1 |
| P-03 por colocación | 10 pruebas obligatorias. Contraejemplo y cambio de día bloqueados. **1 escenario cambia** (`VALIDACION_P03_POR_COLOCACION.md` §3) |
| Dolor y planes publicados | `VALIDACION_DOLOR_Y_PLANES_PUBLICADOS.md` |

## 6. Diferencias con v1

`COMPARACION_V1_V11.md`: 6 archivos de código, todos para P-03 por colocación. Ningún dato ni migración cambia.

**Riesgos y limitaciones:** `RIESGOS_RESIDUALES.md` y `LIMITACIONES_PARA_TESIS.md`. Esta versión **no** está validada clínicamente ni por profesionales y **no** garantiza la seguridad.
