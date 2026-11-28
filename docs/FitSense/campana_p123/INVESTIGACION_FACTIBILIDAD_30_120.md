# Investigación: factibilidad de entrenamientos de 30–120 minutos

## Resumen

**Pregunta.** P-01 admite de 30 a 120 minutos para los cuatro objetivos. ¿Fallan las sesiones largas porque el motor de reglas no encuentra planes que sí existen, o porque las reglas aprobadas las hacen imposibles?

**Respuesta: las dos cosas, y se han separado con demostraciones.**

1. **El motor sí tenía defectos (CASO A).**
   - En 126 combinaciones existía una semana que pasa V1–V25 y el motor no la construía.
   - Se corrigieron cuatro defectos del generador. Ahora construye **118** de esas semanas.
   - Quedan **8** casos de GENERAL_FITNESS con 4–6 días. Están documentados con su testigo y no se han corregido; ver «Pendiente».
2. **La mayoría de los fallos son imposibles con las reglas aprobadas (CASO B, 565 combinaciones).**
   - Se demuestra por cálculo: ni siquiera la sesión más larga posible llega al piso de V20 (70 % de los minutos).
   - En 527 casos basta la cota simple: tope de colocaciones de V24 × la dosis aprobada más larga.
   - En 38 casos hace falta además lo que la semana obliga a colocar: V23 y V25.
3. **13 combinaciones son factibles con gimnasio y dejan de serlo por el equipo o las restricciones del usuario (CASO C).**
4. **11 combinaciones quedan sin demostrar (NO_DEMOSTRADO).** Ver la lista.

No se ha tocado P-01, V4, V20, 4D, V8C, D8, la progresión, la reincorporación, el perfil congelado ni M. No se ha aplicado nada en Neon ni en Railway. Llamadas reales a Replicate: **0**.

Detalle por combinación: `MATRIZ_FACTIBILIDAD_30_120.csv`. Correcciones del generador: `CORRECCIONES_GENERADOR_REGLAS.md`.

## 1. Método

### 1.1 Entorno

- PostgreSQL 16 efímero (Testcontainers), con las migraciones V1–V43 y el catálogo real.
- `fitsense.ai.enabled=false`. No hay ningún doble de la IA: se ejecuta el motor de reglas tal cual y se valida con el validador de producción (V1–V25).
- La prueba es `FactibilidadDuracionIT` (opt-in con `-Dfitsense.factibilidad=true`).
- Es reproducible:
  - la semana es fija (2026-11-02);
  - los `user_id` son secuenciales (F0001 → 1, …);
  - la semilla del motor es `user_id × 1.000.003 + día`.

### 1.2 Combinaciones (1.236)

| Bloque | Qué varía | N |
|---|---|---:|
| Base y expansión | 4 objetivos × 3 niveles × 3 equipos (gimnasio; casa con mancuernas y bandas; casa solo peso corporal) × 1/2/3/4/6 días × 30/60/75/90/105/120 min | 1.080 |
| Frecuencia | 7 días pedidos (tope: 4 principiante, 6 el resto), 60 min, gimnasio | 12 |
| Restricciones | Sin impacto, suelo, rodillas, colgarse ni por encima de la cabeza; gimnasio, 3 días | 72 |
| Bloqueos | Todo el cardio del catálogo bloqueado; gimnasio, 3 días | 72 |

Para cada combinación se registra:
- objetivo, nivel, días pedidos y sesiones efectivas, minutos, equipo, restricciones y bloqueos;
- candidatos elegibles y cuántos tienen dosis;
- sesiones, enfoques, ejercicios por sesión y la prescripción (series × repeticiones o segundos / descanso);
- segundos estimados y minutos declarados por sesión;
- piso de V20, techo de V4, tope de V24 y presupuesto 4D;
- la cota, los validadores que rechazan, el resultado, la clasificación y su causa.

### 1.3 Cota superior independiente

El estimador es aditivo:

`segundos = calentamiento (300) + Σ [trabajo + (series − 1) × descanso] + (n − 1) × transición (60)`

Cada colocación suma su coste más una transición. Por tanto, la sesión más larga posible usa el tope de colocaciones de V24 con los ejercicios elegibles más caros, cada uno con la **dosis aprobada** de su capacidad admisible y en su serie máxima admitida. La cota es:

