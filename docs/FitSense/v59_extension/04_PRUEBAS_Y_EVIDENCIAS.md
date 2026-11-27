# 04 · Pruebas y evidencias

**Ninguna llamada a Replicate.** Las pruebas usan:
- un Replicate **falso** local (`com.sun.net.httpserver`), con el cliente HTTP de producción;
- PostgreSQL 16 efímero (Testcontainers);
- el pipeline, los validadores, el respaldo y la publicación **reales**.

Los participantes son sintéticos. No se ejecutó el cron real ni se tocaron usuarios reales (ni el demo 48).

**El «reinicio»** reproduce el estado que deja un proceso muerto:
- el lease vencido, con otro propietario;
- la conexión del bloqueo consultivo cerrada (`pg_terminate_backend`);
- su hilo bloqueado en el GET (un **zombi**), que al final se suelta para comprobar el fencing.

**V59-03b** arranca un **segundo contexto de Spring**: otro proceso lógico, con otro propietario y la memoria vacía, sobre la misma base.

## 1. Escenarios (`OperacionesIaV66IT`, 15 pruebas, y `MigracionV65aV66IT`)

| Caso | Prueba | Comprobado | Resultado |
|---|---|---|---|
| V59-01 | `v59_01_reinicioSincronoPrediccionEnCurso` | Reinicio en la vía síncrona con la predicción en curso. La petición repetida **adopta la misma operación** (`executions` = 2) y sigue la predicción por su id. **1 POST**, 1 plan `AI`. El zombi despierta y el fencing le impide publicar | ✓ |
| V59-02 / V59-03 | `v59_02_03_reinicioDuranteLaAdaptacion` | Reinicio durante la **adaptación** del cierre semanal. `recuperarInterrumpidos()` relanza el ciclo y adopta la operación (`WEEKLY_CYCLE:u=…:medida=…:semana=…`). **3 POST**, los mismos que sin caída. **1 semana activa** y 1 evidencia V32 | ✓ |
| V59-03b | `v59_03b_reinicioRealDelContexto` | **Reinicio real:** el segundo contexto de Spring recupera el ciclo. 3 POST, 1 semana y operación `COMPLETED` | ✓ |
| V59-04 | `v59_01_…` | Predicción confirmada en ejecución: se sondea (2 *processing*) hasta terminar | ✓ |
| V59-05 | `v59_05_terminoDuranteLaCaida` | Terminó durante la caída: se recupera por su id, se **valida** y se publica. **0 POST** nuevos; fila `SUCCEEDED` | ✓ |
| V59-06 | `v59_06_postInciertoNoSeReenvia` | El proceso muere con el POST sin respuesta (`SOLICITANDO`). La reanudación lo deja `NO_CONFIRMADA`, **sin reenviar** ni inventar un id. Respaldo `RULE_ENGINE` | ✓ |
| V59-07 | `v59_07_dosUsuariosNoIntercambian` | Dos usuarios con perfil idéntico. El envío incierto de A **no consulta** las predicciones de B ni las adopta | ✓ |
| V59-08 | `v59_08_dosInstancias` | **Otra instancia viva:** `EnCursoEnOtroProceso`. **Dos instancias adoptando la misma huérfana a la vez:** gana una | ✓ |
| V59-09 | `v59_02_03_…` | Recuperación repetida, más el ciclo relanzado a mano, más el zombi: **ninguna publicación ni predicción más** | ✓ |
| V59-10 | `v59_10_planCambiado` | El plan medido cambió durante la espera. La predicción antigua queda **`DESCARTADA`** (no se aplica) y se pide con el estado actual. Con ese cambio la copia ya no lleva una dosis autorizada, así que **V24 la rechaza**, `NO_FACTIBLE`: no se publica nada inválido ni obsoleto. Tampoco el zombi publica | ✓ |
| V59-11 | `v59_11_resultadoInvalido` | La salida recuperada es inválida: los validadores la rechazan y se reintenta (3 POST); se publica `RULE_ENGINE` | ✓ |
| V59-12 | `v59_12_o20` | La salida recuperada trae un ejercicio **excluido por dolor**: lo publicado no lo contiene | ✓ |
| V59-13 / V59-14 | `v59_13_14_erroresYCancelacion` | **4xx:** `ERROR_ENVIO`, 0 predicciones. **Cancelada:** `CANCELED`. Ninguna cuenta como éxito; respaldo y operación `COMPLETED` | ✓ |
| V59-15 | `MigracionV65aV66IT` | V66 sobre datos V65: filas intactas y restricciones validadas. La asíncrona recupera y reclama las suyas; la API asíncrona no ve las nuevas; la unicidad `FIRST_PLAN` sigue. Una huérfana con predicción `ENVIADA` queda intacta | ✓ |
| V59-16 | `v59_16_getTransitorio` | 2 GET con 503: se toleran sin cancelar; se publica `AI` con 1 POST | ✓ |
| V59-17 | `v59_17_reiniciosRepetidos` | Dos caídas seguidas: cada adopción sigue **la misma** predicción (1 POST en total, `executions` = 3). Con las adopciones agotadas, `EJECUCION_INTERRUMPIDA` | ✓ |
| V59-18 | `v59_18_variosParticipantes` | El barrido relanza a 2 participantes aislados (3 POST cada uno, 1 semana cada uno). Un tercero fuera de su ventana queda `TIEMPO_EXCEDIDO`, sin plan | ✓ |
| Aislamiento | `aislamientoDeLaApiAsincrona` | `GET /generation/latest` no ve la operación síncrona | ✓ |

