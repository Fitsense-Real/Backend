# Seguridad y configuración (fase 3)

## 1. Configuración local segura (3.1). Fitsense-Real/Backend#26

| Requisito | Cómo se cumple |
|---|---|
| PostgreSQL local o efímero | Perfil `local-dev` → `localhost:5432` (`docker-compose.local.yml`, solo en `127.0.0.1`). Las pruebas usan Testcontainers |
| No apuntar a Neon ni a Railway por defecto | El perfil no los menciona. **`LocalProfileGuard`** impide arrancar con los perfiles `local`/`local-dev` si el host no es local, **antes de crear el contexto** |
| Credenciales de desarrollo | `fitsense-local-solo-desarrollo` y una clave JWT de desarrollo, ambas sustituibles por variable |
| IA desactivada; sin token de Replicate | `fitsense.ai.enabled=false` y `api-token=` vacío. El guardián también rechaza la IA encendida |
| Pruebas de integración reproducibles | Testcontainers (PostgreSQL 16 efímero); no dependen del perfil |
| No alterar producción | `application.properties` no cambia. Fuera de esos perfiles, el guardián no actúa (probado) |

**Verificación real con el *jar*:**

| Ejecución | Resultado |
|---|---|
| `local-dev` + URL de Neon | Salida 1; 0 líneas de Hikari/Flyway |
| `local-dev` + URL de Railway | Salida 1; 0 líneas de Hikari/Flyway |
| `local-dev` + base local | 44 migraciones; 20/20 llamadas con curl |

**Riesgo que sigue abierto (producción):** `application.properties` mantiene **la URL y el usuario de Neon como valor por defecto**. Recomendación, **pendiente de tu decisión**:
- en Railway, definir `SPRING_DATASOURCE_URL` y `SPRING_DATASOURCE_USERNAME` explícitamente;
- en un PR posterior, quitar el valor por defecto de Neon.

No se cambia ahora porque podría afectar al entorno desplegado actual, cuya configuración no se ha visto.

## 2. Seguridad de la API (3.2)

### 2.1. Verificado y correcto (sin cambios)

| Aspecto | Evidencia |
|---|---|
| JWT firmado con HMAC (clave ≥ 32 bytes); caducado, mal formado, con otra firma o con la carga alterada → 401 | `ContratoErroresApiIT.tokensInvalidos` (#23) |
| Cuenta INACTIVE/DELETED → 401 en la siguiente petición; los roles se leen de la base en cada petición | `SeguridadRolesIT.cuentaInactiva` |
| Propiedad de los recursos: plan, inicio, sesión, abandono y ejercicio ajenos → 404 | `SeguridadRolesIT.Aislamiento` |
| **Nuevo:** `skip`, `report` y `finish` ajenos → 404, sin tocar nada | `ContratoErroresApiIT.recursosAjenos` (#23) |
| Reincorporación por dolor: `confirmNoPain` null o sin cuerpo → 400; `false` → 422; de otro usuario → 404; sin token → 401 | `PoliticasP123IT`; `ContratoErroresApiIT` (#23) |
| Administración: `hasRole(ADMIN)` en la URL y `@PreAuthorize` en los controladores; USER → 403; anónimo → 401 | `SeguridadRolesIT` |
| `/cycle/run-for-me`: `denyAll` salvo con `FITSENSE_MANUAL_CYCLE_ENABLED=true` | `SeguridadRolesIT` |
| Validaciones de perfil y disponibilidad (P-01, 422 con el rango por objetivo) | `smoke-local.sh`; `PoliticasP123IT` |
| Sin contraseñas, cabeceras `Authorization` ni cuerpos de autenticación en los registros | Revisión del código (sin hallazgos) |

### 2.2. Corregido en esta etapa

| Riesgo | Corrección | PR |
|---|---|---|
| **Login fallido → 500** (reproducido) | 401, mismo mensaje con o sin cuenta | #23 |
| Id no numérico o JSON mal formado → cuerpo de Spring Boot | 400 con `ApiErrorResource` | #23 |
| Caducidad del JWT no configurable | Se respeta la propiedad | #23 |
| `/plan/current` con la fecha del servidor | Día local del usuario | #24 |
| Recuperación de contraseña inexistente | Flujo con token de un solo uso y hash | #25 |
| Arranque local contra Neon | Guardián del perfil local | #26 |

### 2.3. Riesgos abiertos (documentados; requieren decisión)

| Riesgo | Gravedad | Propuesta | Clasificación |
|---|---|---|---|
| Sin revocación de tokens (7 días); la retirada del estudio no desactiva la cuenta; un cambio de contraseña no cierra las sesiones | Media | Marca «válido desde» en el usuario, comprobada en el filtro | Antes de pruebas con usuarios |
| El correo va dentro del JWT (legible) | Baja | Quitar la *claim* `email` (no se usa para autorizar) | Mejora posterior |
| El registro con un correo existente revela la cuenta (422) | Baja | Decidir: usabilidad frente a privacidad | Decisión |
| Tiempos de respuesta de la recuperación (con o sin cuenta) | Baja | Envío asíncrono | Antes de usuarios, si se activa el correo |
| Sin límite por IP (solo por cuenta) | Media | En la pasarela o en Railway | Antes de usuarios |
| Eventos de dolor y salida de la IA registrados con el `userId` interno (INFO/WARN) | Baja | Revisar el nivel en producción | Mejora posterior |
| Tres formatos de cuerpo de error (advice, filtros de seguridad y `/error`) | Baja | Unificar los filtros con `ApiErrorResource` | Mejora posterior |
| Bloqueo de la fila de la sesión antes de comprobar la propiedad (ajena: bloqueo breve) | Baja | Filtrar por usuario dentro de la consulta | Mejora posterior |

## 3. Migraciones (3.3)

Ver `MIGRACIONES_Y_DESPLIEGUE.md`.
