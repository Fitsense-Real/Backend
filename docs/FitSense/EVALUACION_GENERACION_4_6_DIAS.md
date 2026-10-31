# Evaluación de la generación: 4–6 días, V15, V20, catálogo y frecuencia

Evaluación del 28/10/2026. Se hizo **solo con pruebas y lectura**:
- No se cambió ninguna regla ni el código de producción.
- No hubo llamadas a Replicate y Neon no se tocó.
- La generación es la del **motor de reglas**, que es también el respaldo cuando la IA falla. PostgreSQL efímero con V1–V35.

**Material de la evaluación:**

| Qué | Dónde |
|---|---|
| Prueba de evaluación (opcional) | `src/test/java/.../planning/EvaluacionFrecuenciaIT.java`, con `-Dfitsense.evaluacion=true` |
| Evidencia | `docs/FitSense/evidencia/frecuencia/`: una fila por plan y una por sesión |
| Defecto de V20 | `V20UmbralRedondeoTest` (unitaria, fija el comportamiento actual) |

## 1. Generación de 4, 5 y 6 días

**Perfiles:** 72, de 4 objetivos × 3 niveles × 4/5/6 días × gimnasio/casa, todos con 45 minutos. Los días disponibles son consecutivos desde el lunes y el equipo es el de las 72 referencias.

| Resultado | Perfiles |
|---|---|
| Plan válido, cobertura de capacidades `COMPLETO` | 60 |
| **Sin plan**: el motor de reglas no pasa sus propios validadores | **12** |

**Días solicitados frente a programados:**

| Nivel | 4 días | 5 días | 6 días |
|---|---|---|---|
| BEGINNER | 4 | **4** (tope) | **4** (tope) |
| INTERMEDIATE | 4 | 5, o sin plan | 6, o sin plan |
| ADVANCED | 4 | 5, o sin plan | 6, o sin plan |

**Divisiones:**

| Sesiones | División | Planes |
|---|---|---|
| 4 | superior / inferior / superior / inferior | 40 |
| 5 | empuje / tirón / piernas / superior / inferior | 10 |
| 6 | empuje / tirón / piernas × 2 | 10 |

### Los 12 perfiles sin plan

**Patrón:** LOSE_WEIGHT intermedio y avanzado (gimnasio y casa, 5 y 6 días), y GENERAL_FITNESS intermedio y avanzado (casa, 5 y 6 días).

**Causa:** todos fallan **V15** en las sesiones de empuje, tirón y piernas:
- «LEGS solo trabaja [upper legs]: debe cubrir al menos 2 grupos de [upper legs, lower legs]».
- «PULL solo trabaja [back]: debe cubrir al menos 2 grupos de [back, upper arms]».

El generador por capacidades (`CapabilityWeekBuilder`, que usan LOSE_WEIGHT, GENERAL_FITNESS e INCREASE_STRENGTH) coloca poco trabajo de fuerza en esos objetivos y no garantiza dos grupos en PULL y LEGS.

**Consecuencia en producción:**
- Para estos perfiles el respaldo **no existe**.
- Si la IA falla 3 veces o Replicate no responde, la persona se queda **sin plan semanal** y su adherencia es `NULL`.
- Solo se midió la semana del 02/11/2026. La semilla depende del usuario y de la semana, así que en otras semanas el resultado puede variar.

### Sesiones válidas con una distribución dudosa

De 270 sesiones, 144 tienen al menos una señal:

| Señal | Sesiones | Dónde |
|---|---|---|
| Ejercicio en revisión (§3): 1267 en 44, 872 en 27, 1059 en 25, 1296 en 6 | 96 | Sobre todo superior y empuje (1267), inferior (1059, 872) |
| Por debajo del 70 % exacto de la duración (§4) | 55 | Todas las divisiones |
| Menos de 3 ejercicios de trabajo | 16 | Superior e inferior de LOSE_WEIGHT |
| `waist` iguala o supera a la pierna en un tren inferior | 12 | LOSE_WEIGHT |
| Un grupo con más de la mitad, sin que V15 se aplique (menos de 4 de trabajo) | 10 | Inferior de LOSE_WEIGHT principiante (`upper legs=2, waist=1`) |
| Tren inferior con menos de 2 ejercicios de pierna | 8 | LOSE_WEIGHT intermedio y avanzado, 4 días: `{upper legs=1, waist=1}` |

