# Regresión final (#71 + #72)

Rama `fix/async-generacion-resiliente` (#72), sobre `claude/async-first-plan-generation` (#71), sobre `generation`
(`2578719`).

Se ejecutó en local con el mismo comando que el CI (`mvn verify -Pintegration`): PostgreSQL 16 de Testcontainers
migrada desde cero (V1..V59), sin Replicate, con la IA apagada salvo en `ReanudacionConReplicateSimuladoIT`, que
usa un Replicate falso en 127.0.0.1. Fecha de la ejecución: **lunes 16/11/2026**.

## Resultado

| Fase | Pruebas | Fallos | Errores | Omitidas | Nota |
|---|---|---|---|---|---|
| Unitarias (surefire) | **1.576** | 0 | 0 | 1 | +4 respecto de la entrega anterior: 3 del libro (carreras, fencing) y 1 del worker (barrido) |
| Integración (failsafe) | **805** | **2** | 0 | 72 | Las 72 omitidas son las opt-in de siempre (base o token explícitos). +10: R02, R15/R20 y unicidad del libro en `GeneracionAsincronaResilienciaIT`; 6 en `ReanudacionConReplicateSimuladoIT`; 1 en `MigracionV58aV59IT` |
| Opt-in `GeneracionAsincronaIT` (#71) contra una base local vacía | 15 | 0 | 0 | 0 | — |

### Los 2 fallos **no son de esta PR**

`ReparacionLinajeIT.repararYHeredar` y `ReparacionLinajeRev2IT.repararYHeredar` fallan. El mensaje es:
*P-P2: la semana con la dosis progresada se puede reparar … estado=NO_VALIDA … V24: el ejercicio 104 del 2026-12-09
lleva 2x10 … exige 2x13*.

| Comprobación | Resultado |
|---|---|
| ¿Fallan en `generation` (`2578719`) sin #71 ni #72? | **Sí**, el mismo fallo (ejecutado hoy en un worktree limpio de `origin/generation`) |
| ¿Pasaban antes? | Sí: la regresión local del 15/11 (795/0) y el CI de #71 y #72 de ese día, en verde |
| Causa | La prueba fija sus semanas con `LocalDate.now()` (`S1 = lunes de la semana siguiente`). Con la semana que empieza el 16/11, la reparación de una semana progresada sale `NO_VALIDA` por V24. Depende de la fecha y está en reparación/progresión (P-P2/H6), un código que #71 y #72 no tocan. |
| Qué se ha hecho | Nada en el código: la orden prohíbe tocar la progresión y una prueba no se cambia para ocultar un fallo. Queda como **hallazgo para investigar por separado** (`RIESGOS_RESIDUALES.md`). |

## Qué cubre

| Área | Pruebas |
|---|---|
| Unitarias | Libro (20), worker (5), clasificador (6), transiciones (5); Replicate (R-1, R-2, R-3, sondeo, tiempos) |
| Integración de la generación | Pipeline real, publicación, `PlanAlreadyExists`, NO_FACTIBLE, error técnico (`GeneracionAsincronaResilienciaIT`, `GeneracionAsincronaIT`) |
| #71 específicas | `GeneracionAsincronaIT` (15) más las unitarias de #71 |
| #72 específicas | `DiarioDePrediccionesReplicateTest`, `GeneracionAsincronaResilienciaIT`, `ReanudacionConReplicateSimuladoIT`, `MigracionV58aV59IT` |
| Concurrencia | 4 workers → 1 reclamo; 8 peticiones → 1 operación; 10 usuarios con doble toque y el worker real; dos instancias; carrera en el libro |
| Persistencia | Libro JDBC, restricciones de la base, rollback de la publicación (R16) |
| Migraciones | V1..V59 desde una base vacía; V58 → V59 con datos (`MIGRACIONES_V58_V59.md`) |
| Seguridad | `MockMvc` con Spring Security: propiedad por JWT, 404 a otro usuario |
| Generación inicial y validadores | Todas las IT de planificación y catálogo existentes (V15, V20, V24, V25, V8C, D8, P-03 por colocación, catálogo v1.1), en verde |
| NO_FACTIBLE | `noFactible` y las IT de factibilidad existentes |

## Restricciones comprobadas

- Cambios respecto de `generation`: solo los archivos de la generación asíncrona.
  - `src/main/java`: 16 archivos. Los de #71, más `DiarioDePredicciones`, `JdbcDiarioDePredicciones`,
    `PoliticaTiemposGeneracion` y el camino con ámbito de `ReplicateTrainingPlanGenerator`.
  - `PlanGenerationPipeline`: solo `AI_ATTEMPTS`, que pasa de privada a pública.
- `application.properties`: solo se añaden las propiedades `fitsense.plan-generation.*`. **Ninguna** bandera
  experimental cambia: P-03 por colocación, la validación de capacidades en la adaptación y las demás conservan su
  valor por omisión.
- Sin cambios en el catálogo v1.1, V57, las migraciones V1..V58, los validadores V15, V20, V24, V25 y V8C, D8,
  P-03, H2 y #69, la progresión, la reincorporación ni la fórmula de adherencia.
- No se consumió Replicate, no se tocaron ni Railway ni datos reales, y no hubo merge ni despliegue.

## CI de GitHub

El estado del CI de cada cabeza está en `ESTADO_PR71_PR72.md`.
