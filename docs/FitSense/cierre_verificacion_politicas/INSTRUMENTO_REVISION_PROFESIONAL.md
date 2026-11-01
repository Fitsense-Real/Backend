# Instrumento de revisión profesional de planes

**Estado: listo para usar. EVALUACIÓN PENDIENTE: no hay ningún evaluador registrado y no se ha realizado ninguna revisión humana.** Este documento no contiene evaluaciones.

## 1. Material

| Archivo | Contenido | Quién lo ve |
|---|---|---|
| `MUESTRA_REVISION_PROFESIONAL.csv` | Planes de la muestra con código M01–Mnn. Por ejercicio: objetivo, nivel, días, minutos configurados, equipo, restricciones, fecha, enfoque, duración estimada de la sesión, % de M, ejercicio, zona, patrón, slot, clase, series, repeticiones, segundos y descanso | Evaluador |
| `CLAVE_MUESTRA_NO_ENTREGAR_AL_EVALUADOR.csv` | Código → perfil, camino (C1, C2-C o C2-D) y motivo de selección | Solo el equipo, tras la evaluación |

- **Evaluación ciega:** el evaluador no sabe qué planes vienen de C1 y cuáles de C2. El orden se mezcla con una semilla fija.
- **Datos sintéticos:** todos los perfiles son sintéticos, sin datos personales.
- **Composición de la muestra:**
  - un plan por cada objetivo y nivel (12), con 30, 45, 60 y 75 min y 2, 3, 4 y 6 días;
  - 4 casos recuperados por C2, en las dos variantes (regenerado y mejor esfuerzo);
  - 2 con restricciones;
  - 2 de LOSE_WEIGHT con el menor cardio programado;
  - 2 con el volumen semanal más alto;
  - 2 sesiones cortas (30 min, peso corporal).

## 2. Perfil del evaluador

- Profesional del ejercicio cualificado: licenciatura en Ciencias del Deporte o del Movimiento Humano, o certificación ACSM-EP o NSCA-CSCS, o equivalente.
- Al menos 2 años de prescripción de fuerza con adultos.
- Sin relación con el desarrollo del motor.
- **Idealmente dos evaluadores independientes.**

## 3. Ficha por plan (una por código)

Escala de 1 a 5:
- 1 = inadecuado;
- 2 = deficiente;
- 3 = aceptable con cambios;
- 4 = adecuado;
- 5 = muy adecuado.

Además, «NS» si no se puede valorar con la información dada.

| # | Criterio | Pregunta guía | 1–5 / NS | Comentario |
|---:|---|---|---|---|
| 1 | Estructura y objetivo | ¿La semana responde al objetivo declarado? | | |
| 2 | Series y repeticiones | ¿Son adecuadas para el objetivo y el nivel? | | |
| 3 | Descansos | ¿Son adecuados (p. ej. 150 s en fuerza, 90 s en hipertrofia)? | | |
| 4 | Selección de ejercicios | ¿Los ejercicios son pertinentes, variados y no redundantes? | | |
| 5 | Distribución semanal | ¿El reparto por días y enfoques es razonable? | | |
| 6 | Recuperación | ¿Hay suficiente recuperación entre sesiones del mismo grupo? | | |
| 7 | Volumen | ¿El volumen semanal por grupo es suficiente y no excesivo? | | |
| 8 | Intensidad | ¿La intensidad prescrita está bien orientada, sabiendo que no hay carga ni RIR? | | |
| 9 | Seguridad | ¿Hay algún riesgo evidente para un adulto sano del nivel indicado? | | |
| 10 | Duración | ¿La duración estimada es adecuada para el tiempo configurado y el objetivo? | | |
| 11 | Nivel | ¿Es adecuado para el nivel (principiante, intermedio, avanzado)? | | |
| 12 | Observaciones | Correcciones sugeridas (ejercicio, dosis o día) | — | |

**Pregunta específica** (en todas las fichas; es crítica en las que el evaluador perciba duración menor que la configurada):

> **¿La sesión es suficientemente completa para el objetivo del usuario aunque dure menos que el tiempo configurado?** Sí / No / Con cambios. ¿Qué le falta?

**Preguntas globales** (una vez, al final):
- G1. ¿Es suficiente el cardio programado en LOSE_WEIGHT si fuera la única actividad aeróbica? ¿Qué mínimo recomendaría?
- G2. ¿Recomendaría 180 s de descanso en los básicos pesados de fuerza para intermedios y avanzados?
- G3. ¿Es aceptable que LOSE_WEIGHT tenga semanas sin fuerza para algún grupo grande?
- G4. ¿Es razonable no repetir el mismo grupo grande en días consecutivos (V18)?

## 4. Criterios de consenso

- **Plan aceptable:** media ≥ 3 en los criterios 1–11, ningún criterio ≤ 2 en «Seguridad» y respuesta «Sí» o «Con cambios» a la pregunta específica.
- **Con dos evaluadores:**
  - acuerdo si la diferencia es ≤ 1 punto por criterio;
  - si es > 1, se discute el plan y se registra la puntuación de consenso y el motivo;
  - se informa el porcentaje de acuerdo exacto y en ±1.
- **Decisión sobre C2:** C2 se considera aceptable si al menos el 80 % de los planes recuperados por C2 son «aceptables» y ninguno tiene «Seguridad» ≤ 2. Estas cifras son una propuesta y requieren aprobación.
- **Registro:**
  - nombre o código del evaluador, cualificación, fecha y firma o confirmación;
  - las fichas se guardan sin editar.
  - **No se rellena ninguna ficha en nombre del evaluador.**

## 5. Plantilla CSV de respuestas

```
codigo_muestra,evaluador,fecha,c1_estructura,c2_series_reps,c3_descansos,c4_ejercicios,c5_distribucion,c6_recuperacion,c7_volumen,c8_intensidad,c9_seguridad,c10_duracion,c11_nivel,completa_pese_a_menor_duracion,observaciones
```
