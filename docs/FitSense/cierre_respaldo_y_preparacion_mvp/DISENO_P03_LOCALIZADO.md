# Diseño de P-03 localizado (D2): propuesta, sin implementar

**Estado:**
- Solo diseño. **No se ha implementado ni se ha tocado P-03 nivel 2.**
- Requiere tu aprobación funcional y quedaría detrás de un indicador **apagado**.

**La comparación medida** está en `COMPARACION_P03_ACTUAL_VS_LOCALIZADA.md`.

## 1. Dónde encaja

**La cadena propuesta**, solo ante dolor y con la exclusión P-02 ya registrada:

```
IA (3 intentos)
  └─ no usable → RESPALDO
        ├─ nivel 1: SUST-SEG-1 sustituye el ejercicio que dolió      (hoy)
        └─ no hay sustituto → NO_FACTIBLE en V32
              ├─ NIVEL 1 BIS (nuevo): reconstruir SOLO las sesiones con dolor   (propuesta)
              └─ no válido → nivel 2: semana entera con reglas                   (hoy, sin cambios)
                    └─ no válido → NO_FACTIBLE, nada publicado                  (hoy)
```

**Recomendación:** que el nivel 2 siga existiendo como último recurso. La medición muestra que juntos resuelven más casos que cada uno por separado (36 de 40, frente a 35 y 30).

## 2. Algoritmo

**Entrada:**
- la semana medida;
- las sesiones con dolor que el nivel 1 no resolvió (las de los motivos `SEGURIDAD:` de V32);
- el contexto preparado de la semana nueva (el mismo `prepare` que usa hoy el nivel 2).

**Pasos:**
1. **Conservar:** cada sesión **no afectada** se copia de la semana medida (fecha +7, mismos ejercicios, dosis, descansos y minutos), igual que `continuidad`. Se aplican también las correcciones de seguridad que ya existen (V11 y SUST-SEG-1 de otros heredados).
2. **Reconstruir:** para cada sesión afectada, el motor de reglas genera **solo ese día**, con:
   - el **mismo** enfoque, fecha y minutos de la sesión medida;
   - el conjunto elegible del contexto, que ya excluye lo que dolió;
   - como **bloqueados**, los ejercicios de las sesiones conservadas del día anterior y del siguiente (V18) y, si se decide, también los del resto de la semana, para no repetir ejercicios;
   - los **slots 4C** ya cubiertos por las sesiones conservadas: solo se exigen los que faltan;
   - la dosis según la política del objetivo (4D en GAIN_MUSCLE).
3. **Validar la semana mixta:**
   - **V1–V25** con `PlanDraftValidator`;
   - **V8C** en modo continuidad, con una excepción nueva: «sesión reconstruida por seguridad» (§4);
   - **dolor:** ningún ejercicio excluido (comprobación explícita, como en el nivel 2);
   - **trabajo:** la sesión reconstruida no sube ni las repeticiones ni los segundos de la sesión medida, y la semana tampoco (§5);
   - **dosis autorizadas (V24):** las sesiones conservadas mantienen las suyas; la reconstruida no hereda autorizaciones.
4. **Publicar** solo si todo pasa. Si no, se pasa al nivel 2, como hoy.

## 3. Cambios de arquitectura y clases

