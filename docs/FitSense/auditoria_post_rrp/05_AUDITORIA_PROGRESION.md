# 05 · Auditoría de progresión, reincorporación, V24 y autorizaciones de dosis

> **Estado:** auditoría integrada en la auditoría post R/R′ (05/11/2026).
>
> **Cómo se hizo:**
> - El análisis de código (§0–§15) se hizo **en solo lectura** y cita cada afirmación con su archivo:línea.
> - La ejecución de las pruebas de progresión que ya existen forma parte de la regresión completa de la fase 8. Sus resultados están en el documento 07 §2.
> - Los defectos marcados NO_VERIFICADO (H6, H8 y H14) **no se han reproducido en ejecución**. Se presentan como riesgos deducidos del código, no como hechos.
>
> **Respuestas cortas a la orden:**
>
> | Pregunta | Respuesta |
> |---|---|
> | ¿Hacen falta 2 semanas con ≥ 80 %? | Solo en la recuperación y la reincorporación. La progresión ordinaria exige **100 % real** durante dos semanas. Es una decisión documentada (POLÍTICA §1), pero el diseño y la implementación V34 aún dicen 80 % (discrepancia documental P1). |
> | ¿Pasos del 10 % y del 5 %? | ≤ 10 % en la recuperación y ≤ 5 % en la progresión. Ningún paso se fuerza. |
> | ¿Techo del 110 %? | Se cumple: 1,10 × el volumen de la generación inicial, por raíz V31. Es una constante del código; la configuración dice 25 % y no se usa. |
> | ¿Linaje? | Se valida en `ProgresionResolver.linaje`, `DosisLinajeResolver` y `ReduccionAutorizadaResolver`, y además en la base de datos con `ck_paa_un_paso`. Riesgo H6: una versión reparada por seguridad podría romper el linaje (no verificado). |
> | ¿Interruptores? | La progresión está apagada por omisión y K20 la fija a `false`. **K20 no aporta ninguna evidencia sobre la progresión.** Riesgo H4: al apagar la progresión tras un paso en GAIN_MUSCLE, la copia siguiente la rechaza V24. |
> | ¿V24 en la generación inicial (cuatro objetivos)? | **No cambia.** La generación valida con un informe vacío, sin autorizaciones (`PlanDraftValidator.java:128-130`). AUD-G1 no toca V24. `GeneracionReferenciaIT` da 72/72 idénticos y D/D′ coinciden con las huellas congeladas (documento 07). |


- **Alcance:** solo lectura sobre `/home/user/wt_aud`. No se ejecutó Maven, ni pruebas, ni Docker, ni se modificó nada.
- **Fecha:** 05/11/2026.
- **Fuentes de diseño:**
  - `docs/FitSense/DISENO_PROGRESION_Y_REINCORPORACION.md` (v0.2.1);
  - `docs/FitSense/IMPLEMENTACION_PROGRESION_V34.md`;
  - `docs/FitSense/POLITICA_ADAPTACION_Y_PROGRESION.md` (28/10);
  - `docs/FitSense/ADENDA_4D_v3_2.md` y `ADENDA_4D_v3_3.md`.
- **Abreviaturas de rutas:**
  - `PR` = `src/main/java/main/web/services/fitsense/planning/application/internal/commandservices/ProgresionResolver.java`
  - `CV` = `.../planning/domain/services/ConservationValidator.java`
  - `PDV` = `.../planning/domain/services/PlanDraftValidator.java`
  - `AWCS` = `.../planning/application/internal/commandservices/AdaptedWeekCommandService.java`
  - `AWP` = `.../planning/domain/services/AdaptedWeekProducer.java`
  - `DLR` = `.../planning/application/internal/commandservices/DosisLinajeResolver.java`
  - `RAR` = `.../planning/application/internal/commandservices/ReduccionAutorizadaResolver.java`
  - `PDP` = `.../planning/domain/services/ProgresionDosisPolicy.java`
  - `CR` = `.../planning/domain/model/valueobjects/ConservationReport.java`
  - `AEM` = `.../planning/domain/services/AdaptationEvidenceMapper.java`
  - `PPSR` = `.../planning/application/internal/commandservices/PublishedPlanSafetyRepairService.java`
  - `IT/*` = `src/test/java/main/web/services/fitsense/orchestation/*`

**Clasificación usada:**
- COMPROBADO_EN_CODIGO: implementado y cubierto por una prueba que lo afirma.
- IMPLEMENTADO_SIN_PRUEBA: implementado, sin prueba dirigida.
- DISCREPANCIA: el código y el diseño o la documentación no coinciden, o hay una contradicción lógica.
- NO_IMPLEMENTADO.
- NO_VERIFICADO: no se pudo confirmar sin ejecutar, o queda como riesgo deducido del código.

---

## 0. Mapa del flujo (código real)

1. **Ciclo semanal** (`orchestration/application/internal/WeeklyCycleService.java:104-183`): cierra la semana, mide, decide y publica.
   - Si la orden de esta medición ya produjo un plan, lo devuelve sin adaptar otra vez (`:130-135`).
2. **`AWCS.adaptar`** (`:162-197`):
   - sin orden de adaptar → `continuidad(...)`;
   - solo con `progresionHabilitada && decision == NO_CHANGE` se piden candidatos a `PR.candidatos` (`AWCS:275-277`).
   - Los candidatos se revalidan de uno en uno con `producer.produce(..., paso)` y se publica **el primero válido** (`AWCS:282-298`).
   - Si ninguno es válido, se publica la copia, con los motivos en `pasos_bloqueados` (`AWCS:303-308`).
3. **`PR`** propone pasos por sesión: vuelta atrás, recuperación, progresión, reincorporación. Además:
   - recorre el linaje V31 + V32 (`PR:272-290`);
   - aplica los criterios (`PR:292-351`).
4. **`CV.revisar`** (V8C) valida el paso estrecho (`CV:450-478`, `pasoEstrecho` `CV:583-617`). Emite las `DosisAutorizada` (PASO, HEREDADA, REDUCCION) para V24 (`CV:465-473`, `494-498`, `649-674`).
5. **`PDV`** (V24):
   - acepta una dosis distinta de la exacta solo si `CR.autorizacionDe(fecha, ejercicio)` la autoriza y cae en el rango de `PDP` (`PDV:265-277` capacidades, `PDV:594-610` 4D);
   - la generación usa `validate(draft, ctx, divisor)`, que llama con `ConservationReport.vacio()` (`PDV:128-130`).
