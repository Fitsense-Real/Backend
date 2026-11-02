# Estado inicial del backend (fase 1)

**Verificado el 02/11/2026** sobre `origin/generation` y `origin/feature/c2d-v4c-versionado`. Solo se ha leído el repositorio y GitHub: nada se ha desplegado ni migrado.

## 1. Comprobaciones

| Comprobación | Resultado | Evidencia |
|---|---|---|
| `generation` contiene los PR #18, #19, #20 y #21 | **Sí** | Fusiones bfd5756 (#18), dd61837 (#19), 8f112d5 (#20) y **7c19703** (#21, cabeza actual) |
| CI verde en el último commit | **Sí** | `verify`: ejecuciones 116 (*push*) y 117 (PR), *success* en 7c19703 |
| V44 una sola vez | **Sí** | `V44__planning_policy_version.sql`, archivo único (SHA-256 `640b0169…`) |
| PR #17 en borrador | **Sí** | Fitsense-Real/Backend#17, `draft: true`, CI verde en 3a27f0b |
| V45 solo en el PR #17 | **Sí** | `V45__planning_policy_elegibilidad_funcional.sql` solo existe en `feature/c2d-v4c-versionado` |
| Política por defecto | **`PLAN-HIST-1`** | `PlanningPolicySelector`: `@Value("${fitsense.planning.policy:PLAN-HIST-1}")` |
| `allow-candidates` | **`false`** | Mismo selector |
| Políticas experimentales activas | **Ninguna** | C2-D, EF, LW-PREF, REP-SUST y NOFACT-0.1 no están en `generation`. En el PR #17 están apagadas |
| Cambios de producción sin documentar | **Ninguno conocido** | Sin despliegue ni migraciones remotas en esta etapa. **No se ha consultado** el estado real de Neon ni de Railway, por orden: ver `MIGRACIONES_Y_DESPLIEGUE.md` §2 |
| Secretos en el repositorio | **Ninguno** | `DB_PASSWORD`, `JWT_SECRET` y `REPLICATE_API_TOKEN` son variables de entorno sin valor por defecto |
| Riesgo de configuración | **Sí** | La URL y el usuario de **Neon** son el valor por defecto de `application.properties`: ver `SEGURIDAD_Y_CONFIGURACION.md` §1 |

## 2. Resultados de referencia y sus limitaciones

Son resultados ya obtenidos; no se repiten aquí. La regresión de esta etapa está en `REGRESION_CIERRE_BACKEND.md`.

| Resultado | Commit | Qué demuestra | Limitación |
|---|---|---|---|
| 1.046 unitarias | 51a311f (PR #17) | Estabilidad del código del PR #17 | `generation` tiene 1.028 |
| 441 de integración aprobadas y 57 omitidas | 51a311f | Flujos completos con PostgreSQL efímero | Las 57 omitidas son *opt-in* o de IA real: **no cuentan como aprobadas** |
| 4 baterías × 223 | 51a311f | Las mismas 15 clases pasan con cada política candidata | Solo el PR #17 tiene las cuatro políticas |
| 960 perfiles × 5 políticas idénticos | 51a311f frente a referencias | Estabilidad de la **generación inicial** con el motor de reglas | No cubre la adaptación semanal ni la IA real |
| REPLAY de 100 semanas | 51a311f | Ciclo semanal reproducible con IA **simulada** | No es una validación con IA real ni con personas |
| 48 perfiles sin tracción (EF) | 51a311f | EF los identifica | Su idoneidad está **pendiente de la evaluación profesional** |

**Ninguno de estos resultados es una validación profesional del entrenamiento.**
