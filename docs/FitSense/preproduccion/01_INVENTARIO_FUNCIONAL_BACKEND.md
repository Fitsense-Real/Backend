# 01 · Inventario funcional del backend (estado verificado)

**Repositorio:** `Fitsense-Real/Backend`.
**Rama auditada:** `generation` en `77b9df3` (Merge PR #88). La copia de trabajo es la rama `audit/preproduccion-backend`, que solo añade esta carpeta de documentación.
**Fecha:** 27/11/2026.
**Tecnología:** Spring Boot 4.1.1, Java 21, PostgreSQL 16 (Testcontainers) y Flyway V1–V65.
**IA:** `openai/gpt-5-mini` en Replicate. **No se ha hecho ninguna llamada**: no hay token en el entorno y el `pom` fuerza `fitsense.ai.enabled=false`.

## Documentos de esta auditoría

| Documento | Contenido |
|---|---|
| `01_INVENTARIO_FUNCIONAL_BACKEND.md` | Este inventario y el resultado de las pruebas |
| `02_AUTENTICACION_Y_CONSENTIMIENTOS.md` | Cuentas, contraseña, correo y consentimientos |
| `03_GENERACION_Y_ADAPTACION.md` | Generación, adaptación, validadores, respaldo y O20 |
| `04_INFRAESTRUCTURA_Y_SEGURIDAD.md` | Railway, PostgreSQL, seguridad, notificaciones y panel |
| `05_RIESGOS_PARA_ESTUDIO_12_SEMANAS.md` | Riesgos y arnés de 12 semanas |
| `06_PLAN_DE_CIERRE_BACKEND.md` | Matriz priorizada P0/P1/P2 con criterios de aceptación |

## Metodología

- **Código.** Se leyeron los 18 controladores, los servicios, las entidades, las 66 migraciones y las propiedades. Las afirmaciones críticas se comprobaron directamente sobre el código.
- **Revisión en paralelo.** Parte de la lectura se repartió en tres revisiones (generación, adaptación y O20; persistencia y tareas; panel y notificaciones). Sus hallazgos se contrastaron con el código antes de usarlos.
  - Una conclusión se corrigió: una de las revisiones afirmaba que no existía la ampliación V59. Sí existe en la rama remota `feature/v59-recovery-all-ai-flows`.
- **Historial.** Se consultaron la documentación de evidencia de `docs/FitSense/**` y el historial de git y de GitHub, solo en lectura.
- **Pruebas.** Se ejecutó `mvn -B verify -Pintegration` con Docker local (§1).
- **Qué no se hizo.** No hubo despliegue, merge, IA real ni acceso a Railway, Neon o Firebase. No se modificó código ni datos y no se tocó el usuario demo 48 (no aparece en el repositorio).

**Estados:**

| Estado | Significado |
|---|---|
| **VERIFICADA** | Existe y la comprueba una prueba automatizada que pasó en esta ejecución. |
| **SIN VAL. INTEGRAL** | Existe, pero falta el flujo real: proveedor, despliegue o app. |
| **PARCIAL** | Faltan partes de la funcionalidad. |
| **NO IMPL.** | No existe. |
| **CON ERROR** | Existe y tiene un fallo reproducible o una contradicción comprobable. |
| **NO APLICA** | No corresponde al backend. |
| **SIN VERIFICAR** | Depende de consolas externas a las que no se ha accedido. |

---

## 1. Resultado de la suite automatizada

Comando: `mvn -B verify -Pintegration` (Maven 3.9.11, OpenJDK 21.0.13, Docker 29.9.3, `postgres:16-alpine` de Testcontainers).

| Fase | Ejecutadas | Aprobadas | Fallidas | Errores | Omitidas |
|---|---|---|---|---|---|
| Unitarias (surefire) | 1.666 | 1.665 | 0 | 0 | 1 |
| Integración (failsafe) | __IT_RUN__ | __IT_OK__ | __IT_FAIL__ | __IT_ERR__ | __IT_SKIP__ |
| **Total** | __TOT_RUN__ | __TOT_OK__ | __TOT_FAIL__ | __TOT_ERR__ | __TOT_SKIP__ |

Resultado del *build*: __BUILD__. Duración: __DUR__.

**Omitidas y pruebas que necesitan servicios externos.** Ninguna se ha ejecutado. Todas se desactivan solas sin su propiedad:

| Grupo | Pruebas | Se activan con |
|---|---|---|
| Replicate real | `CampanaReplicateRealIT`, `A01AdaptacionRealIT`, `ReincorporacionIaRealIT`, `CampanaMvp8UsuariosIT` (salvo con `campana.ia=false`), `EvaluacionK20IT` en modo REPLICATE, `CorreccionReplicateEvidenciaIT`, y un caso de *DEV-R: control del consumo real* (es la omitida de las unitarias) | `-Dfitsense.replicate.real`, `-Dfitsense.campana.mvp`, `-Dfitsense.reincorporacion.ia`, `-Dfitsense.evaluacion.k20` |
| Réplica P2-D de pruebas | `GeneracionAsincronaIT`, `PublicacionAtomicaIT`, `InvariantesV33IT`, `DisparadorDiferidoIT` y otras `p2d/*` | `-Dfitsense.pruebas.url` |
| Neon en lectura | `NeonBloqueoDirectoIT` | `-Dfitsense.neon.verificar` + `DB_PASSWORD` |
| Análisis y campañas | `EvaluacionLimpiaProgresionIT`, `CampanaP123Cohorte02IT`, `DiagnosticoRespaldoMultisemanaIT`, `TiemposGeneracionIT`, `AuditoriaFactibilidadIT`, `ImpactoRestriccionesIT`, `ProgresionFactibilidadIT`, `ProgresionReservaIT`, `GeneracionReferenciaCandidataIT`, `ValidacionEstrategiasDificultadIT`, `ImpactoLimiteDificultadIT`, `AnalisisSustitucion1373IT`, `ExportarCatalogoIT` | Su propiedad `fitsense.*` |

**Cómo se simulan los proveedores en las pruebas que sí se ejecutan:**

| Proveedor | Sustituto |
|---|---|
| Correo | GreenMail (SMTP local) y notificadores en memoria |
| Replicate | `ReplicateFalso`, servidores `HttpServer` locales y `@MockitoBean` |
| Firebase | `GoogleSimulado` |

**Cobertura funcional conocida.** Está cubierto por pruebas que se ejecutan por omisión todo lo marcado **VERIFICADA** en §2.

**Riesgos no cubiertos por ninguna prueba:**
- la lógica de hora y zona del cierre semanal;
- la recuperación de un *tick* perdido;
- un reinicio a mitad del ciclo semanal;
- 12 semanas de punta a punta con patrones mixtos;
- la confianza en el proxy de Railway;
- la entrega real de correo y de push;
- la restauración de una copia;
- la salida de auditoría de administración;
- la no generación para no participantes;
- el IDOR de `skip`, `report` y `finish`.

**Incidencias reproducibles.** Ninguna prueba falló: __INCIDENCIAS__. Los fallos de §2 marcados **CON ERROR** se demuestran leyendo el código (con fichero y línea), no con una prueba que falle, porque ninguna los cubre.

---

## 2. Inventario

### 2.1 Cuentas y acceso (detalle en `02`)

| ID | Funcionalidad | Estado | Evidencia principal |
|---|---|---|---|
| F-01 | Registro | VERIFICADA | `AuthenticationController.register`, `RegistroConConsentimientos`; `ConsentimientoExigidoIT` |
| F-02 | Login | VERIFICADA | `UserCommandServiceImpl`; `ContratoErroresApiIT.loginFallido` |
| F-03 | JWT (HS256, 7 días, revocación por `token_version` y estado) | VERIFICADA | `TokenServiceImpl`, `BearerAuthorizationRequestFilter`; `ContratoErroresApiIT.tokensInvalidos` |
| F-04 | Renovación de sesión | NO IMPL. | No existe endpoint de *refresh* |
| F-05 | Cierre de sesión en el servidor | NO IMPL. | No existe *logout* |
| F-06 | Eliminación de cuenta (app y web) | VERIFICADA | `EliminacionDeCuentaIT`, `EliminacionWebIT` |
| F-07 | Cambio de contraseña autenticado | NO IMPL. | `User.changePassword` solo se usa desde la recuperación |
| F-08 | Recuperación de contraseña | SIN VAL. INTEGRAL | `PasswordResetService`; `RecuperacionContrasenaIT`, `RecuperacionPorCorreoIT` (GreenMail); `delivery=none` |
| F-09 | Verificación de correo | NO IMPL. | — |
| F-10 | Protección contra abuso | PARCIAL | `LimitePorIp` solo en recuperación y eliminación web; login y registro sin límite |
| F-11 | Validación del registro | VERIFICADA | `SignUpResource`, `EmailAddress`, `ck_users_email_lower` |
| F-12 | Cuentas duplicadas | VERIFICADA | `uq_users_email`, 422 o 409 |
| F-13 | Revocación de sesiones al cambiar la contraseña | VERIFICADA | V61; `RecuperacionPorCorreoIT.flujoCompleto` |
| F-14 | Edad mínima de 18 años | VERIFICADA | `ConsentimientoYEdadIT.edadMinima` |
| F-15 | Roles USER y ADMIN, denegación por omisión | VERIFICADA | `WebSecurityConfiguration`; `SeguridadRolesIT` |
| F-16 | Aislamiento entre usuarios (IDOR) | VERIFICADA | `SeguridadRolesIT.recursosAjenos`, `ContratoErroresApiIT.recursosAjenos`. Sin prueba para `skip`, `report` y `finish` |

### 2.2 Consentimientos (detalle en `02` §4)

| ID | Funcionalidad | Estado | Evidencia principal |
|---|---|---|---|
| F-20 | Modelo versionado (`user_consents`, V63) | VERIFICADA | `ConsentimientoYEdadIT`, `MigracionCuentasV60aV63IT` |
| F-21 | Exigir términos y privacidad en el registro | PARCIAL (apagado) | `enforce-on-signup=false`, sin versiones configuradas; funciona si se enciende (`ConsentimientoExigidoIT`) |
| F-22 | Consentimiento informado con versión del documento | PARCIAL | Casilla booleana; versión `MVP-1.0` fijada por el servidor, sin documento aprobado |
| F-23 | Registro de negativas | NO IMPL. | Sin participación no se escribe ninguna fila |
| F-24 | Retirada del estudio | VERIFICADA | `ConsentimientoYEdadIT.retirada` |
| F-25 | Separación usuario habitual / participante | CON ERROR | `User.isEligibleForPlanGeneration` exige ser participante: sin participar no hay plan |
| F-26 | Prueba del consentimiento tras eliminar la cuenta | NO IMPL. (decisión P-4) | Se borra con la cuenta |

### 2.3 Perfil y generación (detalle en `03`)

| ID | Funcionalidad | Estado | Evidencia principal |
|---|---|---|---|
| F-30 | Perfil (crear, leer, actualizar; congelado tras el primer plan) | VERIFICADA | `UserProfileController`; `PerfilesLimiteIT`, `PoliticaP01DuracionMinimaTest` |
| F-31 | Objetivos, disponibilidad, duración, equipamiento y restricciones | VERIFICADA | `GeneracionPorObjetivoIT`, `LimitacionesPlanIT`, `CatalogoElegibleIT` |
| F-32 | Catálogo (`/api/v1/catalog/**`) y filtrado de elegibles | VERIFICADA | `ExerciseRepository.findEligible`, `ExternalCatalogService`; `ContencionTextoBanderaIT` |
| F-33 | Contexto de la IA (`GEN-IN-1.13`) | VERIFICADA | `PlanInputSnapshot`; incluye `health_notes` y `goal_text` |
| F-34 | Solicitud a Replicate (gpt-5-mini, G08 por id) | SIN VAL. INTEGRAL (aquí); REAL histórico | `ReplicateRecuperacionTransporteTest` (SIM), `Gpt5MiniConO20IT` (SIM); `docs/FitSense/migracion_gpt5_mini/` |
| F-35 | Validación V1–V27 | VERIFICADA | `PlanDraftValidator`; varias pruebas U e IT |
| F-36 | Reintentos (3) y cortacircuitos | VERIFICADA (SIM) | `PlanGenerationPipelineCircuitBreakerTest`, `ReintentosYFactibilidadIT` |
| F-37 | Respaldo `RULE_ENGINE` y variantes D-G3 | VERIFICADA | Pruebas IT con la IA apagada |
| F-38 | Persistencia y versionado de planes | VERIFICADA | `ux_plan_active`, `uq_plan_user_week_version`; `LinajeIntegridadIT` |
| F-39 | Generación asíncrona V58/V59 (*lease*, libro de predicciones, reanudación) | VERIFICADA (SIM) | `GeneracionAsincronaResilienciaIT`, `ReanudacionConReplicateSimuladoIT`, `MigracionV58aV59IT` |
| F-40 | Consulta desde Mobile (`/plan/current`, `/plans`, `/plan/{id}`, `/limitations`) | VERIFICADA | `CicloCompletoHttpIT`, `ZonaHorariaUsuarioIT` |
| F-41 | *Prompt* de generación activo | VERIFICADA | `PR-1.7` (constante); H2 y H2-v4 no están en `src/` |
| F-42 | *Prompt* de adaptación activo | VERIFICADA | `v2.1` (`FIX-PR-2.1-EXP`) por `application.properties`; el código usa `v1` por omisión |
| F-43 | Límite de generaciones por usuario | PARCIAL | Solo en la ruta síncrona; sin tope global de gasto |
| F-44 | Métricas de generación consultables | PARCIAL | Origen e intentos en la base; tiempo y predicciones solo en la ruta asíncrona; motivo del respaldo solo en el log |

### 2.4 Ejecución y adaptación (detalle en `03` §3)

| ID | Funcionalidad | Estado | Evidencia principal |
|---|---|---|---|
| F-50 | Sesiones completas, parciales y abandonadas; reporte retroactivo | VERIFICADA | `WorkoutSessionController`; `EjecucionEntrenamientosIT` |
| F-51 | Omisión con motivo y vencimiento automático | VERIFICADA | `/workouts/{id}/skip`, `OverdueWorkoutScheduler`; `EjecucionEntrenamientosIT` |
| F-52 | Adherencia (ponderada, observada, cobertura) | VERIFICADA | `WeeklyMetricsCalculator`; `AdherenciaIT` |
| F-53 | Señales y decisión (`AdaptationOrderDecider`) | VERIFICADA | `AdaptationOrderDeciderTest`, `AdaptacionPorCausasIT` |
| F-54 | Propuesta de la IA, validación V8C y estricta, 3 reintentos, `RESPALDO` | VERIFICADA (SIM) | `AdaptedWeekProducerTest`, `ConservationValidatorTest` |
| F-55 | Publicación atómica con evidencia V32 | VERIFICADA | `publishAdapted`; `CicloConcurrenteIT` |
| F-56 | Historial (`/plan/adaptations`, `/interventions`) | VERIFICADA | `LinajeIntegridadIT` |
| F-57 | Progresión y reincorporación | VERIFICADA | `ProgresionIT`, `CopiaSinIaIT` |
| F-58 | Prevención de adaptaciones duplicadas | VERIFICADA | Bloqueo consultivo, `bloquearMedicion`; `CicloConcurrenteIT` |
| F-59 | Evitar sobrescribir un plan recién publicado | PARCIAL | Sin control optimista: `previousActivePlanId` no se compara |
| F-60 | Notificación de adaptación publicada | SIN VAL. INTEGRAL | Push apagado; Google simulado |

### 2.5 Seguridad O20 (detalle en `03` §4)

| ID | Funcionalidad | Estado | Evidencia principal |
|---|---|---|---|
| F-70 | Bloqueo por dolor (identificado y sesión sin identificar) | VERIFICADA | `PainMemoryService`; `DolorFamiliaEquivalenteIT`, `O12Dolor*IT` |
| F-71 | Equivalencias (118 grupos) y precauciones por ángulo | VERIFICADA (SIM) | `FamiliaDeMovimientoEquivalenteTest`, `PainMemoryFamiliaTest`, `Gpt5MiniConO20IT`. Nunca probado con IA real |
| F-72 | Identificación posterior (V65), confirmaciones y reactivación rechazada | VERIFICADA | `PainExclusionController`; `MigracionV65IT`, `ContratoErroresApiIT.reincorporacionSinConfirmacion` |
| F-73 | Protección de planes publicados y reparación | VERIFICADA | `ProteccionPlanesPublicadosIT`, `PublishedPlanSafetyServiceDolorTest` |
| F-74 | Restricciones durante la adaptación (SUST-SEG-1) | VERIFICADA | `SustitucionSeguridadTest`, `RespaldoConDolorIT`, `SinSustitutoDolor*IT` |
| F-75 | Ningún camino publica sin superar V1 | VERIFICADA por lectura | `RULE_ENGINE`, D-G3, P-03, `CONSERVED`, progresión y reparación pasan por la validación |

### 2.6 Procesos automáticos y persistencia (detalle en `04` y `05`)

| ID | Funcionalidad | Estado | Evidencia principal |
|---|---|---|---|
| F-80 | Migraciones V1–V65 | VERIFICADA | `Migracion*IT`; falta V45 (reservada) |
| F-81 | V58 (operaciones) y V59 (*lease* y libro de predicciones) | VERIFICADA | `MigracionV58aV59IT`; `ck_pgo_lease` sigue `NOT VALID` |
| F-82 | Ampliación V59 (síncrona, ciclo, adaptación; V66) | **PENDIENTE** (en desarrollo, no integrada) | Rama `feature/v59-recovery-all-ai-flows`, 1 commit WIP del 27/11/2026 |
| F-83 | Cierre semanal automático | CON ERROR (sin recuperación) | `WeeklyCycleScheduler:54-55,74`; ninguna prueba de hora o zona |
| F-84 | Doble ejecución del cierre | VERIFICADA (concurrente) / PARCIAL (tras un fallo, repite la IA) | `CicloConcurrenteIT` |
| F-85 | Recuperación tras reinicio (generación asíncrona, eliminaciones) | VERIFICADA | `GeneracionAsincronaResilienciaIT`, `EliminacionDeCuentaIT.interrumpidaYReanudada` |
| F-86 | Transacciones fuera de las llamadas a la IA | VERIFICADA por lectura | `WeeklyCycleService.runFor`, `generate` y `AdaptedWeekCommandService` no son transaccionales |
| F-87 | Copias y restauración | SIN VERIFICAR | Solo documentación |
| F-88 | Tareas programadas en un único hilo | CON ERROR (riesgo) | No hay `spring.task.scheduling.pool.size` |

### 2.7 Notificaciones, panel e infraestructura (detalle en `04`)

| ID | Funcionalidad | Estado | Evidencia principal |
|---|---|---|---|
| F-90 | Registro, actualización y baja de FID; preferencias | VERIFICADA | `NotificacionesPushIT`, `ContratoPushHttpIT` |
| F-91 | Envío FCM (4 eventos) y manejo de errores | SIN VAL. INTEGRAL | `ArranquePush*IT` (simulado); apagado por omisión |
| F-92 | Desvinculación al cerrar sesión | PARCIAL | Depende de Mobile; no hay *logout* |
| F-93 | Recordatorios | NO APLICA / NO IMPL. | El servidor no envía recordatorios; corresponden a Mobile si se quieren |
| F-94 | Panel: participantes, adherencia y analítica | VERIFICADA | `SeguridadRolesIT.cifras`, `datosPersonales` |
| F-95 | Panel: historial de entrenamientos y adaptaciones | NO IMPL. | No hay endpoint de administración |
| F-96 | Exportación | NO IMPL. | Solo SQL de solo lectura en `docs/FitSense/consultas` |
| F-97 | Restricción de datos personales en el panel | PARCIAL | El detalle muestra nombre y correo de cualquier cuenta USER |
| F-98 | Trazabilidad temporal | PARCIAL | Métricas sobrescritas; `calculated_at` no se actualiza |
| F-99 | HTTPS, cabeceras, CORS, health y Swagger cerrado | VERIFICADA | `DespliegueHttpsIT`, `CorsOriginsTest` |
| F-100 | Logs sin datos sensibles | CON ERROR (S-1) | `ReplicateTrainingPlanGenerator:536,824` |
| F-101 | Monitorización y alertas | NO IMPL. | Solo `actuator/health` |
| F-102 | Configuración de Railway y aislamiento de entornos | SIN VERIFICAR / PARCIAL | `PREPARACION_HTTPS_RAILWAY.md`; S-3 |

---

## 3. Resumen

| Estado (primer estado de cada fila) | N.º de 77 |
|---|---|
| VERIFICADA (incluye pruebas con proveedor simulado) | 45 |
| VERIFICADA por lectura de código (sin prueba específica) | 2 |
| SIN VAL. INTEGRAL | 4 |
| PARCIAL | 9 |
| NO IMPL. | 9 |
| CON ERROR | 4 |
| SIN VERIFICAR | 2 |
| PENDIENTE (ampliación V59) | 1 |
| NO APLICA | 1 |

Las filas con estado doble (p. ej. F-84, F-102) se cuentan por el primero.

**Lectura:**
- La lógica del dominio (generación, validación, adaptación y O20) está **ampliamente implementada y cubierta por pruebas**.
- Lo que falta para el piloto es de **operación, privacidad y consentimiento**, no de entrenamiento. Ver `06`, que recoge 9 P0. Cinco de ellos (P0-04 en su opción A, P0-06, P0-07, P0-08 y P0-09) son decisiones u operación sin código nuevo. Solo P0-01, P0-02 y P0-05 requieren código, y P0-03 un cambio pequeño de API.
