# Protocolo de revisión visual del catálogo FitSense (v1)

Eres revisor visual de un catálogo de ejercicios. Cada ejercicio tiene un GIF original (Gym visual) que es la
**evidencia más fiable** del movimiento. El nombre en español (`name_es`), el nombre de la fuente (`name_en`), las
instrucciones (`ES`/`EN`) y los atributos pueden estar mal: **no los des por buenos**. Tampoco decidas por el nombre.

## Material de tu lote

- `hojas/hoja_XXXX.png`: 5 ejercicios por hoja. Cada fila = cabecera de 2 líneas + 8 fotogramas equiespaciados del
  GIF + (última columna) la imagen fija. Línea 1: `#id name_es // name_en`. Línea 2 (azul):
  `body_part/target/movement_pattern dN PRESCRIPCION | eq=equipo principal req=requisitos (grupo:equipo; mismo
  grupo = alternativas, grupos distintos = todos necesarios) | banderas activas`.
- `fichas.txt`: por ejercicio, los mismos atributos más las instrucciones completas ES y EN y todas las banderas.

**Debes abrir (Read) TODAS las hojas de tu lote** y leer la ficha de cada ejercicio. Si una fila no se distingue bien,
puedes generar fotogramas grandes:
`python3 -I -B /tmp/claude-0/-home-user/d726e240-bf6c-5000-9fec-bb22a3143d76/scratchpad/cati/herr/zoom.py /tmp/claude-0/-home-user/d726e240-bf6c-5000-9fec-bb22a3143d76/scratchpad/cati/base/catalogo.tsv /tmp/claude-0/-home-user/d726e240-bf6c-5000-9fec-bb22a3143d76/scratchpad/cati/ds <salida.png> <id> 6 280`
y abrir la salida (guárdala dentro de tu carpeta de lote).

## Banderas (definiciones vigentes del esquema; marca las que el MOVIMIENTO DEL GIF exige)

- **HI** alto impacto: saltos, pliometría, levantamientos olímpicos, carrera con fase de vuelo.
- **FL** suelo: exige bajar al suelo y levantarse (tumbado, sentado o a cuatro patas en el suelo, plancha en el
  suelo, manos en el suelo con el cuerpo cerca del suelo). Un banco o una colchoneta elevada no es suelo. De pie no es suelo.
- **AX** carga axial: carga compresiva relevante sobre columna o cuello (barra sobre la espalda/hombros, cargas
  pesadas sostenidas de pie sobre la columna, peso en la cabeza/cuello). Peso corporal de pie NO es carga axial.
- **KN** rodillas: una rodilla apoya y carga peso (en suelo, banco o almohadilla).
- **HG** colgarse: el cuerpo cuelga del agarre (barra, anillas, correas), pies sin apoyo. Incluye asistidas.
- **OH** sobre la cabeza: brazos por encima de la cabeza en alguna fase, empujando, tirando o sin carga.
- **INV** inversión: cabeza claramente por debajo de la cadera con el peso en brazos, hombros o cabeza.

**Apoyo suspendido** (no es una bandera; regístralo aparte en `apoyo_suspendido`):
- `BRAZOS_EXTENDIDOS`: el peso corporal se sostiene sobre los brazos/antebrazos en paralelas, estación de fondos,
  silla romana/captain's chair o anillas, con los pies sin apoyo (no es colgarse de una barra).
- `PIES_SUSPENDIDOS`: pies en correas (TRX/suspensión) o anillas con el cuerpo apoyado en manos/antebrazos.
- `MAQUINA_PARCIAL`: apoyo parcial del cuerpo en una máquina (p. ej. dominada asistida en máquina de rodillas).
- `NO`: nada de lo anterior.

## Qué compruebas en cada ejercicio

1. `movimiento_gif`: describe en una frase, en español, lo que el GIF muestra: posición (de pie / sentado en
   banco / tumbado boca arriba en suelo / colgado / en paralelas…), implemento y acción. Es la referencia de todo lo demás.
2. `nombre_ok`: ¿`name_es` describe ESE movimiento? `NO` si nombra otro ejercicio (p. ej. «dominadas» cuando el GIF
   es un remo invertido con talones en el suelo), otra posición, otro implemento, o es una traducción que cambia el
   sentido. Errores menores de estilo = `SI` con nota. Si `NO`, propón `nombre_propuesto` (español natural, describe el GIF).
