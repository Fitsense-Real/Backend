# Campaña con Replicate real · modo adaptacion

Duración 28 min · 12 perfiles · 132 llamadas reales · 132 predicciones con métricas · tokens 6616416 de entrada y 569192 de salida · **coste estimado US$ 2.79** (a 0.25 / 2.0 US$ por millón; la cifra oficial es la del panel de Replicate)

## Generación inicial

| Objetivo | Perfiles | Origen IA | Origen reglas (respaldo) | Llamadas | Válida V24+V25 | Rechazos por regla (intentos fallidos) |
|---|---:|---:|---:|---:|---:|---|
| LOSE_WEIGHT | 3 | 3 | 0 | 3 | 3 | — |
| GAIN_MUSCLE | 3 | 2 | 1 | 6 | 3 | V20×3, V24×4 |
| INCREASE_STRENGTH | 3 | 3 | 0 | 3 | 3 | — |
| GENERAL_FITNESS | 3 | 3 | 0 | 7 | 3 | V23×4, V24×2 |

## Ciclos semanales

| Objetivo | Ciclos | Con IA | Llamadas | APLICADA | SIN_EFECTO | RESPALDO | RECHAZADA | NO_FACTIBLE | SIN_ORDEN | Con reintento | Pasos (prog/recup/reinc/vuelta) | Rechazos por regla (intentos fallidos) | Violaciones |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|---|---:|
| LOSE_WEIGHT | 25 | 17 | 30 | 15 | 0 | 1 | 0 | 1 | 8 | 9 | 0/0/0/0 | V11×2, V17×11, V20×5, V8C×4 | 29 |
| GAIN_MUSCLE | 25 | 17 | 23 | 10 | 6 | 0 | 0 | 1 | 8 | 4 | 0/0/0/0 | V11×3, V17×2, V20×1, V24×9, V8C×2 | 0 |
| INCREASE_STRENGTH | 27 | 18 | 33 | 18 | 0 | 0 | 0 | 0 | 9 | 14 | 0/0/0/0 | V11×3, V17×17, V20×6, V8C×2 | 19 |
| GENERAL_FITNESS | 20 | 14 | 27 | 12 | 0 | 1 | 0 | 0 | 7 | 10 | 0/0/0/0 | V11×2, V17×14, V20×4, V8C×1 | 18 |

## Por intervención (ciclos con IA)

| Intervención | Ciclos | APLICADA | SIN_EFECTO | RESPALDO | NO_FACTIBLE/RECHAZADA | Llamadas |
|---|---:|---:|---:|---:|---:|---:|
| {ACORTAR_O_REORGANIZAR} | 11 | 9 | 2 | 0 | 0 | 17 |
| {INVESTIGAR_CAMBIO_MINIMO,INVESTIGAR_CAMBIO_MINIMO} | 12 | 9 | 1 | 0 | 2 | 28 |
| {REVISAR_CARGA_Y_RECUPERACION} | 23 | 20 | 3 | 0 | 0 | 34 |
| {SEGURIDAD_ANTE_DOLOR} | 8 | 8 | 0 | 0 | 0 | 11 |
| {VARIANTE_MAS_SENCILLA_O_CARGA} | 11 | 9 | 0 | 2 | 0 | 21 |
| {} | 1 | 0 | 0 | 0 | 0 | 2 |

Latencia por llamada: media 40 s, máxima 103 s. Llamadas con error del proveedor: 0.

## Violaciones de las comprobaciones independientes (18)

- LOSE_WEIGHT/INTERMEDIATE/45/GYM s4: [dosis subida en la sesion senalada 33: 755:3x6r/150s -> 755:2x8r/120s, dosis subida en la sesion senalada 33: 592:3x6r/150s -> 592:2x8r/120s, dosis subida en la sesion senalada 33: 515:3x6r/150s -> 515:2x8r/120s]
- LOSE_WEIGHT/INTERMEDIATE/45/GYM s6: [dosis subida en la sesion senalada 48: 755:2x8r/120s@20kg -> 755:3x8r/150s@20kg, dosis subida en la sesion senalada 48: 592:2x8r/120s -> 592:3x8r/120s, dosis subida en la sesion senalada 48: 1225:2x10r/120s -> 1225:3x10r/150s]
- LOSE_WEIGHT/INTERMEDIATE/45/GYM s7: [dosis subida en la sesion senalada 68: 1324:3x120s/60s -> 1324:2x180s/60s, dosis subida en la sesion senalada 68: 1133:3x120s/60s -> 1133:2x180s/60s, dosis subida en la sesion senalada 68: 548:3x6r/150s -> 548:2x8r/90s, dosis subida en la sesion senalada 68: 1103:3x10r/150s -> 1103:2x12r/90s]
- LOSE_WEIGHT/ADVANCED/60/GYM s2: [dosis subida en la sesion senalada 3: 1096:3x6r/150s -> 1096:2x8r/180s, dosis subida en la sesion senalada 3: 389:3x6r/150s -> 389:2x8r/180s, dosis subida en la sesion senalada 3: 553:3x6r/150s -> 553:2x8r/180s, dosis subida en la sesion senalada 3: 783:3x6r/150s -> 783:2x8r/180s, dosis subida en la sesion senalada 3: 517:3x6r/150s -> 517:2x8r/180s]
- LOSE_WEIGHT/ADVANCED/60/GYM s6: [dosis subida en la sesion senalada 30: 1135:2x60s/30s -> 1135:3x60s/30s, dosis subida en la sesion senalada 30: 1293:2x60s/30s -> 1293:3x60s/30s, dosis subida en la sesion senalada 30: 389:2x6r/120s -> 389:3x6r/150s, dosis subida en la sesion senalada 30: 553:2x6r/120s -> 553:4x6r/150s, dosis subida en la sesion senalada 30: 783:2x6r/120s -> 783:3x6r/150s, dosis subida en la sesion senalada 30: 1102:2x10r/90s -> 1102:3x10r/120s, dosis subida en la sesion senalada 30: 517:2x6r/120s -> 517:3x6r/150s]
- LOSE_WEIGHT/ADVANCED/60/GYM s7: [dosis subida en la sesion senalada 71: 1092:3x6r/150s -> 1092:3x8r/90s, dosis subida en la sesion senalada 71: 357:3x6r/150s -> 357:3x8r/90s, dosis subida en la sesion senalada 71: 554:3x6r/150s -> 554:3x8r/90s, dosis subida en la sesion senalada 71: 704:3x6r/150s -> 704:3x8r/90s, dosis subida en la sesion senalada 71: 872:3x10r/150s -> 872:3x12r/60s, dosis subida en la sesion senalada 71: 1111:3x6r/150s -> 1111:3x10r/60s]
- LOSE_WEIGHT/ADVANCED/60/GYM s8: [dosis subida en la sesion senalada 78: 1096:2x6r/150s@20kg -> 1096:3x6r/150s]
- INCREASE_STRENGTH/INTERMEDIATE/45/GYM s6: [dosis subida en la sesion senalada 156: 418:2x6r/120s@20kg -> 418:3x6r/150s@18kg, dosis subida en la sesion senalada 156: 554:2x6r/120s -> 554:3x6r/150s, dosis subida en la sesion senalada 156: 788:2x6r/120s -> 788:3x6r/150s]
- INCREASE_STRENGTH/INTERMEDIATE/45/GYM s8: [dosis subida en la sesion senalada 194: 992:3x6r/150s -> 992:4x6r/150s, dosis subida en la sesion senalada 194: 1003:3x6r/150s -> 1003:4x6r/150s, dosis subida en la sesion senalada 195: 418:3x6r/150s@18kg -> 418:4x6r/150s, dosis subida en la sesion senalada 195: 780:2x6r/150s -> 780:3x6r/150s]
- INCREASE_STRENGTH/ADVANCED/60/GYM s2: [dosis subida en la sesion senalada 204: 183:3x6r/150s -> 183:4x6r/180s, dosis subida en la sesion senalada 204: 151:3x6r/150s -> 151:4x6r/180s, dosis subida en la sesion senalada 204: 474:2x10r/90s -> 474:3x10r/90s]
- INCREASE_STRENGTH/ADVANCED/60/GYM s6: [dosis subida en la sesion senalada 237: 183:3x6r/150s@20kg -> 183:6x6r/180s@20kg, dosis subida en la sesion senalada 237: 503:2x12r/60s -> 503:4x12r/60s]
- INCREASE_STRENGTH/ADVANCED/60/GYM s7: [dosis subida en la sesion senalada 257: 975:2x13r/90s -> 975:3x13r/90s, dosis subida en la sesion senalada 257: 577:2x10r/90s -> 577:3x10r/90s]
- INCREASE_STRENGTH/ADVANCED/60/GYM s8: [dosis subida en la sesion senalada 273: 186:2x6r/180s -> 186:3x6r/180s, dosis subida en la sesion senalada 273: 151:2x6r/180s -> 151:3x6r/180s, dosis subida en la sesion senalada 273: 435:2x6r/180s -> 435:3x6r/180s, dosis subida en la sesion senalada 273: 474:2x10r/120s -> 474:3x10r/120s, dosis subida en la sesion senalada 273: 577:2x10r/120s -> 577:3x10r/120s]
- GENERAL_FITNESS/BEGINNER/30/CASA s2: [dosis subida en la sesion senalada 240: 147:2x8r/120s -> 147:3x8r/120s, dosis subida en la sesion senalada 240: 1102:2x15r/45s -> 1102:3x10r/45s, dosis subida en la sesion senalada 240: 1157:2x10r/90s -> 1157:3x8r/90s]
- GENERAL_FITNESS/BEGINNER/30/CASA s9: [ai_calls V32 = 2, llamadas contadas = 3]
- GENERAL_FITNESS/INTERMEDIATE/45/GYM s4: [ai_calls V32 = 0, llamadas contadas = 2, ciclo determinista con 2 llamadas a la IA]
- GENERAL_FITNESS/ADVANCED/60/GYM s6: [dosis subida en la sesion senalada 306: 1104:2x40s/60s -> 1104:3x30s/60s, dosis subida en la sesion senalada 306: 432:2x10r/180s -> 432:3x8r/180s, dosis subida en la sesion senalada 306: 539:2x10r/90s -> 539:3x10r/90s, dosis subida en la sesion senalada 306: 976:2x13r/90s -> 976:3x13r/90s]
- GENERAL_FITNESS/ADVANCED/60/GYM s8: [dosis subida en la sesion senalada 311: 446:3x18r/45s -> 446:4x18r/45s, dosis subida en la sesion senalada 311: 553:3x10r/90s -> 553:4x10r/90s, dosis subida en la sesion senalada 311: 609:3x10r/90s -> 609:4x10r/90s, dosis subida en la sesion senalada 311: 512:3x10r/90s -> 512:4x10r/90s, dosis subida en la sesion senalada 311: 1187:2x13r/90s -> 1187:4x13r/90s, dosis subida en la sesion senalada 312: 533:2x60s/90s -> 533:3x60s/60s, dosis subida en la sesion senalada 312: 164:2x6r/180s@20kg -> 164:3x6r/180s, dosis subida en la sesion senalada 312: 710:2x6r/180s -> 710:3x6r/180s]

