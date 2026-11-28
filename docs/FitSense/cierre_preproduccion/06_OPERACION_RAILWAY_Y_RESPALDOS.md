# 06 · Operación en Railway, copias y restauración (Backend-B4.2 y B4.3; P0-06 y P0-08)

- **No se ha desplegado ni modificado ningún servicio de Railway.**
- **No se ha tocado ninguna base real.**
- Esta entrega deja la configuración propuesta y un procedimiento ejecutable para cuando se autorice.

## 1. B4.2 · Copias y restauración (P0-06): **PREPARADO SIN ACTIVAR**

### 1.1 Procedimiento

| # | Paso | Cómo | Quién |
|---|---|---|---|
| 1 | **Identificar la base de preproducción** | Confirmar en el panel si es **Neon** (la configuración actual apunta a Neon) o un PostgreSQL de Railway. **Nunca la misma base que producción** | Responsable técnico |
| 2 | **Copias automáticas y retención** | Neon: *point-in-time restore* con una retención de al menos **12 semanas del estudio, más el margen aprobado** (P-14). Según el plan de Neon puede no llegar a 12 semanas: si es así, **copias `pg_dump` semanales** fuera del proveedor. Railway PostgreSQL: copias del servicio, con la retención del plan | Responsable técnico; la retención la aprueba el equipo del estudio |
| 3 | **Copia verificable** | `scripts/respaldo/verificar_respaldo.sh ORIGEN_URL DESTINO_URL [DIR]`: lee el origen **en solo lectura** (`default_transaction_read_only=on`) con `pg_dump --format=custom` y calcula su SHA-256 | Responsable técnico |
| 4 | **Restaurar en una base aislada** | El mismo script restaura en `DESTINO_URL`. **Se niega** si el destino es la misma base o si tiene alguna tabla. Lo correcto es una base o rama de Neon nueva y vacía | Responsable técnico |
| 5 | **Comparar** | Filas tabla por tabla y la última versión de Flyway. Termina con código 0 solo si todo coincide (`informe.txt`) | El script |
| 6 | **Tiempo real de recuperación** | El informe da `segundos_copia`, `segundos_restauracion` y `segundos_total` | El script |
| 7 | **Quién puede restaurar** | Solo quien tenga en el proveedor de la base el rol de **propietario**, por escrito y con dos personas: quien ejecuta y quien verifica. Una restauración sobre producción solo se hace con autorización expresa del responsable del estudio, tras una copia del estado actual | A decidir (P-14) |
| 8 | **Antes de cada migración** (V66 y las propuestas de `07`) | Una copia con los pasos 3–6 inmediatamente antes. Flyway no tiene vuelta atrás | Responsable técnico |

**No se da por validado ningún respaldo que no se haya restaurado.** El paso 4 es obligatorio.

### 1.2 Ensayo del script (datos sintéticos)

Ensayo en local, con dos PostgreSQL 16 efímeros (Docker) y datos sintéticos: 250 usuarios, 3.000 métricas y Flyway.

| Caso | Resultado |
|---|---|
| Origen igual al destino | `ABORTADO`, salida 2 |
| Restauración en una base vacía | **OK**: todas las tablas y la versión de Flyway coinciden; salida 0. Informe en `evidencia/ensayo_respaldo_sintetico.txt` |
| Destino con tablas | `ABORTADO`, salida 3 |
| ¿Se puede escribir en el origen con la conexión del script? | **No**: `cannot execute INSERT in a read-only transaction` |

**Pendiente:** ejecutarlo contra la base de preproducción real, con autorización, y guardar el informe. Hasta entonces, P0-06 sigue **sin cerrar**.

## 2. B4.3 · Lista de configuración de preproducción (P0-08): **PREPARADO SIN ACTIVAR**

La plantilla de variables está en `railway_preproduccion.env.ejemplo`, **sin ningún secreto**.

