# Preparación HTTPS y Railway (PR E): guía reproducible

**No se ha desplegado nada.** Esta guía y la configuración de la PR E describen cómo publicarlo. El despliegue en Railway lo ejecuta quien tenga acceso a la cuenta.

## 1. Arquitectura de publicación

```
App Flutter / página web ──HTTPS──► Proxy de Railway (TLS) ──HTTP + X-Forwarded-Proto/For──► contenedor (java -jar, 1 instancia)
                                                                                                    │
Health check de Railway ──HTTP interno──► /actuator/health ─────────────────────────────────────────┘
                                                                                     PostgreSQL (Neon u otro, sslmode=require)
```

- **Una sola instancia.** Hay tres mecanismos que viven **en memoria**:
  - los límites por IP;
  - la reautenticación;
  - el cortacircuitos de Replicate.

  Con dos réplicas, cada límite se multiplicaría por dos y el cortacircuitos no se compartiría. En Railway, `numReplicas = 1` y sin escalado horizontal.
- **El TLS lo termina Railway.** El contenedor recibe HTTP. Con `server.forward-headers-strategy=native`, Tomcat lee:
  - `X-Forwarded-Proto`, para saber el esquema;
  - `X-Forwarded-For`, para saber la IP del cliente.

  Solo acepta esas cabeceras si vienen de un proxy de confianza. Por omisión, Tomcat confía en los rangos privados: 10/8, 172.16/12, 192.168/16, 100.64/10 y 127/8, entre otros.
- **Sin cookies ni sesiones.** El JWT viaja en `Authorization: Bearer`, así que no hay cookies que marcar como `Secure`, `HttpOnly` o `SameSite`.

## 2. Qué añade la PR E

| Aspecto | Configuración | Verificado por |
|---|---|---|
| HTTPS obligatorio | `fitsense.security.require-https=true` en `prod`. Cualquier petición que no llegue por HTTPS recibe **403 `HTTPS_REQUERIDO`** y no se procesa. No hay redirección: así el token nunca se reenvía por HTTP | `DespliegueHttpsIT.httpRechazado` |
| Detección de HTTPS detrás del proxy | `server.forward-headers-strategy=native` | `DespliegueHttpsIT.ipYEsquemaDelProxy`: con `X-Forwarded-*`, `isSecure=true` y la IP real |
| Cabeceras de seguridad | <ul><li>HSTS `max-age=31536000; includeSubDomains`, solo en HTTPS.</li><li>`Referrer-Policy: no-referrer`.</li><li>`Content-Security-Policy: default-src 'none'; frame-ancestors 'none'`.</li><li>`X-Content-Type-Options: nosniff`.</li><li>`X-Frame-Options: DENY`.</li><li>`Cache-Control: no-store`.</li></ul> | `DespliegueHttpsIT.httpsTrasProxy` |
| Health check | <ul><li>`/actuator/health` devuelve `{"status":"UP"}` sin componentes ni detalles.</li><li>El resto de actuator está cerrado.</li><li>El indicador de correo está apagado: con el SMTP bloqueado, marcaría el servicio como DOWN.</li></ul> | `DespliegueHttpsIT.httpRechazado`, `actuatorCerrado` |
| CORS | Solo los orígenes exactos de `FITSENSE_CORS_ORIGINS`. Si se pone un comodín, la aplicación no arranca | `DespliegueHttpsIT.cors`, `CorsOriginsTest` |
| Sin Swagger ni ciclo manual | Los dos quedan apagados en `prod` | `DespliegueHttpsIT.sinSwagger` |
| Errores | `server.error.include-*=never` | — |
| Base de datos | En `prod`, `SPRING_DATASOURCE_URL` y `SPRING_DATASOURCE_USERNAME` **no tienen valor por omisión**. Si faltan, la aplicación no arranca, en lugar de conectarse a la base equivocada | `DespliegueHttpsIT` arranca con el perfil `prod` |
| Rutas de administración | `/api/v1/admin/**` exige el rol ADMIN; el resto, USER, salvo las rutas públicas | `SeguridadRolesIT` |

## 3. Variables de entorno en Railway

Todos los secretos se guardan como variables de Railway, nunca en el repositorio. Los valores de esta tabla son ejemplos de formato, no valores reales.

| Variable | Obligatoria | Ejemplo o nota |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | sí | `prod` |
| `SPRING_DATASOURCE_URL` | sí | `jdbc:postgresql://<host>/<db>?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | sí | |
| `DB_PASSWORD` | sí, **secreto** | |
| `JWT_SECRET` | sí, **secreto** | Al menos 32 bytes, en Base64 o en texto. Se genera con `openssl rand -base64 48`. **Si se cambia, todas las sesiones dejan de valer** |
| `FITSENSE_CORS_ORIGINS` | no | Solo si hay una web en otro origen: `https://<dominio-web>` |
| `REPLICATE_API_TOKEN`, `FITSENSE_AI_ENABLED` | según se decida sobre la IA | Sin token, el plan lo genera el motor de reglas |
| `FITSENSE_MAIL_TRANSPORT`, `FITSENSE_MAIL_FROM`, `FITSENSE_MAIL_API_KEY` (**secreto**) | para la recuperación de contraseña y la eliminación web | Ver [COMPARATIVA_PROVEEDORES_CORREO](COMPARATIVA_PROVEEDORES_CORREO.md) |
| `FITSENSE_PASSWORD_RESET_DELIVERY` | para la recuperación de contraseña | `email` |
| `FITSENSE_PASSWORD_RESET_LINK_TEMPLATE`, `FITSENSE_ACCOUNT_DELETION_WEB_LINK_TEMPLATE` | no | `https://<dominio-web>/…{token}`, cuando exista la página |
| `FITSENSE_CONSENT_*` | **no tocar** hasta que haya documentos aprobados | [CONSENTIMIENTO_Y_EDAD](CONSENTIMIENTO_Y_EDAD.md) |
| `FITSENSE_REQUIRE_HTTPS` | no | Vale `true` por omisión en `prod`. Solo se pone a `false` para diagnosticar (apartado 6) |

