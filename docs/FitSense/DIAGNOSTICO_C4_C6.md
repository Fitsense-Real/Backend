# Diagnóstico C4/C6 · grupo y patrón de 8 ejercicios

**Fecha:** 30/10/2026. **Rama:** `c4-c6`, creada desde `generation` (`90350a0`). **Solo investigación:** no se cambia el catálogo oficial, no hay V40, no se tocan los validadores ni las referencias, y no se llama a Replicate.

**Tipo de evidencia.** Las observaciones visuales son una **revisión asistida por IA**: fotogramas de los GIF del dataset y lectura de las instrucciones. **No hay ninguna evaluación humana.** «Confirmado» significa «confirmado por la IA con evidencia concordante», no un consenso.

**Material** (todo en `evidencia/c4_c6/`):

| Archivo | Contenido |
|---|---|
| `matriz_c4_c6.tsv` | Clasificación actual frente a la propuesta, con evidencia, impacto y confianza |
| `uso_v39.md` | Uso de los 8 en las 72 referencias de V37 y V39, en el barrido y en los 192 escenarios |
| `variantes/*.sql` | Los prototipos (migración temporal V40 cargada desde fuera de `db/migration`, con comprobación del estado previo) |
| `resultados/<variante>/` | Salida de cada simulación |
| `comparativa_variantes.md` | Comparación con el control V39 (`comparar_c4c6.py`) |
| `cruces_grupo_slot_v39.tsv` | Los 24 ejercicios activos cuyo grupo contradice el slot que acredita (§6) |

Los GIF están en `hasaneyldrm/exercises-dataset` (`videos/<código>-*.gif`). Las hojas de fotogramas no se versionan por derechos de autor; se regeneran con `revision_visual/hoja_fotogramas.py`.

## 1. Cómo usa el motor el grupo y el patrón (criterio vigente, sin cambios)

- **El grupo** (`body_part`) decide en qué días puede entrar un ejercicio, según el enfoque del día. Por ejemplo, `waist` entra en FULL_BODY, LOWER_BODY y CORE. El grupo es también lo que cuenta V15.
- **El patrón** (`movement_pattern`) decide:
  - el **slot** que acredita (LOWER_KNEE, LOWER_HIP, UPPER_PUSH o UPPER_PULL), y con él la cobertura de V23;
  - la **clase**, que fija qué capacidades puede acreditar:
    - un patrón con slot es ESTRUCTURAL y puede acreditar fuerza, hipertrofia y resistencia;
    - uno sin slot es RESISTIDO y solo puede acreditar hipertrofia y resistencia.
- **El precedente C4** (V36 y la decisión D6) dice que en un ejercicio combinado domina el movimiento que desplaza el cuerpo. En sentadilla + curl o zancada + extensión, domina el tren inferior.

**Defecto típico.** Un ejercicio de tronco o de brazos con un patrón de pierna, o al revés, rinde doble en un FULL_BODY:
- aporta un grupo distinto, que es lo que pide V15;
- y a la vez acredita un slot de pierna.

El motor lo prefiere por eso, no porque trabaje lo que dice acreditar.

## 2. Los ocho casos

### 2.1 Primera prioridad

**1225 · smith hip raise: `waist` / `HIP_EXTENSION`**

| | |
|---|---|
| Movimiento (GIF 0756) | Tumbado boca arriba, con las piernas verticales; eleva la pelvis del suelo. Es un crunch invertido; no es un puente de glúteo |
| Grupo principal | Abdomen. Secundario: flexores de cadera |
| Clasificación actual | **Incorrecta en el patrón**. El grupo es correcto |
| Propuesta | Patrón `TRUNK_FLEXION`, como sus hermanos 865 (*hanging leg hip raise*) y 385 (*cable reverse crunch*) |
| Efecto | Deja de acreditar LOWER_HIP y fuerza. Pasa a RESISTIDO |
| Equipamiento, dificultad, dosis | Máquina Smith y suelo, sin cambios. Nivel 2 sin cambios: no se toca sin el instrumento. La dosis pasa a la de un ejercicio resistido de tronco, que es la que le corresponde |
| Uso | En las referencias, de 8 (V37) a **31** (V39). Siempre en FULL_BODY de gimnasio, intermedio y avanzado, en los 4 objetivos. En INCREASE_STRENGTH intermedio aparece 9 veces **como fuerza de cadera**. En el barrido aparece en días LOWER_BODY |
| Respuestas | Sí acredita una capacidad que no trabaja: «fuerza de cadera». Sí desplaza ejercicios más apropiados: con la corrección entra 878 (*glute bridge march*). Ayuda a pasar V15 de forma artificial, porque aporta `waist` y además cubre la cadera |
| Confianza | **CONFIRMADO (IA)**. Recomendación: **CORREGIR** |

