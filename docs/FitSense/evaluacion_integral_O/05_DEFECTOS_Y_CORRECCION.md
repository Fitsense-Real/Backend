# 05 · Fallos observados, causa y corrección propuesta

**Regla de la orden:** «Si aparece un defecto, conservar el resultado, identificar su causa y presentar la corrección antes de efectuar una nueva evaluación completa.»
- **El resultado oficial se conserva tal como se obtuvo:** 139 APROBADO, 3 FALLO y 97,9 % de aprobación técnica.
- No se ha repetido ninguna corrida para «arreglarlo».
- No se ha modificado el oráculo congelado.

## Los tres FALLO de la corrida principal S_O (oráculo v1.0)

| Perfil | Aspecto | Lo que dice el oráculo | Lo que muestra la evidencia | Causa |
|---|---|---|---|---|
| O03 | A7 | «C3: paso de progresión con la progresión apagada» | El paso es una **RECUPERACION** del ejercicio 899: 2 → 3 series, exactamente su dosis original tras la reducción por fatiga de C1. Volumen 66 → 72; techo 79 | **Defecto del oráculo (D-OR-1).** En PLAN-REV-2.0, la regla aprobada P-A1 (`AdaptedWeekCommandService.continuidad`) admite con la progresión apagada **solo** la recuperación de una reducción autorizada (fatiga o desmotivación), sin superar la referencia. El oráculo 1.0 trataba cualquier paso como prohibido |
| O14 | A7 | «C4: paso de progresión con la progresión apagada» | **RECUPERACION** del ejercicio 253: 2 → 3 series de 90 s, su dosis original tras la reducción por fatiga de C2. Volumen 79 → 82; techo 90 | **D-OR-1**, igual que O03 |
| O19 | A8 | «C1–C8: el retirado 853 vuelve sin paso de REINCORPORACION»; «C9: no va al final» | 853 se retiró en C1 **solo** de la sesión del linaje 121 (ACORTAR) y estaba desde S1 en las otras dos sesiones de la semana. En C9 vuelve **a su linaje** (sesión 148): al final, con su dosis inicial exacta (3 × 120 s, descanso 60), tras dos PROGRESION en ese linaje (1179: 6 → 7 → 8 repeticiones) y con volumen 84 ≤ techo 85 | **Defecto del oráculo (D-OR-2).** El oráculo 1.0 buscaba el retirado en **toda la semana** en lugar de en la sesión de su linaje, y tomaba la posición de otra sesión |

**Ninguno de los tres es un defecto del sistema.** El backend se comportó según las reglas documentadas:
- P-A1;
- la reincorporación condicionada (§5 de `ProgresionResolver`).

## Corrección propuesta (NO aplicada a los resultados oficiales)

**Fichero:** `herramientas/oraculo_integral_O_v1_1.py`. Es una copia del 1.0 con dos cambios, marcados en el código:
- **D-OR-1:** con la progresión apagada y PLAN-REV-2.0, los pasos **RECUPERACION** se evalúan con sus reglas (nunca por encima de la referencia, techo 1,10). Cualquier otro paso sigue siendo FALLO.
- **D-OR-2:** el retirado por ACORTAR se busca solo en la sesión de su linaje.

## Recalificación diagnóstica

Oráculo v1.1 sobre **las mismas salidas** de S_O, sin ninguna corrida nueva. **No es el resultado oficial.**

| | Oficial (v1.0) | Diagnóstico (v1.1) |
|---|---|---|
| APROBADO | 139 | 142 |
| FALLO | 3 | 0 |
| NO APLICA | 43 | 43 |
| REQUIERE REVISIÓN | 15 | 15 |
| Aprobación técnica | 139/142 = **97,9 %** | 142/142 = **100 %** |
| Celdas que cambian | — | Exactamente 3: O03·A7, O14·A7, O19·A8 (FALLO → APROBADO). Ninguna otra |

**Siguiente paso, si lo apruebas:**
1. Congelar el oráculo v1.1.
2. Repetir la evaluación completa (S_O y S_O′) con un commit nuevo y una carpeta nueva.
3. Informarla como una corrida distinta, sin mezclarla con esta.