`cota = 300 + (k − 1) × 60 + Σ de los k costes mayores`, donde k es el tope de V24.

Esta cota ignora V15, V18, V23, V25 y el enfoque, que solo pueden bajarla. Si queda por debajo del piso de V20, **no existe ningún plan válido**.

**Cota con obligaciones semanales.** Si la cota simple llega al piso, se comprueba además si las piezas que la semana obliga a colocar caben en los días. Esas piezas son:
- los slots de 4C (V23; en INCREASE_STRENGTH, con dosis de fuerza por V25);
- cada capacidad principal factible (V25);
- el cardio de cada sesión en LOSE_WEIGHT (V25).

Cada pieza resta (c_max − su mejor coste) a la holgura de su día. Se prueban **todos** los repartos (búsqueda exhaustiva), y se admite que una colocación cubra a la vez un slot y una capacidad. Es una relajación: si ningún reparto cabe, no hay plan válido.

### 1.4 Verificadores de existencia (CASO A)

Si la cota llega al piso y el motor falla, se busca un **testigo**: una semana que pase el validador de producción **completo**.
- **Constructivo**, sin el motor: slots de 4C con fuerza repartidos entre los días, una colocación de cada capacidad principal en el día con menos piezas obligatorias, y el resto con lo admisible más largo.
- **Búsqueda local** desde la semana construida y desde el borrador del motor:
  - movimientos: subir series hasta el máximo admitido, añadir hasta el tope o sustituir por otro ejercicio elegible;
  - solo con dosis aprobadas y sin alargar descansos;
  - la duración declarada es igual a la calculada.

Si aparece un testigo, el caso es CASO A con la semana que lo prueba. Si no aparece, queda NO_DEMOSTRADO.

## 2. Por qué fallaban 90–120 minutos

### 2.1 Cota por objetivo y nivel (gimnasio, sin restricciones)

| Objetivo | Nivel | Tope V24 | Colocación más larga admitida | Cota de sesión | Máximo de minutos con piso al 70 % |
|---|---|---:|---|---:|---:|
| GAIN_MUSCLE | todos | 7 (≥ 60 min) | 3x13/90 (zona pequeña, 297 s) | 2.739 s (45,7 min) | **65** |
| INCREASE_STRENGTH | principiante | 8 | fuerza 3x8/120 (312 s) | 3.216 s | **76** |
| INCREASE_STRENGTH | intermedio/avanzado | 8 | fuerza 3x6/150 (354 s) | 3.552 s | **84** |
| LOSE_WEIGHT | principiante | 8 | cardio 3×90 s / 60 (390 s) | 3.684 s | **87** |
| LOSE_WEIGHT | intermedio/avanzado | 8 | cardio 3×120 s / 60 (480 s) | 4.560 s | **108** |
| GENERAL_FITNESS | principiante | 8 | cardio 3×90 s / 60 | 3.684 s | **87** |
| GENERAL_FITNESS | intermedio/avanzado | 8 | cardio 3×120 s / 60 | 4.560 s | 108 por cota simple; **< 105 por obligaciones** (ver 2.2) |

**Por qué no llega ninguno a 120 min.**
- El piso de 120 min es 5.040 s.
- Con 8 colocaciones, la sesión más larga posible es de 4.560 s (76 min).
- Con 7 colocaciones de 4D, es de 2.739 s (46 min).
- Ningún objetivo ni nivel puede llegar a 120: **CASO B para los cuatro**.

### 2.2 Los casos que la cota simple no ve

- **GENERAL_FITNESS intermedio/avanzado, 105 min (piso 4.410 s).**
  - V25 exige resistencia muscular (ME) en la semana, y la ME más larga es 3x18/45 = 252 s.
  - El día que la lleva dura como mucho 300 + 420 + 252 + 7 × 480 = **4.332 s < 4.410**. El día con movilidad, como mucho 4.170 s.
  - Resultado: imposible.
- **GENERAL_FITNESS principiante, 1 día, 75 min (piso 3.150 s).**
  - La única sesión debe llevar los 4 slots con fuerza (4 × 312 s), ME (225 s), movilidad (90 s) y cardio en las 2 plazas libres (2 × 390 s).
  - Total: **3.063 s < 3.150**.
  - Resultado: imposible.

