# Verificación final: dolor, decisiones de seguridad, V53-B, 775, plan 78, V54 y fichas (orden de 14/11/2026, fases 2–6)

No se ha vuelto a revisar ningún ejercicio; se reutiliza la evidencia válida (registro, expediente, fichas y GIF ya adjudicados). No se ha activado ningún interruptor. No se ha inventado ninguna validación profesional: todas siguen en **NINGUNA**.

## Fase 2 · Garantías ante dolor

Cada garantía, con su caso **positivo** (la salida legítima) y su caso **negativo** (la violación se rechaza), y la prueba que lo exige.

| Garantía | Positivo | Negativo | Mutación que lo rompe |
|---|---|---|---|
| Lo que dolió no reaparece | `HerenciaD8IT.dolorConRegeneracionSegura` (9004); `RegeneracionPorColocacionRev2IT` 9701 y 9710 | `SafeRegenerationService` rechaza `EXCLUIDO_REAPARECE` | M3 (`REGRESIONES_V52_1.md` §4): falla «reaparece lo que dolió» |
| No se añade trabajo sin autorización: total **y por colocación** | 9004, 9701, 9710: ninguna colocación sube y el total no sube (`ComparadorDosisPorColocacion`) | `dolorNoAprovechaD8` (9001): NO_FACTIBLE «sube el trabajo». **HALL-GM-01** (9703): el sustituto ya no recibe la base sobre un mantenimiento → NO_FACTIBLE | M1 y M4; la propia HALL-GM-01 sin la corrección (la prueba 9703 falla) |
| La IA nunca publica una adaptación inválida | En 9004 y 9703 el intento de la IA sigue NO_FACTIBLE; publica solo el motor determinista o nadie | 9703: tres rechazos de V24 a la propuesta de la IA, registrados por V32 | — |
| La regeneración solo publica semanas enteramente válidas y con dosis admisibles | 9004: `ValidacionIndependiente` = `VALIDA`, todas las dosis `BASE`, 0 fechas reducibles | `conBarraDosisInadmisibleSeRechaza` (V24) | M2 y M5: `INVALIDA [V24 …]` |
| Sin progresión indebida, restauración automática ni reincorporación no autorizada | 9701: las colocaciones a mantenimiento siguen a 2x10; 9710: el sustituto que existe reducido otro día no restaura esa colocación | Comparador: `restauracionDeUnaReduccion` (2x10 → 3x10 el mismo día se detecta aunque el total baje). `ProgresionIT`, `ProgresionRev2IT`, `Reincorporacion4DTest` y `1(a) · solo una PROGRESION posterior…` | — |
| Se respetan las restricciones del usuario | Los sustitutos y la regeneración salen del conjunto elegible del perfil (mismo `SafetyProfile`) | `AdaptacionRegresionIT.sinBrazosArribaSeSustituye`, `sinBarraSeSustituye`; O12 (7) | — |
| NO_FACTIBLE si no hay alternativa segura | — | 9001 y 9703: sin plan nuevo, semana medida intacta, exclusión activa, motivo registrado (`safe_regeneration_attempts`) | — |

**Riesgo residual (documentado, sin corregir):**
- P-03 nivel 2 compara el trabajo total y no por colocación.
- No se ha observado ninguna restauración por esa vía:
  - en REV2 ningún participante publica por P-03 nivel 2;
  - en PLAN-HIST-1, el caso 9004 pasa la comparación por colocación.
- Endurecerlo es un cambio de política de P-03. Queda como propuesta (`HALL_GM_01.md` §7).

## Fase 3 · Las decisiones de seguridad abiertas

**Tabla completa:** `CLASIFICACION_SEGURIDAD.tsv`. Script reproducible: `herramientas/clasificar_seguridad.py`, que solo lee el registro y el expediente. Son 59 filas: las 43 de prioridad 1 y 16 casos adicionales del expediente.

