# V37.1 · Diagnóstico y propuesta de corrección del catálogo

**Fecha:** 30/10/2026. **Rama:** `catalogo-v37-1`, creada desde `generation` en `1482021` (V37 + V20 exacto).

**Alcance:** diagnóstico y propuesta, **sin implementar**.
- No hay migración nueva en `src/main/resources/db/migration`.
- No se ha modificado ninguna política ni validador.
- No se ha tocado Neon, Railway ni Replicate.

Las simulaciones se hicieron en PostgreSQL aislado (Testcontainers).

**Material:**
- Matriz: `auditoria_catalogo/v37_1/matriz_v37_1.tsv`.
- Evidencia: `evidencia/v37_1/`.
- Hojas de fotogramas: `revision_visual/hoja_fotogramas.py`, revisadas una a una para este informe.

## 1. Resumen

| Prioridad | Qué | Situación | Propuesta |
|---|---|---|---|
| 1 | **142** | V37.1 ya lo corrige (`back` → `upper legs`) | Mantener. Verificado: deja de ir a días de tren superior, sigue cubriendo LOWER_HIP |
| 1 | **1106** | Flexión dinámica registrada como cintura isométrica | `chest`, `pectorals`, `SETS_REPS`. Dificultad: decisión D7 |
| 1 | **1253** | Exige un escalón que no está registrado | Requisito `step platform`, un código que ya existe |
| 1 bis | **1102 y las flexiones 1111, 1113, 1115 y 1262** | Mismo tipo de error que 142, sin corregir. 1102 es el ejercicio más usado | Decisiones D4 y D5 |
| 2 | **Cardio de casa** | Solo hay 3 prescribibles (253, 1054 y 1093). Hay **2 candidatos con evidencia** (853 y 1117) que bloquea una lista de código desactualizada | D1. Prototipo: **resuelve los dos perfiles sin plan sin crear ninguno nuevo** |
| 3 | **Equipamiento** | V37.1 corrige los implementos modelables. **No corrige** los implementos que el esquema no puede expresar: espaldera, toalla con anclaje, banco GHD, banco romano, rack y banco Scott. Algunos se usan en planes de casa | D3: ampliar el modelo, código genérico o desactivar |

Ninguna de estas propuestas toca V15, V18, V20, V23, V24, V25, las dosis ni las políticas de capacidades. Las que cambian criterios están marcadas como **decisión**.

## 2. Primera prioridad: 142, 1106 y 1253

### 142 · *band straight leg deadlift* (fuente 1010)

**GIF:** de pie con la banda bajo los pies, bisagra de cadera con las piernas casi rectas. Resalta isquiotibiales y glúteo. Es un peso muerto rumano con banda.

**Clasificación:**

| Catálogo | Grupo / músculo | Patrón | Equipamiento |
|---|---|---|---|
| V37 | `back` / `spine` | `HIP_HINGE` | `band` |
| V37 + V37.1 | `upper legs` / `hamstrings` | `HIP_HINGE` | `band` |

La corrección ya está en V37.1 y la evidencia la confirma.

**Efecto medido** (barrido de 4 a 6 días y 72 referencias, motor con V20 exacto):

| | Sesiones UPPER_BODY o PULL con 142 | Sesiones LEGS, LOWER_BODY o FULL_BODY con 142 |
|---|---:|---:|
| V37 | **66** (42 UPPER_BODY + 24 PULL) | 73 (FULL_BODY) |
| V37 + V37.1 | **0** | 14 |

- **LOWER_HIP:** 142 sigue siendo elegible para LOWER_HIP en los 192 escenarios con restricciones y **en ninguno es el único candidato**: el mínimo es 5 con V37 y con V37.1.
- **V15, V18, V23:** cero rechazos atribuibles en el barrido, la robustez, la continuidad y los 192 escenarios (§5).
- **Capacidades:** sin cambios en las principales.
- **Nota:** que un plan pase V15 no garantizaba que la distribución fuera correcta. Con V37, 142 ocupaba el «grupo espalda» en días de tirón sin trabajar la espalda.

