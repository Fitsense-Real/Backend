# V20 · Prototipos de composición (A y B) y regla del 70 % exacto

**Fecha:** 30/10/2026. **Rama:** `v20-duracion`, solo experimentación en bases de prueba (Testcontainers).

**Qué no cambia este informe:**
- no modifica producción por defecto;
- no toca V15, V18, V23 ni V24, ni las políticas de GAIN_MUSCLE, INCREASE_STRENGTH o GENERAL_FITNESS;
- no usa prompts ni Replicate; no aplica V37.1 en Neon; no despliega en Railway;
- no fusiona a `generation`; no actualiza referencias oficiales.

Continúa `DIAGNOSTICO_V20.md`.

## 0. Cómo se reproduce

Los dos prototipos están detrás de propiedades del sistema **apagadas por defecto**. Se pueden activar por separado o juntos.

| Propiedad | Prototipo | Qué cambia |
|---|---|---|
| `fitsense.experimento.v20a=true` | A | `GoalCapabilityPolicy.forGoal(LOSE_WEIGHT)` devuelve una copia con `MUSCULAR_ENDURANCE` secundaria `OPPORTUNISTIC`. `CapabilityWeekBuilder.siguienteRelleno` puede añadir **un** resistido complementario de los grupos del enfoque, con la dosis ME vigente, antes de rendirse. No se toca V24 ni la política global. |
| `fitsense.experimento.v20exacto=true` | 70 % exacto | El validador compara `SessionDurationEstimator.estimateSeconds` con `ceil(minutos × 60 × 70 / 100)` (1.890 s para 45 min). El constructor y el respaldo usan el mismo criterio para decidir si la sesión es corta. |

**Guion:** `evidencia/v20/prototipos/simv20.sh`. Ejecuta, por variante, `GeneracionReferenciaCandidataIT` (72 referencias), `EvaluacionFrecuenciaIT` (barrido de 72 perfiles de 4 a 6 días y 288 generaciones de robustez), `ImpactoRestriccionesIT` (192 escenarios), `ContinuidadSemanalIT` (96 perfiles) y `DiagnosticoV20ComposicionIT#diagnostico`.

**Catálogos:**
- «v37»: Flyway oficial.
- «v37b»: V37 + V37.1 (`filesystem:docs/FitSense/auditoria_catalogo/v37/separables`).

**Comparativa:** `evidencia/v20/prototipos/comparar.py` genera `comparativa.txt`.

**Aislamiento comprobado:** `base_v37` y `base_v37b`, con los interruptores apagados, son **idénticas byte a byte** a las simulaciones anteriores (referencias, barrido, robustez y 192 perfiles). La referencia de `base_v37` es igual a la oficial `evidencia/progresion/generacion_referencia.txt`. Cada variante arranca su propio contenedor.

## 1. Traza definitiva del cardio frente a V18

`evidencia/v20/prototipos/traza_cardio_v37b.txt` cubre LOSE_WEIGHT principiante en casa, 45 min, con V37.1.

**Cardio elegible y prescribible.** Solo hay **3**, los tres DURATION con la dosis vigente de principiante (3×90 s, descanso 60 s):
- 253 «Gateo de oso»;
- 1054 «Escalador»;
- 1093 «Empujar para correr».

Ningún otro cardio pasa los filtros del perfil (casa, nivel 1 y restricciones).

**Qué bloquea V18.** El constructor pone 2 cardios por día. En días consecutivos:

| Día | Cardio usado | Bloqueado por V18 (del día anterior) | Cardio que aún vale |
|---|---|---|---|
| Lunes, superior | 2 (p. ej. 1054, 1093) | — | 1 (253) |
| Martes, inferior | 1 (253) | 1054, 1093 | **0** |
| Miércoles, superior | 2 (1054, 1093) | 253 | **0** |
| Jueves, inferior | 1 (253) | 1054, 1093 | **0** |

Dos días seguidos solo pueden repartirse 3 cardios distintos: si uno lleva 2, el siguiente lleva como mucho 1. El día corto (inferior) se queda en 4 colocaciones y **1.824 s (67,6 %)**.

**¿Otra distribución semanal lo evita?** No, sin romper reglas:
- Pasar a 1 cardio el día superior le quita entre 222 y 450 s. Por ejemplo, 2.052 s pasa a 1.830 s si el cardio se cambia por un estructural. Ese día baja entonces del 70 %.
- Con 1,5 cardios por día de media, los dos días quedan por debajo.
- Repetir cardio en días consecutivos lo prohíbe V18, que no se toca.
- En 6 días (con días no consecutivos) sí hay cardio libre. Ese perfil falla por otra causa (§5).

