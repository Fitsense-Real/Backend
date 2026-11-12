# Base científica para el rediseño de dosis de FitSense

**Fecha:** 05/11/2026 · **Alcance:** solo lectura; no se ha modificado ningún repositorio. · **Complementa a:** `docs/FitSense/auditoria_post_rrp/02_VALIDACION_CIENTIFICA.md`. Las referencias F1–F31 remiten a ese documento y las nuevas se numeran N1–N35 (§8).

**Etiquetas:**
- **[SP]** salud pública;
- **[ER]** evidencia de entrenamiento;
- **[HP]** heurística de producto;
- **[PRO]** decisión profesional;
- **[SEG]** criterio de seguridad, cuando aplica.

**Cómo se verificó:**
- Todas las fuentes nuevas se comprobaron con búsqueda web (PubMed, editorial, repositorios). **No se leyó el texto completo de ninguna.**
- WebFetch no pudo abrir `shura.shu.ac.uk` ni `fisiologiadelejercicio.com` (dominios fuera de la red permitida). Para usar textos completos habría que permitirlos.
- Cuando una cifra procede de un resumen secundario o de memoria, se indica.

**Todas las cifras de las propuestas son [HP] o [PRO] y están pendientes de validación profesional.**

---

## 1. Dosis semanal por objetivo y nivel

| Afirmación | Etiqueta | Fuente(s) | Verificación |
|---|---|---|---|
| Hay que trabajar todos los grupos principales **≥2 días por semana**. Para hipertrofia, unas **10 series por grupo y semana**. Para fuerza, unos 80 % de 1RM y 2-3 series por ejercicio. El fallo, el tipo de equipo y la periodización compleja no cambian los resultados de forma consistente. | [ER] | F1 | Comunicado del ACSM y notas de prensa (no el texto completo). |
| Relación dosis-respuesta con rendimientos decrecientes, **más marcados en fuerza**. La frecuencia solo influye de forma identificable en la fuerza. | [ER] | F8 | Resumen y registros. **No hay un umbral publicado en series por semana.** |
| Volumen semanal en fuerza: los volúmenes medio y alto superan al bajo. En novatos e intermedios, el volumen bajo rinde menos. | [ER] | N1 | Resumen. Los umbrales exactos de cada categoría **no están verificados**. |
| En hombres entrenados basta **1 serie por ejercicio, 2-3 veces por semana** (6-12 repeticiones, 70-85 % de 1RM) para aumentar el 1RM de sentadilla y press de banca, aunque de forma subóptima. | [ER] | N2 | Resúmenes secundarios. |
| Con el volumen igualado, la frecuencia no cambia la ganancia de fuerza: el efecto lo explica el volumen. | [ER] | N3 | Resumen. |
| **9 series semanales de bíceps en 1 sesión** bastaron para aumentar el grosor muscular; con 18-27 series hubo más fuerza. | [ER] | N8 | Resumen. |
| Dosis mínima para principiantes durante las primeras 12 semanas: **1 sesión por semana, menos de 3 series por ejercicio multiarticular**, y al menos 1 serie de 6-15 repeticiones. | [ER] | N5 | Resumen y puntos clave. Es una revisión narrativa. |
| 150-300 min por semana de actividad moderada (o 75-150 de vigorosa), más fuerza ≥2 días por semana. | [SP] | F3 | Verificado (F3). |
| Con menos de 150 min por semana, el efecto sobre el peso es mínimo. Con 225-420 min, la pérdida es de unos 5-7,5 kg. | [ER] | F23 | Verificado (F23). |
| Para mantener el peso perdido: **200-300 min por semana**, dentro de un programa integral. | [SP] | N34 | Resumen y sociedades. La cifra de 150 min de esa guía **no está verificada**. |
| Para inactivos: «empezar bajo e ir despacio», con pequeños aumentos cada 1-2 semanas (los mayores, cada 2-4). | [SP] | N35 | **Solo fuentes secundarias.** No leí el documento del HHS. |
| La regla del 10 % no evita lesiones en corredores novatos. | [ER] | F27 | Verificado (F27). |

**Propuesta operativa** (series **directas** por grupo principal y semana; las indirectas cuentan 0,5, como en F8). Todos los valores son [HP] y [PRO], pendientes de validación.

