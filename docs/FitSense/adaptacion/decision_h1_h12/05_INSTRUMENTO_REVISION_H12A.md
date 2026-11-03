# 05 · Instrumento de revisión profesional: H12-A y la respuesta a «demasiado difícil»

**Para:** un profesional del entrenamiento (preparador físico, entrenador o fisioterapeuta deportivo).
**Tiempo estimado:** 45–60 minutos.
**Respuestas:** en `05b_FORMULARIO_RESPUESTAS_H12A.csv` (una fila por pregunta y caso) o sobre este documento.

## 0. Contexto en una página

**FitSense** genera un plan semanal y lo adapta cuando la persona omite una sesión y registra un motivo. Cuando el motivo es **«demasiado difícil» (`TOO_DIFFICULT`)**, la intervención autoriza dos cosas:
- **sustituir** ejercicios por otros del **mismo patrón de movimiento**, por ejemplo un empuje horizontal por otro;
- **bajar la dosis** (series, repeticiones o tiempo), salvo en ganancia muscular, donde la dosis es fija por regla, y nunca por debajo de la duración mínima de la sesión.

**Lo que se observó con la IA real:**
- En **principiantes**, todos los ejercicios disponibles tienen **dificultad 1** en el catálogo (la mínima). La IA cambia **6 a 8 ejercicios por otros también de dificultad 1**, dice que son «más sencillos» y, si la persona vuelve a decir que es difícil, **los vuelve a cambiar**. A veces vuelve a ejercicios que ya habían resultado difíciles.
- En **intermedios**, la IA sí elige con frecuencia variantes de menor dificultad (41 de 64).

**La política candidata H12-A** (no activada) solo admitiría sustituciones que **bajen la dificultad del catálogo**. Si no las hay, se bajaría la dosis donde se permita, y si tampoco se puede, la sesión **se mantendría**. La duda es si eso es aceptable, y qué decir a la persona.

**Lo que el sistema puede medir:** dificultad del catálogo (1–3), patrón, músculo objetivo, equipamiento y dosis.
**Lo que no puede medir:** estabilidad, técnica, coordinación, rango, asistencia mecánica ni demanda de agarre.

## 1. Casos

Los datos salen de las corridas conservadas: PR #28–#30 con IA real y la carpeta `validacion_h1_h12` con escenarios controlados. Dosis: series × repeticiones (o segundos) y descanso.

### Caso A · R01: GANAR MÚSCULO, principiante, casa (peso corporal, mancuerna y banda), 45 min
- **Sesión que resultó difícil** (todos de dificultad 1, 3 × 10, 90 s; dosis fija por regla):
  - 933 *flexión de rodillas en apoyo*;
  - 143 *jalón con banda, agarre supino*;
  - 1195 *curl invertido de piernas asistido*;
  - 101 *press de hombros con banda*;
  - 110 *elevación de piernas rectas con banda* (2 × 13);
  - 1099 *flexión con extensión de hombro*.
- **Semana 1 (IA real):** cambió los 6, todos por otros de dificultad 1:
  - 129 *flexión con agarre cerrado (banda)*;
  - 115 *jalón con banda, agarre estrecho*;
  - 144 *sentadilla con banda*;
  - 704 *press alterno sentado con mancuerna*;
  - 63 *sit-up completo con brazos elevados*;
  - 460 *flexión con toques de pecho*.

  Justificación que vio la persona: «variantes más sencillas del mismo patrón».
- **Semana 2** (la persona vuelve a decir «difícil»): cambió **otra vez los 6**:
  - 129 → 509 *flexión cerrada de rodillas*;
  - 115 → 117;
  - 144 → 1076 *sentadilla marcha contra la pared*;
  - 704 → 739;
  - 63 → 57 *abdominal 3/4*;
  - 460 → 318 *levantamiento de cuerpo*.
- **Con H12-A:** no hay variante de menor dificultad ni dosis reducible, así que la sesión **se mantiene igual**.