**Cuántos ejercicios hay realmente (117 frente a 97).** En el día inferior quedan 117 ejercicios de trabajo libres (casa, nivel ≤ 1, sin restricciones del perfil). Para LOSE_WEIGHT, **solo 30 son prescribibles**: estructurales de `upper legs`, como STRENGTH. Los otros 87 son elegibles por catálogo, pero ninguna capacidad de LOSE_WEIGHT les asigna dosis:
- RESISTIDO, 73: cintura SETS_REPS 50, `upper legs` SETS_REPS 12, `lower legs` 5, `upper legs` DURATION 2 y cintura DURATION 4;
- EN_REVISION, 9; SIN_CAPACIDAD, 3; MOVILIDAD, 1;
- un estructural de cintura en DURATION, 1.

Los 97 de la cifra anterior eran 50 + 30 + 12 + 5: solo los de SETS_REPS de esas clases. Los 20 que faltaban son los 9 EN_REVISION, 3 SIN_CAPACIDAD, 1 MOVILIDAD, 1 estructural de cintura en DURATION y 6 RESISTIDO en DURATION.

## 2. Prototipo B (complemento de cardio): no viable, no se implementa

B solo podría añadir cardio elegible con su dosis vigente. La traza demuestra que en los perfiles que fallan **no queda ningún cardio válido** en el día corto. Los 3 prescribibles están usados el mismo día o bloqueados por V18 desde el anterior.

Para que B funcione habría que hacer al menos una de estas cosas, y todas están fuera de lo autorizado:
- relajar V18;
- subir series o duración por encima de la dosis vigente;
- ampliar el catálogo de cardio en casa.

Por la orden recibida, **B no se fuerza**: no hay código de B. Queda como causa documentada: **escasez de cardio de casa nivel 1 (3 ejercicios)**. La solución de fondo es de catálogo (más cardio de casa revisado) y no de motor.

## 3. Prototipo A (resistido complementario)

**Qué añade.** Cuando el día sigue corto tras estructural y cardio, A añade un resistido de los grupos del enfoque con la dosis de resistencia muscular vigente para principiante:
- 2×15, descanso 45 s;
- o isométrico de 3×40 s, descanso 45 s.

No cambia descansos, no añade sesiones y no toca otros objetivos. Las referencias de GAIN_MUSCLE, INCREASE_STRENGTH y GENERAL_FITNESS son idénticas con A.

**Resultado en el perfil que fallaba** (V37 + V37.1, 4 días, casa):

| | Base | A |
|---|---|---|
| Día inferior | 4 colocaciones, 1.824 s (67,6 %) | 6 colocaciones, **1.851 s (68,6 %)** |
| Ejercicios | 1073 3×8, 1102 3×10, 507 3×8, 1054 cardio | 1073 2×8, 1102 2×10, 507 2×8, **1106** 3×40 s, **1253** 2×15, 1054 cardio |
| V20 vigente (redondeo) | rechaza | pasa (31 min) |
| 70 % exacto | rechaza | **rechaza** (faltan 39 s) |

**Calidad.** Hay tres problemas que impiden recomendar A:

1. **Depende de etiquetas incorrectas del catálogo.**
   - 1106 «Flexión a plancha lateral» es un empuje horizontal etiquetado como `waist`. La auditoría lo marca PROBABLE, con grupo real `chest`. Además se prescribe como isométrico, aunque el GIF es dinámico. Es el ejercicio que más añade A: 20–31 apariciones nuevas según la variante.
   - 1253 «Elevación de talones de pie (en una escalera)» requiere un escalón que `equipment_types` no modela (CONFIRMADO, gravedad baja).
   - Sin esas dos etiquetas, A apenas añade tiempo. **No cumple** la condición de funcionar con el catálogo corregido.
2. **Desplaza la fuerza en lugar de sumar.** Al haber más colocaciones, `ampliarSeries` ya no actúa. Los estructurales bajan de 3 a 2 series, y la sesión gana solo 27 s.
3. **Reparto.** La bandera `WAIST_IGUALA_O_SUPERA_PIERNA` en sesiones LOSE_WEIGHT pasa de 12 a 20 con A + exacto sobre V37.1. Las sesiones no se alargan en exceso (máximo 2.832 s, 80,9 %; MAX_COLOC_DIA = 8). El cardio no pierde colocaciones, pero el día inferior queda con 1 cardio frente a 4–5 resistidos.

**Interacción con las reglas.**
- V15: sin fallos con A solo.
- V24: sin excepciones generales; el cambio está aislado en una copia de la política detrás de la propiedad.
- V25: el cardio sigue presente.
- V18: intacta.

## 4. Comparación con V37 y V37 + V37.1

«Bajo 70 %» cuenta las sesiones con `segundos < piso_exacto_s` en todos los objetivos: 72 referencias y barrido de 4 a 6 días.

