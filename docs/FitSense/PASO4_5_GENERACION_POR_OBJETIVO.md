# Generación inicial por objetivo: primero lo principal, después lo secundario (4A v6 + 4D v4)

**Fecha:** 24/10/2026 · **Rama:** `generation`

**Estado:**
- Desarrollado y probado con el catálogo real en PostgreSQL efímero.
- Sin llamadas a Replicate.
- Sin migraciones. V34 sigue sin desplegarse.
- Los 21 ejercicios en revisión no se reclasificaron.
- La adaptación P2-D no se modificó.

Ejemplos: [`generacion/SEMANAS_EJEMPLO.md`](generacion/SEMANAS_EJEMPLO.md) · Barrido de 144 perfiles: [`generacion/BARRIDO_144_PERFILES.md`](generacion/BARRIDO_144_PERFILES.md)

## 1. El defecto que se corrige

Antes, el motor de reglas **quitaba el cardio y la movilidad antes de elegir** (Paso 2: «el motor de reglas no elige complementos»). Prescribía la misma dosis genérica de `TargetPrescription` a todo. Por eso:

- `LOSE_WEIGHT`, cuya única principal es `CARDIO`, recibía semanas **sin una sola colocación de cardio**. Comprobado en el código anterior: el perfil «principiante, casa, sin suelo» se publicaba con 0 colocaciones de cardio o movilidad, como si cumpliera el objetivo.
- `GENERAL_FITNESS` no tenía ni cardio ni movilidad.
- `INCREASE_STRENGTH` usaba 4x6 con 120 s para todo, sin distinguir fuerza de hipertrofia.
- V23 exigía los cuatro slots resistidos también a `LOSE_WEIGHT`.

## 2. Una sola política para generador, validadores y prompt

**`GoalCapabilityPolicy` (4A v6)** sigue siendo la única matriz:

| Objetivo | Principales | Secundarias |
|---|---|---|
| `GAIN_MUSCLE` | `HYPERTROPHY` | `STRENGTH` |
| `INCREASE_STRENGTH` | `STRENGTH` | `HYPERTROPHY`, `POWER` condicional |
| `LOSE_WEIGHT` | `CARDIO` | `STRENGTH` |
| `GENERAL_FITNESS` | `STRENGTH`, `MUSCULAR_ENDURANCE`, `CARDIO`, `MOBILITY` | `HYPERTROPHY`, `BALANCE` oportunista |

**Clases y capacidades** (`CapabilityPrescriptionPolicy`, 4D v4, nueva). La clase de cada colocación sale del `movement_pattern` del catálogo:

| Clase | Patrones | Puede demostrar |
|---|---|---|
| Estructural | los 4 slots de 4C | fuerza, hipertrofia, resistencia muscular |
| Resistido | otro patrón de fuerza | hipertrofia, resistencia muscular |
| Cardio | `LOCOMOTION_CARDIO` | cardio |
| Movilidad | `MOBILITY` | movilidad |
| Equilibrio | `BALANCE_CONTROL` | equilibrio |

**La capacidad la demuestra la DOSIS exacta, no el ejercicio:**

| Capacidad | Intermedio / avanzado | Principiante |
|---|---|---|
| `STRENGTH` (solo estructural) | 3×5, 150 s | 2×8, 120 s |
| `HYPERTROPHY` | 3×10 / relleno 2×10, 90 s (+3 reps en zona pequeña) | 2×10, 90 s |
| `MUSCULAR_ENDURANCE` | 3×18, 45 s · isométricos 3×40 s, 45 s | 2×15, 45 s |
| `CARDIO` (solo `DURATION`) | 3×120 s, 60 s · nota «RPE 5-6» | 3×90 s, 60 s |
| `MOBILITY` | 2×30 s, 15 s | igual |
| `BALANCE` | 2×30 s, 30 s | igual |

**Reglas comunes:**
- Las repeticiones nunca bajan del mínimo de V16 por zona y nivel. Por eso la fuerza sale a 3×6 con el mínimo general de 6, y a 3×10 en cintura.
- Las series pueden subir hasta 3 **solo para llegar al mínimo de V20**: es la regla que el motor ya aplicaba (ACSM 1-3 series), ahora declarada en la política.
- **`GAIN_MUSCLE` no cambia:** dosis de 4D v3 (`MvpDosePolicy`) y solo `HYPERTROPHY` asignable. 4D v3 declara la fuerza, pero no la asigna mientras no exista el denominador del 25 %.
- **`POWER` no se prescribe:** el catálogo no clasifica ejercicios explosivos y no hay forma de verificar sus condiciones. Su ausencia nunca invalida `INCREASE_STRENGTH`.
- **No acreditan nada:** los ejercicios en revisión de clasificación y los patrones `OTHER` o nulos.

