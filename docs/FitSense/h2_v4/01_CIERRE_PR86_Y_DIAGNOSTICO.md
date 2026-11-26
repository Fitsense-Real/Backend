# 01 · Cierre de la PR #86 y diagnóstico (K2, G17, G21, G24)

## 1. Cierre de la PR #86

| Comprobación | Resultado |
|---|---|
| Informe revisado | `docs/FitSense/h2_v3/06_EVALUACION_REAL_Y_DECISION.md`: **RECHAZADA por K2**. Mediana de `predict_time`: 61,0 s frente a 59,8 s permitidos (54,4 × 1,10). Se mantiene tal cual, sin reinterpretarla |
| Lo que sí mejoró | Válidas a la primera: 23/26 frente a 18/26.<br>V15 de la mitad o V26: 1 frente a 4 casos.<br>V26: 0 frente a 5 rechazos.<br>V24/V25: 0 frente a 1 caso |
| Evidencia conservada | Rama `feature/h2-v3-pr21` (cabeza `0e109094`), carpeta `docs/FitSense/h2_v3/evaluacion_real/`:<br>• 52 salidas reales;<br>• `primera_propuesta.csv` y `predict_time.csv`;<br>• `revalidacion_sin_coste.csv`;<br>• `libro_presupuesto.csv`: USD 3,1327 de 7,60.<br>Esta rama parte de esa cabeza y no los modifica |
| PR #86 | **Cerrada sin fusionar**, con un comentario de constancia. La rama se conserva |
| `generation` | Sigue con **PR-1.7**, la única versión que contiene |

## 2. K2: qué explica la latencia de PR-2.1

Replicate no devuelve tokens de gpt-5-mini; la consulta de la predicción solo trae `predict_time` y `total_time`. Por eso la causa **no se puede demostrar**. Lo que sí dicen los datos ya medidos (`analisis_latencia.py` y `evidencia/analisis_latencia.txt`):

| Medida (26/11, PR-1.7 frente a PR-2.1) | Valor |
|---|---|
| Entrada B − A por pareja (mediana) | +4.974 caracteres (prompt e instrucción de sistema) |
| Pendiente de `predict_time` sobre el tamaño de la entrada (brazo A) | 1,72 s por cada 10.000 caracteres. Es una **cota alta**: el tamaño varía sobre todo entre perfiles (catálogo de casa o de gimnasio) y mezcla tamaño con dificultad |
| Lo que explicaría la entrada extra | **≈0,9 s** |
| `predict_time` B − A por pareja (mediana) | **+6,3 s** |
| Sin el efecto de la entrada | **+5,5 s** |
| Salida visible (mediana) | A 4.788 caracteres, B 4.385: B escribe **menos** |

**Lectura (hipótesis, no demostrada).**
- El tamaño de la entrada explica poco: menos de 1 s de los 6,3.
- B escribe menos y tarda más, así que lo más probable es **más razonamiento oculto**.
- Las causas candidatas son las comprobaciones que PR-2.1 pide hacer: cruzar dos listas de ids con el catálogo, consultar una tabla por sesión y repasar cinco puntos.
- Por eso PR-2.2 recorta caracteres y, sobre todo, **quita trabajo de cálculo**: lo que antes había que cruzar va marcado en cada ejercicio.

**El 25/11, PR-2.0 frente a PR-1.7,** la diferencia por pareja fue **+15,4 s** de mediana, y aun así K2 se cumplió (86,4 frente a 91,1 s permitidos): K2 compara la mediana de cada brazo. El 26/11 la diferencia pareada fue menor (+6,3 s) y K2 falló. El criterio es **ruidoso entre días**. Se trata en el protocolo (`04`), sin cambiarlo.

## 3. G17, G21 y G24 (los rechazos que quedaron con PR-2.1)

Evidencia ejercicio por ejercicio en `evidencia/diagnostico_G17_G21_G24.txt`.

| Caso | PR-1.7 (A) | PR-2.1 (B) | Rechazo de B |
|---|---|---|---|
| **G21** GF · intermedio · gimnasio · 4 días | Inferiores de 5 ejercicios: 2 de piernas de trabajo, cardio, movilidad de piernas y `waist` o `lower legs` | La inferior del 27/11 tiene **4**: 2 de piernas de trabajo, **cardio y movilidad de piernas, que son complementos**. Solo trabaja `upper legs` | V15: «debe cubrir al menos 2 grupos distintos de [upper legs, lower legs, waist]» |
| **G24** GF · principiante · gimnasio · 4 días | Inferiores de 5 ejercicios, con `lower legs` o `waist` | La inferior del 26/11 tiene 4: 2 de piernas, cardio y movilidad de piernas | V15, igual |
| **G17** IS · intermedio · gimnasio · 5 días | Concentraba piernas y espalda (V15 y V26 ×4) | PUSH: **3 de 5 en pecho** (máximo 2). LOWER_BODY: **3 de 4 en piernas** (máximo 2). V26 corregido (0) | V15 de la mitad ×2 |

**Causa.** No es una regla nueva ni un validador. Es el **recuento**, que PR-2.1 obligaba a hacer cruzando datos:

- **G21 y G24.** El modelo hace sesiones más cortas, como pide PR-2.0/2.1 («no llenes tiempo»), y el ejercicio que cae es el que daba la diversidad. Lo que queda son dos de trabajo del mismo grupo y dos complementos que **no cubren grupo**.
  - Que cardio y movilidad no cuentan solo se sabía cruzando `exercise_id` con `constraints.complementos_no_cuentan`, una lista de 38–49 ids.
  - Además, `waist` y `lower legs` no aparecen en `prioridad_series_semana` (en GENERAL_FITNESS valen 0), como en C11.
- **G17.** La mitad llegaba como una tabla por sesión (`{"4":2,"5":2,"6":3,…}`) que había que consultar con un `n` que cuenta solo los ejercicios de trabajo, sin los complementos de esa otra lista. En las dos sesiones el modelo puso un ejercicio de más del grupo principal.

**Lo que no es:**
- no es un problema de dosis: B no tiene ningún rechazo V24 ni V25;
- no es falta de material: las sesiones inferiores de G21 y G24 tienen `lower legs` y `waist` en `body_parts_con_material`;
- no es un error de V15: la regla es correcta y el backend la aplica igual en todas las versiones.

**Corrección en PR-2.2** (`02`): la marca `"cuenta"` en cada ejercicio, la mitad dicha como regla junto al mínimo de grupos, y «cardio y movilidad no cubren ningún grupo», sin ejemplos de un perfil concreto.
