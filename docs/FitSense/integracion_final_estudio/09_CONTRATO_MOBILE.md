# 09 · Contrato para Mobile (entrega final)

**Backend:**
- `generation` con el código final en **`cca9fc2162f5a7e00f82e36a64a8e7a4c48ab03d`** (#96, #97 y #98 fusionadas);
- este documento llega con #100, que solo añade documentación.

**#99 (V67) no está incluida.** [#101](https://github.com/Fitsense-Real/Backend/pull/101) (CORS), [#102](https://github.com/Fitsense-Real/Backend/pull/102) (código vigente) y [#103](https://github.com/Fitsense-Real/Backend/pull/103) (mensajes sin id) están **abiertas, sin fusionar**: lo que cambiarían se indica aparte.

**Cómo se obtuvo:**
- todas las respuestas de §2 vienen de **ejecutar** ese código con un servidor HTTP real (Tomcat), PostgreSQL de Testcontainers y IA apagada (motor de reglas);
- documentos **sintéticos**, con el registro definitivo encendido **solo en esa ejecución**;
- `<JWT>` sustituye al token;
- de las cabeceras de respuesta solo se muestran `Retry-After` y las de CORS;
- los cuerpos largos están recortados.

**Ninguna versión, título ni URL es real.** Hoy, en cualquier entorno, `GET /api/v1/legal-documents` devuelve `documents: []` y `enforcedOnSignup: false`.

## 1. Reglas para la app

1. **El código está en `details[0]`; el texto de `message` es para mostrarlo, no para interpretarlo.** Tabla completa en §3.
2. `acceptsStudyParticipation` es **obligatorio** en el JSON del registro (sin él, 400). La casilla empieza sin marcar.
3. La app envía **exactamente** la `version` de cada documento que mostró, y abre su `url`: el backend no sirve el texto.
4. **`requiredForSignup`** dice qué documentos exige el registro. Con el registro definitivo y la opción A, los tres.
5. Ante `VERSION_NO_VIGENTE`: volver a pedir `/legal-documents`, mostrar el documento vigente y repetir la aceptación.
6. Ante 429: esperar `Retry-After` segundos si se puede leer. **Si no se puede, mostrar un mensaje genérico ("Demasiados intentos; espera unos minutos") y no reintentar automáticamente.** El cuerpo siempre lleva `DEMASIADAS_SOLICITUDES`.
7. Una sola ruta para el primer plan: la **asíncrona** (`POST /plan/generation` y luego consultar cada `pollAfterSeconds`). No combinarla con la síncrona (§3.2).


## 2. Intercambios reales

### C-01 · Documentos vigentes (sin autenticación)

```http
GET /api/v1/legal-documents HTTP/1.1
```

```http
HTTP/1.1 200
Content-Type: application/json

{
  "enforcedOnSignup" : true,
  "privacyRecord" : "ACEPTACION",
  "documents" : [ {
    "type" : "STUDY_PARTICIPATION",
    "version" : "CI-EJEMPLO-1",
    "title" : "Consentimiento informado (EJEMPLO SINTETICO)",
    "url" : "https://documentos.ejemplo.invalid/consentimiento-1",
    "status" : "PUBLISHED",
    "effectiveFrom" : "2026-01-01",
    "requiredForSignup" : true
  }, {
    "type" : "TERMS_OF_SERVICE",
    "version" : "T-EJEMPLO-2",
    "title" : "Condiciones de uso (EJEMPLO SINTETICO)",
    "url" : "https://documentos.ejemplo.invalid/condiciones-2",
    "status" : "PUBLISHED",
    "effectiveFrom" : "2026-11-01",
    "requiredForSignup" : true
  }, {
    "type" : "PRIVACY_POLICY",
    "version" : "P-EJEMPLO-1",
    "title" : "Politica de privacidad (EJEMPLO SINTETICO)",
    "url" : "https://documentos.ejemplo.invalid/privacidad-1",
    "status" : "PUBLISHED",
    "effectiveFrom" : "2026-01-01",
    "requiredForSignup" : true
  } ]
}
```

### C-02 · Registro válido

```http
POST /api/v1/auth/register HTTP/1.1
Content-Type: application/json

{
  "email" : "ana@ejemplo.invalid",
  "password" : "Clave-de-ejemplo-1",
  "firstName" : "Ana",
  "timezone" : "America/Lima",
  "acceptsStudyParticipation" : true,
  "acceptedTermsVersion" : "T-EJEMPLO-2",
  "acceptedPrivacyVersion" : "P-EJEMPLO-1",
  "acceptedStudyConsentVersion" : "CI-EJEMPLO-1"
}
```

```http
HTTP/1.1 201
Content-Type: application/json

{
  "userId" : 1,
  "email" : "ana@ejemplo.invalid",
  "firstName" : "Ana",
  "lastName" : null,
  "timezone" : "America/Lima",
  "status" : "ACTIVE",
  "studyParticipant" : true,
  "participantCode" : "P-001",
  "enrolledAt" : "2026-11-28T05:52:01.652657908Z",
  "withdrawnAt" : null,
  "lastLoginAt" : null,
  "role" : "USER"
}
```

### C-03 · Versión inexistente del consentimiento informado (`CI-EJEMPLO-9`)

```http
POST /api/v1/auth/register HTTP/1.1
Content-Type: application/json

{
  "email" : "c03@ejemplo.invalid",
  "password" : "Clave-de-ejemplo-1",
  "firstName" : "Ana",
  "timezone" : "America/Lima",
  "acceptsStudyParticipation" : true,
  "acceptedTermsVersion" : "T-EJEMPLO-2",
  "acceptedPrivacyVersion" : "P-EJEMPLO-1",
  "acceptedStudyConsentVersion" : "CI-EJEMPLO-9"
}
```

```http
HTTP/1.1 422
Content-Type: application/json

{
  "timestamp" : "2026-11-28T05:52:01.714728342Z",
  "status" : 422,
  "error" : "Unprocessable Entity",
  "message" : "La version aceptada de STUDY_PARTICIPATION no es la vigente (CI-EJEMPLO-1).",
  "details" : [ "VERSION_NO_VIGENTE" ]
}
```

### C-04 · Versión incorrecta: cambió durante el registro (la app mostró `T-EJEMPLO-1`; ahora rige `T-EJEMPLO-2`)

```http
POST /api/v1/auth/register HTTP/1.1
Content-Type: application/json

{
  "email" : "c04@ejemplo.invalid",
  "password" : "Clave-de-ejemplo-1",
  "firstName" : "Ana",
  "timezone" : "America/Lima",
  "acceptsStudyParticipation" : true,
  "acceptedTermsVersion" : "T-EJEMPLO-1",
  "acceptedPrivacyVersion" : "P-EJEMPLO-1",
  "acceptedStudyConsentVersion" : "CI-EJEMPLO-1"
}
```

```http
HTTP/1.1 422
Content-Type: application/json

{
  "timestamp" : "2026-11-28T05:52:01.752274355Z",
  "status" : 422,
  "error" : "Unprocessable Entity",
  "message" : "La version aceptada de TERMS_OF_SERVICE no es la vigente (T-EJEMPLO-2).",
  "details" : [ "VERSION_NO_VIGENTE" ]
}
```

### C-05 · Consentimiento informado ausente

```http
POST /api/v1/auth/register HTTP/1.1
Content-Type: application/json

{
  "email" : "c05@ejemplo.invalid",
  "password" : "Clave-de-ejemplo-1",
  "firstName" : "Ana",
  "timezone" : "America/Lima",
  "acceptsStudyParticipation" : true,
  "acceptedTermsVersion" : "T-EJEMPLO-2",
  "acceptedPrivacyVersion" : "P-EJEMPLO-1"
}
```

```http
HTTP/1.1 422
Content-Type: application/json

{
  "timestamp" : "2026-11-28T05:52:01.759465142Z",
  "status" : 422,
  "error" : "Unprocessable Entity",
  "message" : "Para participar en el estudio tienes que aceptar la version vigente del consentimiento informado.",
  "details" : [ "CONSENTIMIENTO_REQUERIDO" ]
}
```

### C-06 · Participación rechazada

```http
POST /api/v1/auth/register HTTP/1.1
Content-Type: application/json

{
  "email" : "c06@ejemplo.invalid",
  "password" : "Clave-de-ejemplo-1",
  "firstName" : "Ana",
  "timezone" : "America/Lima",
  "acceptsStudyParticipation" : false,
  "acceptedTermsVersion" : "T-EJEMPLO-2",
  "acceptedPrivacyVersion" : "P-EJEMPLO-1"
}
```

```http
HTTP/1.1 422
Content-Type: application/json

{
  "timestamp" : "2026-11-28T05:52:01.765426426Z",
  "status" : 422,
  "error" : "Unprocessable Entity",
  "message" : "Kora solo esta disponible para las personas que participan en el estudio. Para registrarte tienes que aceptar participar.",
  "details" : [ "PARTICIPACION_REQUERIDA" ]
}
```

### Inicio de sesión

```http
POST /api/v1/auth/login HTTP/1.1
Content-Type: application/json

{
  "email" : "ana@ejemplo.invalid",
  "password" : "Clave-de-ejemplo-1"
}
```

```http
HTTP/1.1 200
Content-Type: application/json

{
  "userId" : 1,
  "email" : "ana@ejemplo.invalid",
  "fullName" : "Ana",
  "token" : "<JWT>",
  "role" : "USER"
}
```

### C-07 · Generación asíncrona: solicitud (operación nueva, 202)

```http
POST /api/v1/users/me/plan/generation HTTP/1.1
Authorization: Bearer <JWT>
```

```http
HTTP/1.1 202
Content-Type: application/json

{
  "operationId" : "8efa8dab-cbe2-468b-91f1-243919018385",
  "status" : "PENDING",
  "terminal" : false,
  "weekStartDate" : "2026-11-30",
  "planId" : null,
  "reasonCode" : null,
  "message" : null,
  "retryable" : false,
  "createdAt" : "2026-11-28T05:52:02.065390982Z",
  "startedAt" : null,
  "finishedAt" : null,
  "pollAfterSeconds" : 3
}
```

### C-08 · Generación asíncrona: última operación

```http
GET /api/v1/users/me/plan/generation/latest HTTP/1.1
Authorization: Bearer <JWT>
```

```http
HTTP/1.1 200
Content-Type: application/json

{
  "operationId" : "8efa8dab-cbe2-468b-91f1-243919018385",
  "status" : "PENDING",
  "terminal" : false,
  "weekStartDate" : "2026-11-30",
  "planId" : null,
  "reasonCode" : null,
  "message" : null,
  "retryable" : false,
  "createdAt" : "2026-11-28T05:52:02.065391Z",
  "startedAt" : null,
  "finishedAt" : null,
  "pollAfterSeconds" : 3
}
```

### C-09 · Generación asíncrona: repetir la solicitud con una operación pendiente (200, la misma)

```http
POST /api/v1/users/me/plan/generation HTTP/1.1
Authorization: Bearer <JWT>
```

```http
HTTP/1.1 200
Content-Type: application/json

{
  "operationId" : "8efa8dab-cbe2-468b-91f1-243919018385",
  "status" : "PENDING",
  "terminal" : false,
  "weekStartDate" : "2026-11-30",
  "planId" : null,
  "reasonCode" : null,
  "message" : null,
  "retryable" : false,
  "createdAt" : "2026-11-28T05:52:02.065391Z",
  "startedAt" : null,
  "finishedAt" : null,
  "pollAfterSeconds" : 3
}
```

### C-10 · Generación síncrona con una asíncrona todavía `PENDING` (ver la nota de §3.2)

```http
POST /api/v1/users/me/plan/generate HTTP/1.1
Authorization: Bearer <JWT>
```

```http
HTTP/1.1 201
Content-Type: application/json

{
  "planId" : 1,
  "weekNumber" : 1,
  "weekStartDate" : "2026-11-30",
  "weekEndDate" : "2026-12-06",
  "planVersion" : 1,
  "parentPlanId" : null,
  "plannedDaysCount" : 3,
  "status" : "ACTIVE",
  "generationSource" : "RULE_ENGINE",
  "modelName" : null,
  "adjustmentApplied" : null,
  "adjustmentReason" : null,
  "activatedAt" : "2026-11-28T05:52:02.58266956Z",
  "workouts" : [ {
    "plannedWorkoutId" : 1,
    "scheduledDate" : "2026-11-30",
    "focusCode" : "FULL_BODY",
    "workoutName" : "Cuerpo completo",
    "expectedDurationMinutes" : 25,
    "displayOrder" : 1,
    "status" : "SCHEDULED",
    "skipReason" : null,
    "expiresAt" : "2026-11-30T23:59:00-05:00",
    "exercises" : [ {
      "plannedExerciseId" : 1,
      "exerciseId" : 507,
      "exerciseName" : "Sentadilla curtsy",
      "targetMuscle" : "glutes",
      "bodyPart" : "upper legs",
      "equipment" : "body weight",
      "difficultyLevel" : 1,
      "exerciseOrder" : 1,
      "prescriptionType" : "SETS_REPS",
      "plannedSets" : 2,
      "plannedReps" : 8,
      "plannedDurationSeconds" : null,
      "targetLoadKg" : null,
      "restSeconds" : 120,
      "notes" : null,
      "gifPath" : "videos/3769-gUjqdei.gif",
      "imagePath" : "images/3769-gUjqdei.jpg",
      "mediaAttribution" : "© Gym visual — https://gymvisual.com/",
      "safetyBlockReason" : null
    }, {
      "plannedExerciseId" : 2,
      "exerciseId" : 878,
      "exerciseName" : "Marcha en puente de glúteos",
      "targetMuscle" : "glutes",
      "bodyPart" : "upper legs",
      "equipment" : "body weight",
      "difficultyLevel" : 1,
      "exerciseOrder" : 2,
      "prescriptionType" : "SETS_REPS",
      "plannedSets" : 2,
      "plannedReps" : 8,
      "plannedDurationSeconds" : null,
      "targetLoadKg" : null,
      "restSeconds" : 120,
      "notes" : null,
      "gifPath" : "videos/3561-GibBPPg.gif",
      "imagePath" : "images/3561-GibBPPg.jpg",
      "mediaAttribution" : "© Gym visual — https://gymvisual.com/",
      "safetyBlockReason" : null
    }, {
      "plannedExerciseId" : 3,
      "exerciseId" : 933,
      "exerciseName" : "Flexión de brazos con rodillas apoyadas",
      "targetMuscle" : "pectorals",
      "bodyPart" : "chest",
      "equipment" : "body weight",
      "difficultyLevel" : 1,
      "exerciseOrder" : 3,
      "prescriptionType" : "SETS_REPS",
      "plannedSets" : 2,
      "plannedReps" : 15,
      "plannedDurationSeconds" : null,
      "targetLoadKg" : null,
      "restSeco
  ... (recortado)
```

### C-11 · Historial de consentimientos

```http
GET /api/v1/users/me/consents HTTP/1.1
Authorization: Bearer <JWT>
```

```http
HTTP/1.1 200
Content-Type: application/json

{
  "currentVersions" : {
    "STUDY_PARTICIPATION" : "CI-EJEMPLO-1",
    "TERMS_OF_SERVICE" : "T-EJEMPLO-2",
    "PRIVACY_POLICY" : "P-EJEMPLO-1"
  },
  "approvedDocuments" : {
    "STUDY_PARTICIPATION" : true,
    "TERMS_OF_SERVICE" : true,
    "PRIVACY_POLICY" : true
  },
  "enforcedOnSignup" : true,
  "history" : [ {
    "type" : "STUDY_PARTICIPATION",
    "documentVersion" : "CI-EJEMPLO-1",
    "grantedAt" : "2026-11-28T05:52:01.681948Z",
    "affirmativeAction" : "CASILLA_REGISTRO",
    "source" : "APP",
    "withdrawnAt" : null
  }, {
    "type" : "TERMS_OF_SERVICE",
    "documentVersion" : "T-EJEMPLO-2",
    "grantedAt" : "2026-11-28T05:52:01.686710Z",
    "affirmativeAction" : "CASILLA_REGISTRO",
    "source" : "APP",
    "withdrawnAt" : null
  }, {
    "type" : "PRIVACY_POLICY",
    "documentVersion" : "P-EJEMPLO-1",
    "grantedAt" : "2026-11-28T05:52:01.687519Z",
    "affirmativeAction" : "CASILLA_REGISTRO",
    "source" : "APP",
    "withdrawnAt" : null
  } ]
}
```

### C-12 · Retirada del estudio

```http
POST /api/v1/users/me/consents/STUDY_PARTICIPATION/withdraw HTTP/1.1
Authorization: Bearer <JWT>
```

```http
HTTP/1.1 204
```

### C-13 · Generación asíncrona tras la retirada, con una operación anterior pendiente (200, la misma)

```http
POST /api/v1/users/me/plan/generation HTTP/1.1
Authorization: Bearer <JWT>
```

```http
HTTP/1.1 200
Content-Type: application/json

{
  "operationId" : "8efa8dab-cbe2-468b-91f1-243919018385",
  "status" : "PENDING",
  "terminal" : false,
  "weekStartDate" : "2026-11-30",
  "planId" : null,
  "reasonCode" : null,
  "message" : null,
  "retryable" : false,
  "createdAt" : "2026-11-28T05:52:02.065391Z",
  "startedAt" : null,
  "finishedAt" : null,
  "pollAfterSeconds" : 3
}
```

### C-14 · Generación síncrona tras la retirada (422)

```http
POST /api/v1/users/me/plan/generate HTTP/1.1
Authorization: Bearer <JWT>
```

```http
HTTP/1.1 422
Content-Type: application/json

{
  "timestamp" : "2026-11-28T05:52:02.842526124Z",
  "status" : 422,
  "error" : "Unprocessable Entity",
  "message" : "Tu cuenta no tiene una participacion activa en el estudio: Kora no puede preparar rutinas nuevas. Si crees que es un error, contacta al equipo del estudio.",
  "details" : [ "PARTICIPACION_NO_VIGENTE" ]
}
```

### C-15 · 429 desde un origen autorizado (navegador), **sin #101**

```http
POST /api/v1/auth/register HTTP/1.1
Origin: https://app.ejemplo.invalid
Content-Type: application/json

{
  "email" : "y@ejemplo.invalid",
  "password" : "Clave-de-ejemplo-1",
  "firstName" : "Ana",
  "timezone" : "America/Lima",
  "acceptsStudyParticipation" : true,
  "acceptedTermsVersion" : "T-EJEMPLO-2",
  "acceptedPrivacyVersion" : "P-EJEMPLO-1",
  "acceptedStudyConsentVersion" : "CI-EJEMPLO-1"
}
```

```http
HTTP/1.1 429
Retry-After: 898
Access-Control-Allow-Origin: https://app.ejemplo.invalid
Content-Type: application/json

{
  "timestamp" : "2026-11-28T05:52:03.143864905Z",
  "status" : 429,
  "error" : "Too Many Requests",
  "message" : "Demasiadas solicitudes desde esta conexion. Espera antes de volver a intentarlo.",
  "details" : [ "DEMASIADAS_SOLICITUDES" ]
}
```


## 3. Códigos y estados

### 3.1 Registro y documentos (`POST /api/v1/auth/register`, `POST /api/v1/users/me/consents`)

| HTTP | `details[0]` | Cuándo | Qué hace la app |
|---|---|---|---|
| 201 | — | Cuenta creada | Iniciar sesión |
| 400 | — | JSON inválido (p. ej. falta `acceptsStudyParticipation`) | Error de la app |
| 422 | `CONSENTIMIENTO_REQUERIDO` | Falta una aceptación obligatoria | Mostrar el documento que falta |
| 422 | `PARTICIPACION_REQUERIDA` | Registro definitivo y no acepta participar | Explicar que Kora es solo para el estudio |
| 422 | `DOCUMENTO_NO_PUBLICADO` | Se envía la versión de un tipo que no tiene documento aprobado (p. ej. hoy, cualquier versión) | No enviar versiones de documentos que `/legal-documents` no lista |
| 422 | `VERSION_NO_VIGENTE` | Versión inexistente, vencida o cambiada durante el registro | Volver a leer `/legal-documents` |
| 422 | `VERSION_SIN_PARTICIPACION` | Versión del consentimiento informado con `acceptsStudyParticipation=false` | Error de la app |
| 422 | — | Correo ya registrado ("Ya existe una cuenta…") | Ofrecer iniciar sesión o recuperar la contraseña |
| 429 | `DEMASIADAS_SOLICITUDES` | Límite por IP (registro: 30 por hora) | Regla 6 de §1 |

### 3.2 Generación del primer plan

**Asíncrona (recomendada)**

| Petición | Respuesta |
|---|---|
| `POST /api/v1/users/me/plan/generation` | **202** con una operación nueva `{operationId, status: PENDING, terminal: false, weekStartDate, planId: null, reasonCode, message, retryable, pollAfterSeconds…}`. **200** si ya existe una operación (se devuelve esa, sin crear otra: C-09 y C-13) |
| `GET /api/v1/users/me/plan/generation/latest` y `/{operationId}` | 200 con la operación. `status`: `PENDING`, `PROCESSING`, `COMPLETED` (con `planId`), `FAILED` o `NO_FACTIBLE` (con `reasonCode` y `message`) |
| 422 `PARTICIPACION_NO_VIGENTE` | No participa, se retiró, la cuenta no está activa o (con el registro definitivo) no tiene un consentimiento válido |
| 422 `PERFIL_INCOMPLETO` | Sin perfil |
| 429 (`details=[]`) | Límite de generaciones por usuario y hora, con `Retry-After` |

Una operación creada **antes** de una retirada y todavía pendiente se devuelve tal cual (200) y termina en **`FAILED`, `reasonCode=NO_GENERABLE`**, sin publicar ningún plan (C1). Mobile la trata como cualquier `FAILED` sin reintento (`retryable=false`).

**Síncrona (`POST /api/v1/users/me/plan/generate`)**

| Respuesta | Cuándo |
|---|---|
| 201 | Plan publicado |
| 404 | Sin perfil ("Perfil del usuario no encontrado: …") |
| 422 `PARTICIPACION_NO_VIGENTE` | Igual que en la asíncrona |
| 422 sin código | Ya hay un plan activo para esa semana, u otras reglas del plan |
| **409** sin código | "Ya se esta preparando tu plan…": otra generación del mismo usuario está **en ejecución** (exclusión R-5, también contra el cierre semanal) |
| **409** `PLAN_CAMBIADO_DURANTE_LA_GENERACION` | El plan activo cambió mientras se preparaba: volver a leer `GET /users/me/plan/current` |
| **409** sin código | "La operacion choco con otra simultanea…": choque en la base entre dos peticiones simultáneas; consultar el estado |
| 429 (`details=[]`) | Límite por usuario y hora |

**Nota (C-10):** la exclusión de la síncrona actúa mientras otra generación **se ejecuta**, no mientras una asíncrona espera `PENDING`. Por eso C-10 publica (201) con una asíncrona pendiente. Cuando esa asíncrona se ejecute, termina **`COMPLETED` con el mismo `planId`** del plan ya publicado y no crea otro (`PlanGenerationOperationService`, rama `PlanAlreadyExistsException`). Si la persona se retiró antes, falla la elegibilidad y termina `FAILED`. Este comportamiento existía antes de esta orden. Por la regla 7 de §1, la app no debe combinar las dos rutas.

**Mensajes con el id interno (hallazgo de la revisión final, anterior a esta orden).** Algunos errores de generación que ya existían llevan el id interno de la propia cuenta en `message`:
- 404 "Perfil del usuario no encontrado: 5";
- 422 "El usuario 5 ya tiene un plan activo…";
- y otros del motor de planes.

No exponen datos de otras personas, y **los errores nuevos de C1 no los llevan**. La app **no** debe mostrar `message` de esos errores sin filtrar. La corrección está en [#103](https://github.com/Fitsense-Real/Backend/pull/103) (C8), abierta y sin fusionar (`08`).

### 3.3 429 y navegador (CORS)

- Las respuestas 429 llevan `Retry-After` (segundos) y `details[0]="DEMASIADAS_SOLICITUDES"` en registro, inicio de sesión, consentimientos y recuperación.
- **En `generation` hoy** (C-15), un navegador recibe `Access-Control-Allow-Origin` del origen autorizado, pero **no** `Access-Control-Expose-Headers`: **no puede leer `Retry-After`**. Una app nativa (Android) sí lo lee.
- [**#101**](https://github.com/Fitsense-Real/Backend/pull/101) (abierta, sin fusionar) expone solo `Retry-After`, solo a los orígenes de `FITSENSE_CORS_ORIGINS`. Un origen ajeno sigue sin recibir cabeceras CORS.
- Mientras no se fusione, la app web aplica la regla 6 de §1.

### 3.4 Recuperación de contraseña (sin cambios de contrato)

| Petición | Respuesta |
|---|---|
| `POST /api/v1/auth/password-reset/request` `{email}` | **202** siempre, con el mismo cuerpo, exista o no la cuenta |
| `POST /api/v1/auth/password-reset/confirm` `{token, newPassword}` | **204** (los JWT anteriores dejan de valer: hay que iniciar sesión); **400** "El codigo de recuperacion no es valido o ha caducado." (incorrecto, usado o caducado); **400** contraseña débil; **429** |

- El código tiene 43 caracteres y caduca en 30 min.
- **Hoy** un código anterior sigue valiendo hasta que se usa uno.
- Con [**#102**](https://github.com/Fitsense-Real/Backend/pull/102) (abierta) solo valdría el más reciente; la app puede avisar "usa el código del último correo".
- **Sin proveedor de correo no llega ningún correo** (`03`).

## 4. Pruebas locales con documentos sintéticos

Solo en un entorno **local y aislado**, nunca en uno con personas reales:

```properties
fitsense.consent.enforce-on-signup=true
fitsense.legal-documents.items[0].type=TERMS_OF_SERVICE
fitsense.legal-documents.items[0].version=T-EJEMPLO-2
fitsense.legal-documents.items[0].title=Condiciones de uso (EJEMPLO SINTETICO)
fitsense.legal-documents.items[0].url=https://documentos.ejemplo.invalid/condiciones-2
fitsense.legal-documents.items[0].status=PUBLISHED
fitsense.legal-documents.items[0].effective-from=2026-11-01
fitsense.legal-documents.items[0].approval-reference=SINTETICO
fitsense.legal-documents.items[1].type=PRIVACY_POLICY
fitsense.legal-documents.items[1].version=P-EJEMPLO-1
fitsense.legal-documents.items[1].title=Politica de privacidad (EJEMPLO SINTETICO)
fitsense.legal-documents.items[1].url=https://documentos.ejemplo.invalid/privacidad-1
fitsense.legal-documents.items[1].status=PUBLISHED
fitsense.legal-documents.items[1].effective-from=2026-01-01
fitsense.legal-documents.items[1].approval-reference=SINTETICO
fitsense.legal-documents.items[2].type=STUDY_PARTICIPATION
fitsense.legal-documents.items[2].version=CI-EJEMPLO-1
fitsense.legal-documents.items[2].title=Consentimiento informado (EJEMPLO SINTETICO)
fitsense.legal-documents.items[2].url=https://documentos.ejemplo.invalid/consentimiento-1
fitsense.legal-documents.items[2].status=PUBLISHED
fitsense.legal-documents.items[2].effective-from=2026-01-01
fitsense.legal-documents.items[2].approval-reference=SINTETICO
# Para la app web local:
fitsense.cors.allowed-origins=http://localhost:<puerto de la app>
# Sin IA ni gasto:
fitsense.ai.enabled=false
```

Como variables de entorno: `FITSENSE_CONSENT_ENFORCE_ON_SIGNUP=true`, `FITSENSE_CORS_ORIGINS`, `FITSENSE_AI_ENABLED=false`. Los documentos van como propiedades: con Spring, `FITSENSE_LEGAL_DOCUMENTS_ITEMS_0_TYPE=…`, etc.

**Las versiones, URL y referencias de ejemplo no son reales** y no pueden publicarse en ningún entorno con personas.