6. **`AEM`** comprueba que lo publicado es exactamente `paso.contenido()` antes de escribir V32 (`AEM:102-111`). Escribe `progressed_sessions` (`AEM:135`).

---

## 1. Condición de dos semanas con adherencia ≥ 80 %

**Clasificación: DISCREPANCIA (documental, deliberada) + COMPROBADO_EN_CODIGO.**

### Qué semanas se miran

Se miran **dos semanas consecutivas**:
- la semana **medida**, de la sesión S;
- la semana **anterior**, de su origen O por V31.

En el código (`PR:292-316`):
- O→S tiene que ser una **copia** (`Clase.CONSERVADA`, sin sustitución por seguridad), y la decisión que publicó S tiene que ser `NO_CHANGE` (`PR:296-298`). Por eso «las dos semanas cuentan desde el último paso»: tras un paso, O→S es `PROGRESADA` y `cumple` devuelve false.
- No hay ningún contador `progression_required_weeks`: el «2» es estructural (S y O).

### Cómo se mide

- **Adherencia semanal OBSERVADA** de las dos semanas (`PR:300-301`):
  - la de la semana medida: `medida.observedAdherencePct()`, pasada desde `AWCS:276`;
  - la de la semana de O: la `observed_adherence_pct` del intento V32 que publicó S.
- **Sesión completada:**
  - S: `COMPLETED` y registrada (`PR:303`);
  - O: `COMPLETED` (`PR:310-312`).
  - `COMPLETED` significa ≥ 80 % de cumplimiento de la sesión: `execution/domain/model/valueobjects/CompletionThresholds.java:16` («>= 80 COMPLETED, 30 a 79.99 PARTIAL, < 30 SKIPPED»).

### Umbral exacto

| Paso | Umbral | Dónde |
|---|---|---|
| **Recuperación, reincorporación** (y elegibilidad general en `cumple`) | `good_threshold_pct` = **80** (V7), leído en cada ciclo; si falta, 80 | `PR:133-138`, `PR:300-301`; `>=` en `PR:353-355` |
| **Progresión ordinaria** | **100 % «real»** en las dos semanas: adherencia observada 100; cobertura de registro 100 en la semana de O; todas las sesiones registradas y COMPLETED con 100 %; todos los ejercicios COMPLETED al 100 % sin `skipReason`; una semana sin sesiones no cuenta | `PR:114` (`UMBRAL_PROGRESION = 100.0`, en el código, no en la configuración); `PR:226-229`; `dosSemanasPerfectas` `PR:325-351` |

### Discrepancias con el diseño

- **Umbral del 100 %:**
  - `DISENO_PROGRESION…` §0 (línea 24), §3.1 (filas 2-3), §12.1 y §12.7 (D-P1 «Aprobada: 80 %») e `IMPLEMENTACION_PROGRESION_V34.md` §5.2 dicen **80 %**;
  - el código exige **100 %** para la progresión ordinaria;
  - el cambio está decidido y documentado en `POLITICA_ADAPTACION_Y_PROGRESION.md` §1 (27-28/10), pero **el diseño y el documento de implementación no se actualizaron**.
  - La afirmación del encargo («adherencia ≥ 80 %») **solo es cierta para la recuperación y la reincorporación**.
- **`progression_required_weeks` (V15 = 2) no se lee en ningún sitio de la progresión V34.** Solo lo usa la tabla antigua `AdjustmentDecisionTable:197`, que está fuera del flujo.
  - Además, `CalculationParams.java:88` tiene como valor por omisión **1** (`requiredWeeksOrDefault`).
  - Cambiar la configuración no cambia el comportamiento. El diseño §2 decía que se usaría.

### Pruebas que lo cubren

- `IT/ProgresionCienPorCienIT`:
  - `dosSemanasPerfectasProgresa`: una semana al 100 % no progresa; dos sí; `ai_calls = 0`;
  - `semanaParcialBloqueaDosSemanas`: con 97,22 % no progresa y hacen falta dos semanas nuevas al 100 %;
  - `registroIncompletoNoProgresa`;
  - `bajoOchentaAdaptaSinProgresar`.
- `IT/ProgresionIT.progresionOrdinaria`: la semana 2 (una sola semana cumplida) no progresa (`:151`).

**Hueco:** ninguna prueba demuestra que la **recuperación** se da con el 80-99 % (umbral distinto del de la progresión).

---

## 2. Fatiga y dolor

**Clasificación: COMPROBADO_EN_CODIGO** (salvo las dos lagunas de §15).

### Bloqueo del paso (`PR:304-314`)

- Fatiga o dolor declarados en S en la semana medida: la sesión o cualquiera de sus ejercicios (`declara`, `PR:358-363`).
- Dolor en **otro uso** de cualquiera de sus ejercicios (`dolio`, `PR:365-368`).
- Ejercicio excluido por seguridad (P-02, `exclusiones.incluye`) o que ya no es elegible (`PR:305-308`).
- Fatiga o dolor en los ejercicios de O (`PR:313-314`).

### Vuelta atrás (D-P6)

- Si O→S fue un paso y consta **fatiga sin dolor**, se propone `VUELTA_ATRAS` a la dosis anterior (`PR:201-206`, `vueltaAtras` `PR:490-514`).
- Con dolor no hay vuelta atrás: manda la seguridad (V8C no republica lo que dolió; `CV:437-444`).
- Solo ocurre en un ciclo `NO_CHANGE`. Con fatiga y menos del 80 % hay orden de adaptar y decide la IA, que nunca puede subir (`CV:649-674`).

### Retirado que dolió

Un retirado que dolió o quedó excluido pasa a `DESCARTADA` (`PR:231-237`).

### Pruebas

- `IT/ProgresionIT`:
  - `fatigaTrasUnPaso`: «la dosis vuelve atrás o la IA la reduce; nunca sube»;
  - `dolorTrasUnPaso`;
  - `p02ProgresionSinElExcluido`;
  - `p02ReincorporacionDescartadaPorDolor`.
- `IT/CopiaSinIaIT`: `incumplimientoNoReincorpora` (fatiga y sin registro) y `retiradoPorDolorNoVuelve`.
- `IT/ProgresionCienPorCienIT.bajoOchentaAdaptaSinProgresar`.

**Hueco:** no hay prueba unitaria de `ProgresionResolver.cumple`. La POLÍTICA §6 cita el propio método como «prueba».

---

## 3. Incrementos máximos y pasos de 10 % / 5 %

