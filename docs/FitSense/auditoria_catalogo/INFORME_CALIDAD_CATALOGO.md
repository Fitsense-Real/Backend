# Auditoría del catálogo de ejercicios · Informe de calidad (fases 1 y 2)

**Fecha:** 29/10/2026 · **Rama:** `auditoria-catalogo` (desde `generation` `8543a20`, catálogo V1–V36)

**Estado:** solo diagnóstico.
- No hay V37 ni ninguna migración nueva.
- No se tocó Neon ni ninguna base compartida: las consultas corrieron sobre una copia local V1–V36 y sobre bases Testcontainers desechables.
- No hubo llamadas a Replicate ni despliegues.

## 1. Resumen

| Medida | Valor |
|---|---:|
| Ejercicios en el catálogo | 1.376 |
| Activos evaluados por la auditoría automática (fase 1) | **1.220** |
| Activos con alguna sospecha automática | 427 (42 de gravedad alta, 122 media, 263 baja) |
| Activos con revisión visual del GIF (fase 2) | **150** (142 en esta auditoría y 8 activos ya revisados en V36) |
| Ejercicios con **error confirmado** en el GIF | **43**: 26 de gravedad alta, 16 media, 1 baja |
| Errores confirmados (un ejercicio puede tener varios) | 57: 27 de gravedad alta, 25 media, 5 baja |
| Error probable (falta una segunda mirada) | 13 |
| Pendiente de un criterio que hay que decidir (dificultad, combinados) | 16 |
| Activos **pendientes de revisión visual** | **1.070**: 83 sospechas de gravedad media, 260 de gravedad baja y 727 sin sospechas |

Conclusiones principales:

1. **El catálogo tiene errores de seguridad reales y frecuentes.**
   - En la muestra aleatoria de ejercicios **sin ninguna sospecha automática**, 6 de 40 tenían un error confirmado. Cuatro eran banderas de seguridad.
   - Extrapolado a los 727 activos sin sospecha que quedan, son unos **110 ejercicios con algún error** (IC 95 % 52–212). De ellos, unos **73 tienen una bandera de seguridad mal puesta** (IC 95 % 29–168).
   - La auditoría por texto no basta: 11 de los 26 errores graves confirmados no los marcó ninguna regla de gravedad alta.
2. **El catálogo no causó los tres rechazos de Replicate del usuario 31.**
   - Con el catálogo corregido se reproducen exactamente los mismos rechazos (V18 y V23 en el intento 1; V15 en los intentos 2 y 3).
   - Fueron errores de selección de la IA.
3. **El catálogo sí causó dos violaciones de restricciones en el plan 63, el que se publicó.**
   - 776 «Elevador» es una suspensión invertida en anillas y se dio a un perfil sin suspensión.
   - 1320 «Dip de tríceps en piso» se hace sentado en el suelo y se dio a un perfil sin suelo.
   - El validador no las detecta porque las banderas dicen lo contrario.
   - Con las correcciones, el motor de reglas sigue publicando un plan válido para este perfil, sin relajar restricciones.
4. **Impacto en la generación determinista.**
   - En los 72 planes de referencia de V36, **49 contienen al menos un ejercicio con un error grave confirmado**. La causa es sobre todo 776, que aparece en 40 planes, incluidos perfiles de casa sin anillas y principiantes.
   - Además, **68 de 72** contienen algún ejercicio con un error confirmado de cualquier gravedad, sobre todo 1103 (en 49 planes) y 142 (en 32).
5. **Hay dos tipos de discrepancias con revisiones humanas anteriores** (sección 6):
   - **V25** fijó en `false` el suelo de 1320, 825, 1089, 1297, 1301 y 1082.
   - **V28** aprobó 776 con nivel 1 a partir de material de texto.

   No se modificó nada: es una decisión del responsable.

## 2. Método

### Fase 1 · Auditoría automática (los 1.220 activos)

