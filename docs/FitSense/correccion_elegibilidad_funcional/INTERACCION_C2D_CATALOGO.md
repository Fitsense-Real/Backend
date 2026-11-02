# Interacción de C2-D con el catálogo y la elegibilidad funcional

**Fuente:** `MATRIZ_COMPARACION_FUNCIONAL.csv`.
- 960 perfiles × 6 políticas, con el generador y el validador reales.
- Mismos perfiles, `user_id` y semillas que las matrices anteriores.
- PostgreSQL efímero, sin IA, 0 llamadas a Replicate.

## 1. Resultados

| Política | Válidos | Inválidos | Motivos de rechazo |
|---|---|---|---|
| PLAN-HIST-1 | **875** | 85 | V20 79 · V15 6 |
| PLAN-C2D-0.1 | **951** | 9 | V15 6 · V20 3 |
| PLAN-C2D-V4-0.1 | 951 | 9 | Igual que C2-D |
| PLAN-EF-0.1 | 842 | 118 | V20 64 · V20+VEF 32 · VEF 16 · V15 6 |
| **PLAN-C2D-EF-0.1** | **902** | 58 | V20+VEF 32 · VEF 16 · V15 6 · V20 4 |
| PLAN-C2D-V4-EF-0.1 | 902 | 58 | Igual que C2-D-EF |

**Regresión de las versiones existentes:** HIST, C2D y C2D-V4 coinciden **perfil a perfil** con la matriz anterior (`implementacion_controlada_c2d_v4c/RESULTADOS_MATRIZ_C2D.csv`) en resultado, segundos y series. **0 diferencias.**

## 2. Calidad funcional de los planes válidos

| Política | Válidos | Con alguna colocación no computable | PULL sin tracción | PUSH sin empuje | LEGS sin pierna |
|---|---|---|---|---|---|
| PLAN-HIST-1 | 875 | **307** | 57 | 13 | 12 |
| PLAN-C2D-0.1 | 951 | **331** | 73 | 13 | 12 |
| PLAN-EF-0.1 | 842 | **0** | 24* | 12* | 12* |
| PLAN-C2D-EF-0.1 | 902 | **0** | 24* | 12* | 12* |

\* Todas son sesiones de **LOSE_WEIGHT** de **solo cardio** (sin trabajo resistido propio). No afirman una función de tracción, empuje o pierna. VEF no les exige ancla (`ELEGIBILIDAD_FUNCIONAL_EXPERIMENTAL.md` §2.2).

**«Colocación no computable»:** un ejercicio resistido, que no es complemento, cuyo patrón no pertenece a la función de la sesión o que la capa EF no cuenta (tronco fuera de `waist`, 315).

## 3. C2-D frente a C2-D-EF (lo que cambia)

| Transición | Perfiles | Detalle |
|---|---|---|
| Válido → válido, **idéntico** | **620** | Ya eran funcionalmente correctos: el generador conserva el plan de siempre |
| Válido → válido, **cambiado** | **282** | **Los 282 tenían colocaciones no computables.** Se regeneran con EF y siguen siendo válidos |
| Válido → **NO_FACTIBLE** | **48** | Peso corporal, 6–7 días, PUSH/PULL/LEGS (GAIN_MUSCLE, INCREASE_STRENGTH, GENERAL_FITNESS). VEF: dos PULL sin tracción y **0 tracciones elegibles** con el material declarado |
| Válido → inválido (V20) | **1** | R0808 (GENERAL_FITNESS principiante, peso corporal, 6 días, 75 min). Con EF, el plan sin colocaciones no computables queda corto y **C2-D no lo exime**: NO_SATURADA, porque existe una SUSTITUCIÓN (1178 → 1373) que lo mejora. Es un problema de **selección**, no de duración |
| Inválido → válido | **0** | — |

Por equipamiento, los válidos de C2-D frente a C2-D-EF son:

| Equipamiento | C2-D | C2-D-EF |
|---|---|---|
| Gimnasio | 381 | 381 |
| Mancuernas y bandas | 285 | 285 |
| **Solo peso corporal** | 285 | **236** |

Por días, la pérdida se concentra en 6 días (144 → 119) y 7 días (144 → 120). Entre 1 y 4 días no cambia nada.

## 4. ¿C2-D sigue actuando solo cuando corresponde?

| Pregunta | Respuesta | Evidencia |
|---|---|---|
| ¿C2-D publica un plan funcionalmente inválido? | **No.** Con EF, cualquier VEF hace que no todo sea V20, y `aplicaC2D` es falso | 48 NO_FACTIBLE no exentos; `ElegibilidadFuncionalTest.c2dNoEximeVEF` |
| ¿La saturación considera la elegibilidad real? | **Sí.** El verificador evalúa sus movimientos con el mismo validador, VEF incluido. Un movimiento que rompe VEF no demuestra mejora. El borrador base ya no contiene colocaciones no computables | De las 76 SATURADA con C2-D, **60** siguen SATURADA y válidas con C2-D-EF. Las otras **16** pasan a NO_FACTIBLE (V20 + VEF): su saturación se había demostrado sobre contenido sin tracción |
| ¿Convierte problemas de selección en excepciones de duración? | **No.** R0808: con EF el plan queda corto y C2-D lo rechaza por NO_SATURADA. R0040, R0328 y R0616 siguen NO_SATURADA | `c2d_estado` y `c2d_motivo` en la matriz |
| ¿Cambia ejercicios o dosis? | C2-D no. EF cambia la **elección** solo en los 282 planes con colocaciones no computables. **Las dosis 4D no se tocan** | — |

## 5. Lo que se mantiene

- **V4-C:** 902 de 902 válidos de C2-D-EF dentro de `segundos × 100 ≤ M × 60 × 115` (`v4_segundos_ok`). El combinado C2-D-V4-EF da los mismos 902.
- **Sin cambios:** V8C, V15, V18, V22, V23, V24 y V25. P-02 y P-03 en el ciclo real (`LONGITUDINAL_C2D_CORREGIDO.csv`).
- Media de series semanales en los válidos: 55,5 (HIST), 57,3 (C2-D), **54,7** (C2-D-EF). Porcentaje mínimo de M por sesión: 76,4 / 74,9 / 75,1.

## 6. Conclusión

- **No se ha intentado recuperar los 951.** EF elimina **los 49 planes** que C2-D publicaba sin contenido pertinente o que eran cortos por un problema de selección.
- **Ningún plan inválido pasa a válido.**
- Los 902 restantes **no contienen ninguna colocación no computable.**
