# Decisiones pendientes (requieren tu aprobación expresa)

**No se ha fusionado, desplegado ni activado nada.**

## A. Integración y despliegue

| # | Decisión | Qué implica | Prerrequisitos |
|---|---|---|---|
| A1 | **Fusionar el PR #17 en `generation`** | Llega todo el código nuevo **apagado**: versionado, V4-C, C2-D, EF, LW-PREF-0.2, REP-SUST-0.1, NOFACT-0.1 y los contratos de API. Con la configuración por defecto el comportamiento es idéntico (960 de 960 perfiles y campaña REPLAY) | Revisión técnica del PR |
| A2 | **Desplegar V44 y V45** en staging y luego en producción | Dos columnas nullable y una lista cerrada de versiones. Flyway las aplica al arrancar | `PLAN_MIGRACIONES_V44_V45.md`, copia previa, ventana |
| A3 | **Separar V4-C** si se integra antes que C2-D y EF | Ver §E | — |

## B. Activación de políticas (cada una por separado)

| # | Decisión | Estado técnico | Bloqueos |
|---|---|---|---|
| B1 | `PLAN-V4-0.1` (solo V4-C) | Listo | Campaña con IA simulada, ya hecha en esta rama |
| B2 | **`PLAN-C2D-EF-0.1`** (o `PLAN-C2D-V4-EF-0.1`) **en lugar de** `PLAN-C2D-0.1` | Listo | **Revisión profesional** (muestra de 36). **Flutter** (contrato de duración y NO_FACTIBLE). La decisión C1 |
| B3 | No activar nunca `PLAN-C2D-0.1` sin EF | — | Publica PULL sin tracción (48 perfiles) |

## C. Producto: los 48 perfiles sin tracción

| # | Decisión | Opciones |
|---|---|---|
| C1 | **Qué ofrecer al usuario de peso corporal y 6–7 días** | a) NO_FACTIBLE con la sugerencia de declarar material, si ya lo tiene (recomendado). b) Proponer menos días: **no resuelve** la falta de tracción. c) Validar profesionalmente una tracción sin material antes de añadirla al catálogo |
| C2 | **Lista de material sugerido** | El laboratorio verificó 6: `band`, `dumbbell`, `pull-up bar`, `suspension straps`, `wall bars` y `anchor point`. `PlanLimitationsService` sugiere hoy 3 (band, wall bars, anchor point). ¿Unificar? ¿Excluir `anchor point`, que da un único ejercicio y sesiones del 55 %? |
| C3 | **Activar NOFACT-0.1** (`fitsense.api.no-factible-estructurado.enabled`) | Cuando Flutter lo consuma |

## D. Componentes experimentales

| # | Decisión | Recomendación |
|---|---|---|
| D1 | **LW-PREF-0.2** | Activarla solo dentro de una versión de política (p. ej. `PLAN-C2D-EF-LW-0.1`, V46) y tras la revisión profesional. **0 regresiones** combinada con EF |
| D2 | **REP-SUST-0.1** | **No activar.** No recupera R0040, R0328 ni R0616 (no hay ninguna sustitución de la misma clase) y solo recupera R0808 por 15 s |
| D3 | **Verificador de C2-D con sustituciones de la misma clase** | Haría a C2-D **más permisivo** (R0040, R0328 y R0616 se publicarían al 65–67 %). La orden prohíbe debilitar C2-D: **sin cambios**; decisión tuya |
| D4 | Traza `aplicada: true/false` para LW-PREF y REP-SUST | Cambio menor; útil para la auditoría en producción |

## E. Contratos

| # | Decisión | Recomendación |
|---|---|---|
| E1 | Sustituir el código `ESTRUCTURA_COMPLETA` por `DURACION_POR_CONTENIDO` | Hacerlo **antes** de activar el indicador (no hay consumidores) |
| E2 | Activar `fitsense.api.duracion-estimada.enabled` y `fitsense.metricas.aerobica.enabled` | Con Flutter |

## F. Catálogo (`CATALOGO_CORRECCIONES_PRIORIZADAS.md`)

| # | Decisión |
|---|---|
| F1 | P1: 775 (movimiento real), y requisitos de equipo de 318, 1152, 1196 y 1077 |
| F2 | ¿Se presupone una silla doméstica? (1114, 1320, 1136, 1188, 465) |
| F3 | Reclasificar estiramientos y movimientos de tronco a MOBILITY o zona waist, tras revisión |
| F4 | 320: ¿acredita hipertrofia? |

## G. Revisión profesional

| # | Decisión |
|---|---|
| G1 | **Designar uno o dos evaluadores** y entregarles: `MUESTRA_PROFESIONAL_CIEGA.csv`, `FICHAS_NO_FACTIBLE.csv`, `INSTRUMENTO_PROFESIONAL_FINAL.md` y `PLANTILLA_RESPUESTAS_EVALUADOR.csv`. **Nunca** `CLAVE_INTERNA_MUESTRA.csv` |
| G2 | Aprobar los umbrales de consenso propuestos (≥ 80 % aceptables y ningún «Seguridad» ≤ 2) |
| G3 | Decidir sobre las familias de EF (EF-0.2) según las respuestas |
