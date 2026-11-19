# Auditoría y corrección controlada del catálogo (v46): informe consolidado

**Rama aislada:** `investigacion/catalogo-v46`, desde `ae5fdd4`.

**Sin hacer:**
- llamadas a Replicate, ni ningún gasto;
- cambios en Neon, merge o despliegue;
- cambios en `mobileapp`, el short paper o el PR #41.

**La evidencia de la evaluación real queda intacta** (verificada: `SHA256SUMS` 7/7). El catálogo original sigue reproducible con `spring.flyway.target=44.3`.

| Documento | Contenido |
|---|---|
| `A_INFORME_AUDITORIA.md` | Hallazgos, evidencia, clase y severidad |
| `B_MATRIZ_CORRECCIONES.md` | Implementado, propuesto y pendiente |
| `C_INFORME_IMPACTO.md` | Original, V46 y variantes: generación, dosis, patrones, factibilidad y adaptación |
| `D_EVIDENCIA_PRUEBAS.md` | Pruebas, resultados y huellas |
| `E_DECISIONES_PENDIENTES.md` | P15, dosis por tronco, dificultad, identificación visual, ejercicios sin equivalente |
| `F_PROPUESTA_EVALUACION.md` | Evaluación futura con IA real: cohorte nueva y presupuesto separado. **No autorizada** |

## 1. Qué se corrigió (implementado en V46; objetivo y demostrado)

| Ejercicio | Antes | Ahora | Por qué es objetivo |
|---|---|---|---|
| 973 | «Giro izquierdo. boxeo» | «Gancho de izquierda (boxeo)» | Nombre original, instrucciones y GIF coinciden |
| 1263 | «Sentadillas búlgaras» | «Sentadilla dividida (zancada estática)» | Nombre original, instrucciones (pie trasero en el suelo) y GIF coinciden |

**Efecto demostrado:** solo cambia lo que ve la persona.
- Barridos HIST y REV2 idénticos byte a byte.
- Huellas K20 iguales: también la HIST congelada y la de la IA simulada.
- Prompts de la IA idénticos.
- 1.301 unitarias y 593 de integración, 0 fallos.

## 2. Cómo cambiaría la selección real si se corrige lo pendiente (medido, no aplicado)

| Si… | REV2 | HIST |
|---|---|---|
| 775 y 973 dejan de aportar dosis | 0 planes perdidos. DOSIS_LIMITADA +442. **Unos 330 planes por objetivo quedan sin hombro.** La espalda pasa a otros ejercicios de tronco igual de dudosos (1261, 1308, 1259) | **−110 planes válidos** (V20) |
| …además 1261, 1308, 1259 y 1146 | 0 planes perdidos. La espalda pasa a 1066 (extensión de tronco). **En casa sin material, el catálogo no tiene ningún hombro con dosis** | **−146** |
| Los aislados dejan de cubrir el patrón de rodilla (P15-B) | 0 planes perdidos. Rodilla solo con aislados: 324 → **0**. LOSE_WEIGHT sin rodilla +5. Sustitutos multiarticulares (el más usado, el sissy squat, tiene dificultad 1) | 0 planes perdidos; 181 → 0 |
| Adaptaciones con IA simulada (U, N) con esas variantes | 0 incumplimientos y 0 cambios no pertinentes | — |

**Lo que esto demuestra:** reclasificar no basta.
- En la espalda, el problema se desplaza a otros ejercicios de tronco.
- En el hombro, deja un hueco: falta contenido real en el catálogo.
- En HIST, rompe planes.

Por eso no se ha aplicado ninguna de estas variantes.

## 3. Qué planes siguen teniendo limitaciones

| Limitación | Planes REV2 / HIST | Severidad |
|---|---|---|
| **Curl nórdico (6 ejercicios) con dificultad 1**, instrucciones de otro movimiento y anclaje no declarado | **650 / 1.040**, de ellos **289 / 468 de principiante** | **Alta. También en HIST, la política por defecto** |
| La espalda la aporta solo 775 | 615 / 640 | Alta |
| El hombro lo aporta solo 973 (casa sin material: no hay otro) | 917 / 537 | Alta |
| Rodilla cubierta solo por aislados | 324 / 181 | Media-alta |
| 1261, 1308, 1259 y 1146 como dosis de espalda | 342 / 370 / 225 / 89 en REV2 | Media |
| 853 y 1093: nombre e instrucciones distintos del GIF | 1.648 y 433 en REV2 (cardio) | Media / baja (solo lo que se muestra) |
| 1320: instrucciones que piden silla | 194 REV2 en casa sin material | Media (instrucciones) |
| 580: «Curl» erróneo; ¿press o apertura? | 53 / 76 | Baja |
| Asiento no modelado | 78 ejercicios usados | Sistémica |

**Relectura de la evaluación real** (descriptiva; no cambia el informe 25): de los 20 REQUIERE_REVISION, **10 tocan solo el 853**, cuyo problema afecta a lo que se muestra, no a la selección. Las otras 10 dependen de decisiones pendientes:
- 4 de la dosis por tronco (775 y 973);
- 3 de la dificultad (1262 y 1115);
- 3 de P15 (1009).

## 4. Qué requiere validación profesional o revisión visual humana

**Detalle en `E`.**

| Asunto | Recomendación |
|---|---|
| **E3 · Dificultad del curl nórdico** | Lo más urgente para usuarios reales, porque afecta a HIST |
| E1 · P15 | Alternativa C (separar cobertura de patrón y dosis), condicionada a revisar antes la dificultad del sissy squat |
| E2 · Tronco contado como espalda u hombro | E2-a (declarar la limitación) hasta que existan ejercicios reales de hombro sin material (E2-c). No aplicar reclasificaciones a HIST |
| E4 · Movimientos que hay que identificar | 853, 580, 775, 1093 y el anclaje de 1195/1196: revisión visual humana. **La revisión de esta auditoría está asistida por IA y no es una validación profesional** |
| E7 · Instrucciones nuevas | 1320 y curl nórdico: textos propuestos, sin escribir en la base de datos |

**Me detengo aquí.**
- No aplico ningún cambio deportivo sin tu aprobación.
- No propongo otra evaluación con IA real sin una nueva autorización (`F`).