### Caso B · R05: PERDER PESO, principiante, gimnasio, 60 min (sesión justo en la duración mínima)
- **Sesión:**
  - 862 *dominada gorila* (2 × 10);
  - 988 *press de pecho en palanca*;
  - 1191 *chin lateral*;
  - 1046 *curl de piernas sentado*;
  - 987 *press militar en palanca*;
  - 1092 *press de pecho interno en máquina* (2 × 8, 120 s);
  - 1054 *escalador* y 253 *gateo de oso* (3 × 90 s).
- **Semana 1 (IA real):** 8 de 8 cambiados, todos de dificultad 1, la mayoría por **máquinas asistidas**:
  - 74 *dominada paralela asistida*;
  - 72 *fondos de pecho asistidos (de rodillas)*;
  - 75 *dominada asistida*;
  - …
  - 1117 *pies rápidos* y 853 *media sentadilla*.
- **Semana 2:** 8 de 8 otra vez, y **vuelve a 1054 *escalador* y 253 *gateo de oso***, de la sesión que había resultado difícil.
- **Con H12-A:** no hay variante menor y no queda margen de duración para bajar la dosis, así que la sesión se mantiene.

### Caso C · U04: GANAR MÚSCULO, intermedio, gimnasio, 60 min
- **La IA cambia los 7 ejercicios, y 5–6 bajan de dificultad** según la corrida. Por ejemplo:
  - 380 *press inclinado a una mano en polea* (2) → 72 *fondos de pecho asistidos* (1);
  - 389 *jalón con cable* (2) → 74 *dominada paralela asistida* (1).
- **1–2 son laterales**, como 580 (1) → 313 *fondos en banco* (1), que además pasa de pectoral a tríceps.
- **Con H12-A:** se aceptan las bajadas y se rechazan solo las laterales.

### Caso D · E09: FUERZA, avanzado, gimnasio (escenario controlado)
- 923 *press alterno con kettlebell* (3) → 101 *press de hombros con banda* (1);
- 551 *limpieza con mancuerna* (3) → 125 *pase de banda* (1);
- 339 *press declinado a una mano en polea* (3) → 56 *flexión arquero* (2).
- **Las tres bajan de dificultad.** Un límite de 2 cambios por sesión (H12-C) bloquearía esta adaptación; H12-A la acepta.

### Caso E · E10: GANAR MÚSCULO, intermedio, gimnasio (escenario controlado)
- **Semana 1, propuesta incorrecta:** 1147 *dominada de escápula* (tirón vertical) → 73 *fondos asistidos* (empuje). La sesión pierde su tirón; **H1 lo rechaza**, y está aprobado.
- **Semana 2:**
  - 1110 *sentadilla a una pierna* (2) → 144 *sentadilla con banda* (1);
  - 1220 *press declinado invertido en Smith* (2) → 78 *fondos de tríceps asistidos* (1);
  - 354 *polea al pecho* (2) → 74 *dominada paralela asistida* (1).

### Caso F · E11: CONDICIÓN GENERAL, principiante, casa (escenario controlado)
- **Sin H12-A**, rotación en tres semanas: 775 *elevación de codo y push-up invertido* → 318 *levantamiento de cuerpo* → 460 *flexión con toques* → **318** otra vez.
- **Con H12-A:** baja una repetición en 3 ejercicios (semana 1) y en 1 (semana 2). En la semana 3 la sesión ya está en la duración mínima y **se mantiene**.

### Caso G · E12: GANAR MÚSCULO, principiante, casa, solo peso corporal (escenario controlado)
- No hay ninguna variante de menor dificultad, y la dosis es fija por regla.
- **Con cualquier política, la sesión se mantiene** las tres semanas.

## 2. Preguntas

Para cada pregunta, marque una opción por caso cuando se indique, y justifique en una línea.

### P1. ¿Es razonable conservar temporalmente el plan cuando no existe una alternativa autorizada?
Casos A, B y G.

| Opción | |
|---|---|
| a | Sí, una semana, si se comunica de forma honesta |
| b | Sí, pero como máximo N semanas seguidas (indique N) |
| c | No: es preferible un cambio lateral (como el actual) |
| d | No: debería pedirse una revisión del entrenamiento (P6) |
| e | Otra (explique) |

