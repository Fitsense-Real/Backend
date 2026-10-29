# Auditoría integral del catálogo y propuesta V37 · Informe

**Fecha:** 29/10/2026 · **Rama:** `auditoria-catalogo` (desde `generation` `8543a20`, catálogo V1–V36).

**Estado:** propuesta.
- **V37 no está en `src/main/resources/db/migration` y no se ha aplicado en Neon.** No hubo despliegues ni llamadas a Replicate.
- Todo se ejecutó en bases PostgreSQL 16 aisladas: una copia local y bases de Testcontainers.
- No se modificaron V15, V20, V22, V23, V24 ni V25, ni las políticas de dificultad y capacidades, ni la adaptación, la progresión o los prompts.

Sustituye como entrega a `INFORME_CALIDAD_CATALOGO.md`, que se conserva como primera entrega.

## 1. Cobertura visual acumulada

**Revisados: 1.220 de 1.220 activos (100 %).**
- 1.212 en esta auditoría: fase 2 y lotes A–D.
- 8 activos ya revisados en V36. 1267, también revisado en V36, ya está inactivo.

Cada ejercicio se juzgó con ocho fotogramas equiespaciados de su GIF original. Los veredictos son:
- `CONFIRMADO`: el GIF contradice el atributo sin ambigüedad.
- `PROBABLE`: la contradicción es clara, pero hay más de una clasificación razonable.
- `PENDIENTE_CRITERIO`: hace falta una regla funcional.
- `SIN_ERROR`: el GIF coincide con los atributos.

| Lote | Ejercicios | Confirmado | Probable | Pendiente de criterio | Sin error |
|---|---:|---:|---:|---:|---:|
| Fase 2: prioridad | 55 | 26 | 4 | 3 | 22 |
| Fase 2: grupo y patrón | 30 | 8 | 9 | 12 | 1 |
| Fase 2: muestra aleatoria | 40 | 6 | 0 | 1 | 33 |
| Fase 2: usuario 31 | 17 | 3 | 0 | 0 | 14 |
| A: sospecha media (pendientes) | 83 | 25 | 8 | 19 | 31 |
| B: nivel 1 con riesgo de restricciones | 375 | 72 | 8 | 31 | 264 |
| C: sospecha baja | 158 | 75 | 0 | 38 | 45 |
| D: sin sospechas automáticas | 454 | 63 | 2 | 25 | 364 |
| **Total** | **1.212** | **278** | **31** | **129** | **774** |

**Evidencia:**
- `revision_visual/matriz_revision_visual_completa.tsv`: una fila por ejercicio y atributo, con lote, veredicto, gravedad, valor actual, valor según el GIF, discrepancia previa y descripción visual.
- `revision_visual/lotes/`: los identificadores de cada lote y las notas.
- Las hojas de fotogramas se regeneran con `revision_visual/hoja_fotogramas.py`. No se versionan porque son derivados de los GIF, que tienen copyright.

**Límite:** es una revisión visual asistida por IA. **No sustituye la doble revisión humana independiente**, que sigue pendiente. No se inventan revisores ni resultados.

**Las alertas automáticas no bastaban:**
- En el lote D, sin ninguna alerta, hubo 63 ejercicios con error confirmado.
- En total, los activos sin sospecha automática acumulan 119 errores confirmados. Es coherente con la estimación de la primera entrega: unos 110, con intervalo de 52 a 212.

| Gravedad automática (fase 1) | Marcados | Con error confirmado | Falsos positivos (sin error) | Sin decidir |
|---|---:|---:|---:|---:|
| ALTA | 41 | 20 | 14 | 7 |
| MEDIA | 117 | 36 | 33 | 48 |
| BAJA | 263 | 103 | 102 | 58 |
| Sin sospecha | 791 | 119 | 625 | 47 |

## 2. Errores confirmados por gravedad y dimensión

**278 ejercicios con algún error confirmado.** Por gravedad máxima: 98 alta, 154 media y 26 baja. En total son 325 errores, porque un ejercicio puede tener varios.

| Dimensión | Alta | Media | Baja | Total |
|---|---:|---:|---:|---:|
| Equipamiento (implemento que se usa y no se exige, o al revés) | 0 | 152 | 14 | 166 |
| Seguridad (banderas) | 98 | 0 | 26 | 124 |
| Patrón | 0 | 13 | 0 | 13 |
| Dificultad | 7 | 3 | 0 | 10 |
| Nombre e instrucciones | 0 | 1 | 5 | 6 |
| Grupo muscular | 0 | 6 | 0 | 6 |

