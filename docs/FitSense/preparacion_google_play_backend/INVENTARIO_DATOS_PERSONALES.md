# Inventario de datos personales (backend)

**Base:** `generation` = `a1ccf53`, con V60 aplicada encima.

**Método:**
- Base PostgreSQL 16 aislada, migrada desde cero con V1–V60.
- Lectura de `information_schema` y `pg_constraint`: columnas, FK y acciones `ON DELETE`.
- Lectura del código que escribe cada tabla, del que llama a Replicate y de los logs.

No se consultó ninguna base con datos reales.

## 1. Tablas con datos de una persona

| Dato | Tabla | Relación con usuario | Sensibilidad | Tratamiento propuesto |
|---|---|---|---|---|
| Correo, hash de contraseña (BCrypt), nombre, apellido | `users` | Fila propia (`user_id`) | Identificativo directo | Se borra al eliminar la cuenta |
| Zona horaria, rol, estado, último acceso, fechas de alta | `users` | Fila propia | Personal | Se borra |
| Consentimiento: `consent_granted_at`, `consent_version`, `is_study_participant` | `users` | Fila propia | Personal (prueba del consentimiento) | Se borra. Conservar la prueba del consentimiento es una decisión pendiente ([PENDIENTES_APROBACION](PENDIENTES_APROBACION.md)) |
| Código de participante P-xxx, `enrolled_at`, `withdrawn_at` | `users` | Fila propia | Seudónimo: enlaza con todo lo demás | Se borra. V60 impide que el código se reutilice |
| Fecha de nacimiento, sexo biológico, altura, peso actual y objetivo | `user_profiles` | FK `user_id` con CASCADE | Personal. Combinados, son casi identificativos | Se borra (cae con `users`) |
| Nivel, objetivo, lugar, días, minutos, equipamiento, ejercicios bloqueados, permisos de impacto, suelo, rodillas, colgarse y por encima de la cabeza | `user_profiles` | FK CASCADE | Personal | Se borra |
| `goal_text` y `health_notes` (texto libre) | `user_profiles` | FK CASCADE | **Alta**: texto libre; `health_notes` puede contener datos de salud | Se borra |
| Ejercicios retirados por dolor, motivo, evidencia de reactivación | `user_safety_exclusions` | FK `user_id` | **Alta**: dato de salud (dolor) | Se borra |
| Plan semanal, versión, origen (IA o reglas), `input_snapshot` y `output_snapshot` (JSONB) | `weekly_training_plans` | FK `user_id` | **Alta**: `input_snapshot` copia edad, sexo, altura, peso, `goal_text`, `health_notes` y la ejecución de la semana anterior | Se borra |
| Sesiones planificadas, omisiones y su motivo (incluido `PAIN_OR_DISCOMFORT`), minutos declarados | `planned_workouts` | FK CASCADE vía el plan | Salud en el motivo de omisión | Se borra (cae con el plan) |
| Ejercicios prescritos y su dosis | `planned_workout_exercises` | FK CASCADE vía la sesión | Personal | Se borra |
| Sesiones ejecutadas: horas, minutos activos, RPE, satisfacción | `workout_sessions` | FK `user_id` | Personal; el RPE es esfuerzo percibido | Se borra |
| Series, repeticiones, segundos y carga ejecutados, motivo de omisión | `workout_session_exercises` | FK CASCADE vía la sesión | Personal | Se borra |
| Adherencia, minutos, RPE medio, riesgo de abandono (`risk_score`, `risk_level`, `risk_factors`, `is_dropout`) | `weekly_user_metrics` | FK `user_id` | Personal; perfilado del comportamiento | Se borra |
| Intentos de adaptación: decisión, motivos, sesiones afectadas, informe | `plan_adaptation_attempts` | FK `user_id` | Personal | Se borra |
| Intervenciones aplicadas y su efecto | `user_interventions` | FK `user_id` | Personal | Se borra |
| Regeneraciones seguras (P-03), ejercicios excluidos | `safe_regeneration_attempts` | FK `user_id` | Personal; puede reflejar dolor | Se borra |
| Operaciones de generación asíncrona (estado, motivo) | `plan_generation_operations` | FK `user_id` | Bajo (técnico, ligado a la persona) | Se cancela al registrar la eliminación y se borra |
| Libro de predicciones de Replicate: id de predicción, hash de la entrada, **salida completa de la IA** | `plan_generation_predictions` | FK `user_id` | **Alta**: la salida es el plan de la persona | Se borra |
| Tokens de recuperación de contraseña (hash SHA-256) | `password_reset_tokens` | FK `user_id` | Credencial temporal | Se borra |
| Registro de eliminaciones | `account_deletions` (V60) | `user_id` solo mientras está PENDIENTE; NULL al completarse | Mínimo: id interno, origen, estado, fechas y recuento de filas. Sin correo, nombre ni código | Se conserva como prueba técnica de la eliminación. Su plazo está pendiente |

