# Criterios de cierre del catálogo

| # | Criterio de la orden | Cumplido | Evidencia o bloqueo |
|---|---|---|---|
| 1 | Los 1.219 activos revisados a fondo con la animación completa | **Sí** | R2 sobre los 1.219 (20 lotes) y V3 sobre las 717 propuestas (`REVISION_VISUAL_COMPLETA.md`) |
| 2 | Inventario de los 157 inactivos | **Sí** | `cierre/inactivos/INVENTARIO_INACTIVOS.tsv` |
| 3 | Procedencia de cada atributo | **Sí** | Procedencia por migración (V1–V50, y V1–V53 en la auditoría final) y columnas de origen del registro maestro |
| 4 | El impacto no depende del nombre | **Sí** | V51 y `AtributosExplicitosV51Test`. La regla por nombre solo se usa si falta el dato, y el `CHECK` lo impide en los activos |
| 5 | 853 y 1093 resueltos | **Sí** | Los dos BAJO con evidencia por fotograma (`VALIDACION_SEGURIDAD.md` §4). La identidad de 853 queda AMBIGUA y va a la lista profesional |
| 6 | Apoyo suspendido: 13 + 5 casos | **Parcial** | Dato y código listos (V53), pero el **significado del permiso exige tu aprobación** |
| 7 | Pendientes resueltos uno a uno | **Parcial** | Lo decidible con el GIF está resuelto (561 cambios en 442 ejercicios). De los 320/59/11 pendientes anteriores se resuelven 181/39/10. Siguen **313 decisiones** en la lista profesional (291 activos) |
| 8 | Segunda pasada independiente | **Sí** | R2 y V3 independientes, más el coordinador |
| 9 | Correcciones en migraciones versionadas y protegidas | **Sí** | V51, V52 y V53 fallan ante un estado previo inesperado |
| 10 | Contenciones reevaluadas con evidencia | **Sí** | `CONTENCIONES_DEFINITIVAS.md`. Ninguna se retira sin decisión: 5 de INC-4C-03 quedan con propuesta de liberación |
| 11 | Coherencia del sistema: generación, adaptación, progresión, reincorporación y planes publicados | **Sí, con hallazgos** | Plan 78: el 910 se suma al 916 como límite de la reparación. La corrección de equipo de 775 dejaría sin plan a GM intermedio en casa con solo peso corporal a 74–75 min, así que **no se aplica** y queda como decisión. HALL-PROG-01: p02 de progresión falla desde #56 (`IMPACTO_GENERACION_ADAPTACION.md`) |
| 12 | Regresión profunda | **Sí, con 2 fallos ajenos al cierre** | En las dos ramas: 1.454 unitarias y 722 de integración. Fallan solo las 2 de HALL-PROG-01, que vienen de #56 (`REGRESION_FINAL.md`) |
| 13 | Segunda auditoría desde una base nueva | **Sí** | V1–V53 una a una: V52 = 442 decididos y V53 = 16 (`cierre/auditoria_final/`) |
| 14 | Registro maestro de 1.376 filas | **Sí** | `REGISTRO_MAESTRO_CATALOGO.xlsx`. Contrastado campo a campo con la huella de la base real: 0 discrepancias |
| 15 | Controles permanentes | **Sí** | Huella ampliada (impacto, estiramiento, apoyo suspendido y firma de textos), paridad con V51, oráculo de elegibilidad con atributos explícitos, CHECK de V51 y matriz de 13 reglas (`MATRIZ_COHERENCIA_FINAL.xlsx`). La matriz encontró 6 contradicciones texto/prescripción, ya corregidas, y la de 1083, que sigue abierta |
| 16 | Validación profesional registrada | **No** | **Sin acceso a ningún profesional.** No se ha inventado ninguna firma ni aprobación (`REVISION_PROFESIONAL.md`) |
| 17 | Sin bloqueos relevantes | **No** | Ver la lista de bloqueos |

## Bloqueos que impiden declarar el catálogo cerrado

1. **Validación profesional inexistente:** 313 decisiones listadas en `cierre/REVISION_PROFESIONAL_LISTA.tsv`, entre ellas:
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
3. **Decisiones previas que exigen revisión humana:**
   - D2 de V37.1: 854 fuera hasta una segunda revisión humana.
   - Dictamen de V39: 1113 conserva sus valores hasta su verificación.

   Las dos se respetan y no se aplican.
4. **Plan 78:** la reparación no puede publicar mientras la semana contenga ejercicios que subieron de nivel después de publicarse (916, y ahora 910). No se resuelve relajando la dificultad.
5. **Contradicciones materiales abiertas en elegibles:**
   - 1083: impacto ALTO sin HI, decide DEC-HI-LANZAMIENTOS;
   - hallazgos colaterales sin R2 ni V3: 231, 620, 1323, 367, 1182 y 1017. 1084 ya está resuelto y 1303 está contenido.
6. **HALL-PROG-01**, desde #56: `ProgresionIT` y `ProgresionRev2IT` p02 fallan por la herencia de la dosis progresada en el sustituto (defecto latente de la adaptación) y la composición del catálogo de #56. Hay que decidirlo antes de fusionar #56 (`IMPACTO_GENERACION_ADAPTACION.md` §3).