## Hallazgos cuantificados (no son violaciones de una regla vigente)

- APLICADA_SOLO_POR_MINUTOS: 1 ciclos
- ANADE_MAS_DE_LO_QUE_RETIRA: 0 ciclos
  - INCREASE_STRENGTH/INTERMEDIATE/45/GYM s2 {REVISAR_CARGA_Y_RECUPERACION}: [APLICADA_SOLO_POR_MINUTOS sesion 132: 36 -> 32 min]

## Perfiles interrumpidos (4)

- LOSE_WEIGHT/INTERMEDIATE/45/GYM: sin semana publicada en la 8
- GAIN_MUSCLE/BEGINNER/30/CASA: sin semana publicada en la 8
- GENERAL_FITNESS/BEGINNER/30/CASA: sin semana publicada en la 9
- GENERAL_FITNESS/INTERMEDIATE/45/GYM: sin semana publicada en la 4

## Detalle por perfil

### LOSE_WEIGHT/BEGINNER/30/CASA

Inicial: AI, 1 llamadas, VALIDA

- s2 [HECHO,HECHO,FATIGA] ADAPT_LOCALIZED/APLICADA {REVISAR_CARGA_Y_RECUPERACION} · llamadas 1
  - antes: `6 34min [1054:3x90s/60s, 1093:3x90s/60s, 1156:2x8r/120s, 1262:2x8r/120s, 933:2x8r/120s, 705:2x8r/120s]`
  - después: `9 28min [1054:3x60s/75s, 1093:3x60s/75s, 1156:2x8r/150s, 1262:2x8r/150s, 933:2x8r/150s, 705:2x8r/150s]`
- s3 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s4 [HECHO,HECHO,TIEMPO] ADAPT_LOCALIZED/APLICADA {ACORTAR_O_REORGANIZAR} · llamadas 2
  - antes: `15 28min [1054:3x60s/75s, 1093:3x60s/75s, 1156:2x8r/150s, 1262:2x8r/150s, 933:2x8r/150s, 705:2x8r/150s]`
  - después: `36 25min [1054:2x40s/60s, 1093:2x40s/60s, 1156:2x8r/90s, 1262:2x8r/90s, 933:2x8r/90s, 705:2x8r/90s]`
- s5 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s6 [HECHO,HECHO,PARCIAL_CARGA] ADAPT_LOCALIZED/APLICADA {REVISAR_CARGA_Y_RECUPERACION} · llamadas 2
  - antes: `39 25min [1054:2x40s/60s, 1093:2x40s/60s, 1156:2x8r/90s@20kg, 1262:2x8r/90s, 933:2x8r/90s, 705:2x8r/90s]`
  - después: `63 32min [1054:2x40s/120s, 1093:2x40s/120s, 1156:2x8r/180s@20kg, 1262:2x8r/180s, 933:2x8r/180s, 705:2x8r/180s]`
- s7 [HECHO,DIFICIL,HECHO] ADAPT_LOCALIZED/RESPALDO {VARIANTE_MAS_SENCILLA_O_CARGA} · llamadas 3
  - antes: `62 34min [1093:3x90s/60s, 253:3x90s/60s, 101:2x8r/120s, 1118:2x8r/120s, 896:2x8r/120s, 872:2x10r/120s]`
  - después: `83 34min [1093:3x90s/60s, 253:3x90s/60s, 101:2x8r/120s, 1118:2x8r/120s, 896:2x8r/120s, 872:2x10r/120s]`
- s8 [HECHO,MOTIVACION,MOTIVACION] ADAPT_EXPANDED/APLICADA {INVESTIGAR_CAMBIO_MINIMO,INVESTIGAR_CAMBIO_MINIMO} · llamadas 2
  - antes: `83 34min [1093:3x90s/60s, 253:3x90s/60s, 101:2x8r/120s, 1118:2x8r/120s, 896:2x8r/120s, 872:2x10r/120s] 84 32min [1054:2x40s/120s, 1093:2x40s/120s, 1156:2x8r/180s@20kg, 1262:2x8r/180s, 933:2x8r/180s, 705:2x8r/180s]`
  - después: `89 33min [1093:3x75s/60s, 253:3x75s/60s, 101:2x8r/120s, 1118:2x8r/120s, 896:2x8r/120s, 872:2x10r/120s] 90 26min [1054:2x30s/90s, 1093:2x30s/90s, 1156:2x8r/180s, 1262:2x8r/180s, 933:2x8r/180s, 705:2x8r/180s]`
