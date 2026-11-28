# 04 · R-4: estados de adaptación

**Antes:**
- `AdaptationOutcome.respaldo` decidía entre `RESPALDO_PROPUESTA_IA_INVALIDA` y `RESPALDO_SIN_IA` solo por `ai_calls > 0`.
- Como `AWCS` incrementa el contador **antes** de llamar, una caída del proveedor en el intento 1 se presentaba como «la IA propuso y se rechazó».

**Ahora:**
- `AWCS` cuenta las **respuestas** recibidas.
- Si se llamó y no llegó ninguna, añade a `correcciones_del_backend` del informe V32 la nota `IA_NO_DISPONIBLE: <causa técnica, ≤ 200 caracteres>`.
- `AdaptationOutcome` traduce ese caso a **`RESPALDO_SIN_IA`**.

**Sin estados nuevos, sin migraciones y sin cambios en el contrato de la API.**

| # | Situación | Estado V32 | Resultado de la API |
|---|---|---|---|
| 1 | IA no disponible (apagada, sin token, cortacircuitos): 0 llamadas | RESPALDO | `RESPALDO_SIN_IA` |
| 2 | Se llamó y el proveedor no devolvió ninguna propuesta (caída, timeout, respuesta ilegible) | RESPALDO + nota | **`RESPALDO_SIN_IA`** (antes `RESPALDO_PROPUESTA_IA_INVALIDA`) |
| 3 | La IA devolvió una propuesta inválida (contrato) | RESPALDO | `RESPALDO_PROPUESTA_IA_INVALIDA` |
| 4 | Propuesta rechazada por V8C u otro validador | RESPALDO | `RESPALDO_PROPUESTA_IA_INVALIDA` |
| 5 | Propuesta válida sin cambios | SIN_EFECTO | `SIN_CAMBIOS_VALIDOS` |
| 6 | Adaptación aplicada | APLICADA | `ADAPTACION_APLICADA` |
| 7 | Intervención no factible | NO_FACTIBLE | `NO_FACTIBLE` / `NO_FACTIBLE_POR_SEGURIDAD`; la nota queda en la evidencia |
| 8 | El sistema decidió conservar | SIN_EFECTO | `CONSERVACION_DELIBERADA` |
| — | Factibilidad previa imposible | RESPALDO | `ACORTAMIENTO_IMPOSIBLE` / `REPROGRAMACION_IMPOSIBLE` / `ADAPTACION_IMPOSIBLE` (sin cambios) |
| — | Sustitución por seguridad (P-03) | RESPALDO | `REGENERACION_SEGURA_SUSTITUCION` (tiene prioridad, sin cambios) |

**Mixto:** si la IA respondió al menos una vez y luego el proveedor cayó, es el caso 3 o 4: hubo propuesta.

**Qué no cambia:**
- `ai_calls` sigue siendo el **número de llamadas hechas** (coste).
- La causa técnica **no** se añade a `motivos`: no se muestra al usuario. Queda en V32 y en el log, sin datos sensibles.
- No cambian la adherencia, las decisiones de adaptación, V8C ni la conservación de sesiones. Lo publicado es idéntico.