**Clasificación: COMPROBADO_EN_CODIGO.**

| Paso | Cuándo | Límite | Código |
|---|---|---|---|
| **Recuperación** | Hay una reducción de dosis de la IA (`REVISAR_CARGA_Y_RECUPERACION` o `INVESTIGAR_CAMBIO_MINIMO`, APLICADA) en el linaje (`referenciaDeReduccion`, `PR:375-385`) y un ejercicio está por debajo de su referencia | Paso de la sesión ≤ `recovery_step_pct` (**10**); hacia la referencia **sin superarla**; una dimensión | `PR:216-221`, `haciaReferencia` `PR:540-565`, `dentroDelPaso` `PR:567-570` |
| **Progresión ordinaria** | Sin recuperación posible (no hay reducción, o la recuperación no cabe en el 10 %: opción 2) | Paso de la sesión ≤ `progression_increase_pct` (**5**); no se fuerza (`continue`) | `PR:603` |

**Regla de elección de la progresión ordinaria** (`PR:589-600`):
- primero +1 repetición;
- +1 serie solo con las repeticiones en el máximo del rango;
- se ordena por menor cambio relativo en el ejercicio, después por menor incremento en la sesión y después por posición.
- Solo `SETS_REPS` (`PR:582`): los ejercicios de DURACIÓN **no progresan nunca**. El diseño §1 contemplaba «+5 s»; no es un error, pero conviene declararlo.

**Rangos D-P3** (`PDP:31`, `PDP:39-66`; `PR.rango` `PR:624-656`):
- reps hasta base + 2;
- series hasta `SERIES_MAX_AMPLIACION` = 3 (`CapabilityPrescriptionPolicy.java:138`);
- en los estructurales de `GAIN_MUSCLE`, series fijas;
- el descanso no cambia.

**Pruebas:**
- `IT/ProgresionIT.comprobarPaso` (`:694-733`) afirma: +1 rep o +1 serie, mismo descanso, ≤ 5 % de la sesión, ≤ 1,10 × base, minutos que no bajan y una sola sesión cambiada.
- `IT/PendientesProgresionIT`:
  - `serieNoRecuperableYProgresionDeOtros` (opción 2: devolver la serie supera el 10 %);
  - `sinProgresionNoReincorpora` (LOSE_WEIGHT/30: el menor paso supera el 5 %).
- `ProgresionV24V8CTest`: `pasoFueraDeRango` (3×13) y `estructuralNoSubeSeries` (4×10).
- `IT/ProgresionIT.reduccionPorFatigaYRecuperacion`: la recuperación llega a la referencia y no la supera.

---

## 4. Techo del 110 % del volumen de referencia

**Clasificación: COMPROBADO_EN_CODIGO + DISCREPANCIA menor (parametrización).**

- **Referencia (V_base):** el volumen de la sesión **raíz del linaje V31**, es decir, la generación inicial (`PR:211`: `linaje.get(linaje.size()-1).origen()`). `linaje()` devuelve los tramos hasta un `source_planned_workout_id` nulo (`PR:277-278`).
  - Una regeneración (P-03 `SafeRegenerationService`, o reemplazar el plan con `generateWeeklyPlan`) crea una raíz nueva con dosis base, así que la base se reinicia (declarado en `SafeRegenerationService.java:33-42`).
- **Techo:** `floor(V_base × 1,10)` (`PR:74`, `PR:212`).
- **Con un relleno retirado (R2, PENDIENTE):** el tope es `techo − V_ret` (`PR:246-247`); la comprobación `nv > tope` está en `PR:607`.
- **Reincorporación:** solo si `v + V_ret ≤ techo` (`PR:259`).
- **Discrepancias:**
  - `TECHO = 1.10` es una **constante del código**. El diseño §8 preveía «un parámetro nuevo de techo para el MVP en una versión de configuración nueva».
  - `max_cumulative_volume_increase_pct` = 25 (V15) no se usa.
- **Recuperación y vuelta atrás** no comprueban `techo` explícitamente: están acotadas por la referencia o por la dosis anterior. Es correcto por construcción.
- **Pruebas:**
  - `IT/CopiaSinIaIT.techo` (16 semanas, INCREASE_STRENGTH; afirma vol ≤ ⌊1,10 × base⌋ en cada semana y mantenimiento final idéntico);
  - `IT/ProgresionIT.comprobarPaso:727`;
  - `IT/ProgresionIT.acortadaProgresaYReincorpora` (total 182 = techo, según la documentación).

---

## 5. Ausencia de progresiones duplicadas

**Clasificación: COMPROBADO_EN_CODIGO.**

| Garantía | Dónde |
|---|---|
| A lo sumo un paso por ciclo: se publica el primero válido y se sale | `AWCS:282-298` |
| En V8C, un paso solo sobre la sesión declarada; con orden de adaptar no hay paso | `CV:450` (`intervencion == null && paso != null && origen.equals(paso.origen())`); `AWCS:275` |
| BD: `cardinality(progressed_sessions) <= 1`; solo `SIN_ORDEN`; solo con plan publicado; sin repetidos y disjunto de los demás arreglos | `db/migration/V34__progresion_de_sesiones.sql:34-45` (`ck_paa_un_paso`, `ck_paa_paso_sin_orden`, `ck_paa_paso_publicado`, `ck_paa_progresadas_*`) |
| La misma sesión no progresa dos semanas seguidas: O→S tiene que ser una copia | `PR:296-298` |
| Repetir el ciclo devuelve el plan ya publicado | `WeeklyCycleService.java:130-135`; además, `CicloSemanalBloqueo` y el bloqueo de la fila de medición (`PlanAdaptationAttemptRepository.java:40+`) |
| Lo publicado es exactamente el paso propuesto, o se deshace la transacción | `AEM:102-111` |

**Pruebas:**
- `IT/ProgresionCienPorCienIT.reejecucionNoDuplica`: mismo plan, sin evidencia nueva, un único plan ACTIVE.
- `IT/PendientesProgresionIT.ciclosSimultaneos`: una semana y una evidencia.
- `ProgresionV24V8CTest`: `unaModificacionPorCiclo` y `pasoDeDosEjercicios`.
- `IT/ProgresionIT.assertSinViolaciones` (`:768-776`): ninguna fila con más de una progresada ni fuera de `SIN_ORDEN`.

---

## 6. Alternancia justa entre sesiones

**Clasificación: IMPLEMENTADO_SIN_PRUEBA** (sin prueba dirigida).