3. `instr_ok`: ¿las instrucciones ES describen ESE movimiento (posición inicial, implemento, trayectoria)? `NO` si
   describen otra posición/implemento/ejercicio (p. ej. «túmbate en el suelo» cuando el GIF es de pie, «cuélgate de una
   barra» cuando el GIF es en suelo con correas). Imprecisión menor = `SI` con nota. En `problema_instr` cita la frase
   que contradice al GIF.
4. `equipo_ok`: ¿el equipo/requisitos bastan y corresponden al GIF? Si no, `equipo_gif` = lo que el GIF usa
   (códigos posibles: body weight, dumbbell, barbell, ez barbell, olympic barbell, kettlebell, cable, band,
   leverage machine, smith machine, sled machine, bench, pull-up bar, parallel bars, captains chair, gymnastic rings,
   suspension straps, stability ball, medicine ball, bosu ball, rope, landmine, step platform, plyo box,
   stationary bike, treadmill, ab wheel, balance board, weighted, assisted, other, ghd bench, hyperextension bench,
   preacher bench, squat rack, vertical pole).
5. `clasif_ok`: ¿body_part / target / movement_pattern son coherentes con el movimiento? Patrones válidos:
   HORIZONTAL_PUSH, HORIZONTAL_PULL, VERTICAL_PUSH, VERTICAL_PULL, KNEE_DOMINANT, HIP_HINGE, HIP_EXTENSION,
   HIP_ABDUCTION_ADDUCTION, ELBOW_FLEXION, ELBOW_EXTENSION, SHOULDER_RAISE, SHOULDER_EXTENSION, HORIZONTAL_ADDUCTION,
   SCAPULAR_ELEVATION, WRIST_FOREARM, ANKLE_PLANTAR_FLEXION, TRUNK_FLEXION, TRUNK_EXTENSION,
   TRUNK_ROTATION_LATERAL_FLEXION, CORE_STABILITY, LOCOMOTION_CARDIO, MOBILITY, BALANCE_CONTROL, OTHER.
   Body parts: back, cardio, chest, lower arms, lower legs, neck, shoulders, upper arms, upper legs, waist.
   Solo `NO` si es claramente errónea (p. ej. una sentadilla clasificada como back/HIP_HINGE); si es discutible, `DUDA`.
6. `banderas_gif`: lista separada por espacios de las banderas que el GIF EXIGE según las definiciones (o `-`).
   Decide por el GIF, sin mirar las banderas actuales. Si dudas en una, añádela a `banderas_duda`.
7. `apoyo_suspendido`: uno de los valores de arriba.
8. `dificultad_ok`: d1 principiante, d2 intermedio, d3 avanzado. `NO` solo si es claramente inapropiada (habilidad
   avanzada en d1, o ejercicio trivial en d3); si no, `SI` o `DUDA`.
9. `gravedad`: `ALTA` si alguna discrepancia puede exponer a alguien a algo que su perfil excluye (bandera que el
   GIF exige y no está marcada; texto que pide colgarse/suelo/arriba/rodillas contradiciendo al GIF o a las
   banderas) o da a un principiante una habilidad avanzada; `MEDIA` clasificación/equipo; `BAJA` texto menor;
   `NINGUNA` todo coherente.

## Reglas

- No inventes. Si el GIF no permite decidir algo, usa `DUDA` y explícalo en `notas`. Si el GIF no se ve o no
  corresponde a ningún movimiento reconocible, `gif_legible = NO`.
- No marques `SI` en nombre/instrucciones por defecto: compara de verdad con los fotogramas.
- No modifiques ningún archivo fuera de tu carpeta de lote. No accedas a la red.

## Salida (obligatoria)

Escribe `revision.tsv` en tu carpeta de lote, UTF-8, separador TAB, SIN tabuladores ni saltos de línea dentro de
los campos, con esta cabecera exacta y **una fila por cada id de `ids.txt`** (ni más ni menos):

```
id	gif_legible	movimiento_gif	nombre_ok	nombre_propuesto	instr_ok	problema_instr	equipo_ok	equipo_gif	clasif_ok	clasif_propuesta	banderas_gif	banderas_duda	apoyo_suspendido	dificultad_ok	gravedad	notas
```

Valores: `gif_legible` SI/NO; `*_ok` SI/NO/DUDA; campos vacíos permitidos solo en propuestas/notas cuando no aplica
(usa `-`). Al terminar, comprueba con un pequeño script que el archivo tiene exactamente las filas de `ids.txt`.
