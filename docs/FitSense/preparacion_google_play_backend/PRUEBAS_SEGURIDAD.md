# Pruebas de seguridad

Todas las pruebas usan las mismas condiciones:
- PostgreSQL 16 efímero (Testcontainers), migrado V1→V63;
- **datos sintéticos** (`*@pruebas.invalid`);
- servicios externos **simulados**: GreenMail como SMTP, un servidor HTTP local en lugar de Resend y Brevo, y sin Replicate (IA desactivada).

Ninguna toca Neon, Replicate ni un proveedor de correo real.

## Matriz

| Riesgo | Escenario | Prueba | Resultado |
|---|---|---|---|
| Eliminar la cuenta de otra persona | Solo existe `/me` y el token de B solo elimina a B | `EliminacionDeCuentaIT.soloLaPropia` | PASA |
| Eliminación con un token robado | Sin la contraseña actual: 403. Al quinto fallo: 429, aunque la contraseña sea correcta | `EliminacionDeCuentaIT.confirmacionYReautenticacion` | PASA |
| Eliminación accidental | Sin `confirm=true`: 400. En la web, un GET del enlace no borra | `confirmacionYReautenticacion`, `EliminacionWebIT.flujoCompleto` | PASA |
| Doble eliminación o eliminaciones concurrentes | 4 solicitudes a la vez: una sola eliminación | `EliminacionDeCuentaIT.concurrentes` | PASA |
| Fuerza bruta en paralelo contra la reautenticación | 50 intentos simultáneos: solo 5 llegan a comprobar la contraseña | `LimiteDeReautenticacionTest` | PASA |
| Escritura concurrente que deshace un cambio | `@DynamicUpdate` en `User`: un inicio de sesión no reescribe `status`, el hash ni `token_version` | Regresión completa | PASA |
| Reintento tras 503 en la eliminación web | El enlace vuelve a servir | `EliminacionWebIT.registroFallidoDevuelveElEnlace` | PASA |
| Acceso tras la eliminación | El JWT y el login dan 401, también la sesión de la app tras una eliminación web | `eliminacionCompleta`, `EliminacionWebIT.flujoCompleto` | PASA |
| Datos residuales | 0 filas en todas las tablas con `user_id` (leídas del esquema) y en sus dependientes. Hay una guarda contra tablas nuevas | `eliminacionCompleta`, `ningunaTablaSinCubrir` | PASA |
| Publicación después de eliminar | La generación en curso queda en FAILED/`CUENTA_ELIMINADA` y `execute` no publica nada | `interrumpidaYReanudada` | PASA |
| Eliminación interrumpida | 202 PENDING sin afirmar que se completó; la reanudación la completa | `interrumpidaYReanudada` | PASA |
| Reutilizar el P-xxx de una persona eliminada | La secuencia no se repite | `codigoDeParticipanteNoSeReutiliza`, `MigracionCuentasV60aV63IT` | PASA |
| Enumerar correos | Misma respuesta y ningún correo enviado en la recuperación y en la eliminación web. El envío ocurre después del commit, en otro hilo | `RecuperacionPorCorreoIT.sinEnumeracion`, `EliminacionWebIT.sinEnumeracion`, `RecuperacionContrasenaIT` | PASA |
| Reutilizar un token | Recuperación y eliminación web: el segundo uso da 400 | `RecuperacionPorCorreoIT.flujoCompleto`, `EliminacionWebIT.flujoCompleto` | PASA |
| Token caducado o inventado | 400 con el mismo mensaje | `EliminacionWebIT.tokenNoValido`, `RecuperacionContrasenaIT` | PASA |
| Fuerza bruta e inundación de correos | Límite por cuenta (3 por hora) y por IP (429 con `Retry-After`) | `limitePorCuenta`, `limitePorIp` (C y B) | PASA |
| Sesiones tras cambiar la contraseña | El JWT anterior da 401 (V61) y la contraseña antigua ya no sirve | `RecuperacionPorCorreoIT.flujoCompleto` | PASA |
| Contraseña en el correo | El correo nunca la lleva | `RecuperacionPorCorreoIT.flujoCompleto`, `MensajeDeRecuperacionTest` | PASA |
| Datos sensibles en logs | Los logs de la aplicación no contienen el correo, el código, el token ni las contraseñas | `RecuperacionPorCorreoIT`, `EliminacionWebIT` (con `OutputCaptureExtension`), comprobación manual de `EliminacionDeCuentaIT` | PASA |
| Fuga en el error del proveedor | Solo se propaga el código HTTP | `TransportesDeCorreoTest.rechazo` | PASA |
| Configuración de correo incompleta | La aplicación no arranca (falta clave o remitente, o no es https) | `TransportesDeCorreoTest.configuracion` | PASA |
| Menores de edad | Con 18 años menos un día: 422, sin perfil | `ConsentimientoYEdadIT.edadMinima` | PASA |
| Consentimiento sin documento aprobado | 422 | `ConsentimientoYEdadIT.sinDocumentoAprobado` | PASA |
| Cuenta a medias si falla el consentimiento | El registro y los consentimientos van en una sola transacción | `ConsentimientoExigidoIT` | PASA |
| HTTP en claro | 403 `HTTPS_REQUERIDO` | `DespliegueHttpsIT.httpRechazado` | PASA |
| HTTPS detrás del proxy | Se detectan el esquema y la IP real | `DespliegueHttpsIT.ipYEsquemaDelProxy` | PASA |
| Cabeceras | HSTS, CSP, nosniff, DENY, no-referrer, no-store y sin cookies | `DespliegueHttpsIT.httpsTrasProxy` | PASA |
| Actuator expuesto | Solo `/health`, sin detalles | `DespliegueHttpsIT.actuatorCerrado` | PASA |
| CORS abierto | Un origen ajeno no recibe `Access-Control-Allow-Origin`, y un comodín impide arrancar | `DespliegueHttpsIT.cors`, `CorsOriginsTest` | PASA |
| Rutas de administración | Exigen ADMIN | `SeguridadRolesIT` (sin cambios) | PASA |