- **Herramientas:** `exportar_catalogo.sql` (solo lectura, `BEGIN TRANSACTION READ ONLY … ROLLBACK`) y `auditar_catalogo.py`.
- **Qué hacen las reglas:** buscan contradicciones entre el nombre y las instrucciones de la fuente y los atributos, y entre unos atributos y otros.
- **Dimensiones:**

| Dimensión | Reglas |
|---|---|
| Seguridad | S1–S9: suelo, rodillas, suspensión, brazos arriba, inversión, impacto, carga axial y restricciones de más |
| Dificultad | D1–D3: habilidades avanzadas en nivel bajo; suspensión, inversión o impacto en nivel 1 |
| Grupo muscular y patrón | G1–G5: patrón incoherente con la zona, músculo con la zona, nombre con la zona, ejercicios combinados y `OTHER` |
| Equipamiento | E1–E2: el nombre o las instrucciones mencionan un implemento que no se exige |
| Instrucciones | I2: activo sin `instructions_es` |
| Integridad | N1–N5: nombres e instrucciones duplicados, prescripción frente a patrón, suspensión sin aparato, activo sin GIF |

- Las definiciones de las banderas son las de V10 y V25. Por ejemplo, `requires_floor` significa «exige bajar al suelo y levantarse»: tumbarse en un banco no cuenta como suelo.
- **Gravedad:**
  - **ALTA:** puede exponer a un participante a algo que su perfil excluye, o dar a un principiante una habilidad avanzada.
  - **MEDIA:** cambia lo que cuentan el motor y los validadores: grupo para V15, slot 4C para V23, capacidad o equipamiento exigido.
  - **BAJA:** texto, integridad o restricciones de más (excluyen sin necesidad, pero no son inseguras).
- La primera pasada marcaba 831 de 1.220 ejercicios, demasiado ruido. Se ajustaron las reglas: tumbado en banco no es suelo, «grip» no indica antebrazo, «let your arms hang» no es suspensión. Se retiró una regla de texto (I1) porque las instrucciones de la fuente casi nunca repiten el nombre de la acción.

**Resultado** (`fase1/resumen.md`, detalle en `fase1/hallazgos.tsv` y `fase1/por_ejercicio.tsv`):

| Regla | Gravedad | Ejercicios |
|---|---|---:|
| D1 habilidad avanzada en nivel bajo | ALTA | 11 |
| S1 suelo sin bandera | ALTA | 17 |
| S3 suspensión sin bandera | ALTA | 3 |
| S4 brazos arriba sin bandera | ALTA | 5 |
| S5 inversión sin bandera | ALTA | 1 |
| S6 impacto sin bandera | ALTA | 8 |
| D2 suspensión o inversión en nivel 1 | MEDIA | 34 |
| E1 el nombre menciona un implemento que no se exige | MEDIA | 12 |
| G1 patrón incoherente con la zona | MEDIA | 82 |
| G3 nombre incoherente con la zona | MEDIA | 31 |
| G4 ejercicio combinado | MEDIA | 8 |
| N4 suspensión sin aparato exigido | MEDIA | 8 |
| S7 carga axial sin bandera | MEDIA | 3 |
| Reglas de gravedad baja (D3, E2, G5, N1–N3, S9) | BAJA | 328 apariciones |

**Precisión de las reglas** (medida con la fase 2):
- De los 41 ejercicios marcados ALTA (sin contar 1333, ya corregido en V36), **15 tienen un error grave confirmado** y 20 algún error confirmado.
- Las reglas sirven para priorizar, no para decidir.

### Fase 2 · Revisión visual de los GIF

Cada ejercicio se juzgó con ocho fotogramas equiespaciados del GIF original. Las hojas se generan con `revision_visual/hoja_fotogramas.py`; no se versionan porque son derivados con copyright.

