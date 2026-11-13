# Criterios de cierre del catálogo

| # | Criterio de la orden | Cumplido | Evidencia o bloqueo |
|---|---|---|---|
| 1 | Los 1.219 activos revisados a fondo con la animación completa | **Sí** | R2 sobre los 1.219 (20 lotes) y V3 sobre las 717 propuestas (`REVISION_VISUAL_COMPLETA.md`) |
| 2 | Inventario de los 157 inactivos | **Sí** | `cierre/inactivos/INVENTARIO_INACTIVOS.tsv` |
| 3 | Procedencia de cada atributo | **Sí** | Procedencia por migración (V1–V50, y V1–V53 en la auditoría final) y columnas de origen del registro maestro |
| 4 | El impacto no depende del nombre | **Sí** | V51 y `AtributosExplicitosV51Test`. La regla por nombre solo se usa si falta el dato, y el `CHECK` lo impide en los activos |
| 5 | 853 y 1093 resueltos | **Sí** | Los dos BAJO con evidencia por fotograma (`VALIDACION_SEGURIDAD.md` §4). La identidad de 853 queda AMBIGUA y va a la lista profesional |
| 6 | Apoyo suspendido: 13 + 5 casos | **Parcial** | Dato y código listos (V53), pero el **significado del permiso exige tu aprobación** |
| 7 | Pendientes resueltos uno a uno | **Parcial** | Lo decidible con el GIF está resuelto (556 cambios). Siguen pendientes **281 decisiones profesionales**: 248 de dificultad (V27), 27 de identidad, 4 textos y 2 banderas no decidibles |
| 8 | Segunda pasada independiente | **Sí** | R2 y V3 independientes, más el coordinador |
| 9 | Correcciones en migraciones versionadas y protegidas | **Sí** | V51, V52 y V53 fallan ante un estado previo inesperado |
| 10 | Contenciones reevaluadas con evidencia | **Sí** | `CONTENCIONES_DEFINITIVAS.md`. Ninguna se retira sin decisión: 5 de INC-4C-03 quedan con propuesta de liberación |
| 11 | Coherencia del sistema: generación, adaptación, progresión, reincorporación y planes publicados | **Sí, con hallazgos** | Plan 78: el 910 se suma al 916 como límite de la reparación. La corrección de equipo de 775 dejaría sin plan a GM intermedio en casa con solo peso corporal a 74–75 min, así que **no se aplica** y queda como decisión (`IMPACTO_GENERACION_ADAPTACION.md`) |
| 12 | Regresión profunda | Ver `REGRESION_FINAL.md` | — |
| 13 | Segunda auditoría desde una base nueva | Ver `REGRESION_FINAL.md` §3 | — |
| 14 | Registro maestro de 1.376 filas | **Sí** | `REGISTRO_MAESTRO_CATALOGO.xlsx`. Contrastado campo a campo con la huella de la base real: 0 discrepancias |
| 15 | Controles permanentes | **Sí** | Huella ampliada (impacto, estiramiento, apoyo suspendido y firma de textos), paridad con V51, oráculo de elegibilidad con atributos explícitos y CHECK de V51 |
| 16 | Validación profesional registrada | **No** | **Sin acceso a ningún profesional.** No se ha inventado ninguna firma ni aprobación (`REVISION_PROFESIONAL.md`) |
| 17 | Sin bloqueos relevantes | **No** | Ver la lista de bloqueos |

## Bloqueos que impiden declarar el catálogo cerrado

1. **Validación profesional inexistente:** 310 decisiones listadas en `cierre/REVISION_PROFESIONAL_LISTA.tsv`, entre ellas:
   - `difficulty_level` sin aprobación humana (V27);
   - 248 bajadas propuestas;
   - 27 identidades ambiguas.
2. **Decisiones de significado o de criterio pendientes de tu aprobación:**
   - V53 (apoyo suspendido con «no colgarse»);
   - DEC-INV-DECLINADO;
   - DEC-HI-LANZAMIENTOS;
   - DEC-HI-CARRERA;
   - DEC-FACTIBILIDAD-775;
   - liberación de 5 retenidos de INC-4C-03.
3. **Decisión previa que exige revisión humana:** D2 de V37.1 (854 fuera hasta una segunda revisión humana). Se respeta y no se aplica.
4. **Plan 78:** la reparación no puede publicar mientras la semana contenga ejercicios que subieron de nivel después de publicarse (916, y ahora 910). No se resuelve relajando la dificultad.
5. **Hallazgos colaterales sin segunda mirada:** 231, 620, 1084, 1182, 1303, 1323, 367 y 1017.