### 1106 · *push-up to side plank* (fuente 0664)

**GIF:** flexión completa, después giro a plancha lateral con el brazo libre hacia arriba, y vuelta, alternando lados. Es **una combinación dinámica de empuje horizontal y estabilidad lateral**, no una plancha estática.

| Atributo | Actual | Según la evidencia | Confianza |
|---|---|---|---|
| Grupo / músculo | `waist` / `abs` | `chest` / `pectorals`. La estabilidad lateral es un componente secundario | alta |
| Patrón | `HORIZONTAL_PUSH` | `HORIZONTAL_PUSH` (correcto) | alta |
| Prescripción | `DURATION`: el motor le da la dosis isométrica de resistencia, 3×40 s | `SETS_REPS`, por repeticiones | alta |
| Dificultad | 1 | **decisión D7**. Una flexión estándar (1096) es 1. Esta añade apoyo en una mano con rotación. Recomiendo 2 según el criterio v5, con confianza media | media |
| Suelo | sí | sí | alta |

**Uso actual:** 23–25 apariciones en las referencias, como «cintura por duración». Con la corrección deja de poder usarse como abdominal isométrico para completar tiempo.

### 1253 · *standing calf raise (on a staircase)* (fuente 1490)

**GIF:** elevación de talones con el antepié en el borde de un **escalón** de una escalera. El escalón es imprescindible: da el rango por debajo de la horizontal.

**El catálogo sí puede representarlo.**
- El código `step platform` ya existe.
- V37.1 lo usa como requisito en 96 y 97 (elevaciones de talón con banda).
- Hay precedente de requisito «peso corporal + implemento» en 504: `1:body weight, 2:bench`.

**Propuesta:** requisitos `1:body weight, 2:step platform`, sin código nuevo.
- Que el usuario tenga «peso corporal» **no** implica que tenga un escalón.
- Un escalón de escalera y una plataforma de step cumplen la misma función.

**Impacto:** con V37.1, 1253 aparece en 15 sesiones del barrido de perfiles de casa sin step. Dejarían de recibirlo. Hay otras elevaciones de talón sin escalón para sustituirlo.

**Alternativa no recomendada:** crear un código «escalera». Exigiría cambiar el perfil y la interfaz para una única fila.

### 1 bis. Los otros casos del tipo 142, sin corregir en V37.1

Hay **30 ejercicios activos** cuyo grupo contradice su patrón. Todos estaban en la auditoría como `PROBABLE` o `PENDIENTE_CRITERIO` (grupos C4 y C6). Los que más afectan a la generación son estos:

| Ej. | GIF | Hoy | Uso con V37.1 | Por qué importa |
|---|---|---|---|---|
| **1102** *posterior step to overhead reach* | Paso atrás de pie con los brazos estirados por encima de la cabeza. Sin flexión de rodilla significativa | `waist`, `KNEE_DOMINANT`, 2×10 de fuerza | **128 apariciones en las referencias** (61 con V37) | Es el ejercicio más usado del motor y cuenta como fuerza de rodilla (LOWER_KNEE), cuando es un ejercicio de movilidad. **D4** |
| 1111, 1113, 1115, 1262 | Flexiones con patada, de pica a cobra, o de araña | `upper legs`, `HORIZONTAL_PUSH` | 2–13 cada uno | Flexiones que el motor coloca en días de pierna como «upper legs». **D5** |
| 558, 567 | Sentadilla + curl; zancada + extensión de tríceps | `upper arms`, `KNEE_DOMINANT` | 33–42 en el barrido | Domina el tren inferior (precedente de V36). **D6** |

No se modifica ninguno sin tu decisión, porque cambian la cobertura de LOWER_KNEE y UPPER_PUSH.

