# 06 · Pruebas y regresiones

**Condiciones de todas las pruebas:**
- HTTP simulado con `MockRestServiceServer` o con servidores locales `ServerSocket` en 127.0.0.1.
- Reloj y espera simulados (`Clock`, `Durmiente`) para el sondeo y la cancelación.
- PostgreSQL 16 con Testcontainers para la concurrencia.
- IA sustituida por dobles (`@MockitoBean`).
- **Ninguna llamada a Replicate. Coste: USD 0.**
- La regresión se ejecutó sin `REPLICATE_API_TOKEN` en el entorno (`env -u`).

## 1. Pruebas nuevas por PR y evidencia de que fallaban antes

En todos los casos, cada prueba nueva se ejecutó primero contra el código anterior, o contra el código nuevo con la corrección desactivada (mutación), para comprobar que detecta el problema.

| PR | Prueba | Tipo | Nº | Fallaban sin la corrección |
|---|---|---|---|---|
| #49 (R-1) | `ReplicateRespuestasInvalidasTest` | unitaria | 14 | **9 / 14** con el código de `generation` (el sobre ilegible salía como excepción no clasificada) |
| #49 (R-1) | `AdaptedWeekCommandServiceTest.respuestaIlegibleVaAlRespaldo` | unitaria | 1 | **1 / 1** |
| #49 (R-3) | `ReplicateTimeoutsHttpTest` (servidores locales: sin conexión, acepta y calla, respuesta a medias, sondeo colgado, control y configuración) | unitaria | 6 | Antes no había límites HTTP explícitos (`RestClient.builder()` por omisión, sin timeouts configurados): no existía una cota que probar; las pruebas fijan la nueva. |
| #49 | `ReplicateTimingTest.losSieteResultados` | unitaria (actualizada) | — | Obliga a decidir si el nuevo resultado `INVALID_RESPONSE` cuenta para el disyuntor (cuenta). |
| #50 (R-2) | `ReplicateCancelacionTest` | unitaria | 10 | **7 / 10** con la cancelación desactivada (mutación) |
| #50 (R-2) | `ReplicateTimingInstrumentationTest` | unitaria (3 actualizadas) | — | Ahora esperan la petición `POST …/predictions/{id}/cancel` tras agotar el sondeo. |
| #52 (R-5) | `LimiteDeGeneracionesTest` | unitaria | 4 | Clase nueva |
| #52 (R-5) | `GeneracionInicialServiceTest` | unitaria | 7 | Clase nueva |
| #52 (R-5) | `GeneracionInicialConcurrenteIT` (PostgreSQL real y bloqueo consultivo real) | integración | 3 | **2 / 3** sin el bloqueo: dos peticiones simultáneas hacían 2 llamadas a la IA; el bloqueo de otro proceso se ignoraba. |
| #51 (R-4) | `EstadoAdaptacionSinIaTest` | unitaria | 5 | **4** fallaban con el código anterior (sumando estas y las de la fila siguiente): «sin respuesta del proveedor» aparecía como `RESPALDO_PROPUESTA_IA_INVALIDA`. |
| #51 (R-4) | `AdaptedWeekCommandServiceTest` (4 casos nuevos) | unitaria | 4 | (incluidas en la cifra anterior) |

**Total:** +51 unitarias (1345 → 1396) y +3 de integración (603 → 606).

## 2. Hallazgos de la regresión completa (corregidos antes de entregar)

