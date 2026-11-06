# E2 · Dosis de espalda y hombro aportada por ejercicios de tronco

## 1. E2-a: adoptada provisionalmente e implementada (aviso, sin cambiar planes)

**Qué se ha hecho** (commit `f1c6c02`, rama `investigacion/catalogo-v46`):
- `GET /plan/limitations` añade un aviso `DOSE / DOSIS_NO_DEMOSTRADA` cuando, en el plan publicado más reciente, **todos** los ejercicios resistidos que se cuentan para la espalda o el hombro pertenecen a {775, 973, 1261, 1308, 1259, 1146}.

**Lo que NO hace:**
- No reclasifica ni retira ningún ejercicio, ni en HIST ni en REV2.
- No cambia la selección, la dosis calculada ni la traza.
- No añade volumen.
- No afirma que el grupo esté trabajado ni que no lo esté.

**Texto exacto** (con los grupos y los nombres que correspondan):
> «En tu plan, el trabajo de espalda y hombros procede solo de ejercicios cuya clasificación está en revisión (Elevación de codo y push-up invertido, Gancho de izquierda (boxeo)). No podemos afirmar que aporten la dosis orientativa a ese grupo, así que no lo contamos como trabajo demostrado. Tu plan sigue siendo válido y seguro; la revisión profesional del catálogo está pendiente.»

**Reglas:**
- Cuenta un ejercicio para un grupo si su `body_part` es ese grupo y su patrón es resistido. Movilidad, cardio, equilibrio y OTHER no cuentan.
- Si el plan tiene **un solo** ejercicio demostrado del grupo, no hay aviso para ese grupo.
- Un ejercicio que ya no está en el catálogo elegible no se cuenta: no se inventa un aviso.
- Se calcula con una consulta de solo lectura (`WeeklyTrainingPlanRepository.exerciseIdsOfPlan`), sin cargar el agregado.

**Pruebas:**
- `PlanLimitationsDosisNoDemostradaTest`: 3/3. Cubre ambos grupos, la mezcla con un ejercicio demostrado, la movilidad, la ausencia de plan y de catálogo, y un ejercicio fuera del catálogo. Comprueba también que el texto no promete resultados.
- Las 2 pruebas de `PlanLimitationsDosisTest` y las 7 de `PlanLimitationsFrecuenciaTest` siguen pasando.
- Unitarias completas: 1.306 / 0 fallos.

**Exposición estimada** (planes con aviso), medida en los barridos de V46 con la misma regla:

| Política | Planes con aviso | Espalda solo de no demostrados | Hombro solo de no demostrados |
|---|---|---|---|
| REV2 | 1.308 de 3.776 (34,6 %) | 1.015 | 917 |
| HIST | 1.081 de 3.641 (29,7 %) | 868 | 537 |

**Pendiente:**
- La aplicación (`mobileapp`) no se ha tocado: muestra cualquier `Limitation` que reciba, pero eso no se ha verificado en esta orden.
- Los nombres 775 y 973 se muestran tal como están; 973 ya corregido en V46.

## 2. E2-c: ejercicios reales de hombro con peso corporal. SOLO diseño; no se incorpora contenido

**El problema medido:** en «casa sin material» el catálogo activo no tiene ningún ejercicio de hombro con dosis demostrada.
- 973 es un golpe de boxeo.
- 886, la flexión de pino, es de dificultad 3 y de brazos.

Por eso, retirar 973 deja ese hueco (CF1b: planes en casa sin material y sin hombro, 346 → 1.260).

### 2.1 Qué hay ya en el catálogo (no es contenido nuevo)

| ID | Ejercicio | Estado | Por qué podría aportar al hombro | Bloqueo |
|---|---|---|---|---|
| 1113 | Flexión pike a cobra | Activo; pecho; HORIZONTAL_PUSH; dif. 1 | Fase *pike*: empuje con flexión de hombro | Revisión visual pendiente desde V40 (grupo y dificultad); requiere criterio profesional |
| 1058 | Flexión hindú modificada | Activo; pecho; HORIZONTAL_PUSH; dif. 1 | Trayectoria de *pike* a cobra | Ídem |
| 821 | Flexión pike en balón | Activo; exige balón de estabilidad | Empuje vertical | Equipamiento; dificultad 1 dudosa con balón |
| 886 | Flexión de pino | Activo; dif. 3 | Empuje vertical | Solo avanzado; **no sirve para principiantes** |
| 3 | *Pike push-up* (semilla MVP) | **Inactivo**; sin GIF ni instrucciones con licencia; dif. 3 | Empuje vertical | Sin medios con licencia; dificultad sin revisar |

### 2.2 Diseño propuesto (para aprobar antes de hacer nada)

1. **Contenido mínimo:** tres ejercicios con progresión, todos con peso corporal y un apoyo doméstico opcional:
   - *pike push-up* con pies en el suelo (base);
   - *pike push-up* con manos elevadas, como regresión para principiantes;
   - elevaciones Y-T en prono en el suelo (hombro posterior, carga baja).
2. **Ficha obligatoria de cada uno**, sin la cual no entra en el catálogo:
   - licencia del GIF (mismo *dataset* u otro con licencia compatible, registrada);
   - nombre EN/ES;
   - instrucciones según el GIF;
   - músculo objetivo y sinergistas;
   - patrón (VERTICAL_PUSH o un patrón de hombro no estructural);
   - `is_isolation`;
   - **dificultad justificada**;
   - equipamiento real, con requisitos V23;
   - banderas de seguridad V10/V25 (la carga sobre la cabeza activa `overhead`);
   - prescripción.
3. **Validación profesional** de dificultad, dosis por grupo y contraindicaciones (hombro, muñeca, hipertensión en inversión) antes de activarlos.
4. **Medición previa, como en esta orden:** barridos HIST y REV2 con un `sql-previo` que los añada inactivos y los active solo en la base efímera. Se miden factibilidad, hombro sin dosis y V8C.
5. **Solo entonces** se retiraría la dosis de 973 (CF1) en REV2. **En HIST no se toca** (orden).

**Alternativa sin contenido nuevo (no recomendada sin validación):** contar como hombro una fracción de la dosis de los empujes con flexión de hombro (1113, 1058) a través de sus sinergistas. Es una decisión de dosis que requiere criterio profesional y cambia el modelo de dosis por grupo. No se ha implementado.
