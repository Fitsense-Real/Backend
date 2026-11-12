# Generación inicial por objetivo: primero lo principal, después lo secundario (4A v6 + 4D v4)

**Fecha:** 24/10/2026 · **Rama:** `generation`

**Estado:**
- Desarrollado y probado con el catálogo real en PostgreSQL efímero.
- Sin llamadas a Replicate.
- Sin migraciones. V34 sigue sin desplegarse.
- Los 21 ejercicios en revisión no se reclasificaron.
- La adaptación P2-D no se modificó.

> **Cierre de la generación (regla definitiva):** principales disponibles → secundarias disponibles → validación → **publicación automática**. Se eliminaron del flujo los estados `COMPLETO` / `PARCIAL` / `NO_FACTIBLE` / `REQUIERE_ACEPTACION`, la respuesta 409, el bloqueo de semanas parciales, la negativa a generar sin principales y el registro de capacidades en `output_snapshot` y en la API. La sección 5 manda sobre lo que digan las demás.
>
> **Revisión del 24/10, segunda entrega.** Se aplicaron las decisiones de la revisión:
> - `GAIN_MUSCLE` corregido con [4D v3.1](ADENDA_4D_v3_1.md): 36 de 36 perfiles.
> - Solo lo que puede demostrar hipertrofia la acredita.
> - Un `PARCIAL` ya no se activa: se devuelve como propuesta (sección 5).
>
> El barrido de 144 perfiles tiene 0 fallos y P2-D pasa sus regresiones. Las secciones 5, 7 y 8 están actualizadas.

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

## 5. Publicación: automática, sin clasificaciones ni aceptación

1. Se incorporan todas las capacidades **principales factibles**. V25 rechaza cualquier semana que omita una principal factible: una secundaria nunca la desplaza.
2. El tiempo restante, o el de las principales que no son factibles para el perfil, se completa con **secundarias compatibles**.
3. **Si ninguna principal es factible, se genera con las secundarias**, siempre que la semana sea segura y tenga dosis válidas. Ejemplo: `LOSE_WEIGHT` sin ningún cardio posible recibe una semana de fuerza. No contiene cardio y no se presenta como si lo tuviera: los ejercicios y sus dosis son lo que la ficha muestra.
4. **El plan válido se publica automáticamente**, sin pantalla de aceptación ni respuesta especial.
5. **Si no hay ejercicios compatibles o no se puede construir una semana válida**, hay un error de generación sencillo (`EmptyEligibleSetException` o `PlanGenerationFailedException` con los motivos de validación). Nunca se relajan restricciones ni se inventan ejercicios.

**Lo que no se registra:** la cobertura de capacidades no se guarda en `output_snapshot` ni viaja en la API. Solo queda una línea `COBERTURA_SEMANA` en el log para diagnóstico. Lo que lleva cada semana se deduce de sus ejercicios y prescripciones.

## 6. Prompt de la IA (sin llamadas pagadas)

Para los tres objetivos, `GEN-IN-1.11` / `PR-1.4` añaden:
- en el snapshot, `constraints.capacidades`: principales, secundarias, principales factibles y no factibles con su motivo;
- en cada candidato, `dosis_por_capacidad`, la misma tabla que exige V24;
- un bloque de reglas en el texto;
- fuera de la lista, los ejercicios que no aportan nada al objetivo.

`slots_exigibles` llega ahora también a `INCREASE_STRENGTH` y `GENERAL_FITNESS`: V23 ya se los exigía sin decírselo. `GAIN_MUSCLE` recibe exactamente lo de antes. El prompt **no se ha probado contra el modelo**: requiere una corrida pagada.

## 7. Pruebas

**Unitarias: 631/631.** Nuevas:
- `CapacidadesPorObjetivoTest` (18, dos de 4D v3.1):
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

**Integración: `mvn verify -Pintegration`, 68 ejecutadas, 0 fallos.** Las 8 saltadas son las P2-D antiguas que exigen `-Dfitsense.pruebas.url`. Esas tres clases, contra un PostgreSQL local: 7/7, sin residuos.

**`GeneracionPorObjetivoIT` (15):** el flujo real (`generateWeeklyPlan`) con catálogo real y validadores reales en ENFORCE. Además del resultado, cada semana se comprueba así:
- ningún ejercicio de alto impacto, suelo, brazos arriba, carga axial o dificultad incompatible con el perfil;
- ninguno en revisión;
- cada colocación acreditada a una capacidad.

| Caso pedido | Prueba | Resultado |
|---|---|---|
| 1. Todas las principales disponibles | `Completas` (4 objetivos) | COMPLETO. `GENERAL_FITNESS` con las 4 principales y los 4 slots con dosis de fuerza. La API no pide aceptación |
| 2. Algunas sí y otras no | `condicionGeneralSinCardio`, `apiPublica` | **Publicado automáticamente** (201, plan activo): fuerza en todos los patrones, resistencia y movilidad; sin cardio, y no se inventa ninguno |
| 3. Principales que consumen el tiempo | `principalesConsumenElTiempo` (`INCREASE_STRENGTH`, 20 min) | COMPLETO, sin ninguna secundaria |
| 4. Tiempo restante para secundarias | `tiempoRestanteParaSecundarias` (`INCREASE_STRENGTH`, 75 min) | Hipertrofia incorporada; la fuerza no se desplaza |
| 5. Principal ausente y secundarias que completan | `secundariasCompletan` | PARCIAL con hipertrofia. Ninguna principal factible se pierde y los slots llevan fuerza |
| 6. Ninguna principal factible | `perderPesoSinCardio`, `sinEjerciciosCompatibles` | **Semana de fuerza publicada** (la secundaria), sin cardio. Sin ningún ejercicio compatible: error de generación y 0 planes |
| 7. Un único ejercicio compatible | `unUnicoCardio` | El único cardio posible (1325, cinta) en cada sesión no contigua; COMPLETO |
| 8. Impacto, suelo, brazos arriba, equipamiento | `mayorConRestricciones` (66 años), `sinAltoImpacto`, perfiles de casa | Cardio solo de bajo impacto; ningún ejercicio incompatible |
| 9. Validadores que rechazan | unitarias de V1, V24 y V25 | Rechazan lo inseguro, la dosis incorrecta y la principal omitida |
| 10. `GAIN_MUSCLE` | `Hipertrofia` (30, 45 y 60 min), `ganarMusculo`, `AdaptacionRegresionIT` (18/18) | Completo a 30, 45 y 60 min. Tope y presupuesto respetados; cuatro slots; solo evidencia de hipertrofia |

**Barrido** (`Barrido`, cierre): 144 perfiles, 4 objetivos × 3 niveles × casa/gimnasio × 30/45/60 min × con/sin suelo. Cada semana publicada pasa las comprobaciones de seguridad y acreditación. **144 de 144 publicadas, 0 fallos.** Lo que contiene cada una lo calcula la prueba (es descriptivo, no una función del backend):

| Objetivo | Todas sus principales | Algunas principales + secundarias | Solo secundarias | Fallo |
|---|---|---|---|---|
| `GAIN_MUSCLE` | 36 | 0 | 0 | 0 |
| `INCREASE_STRENGTH` | 36 | 0 | 0 | 0 |
| `LOSE_WEIGHT` | 33 | 0 | 3 (principiante en casa sin suelo: fuerza, ningún cardio posible) | 0 |
| `GENERAL_FITNESS` | 33 | 3 (mismo perfil: sin cardio) | 0 | 0 |
