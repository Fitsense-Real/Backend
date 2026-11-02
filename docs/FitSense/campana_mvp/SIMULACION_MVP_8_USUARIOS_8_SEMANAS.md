# Campaña MVP · 8 usuarios sintéticos × 8 semanas con IA real

**Ejecución:** 30–31/10/2026. **Rama:** `campana-8-usuarios`, desde `generation` (`9832f33`). **Simulador:** `orchestation/CampanaMvp8UsuariosIT`.

**Cómo se hizo:**
- PostgreSQL efímero (Testcontainers). **No se tocaron Neon, Railway, Flutter ni `main`.**
- Replicate real, modelo `openai/gpt-5-structured` con `gpt-5-mini` y razonamiento medio, a través del proxy del entorno.
- Servicios reales para generar, registrar, cerrar la semana, decidir, adaptar, validar, publicar y progresar.
- Las sesiones y omisiones se registraron con los mismos comandos y controladores que usa la app. **Ninguna adherencia se introdujo a mano.**
- Progresión encendida **solo en este contexto aislado**; en `application.properties` sigue en `false`.
- Calendario controlado: S1 = 2/11/2026 y S8 = 21–27/12/2026, sin cambiar el reloj global.

**Límites de la interpretación:**
- Son personas **sintéticas**: no hay validación humana.
- Los resultados de adherencia **no demuestran mejora causal**.

## 1. Respuestas del criterio de cierre

| Pregunta | Respuesta |
|---|---|
| ¿Cuántos usuarios obtuvieron plan inicial? | **7 de 8**. U06 no: la IA falló (una propuesta rechazada y después un timeout del proveedor) y el respaldo de reglas tampoco pudo, por un conflicto de políticas (§5) |
| ¿Cuántas de las 64 semanas terminaron con plan válido? | **56 de 64**. Las 56 de los 7 usuarios con plan se publicaron y pasaron V1–V25 y V8C con sus exenciones autorizadas. Las 8 que faltan son de U06 |
| ¿Cómo evolucionó la adherencia? | Tabla de §3 |
| Adaptaciones intentadas y aplicadas | **14 ciclos con orden de adaptar:** 7 APLICADA, 4 SIN_EFECTO (una de ellas CONSERVAR, sin IA) y 3 RESPALDO. **Ninguna** quedó NO_FACTIBLE ni RECHAZADA: siempre se publicó la semana |
| Causas que produjeron cambios | Dolor (4 de 4 aplicadas), menos tiempo habitual (1 de 3), fatiga (1 de 2) y desmotivación (1 de 2). Dificultad y cambio de horario no produjeron cambios (RESPALDO y SIN_EFECTO) |
| Llamadas reales a Replicate | **45**: 17 de generación y 28 de adaptación. 44 SUCCEEDED y 1 TIMEOUT. Tope de 80: no se alcanzó |
| Propuestas rechazadas y por qué | **24** intentos fallidos (§4) |
| Progresiones | **13 pasos publicados:** 11 de progresión y 2 de recuperación, todos ≤ 5 % (máximo 3,96 %) y bajo el techo. 0 pasos bloqueados registrados. En los demás 28 ciclos NO_CHANGE se mantuvo la dosis (§6) |
| Ejercicios retirados | **5:** 4 por dolor (1152, 517, 177, 1103), sustituidos, y 1 por ACORTAR (864, en U08 S7) |
| Reincorporaciones | **0 en la campaña:** solo 864 era reincorporable y la retirada fue en S7. **1 en la prueba focalizada** (§7) |
| Errores y correcciones | §8 y `HALLAZGOS_Y_CORRECCIONES_MVP.md`. **Ningún defecto de código del backend.** Hay un conflicto de políticas (pendiente de decisión), una etiqueta del simulador corregida y cobertura nueva para 60 minutos |
| Límites que impidieron cambios | V8C bloqueó **10 subidas de dosis de la IA, en 4 ciclos**. También actuaron V17 (minutos declarados), V24, V20, V9, V12 y el patrón de sustitución. Los 4 intentos de cambiar el perfil se rechazaron (422) |
| Tiempo y coste de IA | 45 llamadas, **44 min de espera acumulada** (media de 59 s y máximo de 172 s), 2586 s de cómputo, 1,36 M tokens de entrada y 0,27 M de salida. **≈ US$ 0,87** con la tarifa pública de GPT-5 mini; la cifra oficial es la del panel de Replicate. La campaña duró 13 min con 4 hilos |
| Qué sigue sin validar | §9 |

## 2. Perfiles y sesiones programadas