**Tablas sin datos de personas:**
- Catálogo: `exercises`, `body_parts`, `equipment_types`, `exercise_equipment_requirements`, configuración.
- `exercise_difficulty_approvals`: sus revisores son el equipo, no participantes.

**Cobertura:**
- `EliminacionDeCuentaIT.ningunaTablaSinCubrir` lee el esquema real.
- Falla si una tabla nueva tiene `user_id` o una FK hacia estos datos y el borrado no la cubre.

## 2. Fuera de PostgreSQL

| Dato | Dónde | Sensibilidad | Tratamiento propuesto |
|---|---|---|---|
| Entrada de la IA: edad, sexo, altura, peso, peso objetivo, nivel, objetivo, **`goal_text`**, **`health_notes`**, equipamiento, disponibilidad, ejecución de la semana anterior (series, repeticiones, RPE, motivos de omisión) | Replicate (`openai/gpt-5-structured`), y a través de él el proveedor del modelo | **Alta**. No se envían nombre, correo ni código, pero **no es anónimo**: el texto libre puede identificar y contiene salud | Declararlo como dato compartido con un tercero. El borrado de FitSense **no** borra lo que conserve Replicate: ver el apartado 4 de [POLITICA_ELIMINACION_PROPUESTA](POLITICA_ELIMINACION_PROPUESTA.md) |
| JWT (7 días), con el claim `email` | Dispositivo de la persona | Credencial | Deja de servir en cuanto la cuenta pasa a DELETED, porque el filtro recarga al usuario en cada petición. Propuesta para PR E: quitar `email` del claim |
| Logs de la aplicación | Railway (stdout) | Ids internos, ids de operación. Nunca contraseñas ni tokens | La eliminación solo registra el id de la eliminación y el recuento de filas. La revisión de los demás logs está en PRUEBAS_SEGURIDAD.md (PR E) |
| Copias de seguridad / historial de la base | Proveedor de PostgreSQL (Neon por defecto en `application.properties`) | Todo lo anterior | El borrado no alcanza las copias ya hechas. Su plazo depende del plan y la configuración del proveedor: **comprobarlo en la consola, no se ha supuesto ninguno** |
| Correo de recuperación (cuando exista proveedor, PR C) | Proveedor de correo | Correo electrónico y enlace temporal | Declararlo. El proveedor conserva sus registros según su propia política |

## 3. Hallazgos que esta auditoría corrige o deja anotados

| # | Hallazgo | Estado |
|---|---|---|
| H-1 | El código P-xxx se calculaba con `count(*)+1`. Tras eliminar una cuenta, el siguiente registro chocaría con un código vivo (`uq_users_participant_code`) o heredaría el de la persona eliminada | **Corregido en V60** (`participant_code_seq`) |
| H-2 | Ninguna FK hacia `users` tiene CASCADE salvo la de `user_profiles`, así que borrar `users` fallaba | Resuelto con el borrado ordenado de `EliminacionDeCuentaService` |
| H-3 | Cambiar la contraseña no invalida los JWT ya emitidos | Se corrige en PR C (`token_version`) |
| H-4 | No hay edad mínima: `birth_date` solo exige `@Past` | PR D |
| H-5 | La entrada a Replicate lleva texto libre y datos de salud | Hay que declararlo. Minimizarlo cambiaría la generación y queda fuera de esta orden |
| H-6 | El JWT incluye el correo | Propuesta en PR E |
