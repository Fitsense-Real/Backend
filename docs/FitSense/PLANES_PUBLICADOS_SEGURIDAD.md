# Planes ya publicados con un ejercicio que hoy no es seguro

**Fecha:** 24/10/2026 · **Estado:** desarrollado y probado en PostgreSQL efímero. **No desplegado.** No se ha ejecutado ninguna reparación ni consulta sobre datos reales. V34 sigue fuera de `db/migration`.

## 1. El problema

Un plan guardado no se revalida solo. Si un ejercicio deja de ser compatible después de publicarse (el 1374, una restricción declarada después, una bandera corregida), la sesión lo sigue mostrando. SUST-SEG-1 solo arregla la semana **siguiente**.

## 2. Qué se hizo

| Parte | Qué hace | Dónde |
|---|---|---|
| **Causa verificable** | Un ejercicio choca con la seguridad solo si hay una causa comprobable: una bandera del catálogo contra una restricción actual del perfil, la retención `INC-4C-03` o la contención `INC-CAT-1374`. Un bloqueo, el equipamiento o el filtro funcional **no** son causas. La misma regla sirve para SUST-SEG-1, para V8C y para los planes publicados | `SafetyConflictPolicy`, `SafetyConflictChecker` |
| **Lectura marcada** | `GET /users/me/plan/current` y `/plan/{id}` comprueban las sesiones **abiertas** (`SCHEDULED`, `IN_PROGRESS`) de un plan **activo** contra la seguridad de hoy. Campos nuevos: `safetyBlocked` y `safetyBlockMessage` por sesión, y `safetyBlockReason` por ejercicio. Son aditivos: un cliente que no los lea sigue funcionando. El histórico (sesiones terminadas, `REPLACED`, planes no activos) se devuelve tal cual, sin marcar | `PublishedPlanSafetyService`, `TrainingPlanController` |
| **Ejecución rechazada en el backend** | `POST /workouts/{id}/start` responde **422** con un mensaje que empieza por `SESION_BLOQUEADA_POR_SEGURIDAD`. No crea sesión ni marca el entrenamiento en curso. En una sesión abierta **antes** del bloqueo, registrar como hecho el ejercicio inseguro también da 422; registrarlo como omitido sí se admite. El reporte retroactivo de una sesión aún abierta aplica la misma regla; el de una sesión cerrada es histórico y no cambia | `WorkoutSessionCommandServiceImpl`, `PlannedWorkoutView.safetyConflicts` |
| **Reparación por versión nueva** | Publica la versión `n+1` de la semana con el ejercicio sustituido por SUST-SEG-1. La versión anterior pasa a `REPLACED` **sin cambiar su contenido**. Es idempotente y no deja dos versiones activas | `PublishedPlanSafetyRepairService` |
| **Exposición real** | Consultas de solo lectura | `consultas/exposicion_planes_publicados.sql` |

**Esta fase no toca Flutter.** El backend ya impide iniciar la sesión aunque la app no lea los campos nuevos. Que la app muestre el aviso es trabajo de Flutter.

## 3. La reparación

`PublishedPlanSafetyRepairService.reparar(usuario, semana, hoy)` devuelve uno de estos estados:

| Estado | Cuándo | Qué queda |
|---|---|---|
| `REPARADA` | Semana activa, ninguna sesión empezada, con sustituto y la semana entera válida | La versión `n+1` es la activa (`adjustment_applied = SAFETY_REPAIR`, `output_snapshot` `SAFETY_REPAIR_V1`). La versión `n`, `REPLACED` e intacta |
| `NADA_QUE_REPARAR` | Ningún conflicto, o ya reparada (reintento) | Nada cambia |
| `SIN_SUSTITUTO` | Ningún ejercicio seguro del mismo slot | Ninguna versión nueva. La sesión sigue bloqueada y requiere intervención |
| `NO_VALIDA` | La semana reparada no pasa V8C o la validación estricta, o no se pudo preparar el contexto de hoy | Ninguna versión nueva. La sesión sigue bloqueada |
| `SESION_EN_CURSO` | Hay una sesión en curso | Se reintenta cuando termine |
| `SEMANA_EMPEZADA` | Ya hay sesiones terminadas esta semana | Ninguna versión nueva. La sesión sigue **bloqueada**. **Requiere decisión** (sección 4) |
| `SEMANA_PASADA` | La semana ya terminó | Histórico: no se repara |
| `CAMBIO_CONCURRENTE` | Otra escritura publicó a la vez | No se publica nada. Lo garantizan `ux_plan_active`, `uq_plan_user_week_version` y la comprobación de que la versión leída sigue activa |

