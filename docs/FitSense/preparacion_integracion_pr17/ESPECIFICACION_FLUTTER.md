# Especificación para Flutter: duración estimada, NO_FACTIBLE y equipamiento

**Estado:**
- Especificación **sin implementar**. **Flutter no se ha modificado** (no autorizado; su repositorio no está en esta sesión).
- Los campos que describe solo existen en el PR #17, detrás de indicadores **apagados**:
  - `fitsense.api.duracion-estimada.enabled`
  - `fitsense.metricas.aerobica.enabled`
  - `fitsense.api.no-factible-estructurado.enabled`
- Con los indicadores apagados, la API es la de siempre: `ContratoDuracionApagadoIT` y `NoFactibleApiApagadoIT`.

**Regla general:**
- La app **nunca** cambia el perfil, el equipamiento, los días ni la división sin una acción explícita y confirmada del usuario.
- **Nunca** muestra un plan parcial.
- **Nunca** sugiere comprar nada ni presupone superficies o anclajes.

## 1. Modelos de datos (Dart, propuesta)

```dart
/// Opcional en cada sesión; null si el indicador está apagado (el campo no llega).
class DurationInfo {
  final int? availableMinutes;   // null => no mostrar la disponibilidad (SIN_DATOS)
  final int estimatedMinutes;
  final int? differenceMinutes;  // NO mostrar como "te faltan"
  final String policy;           // trazabilidad; no mostrar
  final DurationReason reason;
  final String message;          // texto listo para mostrar
}

enum DurationReason {
  dentroDelTiempo,        // DENTRO_DEL_TIEMPO
  duracionPorContenido,   // DURACION_POR_CONTENIDO (antes ESTRUCTURA_COMPLETA)
  reduccionAutorizada,    // REDUCCION_AUTORIZADA
  sesionAcortada,         // SESION_ACORTADA
  sinDatos,               // SIN_DATOS
  desconocido,            // cualquier valor futuro: tratar como sinDatos y mostrar `message`
}

/// Opcional en el plan; null si el indicador está apagado.
class AerobicActivity {
  final double programmedWorkMinutes;
  final double programmedBlockMinutes;
  final double? executedMinutes;
  final double? externalMinutes;
  final String intensity;          // DESCONOCIDA, MODERADA, VIGOROSA
  final double? equivalentMinutes; // null si la intensidad es desconocida
  final String message;            // aviso informativo; no es una meta
}

/// Cuerpo del 422 de POST /plan/generate con NOFACT-0.1 encendido.
class NoFactibleDiagnosis {
  final String status;                       // "NO_FACTIBLE"
  final NoFactibleReason reason;
  final List<String> missingPatterns;        // técnico: NO mostrar
  final bool equipmentRestriction;
  final List<String> optionalEquipmentCodes; // solo "si ya lo tienes"
  final String message;                      // texto listo para mostrar
  final String version;                      // "NOFACT-0.1"
}

enum NoFactibleReason {
  pullNoDisponible, pushNoDisponible, piernaNoDisponible, patronesNoDisponibles, desconocido,
}
```

**Deserialización:**
- Todos los campos nuevos son **opcionales**. La app debe funcionar si no llegan, como hoy.
- Un `reason` desconocido no puede romper la pantalla: se muestra `message`.

## 2. Casos A–E

### A. Plan generado normalmente

**JSON** (fragmento de `GET /api/v1/users/me/plan/current` con el indicador encendido):

```json
{
  "planId": 101, "status": "ACTIVE", "planningPolicy": "PLAN-HIST-1",
  "workouts": [{
    "plannedWorkoutId": 9001, "scheduledDate": "2026-11-02", "focusCode": "FULL_BODY",
    "expectedDurationMinutes": 52,
    "durationInfo": {
      "availableMinutes": 60, "estimatedMinutes": 52, "differenceMinutes": 8,
      "policy": "PLAN-HIST-1", "reason": "DENTRO_DEL_TIEMPO",
      "message": "Tu rutina tiene una duración estimada de 52 minutos. Dispones de hasta 60 minutos para entrenar."
    }
  }]
}
```

**Pantalla:**
- Dos datos separados: «Duración estimada: 52 min» y «Tienes: 60 min».
- **No** mostrar `differenceMinutes` como «te faltan 8 min».
- **Estado de la interfaz:** `PlanListo`.

### B. Plan más corto que el tiempo disponible

**Variantes de `reason`:**
- `DURACION_POR_CONTENIDO`: la sesión es más corta porque el contenido disponible y las restricciones dan esa duración (C2-D).
- `REDUCCION_AUTORIZADA`: la semana se adaptó y reduce el trabajo.
- `SESION_ACORTADA`: la persona dijo que tenía menos tiempo.

