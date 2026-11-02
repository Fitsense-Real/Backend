# Propuesta de corrección del catálogo

**Estado: PROPUESTA.**
- **No se ha modificado ningún registro.**
- No hay migración de catálogo en esta rama.
- Las restricciones se aplican de forma **funcional y reversible** con EF-0.1, que está desactivada por defecto.
- Cada cambio de esta lista necesita aprobación y, donde se indica, revisión profesional.

**Fuentes:**
- `ESCANEO_CATALOGO_ACTIVO.csv`: escaneo automático de los **1.219 ejercicios activos**, con zona frente a patrón, tronco fuera de `waist`, material en el texto con equipo de peso corporal y requisitos de equipo de V39.
- `MATRIZ_CATALOGO_INCONSISTENCIAS.csv`: clasificación fila a fila.
- Lectura de los 7 registros señalados (instrucciones completas en `implementacion_controlada_c2d_v4c/CATALOGO_CASOS_EXTREMOS.csv`).
- **No hay revisión visual de vídeo ni GIF** en esta orden.

## 1. Los 7 ejercicios señalados

| ID | Registro | Lo que dicen las instrucciones | Clase | Propuesta |
|---|---|---|---|---|
| 775 | «Elevación de codo y push-up invertido», back, peso corporal, HORIZONTAL_PUSH, objetivo upper back | «Tumbado boca abajo… presiona con las palmas para levantar la parte superior del cuerpo… apretando los músculos de la espalda alta». El texto describe un empuje desde el suelo. El nombre inglés (*elbow lift – reverse push-up*) suele designar un ejercicio en **decúbito supino** empujando con los codos (extensión de hombro o retracción escapular) | **Inconsistente** (zona y objetivo frente a patrón y texto) | Revisión profesional del movimiento real. Ni la versión del texto ni la del nombre es una **tracción**, así que **no debe contar en PULL** con ninguna lectura (lo aplica EF). Si es supino: SHOULDER_EXTENSION, zona back. Si es prono: HORIZONTAL_PUSH, zona chest |
| 1066 | «Curl de espalda baja», back, TRUNK_EXTENSION | Extensión lumbar en prono | **Correcto** (patrón y zona) | Ninguna. En PULL cuenta como complemento de la familia, **no como tracción** |
| 1259 | «Arquero de pie», back, TRUNK_ROTATION_LATERAL_FLEXION, dificultad 2 | Rotación de tronco de pie con los brazos extendidos, sin carga | **Inconsistente** (tronco con zona back; sin resistencia) | Reclasificar como MOVILIDAD o zona waist, tras revisión. Hoy EF no lo cuenta como resistido |
| 1261 | «Inclinación pélvica de pie», back, TRUNK_FLEXION | Basculación pélvica de pie, sostenida unos segundos | **Requiere revisión profesional** (control motor o movilidad, no resistido) | Probable MOBILITY. EF no lo cuenta como resistido |
| 1308 | «Toque de dos dedos de los pies», back, TRUNK_FLEXION | Flexión de tronco para tocar la punta de los pies con las piernas rectas | **Requiere revisión profesional** (estiramiento de isquiotibiales) | Probable MOBILITY. EF no lo cuenta como resistido |
| 315 | «Curl de concentración de bíceps con pierna», upper arms, **peso corporal**, ELBOW_FLEXION | «Siéntate en un **banco**… Sujeta una **mancuerna**…» | **Inconsistente** (equipo) | Equipo `dumbbell` (y requisito de banco) o sustituir las instrucciones por la versión sin material. EF lo excluye (lista cerrada) |
| 320 | «Curl de bíceps acostado de lado con peso corporal», ELBOW_FLEXION | Flexión de codo sin carga externa | **Requiere revisión profesional** (patrón coherente; su capacidad como hipertrofia no está demostrada) | Decidir si queda como RESISTIDO o pasa a movilidad o activación. **EF no lo excluye** |

## 2. Resultado del escaneo sistemático (1.219 activos → 52 filas marcadas)

