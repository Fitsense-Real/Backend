# 02 · Autenticación, cuentas y consentimientos

**Rama auditada:** `generation` en `77b9df3` (copia de trabajo en `audit/preproduccion-backend`).
**Fecha:** 27/11/2026. **Método:** lectura del código, de las migraciones y de las pruebas, y ejecución de `mvn verify -Pintegration` (resultado en `01_INVENTARIO_FUNCIONAL_BACKEND.md` §1). No se modificó código, configuración ni datos.

> Leyenda de estados: **VERIFICADA** = implementada y comprobada por una prueba automatizada que pasó en esta ejecución; **SIN VALIDACIÓN INTEGRAL** = existe, pero falta un flujo completo real (proveedor real, despliegue o app); **PARCIAL**; **NO IMPLEMENTADA**; **CON ERROR**; **NO APLICA**.
> "Simulado" significa que la prueba usa un sustituto del proveedor externo (GreenMail como SMTP, un notificador en memoria, etc.).

---

## 1. Resumen ejecutivo

| Tema | Estado real |
|---|---|
| Registro, login y JWT | Implementados y verificados por pruebas de integración (Testcontainers). |
| **Límite de intentos en login y registro** | **NO IMPLEMENTADO.** El limitador por IP solo protege la recuperación de contraseña y la eliminación web. `POST /auth/login` y `POST /auth/register` no tienen límite ni bloqueo de cuenta. |
| Recuperación de contraseña | **Implementada y verificada con proveedor simulado (GreenMail).** No operativa en ningún entorno real: `fitsense.password-reset.delivery=none` y `fitsense.mail.transport=none` por omisión, sin proveedor contratado (P-10). |
| Cambio de contraseña con sesión iniciada | **NO IMPLEMENTADO** (solo existe el cambio vía código de recuperación). |
| Cierre de sesión en servidor / renovación de token | **NO IMPLEMENTADOS.** JWT de 7 días sin *refresh*; el "logout" solo puede ser local en Mobile. |
| Revocación de sesiones al cambiar la contraseña | **VERIFICADA** (V61 `token_version`, `RecuperacionPorCorreoIT.flujoCompleto`). |
| Verificación de correo | **NO IMPLEMENTADA.** |
| Eliminación de cuenta | **VERIFICADA** (app y web; borrado completo, alternativa A). |
| Consentimientos versionados (V63) | **PARCIAL.** El modelo, los endpoints y el historial existen y están probados, pero **la exigencia está apagada** y **no hay ningún documento aprobado** configurado. Hoy cualquiera puede registrarse por API sin aceptar condiciones ni privacidad. |
| Consentimiento informado de investigación | **PARCIAL.** Es una casilla booleana del cliente; el backend sella una versión fija de configuración (`MVP-1.0`) que **no corresponde a ningún documento aprobado** (P-6). |
| Separación usuario habitual / participante | **CON ERROR respecto a lo documentado.** Un usuario que no marca la participación **no recibe ningún plan** (ver §4.4): el uso del servicio queda condicionado a participar en la investigación. |

---

## 2. Autenticación y gestión de cuentas

