# Diagnóstico del respaldo multisemana

**Pregunta de partida:** `ValidacionMultisemanaIT` exige que una semana con estado `RESPALDO` sea **idéntica** a la anterior. La aserción pasa con la semana de referencia del 2 de noviembre y falla con las del 26 de octubre y del 9 de noviembre. ¿Está mal la prueba o hay un problema funcional?

**Límites de esta fase:** es solo diagnóstico.
- No se ha modificado `src/main` y no se ha tocado la aserción.
- Las referencias de código son al `src/main` de `generation` **dd61837**. Los seis archivos implicados son idénticos en el PR #17 (`git diff origin/generation 7b6f666` vacío para ellos).

**Laboratorios utilizados** (en el PR de laboratorios Fitsense-Real/Backend#20, en borrador y sin fusionar; rama `lab/reloj-pruebas-opcionales`, commit **bc05ec9**, que es `generation` dd61837 más pruebas; ver `LABORATORIOS_OPCIONALES_FECHA.md`):
- `DiagnosticoRespaldoMultisemanaIT`, nuevo y *opt-in*;
- la prueba original `ValidacionMultisemanaIT`, ejecutada con `-Dfitsense.pruebas.semana-referencia`.

**Evidencia:** `evidencia/diagnostico_respaldo/`:
- `ciclos.csv`: un ciclo por fila, con decisión, estado, sesiones señaladas, nota P-03, sustituciones, excluidos, resultado de la aserción actual y motivos de rechazo de la IA simulada;
- `sesiones.csv`: antes y después de cada sesión, con su comprobación;
- `validacion_multisemana_<semana>.txt`.

Todo con PostgreSQL efímero, IA simulada y **0 llamadas a Replicate**.

**Conclusión, en una línea:** la aserción está escrita para un respaldo que es una copia. Con dolor, el código vigente hace otra cosa, aprobada (P-03): sustituye **solo** el ejercicio que dolió con la regla SUST-SEG-1. La propia prueba exige a la vez que ese ejercicio no vuelva, así que las dos aserciones son incompatibles en ese caso. Es el **Resultado 2**: la política permite una diferencia concreta, y la aserción es más estricta que la política. **No hay defecto funcional.**

---

## 1. El mecanismo real (6.1)

### 1.1 Qué condición activa `RESPALDO`

`RESPALDO` es el estado V32 (`plan_adaptation_attempts.status`) de una adaptación que **tenía orden de adaptar** (`ADAPT_LOCALIZED` o `ADAPT_EXPANDED`) pero **ninguna propuesta de la IA utilizable**, y cuya semana resultante **sí se publica**.

**Camino** (`AdaptedWeekCommandService.corregir`, l. 360-458; `sinPropuestaUsable`, l. 470-475):

| Disparador | Línea |
|---|---|
| IA apagada (`fixGenerator.isEnabled()` falso) o cortacircuitos abierto (`proveedorDisponible`) | l. 372-373 |
| Factibilidad previa: el backend demuestra antes de llamar que ninguna propuesta cabe | l. 377-390 |
| **Los 3 intentos** (`AI_ATTEMPTS = 3`) fallan: respuesta fuera de contrato, o semana propuesta rechazada por V1-V25/V8C | l. 399-457 |

En los tres casos el productor recibe `correcciones = null` y entra en `AdaptedWeekProducer.respaldo` (l. 230-231). Una lista vacía sería otra cosa: «la IA no propone nada», que conserva sin el control de seguridad.

**El estado** lo asigna `AdaptationEvidenceMapper` (l. 154-156): `RESPALDO_IA` publicable → `RESPALDO`; no publicable → `NO_FACTIBLE`.

### 1.2 Qué componente lo decide

| Paso | Clase y método |
|---|---|
| Decide que no hay propuesta usable | `AdaptedWeekCommandService.corregir` / `sinPropuestaUsable` |
| Decide qué semana sale | `AdaptedWeekProducer.respaldo` (l. 276-323), que llama a `continuidad` (l. 338-366) |
| Sustituye lo inseguro | `SafetySubstitutionPolicy.reparar` (regla **SUST-SEG-1**) |
| Comprueba que sea una copia salvo lo autorizado | `ConservationValidator.validateContinuidad` (V8C con orden vacía) |
| Validación estricta final | `revisarYCerrar` (V1-V25) |
| Si no sale nada: nivel 2 por dolor | `SafeRegenerationService.regenerar`, invocado desde `PlanningContextFacade` (l. 232-245) |
| Resultado para la API | `AdaptationOutcome.de` (l. 68): `RESPALDO` → `REGENERACION_SEGURA_SUSTITUCION` si hay nota P-03; si no, `RESPALDO_PROPUESTA_IA_INVALIDA` o `RESPALDO_SIN_IA` |

### 1.3 ¿Copia o regeneración?

Depende de si la orden incluye una sesión con `SEGURIDAD_ANTE_DOLOR` (`respaldo`, l. 281-323):

| Caso | Qué hace | ¿Copia exacta? |
|---|---|---|
| **R1. Sin sesión de dolor** | `continuidad(RESPALDO_IA)`: copia de la semana medida con las fechas nuevas | **Sí**, salvo dos correcciones heredadas: la carga sugerida que V11 ya no admite pasa a `null` (`sinCargasInvalidas`), y un ejercicio que se volvió inseguro antes (memoria P-02) se sustituye por SUST-SEG-1. Las dos quedan en V32 |
| **R2. Con dolor y memoria P-02 que cubre lo que dolió** (`cubiertoPorMemoriaDelDolor`) | **P-03 nivel 1**: `continuidad` con `SafetySubstitutionPolicy.reparar`, que **sustituye el ejercicio que dolió** (mismo día, enfoque y minutos; misma clase de slot y tipo de prescripción; dosis recalculada) | **No**: copia **salvo** el ejercicio excluido. Además, si sube las repeticiones o los segundos totales de esa sesión (`masTrabajoQueLaMedida`), queda NO_FACTIBLE. Deja en V32 la nota `REGENERACION_SEGURA: …` y la sustitución |
| **R3. Con dolor no cubierto por la memoria** | NO_FACTIBLE: conservar volvería a publicar lo que dolió | No se publica |

Es decir, **RESPALDO no es una regeneración con reglas.** El comentario de clase de `AdaptedWeekCommandService` lo dice: «NO HAY RESPALDO POR MOTOR DE REGLAS».

El comentario de `produce` («RESPALDO es una copia exacta», l. 200-201) explica por qué no se acepta un paso de progresión. Es anterior a P-03 y **no describe el caso R2**: es la misma imprecisión que arrastra la aserción.

### 1.4 ¿Interviene la IA?

**No** en la semana que se publica. La IA solo pudo intervenir antes, en los intentos rechazados: se cuentan en `ai_calls`, y sus motivos van al registro y a la retroalimentación. La semana de respaldo sale entera del backend y es determinista.

### 1.5 Qué se reutiliza del plan anterior

De la semana medida (`PreviousWeekSummary` y la derivación V31):
- sesiones, fechas (+7 días), enfoque, nombre y minutos declarados;
- ejercicios en el mismo orden, con tipo, series, repeticiones, segundos, descanso y carga sugerida (salvo la carga que V11 retira);
- el linaje (`source_planned_workout_id`).

El origen se registra como `CONSERVED`.

### 1.6 Qué restricciones se vuelven a comprobar

- **V8C de continuidad:** cada sesión igual en día, ejercicios, dosis, enfoque y minutos, salvo las sustituciones autorizadas; ningún ejercicio que dolió vuelve; ninguna sesión desaparece.
- **SUST-SEG-1:**
  - el sustituto es elegible hoy (equipamiento, dificultad, bloqueos);
  - tiene la misma clase de slot (4C), el mismo tipo (V7) y la misma condición de complemento;
  - su zona la admite el enfoque (V15) y respeta V22;
  - no está en revisión y no está ya en la sesión ni en un día contiguo (V18).
- **«Ante dolor no se añade trabajo»** (P-03): repeticiones y segundos totales de la sesión, por separado.
- **Validación estricta completa V1-V25.** Una semana conservada puede salir NO_FACTIBLE si las reglas se han endurecido.

### 1.7 ¿Interviene la semilla semanal?

**No directamente.** `SafetySubstitutionPolicy.elegir` ordena así: misma zona, mismo patrón, mismo músculo y, al final, **id**. No hay aleatoriedad. `AdaptedWeekProducer` tampoco tiene ninguna.

La semilla (`RuleBasedTrainingPlanGenerator.seedFor = userId × 1.000.003 + weekStartDate.toEpochDay()`) solo actúa en el motor de reglas:
- en la **generación inicial** del escenario;
- en la regeneración de **P-03 nivel 2**.

**Influye de forma indirecta**, porque decide qué plan inicial tiene la persona:
- qué ejercicio queda último en la sesión del miércoles, que es el que el escenario marca con dolor;
- y, con ello, si la IA simulada de la prueba produce una propuesta válida o rechazada (§2.2).

### 1.8 Qué puede cambiar legítimamente en un RESPALDO

| Puede cambiar | Condición |
|---|---|
| Las fechas | +7 días, siempre |
| El ejercicio que dolió, en la sesión señalada por dolor | Solo por SUST-SEG-1, en la misma posición, sin subir el trabajo de la sesión y con la nota y la sustitución en V32 |
| Un ejercicio heredado que se volvió inseguro antes (memoria P-02) | Por SUST-SEG-1, en cualquier sesión, registrado en V32 |
| La carga sugerida | A `null` si V11 ya no la admite, nunca a un valor nuevo |
| **Nada más** | Las sesiones no señaladas, los minutos, los demás ejercicios y sus dosis, el orden y el enfoque son idénticos. No hay progresión, ni reincorporación, ni variedad |

### 1.9 Qué pasa si no hay respaldo válido

1. **NO_FACTIBLE** en V32, con los motivos. No se publica una semana inválida.
2. **Si es por dolor, P-03 nivel 2:** `SafeRegenerationService.regenerar` regenera la semana con el **motor de reglas** (sin IA), en el mismo ciclo y con estas condiciones:
   - el conjunto elegible ya sin lo que dolió;
   - dosis base de 4D;
   - validación V1-V25;
   - la comprobación explícita de que nada excluido reaparece;
   - la regla de no subir repeticiones ni segundos de la semana medida;
   - idempotencia por medición.

   Si tampoco vale, queda NO_FACTIBLE y **no se publica nada**. Es la única regeneración del camino, y solo está aprobada para el dolor (P-03).
3. **Sin dolor:** no hay nivel 2. Queda NO_FACTIBLE y el ciclo siguiente parte de cero, como antes.

---

## 2. Reproducción de la diferencia entre semillas (6.2)

### 2.1 La prueba original con las tres semanas

`ValidacionMultisemanaIT` sin modificar, con la semana de referencia movida solo para el diagnóstico (`-Dfitsense.pruebas.semana-referencia`), código de `generation` dd61837 más el reloj de pruebas:

| Semana de referencia | Resultado (57 pruebas, 2 omitidas *opt-in*) | Fallos: escenario (todos GENERAL_FITNESS) | Diferencia exacta en la sesión 1 | Errores de vencimiento |
|---|---|---|---|---|
| **2026-10-26** (ya pasada) | **2 fallos** | «7 dolor» y «10 varios motivos», ambos `ADAPT_LOCALIZED/RESPALDO {SEGURIDAD_ANTE_DOLOR}` | Solo el **último** ejercicio: `455 (DURATION 2×30 s/15) → 453 (DURATION 2×30 s/15)`. Los otros 4 ejercicios, iguales | **0** |
| **2026-11-09** | **1 fallo** | «7 dolor», `ADAPT_LOCALIZED/RESPALDO {SEGURIDAD_ANTE_DOLOR}` | Solo el último: `1178 (DURATION 2×30 s/15) → 66 (DURATION 2×30 s/15)` | **0** |
| **2026-11-02** (la fijada) | **0 fallos** | — | — | **0** |

**Coincide exactamente con lo observado antes de fijar la semana** (26-oct: dos fallos en esos mismos escenarios; 9-nov: «esperado 1178, obtenido 66»).

**En todos los fallos:**
- la diferencia es **una sola posición** de la sesión señalada por dolor;
- **el ejercicio quitado es el que dolió** (el escenario marca con dolor el último);
- el sustituto tiene **el mismo tipo y la misma dosis**;
- las demás aserciones de la prueba se cumplen, entre ellas «lo que dolió no vuelve», que pasa precisamente **por** la sustitución.

**El reloj de pruebas funciona:** con una semana ya vencida (26-oct) hay 0 errores «ya venció».

### 2.2 Por qué una semilla llega a RESPALDO y otra no

El laboratorio `DiagnosticoRespaldoMultisemanaIT` repite las dos secuencias con dolor («7 dolor» y «10 varios motivos») con los mismos perfiles, la misma ejecución y la misma IA simulada.

**Recorrido:**
- 4 objetivos × 2 secuencias × 3 réplicas (usuarios distintos, es decir, semillas distintas) × 3 semanas;
- **72 recorridos**, **175 ciclos** y **525 comparaciones de sesión**.

**Estado del ciclo con dolor (el primero):**

| Semana | APLICADA | RESPALDO | NO_FACTIBLE | Total |
|---|---|---|---|---|
| 2026-10-26 | 21 | 2 | 1 | 24 |
| 2026-11-02 | 22 | 1 | 1 | 24 |
| 2026-11-09 | 23 | 1 | 0 | 24 |

Los ciclos siguientes (todo hecho, sin orden) son **105 `SIN_ORDEN`, todos idénticos** a la semana anterior.

**Los 4 RESPALDO y los 2 NO_FACTIBLE son de GENERAL_FITNESS.** La causa está en la IA simulada de la prueba, no en el backend:
1. El escenario marca con dolor el **último** ejercicio del miércoles. En GENERAL_FITNESS ese último ejercicio es a veces un relleno de **duración** (2×30 s, 15 s de descanso). Cuál es depende del plan inicial, es decir, de la semilla.
2. La IA simulada sustituye el ejercicio que dolió por el primer elegible del mismo patrón y la misma zona, **copiando el tipo de prescripción del original**. Si el sustituto que encuentra es de series y repeticiones, la propuesta incumple V7: «el ejercicio 1373 se prescribió como DURATION y debe ir como SETS_REPS».
3. V7 la rechaza en los 3 intentos y el ciclo entra en el respaldo.

**Con la semilla del 2 de noviembre**, los usuarios de `ValidacionMultisemanaIT` tienen un plan inicial con el que la IA simulada propone algo válido: `APLICADA`, y la aserción de igualdad no se evalúa. **Con el 26 de octubre y el 9 de noviembre**, alguno llega a `RESPALDO` y la aserción falla. El laboratorio muestra que **también con el 2 de noviembre** hay semillas (otros `user_id`) que llegan a `RESPALDO`: la semana fija solo hace que la prueba sea determinista, no que la aserción sea correcta.

### 2.3 Qué cambia exactamente en los 4 RESPALDO

Sesión señalada (la del dolor, posición 1). Las demás sesiones son idénticas:

| Semana | Usuario | Antes | Después | Posición | Dosis | Minutos |
|---|---|---|---|---|---|---|
| 2026-10-26 | 19 | `991 485 56 1176 455` | `991 485 56 1176 453` | 4 (la última) | 2×30 s/15 → 2×30 s/15 | 34 → 34 |
| 2026-10-26 | 21 | `102 1041 1113 854 1178` | `102 1041 1113 854 66` | 4 | 2×30 s/15 → 2×30 s/15 | 34 → 34 |
| 2026-11-02 | 43 | `1334 1323 784 1176 1178` | `1334 1323 784 1176 66` | 4 | 2×30 s/15 → 2×30 s/15 | 34 → 34 |
| 2026-11-09 | 68 | `102 118 1220 1134 455` | `102 118 1220 1134 453` | 4 | 2×30 s/15 → 2×30 s/15 | 34 → 34 |

**En los 4 casos:**
- `decision = ADAPT_LOCALIZED`, intervención `{SEGURIDAD_ANTE_DOLOR}`, una sola sesión señalada;
- `generation_source = CONSERVED` y 3 llamadas (simuladas) rechazadas por V7;
- la nota `REGENERACION_SEGURA` está en V32, y la sustitución también (`455->453` o `1178->66`, regla SUST-SEG-1);
- el ejercicio que dolió queda en `user_safety_exclusions` (ACTIVA).

**El reemplazo `1178 → 66`** es exactamente el que se vio en el CI («esperado 1178 … obtenido 66» en «7 dolor sesión 1»). Es la sustitución determinista SUST-SEG-1 del ejercicio que dolió: el primero por id que cumple slot, tipo, zona y contigüidad.

**La semana siguiente** (todo hecho, `SIN_ORDEN`) conserva el sustituto:
- **no se reincorpora** el ejercicio que dolió;
- no hay variedad añadida.

### 2.4 Los 2 NO_FACTIBLE (caso E)

| Semana | Usuario | Qué pasó |
|---|---|---|
| 2026-10-26 | 22 | Dolió **1373** (relleno de series y repeticiones, enfoque FULL_BODY). SUST-SEG-1 **no encuentra sustituto** con el mismo slot, tipo y una zona que el enfoque admita, sin repetir en la sesión ni en días contiguos. Conservarlo volvería a prescribir lo que dolió (V8C y V1), así que la semana queda **NO_FACTIBLE**. **P-03 nivel 2** regenera con reglas, pero esa semana **sube los segundos totales** (1.140 → 1.260, aunque baje las repeticiones de 436 a 396). La regla aprobada lo prohíbe, así que **no se publica nada** |
| 2026-11-02 | 44 | El mismo patrón |

**Es el comportamiento implementado y aprobado** («ante dolor no se añade trabajo», con las dos magnitudes por separado):
- no se publica una semana inválida;
- no hay regeneración libre.

La consecuencia (la persona se queda sin semana) es conocida y está registrada en V32. Ver `DECISIONES_PENDIENTES.md` (D-3).

---

## 3. Comprobación contra las políticas (6.3)

Ver `VERIFICACION_POLITICAS_ADAPTACION.md` para el detalle. En resumen, sobre los 525 pares de sesiones del laboratorio:

| Caso | Comprobación | Resultado |
|---|---|---|
| **A**: sin causa autorizada | 105 ciclos sin orden: semana idéntica (ejercicios, orden, dosis, fechas, minutos) | **0 incumplimientos**. El respaldo no deja variaciones que se arrastren: el sustituto se conserva y no aparece ningún cambio nuevo |
| **B**: adaptación localizada | Toda sesión **no señalada** idéntica, en los 66 APLICADA y en los 4 RESPALDO | **0 incumplimientos**. En los RESPALDO solo cambia la sesión del miércoles |
| **C**: exceso de capacidad (progresión) | Fuera de este laboratorio (progresión apagada, como en `ValidacionMultisemanaIT`). Cubierto por `ProgresionIT` (24) y `ProgresionCienPorCienIT` (5) en la regresión del PR #17 | Sin fallos |
| **D**: dolor | Lo que dolió no vuelve en ninguna semana. En RESPALDO: solo cambia la posición del ejercicio excluido, el sustituto tiene el mismo tipo y slot, sin subir series, repeticiones ni segundos, el trabajo de la sesión no sube, y la nota y la sustitución están en V32 | **0 incumplimientos** |
| **E**: sin plan válido | 2 NO_FACTIBLE: no se publica nada inválido, P-03 nivel 2 se intenta y se rechaza por la regla de trabajo, sin regeneración libre | Conforme a lo implementado |

---

## 4. Clasificación (6.4): **Resultado 2**

**La política no exige la igualdad completa en un RESPALDO con dolor. Exige igualdad en todo salvo una diferencia concreta y autorizada:**
- P-03 (aprobada el 31/10/2026) y SUST-SEG-1;
- «Si una sesión falla, se corrige esa sesión»;
- «ante dolor, los ejercicios excluidos no reaparecen».

**La aserción actual es, además, internamente contradictoria en ese caso:**
- La misma prueba afirma «Lo que dolió no vuelve» (`assertFalse(dolieron.contains(e.id()))`).
- Si el respaldo fuera la copia exacta que exige la primera aserción, el ejercicio que dolió volvería y fallaría la segunda.
- **Ninguna implementación correcta puede cumplir las dos a la vez** cuando el ciclo cae en RESPALDO con dolor.

**Sin dolor (caso R1), la igualdad sí es la política:** la continuidad. Ahí la aserción es correcta y debe mantenerse.

### Aserción propuesta (NO aplicada; requiere aprobación)

En `ValidacionMultisemanaIT.recorrer`, mantener la igualdad para `{}`, `{CONSERVAR}`, `SIN_EFECTO` y `RESPALDO` **sin** sesiones de dolor. Para `RESPALDO` con `SEGURIDAD_ANTE_DOLOR`, sustituirla por estas invariantes (las mismas que comprueba `DiagnosticoRespaldoMultisemanaIT`):

1. **Sesiones no señaladas:** idénticas (ejercicios, orden, dosis) y con la misma fecha+7, el mismo enfoque y los mismos minutos.
2. **Sesión señalada por dolor:**
   - misma longitud y mismo orden;
   - solo difieren las posiciones cuyo ejercicio anterior **dolió**;
   - en cada una, el sustituto tiene el mismo tipo de prescripción y la misma clase de slot, y no sube series, repeticiones ni segundos;
   - repeticiones y segundos totales de la sesión, no mayores;
   - fecha, enfoque y minutos iguales.
3. **V32:**
   - la nota `REGENERACION_SEGURA` está presente;
   - cada cambio figura en `conservation_report.sustituciones` con `regla = SUST-SEG-1`;
   - `generation_source = CONSERVED`.
4. **Lo que dolió no vuelve** (ya existe).

**Por qué es más estricta que «pasar las validaciones técnicas»:** exige que la diferencia sea **exactamente** la sustitución autorizada y nada más.

---

## 5. Respuestas (6.5)

1. **¿Qué significa técnicamente RESPALDO?**
   - Es una adaptación ordenada que se resuelve **sin propuesta de la IA**: IA apagada, cortacircuitos, factibilidad previa imposible o 3 propuestas rechazadas.
   - La semana se publica con origen `CONSERVED`.
   - Es una copia de la semana medida, revalidada, con las únicas correcciones de seguridad y de carga autorizadas.

2. **¿En qué situaciones se utiliza?**
   - Cuando hay orden de adaptar y no hay propuesta de la IA usable.
   - Sin orden de adaptar no hay RESPALDO: es `SIN_ORDEN` (continuidad).

3. **¿Copia un plan anterior o genera otro?**
   - **Copia.** Con dolor cubierto por la memoria P-02, copia **salvo** el ejercicio que dolió, que se sustituye con SUST-SEG-1 (P-03 nivel 1).
   - **Nunca regenera con reglas.** La única regeneración con reglas es P-03 nivel 2, que ocurre **después** de un NO_FACTIBLE por dolor y no se llama RESPALDO.

4. **¿Por qué cambia el resultado cuando cambia la semilla?**
   - La semilla cambia el **plan inicial** y, con él, qué ejercicio es el último del miércoles (el que el escenario marca con dolor).
   - Según cuál sea, la **IA simulada de la prueba** propone algo válido (APLICADA, la aserción no se evalúa) o algo que V7 rechaza (RESPALDO, la aserción falla).
   - La sustitución en sí no depende de la semilla: es determinista, por id.

5. **¿Qué ejercicios pueden modificarse legalmente?**
   - Solo el ejercicio que dolió, en la sesión señalada por dolor, y los heredados que la memoria P-02 haya vuelto inseguros, siempre por SUST-SEG-1.
   - Ninguno más.

6. **¿Se respetó la adaptación localizada en los casos observados?**
   - **Sí.** En los 4 RESPALDO solo cambió la sesión señalada, y en ella solo la posición del ejercicio que dolió.
   - 0 cambios en sesiones no señaladas.

7. **¿Se conservaron los bloqueos por dolor?**
   - **Sí.** El ejercicio que dolió queda excluido (`user_safety_exclusions` ACTIVA).
   - No vuelve ni esa semana ni las siguientes, y no hay reincorporación automática.

8. **¿Se conservaron las dosis autorizadas?**
   - **Sí.** El sustituto recibió la misma dosis (2×30 s, 15 s de descanso) y la sesión mantiene sus minutos (34).
   - Las demás dosis son idénticas.

9. **¿La prueba está mal planteada o hay un problema funcional?**
   - **La prueba está mal planteada en un caso concreto:** RESPALDO con dolor, donde contradice su propia aserción de seguridad y la política P-03.
   - **No hay un problema funcional** en el respaldo.
   - Hay además una limitación del doble de IA de la prueba: copia el tipo de prescripción del original. Hace que el RESPALDO dependa de la semilla, pero no es un defecto del backend.

10. **¿Qué corrección concreta recomiendo?**
    - **(a)** Sustituir la aserción de igualdad por las invariantes del §4 **solo** para `RESPALDO` con `SEGURIDAD_ANTE_DOLOR`, manteniendo la igualdad en el resto. Requiere tu aprobación.
    - **(b)** Opcional: actualizar el comentario de `AdaptedWeekProducer.produce` («RESPALDO es una copia exacta») para mencionar P-03. Solo es documentación, pero está en `src/main` y también requiere aprobación.
    - **(c)** Opcional: que la IA simulada use el tipo de prescripción del sustituto. Haría que la prueba ejercite más a menudo el camino APLICADA, pero **ocultaría** el camino RESPALDO. Recomiendo mantener (a) y no hacer (c), o hacer (c) añadiendo un escenario explícito de RESPALDO con dolor.

**Mientras no se apruebe,** la prueba sigue como está y es determinista con la semana fija del 2 de noviembre.
