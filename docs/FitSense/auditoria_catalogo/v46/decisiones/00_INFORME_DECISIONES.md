# Resolución de decisiones deportivas del catálogo V46: informe

**Rama aislada:** `investigacion/catalogo-v46`. Commits de esta orden: `254f02a`, `f1c6c02` y siguientes (solo documentos, evidencia y SQL de medición).

**No se ha hecho nada de lo siguiente:**
- merge ni despliegue;
- cambios en Neon;
- llamadas a Replicate ni gasto;
- cambios en `mobileapp`, el short paper o el PR #41;
- evaluación de 20 usuarios;
- **ninguna decisión deportiva aplicada de forma definitiva** (ninguna migración nueva);
- relajar V20, V24, V25 o V8C.

**La revisión de los GIF está asistida por IA: no es una validación profesional.**

| Documento | Contenido |
|---|---|
| `MATRIZ_E1_E7.md` | Matriz de decisiones actualizada |
| `RESULTADOS_VARIANTES.md` | Todas las variantes frente a V46 (HIST y REV2) y el K20 con IA simulada |
| `E3_CURL_NORDICO.md` | Fichas de los 6 nórdicos, dificultad propuesta y alternativas |
| `E2_DOSIS_POR_TRONCO.md` | E2-a implementado; diseño E2-c |
| `E4_E7_MATRIZ_REVISION_HUMANA.md` | Contradicciones objetivas frente a decisiones profesionales; instrucciones propuestas para 1320 y los nórdicos |
| `E5_E6_EQUIPAMIENTO.md` | Diseño de las cuatro categorías de equipamiento |
| `REGRESION.md` | Evidencia de regresión |
| `APROBACIONES_PENDIENTES.md` | 14 preguntas profesionales |
| `PROTOCOLO_EVALUACION_20_USUARIOS.md` | Protocolo de 10 aspectos (no ejecutado) |

## 1. Qué se ha implementado (rama aislada)

| Cambio | Efecto en los planes | Pruebas |
|---|---|---|
| **P15-C experimental** (`CoberturaDePatron`, interruptor `fitsense.experimental.p15c.aislados-sin-cobertura`, **apagado**) | **Ninguno con el interruptor apagado:** barridos idénticos byte a byte y huellas K20 iguales | `CoberturaDePatronTest` 2/2 |
| **Aviso E2-a** `DOSIS_NO_DEMOSTRADA` en `/plan/limitations` | Ninguno: solo informa (35 % de los planes REV2 y 30 % de los HIST) | `PlanLimitationsDosisNoDemostradaTest` 3/3 |

**Regresión:**
- unitarias **1.306 / 0** e integración **593 / 0**;
- huellas K20 `a662cb4d`, `11f51d44`, `f9f67973` (HIST congelada), `84dfb6d2` y `da34db35`, iguales a V46.

## 2. Curl nórdico (E3)

**Qué muestran los GIF** de los seis ejercicios:
- son **curls nórdicos asistidos** (recogida con las manos o descarga con la polea);
- todos **exigen anclar los tobillos**;
- las instrucciones de los seis describen otro ejercicio.

**Dificultad propuesta: 2**, pendiente de validación profesional. No se propone 3: con 3 los intermedios pierden lo mismo (+75 / +113 planes sin isquios) sin una justificación deportiva.

**Impacto de excluirlos de principiantes** (CF3a):

| Medida | Resultado |
|---|---|
| Planes perdidos | **0** en ambas políticas |
| Dosis y duración | Sin cambios |
| Principiantes con nórdico | 289 → 0 (REV2) y 468 → 0 (HIST) |
| Planes de principiante sin ejercicio dirigido de isquios | **Unos +190** (conservan los puentes) |

**Perfiles sin alternativa fiable:** principiantes en **casa sin material** (79 planes REV2 y 127 HIST).
- El catálogo solo ofrece 939, cuya clasificación es incoherente.
- Con mancuernas o bandas, o en gimnasio, sí hay alternativas.

## 3. P15-C (resultados propios)

