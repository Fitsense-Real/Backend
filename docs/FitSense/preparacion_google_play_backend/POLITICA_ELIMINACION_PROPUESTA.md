# Política de eliminación (propuesta para aprobación)

**Estado:**
- Alternativa A implementada (PR A).
- Alternativa B solo documentada y **no activada**.

La decisión entre ambas corresponde a la responsable del estudio y, cuando proceda, al comité de ética. Este documento no afirma que exista una aprobación.

## 1. Retirarse del estudio no es eliminar la cuenta

| | Retirada (`POST /api/v1/users/me/withdraw`, ya existía) | Eliminación (`DELETE /api/v1/users/me`, nueva) |
|---|---|---|
| Acceso a la app | Se mantiene | Termina en el acto |
| Nuevos planes | No (el cierre semanal excluye a quien tiene `withdrawn_at`) | No |
| Datos ya recogidos | Se conservan | Se borran todos (alternativa A) |
| Reversible | Sí, con intervención del equipo | No |

La app debe explicar las dos opciones por separado. Retirarse no es una forma de borrar los datos.

## 2. Alternativa A: eliminación completa (activa)

**Qué se borra:** todas las filas de la persona en las 11 tablas del inventario, más las que dependen de ellas en cascada. Esto incluye:
- el perfil y los textos libres;
- los planes con sus `input_snapshot` y `output_snapshot`;
- las sesiones, las métricas y la adaptación;
- las exclusiones por dolor;
- el libro de predicciones con la salida de la IA;
- los tokens.

**Qué queda:**
- Una fila en `account_deletions`: id aleatorio, origen (APP o WEB), fechas y recuento de filas borradas por tabla. Al completarse no guarda `user_id`, correo, nombre ni código.
- Fuera de la base, lo descrito en el apartado 4.

**Garantías implementadas:**

1. **Corte inmediato.** Al registrar la solicitud, la cuenta pasa a `DELETED`. Desde ese momento:
   - falla el inicio de sesión;
   - deja de servir cualquier JWT emitido;
   - las generaciones en curso quedan cerradas (FAILED / `CUENTA_ELIMINADA`, sin reintento).
2. **Nada se publica después.** El worker comprueba la operación antes de cada predicción y antes de publicar (fencing de V59), así que no envía más predicciones a Replicate ni publica un plan.
3. **Todo o nada.** El borrado es una sola transacción con la fila de `users` bloqueada:
   - una escritura concurrente que llegue tarde no deja filas huérfanas, sino que falla por la FK;
   - si algo falla, se deshace entero y la eliminación sigue PENDIENTE;
   - la tarea programada la reanuda cada 5 minutos;
   - la respuesta no dice "eliminada" hasta que lo está.
4. **Una sola por cuenta.** El índice único parcial impide dos eliminaciones PENDIENTE simultáneas, y las solicitudes repetidas devuelven la misma.
5. **Códigos que no se reutilizan.** Con `participant_code_seq`, un P-xxx eliminado no vuelve a asignarse.

## 3. Alternativa B: anonimización irreversible (NO activada)

Solo sería aceptable si el estudio necesita conservar datos de quien elimina su cuenta y esa conservación tiene una base aprobada. **Sustituir el nombre por P-xxx no es anonimizar.** El código seudónimo sigue enlazando todas las filas, y los datos que quedan son casi identificativos en una cohorte pequeña:
- edad exacta, sexo, altura y peso;
- zona horaria;
- fechas de cada sesión;
- textos libres.

Para que un registro pueda considerarse anónimo, como mínimo:

| Requisito | Por qué |
|---|---|
| Borrar `users`, el perfil completo, `goal_text`, `health_notes`, `input_snapshot`, `output_snapshot` y la salida de la IA del libro | Son texto libre o copias completas del perfil |
| Romper el enlace: un identificador nuevo aleatorio por persona, sin tabla de correspondencia, o agregados sin identificador | El P-xxx se puede volver a enlazar con lo que conserve el equipo |
| Generalizar los casi identificadores: edad en rangos, altura y peso en rangos o fuera, fechas convertidas en semana relativa del estudio | Una edad exacta con altura y peso identifica en decenas de personas |
| Quitar las exclusiones por dolor con su ejercicio y fecha, o conservarlas solo como recuentos | Es dato de salud con fecha |
| Evaluar el riesgo de reidentificación con el conjunto final (k-anonimato o la regla que fije el comité) | La anonimización se comprueba sobre los datos que quedan, no se presume |

Si se aprueba, se implementaría como un modo alternativo de `EliminacionDeCuentaService`:
- propiedad `fitsense.account-deletion.mode=ANONIMIZAR`, que hoy no existe;
- transformaciones revisadas;
- pruebas de que no queda ningún campo de la lista.

Mientras no haya aprobación, **ningún mecanismo conserva automáticamente registros identificables después de eliminar una cuenta.**

## 4. Lo que el borrado de FitSense no alcanza

| Dónde | Qué | Qué hacer |
|---|---|---|
| Replicate y el proveedor del modelo | Las entradas y salidas de cada predicción ya enviada | Replicate conserva datos de predicción según su política y el tipo de cuenta. **No se ha verificado el plazo.** Pendiente: consultar su política vigente o usar su API de borrado de predicciones, si la ofrece, al eliminar la cuenta. No se ha implementado ninguna llamada nueva a Replicate |
| Copias de seguridad / historial de PostgreSQL | Todo, hasta que caduque la copia | Comprobar el plazo en la consola del proveedor y declararlo |
| Logs de Railway | Ids internos y de operación, sin correo ni contraseña | Comprobar el plazo de retención del plan |
| Exportaciones ya hechas para el análisis | Lo exportado | Decisión del estudio: si se excluye a quien elimina su cuenta, y hasta qué fecha |

## 5. Decisiones que esta propuesta deja abiertas

Están en [PENDIENTES_APROBACION.md](PENDIENTES_APROBACION.md):
- A o B;
- qué hacer con los datos ya exportados;
- plazo de `account_deletions`;
- si se conserva la prueba del consentimiento;
- texto que verá la persona antes de confirmar.