| ID | Objetivo · nivel | Días declarados | Sesiones programadas | Entorno | Min | Plan inicial |
|---|---|---|---:|---|---:|---|
| U01 | GAIN_MUSCLE · intermedio | mar, mié | 2 (UPPER/LOWER) | casa: mancuernas, bandas | 45 | reglas, tras 3 propuestas de IA rechazadas |
| U02 | LOSE_WEIGHT · principiante | sáb, dom | 2 (UPPER/LOWER) | casa: peso corporal y banda | 30 | reglas, tras 3 rechazadas |
| U03 | INCREASE_STRENGTH · avanzado | lun–jue | 4 (U/L/U/L) | gimnasio completo | 60 | IA, en el 3.er intento |
| U04 | GENERAL_FITNESS · principiante | sáb | 1 (FULL_BODY) | casa: peso corporal | 30 | reglas, tras 3 rechazadas |
| U05 | GENERAL_FITNESS · intermedio | lun–dom (7) | **6** (PPL ×2; el jueves libre) | gimnasio, sin impacto, rodillas ni sobre la cabeza | 30 | IA, 1.er intento |
| U06 | GAIN_MUSCLE · avanzado | lun, mié, jue, sáb, dom | — | gimnasio completo | 60 | **SIN PLAN** |
| U07 | LOSE_WEIGHT · intermedio | lun, mar, mié, vie, sáb, dom | 6 (PPL ×2) | gimnasio sin barra, smith, kettlebell ni ez | 45 | IA, 1.er intento |
| U08 | INCREASE_STRENGTH · principiante | lun, mié, vie | 3 (FULL_BODY) | casa: mancuernas, bandas y banco | 45 | IA, 1.er intento |

- Cada perfil declaró `daysPerWeek` igual a sus días disponibles. **No se cambió ningún día, nivel ni objetivo.**
- U05 recibe 6 sesiones y no 7 porque el tope es `MAX_SESSIONS_OTHERS = 6`, una política explícita. El motor no obliga a entrenar todos los días disponibles.
- Ningún perfil recibió equipamiento no declarado: 0 violaciones en las comprobaciones de V1 y en las independientes.

## 3. Adherencia (oficial del backend, Σ A_s / m)

| Usuario | Sesiones/sem | S1 | S2 | S3 | S4 | S5 | S6 | S7 | S8 | Media |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| U01 | 2 | 100 | 50 | 100 | 100 | 100 | 90 | 100 | 100 | 92,5 |
| U02 | 2 | 100 | 66,66 | 50 | 100 | 50 | 100 | 50 | 100 | 77,1 |
| U03 | 4 | 100 | 100 | 95,83 | 100 | 100 | 100 | 85,72 | 100 | 97,7 |
| U04 | 1 | 100 | 100 | 100 | 0 (sin registro) | 0 | 100 | 0 | 100 | 62,5 |
| U05 | 6 | 100 | 100 | 80 | 70 | 100 | 80 | 100 | 100 | 91,2 |
| U06 | — | — | — | — | — | — | — | — | — | — |
| U07 | 6 | 100 | 66,67 | 83,33 | 100 | 83,33 | 100 | 100 | 100 | 91,7 |
| U08 | 3 | 100 | 100 | 83,33 | 100 | 95,83 | 100 | 66,67 | 100 | 93,2 |

**Fórmula de la orden frente a la del backend:**
- La orden calcula (completas + 0,5 × parciales) / programadas; el backend, Σ A_s / m (`ADHERENCIA.md`, aprobada).
- Coinciden en **48 de 56** semanas. Difieren en 8: las que tienen una parcial que no queda al 50 %, o una sesión completada con un ejercicio dejado por dolor (A_s de 0,75–0,83, que la orden cuenta como 1). Ejemplo: U02 S2 da 75 con la orden y 66,66 en el backend.
- **No se cambió la fórmula oficial.**

**Sin registro y omisión:**
- **Sin registro** (U04 S4): la observada es NULL y la decisión, INSUFFICIENT_DATA, sin adaptar. **No** se confundió con una omisión.
- **Omisión declarada:** cuenta 0 en ambas.

**Riesgo:** 46 semanas LOW, 8 MODERATE y 2 HIGH (las de U04 con 0 %).

## 4. Adaptaciones con IA real

Detalle en `ADAPTACIONES_IA_REAL.csv`.