| Objetivo | Nivel | Mínimo eficaz | Objetivo típico | Techo (ver §3) | Frecuencia por grupo |
|---|---|---|---|---|---|
| `GAIN_MUSCLE` | Principiante | 4 | 6-10 | 12 | 2 días/sem (1 si solo hay 1-2 días) |
| `GAIN_MUSCLE` | Intermedio | 6 | 10-16 | 20 | 2 días/sem |
| `GAIN_MUSCLE` | Avanzado | 8 | 12-20 | 24 | 2-3 días/sem |
| `INCREASE_STRENGTH` (patrón principal: series pesadas, RIR 1-3) | Principiante | 3 (1×3 días) | 4-6 | 8 | 2-3 días/sem |
| `INCREASE_STRENGTH` | Intermedio | 3 | 6-9 | 12 | 2-3 días/sem |
| `INCREASE_STRENGTH` | Avanzado | 4 | 8-12 | 15 | 2-4 días/sem |

`INCREASE_STRENGTH`: el trabajo accesorio va aparte y cuenta para el techo de hipertrofia.

- **`LOSE_WEIGHT`:**
  - **Meta:** 150 min por semana de aeróbico moderado [SP], con una meta ampliada de 200-300 min si el usuario la acepta (N34, F23).
  - **Arranque del principiante:** lo que quepa en 3 sesiones de 15-20 min (unos 45-60 min por semana), y luego **+10-15 min por semana en total cada 1-2 semanas** si no hay fatiga ni dolor. Con ese ritmo, se llega a 150 min en unas 8-12 semanas [HP]. Es un incremento fijo y pequeño, no una «regla del 10 %» (F27).
  - **Fuerza:** ≥2 días por semana, con todos los grupos principales a 1-3 series por grupo y sesión (F3). Objetivo: 4-8 series por grupo y semana [HP].
- **`GENERAL_FITNESS`:**
  - progresar hacia 150 min por semana de aeróbico [SP];
  - fuerza ≥2 días por semana con 4-10 series por grupo y semana [HP];
  - movilidad o equilibrio opcionales. La dosis de F31 sigue sin verificar.
- **La disponibilidad solo limita.** Si los días o los minutos no permiten llegar al **mínimo eficaz**, el plan se genera igualmente y lo indica («dosis por debajo de lo recomendado para tu objetivo»). No se rellena para alcanzar el objetivo típico.

## 2. Sesiones cortas (20-35 min)

| Afirmación | Etiqueta | Fuente(s) | Verificación |
|---|---|---|---|
| Mínimo de unas 4 series por grupo y semana. Prioriza el volumen semanal sobre la frecuencia. Superseries, *drop sets* y *rest-pause* ahorran tiempo. | [ER] | F24 | Verificado (F24). |
| Las dosis mínimas, con menos volumen y carga moderada-alta, o «*snacks*» frecuentes sin material, mejoran la fuerza y la función. Pueden provocar **menos respuestas afectivas negativas**. Conviene elegir ejercicios para los grupos principales. | [ER] | N4 | Resumen. |
| Las **superseries** acortan la sesión sin reducir el volumen. Los efectos crónicos sobre fuerza e hipertrofia son similares (hipertrofia: DME −0,05). Las de agonista-antagonista permiten más repeticiones. **Aumentan el esfuerzo percibido.** | [ER] | N6 | Resumen (19 estudios). El ahorro de tiempo exacto (37-50 %) no es seguro. |
| El calentamiento **específico**, con series de aproximación en el propio ejercicio, mejora el rendimiento. Añadir un calentamiento general no está claro que aporte. | [ER] | N7 | Resumen. Pocos estudios. |
| La actividad aeróbica cuenta sea cual sea la duración del bloque. | [SP] | F28 | Verificado (F28). |

**Propuesta operativa** [HP], pendiente de validación. Una sesión corta es **razonable** si cumple todo esto:
1. **Calentamiento de 3-5 min:** 1-2 series de aproximación del primer ejercicio, o 3 min de aeróbico suave. No hacen falta 10 min.
2. **Ejercicios:** 3-5, priorizando los **multiarticulares** y cubriendo en la semana los patrones empuje, tracción, dominante de rodilla y dominante de cadera. En una sesión de cuerpo completo de 20-25 min, al menos 3 patrones; en una de 30-35 min, 4.
3. **Volumen:** 2-3 series por ejercicio, en total **6-12 series efectivas** (unos 2-2,5 min por serie con 60-90 s de descanso; hasta unas 16 series con superseries agonista-antagonista).
4. **Dosis semanal:** la del grupo llega al mínimo eficaz de §1, sumando las sesiones.

