# Auditoría funcional del backend (módulos A–G)

Fecha: 16/11/2026. Base: `generation` (`2578719`) con #73 (rama `validacion/backend-integral`). Lo asíncrono se revisa
sobre #72 (`8658089`). El catálogo no se reaudita: se usa el congelado (v1.1, V57).

> **Actualización 17/11/2026.** #73, #71 y #72 ya están en `generation` (`b9c51ef`): la generación asíncrona (V58 y V59) es parte de la base. Las referencias a #71/#72 de este documento describen dónde se probó cada cosa antes de integrarla.

**Criterio:** un módulo se considera **validado** solo si hay una prueba que lo ejecuta y pasa. Las pruebas deben estar
en la regresión completa (1540 unitarias y 759 de integración en verde) o en `CicloCompletoHttpIT` (9/9). Las
afirmaciones sin prueba se marcan como tales.

Clasificación:

- **Completo:** implementado, probado y sin defectos abiertos que bloqueen el flujo.
- **Parcial:** funciona, pero le falta algo para su uso real.
- **Faltante:** no existe.

## A. Usuarios y perfil — Completo (con un parcial: la recuperación de contraseña)

| Función | Evidencia | Resultado |
|---|---|---|
| Registro y login con JWT | `CicloCompletoHttpIT` (cada prueba registra y entra), `ContratoErroresApiIT` | Validado |
| Perfil: crear, leer, actualizar y guardar los bloqueos | `CicloCompletoHttpIT` (4 objetivos, casa y gimnasio, bloqueos), `PerfilCongeladoIT` | Validado |
| P-01: duración mínima por objetivo (30–75 min) | `PoliticaP01DuracionMinimaTest`, `PoliticasP123IT` | Validado |
| Retiro del estudio | `CicloCompletoHttpIT.restoDeLaApi` (`withdrawnAt`, nada borrado) | Validado |
| Roles USER y ADMIN | `SeguridadRolesIT`, `CicloCompletoHttpIT` (403 / 200) | Validado |
| Recuperación de contraseña | `RecuperacionContrasenaIT` (token, validez, límite) | **Parcial:** no hay proveedor de correo (`delivery=none`) |

## B. Generación inicial — Completo en el camino síncrono; asíncrono pendiente de integrar

| Función | Evidencia | Resultado |
|---|---|---|
| Plan para los 4 objetivos | `CicloCompletoHttpIT.cumpleTodo` ×4 (201, ACTIVE, sin sesiones bloqueadas); `GeneracionPorObjetivoIT`; `GeneracionReferencia*IT` | Validado |
| Validadores (V1–V27, V24 por capacidades, V8C) | Toda la batería `planning` (167 clases) | Validado, sin cambios |
| V24 en la generación inicial | Sin modificar (restricción de la orden) | Validado |
| Concurrencia: un solo plan activo | `GeneracionInicialConcurrenteIT`, índice `ux_plan_active` | Validado |
| Respaldo del motor de reglas cuando falla la IA | `Respaldo*IT` (5 y 6 días), `R-1` (14), cortocircuito | Validado |
| Limitaciones explicadas antes de generar | `CicloCompletoHttpIT.coberturaParcialExplicada` | Validado |
| NO_FACTIBLE | Con `ENFORCE_*`: 422 (`SlotCoverage4CTest`). Con la configuración entregada (`pregeneration=OBSERVE`): se publica una cobertura parcial explicada. | Validado, con una **decisión por confirmar** (PD-07) |
| Generación asíncrona (V58/V59) | #71/#72: `GeneracionAsincronaIT` y los 20 escenarios de resiliencia | **Integrada** en `generation` el 17/11/2026, con el CI en verde. |
| IA real (Replicate) | No se ejecuta en esta orden | Fuera de alcance |

## C. Ejecución — Completo

| Función | Evidencia | Resultado |
|---|---|---|
| Iniciar, registrar por ejercicio y terminar | `CicloCompletoHttpIT` (todas), `EjecucionEntrenamientosIT` | Validado |
| Omitir con causa | `CicloCompletoHttpIT.omiteYBajaAdherencia` (LACK_OF_TIME, FATIGUE y PAIN_OR_DISCOMFORT conservadas) | Validado |
| Abandonar: no cuenta y la sesión sigue exigible | `CicloCompletoHttpIT.restoDeLaApi`, `AbandonoIT` | Validado |
| Reporte retroactivo | `CicloCompletoHttpIT.restoDeLaApi` | Validado |
| Una sesión en curso y un intento que cuenta | `ux_session_active`, `ux_session_counted`; `EjecucionEntrenamientosIT` | Validado |
| Recursos ajenos: 404 | `ContratoErroresApiIT`, `CicloCompletoHttpIT` | Validado |
| Sesión bloqueada por seguridad (`safetyBlocked`) | `PublishedPlanSafetyServiceDolorTest`, `ReparacionLinajeIT` | Validado. Conocido: tras una reparación NO_VALIDA la sesión sigue bloqueada hasta el ciclo siguiente (PD-04). |

