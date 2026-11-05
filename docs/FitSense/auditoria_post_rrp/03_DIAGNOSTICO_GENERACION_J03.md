# 03 · Diagnóstico de la generación inicial: J03 y barrido sistemático

**Método:**
- motor de reglas real (`ruleBasedTrainingPlanGenerator`) con el contexto real de `prepare`;
- validador de producción (`PlanDraftValidator`);
- Testcontainers con `postgres:16-alpine`;
- sin IA y sin Replicate.

**Herramientas:**
- `DiagnosticoGeneracionJ03IT`, que hace una búsqueda exhaustiva de un paso: añadir, +1 serie o sustituir cada colocación por cualquier candidato elegible con cualquier dosis admisible, más redistribución de cardio entre días seguidos;
- `BarridoGeneracionInicialIT`, con 3.780 perfiles y 10 semillas extra por cada inválido.

Evidencia: `evidencia/j03/`, `evidencia/familias/` y `evidencia/barrido*/`.

## 1. ¿Qué significa `sessionMinutes` en el código?

- El código no lo usa ni como máximo ni como objetivo exacto, sino como un **valor nominal con un intervalo aceptado de [⌈0,70·M·60⌉ s, 1,15·M]**.
  - El piso lo pone V20 y el techo, V4.
  - Ambos se miden con el estimador: 300 s de calentamiento, más trabajo y descansos, más 60 s por transición.
- La documentación lo describe de cuatro maneras incompatibles (documento 01 §2.a): preferencia, tiempo reservado, disponibilidad y límite de admisión.
- **Consecuencia:** si la persona declara 70 min como *disponibles*, el sistema la obliga a entrenar al menos 49. Y si con el catálogo de su equipo no se llega a 49, **la deja sin plan**.

## 2. J03: LOSE_WEIGHT, principiante, casa sin material, 4 días, 70 min

### Lo que genera el motor

| Sesión | Enfoque | Contenido | Segundos | Piso V20 |
|---|---|---|---|---|
| Lun | UPPER_BODY | 6 de fuerza (3 de pecho, 2 de brazo, 1 de espalda; 3×8 o 2×8, 120 s) + 2 de cardio (3×90 s, 60 s) | 2.940 | 2.940 ✔ |
| Mar | LOWER_BODY | 3 de fuerza de pierna (3×8, 120 s) + 3 de cardio (3×90 s) | **2.706** | 2.940 ✘ (−234 s) |
| Jue | UPPER_BODY | igual que el lunes | 2.940 | ✔ |
| Vie | LOWER_BODY | igual que el martes | **2.706** | ✘ (−234 s) |

### Búsqueda exhaustiva de un paso

| Modificación (en cada sesión LOWER) | Intentos | Válidos | Qué lo impide |
|---|---|---|---|
| +1 ejercicio de pierna (upper legs) | 30 | 0 | Con 2 series no llega a V20. Con 3 series llega (≥ 2.940 s), pero queda **un único problema**: la **regla de la mitad de V15** (4 de 4 ejercicios de trabajo en un mismo grupo). |
| +1 cardio | 2 | 0 | Solo hay 5 cardios elegibles y V18 prohíbe repetirlos en días seguidos (lun-mar, jue-vie). |
| +1 de pecho, espalda o brazo | 52 | 0 | V15: grupos ajenos al enfoque LOWER_BODY. |
| +1 serie | 6 | 0 | V24: el cardio y la fuerza ya están en 3 series, el máximo de su capacidad. |
| Sustituir una colocación | 504 | 0 | V20, V15 y V18. |
| Mover un cardio desde la sesión del lunes | — | 0 | El lunes queda justo en 2.940 s. Si pierde un cardio, baja del piso. |