Es **insuficiente** si no llega al mínimo semanal de algún grupo principal sin que el plan lo advierta, si deja sin cubrir un patrón principal en toda la semana, o si en fuerza no hay al menos 1-2 series pesadas por patrón.

**La duración de la sesión no es un criterio.**

## 3. Techo de volumen por experiencia

| Afirmación | Etiqueta | Fuente(s) | Verificación |
|---|---|---|---|
| Faltan datos para saber si más de 10 series por semana añaden beneficio. | [ER] | F4 | Verificado (F4). |
| En entrenados: 12, 18 y 24 series por semana de cuádriceps dieron la misma hipertrofia (F11); 9 series en 1 sesión ya hicieron crecer el bíceps (N8); se recomiendan 12-20 series por semana, y más de 20 solo ayudó al tríceps (F13). | [ER] | F11, N8, F13 | Verificados (resúmenes). |
| Con 30-45 series por semana hubo más hipertrofia, pero en hombres jóvenes entrenados, 8 semanas y con supervisión. | [ER] | F10 | Verificado (F10). |
| Por sesión, el beneficio se aplana hacia unas 11 series fraccionales por músculo (hipertrofia) y unas 2 (fuerza). | [ER] | F9 | **Preprint** sin revisión por pares. |
| Guías de volumen para maximizar la hipertrofia. | [ER] | N9 | Existe. **No pude verificar sus cifras** (de memoria: unas 10-20 series por semana); no se usa como respaldo numérico. |
| Los «hitos de volumen» MEV/MAV/MRV (Israetel) son un marco práctico de entrenadores. | [HP] | — | No revisado por pares; no se cita como evidencia. |
| No hay estudios que fijen un techo específico para principiantes. | — | — | Ausencia de evidencia: el techo del principiante es prudencia, no un dato. |

**Propuesta operativa** [HP]+[PRO]. Techos duros con series fraccionales:

| Nivel | Techo semanal por grupo | Techo por sesión y grupo (hipertrofia) | Techo por sesión y patrón (fuerza pesada) |
|---|---|---|---|
| Principiante | **12** | **6** | 3 |
| Intermedio | **20** | **8-10** | 4 |
| Avanzado | **24** (26 como excepción si el usuario lo pide) | **10** | 5 |

- Si el techo choca con los minutos disponibles, **gana el techo**: la sesión termina antes.
- La progresión no supera el techo.
- **[PRO]:** los grupos pequeños (gemelo, abdomen, deltoide posterior) pueden tener techos propios.

## 4. Fatiga y *deload*

| Afirmación | Etiqueta | Fuente(s) | Verificación |
|---|---|---|---|
| El *deload* es un periodo corto de menos estrés. Se reducen las repeticiones por serie, las series o ambas. | [ER]·consenso | F19 | Resumen. **No pude leer sus principios de diseño concretos** (dominio bloqueado). |
| Encuesta a 246 atletas: el *deload* dura unos **6,4 días**, cada unos **5,6 semanas**. Se reducen las series y las repeticiones, **también la carga**, y el esfuerzo (**más RIR**). Se mantienen la frecuencia y la **selección de ejercicios**. | [HP]·práctica | N11 | Resumen. Es una descripción de práctica real, no prueba de eficacia. |
| Puesta a punto (*taper*) óptima en deportistas: **2 semanas, volumen −41-60 %**, manteniendo la intensidad y la frecuencia. | [ER] | N10 | Resumen. Se estudió en resistencia y competición, no en hipertrofia. |
| Con 1/3 del volumen se mantuvo la hipertrofia 32 semanas en jóvenes; en mayores hace falta más. Una semana sin entrenar no la alteró. | [ER] | F20, F21, F22 | Verificado. |
| El sobreentrenamiento no funcional y el síndrome de sobreentrenamiento se diagnostican **por exclusión clínica**. Ningún marcador es suficiente por sí solo, y el estrés no deportivo contribuye. | [ER]·consenso | N12 | Resumen. |
| Signos que obligan a derivar: dolor o molestia torácica, en cuello, mandíbula o brazo; disnea o **fatiga inusual con las actividades habituales**; mareo o síncope; palpitaciones; soplo conocido. | [SEG] | N13 | Existe. **La lista exacta procede de una fuente secundaria.** |