### P2. ¿Qué debería comunicarse al usuario en ese caso?
Valore los textos de `04_TEXTOS_JUSTIFICACION.md` (§2) y proponga cambios. En particular:
- **(a)** ¿Es adecuado «no encontramos un cambio compatible con tu plan, así que por ahora se mantiene igual»?
- **(b)** ¿Debe invitarse a la persona a contar qué le resultó difícil (técnica, fuerza, cansancio, dolor)?
- **(c)** ¿Debe recomendarse consultar a un profesional, y cuándo?

### P3. ¿Cuándo una reducción de dosis puede resolver «demasiado difícil»?
Casos E11 (una repetición menos por semana) y R03 (2 × 10 → 2 × 9).

| Opción | |
|---|---|
| a | Siempre que se permita: menos volumen es más llevadero |
| b | Solo si la dificultad es de resistencia o volumen, no de técnica |
| c | Solo con una reducción mínima de X % (indique) |
| d | Nunca resuelve, por sí sola, una dificultad técnica |
| e | Otra |

¿Es aceptable que la reducción se acumule semana a semana hasta la duración mínima (E11, semana 3)?

### P4. ¿Qué variantes del mismo nivel podrían ser legítimamente más sencillas?
Para cada par, marque **S** (más sencilla), **I** (igual), **M** (más difícil) o **D** (depende; explique):

| Original (dif.) | Sustituto (dif.) | Caso |
|---|---|---|
| 1191 *chin lateral* (1) | 75 *dominada asistida* (1, máquina) | B |
| 988 *press de pecho en palanca* (1) | 72 *fondos de pecho asistidos de rodillas* (1, máquina) | B |
| 862 *dominada gorila* (1) | 74 *dominada paralela asistida* (1, máquina) | B |
| 933 *flexión de rodillas* (1) | 129 *flexión con agarre cerrado (banda)* (1) | A |
| 1195 *curl invertido de piernas asistido* (1) | 144 *sentadilla con banda* (1) | A |
| 1099 *flexión con extensión de hombro* (1) | 460 *flexión con toques de pecho* (1) | A |
| 1054 *escalador* (1) | 1117 *pies rápidos* (1) | B |
| 580 *press inclinado con mancuerna* (1) | 313 *fondos en banco con rodillas flexionadas* (1) | C |

Si hay criterios generales (por ejemplo, «asistida > libre», «rodillas apoyadas > pies», «máquina > peso libre» para un principiante), ¿cuáles? Con ellos se podría construir una tabla de pares aprobados (H12-B) **sin inferirlo del nombre**.

### P5. ¿Qué sustituciones repetidas deberían evitarse?
Casos A (semana 2), B (semana 2: vuelta a 1054 y 253) y F (318 → 460 → 318).
- **(a)** ¿Volver a un ejercicio que la persona declaró difícil? Sí / No / Depende.
- **(b)** ¿Cambiar en semanas seguidas un ejercicio que acaba de entrar? Sí / No / Depende.
- **(c)** ¿Cuántas semanas debería «recordarse» un ejercicio retirado por dificultad? (indique)

### P6. ¿Qué condiciones justificarían solicitar una revisión del entrenamiento?
Marque las que correspondan e indique umbrales:

| Condición | ¿Revisión? | Umbral |
|---|---|---|
| «Demasiado difícil» N semanas seguidas en la misma sesión | | N = |
| No hay variante menor ni dosis reducible (casos A, B, G) | | |
| La sesión ya está en la duración mínima | | |
| Principiante con todo el catálogo en dificultad 1 | | |
| Se sustituye más de la mitad de la sesión | | |
| Otra | | |

¿Quién haría la revisión (profesional, la propia persona con un cuestionario, o un cambio de nivel o de objetivo)?

## 3. Lo que NO se pide

- No se pide validar el catálogo completo: eso está en `06_FICHAS_CATALOGO_PENDIENTES.md`.
- No se pide aprobar ninguna activación.
- Las respuestas sirven para decidir si H12-A (o una variante con la tabla de H12-B) se activa, y con qué mensaje.