**439 · cable squat row: `back` / `KNEE_DOMINANT`**

| | |
|---|---|
| Movimiento (GIF 1717) | Sentadilla y remo con cuerda a la vez. Se resaltan piernas y espalda |
| Clasificación actual | **Incoherente**. Cuenta como espalda para V15 y acredita LOWER_KNEE. Por eso aparece en días de **tirón** (PULL o UPPER_BODY del barrido) como si fuera un tirón, sin acreditar UPPER_PULL |
| Opción A (recomendada) | `upper legs` / `quads`, manteniendo `KNEE_DOMINANT`. Sigue el precedente C4 y a su hermano 145 (*band squat row*, `upper legs`/`KNEE_DOMINANT`) |
| Opción B | `back` / `HORIZONTAL_PULL`. Su argumento: la resistencia del cable es horizontal y solo la vence el remo |
| Efecto simulado | Con A deja de usarse (17 → 0) y en los días de tirón entran remos reales (518, 1037, 351). Con B baja a 1 uso |
| Equipamiento, dificultad, dosis | Cable, nivel 2. Sin cambios |
| Confianza | **PROBABLE (IA)**. Recomendación: **CORREGIR** con A, pendiente de tu elección del criterio para los remos en sentadilla. Ese mismo criterio decide 301 y 302, que siguen en C6 |

### 2.2 Segunda parte de la primera prioridad

**959 · kettlebell figure 8: `waist` / `HIP_HINGE`**

| | |
|---|---|
| Movimiento (GIF 0532) | Postura flexionada casi estática, pasando la kettlebell entre las piernas. Se resaltan abdomen y oblicuos. La cadera no hace una bisagra dinámica |
| Propuesta | Patrón `TRUNK_ROTATION_LATERAL_FLEXION`. `CORE_STABILITY` tiene el mismo efecto en el motor; queda a elección. El grupo `waist` es correcto |
| Efecto | Deja de acreditar LOWER_HIP. Uso: de 5 a 15, en FULL_BODY y LOWER_BODY de avanzados de gimnasio |
| Simulación | Si se corrige **solo**, lo sustituye 1225, otro dudoso. Hay que corregirlo junto a 1225 |
| Confianza | **PROBABLE (IA)**. La auditoría anterior no le vio error. Recomendación: **CORREGIR** |

**962 · kettlebell sumo high pull: `back` / `KNEE_DOMINANT`**

| | |
|---|---|
| Movimiento (GIF 0548) | Sentadilla sumo con impulso de piernas y jalón al mentón. Las piernas mueven el cuerpo. Tiene carga axial |
| Propuesta | `upper legs` / `glutes`, manteniendo `KNEE_DOMINANT`. Sigue el precedente C4 y la auditoría ya lo marcó PROBABLE. El patrón (`KNEE_DOMINANT` o `HIP_HINGE`) queda pendiente |
| Efecto | Uso de 0 a 6 en FULL_BODY de avanzados de gimnasio. Si se corrige solo, lo sustituye 439 |
| Confianza | **PROBABLE (IA)**. Recomendación: **CORREGIR** el grupo |

### 2.3 Segunda prioridad

**927 · kettlebell bent press: `waist` / `VERTICAL_PUSH`**

| | |
|---|---|
| Movimiento (GIF 0524) | La kettlebell se queda quieta arriba mientras el tronco se inclina de lado por debajo. Se resaltan los oblicuos. No hay un press real del brazo |
| Propuesta | `TRUNK_ROTATION_LATERAL_FLEXION`, como los *windmills* 926, 958 y 960. La auditoría proponía la alternativa `shoulders` / `VERTICAL_PUSH` |
| Efecto | Hoy acredita UPPER_PUSH y aparece en días LOWER_BODY |
| Confianza | **PROBABLE (IA)**. Recomendación: **CORREGIR el patrón**, o investigar más entre las dos opciones |

**1223 · smith incline shoulder raises: `chest` / `HORIZONTAL_PULL`**

