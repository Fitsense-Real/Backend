# 05 · Preproducción aislada y migraciones

**Nada de esto se ha desplegado.** Ni Railway, ni ningún PostgreSQL persistente. V66 **no se ha aplicado a ninguna base real**.

Este documento amplía `cierre_preproduccion/06_OPERACION_RAILWAY_Y_RESPALDOS.md` y su plantilla `railway_preproduccion.env.ejemplo`, que siguen vigentes.

## 1. Propuesta de entorno de pruebas aislado

| Elemento | Propuesta | Por qué |
|---|---|---|
| **Servicio backend** | Un servicio de Railway **nuevo** (`kora-backend-preproduccion`), en otro proyecto o al menos en otro entorno que el actual. Mismo `Dockerfile`, rama `generation` | Que un error de configuración no alcance al servicio actual ni al usuario demo 48 |
| **PostgreSQL** | Una base **nueva** de PostgreSQL 16, solo para preproducción. **Nunca** una copia de la de producción con datos reales | La orden prohíbe usar la base de producción como entorno de prueba |
| **Usuarios** | Solo sintéticos (`@pruebas.invalid` o un dominio del equipo), creados por la API. Códigos P-xxx de prueba | Ningún dato de una persona |
| **Credenciales** | Todas propias de preproducción: `JWT_SECRET` (`openssl rand -base64 48`), `DB_PASSWORD` y, cuando existan, la clave de correo y las credenciales de Firebase de pruebas. Solo como variables de Railway | Una filtración de preproducción no compromete nada más |
| **Perfil seguro** | `SPRING_PROFILES_ACTIVE=prod`. Activa las guardas (`GuardasDeSeguridadEnProduccion`, que con C1 incluye la opción A), HTTPS obligatorio y la prohibición del log detallado | Preproducción tiene que comportarse como producción |
| **HTTPS y proxy** | `FITSENSE_REQUIRE_HTTPS=true`, `server.forward-headers-strategy=native`. Verificar en el panel que el proxy de Railway está en los rangos de confianza de Tomcat. Si no lo está, `SERVER_TOMCAT_REMOTEIP_INTERNAL_PROXIES` con el rango exacto, **nunca `.*`** | Si no, los límites por IP afectarían a todos a la vez (`cierre_preproduccion/02_SEGURIDAD_Y_LOGS.md`) |
| **CORS** | `FITSENSE_CORS_ORIGINS=https://<panel-preproduccion>`, solo ese origen | — |
| **Firebase de pruebas** | Un **proyecto de Firebase de pruebas**, distinto de `kora-fitpe`, con `FITSENSE_PUSH_ENABLED=false` hasta probarlo con un dispositivo del equipo. Hay que fijar `FITSENSE_FIREBASE_PROJECT_ID`, porque por omisión vale el del proyecto real | Ningún aviso puede llegar a un dispositivo que no sea de prueba |
| **Correo de pruebas** | Proveedor con dominio de pruebas y buzones sintéticos (`03` §6). Hasta entonces, `FITSENSE_MAIL_TRANSPORT=none` | — |
| **IA** | `FITSENSE_AI_ENABLED=false`, **sin** `REPLICATE_API_TOKEN`. **Por omisión el código la tiene encendida** (`FITSENSE_AI_ENABLED:true`): la variable es obligatoria | Cero gasto; los planes salen del motor de reglas |
| **Health checks** | Ruta de salud de Railway: `/actuator/health`. Es la única expuesta, sin detalles | — |
| **Monitoreo y alertas** | Regla del log de Railway sobre `ALERTA_OPERATIVA` (`cierre_preproduccion/07_TRAZABILIDAD_Y_MONITOREO.md`), que incluye `CIERRE_SEMANAL_FUERA_DE_VENTANA` (C3), y una comprobación externa de *uptime* de `/actuator/health` cada 5 min | — |
| **Respaldo y restauración** | `scripts/respaldo/verificar_respaldo.sh` (§3) contra una base aislada vacía, antes de V66 y después periódicamente | — |
| **Control de gastos** | IA apagada. Si un día se enciende para una prueba autorizada: `FITSENSE_AI_GENERATE_MAX_PER_HOUR` bajo y el tope de gasto de Replicate en su panel. El tope global en el backend sigue **pendiente** (P1-07) | — |
| **Límites de recursos** | Una sola réplica: los límites y la cola de correo están en memoria (P2-03). Memoria del contenedor ≥ 1 GB. el *pool* de Hikari (10 conexiones por omisión: no se configura en el repositorio; se cambiaría con `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE`) por debajo del máximo de conexiones del plan de PostgreSQL | — |

