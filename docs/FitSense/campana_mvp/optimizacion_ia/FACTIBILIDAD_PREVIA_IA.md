# Factibilidad previa a la IA

**Fecha:** 31/10/2026, fase C de la orden de optimización.
**Clase:** `planning/domain/services/FactibilidadPreviaIA`, llamada desde `AdaptedWeekCommandService.corregir` antes del primer intento.

## Qué hace y qué no

- **Qué hace:** solo **demuestra imposibilidades**.
- **Cuándo evita la IA:** únicamente si **todas** las sesiones que irían a la IA son imposibles.
  - Si una sola puede salir, se llama.
  - Ante cualquier duda (faltan minutos, prescripción o catálogo, o hay menos de 3 ejercicios) devuelve «sin motivo» y la IA se llama.
- **Qué no es:** no es un planificador. No propone ni elige nada. El resultado de una imposibilidad es el mismo respaldo que se publicaba tras tres intentos fallidos (`sinPropuestaUsable`): la semana conservada, con el motivo en el log (`FACTIBILIDAD_PREVIA`).
- **Validaciones:** no se relaja ninguna. La comprobación usa el mismo estimador de V17 y V20 y las mismas operaciones que V8C permite.

## Las dos comprobaciones

| Caso | Cota | Imposible si |
|---|---|---|
| **ACORTAR con minutos declarados** | La mínima entre las dos operaciones que V8C permite. Se cuentan por separado porque no se pueden combinar | La cota supera los minutos declarados |
| **REPROGRAMAR** | Fechas de la semana en `available_days`, sin otra sesión, distintas de la actual | No queda ninguna |

**Las dos operaciones de ACORTAR:**
- **Retirar una colocación.** En GAIN_MUSCLE solo un relleno (sin slot de 4C).
- **Bajar todas las dosis al mínimo de V24:** 2 series, `min_reps` y 20 s, sin tocar el descanso. No se aplica en GAIN_MUSCLE, donde la dosis 4D no se reduce.

**Otras causas de imposibilidad que se valoraron:**
- **No hay sustituto:** ya lo cubre SUSTITUTOS ADMITIDOS. Si la lista está vacía, la IA puede omitir la sesión.
- **Capacidades obligatorias que no se pueden mantener:** no se demuestran de forma barata sin replanificar.

En los dos casos se llama a la IA, que es lo que pide la orden: no bloquear si hay duda.

## Los casos de la campaña

Son cifras del backend actual sobre el mismo estado, recreado con las respuestas originales (`COMPARACION_IA_ANTES_DESPUES.md`).

| Caso | Datos | Cota | Antes | Ahora |
|---|---|---:|---|---|
| U01 S2 · ACORTAR, GAIN_MUSCLE | 22 min declarados, sesión de 32 | 28 min retirando un relleno | 2 llamadas, todas rechazadas → SIN_EFECTO | 0 llamadas → RESPALDO, con la misma semana |
| U04 S5 · ACORTAR, GENERAL_FITNESS principiante | 20 min declarados, sesión de 29 | 21 min (la mejor de las dos operaciones) | 3 llamadas rechazadas → RESPALDO | 0 llamadas → RESPALDO |
| U07 S2 · REPROGRAMAR | 6 días disponibles y 6 sesiones | sin fecha libre | 2 llamadas (V12 en la 1.ª; la 2.ª no cambió nada) → SIN_EFECTO | 0 llamadas → RESPALDO |

**En los tres casos se publica la misma semana:** las sesiones conservadas tal cual.

**La diferencia de estado:**
- SIN_EFECTO quiere decir «la IA respondió sin cambios válidos».
- RESPALDO quiere decir «no hubo propuesta usable».
- Ahora consta RESPALDO porque no se pidió nada.

**Llamadas evitadas:** 7 (2 + 3 + 2), el 16 % de las 45 llamadas de la campaña original, y ninguna podía terminar en una adaptación válida. La campaña no tuvo ninguna adaptación factible que esta comprobación hubiera bloqueado.

## Comprobación de que no bloquea de más

| Prueba | Resultado |
|---|---|
| `FactibilidadPreviaIATest` (7) | Imposible: GAIN_MUSCLE a 10 min y REPROGRAMAR sin fecha. Se llama: retirar un relleno cabe, bajar la dosis cabe, sin minutos declarados, con una fecha libre, o si una de dos señaladas puede salir |
| `AdaptacionPorCausasIT.menosTiempoHabitualInviable` | Antes, 3 llamadas para llegar al RESPALDO; ahora 0, con la misma semana publicada (premisa actualizada) |
| `AdaptacionPorCausasIT` (11) y el resto de las IT de adaptación | Los casos factibles siguen llamando y aplicando |
| Campaña en modo foco | Ver `COMPARACION_IA_ANTES_DESPUES.md` |
