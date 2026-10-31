# Campaña integral de estrés del MVP: 8 usuarios × 10 semanas

**Fecha:** 31/10/2026.

**Rama y base:** rama `campana-estres`, desde `generation` (`22fb709`, con el PR #14 fusionado y CI en verde).

**Instrumento:** `CampanaMvp8UsuariosIT` con `-Dfitsense.campana.conjunto=estres`.
- Usa los servicios reales: generación, ejecución por los comandos de la app, ciclo semanal, adaptación, progresión y validadores.
- Corre sobre **PostgreSQL efímero (Testcontainers)**.
- La URL efectiva se comprueba al empezar y se registra en `eventos.log`. Si no es local, la campaña se detiene.
- No se usaron ni Neon ni Railway.

**Evidencia:**
- `evidencia/real/` (con IA real) y `evidencia/ensayo_seco/` (sin IA, la comprobación previa).
- Las 20 predicciones completas, sin credenciales, están en `evidencia/real/predicciones/`.

## 1. Fase 0: verificación

| Comprobación | Resultado |
|---|---|
| Commit de partida | `22fb709` (PR #14), CI en verde |
| Límites de sesiones | `PlanGenerationContext.MAX_SESSIONS_BEGINNER = 4` y `MAX_SESSIONS_OTHERS = 6`, aplicados en `effectiveDaysPerWeek()`. V2 exige **exactamente** ese número en cada borrador (IA, reglas, adaptación y copia) |
| Disponibilidad | El perfil admite de 1 a 7 días (`ck_profile_days_per_week`) y de 15 a 180 minutos |
| Replicate | `gpt-5-mini`, esfuerzo `medium`, `prefer-wait-seconds = 20`, a través del proxy del entorno |
| Presupuesto | 58 llamadas acumuladas de 80 → **22 disponibles** |
| Aislamiento | URL efectiva `jdbc:postgresql://localhost:<puerto>/test` (Testcontainers) |

## 2. Perfiles y guion

El detalle está en `PERFILES_SINTETICOS.csv`. El guion de 10 semanas quedó fijado en el código (`usuariosDeEstres()`) antes de la ejecución real.

| Usuario | Objetivo | Nivel | Días | Min | Lugar y equipamiento | Restricciones | Guion |
|---|---|---|---:|---:|---|---|---|
| U01 | GAIN_MUSCLE | INTERMEDIATE | 2 (L, J) | 30 | Gimnasio completo | — | S2: jueves parcial por falta de tiempo (sin M); después cumple |
| U02 | GAIN_MUSCLE | ADVANCED | 5 | 60 | Casa: peso corporal, mancuernas y bandas | — | S3 fatiga parcial; S5 omite una por falta de motivación |
| U03 | LOSE_WEIGHT | BEGINNER | **7** | 30 | Casa: peso corporal y banda | Sin impacto ni colgarse | S2 sesión difícil; S3 dos parciales; S4 dos omisiones |
| U04 | LOSE_WEIGHT | INTERMEDIATE | 6 | 45 | Gimnasio sin barra | 3 bloqueados (1176, 1293, 1324) | S2 dos omisiones por cambio de horario; S5 imprevisto |
| U05 | INCREASE_STRENGTH | BEGINNER | **1** | 45 | Casa: mancuernas y bandas | — | S2 sesión difícil |
| U06 | INCREASE_STRENGTH | ADVANCED | 4 | 60 | Gimnasio | 3 bloqueados (495, 350, 1264); sin trabajo sobre la cabeza | S5 dolor en un ejercicio |
| U07 | GENERAL_FITNESS | BEGINNER | 1 | **15** | Casa: solo peso corporal | Sin impacto ni colgarse | S2 imprevisto; S3 sin registro |
| U08 | GENERAL_FITNESS | INTERMEDIATE | 3 | 45 | Gimnasio | 2 bloqueados (507, 878); sin arrodillarse | S2 dolor; S4 fatiga parcial |

- **Calendario:** del 2/11/2026 (S1) al 4/1/2027 (S10).
- **Única calibración**, hecha en el ensayo seco previo y no en la ejecución real: U04 pasó de una omisión por cambio de horario a dos. Con una, la semana quedaba en 83,3 % y no se adaptaba; con dos (66,7 %) se pide reprogramar.

## 3. Presupuesto y orden de ejecución

- **Primero las 8 generaciones iniciales:** una barrera impide empezar las semanas hasta que terminan todas. Cada operación reserva 3 llamadas y espera mientras otras tengan reservas en curso.
- **Sin presupuesto, la campaña sigue sin llamar.**
  - Cada intento sin llamada queda como `SIN_PRESUPUESTO`: V32 lo cuenta como intento y la contabilidad lo separa.
  - La API lo traduce como `RESPALDO_PROPUESTA_IA_INVALIDA` porque V32 registra intentos. **En esta campaña esos casos no son propuestas inválidas de la IA, sino falta de presupuesto.**

| | Llamadas reales | Coste estimado | Latencia de IA |
|---|---:|---:|---:|
| Generación inicial (8 usuarios) | 17 | US$ 0,473 | 1 638 s |
| Adaptación (U01 S2, U08 S2) | 3 | US$ 0,063 | 87 s |
| **Total** | **20 de 22** | **US$ 0,536** | 1 725 s |

**Total global:** 78 de 80 llamadas autorizadas.

**Adaptaciones sin presupuesto:** U03 S3 y S4, U05 S2 y U06 S5. Con IA real habrían necesitado como mucho **12 llamadas más**.

## 4. Resultados por usuario

| Usuario | Plan inicial (llamadas) | Semanas con plan | Sesiones (completas/parciales/omitidas) | Adherencia media | Órdenes de adaptación | Resultado | Progresiones |
|---|---|---:|---|---:|---|---|---:|
| U01 | **IA** (2) | 10/10 | 20 (19/1/0) | 97,5 % | S2 ACORTAR | **Aplicada con IA real: retira el relleno 416** | 7 |
| U02 | **IA** (3) | 10/10 | 50 (48/1/1) | 96,9 % | — (S3 88,6 %; S5 en el **80,00 % exacto**: no adapta) | Vuelta atrás tras la fatiga de S3 | 5 + 1 vuelta atrás |
| U03 | **Reglas** (3 intentos de IA rechazados) | 10/10 | 40 (35/3/2) | 90,0 % | S3 REVISAR, S4 INVESTIGAR | Sin presupuesto: se conserva | 0 |
| U04 | **IA** (1) | 10/10 | 60 (57/0/3) | 95,0 % | S2 REPROGRAMAR | **REPROGRAMACION_IMPOSIBLE, 0 llamadas** (6 días y 6 sesiones) | 5 |
| U05 | **IA** (1) | 10/10 | 10 (9/1/0) | 95,0 % | S2 VARIANTE | Sin presupuesto: se conserva | 4 |
| U06 | **IA** (3) | **9/10** | 36 (36/0/0) | 99,6 % | S5 SEGURIDAD (dolor) | Sin presupuesto: **NO_FACTIBLE_POR_SEGURIDAD**. S6 sin plan; el ciclo regenera **sin 151**, el que dolió | 6 |
| U07 | **Sin plan** (3 de IA y reglas rechazados) | **0/10** | — | — | — | Inviable (§5) | — |
| U08 | **IA** (1) | 10/10 | 30 (29/1/0) | 97,8 % | S2 SEGURIDAD (dolor) | **Aplicada con IA real** (2 llamadas: sustitución segura) | 5 |

- **Semanas con plan válido:** **69 de 80**. Faltan U07 (10) y U06 S6 (1).
- **Validación independiente:** todas las publicadas son válidas (32 VALIDA y 37 VALIDA_CON_AUTORIZACIONES), con **0 violaciones**.
- **Frecuencia:** 76 planes comprobados, **ninguno por encima del tope** (ver `CONTROL_FRECUENCIA_SEMANAL.md`).

## 5. Combinaciones inviables

**U07 (GENERAL_FITNESS principiante, 15 min, un día, solo peso corporal): sin plan.**
- **Motor de reglas:**
  - V15: la sesión necesita 2 grupos del enfoque.
  - V4: la composición mínima de sus capacidades dura **21 min**, con un techo de 17.
- **IA real (3 intentos):** V15, V23 (los cuatro patrones) y V4.
- **Comprobación determinista (`PerfilesLimiteIT`):**
  - LOSE_WEIGHT y GENERAL_FITNESS de 15 min no tienen plan.
  - GAIN_MUSCLE e INCREASE_STRENGTH sí lo tienen, con una sesión real de 15 min.
- **No se relajó V15, V20, V4 ni V25.** Es un conflicto de políticas, documentado para decidir (ver `CIERRE_CAMPANA_ESTRES_MVP.md`).

**Hallazgo relacionado:** antes de esta campaña, el motor de reglas **publicaba** LOSE_WEIGHT de 15 min con una sesión real de 19 min, declarando 17. Corregido (ver `ERRORES_ENCONTRADOS_Y_CORRECCIONES.md`).

## 6. Generación con IA real

| Usuario | Llamadas | Resultado | Rechazos de los intentos fallidos |
|---|---:|---|---|
| U01 | 2 | IA | 1.º: presupuesto 4D §5 (28,3 min de coste con 25,5 de presupuesto) |
| U02 | 3 | IA | 1.º: **3×13 innecesarias** (con 2×13 la sesión ya llegaba: 2529 s ≥ 2520); 2.º: V20 (faltaban 378 s) |
| U03 | 3 | **Reglas** | V15 (LOWER_BODY solo con `upper legs`), V4 (38 min con techo 35), V24 |
| U04 | 1 | IA | — |
| U05 | 1 | IA, **con una sola sesión** | — |
| U06 | 3 | IA | V15 (`waist` en UPPER_BODY; más de la mitad en un grupo), V20 |
| U07 | 3 | Sin plan | V15, V23, V4 |
| U08 | 1 | IA | — |

- **Aceptadas al primer intento:** 3 de 8 (U04, U05, U08).
- **Por IA en total:** 6 de 8 (con reintentos).
- **Por reglas:** 1. **Sin plan:** 1.
- Ninguna generación por respaldo cuenta como IA.

## 7. Adaptación con IA real

| Caso | Intervención | Llamadas | Resultado |
|---|---|---:|---|
| U01 S2 | ACORTAR (parcial sin M) | 1 | APLICADA: retira el relleno 416 (2×10) del jueves y deja 517, 390 y 1004 con su dosis |
| U08 S2 | SEGURIDAD_ANTE_DOLOR | 2 | APLICADA en el 2.º intento: sustitución segura |
| U04 S2 | REPROGRAMAR | **0** | REPROGRAMACION_IMPOSIBLE: factibilidad previa (ninguna fecha libre) |
| U03 S3 y S4, U05 S2, U06 S5 | REVISAR, INVESTIGAR, VARIANTE, SEGURIDAD | 0 (sin presupuesto) | Se conserva la semana. U06: NO_FACTIBLE y semana sin plan, sin publicar la sesión con dolor |

Ninguna adaptación subió dosis, tocó sesiones no señaladas ni añadió sesiones (V8C y V2).

## 8. Progresión y reincorporación

- **32 pasos de progresión y 1 vuelta atrás** (U02, tras la fatiga), todos del backend y con 0 llamadas.
- Cada paso respeta el 5 % de la sesión y el techo de 1,10 × base, y hay uno por ciclo como máximo (ver `PROGRESION_Y_REINCORPORACION.csv`).
- **Retirados:** 1 (U01, 416, en el ciclo S2 con IA real).
- **Reincorporados:** 0.
  - El jueves de U01 dio sus pasos (517 a 3×12 y 390 a 3×11 hasta S10), pero la reincorporación no llega dentro de las 10 semanas.
  - La prueba focalizada anterior la situó en el **ciclo 10 después de la retirada**; aquí la retirada fue en S2.
  - **No se adelantó nada.**
- **Reincorporados y entrenados dentro de las 10 semanas:** 0.

## 9. Lo que no se pudo ejercitar en la ejecución real

- **«Semana sin registro»:** era de U07, que no tuvo plan. Está cubierta por pruebas deterministas: `AdaptacionCompletaIT.vencidosAntesDelCiclo` (INSUFFICIENT_DATA, sin datos ≠ 0 %).
- **Adaptaciones de U03, U05 y U06:** sin presupuesto.
- **Reincorporación dentro de 10 semanas:** no es alcanzable con una retirada en S2.
