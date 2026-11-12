# Campaña con Replicate real · modo adaptacion

Duración 88 min · 12 perfiles · 138 llamadas reales · 138 predicciones con métricas · tokens 6582515 de entrada y 588029 de salida · **coste estimado US$ 2.82** (a 0.25 / 2.0 US$ por millón; la cifra oficial es la del panel de Replicate)

## Generación inicial

| Objetivo | Perfiles | Origen IA | Origen reglas (respaldo) | Llamadas | Válida V24+V25 | Rechazos por regla (intentos fallidos) |
|---|---:|---:|---:|---:|---:|---|
| LOSE_WEIGHT | 3 | 3 | 0 | 3 | 3 | — |
| GAIN_MUSCLE | 3 | 2 | 1 | 5 | 3 | CONTRATO/OTRO×1, V24×1 |
| INCREASE_STRENGTH | 3 | 3 | 0 | 5 | 3 | V24×4 |
| GENERAL_FITNESS | 3 | 3 | 0 | 4 | 3 | V23×1 |

## Ciclos semanales

| Objetivo | Ciclos | Con IA | Llamadas | APLICADA | SIN_EFECTO | RESPALDO | RECHAZADA | NO_FACTIBLE | SIN_ORDEN | Con reintento | Pasos (prog/recup/reinc/vuelta) | Rechazos por regla (intentos fallidos) | Violaciones |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|---|---:|
| LOSE_WEIGHT | 20 | 14 | 28 | 9 | 0 | 2 | 0 | 3 | 6 | 9 | 0/0/0/0 | V11×1, V17×8, V20×6, V8C×21 | 0 |
| GAIN_MUSCLE | 24 | 16 | 24 | 8 | 6 | 1 | 0 | 1 | 8 | 5 | 0/0/0/0 | V11×2, V17×1, V20×4, V24×1, V8C×4 | 0 |
| INCREASE_STRENGTH | 24 | 16 | 32 | 13 | 0 | 2 | 0 | 1 | 8 | 11 | 0/0/0/0 | V11×2, V15×1, V16×1, V17×9, V20×9, V8C×14 | 0 |
| GENERAL_FITNESS | 22 | 16 | 37 | 9 | 1 | 3 | 0 | 3 | 6 | 14 | 0/0/0/0 | V11×2, V17×16, V20×8, V8C×19, V9×6 | 0 |

## Por intervención (ciclos con IA)

| Intervención | Ciclos | APLICADA | SIN_EFECTO | RESPALDO | NO_FACTIBLE/RECHAZADA | Llamadas |
|---|---:|---:|---:|---:|---:|---:|
| {ACORTAR_O_REORGANIZAR} | 12 | 10 | 1 | 1 | 0 | 19 |
| {INVESTIGAR_CAMBIO_MINIMO,INVESTIGAR_CAMBIO_MINIMO} | 7 | 3 | 2 | 2 | 0 | 18 |
| {REVISAR_CARGA_Y_RECUPERACION} | 24 | 15 | 4 | 5 | 0 | 41 |
| {SEGURIDAD_ANTE_DOLOR} | 7 | 4 | 0 | 0 | 3 | 18 |
| {VARIANTE_MAS_SENCILLA_O_CARGA} | 12 | 7 | 0 | 0 | 5 | 25 |

Latencia por llamada: media 37 s, máxima 80 s. Llamadas con error del proveedor: 0.

## Violaciones de las comprobaciones independientes (0)

Ninguna.

## Hallazgos cuantificados (desde las correcciones 4 y 5 tambien cuentan como violacion)

- APLICADA_SOLO_POR_MINUTOS: 0 ciclos
- ANADE_MAS_DE_LO_QUE_RETIRA: 0 ciclos

## Perfiles interrumpidos (8)

- LOSE_WEIGHT/BEGINNER/30/CASA: sin semana publicada en la 9
- LOSE_WEIGHT/INTERMEDIATE/45/GYM: sin semana publicada en la 7
- LOSE_WEIGHT/ADVANCED/60/GYM: sin semana publicada en la 7
- GAIN_MUSCLE/INTERMEDIATE/45/GYM: sin semana publicada en la 7
- INCREASE_STRENGTH/ADVANCED/60/GYM: sin semana publicada en la 7
- GENERAL_FITNESS/BEGINNER/30/CASA: sin semana publicada en la 9
- GENERAL_FITNESS/INTERMEDIATE/45/GYM: sin semana publicada en la 9
- GENERAL_FITNESS/ADVANCED/60/GYM: sin semana publicada en la 7

## Detalle por perfil

### LOSE_WEIGHT/BEGINNER/30/CASA

Inicial: AI, 1 llamadas, VALIDA

- s2 [HECHO,HECHO,FATIGA] ADAPT_LOCALIZED/APLICADA {REVISAR_CARGA_Y_RECUPERACION} · llamadas 1
  - antes: `3 34min [1054:3x90s/60s, 1093:3x90s/60s, 1073:2x8r/120s, 115:2x8r/120s, 1096:2x8r/120s, 1102:2x10r/120s]`
  - después: `6 25min [1054:2x60s/90s, 1093:2x60s/90s, 1073:2x8r/120s, 115:2x8r/120s, 1096:2x8r/120s, 1102:2x10r/120s]`
- s3 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s4 [HECHO,HECHO,TIEMPO] ADAPT_LOCALIZED/APLICADA {ACORTAR_O_REORGANIZAR} · llamadas 1
  - antes: `9 25min [1054:2x60s/90s, 1093:2x60s/90s, 1073:2x8r/120s, 115:2x8r/120s, 1096:2x8r/120s, 1102:2x10r/120s]`
  - después: `12 18min [1054:2x30s/60s, 1093:2x30s/60s, 1073:2x8r/60s, 115:2x8r/60s, 1096:2x8r/60s, 1102:2x10r/60s]`
