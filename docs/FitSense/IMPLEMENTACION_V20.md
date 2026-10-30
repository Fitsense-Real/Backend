# V20 exacto y dos correcciones de composición (candidato de integración)

**Fecha:** 30/10/2026. **Rama:** `v20-duracion`. **No fusionado.**

**Orden aplicada:** «Decisión sobre V20 y composición» del responsable del proyecto. Continúa `PROTOTIPOS_V20.md`.

**Qué no se toca:**
- Neon, Railway y Replicate;
- las políticas de adaptación, progresión, capacidades y dosis;
- V15, V18, V23, V24 y V25 (las reglas del validador siguen iguales);
- las referencias oficiales.

**Qué se retira:** los prototipos A (resistencia muscular oportunista) y B. No queda ningún interruptor experimental.

## 1. Qué cambia

### 1.1 V20 en segundos (`PlanDraftValidator`, `SessionDurationEstimator`)

- **El piso** es `ceil(minutos × 60 × floorPct / 100)`, con aritmética entera: 1.890 s para 45 min y 3.150 s para 75.
- **La duración** es `SessionDurationEstimator.estimateSeconds`: calentamiento + trabajo + descansos + transiciones, **sin redondear**. Es el mismo estimador de antes, antes de su redondeo al minuto.
- **El mensaje** dice los segundos que faltan: «dura unos 1824 s y el mínimo es 1890 s (70 % de 45 minutos): le faltan 66 s».
- No cambia el 70 %, ni los descansos, ni V21 (descanso máximo), ni la exención V8C, ni la regla de que una orden de volumen anula el piso.

**Coherencia entre las tres piezas:**

| Pieza | Antes | Ahora |
|---|---|---|
| Validador | minutos redondeados frente al piso truncado (31) | segundos frente a 1.890 s |
| Constructor por capacidades (`CapabilityWeekBuilder.corto`) | minutos | segundos, mismo cálculo |
| Motor 4D de GAIN_MUSCLE (`RuleBasedTrainingPlanGenerator`, el relleno y la sustitución por estructural) | minutos | segundos, mismo cálculo |

El techo (V4/V17) sigue en minutos, como antes.

### 1.2 Primera corrección: V15 antes que añadir (`CapabilityWeekBuilder`, paso 2 de relleno)

Si el siguiente relleno dejaría a un grupo con más de la mitad de la sesión, se hace esto, en orden:

1. **Subir series** de lo que ya hay, con `ampliarSeries`, que ya existía. Solo dentro de la dosis vigente: `Dosis.conSeries` no pasa de `seriesMax` y el techo se comprueba. Si así se llega al piso, no se añade nada.
2. **Si no hay series que subir** (todas al máximo de su dosis), se elige **otro ejercicio del enfoque que no rompa V15**, con la misma capacidad que tenía el relleno rechazado. Pasa por el mismo selector, que mantiene V18, V22, los topes de grupos pequeños y el equipamiento. La dosis sale de `CapabilityPrescriptionPolicy`.
3. **Si tampoco hay ninguno**, se sigue como antes y V15 decide.

**Por qué hizo falta el paso 2** (causa exacta, GENERAL_FITNESS avanzado, 4 días, gimnasio, V37.1):
- El jueves inferior llevaba 554 (upper legs), 1015 (lower legs), 1141 (waist), cardio 1093 y **884, una movilidad de pierna por repeticiones** (`upper legs`, 2×10). V15 la cuenta como trabajo.
- Con 1.866 s faltaba tiempo. El relleno pedía fuerza para un patrón pendiente y elegía 1202 (`upper legs`): upper legs quedaba en **3 de 5** y V15 rechazaba la sesión.
- El selector no ve la movilidad, solo los resistidos, así que no detectaba que rompía la mitad.
- Las cinco colocaciones ya estaban en 3 series, el máximo de su dosis: **subir series no era posible**.

Con solo el paso 1, el perfil seguía sin plan en el barrido con V37.1 (`evidencia/v20/final/correccion1_sin_alternativa/`). El paso 2 elige otro grupo del enfoque y lo resuelve.

**⚠ Para tu revisión:** el paso 2 va un poco más allá de la literalidad de la orden («comprobar si puede ampliarse una serie»). Sigue siendo orden de composición: no relaja reglas, ni dosis, ni capacidades. Si no lo apruebas, se quita y GENERAL_FITNESS intermedio y avanzado, 4 días en gimnasio, vuelve a fallar con V37.1.

