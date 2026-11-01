# Pruebas del ajuste de la traza

**Commit:** a52e35e.

**Entorno:**
- PostgreSQL 16 efímero (Testcontainers);
- dobles de IA: 0 llamadas a Replicate.

## 1. Unitarias: `TrazaV4CTest` (15 pruebas, 0 fallos)

Cada caso compara la traza con el **validador real** (`PlanDraftValidator`). `dentro_limite_v4_aplicado` debe ser `true` **exactamente** cuando el validador no devuelve ningún problema V4.

| Escenario | Prueba | Resultado |
|---|---|---|
| Perfil de 30 min | `limiteAplicadoPorPerfil[30]` | Límite aplicado 2.070 s. 2.069 y 2.070 dentro, 2.071 fuera, igual que el validador |
| Perfil de 45 min | `[45]` | 3.105 s. 3.104 y 3.105 dentro, 3.106 fuera |
| Perfil de 60 min | `[60]` | 4.140 s. 4.139 y 4.140 dentro, 4.141 fuera |
| Perfil de 75 min | `[75]` | 5.175 s. 5.174 y 5.175 dentro, 5.176 fuera |
| Perfil de 60 min, ACORTAR a 30 | `acortarDistingueLosMinutos` | `minutos_perfil = 60`, `minutos_efectivos = 30`, `minutos_disponibles = 30`, `limite_v4_aplicado.segundos = 4140` (`minutos_base = 60`), `limite_v4 = {segundos: 2070}` (informativo, `limite_v4_minutos_base = 30`). Sesiones de 3.000 y 4.140 s dentro, 4.141 fuera, igual que el validador |
| Política histórica, 30/45/60/75 min, declarado en el techo y +1 | `historicaSinCambios` (8) | Regla `V4_MINUTOS`, `minutos_declarados = round(M × 1,15)`, sin `segundos`. `limite_v4` y `v4 = HISTORICO` como antes. Aceptación idéntica a la del validador histórico |
| Histórica con ACORTAR | `historicaConAcortar` | El límite aplicado sigue siendo 69 (perfil de 60), como el validador |
| Sin bloque `prescription` | `sinPrescripcion` | `segundos_estimados = null` y `pct_de_m = null` (no se inventan). Regla `V4_MINUTOS` aunque la política sea V4-C, como el validador. 35 min dentro y 36 fuera, igual que el validador |

**Pruebas anteriores que siguen pasando sobre el mismo código:**

| Prueba | Pruebas |
|---|---|
| `PoliticaPlanificacionTest` | 18 |
| `V4CValidadorRealTest` | 19 |
| `BrechaRedondeoV4Test` | 16 |
| `ContratoV4CIATest` | 3 |

## 2. Integración real: `PoliticaV4CCicloIT.acortarTrazaDiferenciada` (PLAN-V4-0.1)

| Paso | Qué ocurre | Comprobación |
|---|---|---|
| 1. Generación inicial | Perfil GAIN_MUSCLE intermedio, gimnasio, 3 días de 60 min | Traza `GENERACION`, 60 configurados y 60 efectivos |
| 2. Publicación con V4-C | `planning_policy = PLAN-V4-0.1` | Todas las sesiones dentro del límite aplicado |
| 3. Falta de tiempo | Las 3 sesiones se omiten con `LACK_OF_TIME` / `MENOS_TIEMPO_HABITUAL`, 30 min | — |
| 4. Adaptación por el ciclo real | `WeeklyCycleService.runFor` | Idempotente (repetirlo da el mismo plan); 1 plan activo; traza `ADAPTACION`. El decisor actual no fija minutos: `minutos_efectivos = minutos_disponibles = 60` |
| 5. Orden ACORTAR a 30 y publicación | `generateWeeklyPlan` con `forcedSessionMinutes = 30`, que es la vía por la que hoy bajan los minutos efectivos | 1 plan activo; política PLAN-V4-0.1 |
| 6. Traza persistida | `planning_policy_trace` | `minutos_perfil = 60`, `minutos_efectivos = 30`, `limite_v4_aplicado = {V4C_SEGUNDOS, 60, 4140}`, `limite_v4 = {2070}`, `limite_v4_minutos_base = 30`. Las 3 sesiones (1.560, 1.440 y 1.440 s) con `dentro_limite_v4_aplicado = true` |

**La misma clase** también ejecuta `cicloConV4C` (30/45/60/75 min con ACORTAR, dolor y fatiga) y `planesHistoricosIntactos`: **3/3**.

## 3. Integración real: `TrazaHistoricaIT` (PLAN-HIST-1, configuración por defecto)

- Perfil de 60 min con orden ACORTAR a 30.
- Comprobaciones:
  - `planning_policy = PLAN-HIST-1`;
  - `minutos_perfil = 60` y `minutos_efectivos = 30`;
  - `limite_v4_aplicado = {V4_MINUTOS, minutos_base 60, minutos_declarados 69}`, sin segundos;
  - `limite_v4 = {minutos_declarados: 35}`, el informativo de siempre;
  - todas las sesiones dentro;
  - 1 plan activo.
- **Resultado: 1/1.**

## 4. Sin cambios en ejercicios ni dosis

La traza se escribe **después** de validar y no la lee ningún generador ni validador. Para confirmarlo, la comparación de 960 perfiles se repite sobre a52e35e (`COMPARACION_HISTORICA_PR18.md`).