- s5 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s6 [HECHO,HECHO,PARCIAL_CARGA] ADAPT_LOCALIZED/APLICADA {REVISAR_CARGA_Y_RECUPERACION} · llamadas 2
  - antes: `15 18min [1054:2x30s/60s, 1093:2x30s/60s, 1073:2x8r/60s@20kg, 115:2x8r/60s, 1096:2x8r/60s, 1102:2x10r/60s]`
  - después: `18 33min [1054:2x20s/180s, 1093:2x20s/180s, 1073:2x8r/180s@20kg, 115:2x8r/180s, 1096:2x8r/180s, 1102:2x10r/180s]`
- s7 [HECHO,DIFICIL,HECHO] ADAPT_LOCALIZED/APLICADA {VARIANTE_MAS_SENCILLA_O_CARGA} · llamadas 1
  - antes: `17 34min [1093:3x90s/60s, 253:3x90s/60s, 1263:2x8r/120s, 739:2x8r/120s, 1156:2x8r/120s, 1103:2x10r/120s]`
  - después: `20 28min [1114:2x10r/90s, 1154:2x8r/90s, 307:2x10r/90s, 1103:2x10r/90s, 1093:2x60s/60s, 253:2x45s/60s]`
- s8 [HECHO,MOTIVACION,MOTIVACION] ADAPT_EXPANDED/APLICADA {INVESTIGAR_CAMBIO_MINIMO,INVESTIGAR_CAMBIO_MINIMO} · llamadas 2
  - antes: `20 28min [1114:2x10r/90s, 1154:2x8r/90s, 307:2x10r/90s, 1103:2x10r/90s, 1093:2x60s/60s, 253:2x45s/60s] 21 33min [1054:2x20s/180s, 1093:2x20s/180s, 1073:2x8r/180s@20kg, 115:2x8r/180s, 1096:2x8r/180s, 1102:2x10r/180s]`
  - después: `23 21min [1114:2x8r/60s, 1154:2x8r/60s, 307:2x8r/60s, 1103:2x10r/60s, 1093:2x45s/45s, 253:2x30s/45s] 24 26min [1054:2x20s/90s, 1093:2x20s/90s, 1073:2x8r/90s, 115:2x8r/90s, 1096:2x8r/90s, 1102:2x10r/90s]`
- s9 [HECHO,HECHO,DOLOR] ADAPT_LOCALIZED/NO_FACTIBLE {SEGURIDAD_ANTE_DOLOR} · llamadas 3 · motivo: Sin respuesta de la IA no se puede resolver el dolor de las sesiones [24]: conservarlas volveria a publicar el ejercicio que dolio.

### LOSE_WEIGHT/INTERMEDIATE/45/GYM

Inicial: AI, 1 llamadas, VALIDA

- s2 [HECHO,HECHO,FATIGA] ADAPT_LOCALIZED/APLICADA {REVISAR_CARGA_Y_RECUPERACION} · llamadas 2
  - antes: `27 51min [893:3x120s/60s, 253:3x120s/60s, 1096:3x6r/150s, 305:3x6r/150s, 553:3x6r/150s, 704:3x6r/150s]`
  - después: `30 41min [893:3x90s/60s, 253:3x90s/60s, 1096:3x6r/150s, 305:3x6r/150s, 553:3x6r/150s, 704:3x6r/150s]`
- s3 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s4 [HECHO,HECHO,TIEMPO] ADAPT_LOCALIZED/APLICADA {ACORTAR_O_REORGANIZAR} · llamadas 2
  - antes: `33 41min [893:3x90s/60s, 253:3x90s/60s, 1096:3x6r/150s, 305:3x6r/150s, 553:3x6r/150s, 704:3x6r/150s]`
  - después: `36 26min [893:2x60s/45s, 253:2x60s/45s, 1096:2x6r/120s, 305:2x6r/120s, 553:2x6r/120s, 704:2x6r/120s]`
- s5 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s6 [HECHO,HECHO,PARCIAL_CARGA] ADAPT_LOCALIZED/RESPALDO {REVISAR_CARGA_Y_RECUPERACION} · llamadas 3
  - antes: `39 26min [893:2x60s/45s, 253:2x60s/45s, 1096:2x6r/120s@20kg, 305:2x6r/120s, 553:2x6r/120s, 704:2x6r/120s]`
  - después: `42 26min [893:2x60s/45s, 253:2x60s/45s, 1096:2x6r/120s@20kg, 305:2x6r/120s, 553:2x6r/120s, 704:2x6r/120s]`
- s7 [HECHO,DIFICIL,HECHO] ADAPT_LOCALIZED/NO_FACTIBLE {VARIANTE_MAS_SENCILLA_O_CARGA} · llamadas 3 · motivo: V20: el entrenamiento del 2026-12-18 dura unos 26 minutos y el minimo es 31 (70 % de 45): le faltan unos 5 minutos. Agrega un ejercicio del enfoque o una serie a los que ya tiene, no descanso.

### LOSE_WEIGHT/ADVANCED/60/GYM

Inicial: AI, 1 llamadas, VALIDA