## 2. Pruebas existentes más expuestas (antes de la regresión completa)

| Suite | Resultado |
|---|---|
| `GeneracionAsincronaIT` | ✓ |
| `GeneracionAsincronaResilienciaIT` | ✓ |
| `ReanudacionConReplicateSimuladoIT` | ✓ |
| `CicloConcurrenteIT` | ✓, después de corregir el cierre ante un `Error` (ver más abajo) |
| `GeneracionInicialConcurrenteIT` | ✓ |
| `NotificacionesPushIT` | ✓ |
| `EliminacionDeCuentaIT` | ✓ |
| `MigracionV58aV59IT` | ✓ |
| `GeneracionInicialServiceTest` | ✓ |

**Error encontrado y corregido durante las pruebas** (`CicloConcurrenteIT.falloLiberaBloqueo`):
- Un `Error` que salía del ciclo dejaba la operación `PROCESSING`, con el lease vivo y de este mismo proceso.
- La siguiente ejecución la tomaba por otra en curso.
- Ahora se cierra también ante un `Error`, y una operación propia que ya no ejecuta nadie se adopta.

## 3. Pruebas inversas (`evidencia/pruebas_inversas.txt`)

Se retiró **cada protección** por separado y se ejecutó `OperacionesIaV66IT` completa. Después se restauró el código; se comprobó con `cmp` y `git status` limpio.

| Mutación | Pruebas que fallan |
|---|---|
| **M1:** la vía síncrona sin seguimiento | **10 de 15:** V59-01, 05, 06, 07, 08, 11, 12, 13/14, 17 y el aislamiento |
| **M2:** el ciclo semanal sin seguimiento | **4 de 15:** V59-02/03, **03b (reinicio real)**, 10 y 18. Son todas las del ciclo |
| **M3:** sin adopción (cada ejecución abre otra operación) | **6 de 15:** V59-01, 02/03, 06, 08, 10 y 17 |
| **M4:** sin fencing | **1 de 15:** V59-10. En los demás casos, la doble **publicación** también la impide la idempotencia de siempre (`prepare`/`persist` y `publishAdapted`). El fencing es la segunda barrera, y la única que impide que un zombi siga pidiendo predicciones para un contexto obsoleto |

## 4. Regresión completa

Ver `07_CIERRE.md`.
