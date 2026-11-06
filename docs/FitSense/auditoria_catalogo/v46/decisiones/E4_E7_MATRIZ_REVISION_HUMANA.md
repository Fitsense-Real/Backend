# E4 / E7 · Matriz de revisión humana

**Estado: documento para revisión. No es una validación profesional.**
- La descripción del GIF sale de una revisión **asistida por IA**: 8 fotogramas por GIF, con hojas generadas en el *scratchpad*.
- Las hojas no se versionan porque los GIF tienen copyright. Se regeneran con `ExportarCatalogoIT` y el *dataset* `hasaneyldrm/exercises-dataset`, en la ruta de la columna `gif_path`.
- **No se ha escrito ninguna instrucción nueva en la base de datos ni en ninguna migración.** Los textos de §3 son propuestas.

**Tipos de hallazgo:**
- **OBJ** (contradicción objetiva): dos fuentes del propio catálogo dicen cosas incompatibles. Por ejemplo, el nombre o las instrucciones frente al GIF, o el equipamiento frente a las instrucciones. Se comprueba sin criterio deportivo.
- **PRO** (decisión profesional): requiere criterio de una persona con formación en ciencias del deporte (dificultad, patrón, dosis o seguridad).

## 1. Matriz

Las columnas de la clasificación actual son: grupo, músculo objetivo, equipamiento, dificultad, patrón y aislado.

