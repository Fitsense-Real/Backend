# 03 · Pruebas (sin coste)

**Ninguna llamada a Replicate.** Las pruebas usan dos montajes:
- `ReplicateFalso`: un servidor local sobre un **socket real**, que atiende al cliente HTTP **de producción** (`ReplicateHttp`, `HttpURLConnection` con sus límites). Reproduce cortes RST y FIN, cabeceras ausentes, cuerpos a medias y conexiones cerradas.
  - Cada POST crea, o no, una predicción con su id y **guarda su entrada**.
  - Los GET devuelven la entrada de cada una, y el listado devuelve todas.
  - Así se simulan dos usuarios con peticiones idénticas contra el mismo servidor.
- Libros V59 en memoria (`LibroEnMemoria`), con la semántica de `JdbcDiarioDePredicciones`. Para la concurrencia, el libro va bajo un cerrojo.

Los tokens son literales de prueba.

## 1. `ReplicateRecuperacionTransporteTest`: 36 pruebas, 36 aprobadas

### Diagnóstico (3)

| Prueba | Qué comprueba |
|---|---|
| `g08ReproducidoConElClienteDeProduccion` | El mensaje **exacto** de G08: 2xx sin `Content-Type` y un RST a mitad del cuerpo |
| `elTipoOctetStreamSaleDeLaFaltaDeCabecera` | Con la cabecera, Spring dice `[application/json]` |
| `idDeSuRespuesta` | Solo un id de **primer nivel**, **completo** y alfanumérico. Ni uno anidado, ni uno a medias, ni uno no alfanumérico |

### T01–T14 (orden de transporte)

| # | Prueba(s) | Resultado comprobado |
|---|---|---|
| T01 | `t01RespuestaCorrecta` | Se sigue por su id. 1 POST, 0 listados |
| T02 | `t02G08SeRecuperaPorSuId`, `t02TruncadaSeRecupera` | El id llegó antes del corte (RST o FIN): `RECUPERADA_POR_ID`, se usa la salida. 1 POST, 0 listados. Queda registrado «Content-Type ausente» |
| T03 | `t03SinIdentificador` | `PREDICCION_NO_CONFIRMADA`, `prediction_id=desconocido`. 1 POST, 0 listados |
| T04 | `t04AMediasConId`, `t04TimeoutSinNada` | **Límite de lectura con el id recibido:** se confirma y se usa. **Sin recibir nada:** incierta. 1 POST |
| T05 | `t05CerradaSinRespuesta` | El cliente **no reenvía** el POST. Incierta |
| T06 | `t06Errores` | **5xx:** incierta, sin buscar. **4xx:** definitivo. **Sin conexión:** no enviada |
| T07 | `t07EnCurso` | La confirmada en curso se sondea hasta el final |
| T08 | `t08Completada` | La confirmada y terminada se lee una sola vez |
| T09 | `t09Cancelada` | No es un éxito |
| T10 | `t10Fallida` | Fallo del proveedor |
| T11 | `t11SalidaInvalidaRecuperada` | La salida recuperada pasa por el mismo contrato. Se reintenta como plan inválido |
| T12 | `t12Concurrencia` | Misma operación en dos hilos: 1 POST |
| T13 | `t13ConLibroG08` | Libro: `SUCCEEDED` con el id de la respuesta cortada. Reanudar reutiliza sin llamar |
| T14 | `t14Adaptacion` | Las correcciones de sesión, con su esquema, se confirman igual |

### S01–S15 (orden de validación final)

| # | Prueba(s) | Resultado comprobado |
|---|---|---|
| S01 | `s01DosUsuariosIdenticos`, `s01DosUsuariosAmbasExisten` | El usuario B (misma entrada) termina bien. Al usuario A se le corta el POST: **`PREDICCION_NO_CONFIRMADA`**, y **ni consulta la predicción de B**. Si la de A también existe sin id, tampoco se elige ninguna: son indistinguibles |
| S02 | `s02UnaCoincidenciaSinIdentidad` | Existe **exactamente una** predicción con esa entrada (la propia, sin id) y **no se adopta** |
| S03 | `s03IdEnLaRespuesta`, `s03LocationNoDocumentadoSeIgnora` | Con 201 y con 202, el id se anota en el libro (`enviada <id>`) y se sigue. Un `Location` (no documentado) que apunta a otra predicción **se ignora** |
| S04 | `s04ParcialConId` | **Id cortado:** incierta, sin ningún GET. **Id solo anidado:** incierta, sin ningún GET. **Id completo cuyo GET trae otra entrada:** `IDENTIDAD_DISTINTA`, no se adopta |
| S05 | `s05VariasCandidatas` | Tres predicciones con la misma entrada: no se elige ninguna. 0 listados |
| S06 | `s06BusquedaIncompletaNoEsInexistencia` | Con 30 predicciones de ruido: `NO_CONFIRMADA` (pudo crearse), **nunca `ERROR_ENVIO`**. Ningún mensaje dice «no existe» |
| S07 | `s07DesfaseDeReloj` | Predicción creada 5 h antes y reloj del backend 5 h adelantado: se confirma por id. No hay ventanas de tiempo |
| S08 | `s08DatosBorrados` | `data_removed`: `NO_VERIFICABLE_DATOS_BORRADOS`, incierta |
| S09 | `s09PostSinRespuesta` | POST recibido y creado, conexión cerrada: **1 POST**, 1 predicción |
| S10 | `s10GetIntermitente` | **2 fallos de consulta:** se toleran, sin cancelar. **Fallos persistentes:** `POLL_ERROR` y **una** cancelación |
| S11 | `s11ConcurrentesConCorte` | Misma operación en dos hilos con la respuesta cortada: 1 POST |
| S12 | `s12AdaptacionNoReutilizaAjena` | Adaptación con dos usuarios idénticos: A no consulta ni adopta la de B |
| S13 | `s13ReanudacionV59` | **`ENVIADA`:** se sigue por su id. **`SUCCEEDED`:** se reutiliza, 0 peticiones. **`SOLICITANDO`:** incierta, sin POST ni búsqueda, aunque haya predicciones con esa entrada |
| S14 | `s14ValidacionTrasRecuperar` | El **validador** rechaza la rutina de una predicción recuperada; el pipeline reintenta y publica la segunda. Validador llamado 2 veces |
| S15 | `s15RespaldoSeguro` | Escenario S01 en el pipeline: **`RULE_ENGINE`**, ninguna salida incierta llega al contrato de la IA, A hace 1 POST |

