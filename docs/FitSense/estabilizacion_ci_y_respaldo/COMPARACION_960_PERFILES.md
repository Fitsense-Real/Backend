# Comparación de los 960 perfiles

**Prueba:** `ComparacionPerfilesIT` (*opt-in*, `-Dfitsense.comparacion.perfiles=true`). Son los mismos 960 perfiles, con las mismas semillas y el mismo orden de usuarios, para las 5 políticas (HIST, C2-D, C2-D-V4, C2-D-EF y C2-D-V4-EF).

**Código:** PR #17 en **7b6f666** (fusión de `generation` dd61837 más el reloj de `CicloRealPoliticaBase`).
- 97befb6 solo cambia dos pruebas que no intervienen en este volcado (`LinajeIntegridadIT` y `PoliticasP123IT`).
- `src/main` es idéntico al de 73b7008 y 1e42519.

**Entorno:** PostgreSQL efímero, sin IA y **0 llamadas a Replicate**.

## 1. Huellas

| Archivo | PR #17 antes de esta orden (1e42519) | **PR #17 ahora (7b6f666)** | ¿Igual? |
|---|---|---|---|
| `planes.csv` (4.800 filas = 960 × 5) | `c582fce21cac5bd731226871015ac222096f6637e6babc244ec349bd69574ee2` | `c582fce21cac5bd731226871015ac222096f6637e6babc244ec349bd69574ee2` | **Idéntico byte a byte** |
| `colocaciones.csv` (96.832 filas) | `ee9d21118e65679dd68f2886c40e2c35f7206da5407a47792e6651bb2ccaa3d9` | `ee9d21118e65679dd68f2886c40e2c35f7206da5407a47792e6651bb2ccaa3d9` | **Idéntico byte a byte** |

**Qué incluye el volcado:**
- por perfil y política: resultado, reglas incumplidas, estado y motivo de C2-D, excluidos por dolor, sesiones y segundos;
- por ejercicio: ejercicio, patrón, clase, series, repeticiones, segundos y descanso.

**«Idéntico» significa** que no cambia ningún ejercicio, dosis, descanso, duración ni resultado, en ninguna de las 5 políticas.

## 2. Política histórica frente a `generation`

| Comparación | Resultado |
|---|---|
| PR #17 con `PLAN-HIST-1` frente a `generation` | **Idénticos línea a línea** (960/960 planes y 19.560/19.560 colocaciones), por transitividad: el volcado del PR #17 no cambia, y el `src/main` de `generation` no cambia entre bfd5756 (la referencia comparada en `integracion_controlada_pr18_pr17/COMPARACION_HISTORICA.md`) y dd61837 (`git diff bfd5756 dd61837 -- src/main` vacío) |
| `generation` antes y después del PR #19 | Mismo `src/main`, así que mismos planes |

## 3. Recuentos (sin cambios)

| Política | Válidos | Inválidos | Inválidos por VEF |
|---|---|---|---|
| PLAN-HIST-1 | 875 | 85 | — |
| PLAN-C2D-0.1 | 951 | 9 | — |
| PLAN-C2D-V4-0.1 | 951 | 9 | — |
| PLAN-C2D-EF-0.1 | 902 | 58 | **48** |
| PLAN-C2D-V4-EF-0.1 | 902 | 58 | **48** |

**Los 48 perfiles de peso corporal sin tracción** siguen identificados por EF (48/48 con las dos políticas EF).

## 4. Condiciones de la orden (fase 3)

| Condición | Resultado |
|---|---|
| Comportamiento histórico idéntico | **Sí** (§2) |
| 0 cambios inesperados en ejercicios, dosis o descansos | **Sí**: volcado idéntico byte a byte |
| Los 48 perfiles siguen identificados con EF | **Sí** (48/48) |
| La corrección de fechas no cambia decisiones del motor | **Sí**: el PR #19 y los commits 7b6f666 y 97befb6 solo tocan `src/test`. Las pruebas de referencia de `generation` (`GeneracionReferencia*IT`, `Usuario31ReproduccionIT`) siguen comparando los mismos valores esperados, ahora con la semana fija con que se capturaron |
