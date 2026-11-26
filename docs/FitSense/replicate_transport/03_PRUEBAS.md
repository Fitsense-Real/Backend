# 03 · Pruebas (sin coste)

**Ninguna llamada a Replicate.** Las pruebas usan dos montajes:
- `ReplicateFalso`: un servidor local sobre un **socket real**, que atiende al cliente HTTP **de producción** (`ReplicateHttp`, `HttpURLConnection` con sus límites). Así se reproducen cortes, cabeceras ausentes y conexiones cerradas, que `MockRestServiceServer` no puede simular.
- Libros V59 en memoria (`LibroEnMemoria`), con la misma semántica que `JdbcDiarioDePredicciones`.

Los tokens son literales de prueba.

## 1. Escenarios T01–T14 (`ReplicateRecuperacionTransporteTest`, 25 pruebas)

| # | Escenario | Prueba(s) | Resultado esperado y comprobado | Resultado |
|---|---|---|---|---|
| Diag. | Error de G08 | `g08ReproducidoConElClienteDeProduccion`, `elTipoOctetStreamSaleDeLaFaltaDeCabecera` | El mensaje **exacto** de la evaluación con 2xx sin `Content-Type` y un RST; con la cabecera, `[application/json]` | ✓ |
| T01 | JSON correcto | `t01RespuestaCorrecta` | Se sigue por su id y se usa la salida. 1 POST, sin listado | ✓ |
| T02 | `octet-stream` recuperable (G08) | `t02G08SeRecupera`, `t02TruncadaSeRecupera` | Se recupera la predicción existente: `REPLICATE_RECUPERACION resultado=RECUPERADA prediction_id=g40…`, `REPLICATE_TIMING … SUCCEEDED`. **1 POST** | ✓ |
| T02 inverso | Candidata con otra entrada, o dos iguales | `t02OtraEntradaNoSeAdopta`, `t02AmbiguaNoSeAdopta` | No se adopta: `PREDICCION_NO_CONFIRMADA` o `AMBIGUA`, con el motivo original. 1 POST | ✓ |
| T03 | 2xx sin id | `t03SinIdentificador`, `t03SinIdentificadorRecuperada` | `prediction_id=desconocido`: nunca se inventa. Se busca (2 rondas); si no aparece, incierta; si aparece, se sigue. 1 POST | ✓ |
| T04 | Timeout tras crearla | `t04TimeoutDelPostConPrediccionCreada`, `t04AMediasSeRecupera`, `t04SondeoFallaUnaVez` | **POST colgado o cortado:** se recupera por el listado. **Sondeo que falla 2 veces:** se reintenta y termina bien, **sin cancelar**. 1 POST | ✓ |
| T05 | Timeout antes de confirmar | `t05CerradaSinRespuesta`, `t05InciertaEnElPipeline` | **Conexión cerrada sin respuesta:** el cliente **no reenvía** el POST. Queda `PREDICCION_NO_CONFIRMADA`. **En el pipeline:** 1 POST y respaldo `RULE_ENGINE`, sin un segundo intento de IA | ✓ |
| T06 | 5xx y 4xx | `t06Errores5xxY4xx`, `t06SinConexion` | **502 con la predicción creada:** se recupera. **502 sin ella:** incierta («http 502»). **422:** rechazo definitivo, sin buscar. **Sin conexión:** ni se busca | ✓ |
| T07 | En curso | `t07RecuperadaEnCurso` | La recuperada en `processing` se sondea hasta el final. 1 POST | ✓ |
| T08 | Completada | `t08RecuperadaCompletada` | Su salida se lee **una** vez, sin sondeo | ✓ |
| T09 | Cancelada | `t09Cancelada` | No es un éxito: `AiProviderUnavailableException` «canceled» | ✓ |
| T10 | Fallida | `t10Fallida` | El fallo del proveedor de siempre | ✓ |
| T11 | Salida inválida | `t11SalidaInvalidaRecuperada` | La salida recuperada pasa por el **mismo** contrato (`toDraft`), que la rechaza. El pipeline reintenta como plan inválido y publica la IA en el segundo intento. El segundo POST es ese reintento, no un duplicado | ✓ |
| T12 | Concurrencia | `t12Concurrencia` | Dos hilos, misma operación e intento, libro con cerrojo: **1 POST** | ✓ |
| T13 | Reanudación | `t13ReanudacionSolicitando`, `t13ConLibroG08` | **`SOLICITANDO` tras morir el proceso:** se recupera, con **0 POST**, y la fila queda `SUCCEEDED` con el id. **G08 con libro:** `SUCCEEDED`, y una nueva ejecución reutiliza la salida sin llamar | ✓ |
| T14 | Generación y adaptación | `t14Adaptacion` (más T01–T13, que son de generación) | La petición de correcciones de sesión, con su propio esquema, recupera G08 igual | ✓ |

## 2. Prueba de regresión inversa

Se ejecutaron las 25 pruebas contra el `ReplicateTrainingPlanGenerator` de `generation` (`98276a93`). Solo se le añadieron las dos constantes que las pruebas nombran, sin comportamiento (`evidencia/prueba_inversa.txt`).

| Con el cliente anterior | Con la corrección |
|---|---|
| **20 fallan** (6 fallos y 14 errores): todas las de T02–T11, T13 y T14 | **25/25 pasan** |
| Pasan 5: las dos de diagnóstico, T01 y T06 sin conexión (que no cambian), y **T12**, porque la concurrencia ya la protegía el libro V59 | — |

## 3. Pruebas existentes actualizadas al contrato nuevo

Cambian porque **el contrato cambió a propósito**: un envío incierto ahora se busca antes de rendirse, y una consulta fallida se reintenta. El desenlace que comprueban (fallo del proveedor, sin reenviar, con la cancelación registrada) **no cambia**. Ahora declaran también las consultas gratuitas, con `RecuperacionSimulada.sinCoincidencia`.

| Prueba | Cambio |
|---|---|
| `ReplicateRespuestasInvalidasTest` (sobre ilegible, vacío, `{}`, pipeline) | Esperan, además, el listado vacío (2 rondas). Siguen comprobando «no se pudo interpretar», que no se copia el cuerpo y el respaldo con 1 llamada |
| `ReplicateTimingInstrumentationTest` (5xx del POST, POST lento, consulta fallida) | El listado vacío. La consulta fallida se reintenta: 3 consultas, `POLL_ERROR` |
| `DiarioDePrediccionesReplicateTest` (`SOLICITANDO`, 5xx, corte, carrera sin confirmar) | El listado vacío. Siguen terminando `NO_CONFIRMADA`, sin reenviar |
| `ReplicateCancelacionTest.sondeoFallidoTambienCancela` | 3 consultas fallidas seguidas; luego se cancela, como antes |
| `ReplicateTimeoutsHttpTest.respuestaParcial` | Límite de 15 s en lugar de 8 s, porque ahora incluye la búsqueda, también acotada. El POST no se reenvía |

## 4. Regresión completa

Ver `05_CIERRE.md` §2.
