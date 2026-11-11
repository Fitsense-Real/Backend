# 01 · Riesgo de conexión accidental a Neon: diagnóstico y propuesta

**Estado:**
- **Solo diagnóstico y propuesta.** No se ha implementado ni fusionado ninguna corrección.
- No se ha conectado a ninguna base remota, ejecutado migraciones remotas ni modificado Railway.
- Base del análisis: `generation` después de las PR #47 y #48.

## 1. Diagnóstico

### 1.1 Dónde se define la URL por defecto

`src/main/resources/application.properties`, líneas 3–5:

| Propiedad | Valor en el archivo | Variable de entorno |
|---|---|---|
| `spring.datasource.url` | **Literal**: el *pooler* del proyecto Neon (`ep-dark-brook-…-pooler…neon.tech/neondb`) | No referencia ninguna |
| `spring.datasource.username` | **Literal** `neondb_owner` | No referencia ninguna |
| `spring.datasource.password` | `${DB_PASSWORD}` | `DB_PASSWORD`, **sin valor por defecto** |
| `spring.flyway.enabled` | `true` (líneas 10–12; `locations=classpath:db/migration`) | — |

**Otras rutas hacia Neon:**
- `CicloSemanalBloqueo` (`orchestration/infrastructure/locking`) abre su propia conexión para el bloqueo del ciclo semanal.
  - Con `fitsense.cycle.lock.url` vacío, usa la URL del pool sin `-pooler`, es decir, el endpoint directo del mismo proyecto Neon.
  - Por tanto, sigue a `spring.datasource.url`.
- No hay otra configuración de producción: no existe `application-prod`, y el repositorio no tiene archivos de Railway.

### 1.2 Cómo se sobrescribe

- Spring Boot da a las variables de entorno **más precedencia** que a `application.properties`, mediante el enlace relajado:
  - `SPRING_DATASOURCE_URL` → `spring.datasource.url`;
  - `SPRING_DATASOURCE_USERNAME` → `spring.datasource.username`.
- Lo mismo vale para `SPRING_FLYWAY_URL`, `SPRING_DATASOURCE_HIKARI_JDBC_URL` y `SPRING_APPLICATION_JSON`, que también pueden cambiar la conexión. Esto ya está documentado en `pom.xml`.
- **Usuario:** literal `neondb_owner`, salvo que se defina `SPRING_DATASOURCE_USERNAME`.
- **Contraseña:** solo `DB_PASSWORD`, o bien `SPRING_DATASOURCE_PASSWORD`, que gana a la propiedad.

### 1.3 Qué hace Flyway al arrancar

- Con `spring.flyway.enabled=true`, antes de abrir la aplicación, Flyway:
  - valida las migraciones ya aplicadas (`validateOnMigrate`, activo por defecto);
  - **aplica todas las pendientes** de `db/migration` (hoy hasta V46) a la base de la conexión.
- Si una migración falla, la aplicación no arranca, pero lo ya aplicado queda aplicado.
- Después, Hibernate valida el esquema (`ddl-auto=validate`).
- Los schedulers empiezan a escribir en esa base:
  - el ciclo semanal, cada hora;
  - las sesiones vencidas, a los 30 minutos de cada hora;
  - las sesiones abandonadas, a los 35 minutos de cada hora.

### 1.4 Qué pasa en cada combinación de variables (sin perfil `local`/`local-dev`)

| `SPRING_DATASOURCE_URL` | `DB_PASSWORD` | Resultado al arrancar |
|---|---|---|
| Sin definir | Sin definir | **Falla:** no se puede resolver `${DB_PASSWORD}`. No conecta ni migra. Seguro, pero con un error poco explícito |
| Sin definir | Contraseña de **otra** base | Intenta conectarse a **Neon** como `neondb_owner`, falla la autenticación y no arranca. No migra |
| Sin definir | **Contraseña de Neon** (p. ej., variables copiadas del servicio de producción) | **Se conecta a Neon, Flyway migra Neon** y los schedulers escriben en ella. **Este es el riesgo** |
| Base de prueba | Contraseña de la base de prueba | Correcto: conecta y migra la base de prueba |
| URL mal escrita o inalcanzable | Cualquiera | Falla al arrancar (Hikari o Flyway). No migra nada |

### 1.5 Guardas existentes y por qué no cubren el riesgo

| Guarda | Ámbito | Cubre el despliegue |
|---|---|---|
| `LocalProfileGuard` (`EnvironmentPostProcessor`, en `src/main/resources/META-INF/spring.factories`) | Solo con el perfil `local` o `local-dev`: exige un host local y la IA apagada | **No:** fuera de esos perfiles no hace nada |
| `BaseDeProduccionBloqueada` (`src/test`) | Solo en el classpath de pruebas: aborta si el DataSource o Flyway apuntan al host de producción | **No:** no existe en el JAR desplegado |
| Propiedades de surefire y failsafe en `pom.xml` (`fitsense.bd.bloqueada`) | Solo durante `mvn test` y `mvn verify` | **No** |

