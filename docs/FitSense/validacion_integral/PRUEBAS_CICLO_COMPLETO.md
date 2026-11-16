# Pruebas del ciclo completo con usuarios sintéticos

Fecha: 16/11/2026. Rama `validacion/backend-integral`, que contiene `generation` (`2578719`) y la corrección de #73.

Prueba: `src/test/java/main/web/services/fitsense/integral/CicloCompletoHttpIT.java`.

Resultado: **9 pruebas, 0 fallos, 0 errores, 0 omitidas** (8 en 26 s; la 9.ª en 21 s, ejecutada aparte).

## 1. Cómo se ejecuta

- **Base de datos.** PostgreSQL 16 en Testcontainers, con todas las migraciones de Flyway y el catálogo vigente. No toca
  ninguna base real.
- **Sin IA.** `fitsense.ai.enabled=false`: genera y adapta el motor de reglas, el mismo respaldo que se usa en
  producción cuando falla Replicate. No hay llamadas a Replicate ni gasto.
- **Todo por la API HTTP**, con MockMvc, la cadena de seguridad real y JWT:
  - `/auth/register` y `/auth/login`;
  - `/users/me/profile`;
  - `/users/me/plan/generate`, `/plan/current`, `/plan/{id}`, `/plans`, `/plan/adaptations` y `/plan/limitations`;
  - `/workouts/{id}/start` y `/skip`;
  - `/sessions/{id}/exercises/{pe}` y `/finish`;
  - `/users/me/metrics/weekly[/{semana}]`;
  - `/users/me/pain-exclusions` y su reactivación;
  - `/admin/participants/{id}/adherence` y `/admin/analytics/overview`.
- **El cierre semanal** se invoca con `WeeklyCycleService.runFor`, igual que el planificador.
- **El administrador** se crea con el script oficial `docs/FitSense/operaciones/cambiar_rol.sql`.

Con el motor de reglas no hace falta IA simulada. La IA simulada ya está cubierta en otras pruebas:

- **#72:** `ReanudacionConReplicateSimuladoIT` y los 20 escenarios de `GeneracionAsincronaResilienciaIT`;
- **`generation`:** `FactibilidadGeneracionIAIT` y `PoliticasP123IT`.

## 2. Escenarios

| # | Escenario | Usuario | Semanas | Qué se comprueba |
|---|---|---|---|---|
| 1-4 | Cumple todo (4 objetivos: GAIN_MUSCLE, LOSE_WEIGHT, INCREASE_STRENGTH, GENERAL_FITNESS) | Gimnasio, 3 días | 3 | Ver el desglose debajo de la tabla. |
| 5 | Cumplimiento parcial | LOSE_WEIGHT, gimnasio | 1 + ciclo | Ver el desglose debajo de la tabla. |
| 6 | Baja adherencia y omisiones | GENERAL_FITNESS | 1 + ciclo | Ver el desglose debajo de la tabla. |
| 7 | Dolor y reincorporación | GAIN_MUSCLE | 1 + ciclo | Ver el desglose debajo de la tabla. |
| 8 | Restricción que deja un slot sin ejercicios | GAIN_MUSCLE con todas las caderas bloqueadas (57 ejercicios) | Generación | Ver el desglose debajo de la tabla. |
| 9 | Resto de la API que usa Flutter | GENERAL_FITNESS | 1 + ciclo | Ver el desglose debajo de la tabla. |

**1-4. Cumple todo.**

- La generación da 201 y un plan ACTIVE, publicable, sin sesiones `safetyBlocked`, con 3 sesiones.
- `/plan/current` devuelve el plan generado.
- Adherencia oficial = 100 y `completedWorkouts` = 3 en cada semana.
- El ciclo publica la semana siguiente (lunes + 7) y el volumen no baja.
- El historial (`/plans`) contiene los 3 planes y cada uno se puede leer.
- `/plan/adaptations` tiene al menos 2 decisiones.
- Las 3 sesiones COMPLETED están ligadas a su plan.
- **Aislamiento:**
  - otro usuario recibe 404 al leer el plan o al omitir una sesión ajena;
  - el panel admin ve la misma adherencia (100);
  - el usuario normal recibe 403 en `/admin`.

**5. Cumplimiento parcial.**

- Se registra la mitad de cada ejercicio.
- La adherencia queda estrictamente entre 0 y 100.
- Queda registrada una decisión de adaptación y la semana nueva es publicable.

**6. Baja adherencia y omisiones.**

- El usuario omite 2 sesiones (LACK_OF_TIME, FATIGUE) y entrena 1.
- Adherencia < 50 y `skippedWorkouts` = 2.
- Las causas se conservan en el historial de la semana medida.
- La decisión es ADAPT_* o NO_CHANGE, y la semana nueva es publicable.

**7. Dolor y reincorporación.**

