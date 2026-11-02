# Verificación de las políticas de adaptación (fases 5 y 6.3)

**Regla central** (sin cambios):

> «FitSense adapta únicamente lo necesario. Si una sesión falla, se corrige esa sesión; si corresponde progresar, se aumenta la dosis autorizada conservando los ejercicios; si no existe motivo de cambio, se mantiene la continuidad. Un respaldo no permite saltarse estas decisiones.»

**Fuentes:**
- **D**: `DiagnosticoRespaldoMultisemanaIT`, con 72 recorridos, 175 ciclos y 525 pares de sesiones en las semanas 26-oct, 2-nov y 9-nov, sobre `generation` dd61837 con las pruebas de laboratorio. Evidencia: `evidencia/diagnostico_respaldo/` (`ciclos.csv`, `sesiones.csv`, `resumen.md`), resumida en `DIAGNOSTICO_RESPALDO_MULTISEMANA.md`. Código ejecutado: rama `lab/reloj-pruebas-opcionales` **bc05ec9**, es decir, `generation` dd61837 más pruebas (Fitsense-Real/Backend#20).
- **R**: la regresión del PR #17 (`REGRESION_PR17_ACTUALIZADO.md`).
- **C**: el código de producción, citado.

| Regla (fase 5) | Qué se comprobó | Fuente | Resultado |
|---|---|---|---|
| **5.1 Baja adherencia**: solo lo necesario | En los 66 ciclos APLICADA y en los 4 RESPALDO, las sesiones **no señaladas** son idénticas (ejercicios, orden, dosis, fecha+7, enfoque, minutos). En «10 varios motivos» (miércoles con dolor y viernes omitido por tiempo), la orden señala solo la peor sesión y el viernes no cambia | D; `AdaptacionPorCausasIT` y `AdaptacionCompletaIT` en R | **Se cumple** (0 incumplimientos) |
| **5.2 Exceso de capacidad**: mismos ejercicios, solo el incremento autorizado | Fuera del laboratorio D (progresión apagada). En R: `ProgresionIT` (24), `ProgresionCienPorCienIT` (5), V34 «un paso, el mismo ejercicio» e idempotencia | R | **Se cumple** (0 fallos) |
| **5.3 Cumplimiento adecuado**: continuidad, sin variedad | 105 ciclos `SIN_ORDEN`: semana **idéntica**. Tras un RESPALDO con sustitución, la semana siguiente conserva el sustituto y no introduce ningún cambio | D; `ContinuidadSemanalIT` en R | **Se cumple** |
| **5.4 Dolor**: sin reincorporación automática; solo las sustituciones autorizadas | Lo que dolió **no vuelve** en ninguna semana posterior. En RESPALDO solo cambia la posición del ejercicio excluido, con SUST-SEG-1: mismo tipo y slot, sin subir series, repeticiones ni segundos, sin subir el trabajo de la sesión, y con la nota y la sustitución en V32. Exclusión ACTIVA en `user_safety_exclusions`. La reincorporación solo ocurre por confirmación explícita (`PoliticasP123IT` y `Reincorporacion4DTest` en R) | D, R | **Se cumple** |
| **5.5 Fallo**: no publicar lo inválido | Los 2 NO_FACTIBLE no publican nada. Un RESPALDO también se revalida entero (V8C y V1-V25: `revisarYCerrar`), así que una semana anterior que dejó de ser válida no se reutiliza | D; C (`AdaptedWeekProducer.revisarYCerrar`) | **Se cumple** |
| 5.5: no eliminar restricciones | La exclusión por dolor se mantiene incluso cuando deja la semana sin publicar | D | **Se cumple** |
| 5.5: no regenerar libremente | El respaldo **no** regenera: copia y sustituye solo lo excluido. **Pero ver la observación sobre P-03 nivel 2** | C | **Se cumple para RESPALDO**; ver la observación |
| 5.5: el respaldo, según las reglas del código | Descrito en `DIAGNOSTICO_RESPALDO_MULTISEMANA.md` §1. No se ha implementado ninguna política nueva | C | Documentado |

## Casos A–E (fase 6.3)

| Caso | Pregunta | Respuesta |
|---|---|---|
| **A**: sin causa autorizada | ¿El respaldo cambia ejercicios por efecto de la semilla? | **No.** Sin orden, la semana es idéntica (105/105). Con orden y sin propuesta usable, el único cambio es el del ejercicio que dolió, elegido de forma **determinista** (orden por zona, patrón, músculo e id; sin aleatoriedad). La semilla solo decide el plan inicial |
| **B**: adaptación localizada | ¿El cambio se limita al alcance autorizado? | **Sí:** 0 cambios en sesiones no señaladas y, en la señalada, solo la posición del ejercicio excluido |
| **C**: exceso de capacidad | ¿Se conservan los ejercicios y solo cambia el volumen autorizado? | **Sí**, en la regresión (`ProgresionIT`, `ProgresionCienPorCienIT`). Con una orden de adaptar no se progresa en el mismo ciclo (`AdaptedWeekProducer`, l. 226-227) |
| **D**: dolor o restricción | ¿Reaparecen los excluidos? ¿Se mantiene la seguridad? | **No reaparecen** y la seguridad se mantiene (§5.4) |
| **E**: no hay plan válido | ¿Se publica algo inválido o arbitrario? | **No.** 2 NO_FACTIBLE en GENERAL_FITNESS: SUST-SEG-1 no encuentra sustituto para 1373 y P-03 nivel 2 se descarta porque subiría los segundos totales (1.140 → 1.260). No se publica nada y el motivo queda en V32 |

## Observación (no es un defecto demostrado; requiere decisión)

**P-03 nivel 2** (`SafeRegenerationService`), aprobado el 31/10/2026 y fusionado con el PR #16:
- tras un NO_FACTIBLE **por dolor**, regenera **la semana entera** con el motor de reglas (dosis base, sin linaje);
- está acotado: sin IA, sin lo excluido, con V1-V25, sin subir repeticiones ni segundos e idempotente;
- **no forma parte de RESPALDO**, pero es una regeneración completa de ejercicios.

La regla 5.5 de esta orden dice «no regenerar libremente todos los ejercicios». **No es libre, porque está acotada,** pero tampoco es localizada: la regla 5.1 dice «sin rediseñar innecesariamente lunes y miércoles».

**No se ha cambiado nada.** Se plantea como decisión D-2 en `DECISIONES_PENDIENTES.md`.
