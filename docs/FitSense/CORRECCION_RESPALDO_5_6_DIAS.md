# Corrección del respaldo para 5 y 6 días

Rama `respaldo-5-6-dias`, a partir de `generation`. Es la recomendación 1 de `EVALUACION_GENERACION_4_6_DIAS.md`, con la opción A: corregir el generador, no limitar las sesiones.

**Lo que no se tocó:**
- V15, V20 y V22;
- el catálogo, la política de frecuencia y las migraciones;
- la IA, la adaptación y la progresión;
- Neon. Tampoco hubo llamadas a Replicate.

## Qué fallaba y qué se cambió

En `CapabilityWeekBuilder`, el generador por capacidades de INCREASE_STRENGTH, LOSE_WEIGHT y GENERAL_FITNESS, había dos defectos que solo aparecen en las divisiones de 5–6 días (empuje / tirón / piernas):

1. **Sesiones con un solo grupo** (V15 exige 2 cuando el conjunto elegible los tiene). La selección por patrón de movimiento llenaba las 2–3 colocaciones con el mismo grupo:
   - tirón = remo + remo + bisagra, todo `back`;
   - piernas = sentadilla + bisagra, todo `upper legs`.

   **Corrección:** un paso nuevo, `asegurarGrupos`, que **solo actúa si la sesión incumple** ese mínimo. Cambia una colocación de un grupo repetido por un ejercicio del grupo que falta. Para entrar, ese ejercicio tiene que:
   - ser trabajo resistido **prescribible para el objetivo y el nivel**, con dosis de sus capacidades. Si no hay ninguno, no se fuerza nada;
   - pertenecer al enfoque y respetar V22 (bíceps en tirón);
   - no repetir el día anterior ni lo que la sesión ya tiene;
   - dejar la sesión bajo el techo de duración, con la regla de la mitad, y sin bajar del mínimo de V20.

   Se cambian antes las colocaciones que no cubren un patrón de fuerza.

2. **Un estiramiento de tríceps (1322, 1119) en una sesión de tirón** (V22). Los complementos de movilidad se elegían sin el filtro de V22.

   **Corrección:** si la elección viola V22, se vuelve a elegir entre los admitidos. Solo en ese caso, para que una sesión que ya era válida salga idéntica.

Diff: `CapabilityWeekBuilder` (~120 líneas, la mayor parte comentario y el método nuevo). Nada más de producción.

## 1. Los 12 perfiles que se quedaban sin plan

`RespaldoCincoSeisDiasIT` prueba el generador con el validador de producción. Los casos de GENERAL_FITNESS se repiten con **4 semillas** (la semilla depende de usuario y semana).

| Perfil | Antes | Después |
|---|---|---|
| GENERAL_FITNESS INTERMEDIATE 5d casa | sin plan (tirón solo `back`) | **válido** |
| GENERAL_FITNESS INTERMEDIATE 6d casa | sin plan | **válido** |
| GENERAL_FITNESS ADVANCED 5d casa | sin plan | **válido** |
| GENERAL_FITNESS ADVANCED 6d casa | sin plan (además V22) | **válido** |
| LOSE_WEIGHT INTERMEDIATE/ADVANCED, 5/6d, gimnasio/casa (8) | sin plan (tirón y piernas) | **sigue sin plan**: el tirón ya sale bien; queda solo piernas, **demostrado imposible** con las reglas vigentes |

Con el código anterior, la misma prueba falla en 9 de sus 14 casos.

### Por qué los 8 de LOSE_WEIGHT no tienen solución sin cambiar una regla

La prueba fija las dos mitades de la contradicción:
- **V15** cuenta `lower legs` como grupo disponible (hay de 15 a 38 ejercicios de `lower legs` elegibles según nivel y lugar) y exige dos grupos en piernas.
- **4A v6 / 4D:** en LOSE_WEIGHT la única capacidad resistida es la fuerza, cuya dosis solo existe para ejercicios estructurales. Todo `lower legs` es RESISTIDO (flexión plantar) y su lista de capacidades admisibles para LOSE_WEIGHT está **vacía**, en los 4 niveles y lugares. Un gemelo en LOSE_WEIGHT no tiene dosis válida, y V24/V25 lo rechazarían.

