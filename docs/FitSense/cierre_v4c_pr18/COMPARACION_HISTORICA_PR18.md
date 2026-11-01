# Comparación histórica del PR #18 (generación anterior intacta)

## 1. Método

**Prueba:** `ComparacionPerfilesIT` (opt-in), los mismos 960 perfiles, semillas y orden de usuarios de las matrices anteriores.

**Para cada perfil y política** se genera con el motor de reglas, se valida con el validador real y se vuelcan:
- `planes.csv`: resultado, reglas, excluidos por dolor, sesiones y segundos;
- `colocaciones.csv`: fecha, enfoque, segundos de la sesión, orden, ejercicio, patrón, clase, tipo, series, repeticiones, segundos y descanso.

| Versión | Commit | Políticas |
|---|---|---|
| `generation` | afef98a | PLAN-HIST-1 |
| PR #18 anterior | d1de25c | PLAN-HIST-1 y PLAN-V4-0.1 |
| **PR #18 final** | **a52e35e** | PLAN-HIST-1 y PLAN-V4-0.1 |

## 2. Resultados

| Comparación | Resultado |
|---|---|
| `generation` frente a a52e35e, PLAN-HIST-1 | **Idénticos línea a línea:** 960 de 960 planes y 19.560 de 19.560 colocaciones |
| d1de25c frente a a52e35e (HIST y V4-C) | **Idénticos byte a byte** (`planes.csv` y `colocaciones.csv`, mismo SHA-256) |
| PLAN-HIST-1 frente a PLAN-V4-0.1 en a52e35e | Mismos recuentos: **875 válidos / 85 inválidos** en las dos. Ningún plan del motor de reglas cambia con V4-C |

**Conclusión:**
- La corrección de la traza **no cambia ningún ejercicio, dosis, descanso, composición, duración ni resultado**.
- La configuración histórica sigue siendo **idéntica** a la de `generation`.

## 3. Huellas (SHA-256)

| Archivo | SHA-256 |
|---|---|
| a52e35e y d1de25c · `planes.csv` | `d109a3daa156d6d28bdfbebf109e5b5066bee8213c3663b21ad7b177dd18a859` |
| a52e35e y d1de25c · `colocaciones.csv` | `d227848ba040e73af459b052ab518f67d0b80af55cc731cbe2a455f5abe265ef` |
| `generation` · `planes.csv` | `761d67cfba8f01b66e571ea6ea25890b12d0043fb718ad4acd4f60043e348a8d` |
| `generation` · `colocaciones.csv` | `fa9fa3edc200ae123a69427cbe35f0e03bc5e0c0a8e63ecbcf1a04a1685619d4` |

Los archivos de `generation` solo contienen PLAN-HIST-1. Los del PR #18 contienen HIST y V4-C; la comparación se hace sobre las filas de HIST.

## 4. Otras pruebas del comportamiento histórico (en la ejecución completa de a52e35e)

**Configuración por defecto:** las 433 pruebas de integración ejecutadas, sin fallos.

**Comprobaciones explícitas de la histórica:**
- `GeneracionIAHistoricaIT`: la propuesta de la IA se rechaza con la V4 de siempre.
- `V4CValidadorRealTest.historicaIntacta`.
- `TrazaV4CTest.historicaSinCambios` (8 casos de aceptación).
- `TrazaHistoricaIT`.
- `ContratoV4CIATest`: JSON y prompt de la IA idénticos con la histórica.
