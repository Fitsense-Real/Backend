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

La lista completa, con id, nombre, decisión pendiente y evidencia, está en `cierre/REVISION_PROFESIONAL_LISTA.tsv` (313 filas).

| Bloque | Ids | Qué hay que decidir |
|---|---|---|
| Dificultad (V27) | 248 ejercicios | `difficulty_level` nunca fue aprobado por personas (V27). R2 propone **bajar** 248, y no se aplica ninguna bajada. Las subidas aplicadas (63) son conservadoras |
| Identidad | 64, 257, 261, 310, 320, 427, 494, 535, 620, 675, 853, 902, 922, 930, 937, 944, 994, 1068, 1086, 1124, 1176, 1177, 1192, 1261, 1294, 1298, 1311 | El GIF no permite confirmar qué ejercicio es. La V3 confirmó la identidad (CLARA) en 64, 620 y 1261, que solo necesitan visto bueno |
| DEC-APOYO-SUSPENDIDO (V53) | 16 marcados más los 8 de suelo documentados | Si «no colgarse» debe excluir el apoyo suspendido sobre un aparato (opción 2 de `PROPUESTA_APOYO_SUSPENDIDO.md`) |
| DEC-INV-DECLINADO | 171, 377, 840, 841, con la referencia de 540, 566, 1224 y 1241 | Si un banco declinado, con el peso sobre la espalda, cuenta como inversión. Hoy el catálogo es desigual |
| DEC-HI-LANZAMIENTOS | 1083, 1084, 1085, 1087 | Si el lanzamiento balístico con balón medicinal es pliometría. **1083** queda hoy con una contradicción visible: la V3 aceptó subir su impacto a ALTO y rechazó añadir HI. Como la restricción «sin impacto» del perfil filtra por HI, sigue siendo elegible para quien no admite impacto (regla R03 de `MATRIZ_COHERENCIA_FINAL.xlsx`). No se resuelve en silencio: añadir HI exige la V3 y quitar el impacto ALTO relajaría P-C2. Decide la misma DEC |
| Decisión previa D2 (V37.1) | 854 | R2 y V3 ven el pie de apoyo siempre en el suelo, pero D2 exige una segunda revisión **humana** antes de quitarle HI |
| Decisión previa V39 | 1113 | R2 y V3 proponen subirlo a nivel 2 (empuje del peso corporal desde la plancha baja), pero el dictamen de V39 fija que «1113 conserva los valores de V39» hasta su verificación. Se mantiene el nivel 1 |
| DEC-FACTIBILIDAD-775 | 775 | El equipo correcto es banco (codos sobre apoyos elevados), pero aplicarlo deja sin plan a GAIN_MUSCLE intermedio en casa solo con peso corporal a 74–75 min. Hay que decidir si se acepta esa pérdida |
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
