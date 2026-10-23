# Adenda 4A v6: capacidades por objetivo

**Fecha:** 23/10/2026  
**Sustituye a:** nada. **4A v5 se conserva congelado y sin modificaciones**; esta adenda registra la decisión posterior y la versión que implementa el backend.  
**Implementación:** `planning/domain/model/valueobjects/GoalCapabilityPolicy.java` (`VERSION = "4A-v6"`).  
**Estado en el backend:** solo observación. Se registra en el log (`CAPACIDADES_4A`). No rechaza planes ni entra en el prompt.

## 1. Cambio respecto a 4A v5

4A v5 exigía para pérdida de peso, además de `CARDIO`, la preservación de masa magra (`LEAN_MASS_PRESERVATION`) como capacidad principal.

**Decisión v6:** para `LOSE_WEIGHT`, `CARDIO` es la **única** capacidad principal y `STRENGTH` pasa a ser secundaria. La preservación de masa muscular sigue siendo una consideración relevante para la prescripción, pero no es una capacidad principal obligatoria.

`ENDURANCE` aparece históricamente en 4A/4B, pero no es un objetivo del enum `GoalType` del MVP. Por eso queda fuera de la matriz.

## 2. Matriz vigente

| `GoalType` | Principales | Secundarias |
|---|---|---|
| `GAIN_MUSCLE` | `HYPERTROPHY` | `STRENGTH` |
| `INCREASE_STRENGTH` | `STRENGTH` | `HYPERTROPHY`; `POWER` condicional |
| `LOSE_WEIGHT` | `CARDIO` | `STRENGTH` |
| `GENERAL_FITNESS` | `STRENGTH`, `MUSCULAR_ENDURANCE`, `CARDIO`, `MOBILITY` | `HYPERTROPHY`; `BALANCE` oportunista |

## 3. Reglas

1. **Una capacidad secundaria nunca hace fallar un plan que cumple sus capacidades principales.** La única respuesta de aceptación de la política (`Assessment.primariesSatisfied()`) depende solo de las principales. Las secundarias ausentes se informan.
2. Una capacidad principal debe cubrirse en la **semana**, no necesariamente en cada sesión.
3. Una capacidad principal es siempre incondicional; la clase rechaza construir una principal condicional.
4. **`POWER` condicional:** no aplica a participantes `BEGINNER`, porque 4D la declara estructuralmente inalcanzable para ese nivel. Para los demás niveles es una secundaria más. *Interpretación de implementación pendiente de confirmar metodológicamente: la condición se limita al nivel.*
5. **`BALANCE` oportunista:** se incluye si el conjunto elegible ofrece una ruta compatible. Su ausencia nunca invalida el objetivo.

## 4. Lo que esta versión no hace todavía

- Calcular qué capacidades cubre un plan concreto (pasos 3 y 4). `evaluate()` recibe ese conjunto ya calculado.
- Cambiar filtros de catálogo (paso 2), 4C (paso 3) o 4D (paso 4).
- Viajar en `input_snapshot`. Ese JSON es también la sección «Datos de entrada» del prompt de generación inicial; añadirlo cambiaría lo que ve la IA y dejaría de ser solo observación. En la base no hay impedimento: la columna es `JSONB NOT NULL`, V32 y V33 no la restringen y los lectores ignoran propiedades desconocidas. Queda como decisión para cuando la política participe en la generación.
