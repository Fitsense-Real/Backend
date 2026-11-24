# 04 · Dependencias de Mobile (especificación; no se modificó `Fitsense-Real/MobileApp`)

Hallazgos de la lectura de `MobileApp`:
- rama `generation` en `a7516b2`;
- rama `main` en `3a4e26d`.

El backend no cambia por ellos.

## D-M1 · El cumplimiento de los ejercicios por tiempo con varias series (prioridad ALTA)

**Defecto:**
- **Dónde:** `lib/features/workouts/presentation/exercise_result_dialog.dart`, función `prescribedResult`, usada en «Lo completé».
- **Qué envía:** `'actualDurationSeconds': exercise['plannedDurationSeconds'] ?? 0`.
  - `plannedDurationSeconds` es la duración de **una** serie.
- **Qué espera el backend:** el **total** realizado (`PlannedExerciseTarget.plannedDurationTotal` = series × duración, el denominador del cumplimiento, 17.1).

**Efecto:**
- Un ejercicio de 3 × 120 s marcado «Lo completé» envía 120 y queda en **33 % / PARTIAL**.
- La adherencia semanal se infravalora para todo DURATION con más de una serie. Es lo habitual: el cardio de LOSE_WEIGHT y los isométricos, que son 2–3 series.
- Esa adherencia decide las adaptaciones: puede ordenar adaptaciones por baja adherencia que no corresponden.

**Comprobado en el backend:** `DuracionSinSeriesHttpIT`, paso 4. Enviar `d` cuenta `1/N` y enviar `N × d` completa.

**Cambio en Mobile:**

```dart
// prescribedResult
'actualDurationSeconds': (exercise['plannedSets'] as num? ?? 1) * (exercise['plannedDurationSeconds'] as num? ?? 0),
```

- **Etiqueta del campo:** «Duración realizada (segundos)» pasa a «Duración total realizada (todas las series, segundos)», igual que el campo de repeticiones.
- **Sin series** (`plannedSets == null`): el factor es 1, que ya funciona bien (O04).
- **Pruebas sugeridas en Mobile:**
  - 3 × 120 → envía 360;
  - sin series, 120 → envía 120;
  - SETS_REPS sin cambios.

## D-M2 · Publicar desde el cliente asíncrono de generación (prioridad MEDIA)

- **Rama `generation`:** usa la API asíncrona (`/users/me/plan/generation`) con la pantalla de 30 s y el estado «pendiente». Es **compatible** con generaciones de más de 30 s (`03` §4).
- **Rama `main`:** usa `POST /users/me/plan/generate` síncrono con un límite de 5 min, con un margen de 7 s sobre el máximo medido con gpt-5-mini `medium` (`03` §5).
- **Requisito:** la versión que se publique debe incluir el cliente asíncrono. No hace falta ningún cambio en el backend.

## D-M3 · Mostrar un DURATION sin series (informativa; sin cambio necesario)

- `exercisePrescription` ya omite las series nulas y muestra «Por tiempo · sin repeticiones asignadas · 120 s».
- **Sugerencia opcional:** «1 bloque continuo de 120 s», para que no parezca que faltan datos.