| Lote | Ejercicios | Criterio de selección |
|---|---:|---|
| Prioridad | 55 | Los 42 marcados ALTA y los ejercicios del usuario 31 señalados por el responsable o por las reglas (1320, 1103, 776, 301, 94, 1185, 1077, 505, 309, 791, 1004, 1003, 144, 139, 985 y 1254) |
| Grupo y patrón | 30 | Sospechas G1/G3/G4 que cuentan para un slot 4C o para un grupo de V15, con nivel 1–2 |
| Muestra aleatoria | 40 | Al azar entre los 793 activos sin ninguna sospecha (`random.seed(20261029)`); sirve para estimar lo que las reglas no ven |
| Usuario 31 (resto) | 17 | Los demás ejercicios de las tres propuestas y del plan 63 |

**Matriz completa:** `revision_visual/matriz_revision_visual.tsv`. Columnas: veredicto, gravedad, atributo, valor actual, valor según el GIF, discrepancia previa, relación con el usuario 31 y evidencia.

**Veredictos:**
- `CONFIRMADO`: el GIF contradice el atributo sin ambigüedad.
- `PROBABLE`: la contradicción es clara, pero la clasificación correcta admite más de una opción.
- `PENDIENTE_CRITERIO`: hace falta decidir un criterio, por ejemplo la dificultad según v5 o qué movimiento domina en un combinado.
- `SIN_ERROR`: el GIF coincide con los atributos.

> **Limitación:** es una revisión visual asistida, hecha en el desarrollo con ayuda de IA. **No es la doble revisión humana independiente** y no la sustituye. Los veredictos son la propuesta que esa revisión tendría que confirmar.

| Lote | Revisados | Confirmado | Probable | Pendiente de criterio | Sin error |
|---|---:|---:|---:|---:|---:|
| Prioridad | 55 | 26 | 4 | 3 | 22 |
| Grupo y patrón | 30 | 8 | 9 | 12 | 1 |
| Muestra aleatoria | 40 | 6 | 0 | 1 | 33 |
| Usuario 31 (resto) | 17 | 3 | 0 | 0 | 14 |
| **Total** | **142** | **43** | **13** | **16** | **70** |

## 3. Errores confirmados de gravedad alta (26 ejercicios)

| Ej. | Nombre de la fuente | Error | El GIF muestra |
|---:|---|---|---|
| 776 | elevator | Suspensión, brazos arriba e inversión en `false`; nivel 1; peso corporal | Suspensión invertida en **anillas**, cabeza abajo: habilidad gimnástica (nivel 3) |
| 1374 | wind sprints | Suspensión y brazos arriba en `false`; peso corporal; `LOCOMOTION_CARDIO` | **Colgado de una barra** elevando las rodillas; no es cardio ni tiene fase de vuelo |
| 1320 | triceps dips floor | Suelo en `false` | Sentado en el suelo, manos en el suelo |
| 825 | exercise ball one leg prone lower body rotation | Suelo en `false` | Manos en el suelo, piernas sobre el balón |
| 1089 | medicine ball supine chest throw | Suelo en `false` | Tumbado boca arriba en el suelo |
| 1161 | reverse hyper extension (on stability ball) | Suelo en `false` | Manos en el suelo |
| 1297 | suspended push-up | Suelo en `false` | Pies en el suelo, posición de flexión |
| 1301 | suspended reverse crunch | Suelo en `false` y, de más, suspensión en `true`; exige barra | Manos en el suelo, pies en las correas |
| 1225 | smith hip raise | Suelo en `false` | Tumbado en el suelo |
| 1125 | pull-in (on stability ball) | Suelo en `false` | Manos en el suelo |
| 1199 | side hip abduction | Suelo en `false` | Tumbado de lado en el suelo |
| 1082 | medicine ball chest push multiple response | Suelo y rodillas en `false` | De rodillas en el suelo; cae sobre las manos |
| 309 | bench hip extension | Rodillas en `false`; las instrucciones describen un hip thrust | Rodilla y manos en el banco (patada de glúteo) |
| 606 | dumbbell kickback | Rodillas en `false`; no exige banco | Rodilla y mano en el banco |
| 916 | inverse leg curl (on pull-up cable machine) | Rodillas en `false` | Rodillas en la almohadilla (curl nórdico) |
| 1055 | modified push up to lower arms | Rodillas en `false` | De rodillas en el suelo |
| 68, 254, 882, 1308 | toe touch (male) | Brazos arriba en `false` | De pie, brazos por encima de la cabeza (no son de suelo) |
| 1185 | side-to-side toe touch (male) | Brazos arriba en `false` | Ídem |
| 514 | dumbbell bench seated press | Brazos arriba en `false` | Press sentado por encima de la cabeza |
| 393 | cable reverse-grip straight back seated high row | Brazos arriba en `false`; `HORIZONTAL_PULL` | Jalón desde polea alta (`VERTICAL_PULL`) |
| 456 | chest and front of shoulder stretch | Brazos arriba en `false` | Palo o banda por encima de la cabeza |
| 1058 | modified hindu push-up (male) | Inversión en `false` | Posición de pica, cabeza bajo la cadera |
| 974 | l-sit on floor | Nivel 1 | L-sit: habilidad avanzada al alcance de un principiante con suelo permitido |