## 2. Pruebas inversas (`evidencia/revision_seguridad_pruebas_inversas.txt`)

Se ejecutaron las 36 pruebas sobre otras versiones del cliente. Solo se añadieron *stubs* sin comportamiento para compilar.

| Versión del cliente | Fallan | Lo que demuestra |
|---|---|---|
| **#88 antes de la revisión** (`f3786e60`), con un Replicate falso realista (la predicción «otra1» de otro usuario existe) | **S01 (2), S02, S05, S12 y S15: 6 de 6** | **Atribución ajena:** `REPLICATE_RECUPERACION resultado=RECUPERADA prediction_id=prediccionb1` para el usuario A. En S15, el pipeline **publica como IA** la salida de B |
| #88 antes de la revisión, con la suite completa (con el falso de la primera pasada, en el que «otra1» respondía 404) | 25 de 36: `idDeSuRespuesta`, S01 (2), S02, S04, S05, S07, S08, S11, S12, S13, S14, T02 (2), T03, T04 (2), T06–T11, T13 y T14 | Con ese falso, S15 pasaba por casualidad: el 404 de «otra1» dejaba la búsqueda incierta. Por eso se repitió con el falso realista (fila anterior) |
| `generation` (`98276a93`), el cliente original | 28 de 36 | Sin la corrección no se recupera nada; además, S10 (cancelaba al primer fallo) y S09/T05 (mensaje) |
| **M1:** la entrada no se compara al confirmar | S04 | La comparación es la que impide adoptar un id con otra entrada |
| **M2:** se acepta un `"id"` a cualquier profundidad | `idDeSuRespuesta` y S04 | La regla del primer nivel es la que impide tomar el id anidado |
| **M4:** el sondeo sin tolerancia (1 fallo) | S10 | La tolerancia es la que evita cancelar por un fallo pasajero |

**Después de cada prueba inversa se restauró el archivo** (comprobado con `cmp`). La versión entregada es la que pasa las 36.

**La primera ronda** de pruebas inversas, de la versión inicial de #88, queda en `evidencia/prueba_inversa.txt` como historial.

## 3. Pruebas existentes

Desde la revisión ya **no se busca por contenido**. Por eso, las pruebas previas vuelven a esperar exactamente lo que esperaban en `generation`: sin listado, el mismo desenlace.

Solo cambian estas:

| Prueba | Cambio | Motivo |
|---|---|---|
| `ReplicateRespuestasInvalidasTest.sobreIlegible` (el caso del JSON cortado `{"id":"p1",…`) | Espera el GET de confirmación de `p1`, que responde 404. Sigue siendo un fallo del proveedor «no se pudo interpretar» | El sobre cortado traía su id completo |
| `sobreVacio` y `sobreJsonVacio` (mismo archivo), `DiarioDePrediccionesReplicateTest.corteDeLecturaEsIncierto` | Añaden `server.verify()` | Más estrictas: ninguna petición de más |
| `ReplicateTimingInstrumentationTest.falloDeConsultaNoEsTimeout` y `ReplicateCancelacionTest.sondeoFallidoTambienCancela` | 3 consultas fallidas seguidas antes de cancelar, en vez de 1 | Tolerancia del sondeo (§6 de `02`) |
| `ReplicateTimeoutsHttpTest.respuestaParcial` | Límite de 15 s en vez de 8 s | Ahora incluye un GET de confirmación, acotado. Cada límite cuenta dos veces porque `HttpURLConnection` vacía el resto del cuerpo al cerrar. El POST no se reenvía |

## 4. Regresión completa

Ver `05_CIERRE.md` §2.
