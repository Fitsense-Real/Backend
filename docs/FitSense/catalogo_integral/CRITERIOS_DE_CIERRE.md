# Criterios de cierre del catálogo

| # | Criterio de la orden | Cumplido | Evidencia o bloqueo |
|---|---|---|---|
| 1 | Los 1.219 activos revisados a fondo con la animación completa | **Sí** | R2 sobre los 1.219 (20 lotes) y V3 sobre las 717 propuestas (`REVISION_VISUAL_COMPLETA.md`) |
| 2 | Inventario de los 157 inactivos | **Sí** | `cierre/inactivos/INVENTARIO_INACTIVOS.tsv` |
| 3 | Procedencia de cada atributo | **Sí** | Procedencia por migración (V1–V50, y V1–V53 en la auditoría final) y columnas de origen del registro maestro |
| 4 | El impacto no depende del nombre | **Sí** | V51 y `AtributosExplicitosV51Test`. La regla por nombre solo se usa si falta el dato, y el `CHECK` lo impide en los activos |
| 5 | 853 y 1093 resueltos | **Sí** | Los dos BAJO con evidencia por fotograma (`VALIDACION_SEGURIDAD.md` §4). La identidad de 853 queda AMBIGUA y va a la lista profesional |
| 6 | Apoyo suspendido: 13 + 5 casos | **Parcial** | V53 con criterio conservador: 15 marcados (1301 sale) y **comportamiento apagado** hasta tu aprobación del significado (`cierre/V53_APOYO_SUSPENDIDO.md`) |
| 7 | Pendientes resueltos uno a uno | **Parcial** | Lo decidible con el GIF está resuelto: 563 cambios en 442 ejercicios. Registro único de **323 decisiones** (`REGISTRO_DECISIONES.md`): 309 piden un profesional, 6 al propietario y a un profesional, y 6 otra ronda técnica |
| 8 | Segunda pasada independiente | **Sí** | R2 y V3 independientes, más el coordinador |
| 9 | Correcciones en migraciones versionadas y protegidas | **Sí** | V51, V52 y V53 fallan ante un estado previo inesperado |
| 10 | Contenciones reevaluadas con evidencia | **Sí** | `CONTENCIONES_DEFINITIVAS.md`. Ninguna se retira sin decisión: 5 de INC-4C-03 quedan con propuesta de liberación |
| 11 | Coherencia del sistema: generación, adaptación, progresión, reincorporación y planes publicados | **Sí, con hallazgos documentados** | HALL-PROG-01 corregido en el motor (`HALL_PROG_01.md`). 775 aplicado: 14 perfiles de casa solo con peso corporal quedan sin plan (V20, legítimo). Plan 78: el 910 y el 916 siguen siendo el límite de la reparación |
| 12 | Regresión profunda | **Sí, sin fallos** | PR A: 1.468 unitarias y 722 de integración. PR B: 1.468 y 727. #62: 1.431 y 617. **0 fallos** y CI en verde en las tres (`REGRESION_FINAL.md`) |
| 13 | Segunda auditoría desde una base nueva | **Sí** | V1–V53 una a una (`cierre/auditoria_final/`); ver `REGRESION_FINAL.md` §4 |
| 14 | Registro maestro de 1.376 filas | **Sí** | `REGISTRO_MAESTRO_CATALOGO.xlsx`. Contrastado campo a campo con la huella de la base real: 0 discrepancias |
| 15 | Controles permanentes | **Sí** | Huella ampliada, paridad con V51, oráculo de elegibilidad, CHECK de V51, **CHECK «ALTO implica HI» en V52** y matriz de 13 reglas. R03 (1083) ya no tiene incumplimientos |
| 16 | Validación profesional registrada | **No** | **Sin acceso a ningún profesional.** No se ha inventado ninguna firma ni aprobación (`REVISION_PROFESIONAL.md`) |
| 17 | Sin bloqueos relevantes | **No** | Ver la lista de bloqueos |

## Bloqueos que impiden declarar el catálogo cerrado

1. **Validación profesional inexistente.** 309 decisiones del registro único piden a un profesional (`cierre/REGISTRO_DECISIONES_PENDIENTES.tsv`): 43 de seguridad, 248 de dificultad (V27), 27 identidades y textos no verificables.
2. **Decisiones de significado o de criterio pendientes de tu aprobación:**
   - V53 (apoyo suspendido con «no colgarse»), con el interruptor apagado;
   - DEC-INV-DECLINADO, DEC-HI-LANZAMIENTOS (con 1083 ya contenido) y DEC-HI-CARRERA;
   - liberación de 5 retenidos de INC-4C-03.
3. **Decisiones previas que exigen revisión humana:** D2 (854) y el dictamen de V39 (1113). Se respetan.
4. **Plan 78:** la reparación no publica mientras la semana contenga el 916 y el 910. No se resuelve relajando la dificultad.
5. **Contradicciones materiales abiertas en elegibles:**
   - 1294, con un texto erróneo, no es elegible en la rejilla;
   - 1323, por su texto;
   - 231 y 367, por su clasificación.

   Se resuelven con otra ronda técnica (R2 + V3).
6. **Consecuencia de 775:** los 14 perfiles sin plan. Está documentada y no se ha ocultado.

HALL-PROG-01 y la contradicción de 1083 **ya no bloquean**: están corregidos y verificados (`DESBLOQUEO_CIERRE.md`).
