# 05 · Riesgos para el piloto y el estudio de 12 semanas

**Rama auditada:** `generation`, commit `77b9df3`. **Fecha:** 27/11/2026.
Cada riesgo remite a su evidencia y a la acción de `06_PLAN_DE_CIERRE_BACKEND.md`.

**Escalas.** Probabilidad: A (alta), M (media), B (baja). Impacto sobre el estudio: Crítico (invalida datos o expone a personas), Alto, Medio.

---

## 1. Riesgos operativos

| ID | Riesgo | Prob. | Impacto | Evidencia | Acción |
|---|---|---|---|---|---|
| **R-01** | **Una semana sin cierre ni plan si se pierde el tick del lunes a las 03:00.** Basta un despliegue, un reinicio, el único hilo de tareas ocupado, un bloqueo `EN_CURSO` o `SIN_BLOQUEO`, o una cohorte de la misma zona horaria que tarde más de 60 min. Las personas se quedan sin rutina toda la semana. El lunes siguiente se mide una semana sin plan y se regenera desde cero. La adherencia y el linaje de esa semana quedan rotos. | A (en 12 semanas hay 12 ventanas de riesgo; los despliegues durante el piloto son probables) | **Crítico** | `WeeklyCycleScheduler.java:54-55,74`; no existe `spring.task.scheduling.pool.size`; ninguna prueba cubre la lógica de hora y zona; pendiente conocido (`IMPLEMENTACIONES_PENDIENTES_MVP.md` n.º 2, D7). | P0-05 |
| R-02 | Una generación fallida en el ciclo no se reintenta hasta el lunes siguiente. Una re-ejecución manual vuelve a pagar la IA. | M | Alto | `WeeklyCycleService.java:170-177`. No hay ningún registro de intento que lo impida. | P0-05, P1-07 |
| R-03 | La copia de seguridad o la restauración no funcionan cuando hacen falta: retención desconocida y restauración nunca ensayada. | M | **Crítico** (pérdida de registros del estudio) | `PENDIENTES_APROBACION.md` P-14. En `scripts/` no hay copias. | P0-06 |
| R-04 | Fallos silenciosos. No hay métricas, alertas ni panel de errores. Un cierre semanal fallido, el cortacircuitos de Replicate abierto o un correo descartado solo se ven en los logs. | A | Alto | `management.endpoints.web.exposure.include=health`; no hay Micrometer ni Sentry. | P1-01 |
| R-05 | Latencia de la IA de 1,5 a 5 min y alrededor del 42 % de reintentos. Si Mobile usa la ruta síncrona, se producen *timeouts* y peticiones repetidas. | M | Medio | Ver `03` §2.1. | Dependencia de Mobile (ruta asíncrona) |
| R-06 | Gasto de Replicate sin tope global. La ruta asíncrona no aplica el límite de 5 por hora ni el bloqueo del ciclo. Los POST con respuesta ilegible se cobran (I-5). | M | Medio | `GeneracionInicialService:50-53`; `RIESGO_COSTOS_DUPLICADOS.md`. | P1-07, P1-08 |
| R-07 | La ampliación V59 aún no está integrada. Un reinicio con un POST en curso en la ruta síncrona, el ciclo o la adaptación no se reanuda y puede duplicar el coste. | M | Medio | Rama `feature/v59-recovery-all-ai-flows` (WIP, V66, falta IT). | P1-08 |
| R-08 | Un solo contenedor. Cada despliegue corta el servicio, y los límites y el cortacircuitos están en memoria y se reinician. | A | Medio | `PREPARACION_HTTPS_RAILWAY.md` §1. | P1-01. Desplegar fuera de la ventana de los lunes de 02:00 a 05:00 en hora local |
| R-09 | Los correos pendientes se pierden al reiniciar (cola en memoria de 200 mensajes). | B | Bajo | `PREPARACION_HTTPS_RAILWAY.md` §5. | Aceptado |

## 2. Riesgos de seguridad y privacidad