**Dependencia importante:** `BaseDeProduccionBloqueada.hostDeProduccion()` lee el host de producción **del literal de `application.properties`**. Si ese literal se elimina, la guarda de pruebas deja de proteger, porque el host queda vacío y ya no comprueba nada.

### 1.6 Railway y despliegue automático

- El repositorio no contiene configuración de Railway: ni `railway.json`, ni `railway.toml`, ni `Procfile`, ni `Dockerfile`, ni `nixpacks`.
- El único workflow (`.github/workflows/ci.yml`) compila y prueba, **sin pasos de despliegue ni secretos**.
- Según `docs/FitSense/CATALOGO_V37.md`, el responsable confirmó que **Railway no despliega desde `generation`**.
- **No se puede verificar desde aquí** si existe un servicio de Railway conectado al repositorio, ni qué rama vigila: no hay acceso al panel.
  - Hay que confirmarlo en el panel antes de cualquier despliegue (`cierre_definitivo_backend/MIGRACIONES_Y_DESPLIEGUE.md` §2).

## 2. Propuesta de corrección (para una orden posterior; no implementada)

**Objetivo:** fuera de los perfiles locales autorizados, el backend **no arranca** si no se le da explícitamente una base de datos. Nunca usa Neon por omisión.

### Opción A (recomendada): eliminar el valor por defecto y fallar de forma explícita

1. **`application.properties`:** sustituir los literales por variables obligatorias, sin valor por defecto:
   ```properties
   spring.datasource.url=${SPRING_DATASOURCE_URL}
   spring.datasource.username=${SPRING_DATASOURCE_USERNAME}
   spring.datasource.password=${DB_PASSWORD}
   ```
   Sin esas variables, Spring no puede resolver el marcador y el arranque falla **antes** de abrir el pool o ejecutar Flyway.
2. **Guarda de arranque `DatasourceRequiredGuard`**, un `EnvironmentPostProcessor` registrado junto a `LocalProfileGuard`, para que el error sea claro y temprano:
   - fuera de `local` y `local-dev`, exige que `spring.datasource.url` empiece por `jdbc:postgresql://` y tenga host;
   - si no, lanza `IllegalStateException("No hay base de datos configurada: defina SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME y DB_PASSWORD")`.
   - No consulta la red ni decide qué host es «producción»: solo exige configuración explícita.
3. **Mantener la guarda de pruebas:** trasladar el host de producción que usa `BaseDeProduccionBloqueada` a un recurso de prueba, por ejemplo `src/test/resources/host-produccion-bloqueado.txt` o una constante de la prueba. Así sigue bloqueando aunque el literal desaparezca de `application.properties`.
4. **Pruebas:**
   - unitarias de la guarda: sin URL, URL vacía, URL no PostgreSQL y URL válida; los perfiles locales quedan exentos;
   - `BaseDeProduccionBloqueadaTest` sigue en verde con el host trasladado;
   - una prueba de que el contexto **no** arranca sin `SPRING_DATASOURCE_URL`.

**Dependencias que hay que resolver antes de fusionarla:**
- **Cualquier servicio de Railway existente** que dependa del literal (definiendo solo `DB_PASSWORD`) dejaría de arrancar tras desplegar la corrección.
  - Antes hay que añadir `SPRING_DATASOURCE_URL` y `SPRING_DATASOURCE_USERNAME` a esos servicios, con sus valores actuales.
  - Es una decisión operativa del responsable.
- Los documentos que citan «`application.properties` apunta a Neon» (`CATALOGO_V37.md`, `MIGRACIONES_Y_DESPLIEGUE.md`, `pom.xml`) habría que actualizarlos en la misma PR.
- El perfil `local-dev` no cambia: ya usa `LOCAL_DB_URL`, con localhost por defecto, y `LocalProfileGuard`.

### Opción B (complementaria, decisión de política): bloquear el host de producción salvo autorización explícita

- Además de A, la guarda rechazaría el host de producción conocido salvo con `FITSENSE_ALLOW_PRODUCTION_DB=true`, que se definiría solo en el servicio de producción.
- Protege incluso si alguien copia la URL de producción en un entorno de prueba.
- **Exige decidir qué host es producción.** Es una política nueva, así que queda para una decisión aparte.

### Lo que **no** se propone

- Desactivar Flyway por defecto: dejaría los entornos nuevos sin esquema y movería el riesgo a migraciones manuales.
- Cambiar o borrar configuración histórica sin migrar antes las variables de los servicios existentes.
