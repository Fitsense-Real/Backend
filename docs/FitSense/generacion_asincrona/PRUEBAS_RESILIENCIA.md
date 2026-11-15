# Pruebas de resiliencia (V59)

Todas son deterministas y ninguna usa Replicate real. Los tiempos largos (30–1.500 s y más del límite) se simulan
fijando las marcas de tiempo en la base (`first_started_at`, `lease_expires_at`) o con un reloj controlado, sin
esperar.

## Suites

| Suite | Tipo | Dónde corre | Casos | Resultado |
|---|---|---|---|---|
| `DiarioDePrediccionesReplicateTest` | Unitaria. HTTP simulado (`MockRestServiceServer`), reloj fijo, libro en memoria. | `mvn test` y CI | 17 | 17/17 |
| `GeneracionAsincronaResilienciaIT` | Integración. PostgreSQL 16 de Testcontainers migrada V1..V59, motor de reglas real, `MockMvc` con seguridad. | `verify -Pintegration` y CI | 20 | 20/20 |
| `ReanudacionConReplicateSimuladoIT` | Integración. Pipeline real con la IA **encendida** contra un Replicate falso local (`com.sun.net.httpserver`, 127.0.0.1, token ficticio). | `verify -Pintegration` y CI | 5 | 5/5 |
| `GeneracionAsincronaIT` (de #71, adaptada) | Integración, opt-in (`-Dfitsense.pruebas.url`). Base vacía en local. | local | 15 | 15/15 |

## Duraciones

`generacionesLargasVivasNoVencen` mantiene la operación viva (lease vigente) con `first_started_at` hace 30, 90, 300,
600, 900 y 1.500 s, y ejecuta el barrido: **ninguna** vence. En #71 la de 600 s y la de 900 s pasaban a
`FAILED`.

`superaElLimiteDeLaOperacion` usa 1.801 s y obtiene `FAILED / TIEMPO_EXCEDIDO`, reintentable y sin lease.

`elLimiteNoSeReiniciaAlReencolar` comprueba que el límite cuenta desde la primera ejecución aunque haya
reencolados.

## Los 17 escenarios pedidos

| # | Escenario | Prueba(s) | Qué se comprueba |
|---|---|---|---|
| 1 | Flutter se cierra | `appCerradaYReabierta` | La generación sigue sin consultas. 1 operación y 1 plan. |
| 2 | Flutter vuelve | `appCerradaYReabierta`, `enCursoSeVeProcessing` | `GET /latest` devuelve la misma operación. Un `POST` devuelve 200 con el mismo id. En curso: `PROCESSING`, `pollAfterSeconds=3`, sin `planId`. |
| 3 | Reinicio del backend | `reinicioRecuperaYCompleta`, `reanudacionNoPagaOtraVez` | Lease vencido, reencolado y nuevo reclamo; `COMPLETED` con 1 plan y 0 POST nuevos. |
| 4 | Recuperar una predicción identificada | `reanudaPorIdentificador` | Solo GET por id. Cero POST. |
| 5 | Dos workers con la misma operación | `dosWorkersUnaOperacion`, `sinLeaseNoPublica`, `sinPropiedadNoSeEnvia` | 1 reclamo de 4. Sin lease no publica, no cierra, su latido no renueva y no envía. |
| 6 | Dos solicitudes simultáneas | `solicitudesSimultaneas` | 1 operación de 8 solicitudes. |
| 7 | La predicción termina durante el reinicio | `terminoDuranteElReinicio` | Un GET la recoge. Cero POST. |
| 8 | El proveedor responde después del vencimiento local | `adoptaLaDeUnaOperacionVencida`, `adoptaUnaEnCurso`, `reintentoManualAdopta` | La nueva operación del mismo usuario la adopta. Cero POST. |
| 9 | Operación sin actividad | `latidoAtrasadoConLeaseVigenteNoReencola`, `pendienteSinWorkerTermina` | Un latido atrasado con el lease vigente no se reencola. Una `PENDING` sin worker vence. |
| 10 | Resultado válido recibido dos veces | `resultadoRecibidoDosVeces`, `dosReiniciosSiguenSiendoTres` | Cero HTTP al reutilizar. Siempre 3 POST y 1 plan. |
| 11 | Plan ya publicado | `planYaPublicadoAntesDeCerrar` | Se cierra con **ese** plan, sin duplicar. |
| 12 | NO_FACTIBLE | `noFactible` | Terminal, no reintentable, sin plan. |
| 13 | Error del proveedor | `errorRegistradoSeRepite`, `cincoXXEsIncierto`, `cuatroXXNoExiste`, `errorTecnicoYReintentoManual` | Clasificación correcta. Se repite sin POST. `FAILED` reintentable. |
| 14 | Proveedor temporalmente inaccesible | `conexionRechazada`, `corteDeLecturaEsIncierto` | Conexión rechazada → `ERROR_ENVIO`. Lectura cortada → `NO_CONFIRMADA`. |
| 15 | Reintento manual | `errorTecnicoYReintentoManual`, `reintentoManualAdopta`, `libroReutilizable` | Una operación nueva que adopta lo pagado. |
| 16 | Máximo de la operación | `vivaQueAlcanzaElMaximoNoPublica` | `FAILED`. El resultado tardío no se publica. |
| 17 | Aceptada externamente sin id confirmado | `aceptadaSinIdNoSeReenvia` (unitaria e IT) | `NO_CONFIRMADA`, **sin reenvío**, respaldo validado. 1 POST antes y después del reinicio. |

Otras pruebas:

- `libroJdbc`, `libroRechazaIncompletos`: la base rechaza un `SUCCEEDED` sin salida y un `ENVIADA` sin id.
- `peticionDistintaSeDescarta`.
- `noSeReutilizaDeOtroUsuarioNiOtraPeticion`.
- `huellaEstable`.
- `sinAmbitoNadaCambia`: el endpoint síncrono, el ciclo semanal y la adaptación siguen con `Prefer: wait`, como
  antes.
- `apagadoSoloLasPropias`.
- `interrupcionesRepetidasTerminan`.

## Objetivos

| Objetivo | Resultado |
|---|---|
| Cero operaciones duplicadas | Cumplido en E5, E6 y E1/E2 |
| Cero planes duplicados | Cumplido: cada prueba comprueba `count(weekly_training_plans) = 1` |
| Cero publicaciones inválidas | Cumplido: el pipeline (validadores, respaldo, `persist`) no cambia. Lo reutilizado se vuelve a validar. Sin lease no se publica. |
| Cero pérdidas silenciosas | Cumplido: lo pagado queda en el libro, y un vencimiento o un reintento lo adopta. Cada ejecución deja su recuento en el log. |
| Cero reintentos externos innecesarios | Cumplido en las pruebas: 0 POST nuevos en las reanudaciones. Un `NO_CONFIRMADA` nunca se reenvía. |
