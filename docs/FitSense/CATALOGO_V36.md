# V36 · Corrección del catálogo según los GIF originales

**Fecha:** 29/10/2026 · **Rama:** `catalogo-v36` → `generation` (PR #4) · **Migración:** `V36__catalog_review_gif_corrections.sql`.

**Estado:** lista para aprobación final, con estos pendientes (§11):
- revisión humana doble e independiente;
- decisión sobre 1296;
- defecto de continuidad del PR #3 (no causado por V36).

**No se ha hecho:** no se ha ejecutado en Neon ni desplegado, no se ha llamado a Replicate y nada se ha fusionado.

**No cambia:** V15, V20, V22, V24, V25, V35, adaptación, progresión, D8, adherencia, seguridad, roles, prompts ni `src/main/java`.

## 1. Resumen

| Criterio de cierre | Resultado |
|---|---|
| El catálogo representa correctamente los ejercicios | 8 de 9 corregidos y cotejados con su GIF. **1296 detenido:** el GIF contradice la condición del dictamen (§3) |
| Las rutinas nuevas se generan | 72/72 referencias VALIDA. Barrido 4–6 días: 72/72 con el PR #3 (60/72 en la base, por el defecto que corrige el PR #3). Robustez: 0/288 sin plan con el PR #3 |
| Las rutinas nuevas continúan sin incompatibilidades **nuevas del catálogo** | 72/72 continúan en la base con V36. En la combinación con el PR #3, 8 planes de LOSE_WEIGHT de 5–6 días no continúan **por un defecto del PR #3**, reproducido sin V36 (§8) |
| La evidencia de V28 se mantiene íntegra y bien interpretada | V28 intacta. 872 ya no cuenta para LOWER_HIP: los aprobados pasan de 4 a 3. Anexo de rectificación (§5) |

## 2. Migración V36 definitiva

| Id | Columna | V35 | V36 |
|---:|---|---|---|
| 1267 | `is_active` | true | **false** (no se borra) |
| 1333 | `equipment_id` / requisito | ab wheel / `1:ab wheel` | **body weight / ninguno** |
| 1333 | `high_impact` · `requires_floor` · `requires_overhead` | false · true · true | **true · false · false** |
| 1333 | `name_es` | Rodar con rueda | **Carrera con zancadas amplias** |
| 1333 | `instructions_es`, `instruction_steps_es` | Rueda abdominal | **Carrera** (§4) |
| 872 | `movement_pattern` | HIP_EXTENSION | **TRUNK_FLEXION** (`waist/abs` y nivel 1 sin cambio) |
| 872 | `name_es` | Levantamiento de cadera con rodillas flexionadas | **Encogimiento invertido** |
| 872 | `instructions_es`, `instruction_steps_es` | Puente de glúteo | **Encogimiento invertido** (§4) |
| 1059 | `body_part` / `target_muscle` / `synergist_muscle` | waist / abs / quadriceps | **upper legs / quads / abs** |
| 527 | ídem | upper arms / biceps / forearms | **upper legs / quads / biceps** |
| 609 | ídem | upper arms / biceps / quadriceps | **upper legs / quads / biceps** |
| 725 | ídem | upper arms / biceps / shoulders | **shoulders / delts / biceps** |
| 760 | ídem | upper arms / biceps / forearms | **shoulders / delts / biceps** |

**Sin cambios:**
- **1296** (§3).
- **Banco en 725** (§6).
- Patrones de 1059, 527 y 609 (`KNEE_DOMINANT`) y de 725 y 760 (`VERTICAL_PUSH`, con brazos arriba).
- `instructions_en`, el texto de la fuente: la app muestra `instructions_es`.

**Controles de la migración:**
- **Precondición estricta:** valores anteriores exactos, la huella MD5 de las instrucciones y el requisito único de 1333.
- **Poscondición:** el estado final y que `instructions_es` sea los pasos unidos, como en los 1324 ejercicios con pasos.
- **Fallo controlado verificado en PostgreSQL aislado:** aplicarla dos veces o alterar antes las instrucciones la aborta sin aplicar nada.
- **Nada más se mueve:** las otras 1368 filas, los requisitos y el historial V27/V28 dan huellas idénticas antes y después.

## 3. Cotejo de los GIF

Detalle en `evidencia/catalogo_v36/revision_humana/VERIFICACION_GIF_V36.md`, reproducible con `cotejo_gif_v36.py`. Lo hizo el asistente de desarrollo (IA) sobre los GIF originales; no es una revisión humana.

**1267, 1333, 872, 1059, 527, 609, 725 y 760 coinciden con el dictamen:**
- **1333:** fase de vuelo en los fotogramas 5–8 y 22–26, así que el alto impacto queda confirmado.
- **872:** encogimiento invertido; la cadera no se extiende.
- **725:** sentado en un banco con respaldo.

**1296 lo contradice:**
- Es una flexión de **palanca larga**, con los brazos extendidos al frente.
- Manos y pies no se despegan del suelo en ninguno de los 12 fotogramas (y=107–109 en todos), así que no hay fase pliométrica.
- Según la orden, **se detiene su corrección**.
- **Propuesta pendiente:** nivel 3 sin alto impacto, con instrucciones de palanca larga. Mientras no se decida, 1296 sigue como en V35, elegible para principiantes.

## 4. Instrucciones finales (`instruction_steps_es`)

**1333 · Carrera con zancadas amplias**
1. Ponte de pie con el tronco ligeramente inclinado hacia delante y los brazos flexionados a unos 90 grados.
2. Impúlsate con una pierna y avanza con una zancada larga, llevando la rodilla contraria hacia delante y arriba.
3. En cada zancada los dos pies se despegan del suelo un instante: aterriza suave sobre la parte delantera del pie, con la rodilla ligeramente flexionada.
4. Coordina los brazos con las piernas: adelanta el brazo contrario a la pierna que avanza.
5. Encadena zancadas de forma continua durante el tiempo indicado, con el tronco firme y la respiración regular, en una superficie plana y con espacio libre delante.

**872 · Encogimiento invertido**
1. Túmbate boca arriba con las rodillas flexionadas, los pies apoyados en el suelo y los brazos a los lados, con las palmas hacia abajo.
2. Activa el abdomen y despega los pies del suelo, llevando las rodillas hacia el pecho sin estirar las piernas.
3. Al final del recorrido, enrolla la pelvis y despega ligeramente la cadera del suelo con la fuerza del abdomen, sin impulsarte con las piernas.
4. Baja la cadera y después los pies de forma controlada hasta volver a apoyarlos en el suelo.
5. Mantén la cabeza y los hombros apoyados durante todo el ejercicio y repite el número de repeticiones indicado.

**1296:** sin cambio (§3).

## 5. 872 frente a V28

Rectificación completa en `revision_humana/ANEXO_872_V28.md`.

- **V28 queda intacta:** migración, evento `APPROVED` de nivel 1, revisores (Juan y Daniel) y resultados.
- **El evento de V28 no prueba la nueva identidad.** Se revisó sobre un texto de puente de glúteo.
- **LOWER_HIP pasa de 4 a 3 aprobados** (122, 776, 878). La vista de V27 toma el patrón del catálogo, y 872 ya es `TRUNK_FLEXION`.
- **La aprobación de nivel no se transfiere.** El filtro global de aprobación humana sigue **apagado**.
- **Nivel operativo:** 872 sigue en nivel 1.
- **Eventos:** ninguno nuevo, ni de revisión ni de revocación.
- **Pendiente:** la revisión humana de 872 como encogimiento invertido.
- **Condición para encender el filtro en el futuro:** registrar antes esa revisión o un REVOKED, con el contrato de V27.

`CatalogoV36IT` comprueba los conteos, la integridad de V28 y el filtro apagado.

## 6. Banco del ejercicio 725

- **Lo que muestra el GIF:** un banco con respaldo.
- **Lo que permite el modelo:** exigirlo sin cambiar nada, con un grupo 2 de `exercise_equipment_requirements` (como 514 y 720).
- **Por qué no se aplica:**
  - los otros sentados con mancuerna (704, 723, 726, 739 y 740) también aparecen en banco en su GIF y ninguno lo exige;
  - la convención de V24 lo añadió solo cuando el nombre dice «bench».
- **Qué pasaría si se exigiera solo en 725:** un perfil de casa perdería 725 y conservaría 739, que tiene la misma postura.
- **Alternativa mínima:** fijar la convención para los 52 ejercicios activos «seated» sin implemento de apoyo, en una migración posterior.

## 7. Las 72 referencias

- **Oficial desde V36:** `evidencia/progresion/generacion_referencia.txt`, protegida por `GeneracionReferenciaIT`.
- **V35, como evidencia:** `generacion_referencia_v35.txt`. `GeneracionReferenciaV35IT` comprueba con Flyway hasta V35 que se sigue reproduciendo, es decir, que el código de generación no cambió.

| Medida | V35 | V36 |
|---|---:|---:|
| Perfiles VALIDA | 72/72 | **72/72** |
| Cobertura de capacidades | COMPLETO 72 | **COMPLETO 72** |
| Sesiones con otro día o enfoque | — | **0** |
| Sesiones con otros ejercicios o dosis | — | 194 de 216 (70 perfiles) |
| · directas (contienen alguno de los nueve) | — | 75 |
| · indirectas (selección determinista) | — | 119 |
| Sesiones con otros minutos estimados | — | 23 (±4 min) |
| Por debajo del 70 % exacto (V20, sin cambiar) | 17 | 16 |
| Colocaciones que la clasificación correcta no admite | 51 de 1267 en el barrido; 21–31 combinados en PULL | **0** |

**Por qué cambian sesiones que no contenían ninguno de los nueve.** El motor siembra con `userId × 1 000 003 + díaDeLaSemana` y sortea sobre listas ordenadas de candidatos. 1267 era elegible para los 72 perfiles: al desactivarlo, todas las listas pierden un elemento y la misma semilla elige otros índices. No se modificó el algoritmo.

**Comparado con la simulación anterior (213 sesiones):** ahora son 194, porque 1296 no cambia.

Tablas completas en `evidencia/catalogo_v36/resultados_comparacion.md`.

## 8. Barrido, robustez y continuidad

| Medida | Base sin V36 | Base + V36 | PR #3 sin V36 | PR #3 + V36 |
|---|---:|---:|---:|---:|
| Barrido 4–6 días: perfiles VALIDA | 60/72 | 60/72 | 72/72 | **72/72** |
| Sesiones = días pedidos | 44/60 | 44/60 | 56/72 | 56/72 |
| Robustez 288: sin plan | 52 | 52 (los mismos) | 0 | **0** |
| 1267 colocado / combinados en PULL | 44 / 21 | 0 / 0 | 51 / 31 | **0 / 0** |
| Dificultad o impacto inadmisibles | 0 | 0 | 0 | 0 |
| Cobertura de capacidades | COMPLETO 60 | COMPLETO 60 | COMPLETO 72 | **COMPLETO 72** |

**Continuidad semanal de planes nuevos** (`CatalogoV36IT.continuidadDePlanesNuevos`):
- **Qué hace:** genera con V36 los perfiles de 4 objetivos × 3 niveles × 3–6 días × gimnasio/casa y ejecuta el ciclo real. La mitad cumple todas sus sesiones y la otra mitad no registra nada.
- **Base + V36:** **72/72 publican la semana siguiente**, SIN_ORDEN/CONSERVED, sin 1267 ni combinados en PULL y sin llamadas a la IA.
- **PR #3 + V36:** 88/96. Fallan los 8 de LOSE_WEIGHT intermedio/avanzado de 5–6 días, con NO_FACTIBLE: «V15: … enfoque LEGS y solo trabaja [upper legs]».
- **El defecto no viene del catálogo.** Con el PR #3 **sin V36** fallan exactamente los mismos 8.

**Causa:**
- El PR #3 hizo que V15 cuente como disponibles solo los grupos prescribibles, pero solo en la generación.
- La copia semanal valida con `StrictValidationAdapter`, que activa las capacidades solo con `fitsense.adaptation.capability-validation.enabled` o con la progresión (las dos apagadas).
- Por eso la copia sigue exigiendo gemelos a LOSE_WEIGHT, que no los puede prescribir.

**Propuesta, que toca la validación de la adaptación y necesita autorización:** aplicar el filtro de grupos prescribibles de V15 siempre que el objetivo tenga política de capacidades, no solo con capacidades en ENFORCE.
- Solo deja de exigir grupos que el objetivo no puede prescribir.
- No relaja ninguna otra regla.

## 9. Pruebas

| Prueba | Catálogo | Qué demuestra |
|---|---|---|
| `CatalogoV36IT` (7) | V35 → V36 con Flyway | <br>1. Matriz exacta, instrucciones, 1296 sin cambios y 725 sin banco. <br>2. Huellas intactas del resto, de los requisitos, de V27/V28 y de los planes publicados. <br>3. Conteo de aprobaciones de LOWER_HIP (4 → 3) y filtro apagado. <br>4. Elegibilidad por nivel, edad, equipamiento y restricciones. <br>5. Planes nuevos: ningún 1267, ningún combinado en PULL y 1333 nunca en principiantes. <br>6. Continuidad de planes nuevos (§8). <br>7. Evidencia V35→V36 de planes sintéticos (1267 y combinados en PULL no copiables) |
| `GeneracionReferenciaIT` | V36 | Referencia oficial |
| `GeneracionReferenciaV35IT` | V35 | Referencia histórica: el código de generación no cambió |
| `DuracionForzadaIT` | V36 | Comportamiento vigente de la duración forzada |
| `DuracionForzadaPlan60IT`, `Plan60HipertrofiaIT` | V35 | **Históricas:** validan el plan 60 tal como se publicó (872 como estructural a 3x13). Con V36, V24 le exige la dosis de relleno |
| `HerenciaD8IT`, `ProgresionIT` | **V36** | Comportamiento vigente. Su premisa (un viernes acortado bajo el piso de 31 min) dependía de la semana generada; ahora se busca un participante que la cumpla, hasta 10. No cambia ninguna aserción de comportamiento ni el doble de la IA |

**Planes sintéticos de V35.** Por decisión del responsable no hay compatibilidad retroactiva. Los planes 60 y 61 y los casos de la prueba 7 quedan como evidencia del cambio. `consultas/exposicion_v36.sql` queda como diagnóstico opcional.

## 10. Regresión y CI

| Ejecución | Unitarias | Integración | Resultado |
|---|---|---|---|
| `catalogo-v36` (PR #4), local | 816/816 | 255, 0 fallos, 12 omitidas (opcionales) | **BUILD SUCCESS** |
| Combinación PR #3 + V36 (local, sin publicar) | 829/829 | 270, 1 fallo, 10 omitidas | El único fallo es la continuidad de los 8 LOSE_WEIGHT de 5–6 días: el defecto del PR #3 (§8) |
| CI del PR #4 (`verify`, commit 7641812) | — | — | **En verde** |

## 11. Pendientes y efecto real en el MVP

| Pendiente | Causa | Efecto en el MVP |
|---|---|---|
| Revisión humana doble e independiente | Hacen falta dos revisores y no hay formularios entregados | Ninguno técnico. Afecta a la trazabilidad académica. El expediente está listo |
| Decisión sobre 1296 | El GIF contradice la condición del dictamen | 1296 sigue de nivel 1: un principiante puede recibir una flexión de palanca larga. Recomiendo nivel 3 sin impacto |
| Continuidad de LOSE_WEIGHT 5–6 días | Defecto del PR #3 (§8), no de V36 | Esos participantes se quedarían sin semana 2 si se integra el PR #3 tal cual. Necesita autorización para tocar la validación de la adaptación |
| Fusión del PR #3 en `generation` | Pendiente de tu aprobación (orden, punto 2) | Hasta entonces, el PR #4 se valida sobre la base y la combinación solo en local |
| Convención de banco para «sentados» | Coherencia del catálogo | Ninguno inmediato |
