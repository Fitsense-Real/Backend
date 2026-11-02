# Regresión del PR #17 actualizado

**Código probado:** **1e42519** (fusión de `generation` en `feature/c2d-v4c-versionado`). El commit siguiente, 697ff0e, solo añade documentación.

**Entorno:**
- PostgreSQL 16 efímero (Testcontainers);
- `fitsense.ai.enabled=false` y dobles de IA: **0 llamadas a Replicate**;
- sin Neon ni Railway.

## 1. Incidencia de entorno (no de código)

**Qué pasó:**
- El primer intento falló en todas las pruebas con contenedor: «Could not find a valid Docker environment».
- El demonio de Docker de la sesión se había detenido.
- La cadena se detuvo y Docker se reinició.

**Cómo se repitió:**
- La regresión completa se repitió entera, con una comprobación de Docker antes de cada fase, en `/tmp/claude-0/pr17m2/`.
- No se reutilizó ningún resultado del primer intento.

## 2. Resultados

| Ejecución | Contabilizadas | Ejecutadas | Aprobadas | Fallidas | Omitidas |
|---|---|---|---|---|---|
| Unitarias (`mvn -o verify -Pintegration` completa) | 1.028 | 1.028 | 1.028 | 0 | 0 |
| Integración (la misma ejecución) | 492 | 438 | 438 | 0 | **54** |
| Batería de 15 clases con `PLAN-HIST-1` | 225 | 223 | 223 | 0 | 2 |
| Batería con `PLAN-C2D-0.1` | 225 | 223 | 223 | 0 | 2 |
| Batería con `PLAN-C2D-EF-0.1` | 225 | 223 | 223 | 0 | 2 |
| Batería con `PLAN-C2D-V4-EF-0.1` | 225 | 223 | 223 | 0 | 2 |
| `ComparacionPerfilesIT` (960 × 5 políticas) | 1 | 1 | 1 | 0 | 0 |
| Campaña REPLAY de 100 semanas (`CampanaP123Cohorte02IT`) | 2 | 1 | 1 | 0 | 1 |

**Omitidas (no cuentan como aprobadas):**
- **54** en la integración: laboratorios opt-in y pruebas que exigen IA real. Son las mismas 54 que antes de la fusión.
- **2** en cada batería: las de siempre.
- **1** en la campaña: la de siempre.

**CI de GitHub (`verify`) en el PR #17 (697ff0e, código 1e42519):** **success** (ejecución #96).

### CI en rojo desde el 2 de noviembre (no es de este PR)

**Qué falla:** el CI del commit siguiente, f913d4d (solo documentación), ejecutado el **lunes 2026-11-02**, falla con 8 pruebas:
- 6 × `GeneracionReferencia*IT`;
- `ValidacionMultisemanaIT`;
- `Usuario31ReproduccionIT`.

**Por qué no es de este PR:**
- En local fallan igual en `generation` (bfd5756) **y en `generation` antes del PR #18 (afef98a)**.
- Las tres clases calculan su semana con `LocalDate.now()`, y sus valores esperados corresponden a la semana del 2 de noviembre.
- Es un **defecto de las pruebas de `generation`, dependiente de la fecha**. No viene de este PR ni de la integración.

**Arreglo propuesto:**
- fijar la semana de referencia (`LocalDate.of(2026, 11, 1)`) en `GeneracionReferenciaBase`, `ValidacionMultisemanaIT` y `Usuario31Base`;
- probado en local: 60 pruebas, 0 fallos.

**No aplicado:** queda pendiente de autorización. Está documentado en el comentario del PR #17.

## 3. Comparación con el PR #17 antes de la fusión (361d270 / 6d6e522)

