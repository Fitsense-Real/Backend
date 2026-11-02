# Implementación de V4-C

**Condición:** `segundosEstimados × 100 ≤ minutosPerfil × 60 × 115`, en aritmética entera (`long`). El límite exacto es `M × 69` s (`PlanningPolicy.limiteV4Segundos`).

**Solo con `PLAN-V4-0.1` o `PLAN-C2D-V4-0.1`.** Con la histórica, V4 es exactamente el de siempre.

## 1. Dónde se aplica el mismo límite

| Ruta | Cambio | Histórica |
|---|---|---|
| **Validador** (`PlanDraftValidator.validateSessions`) | Con V4-C, compara los segundos estimados (`SessionDurationEstimator.estimateSeconds`, la fórmula de V17 y V20) con el límite. El mensaje da los segundos y el techo en segundos. **Ignora lo declarado** | Minutos declarados frente a `round(M × 1,15)` |
| **Constructor por capacidades** (`CapabilityWeekBuilder`) | Las 9 comparaciones con el techo pasan por `excede(...)` → `policy.excedeTecho(segundos, minutos, M)` | `excedeTecho` histórico = `minutos > round(M × 1,15)`, la misma comparación que antes |
| **Motor 4D** (`RuleBasedTrainingPlanGenerator`) | El recorte final por techo usa `excedeTecho` | Rama histórica intacta (`minutos > techo`) |
| **Contexto de la IA** (`PlanInputSnapshot.constraints.max_session_seconds`) | Con V4-C viaja el límite en segundos, y el prompt añade una frase: «ninguna sesión puede pasar de N s… aunque declares menos minutos» | El campo es `null` y **no se serializa** (`@JsonInclude(NON_NULL)`): JSON y texto idénticos |
| **Corrección de sesiones (adaptación con IA)** (`SessionFixInputSnapshot`, `SessionFixPromptBuilder`) | El mismo campo y una frase equivalente | Sin cambios |
| **Revalidación, adaptación y regeneración segura (P-03)** | Usan el validador con el contexto de su operación, que lleva la versión | Sin cambios |

**No es un cambio de `floor` por `round`:** con V4-C se validan los **segundos estimados reales**.

**Base de M.** V4-C usa `profile.sessionMinutes()`, la misma base que el V4 histórico (no los minutos forzados por un ajuste). Así una sesión acortada no recibe un techo distinto del de siempre.

## 2. Pruebas

| Prueba | Casos |
|---|---|
| `V4CValidadorRealTest.bordes` (**validador real**) | 30, 45, 60 y 75 min: límite exacto, −1 s y +1 s, con duración declarada correcta, menor y mayor que la estimada (17 casos) |
| `V4CValidadorRealTest.historicaIntacta` | La histórica sigue comparando lo declarado: 60 declarados en un perfil de 30 se rechazan; 35 declarados con 2.071 s pasan |
| `V4CValidadorRealTest.caso142` | **El caso del 142 %** (7 × 3×10/90 s = 2.550 s, declarado 35 min, perfil de 30): la histórica lo admite; **V4-C lo rechaza** («dura unos 2550 s… techo 2070 s»); la combinada también; C2-D sola no cambia V4 |
| `PoliticaPlanificacionTest.v4cBordes` / `v4Historico` | Aritmética del límite y equivalencia exacta de la rama histórica con `round(M × 1,15)` |
| `ContratoV4CIATest` | El snapshot histórico no contiene `max_session_seconds`; con C2-D el JSON es idéntico al histórico; con V4-C el único cambio es `"max_session_seconds":2070`; en el prompt, la única diferencia es una frase |

**Segundos fraccionarios.** El estimador trabaja en segundos enteros (calentamiento, repeticiones × 3 s, descansos y transiciones son enteros), así que no hay fracciones. El redondeo al minuto solo existe en lo **declarado**, que V4-C ya no usa.

**Campaña histórica.** 0 de 456 sesiones cambian de resultado con V4-C (recalculado en la verificación anterior). Con la política histórica, la campaña REPLAY se vuelve a ejecutar en esta rama (`REGRESION_COMPLETA.md`).

## 3. Divergencia generador–validador

**Eliminada para V4-C:** el generador, el validador y la IA usan `PlanningPolicy.excedeTecho` o `limiteV4Segundos` con la misma M. En la matriz de 960 perfiles, ningún plan generado con V4-C supera el límite en segundos (columna `v4_segundos_ok` de `RESULTADOS_MATRIZ_C2D.csv`).
