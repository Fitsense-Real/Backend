# Métrica aeróbica implementada (informativa, desactivada)

**Componente:** `planning/domain/services/MetricaAerobica`, de dominio y con pruebas.

**Exposición:** campo opcional `aerobicActivity` del plan, detrás de `fitsense.metricas.aerobica.enabled` (por defecto `false`). Desactivado, la respuesta no cambia.

## 1. Qué mide

| Campo | Cálculo | Nota |
|---|---|---|
| `programmedWorkMinutes` | Σ series × segundos de los ejercicios con patrón `LOCOMOTION_CARDIO` | **Minutos aeróbicos programados** |
| `programmedBlockMinutes` | Lo anterior más las pausas entre series | Lo que **ocupa** en la sesión. **Las pausas no se cuentan como minutos aeróbicos** |
| `executedMinutes` | — | **`null`** en esta versión: el contrato lo admite, pero leerlo exige cruzar con el contexto de ejecución, y no se ha integrado (riesgo pendiente) |
| `externalMinutes` | — | `null`: no hay registro de actividad fuera de la app |
| `intensity` | `UNKNOWN` | No hay RPE ni talk test por bloque |
| `equivalentMinutes` | Solo con intensidad conocida (moderada ×1, vigorosa ×2) | **`null` con intensidad desconocida**: nunca se supone |
| `message` | «Tu plan incluye unos X minutos de actividad aeróbica esta semana. La actividad que realizas fuera de FitSense (caminar, ir en bicicleta, hacer deporte) también cuenta.» | X = minutos de **trabajo**. Sin juicio de incumplimiento y sin promesa de pérdida de peso |

## 2. Pruebas

`DuracionYAerobicaAssemblerTest.aerobica`:
- 3×90 s y 3×120 s dan 10,5 min de trabajo y 14,5 min de bloque;
- sin intensidad no hay equivalentes;
- con intensidad vigorosa, (10 + 20) × 2 = 60;
- el aviso usa el trabajo y no contiene expresiones prohibidas.

## 3. Limitaciones

- **No se aumenta la dosis 4D de cardio.**
- La métrica no puede decir si la persona cumple la recomendación de la OMS: no conoce su actividad total.
- Falta integrar los minutos ejecutados (lectura de `session_exercises`) y un registro de minutos externos e intensidad. Ambos están anotados en `RIESGOS_PENDIENTES.md`.
