# PR-1.5, primer plan sin fechas pasadas y vencidos sin causa

Este documento parte del commit `9d680f7` y responde a la prueba con GPT-5-mini real del 24/10/2026 (plan 53, `GENERAL_FITNESS`). El plan salió en el **segundo** intento: el primero omitió CARDIO y MOBILITY (V25). Además, las tres sesiones nacieron vencidas.

## 1. Prompt PR-1.5 y entrada GEN-IN-1.12

**Qué cambió en el texto.** Solo afecta a los objetivos por capacidades: `INCREASE_STRENGTH`, `LOSE_WEIGHT` y `GENERAL_FITNESS`.
- **Regla prioritaria al principio,** antes de las reglas generales. Nombra las principales factibles de esa persona e impone un orden de construcción: primero una colocación con dosis de cada principal, luego el reparto y al final las secundarias.
- **Candidatos escasos:** para las principales con 40 candidatos o menos, la regla da sus `exercise_id`. Con GENERAL_FITNESS en gimnasio son 17 de CARDIO y 38 de MOBILITY, entre 842 ejercicios.
- **Comprobación final,** después de los datos (es lo último que lee el modelo): cada principal factible presente, ninguna secundaria en su lugar y cada dosis admitida.
- **Reintento:** `plan_name` y `rationale` no mencionan el rechazo. Esta línea es la única que también cambia para GAIN_MUSCLE.

**Qué cambió en la entrada,** sin quitar ningún ejercicio elegible ni ningún dato:
- Las dosis por capacidad viajan **una vez**, en `constraints.capacidades.dosis`, con un código que empieza por la capacidad (por ejemplo `CARDIO_1`). Cada ejercicio cita sus códigos en `dosis`. Antes cada ejercicio repetía la dosis completa.
- Los campos nulos de cada ejercicio ya no se envían. Un lector antiguo los lee como `null`.

**Tamaño del prompt** (gimnasio, nivel intermedio, 3 días de 45 minutos):

| Objetivo | Antes (PR-1.4) | Ahora (PR-1.5) | Cambio |
|---|---:|---:|---:|
| GENERAL_FITNESS | 465 784 | 256 849 | −45 % |
| INCREASE_STRENGTH | 354 001 | 221 128 | −38 % |
| GAIN_MUSCLE | 266 482 | 238 684 | −10 % |
| LOSE_WEIGHT | 164 265 | 114 167 | −30 % |

**Qué no cambió:** los validadores (V25 sigue rechazando), el modelo, los reintentos y el motor de reglas.

**Pendiente: no está medido con la IA real.** Solo una corrida pagada puede decir si ahora sale al primer intento y cuánto baja el tiempo. La parte más lenta es el razonamiento del modelo, no solo la lectura del prompt.

## 2. El primer plan ya no nace con sesiones pasadas

`POST /users/me/plan/generate` antes generaba siempre la semana actual con la fecha del servidor. Un sábado, con lunes, miércoles y viernes como días de entrenamiento, las tres sesiones nacían vencidas.

**Ahora:**
- Usa la fecha local del participante.
- Genera la semana actual solo si ninguna de las sesiones que elegiría el generador (`WeeklySplitPlanner`) ya pasó; si no, genera la siguiente.
- `GET /users/me/plan/current` devuelve ese plan aunque empiece el lunes que viene.
- El ciclo semanal respeta ese plan: si no hay semana medida y la semana nueva ya tiene plan, lo devuelve sin generar otro.

**Para Flutter:** el primer plan puede empezar el lunes siguiente. La app debe mostrarlo como "empieza el lunes", no como "sin plan".

## 3. Vencidos sin causa no cuentan como registro

La tarea horaria cierra como `SKIPPED`, **sin causa**, todo lo que no se registró, y el cálculo semanal contaba cualquier `SKIPPED` como omisión declarada. Por eso, al llegar el ciclo del lunes, la cobertura salía siempre 100 % y `INSUFFICIENT_DATA` no podía ocurrir en producción.

**Ahora** solo es omisión declarada un `SKIPPED` con causa. La causa es obligatoria al omitir y puede darse después sobre un vencido. Afecta a la cobertura, a la adherencia observada y a las sesiones señaladas.

La adherencia oficial **no cambia**: lo no registrado sigue contando 0.

## Pruebas