- s9 [HECHO,HECHO,DOLOR] ADAPT_LOCALIZED/APLICADA {SEGURIDAD_ANTE_DOLOR} · llamadas 1
  - antes: `90 26min [1054:2x30s/90s, 1093:2x30s/90s, 1156:2x8r/180s, 1262:2x8r/180s, 933:2x8r/180s, 705:2x8r/180s]`
  - después: `102 30min [739:2x10r/90s, 119:2x10r/90s, 791:2x12r/90s, 1073:2x12r/90s, 1097:2x10r/90s, 253:2x45s/60s]`
- s10 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0

### LOSE_WEIGHT/INTERMEDIATE/45/GYM

Inicial: AI, 1 llamadas, VALIDA

- s2 [HECHO,HECHO,FATIGA] ADAPT_LOCALIZED/APLICADA {REVISAR_CARGA_Y_RECUPERACION} · llamadas 1
  - antes: `12 51min [1176:3x120s/60s, 1054:3x120s/60s, 755:3x6r/150s, 592:3x6r/150s, 515:3x6r/150s, 1225:3x10r/150s]`
  - después: `27 41min [1176:3x90s/60s, 1054:3x90s/60s, 755:3x6r/150s, 592:3x6r/150s, 515:3x6r/150s, 1225:2x10r/150s]`
- s3 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s4 [HECHO,HECHO,TIEMPO] ADAPT_LOCALIZED/APLICADA {ACORTAR_O_REORGANIZAR} · llamadas 1
  - antes: `33 41min [1176:3x90s/60s, 1054:3x90s/60s, 755:3x6r/150s, 592:3x6r/150s, 515:3x6r/150s, 1225:2x10r/150s]`
  - después: `45 30min [1176:2x60s/60s, 1054:2x60s/60s, 755:2x8r/120s, 592:2x8r/120s, 515:2x8r/120s, 1225:2x10r/120s]`
  - **violaciones:** [dosis subida en la sesion senalada 33: 755:3x6r/150s -> 755:2x8r/120s, dosis subida en la sesion senalada 33: 592:3x6r/150s -> 592:2x8r/120s, dosis subida en la sesion senalada 33: 515:3x6r/150s -> 515:2x8r/120s]
- s5 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s6 [HECHO,HECHO,PARCIAL_CARGA] ADAPT_LOCALIZED/APLICADA {REVISAR_CARGA_Y_RECUPERACION} · llamadas 2
  - antes: `48 30min [1176:2x60s/60s, 1054:2x60s/60s, 755:2x8r/120s@20kg, 592:2x8r/120s, 515:2x8r/120s, 1225:2x10r/120s]`
  - después: `69 33min [1176:2x45s/60s, 1054:2x45s/60s, 755:3x8r/150s@20kg, 592:3x8r/120s, 515:2x6r/120s, 1225:3x10r/150s]`
  - **violaciones:** [dosis subida en la sesion senalada 48: 755:2x8r/120s@20kg -> 755:3x8r/150s@20kg, dosis subida en la sesion senalada 48: 592:2x8r/120s -> 592:3x8r/120s, dosis subida en la sesion senalada 48: 1225:2x10r/120s -> 1225:3x10r/150s]
- s7 [HECHO,DIFICIL,HECHO] ADAPT_LOCALIZED/APLICADA {VARIANTE_MAS_SENCILLA_O_CARGA} · llamadas 3
  - antes: `68 51min [1324:3x120s/60s, 1133:3x120s/60s, 553:3x6r/150s, 518:3x6r/150s, 548:3x6r/150s, 1103:3x10r/150s]`
  - después: `80 36min [1324:2x180s/60s, 1133:2x180s/60s, 148:2x8r/90s, 433:2x8r/90s, 548:2x8r/90s, 1103:2x12r/90s]`
  - **violaciones:** [dosis subida en la sesion senalada 68: 1324:3x120s/60s -> 1324:2x180s/60s, dosis subida en la sesion senalada 68: 1133:3x120s/60s -> 1133:2x180s/60s, dosis subida en la sesion senalada 68: 548:3x6r/150s -> 548:2x8r/90s, dosis subida en la sesion senalada 68: 1103:3x10r/150s -> 1103:2x12r/90s]
- s8 [HECHO,MOTIVACION,MOTIVACION] ADAPT_EXPANDED/NO_FACTIBLE {INVESTIGAR_CAMBIO_MINIMO,INVESTIGAR_CAMBIO_MINIMO} · llamadas 3 · motivo: V11: el ejercicio 755 sugiere 20.00 kg y la persona no anoto peso la semana anterior; debe ir en null.

### LOSE_WEIGHT/ADVANCED/60/GYM

Inicial: AI, 1 llamadas, VALIDA

- s2 [HECHO,HECHO,FATIGA] ADAPT_LOCALIZED/APLICADA {REVISAR_CARGA_Y_RECUPERACION} · llamadas 1
  - antes: `3 64min [1135:3x120s/60s, 1293:3x120s/60s, 1096:3x6r/150s, 389:3x6r/150s, 553:3x6r/150s, 783:3x6r/150s, 1102:3x10r/150s, 517:3x6r/150s]`
  - después: `18 46min [1135:3x90s/60s, 1293:3x90s/60s, 1096:2x8r/180s, 389:2x8r/180s, 553:2x8r/180s, 783:2x8r/180s, 1102:2x10r/180s, 517:2x8r/180s]`
  - **violaciones:** [dosis subida en la sesion senalada 3: 1096:3x6r/150s -> 1096:2x8r/180s, dosis subida en la sesion senalada 3: 389:3x6r/150s -> 389:2x8r/180s, dosis subida en la sesion senalada 3: 553:3x6r/150s -> 553:2x8r/180s, dosis subida en la sesion senalada 3: 783:3x6r/150s -> 783:2x8r/180s, dosis subida en la sesion senalada 3: 517:3x6r/150s -> 517:2x8r/180s]
- s3 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s4 [HECHO,HECHO,TIEMPO] ADAPT_LOCALIZED/APLICADA {ACORTAR_O_REORGANIZAR} · llamadas 1
  - antes: `21 46min [1135:3x90s/60s, 1293:3x90s/60s, 1096:2x8r/180s, 389:2x8r/180s, 553:2x8r/180s, 783:2x8r/180s, 1102:2x10r/180s, 517:2x8r/180s]`
  - después: `24 30min [1135:2x60s/30s, 1293:2x60s/30s, 1096:2x6r/120s, 389:2x6r/120s, 553:2x6r/120s, 783:2x6r/120s, 1102:2x10r/90s, 517:2x6r/120s]`
- s5 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s6 [HECHO,HECHO,PARCIAL_CARGA] ADAPT_LOCALIZED/APLICADA {REVISAR_CARGA_Y_RECUPERACION} · llamadas 3
  - antes: `30 30min [1135:2x60s/30s, 1293:2x60s/30s, 1096:2x6r/120s@20kg, 389:2x6r/120s, 553:2x6r/120s, 783:2x6r/120s, 1102:2x10r/90s, 517:2x6r/120s]`
  - después: `72 55min [1135:3x60s/30s, 1293:3x60s/30s, 1096:2x6r/150s@20kg, 389:3x6r/150s, 553:4x6r/150s, 783:3x6r/150s, 1102:3x10r/120s, 517:3x6r/150s]`
  - **violaciones:** [dosis subida en la sesion senalada 30: 1135:2x60s/30s -> 1135:3x60s/30s, dosis subida en la sesion senalada 30: 1293:2x60s/30s -> 1293:3x60s/30s, dosis subida en la sesion senalada 30: 389:2x6r/120s -> 389:3x6r/150s, dosis subida en la sesion senalada 30: 553:2x6r/120s -> 553:4x6r/150s, dosis subida en la sesion senalada 30: 783:2x6r/120s -> 783:3x6r/150s, dosis subida en la sesion senalada 30: 1102:2x10r/90s -> 1102:3x10r/120s, dosis subida en la sesion senalada 30: 517:2x6r/120s -> 517:3x6r/150s]
