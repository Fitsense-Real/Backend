# 13 · CI y estado de los PR (fase 9)

**Workflow:** `.github/workflows/ci.yml`, job `verify` (`mvn -B -ntp verify -Pintegration`: unitarias e integración con Testcontainers). Se dispara con `pull_request` sin filtro de rama, así que corre también en PR apilados.

**Ningún PR se ha fusionado** ni se fusionará automáticamente. Todos están en **borrador**.

| PR | Rama → base | Contenido | SHA con CI | Ejecución | Resultado |
|---|---|---|---|---|---|
| [#31](https://github.com/Fitsense-Real/Backend/pull/31) | `experiment/adaptation-functional-safety-h1-h12` → `experiment/adaptation-v2-quality-validation` (#30) | **H1:** V8C multiconjunto (apagado), pruebas, laboratorio `v8c-patron`, entregables 01–05 y evidencia | `77ca7dc` | [41662611426](https://github.com/Fitsense-Real/Backend/actions/runs/41662611426) | **success** (15:19–15:33 UTC) |
| [#32](https://github.com/Fitsense-Real/Backend/pull/32) | `docs/catalogo-revision-580-775-780-862` → rama de #31 | **Catálogo:** ficha 10 para revisión profesional (solo documentación; los datos no cambian) | `4d43ee1` | [41662819977](https://github.com/Fitsense-Real/Backend/actions/runs/41662819977) | **success** (15:20–15:34 UTC) |
| [#33](https://github.com/Fitsense-Real/Backend/pull/33) | `experiment/adaptation-h12-policy` → rama de #32 | **H12:** prototipos H12-A/H12-C (apagados), prompt filtrado (apagado), laboratorio de 3 semanas con guion H12, escenarios, entregables 06–09 y 11–15 | `a38b312` (entregables provisionales) | [41664130472](https://github.com/Fitsense-Real/Backend/actions/runs/41664130472) | **success** (15:27–15:42 UTC) |

**Notas:**
- **SHA exacto:** cada ejecución corrió sobre la cabeza del PR en ese momento (`head_sha` del evento `check_suite.completed`).
- **El último *push* de #33** añade solo documentos y evidencia (12, 13, 15 definitivos y `evidencia/regresion`). Su CI se registra en el PR y en el informe final.
- **Pruebas ejecutadas por el CI:** el mismo `verify -Pintegration` de la regresión local (`12` §1).
  - El recuento exacto del CI no se puede extraer desde esta sesión: el proxy bloquea la descarga del ZIP de registros, como en el PR #30.
  - Localmente, sobre el mismo código: 1.119 unitarias y 489 IT, 0 fallos, 37 omitidas *opt-in*.
- **Fallos y regresiones con la configuración por defecto:** ninguno.
- **Omisiones:** las 37 IT *opt-in*, más 1 unitaria (impacto H1). Se ejecutan a mano: Replicate real, campañas, 960 perfiles y REPLAY (`12`).

**Un CI verde demuestra que el código pasa sus pruebas. No que una decisión de entrenamiento esté validada profesionalmente.**
