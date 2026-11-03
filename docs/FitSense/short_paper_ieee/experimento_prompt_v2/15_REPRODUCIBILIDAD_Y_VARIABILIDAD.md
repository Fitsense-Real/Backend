# Reproducibilidad y variabilidad

## 1. Lo que es idéntico entre corridas (determinista)

| Elemento | Comprobación |
|---|---|
| Código | Las 4 corridas, con el *commit* del protocolo congelado o posterior, sin cambios en `src/` (`00_configuracion.txt` de cada corrida) |
| Plan inicial, métricas y decisiones de U01–U20 | Iguales en las 4 corridas, en GUION y en la etapa C original. Solo se consulta a la IA en la adaptación |
| Prompt del intento 1 | **Idéntico** dentro de cada condición. C1 es además idéntico byte a byte a la etapa C (`06` §3). Los *tokens* de entrada de C2 r1 y C2 r2 son exactamente los mismos (291.118) |
| Controles (12 no elegibles) | 0 llamadas y el mismo resultado en las 4 corridas |

## 2. Lo que varía (el modelo)

| Caso | C1 r1 | C1 r2 | C2 r1 | C2 r2 |
|---|---|---|---|---|
| U03 | SIN_EFECTO (omite) | SIN_EFECTO (omite) | SIN_EFECTO (omite) | SIN_EFECTO (omite) |
| U04 | APLICADA (S2) | APLICADA (S2) | APLICADA (S2) | APLICADA (S2) |
| U08 | APLICADA, 2 intentos | APLICADA, 1 | APLICADA, 1 | APLICADA, 1 |
| U09 | APLICADA (**solo S2**), 3 | APLICADA (**solo S3**), 3 | APLICADA (S2 y S3), 1 | APLICADA (S2 y S3), 1 |
| U13 | APLICADA, 1 | APLICADA, 1 | APLICADA, 1 | APLICADA, 1 |
| U14 | APLICADA (**solo S2**), 3 | APLICADA (**solo S2**), 3 | APLICADA (S2 y S3), 1 | APLICADA (S2 y S3), 1 |
| U18 | **SIN_EFECTO**, 2 | **SIN_EFECTO**, 2 | APLICADA, 1 | APLICADA, 1 |
| U19 | APLICADA (S2 y S3), 2 | **RESPALDO**, 3 rechazos | APLICADA (S2 y S3), 1 | APLICADA (S2 y S3), 1 |
| **Aplicadas / 8** | **6** | **5** | **7** | **7** |
| Sesiones con cambio efectivo / 10 factibles | 7 | 5 | **10** | **10** |

### Variabilidad dentro de cada condición

**C1 varía entre corridas y respecto a la etapa C** (4 de 8 aplicadas, intento 3, en PR #28):

| | Etapa C | C1 r1 | C1 r2 |
|---|---:|---:|---:|
| Aplicadas | 4 | 6 | 5 |

- U19 pasó de APLICADA a RESPALDO.
- U09 cambió de sesión aplicada.
- Las 3 mediciones de la misma condición van de **4 a 6**. **Una sola corrida con C1 no es fiable para estimar la tasa.**

**C2 da el mismo resultado de casos en las 2 corridas:** 7 de 8 y 10 de 10 sesiones factibles. Los cambios concretos no son idénticos:
- U13: una serie menos de cardio frente a −10 s en dos cardios;
- U19: una serie menos de cardio frente a reducciones repartidas en 8 ejercicios;
- U04, U09 y U14: sustitutos distintos.

**El resultado agregado es estable, pero la respuesta no es determinista.**

## 3. Límites

- **Muestra pequeña:** 8 casos × 2 corridas por condición. Las diferencias son descriptivas; no hay contraste estadístico.
- **Techo:** C2 alcanzó el máximo teórico (7 de 8 y 10 de 10) en las dos corridas. **No se puede saber si con más corridas bajaría.**
- **Efecto combinado:** C2 lleva cálculo previo y texto nuevo a la vez (`05`). No se separa la contribución de cada parte.
- **Un solo modelo** (`gpt-5-mini`), una configuración y una semana del ciclo. No se generaliza a otros modelos ni a ciclos largos.
- **Sin repeticiones selectivas:** las 4 corridas se ejecutaron en el orden congelado (C1, C2, C2, C1), una vez cada una, y todas se conservan.