```json
"durationInfo": {
  "availableMinutes": 75, "estimatedMinutes": 43, "differenceMinutes": 32,
  "policy": "PLAN-C2D-0.1", "reason": "DURACION_POR_CONTENIDO",
  "message": "Tu rutina tiene una duración estimada de 43 minutos. Dispones de hasta 75 minutos para entrenar. La duración se calculó según los ejercicios disponibles y las restricciones de tu plan."
}
```

**Pantalla:**
- `message` tal cual.
- **No** sugerir «añade ejercicios para completar el tiempo».
- **No** usar colores de aviso ni de error: es información.
- **Estado de la interfaz:** `PlanListo` con la nota de duración.

### C. NO_FACTIBLE: acción «revisar mi equipamiento»

`POST /api/v1/users/me/plan/generate` responde **422** con NOFACT-0.1 encendido:

```json
{
  "timestamp": "2026-11-02T09:00:00Z", "status": 422, "error": "Unprocessable Entity",
  "diagnosis": {
    "status": "NO_FACTIBLE", "reason": "PULL_NO_DISPONIBLE",
    "missingPatterns": ["HORIZONTAL_PULL", "VERTICAL_PULL"],
    "equipmentRestriction": true,
    "optionalEquipmentCodes": ["band", "dumbbell", "pull-up bar", "suspension straps", "wall bars", "anchor point"],
    "message": "Con el equipamiento que indicaste no encontramos ejercicios de tracción (remos o dominadas) para las sesiones de espalda de esta semana, así que no generamos una rutina incompleta. Si ya tienes una banda elástica, mancuernas, una barra de dominadas, correas de suspensión, una espaldera o un punto de anclaje firme, puedes añadirlo en tu perfil. No necesitas comprar nada.",
    "version": "NOFACT-0.1"
  }
}
```

**Pantalla:**
- `diagnosis.message`, sin plan y sin semana parcial.
- **Botón:** «Revisar mi equipamiento» → pantalla de equipamiento del perfil.
- **Estado de la interfaz:** `NoFactible(equipo)`.

**Con el indicador apagado** llega el 422 de siempre (`ApiErrorResource` con `message` técnico):
- mostrar un texto genérico propio («No pudimos generar tu rutina con tu equipamiento actual»);
- **no** mostrar el `message` técnico.

### D. Alternativas de equipamiento: solo las verificadas

- La pantalla de equipamiento destaca **solo** los códigos de `optionalEquipmentCodes` (o la lista unificada del §4, si se aprueba), con el texto «Marca solo lo que ya tienes».
- **Sin** enlaces de compra.
- **Sin** preseleccionar nada.
- **Sin** inferir superficies («una puerta», «un árbol») ni que se pueda colgar.
- **Punto de anclaje** (`anchor point`):
  - mostrarlo **diferenciado**, con la nota «permite un único ejercicio de tracción (remo con toalla) y requiere un anclaje firme y seguro»;
  - pedir una confirmación aparte de seguridad.
  - La suficiencia de un único ejercicio de tracción está **pendiente de revisión profesional** (G3).
- **Tras marcar algo:**
  1. guardar el perfil (endpoint actual);
  2. volver a llamar a `POST /plan/generate`;
  3. si vuelve a ser NO_FACTIBLE, mostrar el caso C de nuevo, sin bucles automáticos.
- **Estado de la interfaz:** `RevisandoEquipo`.

### E. Restricciones por dolor: sin planes parciales

**Ejercicios excluidos por dolor (P-02):**
- el plan llega normalmente, sin esos ejercicios;
- la app no tiene que hacer nada especial;
- **no** debe ofrecer reincorporarlos automáticamente. La reactivación es explícita, por el flujo existente de reactivación.

**Si las restricciones dejan sin opciones una sesión obligatoria:**
- la generación falla: hoy, 422 genérico sin `diagnosis`;
- en el ciclo semanal, V32 «no factible», visible en `GET /plan/adaptations`;
- **pantalla:** «No pudimos generar tu rutina con tus restricciones actuales»;
- **botón:** «Revisar mis restricciones».

**Nunca:**
- mostrar una semana a medias;
- relajar las restricciones desde la app.

**Estado de la interfaz:** `NoFactible(restricciones)`.

## 3. Estados de la interfaz

| Estado | Entrada | Salida |
|---|---|---|
| `Cargando` | Abrir el plan o generar | `PlanListo`, `NoFactible(...)`, `Error` |
| `PlanListo` | 200/201 con plan | — |
| `NoFactible(equipo)` | 422 con `diagnosis.equipmentRestriction = true` | `RevisandoEquipo` (por acción del usuario) |
| `NoFactible(restricciones)` | 422 sin `diagnosis`, o V32 no factible | Revisión de restricciones (por acción del usuario) |
| `RevisandoEquipo` | Botón del caso C | `Cargando` tras guardar el perfil |
| `Error` | 5xx o red | Reintento manual |

