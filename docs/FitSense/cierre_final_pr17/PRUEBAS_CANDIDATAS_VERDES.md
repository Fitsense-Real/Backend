# Pruebas con las políticas candidatas (verdes)

## 1. La aserción histórica, parametrizada por política

**Prueba:** `PoliticasP123IT.cuatroObjetivosEnLosLimites` (16 casos: 4 objetivos × 30, 45, 60 y 75 min).

**Antes de esta orden:** exigía que ninguna sesión inicial durara menos del piso de V20, sin mirar la política. Con C2-D fallaba el caso [13] (GAIN_MUSCLE, 75 min), porque C2-D publica a propósito sesiones bajo el piso cuando la saturación está demostrada.

**Ahora** la prueba lee la versión activa (`PlanningPolicySelector.activa()`) y comprueba:

| Política | Qué se exige |
|---|---|
| **Todas** | El plan guarda **su** versión (`planning_policy` = la activa) |
| | El piso del generador **sigue en el 70 %** (`calculation_configs`) |
| | Ninguna sesión por encima del techo de V4 |
| **PLAN-HIST-1** | Ninguna sesión bajo el piso. Una sesión corta hace fallar la prueba («sin C2-D»): **protege la versión histórica igual que antes** |
| **Con C2-D** | Una sesión bajo el piso solo se admite si la **traza del plan** registra `excepcion_c2d=true` en esa fecha (saturación demostrada y registrada) |
| **Con EF** | Además, ningún PULL con trabajo resistido sin HORIZONTAL_PULL ni VERTICAL_PULL, ningún empuje en PULL y ninguna tracción en PUSH (`sinFalsaCobertura`) |

**No se ha eliminado, omitido ni relajado** para la histórica.

**Evidencia de la ejecución (GAIN_MUSCLE 75 min):**

| Política | Resultado |
|---|---|
| PLAN-HIST-1 | No genera plan (V20), como antes |
| PLAN-C2D-0.1 | `PLAN 13 excepciones_c2d=[2026-11-02, 2026-11-04, 2026-11-06]` |
| PLAN-C2D-EF-0.1 | `PLAN 13 excepciones_c2d=[2026-11-02, 2026-11-04, 2026-11-06]`, y la comprobación de EF pasa |

## 2. Baterías con IA simulada (15 clases)

**Las mismas que en la orden anterior:**
- `AdaptacionCompletaIT`, `AdaptacionPorCausasIT`, `AdaptacionRegresionIT`, `HerenciaD8IT`, `PoliticasP123IT`;
- `ProgresionIT`, `ProgresionCienPorCienIT`, `ProteccionPlanesPublicadosIT`, `CicloConcurrenteIT`, `LinajeIntegridadIT`;
- `DuracionForzadaIT`, `ValidacionMultisemanaIT`, `AdherenciaIT`, `CorreccionesReplicateEncendidoIT`, `ContinuidadSemanalIT`.

Cubren **D8, ACORTAR, C-2, P-02, P-03**, progresión, reincorporación, concurrencia y linaje. Dobles de IA, 0 llamadas a Replicate.

| Política | Contabilizadas | Ejecutadas | Aprobadas | Fallidas | Errores | Omitidas |
|---|---|---|---|---|---|---|
| PLAN-HIST-1 (por defecto) | 225 | 223 | **223** | **0** | **0** | 2 |
| PLAN-C2D-0.1 | 225 | 223 | **223** | **0** | **0** | 2 |
| PLAN-C2D-EF-0.1 | 225 | 223 | **223** | **0** | **0** | 2 |

**Las 2 omitidas** son de `ValidacionMultisemanaIT` y se omiten en todas las políticas, también en la regresión por defecto. No dependen de la política.

**Cada arranque registra la versión activa** («POLITICA_PLANIFICACION candidata ACTIVA: …»), comprobado en los tres registros.

**Incidencia durante el trabajo:**
- El primer intento con C2-D-EF dio **12 errores** en `PoliticasP123IT`.
- **Causa:** la comprobación nueva de EF construía un `Set.of(...)` con patrones repetidos de una misma sesión (`IllegalArgumentException: duplicate element`).
- **Era un fallo de la prueba, no del motor.** Corregida (`HashSet`) y repetida: 0 errores.
- El registro del primer intento se conserva como evidencia.

## 3. Pruebas nuevas de esta orden

| Prueba | Tipo | Qué comprueba |
|---|---|---|
| `DiagnosticoNoFactibleTest` (3) | Unitaria | Diagnóstico solo con VEF NO_FACTIBLE; sin texto técnico; material solo cuando está demostrado |
| `NoFactibleApiEstructuradoIT` | Integración | Con el indicador encendido y C2-D-EF: 422 con `diagnosis` PULL_NO_DISPONIBLE |
| `NoFactibleApiApagadoIT` | Integración | Apagado: 422 de siempre, sin `diagnosis` |
| `CierrePR17LabIT` (7) | Laboratorio opcional | 48 alternativas, reparación por sustitución, EF × LW, uso de ejercicios, familias y muestra ciega |
| `CicloRealC2DEFLW02IT` | Laboratorio opcional | Ciclo real con C2-D-EF y LW-PREF-0.2: traza `opciones` |
