# 09 · Resultados de CI

**Workflow:** `.github/workflows/ci.yml`, trabajo `verify` (`mvn -B -ntp verify -Pintegration`). Ejecuta las unitarias y las IT no *opt-in*, con PostgreSQL de Testcontainers y sin Replicate.

| PR | Rama | Commit | Run | Resultado |
|---|---|---|---|---|
| `generation` (referencia) | `generation` | `0974fb3` | 41545633892 | ✅ success |
| **A (#36)** | `integration/h1-multiconjunto-mvp` | **`db11d98`** (código `983e216` + documentos) | 41709276643 | ✅ success |
| **B (#37)** | `integration/adjustment-messages-mvp` | `a70a1f0` | 41711784004 | ⚪ **cancelled**: la sustituyó la subida siguiente. **No cuenta como superada** |
| **B (#37)** | `integration/adjustment-messages-mvp` | **`a7e8de4`** (código `cc2bf7b` + documentos) | 41712122802 | ✅ success |

El commit que añade este documento solo cambia `docs/`. Su CI se ve en el PR B y se informa en el resumen final.

**Comprobaciones antes de recomendar la fusión (fase 8):**

| Comprobación | A | B |
|---|---|---|
| Diff completo contra `generation` revisado | ✅ 7 archivos de código y prueba + `docs/` (`03`) | ✅ 10 archivos de código y prueba sobre A + `docs/` (`evidencia/diff_B_contra_A_stat.txt`) |
| Sin código experimental no autorizado | ✅ ni V2, ni H12, ni laboratorios | ✅ |
| SHA final verificado | `db11d98` | el último commit del PR |
| CI sobre ese SHA | ✅ | ✅ en `a7e8de4`; el del commit de documentación, en el PR |
| Sin conflictos | ✅ (`mergeable_state` limpio; base sin cambios) | ✅ (base = A) |
| Sin migraciones inesperadas | ✅ 0 | ✅ 0 |
| Configuración por defecto revisada | `pattern-rule=multiconjunto`; V2 inexistente; H12 inexistente | Sin propiedades nuevas |
| Reversión documentada | ✅ `02` §4 y `03` | ✅ `02` §4 |
