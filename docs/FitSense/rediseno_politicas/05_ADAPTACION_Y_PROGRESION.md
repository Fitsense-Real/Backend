> **Nota de integración (05/11/2026).** Informe del trabajo de adaptación y progresión, hecho en una rama aislada (`rediseno-adaptacion`, local) e integrado por cherry-pick en `investigacion/rediseno-politicas`. Equivalencias: 35f3274→2ae2fd3, 2327463→64db937, 4828145→138ee14, 0e8fe26→f23cfaf, d05a689→807cde7, 16613e1→e29477e. Los puntos de §7 que dependían del integrador ya están hechos: `EvaluacionK20IT` usa `PermisosIntervencion` (dbf7cc6) y `PoliticaPlanificacionTest.catalogo` espera 5 versiones (1b60926). Las referencias a `aud2/ciencia_rediseno.md` remiten a `02_EVIDENCIA_DOSIS.md`.

# Rediseño R/R' · adaptación (P-A1, P-A2) y defectos de progresión

**Rama local:** `rediseno-adaptacion`, desde `be825f9`. Sin push, sin merge, sin Replicate y sin Neon.
**Fecha:** 05/11/2026.
**Evidencia de partida:** `aud2/ciencia_rediseno.md` §4 (fatiga) y §5 (desmotivación), y `docs/FitSense/auditoria_post_rrp/05_AUDITORIA_PROGRESION.md` (H1, H4/P-P1, H6, H7, H9, H14).

---

## 0. Resumen

| # | Qué | Alcance | Estado | Prueba principal |
|---|---|---|---|---|
| P-A1 | Fatiga en `GAIN_MUSCLE`: dosis de mantenimiento trazable (V8C → `MANTENIMIENTO` → V24), recuperación gradual y avisos de fatiga persistente | Solo `PLAN-REV-2.0` (`fatigaGM`) | Hecho | `RedisenoAdaptacionTest$Fatiga` (10), `RedisenoFatigaGmIT` (2), `PromptRev2Test` |
| P-A2 | Desmotivación (`INVESTIGAR_CAMBIO_MINIMO`): como máximo UNA sustitución lateral estricta | Solo REV2 (`sustitucionDesmotivacion`) | Hecho | `RedisenoAdaptacionTest$Desmotivacion` (7), `PromptRev2Test` |
| P-C2 (V8C) | Una `VARIANTE` no puede subir el impacto (`ImpactoPolicy.sube`) | Solo REV2 (`impacto`) | Hecho | `varianteNoSubeImpacto` |
| P-P1 / H4 | Las dosis con linaje se heredan siempre, también con la progresión apagada | Todas las políticas | Corregido | `ProgresionV24V8CTest.apagadoHereda` y `apagadoSinLinajeRechazada` |
| P-P2 | La reparación de seguridad emite las autorizaciones heredadas | Todas | Reproducido en IT y corregido | `ReparacionLinajeIT` |
| H6 | El linaje atraviesa la versión `SAFETY_REPAIR` sin intento propio | Todas | Reproducido en IT y corregido | `ReparacionLinajeIT` |
| H7 | Una `VUELTA_ATRAS` conserva la exención de V20 de la reducción autorizada | Todas | Corregido | `dp7VueltaAtrasConservaExencion` y `vueltaAtrasSinReduccionNoGanaExencion` |
| H14 | Dosis heredada de GM con otro objetivo | — | No hace falta código: V24 revalida con el objetivo vigente | `RedisenoAdaptacionTest$CambioDeObjetivo` |
| Reincorporación | Varios retirados y acortamientos consecutivos | — | Analizado y documentado; no se corrige (decisión de diseño D-P8) | — (§5.6) |
| Docs | Nota fechada sobre 100 % frente a 80 % | Documentación | Hecho | — |

**Lo que no cambia:**
- La progresión sigue **apagada por defecto**.
- No hay **restauración automática**.
- La generación inicial y la V24 de generación no cambian. Lo prueban `generacionIntacta`, `GeneracionReferenciaIT` y `PromptHistoricoHuellaTest`.
- Con `PLAN-HIST-1` el prompt es **idéntico byte a byte**.

---

## 1. Decisiones

1. **P-A1 y P-A2 se activan solo por las banderas de la política.**
   - `PlanningPolicy.fatigaGM()` y `sustitucionDesmotivacion()` solo están a `true` en REV2.
   - `TipoIntervencion` (en `shared`, sin política) no cambia.
   - Lo que la política añade vive en `PermisosIntervencion`. La usan V8C, el prompt y la factibilidad, así que los tres dicen lo mismo.
