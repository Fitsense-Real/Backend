# Diagnóstico H2 · contradicción entre el prompt de generación y V15

## Problema

La IA recibe una cuota por grupo muscular distinta de la que después verifica el backend (V15). El modelo cumple la cuota comunicada y aun así V15 rechaza la propuesta. Eso provoca un reintento completo (otra predicción de unos 70 s) y, si vuelve a fallar, la semana se publica con el respaldo determinista.

## Evidencia

**Expediente original** (rama `audit/caso-usuario-39`): `docs/FitSense/auditoria_caso_39/INFORME_CASO_39.md`.

- §B.2: causa raíz.
- §F.1: H2, de gravedad **Alta**.
- §I.5: diseño de la corrección.
- Sonda y rejilla en `evidencia/cierre/`.

**Caso real (usuario 39, plan 78, LOWER_BODY):**

- Prompt PR-1.7 reproducido: `max_per_body_part = 6`, `min_exercises_per_session = 12` y `max_colocaciones_dia = 8`.
- Propuesta de la IA: 4 ejercicios de `upper legs`. Cumple el tope comunicado (4 ≤ 6).
- V15 la rechaza: con 6 ejercicios de trabajo (8 − 2 complementos), el tope real era 3.
- Consecuencia: un reintento de 211 s y la publicación del respaldo.

**Sonda con el validador real:** 80 combinaciones por política (UPPER y LOWER, n = 2…6, k = 1…n). La función `floor(n/2)` predice exactamente el resultado de V15 en las 80.

**Rejilla de contradicciones (384 perfiles, 1.312 sesiones):**

| Contradicción | PLAN-HIST-1 | PLAN-REV-2.0 |
|---|---|---|
| C1 · `min_exercises_per_session` > `max_colocaciones_dia` | 320 | 0 |
| C2 · el tope comunicado admite una concentración que V15 rechaza | 400 | 0 |
| C3 · V15 aplica la mitad y el prompt no da tope | 48 | 1.184 (todas) |

**Frecuencia histórica:**

- En 112 llamadas de generación, 5 rechazos incluían la regla de la mitad de V15, y 2 se debieron solo a ella.
- En la auditoría de 48 predicciones, 5 rechazos la incluían y 3 se debieron solo a ella.

Es una causa minoritaria, pero se repite.

**Estado en el código actual** (`generation` tras el merge del catálogo): sin cambios. `PlanInputSnapshot.suggestedSession` sigue enviando `max_per_body_part = min_exercises_per_session / 2`, y `minExercisesOrNull` sigue sin acotar.

## Causa

1. **Instrucción que contradice la regla:** la regla 15 y la comprobación final de `PlanPromptBuilder` dicen «no pongas más de `max_per_body_part` ejercicios del mismo body_part». Un solo número no puede expresar la regla de V15, porque esta depende del número de ejercicios de trabajo de la sesión, que decide la IA.
2. **Dato incorrecto:** `min_exercises_per_session` se calcula con `target_sets × target_reps`, no con las dosis por capacidad que exige V24, y no está acotado por `max_colocaciones_dia`. Con PLAN-HIST-1 puede pedir 12 ejercicios donde solo caben 8.
3. **Complementos:** V15 no cuenta los complementos de capacidad (cardio y movilidad, `CatalogEligibilityPolicy.complementos`), y el prompt no lo dice.

## Validador que rechaza

`PlanDraftValidator.validateFocusCoverage`, regla V15. Cuando el enfoque tiene 3 grupos o más y la sesión tiene 4 ejercicios de trabajo o más, ningún grupo puede pasar de `n/2` (división entera). **No es el validador el que se equivoca**, sino la instrucción.

## Comportamiento aprobado

Es V15 tal como está. La regla de la mitad es una política aprobada, y H2 no la cambia. Lo que hay que corregir es la instrucción: la IA debe recibir la misma regla que se va a verificar.

## Alcance

| Flujo | ¿Le afecta? |
|---|---|
| **Generación inicial** (PR-1.7) | **Sí:** C1, C2 y C3 |
| **Adaptación** (SessionFix, FIX-PR-1.3 / V2.1) | **Parcialmente.** No envía `suggested_split`, así que la regla de la mitad no se comunica; las sesiones se conservan y solo se corrigen las señaladas. Sí envía `min_exercises_per_session`, así que hereda C1 |

## Solución

Corrección de instrucciones, sin cambiar ninguna política. Detalle en `CORRECCION_H2.md`.

**Es necesaria una decisión** porque `ConfiguracionEvaluadaTest` fija que la generación evaluada para la tesis es PR-1.7 sin variantes, y corregir H2 cambia lo que recibe la IA. Por eso la corrección va detrás de un interruptor apagado por defecto.

## Pruebas

En `CORRECCION_H2.md`.

## Pendientes

Autorizar la evaluación pareada con la IA real y decidir si se activa (`PLAN_PRUEBAS_IA_REAL.md` §H2).