No se fuerza ni se oculta. La salida requiere una decisión, y **ninguna está dentro de lo autorizado**:

| Opción | Qué cambia | Efecto |
|---|---|---|
| **a** | V15 cuenta como disponibles solo los grupos **prescribibles** para el objetivo | Piernas pasaría solo con muslo. Toca V15 |
| **b** | Admitir resistencia muscular o hipertrofia para ejercicios resistidos en LOSE_WEIGHT | Toca la política de capacidades (4A/4D) |
| **c** | Una división sin piernas aisladas para LOSE_WEIGHT de 5–6 días (p. ej. superior / inferior / cuerpo completo) | Toca `WeeklySplitPlanner`, que también recibe la IA |
| **d** | Tope provisional de 4 sesiones para LOSE_WEIGHT intermedio y avanzado | Es la opción B, que se descartó |

Recomendación: **a** es la más coherente con el objetivo («no forzar»): no tiene sentido exigir un grupo que el objetivo no puede prescribir.

## 2. Los 72 perfiles del barrido (4–6 días, 45 min)

Evidencia en `evidencia/frecuencia/correccion_respaldo/{antes,despues}/`.

| | Antes | Después |
|---|---|---|
| Planes válidos | 60 | **64** |
| Sin plan | 12 | **8** (solo LOSE_WEIGHT, solo piernas) |
| Sesiones | 270 | 292 (+22, las de los 4 planes recuperados) |
| Sesiones de planes antes válidos que cambian | — | **0** |
| Días programados según la política actual (BEGINNER ≤ 4) | sí | sí |
| Cobertura de capacidades de los válidos | 60 `COMPLETO` | 64 `COMPLETO` |
| Duración (% de lo configurado) | 67,8–82,3 | 67,8–82,3 |

**Robustez:** 288 generaciones, 4 semillas por perfil.

| | Antes | Después |
|---|---|---|
| Semanas sin plan | 52 | **32** (las 8 de LOSE_WEIGHT × 4, todas por piernas) |
| GENERAL_FITNESS sin plan | **20 de 24**, incluidos 4 de gimnasio que el barrido de una semilla no veía | **0** |

### Las 22 sesiones nuevas (GENERAL_FITNESS, casa)

- **Empuje:** pecho, hombro y tríceps.
- **Tirón:** 2 de espalda y 1 de bíceps.
- **Piernas:** 2–3 de muslo y 1 de gemelo.
- **Superior / inferior:** 3–4 grupos.
- Todas con 3–4 ejercicios de trabajo, 1 cardio y 1 movilidad, y dosis de las capacidades del objetivo.
- Duración del 68,6 % al 79,2 % de lo configurado.

**Señales que siguen abiertas, ajenas a esta corrección:**
- 11 sesiones quedan por debajo del 70 % exacto: es el defecto de V20, que se corregirá aparte.
- Stalder press (1267) y superman push-up (1296) entran en empuje: es el problema del catálogo.
- Algunos tirones abren con una bisagra catalogada como `back` (p. ej. 776), porque el selector elige por patrón de 4C. Funcionalmente es discutible. Corresponde a la revisión del catálogo y de V15, no a esta corrección.

## 3. Validadores y regresión

| Prueba | Resultado |
|---|---|
| `RespaldoCincoSeisDiasIT` | 14 de 14 (6 perfiles de GENERAL_FITNESS × 4 semillas, más los 8 de LOSE_WEIGHT con la imposibilidad demostrada) |
| `GeneracionReferenciaIT` | **72 de 72 idénticas**. Además, en el barrido, 0 de 216 sesiones de referencia cambian |
| Regresión completa | Ver la sección de regresión al final del documento |

V15, V20, V22, V24 y V25 son los de producción, sin cambios: la corrección está en el generador.