- s7 [HECHO,DIFICIL,HECHO] ADAPT_LOCALIZED/APLICADA {VARIANTE_MAS_SENCILLA_O_CARGA} · llamadas 1
  - antes: `71 64min [293:3x120s/60s, 1324:3x120s/60s, 1092:3x6r/150s, 357:3x6r/150s, 554:3x6r/150s, 704:3x6r/150s, 872:3x10r/150s, 1111:3x6r/150s]`
  - después: `77 42min [293:3x60s/45s, 1324:3x120s/45s, 1092:3x8r/90s, 357:3x8r/90s, 554:3x8r/90s, 704:3x8r/90s, 872:3x12r/60s, 1111:3x10r/60s]`
  - **violaciones:** [dosis subida en la sesion senalada 71: 1092:3x6r/150s -> 1092:3x8r/90s, dosis subida en la sesion senalada 71: 357:3x6r/150s -> 357:3x8r/90s, dosis subida en la sesion senalada 71: 554:3x6r/150s -> 554:3x8r/90s, dosis subida en la sesion senalada 71: 704:3x6r/150s -> 704:3x8r/90s, dosis subida en la sesion senalada 71: 872:3x10r/150s -> 872:3x12r/60s, dosis subida en la sesion senalada 71: 1111:3x6r/150s -> 1111:3x10r/60s]
- s8 [HECHO,MOTIVACION,MOTIVACION] ADAPT_EXPANDED/APLICADA {INVESTIGAR_CAMBIO_MINIMO,INVESTIGAR_CAMBIO_MINIMO} · llamadas 2
  - antes: `77 42min [293:3x60s/45s, 1324:3x120s/45s, 1092:3x8r/90s, 357:3x8r/90s, 554:3x8r/90s, 704:3x8r/90s, 872:3x12r/60s, 1111:3x10r/60s] 78 55min [1135:3x60s/30s, 1293:3x60s/30s, 1096:2x6r/150s@20kg, 389:3x6r/150s, 553:4x6r/150s, 783:3x6r/150s, 1102:3x10r/120s, 517:3x6r/150s]`
  - después: `86 46min [293:3x45s/30s, 1324:3x120s/30s, 1092:3x8r/75s, 357:3x8r/75s, 554:3x8r/90s, 704:3x8r/75s, 872:3x12r/60s, 1111:3x10r/60s] 87 44min [1135:3x60s/30s, 1293:3x60s/30s, 1096:3x6r/150s, 389:3x6r/120s, 553:3x6r/150s, 783:3x6r/120s, 1102:3x10r/120s, 517:2x6r/120s]`
  - **violaciones:** [dosis subida en la sesion senalada 78: 1096:2x6r/150s@20kg -> 1096:3x6r/150s]
- s9 [HECHO,HECHO,DOLOR] ADAPT_LOCALIZED/APLICADA {SEGURIDAD_ANTE_DOLOR} · llamadas 1
  - antes: `87 44min [1135:3x60s/30s, 1293:3x60s/30s, 1096:3x6r/150s, 389:3x6r/120s, 553:3x6r/150s, 783:3x6r/120s, 1102:3x10r/120s, 517:2x6r/120s]`
  - después: `93 55min [1133:-x600s/nulls, 1235:3x8r/120s, 238:3x8r/120s, 712:3x8r/90s, 896:3x10r/90s, 739:3x8r/90s, 1225:3x12r/90s, 894:-x180s/nulls]`
- s10 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0

### GAIN_MUSCLE/BEGINNER/30/CASA

Inicial: AI, 2 llamadas, VALIDA

- s2 [HECHO,HECHO,FATIGA] ADAPT_LOCALIZED/SIN_EFECTO {REVISAR_CARGA_Y_RECUPERACION} · llamadas 1
  - antes: `42 24min [1096:3x10r/90s, 307:3x10r/90s, 1263:3x10r/90s, 1105:2x13r/90s]`
  - después: `51 24min [1096:3x10r/90s, 307:3x10r/90s, 1263:3x10r/90s, 1105:2x13r/90s]`
- s3 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s4 [HECHO,HECHO,TIEMPO] ADAPT_LOCALIZED/APLICADA {ACORTAR_O_REORGANIZAR} · llamadas 1
  - antes: `54 24min [1096:3x10r/90s, 307:3x10r/90s, 1263:3x10r/90s, 1105:2x13r/90s]`
  - después: `57 18min [1096:3x10r/90s, 307:3x10r/90s, 1263:3x10r/90s]`
- s5 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s6 [HECHO,HECHO,PARCIAL_CARGA] ADAPT_LOCALIZED/APLICADA {REVISAR_CARGA_Y_RECUPERACION} · llamadas 1
  - antes: `60 18min [1096:3x10r/90s@20kg, 307:3x10r/90s, 1263:3x10r/90s]`
  - después: `66 18min [1096:3x10r/90s@18kg, 307:3x10r/90s, 1263:3x10r/90s]`
- s7 [HECHO,DIFICIL,HECHO] ADAPT_LOCALIZED/APLICADA {VARIANTE_MAS_SENCILLA_O_CARGA} · llamadas 1
  - antes: `65 26min [896:3x10r/90s, 115:3x10r/90s, 1073:3x10r/90s, 1102:3x13r/90s]`
  - después: `74 24min [1098:3x10r/90s, 117:3x10r/90s, 122:3x10r/90s, 1102:3x13r/90s]`
- s8 [HECHO,MOTIVACION,MOTIVACION] ADAPT_EXPANDED/NO_FACTIBLE {INVESTIGAR_CAMBIO_MINIMO,INVESTIGAR_CAMBIO_MINIMO} · llamadas 3 · motivo: V11: el ejercicio 1096 sugiere 18.00 kg y la persona no anoto peso la semana anterior; debe ir en null.

### GAIN_MUSCLE/INTERMEDIATE/45/GYM

Inicial: AI, 1 llamadas, VALIDA

- s2 [HECHO,HECHO,FATIGA] ADAPT_LOCALIZED/SIN_EFECTO {REVISAR_CARGA_Y_RECUPERACION} · llamadas 1
  - antes: `99 38min [547:3x10r/90s, 998:3x10r/90s, 553:3x10r/90s, 739:3x10r/90s, 1103:3x13r/90s, 519:3x10r/90s]`
  - después: `108 38min [547:3x10r/90s, 998:3x10r/90s, 553:3x10r/90s, 739:3x10r/90s, 1103:3x13r/90s, 519:3x10r/90s]`
- s3 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s4 [HECHO,HECHO,TIEMPO] ADAPT_LOCALIZED/SIN_EFECTO {ACORTAR_O_REORGANIZAR} · llamadas 2
  - antes: `111 38min [547:3x10r/90s, 998:3x10r/90s, 553:3x10r/90s, 739:3x10r/90s, 1103:3x13r/90s, 519:3x10r/90s]`
  - después: `117 38min [547:3x10r/90s, 998:3x10r/90s, 553:3x10r/90s, 739:3x10r/90s, 1103:3x13r/90s, 519:3x10r/90s]`
- s5 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s6 [HECHO,HECHO,PARCIAL_CARGA] ADAPT_LOCALIZED/APLICADA {REVISAR_CARGA_Y_RECUPERACION} · llamadas 1
  - antes: `120 38min [547:3x10r/90s@20kg, 998:3x10r/90s, 553:3x10r/90s, 739:3x10r/90s, 1103:3x13r/90s, 519:3x10r/90s]`
  - después: `129 37min [547:3x10r/90s@18kg, 998:3x10r/90s, 553:3x10r/90s, 739:3x10r/90s, 1103:3x13r/90s, 519:3x10r/90s]`
- s7 [HECHO,DIFICIL,HECHO] ADAPT_LOCALIZED/APLICADA {VARIANTE_MAS_SENCILLA_O_CARGA} · llamadas 1
  - antes: `128 38min [548:3x10r/90s, 349:3x10r/90s, 755:3x10r/90s, 705:3x10r/90s, 1059:3x13r/90s, 1002:3x10r/90s]`
  - después: `146 36min [548:3x10r/90s, 349:3x10r/90s, 144:3x10r/90s, 740:3x10r/90s, 1315:3x10r/90s, 433:3x10r/90s]`
