# 03 · Oráculo K20 versionado por política

**Por qué hace falta:**
- El oráculo independiente K20 (`short_paper_ieee/k20_adaptacion_real/herramientas/oraculo_k20.py`, evidencia del PR #41) **reimplementa en Python las reglas de la política histórica**, incluido el piso del 70 % de V20.
- Con `PLAN-REV-2.0`, V20 deja de rechazar por el 70 % (P-G1, aprobado). El oráculo histórico marca entonces FAIL en todo plan que use menos del 70 % del tiempo, aunque sea correcto según la política aprobada.

**Regla seguida:** «No cambies los criterios del oráculo para ocultar nuevas deficiencias.»
- El oráculo histórico **no se modifica**: sigue en su sitio, con su código y sus resultados.
- El versionado (`herramientas/oraculo_k20_politica.py`) lo **importa** y cambia una sola función, `validar_plan`, de forma explícita.
- **Quita una regla**, la que la orden aprobó retirar, y **añade cuatro más estrictas**.
- Las dos versiones se publican **siempre juntas** en cada resultado.

## 1. Qué cambia

| Regla | Histórico (`PLAN-HIST-1`) | Versionado (`PLAN-REV-2.0`) | Fuente |
|---|---|---|---|
| V20: piso del 70 % de M | Exige | **No exige** | P-G1 aprobado: «V20 deja de rechazar solo por no llegar al 70 %» |
| V4: minutos declarados ≤ round(M × 1,15) | Exige | Exige (igual) | — |
| **V4M**: segundos estimados ≤ M × 60 | — | **Exige** | P-G1: «V4 respeta el máximo» |
| **V26O**: series SETS_REPS por grupo grande ≤ 6/10 por sesión; ≤ 12/20/24 por semana | — | **Exige** | Techo de `DosisSemanalPolicy`. Cuenta también lo que el backend trata como complemento, así que es igual o más estricta que V26 |
| **V27O**: una sesión PULL tiene al menos un HORIZONTAL_PULL o VERTICAL_PULL | — | **Exige** | P-G3: «no PULL sin tracción efectiva» |
| **QINS**: sesión con < 3 ejercicios o < 6 series | — | **Exige** (FAIL) | `CalidadDosis` (el backend solo la clasifica; el oráculo la rechaza) |
| Resto: días, semana, V1, V5, V6, V7, V17, V21, equipamiento, registro, adherencia, decisión, adaptación, publicación, continuidad, huella | Exige | Exige (igual: es el mismo código) | — |

Las cifras de V26O y QINS son [HP][PRO], pendientes de validación profesional (`02_EVIDENCIA_DOSIS.md`).

## 2. Controles del oráculo versionado

| Control | Resultado |
|---|---|
| Con `PLAN-HIST-1`, el versionado da exactamente lo mismo que el histórico | U: `{"PASS": 20}` con la misma huella `f9f67973…` |
| El versionado REV2 **no oculta** defectos de los planes históricos: los detecta | Corridas HIST: J **6 FAIL** (J03 sin plan; J05, J06, J09, J12 y J15 por V26O, hasta 27 series de pecho y pierna en la semana); K **2 FAIL** (K06 y K13 por V26O, hasta 24 series de pecho en principiante) |
| Lo que el histórico marca en las corridas REV2 es **solo** V20 | U: 30 + 30 + 30 avisos «V20 … s < … s» (A, F y G de U01–U10); J: 26 + 26 + 26; nada más |

**Lectura:** el único FAIL que el histórico atribuye a REV2 es la regla que la orden retiró. A la vez, el versionado es más exigente que el histórico en volumen, en PULL, en duración por arriba y en sesiones mínimas.

## 3. Uso

```
python3 -I docs/FitSense/rediseno_politicas/herramientas/oraculo_k20_politica.py <PLAN-HIST-1|PLAN-REV-2.0> \
        <carpeta_de_la_corrida> <perfiles.csv> [<teorico.csv>]
```

El resultado se escribe en `<carpeta>/oraculo_<política>/` y no pisa `<carpeta>/oraculo/` (el del histórico).
