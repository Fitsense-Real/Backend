# Contratos del backend con Mobile (Flutter) y Web (panel admin)

Fecha: 16/11/2026. Ramas comparadas:

- **Backend:** `generation` (`2578719`); lo asíncrono, en #71 y #72.
- **Flutter MVP:** `mobileapp` `feature/mvp-integracion-api` (`fdc9439`).
- **Flutter PR #2:** de MobileApp, cabeza `f26c7ce`.

> **Actualización 17/11/2026.** #73, #71 y #72 ya están en `generation` (`b9c51ef`): la generación asíncrona (V58 y V59) es parte de la base. Las referencias a #71/#72 de este documento describen dónde se probó cada cosa antes de integrarla.

Las rutas usadas por Flutter se han extraído de `lib/` (`ApiConstants` y repositorios). Todas llevan el prefijo
`/api/v1`. No se ha construido nada nuevo.

**Leyenda:**

- **Disponible:** existe, está probado y Flutter lo usa o puede usarlo.
- **Incompleto:** existe, pero falta algo para su uso real.
- **Faltante:** no existe.
- **Incompatible:** existe, pero el cliente no puede usarlo tal como está.

## 1. Endpoints del backend

### Autenticación y cuenta

| Endpoint | Flutter MVP | Flutter PR #2 | Estado | Pruebas |
|---|---|---|---|---|
| POST `/auth/register`, `/auth/login` | Sí | Sí | Disponible | `ContratoErroresApiIT`, `CicloCompletoHttpIT` |
| POST `/auth/password-reset/request`, `/confirm` | No | Sí | **Incompleto**: `delivery=none`, no se envía correo | `RecuperacionContrasenaIT` |
| GET `/users/me`, POST `/users/me/withdraw` | GET | GET y withdraw | Disponible | `SeguridadRolesIT`, `CicloCompletoHttpIT.restoDeLaApi` |
| GET/POST/PUT `/users/me/profile` | Sí | Sí | Disponible | `CicloCompletoHttpIT`, `PerfilCongeladoIT`, `PoliticaP01DuracionMinimaTest` |

### Plan

| Endpoint | Flutter MVP | Flutter PR #2 | Estado | Pruebas |
|---|---|---|---|---|
| POST `/users/me/plan/generate` (síncrono, 201) | Sí | Sí (respaldo) | Disponible | `CicloCompletoHttpIT`, `GeneracionInicialConcurrenteIT` |
| POST `/users/me/plan/generation` (202/200), GET `/{id}`, GET `/latest` | No | Sí | Disponible (en `generation` desde el 17/11/2026) | `GeneracionAsincronaIT`, `GeneracionAsincronaResilienciaIT` |
| GET `/users/me/plan/current`, `/plan/{id}`, `/plans`, `/plan/adaptations` | Sí | Sí | Disponible | `CicloCompletoHttpIT` |
| GET `/users/me/plan/limitations` | No | Sí | Disponible | `CicloCompletoHttpIT.coberturaParcialExplicada` |

### Entrenamiento

| Endpoint | Flutter MVP | Flutter PR #2 | Estado | Pruebas |
|---|---|---|---|---|
| POST `/workouts/{id}/start`, `/skip`, `/report` | start, skip | Los tres | Disponible | `EjecucionEntrenamientosIT`, `ContratoErroresApiIT`, `CicloCompletoHttpIT.restoDeLaApi` |
| GET `/sessions/current`, `/sessions/{id}`; PUT `/sessions/{id}/exercises/{pe}`; POST `/finish`, `/abandon` | Sí | Sí | Disponible | `EjecucionEntrenamientosIT`, `CicloCompletoHttpIT` (abandono en `restoDeLaApi`) |
| GET `/users/me/pain-exclusions`, POST `/{e}/reactivation` | Sí | Sí | Disponible | `PoliticasP123IT`, `ContratoErroresApiIT`, `CicloCompletoHttpIT` |
| GET `/users/me/pain-exclusions/effective` y `/confirmation-requests`, POST `/{e}/identification` (con `unrelatedExerciseIds`); campos aditivos `confirmed` e `identifiedAt` (O20) | No | No | Disponible en la PR de seguridad O20; Mobile sin adoptar (`seguridad_o20/ESPEC_MOBILE_EXCLUSIONES.md`) | `CicloCompletoHttpIT.dolorYReincorporacion`, `ContratoErroresApiIT`, `DolorFamiliaEquivalenteIT` |

