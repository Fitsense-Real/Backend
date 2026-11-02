# Matriz de contratos de la API (fase 2.5)

**Método:**
- se revisaron los controladores, DTO, servicios, validaciones y migraciones de `generation` 7c19703;
- un agente hizo una revisión de seguridad, zonas horarias y medios **en modo solo lectura**;
- cada hallazgo se verificó en el código y, cuando era posible, se ejecutó.

**Ninguna fila cambia las reglas de entrenamiento.**

| # | Contrato existente | Problema identificado | Cambio mínimo | Compatibilidad | Pruebas | Migración | Prioridad MVP | Estado |
|---|---|---|---|---|---|---|---|---|
| 1 | `AdaptationOutcome` (`GET /plan/adaptations`, `plan.adaptation`) | El NO_FACTIBLE de la adaptación solo tenía `motivos` en texto técnico | Código estable `NoFactibleReason` **asignado por la regla que decide** y guardado en la evidencia; seis campos aditivos (`estadoPublicacion`, `motivoCodigo`, `motivoTexto`, `codigos`, `sesionAfectada`, `restricciones`) | Total: los 9 campos de siempre no cambian; las filas anteriores no llevan código | `AdaptationOutcomeContratoTest` (9), `AdaptationOutcomeTest`, `RespaldoConDolorIT` B y C | **V44_1** (2 columnas NULL) | Alta | **Fitsense-Real/Backend#22** |
| 2 | `POST /auth/login` | Un login fallido devolvía **500** (`InvalidCredentialsException` sin manejador); **reproducido con curl** | 401 con `ApiErrorResource` y el mismo mensaje haya o no cuenta | Mejora: 500 → 401 | `ContratoErroresApiIT` | No | **Bloqueante** | **Fitsense-Real/Backend#23** |
| 3 | Errores 400 | Un id no numérico o un JSON mal formado salían por `/error` con otro formato | 400 con `ApiErrorResource` | Cuerpo homogéneo | `ContratoErroresApiIT` | No | Media | #23 |
| 4 | JWT | La caducidad configurada se ignoraba (nombre de propiedad distinto) | Leer `authorization.jwt.expiration.days` | Mismo valor (7 días) | `ContratoErroresApiIT.caducidadConfigurada` | No | Baja | #23 |
| 5 | `GET /plan/current` | Usaba la fecha del **servidor**: con el servidor en UTC, un usuario de Lima recibía 404 el domingo por la noche | «Hoy» en la zona del usuario (`users.timezone`), con reloj inyectable | Misma respuesta | `ZonaHorariaUsuarioIT` (Lima, Tokio, horario de verano en Madrid y Nueva York) | No | **Alta** (el servidor de Railway casi seguro está en UTC) | **Fitsense-Real/Backend#24** |
| 6 | Recuperación de contraseña | No había endpoints (la pantalla de Flutter existía) | `POST /auth/password-reset/request\|confirm`: token de un solo uso con hash, 30 min, límite 3/h, sin enumeración | Endpoints nuevos | `RecuperacionContrasenaIT` (5) | **V44_2** (tabla nueva) | Media: **no operativa sin proveedor de correo** | **Fitsense-Real/Backend#25** |
| 7 | Configuración local | Neon era el valor por defecto; no había un perfil local seguro | Perfil `local-dev` + guardián que impide arrancar contra una base remota | No toca producción | `LocalProfileGuardTest` (4) + arranque real | No | **Alta** (riesgo de escribir en Neon) | **Fitsense-Real/Backend#26** |
| 8 | Medios (`gifPath`, `imagePath`) | Rutas relativas; **los archivos no están en el repositorio**; ningún endpoint los sirve; la licencia es de terceros (© Gym visual) | **Propuesta, sin implementar:** ver §2 | Aditiva | — | No | Media (la app funciona sin imágenes) | Decisión pendiente |
| 9 | Cambio de zona horaria | `User.updateTimezone` no tiene endpoint; `UserTimezone` acepta desfases fijos (`-05:00`), que no aplican el horario de verano | Propuesta: `PUT /users/me/timezone` solo con IDs de región; efecto desde la semana siguiente | Aditiva | — | No | Baja | Decisión pendiente (regla de negocio) |
| 10 | Planificador semanal | Solo actúa en el tick de las 03:00 locales; si se pierde (reinicio), esa semana no se cierra para esa zona | Propuesta: recuperación en los ticks siguientes del lunes, usando la idempotencia por medición que ya existe | Interna | — | No | **Antes de usuarios** | Decisión pendiente |
| 11 | Comprobación de salud | No hay Actuator: `/actuator/health` devuelve 404 | Añadir `spring-boot-starter-actuator` solo con `health`, o usar otra ruta en Railway | Aditiva | — | No | Antes del despliegue | Decisión pendiente |
| 12 | Retirada del estudio | `POST /users/me/withdraw` no desactiva la cuenta; el token sigue valiendo 7 días | Definir si la retirada cierra la sesión | — | — | No | Antes de usuarios | Decisión pendiente |
| 13 | Registro | Con un correo ya usado devuelve 422 con mensaje explícito (permite descubrir cuentas) | Aceptarlo por usabilidad o devolver un mensaje genérico | Cambio de texto | — | No | Baja | Decisión pendiente |

## 1. Lo que NO faltaba

**Verificado con `scripts/smoke-local.sh`** (curl contra el *jar*): registro, perfil (incluido el rango por objetivo), catálogo, generación, plan, sesiones, dolor, omisión, métricas, exclusiones y reincorporación.

**Cubierto por pruebas existentes:**
- propiedad de los recursos (`SeguridadRolesIT`);
- reincorporación (`PoliticasP123IT`).

## 2. Medios de los ejercicios: propuesta (sin implementar)

**Estado:**
- 1.324 ejercicios referencian `videos/*.gif` e `images/*.jpg` (`V9__catalog_full.sql`, procedente de `hasaneyldrm/exercises-dataset`).
- Los archivos **no están** en el repositorio y ningún endpoint los sirve.
- La licencia de los datos es MIT. **Las imágenes y los GIF son © Gym visual**, redistribuidos con permiso **al autor del conjunto de datos**. El derecho de FitSense a usarlos no está documentado.

**Propuesta mínima y estable** (requiere tu aprobación del alojamiento y de la licencia):
1. Propiedad `fitsense.media.base-url`, vacía por defecto.
2. Campos **aditivos** `gifUrl` e `imageUrl` = base + ruta, o `null` si no hay base o ruta. Flutter nunca construye la URL.
3. Si falta un recurso, `null`: la app muestra un marcador sin imagen.
4. `mediaAttribution` se muestra siempre junto a la imagen.

**No se inventan direcciones públicas ni se contrata almacenamiento.** Hasta decidirlo, la app no muestra imágenes (`MobileApp/docs/mvp/PENDIENTES_MOVIL.md`).
