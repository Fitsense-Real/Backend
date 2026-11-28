# 04 · Cierre semanal robusto y ampliación V59 (Backend-B3)

- **Rama:** `preproduccion/b3-cierre-semanal-recuperable`, sobre `generation` (`77b9df3`).
- **Auditoría de referencia:** P0-05 y P1-02.
- **Sin migraciones.**
- **No modifica la rama de la ampliación V59 ni duplica V66.**

## 1. B3.1 · Diagnóstico previo (27/11/2026)

| Elemento | Estado |
|---|---|
| `WeeklyCycleScheduler` (`generation`) | Solo actuaba el **lunes a las 03:00** locales (`getHour() == 3`). Si ese tick no llegaba, la semana quedaba sin cierre ni plan hasta el lunes siguiente, aunque el log decía "se reintentara" |
| Ampliación V59 | **PR [Fitsense-Real/Backend#89](https://github.com/Fitsense-Real/Backend/pull/89)** (`feature/v59-recovery-all-ai-flows`, 6 commits, CI en verde, **sin fusionar**) |
| V66 (en #89) | Añade los tipos de operación `FIRST_PLAN_SYNC` y `WEEKLY_CYCLE`, `operation_key` y `measured_week_start`, con unicidad por clave en curso. **Reservada por #89**: B3 no usa ninguna migración |
| `WeeklyCycleRecoveryScheduler` (en #89) | Relanza los ciclos **interrumpidos**: una operación `WEEKLY_CYCLE` que empezó y cuyo *lease* venció. Lo hace durante 24 h (`weekly-recovery-hours`) |
| Lo que #89 **no** cubre | Un ciclo que **nunca empezó** porque el tick de las 03:00 no llegó, y una zona saltada porque otra tardó más de una hora. Sin operación no hay nada que relanzar |

**Conclusión:** B3 no reimplementa la recuperación de #89, que actúa sobre lo interrumpido. Cubre lo que no empezó. Los dos mecanismos convergen en la misma llamada idempotente, `WeeklyCycleService.ejecutar`.

## 2. B3.2 · Recuperar cierres perdidos (P0-05): **RESUELTO (en la PR, sin fusionar)**

`WeeklyCycleScheduler` con reloj inyectable:

- **Lunes 03:00 local:** se ejecuta a **todos** los participantes de la zona, exactamente como antes.
- **Ticks posteriores dentro de `fitsense.weekly-cycle.catch-up-hours`** (24 por omisión, la misma ventana que V66): se ejecuta **solo a los pendientes** (`CiclosSemanalesPendientes`). Pendiente es quien no tiene la medición de la semana cerrada o no tiene plan `ACTIVE` para la nueva.
- **Como mucho `catch-up-max-attempts` (3) recuperaciones por participante y semana.** Así no se paga la IA cada hora si una generación falla siempre. El contador vive en memoria: tras un reinicio se vuelve a intentar, y eso es aceptable.
- **Fuera de la ventana no se publica una semana tarde**, coherente con V66.
- **Idempotencia:** se reutiliza la de siempre. Cerrar una semana dos veces no hace nada; la medición es una por semana; `publishAdapted` bloquea la medición; `CicloSemanalBloqueo` responde `EN_CURSO` a otra ejecución. No se añadió ningún mecanismo nuevo.
- **Hilo del planificador:** se mantiene **uno** (`GeneracionAsincronaResilienciaIT` lo vigila). Un cierre largo puede retrasar los ticks siguientes, pero ya no pierde semanas.

| Situación de la orden | Comportamiento | Prueba (`CierreSemanalRecuperableIT`) |
|---|---|---|
| Railway se reinicia a las 02:59 y vuelve a las 04:00 | El tick de las 04:00 recupera; el de las 05:00 no repite | `tickPerdido` |
| El backend está apagado a las 03:00 | Ídem (el primer tick dentro de la ventana) | `tickPerdido` |
| Una cohorte tarda más de lo esperado | La zona evaluada a las 04:40 se cierra | `dosZonasMismaHora` |
| Segunda zona horaria | Cada zona a su hora local | `zonasDistintas` |
| Una tarea falla a mitad del cierre | Si nunca empezó la operación, aquí. Si se interrumpió, V66 (#89) | `reintentosAcotados`, y `OperacionesIaV66IT` en #89 |
| El proceso se ejecuta dos veces | Una medición y un solo plan activo | `dosInstancias`, `tickPerdido` |
| La semana nueva ya tenía plan | Se completa la medición sin un segundo plan activo | `yaTeniaPlan` |
| Fuera de la ventana | No se publica tarde | `fueraDeLaVentana` |
| Tick normal | Igual que antes | `ticknormal` |

## 3. B3.3 · Concurrencia

| Caso | Cobertura |
|---|---|
| Dos ejecuciones simultáneas o dos instancias | `CierreSemanalRecuperableIT.dosInstancias`; `CicloConcurrenteIT` (existente: 5 carreras, dos instancias, conexión del bloqueo perdida durante la IA) |
| Reintento tras un fallo parcial | `reintentosAcotados`; V66 (#89) para un fallo dentro de la operación |
| Adaptación pendiente durante una nueva publicación | §4 (P1-02) |
| Reinicio a mitad de operación | V66 (#89): `OperacionesIaV66IT` V59-03b con reinicio real del contexto |
| Cierre que termina cuando la semana ya tiene un plan | `yaTeniaPlan`, `PublicacionConcurrenteIT` |

**Compatibilidad con #89, comprobada.** En una rama local temporal (sin subir) se fusionó B3 con `feature/v59-recovery-all-ai-flows`:
- la fusión fue limpia, sin conflictos;
- `OperacionesIaV66IT`, `MigracionV65aV66IT`, `CicloConcurrenteIT`, `CierreSemanalRecuperableIT` y `PublicacionConcurrenteIT` pasaron: **37 pruebas, 0 fallos**.

**Orden recomendado:** primero #89, después B3. Al rebasar B3 no hace falta tocar nada.

## 4. B3.4 · Publicación concurrente (P1-02): **RESUELTO**

- `persist()` y `publishAdapted()` comprueban que el plan activo **releído** es el mismo que leyó la fase 1 (`PreparedPlanGeneration.previousActivePlanId`).
- Si otro proceso publicó o reemplazó una versión mientras la IA respondía, lanzan `PlanCambioDuranteLaGeneracionException`:
  - **no se publica**;
  - el plan vigente y el historial quedan intactos;
  - por HTTP responde **409** `PLAN_CAMBIADO_DURANTE_LA_GENERACION`.
- En el cierre semanal, el ciclo lo registra y devuelve vacío. El plan del otro camino sigue siendo el único activo.
- Se comprueba **después** de la protección existente (`PlanAlreadyExists` sin reemplazo y "una publicación por medición"). El orden de esas dos guardas no cambia.

| Caso | Prueba (`PublicacionConcurrenteIT`) |
|---|---|
| Se preparó sin plan y otro proceso publicó uno | `aparecioUnPlan`: no se reemplaza; solo queda la versión del otro |
| Se preparó sobre v1 y otro proceso publicó v2 | `cambioLaVersion`: no hay v3; el historial es `REPLACED`, `ACTIVE` |
| Sin carrera | `sinCarrera`: el reemplazo v1 → v2 de siempre funciona |
| Sin reemplazo pedido | `sinReemplazo`: sigue siendo `PlanAlreadyExists`, como antes |

## 5. Pruebas ejecutadas en local

- Toda la suite de B3 junto a `CicloConcurrenteIT`, `ValidacionMultisemanaIT`, `CopiaSinIaIT`, `ProteccionPlanesPublicadosIT`, `GeneracionInicialConcurrenteIT`, `GeneracionAsincronaResilienciaIT`, `SeguridadRolesIT`, `AdaptacionCompletaIT` y `CicloCompletoHttpIT`, más las unitarias `PublishAdaptedTest`, `AdaptedWeekCommandServiceTest`, `WeeklyCycleAdjustmentTest` y `CicloSemanalBloqueoTest`.
  - La primera ejecución dio 2 fallos, que se corrigieron y se volvieron a ejecutar (31/31).
  - Uno se debía a que había subido el *pool* del planificador, cambio que se revirtió.
  - El otro era un error de la propia prueba nueva.
- La combinación con #89 se recoge en §3.

## 6. Pendiente

- Revisar y fusionar #89 y luego B3 (decisión del equipo).
- **No desplegar a las 02:00–05:00 de un lunes en hora local** de ninguna cohorte. Ahora la semana se recupera, pero los participantes recibirían el plan más tarde.
- Alertar de `CICLO_SEMANAL_PENDIENTE` y `CICLO_SEMANAL_RECUPERADO` (ver `07`).