| ID | Riesgo | Prob. | Impacto | Evidencia | Acción |
|---|---|---|---|---|---|
| **R-10** | **Datos de salud y de entrenamiento en los logs de Railway (S-1).** Se registran en INFO la salida completa de la IA y el detalle del dolor y de la adaptación, ligados al `user_id`. | A (ocurre en cada generación) | **Crítico** | `ReplicateTrainingPlanGenerator.java:536,824`; `PRUEBAS_SEGURIDAD.md` S-1 lo declara "bloquea la producción". | P0-01 |
| **R-11** | **Fuerza bruta en el login y alta masiva de cuentas.** No hay límite por IP ni por cuenta, ni bloqueo. | M | **Crítico** | `grep limite.comprobar`: solo se usa en la recuperación de contraseña y en la eliminación web. | P0-02 |
| R-12 | Un token robado vale 7 días. No hay cierre de sesión en el servidor ni renovación, y el JWT incluye el correo (S-2). | M | Alto | `TokenServiceImpl:53`. | P1-03 |
| R-13 | Un ADMIN ve el nombre y el correo de cualquier cuenta, participe o no. No hay un rol de investigador con datos seudonimizados, y la auditoría de accesos solo va a los logs. | M | Alto | `AdminAnalyticsQueryService.participant` (`WHERE role='USER'`); `AdminAuditInterceptor`. | P1-05 |
| R-14 | `health_notes` y `goal_text` (texto libre) se envían a Replicate sin una base legal decidida (P-13). | A | Alto | `PlanInputSnapshot:59-60,519`. | P0-07 (decisión) |
| R-15 | URL y usuario de Neon por omisión en el repositorio (S-3): un arranque sin el perfil `prod` se conecta a la base real. | B | Alto | `application.properties:3-4`. | P1-04 |
| R-16 | Interruptores de seguridad (`FITSENSE_DIFFICULTY_GUARD`, `DOSE`/`CAPABILITY_ENFORCEMENT`, contención) que una variable mal puesta apaga sin avisar. | B | **Crítico** (rutina insegura) | `03` §4, riesgo residual 2. | P1-06 |
| R-17 | El límite por IP ve la IP del proxy si Tomcat no confía en Railway, y bloquearía a todo el mundo a la vez. | M | Medio | `PREPARACION_HTTPS_RAILWAY.md` §6, sin verificar. | P0-08 |
| R-18 | Sin verificación de correo: cuentas con correos ajenos o mal escritos que no pueden recuperarse. | M | Medio | `02` A9. | P1-10 |

## 3. Riesgos de investigación (validez y ética)

| ID | Riesgo | Prob. | Impacto | Evidencia | Acción |
|---|---|---|---|---|---|
| **R-20** | **Captación sin documentos aprobados.** Hoy cualquiera se registra sin aceptar términos ni privacidad. El consentimiento informado es una casilla con una versión `MVP-1.0` que no corresponde a ningún documento. No queda constancia de qué texto vio la persona. | A | **Crítico** | `02` §4. | P0-03 |
| **R-21** | **Participar en el estudio es condición para recibir planes** (`User.isEligibleForPlanGeneration`). El consentimiento deja de ser libre, y quien se retira pierde el servicio. La documentación afirma lo contrario. | A (ocurre siempre) | **Crítico** (ético) | `02` §4.4. | P0-04 (decisión) |
| R-22 | Eliminar la cuenta borra los datos del estudio **y la prueba del consentimiento**. Las exportaciones previas pueden quedar con datos de personas eliminadas. | M | Alto | `EliminacionDeCuentaService.TABLAS_EN_ORDEN`; P-1, P-3, P-4. | P0-07 (decisión) |
| R-23 | Trazabilidad temporal incompleta. `weekly_user_metrics` se sobrescribe y `calculated_at` conserva la fecha del primer cálculo. Las sesiones y los ejercicios se editan en el mismo registro. No queda el "qué se sabía en cada momento". | A | Alto | `WeeklyUserMetrics:181-203`. | P1-09 |
| R-24 | No hay exportación reproducible ni un rol de solo lectura. El análisis depende de ejecutar SQL a mano contra la base operativa. | A | Medio | `04` P7, P10. | P1-05 |
| R-25 | El motivo del respaldo `RULE_ENGINE` no se guarda, y el tiempo y las predicciones solo se registran en la ruta asíncrona. Con lo que hay en la base no se puede caracterizar la intervención (IA frente a reglas). | A | Medio | `03` §2. | P2-02 |
| R-26 | No hay cribado de aptitud física (tipo PAR-Q). `health_notes` no se interpreta: una lesión escrita ahí no restringe nada. | M | Alto, según el protocolo | `03` §4, riesgo residual 4. | P0-07 (decisión) |
| R-27 | Una adaptación tardía puede reemplazar sin aviso una versión publicada por otro camino mientras la IA responde (no hay control optimista). | B | Medio | `03` §3.2. | P1-02 |
| R-28 | La lógica de 12 semanas nunca se ha ensayado de principio a fin con interrupciones y reinicios. | — | Alto | §4. | P1-11 |
| R-29 | Las exclusiones de equivalencias y de ángulo (O20) **nunca se han probado con IA real**. | M | Medio | `D/seguridad_o20/README.md:11`. | Aceptado con validación V1. Muestreo en el piloto |

---

## 4. Simulación de 12 semanas: lo que existe y lo que falta

### 4.1 Arneses existentes (sin Replicate)