| # | Aspecto | Configuración propuesta | Cómo se comprueba |
|---|---|---|---|
| 1 | Perfil | `SPRING_PROFILES_ACTIVE=prod` (lo más seguro disponible; no hay un perfil `staging`) | Log de arranque: `GUARDAS_SEGURIDAD perfil_prod=true` (B1) |
| 2 | Base PostgreSQL aislada | Una rama de Neon o una base nueva **solo para preproducción**, con usuario propio | `SPRING_DATASOURCE_URL` distinta de la de producción. Con B1, sin ella la aplicación no arranca |
| 3 | Credenciales distintas de producción | `JWT_SECRET` nuevo (`openssl rand -base64 48`); `DB_PASSWORD` propio; Firebase, Replicate y correo con cuentas o claves de pruebas | Revisión en el panel |
| 4 | HTTPS | Dominio de Railway con certificado gestionado; `FITSENSE_REQUIRE_HTTPS=true` (lo da `prod`) | `curl -sI http://…/api/v1/auth/login`: **403 `HTTPS_REQUERIDO`**; por https, HSTS presente |
| 5 | CORS | `FITSENSE_CORS_ORIGINS=https://<panel de preproducción>`, sin comodines | `DespliegueHttpsIT.cors` |
| 6 | Proxy de confianza | Comprobar que `getRemoteAddr()` es la IP del cliente (`PREPARACION_HTTPS_RAILWAY.md` §6). Si no, `SERVER_TOMCAT_REMOTEIP_INTERNAL_PROXIES=<regex del rango de Railway>`, **nunca `.*`** | Dos clientes desde redes distintas tienen cupos distintos (B1: `ProxyDeConfianzaRailwayIT`) |
| 7 | Health check | Railway: *Healthcheck Path* `/actuator/health` | `{"status":"UP"}` sin detalles |
| 8 | Swagger y endpoints internos | `prod` los cierra; ciclo manual apagado | `/v3/api-docs` y `/actuator/env` dan 401 o 403 |
| 9 | Correo | `05_CORREO_Y_RECUPERACION.md` §2, con una cuenta de pruebas | `05` §3 |
| 10 | Firebase | Proyecto de pruebas; `FITSENSE_FIREBASE_CREDENTIALS_BASE64` (secreto); `FITSENSE_PUSH_ENABLED=true` solo cuando se pruebe | Log `PUSH` y un dispositivo de pruebas |
| 11 | **Replicate desactivado** | `FITSENSE_AI_ENABLED=false` y **sin** `REPLICATE_API_TOKEN`: los planes salen del motor de reglas, sin coste | Log `GENERACION_TIEMPOS … motivo_respaldo=IA_APAGADA` y `generation_source=RULE_ENGINE` |
| 12 | Procesos programados | Cierre semanal con recuperación (B3) y vigilante operativo (B5) activos; worker de generación activo | Logs `Cierre semanal en …` y `ALERTA_OPERATIVA` |
| 13 | Logs | **Sin detalle sensible** (`FITSENSE_LOGGING_SENSITIVE_DETAIL` sin definir; con `prod` no puede activarse). Retención según el plan de Railway (**sin verificar**, P-14). Acceso solo del equipo técnico | Revisión del panel; una generación de prueba no deja la salida de la IA en el log |
| 14 | Copias | §1 | Informe del script |
| 15 | Réplicas | **1** (límites, cortacircuitos y reautenticación en memoria) | Panel: *Replicas = 1*, sin escalado automático |
| 16 | Guardas de seguridad | Sin `FITSENSE_DOSE_ENFORCEMENT`, `…_CAPABILITY_…`, `…_COVERAGE_…`, `FITSENSE_DIFFICULTY_GUARD` ni `FITSENSE_PAIN_LOCALIZED_REMOVAL` (se usan los valores aprobados) | Con B1, un valor no aprobado impide arrancar |
| 17 | Consentimientos | **Exigencia apagada** hasta tener los documentos aprobados y Mobile coordinado (`03`) | `GET /api/v1/legal-documents` |
| 18 | Ventana de despliegue | Nunca los lunes de 02:00 a 05:00 en hora local de ninguna zona con participantes | Procedimiento |

### 2.1 Procedimiento cuando se autorice el despliegue

1. Fusionar las PR en el orden de `10_ESTADO_FINAL_Y_BLOQUEANTES.md` §3.
2. Crear la base de preproducción (§2, fila 2) y ejecutar **una copia previa** (§1) si ya tiene datos.
3. Configurar las variables a partir de `railway_preproduccion.env.ejemplo`. Los secretos, en el panel.
4. Desplegar y comprobar las filas 1–16, guardando las salidas.
5. Ejecutar `scripts/smoke-local.sh` adaptado a la URL de preproducción. El script actual solo admite localhost a propósito: hay que copiarlo, no relajarlo.
6. Firmar la lista.

## 3. Pendiente

- Decidir la base y la retención (P-14) y ejecutar el ensayo real (§1).
- Desplegar preproducción (requiere autorización).
- Verificar el proxy (§2, fila 6) y la retención de los logs.
- Decidir quién puede restaurar (§1, paso 7).
