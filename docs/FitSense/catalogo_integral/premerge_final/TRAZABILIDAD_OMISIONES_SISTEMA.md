# Omisiones impuestas por el sistema y adherencia

## 1. Metodología actual (sin cambios)

Para la candidata v1.1 se mantiene todo igual:

- No cambian la fórmula de adherencia, sus pesos, sus umbrales ni el denominador.
- No se elimina ni se reclasifica ninguna sesión del historial.

**Hecho:** dos tipos de omisión impuesta por FitSense cuentan hoy como omisiones normales:

- una sesión bloqueada por seguridad que vence (SKIPPED sin motivo; V54 guarda la causa en `planned_workouts.system_block_cause`);
- un ejercicio marcado por seguridad que solo puede registrarse como omitido (P-DOLOR-PUB y causas del perfil).

Es una **limitación de la medición**, documentada en `LIMITACIONES_PARA_TESIS.md`.

## 2. ¿Se puede reconstruir la causa histórica con los registros actuales?

| Situación | Ejercicio omitido por **dolor registrado** | Ejercicio omitido por **otra causa de seguridad** (restricción del perfil, contención, catálogo) |
|---|---|---|
| La exclusión deja de estar activa (reactivación) | **Sí**: `user_safety_exclusions` no se borra; la reactivación conserva la fila con `reactivated_at`. Ventana de vigencia: `recorded_at ≤ t < coalesce(reactivated_at, ∞)` | **No**: el conflicto se calcula al leer, a partir del perfil y del catálogo de ese momento, y no se guarda |
| El usuario recibe una planificación nueva | **Sí**: la fila de `workout_session_exercises` y la exclusión se conservan; las versiones nuevas del plan no borran las anteriores | **No**, por la misma razón |
| Cambia el estado de la sesión (terminada, vencida, reemplazada) | **Sí**: el registro del ejercicio no cambia | **No** |
| Se consulta la adherencia semanas después | **Sí, con dos supuestos**:<br>1. que P-DOLOR-PUB estaba encendida en ese momento. La configuración vigente en cada instante **no se guarda**;<br>2. que la consulta usa la ventana de vigencia | **No** |

**Conclusión:**

- **Dolor:** la causa se puede reconstruir, pero depende de la configuración vigente, que no se registra.
- **Otras causas:** se pierden.
- La causa **no** queda guardada de forma duradera en la fila de la omisión.
- `OTHER` (o el motivo que elija la persona) no distingue por sí solo una omisión voluntaria de una impuesta.

## 3. Propuesta mínima (PR independiente, NO incluida en la candidata)

**Rama:** `propuesta/traza-omision-sistema`, commit `ce775b8`, sobre la candidata `12c6897`.

| Elemento | Qué hace |
|---|---|
| `V57__omision_impuesta_por_el_sistema.sql` | Añade `workout_session_exercises.system_block_cause VARCHAR(200)` NULL, con comentario. Sin datos y sin backfill |
| `WorkoutSessionExercise.anotarBloqueoDelSistema` | Guarda la causa de seguridad vigente cuando se registra el ejercicio (la misma cadena que da la API, p. ej. `dolor_registrado` o `requires_floor+dolor_registrado`) |
| `WorkoutSessionCommandServiceImpl` | La anota en el registro en vivo y en el retroactivo. NULL si el ejercicio no estaba marcado |
| `EsquemaVigenteSobreCatalogoAnterior` | V57 se registra en `SOLO_ESQUEMA` (columna nullable, sin datos), igual que V54 y V55 |
| Prueba | `RegeneracionPorColocacionRev2IT#dolorMarcadoEnSesionesAbiertas` comprueba que la omisión impuesta queda con `dolor_registrado` y las demás filas de la sesión con NULL |

**Cumple los requisitos:**

- distingue la omisión voluntaria (NULL) de la impuesta (con causa);
- conserva la causa de seguridad con trazabilidad histórica, sin depender de restricciones activas ni de la configuración;
- **no** cambia `status`, `skip_reason` ni ningún cálculo (adherencia, riesgo, adaptación);
- **no** altera ni reclasifica el historial existente: las filas anteriores quedan NULL.

**Impacto:**

| | |
|---|---|
| Esquema | 1 columna nullable |
| Contrato de la API | Ninguno; la columna no se expone |
| Flutter | Ninguno |
| Pruebas | Las ejecutadas sobre la rama pasan: `RegeneracionPorColocacionRev2IT`, `ContencionTextoBanderaIT`, `Usuario31ReproduccionIT` (V36 + `SOLO_ESQUEMA`), `RetiradaLocalizadaLimiteIT` (V50 + `SOLO_ESQUEMA`), `CatalogoCalidadIT` y `PoliticasP123IT` |
| Pendiente si se aprueba | Su propia regresión completa y su CI |
| Orden de integración | Después de la PR final de la candidata, y solo si se aprueba |

## 4. ¿Sirve la versión conservadora para evaluar la adherencia?

Conviene distinguir dos cosas:

- **Generar rutinas:** la versión v1.1 es válida para generar y adaptar rutinas (ver `REGRESION_INTEGRADA_FINAL.md`).
- **Medir la adherencia con los registros actuales:** es válida **con una salvedad declarada**. El indicador oficial cuenta como no hechas las omisiones que impuso el sistema. En una evaluación con participantes reales, la tesis debe hacer una de dos cosas:
  - **(a)** reportar el indicador oficial y declarar la limitación. Si se quieren separar las omisiones por dolor, se puede hacer en el análisis con la ventana de `user_safety_exclusions` (`../cierre_conservador/VALIDACION_DOLOR_PUBLICADO.md` §5) y declarando el supuesto de configuración;
  - **(b)** si se necesita separar **todas** las omisiones impuestas, incluidas las de otras causas, integrar antes la propuesta V57. Sin ella, esas causas no se pueden recuperar después.

**Recomendación:** si la evaluación de adherencia con personas reales va a usar FitSense, **integrar V57 antes de empezar a recoger datos**. V57 no cambia ningún cálculo, pero sin ella la información no se puede recuperar a posteriori. Para el cierre técnico del catálogo, que es lo que se pide aprobar ahora, no es un bloqueo.