| | |
|---|---|
| Movimiento (GIF 0759) | Tumbado en banco inclinado con los brazos extendidos: protracción escapular, un empuje de escápulas. **No es un remo** |
| Propuesta | `SHOULDER_RAISE`, como sus hermanos 178 y 590 (*incline shoulder raise*, `chest`/`serratus`/`SHOULDER_RAISE`) |
| Efecto | Hoy **acredita tracción (UPPER_PULL) en días de empuje**: en el barrido aparece en 17 sesiones, 13 de ellas PUSH y 4 UPPER_BODY. Con la corrección deja de hacerlo, sin dejar ningún perfil sin plan |
| Confianza | **CONFIRMADO (IA)**. Recomendación: **CORREGIR** |

**674 · dumbbell one arm snatch: `upper legs` / `VERTICAL_PUSH`**

| | |
|---|---|
| Movimiento (GIF 3888) | Arrancada: bisagra y triple extensión, con recepción por encima de la cabeza |
| Situación | La incoherencia es de **familia**: las arrancadas 175, 942 y 947 están en `shoulders`/`VERTICAL_PUSH` y las cargadas 551, 966 y 1129 en `upper legs`/`HIP_HINGE` |
| Uso | Mínimo (3), solo en avanzados sin restricciones |
| Recomendación | **INVESTIGAR MÁS**, decidiéndolo junto con su familia |

**162 · barbell clean and press: `upper legs` / `VERTICAL_PUSH`**

| | |
|---|---|
| Movimiento (GIF 0028) | Cargada desde el suelo, que es una bisagra, seguida de un **press estricto**. Son dos movimientos con peso real |
| Hermano | 943 (*clean and jerk*) está en `shoulders`/`VERTICAL_PUSH` |
| Uso | 1 aparición |
| Recomendación | **MANTENER por ahora** y decidirlo con la familia olímpica |

## 3. Simulaciones (base PostgreSQL aislada, V39 como control)

Para cada variante se corrieron:
- las 72 referencias;
- el barrido de 4 a 6 días;
- las 288 generaciones de robustez;
- los 192 escenarios de restricciones con el equipamiento real;
- la continuidad semanal de 96 perfiles.

No se ejecutó la suite completa: queda para el CI cuando se implemente.

| Variante | Referencias válidas | Perfiles que cambian | Barrido válido / COMPLETO | Robustez | 192 publicados | Implemento ausente / violaciones | Continuidad |
|---|---|---|---|---|---|---|---|
| Control V39 | 72/72 | — | 72/72 / 72/72 | 288/288 | 192/192 | 0 / 0 | OK |
| 1225 | 72/72 | 18 | 72 / 72 | 288 | 192 | 0 / 0 | OK |
| 439, opción A (grupo) | 72/72 | 22 | 72 / 72 | 288 | 192 | 0 / 0 | OK |
| 439, opción B (patrón) | 72/72 | 16 | 72 / 72 | 288 | 192 | 0 / 0 | OK |
| 959 | 72/72 | 11 | 72 / 72 | 288 | 192 | 0 / 0 | OK |
| 962 | 72/72 | 10 | 72 / 72 | 288 | 192 | 0 / 0 | OK |
| 927 | 72/72 | 7 | 72 / 72 | 288 | 192 | 0 / 0 | OK |
| 1223 | 72/72 | 16 | 72 / 72 | 288 | 192 | 0 / 0 | OK |
| **Combinada** (los 6 recomendados, 439 con A) | 72/72 | 22 | 72 / 72 | 288 | 192 | 0 / 0 | OK |
| Combinada + 953 | 72/72 | 22 | 72 / 72 | 288 | 192 | 0 / 0 | OK |

**Ninguna corrección deja perfiles sin plan ni rompe V15, V18, V20, V23, V24 o V25.** Todas las semanas siguen cubriendo las capacidades de su objetivo (COMPLETO).

**Distribución muscular.** El barrido marca las sesiones con avisos, que son datos para revisión, no fallos:

| Aviso | Control | Combinada |
|---|---|---|
| «Menos de 2 de pierna» / «abdomen iguala o supera a pierna» en un día LOWER_BODY | 6 | **0** |
| «Más de la mitad» | 24 | 29 |

Los 5 avisos «más de la mitad» de más son días de pierna cortos (2 ejercicios de trabajo) que pasan de «1 de pierna + 1 de abdomen» a «2 de pierna». En un día de pierna eso es lo correcto. **La combinada mejora la composición.**