- s2 [HECHO,HECHO,FATIGA] ADAPT_LOCALIZED/APLICADA {REVISAR_CARGA_Y_RECUPERACION} · llamadas 1
  - antes: `45 65min [53:3x120s/60s, 1324:3x120s/60s, 1004:3x6r/150s, 1110:3x6r/150s, 548:3x6r/150s, 357:3x6r/150s, 704:3x6r/150s, 1225:3x10r/150s]`
  - después: `48 46min [53:3x90s/60s, 1324:3x90s/60s, 1004:3x6r/180s, 1110:2x6r/180s, 548:2x6r/180s, 357:2x6r/180s, 704:2x6r/180s, 1225:3x10r/180s]`
- s3 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s4 [HECHO,HECHO,TIEMPO] ADAPT_LOCALIZED/APLICADA {ACORTAR_O_REORGANIZAR} · llamadas 1
  - antes: `51 46min [53:3x90s/60s, 1324:3x90s/60s, 1004:3x6r/180s, 1110:2x6r/180s, 548:2x6r/180s, 357:2x6r/180s, 704:2x6r/180s, 1225:3x10r/180s]`
  - después: `54 36min [53:2x60s/45s, 1324:2x60s/45s, 1004:2x6r/120s, 1110:2x6r/120s, 548:2x6r/120s, 357:2x6r/120s, 704:2x6r/120s, 1225:2x10r/120s]`
- s5 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s6 [HECHO,HECHO,PARCIAL_CARGA] ADAPT_LOCALIZED/RESPALDO {REVISAR_CARGA_Y_RECUPERACION} · llamadas 3
  - antes: `57 36min [53:2x60s/45s, 1324:2x60s/45s, 1004:2x6r/120s@20kg, 1110:2x6r/120s, 548:2x6r/120s, 357:2x6r/120s, 704:2x6r/120s, 1225:2x10r/120s]`
  - después: `60 36min [53:2x60s/45s, 1324:2x60s/45s, 1004:2x6r/120s@20kg, 1110:2x6r/120s, 548:2x6r/120s, 357:2x6r/120s, 704:2x6r/120s, 1225:2x10r/120s]`
- s7 [HECHO,DIFICIL,HECHO] ADAPT_LOCALIZED/NO_FACTIBLE {VARIANTE_MAS_SENCILLA_O_CARGA} · llamadas 3 · motivo: V20: el entrenamiento del 2026-12-18 dura unos 34 minutos y el minimo es 42 (70 % de 60): le faltan unos 8 minutos. Agrega un ejercicio del enfoque o una serie a los que ya tiene, no descanso.

### GAIN_MUSCLE/BEGINNER/30/CASA

Inicial: AI, 1 llamadas, VALIDA

- s2 [HECHO,HECHO,FATIGA] ADAPT_LOCALIZED/SIN_EFECTO {REVISAR_CARGA_Y_RECUPERACION} · llamadas 1
  - antes: `63 27min [115:3x10r/90s, 122:3x10r/90s, 709:2x10r/90s, 726:2x10r/90s, 814:2x13r/90s]`
  - después: `66 27min [115:3x10r/90s, 122:3x10r/90s, 709:2x10r/90s, 726:2x10r/90s, 814:2x13r/90s]`
- s3 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s4 [HECHO,HECHO,TIEMPO] ADAPT_LOCALIZED/APLICADA {ACORTAR_O_REORGANIZAR} · llamadas 1
  - antes: `69 27min [115:3x10r/90s, 122:3x10r/90s, 709:2x10r/90s, 726:2x10r/90s, 814:2x13r/90s]`
  - después: `72 20min [115:3x10r/90s, 122:3x10r/90s, 709:2x10r/90s, 814:2x13r/90s]`
- s5 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s6 [HECHO,HECHO,PARCIAL_CARGA] ADAPT_LOCALIZED/APLICADA {REVISAR_CARGA_Y_RECUPERACION} · llamadas 1
  - antes: `75 20min [115:3x10r/90s@20kg, 122:3x10r/90s, 709:2x10r/90s, 814:2x13r/90s]`
  - después: `78 20min [115:3x10r/90s@18kg, 122:3x10r/90s, 709:2x10r/90s, 814:2x13r/90s]`
- s7 [HECHO,DIFICIL,HECHO] ADAPT_LOCALIZED/APLICADA {VARIANTE_MAS_SENCILLA_O_CARGA} · llamadas 1
  - antes: `77 27min [496:3x10r/90s, 939:3x10r/90s, 706:2x10r/90s, 723:2x10r/90s, 141:2x13r/90s]`
  - después: `80 22min [1097:3x10r/90s, 1076:3x10r/90s, 709:2x10r/90s, 141:2x13r/90s]`
- s8 [HECHO,MOTIVACION,MOTIVACION] ADAPT_EXPANDED/SIN_EFECTO {INVESTIGAR_CAMBIO_MINIMO,INVESTIGAR_CAMBIO_MINIMO} · llamadas 3
  - antes: `80 22min [1097:3x10r/90s, 1076:3x10r/90s, 709:2x10r/90s, 141:2x13r/90s] 81 20min [115:3x10r/90s@18kg, 122:3x10r/90s, 709:2x10r/90s, 814:2x13r/90s]`
  - después: `83 22min [1097:3x10r/90s, 1076:3x10r/90s, 709:2x10r/90s, 141:2x13r/90s] 84 20min [115:3x10r/90s, 122:3x10r/90s, 709:2x10r/90s, 814:2x13r/90s]`
- s9 [HECHO,HECHO,DOLOR] ADAPT_LOCALIZED/APLICADA {SEGURIDAD_ANTE_DOLOR} · llamadas 2
  - antes: `84 20min [115:3x10r/90s, 122:3x10r/90s, 709:2x10r/90s, 814:2x13r/90s]`
  - después: `87 21min [116:3x10r/90s, 121:3x10r/90s, 708:2x10r/90s, 815:2x13r/90s]`
