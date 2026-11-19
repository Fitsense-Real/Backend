> **Nota de integración.** Informe de la rama aislada `rediseno-cierre-adapt` (desde `d4556b1`), integrado por cherry-pick: 8544792→f3f7863, b919024→a3e231d, 47fd380→9c956a0. Las cifras de pruebas son las de esa rama; las del código integrado final están en `17_VERIFICACION_FINAL.md`.

# Cierre técnico de PLAN-REV-2.0 · Fases 3, 4 y 7 (bifurcación aislada)

**Rama:** `rediseno-cierre-adapt` (local, desde `d4556b1`).
**Restricciones:** sin push, sin merge, sin Replicate y sin Neon. No se han tocado el short paper, el PR #41, la evidencia R/R′, las cohortes ni los oráculos. Tampoco `EvaluacionK20IT`, `BarridoGeneracionInicialIT` ni el generador de reglas.

**Se mantienen:**
- la política por defecto (`PLAN-HIST-1`);
- V24 de generación inicial;
- los topes de sesiones;
- el ritmo de recuperación y la regla de varios retirados;
- la progresión apagada por defecto, sin restauración automática.

## Commits (para cherry-pick, en este orden)

| SHA | Contenido |
|---|---|
| `8544792` | Fases 3 y 4: equivalentes REV2 de las IT de progresión y linaje; SUST-CAT-1 (ejercicio desactivado, solo REV2) |
| `b919024` | Fase 7: la política de una semana nueva sigue el linaje del usuario; `TransicionesPoliticaIT`, `ReparacionLinajeRev2IT` |
| `47fd380` | Fase 7: expectativa de `PoliticaV4CCicloIT.planesHistoricosIntactos` alineada con el linaje (justificada en el test); contexto propio para `TransicionesPoliticaIT` |

## Ficheros tocados

**Main:**
- `PlanningPolicy`: nuevo componente `reparaCatalogoInactivo`, `true` solo en REV2. Hay 5 constructores, todos en este fichero.
- `SafetyConflictPolicy`: `CATALOGO_INACTIVO`, `CAUSAS_DE_SUSTITUCION`, `esMotivoDeSustitucion` y `esCatalogoInactivo`. `CAUSAS` (las de seguridad) no cambia.
- `ExclusionesDeSeguridad`: acepta también `catalogo_inactivo`.
- `SafetyExclusionResolver`: con la bandera, un heredado inactivo y sin causa de seguridad pasa a tener la causa `catalogo_inactivo`.
- `SafetySubstitutionPolicy`: regla `SUST-CAT-1` (`elegirEquivalenteEstricto`) con un mensaje propio, «CATALOGO: …».
- `ConservationValidator` (V8C): vuelve a comprobar el equivalente estricto cuando el motivo es `catalogo_inactivo`.
- `WeeklyTrainingPlanCommandServiceImpl.prepare`: la política de una semana nueva sale del linaje del usuario (`politicaDelLinaje`) y se registra `POLITICA_LINAJE` en el log.

**Pruebas:**
- Nuevas: `PendientesProgresionRev2IT`, `LinajeIntegridadRev2IT`, `CopiaSinIaRev2IT`, `ProgresionRev2IT`, `ReparacionLinajeRev2IT`, `CatalogoInactivoIT`, `CatalogoInactivoHistIT` y `TransicionesPoliticaIT`.
- Históricas: `PendientesProgresionIT`, `CopiaSinIaIT`, `LinajeIntegridadIT`, `ProgresionIT`, `RedisenoFatigaGmIT` y `ReparacionLinajeIT`. **No cambia ninguna aserción.** Solo pasan de `private` a visibilidad de paquete para que las subclases REV2 reutilicen el material, y en `LinajeIntegridadIT` el punto F se extrae a `puntoF()` sin tocarlo.

---

## FASE 3 · Las 4 pruebas que fallaban con REV2