| # | Funcionalidad | Estado | Evidencia | Observaciones |
|---|---|---|---|---|
| A1 | Registro | VERIFICADA | `AuthenticationController.register` → `RegistroConConsentimientos.registrar` (transacción única cuenta + consentimientos); `SignUpResource` (`@Email`, `@Size`); `ConsentimientoExigidoIT.registroExigido`, `SeguridadRolesIT.registroSinAdmin` | El rol siempre es `USER` (`User` constructor, línea 71). |
| A2 | Inicio de sesión | VERIFICADA | `UserCommandServiceImpl.handle(SignInCommand)`; `ContratoErroresApiIT.loginFallido` (401 idéntico para correo inexistente, clave errónea o cuenta inactiva) | — |
| A3 | Autenticación JWT | VERIFICADA | `TokenServiceImpl` (HS256, secreto ≥ 32 bytes o no arranca), `BearerAuthorizationRequestFilter` (recarga el usuario en cada petición, exige `ACTIVE` y `token_version` igual); `ContratoErroresApiIT.tokensInvalidos`, `caducidadConfigurada` | **S-2 sigue abierto:** el JWT lleva el claim `email` (`TokenServiceImpl:53`). |
| A4 | Renovación y expiración | PARCIAL | Expiración: `authorization.jwt.expiration.days=7`. **No existe endpoint de renovación** (`grep refresh` sin resultados). | Al caducar, Mobile debe volver a pedir la contraseña. Aceptable para el piloto si Mobile lo gestiona; documentarlo en el contrato. |
| A5 | Cierre de sesión | NO IMPLEMENTADA | No hay endpoint `logout` ni lista de revocación. La única revocación global es subir `token_version` (cambio de contraseña) o desactivar/eliminar la cuenta. | Un token robado sigue valiendo hasta 7 días. Tampoco se desvinculan los dispositivos push al cerrar sesión (no hay evento de servidor; ver `04` §5). |
| A6 | Eliminación de cuenta | VERIFICADA | `DELETE /api/v1/users/me` (contraseña + `confirm`, `LimiteDeReautenticacion`), `EliminacionDeCuentaService` (dos fases, `FOR UPDATE`, reanudación cada 5 min); web: `/api/v1/account-deletion/**`. `EliminacionDeCuentaIT` (`eliminacionCompleta`, `concurrentes`, `interrumpidaYReanudada`, `ningunaTablaSinCubrir`), `EliminacionWebIT` (GreenMail) | Borra **también** `user_consents` y todos los datos del estudio (decisiones P-1, P-3, P-4 abiertas). |
| A7 | Cambio de contraseña (autenticado) | NO IMPLEMENTADA | `User.changePassword` solo lo invoca `PasswordResetService.confirmar` (`grep changePassword`). | Requiere correo operativo para cambiar la contraseña. |
| A8 | Recuperación por correo | SIN VALIDACIÓN INTEGRAL | Ver §3. | Completa en código; sin proveedor real. |
| A9 | Verificación de correo | NO IMPLEMENTADA | No hay columna, token ni endpoint (`grep -i verif` en `iam`). | Riesgo: cuentas con correos ajenos o mal escritos → la recuperación no llega y se contamina la muestra. |
| A10 | Protección contra intentos abusivos | **PARCIAL** | `LimitePorIp` (20 / 15 min por IP y acción, en memoria) solo en `PasswordResetController` y `AccountDeletionWebController`; límite por cuenta de 3/h en recuperación; `LimiteDeReautenticacion` en la eliminación. **Login y registro: sin límite** (`grep limite.comprobar`). | **P0**: fuerza bruta de contraseñas y alta masiva de cuentas. |
| A11 | Validación de datos de registro | VERIFICADA | `SignUpResource` (correo ≤ 254, contraseña 8–72, nombre ≤ 100); `EmailAddress` normaliza a minúsculas; CHECK `ck_users_email_lower` (V1). `SeguridadRolesIT.entradaInvalida`, `ContratoErroresApiIT.entradaMalFormada` | Sin política de complejidad más allá de la longitud. Zona horaria: `UserTimezone` (validación en el VO). |
| A12 | Cuentas duplicadas | VERIFICADA | `existsByEmail` → `EmailAlreadyRegisteredException` (422) + `uq_users_email` (V1); carrera → `DataIntegrityViolationException` → 409 (`GlobalExceptionHandler:113`). | El mensaje 422 incluye el correo y confirma que existe (enumeración; decisión D14 ya tomada: se mantiene por usabilidad). |
| A13 | Sesiones tras cambiar la contraseña | VERIFICADA | V61 `token_version`; `User.changePassword` incrementa; filtro compara. `RecuperacionPorCorreoIT.flujoCompleto` (línea 169: el JWT anterior devuelve 401) | Solo aplica al cambio por recuperación (único camino que existe). |

---

## 3. Recuperación de contraseña: diagnóstico

**Conclusión: ya está implementada.** No hay que desarrollarla de nuevo. Falta **operarla** (proveedor de correo y decisiones) y **adaptar Mobile**.