- s10 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0

### GAIN_MUSCLE/INTERMEDIATE/45/GYM

Inicial: RULE_ENGINE, 3 llamadas, VALIDA

- s2 [HECHO,HECHO,FATIGA] ADAPT_LOCALIZED/SIN_EFECTO {REVISAR_CARGA_Y_RECUPERACION} · llamadas 1
  - antes: `93 33min [1099:3x10r/90s, 549:2x10r/90s, 1157:3x10r/90s, 743:3x10r/90s, 815:2x13r/90s, 421:3x10r/90s]`
  - después: `96 33min [1099:3x10r/90s, 549:2x10r/90s, 1157:3x10r/90s, 743:3x10r/90s, 815:2x13r/90s, 421:3x10r/90s]`
- s3 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s4 [HECHO,HECHO,TIEMPO] ADAPT_LOCALIZED/APLICADA {ACORTAR_O_REORGANIZAR} · llamadas 1
  - antes: `99 33min [1099:3x10r/90s, 549:2x10r/90s, 1157:3x10r/90s, 743:3x10r/90s, 815:2x13r/90s, 421:3x10r/90s]`
  - después: `102 28min [1099:3x10r/90s, 549:2x10r/90s, 1157:3x10r/90s, 743:3x10r/90s, 421:3x10r/90s]`
- s5 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s6 [HECHO,HECHO,PARCIAL_CARGA] ADAPT_LOCALIZED/RESPALDO {REVISAR_CARGA_Y_RECUPERACION} · llamadas 3
  - antes: `105 28min [1099:3x10r/90s@20kg, 549:2x10r/90s, 1157:3x10r/90s, 743:3x10r/90s, 421:3x10r/90s]`
  - después: `108 28min [1099:3x10r/90s@20kg, 549:2x10r/90s, 1157:3x10r/90s, 743:3x10r/90s, 421:3x10r/90s]`
- s7 [HECHO,DIFICIL,HECHO] ADAPT_LOCALIZED/NO_FACTIBLE {VARIANTE_MAS_SENCILLA_O_CARGA} · llamadas 3 · motivo: V20: el entrenamiento del 2026-12-18 dura unos 30 minutos y el minimo es 31 (70 % de 45): le faltan unos 1 minutos. Agrega un ejercicio del enfoque o una serie a los que ya tiene, no descanso.

### GAIN_MUSCLE/ADVANCED/60/GYM

Inicial: AI, 1 llamadas, VALIDA

- s2 [HECHO,HECHO,FATIGA] ADAPT_LOCALIZED/SIN_EFECTO {REVISAR_CARGA_Y_RECUPERACION} · llamadas 1
  - antes: `111 44min [625:3x10r/90s, 1099:3x10r/90s, 183:3x10r/90s, 554:3x10r/90s, 1037:3x10r/90s, 743:3x10r/90s, 1102:3x13r/90s]`
  - después: `114 44min [625:3x10r/90s, 1099:3x10r/90s, 183:3x10r/90s, 554:3x10r/90s, 1037:3x10r/90s, 743:3x10r/90s, 1102:3x13r/90s]`
- s3 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s4 [HECHO,HECHO,TIEMPO] ADAPT_LOCALIZED/SIN_EFECTO {ACORTAR_O_REORGANIZAR} · llamadas 1
  - antes: `117 44min [625:3x10r/90s, 1099:3x10r/90s, 183:3x10r/90s, 554:3x10r/90s, 1037:3x10r/90s, 743:3x10r/90s, 1102:3x13r/90s]`
  - después: `120 44min [625:3x10r/90s, 1099:3x10r/90s, 183:3x10r/90s, 554:3x10r/90s, 1037:3x10r/90s, 743:3x10r/90s, 1102:3x13r/90s]`
- s5 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s6 [HECHO,HECHO,PARCIAL_CARGA] ADAPT_LOCALIZED/APLICADA {REVISAR_CARGA_Y_RECUPERACION} · llamadas 1
  - antes: `123 44min [625:3x10r/90s@20kg, 1099:3x10r/90s, 183:3x10r/90s, 554:3x10r/90s, 1037:3x10r/90s, 743:3x10r/90s, 1102:3x13r/90s]`
  - después: `126 44min [625:3x10r/90s@18kg, 1099:3x10r/90s, 183:3x10r/90s, 554:3x10r/90s, 1037:3x10r/90s, 743:3x10r/90s, 1102:3x13r/90s]`
- s7 [HECHO,DIFICIL,HECHO] ADAPT_LOCALIZED/APLICADA {VARIANTE_MAS_SENCILLA_O_CARGA} · llamadas 1
  - antes: `125 44min [151:3x10r/90s, 102:3x10r/90s, 164:3x10r/90s, 785:3x10r/90s, 355:3x10r/90s, 1282:3x10r/90s, 1225:3x13r/90s]`
  - después: `128 45min [553:3x10r/90s, 989:3x10r/90s, 102:3x10r/90s, 785:3x10r/90s, 355:3x10r/90s, 1024:3x10r/90s, 1225:3x13r/90s]`