**Qué cumple:**
- 0 planes perdidos en los 4 objetivos y en ambas políticas;
- rodilla cubierta solo por aislados: 324 → 0 (REV2) y 181 → 0 (HIST);
- **dosis idéntica a V46.** No se comporta como CF2-B: conserva más nórdicos y pierde menos isquios.

**Coste: plans without hamstring work rise from 1,572 to 1,953 (REV2) and from 1,023 to 1,446 (HIST).** Los curls femorales dejan de cubrir la rodilla y entran multiarticulares de cuádriceps. Es una pregunta profesional (P-4).

**1205 sissy:**
- su GIF muestra flexión profunda con los talones elevados, y sus instrucciones lo contradicen;
- con dificultad 2, P15-C no pierde cobertura (P15C + CF6).

**1076:** isométrico en pared, sin impacto.

## 4. HIST frente a REV2

| | REV2 | HIST |
|---|---|---|
| V46: con nórdico / principiante con nórdico | 650 / 289 | **1.040 / 468** |
| Rodilla solo aislados (V46) | 324 | 181 |
| Aviso E2-a | 1.308 (34,6 %) | 1.081 (29,7 %) |
| Planes perdidos con P15C, CF3a, CF3b, CF5b o sus combinaciones | 0 | 0 |
| Planes perdidos con CF5a (asiento sin silla) | 0 | **24** (V20) |
| K20 HIST D U | — | 20 PASS en todas las variantes; huella congelada sin cambios con P15-C apagado |

**HIST, la política por defecto, es la más expuesta al problema del nórdico.** Cualquier corrección de E3 en el catálogo afecta también a HIST. **No se ha aplicado.**

## 5. Equipamiento (E5/E6)

| Medida | Resultado |
|---|---|
| Declarar el anclaje del nórdico (CF5b) | 0 planes perdidos; nórdico en casa sin material 252 / 389 → 0 |
| Asiento obligatorio sin silla (CF5a, peor caso) | REV2 sin pérdidas; **HIST −24** (INCREASE_STRENGTH intermedio o avanzado en casa sin material, 5–7 días) |

**Pendiente:**
- revisión visual de los 75 ejercicios de asiento;
- preguntas nuevas en `mobileapp` (no tocado).

## 6. Versión técnica recomendada para los 20 usuarios sintéticos

**Recomendación: congelar la VERSIÓN A** = `f1c6c02` (V46 + aviso E2-a), con **P15-C apagado** y **sin contrafactuales**.

**Por qué:**
- Es la única versión sin decisiones deportivas no validadas.
- Está íntegramente regresionada: barridos idénticos a V46 y huellas iguales.
- Los casos que toquen nórdicos, 775/973/1261/1308/1259/1146, 853/580/1093/1320 o 1205 se marcan como **REQUIERE REVISIÓN** (protocolo §3), no como aprobados.

**Brazo de comparación sin coste** (solo DETERMINISTA y SIMULADA; no cuenta como evaluación de la versión A):
- **VERSIÓN B** = A + nórdico con dificultad 2 + anclaje 1195/1196 + sissy con dificultad 2 + P15-C activado.
- Medida aquí: 0 planes perdidos y 14/14 criterios con IA simulada en U y N.
- **Coste:** menos trabajo dirigido de isquios (§2–3).

**Paso a B como versión evaluada:** solo cuando se respondan P-1, P-3, P-4 y P-5 **y** se escriba la migración correspondiente, con su propia regresión.

**Antes de ejecutar, en cualquier caso:**
- aprobar el protocolo;
- generar y congelar la cohorte O01–O20;
- extender el arnés (aspectos 2 y 8).

## 7. Me detengo aquí

Para avanzar necesito tu decisión sobre estos puntos:
1. La versión de la evaluación (A, o B cuando haya validación).
2. Las preguntas de `APROBACIONES_PENDIENTES.md`, en especial P-1 a P-5.
3. La aprobación del protocolo de 20 usuarios.
4. Si se escriben ya las correcciones objetivas de E4/E7 (nombres e instrucciones de 853, 580, 1093, 1320, 1205 y los nórdicos) como migración candidata en la rama aislada. Son solo lo que se muestra; los textos están en E4/E7 §3 y no se ha escrito nada.
