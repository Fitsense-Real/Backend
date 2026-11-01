# Verificación de C2 por objetivo

**Laboratorio:** `VerificacionPoliticasIT` (opt-in `-Dfitsense.verificacion.politicas=true`).
- PostgreSQL 16 efímero (Testcontainers): el JDBC se comprueba como `localhost` antes de cada prueba y Flyway corre sobre la base efímera.
- Motor de reglas, sin IA y sin Replicate.
- Datos: `MATRIZ_C2_VERIFICADA.csv` (960 perfiles × 6 caminos), `DIAGNOSTICO_DISCREPANTES.csv` y `PLANES_RECUPERADOS_DETALLE.csv`.

## 1. Reproducción del laboratorio anterior

- Mismos 960 perfiles, en el mismo orden y con las mismas semillas (por `user_id`). No se tocó ninguna semilla.
- Comparado fila a fila con `auditoria_cientifica_entrenamiento/MATRIZ_EXPERIMENTOS_REVISADA.csv`:
  - **0 diferencias** en `user_id`, resultado, piso usado, series semanales y segundos de la sesión más larga, tanto en C1 como en C2-A (antes C2_MINIMA).
  - **C1: 875 válidos** (79 rechazados por V20, 6 por V15).
  - **C2-A: 950** (piso del 70 % en 881, del 55 % en 64, del 65 % en 7, del 60 % en 4; 4 inválidos al 50 %).
  - **GAIN_MUSCLE a 75 min: 56/60.**
- **V4:** 0 sesiones por encima de M × 1,15 en todos los caminos (V4 estricto, en segundos).
- **Dosis:** ninguna dosis no autorizada. Todos los planes pasan el validador vigente con V24 en ENFORCE y las mismas dosis 4D.
- **V24:** sin modificar. Ninguna variante C2 cambia topes ni presupuesto.
- **Planes que ya eran válidos:** idénticos en las tres variantes regeneradas (881 de 881 con piso del 70 %). Las diferencias aparecen solo en los planes que se regeneran (C2-A: 925 de 960 idénticos).
- **Determinismo:** dos ejecuciones dan una matriz idéntica, salvo la columna de milisegundos.

## 2. Resultados por variante

| Objetivo | C1 | C2-A (50 %) | C2-B (55 %) | C2-C (55/65 %) | C2-D (saturación) | C2-E (otra composición, 70 %) |
|---|---:|---:|---:|---:|---:|---:|
| GAIN_MUSCLE | 172 | 236 | 236 | 236 | **240** | 172 |
| INCREASE_STRENGTH | 236 | 240 | 240 | 236 | **240** | 236 |
| LOSE_WEIGHT | 230 | 234 | 234 | 230 | **234** | 230 |
| GENERAL_FITNESS | 237 | 240 | 240 | 240 | **240** | 237 |
| **Total (de 960)** | **875** | **950** | **950** | **942** | **954** | **875** |
| GAIN_MUSCLE 75 min (de 60) | 0 | 56 | 56 | 56 | 60 | 0 |
| Planes distintos de C1 | — | 35 | 35 | 31 | **0** | 0 |

- **C2-B** da lo mismo que C2-A: ningún perfil necesita el 50 % para quedar válido. Los 4 que lo intentan siguen inválidos.
- **C2-C da 942**, como se anticipaba (950 − 8). No se asumió: se ejecutó.
- **C2-D (diferenciado por contenido)** no impone un porcentaje. Acepta la semana del motor vigente cuando V20 es la única regla incumplida y las sesiones cortas están **saturadas**. El porcentaje resultante se informa, no se fija:

| % de M de la sesión más corta | 46 | 56 | 57 | 58 | 60 | 65 | 66 | 68 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Planes | 4 | 56 | 4 | 4 | 4 | 2 | 1 | 4 |

- **C2-E** no recupera ningún caso: las 79 semanas rechazadas por V20 ya estaban saturadas. No hay otra composición válida, dentro de las reglas, que llegue al 70 %.

**Sobre C2-D frente a mínimos «por evidencia».** La revisión científica no encontró ninguna evidencia de una duración mínima de sesión, ni por objetivo ni por nivel (`REVISION_CIENTIFICA_ACTUALIZADA.md` §4.10). Por eso **no hay base para fijar porcentajes distintos por objetivo**. Los 55/65 % de C2-C son decisiones de ingeniería. El único criterio defendible sin inventar cifras es el de **contenido**: la sesión tiene todo lo que las reglas de 4A, 4C, 4D, V15, V18, V22 y V24 permiten.

## 3. Casos que siguen sin plan

| Variante | Sin plan |
|---|---|
| C2-C (18) | Los 8 discrepantes (abajo); 4 GAIN_MUSCLE intermedio/avanzado con peso corporal, 6–7 días y 75 min (R0788, R0792, R0836, R0840), con sesión PULL al 46 %; 6 LOSE_WEIGHT de 1 día y 45 min por **V15** |
| C2-A y C2-B (10) | Los 4 GAIN_MUSCLE con peso corporal y los 6 por V15 |
| C2-D (6) | Solo los 6 por V15: no es V20, así que C2 no actúa (condición estricta) |

## 4. Los ocho casos discrepantes de C2-C

