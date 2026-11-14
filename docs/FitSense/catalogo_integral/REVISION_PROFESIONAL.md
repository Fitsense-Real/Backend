# Revisión profesional pendiente: bloqueos reales del cierre

## 1. Situación

**No hubo acceso a ningún profesional** (entrenador, fisioterapeuta ni médico) durante este cierre. No se ha inventado ninguna firma ni aprobación. Las únicas validaciones humanas registradas en el sistema son las de V28: aprobación de dificultad de 122, 776, 872 y 878, firmada por Juan y Daniel el 10/10/2026.

Todo lo hecho en el cierre se hizo en tres pasos, todos con IA:
1. Segunda revisión visual independiente de los 1.219 activos con la animación completa: 20 revisores.
2. Verificación independiente de cada propuesta: 18 verificadores.
3. Adjudicación del coordinador.

Es evidencia sólida y trazable, pero **no es una validación profesional**.

Cada decisión del cierre registra:
- **Quién decidió y en qué función:** «revisor R2, lote Lnn», «verificador V3, lote Vnn» y «coordinador (IA)».
- **Evidencia:** fotogramas citados en `cierre/adjudicacion/verificacion3_consolidada.tsv` y `adjudicacion_traza.tsv`.
- **Resultado:** campo `resultado`.

## 2. Lista concreta

**Desde la orden de desbloqueo (14/11/2026), la fuente única es `cierre/REGISTRO_DECISIONES_PENDIENTES.tsv`** (323 filas; ver `REGISTRO_DECISIONES.md`). Incluye las 313 filas de `cierre/REVISION_PROFESIONAL_LISTA.tsv`, que se conserva como entrada histórica, con prioridad, quién decide, estado, valor vigente y una comprobación reproducible por fila.

| Bloque | Ids | Qué hay que decidir |
|---|---|---|
| Dificultad (V27) | 248 ejercicios | `difficulty_level` nunca fue aprobado por personas (V27). R2 propone **bajar** 248, y no se aplica ninguna bajada. Las subidas aplicadas (63) son conservadoras |
| Identidad | 64, 257, 261, 310, 320, 427, 494, 535, 620, 675, 853, 902, 922, 930, 937, 944, 994, 1068, 1086, 1124, 1176, 1177, 1192, 1261, 1294, 1298, 1311 | El GIF no permite confirmar qué ejercicio es. La V3 confirmó la identidad (CLARA) en 64, 620 y 1261, que solo necesitan visto bueno |
| DEC-APOYO-SUSPENDIDO (V53) | 15 marcados (1301 sale: manos en el suelo), más 1301 y los 8 de suelo documentados | Si «no colgarse» debe excluir el apoyo suspendido sobre un aparato sin soporte de pies. V53 solo migra el dato; el comportamiento está apagado hasta vuestra aprobación (`cierre/V53_APOYO_SUSPENDIDO.md`, rama de la PR B) |
| DEC-INV-DECLINADO | 171, 377, 840, 841, con la referencia de 540, 566, 1224 y 1241 | Si un banco declinado, con el peso sobre la espalda, cuenta como inversión. Hoy el catálogo es desigual |
| DEC-HI-LANZAMIENTOS | 1083, 1084, 1085, 1087 | Si el lanzamiento balístico con balón medicinal es pliometría. **1083 ya no es una contradicción abierta:** por la orden de desbloqueo se le añadió HI de forma conservadora (coordinador `APLICAR_CONSERVADOR`), y V52 añade el CHECK «impacto ALTO implica HI». Ya no es elegible para quien no admite impacto. No se bajó su impacto para recuperar elegibilidad. Si el profesional decide que no es pliometría, quitar HI exigirá revisar también su impacto ALTO |
| Decisión previa D2 (V37.1) | 854 | R2 y V3 ven el pie de apoyo siempre en el suelo, pero D2 exige una segunda revisión **humana** antes de quitarle HI |
| Decisión previa V39 | 1113 | R2 y V3 proponen subirlo a nivel 2 (empuje del peso corporal desde la plancha baja), pero el dictamen de V39 fija que «1113 conserva los valores de V39» hasta su verificación. Se mantiene el nivel 1 |
| DEC-FACTIBILIDAD-775 | 775 | **Resuelta por la orden de desbloqueo:** exige banco, como muestra el GIF, y ya no figura como solo peso corporal. Consecuencia, que queda documentada: 14 perfiles GAIN_MUSCLE en casa solo con peso corporal (3–4 días, 58–75 min) quedan sin plan por V20 (`Hipertrofia4Dv34IT`) |
| DEC-HI-CARRERA | 1133, 1134, 1175 | Si la carrera por debajo del sprint lleva HI o solo impacto MODERADO |
| Olímpicos dudosos | 922, 930, 971 | Si el GIF es un clean (HI) o un curl hasta el rack |
| Contención C39 | 131, 400, 448 | Si el curl alto con el brazo a la altura del hombro cuenta como «brazos por encima de la cabeza» |
| Contención INC-4C-03 | 813, 858, 861, 873, 885 (salida), 1192, 1298 (retenidos) | Aprobar la liberación de los cinco primeros |
| Textos no decidibles | 494, 535, 1086, 1294 | Instrucciones que el GIF no permite verificar |
| Banderas no decidibles | 868 (INV), 1177 (HI), 922 (HI) | Fase no visible en el GIF |
| Plan 78 / 916 | 916 | La reparación localizada del jueves exige un sustituto. No se resuelve relajando la dificultad (ver `IMPACTO_PLANES_PUBLICADOS.md`) |

## 3. Paquete para el profesional

- Por ejercicio:
  - la hoja de fotogramas completa: `ej_NNNN.png`, regenerable con `cierre/herramientas/hojas_completas.py`;
  - la ficha final: `REGISTRO_MAESTRO_CATALOGO.xlsx`;
  - las decisiones de R2, V3 y el coordinador.
- Plantilla de respuesta: una fila por id con `decision` (APRUEBA / RECHAZA / MODIFICA), `valor`, `nombre`, `credenciales o funcion`, `fecha` y `observaciones`. Cada respuesta se aplicaría con una migración versionada y protegida, con el mismo generador.

## 4. Consecuencia para el cierre

Mientras esta lista no esté resuelta por una persona con la función adecuada, el catálogo **no puede declararse cerrado**. Es un bloqueo real, no un pendiente menor.
