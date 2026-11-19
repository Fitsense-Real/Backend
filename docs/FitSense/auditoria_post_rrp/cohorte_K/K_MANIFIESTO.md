# Cohorte K01–K16 · manifiesto (independiente; publicada ANTES de ejecutarla)

**Fecha:** 05/11/2026.
**Contexto:** fase 8 de la auditoría post R/R′, la cohorte nueva e independiente.

## Ficheros

| Fichero | SHA-256 |
|---|---|
| `K_PERFILES_INDEPENDIENTES.csv` | `ec0c18db68be52ee75533db30c0bfddee51a580b2fd1dbbcb248ee2e69cd440a` |
| `generar_cohorte_K.py` | `c2a65aaf3bde28961c2ca9cd0cdda4668f8d584231ab8c6d9bd1badb306d8a60` |

## Reproducción

```
cd docs/FitSense
python3 -I auditoria_post_rrp/cohorte_K/generar_cohorte_K.py \
  short_paper_ieee/evaluacion_final_k20/entrada/03_PERFILES_U01_U20.csv \
  short_paper_ieee/k20_adaptacion_real/entrada/J_PERFILES_INDEPENDIENTES.csv salida.csv
sha256sum salida.csv
```

## Regla de construcción

Es determinista y se fija en el docstring del generador. Difiere de la de H y de la de J en nivel, ubicación, días y minutos.

| Eje | K | J | H |
|---|---|---|---|
| Objetivo | `GOALS[i % 4]` | igual | igual |
| Causa | `CAUSAS[i // 4]` | igual | igual |
| Nivel | `(2g + k) % 3` | `(g + k + 1) % 3` | `(g + 2k) % 3` |
| Ubicación | `(g + 2k + 2) % 3` | `(2g + k + 1) % 3` | `(g + k) % 3` |
| Días | `[3, 4][(g + 2k + 1) % 2]` | `[3, 4][(g + k + 1) % 2]` | `(g + k) % 2` |
| Minutos | 33, 44, 56 o 67, rotados `(3g + k) % 4` | 36, 47, 59 o 70 | 30, 45, 60 o 75 |

- **Minutos K:** un valor por cuartil del rango de P-01 (30–75). Ninguna cohorte anterior lo usa (U: 45 y 60; H: 30, 45, 60 y 75; J: 36, 47, 59 y 70).
- **Semilla del motor:** `userId × 1.000.003 + epochDay(2026-11-02)`, igual que en U, H y J.

## Reglas de uso

1. La regla y el CSV se publican (commit) **antes** de ejecutar nada sobre K.
2. Los resultados de K **no se usan para cambiar el sistema, el prompt ni los criterios**. Lo que aparezca, fallos incluidos, se informa como resultado.
3. Hasta que haya un presupuesto **nuevo** autorizado de forma explícita, sobre K solo se ejecutan los modos gratuitos: `DETERMINISTA` (D y D′) y la sonda de factibilidad.
