# 05 · Cierre

**Orden:** «ORDEN DE TRABAJO — CIERRE H2-v4 Y CORRECCIÓN DE TRANSPORTE REPLICATE».
**Autorización:** cerrar la PR #87; investigar y corregir el transporte con pruebas sin coste; preparar una PR independiente.
**NO MERGE. NO DESPLIEGUE. NO GASTOS DE IA.**

## 1. H2-v4 (PR-2.2) cerrada

- La **PR #87 se cerró sin fusionar**, con un comentario de cierre: **RECHAZADA** por P1 (3 perfiles frente a un máximo de 1) y K2 (75,9 s frente a 72,9 s).
- Su rama `feature/h2-v4-pr22` (cabeza `3f27f54b`, CI en verde) **se conserva**, con:
  - el código de PR-2.2;
  - el informe `06_EVALUACION_REAL_Y_DECISION.md`;
  - el libro, con 104 reservas;
  - las 103 respuestas oficiales y la sensibilidad de G08.
- No se modificó nada de esa evidencia. PR-1.7 sigue por omisión.

## 2. Resultado técnico

**Causa** (`01`):
- Cabeceras 2xx **sin `Content-Type`**: Spring lo da por `application/octet-stream`.
- Una conexión **reiniciada a mitad del cuerpo**, que lanza `RestClientException` «Error while extracting response…».
- El camino sin libro convertía **cualquier** fallo del POST en «proveedor caído», sin id con que seguir la predicción, que terminó y se facturó.
- Se reproduce sin coste, con el mensaje exacto.

**Corrección** (`02`), en un solo archivo de producción (`ReplicateTrainingPlanGenerator.java`):
- una clasificación única del POST: no enviada, rechazada, incierta o creada;
- recuperación de la predicción existente por la API documentada (listado y consulta por id, con la misma `input`, solo si hay una);
- `PREDICCION_NO_CONFIRMADA` sin reenviar cuando no aparece;
- la misma recuperación al reanudar un intento `SOLICITANDO`;
- un sondeo que tolera fallos pasajeros en lugar de cancelar.

**Pruebas:**

| Ejecución | Resultado |
|---|---|
| `ReplicateRecuperacionTransporteTest` (T01–T14 y diagnóstico) | **25/25** |
| Prueba inversa (las 25 contra el cliente de `generation`) | **20 fallan** con el cliente anterior; 25/25 pasan con la corrección (`03` §2) |
| `mvn verify -Pintegration` completo, local | Unitarias **1.655, 0 fallos**, 1 omitida: las 1.630 de `generation` más las 25 nuevas. Integración **887, 0 fallos**, 72 omitidas (las opcionales y de pago), las mismas que en `generation`. **BUILD SUCCESS** (28 min 57 s) |
| CI de GitHub (`verify`) | Ver la PR |

**Incluido en la suite:** generación inicial, adaptación, integración con gpt-5-mini (`ReplicateGpt5MiniDirectoTest`), estados y errores de Replicate, reintentos, respaldo `RULE_ENGINE`, O20, V65, validadores, persistencia del libro V59 (`GeneracionAsincronaResilienciaIT`, `ReanudacionConReplicateSimuladoIT`, `MigracionV58aV59IT`) y concurrencia.

## 3. Condiciones de aceptación (orden §13)

| # | Condición | Estado |
|---|---|---|
| 1 | G08 reproducido sin gasto | **Sí** (`g08ReproducidoConElClienteDeProduccion`) |
| 2 | Causa identificada con evidencia | **Sí.** El *qué* está demostrado; el *quién* no (`04` §1) |
| 3 | Recupera predicciones existentes cuando es posible | **Sí:** T02, T03, T04, T06, T07, T08, T13 y T14 |
| 4 | Sin duplicados ante respuestas recuperables | **Sí:** 1 POST en todos los casos; 0 al reanudar |
| 5 | Estados inciertos con un tratamiento seguro | **Sí:** `PREDICCION_NO_CONFIRMADA`, sin reenviar, y respaldo (T05). Limitaciones en `04` |
| 6 | No se relajan validadores | **Sí:** no se tocan, y lo recuperado pasa por los mismos (T11) |
| 7 | Los reintentos por plan inválido siguen | **Sí** (T11, `ReplicateRespuestasInvalidasTest`) |
| 8 | `RULE_ENGINE` disponible | **Sí** (T05, `ReplicateRespuestasInvalidasTest`) |
| 9 | Generación y adaptación sin regresiones | **Sí:** suite completa y T14 |
| 10 | Pruebas en verde | **Sí** |
| 11 | CI en verde | Ver la PR |
| 12 | PR-1.7 vigente | **Sí:** no se toca `application.properties` |

**Cero llamadas pagadas.** Solo hubo consultas `GET` gratuitas a Replicate, hechas durante el cierre de H2-v4 para diagnosticar, y ninguna en esta PR.

`main` y `generation` no se modificaron.

## 4. Recomendaciones

1. **Revisar y decidir el merge de esta PR.** Corrige un riesgo real de coste y de calidad: el plan válido de la IA se perdía.
2. **Propuesta aparte, sin hacer aquí: extender el libro V59** a los caminos sin libro (endpoint síncrono, ciclo semanal y adaptación). Así sobrevivirían a un reinicio con un POST en curso (`04` §4). Exige diseño, porque la tabla está ligada a `plan_generation_operations`.
3. **Vigilar en Railway**, cuando se despliegue con autorización, las líneas `REPLICATE_RECUPERACION` y `PREDICCION_NO_CONFIRMADA`. Su frecuencia dirá si los cortes eran del proxy de la evaluación o también ocurren en producción.
4. **No retomar H2-v5** hasta decidir esta PR, como pide la orden.
