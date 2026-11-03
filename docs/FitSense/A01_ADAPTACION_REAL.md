# A01 · Primera adaptación real con GPT-5-mini (`ADAPT_LOCALIZED`)

**Resultado: aprobada.** Una ejecución, una predicción y 22 de 22 comprobaciones superadas. Corrida del 27/10/2026 sobre `b03a24f`, en la rama `generation`.

**Naturaleza de la prueba.** Es una ejecución **experimental**:
- la ejecución de la semana se registró con los servicios reales del backend, pero con datos simulados y fechas de prueba;
- no es una semana entrenada por una participante;
- la corrección la hizo GPT-5-mini real, en Replicate.

| | |
|---|---|
| Base de datos | PostgreSQL 16 temporal (Testcontainers), nunca Neon |
| Modelo | `gpt-5-mini`, esfuerzo `medium` |
| Prueba | `A01AdaptacionRealIT`, solo con `-Dfitsense.replicate.real=true`; la suite normal la omite |
| Evidencia | `docs/FitSense/evidencia/A01/` |

## Escenario

**Perfil:**
- `GAIN_MUSCLE`, mujer, intermedia, gimnasio;
- 3 días (lunes, miércoles y viernes) de 45 minutos;
- equipo: mancuerna, barra, polea, máquina de palanca, banco y banda.

**Semana medida** (02 al 08/11/2026): la generó el motor de reglas por los servicios de producción y pasó V1–V25 con 4D en ENFORCE. No se pagó la generación inicial.

| Día | Ejercicios (series × repeticiones, descanso de 90 s) | Duración | Ejecución |
|---|---|---:|---|
| Lun 02/11 | 1059 (3×13), 776, 624, 696, 1099 (3×10) | 32 min | Completada al 100 % |
| Mié 04/11 | 583 (3×10), 550 (2×10), 1006 (3×10), 973 (2×10), 476 (2×13), 896 (3×10) | 31 min | Completada al 100 % |
| Vie 06/11 | 1026, 394, 1148 (3×10), 986 (2×10), **1256 (2×13)**, 1097 (3×10) | 33 min | Omitida por falta de tiempo |

**Fechas.** La orden pedía la semana del 19 al 25/10, pero el reporte retroactivo admite 7 días (V16), así que el lunes 19/10 ya no se podía registrar. Se usaron las dos semanas siguientes, por decisión del 27/10.

**Factibilidad comprobada antes de llamar a la IA:** el viernes sin el relleno 1256 pasa V1–V25, con la misma exención de V20 que aplica P2-D a una sesión autorizada a acortarse.

**Por qué el 1256 («giro de columna») era relleno:**

| Ejercicio | Patrón | Slot 4C | Clase 4D | Dosis exigida |
|---|---|---|---|---|
| 1026 Press de pecho en palanca | HORIZONTAL_PUSH | UPPER_PUSH | ESTRUCTURAL | 3×10 |
| 394 Remo sentado con cuerda | HORIZONTAL_PULL | UPPER_PULL | ESTRUCTURAL | 3×10 |
| 1148 Hiperextensión invertida | HIP_EXTENSION | LOWER_HIP | ESTRUCTURAL | 3×10 |
| 986 Elevación lateral con palanca | SHOULDER_RAISE | ninguno | RESISTIDO | 2×10 (relleno) |
| **1256 Giro de columna** | TRUNK_ROTATION_LATERAL_FLEXION | **ninguno** | RESISTIDO | **2×13** (relleno; cintura, zona pequeña, +3 repeticiones) |
| 1097 Flexión en pared | HORIZONTAL_PUSH | UPPER_PUSH | ESTRUCTURAL | 3×10 |

El 1256 no cubre ningún slot de 4C y lleva la dosis de relleno. Sin él, los cuatro slots de la semana siguen cubiertos (`faltan []` en el log).

## Medición y decisión (cálculo real del ciclo)

**Métricas:**
- Adherencia oficial A_w = (1 + 1 + 0) / 3 = **66,67 %**; observada **66,67 %**; cobertura **100 %**.
- Riesgo 15 (LOW). Sin abandono.

