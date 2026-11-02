# Regresión final del backend (PR #17 en 51a311f)

**Entorno:**
- PostgreSQL efímero (Testcontainers) y `mvn -o`.
- **IA simulada; 0 llamadas a Replicate.**
- Sin Neon y sin Railway.

**Una sola ejecución en la copia de trabajo:** se detuvo una cadena anterior duplicada que compartía `target/` y se relanzó limpia.

**Las pruebas omitidas no cuentan como aprobadas.**

## 1. Resultados

| Bloque | Contabilizadas | Ejecutadas | Superadas | Fallidas | Omitidas |
|---|---:|---:|---:|---:|---:|
| Unitarias (`mvn verify`) | 1.046 | 1.046 | 1.046 | 0 | 0 |
| Integración (`-Pintegration`) | 498 | 441 | 441 | 0 | **57** |
| Batería PLAN-HIST-1 (15 clases) | 225 | 223 | 223 | 0 | 2 |
| Batería PLAN-C2D-0.1 | 225 | 223 | 223 | 0 | 2 |
| Batería PLAN-C2D-EF-0.1 | 225 | 223 | 223 | 0 | 2 |
| Batería PLAN-C2D-V4-EF-0.1 | 225 | 223 | 223 | 0 | 2 |
| Ciclo real de 10 semanas (6 configuraciones) | 6 | 6 | 6 | 0 | 0 |
| 960 perfiles × 5 políticas (volcado) | 1 | 1 | 1 | 0 | 0 |
| Campaña P-1/2/3 cohorte 02, **REPLAY** (100 semanas) | 2 | 1 | 1 | 0 | 1 (LIVE, IA real: no autorizado) |

**Las 57 omitidas de la integración** son todas *opt-in* o de IA real. Frente a cc97045 (493/438/55) se suman:
- `RespaldoConDolorIT`: 3 pruebas ejecutadas;
- `AnalisisSustitucion1373IT`: 2 omitidas.

**Las omitidas de las baterías** son 2 escenarios *opt-in* de `ValidacionMultisemanaIT`, igual que antes.

## 2. Suites exigidas por la orden

| Suite | Resultado |
|---|---|
| V4-C (baterías con `PLAN-C2D-V4-EF-0.1` y ciclo real `PLAN-C2D-V4-0.1`/`-V4-EF-0.1`) | Sin fallos |
| RESPALDO y P-03 | `RespaldoConDolorIT` («Respaldo forzado: con dolor y sustituto, sin dolor, y con dolor sin sustituto») 3/3; `PoliticasP123IT` 39/39; `AdaptationOutcomeTest` 4/4 |
| Progresión y reincorporación | `ProgresionIT` 24/24; progresión al 100 % 5/5; `Reincorporacion4DTest` 5/5; «solo una PROGRESIÓN posterior… habilita la reincorporación» 7/7 |
| V8C y V24 | V8C sin subir dosis 40/40; dolor 6/6; excepción por seguridad 8/8; carga 10/10; retirar relleno 10/10; V34 (V24 + V8C) 16/16; validadores V23/V24/V25 6/6 |
| Concurrencia e idempotencia | «Ciclo semanal concurrente: bloqueo de sesión por participante» 9/9 |
| Multisemana | `ValidacionMultisemanaIT` 57 (55 ejecutadas, 2 omitidas), sin fallos, en la ejecución completa y en las cuatro baterías |

## 3. Comparación histórica

| Comprobación | Resultado |
|---|---|
| 960 perfiles × 5 políticas, `planes.csv` | **Idéntico byte a byte** a las referencias anteriores (SHA-256 `c582fce2…`), con 4.800 filas |
| `colocaciones.csv` | **Idéntico** (SHA-256 `ee9d2111…`) |
| Válidos por política | PLAN-HIST-1 875; C2D-0.1 951; C2D-V4-0.1 951; C2D-EF-0.1 902; C2D-V4-EF-0.1 902: **los mismos que antes** |
| Perfiles EF identificados (regla VEF) | **48**, los mismos |
| REPLAY (100 semanas, IA simulada) | **Idéntica**. Solo cambian dos marcas de tiempo (`recorded_at`, `reactivated_at`) en `exclusiones_dolor.csv`; se descartaron y no se confirmaron. El resto de la evidencia (planes, V32, historial, métricas) no cambia |

**Conclusión:** el resultado histórico es **idéntico** y los 48 perfiles EF siguen identificados.
