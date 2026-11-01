# Auditoría del cardio y de la fuerza en LOSE_WEIGHT

**Fuente:** `LOSE_WEIGHT_FUERZA_Y_CARDIO.csv`. Son los 230 planes válidos de LOSE_WEIGHT del camino vigente (C1), medidos con `MetricaAerobica` y `VolumenSemanal`.

**No se ha cambiado la dosis de cardio ni ninguna obligación de fuerza.**

## 1. Cardio programado

| Medida | Mínimo | Media | Máximo |
|---|---:|---:|---:|
| Minutos de **trabajo** por semana (series × segundos) | 9,0 | 41,9 | 120,0 |
| Minutos de **bloque** por semana (trabajo + pausas de 60 s) | 13,0 | 57,0 | 160,0 |

| Sesiones/semana | Principiante (bloque) | Intermedio / avanzado (bloque) |
|---:|---:|---:|
| 1 | 13 | 16 |
| 2 | 26 | 32 |
| 3 | 39 | 48 |
| 4 | 66 (máx. 85) | 72 (máx. 96) |
| 6 | — | 105 (máx. 160) |

- Planes con ≥ 150 min de bloque: **4 de 230**, todos con 6 sesiones.
- Planes con ≥ 75 min de bloque: 62.

## 2. Qué se puede afirmar y qué no

**Se puede afirmar:** el cardio **programado** por FitSense está por debajo de 150 min/semana en casi todos los planes. Esa es la cifra con dosis-respuesta para el peso según el resumen del ACSM 2024 [S04, RESUMEN].

**No se puede afirmar:**
1. **Que la persona haga menos de lo recomendado.** La OMS cuenta toda la actividad (caminar, transporte, trabajo, deporte), y FitSense no la conoce.
2. **Que los bloques sean «vigorosos».** No se mide la intensidad. Por tanto, la equivalencia de 75 min vigorosos no se puede aplicar, y además es una inferencia de los rangos de la OMS (ver `REVISION_CIENTIFICA_ACTUALIZADA.md` §6).
3. **Que se ejecute lo programado.** El laboratorio solo mide lo planificado.

**Matiz de la clasificación.** LW2 sigue siendo **E**, pero solo **si el cardio de la app es la única actividad aeróbica**. No es un problema de seguridad.

## 3. Fuerza en LOSE_WEIGHT

| Indicador | Valor |
|---|---|
| Planes con al menos un grupo grande sin series en la semana | **148 de 230** |
| Grupo más afectado | Hombros (78 planes) |
| Los cuatro grupos grandes a cero | 40 planes, casi todos con sesiones de 30 min |
| Por duración | 30 min: 60; 45 min: 34; 60 min: 38; 75 min: 16 |
| Por equipo | Peso corporal 68; gimnasio 47; mancuernas y bandas 33 |
| Con restricciones o dolor | 14 de 148: **no es, sobre todo, efecto de las restricciones** |

**Causa:** es la estructura de 4A v6.
- En LOSE_WEIGHT, la fuerza es **secundaria** y V23 no exige los 4 patrones.
- Con cardio en todas las sesiones y sesiones cortas, el motor coloca pocos ejercicios de fuerza y no siempre reparte los grupos.

**¿Hay alternativa dentro de las reglas vigentes?** Se probó a añadir un ejercicio del grupo que falta, con su dosis base aprobada, en una sesión con hueco, validando con **todo** el validador vigente al 70 %.

| Resultado | Planes |
|---|---:|
| **Sí** (alternativa válida) | **104 de 148** |
| No (sin candidato admisible del grupo o sin hueco por techo o tope) | 44 |

**Valoración**
- La OMS recomienda fortalecer todos los grandes grupos ≥ 2 d/semana a todos los adultos [S05, RESUMEN]. En un programa combinado es una **brecha relevante**, aunque no de seguridad.
- En 104 planes se podría cerrar **con el generador** (preferencia por el grupo no cubierto al añadir fuerza secundaria) **sin cambiar 4A, 4C ni 4D**.
- **No se implementa:** cambia la composición de planes válidos y requiere aprobación (D-07 de la auditoría anterior). La propuesta queda en `DECISION_FINAL_C2_V4_CARDIO.md`.

## 4. ¿Hay que aumentar el cardio programado?

**Todavía no.** Primero se mide lo programado y lo ejecutado, y se informa sin juzgar (`PROPUESTA_METRICAS_AEROBICAS.md`). Alargar los bloques:
- cambia la dosis 4D;
- quita tiempo a la fuerza, que ya es la brecha del §3;
- puede reabrir V20 y V24.

Debe decidirse con datos reales de actividad y con revisión profesional (pregunta global G1 del instrumento).
