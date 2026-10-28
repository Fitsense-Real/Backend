# V36 · Expediente de revisión humana

**Estado: PENDIENTE.** No hay ninguna revisión registrada. Este expediente no inventa revisores ni resultados: las columnas de revisor quedan vacías hasta que dos personas identificadas entreguen su formulario.

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
| 1296 | dificultad | 1 | **3** | | | | Flexión pliométrica: manos y pies dejan el suelo a la vez |
| 1296 | alto impacto | no | **sí** | | | | Fase de vuelo y aterrizaje sobre las manos |
| 1333 | implemento | ab wheel | **peso corporal** | | | | El GIF es una carrera con zancadas amplias; no hay rueda. V24 asignó el implemento por el nombre |
| 1333 | alto impacto | no | **sí** | | | | Carrera con fase de vuelo |
| 1333 | suelo / brazos arriba | sí / sí | **no / no** | | | | De pie, brazos en braceo de carrera |
| 1333 | nombre (es) | Rodar con rueda | **Carrera con zancadas amplias** | | | | Describe el GIF |
| 872 | patrón | HIP_EXTENSION | **TRUNK_FLEXION** | | | | Tumbado, lleva las rodillas flexionadas al pecho y despega la pelvis: encogimiento invertido. La cadera no se extiende |
| 872 | nombre (es) | Levantamiento de cadera con rodillas flexionadas | **Encogimiento invertido** | | | | Describe el GIF |
| 1059 | grupo / objetivo | waist / abs | **upper legs / quads** | | | | Zancada con giro de tronco; la carga principal es la pierna adelantada. El giro queda como sinergista (abs) |
| 527 | grupo / objetivo | upper arms / biceps | **upper legs / quads** | | | | Zancada con curl; la rodilla de atrás llega casi al suelo. El curl queda como sinergista |
| 609 | grupo / objetivo | upper arms / biceps | **upper legs / quads** | | | | Ídem |
| 725 | grupo / objetivo | upper arms / biceps | **shoulders / delts** | | | | Curl seguido de press por encima de la cabeza, sentado. El press domina el recorrido; ya tenía `VERTICAL_PUSH` y brazos arriba |
| 760 | grupo / objetivo | upper arms / biceps | **shoulders / delts** | | | | Curl martillo alterno y press por encima de la cabeza, de pie |

## Puntos que la revisión debe resolver expresamente

1. **872 y V28.** V28 registró a 872 como APROBADO de nivel 1 en el slot LOWER_HIP (revisión de Juan y Daniel, 10/10/2026). Ese material era **texto**, y las instrucciones del catálogo de 872 describen un **puente de glúteo**: V28 anota que coinciden en un 92,8 % con las de 1073 (*low glute bridge*). El GIF muestra un encogimiento invertido. La aprobación de V28 es de **dificultad**, no de patrón, y V36 no la toca. Pero si V36 se confirma, LOWER_HIP pasa de 4 a 3 aprobados (122, 776, 878). Los revisores deben confirmar qué describe el GIF y si la aprobación de V28 sigue teniendo sentido.
2. **Instrucciones contradictorias**, fuera de las columnas aprobadas, así que V36 no las cambia:
   - **1333:** sigue diciendo «manos sobre el rodillo»;
   - **872:** describe un puente de glúteo;
   - **1296:** describe una flexión con elevación alterna de brazo y pierna;
   - **1267:** describe una sentadilla con los brazos arriba, pero ya queda inactivo.

   El participante vería el nombre y el GIF nuevos con un texto que no corresponde.
3. **Banco en 725.** El GIF es sentado en banco y `exercise_equipment_requirements` no lo exige. No está entre los cambios aprobados.
4. **Rodilla en 527 y 609.** La rodilla de atrás llega casi al suelo, y `requires_kneeling=false` en los dos. Además, `axial_load` es `false` en 527 y `true` en 609 para el mismo patrón. No está entre los cambios aprobados.