- s8 [HECHO,MOTIVACION,MOTIVACION] ADAPT_EXPANDED/APLICADA {INVESTIGAR_CAMBIO_MINIMO,INVESTIGAR_CAMBIO_MINIMO} · llamadas 3
  - antes: `146 36min [548:3x10r/90s, 349:3x10r/90s, 144:3x10r/90s, 740:3x10r/90s, 1315:3x10r/90s, 433:3x10r/90s] 147 37min [547:3x10r/90s@18kg, 998:3x10r/90s, 553:3x10r/90s, 739:3x10r/90s, 1103:3x13r/90s, 519:3x10r/90s]`
  - después: `176 36min [548:3x10r/90s, 349:3x10r/90s, 144:3x10r/90s, 740:3x10r/90s, 1315:3x10r/90s, 433:3x10r/90s] 177 36min [547:3x10r/90s, 998:3x10r/90s, 553:3x10r/90s, 739:3x10r/90s, 1103:3x13r/90s, 519:3x10r/90s]`
- s9 [HECHO,HECHO,DOLOR] ADAPT_LOCALIZED/APLICADA {SEGURIDAD_ANTE_DOLOR} · llamadas 1
  - antes: `177 36min [547:3x10r/90s, 998:3x10r/90s, 553:3x10r/90s, 739:3x10r/90s, 1103:3x13r/90s, 519:3x10r/90s]`
  - después: `186 34min [517:3x10r/90s, 437:3x10r/90s, 715:3x10r/90s, 791:3x10r/90s, 1102:3x13r/90s, 572:2x10r/90s]`
- s10 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0

### GAIN_MUSCLE/ADVANCED/60/GYM

Inicial: RULE_ENGINE, 3 llamadas, VALIDA

- s2 [HECHO,HECHO,FATIGA] ADAPT_LOCALIZED/SIN_EFECTO {REVISAR_CARGA_Y_RECUPERACION} · llamadas 1
  - antes: `159 43min [1094:3x10r/90s, 712:3x10r/90s, 787:3x10r/90s, 1271:3x10r/90s, 1059:3x13r/90s, 992:3x10r/90s, 1031:3x10r/90s]`
  - después: `162 43min [1094:3x10r/90s, 712:3x10r/90s, 787:3x10r/90s, 1271:3x10r/90s, 1059:3x13r/90s, 992:3x10r/90s, 1031:3x10r/90s]`
- s3 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s4 [HECHO,HECHO,TIEMPO] ADAPT_LOCALIZED/SIN_EFECTO {ACORTAR_O_REORGANIZAR} · llamadas 1
  - antes: `165 43min [1094:3x10r/90s, 712:3x10r/90s, 787:3x10r/90s, 1271:3x10r/90s, 1059:3x13r/90s, 992:3x10r/90s, 1031:3x10r/90s]`
  - después: `171 43min [1094:3x10r/90s, 712:3x10r/90s, 787:3x10r/90s, 1271:3x10r/90s, 1059:3x13r/90s, 992:3x10r/90s, 1031:3x10r/90s]`
- s5 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s6 [HECHO,HECHO,PARCIAL_CARGA] ADAPT_LOCALIZED/APLICADA {REVISAR_CARGA_Y_RECUPERACION} · llamadas 1
  - antes: `174 43min [1094:3x10r/90s@20kg, 712:3x10r/90s, 787:3x10r/90s, 1271:3x10r/90s, 1059:3x13r/90s, 992:3x10r/90s, 1031:3x10r/90s]`
  - después: `183 43min [1094:3x10r/90s@18kg, 712:3x10r/90s, 787:3x10r/90s, 1271:3x10r/90s, 1059:3x13r/90s, 992:3x10r/90s, 1031:3x10r/90s]`
- s7 [HECHO,DIFICIL,HECHO] ADAPT_LOCALIZED/APLICADA {VARIANTE_MAS_SENCILLA_O_CARGA} · llamadas 1
  - antes: `182 43min [1286:3x10r/90s, 1001:3x10r/90s, 1074:3x10r/90s, 511:3x10r/90s, 959:3x13r/90s, 56:3x10r/90s, 77:3x10r/90s]`
  - después: `191 42min [1286:3x10r/90s, 1001:3x10r/90s, 595:3x10r/90s, 511:3x10r/90s, 715:3x10r/90s, 1096:3x10r/90s, 77:3x10r/90s]`
- s8 [HECHO,MOTIVACION,MOTIVACION] ADAPT_EXPANDED/SIN_EFECTO {INVESTIGAR_CAMBIO_MINIMO,INVESTIGAR_CAMBIO_MINIMO} · llamadas 2
  - antes: `191 42min [1286:3x10r/90s, 1001:3x10r/90s, 595:3x10r/90s, 511:3x10r/90s, 715:3x10r/90s, 1096:3x10r/90s, 77:3x10r/90s] 192 43min [1094:3x10r/90s@18kg, 712:3x10r/90s, 787:3x10r/90s, 1271:3x10r/90s, 1059:3x13r/90s, 992:3x10r/90s, 1031:3x10r/90s]`
  - después: `200 42min [1286:3x10r/90s, 1001:3x10r/90s, 595:3x10r/90s, 511:3x10r/90s, 715:3x10r/90s, 1096:3x10r/90s, 77:3x10r/90s] 201 43min [1094:3x10r/90s, 712:3x10r/90s, 787:3x10r/90s, 1271:3x10r/90s, 1059:3x13r/90s, 992:3x10r/90s, 1031:3x10r/90s]`
- s9 [HECHO,HECHO,DOLOR] ADAPT_LOCALIZED/APLICADA {SEGURIDAD_ANTE_DOLOR} · llamadas 1
  - antes: `201 43min [1094:3x10r/90s, 712:3x10r/90s, 787:3x10r/90s, 1271:3x10r/90s, 1059:3x13r/90s, 992:3x10r/90s, 1031:3x10r/90s]`
  - después: `216 43min [151:3x10r/90s, 232:3x10r/90s, 160:3x10r/90s, 238:3x10r/90s, 221:3x10r/90s, 404:3x10r/90s, 872:3x13r/90s]`
- s10 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0

### INCREASE_STRENGTH/BEGINNER/30/CASA

Inicial: AI, 1 llamadas, VALIDA

- s2 [HECHO,HECHO,FATIGA] ADAPT_LOCALIZED/APLICADA {REVISAR_CARGA_Y_RECUPERACION} · llamadas 1
  - antes: `114 28min [1315:2x8r/120s, 1118:2x8r/120s, 1096:2x8r/120s, 1156:2x8r/120s, 501:2x13r/90s, 706:2x10r/90s]`
  - después: `123 30min [1315:2x8r/150s, 1118:2x8r/150s, 1096:2x8r/150s, 1156:2x8r/150s, 501:2x10r/90s, 706:2x8r/90s]`
- s3 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s4 [HECHO,HECHO,TIEMPO] ADAPT_LOCALIZED/APLICADA {ACORTAR_O_REORGANIZAR} · llamadas 2
  - antes: `126 30min [1315:2x8r/150s, 1118:2x8r/150s, 1096:2x8r/150s, 1156:2x8r/150s, 501:2x10r/90s, 706:2x8r/90s]`
  - después: `135 22min [1096:2x8r/90s, 1156:2x8r/90s, 1315:2x8r/90s, 1118:2x8r/60s, 706:2x8r/45s, 501:2x10r/45s]`
- s5 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s6 [HECHO,HECHO,PARCIAL_CARGA] ADAPT_LOCALIZED/APLICADA {REVISAR_CARGA_Y_RECUPERACION} · llamadas 1
  - antes: `138 22min [1096:2x8r/90s@20kg, 1156:2x8r/90s, 1315:2x8r/90s, 1118:2x8r/60s, 706:2x8r/45s, 501:2x10r/45s]`
  - después: `150 28min [1096:2x8r/120s@18kg, 1156:2x8r/120s, 1315:2x8r/120s, 1118:2x8r/60s, 706:2x8r/45s, 501:2x10r/45s]`
