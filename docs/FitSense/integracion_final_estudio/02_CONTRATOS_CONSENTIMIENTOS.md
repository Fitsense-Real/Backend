# 02 · Contrato de documentos y consentimientos

**PR:** [#96](https://github.com/Fitsense-Real/Backend/pull/96) (Backend-C1), sobre el sistema de #91. **No se ha creado otro sistema de consentimientos.**

## 1. Qué devuelve de verdad `GET /api/v1/legal-documents`

Se ha comprobado en el código (`LegalDocumentsController`, `DocumentosLegales`) y ejecutándolo (§4, ejemplo 1).

- **No sirve el contenido del documento.** Devuelve metadatos y una **URL autorizada**: `type`, `version`, `title`, `url` (`https` obligatoria), `status` (siempre `PUBLISHED`) y `effectiveFrom`. Además, `enforcedOnSignup` y `privacyRecord`.
- Sin autenticación y con `Cache-Control: no-cache`.
- Solo salen los `PUBLISHED` que ya entraron en vigor, uno por tipo: el de `effective-from` más reciente. Los borradores (`DRAFT`) no salen nunca. La referencia de aprobación (`approval-reference`) no se publica.
- **Hoy la lista está vacía:** no hay documentos aprobados.

### 1.1 Fuente autorizada y versionada

La fuente es la configuración desplegada (`fitsense.legal-documents.items[i].*`), no una tabla. Cada cambio queda en el historial de despliegues. Al arrancar, un documento mal declarado **impide arrancar**:
- un `PUBLISHED` necesita `title`, `url https`, `effective-from` y `approval-reference`;
- no puede haber dos versiones publicadas iguales del mismo tipo;
- la versión del consentimiento informado tiene como mucho 20 caracteres.

**Qué asocia la versión con lo que vio la persona:** el par `version` + `url`. El backend no guarda el texto, así que el documento que se publique en esa URL **no puede cambiar** sin cambiar de versión. Esto es una obligación de quien publique los documentos (`08`, A-3). Para poder demostrarlo más adelante, conviene guardar el PDF y su SHA-256 en el expediente del comité.

**No se ha publicado ningún documento ni se ha inventado ninguna versión oficial.** Las pruebas usan documentos **sintéticos** (`PRUEBA-*`, `*-EJEMPLO-*`, referencia `SINTETICO`) en PostgreSQL de Testcontainers.

## 2. Campos del registro (`POST /api/v1/auth/register`)

| Campo | Tipo | Hoy (exigencia apagada) | Registro definitivo (`enforce-on-signup=true`) |
|---|---|---|---|
| `acceptsStudyParticipation` | booleano, **obligatorio en el JSON** (sin él, 400) | `true` inscribe; `false` no inscribe | **Tiene que ser `true`**; si no, 422 `PARTICIPACION_REQUERIDA` (opción A) |
| `acceptedTermsVersion` | texto ≤ 40, opcional | Si llega, tiene que ser la vigente | Obligatorio y vigente |
| `acceptedPrivacyVersion` | texto ≤ 40, opcional | Ídem | Obligatorio y vigente |
| `acceptedStudyConsentVersion` | texto ≤ 20, opcional | Si llega: hace falta participar y un documento aprobado | Obligatorio y vigente |

Garantías:
- **Transaccionalidad.** Todo se valida antes de crear la cuenta, y la cuenta, la participación y las filas de aceptación se guardan en una sola transacción (`RegistroConConsentimientos`). Un rechazo no deja ninguna cuenta a medias.
- **Ninguna aceptación por omisión.** Sin la casilla no hay fila de investigación. Con la exigencia apagada, la casilla se guarda como `CASILLA_SIN_DOCUMENTO_APROBADO`, que **no** es evidencia válida (`01` §2).
- **`MVP-1.0` no es un consentimiento válido.** Es solo la etiqueta que se usa cuando no hay documento. Enviarla como versión da 422 `DOCUMENTO_NO_PUBLICADO`, y con el registro definitivo no puede asignarse.

## 3. Errores del contrato

| Caso de la orden | Respuesta | `details[0]` | Qué hace Mobile |
|---|---|---|---|
| Documento no publicado (se envía una versión de un tipo sin documento aprobado) | 422 | `DOCUMENTO_NO_PUBLICADO` | No enviar versiones de documentos que `/legal-documents` no lista |
| Versión incorrecta, inexistente o vencida | 422 | `VERSION_NO_VIGENTE` | Volver a leer `/legal-documents`, mostrar la vigente y pedir otra vez la aceptación |
| Cambio de versión durante el registro | 422 | `VERSION_NO_VIGENTE` (el mensaje dice cuál es la vigente) | Igual que el anterior |
| Consentimiento obligatorio ausente (condiciones, privacidad o consentimiento informado) | 422 | `CONSENTIMIENTO_REQUERIDO` | Mostrar el documento que falta |
| Participación rechazada (registro definitivo) | 422 | `PARTICIPACION_REQUERIDA` | Explicar que Kora es solo para el estudio; no se crea la cuenta |
| Versión del consentimiento informado sin participar | 422 | `VERSION_SIN_PARTICIPACION` | Error de la app: no enviarla sin la casilla |
| Participante no elegible al pedir un plan (no participa, retirado, cuenta inactiva o sin consentimiento válido) | 422 | `PARTICIPACION_NO_VIGENTE` | Mostrar el mensaje; no reintentar |
| Menor de 18 al crear el perfil | 422 (regla de perfil) | — | Ya existente (`ConsentimientoYEdadIT`) |
| Exceso de intentos | **429** con `Retry-After` (s) | `DEMASIADAS_SOLICITUDES` | Esperar; no reintentar solo |
| Solicitud repetida (mismo correo) | 422 «Ya existe una cuenta registrada con el correo…» | — | Ofrecer iniciar sesión o recuperar la contraseña. **No duplica** cuenta ni evidencias (`ConsentimientoExigidoIT.solicitudRepetida`) |
| JSON incompleto (falta `acceptsStudyParticipation`) | 400 «El cuerpo de la solicitud no es valido.» | — | Enviar siempre el booleano |

**Datos personales:** ningún mensaje lleva identificadores internos ni datos de la cuenta. La versión del documento vigente sí aparece, porque es pública. El mensaje de «solicitud repetida» repite el correo que la propia app acaba de enviar: confirma que existe una cuenta con él. Es un riesgo de enumeración que ya existía. Lo acotan los 30 registros por IP y hora, y lo eliminaría la verificación de correo (P1-10), que sigue **pendiente**.

**Compatibilidad:** con la configuración por omisión no cambia ningún estado HTTP ni mensaje. Solo cambia `details`, que antes iba vacío. Única excepción: el mensaje de la ruta síncrona para un no participante ya no lleva el id interno.

## 4. Ejemplos HTTP completos para Mobile

Se obtuvieron **ejecutando** el backend de la rama C1, con MockMvc y PostgreSQL de Testcontainers, y con **documentos sintéticos de ejemplo** y el registro definitivo encendido solo en esa ejecución. De las cabeceras de respuesta solo se muestran `Cache-Control` y `Retry-After`. `<JWT>` sustituye al token real.

**Ninguna versión, título ni URL es real.** Las versiones reales serán las que se aprueben (`08`).

### 1. Documentos vigentes (sin autenticación)

```http
GET /api/v1/legal-documents HTTP/1.1
```

```http
HTTP/1.1 200
Cache-Control: no-cache
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
    "effectiveFrom" : "2026-01-01"
  }, {
    "type" : "TERMS_OF_SERVICE",
    "version" : "T-EJEMPLO-2",
    "title" : "Condiciones de uso (EJEMPLO SINTETICO)",
    "url" : "https://documentos.ejemplo.invalid/condiciones-2",
    "status" : "PUBLISHED",
    "effectiveFrom" : "2026-11-01"
  }, {
    "type" : "PRIVACY_POLICY",
    "version" : "P-EJEMPLO-1",
    "title" : "Politica de privacidad (EJEMPLO SINTETICO)",
    "url" : "https://documentos.ejemplo.invalid/privacidad-1",
    "status" : "PUBLISHED",
    "effectiveFrom" : "2026-01-01"
  } ]
}
```

### 2. Registro válido (opción A, registro definitivo)

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
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
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
  "enrolledAt" : "2026-11-28T02:24:59.223378773Z",
  "withdrawnAt" : null,
  "lastLoginAt" : null,
  "role" : "USER"
}
```

### 3a. Registro rechazado: no acepta participar

```http
POST /api/v1/auth/register HTTP/1.1
Content-Type: application/json