| Categoría | Filas | Cuáles | Qué significa |
|---|---|---|---|
| Error técnico demostrable | **1** (y 5 más ya cerradas antes: 231, 367, 1182, 1017, 620) | 1323 (texto de los «rodillos», corregido en V52.1) | Ya corregido; no queda decisión de seguridad |
| Restricción conservadora ya aplicada | **41** | INC-4C-03: 813, 858, 861, 873, 885, 1192, **1294** y 1298. INC-CAT-C39: 131, 400 y 448. HI en 854 (D2), 1083–1085, 1087, 1133, 1134, 1175, 1177, 922, 930, 937, 971 y 1068. INV en 171, 377, 840, 841 y **868**. INC-CAT-1374. 1069 (HG) | Protege a quien declara la restricción; queda la confirmación profesional |
| Caso del propietario | **6** | A-V53, 1153, 1301, 1303, 1304 y 1306 | Significado de «no colgarse». Todos menos A-V53 están contenidos por INC-CAT-C39 |
| Caso profesional | Todas menos A-V53 | — | Ninguna tiene validación profesional. Una revisión hecha por la IA no cuenta como tal |
| **Elegible con incertidumbre material** | **11** | Los **10 de apoyo suspendido** (457, 458, 459, 462, 777, 920, 934, 1184, 1317 y 1335) y **1113** | Llegan hoy a perfiles con la restricción pertinente, sin ninguna contención |

**Prioridades de la orden:**

| Caso | Estado verificado |
|---|---|
| 1113 | Dificultad 1 vigente (dictamen V39). R2 y V3 ven nivel 2. Elegible en 120 de 168 perfiles, también para principiantes. **No se cambia**: la dificultad no se toca para ganar ni para perder factibilidad sin decisión profesional. Es incertidumbre material |
| 1083–1087 | Contenidos por HI. La contradicción de 1083 se resolvió de forma conservadora. 1086 solo tiene decisiones de identidad y texto. 0 de 168 perfiles |
| 854 (D2) | HI conservado (decisión histórica D2). Elegible en 40 de 168 perfiles, solo para quien no restringe el impacto |
| 868 | INV añadido en V52.1. Contenido por INV, HG y OH |
| 1294 | Retenido (INC-4C-03): identidad ambigua. 0 de 168 perfiles |
| 1323 | Texto corregido (V52.1). Cerrado |
| INC-4C-03 | 8 retenidos. 813, 858, 861, 873 y 885 cumplen el criterio de salida (propuesta de liberación); **no se liberan** sin profesional |
| INC-CAT-C39 | 131, 400 y 448 para OH; 1153, 1301, 1303, 1304 y 1306 para «no colgarse». Vigente |
| Los 10 de apoyo suspendido | Elegibles para «no colgarse». Contenerlos sería activar V53, y la orden lo prohíbe. V53-B queda preparado y apagado (fase 4) |

**No se ha liberado ningún ejercicio contenido ni se ha cambiado ninguna dificultad.**

## Fase 4 · V53, opción B

Ver `cierre/v53b/PROPUESTA_V53B.md` en la rama `propuesta/v53b-permiso-apoyo-suspendido`. Es una propuesta independiente, con el interruptor apagado, que no se ha fusionado.

## Fase 5 · 775, plan 78 y V54

### 775

| Comprobación | Resultado |
|---|---|
| ¿Hay alguna alternativa segura sin cambiar de equipo? | **No.** Consulta independiente sobre la huella del catálogo (1.219 activos): ningún ejercicio con patrón de tracción (HORIZONTAL_PULL, VERTICAL_PULL o SHOULDER_EXTENSION) sin equipo. Los 13 de espalda sin equipo son movilidad o tronco (938, 1066, 1070, 1188, 1190, 1257–1261, 1308–1310) |
| ¿V20 rechaza correctamente? | Sí. `Hipertrofia4Dv34IT` exige exactamente 14 NO_FACTIBLE, todos por V20 (por ejemplo, 2.430 s < 2.520 s a 60 min) |
| ¿Interviene otra clasificación errónea? | No en la factibilidad: 775 exige `bench`, así que sin banco no es elegible. Su patrón (HORIZONTAL_PUSH con zona espalda) **ya está en duda** (`PENDIENTE_PROFESIONAL`, aviso E2-a `DOSIS_NO_DEMOSTRADA`). No crea una factibilidad falsa |
| ¿Se han relajado V20 o V25? | No |

**Cómo debería explicarlo FitSense** (propuesta de texto, sin cambiar código ni Flutter). El aviso `SLOT/UPPER_PULL` actual dice «Tu plan trabaja empuje, piernas y core», pero en estos 14 perfiles **no se genera ningún plan**, así que el texto no es exacto para ellos. Texto propuesto para el NO_FACTIBLE de generación:

> «Con solo tu peso corporal en casa no tenemos ningún ejercicio de tracción (espalda) que podamos prescribirte con seguridad, y sin él una sesión de hipertrofia de {N} minutos no llega al mínimo de trabajo que exigimos. Puedes:
> - añadir en tu perfil un banco, una banda elástica, una espaldera o un punto de anclaje si ya los tienes (no necesitas comprar nada);
> - o elegir sesiones más cortas: con 57 minutos el plan es posible.»