Los resultados completos y los números están en [REGRESION_FINAL](REGRESION_FINAL.md).

## Hallazgos que no se corrigen en esta orden

> **Logs detallados (S-1).** Por instrucción expresa, los registros detallados de generación, adaptación y respuestas de la IA **se conservan sin cambios** para las próximas pruebas.
>
> - Esas pruebas usan solo datos sintéticos.
> - Esos registros no se publican ni se incluyen en ningún entregable. Este documento solo nombra el formato de las líneas, nunca su contenido.
> - Mientras S-1 no esté resuelto, **no se activa esta configuración en producción ni se prueba con participantes reales.**

| # | Hallazgo | Riesgo | Por qué no se corrige aquí | Propuesta |
|---|---|---|---|---|
| S-1 | `ReplicateTrainingPlanGenerator` registra en INFO la **salida completa de la IA** de cada intento: el plan de una persona, identificada por su id interno. Se añadió como evidencia para la tesis. Los registros detallados de generación y adaptación tienen el mismo carácter | Datos de entrenamiento personales en los logs de Railway | **Se mantiene a propósito (19/11/2026).** Hacen falta para las próximas pruebas, que solo usan datos sintéticos. No se eliminan logs, no se reduce su detalle y no se sanitiza nada | **Pendiente de seguridad y privacidad: bloquea la producción.** Antes de usar el backend con participantes reales, registrar en `prod` solo la longitud y el hash, y conservar la salida en `plan_generation_predictions`, que ya se borra al eliminar la cuenta. Hasta entonces, nada de participantes reales ni de configuración de producción con estos logs. Requiere aprobación |
| S-2 | El JWT lleva el claim `email` | El token, que se guarda en el dispositivo, revela el correo | Quitarlo podría romper la app si lo lee | Comprobar en Flutter y quitarlo |
| S-3 | La URL y el usuario de Neon están como valores por omisión en `application.properties` | Información de infraestructura en el repositorio | Cambiar la configuración base afecta a los entornos locales | Quitarlos (en `prod` ya no se usan) y valorar rotar el usuario |
| S-4 | Límites en memoria | Con más de una instancia se multiplican | Hoy se despliega una sola instancia, a propósito | Mantener una réplica, o pasar a límites en la base si se escala |