2. **La dosis de mantenimiento se crea solo con la política, pero se hereda con cualquiera** (`ProgresionDosisPolicy.admite4D`):
   - `MANTENIMIENTO` exige `fatigaGM`.
   - `HEREDADA` con linaje se acepta siempre. Así, si se vuelve de REV2 a la histórica, V24 no deja la semana NO_FACTIBLE. Es el mismo criterio que P-P1.
   - **Límite medido:** con la histórica sigue rigiendo el piso del 70 % de V20. D8 solo concede la exención de V20 a un `ACORTAR`, y en una sesión al límite la copia heredada puede quedarse corta (30 s en `copiaHereda`). Afecta solo a la reversión de política, que no está prevista. Si se quiere cubrir, D8 tendría que reconocer el `MANTENIMIENTO` como origen de reducción.
3. **Recuperación con la progresión apagada (solo REV2):**
   - `AdaptedWeekCommandService.continuidad` propone **solo pasos `RECUPERACION`**, con sus reglas de siempre: 80 %, dos semanas cumplidas, sin fatiga ni dolor, ≤ `recovery_step_pct` y un paso por ciclo.
   - Nunca propone `PROGRESION`, `REINCORPORACION` ni `VUELTA_ATRAS`.
   - Sin esto, la dosis de mantenimiento quedaba para siempre. La orden pide una vuelta gradual, no automática.
4. **Los defectos de implementación (P-P1, P-P2, H6, H7) se corrigen en todas las políticas.** Son errores de linaje o de autorización y no cambian ninguna regla de entrenamiento.
5. **No he tocado** `CapabilityWeekBuilder`, `RuleBasedTrainingPlanGenerator`, `ExerciseSelector`, `WeeklySplitPlanner`, la forma de `PlanningPolicy`, V20/V4/V15 de `PlanDraftValidator` (solo las dos llamadas de V24 a `admite4D`), `BarridoGeneracionInicialIT`, `DiagnosticoGeneracionJ03IT` ni `EvaluacionK20IT`.

---

## 2. Dosis autorizadas (P-A1)

| Situación | Dosis | Origen de la autorización | Política |
|---|---|---|---|
| Fatiga (`REVISAR_CARGA_Y_RECUPERACION`) en GM, colocación **estructural** de 3 series | **3×10 → 2×10**: una serie menos (−33 %), mismas repeticiones y mismo descanso | `MANTENIMIENTO` (V8C, `revisarDosisDeLaIa`) | Solo REV2 |
| Relleno (2 series) | No baja (V9 prohíbe < 2) | — | — |
| Repeticiones o descanso | No cambian | — | — |
| Copia posterior con linaje | La misma dosis de mantenimiento | `HEREDADA` | Todas |
| Vuelta | +1 serie en un ejercicio por paso, nunca por encima de la dosis de antes | `PASO` (`RECUPERACION`) | REV2 (con la progresión apagada); todas con ella encendida |
| Cualquier otra reducción en GM (reps, 1 serie, desmotivación…) | Rechazada por V24 | — | — |

- **Base:** `ciencia_rediseno` §4 (series −33 a −50 % una semana, mismos ejercicios y frecuencia). Elegí el extremo inferior (−33 %) porque es el único que cabe en 4D sin bajar de V9.
- La dosis concreta queda **PENDIENTE DE VALIDACIÓN PROFESIONAL**. En ningún caso es una dosis arbitraria de la IA: la IA solo puede proponer exactamente esta.
- **Fatiga persistente** (`PreviousWeekSummary.fatigaConsecutiva`, calculada por `DosisLinajeResolver` sobre el linaje V31/V32):
  - 2 semanas seguidas → `FATIGA_REPETIDA`: revisar sueño, estrés, alimentación o enfermedad.
  - 3 o más → `FATIGA_PERSISTENTE`: consultar a un profesional sanitario.
  - En ambos casos la dosis no se reduce más.
  - Los avisos van en `ConservationReport.avisos` (`conservation_report.avisos`).
- **Dolor ≠ fatiga:** si consta dolor, una bajada de dosis no basta: manda la seguridad (`dolorNoEsFatiga`).
- **Límite medido (no se fuerza):**
  - Cada paso de recuperación exige dos semanas cumplidas desde el anterior. Con 3 series quitadas, la vuelta llega en las semanas **4, 6 y 8** (`RedisenoFatigaGmIT`: 6 semanas para volver).
  - La evidencia describe una vuelta en dos pasos (80–85 % → 100 %).
  - Acelerarla exige cambiar la regla de un paso por ciclo (`ck_paa_un_paso`) o la espera de dos semanas del resolutor. Es una decisión de diseño, así que **no la tomo aquí** y la dejo propuesta.

