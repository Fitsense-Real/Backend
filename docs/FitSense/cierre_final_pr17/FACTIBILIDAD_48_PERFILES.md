# Factibilidad de los 48 perfiles sin tracción

**Perfiles:** los 48 que C2-D validaba y C2-D-EF rechaza por VEF.
- 3 objetivos (GAIN_MUSCLE, INCREASE_STRENGTH, GENERAL_FITNESS) × intermedio y avanzado × 6 y 7 días × 30, 45, 60 y 75 minutos.
- **Solo peso corporal.**
- El planificador asigna PUSH/PULL/LEGS a partir de 6 días: el usuario elige los días, no la división.
- Los ciclos L4 (INCREASE_STRENGTH, intermedio, 6 días, 75 min) y L8 (GAIN_MUSCLE, avanzado, 6 días, 75 min) son dos de estos perfiles en el ciclo real.

**Fuente:** `MATRIZ_ALTERNATIVAS_48.csv`, del laboratorio `CierrePR17LabIT.alternativas48`. Generador y validador reales con `PLAN-C2D-EF-0.1`, PostgreSQL efímero, sin IA.

**Cada alternativa usa un perfil nuevo** con los mismos datos y solo el cambio evaluado. Su `user_id` y su semilla son distintos, así que la composición no es comparable ejercicio a ejercicio con el plan original.

## 1. Alternativa A: material adicional declarado (uno cada vez)

**No se supone que la persona lo tenga.** Es una opción para quien **ya** lo tiene.

| Material (código) | Tracciones elegibles | Patrones | Planes válidos | Colocaciones de tracción por semana (mediana) | % de M de la sesión más corta (mediana) | Sesiones bajo el 70 % (publicadas por C2-D) | Series por semana (mediana) |
|---|---|---|---|---|---|---|---|
| Banda elástica (`band`) | 9 | HORIZONTAL_PULL y VERTICAL_PULL | **48/48** | 9 | 71,0 | 4 | 109 |
| Mancuernas (`dumbbell`) | 5–6 | HORIZONTAL_PULL | **48/48** | 6 | 70,5 | 12 | 107 |
| Barra de dominadas (`pull-up bar`) | 11–26 | HORIZONTAL_PULL y VERTICAL_PULL | **48/48** | 10 | 70,5 | 20 | 96,5 |
| Correas de suspensión (`suspension straps`) | 3 | HORIZONTAL_PULL | **48/48** | 6 | 70,2 | 20 | 93,5 |
| Espaldera (`wall bars`) | 8 | HORIZONTAL_PULL | **48/48** | 9 | 71,2 | 20 | 98,5 |
| Punto de anclaje firme (`anchor point`) | **1** (1072, remo con toalla) | HORIZONTAL_PULL | **48/48** | 2 | **55,2** | 28 | 82 |

**En todos los casos:**
- validador real sin ningún problema;
- V4-C dentro del 115 % (288 de 288);
- la tracción entra como ancla del PULL con la dosis base 4D (p. ej. 3×10).

El detalle por perfil está en la matriz: ejercicios elegidos, segundos, porcentaje de M y series.

**Matices que requieren revisión profesional:**
- **Punto de anclaje:** todo el trabajo de tracción de la semana depende de **un solo ejercicio**, repetido en los dos PULL. Las sesiones son más cortas (mediana del 55 % de M; 28 de 48 perfiles con alguna sesión bajo el 70 %, publicadas por C2-D).
  - Es factible técnicamente; su suficiencia no está demostrada.
  - Además, su seguridad depende de que el anclaje sea firme: el catálogo (V39) lo describe y exige declararlo explícitamente.
- **Barra de dominadas:** requiere `allowHangingExercises`. En el laboratorio los perfiles no tienen restricciones; con esa restricción, las tracciones colgadas no serían elegibles.
- **No se ha considerado** ninguna mesa, puerta sin anclaje ni objeto doméstico.

## 2. Alternativa B: otra distribución semanal

