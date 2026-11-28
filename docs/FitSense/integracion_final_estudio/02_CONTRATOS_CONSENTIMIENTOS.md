# 02 · Contrato de documentos y consentimientos

**PR:** [#96](https://github.com/Fitsense-Real/Backend/pull/96) (Backend-C1), **fusionada** (`434a9dc0`), sobre el sistema de #91. **No se ha creado otro sistema de consentimientos.**

## 1. Qué devuelve de verdad `GET /api/v1/legal-documents`

Se ha comprobado en el código (`LegalDocumentsController`, `DocumentosLegales`) y ejecutándolo (§4, ejemplo 1).

- **No sirve el contenido del documento.** Devuelve metadatos y una **URL autorizada**: `type`, `version`, `title`, `url` (`https` obligatoria), `status` (siempre `PUBLISHED`) y `effectiveFrom`. Además, `enforcedOnSignup` y `privacyRecord`.
- Sin autenticación y con `Cache-Control: no-cache`.
- Solo salen los `PUBLISHED` que ya entraron en vigor, uno por tipo: el de `effective-from` más reciente. Los borradores (`DRAFT`) no salen nunca. La referencia de aprobación (`approval-reference`) no se publica.
- **`requiredForSignup`** por documento (revisión final): indica si el registro exige su versión vigente. Con la exigencia apagada, `false`; con la opción A y el registro definitivo, los tres. Así se identifican el tipo (`type`), la versión vigente (`version`), la URL (`url`), el estado (`status`) y si es obligatorio, sin interpretar nada.
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
| Pedir un plan sin perfil (ruta asíncrona) | 422 | `PERFIL_INCOMPLETO` | Llevar al perfil. La síncrona responde 404 |
| Menor de 18 al crear el perfil | 422 (regla de perfil) | — | Ya existente (`ConsentimientoYEdadIT`) |
| Exceso de intentos | **429** con `Retry-After` (s) | `DEMASIADAS_SOLICITUDES` | Esperar; no reintentar solo |
| Solicitud repetida (mismo correo) | 422 «Ya existe una cuenta registrada con el correo…» | — | Ofrecer iniciar sesión o recuperar la contraseña. **No duplica** cuenta ni evidencias (`ConsentimientoExigidoIT.solicitudRepetida`) |
| JSON incompleto (falta `acceptsStudyParticipation`) | 400 «El cuerpo de la solicitud no es valido.» | — | Enviar siempre el booleano |

**Datos personales:** ningún mensaje lleva identificadores internos ni datos de la cuenta. La versión del documento vigente sí aparece, porque es pública. El mensaje de «solicitud repetida» repite el correo que la propia app acaba de enviar: confirma que existe una cuenta con él. Es un riesgo de enumeración que ya existía. Lo acotan los 30 registros por IP y hora, y lo eliminaría la verificación de correo (P1-10), que sigue **pendiente**.

**Compatibilidad:** con la configuración por omisión no cambia ningún estado HTTP ni mensaje. Solo cambia `details`, que antes iba vacío. Única excepción: el mensaje de la ruta síncrona para un no participante ya no lleva el id interno.

## 4. Ejemplos HTTP completos para Mobile

Están en **`09_CONTRATO_MOBILE.md`**, capturados de nuevo con el código fusionado (`cca9fc21`), con servidor HTTP real y documentos sintéticos.

La primera captura, hecha con una cabeza anterior de C1 y MockMvc, se sustituyó porque no tenía `requiredForSignup` ni `PERFIL_INCOMPLETO`.

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
| Contrato y códigos de error | **RESUELTO** (integrado: #96) |
| Mecanismo de documentos versionados | **PREPARADO SIN ACTIVAR** (lista vacía) |
| Documentos aprobados (condiciones, privacidad, consentimiento informado) | **BLOQUEADO POR APROBACIÓN** |
| Registro definitivo | **PREPARADO SIN ACTIVAR**: necesita los documentos aprobados y Mobile |
| Verificación de correo para evitar la enumeración | **PENDIENTE** (P1-10, necesita una migración) |
