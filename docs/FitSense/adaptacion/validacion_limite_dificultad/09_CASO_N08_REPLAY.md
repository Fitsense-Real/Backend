# 09 · Caso N08 en la REPLAY (100 semanas)

**Prueba:** `CampanaP123Cohorte02IT` en modo REPLAY, con las respuestas de IA **registradas** de la campaña P123. No hace llamadas a Replicate.

Se ejecutó con `off`, `on` y `preferencia` sobre `2385b64`. La evidencia está en `evidencia/`:

| Fichero | Contenido |
|---|---|
| `replay_off.diff` | diferencias respecto de las evidencias versionadas, con `off` |
| `replay_on.diff` | ídem, con `on` |
| `replay_preferencia.diff` | ídem, con `preferencia` |
| `n08_rechazos_on.log` | los rechazos de N08 |

Las evidencias versionadas de `docs/FitSense/campana_p123/evidencia/` se restauraron tras cada corrida; no se sobrescriben.

## Resultado por configuración

| Configuración | Diferencias con la evidencia versionada |
|---|---|
| `off` | Solo marcas de tiempo de `exclusiones_dolor.csv` (`recorded_at`, `reactivated_at`). **Comportamiento histórico intacto.** |
| `on` | N08 (usuario 9) desde la semana 2 de la campaña, más las mismas marcas de tiempo. |
| `preferencia` | **Idéntico a `on`**, y ambos idénticos a la corrida del 03/11 con el límite (`docs/FitSense/limite_dificultad/evidencia/replay_con_limite.diff`). |

## N08, semana 2 (dolor): comprobaciones de la fase 6

| # | Comprobación | Histórico / `off` | `on` / `preferencia` |
|---|---|---|---|
| 1 | Rechazo exacto | — (se acepta) | «V8C: la sesion 62 esta senalada como SEGURIDAD_ANTE_DOLOR y sustituye por el ejercicio 124, de dificultad registrada 2, cuando el retirado que le corresponde (791) tiene 1. Con el limite de dificultad activo, un sustituto no puede ser de dificultad registrada mayor que la del ejercicio que reemplaza.» |
| 2 | Los tres intentos | 1 llamada (`ai_calls = 1`) | **3** (`ai_calls = 3`); intentos 1, 2 y 3 con el mismo motivo, luego «Sin correccion usable tras 3 intento(s)» |
| 3 | Motivo del rechazo | — | Solo el límite de dificultad; ningún otro validador objeta |
| 4 | Selección del respaldo | — | SUST-SEG-1, `motivo dolor_registrado`: **791 → 1003** |
| 5 | AI frente a RESPALDO | `generation_source = AI`, `status = APLICADA`, `applied_sessions = {62}` | `generation_source = CONSERVED`, `status = RESPALDO`, `no_effect_sessions = {62}`, `conservation_report.sustituciones = [791→1003]`, `correcciones_del_backend = ["REGENERACION_SEGURA: sesiones [62] resueltas sin IA por sustitucion SUST-SEG-1 de [791->1003]"]` |
| 6 | Sesión publicada (16/11) | 124 · 239 · 102 · 295 · 1175 · 1075 | **1003** · 239 · 102 · 295 · 1175 · 1075. Solo cambia la posición 1, con la misma dosis (3x6, 150 s) |
| 7 | Exclusión del original | 791 `DOLOR_IDENTIFICADO`, reactivada por confirmación explícita en el guion | Igual: la fila 1 de `exclusiones_dolor.csv` solo cambia en las marcas de tiempo |
| 8 | Semanas posteriores | La sesión de origen 62, en las semanas 23/11 a 11/01 (8 semanas), lleva **124** | Las mismas 8 semanas llevan **1003**; el resto de cada plan es idéntico (50 líneas de `planes.csv`, todas del usuario 9) |
| 9 | Adherencia | — | `metricas.csv` **sin cambios** |
| 10 | Linaje | 100←62, 138←100, 176←138… | El mismo: la sustitución conserva `source_planned_workout_id` |
| 11 | Otros usuarios | — | **Ninguno cambia.** El diff solo tiene filas del usuario 9, N08 en `historial.csv` y los nombres 124 y 1003 en `nombres_ejercicios.csv` |

Fichas de los ejercicios, del catálogo V44_2:

| Id | Nombre | Zona, equipo, patrón, músculo | Dificultad |
|---|---|---|---:|
| 791 | «Sentadilla con apoyo de mancuerna» | upper legs, dumbbell, KNEE_DOMINANT, quads | 1 |
| 124 | «Sentadilla dividida a una pierna con banda y una mano» | band, KNEE_DOMINANT | 2 |
| 1003 | «Prensa alterna de piernas con palanca» | leverage machine, KNEE_DOMINANT, quads | 1 |

**Registro como cambio funcional esperado:** con el límite activo, N08 pasa de **AI/APLICADA** a **RESPALDO/CONSERVED con sustitución SUST-SEG-1**, y el ejercicio de la posición pasa de dificultad 2 a 1 durante 9 semanas (la semana del dolor y las 8 siguientes). No es una regresión: es exactamente el aumento que el límite debe impedir.

Una observación menor, R-7 en el 14: con RESPALDO la sesión 62 figura como `no_effect` aunque lleve la sustitución de seguridad. Así registra hoy el respaldo y no lo cambia el límite.
