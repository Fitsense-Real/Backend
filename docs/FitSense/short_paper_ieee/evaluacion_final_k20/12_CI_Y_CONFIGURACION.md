# 12 · CI y configuración

| Elemento | Valor |
|---|---|
| Rama | `eval/final-ieee-k20` (no se fusiona) |
| PR | [#40](https://github.com/Fitsense-Real/Backend/pull/40), borrador abierto solo para la CI (job `verify` = `mvnw verify -Pintegration`) |
| SHA congelado | `a31c36bbb0c94eb99c84d8fcb37465797efec459` |
| `src/main` | = `7ee69c2`: `generation` `76e15c5` + #38 + #39 + P3 |
| CI | **Verde** en `1986b2d` (mismo `src/` que el SHA congelado), run 41859969622 |
| Regresión local | 1.185/0/0 unitarias · 480/0/41 integración (`11`) |
| Base | PostgreSQL 16.15 efímero (Testcontainers). Ni Railway ni Neon |
| Migraciones | 46 (última 44.2), checksum `9981294ded945dc1882bd26f2468c30f`. Ninguna nueva |
| Catálogo | 1.219 activos, huella `425fc958d26b405a00a233fe9d8a3c3b`, sin cambios |
| Perfiles | SHA-256 `584e2204…` (= blob histórico `95c2839`) |
| Configuración de cálculo | `MVP-1.8` |
| Configuración de la candidata | Límite `preferencia`, retirada `on`, progresión `off`, V8C multiconjunto, PLAN-HIST-1, prompt V1 (`01`) |
| IA (D, D′) | Desactivada; 0 llamadas |
| IA (R, R′) | Pendiente; `gpt-5-mini` / `openai/gpt-5-structured` / `medium` |

## Comandos de la evaluación congelada

Ejecutar desde la raíz, con `SHA=$(git rev-parse HEAD)`:
```
sh ./mvnw -o -B -ntp verify -Pintegration -Dit.test=EvaluacionK20IT -DskipUTs=true -Dtest=NONE \
  -Dsurefire.failIfNoSpecifiedTests=false -Dfitsense.evaluacion.k20=true \
  -Dfitsense.evaluacion.k20.modo=DETERMINISTA \
  -Dfitsense.evaluacion.k20.salida=docs/FitSense/short_paper_ieee/evaluacion_final_k20/evidencia/final/D \
  -Dfitsense.evaluacion.k20.commit=$SHA
python3 -I docs/FitSense/short_paper_ieee/evaluacion_final_k20/herramientas/oraculo_k20.py <carpeta> \
  docs/FitSense/short_paper_ieee/evaluacion_final_k20/entrada/03_PERFILES_U01_U20.csv \
  docs/FitSense/short_paper_ieee/evaluacion_final_k20/entrada/04_ORACULO_TEORICO_HISTORICO.csv
python3 -I docs/FitSense/short_paper_ieee/evaluacion_final_k20/herramientas/test_oraculo_k20.py
python3 -I docs/FitSense/short_paper_ieee/evaluacion_final_k20/herramientas/informe_k20.py
```

## Estado de la CI

| Commit | Run | Resultado |
|---|---|---|
| `1986b2d` | [41859969622](https://github.com/Fitsense-Real/Backend/actions/runs/41859969622) | **verify ✔ success** |
| `a31c36b` | 41858257622 | Cancelada: la sustituyó el push siguiente |
| `f41aafb` | 41857886460 | Cancelada: la sustituyó el push siguiente |

`1986b2d` tiene el mismo `src/` que el SHA congelado `a31c36b` (`git diff --stat a31c36b 1986b2d -- src` vacío): solo añade evidencia y documentación. Por eso su CI verde vale para el código congelado.