**Propuesta operativa** [HP]+[PRO]:
1. **Fatiga declarada → 1 semana de dosis reducida:**
   - **series −33 a −50 %** (3→2 o 2→1 por ejercicio);
   - mantener los ejercicios y la frecuencia;
   - **RIR +1-2** (por ejemplo, de 2 a 3-4);
   - carga igual o un ~10 % menor;
   - en aeróbico, minutos −30 a −50 % e intensidad suave.

   Vale también para `GAIN_MUSCLE`, como «dosis de mantenimiento» válida (F21).
2. **Vuelta en 2 pasos**, si la semana anterior no se declaró fatiga ni dolor:
   - semana +1: un **80-85 %** de la dosis previa;
   - semana +2: el **100 %**.

   Si vuelve la fatiga, repetir el paso, no la reducción completa.
3. **Fatiga persistente:**
   - **2 semanas seguidas, o 3 de las últimas 6:** proponer revisar el sueño, el estrés, la alimentación y si hay enfermedad, y mantener la dosis reducida.
   - **≥ 3-4 semanas con fatiga o caída del rendimiento:** recomendar consultar a un profesional sanitario (N12) y no aumentar la dosis.
4. **Fatiga normal frente a señales de atención:**
   - Normal: cansancio o agujetas que ceden en 24-72 h.
   - Atención inmediata (parar y derivar): cualquier signo de N13, dolor articular agudo o localizado, dolor que empeora durante la sesión o persiste más de 48-72 h, hinchazón, pérdida de fuerza súbita, hormigueo o adormecimiento.

   Los plazos de horas son [HP]: no encontré una fuente que los fije.

## 5. Desmotivación y adherencia: variar frente a reducir

| Afirmación | Etiqueta | Fuente(s) | Verificación |
|---|---|---|---|
| Variar aleatoriamente los ejercicios en cada sesión (con una app) **aumentó la motivación** y dio adaptaciones similares (n ≈ 21, hombres entrenados, 8 semanas). | [ER] | N14 | Resumen. Muestra pequeña. |
| Una variación **sistemática** puede favorecer la hipertrofia regional y la fuerza; la variación **excesiva o aleatoria** puede comprometerlas. | [ER] | N15 | Resumen (8 estudios, todos con hombres jóvenes). |
| En 121 estudiantes inactivos, durante 6 semanas, el programa con **apoyo a la variedad** logró **más adherencia** (ηp² = 0,05), mediada por la variedad percibida. | [ER] | N16 | Resumen. |
| La motivación autónoma (autonomía, competencia) predice la práctica de ejercicio. Las intervenciones basadas en la teoría de la autodeterminación mejoran algo la conducta. | [ER] | N17, N18 | Resúmenes. Efectos pequeños-moderados. |
| El afecto **durante** el ejercicio moderado predice la actividad futura; el afecto posterior, no. | [ER] | N19 | Resumen secundario. Volumen y páginas no verificados. |
| Las dosis mínimas pueden reducir el malestar y la caída del disfrute. | [ER] | N4 | Resumen. |
| No encontré **ninguna evidencia** de que quitar 1 repetición mejore la motivación. | — | — | Ausencia de evidencia. |

**Propuesta operativa** [HP]:
- **Por defecto, ante `LACK_OF_MOTIVATION`:** **sustituir 1 ejercicio accesorio** por otro equivalente (mismo grupo y patrón, igual o menor dificultad) y conservar los principales al menos 4-6 semanas para no frenar la fuerza (N15).
- **Ofrecer elegir** (autonomía, N17 y N18):
  - (a) cambiar 1-2 ejercicios;
  - (b) una sesión más corta, quitando las series accesorias, nunca por debajo del mínimo eficaz semanal;
  - (c) mover o quitar un día.