**Decisión:** `ADAPT_LOCALIZED`, porque la observada está entre 50 % y 80 %.
- Señalada solo la sesión 3 (viernes), con intervención `ACORTAR_O_REORGANIZAR`.
- `forcedSessionMinutes` = null.

## Corrección de GPT-5-mini

| | |
|---|---|
| Predicción | `3nqnh6tffdrmy0d1fb7tqhh294` |
| Intentos | 1 (`SUCCEEDED`), sin reintentos |
| Tiempo | 23,2 s (POST 23.248 ms, sin sondeo) |
| Prompt | FIX-PR-1.3 / FIX-1.0, 198.205 caracteres (`prompt_intento_1.txt`) |

**Respuesta:** quitó el 1256 y mantuvo las otras cinco colocaciones con sus dosis. Declaró 28 minutos, sin cambiar el día. Su justificación: «Recorté una colocación de relleno para ajustar la sesión por falta de tiempo y mantuve las dosis de los ejercicios que se quedan.»

## Validación

- **V8C:** 0 problemas. Aplicada la 3; conservadas la 1 y la 2; sin sustituciones; el viernes autorizado a acortarse.
- **Validación estricta V1–V25:**
  - 4D: 16 colocaciones comprobadas, 0 incumplimientos;
  - capacidades: COMPLETO;
  - 4C: sin slots faltantes.
- **Revalidación de la semana publicada** con el validador de producción: 0 problemas.

## Semana publicada (09 al 15/11/2026)

| Día | Origen | Cambio |
|---|---|---|
| Lun 09/11 | sesión 1 | Idéntica: mismos ejercicios, dosis, enfoque y 32 min |
| Mié 11/11 | sesión 2 | Idéntica: mismos ejercicios, dosis, enfoque y 31 min |
| Vie 13/11 | sesión 3 | Retirado el 1256 (2×13). Las otras cinco sin cambios. 33 → 28 min |

**Plan publicado:**
- plan 2, versión 1, `ACTIVE`, la única versión vigente de la semana;
- `generation_source` AI, `model_name` gpt-5-mini;
- linaje V31 en las tres sesiones;
- ninguna sesión bloqueada y ningún ejercicio nuevo.

**Evidencia V32** (`plan_adaptation_attempts`):

| `decision` | `status` | `generation_source` | `ai_calls` | `targeted_sessions` | `applied_sessions` | `conserved_sessions` | `source_plan_id` | `resulting_plan_id` |
|---|---|---|---:|---|---|---|---:|---:|
| ADAPT_LOCALIZED | APLICADA | AI | 1 | {3} | {3} | {1,2} | 1 | 2 |

**Intervención:** la 1, enlazada al plan 2. `trigger_adherence_pct` 66,67; volumen 481 → 455 (−5,41 %). El historial de la semana medida (sesiones, ejercicios ejecutados y lo indicado) quedó intacto.

## La primera ejecución (preliminar)

El mismo escenario corrió una vez antes, con la predicción `x3q2h3nh4xrmt0d1fb3as6wsfc`:
- 1 intento y 28,9 s;
- la misma corrección (retiró el 1256);
- `CORRECCION_IA`, V8C con 0 problemas, publicada según el log.

El arnés falló **después** de publicar, al serializar un arreglo de PostgreSQL, y la base temporal se perdió. Por eso no cuenta como expediente.

Antes de esta segunda corrida, un ensayo sin IA (`-Dfitsense.a01.seco=true`, 0 llamadas) comprobó que toda la evidencia se conserva aunque una comprobación falle.

## Archivos

| Archivo | Contenido |
|---|---|
| `evidencia.json` | Perfil, plan inicial, factibilidad, clasificación del viernes, ejecución, métricas, V32, intervención, plan resultante, diferencias, revalidación y las 22 comprobaciones |
| `prompt_intento_1.txt` | Prompt de corrección enviado (sin credenciales: el token lo pone el proxy) |
| `snapshot_intento_1.json` | Datos de entrada de la corrección |
| `respuesta_intento_1.json` | Respuesta cruda de GPT-5-mini |
| `log_produccion.txt` | Líneas del log de producción: tiempos, 4C, 4D, capacidades, publicación |
