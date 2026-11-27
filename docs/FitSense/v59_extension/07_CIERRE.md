# 07 · Cierre

**Orden:** «ORDEN DE TRABAJO — BACKEND: AMPLIACIÓN V59 Y CONTROL DE PREDICCIONES REPLICATE».
**Autorización:** investigar, implementar, probar y preparar la PR. **NO MERGE. NO DESPLIEGUE. NO CONSUMIR REPLICATE.**
**Rama:** `feature/v59-recovery-all-ai-flows`, desde `generation` (`77b9df3d`, con PR #88 fusionada).

## 1. Resultado

| Entregable | Estado |
|---|---|
| **A · Ampliación de V59** | La generación **síncrona** del primer plan y el **cierre semanal** (generación **y** adaptación) tienen el seguimiento V59: operación persistente, lease, fencing, libro de predicciones y adopción tras un reinicio. Hay además un **barrido** que relanza los ciclos interrumpidos. Una sola migración (V66, solo esquema) |
| **B · `Cancel-After`** | Investigación con el contrato OpenAPI y 207 latencias reales. **Recomendación: no activarlo todavía.** Candidata: `5m`, tras una verificación pagada con autorización nueva (`06`). Ningún cambio funcional |

## 2. Pruebas y regresión

| Ejecución | Resultado |
|---|---|
| `OperacionesIaV66IT`: V59-01 a 18, salvo el 15; con reinicio real del contexto en V59-03b | **15/15** |
| `MigracionV65aV66IT` (V59-15) | **1/1** |
| Pruebas inversas M1–M4 | Las cuatro rompen las pruebas que vigilan su protección: 10, 4, 6 y 1 (`04` §3) |
| `mvn verify -Pintegration` completo, local, sobre `b9db1b19` (el código es el de la cabeza; lo posterior es documentación) | Unitarias **1.666, 0 fallos**, 1 omitida. Integración **903, 0 fallos, 0 errores**, 72 omitidas: las 887 de `generation` más las 16 nuevas. Las omitidas son las mismas opcionales y de pago. **BUILD SUCCESS** (32 min 24 s) |
| CI de GitHub (`verify`) | Ver la PR |

**La primera regresión completa dio 48 errores.** Las IT que fijan un catálogo anterior aplican encima las migraciones de solo esquema (`EsquemaVigenteSobreCatalogoAnterior`), y faltaba V66 en esa lista. Hibernate (`validate`) no encontraba las columnas nuevas. Se añadió V66, como se hizo con V58–V65, y la segunda regresión quedó en verde.

**Suites revisadas expresamente dentro de la regresión:**
- V58/V59: `GeneracionAsincrona*`, `ReanudacionConReplicateSimuladoIT`, `MigracionV58aV59IT`;
- transporte PR #88: `ReplicateRecuperacionTransporteTest`;
- gpt-5-mini directo;
- generación inicial, síncrona y asíncrona;
- adaptación: `CorreccionesReplicate*IT`;
- cierre semanal: `CicloConcurrenteIT`;
- O20 y V65, progresión y reincorporación;
- motor de reglas, notificaciones y eliminación de cuentas.

## 3. Condiciones de aceptación (orden §11)

| # | Condición | Estado |
|---|---|---|
| 1 | Todos los caminos de IA identificados | **Sí** (`01` §2): 4 que llaman a la IA, más la regeneración determinista, que no la llama |
| 2 | Seguimiento persistente en los caminos incluidos | **Sí:** `FIRST_PLAN_SYNC` y `WEEKLY_CYCLE` |
| 3 | Predicciones confirmadas recuperables tras un reinicio | **Sí:** V59-01, 03b, 04, 05 y 17 |
| 4 | Operaciones inciertas sin POST automáticos | **Sí:** V59-06 |
| 5 | Sin atribución entre usuarios | **Sí:** V59-07, más PR #88 |
| 6 | Sin publicaciones duplicadas por concurrencia | **Sí:** V59-08 y 09, `CicloConcurrenteIT` |
| 7 | Cierre semanal recuperable sin duplicar planes | **Sí:** V59-02/03, 03b y 18 |
| 8 | Adaptaciones recuperadas que respetan el plan vigente | **Sí:** V59-10 (la antigua queda `DESCARTADA` y se valida sobre el estado actual) |
| 9 | Validadores obligatorios | **Sí:** V59-10, 11 y 12 |
| 10 | O20 intacto | **Sí:** V59-12 y la suite O20 |
| 11 | Respaldo del motor de reglas | **Sí:** V59-06, 11 y 13/14 |
| 12 | Regresión completa en verde | **Sí** |
| 13 | CI en verde | Ver la PR |
| 14 | PR-1.7 activa | **Sí:** no se toca |
| 15 | Sin llamadas pagadas | **Sí.** Solo hubo un GET gratuito de `openapi.json` |

**Limitaciones declaradas, no ocultas** (`05`):
- la vía síncrona solo se reanuda si la persona repite la petición;
- una predicción sin id puede cobrarse aunque no se duplique;
- el ciclo se relanza durante 24 h;
- la publicación del ciclo se protege con la idempotencia de siempre, no con fencing.

## 4. Estado

- La PR queda **abierta, sin merge**, esperando la revisión.
- `generation` y `main` no se modificaron.
- No hubo despliegue ni llamadas a Replicate.
- Prompt PR-1.7 y gpt-5-mini sin cambios; PR-2.2 no se activó.

## 5. Recomendaciones

1. **Revisar y decidir el merge.** V66 se aplica sobre datos existentes sin reescribirlos (V59-15).
2. **Tras desplegar con autorización**, vigilar:
   - `OPERACION_IA_REANUDADA`, `CICLO_SEMANAL_RECUPERACION` y `OPERACION_IA_VENCIDAS`;
   - el recuento `no_confirmadas` de `OPERACION_IA_PREDICCIONES`.
3. **`Cancel-After`:** si se quiere acotar el coste de las predicciones huérfanas, autorizar primero la verificación pagada mínima de `06` §5 y, con su resultado, decidir `5m`.
4. **Mobile:** sin cambios necesarios. Si la app quisiera, podría reintentar `POST /users/me/plan/generate` tras un corte: ahora la reanudación no paga otra predicción.
