# Cohorte N01–N20 · manifiesto (independiente; congelada ANTES de cualquier ejecución)

**Fecha:** 05/11/2026.

**Código cerrado:** `src/main` es idéntico a `cc76bd1` (`git diff cc76bd1 HEAD -- src/main` vacío).
- Último commit anterior a esta congelación: `a31c628`.
- Todo lo posterior a `cc76bd1` son pruebas, herramientas, documentos y evidencia.

**Verificación sobre el código final antes de congelar (F10):**
- 23 corridas K20 con las huellas de F8;
- barridos HIST y REV2 idénticos byte a byte (`pre_replicate/22_AUDITORIA_COHERENCIA.md` §3).

**Uso:** cohorte de la evaluación con IA real, opción A (U + N), según `pre_replicate/23_PROTOCOLO_EVALUACION.md`. **No autorizada todavía.**

## Ficheros

| Fichero | SHA-256 |
|---|---|
| `N_PERFILES_INDEPENDIENTES.csv` | `8cbf3b851d7b7328943b26911ae2f306518d92d7b5cd10f96acbb00caf9ad70c` |
| `generar_cohorte_N.py` | `d78400845c387d1cd287466d75234fc2e693ca8af83816868a2a0315c4ac64a0` |
| Entrada: `03_PERFILES_U01_U20.csv` | `584e2204ec8eb41ef95bb61b7fe6a6f6c90164a36fe920d1097dcae250ae2307` |
| Entrada: `M_PERFILES_INDEPENDIENTES.csv` (solo la cabecera) | `c82756c157f570a4eb5e4fc355fdda7d50a708ca91d3f76476e3c8cbb1ab6b41` |

## Reproducción

```
cd docs/FitSense
python3 -I rediseno_politicas/cohorte_N/generar_cohorte_N.py \
  short_paper_ieee/evaluacion_final_k20/entrada/03_PERFILES_U01_U20.csv \
  rediseno_politicas/cohorte_M/M_PERFILES_INDEPENDIENTES.csv salida.csv
sha256sum salida.csv
```

**Transparencia:**
- La regla (el generador) se publicó en `d93086d`, **antes** del CSV.
- Antes de esa publicación, el generador se ejecutó **solo** para comprobar que el CSV estaba bien formado y que la rejilla cubría lo previsto. Fue en un fichero del scratchpad, sin ejecutar el sistema.
- El CSV publicado es idéntico byte a byte a esa prueba (`cmp`).
- **El sistema nunca se ha ejecutado sobre N:** ni D, ni IA simulada, ni oráculo, ni barrido.

## Regla de construcción (completa en el docstring del generador)

**Diferencias con U, J, K, L y M (ninguna cohorte anterior las usa):**
- minutos 34, 41, 49, 54, 62, 68 y 72;
- días disponibles > días pedidos en 2 de cada 3 perfiles;
- conjuntos de días que empiezan en días distintos;
- el suceso de la semana medida no siempre en la última sesión.

**Causas:** las mismas de las cohortes anteriores (fatiga, demasiado difícil, desmotivación, difícil + desmotivación y parcial P40), para medir lo mismo que en U. Dolor, falta de tiempo y cambio de horario **quedan fuera**.

**Secuencias:** se escriben sobre las sesiones **programadas** (S-MAX), no sobre los días pedidos.

**Rejilla N01–N18:** los 4 objetivos, los 3 niveles y las 3 ubicaciones. N01–N12 cubren las 12 combinaciones objetivo × nivel.

**Casos frontera**, fijados por el contrato de `11_FUERZA_1_DIA_30_MIN.md` y `FuerzaSinTiempoIT` (mínimo de 32 min), no por ninguna ejecución:

| Id | Perfil | Esperado con REV2 | Esperado con HIST |
|---|---|---|---|
| **N19** | INCREASE_STRENGTH avanzado, casa con mancuernas y bandas, 1 día de 31 min | **Sin plan** (`FUERZA_SIN_TIEMPO`) | Plan con 1 sesión |
| **N20** | INCREASE_STRENGTH intermedio, gimnasio, 1 día de 32 min | Plan con 1 sesión | — |

La columna `esperado` del CSV recoge el resultado previsto de **cada** perfil antes de ejecutar: las sesiones programadas según S-MAX, o «sin plan (CÓDIGO)». La usa `pre_replicate/clasificar_resultados.py` para separar PASS, ESPERADO_SIN_PLAN y DEFECTO. **No** es un criterio del oráculo.

## Reglas de uso

1. Los resultados de N **no se usan para cambiar** el sistema, las políticas, el prompt, los oráculos ni los criterios. Se informan tal cual, fallos incluidos.
2. Si cualquier ejecución sobre N revela un defecto que obligue a cambiar el código, **N deja de ser no vista**: se declara así y la evaluación no vista requiere una cohorte nueva.
3. Sobre N, primero D y D′ con REV2 (gratuitos, puerta del protocolo); después R y R′ con IA real, **solo con tu autorización explícita**.
4. Oráculo vigente: `PLAN-REV-2.0+DIAS-1`. El histórico y el REV2 se informan como referencia.
5. **Replicate no está autorizado.**