Las dosis de los objetivos distintos de `GAIN_MUSCLE` son **criterio de diseño provisional**. Vienen de la tabla de 4D v2 §2 y de ACSM 2009, y requieren el visto bueno metodológico. **La intensidad del cardio se indica en la nota y no se valida:** no hay columna, igual que con el RIR.

**`CapabilityCoveragePolicy` (nueva)** responde dos preguntas con la misma regla:
- **Factibilidad:** ¿hay al menos un ejercicio verificado y elegible con el que prescribir esa dosis? El conjunto elegible ya aplica nivel, edad, equipamiento, ubicación, restricciones y bloqueos.
- **Cobertura:** ¿la semana tiene al menos una colocación acreditada de cada principal?

## 3. Cómo genera el motor de reglas (`CapabilityWeekBuilder`)

Solo para `INCREASE_STRENGTH`, `LOSE_WEIGHT` y `GENERAL_FITNESS`. `GAIN_MUSCLE` sigue por su camino, sin cambios.

1. **Principales factibles, en cada sesión:**
   - `INCREASE_STRENGTH`: estructurales con dosis de fuerza. Si V15 lo permite, un no estructural se cambia por uno estructural.
   - `LOSE_WEIGHT`: dos bloques de cardio, o uno si solo hay uno utilizable ese día.
   - `GENERAL_FITNESS`: fuerza para los slots pendientes, una colocación de resistencia muscular, un bloque de cardio y uno de movilidad.

   En la última sesión, la fuerza completa los slots de 4C que falten, si caben bajo el techo.
2. **Secundarias, solo con el tiempo que queda hasta el mínimo de V20:**
   - antes, los slots que aún le faltan a la fuerza (principal);
   - después, hipertrofia (`INCREASE_STRENGTH`, `GENERAL_FITNESS`) o fuerza (`LOSE_WEIGHT`);
   - si una principal no es factible, su tiempo lo ocupan las secundarias;
   - el equilibrio entra una vez por semana si cabe.
3. **Techo de la sesión:**
   - se quita primero lo secundario, después lo principal ya representado;
   - nunca la única colocación de una principal;
   - nunca de forma que la sesión se quede con un solo grupo (V15).
4. **Coherencia que no se relaja:**
   - el trabajo resistido lo elige `ExerciseSelector` (V15, V18, V22, topes de grupos pequeños);
   - nada se repite del día anterior;
   - un complemento solo trabaja zonas que la sesión ya entrena o que no necesitan recuperación;
   - solo se usan ejercicios verificados con una dosis admisible.

## 4. Validadores

| Regla | Cambio |
|---|---|
| **V23** | No exige los cuatro slots a `LOSE_WEIGHT` (`GoalFeasibilityPolicy.exigeSlotsResistidos`). El resto de objetivos, igual |
| **V24 extendida** | En `INCREASE_STRENGTH`, `LOSE_WEIGHT` y `GENERAL_FITNESS`, cada colocación lleva la dosis exacta de una capacidad admisible del objetivo. Una colocación que no aporta nada al objetivo se rechaza (un abdominal en `LOSE_WEIGHT`, un ejercicio en revisión). Máximo 8 por día. `GAIN_MUSCLE` mantiene su V24 de 4D v3 |
| **V25 (nueva)** | Una principal factible ausente de la semana se rechaza: una secundaria no la desplaza. Las no factibles no se exigen y las secundarias nunca |

**Interruptor propio:** `fitsense.capability.enforcement`, por defecto `ENFORCE`, para la IA y el motor de reglas por igual. La validación estricta de la adaptación P2-D lo deja en **OFF**: las semanas que conserva se publicaron con la dosis anterior, y exigirles la nueva las dejaría sin plan. Llevar estas reglas a P2-D es una decisión aparte.

## 5. COMPLETO, PARCIAL y NO_FACTIBLE, sin estados persistentes nuevos

