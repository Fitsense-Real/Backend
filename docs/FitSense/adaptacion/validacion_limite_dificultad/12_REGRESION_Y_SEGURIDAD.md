# 12 · Regresión completa y seguridad

**Código evaluado:** `2385b64`.

Después de este SHA solo cambian:
- pruebas opt-in (laboratorio de los 22 perfiles);
- la base de pruebas del 135, refactorizada y vuelta a ejecutar: 16/16;
- documentación.

Ningún cambio de producción. **CI sobre el SHA final:** ver 13.

**Ejecución:** `mvnw verify -Pintegration` completo (surefire + failsafe), una vez por configuración, con `-Dfitsense.adaptation.difficulty-guard={off,on,preferencia}`. Después, en cada configuración, la comparación de 960 perfiles y la REPLAY de 100 semanas.

Evidencia, en `evidencia/`:

| Fichero | Contenido |
|---|---|
| `regresion_{off,on,preferencia}_pruebas.txt` | recuento por clase |
| `c960_replay_*_pruebas.txt` | 960 perfiles y REPLAY |
| `regresion_resumen.txt` | hashes |
| `regresion_on_fallo_esperado.txt` | el fallo esperado |
| `replay_*.diff` | diferencias de la REPLAY |

## 1. Resultados

Se registran aprobadas, fallidas y omitidas. **Una omitida no cuenta como aprobada.**

| Bloque | off | on | preferencia |
|---|---|---|---|
| Unitarias (surefire) | **1.172 aprobadas**, 0 fallidas, 0 omitidas | 1.172 / 0 / 0 | 1.172 / 0 / 0 |
| Integración (failsafe) | **468 aprobadas**, 0 fallidas, 38 omitidas (de 506) | **467** aprobadas, **1 fallida**, 38 omitidas | 467 / **1** / 38 |
| 960 perfiles (`ComparacionPerfilesIT`, opt-in ejecutado aparte) | hashes de referencia | **idénticos** | **idénticos** |
| REPLAY 100 semanas (`CampanaP123Cohorte02IT`, opt-in ejecutado aparte) | solo marcas de tiempo | solo N08 (09) | igual que on |
| Laboratorio de 22 perfiles (opt-in) | 1/1 | 1/1 | 1/1 |
| 123 sustituciones históricas, 59 sesiones (`H1ImpactoHistoricoTest`, copia temporal) | 59/59 aceptadas | 59/59; 0 cambios | 59/59; 0 cambios |
| Barrido de 19.562 colocaciones (opt-in) | A = referencia (0 diferencias) | B = referencia (0 diferencias) | C: 05 |

**La fallida de on y preferencia es esperada:** `ConservacionPatronH1IT.reglaPorDefecto`, con el mensaje «el limite de dificultad candidato esta APAGADO por defecto ==> expected: <false> but was: <true>».
- La prueba comprueba la **configuración por defecto**, y la corrida la cambia a propósito con `-D`.
- Con la propiedad por defecto (`off`, y en CI) pasa.
- **No se modifica:** es una prueba de referencia, y su fallo bajo `on` demuestra que el `-D` llegó a las pruebas.
- Ninguna otra prueba falla.

**Las 38 omitidas** son laboratorios opt-in: Replicate real, Neon, campañas, barridos y volcados. Varios se ejecutaron aparte en esta validación con su propiedad (960, REPLAY, barrido y 22 perfiles). El resto no se ejecutó y **no se cuenta como aprobado**. Lista completa en `regresion_off_pruebas.txt`, en las líneas con `Skipped`.

Con `off` la **configuración conserva el resultado histórico**: 0 fallos, hashes de referencia y REPLAY sin cambios de contenido. Con `on` y `preferencia` hay **una sola diferencia de comportamiento**, N08, explicada en el 09.

## 2. Cobertura de la lista de la fase 9