## 3. Sustituir 1 ejercicio frente a quitar 1 repetición (P-A2)

- **Evidencia (`ciencia_rediseno` §5):**
  - Sustituir un accesorio equivalente tiene respaldo: variedad y autonomía (Baz-Valle 2019, Sylvester 2016).
  - Quitar una repetición **no tiene ninguno** y no cambia la experiencia de la sesión.
- **Decisión con REV2:**
  - El prompt prefiere **una** sustitución. Solo si la lista de sustitutos está vacía admite «bajar un poco la dosis» (en objetivos por capacidades).
  - En GM, la dosis 4D sigue sin reducirse ante desmotivación: V24 lo rechaza (`soloFatiga`) y el bloque DOSIS del prompt lo dice.
  - Nunca las dos cosas en la misma sesión.
- **Regla de V8C** (`revisarSustitucionPorDesmotivacion`). Exige:
  - exactamente 1 añadido y 1 retirado;
  - ningún cambio de dosis;
  - el sustituto en el conjunto elegible (bloqueados y exclusiones respetados);
  - mismo patrón, mismo slot 4C (`MandatorySlot`) y mismo grupo (`bodyPart`);
  - músculo objetivo compatible;
  - dificultad no superior;
  - `!ImpactoPolicy.sube(original, sustituto)`. Con el impacto desconocido no se admite ninguno, por prudencia.
- Cada incumplimiento da un motivo explícito. Además, se rechaza tocar una sesión no señalada (lo hace `SessionFixApplier`).
- **Coherencia:**
  - Prompt (`SessionFixPromptBuilder`): instrucción de `PermisosIntervencion.instruccion` y lista de sustitutos filtrada con `equivalenteEstricto` y la cabecera «COMO MÁXIMO UNO».
  - Factibilidad previa (`FactibilidadAdaptacion`): la misma lista estricta. Los bloques B y E del texto V2/V2.1 dicen «COMO MÁXIMO UN ejercicio … no cambies ninguna dosis».
  - Con la histórica todo es como antes (huellas SHA-256 de 18 prompts: U03, U04, U08, U09, U18 y U19 × v1, v2 y v2.1).
- **`FactibilidadPreviaIA`** solo trata `ACORTAR_O_REORGANIZAR` (tiempo). P-A1 y P-A2 no la afectan: no se toca.

## 4. Cambios por fichero

| Fichero | Cambio |
|---|---|
| `IntentoDeLinaje` (nuevo) | H6: sigue el intento de la versión `SAFETY_REPAIR` por el plan padre si la sesión gemela es idéntica |
| `DosisLinajeResolver`, `ReduccionAutorizadaResolver`, `ProgresionResolver` | Usan `IntentoDeLinaje`. H7 en RAR. `fatigaConsecutiva` |
| `PreviousWeekAssembler` | P-P1 (`dosisHeredadas` siempre que haya resolutor) y `fatigaConsecutiva` |
| `PublishedPlanSafetyRepairService` | P-P2: `validateContinuidad(..., null, true)` |
| `AdaptedWeekProducer` | P-P1 (`autorizarDosis = true`). La política llega a V8C en la vía de corrección de la IA |
| `ConservationValidator` | P-A1 (`MANTENIMIENTO` y avisos), P-A2, P-C2 y H7 |
| `ProgresionDosisPolicy` | `admite4D(..., fatigaGM)` y `mantenimiento4D` |
| `PlanDraftValidator` | Solo las 2 llamadas de V24 a `admite4D` pasan `fatigaGM` |
| `DosisAutorizada` | `Origen.MANTENIMIENTO` |
| `PreviousWeekSummary`, `ConservationReport` | Campos `fatigaConsecutiva` y `avisos`, con constructores de compatibilidad |
| `AdaptedWeekCommandService` | Solo RECUPERACION con REV2 y la progresión apagada |
| `PermisosIntervencion` (nuevo), `SessionFixPromptBuilder`, `FactibilidadAdaptacion` | Prompt y factibilidad coherentes con P-A1 y P-A2 (solo REV2) |
| `docs/FitSense/DISENO_PROGRESION_Y_REINCORPORACION.md`, `IMPLEMENTACION_PROGRESION_V34.md` | Nota fechada al final, sin reescribir el historial |