## 2 bis. Segunda prioridad: cardio para principiantes en casa

**Criterio vigente** (código, no cambia):
- Un ejercicio solo da CARDIO si su patrón es `LOCOMOTION_CARDIO`, su prescripción es `DURATION` y no está en `GoalFeasibilityPolicy.CLASIFICACION_EN_REVISION`.
- Para un principiante en casa, además: nivel 1, sin alto impacto y sin implementos fuera de peso corporal, mancuerna o banda.

**Inventario** (catálogo completo con V37.1; `evidencia/v37_1/cardio_catalogo_v371.txt`):

| Grupo | Ejercicios |
|---|---|
| `LOCOMOTION_CARDIO` activos | 27 |
| Prescribibles hoy para principiante en casa | **3**: 253 gateo de oso, 1054 escalador, 1093 empujar para correr |
| Cumplen todo salvo la lista «en revisión» | **853** y **298** |
| Bloqueados solo por alto impacto | 10: 53, 293, 854, 893, 1133, 1135, 1175, 1176, 1177, 1333 |
| Bloqueados solo por prescripción `SETS_REPS` | 902, 903 y 1117 |
| Bloqueados por equipamiento | 1325 (cinta) y otros dos con máquina o bicicleta |

### Ficha por candidato (GIF revisado)

| Ej. | Ejecución real (GIF) | Patrón | Dificultad e impacto | Dosis cardio vigente posible | Veredicto |
|---|---|---|---|---|---|
| **853** *half knee bends* | Marcha en el sitio con rodillas altas, sin fase de vuelo, de pie y sin suelo | `LOCOMOTION_CARDIO` (lo corrige V37.1; V37 decía `KNEE_DOMINANT`) | 1, sin impacto | Sí, 3×90 s con 60 s de descanso (ya es `DURATION`) | **Candidato. Confianza alta** |
| **1117** *quick feet v. 2* | Apoyos rápidos alternos, de pie y en semiflexión, sin fase de vuelo. La revisión del 23/10 ya lo dio por CORRECTO | `LOCOMOTION_CARDIO` | 1, sin impacto | Solo si pasa a `DURATION`: sus instrucciones dicen «durante el tiempo», y el ejercicio es continuo | **Candidato. Confianza media-alta**. Propuesta: `DURATION` y grupo `cardio`, como 1054 y 1093 |
| 298 *mountain climber cruzado* | Escalador cruzado en plancha. Las instrucciones piden ritmo controlado | dudoso: cardio o core | 1, suelo | Sí | Sigue **en revisión**. No hace falta |
| 854 *high knee against wall* | Rodillas altas con las manos en la pared. La auditoría vio «sin fase de vuelo clara» | `LOCOMOTION_CARDIO` | 1, **marcado de alto impacto** | Sí | **No** sin una segunda revisión humana (C7). No hace falta |
| 1133, 1175, 1177 | Trote en el sitio o pasos de esquí con fase de vuelo | — | alto impacto correcto | — | **No** para principiantes |
| 53, 293, 893, 1135, 1176, 1333 | Saltos, burpees y similares | — | alto impacto correcto | — | **No** |
| 902, 903 *inchworm* | Gusano: movilidad o core | dudoso (C6) | — | — | **No es cardio** |
| 973 *left hook* | Un gancho de boxeo aislado | rotación de tronco | — | — | **No es cardio**: un golpe no es locomoción continua |
| 1295 | Zancadas caminando con rodilla alta | fuerza | — | — | **No es cardio** |

**Ninguno se propone como cardio solo por moverse o por subir la frecuencia cardíaca.** 853 y 1117 son locomoción cíclica y continua sin impacto, igual que 1054 y 1093.

**El bloqueo es de código, no de datos.**
- `CLASIFICACION_EN_REVISION` es de octubre y no se actualizó con dos revisiones posteriores:
  - `REVISION_CATALOGO_CARDIO.md` (23/10) ya decía «1117 sale de revisión»;
  - V37.1 confirma 853 como cardio.