**Errores de gravedad media confirmados**, por tipo:
- **Equipamiento no exigido** (14): banco en 314, 1146, 243, 606, 718 y 1255; cajón en 791; anillas en 776, 937, 1068 y 1192; barra en 1374; correas en 1301; espaldera, que no está modelada, en 301.
- **Grupo** (2): 1103 waist → upper legs; 142 back → upper legs.
- **Patrón** (5):
  - 449 `KNEE_DOMINANT` → `ELBOW_FLEXION`;
  - 853 `KNEE_DOMINANT` → `LOCOMOTION_CARDIO`;
  - 59 y 1374 `LOCOMOTION_CARDIO` → tronco;
  - 393 → `VERTICAL_PULL`.
- **Dificultad** (3): nivel 1 → 3 en 461, 937 y 1068. El alto impacto ya los excluye para principiantes.

**Errores probables y casos pendientes de criterio:** ver la matriz. Destacan:
- 301 y 302: «remo en sentadilla» con espaldera, que hoy cuentan como `LOWER_KNEE`.
- Los push-ups con patada (1111, 1113, 1115, 1262) y 1106, clasificados como upper legs o waist.
- Los combinados 145, 439, 558, 567, 796 y 671. El precedente de V36 diría que domina el tren inferior.
- La dificultad de 56, 64, 862, 916, 1192, 1201 y 1305, según el criterio v5.

## 4. Usuario 31 (perfil sin cambios)

**Perfil:** BEGINNER, GENERAL_FITNESS, 21 años, MIXED, sábado y domingo, 45 minutos. Sin alto impacto, sin suelo y sin suspensión; rodillas y brazos arriba permitidos.

**Hallazgos en sus 33 ejercicios** (revisados todos):

| Ej. | Dónde aparece | Hallazgo | Efecto |
|---:|---|---|---|
| 776 | Plan 63 | Suspensión invertida en anillas (sin banderas, nivel 1) | **Violación de la restricción de suspensión** |
| 1320 | Plan 63 | Sentado en el suelo (suelo en `false`) | **Violación de la restricción de suelo** |
| 1103 | Plan 63 | Sentadilla clasificada como waist/abs | V15 contaba 3 de 7 en upper legs; corregido serían 4 de 7 |
| 456 | Plan 63 | Brazos arriba sin bandera | Sin efecto para este perfil (brazos arriba permitidos) |
| 301 | Plan 63 | Remo con espaldera clasificado `KNEE_DOMINANT` | Cubre `LOWER_KNEE` con un ejercicio de tracción (probable) |
| 1077 | Plan 63 | El GIF no lleva banda; las instrucciones sí | Solo texto |
| 1185 | Intento 1 | Brazos arriba sin bandera | Sin efecto para este perfil |
| 393 | Intentos 1–3 | Jalón vertical con brazos arriba, clasificado `HORIZONTAL_PULL` | Sin efecto para este perfil (sigue siendo `UPPER_PULL`) |
| 791 | Intentos 1–2 | No exige el cajón | El perfil tiene banco |
| 309 | Intento 3 | Rodilla en el banco sin bandera | Sin efecto para este perfil (rodillas permitidas) |
| 1255 | Intento 3 | No exige banco | El perfil tiene banco |
| Los otros 22 | — | Sin error (94, 505, 1003, 1004, 548, 436, 1325…) | — |