### Métricas, intervenciones y catálogo

| Endpoint | Flutter MVP | Flutter PR #2 | Estado | Pruebas |
|---|---|---|---|---|
| GET `/users/me/metrics/weekly`, `/weekly/{semana}` | Lista | Lista | Disponible | `AdherenciaIT`, `CicloCompletoHttpIT` |
| GET `/users/me/interventions` | No | Sí | Disponible | `SeguridadRolesIT`, `CicloCompletoHttpIT.restoDeLaApi` |
| GET `/catalog/body-parts`, `/equipment-types`, `/exercises`, `/exercises/{id}` | equipment-types | Los cuatro | Disponible | `SeguridadRolesIT`, `CicloCompletoHttpIT.restoDeLaApi` |
| GET `/config/active` | No | No | Disponible (compatibilidad) | `SeguridadRolesIT` |

### Ciclo y panel de administración

| Endpoint | Flutter MVP | Flutter PR #2 | Estado | Pruebas |
|---|---|---|---|---|
| POST `/cycle/run-for-me` | No | Constante y método **definidos pero nunca llamados** | `denyAll` en producción (`FITSENSE_MANUAL_CYCLE_ENABLED=false`) | `SeguridadRolesIT` |
| GET `/admin/participants`, `/{id}`, `/{id}/adherence` | — | — | Disponible (ADMIN) | `SeguridadRolesIT`, `CicloCompletoHttpIT` |
| GET `/admin/analytics/overview`, `/adherence`, `/adherence/trends`, `/segments` | — | — | Disponible (ADMIN) | `SeguridadRolesIT`, `CicloCompletoHttpIT` |

**Resultado:** ningún endpoint que Flutter llame es incompatible.

- Todas las rutas que usa el MVP existen con el mismo método.
- Las de PR #2 también, incluida la generación asíncrona, integrada con #71 y #72.
- La compatibilidad del contrato asíncrono (202/200, estados, 404 ajeno) está revisada en
  `docs/FitSense/generacion_asincrona/COMPATIBILIDAD_FLUTTER.md` (#72).

### Observaciones

1. **PR #2 de Flutter y #71/#72.** PR #2 llama a `/users/me/plan/generation`. Con #71/#72 ya en `generation`, la
   secuencia de despliegue sigue siendo: primero el backend, después Flutter. Un backend anterior daría 404 en el
   arranque de la generación.
2. **`runCycle()`** existe en `dashboard_repository.dart` y nadie lo llama. Si se conectara a un botón, en producción
   daría 403. Conviene quitarlo o dejarlo detrás de una opción de desarrollo. No es un defecto del backend.
3. **Errores.** Flutter recibe siempre `ApiErrorResource` (`ContratoErroresApiIT`):

   | Código | Cuándo |
   |---|---|
   | 400 | Validación o JSON mal formado; la reactivación sin cuerpo o con `confirmNoPain: null` |
   | 401 | JWT inválido |
   | 403 | Rol |
   | 404 | Recurso inexistente o ajeno |
   | 409 | Generación en curso |
   | 422 | Regla de dominio; la reactivación con `confirmNoPain: false` |
   | 429 | Demasiadas generaciones |

## 2. Panel web (administración)

El backend ofrece al panel **solo lectura**:

- la lista paginada de participantes;
- el detalle de cada participante, con su perfil;
- la adherencia por semana de cada participante;
- el resumen del estudio: inscripción, medición, media y mediana, riesgo y abandonos;
- la distribución de los indicadores (OFFICIAL, FREQUENCY, WORKOUT), las tendencias y los segmentos.

No existe ningún cliente web en los repositorios revisados.

Para un panel útil en el estudio **falta**, sin construir ahora (prioridades en `PENDIENTES_PRIORIZADOS.md`):

| Necesidad | Hoy |
|---|---|
| Ver los planes, sesiones y adaptaciones de un participante | Solo con la cuenta del participante o por SQL |
| Exportar los datos del estudio (CSV) | Solo SQL |
| Seguir las generaciones asíncronas y sus fallos | Solo la base y los registros |
| Gestionar roles | Script SQL |
| Ver las exclusiones por dolor y las sesiones `safetyBlocked` por participante | Solo SQL |
| Editar la configuración y el catálogo | No previsto: el catálogo conservador está congelado |