Variables de la plantilla que **no** se definen, porque sus valores aprobados están en el código y otro valor impide arrancar con `prod`: las de `railway_preproduccion.env.ejemplo`, más `FITSENSE_STUDY_PLANS_REQUIRE_PARTICIPATION` (C1).

## 2. Migración V66 antes de cualquier entorno persistente

| Paso | Cómo | Estado |
|---|---|---|
| 1. Identificar la base y su versión | `SELECT version, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;` en la base de destino. Se espera `65 true` | **PENDIENTE**: no hay base de preproducción |
| 2. Verificar el historial de Flyway | Ninguna fila con `success = false`. Las versiones coinciden con `src/main/resources/db/migration` (V45 reservada sin fichero: P2-04) | Pendiente (ídem) |
| 3. Copia segura | `verificar_respaldo.sh ORIGEN DESTINO_AISLADO`: el origen en solo lectura, permisos 600/700 y SHA-256 en el informe | **Ensayado con datos sintéticos** (§2.1) |
| 4. Restauración en un destino aislado | El mismo script: el destino tiene que estar **vacío** y ser **otra** base; si no, se detiene. Compara las filas tabla por tabla | **Ensayado** (§2.1) |
| 5. Ensayo de V66 sobre una copia sintética representativa | Flyway a 66 sobre la copia restaurada | **Ensayado** (§2.1) |
| 6. Integridad posterior | Filas por tabla, huella de las métricas, operaciones `FIRST_PLAN` intactas, huérfanas y predicciones conservadas, restricciones validadas | **Ensayado** (§2.1) |
| 7. Recuperación ante fallo | §2.2 | **Ensayado** |

### 2.1 Ensayo realizado (28/11/2026, solo bases locales de Docker)

Evidencia completa en `evidencia/ensayo_v66_copia_sintetica.txt`.

- **Origen:**
  - PostgreSQL 16 llevado a V65 con las migraciones reales;
  - 200 participantes sintéticos y 2.400 mediciones;
  - 200 operaciones `FIRST_PLAN`: 180 `FAILED`, 20 `PENDING` y 20 `PROCESSING` huérfanas, con 20 predicciones `ENVIADA`;
  - 200 consentimientos de la casilla `MVP-1.0`.
- **Copia y restauración** con el script de #95: `RESULTADO=OK`, todas las tablas coinciden, permisos 700/600.
- **V66 en el destino aislado:**
  - 1 migración aplicada, 0 fallidas;
  - **las 11 tablas con filas coinciden** con el origen y no aparece ninguna tabla nueva;
  - las 200 operaciones siguen siendo `FIRST_PLAN`, sin `operation_key`;
  - las 20 huérfanas siguen recuperables y las 20 predicciones `ENVIADA` se conservan;
  - **la huella MD5 de las 2.400 mediciones es idéntica** (`eae52e9d…`);
  - el origen sigue en V65.
- **Restricción sin validar:** solo `ck_pgo_lease`, que es `NOT VALID` desde antes de V66 (P2-04). Hay que hacer `VALIDATE CONSTRAINT` en preproducción antes del estudio.

### 2.2 Recuperación ante fallo (ensayada)

Sobre una tercera base con la misma copia se provocó un fallo de V66, un objeto que choca con ella:
- **V66 falla y no deja nada:** el esquema queda idéntico, Flyway sigue en `65 true` y `ck_pgo_kind`, que V66 borra primero, sigue existiendo. Es una sola transacción de PostgreSQL.
- Al retirar el objeto ajeno y repetir, se aplica bien.