| Arnés | Semanas | Qué cubre | ¿Se ejecuta en `mvn verify -Pintegration`? |
|---|---|---|---|
| `orchestation/CopiaSinIaIT.techo` | **17 ciclos** | Adherencia total, techo de progresión y mantenimiento | **Sí** |
| `orchestation/CopiaSinIaIT.reincorporacionYMantenimiento` | hasta 12 | Reincorporación | **Sí**. Depende de la fecha: S1 = semana actual + 1 (`:88`) |
| `orchestation/ValidacionMultisemanaIT` | 2–4 por secuencia (13 secuencias) | Hecho, falta puntual o recurrente, falta de tiempo, 0 minutos, fatiga, parcial con dolor, demasiado difícil, desmotivación, varios motivos, alternancia, sin registro. Linaje y "lo que dolió no vuelve" | **Sí** (2 casos omitidos por propiedad, entre ellos `cambiaSuPerfil`) |
| `orchestation/CicloConcurrenteIT` | 1–2 | Carreras, dos instancias, conexión de bloqueo perdida | **Sí** |
| `integral/CicloCompletoHttpIT` | hasta 3 | Flujo HTTP completo con JWT: parcial, baja adherencia, dolor, retirada | **Sí** |
| `planning/GeneracionAsincronaResilienciaIT`, `ReanudacionConReplicateSimuladoIT` | — | Reinicio y segunda instancia en la generación asíncrona | **Sí** |
| `orchestation/EvaluacionLimpiaProgresionIT` | 10 (72 perfiles) | Patrón fijo con fatiga y falta de tiempo | No (`-Dfitsense.evaluacion.limpia=true`) |
| `orchestation/CampanaP123Cohorte02IT` | 10–16 | Reincorporación hasta S16 | No (`-Dfitsense.campana.p123=true`) |
| `orchestation/CampanaMvp8UsuariosIT` | 8–10 | 8 usuarios con los servicios y controladores reales | No. **Llama a Replicate salvo con `-Dfitsense.campana.ia=false`** |
| `orchestation/EvaluacionK20IT` | 2 ciclos | Modos DETERMINISTA y SIMULADA | No |

Todos avanzan las semanas llamando a `WeeklyCycleService.runFor(...)` con fechas explícitas y `RelojPruebas`. **El código principal no tiene un `Clock` global.** El planificador, los vencimientos y las sesiones usan `now()` del sistema, así que el reloj no se puede inyectar.

### 4.2 Capacidad frente a lo que pide la orden

| Escenario | ¿Cubierto hoy? |
|---|---|
| Adherencia alta, media y baja | Sí (por secuencias cortas) |
| Sesiones completas, parciales y omitidas | Sí |
| Dolor y restricciones nuevas | Sí |
| Adaptaciones sucesivas | Sí (hasta 17 ciclos de un solo patrón) |
| Semanas sin datos | Sí (`INSUFFICIENT_DATA`, sin registro) |
| Interrupción temporal y reanudación | Parcial (reincorporación). **No cubre un ciclo saltado** |
| Cierre semanal real (planificador, hora y zona) | **No** |
| Progresión y reincorporación | Sí |
| Reinicios del backend | Solo la generación asíncrona. **El ciclo no** |
| Duplicación de planes | Sí (concurrencia) |
| Integridad del historial en 12 semanas con patrones mixtos | **No** (no hay un único arnés de 12 semanas) |

### 4.3 Propuesta de arnés (no implementada)

**Objetivo:** una IT nueva, `Estudio12SemanasIT`, que se ejecute en la suite por omisión, sin red y en menos de 10 minutos.

1. **Reloj controlable.**
   - Un *bean* `Clock` opcional en el código principal. Ya lo aceptan `ConsentimientoService`, `PasswordResetService` y `LimitePorIp`.
   - Usarlo también en `WeeklyCycleScheduler`, `OverdueWorkoutScheduler`, `StaleSessionScheduler` y `WorkoutSession`.
   - Esto es un cambio de inyección, no de reglas. Es prerrequisito para probar **el planificador real** en lugar de `runFor`.
2. **IA simulada determinista.**
   - `@MockitoBean ReplicateTrainingPlanGenerator` y `ReplicateSessionFixGenerator`, con respuestas grabadas de las campañas reales (`D/campana_mvp/evidencia/predicciones`).
   - Las respuestas se reutilizan, nunca se piden de nuevo.
3. **Cohorte sintética de 8 a 12 perfiles** con patrones por semana:
   - adherencia alta, media y baja;
   - completa, parcial y omitida;
   - dolor en S3;
   - pausa entre S5 y S6 y vuelta en S7;
   - una semana sin datos;
   - fatiga.
4. **Fallos inyectados:**
   - *tick* de las 03:00 perdido en S4;
   - reinicio de contexto (cerrar y abrir el `ApplicationContext` sobre el mismo contenedor) en S8 durante la IA;
   - dos instancias en S10.
5. **Comprobaciones semanales:**
   - exactamente un plan `ACTIVE` por usuario y semana;
   - linaje continuo (`parent_plan_id`);
   - una medición por semana;
   - lo que dolió no vuelve;
   - ningún plan que no supere V1;
   - número de llamadas a la IA igual al esperado (sin gasto duplicado);
   - una semana perdida se recupera (cuando exista P0-05).
6. **Salida:** una tabla CSV por usuario y semana en `target/`, comparable entre ejecuciones.

**Restricciones:**
- no ejecuta el cierre semanal real contra ninguna base;
- usa solo Testcontainers;
- no toca el usuario demo 48 ni ningún dato real.