- s8 [HECHO,MOTIVACION,MOTIVACION] ADAPT_EXPANDED/SIN_EFECTO {INVESTIGAR_CAMBIO_MINIMO,INVESTIGAR_CAMBIO_MINIMO} · llamadas 2
  - antes: `128 45min [553:3x10r/90s, 989:3x10r/90s, 102:3x10r/90s, 785:3x10r/90s, 355:3x10r/90s, 1024:3x10r/90s, 1225:3x13r/90s] 129 44min [625:3x10r/90s@18kg, 1099:3x10r/90s, 183:3x10r/90s, 554:3x10r/90s, 1037:3x10r/90s, 743:3x10r/90s, 1102:3x13r/90s]`
  - después: `131 45min [553:3x10r/90s, 989:3x10r/90s, 102:3x10r/90s, 785:3x10r/90s, 355:3x10r/90s, 1024:3x10r/90s, 1225:3x13r/90s] 132 44min [625:3x10r/90s, 1099:3x10r/90s, 183:3x10r/90s, 554:3x10r/90s, 1037:3x10r/90s, 743:3x10r/90s, 1102:3x13r/90s]`
- s9 [HECHO,HECHO,DOLOR] ADAPT_LOCALIZED/APLICADA {SEGURIDAD_ANTE_DOLOR} · llamadas 1
  - antes: `132 44min [625:3x10r/90s, 1099:3x10r/90s, 183:3x10r/90s, 554:3x10r/90s, 1037:3x10r/90s, 743:3x10r/90s, 1102:3x13r/90s]`
  - después: `135 42min [182:3x10r/90s, 164:3x10r/90s, 151:3x10r/90s, 1039:3x10r/90s, 221:3x10r/90s, 236:3x10r/90s, 404:3x10r/90s]`
- s10 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0

### INCREASE_STRENGTH/BEGINNER/30/CASA

Inicial: AI, 1 llamadas, VALIDA

- s2 [HECHO,HECHO,FATIGA] ADAPT_LOCALIZED/APLICADA {REVISAR_CARGA_Y_RECUPERACION} · llamadas 1
  - antes: `141 24min [1156:2x8r/120s, 705:2x8r/120s, 145:2x8r/120s, 1073:2x8r/120s, 57:2x13r/90s]`
  - después: `144 30min [1156:2x8r/150s, 705:2x8r/150s, 145:2x8r/150s, 1073:2x8r/150s, 57:2x10r/120s]`
- s3 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s4 [HECHO,HECHO,TIEMPO] ADAPT_LOCALIZED/APLICADA {ACORTAR_O_REORGANIZAR} · llamadas 2
  - antes: `147 30min [1156:2x8r/150s, 705:2x8r/150s, 145:2x8r/150s, 1073:2x8r/150s, 57:2x10r/120s]`
  - después: `150 19min [145:2x8r/90s, 1156:2x8r/90s, 705:2x8r/90s, 1073:2x8r/60s, 57:2x10r/45s]`
- s5 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s6 [HECHO,HECHO,PARCIAL_CARGA] ADAPT_LOCALIZED/APLICADA {REVISAR_CARGA_Y_RECUPERACION} · llamadas 2
  - antes: `153 19min [145:2x8r/90s@20kg, 1156:2x8r/90s, 705:2x8r/90s, 1073:2x8r/60s, 57:2x10r/45s]`
  - después: `156 22min [145:2x8r/120s@18kg, 1156:2x8r/120s, 705:2x8r/120s, 1073:2x8r/90s, 57:2x10r/60s]`
- s7 [HECHO,DIFICIL,HECHO] ADAPT_LOCALIZED/APLICADA {VARIANTE_MAS_SENCILLA_O_CARGA} · llamadas 2
  - antes: `155 24min [117:2x8r/120s, 1096:2x8r/120s, 1265:2x8r/120s, 148:2x8r/120s, 501:2x13r/90s]`
  - después: `158 25min [114:3x8r/90s, 1097:2x8r/90s, 1114:2x8r/90s, 125:2x8r/90s, 1062:2x10r/60s]`
- s8 [HECHO,MOTIVACION,MOTIVACION] ADAPT_EXPANDED/RESPALDO {INVESTIGAR_CAMBIO_MINIMO,INVESTIGAR_CAMBIO_MINIMO} · llamadas 3
  - antes: `158 25min [114:3x8r/90s, 1097:2x8r/90s, 1114:2x8r/90s, 125:2x8r/90s, 1062:2x10r/60s] 159 22min [145:2x8r/120s@18kg, 1156:2x8r/120s, 705:2x8r/120s, 1073:2x8r/90s, 57:2x10r/60s]`
  - después: `161 25min [114:3x8r/90s, 1097:2x8r/90s, 1114:2x8r/90s, 125:2x8r/90s, 1062:2x10r/60s] 162 22min [145:2x8r/120s, 1156:2x8r/120s, 705:2x8r/120s, 1073:2x8r/90s, 57:2x10r/60s]`
- s9 [HECHO,HECHO,DOLOR] ADAPT_LOCALIZED/APLICADA {SEGURIDAD_ANTE_DOLOR} · llamadas 3
  - antes: `162 22min [145:2x8r/120s, 1156:2x8r/120s, 705:2x8r/120s, 1073:2x8r/90s, 57:2x10r/60s]`
  - después: `165 21min [116:2x8r/120s, 739:2x8r/120s, 791:2x8r/120s, 1157:2x8r/90s, 502:2x10r/60s]`
- s10 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0

### INCREASE_STRENGTH/INTERMEDIATE/45/GYM

Inicial: AI, 3 llamadas, VALIDA

- s2 [HECHO,HECHO,FATIGA] ADAPT_LOCALIZED/APLICADA {REVISAR_CARGA_Y_RECUPERACION} · llamadas 2
  - antes: `171 51min [755:3x6r/150s, 715:3x6r/150s, 177:3x6r/150s, 389:3x6r/150s, 547:3x6r/150s, 739:3x10r/90s, 872:3x10r/150s]`
  - después: `174 45min [755:3x6r/150s, 715:3x6r/150s, 177:3x6r/150s, 389:3x6r/120s, 547:2x6r/120s, 739:2x8r/90s, 872:2x10r/150s]`
