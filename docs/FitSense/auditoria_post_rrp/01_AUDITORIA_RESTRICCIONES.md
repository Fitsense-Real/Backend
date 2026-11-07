# 01 · Auditoría de restricciones de FitSense

> **Estado:** inventario integrado en la auditoría post R/R′ (05/11/2026). Se elaboró **en solo lectura** sobre la rama `investigacion/k20-post-rrp-auditoria`, partiendo de `2dc4abe`, con referencias archivo:línea en el código.
>
> **Comprobaciones de ejecución:**
> - Las respuestas b) y d) se contrastaron después con el barrido de 3.780 perfiles y los diagnósticos por familia (documento 03).
> - La pregunta c) se contrastó con la sonda de factibilidad de las 5 sesiones (documento 04).
>
> **Diferencia con el barrido:** el resumen §0.5 cita los 29 perfiles sin plan de la matriz histórica de 960. El barrido nuevo amplía la rejilla y encuentra **154 de 3.780**, en 6 familias. Las familias coinciden.
>
> **Pendiente de verificar:** la configuración desplegada (Railway y Neon).
>
> **Clasificación:** las categorías pedidas en la orden se corresponden así:
>
> | Etiqueta en la tabla | Categoría de la orden |
> |---|---|
> | SEGURIDAD_INDISPENSABLE | Seguridad indispensable |
> | FUNCIONAL_NECESARIA | Restricción funcional necesaria |
> | HEURISTICA_PLANIFICACION | Heurística de planificación |
> | POTENCIALMENTE_EXCESIVA | Restricción potencialmente excesiva |
> | CONTRADICTORIA_O_DEFECTUOSA | Regla contradictoria o defectuosa |
> | PENDIENTE_VALIDACION_PROFESIONAL | Pendiente de validación profesional |


Repositorio auditado: `/home/user/wt_aud`, rama `investigacion/k20-post-rrp-auditoria`. Solo lectura: no se ejecutó Maven, ni pruebas, ni servicios externos.
Fecha: 05/11/2026.

Abreviaturas de rutas (todas bajo `src/main/java/main/web/services/fitsense/`):
- `PV` = `planning/domain/services/PlanDraftValidator.java`
- `CV` = `planning/domain/services/ConservationValidator.java` (V8C)
- `MDP` = `planning/domain/model/valueobjects/MvpDosePolicy.java`
- `CPP` = `planning/domain/services/CapabilityPrescriptionPolicy.java`
- `RB` = `planning/infrastructure/generation/rules/RuleBasedTrainingPlanGenerator.java`
- `CWB` = `planning/infrastructure/generation/rules/CapabilityWeekBuilder.java`
- `AWP` = `planning/domain/services/AdaptedWeekProducer.java`
- `PGC` = `planning/domain/model/valueobjects/PlanGenerationContext.java`
- `SDE` = `planning/domain/services/SessionDurationEstimator.java`
- `AOD` = `adaptation/domain/services/AdaptationOrderDecider.java`
- `docs/` = `docs/FitSense/`

Las líneas citadas son del árbol de trabajo auditado. Los valores por defecto de las propiedades son los de `src/main/resources/application.properties` y de las anotaciones `@Value`. **Lo que esté desplegado en Railway o Neon (variables de entorno y configuración activa de `calculation_configs`) NO está verificado.**

---

## 0. Resumen de conflictos y riesgos principales

1. **`session_minutes` no tiene un significado único.** El código lo trata como un **valor nominal con intervalo aceptado de [0,70·M, 1,15·M]**, medido con el estimador (V20 piso, V4 techo). La documentación lo llama a la vez «preferencia» (migración V14), «tiempo que la persona reservó» (prompt PR-1.7), «disponibilidad» (FaltaDeTiempoPolicy) y «límite de admisión» (GoalType). Si es el tiempo disponible, V4 deja pasarse hasta +15 % (+11 min a 75). Si es un objetivo, el diseño de 4D lo impide en GAIN_MUSCLE: el contenido máximo llega como mucho al 71–82 % de M.
2. **El tope de colocaciones de GAIN_MUSCLE (4D v3.1/v3.4/C-8) se deriva del piso de V20.** Es el mínimo de colocaciones estructurales que llega al 70 %, así que el techo y el piso casi coinciden: a 75 min, 9 colocaciones dan 3.210 s y el piso es 3.150 s (60 s de margen). Cualquier relleno (150 s frente a 270 s) rompe V20 salvo con la serie adicional de v3.2/v3.3. El resultado son sesiones rígidas y un volumen de hasta **36 series semanales por grupo** que no limita ninguna regla. Los propios documentos lo declaran «adecuación profesional no demostrada» (`docs/ADENDA_4D_v3_4.md` §4.2).
3. **Contradicción generación ↔ adaptación en GAIN_MUSCLE.** En la adaptación, V24/4D va siempre en ENFORCE (`StrictValidationAdapter`) y `ProgresionDosisPolicy.admite4D` rechaza toda reducción. Ante FATIGA o falta de motivación, la IA no puede bajar series ni repeticiones y tampoco quitar ejercicios (solo ACORTAR lo permite). Lo único que puede cambiar es la carga sugerida, si la hay. Los documentos lo cuentan como «imposibilidad legítima» (`docs/short_paper_ieee/k20_adaptacion_real/13_LIMITACIONES.md` §3). En realidad la produce la combinación de reglas, no el perfil.
4. **V20 rige también en la adaptación.** `PlanAdjustment.isActive()` es siempre falso en el flujo actual, porque la tabla porcentual está fuera de él. Solo quedan exentas las sesiones ACORTAR y su herencia D8. Una reducción por fatiga no puede bajar del 70 % de M en ningún objetivo.
5. **Hay perfiles que P-01 admite y el generador no puede satisfacer.** La matriz de 960 perfiles registra 29 (`…/evidencia/v34/PERFILES_SIN_PLAN_VALIDO.csv`): LOSE_WEIGHT de 1 día y 45 min (V15); principiantes de 75 min en LOSE_WEIGHT y GENERAL_FITNESS (V20); peso corporal con 6–7 días en GAIN_MUSCLE e INCREASE_STRENGTH (V20). Además, J03: LOSE_WEIGHT principiante, peso corporal, 4 días, 70 min.
6. **El sistema no distingue imposibilidad real de fallo del algoritmo en la generación inicial.** Si el motor de reglas falla, se lanza `PlanGenerationFailedException`, sin código y con la semana sin plan. El comentario del pipeline lo interpreta como «el perfil hace imposible cualquier plan válido», pero `docs/DIAGNOSTICO_V20.md` §2 demuestra un fallo de composición con 117 ejercicios elegibles sin usar. La adenda 4D v3.1 promete un prefijo `NO_FACTIBLE:` que el código no emite.
7. **«Variante más sencilla» sin límite de dificultad.** `fitsense.adaptation.difficulty-guard=off` está apagado por defecto, y `LOWER_DIFFICULTY` ya no se emite porque la tabla porcentual está fuera de flujo. Ante TOO_DIFFICULT, la IA puede sustituir por un ejercicio **más difícil** del mismo patrón, siempre por debajo del techo del nivel.
8. **El perfil queda bloqueado tras el primer plan** (`UserProfileLockedException`). Esto contradice el javadoc de `UserProfile.update` («se aplican el lunes siguiente») y el mensaje de `PlanLimitationsService` («puedes añadirlo en tu perfil»). Quien pierde material o cambia de disponibilidad no puede corregirlo. La copia semanal sigue con los datos antiguos.
9. **Asimetrías entre IA y respaldo.** V23 en `ENFORCE_AI_ONLY` rechaza a la IA y no al motor de reglas ni a la copia CONSERVED. El motor de reglas de los objetivos por capacidades no rellena hasta el piso cuando hay orden de volumen, mientras que el de GAIN_MUSCLE sí lo hace (código latente).
10. **Los comentarios de configuración contradicen los valores por defecto.** `application.properties` dice «NO activar ENFORCE» para la dosis y la pone en `ENFORCE`. Dice «Se entrega en OBSERVE» para la cobertura y la pone en `ENFORCE_AI_ONLY`. El javadoc de `StrictValidationAdapter` dice «hoy OBSERVE».
11. **La progresión es muy restrictiva y está apagada.** Exige 100 % real dos semanas seguidas. El techo de 1,10 × base está fijado en código (`ProgresionResolver.TECHO`), mientras la configuración dice 25 % y el diseño pedía un parámetro versionado.
12. **Hay código latente con defectos.** La tabla porcentual (`AdjustmentDecisionTable`) está fuera de flujo. Si se reactivara: REDUCE_DAYS con piso de 2 días puede **subir** días o pedir más días de los disponibles (V2 imposible), `min_session_minutes = 20` está por debajo de P-01, y V4 no usa los minutos forzados.

---

## 1. Tabla resumen

