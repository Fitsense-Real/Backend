# 07 · Pruebas de dolor y respaldo, casos A-J

**Commits:** `65878c2` y `2385b64`.

Unitarias sobre la reparación **real** de SUST-SEG-1 (`SafetySubstitutionPolicy.reparar`) y sobre V8C ante `SEGURIDAD_ANTE_DOLOR`, con las tres estrategias: A = off, B = on, C = preferencia.

En las pruebas unitarias el ejercicio que dolió es el 70001 (dificultad 2, chest, HORIZONTAL_PUSH). Como en producción, el conjunto elegible **ya no** contiene ningún excluido por dolor:
- P-02 los filtra en `WeeklyTrainingPlanCommandServiceImpl:171`;
- `SafetyExclusionResolver:58` solo declara exclusiones fuera del elegible.

| Clase | Resultado |
|---|---|
| `LimiteDificultadDolorTest` | **22/22**, 0 omitidas |
| `SinSustitutoDolorLimite{Apagado,Encendido,Preferencia}IT` | **3/3**, 2 semanas cada una |
| `RespaldoConDolorIT`, `AdaptacionCompletaIT`, `InvariantesRespaldo` | ya existían; corren en la regresión con off, on y preferencia (12) |
| REPLAY P123, caso N08 | 09 |

## Casos

| Caso | Situación | A | B | C | Evidencia |
|---|---|---|---|---|---|
| **A** | Hay un sustituto del mismo nivel: candidatos 70002 (3), 70003 (2), 70004 (1) | **70002, sube a 3** (gana por id) | 70003, igual | 70003, igual | `casoA` |
| A2 | Hay uno menor de id más bajo y uno igual: 70002 (1), 70003 (2) | — | 70002 (menor, por id) | **70003 (igual)** | `casoA2`: la única diferencia entre B y C |
| **B** | Solo hay sustitutos menores: 70004 (1), 70005 (1) | 70004 | 70004 | 70004 | `casoB` |
| **C** | Solo hay uno **más difícil**: 70002 (3) | 70002, sube | **ninguno**: se declara `SEGURIDAD: ... no hay sustituto elegible` | ninguno, igual que B | `casoC` |
| **D** | No hay ningún sustituto del slot (solo uno de KNEE_DOMINANT) | ninguno, declarado | ninguno | ninguno | `casoD` |
| **E** | El candidato del mismo nivel (70003) **también dolió** y está fuera del elegible; quedan 70002 (3) y 70004 (1) | 70002, nunca el 70003 | 70004, nunca el 70003 | 70004 | `casoE` |
| **F** | El original ya está retirado y la propuesta lo **vuelve a prescribir** (580 → 580) | **rechazada** | rechazada | rechazada | `casoF`: «vuelve a prescribir [580]» |
| **G** | La IA propone uno más difícil ante dolor (580 dif 1 → 56 dif 2) | aceptada (hoy) | **rechazada** por el límite | rechazada | `casoG`; de extremo a extremo, N08 (09) |
| **H** | Tres intentos de IA inválidos | respaldo completo | respaldo completo con SUST-SEG-1 ≤ | ídem | `RespaldoConDolorIT`, cuya IA rechaza siempre; `LimiteDificultadTooDifficultIT` (3 intentos y luego CONSERVED); N08 en el REPLAY |
| **I** | Varios candidatos y solo uno válido; 100 órdenes de inserción aleatorios con semilla fija | siempre 70002 | siempre 70004 | siempre 70004 | `casoI`: un único resultado en las 100 vueltas |
| **J** | La sesión ya fue adaptada antes: historial y exclusiones | — | se respetan | se respetan | N08 en el REPLAY (9 semanas posteriores, 09) y `RespaldoConDolorIT.B`: la semana siguiente conserva el sustituto y no reincorpora lo que dolió |

En todos los casos se comprueban además dos invariantes:
- `invarianteNoReintroduce`: con sustituto, lo que dolió desaparece de la sesión; sin sustituto, queda **declarado** como problema y `revisarYCerrar` impide publicarlo;
- `dosisNoSube`: el sustituto lleva la dosis de la posición original, 3x10.

## Lo que no se observó

Con ninguna estrategia se observó:
- la reintroducción de un excluido;
- una subida de dosis;
- un reintento sin fin: un intento V32 por semana y, como mucho, un intento P-03;
- una modificación de sesiones no señaladas.

El 08 recoge las pruebas multisemana.
