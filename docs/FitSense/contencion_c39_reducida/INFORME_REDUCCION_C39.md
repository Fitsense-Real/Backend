# PR #55 ajustada al catálogo integral corregido: contención de 26 a 8

**Estado:** validada sobre la pila completa #56–#59 (V47–V50 y controles de calidad). **Sin merge, sin despliegue, sin datos reales y sin llamadas a Replicate.**

## 1. Qué se hizo

1. **Variante aislada sobre la pila completa.**
   - Se partió de la rama de la PR #55 original (`e1c3adc`) y se incorporó con un merge, sin reescribir historia, `audit/correccion-integral-catalogo` (#59, que contiene #56–#58).
   - Primero se ejecutó la PR #55 **tal cual** sobre esa pila (`aaf5997`) como línea base. Después se aplicó la reducción.
   - La contención nunca se probó solo contra el catálogo antiguo.
2. **Contención reducida a 8.**
   - Colgarse: 1153, 1301, 1303, 1304, 1306.
   - Brazos sobre la cabeza: 131, 400, 448.
   - Salen 18 por completo. 1301 deja de contenerse por brazos arriba, y 1303 y 1304 por suelo.
   - Cada liberación se justifica id por id porque el texto corregido por V49 ya no pide la acción restringida (prueba `l_liberadosJustificados`). Ninguna se debe a que haga falta para generar un plan.
3. **Verificaciones 1–7** sobre el catálogo V50 (§3).
4. **Lista priorizada de revisión humana** (`REVISION_HUMANA_PRIORIZADA.md`).
5. **Impacto en planes publicados** (`PLANES_PUBLICADOS_C39.md`).

## 2. Cambios exactos de la PR #55

El diff completo frente a la PR #55 original aplicada sobre la pila está en `DIFF_PR55_ORIGINAL_VS_REDUCIDA.diff`.

| Fichero | Cambio |
|---|---|
| `ContencionTextoBandera.java` | Registro de 26 a 8, con el motivo actualizado (apoyo suspendido, curl alto). Añade `LIBERADOS_POR_V49` (18 ids, solo documentación y pruebas). Retira la constante de suelo sin uso. Mismo mecanismo: sin cambios en `ExternalCatalogService`, `SafetyConflictChecker` ni `SafetyConflictPolicy`. |
| `ContencionTextoBanderaTest` (unitaria) | Conjuntos a 8. Nuevas pruebas: `parcial1301`, `liberados` (18 + 8 = los 26 originales) y suelo vacío. |
| `ExternalCatalogServiceContencionC39Test` | Ids de ejemplo cambiados: 1301 y 400 contenidos, 1093 liberado. La misma comprobación de consulta normal igual a consulta de escasez. |
| `ContencionTextoBanderaIT` | Catálogo V1–V50. A y B comprueban los 8 y la liberación de 1301 por brazos arriba. **D**: la IA recibe un subconjunto del conjunto elegible del motor, sin contenidos. **H**: ningún NO_FACTIBLE (antes se esperaba uno). **I**: 8 filas. Nuevas: **L** (liberados justificados y que vuelven al conjunto elegible salvo por su propia bandera) y **R** (los dos perfiles LOSE_WEIGHT recuperados). |
| `CatalogoElegibleIT` (oráculo SQL independiente) | Listas C39 a 8; sin lista de suelo. |
| `Usuario31ReproduccionIT` | El perfil vuelve exactamente a la semana de la pila sin contención: sábado 1320. Solo se comprueba la ausencia de los 5 contenidos por colgarse. |
| `Plan78ContencionReducidaIT` (nuevo) | Copia controlada del plan 78: bloqueo, reparación (NO_VALIDA por el 916; localizada sin el 916), semana empezada y consulta de identificación. |
| `docs/…/CORRECCIONES_SEGURIDAD.md` | §9, la reducción. |
| `docs/FitSense/contencion_c39_reducida/` | Este informe, la matriz, los perfiles, los planes publicados, la revisión humana, los riesgos y el orden de fusión, la evidencia y las herramientas. |

**Expectativas de prueba cambiadas:**
- Solo las tres que dependen del registro de 26: H `SIN_PLAN_ESPERADO`, I (26 → 8) y los conjuntos de las unitarias y del oráculo.
- Ninguna cambia para ocultar un error. El único error encontrado al escribir las pruebas fue mío: suponer que el 400 era elegible para el perfil de la prueba A. Se corrigió calculándolo en vez de suponerlo.

## 3. Verificaciones obligatorias