**Qué se garantiza:**
- **Una sola versión cambia de contenido: la nueva.** La nueva repite cada sesión con la misma fecha, enfoque, nombre, duración y vencimiento, y cada ejercicio con su dosis, salvo el sustituido.
- **El linaje V31 se conserva:** cada sesión nueva apunta al mismo origen de la semana anterior que la reemplazada. La correspondencia con la reemplazada queda en el `output_snapshot`.
- **La semana entera se valida:** V8C, como copia exacta salvo las sustituciones con causa verificada, y la validación estricta (V1–V24, dosis 4D en `ENFORCE`).

**Va por fases, sin una transacción que lo envuelva todo:** leer, preparar el contexto de hoy, validar y escribir en una transacción corta. Así un fallo al preparar no puede dejar marcada para rollback la escritura.

**Nadie la llama todavía.** Solo está `PlanningContextFacade.repairPublishedPlanForSafety`, sin endpoint ni tarea, porque ejecutarla sobre planes reales no está autorizado.

**Evidencia de la reparación:** se guarda en la versión nueva (`adjustment_reason` y `output_snapshot`) y en el log `REPARACION_SEGURIDAD`. No va en `plan_adaptation_attempts`: V32 exige que la semana nueva sea posterior a la medida, y una reparación es de la misma semana. Un rechazo (`SIN_SUSTITUTO`, `NO_VALIDA`…) solo queda en el log. Guardarlo en base necesitaría una migración nueva.

## 4. Decisión pendiente: la semana ya empezada

**El caso más probable en la práctica es este:** el participante hizo la sesión del lunes y el 1374 está el miércoles.

**Por qué no se versiona.** Una versión describe la semana entera:
- `PreviousWeekAssembler`, que da la semana medida de la próxima adaptación, lee **solo la última versión**;
- V32 exige que las sesiones conservadas pertenezcan a **un único plan de origen**.

Una versión nueva a mitad de semana obliga a una de dos cosas:
- **copiar** las sesiones ya hechas: se duplican en el denominador de adherencia, sin sus resultados, porque las sesiones de ejecución apuntan a las originales;
- **dejarlas fuera:** la próxima adaptación partiría de una semana incompleta.

Ninguna de las dos es aceptable sin una decisión.

**Qué pasa hoy en ese caso:**
- La sesión insegura queda **bloqueada en lectura y en ejecución**.
- Las demás sesiones se hacen normal.
- La tarea nocturna cierra la sesión bloqueada como `SKIPPED` al vencer.
- **Consecuencia:** cuenta como no realizada en la adherencia, aunque no sea culpa del participante.

**Opciones para decidir:**
1. **Bloquear y no versionar** (lo implementado). Es simple y seguro, pero la sesión bloqueada penaliza la adherencia de esa semana.
2. **Lo mismo, y la sesión bloqueada no cuenta en el denominador.** Se cerraría con un estado o motivo propio, por ejemplo `SKIPPED` con causa de seguridad. Necesita una migración, porque `ck_pw_skip_reason` no tiene ese valor.
3. **Semana compuesta de varias versiones.** El ensamblador, el volumen y V32 leerían la unión de las sesiones no reemplazadas de todas las versiones. Es el cambio más profundo: toca P2-D, V32 (una migración nueva) y la medición.

## 5. Exposición real

Las consultas de `docs/FitSense/consultas/exposicion_planes_publicados.sql` son **solo lectura**: corren en `BEGIN TRANSACTION READ ONLY … ROLLBACK`. Miden:
- autorizaciones con `IS DISTINCT FROM TRUE`;
- equipamiento normalizado con `lower(btrim())`;
- lo **activo o futuro** separado del **histórico**;
- **participantes distintos**, no combinaciones de perfil.

Calculan cinco cosas:
1. un resumen del 1374;
2. el detalle de lo activo con su motivo, y si la semana ya empezó;
3. si el 1374 se ejecutó alguna vez;
4. los retenidos INC-4C-03;
5. cualquier ejercicio que choque con una restricción **declarada**.

Al final, una comprobación de integridad: ninguna semana con dos versiones activas.

**No se han ejecutado contra producción:** esta sesión no tiene credenciales de Neon (`DB_PASSWORD` no está definida). Para ejecutarlas:

