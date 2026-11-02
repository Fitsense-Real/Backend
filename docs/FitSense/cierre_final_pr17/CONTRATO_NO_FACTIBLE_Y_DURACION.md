# Contrato de NO_FACTIBLE y de duración (API) y propuesta para Flutter

**Estado:**
- Backend implementado detrás de indicadores **apagados**: `fitsense.api.no-factible-estructurado.enabled=false` y `fitsense.api.duracion-estimada.enabled=false`.
- **Flutter no se ha modificado.**

## 1. NO_FACTIBLE (NOFACT-0.1)

**Cuándo:** `POST /api/v1/users/me/plan/generate` no puede generar la semana porque el validador encontró un **VEF NO_FACTIBLE**: falta el patrón principal de una sesión y el catálogo elegible no tiene ninguno.

| Indicador | Respuesta 422 |
|---|---|
| Apagado (por defecto) | La de siempre: `ApiErrorResource {timestamp, status, error, message, details}`, con el mensaje técnico del pipeline. **Sin cambios** (`NoFactibleApiApagadoIT`) |
| Encendido | `{timestamp, status, error, diagnosis}`, sin `message` técnico (`NoFactibleApiEstructuradoIT`) |
| Encendido, fallo de otro tipo | La misma respuesta que el manejador global |

**`diagnosis`:**

| Campo | Valores | Uso |
|---|---|---|
| `status` | `NO_FACTIBLE` | — |
| `reason` | `PULL_NO_DISPONIBLE`, `PUSH_NO_DISPONIBLE`, `PIERNA_NO_DISPONIBLE`, `PATRONES_NO_DISPONIBLES` | Elegir el texto o el icono en la app |
| `missingPatterns` | p. ej. `["HORIZONTAL_PULL","VERTICAL_PULL"]` | **No mostrarlo al usuario** (es técnico) |
| `equipmentRestriction` | `true` | Indica que la causa es el material declarado |
| `optionalEquipmentCodes` | Solo para `PULL_NO_DISPONIBLE`: `band`, `dumbbell`, `pull-up bar`, `suspension straps`, `wall bars`, `anchor point` (verificados en `MATRIZ_ALTERNATIVAS_48.csv`) | Ofrecer «Si ya lo tienes, añádelo». **Nunca una compra** |
| `message` | Texto para el usuario, sin términos técnicos | Mostrar tal cual |
| `version` | `NOFACT-0.1` | Trazabilidad |

**Coherencia con lo existente:**
- `GET /api/v1/users/me/plan/limitations` ya devuelve un `Limitation` de tipo `SLOT UPPER_PULL`, con `optionalEquipmentCodes = [band, wall bars, anchor point]`, para cualquier política.
- **Propuesta:** alinear esa lista con la verificada (decisión pendiente).
- `GET /api/v1/users/me/plan/adaptations` sigue informando las semanas NO_FACTIBLE del ciclo semanal (V32).

## 2. Duración (sin cambios desde la orden anterior)

`durationInfo` en cada sesión, detrás de su indicador:
- campos: `availableMinutes`, `estimatedMinutes`, `differenceMinutes`, `policy`, `reason`, `message`;
- `reason`: `ESTRUCTURA_COMPLETA`, `REDUCCION_AUTORIZADA`, `SESION_ACORTADA`, `DENTRO_DEL_TIEMPO`, `SIN_DATOS`.

**Mensaje neutral** (sin cambios): «Tu rutina tiene una duración estimada de X minutos. Dispones de hasta Y minutos para entrenar. La duración se calculó según los ejercicios disponibles y las restricciones de tu plan.»

**Sobre el código `ESTRUCTURA_COMPLETA`:**
- **Consumidores:** ninguno en el backend fuera del ensamblador y su prueba. Flutter no lo consume todavía; el repositorio de Flutter no está en esta sesión y no se ha podido revisar.
- **Recomendación:** sustituirlo por `DURACION_POR_CONTENIDO` **antes** de activar el indicador. Mientras esté apagado no hay ningún cliente que romper.
- **No se ha cambiado** (decisión pendiente): es un cambio de contrato.

## 3. Especificación para Flutter (sin implementar)

| Caso | Datos de la API | Qué mostrar | Acciones posibles (siempre a elección del usuario) |
|---|---|---|---|
| **Rutina generada normal** | `durationInfo.reason = DENTRO_DEL_TIEMPO` | Plan y duración estimada | — |
| **Duración disponible frente a estimada** | `availableMinutes`, `estimatedMinutes` | «Duración estimada: X min» y «Dispones de: Y min» como dos datos. **No** mostrar `differenceMinutes` como «te faltan» | — |
| **Rutina más corta por saturación** | `reason = ESTRUCTURA_COMPLETA` (o `DURACION_POR_CONTENIDO`) | El `message` tal cual. Sin insinuar que deba completar el tiempo | — |
| **Reducción por adaptación** | `REDUCCION_AUTORIZADA` o `SESION_ACORTADA` | El `message` tal cual | — |
| **NO_FACTIBLE por material** | 422 con `diagnosis.reason = PULL_NO_DISPONIBLE` y `equipmentRestriction = true` | El `message`. Sin plan. **No** mostrar un plan parcial | «Revisar mi equipamiento» → pantalla de perfil con los `optionalEquipmentCodes` marcados como «si ya lo tienes». **No** se cambia nada sin confirmación |
| **NO_FACTIBLE por restricciones** | Hoy llega como 422 genérico (sin `diagnosis`) o, en el ciclo, como V32 «no factible» | Mensaje genérico: «No pudimos generar tu rutina con tus restricciones actuales» | «Revisar mis restricciones» |
| **Patrones que faltan** | `missingPatterns` | **No** mostrarlos tal cual. Traducir el `reason` («ejercicios de espalda o tracción») | — |
| **Cambiar días o división** | — | **Nunca automático.** Si se ofrece, debe explicar que cambia lo que pidió y que no resuelve la falta de material (`FACTIBILIDAD_48_PERFILES.md` §2) | — |
| **SIN_DATOS** | `reason = SIN_DATOS` | No mostrar la disponibilidad | — |

**Pruebas de contrato en el backend:**
- `DuracionYAerobicaAssemblerTest`: texto neutral y palabras prohibidas.
- `DiagnosticoNoFactibleTest`: sin texto técnico; material solo cuando está verificado.
- `NoFactibleApiEstructuradoIT` y `NoFactibleApiApagadoIT`: respuesta HTTP con el indicador encendido y apagado.