| ID | GIF | Nombre original (EN) | Traducción actual (ES) | Instrucciones actuales (resumen) | Clasificación actual | Qué muestra el GIF (asistido) | Contradicciones objetivas (OBJ) | Decisiones profesionales (PRO) | Corrección propuesta |
|---|---|---|---|---|---|---|---|---|---|
| 853 | `videos/3221-ia6kIIl.gif` | half knee bends (male) | Media sentadilla | De pie: flexionar rodillas «como si te sentaras», pausa abajo, subir empujando con talones | cardio · cardiovascular system · body weight · 1 · LOCOMOTION_CARDIO · no | Marcha o trote en el sitio con elevación alterna de rodillas y braceo. **No hay flexión bilateral de rodillas** | (1) El nombre (EN y ES) y las instrucciones describen una sentadilla; el GIF, una marcha. (2) Las instrucciones dicen «repeticiones» y la prescripción es DURATION | ¿Es adecuado como cardio de baja intensidad para principiantes? ¿Con qué duración? | **OBJ:** renombrar según el GIF (p. ej. «Marcha en el sitio con elevación de rodillas») y reescribir las instrucciones. La clasificación de cardio es coherente con el GIF. **Si no se corrige, solo afecta a lo que se muestra.** Evaluación real: 10 de los 20 REQUIERE_REVISION |
| 580 | `videos/0316-B3Rxp6L.gif` | dumbbell incline breeding | Curl inclinado con mancuerna | Banco a 45°: bajar las mancuernas a los lados del pecho con los codos a 90° y empujar arriba (= press) | chest · pectorals · dumbbell + bench · 1 · HORIZONTAL_PUSH · no | Banco inclinado; las mancuernas describen un **arco con los codos casi extendidos**: apertura (*fly*) inclinada | (1) «Curl» es una traducción errónea («breeding» parece una errata de *fly*). (2) Las instrucciones describen un press y el GIF una apertura | ¿Apertura o press? ¿HORIZONTAL_PUSH como patrón de una aducción horizontal? ¿Es aislado (la apertura suele considerarse un ejercicio de aislamiento)? | **OBJ:** traducir «Apertura inclinada con mancuernas» y alinear las instrucciones con el GIF. **PRO:** patrón y `is_isolation` |
| 775 | `videos/1772-wbUYILZ.gif` | elbow lift - reverse push-up | Elevación de codo y push-up invertido | Tumbado **boca abajo en el suelo**: presionar con las palmas para elevar el tronco, codos pegados (similar a una cobra) | back · upper back · body weight · 1 · HORIZONTAL_PUSH · no | Tronco sobre una almohadilla o pedestal, piernas extendidas en el suelo; el tronco sube y baja con los codos flexionados. **No es concluyente si la persona está boca arriba (empujando con los codos) o boca abajo.** Se necesita la almohadilla | (1) El equipamiento es «body weight», pero el GIF usa un soporte. (2) Las instrucciones son «en el suelo» y el GIF usa el soporte. (3) El grupo espalda con el patrón de empuje es incoherente con un remo o tracción | **Identificar el movimiento** (revisión visual humana). ¿Aporta dosis de espalda? (E2) | **Revisión humana primero.** Mientras tanto, E2-a: aviso DOSIS_NO_DEMOSTRADA. No se reclasifica |
| 1093 | `videos/3638-PrQbjvB.gif` | push to run | Empujar para correr | Posición de flexión, flexión y luego rodillas alternas al pecho rápido (escaladores) | cardio · cardiovascular system · body weight · 1 · LOCOMOTION_CARDIO · no | **De pie**: elevaciones alternas de rodilla o patadas frontales con braceo. **No hay flexión ni apoyo de manos** | Las instrucciones (flexión + escaladores en el suelo) no corresponden al GIF (de pie) | Intensidad e impacto (¿alto impacto?) para principiantes | **OBJ:** reescribir las instrucciones según el GIF y revisar el nombre. La clasificación de cardio es coherente |
| 1195 | `videos/0697-GwYwElT.gif` | self assisted inverse leg curl | Curl invertido de piernas asistido | Boca arriba: llevar los muslos al pecho (*knee tuck*) | upper legs · hamstrings · body weight (sin requisitos) · **1** · KNEE_DOMINANT · sí | De rodillas sobre la almohadilla de un **bastidor con rodillos que sujetan los tobillos**; desciende el tronco recto hacia delante, recoge la caída con las manos y vuelve: **curl nórdico con recogida de manos** | (1) Las instrucciones describen otro ejercicio. (2) El equipamiento dice «body weight» pero el GIF exige anclaje de tobillos | Dificultad (ver `E3_CURL_NORDICO.md`); dosis inicial | **OBJ:** instrucciones nuevas (§3) y requisito de anclaje (E5/E6). **PRO:** dificultad |
| 1196 | `videos/1766-ZSY3MsL.gif` | self assisted inverse leg curl | Curl de piernas invertido asistido | Tumbado boca abajo en una **máquina** de curl femoral | upper legs · hamstrings · body weight (sin requisitos) · **1** · KNEE_DOMINANT · sí | De rodillas en el suelo con los **tobillos bajo un banco**; mismo descenso con recogida de manos | (1) Las instrucciones piden una máquina y el equipamiento es peso corporal (error comprobado sin GIF). (2) El anclaje bajo el banco no está declarado | Dificultad; ¿un banco doméstico es un anclaje seguro? | **OBJ:** instrucciones nuevas (§3) y requisito «banco O anclaje». **PRO:** dificultad y seguridad del anclaje |
| 1320 | `videos/0815-7aVz15j.gif` | triceps dips floor | Dip de tríceps en piso | **Sentado en el borde de una silla o banco**, bajar el cuerpo hacia el suelo | upper arms · triceps · body weight · 1 · HORIZONTAL_PUSH · no | **Sentado en el suelo**, manos apoyadas detrás de las caderas y rodillas flexionadas; extiende y flexiona los codos elevando la cadera (fondo en el suelo) | Las instrucciones piden una silla y el nombre y el GIF son en el suelo. 194 planes REV2 en casa sin material reciben instrucciones que piden silla | ¿Patrón de empuje horizontal o vertical (fondos)? | **OBJ:** instrucciones nuevas según el GIF (§3). **No** exigir silla: el GIF no la usa |

### 1.b Añadidos durante la medición (P15-C y E3)

| ID | GIF | Nombre original (EN) | Traducción actual | Instrucciones (resumen) | Clasificación actual | Qué muestra el GIF (asistido) | OBJ | PRO | Propuesta |
|---|---|---|---|---|---|---|---|---|---|
| 1205 | `videos/1489-xdYPUtE.gif` | sissy squat | Sentadilla sissy | Pies a la anchura de los hombros, «tronco erguido», bajar hasta muslos paralelos y «empujar con los talones» | upper legs · quads · body weight · **1** · KNEE_DOMINANT · no | Una mano en el respaldo de un banco inclinado; rodillas muy adelantadas, **talones elevados**, tronco inclinado atrás en línea con los muslos; flexión de rodilla muy profunda | Las instrucciones («tronco erguido», «talones») contradicen el GIF. El apoyo que usa el GIF no está declarado | Dificultad (con P15-C es el sustituto de rodilla más usado) | **PRO:** dificultad 2 (CF6). **OBJ:** instrucciones según el GIF |
| 1076 | `videos/0624-sVQCCeG.gif` | march sit (wall) | Sentadilla marcha (contra la pared) | Sentada en pared a 90° y elevar rodillas de forma alterna | upper legs · glutes · body weight · 1 · KNEE_DOMINANT · no | Coincide: sentada isométrica en pared con elevación alterna de rodilla | — | ¿Adecuado para principiantes como cobertura de rodilla? | Mantener 1 (pendiente) |
| 939 | (ver `gif_path`) | kick out sit | Abdominal patada | Sentado en el borde de un banco o silla, extender las piernas al frente | upper legs · **hamstrings** · body weight · 1 · KNEE_DOMINANT · no | No revisado | Extender las rodillas es trabajo de cuádriceps, no de isquios. Pide silla sin declararla | Clasificación | Revisión visual y reclasificación |