**Procedimiento ante un fallo real:**
1. no tocar la base a mano;
2. leer la causa en el log de arranque;
3. desplegar otra vez el *build* anterior, que funciona con el esquema V65;
4. corregir la causa y repetir.

**Si V66 se aplicó y el fallo aparece después:** el *build* anterior también funciona con las columnas de V66, que solo añade. Si aun así hace falta volver atrás, se restaura la copia del paso 3 en una base nueva y se cambia la URL. Nunca se restaura encima.

**Prohibido en esta orden:** aplicar V66 a producción.

### 2.3 V67 (propuesta C4): NO está en `generation`

**V66 es la última migración de `generation`.** V67 solo existe en la rama de [#99](https://github.com/Fitsense-Real/Backend/pull/99), un borrador excluido de esta integración. Solo se aplicaría si se aprueba. Crea una tabla y no toca ninguna fila: `MigracionV66aV67IT` comprueba que una medición de V66 queda idéntica. Seguiría el mismo procedimiento.

## 3. Respaldo: lo que conserva el script y lo que falta

| Requisito | Estado |
|---|---|
| Origen en solo lectura | **Sí** (`default_transaction_read_only=on`) |
| Destino aislado | **Sí** (rechaza la misma base que el origen) |
| Rechazo de un destino no vacío | **Sí** (código 3) |
| Permisos restrictivos | **Sí** (`umask 077`, copia 600, desde H-3) |
| Verificación de integridad | **Sí** (SHA-256, filas por tabla y versión de Flyway) |
| **Cifrado** | **No lo hace el script.** Procedimiento: `gpg --symmetric --cipher-algo AES256 fitsense.dump`, con la frase en el gestor de secretos del equipo, o `age -r <clave pública del equipo>`. El `.dump` sin cifrar no sale de la máquina donde se hizo |
| **Retención** | Propuesta: una copia antes de cada migración y una semanal durante el estudio; se conservan las 4 últimas semanales y las de cada migración hasta el cierre del estudio. **Pendiente de aprobación** (P-14) |
| **Control de acceso** | El almacén de copias (un *bucket* privado o un disco cifrado del equipo) solo lo pueden leer la responsable del estudio y la persona de operación, con registro de accesos |
| **Eliminación segura** | `shred -u` del `.dump` sin cifrar en cuanto existe la copia cifrada (en un SSD, ver la nota). Las copias cifradas vencidas se borran del almacén y se apunta en el acta. En el ensayo de §2.1 se borró con `shred -u` |

Nota: en un SSD o en un sistema de ficheros con copia en escritura, `shred` no garantiza el borrado físico. Por eso **el `.dump` sin cifrar no debe salir de un volumen cifrado**.

## 4. Requisitos para autorizar preproducción

1. Una base de preproducción **nueva y vacía**, y un servicio separado (§1).
2. Variables de §1 cargadas y comprobadas en el panel, **sin** `REPLICATE_API_TOKEN`.
3. Copia y restauración ensayadas **contra esa base**, aunque esté vacía: comprueba los permisos y la red.
4. Primer arranque: V1..V66 sobre la base vacía, las guardas en el log (`GUARDAS_SEGURIDAD perfil_prod=true`), `REGISTRO_NO_DEFINITIVO` presente (es lo esperado en preproducción) y `/actuator/health` en 200.
5. `VALIDATE CONSTRAINT ck_pgo_lease`.
6. Regla de alertas y *uptime* configurados.
7. C1–C3 ya están en `generation` (#96–#98). Decidir antes si se fusionan [#101](https://github.com/Fitsense-Real/Backend/pull/101) (CORS), [#102](https://github.com/Fitsense-Real/Backend/pull/102) (código vigente) y [#103](https://github.com/Fitsense-Real/Backend/pull/103) (mensajes sin id). C4 (#99) solo con su validación metodológica.

Clasificación: **PREPARADO SIN ACTIVAR**, con la autorización de despliegue y la base **BLOQUEADAS POR INFRAESTRUCTURA**.