| Pregunta de la orden | Respuesta | Evidencia |
|---|---|---|
| ¿Existen endpoints? | Sí. `POST /api/v1/auth/password-reset/request` (siempre 202, mismo cuerpo) y `POST /api/v1/auth/password-reset/confirm` (204 o 400 con el mismo mensaje). Públicos. | `PasswordResetController` |
| ¿Integración de correo? | Sí: `EnvioDeCorreo` con transportes `smtp`, `resend` y `brevo` (`TransportesDeCorreo`), envío **tras el commit y fuera del hilo** (`trasConfirmar`), cola en memoria de 200 y un reintento. | `shared/infrastructure/mail/*`, `PasswordResetNotifiers.porCorreo` |
| ¿Se generan tokens? | Sí: 32 bytes de `SecureRandom`, Base64url (43 caracteres). | `PasswordResetService.solicitar` |
| ¿Caducidad y un solo uso? | Sí. TTL 30 min (rango 5–120 o no arranca); `used_at`; al confirmar se invalidan los demás pendientes; bloqueo de fila (`bloquearPorHash`) contra doble confirmación. | `PasswordResetToken.usableAt`, `RecuperacionContrasenaIT.unSoloUso`, `caducadoEInventado` |
| ¿Almacenamiento seguro? | Sí: solo el SHA-256 (`token_hash`, 64 hex). | V44_2, `PasswordResetToken` |
| ¿Evita revelar cuentas? | Sí en la respuesta (mismo 202). Diferencia de tiempos residual menor: con cuenta hay 2 consultas + 1 inserción; sin cuenta, 1 consulta. El correo ya no se envía en el hilo de la petición. | `RecuperacionContrasenaIT.sinEnumeracion`, `RecuperacionPorCorreoIT.sinEnumeracion` |
| ¿Invalida sesiones anteriores? | Sí (V61). | `RecuperacionPorCorreoIT.flujoCompleto` |
| ¿Abuso? | 3 solicitudes/h por cuenta + 20/15 min por IP (en memoria, una instancia). | `RecuperacionContrasenaIT.limite`, `RecuperacionPorCorreoIT.limitePorIp` |
| ¿Modo de desarrollo? | `dev-log` escribe el código en el log **solo** con perfil `local`/`local-dev`; en otro perfil la app no arranca. | `PasswordResetNotifiersTest` |

**Qué falta para que una persona recupere su contraseña desde Mobile:**

1. **Backend / operación (sin código nuevo):** elegir proveedor y dominio remitente (P-10), configurar en Railway `FITSENSE_MAIL_TRANSPORT`, `FITSENSE_MAIL_FROM`, `FITSENSE_MAIL_API_KEY` (secreto) y `FITSENSE_PASSWORD_RESET_DELIVERY=email`; configurar SPF/DKIM del dominio; una prueba real de entrega con una cuenta de prueba.
2. **Decisión P-11:** código (actual, sin plantilla) o enlace (`FITSENSE_PASSWORD_RESET_LINK_TEMPLATE=https://…{token}`, exige una página o *deep link*). Con código, el usuario debe copiar 43 caracteres: recomendable enlace profundo a la app.
3. **Mobile:** pantalla "olvidé mi contraseña" (request), pantalla de confirmación (token + nueva contraseña, 8–72), manejo de 202/204/400/429 y, tras el éxito, descartar el JWT guardado (ya no vale).
4. **Mejora opcional (P2):** igualar tiempos de respuesta (realizar la consulta/inserción de forma asíncrona o con retardo constante).

**Propuesta de solución (reutilizando la arquitectura):** no se requiere código nuevo para el flujo básico. Para el piloto, añadir solo (a) el endpoint de cambio de contraseña autenticado reutilizando `HashingService`, `User.changePassword` y `AvisosDeCuenta.contrasenaCambiada`, con reautenticación por `LimiteDeReautenticacion`, y (b) el límite de intentos en login/registro reutilizando `LimitePorIp` (ver `06`, P0-02). Ambos sin migraciones.