**Stalder press (1267) aparece en las 12 combinaciones de objetivo y nivel** de 4–6 días. No aparecía en las referencias porque en cuerpo completo no entra `upper arms`.

## 2. Análisis de V15

**Qué exige**, sobre los ejercicios de trabajo (el cardio y la movilidad que entran como complemento no cuentan):
1. ningún grupo ajeno al enfoque;
2. al menos 2 grupos distintos del enfoque, si hay material;
3. si el enfoque tiene 3 o más grupos y hay 4 o más ejercicios de trabajo, ningún grupo por encima de la mitad.

**Hallazgos:**

1. **Cuenta grupos de catálogo, no función.** `LOWER_BODY` = `upper legs`, `lower legs` y `waist`. Hay ejercicios de pierna catalogados como `waist`: 1059 «zancada con giro», 1102 y 1103 (los tres `KNEE_DOMINANT`). En el caso del usuario 29, el 2.º intento de la IA se rechazó por «4 de 6 a waist», pero esos 4 eran 1103, 1059, 1102 y 872: **tres ejercicios de pierna mal etiquetados** y un abdominal. La regla rechazó por la etiqueta, no por un desequilibrio real.
2. **La regla de la mitad solo se aplica con 4 o más ejercicios de trabajo.** El motor de reglas la esquiva con 2–3 ejercicios de trabajo y sesiones más cortas. Las sesiones `upper legs=2, waist=1` pasan.
3. **El mínimo de 2 grupos se cumple con `waist`:** un «tren inferior» con un solo ejercicio de pierna y uno de abdomen pasa (8 sesiones).
4. **Es la causa de los 12 perfiles sin plan.** Con poca fuerza por sesión, el mínimo de 2 grupos en PULL y LEGS no se alcanza.
5. **Sin evidencia de IA en divisiones:** las 4 campañas con Replicate solo generaron cuerpo completo.

**Conclusión:** V15 funciona como está programada. Sus dos supuestos son los débiles: que `body_part` describe la función y que `waist` es tren inferior. Antes de tocar la regla conviene corregir las etiquetas de §3 y decidir funcionalmente si `waist` debe contar como tren inferior.

## 3. Revisión visual del catálogo

Se revisaron 6 fotogramas de cada GIF del dataset de origen (`hasaneyldrm/exercises-dataset`, © Gym visual) junto con sus metadatos.

| Id | Ejercicio | Lo que muestra el GIF | Catálogo | Inconsistencias |
|---|---|---|---|---|
| **1267** | Stalder press | Desde la **vertical de manos**, baja abierto de piernas a apoyo y vuelve a subir: habilidad gimnástica avanzada | nivel 1, `upper arms`/tríceps, `KNEE_DOMINANT`, `requires_inversion = false` | Dificultad (debería ser 3 o quedar excluido); patrón (no es de rodilla, es empuje vertical invertido); **inversión no marcada**, un indicador de seguridad |
| **1296** | Superman push-up | Flexión **explosiva**: el cuerpo despega y extiende los brazos al frente | nivel 1, `high_impact = false` | Dificultad y **alto impacto no marcado**. Es el único pliométrico sin `high_impact` del catálogo, junto con «smith sprint lunge» |
| **1333** | Wheel run («Rodar con rueda») | **Carrera / sprint de pie**; no aparece ninguna rueda abdominal | `cardio`, `LOCOMOTION_CARDIO`, equipo **`ab wheel`**, `requires_floor = true`, `requires_overhead = true` | El cardio es **correcto**. El equipo es **erróneo**: el dataset dice `body weight` y V24 lo cambió a `ab wheel` por el nombre. También son erróneos suelo y por encima de la cabeza, y el nombre en español. Solo se ofrece a quien declara rueda abdominal, y las notas de la IA hablaban de «la rueda» |
| **1059** | Zancada con giro | Zancada con rotación del tronco | `waist` (abs), `KNEE_DOMINANT` | El patrón es correcto; `waist` lo cuenta como abdomen en V15. El grupo debería decidirse por función |
| **872** | Levantamiento de cadera con rodillas flexionadas | Tumbado, eleva las piernas flexionadas hacia el pecho: es un **encogimiento invertido** | `waist`, `HIP_EXTENSION` | `waist` es correcto; el **patrón es erróneo** (es `TRUNK_FLEXION`, como el «reverse crunch» 1139). El nombre en español sugiere un puente de glúteo |