| Pieza | Cambio | Tipo |
|---|---|---|
| `CapabilityWeekBuilder` / `RuleBasedTrainingPlanGenerator` | Un modo «una sesión con contexto semanal»: recibe las sesiones fijas (ejercicios usados, slots cubiertos, capacidades colocadas) y construye **un** día. Hoy el constructor solo arma la semana entera, con estado semanal (`usadosEnSemana`, `enSemana`, slots pendientes) | **Nuevo** en el generador: es el cambio más delicado |
| `SafeLocalizedRegenerationService` (nuevo) o un método nuevo en `SafeRegenerationService` | Orquesta los pasos 1 a 4, con la misma idempotencia por `weekly_metric_id` | Nuevo |
| `PlanningContextFacade.adaptWeeklyPlan` | Llama al nivel 1 bis antes que al nivel 2, si el indicador está activo | Cambio pequeño |
| `ConservationValidator.validateContinuidad` | Acepta una `SesionReconstruida(sesionOrigen, motivo, regla)` y comprueba que **solo** esa sesión cambia y que mantiene día, enfoque y minutos | Nuevo tipo de excepción, como `SustitucionSeguridad` |
| Evidencia V32 / `safe_regeneration_attempts` | `source = 'RULES_SESSION'` (ampliar el CHECK) y la lista de sesiones reconstruidas | **Migración** nueva |
| `AdaptationOutcome` / API | Resultado nuevo `REGENERACION_SEGURA_SESION` | Contrato nuevo (Flutter) |
| Linaje V31 | Sesiones conservadas: `source_planned_workout_id` como hoy. Sesión reconstruida: el mismo origen, marcado como «reconstruida» (o sin linaje de dosis) | Decisión de diseño |
| Indicador | `fitsense.adaptation.p03-localizado.enabled=false` | Nuevo |

## 4. V8C: la excepción «sesión reconstruida»

**Qué debe exigir:**
- la sesión reconstruida es una sesión **señalada por dolor** con una exclusión P-02 activa;
- misma fecha (+7), mismo enfoque, mismos minutos declarados (o menos, si una causa lo autoriza);
- ningún ejercicio excluido;
- **las demás sesiones**, la comparación de continuidad de siempre: idénticas salvo las excepciones ya autorizadas.

**Qué no debe permitir:**
- reconstruir sesiones sin dolor;
- cambiar sus minutos al alza;
- reintroducir un ejercicio excluido.

## 5. La regla de trabajo

**Qué dice hoy** (P-03 y nivel 2): no se suben ni las repeticiones totales ni los segundos totales, comparados **por separado**.

**El problema:** sustituir un ejercicio de repeticiones por uno de duración siempre sube los segundos. Eso fue lo que hizo fallar las dos alternativas en el caso real de 1373 (C2).

**La decisión** (D3 c) debe tomarse **antes** de implementar:
- **a. Mantener la regla tal cual (recomendado por seguridad):** algunos casos quedan NO_FACTIBLE.
- **b. Una equivalencia acotada** entre repeticiones y segundos solo para rellenos de movilidad, con el mismo divisor que ya usa V8 (`durationToRepsDivisor`). Es un cambio funcional que requiere tu aprobación.

## 6. Pruebas necesarias (cuando se implemente)

| Prueba | Qué comprueba |
|---|---|
| Unitaria del constructor en modo sesión | Respeta los bloqueados (V18), los slots ya cubiertos y el enfoque; es determinista con la semilla |
| Unitaria de V8C | La excepción «sesión reconstruida» acepta solo la sesión con dolor y rechaza cualquier otro cambio |
| IT de los 3 casos: lunes válido, miércoles con dolor, viernes válido | Lunes y viernes idénticos; miércoles reconstruido; nada excluido; V1–V25; trabajo no mayor |
| IT de varias sesiones con dolor | Solo esas se reconstruyen |
| IT con dosis reducidas por fatiga (D8) | Las conservadas mantienen la reducción; la reconstruida no supera el trabajo |
| IT del caso imposible | Pasa al nivel 2 y, si este falla, NO_FACTIBLE sin publicar |
| Idempotencia | Repetir el ciclo devuelve el mismo plan; una sola fila por medición |
| Concurrencia | Dos ciclos simultáneos no publican dos planes |
| Regresión | 960 perfiles idénticos con el indicador apagado, la REPLAY idéntica y las cuatro baterías |
| El prototipo actual | `AnalisisSustitucion1373IT.prototipoP03Localizado` como línea base: 35/40 publicables |

## 7. Qué **no** haría este diseño

- **No elude validaciones:** la semana mixta pasa por las mismas que hoy, y una más (V8C).
- **No regenera libremente:** solo se rehacen las sesiones con dolor; las demás se conservan.
- **No reincorpora ejercicios:** el conjunto elegible ya excluye lo que dolió.
- **No sustituye al nivel 2 ni lo desactiva.**
