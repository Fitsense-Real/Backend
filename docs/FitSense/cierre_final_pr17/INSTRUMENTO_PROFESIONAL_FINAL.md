# Instrumento de revisión profesional (final)

**Estado: listo para usar. EVALUACIÓN PENDIENTE.**
- No hay ningún evaluador registrado y **no se ha hecho ninguna revisión humana**.
- Este documento no contiene evaluaciones.
- **Ninguna cifra de este proyecto aprueba C2-D, EF ni LW-PREF-0.2 en nombre de un profesional.**

Conserva los 12 criterios del instrumento anterior (`cierre_verificacion_politicas/INSTRUMENTO_REVISION_PROFESIONAL.md`, en la rama `feature/modelo-e-planificacion`, leído sin fusionarla). Añade las preguntas específicas de esta orden (bloque E).

## 1. Material (y quién lo ve)

| Archivo | Contenido | Quién lo ve |
|---|---|---|
| `MUESTRA_PROFESIONAL_CIEGA.csv` | Planes completos con código anónimo (P01–Pnn). Por ejercicio: objetivo, nivel, días, minutos, equipamiento, restricciones sintéticas, sesión, día relativo, enfoque, duración estimada, orden, nombre, **descripción completa**, patrón, músculo principal y secundarios, tipo, series, repeticiones, segundos y descanso | **Evaluador** |
| `FICHAS_NO_FACTIBLE.csv` | Fichas de diagnóstico (D01–Dnn): perfil, material, sesión que se pedía y ejercicios disponibles. **No contiene ningún plan.** Se pide valorar si no generar la semana fue pertinente | **Evaluador** |
| `PLANTILLA_RESPUESTAS_EVALUADOR.csv` | Una fila por código, sin rellenar | **Evaluador** |
| `CLAVE_INTERNA_MUESTRA.csv` | Código → perfil, política, motivo de selección, estado y problema investigado | **Solo el equipo, después de la evaluación. No se entrega** |

**Ceguera:**
- El evaluador no ve la política, la versión, C1/C2-D/EF, el motivo de selección, el estado de aceptación ni el resultado esperado.
- Los códigos se asignan al azar con una semilla fija.
- Los planes antes y después de un cambio (EF o LW-PREF-0.2) llevan **códigos distintos y no correlativos**.
- Los perfiles son sintéticos y no tienen datos personales.

## 2. Perfil del evaluador

- Profesional del ejercicio cualificado: licenciatura en Ciencias del Deporte o equivalente, o certificación ACSM-EP o NSCA-CSCS.
- Al menos 2 años prescribiendo fuerza a adultos.
- Sin relación con el desarrollo del motor.
- **Idealmente dos evaluadores independientes.**

## 3. Ficha por plan (P01–Pnn): los 12 criterios (escala 1–5 o NS)

| # | Criterio | Pregunta guía |
|---:|---|---|
| 1 | Estructura y objetivo | ¿La semana responde al objetivo declarado? |
| 2 | Series y repeticiones | ¿Son adecuadas para el objetivo y el nivel? |
| 3 | Descansos | ¿Son adecuados? |
| 4 | Selección de ejercicios | ¿Son pertinentes, variados y no redundantes? |
| 5 | Distribución semanal | ¿El reparto por días y enfoques es razonable? |
| 6 | Recuperación | ¿Hay recuperación suficiente entre sesiones del mismo grupo? |
| 7 | Volumen | ¿El volumen semanal por grupo es suficiente y no excesivo? |
| 8 | Intensidad | ¿Está bien orientada, sabiendo que no hay carga ni RIR? |
| 9 | Seguridad | ¿Hay algún riesgo evidente para un adulto sano del nivel indicado? |
| 10 | Duración | ¿La duración estimada es adecuada para el tiempo configurado y el objetivo? |
| 11 | Nivel | ¿Es adecuado para el nivel? |
| 12 | Observaciones | Correcciones sugeridas (ejercicio, dosis o día) |

**Escala:** 1 = inadecuado, 2 = deficiente, 3 = aceptable con cambios, 4 = adecuado, 5 = muy adecuado. «NS» = no se puede valorar con la información dada.

