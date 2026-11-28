# 02 · Interacción entre progresión y V24

## 1. Ruta de ejecución y propiedades

| Propiedad | Dónde se lee | Efecto |
|---|---|---|
| `fitsense.adaptation.progression.enabled` (`FITSENSE_PROGRESSION_ENABLED`, por defecto `true`) | `StrictValidationAdapter` | `capacidades = capability-validation.enabled \|\| progresion`: con la progresión encendida, la validación estricta de **todas** las adaptaciones incluye la V24 por capacidades |
| ídem | `AdaptedWeekCommandService`, `PreviousWeekAssembler`, `AdaptationWiringConfiguration` | Pasos de progresión, recuperación y reincorporación en ciclos NO_CHANGE |
| `fitsense.adaptation.capability-validation.enabled` (por defecto `false`) | `StrictValidationAdapter` | La misma V24 por capacidades, sin progresión (D-P11) |
| `fitsense.dose.enforcement`, `fitsense.capability.enforcement` (`ENFORCE`) | `PlanDraftValidator` (bean de generación inicial) | V24/4D y V24 por capacidades en la generación. **No dependen de la progresión** |

**Cadena de una adaptación con IA:**

`AdaptedWeekCommandService` → `AdaptedWeekProducer.produce` → `SessionFixApplier` → `SafetySubstitutionPolicy.reparar` → `ConservationValidator` (V8C) → corrección de la auditoría `subidasPorSustitucion` → `StrictValidationAdapter.revisar` (V1–V27, con 4D siempre y V24 por capacidades si la progresión está encendida).

## 2. Respuestas a las preguntas obligatorias

1. **¿Por qué activar la progresión cambia la validación de una adaptación normal?**
   - Porque `StrictValidationAdapter` construye su validador con la V24 por capacidades en `ENFORCE` cuando la progresión está encendida.
   - El motivo está en el código: «la progresión exige la V24 por capacidades (diseño §4.5): sin ella, una dosis progresada de INCREASE_STRENGTH, LOSE_WEIGHT o GENERAL_FITNESS no la comprobaría nadie».
   - El mismo validador sirve para adaptaciones, progresión, recuperación y reincorporación.
2. **¿Es una política intencional o un acoplamiento accidental?**
   - **Intencional y documentada:** `POLITICA_ADAPTACION_Y_PROGRESION.md` §5 dice «encendido, también activa la V24 por capacidades en la adaptación», y lo recoge el comentario del constructor.
   - Su efecto sobre adaptaciones que no son de progresión es un acoplamiento aceptado por diseño, no un error.
3. **¿Se aplican restricciones de generación inicial a la adaptación?**
   - Se aplica la misma regla de dosis exacta por capacidad.
   - Las autorizaciones propias de la adaptación existen y llegan a V24 a través de V8C:
     - REDUCCION y MANTENIMIENTO para lo que se queda;
     - HEREDADA para lo que no cambia;
     - PASO para la progresión.
   - Un sustituto no recibe autorización: lleva la dosis exacta de su clase. Así lo dice V8C.
4. **¿Una reducción permitida puede ser rechazada por una condición ajena?**
   - **Sí, en un caso concreto:** cuando la IA reduce la dosis de un ejercicio que el backend sustituye después por seguridad, porque el sustituto hereda la dosis reducida sin autorización.
   - Ocurre solo con ejercicios que dolieron, que la IA no debería mantener según `SEGURIDAD_ANTE_DOLOR`.
   - Las reducciones de ejercicios que se quedan (fatiga, dificultad, acortar) siguen autorizadas: en el bloque G, O03 y O14 por fatiga y O04 por dificultad no cambiaron, y en el bloque P tampoco O17, O18 y O19.
5. **¿Distingue el backend entre adaptación con IA, progresión y recuperación?**
   - **Sí:**
     - `produce` lanza una excepción si llega un paso de progresión con una orden de adaptar («con una orden de adaptar no se progresa en el mismo ciclo»);
     - las autorizaciones PASO solo se emiten en continuidad;
     - V8C rechaza cualquier subida de la IA (`DosisIaV8CTest`, 40 casos).
   - La corrección de esta auditoría cierra la única vía encontrada de subida a través de un sustituto.
6. **¿Afecta solo a O12?**
   - En la cohorte O con IA simulada (bloques G y P, con la progresión apagada y encendida), el estado de adaptación solo cambia en **O12**.
   - En O20 (dolor) aparece una candidata más rechazada por V24 (56 válidas frente a 57), sin cambiar el resultado.
   - El mecanismo es general: afecta a cualquier adaptación por dolor en la que la IA reduzca la dosis de un ejercicio excluido.

## 3. Comparación de la cohorte O (IA simulada, sin red)

| Bloque | Semanas | Diferencias de estado de adaptación | Otras diferencias |
|---|---|---|---|
| G (O01–O14) | 52 / 52 | 1: O12 semana 1, APLICADA → SIN_EFECTO (CONSERVED) | 21 pasos de PROGRESION en ciclos NO_CHANGE |
| P (O15–O20) | 72 / 51 | 0 | 35 PROGRESION, 2 REINCORPORACION (O18 y O19, solo con progresión) y RECUPERACION igual. El bloque P termina antes con la progresión encendida porque el horizonte acaba al reincorporar |

**Oráculo independiente v1.4:**
- **A10:** los «FALLO» (6 con la progresión apagada, 13 encendida) son del protocolo congelado del oráculo, que exige el bloque G sin progresión y el bloque P con ella. No son fallos del sistema.
- **A7:** 2 «FALLO» con la progresión encendida, en O06 y O10: dos semanas al 100 % sin paso ni bloqueo registrado.
  - El backend no genera candidatos. Solo progresan ejercicios de series y repeticiones con dosis base reconocida y con incrementos de ≤ 5 % de la sesión, y la política dice «no se fuerza».
  - Pero no registra que no hubo candidato.
  - Es una laguna de trazabilidad, sin efecto en la seguridad; la causa exacta por ejercicio no se ha desglosado (ver `07`).

## 4. Garantías sobre la generación inicial

- **El validador de generación** es el bean `PlanDraftValidator`, configurado solo con `fitsense.coverage/dose/capability.enforcement`. No lee la progresión.
- **V24 en la generación** se llama con `ConservationReport.vacio()`: sin autorizaciones y con dosis exactas.
- **La corrección de la auditoría** está en `AdaptedWeekProducer`, en el camino de corrección de la IA. No toca `PlanDraftValidator`, `CapabilityPrescriptionPolicy`, `MvpDosePolicy` ni los generadores.
- **Pruebas que siguen verdes:**
  - `ConfiguracionEvaluadaIT`: los cuatro objetivos con PLAN-REV-2.0;
  - `ProgresionV24V8CTest`: «la generación, intacta»;
  - las referencias de generación y la regresión completa (`05_REGRESIONES.md`).

## 5. Las autorizaciones de progresión no se usan en la adaptación

| Garantía | Prueba |
|---|---|
| La IA no sube series, repeticiones ni segundos, con o sin interruptores | `DosisIaV8CTest` (40) |
| Solo la dosis autorizada y en rango; la generación, intacta | `ProgresionV24V8CTest` (18) |
| Correcciones de la IA real con la progresión encendida | `CorreccionesReplicateEncendidoIT` (5) |
| Una subida de la IA a través de un sustituto se rechaza, con la progresión apagada y encendida | `O12DolorProgresion{Apagada,Encendida}IT.subeSeries` (nuevas) |