| Prueba | Diagnóstico | Equivalente REV2 | Estado |
|---|---|---|---|
| `PendientesProgresionIT.sinProgresionNoReincorpora` [GYM] y [CASA] | **Premisa solo histórica.** Supone que LW principiante de 30 min «no puede dar ningún paso» tras acortar; con HIST las sesiones van al límite de V4 por el piso del 70 %. Con REV2 hay margen y la progresión encendida da pasos PROGRESION legítimos (pasos en s4–s8 y s10). | `PendientesProgresionRev2IT`. Exige: solo pasos PROGRESION o REINCORPORACION; ninguna REINCORPORACION sin una PROGRESION previa (1(a)); `volumen_despues ≤ techo` en cada paso; el viernes acortado conserva todos sus ejercicios y solo podría recuperar los retirados. | **COMPROBADO** (2/2) |
| `LinajeIntegridadIT.linaje`, punto F | **Premisa solo histórica.** Espera que **V20 (70 %)** rechace la copia de un acortado sin evidencia V32. REV2 no tiene piso (P-G1 aprobado). | `LinajeIntegridadRev2IT.puntoF`: la copia se publica; conserva **exactamente** el acortado (sin evidencia no se restaura nada); la consulta de integridad sigue detectando `PLAN_DERIVADO_SIN_INTENTO_UNICO` en el plan sin intento. Los puntos A–E y 1–3 se ejecutan sin cambios. | **COMPROBADO** |
| `CopiaSinIaIT.retiradoNoElegibleNoVuelve` | **Premisa histórica y defecto real.** La prueba desactiva el retirado suponiendo que no está en otra sesión (cierto en HIST). En REV2 sí lo está, la copia fallaba V1 y la semana quedaba NO_FACTIBLE (limitación L7). | `CopiaSinIaRev2IT`: nunca se publica el inactivo; la otra colocación se sustituye con **SUST-CAT-1** o la semana queda NO_FACTIBLE con motivo «CATALOGO»; la reserva se descarta. En la ejecución el retirado fue 1117: SUST-CAT-1 aplicada y 12 ciclos publicados. | **CORREGIDO** (fase 4) |

**Batería completa con REV2:**
- `ProgresionRev2IT` 24/24, `PendientesProgresionRev2IT` 6/6, `CopiaSinIaRev2IT` 12/12 y `LinajeIntegridadRev2IT` 1/1.
- Las históricas siguen igual: 24 + 6 + 12 + 1.
- Ejecución conjunta: **86/86**.

### Los 10 puntos multisemana (qué prueba cubre cada uno, sin duplicar)

| # | Punto | Pruebas | Estado |
|---|---|---|---|
| 1 | Adaptación ante fatiga | `RedisenoFatigaGmIT.mantenimientoYRecuperacionGradual` (REV2, progresión apagada); `ProgresionRev2IT` «D-P11» y «fatiga: la reducción de la IA se publica» | COMPROBADO |
| 2 | Mantenimiento de dosis autorizadas | `RedisenoFatigaGmIT`: MANTENIMIENTO APLICADA, solo estructurales 3→2, mismas repeticiones y descanso; `fatigaPersistente`: nunca baja de 2 series; avisos REPETIDA y PERSISTENTE | COMPROBADO (dosis [PRO]) |
| 3 | Herencia correcta de dosis | `ProgresionRev2IT` «copia hereda la dosis»; `TransicionesPoliticaIT.progresionPreviaInterruptorApagado`; `ReparacionLinajeRev2IT` (H6) | COMPROBADO |
| 4 | Recuperación gradual | `RedisenoFatigaGmIT` (pasos RECUPERACION en las semanas 4, 6 y 8, sin pasar de la dosis previa); `TransicionesPoliticaIT.mantenimientoACambioHistorica` (igual tras el cambio a HIST) | COMPROBADO; ritmo PENDIENTE DE VALIDACIÓN (P3, sin tocar) |
| 5 | Progresión con el interruptor encendido | `ProgresionRev2IT` 24/24 (dos semanas al 100 % → un paso; copia del paso; techo) | COMPROBADO |
| 6 | Sin progresión cuando no corresponde | `ProgresionRev2IT` («la IA intenta subir la dosis: V8C la rechaza»; «dosis sin paso registrado: V24 la rechaza»); `RedisenoFatigaGmIT` y `TransicionesPoliticaIT` (apagada: 0 PROGRESION) | COMPROBADO |
| 7 | Reincorporación solo tras un retiro (y una PROGRESION posterior) | `PendientesProgresionRev2IT`; `ProgresionRev2IT` («D-P7 + R2 … reincorpora», «P-02 + reincorporación»); `CopiaSinIaRev2IT` (retirado inactivo, reserva DESCARTADA) | COMPROBADO; varios retirados PENDIENTE (P5, sin tocar) |
| 8 | Control de pasos y techo | `CopiaSinIaRev2IT.techo` (16 semanas, ≤ 1,10 × base); `PendientesProgresionRev2IT` (techo por paso); restricción de un paso por ciclo (`assertSinViolaciones`) | COMPROBADO |
| 9 | Conservación del linaje | `LinajeIntegridadRev2IT`; consulta `linaje_v31_v32.sql` sin violaciones en todas las IT; `ReparacionLinajeRev2IT` | COMPROBADO |
| 10 | Interruptor encendido o apagado | `TransicionesPoliticaIT.progresionPreviaInterruptorApagado` (paso con el interruptor encendido → se apaga → 3 copias heredan la dosis progresada sin pasos nuevos); `RedisenoFatigaGmIT` (apagado) frente a `ProgresionRev2IT` (encendido) | COMPROBADO |

