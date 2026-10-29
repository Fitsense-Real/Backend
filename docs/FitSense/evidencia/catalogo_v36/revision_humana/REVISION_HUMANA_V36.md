# V36 · Expediente de revisión humana

**Estado (29/10/2026):**
- **Dictamen técnico asistido por IA:** aprobado por el responsable del proyecto (`APROBACION_DICTAMEN_V36.md`).
- **Cotejo de los GIF:** hecho (`VERIFICACION_GIF_V36.md`). Ocho coinciden con el dictamen. En 1296 la evidencia corrigió la hipótesis del dictamen; tras decidirlo el responsable, pasa a nivel 3 sin alto impacto.
- **Rectificación de 872 frente a V28:** `ANEXO_872_V28.md`.
- **Dos revisiones humanas independientes:** **PENDIENTES**. No hay formularios entregados.

Este expediente no inventa revisores ni resultados: las columnas R1 y R2 quedan vacías hasta que dos personas identificadas entreguen su formulario. El dictamen aprobado no ocupa esas columnas.

## Procedimiento (el de V27/V28)

1. Dos revisores **identificados y distintos** reciben, cada uno por separado, `material_ciego_v36.md` y una copia de `formulario_revisor_v36.csv`. El material no lleva la clasificación anterior ni la propuesta.
2. Cada uno juzga el **GIF original** con el criterio `criterio-dificultad-v5.md` (SHA-256 `0e7caac0…8a59ec`) y anota la huella del archivo que usó.
3. Entregan `formulario_<revisor>_v36.csv` en esta carpeta, con su nombre y la fecha. Se calcula la SHA-256 de cada entrega y se anota abajo.
4. Se completa la tabla de consolidación. Si un revisor contradice la propuesta de V36, **se presenta la discrepancia antes de cambiar la decisión** y se registra el consenso con su justificación.
5. V36 solo pasa a definitiva cuando cada fila tiene consenso. El filtro global de aprobación humana **no se activa**. La revisión no crea eventos en `exercise_difficulty_approvals`; eso exigiría una migración posterior con autorización.

## Entregas

| Revisor | Fecha | Archivo | SHA-256 de la entrega | SHA-256 del criterio usado |
|---|---|---|---|---|
| _(pendiente)_ | | | | |
| _(pendiente)_ | | | | |

## Consolidación

«Anterior» es el estado en V35; «V36» es la propuesta aprobada que aplica la migración. Las columnas R1, R2 y Consenso quedan vacías hasta las entregas.

| Ej. | Atributo | Anterior | V36 | R1 | R2 | Consenso | Justificación visual y funcional |
|---:|---|---|---|---|---|---|---|
| 1267 | activo | sí | **no** | | | | Desde el pino, baja a apoyo con piernas abiertas y vuelve: habilidad gimnástica con inversión y control corporal avanzado. Nivel 1 y `KNEE_DOMINANT` eran incorrectos |
| 1296 | dificultad | 1 | **3** | | | | Flexión de palanca larga (brazos extendidos al frente), de alta exigencia. El GIF no muestra la fase pliométrica que suponía el dictamen. Decisión del responsable, 29/10/2026 |
| 1296 | alto impacto | no | **no (sin cambio)** | | | | No hay fase de vuelo en ninguno de los 12 fotogramas |
| 1296 | instrucciones (es) | Elevación alterna de brazo y pierna | **Flexión de palanca larga** (5 pasos) | | | | Las anteriores describían otro ejercicio |
| 1333 | implemento | ab wheel | **peso corporal** | | | | El GIF es una carrera con zancadas amplias; no hay rueda. V24 asignó el implemento por el nombre |
| 1333 | alto impacto | no | **sí** | | | | Carrera con fase de vuelo |
| 1333 | suelo / brazos arriba | sí / sí | **no / no** | | | | De pie, brazos en braceo de carrera |
| 1333 | nombre (es) | Rodar con rueda | **Carrera con zancadas amplias** | | | | Describe el GIF |
| 1333 | instrucciones (es) | Plancha con rueda abdominal | **Carrera con zancadas amplias** (5 pasos) | | | | Las anteriores describían otro ejercicio |
| 872 | patrón | HIP_EXTENSION | **TRUNK_FLEXION** | | | | Tumbado, lleva las rodillas flexionadas al pecho y despega la pelvis: encogimiento invertido. La cadera no se extiende |
| 872 | nombre (es) | Levantamiento de cadera con rodillas flexionadas | **Encogimiento invertido** | | | | Describe el GIF |
| 872 | instrucciones (es) | Puente de glúteo | **Encogimiento invertido** (5 pasos) | | | | Las anteriores describían un puente de glúteo |
| 1059 | grupo / objetivo | waist / abs | **upper legs / quads** | | | | Zancada con giro de tronco; la carga principal es la pierna adelantada. El giro queda como sinergista (abs) |
| 527 | grupo / objetivo | upper arms / biceps | **upper legs / quads** | | | | Zancada con curl; la rodilla de atrás llega casi al suelo. El curl queda como sinergista |
| 609 | grupo / objetivo | upper arms / biceps | **upper legs / quads** | | | | Ídem |
| 725 | grupo / objetivo | upper arms / biceps | **shoulders / delts** | | | | Curl seguido de press por encima de la cabeza, sentado. El press domina el recorrido; ya tenía `VERTICAL_PUSH` y brazos arriba |
| 760 | grupo / objetivo | upper arms / biceps | **shoulders / delts** | | | | Curl martillo alterno y press por encima de la cabeza, de pie |

## Puntos que la revisión humana debe resolver expresamente

1. **872 y V28:** ver `ANEXO_872_V28.md`. Falta la revisión humana de la nueva identidad (encogimiento invertido). La aprobación de V28 no la cubre.
2. **1296:** confirmar que el GIF muestra una flexión de palanca larga sin fase de vuelo, y el nivel 3.
3. **Banco en 725:** aplazado por decisión del responsable; convención de los ejercicios sentados (`VERIFICACION_GIF_V36.md`).
4. **Rodilla en 527 y 609:** la rodilla de atrás llega casi al suelo y `requires_kneeling=false`. Además, `axial_load` difiere entre los dos. No está en los cambios aprobados.
