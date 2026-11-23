# 06 · Pruebas automatizadas y CI

## 1. Pruebas nuevas o ajustadas

Todas son unitarias, sin red y sin Replicate.

| Prueba | Casos | Qué demuestra |
|---|---:|---|
| `ReplicateGpt5MiniDirectoTest` | 8 | La entrada `direct` lleva **exactamente** `system_prompt`, `prompt`, `reasoning_effort`, `verbosity` y `max_completion_tokens`, sin `model` ni `json_schema`. El esquema del `system_prompt` es el mismo de `json_schema` y es determinista, por la huella V59. La salida fragmentada se une en orden. Un texto que no es JSON, una respuesta vacía o un JSON cortado se **rechazan**. `REPLICATE_CONSUMO` aparece con y sin tokens, nunca con el prompt ni con la credencial. Valores por omisión, deducción y forzado del formato, y acotado del tope. Etiqueta del modelo por el endpoint |
| `ReplicateSchemaRoutingTest` | 6 (+1) | Generación y adaptación llevan su propio esquema en `direct` y en `structured`, y cada una se registra con su etiqueta |
| `ControlPresupuestoK20Test` | 9 (+2) | Política `COTA_POR_TOPE` en todos los estados del proveedor. Cota ausente o mayor que la reserva. Medición por tokens. Las demás políticas no cambian. Lectura de las claves de `REPLICATE_CONSUMO` |

**Sin cambios:** el resto de las pruebas del cliente de Replicate (cancelación R-2, límites HTTP R-3, respuestas inválidas R-1, libro V59, sondeo, instrumentación), que pasan con el nuevo formato por omisión.

## 2. Regresión completa

Ejecutada con `mvn verify -Pintegration`, la misma orden que el CI.

| Ejecución | Resultado |
|---|---|
| Unitarias (surefire) sobre `f53b3b8a` | **1.609 ejecutadas, 0 fallos, 0 errores**, 1 omitida (antes: 1.598) |
| Integración (failsafe, PostgreSQL 16 de Testcontainers) sobre `f53b3b8a` | **867 ejecutadas, 0 fallos, 0 errores**, 72 omitidas (opt-in). **BUILD SUCCESS** (30 min 57 s) |
| CI de GitHub (`verify`) sobre el commit final de la PR | {{CI}} |

Las pruebas no llaman a Replicate. Surefire y failsafe apagan la IA, y las ITs con IA real son opt-in y quedan omitidas. Tampoco llaman a Google: el transporte de FCM está simulado.

El commit `e35eabcb`, posterior, solo cambia el valor por omisión de `fitsense.ai.reasoning-effort`, de `low` a `medium`, y documentación. Las pruebas no dependen de ese valor: las que lo usan lo fijan ellas mismas. El CI lo verifica sobre el commit final.

**Cubiertos en esta regresión, sin cambios en las pruebas:**
- **Los cuatro objetivos:** `GeneracionPorObjetivoIT` y `FactibilidadPorObjetivoIT`, más las suites de adaptación con GAIN_MUSCLE, LOSE_WEIGHT, INCREASE_STRENGTH y GENERAL_FITNESS.
- **Adaptación ante baja adherencia:** `AdherenciaIT`, `AdherenciaRiesgoIT`, `AdaptacionPorCausasIT`, `AdaptacionCompletaIT` y `AdaptacionRegresionIT`.
- **Progresión:** `ProgresionIT`, `ProgresionRev2IT`, `ProgresionReservaIT`, `ProgresionCienPorCienIT`, `ProgresionFactibilidadIT`, `PendientesProgresion*IT` y `EvaluacionLimpiaProgresionIT`.
- **Reincorporación:** `O12DolorProgresion{Apagada,Encendida}IT` y la reincorporación de `CicloCompletoHttpIT` y `CopiaSinIa*IT`.
- **V24:** `EvaluacionDirectaV24IT`, `EvaluacionLecturaV24IT` y `Adenda4Dv32IT`.
- **V8C:** las suites de adaptación con V8C.
- **Integridad de los planes publicados:** `ProteccionPlanesPublicadosIT` y `LinajeIntegridad{,Rev2}IT`.
- **Compatibilidad con Mobile:**
  - `ContratoErroresApiIT` y `ContratoPushHttpIT`;
  - `CicloCompletoHttpIT`;
  - el recurso `WeeklyTrainingPlanResource` no cambia, y `modelName` sigue valiendo `gpt-5-mini`.
- **Notificaciones de Firebase:** `NotificacionesPushIT`, `ContratoPushHttpIT` y `ArranquePush*IT`.

## 3. Pruebas reales (no automatizadas en CI)

Son las de `03` y `04`.
- **Comando:** `evidencia/correr_G5M.sh`, sobre el arnés `EvaluacionK20IT`. Es opt-in y exige autorización expresa en la línea de órdenes.
- **Llamadas:** 102 predicciones del arnés, todas con `REPLICATE_TIMING … resultado=SUCCEEDED`.
