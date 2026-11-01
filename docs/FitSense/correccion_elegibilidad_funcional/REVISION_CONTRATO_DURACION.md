# Revisión del contrato de duración visible

**Cambio:** el mensaje del motivo `ESTRUCTURA_COMPLETA` ya no afirma que la rutina sea completa ni suficiente.

| | Texto |
|---|---|
| Antes | «Tu rutina tiene una duración estimada de 43 minutos. Dispones de hasta 75 minutos para entrenar. **Incluye todos los ejercicios y series que tu plan admite con las reglas actuales.**» |
| Ahora | «Tu rutina tiene una duración estimada de 43 minutos. Dispones de hasta 75 minutos para entrenar. **La duración se calculó según los ejercicios disponibles y las restricciones de tu plan.**» |

**Por qué:**
- «Incluye todos los ejercicios y series que tu plan admite» afirma una **suficiencia** que no está demostrada.
- La saturación de C2-D es **local**, no global.
- Con el catálogo vigente, «lo que tu plan admite» podía incluir ejercicios sin pertinencia funcional (`DIAGNOSTICO_SELECTOR_EJERCICIOS.md`).
- El texto nuevo solo dice **de dónde sale** la duración.

## 1. Contrato (sin cambios de forma)

`PlannedWorkoutResource.durationInfo`:
- Campos: `availableMinutes`, `estimatedMinutes`, `differenceMinutes`, `policy`, `reason` y `message`.
- Valores de `reason`:
  - `ESTRUCTURA_COMPLETA`
  - `REDUCCION_AUTORIZADA`
  - `SESION_ACORTADA`
  - `DENTRO_DEL_TIEMPO`
  - `SIN_DATOS`
- Sigue detrás de `fitsense.api.duracion-estimada.enabled=false`. Desactivado, la respuesta es idéntica (`NON_NULL`).

**Nombre del motivo:**
- Se mantiene `ESTRUCTURA_COMPLETA` como **código** para no romper el contrato especificado.
- Se recomienda que Flutter **no muestre el código**, solo `message`.
- Si se quiere un código neutral (p. ej. `DURACION_POR_CONTENIDO`), es un cambio de contrato que requiere aprobación. No se ha hecho.

## 2. Pruebas de contrato

**`DuracionYAerobicaAssemblerTest`:**
- Comprueba el **texto exacto** del mensaje de C2-D.
- Ningún mensaje de ningún motivo contiene: «optim», «suficiente», «añade», «agrega», «rellena», «incumpl», «perder», «garantiza», **«todos», «complet», «admite», «máximo»** (las cuatro últimas son nuevas).

## 3. Especificación para Flutter (sin modificar Flutter)

1. Mostrar `message` tal cual. **No** construir frases a partir de `reason`.
2. `availableMinutes` y `estimatedMinutes`: dos datos distintos («Dispones de» / «Duración estimada»). No mostrar `differenceMinutes` como un «faltan» ni como un objetivo por cumplir.
3. Con `reason = SIN_DATOS`, no mostrar disponibilidad: el backend no la inventa.
4. **No** mostrar nada que sugiera que el usuario debe completar el tiempo con más ejercicios.

**`CONTRATO_DURACION_ESTIMADA.md`** (carpeta de la implementación controlada):
- Se conserva sin cambios, como registro.
- Su texto de ejemplo para `ESTRUCTURA_COMPLETA` queda **sustituido por este**.
