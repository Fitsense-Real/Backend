# 05 · Regresiones, presupuesto, incidencias y pendientes

## 1. Regresiones (entregable 5)

| Ejecución | Commit | Resultado |
|---|---|---|
| `mvn verify -Pintegration` completo, local | `46490225` (#83 con las pruebas nuevas; el resto son commits de documentación) | Unitarias: **1.609, 0 fallos**, 1 omitida. Integración: **869, 0 fallos**, 72 omitidas (opt-in). **BUILD SUCCESS** (29 min 51 s) |
| Pruebas nuevas o ajustadas en esta tarea | `dc541074` | `DuracionSinSeriesHttpIT` (O04 y el contrato de Mobile), `O06LimiteDuracionIT`, `ReanudacionConReplicateSimuladoIT` en formato directo (12) y `GeneracionAsincronaResilienciaIT` (23): **0 fallos** |
| Rama de evaluación `eval/h2-gpt5-mini` (#69 + #83) | `e0128cef`+ | Pruebas de H2 (`ContratoV15PromptTest`, `RepartoV15Test`, `ContratoPromptV15IT`) y de G5M (`ReplicateGpt5MiniDirectoTest`, `ReplicateSchemaRoutingTest`, `PromptRev2Test`): **29, 0 fallos** |
| CI de GitHub (`verify`) en #83 | `f46f48fe` (pruebas nuevas e informe) | **Verde** ([run 45274221577](https://github.com/Fitsense-Real/Backend/actions/runs/45274221577)). Los commits posteriores son solo de documentación |

**Cobertura de la lista de la orden** (todas dentro de la regresión completa; nombres en `migracion_gpt5_mini/06` §2):

| Área | Pruebas |
|---|---|
| Generación inicial de los cuatro objetivos | `GeneracionPorObjetivoIT`, `FactibilidadPorObjetivoIT` |
| V8C, V15, V20, V24 | Suites de adaptación con V8C, `EvaluacionDirectaV24IT`, `EvaluacionLecturaV24IT`, `Adenda4Dv32IT` y las pruebas de V15/V20 del validador |
| Duración máxima | `V4CValidadorRealTest`, `DuracionForzadaPlan60IT` y la nueva **`O06LimiteDuracionIT`** |
| Restricciones y bloqueados | `ProteccionPlanesPublicadosIT` y las pruebas de catálogo funcional |
| Baja adherencia y exceso de esfuerzo | `AdherenciaIT`, `AdherenciaRiesgoIT`, `AdaptacionPorCausasIT`, `AdaptacionCompletaIT` |
| Progresión gradual, reincorporación condicionada y no restauración | `Progresion*IT`, `PendientesProgresion*IT`, `O12DolorProgresion{Apagada,Encendida}IT`, `CopiaSinIa*IT` |
| Integridad de los planes publicados | `LinajeIntegridad{,Rev2}IT`, `ProteccionPlanesPublicadosIT` |
| Contratos de Mobile | `CicloCompletoHttpIT`, `ContratoErroresApiIT`, `ContratoPushHttpIT` y la nueva **`DuracionSinSeriesHttpIT`** |
| Errores y uso del respaldo | `ReplicateRespuestasInvalidasTest`, `GeneracionAsincronaResilienciaIT` (R07, R11, R12) |
| Firebase | `NotificacionesPushIT`, `ArranquePush*IT`, `ArranqueFirebaseTest` |

### O06: tiempo solicitado frente a tiempo autorizado

`O06LimiteDuracionIT` (nueva) fija tres magnitudes distintas:
- **solicitado:** 35 min, admitido por P-01 para LOSE_WEIGHT (30–75);
- **autorizado:** `limiteV4SegundosDe(35)` = **2.100 s** con PLAN-REV-2.0, sin tolerancia (la histórica daba 2.415 s). Es lo que llega a la IA como `max_session_seconds`;
- **publicado:** cada sesión ≤ 35 min, y el plan publicado pasa V4.

**En las corridas reales con gpt-5-mini:**
- O06 publicó sesiones de 29 min;
- tras la adaptación por dolor, 16–19 min (D8 permite acortar);
- **nunca** por encima de lo autorizado.

**No se modificó ninguna regla.**

## 2. Presupuesto (entregable 6)

La reserva es de USD 8 para toda la evaluación de gpt-5-mini y **no se amplió**.

gpt-5-mini no informa tokens, así que se distinguen tres cifras:
- **cota** (reserva conservadora): bytes de entrada como tokens + 256 + 16.000 de salida, a 0,25 y 2,00 USD por millón;
- **mínimo estimado** (sin razonamiento);
- **facturado**, que solo se ve en el panel de Replicate y no está verificado desde la sesión.

| Concepto | Predicciones | Cota cargada (USD) |
|---|---:|---:|
| Sondas del diagnóstico de E002 (23/11) | 3 | ≈ 0,00 |
| Evaluación G5M (corridas del arnés, `migracion_gpt5_mini/04`) | 102 | 6,5362 |
| H2 fase 1 (8 parejas; 2 recuperadas por GET) | 16 | 0,9361 |
| H2 fase 2 (5 parejas) | 10 | 0,4715 |
| Consultas GET de métricas y recuperación | — | 0 (gratuitas) |
| **Total** | **131** | **7,9438** |
| **Saldo de la reserva** | | **0,0562** |

- **Mínimo estimado de G5M:** USD 1,18. El de H2, con la misma razón de 3,44 caracteres por token, es del orden de USD 0,4. Sin razonamiento: el gasto real está entre ese mínimo y la cota.
- **Para seguir** (por ejemplo, la evaluación de H2-v2 con el pipeline completo, `01` §6) hace falta **otra autorización de presupuesto**. Con el saldo no cabe ni una pareja.
- La cota **no** es gasto real y no se presenta como tal.

## 3. Incidencias de esta tarea

| # | Incidencia | Efecto | Tratamiento |
|---|---|---|---|
| I-4 | **Reinicio del contenedor** durante la corrida real de H2: se perdieron el proceso, la corrida de regresión y los ficheros que el arnés escribe al final | Ninguna predicción pagada se perdió ni se repitió | Las 14 salidas ya guardadas se revalidaron sin coste (modo `VALIDAR`), idénticas a lo registrado en vivo. La regresión se relanzó |
| I-5 | **Dos POST con cuerpo ilegible** (`Error while extracting response … application/octet-stream`, a los 12–15 s). El backend las registró `POST_ERROR` | **Replicate sí creó y facturó las dos predicciones**, y terminaron `succeeded`. El backend nunca recibió su salida | Se recuperaron por su id (GET, sin coste) y se comprobó su entrada. **En producción**, en la generación síncrona (sin el libro V59), el pipeline lo trata como fallo del proveedor y **pide otra predicción**: se pagan dos. En la asíncrona, el libro V59 la marca `NO_CONFIRMADA` y no reenvía. Es el riesgo ya documentado en `generacion_asincrona/RIESGO_COSTOS_DUPLICADOS.md`. La app actual de Mobile (rama `generation`) usa la asíncrona |

## 4. Pendientes que impiden autorizar el merge (entregable 8)

| # | Pendiente | Bloquea | Quién decide |
|---|---|---|---|
| P-1 | **Seguridad de `SUST-SEG-1` tras dolor:** puede sustituir el ejercicio que dolió por una variante del mismo gesto («front raise» → «front raise v. 2»). Afecta a 25 familias del catálogo. **No es una infracción de la política vigente, pero no podemos declarar «sin sustituciones inseguras»** (`02` O20 §5, opciones A, B y C) | **Sí** (condición de cierre: «no existan sustituciones inseguras conocidas») | Responsable del proyecto |
| P-2 | **Mobile D-M1:** el cumplimiento de los DURATION con varias series cuenta 1/N con «Lo completé». Es un defecto de Mobile, anterior a G5M, que afecta a la adherencia y a las adaptaciones (`04`) | No bloquea #83 (backend), pero sí la validez de la adherencia en producción | Equipo Mobile |
| P-3 | **H2:** desfavorable con estos criterios. Se mantiene desactivada y hace falta H2-v2 con V26 y otra evaluación (`01`) | No bloquea #83 (H2 no está en #83) | Responsable |
| P-4 | **Mobile D-M2:** publicar con el cliente asíncrono (rama `generation`). La rama `main` (síncrona, 300 s) tiene un margen de 7 s sobre el máximo medido | No bloquea #83 | Equipo Mobile |
| P-5 | **Gasto real de Replicate** sin verificar (solo la cota) | No | Comprobar en el panel de facturación |
| P-6 | Oráculo v1.5 (propuestas O04 y O20). v1.4 se conserva congelado | No | Responsable de la evaluación |