**Pregunta específica de duración** (en todas las fichas): «¿La sesión es suficientemente completa para el objetivo aunque dure menos que el tiempo configurado?» Sí / No / Con cambios. ¿Qué le falta?

## 4. Bloque E: preguntas específicas de esta orden (en cada ficha, «Sí / No / Con cambios / NS»)

| # | Pregunta |
|---|---|
| E1 | **Correspondencia ejercicio-patrón:** ¿algún ejercicio no hace lo que dice su patrón o su descripción? ¿Cuál? |
| E2 | **Función de la sesión:** en las sesiones PUSH, PULL y LEGS, ¿todos los ejercicios pertenecen a esa función? ¿Cuál sobra o falta? |
| E3 | **Tracción:** ¿las sesiones de espalda (PULL o tren superior) incluyen al menos un ejercicio real de tracción? ¿Es suficiente? |
| E4 | **Volumen por sesión:** ¿hay alguna sesión con demasiadas series para un mismo grupo? |
| E5 | **Duración reducida:** si la sesión dura bastante menos que el tiempo disponible, ¿es aceptable con ese contenido? |
| E6 | **Después del dolor** (solo fichas con «ejercicios excluidos por dolor previo»): ¿la sesión es razonable sin esos ejercicios? |
| E7 | **Fuerza en LOSE_WEIGHT** (solo esas fichas): ¿la distribución de fuerza entre grupos grandes es aceptable? ¿Falta algún grupo? |

## 5. Fichas de diagnóstico (D01–Dnn)

> «Con ese material y esa frecuencia, el sistema **no generó** la semana porque no encontró ningún ejercicio de tracción. A la vista de los ejercicios disponibles:
> - ¿es pertinente no generar la semana? Sí / No.
> - Si no lo es, ¿qué sesión de espalda propondría con **solo** esos ejercicios?
> - ¿Qué material mínimo recomendaría?»

## 6. Preguntas globales (una vez, al final)

- **G1.** ¿Pertenecen TRUNK_EXTENSION y ELBOW_FLEXION a una sesión de tracción? ¿Y SHOULDER_RAISE a una de empuje? ¿Debe contar el core como trabajo resistido del tren inferior?
- **G2.** ¿Las palancas en barra (*back lever*, *front lever*) son trabajo de espalda?
- **G3.** ¿Basta un único ejercicio de tracción (remo con toalla en un anclaje firme) como tracción de la semana?
- **G4.** ¿Es aceptable que LOSE_WEIGHT tenga semanas sin fuerza en algún grupo grande?
- **G5.** ¿Es aceptable sustituir un ejercicio de fuerza por cardio para alcanzar la duración mínima? (Contexto: GENERAL_FITNESS principiante de 1 día.)

## 7. Criterios de consenso (propuesta; requieren aprobación)

- **Plan aceptable:**
  - media ≥ 3 en los criterios 1–11;
  - ningún «Seguridad» ≤ 2;
  - «Sí» o «Con cambios» en la pregunta de duración;
  - ningún «No» en E2 ni en E3.
- **Con dos evaluadores:**
  - acuerdo si la diferencia es ≤ 1 por criterio;
  - si es mayor, consenso con el motivo registrado;
  - se informa el porcentaje de acuerdo exacto y en ±1.
- **Decisión sobre C2-D + EF:** aceptable si al menos el **80 %** de los planes con sesión corta publicada por C2-D son aceptables y ninguno tiene «Seguridad» ≤ 2.
- **Registro:**
  - código del evaluador, cualificación, fecha y confirmación;
  - las fichas no se editan;
  - **no se rellena ninguna en su nombre**.

## 8. Plantilla de respuestas

Archivo: `PLANTILLA_RESPUESTAS_EVALUADOR.csv`.

```
codigo,evaluador,fecha,c1_estructura,c2_series_reps,c3_descansos,c4_ejercicios,c5_distribucion,c6_recuperacion,c7_volumen,c8_intensidad,c9_seguridad,c10_duracion,c11_nivel,c12_observaciones,completa_pese_a_menor_duracion,e1_patron,e2_funcion,e3_traccion,e4_volumen_sesion,e5_duracion_reducida,e6_tras_dolor,e7_fuerza_lose_weight,observaciones
```