| Código | Fase | Límite principal | Clasificación |
|---|---|---|---|
| P-01 minutos | Perfil / prepare | 30–75 min (BD admite 15–180) | FUNCIONAL_NECESARIA (admite perfiles no generables) |
| P-DÍAS | Perfil | días 1–7; días disponibles ≥ días/semana | FUNCIONAL_NECESARIA |
| P-BLOQ | Perfil | perfil inmutable desde el primer plan | POTENCIALMENTE_EXCESIVA / CONTRADICTORIA |
| E-DIF (V6 en consulta) | Catálogo | BEGINNER 1, INTERMEDIATE 2, ADVANCED 3 | SEGURIDAD_INDISPENSABLE (dato pendiente de revisión) |
| E-EQ | Catálogo | requisitos N:M de equipamiento; HOME sin `requires_gym` | FUNCIONAL_NECESARIA |
| E-SEG SafetyProfile | Catálogo | edad ≥60 / ≥65 / ≥70; principiante sin impacto; 5 permisos declarados | SEGURIDAD_INDISPENSABLE + PENDIENTE_VALIDACION_PROFESIONAL |
| E-RET INC-4C-03 / INC-CAT-1374 | Catálogo | 7 ids retenidos; 1374 condicionado | SEGURIDAD_INDISPENSABLE |
| E-FUN CatalogEligibilityPolicy | Catálogo | sin neck, lower arms, cardio ni «estiramiento», salvo ruta de capacidad | HEURISTICA_PLANIFICACION |
| E-REV clasificación en revisión | Catálogo / V24 / V25 | 21 ids no acreditan capacidad | FUNCIONAL_NECESARIA (temporal) |
| E-DOLOR P-02 | Catálogo | exclusión sin caducidad; solo reactivación explícita | SEGURIDAD_INDISPENSABLE |
| E-APROB | Catálogo | filtro de aprobaciones humanas (apagado) | FUNCIONAL (inactivo) |
| G-4C previa | Pregeneración | 0 = NO_FACTIBLE, 1–2 = FRAGIL, ≥3 = ROBUSTO; modo OBSERVE | HEURISTICA_PLANIFICACION |
| G-4A pool | Pregeneración | mismos umbrales, solo observación | HEURISTICA_PLANIFICACION |
| S-MAX sesiones | Planificación | principiante 4, resto 6 | HEURISTICA_PLANIFICACION |
| S-SPLIT | Planificación | FB / UL / PPL según número de días y días seguidos | HEURISTICA_PLANIFICACION |
| N-TIPO / N-DUR normalizador | Pre-validación IA | tipo del catálogo; duración = estimador | FUNCIONAL_NECESARIA |
| V1 | Validación | ejercicio dentro del conjunto elegible | SEGURIDAD_INDISPENSABLE |
| V2 | Validación | número de sesiones = días efectivos | FUNCIONAL_NECESARIA |
| V3 / V12 | Validación | fechas en días disponibles y dentro de la semana; 1 sesión por día | FUNCIONAL_NECESARIA |
| V4 | Validación | ≤ M × 1,15 (redondeo histórico) | FUNCIONAL_NECESARIA (tolerancia heurística; brecha de redondeo) |
| V5 | Validación | ≥ 2 ejercicios por sesión | HEURISTICA_PLANIFICACION |
| V6 | Validación | dificultad ≤ máximo efectivo | SEGURIDAD_INDISPENSABLE |
| V7 | Validación | tipo = el del catálogo; campos obligatorios | FUNCIONAL_NECESARIA |
| V8 | Validación | volumen en banda (inactiva) | FUNCIONAL (inactiva en el flujo) |
| V9 / V10 | Validación | ≥ 2 series, ≥ 6 reps, ≥ 20 s | HEURISTICA_PLANIFICACION (excesiva al reducir) |
| V11 | Validación | carga +10 % como máximo, solo tras completar; null sin dato | SEGURIDAD_INDISPENSABLE |
| V13 | Validación | mismo enfoque no en días seguidos | HEURISTICA_PLANIFICACION |
| V15 | Validación | ≥ min(2, …) grupos del enfoque; sin grupos ajenos; ninguno > ½ | HEURISTICA_PLANIFICACION / POTENCIALMENTE_EXCESIVA |
| V16 | Validación | reps 6–30; lower legs ≥12, waist ≥10; BEGINNER ≥8 | PENDIENTE_VALIDACION_PROFESIONAL |
| V17 | Validación | declarado vs. estimado ±20 % | FUNCIONAL (neutralizada por el normalizador) |
| V18 | Validación | ni mismo ejercicio ni chest/back/shoulders/upper legs en días seguidos | HEURISTICA_PLANIFICACION |
| V19 | Validación | topes con REDUCE_VOLUME (inactiva) | FUNCIONAL (inactiva) |
| V20 | Validación (generación y adaptación) | contenido ≥ ⌈M·60·70/100⌉ s | POTENCIALMENTE_EXCESIVA (contradictoria en adaptación) |
| V21 | Validación | descanso ≤ 180 s | HEURISTICA_PLANIFICACION |
| V22 | Validación | PUSH sin bíceps, PULL sin tríceps | HEURISTICA_PLANIFICACION |
| V23 | Validación | 4 slots 4C en la semana (solo IA) | HEURISTICA_PLANIFICACION (asimétrica) |
| V24-GM dosis | Validación | dosis 4D exacta (3×10/90 s; relleno 2×; +3 reps en zona pequeña) | PENDIENTE_VALIDACION_PROFESIONAL / POTENCIALMENTE_EXCESIVA |
| V24-GM tope | Validación | 6 / 7 / 8 / 9 colocaciones según minutos | CONTRADICTORIA_O_DEFECTUOSA (derivada de V20) |
| V24-GM presupuesto | Validación | coste 4D ≤ 0,85·M·60 | HEURISTICA_PLANIFICACION |
| V24-GM evidencia | Validación | solo estructural o resistido de repeticiones | HEURISTICA_PLANIFICACION |
| V24-GM serie adicional | Validación | +1 serie en relleno pequeño o bíceps en PULL, solo si V20 la necesita | CONTRADICTORIA_O_DEFECTUOSA (parche de V20) |
| V24-ext | Validación | dosis exacta por capacidad; ≤ 8 colocaciones | PENDIENTE_VALIDACION_PROFESIONAL / POTENCIALMENTE_EXCESIVA |
| V25 | Validación | principales factibles presentes; cardio en cada sesión (LW); 4 slots de fuerza (IS) | HEURISTICA_PLANIFICACION / PENDIENTE_VALIDACION_PROFESIONAL |
| DIVISION | Mensaje | sugiere la división que cumple V13 y V18 | FUNCIONAL |
| R-GM motor 4D | Construcción | relleno ≤ tope y ≤ presupuesto; sin subir series | HEURISTICA_PLANIFICACION |
| R-CAP CapabilityWeekBuilder | Construcción | ≤ 8 colocaciones; series ≤ 3; sin inflar descansos | HEURISTICA_PLANIFICACION |
| R-SEL topes del selector | Construcción | lower legs 1, waist 2, upper arms 2 por sesión | HEURISTICA_PLANIFICACION |
| R-TP TargetPrescription | Construcción / normalizador | principiante 2 series; tabla 20.4 | HEURISTICA_PLANIFICACION |
| R-VR VolumeReducer | Construcción | pisos de 6 reps, 2 series, 2 ejercicios (inactivo) | FUNCIONAL (inactivo) |
| A-INT | IA generación y adaptación | 3 intentos y después reglas o copia | FUNCIONAL_NECESARIA |
| A-CB cortacircuitos | IA | 3 fallos seguidos → 10 min sin IA | FUNCIONAL_NECESARIA |
| A-PROMPT | IA adaptación | prompt v1 (FIX-PR-1.3); v2 / v2.1 experimentales | FUNCIONAL |
| A-FPI factibilidad previa | IA adaptación | prueba imposibilidad solo en ACORTAR y REPROGRAMAR | FUNCIONAL_NECESARIA |
| D-DEC decisor | Adaptación | cobertura ≥ 50 %; < 50 % ampliada; 50–80 % localizada; ≥ 80 % NO_CHANGE; dolor primero | PENDIENTE_VALIDACION_PROFESIONAL |
| D-SEÑ identificador | Analítica | sesión señalada si cumplimiento < 80 % con registro | HEURISTICA_PLANIFICACION |
| D-TIPO permisos | Adaptación | matriz de 8 intervenciones | FUNCIONAL_NECESARIA |
| D-FT FaltaDeTiempo | Adaptación | sin minutos → CONSERVAR; M ≥ sesión → CONSERVAR | FUNCIONAL_NECESARIA |
| V8C (núcleo) | Adaptación | solo cambian las señaladas y solo lo autorizado | FUNCIONAL_NECESARIA |
| V8C-IA-DOSIS | Adaptación | la IA nunca sube dosis | HEURISTICA_PLANIFICACION |
| V8C-RETIRADA | Adaptación | ACORTAR: 1 sola retirada, solo relleno, sin tocar el resto | POTENCIALMENTE_EXCESIVA |
| V8C-DOLOR | Adaptación | no reaparece; añadidos ≤ retirados; volumen ≤ antes | SEGURIDAD_INDISPENSABLE |
| V8C-PATRÓN H1 | Adaptación | mismo patrón (multiconjunto) | HEURISTICA_PLANIFICACION |
| V8C-TOPE-M | Adaptación | sesión corregida ≤ M declarados | FUNCIONAL_NECESARIA |
| V8C-DIF guarda de dificultad | Adaptación | apagada | CONTRADICTORIA_O_DEFECTUOSA (laguna) |
| D8 exención de V20 | Adaptación | solo ACORTAR y su copia idéntica | CONTRADICTORIA_O_DEFECTUOSA |
| D-ESTR validación estricta | Adaptación | 4D en ENFORCE; capacidades solo con interruptor | CONTRADICTORIA_O_DEFECTUOSA (GM) |
| D-SUST SUST-SEG-1 | Adaptación | mismo slot, tipo, zona y V18; dosis recalculada | SEGURIDAD_INDISPENSABLE |
| D-RESP respaldo con dolor | Adaptación | sin IA → NO_FACTIBLE salvo P-02 cubierto | SEGURIDAD_INDISPENSABLE |
| D-RET retirada localizada | Adaptación | apagada; requiere la guarda de dificultad | PENDIENTE_VALIDACION_PROFESIONAL |
| P-03 regeneración | Adaptación | motor de reglas; no más reps ni segundos que la semana medida | SEGURIDAD_INDISPENSABLE (posible bloqueo excesivo) |
| PR-UMB progresión | Progresión | 100 % real en 2 semanas; 5 %; techo 1,10 | POTENCIALMENTE_EXCESIVA (apagada) |
| PR-RANGO | Progresión | +2 reps; series ≤ 3; reducción ≥ min_reps; GM sin reducción | PENDIENTE_VALIDACION_PROFESIONAL |
| PR-REINC | Progresión | dosis inicial exacta; requiere progresión previa; cabe en M | HEURISTICA_PLANIFICACION |
| M-ADH adherencia | Analítica | media sin ponderar; observada; cobertura | FUNCIONAL_NECESARIA |
| M-RIESGO | Analítica | puntos 0/10/20/30; RPE ≥ 8; abandono a los 14 días | HEURISTICA_PLANIFICACION (no decide) |
| M-REP | Ejecución | reporte retroactivo ≤ 7 días; sesión abandonada a las 6 h | FUNCIONAL_NECESARIA |
| T-PORC tabla porcentual | Adaptación (fuera de flujo) | −20/−30/−10 %, +5 %, −40 %/+25 %, mínimo 2 días, mínimo 20 min | CONTRADICTORIA_O_DEFECTUOSA (latente) |
| BD restricciones CHECK | Persistencia | series ≥ 2, reps ≥ 6, ≥ 20 s; duración 10–180 | FUNCIONAL_NECESARIA |

---

## 2. Respuestas explícitas

### a) ¿Qué significa `sessionMinutes`?

**En el código es un valor nominal M con un intervalo aceptado de [0,70·M, 1,15·M].** Ambos extremos se miden con el estimador, que **incluye el calentamiento**.
- **Piso (V20).** `PV:1512` `minimumSessionSeconds = ceil(M·60·floorPct/100)` con `floorPct = 70`. Se aplica a `effectiveSessionMinutes()` (`PGC:167`). Da 1.260 s con 30 min, 1.890 s con 45 y 3.150 s con 75.
- **Techo (V4).** Con la política histórica, `PV:1022` compara `expectedDurationMinutes`, que el normalizador iguala a `round(s/60)`, con `PlanningProfile.maxSessionMinutes() = round(M·1,15)` (`PlanningProfile.java:45`). Con V4-C (candidata, no activa) compara `s·100 > M·60·115` (`PlanningPolicy.java:55`). V4 usa siempre los minutos **del perfil**, no los efectivos (`PV:1006,1009`).
- **Brecha de redondeo.** Con M = 45, el techo histórico acepta hasta 3.149 s; el exacto sería 3.105 s. Lo cubre `BrechaRedondeoV4Test`. Además, `UserProfile.maxSessionMinutesAllowed()` usa `floor` (`UserProfile.java:179-180`, 34 para M = 30) y planning usa `round` (35): el perfil expone un techo distinto del que se valida.

**La documentación no es coherente:**
- `GoalType.java:13-21`: «límite de ADMISIÓN… no un máximo de la duración real».
- Migración V14: «session_minutes es una preferencia del usuario… debe ceder la preferencia».
- `PlanPromptBuilder.java:201-203`: «la persona reservó ese tiempo».
- `FaltaDeTiempoPolicy`: la M declarada es la «disponibilidad para ESTE entrenamiento», y V8C la usa como **máximo** (`CV:517-525`).
- `MDP:62`: el presupuesto es el 85 % «útil».

**Consecuencias:**
- Si M es el tiempo disponible, V4 permite excederlo un 15 %.
- Si M es un objetivo, GAIN_MUSCLE no puede alcanzarlo. Con el tope 4D y solo estructurales, el contenido máximo es 2.220 s con 45 min (82 %), 2.220 s con 52 (71 %), 2.550 s con 60 (71 %) y 3.210 s con 75 (71 %). Cálculo: `300 + n·270 + (n−1)·60` con `n = maxColocaciones(M)`.
- En la práctica, el piso de V20 actúa como objetivo.

### b) ¿Hay perfiles que P-01 admite y el generador no puede satisfacer?

**Sí.** P-01 admite 30–75 min, 1–7 días, 3 niveles, HOME/GYM/MIXED, cualquier equipamiento y permisos de impacto (`UserProfile.java:194-221`, `GoalType.java:22-33`). Su propio javadoc avisa: «tampoco garantiza que exista un plan factible».

Evidencia documental de la matriz de 960 perfiles, 29 inválidos (`docs/short_paper_ieee/k20_adaptacion_real/evidencia/v34/PERFILES_SIN_PLAN_VALIDO.csv`, política PLAN-HIST-1):

| Familia | Perfiles | Regla que falla |
|---|---|---|
| `V15_LOSE_WEIGHT_1_DIA` | 6: LOSE_WEIGHT intermedio o avanzado, 1 día, 45 min, los tres equipamientos | V15 |
| `PRINCIPIANTE_75_MIN` | 7: LOSE_WEIGHT principiante en casa, 4 y 7 días; GENERAL_FITNESS principiante de 1 día en los tres equipamientos | V20 |
| `PESO_CORPORAL_6_7_DIAS` | 16: GAIN_MUSCLE con 45, 60 y 75 min, e INCREASE_STRENGTH con 75 min; intermedio y avanzado, peso corporal | V20 |

Además:
- J03 (LOSE_WEIGHT principiante, peso corporal, 4 días, 70 min) falla V20 por 234 s (`13_LIMITACIONES.md` §4).
- Por construcción, un principiante que pide 5–7 días recibe 4, y un no principiante que pide 7 recibe 6 (`PGC:152-157`). No falla, pero no satisface lo declarado.
- **NO VERIFICADO:** combinaciones de edad ≥ 60/65/70 con permisos declarados en `false` y peso corporal. La matriz no las cubre según los documentos leídos.

### c) ¿Generación y adaptación usan límites contradictorios?