## 5. Defectos

### 5.1 P-P1 / H4 · con la progresión apagada no se heredaban las dosis con linaje
- **Reproducido:** sí. La prueba `ProgresionV24V8CTest.apagadoNoHereda` fijaba el rechazo de una copia progresada con el interruptor apagado.
- **Corregido** en todas las políticas: `PreviousWeekAssembler` siempre resuelve el linaje y `AdaptedWeekProducer` siempre emite `HEREDADA`. Las autorizaciones nuevas (`PASO`) siguen exigiendo la progresión.
- **Pruebas:** `apagadoHereda` (positiva), `apagadoSinLinajeRechazada` (negativa: sin linaje, V24 rechaza igual).

### 5.2 P-P2 · la reparación de seguridad no emitía autorizaciones
- **Reproducido en IT:** con el arreglo revertido, `ReparacionLinajeIT` da `NO_VALIDA` («V24: el ejercicio 1007 … lleva 3x11 … 4D v2 exige 3x10»).
- **Corregido** en todas las políticas. Con el arreglo: `REPARADA`, el 1374 sustituido por el 57 y la sesión progresada intacta.

### 5.3 H6 · la versión reparada cortaba el linaje
- **Reproducido en IT:** con el arreglo revertido, el ciclo siguiente queda `NO_FACTIBLE` («V24: el ejercicio 1007 del 2026-11-30 lleva 3x11…»).
- **Corregido** en todas las políticas. Con el arreglo: `SIN_ORDEN` y la copia hereda 3×11.
- **Garantías:**
  - Solo se sigue al padre si la sesión es **idéntica** (ejercicios, dosis y minutos). Una sesión cambiada por la reparación corta el linaje, como antes.
  - Se aplica a los tres resolutores (DLR, RAR y ProgresionResolver).

### 5.4 H7 · una VUELTA_ATRAS perdía la exención de V20
- **Reproducido:** sí. La prueba `dp7VueltaAtrasNoHereda` fijaba que se perdía, y V20 rechazaba justo la vuelta atrás que D-P6 ordena ante fatiga.
- **Corregido** en todas las políticas, en V8C (la `VUELTA_ATRAS` cuenta como «no reduce») y en RAR (se sigue subiendo por el linaje).
- **Pruebas:** `dp7VueltaAtrasConservaExencion` y la negativa `vueltaAtrasSinReduccionNoGanaExencion` (sin reducción autorizada previa no se gana ninguna exención).

### 5.5 H14 · cambio de objetivo tras un paso
- **No reproducible por la API:** el perfil queda bloqueado tras el primer plan.
- **Prueba unitaria** (`progresionGmConOtroObjetivo`, histórica y REV2): una progresión 4D (3×11/90 s) con linaje, revalidada con `LOSE_WEIGHT` y la V24 por capacidades activa, se **rechaza**. La autorización `HEREDADA` no salva una dosis que no está en el rango del objetivo vigente. En GM la misma copia se acepta.
- **Sin cambio de código.**
- **Riesgo general (no específico de H14):** con `capability-validation` apagada (por defecto), los objetivos por capacidades no validan ninguna dosis.

### 5.6 Límites de la reincorporación (H9): analizados, no corregidos
- `ProgresionResolver.retirado` (líneas ~400-417) solo reincorpora cuando un `ACORTAR` retiró **exactamente un** relleno, sin nada añadido ni sustituido:
  - **Varios retirados en un `ACORTAR`:** no vuelve ninguno (`quitados.size() != 1`).
  - **Acortamientos consecutivos:** vuelve solo el más reciente. Cuando ya volvió, `retirado` devuelve `null` y no sigue buscando más atrás, así que el retirado más antiguo no vuelve nunca.
- Es la regla D-P8 («uno solo»), conservadora y documentada, no un error de implementación. Cambiarla afectaría a la reserva R2 y a todas las políticas.
- **Propuesta (no implementada):** con REV2, seguir buscando más atrás tras un retirado ya devuelto, de uno en uno y dentro de la reserva R2.
- No hay IT ejecutada para este punto. Es un análisis del código.

### 5.7 100 % frente a 80 %
- Nota fechada (05/11/2026) al final de los dos documentos:
  - la progresión ordinaria exige **100 % real** desde `63b5169`;
  - la recuperación y la reincorporación siguen con el **80 %**.
- El texto anterior no se reescribe.

## 6. Resultados