- s3 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s4 [HECHO,HECHO,TIEMPO] ADAPT_LOCALIZED/APLICADA {ACORTAR_O_REORGANIZAR} · llamadas 2
  - antes: `177 45min [755:3x6r/150s, 715:3x6r/150s, 177:3x6r/150s, 389:3x6r/120s, 547:2x6r/120s, 739:2x8r/90s, 872:2x10r/150s]`
  - después: `180 28min [755:2x6r/120s, 715:2x6r/120s, 177:2x6r/120s, 389:2x6r/90s, 547:2x6r/90s, 739:2x6r/90s, 872:2x10r/120s]`
- s5 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s6 [HECHO,HECHO,PARCIAL_CARGA] ADAPT_LOCALIZED/APLICADA {REVISAR_CARGA_Y_RECUPERACION} · llamadas 1
  - antes: `183 28min [755:2x6r/120s@20kg, 715:2x6r/120s, 177:2x6r/120s, 389:2x6r/90s, 547:2x6r/90s, 739:2x6r/90s, 872:2x10r/120s]`
  - después: `186 33min [755:2x6r/150s@20kg, 715:2x6r/150s, 177:2x6r/150s, 389:2x6r/120s, 547:2x6r/120s, 739:2x6r/120s, 872:2x10r/150s]`
- s7 [HECHO,DIFICIL,HECHO] ADAPT_LOCALIZED/APLICADA {VARIANTE_MAS_SENCILLA_O_CARGA} · llamadas 1
  - antes: `185 48min [595:3x6r/150s, 553:3x6r/150s, 586:3x6r/150s, 390:3x6r/150s, 704:3x10r/90s, 1225:3x10r/150s, 422:2x10r/90s]`
  - después: `188 43min [1004:3x6r/120s, 877:3x6r/120s, 583:3x6r/120s, 74:3x6r/120s, 704:3x6r/90s, 1073:3x8r/90s, 342:2x10r/60s]`
- s8 [HECHO,MOTIVACION,MOTIVACION] ADAPT_EXPANDED/APLICADA {INVESTIGAR_CAMBIO_MINIMO,INVESTIGAR_CAMBIO_MINIMO} · llamadas 2
  - antes: `188 43min [1004:3x6r/120s, 877:3x6r/120s, 583:3x6r/120s, 74:3x6r/120s, 704:3x6r/90s, 1073:3x8r/90s, 342:2x10r/60s] 189 33min [755:2x6r/150s@20kg, 715:2x6r/150s, 177:2x6r/150s, 389:2x6r/120s, 547:2x6r/120s, 739:2x6r/120s, 872:2x10r/150s]`
  - después: `191 32min [1004:3x6r/120s, 877:2x6r/120s, 583:2x6r/120s, 74:3x6r/120s, 704:2x6r/90s, 1073:2x8r/90s, 342:2x10r/60s] 192 33min [755:2x6r/150s, 715:2x6r/150s, 177:2x6r/150s, 389:2x6r/120s, 547:2x6r/120s, 739:2x6r/120s, 872:2x10r/150s]`
- s9 [HECHO,HECHO,DOLOR] ADAPT_LOCALIZED/APLICADA {SEGURIDAD_ANTE_DOLOR} · llamadas 3
  - antes: `192 33min [755:2x6r/150s, 715:2x6r/150s, 177:2x6r/150s, 389:2x6r/120s, 547:2x6r/120s, 739:2x6r/120s, 872:2x10r/150s]`
  - después: `195 40min [553:3x6r/150s, 519:3x6r/150s, 517:3x6r/150s, 485:3x6r/120s, 514:2x6r/120s]`
- s10 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0

### INCREASE_STRENGTH/ADVANCED/60/GYM

Inicial: AI, 1 llamadas, VALIDA

- s2 [HECHO,HECHO,FATIGA] ADAPT_LOCALIZED/APLICADA {REVISAR_CARGA_Y_RECUPERACION} · llamadas 1
  - antes: `201 42min [195:3x6r/150s, 235:3x6r/150s, 151:3x6r/150s, 435:3x6r/150s, 975:2x13r/90s, 539:2x10r/90s, 545:2x10r/90s]`
  - después: `204 42min [195:3x6r/180s, 235:3x6r/180s, 151:3x6r/180s, 435:3x6r/180s, 975:2x10r/90s, 539:2x8r/120s, 545:2x8r/120s]`
- s3 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s4 [HECHO,HECHO,TIEMPO] ADAPT_LOCALIZED/APLICADA {ACORTAR_O_REORGANIZAR} · llamadas 1
  - antes: `207 42min [195:3x6r/180s, 235:3x6r/180s, 151:3x6r/180s, 435:3x6r/180s, 975:2x10r/90s, 539:2x8r/120s, 545:2x8r/120s]`
  - después: `210 30min [195:3x6r/150s, 235:3x6r/150s, 151:3x6r/150s, 435:2x6r/120s, 975:2x10r/60s, 539:2x8r/60s, 545:2x8r/60s]`
- s5 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s6 [HECHO,HECHO,PARCIAL_CARGA] ADAPT_LOCALIZED/RESPALDO {REVISAR_CARGA_Y_RECUPERACION} · llamadas 3
  - antes: `213 30min [195:3x6r/150s@20kg, 235:3x6r/150s, 151:3x6r/150s, 435:2x6r/120s, 975:2x10r/60s, 539:2x8r/60s, 545:2x8r/60s]`
  - después: `216 30min [195:3x6r/150s@20kg, 235:3x6r/150s, 151:3x6r/150s, 435:2x6r/120s, 975:2x10r/60s, 539:2x8r/60s, 545:2x8r/60s]`
