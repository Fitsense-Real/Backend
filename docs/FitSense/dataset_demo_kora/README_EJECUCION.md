# Dataset demostrativo FitSense / Kora: ejecución

Scripts SQL que cargan en una base PostgreSQL de FitSense un **usuario sintético**, «Usuario Demo Kora». Con él se puede iniciar sesión en Kora y ver lo siguiente por las APIs reales:

- el perfil y el objetivo;
- la rutina inicial;
- un historial de tres semanas, con sesiones completas, parciales y omitidas;
- la adherencia;
- dos adaptaciones;
- la rutina de la semana en curso.

> **Ejecútelo solo cuando lo decida.** Todo se preparó y se comprobó en una base de prueba (Testcontainers, PostgreSQL 16, esquema real con Flyway hasta V65). Nada se ejecutó en su base ni en Railway, y no se consumió Replicate.

## 1. Contenido

| Archivo | Qué hace |
|---|---|
| `01_USUARIO_DEMO.sql` | Comprueba la cuenta, ya registrada por la API, y añade su perfil |
| `02_RUTINA_INICIAL.sql` | Rutina inicial de la semana W-3: 5 sesiones y 30 prescripciones |
| `03_HISTORIAL_ADHERENCIA.sql` | Ejecuciones de W-3, su métrica semanal y la exclusión por dolor |
| `04_ADAPTACIONES.sql` | Planes de W-2 y W-1, sus ejecuciones y métricas, y los intentos de adaptación e intervenciones que los publicaron |
| `05_RUTINA_VIGENTE.sql` | Plan de la semana en curso (W0) y el intento que lo publicó |
| `06_VERIFICACION.sql` | 14 consultas de solo lectura: datos, adherencia recalculada, diferencias e integridad |
| `07_ELIMINAR_DEMO.sql` | Borra solo los datos de la cuenta demo |
| `MANIFIESTO_GENERADO.txt` | Ids reservados y filas por tabla |
| `evidencia/` | Resultados de la validación: `validacion.txt`, `06_resultados.txt` y `api/*.json` |

Los scripts los produjo `DatasetDemoKoraGeneradorIT` con los **flujos reales del backend**:

1. registro, inicio de sesión y perfil por la API;
2. rutina inicial con el motor de reglas;
3. sesiones por los mismos endpoints que usa la app (`start`, `PUT` de cada ejercicio, `finish`, `skip`);
4. tres cierres semanales reales (`WeeklyCycleService`) con la IA apagada.

Después se exportaron las filas tal como las dejó el backend. Ninguna fila está escrita a mano.

## 2. Qué verá la demo

Las fechas son relativas: **W0 es la semana en curso en Lima** el día en que ejecute 02. En la tabla, las fechas son las de la generación (W0 = 23/11/2026).

| Semana | Plan | Estado | Origen | Qué pasó |
|---|---|---|---|---|
| W-3 (02/11) | `910000000` | COMPLETED | `RULE_ENGINE`, sin modelo | **Rutina inicial.** 2 sesiones completas (RPE 6 y 7); 1 parcial por fatiga (53 %); 1 con dolor en «Aperturas de pecho sentado con palanca» (86 %); 1 omitida por falta de tiempo. **Adherencia 67,81 %** |
| W-2 (09/11) | `910000001` | COMPLETED | `CONSERVED` | **Adaptación 1.** El ejercicio con dolor se sustituye. 5/5 sesiones completas (RPE 7). Adherencia 100 % |
| W-1 (16/11) | `910000002` | COMPLETED | `CONSERVED` | Sin cambios. 5/5 sesiones completas (RPE 6). Adherencia 100 % |
| **W0 (23/11)** | **`910000003`** | **ACTIVE** | `CONSERVED` | **Adaptación 2.** Progresión de un ejercicio. Rutina vigente |

**Perfil:**

- objetivo GAIN_MUSCLE, nivel INTERMEDIATE;
- 5 días (lunes a viernes) de 60 min, en gimnasio, con material completo de gimnasio;
- sin restricciones;
- nacido el 12/04/1995, 176 cm, 74 kg.

### 2.1 Rutina inicial

