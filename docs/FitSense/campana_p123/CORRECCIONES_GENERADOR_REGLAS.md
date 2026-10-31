# Correcciones del generador de reglas (investigación 30–120)

Solo se corrigen **defectos demostrados**. Para cada uno se cumplen tres condiciones:
- existe una semana que pasa V1–V25 y el motor no la construía (testigo en `MATRIZ_FACTIBILIDAD_30_120.csv`);
- hay una prueba de regresión que falla sin la corrección, verificado con el mismo `user_id`, es decir, la misma semilla, y la misma semana;
- la corrección no cambia ninguna política.

Archivos tocados:
- `planning/infrastructure/generation/rules/CapabilityWeekBuilder.java`: INCREASE_STRENGTH, LOSE_WEIGHT y GENERAL_FITNESS.
- `planning/infrastructure/generation/rules/RuleBasedTrainingPlanGenerator.java`: GAIN_MUSCLE, 4D.

## 1. Defectos y correcciones

### D-1 · El día lleno no cambiaba lo corto por lo largo (capacidades)

**Qué pasaba.**
1. El constructor llenaba el día hasta el tope de 8 colocaciones (V24) y subía series hasta 3.
2. Si así no llegaba al piso de V20, se rendía.
3. Nunca cambiaba una colocación corta por otra admisible más larga.

**Ejemplo (F0039, INCREASE_STRENGTH principiante, 75 min).**
- 4 fuerzas 3x8/120 y 4 hipertrofias 3x10/90 suman 3.048 s; el piso es 3.150 s.
- Cambiando 3 hipertrofias por fuerzas 3x8/120 se llega a 3.159 s y la semana es válida.

**Corrección: paso 2b, «sustitución por duración».** Solo actúa si el día sigue bajo el piso después de subir series. Mientras falte:
1. Cambia la colocación más corta que pueda salir por la más larga que pueda entrar.
2. Después sube series, solo lo que falte (`ampliarSeries`).

| Puede salir solo si… | Regla que protege |
|---|---|
| su capacidad sigue en el día o ya está en la semana; en LOSE_WEIGHT, el cardio sigue en el día | V25 |
| no deja sin cubrir un slot de 4C que la semana aún necesitaba (solo en los objetivos con slots resistidos; LOSE_WEIGHT no, 4A v6) | V23 / V25-IS |

| Puede entrar solo si… | Regla que protege |
|---|---|
| es un ejercicio verificado, con la **dosis aprobada** de una capacidad admisible (`CapabilityPrescriptionPolicy`) | V24 |
| no está en el día ni en el día anterior pegado; en LOSE_WEIGHT deja cardio para el día siguiente pegado | V18 |
| es del enfoque, con bíceps en PULL y tríceps en PUSH | V15 / V22 |
| no rompe la mitad ni baja de los grupos exigidos | V15 |
| la sesión no pasa del techo | V4 |

Nunca alarga descansos y nunca añade colocaciones: solo sustituye, así que se queda dentro del tope de V24. Es determinista: sin azar, por duración y por id.

### D-2 · Cada día solo veía lo ya construido (capacidades)

**Qué pasaba.** El lunes no sabía que el miércoles volvería a tener resistencia muscular. Por eso no la cambiaba por un cardio, aunque la semana la conservara.

**Ejemplo (F0063, GENERAL_FITNESS intermedio, 75 min).** El miércoles tenía 2.841 s. El testigo cambia allí una ME 3x18/45 por un cardio 3×120 s, y la movilidad por una hipertrofia, porque la movilidad ya estaba el lunes.

**Corrección: paso 2c, repaso semanal.** Con la semana ya armada, se repasa cada día que siga bajo el piso con las reglas de D-1, pero mirando la semana completa:
- la capacidad que sale sigue en **otro** día (V25);
- el slot que sale sigue cubierto en la semana (V23);
- no entra nada de los días pegados de antes **ni de después** (V18), ni de sus zonas en recuperación.

### D-3 · La fuerza de los cuatro slots quedaba concentrada (capacidades)

**Qué pasaba.** En GENERAL_FITNESS con 90 min, la fuerza de los cuatro slots podía quedar en un solo día. Ese día no llegaba al piso, y los otros llevaban esos patrones como hipertrofia.

**Ejemplo (F0064).** El lunes tenía 3.438 s frente a un piso de 3.780. El testigo reparte la fuerza 2+1+1 entre los días.

**Corrección: dentro del repaso semanal, dos movimientos.**
1. **`moverFuerzaDeSlot`.** Si la fuerza de un slot solo está en el día corto, otro día que ya tiene ese patrón con otra capacidad pasa esa colocación a **dosis de fuerza** (hipertrofia 3x10/90 → fuerza 3x6/150, que dura más).
   - Es la misma pieza con la dosis aprobada de otra capacidad admisible.
   - Solo se aplica si la capacidad que deja sigue en la semana (V25), el otro día no pasa del techo y no queda corto.
2. **`reubicarUnicas`.** Si el día corto lleva la única colocación de una capacidad en la semana (movilidad, ME), se pone esa capacidad en otro día que tenga holgura, sustituyendo allí algo que sí puede salir.
   - Se exige el piso y el techo de ese día, V15, V18 y los slots.

