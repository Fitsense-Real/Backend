# Revisión del contrato de la API (P-01, P-02 y P-03)

> **Actualización (orden de fusión del PR #16).** P-01 queda en **30–75 minutos para los cuatro objetivos**. Fuera de rango, 422 con «La duración por sesión debe estar entre 30 y 75 minutos (objetivo X); indicaste N.». V4 se mantiene: con 75 min, el techo es 86. Los perfiles históricos fuera de 30–75 no se modifican: no generan plan y reciben `PERFIL_FUERA_DE_P01`. Limitación conocida, no oculta: GAIN_MUSCLE a 75 min sigue sin plan posible con V20 y 4D (la sesión más larga posible es de unos 46 min y el piso de 75 min es 52,5). Se estudia en `docs/FitSense/investigacion_reglas_entrenamiento/`.


## 1. Endpoints nuevos (P-02)

### `GET /api/v1/users/me/pain-exclusions`

| | |
|---|---|
| Autenticación | JWT obligatorio; sin token, **401** |
| Respuesta 200 | `[{exclusionId, exerciseId, exerciseName, reason, eventDate, sourcePlannedWorkoutId, status, recordedAt, reactivatedAt}]`, activas y reactivadas, del usuario autenticado |
| Usuario sin exclusiones | **200 con `[]`** |
| Aislamiento | Solo las del usuario del token |

### `POST /api/v1/users/me/pain-exclusions/{exerciseId}/reactivation`

**Cuerpo:** `{"confirmNoPain": true, "comment": "opcional, ≤ 300"}`.

| Código | Cuándo |
|---|---|
| **200** | `{exerciseId, exclusionesReactivadas, reactivadaEn, candidatoHoy, nota}` |
| **401** | Sin token |
| **404** | No hay una exclusión **activa propia** de ese ejercicio (incluido el caso de que exista para otro usuario) |
| **422** | `confirmNoPain` no es `true`, o `REACTIVACION_RECHAZADA_POR_SEGURIDAD` (el ejercicio choca hoy con una restricción vigente) |
| **400** | Cuerpo inválido (`confirmNoPain` nulo, o `comment` de más de 300 caracteres) |

- **Trazabilidad:** fecha, motivo y evidencia de la acción explícita se guardan en la fila.
- **No reincorpora:** el ejercicio solo vuelve al conjunto elegible. `nota` lo explica y `candidatoHoy` dice si hoy sería candidato.
- **Pruebas:** `PoliticasP123IT.reactivacion` (401 en GET y POST, 404, 422, 200, GET vacío y propio, no reincorporación), `reactivacionRechazadaPorSeguridad`, `reactivacionesSimultaneas` y `dolorNuevoTrasReactivar`.

## 2. Resultados de adaptación (`GET /api/v1/users/me/plan/adaptations`)

**Recurso `AdaptationOutcome` sin cambios de forma:** `attemptId, weekStartDate, resultingPlanId, resultado, estado, decision, llamadasIa, motivos, sesiones` (comprobado en `AdaptationOutcomeTest.camposDelContrato`). Ningún campo se renombró ni se eliminó.

| `resultado` | Estado V32 | Significado |
|---|---|---|
| ADAPTACION_APLICADA, CONSERVACION_DELIBERADA, SIN_CAMBIOS_VALIDOS, ACORTAMIENTO_IMPOSIBLE, REPROGRAMACION_IMPOSIBLE, ADAPTACION_IMPOSIBLE, RESPALDO_PROPUESTA_IA_INVALIDA, RESPALDO_SIN_IA, NO_FACTIBLE_POR_SEGURIDAD, NO_FACTIBLE, PROGRESION_DEL_BACKEND, SIN_ADAPTACION | Sin cambios | Los de siempre (pruebas intactas) |
| **REGENERACION_SEGURA_SUSTITUCION** (nuevo) | `RESPALDO` | P-03 nivel 1: el dolor se resolvió sin la IA, sustituyendo lo que dolió |
| **REGENERACION_SEGURA_REGLAS** (nuevo) | `NO_FACTIBLE` (la adaptación falló y consta) | P-03 nivel 2: semana regenerada con reglas en el mismo ciclo. `resultingPlanId` es el plan regenerado y `motivos` lo explica |

- **NO_FACTIBLE con P-03 sin alternativa:** `resultado = NO_FACTIBLE_POR_SEGURIDAD`, `resultingPlanId = null` y en `motivos` aparece `P-03: sin alternativa segura válida: …` (por ejemplo, la regeneración subiría el trabajo).
- **El estado V32 se informa tal cual:** la API no reinterpreta un NO_FACTIBLE como éxito; el resultado dice cómo acabó la semana.

## 3. Errores de P-01 (contrato de siempre)

| Caso | Respuesta |
|---|---|
| Alta o edición con minutos fuera de 30–120 | **422**, mensaje «La duración por sesión debe estar entre 30 y 120 minutos (objetivo X); indicaste N.» |
| Generación para un perfil histórico fuera de rango | **422**, `PERFIL_FUERA_DE_P01: …` |

## 4. Compatibilidad con los consumidores actuales

- Solo hay **adiciones**: dos endpoints y dos valores nuevos de `resultado`.
- Un cliente que no conozca los valores nuevos los recibe como un texto más.
- El mensaje de error de P-01 cambió de texto (el formato `ApiErrorResource` es el mismo).

## 5. Requisitos para Flutter (documentados, **no implementados**)

1. **Alta del perfil:** limitar el selector de minutos a 30–120 para todos los objetivos y mostrar el mensaje 422.
2. **Pantalla «Ejercicios en pausa por dolor»:** `GET /users/me/pain-exclusions`.
3. **Reactivar:** confirmación explícita («ya no siento molestias con este ejercicio y quiero que vuelva a considerarse»), sin preguntas médicas. Tratar 404, 422 y `REACTIVACION_RECHAZADA_POR_SEGURIDAD`, y mostrar `nota`: reactivar no garantiza que el ejercicio aparezca.
4. **Semana adaptada:** mostrar `REGENERACION_SEGURA_SUSTITUCION` («cambiamos el ejercicio que te dolió») y `REGENERACION_SEGURA_REGLAS` («rehicimos la semana sin lo que te dolió»).
5. **Semana sin plan por P-03:** mostrar NO_FACTIBLE_POR_SEGURIDAD con su motivo.
6. **Perfil histórico fuera de rango:** mostrar `PERFIL_FUERA_DE_P01`. No hay edición del perfil congelado (P-04 no se implementa).