- **Origen:** `RULE_ENGINE` y `model_name` nulo, porque la generó el motor de reglas con la IA apagada. **No se atribuye a la IA ni a `gpt-5-mini`.**
- **Validación:** pasó el validador de publicación del backend (`PlanDraftValidator`, el que aplica V8C, V15, V24 y V26, entre otras) con la política vigente `PLAN-REV-2.0`. En el registro: `origen=RESPALDO motivo_respaldo=IA_APAGADA`.
- **Ejercicios:** todos del catálogo real. Cada script comprueba el `exercise_id` y el `source_code` de cada ejercicio antes de cargar.
- **Sesiones:**
  - Empuje (6 ejercicios);
  - Tracción (5);
  - Piernas (5);
  - Tren superior (7);
  - Tren inferior (7);
  - dosis: 20 prescripciones a 3×10, 7 a 2×13 y 3 a 2×10, todas con 90 s de descanso.

### 2.2 Adaptación 1 (W-3 → W-2): baja adherencia con dolor

- **Señal:** la métrica de W-3 dio **67,81 %**. Con 4 sesiones válidas de 5 y la omisión por falta de tiempo, el decisor eligió **`ADAPT_LOCALIZED`**. Se dirigió a la sesión del jueves, con la intervención `SEGURIDAD_ANTE_DOLOR`.
- **Resultado sin IA:** el intento queda en **`RESPALDO`**, con 0 llamadas a la IA. La semana se conserva y el backend aplica su regla determinista **SUST-SEG-1**:
  - «Aperturas de pecho sentado con palanca» (1025) pasa a «Aleteo inclinado con cable» (342), con la misma dosis, 2×10 y 90 s;
  - queda una exclusión `DOLOR_IDENTIFICADO`, activa, del ejercicio 1025.
- **Lo que no se hizo:** la corrección localizada que redactaría la IA no se ejecutó. El dataset muestra lo que hace el backend cuando la IA no está disponible.
- **Trazabilidad** (`plan_adaptation_attempts` 910000000):
  - plan anterior, métrica medida y plan resultante;
  - decisión, estado e intervención;
  - `conservation_report.sustituciones` con la regla, el motivo, lo quitado, lo puesto y la sesión de origen;
  - `correcciones_del_backend`.

### 2.3 Adaptación 2 (W-1 → W0): progresión

- **Señal:** **dos semanas seguidas con el 100 % real** (W-2 y W-1). Es la condición de la progresión ordinaria de `ProgresionResolver`.
- **Resultado:** el cierre decide `NO_CHANGE` / `SIN_ORDEN`, y el backend aplica un paso **`PROGRESION`**:
  - «Dip de tríceps en piso» (1320) pasa de 3×10 a **3×11**;
  - el volumen semanal pasa de 842 a 845, un +0,36 %, dentro del techo.
- **Respuesta de la API:** `resultado = PROGRESION_DEL_BACKEND`, con 0 llamadas a la IA.

**Sobre la segunda señal.** La orden pedía, por ejemplo, esfuerzo o recuperación. Con la IA apagada, una señal de esfuerzo (RPE alto) no produce cambios: el backend devuelve `NO_CHANGE`, porque la dosis de mantenimiento P-A1 es un permiso para la corrección de la IA. Se usó la progresión, que es otra señal que la política autoriza sin IA.

**Otras adaptaciones.** No se forzó ningún cambio ni se inventó ninguna ejecución de IA. Una adaptación por esfuerzo o recuperación necesitaría el modelo real, con coste en Replicate. Sería una operación aparte, con su propia autorización.

**La semana W-2 → W-1** se conservó sin cambios (`NO_CHANGE`, adherencia del 100 %).

**Diferencias** (consulta 11 de `06_VERIFICACION.sql`):

| Plan anterior → resultante | Sesión | Antes | Después |
|---|---|---|---|
| 910000000 → 910000001 | Tren superior (jueves), ejercicio 1 | Aperturas de pecho sentado con palanca 2×10 | Aleteo inclinado con cable 2×10 |
| 910000002 → 910000003 | Empuje (lunes), ejercicio 1 | Dip de tríceps en piso 3×10 | Dip de tríceps en piso 3×11 |

## 3. Requisitos

1. **Backend** con la base migrada por Flyway **hasta V65**, que es la de la rama `generation`, y el catálogo de ejercicios de las migraciones.
   - Los scripts abortan si falta V65 o si un ejercicio no coincide.
2. **Acceso SQL** con un usuario que pueda leer e insertar, con DBeaver o pgAdmin.
   - Se recomienda una **copia de seguridad** antes de cargar.
3. **Ejecutar 01 a 05 seguidos, el mismo día.** 02 fija las semanas con la fecha de Lima, y 05 comprueba que el plan vigente cubre hoy.
   - Lo ideal es cargarlo **el lunes** de la semana de la demo (ver §8).

## 4. Paso 1: registrar la cuenta demo por la API