| Usuario · semana | Causa | Intervención | IA | Resultado | Qué pasó |
|---|---|---|---:|---|---|
| U01 S2 | menos tiempo habitual (70 % de la sesión) | ACORTAR | 2 | SIN_EFECTO | En el 1.er intento la IA retiró y bajó dosis a la vez (V8C: una cosa o la otra); en el 2.º no cambió nada. Se conserva |
| U01 S6 | dolor en 1152 | SEGURIDAD | 1 | **APLICADA** | 1152 sustituido; el volumen no sube |
| U02 S2 | parcial por fatiga | REVISAR | 2 | SIN_EFECTO | Rechazos por V17 y V24 de cardio |
| U02 S3 | demasiado difícil | VARIANTE | 3 | RESPALDO | Sustituciones de otro patrón (V8C) y V17 |
| U02 S5 | desmotivación | INVESTIGAR | 3 | RESPALDO | **La IA subió dosis** (V8C), además de V17, V20 y V24 |
| U02 S7 | imprevisto aislado | **CONSERVAR** | **0** | SIN_EFECTO | ADAPT-2: sin contexto suficiente no se acorta y no se llama a la IA |
| U03 S3 | dolor en 517 | SEGURIDAD | 2 | **APLICADA** | 1.er intento con un ejercicio sin dosis de su capacidad (V24) |
| U04 S5 | menos tiempo habitual, sesión única | ACORTAR (EXPANDED) | 3 | RESPALDO | V9 (baja de 2 series) y V17. Con 16 min no hubo propuesta válida: se publica la original |
| U04 S7 | desmotivación | INVESTIGAR (EXPANDED) | 2 | **APLICADA** | 1.er intento: **subió dosis** (V8C) |
| U05 S3 | dolor en 177 y tiempo en otra sesión | SEGURIDAD (localizada) | 2 | **APLICADA** | El dolor tiene prioridad (corrección de la auditoría). La sesión con falta de tiempo no se señala: la adaptación localizada lleva solo la de dolor. 1.er intento: subió dosis y volumen ante dolor (V8C) |
| U05 S4 | 3 parciales (fatiga ×2, tiempo) | REVISAR (la peor) | 2 | **APLICADA** | Reduce dosis de 508, 709 y 1325. Recuperadas después en S6 y S8 (§6) |
| U07 S2 | 2 omisiones por cambio de horario | REPROGRAMAR | 2 | SIN_EFECTO | 1.er intento: dos sesiones el mismo día (V12) |
| U08 S5 | dolor en 1103 | SEGURIDAD | 2 | **APLICADA** | 1.er intento: **subió dosis** (V8C) |
| U08 S7 | menos tiempo habitual | ACORTAR | 2 | **APLICADA** | Retira 864 (relleno); la sesión no supera los minutos declarados |

**Sin adaptación, por política:**
- Semanas con adherencia observada ≥ 80 %: la fatiga de U07 S3 y U05 S6, la dificultad de U08 S3 y la desmotivación de U07 S5.
- Con dolor se adaptó siempre, también con adherencia buena (U01 S6, U03 S3 y U08 S5, con 90–96 %).

**Rechazos por regla en los 24 intentos fallidos** (generación y adaptación). Un intento puede tener varios rechazos:

| Regla | Qué detectó | Intentos con el rechazo | Problemas |
|---|---|---:|---:|
| V24 | dosis que no corresponde a ninguna capacidad | 12 | 33 |
| V17 | minutos declarados distintos del contenido | 9 | 9 |
| V20 | sesión por debajo del mínimo | 5 | 7 |
| V15 | grupos del enfoque | 5 | 6 |
| V25 | capacidad principal omitida | 4 | 4 |
| V8C | **la IA sube dosis** | 4 | 10 |
| V8C | sustitución de otro patrón | 2 | 4 |
| V8C | ante dolor, el volumen sube | 2 | 2 |
| V8C | retira y cambia la dosis a la vez | 1 | 1 |
| V23 | patrones obligatorios | 2 | 2 |
| V18 | días seguidos | 2 | 2 |
| División | fechas y enfoques | 2 | 2 |
| V9 | baja de series | 1 | 1 |
| V12 | dos sesiones el mismo día | 1 | 1 |

**Resultado de las sesiones adaptadas** (`resultado_sesion_adaptada.sql`): 7 sesiones adaptadas y publicadas.
- 6 COMPLETADAS y 1 PARCIAL_VALIDA (U05 S4, la que había tenido dolor).
- Ninguna se volvió a omitir por la misma causa.
- **No es una prueba causal.**

## 5. Usuario sin plan: U06 (conflicto de políticas)

**Qué pasó en la campaña:**
- La IA propuso un plan que V15 y V24 rechazaron, y el 2.º intento terminó por timeout del proveedor (172 s).
- El respaldo de reglas tampoco publicó: V15 rechazó el día LEGS (solo `upper legs`) y V20 el domingo LOWER_BODY (2142 s frente a 2520 s).