**Sí.**
1. **V20 en la adaptación.** `minimumSessionMinutes` devuelve 0 solo si `adjustment.isActive()` (`PV:1518-1522`). La orden de adaptación nunca fija `targetVolume` (`WeeklyCycleService.toAdjustment`, `porcentual = false`), así que **V20 rige en toda la semana adaptada**. Solo quedan exentas las fechas `fechasConDuracionReducible` (`PV:1491`): ACORTAR (`CV:533-535`) y la herencia D8 (`CV:482-493, 536-555`). FATIGA (REVISAR_CARGA_Y_RECUPERACION), MOTIVACIÓN (INVESTIGAR_CAMBIO_MINIMO) y DIFÍCIL pueden cambiar la dosis según V8C, pero no bajar del 70 %.
2. **4D fija en GAIN_MUSCLE.**
   - `StrictValidationAdapter.java:62-71` construye el validador de adaptación con `DoseEnforcement.ENFORCE` siempre.
   - `AWP:189` se niega a arrancar si no bloquea.
   - `ProgresionDosisPolicy.admite4D` devuelve `false` con origen REDUCCION (`ProgresionDosisPolicy.java:39-41`): «En GAIN_MUSCLE no hay reducciones autorizadas».
   - Con progresión y capacidades apagadas (por defecto), V8C ni siquiera emite autorizaciones (`AWP:198`).
   - Resultado: ante fatiga, una sesión de GAIN_MUSCLE solo puede quedar igual o bajar la carga sugerida (`TipoIntervencion.permiteReducirCarga`). No puede quitar ejercicios (`permiteQuitarEjercicio` solo con ACORTAR).
   - `docs/POLITICA_ADAPTACION_Y_PROGRESION.md` §2 lo reconoce («V24/4D no admite reducir las dosis obligatorias»).
3. **Mínimo de minutos de la tabla porcentual (latente).** `forcedSessionMinutes` tiene piso de 20 (`min_session_minutes` de V7), por debajo de P-01 (30). V4 sigue usando los minutos del perfil (`PV:1006`) mientras V20, V24 y V25 usan los efectivos. La adenda 4D v3.1 §6 documenta que una orden «45 sobre 60» deja P-2D sin semana válida.
4. **Relleno del respaldo.** `RB:172` calcula el piso sin mirar el ajuste («el plan BASE se arma siempre igual»), pero `CWB:139` usa `minimumSessionMinutes(context)`, que es 0 con orden de volumen. Los dos caminos del motor no rellenan igual (latente, porque hoy no hay órdenes de volumen).

### d) ¿Cómo se distingue la imposibilidad real del fallo del algoritmo?

- **Generación inicial: no se distingue.** `PlanGenerationPipeline.run` (líneas 93-174) hace 3 intentos de IA y después el motor de reglas. Si este falla, lanza `PlanGenerationFailedException` con «Ultimos problemas: …», sin código. El comentario (línea 166) dice «el conjunto elegible o el perfil hacen imposible cualquier plan válido», cosa que no se demuestra. `docs/DIAGNOSTICO_V20.md` §2 muestra un fallo de V20 «por composición» con 117 ejercicios elegibles sin usar. `CWB:828-844` existe precisamente porque había «semanas que pasan V1-V25 y que el motor no construía». Las únicas comprobaciones previas son de pool (`SlotCoverageGate`, en OBSERVE, y `GoalFeasibilityPolicy`, solo observación): no prueban la factibilidad de la semana. La adenda 4D v3.1 §4 promete un `NO_FACTIBLE:` que no existe en el código.
- **Adaptación:**
  - `AdaptedWeekResult` con estado `NO_FACTIBLE` y `NoFactibleReason` (10 códigos).
  - `FactibilidadPreviaIA.imposibilidad` (`FactibilidadPreviaIA.java:52`) solo **demuestra** imposibilidad en dos casos: ACORTAR con minutos declarados (cota inferior de operaciones simples) y REPROGRAMAR sin fecha libre. Ante la duda, llama a la IA.
  - `FactibilidadAdaptacion`, del prompt v2 experimental y apagado, describe márgenes de V20 por cambio simple, sin combinaciones.
  - La «sonda» de la evaluación «solo enumera modificaciones simples» (`13_LIMITACIONES.md` §6).
  - **Respaldo:** sin IA válida se publica la copia (continuidad), salvo dolor. Con dolor: P-02 cubierto → SUST-SEG-1; si no, `NO_FACTIBLE`, y después P-03 (motor de reglas determinista con la comprobación «no más trabajo»).
  - `NO_FACTIBLE` mezcla imposibilidad real, imposibilidad inducida por las reglas (GAIN_MUSCLE con fatiga) y fallo de la IA. `SIN_EFECTO` / `RESPALDO` mezclan «no había cambio legal» con «la IA no lo encontró».

### e) ¿Cómo se calcula la duración estimada?

`SDE:46-51` y `SDE:64-91`:

```
s = warmup_minutes·60                                   (300 s, MVP-1.2: «estimaciones del desarrollador»)
  + Σ_ej [ trabajo + max(0, series−1)·descanso ]         (descanso NO tras la última serie;
                                                          null → default_rest_seconds = 60)
  + (n−1)·transition_seconds                             (60 s)
trabajo SETS_REPS = series·reps·seconds_per_rep (3 s);   DURATION = series·segundos;   series null → 1
```

- **Cuenta** calentamiento, transiciones y descansos entre series.
- **No cuenta** el descanso tras la última serie, el trabajo por lado (unilateral ×2), la fatiga ni las esperas.
- Los minutos se redondean con `max(1, round(s/60))`; V20 compara los segundos sin redondear.
- **4D usa otra fórmula** (`MDP:298-301`): `series·(reps·3 + descanso)`. Cobra el descanso tras cada serie, no incluye calentamiento ni transiciones y omite las colocaciones DURATION. El presupuesto es optimista (lo dice `PV:519-529`).
- `MDP:114-118` duplica como constantes 300 s, 60 s y 70 % en lugar de leer la configuración. Solo `MvpDosePolicyTopeTest` detectaría una divergencia.
- Parámetros sin calibrar (migración V14).

### f) ¿Qué hace el motor de reglas cuando no llega al piso de V20?

- **GAIN_MUSCLE** (`RB:184-238`):
  1. `addUntilMinimum` añade colocaciones, primero estructurales, hasta `maxColocaciones(M)` (6/7/8/9) y sin pasar del presupuesto del 85 %.
  2. **No sube series:** la dosis es fija.
  3. `sustituirRellenoPorEstructural` (`RB:345`) cambia relleno (150 s) por estructural (270 s) si se mantienen los grupos de V15.
  4. `ampliarRelleno` (`RB:398`) añade la serie adicional de 4D v3.2/v3.3 (2 → 3) al relleno de zona pequeña o al bíceps en PULL, de una en una y solo mientras falte tiempo.
  5. **Nunca añade cardio** (`puedeDemostrarHipertrofia`) ni alarga descansos.
  6. Si no llega, V20 rechaza → `PlanGenerationFailedException`.
- **INCREASE_STRENGTH, LOSE_WEIGHT y GENERAL_FITNESS** (`CWB:251-362`):
  1. Añade secundarias (o más de la principal, incluido cardio en LOSE_WEIGHT, con la regla «dejar cardio al día siguiente») hasta `MAX_COLOC_DIA = 8` y sin superar V4.
  2. `ampliarSeries` sube series hasta 3 (`SERIES_MAX_AMPLIACION`) dentro del rango de la dosis.
  3. `sustituirPorDuracion` cambia una colocación corta por otra admisible más larga.
  4. `repasoSemanal` revisa la semana entera.
  5. Nunca toca el descanso.
- **Rama heredada** (`raiseSetsUntilMinimum`, 3 series; hasta 8 ejercicios): solo se alcanza con un objetivo sin matriz. Hoy es código muerto para los cuatro objetivos.

---

## 3. Restricciones (11 campos)

Formato: 1 propósito · 2 ubicación · 3 parámetros · 4 fase · 5 razón documentada · 6 interacción · 7 permite · 8 impide · 9 escenarios legítimos bloqueados · 10 evidencia · 11 doc frente a código · **Clasificación**.

### 3.1 Perfil y admisión