## 4. El efecto en cadena (dependencia que obliga a ampliar la mirada)

Corregidos uno a uno, los dudosos **se sustituyen entre sí**:

| Si se corrige… | …entra en su lugar |
|---|---|
| 1225 | 927 y 959 |
| 959 | 1225 |
| 439 | 962 |
| 962 | 439 |
| Los seis a la vez | **953** (*kettlebell two arm clean*, `shoulders`/`HIP_HINGE`), 10 veces |
| Los seis + 953 | **262** (*barbell skier*, `shoulders`/`HIP_HINGE`), 11 veces |

La causa es la del §1: el motor prefiere en FULL_BODY cualquier ejercicio que aporte un grupo de tronco o de brazos y acredite a la vez un slot de pierna. **En el catálogo V39 hay 24 ejercicios activos con ese cruce** (`cruces_grupo_slot_v39.tsv`). Los ocho de esta orden son solo los que más se usan hoy.

**Lectura:**
- Corregir solo los ocho mejora los planes. Los ejercicios que entran son reales: puente de glúteo, remos, peso muerto.
- Pero el siguiente ejercicio cruzado ocupa el hueco. 953 y 262 son, al menos, bisagras reales; su incoherencia es solo de grupo.
- Cerrar C4/C6 de forma estable exige **decidir un criterio general**: el grupo debe ser coherente con el slot que acredita el patrón, salvo excepciones justificadas. Ese criterio se aplicaría a los 24 en una revisión posterior.

No se investigaron los otros 16 más allá de esta constatación, como pide la orden.

## 5. Riesgos y perfiles afectados

- **Perfiles afectados:** sobre todo FULL_BODY de gimnasio, intermedio y avanzado, de los cuatro objetivos (22 de 72 referencias cambian en la combinada). Ningún perfil de casa ni de principiante cambia por 1225, 439, 959, 962, 927 o 1223.
- **Adaptación y progresión:** 1225, 959, 927 y 1223 pasan de ESTRUCTURAL a RESISTIDO, y dejan de poder acreditar fuerza. Las pruebas de adaptación y progresión no se ejecutaron en esta fase. Deben pasar en el CI cuando se implemente, sin cambiar sus expectativas.
- **Dificultad:** no se propone ningún cambio de dificultad, ni sin el instrumento ni con él.
- **Equipamiento:** sin cambios. Los requisitos actuales coinciden con los GIF.
- **Riesgo de la opción A en 439:** si se prefiere el criterio de la carga, la opción es B. Las dos simulaciones son válidas.

## 6. Recomendación

| Decisión | Ejercicios |
|---|---|
| **Corregir ya** (confirmado por la IA) | **1225** → `TRUNK_FLEXION`; **1223** → `SHOULDER_RAISE` |
| **Corregir** (probable) | **439** → `upper legs`/`quads` (opción A); **959** → `TRUNK_ROTATION_LATERAL_FLEXION`; **962** → `upper legs`/`glutes`; **927** → `TRUNK_ROTATION_LATERAL_FLEXION` |
| **Mantener e investigar con su familia** | 674 y 162 |
| **Dependencia** (fuera de los 8, aparece al corregirlos) | 953 → `upper legs`/`glutes` (la auditoría ya lo marcó PROBABLE) |
| **Siguiente paso, para tu decisión** | Aprobar el criterio «grupo coherente con el slot» y revisar los otros 16 cruces (262, 267, 952, 922, 930, 971, 671, 796, 824, 862, 1086, 301, 302 y los cardios 54, 1174 y 1295). Además, añadir una prueba de invariante del catálogo que impida nuevos cruces sin excepción documentada |

## 7. Propuesta de implementación (solo si la autorizas)

1. Crear `V40__catalog_c4_c6.sql` con el mismo contrato de V38 y V39:
   - comprobación del estado previo de cada columna, y aborto si difiere;
   - comprobación del estado final.

   Su contenido serían los prototipos de `evidencia/c4_c6/variantes/combinada953.sql`, o la variante que elijas.
2. Pruebas dirigidas: una `CatalogoV40IT` con los valores nuevos y los slots acreditados, más las pruebas que dependan de esos ejercicios.
3. Referencias:
   - congelar la de V39 como `generacion_referencia_v39_motor_e1e2.txt`, protegida;
   - regenerar la oficial y documentar las diferencias.
4. CI completo en GitHub y las simulaciones extensas una vez, sobre el commit final.