| Cambio | División resultante | Planes «válidos» | Tracciones elegibles | Colocaciones de tracción | Veredicto |
|---|---|---|---|---|---|
| 4 días | UPPER/LOWER | 48/48 | **0** | **0** | **No aceptable** |
| 3 días | FULL_BODY | 48/48 | **0** | **0** | **No aceptable** |

**Pasan el validador porque:**
- EF solo exige ancla de tracción en las sesiones PULL;
- V23 no exige UPPER_PULL cuando el catálogo elegible no lo tiene.

**La carencia sigue intacta:** no hay ningún ejercicio de tracción en la semana. Presentarlo como solución **ocultaría la falta de equipamiento**, que es justo lo que la orden prohíbe.

Además, la división no se elige: sale del número de días. Proponer menos días cambia lo que el usuario pidió, y **nunca se haría en silencio**.

## 3. Alternativa C: sin cambios (NO_FACTIBLE)

- Con el material declarado (solo peso corporal), los 48 perfiles quedan **NO_FACTIBLE**.
- **Tipo de causa: falta de equipamiento.** No es un catálogo vacío: el catálogo tiene tracciones con material (V39), y ninguna sin material.
- No interviene ninguna restricción física: los perfiles no tienen restricciones.
- La frecuencia es factible con material. Sin él, la división PUSH/PULL/LEGS no tiene contenido para PULL.

| Causa | Perfiles |
|---|---|
| Falta de equipamiento | **48** |
| Catálogo insuficiente (ninguna tracción en el catálogo para ningún material) | 0 |
| Restricciones físicas | 0 (no probadas en esta orden; ver §5) |
| Frecuencia o distribución no factible | 0 por sí sola |
| Combinación | 0 |

## 4. Respuesta del backend (propuesta implementada y apagada)

**`DiagnosticoNoFactible` (NOFACT-0.1)** y `NoFactibleExceptionHandler`, detrás de `fitsense.api.no-factible-estructurado.enabled=false`.
- **Apagado:** el *bean* no existe y la respuesta es la de siempre.
- **Encendido**, para un VEF NO_FACTIBLE de PULL, devuelve 422 con:

```json
{
  "status": 422,
  "error": "Unprocessable Entity",
  "diagnosis": {
    "status": "NO_FACTIBLE",
    "reason": "PULL_NO_DISPONIBLE",
    "missingPatterns": ["HORIZONTAL_PULL", "VERTICAL_PULL"],
    "equipmentRestriction": true,
    "optionalEquipmentCodes": ["band", "dumbbell", "pull-up bar", "suspension straps", "wall bars", "anchor point"],
    "message": "Con el equipamiento que indicaste no encontramos ejercicios de tracción (remos o dominadas) para las sesiones de espalda de esta semana, así que no generamos una rutina incompleta. Si ya tienes una banda elástica, mancuernas, una barra de dominadas, correas de suspensión, una espaldera o un punto de anclaje firme, puedes añadirlo en tu perfil. No necesitas comprar nada.",
    "version": "NOFACT-0.1"
  }
}
```

- **Sin texto técnico:** ni códigos de regla ni patrones en el mensaje (`DiagnosticoNoFactibleTest`).
- **El material sugerido es el que el laboratorio verificó.** Con otros patrones que falten, no se sugiere material.
- **No cambia el equipamiento ni la división.** Lo decide la persona en su perfil.
- El contrato completo, junto con Flutter, está en `CONTRATO_NO_FACTIBLE_Y_DURACION.md`.

**Coherencia con lo que ya existe:**
- `GET /users/me/plan/limitations` ya informa «Ejercicios de tracción» cuando el conjunto elegible no tiene UPPER_PULL.
- Ahí sugiere `band`, `wall bars` y `anchor point`.
- Alinear las dos listas es una decisión pendiente (`DECISIONES_PENDIENTES.md`).

## 5. Lo que no se ha hecho

- **Añadir ejercicios** (p. ej. una tracción sin material): prohibido sin validación.
- **Reetiquetar movimientos** como tracción.
- **Probar restricciones físicas** combinadas con material: p. ej. `allowHangingExercises=false` con barra. Queda como caso para la revisión del contrato.