**Conclusión:** con las reglas vigentes **no existe ninguna semana válida** para J03 a un paso del borrador, y el borrador ya está en el máximo analítico de la sesión LOWER. **No es un fallo del algoritmo**: es una imposibilidad que producen las reglas. La regla que lo bloquea en último término es la mitad de V15:
- Sobre los grupos **nominales** del enfoque (upper legs, lower legs, waist), exige que ninguno pase de la mitad.
- Pero para LOSE_WEIGHT solo **upper legs** tiene dosis resistida prescribible. Waist y lower legs no tienen dosis de fuerza para LW.
- Así, la regla pide repartir entre grupos que el propio sistema no deja prescribir.

El código ya corrigió esta misma contradicción para el **mínimo** de grupos de V15, que hoy cuenta solo los grupos prescribibles (`prescribibleParaV15`). La regla de la mitad se dejó a propósito con los grupos nominales (javadoc de `prescribibleParaV15`). La incoherencia está documentada, pero no resuelta.

**Clasificación:** regla contradictoria (V15 mitad frente a V15 mínimo y V24/V25 en LW). Influyen además V20 (heurística sin respaldo, documento 02 A) y el catálogo de cardio sin material (5 elegibles).

**Corrección propuesta (requiere aprobación):** P-G2 del documento 06. Aplicar la regla de la mitad solo cuando el enfoque tenga **2 o más grupos prescribibles**. Su impacto se mide con un prototipo de laboratorio apagado (documento 07).

## 3. Barrido sistemático: 3.780 perfiles admitidos

**Rejilla:** 4 objetivos × 3 niveles × 3 equipamientos (gimnasio; casa con mancuernas y bandas; casa con peso corporal) × 1–7 días × {30, 35, 40, 45, 50, 53, 55, 59, 60, 61, 65, 68, 69, 70, 75} min.

**Resultado de base:**
- **3.626 válidos y 154 sin plan (4,1 %)**;
- **reproducible:** una segunda ejecución da 3.780/3.780 idénticos;
- con 10 semillas más por cada inválido (otros usuarios), **solo 2 de los 154 se rescatan**. Los otros 152 fallan con las 11 semillas.

### Familias

| Familia | Perfiles | Primer fallo | ¿Otra semilla lo arregla? | Causa (diagnóstico por perfil representativo) | Clasificación |
|---|---|---|---|---|---|
| GAIN_MUSCLE intermedio o avanzado, casa sin material, 5–7 días, 45–75 min | 72 | V20 en la sesión **PULL** (y en PUSH a 75 min) | 0/720 | La división de 5–7 días incluye un PULL. En casa sin material solo hay 5 ejercicios «de espalda», ninguno de tracción real (curl lumbar, toques de pies, inclinación pélvica, «arquero de pie», «elevación de codo y push-up invertido»), y 2 curls de bíceps. A 45 min falta medio minuto (1.860 frente a 1.890 s) con el tope de 6 colocaciones; a 70 min faltan 870 s con todo el material disponible. | **Limitación del catálogo** (no hay tracción sin material) **+ heurística de la división** (asigna un PULL sin comprobar que hay material) **+ V20**. Además, el PULL que sí se genera a 30–40 min es de **baja calidad** deportiva (ver §4). |
| INCREASE_STRENGTH intermedio o avanzado, casa sin material, 5–7 días, 65–75 min | 30 | V20 en PULL | 0/300 | La misma escasez de tracción, con 7 colocaciones posibles. | Igual que la anterior. |
| LOSE_WEIGHT principiante, casa (con o sin material), 4, 5 y 7 días, 65–75 min | 30 | V20 en LOWER_BODY | 0/300 | El mecanismo de J03 (§2). | **Regla contradictoria** (mitad de V15) + V20 + catálogo de cardio. |
| LOSE_WEIGHT, 1 día, 35–50 min (todos los equipos y niveles) | 15 | V15: FULL_BODY con un solo grupo | 0/150 | El motor coloca 2 de fuerza de pierna (cada una cubre un slot 4C distinto) + 2 de cardio. `asegurarGrupos` solo sabe **cambiar**, y ninguna de las dos se puede quitar sin perder un slot. Había margen hasta el techo para **añadir** (2.148 s frente a 3.105). | **Fallo del algoritmo.** Corregido en general con AUD-G1 (documento 06). |
| GENERAL_FITNESS principiante, 1 día, 70–75 min (gimnasio y mancuernas) | 4 | V20 | 0/40 | 8 colocaciones (tope) = 2.928 s frente a 3.150. De los 14.876 cambios de un paso, los 238 que arreglan la sesión **todos quitan la única movilidad**, y entonces V25 rechaza la semana (MOBILITY es principal en GF). | Imposibilidad con las reglas: tope 8 + V20 + V25. **Heurística sin respaldo** (V20) + tope de 8 (ingeniería). |
| GENERAL_FITNESS principiante, casa sin material, 1, 4 y 5 días, 75 min | 3 | V20 | **17/30 semillas válidas** (2 de 3 perfiles) | Depende de la semilla: algunas selecciones llegan al piso y otras no. | **Fallo del algoritmo (sensible a la semilla).** No se corrige en esta auditoría (ver documento 06, D-G3). |

