# Auditoría de ejecución de entrenamientos

La auditoría parte del commit `190e2f7`, en la rama `generation`. El alcance es la consulta, el inicio, el registro, el cierre, la omisión, la interrupción y el reporte retroactivo de los entrenamientos generados.

No se tocaron la generación, P2-D, V34 ni el catálogo en revisión. Tampoco hubo migraciones ni llamadas a Replicate.

## Lo que ya existía y funciona

| Paso | Endpoint | Resultado |
|---|---|---|
| Consultar la semana | `GET /users/me/plan/current` y `/users/me/plan/{id}` | Devuelve los entrenamientos con lo indicado, su estado y el bloqueo por seguridad |
| Iniciar | `POST /workouts/{id}/start` | Crea la sesión `IN_PROGRESS` y marca el entrenamiento `IN_PROGRESS`. Rechaza la sesión si el entrenamiento está cerrado, vencido o bloqueado por seguridad, o si hay otra sesión en curso |
| Registrar un ejercicio | `PUT /sessions/{id}/exercises/{plannedExerciseId}` | Guarda series, repeticiones totales, segundos, carga y causa. Reenviar sobrescribe, no acumula |
| Cerrar | `POST /sessions/{id}/finish` | Exige el RPE (1–10). La satisfacción (1–5) y los minutos activos son opcionales. El porcentaje se calcula sobre los ejercicios **indicados** |
| Abandonar | `POST /sessions/{id}/abandon` | La sesión queda `ABANDONED`, no cuenta, y el entrenamiento sigue exigible |
| Omitir | `POST /workouts/{id}/skip` | El entrenamiento queda `SKIPPED` y la causa es obligatoria |
| Reporte retroactivo | `POST /workouts/{id}/report` | Registra en una sola llamada un entrenamiento ya hecho, aunque esté vencido |
| Consultar la sesión | `GET /sessions/{id}` y `/sessions/current` | Devuelve la sesión con sus ejercicios |

Clasificación al cerrar, regla 17.2:

| Porcentaje | Sesión | Entrenamiento |
|---|---|---|
| 80 % o más | `COMPLETED` | `COMPLETED` |
| De 30 % a menos de 80 % | `PARTIAL` | `PARTIAL` |
| Menos de 30 % | se guarda como `PARTIAL` y cuenta con su porcentaje | `SKIPPED` |

Garantías de la base y del dominio:
- `ux_session_active` admite una sola sesión en curso por usuario.
- `ux_session_counted` admite una sola sesión que cuente por entrenamiento.
- `uq_session_attempt` numera los intentos.
- `uq_wse_session_exercise` admite una fila por ejercicio y sesión.
- Las FK sin cascada impiden borrar un plan o un entrenamiento que tiene sesiones.
- La tarea horaria cierra como `ABANDONED` las sesiones en curso desde hace más de 6 horas.

Lo planificado (`planned_workout_exercises`) y lo ejecutado (`workout_session_exercises`) viven en tablas separadas. Ejecutar solo cambia `planned_workouts.status` y `skip_reason`.

## Defectos encontrados y corregidos

Los seis primeros fallan con el código de `190e2f7` en `EjecucionEntrenamientosIT`, y pasan con la corrección. La séptima corrección no tiene una prueba propia.

1. **Omitir un entrenamiento ya cerrado lo pisaba.**
   - Antes: un entrenamiento `COMPLETED` o `PARTIAL` pasaba a `SKIPPED` mientras su sesión seguía contando, y un `REPLACED` también podía omitirse.
   - Ahora: `PlannedWorkout.markSkipped` rechaza `COMPLETED`, `PARTIAL` y `REPLACED`. Sigue admitiendo un `SKIPPED` vencido, para darle después la causa que la tarea de vencimiento deja en NULL.
2. **Un ejercicio inseguro podía registrarse como hecho si se enviaba con causa.**
   - Antes: bastaba una causa más las repeticiones; el ejercicio se clasificaba `COMPLETED` y la causa se descartaba.
   - Ahora: un ejercicio inseguro solo se acepta con causa y sin trabajo.
3. **Se podía reportar sobre una versión `REPLACED`.**
   - Antes: el reporte cerraba la versión vieja, dejaba abierto el mismo día en la vigente, y ese día podía contar dos veces.
   - Ahora: el reporte la rechaza.
