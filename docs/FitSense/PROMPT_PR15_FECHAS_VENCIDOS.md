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
