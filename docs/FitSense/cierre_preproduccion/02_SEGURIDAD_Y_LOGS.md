# 02 · Seguridad y logs (Backend-B1)

- **Rama:** `preproduccion/b1-seguridad-logs-limites`, sobre `generation` en `77b9df3`.
- **Auditoría de referencia:** P0-01, P0-02, P1-04, P1-06 y P2-06.
- **Restricciones respetadas:** no se cambian *prompts*, validadores, reglas O20/V65 ni fórmulas. No hay migraciones ni llamadas a Replicate.

## 1. P0-01 · Logs sin datos sensibles (S-1): **RESUELTO**

### Qué se implementó

`shared/infrastructure/logging/DetalleSensible` es un guardián único para los logs. Por omisión, los datos sensibles salen en **forma técnica**:

| Dato | Antes | Ahora (por omisión) |
|---|---|---|
| Salida de la IA (`ReplicateTrainingPlanGenerator`, ruta asíncrona y síncrona) | Texto completo en INFO | `caracteres=N huella=<12 hex>` |
| Cuerpo crudo de Replicate (DEBUG) | Texto completo | Igual que arriba, también en DEBUG |
| Ejercicios con dolor (`DOLOR_REGISTRADO`, `DOLOR_FAMILIA`, `DOLOR_FAMILIA_NO_CALCULADA`, `DOLOR_IDENTIFICADO_DESPUES`, `DOLOR_REACTIVADO`) | Ids, mapas variante←origen | `n=K`, `[omitido]` |
| `DOLOR_PERSISTENTE`, `EXCLUSIONES_SEGURIDAD`, `REGENERACION_SEGURA`, `REPARACION_SEGURIDAD` | Ids excluidos y motivos | Recuentos |
| `REGENERACION_SEGURA_NO_FACTIBLE` | Motivo en texto | Código del motivo; el texto queda en la base (`safe_regeneration_attempts`) |

**Sobre la huella:**
- Es un SHA-256 truncado de un texto de alta entropía (el JSON completo del plan).
- Sirve para cotejar una línea con `plan_generation_predictions.output`, que ya se borra al eliminar la cuenta.
- No permite reconstruir el contenido.

**Modo detallado:**
- `fitsense.logging.sensitive-detail=true` vuelve a escribir el texto completo.
- **Con el perfil `prod` la aplicación no arranca** (`DetalleSensibleConfiguracion`), con el mismo criterio que `dev-log`. No se puede activar en un entorno con participantes cambiando una variable.
- En `local-dev` está activado por omisión, porque allí solo hay datos sintéticos.

### Qué se revisó y se mantiene

- **No se registran nunca**, ni antes ni ahora:
  - el *prompt*: solo su longitud (lo comprueban `ReplicateGpt5MiniDirectoTest` y `ReplicateTimingInstrumentationTest`);
  - `health_notes` y `goal_text`;
  - el token de Replicate;
  - las contraseñas, los JWT y los códigos de recuperación (salvo `dev-log` en local);
  - los correos.
- Los errores HTTP de Replicate se registran solo con su código (`http 500`), sin el cuerpo.
- Los motivos de rechazo de los validadores (`V1…V27`, contrato) se mantienen, porque son técnicos y necesarios para diagnosticar. Pueden citar ids de ejercicios del plan y valores de campos cortos (fechas, tipos), pero no texto libre ni dolor.
- Jackson no incluye el fragmento de origen en sus errores: `INCLUDE_SOURCE_IN_LOCATION` está desactivado por omisión desde la 2.16.

### Pruebas

`LogsSinDatosSensiblesIT` (IT, PostgreSQL de Testcontainers, **Replicate simulado** con un servidor HTTP local) captura toda la salida de la aplicación y busca marcas únicas:

