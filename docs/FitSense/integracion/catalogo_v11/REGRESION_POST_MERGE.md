# Regresión posterior al merge (fase 4)

## Problema

Tras el merge hay que comprobar sobre el `generation` real que el resultado es exactamente el de la candidata v1.1 probada, y que nada se ha roto.

## Condiciones

| | |
|---|---|
| Commit | **`50849d0f4b5c979cd980bdaceea1dfd7dd550ebc`**, `origin/generation` tras el merge de #67 |
| Árbol | `96dc37e450c925098cf792a68627e58479707b60` |
| Entorno | Worktree aislado (`wt_cons_reg`) con HEAD desacoplado en ese SHA y `target` borrado antes de empezar |
| Orden | `sh mvnw -o verify -Pintegration`, seguida de las ejecuciones opt-in de las matrices |
| Base de datos | PostgreSQL 16 efímero (Testcontainers). Cada clase de integración aplica Flyway V1…V56 **desde una base vacía** y valida con Hibernate `ddl-auto=validate` |
| Configuración de pruebas | La de la candidata, con P-03 por colocación encendida (`src/test/resources/config/application.properties`). En producción sigue apagada |
| Árbol de trabajo | `git status --porcelain` vacío antes de la regresión, después de ella y al terminar las matrices |
| Horario | 14:26:53–14:52:00 UTC; matrices hasta las 14:55 UTC |

## 1. Regresión completa

| | v1.1 (`12c6897`) | **Post-merge (`50849d0`)** |
|---|---|---|
| Unitarias (surefire) | 1540 · 0 fallos · 0 errores · 1 omitida | **1540 · 0 · 0 · 1** |
| Integración (failsafe) | 754 · 0 · 0 · 57 omitidas (opt-in) | **754 · 0 · 0 · 57** |
| Maven | BUILD SUCCESS, 31:39 min | **BUILD SUCCESS, 25:05 min** |

**Por clase:**

- **Surefire:** 143 clases, idéntico a v1.1 (`evidencia/surefire_por_clase.tsv`).
- **Failsafe:** 157 clases, idéntico salvo dos líneas, `CatalogoCongeladoIT` y `CatalogoIntegralComparacionIT`. En v1.1 aparecen con 0 omitidas y ahora con 1. Esta vez los informes se copiaron **antes** de ejecutar las dos a propósito; en v1.1 se copiaron después, y la ejecución opt-in había sobrescrito sus XML (`INFORME_MERGE_CATALOGO.md`, diferencia 754 frente a 749).
- **En los dos casos la regresión las omite**, porque son opt-in, y las matrices las ejecutan aparte (§2).

**Clases que cubren lo pedido** (todas sin fallos):

| Área | Clases |
|---|---|
| Referencia de 72 conservadora | `GeneracionReferenciaConservadoraIT`, más las referencias históricas V35–V40 |
| HALL-PROG-01 y progresión | `ProgresionIT` (24) y `ProgresionRev2IT` (24) |
| Dolor y planes publicados | `ProteccionPlanesPublicadosIT` (12), `RegeneracionPorColocacionRev2IT` (6, 1 opt-in) y `Plan78ContencionReducidaIT` (4) |
| Adaptación localizada y ampliada | `RetiradaLocalizadaLimiteIT` (4), `RetiradaLocalizadaV8CTest` y `DolorSinMasTrabajoV8CTest` |
| P-03, por defecto y por colocación | `P03ComparacionPorColocacionIT` (2) y `ComparacionDosisPorColocacionTest` (10); interruptor apagado en `application.properties` |
| D8 | `HerenciaD8IT` (6, 2 opt-in) |
| Reincorporación y ejecución | `PendientesProgresionIT` y `PendientesProgresionRev2IT`, `CopiaSinIaIT` y `CopiaSinIaRev2IT`, y los escenarios de reincorporación de `ProgresionIT` y `ProgresionRev2IT`; `EjecucionEntrenamientosIT` (25). `ReincorporacionIaRealIT` es opt-in, porque necesita Replicate, y no se ejecuta |
| Contención | `ContencionTextoBanderaIT`, `ContencionTextoBanderaTest` y `CatalogoIntegralSeguridadIT` |
| Configuración evaluada | `ConfiguracionEvaluadaIT` (3) y `ConfiguracionEvaluadaTest`, con PR-1.7, V2.1 y PLAN-REV-2.0 |
| Esquema y migraciones | `FitsenseApplicationIT`, `MigracionV43IT` y las IT que usan el soporte `EsquemaVigenteSobreCatalogoAnterior` (`SOLO_ESQUEMA` V51/V53, V54 y V55): `ContencionTextoBanderaIT`, `Usuario31ReproduccionIT` y `RetiradaLocalizadaLimiteIT` |

## 2. Matrices opt-in, comparadas byte a byte con la evidencia de v1.1

| Matriz | Resultado post-merge | Comparación con v1.1 (`premerge_final/evidencia/`) |
|---|---|---|
| Rejilla de 168, contención **encendida** | 168/168 con plan | **Idéntica byte a byte** (SHA-256 `c21bcf53…`) |
| Rejilla de 168, contención **apagada** | 168/168 con plan | **Idéntica** (`5b6c6284…`). Por tanto, `impacto_168.tsv` es el mismo |
| Rejilla de 312 de hipertrofia | 298 VALIDO, 14 FALLO (V20, peso corporal del 775), `violaciones.txt` vacío | **Idéntica** (`539368a9…`; violaciones `e3b0c442…`, vacío) |
| HALL-GM-01 (40), P-03 por colocación encendida | 35 publicadas sin aumentos, 5 NO_FACTIBLE, nada reaparece, `trabajo_tras_dolor = 0` en las 40 | **Idéntico** (`98631d14…`) |
| Catálogo reconstruido desde PostgreSQL vacío (×2) | **REPRODUCIBLE** entre las dos reconstrucciones | **Huella normalizada idéntica a v1.1:** `exercises` `20d6a795…` (1376), equipamiento `a23c6843…` (1522), aprobaciones `f79df277…` (4) y Flyway `57c239d3…` (59) |

Las sumas y las huellas completas están en `evidencia/`:

- `SHA256_salidas_post_merge.txt`;
- `SHA256_referencias_v11.txt`;
- `HUELLA_NORMALIZADA.txt`.

## 3. Comprobaciones de integración

| Comprobación | Resultado |
|---|---|
| SHA final | `50849d0` para el catálogo. Tras #68, `0d11278` (`INTEGRACION_V57.md`) |
| Código idéntico al probado | `git diff 12c6897 50849d0 -- src pom.xml .github` vacío |
| Migraciones | V1…V56, 59 aplicadas desde una base vacía en cada IT. Ninguna duplicada ni reordenada |
| Duplicados | HALL-PROG-01 aplicado una sola vez. `ProgresionIT#p02ProgresionSinElExcluido` pasa |
| Interruptores | Los aprobados (`ESTADO_INTERRUPT0RES.md`) |
| Seguridad | V15, V20, V24, V25, V8C, D8 y P-03 sin relajar. Las matrices de seguridad de `REGRESION_INTEGRADA_FINAL.md` §3 pasan |
| Despliegue | Ninguno (`INFORME_MERGE_CATALOGO.md`) |
| CI de GitHub | Verde en `a824420` (paso 10) y en la cabeza de #68 (`b69af7f`, regresión completa sobre `generation` + V57) |

## Resultado

**La regresión posterior al merge pasa** y reproduce exactamente v1.1. No hay nada que corregir.

## Pendientes

Ninguno para el catálogo.