| Variante | Refs válidas | Refs que cambian frente a base | Sesiones ref bajo 70 % | Barrido con plan | Sesiones barrido bajo 70 % | Robustez | 192 restricciones | Continuidad 96 |
|---|---|---|---|---|---|---|---|---|
| base · V37 | 72 | — | 20 | 72/72 | 64 | 288/288 | 192 publicados, 0 violaciones | pasa |
| A · V37 | 72 | LW 14 | 20 | 72/72 | 65 | 288/288 | 192, 0 | pasa |
| exacto · V37 | 72 | LW 2, GM 4, GF 7 | **0** | 72/72 | **0** | 288/288 | 192, 0 | pasa |
| A + exacto · V37 | 72 | LW 14, GM 4, GF 7 | 0 | 72/72 | 0 | 288/288 | 192, 0 | pasa |
| base · V37.1 | 72 | — | 16 | **70/72** | 54 | **280/288** | 192, 0 | **falla** (LW/BEG/4d/casa) |
| A · V37.1 | 72 | LW 13 | 16 | 72/72 | 58 | 288/288 | 192, 0 | pasa |
| exacto · V37.1 | 72 | LW 2, GM 3, GF 7 | 0 | **68/72** | 0 | **275/288** | 192, 0 | **falla** (LW/BEG/4d/casa) |
| A + exacto · V37.1 | 72 | LW 13, GM 3, GF 7 | 0 | **71/72** | 0 | **287/288** | 192, 0 | **falla** (V15, ver §5) |

(LW = LOSE_WEIGHT, GM = GAIN_MUSCLE, GF = GENERAL_FITNESS. INCREASE_STRENGTH no cambia en las referencias de 3 días; sí cambia en el barrido con el 70 % exacto.)

**Perfiles sin plan:**
- base · V37.1: LW/BEG/4d/45/casa y LW/BEG/5d/45/casa (4 de 4 semillas cada uno).
- exacto · V37.1: los dos anteriores, LW/BEG/6d/45/casa y GF/INTERMEDIATE/4d/45/GYM. En robustez, además, GF/ADVANCED/4d/45/GYM con 1 semilla.
- A + exacto · V37.1: GF/INTERMEDIATE/4d/45/GYM y, en robustez, GF/ADVANCED/4d/45/GYM con 1 semilla.

**Equipamiento y restricciones.** En las 8 variantes hay 0 violaciones en los 192 escenarios: ni en los ejercicios elegibles ni en los del plan. Con A, ninguna sesión LOSE_WEIGHT añade equipamiento que el usuario no tenga según el catálogo. Pero 1253 necesita un escalón real que el catálogo no modela (§3).

## 5. La regla exacta del 70 % (1.890 s en 45 min)

**Lo que corrige.** Con V37 (catálogo oficial) **funciona sin crear perfiles sin plan**: las 20 sesiones de referencia y las 64 del barrido que pasaban solo por redondeo llegan al 70 % real. Lo hace con más series o con un ejercicio más del enfoque, nunca con descanso. Cambian 13 referencias:
- LW, 2;
- GM, 4: en 45 min se añade 1102 3×13 o un ejercicio del enfoque;
- GF, 7.

Ninguna referencia queda inválida.

**Lo que rompe con V37 + V37.1** (sin A):
- **LW/BEG/4d y 5d casa:** siguen sin plan. Es composición, §1.
- **LW/BEG/6d/45/casa (nuevo):** el jueves superior va justo detrás del miércoles. Le queda 1 cardio y 1.830 s. Para llegar a 1.890 s, la sesión publicada se queda **sin cardio**. La causa probable, sin traza todavía, es la sustitución de relleno por estructural del respaldo. **Corregido en `IMPLEMENTACION_V20.md` §1.3:** la causa exacta es otra. El relleno del miércoles usa el tercer cardio, y el jueves, que va pegado, se queda sin ninguno por V18. Eso lo rechaza V25 («no incluye CARDIO ... y en esa sesión es factible»).
- **GF/INTERMEDIATE/4d/45/GYM (nuevo)** y GF/ADVANCED/4d/45/GYM (1 semilla): el jueves inferior (1.851–1.866 s) se completa con otro ejercicio de `upper legs`. Queda en «3 de 5 a upper legs» y V15 lo rechaza.
- **Continuidad con A + exacto:** falla el usuario 88. Por el orden del recorrido, es GF/BEGINNER/6d/casa: «4 de 7 a upper legs» (V15). Es la misma causa: completar el día inferior añadiendo pierna.

