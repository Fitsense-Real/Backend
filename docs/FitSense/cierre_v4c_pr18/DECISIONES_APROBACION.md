# Decisiones y autorizaciones pendientes

**Nada de lo siguiente se ha hecho.** Cada punto requiere tu aprobación expresa.

## 1. PR #18 (versionado + V4-C)

| # | Acción | Estado | Qué hace falta |
|---|---|---|---|
| 1 | **Fusionar** el PR #18 en `generation` | Listo para revisión (borrador, CI verde, fusionable) | Tu aprobación. Si quieres, pasarlo antes de borrador a «listo para revisión» |
| 2 | **Desplegar** `generation` con el PR #18 | No hecho | Aprobación expresa. Flyway aplica **V44** al arrancar: dos columnas NULL, sin tocar datos. Sin migraciones manuales en Neon ni en Railway |
| 3 | **Activar PLAN-V4-0.1** (`fitsense.planning.policy=PLAN-V4-0.1` y `allow-candidates=true`) | Apagado | Aprobación expresa, **después** de un despliegue con PLAN-HIST-1 que confirme V44 y las trazas |
| 4 | Vigilar tras activarlo | — | Comprobar en las trazas: `limite_v4_aplicado.regla = V4C_SEGUNDOS` y ninguna sesión publicada con `dentro_limite_v4_aplicado = false` |

## 2. PR #17 (C2-D, EF y opciones)

| # | Acción | Estado | Qué hace falta |
|---|---|---|---|
| 5 | Actualizar el PR #17 desde `generation` | **No hecho** | Solo después de fusionar el PR #18. Con *merge*, no *rebase*. En `PlanningPolicyTrace` hay que **llevar los campos de esta orden** a la versión del PR #17 (`CORRECCION_TRAZABILIDAD.md` §5); después, repetir su regresión |
| 6 | Revisión profesional | Paquete listo, **sin enviar** | Elegir a los evaluadores y **aprobar los criterios A1–A6 antes** de enviar (`PREPARACION_EVALUACION_PROFESIONAL.md`) |
| 7 | Activar C2-D, EF, LW-PREF-0.2, REP-SUST-0.1 o NOFACT-0.1 | Apagados | Resultado de la revisión profesional, más Flutter (duración, NO_FACTIBLE y equipamiento), más una aprobación por componente. PLAN-C2D-0.1 **sin** EF no se recomienda para usuarios reales |
| 8 | Lista de equipamiento unificada | Propuesta | Aprobación de un cambio de contrato en `/limitations` |
| 9 | Flutter | Sin tocar | Autorización para empezar el trabajo móvil (`ESPECIFICACION_FLUTTER.md`) |

## 3. Se mantiene sin autorización (no se ha tocado)

- fusiones y despliegues;
- migraciones remotas;
- políticas candidatas;
- la fórmula de V4-C;
- V20, V24, V8C, 4A, 4C y las dosis 4D;
- la progresión y la reincorporación;
- el catálogo;
- Flutter;
- Replicate;
- evaluaciones profesionales.