**Reproducción determinista:** `RespaldoSesentaMinutosIT`, sin IA.
- **Todas** las 24 combinaciones de GAIN_MUSCLE con 5 o 6 días de 60 min fallan (3 niveles × 4 patrones de días × gimnasio/casa).
- Los otros tres objetivos pasan 72/72.

**Causa: la aritmética de las políticas aprobadas.**
- 4D v3.1 fija la dosis de GAIN_MUSCLE sin ampliación: estructural 3×10/90 s ≈ 364 s y resistido de zona pequeña 2×13/90 s ≈ 262 s.
- 4D v3.1 fija también un tope de 7 colocaciones a 60 min.
- V20 exige 2520 s, y V15 dos grupos y, en LOWER_BODY, ninguno por encima de la mitad.

| Día | Mínimo para V15 | Duración | Piso V20 |
|---|---|---:|---:|
| LEGS o PULL | 6 estructurales + 1 de otro grupo | 6 × 364 + 262 = **2448 s** | 2520 s |
| LOWER_BODY | ≤ 3 de `upper legs` + 4 pequeños | 3 × 364 + 4 × 262 = **2142 s** | 2520 s |

**Ninguna composición válida llega al mínimo. No es un fallo del motor ni de la IA.** Cambiarlo exige modificar una política aprobada (4D v3.1, V15 o V20), así que queda **PENDIENTE DE DECISIÓN** (`HALLAZGOS_Y_CORRECCIONES_MVP.md`, H1). No se modificó ningún umbral.

## 6. Progresión (solo en el entorno aislado)

`PROGRESION_Y_REINCORPORACION.csv`:

| Usuario | Paso | Ejercicio | Dosis | Volumen (base, techo) | % |
|---|---|---|---|---|---:|
| U01 S4 | PROGRESION | 1152 | 3×10 → 3×11 | 150 → 153 (150, 165) | 2,00 |
| U01 S5 | PROGRESION | 134 | 2×13 → 2×14 | 168 → 170 (168, 184) | 1,19 |
| U01 S8 | PROGRESION | 1155 | 3×10 → 3×11 | 150 → 153 (150, 165) | 2,00 |
| U03 S2 | PROGRESION | 887 | 2×10 → 2×11 | 132 → 134 (132, 145) | 1,52 |
| U03 S5 | PROGRESION | 1018 | 3×13 → 3×14 | 210 → 213 (210, 231) | 1,43 |
| U03 S6 | PROGRESION | 556 | 3×10 → 3×11 | 120 → 123 (132, 145) | 2,50 |
| U04 S2 | PROGRESION | 1262 | 2×15 → 2×16 | 89 → 91 (89, 97) | 2,25 |
| U05 S2 | PROGRESION | 508 | 3×18 → 3×19 | 106 → 109 (106, 116) | 2,83 |
| U05 S6 | RECUPERACION | 508 | 3×18 → 3×19 | 98 → 101 (106, 116) | 3,06 |
| U05 S8 | RECUPERACION | 709 | 2×8 → 2×10 | 101 → 105 (106, 116) | 3,96 |
| U07 S7 | PROGRESION | 341 | 3×6 → 3×7 | 96 → 99 (96, 105) | 3,13 |
| U07 S8 | PROGRESION | 354 | 3×6 → 3×7 | 96 → 99 (96, 105) | 3,13 |
| U08 S2 | PROGRESION | 500 | 2×13 → 2×14 | 150 → 152 (150, 165) | 1,33 |

**Lo comprobado ciclo a ciclo**, sin violaciones:
- **Criterio de dos semanas:** el primer paso llega en S2, tras S1 y la semana inicial completas.
- **La adaptación tiene prioridad:** nunca hubo paso en un ciclo con orden de adaptar.
- **Un solo paso por ciclo.**
- **Límites:** ≤ 5 % por paso (≤ 10 % la recuperación); techo ≤ 1,10 × base.
- **La IA nunca progresó:** sus 10 subidas de dosis, en 4 ciclos, fueron rechazadas.
- **Recuperación de U05:** recupera las dosis que la IA había reducido por fatiga, en sesiones completas y no en las que tuvieron fatiga en S6.
- **Mantenimiento:** en los demás ciclos NO_CHANGE, porque no se cumplían las dos semanas, había dolor o fatiga en la sesión, o un paso reciente.

