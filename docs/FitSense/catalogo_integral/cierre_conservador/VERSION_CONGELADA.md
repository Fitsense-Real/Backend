# FITSENSE-CATALOGO-TECNICO-CONSERVADOR-v1 · versión congelada

**Cualquier cambio posterior** en el catálogo, en sus migraciones, en las listas de contención o en los interruptores **constituye una versión nueva**, con su propio identificador, regresión y huella.

## 1. Identificación

| | |
|---|---|
| Identificador | `FITSENSE-CATALOGO-TECNICO-CONSERVADOR-v1` |
| Commit del código | **`836a1f5aded805ec4c696e2bd411c97d4ee02f8d`** (rama `cierre/catalogo-conservador-v1`). Los commits posteriores solo añaden documentos y evidencias: `git diff 836a1f5 HEAD -- src` está vacío |
| Fecha | 15/11/2026 |
| Base | `generation` + las 12 PR de la pila (#55–#66; #62 y #60 aportan el mismo parche de HALL-PROG-01, que se aplica una vez) + los commits propios (`ORDEN_INTEGRACION_PR.md`) |
| Migraciones | Flyway **V1…V56**, 59 migraciones aplicadas en este orden: 1–44, 44.1, 44.2, 44.3, 46–52, 52.1, 53, 54, 55, 56. Lista con sus checksums en `evidencia/congelado/flyway.tsv` |
| Estado | **No fusionado ni desplegado.** No se ha tocado Railway, Neon, Flutter ni ningún dato real |

## 2. Catálogo

| | Valor |
|---|---|
| Ejercicios en la base | 1.376 |
| Activos | **1.219** |
| Inactivos | 157 |
| **A** · elegible sin restricción adicional | **919** |
| **B** · elegible con restricciones | **273**: B1, el valor vigente más restrictivo, 252; B2, contención condicionada al perfil, 21 |
| **C** · excluido temporalmente de toda planificación nueva | **27**: 8 de INC-4C-03 y 19 de INC-CAT-CONS-v1 (identidad) |
| **D** · inactivo | **157** |

Las listas completas, con estado, motivo, restricción, decisiones pendientes y GIF, están en:

- `catalogo_clasificado.tsv` (SHA-256 `921855dd…ff474`);
- `CATALOGO_ELEGIBLE_FINAL.xlsx` (A);
- `EJERCICIOS_RESTRINGIDOS.xlsx` (B y C).

Las genera `herramientas/clasificar_catalogo.py`, que lee las listas del propio código.

**Excluidos (C):** 257, 261, 310, 320, 427, 494, 535, 813, 858, 861, 873, 885, 902, 922, 930, 937, 944, 994, 1068, 1086, 1124, 1176, 1177, 1192, 1294, 1298 y 1311.

- INC-4C-03 (`ExternalCatalogService.RETENIDOS_INC_4C_03`): 813, 858, 861, 873, 885, 1192, 1294 y 1298.
- Los 19 restantes, INC-CAT-CONS-v1 (identidad), están en `ContencionConservadora.IDENTIDAD_NO_DETERMINADA`.

**Condicionados (B2):**

| Contención | Ejercicios | Condición |
|---|---|---|
| Apoyo suspendido | 457, 458, 459, 462, 777, 920, 934, 1069, 1153, 1184, 1303, 1304, 1306, 1317 y 1335 | Solo con el permiso propio confirmado (TRUE explícito; hoy, nadie) |
| Dificultad en duda | 1113 | No para principiantes ni para quien no declara nivel |
| INC-CAT-C39 | 131, 400, 448, 1153, 1301, 1303, 1304 y 1306 (1153, 1303, 1304 y 1306 también son de apoyo suspendido) | No para quien declara la restricción que el texto pide incumplir (colgarse o brazos arriba) |
| INC-CAT-1374 | 1374 | Solo con barra y sin restricciones de colgarse ni de brazos arriba |

**Cambios de esta versión sobre los datos:** solo V56, el nombre y las instrucciones del 853 (`REVISION_853.md`). Ninguna dificultad, bandera, equipamiento ni estado de actividad cambió en esta orden.

## 3. Restricciones e interruptores (valores congelados)

| Interruptor | Valor en v1 | Qué hace |
|---|---|---|
| `fitsense.catalogo.contencion-conservadora` | **true** | INC-CAT-CONS-v1 en planificaciones nuevas: generación, motor de reglas, IA, sustitutos, regeneración y reparación |
| `fitsense.seguridad.dolor-en-planes-publicados` | **true** | P-DOLOR-PUB: lo que dolió se marca en las sesiones publicadas que admiten registros |
| `fitsense.regeneracion.p03-por-colocacion` | **false** | Propuesta; requiere aprobación (`VALIDACION_P03.md`) |
| `fitsense.seguridad.apoyo-suspendido-en-no-colgarse` (V53 A) | **false** | Sin activar |
| `fitsense.seguridad.permiso-apoyo-suspendido` (V53-B, #66) | **false** | Sin activar |
| `fitsense.reparacion.tolerar-dificultad-de-publicacion` (#64) | **false** | Sin activar |

Los valores son los que el código toma por defecto (`@Value(...:valor)`). Ningún archivo de configuración los cambia.

**Sin relajar:** V15, V20, V24, V25, V8C, D8 y P-03. No cambian la adherencia, la progresión ni la reincorporación.

## 4. Huella

| Qué | Huella | Cómo se comprueba |
|---|---|---|
| Huella permanente del catálogo, 1.376 filas, atributos y hash de textos | `src/test/resources/catalogo/huella_catalogo.tsv`, SHA-256 `76ad282c…414ae2` | `CatalogoCalidadIT` en cada regresión (pasa) |
| Volcado congelado (`exercises`, `exercise_equipment_requirements`, `exercise_difficulty_approvals` y la historia de Flyway) | Normalizada, sin las dos columnas de hora de la reconstrucción (`exercises.created_at`, `exercise_difficulty_approvals.recorded_at`): ver la tabla siguiente | `herramientas/huella_congelada.py` sobre dos reconstrucciones independientes desde PostgreSQL vacío: **REPRODUCIBLE** |
| Volcado en bruto | `evidencia/congelado/SHA256SUMS`. Incluye las columnas de hora, así que cambia en cada reconstrucción: solo identifica este volcado concreto | — |

Huella normalizada (`evidencia/congelado/HUELLA_NORMALIZADA.txt`):

```
20d6a79531c733124f744c12b94746dee412f58b54f296542ef062e9c49b220e  exercises.tsv  filas=1376  sin=created_at
a23c68431d02052053c346687c322d79ee078a8c01881a8fb35c6cb393cd9ad0  exercise_equipment_requirements.tsv  filas=1522
f79df277101533bfcb26f051bea665e9f99c1ac79f03c81c26405f30177f3606  exercise_difficulty_approvals.tsv  filas=4  sin=recorded_at
57c239d333486f659fc82c25999d162f25c2a75379f3d13bf6427f5aac085025  flyway.tsv  filas=59
```

**Referencias de generación:**

- conservadora: `generacion_referencia_conservador_v1.txt`, SHA-256 `0b52f59e…e6ce`;
- histórica, intacta: `generacion_referencia.txt`, `07ce3ad3…04c0`.

## 5. Reconstrucción desde un PostgreSQL nuevo

```
git checkout 836a1f5
# PostgreSQL 16 vacio: Flyway aplica V1..V56 al arrancar (Hibernate ddl-auto=validate)
sh mvnw -o verify -Pintegration -Dtest=NONE -Dit.test=CatalogoCongeladoIT \
  -Dfitsense.catalogo.congelar=true -Dfitsense.catalogo.salida=<carpeta> \
  -Dsurefire.failIfNoSpecifiedTests=false -Dfailsafe.failIfNoSpecifiedTests=false
python3 -I -B docs/FitSense/catalogo_integral/cierre_conservador/herramientas/huella_congelada.py <carpeta> \
  docs/FitSense/catalogo_integral/cierre_conservador/evidencia/congelado      # debe decir REPRODUCIBLE
```

`CatalogoCongeladoIT` exige además 1.219 activos y 1.376 en total.

## 6. Resultados con los que se congela

| | Resultado |
|---|---|
| Regresión completa | 1530 unitarias + 753 de integración, **0 fallos** (`REGRESION_FINAL.md`) |
| Rejilla de 168 | 168/168 con plan; 0 contenidos (`IMPACTO_FACTIBILIDAD.md`) |
| Rejilla de 312 | 298 válidos + 14 NO_FACTIBLE legítimos (775); 0 violaciones |
| Referencia de 72 | 72/72 VALIDA; diferencias explicadas (`REFERENCIA_CATALOGO_CONSERVADOR.md`) |
| HALL-GM-01 (40) | 35 publicadas sin aumentos y 5 NO_FACTIBLE; 0 trabajos en lo que dolió (`VALIDACION_HALL_GM_01.md`) |
| Dolor en sesiones publicadas | 10/10 casos con prueba (`VALIDACION_DOLOR_PUBLICADO.md`) |

**Riesgos y limitaciones:** `RIESGOS_RESIDUALES.md` y `LIMITACIONES_TESIS.md`. Esta versión **no** está validada clínicamente ni por profesionales, **no** está certificada y **no** garantiza la seguridad.