- **Orden de los candidatos** (`PR:157-163`):
  1. prioridad por tipo: vuelta atrás 0, recuperación 1, progresión 2, reincorporación 3, progresión EN_ESPERA 4 (`PR:170-177`);
  2. después, la sesión con **menos pasos ya dados en su linaje** (`pasosPrevios` = tramos `PROGRESADA`, `PR:195`);
  3. después, la fecha.
- La espera de dos semanas por sesión (`PR:296-298`) impide que la misma sesión progrese en ciclos consecutivos. Otra sesión puede progresar entretanto (POLÍTICA §1, «opción A»).
- **Matices:**
  - `pasosPrevios` cuenta también VUELTA_ATRAS, RECUPERACION y REINCORPORACION. Una sesión que subió y volvió atrás cuenta 2 y queda postergada.
  - La prioridad por tipo domina a la equidad: una recuperación de una sesión siempre gana a la progresión de otra.
- **Cobertura indirecta, no de la regla:**
  - `IT/ProgresionCienPorCienIT.dosSemanasPerfectasProgresa` (`:121-127`): la tercera semana no progresa la misma sesión, que es el efecto de la espera;
  - `IT/CopiaSinIaIT.techo`: todas las sesiones acaban en el techo.
- **Falta** una prueba que, con 2-3 sesiones elegibles a la vez, compruebe que se elige la de menos pasos y que el reparto en N semanas es equilibrado.

---

## 7. Trazabilidad de cada autorización de dosis

**Clasificación: IMPLEMENTADO_SIN_PRUEBA (parcial).** Las autorizaciones **no se persisten como tales**: se reconstruyen.

| Origen | Quién la crea | Qué se guarda | Dónde |
|---|---|---|---|
| `PASO` (este ciclo) | V8C, tras el paso estrecho (`CV:463-470`) | Sesión en `plan_adaptation_attempts.progressed_sessions`; detalle en `conservation_report.pasos` (tipo, ejercicio, dosis antes y después, volúmenes antes y después, base, techo, estado de la reserva); bloqueados en `pasos_bloqueados`; plan publicado con V31 (`source_planned_workout_id`) | V34 (`V34__…sql:28-45`); `PasoProgresion.java:21-25`; `EvidenciaV32.java:29-30`; `AEM:135` |
| `REDUCCION` (IA) | V8C, solo si baja, mismo tipo y descanso, y la intervención permite cambiar la dosis (`CV:669-672`) | **No** como autorización: queda en `applied_sessions` / `targeted_interventions` y en la diferencia entre el plan de origen y el resultante | V32 |
| `HEREDADA` (copia) | V8C, para los ejercicios con `dosisConLinaje` (`CV:494-498`, `CV:470-471`, `CV:657-660`) | **No se guarda**: `DLR` la recalcula en cada ciclo recorriendo V31 + V32 (`DLR:58-99`); la calcula `PreviousWeekAssembler` solo con algún interruptor encendido (`PreviousWeekAssembler.java:68`) | — |

- El conjunto `dosisAutorizadas` que usa V24 lleva `@JsonIgnore` (`CR:78-79`): **nunca llega a `conservation_report`**. Para auditar por qué V24 aceptó una dosis concreta hay que rehacer el recorrido. La evidencia lo permite: el paso trae `despues` y el linaje es íntegro.
- `PasoProgresion.contenido` también lleva `@JsonIgnore` (`PasoProgresion.java:23`), aunque `AEM` lo compara antes de escribir.
- **Pruebas:**
  - `IT/ProgresionIT.validacionIndependienteDistingue`: un validador independiente reconoce el paso registrado y rechaza una dosis que nadie autorizó;
  - `IT/ProgresionIT.dosisArbitrariaRechazada`;
  - `IT/LinajeIntegridadIT`.
- **Sin prueba unitaria de `DosisLinajeResolver`:** no hay ningún `*Test` que lo instancie.

**Recomendación:** persistir en `conservation_report` el resumen de `dosisAutorizadas` (origen, fecha, ejercicio y dosis), o al menos su recuento por origen, para que la autorización sea trazable sin reconstruirla.

---

## 8. Conservación de dosis al copiar planes (copia semanal)

**Clasificación: COMPROBADO_EN_CODIGO con la progresión encendida + DISCREPANCIA en tres vías.**

### Caso normal (interruptor encendido)

- La copia idéntica emite `HEREDADA` para las dosis con linaje (`CV:494-498`) y V24 la acepta (`PDV:270-277`, `PDV:596-598`).
- Pruebas:
  - `IT/ProgresionIT.progresionOrdinaria` (`:161-171`: «la dosis progresada se hereda tal cual»);
  - `ProgresionV24V8CTest`: `copiaConLinajePublicada` (acepta) y `copiaSinLinajeRechazada` (rechaza);
  - `IT/CopiaSinIaIT` («la copia conserva todo»).
- La IA, en una sesión señalada, conserva `HEREDADA` en los ejercicios que no toca (`CV:657-660`).

### DISCREPANCIA A · apagar la progresión rompe las copias de `GAIN_MUSCLE` ya progresadas

- Con el interruptor apagado:
  - `autorizarDosis = progresion || estricta.validaCapacidades()` (`AWP:198`) vale false;
  - `PreviousWeekAssembler` no calcula `dosisConLinaje` (`:68`).
- En P2-D, 4D sigue en ENFORCE (`StrictValidationAdapter.java:67-71`; `AWP:189-193`). Por tanto, una copia de un 3×11 de `GAIN_MUSCLE` la **rechaza V24**.
- Lo **demuestra la propia prueba** `ProgresionV24V8CTest.apagadoNoHereda` («con la progresión apagada no hay herencia: la misma copia la rechaza V24»).
- Esto **contradice** `POLITICA_ADAPTACION_Y_PROGRESION.md` §5, que dice que al revertir «las semanas siguientes vuelven a ser copias sin pasos».
- En los objetivos por capacidades no ocurre: con los dos interruptores apagados, la V24 por capacidades no se aplica en P2-D.
- **Conclusión:** la reversión no es segura para `GAIN_MUSCLE` una vez dado un paso. Esas semanas saldrían `NO_FACTIBLE`.

### DISCREPANCIA B · la reparación de planes publicados (`PPSR`) no emite dosis heredadas