#### P-01 · Rango de minutos por sesión
1. Admitir solo `session_minutes` en 30–75.
2. `profiling/domain/model/valueobjects/GoalType.java:22-33`; `UserProfile.requireMinutesForGoal` (`UserProfile.java:194-201`) y `applyAvailability` (`:203-210`); se repite en `planning/domain/services/DuracionPorObjetivoPolicy.java` y en `WeeklyTrainingPlanCommandServiceImpl.prepare` (`:131-134`, `ProfileOutsideGoalDurationException`).
3. 30 ≤ M ≤ 75, igual para los 4 objetivos. La BD admite 15–180 (`V1__users_and_profiles.sql:80`).
4. Alta y edición de perfil; prepare.
5. «P-01 DEFINITIVA (orden de fusión del PR #16)». Sustituye a 30–120 y 15/30–180.
6. Alimenta V4, V20 y 4D (`maxColocaciones`, presupuesto). Se duplica a propósito entre contextos (`PoliticaP01DuracionMinimaTest`).
7. Cualquier M entero en el rango.
8. M < 30 o > 75, y perfiles históricos fuera de rango.
9. Personas con 20–25 min disponibles (ACORTAR sí puede bajar de 30 en la adaptación: D8).
10. `PoliticaP01DuracionMinimaTest`. Sin cita científica.
11. Coincide. El javadoc reconoce que no garantiza factibilidad (§2.b).
**FUNCIONAL_NECESARIA** (admite perfiles no generables).

#### P-DÍAS · Días por semana y días disponibles
1. Coherencia de disponibilidad.
2. `UserProfile.java:204-222`; `ck_profile_available_days` (`V1:78`).
3. 1 ≤ días ≤ 7; días disponibles únicos ≥ días/semana.
4. Perfil.
5. «Entero exacto… denominador base de la adherencia» (`UserProfile.java:58`).
6. `effectiveDaysPerWeek` lo recorta después (S-MAX).
7. Cualquier combinación coherente.
8. Más sesiones que días marcados.
9. Ninguno relevante.
10. Sin prueba localizada específica (NO VERIFICADO).
11. Coincide.
**FUNCIONAL_NECESARIA**.

#### P-BLOQ · Perfil inmutable desde el primer plan
1. Estabilidad del estudio.
2. `profiling/application/internal/commandservices/UserProfileCommandServiceImpl.java:51-53`.
3. Si existe algún plan → `UserProfileLockedException`.
4. Edición de perfil.
5. Comentario: «Regla del estudio: desde el primer plan, el perfil no cambia».
6. La copia semanal (continuidad) arrastra la semana 1 indefinidamente. El cambio de material o de disponibilidad no llega a ninguna regla.
7. Nada tras el primer plan.
8. Actualizar equipamiento, días, minutos, nivel o permisos de movimiento.
9. Lesión nueva declarada como permiso (`allow_*`), mudanza sin material, cambio de horario. Solo queda el canal del dolor (P-02) o la adaptación.
10. Sin documento de diseño localizado.
11. **Discrepancia:**
    - El javadoc de `UserProfile.update` (`UserProfile.java:117-121`) dice que los cambios «se aplican en la generación del lunes siguiente».
    - `PlanLimitationsService` aconseja «puedes añadirlo en tu perfil».
    - `MovementAllowances.conCambios` diseña con cuidado una actualización que nunca puede ocurrir tras el primer plan.

**POTENCIALMENTE_EXCESIVA / CONTRADICTORIA_O_DEFECTUOSA** (el permiso de seguridad declarado tarde no se puede registrar).

### 3.2 Elegibilidad del catálogo

#### E-DIF · Techo de dificultad por nivel (V6 en consulta)
1. No ofrecer ejercicios por encima del nivel.
2. `FitnessLevel.java` (1/2/3); `PlanningProfile.maxDifficultyLevel` (`:32`); `ExerciseRepository.findEligible` (`difficultyLevel <= :maxDifficulty`); `PGC:135-139` (`effectiveMaxDifficulty`); V6 en `PV:686`.
3. BEGINNER 1, INTERMEDIATE 2, ADVANCED 3. `LOWER_DIFFICULTY` baja un nivel con piso 1 (latente).
4. Catálogo y validación.
5. Sección 20.3 del diseño.
6. V15 (`prescribibleParaV15`) y SUST-SEG-1 lo respetan.
7. Ejercicios ≤ nivel.
8. Ejercicios por encima.
9. Un principiante con buena base; dificultades mal catalogadas (V27 dice que `difficulty_level` «NO representa» una revisión humana).
10. Migraciones V21, V27, V28, V36 y V37 corrigen niveles. El filtro de aprobaciones humanas está apagado (E-APROB).
11. Coincide. **Riesgo de dato:** V37 eleva a 3 ejercicios como «muscle up» o «front lever» que antes eran 1.

**SEGURIDAD_INDISPENSABLE** (con dato pendiente de revisión profesional).

#### E-EQ · Equipamiento (relación N:M)
1. Ofrecer solo lo realizable con el material declarado.
2. `ExerciseRepository.findEligible` (doble `NOT EXISTS`); `ExerciseQueryServiceImpl.handle(GetEligibleExercisesQuery)` (añade siempre «body weight»; HOME excluye `requires_gym`).
3. AND entre grupos y OR dentro del grupo. Un ejercicio sin requisitos pasa siempre.
4. Catálogo.
5. Javadoc del repositorio: «la columna admitía 592, la relación real 530».
6. Si un slot queda vacío → V23 no lo exige y `PlanLimitationsService` lo comunica.
7. Ejercicios compatibles.
8. Ejercicios con material no declarado.
9. MIXED no excluye nada de gimnasio (solo HOME) (NO VERIFICADO si es lo pretendido).
10. Migraciones V22, V23, V29, V38 y V39.
11. Coincide.

**FUNCIONAL_NECESARIA**.

#### E-SEG · SafetyProfile (edad, nivel y permisos declarados)
1. Excluir movimientos de riesgo.
2. `planning/domain/model/valueobjects/SafetyProfile.java:35-41, 82-114`; se aplica en `prepare` (`WeeklyTrainingPlanCommandServiceImpl.java` ~l.165) y en la consulta.
3.
   - Edad ≥ 60 o BEGINNER → sin alto impacto.
   - Edad ≥ 65 → sin carga axial ni inversión.
   - Edad ≥ 70 → sin suelo.
   - Permisos declarados en `false` → sin impacto, suelo, rodillas, suspensión o brazos arriba.
   - La combinación es OR: lo declarado nunca relaja la política.
4. Catálogo. También alimenta `SafetyConflictPolicy` para la adaptación.
5. «Los cortes de edad son criterio conservador, no una guía clínica validada. Conviene contrastarlos con el asesor» (`:21-22`). ACSM 2009 para la pliometría en principiantes (`:103-104`).
6. Reduce el pool. Es causa de V23 y V25 «no factibles» y de los perfiles sin plan.
7. Lo seguro según las banderas.
8. Lo marcado.
9. Personas de 60+ entrenadas que toleran impacto; restricciones que vacían slots enteros.
10. `SafetyProfileDeclaradasTest`. Cita ACSM sin especificar sección.
11. Coincide. La inversión se ata a la edad de la carga axial «como criterio de diseño».

**SEGURIDAD_INDISPENSABLE + PENDIENTE_VALIDACION_PROFESIONAL** (los cortes de edad).

#### E-RET · Retenidos INC-4C-03 y contención INC-CAT-1374
1. Retirar ejercicios con datos peligrosos.
2. `ExternalCatalogService.java:53-54` (ids 813, 858, 861, 873, 885, 1192, 1298) y `:72-80` (el 1374 solo con «pull-up bar», sin suspensión ni brazos arriba restringidos).
3. Lista cerrada, sin interruptor.
4. Catálogo.
5. Auditoría del bloque 7 (15/10) y `docs/INC_CAT_1374.md`.
6. `SafetyConflictPolicy.CAUSAS` lo usa para sustituir heredados.
7. —
8. Esos ids.
9. Ninguno razonable.
10. `docs/INC_CAT_1374.md`, `docs/REVISION_CATALOGO_CARDIO.md`.
11. Coincide.

**SEGURIDAD_INDISPENSABLE**.

#### E-FUN · Filtro funcional (CatalogEligibilityPolicy)
1. Quitar zonas sin enfoque y estiramientos, salvo que sean la ruta de una capacidad exigida.
2. `planning/domain/services/CatalogEligibilityPolicy.java:68, 97-124`; se aplica en `ExternalCatalogService.java:172-174`.
3. Fuera: `neck`, `lower arms`, `cardio` y los nombres con «estiramiento». Vuelven LOCOMOTION_CARDIO (LOSE_WEIGHT, GENERAL_FITNESS) y MOBILITY (GENERAL_FITNESS) como **complemento**.
4. Catálogo.
5. `docs/PASO2_CATALOGO.md`; hallazgo M.
6. Los complementos no cuentan para V15 (`PV:776-806`) ni los elige el selector (`RB:149-157`).
7. Cardio en LOSE_WEIGHT y GENERAL_FITNESS; movilidad en GENERAL_FITNESS.
8. Cardio en GAIN_MUSCLE e INCREASE_STRENGTH; movilidad en LOSE_WEIGHT, GAIN_MUSCLE e INCREASE_STRENGTH; trabajo de antebrazo o cuello.
9. Calentamiento o movilidad en cualquier objetivo; cardio de salud general en GAIN_MUSCLE.
10. `CatalogEligibilityPolicyTest`.
11. Coincide. Reconoce una «inconsistencia conocida» (8 LOCOMOTION_CARDIO de upper legs o waist y 8 MOBILITY entran como trabajo normal).

**HEURISTICA_PLANIFICACION**.

#### E-REV · Clasificación en revisión
1. Que 21 ejercicios con patrón dudoso no acrediten capacidad.
2. `GoalFeasibilityPolicy.java:61-88`; `CPP.claseDe` (→ EN_REVISION).
3. 21 ids; 853 y 1117 salen de revisión si el catálogo trae LOCOMOTION_CARDIO + DURATION.
4. Catálogo, V24, V25 y construcción.
5. Incidencia abierta del Paso 2; decisión D1 (30/10/2026).
6. Reducen el pool efectivo de cardio y movilidad.
7. Que sigan elegibles, pero sin acreditar.
8. Usarlos como evidencia.
9. Pocos cardios en casa para principiantes (agrava V20/V25 en LOSE_WEIGHT).
10. `docs/DIAGNOSTICO_V37_1.md`.
11. Coincide.

**FUNCIONAL_NECESARIA** (temporal).

#### E-DOLOR · Memoria persistente del dolor (P-02)
1. No volver a prescribir lo que dolió.
2. `PainMemoryService.java:40-51, 151, 181`; se resta en `prepare` (`WeeklyTrainingPlanCommandServiceImpl.java` ~l.170-178).
3. **No caduca.** Solo `reactivar` con confirmación explícita.
4. Catálogo (todas las fases).
5. P-02 aprobada (31/10/2026).
6. Genera exclusiones para SUST-SEG-1, V8C y P-03. Con muchas exclusiones el pool se vacía (`EmptyEligibleSetException`).
7. —
8. Los ids que dolieron.
9. Dolor transitorio no relacionado con el ejercicio; exclusiones acumuladas que dejan sin plan.
10. Pruebas `MigracionV43IT` y P-03 (NO VERIFICADO su alcance).
11. Coincide.

**SEGURIDAD_INDISPENSABLE**.

#### E-APROB · Aprobaciones humanas de dificultad
1. Admitir solo dificultades revisadas.
2. `catalog/application/internal/queryservices/ApprovedDifficultyGate.java`.
3. `fitsense.approvals.filter-enabled=false`.
4. Catálogo.
5. V27/V28: solo 4 aprobaciones (LOWER_HIP).
6. Encenderlo dejaría el plan imposible.
7. Todo (apagado).
8. Nada.
9. —
10. Javadoc con números.
11. Coincide.

**FUNCIONAL** (inactivo).

### 3.3 Pregeneración

#### G-4C · Cobertura 4C previa (SlotCoverageGate)
1. Detectar slots sin candidatos antes de gastar IA.
2. `SlotCoverageGate.java`; llamada en `WeeklyTrainingPlanCommandServiceImpl.java:197`.
3. Umbral `MINIMO_ROBUSTO = 3` (`SlotCoverage.java:39`). `fitsense.coverage.pregeneration=OBSERVE`: solo registra. En ENFORCE_* daría 422.
4. prepare.
5. 4C §6, en comentarios de `application.properties`.
6. Independiente de V23.
7. Generar aunque falte un slot.
8. Nada (OBSERVE).
9. —
10. `SlotCoverage4CTest`.
11. Coincide.

**HEURISTICA_PLANIFICACION**.

#### G-4A · Factibilidad por objetivo (GoalFeasibilityPolicy / GoalFeasibilityObserver)
1. Medir el pool por capacidad y la presencia en la semana.
2. `GoalFeasibilityPolicy.java:200-271, 310-347`; `GoalFeasibilityObserver`.
3. Mismos umbrales 0 / 1–2 / ≥ 3; un circuito queda NO_VERIFICABLE.
4. Antes y después de generar, solo en la inicial.
5. Paso 3 (`docs/PASO3_FACTIBILIDAD.md`).
6. No decide nada.
7–9. —
10. `FactibilidadPorObjetivoIT`.
11. El comentario de `exigeSlotsResistidos` (`:384-387`) dice que «V23 los exige hoy a TODOS los objetivos». Ya no es así: `PV:936-941` exime a LOSE_WEIGHT. El texto está desactualizado.

**HEURISTICA_PLANIFICACION**.

### 3.4 Planificación semanal

#### S-MAX · Tope de sesiones por nivel
1. Evitar demasiados días de fuerza.
2. `PGC:142-157`.
3. BEGINNER 4, resto 6.
4. Planificación (V2 y split).
5. «ACSM 2009 recomienda 2-3 días para principiantes».
6. V2 exige este número; la adherencia se mide contra lo planificado.
7. 1–4 o 1–6.
8. 5–7 en principiantes y 7 en el resto.
9. Principiante que quiere caminar o hacer cardio 5 días (LOSE_WEIGHT): no es fuerza y aun así se le recorta.
10. Cita ACSM 2009 (texto genérico).
11. **Discrepancia:** el comentario cita 2–3 días para principiantes y el tope es 4. Se aplica igual a LOSE_WEIGHT, cuya principal es CARDIO.

**HEURISTICA_PLANIFICACION**.

#### S-SPLIT · WeeklySplitPlanner
1. Elegir fechas y enfoques.
2. `WeeklySplitPlanner.java:39-107`.
3.
   - 1 día → FB.
   - 2 días → FB/FB, o UL si son seguidos.
   - 3 días → FB×3, o ULU/LUL si son seguidos.
   - 4 días → ULUL.
   - 5 días → PUSH, PULL, LEGS, UPPER, LOWER.
   - 6 días → PPL×2.
   - Fechas repartidas uniformemente.
4. Planificación (motor, `suggested_split` de la IA y mensaje DIVISION).
5. P1 recuperación de 48 h (ACSM, Garber 2011); P2 frecuencia; P3 simplicidad.
6. Condiciona V13, V15, V18 y V22. Con 6 días seguidos y peso corporal, V18 deja muy poco repertorio, lo que contribuye a la familia `PESO_CORPORAL_6_7_DIAS`.
7. —
8. Otras divisiones (por ejemplo, FB 3 días seguidos con baja intensidad).
9. El split no se revisa entre semanas (domingo → lunes): V18 no ve el cruce de semana.
10. Citas en el javadoc.
11. Coincide.

**HEURISTICA_PLANIFICACION**.

### 3.5 Normalización de la propuesta de IA

#### N-TIPO / N-DUR · PlanDraftNormalizer
1. Corregir el tipo de prescripción (dato del catálogo) y fijar la duración declarada con el estimador.
2. `PlanDraftNormalizer.java:150-153, 195-206, 211-255`.
3. Paso a DURATION: 3 × 40 s con 45 s de descanso. Paso a SETS_REPS: 2 series y las repeticiones de `TargetPrescription` llevadas al mínimo de V16. Duración = `estimateMinutes`.
4. Antes de validar la IA.
5. Plan 36; orden de optimización B2 (31/10/2026): «11 de los rechazos eran V17».
6. **Deja V17 sin efecto práctico** y hace que V4 histórico se compare con `round(s/60)`.
7. —
8. —
9. —
10. `PlanDraftNormalizerDuracionTest`.
11. Coincide. Es una «desviación documentada».

**FUNCIONAL_NECESARIA**.

### 3.6 Validador (PlanDraftValidator, V1–V25)

Orden de ejecución: `PV:145-163`. Devuelve todos los problemas, y esa lista es la entrada del reintento de la IA.

#### V1 · Ejercicio elegible
1. Solo ejercicios del conjunto elegible.
2. `PV:678-683`.
3. —
4. Validación en generación y adaptación.
5. 19.3.
6. Hace cumplir en la salida de la IA todos los filtros E-*. En la adaptación rechaza heredados que dejaron de ser elegibles por causas que **no** son de seguridad (equipamiento, bloqueo, inactivo, reclasificación); para esos no hay SUST-SEG-1 (`SafetyConflictPolicy` javadoc).
7. —
8. Ids fuera del pool.
9. Copia semanal tras un cambio de catálogo (V36–V41 reclasifican) → semana NO_FACTIBLE (SEMANA_NO_VALIDA).
10. Varias pruebas de integración.
11. Coincide.

**SEGURIDAD_INDISPENSABLE**.

#### V2 · Número de sesiones
1. Sesiones = días efectivos.
2. `PV:703-706`.
3. `effectiveDaysPerWeek()` (`PGC:152`).
4. Validación.
5. 19.3.
6. El split usa `min(efectivos, días disponibles)` (`PGC:162`). Con la tabla porcentual latente, `forcedDays` (mínimo 2) podría superar los días disponibles y V2 sería imposible (T-PORC).
7. —
8. Más o menos sesiones.
9. Persona que solo podrá ir 2 de 3 días esa semana (resuelto por adaptación, no por generación).
10. —
11. Coincide.

**FUNCIONAL_NECESARIA**.

#### V3 / V12 · Fechas
1. Fecha en los días disponibles y dentro de la semana; una sesión por día.
2. `PV:711-720`.
3. —
4. Validación.
5. 19.3.
6. REPROGRAMAR está limitado por esto (`FactibilidadPreviaIA`).
7. —
8. Dos sesiones el mismo día o fuera de los días declarados.
9. Sesión doble corta.
10. —
11. Coincide.

**FUNCIONAL_NECESARIA**.

#### V4 · Techo de duración
1. No exceder el tiempo declarado más una tolerancia.
2. `PV:1004-1024`; `PlanningPolicy.java:23, 47, 55`; `PlanningProfile.java:45`; el motor recorta con `RB:240-247, 655-660` y `CWB:759-770, 1168-1172`.
3. Histórica: `round(s/60) ≤ round(M·1,15)` con M del **perfil**. V4-C (candidata): `s·100 ≤ M·60·115`.
4. Validación y construcción.
5. 19.3 «session_minutes + 15 %». La campaña de estrés (31/10) obligó a declarar la duración real. V44 versiona la política.
6. Con V20 forma la banda [70 %, 115 %]. ACORTAR añade un tope propio de M declarados (V8C-TOPE-M).
7. Hasta 34–35 min con 30; 52 con 45; 86 con 75.
8. Exceder.
9. —
10. `BrechaRedondeoV4Test`, `V4CValidadorRealTest`, `PoliticaV4CCicloIT`. Sin cita para el 15 %.
11. **Discrepancias:**
    - Brecha de redondeo: con M = 45 acepta hasta 3.149 s frente a 3.105 s exactos.
    - Usa los minutos del perfil, no los efectivos, a diferencia de V20 y V24.
    - El perfil expone `floor(M·1,15)` (`UserProfile.java:179`); planning usa `round`.

**FUNCIONAL_NECESARIA** (tolerancia heurística; defecto de redondeo menor).

#### V5 · Mínimo de 2 ejercicios por sesión
1. Evitar sesiones triviales.
2. `PV:30, 1026-1029`.
3. ≥ 2.
4. Validación.
5. 18.4 «pisos de seguridad» (`PV:29`).
6. Junto con V9 y V20.
7. —
8. Una sesión de un solo ejercicio (por ejemplo, 30 min de bicicleta).
9. Cardio continuo de un bloque en LOSE_WEIGHT.
10. —
11. El comentario lo llama «límite de seguridad»; no hay justificación de seguridad.

**HEURISTICA_PLANIFICACION**.

#### V6 · Dificultad máxima
Igual que E-DIF, aplicada a la salida (`PV:685-687`). **SEGURIDAD_INDISPENSABLE**.

#### V7 · Tipo de prescripción y campos obligatorios
1. El tipo lo fija el catálogo.
2. `PV:693-697, 1038-1060`.
3. —
4. Validación (el normalizador lo corrige antes para la IA).
5. Migración V18, principio 7 («un estiramiento a 3x7»).
6. Depende de la calidad de `default_prescription` (V18 lo derivó por palabra clave).
7. —
8. Cambiar SETS_REPS ↔ DURATION.
9. Plancha por repeticiones o isométricos dinámicos mal catalogados.
10. —
11. Coincide.

**FUNCIONAL_NECESARIA**.

#### V8 · Volumen en banda (orden porcentual)
1. Comprobar que se obedece la orden de volumen.
2. `PV:1087-1100`.
3. `targetVolumeMin..Max`; `volume_tolerance_pct = 5`.
4. Validación.
5. «Es la validación que sostiene la tesis».
6. **Inactiva:** `isActive()` es falso con la orden actual (`UserInterventionCommandServiceImpl.java` ~l.155-170, `sinAjustePorcentual`). La sustituye V8C.
7–9. —
10. —
11. Coincide; documentado.

**FUNCIONAL** (inactiva en el flujo).

#### V9 / V10 · Pisos de series, repeticiones y segundos
1. Pisos mínimos.
2. `PV:32-34, 1050-1076`; CHECK `ck_pwe_min_*` (`V4__planning.sql:121-123`).
3. series ≥ 2; reps ≥ 6; DURATION ≥ 20 s; series ≥ 2 también en DURATION si se declaran.
4. Validación y persistencia.
5. 18.4; corrección 2 (27/10) para no reventar en la BD.
6. `ProgresionDosisPolicy.bajaBien` admite 1 serie (`:68`), pero V9 la rechaza. `FactibilidadPreviaIA` usa 2 (`:45`). STRENGTH 3×5 se convierte en 3×6 por esto (ver V24-ext).
7. —
8. 1 serie; 5 reps o menos.
9. Fuerza a 3×5 o 5×3 (habitual en fuerza); 1 serie para un principiante con fatiga (ACSM admite 1–3).
10. `SeriesMinimasV9Test`. Sin cita.
11. **Discrepancia:** `ProgresionDosisPolicy` documenta «ni por debajo de una serie», pero el mínimo real es 2.

**HEURISTICA_PLANIFICACION** (POTENCIALMENTE_EXCESIVA en reducción por fatiga y en fuerza).

#### V11 · Carga sugerida
1. Que la sugerencia de peso sea segura.
2. `PV:1122-1194`.
3. null sin registro previo; subir solo si se completó, ≤ +10 % (`max_load_increase_pct`, por defecto 10), sin subir a la vez series o reps; LOWER_LOAD sin subidas; DURATION sin carga.
4. Validación.
5. Principio 8, P-1.2; ACSM 2009 (2–10 %).
6. Se coordina con V8C-CARGA.
7. Mantener o bajar.
8. Subir más del 10 %; subir sin completar.
9. Saltos discretos de mancuerna (8 → 10 kg): el prompt ordena mantener el peso y subir repeticiones.
10. ACSM citado.
11. El RPE ≥ 8 no se valida (sí está en el prompt).

**SEGURIDAD_INDISPENSABLE**.

#### V13 · Enfoque no repetido en días seguidos
1. Variedad y recuperación.
2. `PV:731-745`.
3. —
4. Validación.
5. 19.3.
6. Lo complementa V18.
7–9. —
10. —
11. Coincide.

**HEURISTICA_PLANIFICACION**.

#### V15 · Cobertura del enfoque
1. Que cada sesión trabaje su enfoque.
2. `PV:773-860`; `prescribibleParaV15` `PV:888-904`.
3.
   - Mínimo `min(2, grupos disponibles prescribibles, ejercicios de trabajo)`.
   - Sin grupos ajenos, salvo FULL_BODY.
   - Con ≥ 3 grupos y ≥ 4 ejercicios, ninguno > ½.
   - Los complementos no cuentan.
4. Validación y construcción.
5. Primera prueba con el catálogo completo («seis de bíceps y tríceps»); `docs/CORRECCION_RESPALDO_5_6_DIAS.md`.
6. Choca con V24 en LOSE_WEIGHT (los resistidos no tienen capacidad) y se corrigió con `prescribibleParaV15`. Restringe a SUST-SEG-1 y al relleno de 4D.
7. —
8. Sesiones monogrupo; FULL_BODY sin variedad.
9. LOSE_WEIGHT de 1 día (familia `V15_LOSE_WEIGHT_1_DIA`, 6 perfiles); una sesión centrada en un grupo por preferencia.
10. `V15GruposPrescribiblesTest`, `V15CambioDeGrupoIT`, `CatalogoV15CoherenciaTest`.
11. **Discrepancia:** el comentario (`PV:825-827`) dice «con tope en 3» y el código usa 2.

**HEURISTICA_PLANIFICACION** (POTENCIALMENTE_EXCESIVA en 1 día).

#### V16 · Límite de repeticiones
1. Rechazar lo absurdo y diferenciar zonas.
2. `PV:1216-1266`; `PrescriptionParams.RepLimits` (`:64-91`).
3. 6–30 general; `lower legs` ≥ 12, `waist` ≥ 10 (migración V20); BEGINNER ≥ 8. El `by_goal` antiguo solo aplica a configuraciones anteriores a MVP-1.5.
4. Validación.
5. V18: «max_reps = 30 DECISIÓN DE DISEÑO PROVISIONAL, sin referencia»; V20: los mínimos por zona son «PROVISIONAL, sin referencia»; ACSM 2009 8–12 para principiantes.
6. Obligó a la excepción de zona pequeña de 4D (+3 reps); CPP fuerza STRENGTH a ≥ 6, y a ≥ 8 en principiantes.
7. —
8. Gemelos a 10, abdominales a 8, fuerza a 5.
9. Fuerza de 3–5 reps en ADVANCED (INCREASE_STRENGTH).
10. Migraciones V18 y V20 con su motivo.
11. Coincide.

**PENDIENTE_VALIDACION_PROFESIONAL**.

#### V17 · Coherencia entre duración declarada y contenido
1. Que la duración declarada sea honesta.
2. `PV:1269-1281`.
3. `duration_tolerance_pct = 20`.
4. Validación.
5. Migración V14 (se retira el piso del 70 % de V17).
6. El normalizador y el motor de reglas igualan lo declarado a la estimación, así que **en la práctica no se dispara nunca**.
7–9. —
10. —
11. Coincide; es redundante.

**FUNCIONAL** (neutralizada).

#### V18 · Recuperación entre días seguidos
1. 48 h para los grupos grandes.
2. `PV:1291-1342`.
3. El mismo ejercicio no va en días seguidos; `chest`, `back`, `shoulders` y `upper legs` tampoco.
4. Validación y construcción (selector, SUST-SEG-1, `dejaCardioAlSiguiente`).
5. Plan 30 / prueba; ACSM (Garber 2011) en el split.
6. Limita el cardio de LOSE_WEIGHT en días seguidos (`docs/IMPLEMENTACION_V20.md` §1.3) y el relleno en peso corporal con 6 días.
7. —
8. Repetición en días seguidos.
9. Cardio ligero que se repite; 6–7 días con poco material.
10. —
11. Coincide.

**HEURISTICA_PLANIFICACION**.

#### V19 · Topes de reducción
1. En REDUCE_VOLUME, no subir lo que no se completó.
2. `PV:1354-1460`.
3. HOLD: sin subida. COMPLETED: +1 serie / +2 reps. RPE ≥ 8 → HOLD.
4. Validación.
5. Principio 9.
6. **Inactiva** (sin REDUCE_VOLUME).
7–9. —
10. —
11. —

**FUNCIONAL** (inactiva).

#### V20 · Duración mínima
1. Que la sesión llene el tiempo pedido con trabajo.
2. `PV:1474-1523`; constructores en `RB:172, 184-238` y `CWB:139, 251-362`; prompt en `PlanPromptBuilder.java:420-440`.
3. `ceil(M_ef·60·70/100)` s, con el estimador (§2.e). Sin aplicación si hay orden de volumen (hoy nunca). Exentas las fechas D8/ACORTAR.
4. Validación de generación **y de adaptación** (copias incluidas).
5. «En la prueba, 45 minutos pedidos dieron sesiones de 22-27» (`PV:1464-1469`); V12 creó el 70 %; `docs/IMPLEMENTACION_V20.md` lo pasa a segundos.
6. **Es la regla más acoplada:**
   - determina el tope de 4D (v3.1/v3.4/C-8);
   - creó la serie adicional (v3.2/v3.3);
   - obliga a sustituir relleno por estructural;
   - en la adaptación limita las reducciones por fatiga y obliga a D8 y C-2;
   - en P-03, junto con «no más trabajo», puede dejar sin alternativa.
7. Sesiones del 70–115 % de M.
8. Sesiones cortas no autorizadas.
9.
   - Reducción por fatiga o motivación (cualquier objetivo).
   - Principiantes de 75 min.
   - Peso corporal con 6–7 días.
   - Una persona que prefiere sesiones más cortas que su M y no lo declara como «falta de tiempo».
10. `V20PisoExactoTest`, `V20ComposicionCasosIT`, `GeneracionR0548V20IT`. Sin referencia científica para el 70 %. V12 dice «Sin calibrar».
11. Coincide con `docs/IMPLEMENTACION_V20.md`. **Contradicción semántica** con FaltaDeTiempo, donde M es el máximo disponible, y con V14 («debe ceder la preferencia»).

**POTENCIALMENTE_EXCESIVA** (contradictoria en la adaptación).

#### V21 · Descanso máximo
1. Que la duración no se consiga inflando descansos.
2. `PV:1480-1485`.
3. 180 s (`max_rest_seconds`).
4. Validación.
5. ACSM 2009 (2–3 min para multiarticulares).
6. Ninguna dosis supera 150 s.
7. —
8. Más de 180 s.
9. Fuerza máxima con 3–5 min.
10. Cita ACSM.
11. Coincide.

**HEURISTICA_PLANIFICACION**.

#### V22 · Brazo según enfoque
1. PUSH con tríceps, PULL con bíceps.
2. `PV:1529-1546`.
3. Busca la cadena en `target_muscle`.
4. Validación.
5. Coherencia de enfoque.
6. —
7. —
8. Bíceps en PUSH.
9. —
10. —
11. Coincide.

**HEURISTICA_PLANIFICACION**.

#### V23 · Cobertura semanal de 4 slots (4C)
1. Que la semana toque LOWER_KNEE, LOWER_HIP, UPPER_PUSH y UPPER_PULL.
2. `PV:931-998`; `SlotCoverageEnforcement.java:95-101`.
3. Exige los slots con candidatos y que quepan por posiciones. `fitsense.coverage.enforcement=ENFORCE_AI_ONLY` por defecto: **solo rechaza la IA**. No se aplica a LOSE_WEIGHT (4A v6).
4. Validación.
5. Step 4C §5. Comentario de propiedades: el motor cubre 30 de 90 semanas.
6. En la adaptación, una corrección de la IA (origen AI) puede rechazarse por V23 mientras la copia (CONSERVED) no; es asimétrico. En GAIN_MUSCLE el motor busca los slots.
7. —
8. Semanas de la IA sin un patrón factible.
9. Reparto deliberado (por ejemplo, semana de descarga de pierna).
10. `SlotCoverage4CTest`.
11. **Discrepancia:** el comentario de `application.properties` dice «Se entrega en OBSERVE» y el valor por defecto es ENFORCE_AI_ONLY. `StrictValidationAdapter` lee la misma propiedad con valor por defecto OBSERVE (`:55`).

**HEURISTICA_PLANIFICACION** (asimetría).

#### V24-GM · Dosis 4D exacta (GAIN_MUSCLE)
1. Dosis obligatoria de hipertrofia.
2. `PV:532-668`; `MDP:189-310, 342-361`.
3.
   - Estructural (slot 4C) 3×10 con 90 s.
   - Relleno 2×10 con 90 s.
   - Zona pequeña (`lower legs`, `waist`) +3 reps (13).
   - Las colocaciones DURATION se abstienen.
   - `fitsense.dose.enforcement=ENFORCE` por defecto; en la adaptación, siempre ENFORCE.
4. Validación y construcción (`RB:603-612`).
5. «criterio-4D-alcance-MVP.md v2 (10/10/2026)» por decisión de Juan del 15/10. El descanso de 90 s se califica de «EMPÍRICO DÉBIL, Singer et al. 2024» (`MDP:190`).
6. Impide reducir el volumen ante fatiga (ver §2.c). Choca con V16 (resuelto con +3). Fuerza el cálculo del tope desde V20. STRENGTH secundaria nunca se asigna («25 % sin denominador»).
7. Exactamente esa dosis, o una autorizada por V8C (solo progresión).
8. Cualquier variación: 3×8, 4×10, 2×12, 60 s.
9. Principiantes que toleran 2 series; fatiga; periodización; GAIN_MUSCLE avanzado con más volumen o intensidad.
10. `MvpDosePolicyTest`, `EvaluacionDirectaV24IT`. Una cita (Singer 2024, solo para el descanso). Dosis «primer corte».
11. Coincide con 4D v3. `MDP:19-23`: «Retirar el tope de dos series para principiantes no demuestra que tres sean apropiadas para todos».

**PENDIENTE_VALIDACION_PROFESIONAL / POTENCIALMENTE_EXCESIVA**.

#### V24-GM-TOPE · Tope de colocaciones por día (4D v3.1 / v3.4 / C-8)
1. Limitar ejercicios por sesión.
2. `MDP:66-107`; `PV:555-560`; `RB:127-129`.
3. `max(6 si M < 60, si no 7; ⌈(piso − 240)/330⌉)`: 6 de 30 a 52 min, 7 de 53 a 60, 8 de 61 a 68 y 9 de 69 a 75.
4. Validación y construcción.
5. `docs/ADENDA_4D_v3_1.md` y `docs/ADENDA_4D_v3_4.md` (C-7, C-8).
6. **El techo se deriva del piso:** con el tope se llega justo al piso y con uno menos no. El margen es casi nulo (por ejemplo, 60 s a 75 min), lo que obliga a llenar las sesiones de estructurales y sube el volumen semanal por grupo (36 series con 3 días a 61–75 min).
7. —
8. Más colocaciones aunque el tiempo sobre.
9. Más variedad con dosis menores.
10. `MvpDosePolicyTopeTest`, `Hipertrofia4Dv34IT` (312/312). Adecuación profesional **no demostrada** (adenda §4.2).
11. Coincide. Las constantes 300, 60 y 70 están fijadas en el código y no se leen de la configuración (`MDP:114-118`).

**CONTRADICTORIA_O_DEFECTUOSA** (circular) **+ PENDIENTE_VALIDACION_PROFESIONAL**.

#### V24-GM-PRES · Presupuesto del 85 %
1. Que el día quepa en el tiempo útil.
2. `MDP:63, 379-381`; `PV:642-648`; `RB:430-470` (recorte).
3. `Σ series·(reps·3 + descanso) ≤ 0,85·M·60`; sin DURATION.
4. Validación y construcción.
5. 4D v2 §5.
6. Fórmula distinta de la del estimador (§2.e). En la adenda v3.1 obligó a poner 4 colocaciones a 30 min.
7. —
8. Días caros.
9. —
10. —
11. Coincide; la propia clase lo declara optimista.

**HEURISTICA_PLANIFICACION**.

#### V24-GM-EVID · Evidencia de hipertrofia
1. Solo estructural o resistido de repeticiones.
2. `PV:307-334`; `RB:410-416`.
3. Bajo `capability.enforcement=ENFORCE`.
4. Validación.
5. `docs/ADENDA_4D_v3_1.md` §2.
6. Ni cardio ni movilidad en GAIN_MUSCLE.
7–9. Excluye la movilidad o el calentamiento como colocación.
10. Barrido de 144 perfiles.
11. Coincide. No se aplica en la adaptación P2-D salvo con el interruptor.

**HEURISTICA_PLANIFICACION**.

#### V24-GM-SERIE · Serie adicional (4D v3.2 / v3.3)
1. Llegar a V20 cuando no hay otra vía.
2. `MDP:162-184`; `PV:595-640`; `RB:398-408`.
3. Relleno de zona pequeña o bíceps en PULL: 2 → 3 series, **solo si sin ella no se llega a V20**.
4. Validación y construcción.
5. `docs/ADENDA_4D_v3_2.md` y `docs/ADENDA_4D_v3_3.md` (usuario U06; PULL en casa).
6. Es un parche de V20: la serie solo es válida si hace falta para el tiempo.
7–9. Rechaza la misma serie cuando el tiempo sobra, aunque sea razonable.
10. —
11. Coincide.

**CONTRADICTORIA_O_DEFECTUOSA** (criterio de tiempo, no de entrenamiento).

#### V24-ext · Dosis por capacidad (INCREASE_STRENGTH, LOSE_WEIGHT, GENERAL_FITNESS)
1. Cada colocación acredita una capacidad del objetivo con su dosis **exacta**.
2. `PV:234-293`; `CPP:60, 110-262`.
3.
   - STRENGTH: 3×5 con 150 s; principiante 2×8 con 120 s; solo estructurales.
   - HYPERTROPHY: 3×10 con 90 s; principiante 2 series.
   - MUSCULAR_ENDURANCE: 3×18 con 45 s; principiante 2×15; isométricos 3×40 s con 45 s.
   - CARDIO: 3×120 s, principiante 3×90 s, con 60 s.
   - MOBILITY y BALANCE: 2×30 s.
   - Series ampliables a 3 solo para V20.
   - Repeticiones llevadas a V16.
   - Tope de 8 colocaciones por día.
   - `capability.enforcement=ENFORCE` por defecto; en la adaptación solo con interruptor.
4. Validación y construcción.
5. 4D v4: «criterio de diseño PROVISIONAL», tomado de 4D v2 y de ACSM 2009.
6.
   - Con `min_reps = 6`, STRENGTH queda en **3×6** y no en 3×5 (`CPP:229, 245-249`).
   - En LOSE_WEIGHT un resistido (gemelo, abdominal, plancha resistida) **no tiene ninguna capacidad** y se rechaza.
   - POWER no se prescribe.
7. Solo las dosis tabuladas.
8. Cualquier otra dosis; core y accesorios en LOSE_WEIGHT.
9. Core en LOSE_WEIGHT; fuerza a 4×4; cardio de 20 min continuos; principiantes con 3 series.
10. `CapacidadesPorObjetivoTest` (NO VERIFICADO su contenido). Sin citas específicas por cifra.
11. **Discrepancias:** el javadoc dice «fuerza 3x5» y el efectivo es 3×6. El javadoc de `MAX_COLOC_DIA` dice «el de V20 del motor de reglas».

**PENDIENTE_VALIDACION_PROFESIONAL / POTENCIALMENTE_EXCESIVA**.

#### V25 · Capacidades principales factibles presentes
1. Que una secundaria no desplace a una principal factible.
2. `PV:344-480`; `CapabilityCoveragePolicy.java:198-322`.
3.
   - Principal factible ausente → rechazo.
   - LOSE_WEIGHT: CARDIO en **cada sesión** donde sea factible (decisión del 26/10, plan 55).
   - INCREASE_STRENGTH: los 4 slots con dosis STRENGTH, con 3 por sesión si M < 30 y 4 si no.
   - GENERAL_FITNESS: STRENGTH, ME, CARDIO y MOBILITY.
4. Validación y construcción.
5. 4A v6 (`docs/ADENDA_4A_v6.md`); planes 55 y 58.
6. Con V18 en días seguidos hace difícil el cardio diario en casa (`docs/IMPLEMENTACION_V20.md` §1.3). Con V20, los principiantes de 75 min no se pueden generar.
7. —
8. Semanas sin cardio en LOSE_WEIGHT; INCREASE_STRENGTH con un slot solo de hipertrofia.
9. Día de LOSE_WEIGHT solo de fuerza por preferencia; GENERAL_FITNESS de 1 día con 4 principales en 30 min.
10. Planes 55 y 58.
11. **Discrepancia:** la adenda 4A v6 §3.2 dice «debe cubrirse en la **semana**, no necesariamente en cada sesión» y su estado es «solo observación; no rechaza». El código exige cardio por sesión y rechaza. El documento está desactualizado frente a la decisión del 26/10.

**HEURISTICA_PLANIFICACION / PENDIENTE_VALIDACION_PROFESIONAL**.

#### DIVISION · Sugerencia de división en el mensaje de rechazo
`PV:1553-1560`. Si hay problemas de V13 o V18, se añade el `suggested_split`. **FUNCIONAL** (ayuda al reintento).

### 3.7 Construcción del motor de reglas

#### R-GM · Motor 4D (GAIN_MUSCLE)
1. Generar una semana que pase V1–V25.
2. `RB:105-284, 296-470`.
3.
   - Base de 5 ejercicios (4 si M < 30).
   - Tope = `maxColocaciones(M)`.
   - Presupuesto del 85 %.
   - Preferencia por estructurales.
   - Slots pendientes arrastrados entre días.
   - Recorte por V4 hasta un mínimo de 2.
   - Semilla `userId·1.000.003 + epochDay`.
4. Construcción.
5. 4D v3.1 (adenda §2).
6. Ver §2.f.
7. —
8. Subir series (la dosis es fija).
9. —
10. `Hipertrofia4Dv34IT`.
11. Coincide.

**HEURISTICA_PLANIFICACION**.

#### R-CAP · CapabilityWeekBuilder (INCREASE_STRENGTH, LOSE_WEIGHT, GENERAL_FITNESS)
1. Principales primero, después secundarias hasta V20, recorte por V4 y repaso de V15.
2. `CWB:134-370, 519-568, 752-826, 844-1128`.
3.
   - Resistidas principales: IS 4 (3 si M < 30); GF 3 (2 si M < 30); LW 0.
   - Cardio: 2 bloques en LW y 1 en GF.
   - Movilidad: 1 en GF.
   - Equilibrio: oportunista, 1 por semana.
   - ≤ 8 colocaciones; series ≤ 3.
4. Construcción.
5. 4A v6 + 4D v4; investigación 30–120 (CASO A).
6. El piso se calcula con `minimumSessionMinutes`, es decir, 0 con orden de volumen: distinto de `RB:172` (latente).
7. —
8. Inflar descansos.
9. Ver §2.b (no llega en principiantes de 75 min).
10. `V15CambioDeGrupoIT`, `FactibilidadPorObjetivoIT`.
11. Coincide.

**HEURISTICA_PLANIFICACION**.

#### R-SEL · Topes del selector
`ExerciseSelector.java:29-31, 62-70`: `lower legs` 1, `waist` 2 y `upper arms` 2 por sesión si el enfoque tiene un grupo grande; los grupos grandes pesan el doble; se relajan como último recurso. Es una heurística sin referencia. **HEURISTICA_PLANIFICACION**.

#### R-TP · TargetPrescription
`planning/domain/model/valueobjects/TargetPrescription.java`:
- LOSE_WEIGHT 3×15 con 45 s.
- GAIN_MUSCLE 4×10 con 75 s (sin uso: 4D manda).
- INCREASE_STRENGTH 4×6 con 120 s.
- Resto 3×12 con 60 s.
- Principiante, como mucho 2 series.

Solo la usan el normalizador (`repsFor`) y la rama heredada. **HEURISTICA_PLANIFICACION**.

#### R-VR · VolumeReducer
`VolumeReducer.java`: series hasta 2, reps hasta el mínimo de zona y ejercicios hasta 2; REDUCE_DURATION con piso de 20 min. **Inactivo** (sin orden porcentual). **FUNCIONAL** (inactivo).

### 3.8 IA: intentos, cortacircuitos, prompt y contrato

#### A-INT · Intentos y respaldo
1. Agotar la IA y después recurrir a la vía determinista.
2. Generación: `PlanGenerationPipeline.java:42, 93-174` (3 intentos con los motivos acumulados y la propuesta rechazada → motor de reglas → excepción). Adaptación: `AdaptedWeekCommandService.java:68, 400-460` (3 intentos → productor sin propuesta → continuidad o NO_FACTIBLE).
3. 3 intentos; `ai_calls` entre 0 y 4 (`ck_paa_ai_calls`, V32).
4. Generación y adaptación.
5. 19.4 («3 desde el plan 36»).
6. —
7–9. —
10. Pruebas `CorreccionesReplicate*IT`.
11. El javadoc de `RB:13-16` dice «cuando la IA falla dos veces»: está desactualizado.

**FUNCIONAL_NECESARIA**.

#### A-CB · Cortacircuitos
`AirCircuitBreaker.java:41-44`: tras 3 fallos seguidos del proveedor, 10 minutos sin IA (respaldo por reglas). Los rechazos de validación no cuentan. **FUNCIONAL_NECESARIA**.

#### A-PROMPT · Versiones de prompt y contrato
1. Fijar lo que la IA sabe y devuelve.
2. `PlanPromptBuilder.java:93` (PR-1.7); `PrescriptionPrinciples.java:44` (P-1.3); `SessionFixPromptBuilder.java:57-105` (FIX-PR-1.3, v2 / v2.1 experimentales, `fitsense.ai.adaptation.prompt-version=v1`); `SessionFixJsonSchema.java:45` (FIX-1.0); `PlanInputSnapshot` (GEN-IN-1.13).
3. Esquema estricto sin `minItems` ni `maxItems`; los límites viajan como texto.
4. IA.
5. P-1.3 (16/10): «no duplicar las reglas de 4D en el prompt».
6. El principio 3 del prompt (GAIN_MUSCLE «unas 10 series por grupo a la semana») **contradice** el volumen que fuerza 4D v3.4 (hasta 36). El principio 8 (no subir con RPE ≥ 8) no se valida.
7–9. —
10. `ContratoV4CIATest`, `docs/PRUEBAS_REPLICATE_REAL.md`.
11. **Discrepancia:** «10 series por grupo» frente a lo que genera el motor.

**FUNCIONAL**.

#### A-FPI · Factibilidad previa a la IA
1. No llamar a la IA cuando es imposible.
2. `FactibilidadPreviaIA.java:20-60`.
3. ACORTAR con M: cota inferior con una sola operación (retirar 1, de relleno en GAIN_MUSCLE, o bajar la dosis a 2 series / `min_reps` / 20 s; GAIN_MUSCLE no reduce). REPROGRAMAR: sin fecha libre.
4. Adaptación.
5. Orden de optimización del 31/10 (U01 S2, U04 S5).
6. Usa los mismos pisos que V8C y V24.
7. Llamar a la IA ante la duda.
8. Llamadas inútiles.
9. —
10. `FactibilidadPreviaIATest`.
11. Coincide.

**FUNCIONAL_NECESARIA**.

### 3.9 Adaptación: decisión, señales y permisos

#### D-DEC · AdaptationOrderDecider
1. Decidir si se adapta, con qué alcance y con qué permisos.
2. `AOD:52-162`; `EvidenceRule.java`.
3.
   - Cobertura de registro ≥ `fitsense.adaptation.min-registration-coverage-pct=50.0` (por defecto en `@Value`; no está en `calculation_configs`).
   - Observada ≥ 80 → NO_CHANGE.
   - < 50 → ADAPT_EXPANDED (todas las señaladas).
   - 50–80 → ADAPT_LOCALIZED (la peor).
   - Dolor con adherencia buena o datos insuficientes → localizada solo por seguridad.
   - Señaladas vacías → INSUFFICIENT_DATA.
   - Sin progresión automática.
4. Cierre semanal.
5. Javadoc; `docs/POLITICA_ADAPTACION_Y_PROGRESION.md` §1. Los umbrales de V7 están «sin calibrar».
6. Sustituye a la tabla porcentual (T-PORC). Usa la adherencia **observada**, no la ponderada.
7. Una modificación funcional por ciclo.
8. Adaptar varias cosas a la vez; progresar sin el resolutor.
9. Adherencia del 79 % con una sola sesión mala de 5 → solo se corrige esa; con cobertura del 49 % no se adapta nada.
10. `AdaptationOrderDeciderTest`. Sin calibración.
11. El 50 % de cobertura es «PROVISIONAL, pendiente de aprobación» (javadoc).

**PENDIENTE_VALIDACION_PROFESIONAL**.

#### D-SEÑ · Identificación de sesiones señaladas
1. Marcar las sesiones con incumplimiento registrado.
2. `analytics/domain/services/SessionOutcomeIdentifier.java:56-104`.
3.
   - Señalada si hay registro y cumplimiento < `session_completed_threshold_pct` (80).
   - Sin registro → no se señala.
   - Si algún ejercicio se dejó por dolor → causa PAIN_OR_DISCOMFORT.
   - El RPE no entra.
4. Analítica.
5. Javadoc; `docs/POLITICA_ADAPTACION_Y_PROGRESION.md` §2.1.
6. Alimenta D-DEC.
7–9. Una sesión completada con RPE 9 no se adapta (FATIGUE solo existe si se declara).
10. `SessionOutcomeIdentifierTest`.
11. Coincide.

**HEURISTICA_PLANIFICACION**.

#### D-TIPO · Permisos por intervención (TipoIntervencion)
1. Qué puede cambiar cada causa.
2. `shared/domain/model/valueobjects/TipoIntervencion.java`.
3.

| Intervención | Sustituir | Cambiar dosis | Otros permisos |
|---|---|---|---|
| REPROGRAMAR_DIAS | No | No | solo reprogramar |
| ACORTAR_O_REORGANIZAR | No | Sí | reducir duración y quitar 1 colocación |
| VARIANTE_MAS_SENCILLA_O_CARGA | Sí | Sí | reducir carga |
| SEGURIDAD_ANTE_DOLOR | Sí | Sí | revisión de seguridad |
| REVISAR_CARGA_Y_RECUPERACION | No | Sí | reducir carga |
| INVESTIGAR_CAMBIO_MINIMO | No | Sí | — |
| CAUSA_NO_REGISTRADA / CONSERVAR | No | No | no se envían a la IA |

4. Adaptación.
5. `docs/POLITICA_ADAPTACION_Y_PROGRESION.md` §2.
6. Lo juzga V8C. En GAIN_MUSCLE, «cambiar dosis» es letra muerta por 4D (§2.c).
7–8. Según la tabla.
9. FATIGA no permite quitar un ejercicio; MOTIVACIÓN tampoco.
10. —
11. Coincide.

**FUNCIONAL_NECESARIA**.

#### D-FT · FaltaDeTiempoPolicy
`FaltaDeTiempoPolicy.java`. Solo se acorta una omisión por LACK_OF_TIME con «menos tiempo habitual» y M < duración de la sesión. Sin respuesta, por imprevisto o con M ≥ sesión → CONSERVAR. Se apoya en `docs/DISENO_ADAPTACION_CONTEXTUAL.md` v0.3 y el caso A01. **FUNCIONAL_NECESARIA**.

### 3.10 V8C · Conservación

#### V8C (núcleo)
1. Cambiar la parte señalada y solo esa.
2. `CV:185-560, 857-900, 1360-1400`.
3. En las no señaladas no cambia nada: ejercicios, dosis, enfoque, minutos ni día (`CV:870-894`). Partición completa de las sesiones (`CV:1367-1396`).
4. Adaptación.
5. Sustituye a V8; «convierte el sistema adapta en algo demostrable».
6. Integra SUST-SEG-1, D8, la progresión y las dosis autorizadas para V24.
7. Copia y corrección localizada.
8. Cambios fuera de lo señalado.
9. Un problema detectado en una sesión no señalada (por ejemplo, un heredado no elegible por equipamiento) no se puede arreglar → V1 → NO_FACTIBLE.
10. `ConservationValidatorTest` y otras.
11. Coincide.

**FUNCIONAL_NECESARIA**.

#### V8C-IA-DOSIS · La IA nunca sube la dosis
`CV:649-680` (corrección 1, 27/10: «la IA real publicó 16 subidas»). Solo progresa el backend. **HEURISTICA_PLANIFICACION** (razonable, respaldada por un incidente).

#### V8C-RETIRADA · ACORTAR retira 1 sola colocación de relleno sin tocar el resto
1. Acortar quitando trabajo mínimo.
2. `CV:1100-1180`.
3. Como mucho 1 retirada; solo relleno (sin slot 4C); sin cambiar la dosis de las que quedan; nada añadido.
4. Adaptación.
5. INC-P2D-01.
6. Con estructurales solamente (GAIN_MUSCLE a 60 min: 7 estructurales y ningún relleno, adenda v3.1 §6), ACORTAR no tiene operación legal.
7. Retirar un relleno.
8. Retirar un estructural, retirar dos, o retirar uno y bajar la dosis de otro.
9. Persona con 20 min de 45: no cabe con una sola retirada.
10. `ConservationValidatorQuitarRellenoTest`.
11. Coincide.

**POTENCIALMENTE_EXCESIVA**.

#### V8C-DOLOR · Reglas ante dolor
`CV:1040-1090, 1200-1218`: lo que dolió no reaparece, añadidos ≤ retirados y el volumen de la sesión no sube (corrección 5, 27/10). Lo complementan P-03 y `masTrabajoQueLaMedida` (`AWP:94-118`). **SEGURIDAD_INDISPENSABLE**.

#### V8C-PATRÓN · Mismo patrón (H1, multiconjunto)
`CV:66-89, 1240-1330`; `fitsense.adaptation.v8c.pattern-rule=multiconjunto`. Cada añadido se empareja con un retirado distinto del mismo patrón. No se exige ante dolor. Objetivos secundarios: «no verificable». **HEURISTICA_PLANIFICACION**.

#### V8C-TOPE-M · Sesión corregida ≤ M declarados
`CV:513-525` (DISENO_ADAPTACION_CONTEXTUAL v0.3 §2). **FUNCIONAL_NECESARIA**.

#### V8C-DIF · Guarda de dificultad (candidata, apagada)
1. Que el sustituto no sea más difícil que el original.
2. `CV:97-124, 1216, 1256`; `fitsense.adaptation.difficulty-guard=off`.
3. off / on / preferencia.
4. Adaptación.
5. Pendiente de revisión.
6. `LOWER_DIFFICULTY` ya no se emite, así que con la guarda apagada **nada impide que una «variante más sencilla» sea más difícil** (solo V6, el techo del nivel).
7. —
8. —
9. —
10. —
11. **Contradicción** con el texto de la intervención («variante más sencilla del mismo patrón»).

**CONTRADICTORIA_O_DEFECTUOSA** (laguna con el valor por defecto).

#### D8 · Exención de V20 por reducción autorizada
1. Que una sesión acortada no quede NO_FACTIBLE la semana siguiente.
2. `ReduccionAutorizadaResolver.java:13-50`; `CV:470-493, 533-555`.
3. Solo ACORTAR aplicado y su linaje de copias idénticas (C-2 por identidad). Nunca dolor.
4. Adaptación.
5. `docs/VERIFICACION_DISENO_V03.md`; C-2 (31/10).
6. **No existe una exención equivalente para FATIGA o MOTIVACIÓN.** La reducción por fatiga (si se pudiera) tendría que mantenerse ≥ 70 %.
7. Sesiones cortas heredadas de ACORTAR.
8. Cualquier otra sesión corta.
9. Semana de descarga tras fatiga.
10. `HerenciaReduccionV20Test`, `HerenciaD8IT`.
11. Coincide.

**CONTRADICTORIA_O_DEFECTUOSA** (la asimetría entre causas no está justificada en el documento).

#### D-ESTR · Validación estricta de la adaptación
1. No publicar dosis incompatibles con 4D.
2. `StrictValidationAdapter.java:55-71`; `AWP:183-198`.
3. 4D siempre ENFORCE. Capacidades (V24-ext y V25) solo con `capability-validation.enabled` o `progression.enabled` (ambos `false`). Cobertura según la propiedad, con valor por defecto OBSERVE.
4. Adaptación.
5. Javadoc: «publicar una dosis que 4D rechaza sería peor que no adaptar».
6. Ver §2.c.
7. —
8. Reducciones en GAIN_MUSCLE.
9. Fatiga en GAIN_MUSCLE.
10. `AdaptacionCompletaIT`.
11. **Discrepancia:** el javadoc dice que `fitsense.dose.enforcement` está «hoy OBSERVE»; en `application.properties` el valor por defecto es `ENFORCE`. Además, en los objetivos por capacidades la adaptación **no** valida V24-ext ni V25 por defecto, mientras la generación sí: dos varas distintas.

**CONTRADICTORIA_O_DEFECTUOSA**.

#### D-SUST · SUST-SEG-1
`SafetySubstitutionPolicy.java:9-46` (mismo slot, tipo, complemento, zona, V22 y V18; fuera de revisión; dosis recalculada; carga null) y `SafetyConflictPolicy.java` (causas cerradas; equipamiento y bloqueo **no** son causas). **SEGURIDAD_INDISPENSABLE**.

#### D-RESP · Respaldo con dolor
`AWP:328-378`. Sin respuesta de la IA:
- si la memoria P-02 cubre lo que dolió → SUST-SEG-1, y además el trabajo no puede subir;
- si no → NO_FACTIBLE (`DOLOR_SIN_RESOLUCION_SEGURA`).

Sin dolor → copia. **SEGURIDAD_INDISPENSABLE**.

#### D-RET · Retirada localizada por dolor (candidata)
`AWP:395-480`; `fitsense.adaptation.pain-localized-removal=off`. Solo actúa con la guarda de dificultad encendida (decisión del cierre del PR #39). Contradice el §3 de `docs/POLITICA_ADAPTACION_Y_PROGRESION.md` («No se publica una sesión retirando solo lo que dolió»), por eso está apagada. **PENDIENTE_VALIDACION_PROFESIONAL**.

#### P-03 · Regeneración segura determinista
1. Alternativa tras un NO_FACTIBLE por dolor.
2. `SafeRegenerationService.java:81-189`; `PlanGenerationPipeline.runDeterministic` (`:178`).
3. Motor de reglas con V1–V25 completos y `PlanAdjustment.none()`; sin excluidos; las repeticiones **y** los segundos totales de la semana no pueden superar los de la semana medida.
4. Adaptación.
5. P-03 (31/10); auditoría del nivel 2 («102 → 108 repeticiones»).
6. La regeneración vuelve a las dosis base y a V20 completo; si la semana medida estaba reducida (ACORTAR), puede no existir una semana que cumpla a la vez V20 y «no más trabajo» → NO_FACTIBLE. `porDolor` decide por la intervención **o** porque «dolor» aparezca en el texto del motivo de rechazo (heurística textual).
7. —
8. Más trabajo.
9. Semana medida corta por ACORTAR y después dolor.
10. Pruebas de P-03 (NO VERIFICADO el detalle).
11. Coincide.

**SEGURIDAD_INDISPENSABLE** (con riesgo de bloqueo excesivo).

### 3.11 Progresión y reincorporación (apagadas)

#### PR-UMB · Criterios de progresión
1. Progresar con desempeño sostenido.
2. `ProgresionResolver.java:24-69, 74, 114, 195-355`; `fitsense.adaptation.progression.enabled=false`.
3.
   - Ordinaria: 100 % real dos semanas seguidas (adherencia, cobertura, sesiones y ejercicios al 100 %).
   - La transición anterior de la sesión es una copia NO_CHANGE.
   - Sin fatiga ni dolor.
   - Paso ≤ `progression_increase_pct` (5).
   - Techo `floor(base·1,10)`.
   - Un solo paso por ciclo.
   - Recuperación con `recovery_step_pct` (10) y el umbral del 80 %.
   - Vuelta atrás si hay fatiga tras un paso.
4. Continuidad.
5. `docs/POLITICA_ADAPTACION_Y_PROGRESION.md` §1; `docs/DISENO_PROGRESION_Y_REINCORPORACION.md` (D-P1…D-P12); ACSM «2 por 2» (migración V15).
6. Revalida con V8C y V1–V25; V24-ext se activa con la progresión.
7. Un paso pequeño.
8. Progresar con 97 % o con RPE desconocido.
9. Persona que completa el 95 % durante meses: nunca progresa.
10. `ProgresionCienPorCienIT` y otras.
11. **Discrepancias:**
    - Techo de 1,10 fijado en código frente a `max_cumulative_volume_increase_pct = 25` en la configuración. El diseño (§3.3 y tabla de configuración, línea 225) pedía «un parámetro nuevo… en una versión de configuración nueva».
    - El umbral del 100 % también está en código (declarado como decisión).

**POTENCIALMENTE_EXCESIVA** (apagada).

#### PR-RANGO · Rangos de dosis autorizada
`ProgresionDosisPolicy.java:31-81`:
- Progresión: +2 reps (`REPS_EXTRA`) y series ≤ 3; en GAIN_MUSCLE, las estructurales conservan sus series; descanso igual.
- Reducción: solo por capacidades, ≥ `min_reps` y ≥ 20 s.
- **GAIN_MUSCLE sin reducciones.**

D-P3 está «pendiente de aprobar o ajustar». **PENDIENTE_VALIDACION_PROFESIONAL**.

#### PR-REINC · Reincorporación
`ProgresionResolver.java:248-280`. El relleno retirado por ACORTAR vuelve con su dosis inicial exacta solo si: ha habido ≥ 1 paso de progresión después, los conservados ya no pueden progresar dentro de la reserva R2 (1,10·V_base − V_ret) y cabe en la M vigente. Decisión 1(a) del 27/10. **HEURISTICA_PLANIFICACION**.

### 3.12 Adherencia, señales y ejecución

#### M-ADH · Cálculo de adherencia
1. Medir la ejecución frente a lo planificado.
2. `analytics/domain/services/WeeklyMetricsCalculator.java:28-140`.
3.
   - Oficial (`weighted_adherence_pct`) = media simple del cumplimiento por sesión prevista; sin registro cuenta 0.
   - Observada = media sobre lo registrado.
   - Cobertura = registradas / previstas.
   - Válida ≥ 30 %, completada ≥ 80 %, tope 100 %.
4. Analítica.
5. `docs/ADHERENCIA.md` («sin ponderaciones»; el nombre «weighted» es heredado).
6. El decisor usa la observada.
7–9. Sin plan → NULL.
10. `RegistrationCoverageTest`.
11. **Discrepancia documentada:** el nombre de la columna y las migraciones V13/V14 hablan de ponderar por duración, cosa que ya no se hace.

**FUNCIONAL_NECESARIA**.

#### M-RIESGO · Puntuación de riesgo
`WeeklyMetricsCalculator.java:141-190` y V7:
- Adherencia: < 80 → 10, < 60 → 20, < 40 → 30 puntos.
- Saltos consecutivos al final: 1 → 5, 2 → 15, 3 → 25.
- Caída de adherencia: ≥ 15 pp → 5, ≥ 25 pp → 15.
- RPE ≥ 8 → +10.
- Niveles: 25 / 50 / 70.
- Abandono: ≥ 14 días sin entrenar.

«Sin calibrar». No decide la adaptación. La causa dominante, `dominant()` (`:275-282`), resuelve los empates por el orden del `HashMap` (no determinista); se mitiga para el dolor con D-SEÑ. **HEURISTICA_PLANIFICACION**.

#### M-REP · Ventanas de reporte
Reporte retroactivo ≤ 7 días (`retroactive_report_days`, V16; `WorkoutSessionCommandServiceImpl.java:149`); sesión abandonada a las 6 h (`StaleSessionScheduler.java:28`). **FUNCIONAL_NECESARIA**.

### 3.13 Código latente y persistencia

#### T-PORC · Tabla de decisión porcentual (fuera de flujo)
1. Ajuste por tramos de adherencia (18.2–18.4).
2. `adaptation/domain/services/AdjustmentDecisionTable.java`; no se inyecta (`UserInterventionCommandServiceImpl.java` ~l.155-170).
3.
   - ≥ 80 → +5 % (con 2 semanas y RPE no nulo).
   - 50–80 → −20 %.
   - < 50 → −30 % y −1 día.
   - FATIGUE −10 % extra.
   - Piso −40 % y techo +25 % sobre la base.
   - Mínimo 2 días y 20 min.
   - Carga −15 % / −20 %.
4. Fuera de flujo.
5. Se conserva como comparador de la tesis.
6. Activa V8, V19, VolumeReducer y la exención de V20.
7–9. —
10. —
11. **Defectos latentes:**
    - Con 1 día de perfil, REDUCE_DAYS da `max(0, 2) = 2` días, es decir, **sube** días, y V2 queda imposible si solo hay 1 día disponible.
    - Mínimo de 20 min frente a P-01 (30).
    - V4 sin los minutos efectivos.

**CONTRADICTORIA_O_DEFECTUOSA** (latente).

#### BD · Restricciones CHECK
`V4__planning.sql:78, 121-123`: `expected_duration_minutes` entre 10 y 180; series ≥ 2, reps ≥ 6, ≥ 20 s. `V1__users_and_profiles.sql:77-88` (`session_minutes` 15–180, más amplio que P-01). `V32__adaptation_evidence.sql:249` (`ai_calls` 0–4). V33 (APLICADA exige sesiones). V9 replica los mínimos de la BD para que el error llegue al reintento y no al guardar. **FUNCIONAL_NECESARIA**.

---

## 4. Discrepancias de documentación y configuración (lista corta)

1. `application.properties`, dosis: el comentario dice «NO activar ENFORCE…» y el valor por defecto es `ENFORCE`.
2. `application.properties`, cobertura: el comentario dice «Se entrega en OBSERVE» y el valor por defecto es `ENFORCE_AI_ONLY`. `StrictValidationAdapter` lee la misma propiedad con valor por defecto `OBSERVE`.
3. El javadoc de `StrictValidationAdapter` dice «fitsense.dose.enforcement, hoy OBSERVE».
4. `docs/ADENDA_4D_v3_1.md` §4 promete un `NO_FACTIBLE:` en la generación inicial; el código lanza «No se pudo generar un plan valido…».
5. `docs/ADENDA_4A_v6.md`: «principal en la semana, no en cada sesión» y «solo observación», frente a V25 con cardio por sesión que rechaza.
6. `CPP` dice «fuerza 3x5»; el efectivo es 3×6 por V16/V9.
7. El comentario de V15 dice «tope en 3»; el código usa 2.
8. `GoalFeasibilityPolicy.exigeSlotsResistidos` dice «V23 los exige a TODOS»; en realidad excluye a LOSE_WEIGHT.
9. `RB` dice «cuando la IA falla dos veces»; son 3 intentos.
10. `UserProfile.update` dice «se aplica el lunes siguiente»; el perfil está bloqueado.
11. `PlanLimitationsService` dice «añádelo en tu perfil»; el perfil está bloqueado.
12. Principio 3 del prompt («~10 series por grupo a la semana») frente a hasta 36 series de 4D v3.4.
13. `ProgresionDosisPolicy` dice «no por debajo de una serie»; V9 exige 2.
14. Techo de progresión: 1,10 en código frente a 25 en la configuración; el diseño pedía un parámetro versionado.
15. `MvpDosePolicy` deriva el tope con 300 s, 60 s y 70 % fijados en código, no con la configuración activa.
16. `UserProfile.maxSessionMinutesAllowed` usa `floor` y `PlanningProfile.maxSessionMinutes` usa `round`.

## 5. Límites de esta auditoría

- No se ejecutaron pruebas. Las afirmaciones «la prueba X existe» se basan en el nombre del archivo, no en su resultado.
- Los valores desplegados (variables de entorno en Railway y configuración activa en Neon) **no están verificados**. La configuración activa esperada por las migraciones es MVP-1.8 (V30, mismos parámetros que MVP-1.7).
- Los recuentos de perfiles sin plan proceden de los documentos y CSV de evidencia, no de una ejecución propia.
- Las combinaciones de edad, permisos declarados y equipamiento mínimo no las cubre la matriz documentada (NO VERIFICADO).