- s7 [HECHO,DIFICIL,HECHO] ADAPT_LOCALIZED/NO_FACTIBLE {VARIANTE_MAS_SENCILLA_O_CARGA} · llamadas 3 · motivo: V20: el entrenamiento del 2026-12-18 dura unos 37 minutos y el minimo es 42 (70 % de 60): le faltan unos 5 minutos. Agrega un ejercicio del enfoque o una serie a los que ya tiene, no descanso.

### GENERAL_FITNESS/BEGINNER/30/CASA

Inicial: AI, 1 llamadas, VALIDA

- s2 [HECHO,HECHO,FATIGA] ADAPT_LOCALIZED/SIN_EFECTO {REVISAR_CARGA_Y_RECUPERACION} · llamadas 2
  - antes: `219 30min [1093:3x90s/60s, 978:2x30s/15s, 117:2x8r/120s, 1104:3x40s/45s, 1264:2x10r/90s, 1099:2x10r/90s]`
  - después: `222 30min [1093:3x90s/60s, 978:2x30s/15s, 117:2x8r/120s, 1104:3x40s/45s, 1264:2x10r/90s, 1099:2x10r/90s]`
- s3 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s4 [HECHO,HECHO,TIEMPO] ADAPT_LOCALIZED/APLICADA {ACORTAR_O_REORGANIZAR} · llamadas 2
  - antes: `225 30min [1093:3x90s/60s, 978:2x30s/15s, 117:2x8r/120s, 1104:3x40s/45s, 1264:2x10r/90s, 1099:2x10r/90s]`
  - después: `228 22min [1093:2x60s/60s, 978:2x30s/15s, 117:2x8r/120s, 1104:2x30s/45s, 1264:2x8r/90s, 1099:2x8r/90s]`
- s5 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s6 [HECHO,HECHO,PARCIAL_CARGA] ADAPT_LOCALIZED/APLICADA {REVISAR_CARGA_Y_RECUPERACION} · llamadas 1
  - antes: `231 22min [1093:2x60s/60s, 978:2x30s/15s, 117:2x8r/120s@20kg, 1104:2x30s/45s, 1264:2x8r/90s, 1099:2x8r/90s]`
  - después: `234 21min [1093:-x60s/180s, 978:-x30s/45s, 117:2x8r/180s@18kg, 1104:-x30s/60s, 1264:2x8r/180s, 1099:2x8r/180s]`
- s7 [HECHO,DIFICIL,HECHO] ADAPT_LOCALIZED/APLICADA {VARIANTE_MAS_SENCILLA_O_CARGA} · llamadas 2
  - antes: `233 29min [1054:3x90s/60s, 1193:2x30s/15s, 121:2x8r/120s, 299:2x15r/45s, 1155:2x10r/90s, 706:2x10r/90s]`
  - después: `236 24min [1054:2x45s/60s, 1193:2x30s/15s, 121:2x8r/120s, 299:2x12r/45s, 1155:2x10r/90s, 706:2x10r/90s]`
- s8 [HECHO,MOTIVACION,MOTIVACION] ADAPT_EXPANDED/APLICADA {INVESTIGAR_CAMBIO_MINIMO,INVESTIGAR_CAMBIO_MINIMO} · llamadas 3
  - antes: `236 24min [1054:2x45s/60s, 1193:2x30s/15s, 121:2x8r/120s, 299:2x12r/45s, 1155:2x10r/90s, 706:2x10r/90s] 237 21min [1093:-x60s/180s, 978:-x30s/45s, 117:2x8r/180s@18kg, 1104:-x30s/60s, 1264:2x8r/180s, 1099:2x8r/180s]`
  - después: `239 24min [1054:2x45s/60s, 1193:2x30s/15s, 121:2x8r/120s, 299:2x12r/45s, 1155:2x10r/90s, 706:2x10r/90s] 240 32min [1093:2x60s/180s, 978:2x30s/45s, 117:2x8r/180s, 1104:2x30s/60s, 1264:2x8r/180s, 1099:2x8r/180s]`
- s9 [HECHO,HECHO,DOLOR] ADAPT_LOCALIZED/NO_FACTIBLE {SEGURIDAD_ANTE_DOLOR} · llamadas 3 · motivo: Sin respuesta de la IA no se puede resolver el dolor de las sesiones [240]: conservarlas volveria a publicar el ejercicio que dolio.

### GENERAL_FITNESS/INTERMEDIATE/45/GYM

Inicial: AI, 2 llamadas, VALIDA

- s2 [HECHO,HECHO,FATIGA] ADAPT_LOCALIZED/APLICADA {REVISAR_CARGA_Y_RECUPERACION} · llamadas 2
  - antes: `243 36min [1325:3x120s/60s, 1075:2x30s/15s, 1022:3x6r/150s, 859:3x40s/45s, 592:3x10r/90s, 294:2x13r/90s]`
  - después: `246 42min [1325:3x120s/90s, 1075:2x30s/30s, 1022:3x6r/180s, 859:3x40s/75s, 592:3x10r/135s, 294:2x13r/120s]`
- s3 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s4 [HECHO,HECHO,TIEMPO] ADAPT_LOCALIZED/RESPALDO {ACORTAR_O_REORGANIZAR} · llamadas 3
  - antes: `249 42min [1325:3x120s/90s, 1075:2x30s/30s, 1022:3x6r/180s, 859:3x40s/75s, 592:3x10r/135s, 294:2x13r/120s]`
  - después: `252 42min [1325:3x120s/90s, 1075:2x30s/30s, 1022:3x6r/180s, 859:3x40s/75s, 592:3x10r/135s, 294:2x13r/120s]`
