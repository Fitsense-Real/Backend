# Regresión final (V59)

Rama `fix/async-generacion-resiliente`, sobre la cabeza de #71 (`12a01c6`). Ejecutada en local con el mismo comando
que el CI (`mvn verify -Pintegration`), con PostgreSQL 16 de Testcontainers migrada desde cero V1..V59, sin
Replicate y con la IA apagada salvo en `ReanudacionConReplicateSimuladoIT`, que usa un Replicate falso en
127.0.0.1.

## Resultado

| Fase | Pruebas | Fallos | Errores | Omitidas | Nota |
|---|---|---|---|---|---|
| Unitarias (surefire) | 1.572 | 0 | 0 | 1 | Incluye `DiarioDePrediccionesReplicateTest` (17) y las de #71: clasificador (6), operación (5), worker (4) |
| Integración (failsafe) | 795 | 0 | 0 | 72 | Las 72 omitidas son las opt-in de siempre, que necesitan una base o un token explícitos. #71 tenía 770; las 25 nuevas son `GeneracionAsincronaResilienciaIT` (20) y `ReanudacionConReplicateSimuladoIT` (5). |
| `BUILD SUCCESS` | — | — | — | — | 24 min 19 s |

Además, en local contra una PostgreSQL vacía (`docker postgres:16-alpine`):

| Suite opt-in | Pruebas | Resultado |
|---|---|---|
| `GeneracionAsincronaIT` (de #71, adaptada a V59) | 15 | 15/15 |

## Qué cubre

| Área | Cómo |
|---|---|
| Unitarias | Libro, clasificación del POST, huella, fencing; worker y clasificador de #71 |
| Integración | Pipeline real, publicación, `PlanAlreadyExists`, NO_FACTIBLE, error técnico |
| Recuperación | Lease vencido, reencolado, `max-executions`, apagado ordenado, `first_started_at` |
| Idempotencia | Libro: reutilización, adopción, `NO_CONFIRMADA` sin reenvío, `DESCARTADA` |
| Concurrencia | 4 workers / 1 reclamo; 8 solicitudes / 1 operación; fencing |
| Migraciones desde una base vacía | Testcontainers V1..V59 en todas las IT; base local vacía para `GeneracionAsincronaIT`; `EsquemaVigenteSobreCatalogoAnterior` con V59 solo de esquema |
| Seguridad | `MockMvc` con Spring Security: propiedad por JWT (404 a otro usuario), contrato HTTP |
| Generación inicial y publicación | Todas las IT de planificación existentes, sin cambios |
| NO_FACTIBLE | `noFactible` y las IT de factibilidad existentes |
| Compatibilidad con Flutter | Contrato idéntico (E1/E2 con `MockMvc`) |
| Catálogo v1.1 y V57 | Suites de catálogo (V36, V37, V47–V50, controles permanentes, elegibilidad) en verde, con ningún archivo del catálogo ni migración ≤ V58 modificados |
| H2 y #69 | Sin tocar. `sinAmbitoNadaCambia` prueba que el camino síncrono (el que usa H2) sigue con `Prefer: wait`. |
| P-03 por colocación | Sigue desactivado; no se toca su configuración |

## Archivos cambiados respecto de #71

### Producción

- **`PlanGenerationOperationService`**: lease, fencing, barrido y recuento.
- **`PlanGenerationOperation`**: tres columnas, y `finish` suelta el lease.
- **`PlanGenerationWorker`**: hilos propios.
- **`ReplicateTrainingPlanGenerator`**: un camino nuevo, solo con ámbito asíncrono.
- **`PlanGenerationPipeline`**: `AI_ATTEMPTS` pasa de privada a pública, sin cambiar el valor.
- **`application.properties`**: un bloque nuevo, documentado.
- **Archivos nuevos**:
  - `PoliticaTiemposGeneracion`;
  - `DiarioDePredicciones`;
  - `JdbcDiarioDePredicciones`;
  - `V59__generacion_resiliente_lease_y_predicciones.sql`.

### Pruebas

- **Nuevas**:
  - `DiarioDePrediccionesReplicateTest`;
  - `GeneracionAsincronaResilienciaIT`;
  - `ReanudacionConReplicateSimuladoIT`.
- **Modificadas**:
  - `GeneracionAsincronaIT`: cuatro cambios de semántica, explicados en `PRUEBAS_REINICIO_BACKEND.md`. Ninguna
    aserción se debilita para ocultar un fallo.
  - `EsquemaVigenteSobreCatalogoAnterior`: más V59.

### Documentación

- `docs/generacion-inicial-asincrona.md`, con las cifras actualizadas.
- Esta carpeta.

No se modifican validadores, catálogo, migraciones V1..V58, H2, progresión, reincorporación ni adherencia
(comprobado con `git diff --name-only 12a01c6`).