- `PPSR:189-190` llama a `v8c.validateContinuidad(…, sustituciones)` (5 argumentos), que fija `paso = null` y `autorizarDosis = false` (`CV:276-283`).
- Después valida con la validación estricta (`PPSR:192-193`). Con la progresión encendida, esa validación tiene la V24 por capacidades activa, y 4D siempre lo está.
- **Efecto:** una semana publicada con una dosis progresada **no se puede reparar** por seguridad (sale `NO_VALIDA`).
- Sin prueba: `src/test/java/main/web/services/fitsense/p2d/ProteccionPlanesPublicadosIT.java` no toca la progresión.

### DISCREPANCIA C · una versión reparada rompe el linaje para siempre (deducido; NO_VERIFICADO por ejecución)

- `PPSR` publica una versión nueva (`PPSR:264-295`) sin fila en `plan_adaptation_attempts` con ese `resulting_plan_id`. Conserva el V31 de la sesión reemplazada (`PPSR:276-279`).
- `PR.linaje` (`PR:279-280`), `DLR` (`DLR:65-66`) y `RAR` (`RAR:76-77`) exigen `findByResultingPlanId(plan).size() == 1`. Con 0, el linaje es «roto».
- Como cada linaje posterior atraviesa esa versión reparada, a partir de ahí, para ese participante:
  - no hay progresión, recuperación ni reincorporación;
  - D8 desaparece;
  - no hay dosis heredadas: con la progresión encendida, la copia de una dosis no exacta la rechaza V24.
- **Prueba a añadir:** reparación por seguridad de una semana publicada → ciclo siguiente con la progresión encendida.

### Riesgo D (4D v3.2/v3.3 frente a progresión, NO_VERIFICADO)

- V24 admite la serie adicional (3×13 de zona pequeña, 3×10 del bíceps en PULL) **solo si sin ella la sesión no llega al piso de V20** (`PDV:600-604`, `PDV:619-640`).
- Un paso de progresión en **otro** ejercicio de la misma sesión alarga la sesión y puede hacer innecesaria esa serie. Entonces V24 rechaza el paso, que queda en `pasos_bloqueados`, y la sesión deja de progresar antes del techo.
- No es inseguro, pero contradice `ADENDA_4D_v3_2.md` §2 («Progresión y reincorporación: sin cambios»).
- Sin prueba: `Reincorporacion4DTest.progresionDesdeLaAmpliada` y `progresionDelBicepsAmpliado` solo prueban `rango()`, no la revalidación de la sesión.

---

## 9. Validaciones de linaje

**Clasificación: COMPROBADO_EN_CODIGO (integradas); IMPLEMENTADO_SIN_PRUEBA (unitarias).**

### `PR.linaje` (`PR:272-290`)

Devuelve null (ningún paso para esa sesión) si:
- hay ≠ 1 intento para el plan;
- la clase es ambigua: el origen está en más de un arreglo o en ninguno (`EvidenciaV32.clase` `:40-47`);
- el informe es ilegible (`EvidenciaV32.informe` `:69-79`);
- falta el origen;
- hay un ciclo (`vistas`).

Una excepción al leer el linaje deja la sesión sin candidato y no tumba el ciclo (`PR:151-155`).

### `DLR.autorizada` (`DLR:58-99`)

- Recorre hacia atrás atravesando las copias de esa misma dosis.
- Solo autoriza si la dosis la introdujo:
  - un `PROGRESADA` cuyo `paso.despues` coincide exactamente y que no es una REINCORPORACION;
  - o una `APLICADA` que bajó.
- Rechaza un ejercicio duplicado en la sesión de origen (`DLR:76`).

### `RAR` (D8 + D-P7)

Continúa a través de `PROGRESION` y `RECUPERACION` que no bajan el volumen ni la duración (`RAR:103-111`).

### En la BD y al publicar

- `fn_intento_coherente`: `progressed_sessions` pertenece al plan de origen (`V34__…sql:108-119`).
- `AEM:104-111`: lo publicado coincide exactamente con el paso.
- V8C: la sesión es idéntica a `paso.contenido` (`CV:455-456`).

### Pruebas

- `IT/LinajeIntegridadIT` («se clasifica sin ambigüedad; sin evidencia, se detecta»);
- `IT/ProgresionIT.assertSinViolaciones` (`docs/FitSense/consultas/linaje_v31_v32.sql`);
- `ReduccionAutorizadaResolverTest`.

**Huecos:**
- no hay prueba unitaria de `ProgresionResolver.linaje` ni de `DosisLinajeResolver` con un linaje roto o ambiguo;
- caso C de §8 (versión reparada).

---

## 10. Reincorporación solo de ejercicios previamente retirados

**Clasificación: COMPROBADO_EN_CODIGO.**

- **Solo se construye un paso `REINCORPORACION` desde `retirado(linaje, s)`** (`PR:227-268`, `PR:392-409`). Exige:
  - un tramo `APLICADA` con intento APLICADA e intervención `ACORTAR_O_REORGANIZAR`;
  - sin sustitución por seguridad;
  - **exactamente un** ejercicio quitado y ninguno añadido;
  - que ese ejercicio no esté ya en S.
- **Excluidos** los retirados por dolor, los sustituidos por seguridad y los cambiados por una variante (no son ACORTAR).
- **Más condiciones:**
  - descartado si ya no es elegible, si está excluido o si dolió (`PR:231-237`);
  - al menos una `PROGRESION` de esa misma sesión después del último acortamiento (decisión 1(a), `PR:256`, `progresionesTrasAcortar` `PR:466-478`);
  - entra con la dosis inicial **exacta** de su clase (`exacta`, `PR:659-683`), al final de la sesión;
  - el total cabe en el techo;
  - M vigente (`PR:239-244`, `PR:416-431`).
- V8C exige que la reincorporación **solo añada ese ejercicio** (`CV:603-605`). No vuelve a comprobar que estuviera retirado: confía en que el paso sea idéntico al propuesto por el backend (`CV:455`), que es inalterable por la IA.
- **Pruebas:**
  - `ReincorporacionTrasProgresionTest` (7 casos de 1(a): recuperación, progresión anterior, último acortamiento, otra sesión, otros tipos, posterior, sin acortamiento);
  - `Reincorporacion4DTest` (vuelve a 2×13 o 2×10 base, no a la ampliada ni a la histórica);
  - `IT/CopiaSinIaIT`: `sinRetiradaNoSeReincorpora`, `retiradoPorDolorNoVuelve`, retirado fuera del catálogo → `DESCARTADA`, y «§5 fila 5» con M;
  - `IT/ProgresionIT`: `acortadaProgresaYReincorpora` y `p02ReincorporacionDescartadaPorDolor`;
  - `IT/PendientesProgresionIT.sinProgresionNoReincorpora`.
