# Control de frecuencia semanal

**Política aprobada:**
- Principiante: **4 sesiones** como máximo por semana.
- Intermedio y avanzado: **6**.
- **Nunca 7.**

Son máximos, no obligaciones. `availableDays` son los días en que la persona puede entrenar; las sesiones programadas son las que FitSense asigna.

## Código

| Pieza | Qué hace |
|---|---|
| `PlanGenerationContext.MAX_SESSIONS_BEGINNER = 4` y `MAX_SESSIONS_OTHERS = 6` | Los topes |
| `effectiveDaysPerWeek()` | min(días del perfil, tope del nivel) |
| `suggestedSplit()` | min(efectivas, número de días disponibles) |
| **V2** (`PlanDraftValidator`) | Exige **exactamente** `effectiveDaysPerWeek()` entrenamientos en todo borrador: IA, reglas, adaptación, copia, progresión y reincorporación pasan por el validador |
| V3 / V12 | Cada fecha, en un día disponible y una sola vez |
| Perfil | Admite de 1 a 7 días (`ck_profile_days_per_week`) |

**No hay discrepancias con lo aprobado.**

## Campaña (`evidencia/real/FRECUENCIA_SEMANAL.csv`)

Se comprobaron 76 planes: los 7 iniciales y los 69 publicados en los ciclos.

| Usuario | Nivel | Días disponibles | Tope | Sesiones en todos sus planes | Resultado |
|---|---|---:|---:|---:|---|
| U01 | INTERMEDIATE | 2 | 6 | 2 | OK |
| U02 | ADVANCED | 5 | 6 | 5 | OK |
| U03 | BEGINNER | **7** | **4** | **4** | OK: nunca más de 4 |
| U04 | INTERMEDIATE | 6 | 6 | 6 | OK |
| U05 | BEGINNER | 1 | 4 | 1 | OK: conserva la única sesión las 10 semanas |
| U06 | ADVANCED | 4 | 6 | 4 (también en el plan regenerado tras S6) | OK |
| U07 | BEGINNER | 1 | 4 | — (sin plan) | — |
| U08 | INTERMEDIATE | 3 | 6 | 3 | OK |

En los 76 planes:
- ninguno tiene más sesiones que el tope;
- ninguno tiene 7;
- ninguno repite fecha ni usa un día no disponible;
- ninguna adaptación, progresión o regeneración cambió el número de sesiones.

**Prueba con 7 días** (`PerfilesLimiteIT`):
- `principianteSieteDias`: el perfil se acepta y el plan trae 4 sesiones en 4 fechas distintas.
- `intermedioSieteDias`: 6 sesiones, nunca 7.

**Prueba negativa del límite:** V2 rechaza cualquier borrador con más sesiones que `effectiveDaysPerWeek()`. La cubren las pruebas del validador y los rechazos V2 de la IA en campañas anteriores.