| Resultado | Qué pasa |
|---|---|
| `COMPLETO` | Se publica |
| `PARCIAL` | Se publica y queda registrado como parcial, nunca como completo (detalle debajo) |
| `NO_FACTIBLE` | **No se publica.** `NoPrimaryCapabilityFeasibleException` (422) explica qué falta y qué alternativa limitada habría con las secundarias, «que requiere tu aceptación y no se publica automáticamente». Para la tarea semanal es una semana sin plan, igual que cuando falla el motor de reglas |

**Cómo se registra un `PARCIAL`:**
- en `output_snapshot` (`GEN-OUT-1.1`, campo `capability_coverage`): resultado, principales cubiertas, las que faltan con su motivo, secundarias incorporadas y slots sin fuerza;
- en la API del plan (`capabilityCoverage`): `requiresAcceptance = true` y el aviso «Esta semana es una alternativa parcial…».

**Lo que NO se implementó, y es una decisión:** que un `PARCIAL` quede **pendiente hasta que el participante lo acepte**. Hoy se publica activo y se marca. Para que no estuviera activo hasta aceptarlo haría falta un estado nuevo de plan, y con él una migración (`ck_plan_status`, `ux_plan_active`) y cambios en métricas. No se introdujo sin autorización.

## 6. Prompt de la IA (sin llamadas pagadas)

Para los tres objetivos, `GEN-IN-1.11` / `PR-1.4` añaden:
- en el snapshot, `constraints.capacidades`: principales, secundarias, principales factibles y no factibles con su motivo;
- en cada candidato, `dosis_por_capacidad`, la misma tabla que exige V24;
- un bloque de reglas en el texto;
- fuera de la lista, los ejercicios que no aportan nada al objetivo.

`slots_exigibles` llega ahora también a `INCREASE_STRENGTH` y `GENERAL_FITNESS`: V23 ya se los exigía sin decírselo. `GAIN_MUSCLE` recibe exactamente lo de antes. El prompt **no se ha probado contra el modelo**: requiere una corrida pagada.

## 7. Pruebas

**Unitarias: 629/629.** Nuevas:
- `CapacidadesPorObjetivoTest` (16):
  - dosis distintas por capacidad;
  - el caso principiante;
  - `GAIN_MUSCLE` sin cambios;
  - `POWER` no asignable;
  - la acreditación es exacta;
  - COMPLETO, PARCIAL, omisión y en revisión;
  - V23 en `LOSE_WEIGHT`;
  - V24 con dosis incorrecta y sin capacidad;
  - V1 para lo inseguro;
  - secundaria no obligatoria;
  - el interruptor en OFF.
- `PromptCapacidadesTest` (2).
- `CatalogoV15CoherenciaTest` pasa de la caracterización del Paso 2 a la regla nueva para `LOSE_WEIGHT` y `GENERAL_FITNESS`: ahora usan los complementos verificados y nunca los que están en revisión.

**Integración: `mvn verify -Pintegration`, 64 ejecutadas, 0 fallos.** Las 8 saltadas son las P2-D antiguas que exigen `-Dfitsense.pruebas.url`. Esas tres clases, contra un PostgreSQL local: 7/7, sin residuos.

**`GeneracionPorObjetivoIT` (15):** el flujo real (`generateWeeklyPlan`) con catálogo real y validadores reales en ENFORCE. Además del resultado, cada semana se comprueba así:
- ningún ejercicio de alto impacto, suelo, brazos arriba, carga axial o dificultad incompatible con el perfil;
- ninguno en revisión;
- cada colocación acreditada a una capacidad.

