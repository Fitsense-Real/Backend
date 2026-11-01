# Corrección de las fichas profesionales (`FICHAS_NO_FACTIBLE.csv`)

**Rama:** `feature/c2d-v4c-versionado` (PR #17, borrador). **Commit de la corrección:** 254a18e.

## 1. Defecto

`docs/FitSense/cierre_final_pr17/FICHAS_NO_FACTIBLE.csv` declaraba **10 columnas**, pero las filas D01 y D02 tenían **11**.

**Causa:**
- El campo `sesion_requerida` contiene una coma: «PULL (espalda y biceps), dos veces por semana».
- Se escribió **sin comillas**, y cualquier lector CSV estándar lo partía en dos.
- A partir de ahí, cada valor siguiente caía una columna a la derecha:
  - `ejercicios_disponibles_en_la_zona` recibía «dos veces por semana»;
  - `decision_del_sistema` quedaba desplazada a una undécima columna sin cabecera.

**Origen:** el generador del laboratorio (`CierrePR17LabIT`, línea 603) concatenaba el texto sin escaparlo.

## 2. Corrección

| Elemento | Cambio |
|---|---|
| `cierre_final_pr17/FICHAS_NO_FACTIBLE.csv` | Se unió de nuevo el campo 7 y se reescribió con escapado RFC 4180 (comillas solo donde hace falta). Es la **única** evidencia anterior modificada, con autorización expresa de la orden |
| `CierrePR17LabIT` (línea 603) | El texto pasa por `q(...)`, que lo entrecomilla: si se regenera el laboratorio, no reaparece el defecto |
| Contenido | **Ningún valor ha cambiado**: solo el escapado |

## 3. Verificación

| Comprobación | Resultado |
|---|---|
| Columnas por fila (cabecera, D01, D02) | **10, 10, 10** |
| Parser estándar (Python `csv`) | 10 campos por fila |
| Parser RFC 4180 propio de la prueba | 10 campos por fila |
| D01 | GAIN_MUSCLE · INTERMEDIATE · 6 días · 75 min · peso corporal · `sesion_requerida` = «PULL (espalda y biceps), dos veces por semana» |
| D02 | INCREASE_STRENGTH · INTERMEDIATE · 6 días · 75 min · peso corporal · el mismo texto |
| Columnas posteriores | Cada valor en su columna: `ejercicios_disponibles_en_la_zona`, `patrones_disponibles`, `decision_del_sistema` no vacíos y coherentes |
| Códigos frente a la plantilla | D01 y D02 están en `PLANTILLA_RESPUESTAS_EVALUADOR.csv` y en la clave interna. Sin duplicados ni huérfanos |

## 4. Prueba automática

`src/test/java/main/web/services/fitsense/revision/PaqueteEvaluadorTest.java` (unitaria, se ejecuta siempre con `mvn test`).

| Prueba | Qué comprueba |
|---|---|
| `estructuraYCodigos` | Cada fila de los cuatro CSV tiene exactamente las columnas de su cabecera. 34 planes y 2 fichas (D01, D02), 36 códigos únicos. Plantilla = clave = planes + fichas, sin duplicados ni huérfanos. D01/D02 con cada valor en su columna. Cada ejercicio con nombre, descripción, patrón, músculo, series, dosis (repeticiones o segundos), descanso, enfoque, duración, sesión y orden |
| `paqueteCiegoYSinClave` | El paquete del evaluador no contiene la clave ni ningún término revelador (versión de política, estado de validación, motivo de selección) |

**Control negativo:** con el archivo anterior la prueba falla con «FICHAS_NO_FACTIBLE.csv fila 2 tiene 11 columnas y la cabecera 10». Con el corregido pasa (2/2).

## 5. Paquete del evaluador

`docs/FitSense/revision_profesional/paquete_evaluador/` contiene **solo** el material del evaluador:

| Archivo | Contenido |
|---|---|
| `MUESTRA_PROFESIONAL_CIEGA.csv` | 34 planes completos (copia idéntica byte a byte de la de `cierre_final_pr17/`) |
| `FICHAS_NO_FACTIBLE.csv` | D01 y D02, corregidas (copia idéntica) |
| `PLANTILLA_RESPUESTAS_EVALUADOR.csv` | 36 filas, **todas vacías** salvo el código (0 celdas con valor) |
| `INSTRUCCIONES_EVALUADOR.md` | Escala 1–5 y NS, criterios c1–c12, pregunta de duración, e1–e7, preguntas de D01/D02, G1–G5 y registro. Sin nombres de política, sin clave y sin umbrales de consenso internos |

**Fuera del paquete:**
- `CLAVE_INTERNA_MUESTRA.csv`, `INSTRUMENTO_PROFESIONAL_FINAL.md` (con umbrales internos) y las matrices, que siguen en `cierre_final_pr17/`.

**Ninguna evaluación profesional registrada:** no se ha inventado ninguna puntuación. La revisión profesional está **pendiente**, no completada.