**Errores graves por bandera** (número de ejercicios):

| Bandera | Ejercicios |
|---|---:|
| Suelo (`requires_floor`) que falta | 52 |
| Brazos arriba que faltan | 32 |
| Rodillas que faltan | 29 |
| Habilidad avanzada en nivel 1 | 7 |
| Suspensión que falta | 4 |
| Inversión que falta | 3 |

Los casos que nombraste:
- **776:** suspensión invertida en anillas.
- **1374:** cuelga de una barra; no es cardio ni tiene fase de vuelo.
- **1320:** se hace sentado en el suelo.

Familias repetidas:
- curls de muñeca «over bench», de rodillas en el suelo;
- remos invertidos tumbados (910, 911, 914, 316);
- curls nórdicos con apoyo de rodillas;
- maltesas y banderas humanas en nivel 1 (860, 1298, 813).

## 3. Discrepancias históricas

- **V28, ejercicio 776:** anexo de rectificación en `v37/ANEXO_776_V28.md`, con el mismo esquema que el de 872.
  - Se conservan intactos la migración, el evento `APPROVED` de nivel 1, los revisores y los resultados. `CatalogoV37PropuestaIT` comprueba que `exercise_difficulty_approvals` es idéntica antes y después.
  - La aprobación se dio con material de texto, que describe una bisagra de pie. El GIF muestra otra cosa.
  - Queda **pendiente de tu decisión el patrón de 776**: mantener `HIP_HINGE`, `OTHER`, `VERTICAL_PULL` o desactivarlo. La recomendación técnica es desactivarlo o usar `OTHER`.
- **V25:** 45 ejercicios con algún valor de V25 contradicho por el GIF (`v37/DISCREPANCIAS_V25.md`).
  - **26 tienen una bandera de seguridad que falta:** 1320, 825, 1089, 1297, 1082, 1083, 910, 911, 914, 913, entre otros.
  - El resto son restricciones de más o casos probables.
  - Cada corrección se justifica con la definición vigente de V10 o V25. No se reescribe V25 ni se le atribuye un error de revisión: en muchos casos el texto de la fuente describe otra cosa que el GIF.

## 4. Matriz de correcciones candidatas

Generada solo a partir de los veredictos `CONFIRMADO` (`v37/generar_v37.py`), en `v37/correcciones_v37.tsv`. Cada fila lleva la columna, el valor antes, el valor después, el bloque y la evidencia.

| Bloque | Contenido | Cambios | Ejercicios |
|---|---|---:|---:|
| **V37 · A1** | Banderas de seguridad que faltan: suelo, rodillas, brazos arriba, suspensión e inversión | 120 | 98 |
| **V37 · A2** | Dificultad: habilidades avanzadas en nivel 1, pasan a nivel 3 | 10 | 10 |
| **V37 · A3** | Aparato de los ejercicios de suspensión o gimnasia: anillas, barra, correas | 16 | 8 |
| **V37 · A4** | El resto de la corrección de 1374: patrón y alto impacto | 2 | 1 |
| V37.1 · B1 | Grupo muscular y patrón confirmados (afectan a V15, V23 y 4C) | 25 | 19 |
| V37.1 · B2 | Implemento usado y no exigido: banco, step, cajón, paralelas, multipower… | 151 | ~120 |
| V37.1 · B3 | Restricciones de más: banderas en `true` que el GIF no justifica | 32 | ~25 |

En total, V37 toca 102 ejercicios y V37.1 toca 154.

Los confirmados que no se pueden expresar con el esquema actual quedan fuera de las dos (`v37/no_incluidas_v37.tsv`, 41 casos):
- 34 usan un implemento que no existe en `equipment_types`: banco Scott, espaldera, GHD, rack, poste, escalón, compañero;
- 6 piden redactar nombre o instrucciones;
- 1 no tiene traducción directa.

**SQL de la propuesta:**
- `v37/migracion/V37__catalog_safety_corrections.sql`: bloque A.
- `v37/separables/V37_1__catalog_classification_equipment.sql`: bloque B.

Ambos comprueban, antes de cambiar nada, el `source_code`, el `name_en` y el valor anterior de cada columna, y el estado final al terminar. Si algo no coincide, abortan sin cambios. Respetan dos restricciones de la base: que la suspensión implique brazos arriba, y que el implemento principal esté entre los requisitos (V23/V29).