Después, la sustitución por duración del día corto puede cambiar lo suyo.

### D-4 · El recorte de 4D quitaba el segundo grupo del día (GAIN_MUSCLE)

**Qué pasaba.** `recortarAlPresupuesto`, en su ronda 1, quitaba cualquier relleno no estructural, aunque fuera el único de su grupo.

**Ejemplo (F0313, GAIN_MUSCLE intermedio, 6 días, 30 min).** El día LEGS se quedaba solo con `upper legs` y V15 lo rechazaba. El testigo cambia un cuádriceps por una pantorrilla 2x13/90, que es la dosis base de 4D.

**Corrección.** Un relleno único de su grupo no sale en la ronda 1 si el día quedaría con menos de dos grupos. En su lugar sale, en la ronda 2, una estructural con el slot y el grupo repetidos.

## 2. Garantías

| Garantía | Cómo se comprueba |
|---|---|
| Ningún plan válido anterior cambia | Matriz completa antes y después: **los 521 planes válidos del motor original salen idénticos**, ejercicio por ejercicio. Los cambios solo actúan si el día seguía bajo el piso (D-1 a D-3) o si el recorte iba a dejar un solo grupo (D-4) |
| Campaña de 100 semanas sin cambios | `CampanaP123Cohorte02IT`, re-ejecución determinista (IA simulada, 0 llamadas a Replicate): la evidencia solo difiere en marcas de tiempo |
| Ningún ejercicio artificial | Solo entran ejercicios elegibles con una capacidad admisible del objetivo y su dosis aprobada |
| Ningún descanso adicional | Los descansos salen de la dosis aprobada; nunca se alargan |
| Ningún exceso de dosis | Series hasta `seriesMax`, solo mientras falte para V20; 4D sin cambios |
| Sin D8 en la generación inicial | `GeneradorReglasCasoAIT` exige cada sesión ≥ piso de V20, sin herencia |
| Duración declarada = calculada | `GeneradorReglasCasoAIT`: `expectedDurationMinutes == estimateMinutes` en cada sesión |
| V4, V20, 4D, P-01 y las dosis por capacidad | Sin cambios en `PlanDraftValidator`, `MvpDosePolicy`, `CapabilityPrescriptionPolicy`, `GoalType` ni `UserProfile` |

## 3. Pruebas de regresión

`GeneradorReglasCasoAIT`: 10 casos con el mismo `user_id` y la misma semana que en la matriz. Las 10 fallaban antes de su corrección: las 5 primeras con el código de `12555f6`; las 5 últimas con la versión intermedia de D-1/D-2, como consta en las matrices sucesivas.

| Caso | Perfil | Fallaba con | Lo corrige |
|---|---|---|---|
| F0011 | LOSE_WEIGHT intermedio, 3 días, 105 | V20 (3.804 s / 4.410) | D-1 |
| F0039 | INCREASE_STRENGTH principiante, 3 días, 75 | V20 | D-1 |
| F0063 | GENERAL_FITNESS intermedio, 3 días, 75 | V20 | D-1 |
| F0351 | GENERAL_FITNESS intermedio, 6 días, 75 | V15 + V20 | D-1 |
| F0313 | GAIN_MUSCLE intermedio, 6 días, 30 | V15 | D-4 |
| F0345, F0705 | GENERAL_FITNESS principiante, 6 días, 75 (gimnasio / casa) | V20 | D-2 |
| F0777, F0921 | GENERAL_FITNESS principiante, 3 y 2 días, 75, casa peso corporal | V20 | D-2 |
| F0064 | GENERAL_FITNESS intermedio, 3 días, 90 | V20 | D-3 |

En cada caso se exige:
- el plan completo con el validador de producción (V1–V25);
- la misma frecuencia (V2);
- cada sesión ≥ piso de V20 (sin D8);
- ≤ techo de V4;
- ≤ tope de V24;
- duración declarada = calculada.

## 4. Impacto

| | Antes | Después |
|---|---:|---:|
| Combinaciones con plan válido (de 1.236) | 521 | **639** |
| CASO A conocidos (existe plan y el motor no lo construye) | 126 | **8** |
| Planes válidos alterados | — | 0 |

## 5. Pendiente: 8 CASO A (GENERAL_FITNESS, 4–6 días)

Ver `INVESTIGACION_FACTIBILIDAD_30_120.md` §3.6.

**Causa.** Con divisiones UPPER/LOWER o PUSH/PULL/LEGS, un patrón de fuerza solo cabe en ciertos días. La redistribución exige varios movimientos encadenados. Un repaso más, con tres vueltas, se probó y no resolvió ninguno, así que no se conservó.

**Opciones (tu decisión; ninguna cambia una política):**
- **(a)** Un constructor de semana, con asignación global de slots y capacidades. Es el diseño del verificador constructivo de la prueba: reparte primero lo obligatorio y después llena con lo admisible más largo. Es un cambio mayor del motor, para otro PR.
- **(b)** Dejarlo como está. Esas 8 combinaciones quedan sin plan con el motor de reglas (NO_FACTIBLE), aunque la IA podría encontrarlo.
- **(c)** Usar el verificador constructivo como segundo intento del respaldo cuando el motor falla V20. Es menos invasivo que (a), pero es un mecanismo nuevo.