| Caso | Resultado |
|---|---|
| Generación síncrona con `health_notes` y `goal_text` marcados y 3 propuestas inválidas | Respaldo `RULE_ENGINE`; ninguna marca en el log |
| Asíncrona: propuesta inválida y después válida (reintento) | Publica la IA con `generation_attempts=2`; la salida está en `plan_generation_predictions` y no en el log |
| HTTP 500 con datos en el cuerpo | Respaldo; queda `http 500`, sin el cuerpo |
| Adaptación con dolor (ejercicio dejado por dolor y cierre semanal) | Las líneas de dolor y exclusiones no contienen listas de ids |
| Control: modo detallado | La marca **sí** aparece (la prueba detecta lo que dice detectar) |

`DetalleSensibleTest` (U) cubre la forma técnica, la huella, el modo detallado, que `prod` + detalle no arranca y que `local-dev` sí lo admite.

## 2. P0-02 · Límites de intentos abusivos: **RESUELTO**

### Qué se implementó

- **`LimiteDeAutenticacion`** (nuevo, reutiliza `LimitePorIp`):

  | Límite | Por omisión | Variable |
  |---|---|---|
  | Login por IP | 60 / 15 min | `FITSENSE_AUTH_LOGIN_PER_IP` |
  | Registro por IP | 30 / 15 min | `FITSENSE_AUTH_REGISTER_PER_IP` |
  | Intentos **fallidos** por correo | 5 / 15 min | `FITSENSE_AUTH_LOGIN_FAILURES_PER_ACCOUNT` |
  | Ventana | 15 min | `FITSENSE_AUTH_RATE_LIMIT_WINDOW_MINUTES` |

  - Los cupos por IP son generosos a propósito: en una sesión de captación muchas personas comparten la IP de la misma Wi-Fi. La protección principal contra la fuerza bruta es el límite **por cuenta**.
  - Al superarlo, 429 **aunque la contraseña sea correcta**, hasta que pase la ventana.
  - Se aplica igual exista o no el correo, con el mismo mensaje: no permite enumerar cuentas.
  - Un login correcto vacía el contador.
- **`POST /users/me/consents`:** 20 por IP cada 15 min (`FITSENSE_CONSENT_ACCEPT_PER_IP`).
- **Respuesta:** 429 con `Retry-After` y `details[0]="DEMASIADAS_SOLICITUDES"`, con el manejador existente.
- **IP del cliente:** es `getRemoteAddr()`. Con `server.forward-headers-strategy=native`, Tomcat solo toma `X-Forwarded-For` si la petición llega de un **proxy de confianza** (`server.tomcat.remoteip.internal-proxies`). Si la manda un cliente cualquiera, se ignora.

### Pruebas

| Escenario de la orden | Prueba |
|---|---|
| Intentos sucesivos con contraseña incorrecta | `LimitesDeAutenticacionIT.fallosPorCuenta` |
| Mensajes que no enumeran | `LimitesDeAutenticacionIT.sinEnumeracion` (mismas respuestas exista o no el correo) |
| Recuperación tras la ventana | `LimitesDeAutenticacionIT.ventana` (reloj controlado) |
| Registro masivo | `LimitesDeAutenticacionIT.registroMasivo` |
| Dos IP distintas | `LimitesDeAutenticacionIT.dosIp` |
| Consentimientos | `LimitesDeAutenticacionIT.consentimientos` |
| Reinicio | `LimitesDeAutenticacionIT.reinicio`: los contadores viven en memoria y un arranque nuevo los vacía. Riesgo **aceptado y documentado** (S-4, una sola réplica) |
| `X-Forwarded-For` falsa de un cliente no confiable | `ProxyDeConfianzaLimitesIT` (Tomcat real): la cabecera se ignora y la cuarta alta da 429 |
| Compatibilidad con el proxy de Railway | `ProxyDeConfianzaRailwayIT` (Tomcat real, proxy de confianza): el límite es por cliente real; un cliente no bloquea a otro |

**Las pruebas existentes no cambian de comportamiento.** `src/test/resources/config/application.properties` sube los cupos para los contextos compartidos (que registran cientos de cuentas desde 127.0.0.1). La IT específica fija los suyos, bajos.