Clasificación fila a fila en `MATRIZ_CATALOGO_INCONSISTENCIAS.csv`:
- **18** Inconsistente
- **17** Correcto: falsos positivos o requisito de equipo ya registrado
- **12** Requiere revisión profesional
- **5** Información insuficiente

| Señal | Filas | Resumen |
|---|---|---|
| Patrón de tronco (flexión, rotación, estabilidad) con zona ≠ waist | 17 | **Estiramientos o movilidad clasificados como resistidos:** toques de pies 68, 254, 882 y 883, el giro 310, y 1259, 1261 y 1308. **Core con zona incorrecta:** 875, 901 y 973. **Habilidades con barra donde la zona espalda puede ser correcta:** 65, 873 y 808 |
| Empuje con zona espalda | 3 | 775 (§1), 231 (pull-over a press) y 1146 (fondos de escápula: el texto no coincide con el equipo) |
| Material en el texto con equipo de peso corporal | 31 | §3 |

**Riesgo de EF-0.1 (exclusión excesiva).**
- 65 (*back lever*), 873 (*front lever*) y 808 (extensión de espalda con giro en balón) tienen zona `back` y patrón de tronco. EF no los cuenta como resistidos.
- Pueden ser trabajo legítimo de espalda.
- Todos exigen material (barra o balón): **no afectan al PULL sin material**.
- Su clasificación queda para la revisión profesional.

## 3. Material en el texto con equipo de peso corporal (31)

| Situación | IDs | Clase |
|---|---|---|
| Requisito ya registrado en el catálogo (barras de pared, anclaje, banco, GHD) y texto coherente | 304, 307, 504, 505, 880, 899, 904, 905, 908, 1072 | **Correcto** |
| Requisito barras de pared, pero **el texto pide sujetar una mancuerna** | 303, 305 | **Inconsistente** (instrucciones) |
| Falso positivo: la palabra es una comparación («como si te sentaras en una silla») o viene del nombre en inglés (*step*) | 54, 853, 1102, 1103, 1177, 1195 | **Correcto** |
| **El texto exige material** sin requisito: mancuerna y banco (315), banco o paralelas (318), paralelas (1152), máquina de curl femoral (1196), anillas (1298), banda (1077) | 315, 318, 1077, 1152, 1196, 1298 | **Inconsistente** (falta el requisito o el equipo es incorrecto) |
| Silla, banco o pared doméstica como apoyo | 465, 1114, 1136, 1188, 1320 | **Información insuficiente**: decisión de producto (¿se presupone una silla?) |
| Patrón dudoso además del material | 939 | **Requiere revisión profesional** |
| Banco opcional (esterilla), con zona incoherente | 901 | Ya cuenta en la fila de tronco |

**Lo que EF-0.1 hace con ellas:**
- **Solo excluye 315.** Es la única cuyo registro dice explícitamente «peso corporal» y pide una mancuerna.
- Las demás inconsistencias de material (318, 1152, 1196, 1298, 1077) **siguen siendo elegibles con peso corporal**. Excluirlas es una decisión que requiere aprobación (añadir requisitos de equipo como en V39, o la lista cerrada de EF).
- **1152 y 1320 aparecen en los PUSH sin material de los casos extremos.** Con su requisito, esos PUSH tendrían todavía menos contenido.

## 4. Lo que NO se propone

- **Añadir ejercicios nuevos** (p. ej. un remo sin material): la orden lo prohíbe sin validación.
- **Asumir material no declarado** (mesa, puerta, barra, anclaje) para dar tracción al PULL sin material. El catálogo ya ofrece tracción **con** el material correspondiente (V39). Un usuario que lo tenga puede declararlo.
- **Cambiar dificultades.**
- **Reclasificar en la base de datos** antes de la revisión profesional.

## 5. Impacto (sobre los 960 perfiles)

Comparación del catálogo original frente a las restricciones funcionales: `MATRIZ_COMPARACION_FUNCIONAL.csv` e `INTERACCION_C2D_CATALOGO.md`.

- No se han encontrado **alternativas verificadas** de tracción con peso corporal sin material: ninguna está en el catálogo elegible de esos perfiles.
- Por eso, la tercera configuración pedida («alternativas verificadas») coincide con la de las restricciones funcionales.
