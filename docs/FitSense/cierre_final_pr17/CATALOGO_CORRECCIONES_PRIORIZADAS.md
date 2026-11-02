# Correcciones del catálogo priorizadas (propuesta pendiente de aprobación)

**Estado:**
- **No se ha modificado ningún registro**, ni en local ni en remoto.
- La auditoría de los 1.219 ejercicios activos se conserva sin cambios: `correccion_elegibilidad_funcional/ESCANEO_CATALOGO_ACTIVO.csv` y `MATRIZ_CATALOGO_INCONSISTENCIAS.csv`.
- Esta propuesta **no** modifica en bloque los 52 registros marcados. Los ordena por **impacto funcional real**: colocaciones en los planes válidos de los 960 perfiles (`USO_EJERCICIOS_MARCADOS.csv`).

## 1. Criterio de prioridad

| Prioridad | Criterio |
|---|---|
| **P1** | **Inconsistente**, usado en planes válidos de la política histórica **y** de C2-D-EF (hoy sale en planes reales) |
| **P2** | Inconsistente o con información insuficiente, usado en planes válidos, pero EF-0.1 ya lo neutraliza; o una decisión de producto con uso alto |
| **P3** | Requiere revisión profesional, con uso |
| **P4** | Sin uso en la matriz (no elegible o nunca elegido) |

Las clases (Inconsistente, Requiere revisión profesional, Información insuficiente) son las de la matriz anterior y **se mantienen separadas**.

## 2. Propuesta priorizada

**Colocaciones en planes válidos:** HIST / C2-D / C2-D-EF.

| Prior. | ID | Ejercicio | Clase | Colocaciones | Corrección mínima propuesta | Tipo de cambio | Prueba de regresión necesaria |
|---|---|---|---|---|---|---|---|
| **P1** | 775 | Elevación de codo y push-up invertido | Inconsistente | 494 / 565 / 422 | Revisión del movimiento real; luego **zona** (chest si es un empuje prono) o **patrón** (SHOULDER_EXTENSION si es en supino) | Patrón o zona | Matriz 960 (los UPPER_PUSH y FULL_BODY donde aparece), V23 y V25 |
| **P1** | 318 | Levantamiento de cuerpo | Inconsistente (exige banco o paralelas) | 109 / 129 / 107 | Requisito de equipo «superficie elevada» (como V39) o retirar del peso corporal | Equipamiento | Matriz de peso corporal (PUSH sin material) |
| **P1** | 1152 | Fondos invertidos | Inconsistente (exige barras paralelas) | 80 / 102 / 80 | Requisito `parallel bars` | Equipamiento | Ídem; afecta al PUSH sin material de los casos extremos |
| **P1** | 1196 | Curl de piernas invertido asistido | Inconsistente (texto con máquina de curl femoral) | 79 / 104 / 82 | Corregir el equipo (máquina) **o** sustituir las instrucciones por la versión sin máquina | Equipamiento o instrucciones | Matriz de LEGS y LOWER_BODY |
| **P1** | 1077 | Camino monstruo | Inconsistente (exige banda) | 5 / 5 / 5 | Equipo `band` | Equipamiento | Matriz de pierna |
| **P2** | 1114, 1320 | Sentadilla con apoyo, dip de tríceps en suelo | Información insuficiente (silla) | 91 y 84 / 107 y 104 / 84 y 88 | **Decisión de producto**: ¿se presupone una silla doméstica? Si no, requisito de equipo | Equipamiento | Matriz de peso corporal |
| **P2** | 973 | Gancho de boxeo | Inconsistente (tronco con zona hombros) | 178 / 210 / **0** | Pasar a cardio, o a zona waist | Clase o zona | EF ya lo neutraliza; regresión de LW (cardio) |
| **P2** | 315 | Curl de concentración con pierna | Inconsistente (mancuerna y banco) | 68 / 100 / **0** | Equipo `dumbbell` (+ banco) | Equipamiento | EF ya lo excluye (lista cerrada) |
| **P2** | 1259, 1308, 1261 | Arquero, toque de pies, inclinación pélvica | Inconsistente / revisión | 60, 72, 69 / 91, 103, 98 / **0** | Pasar a MOBILITY tras revisión | Clase | EF ya los neutraliza |
| **P2** | 901, 310, 68, 254, 882, 883, 875 | Toques de pies, giros, core con zona incorrecta | Inconsistente | 2–15 / 2–15 / **0** | MOBILITY o zona waist | Clase o zona | EF ya los neutraliza |
| **P3** | 320 | Curl de bíceps sin carga | Requiere revisión profesional | 65 / 98 / 10 | Decidir si acredita hipertrofia | Capacidad | V25 en PULL sin material |
| **P3** | 939 | Abdominal patada | Requiere revisión profesional | 93 / 105 / 91 | Revisar patrón (¿flexión de cadera o tronco?) y material (banco o silla) | Patrón o equipamiento | Matriz de LEGS |
| **P3** | 1146, 231 | Fondos de escápula, pull-over a press | Requiere revisión profesional | 36 y 6 / 37 y 7 / 32 y 6 | Movimiento real y patrón | Patrón | Matriz de gimnasio |
| **P3** | 1313, 1314 | Patadas de nadador, puente con giro | Requiere revisión profesional | 8 y 11 / 8 y 12 / 0 | Patrón (¿TRUNK_EXTENSION o HIP_EXTENSION?) | Patrón | — |
| **P3** | 1136, 1188 | Estiramientos sentados | Información insuficiente (silla) | 26 y 19 / 26 y 19 / 26 y 15 | Igual que 1114 | Equipamiento | — |
| **P4** | 303, 305 | Remos de pie (requisito barras de pared) | Inconsistente (texto con mancuerna) | 0 | Corregir las instrucciones | Instrucciones | Ninguna (no cambian la elegibilidad) |
| **P4** | 1298 | Maltés en apertura | Inconsistente (anillas) | 0 | Requisito `gymnastic rings` | Equipamiento | — |
| **P4** | 65, 873, 808, 1294, 465 | Palancas, extensión con balón, balanceo, estiramiento en silla | Revisión / información insuficiente | 0 (no elegibles en ningún perfil de la matriz) | Revisión profesional; **la posible exclusión excesiva de EF no tiene efecto hoy** | — | — |

## 3. Cómo se aplicaría una corrección aprobada

1. **Una migración nueva por lote** (V46 en adelante), nunca editando una existente. Con una tabla de cambios con antes y después, como V39, para la trazabilidad.
2. **Prueba de esquema** como `MigracionV44IT`: solo cambian los registros previstos.
3. **Regresión:**
   - matriz de 960 perfiles con las cinco políticas;
   - campaña REPLAY;
   - comparación perfil a perfil del antes y el después.

   Se documenta cada plan que cambia.
4. **Sin aplicar a la base remota** hasta aprobarlo.

## 4. Lo que no se propone

- Añadir ejercicios.
- Cambiar dificultades o dosis.
- Reclasificar en bloque.
- Retirar de forma unilateral ejercicios con información insuficiente.