- `PromptCapacidadesTest`:
  - la regla prioritaria abre el prompt;
  - nombra las principales y sus candidatos escasos;
  - la comprobación va después de los datos;
  - GAIN_MUSCLE no la recibe;
  - en el reintento, el nombre del plan no menciona la corrección;
  - las dosis viajan por código.
- `PlanInputSnapshotDosisTest`: los nulos ya no viajan y se leen como `null`.
- `PrimeraSemanaTest` (5): sábado, lunes, martes, días por delante y "solo los días que el generador elegiría".
- `OmisionDeclaradaTest` (3): vencidos sin causa frente a omisiones con causa, en el cálculo y en las sesiones señaladas.
- `AdaptacionCompletaIT` (+3 casos):
  - lo no registrado vence antes del ciclo y la decisión es `INSUFFICIENT_DATA`;
  - un primer plan anticipado se respeta;
  - `POST generate` sin sesiones pasadas y `GET current`.

## PR-1.6 (26/10/2026), tras el plan 55 (LOSE_WEIGHT)

El plan se publicó en el tercer intento:
- **Intento 1:** 9 colocaciones por sesión cuando el máximo es 8 (V24).
- **Intento 2:** 4 de 7 ejercicios de pecho en una sesión (V15).
- **Plan publicado:** 1 colocación de CARDIO y 23 de fuerza en toda la semana.

**Defecto corregido.** Para los objetivos por capacidades, el tope de 8 colocaciones por sesión se **validaba pero no se enviaba** a la IA. `constraints.max_colocaciones_dia` ahora viaja también para estos objetivos.

**Prompt.** La regla prioritaria ahora pide:
- **Cada principal factible en CADA sesión,** antes que las secundarias. Con CARDIO como única principal, dos bloques de CARDIO por sesión si hay ejercicios para ello.
- **Que no se quite la principal** si no cabe todo: se quitan secundarias.
- **Que se puedan repetir ejercicios** en días no consecutivos.

Es la misma regla que ya aplica el motor de reglas (`CapabilityWeekBuilder`); no es un criterio nuevo.

La comprobación final añade dos controles: el tope de colocaciones y `max_per_body_part`.

**Pendiente de decisión metodológica.** V25 sigue comprobando solo que cada principal aparezca **en la semana**. Que valide además la principal en cada sesión exige aprobar ese criterio. Hasta entonces es una instrucción a la IA, no una regla del validador.

## V25 por sesión para LOSE_WEIGHT (26/10/2026)

La regla se aprobó con un alcance acotado: solo `LOSE_WEIGHT`. Para los demás objetivos, V25 sigue siendo semanal. No se crean mínimos de minutos ni porcentajes.

**La regla.** Cada entrenamiento debe incluir al menos una colocación **acreditada** como CARDIO, es decir, con la dosis de cardio de 4D, siempre que el cardio sea **factible en esa sesión**.

Es factible si existe un ejercicio elegible con dosis de CARDIO que la sesión admita, con el mismo criterio que usa el motor de reglas:
- **Zonas sin recuperación** (como `cardio`): se admiten siempre.
- **Zonas con recuperación** (pecho, espalda, hombros y piernas): solo si el enfoque de la sesión admite esa zona y no se trabajó el día anterior ni el siguiente.

**Lo que no cambió:**
- **Repetición:** un ejercicio puede repetirse en días no consecutivos, así que basta con un solo ejercicio de cardio elegible.
- **Dos bloques por sesión:** siguen siendo preferencia del prompt y del motor de reglas; no se validan.
- **El resto de las reglas:** el límite de 8 colocaciones, la seguridad y las dosis siguen igual, y no se fuerza ningún ejercicio incompatible.
- **Adaptación (P2-D):** su validador tiene las reglas de capacidad apagadas y no se ve afectado.

**El mensaje a la IA** nombra la fecha de la sesión, pide al menos una colocación de cardio (mejor dos) antes que la fuerza y recuerda que puede repetir.

**Pruebas:**
- `CardioPorSesionTest` (8):
  - el plan 55 se rechaza por el miércoles y el viernes;
  - tres sesiones reutilizando un solo cardio pasan;
  - la dosis decide si una colocación es cardio;
  - sin cardio elegible no se exige nada;
  - un enfoque que no admite la zona del único cardio no lo exige;
  - un día vecino de piernas lo deja fuera por recuperación;
  - GENERAL_FITNESS no cambia;
  - con la regla apagada (P2-D) no se mira.