### 2.3 Lo que no era imposible (CASO A, corregido)

El motor de capacidades armaba cada día por separado.
- Al llenar el tope de 8 colocaciones solo subía series. Nunca cambiaba una colocación corta por otra admisible más larga: una movilidad (75–90 s), una ME 3x18/45 (252 s) o una hipertrofia 3x10/90 (270 s) frente a un cardio 3×120 s (480 s) o una fuerza 3x8/120 (312 s).
- No sabía que otro día de la semana ya llevaba una capacidad, ni redistribuía la fuerza de los slots.
- En GAIN_MUSCLE, el recorte al presupuesto de 4D quitaba la única pantorrilla del día LEGS (V15).

Ejemplos con testigo:

| Caso | Perfil | Motor original | Plan que existe |
|---|---|---|---|
| F0011 | LOSE_WEIGHT intermedio, 105 min | 3.804 s (piso 4.410) | 7 cardios 3×120 s + 1 fuerza 3x6/150: 4.434 s cada día |
| F0039 | INCREASE_STRENGTH principiante, 75 min | 3.048 s (piso 3.150) | 3 hipertrofias 3x10/90 cambiadas por fuerza 3x8/120: 3.159 s |
| F0063 | GENERAL_FITNESS intermedio, 75 min | 2.841 s | ME → cardio; segunda movilidad de la semana → hipertrofia |
| F0064 | GENERAL_FITNESS intermedio, 90 min | 3.438 s (piso 3.780) | Fuerza de slots repartida 2+1+1; movilidad y ME en días distintos |
| F0313 | GAIN_MUSCLE intermedio, 6 días, 30 min | LEGS solo con `upper legs` (V15) | Una pantorrilla 2x13/90 en lugar de un cuádriceps |

## 3. Resultados

### 3.1 Totales

| | Motor original | Motor corregido |
|---|---:|---:|
| Combinaciones evaluadas | 1.236 | 1.236 |
| **Con plan válido** | **521** | **639** |
| Rechazadas | 715 | 597 |
| — por V20 (solo) | 685 | 585 |
| — por V15 y V20 | 24 | 12 |
| — por V15 (solo) | 6 | 0 |
| — por V24 / 4D (rechazo directo) | 0 | 0 |
| — por cualquier otro validador | 0 | 0 |

Los 521 planes válidos del motor original salen **idénticos**, ejercicio por ejercicio, con el motor corregido.

**4D en GAIN_MUSCLE.** No rechaza nunca: el motor respeta su tope de colocaciones, sus dosis y su presupuesto. Pero es la causa de que V20 no se alcance: el tope de 7 colocaciones con 3x10/90 o 3x13/90 hace imposible pasar de 65 min. Por eso los 204 CASO B de GAIN_MUSCLE vienen de 4D.

### 3.2 Clasificación final

| Clase | N | Significado |
|---|---:|---|
| VÁLIDO | 639 | El motor construye la semana y pasa V1–V25 |
| CASO_B | 565 | Imposible con las reglas vigentes: 527 por la cota simple, 38 por las obligaciones semanales |
| CASO_C | 13 | Factible con gimnasio; inviable por restricciones (4) o por el bloqueo del cardio (9) |
| CASO_A | 8 | Existe un plan válido y el motor no lo construye (pendiente) |
| NO_DEMOSTRADO | 11 | Ni testigo ni demostración de imposibilidad |

Por objetivo:

| Objetivo | Válido | CASO_B | CASO_C | CASO_A | NO_DEMOSTRADO |
|---|---:|---:|---:|---:|---:|
| GAIN_MUSCLE | 103 | 204 | 0 | 0 | 2 |
| INCREASE_STRENGTH | 154 | 153 | 0 | 0 | 2 |
| LOSE_WEIGHT | 216 | 85 | 6 | 0 | 2 |
| GENERAL_FITNESS | 166 | 123 | 7 | 8 | 5 |

### 3.3 Base: gimnasio, 3 días (motor original → corregido)

OK = plan válido; B = imposible (CASO B); entre paréntesis, cota / piso en segundos.

