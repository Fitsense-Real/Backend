# Criterios de adaptación correcta, métricas y aceptación

**Estado: PROPUESTA PARA REVISIÓN.** Este documento se congela con el `01` antes de ejecutar.

Todo lo que aquí se mide lo recalcula un **oráculo independiente**:
- es un script en Python;
- lee las filas de la base de datos exportadas tras cada corrida;
- **no reutiliza** funciones ni validadores del backend.

Si el oráculo y el backend discrepan, gana el oráculo. La discrepancia se informa como hallazgo.

## 1. Unidades y notación

- **Semana medida (W):** la semana con registros. **Semana adaptada (W+1):** la que se publica después.
- **Sesión señalada:** la que, según la política, debe adaptarse. El oráculo la deduce de forma independiente a partir de los registros:

  | Registro de la sesión | Se señala por |
  |---|---|
  | omitida con motivo | el motivo |
  | parcial (< 100 %) | baja ejecución |
  | algún ejercicio con `PAIN_OR_DISCOMFORT` | dolor identificado |

- **Sesión no señalada:** todas las demás.
- **Dificultad de un ejercicio:** `exercises.difficulty_level` (1–3), leída del catálogo congelado.
- **Dosis de un ejercicio:**
  - `planned_sets`;
  - `planned_reps`;
  - `planned_duration_seconds`;
  - `target_load_kg`;
  - `rest_seconds` (más descanso no se cuenta como aumento).
- **Trabajo de una sesión:** el trabajo de cada ejercicio, sumado. El de un ejercicio es:
  - `planned_sets × planned_reps`;
  - o, si es por tiempo, `planned_sets × planned_duration_seconds`.

  La duración de la sesión es `expected_duration_minutes`.
- **Origen de la sesión adaptada:** se lee de `plan_adaptation_attempts` (V32: modo, `applied_sessions`, `correcciones_del_backend`) y del linaje (`source_planned_workout_id`).

  | Origen | Cuándo |
  |---|---|
  | `IA` | modo IA, con la sesión en `applied_sessions` |
  | `RESPALDO-SUST` | respaldo con sustitución SUST-SEG-1 |
  | `RESPALDO-COPIA` | respaldo que copia la sesión sin cambios |
  | `RETIRADA` | nota `RETIRADA_SEGURA` (solo VC-B) |
  | `REGEN-P03` | plan regenerado por P-03, sin linaje |
  | `SIN-PLAN` | la semana no se publica |
  | `NINGUNO` | la decisión era mantener |

## 2. Criterios duros de seguridad (S)

Se miden en **todas** las sesiones adaptadas de todas las condiciones. Basta un incumplimiento para que la corrida falle (A1).

| Id | Criterio | Cómo lo comprueba el oráculo |
|---|---|---|
| **S1** | Ningún ejercicio con dolor registrado reaparece mientras su exclusión siga activa | Para cada `(usuario, ejercicio)` con `PAIN_OR_DISCOMFORT`: el ejercicio no está en W+1. Ante dolor de sesión: ninguno de los ejercicios de esa sesión aparece en la sesión que la sustituye |
| **S2** | Ninguna dosis aumenta en una sesión adaptada por dolor, fatiga, dificultad o falta de tiempo | Para cada ejercicio conservado:<br>• `sets`, `reps`, `duration` y `load` en W+1 ≤ W;<br>• el trabajo total de la sesión en W+1 ≤ W.<br>Lo añadido cuenta como aumento, salvo un sustituto que no supere el trabajo del ejercicio que reemplaza |
| **S3** | Las sesiones no señaladas no cambian | Mismos ejercicios, mismo orden, misma dosis y misma fecha. Se exceptúa el desplazamiento de fecha que exige un cambio de horario, solo si la política lo prevé |
| **S4** | Todo lo publicado es válido | El oráculo recalcula de forma independiente:<br>• equipamiento disponible;<br>• duración ≥ 70 % y ≤ 100 % del perfil (V20);<br>• días dentro de la disponibilidad;<br>• ejercicios activos y del catálogo;<br>• sin repeticiones en una misma sesión |
| **S5** | Ningún sustituto es más difícil que el ejercicio retirado | `difficulty_level(sustituto) ≤ difficulty_level(original)` ante dolor y ante `TOO_DIFFICULT`.<br>**Se mide siempre.** Con VC-A se **informa** (limitación conocida, N08). Con VC-B es **duro** |

