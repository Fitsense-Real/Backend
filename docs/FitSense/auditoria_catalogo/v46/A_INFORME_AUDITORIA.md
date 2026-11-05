# A · Informe de auditoría del catálogo (v46)

**Rama aislada:** `investigacion/catalogo-v46`, creada desde `ae5fdd4`, el cierre experimental, verificado: `SHA256SUMS` de la evidencia real 7/7 OK.

**Sin tocar:**
- Replicate: no se ha llamado.
- Neon, merge, despliegue, `mobileapp`, short paper y PR #41.
- La evidencia R/R′ y U/J/K/L/M/N, los informes 17–25 y los oráculos.

## 1. Fuentes y método

| Fuente | Uso |
|---|---|
| Catálogo exportado tras las 47 migraciones (`ExportarCatalogoIT`, solo lectura; 1.376 ejercicios) | Nombres, traducción, grupo, músculos, patrón, aislado, dificultad, equipamiento, requisitos, banderas, instrucciones EN/ES y ruta del GIF |
| GIF originales del dataset de origen (`hasaneyldrm/exercises-dataset`, © Gym visual; descarga parcial, no versionada) | Hojas de 8 fotogramas por ejercicio con `revision_visual/hoja_fotogramas.py` |
| Barridos `REV2_cierre` y `HIST_cierre` (3.776 y 3.641 planes válidos) | Uso real en la generación |
| `SESIONES_REV2.csv` y `EXPOSICION_CATALOGO.csv` (evaluación real) | Uso real en las adaptaciones |
| Código: `CapabilityPrescriptionPolicy.claseDe`, `MandatorySlot`, `ImpactoPolicy`, `CalidadDosis` | Cómo cuenta cada dato |
| `escanear_catalogo.py` | Detección sistemática de contradicciones entre datos, en todo el catálogo activo |

**Sobre la revisión visual:**
- Está **asistida por IA**: la hizo Claude sobre las hojas de fotogramas.
- Es la segunda revisión de este tipo; la anterior está en `revision_visual/matriz_revision_visual_completa.tsv`.
- **No es una validación profesional.** Donde la conclusión depende solo de ella, el hallazgo queda en REVISIÓN VISUAL o en VALIDACIÓN PROFESIONAL.

**Clases de hallazgo:**

| Clase | Significado |
|---|---|
| **ERROR COMPROBADO** | Contradicción verificable entre los datos del propio catálogo, o entre los datos y su uso en el código |
| **CORRECCIÓN OBJETIVA** | Ajuste inequívoco respaldado por varias fuentes coincidentes. Implementado en V46 |
| **REVISIÓN VISUAL** | El movimiento real no se puede fijar sin una revisión humana del GIF |
| **VALIDACIÓN PROFESIONAL** | Decisión deportiva o de prescripción |

## 2. Hallazgos de prioridad 1 (implicados en la evaluación real)

| Id | Lo que dice el catálogo | Lo que muestran nombre, instrucciones y GIF | Clase | Severidad |
|---|---|---|---|---|
| **853** «half knee bends» → «Media sentadilla» | Cardio, `LOCOMOTION_CARDIO`, DURATION (reclasificado en V38 por su GIF) | Nombre e instrucciones: media sentadilla («as if sitting back into a chair»). GIF: **marcha con elevación de rodillas**. Dos fuentes dicen sentadilla y dos (patrón y GIF) dicen marcha | ERROR COMPROBADO (los datos se contradicen) · REVISIÓN VISUAL (cuál es el movimiento) | Media: **solo afecta a lo que se muestra**. Como cardio no aporta series, y su patrón coincide con el GIF |
| **775** «elbow lift - reverse push-up» | Espalda / dorsal alto, `HORIZONTAL_PUSH`, peso corporal sin requisitos, en el suelo | GIF: **tumbado boca abajo sobre un banco**, elevando el tronco con los codos. Instrucciones: tumbado en el suelo, empujando con las palmas. No es un empuje ni una tracción | **ERROR COMPROBADO** (grupo y músculo de espalda con patrón de empuje; instrucciones y GIF no coinciden) · VALIDACIÓN PROFESIONAL (qué patrón y qué dosis) | **Alta**: único ejercicio de espalda en 615 planes REV2 (640 HIST) |
| **973** «left hook. boxing» → «Giro izquierdo. boxeo» | Hombro / deltoides, `TRUNK_ROTATION_LATERAL_FLEXION`, sin carga | Nombre, instrucciones («punching motion») y GIF: **gancho de boxeo** | **CORRECCIÓN OBJETIVA** (traducción; hecho en V46) · **ERROR COMPROBADO** (patrón de tronco contado como dosis directa de hombro) · VALIDACIÓN PROFESIONAL (si es estímulo de hombro) | **Alta**: único hombro en 917 planes REV2 (537 HIST) |
| **1009** «lever lying leg curl» | Isquios, `KNEE_DOMINANT`, aislado, máquina | Curl femoral en máquina: **la ficha es correcta** | ERROR COMPROBADO de **uso** (el código acredita un aislado como patrón principal de rodilla; P15) · VALIDACIÓN PROFESIONAL | Media |
| **1262** «spider crawl push up» | Pecho, empuje, dificultad 1 | Flexión con rodilla al codo | VALIDACIÓN PROFESIONAL (dificultad 1 dudosa) | Media |
| **1115** «push-up inside leg kick» | Pecho, empuje, dificultad 1 | Flexión con patada por debajo del cuerpo | VALIDACIÓN PROFESIONAL (dificultad 1 dudosa) | Media |

