# Pruebas con IA simulada y C2-D activo (D8, ACORTAR, C-2, P-02, P-03)

**Método:**
- Las baterías de integración existentes que ejercitan la adaptación con **IA simulada** (dobles de Mockito, `fitsense.ai.enabled=false`) se ejecutaron **dos veces más**, con la política candidata activa en todo el contexto de Spring:
  - `-Dfitsense.planning.policy=PLAN-C2D-0.1 -Dfitsense.planning.policy.allow-candidates=true`
  - `-Dfitsense.planning.policy=PLAN-C2D-EF-0.1 -Dfitsense.planning.policy.allow-candidates=true`
- Cada arranque registra `POLITICA_PLANIFICACION candidata ACTIVA: <versión>`, comprobado en los dos registros.
- **0 llamadas a Replicate.** `A01AdaptacionRealIT`, que usa IA real, se excluyó.

## 1. Baterías y resultado

| Batería | Cubre | PLAN-C2D-0.1 | PLAN-C2D-EF-0.1 |
|---|---|---|---|
| `AdaptacionCompletaIT` (22) | Flujo de adaptación: D8 (REDUCE_VOLUME), ACORTAR, publicación conservada, respaldo, cortacircuitos, IA corrige sesiones | 22/22 | 22/22 |
| `AdaptacionPorCausasIT` (11) | Falta de tiempo con contexto (A01), ACORTAR, sin IA cuando no hay nada que pedir | 11/11 | 11/11 |
| `HerenciaD8IT` (3) | D8 y su herencia limitada (C-2); el dolor no la aprovecha | 3/3 | 3/3 |
| `PoliticasP123IT` (39) | P-01, **P-02** (memoria del dolor, reactivación) y **P-03** (regeneración segura) | **38/39** | **38/39** |
| `ProgresionIT` (24), `ProgresionCienPorCienIT` (5) | Progresión y sus límites; sin progresión tras fatiga, dolor o baja adherencia | 29/29 | 29/29 |
| `CorreccionesReplicateEncendidoIT` (5) | Correcciones de la IA (simuladas) con progresión encendida | 5/5 | 5/5 |
| `ValidacionMultisemanaIT` (57, 2 omitidas por diseño) | Escenarios humanos multisemana en los cuatro objetivos | 55 + 2 omitidas | 55 + 2 omitidas |
| `CicloConcurrenteIT` (9) | Concurrencia (ver `PRUEBAS_CONCURRENCIA_C2D.md`) | 9/9 | 9/9 |
| `LinajeIntegridadIT` (1), `ContinuidadSemanalIT` (2), `DuracionForzadaIT` (4), `ProteccionPlanesPublicadosIT` (subclases), `AdaptacionRegresionIT` (subclases), `AdherenciaIT` (subclases) | Linaje, continuidad, ACORTAR forzado, reparación de publicados, regresión de la adaptación, adherencia | Todas pasan | Todas pasan |
| **Total** | | **225: 222 pasan, 1 falla, 2 omitidas** | **225: 222 pasan, 1 falla, 2 omitidas** |

## 2. La única diferencia (esperada)

`PoliticasP123IT.cuatroObjetivosEnLosLimites[13]` (GAIN_MUSCLE, intermedio, gimnasio, 75 min):

> «sesión inicial de 43 min bajo el piso 53»

- La prueba fija la semántica **histórica**: ninguna sesión inicial por debajo del piso de V20.
- Con C2-D, GAIN_MUSCLE a 75 min se publica al 56,7 % **por la excepción de saturación**: es el comportamiento que define C2-D (60 de 60 en la matriz).
- **No es un defecto.** No se ha cambiado la prueba, que sigue siendo correcta para la política por defecto.
- Antes de activar C2-D habría que parametrizar esa aserción por versión.

## 3. Coherencia entre lo que recibe la IA y lo que valida el backend

| Regla | Prueba |
|---|---|
| V4-C: `max_session_seconds` y la frase del techo, solo con V4-C | `ContratoV4CIATest.snapshot` y `textoDelPrompt` |
| **EF**: la regla de patrones llega a la IA **solo** con una versión EF, idéntica a la que aplica VEF; el JSON no cambia | `ContratoV4CIATest.elegibilidadFuncionalSoloConEF`; `ElegibilidadFuncionalTest.reglaDeLaIACoincideConElValidador` (cada patrón de cada familia está en el texto) |
| C2-D: no cambia lo que ve la IA | `ContratoV4CIATest.snapshot` (historia = C2-D) |
| Corrección de sesiones (`SessionFixPromptBuilder`) con EF | Mismo texto `reglaParaIA()` |

## 4. Escenarios de la orden

| Escenario | Cubierto por | Resultado con C2-D / C2-D-EF |
|---|---|---|
| D8 | `AdaptacionCompletaIT`, `HerenciaD8IT` | Pasa: la reducción se publica con su autorización (V8C) y C2-D no interviene (`aplicaC2D` exige conservación vacía) |
| ACORTAR | `AdaptacionPorCausasIT`, `DuracionForzadaIT` y el ciclo real (S4) | Pasa: se registra y conserva |
| C-2 | `HerenciaD8IT` | Pasa |
| P-02 | `PoliticasP123IT` y el ciclo real (S3, S7) | Pasa: 0 ejercicios excluidos reaparecen; la reactivación devuelve el candidato |
| P-03 | `PoliticasP123IT` y el ciclo real (L6 al 40,1 %) | Pasa. L7 S4 sigue NO_FACTIBLE (sube el trabajo) |
| Fatiga | Ciclo real (S5, RPE 9) y `ProgresionIT` | Pasa: sin subida |
| Dolor | Ciclo real (S3) y P-02 | Pasa |
| Baja adherencia | Ciclo real (S6) y `ProgresionIT` | Pasa: sin subida |
| Conservación | `AdaptacionCompletaIT` (sesión conservada) | Pasa |
| Progresión | `ProgresionIT`, `ProgresionCienPorCienIT` | Pasa |
| Reincorporación | Ciclo real (S7) | Pasa |

**Limitación:** la IA simulada devuelve respuestas fijas de los dobles. **No mide** cómo respondería un modelo real a la regla de EF. Eso requiere una campaña con IA real, que esta orden prohíbe.
