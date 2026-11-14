# Protocolo de la segunda revisión visual independiente (cierre del catálogo)

Eres un revisor **independiente**. No tienes ni debes buscar las revisiones anteriores. Tu trabajo es comprobar, contra la **animación completa** del GIF original, cada atributo **final** de los ejercicios de tu lote, tal como quedarían instalados tras las migraciones V47–V50.

## Material de tu lote

| Material | Contenido |
|---|---|
| `manifiesto.md` | Una ficha por ejercicio con los atributos finales: nombre en español, nombre de la fuente en inglés (puede ser erróneo), zona, músculo objetivo y secundarios, patrón, prescripción, dificultad, equipo y requisitos, banderas, impacto e indicador de estiramiento derivados por regla, e instrucciones en español. |
| `../../hojas/ej_NNNN.png` | Hoja del ejercicio NNNN con **todos** los fotogramas del GIF (f1…fN, normalmente 12; algunos tienen 18 o 24) y la imagen fija. |

Si necesitas ver un fotograma más grande, puedes recortarlo y ampliarlo con Python y PIL a partir del GIF original del dataset. La ruta del GIF figura en la cabecera de la hoja, dentro de `/tmp/claude-0/-home-user/d726e240-bf6c-5000-9fec-bb22a3143d76/scratchpad/cati/ds/`.

**Mira todos los fotogramas antes de decidir.** Muchos GIF son fundidos entre dos o tres poses clave: describe solo lo que se ve. No inventes fases que no aparecen. Si una fase relevante no se ve, como un vuelo, un agarre o una entrada a la posición, dilo y baja la confianza.

## Definiciones oficiales (comentarios de las migraciones)

### Banderas de seguridad

| Bandera | Columna | Significado |
|---|---|---|
| **HI** | `high_impact` | Saltos, pliometría y levantamientos olímpicos. |
| **FL** | `requires_floor` | Exige bajar al suelo y levantarse: tumbado, sentado o a cuatro apoyos en el suelo. No cuenta tumbarse en un banco. |
| **AX** | `axial_load` | Carga compresiva relevante sobre la columna o el cuello: barra en la espalda, cargas pesadas sobre los hombros, cargas en press por encima de la cabeza… No incluye la posición invertida. |
| **KN** | `requires_kneeling` | Una rodilla apoya y carga peso, en el suelo, un banco o una almohadilla. |
| **HG** | `requires_hanging` | El cuerpo cuelga del agarre con los pies sin apoyo. Incluye las versiones asistidas. |
| **OH** | `requires_overhead` | Brazos por encima de la cabeza en alguna fase, empujando, tirando o sin carga. |
| **INV** | `requires_inversion` | Cabeza claramente por debajo de la cadera, con el peso en brazos, hombros o cabeza. |

### Apoyo suspendido

No tiene bandera propia. Anótalo cuando el cuerpo **no tiene los pies apoyados** y se sostiene **por encima** del apoyo, sin colgar:

| Clase | Ejemplos |
|---|---|
| BRAZOS_EXTENDIDOS | Fondos en paralelas o anillas |
| ANTEBRAZOS | Silla romana o capitán, con los antebrazos en almohadillas |
| PIES_SUSPENDIDOS | Pies en correas o anillas con las manos en el suelo |
| MAQUINA_PARCIAL | Máquina asistida de rodillas o de pie sobre una plataforma |

### Impacto

| Nivel | Criterio |
|---|---|
| BAJO | Siempre al menos un pie en el suelo, o sin cargar el peso corporal. |
| MODERADO | Vuelo de baja amplitud, bilateral y repetitivo, o carrera por debajo del sprint: trote, skipping, jumping jacks, comba. |
| ALTO | Saltos, pliometría, olímpicos o sprint. |

Si no hay ningún fotograma con los dos pies en el aire pero el ritmo y la postura corresponden a una carrera, como un trote en el sitio con talón de apoyo elevado y braceo, anota MODERADO y explica la duda.

### Patrones (V26)

KNEE_DOMINANT, HIP_HINGE, HIP_EXTENSION, HIP_ABDUCTION_ADDUCTION, ANKLE_PLANTAR_FLEXION, HORIZONTAL_PUSH, VERTICAL_PUSH, HORIZONTAL_PULL, VERTICAL_PULL, SHOULDER_RAISE, HORIZONTAL_ADDUCTION, SHOULDER_EXTENSION, SCAPULAR_ELEVATION, ELBOW_FLEXION, ELBOW_EXTENSION, WRIST_FOREARM, TRUNK_FLEXION, TRUNK_EXTENSION, TRUNK_ROTATION_LATERAL_FLEXION, CORE_STABILITY, LOCOMOTION_CARDIO, MOBILITY, BALANCE_CONTROL, OTHER.

Una sola clase por ejercicio: la del movimiento principal. LOCOMOTION_CARDIO es para el cardio rítmico continuo; MOBILITY, para los estiramientos y la movilidad.

### Prescripción

- **SETS_REPS:** repeticiones de un movimiento dinámico.
- **DURATION:** sostener una posición o una actividad continua por tiempo, como estiramientos, planchas o cardio.

### Dificultad

| Nivel | Perfil |
|---|---|
| 1 | Asequible para un principiante sin experiencia: postura estable, técnica simple, poca coordinación, sin soportar el peso corporal en suspensión. |
| 2 | Exige técnica básica o experiencia: peso libre con coordinación moderada, equilibrio a una pierna, fuerza relativa moderada. |
| 3 | Avanzado: fuerza relativa alta (dominadas, fondos, muscle-up), técnica compleja (olímpicos), pliometría exigente, equilibrio o suspensión exigentes. |