**La contraseña la elige usted.** No está en ningún script, y los scripts no crean usuarios, hashes ni tokens.

**Registre la cuenta *sin* participación en el estudio** (`acceptsStudyParticipation: false`), por estos motivos:

- **Sin cierre automático:** el cierre semanal de los lunes solo procesa participantes. Así no se genera otro plan con IA (con coste en Replicate) ni se altera la demo.
- **Sin generación a petición:** una generación pedida desde la app se rechaza.
- **Fuera de la cohorte:** la cuenta no entra en la cohorte del estudio.

01 aborta si la cuenta participa.

```bash
API=https://SU-BACKEND   # por ejemplo, la URL del backend de pruebas
curl -X POST "$API/api/v1/auth/register" -H "Content-Type: application/json" -d '{
  "email": "usuario.demo.kora@example.com",
  "password": "ELIJA-UNA-CONTRASENA",
  "firstName": "Usuario",
  "lastName": "Demo Kora",
  "timezone": "America/Lima",
  "acceptsStudyParticipation": false
}'
```

En PowerShell:

```powershell
$API = "https://SU-BACKEND"
Invoke-RestMethod -Method Post -Uri "$API/api/v1/auth/register" -ContentType "application/json" -Body (@{
  email = "usuario.demo.kora@example.com"; password = "ELIJA-UNA-CONTRASENA"; firstName = "Usuario";
  lastName = "Demo Kora"; timezone = "America/Lima"; acceptsStudyParticipation = $false } | ConvertTo-Json)
```

**No complete el perfil en la app antes de ejecutar 01.** 01 añade el perfil de la demo y aborta si la cuenta ya tiene uno distinto.

**Otro correo.** Para usar otro, cambie `usuario.demo.kora@example.com` en los 7 scripts. En 06 aparece en cada consulta: use buscar y reemplazar. No hay más valores que configurar.

## 5. Paso 2: ejecutar 01 a 05

Cada script es **un solo bloque `DO`**:

- **Atómico:** si algo falla, no se aplica nada.
- **Idempotente:** repetirlo no duplica filas; avisa «ya aplicado… No se cambia nada».
- **Comprobaciones antes de insertar:**
  - el paso anterior;
  - la migración V65;
  - el catálogo;
  - que ningún id reservado (rango `910000000`–`910999999`) esté ocupado por otra cuenta.

**DBeaver**

1. Conéctese a la base, con auto-commit activado (el modo por defecto).
2. Abra `01_USUARIO_DEMO.sql` y use **Ejecutar script** (Alt+X).
3. El aviso `DEMO 01 aplicado…` aparece en la pestaña **Output**. Si no la ve, actívela con «Show server output».
4. Repita con 02, 03, 04 y 05, en ese orden.

**pgAdmin**

1. Abra el **Query Tool**, luego **Open File** y elija `01_USUARIO_DEMO.sql`.
2. Ejecútelo con **F5**. El aviso sale en la pestaña **Messages**.
3. Repita con 02 a 05.

Avisos esperados:

```
DEMO 01 aplicado: perfil de la cuenta <id>
DEMO 02 aplicado a la cuenta <id> (weekly_training_plans=1, planned_workouts=5, planned_workout_exercises=30)
DEMO 03 aplicado a la cuenta <id> (workout_sessions=4, workout_session_exercises=23, weekly_user_metrics=1, user_safety_exclusions=1)
DEMO 04 aplicado a la cuenta <id> (weekly_training_plans=2, planned_workouts=10, planned_workout_exercises=60, workout_sessions=10, workout_session_exercises=60, weekly_user_metrics=2, plan_adaptation_attempts=2, user_interventions=2)
DEMO 05 aplicado a la cuenta <id> (weekly_training_plans=1, planned_workouts=5, planned_workout_exercises=30, plan_adaptation_attempts=1, user_interventions=1)
```

**Si sale un `ERROR: DEMO 0x: …`, no se cargó nada de ese script.** El mensaje dice qué falta.

**Qué escriben los scripts.** Solo insertan filas de la cuenta demo en estas tablas:

- `user_profiles`;
- `weekly_training_plans`, `planned_workouts`, `planned_workout_exercises`;
- `workout_sessions`, `workout_session_exercises`, `weekly_user_metrics`;
- `user_safety_exclusions`, `plan_adaptation_attempts`, `user_interventions`.

**Qué no hacen:**

- no modifican `users` ni otras cuentas;
- no tocan el catálogo ni el esquema;
- no desactivan restricciones ni disparadores: los disparadores de coherencia V31, V32 y V34 se ejecutan y validan cada fila;
- no crean avisos push.