- **Descartar «quitar 1 repetición» como intervención motivacional:** el cambio de estímulo y de experiencia es imperceptible, y no hay evidencia.
- **Desmotivación 2 semanas seguidas:** preguntar por las preferencias de ejercicio y de horario.

## 6. Impacto articular

| Afirmación | Etiqueta | Fuente(s) | Verificación |
|---|---|---|---|
| Fuerza vertical máxima al **caminar, unas 1,2 veces el peso corporal**, que sube hasta unas **2,5 al correr a 6 m/s** y se estabiliza al esprintar. El **trote lento** produjo picos y tasas de carga **más de un 50 % mayores** que la marcha. | [ER] | N20 | Resumen. |
| **Saltos:** salto vertical, 3,34 veces el peso corporal; salto horizontal, 4,22; caída desde 60-90 cm, 4,30-5,39. | [ER] | N21 | Resumen (14 atletas). |
| La intensidad pliométrica se puede cuantificar por fuerzas y tasa de desarrollo de la fuerza. Los gestos **a una pierna** cargan más cada miembro. | [ER] | N22, N23 | N22, solo la cita y fuentes que la citan; N23, el resumen sin cifras. |
| Según la NSCA (Potach y Chu), la intensidad pliométrica depende de los **puntos de apoyo** (una pierna > dos), la velocidad, la altura y el peso del sujeto. Escala de menos a más: saltos en el sitio < saltos de pie < cajones < saltos con rebote y multisaltos < caídas desde altura. | [ER]·manual | N26 | **No verificado en la fuente primaria** (solo diapositivas y fichas secundarias). |
| La **elíptica** tiene una tasa de carga menor que la marcha en el contacto del talón, aunque más momento extensor de rodilla. | [ER] | N24 | Resumen (15 hombres). |
| En personas con obesidad: caminar, bicicleta estática, natación y actividades de bajo impacto. Progresión gradual. | [SP]·revisión clínica | N25 | Resumen. No es un posicionamiento del ACSM. **No verifiqué el texto del ACSM** sobre obesidad. |

**Propuesta operativa: criterio para el catálogo** [HP], con la base física de N20, N21 y N26.
- **ALTO:** el ejercicio tiene **fase de vuelo con aterrizaje vertical de alta exigencia**, o es **a una pierna**, o es un **sprint**, o hay **caída desde altura**.
  - Palabras clave: *salto, jump, hop, bound, tuck, box jump, depth, pliométrico, burpee (con salto), sprint, salto a una pierna, lunge jump, skater.*
  - El sprint se clasifica ALTO por la exigencia muscular y la tasa de carga, no porque su fuerza máxima sea mayor (N20) [HP].
- **MODERADO:** **vuelo de baja amplitud, bilateral y repetitivo**, o carrera por debajo del sprint.
  - Palabras clave: *trote, jog, correr, skipping, high knees, jumping jack, comba o cuerda, rebote suave, mountain climber rápido.*
- **BAJO:** **siempre hay un pie en el suelo, o no se carga el peso corporal**.
  - Palabras clave: *caminar, marcha (en el sitio), step-touch, subir escalón sin salto, bici, ciclo, elíptica, remo (ergómetro), natación,* y los ejercicios de fuerza sin salto.
  - Bici, remo y natación no tienen impacto contra el suelo: es una deducción por ausencia de apoyo, no una medición.
- **Campos recomendados** en el catálogo: `flightPhase` (sí/no), `unilateralLanding`, `dropHeight`, `impactLevel`. La regla por palabras clave sirve solo de respaldo; la etiqueta la revisa una persona.
- **Valores por defecto [SEG]+[PRO]:**
  - principiante, o `LOSE_WEIGHT` en casa: **solo BAJO**;
  - MODERADO, opcional tras unas 4 semanas sin dolor;
  - ALTO, solo para intermedios y avanzados que lo acepten.

## 7. Tracción sin material