| Objetivo | Nivel | 30 | 60 | 75 | 90 | 105 | 120 |
|---|---|:-:|:-:|:-:|:-:|:-:|:-:|
| GAIN_MUSCLE | todos | OK | OK | B (2739/3150) | B | B | B |
| INCREASE_STRENGTH | principiante | OK | OK | **✗ → OK** | B (3216/3780) | B | B |
| INCREASE_STRENGTH | intermedio/avanzado | OK | OK | OK | B (3552/3780) | B | B |
| LOSE_WEIGHT | principiante | OK | OK | OK | B (3684/3780) | B | B |
| LOSE_WEIGHT | intermedio/avanzado | OK | OK | OK | OK | **✗ → OK** | B (4560/5040) |
| GENERAL_FITNESS | principiante | OK | OK | **✗ → OK** | B | B | B |
| GENERAL_FITNESS | intermedio/avanzado | OK | OK | **✗ → OK** | **✗ → OK** | B (obligaciones) | B |

### 3.4 Frecuencia

Con 7 días pedidos, la semana tiene **4 sesiones** en principiantes y **6** en el resto: nunca 7. Las 12 combinaciones tienen plan válido.

### 3.5 Robustez de la semilla

`SEMILLAS_30_120.csv`: 72 perfiles (gimnasio, 3 días), con 8 semillas cada uno (usuario y semana distintos).
- Todos dan **8/8 o 0/8**. Ningún perfil depende de la suerte de la semilla.

### 3.6 CASO A pendientes (8)

| Id | Perfil | Motor | Testigo |
|---|---|---|---|
| F0280, F0286 | GENERAL_FITNESS intermedio/avanzado, 4 días, 90 min, gimnasio | Viernes en 3.633 s (piso 3.780) | En `MATRIZ_FACTIBILIDAD_30_120.csv`, columna `causa` |
| F0640 | ídem, casa con mancuernas y bandas | ídem | ídem |
| F1000, F1006 | ídem, casa solo peso corporal | ídem | ídem |
| F1065 | GENERAL_FITNESS principiante, 6 días, 75 min, casa peso corporal | Lunes en 3.057 s (piso 3.150) | ídem |
| F1072, F1078 | GENERAL_FITNESS intermedio/avanzado, 6 días, 90 min, casa peso corporal | Lunes en 3.399 s | ídem |

**Causa.** Es la misma: el reparto semanal de los slots de fuerza y de las capacidades únicas.
- Con divisiones UPPER/LOWER o PUSH/PULL/LEGS, cada patrón solo cabe en ciertos días, y el repaso semanal de un solo cambio no encuentra la redistribución.
- Corregirlo exige un constructor de semana (asignación global de slots y capacidades), no otro ajuste local.
- No se implementa en este PR. Opciones en `CORRECCIONES_GENERADOR_REGLAS.md` §5.

### 3.7 NO_DEMOSTRADO (11)

| Id | Perfil | Motor (s) / piso | Hipótesis (no verificada) |
|---|---|---|---|
| F0208, F0214, F0568, F0574 | GENERAL_FITNESS intermedio/avanzado, 2 días, 90 min (gimnasio / casa con mancuernas) | 3.675–3.996 / 3.780 | Con 2 días, los 4 slots, la ME y la movilidad llenan las sesiones. Cota simple y obligaciones caben; ningún testigo |
| F0579, F0939 | LOSE_WEIGHT principiante, 4 días, 75 min (casa) | 2.706 en 2 sesiones / 3.150 | Días pegados (V18): el cardio de casa no daría para dos días seguidos |
| F0849 | GENERAL_FITNESS principiante, 1 día, 75 min, casa peso corporal | 3.006 / 3.150 | Mismo cálculo que F0129, pero con el catálogo de casa la cota de obligaciones aún cabe |
| F1034, F1040 | GAIN_MUSCLE intermedio/avanzado, 6 días, 60 min, casa peso corporal | 2.070–2.577 / 2.520 | PUSH/PULL/LEGS con peso corporal: pocas estructurales por enfoque |
| F1053, F1059 | INCREASE_STRENGTH intermedio/avanzado, 6 días, 75 min, casa peso corporal | 2.634 / 3.150 | Estructurales de fuerza de peso corporal escasas por enfoque |

Estos 11 casos no se presentan ni como factibles ni como imposibles.