**Reproducción local de las respuestas guardadas** (`Usuario31ReproduccionIT`):
- Sin Replicate, con las tres salidas JSON del registro y el plan 63 tal como se publicó.
- Corre sobre una base Testcontainers V1–V36 desechable.
- Pasa por el validador de producción.

| Caso | Catálogo actual (V36) | Catálogo con las correcciones confirmadas (solo en la base de prueba) |
|---|---|---|
| Intento 1 | V18 (1325 en días consecutivos) y V23 (falta `LOWER_HIP`, con 13 elegibles) | **Igual**: V18 y V23 (12 elegibles, porque 776 deja de serlo) |
| Intento 2 | V15: 4 de 6 en upper legs (791, 1003, 1004, 94) | **Igual** |
| Intento 3 | V15: 3 de 5 en upper legs (309, 1003, 1004) | **Igual** |
| Plan 63 | **VÁLIDO** | Rechazado: V1 (776 y 1320 ya no son elegibles) y V15 (4 de 7 en upper legs, con 1103) |
| Motor de reglas para el mismo perfil | Publica; vuelve a elegir **776 y 1320** | **Publica** un plan válido sin 776 ni 1320 |

Las correcciones aplicadas en el escenario corregido son las confirmadas de 776, 1320, 1103, 1185 y 309. No es una migración.

**Conclusiones sobre el usuario 31:**
- Los rechazos de la IA no se deben al catálogo:
  - repitió 1325 dos días seguidos;
  - no cubrió `LOWER_HIP` teniendo 13 opciones;
  - concentró los ejercicios de pierna en el domingo.
- 94 (tabla de equilibrio) cuenta como upper legs, pero aun sin él el intento 2 seguiría en 3 de 5.
- El plan publicado, en cambio, **sí incumplía dos restricciones del perfil por errores del catálogo**.
- El motor de reglas no da hoy exactamente el plan 63: hay sustituciones dentro del mismo patrón, como 1098 por 992. La prueba no compara planes completos; afirma la elegibilidad y la presencia o ausencia de 776 y 1320.

## 5. Impacto esperado de corregir

1. **Seguridad.**
   - Hoy los perfiles que declaran «sin suelo», «sin rodillas», «sin suspensión» o «sin brazos arriba» pueden recibir ejercicios que lo exigen. Solo en lo revisado hay 26 casos.
   - La muestra aleatoria indica que quedan decenas más (sección 1).
   - El validador no puede detectarlo: confía en las banderas.
2. **Planes de referencia** (`generacion_referencia.txt`, 72 perfiles de V36): 49 contienen un error grave confirmado y 68 un error confirmado de cualquier gravedad.

   | Ejercicio | Planes en que aparece |
   |---|---:|
   | 1103 | 49 |
   | 776 | 40 |
   | 142 | 32 |
   | 1058 | 8 |
   | 1225 | 7 |
   | 301 | 5 |

   **Cuidado:** corregir 776 y 1103 cambiará planes de referencia. 776 es hoy uno de los pocos `LOWER_HIP` sin suelo (V28 lo aprobó por eso). Corregirlo reducirá la factibilidad de `LOWER_HIP` en perfiles sin suelo. Hay que medirlo en la fase 4 antes de migrar.
3. **V23 y slots 4C.**
   - Hay ejercicios de tren superior que hoy cubren slots de tren inferior: 301, 302, 449, 1086 y 776 como «bisagra».
   - El motor los usa en días de tren superior para cumplir V23.
   - Corregirlos vuelve más estricta la cobertura real. Puede hacer falta revisar la factibilidad de algunos perfiles; eso es la fase 4, no un cambio en V23.
