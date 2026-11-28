# 06 · Trazabilidad de los recálculos (P1-09)

**PR:** [#99](https://github.com/Fitsense-Real/Backend/pull/99) (Backend-C4). Es un **BORRADOR, PENDIENTE DE VALIDACIÓN METODOLÓGICA**: no se fusiona sin esa aprobación. Lleva migración (V67) y nada más. **Se excluyó de la integración final del 28/11/2026:** V67, su tabla, su disparador y su código **no están en `generation`**, cuya última migración es V66.

## 1. El problema

`weekly_user_metrics` guarda una fila por participante y semana. Un recálculo **la sobrescribe**. Ocurre cuando el cierre semanal se vuelve a ejecutar para la misma semana medida:
- relanzado a mano;
- recuperado por B3 dentro de la ventana;
- reanudado por V66 tras un reinicio.

`calculated_at` es, a propósito, la fecha del **primer** cálculo (`ADHERENCIA.md`). El intento de B5 de renovarla se revirtió en H-2. Hasta ahora **no queda constancia** de que hubo un recálculo, de cuándo fue, de qué valores había antes ni de quién lo hizo.

## 2. Propuesta: V67 `weekly_metric_recalculations`

Una tabla de **solo inserción**: un disparador impide el `UPDATE`. El `DELETE` sí se permite, porque la eliminación de cuenta borra los datos de la persona. Una fila por cada recálculo de una medición **que ya existía**. El primer cálculo no deja fila: es la propia medición.

| Distinción que pide la orden | Columna | Valor |
|---|---|---|
| Fecha del primer cálculo | `first_calculated_at` | Copia de `weekly_user_metrics.calculated_at`, que **no se toca** |
| Fecha de recálculo | `recalculated_at` | Momento del recálculo (no puede ser anterior al primero) |
| Valor anterior | `previous_values` | JSONB con las 32 columnas medidas antes de sobrescribir |
| Valor recalculado | `recalculated_values` | JSONB con las mismas columnas después |
| (Qué cambió) | `changed_fields` | Columnas que cambian; vacío si el recálculo da lo mismo. `risk_factors` se compara como JSON, porque `jsonb` lo devuelve con otro orden |
| Motivo | `reason` | `CIERRE_REPETIDO`. `RECALCULO_MANUAL` queda reservado: no existe ningún procedimiento manual |
| Versión del algoritmo | `previous_calculation_version` / `calculation_version` | La versión de umbrales (`calculation_configs`) de antes y de después |
| Fuente de los datos | `data_source` | `SESIONES_REGISTRADAS` (las sesiones de la semana y su plan) |
| Usuario o proceso | `actor` | `PROCESO:CIERRE_SEMANAL`. Admite `ADMIN:<id>` si un día se aprueba un procedimiento manual |

Garantías:
- **Misma transacción** que el recálculo: o se guardan los dos o ninguno.
- **No cambia la medición oficial.** La fórmula de adherencia, las columnas de `weekly_user_metrics` y `calculated_at` quedan igual. Ninguna fila existente se reescribe ni se borra. La tabla se limita a observar.
- **Compatible con V66.** Solo crea su tabla, su función y su disparador. `MigracionV66aV67IT` comprueba que una medición escrita con V66 queda idéntica columna a columna.
- **Datos personales.** Contiene adherencias, así que la eliminación de cuenta también la borra. `EliminacionDeCuentaIT` comprueba que no queda ninguna tabla con `user_id` fuera de la lista.

## 3. Pruebas

| Prueba | Qué demuestra |
|---|---|
| `HistorialDeRecalculosIT.recalculoSinCambios` | El primer cálculo no deja historial. Un recálculo igual deja una fila con `changed_fields` vacío, valores antes = después y `calculated_at` intacto |
| `HistorialDeRecalculosIT.recalculoConCambios` | Con una sesión omitida tras el primer cálculo, la fila registra el valor anterior, el recalculado (el mismo que queda en la medición) y `skipped_workouts` |
| `HistorialDeRecalculosIT.soloInsercion` | No se puede modificar un recálculo registrado |
| `MigracionV66aV67IT` | Compatibilidad con V66; motivo, actor y fuente acotados por `CHECK` |
| `CamposCambiadosTest` | 12.50 y 12.5 no son un cambio; un valor distinto o un nulo, sí |
| `EliminacionDeCuentaIT`, `AdherenciaIT`, `OperacionYExportacionIT`, `CierreSemanalRecuperableIT` | Sin regresiones |

El primer ensayo encontró un falso «cambio» en un recálculo idéntico: `risk_factors`, normalizado por `jsonb`. Se corrigió comparando y guardando como JSON.

## 4. Lo que tiene que validar el equipo del estudio

1. **Qué columnas cuentan como «medición».** Hoy son las 32 que escribe el cálculo, sin identificadores ni fechas.
2. **Un recálculo que cambia la adherencia oficial** (`weighted_adherence_pct`): ¿basta con registrarlo, o además debe alertar o bloquearse a partir de cierta fecha (por ejemplo, después de exportar los datos para el análisis)?
3. **Recálculo manual.** ¿Se permite? Si se permite, con qué procedimiento y quién (`ADMIN:<id>`). No existe ningún endpoint.
4. **Conservación.** El historial se conserva con la medición y se borra con la cuenta. Para quien se retira, según D-1.

Hasta esa validación: **PENDIENTE** (P1-09), con la propuesta **PREPARADA SIN ACTIVAR** en #99.

## 5. Preguntas que el equipo debe responder antes de aprobar #99

1. ¿Qué origina un recálculo? Hoy, solo repetir el cierre de una semana ya medida.
2. ¿Cómo se distingue del cálculo inicial? La fila de `weekly_user_metrics` es el primero, y cada recálculo deja una fila en el historial.
3. ¿Puede un recálculo modificar resultados que ya se usaron (exportados o analizados)? ¿Se bloquea a partir de una fecha?
4. ¿Cómo se preserva el valor histórico: basta `previous_values`, o la medición oficial no debe sobrescribirse?
5. ¿Qué versión del algoritmo se registra: la de umbrales (`calculation_version`) o también la del código?
6. ¿Cómo se auditan las correcciones manuales, si se permiten?
7. ¿Cómo se reportan los recálculos en la tesis?
