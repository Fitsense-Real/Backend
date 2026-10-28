# V36 · Corrección de nueve ejercicios del catálogo

**Fecha:** 28/10/2026 · **Rama:** `catalogo-v36` (desde `generation`, separada del PR #3).

**Estado:** implementada y validada en PostgreSQL 16 aislado. **Pendiente** de:
- la revisión humana doble;
- la decisión sobre los planes ya publicados (§8).

**Lo que NO se hizo:**
- no se ejecutó en Neon ni se desplegó;
- no se llamó a Replicate;
- no se sobrescribió la referencia oficial;
- no se tocó V15, V20, V22, V24, V25, V35, adaptación, progresión, D8, adherencia ni roles.

## 1. Resumen

| Criterio de la orden | Resultado |
|---|---|
| 1. Clasificaciones correctas y justificadas | Aplicadas por V36 y justificadas con el GIF (§2). La **revisión humana doble está pendiente** (§3) |
| 2. Los ejercicios problemáticos dejan de aparecer en contextos inadecuados | 1267: 0 colocaciones (antes 51 en el barrido). Combinados en PULL: 0 (antes 31). 1296 y 1333 en principiantes: 0. Dificultad por encima del nivel: 0 |
| 3. Las 72 referencias candidatas siguen siendo válidas | **72/72 VALIDA**. La oficial se conserva y se sigue verificando con el catálogo anterior |
| 4. El barrido de 4–6 días no presenta fallos nuevos | Base: 60/72 antes y después (los 12 sin plan son el defecto del respaldo que corrige el PR #3). Con el PR #3: **72/72** antes y después |
| 5. 288 generaciones de robustez sin perfiles factibles sin plan | Con el PR #3: **0 sin plan** antes y después. Base: los mismos 52 fallos (perfil, semilla) antes y después; ninguno nuevo |
| 6. Cambios de rutina identificados y explicables | 213 de 216 sesiones de referencia cambian: 77 directas y 136 indirectas por la siembra determinista. 0 cambian de día o enfoque (§6) |
| 7. Sin relajar reglas ni tocar adaptación o progresión | Sin cambios en `src/main/java`. Solo la migración, pruebas y documentación |

**Incompatibilidad encontrada y no corregida (§8).** Un plan **publicado antes de V36** que contenga 1267, 1296 (en un intermedio), un combinado en una sesión PULL o 872 con dosis de estructural (como el plan 60 real) **no puede copiarse** en el ciclo semanal siguiente. Esa semana quedaría sin plan. Está reproducida en `CatalogoV36IT` y es una decisión previa al despliegue.

## 2. Migración V36 y matriz antes/después

`src/main/resources/db/migration/V36__catalog_review_nine_exercises.sql`. El número 36 estaba libre (`generation` termina en V35) y no hay otra V36 en ninguna rama remota.

| Id | Columna | Antes (V35) | Después (V36) |
|---:|---|---|---|
| 1267 | `is_active` | true | **false** (no se borra: fila, id e historial se conservan) |
| 1296 | `difficulty_level` | 1 | **3** |
| 1296 | `high_impact` | false | **true** |
| 1333 | `equipment_id` | ab wheel | **body weight** |
| 1333 | requisito de equipamiento | `1:ab wheel` | **ninguno** (peso corporal) |
| 1333 | `high_impact` | false | **true** |
| 1333 | `requires_floor` | true | **false** |
| 1333 | `requires_overhead` | true | **false** |
| 1333 | `name_es` | Rodar con rueda | **Carrera con zancadas amplias** |
| 872 | `movement_pattern` | HIP_EXTENSION | **TRUNK_FLEXION** |
| 872 | `name_es` | Levantamiento de cadera con rodillas flexionadas | **Encogimiento invertido** |
| 1059 | `body_part` / `target_muscle` | waist / abs | **upper legs / quads** |
| 1059 | `synergist_muscle` | quadriceps | **abs** (conserva el giro) |
| 527 | `body_part` / `target_muscle` | upper arms / biceps | **upper legs / quads** |
| 527 | `synergist_muscle` | forearms | **biceps** (conserva el curl) |
| 609 | `body_part` / `target_muscle` | upper arms / biceps | **upper legs / quads** |
| 609 | `synergist_muscle` | quadriceps | **biceps** (conserva el curl) |
| 725 | `body_part` / `target_muscle` | upper arms / biceps | **shoulders / delts** |
| 725 | `synergist_muscle` | shoulders | **biceps** (conserva el curl) |
| 760 | `body_part` / `target_muscle` | upper arms / biceps | **shoulders / delts** |
| 760 | `synergist_muscle` | forearms | **biceps** (conserva el curl) |

**Sin cambio, comprobado:**
- los patrones `KNEE_DOMINANT` (1059, 527, 609) y `VERTICAL_PUSH` (725, 760);
- `requires_overhead=true` en 725 y 760, que la restricción declarada de brazos arriba sigue excluyendo;
- `HORIZONTAL_PUSH` de 1296, y `LOCOMOTION_CARDIO`, `DURATION` y dificultad 1 de 1333: su impacto alto ya lo excluye de principiantes;
- nombres e instrucciones de los demás, y `secondary_muscles`.

**Fallo controlado.**
- **Precondición:** cada columna que cambia debe tener exactamente su valor anterior, y 1333 un único requisito `1:ab wheel`. Si no, aborta con `RAISE EXCEPTION` y lista los ids afectados, sin aplicar nada.
- **Poscondición:** el estado final de las nueve filas se verifica antes del COMMIT. Los disparadores diferidos de V23 se comprueban en el COMMIT.

**Verificación en PostgreSQL 16 aislado** (V1–V35 aplicadas con `psql -1`, como Flyway):

| Prueba | Resultado |
|---|---|
| Aplicar V36 | OK: 1+1+1(DELETE)+1+1+1+2+2 filas |
| Resto del catálogo (1367 filas) | Huella MD5 idéntica antes y después |
| Requisitos del resto (1337 filas) | Huella idéntica |
| Aplicarla dos veces | Aborta: «no tienen la clasificación anterior esperada: 527, …, 1333» |
| 1296 alterado a mano antes | Aborta: «…: 1296»; 1267 sigue activo (no se aplicó nada) |
| Requisito extra en 1333 | Aborta: «los requisitos de 1333 son 1:ab wheel,1:body weight» |

## 3. Revisión humana

**Pendiente.** No hay revisores ni resultados inventados. El expediente está preparado con el procedimiento de V27/V28: dos revisores identificados e independientes, material ciego, criterio v5 con huella y consolidación.

- `evidencia/catalogo_v36/revision_humana/REVISION_HUMANA_V36.md`: procedimiento, entregas y tabla de consolidación (anterior, propuesta, R1, R2, consenso, justificación).
- `evidencia/catalogo_v36/revision_humana/material_ciego_v36.md`: fichas en orden aleatorio, GIF y 10 preguntas, sin la clasificación anterior ni la propuesta.
- `evidencia/catalogo_v36/revision_humana/formulario_revisor_v36.csv`: un formulario por revisor.

**Discrepancias que la revisión debe resolver** (detalle en el expediente):

1. **872 frente a V28.** V28 aprobó 872 como nivel 1 de LOWER_HIP con material de **texto**, y sus instrucciones describen un **puente de glúteo** (V28 anota un 92,8 % de coincidencia con 1073 *low glute bridge*). El GIF es un encogimiento invertido. V36 no toca `exercise_difficulty_approvals`. Si V36 se confirma, LOWER_HIP queda con 3 aprobados en vez de 4. No afecta a Java, porque el filtro de aprobación no está activo.
2. **Instrucciones contradictorias** (no son columnas aprobadas; V36 no las cambia): 1333 sigue hablando de un rodillo, 872 de un puente de glúteo y 1296 de una flexión con elevación alterna. El participante vería un nombre y un GIF que no corresponden al texto. **Recomendación:** corregir `instructions_es` e `instruction_steps_es` de 1333, 872 y 1296 en la misma V36, si se autoriza, antes de desplegar.
3. **Banco en 725** no exigido, **rodilla casi en el suelo** en 527 y 609 sin `requires_kneeling`, y `axial_load` distinto entre 527 y 609. No están aprobados; se dejan anotados.

## 4. Pruebas

| Prueba | Qué demuestra |
|---|---|
| `CatalogoV36IT` (nueva, 6 pruebas) | Flyway hasta V35, publica planes reales y después aplica V36 con la configuración de Flyway de la aplicación. Comprueba: <br>1. Matriz exacta de las nueve filas; resto del catálogo, requisitos, historial V27/V28 y planes publicados sin cambios (huellas). <br>2. Planes publicados: 1296 se marca `alto_impacto` para el principiante; 1267 no es causa de seguridad y se sigue mostrando. <br>3. Ciclo semanal siguiente (§8). <br>4. Conjunto elegible por nivel, equipamiento, edad y restricciones: 1267 nunca; 1296 solo avanzado con impacto permitido y menos de 60 años; 1333 sin rueda y nunca para principiantes ni con «sin impacto». 872 deja de pertenecer a un slot obligatorio; 1059, 527 y 609 son LOWER_KNEE y `upper legs`; 725 y 760 son UPPER_PUSH y `shoulders`, y respetan «sin brazos arriba». <br>5. Planes nuevos (36 perfiles en la base, 48 con el PR #3): ningún 1267; 527, 609, 725 y 760 nunca en PULL; 1296 solo en avanzados; 1333 nunca en principiantes. <br>Sin llamadas a la IA (`never()`) |
| `GeneracionReferenciaIT` | **La referencia oficial, intacta.** Ahora fija `spring.flyway.target=35`: las 72 generaciones con el catálogo anterior siguen idénticas a `progresion/generacion_referencia.txt`, lo que demuestra que el código de generación no cambió |
| `GeneracionReferenciaCandidataV36IT` (nueva) | Las 72 generaciones con V36 coinciden con la candidata `catalogo_v36/generacion_referencia_candidata_v36.txt`. Se captura aparte con `-Dfitsense.generacion.capturar.v36=true` |
| `GeneracionReferenciaBase` | Lógica común de las dos anteriores (antes estaba en `GeneracionReferenciaIT`) |

## 5. Las 72 referencias: oficial frente a candidata

| Medida | Oficial (antes de V36) | Candidata (V36) |
|---|---:|---:|
| Perfiles con plan VALIDA | 72/72 | **72/72** |
| Sesiones | 216 | 216 |
| Sesiones con otro día o enfoque | — | **0** |
| Sesiones con otros minutos estimados | — | 26 (entre −4 y +4 min) |
| Colocaciones | 1155 | 1153 |
| Sesiones con algún ejercicio o dosis distinta | — | 213 (en los 72 perfiles) |
| · directas (contienen alguno de los nueve, antes o después) | — | 77 |
| · indirectas | — | 136 |
| Cobertura de capacidades | COMPLETO 72 | COMPLETO 72 |
| Sesiones por debajo del 70 % exacto (V20, sin cambiar) | 17 | 18 |

**Seguridad y nivel, juzgando los dos planes con la clasificación corregida:**

| Exposición | Oficial | Candidata |
|---|---:|---:|
| Alto impacto en principiante (1296) | 3 | **0** |
| Dificultad por encima del nivel (1296 en principiante e intermedio) | 6 | **0** |

**Dosis.** Las mismas familias en proporciones casi iguales; ninguna dosis nueva. Por ejemplo, `3x10r/90` pasa de 263 a 270 y `3x6r/150` de 187 a 185.

**Patrones.** HIP_EXTENSION baja de 43 a 29 (872 deja de contar como cadera). Al motor le quedan los extensores de cadera reales, y HIP_HINGE sube de 88 a 102. Las tablas completas están en `evidencia/catalogo_v36/resultados_comparacion.md`. El detalle sesión por sesión, marcado DIRECTO o INDIRECTO, está en `referencias_sesiones_cambiadas.tsv`.

## 6. Cambios directos e indirectos

**Directos (77 sesiones).** La sesión contenía uno de los nueve y ya no lo contiene, o lo contiene ahora. Ejemplos:
- 1296 sale de los principiantes;
- 872 deja de ocupar el hueco de cadera;
- 1059 deja de contar como core;
- 1333 entra como cardio en intermedios y avanzados.

**Indirectos (136 sesiones).** El motor de reglas siembra su generador pseudoaleatorio con `userId × 1 000 003 + díaDeLaSemana` (`RuleBasedTrainingPlanGenerator.seedFor`). Después sortea sobre listas ordenadas de candidatos elegibles.

- 1267 (peso corporal, nivel 1, sin restricciones declaradas) **era elegible para los 72 perfiles**. Al desactivarlo, todas sus listas pierden un elemento.
- Las reclasificaciones de 1059, 527, 609, 725 y 760 mueven candidatos entre grupos.
- Con listas distintas, la misma semilla produce otros índices. Los ejercicios elegidos cambian aunque la sesión nunca tuviera uno de los nueve.

**Lo que no cambia:**
- 0 sesiones cambian de día o enfoque;
- la división de los 72 es la misma;
- todas las dosis salen de la misma tabla;
- la cobertura sigue COMPLETA en los 72;
- los 72 planes son válidos.

Es la misma cascada que vio la simulación anterior (213 de 216), ahora con la migración real.

## 7. Barrido de 4–6 días y robustez

La base (`generation`) aún no tiene el PR #3. Por eso se validó **por separado y combinado** en un entorno aislado: una rama local `respaldo-5-6-dias` + `catalogo-v36` fusionada **sin publicar**. La fusión no tuvo conflictos.

**Dependencia:** los 5–6 días de GENERAL_FITNESS y LOSE_WEIGHT necesitan el PR #3, con o sin V36. `CatalogoV36IT` los incluye solo cuando la prueba del respaldo está en el árbol.

| Medida | Base sin V36 | Base + V36 | PR #3 sin V36 | PR #3 + V36 |
|---|---:|---:|---:|---:|
| Perfiles con plan VALIDA | 60/72 | 60/72 | 72/72 | **72/72** |
| Perfiles sin plan | 12 | 12 | 0 | **0** |
| Sesiones = días pedidos (frecuencias actuales) | 44/60 | 44/60 | 56/72 | 56/72 |
| Robustez (288): sin plan | 52 | 52 (los mismos) | 0 | **0** |
| 1267 colocado | 44 | 0 | 51 | **0** |
| Alto impacto en principiante | 4 | 0 | 4 | **0** |
| Dificultad por encima del nivel | 6 | 0 | 7 | **0** |
| Trabajo fuera de grupo en PULL (combinados) | 21 | 0 | 31 | **0** |
| Bandera «ejercicio en revisión» de la evaluación | 96 | 15 | 104 | 19 |
| Sesiones por debajo del 70 % exacto | 55 | 48 | 66 | 59 |
| `MENOS_DE_3_DE_TRABAJO` / `MENOS_DE_2_DE_PIERNA` / `WAIST_IGUALA_O_SUPERA_PIERNA` / `MAS_DE_LA_MITAD_SIN_V15` | 16 / 8 / 12 / 10 | iguales | 60 / 12 / 16 / 10 | iguales |

**Detalle:**
- La bandera «en revisión» es la lista fija de la evaluación (`DUDOSOS`), que incluye 1333 y 1059. No es una regla del sistema.
- Las sesiones cambian de ejercicios (270/270 y 336/336) por la misma cascada, pero **0 cambian de enfoque**.
- **Sin incompatibilidades nuevas entre catálogo y capacidades.** Cobertura COMPLETA en todos los planes generados; las banderas de reparto no varían.
- V15, V20, V22, V24 y V25 se aplican sin cambios: todos los planes que validan lo hacen con las reglas actuales.

**Dónde aparecen los nueve en el barrido** (PR #3 sin V36 → PR #3 + V36):

| Ejercicio | Antes | Después |
|---|---|---|
| 527 | PULL 10, UPPER_BODY 7 | LEGS 1, LOWER_BODY 1 |
| 609 | PULL 5, UPPER_BODY 2 | LOWER_BODY 2 |
| 725 | PULL 9, UPPER_BODY 6 | PUSH 1, UPPER_BODY 3 |
| 760 | PULL 7, UPPER_BODY 1 | 0 |
| 872 | LOWER_BODY 27 | LOWER_BODY 1 |
| 1059 | LOWER_BODY 25 | LEGS 4, LOWER_BODY 4 |
| 1267 | PUSH 17, UPPER_BODY 34 | **0** |
| 1296 | PUSH 3, UPPER_BODY 5 (incluye principiantes) | PUSH 1 (avanzado) |
| 1333 | 0 (nadie declara rueda) | LOWER_BODY 2, PULL 2 (como cardio), UPPER_BODY 5; solo intermedio y avanzado |

Evidencia: `evidencia/catalogo_v36/{base_antes,base_v36,pr3_antes,pr3_v36}/`. La comparación se reproduce con `comparar_v36.py`.

## 8. Planes ya publicados · incompatibilidad con la copia semanal

**V36 no elimina ni altera ningún plan publicado.** `CatalogoV36IT` lo comprueba con una huella de `weekly_training_plans`, `planned_workouts` y `planned_workout_exercises` antes y después.

**Lectura y ejecución.** `PublishedPlanSafetyService` marca 1296 en las sesiones abiertas del principiante como `alto_impacto`, y execution se niega a registrarlo. Es la protección vigente, sin cambios. 1267 no se marca: un ejercicio inactivo **no es causa de seguridad** (`SafetyConflictPolicy`), así que se sigue mostrando y ejecutando en la semana ya publicada.

**Semana siguiente.** El ciclo semanal (`WeeklyCycleService` → copia V31) copia la semana medida y la valida con el catálogo **actual**. Resultado, con el ciclo real y sin IA:

| Caso (plan publicado antes de V36) | Resultado | Motivo |
|---|---|---|
| Contiene 1267 | **NO_FACTIBLE, semana sin plan** | `V1: el ejercicio 1267 no esta en available_exercises` |
| Intermedio con 1296 | **NO_FACTIBLE, semana sin plan** | `V1: …1296…`: deja de ser elegible y la dificultad no es causa de seguridad |
| Combinado (527, 609, 725, 760) en una sesión PULL | **NO_FACTIBLE, semana sin plan** | `V15: … enfoque PULL pero incluye ejercicios de [upper legs]` y `V18` |
| Principiante con 1296 | Copia publicada | Sustitución obligatoria SUST-SEG-1 por `alto_impacto` |
| 872 prescrito como estructural (plan 60 real: GAIN_MUSCLE, 3x13 a 90 s) | **Rechazado por V24** | `V24: el ejercicio 872 … lleva 3x13 … y 4D v2 exige 2x13 (relleno …)`: al dejar de ser HIP_EXTENSION, su dosis es la de relleno. Reproducido por `Plan60HipertrofiaIT` y `DuracionForzadaIT` con V36 (§9) |
| Contiene 872 o 1059 (en los casos probados, sin 1267) | Copia publicada (CONSERVED) | Sin problemas. En el plan con 1267, la copia falló además por V15 en una sesión LOWER_BODY que, con la reclasificación, solo trabaja `upper legs`: un plan con 1059 o 527/609 puede fallar igual si era su único ejercicio fuera de `upper legs` |

Por 19.4, una semana sin plan queda con adherencia NULL, y el ciclo siguiente vuelve a la generación inicial. Se perdería una semana de datos del participante. **No se ha corregido:** la orden pide reportarlo y reproducirlo antes de proponer cambios. No se relaja V8C ni V1.

**Medirlo antes de decidir:**
- `docs/FitSense/consultas/exposicion_v36.sql`: consulta de **solo lectura** (`BEGIN READ ONLY … ROLLBACK`) para que la ejecutes tú contra Neon con un usuario de lectura.
- Cuenta participantes y planes ACTIVOS en cada caso, el detalle de los casos que dejarían la semana sin plan, los reclasificados con su dosis (para detectar casos como el plan 60) y el histórico.
- Está probada contra el esquema aislado.

**Opciones (ninguna implementada):**
1. **Si la consulta devuelve 0 en los casos A, B y D, y el detalle 2b no muestra dosis de estructural en 872:** desplegar V36 sin más. La incompatibilidad no se manifiesta.
2. **Si afecta a pocos participantes:** desplegar y aceptar, documentándolo como incidencia del estudio, que esos participantes pasen una semana sin plan y vuelvan con la generación inicial. La alternativa es aplazar V36 hasta que terminen. Hoy no existe un flujo para sustituir una semana ACTIVA fuera de la reparación de seguridad (`generateWeeklyPlan` falla si ya hay un plan activo), así que regenerarla a mano no es una opción sin código.
3. **Cambio de código:** tratar la «retirada por corrección de catálogo» como causa con nombre (precedente: `INC-CAT-1374`) para permitir una sustitución en la copia. Afecta a V8C, y la clase de slot del ejercicio quitado era errónea (1267 «KNEE_DOMINANT» en una sesión de tren superior). Exige diseño y aprobación.

**Recomendación:** medir primero. Opción 1 si sale 0. Si no, opción 2 para un número pequeño, u opción 3 si se esperan más correcciones de catálogo durante el estudio.

## 9. Regresión completa

**Primera ejecución con V36 (rama `catalogo-v36`):** 816 pruebas unitarias sin fallos. 254 de integración con **4 fallos**, todos causados por V36:

| Prueba | Causa |
|---|---|
| `Plan60HipertrofiaIT.sesionDeSesentaMinutos` | El plan 60 real (publicado el 26/10) lleva 872 como estructural a 3x13. Con V36, V24 exige 2x13. **Es un caso más de §8**, no un fallo de la regla |
| `DuracionForzadaIT.sinMinutosForzadosRigeElPerfil` | El mismo plan 60 como borrador |
| `HerenciaD8IT.propuestaIdenticaConservaD8` | Falla la premisa del escenario: «el viernes acortado (32) bajo el piso 31». La semana generada cambia por la cascada (§6), y el escenario estaba calibrado con ella |
| `ProgresionIT.acortadaProgresaYReincorpora` | La misma premisa |

**Qué se hizo.** Esas cuatro clases fijan ahora `spring.flyway.target=35`, con un comentario que lo explica. No se cambió ninguna regla ni ninguna aserción:
- Las dos primeras validan un plan **publicado con el catálogo anterior**.
- Las dos últimas prueban D8 y la progresión, que no dependen del catálogo, sobre una semana calibrada con él.

**Alternativa, si se prefiere:** recalibrar sus escenarios sobre el catálogo con V36. Supone tocar los datos de las pruebas, no la lógica.

### 9.1 Segunda ejecución

Ejecución completa de la rama `catalogo-v36`, con la base P2-D local: **BUILD SUCCESS**.
- Unitarias: 816 sin fallos.
- Integración: 254, 0 fallos, 12 omitidas. Las omitidas son opcionales: Replicate real, Neon y evaluaciones manuales.

Incluye `CatalogoV36IT`, `GeneracionReferenciaIT` (oficial, hasta V35) y `GeneracionReferenciaCandidataV36IT`.

Ejecución combinada con el PR #3 (rama local, sin publicar): `CatalogoV36IT` (48 perfiles), `RespaldoCincoSeisDiasIT` 14/14, las dos referencias y `EvaluacionFrecuenciaIT` (barrido y robustez), todas en verde.

## 10. Decisiones pendientes

1. **Revisión humana doble** (§3), incluida la discrepancia de 872 con V28.
2. **Instrucciones** de 1333, 872 y 1296: ¿se corrigen en V36 antes de desplegar? Lo recomiendo.
3. **Planes publicados** (§8): ejecutar `exposicion_v36.sql` en Neon (solo lectura) y elegir la opción 1, 2 o 3.
4. **Referencia oficial:** si se aprueba V36, pasar la candidata a oficial con una decisión explícita. Hasta entonces conviven las dos.
5. **Orden de integración:** el barrido de 5–6 días depende del PR #3. V36 se puede integrar antes o después: la fusión no tuvo conflictos y los resultados combinados están arriba.