| Bloque | Resultado |
|---|---|
| Unitarias completas (`mvn test`) | **1.248 ejecutadas, 1 fallo, 1 omitida.** El fallo, `PoliticaPlanificacionTest.catalogo` (espera 2 políticas y hay 5), **ya existe en `be825f9`** (el andamiaje añadió DUR-A, DUR-B y REV2). No lo toco: es del padre |
| `RedisenoAdaptacionTest` | 17/17 |
| `PromptRev2Test` | 5/5 (REV2: INVESTIGAR ofrece una sustitucion; con impacto desconocido, ninguna; la lista solo trae equivalentes estrictos; los bloques B y E coherentes; REVISAR en GM solo ofrece 3x10 -> 2x10; GENERAL_FITNESS sin cambios) |
| `PromptHistoricoHuellaTest` | 18/18 huellas idénticas a `be825f9` |
| `ProgresionV24V8CTest` | 18/18 (2 pruebas convertidas al comportamiento nuevo y 2 negativas nuevas) |
| `ReduccionAutorizadaResolverTest`, `Reincorporacion4DTest`, `ReincorporacionTrasProgresionTest`, `ConservationValidator*Test`, `AdaptedWeekProducerTest`, `SessionFixPrompt*Test`, `FactibilidadPreviaIATest` | 9, 5, 7, 49, 34, 50 y 7 ejecutadas, 0 fallos |
| `RedisenoFatigaGmIT` (REV2, progresión apagada) | 2/2. Mantenimiento `APLICADA`; recuperación en las semanas 4, 6 y 8 hasta la dosis de antes; 0 pasos `PROGRESION`; avisos `[-, REPETIDA, PERSISTENTE]`; nunca menos de 2 series |
| `ReparacionLinajeIT` | 1/1 con los arreglos. 1 fallo esperado con cada arreglo revertido (P-P2: `NO_VALIDA`; H6: `NO_FACTIBLE`) |
| Regresión IT (Testcontainers, `verify -Pintegration`) | **64/64, 0 fallos**: ProgresionIT 24, CopiaSinIaIT 12, PendientesProgresionIT 6, ProgresionCienPorCienIT 5, ProteccionPlanesPublicadosIT 12, LinajeIntegridadIT 1, GeneracionReferenciaIT 1, ReparacionLinajeIT 1 y RedisenoFatigaGmIT 2 |

## 7. Lo que tiene que hacer el integrador

1. **Hacer cherry-pick, en este orden,** de `35f3274`, `2327463`, `4828145`, `0e8fe26`, `d05a689` y los que se añadan al final (§8). **Saltar `36e6567`**: es la copia de `2d43f16` (ImpactoPolicy), que ya está en tu rama.
2. **`EvaluacionK20IT` (no lo he tocado):**
   - Las líneas ~956 y ~1000 usan `s.intervencion().permiteSustituirEjercicio()`. Para que la sonda vea P-A2 con REV2 deberían usar `PermisosIntervencion.permiteSustituir(s.intervencion(), ctx.policy())`.
   - La línea ~981 (`permiteCambiarDosis`) debería limitar GM con REV2 a la dosis de mantenimiento: `PermisosIntervencion.mantenimientoGM(...)`, solo −1 serie en las estructurales de 3 series.
   - Con la histórica no cambia nada.
3. **`PoliticaPlanificacionTest.catalogo`** espera 2 políticas y el catálogo tiene 5 desde `be825f9`.
4. **Decisiones abiertas, todas pendientes de validación profesional:**
   - el ritmo de la recuperación (§2);
   - la exención de V20 para `MANTENIMIENTO` en D8 (§1.2);
   - la reincorporación de varios retirados (§5.6).

## 8. Commits

Rama `rediseno-adaptacion`, en orden:

| SHA | Contenido | Integrar |
|---|---|---|
| `35f3274` | Helper `IntentoDeLinaje` (H6) | Si |
| `36e6567` | Cherry-pick de `2d43f16` (ImpactoPolicy) | **NO**: ya esta en la rama del padre |
| `2327463` | P-A1 y P-A2 en V8C/V24; P-P1, P-P2, H6 y H7; RedisenoAdaptacionTest, PromptHistoricoHuellaTest y ProgresionV24V8CTest | Si |
| `4828145` | PermisosIntervencion, prompt y factibilidad (solo REV2) | Si |
| `0e8fe26` | Notas fechadas en los dos documentos de progresion | Si |
| `d05a689` | RedisenoFatigaGmIT y ReparacionLinajeIT | Si |
| `16613e1` | Bloques B y E de la factibilidad V2/V2.1 coherentes con P-A2 | Si |