**Pendiente de verificar en Railway:** que el proxy llega desde un rango de confianza (`PREPARACION_HTTPS_RAILWAY.md` §6). Si no es así, el límite por IP vería la IP del proxy. Ver `06_OPERACION_RAILWAY_Y_RESPALDOS.md`.

## 3. P1-04 · Sin Neon por omisión: **RESUELTO**

- `application.properties` ya no lleva la URL ni el usuario de Neon: `spring.datasource.url=${SPRING_DATASOURCE_URL}` y `spring.datasource.username=${SPRING_DATASOURCE_USERNAME}`, sin valor por omisión. Sin esas variables la aplicación no arranca.
- La guardia de pruebas `BaseDeProduccionBloqueada` lee ahora el host de producción de `src/test/resources/produccion-bloqueada.properties`, un recurso **solo de pruebas** que no va en el jar. Las pruebas siguen sin poder tocar esa base (`BaseDeProduccionBloqueadaTest`).
- El host sigue en el historial de git. **Rotar el usuario de Neon** es una decisión operativa pendiente (P-16).

## 4. P1-06 · Guardas de seguridad que no se relajan en silencio: **RESUELTO**

- `GuardasDeSeguridadEnProduccion` registra al arrancar (`GUARDAS_SEGURIDAD`) el valor efectivo de estas propiedades:
  - `fitsense.dose.enforcement`, `capability.enforcement` y `coverage.enforcement`;
  - `adaptation.difficulty-guard` y `adaptation.pain-localized-removal`;
  - `catalogo.contencion-conservadora`;
  - `security.require-https`, `logging.sensitive-detail`, `api-docs.enabled` y `cycle.manual-run.enabled`.
- **Con `prod`, un valor distinto del aprobado impide arrancar.** Valor aprobado es el de `application.properties` y `application-prod.properties` a 27/11/2026.
- Una excepción tiene que declararse expresamente en `fitsense.security.guard-overrides`, y arranca con un WARN por cada una.
- **Hallazgo:** los valores por omisión del **código** son más laxos que los aprobados. Por ejemplo, `dose.enforcement` vale `OBSERVE` en el código y `ENFORCE` en la configuración. Una propiedad ausente también se rechaza.
- Fuera de `prod` solo se registra: las pruebas y los laboratorios usan otras combinaciones.
- No cambia ninguna regla: solo comprueba que se usa la configuración aprobada.
- Pruebas: `GuardasDeSeguridadEnProduccionTest` (U), que carga la configuración real del repositorio, y `DespliegueHttpsIT`, que arranca con `prod`.

## 5. P2-06 · IDOR en `skip`, `report` y `finish`: **NO APLICA (ya cubierto)**

La auditoría lo dio por no probado, y **era un error de la auditoría**. `ContratoErroresApiIT.recursosAjenos` ya prueba las tres rutas con el token de otro usuario: 404, y comprueba que la sesión y el entrenamiento ajenos no cambian. No se añade una prueba duplicada.

## 6. Contrato para Mobile

| Endpoint | Cambio |
|---|---|
| `POST /api/v1/auth/login` | Puede responder **429** con `Retry-After` (segundos) y `details[0]="DEMASIADAS_SOLICITUDES"`. Mensaje fijo: *"Demasiados intentos de inicio de sesion. Espera antes de volver a intentarlo."* Mostrar el tiempo de espera; no reintentar de forma automática |
| `POST /api/v1/auth/register` | Puede responder **429** con `Retry-After` |
| `POST /api/v1/users/me/consents` | Puede responder **429** con `Retry-After` |

No se añaden ni se quitan campos.

## 7. Pendiente

- Verificar en Railway la IP del proxy (§2).
- Rotar el usuario de Neon (§3).
- Los límites siguen en memoria (S-4): si algún día hay más de una réplica, hay que pasarlos a la base.