- El usuario omite 1 sesión por PAIN_OR_DISCOMFORT.
- P-02: los ejercicios de esa sesión quedan en `/pain-exclusions`.
- La semana siguiente no trae ninguno.
- La reactivación con `confirmNoPain=false` da 422.
- Con `true` el ejercicio deja de estar ACTIVA.
- El `skipReason` se conserva.

**8. Slot sin ejercicios.**

- `/plan/limitations` explica LOWER_HIP antes de generar.
- El plan se publica con el resto del cuerpo.
- Ningún ejercicio bloqueado entra en el plan.
- El perfil conserva los 57 bloqueos.

**9. Resto de la API.**

- Catálogo: grupos, equipamiento, búsqueda con `maxDifficulty=1` (el filtro se respeta) y ficha.
- Abandono: la sesión queda ABANDONED, no cuenta para la adherencia y el entrenamiento se puede hacer después.
- Reporte retroactivo («ya lo hice») de una sesión completa: cuenta para la adherencia.
- Tras el ciclo: `completedWorkouts` = 2 (la entrenada tras abandonar y la reportada) y `skippedWorkouts` = 1.
- `/users/me/interventions` devuelve exactamente las intervenciones registradas en la base.
- Retiro voluntario: `withdrawnAt` queda fijado (también en `/users/me`) y no se borra ningún plan.

## 3. Volumen por objetivo (escenarios 1-4)

| Objetivo | Volumen semanas 1 / 2 / 3 | Última decisión |
|---|---|---|
| GAIN_MUSCLE | 502 / 502 / 505 | NO_CHANGE (SIN_ORDEN) |
| LOSE_WEIGHT | 882 / 882 / 885 | NO_CHANGE (SIN_ORDEN) |
| INCREASE_STRENGTH | 236 / 236 / 238 | NO_CHANGE (SIN_ORDEN) |
| GENERAL_FITNESS | 920 / 920 / 923 | NO_CHANGE (SIN_ORDEN) |

El volumen es la suma de series × repeticiones planificadas. La semana 3 sube ligeramente: es el paso de progresión
tras dos semanas al 100 % (`ProgresionCienPorCienIT`). La decisión de adaptación es NO_CHANGE, porque no hay nada que
adaptar.

Las cifras dependen de la semana de ejecución, porque la semilla del motor de reglas incluye la fecha. En otra
ejecución (mismo día) GAIN_MUSCLE dio 512/512/514 y GENERAL_FITNESS 1030/1030/1033. Por eso las aserciones comprueban
**propiedades** y no cifras:

- el volumen no baja;
- la semana es publicable;
- la adherencia es exacta.

## 4. Hallazgo: un slot vacío no da 422

La orden pide un «usuario que no puede generar un plan factible». Con la configuración vigente, un perfil que vacía un
slot obligatorio **no** recibe 422:

- `fitsense.coverage.pregeneration=OBSERVE` es la configuración entregada (`application.properties`): *«convertir un
  flujo que hoy funciona en un 422 es decisión del estudio»*.
- La comprobación previa 4C registra el NO FACTIBLE y genera igualmente.
- `/plan/limitations` lo explica al usuario: *«no hay ejercicios de cadera… El plan trabaja el resto del cuerpo»*.

No es un defecto. Es una decisión documentada que conviene **confirmar** antes del estudio
(`PENDIENTES_PRIORIZADOS.md`, P-07). Los caminos que sí terminan en NO_FACTIBLE están probados en otras pruebas:

- 422 con `ENFORCE_*`: `SlotCoverage4CTest` lanza `InsufficientSlotCoverageException`, una `DomainRuleViolationException` que da 422;
- perfil fuera de la duración del objetivo: `PoliticasP123IT`, `PoliticaP01DuracionMinimaTest`;
- NO_FACTIBLE de adaptación: `AdaptacionCompletaIT`, `RetiradaLocalizadaFactibilidadIT`;
- NO_FACTIBLE en la generación asíncrona: `GeneracionAsincronaIT` y `GeneracionAsincronaResilienciaIT`, en #71 y #72.

La prueba no se ha forzado para que dé 422: comprueba el comportamiento real.

## 5. Qué no cubre

- **IA real.** Está fuera de alcance: no se usa Replicate.
- **La generación asíncrona (V58 y V59).** Vive en #71 y #72 y se prueba allí. Estas pruebas usan el POST síncrono, que
  sigue existiendo.
- **Semanas con más de 3 días.** Están cubiertas por `GeneracionPorObjetivoIT` y `Respaldo*` (5 y 6 días).
- **Que el planificador deje de generar planes a un retirado.** Lo garantiza la consulta de participantes activos
  (`UserRepository`, `withdrawnAt IS NULL`); esta prueba invoca el ciclo directamente y no la ejercita.
