# Párrafos para el short paper (sustituyen los dos párrafos amarillos con X)

**Archivo fuente:** el short paper no está en el repositorio, así que estos párrafos **deben insertarse en el archivo fuente**.

**Contenido:**
- Las cifras salen de `metricas.json` (BASE-R1, BASE-R2, GRA-R1 y GRA-R2).
- No queda ningún X, XX ni TBD.
- La comparación es descriptiva salvo la prueba de McNemar indicada.
- No se afirma más que lo medido.

## Párrafo 1 · generación (inglés, para el artículo)

To measure the effect of the rules, we ran the same model, with the same profiles and the same filtered catalogue, without FitSense's planning rules (no dose, volume, pattern or distribution constraints, no validator feedback, no retries, no fallback), in two runs. Evaluated afterwards with the backend's own validators, 38/38 (100.0%) of the unconstrained first proposals for the feasible profiles violated at least one rule, with 1296 distinct violations (34.1 per plan), mainly dose (783), mandatory movement patterns (117), sets and repetitions (107) and per-muscle-group volume (87). Under FitSense, 13/38 (34.2%) first proposals were rejected (41 violations), every rejection was resolved on retry, and no published plan violated a rule. The unconstrained model also produced a plan for the infeasible profile (19/17 violations), which FitSense had reported as a limitation before any model call. In the paired per-profile comparison, the difference was significant in each run (exact McNemar, p = 0.00049, p = 0.00024). The unconstrained model always selected exercises from the filtered catalogue.

## Párrafo 2 · adaptación (inglés, para el artículo)

For adaptation, the unconstrained model received the same published plans and execution events as FitSense, without the flagged sessions, permitted interventions or conservation rules. 20/20 (100.0%) of its proposals contained at least one unauthorized change (107 in total), mostly modifications of sessions that did not require changes (44) and increases in dose or session workload (31), and 5 cases kept an exercise excluded for pain. Under FitSense, 0/20 (0.0%) published adaptations contained unauthorized changes. The same independent oracle, which encodes the backend's intervention permissions and conservation rules, evaluated both conditions.

## Notas para quien inserte el texto

- **Denominadores:** 19 perfiles factibles por corrida; O11 aparte. En adaptación, 10 ciclos por corrida, los mismos de GRA.
- **Etapa comparable:** primera propuesta de cada condición tras el normalizador mecánico del backend. Sin normalizador, las cifras de la línea base son mayores; ver la Tabla A.
- **Peso de V24:** gran parte de las infracciones de dosis (V24) incumplen tablas de dosis internas de FitSense que la línea base, por diseño, no recibe. Es justo lo que mide el experimento (el efecto de esas reglas), pero conviene decirlo en una frase si el revisor lo pregunta.
- **Versión:** la versión del modelo se solicitó fijada a la de GRA (`9f4cd9ec…`). Replicate la informa como `hidden`, así que no se puede confirmar.
