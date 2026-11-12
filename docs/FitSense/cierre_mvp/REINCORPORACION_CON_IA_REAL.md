# Reincorporación con IA real

**Fecha:** 31/10/2026.
**Orden:** «ORDEN DE CIERRE DEL MVP: REINCORPORACIÓN CON IA REAL, TRAZABILIDAD Y SIMULADOR».
**Prueba:** `ReincorporacionIaRealIT`, opt-in con `-Dfitsense.reincorporacion.ia=true`.
- Usa los servicios reales sobre PostgreSQL efímero, los comandos reales de ejecución, V8C, V1–V25, la política de adherencia y V31/V32/V34.
- No inserta ningún resultado.

**Evidencia:** `evidencia/1_ia_real_GAIN_MUSCLE_30min_2dias/`.

| Fichero | Contenido |
|---|---|
| `resumen.json` | Resumen de la ejecución |
| `llamadas.jsonl` | Las llamadas en vivo |
| `predicciones/4rckyzeapdrmw0d1j02atkdqf4.json` | La predicción completa, con prompt y salida |
| `HISTORIAL_REINCORPORACION.csv` | Una fila por ciclo |
| `TRAZA_EJERCICIO_RETIRADO.csv` | El ejercicio retirado, semana a semana |
| `eventos.log` | Eventos del backend |

## Resultado

**Recorrido cerrado:** IA real adapta → la persona cumple → el backend progresa → el backend reincorpora → la persona entrena.

| Etapa | Semana | Qué pasó | Quién |
|---|---|---|---|
| A · Plan inicial | 1 | GAIN_MUSCLE intermedio, gimnasio, 30 min, lunes y jueves. El jueves lleva 72, 480, 715 y 513 (3×10, **2×10 el relleno 480**, 3×10, 3×10) | Motor de reglas (0 llamadas) |
| B · Problema de tiempo | 1 | El jueves se empieza y se deja al 35 % por falta de tiempo: sesión PARCIAL. La semana queda en 67,5 %, por debajo de 80 | Comandos de ejecución |
| C · Adaptación | ciclo 1 | Orden ADAPT_LOCALIZED → ACORTAR. **Replicate, predicción `4rckyzeapdrmw0d1j02atkdqf4`, 1 llamada, aceptada al primer intento:** retira el relleno 480 y deja 72, 715 y 513 con su dosis. V32 APLICADA | **IA real** |
| D · Ejecución adaptada | 2–10 | Todas las semanas completas al 100 %. 480 ausente. Sin restauración automática | Comandos de ejecución |
| E · Progresión | ciclos 3–9 | 7 pasos, uno por ciclo. En el jueves: 72 a 3×11 y 3×12, 715 a 3×11 (volumen 90 → 99), con la reserva de 480 PENDIENTE | Backend (0 llamadas) |
| F · Reincorporación | ciclo 10 | 480 vuelve con su **dosis inicial 2×10 / 90 s**, al final de la sesión. Volumen 99 → 119, techo 121 | Backend (0 llamadas) |
| G · Mantenimiento | 11–12 | Se entrena el jueves con 480 (COMPLETED, 2 series y 20 repeticiones). El ciclo siguiente lo mantiene, sin duplicarlo | Comandos y backend |

**Coste de esta parte:** 1 llamada, 54 741 tokens de entrada y 1 738 de salida, ≈ US$ 0,017 y 24 s.

## Por qué este perfil

**Primer intento: el perfil de U08 (LOSE_WEIGHT intermedio, 60 min, 3 días).**
- **Comprobación previa (ensayo seco):** la ruta existía. Retirada en s1 y reincorporación en s10 (`evidencia/seco_LOSE_WEIGHT_60min_3dias`).
- **Con IA real:** 4 escenarios, 4 llamadas. Las 4 propuestas fueron **reducciones de dosis válidas**: 3×6 → 2×6 y cardio de 120 a 60 s, con descansos iguales.
  - ACORTAR las admite para los objetivos por capacidades.
  - Se aplicaron (APLICADA), pero **no son retiradas**.
