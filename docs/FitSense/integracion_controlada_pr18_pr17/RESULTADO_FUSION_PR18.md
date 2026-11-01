# Resultado de la fusión del PR #18 en `generation`

## 1. Fusión

| | Valor |
|---|---|
| PR | Fitsense-Real/Backend#18 |
| Paso previo | Se quitó el estado de borrador (GitHub no fusiona borradores) |
| Método | **Merge commit**, el mismo que en los PR anteriores (#15, #16): conserva los 5 commits del PR y su trazabilidad |
| Cabeza fusionada | fa7402b, comprobada con `expectedHeadSha` |
| **Commit resultante en `generation`** | **bfd57563d0e594e190b5e0b6068f6332558b43e8** («Merge pull request #18 from Fitsense-Real/feature/v4c-versionado-aislado») |
| Estado del PR | `merged = true` |

## 2. Comprobaciones posteriores

| Comprobación | Resultado |
|---|---|
| `generation` contiene V4-C y el versionado | **Sí:** el árbol de bfd5756 es **idéntico** al de fa7402b (`git diff fa7402b bfd5756` vacío) |
| Cambios inesperados | **Ninguno:** afef98a..bfd5756 cambia los mismos 41 archivos que el PR |
| V44 | **Un único archivo**, SHA-256 `640b0169…e2708400` |
| V45 | No está en `generation` (sigue solo en el PR #17) |
| Política por defecto | `PLAN-HIST-1` |
| `PLAN-V4-0.1` | **Sin activar:** `allow-candidates=false` y ninguna propiedad de configuración lo selecciona |
| CI tras el `push` a `generation` | Se lanzó **solo** el workflow `CI` (ejecución #94, `mvn verify` con Testcontainers): **success**. Además se ejecutó #95, el CI del PR #1, cuya rama es `generation`: **success** |
| Despliegue o migración remota | **Ninguno** iniciado por esta sesión. Ningún workflow de despliegue existe ni se ejecutó |

**La autorización cubría solo la fusión de código:** no se ha desplegado, no se ha activado V4-C y no se ha tocado Neon ni Railway.

**Suscripción a la actividad del PR #18:** cancelada, porque el PR está cerrado.
