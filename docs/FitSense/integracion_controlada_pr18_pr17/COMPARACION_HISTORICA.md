# Comparación histórica tras la integración

**Prueba:** `ComparacionPerfilesIT` (opt-in). Mismos 960 perfiles, semillas y orden de usuarios.

**Para cada perfil y política se vuelcan:**
- el resultado, las reglas y los excluidos por dolor;
- por ejercicio: ejercicio, patrón, clase, series, repeticiones, segundos y descanso;
- por sesión: la duración y la composición.

## 1. Resultados

| Comparación | Políticas | Resultado |
|---|---|---|
| PR #17 actualizado (1e42519) frente al PR #17 anterior (254a18e) | Las 5 (HIST, C2-D, C2-D-V4, C2-D-EF, C2-D-V4-EF) | **Idénticos byte a byte**: mismo SHA-256 en `planes.csv` y `colocaciones.csv` |
| PR #17 actualizado frente a `generation` | PLAN-HIST-1 | **Idénticos línea a línea**: 960/960 planes y 19.560/19.560 colocaciones |
| `generation` después del PR #18 frente a `generation` antes (afef98a) | PLAN-HIST-1 | Idénticos. El árbol de bfd5756 es el del PR #18, cuya comparación dio 960/960 (`cierre_v4c_pr18/COMPARACION_HISTORICA_PR18.md`) |

**Recuentos** (sin cambios):

| Política | Válidos | Inválidos |
|---|---|---|
| PLAN-HIST-1 | 875 | 85 |
| PLAN-C2D-0.1 | 951 | 9 |
| PLAN-C2D-V4-0.1 | 951 | 9 |
| PLAN-C2D-EF-0.1 | 902 | 58 |
| PLAN-C2D-V4-EF-0.1 | 902 | 58 |

**Los 48 perfiles de peso corporal sin tracción:**
- con EF: inválidos por VEF, 48/48 con las dos políticas EF;
- sin EF (PLAN-C2D-0.1): se publicarían los 48, como ya se sabía.

## 2. Huellas (SHA-256)

| Archivo | SHA-256 |
|---|---|
| PR #17 actualizado y anterior · `planes.csv` | `c582fce21cac5bd731226871015ac222096f6637e6babc244ec349bd69574ee2` |
| PR #17 actualizado y anterior · `colocaciones.csv` | `ee9d21118e65679dd68f2886c40e2c35f7206da5407a47792e6651bb2ccaa3d9` |

## 3. Ciclo real y campaña

**Campaña REPLAY de 100 semanas** (política por defecto):
- evidencia idéntica salvo las marcas de tiempo de `exclusiones_dolor.csv`;
- comparada con las marcas normalizadas, es igual;
- se revirtió.

**Ciclos reales y trazas de las cuatro políticas del PR #17:** se ejecutan dentro de las baterías (§2 de `REGRESION_PR17_ACTUALIZADO.md`), sin fallos.

## 4. Conclusión

- La integración del PR #18 **no cambia ningún plan** de ninguna política: ni ejercicios, ni dosis, ni descansos, ni duración, ni resultado.
- Solo añade trazabilidad.
- La configuración histórica sigue siendo idéntica a la de `generation`.