**Compatibilidad con V24:**
- El backend publica los pasos con la dosis autorizada por la progresión (D-P3: +1 repetición).
- La validación independiente del simulador, que no conoce esas autorizaciones, los marca como V24.
- Lo mismo ocurre con las reducciones autorizadas por REVISAR o INVESTIGAR y con la sesión acortada que hereda U08 S8 (D8). Son las 29 semanas «INVALIDA» de la columna `validacion_independiente`.
- **No se encontró ninguna publicación fuera de lo que autorizan las políticas.**

**Observación:** la reducción por desmotivación de U04 S7 dejó su única colocación de cardio con dosis reducida en S8 (V25 sin exención la marcaría). Lo permite la política de reducciones autorizadas y la recuperación (como en U05) puede restituirla, pero en un perfil de una sesión semanal significa una semana sin la dosis completa de cardio.

## 7. Reincorporación

**En la campaña: 0.**
- Los retirados por dolor **no** son reincorporables por diseño: el ejercicio que dolió no vuelve.
- El único relleno retirado por ACORTAR (864, U08 S7) no tenía semanas posteriores con un paso de progresión. La decisión 1(a) exige ese paso antes de reincorporar.
- **No se alteró la campaña para provocarla.**

**Prueba focalizada adicional** (backend real y datos sintéticos; la IA solo simula la orden de ACORTAR): `CopiaSinIaIT.reincorporacionYMantenimiento`, en verde el 31/10/2026.

| Orden | Ciclo | Hecho |
|---|---|---|
| 1 | ACORTAR | Retira el relleno 1324 de la sesión acortada; queda trazado en V32 con reserva PENDIENTE |
| 2 | Progresión | 1006, 912 y 494 (3×6 → 3×7 → 3×8), un paso por ciclo; la sesión acortada progresa dentro de la reserva |
| 3 | Mantenimiento | Ciclo sin paso posible |
| 4 | Reincorporación | 1324 vuelve con su **dosis inicial exacta** (3×120 s / 60 s), al final de la sesión: volumen 72 → 84, techo 85. Sigue elegible y sin dolor ni restricciones. No coincide con una progresión en el mismo ciclo |

Cumple los 8 puntos de la orden: retirada real, trazada, posterior a la progresión, elegible, sin bloqueo, dosis válida, bajo el techo y un solo paso por ciclo.

**Estos resultados son de la prueba focalizada, no de los 8 usuarios.**

## 8. Cambios de perfil del guion

U03 S5 (días), U03 S6 (equipamiento), U05 S5 (sin suelo) y U07 S6 (sin cable) **se intentaron por el contrato de la app**: `PUT` del perfil con el controlador real.

- Los 4 respondieron **422** y el perfil quedó intacto (regla aprobada el 30/10/2026: el perfil es fijo durante el estudio).
- Las 4 personas siguieron con su plan y sus semanas se publicaron con normalidad.
- No se forzó nada por SQL.

## 9. Lo que sigue sin validar

1. **GAIN_MUSCLE de 5–6 días a 60 min:** no tiene plan, ni de la IA ni del respaldo (H1).
2. **Reincorporación con IA real:** no hubo un caso de ACORTAR seguido de progresión dentro de las 8 semanas.
3. **Acortar cuando el tiempo es poco:** la IA no lo consigue de forma fiable. De 3 casos, 1 quedó SIN_EFECTO y 1 en RESPALDO; con 16 min no fue posible. La persona recibe la sesión original sin acortar.
4. **REPROGRAMAR_DIAS:** quedó SIN_EFECTO; no hay ningún caso aplicado con IA real.
5. **Dificultad excesiva** con adherencia ≥ 80 % (U08 S3) no produce adaptación. Es lo que dice la política vigente; la orden lo plantea como caso a observar.
6. **Validación humana** de los planes y de los ejercicios sustituidos: no la hay.
7. **Coste:** es una estimación de tokens; la cifra oficial está en el panel de Replicate.

## 10. Reproducir

```
# ensayo seco (sin IA ni coste) y campaña real (Replicate, coste)
mvn verify -Pintegration -Dit.test=CampanaMvp8UsuariosIT -Dfitsense.campana.mvp=true -Dfitsense.campana.ia=false
mvn verify -Pintegration -Dit.test=CampanaMvp8UsuariosIT -Dfitsense.campana.mvp=true
# conflicto de GAIN_MUSCLE a 60 min (sin IA)
mvn verify -Pintegration -Dit.test=RespaldoSesentaMinutosIT -Dfitsense.pendientes=true
```

La IA no es determinista: otra ejecución real dará propuestas distintas. Las respuestas originales de esta están en `evidencia/predicciones/` (45 archivos, sin secretos), y la traza completa en `evidencia/eventos.log` y `evidencia/llamadas.jsonl`.
