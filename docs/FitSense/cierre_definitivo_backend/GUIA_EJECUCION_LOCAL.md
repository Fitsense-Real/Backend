# Guía de ejecución local del backend

**Requisito:** el perfil `local-dev`, de Fitsense-Real/Backend#26. Hasta que se fusione, ejecutar desde esa rama o desde la combinación de los PR.

**Material necesario:**
- Java 21;
- Maven, o el *jar* ya construido;
- Docker con Compose;
- `curl` y `python3` (para la prueba de humo).

**No necesitas** credenciales de Neon, Railway ni Replicate.

## 1. Arranque

```bash
# 1) Base local (solo en 127.0.0.1:5432; datos en el volumen fitsense-local-data)
docker compose -f docker-compose.local.yml up -d

# 2) Backend con el perfil local-dev (IA apagada; base local; clave JWT de desarrollo)
./mvnw -q package -DskipTests        # o: mvn -o -q package -DskipTests
SPRING_PROFILES_ACTIVE=local-dev java -jar target/fitsense-backend-0.0.1-SNAPSHOT.jar
#   -> Flyway aplica las migraciones en la base local (44 en generation; 46 con los PR #22 y #25)
#   -> http://localhost:8080/api/v1 ; Swagger en http://localhost:8080/swagger-ui.html

# 3) Prueba de humo con un cliente HTTP independiente (20 llamadas)
./scripts/smoke-local.sh http://localhost:8080/api/v1
```

## 2. Variables de entorno (todas opcionales en local)

| Variable | Por defecto en `local-dev` | Uso |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | — (**obligatoria**: `local-dev`) | Activa el perfil y el guardián |
| `LOCAL_DB_URL` | `jdbc:postgresql://localhost:5432/fitsense` | **Solo hosts locales.** Con Neon o Railway no arranca |
| `LOCAL_DB_USER` / `LOCAL_DB_PASSWORD` | `fitsense` / `fitsense-local-solo-desarrollo` | Credenciales de desarrollo |
| `JWT_SECRET` | Clave de desarrollo | ≥ 32 bytes |
| `FITSENSE_MANUAL_CYCLE_ENABLED` | `true` | `POST /api/v1/cycle/run-for-me` para cerrar una semana a mano |
| `FITSENSE_PASSWORD_RESET_DELIVERY` | `dev-log` | El código de recuperación se escribe en el log (**solo local**) |
| `FITSENSE_PLANNING_POLICY` | `PLAN-HIST-1` | No cambiar: las candidatas solo existen en el PR #17 |

**La IA está siempre apagada en `local-dev`:** el guardián impide arrancar con `fitsense.ai.enabled=true`.

## 3. Base URL para la app

| Dónde corre la app | `API_BASE_URL` |
|---|---|
| Emulador de Android | `http://10.0.2.2:8080/api/v1` |
| Dispositivo físico (misma red) | `http://<IP-LAN-del-PC>:8080/api/v1` |
| Simulador de iOS | `http://localhost:8080/api/v1` |

Para el dispositivo físico, abrir el puerto 8080 en el cortafuegos del PC. **La base de datos sigue solo en 127.0.0.1.**

## 4. Autenticación y errores

| Paso | Detalle |
|---|---|
| Registro | `POST /auth/register` (201, sin token) |
| Login | `POST /auth/login` → `token` |
| Resto de llamadas | `Authorization: Bearer <token>` |

Todos los errores usan `ApiErrorResource`: ver `CONTRATOS_FINALES_PARA_FLUTTER.md` §1.

## 5. Parada y limpieza

```bash
# Parar el backend: Ctrl+C (cierre ordenado de Spring y del pool de conexiones)
docker compose -f docker-compose.local.yml down        # para la base y conserva los datos
docker compose -f docker-compose.local.yml down -v     # para la base y BORRA los datos locales
```

Para empezar de cero sin borrar el volumen:

```bash
docker exec fitsense-local-db psql -U fitsense -d fitsense \
  -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
```

## 6. Pruebas automatizadas

```bash
mvn -o test                              # unitarias
mvn -o verify -Pintegration              # + integración (Testcontainers: PostgreSQL efímero por clase)
```

Las pruebas *opt-in* (IA real, laboratorios) están desactivadas por defecto y **no cuentan como aprobadas**.

## 7. Verificado (02/11/2026)

**Jar construido desde la rama de #26**, con perfil `local-dev`:

| Prueba | Resultado |
|---|---|
| Base local | 44 migraciones; `smoke-local.sh` 20/20 |
| `LOCAL_DB_URL` de Neon o Railway | Salida 1, sin abrir ninguna conexión |

**Con `generation` sin #23:** el login con una contraseña incorrecta devuelve **500**, reproducido con curl.