### 1.3 Segunda corrección: el último cardio del día siguiente (`CapabilityWeekBuilder.siguienteRelleno`)

**Causa exacta** (LOSE_WEIGHT principiante, 6 días, casa, V37.1). La hipótesis de `PROTOTIPOS_V20.md` §5 (la sustitución del respaldo) **era incorrecta**. Lo que pasa es esto:
- El miércoles inferior (1.836 s) no llegaba a 1.890 s. El relleno añadía **el tercer cardio** (1054) y la sesión quedaba con 253, 1093 y 1054.
- El jueves va pegado. V18 le bloqueaba los tres cardios de casa nivel 1, así que se rellenaba con 8 resistidos y **ningún cardio**. V25 lo rechazaba.

**Corrección.** En LOSE_WEIGHT, cuando la sesión siguiente va pegada, el relleno de cardio solo puede elegir un cardio si deja al menos otro libre para mañana: uno que no esté hoy y que admita el enfoque de mañana. Si no hay ninguno, no se añade cardio, y el día corto sube series o elige fuerza estructural.

No se inventa un segundo cardio ni se permite repetir en días consecutivos. V18 y la dosis de cardio no cambian. Las principales (los 2 bloques de cardio) se eligen como antes.

## 2. Resultados

`docs/FitSense/evidencia/v20/final/comparativa_final.md` se genera con `comparar_final.py`.

**Las tres variantes:**
- **antes**: redondeo, el código de `generation`;
- **exacto sin correcciones**: el prototipo de `PROTOTIPOS_V20.md`;
- **final**: este cambio.

### 2.1 V37 (catálogo oficial)

| Variante | Refs válidas | Refs que cambian | Sesiones ref < 70 % | Barrido con plan | Sesiones barrido < 70 % | Robustez | 192 restricciones | Continuidad 96 |
|---|---|---|---|---|---|---|---|---|
| antes | 72/72 | — | 20 | 72/72 | 64 | 288/288 | 192 publicados, 0 violaciones | 96/96 |
| exacto sin correcciones | 72/72 | 13 | 0 | 72/72 | 0 | 288/288 | 192, 0 | 96/96 |
| **final** | **72/72** | **13** | **0** | **72/72** | **0** | **288/288** | **192, 0** | **96/96** |

- **Planes válidos:** todos.
- **Sesiones rechazadas:** ninguna.
- **Perfiles sin plan:** ninguno.
- **Las dos correcciones** no cambian nada sobre V37: las 72 referencias son idénticas a las del exacto sin correcciones.

### 2.2 V37 + V37.1 (separada, solo en pruebas)

| Variante | Barrido con plan | Perfiles sin plan (barrido) | Robustez | Sesiones rechazadas en robustez | Continuidad 96 |
|---|---|---|---|---|---|
| antes | 70/72 | LW/BEG/4d casa, LW/BEG/5d casa | 280/288 | V20: 16 | se detenía en el primero (LW/BEG/4d casa) |
| exacto sin correcciones | 68/72 | los dos anteriores, LW/BEG/6d casa (V25) y GF/INT/4d gimnasio (V15) | 275/288 | V20: 16, V25: 4, V15: 1 | — |
| solo la corrección 1 (sin alternativa) + corrección 2 | 69/72 | LW/BEG/4d, LW/BEG/5d y GF/INT/4d gimnasio (V15) | 279/288 | V20: 16, V15: 1 | — |
| **final** | **70/72** | **LW/BEG/4d casa, LW/BEG/5d casa** | **280/288** | **V20: 16** | **94/96** (solo fallan los dos anteriores) |

En las 72 referencias: 72/72 válidas y 0 sesiones bajo el 70 %. En los 192 escenarios con restricciones: 192 publicados y 0 violaciones en todas las variantes.

**Resultado con V37.1:** el final no crea ningún perfil sin plan nuevo frente a «antes». Los dos que quedan son los conocidos de §1 de `PROTOTIPOS_V20.md`. Su lunes y miércoles superiores llegan (2.052 s). El martes y jueves inferiores se quedan en 1.824 s, porque solo hay 3 cardios de casa nivel 1 y V18. No se tapan.