Tu opinión se registra, pero **solo puedes proponer**. Bajar una dificultad nunca se aplica sin aprobación humana (V27).

### Equipo

Lo **imprescindible** para hacer el movimiento del GIF. Los requisitos usan grupos: el mismo grupo significa alternativas (O) y grupos distintos significan que hacen falta todos (Y). Un accesorio que aparece en el GIF pero que no es imprescindible no es un error: anótalo en las observaciones.

### Estiramiento

SI cuando el ejercicio es un estiramiento: mantener una posición de elongación, aunque tenga balanceos suaves. NO en cualquier otro caso.

## Qué decides por ejercicio

Una fila por id del manifiesto, en `revision2.tsv` dentro de tu carpeta de lote. UTF-8, separado por TAB, sin tabuladores ni saltos de línea dentro de los campos. Cabecera exacta:

```
id	identidad	movimiento_observado	fotogramas_clave	pies	impacto	cuelga	apoyo_suspendido	brazos_sobre_cabeza	suelo	rodilla_apoyada	invertido	carga_axial	banderas_correctas	banderas_anadir	banderas_quitar	nombre	nombre_propuesto	instrucciones	problema_instrucciones	instrucciones_propuestas	equipo	equipo_propuesto	zona_objetivo	zona_objetivo_propuesto	patron	patron_propuesto	prescripcion	estiramiento	dificultad	exigencias	confianza	observaciones
```

### Hechos observados en el GIF

| Campo | Valores |
|---|---|
| `identidad` | CLARA, AMBIGUA o NO_IDENTIFICABLE |
| `movimiento_observado` | Una frase: posición inicial → movimiento → final, implemento y apoyos |
| `fotogramas_clave` | Por ejemplo, `f1 inicio; f7 final` |
| `pies` | AMBOS_SIEMPRE, ALTERNOS_SIN_VUELO, FASE_DE_VUELO, SIN_APOYO_DE_PIES o NO_APLICA (tumbado o sentado) |
| `impacto` | BAJO, MODERADO o ALTO |
| `cuelga` | SI o NO |
| `apoyo_suspendido` | NO o una de las cuatro clases |
| `brazos_sobre_cabeza` | SI o NO, con el fotograma si es SI |
| `suelo` | SI o NO |
| `rodilla_apoyada` | SI o NO |
| `invertido` | SI o NO |
| `carga_axial` | SI, NO o DUDA |

### Banderas

| Campo | Valores |
|---|---|
| `banderas_correctas` | SI o NO, comparando las banderas finales del manifiesto con lo observado |
| `banderas_anadir` | Las que faltan, por ejemplo `OH AX`, o `-` |
| `banderas_quitar` | Las que sobran, o `-` |

### Textos

| Campo | Valores |
|---|---|
| `nombre` | OK, NO o DUDA |
| `nombre_propuesto` | Texto si `nombre` es NO; si no, `=` |
| `instrucciones` | OK, NO o DUDA |
| `problema_instrucciones` | `-` o la contradicción concreta: qué dice el texto y qué muestra el GIF |
| `instrucciones_propuestas` | Si `instrucciones` es NO, pasos separados por ` \|\| `; si no, `=` |

### Clasificación

| Campo | Valores |
|---|---|
| `equipo` | OK, NO o DUDA |
| `equipo_propuesto` | `=` o la propuesta, en el formato `principal; requisitos` |
| `zona_objetivo` | OK, NO o DUDA |
| `zona_objetivo_propuesto` | `=` o la propuesta |
| `patron` | OK, NO o DUDA |
| `patron_propuesto` | `=` o una clase de la lista |
| `prescripcion` | OK o NO |
| `estiramiento` | SI o NO |
| `dificultad` | OK, SUBIR, BAJAR o DUDA |
| `exigencias` | Por ejemplo, `fuerza relativa alta; equilibrio a una pierna`, o `-` |
| `confianza` | ALTA, MEDIA o BAJA |
| `observaciones` | Cualquier cosa relevante, o `-` |

## Reglas de redacción (solo cuando el campo es NO)

1. **Solo lo que muestra el GIF.** Si no ves algo, como el agarre o la altura, no lo afirmes.
2. **Nada que contradiga lo observado.** No escribas «túmbate en el suelo», «cuélgate», «brazos por encima de la cabeza» o «apoya la rodilla» si el GIF no lo muestra. Si lo muestra, dilo con claridad.
3. **Sin dosis concretas.** Termina con «Repite el número de repeticiones indicado.» (SETS_REPS) o «Mantén la duración indicada.» (DURATION). Si el GIF alterna, añade «Alterna los lados».
4. **Conserva las advertencias de seguridad** que tenga el texto actual si siguen siendo ciertas, como comprobar el anclaje o mantener las rodillas alineadas.
5. **Estilo:** español neutro, segunda persona, imperativo, de 3 a 6 pasos, comprensible para un principiante.

## Prohibiciones

- No marques OK por parecido de nombre: compara con los fotogramas.
- No inventes evidencia. Si no puedes decidir, usa DUDA o AMBIGUA, baja la confianza y explica por qué.
- No leas otras carpetas de lotes ni ficheros de revisiones anteriores.

## Comprobación final

Antes de terminar, comprueba con un script que el TSV tiene exactamente una fila por id del manifiesto, 33 columnas por fila y valores dentro de los dominios. Corrige lo que falle.