## 3. Pertinencia por causa (P)

Se aplica a cada **sesión señalada**. Comprueba que el cambio responde a la causa, además de ser seguro.

| Causa (registro) | Adaptación correcta (ACC) | Alternativa aceptable (ALT) | Incorrecta (INC) |
|---|---|---|---|
| `TOO_DIFFICULT` | Baja la dificultad registrada de al menos un ejercicio, o baja la dosis | Cambio lateral: misma dificultad, otro estímulo y dosis no mayor | Más difícil, o más trabajo |
| `FATIGUE` | Baja el trabajo o la carga sin añadir ejercicios | Cambio de ejercicio con un trabajo no mayor | Más trabajo |
| `LACK_OF_MOTIVATION` | Cambio mínimo: ≤ 1 ejercicio, mismo patrón y dosis no mayor | Conservar la sesión | > 2 ejercicios cambiados, o la dosis sube |
| `LACK_OF_TIME` | Baja la duración prevista, mantiene los días y conserva las colocaciones estructurales | Conservar la sesión, si ya es la mínima válida (V20) | Más duración, o se pierde un patrón estructural sin cobertura semanal |
| `SCHEDULE_CHANGE` | Solo cambia el día, dentro de la disponibilidad | Conservar la sesión | Cambia el contenido |
| Dolor identificado | El ejercicio no está y se cumple una de estas:<br>• el sustituto no es más difícil y no hay más trabajo;<br>• retirada válida;<br>• regeneración P-03 | — | El ejercicio sigue, o S1/S2/S5 fallan |
| Dolor de sesión | Ningún ejercicio de la sesión reaparece; la sesión sustituta es válida y no tiene más trabajo | Regeneración P-03 | Reaparece alguno |
| Baja ejecución (P40) | Baja la dosis, o se conserva según la política | — | La dosis sube |

**Categorías finales de cada sesión señalada:**

| Categoría | Significado |
|---|---|
| **ACC** | Adaptación correcta |
| **ALT** | Alternativa aceptable |
| **CONS-J** | Conservación justificada: la sesión no cambió, la semana quedó válida y el motivo está registrado. Por ejemplo, un respaldo tras rechazar la IA, o una decisión de mantener |
| **NF** | No factible: no se publica la semana, o lo exige la política, con motivo registrado |
| **INC** | Incorrecta: cumple una condición de la columna INC, o cambió sin encajar en ACC ni en ALT |

**Regla de cierre.** Una sesión sin cambios nunca es INC si su motivo está registrado; es CONS-J. Así, un respaldo que copia la sesión no se confunde con un error. Sí cuenta en O1 como `RESPALDO-COPIA`, es decir, como **adaptación no realizada**.

La tabla es normativa. Antes de congelarla, la contrasto con la política documentada del backend. Cualquier diferencia entre ambas se te presenta para decidir; no se ajusta después de ver resultados.

**Una INC es un hallazgo y nunca se corrige después.**
- **Con IA:** cuenta en las métricas de la IA, aunque el validador la haya rechazado. Ahí cuenta como propuesta rechazada, no como publicada.
- **En lo publicado:** solo hay INC si falló algún S, lo que ya hace fallar la corrida.

## 4. Métricas

Las métricas de la IA y las del respaldo **se informan por separado**. Una adaptación solo se atribuye a la IA si la sesión publicada viene de su propuesta (`origen = IA`).

### 4.1 Generación inicial

| Métrica | Definición |
|---|---|
| G1 | Planes válidos / perfiles, con S4 aplicado al plan inicial |
| G2 | Fallos de generación por validador (V20, V24…), con el perfil. R0548 se cuenta si aparece; no se excluye |
| G3 (solo con los gemelos de IA) | Planes de la IA aceptados / intentos; rechazos por validador |

### 4.2 Métricas y decisión

