# Validación de tiempos de la generación asíncrona

#71 trataba como un solo número (600 s desde la ejecución actual) varios tiempos que son distintos. V59 los separa,
y `PoliticaTiemposGeneracion` comprueba al arrancar que sean coherentes: si no lo son con la IA encendida, la
aplicación **no arranca**. El diagnóstico original está en `DIAGNOSTICO_TIMEOUT_600S.md`.

## 1. Los cinco tiempos que pide la orden

| Tiempo | Valor | Qué mide | Qué pasa al vencer |
|---|---|---|---|
| **Cliente HTTP de Flutter** | 20 s (`dio_client.dart`) | Cada petición de la app: `POST` y `GET` de la operación | Error de conexión. La app reintenta con espera creciente (3 → 20 s); **no** es un fallo de la generación. |
| **Espera de una predicción** | ≤ 331 s | POST corto (conectar 10 + leer 90), sondeo 150 s, último GET (1 + 10 + 30) y cancelación (10 + 30) | Se pide cancelar y la fila del libro queda `TIMEOUT` o `CANCELED`. El pipeline sigue con el siguiente intento o con el respaldo validado. |
| **Tiempo máximo sin latido** | — | Ya no existe como regla. Un `updated_at` antiguo con el lease vigente **no** reencola (prueba `latidoAtrasadoConLeaseVigenteNoReencola`, 181 s). | — |
| **Vencimiento del lease** | 90 s, renovado cada 20 s desde un hilo propio | Si el proceso propietario sigue vivo | Huérfana: vuelve a `PENDING` sin propietario, hasta `max-executions` (3). |
| **Tiempo máximo de la operación** | 1.800 s desde `first_started_at` (no se reinicia al reencolar) | Red de seguridad ante un defecto | `FAILED / TIEMPO_EXCEDIDO`, reintentable. El resultado tardío no se publica y queda en el libro para el reintento. |

## 2. Política completa y justificación

| # | Parámetro | Valor | Justificación | Origen del valor |
|---|---|---|---|---|
| 1 | Predicción, cota | 331 s | Suma de los límites HTTP de R-3 y R-2 (`ReplicateProperties.esperaMaximaConCancelacionSegundos`) | **Cálculo** sobre la configuración |
| 2 | Pipeline, peor caso | 1.113 s = 3 × 331 + 120 | 3 intentos de IA (`AI_ATTEMPTS`), más validación, respaldo y publicación. Los 120 s son un margen holgado: se miden decenas de ms. | **Cálculo**. Sin cancelación serían 3 × 291 = 873 s y con cancelación 993 s, las cifras de la estimación anterior. |
| 3 | Latido | 20 s | Coste despreciable (un `UPDATE` indexado). Deja 4 latidos por lease. | **Provisional**, sin medición en Railway |
| 4 | Lease | 90 s | Al menos dos latidos por lease (se comprueba al arrancar): una pausa de GC o un fallo de red puntual no basta para declarar muerto al worker | **Provisional** |
| 5 | Operación | 1.800 s | ≥ peor caso (se comprueba al arrancar), con 11 min de margen. Igual que el máximo en cola (`max-queued-seconds`). Nunca infinito. | **Provisional**. La cota inferior es un cálculo. |
| 6 | Recuperación tras un reinicio | ≤ 90 s (lease) + ≤ 30 s (barrido, `recover-ms`) + ≤ 2 s (reclamo, `poll-ms`) ≈ 2 min | Con SIGTERM es inmediata: se devuelve a la cola sin gastar una ejecución | **Cálculo** |
| 7 | Reintentos | 3 intentos de IA por ejecución, sin repetir los anotados. 3 ejecuciones por operación. Un `NO_CONFIRMADA` nunca se reenvía. El reintento manual adopta lo pagado (24 h). | Ninguno es ilimitado. Una reanudación no vuelve a pagar. | Diseño |
| 8 | Estado incierto | `NO_CONFIRMADA`: no se reenvía, cuenta como fallo del proveedor y el pipeline publica su respaldo validado | Replicate no ofrece una idempotencia verificable (`RECUPERACION_PREDICCIONES.md`) | Diseño conservador |

## 3. Lo medido con Replicate real

Todo esto viene de campañas anteriores (`docs/FitSense/rendimiento_generacion/`, 40 generaciones de GRA y 48 del
estrés). En esta orden **no** se llamó a Replicate.

| Medida | Valor |
|---|---|
| Una predicción de generación | mínimo 35,7 s, p50 67,6 s, p95 133,8 s, máximo 144,9 s |
| Una generación completa (con reintentos) | p50 75,8 s, p95 268,7 s, máximo 350,1 s |
| Con tres intentos | 265–350 s típicos; 394 s y 460 s observados en el tercer intento |
| Peor caso teórico de la configuración anterior (con `Prefer: wait`) | ~919 s |

**Conclusión.** El máximo observado (460 s) y el peor caso calculado (1.113 s) caben en los 1.800 s. Con #71, el
límite de 600 s ya estaba por debajo de generaciones con tres intentos largos posibles según esas cotas, y del
propio peor caso.

## 4. Validación con pruebas deterministas

Sin Replicate real. Los tiempos se fijan en la base, sin esperar.

| Comprobación | Prueba | Resultado |
|---|---|---|
| Operaciones vivas de 30, 90, 300, 600, 900 y 1.500 s no vencen | `GeneracionAsincronaResilienciaIT.generacionesLargasVivasNoVencen` | PASS |
| 1.801 s pasa a `FAILED / TIEMPO_EXCEDIDO`, sin lease | `superaElLimiteDeLaOperacion` | PASS |
| El límite cuenta desde la primera ejecución aunque se reencole | `elLimiteNoSeReiniciaAlReencolar` | PASS |
| Un latido atrasado 181 s con el lease vigente no reencola | `latidoAtrasadoConLeaseVigenteNoReencola` | PASS |
| **Una generación que lleva 1.000 s viva recibe tras una espera larga una salida válida y se publica como IA**, con el barrido corriendo durante la espera | `ReanudacionConReplicateSimuladoIT.salidaValidaTrasUnaEsperaLarga` | PASS (`generation_source = AI`, 1 POST) |
| Vence de verdad con la predicción en curso: `FAILED`, sin publicar ni pagar más. La salida tardía queda en el libro y el reintento la adopta. | `venceDeVerdadDuranteUnaPrediccion` | PASS (1 POST y después 2 más, 3 en total) |
| Una configuración incoherente no arranca | `PoliticaTiemposGeneracion.comprobar` (validación al arrancar) | — |

## 5. Pendiente de la prueba con IA real

- Confirmar en Railway que el lease (90 s) y el latido (20 s) no producen reencolados falsos, con métricas de
  `lease_expires_at` y los logs del barrido.
- Medir la cola real del worker (`max-concurrent=2`) bajo concurrencia de usuarios.
- Ver si Replicate conserva las salidas lo bastante para reanudar una `ENVIADA` horas después (retención no
  verificada).
