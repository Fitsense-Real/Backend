# Instrucciones para la evaluación de planes de entrenamiento

Gracias por participar. Este paquete contiene planes de entrenamiento semanales generados para perfiles **ficticios** de adultos sanos, sin datos personales. Le pedimos su valoración profesional e independiente.

## 1. Archivos

| Archivo | Contenido |
|---|---|
| `MUESTRA_PROFESIONAL_CIEGA.csv` | Planes con código P01–P99. Una fila por ejercicio, con estos datos: objetivo, nivel, días disponibles, minutos configurados, equipamiento, restricciones, número de sesión, día relativo de la semana, enfoque de la sesión, duración estimada de la sesión, orden, nombre, descripción, patrón de movimiento, músculo principal y secundarios, tipo de prescripción, series, repeticiones o segundos y descanso |
| `FICHAS_NO_FACTIBLE.csv` | Fichas D01–D02. Describen perfiles para los que **no se generó** una semana de entrenamiento y los ejercicios que había disponibles |
| `PLANTILLA_RESPUESTAS_EVALUADOR.csv` | Una fila por código, para registrar sus respuestas |

**Notas sobre los datos:**
- No hay cargas en kg ni RIR: la intensidad se expresa con series, repeticiones o segundos y descanso.
- La duración estimada incluye el calentamiento y las transiciones.

## 2. Escala de valoración de cada plan (P01–P99)

Valore cada criterio de 1 a 5, o NS si no se puede valorar con la información dada:
- 1 = inadecuado;
- 2 = deficiente;
- 3 = aceptable con cambios;
- 4 = adecuado;
- 5 = muy adecuado.

| # | Criterio | Pregunta guía |
|---:|---|---|
| c1 | Estructura y objetivo | ¿La semana responde al objetivo declarado? |
| c2 | Series y repeticiones | ¿Son adecuadas para el objetivo y el nivel? |
| c3 | Descansos | ¿Son adecuados? |
| c4 | Selección de ejercicios | ¿Son pertinentes, variados y no redundantes? |
| c5 | Distribución semanal | ¿El reparto por días y enfoques es razonable? |
| c6 | Recuperación | ¿Hay recuperación suficiente entre sesiones del mismo grupo? |
| c7 | Volumen | ¿El volumen semanal por grupo es suficiente y no excesivo? |
| c8 | Intensidad | ¿Está bien orientada, sabiendo que no hay carga ni RIR? |
| c9 | Seguridad | ¿Hay algún riesgo evidente para un adulto sano del nivel indicado? |
| c10 | Duración | ¿La duración estimada es adecuada para el tiempo configurado y el objetivo? |
| c11 | Nivel | ¿Es adecuado para el nivel indicado? |
| c12 | Observaciones | Correcciones sugeridas (ejercicio, dosis o día) |

**Pregunta de duración:** «¿La sesión es suficientemente completa para el objetivo aunque dure menos que el tiempo configurado?» Responda Sí / No / Con cambios, e indique qué le falta.

**Preguntas específicas** (responda Sí / No / Con cambios / NS):

| # | Pregunta |
|---|---|
| e1 | ¿Algún ejercicio no hace lo que indican su patrón o su descripción? ¿Cuál? |
| e2 | En las sesiones de empuje (PUSH), tracción (PULL) o piernas (LEGS), ¿todos los ejercicios pertenecen a esa función? ¿Cuál sobra o falta? |
| e3 | ¿Las sesiones de espalda o de tren superior incluyen al menos un ejercicio real de tracción (remo o dominada)? ¿Es suficiente? |
| e4 | ¿Hay alguna sesión con demasiadas series para un mismo grupo muscular? |
| e5 | Si la sesión dura bastante menos que el tiempo disponible, ¿es aceptable con ese contenido? |
| e6 | Solo planes con «ejercicios excluidos por dolor previo»: ¿la sesión es razonable sin esos ejercicios? |
| e7 | Solo planes de pérdida de peso (LOSE_WEIGHT): ¿la distribución de fuerza entre los grupos grandes es aceptable? ¿Falta algún grupo? |

## 3. Fichas D01–D02

Para el perfil y el material indicados no se generó la semana, porque no había ningún ejercicio de tracción disponible. A la vista de los ejercicios disponibles:

1. ¿Es pertinente no generar la semana? Sí / No.
2. Si no lo es, ¿qué sesión de espalda propondría con **solo** esos ejercicios?
3. ¿Qué material mínimo recomendaría?

Puede escribir las respuestas en la columna «observaciones» de la plantilla.

## 4. Preguntas generales (una vez, al final)

- **G1.** ¿Pertenecen la extensión de tronco y la flexión de codo a una sesión de tracción? ¿Y las elevaciones de hombro a una de empuje? ¿Debe contar el trabajo de core como trabajo resistido del tren inferior?
- **G2.** ¿Las palancas en barra (*back lever*, *front lever*) son trabajo de espalda?
- **G3.** ¿Basta un único ejercicio de tracción (remo con toalla en un anclaje firme) como tracción de toda la semana?
- **G4.** ¿Es aceptable que un plan de pérdida de peso tenga semanas sin fuerza en algún grupo grande?
- **G5.** ¿Es aceptable sustituir un ejercicio de fuerza por uno de cardio para alcanzar la duración mínima de una sesión (perfil principiante de 1 día de entrenamiento general)?

## 5. Registro

- Indique en la plantilla su código de evaluador y la fecha.
- Si lo desea, su cualificación, aparte.
- Valore cada plan con independencia de los demás.
- No hay respuestas correctas esperadas: nos interesa su criterio profesional.