- **Límites no documentados:**
  1. Un ACORTAR que quitó **dos** ejercicios no se reincorpora nunca: se salta por `quitados.size() != 1` (`PR:403`).
  2. Con **dos acortamientos sucesivos**, solo se considera el más reciente. Cuando vuelve, `retirado()` devuelve null (`PR:404`) y el primero ya no se evalúa ni reserva margen. Sin prueba.

---

## 11. Prohibición de restauración automática

**Clasificación: COMPROBADO_EN_CODIGO.**

- No queda código de restauración: `grep -ri "restauracion|restoration|restored_sessions"` en `src/main` solo encuentra comentarios en migraciones (`V34__…sql:4-5`, `V35__user_roles.sql:5`, `V42__…sql:5`).
- `fitsense.adaptation.restoration.enabled` no existe.
- **Sustitutos acotados:**
  - recuperación gradual, como mucho el 10 % por paso, nunca por encima de la referencia (`PR:540-565`);
  - la reincorporación devuelve la dosis **base**, no la histórica (`PR:659-683`; `Reincorporacion4DTest.noRestauraLaHistorica`);
  - la IA nunca sube dosis, con cualquier interruptor (`CV:500-507`, `CV:662-667`).
- **Pruebas:** `ProgresionV24V8CTest.iaNoSubeDosis`, `IT/ProgresionIT.iaNoSubeDosis`, `IT/PendientesProgresionIT.subidaDeCargaRechazada`.

---

## 12. Progresar cuando nunca se retiró un ejercicio

**Clasificación: COMPROBADO_EN_CODIGO.**

- Con `retirado == null`, progresión ordinaria hasta el techo completo (`PR:227-229`). La progresión no depende de una reducción previa (diseño §0).
- **Pruebas:**
  - `IT/ProgresionIT.progresionOrdinaria` (los 4 objetivos, sin adaptaciones);
  - `IT/CopiaSinIaIT.sinRetiradaNoSeReincorpora` (12 semanas: progresión y mantenimiento, ninguna reincorporación);
  - `IT/CopiaSinIaIT.techo`;
  - `IT/ProgresionCienPorCienIT.dosSemanasPerfectasProgresa`.

---

## 13. Feature flag: por omisión y en la evaluación K20

**Clasificación: COMPROBADO_EN_CODIGO.**

### Por omisión: APAGADA

- `src/main/resources/application.properties`:
  - `:117` `fitsense.adaptation.progression.enabled=${FITSENSE_PROGRESSION_ENABLED:false}`;
  - `:120` `fitsense.adaptation.capability-validation.enabled=${FITSENSE_ADAPTATION_CAPABILITY_VALIDATION:false}`.
- Los mismos valores por omisión (`:false`) se repiten en `AWCS:142`, `AdaptationWiringConfiguration.java:53`, `StrictValidationAdapter.java:56-57` y `PreviousWeekAssembler.java:63-65`.
- No hay ninguna otra fuente que la encienda: `application-local-dev.properties` no la menciona y no hay compose ni `.env` en el repositorio.

### Lo que hace el interruptor encendido (`progression=true`)

1. pasos en `NO_CHANGE` (`AWCS:275`);
2. `autorizarDosis` (`AWP:198`);
3. dosis heredadas (`PreviousWeekAssembler.java:68`);
4. **la V24 por capacidades en P2-D**, aunque `capability-validation` esté en false (`StrictValidationAdapter.java:62`).

### K20 (`IT/EvaluacionK20IT.java`)

- Fija `fitsense.adaptation.progression.enabled=false` en `@SpringBootTest(properties=…)` (`:113-118`), junto con `difficulty-guard=preferencia` y `pain-localized-removal=on`.
- No fija `capability-validation`, que queda en **false** por omisión.
- Consecuencias:
  - en K20 no hay pasos, ni recuperación, ni reincorporación, ni dosis heredadas;
  - P2-D valida **sin** la V24 por capacidades: solo 4D en `GAIN_MUSCLE`;
  - la IA puede bajar dosis libremente en los objetivos por capacidades (sin el rango de `PDP`), pero no subirlas.
- El informe de configuración escribe `cfg.put("progresion", "false")` como **literal** (`:276`), no leído del contexto. Si alguien cambiara la propiedad, el informe seguiría diciendo false. Mejor leerlo del `Environment`.
- La cabecera de la clase lo declara: «progresión apagada… (no se puede cambiar con -D)» (`:104-106`).
- **Implicación para la tesis:** K20 no aporta ninguna evidencia sobre progresión ni reincorporación. Esa evidencia está en `ProgresionIT`, `ProgresionCienPorCienIT`, `PendientesProgresionIT`, `CopiaSinIaIT` y `EvaluacionLimpiaProgresionIT`, esta última solo con `-Dfitsense.evaluacion.limpia=true` (`:76`).

### Pruebas con el interruptor encendido

`ProgresionIT`, `ProgresionCienPorCienIT`, `PendientesProgresionIT`, `CopiaSinIaIT`, `AdaptacionPorCausasIT`, `PoliticaV4CCicloIT`, `C2ReproduccionN03IT`, `CorreccionesReplicateEncendidoIT` y `EvaluacionLimpiaProgresionIT` (condicionada).

---

## 14. Separación entre la V24 de la generación inicial y las autorizaciones de adaptación y progresión

**Clasificación: separación COMPROBADO_EN_CODIGO; contradicciones puntuales = DISCREPANCIA.**

### Separadas y explícitas: sí

- **Generación:** `PlanGenerationPipeline.java:127,158,182` llama a `validator.validate(draft, context, divisor)` (3 argumentos), que fija `ConservationReport.vacio()` (`PDV:128-130`), así que **no hay ninguna autorización**.
  - El bean `PlanDraftValidator` de la generación no lee ningún interruptor de progresión.
  - V24 por capacidades en la generación: `fitsense.capability.enforcement=ENFORCE` (`application.properties:106`).