- s7 [HECHO,DIFICIL,HECHO] ADAPT_LOCALIZED/APLICADA {VARIANTE_MAS_SENCILLA_O_CARGA} · llamadas 2
  - antes: `149 28min [1264:2x8r/120s, 1073:2x8r/120s, 1155:2x8r/120s, 117:2x8r/120s, 501:2x13r/90s, 708:2x10r/90s]`
  - después: `167 23min [1114:2x8r/90s, 1157:2x8r/90s, 115:2x8r/90s, 1097:2x8r/90s, 709:2x8r/60s, 501:2x10r/60s]`
- s8 [HECHO,MOTIVACION,MOTIVACION] ADAPT_EXPANDED/APLICADA {INVESTIGAR_CAMBIO_MINIMO,INVESTIGAR_CAMBIO_MINIMO} · llamadas 2
  - antes: `167 23min [1114:2x8r/90s, 1157:2x8r/90s, 115:2x8r/90s, 1097:2x8r/90s, 709:2x8r/60s, 501:2x10r/60s] 168 28min [1096:2x8r/120s@18kg, 1156:2x8r/120s, 1315:2x8r/120s, 1118:2x8r/60s, 706:2x8r/45s, 501:2x10r/45s]`
  - después: `197 21min [1114:2x8r/60s, 1157:2x8r/60s, 115:2x8r/60s, 1097:2x8r/60s, 709:2x8r/45s, 501:2x10r/45s] 198 22min [1096:2x8r/90s, 1156:2x8r/90s, 1315:2x8r/90s, 1118:2x8r/45s, 706:2x8r/45s, 501:2x10r/45s]`
- s9 [HECHO,HECHO,DOLOR] ADAPT_LOCALIZED/APLICADA {SEGURIDAD_ANTE_DOLOR} · llamadas 2
  - antes: `198 22min [1096:2x8r/90s, 1156:2x8r/90s, 1315:2x8r/90s, 1118:2x8r/45s, 706:2x8r/45s, 501:2x10r/45s]`
  - después: `207 33min [933:3x8r/120s, 116:3x8r/120s, 791:3x8r/120s, 122:2x10r/120s, 709:2x10r/60s, 502:2x12r/60s]`
- s10 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0

### INCREASE_STRENGTH/INTERMEDIATE/45/GYM

Inicial: AI, 1 llamadas, VALIDA

- s2 [HECHO,HECHO,FATIGA] ADAPT_LOCALIZED/APLICADA {REVISAR_CARGA_Y_RECUPERACION} · llamadas 1
  - antes: `132 36min [418:3x6r/150s, 780:3x6r/150s, 554:3x6r/150s, 788:3x6r/150s, 422:2x10r/90s]`
  - después: `141 32min [418:3x6r/150s, 780:3x6r/150s, 554:3x6r/150s, 788:3x6r/150s, 422:2x10r/90s]`
- s3 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s4 [HECHO,HECHO,TIEMPO] ADAPT_LOCALIZED/APLICADA {ACORTAR_O_REORGANIZAR} · llamadas 2
  - antes: `144 32min [418:3x6r/150s, 780:3x6r/150s, 554:3x6r/150s, 788:3x6r/150s, 422:2x10r/90s]`
  - después: `153 21min [418:2x6r/120s, 780:2x6r/120s, 554:2x6r/120s, 788:2x6r/120s, 422:2x8r/60s]`
- s5 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s6 [HECHO,HECHO,PARCIAL_CARGA] ADAPT_LOCALIZED/APLICADA {REVISAR_CARGA_Y_RECUPERACION} · llamadas 2
  - antes: `156 21min [418:2x6r/120s@20kg, 780:2x6r/120s, 554:2x6r/120s, 788:2x6r/120s, 422:2x8r/60s]`
  - después: `180 33min [418:3x6r/150s@18kg, 780:2x6r/150s, 554:3x6r/150s, 788:3x6r/150s, 422:2x8r/90s]`
  - **violaciones:** [dosis subida en la sesion senalada 156: 418:2x6r/120s@20kg -> 418:3x6r/150s@18kg, dosis subida en la sesion senalada 156: 554:2x6r/120s -> 554:3x6r/150s, dosis subida en la sesion senalada 156: 788:2x6r/120s -> 788:3x6r/150s]
- s7 [HECHO,DIFICIL,HECHO] ADAPT_LOCALIZED/APLICADA {VARIANTE_MAS_SENCILLA_O_CARGA} · llamadas 2
  - antes: `179 36min [177:3x6r/150s, 390:3x6r/150s, 1003:3x6r/150s, 715:3x6r/150s, 335:2x10r/90s]`
  - después: `194 31min [992:3x6r/150s, 998:3x6r/150s, 1003:3x6r/150s, 1004:3x6r/150s]`
- s8 [HECHO,MOTIVACION,MOTIVACION] ADAPT_EXPANDED/APLICADA {INVESTIGAR_CAMBIO_MINIMO,INVESTIGAR_CAMBIO_MINIMO} · llamadas 2
  - antes: `194 31min [992:3x6r/150s, 998:3x6r/150s, 1003:3x6r/150s, 1004:3x6r/150s] 195 33min [418:3x6r/150s@18kg, 780:2x6r/150s, 554:3x6r/150s, 788:3x6r/150s, 422:2x8r/90s]`
  - después: `212 32min [992:4x6r/150s, 998:3x6r/150s, 1003:4x6r/150s, 1004:3x6r/150s] 213 32min [418:4x6r/150s, 780:3x6r/150s, 554:3x6r/150s, 788:3x6r/150s, 422:2x8r/90s]`
  - **violaciones:** [dosis subida en la sesion senalada 194: 992:3x6r/150s -> 992:4x6r/150s, dosis subida en la sesion senalada 194: 1003:3x6r/150s -> 1003:4x6r/150s, dosis subida en la sesion senalada 195: 418:3x6r/150s@18kg -> 418:4x6r/150s, dosis subida en la sesion senalada 195: 780:2x6r/150s -> 780:3x6r/150s]
- s9 [HECHO,HECHO,DOLOR] ADAPT_LOCALIZED/APLICADA {SEGURIDAD_ANTE_DOLOR} · llamadas 2
  - antes: `213 32min [418:4x6r/150s, 780:3x6r/150s, 554:3x6r/150s, 788:3x6r/150s, 422:2x8r/90s]`
  - después: `222 32min [519:4x6r/150s, 553:3x6r/150s, 711:3x6r/120s, 512:3x6r/120s]`
- s10 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0

### INCREASE_STRENGTH/ADVANCED/60/GYM

Inicial: AI, 1 llamadas, VALIDA

- s2 [HECHO,HECHO,FATIGA] ADAPT_LOCALIZED/APLICADA {REVISAR_CARGA_Y_RECUPERACION} · llamadas 2
  - antes: `204 44min [183:3x6r/150s, 186:3x6r/150s, 151:3x6r/150s, 435:3x6r/150s, 503:2x13r/90s, 474:2x10r/90s, 577:2x10r/90s]`
  - después: `228 47min [183:4x6r/180s, 186:3x6r/150s, 151:4x6r/180s, 435:3x6r/150s, 503:2x13r/90s, 474:3x10r/90s, 577:2x10r/90s]`
  - **violaciones:** [dosis subida en la sesion senalada 204: 183:3x6r/150s -> 183:4x6r/180s, dosis subida en la sesion senalada 204: 151:3x6r/150s -> 151:4x6r/180s, dosis subida en la sesion senalada 204: 474:2x10r/90s -> 474:3x10r/90s]
