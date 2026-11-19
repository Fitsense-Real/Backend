# Correcciones del simulador

**Fecha:** 31/10/2026, §8–10 de la orden.

**Alcance:** solo el instrumento de pruebas (`CampanaMvp8UsuariosIT`, `ValidacionIndependiente`, `ReincorporacionIaRealIT`).

**Sin cambios en:** las tablas, la producción, las migraciones y los registros reales de V32.

## 1. Identificadores reproducibles (§8)

**Problema.** Al repetir respuestas archivadas, cada ejecución creaba usuarios y sesiones con otros ids. Había dos efectos:
- Una respuesta de adaptación que nombraba la sesión 15 apuntaba a una sesión inexistente o ajena. Se rechazaba por «sesión no señalada» (U04 S7 y U01 S6 en la comparación anterior).
- **Hallazgo adicional:** la semilla del motor de reglas es `user_id × 1 000 003 + fecha`.
  - Los usuarios se creaban en hilos, así que su id dependía del orden de llegada.
  - Con otro id cambia el plan: dos repeticiones del mismo historial daban resultados distintos en 7 de 48 semanas (U01 tenía 12 u 11 ejercicios).

**Corrección.**

| Dato | Cómo se resuelve |
|---|---|
| Usuario | En modo foco, cada usuario sintético se crea con **su `user_id` original** (`USUARIOS_SINTETICOS.csv`, `OVERRIDING SYSTEM VALUE`), solo en la base efímera. Así su plan es el original |
| Sesión | Cada sesión original se traduce a una **clave lógica (usuario, fecha)** con `SESIONES_8_SEMANAS.csv`. En la ejecución nueva, la clave se resuelve contra las sesiones que la derivación corrige |
| Ejercicio | Sin cambios: son ids del catálogo, estables por las migraciones |
| Semana | Sin cambios: la campaña usa una fecha fija (S1 = 2/11/2026) |

- Si una correspondencia no queda **exactamente única**, la repetición falla con un error explícito («se exige exactamente una»). No hay coincidencias aproximadas.
- Cada traducción se registra en `repeticiones.log`, por ejemplo: `U02 · s3 · sesion 31 (U02|2026-11-22) -> 21`.

**Prueba obligatoria: el mismo historial repetido dos veces**, con 0 llamadas (`max-llamadas=0`). Evidencia en `evidencia/simulador/repeticion_1` y `repeticion_2`.

| Comparación | Resultado |
|---|---|
| Por semana: origen del plan, decisión, estado V32, intervenciones, adherencia oficial, pasos y validación | **48 de 48 semanas iguales** |
| `repeticiones.log` (operaciones, intentos, sesión lógica y resultado), sin los ids nuevos | **Idéntico** (mismo hash) |
| Llamadas reales | 0 en las dos ejecuciones |

**Frente a la campaña original:**
- Las semanas coinciden salvo 4, todas atribuibles a cambios reales y documentados del backend.
- U01 S2 y U07 S2: hoy son RESPALDO por factibilidad previa (antes SIN_EFECTO tras 2 llamadas). Es la misma semana publicada.
- **U08 S7:** la retirada original de 864 se aceptó porque la IA **declaró** 24 min, dentro del 20 % de V17, para un contenido de al menos 30. Desde el PR #13 la duración la calcula el backend, y esa retirada ya no cabe en la M = 24 declarada. La factibilidad previa lo demuestra sin llamada.
- U08 S8 hereda ese cambio.

## 2. Contabilización de llamadas (§9)

**Columnas nuevas:**
- **Por semana, en `ADHERENCIA_SEMANAL.csv`:** la columna `contabilidad_live_replay`, con `LIVE=n REPLAY=n SIN_SALIDA=n V32=n`.
- **Por generación inicial, en el plan inicial:** `LIVE n, REPLAY n, SIN_SALIDA n`.

| Concepto | Qué es | Cuenta como llamada pagada |
|---|---|---|
| LIVE | Enviada a Replicate (`llamadas.jsonl`, `predicciones/`, `METRICAS_REPLICATE.csv`) | Sí |
| REPLAY | Respuesta original repetida, validada por el backend actual | No |
| SIN_SALIDA | Intento rechazado sin llamada porque la campaña original no llegó a ese intento | No |
| V32 `ai_calls` | Intentos que el backend hizo al generador: no puede distinguir los tres casos | Se conserva **sin alterar** |

**La comprobación de coherencia:**
- Antes era `V32 = llamadas contadas`, y marcaba una violación falsa en cada repetición: «ai_calls V32 = 3, llamadas contadas = 0».
- Ahora es `V32 = LIVE + REPLAY + SIN_SALIDA`.
- Una respuesta repetida cuenta como **REPLAY = 1, LIVE_REPLICATE = 0**.

## 3. Validación independiente (§10)

**Problema:** las 29 semanas «INVALIDA» de la campaña eran dosis autorizadas que el simulador desconocía (progresión, reducción y herencia).

**Corrección: `ValidacionIndependiente`.**
- **No usa los resolutores del backend.** Lee V31 (`source_planned_workout_id`) y V32 (`plan_adaptation_attempts`, con su informe JSON) **en crudo**.
- Reconstruye el origen de cada dosis y valida la semana con esas autorizaciones.

| Origen | Cuándo |
|---|---|
| BASE | La generación inicial o un sustituto. Incluye la serie adicional 4D, que el propio V24 condiciona |
| PASO | El V32 del ciclo registra un paso de **ese** ejercicio en **esa** sesión con **esa** dosis (progresión o recuperación) |
| REINCORPORACION | El paso registrado es una reincorporación: se valida como dosis base |
| REDUCCION | Una adaptación APLICADA de esa sesión bajó la dosis, sin subir nada y con el mismo descanso |
| HEREDADA | Copia idéntica de un PASO o una REDUCCION anterior |
| Fecha reducible | La sesión viene de un ACORTAR aplicado por copias o pasos (D8, exención de V20) |
| **SIN_AUTORIZACION** | Ninguna regla la explica: **violación real** |
| AMBIGUO | Más de un intento por plan, un informe ilegible o un ejercicio repetido: **NO_CONCLUYENTE**, nunca válida |

**Resultados:**
- **Repetición de la campaña (6 usuarios, 48 semanas):** 25 VALIDA, 23 VALIDA_CON_AUTORIZACIONES y **0 INVALIDA**.
- **Escenario con IA real:** las 11 semanas son VALIDA_CON_AUTORIZACIONES.
- **Que sigue detectando violaciones** (`ProgresionIT.validacionIndependienteDistingue`):
  - Una semana con un paso real da VALIDA_CON_AUTORIZACIONES (PASO = 1).
  - La misma semana con +1 repetición puesta a mano da **INVALIDA** (SIN_AUTORIZACION = 1, V24).

## 4. Otros cambios del instrumento

`ReincorporacionIaRealIT` tiene dos modos:
- **Ensayo seco (`=seco`):** mismos participantes y planes, con una corrección simulada que solo retira el último relleno. Es la comprobación determinista previa que pide la orden.
- **IA real (`=true`):** con presupuesto y registro completo de las predicciones.

Las dos modalidades marcan su evidencia por separado.