- Sacarlos de la lista es un cambio de criterio de clasificación en el código: **decisión D1**.

### Prototipo (base aislada, sin subir nada)

- **Cambios temporales:** V37.1 + 1117 en `DURATION`/`cardio` + 853 y 1117 fuera de la lista.
- **Revertidos:** el código vuelve a estar como en `generation`.
- **Evidencia:** `evidencia/v37_1/prototipo_cardio/`.

| | V37 + V37.1 | V37 + V37.1 + 853 y 1117 |
|---|---|---|
| 72 referencias | 72 válidas | 72 válidas; **36 cambian**: todos los LW y GF, porque entran dos cardios |
| Barrido de 72 perfiles (4–6 días) | 70/72. Sin plan: LW/BEG/4d casa y LW/BEG/5d casa | **72/72** |
| Robustez de 288 | 280/288 | **288/288** |
| Continuidad de 96 | 94/96 | **96/96** |
| 192 restricciones | 192 publicados, 0 violaciones | 192 publicados, 0 violaciones |
| Sesiones bajo el 70 % exacto | 0 | 0 |
| Uso de cardio de casa (barrido + referencias) | 253: 47, 1054: 52, 1093: 43 | 253: 44, 1054: 43, 1093: 35, **853: 44, 1117: 37** |

**LOSE_WEIGHT principiante, casa, 4 días:**
- Superiores con 2 cardios (1117 y 253).
- Inferiores con 3 cardios (853, 1093 y 1054).
- 2.052 y 2.286 s. V18 se cumple: el martes no repite nada del lunes.

**Conclusión del cardio:**
- Hay **suficientes candidatos con evidencia (2)** para resolver los dos perfiles sin cambiar días, división, dosis ni capacidades.
- Si no apruebas D1, **no hay alternativa de catálogo** que lo resuelva sin cambiar criterios de impacto (854) o la clasificación de un incierto (298). Los dos perfiles seguirían sin plan.

## 3. Tercera prioridad: equipamiento real

**Qué eran los «93».** Son los perfiles de los 192 escenarios que, con V37, reciben algún ejercicio cuyo implemento visto en el GIF no está registrado (`resultados_simulacion.md`). V37.1 (bloque B2) baja esa cifra a 0, **pero solo contaba los implementos que el esquema puede expresar**.

**Clasificación de los casos:**

| Clase | Casos | Ejemplos | Cómo se resuelve |
|---|---|---|---|
| Errores reales del equipamiento registrado | 151 cambios ya en V37.1 (B2) | banco en press inclinado; step en zancadas; `body weight` → `bench` en 314 | V37.1 |
| **Implementos auxiliares no representados** | 34 en `no_incluidas_v37.tsv` (20 de banco Scott y 14 más), y 302, que está en C6. **12 se usan hoy** en planes | espaldera (301–308), toalla con anclaje (1072), banco GHD (880), banco romano (58), rack (235), poste (813), compañero (1080), escalón (1253, que se resuelve con `step platform`) y banco Scott (20, **no usados** en ningún plan medido) | **D3** |
| Restricciones demasiado conservadoras | según V37.1: 1093 sin suelo, 253 sin rodillas (probable) | — | V37.1 o C8 |
| Necesitan ampliar el modelo | los de la fila de implementos no representados | — | **D3** |
| Ejecución física sin confirmar | C1 (74 sentados en banco o cajón), C2 (balón apoyado en el suelo) | — | criterios C1 y C2 pendientes |

**Lo más grave que sigue abierto.** Los **remos con espaldera (301–308)** se publican hoy en planes de casa: 6 a 9 apariciones en referencias por ejercicio y hasta 30 escenarios de restricciones. El usuario no tiene espaldera. Pasan el validador porque su único requisito registrado es `body weight`. **Que el validador no lo detecte no significa que sea seguro ni realizable.**