- **Adaptación:** usa otro validador (`StrictValidationAdapter`), construido según los interruptores. Las autorizaciones solo las emite V8C (`CV`).
- **Rangos:** V24 solo acepta una autorización dentro del rango de `PDP`. Fuera de rango la rechaza (`ProgresionV24V8CTest.pasoFueraDeRango`).
- **Alcance:** cada autorización es estrecha: fecha de la sesión + `exerciseId` + series, reps, segundos y descanso exactos (`DosisAutorizada.autoriza`, `DosisAutorizada.java:22-27`).
- **Pruebas de aislamiento:**
  - `ProgresionV24V8CTest`: `generacionRechazaDosisNoExacta` y `generacionSinPermisos`;
  - `GeneracionReferenciaIT` (72 perfiles idénticos a `docs/FitSense/evidencia/progresion/generacion_referencia.txt`).
  - **Hueco:** `GeneracionReferenciaIT` se ejecuta con la progresión en su valor por omisión (false). Conviene una variante con `progression.enabled=true` y `capability-validation.enabled=true`, para demostrar que encenderlas no cambia la generación (hoy está garantizado por estructura, no por prueba).

### ¿Puede una autorización de progresión afectar a la generación inicial? No por código

- La generación nunca recibe un `ConservationReport` con autorizaciones.
- El único efecto indirecto es la regeneración P-03 (`SafeRegenerationService`), que parte de las dosis base: pierde las progresiones por diseño.

### ¿Puede afectar a otro objetivo? No verificado del todo

- La autorización se calcula dentro del ciclo de un participante, y V24 vuelve a comprobar el rango con el `goalType` del perfil **actual** (`PDV:238-243`, `PR:626`).
- `DLR` **no comprueba el objetivo** con el que se dio el paso. Si un participante cambiara de objetivo y el ciclo siguiera copiando la semana (no se verificó si un cambio de perfil fuerza una regeneración), una dosis progresada en `GAIN_MUSCLE` podría quedar como `HEREDADA` bajo `LOSE_WEIGHT` si cae en ese rango. Además, V_base sería la de la raíz del objetivo anterior. **NO_VERIFICADO.** Prueba sugerida: cambio de objetivo tras un paso.

### Contradicciones encontradas

1. **Progresión → copia siguiente con V24:** resuelta mientras el interruptor siga encendido (HEREDADA).
   - Al apagarlo, `GAIN_MUSCLE` falla (§8-A, demostrado por `apagadoNoHereda`).
   - Al reparar la semana por seguridad, falla (§8-B) y el linaje se rompe (§8-C).
2. **Vuelta atrás frente a D-P7 (D8):**
   - Una sesión acortada bajo el piso de V20 que progresa conserva la exención (`CV:476-477`, `PasoProgresion.noReduce` `:35-40`).
   - En cambio, la **vuelta atrás no la hereda** (`noReduce()` false; `RAR:107`), así que V20 rechaza la vuelta atrás si la sesión sigue bajo el piso.
   - Resultado: ante fatiga tras un paso, en esas sesiones **no se puede volver atrás** y se publica la copia con la dosis progresada, que sí conserva D8 por su linaje PROGRESION.
   - Contradice D-P6 y el diseño §3.4 («Fatiga… tras un paso → vuelve al paso anterior»).
   - `ProgresionV24V8CTest.dp7VueltaAtrasNoHereda` solo afirma que no hay exención; **no afirma qué se publica**. Lo mismo vale para la vuelta atrás de una REINCORPORACION que deje la sesión bajo el piso.
3. **Reducción de la IA sobre una dosis progresada de `GAIN_MUSCLE`:**
   - `PDP.admite4D` rechaza siempre el origen `REDUCCION` (`PDP:41`). Una bajada intermedia (3×12 → 3×11), dentro del rango de progresión, la rechaza V24; solo la vuelta a la base exacta (3×10) pasa.
   - Es conservador, pero no está documentado (`ProgresionIT.fatigaTrasUnPaso` solo afirma «nunca sube»).
4. **Incoherencia de la V24 por capacidades en P2-D:** con los interruptores apagados (producción y K20), P2-D no la aplica (`StrictValidationAdapter.java:67-71`), así que la IA puede publicar cualquier dosis **inferior** en esos objetivos. Está declarada (diseño §1.3 y §12.5; D-P11 «no activar todavía») y no es un defecto nuevo, pero sigue abierta.
5. **4D v3.2/v3.3 frente a progresión** (§8-D): posible bloqueo prematuro de pasos. NO_VERIFICADO.

---

## 15. Defectos, huecos de pruebas y discrepancias entre diseño e implementación

| # | Hallazgo | Tipo | Evidencia |
|---|---|---|---|
| H1 | Progresión ordinaria al **100 %**; el diseño y la implementación V34 dicen 80 % | DISCREPANCIA (documental) | `PR:114`, `PR:226`; DISENO §0/§12.7; IMPLEMENTACION §5.2; POLITICA §1 |
| H2 | `progression_required_weeks` no se usa; `requiredWeeksOrDefault` = 1 | DISCREPANCIA | `CalculationParams.java:88`; `AdjustmentDecisionTable:197` (fuera del flujo) |
| H3 | Techo 1,10 fijo en el código; 25 % de V15 sin uso | DISCREPANCIA menor | `PR:74` |
| H4 | Reversión del interruptor: las copias de `GAIN_MUSCLE` progresadas pasan a `NO_FACTIBLE` | DISCREPANCIA (POLÍTICA §5) | `AWP:198`, `PreviousWeekAssembler:68`, `ProgresionV24V8CTest.apagadoNoHereda` |
| H5 | Una reparación de plan publicado no emite dosis heredadas: no se puede reparar una semana progresada | DEFECTO probable | `PPSR:189-193`, `CV:276-283` |
| H6 | Una versión reparada sin intento V32 rompe el linaje: sin progresión, D8 ni herencias desde entonces | DEFECTO probable, NO_VERIFICADO | `PPSR:264-295`; `PR:279-280`; `DLR:65-66`; `RAR:76-77` |
| H7 | Vuelta atrás bajo el piso de V20 sin D-P7: no se puede volver atrás tras fatiga | DISCREPANCIA (D-P6) | `PasoProgresion.java:39`; `RAR:107`; `CV:476-477` |
| H8 | 4D v3.2/v3.3 (serie adicional «necesaria») puede bloquear pasos en otros ejercicios | Riesgo, NO_VERIFICADO | `PDV:600-640` |
| H9 | ACORTAR que quitó 2 ejercicios, o dos acortamientos sucesivos: no se reincorpora o se pierde el primero | Límite no documentado | `PR:403-404` |
| H10 | `dosisAutorizadas` y `contenido` no se persisten (`@JsonIgnore`) | Trazabilidad parcial | `CR:78-79`; `PasoProgresion.java:23` |
| H11 | Los ejercicios de DURACIÓN no progresan | Límite no documentado como tal | `PR:582` |
| H12 | `IMPLEMENTACION_PROGRESION_V34.md` §8 y §11 dicen que M «no existe en el backend»; el código ya la usa (`mVigente`, V42) | Documento desfasado | `PR:239-244`, `PR:416-431`; `CopiaSinIaIT` «§5 fila 5» |
| H13 | K20 escribe «progresion=false» como literal | Menor | `EvaluacionK20IT.java:276` |
| H14 | `DLR` no comprueba el objetivo: posible herencia entre objetivos tras un cambio de perfil | NO_VERIFICADO | `DLR:58-99` |
| H15 | Una reducción intermedia de la IA en `GAIN_MUSCLE` progresado se rechaza siempre | Comportamiento sin documentar | `PDP:41` |

