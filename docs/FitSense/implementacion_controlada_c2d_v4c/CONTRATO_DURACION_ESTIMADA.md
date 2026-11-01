# Contrato de duración estimada (API) y presentación prevista en Flutter

**Estado:** implementado en el backend detrás de `fitsense.api.duracion-estimada.enabled` (por defecto `false`). **Flutter no se ha modificado.**

## 1. Contrato (retrocompatible)

Son campos **añadidos y opcionales**. Con el indicador desactivado se omiten (`@JsonInclude(NON_NULL)`) y la respuesta es idéntica a la actual.

**`GET` del plan** (`WeeklyTrainingPlanResource`):

```json
{
  "planId": 123,
  "...": "campos de siempre",
  "planningPolicy": "PLAN-C2D-0.1",
  "workouts": [
    {
      "plannedWorkoutId": 456,
      "expectedDurationMinutes": 43,
      "...": "campos de siempre",
      "durationInfo": {
        "availableMinutes": 75,
        "estimatedMinutes": 43,
        "differenceMinutes": 32,
        "policy": "PLAN-C2D-0.1",
        "reason": "ESTRUCTURA_COMPLETA",
        "message": "Tu rutina tiene una duracion estimada de 43 minutos. Dispones de hasta 75 minutos para entrenar. Incluye todos los ejercicios y series que tu plan admite con las reglas actuales."
      }
    }
  ]
}
```

| Campo | Fuente | Nota |
|---|---|---|
| `availableMinutes` | `planning_policy_trace.minutos_disponibles` (M del perfil al generar) | El perfil congelado y `sessionMinutes` **no cambian** |
| `estimatedMinutes` | `segundos_estimados` de la traza, redondeado | La misma fórmula que valida V20 y V4-C |
| `reason` | Por sesión | `ESTRUCTURA_COMPLETA`, `REDUCCION_AUTORIZADA`, `SESION_ACORTADA`, `DENTRO_DEL_TIEMPO` o `SIN_DATOS` |
| `message` | Plantilla por motivo | Ver §2 |

**Motivos**

| Código | Significado |
|---|---|
| `ESTRUCTURA_COMPLETA` | Excepción C2-D: la sesión está saturada |
| `REDUCCION_AUTORIZADA` | Exención V8C por una adaptación (por ejemplo, D8 por fatiga) |
| `SESION_ACORTADA` | Exención V8C cuando el ajuste del plan es ACORTAR (falta de tiempo) |
| `DENTRO_DEL_TIEMPO` | Ninguna diferencia que explicar |
| `SIN_DATOS` | Plan anterior a V44, sin traza. **No se inventa la disponibilidad** |

## 2. Mensajes

Todos empiezan por: «Tu rutina tiene una duración estimada de X minutos. Dispones de hasta Y minutos para entrenar.» Después, según el motivo:

| Motivo | Texto añadido |
|---|---|
| `ESTRUCTURA_COMPLETA` | «Incluye todos los ejercicios y series que tu plan admite con las reglas actuales.» |
| `REDUCCION_AUTORIZADA` | «Esta semana la sesión es más corta porque tu plan se adaptó.» |
| `SESION_ACORTADA` | «Se acortó para ajustarse al tiempo que indicaste.» |
| `DENTRO_DEL_TIEMPO` | (nada) |

**Prohibido**, y comprobado en `DuracionYAerobicaAssemblerTest`:
- decir que la sesión es «óptima» o «suficiente»;
- sugerir añadir ejercicios, series o calentamiento para llenar el tiempo;
- juzgar o prometer resultados.

## 3. Presentación prevista en Flutter (especificación, sin implementar)

1. **Tarjeta del entrenamiento:** «43 min (estimado)». Nunca mostrar «75 min» como duración de la rutina.
2. **Detalle:** el `message` del backend, sin rehacerlo en el cliente.
3. **Si `durationInfo` no viene** (indicador apagado o cliente antiguo): seguir mostrando `expectedDurationMinutes`, como hoy.
4. **`reason = SIN_DATOS`:** solo la duración estimada, sin «Dispones de…».
5. **Pruebas del cliente:** deserializar con y sin el campo, y con cada uno de los cinco motivos.

**Condición de activación de C2-D:** este contrato y su presentación en la app deben estar listos antes de activar `PLAN-C2D-0.1` fuera del laboratorio.
