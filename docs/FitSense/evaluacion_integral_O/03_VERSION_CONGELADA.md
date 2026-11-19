# 03 · Versión evaluada y estado congelado

| Elemento | Valor |
|---|---|
| Versión aprobada | **VERSIÓN A**: catálogo V46, aviso E2-a `DOSIS_NO_DEMOSTRADA`, P15-C desactivado, sin contrafactuales |
| Código de producción (`src/main`) | Idéntico a `f1c6c02`: `git diff f1c6c02 cb18381 -- src/main` está vacío |
| Commit ejecutado | `cb18381`: arnés extendido (solo pruebas), cohorte y oráculo congelados |
| Política | PLAN-REV-2.0. Progresión activada solo en el bloque P (O15–O20) |
| IA | Simulada (`modo=SIMULADA`, prompt V2.1, sin red). **Sin Replicate y sin gasto**: 0 llamadas al proveedor |
| Migraciones | Flyway 46 (`flyway_version` en `00_configuracion.txt` de cada corrida) |
| Cohorte | `O_PERFILES.csv` con SHA-256 `9b996825…2106`. El arnés lo comprueba y aborta si no coincide |
| Oráculo oficial | `herramientas/oraculo_integral_O.py` v1.0, SHA-256 `628be48e…b4b`. No se modificó tras ver los resultados |
| Oráculo histórico reutilizado (solo funciones puras) | `short_paper_ieee/k20_adaptacion_real/herramientas/oraculo_k20.py`, SHA-256 `d0261691…237`. Sin cambios |

## Reproducción de las huellas de referencia con el arnés extendido

Mismo commit `cb18381`, **sin el modo integral**:

| Corrida | Huella del oráculo | Referencia | Resultado |
|---|---|---|---|
| REV2 D, U | `a662cb4d` (20 PASS) | `a662cb4d` | **Igual** |
| REV2 D, N | `11f51d44` (19 PASS + N19) | `11f51d44` | **Igual** |
| HIST D, U | `f9f67973` (20 PASS) | `f9f67973` (congelada) | **Igual** |
| REV2 IA simulada, U | `84dfb6d2` | `84dfb6d2` | **Igual** |
| REV2 IA simulada, N | `da34db35` | `da34db35` | **Igual** |

**Conclusión:** el modo integral no altera las corridas anteriores, y HIST conserva su huella congelada.

## Qué se añadió (solo pruebas, opt-in)

En `EvaluacionK20IT`, con `-Dfitsense.evaluacion.k20.integral=true`:
- 4 semanas medidas con su guion y un horizonte de reincorporación;
- `GET /plan/limitations` tras la generación y al final;
- restricciones del perfil (`permite_*`, `ejercicios_bloqueados`);
- pasos nuevos: P60, HF, HD, PT, `O:<motivo>`;
- repetición idempotente del ciclo 1;
- progresión solo en el bloque que la necesita (`fitsense.evaluacion.k20.progresion=true`);
- filtro de bloque (`solo`) y comprobación del SHA de la cohorte;
- en la IA simulada, ACORTAR propone retirar el último ejercicio de relleno.

**Sin el modo integral**, el comportamiento es el de siempre (tabla anterior).