**100 % frente a 80 %:** la progresión ordinaria exige dos semanas al 100 % (`ProgresionRev2IT`). La recuperación y la reincorporación siguen con el 80 % (`RedisenoFatigaGmIT`). `ProgresionCienPorCienIT` no tiene subclase REV2; en la ejecución anterior pasó 5/5 con la propiedad REV2. No se ha cambiado nada de esto.

---

## FASE 4 · Ejercicio desactivado mientras está en un plan publicado

**Diagnóstico (código y IT):**
- `SafetyConflictPolicy` declara que un inactivo **no es causa de seguridad**. `SafetyExclusionResolver` solo admite causas verificadas. Por eso un heredado inactivo acababa en **V1**: la copia sin IA, y también la vía con IA (V8C prohíbe tocar las sesiones no señaladas), quedaban **NO_FACTIBLE** en todas las semanas siguientes.
- No se publicaba nada inactivo, pero el usuario se quedaba sin plan.
- La semana en curso no cambia: el inactivo se puede seguir leyendo y ejecutando, porque no es un motivo de seguridad.

**Procedimiento implementado (solo `PLAN-REV-2.0`, bandera `reparaCatalogoInactivo`):**
1. **Causa comprobable:** `is_active = false` en el catálogo da el motivo `catalogo_inactivo`. **No** se añade a las causas de seguridad, así que no bloquea la lectura ni la ejecución de la semana en curso.
2. **Regla SUST-CAT-1:** solo en la semana **derivada** y solo en **la colocación afectada**. Se aplican todas las restricciones de SUST-SEG-1 (elegible para el perfil, con equipamiento y restricciones; mismo slot 4C y tipo de prescripción; zona del enfoque; ni en la sesión ni en las contiguas; fuera de revisión) y además el **equivalente estricto** (`PermisosIntervencion.equivalenteEstricto`): mismo patrón, misma zona, músculo objetivo compatible, dificultad no mayor e impacto no mayor; si el impacto es desconocido, no se acepta ninguno. La dosis se recalcula como en SUST-SEG-1.
3. **V8C** vuelve a comprobar el equivalente estricto con el ejercicio descrito por el catálogo. La sustitución queda trazada en `conservation_report.sustituciones` con regla `SUST-CAT-1` y motivo `catalogo_inactivo`.
4. **Sin equivalente:** no se publica nada. El intento V32 queda NO_FACTIBLE con un motivo explícito: «CATALOGO: el ejercicio X … se desactivó en el catálogo y no hay un sustituto equivalente estricto … requiere intervención». No queda en silencio y no se publica ningún inactivo.
5. **Con la histórica no cambia nada:** V1 y NO_FACTIBLE, comprobado en `CatalogoInactivoHistIT`.

