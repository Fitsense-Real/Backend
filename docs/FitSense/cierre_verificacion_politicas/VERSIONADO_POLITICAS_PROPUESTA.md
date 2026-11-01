# Propuesta de versionado de políticas

**Estado:** modelo de versiones implementado como código experimental (`VersionPoliticaPlanificacion`) y probado de forma aislada (`PoliticasCandidatasTest.versionado`). **No está cableado, no hay migración aplicada y no se ha tocado `db/migration`.**

## 1. Versiones definidas

| Id | Activa | V20 | V4 | Dosis | Tope | E |
|---|---|---|---|---|---|---|
| `PLAN-HIST-1` | **Sí** | 70 % fijo (`calculation_configs`) | HISTÓRICO (round, declarado) | 4D v3.3 / 4D v4 | V24 6/7/8 | Desactivado |
| `PLAN-C2-0.1` | No | C2-C candidata (solo si V20 es la única regla incumplida) | HISTÓRICO | igual | igual | Desactivado |
| `PLAN-V4-0.1` | No | 70 % fijo | SEGUNDOS (V4-C) | igual | igual | Desactivado |
| `PLAN-E-0.1` | No | Sin V20 (E1–E4) | SEGUNDOS | 4D base | Techos E sin aprobar | `E-0.1-EXPERIMENTAL-SIN-APROBAR` |

**Garantías probadas:**
- solo la histórica es `activable()`;
- un plan **sin versión** se lee como histórico;
- una versión desconocida es un error (no hay *fallback* silencioso).

## 2. Qué se registraría con cada plan (traza)

`traza(...)` produce:
- `politica`, `v20`, `piso_v20_usado_pct`;
- `v4`, `techo_v4_minutos`;
- `dosis`, `tope`, `modelo_e`;
- `version_calculo` (la de `calculation_configs`);
- `contexto` (generación inicial, adaptación, P-03, reincorporación).

Las autorizaciones (D8, C-2, P-02) ya tienen su propia traza y **no se duplican**: se enlazan por el id del plan.

## 3. Migración candidata (solo documentación; NO aplicar)

```sql
-- PROPUESTA, no aplicada. Requiere aprobación, una version Vnn nueva y pruebas en base efimera.
ALTER TABLE weekly_training_plans
    ADD COLUMN planning_policy_version VARCHAR(32) NULL,
    ADD COLUMN planning_policy_trace JSONB NULL;
-- NULL = historico (PLAN-HIST-1). No se rellena retroactivamente.
```

**Reglas de compatibilidad:**
1. **No se reinterpreta nada.** Las semanas, adaptaciones, reducciones, progresiones, dosis autorizadas, reincorporaciones y trazas de seguridad existentes conservan su validación (`NULL` → histórico).
2. **V8C y el linaje no cambian.** Una semana adaptada valida con la versión de su semana de origen, no con la vigente el día de la adaptación. Así una política nueva no cambia lo que D8 o C-2 autorizaron.
3. **La progresión** se mide con la misma versión que el plan base (D-P5: techo de 1,10 × V_base calculado con las reglas del plan base).
4. **Activación** con una fila nueva de `calculation_configs`, con la clave `planning_policy`. Nunca editando la activa.

## 4. Lo que falta para cablearlo (no autorizado)

| Pieza | Cambio |
|---|---|
| `PlanDraftValidator` | Rama por versión: V20 con `PisoV20Acotado` (C2); V4 con `TechoV4Versionado` |
| Generador | El mismo techo en segundos que el validador cuando la versión es V4-C |
| `WeeklyTrainingPlanCommandServiceImpl` | Guardar la versión y la traza |
| Adaptación y P-03 | Leer la versión del plan de origen |
| IA (prompt GEN-IN) | `min_session_minutes` y techo según la versión |
| Pruebas | Batería espejo con la versión candidata; la histórica debe seguir idéntica |