1. **Enlace de `ReplicateProperties` (#49).**
   - **Síntoma:** al añadir un constructor de compatibilidad al `record`, Spring no sabía cuál usar: «No default constructor found». Fallaron 557 pruebas de integración en CI.
   - **Corrección:** `@ConstructorBinding` en el constructor canónico (commit `9a30036`), propagada a #50 por fusión.
2. **`CicloConcurrenteIT.falloLiberaBloqueo` (#49).**
   - **Síntoma:** un `catch (RuntimeException)` añadido en la adaptación publicaba una semana de respaldo ante un error inesperado. El contrato del ciclo dice que en ese caso no se publica nada y se libera el bloqueo.
   - **Corrección:** no se cambió la prueba. Se retiró ese `catch` (commit `8f4e337`) y se documentó la decisión en `02_RESPUESTAS_INVALIDAS_Y_RESPALDO.md`.
   - **Resultado:** en la adaptación, una respuesta ilegible sigue yendo al respaldo, porque es un fallo del proveedor.
   - Esta primera regresión de #49 dio 1367/0/1 unitarias y 603/1/44 de integración. Tras la corrección, CI quedó en verde.
3. **Conflicto en `application.properties` entre #49 y #52.**
   - **Causa:** las dos PR añadían líneas tras `fitsense.ai.prefer-wait-seconds`.
   - **Corrección:** el bloque de #52 se movió tras `fitsense.ai.adaptation.prompt-version` (commit `2d53f46`). Ahora las cuatro PR se fusionan sin conflictos en cualquier orden compatible con el apilamiento #49 → #50.

## 3. Regresión combinada (#49 + #50 + #51 + #52 fusionadas en local)

Fusión local, no subida, de las cuatro ramas sobre `generation` `3374213`. Se ejecutó `./mvnw verify -Pintegration`.

| Suite | Resultado |
|---|---|
| Unitarias | **1396 ejecutadas, 0 fallos, 0 errores, 1 omitida**: la misma omitida que en `generation` |
| Integración | **606 ejecutadas, 0 fallos, 0 errores, 44 omitidas**: las mismas 44 omitidas por propiedad que en `generation`, porque requieren activar evaluaciones manuales |
| Build | **BUILD SUCCESS** (19 min 17 s) |

**Incluidas en la regresión combinada, sin cambios de resultado:**
- R-1 a R-5: todas las suites nuevas de la sección 1.
- Generación inicial con **V24 intacta** y los **cuatro objetivos**: suites de configuración evaluada y de generación por objetivo.
- Adaptación con **V8C**: `AdaptedWeekCommandServiceTest` y validación multisemana (57 / 0 / 2 omitidas).
- **Progresión:** `ProgresionIT` (24) y la progresión de PLAN-REV-2.0 (24).
- Ciclo semanal concurrente: `CicloConcurrenteIT` (9).
- Suites de **reincorporación**.

**Cohorte integral con IA simulada** (`EvaluacionK20IT`, PLAN-REV-2.0, progresión activada):
- Escenarios O06, O12 y O20, 8 semanas cada uno.
- **0 diferencias** de estado, decisión, tipos del informe y llamadas frente a `generation`.
- 0 llamadas a la IA y 0 incidencias.

## 4. CI de cada PR (GitHub Actions, job `verify`)

| PR | Cabeza | CI |
|---|---|---|
| #49 | `8f4e337` | ✅ success |
| #50 | `472c11a` | ✅ success |
| #51 | `25edc51` | ✅ success |
| #52 | `2d53f46` | ✅ success |

## 5. Qué no prueban estas pruebas

- **Replicate real.** Los formatos de error, los tiempos y la semántica de `cancel` provienen de la documentación pública y se reprodujeron con simuladores, sin cuenta real.
- **Varias instancias de la aplicación.** La exclusión entre procesos se prueba con dos conexiones a la misma base de datos, que es el mecanismo que la garantiza. El límite de frecuencia es **por instancia**: no hay prueba de varias instancias porque el límite no pretende cubrirlas (ver `05_CONTROL_SOLICITUDES_Y_COSTOS.md`).
- **Cuerpo de POST que llega gota a gota.** Un servidor que envía un byte antes de cada límite de lectura no queda acotado por el timeout de socket. Está documentado como limitación en `03_TIMEOUTS_Y_CANCELACION.md`.
