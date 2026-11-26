# 01 · Cierre de la PR #85 y diagnóstico de las regresiones de PR-2.0

## 1. Cierre de la PR #85

| Comprobación | Resultado |
|---|---|
| Informe revisado | `docs/FitSense/h2_v2/07_EVALUACION_REAL_Y_DECISION.md`: PR-2.0 **no cumplió P1** (B = 2 casos con V15 de la mitad o V26; se exigía como mucho 1) **ni K1** (V25 subió en 2 casos) |
| Resultados conservados | En la rama `feature/h2-v2-contrato-v15-v26` (cabeza `971596e1`), carpeta `docs/FitSense/h2_v2/evaluacion_real/`: <br>• `etapa1/primera_propuesta.csv`, `registro.csv` y `predict_time.csv`; <br>• `libro_presupuesto.csv`; <br>• `etapa1/salidas/`: 51 salidas reales y los problemas de cada brazo (G21 B se canceló sin salida). <br>Esta rama parte de esa cabeza y no los modifica |
| PR #85 | **Cerrada sin fusionar**, con un comentario que deja constancia del resultado y de la evidencia. La rama se conserva |
| `generation` | Sigue con **PR-1.7**: es la única versión que contiene (#85 nunca se fusionó y `generation` no tiene la propiedad `fitsense.ai.generacion.prompt-version`) |
| Criterios de la evaluación anterior | Sin cambios (`h2_v2/05` §4) |

## 2. Diagnóstico

**Método, sin coste:**
- se reconstruyeron las peticiones exactas de PR-1.7 y PR-2.0 de G02, G03, G15, G18 y G19 (modo `PROMPTS`);
- se compararon con las salidas reales ya pagadas, ejercicio por ejercicio (`evidencia/diagnostico_G02_G03_G15_G18_G19.txt`, generado con `evidencia/diagnostico_reparto.py`);
- se contrastó lo que dice el texto con lo que hace la política real (`DosisSemanalPolicy`, `ControlVolumen`, `PlanDraftValidator`).

### 2.1 Lo que hizo el modelo

| Caso | PR-1.7 (A) | PR-2.0 (B) | Rechazo de B |
|---|---|---|---|
| **G02** GM · principiante · casa · 4 días | 4 ejercicios por sesión con la dosis exacta de su clase (3×10/90, 2×10/90, 2×13/90). La sesión inferior lleva 3 de piernas (9 series; techo 6) | 4 ejercicios por sesión, pero **todas las estructurales a 2×10** en vez de 3×10, y rellenos a 2×12. Piernas: 8 series en la semana; `objetivo_series_semana` de piernas = 6 | V24 ×16, V25 |
| **G03** GM · intermedio · gimnasio · 4 días | 4 ejercicios por sesión, dosis exactas. **Válida** | **6 ejercicios** por sesión, con 3×8/90, 2×10/60 y 2×12/60. Pecho: exactamente 10 series en la semana = `objetivo_series_semana`. Ningún techo de V26 obligaba: el máximo por grupo y sesión fue 5, con techo 10 | V24 ×24, V25 |
| **G15** IS · intermedio · gimnasio · 4 días | Inferior con `waist` y `lower legs`, además de piernas | Inferior: **3 de 4 de upper legs** y 1 de `lower legs`; **sin `waist`** (`objetivo_series_semana`: `waist` = 0 y `lower legs` = 0) | V15 de la mitad ×2 |
| **G18** IS · principiante · gimnasio · 4 días | **Válida**: inferiores con `waist` y `lower legs` | La inferior del domingo deja `waist` y pone **3 de 4 de upper legs** | V15 de la mitad |
| **G19** GF · principiante · gimnasio · 3 días | **Válida**: 6 ejercicios por sesión | 5 por sesión, uno menos, con menos colocaciones de piernas: queda bajo la regla de «menos ejercicios es válido» y cerca de la orientación de 4 series semanales. La semana se queda **sin LOWER_HIP** | V23 |

### 2.2 Causas en el texto de PR-2.0

| # | Contradicción | Texto de PR-2.0 | Política real | Efecto |
|---|---|---|---|---|
| **C10** | La orientación por grupo, presentada como una cifra que alcanzar | Regla 21: «`objetivo_series_semana` es una orientación por grupo […] acércate a ella si cabe» | `ControlVolumen.grupoPendiente` solo la usa para **elegir a qué grupo dar una colocación más, siempre con su dosis completa**, mientras quepa en los techos. Nunca reduce una dosis. No se valida | El modelo **ajusta series y repeticiones** para clavar la cifra (G02 baja; G03 añade ejercicios y cambia dosis), y eso dispara V24 y V25 |
| **C11** | El 0 de las zonas pequeñas se lee como una prohibición | `objetivo_series_semana`: `waist` = 0 y `lower legs` = 0 (objetivos distintos de GAIN_MUSCLE) | El 0 significa **«sin prioridad»**. El motor de reglas sigue repartiendo entre esos grupos, y V15 los necesita en LOWER_BODY | El modelo los evita y concentra piernas, lo que dispara V15 (G15 y G18) |
| **C12** | El reintento invita a recortar la dosis | «V26: […] **baja series** o quita una colocación» y «Para V26 también puedes **bajar series** dentro de la dosis admitida». PR-1.7 ya decía «salvo que un motivo exija bajar […] **las series**» | Con `clase_dosis` (GAIN_MUSCLE) las series son fijas (V24); por capacidades, solo dentro de `series..series_max` | Un reintento tras V26 empuja al mismo error que la primera propuesta |
| **C13** | «Menos ejercicios es válido», sin la salvedad de la cobertura | Comprobación del reparto: «la sesión lleva menos ejercicios: es válido» | V23 exige los cuatro slots en la semana, y V25 las capacidades principales factibles | Al recortar, la semana pierde un slot (G19) |

Las cuatro son contradicciones de texto. **Ningún validador ni política está mal:** el backend rechaza exactamente lo que debe.

Las **mejoras** de PR-2.0 vienen de C1 y C2 (tabla de la mitad y techos de V26 comunicados) y se conservan en PR-2.1:
- V26 pasó de 7 rechazos a 0;
- V15 de la mitad, de 7 a 3.

### 2.3 Revisión del contrato completo (punto 5 de la orden)

| Regla | Qué dice PR-2.1 al modelo | ¿Choca con otra? |
|---|---|---|
| **V24** (dosis) | Regla 22.1: la dosis exacta de su clase (GAIN_MUSCLE: `series`, sin la serie adicional cuando no hay mínimo de duración) o de un código de capacidad (series entre `series` y `series_max`). Nunca se recorta. Coincide con el bloque de capacidades y con `CapabilityPrescriptionPolicy` | No. Es la primera de la jerarquía |
| **V26** (techos) | Regla 21: los mismos techos (`DosisSemanalPolicy`). Se cumplen con el número de colocaciones; por capacidades, también con menos series dentro del rango | No: ya no se pide bajar series fuera de la dosis |
| **V15** (reparto) | Regla 15 y 22.3: mínimo de grupos de `body_parts_con_material` y la mitad por `n` de trabajo; se reparte entre grupos con material, tengan o no prioridad, y respetando la regla 20 | No: un grupo sin prioridad ya no parece prohibido |
| **V23 / V25** (cobertura) | Regla 22.4: al quitar o cambiar, se conservan los slots exigibles y las capacidades principales (HYPERTROPHY en GAIN_MUSCLE) | No |
| **V20 / regla 4** (duración) | Regla 22.5 y regla 4 de PR-2.0: ningún exceso sobre `max_session_seconds`. Sin mínimo con `PLAN-REV-2.0`. Si no cabe, se quitan secundarias | No: no se acortan descansos ni dosis |
| **V8C** | Solo aplica a las correcciones de la adaptación, que no leen esta propiedad. La generación inicial no la toca | — |
| **Disponibilidad y equipamiento** | Sin cambios: fechas de `available_days`; solo ejercicios de `available_exercises`, que ya están filtrados por material | Las sustituciones de 22.3 y 22.4 son siempre dentro de `available_exercises` |
| **O20** | Sin cambios: lo excluido por dolor, las equivalencias confirmadas y las precauciones no llegan a `available_exercises` | No (probado con Replicate simulado, `03`) |
| **Reintentos** | Sin «bajar series»; pistas de V24, V25, V23, V15 y V26 que dicen cómo corregir sin romper otra regla; «las reglas 1 a 22» | No |