## 6. Paso 3: verificar con 06

Ejecute las consultas de `06_VERIFICACION.sql` una a una. Lo esperado:

| Consulta | Esperado |
|---|---|
| 1 | Cuenta ACTIVE, America/Lima, `is_study_participant = false` |
| 3 | 4 planes: 3 COMPLETED y 1 ACTIVE, ninguno con origen `AI` ni modelo |
| 4 | **Exactamente 1 fila**: el plan vigente hoy en Lima |
| 9 | `coincide = true` en las 3 semanas: la adherencia guardada es la que se recalcula desde las sesiones |
| 11 | 2 filas: la sustitución y la progresión |
| 14 | `ok = true` en las 10 comprobaciones |

## 7. Paso 4: entrar desde Kora

En la app, inicie sesión con `usuario.demo.kora@example.com` y la contraseña que eligió en el paso 1.

> **PENDIENTE: no se ha probado en un dispositivo con Kora.** La validación usó la aplicación Spring completa y los mismos endpoints REST que usa la app, pero no la app. Que cada pantalla muestre bien estos datos queda por comprobar en el dispositivo.

Para comprobar los datos por la API sin la app:

```bash
TOKEN=$(curl -s -X POST "$API/api/v1/auth/login" -H "Content-Type: application/json" \
  -d '{"email":"usuario.demo.kora@example.com","password":"SU-CONTRASENA"}' | sed 's/.*"token":"\([^"]*\)".*/\1/')
for p in users/me users/me/profile users/me/plan/current users/me/plans users/me/plan/adaptations \
         users/me/metrics/weekly users/me/interventions users/me/pain-exclusions; do
  echo "== $p"; curl -s -H "Authorization: Bearer $TOKEN" "$API/api/v1/$p"; echo; done
```

Las respuestas que dio la validación están en `evidencia/api/`, una por endpoint.

## 8. Qué hará el backend después de la carga

- **Días pasados de W0.** La tarea de vencimiento, cada hora a los :30, marcará como `SKIPPED` sin causa los entrenamientos de W0 de días ya pasados. Es el comportamiento real: el usuario demo no los hizo.
  - Si carga el dataset un jueves, lunes a miércoles aparecerán como no hechos.
  - **Cárguelo el lunes** si quiere la semana entera disponible.
  - El entrenamiento de hoy se puede hacer en directo desde la app.
- **El próximo lunes:**
  - no se genera un plan nuevo, porque la cuenta no participa;
  - desde ese día `GET /plan/current` responde 404 y la demo deja de tener semana vigente;
  - **para refrescarla:** `07_ELIMINAR_DEMO.sql` y luego 01–05 otra vez.
- **Generar desde la app:**
  - `POST /users/me/plan/generate` se rechaza con 422;
  - `POST /users/me/plan/generation` devuelve el plan ya publicado, como operación `COMPLETED` sin IA, y no crea plan.
- **Avisos push** (solo si el entorno tiene el envío activado y la app registra el dispositivo):
  - si carga el dataset un lunes antes de las 09:00 de Lima, el detector puede crear un aviso «plan de la semana listo» para la cuenta demo, porque el intento de W0 tiene hora de lunes 03:00;
  - 07 borra esos avisos.

## 9. Paso 5: eliminar la demo (07)

`07_ELIMINAR_DEMO.sql` borra **todos los datos de la cuenta demo**. Incluye las sesiones que se hagan en directo durante la presentación, la operación de generación y los avisos.

**Guardas:**

- busca la cuenta por su correo exacto;
- **solo borra si esa cuenta es dueña del plan marcador `910000000`**; una cuenta con ese correo pero sin el dataset no se toca;
- es un solo bloque, así que si falla no borra nada.

**La cuenta:**

- por omisión se **conserva** (`v_eliminar_cuenta = false`), para poder volver a cargar 01–05;
- para borrarla también, cambie esa constante a `true`.

## 10. Resultado de la validación

`DatasetDemoKoraValidacionIT` usa una base nueva, con Flyway hasta V65 y la aplicación completa, sin IA. El detalle está en `evidencia/validacion.txt`.