4. **V15.** Corregir 1103, 142 y los push-ups clasificados como upper legs cambia los recuentos por grupo. Algunos planes publicados hoy dejarían de cumplir la regla de la mitad (como el plan 63).
5. **Capacidades.** 59 y 1374 cuentan hoy como CARDIO sin serlo. Para perfiles con pocos ejercicios de locomoción, el veredicto de CARDIO puede empeorar al corregirlos.

## 6. Discrepancias con revisiones humanas anteriores (sin modificar)

| Ej. | Revisión anterior | Lo que muestra el GIF | Decisión necesaria |
|---:|---|---|---|
| 776 | **V28**: aprobación de nivel 1 (Juan y Daniel, 10/10/2026), `HIP_HINGE`, «único de los cuatro que no exige suelo». El material de revisión era de texto (`revision-LOWER_HIP.txt`); las instrucciones describen una bisagra de pie | Suspensión invertida en anillas | Igual que 872 (`ANEXO_872_V28.md`): **no se tocan los registros de V28**. Una corrección posterior tendría que añadir un anexo y, si procede, un evento nuevo, nunca reescribir el existente |
| 1320, 825, 1089, 1297, 1082 | **V25** (hoja revisada `FitSense-banderas-revision-FINAL.xlsx`) fijó el suelo en `false` (y las rodillas de 1082) | Trabajo en el suelo | Confirmar con la definición de V10 («exige bajar al suelo y levantarse») |
| 1301 | **V25**: suspensión y brazos arriba en `true`, suelo en `false` | No cuelga; manos en el suelo | Ídem |

## 7. Pendiente

- **Revisión visual de 1.070 activos.** Orden propuesto:
  1. las 83 sospechas de gravedad media que quedan;
  2. los ejercicios elegibles para perfiles con alguna restricción (suelo, rodillas, suspensión, brazos arriba) y nivel 1;
  3. el resto.

  La tasa de la muestra aleatoria justifica revisar todo, no solo lo marcado.
- **Criterios que debe fijar el responsable:**
  - qué movimiento domina en los ejercicios combinados (extender o no el precedente de V36);
  - la dificultad v5 de las habilidades marcadas;
  - si «sentado en un banco» exige banco (la convención de los sentados, aplazada en V36);
  - cómo modelar los apoyos que no existen como equipamiento (espaldera, cajón para sentarse).
- **Doble revisión humana:** sigue pendiente, como en V36. No se inventan revisores ni resultados.
- **1333:** `instructions_en` sigue describiendo la rueda abdominal. V36 corrigió solo `instructions_es`. Es de gravedad baja.

## 8. Siguientes pasos (cada uno requiere aprobación)

1. Aprobar o ajustar los veredictos `CONFIRMADO` de la matriz y decidir las discrepancias de la sección 6.
2. **Fase 3:** matriz de corrección y propuesta de migración (V37), **sin aplicarla**.
3. **Fase 4:** con la propuesta aplicada solo en bases de prueba, repetir:
   - la generación determinista (72 referencias y el barrido de 4 a 6 días);
   - la cobertura de capacidades;
   - la continuidad semanal (96 perfiles);
   - `Usuario31ReproduccionIT`.
4. **Fase 5:** volver a evaluar la IA. Los rechazos del usuario 31 ya indican que el problema de selección persiste con datos correctos (V18, V23 y V15).

## Archivos

| Archivo | Contenido |
|---|---|
| `exportar_catalogo.sql` | Exportación del catálogo, solo lectura |
| `auditar_catalogo.py` | Reglas de la fase 1 |
| `fase1/resumen.md`, `fase1/hallazgos.tsv`, `fase1/por_ejercicio.tsv` | Salida de la fase 1 sobre V1–V36 |
| `revision_visual/matriz_revision_visual.tsv` | Veredictos de la fase 2 (142 ejercicios) |
| `revision_visual/hoja_fotogramas.py` | Genera las hojas de fotogramas para repetir la revisión |
| `src/test/java/.../Usuario31ReproduccionIT.java` | Reproducción del usuario 31, con el catálogo actual y con el corregido |
