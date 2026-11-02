# Estado del backend para el MVP

**Referencia:** `generation` 7c19703.

**Inventario completo de contratos:** `cierre_respaldo_y_preparacion_mvp/ESTADO_CONTRATOS_FLUTTER.md`. Aquí no se repite; solo se resume y se añade lo **verificado ahora con la app**.

## 1. Listo para el MVP (contratos estables en `generation`)

| Flujo | Endpoints | Verificado desde la app Flutter contra el backend local |
|---|---|---|
| Registro e inicio de sesión | `POST /auth/register` (201, sin token) y `POST /auth/login` | Sí |
| Usuario | `GET /users/me` | Sí |
| Perfil | `GET`, `POST` y `PUT /users/me/profile`; `GET /catalog/equipment-types` | Sí: 404, 422 con el rango por objetivo (P-01, 30–75 para GENERAL_FITNESS), 201 y 422 de perfil congelado tras el primer plan |
| Generación y plan | `POST /plan/generate`, `GET /plan/current`, `GET /plans`, `GET /plan/adaptations` | Sí |
| Ejecución | `POST /workouts/{id}/start`, `GET /sessions/current` (204 sin sesión), `PUT /sessions/{id}/exercises/{peId}` (también con `PAIN_OR_DISCOMFORT`), `POST /sessions/{id}/finish`, `POST /workouts/{id}/skip` | Sí |
| Seguimiento | `GET /metrics/weekly` y `GET /pain-exclusions` | Lectura sí. Con datos, solo tras cerrar la semana |
| Reincorporación | `POST /pain-exclusions/{id}/reactivation` (`confirmNoPain`) | Con adaptador simulado: no había ninguna exclusión activa sin cerrar el ciclo |
| Ciclo semanal | Automático: lunes a las 03:00, hora local | No ejecutado con la app |

**Detalle de las pruebas:** `MobileApp/docs/mvp/PRUEBAS_INTEGRACION_MVP.md`. Son 49 pruebas unitarias más 1 de integración real.

## 2. Detrás de indicadores apagados (PR #17, sin fusionar)

Ninguno está en `generation`:
- duración estimada por contenido;
- métrica aeróbica;
- NO_FACTIBLE estructurado (NOFACT-0.1);
- las políticas C2-D, EF, V4-C, LW-PREF y REP-SUST.

La app **no depende** de ninguno.

## 3. Lo que falta en el backend para el MVP

| Necesidad | Prioridad | Nota |
|---|---|---|
| Entorno de pruebas accesible desde el teléfono | **Alta** (bloquea probar en un dispositivo fuera de la LAN) | Requiere tu autorización: no se ha desplegado nada |
| Recuperación de contraseña | Media | No hay endpoints |
| Motivo estructurado de NO_FACTIBLE en la adaptación | Media | Solo diseño (`MobileApp/docs/mvp/MODELOS_Y_CONTRATOS.md` §3) |
| URL pública de los medios de los ejercicios (`gifPath`, `imagePath`) | Media | Rutas relativas sin una base pública |
| Seguridad de la configuración local | Media | `application.properties` apunta por defecto a Neon. Para trabajar en local hay que pasar `--spring.datasource.*`. Conviene un perfil `local` que no tenga Neon por defecto (cambio de configuración pendiente de aprobación) |

## 4. Configuración de ejecución (sin secretos)

| Variable | Uso | En local |
|---|---|---|
| `JWT_SECRET` | Firma de los tokens | Clave de desarrollo, solo en la variable de entorno |
| `FITSENSE_AI_ENABLED` | IA de adaptación | `false` (sin Replicate) |
| `REPLICATE_API_TOKEN` | Token de la IA | Vacío |
| `FITSENSE_MANUAL_CYCLE_ENABLED` | `POST /cycle/run-for-me` (demostración) | Opcional |
| `fitsense.planning.policy` | Política | `PLAN-HIST-1` (por defecto) |
