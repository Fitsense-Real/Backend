# Auditoría de R0800

**Perfil:** INCREASE_STRENGTH, intermedio, **solo peso corporal**, 6 días, 75 minutos. División PUSH/PULL/LEGS.

**Datos:**
- `AUDITORIA_R0800_DETALLE.csv`: series por zona y sesión, y repeticiones de ejercicio en la semana.
- `CASOS_EXTREMOS_DETALLE.csv`

## 1. Separar selección y volumen

| Problema | Tipo | Evidencia |
|---|---|---|
| La «fuerza» del PULL es **775, un empuje** | **Selección** | En las dos sesiones PULL, 775 es la única colocación ESTRUCTURAL (3×6). El resto son tronco o movilidad (1259, 1261, 1308), 315 (mancuerna) y 320 o 1066 |
| No hay tracción en toda la semana | **Selección y catálogo** | 0 tracciones elegibles (`CASOS_EXTREMOS_ELEGIBLES.csv`) |
| 21 series de pierna en una sesión | **Volumen** | 7 colocaciones de `upper legs` × 3 series en cada LEGS |
| 138 series por semana | **Volumen** | 6 sesiones de 7–8 colocaciones de 3 series |
| 20 colocaciones repiten un ejercicio de la semana | **Selección** (catálogo estrecho) | Los PUSH y PULL repiten casi el mismo grupo de ejercicios |

## 2. Por política

| Política | Resultado | Series/semana | Por zona | Máximo zona-sesión | Colocaciones repetidas |
|---|---|---|---|---|---|
| PLAN-HIST-1 | INVÁLIDO (V20) | 138 | upper legs 42 · back 30 · upper arms 30 · chest 24 · shoulders 6 · lower legs 6 | 21 | 20 |
| PLAN-C2D-0.1 | **VÁLIDO** (C2-D, SATURADA) | 138 | Igual | 21 | 20 |
| **PLAN-C2D-EF-0.1** | **NO_FACTIBLE** (VEF) + V20 | 84 | upper legs 42 · upper arms 18 · chest 12 · back 6 · lower legs 6 | 21 | 10 |

Con EF:
- **El problema de selección se resuelve rechazando el plan.** No se rellena el PULL con otro movimiento: el plan no es factible con ese material.
- **El problema de volumen sigue** en las sesiones LEGS: 21 series de pierna. **No se ha cambiado.** Las dosis 4D no se tocan y la orden prohíbe imponer un tope universal de series sacado del preprint.

## 3. Volumen: qué se puede afirmar

- 21 series de pierna en una sesión y 42 por semana **no son una infracción** de ninguna regla vigente:
  - V24 limita la dosis por colocación;
  - el tope de 4D es de colocaciones, no de series por grupo.
- **No se declara inseguro ni adecuado.** Es una pregunta de revisión profesional: ¿es razonable con peso corporal y dosis de fuerza 3×6–10?
- **No se propone ningún tope de series por grupo.** Habría que justificarlo con evidencia y versionarlo como una política propia.

## 4. Conclusión

| Pregunta | Respuesta |
|---|---|
| ¿R0800 era adecuado? | **No:** su fuerza de tracción era un empuje. C2-D lo publicaba por saturación local |
| ¿Qué pasa con la corrección? | **NO_FACTIBLE** por falta de tracción con el equipamiento declarado (VEF) |
| ¿Queda algo pendiente? | El volumen de pierna por sesión, para revisión profesional (muestra, prioridad 1) |