| # | Ámbito | Dónde se comprueba | Resultado |
|---|---|---|---|
| 1-2 | Unitarias e integración | §1 | ✔ (la única fallida es la esperada) |
| 3 | Generación inicial | 960 perfiles; `GeneracionReferencia*IT` | ✔ idéntica |
| 4 | V8C | `V8C*Test`, `LimiteDificultad*Test`, `ConservationValidator*Test` | ✔ |
| 5 | H1 | `ConservacionPatronH1IT` (salvo la prueba del valor por defecto bajo -D), `ConservacionPatronH1ConjuntoIT`, `h1SigueActuando` | ✔ |
| 6 | V20 | `V20ComposicionCasosIT`, `DuracionForzada*IT`, TD-13 | ✔ |
| 7 | V24 | `StrictValidationAdapterTest`, `ComposicionE1E2IT`, `V15CambioDeGrupoIT`, TD-14 | ✔ |
| 8 | Dolor | `RespaldoConDolorIT`, `SinSustitutoDolor*IT`, `LimiteDificultadDolorTest`, `AdaptacionCompletaIT` | ✔ |
| 9 | SUST-SEG-1 | `SustitucionDificultadMayorTest`, `LimiteDificultadTest`, `LimiteDificultadDolorTest` | ✔ |
| 10 | RESPALDO | `RespaldoConDolorIT`, `RespaldoCincoSeisDiasIT`, `RespaldoSesentaMinutosIT`, `AdaptedWeekProducerTest` | ✔ |
| 11 | SIN_EFECTO | `AdaptedWeekProducerTest`, `AdaptationOutcomeTest`, `DescripcionAdaptacionTest` | ✔ |
| 12 | NO_FACTIBLE | `AdaptationOutcomeContratoTest`, `AdaptacionRegresionIT`, `ContinuidadSemanalIT`, `SinSustitutoDolorLimiteEncendidoIT` | ✔ |
| 13 | Adherencia | `AdaptationOrderDeciderTest`, `SinAjustePorcentualTest`; REPLAY `metricas.csv` sin cambios | ✔ |
| 14 | Cobertura | `SlotCoverage4CTest`, `WeeklySlotDistributionTest` | ✔ |
| 15 | Adaptación localizada y ampliada | `AdaptacionPorCausasIT`, `WeekDerivationBuilderTest`, `MensajeDeIntervencionTest` | ✔ |
| 16 | Progresión | `ProgresionIT`, `ProgresionReservaIT`, `ProgresionCienPorCienIT` | ✔ |
| 17 | Reincorporación | `Reincorporacion4DTest`, `ReincorporacionTrasProgresionTest` (la IA real, opt-in, omitida) | ✔ |
| 18 | Concurrencia | `CicloConcurrenteIT` | ✔ |
| 19 | Idempotencia | `ProgresionIT`, `PoliticasP123IT` | ✔ |
| 20 | Linaje | `LinajeIntegridadIT`, `HerenciaD8IT` | ✔ |
| 21 | 960 perfiles | §1 | ✔ idéntico |
| 22 | REPLAY 100 semanas | 09 | ✔ solo N08 |

## 3. Seguridad

| Garantía | Con off (hoy) | Con on o preferencia |
|---|---|---|
| Un ejercicio excluido por dolor no se reintroduce | ✔ (03 §3) | ✔ |
| Ningún sustituto por dolor más difícil (IA) | ✘ (N08 lo publicó) | ✔ (V8C) |
| Ningún sustituto por dolor más difícil (SUST-SEG-1) | ✘ (2.469 exposiciones; 21/21 en el laboratorio) | ✔ |
| Ningún sustituto más difícil en VARIANTE | ✘ (solo lo pedía el prompt) | ✔ (V8C, emparejamiento por patrón) |
| La igualdad sigue permitida (no H12-A) | ✔ | ✔ |
| Sin sustituto no se publica nada inválido | ✔ | ✔: NO_FACTIBLE, P-03 o semana sin plan |
| La IA no puede terminar publicando un cambio inválido | ✔ en lo que validan V8C, V1-V25 y 4D; ✘ en la dificultad | ✔ |

## 4. Exposición, propuesta, rechazo, efectiva, publicada y respaldo (fase 8.3)

| Categoría | Cifra | Fuente |
|---|---:|---|
| **Exposición simulada** (SUST-SEG-1 subiría la dificultad) | 2.469 de 19.562 colocaciones; 573 perfiles | barrido |
| **Propuestas de IA registradas** con sustitución (históricas) | 123 sustituciones en 59 sesiones; ninguna sube con emparejamiento | `historico_123_*.csv` |
| **Propuestas de IA que suben** (REPLAY) | 1 (N08: 791 → 124), repetida en sus 3 intentos | 09 |
| **Rechazadas por el límite** | REPLAY: 3 intentos de N08; históricas: 0 | 09; §1 |
| **Sustituciones efectivas que suben** con off | REPLAY: 1 (N08, publicada 9 semanas); laboratorio de 22 perfiles: 21 (respaldo) | 09; 08 |
| **Publicadas con el límite** | 0 subidas | — |
| **Resultado del respaldo con el límite** | N08: 1003 (dif 1). 22 perfiles: 14 regeneradas y 7 semanas sin plan | 09; 08 |