| Caso pedido | Prueba | Resultado |
|---|---|---|
| 1. Todas las principales disponibles | `Completas` (4 objetivos) | COMPLETO. `GENERAL_FITNESS` con las 4 principales y los 4 slots con dosis de fuerza. La API no pide aceptación |
| 2. Algunas sí y otras no | `condicionGeneralSinCardio` | PARCIAL: cardio no factible y nombrado; fuerza, resistencia y movilidad presentes; API con `requiresAcceptance` y el aviso |
| 3. Principales que consumen el tiempo | `principalesConsumenElTiempo` (`INCREASE_STRENGTH`, 20 min) | COMPLETO, sin ninguna secundaria |
| 4. Tiempo restante para secundarias | `tiempoRestanteParaSecundarias` (`INCREASE_STRENGTH`, 75 min) | Hipertrofia incorporada; la fuerza no se desplaza |
| 5. Principal ausente y secundarias que completan | `secundariasCompletan` | PARCIAL con hipertrofia. Ninguna principal factible se pierde y los slots llevan fuerza |
| 6. Ninguna principal factible | `perderPesoSinCardio` | `NO_FACTIBLE` explicado, 0 planes en la base |
| 7. Un único ejercicio compatible | `unUnicoCardio` | El único cardio posible (1325, cinta) en cada sesión no contigua; COMPLETO |
| 8. Impacto, suelo, brazos arriba, equipamiento | `mayorConRestricciones` (66 años), `sinAltoImpacto`, perfiles de casa | Cardio solo de bajo impacto; ningún ejercicio incompatible |
| 9. Validadores que rechazan | unitarias de V1, V24 y V25 | Rechazan lo inseguro, la dosis incorrecta y la principal omitida |
| 10. Regresión `GAIN_MUSCLE` | `ganarMusculo`, `ganarMusculoEnCasa`, `AdaptacionRegresionIT` (18/18) | Igual que antes |

**Barrido** (`Barrido`): 144 perfiles, 4 objetivos × 3 niveles × casa/gimnasio × 30/45/60 min × con/sin suelo. Repetido con tres juegos de semillas, 432 generaciones: los únicos fallos fueron siempre los 24 de `GAIN_MUSCLE` por juego (72 en total), ninguno en los demás objetivos.

| Objetivo | COMPLETO | PARCIAL | NO_FACTIBLE explicado | Fallo de validación |
|---|---|---|---|---|
| `INCREASE_STRENGTH` | 36 | 0 | 0 | 0 |
| `LOSE_WEIGHT` | 33 | 0 | 3 (principiante en casa sin suelo: ningún cardio posible) | 0 |
| `GENERAL_FITNESS` | 33 | 3 (mismo perfil: sin cardio) | 0 | 0 |
| `GAIN_MUSCLE` | 12 | 0 | 0 | **24, anteriores a este cambio** (sección 8) |

**Ajuste de pruebas existentes:**
- `CatalogoElegibleIT` cambia la misma caracterización del Paso 2, ahora con todo el catálogo: ninguna principal factible omitida en toda su matriz.
- `AdaptacionRegresionIT` › `cardioEnLaSesionCorregidaSePublica` necesitaba un relleno en la semana medida, que ahora puede no existir. Su premisa admite también una estructural cuyo slot está cubierto en otra parte de la semana. El código de P2-D no se tocó.

## 8. Hallazgos y límites, con lo que requieren

1. **`GAIN_MUSCLE` no genera a 30 ni a 60 minutos, en ningún nivel** (24 de 36 perfiles). **Es anterior a este cambio:** se comprobó con el commit previo, que falla los mismos 24 casos con los mismos mensajes.
   - **A 30 min:** el coste de 4D v2 §5 (30,5 min) supera su presupuesto (25,5).
   - **A 60 min:** el tope de 6 colocaciones de 4D v3 no llega al mínimo de V20 (37 contra 42).

   Es un conflicto entre criterios congelados (4D v3 y V20). No se cambió: **requiere decisión metodológica.** Opciones: subir el tope de colocaciones, permitir la ampliación de series, o ajustar el mínimo de V20 para `GAIN_MUSCLE`.
2. **`PARCIAL` publicado y marcado, no pendiente de aceptación** (sección 5). Requiere decisión: estado nuevo con migración, o flujo en la app.
3. **`GENERAL_FITNESS` a 30 min:** en 3 perfiles un patrón (`UPPER_PUSH`) se queda sin dosis de fuerza porque no cabe bajo el techo. La fuerza sigue cubierta y el hueco consta en `slots_sin_cubrir`.
4. **Principiantes con sesiones muy largas:** por ejemplo, `GENERAL_FITNESS` a 75 min sin cardio. Con 8 colocaciones de 3 series como máximo no se llega al 70 % de V20, así que la generación falla en vez de rellenar con incoherencias. No se midió en el código anterior.
5. **La adaptación P2-D no aplica V24 extendida ni V25** (sección 4).
6. **Datos del catálogo que salen tal cual:**
   - ejercicios pliométricos como «Sentadilla con salto» clasificados `KNEE_DOMINANT` reciben dosis de fuerza;
   - el equilibrio casi nunca es factible: 94 exige tablero, 295 es de alto impacto y 885 está retenido.

   Corresponde a la revisión de clasificación, no a este paso.