| Afirmación | Etiqueta | Fuente(s) | Verificación |
|---|---|---|---|
| Entrenar **todos los grupos principales**. | [SP]/[ER] | F3, F1 | Verificado. |
| **No encontré ninguna guía** que fije una proporción empuje/tracción. El ACSM de 2009 habla de emparejar antagonistas para potenciar el rendimiento, no de equilibrio. | — | F2 | Búsqueda negativa. |
| Hasta un **36 %** de las lesiones atribuidas al entrenamiento de resistencia son de hombro. Entre los factores, los desequilibrios musculares y la técnica, con evidencia **retrospectiva**. | [ER] | N32 | Resumen. |
| Quien entrena con pesas por ocio tiene ratios agonista/antagonista alterados frente a quien no entrena. | [ER] | N31 | Resumen. Es una asociación, no una causa. |
| El remo invertido (con barra) activa más el dorsal ancho y la espalda alta, con **la menor carga lumbar**. | [ER] | N29 | Resumen (n = 7). |
| Las **bandas elásticas** dan una fuerza similar a la resistencia convencional, y las flexiones con banda, a la del press de banca. | [ER] | N27, N28 | Resúmenes. Para N27 hay una fe de erratas de 2020. |
| Ejercicios escapulares sin carga externa útiles para el trapecio medio e inferior: abducción horizontal en prono con rotación externa, extensión en prono, rotación externa y flexión en decúbito lateral. | [ER] | N30 | Resumen. Electromiografía en sanos; no hay resultados clínicos. |
| **Seguridad de mesa, puerta o toalla:** no hay estudios, solo blogs (riesgo de vuelco o de que la puerta ceda). | [SEG] | — | Búsqueda negativa. |
| El **retroceso de las bandas** produce traumatismos oculares (11 pacientes y 14 ojos; un 33 % con agudeza ≤ 20/60). Se recomienda **protección ocular** y un anclaje seguro. | [SEG] | N33 | Resumen. |

**Propuesta operativa** [HP]+[SEG]+[PRO]:
- **Casa solo con peso corporal:**
  - **no generar** por defecto remo bajo mesa, remo con toalla en puerta ni dominadas improvisadas;
  - incluir **2-4 series por semana de ejercicios escapulares en prono** (Y-T-W, extensión en prono, «ángeles» inversos) como trabajo de espalda alta, que cuentan como series directas de trapecio medio e inferior y deltoide posterior, pero **no** de dorsal;
  - mostrar el aviso: «sin material de tracción, el trabajo de dorsal queda limitado; una banda o una barra lo resuelve».
- **Opción del usuario:**
  - *«Tengo banda»* activa remos y jalones con banda: anclaje de puerta fabricado para ello, por el lado de la bisagra con la puerta cerrada hacia el lado contrario, revisar la banda antes de usarla y protección ocular recomendada (N33).
  - *«Tengo barra o soporte firme»* activa el remo invertido (N29).
- **Equilibrio:** objetivo blando de series de tracción ≥ series de empuje horizontal en la semana, siempre que haya equipo [HP]. Sin equipo, se acepta el desequilibrio y se informa de ello, en lugar de inventar ejercicios inseguros.

---

## 8. Fuentes nuevas