## 3. Hallazgos de prioridad 2

| Id | Hallazgo | Clase | Severidad |
|---|---|---|---|
| **1320** «triceps dips floor» | Nombre («floor») y GIF: fondos **en el suelo**. Instrucciones: «sit on the edge of a chair or bench». Peso corporal sin requisitos. La API muestra esas instrucciones a quien entrena sin material | **ERROR COMPROBADO** (instrucciones frente a nombre y equipamiento). Nuevo texto: propuesto (`B`), **no implementado** (sería texto nuevo y el GIF solo se ha revisado con IA) | Media (instrucciones) |
| **580** «dumbbell incline breeding» → «Curl inclinado con mancuerna» | Pecho, `HORIZONTAL_PUSH`, no aislado, 3×6 de fuerza. «Curl» no corresponde al nombre ni al pectoral. Las instrucciones describen un **press**; el GIF muestra una **apertura** («breeding» es el artefacto conocido de «разводка» = apertura) | **ERROR COMPROBADO** (traducción «curl»; instrucciones frente a GIF) · REVISIÓN VISUAL (¿press o apertura?) · VALIDACIÓN PROFESIONAL (si es apertura: `HORIZONTAL_ADDUCTION`, aislado, sin dosis de fuerza) | Baja en la selección (53 planes) |
| **1261** «standing pelvic tilt», **1308** «two toe touch» | Espalda con `TRUNK_FLEXION`: sus series cuentan como dosis de espalda. GIF: inclinación pélvica de pie y toque de pies con los brazos arriba, sin carga | **ERROR COMPROBADO** (flexión de tronco contada como espalda) · VALIDACIÓN PROFESIONAL (¿movilidad?) | Media (342 y 370 planes) |
| **1263** «split squats» → «Sentadillas búlgaras» | Nombre, instrucciones (pie trasero en el suelo) y GIF: **sentadilla dividida**. La búlgara exige el pie trasero elevado | **CORRECCIÓN OBJETIVA** (hecha en V46) | Baja (solo el nombre) |
| **1093** «push to run» → «Empujar para correr» | Instrucciones: flexión con impulso de rodillas. GIF: elevaciones de rodilla de pie. Cardio | ERROR COMPROBADO (instrucciones frente a GIF) · REVISIÓN VISUAL | Baja (cardio, solo lo que se muestra) |
| **13 aislados `KNEE_DOMINANT`** | Ver §4 y P15 (`E`) | ERROR COMPROBADO de uso · VALIDACIÓN PROFESIONAL | Media-alta |

## 4. Hallazgo nuevo de severidad alta: curl nórdico como «dificultad 1»

**Seis ejercicios son el curl nórdico** (rodillas apoyadas y tobillos anclados, cayendo hacia delante con control excéntrico de los isquios):
- 1195, 1196 y 1197 «self assisted inverse leg curl»;
- 327 «cable assisted inverse leg curl»;
- 915 «inverse leg curl (bench support)»;
- 916 «inverse leg curl (on pull-up cable machine)».