{
  "email" : "beto@ejemplo.invalid",
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
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Content-Type: application/json

{
  "timestamp" : "2026-11-28T02:24:59.332117306Z",
  "status" : 422,
  "error" : "Unprocessable Entity",
  "message" : "Kora solo esta disponible para las personas que participan en el estudio. Para registrarte tienes que aceptar participar.",
  "details" : [ "PARTICIPACION_REQUERIDA" ]
}
```

### 3b. Registro rechazado: participa sin la versión del consentimiento informado

```http
POST /api/v1/auth/register HTTP/1.1
Content-Type: application/json

{
  "email" : "beto@ejemplo.invalid",
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
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Content-Type: application/json

{
  "timestamp" : "2026-11-28T02:24:59.366963501Z",
  "status" : 422,
  "error" : "Unprocessable Entity",
  "message" : "Para participar en el estudio tienes que aceptar la version vigente del consentimiento informado.",
  "details" : [ "CONSENTIMIENTO_REQUERIDO" ]
}
```

### 3c. Registro rechazado: falta `acceptsStudyParticipation` (campo obligatorio)

```http
POST /api/v1/auth/register HTTP/1.1
Content-Type: application/json

{
  "email" : "beto@ejemplo.invalid",
  "password" : "Clave-de-ejemplo-1",
  "firstName" : "Beto"
}
```

```http
HTTP/1.1 400
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Content-Type: application/json

{
  "timestamp" : "2026-11-28T02:24:59.372839141Z",
  "status" : 400,
  "error" : "Bad Request",
  "message" : "El cuerpo de la solicitud no es valido.",
  "details" : [ ]
}
```

### 4. Cambio de versión durante el registro (la app mostró `T-EJEMPLO-1`; mientras tanto entró en vigor `T-EJEMPLO-2`)

```http
POST /api/v1/auth/register HTTP/1.1
Content-Type: application/json

{
  "email" : "beto@ejemplo.invalid",
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
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Content-Type: application/json

{
  "timestamp" : "2026-11-28T02:24:59.380070805Z",
  "status" : 422,
  "error" : "Unprocessable Entity",
  "message" : "La version aceptada de TERMS_OF_SERVICE no es la vigente (T-EJEMPLO-2).",
  "details" : [ "VERSION_NO_VIGENTE" ]
}
```

### 5. Inicio de sesion

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
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Content-Type: application/json

{
  "userId" : 1,
  "email" : "ana@ejemplo.invalid",
  "fullName" : "Ana",
  "token" : "<JWT>",
  "role" : "USER"
}
```

### 6. Historial de aceptación

```http
GET /api/v1/users/me/consents HTTP/1.1
Authorization: Bearer <JWT>
```

```http
HTTP/1.1 200
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
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
    "grantedAt" : "2026-11-28T02:24:59.302604Z",
    "affirmativeAction" : "CASILLA_REGISTRO",
    "source" : "APP",
    "withdrawnAt" : null
  }, {
    "type" : "TERMS_OF_SERVICE",
    "documentVersion" : "T-EJEMPLO-2",
    "grantedAt" : "2026-11-28T02:24:59.307698Z",
    "affirmativeAction" : "CASILLA_REGISTRO",
    "source" : "APP",
    "withdrawnAt" : null
  }, {
    "type" : "PRIVACY_POLICY",
    "documentVersion" : "P-EJEMPLO-1",
    "grantedAt" : "2026-11-28T02:24:59.308150Z",
    "affirmativeAction" : "CASILLA_REGISTRO",
    "source" : "APP",
    "withdrawnAt" : null
  } ]
}
```

### 7. Retirada del estudio

```http
POST /api/v1/users/me/consents/STUDY_PARTICIPATION/withdraw HTTP/1.1
Authorization: Bearer <JWT>
```

```http
HTTP/1.1 204
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
```

### 8. Historial tras la retirada (`withdrawnAt` relleno)

```http
GET /api/v1/users/me/consents HTTP/1.1
Authorization: Bearer <JWT>
```

```http
HTTP/1.1 200
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
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
    "grantedAt" : "2026-11-28T02:24:59.302604Z",
    "affirmativeAction" : "CASILLA_REGISTRO",
    "source" : "APP",
    "withdrawnAt" : "2026-11-28T02:24:59.626407Z"
  }, {
    "type" : "TERMS_OF_SERVICE",
    "documentVersion" : "T-EJEMPLO-2",
    "grantedAt" : "2026-11-28T02:24:59.307698Z",
    "affirmativeAction" : "CASILLA_REGISTRO",
    "source" : "APP",
    "withdrawnAt" : null
  }, {
    "type" : "PRIVACY_POLICY",
    "documentVersion" : "P-EJEMPLO-1",
    "grantedAt" : "2026-11-28T02:24:59.308150Z",
    "affirmativeAction" : "CASILLA_REGISTRO",
    "source" : "APP",
    "withdrawnAt" : null
  } ]
}
```

### 9. Pedir un plan tras la retirada (ruta asíncrona; cuenta con perfil)

```http
POST /api/v1/users/me/plan/generation HTTP/1.1
Authorization: Bearer <JWT>
```

```http
HTTP/1.1 422
Content-Type: application/json

{
  "timestamp" : "2026-11-28T02:26:04.174966844Z",
  "status" : 422,
  "error" : "Unprocessable Entity",
  "message" : "En este momento no es posible preparar tu rutina. Si persiste, contacta al equipo del estudio.",
  "details" : [ "PARTICIPACION_NO_VIGENTE" ]
}
```

### 10. Pedir un plan tras la retirada (ruta síncrona; cuenta con perfil)

```http
POST /api/v1/users/me/plan/generate HTTP/1.1
Authorization: Bearer <JWT>
```

```http
HTTP/1.1 422
Content-Type: application/json

{
  "status" : 422,
  "error" : "Unprocessable Entity",
  "message" : "Tu cuenta no tiene una participacion activa en el estudio: Kora no puede preparar rutinas nuevas. Si crees que es un error, contacta al equipo del estudio.",
  "details" : [ "PARTICIPACION_NO_VIGENTE" ]
}
```

> El texto de la ruta síncrona es el de la última versión de C1 (`035e961`). La captura original llevaba el id interno de la cuenta ("El usuario 1…") y se corrigió en esa versión. **El perfil se comprueba antes que la participación.** Sin perfil, la asíncrona responde 422 «Completa tu perfil antes de preparar tu primera rutina.» y la síncrona, 404 «Perfil del usuario no encontrado». Ninguna de las dos lleva código; ver §3.

### 11. Recuperación: solicitud (misma respuesta exista o no la cuenta)

```http
POST /api/v1/auth/password-reset/request HTTP/1.1
Content-Type: application/json

{
  "email" : "ana@ejemplo.invalid"
}
```

```http
HTTP/1.1 202
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Content-Type: application/json

{
  "message" : "Si el correo corresponde a una cuenta, recibiras un codigo para restablecer tu contrasena."
}
```

### 12. Recuperación: código incorrecto, usado o caducado

```http
POST /api/v1/auth/password-reset/confirm HTTP/1.1
Content-Type: application/json

{
  "token" : "codigo-que-no-existe",
  "newPassword" : "Clave-nueva-de-ejemplo-2"
}
```

```http
HTTP/1.1 400
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Content-Type: application/json

{
  "timestamp" : "2026-11-28T02:24:59.804557181Z",
  "status" : 400,
  "error" : "Bad Request",
  "message" : "El codigo de recuperacion no es valido o ha caducado.",
  "details" : [ ]
}
```

### 13. Exceso de intentos de registro desde la misma IP (429)

```http
POST /api/v1/auth/register HTTP/1.1
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
Retry-After: 899
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Content-Type: application/json

{
  "timestamp" : "2026-11-28T02:24:59.823312533Z",
  "status" : 429,
  "error" : "Too Many Requests",
  "message" : "Demasiadas solicitudes desde esta conexion. Espera antes de volver a intentarlo.",
  "details" : [ "DEMASIADAS_SOLICITUDES" ]
}
```

## 5. Coordinación con Mobile

- **El backend devuelve una URL autorizada, no el texto.** Mobile abre la URL, en el navegador o en una vista web, y envía la `version` que mostró.
- La casilla de participación tiene que empezar **sin marcar**, y `acceptsStudyParticipation` hay que enviarlo siempre.
- **El registro definitivo no se activa** (`FITSENSE_CONSENT_ENFORCE_ON_SIGNUP=false`) hasta que:
  1. haya documentos formalmente aprobados, con su referencia;
  2. exista una compilación de Mobile que envíe las tres versiones y trate los cinco códigos de §3;
  3. el equipo fije la fecha.

  Activarlo antes impediría inscribir a nadie con la app actual.
- No se asume que haya pantallas nuevas fusionadas en Mobile.

## 6. Estado

| Punto | Clasificación |
|---|---|
| Contrato y códigos de error | **RESUELTO** (en C1, sin fusionar) |
| Mecanismo de documentos versionados | **PREPARADO SIN ACTIVAR** (lista vacía) |
| Documentos aprobados (condiciones, privacidad, consentimiento informado) | **BLOQUEADO POR APROBACIÓN** |
| Registro definitivo | **PREPARADO SIN ACTIVAR**: necesita los documentos aprobados y Mobile |
| Verificación de correo para evitar la enumeración | **PENDIENTE** (P1-10, necesita una migración) |