| Comprobación | Resultado |
|---|---|
| Otra cuenta normal con plan y una sesión, antes de la demo | Creada |
| 01 y 02 sin la cuenta demo | `ERROR … no existe la cuenta`; no queda nada |
| Registro de la demo por la API, sin participación | user_id asignado por la secuencia normal |
| 01 a 05 | Aplicados: 4 planes, 20 entrenamientos, 120 prescripciones, 14 sesiones, 83 ejercicios registrados, 3 métricas, 3 intentos, 3 intervenciones, 1 exclusión |
| 01 a 05 otra vez | «ya aplicado… No se cambia nada»; filas y huella idénticas |
| 06 | Integridad 10/10 `ok`; adherencia recalculada igual en las 3 semanas; 1 plan vigente; 2 diferencias |
| API como el usuario demo | 200 en `/users/me`, `/profile`, `/plan/current` (plan 910000003, semana en curso, 5 sesiones), `/plans`, `/plan/{id}` ×4, `/plan/adaptations`, `/metrics/weekly`, `/metrics/weekly/{semana}` ×3, `/interventions`, `/pain-exclusions`, `/pain-exclusions/effective` y `/sessions/{id}` |
| `/plan/limitations` | **422**: exige participar en el estudio (ver §11) |
| `POST /plan/generate` | 422 «no esta activo en el estudio»; ningún plan nuevo |
| `POST /plan/generation` | 200 `COMPLETED` con el plan 910000003; sin predicciones de IA |
| `POST /cycle/run-for-me` | 403 |
| Cierre de los lunes | La demo no está entre los participantes de America/Lima |
| Tarea real de vencimiento | Solo los días de W0 anteriores a hoy pasan a `SKIPPED`; sigue 1 plan ACTIVE |
| 07 | Toda la demo a 0 filas, con la cuenta conservada; la otra cuenta y el catálogo idénticos (conteos y huella md5) |
| 07 otra vez | «no tiene el dataset cargado»; sin cambios |
| 01 a 05 después de 07, y 07 | Recarga correcta y limpieza correcta |

## 11. Limitaciones

1. **App sin probar en un dispositivo** (§7).
2. **`GET /users/me/plan/limitations` responde 422** a una cuenta que no participa. Calcula la próxima generación, y esta exige participación.
   - Según el cierre de políticas (P19), la app no consume este endpoint.
   - Si una pantalla lo usa, mostrará un error con la cuenta demo.
3. **Validez de una semana.** El dataset vale para la semana en que se carga (§8), y los días pasados de W0 quedan como no hechos.
4. **Sesiones cortas para el perfil.** Las sesiones estiman entre 28 y 39 min frente a los 60 min del perfil. Es lo que produce el motor de reglas con la política vigente (V20 sin mínimo de duración, ver H2); no se ha retocado.
5. **Dos adaptaciones deterministas, no de IA.**
   - La primera queda en `RESPALDO`: sustitución de seguridad sin la corrección localizada de la IA.
   - La segunda es una progresión por adherencia, no por esfuerzo ni recuperación.
   - `parent_plan_id` es nulo en todos los planes: el backend no lo rellena en los cierres semanales. La cadena se sigue por `plan_adaptation_attempts` y por `planned_workouts.source_planned_workout_id`.
6. **Horas sintéticas, pero coherentes:**
   - sesiones a las 18:30 de Lima;
   - cierres el lunes a las 03:00;
   - alta y rutina inicial el domingo anterior a W-3.

   `users.created_at` es el del registro real, porque los scripts no tocan `users`.
7. **Mismo esquema y catálogo.** Los scripts exigen V65 y el mismo catálogo. Si el esquema cambia en una migración futura, la inserción falla entera y hay que regenerarlos (§12).
8. **Clientes SQL.** Los scripts se ejecutaron por JDBC en la validación, y `psql` los analiza como un único bloque cada uno. **No se probaron en DBeaver ni en pgAdmin.** Si DBeaver partiera el bloque, ejecute el archivo entero con Alt+X, no la sentencia bajo el cursor.
9. **Analíticas de administración.** La cuenta queda fuera del cierre semanal, comprobado. No se ha comprobado cómo la listan las analíticas de administración, que pueden mostrarla como cuenta no participante.

## 12. Regenerar o volver a validar (solo en local, base de prueba)

```bash
# Genera 01-05 y MANIFIESTO_GENERADO.txt con la semana actual de Lima (no hace falta: las fechas son relativas)
mvn verify -Pintegration -Dit.test=DatasetDemoKoraGeneradorIT -Dfitsense.demo.generar=SI
# Valida los scripts en una base nueva y escribe evidencia/
mvn verify -Pintegration -Dit.test=DatasetDemoKoraValidacionIT -Dfitsense.demo.evidencia=SI
```

Las dos pruebas levantan su propia PostgreSQL con Docker (Testcontainers), con la IA apagada y sin Replicate. La validación también se ejecuta en CI, sin escribir evidencia.