## D. Adherencia — Completo (fórmula oficial intacta)

| Función | Evidencia | Resultado |
|---|---|---|
| A_e, A_s y A_w (`ADHERENCIA.md`) | `AdherenciaIT` (13), `AdherenciaSinPesosTest`, `AdherenciaSesionesProgramadasTest` | Validado |
| Cifras exactas de extremo a extremo | `CicloCompletoHttpIT`, que da el valor al centésimo con la fórmula oficial: 100 (todo), ⌈n/2⌉/n (parcial), 33,33 (dos omitidas) y 66,67 (abandono + reporte + omisión) | Validado |
| Riesgo y abandono | `AdherenciaRiesgoIT`, `AbandonoIT` | Validado |
| La fórmula no se ha cambiado | Ningún diff en `analytics` ni `execution` | Validado |

## E. Adaptación — Completo

| Función | Evidencia | Resultado |
|---|---|---|
| Decisiones NO_CHANGE / INSUFFICIENT_DATA / ADAPT_LOCALIZED / ADAPT_EXPANDED | `AdaptationOrderDeciderTest`, `AdaptacionCompletaIT`, `AdaptacionPorCausasIT` | Validado |
| Estados APLICADA / SIN_EFECTO / RECHAZADA / RESPALDO / NO_FACTIBLE / SIN_ORDEN | `EstadoAdaptacionSinIaTest`, `AdaptationOutcomeTest` | Validado |
| Ciclo semanal de extremo a extremo (sin IA) | `CicloCompletoHttpIT`: cada cierre publica una semana publicable y registra la decisión | Validado |
| Dolor: P-02 (memoria) y P-03 (regeneración segura) | `PoliticasP123IT` (39), `CicloCompletoHttpIT.dolorYReincorporacion` | Validado. P-03 sigue sin activar, como pide la orden. |
| H2 | Sin activar (restricción) | — |
| Intervenciones visibles | `CicloCompletoHttpIT.restoDeLaApi` (API = base) | Validado |

## F. Progresión y reincorporación — Completo

| Función | Evidencia | Resultado |
|---|---|---|
| Progresión tras dos semanas al 100 % | `ProgresionCienPorCienIT`, `ProgresionIT`/Rev2 (24 + 24); `CicloCompletoHttpIT` (la semana 3 sube) | Validado |
| Reincorporación autorizada y no autorizada | `ReincorporacionTrasProgresionTest`, `Reincorporacion4DTest`, `HerenciaReduccionV20Test`, `PoliticasP123IT` | Validado |
| Reparación del linaje (P-P2, H6) | `ReparacionLinajeIT`/Rev2 (deterministas desde #73), `LinajeIntegridadIT`/Rev2 | Validado |
| Fechas: domingo/lunes, mes, año, bisiestos | `TrainingWeekTest`, `repararYHeredarEnCambiosDeMesYAno` | Validado |
| Detalle | `docs/FitSense/correccion_ci_linaje/REGRESION_PROGRESION.md` | — |

## G. Trazabilidad y analítica — Completo para el estudio; panel web parcial

| Función | Evidencia | Resultado |
|---|---|---|
| Historial de planes y versiones (REPLACED) | `CicloCompletoHttpIT` (`/plans` y `/plan/{id}` legibles), `LinajeIntegridadIT` | Validado |
| Causas por semana (V57) | `AdaptacionPorCausasIT`, `TrazaHistoricaIT` | Validado |
| Traza de la política y de V4-C | `TrazaV4CTest`, `ConfiguracionEvaluadaTest` | Validado |
| Analítica del panel (resumen, adherencia, tendencias, segmentos, participantes) | `SeguridadRolesIT`, `CicloCompletoHttpIT` (el admin ve el 100 del participante) | Validado (solo lectura) |
| Vista del panel por participante: planes, sesiones, adaptaciones, exclusiones; exportación | No existe | **Faltante** (`CONTRATOS_MOBILE_WEB.md` §2) |

## Defectos confirmados frente a mejoras

**Defectos confirmados en esta orden:** ninguno en el código de producción.

El único fallo encontrado fue el de las dos pruebas de linaje, que dependían de la fecha (caso A). Está corregido en
#73 sin tocar producción.

**Decisiones por confirmar y mejoras**, que no son defectos (detalle en `PENDIENTES_PRIORIZADOS.md`):

- un slot vacío publica una cobertura parcial en vez de dar 422 (PD-07);
- la sesión queda `safetyBlocked` tras una reparación NO_VALIDA (PD-04);
- falta el proveedor de correo (PD-03);
- el panel web es de solo lectura y sin exportación (PD-05/PD-06);
- 38 pruebas dependen de `LocalDate.now()` (PD-01);
- el interruptor de la IA es local a cada instancia (PD-08).
