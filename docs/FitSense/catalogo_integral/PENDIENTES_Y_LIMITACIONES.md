# Pendientes y limitaciones de la revisión

**El catálogo NO está completamente saneado.** Sobre los 1.219 activos, al cierre de esta auditoría:

| Estado final | Ejercicios | Qué falta |
|---|---|---|
| VERIFICADO | 552 | Nada técnico. Tienen revisión visual por IA, sin validación profesional. |
| CORREGIDO | 261 | Nada técnico. La corrección está aplicada y probada. |
| CONTENIDO | 16 | Decisión profesional para levantar la contención: INC-CAT-C39 (8), INC-4C-03 (7) e INC-CAT-1374 (1). |
| PENDIENTE_VISUAL | 59 | El GIF no permite describir el movimiento o el texto con seguridad (vista cenital, fase no mostrada, agarre no visible…). |
| PENDIENTE_PROFESIONAL | 320 | Decisión especializada (desglose abajo). |
| PENDIENTE_TECNICO | 11 | Inconsistencia de equipamiento comprobada y sin resolver. |

El estado de cada ejercicio, con su motivo, está en `REGISTRO_MAESTRO_CATALOGO.xlsx` (filtro «Estado final»). Prioridad: CONTENIDO > PENDIENTE_VISUAL > PENDIENTE_PROFESIONAL > PENDIENTE_TECNICO > CORREGIDO > VERIFICADO. Un ejercicio corregido que aún tiene otro aspecto pendiente figura como pendiente, para no ocultarlo.

## PENDIENTE_PROFESIONAL (320): desglose

Un ejercicio puede tener varios motivos.

| Motivo | Ejercicios |
|---|---|
| Dificultad en duda (el revisor no pudo decidir) | 109 |
| Clasificación (patrón o grupo) en duda | 88 |
| Bandera que el GIF podría exigir y no está marcada, en duda (sobre todo AX en cargas ligeras y OH límite) | 65 |
| Cambio de dificultad propuesto y no aplicado (sobre todo bajadas: curls y curls de muñeca en d3) | 39 |
| Banderas de más, no relajadas (la regla es no relajar sin revisión): 108, 142, 147, 148, 262, 296, 496, 533, 549, 550, 608, 609, 618, 711, 797, 822, 830, 888, 930, 952 | 18 |
| Apoyo suspendido sin clase en el esquema (ver `PROPUESTA_APOYO_SUSPENDIDO.md`) | 18 |
| Subida de dificultad V48 a confirmar | 10 activos (más 1192, retenido) |
| Clasificación NO no aplicada por duda (97, 320, 1086) | 4 |

## PENDIENTE_VISUAL (59)

54, 65, 207, 214, 231, 257, 292, 303, 305, 306, 320, 465, 494, 535, 580, 608, 620, 665, 666, 675, 913, 930, 935, 937, 940, 971, 973, 976, 981, 982, 984, 997, 1019, 1032, 1033, 1034, 1041, 1068, 1078, 1084, 1120, 1121, 1123, 1126, 1142, 1180, 1184, 1185, 1188, 1190, 1191, 1214, 1215, 1223, 1294, 1299, 1321, 1322, 1324.

Casos de seguridad entre ellos:
- **937** «Muscle up kipping»: el GIF solo muestra el balanceo.
- **913**: el GIF no aclara cómo se queda cabeza abajo.
- **1294**: giro colgado.
- **54**: no se distingue si hay fase de vuelo.
- **65** back lever: falta la entrada a la posición.
- **675** «pliométrica»: el GIF no muestra el salto.

Todos conservan su texto y sus banderas anteriores; no se inventó nada.

## PENDIENTE_TECNICO (11)

70, 456, 490, 521, 556, 571, 796, 1072, 1156, 1157 y 1338: accesorios que el GIF usa y no están en los requisitos (arm blaster, palo, asiento con respaldo, anclaje o balón). La regla aplicada en V50 es exigir solo lo imprescindible. Estos casos necesitan decidir si el accesorio lo es.

## CONTENIDO (16)

- INC-CAT-C39, solo con la PR #55: 1153, 1301, 1303, 1304 y 1306 (apoyo suspendido); 131, 400 y 448 (curl alto).
- INC-4C-03: 813, 858, 861, 873, 885, 1192 y 1298.
- INC-CAT-1374: 1374.

## Limitaciones de la revisión

1. **La revisión visual la hicieron revisores de IA**: 10 lotes, con el mismo protocolo y sobre 8 fotogramas más la imagen fija de cada GIF original. No es una revisión humana ni profesional.
   - El coordinador verificó de forma independiente las 26 banderas de V47, las 9 reclasificaciones de V50, los 26 ejercicios de la PR #55 y una muestra de los textos.
   - Los textos de V49 se redactaron con un protocolo estricto (solo lo que muestra el GIF) y pasaron controles automáticos: reglas por nombre intactas, sin contradicción con las banderas, formato y advertencias conservadas.
   - Aun así, un profesional debería revisar una muestra antes de considerarlos definitivos.
2. **Ocho fotogramas** no capturan todo: la fase de vuelo, el agarre o una vista cenital quedan a veces sin resolver. Por eso existen los PENDIENTE_VISUAL y las banderas en duda.
3. **Dificultad:** según V27, la dificultad la aprueban dos revisores humanos. Esta auditoría solo sube de forma preventiva 12 habilidades claramente avanzadas y no baja ninguna.
4. **Duplicados:** 18 nombres repetidos entre activos, registrados en `nombres_duplicados_conocidos.tsv`. No se fusionan ni se eliminan ids. Queda pendiente decidir su equivalencia; la prueba impide duplicados nuevos.
5. **`is_isolation`** no se revisó sistemáticamente. El lote L01 señaló curls marcados como isométricos (`iso=true`) siendo dinámicos.
6. **Planes publicados:** sin acceso a la base real. La matriz de impacto es por código y se acompaña de una consulta de exposición para ejecutarla en producción.
7. **Instrucciones en inglés (`instructions_en`):** se conservan las de la fuente, como trazabilidad, aunque en 372 casos no describen el GIF. La app usa `instructions_es`.

## Qué impide declarar una versión completamente auditada

- 59 ejercicios sin verificación visual posible con el material disponible.
- 320 decisiones profesionales: banderas en duda o de más, apoyo suspendido, dificultad y clasificación dudosa.
- 11 inconsistencias técnicas de equipamiento.
- 16 ejercicios contenidos por seguridad, sin decisión de levantar la contención.
- Ninguna validación profesional registrada.
