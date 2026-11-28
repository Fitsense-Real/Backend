# Cohorte M01–M30 · manifiesto (independiente; publicada ANTES de ejecutarla)

**Fecha:** 05/11/2026.
**Código cerrado:** `9c956a0`, último commit de `src`; unitarias 1.301/0. Los commits posteriores solo añaden documentos y evidencia.

## Ficheros

| Fichero | SHA-256 |
|---|---|
| `M_PERFILES_INDEPENDIENTES.csv` | `c82756c157f570a4eb5e4fc355fdda7d50a708ca91d3f76476e3c8cbb1ab6b41` |
| `generar_cohorte_M.py` | `dd9353a437da0ca6d64bbc5f5c30e206590c4cafe4e05c691f018d46ee24f6a0` |

## Reproducción

```
cd docs/FitSense
python3 -I rediseno_politicas/cohorte_M/generar_cohorte_M.py \
  short_paper_ieee/evaluacion_final_k20/entrada/03_PERFILES_U01_U20.csv \
  short_paper_ieee/k20_adaptacion_real/entrada/J_PERFILES_INDEPENDIENTES.csv salida.csv
sha256sum salida.csv
```

**Transparencia:** antes de publicar la regla (commit `c2dbb86`), el generador se ejecutó **solo** para comprobar que producía un CSV bien formado, en un fichero del scratchpad. El sistema **nunca** se ha ejecutado sobre M. El CSV publicado es idéntico byte a byte a esa prueba (`cmp`).

## Regla de construcción

La regla completa está en el docstring del generador.

**M01–M28: rejilla de 4 objetivos × 7 valores de días (1–7).**
- Nivel, ubicación y minutos rotan:
  - nivel `(g + d) % 3`;
  - ubicación `(g + 2d) % 3`;
  - minutos `[30, 31, 38, 46, 57, 66, 75][(g + 3d) % 7]`.
- Esos minutos incluyen las dos fronteras de P-01 y valores que no usa ninguna cohorte anterior.
- Causas en rotación: fatiga, demasiado difícil, desmotivación, difícil + desmotivación, y parcial (P40).
- **La secuencia se escribe sobre las sesiones PROGRAMADAS (S-MAX)**, no sobre los días pedidos. Es la lección de L05.

**M29–M30: casos frontera** fijados de antemano.
- **M29:** INCREASE_STRENGTH intermedio en gimnasio, 1 día de 30 min.
  - Esperado con REV2: **sin plan** (`FUERZA_SIN_TIEMPO`).
  - Esperado con HIST: plan.
- **M30:** GAIN_MUSCLE principiante en casa sin material, 7 días.
  - Esperado: **4** sesiones programadas.

La columna `esperado` del CSV recoge el resultado esperado de cada perfil **antes** de la ejecución. **No** es un criterio del oráculo.

## Reglas de uso

1. La regla y el CSV se publican antes de ejecutar nada sobre M.
2. Los resultados de M **no se usan para cambiar el sistema, las políticas, el prompt ni los oráculos**. Se informan tal cual, fallos incluidos.
3. Sobre M solo se ejecutan modos gratuitos:
   - D y D′ con REV2;
   - D con HIST;
   - IA simulada con REV2.
4. Oráculos sobre M:
   - el histórico;
   - el REV2;
   - REV2 + DIAS-1 (contrato de días corregido).
5. **Replicate no está autorizado.**