- s5 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s6 [HECHO,HECHO,PARCIAL_CARGA] ADAPT_LOCALIZED/APLICADA {REVISAR_CARGA_Y_RECUPERACION} · llamadas 2
  - antes: `255 42min [1325:3x120s/90s, 1075:2x30s/30s, 1022:3x6r/180s@20kg, 859:3x40s/75s, 592:3x10r/135s, 294:2x13r/120s]`
  - después: `258 32min [1325:3x120s/90s, 1075:2x30s/30s, 1022:3x6r/180s@20kg, 859:2x40s/75s, 592:3x10r/135s, 294:2x13r/120s]`
- s7 [HECHO,DIFICIL,HECHO] ADAPT_LOCALIZED/APLICADA {VARIANTE_MAS_SENCILLA_O_CARGA} · llamadas 2
  - antes: `257 36min [1324:3x120s/60s, 1190:2x30s/15s, 1037:3x6r/150s, 1140:3x40s/45s, 755:3x10r/90s, 429:2x13r/90s]`
  - después: `260 33min [1324:3x90s/45s, 1190:2x20s/15s, 336:3x8r/120s, 1140:3x30s/45s, 791:3x10r/90s, 935:3x12r/60s]`
- s8 [HECHO,MOTIVACION,MOTIVACION] ADAPT_EXPANDED/RESPALDO {INVESTIGAR_CAMBIO_MINIMO,INVESTIGAR_CAMBIO_MINIMO} · llamadas 3
  - antes: `260 33min [1324:3x90s/45s, 1190:2x20s/15s, 336:3x8r/120s, 1140:3x30s/45s, 791:3x10r/90s, 935:3x12r/60s] 261 32min [1325:3x120s/90s, 1075:2x30s/30s, 1022:3x6r/180s@20kg, 859:2x40s/75s, 592:3x10r/135s, 294:2x13r/120s]`
  - después: `263 33min [1324:3x90s/45s, 1190:2x20s/15s, 336:3x8r/120s, 1140:3x30s/45s, 791:3x10r/90s, 935:3x12r/60s] 264 32min [1325:3x120s/90s, 1075:2x30s/30s, 1022:3x6r/180s, 859:2x40s/75s, 592:3x10r/135s, 294:2x13r/120s]`
- s9 [HECHO,HECHO,DOLOR] ADAPT_LOCALIZED/NO_FACTIBLE {SEGURIDAD_ANTE_DOLOR} · llamadas 3 · motivo: Sin respuesta de la IA no se puede resolver el dolor de las sesiones [264]: conservarlas volveria a publicar el ejercicio que dolio.

### GENERAL_FITNESS/ADVANCED/60/GYM

Inicial: AI, 1 llamadas, VALIDA

- s2 [HECHO,HECHO,FATIGA] ADAPT_LOCALIZED/APLICADA {REVISAR_CARGA_Y_RECUPERACION} · llamadas 1
  - antes: `267 50min [179:3x6r/150s, 1104:3x40s/45s, 532:3x120s/60s, 1188:2x30s/15s, 710:3x10r/90s, 180:3x10r/90s, 783:3x10r/90s, 553:3x10r/90s]`
  - después: `270 48min [179:3x6r/180s, 1104:3x30s/45s, 532:3x120s/60s, 1188:2x30s/15s, 710:3x8r/120s, 180:3x8r/120s, 783:3x8r/120s, 553:3x8r/120s]`
- s3 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s4 [HECHO,HECHO,TIEMPO] ADAPT_LOCALIZED/APLICADA {ACORTAR_O_REORGANIZAR} · llamadas 2
  - antes: `273 48min [179:3x6r/180s, 1104:3x30s/45s, 532:3x120s/60s, 1188:2x30s/15s, 710:3x8r/120s, 180:3x8r/120s, 783:3x8r/120s, 553:3x8r/120s]`
  - después: `276 28min [179:2x6r/90s, 1104:2x30s/45s, 532:2x120s/60s, 1188:2x30s/15s, 710:2x8r/90s, 180:2x8r/90s, 783:2x8r/90s, 553:2x8r/90s]`
- s5 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s6 [HECHO,HECHO,PARCIAL_CARGA] ADAPT_LOCALIZED/RESPALDO {REVISAR_CARGA_Y_RECUPERACION} · llamadas 3
  - antes: `279 28min [179:2x6r/90s@20kg, 1104:2x30s/45s, 532:2x120s/60s, 1188:2x30s/15s, 710:2x8r/90s, 180:2x8r/90s, 783:2x8r/90s, 553:2x8r/90s]`
  - después: `282 28min [179:2x6r/90s@20kg, 1104:2x30s/45s, 532:2x120s/60s, 1188:2x30s/15s, 710:2x8r/90s, 180:2x8r/90s, 783:2x8r/90s, 553:2x8r/90s]`
- s7 [HECHO,DIFICIL,HECHO] ADAPT_LOCALIZED/NO_FACTIBLE {VARIANTE_MAS_SENCILLA_O_CARGA} · llamadas 3 · motivo: V20: el entrenamiento del 2026-12-18 dura unos 31 minutos y el minimo es 42 (70 % de 60): le faltan unos 11 minutos. Agrega un ejercicio del enfoque o una serie a los que ya tiene, no descanso.


## Integridad V31–V34

Consulta de linaje: 0 violaciones.