## 2. Resumen: objetivo frente a profesional

| ID | Contradicciones objetivas (se pueden corregir sin criterio deportivo, con aprobación) | Requiere decisión profesional |
|---|---|---|
| 853 | Nombre (EN/ES) e instrucciones frente al GIF | Cardio para principiantes (duración) |
| 580 | Traducción «Curl»; instrucciones de press frente al GIF de apertura | Patrón, aislado |
| 775 | Equipamiento (soporte); instrucciones «en el suelo» | **Identificar el movimiento**; dosis de espalda |
| 1093 | Instrucciones frente al GIF | Impacto e intensidad |
| 1195 / 1196 | Instrucciones de otro ejercicio; anclaje no declarado | Dificultad; dosis; seguridad del anclaje |
| 1320 | Instrucciones con silla frente al GIF en el suelo | Patrón |

## 3. Instrucciones propuestas (E7; NO escritas en producción)

**Criterios:**
- describen solo lo que muestra el GIF;
- no prometen resultados;
- indican la regresión disponible y cuándo parar;
- usan el tuteo, como el resto del catálogo.

### 1320 · Fondos de tríceps en el suelo
> Siéntate en el suelo con las rodillas flexionadas y los pies apoyados, separados a la anchura de las caderas. Apoya las manos en el suelo detrás de las caderas, con los dedos apuntando hacia los pies. Empuja con las manos para extender los codos y elevar ligeramente la cadera. Flexiona los codos de forma controlada hasta acercar los glúteos al suelo, sin apoyarlos, y vuelve a extenderlos. Mantén los hombros lejos de las orejas. Si notas molestias en los hombros o las muñecas, reduce el recorrido.

### Curl nórdico asistido con las manos (1195, 1196, 1197, 915)
> Arrodíllate sobre una superficie acolchada con los tobillos bien sujetos: bajo los rodillos de un banco, bajo un banco estable o con una correa anclada. Mantén el cuerpo en línea recta desde las rodillas hasta la cabeza, con los brazos delante del pecho. Inclínate hacia delante muy despacio, frenando el descenso con la parte posterior de los muslos mientras puedas controlarlo. Cuando ya no puedas frenar, apoya las manos en el suelo como en una flexión y empuja suavemente para volver a la posición de rodillas, ayudándote con los brazos todo lo necesario. Haz pocas repeticiones y detente si notas dolor en la parte posterior del muslo.

**Variante específica de cada ejercicio:**
- **1195:** «bajo los rodillos de un banco de nórdicos o de un bastidor».
- **1196:** «con los tobillos enganchados bajo un banco estable que no pueda volcar».
- **1197:** «sobre un *step* con los tobillos sujetos por una correa».
- **915:** «sobre la almohadilla de un banco con rodillos».

### Curl nórdico asistido en polea (327) y en máquina de polea (916)
> **327:** Arrodíllate en la almohadilla de la máquina con los tobillos bajo los rodillos y sujeta la barra de la polea alta. La polea descarga parte de tu peso. Inclínate hacia delante muy despacio, con el cuerpo recto desde las rodillas hasta la cabeza, frenando con la parte posterior de los muslos. Vuelve a la posición de rodillas con ayuda de la polea. Ajusta el peso de la polea para que puedas controlar todo el descenso.
>
> **916:** Arrodíllate en la almohadilla de la máquina de jalón con los tobillos bajo los rodillos y un banco delante para recogerte con las manos. Desciende como en el curl nórdico asistido y vuelve empujando con las manos.

**Pendiente profesional:** si la recogida con manos y el tope de repeticiones son suficientes como regresión para principiantes, o si el ejercicio no debe proponerse a ese nivel (ver E3).
