# 05 · Cierre

**Órdenes:**
- «ORDEN DE TRABAJO — CIERRE H2-v4 Y CORRECCIÓN DE TRANSPORTE REPLICATE»;
- «ORDEN DE TRABAJO — VALIDACIÓN FINAL Y MERGE CONDICIONAL DE PR #88».

**Límites que se respetan:** sin despliegue, sin llamadas pagadas, sin tocar H2, PR-1.7 por omisión.

## 1. H2-v4 (PR-2.2), cerrada

- La PR #87 está **cerrada sin fusionar**: RECHAZADA por P1 (3 > 1) y K2 (75,9 s > 72,9 s).
- La rama `feature/h2-v4-pr22` (`3f27f54b`) conserva su código y su evidencia, sin cambios.

## 2. Resultado técnico

**Causa de G08** (`01`): cabeceras 2xx **sin `Content-Type`**, que Spring da por `application/octet-stream`, y una conexión **reiniciada a mitad del cuerpo**. Se reproduce sin coste.

**Revisión de seguridad (§3 de la orden).** La primera versión de esta PR recuperaba buscando por contenido, y eso **permitía atribuir a una petición la predicción de otra idéntica**.
- Quedó demostrado: contra `f3786e60`, el usuario A publicaba como IA la salida del usuario B (S01, S12, S15).
- **Corregido** en `64e0432d`.
- El contrato oficial de Replicate (OpenAPI 1.0.0-a1) no ofrece otra prueba de identidad que el `id` de la respuesta del POST: ni `Location`, ni idempotencia, ni metadatos.

**Diseño final** (`02`):
- se recupera **solo** por el `id` de la propia respuesta, entero o al principio de un cuerpo cortado;
- ese id se confirma por GET: mismo id, entrada presente e igual a la enviada;
- en cualquier otro caso queda `PREDICCION_NO_CONFIRMADA`, **sin reenviar**, y se publica el respaldo;
- el sondeo tolera fallos pasajeros.

**Un solo archivo de producción:** `ReplicateTrainingPlanGenerator.java`.

**Pruebas:**

| Ejecución | Resultado |
|---|---|
| `ReplicateRecuperacionTransporteTest`: diagnóstico (3), T01–T14 (18) y S01–S15 (15 escenarios) | **36/36** |
| Pruebas inversas (`03` §2) | Contra #88 sin revisar: **6/6** de las de identidad fallan (atribución ajena). Contra `generation`: 28 de 36 fallan. Las mutaciones M1, M2 y M4 rompen las pruebas que las vigilan |
| `mvn verify -Pintegration` completo, local, sobre `64e0432d` (lo posterior es solo documentación) | Unitarias **1.666, 0 fallos**, 1 omitida (1.630 de `generation` más las 36). Integración **887, 0 fallos**, 72 omitidas (las opcionales y de pago), las mismas que en `generation`. **BUILD SUCCESS** (26 min 49 s) |
| Suites clave dentro de esa ejecución | V59 libro (20), V59 asíncrona (23), V59 reanudación (12), V58→V59 (1), gpt-5-mini con O20 (5), correcciones de adaptación (5 + 5), O20, V65, progresión y reincorporación: **todas en verde** |
| CI de GitHub | Ver la PR |

## 3. Condiciones para el merge (orden §10)

| # | Condición | Estado |
|---|---|---|
| 1 | Sin riesgo conocido de adoptar resultados de otra operación | **Sí.** No se busca por contenido; S01, S02, S05, S12 y S15 lo vigilan |
| 2 | Identidad verificada | **Sí:** el id de su propia respuesta, confirmado por GET con la misma entrada |
| 3 | Cabeceras y campos según el contrato real | **Sí:** OpenAPI. `Location` no está documentado y se ignora (S03) |
| 4 | Lo ambiguo termina de forma segura | **Sí:** `PREDICCION_NO_CONFIRMADA` y respaldo (S05, S06, S08, S15) |
| 5 | Sin POST automáticos ante lo incierto | **Sí:** 1 POST en T05, S09, S11 y T12 |
| 6 | Los reintentos por respuestas inválidas siguen | **Sí** (T11, S14) |
| 7 | La recuperación no debilita los validadores | **Sí:** no se tocan, y lo recuperado pasa por ellos (S14) |
| 8 | S01–S15 y T01–T14 aprobadas | **Sí**, 36/36 |
| 9 | Regresión sin fallos | **Sí** |
| 10 | CI en verde | Comprobado antes del merge |
| 11 | Sin conflictos con `generation` | Comprobado antes del merge |
| 12 | PR-1.7 por omisión | **Sí:** `application.properties` no se toca |
| 13 | Sin cambios en validadores, políticas ni prompts | **Sí:** el diff de producción es un solo archivo de transporte |

**Cero llamadas pagadas.** Solo hubo GET gratuitos a `api.replicate.com`: la especificación OpenAPI y, en el cierre de H2-v4, las consultas de diagnóstico.

`main` no se modifica.

## 4. Recomendaciones

1. **Extender V59** a los caminos sin libro (endpoint síncrono, ciclo semanal, adaptación), para sobrevivir a un reinicio con un POST en curso (`04` §5). Es una propuesta aparte, con diseño.
2. **Evaluar `Cancel-After`** (cabecera documentada) igual al límite de cada llamada. Acotaría el coste de una predicción incierta que no se puede cancelar por falta de id. Cambia el comportamiento de todas las predicciones, así que necesita su propia decisión.
3. **Vigilar** en Railway, cuando se despliegue con autorización, las líneas `REPLICATE_RECUPERACION` y `PREDICCION_NO_CONFIRMADA`.

## 5. Merge

El SHA del merge, el estado `MERGED`, la regresión sobre el commit resultante y el CI posterior se registran en el comentario final de la PR #88 y en el informe de entrega.
- Este documento se escribe **antes** del merge, para no añadir commits a `generation` después.
