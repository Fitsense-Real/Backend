# Dependencias del versionado y de V44: cómo se integra el PR #17 cuando V44 ya esté en `generation`

## 1. Situación

| Rama | Migraciones de planificación | Estado |
|---|---|---|
| `generation` (afef98a) | Hasta V43 | Estable |
| `feature/v4c-versionado-aislado` (nuevo PR borrador) | **V44** | Sin fusionar |
| `feature/c2d-v4c-versionado` (PR #17, borrador) | **V44** (idéntica) y **V45** | Sin fusionar |

**V44 es la misma en ambas ramas:**
- mismo nombre de archivo (`V44__planning_policy_version.sql`);
- mismo contenido byte a byte (SHA-256 `640b0169d20f24c0ae7a68c2cff99d2c7f24b272910dd5c6ede8acae2e708400`), y por tanto la misma suma de verificación de Flyway.

**No existe una segunda V44** con otro contenido. **V45 solo existe en el PR #17.**

**No se ha aplicado ninguna migración en Neon ni en Railway.** Todas las pruebas se ejecutaron en PostgreSQL efímero (Testcontainers).

## 2. Orden de integración propuesto (pendiente de aprobación)

1. **Revisar y fusionar** el PR de la rama aislada en `generation`. Esto requiere aprobación expresa.
2. **Actualizar el PR #17** fusionando `generation` en `feature/c2d-v4c-versionado`. Ver el §3; con *merge*, no *rebase*.
3. Repetir en el PR #17:
   - la regresión completa;
   - las baterías por política;
   - `ComparacionPerfilesIT`.
4. El PR #17 queda con un diff frente a `generation` que **ya no contiene V44** ni el versionado básico; solo C2-D, EF, V45 y las opciones.

**Bases reales:**
- Flyway aplicaría V44 al desplegar `generation`, y V45 solo cuando se despliegue el PR #17.
- Con la misma suma de verificación, una base que ya tenga V44 no la vuelve a aplicar ni falla la validación.
- **Ningún despliegue está autorizado.**

## 3. Simulación de la actualización del PR #17 (hecha, sin publicar)

**Pasos:**
1. En un *worktree* temporal sobre el PR #17 (254a18e) se ejecutó `git merge --no-commit --no-ff feature/v4c-versionado-aislado`.
2. Se revisaron los conflictos.
3. No se guardó ni se publicó nada.

**V44:** sin conflicto, porque las dos ramas añaden el mismo archivo con el mismo contenido. Queda **un único** archivo V44, junto a V45.

**Conflictos esperados** (11), todos con la misma regla: **conservar la versión del PR #17**, que es un superconjunto de la aislada:

| Archivo | Tipo |
|---|---|
| `PlanningPolicySelector.java` | add/add |
| `PlanningPolicy.java` | add/add (el PR #17 tiene las 7 versiones y los indicadores c2d/EF) |
| `PlanningPolicyTrace.java` | add/add (el PR #17 añade C2-D y EF) |
| `WeeklyTrainingPlanCommandServiceImpl.java` | contenido (el PR #17 añade las opciones LW/REP) |
| `WeeklyTrainingPlan.java` | contenido (solo comentarios) |
| `SessionFixPromptBuilder.java` | contenido (el PR #17 añade la regla EF) |
| `RuleBasedTrainingPlanGenerator.java` | contenido (el PR #17 añade REP-SUST) |
| `EsquemaVigenteSobreCatalogoAnterior.java` | contenido (el PR #17 añade V45) |
| `PoliticaPlanificacionTest.java`, `ContratoV4CIATest.java`, `V4CValidadorRealTest.java` | add/add (las del PR #17 cubren también C2-D y EF) |

**Resultado de la simulación**, tras resolver con «la versión del PR #17»:
- `src/main` queda **idéntico** al PR #17: 0 archivos distintos;
- solo se incorporan dos pruebas de la rama aislada:
  - `PoliticaV4CCicloIT`, que compila contra el PR #17 y añade cobertura;
  - `ComparacionPerfilesIT`. El PR #17 tiene su propia versión con 5 políticas, que también se conserva al resolver;
- `mvn test-compile` sobre el resultado: **correcto**.

**Defecto detectado en la simulación y corregido:**
- **El defecto:** en la primera versión de la rama aislada, el método `excede(...)` de `CapabilityWeekBuilder` estaba al final de la clase, y en el PR #17 está antes de `minutos(...)`. Git fusionaba **sin conflicto, pero duplicando el método**, y el resultado no habría compilado.
- **La corrección:** se movió el método a la misma posición que en el PR #17 (commit 6aec7e0). El fragmento es ahora idéntico en las dos ramas y git lo une limpiamente.
- **Quedó verificado** repitiendo la simulación.

## 4. Reglas para cuando se actualice el PR #17

- **Merge**, no *rebase* ni *force-push*: el PR #17 conserva su historia.
- Al resolver, no tocar V44: debe quedar el único archivo, sin cambios.
- **No crear** V44b ni renumerar V45.
- Si la revisión del PR aislado cambiara V44 antes de fusionarse, el cambio debe aplicarse **también** al PR #17 en el mismo contenido. Si no, Flyway detectaría sumas distintas.
- Después de fusionar: la regresión completa del PR #17 y la comparación de 960 perfiles, que debe seguir dando 0 diferencias con PLAN-HIST-1.
