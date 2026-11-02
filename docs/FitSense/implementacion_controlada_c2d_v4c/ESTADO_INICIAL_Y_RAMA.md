# Estado inicial y rama

## 1. Git

| Elemento | Estado |
|---|---|
| Rama estable | `generation` en `afef98a` (merge del PR #16, P-01 30–75). Sin cambios en esta orden |
| Rama de investigación | `feature/modelo-e-planificacion` (`022dd5e`). **No se ha fusionado**: contiene el modelo E y laboratorios no aprobados. Se usa solo como referencia (documentos y CSV de evidencia) |
| Rama de trabajo | **`feature/c2d-v4c-versionado`**, creada desde `origin/generation` (`afef98a`) |
| Portado de la investigación | Solo las ideas, reescritas como código de producción versionado: política, V4 en segundos, saturación de C2-D, métrica aeróbica y contrato de duración. **No se ha portado** el modelo E (`ParametrosModeloE`, `ConstructorSemanaModeloE`, `ValidadorModeloE`) ni los laboratorios de la investigación |
| `main` | Sin tocar |

## 2. Migraciones

| Migración | Estado |
|---|---|
| V1–V43 | **Sin modificar** (incluida V43, de P-02 y P-03) |
| **V44** `planning_policy_version` | Nueva y solo de esquema: añade `weekly_training_plans.planning_policy` (`VARCHAR(32)`, nullable, con lista cerrada mediante `CHECK`) y `planning_policy_trace` (`JSONB`, nullable). No rellena nada: `NULL` = plan anterior = `PLAN-HIST-1` |
| Dónde se aplicó | **Solo en PostgreSQL 16 efímero** (Testcontainers) durante las pruebas. Añadida a `EsquemaVigenteSobreCatalogoAnterior.SOLO_ESQUEMA` para las pruebas con catálogo fijado |
| Neon / Railway | **No se ha conectado ni migrado nada.** Fusionar esta rama y desplegarla aplicaría V44: requiere una orden explícita |

## 3. Aislamiento de ejecución

| Comprobación | Cómo |
|---|---|
| JDBC efectivo | Cada IT nuevo afirma `jdbc:postgresql://localhost:` (contenedor efímero) antes de cada prueba |
| Flyway | `spring.flyway.enabled=true` sobre la base efímera; `ddl-auto=validate` comprueba que la entidad coincide con V44 |
| Base destino | Un contenedor por clase de prueba; los ciclos reales usan cada uno el suyo para conservar los user_id y las semillas |
| Replicate | `fitsense.ai.enabled=false`, `fitsense.ai.api-token=` vacío y `@MockitoBean ReplicateSessionFixGenerator`. **0 llamadas** |
| `calculation_configs` | No se modifica. El ciclo real afirma que el piso sigue al 70 % |
| Despliegue | Ninguno |

## 4. Políticas en la configuración por defecto

| Propiedad | Valor por defecto | Efecto |
|---|---|---|
| `fitsense.planning.policy` | `PLAN-HIST-1` | Generación y validación históricas |
| `fitsense.planning.policy.allow-candidates` | `false` | Una versión candidata no arranca sin este permiso: el contexto falla, en lugar de activarla por error |
| `fitsense.api.duracion-estimada.enabled` | `false` | La API no cambia |
| `fitsense.metricas.aerobica.enabled` | `false` | La API no cambia |
| `fitsense.generation.lose-weight.preferir-grupo-no-cubierto` | `false` | La composición de LOSE_WEIGHT no cambia |

**Todo está desactivado por defecto. No hay un interruptor único**: cada versión del catálogo es una combinación explícita y cada opción tiene su propiedad.
