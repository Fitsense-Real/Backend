# 05 · Regresiones

**Condiciones de todas las ejecuciones:**
- locales, sin `REPLICATE_API_TOKEN` (`env -u REPLICATE_API_TOKEN`);
- PostgreSQL en Testcontainers, sin red hacia el proveedor;
- **0 llamadas a Replicate, USD 0**.

**Base:** `generation` `471296d` (PR #45 y #46 fusionadas).

## 1. Referencia antes de la auditoría

| Suite | Resultado | Fuente |
|---|---|---|
| Unitarias | 1335 / 0 / 1 (ejecutadas / fallos+errores / omitidas) | Regresión de la PR #45 |
| Integración (`-Pintegration`) | 596 / 0 / 44 | ídem |

## 2. Pruebas nuevas

| Prueba | Tipo | PR | Qué comprueba |
|---|---|---|---|
| `O12DolorProgresionApagadaIT` (3) | Integración | técnica (O12) | O12 con la progresión apagada y tres respuestas fijas de la IA: reduce, vacía, sube. Ningún ejercicio excluido por dolor reaparece en las semanas 2–5. Con «sube», la propuesta no se publica (V8C «sube la dosis») |
| `O12DolorProgresionEncendidaIT` (3) | Integración | técnica (O12) | Lo mismo con la progresión encendida |
| `SubidaPorSustitucionTest` (7) | Unitaria | técnica (O12) | El sustituto no puede subir series, repeticiones ni segundos respecto a la semana medida; controles con la misma dosis, bajadas, sin dosis medida y sin sustituciones. Añadida en la PR #47 después de la regresión del §5; con la comprobación anulada fallan las 3 de subida |
| `HG9GeneralFitnessIT` (1) | Integración | documental | Cota inferior de duración de GENERAL_FITNESS por nivel y minutos; si no cabe, el validador da V4; plan real de respaldo |
| `SinTokenReplicateTest` (3) | Unitaria | documental | Sin token (nulo, vacío o en blanco) la IA no es utilizable; con la IA apagada tampoco; tiempos por defecto |

## 3. La prueba falla antes de la corrección

`O12DolorProgresionApagadaIT.subeSeries`, sobre `471296d` sin la corrección:
- **FALLA:** el estado es APLICADA y la semana publicada lleva el sustituto 72 a 4×18, frente al 3×18 medido del ejercicio 991.
- **Con la corrección:** RESPALDO, con el problema «V8C: la sesión … sube la dosis a través del sustituto 72 de 991 (4x18; desde la semana medida corresponde 3x18)».
- `O12DolorProgresionEncendidaIT.subeSeries` también falla sin la corrección, pero **solo por el motivo**: la propuesta ya se rechazaba, por V24 («el ejercicio 72 … no lleva la dosis de ninguna capacidad»), y no se publicaba. La prueba exige el motivo V8C («sube la dosis»), que no depende de la progresión. Con la corrección pasa.
- Las otras 4 pruebas O12 (reduce y vacía, con la progresión apagada y encendida) pasan antes y después: la corrección no cambia los caminos que reducen o no hacen nada.
- Registro: `aud_o12c.log` (antes de la corrección, 7 ejecutadas, 2 fallos) y `aud_fix1.log` (después, 0 fallos), en el directorio de trabajo de la sesión.

## 4. Ejecuciones dirigidas tras la corrección

| Suite | Resultado |
|---|---|
| Unitarias: `DolorSinMasTrabajoV8CTest`, `DosisIaV8CTest` (40), `ProgresionV24V8CTest` (18), `SinTokenReplicateTest` (3) | 67 / 0 / 0 |
| Integración: O12 × 6, `HerenciaD8*`, `RetiradaLocalizada*`, `CorreccionesReplicate{Apagado,Encendido}IT`, `RespaldoConDolor*`, `TextoRespaldoSustitucion*`, `SinSustitutoDolor*` | 39 / 0 / 2 omitidas |

## 5. Regresión completa con la corrección y las pruebas nuevas

`./mvnw -o -B verify -Pintegration` sobre `471296d` + corrección + las 10 pruebas nuevas (19 min 33 s, `BUILD SUCCESS`):

| Suite | Resultado | Diferencia con la referencia |
|---|---|---|
| Unitarias | **1338 / 0 / 1** | +3 (`SinTokenReplicateTest`) |
| Integración | **603 / 0 / 44** | +7 (6 O12 + 1 H-G9) |

Esta regresión es anterior a `SubidaPorSustitucionTest`. La PR #47 se fusionó con ella (merge `5cbea9e`) y el CI de la PR pasó en verde. La regresión final sobre `generation`, después de fusionar las PR #47 y #48, se registra en el informe de cierre.

**Ninguna prueba existente cambió de resultado.** En particular, siguen en verde todas las de V8C, sustitución de seguridad, dolor, progresión, reincorporación, ConfiguracionEvaluada y la cohorte.

## 6. Cohorte O con la progresión apagada y encendida (IA simulada)

Detalle en `02` §3. Resumen:
- **Bloque G (O01–O14):** 52/52 semanas. La única adaptación que cambia es O12 semana 1, de APLICADA a SIN_EFECTO. Además, 21 pasos de PROGRESION.
- **Bloque P (O15–O20):** estados de adaptación idénticos; 35 PROGRESION y 2 REINCORPORACION (O18, O19), solo con la progresión encendida.
- **0 llamadas al proveedor.**

## 7. Análisis de efectos secundarios de la corrección

| Camino | ¿Afectado? | Motivo |
|---|---|---|
| Generación inicial | No | La corrección solo está en `AdaptedWeekProducer.produce` (camino de la IA en la adaptación) |
| Continuidad, respaldo y P-03 | No | No se llama a `subidasPorSustitucion`. Además, parten de la semana medida, así que el sustituto ya tiene la dosis esperada |
| Progresión, recuperación y reincorporación | No | Pasos deterministas del backend, fuera de ese método |
| Adaptación con IA sin sustituciones | No | Lista vacía: sin problemas añadidos |
| Adaptación con IA que reduce o mantiene la dosis del ejercicio sustituido | No | La comparación solo rechaza **subidas** (series, repeticiones o segundos) |
| Adaptación con IA que sube la dosis del ejercicio que luego se sustituye | **Sí, a propósito** | Se rechaza por V8C y se usa el respaldo, que excluye y sustituye igual lo que dolió |