| Perfil | Objetivo / nivel | Equipo | Días (sesiones) | M | Piso necesario (C2-A) | Sesión más corta del plan vigente |
|---|---|---|---|---:|---:|---|
| R0800 | INCREASE_STRENGTH intermedio | Peso corporal | 6 (6) | 75 | 55 % | PULL: 2.634 s (58,5 %), 7 ejercicios |
| R0804 | INCREASE_STRENGTH avanzado | Peso corporal | 6 (6) | 75 | 55 % | PULL al 58,5 % |
| R0848 | INCREASE_STRENGTH intermedio | Peso corporal | 7 (6) | 75 | 55 % | PULL al 58,5 % |
| R0852 | INCREASE_STRENGTH avanzado | Peso corporal | 7 (6) | 75 | 55 % | PULL al 58,5 % |
| R0436 | LOSE_WEIGHT principiante | Mancuernas y bandas | 4 (4) | 75 | 60 % | LOWER_BODY: 2.706 s (60,1 %), 6 colocaciones |
| R0532 | LOSE_WEIGHT principiante | Mancuernas y bandas | 7 (4) | 75 | 60 % | LOWER_BODY al 60,1 % |
| R0724 | LOSE_WEIGHT principiante | Peso corporal | 4 (4) | 75 | 60 % | LOWER_BODY al 60,1 % |
| R0820 | LOSE_WEIGHT principiante | Peso corporal | 7 (4) | 75 | 60 % | LOWER_BODY al 60,1 % |

### 4.1 Contenido

**INCREASE_STRENGTH**
- PULL: 1 ejercicio de fuerza 3×6/150 s («Elevación de codo y puño») y 6 de hipertrofia 3×10/90 s: «Arquero de pie», «Toque de dos dedos», «Curl de bíceps acostado», «Curl de concentración», «Inclinación pélvica», «Curl de espalda baja».
- PUSH y LEGS: 4 de fuerza 3×6/150 y 4 de hipertrofia, al 70–71,5 %.

**LOSE_WEIGHT**
- LOWER_BODY: 3 ejercicios de fuerza 3×8/120 s (pierna) y 3 de cardio 3×90 s/60 s.
- UPPER_BODY: 6 de fuerza y 2 de cardio, al 71,7 %.

Detalle completo en `PLANES_RECUPERADOS_DETALLE.csv`.

### 4.2 Por qué no llegan al 65 % (`DIAGNOSTICO_DISCREPANTES.csv`)

Se probó en cada sesión corta:
- **añadir cada ejercicio elegible** con su dosis base aprobada;
- **añadir una serie**, hasta 3, a cada ejercicio.

Cada candidato se validó con el validador vigente completo.

| Caso | Candidatos | Ya en el día | Día pegado (V18) | Fuera del enfoque | Sin dosis base | Rechazos del validador | Aceptables | Series |
|---|---:|---:|---:|---:|---:|---|---:|---|
| R0800 PULL | 137 | 7 | 16 | 93 | 15 | V22 = 6 (tríceps en PULL) | **0** | Las 7 ya tienen 3 |
| R0436 LOWER_BODY | 197 | 6 | 8 | 72 | 87 | V15 = 24 (otro ejercicio de pierna dejaría ese grupo por encima de la mitad) | **0** | Las 6 ya tienen 3 o son de duración |
| R0724 LOWER_BODY | 127 | 6 | 8 | 29 | 69 | V15 = 15 | **0** | Igual |

### 4.3 Causa

**INCREASE_STRENGTH**
- **Catálogo sin material, más V18 y V22.**
- Con 6 sesiones (PUSH, PULL, LEGS ×2), los ejercicios de tracción sin material se agotan.
- Los que quedan del enfoque son de tríceps, que V22 no admite en PULL.
- Tampoco hay series que subir: todas las colocaciones tienen 3.
- **No es la dosis ni el constructor:** el constructor ya probó a ampliar series y a sustituir.

**LOSE_WEIGHT**
- **Composición del día y V15.**
- La sesión de tren inferior solo admite grupos de pierna, y añadir otro ejercicio de pierna rompe la regla de la mitad.
- Los ejercicios de nivel principiante con dosis admisible son pocos (87 de 197 sin dosis base).

### 4.4 ¿Existe otra composición válida que llegue al 65 %?

**No, dentro de las reglas vigentes.**
- La búsqueda C2-E (añadir o subir series, paso a paso) no encontró ningún cambio válido.
- El constructor vigente ya intentó sustituciones por duración.
- **Límite:** la búsqueda es voraz y no exhaustiva. No se probaron combinaciones de varias sustituciones simultáneas.

### 4.5 ¿Es razonable reducir V20 en estos casos?

**Sí, con C2-D. Con C2-C, no se recuperan.**

| Caso | Situación |
|---|---|
| INCREASE_STRENGTH | Las sesiones están llenas: 7 ejercicios de 3 series, y el tope es 8. El volumen semanal es alto (138 series; 42 de pierna) |
| LOSE_WEIGHT | 45 min (60 %) para un principiante con 3 ejercicios de fuerza y 3 bloques de cardio |

- No hay evidencia de que una sesión deba durar al menos un porcentaje del tiempo disponible.
- El riesgo no es fisiológico. Es la expectativa del usuario (75 configurados frente a 45), y se trata mostrando la duración estimada (`CALIDAD_PLANES_RECUPERADOS.md` §5).
- **Pendiente: revisión profesional.** La muestra incluye R0800 y R0436 (pregunta específica de la ficha).

## 5. GAIN_MUSCLE a 75 min

- **Duración real:** 2.550 s = **42,5 min (56,7 %)** en gimnasio y en casa con 1–4 días.
- 7 ejercicios (el tope de V24), estructurales 3×10/90 s; zona pequeña 3×13.
- **Es el mismo plan que con 60 min** (`REFERENCIA_MENOR_TIEMPO.csv`).
- **Causa:** el tope de 7 colocaciones (V24 a ≥ 60 min) y las 3 series máximas.
- Ninguna variante C2 añade contenido. C2 solo permite publicar el plan que la estructura ya da.