| # | Referencia | Estado |
|---|---|---|
| N1 | Ralston GW, Kilgore L, Wyatt FB, Baker JS. The Effect of Weekly Set Volume on Strength Gain: A Meta-Analysis. *Sports Med.* 2017;47(12):2585-2601. doi:10.1007/s40279-017-0762-7. PMID 28755103. | Resumen. Los tamaños del efecto varían entre versiones. |
| N2 | Androulakis-Korakakis P, Fisher JP, Steele J. The Minimum Effective Training Dose Required to Increase 1RM Strength in Resistance-Trained Men. *Sports Med.* 2020 (en línea en 2019). PMID 31797219. | Resúmenes secundarios. **El volumen, las páginas, el DOI y los coautores (de memoria) no están verificados.** |
| N3 | Grgic J, Schoenfeld BJ, Davies TB, Lazinica B, Krieger JW, Pedisic Z. Effect of Resistance Training Frequency on Gains in Muscular Strength. *Sports Med.* 2018;48(5):1207-1220. PMID 29470825 (probable). | Resumen. **El DOI no está verificado.** |
| N4 | Fyfe JJ, Hamilton DL, Daly RM. Minimal-Dose Resistance Training… *Sports Med.* 2022;52(3):463-479. doi:10.1007/s40279-021-01605-8. PMID 34822137. | Resumen. |
| N5 | Behm DG, Granacher U, Warneke K, Aragão-Santos JC, Da Silva-Grigoletto ME, Konrad A. Minimalist Training… *Sports Med.* 2024;54(2):289-302. doi:10.1007/s40279-023-01949-3. PMID 37924459. | Resumen y puntos clave. |
| N6 | Zhang X, Weakley J, Li H, Li Z, García-Ramos A. Superset Versus Traditional Resistance Training Prescriptions… *Sports Med.* 2025 (en línea el 4/2/2025). doi:10.1007/s40279-025-02176-8. | Resumen. **El volumen y las páginas no están verificados.** |
| N7 | Ribeiro B et al. The effect of warm-up in resistance training and strength performance: a systematic review. *Motricidade.* 2021;17(1). | Resumen. **Los coautores no están verificados.** |
| N8 | Heaselgrave SR et al. Dose-Response Relationship of Weekly Resistance-Training Volume and Frequency… *Int J Sports Physiol Perform.* 2019;14(3):360-368. PMID 30160627. | Resumen. |
| N9 | Schoenfeld BJ, Grgic J. Evidence-based guidelines for resistance training volume to maximize muscle hypertrophy. *Strength Cond J.* 2018;40(4):107-112. | Existe. **El contenido no está verificado.** |
| N10 | Bosquet L, Montpetit J, Arvisais D, Mujika I. Effects of tapering on performance: a meta-analysis. *Med Sci Sports Exerc.* 2007;39(8):1358-1365. doi:10.1249/mss.0b013e31806010e0. PMID 17762369. | Resumen. |
| N11 | Rogerson D, Nolan D, Korakakis PA, Immonen V, Wolf M, Bell L. Deloading Practices in Strength and Physique Sports: A Cross-sectional Survey. *Sports Med Open.* 2024 (PMC10948666). | Resumen. **El DOI y el número de artículo no están verificados.** |
| N12 | Meeusen R et al. Prevention, diagnosis and treatment of the overtraining syndrome: joint consensus ECSS/ACSM. *Med Sci Sports Exerc.* 2013;45(1):186-205. doi:10.1249/MSS.0b013e318279a10a (también en *Eur J Sport Sci* 2013;13(1):1-24). | Resumen. |
| N13 | Riebe D et al. Updating ACSM's Recommendations for Exercise Preparticipation Health Screening. *Med Sci Sports Exerc.* 2015;47(11):2473-2479. doi:10.1249/MSS.0000000000000664. | Existe. **La lista de signos procede de una fuente secundaria.** |
| N14 | Baz-Valle E, Schoenfeld BJ, Torres-Unda J, Santos-Concejero J, Balsalobre-Fernández C. The effects of exercise variation in muscle thickness, maximal strength and motivation in resistance trained men. *PLoS One.* 2019;14(12):e0226989. | Resumen. **Los coautores son de memoria.** |
| N15 | Kassiano W, Nunes JP, Costa B, Ribeiro AS, Schoenfeld BJ, Cyrino ES. Does Varying Resistance Exercises Promote Superior Muscle Hypertrophy and Strength Gains? *J Strength Cond Res.* 2022;36(6):1753-1762. doi:10.1519/JSC.0000000000004258. PMID 35438660. | Resumen. |
| N16 | Sylvester BD et al. Variety support and exercise adherence behavior: experimental and mediating effects. *J Behav Med.* 2016;39(2):214-224. doi:10.1007/s10865-015-9688-4. | Resumen. |
| N17 | Teixeira PJ, Carraça EV, Markland D, Silva MN, Ryan RM. Exercise, physical activity, and self-determination theory: a systematic review. *Int J Behav Nutr Phys Act.* 2012;9:78. doi:10.1186/1479-5868-9-78. | Resumen. |
| N18 | Ntoumanis N et al. A meta-analysis of self-determination theory-informed intervention studies in the health domain. *Health Psychol Rev.* 2021;15(2):214-244. | Resumen. **El DOI no está verificado.** |
| N19 | Rhodes RE, Kates A. Can the affective response to exercise predict future motives and physical activity behavior? A systematic review. *Ann Behav Med.* 2015. doi:10.1007/s12160-015-9704-5. | Fuentes secundarias. **El volumen y las páginas no están verificados.** |
| N20 | Keller TS et al. Relationship between vertical ground reaction force and speed during walking, slow jogging, and running. *Clin Biomech.* 1996;11(5):253-259. doi:10.1016/0268-0033(95)00068-2. PMID 11415629. | Resumen. |
| N21 | Wallace BJ, Kernozek TW et al. Quantification of vertical ground reaction forces of popular bilateral plyometric exercises. *J Strength Cond Res.* 2010;24(1):207-212. doi:10.1519/JSC.0b013e3181c3b841. | Resumen. |
| N22 | Jensen RL, Ebben WP. Quantifying plyometric intensity via rate of force development, knee joint, and ground reaction forces. *J Strength Cond Res.* 2007;21(3):763-767. | **Solo la cita** (no leí el resumen). |
| N23 | Ebben WP et al. Kinetic quantification of plyometric exercise intensity. *J Strength Cond Res.* 2011;25(12):3288-3298. | Resumen sin cifras. |
| N24 | Lu TW, Chien HL, Chen HL. Joint loading in the lower extremities during elliptical exercise. *Med Sci Sports Exerc.* 2007;39(9):1651-1658. PMID 17805099. | Resumen. |
| N25 | Higgins JP, Higgins CL. Prescribing exercise to help your patients lose weight. *Cleve Clin J Med.* 2016;83(2):141-150. doi:10.3949/ccjm.83a.14139. | Resumen. Es una revisión clínica. |
| N26 | Potach DH, Chu DA. Capítulo «Plyometric training», en *NSCA's Essentials of Strength Training and Conditioning* (Human Kinetics). | **No verificado** (solo fuentes secundarias). |
| N27 | Lopes JSS et al. Effects of training with elastic resistance versus conventional resistance on muscular strength. *SAGE Open Med.* 2019;7. doi:10.1177/2050312119831116 (fe de erratas: doi:10.1177/2050312120961220). | Resumen. |
| N28 | Calatayud J et al. Bench press and push-up at comparable levels of muscle activity results in similar strength gains. *J Strength Cond Res.* 2015;29(1):246-253. doi:10.1519/JSC.0000000000000589. PMID 24983847. | Resumen. |
| N29 | Fenwick CM, Brown SH, McGill SM. Comparison of different rowing exercises… *J Strength Cond Res.* 2009;23(2):350-358. PMID 19197209. | Resumen. |
| N30 | Cools AM et al. Rehabilitation of scapular muscle balance: which exercises to prescribe? *Am J Sports Med.* 2007;35(10):1744-1751. doi:10.1177/0363546507303560. | Resumen. |
| N31 | Kolber MJ, Beekhuizen KS, Cheng MS, Hellman MA. Shoulder joint and muscle characteristics in the recreational weight training population. *J Strength Cond Res.* 2009;23(1):148-157. doi:10.1519/JSC.0b013e31818eafb4. | Resumen. |
| N32 | Kolber MJ, Beekhuizen KS, Cheng MS, Hellman MA. Shoulder injuries attributed to resistance training: a brief review. *J Strength Cond Res.* 2010;24(6):1696-1704. doi:10.1519/JSC.0b013e3181dc4330. | Resumen. |
| N33 | Al-khersan H et al. Ocular trauma secondary to exercise resistance bands during the COVID-19 pandemic. *Am J Emerg Med.* 2021;42:217-220. doi:10.1016/j.ajem.2020.11.054. | Resumen. |
| N34 | Jensen MD et al. 2013 AHA/ACC/TOS Guideline for the Management of Overweight and Obesity in Adults. *Circulation* 2014 (también en *J Am Coll Cardiol.* 2014;63(25):2985-3023). doi:10.1161/01.cir.0000437739.71477.ee. | Resúmenes (200-300 min por semana para mantener). |
| N35 | U.S. Department of Health and Human Services. *Physical Activity Guidelines for Americans*, 2.ª ed., 2018 (resumida en F28). | Pasaje «*start low and go slow*»: **solo fuentes secundarias**. |

**Remisiones:** F1 (las cifras de ≥2 días por semana, unas 10 series por semana y un 80 % de 1RM en 2-3 series quedan ahora confirmadas por el comunicado del ACSM, no por el texto completo), F3, F4, F8-F11, F13, F19-F24, F27, F28.