- s3 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s4 [HECHO,HECHO,TIEMPO] ADAPT_LOCALIZED/APLICADA {ACORTAR_O_REORGANIZAR} · llamadas 1
  - antes: `231 47min [183:4x6r/180s, 186:3x6r/150s, 151:4x6r/180s, 435:3x6r/150s, 503:2x13r/90s, 474:3x10r/90s, 577:2x10r/90s]`
  - después: `234 41min [183:3x6r/150s, 186:3x6r/120s, 151:3x6r/150s, 435:3x6r/120s, 474:2x10r/60s, 577:2x10r/60s, 503:2x12r/60s]`
- s5 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s6 [HECHO,HECHO,PARCIAL_CARGA] ADAPT_LOCALIZED/APLICADA {REVISAR_CARGA_Y_RECUPERACION} · llamadas 3
  - antes: `237 41min [183:3x6r/150s@20kg, 186:3x6r/120s, 151:3x6r/150s, 435:3x6r/120s, 474:2x10r/60s, 577:2x10r/60s, 503:2x12r/60s]`
  - después: `258 43min [183:6x6r/180s@20kg, 186:2x6r/180s, 151:2x6r/180s, 435:2x6r/180s, 474:2x10r/120s, 577:2x10r/120s, 503:4x12r/60s]`
  - **violaciones:** [dosis subida en la sesion senalada 237: 183:3x6r/150s@20kg -> 183:6x6r/180s@20kg, dosis subida en la sesion senalada 237: 503:2x12r/60s -> 503:4x12r/60s]
- s7 [HECHO,DIFICIL,HECHO] ADAPT_LOCALIZED/APLICADA {VARIANTE_MAS_SENCILLA_O_CARGA} · llamadas 2
  - antes: `257 44min [279:3x6r/150s, 235:3x6r/150s, 517:3x6r/150s, 1323:3x6r/150s, 975:2x13r/90s, 577:2x10r/90s, 515:3x10r/90s]`
  - después: `272 43min [554:4x6r/150s, 553:3x6r/150s, 517:3x6r/150s, 1323:3x6r/150s, 975:3x13r/90s, 577:3x10r/90s, 515:3x8r/90s]`
  - **violaciones:** [dosis subida en la sesion senalada 257: 975:2x13r/90s -> 975:3x13r/90s, dosis subida en la sesion senalada 257: 577:2x10r/90s -> 577:3x10r/90s]
- s8 [HECHO,MOTIVACION,MOTIVACION] ADAPT_EXPANDED/APLICADA {INVESTIGAR_CAMBIO_MINIMO,INVESTIGAR_CAMBIO_MINIMO} · llamadas 2
  - antes: `272 43min [554:4x6r/150s, 553:3x6r/150s, 517:3x6r/150s, 1323:3x6r/150s, 975:3x13r/90s, 577:3x10r/90s, 515:3x8r/90s] 273 43min [183:6x6r/180s@20kg, 186:2x6r/180s, 151:2x6r/180s, 435:2x6r/180s, 474:2x10r/120s, 577:2x10r/120s, 503:4x12r/60s]`
  - después: `284 49min [554:4x6r/150s, 553:3x6r/150s, 517:3x6r/150s, 1323:3x6r/150s, 975:2x13r/90s, 577:2x10r/90s, 515:2x8r/90s] 285 64min [183:4x6r/180s, 186:3x6r/180s, 151:3x6r/180s, 435:3x6r/180s, 474:3x10r/120s, 577:3x10r/120s, 503:4x12r/60s]`
  - **violaciones:** [dosis subida en la sesion senalada 273: 186:2x6r/180s -> 186:3x6r/180s, dosis subida en la sesion senalada 273: 151:2x6r/180s -> 151:3x6r/180s, dosis subida en la sesion senalada 273: 435:2x6r/180s -> 435:3x6r/180s, dosis subida en la sesion senalada 273: 474:2x10r/120s -> 474:3x10r/120s, dosis subida en la sesion senalada 273: 577:2x10r/120s -> 577:3x10r/120s]
- s9 [HECHO,HECHO,DOLOR] ADAPT_LOCALIZED/APLICADA {SEGURIDAD_ANTE_DOLOR} · llamadas 2
  - antes: `285 64min [183:4x6r/180s, 186:3x6r/180s, 151:3x6r/180s, 435:3x6r/180s, 474:3x10r/120s, 577:3x10r/120s, 503:4x12r/60s]`
  - después: `294 49min [184:4x6r/180s, 164:3x6r/180s, 517:3x6r/180s, 981:3x6r/120s, 221:3x6r/120s, 976:4x12r/60s]`
- s10 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0

### GENERAL_FITNESS/BEGINNER/30/CASA

Inicial: AI, 2 llamadas, VALIDA

- s2 [HECHO,HECHO,FATIGA] ADAPT_LOCALIZED/APLICADA {REVISAR_CARGA_Y_RECUPERACION} · llamadas 2
  - antes: `240 29min [1093:3x90s/60s, 938:2x30s/15s, 147:2x8r/120s, 1102:2x15r/45s, 1097:2x10r/90s, 1157:2x10r/90s]`
  - después: `252 31min [1093:3x60s/60s, 938:2x30s/15s, 147:3x8r/120s, 1102:3x10r/45s, 1097:2x8r/120s, 1157:3x8r/90s]`
  - **violaciones:** [dosis subida en la sesion senalada 240: 147:2x8r/120s -> 147:3x8r/120s, dosis subida en la sesion senalada 240: 1102:2x15r/45s -> 1102:3x10r/45s, dosis subida en la sesion senalada 240: 1157:2x10r/90s -> 1157:3x8r/90s]
- s3 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s4 [HECHO,HECHO,TIEMPO] ADAPT_LOCALIZED/APLICADA {ACORTAR_O_REORGANIZAR} · llamadas 1
  - antes: `255 31min [1093:3x60s/60s, 938:2x30s/15s, 147:3x8r/120s, 1102:3x10r/45s, 1097:2x8r/120s, 1157:3x8r/90s]`
  - después: `261 25min [1093:2x45s/45s, 938:2x30s/15s, 147:2x8r/90s, 1102:2x10r/45s, 1097:2x8r/120s, 1157:2x8r/90s]`
- s5 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s6 [HECHO,HECHO,PARCIAL_CARGA] ADAPT_LOCALIZED/APLICADA {REVISAR_CARGA_Y_RECUPERACION} · llamadas 1
  - antes: `264 25min [1093:2x45s/45s, 938:2x30s/15s, 147:2x8r/90s@20kg, 1102:2x10r/45s, 1097:2x8r/120s, 1157:2x8r/90s]`
  - después: `270 24min [1093:2x45s/60s, 938:2x30s/20s, 147:2x8r/150s@18kg, 1102:2x10r/90s, 1097:2x8r/120s, 1157:2x8r/120s]`
- s7 [HECHO,DIFICIL,HECHO] ADAPT_LOCALIZED/APLICADA {VARIANTE_MAS_SENCILLA_O_CARGA} · llamadas 2
  - antes: `269 29min [1054:3x90s/60s, 1193:2x30s/15s, 791:2x8r/120s, 1302:2x15r/45s, 1099:2x10r/90s, 1073:2x10r/90s]`
  - después: `275 19min [1054:2x45s/45s, 1193:2x30s/15s, 1076:2x10r/90s, 1302:2x12r/45s, 1097:2x10r/60s, 1073:2x10r/60s]`
- s8 [HECHO,MOTIVACION,MOTIVACION] ADAPT_EXPANDED/APLICADA {INVESTIGAR_CAMBIO_MINIMO,INVESTIGAR_CAMBIO_MINIMO} · llamadas 2
  - antes: `275 19min [1054:2x45s/45s, 1193:2x30s/15s, 1076:2x10r/90s, 1302:2x12r/45s, 1097:2x10r/60s, 1073:2x10r/60s] 276 24min [1093:2x45s/60s, 938:2x30s/20s, 147:2x8r/150s@18kg, 1102:2x10r/90s, 1097:2x8r/120s, 1157:2x8r/120s]`
  - después: `281 21min [1054:2x30s/45s, 1193:2x30s/15s, 1076:2x10r/90s, 1302:2x10r/45s, 1097:2x8r/60s, 1073:2x8r/60s] 282 23min [1093:2x30s/60s, 938:2x30s/20s, 147:2x8r/120s, 1102:2x10r/90s, 1097:2x8r/90s, 1157:2x8r/90s]`
