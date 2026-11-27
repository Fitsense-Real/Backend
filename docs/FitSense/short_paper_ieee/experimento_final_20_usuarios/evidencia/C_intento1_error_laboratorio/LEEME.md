# Etapa C, intento 1: invalidado por un error del laboratorio (0 predicciones reales)

| Campo | Valor |
|---|---|
| Fecha | 2026-11-03 01:36 UTC |
| Laboratorio | `46e2366` |
| Qué pasó | El espía del proveedor calculaba la huella con `String.valueOf(inv.getArgument(0))`. Java lo resolvió como `valueOf(char[])` y lanzó `ClassCastException` **antes** de llamar a Replicate |
| Contador del laboratorio | 28 «llamadas». Se incrementaba antes del fallo: **no son predicciones** |
| Predicciones reales | **0**. Comprobado con `GET /v1/predictions`: ninguna creada desde el 2026-11-03; la última es del 2026-10-31 |
| Coste | **US$ 0** |
| Efecto en los datos | Los 20 gemelos dan «NO_FACTIBLE» (`ClassCastException` en `generacion.csv`). E3/E4 quedan sin plan siguiente. U01–U20 en E1, E2 y E5 coinciden con A1 |
| Categoría | **A** (laboratorio) |
| Corrección | `c63bad2`: todo el trabajo del laboratorio va antes de contar y no puede romper la llamada |
| Registro de Maven | No se conservó: se borró por error al relanzar. Los CSV de esta carpeta son la evidencia íntegra del intento |

Se conserva sin editar, como pide la orden. La ejecución válida de la etapa C está en `evidencia/C`.
