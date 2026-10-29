# V36 · Registro de aprobación del dictamen técnico

| Campo | Valor |
|---|---|
| Documento | `Dictamen_revision_V36_y_rectificacion_872_para_aprobacion.md` (en esta carpeta, sin modificar) |
| SHA-256 | `4d2ce598cd8d03e0c56d4c958102e87b66bea8810920c92a50dd8bfcf3072da5` |
| Origen | Evaluación técnica **asistida por IA** sobre fuentes documentales, fechada el 28/10/2026. El propio dictamen indica que sus autores no inspeccionaron los GIF y que sus afirmaciones condicionadas requieren cotejo |
| Aprobación | **Aprobado** por el responsable del proyecto en la «Orden final de aprobación y cierre de V36» (28/10/2026), entregada en la sesión de desarrollo |
| Condiciones de la aprobación | Comprobar los nueve GIF (en especial 1296, 1333 y 872) y el banco de 725. Detener la corrección cuyo GIF contradiga el dictamen |
| Qué NO es | No es una revisión humana independiente ni un consenso ciego R1/R2 del procedimiento V27/V28. Las respuestas de la IA no se registran como evaluaciones de ningún revisor |

## Resultado de las condiciones

| Condición | Resultado |
|---|---|
| GIF de 1267, 1333, 872, 1059, 527, 609, 725 y 760 | Coinciden con el dictamen (`VERIFICACION_GIF_V36.md`) |
| GIF de 1296 | **Lo contradice:** no hay fase pliométrica. Corrección detenida; propuesta en `VERIFICACION_GIF_V36.md` |
| Alto impacto de 1333 | Confirmado: hay fase de vuelo |
| Banco de 725 | Modelable con las relaciones existentes, pero **no se aplica** por coherencia con 704, 723, 726, 739 y 740 (`VERIFICACION_GIF_V36.md`) |
| Rectificación de 872 frente a V28 | `ANEXO_872_V28.md` |

## Estado de la revisión humana

- **Dictamen técnico (IA) aprobado por el responsable:** sí.
- **Dos revisiones humanas independientes (V27/V28):** **PENDIENTES.** El expediente ciego está preparado (`material_ciego_v36.md`, `formulario_revisor_v36.csv`). No hay formularios entregados.
- **Consenso ciego R1/R2:** no registrado.