### Pruebas existentes (nombre → qué afirman)

- **`ProgresionV24V8CTest`** (unitaria, 16): la generación sin permisos; la copia sin linaje se rechaza y con linaje se acepta; con el interruptor apagado no hay herencia; paso autorizado sí, arbitrario no; fuera de rango (3×13) no; estructural con +1 serie no; dos ejercicios no; derivada distinta del paso no; con orden no; la IA no sube dosis; D-P7 (sí con D8, V20 sin D8, la vuelta atrás no hereda).
- **`ProgresionIT`** (IT, interruptor encendido):
  - `progresionOrdinaria` (4 objetivos, 5 semanas: un paso, +1 rep o serie, ≤ 5 %, ≤ techo, sin IA, herencia);
  - `reduccionPorFatigaYRecuperacion`;
  - `iaNoSubeDosis`;
  - `acortadaProgresaYReincorpora` (D-P7 + R2);
  - `reduccionPorFatigaCuatroObjetivos`;
  - `dosisArbitrariaRechazada`;
  - `validacionIndependienteDistingue`;
  - `fatigaTrasUnPaso`;
  - `dolorTrasUnPaso`;
  - `p02ProgresionSinElExcluido`;
  - `p02ReincorporacionDescartadaPorDolor` (DESCARTADA);
  - `p03Nivel2TrasReduccion` y `p03Nivel2TrasProgresion`;
  - invariantes V31-V34 (`assertSinViolaciones`).
- **`ProgresionCienPorCienIT`:** `dosSemanasPerfectasProgresa`, `semanaParcialBloqueaDosSemanas`, `registroIncompletoNoProgresa`, `bajoOchentaAdaptaSinProgresar` y `reejecucionNoDuplica`.
- **`PendientesProgresionIT`:** `serieNoRecuperableYProgresionDeOtros` (opción 2), `adaptacionSoloDeCarga`, `subidaDeCargaRechazada`, `sinProgresionNoReincorpora` (1(a)) y `ciclosSimultaneos`.
- **`CopiaSinIaIT`:**
  - copia y progresión con 0 llamadas, sin duplicar;
  - datos insuficientes;
  - fatiga, 1 llamada;
  - mantenimiento, progresión y reincorporación tras acortar;
  - §5 fila 5 (M, EN_ESPERA_DE_TIEMPO);
  - sin retirada no hay reincorporación;
  - incumplimiento no reincorpora;
  - retirado por dolor no vuelve;
  - retirado no elegible → DESCARTADA;
  - techo y mantenimiento.
- **`ReincorporacionTrasProgresionTest`** (unitaria, 7): regla 1(a).
- **`Reincorporacion4DTest`** (unitaria, 5): dosis base al reincorporar y rangos desde la serie ampliada.
- **Condicionadas** (no corren por omisión):
  - `EvaluacionLimpiaProgresionIT` (`-Dfitsense.evaluacion.limpia=true`);
  - `ReincorporacionIaRealIT` (`-Dfitsense.reincorporacion.ia`);
  - `ProgresionFactibilidadIT` y `ProgresionReservaIT` (`-Dfitsense.factibilidad=true`);
  - `EvaluacionLecturaV24IT` y `EvaluacionDirectaV24IT` (requieren URL).
- **`GeneracionReferenciaIT`:** 72 planes iniciales idénticos a la referencia (con el interruptor en false).

### Pruebas que conviene EJECUTAR (requieren Docker; no se ejecutaron aquí)

```
mvn verify -Pintegration -Dit.test='ProgresionIT,ProgresionCienPorCienIT,PendientesProgresionIT,CopiaSinIaIT,GeneracionReferenciaIT,LinajeIntegridadIT'
mvn test -Dtest='ProgresionV24V8CTest,ReincorporacionTrasProgresionTest,Reincorporacion4DTest,ReduccionAutorizadaResolverTest'
```

### Pruebas que conviene AÑADIR

1. **Reversión del interruptor (H4):** `GAIN_MUSCLE` progresa con el interruptor encendido → se reinicia con false → el ciclo siguiente. Se espera saber si sale `NO_FACTIBLE`; hay que decidir qué debe pasar y corregir la POLÍTICA §5 o el código.
2. **Reparación de un plan publicado con progresión (H5, H6):** semana con una dosis heredada + exclusión de seguridad → `repairPublishedPlanForSafety`; después, dos ciclos más. Comprobar la reparación, el linaje, D8 y la herencia.
3. **Vuelta atrás bajo el piso de V20 (H7):** sesión acortada (D8) que progresa → fatiga con `NO_CHANGE`. Afirmar qué se publica: vuelta atrás o copia con la dosis progresada.
4. **4D v3.2 + progresión (H8):** `GAIN_MUSCLE` con un relleno de zona pequeña a 3×13 «necesario» → progresión en otro ejercicio hasta que la serie deje de ser necesaria.
5. **Alternancia (§6):** 3 sesiones elegibles a la vez; afirmar que se elige la de menos pasos y que, en N semanas, el reparto es equilibrado.
6. **Unitarias de `ProgresionResolver`:** `cumple`, `dosSemanasPerfectas`, `linaje` roto o ambiguo; recuperación con el 80-99 % frente a progresión con el 100 %.
7. **Unitarias de `DosisLinajeResolver`:** dosis introducida por PROGRESION, REDUCCION, subida de la IA, REINCORPORACION y VUELTA_ATRAS; ejercicio duplicado; informe ilegible.
8. **`GeneracionReferenciaIT` con `progression.enabled=true` y `capability-validation.enabled=true`:** 72/72 idénticos.
9. **Dos acortamientos sucesivos y ACORTAR de dos ejercicios (H9).**
10. **Cambio de objetivo tras un paso (H14).**