**En el repositorio no hay secretos**, pero sí un dato delicado:
- `application.properties` lleva como valores por omisión la URL de Neon y el usuario, aunque no la contraseña.
- En `prod` se sustituyen por variables de entorno.
- Aun así, siguen en el historial del repositorio.

**Propuesta:** quitarlos de la configuración base y rotar el usuario si se considera sensible. La decisión está en PENDIENTES_APROBACION.

## 4. Pasos (los ejecuta quien tenga acceso a Railway)

0. **Bloqueo previo: S-1** ([PRUEBAS_SEGURIDAD](PRUEBAS_SEGURIDAD.md)).
   - Los logs detallados de generación, adaptación y respuestas de la IA se mantienen sin cambios para las pruebas.
   - Esas pruebas usan solo datos sintéticos.
   - **No desplegar para participantes reales ni activar esta configuración en producción** hasta resolver S-1 con aprobación.
   - Un entorno de pruebas en Railway con datos sintéticos sí es compatible, siempre que sus logs no se compartan ni se publiquen.
1. Fusionar las PR en orden **A → C → B → D → E** ([REGRESION_FINAL](REGRESION_FINAL.md)).
2. Configurar el servicio en Railway:
   - **Settings → Deploy:**
     - comando de arranque: `java -jar target/fitsense-backend-0.0.1-SNAPSHOT.jar`;
     - **Healthcheck Path:** `/actuator/health`;
     - réplicas: 1.
   - **Variables:** las del apartado 3. Los secretos van como variables.
   - **Networking:** dominio de Railway o dominio propio. Railway sirve HTTPS con un certificado gestionado.
3. Preparar la base de datos:
   - Al arrancar, Flyway aplica V60–V63. Esas migraciones solo añaden tablas, columnas y una secuencia; no borran nada.
   - Antes, hacer una **copia o una rama de la base** (Neon permite crear ramas) para poder volver atrás.
   - La vuelta atrás consiste en restaurar esa copia: Flyway no tiene "down".
4. Comprobar el despliegue con una cuenta de prueba, sin datos reales:

   ```bash
   curl -sI https://<dominio>/actuator/health                 # 200; status UP
   curl -s -o /dev/null -w '%{http_code}\n' -X POST https://<dominio>/api/v1/auth/login \
        -H 'Content-Type: application/json' -d '{"email":"nadie@ejemplo.invalid","password":"x-x-x-x-x"}'   # 401
   curl -sI -X POST https://<dominio>/api/v1/auth/login | grep -i strict-transport   # HSTS presente
   curl -s -o /dev/null -w '%{http_code}\n' https://<dominio>/actuator/env          # 401/403
   ```

   Si el login por HTTPS devuelve **403 `HTTPS_REQUERIDO`**, o falta la cabecera **HSTS**, el proxy no se reconoce como de confianza. Ver el apartado 6.
5. Revisar los logs: deben aparecer `CORREO transporte=…` y `ELIMINACION_*` sin ningún correo electrónico (ver PRUEBAS_SEGURIDAD).

## 5. Operaciones asíncronas, recuperación y copias

| Tema | Estado |
|---|---|
| Generación asíncrona | La reanudación tras un reinicio (V58/V59, HR-1) y el apagado ordenado se validaron en la orden anterior |
| Eliminaciones pendientes | Una tarea programada las reanuda cada 5 minutos, también después de un reinicio |
| Correo | La cola está en memoria (200 mensajes) y hace un reintento. Si el servicio se reinicia con correos pendientes, se pierden y la persona tiene que volver a pedirlos. Se considera aceptable para la recuperación de contraseña y la eliminación |
| Copias de la base | Las hace el proveedor de PostgreSQL. **El plazo de retención no está verificado:** consultarlo en la consola y declararlo |
| Retención de logs | Depende del plan de Railway. **No está verificada** |

## 6. Si la detección de HTTPS falla

Pasa cuando Railway llega desde una IP que no está en los rangos de confianza de Tomcat.

1. Poner `FITSENSE_REQUIRE_HTTPS=false`, solo de forma temporal y para diagnosticar.
2. Averiguar la IP del proxy. Por ejemplo, con un log temporal de `request.getRemoteAddr()`.
3. Fijar `server.tomcat.remoteip.internal-proxies=<regex del rango de Railway>` mediante la variable `SERVER_TOMCAT_REMOTEIP_INTERNAL_PROXIES`.
4. Volver a poner `FITSENSE_REQUIRE_HTTPS=true`.

**Nunca hay que confiar en cualquier IP (`.*`):** cualquiera podría falsear `X-Forwarded-For` y saltarse los límites por IP.