| Métrica | Definición |
|---|---|
| M1 | Error absoluto máximo de adherencia (Aw, Aobs, C), backend frente al oráculo, en puntos porcentuales |
| M2 | Coincidencia de la decisión semanal: backend frente a la política del oráculo |
| M3 | Coincidencia de la decisión con la regla del artículo; se informa aparte, por D1 |
| M4 | Coincidencia de las sesiones señaladas: backend frente al oráculo |

### 4.3 Adaptación por origen

| Métrica | Definición |
|---|---|
| O1 | Distribución de las sesiones señaladas por origen (IA, RESPALDO-SUST, RESPALDO-COPIA, RETIRADA, REGEN-P03, SIN-PLAN) |
| O2 | Por origen: ACC, ALT, CONS-J, NF, INC |
| O3 | Semanas sin plan, y su causa |

### 4.4 IA (solo en R y R′)

| Métrica | Definición |
|---|---|
| IA1 | Operaciones elegibles, propuestas recibidas y propuestas aplicadas |
| IA2 | Rechazos por validador y por intento; motivo literal |
| IA3 | Propuestas sin efecto (SIN_EFECTO) |
| IA4 | Pertinencia de las propuestas, **también de las rechazadas**: ACC, ALT, INC según la tabla del §3 |
| IA5 | Latencia por predicción (mediana, p90 y máximo) |
| IA6 | Coste: predicciones facturadas × precio, y coste por adaptación aplicada |
| IA7 | Con R′: coincidencia entre corridas, de origen y de categoría, por sesión |

### 4.5 Comunicación

| Métrica | Definición |
|---|---|
| T1 | El texto publicado describe el origen y la acción reales: no dice «no aplicamos cambios» si hubo una sustitución o una retirada, ni menciona la IA si el origen es el respaldo |

### 4.6 Reproducibilidad

| Métrica | Definición |
|---|---|
| X1 | D frente a D′: hash SHA-256 de las exportaciones normalizadas, sin identificadores sustitutos ni marcas de tiempo |
| X2 | Control de 960 perfiles y REPLAY: los hashes de referencia (`planes` `d109a3da…`, `colocaciones` `d227848b…`) o las diferencias explicadas |

### Intervalos

Las proporciones se dan con su **intervalo de Wilson al 95 %** y con su n. Con estos tamaños, las cifras son **descriptivas** y no se usan para contrastar hipótesis.

## 5. Criterios de aceptación

Si falla un criterio duro (A1–A6), se paran el análisis y la corrida (`01` §5).

| Id | Criterio | Umbral | Tipo |
|---|---|---|---|
| **A1** | Seguridad: S1–S4 (y S5 con VC-B) en todo lo publicado, en todas las condiciones | **0 incumplimientos** | Duro |
| **A2** | Ninguna rutina publicada inválida (S4) | **0** | Duro |
| **A3** | Exactitud de la adherencia (M1) | **≤ 0,01 pp** | Duro |
| **A4** | Decisión y señalamiento iguales a los de la política (M2, M4) | **100 %** | Duro |
| **A5** | Reproducibilidad determinista (X1, X2) | **Hashes idénticos**, o solo las diferencias explicadas de antemano | Duro |
| **A6** | Ninguna sesión publicada INC por origen determinista (O2) | **0** | Duro |
| **A7** | Coherencia del texto (T1) | **100 %** con VC-B. Con VC-A, se informa: la limitación P3 es conocida | Duro con VC-B; si no, informativo |
| **A8** | IA (IA1–IA7) | **Sin umbral**: se informa con IC y n | Informativo |
| **A9** | Protocolo:<br>• no se supera el presupuesto;<br>• no hay repeticiones selectivas;<br>• las carpetas son nuevas;<br>• no hay cambios tras ver resultados | **Cumplido**; cualquier desviación se documenta | Duro |

**No hay umbral para la IA, a propósito.** La seguridad la garantizan los validadores y el respaldo. Por eso el artículo debe describir la IA por lo que se mida (tasa aplicada, rechazos, coste) y no por un objetivo fijado de antemano.

Si quieres un umbral de éxito para la IA (por ejemplo, ≥ 50 % de propuestas aplicadas), debe fijarse **ahora**, antes de ejecutar.