---

## 4. Términos, privacidad y consentimiento

### 4.1 Qué existe (V63)

- Tabla `user_consents` (`V63__consentimientos.sql`): `consent_type` (`STUDY_PARTICIPATION`, `TERMS_OF_SERVICE`, `PRIVACY_POLICY`), `document_version`, `granted_at`, `affirmative_action` (`CASILLA_REGISTRO`, `BOTON_ACEPTAR`, `REGISTRO_ANTERIOR_A_V63`), `source` (`APP`, `WEB`, `MIGRACION`), `withdrawn_at`.
- Servicio `ConsentimientoService` y endpoints `GET/POST /api/v1/users/me/consents`, `POST /api/v1/users/me/consents/{type}/withdraw`; además `POST /api/v1/users/me/withdraw` (anterior) también deja la retirada.
- Columnas heredadas en `users`: `consent_granted_at`, `consent_version`, `is_study_participant`, `participant_code` (seudónimo `P-xxx` de secuencia sin reutilización, V60), `enrolled_at`, `withdrawn_at`.

### 4.2 Lo que el backend realmente exige hoy

| # | Requisito de la orden | Estado | Evidencia |
|---|---|---|---|
| C1 | Aceptación obligatoria de términos vigentes | **NO EXIGIDA** (implementada pero apagada) | `fitsense.consent.enforce-on-signup=${FITSENSE_CONSENT_ENFORCE_ON_SIGNUP:false}`; `fitsense.consent.terms-version` vacío. Con exigencia encendida funciona: `ConsentimientoExigidoIT.registroExigido` (422 sin aceptar, sin cuenta a medias). |
| C2 | Registro de la decisión de privacidad | **NO EXIGIDA** (igual que C1) | `fitsense.consent.privacy-version` vacío. Sin versión, cualquier aceptación se rechaza (422): `ConsentimientoYEdadIT.sinDocumentoAprobado`. |
| C3 | Consentimiento informado independiente | **PARCIAL** | Es independiente en el modelo (tipo propio). Pero: (a) es una casilla `acceptsStudyParticipation` sin versión enviada por el cliente; (b) la versión la pone el servidor (`fitsense.study.consent-version:MVP-1.0`), aunque no hay documento aprobado (P-6); (c) no se registra qué texto vio la persona (ni hash ni versión mostrada). |
| C4 | Versión de cada documento | PARCIAL | Términos/privacidad: versión exacta validada contra la vigente. Investigación: versión impuesta por configuración, no confirmada por el cliente. |
| C5 | Fecha y evidencia | PARCIAL | `granted_at`, `affirmative_action`, `source`. No hay IP, *user agent*, versión de la app ni huella del documento. `StudyEnrollment.enrolledNow` usa `OffsetDateTime.now()` (zona del servidor) en vez del `Clock` inyectado. |
| C6 | Negativas y revocaciones | PARCIAL | Revocación del consentimiento de investigación: sí (`withdrawn_at`, `ConsentimientoYEdadIT.retirada`). **La negativa no se registra** (si la casilla es `false`, no hay fila: `registroDeParticipacion`). Términos/privacidad no se revocan (solo eliminando la cuenta). |
| C7 | Separación usuario habitual / participante | **CON ERROR** | Ver §4.4. |
| C8 | Protección de datos personales y de salud | PARCIAL | Ver `04` §6: S-1 (salida de la IA en logs INFO) sigue abierto; `health_notes` y `goal_text` (texto libre) se envían a Replicate (`PlanInputSnapshot:59-60,519`, P-13); fecha de nacimiento completa (P-8). |
| C9 | Edad mínima | VERIFICADA | `fitsense.profile.minimum-age=18`; `ConsentimientoYEdadIT.edadMinima`. Se valida al crear el perfil, no al registrarse. |

### 4.3 ¿Se puede registrar alguien por API sin aceptaciones? ¿Se pueden falsificar los indicadores?