### Casos parecidos (consultas deterministas sobre el catálogo activo)

| Consulta | Resultado |
|---|---|
| Habilidades avanzadas con dificultad 1–2 (tras quitar los falsos positivos de «lever» = máquina) | **Nivel 1:** 461 clap push up, 937 kipping muscle up, 1068 muscle up, 1267, 1296. **Nivel 2:** 56 archer push up, 64 archer pull up, 1078 one arm dip, 1100 y 1180 single arm push-up |
| Pliométricos sin `high_impact` | 1296 superman push-up, 1289 smith sprint lunge |
| Invertidos sin `requires_inversion` | 1267 stalder press. Los 5 «inverted row» son remos, no inversiones: correctos |
| `body_part` incoherente con el patrón | **37 ejercicios en 12 combinaciones.** Entre ellos: `upper arms` + `KNEE_DOMINANT` (7, con 1267), `back` + `KNEE_DOMINANT` (5), `waist` + `KNEE_DOMINANT` (1059, 1102, 1103), `waist` + `HIP_EXTENSION` (872, 1225), `upper legs` + `TRUNK_FLEXION` (5), `upper legs` + empuje (6) |

**Contexto:** ninguna dificultad del catálogo está aprobada por personas. V9 cargó 2 como marcador, V21/V24 la ajustaron sin revisión visual, V27 tiene solo 4 aprobaciones y el filtro está apagado.

**Propuesta:** no cambiar clasificaciones sin revisión humana.
1. Revisar visualmente las 37 incoherencias de patrón y los 10 casos de habilidad avanzada.
2. Registrar las decisiones con el flujo de V27: dos revisores.
3. Corregir con **una migración nueva**. V24 no se modifica.

## 4. Impacto de exigir el 70 % exacto en V20

**Defecto** (`V20UmbralRedondeoTest`): el mínimo se trunca (45 × 70 / 100 = 31) y la duración estimada se redondea al minuto. Por eso:

| Configurado | Admitido hoy desde | 70 % exacto |
|---|---|---|
| 30 min | 20:30 | 21:00 |
| 45 min | 30:30 (67,8 %) | 31:30 |
| 60 min | 41:30 | 42:00 |

**Con la comparación en segundos frente al 70 % exacto:**

| Conjunto | Sesiones por debajo | Planes afectados | Déficit |
|---|---|---|---|
| **72 referencias** (3 días) | 17 de 216 | **11 de 72** | 12–54 s por sesión |
| Barrido de 4–6 días | 55 de 270 | 25 de 60 | 24–60 s |

Referencias que cambiarían:
- LOSE_WEIGHT/BEGINNER/45 (gimnasio y casa): 30:36, 68,0 %.
- GAIN_MUSCLE intermedio y avanzado/45/casa: 31:18, 69,6 %.
- GENERAL_FITNESS/BEGINNER/45 (gimnasio y casa): 30:48, 68,4 %.
- GENERAL_FITNESS/BEGINNER/60/casa: 41:30.
- GENERAL_FITNESS intermedio y avanzado/60 (gimnasio y casa): 41:45, 69,6 %.