**Consecuencia de corregirlos** (medida en el catálogo V37.1):
- Perfil **solo con peso corporal**: hoy tiene 3 ejercicios de tirón (nivel 1) y 7 (nivel 2 o más). Sin los de espaldera y toalla, tiene **0**. UPPER_PULL dejaría de ser factible para ellos (V23 lo marcaría «no factible», no lo forzaría).
- Perfil **de casa** (peso corporal, mancuerna y banda): pasa de 11 a 8 ejercicios de tirón con nivel 1, todos con banda. Cuatro de ellos (114, 115, 116 y 143) están además en la convención C1 de los sentados.

**Opciones para D3** (requieren tu aprobación):
1. **Ampliar `equipment_types`** con `wall bars` (espaldera), `door anchor` (anclaje para toalla o banda), `ghd bench`, `hyperextension bench`, `squat rack` y `preacher bench`.
   - Es lo más fiel.
   - Exige que el perfil y la aplicación ofrezcan esos implementos. Hasta entonces, nadie los tendría.
2. Requisito con el código genérico `other`, que ya existe. Equivale a «solo si el usuario declara otro equipo». Es conservador, pero semánticamente pobre.
3. **Desactivar** los no usados (banco Scott) y aplicar 1 o 2 a los usados.

**Recomendación:** 1 para la espaldera y el anclaje, porque son implementos comunes en casa; 3 para el banco Scott. Antes, decidir qué hacer con la cobertura de UPPER_PULL de los perfiles solo con peso corporal.

Matriz completa con ID, evidencia, actual, requerido, corrección, impacto y confianza: `auditoria_catalogo/v37_1/matriz_v37_1.tsv`.

## 4. Propuesta de migración (no creada todavía)

Siguiente número disponible: **V38** (`V38__catalog_v37_1_...sql`). No toca V28, V35, V36 ni V37.

**Contenido en dos bloques:**

| Bloque | Contenido | Necesita |
|---|---|---|
| **A · Datos verificados** | V37.1 tal como está (154 ejercicios, 208 cambios, ya con comprobaciones previas y posteriores) + **1106** (grupo, músculo, prescripción) + **1253** (requisito step) + **1117** (`DURATION`, `cardio`) | Tu visto bueno a esta lista |
| **B · Datos con decisión** | 1102 (D4), flexiones (D5), combinados (D6), dificultad de 1106 (D7), implementos (D3) | Una decisión por grupo |

**Fuera de la migración:** **D1** (lista `CLASIFICACION_EN_REVISION`) es un cambio de código. Iría en un commit separado, con su propia prueba, para distinguir **corrección de datos** de **comportamiento del motor**.

**Formato:** el de V37.
- Tabla de cambios con valor «antes» y «después» por columna.
- Comprobación del estado previo de cada columna (aborta si no coincide).
- Comprobación final e idempotencia.
- Sin tocar `exercise_difficulty_approvals`.

**Pruebas que se añadirían:** `CatalogoV38IT` (estado antes y después, idempotencia), casos expresos de 142, 1106, 1253, 853 y 1117, y que ningún plan de casa contenga un requisito ausente.

## 5. Pruebas ejecutadas en esta fase

| Prueba | V37 | V37 + V37.1 | V37 + V37.1 + prototipo de cardio |
|---|---|---|---|
| 72 referencias | 72 válidas | 72 válidas | 72 válidas (36 cambian) |
| Barrido de 72 (4–6 días): con plan / rechazados / regla | 72 / 0 | 70 / 2 (V20) | 72 / 0 |
| Robustez de 288 | 288 | 280 (8 por V20) | 288 |
| Continuidad de 96 | 96 | 94 | 96 |
| 192 restricciones: publicados / violaciones | 192 / 0 | 192 / 0 | 192 / 0 |
| Sesiones bajo el 70 % exacto | 0 | 0 | 0 |