| | Antes | **Ahora** | Diferencia y causa |
|---|---|---|---|
| Unitarias | 1.013 | **1.028** | +15: `TrazaV4CTest` (del PR #18) |
| Integración ejecutadas | 436 | **438** | +2: `PoliticaV4CCicloIT.acortarTrazaDiferenciada` y `TrazaHistoricaIT` (del PR #18) |
| Integración omitidas | 54 | **54** | Sin cambios |
| Baterías HIST / C2-D / C2-D-EF | 223/225 cada una | **223/225** | Sin cambios |
| Batería C2-D-V4-EF | No ejecutada antes en esta forma | **223/225** | Nueva en esta orden |

## 4. Pruebas pedidas por la orden

| # | Grupo | Dónde | Resultado |
|---|---|---|---|
| 1 | Regresión completa | §2 | 0 fallos |
| 2 | V44 y V45 | `MigracionV44IT`, `MigracionV45IT`, `MigracionV43IT`, `EsquemaVigenteSobreCatalogoAnterior` | 0 fallos |
| 3 | Trazabilidad de V4-C | `TrazaV4CTest` (15), `PoliticaV4CCicloIT` (3), `TrazaHistoricaIT` (1) | 0 fallos |
| 4–7 | Baterías HIST, C2-D, C2-D-EF y C2-D-V4-EF | §2 | 0 fallos |
| 8 | D8, ACORTAR, P-02 y P-03 | `HerenciaD8IT`, `AdaptacionPorCausasIT`, `DuracionForzadaIT`, `AdaptacionCompletaIT`, `PoliticasP123IT` (39) y `LinajeIntegridadIT`, en la completa y en las 4 baterías | 0 fallos |
| 9 | Progresión, reincorporación, concurrencia e idempotencia | `ProgresionIT` (24), `ProgresionCienPorCienIT` (5), `Reincorporacion4DTest`, `CicloConcurrenteIT` y ciclos idempotentes de `PoliticaV4CCicloIT` | 0 fallos |
| 10 | 960 perfiles | `COMPARACION_HISTORICA.md` | Idénticos |
| 11 | REPLAY de 100 semanas | §2 | Idéntica salvo marcas de tiempo (`exclusiones_dolor.csv`, revertido) |

## 5. Condiciones de la orden

| Condición | Resultado | Evidencia |
|---|---|---|
| La política histórica permanece idéntica | **Sí** | 960/960 iguales a `generation`; REPLAY idéntica; batería HIST 223/225 |
| C2-D no modifica ejercicios ni dosis por sí solo | **Sí** | Volcado de las 5 políticas idéntico byte a byte al anterior, que ya mostraba que C2-D solo cambia la publicación (control positivo de `preparacion_integracion_pr17/COMPARACION_960_PERFILES.md` §3) |
| EF impide la falsa cobertura de tracción | **Sí** | `PoliticasP123IT` con EF (`sinFalsaCobertura`) en las baterías C2-D-EF y C2-D-V4-EF |
| Los 48 perfiles sin material de tracción siguen identificados | **Sí** | Con PLAN-C2D-EF-0.1 y PLAN-C2D-V4-EF-0.1, los 48 son inválidos por VEF (48/48) |
| V4-C mantiene su techo exacto | **Sí** | `TrazaV4CTest`, `V4CValidadorRealTest` y `BrechaRedondeoV4Test` |
| Las trazas corresponden a decisiones reales | **Sí** | `VERIFICACION_TRAZABILIDAD.md` |
| Sin nuevas infracciones de V15, V24 ni V8C | **Sí** | El volcado de 960 × 5 es idéntico al anterior; baterías sin fallos |
| Sin restauración automática de ejercicios | **Sí** | Reincorporación explícita en las baterías; 0 excluidos que vuelvan (`PoliticaV4CCicloIT`) |
| Sin progresiones adicionales por reintentos | **Sí** | `ProgresionIT`, `ProgresionCienPorCienIT` y la idempotencia de los ciclos |

**No se exigió ningún número mínimo de planes válidos.** Los recuentos (875, 951, 902…) son los mismos que antes.