| Dato | Valor en el catálogo | Contradicción |
|---|---|---|
| Dificultad | **1** en los seis | Revisión visual asistida y nombre: un excéntrico exigente. **La dificultad la decide un profesional** |
| Equipamiento | 1195 y 1196: **peso corporal sin requisitos**. 1197: step. 915: banco. 327 y 916: polea | El GIF de 1195 muestra los tobillos anclados en un bastidor; el de 1196, bajo un banco. **Sin anclaje no se puede hacer** |
| Instrucciones | 1195: tumbado boca arriba llevando los muslos al pecho. 1196: «lie face down on a leg curl machine». 1197: puente con curl. 915 y 916: curl tumbado | **Ninguna describe el ejercicio del nombre y del GIF.** 1196 pide una máquina con equipamiento de peso corporal: ERROR COMPROBADO sin necesidad del GIF |

**Uso real:**

| Política | Planes con alguna variante | De principiante | En casa sin material |
|---|---:|---:|---:|
| REV2 | **650** | **289** | 252 |
| HIST (política por defecto actual) | **1.040** | **468** | 389 |

**Clase:**
- ERROR COMPROBADO: instrucciones frente a nombre y equipamiento.
- VALIDACIÓN PROFESIONAL: dificultad.
- REVISIÓN VISUAL: requisito de anclaje.

**Severidad ALTA:** un principiante puede recibir un ejercicio exigente con instrucciones de otro movimiento. **No se ha cambiado nada:** la dificultad y el equipamiento alteran la selección y requieren tu aprobación (`E`).

## 5. Detecciones sistemáticas en todo el catálogo activo (`escanear_catalogo.py`)

| Detección | Activos | Usados en REV2 | Lectura |
|---|---:|---:|---|
| S1 · Las instrucciones piden apoyo (silla, banco, step, máquina, barra) que el equipamiento no declara | 115 | 78 | **Sistémica:** casi todos son ejercicios «sentado» (press, elevaciones de gemelo) cuyo asiento no está modelado como equipamiento. Es una decisión de diseño del modelo de equipamiento, no un error por ejercicio. La excepción es 1320 (§3) |
| S2 · «Curl» en la traducción sin serlo en el original | 1 | 1 | 580 |
| S3 · Apertura (*fly*) sin `HORIZONTAL_ADDUCTION` o no aislada (sin contar las *reverse/rear fly*, que son de deltoide posterior y están bien clasificadas) | 1 | 1 | 580 |
| S4 · Curl nórdico | 6 | 5 | §4 |
| S5 · Aislado con patrón principal (slot) | 13 | 12 | P15 |
| S6 · Grupo espalda con un patrón que no es tracción, bisagra ni extensión, y que cuenta como dosis de espalda | 14 | 9 | **775** (1.441 planes), 1308 (370), 1261 (342), **1259 «standing archer»** (rotación de tronco, 225), **1146 «scapula dips»** (espalda con empuje, 89) y otros con poco uso |
| S7 · Grupo hombro con patrón de tronco | 1 | 1 | 973 |
| S8 · Flexión de tronco contada como espalda | 2 | 2 | 1261 y 1308 |

**Causa común de S6/S7/S8:** el código cuenta la dosis de un grupo por `body_part`, sea cual sea el patrón, siempre que la clase sea resistida.
- **Por eso un ejercicio de tronco puede ser «la espalda» o «el hombro» de un plan.**
- Es una regla del modelo de dosis, no un dato aislado. Cambiarla es una decisión deportiva (`E`).

## 6. Resumen por severidad

| Severidad | Hallazgos | Afecta a la selección real |
|---|---|---|
| **Alta** | Curl nórdico (6), 775, 973 | Sí: dificultad y equipamiento de 650/1.040 planes; dosis de espalda u hombro de 615/917 planes REV2 |
| Media-alta | 13 aislados como patrón de rodilla (P15) | Sí: 324 planes REV2 con la rodilla cubierta solo por aislados |
| Media | 1261, 1308, 1259, 1146 (dosis de espalda por tronco); 1320 (instrucciones); 1262 y 1115 (dificultad); 853 (nombre e instrucciones) | 853 y 1320, solo lo que se muestra; el resto, la dosis |
| Baja | 580, 1093 (nombres e instrucciones); 1263 y 973 (traducciones; **corregidas**) | No |
| Sistémica | Asiento no modelado (S1) | Posible inviabilidad en casa, no medida |

**Corregido en V46 (objetivo; solo el nombre que se muestra):** 973 y 1263. Todo lo demás está pendiente de revisión visual o de validación profesional (`B` y `E`).
