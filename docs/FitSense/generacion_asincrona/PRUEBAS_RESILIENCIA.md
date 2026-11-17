# Pruebas de resiliencia de la generación asíncrona (R01–R20)

**Todas son deterministas y ninguna usa Replicate real.** Donde hace falta un proveedor se usa:

- `MockRestServiceServer` en las unitarias;
- un **Replicate falso local** (`com.sun.net.httpserver` en 127.0.0.1, con token ficticio) en las IT. Este falso
  cuenta los POST aceptados, que son las predicciones que se cobrarían.

Los tiempos largos se simulan fijando las marcas de tiempo en la base (`first_started_at`, `lease_expires_at`), sin
esperar. El «reinicio» es lo que ve la base cuando un proceso muere: el lease vence o lo tiene otro propietario.
«Otra instancia» es un segundo `PlanGenerationOperationService`, con otro propietario, sobre la misma base.
Estos resultados son de **simulación**, no de una ejecución con Replicate real.

## Suites

| Suite | Tipo | Corre en | Casos |
|---|---|---|---|
| `DiarioDePrediccionesReplicateTest` | Unitaria (HTTP simulado, reloj fijo, libro en memoria con las mismas reglas que el de JDBC) | `mvn test` y CI | 20 |
| `PlanGenerationWorkerTest` | Unitaria (worker con un servicio simulado) | `mvn test` y CI | 5 |
| `GeneracionAsincronaResilienciaIT` | Integración, PostgreSQL 16 de Testcontainers (V1..V59), motor de reglas real, `MockMvc` con seguridad y worker real con sus hilos | `verify -Pintegration` y CI | 23 |
| `ReanudacionConReplicateSimuladoIT` | Integración, pipeline real con la **IA encendida** contra el Replicate falso | `verify -Pintegration` y CI | 11 |
| `MigracionV58aV59IT` | Integración: Flyway hasta V58 con datos, después la aplicación aplica V59 | `verify -Pintegration` y CI | 1 |
| `GeneracionAsincronaIT` (de #71) | Integración opt-in (`-Dfitsense.pruebas.url`), base vacía local | local | 15 |

## R01–R20

| Caso | Condiciones iniciales | Acción simulada | Esperado | Obtenido | Evidencia | Estado | Prueba |
|---|---|---|---|---|---|---|---|
| R01 | Operaciones `PROCESSING` con lease vigente y 30/90/300/600/900/1.500 s desde su primera ejecución | Barrido | No vencen | Las 6 siguen `PROCESSING` | Estado en la base | PASS | `GeneracionAsincronaResilienciaIT.generacionesLargasVivasNoVencen`; también `ReanudacionConReplicateSimuladoIT.salidaValidaTrasUnaEsperaLarga` (1.000 s y publicada) |
| R02 | Worker real generando; el planificador de Spring con su único hilo | Se ocupa 3 s ese hilo (un «cierre semanal») | El latido no se bloquea | `lease_expires_at` avanza durante el bloqueo; una tarea en el planificador de Spring no corre; `COMPLETED` con 1 ejecución | lease antes y después; tamaño del pool = 1 | PASS | `GeneracionAsincronaResilienciaIT.elCierreSemanalNoBloqueaElLatido` |
| R03 | 1 `PENDING` | 4 workers reclaman a la vez | Solo uno | 1 reclamo | Resultados de `claimNext` | PASS | `dosWorkersUnaOperacion` |
| R04 | Otro proceso tiene el lease | El worker antiguo intenta publicar, cerrar, latir y enviar | No puede | No publica, no cierra, su latido renueva 0 filas, no envía y no marca `NO_CONFIRMADA` la fila del propietario | Plan 0; filas | PASS | `sinLeaseNoPublica`, `sinPropiedadNoSeEnvia`, `sinPropiedadNoSeTocaElLibro` |
| R05 | La predicción p1 está `ENVIADA` | Reinicio: el lease vence y otra instancia reclama | Recupera la predicción | B sigue p1 por GET, 0 POST para ese intento; total 3 POST y 1 plan | `POSTS`, `GET_ESPERANDO`, libro | PASS | `ReanudacionConReplicateSimuladoIT.dosInstanciasNoDuplicanElTrabajo`, `reanudaPorIdentificador` |
| R06 | La predicción terminó mientras el backend estaba caído | Reanudación | Recoge el resultado | Un GET y 0 POST; con todo pagado, 0 POST nuevos | Libro: 3 iniciadas, 3 completadas | PASS | `terminoDuranteElReinicio`, `reanudacionNoPagaOtraVez` |
| R07 | Replicate acepta el POST y corta sin responder | Ejecución y después reinicio | Sin reenvío | `NO_CONFIRMADA` y respaldo validado; 1 POST antes y después; en una carrera con el mismo intento, tampoco se envía | Libro: 1 no confirmada, ningún id | PASS | `aceptadaSinIdNoSeReenvia` (unitaria y E2E), `carreraConOtraEjecucionSinConfirmar` |
| R08 | La app pide dos veces, o 8 a la vez, o 10 usuarios con doble toque | `POST /plan/generation` | Una operación activa por usuario | 1 operación por usuario; un `POST` tras `COMPLETED` devuelve la misma | Filas | PASS | `solicitudesSimultaneas`, `appCerradaYReabierta`, `variosUsuariosConElWorkerReal` |
| R09 | El plan se publicó y el proceso murió antes de cerrar | Reanudación | No se publica otra vez | `COMPLETED` con **ese** plan; 1 plan | `plan_id` | PASS | `planYaPublicadoAntesDeCerrar` |
| R10 | La operación venció con su predicción en curso | La predicción termina tarde y el usuario reintenta | Política documentada: se guarda, no se publica y el reintento la adopta | Salida en el libro; la operación sigue `FAILED`; la nueva adopta el intento 1 y paga 2 (3 en total) | `reutilizadas = 1` | PASS | `venceDeVerdadDuranteUnaPrediccion`, `adoptaLaDeUnaOperacionVencida` |
| R11 | Replicate responde 503 al sondear | Ejecución | Recuperación controlada | Cancelación y respaldo validado; 1 POST; sin bucle | Libro: 1 cancelada o vencida | PASS | `ReanudacionConReplicateSimuladoIT.errorTemporalAlSondear`, `conexionRechazada` |
| R12 | Replicate rechaza el POST (4xx) | Ejecución | Estado terminal correcto | `ERROR_ENVIO`, 0 predicciones creadas, `COMPLETED` con el respaldo validado | `RECHAZADOS = 1`, `POSTS = 0` | PASS | `errorDefinitivoDelProveedor`, `cuatroXXNoExiste` |
| R13 | Perfil sin semana posible | Ejecución | `NO_FACTIBLE` sin relajar | Terminal, no reintentable, sin plan | Estado | PASS | `noFactible` |
| R14 | Una predicción en curso y 1.801 s desde la primera ejecución | Barrido; después la predicción termina | Termina bien y sin trabajo huérfano que publique o pague | `FAILED / TIEMPO_EXCEDIDO`; tras vencer, 0 POST nuevos y 0 planes | `POSTS = 1` | PASS | `venceDeVerdadDuranteUnaPrediccion`, `vivaQueAlcanzaElMaximoNoPublica` |
| R15 | 10 usuarios | Piden a la vez (con doble toque) y los procesa el worker real | Operaciones aisladas | 10 operaciones, 10 planes, cada uno con el suyo | Filas por usuario | PASS | `variosUsuariosConElWorkerReal` |
| R16 | Plan escrito y el proceso muere antes del commit | Reanudación | Una sola publicación consistente | Rollback (0 planes y 0 sesiones), después 1 plan; 0 POST nuevos | Filas | PASS | `ReanudacionConReplicateSimuladoIT.muereDuranteLaPublicacion` |
| R17 | Dos instancias del backend | A pierde el lease con la predicción en curso; B reclama | No duplican el trabajo | 3 POST, 1 plan, 0 `NO_CONFIRMADA`; en una carrera por el mismo intento, la segunda no anota | Libro y `POSTS` | PASS | `dosInstanciasNoDuplicanElTrabajo`, `carreraConOtraEjecucionEnviada`, `libroNoAdmiteDosVigentesDeLaMismaPeticion` |
| R18 | La generación lleva 1.000 s viva y la predicción tarda (8 respuestas «processing») | Termina con una salida **válida** | Se aprovecha | Publicada como `AI`, 1 POST | `generation_source = AI` | PASS | `salidaValidaTrasUnaEsperaLarga` |
| R19 | Operación `COMPLETED` | Otra ejecución (dos instancias) y un resultado repetido | Sin publicación duplicada | El pipeline no corre otra vez; la fila `SUCCEEDED` no cambia; 1 plan | Contador de ejecuciones del pipeline = 1 | PASS | `caminoNormalYRepeticiones` |
| R20 | Worker real con 10 operaciones | Procesarlas; contar barridos | Sin saturación | Como máximo 2 en paralelo; 10 en unos 1,6 s; 1 barrido cada 20 ciclos (en #71, 20); CI de unos 26 min (en #71 llegó a cancelarse a los 45 min) | Máximo observado = 2 | PASS | `variosUsuariosConElWorkerReal`, `PlanGenerationWorkerTest.elBarridoNoCorreEnCadaCiclo` |

Otros casos cubiertos:

- **Actualización V58 → V59 con datos heredados:** `MigracionV58aV59IT`. Ver `MIGRACIONES_V58_V59.md`.
- **Apagado ordenado:** `apagadoSoloLasPropias`.
- **Tres interrupciones:** `interrupcionesRepetidasTerminan`.
- **`PENDING` sin worker:** `pendienteSinWorkerTermina`.
- **Libro en JDBC:** `libroJdbc`, `libroRechazaIncompletos`, `libroReutilizable`.
- **Huella de la petición:** `huellaEstable`, `peticionDistintaSeDescarta`, `noSeReutilizaDeOtroUsuarioNiOtraPeticion`.
- **Camino síncrono sin cambios:** `sinAmbitoNadaCambia`.

## Objetivos

| Objetivo | Resultado |
|---|---|
| No perder una generación válida | La salida válida tras una espera larga se publica (R18). Lo pagado queda en el libro y una reanudación o un reintento lo reutiliza (R05, R06, R10). |
| No ejecutar predicciones duplicadas innecesarias | 0 POST nuevos en todas las reanudaciones (R05, R06, R16 y R17). `NO_CONFIRMADA` nunca se reenvía (R07). |
| Recuperarse tras un reinicio | R05, R06, R09, R16 y la actualización con una `PROCESSING` heredada |
| Cero planes duplicados | Todas las pruebas comprueban `count(weekly_training_plans) = 1` |
| Cero publicaciones inválidas | El pipeline, los validadores y `persist` no cambian, y lo reutilizado se vuelve a validar |