| Punto | Prueba | Resultado | Estado |
|---|---|---|---|
| Copia sin IA con un equivalente | `CatalogoInactivoIT.conSustituto` | Publicada. Solo esa colocación cambia (1291 → 519, mismo patrón y zona, dificultad e impacto no superiores); el resto de la semana es idéntico; la semana medida conserva el inactivo (la historia no se toca); linaje sin violaciones | CORREGIDO (REV2) |
| Sin sustituto | `CatalogoInactivoIT.sinSustituto` | NO_FACTIBLE con motivo «CATALOGO: …»; nada inactivo publicado | CORREGIDO (REV2) |
| Adaptación con IA (otra sesión señalada) | `CatalogoInactivoIT.conIaOtraSesionSenalada` | La sesión no señalada se repara (1100 → 56, SUST-CAT-1); SIN_EFECTO para la IA | CORREGIDO (REV2) |
| Comportamiento histórico | `CatalogoInactivoHistIT` (3/3) | NO_FACTIBLE por V1, como antes | COMPROBADO (sin cambio) |
| V1 sigue rechazando un inactivo que trae la IA | Por construcción: SUST-CAT-1 solo actúa sobre lo **heredado** de esa sesión | — | COMPROBADO (código) |
| Mostrar al usuario el NO_FACTIBLE «CATALOGO» en la API o la app | No se ha probado aquí; el NO_FACTIBLE estructurado (CD-2) ya existe | — | NO EVALUADO (lo hace el principal, F1/API) |
| Retirar solo la colocación cuando no hay sustituto | Es un cambio deportivo (quita trabajo) | — | PENDIENTE DE VALIDACIÓN (propuesto, no implementado) |
| Llevar SUST-CAT-1 también a HIST | Cambiaría la política congelada | — | PENDIENTE DE DECISIÓN |

---

## FASE 7 · Transiciones entre políticas

**Cómo se determina la política (antes):**
- `prepare()` usaba la del plan de la **misma** semana (nuevas versiones: reparación, regeneración), y si no lo había, **la activa de la configuración**.
- Una semana nueva (copia o adaptación) de un usuario en curso se validaba con la política de la configuración del momento.

**Defecto reproducido** (`TransicionesPoliticaIT`, antes de la corrección):

| Transición | Resultado antes |
|---|---|
| HIST → REV2 (GF intermedio, 5 días de 31 min) | **Sin plan**: «V4: … dura unos 2055 s … y el techo es 1860 s (31 minutos + 0 %)» |
| REV2 → HIST (IS principiante, casa, 4 días de 64 min) | **Sin plan**: «V20: … dura unos 1134 s y el mínimo es 2688 s (70 % de 64 minutos)» |
| Mantenimiento P-A1 (REV2) → HIST | **Sin plan**: NO_FACTIBLE (V20). Es la limitación L9, también por la vía del cambio de configuración |

**Corrección (técnica, no deportiva):**
- Una semana nueva de un usuario **con planes** toma la política de **su linaje**: la del plan más reciente anterior a esa semana, en su última versión.
- La configuración solo decide la de los usuarios **sin planes** (generación inicial).
- Si la política del linaje difiere de la configuración, se registra `POLITICA_LINAJE` en el log.
- **No hay cambios automáticos de política**: pasar a un usuario en curso a otra política exige una migración explícita, que no está implementada (propuesta abajo).
- Con HIST por defecto y en las cohortes K20 no cambia nada: sus usuarios empiezan con la política configurada y su linaje es esa misma.

| # | Transición | Prueba | Resultado después | Estado |
|---|---|---|---|---|
| 1 | HIST → REV2 | `historicaARev2` | 3 semanas publicadas, todas `PLAN-HIST-1`; el plan publicado conserva su política; un usuario **nuevo** recibe `PLAN-REV-2.0` | CORREGIDO |
| 2 | REV2 → HIST | `rev2AHistorica` | 3 semanas publicadas, todas `PLAN-REV-2.0`; el usuario nuevo recibe `PLAN-HIST-1` | CORREGIDO |
| 3 | REV2 con mantenimiento → continuidad | `mantenimientoACambioHistorica` | 6 semanas publicadas tras el cambio a HIST, todas REV2 | CORREGIDO |
| 4 | Mantenimiento → recuperación | El mismo test y `RedisenoFatigaGmIT` | RECUPERACION en las semanas 4, 6 y 8, también tras el cambio | COMPROBADO |
| 5 | Progresión previa → interruptor apagado | `progresionPreviaInterruptorApagado` | La dosis progresada se hereda (HEREDADA, P-P1) en 3 copias; 0 pasos nuevos | COMPROBADO |
| 6 | Adaptación publicada → reparación de seguridad | `ReparacionLinajeRev2IT` (REV2) y `ReparacionLinajeIT` (HIST) | REPARADA; la versión nueva conserva la política (`nextVersionOf`) y la siguiente hereda el paso (H6) | COMPROBADO |
| 7 | Desactivación en el catálogo | Fase 4 | REV2: SUST-CAT-1 o NO_FACTIBLE explicado; HIST: V1 | CORREGIDO (REV2) / sin cambio (HIST) |
| 8 | Cambio de configuración durante un ciclo semanal | Tests 1–3: el cambio ocurre entre la generación y la ejecución o el ciclo | El plan en curso y los siguientes del usuario conservan su política; las versiones de la misma semana la heredan | CORREGIDO |

