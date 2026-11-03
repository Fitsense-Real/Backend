# Estado de los PR #28 y #29 y de su evidencia

**Fecha de la comprobación:** 03/11/2026.

**Rama de esta evaluación:** `experiment/adaptation-v2-quality-validation`, creada desde `51c145b` (cabeza del PR #29).

**Nada de los PR #28 y #29 se ha modificado.**

## SHA

| Elemento | Rama | SHA | Relación |
|---|---|---|---|
| Backend de referencia | `generation` | `0974fb3eafc39e8b7397b4dbfe7c4405e4847440` | Antecesor de #28 y #29 |
| PR #28 (experimento 20 usuarios) | `lab/experimento-kora-20usuarios` | `0f17ac770a94dda2b31fc4ec384e36cff390a117` | Base de #29 |
| PR #29 (prompt V2, C1/C2) | `experiment/adaptation-prompt-v2` | `51c145b2887e621acc52fc81605f58e64edd0a5c` | Contiene #28 |
| Código evaluado en las 4 corridas C1/C2 | — | `f23bda9`, `a6e695b`, `3430e7b` y `0f4bffc` (registrados en cada `00_configuracion.txt`) | **Sin cambios en `src/` ni en `pom.xml`** desde el protocolo congelado `259bd94` |

## Integridad de la evidencia

Cada archivo de evidencia en el árbol de trabajo se comparó con el *blob* guardado en git en el SHA de su PR (`git hash-object` frente a `git ls-tree`):

| Carpeta | SHA | Archivos | Distintos |
|---|---|---:|---:|
| `experimento_final_20_usuarios/` (A0, A1, A1 repetición, C, intentos fallidos, perfiles, predicciones, registros) | `0f17ac7` | 143 | **0** |
| `experimento_prompt_v2/` (diagnóstico, GUION v1/v2, C1 r1, C2 r1, C2 r2, C1 r2, prompts, predicciones, registros, entregables) | `51c145b` | 216 | **0** |

**La evidencia está íntegra.** Esta evaluación solo **lee** esas carpetas y escribe en `evaluacion_calidad_adaptacion_v2/`.

## CI del PR #29 (corrección de un informe anterior)

**En el informe anterior dije que el PR #29 no tendría CI** porque su base no era `main` ni `generation`. **Era un error de lectura del *workflow*.**

`.github/workflows/ci.yml` se dispara con `pull_request:` **sin filtro de rama**. El filtro `branches: [main, generation]` solo afecta a `push`.

| Campo | Valor |
|---|---|
| *Workflow* | `CI` (`.github/workflows/ci.yml`), job `verify`: `mvn -B -ntp verify -Pintegration` |
| Ejecución | [41577492912](https://github.com/Fitsense-Real/Backend/actions/runs/41577492912) (*run* 158), evento `pull_request` |
| SHA | `51c145b2887e621acc52fc81605f58e64edd0a5c` (la cabeza del PR) |
| Fecha | 03/11/2026, de 04:19:58 a 04:33:22 UTC |
| Resultado | **success** |
| Integración (failsafe) | **489 ejecutadas, 0 fallos, 0 errores, 37 omitidas** (*opt-in*) |
| Unitarias (surefire) | Aprobadas: `BUILD SUCCESS` en un `verify` que las ejecuta antes. **El recuento exacto no se extrajo**, porque el proxy de esta sesión bloquea la descarga del ZIP de registros; la API devuelve solo el final del registro |
| Problemas | Ninguno. El registro muestra conexiones rechazadas a PostgreSQL al apagar contenedores, y el aviso de que surefire termina su JVM; son las mismas trazas de siempre al cerrar y no fallan ninguna prueba |

**El CI de esta rama** (`experiment/adaptation-v2-quality-validation`) corre en su propio PR. Ver `15_CI_Y_REGRESION.md`.