## 4. Pruebas para Flutter (a implementar cuando se autorice)

| # | Caso | Esperado |
|---|---|---|
| F1 | Plan sin `durationInfo` ni `planningPolicy` (indicador apagado) | Se ve como hoy; sin errores de deserialización |
| F2 | `DENTRO_DEL_TIEMPO` | Dos datos (estimada y disponible); sin «te faltan» |
| F3 | `DURACION_POR_CONTENIDO` | `message` tal cual; sin sugerencias de completar el tiempo |
| F4 | `REDUCCION_AUTORIZADA` / `SESION_ACORTADA` | `message` tal cual |
| F5 | `SIN_DATOS` | Sin disponibilidad; solo la duración estimada |
| F6 | `reason` desconocido | Se muestra `message`; no falla |
| F7 | 422 con `PULL_NO_DISPONIBLE` | Mensaje, sin plan, botón «Revisar mi equipamiento»; `missingPatterns` no visible |
| F8 | Pantalla de equipamiento desde F7 | Solo los códigos verificados, ninguno preseleccionado, sin compras; `anchor point` diferenciado con confirmación |
| F9 | Volver a generar tras marcar equipamiento y seguir sin tracción | El caso C de nuevo; sin bucle |
| F10 | 422 sin `diagnosis` | Texto genérico; no el `message` técnico |
| F11 | Plan con ejercicios excluidos por dolor | Plan normal; sin reincorporación automática |
| F12 | `aerobicActivity` presente | Informativa; sin metas ni «te falta» |

## 5. Lista de equipamiento (propuesta unificada, pendiente de aprobación)

**Hoy hay dos listas distintas en el backend:**

| Fuente | Lista |
|---|---|
| `GET /plan/limitations` (`PlanLimitationsService.TRACCION_EN_CASA`, ya en `generation`) | `band`, `wall bars`, `anchor point` |
| NOFACT-0.1 (`DiagnosticoNoFactible.MATERIAL_TRACCION_VERIFICADO`, PR #17) | `band`, `dumbbell`, `pull-up bar`, `suspension straps`, `wall bars`, `anchor point` |

**Propuesta: una única lista ordenada por niveles**, con lo que de verdad hace elegible el perfil. Medición: `MATRIZ_ALTERNATIVAS_48.csv`, los 48 perfiles de peso corporal sin tracción, añadiendo **un** material cada vez.

| Nivel | Material | Tracciones elegibles | Patrones | Planes válidos | Revisión profesional |
|---|---|---|---|---|---|
| 1 | `band` (banda elástica) | 9 | Horizontal y vertical | 48/48 | 44 recomendadas; 4 con una sesión bajo el 70 % |
| 1 | `dumbbell` (mancuernas) | 5–6 | Horizontal | 48/48 | 36 recomendadas; 12 con una sesión bajo el 70 % |
| 2 | `pull-up bar` (barra de dominadas) | 11–26 | Horizontal y vertical | 48/48 | 28 recomendadas; 20 con una sesión bajo el 70 % |
| 2 | `wall bars` (espaldera) | 8 | Horizontal | 48/48 | 28 / 20 |
| 2 | `suspension straps` (correas de suspensión) | 3 | Horizontal | 48/48 | 28 / 20 |
| 3 (diferenciado) | `anchor point` (punto de anclaje firme) | **1** (remo con toalla) | Horizontal | 48/48 | **48/48 a revisar**: un único ejercicio de tracción para toda la semana (G3) |

**Criterios de los niveles:**
- **Nivel 1:** más tracciones y menos sesiones cortas.
- **Nivel 2:** material fijo, que puede implicar colgarse o una instalación.
- **Nivel 3:** un solo ejercicio, con requisito de seguridad.

**Reglas de la propuesta:**
1. **Una sola constante en el backend**, usada por `/limitations` y por NOFACT-0.1, con el nivel de cada código. Es un cambio de contrato en `/limitations` (hoy 3 códigos), por eso **no se ha aplicado**.
2. **No suponer que se pueda colgar:**
   - `pull-up bar` y `wall bars` solo se ofrecen si el perfil no tiene restricciones de hombro o de agarre que excluyan la suspensión;
   - la lista debe calcularse **con el mismo filtro de seguridad del perfil** que usa la generación (`prepare`, como ya hace `/limitations`), simulando cada código: se ofrece solo si añade al menos una tracción **elegible para esa persona**. No debe ser una lista fija.
   - **No se ha medido** con perfiles con restricciones: los 48 perfiles no las tienen.
3. **El punto de anclaje:**
   - siempre aparte, con la nota de ejercicio único y la confirmación de anclaje firme;
   - nunca como primera opción.
4. **Nunca** como compra: «si ya lo tienes».