## 4. Implicaciones para un usuario con perfil congelado (fase F)

### 4.1 Cuándo se congela el perfil

- `UserProfileCommandServiceImpl.handle(UpdateUserProfileCommand)` lanza `UserProfileLockedException` en cuanto existe **una fila** en `weekly_training_plans` (`userHasAnyPlan`).
- No hay otra condición: ni métricas ni sesiones.

### 4.2 Qué pasa si la IA y el respaldo fallan

1. Si la IA está habilitada, el pipeline hace hasta 3 intentos con la IA, validados, y después el motor de reglas, también validado.
2. Si nada pasa, lanza `PlanGenerationFailedException` (HTTP 422 en la generación pedida por API). En el ciclo semanal se registra y la semana queda sin plan (adherencia NULL).
3. **No se escribe ninguna fila de plan.** Por tanto, el perfil **no se congela** con un fallo de la generación inicial.

**Ejemplo pedido: INCREASE_STRENGTH intermedio, 120 min, 3 días, gimnasio.**
- Es CASO B: cota 3.552 s frente a un piso de 5.040 s.
- P-01 lo admite. La primera generación falla con 422 (V20) y no hay plan.
- El perfil **sigue editable**, y la persona puede bajar los minutos (≤ 84 con ese equipo y nivel).
- El riesgo no es quedar congelado, sino tres:
  - **(i)** el mensaje es técnico (V20), sin decir cuál sería un valor viable;
  - **(ii)** con la IA habilitada se gastan hasta **3 llamadas pagadas** antes del respaldo, porque no hay una comprobación previa de la cota en la generación inicial (`FactibilidadPreviaIA` solo actúa en la adaptación; `GoalFeasibilityObserver` solo observa; `SlotCoverageGate` solo mira los slots);
  - **(iii)** la persona puede reintentar y repetir ese gasto.

### 4.3 Cuándo sí queda congelado sin plan

Solo si una semana posterior falla después de haber tenido plan. Hay tres vías:
- **La semilla:** descartada para estos perfiles (§3.5).
- **P-02:** las exclusiones por dolor reducen el conjunto elegible y P-03 nivel 2 queda NO_FACTIBLE. Es seguro y está documentado: se recuerda el dolor y no se publica nada inseguro.
- **Un CASO A pendiente o un NO_DEMOSTRADO** en el que la IA hubiera encontrado plan la primera semana y el respaldo no lo encuentre otra semana.

### 4.4 Diseño propuesto (no implementado)

Una comprobación previa de factibilidad del perfil. No cambia el ciclo de vida del perfil ni ninguna política:

1. **`FactibilidadPerfilPolicy`** (dominio, pura). Recibe el objetivo, el nivel, los minutos, los días y el conjunto elegible ya filtrado por equipo y restricciones. Calcula la cota simple y la de obligaciones (§1.3) y devuelve `FACTIBLE`, `INVIABLE(maxMinutos)` o `SIN_DEMOSTRAR`.
2. **En la generación inicial**, antes de la IA: si es `INVIABLE`, no se llama a la IA (0 llamadas pagadas). Se devuelve 422 con el código `PERFIL_INVIABLE_POR_DURACION` y el máximo viable. El perfil no se congela, como hoy.
3. **En el alta y la edición del perfil:** informar, sin rechazar, del máximo viable. Rechazar sería una política nueva y requiere tu decisión.
4. **Para un perfil ya congelado e inviable:** no se modifica (regla del estudio). Se registra `PERFIL_CONGELADO_INVIABLE` para que el equipo decida caso por caso.

## 5. Políticas intactas

| Política | Estado |
|---|---|
| P-01 30–120 | Intacta (`GoalType`, `DuracionPorObjetivoPolicy`) |
| V4 (techo × 1,15) | Intacta; el testigo y el motor la respetan (`GeneradorReglasCasoAIT`) |
| V20 (70 %) | Intacta; ninguna exención nueva; generación inicial sin D8 |
| 4D (dosis, tope 6/7, presupuesto 85 %) | Intacta |
| Dosis por capacidad, tope de 8 | Intactas; las sustituciones usan solo dosis aprobadas |
| P-02, P-03, C-2, D8 | Intactas; sin cambios en V8C, `PainMemoryService` ni `SafeRegenerationService` |