### 2.3 Perfiles reproducidos expresamente (`V20ComposicionCasosIT`)

Con el generador y el validador reales, sin IA, 8 semillas por perfil:

| Perfil | V37 | V37 + V37.1 |
|---|---|---|
| GENERAL_FITNESS intermedio, 4d gimnasio | plan válido; todas las sesiones ≥ 1.890 s; sin repeticiones en días pegados | igual |
| GENERAL_FITNESS avanzado, 4d gimnasio | igual | igual (fallaba la semilla 3 sin el paso 2) |
| GENERAL_FITNESS principiante, 6d casa | igual | igual |
| LOSE_WEIGHT principiante, 6d casa | igual, y cardio en todas las sesiones | igual (antes, V25 el jueves) |
| LOSE_WEIGHT principiante, 4d y 5d casa | plan válido | **sin plan, solo por V20** (1.824 s frente a 1.890 s). La prueba comprueba que no aparece ninguna otra regla |

### 2.4 Qué cambia en las 72 referencias (V37)

Cambian 13, todas en sesiones de 45 o 60 minutos que pasaban solo por redondeo. Todas siguen válidas.

| Objetivo | Perfiles | Qué pasa |
|---|---|---|
| LOSE_WEIGHT | principiante 45 gimnasio y casa | Cada sesión pasa de 31 a 34 min con un estructural más (2×8). El cardio sigue (2 bloques). |
| GAIN_MUSCLE | principiante 45 gimnasio; intermedio 45 gimnasio; avanzado 45 gimnasio y casa | Un relleno de 2×13 se cambia por un estructural de 3×13 (1102, 1103 o 927), y la sesión pasa de 31 a 33 min. Es la sustitución por estructural que ya existía, ahora con el piso en segundos. En dos casos la sesión siguiente cambia por arrastre. |
| GENERAL_FITNESS | principiante 45 gimnasio y casa; principiante 60 gimnasio; intermedio y avanzado 60 gimnasio y casa | Se añade un ejercicio o se sube una serie (por ejemplo 992, de 2 a 3 series). En 60 min, el lunes pasa de 42 a 49–51 min con un estructural más de 3×6 y 150 s de descanso, dentro del techo de 60. Las sesiones siguientes cambian por arrastre (la semana usa otros ejercicios). |
| INCREASE_STRENGTH | — | Ninguna referencia cambia; en el barrido de 4 a 6 días sí cambian algunas sesiones, todas válidas. |

**Distribución.** Media de ejercicios por sesión en el barrido y las referencias con V37:
- LOSE_WEIGHT: de 4,35 a 4,46;
- GENERAL_FITNESS: de 5,78 a 6,18;
- INCREASE_STRENGTH: de 5,35 a 5,43;
- GAIN_MUSCLE: 5,75, igual.

La duración máxima no cambia en ningún objetivo.

**Catálogo.** El cambio no se apoya en ejercicios con errores de catálogo. Apariciones en referencias y barrido con V37, antes frente a ahora:
- 1106: 22/8 frente a 23/7;
- 1253: 0/10 frente a 0/11;
- 142: 73/66 frente a 73/66.

Con el prototipo A, 1106 subía en 20–31 apariciones.

## 3. Pruebas

| Prueba | Qué comprueba | Resultado |
|---|---|---|
| `V20PisoExactoTest` (sustituye a `V20UmbralRedondeoTest`, que fijaba el defecto) | Pisos de 30, 45, 60 y 75 min (1.260, 1.890, 2.520 y 3.150 s). Con el validador real, 45 min: 1.830, 1.836 y 1.889 s se rechazan; 1.890 y 1.891 s se aceptan. 1.830 s se redondea a 31 min y aun así se rechaza. Lo mismo en el límite de 30, 60 y 75 min. | 16/16 |
| Pruebas unitarias completas (`mvn test`) | Toda la lógica de dominio | 733/733 |
| `V20ComposicionCasosIT` (nueva, en CI con V37) | §2.3 | V37: 2/2; V37 + V37.1: 2/2 |
| `ContinuidadSemanalIT` | 96 perfiles y LOSE_WEIGHT 5–6 días. Ahora anota todos los perfiles sin plan en vez de pararse en el primero; lo que pasa y lo que falla es igual que antes. | V37: pasa; V37 + V37.1: 94/96 |
| Barrido de 72, robustez de 288, 192 restricciones, 72 referencias candidatas | §2 | §2 |
| Suite de integración completa con V37 | V15, V18, V23, V24, V25, adaptación, progresión, copia, seguridad | §3.1 |