**Pruebas de seguridad de la propuesta** (`CatalogoV37PropuestaIT`, 3 de 3 en verde):
- aplica exactamente la matriz y ningún otro ejercicio cambia;
- V27 y V28 quedan intactas;
- suspensión implica brazos arriba;
- reaplicar la migración aborta sin cambios;
- una edición previa hace abortar V37.1 sin cambios.

## 5. Casos que necesitan una decisión funcional o humana

Están en `v37/DECISIONES_PENDIENTES.md`. No entran en ninguna migración.

| Criterio | Ejercicios |
|---|---:|
| C1 Convención de los sentados: ¿sentarse en un banco o cajón exige banco? (aplazada en V36) | 74 |
| C2 Balón o bosu apoyado en el suelo: ¿cuenta como suelo? | 5 |
| C3 Dificultad según el criterio v5: suspensiones, dominadas, curls nórdicos, sissy… | 30 |
| C4 Ejercicios combinados: ¿qué movimiento domina? (precedente de V36) | 14 |
| C5 Remo al mentón: ¿`HORIZONTAL_PULL` (`UPPER_PULL`) o elevación de hombros? | 6 |
| C6 Grupo o patrón con más de una opción razonable (push-ups con patada, remos en sentadilla 301/302…) | 38 |
| C7 Lanzamientos sin fase de vuelo: ¿alto impacto? | 4 |
| C8 Banderas probables, para confirmar con una segunda mirada | 18 |
| Patrón de 776 | 1 |
| Implementos que no existen en el esquema | 34 |

## 6. Impacto simulado

Detalle completo en `v37/resultados_simulacion.md`, generado por `v37/comparar_v37.py`.
- **Catálogos simulados:** V36 frente a V37, y frente a V37 + V37.1. La propuesta se carga con `spring.flyway.locations`; las pruebas existentes no cambian.
- **Pruebas nuevas:** `GeneracionReferenciaCandidataIT`, `ImpactoRestriccionesIT` y el modo con la propuesta de `Usuario31ReproduccionIT`.
- V36 simulado es **idéntico** a la referencia oficial de V36.

| Prueba | V36 | V37 (A) | V37 + V37.1 |
|---|---|---|---|
| 72 generaciones de referencia: válidas | 72 | **72** (68 perfiles y 180 sesiones cambian) | 72 (70 perfiles y 207 sesiones cambian) |
| Barrido de 4 a 6 días (72 perfiles): válidos | 72 | **72** | 70 (**2 sin plan**) |
| Robustez (288 generaciones): válidas | 288 | **288** | 280 (**8 sin plan**) |
| Continuidad semanal (8 LOSE_WEIGHT de 5–6 días y 96 perfiles) | correcta | **correcta** | **1 fallo** |
| 192 perfiles con restricciones: planes con un ejercicio que su perfil excluye | **124** | **0** | 0 |
| 192 perfiles con restricciones: elegibles prohibidos | 2.332 en 184 perfiles | **0** | 0 |
| 192 perfiles con restricciones: sin plan / 4C no factible o frágil | 0 / 0 | **0 / 0** | 0 / 0 |
| `LOWER_HIP` en perfiles sin suelo o sin suspensión (96): mínimo y media de elegibles | 6 y 20,3 | **5 y 18,4** (ninguno con 0 a 2) | 5 y 18,4 |

**Lectura:**

1. **Seguridad.** Con el catálogo actual:
   - 124 de 192 perfiles reciben en su plan un ejercicio que su restricción, su nivel o su equipamiento excluyen;
   - incluso sin restricciones, 11 de 24 lo reciben, por la dificultad o por un aparato que no tienen.

   Con V37, cero, y ningún perfil se queda sin plan. **No hizo falta relajar ninguna restricción.**
2. **`LOWER_HIP` sin suelo o sin suspensión.** Pierde de 1 a 2 ejercicios elegibles de media, entre ellos 776. Todos los perfiles mantienen al menos 5 elegibles: la cobertura sigue robusta.
3. **V37.1 tiene un coste que hay que decidir aparte.**
   - Los perfiles de casa (peso corporal, mancuerna y banda) reciben hoy ejercicios cuyo implemento no tienen: paralelas en 777, step en 1197 y 896, banco en 314. Pasa en **98 de 192** perfiles con V36 y en **93** con V37.
   - V37.1 lo corrige, pero entonces **LOSE_WEIGHT principiante de casa con 4 o 5 días** queda sin plan. Sus sesiones de tren inferior duran unos 30 minutos y el mínimo de V20 es 31 (70 % de 45): falta **1 minuto**.
   - Es el defecto de redondeo de V20 ya documentado («V20 al 70 % exacto»). No se ha tocado V20, como indicaste.
   - Hay alternativa segura: un ejercicio más del enfoque o una serie más. Pero exige corregir V20 o la composición de la sesión, no los datos.