- No se convirtieron en retiradas ni se editó ninguna respuesta (`evidencia/0_ia_real_LOSE_WEIGHT_60min_3dias`).

**Por qué GAIN_MUSCLE:** su dosis 4D no se reduce, así que la única operación que V8C admite en ACORTAR es **retirar un relleno**.

**Comprobación previa del perfil:**

| Configuración | Retirada | Reincorporación |
|---|---|---|
| 30 o 45 min, 3 días | Sí | **No llega en 16 semanas**: los pasos se reparten entre tres sesiones |
| 30 min, 2 días | Sí | **s10** |
| 45 min, 2 días | Sí | s16 |
| 60 min | No hubo acortamiento (SIN_EFECTO) | — |

**El perfil elegido:** 30 min y 2 días, el mismo `user_id` y el mismo plan que el ensayo seco. La IA real hizo exactamente la retirada legítima.

**Ninguna política se cambió para hacer posible la demostración.**

## Comprobaciones de cada etapa

| Etapa | Comprobación | Resultado |
|---|---|---|
| C | La retirada es de un **relleno sin slot de 4C** (480, sin patrón obligatorio), única, sin nada añadido y con el resto de dosis idénticas | Comprobada en la prueba, que exige esa forma exacta |
| C | La propuesta superó el backend completo: V8C (operación de ACORTAR, sin subir dosis), V17 (duración calculada por el backend), V20 (la sesión acortada queda exenta de su mínimo: D8), V24 (dosis 4D exacta), equipamiento, seguridad y publicación | V32 APLICADA |
| D | 480 ausente en las semanas 2 a 10 | `TRAZA_EJERCICIO_RETIRADO.csv`, filas AUSENTE |
| D | Adherencia | Observada 100 % y cobertura 100 % en cada semana (V32) |
| E | Cada paso respeta el 5 % de la sesión, el rango 4D (+2 repeticiones, 3 series en estructurales) y el techo 1,10 × base. Ninguno lo decide la IA | `HISTORIAL_REINCORPORACION.csv` y 0 llamadas en todos los ciclos |
| F | Las 12 condiciones de la orden | §5 de `TRAZABILIDAD_EJERCICIO_RETIRADO.md` |
| G | Registro real del reincorporado | `workout_session_exercises`: COMPLETED, 2 series y 20 repeticiones |
| G | Repetir el ciclo de la reincorporación | Devuelve la misma semana y no registra otra evidencia. Una sola colocación de 480 |
| Todas | Linaje V31/V32 sin violaciones y a lo sumo un paso por ciclo (consulta `linaje_v31_v32.sql`) | Sin violaciones |
| Todas | Validación independiente con autorizaciones | Las 11 semanas, VALIDA_CON_AUTORIZACIONES: base, paso, heredada, reincorporación y fecha reducible del jueves acortado |

## Hallazgo previo corregido: la M declarada

**El defecto:** el diseño aprobado (`DISENO_PROGRESION_Y_REINCORPORACION.md` §5, fila 5) solo permite reincorporar un ejercicio retirado por falta de tiempo **con M declarada** si hay una **M nueva mayor**. `ProgresionResolver` no lo comprobaba: lo declaraba como limitación.

**Corregido:**
- Si la sesión con el retirado no cabe en la M vigente, la reserva queda EN ESPERA DE TIEMPO y los conservados progresan hasta el techo.
- La M vigente es la última declarada en el linaje desde el acortamiento.

**Prueba:** `CopiaSinIaIT.conMNoSeReincorpora`.

**Por eso el escenario usa una sesión PARCIAL por falta de tiempo,** el único acortamiento sin M:
- en las parciales no se pregunta nada;
- las pruebas de reincorporación anteriores, que usaban la omisión con M, se cambiaron a ese camino.

**La campaña:** el 864 de U08 se retiró con M = 24, así que no habría podido volver aunque hubiera tenido semanas. Además, con el backend actual esa retirada ya no cabría en M: ver `CIERRE_VERIFICACION_MVP.md`.