### 3.1 Suite de integración completa (V37)

`mvn verify -Pintegration`: 845 unitarias sin fallos y 285 de integración (24 omitidas por opt-in). Hubo 6 fallos en la primera pasada.

**3 corregidos (cambio de premisa justificado):** `Usuario31ReproduccionIT` (2) y `Usuario31V37IT` (1).
- Reproducen las propuestas reales de la IA del incidente del usuario 31.
- En los intentos 2 y 3, el sábado dura **1.866 s (69,1 %)**. Antes pasaba solo por el redondeo a 31 min; ahora V20 lo rechaza además de V15.
- La prueba exigía exactamente un problema por intento. Ahora exige los dos: el mismo V15 y el nuevo V20 con sus segundos.
- El rechazo de la propuesta no cambia, y el plan del motor sigue publicándose. Tras el ajuste, 3/3 pasan.

**3 esperados, pendientes de tu aprobación:** las referencias protegidas.
- `GeneracionReferenciaIT` (oficial, V37): 13 perfiles cambian (§2.4).
- `GeneracionReferenciaV36IT` (catálogo V36): 11 perfiles cambian (LW 2, GM 2, GF 7).
- `GeneracionReferenciaV35IT` (catálogo V35): 11 perfiles cambian (LW 2, GM 2, GF 7).

En los tres casos todos los perfiles siguen válidos. Las tres protegen la generación «tal cual» y fallan con cualquier cambio del motor. No se han regenerado. Candidatas:
- `evidencia/v20/final/fin_v37/generacion_referencia.txt`;
- `evidencia/v20/final/generacion_referencia_v36_candidata.txt`;
- `evidencia/v20/final/generacion_referencia_v35_candidata.txt`.

**El resto de la suite pasa:**
- V15, V18, V22, V23, V24 y V25;
- adaptación, progresión y reserva, copia semanal, D8 y V8C;
- seguridad, restricciones, 4–6 días y respaldo.

## 4. Riesgos y pendientes

1. **Referencias.** Las tres pruebas de referencia (oficial V37 y las históricas V36 y V35) fallarán hasta que decidas (§3.1). Para las históricas hay dos opciones:
   - regenerarlas con el motor nuevo, sobre el mismo catálogo;
   - o fijarlas al código anterior, como evidencia, dejando de ejecutarlas contra el motor actual.

   Recomiendo la primera: siguen protegiendo que el catálogo V35 y V36 produce planes válidos.
2. **La IA.** El dato que recibe la IA (`PlanInputSnapshot.min_session_minutes`) sigue siendo el piso entero, 31 min, porque no se tocan los prompts. Una propuesta de la IA de 31 min justos (1.860 s) ahora se rechaza y va al reintento o al respaldo. Con la IA apagada en las pruebas no tiene efecto. Conviene decidir si se le pasa también el mínimo en segundos.
3. **GENERAL_FITNESS de 60 min.** Algunas sesiones pasan de 42 a 49–51 min, porque la única forma de completar es un estructural con 150 s de descanso. Están dentro del techo. Es el coste de exigir el 70 % real.
4. **V37.1 sigue separada.** Con ella, LOSE_WEIGHT principiante en casa, 4 y 5 días, sigue sin plan. La solución es de catálogo, no de motor.
5. **Pendientes de V37.1, sin tocar:**
   - 1106: grupo, tipo de ejecución y prescripción por duración;
   - 1253: escalón real;
   - 142: de espalda a tren inferior;
   - revisión del cardio de principiante en casa.

   Cada cambio necesita evidencia de ejecución y comprobar V15, V18, V23 y V25.
6. **Revisiones humanas independientes:** siguen pendientes.

## 5. Para integrar (requiere tu aprobación)

1. Revisar §1.2, paso 2.
2. Aprobar las tres referencias candidatas (V37 oficial, V36 y V35). Después, regenerarlas con su propiedad de captura y comprobar que coinciden con las candidatas.
3. Fusionar `v20-duracion` a `generation`. El CI completo se ejecutará en el PR.
4. V37.1 queda fuera hasta resolver el cardio de casa.
