# Auditoría del catálogo: los cuatro casos del 46 % y R0800

**No se ha cambiado ninguna clasificación, dificultad ni registro del catálogo.**

**Datos:**
- `CATALOGO_CASOS_EXTREMOS.csv`: fila completa del catálogo de cada ejercicio usado (id, `source_code`, nombre en inglés y español, zona, equipo, músculo objetivo, sinergista, secundarios, patrón, aislamiento, dificultad, prescripción, activo e instrucciones).
- `USO_EJERCICIOS_CASOS_EXTREMOS.csv`: dónde y con qué dosis se usa cada uno.
- `CATALOGO_ALTERNATIVAS_PULL_PESO_CORPORAL.csv`: todo lo elegible de espalda y brazo para un PULL sin material.

## 1. Sesiones PULL de R0788, R0792, R0836 y R0840

Perfiles GAIN_MUSCLE intermedio o avanzado, peso corporal, 6–7 días, 75 min. La sesión PULL dura 2.070 s (34,5 min, 46 %).

| Id | Nombre (catálogo) | Zona / objetivo | Patrón | Clase en el motor | Dosis | Función real según el catálogo | Valoración |
|---|---|---|---|---|---|---|---|
| 775 | Elevación de codo y push-up invertido | back / upper back (sinergista: tríceps) | **HORIZONTAL_PUSH** | ESTRUCTURAL, slot **UPPER_PUSH** | 3×10/90 | Las instrucciones describen presionar con las palmas desde boca abajo: es un **empuje** | **Posiblemente mal clasificado para PULL**: la zona dice espalda y el patrón, empuje. Ocupa en PULL el lugar de una tracción |
| 1066 | Curl de espalda baja | back / spine (glúteos, isquios) | TRUNK_EXTENSION | RESISTIDO | 2×10/90 | Extensión lumbar en el suelo | **Correcto como extensión de tronco**, pero **no es tracción**. Pertinencia en PULL: dudosa |
| 1259 | Arquero de pie | back / upper back (hombros, bíceps) | TRUNK_ROTATION_LATERAL_FLEXION | RESISTIDO | 2×10/90 | Rotación de tronco de pie con brazos extendidos, **sin resistencia** | **Dudoso como hipertrofia**: parece movilidad o calentamiento |
| 1261 | Inclinación pélvica de pie | back / spine (abdominales) | TRUNK_FLEXION | RESISTIDO | 2×10/90 | Basculación pélvica de pie | **Posiblemente mal clasificado como resistido**: es control motor o movilidad |
| 1308 | Toque de dos dedos de los pies | back / spine (isquios, glúteos) | TRUNK_FLEXION | RESISTIDO | 2×10/90 | Flexión de tronco hacia los pies: estiramiento | **Posiblemente mal clasificado como resistido** (movilidad o estiramiento) |
| 315 | Curl de concentración de bíceps con pierna | upper arms / biceps | ELBOW_FLEXION | RESISTIDO | 3×10/90 | **Las instrucciones dicen «sujeta una mancuerna»**, pero el equipo es `body weight` | **Datos incoherentes**: o falta el equipo o las instrucciones no corresponden («con pierna» sugiere resistencia con la propia pierna) |
| 320 | Curl de bíceps acostado de lado con peso corporal | upper arms / biceps | ELBOW_FLEXION | RESISTIDO | 3×10/90 | Flexión de codo **sin carga** | **Dudoso como hipertrofia**: sin resistencia externa no se puede asegurar el estímulo. Pertinencia: **no demostrada** |

**Alternativas** (`CATALOGO_ALTERNATIVAS_PULL_PESO_CORPORAL.csv`, 19 elegibles de espalda y brazo para R0788):
- **0 con patrón HORIZONTAL_PULL o VERTICAL_PULL.** El catálogo de peso corporal no tiene ninguna tracción horizontal ni vertical elegible para este perfil.
- Las 19 son 8 extensiones de codo o empujes de brazo (no admitidos en PULL por V22), 2 flexiones de codo, 2 de movilidad, 2 flexiones de tronco, 1 extensión de tronco, 1 rotación, 1 sin patrón (OTHER) y 2 de empuje con zona espalda.

**Conclusión**
- **El PULL sin material no contiene ningún ejercicio de tracción.** Se compone de un empuje con etiqueta de espalda, cuatro movimientos de tronco y dos flexiones de codo sin carga.
- La saturación (46 %) es real con este catálogo, pero **el problema de fondo es el catálogo, no V20**.
- **Los cuatro casos del 46 % no deben considerarse científicamente adecuados por pasar C2-D.** Quedan como **no demostrados** y van a la revisión profesional (anexo de la muestra: A01, A04, A07 y A08).

## 2. R0800 y relacionados (INCREASE_STRENGTH intermedio/avanzado, peso corporal, 6–7 días, 75 min)

| Aspecto | Medido |
|---|---|
| Sesiones | PUSH ×2 (71,5 %), PULL ×2 (58,5 %), LEGS ×2 (70,2 %) |
| Series por semana | 138; pierna (upper legs): 42 |
| Máximo de un grupo en una sesión | **21 series** (LEGS: 7 ejercicios de pierna de 3 series) |
| Repetición de ejercicios | 10 ejercicios aparecen dos veces en la semana, por la disponibilidad limitada |
| Fuerza en PULL | La única dosis de fuerza (3×6/150) es **775, un empuje**. Los slots cubiertos son LOWER_HIP, LOWER_KNEE y UPPER_PUSH: **UPPER_PULL no se cubre como fuerza** |
| Distribución | PUSH, PULL y LEGS dos veces, sin repetir grupo en días seguidos (V18 cumplida) |

**Valoración**
- **No se declara inseguro** por el número de series.
- **Tampoco adecuado** por cumplir V24.
- **Limitaciones demostradas:**
  1. sin tracción real;
  2. volumen de pierna muy concentrado (21 series en una sesión, por encima del orden de magnitud que la revisión cita como saturación por sesión, ~11 fraccionales, aunque la fuente es un preprint);
  3. ejercicios de «espalda» que no trabajan como tal.
- **Pendiente de revisión profesional:** R0800 ya está en la muestra original (M09, C2-D) y su ciclo real en el anexo (A06).

## 3. Efecto sobre la factibilidad y propuestas (sin aplicar)

| Opción | Efecto previsible | Requiere |
|---|---|---|
| Reclasificar 1261, 1308 y 1259 como MOVILIDAD | Saldrían del PULL resistido; el PULL sin material se quedaría aún más corto o sin grupos (V15) | Decisión específica sobre el catálogo |
| Corregir el equipo o las instrucciones de 315 | Podría dejar de ser elegible con peso corporal | Revisión del registro |
| Patrón de 775 coherente con su zona | Si es empuje, no debería contar como espalda en PULL | Decisión específica |
| Añadir tracciones sin material (remo invertido bajo mesa, remo con toalla en puerta) | Daría un UPPER_PULL real a peso corporal | Alta de ejercicios y revisión de seguridad |

**Ninguna se ha aplicado:** cambiar el catálogo altera validaciones anteriores y requiere aprobación.
