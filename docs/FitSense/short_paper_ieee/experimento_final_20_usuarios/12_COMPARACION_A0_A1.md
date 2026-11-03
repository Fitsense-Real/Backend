# Comparación A0 / A1 / repetición de A1

| Ejecución | Laboratorio | Backend | Base | Carpeta |
|---|---|---|---|---|
| A0 | v1 `911a0d3` | `0974fb3` | PostgreSQL 16 efímero, vacía | `evidencia/A0` |
| A1 | v2 `2c0cc8b` | `0974fb3` (sin correcciones: ninguna era necesaria) | Nueva, vacía | `evidencia/A1` |
| A1, repetición | v2 `2c0cc8b` | `0974fb3` | Nueva, vacía | `evidencia/A1_repeticion` |

## Resultado: idénticas

| Archivo | A0 | A1 | Repetición de A1 |
|---|---|---|---|
| `generacion.csv` | 57f4a247… | 57f4a247… | 57f4a247… |
| `plan_inicial.csv` (planes inicial y siguiente, ejercicio a ejercicio) | 3b61f395… | 3b61f395… | 3b61f395… |
| `ejecucion.csv` (60 sesiones, dosis prescrita y ejecutada) | e5ef00ad… | e5ef00ad… | e5ef00ad… |
| `adherencia.csv` | fa434a8f… | fa434a8f… | fa434a8f… |
| `plan_siguiente.csv` | 25b6fd91… | 25b6fd91… | 25b6fd91… |
| `decisiones.csv`, columnas comunes | — | 0 diferencias | 0 diferencias |
| `resultados_20.csv`, columnas comunes | — | 0 diferencias | 0 diferencias |

**Lo único que A1 añade** son las columnas `api_*`, que no existían en el laboratorio v1.

**Conclusiones:**
- La ejecución determinista es **reproducible**: los mismos identificadores, semillas, planes, métricas, decisiones, estados V32 y planes siguientes en tres bases distintas.
- Como no hubo correcciones, A1 confirma A0. No mide ninguna mejora.

| Indicador | A0 | A1 |
|---|---:|---:|
| Planes iniciales publicados | 20/20 | 20/20 |
| Aw, Aobs y C iguales al oráculo (±0,01) | 18, 18, 20 | 18, 18, 20 |
| Métricas iguales al recálculo independiente | 20/20 | 20/20 |
| Decisión igual al oráculo | 18/20 | 18/20 |
| Adaptaciones aplicadas | 0 | 0 |
| Respaldos | 8 | 8 |
| Conservaciones deliberadas | 2 | 2 |
| NO_FACTIBLE | 0 | 0 |
| Violaciones | 0 | 0 |