- `Plan55CardioIT`: el plan 55, con los mismos ejercicios y dosis, pasa por el validador real con el catálogo real.
  - Tal como se publicó, se rechaza por el miércoles y el viernes.
  - Con el gateo de oso repetido los tres días, V25 lo acepta.
- **Regresión de los cuatro objetivos:** pasa, incluido el barrido de 144 perfiles del motor de reglas. Ningún plan del motor de reglas incumple la regla nueva.

## INCREASE_STRENGTH: V25 por slot, PR-1.7 y un defecto del motor de reglas (26/10/2026)

**Origen.** Con el plan 58 se compararon la IA y el motor de reglas para el mismo perfil:

| | Fuerza | Hipertrofia | Bisagra de cadera (LOWER_HIP) con fuerza |
|---|---:|---:|---|
| Plan 58 (IA) | 3 colocaciones | 18 colocaciones | No |
| Motor de reglas (3 semanas) | 12 colocaciones | 6 colocaciones | Sí, los cuatro patrones cada semana |

**V25 (aprobada).** Para `INCREASE_STRENGTH`, cada uno de LOWER_KNEE, LOWER_HIP, UPPER_PUSH y UPPER_PULL necesita al menos una colocación **acreditada como STRENGTH** en la semana, cuando sea factible:
- **Factible** quiere decir que hay un ejercicio elegible de ese slot con dosis de fuerza.
- **Factibilidad conjunta:** se exigen tantos slots distintos como quepan, con 3 colocaciones de fuerza por sesión de menos de 30 minutos y 4 en las demás, como el motor de reglas. Dos slots pueden ir en la misma sesión, así que la recuperación entre días no lo impide.
- **El rechazo** nombra el slot y pide dosis STRENGTH, no HYPERTROPHY.
- **No se valida** cuántas colocaciones de fuerza van por sesión ni ninguna proporción frente a la hipertrofia.

**PR-1.7 y GEN-IN-1.13.**
- **Prompt:** con STRENGTH como principal, cada sesión empieza por los estructurales con dosis de fuerza. Se priorizan los slots que la semana aún no cubre, con una referencia de 4 por sesión (3 si dura menos de 30 minutos). La hipertrofia va solo después.
- **Comprobación final:** pide los cuatro slots con fuerza.
- **Entrada:** el `slot` de cada ejercicio ahora viaja también a INCREASE_STRENGTH y GENERAL_FITNESS. Antes la IA tenía que deducirlo del patrón de movimiento.

**Defecto del motor de reglas, encontrado por la regresión.**
- **Qué pasaba:** el motor daba un slot por cubierto al **colocar** el ejercicio, y el recorte por tiempo podía quitar esa colocación después. El slot quedaba como cubierto sin estarlo y no se volvía a pedir.
- **Dónde se veía:** con INCREASE_STRENGTH en 3 sesiones de 20 minutos, la semana salía sin UPPER_PUSH con fuerza.
- **Por qué estaba oculto:** V23 no se aplica a los planes del motor de reglas (`ENFORCE_AI_ONLY`).
- **Corrección:** los slots pendientes se recalculan **después** del recorte, con lo que queda en la sesión.
- **Resultado:** el barrido de 144 perfiles publica todos los casos sin ningún rechazo por V25.

**Pruebas:**
- `FuerzaPorSlotTest` (6):
  - la bisagra solo con hipertrofia se rechaza por LOWER_HIP y solo por él;
  - cuatro slots con fuerza pasan;
  - un slot no factible no se exige;
  - en una sesión de 25 minutos caben 3: cubrir 3 de 4 pasa y cubrir 2 se rechaza;
  - GENERAL_FITNESS no cambia;
  - con la regla apagada (P2-D) no se mira.
- `PromptCapacidadesTest`: la regla de fuerza solo aparece en INCREASE_STRENGTH y el slot viaja.
- `Plan58FuerzaIT`: el plan 58 real, con el perfil del usuario 25 y el catálogo real.
  - Tal como se publicó, se rechaza solo por LOWER_HIP.
  - Con la bisagra del miércoles a 3×6 y 150 s de descanso, V25 lo acepta.
- **Regresión de los cuatro objetivos:** pasa, con 688 pruebas unitarias y 144 de integración (1 omitida, Replicate).