**Qué validador se usa:**
- El contexto de cada `prepare()` lleva la política resuelta (misma semana > linaje > configuración).
- `PlanDraftValidator`, V8C, el prompt y la factibilidad la leen de `ctx.policy()`: V20, V4, V26, V27 y P-A1/P-A2/P-C2 son de la política.
- Cada plan publicado la registra en `weekly_training_plans.planning_policy` y `planning_policy_trace` (V44).

**L9 (MANTENIMIENTO → piso histórico de V20):**
- Con la corrección del linaje, un cambio de configuración **ya no** lleva a un usuario en mantenimiento a HIST: el caso queda resuelto en la práctica (prueba 3).
- La exención de V20 para `MANTENIMIENTO` en D8 (P4) solo haría falta si se migrara de forma explícita a un usuario en mantenimiento a HIST. Queda **PENDIENTE DE DECISIÓN**, sin implementar.

**Procedimiento de migración controlada (propuesto, no implementado):**
1. Solo por acción explícita (operación de administración), nunca por configuración.
2. Solo al empezar una semana sin sesiones hechas.
3. Genera la semana con la política nueva como **generación inicial** (linaje nuevo, sin heredar dosis autorizadas).
4. Registra el evento.
5. Si la generación no es factible, no migra y el usuario sigue con su política.

**Bloqueo abierto (PENDIENTE DE DECISIÓN):** si la configuración vuelve a `allow-candidates=false` mientras hay usuarios con linaje REV2, esos usuarios **siguen** con REV2, porque `deRegistro` admite leer una candidata ya registrada. Es lo seguro para no dejarlos sin plan; la alternativa sería bloquearlos de forma explícita.

---

## Validación

| Bloque | Resultado |
|---|---|
| IT de fases 3 y 4 (históricas + REV2, ejecución conjunta) | 86/86 |
| `CatalogoInactivoIT` + `CatalogoInactivoHistIT` | 6/6 |
| `TransicionesPoliticaIT` (incluye las 2 heredadas de `RedisenoFatigaGmIT` con REV2) | 6/6 tras la corrección (3 fallos reproducidos antes). `PoliticaV4CCicloIT` 3/3 |
| `ReparacionLinajeRev2IT` | 1/1 |
| Regresión completa `verify -Pintegration` (unitarias + todas las IT, política por defecto) sobre `47fd380` | **Unitarias 1.263, 0 fallos (1 omitida). IT 584, 0 fallos, 0 errores (43 omitidas: los laboratorios opcionales). BUILD SUCCESS** |

**Primera regresión (`b919024`):** 1 fallo y 6 errores.
- **Errores (6), de infraestructura de pruebas:** `TransicionesPoliticaIT` tenía las mismas propiedades que su padre y reutilizaba el contexto en caché, conectado al contenedor ya detenido. Se resuelve con una propiedad propia.
- **Fallo (1), una expectativa de diseño anterior:** `PoliticaV4CCicloIT.planesHistoricosIntactos` esperaba que la semana siguiente de un plan anterior a V44 «naciera» con la política de la configuración. Ese es el cambio automático de política que la orden prohíbe.
  - **Expectativa nueva (cambiada y justificada en el test):** la semana siguiente sigue el linaje (HIST) y un usuario nuevo nace con V4-C. Se conserva que el plan antiguo no se reescribe.
  - **PENDIENTE DE VALIDACIÓN por el usuario:** confirmar que los usuarios con planes anteriores a V44 (o con HIST) se quedan en HIST hasta una migración explícita.