El mismo piso lo usa el motor de reglas para rellenar sesiones (`CapabilityWeekBuilder`, `RuleBasedTrainingPlanGenerator`). Corregirlo **cambiaría esas 11 referencias**: el motor añadiría una serie o un ejercicio. Por eso hay que recapturar `generacion_referencia.txt` con aprobación y revisar las diferencias antes de adoptarlo.

**No afecta:**
- los planes ya publicados;
- la adherencia;
- la exención de V20 para las sesiones autorizadas a acortarse (D8).

## 5. Frecuencia de principiantes: propuesta por objetivo

**Hoy:** `MAX_SESSIONS_BEGINNER = 4` para todos los objetivos.
- Su única justificación es un comentario («ACSM 2009 recomienda 2-3 días para principiantes»), que además no coincide con el valor 4.
- ACSM 2009 se refiere a **entrenamiento de fuerza** (2–3 días de cuerpo completo para novatos), no a la actividad total.
- La recomendación cardiorrespiratoria de ACSM contempla 5 días de intensidad moderada.

**Lo que hace el sistema con 4 sesiones:** superior / inferior × 2. Cada región se trabaja 2 veces por semana, dentro de la frecuencia por grupo muscular habitual para principiantes.

| Objetivo | Propuesta | Por qué |
|---|---|---|
| GAIN_MUSCLE, INCREASE_STRENGTH | **Mantener 4** | Toda sesión es de fuerza; 4 con superior/inferior ya da 2 veces por semana a cada región; más días no añade frecuencia útil a un novato |
| LOSE_WEIGHT, GENERAL_FITNESS | **Mantener 4 en el MVP**; evaluar 5 en una fase posterior | Un 5.º día solo tiene sentido como sesión de cardio o movilidad sin fuerza, y hoy el generador no tiene ese tipo de sesión. Además, el barrido muestra que las divisiones de 5–6 sesiones de estos objetivos **ya fallan** en intermedios y avanzados (§1) |

**Acciones mínimas, sin cambiar la política:**
- corregir el comentario y documentar la justificación real (frecuencia por región, no «prohibición ACSM»);
- que la app informe cuando se programan menos días de los pedidos.

## 6. Recomendaciones mínimas, sin ampliar el MVP

En orden de prioridad:

1. **Respaldo sin plan en 5–6 días (§1).** Es el riesgo más alto: afecta a intermedios y avanzados de LOSE_WEIGHT y GENERAL_FITNESS, no a un caso aislado. Hay dos opciones a decidir:
   - **a)** que el generador por capacidades garantice 2 grupos en PULL y LEGS (curl en PULL, gemelo en LEGS);
   - **b)** limitar provisionalmente a 4 las sesiones de esos objetivos hasta tener (a).
2. **Catálogo (§3):** corregir 1333 (equipo, suelo, por encima de la cabeza, nombre), 1267 (dificultad, patrón, inversión) y 1296 (dificultad, impacto), con revisión humana y en una migración nueva. Después, revisar las 37 incoherencias de patrón.
3. **V20 (§4):** comparar en segundos frente al 70 % exacto, recapturando las 11 referencias con aprobación.
4. **V15 (§2):** decidir si `waist` cuenta como tren inferior. Hasta entonces, no relajar la regla ni cambiar el prompt.
5. **Cobertura de pruebas:** dejar `EvaluacionFrecuenciaIT` como evaluación periódica. Cuando se corrija el punto 1, convertir los 12 perfiles en una regresión: ningún perfil sin plan.

**No se recomienda por ahora:**
- cambiar el prompt;
- hacer nuevas llamadas a Replicate;
- activar el filtro de aprobación humana globalmente: con 4 aprobaciones, dejaría el conjunto elegible inservible.