## 7. Usuario 31 (mismo perfil, sin relajar restricciones)

Reproducción con las respuestas guardadas de Replicate y el plan 63 (`Usuario31ReproduccionIT`):

| Caso | V36 | V37 | V37 + V37.1 |
|---|---|---|---|
| ¿776 y 1320 son elegibles? | sí | **no** | no |
| Intento 1 | V18 (1325 dos días seguidos) y V23 (falta `LOWER_HIP`, con 13 elegibles) | **Igual** (11 elegibles) | Igual |
| Intento 2 | V15: 4 de 6 en upper legs | **Igual** | Igual |
| Intento 3 | V15: 3 de 5 en upper legs | **Igual** | Igual |
| Plan 63 publicado | VÁLIDO, aunque incumplía suelo y suspensión | **Rechazado por V1** (776 y 1320 ya no elegibles) | Rechazado por V1 y V15 (1103 pasa a upper legs) |
| Motor de reglas para su perfil | Publica, **con 776 y 1320** | **Publica sin ellos** | Publica sin ellos |

**Conclusión:**
- El catálogo explica las dos violaciones del plan 63.
- No explica ninguno de los tres rechazos de Replicate. Persisten con los datos corregidos: son errores de selección de la IA.
- Queda documentado. No se modifican prompts ni reglas, y no se harán llamadas pagadas hasta cerrar las correcciones de datos y aprobar una prueba controlada.

## 8. Recomendación

**Entrar en V37** (bloque A, 102 ejercicios, 148 cambios):
- todas las banderas de seguridad confirmadas que faltan;
- las 10 habilidades avanzadas a nivel 3;
- el aparato de los 8 ejercicios de suspensión;
- la corrección completa de 1374.

En la simulación elimina las 124 violaciones sin dejar ningún perfil sin plan ni romper referencias, barrido, robustez ni continuidad. Requisitos previos:
- tu aprobación de los cambios concretos (`correcciones_v37.tsv`, bloque A);
- tu decisión sobre el anexo de 776;
- la promoción de las 72 referencias candidatas de V37 (`resultados_simulacion.md`).

**Separar en V37.1, no ahora:**
- **B2, implemento no exigido:** es correcto y mejora la factibilidad real de los planes de casa, pero deja sin plan a LOSE_WEIGHT principiante en casa por el defecto de V20. Debe ir junto con, o después de, la corrección de V20, que no se ha iniciado.
- **B1, grupo y patrón:** cambia lo que cuentan V15 y 4C. Por ejemplo, el plan 63 pasaría a fallar V15 por 1103. Conviene aprobarlo con la decisión de los combinados (C4) y de los remos en sentadilla (C6).
- **B3, restricciones de más:** amplía la elegibilidad. Es seguro según el GIF, pero no es urgente.

**No entran en ninguna migración todavía:** los 31 probables, los 129 pendientes de criterio y los 41 confirmados sin traducción directa en el esquema.

## 9. Archivos

| Archivo | Contenido |
|---|---|
| `revision_visual/matriz_revision_visual_completa.tsv` | Veredictos de los 1.212 ejercicios |
| `revision_visual/lotes/ids`, `revision_visual/lotes/notas` | Lotes A–D y notas de la revisión |
| `v37/generar_v37.py` | Genera la matriz de correcciones y los dos SQL desde la matriz visual |
| `v37/correcciones_v37.tsv`, `v37/no_incluidas_v37.tsv` | Antes y después, con evidencia; confirmados que no entran |
| `v37/migracion/V37__…sql`, `v37/separables/V37_1__…sql` | Propuesta. No está en `db/migration` |
| `v37/ANEXO_776_V28.md`, `v37/DISCREPANCIAS_V25.md`, `v37/DECISIONES_PENDIENTES.md` | Discrepancias históricas y decisiones |
| `v37/comparar_v37.py`, `v37/resultados_simulacion.md` | Simulación y comparación |
| `CatalogoV37PropuestaIT`, `ImpactoRestriccionesIT`, `GeneracionReferenciaCandidataIT`, `Usuario31ReproduccionIT` | Pruebas: la primera corre siempre; las demás son opt-in |