### Mínimo requerido de tiempo y semántica

En las familias con V20, el déficit máximo es:
- GM sin material: 1.080 s;
- IS sin material: 516 s;
- LW principiante en casa: 444 s;
- GF principiante: 222 s.

Ninguna de las fuentes revisadas (documento 02) respalda que una sesión **más corta que el tiempo disponible** sea inválida. La OMS y el ACSM expresan la dosis en minutos por semana y en series por grupo y semana. Con `sessionMinutes` como máximo y un mínimo de dosis semanal por objetivo, estas familias tendrían plan. **Es un cambio de política y del contrato de V20: requiere aprobación (P-G1).**

## 4. Perfiles con plan válido pero de calidad dudosa

Que un plan sea válido no significa que sea bueno.

**GM y IS sin material, sesión PULL (30–40 min).** Lo que se genera como «tracción para hipertrofia» son:
- curl lumbar (extensión de tronco);
- toques de pies e inclinación pélvica (flexión de tronco, prácticamente movilidad);
- «arquero de pie» (rotación);
- «elevación de codo y push-up invertido» (patrón HORIZONTAL_PUSH clasificado como back).

**Ninguno es una tracción horizontal ni vertical.** Pasa los validadores porque el grupo `back` del catálogo los admite, pero **no cumple la intención del enfoque PULL**.

**Propuestas:**
- en casa sin material, no asignar PULL o fusionarlo con UPPER_BODY (P-G3, heurística de división);
- añadir al catálogo remos invertidos con mesa o toalla, con sus requisitos de seguridad (P-C1);
- en ambos casos, revisión profesional.

## 5. Respuestas a las preguntas obligatorias de esta fase

| Pregunta | Respuesta |
|---|---|
| ¿J03 es una imposibilidad real o un fallo del algoritmo? | Imposibilidad **con las reglas vigentes**: la búsqueda exhaustiva de un paso con sustituciones no encuentra ningún plan válido. No es un fallo del motor: el motor llega al máximo analítico. |
| ¿Hay otras familias admitidas sin plan? | Sí, **154 perfiles en 6 familias**. 15 se deben a un fallo del algoritmo (corregido), 3 son sensibles a la semilla y 136 son imposibilidades producidas por reglas, catálogo o división. |
| ¿Alguna familia completa queda sin plan por limitaciones artificiales? | Sí. **GM y IS intermedio o avanzado sin material con 5–7 días y ≥ 45–65 min** (102 perfiles): las causas son la falta de tracción en el catálogo y una división que asigna PULL sin comprobarlo. **LW principiante en casa con 4, 5 o 7 días y ≥ 65 min** (30 perfiles): la causa es la regla de la mitad de V15 sobre grupos no prescribibles. |
| ¿Se corrige J03 en esta auditoría? | **No** sin aprobación. Toda corrección de J03 cambia el contrato de V15 o el de V20. La propuesta, su justificación y su impacto medido están en los documentos 06 y 07. |