- s9 [HECHO,HECHO,DOLOR] ADAPT_EXPANDED/APLICADA {INVESTIGAR_CAMBIO_MINIMO,INVESTIGAR_CAMBIO_MINIMO} · llamadas 3
  - **violaciones:** [ai_calls V32 = 2, llamadas contadas = 3]

### GENERAL_FITNESS/INTERMEDIATE/45/GYM

Inicial: AI, 2 llamadas, VALIDA

- s2 [HECHO,HECHO,FATIGA] ADAPT_LOCALIZED/APLICADA {REVISAR_CARGA_Y_RECUPERACION} · llamadas 1
  - antes: `243 39min [517:3x6r/150s, 1104:3x40s/45s, 1324:3x120s/60s, 1150:2x30s/15s, 592:3x10r/90s, 1102:3x13r/90s]`
  - después: `246 32min [517:3x6r/180s, 1104:-x30s/60s, 1324:3x120s/60s, 1150:2x30s/15s, 592:3x10r/120s, 1102:3x13r/120s]`
- s3 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s4 [HECHO,HECHO,TIEMPO] NO_CHANGE/SIN_ORDEN · llamadas 2
  - **violaciones:** [ai_calls V32 = 0, llamadas contadas = 2, ciclo determinista con 2 llamadas a la IA]

### GENERAL_FITNESS/ADVANCED/60/GYM

Inicial: AI, 3 llamadas, VALIDA

- s2 [HECHO,HECHO,FATIGA] ADAPT_LOCALIZED/APLICADA {REVISAR_CARGA_Y_RECUPERACION} · llamadas 2
  - antes: `279 46min [533:3x120s/60s, 1309:2x30s/15s, 164:3x6r/150s, 1104:3x40s/45s, 710:3x10r/90s, 432:3x10r/90s, 539:2x10r/90s, 976:2x13r/90s]`
  - después: `288 53min [533:3x120s/180s, 1309:2x30s/30s, 164:2x6r/180s, 1104:3x40s/180s, 710:2x8r/180s, 432:2x10r/180s, 539:2x10r/90s, 976:2x13r/90s]`
- s3 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s4 [HECHO,HECHO,TIEMPO] ADAPT_LOCALIZED/APLICADA {ACORTAR_O_REORGANIZAR} · llamadas 3
  - antes: `291 53min [533:3x120s/180s, 1309:2x30s/30s, 164:2x6r/180s, 1104:3x40s/180s, 710:2x8r/180s, 432:2x10r/180s, 539:2x10r/90s, 976:2x13r/90s]`
  - después: `303 32min [533:2x60s/90s, 1309:2x30s/30s, 164:2x6r/180s, 1104:2x40s/60s, 710:2x8r/180s, 432:2x10r/180s, 539:2x10r/90s, 976:2x13r/90s]`
- s5 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0
- s6 [HECHO,HECHO,PARCIAL_CARGA] ADAPT_LOCALIZED/APLICADA {REVISAR_CARGA_Y_RECUPERACION} · llamadas 2
  - antes: `306 32min [533:2x60s/90s, 1309:2x30s/30s, 164:2x6r/180s@20kg, 1104:2x40s/60s, 710:2x8r/180s, 432:2x10r/180s, 539:2x10r/90s, 976:2x13r/90s]`
  - después: `309 44min [533:2x60s/90s, 1309:2x30s/30s, 164:2x6r/180s@20kg, 1104:3x30s/60s, 710:2x6r/180s, 432:3x8r/180s, 539:3x10r/90s, 976:3x13r/90s]`
  - **violaciones:** [dosis subida en la sesion senalada 306: 1104:2x40s/60s -> 1104:3x30s/60s, dosis subida en la sesion senalada 306: 432:2x10r/180s -> 432:3x8r/180s, dosis subida en la sesion senalada 306: 539:2x10r/90s -> 539:3x10r/90s, dosis subida en la sesion senalada 306: 976:2x13r/90s -> 976:3x13r/90s]
- s7 [HECHO,DIFICIL,HECHO] ADAPT_LOCALIZED/RESPALDO {VARIANTE_MAS_SENCILLA_O_CARGA} · llamadas 3
  - antes: `308 49min [1054:3x120s/60s, 1075:2x30s/15s, 482:3x6r/150s, 446:3x18r/45s, 553:3x10r/90s, 609:3x10r/90s, 512:3x10r/90s, 1187:2x13r/90s]`
  - después: `311 49min [1054:3x120s/60s, 1075:2x30s/15s, 482:3x6r/150s, 446:3x18r/45s, 553:3x10r/90s, 609:3x10r/90s, 512:3x10r/90s, 1187:2x13r/90s]`
- s8 [HECHO,MOTIVACION,MOTIVACION] ADAPT_EXPANDED/APLICADA {INVESTIGAR_CAMBIO_MINIMO,INVESTIGAR_CAMBIO_MINIMO} · llamadas 2
  - antes: `311 49min [1054:3x120s/60s, 1075:2x30s/15s, 482:3x6r/150s, 446:3x18r/45s, 553:3x10r/90s, 609:3x10r/90s, 512:3x10r/90s, 1187:2x13r/90s] 312 44min [533:2x60s/90s, 1309:2x30s/30s, 164:2x6r/180s@20kg, 1104:3x30s/60s, 710:2x6r/180s, 432:3x8r/180s, 539:3x10r/90s, 976:3x13r/90s]`
  - después: `314 48min [1054:3x120s/60s, 1075:2x30s/15s, 482:3x6r/150s, 446:4x18r/45s, 553:4x10r/90s, 609:4x10r/90s, 512:4x10r/90s, 1187:4x13r/90s] 315 42min [533:3x60s/60s, 1309:2x30s/30s, 164:3x6r/180s, 1104:3x30s/60s, 710:3x6r/180s, 432:3x8r/180s, 539:3x10r/90s, 976:3x13r/90s]`
  - **violaciones:** [dosis subida en la sesion senalada 311: 446:3x18r/45s -> 446:4x18r/45s, dosis subida en la sesion senalada 311: 553:3x10r/90s -> 553:4x10r/90s, dosis subida en la sesion senalada 311: 609:3x10r/90s -> 609:4x10r/90s, dosis subida en la sesion senalada 311: 512:3x10r/90s -> 512:4x10r/90s, dosis subida en la sesion senalada 311: 1187:2x13r/90s -> 1187:4x13r/90s, dosis subida en la sesion senalada 312: 533:2x60s/90s -> 533:3x60s/60s, dosis subida en la sesion senalada 312: 164:2x6r/180s@20kg -> 164:3x6r/180s, dosis subida en la sesion senalada 312: 710:2x6r/180s -> 710:3x6r/180s]
- s9 [HECHO,HECHO,DOLOR] ADAPT_LOCALIZED/APLICADA {SEGURIDAD_ANTE_DOLOR} · llamadas 1
  - antes: `315 42min [533:3x60s/60s, 1309:2x30s/30s, 164:3x6r/180s, 1104:3x30s/60s, 710:3x6r/180s, 432:3x8r/180s, 539:3x10r/90s, 976:3x13r/90s]`
  - después: `318 47min [553:3x8r/180s, 755:3x10r/150s, 989:3x8r/150s, 981:3x8r/150s, 778:3x8r/150s, 578:3x12r/60s, 859:3x45s/60s, 893:3x45s/60s]`
- s10 [HECHO,HECHO,HECHO] NO_CHANGE/SIN_ORDEN · llamadas 0


## Integridad V31–V34

Consulta de linaje: 0 violaciones.