**Cómo podrían completarse con dosis y ejercicios permitidos** (a probar, no probado):
- **GF y continuidad:**
  - en el día inferior, probar `ampliarSeries` **antes** de añadir un ejercicio que haga superar la mitad a un grupo (V15);
  - o elegir el relleno solo entre grupos por debajo de `halfCap`.
  - Faltan 24–39 s, y una serie más de un ejercicio de 2 series aporta más que eso. Hay que comprobar que la serie quede dentro del rango vigente del nivel.
- **LW 6d casa:** que el respaldo no sustituya el **único** cardio de la sesión (V25), y que use antes la ampliación de series.
- **LW 4d/5d casa:** no tiene solución con las dosis y ejercicios permitidos (§1–§3). Hacen falta más cardio de casa o resistidos correctamente etiquetados en el catálogo.

**No integrado.** El cambio sigue detrás de `fitsense.experimento.v20exacto`.

## 6. Efecto en todos los objetivos

| Objetivo | A | 70 % exacto |
|---|---|---|
| LOSE_WEIGHT | Arregla el barrido y la robustez de V37.1, pero con ejercicios mal etiquetados y sin llegar al 70 % real | Llega al 70 % real; con V37.1 no resuelve 4d/5d casa y rompe 6d casa (V25) |
| GAIN_MUSCLE | Sin cambios | 4 referencias (V37) o 3 (V37.1) más largas y válidas; sin perfiles sin plan |
| INCREASE_STRENGTH | Sin cambios | Sesiones del barrido completadas; sin perfiles sin plan |
| GENERAL_FITNESS | Sin cambios | 7 referencias cambian y siguen válidas; con V37.1, **regresión V15** en 4d GYM (intermedio y avanzado) y 6d casa principiante |

## 7. Propuesta mínima para producción (no implementada)

No se elige A solo porque haga pasar las pruebas: lo consigue apoyándose en errores de catálogo y sin cumplir el 70 % real.

1. **V20 exacto en segundos.** Mismo estimador en el validador y en el constructor. Es el cambio correcto de la regla y no requiere bajar el 70 %. Debe ir **junto** con dos correcciones de composición del motor, sin cambiar ninguna política:
   - ampliar series antes de añadir un ejercicio que rompa V15;
   - no sustituir el único cardio de una sesión LOSE_WEIGHT.
2. **No adoptar A ni B.** B no es viable (3 cardios de casa). A depende de 1106 y 1253.
3. **Catálogo primero para LW principiante en casa (4–5 días):**
   - revisar y añadir cardio de casa nivel 1;
   - corregir 1106 (`chest`) y 1253 (escalón) en V37.1;
   - y 142 (bisagra de cadera clasificada como `back`), ya pendiente.
4. **Si tras el catálogo siguen sin plan:** decisión explícita del responsable entre:
   - ampliar la política de capacidades de LOSE_WEIGHT (A, limpia de etiquetas incorrectas);
   - o aceptar que esos perfiles se generen con menos días o con otra división.

   Ninguna de las dos se toma aquí.

Orden sugerido: (1) con las dos correcciones, medir otra vez sobre V37 y V37 + V37.1; luego (3); luego, si hace falta, (4).

## 8. Riesgos, cambios de política y pruebas necesarias

**Riesgos:**
- Con el 70 % exacto, cualquier generador que complete sesiones añadiendo ejercicios puede chocar con V15 y V25. Ya ocurre en GF y en LW 6d.
- A cambia de hecho la política de LOSE_WEIGHT y aumenta el peso de cintura y gemelo en el día inferior.
- La referencia oficial V37 cambiaría en 13 perfiles con el 70 % exacto. Se regenera solo con aprobación.

**Políticas:**
- (1) no cambia políticas: solo la regla V20 y el orden de relleno.
- A requeriría aprobar una secundaria ME oportunista para LOSE_WEIGHT.
- B requeriría catálogo nuevo.

**Pruebas necesarias antes de integrar:**
- Prueba unitaria del piso en segundos: 1.889 s rechaza, 1.890 s acepta; 1.830 s y 1.836 s rechazan.
- Prueba de que el respaldo nunca deja una sesión LOSE_WEIGHT sin cardio factible.
- Prueba de que completar el día inferior no supera la mitad por grupo (GF 4d GYM y 6d casa).
- Repetir: 72 referencias, barrido de 72, 288 de robustez, 96 de continuidad y 192 restricciones, con V37 y con V37 + V37.1.
- Comprobar que las referencias de GM, IS y GF solo cambian en duración o composición válida.

## Evidencia

`docs/FitSense/evidencia/v20/prototipos/`:
- una carpeta por variante (`base_*`, `A_*`, `EX_*`, `AEX_*`) con referencias, barrido, robustez, 192 restricciones y diagnóstico;
- `comparativa.txt`, `traza_cardio_v37b.txt`, `comparar.py` y `simv20.sh`.
