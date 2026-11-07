# Cohorte L01–L16 · manifiesto (independiente; publicada ANTES de ejecutarla)

**Fecha:** 05/11/2026.
**Código cerrado:** `30f1647`. Ninguna línea de código cambia después de publicar esta cohorte.
- **Unitarias:** 1.263 ejecutadas, 0 fallos.
- **Regresión `verify -Pintegration`:** 528 IT, 0 fallos (43 omitidas: los laboratorios opcionales).

Los commits posteriores a `30f1647` solo añaden documentos y evidencia.

## Ficheros

| Fichero | SHA-256 |
|---|---|
| `L_PERFILES_INDEPENDIENTES.csv` | `bc0baebf465b26e8a1cca36f762a92f10f55b58483c06a7d5fa0b723bde5d657` |
| `generar_cohorte_L.py` | `d92e5025909b3332d5eeadcfd6a6ab18ec63c36b63002dabae4ac14380a2fb8a` |

## Reproducción

```
cd docs/FitSense
python3 -I rediseno_politicas/cohorte_L/generar_cohorte_L.py \
  short_paper_ieee/evaluacion_final_k20/entrada/03_PERFILES_U01_U20.csv \
  short_paper_ieee/k20_adaptacion_real/entrada/J_PERFILES_INDEPENDIENTES.csv salida.csv
sha256sum salida.csv
```

## Regla de construcción

La regla es determinista y está fijada en el docstring del generador. Difiere de las de H, J y K en nivel, ubicación, días y minutos.

| Eje | L | K | J | H |
|---|---|---|---|---|
| Nivel | `(2g + 2k + 1) % 3` | `(2g + k) % 3` | `(g + k + 1) % 3` | `(g + 2k) % 3` |
| Ubicación | `(2g + 2k) % 3` | `(g + 2k + 2) % 3` | `(2g + k + 1) % 3` | `(g + k) % 3` |
| Días | **2, 3, 4 o 5**: `(g + 3k) % 4` | 3 o 4 | 3 o 4 | 3 o 4 |
| Minutos | 31, 42, 53 o 64: `(g + 3k + 1) % 4` | 33, 44, 56, 67 | 36, 47, 59, 70 | 30, 45, 60, 75 |

Por qué difiere así:
- L es la primera cohorte con **2 y 5 días**. Con 5 días, la división incluye PULL, que es donde actúa P-G3.
- Los minutos no los usa ninguna cohorte anterior.

**Convenio de la causa combinada (DM):** omisiones alternas, `TOO_DIFFICULT` en las sesiones pares y `LACK_OF_MOTIVATION` en las impares. Con 2 días (L16), DM se queda en una sola omisión `TOO_DIFFICULT`, y así se informa.

## Reglas de uso

1. La regla y el CSV se publican (commit y push) **antes** de ejecutar nada sobre L.
2. Los resultados de L **no se usan para cambiar el sistema, las políticas, el prompt ni los oráculos**. Lo que aparezca, fallos incluidos, se informa como resultado.
3. Sobre L solo se ejecutan modos gratuitos:
   - D y D′ con `PLAN-REV-2.0`;
   - D con `PLAN-HIST-1` como referencia;
   - IA simulada con `PLAN-REV-2.0`.

   **Replicate no está autorizado.**