```bash
psql "<URL de lectura de Neon>" -v ON_ERROR_STOP=1 -f docs/FitSense/consultas/exposicion_planes_publicados.sql
```

**Limitación:** la política por edad y nivel no está en SQL; esa comprobación completa la hace el backend. El archivo se probó en dos sitios:
- en una base vacía con V1–V33, sin errores;
- en `ProteccionPlanesPublicadosIT` › `3`: con datos, cuenta como incompatible exactamente la sesión que el backend bloquea, y no cuenta el caso compatible.

## 6. Pruebas

**`ProteccionPlanesPublicadosIT`:** PostgreSQL 16 efímero con V1–V33, motor de reglas, sin IA ni Replicate. El 1374 se inyecta como si se hubiera publicado antes de la contención. 12/12.

| Caso | Resultado |
|---|---|
| 1374 sin barra · lectura | Solo esa sesión sale `safetyBlocked`, con `safetyBlockReason = INC-CAT-1374` en el ejercicio |
| 1374 sin barra · iniciar | 422 `SESION_BLOQUEADA_POR_SEGURIDAD`; ni sesión ni `IN_PROGRESS`. Otra sesión se inicia normal |
| Sesión abierta antes del bloqueo | Registrar el 1374 como hecho se rechaza; como omitido, se admite |
| 1374 **compatible** (barra, sin restricciones) | Nada bloqueado; se inicia; `NADA_QUE_REPARAR`; el contenido no cambia |
| Sustitución exitosa | Versión 2 activa, hija de la 1. La 1 queda `REPLACED` con el **mismo contenido** fila a fila. La 2 es idéntica salvo el 1374, sustituido por un relleno. Snapshot `SAFETY_REPAIR_V1`. La 2 se lee sin bloqueos y se inicia; la 1 sigue consultable |
| Reintento duplicado | `NADA_QUE_REPARAR`; siguen siendo 2 versiones y 1 activa |
| Dos reparaciones simultáneas | Una `REPARADA` y otra `CAMBIO_CONCURRENTE` (en la corrida la frenó `uq_plan_user_week_version`); 2 versiones, 1 activa |
| Sin sustituto (todo el slot exige suelo) | `SIN_SUSTITUTO` con `SEGURIDAD: …`; ninguna versión nueva; contenido intacto; la sesión sigue bloqueada (`suelo`) y no se inicia |
| Semana empezada | `SEMANA_EMPEZADA`; ninguna versión nueva; sigue bloqueada |
| Sesión en curso | `SESION_EN_CURSO`; ninguna versión nueva |
| Semana pasada | `SEMANA_PASADA`; contenido intacto y consultable |
| Consulta de exposición | Detecta la sesión que el backend bloquea; ignora la compatible; ninguna semana con dos versiones activas |

**Unitarias** (`SustitucionSeguridadTest`, que pasa de 17 a 22):
- una autorización fabricada sin causa verificada (un bloqueo) se rechaza, y el cambio sigue contando como diferencia;
- un motivo distinto de la causa verificada se rechaza;
- una exclusión con un motivo que no es de seguridad no se puede ni construir;
- las causas salen de la bandera contra la restricción.

**Resultados del 24/10/2026:**
- `mvn test`: 611/611.
- `mvn verify -Pintegration`: 49 ejecutadas, 0 fallos, 8 saltadas: las P2-D antiguas, que exigen `-Dfitsense.pruebas.url`.
- Esas tres clases (`InvariantesV33IT`, `PublicacionAtomicaIT`, `DisparadorDiferidoIT`) contra un PostgreSQL local con V1–V33: 7/7, sin residuos.
- `CorreccionReplicateEvidenciaIT`: pendiente de una corrida pagada.

## 7. Pendiente antes de desplegar

1. Ejecutar las consultas de exposición en producción, con un usuario de lectura.
2. Decidir el caso de la semana empezada (sección 4).
3. Decidir **cómo se dispara** la reparación: tarea, comando de administración o paso de la tarea del lunes. Y autorizarla sobre datos reales.
4. Que Flutter muestre `safetyBlocked`, `safetyBlockMessage` y `safetyBlockReason`.
5. V34, que corrige la fila del 1374 en el catálogo, sigue sin desplegar. Con V34 aplicada, las banderas del 1374 darán la causa directamente (`suspension`, `brazos_arriba`), además de la contención.