Las cifras de V37 y V37 + V37.1 son las de la verificación final de V20 (`evidencia/v20/final/`), con el mismo motor.

**Pendiente para la implementación:** la suite completa y las pruebas de V15, V18, V20, V23, V24 y V25, adaptación, progresión y copia, una vez aprobada la lista.

## 6. Impacto sobre la generación y la adaptación

- **Generación:**
  - 142: el tirón vuelve a ser tirón y la bisagra pasa a los días de pierna.
  - Prototipo de cardio: cambia los 36 planes de referencia de LW y GF y abre más rotación de cardio.
  - 1106 y 1253: pocos cambios.
  - D4 (1102) sería **el cambio con más efecto**: es el relleno de LOWER_KNEE más usado.
- **Adaptación, progresión y copia semanal:** las dosis y las políticas no cambian. Los cambios de grupo o patrón pueden variar qué sustituto se elige (V31 y V34). Se medirá con la regresión completa en la fase de implementación.

## 7. Validación posterior con Replicate (preparación, sin llamadas)

- **Respuestas guardadas** (507 llamadas de la campaña):
  - con V20 exacto, 12 propuestas reciben un rechazo nuevo (`evidencia/v20/final/replay_v20_replicate_real.txt`);
  - las del usuario 31 están en `Usuario31Base`.
- **Usuarios 32 y 33: no encuentro sus predicciones** en esta sesión ni en el repositorio. En el registro de la sesión solo aparecen tiempos de los usuarios 1 a 31. Necesito que me pases el texto o el archivo para incorporarlas como en `Usuario31Base`.
- **`min_session_minutes` frente a V20:** documentado en `IMPLEMENTACION_V20.md` §6.3. No se implementa `min_session_seconds`.
- **Reducir ejercicios y tokens** (propuesta, por medir):
  - mandar a la IA solo los elegibles del enfoque de cada día;
  - quitar los que nunca serían prescribibles para el objetivo (los SIN_CAPACIDAD y EN_REVISION de §2 bis);
  - comprimir los campos repetidos.

  Se medirá el número de ejercicios y tokens por llamada con las entradas guardadas, sin llamar a Replicate.
- **Campaña pequeña**, cuando V37.1 esté estable y la autorices: una generación por objetivo (4) más tres adaptaciones (fatiga, falta de tiempo y dolor) por objetivo. Unas 16–20 llamadas.

## 8. Decisiones que necesito

| # | Decisión | Recomendación |
|---|---|---|
| **D1** | Sacar **853 y 1117** de `CLASIFICACION_EN_REVISION` (código) y pasar 1117 a `DURATION`/`cardio` | **Sí**. Evidencia visual y revisión previa; resuelve los dos perfiles sin cambiar reglas |
| D2 | 298 y 854: mantenerlos fuera | Sí, hasta una segunda revisión humana |
| **D3** | Implementos no representados: ampliar el modelo, `other` o desactivar. Y qué hacer con UPPER_PULL en perfiles solo con peso corporal | Ampliar el modelo para espaldera y anclaje; desactivar el banco Scott |
| **D4** | 1102 de `KNEE_DOMINANT` a `MOBILITY` | Sí, con una simulación previa de la cobertura de LOWER_KNEE |
| D5 | 1111, 1113, 1115 y 1262 de `upper legs` a `chest` | Sí |
| D6 | Combinados 558 y 567 a `upper legs` (precedente de V36) | Sí |
| D7 | Dificultad de 1106: 1 o 2 | 2 |
| — | Lista del bloque A de §4 | Visto bueno para preparar V38 |

**Riesgos residuales:**
- La revisión visual sigue siendo asistida por IA. La doble revisión humana está pendiente.
- Los criterios C1 (sentados) y C2 (balón en el suelo) siguen abiertos.
- Las referencias oficiales cambiarán con cualquier opción. No se actualizan hasta que revises las diferencias.