4. **El reporte de un entrenamiento vencido (`SKIPPED`) no revisaba la seguridad.**
   - Antes: los conflictos de seguridad solo se calculaban para sesiones abiertas.
   - Ahora: la vista que recibe execution también los calcula para `SKIPPED`, con `PublishedPlanSafetyService.conflictosParaRegistrar`. La lectura del plan no cambia.
5. **Cierres simultáneos de la misma sesión.**
   - Antes: un reintento de la app con mala conexión hacía que los tres cierres respondieran "ok".
   - Ahora: registrar, cerrar y abandonar toman la fila de la sesión con un bloqueo (`findByIdForUpdate`, `PESSIMISTIC_WRITE`), así que uno gana y los demás reciben un 422.
6. **Un registro concurrente con el cierre quedaba fuera del porcentaje.**
   - Antes: la fila entraba después del cierre; la sesión decía 83,33 % y sus filas, 100 %.
   - Ahora: el mismo bloqueo los serializa.
   - La tarea de sesiones colgadas también bloquea la fila, para que no pise un cierre que se hace al mismo tiempo. La prueba de esa carrera pasó también antes de la corrección, así que no demuestra el defecto: solo comprueba que el estado final sea coherente.
7. **Las violaciones de restricciones por peticiones simultáneas respondían 500.**
   - Ejemplos: dos inicios o dos reportes a la vez.
   - Ahora responden 409, sin exponer el nombre de la restricción.

## Pruebas

**`EjecucionEntrenamientosIT`** corre sobre PostgreSQL 16 efímero, con el motor de reglas, y tiene 25 pruebas:
- flujo completo, parcial, por debajo del 30 %, omitido, omitido sin causa, y causa posterior en un vencido seguida de su reporte;
- abandono y reintento, sesión colgada, reenvío de un ejercicio, cierre duplicado, y cuatro carreras: inicios simultáneos, cierres simultáneos, registro contra cierre, y tarea contra cierre;
- transiciones inválidas: una sesión cerrada o abandonada no admite cambios, un entrenamiento cerrado no se omite, una versión `REPLACED` se rechaza, un entrenamiento vencido no se inicia, y se rechazan el ejercicio ajeno, la sesión ajena y el cierre sin RPE;
- historial protegido;
- seguridad: inicio bloqueado (422 por HTTP), causa con trabajo, y reporte de un vencido con un ejercicio inseguro;
- los datos de una semana que recibe la adherencia.

**`EjecucionReglasTest`** tiene 5 pruebas unitarias: omitir según el estado, el ejercicio inseguro, el reporte sobre `REPLACED`, y la respuesta 409.

## Lo que queda disponible para la adherencia

**Denominador** (`planned_workouts`, por plan):
- los entrenamientos indicados, con su estado final, `COMPLETED`, `PARTIAL` o `SKIPPED`;
- la causa declarada del `SKIPPED`;
- para cada ejercicio, lo indicado: series, repeticiones o segundos, carga y descanso.

**Numerador** (`ExecutionContextFacade`):
- `fetchCountedSessions` devuelve, por cada sesión que cuenta: plan, inicio, porcentaje, estado, minutos activos, RPE, satisfacción, ejercicios completados, volumen ejecutado, repeticiones, segundos y causa dominante.
- `fetchCountedResults` devuelve, por entrenamiento, el resultado de cada ejercicio: series, repeticiones totales, segundos, carga, porcentaje, estado y causa.
- `fetchLastCountedSessionAt` devuelve la fecha de la última sesión que contó.

**Para auditar**:
- todos los intentos, con su número y su origen, `APP_TRACKED` o `USER_REPORTED`;
- los abandonados se conservan con lo que registraron.

## Límites conocidos, sin corregir porque no impiden el MVP

- **Reemplazo de la versión con una sesión abierta.** Si la versión se reemplaza mientras hay una sesión en curso, el cierre marca el entrenamiento de la versión vieja. Hoy solo reemplazan la adaptación semanal y la reparación por seguridad, que no está conectada.
- **El cierre duplicado no es idempotente.** El segundo cierre responde 422; la app debe consultar la sesión en vez de reintentar el cierre.
- **Omitir con una sesión abierta.** Corregido después, en la corrección de adherencia: ahora se rechaza hasta que la sesión se cierre o se abandone (ver `ADHERENCIA.md`).