La cifra de 57 min está comprobada para los perfiles de 58–60 min. Para 74–75 min hay que medirla antes de mostrarla.

### Plan 78 (#64)

Comprobado contra el diff de `PublishedPlanSafetyRepairService` y `Plan78ContencionReducidaIT`:

| Requisito | Resultado |
|---|---|
| Solo ejercicios publicados antes | `soloPorDificultad` toma solo ejercicios de la semana publicada que no son sustitutos y no tienen una causa de seguridad. Los sustitutos salen siempre del conjunto elegible de hoy |
| No entra ningún ejercicio nuevo de dificultad incompatible | Sí: la tolerancia no se aplica a los sustitutos |
| La seguridad sigue siendo obligatoria | La búsqueda sin tope de dificultad usa el mismo `SafetyProfile` y las mismas retenciones y contenciones (INC-4C-03, INC-CAT-1374 y C39). Solo se levanta el tope de dificultad |
| Sin cambios de dosis | La prueba exige que no suban series, repeticiones ni segundos |
| Sesiones completadas e historial intactos | Semana empezada → `SEMANA_EMPEZADA`, nada se sustituye. La versión 1 no se toca |
| Sin sustituir la semana entera | Una sola sustitución localizada (1303) |
| Sin una excepción general a V24/V8C | La tolerancia solo quita los problemas `V6` (y los `V1` de esos ejercicios, al añadirlos al conjunto de validación) de los ejercicios tolerados. V8C, V24 y el resto de validadores siguen igual |
| Interruptor apagado y encendido | Apagado: `NO_VALIDA` (V1 de 916 y 910) y el jueves sigue bloqueado. Encendido: `REPARADA`, con `tolerated_publication_difficulty: [910, 916]` en el snapshot |
| Activación | **No activado.** Necesita la aprobación del propietario |

Observación menor: el comentario de `soloPorDificultad` dice «en la misma sesión», pero el código usa los ejercicios de toda la semana publicada. No cambia el resultado: la reparación no mueve ejercicios entre sesiones y los sustitutos no pueden entrar en la tolerancia. Conviene alinear el comentario antes de fusionar #64.

### V54

| Requisito | Resultado |
|---|---|
| La causa es real y trazable | Se calcula al vencer con `PublishedPlanSafetyService.conflictos`, la misma fuente que bloquea la sesión, y se guarda como `ejercicio=causa` en `planned_workouts.system_block_cause` |
| No se confunde con una omisión del usuario | `skip_reason` queda NULL (el usuario no declaró nada) y la causa va en una columna aparte. Para los cálculos, la sesión **sigue contando como no hecha**: ese es el contrato vigente |
| Fórmula de adherencia | Sin cambios: el diff solo toca la reparación, la tarea de vencimiento, `PlannedWorkout` y la migración |
| Historial | Sin cambios: V54 solo añade una columna y no recalcula nada |
| Umbrales | Sin cambios |

La **propuesta metodológica** está separada y sin aplicar (`SESIONES_BLOQUEADAS.md`):
- S3a: informar a la adaptación de que la sesión no se ofreció;
- S3b: excluirla del denominador.

Las dos son decisiones del propietario. El denominador no se ha tocado.

## Fase 6 · Fichas profesionales

`pendientes/FICHAS_PROFESIONALES.tsv` y `.xlsx`:

| Comprobación | Resultado |
|---|---|
| Cobertura | **312 de 312** decisiones no resueltas, incluidas **43 de 43** de seguridad |
| Campos de evidencia (nombre, instrucciones, GIF, hoja de fotogramas, bloque, clasificación vigente, alternativa, diferencia, evidencia, riesgo, impacto, pregunta, estado final, quién decide) | Completos. Solo A-V53 y A-SUELO8 no tienen id, instrucciones ni GIF, porque son decisiones globales |
| Campos de la decisión profesional (decisión, valor, nombre del responsable, credenciales o función, fecha, observaciones) | Presentes y **vacíos en las 312**. No hay ninguna aprobación, y no se ha inventado ninguna |
| GIF | Rutas `videos/…gif` del conjunto de datos de medios, que no está en el repositorio. La hoja de fotogramas se genera con el comando de cada ficha (`herramientas/hojas_completas.py`). **El revisor necesita ese conjunto de datos** |

**Conclusión:** las fichas tienen todos los campos que hacen falta para decidir y para dejar constancia de la decisión. Falta lo que no puede aportar nadie más que un profesional: la decisión, firmada y con fecha.