- **Sí, hoy cualquiera puede registrarse sin aceptar términos ni privacidad.** Con la configuración por omisión, `POST /api/v1/auth/register` con solo correo, contraseña y nombre devuelve 201 (`ConsentimientoYEdadIT.registroDeParticipacion`, que además comprueba `enforcedOnSignup=false` y versiones vigentes nulas). El *smoke* `scripts/smoke-local.sh` registra igual.
- **Indicador de participación:** lo fija el cliente con `acceptsStudyParticipation=true`. Es la declaración de la propia persona, así que no es "falsificación" en sentido estricto, pero **el backend no puede probar que se mostró el consentimiento informado**: basta una petición `curl` para quedar inscrito con versión `MVP-1.0`.
- **Lo que no se puede falsificar desde el cliente:** el rol (`ADMIN` no llega por el comando; `SeguridadRolesIT.registroSinAdmin`), el `participant_code` (secuencia del servidor), la fecha (`granted_at` del servidor), la reinscripción tras retirarse (`aceptar(STUDY_PARTICIPATION)` → 422) y la aceptación de una versión no vigente (422).

### 4.4 Hallazgo: participar en la investigación es condición para recibir planes

`User.isEligibleForPlanGeneration()` = `canSignIn() && enrollment.isActiveParticipant()` (`User.java:113-115`). Lo usan:
- `PlanGenerationOperationService.validateCanGenerate` (línea 182): 422 *"En este momento no es posible preparar tu rutina…"*;
- `WeeklyTrainingPlanCommandServiceImpl.prepare` (línea 121) en el cierre semanal.

Por tanto, **quien no marca la casilla o se retira no recibe ninguna rutina**, aunque `CONSENTIMIENTO_Y_EDAD.md` afirma que "se puede usar la app sin participar". Impacto para el estudio:
- el consentimiento de investigación deja de ser libre si el servicio depende de él (riesgo ético);
- la población "usuarios habituales" que la orden quiere separar no existe funcionalmente;
- ninguna prueba del perfil por omisión cubre este comportamiento (solo `CorreccionReplicateEvidenciaIT`, desactivada por propiedad).

No se modifica. La decisión (mantener "app = estudio" y declararlo en el consentimiento, o desacoplar la generación de la participación) corresponde al equipo del estudio y al comité. Se incluye como **P0-04** en `06`.

### 4.5 Qué falta (sin implementarlo ahora)

1. Documentos aprobados (términos, privacidad, consentimiento informado) y su versión (P-6), y la decisión de activar `FITSENSE_CONSENT_ENFORCE_ON_SIGNUP=true` (P-7). Con eso, C1/C2 pasan a exigirse sin código nuevo.
2. Que el cliente envíe la **versión del consentimiento informado** mostrada y que el backend la valide igual que términos/privacidad (hoy `SignUpResource` no tiene ese campo). Requiere cambio de API coordinado con Mobile.
3. Registrar la **negativa** (o, al menos, la versión ofrecida) si el protocolo lo exige.
4. Decidir qué se conserva como prueba tras la eliminación (P-4) y tras la retirada (P-2).
5. Decidir el acoplamiento participación/servicio (§4.4).
6. Exigir, si el protocolo lo pide, una evaluación previa de aptitud (p. ej. un cuestionario tipo PAR-Q). **No existe** en el backend (`grep -i parq` sin resultados); solo `health_notes` libre.

---

## 5. Dependencias con Mobile

| Tema | Qué necesita Mobile |
|---|---|
| Recuperación | Pantallas de solicitud y confirmación; *deep link* si se elige enlace; descartar el JWT tras el cambio. |
| Consentimientos | Mostrar los textos aprobados, enviar `acceptedTermsVersion`, `acceptedPrivacyVersion` (ya aceptados por la API) y, tras el cambio de API, la versión del consentimiento informado; leer `GET /users/me/consents` para pedir la aceptación de versiones nuevas. |
| Sesión | Manejar el 401 al caducar (7 días) y el cierre de sesión local (borrar JWT y llamar a `DELETE /users/me/push-devices/{installationId}`). |
| JWT | Confirmar si la app lee el claim `email` antes de quitarlo (S-2). |