| # | Verificación | Resultado | Evidencia |
|---|---|---|---|
| 1 | Cero ejercicios incompatibles con las restricciones declaradas | **Cumple.** En la rejilla de 120 perfiles, 0 semanas con un contenido para su restricción y 0 con una bandera restringida. Prueba H con las tres restricciones, 4 objetivos × 2 niveles × 2 lugares. | `evidencia/c39_8.csv`, `ContencionTextoBanderaIT` E, H y R |
| 2 | Los dos perfiles LOSE_WEIGHT recuperan una rutina válida sin relajar V25 | **Cumple.** Los dos publican, con cardio de pie en cada sesión, sin suelo ni prohibidos. Su semana es idéntica a la de la pila sin contención. | `PERFILES_RECUPERADOS.md`, prueba R |
| 3 | IA y motor de reglas usan el mismo catálogo elegible | **Cumple.** Los dos leen el mismo conjunto filtrado. La IA recibe un subconjunto: por diseño previo se omite lo que no tiene dosis admisible para el objetivo. Ninguno contiene un ejercicio contenido. La consulta de escasez aplica la misma contención. | Prueba D, `ExternalCatalogServiceContencionC39Test` |
| 4 | Las sesiones publicadas con los 8 siguen detectando conflictos | **Cumple.** El 1303 bloquea la sesión con la causa INC-CAT-C39, no deja iniciarla y en una sesión en curso solo admite omitirlo. En el plan 78, solo se bloquea el jueves. | F y G de `ContencionTextoBanderaIT`, `Plan78ContencionReducidaIT` |
| 5 | V8C solo autoriza sustituciones localizadas y justificadas, sin aumentar la dosis | **Cumple.** La reparación del plan 78 sin el 916 cambia una sola fila (el 1303), sin más series, repeticiones ni duración, y conserva la versión 1. Las pruebas propias de V8C pasan sin cambios. | `reparacionLocalizada`, regresión |
| 6 | V24, V15, adaptación, progresión y reincorporación mantienen sus invariantes | **Cumple.** Las 339 clases de prueba comunes con la pila dan exactamente el mismo resultado. Ningún validador, política, dosis ni umbral se tocó. | `evidencia/regresion_8_reducida.txt` |
| 7 | Sin regresiones no explicadas respecto a la pila V47–V50 | **Cumple.** 339 clases comunes idénticas; solo se añaden las de la PR #55 y la del plan 78. La rejilla de 120 perfiles frente a la pila solo difiere en los 8 contenidos, y solo para quien declara su restricción. | `evidencia/regresion_*.txt`, `PERFILES_RECUPERADOS.md` §1 |

## 4. Regresión completa

Ejecutada con `mvnw clean verify -Pintegration`, PostgreSQL 16 efímero y sin Replicate:

| Árbol | Unitarias | Integración | Resultado |
|---|---|---|---|
| Pila #56–#59 (referencia, `8a3bd18`) | 1.417 / 0 fallos | 698 / 0 fallos (48 opt-in) | BUILD SUCCESS |
| PR #55 original sobre la pila (`aaf5997`) | 1.439 / 0 | 711 / 0 (49 opt-in) | BUILD SUCCESS |
| **PR #55 reducida** (`5f5fc53`) | **1.445 / 0** | **717 / 0 (49 opt-in)** | **BUILD SUCCESS** |

Después de esa ejecución se añadieron dos comprobaciones a pruebas ya existentes:
- la consulta SQL en el plan 78;
- el subconjunto IA ⊆ motor en D.

Por eso se repitió la regresión completa sobre el **commit entregado `5df34f6`**: unitarias 1.445 / 0 fallos; integración 717 / 0 fallos (49 opt-in); BUILD SUCCESS. Las 339 clases comunes con la pila dan el mismo resultado (`evidencia/regresion_commit_entregado.txt`). El CI de GitHub en `5df34f6` también está en verde.

## 5. Planes publicados (resumen)

| Plan 78 | Original (26) | Reducida (8) |
|---|---|---|
| Sesiones bloqueadas | Jueves (1303) y viernes (1152) | **Solo el jueves (1303)** |

La reparación del plan 78 tal cual da **NO_VALIDA**: la misma sesión lleva el 916, que V48 subió a nivel 3. Sin el 916 se repara de forma localizada.

Limitaciones actuales (detalle en `PLANES_PUBLICADOS_C39.md`):
- con la semana empezada no se repara;
- la sesión bloqueada vence como SKIPPED sin motivo y cuenta en la adherencia;
- IN_PROGRESS se queda pegado (H6);
- no hay aviso proactivo.

La consulta de identificación de solo lectura está probada en la copia del plan 78.

## 6. Documentos

| Documento | Contenido |
|---|---|
| `MATRIZ_26_VS_8.md` (+ `.tsv`) | Los 26, uno a uno. |
| `PERFILES_RECUPERADOS.md` | Las tres variantes en 120 perfiles y las semanas de los dos perfiles recuperados. |
| `PLANES_PUBLICADOS_C39.md` | Sesiones bloqueadas, consulta, plan 78 y limitaciones. |
| `REVISION_HUMANA_PRIORIZADA.md` (+ `.tsv`) | Listas A, B y C y la dependencia del 1093. |
| `RIESGOS_Y_ORDEN_DE_FUSION.md` | Riesgos abiertos, orden de fusión con reajuste de bases y recomendaciones previas a producción. |
| `DIFF_PR55_ORIGINAL_VS_REDUCIDA.diff` | Diff exacto. |
| `evidencia/` | Rejillas (pila, 26, 8), plan 78 (26 y 8) y regresiones. |
| `herramientas/` | Matriz, lista de revisión, comparación de variantes y de regresiones. |
