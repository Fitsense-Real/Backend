# Protocolo experimental congelado: 20 usuarios sintéticos × 3 sesiones

**Congelado antes de cualquier ejecución.** El commit que añade este archivo, `03_PERFILES_U01_U20.csv` y `04_ORACULO_INDEPENDIENTE.csv` precede a todos los resultados. Los cambios posteriores al protocolo, si los hay, se registran en un anexo con su motivo. Nunca se edita lo congelado.

## 1. Base

| Elemento | Valor |
|---|---|
| Código | `generation` `0974fb3eafc39e8b7397b4dbfe7c4405e4847440` (la etiqueta `backend-mvp-1.0` apunta a ese commit) |
| Migraciones | V1–V44, V44_1 y V44_2 (46). El catálogo es el de esas migraciones |
| Política de planificación | `PLAN-HIST-1` (valor por defecto). `allow-candidates=false`. Nada del PR #17 (C2-D, EF, V45) |
| Progresión | Apagada (`fitsense.adaptation.progression.enabled=false`, valor por defecto) |
| Base de datos | PostgreSQL 16 efímero (Testcontainers), creado vacío en cada ejecución. Sin Neon ni Railway |
| IA | **Etapa A:** `fitsense.ai.enabled=false` y sin token. Se usan los *beans* reales: ninguna IA simulada.<br>**Etapa C:** Replicate real (§7) |

## 2. Escenarios

- **20 usuarios:** 4 objetivos × 5 patrones. Los perfiles están en `03_PERFILES_U01_U20.csv`.
- **Perfil por objetivo:**
  - los 5 usuarios de un mismo objetivo comparten perfil, así que dentro de cada objetivo **el patrón es la única variable**;
  - el plan de cada usuario es distinto porque la semilla depende de su identificador.
- **Semilla del motor de reglas:** `userId × 1.000.003 + epochDay(2026-11-02)`.
  - Los usuarios se crean en orden, de U01 a U20, sobre una base vacía.
  - El `userId` real se registra en los resultados.
- **Semana medida:** del lunes 2026-11-02 al domingo 2026-11-08. Días disponibles: lunes, miércoles y viernes (S1, S2 y S3).
- **Semana siguiente:** la que empieza el 2026-11-09.

## 3. Procedimiento por usuario (servicios reales del backend)

| # | Paso | Cómo |
|---|---|---|
| 1 | Alta | `UserCommandService.handle(SignUpCommand)`, con zona `America/Lima` y consentimiento |
| 2 | Perfil | `UserProfileCommandService.handle(CreateUserProfileCommand)` |
| 3 | Plan inicial | `PlanningContextFacade.generateWeeklyPlan(userId, 2026-11-02, PlanAdjustment.none(), null, null)`: el mismo camino que la generación inicial del ciclo semanal |
| 4 | Ejecución | §4 |
| 5 | Cierre semanal | `WeeklyCycleService.runFor(userId, 2026-11-02, 2026-11-09)`.<br>Hace en orden: cerrar la semana, medir (`weekly_user_metrics`), decidir (`AdaptationOrderDecider`) y adaptar o copiar, con la evidencia V32 |
| 6 | Captura | Plan inicial, registros, métricas, decisión, fila V32, plan siguiente y diferencias por sesión |

## 4. Patrones de ejecución

| Código | Qué se hace |
|---|---|
| **H** | `StartWorkoutSessionCommand`.<br>Por cada ejercicio prescrito, `RecordSessionExerciseCommand` con la dosis completa: reps totales = series × reps, o segundos totales = series × segundos.<br>`FinishWorkoutSessionCommand` con RPE 6, satisfacción 4 y minutos activos = previstos |
| **P40** | Igual que H, pero por cada ejercicio: **ejecutado = redondeo half-up de 0,4 × dosis total prescrita**; series = máx(1, redondeo de 0,4 × series).<br>RPE 5, satisfacción 3, minutos activos = redondeo de 0,4 × previstos.<br>Si el redondeo impide el 40 % exacto, se conserva la dosis registrada y el porcentaje real. No se fuerza nada por SQL |
| **O** | Omisión declarada con el endpoint real (`TrainingPlanController.skip`) y el motivo congelado: E3 → S3 `FATIGUE`; E4 → S2 `TOO_DIFFICULT` y S3 `LACK_OF_MOTIVATION` |
| **N** | Nada: la sesión queda sin registro. No se convierte en omisión |

## 5. Control del tiempo

El laboratorio controla el tiempo por tres vías:
- **Semana fija:** la semilla depende de ella.
- **Ventana de ejecución abierta:** `RelojPruebas.abrirVentanaDeEjecucion` lleva `expires_at` al futuro antes de ejecutar, igual que el resto de las pruebas de integración.
- **Barrido horario sustituido** (`OverdueWorkoutScheduler`, *mock*): las sesiones N siguen programadas al cerrar. Con el barrido real, quedarían como SKIPPED sin motivo. Ninguno de los dos casos es un registro.

El ciclo se invoca directamente con `runFor`, no por el disparador de las 03:00.

## 6. Oráculo

`04_ORACULO_INDEPENDIENTE.csv` contiene dos cosas:
- el **oráculo teórico** de la orden, con las definiciones del artículo;
- la **decisión esperada según la política vigente** del backend, leída del código antes de ejecutar (`01_VERIFICACION_PAPER_VS_BACKEND.md`).

Los resultados además **se recalculan de forma independiente** a partir de las dosis registradas en la base, sin llamar a funciones del backend.
- Tolerancia: 0,01 puntos porcentuales.

## 7. Etapa C (Replicate real), prevista

**Condiciones de la ejecución:**
- los mismos 20 perfiles, patrones y motivos;
- `fitsense.ai.enabled=true` con el modelo configurado (`gpt-5-mini` sobre `openai/gpt-5-structured`, esfuerzo `medium`);
- los validadores reales;
- PostgreSQL efímero.

**Medidas que se registran:**
- generación inicial con IA: hasta 3 intentos y después el motor de reglas;
- adaptación desde **una copia controlada del mismo plan inicial** de la etapa A, para aislar el efecto de la IA.

**Límites:**
- **USD 10 o 90 solicitudes** al proveedor, lo que llegue antes;
- 3 intentos por operación, que es la política implementada;
- ningún reintento del laboratorio.

**Guardián:** el laboratorio aborta antes de la primera llamada si la base no es local.

**Bloqueo conocido al congelar:** la sesión no tiene `REPLICATE_API_TOKEN`. Sin token, no hay ninguna llamada y se documenta el bloqueo; no se inventan resultados.

## 8. Reglas de ejecución

- Cada ejecución escribe en una carpeta nueva (`evidencia/A0`, `evidencia/A1`, `evidencia/A1_repeticion`, `evidencia/C`). **El laboratorio se niega a escribir en una carpeta que ya tenga contenido.**
- No se repite selectivamente ningún caso. Una ejecución se repite completa o no se repite.
- Ningún atributo de perfil, patrón o motivo cambia tras observar resultados. Un perfil no factible se conserva como resultado.
