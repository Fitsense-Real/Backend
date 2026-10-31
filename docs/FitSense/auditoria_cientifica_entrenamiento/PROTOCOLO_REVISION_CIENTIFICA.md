# Protocolo de revisión científica: auditoría de reglas de prescripción de FitSense

- **Fecha de búsqueda y redacción:** 2026-10-31 (todas las búsquedas se hicieron ese día).
- **Ámbito:** adultos sanos de Lima Metropolitana. Objetivos GAIN_MUSCLE, INCREASE_STRENGTH, LOSE_WEIGHT y GENERAL_FITNESS. Niveles principiante, intermedio y avanzado. Sesiones de 30–75 min, de 1 a 6 por semana (máximo 4 en principiantes).
- **Producto:** reglas de generación de planes semanales. Se revisan en el documento `REVISION_CIENTIFICA_EXHAUSTIVA.md`.

## 0. Declaración de alcance (léase primero)

**Esto NO es una revisión sistemática formal ni cumple PRISMA 2020 completo.** Es una **revisión rápida estructurada** ("rapid review") con estrategia documentada. Motivos:

1. **Sin acceso a bases bibliográficas.** PubMed, PMC, Europe PMC, Crossref y doi.org estaban bloqueados a nivel de red (ver §2). No se pudieron lanzar cadenas booleanas reproducibles en una base indexada ni exportar registros. Todas las búsquedas se hicieron con un buscador web general (WebSearch), cuyo índice y orden no son reproducibles.
2. **Sin texto completo.** No se pudo leer el texto completo de **ningún** artículo. Las editoriales (LWW, Springer, Frontiers, BMJ) y los repositorios también estaban bloqueados. La extracción y la valoración crítica se basan en resúmenes, fichas de repositorio y fragmentos indexados.
3. **Un solo revisor.** Una sola persona hizo el cribado y la extracción, sin duplicado independiente ni resolución de discrepancias.
4. **Sin protocolo registrado** (PROSPERO/OSF), sin evaluación formal del riesgo de sesgo de los estudios primarios (RoB 2, ROBINS-I) y sin metaanálisis propio.
5. **Cribado aproximado.** No se pudo deduplicar con un gestor bibliográfico, así que los recuentos de cribado son aproximados (§5).

En consecuencia, las conclusiones son **orientativas** y deben leerse con el nivel de acceso declarado para cada fuente.

## 1. Preguntas de investigación

### 1.1 Por objetivo

**GAIN_MUSCLE (hipertrofia)**

- H1. ¿Qué volumen semanal por grupo muscular (series) es defendible por nivel? ¿Existe un techo útil por sesión?
- H2. ¿Son defendibles 3×10 con 90 s de descanso en ejercicios estructurales y 3×13 en zonas pequeñas?
- H3. ¿Es razonable añadir series de "relleno" para cumplir una duración mínima?
- H4. ¿Qué papel tienen la proximidad al fallo (RIR) y el rango de repeticiones?
- H5. ¿Es razonable estimar 3 s por repetición?

**INCREASE_STRENGTH (fuerza)**

- F1. ¿Qué carga, repeticiones y series se asocian a mayores ganancias de 1RM?
- F2. ¿Puede llamarse "alta intensidad" un 3×5 si no se conoce la carga ni el RIR?
- F3. ¿Es defendible un descanso de 150 s en intermedios y avanzados? ¿Y 120 s en principiantes?
- F4. ¿Tiene respaldo exigir los 4 patrones (empuje, tracción, dominante de rodilla, dominante de cadera) en la semana?
- F5. ¿Basta registrar el último peso y el RPE de sesión para controlar la intensidad y la progresión?

**LOSE_WEIGHT (pérdida de peso)**

- P1. ¿Qué dosis aeróbica semanal (minutos, intensidad) se asocia a pérdida de peso o grasa?
- P2. ¿Constituyen 3 bloques de 90–120 s con 60 s de descanso una dosis aeróbica defendible?
- P3. ¿Qué aporta la fuerza (como componente secundario)? ¿Y el entrenamiento combinado?
- P4. ¿Es HIIT o MICT más eficaz o más eficiente en tiempo?

**GENERAL_FITNESS (condición física general)**

- G1. ¿Qué componentes debe tener un programa multicomponente para adultos sanos?
- G2. ¿Son defendibles las dosis de resistencia muscular (2×15 y 3×18 con 45 s; isométricos 3×40 s)?
- G3. ¿Son defendibles la movilidad 2×30 s y el equilibrio de forma oportunista?
- G4. ¿Existe interferencia entre aeróbico y fuerza en un programa concurrente?

### 1.2 Transversales

- T-a. Descanso entre series: valores defendibles y techo de 180 s.
- T-b. Frecuencia y recuperación: ¿hay evidencia para no repetir un grupo grande en días consecutivos?
- T-c. Progresión: +2–10 % de carga; +2 repeticiones o más series.
- T-d. Duración: mínimo del 70 %, techo del 115 %, calentamiento de 5 min, transición de 60 s. ¿Hay base para una duración mínima? ¿Son eficaces las sesiones cortas?
- T-e. Principiantes: máximo de 2 series por ejercicio; frecuencia máxima de 4 sesiones por semana.
- T-f. Rango de repeticiones de 6 a 30; mínimos de 2 ejercicios, 2 series y 20 s.
- T-g. Adherencia: factores asociados; duración de la sesión.
- T-h. Control de intensidad sin 1RM: RPE/RIR, talk test.

## 2. Fuentes de información y accesibilidad

| Recurso | Uso previsto | Estado el 2026-10-31 |
|---|---|---|
| pubmed.ncbi.nlm.nih.gov | Búsqueda indexada | **BLOQUEADO** (curl: CONNECT 403) |
| pmc.ncbi.nlm.nih.gov | Texto completo | **BLOQUEADO** (curl: CONNECT 403) |
| europepmc.org / www.ebi.ac.uk | Búsqueda y API | **BLOQUEADO** (curl a www.ebi.ac.uk: 403) |
| api.crossref.org | Verificación de DOI | **BLOQUEADO** (curl: 403) |
| doi.org | Resolución de DOI | **BLOQUEADO** (curl: 403) |
| iris.who.int | Guía OMS | **BLOQUEADO** (curl: 403) |
| www.researchgate.net | Texto completo de autor | **BLOQUEADO** (curl: 403) |
| journals.lww.com | ACSM 2026 (MSSE) | **BLOQUEADO** (WebFetch: ENOTFOUND) |
| link.springer.com | Pelland 2026 | **BLOQUEADO** (WebFetch: ENOTFOUND) |
| www.frontiersin.org | Singer 2024 | **BLOQUEADO** (WebFetch: ENOTFOUND) |
| bjsm.bmj.com | OMS 2020 | **BLOQUEADO** (WebFetch: ENOTFOUND) |
| sportrxiv.org | Remmert 2025 | **BLOQUEADO** (WebFetch: ENOTFOUND) |
| WebSearch (buscador general) | Localización de fichas, resúmenes y repositorios | **Disponible**. Única vía utilizada. |

Tras el primer fallo de cada dominio no se volvió a intentar, como pedía el encargo. Lo que sí se vio, solo a través de los fragmentos del buscador, fueron fichas de repositorios institucionales (AUT, VU, Deakin, JYU, ECU, Newcastle, White Rose, UVic), registros de PubMed y PMC indexados por el buscador, DOAJ, PEDro, catálogos de bibliotecas y notas de prensa (ACSM, EurekAlert). Ninguna de esas páginas se abrió directamente.

## 3. Estrategia de búsqueda (cadenas exactas, WebSearch, 2026-10-31)

El buscador no admite operadores booleanos completos, así que se usaron cadenas de lenguaje natural con frases entre comillas. "Ext." indica el modo extendido del buscador; el resto se lanzó en modo estándar.

| # | Cadena exacta | Pregunta |
|---|---|---|
| 1 | `Currier 2026 ACSM position stand resistance training prescription muscle function hypertrophy physical performance healthy adults overview of reviews` (ext.) | H1, F1 |
| 2 | `Pelland 2026 Sports Medicine resistance training dose response meta-regressions weekly volume frequency hypertrophy strength 10.1007/s40279-025-02344-w` (ext.) | H1, T-b |
| 3 | `ACSM 2024 consensus statement "Physical Activity and Excess Body Weight and Adiposity for Adults" Jakicic` (ext.) | P1 |
| 4 | `ACSM 2026 resistance training position stand recommendations "sets per week" load 80% 1RM frequency failure rest intervals Phillips` (ext.) | H1, F1, T-a |
| 5 | `Jakicic 2024 ACSM consensus excess body weight resistance training aerobic minutes per week weight loss recommendations Translational Journal` | P1, P3 |
| 6 | `Bull 2020 World Health Organization 2020 guidelines on physical activity and sedentary behaviour British Journal of Sports Medicine 54 1451 adults 150-300 minutes` | P1, G1 |
| 7 | `ACSM 2009 position stand "Progression models in resistance training for healthy adults" Ratamess rest intervals 3-5 min 1-2 min hypertrophy 2-10% load increase` | T-a, T-c |
| 8 | `Singer Wolf Generoso Schoenfeld 2024 "Give it a rest" inter-set rest interval hypertrophy Bayesian meta-analysis Frontiers studies participants` | H2, T-a |
| 9 | `Grgic 2018 Sports Medicine "rest interval duration" muscular strength systematic review untrained 60-120 s trained longer rest` | F3 |
| 10 | `Grgic Lazinica Mikulic Krieger Schoenfeld 2017 short versus long inter-set rest intervals hypertrophy systematic review European Journal of Sport Science` | H2 |
| 11 | `Robinson Pelland Remmert Refalo Jukic Steele Zourdos 2024 proximity to failure meta-regressions strength hypertrophy Sports Medicine number of studies` | H4, F2 |
| 12 | `Refalo Helms Trexler Hamilton Fyfe 2023 influence of resistance training proximity-to-failure on skeletal muscle hypertrophy systematic review meta-analysis Sports Medicine` | H4 |
| 13 | `Refalo 2024 "Similar muscle hypertrophy following eight weeks of resistance training to momentary muscular failure or with repetitions-in-reserve" resistance-trained` | H4 |
| 14 | `Schoenfeld Grgic Krieger 2019 "How many times per week should a muscle be trained" meta-analysis Journal of Sports Sciences number of studies volume-equated` | T-b |
| 15 | `Grgic Schoenfeld Davies Lazinica Krieger Pedisic 2018 "Effect of Resistance Training Frequency on Gains in Muscular Strength" meta-analysis Sports Medicine 22 studies` | T-b |
| 16 | `Remmert Pelland 2025 SportRxiv "per-session volume" hypertrophy strength meta-regression fractional sets point of undetectable superiority` | H1 |
| 17 | `Schoenfeld Ogborn Krieger 2017 dose-response weekly resistance training volume muscle mass meta-analysis Journal of Sports Sciences 10 sets` | H1 |
| 18 | `Baz-Valle 2022 Journal of Human Kinetics systematic review different resistance training volumes muscle hypertrophy 12-20 sets` | H1 |
| 19 | `Androulakis-Korakakis Fisher Steele 2020 minimum effective training dose 1RM strength resistance-trained men systematic review Sports Medicine single set` | F1, T-d |
| 20 | `Iversen Norum Schoenfeld Fimland 2021 "No Time to Lift" time-efficient training programs strength hypertrophy narrative review Sports Medicine` | T-d |
| 21 | `Kassiano 2022 "Does Varying Resistance Exercises Promote Superior Muscle Hypertrophy and Strength Gains" systematic review Journal of Strength and Conditioning Research` | H3 |
| 22 | `Gentil 2015 single vs multi-joint resistance exercises effects on muscle strength and hypertrophy untrained elbow flexors Asian J Sports Med` | F4 |
| 23 | `Paoli Gentil Moro Marcolin Bianco 2017 resistance training with single vs multi-joint exercises at equal total load volume Frontiers in Physiology` | F4 |
| 24 | `Ramos-Campo 2024 split versus full-body resistance training routines meta-analysis hypertrophy strength` | T-b |
| 25 | `Schumann 2022 Sports Medicine compatibility of concurrent aerobic and strength training for skeletal muscle size and function updated systematic review meta-analysis` | G4 |
| 26 | `Willis 2012 Journal of Applied Physiology aerobic resistance or combination training body mass fat mass overweight adults STRRIDE AT/RT` | P3 |
| 27 | `meta-analysis 2023 2024 resistance training versus aerobic versus combined training body fat weight loss adults overweight obesity network meta-analysis` | P3 |
| 28 | `Wewege van den Berg Ward Keech 2017 HIIT vs MICT body composition overweight obese meta-analysis Obesity Reviews 40% less time` | P4 |
| 29 | `Poon 2024 Sports Medicine efficacy of interval training body composition adiposity apparently healthy adults umbrella review meta-analysis` | P4 |
| 30 | `visceral fat network meta-analysis 2024 Obesity Reviews 84 RCTs aerobic resistance combined HIIT authors` | P3 |
| 31 | `Garber 2011 ACSM position stand quantity and quality of exercise cardiorespiratory musculoskeletal neuromotor fitness flexibility 2-3 days 60 seconds per joint neuromotor balance` | G1, G3 |
| 32 | `2023 2024 systematic review meta-analysis stretching range of motion healthy adults flexibility training dose Konrad Behm` | G3 |
| 33 | `systematic review meta-analysis balance training healthy young adults dose-response Lesinski 2015 Sports Medicine` | G3 |
| 34 | `Konrad Alizadeh Behm 2024 Journal of Sport and Health Science chronic effects stretching range of motion meta-analysis 77 studies DOI` | G3 |
| 35 | `Helms 2018 RPE versus percentage 1RM loading periodized programs strength trained men Frontiers in Physiology rating of perceived exertion repetitions in reserve` | F5, T-h |
| 36 | `Schoenfeld Ogborn Krieger 2015 effect of repetition duration during resistance training on muscle hypertrophy systematic review meta-analysis 0.5 to 8 seconds` | H5 |
| 37 | `Ribeiro 2021 Motricidade effect of warm-up in resistance training and strength performance systematic review` | T-d |
| 38 | `2023 2024 2025 systematic review meta-analysis warm-up resistance training performance hypertrophy general specific warm-up` | T-d |
| 39 | `systematic review adherence exercise program session duration dropout adults supervised resistance training adherence meta-analysis 2022 2023 2024` | T-g |
| 40 | `Androulakis-Korakakis 2020 Sports Medicine minimum effective dose abstract "single set" 6-12 repetitions 70-85% 1RM 2-3 times per week squat bench press` | F1 |
| 41 | `Currier 2023 British Journal of Sports Medicine resistance training prescription muscle strength hypertrophy Bayesian network meta-analysis 178 studies` | F1, H1 |
| 42 | `Lopez Radaelli 2021 resistance training load effects muscle hypertrophy strength gain network meta-analysis Medicine Science Sports Exercise low moderate high load` | H4, T-f |
| 43 | `Rhodes Lubans Karunamuni Kennedy Plotnikoff 2017 factors associated with participation in resistance training systematic review British Journal of Sports Medicine` | T-g |
| 44 | `Journal of Exercise Science & Fitness 2024 prevalence adults meeting muscle-strengthening guideline meta-analysis 22.8%` | T-g |
| 45 | `Wilk 2021 Sports Medicine "movement tempo" resistance training review repetition duration time under tension recommendations` | H5 |
| 46 | `ACSM 2009 progression models "2-10%" increase in load "one to two repetitions over the desired number" recommendation` | T-c |
| 47 | `Rhea Alvar Burkett Ball 2003 meta-analysis dose-response strength development untrained trained 4 sets per muscle group 60% 1RM Medicine Science Sports Exercise` | T-e |
| 48 | `Krieger 2010 single vs multiple sets resistance exercise muscle hypertrophy meta-analysis Journal of Strength and Conditioning Research 2-3 sets` | T-e |
| 49 | `Milanović Sporiš Weston 2015 high-intensity interval training versus continuous training VO2max meta-analysis Sports Medicine` | G1, P4 |
| 50 | `WHO 2020 physical activity guidelines removed 10-minute bout requirement "all activity counts" evidence bouts of any length` | P2 |
| 51 | `low-volume high-intensity interval training cardiorespiratory fitness systematic review meta-analysis adults 2022 2023 2024 total work time under 10 minutes` | P2 |
| 52 | `Sultana Sabag Keating Johnson 2019 Sports Medicine effect of low-volume high-intensity interval training on body composition and cardiorespiratory fitness` | P2 |
| 53 | `Jakicic 1995 multiple short bouts versus one continuous bout exercise adherence weight loss overweight women International Journal of Obesity` | P2, T-g |
| 54 | `ACSM 2009 progression models recovery "48 to 72 h" same muscle group novice training frequency rest days between sessions` | T-b |
| 55 | `Nuzzo 2024 Sports Medicine maximal number of repetitions at percentages of one repetition maximum meta-regression` | F2 |
| 56 | `Halperin 2022 Sports Medicine accuracy in predicting repetitions to task failure resistance exercise meta-analysis RIR` | F2, T-h |
| 57 | `Remmert "Is There Too Much of a Good Thing" per-session volume published journal 2025 2026 peer-reviewed` | H1 |
| 58 | `"Resistance Training Prescription for Muscle Function, Hypertrophy, and Physical Performance in Healthy Adults" rest interval recommendation evidence statement inter-set rest` (ext.) | T-a |
| 59 | `meta-analysis muscular endurance adaptations low-load versus high-load resistance training repetitions to failure local muscular endurance` | G2 |
| 60 | `talk test validity exercise intensity prescription systematic review ventilatory threshold Reed Pipe` | T-h, P2 |
| 61 | `"The influence of resistance exercise training prescription variables on skeletal muscle mass, strength, and physical function in healthy adults: An umbrella review" authors Journal of Sport and Health Science` | H1, T-a |
| 62 | `Sports Medicine 2022 10.1007/s40279-022-01690-3 talk test RPE ventilatory threshold respiratory compensation validity` | T-h |
| 63 | `resistance training movement patterns push pull squat hinge balanced program evidence agonist antagonist ratio shoulder injury review` | F4 |
| 64 | `Garber 2011 ACSM position stand flexibility exercise "60 s" each joint ≥2-3 days warm-up cool-down neuromotor exercise 2-3 days per week 20-30 min` | G3, T-d |
| 65 | `Keating Johnson Mielke Coombes 2017 Obesity Reviews systematic review meta-analysis interval training versus moderate-intensity continuous training body adiposity` | P4 |
| 66 | `critique Pelland 2024 dose-response meta-regression weekly volume hypertrophy preprint limitations k=35 studies` | H1 |
| 67 | `ACSM 2009 progression models local muscular endurance "light to moderate loads (40-60% of 1 RM)" high repetitions ">15" short rest periods "<90 s"` | G2 |
| 68 | `Oranchuk Storey Nelson Cronin 2019 isometric training and long-term adaptations effects of muscle length intensity intent systematic review Scandinavian Journal` | G2 |

**Sesgo de la estrategia.** Muchas cadenas parten de autores ya conocidos (las fuentes prioritarias del encargo y un borrador previo de la sesión). Esto favorece la evidencia conocida y puede pasar por alto trabajos menos citados. Para mitigarlo se lanzaron cadenas temáticas abiertas (n.º 27, 32, 38, 39, 51, 59, 63), que localizaron Chen 2024, Wang 2024, Konrad 2024 y Bok 2022.

## 4. Criterios de elegibilidad

**Inclusión**

- **Población:** adultos de 18 años o más, sanos o "aparentemente sanos". Para LOSE_WEIGHT se aceptaron también adultos con sobrepeso u obesidad sin otra enfermedad.
- **Diseño, por orden de preferencia:**
  1. Guías y posicionamientos (OMS, ACSM).
  2. Revisiones paraguas.
  3. Revisiones sistemáticas con o sin metaanálisis.
  4. Metarregresiones.
  5. ECA clave citados por las guías o que responden a una regla concreta.
  6. Preprints solo si son la única evidencia sobre una pregunta (marcados como tales).
- **Fechas:** sin límite inferior, con preferencia por 2023–2026. Se mantienen trabajos clásicos (ACSM 2009, Garber 2011, Rhea 2003, Krieger 2010) cuando siguen siendo la referencia de un valor concreto.
- **Variables:** fuerza (1RM), hipertrofia (grosor, CSA, masa magra), masa/porcentaje de grasa, VO2max, ROM, equilibrio, adherencia/participación, precisión de RIR/RPE.
- **Idioma:** inglés, español o portugués.

**Exclusión**

- Blogs, boletines comerciales, apps y webs de entrenadores (p. ej. biolayne, getfitcraft, planfit, outlift, setforset, substack, vitonica, macrofactor). Se usaron solo como **pista** para localizar la fuente primaria y nunca como evidencia.
- Poblaciones clínicas específicas (cáncer, sarcopenia, diabetes, cardiopatía), salvo como contexto metodológico. Por ejemplo, la revisión del talk test en cardiópatas se excluyó como evidencia.
- Estudios en animales.
- Duplicados y versiones preprint cuando existe la versión publicada (p. ej. el preprint de Pelland frente a Sports Med 2026).
- Trabajos cuyos metadatos básicos (año, revista y título) no pudieron confirmarse en al menos una ficha.

## 5. Cribado (aproximado)

| Paso | Recuento | Comentario |
|---|---|---|
| Consultas lanzadas | 68 | Lista en §3 |
| Enlaces mostrados por el buscador | ≈ 680 (unos 10 por consulta) | Con muchos duplicados entre consultas |
| Registros únicos considerados por título y fragmento | ≈ 140 (estimación) | Sin gestor de referencias; recuento manual aproximado |
| Excluidos por título o fragmento | ≈ 87 | Ver motivos abajo |
| **Incluidos en la tabla de estudios** | **53** | 51 con acceso RESUMEN y 2 SOLO_REFERENCIA |
| Con texto completo leído | **0** | Todos los hosts de texto completo bloqueados |

**Motivos principales de exclusión, de más a menos frecuente (estimado):**

1. Blog o contenido comercial (≈ 35).
2. Población clínica, mayores o animal (≈ 15). Ejemplos: el metaanálisis en pacientes oncológicos mayores, el de roedores y el de sarcopenia.
3. Duplicado o versión preprint de un trabajo publicado (≈ 15).
4. Sin relación con la pregunta (≈ 12). Ejemplos: patentes, protocolos de ClinicalTrials.gov y estudios sobre prescripciones generadas por IA.
5. Metadatos insuficientes o no verificables (≈ 10). Ejemplos: la revisión de 2025 sobre sarcopenia y adherencia, la cohorte de 2026 de un estudio y la revisión de 2025 sobre lesiones.

**Trabajos vistos pero no incluidos en la tabla, con su motivo:**

- Jakicic 1999 (JAMA): solo una frase de resumen secundario. Se menciona en el texto como dato no verificado.
- Fisher et al. 2022 (supervisión en entrenamiento de fuerza): no responde a ninguna regla concreta.
- PAPE 2025 (Sports Med): resultados de rendimiento agudo de potencia.
- Lesinski 2015 en mayores: población fuera del ámbito.
- Vieira 2022 (talk test en cardiópatas): población clínica.

## 6. Extracción de datos

Campos extraídos para cada fuente (ver `ESTUDIOS_SELECCIONADOS.csv`): autores, año, título, revista, DOI o URL, verificación del DOI, tipo de estudio, población, experiencia, n, variables, resultado relevante, limitaciones, nivel de acceso, AMSTAR 2, objetivos de FitSense y reglas relacionadas.

**Reglas de extracción**

- Solo se registraron cifras que aparecieron en un resumen o ficha en esta sesión. Las que solo aparecían en blogs o notas de prensa se marcan como "fuente secundaria / no verificado".
- **doi_verificado = SI** significa que el DOI aparece en al menos una ficha editorial o indexadora vista en los resultados de búsqueda de esta sesión. **No** significa que se haya resuelto en doi.org, que estaba bloqueado. **NO** significa que el DOI no apareció en esta sesión. En ese caso se da el PMID o la URL y nunca un DOI recordado.
- Las listas de autores incompletas se marcan "lista no verificada". No se completaron coautores de memoria.

**Niveles de acceso**

- **TEXTO_COMPLETO:** no se asignó a ninguna fuente.
- **RESUMEN:** se vio el resumen o la ficha con resultados.
- **SOLO_REFERENCIA:** solo se vio la cita o un resumen secundario. Aplica a Lundberg 2022 y a la revisión breve de lesiones de hombro de 2010.
- **NO_DISPONIBLE:** se aplicó a fuentes buscadas y no localizadas, que no figuran en la tabla: ACSM GETP 12.ª ed., una revisión sistemática que aísle la duración de la sesión como predictor de adherencia y una revisión sistemática de 2023+ sobre calentamiento e hipertrofia.

## 7. Valoración crítica

- **AMSTAR 2 aproximado.** AMSTAR 2 exige juzgar 16 ítems; los críticos son el 2, 4, 7, 9, 11, 13 y 15. Con solo el resumen no se pueden juzgar la mayoría (búsqueda exhaustiva, lista de excluidos, riesgo de sesgo, sesgo de publicación). Por eso a casi todas las revisiones se les asigna **"No evaluable"**, añadiendo los indicios visibles en el resumen: protocolo registrado (Singer 2024 en OSF; Schumann 2022 y Androulakis-Korakakis 2020 en PROSPERO), doble revisor (Poon 2024) o valoración de calidad (Kassiano 2022 con PEDro). **Ninguna revisión recibe una calificación global de AMSTAR 2** porque no puede justificarse sin el texto completo. Para Rhea 2003, anterior a AMSTAR 2 y con críticas publicadas, se indica "probable calidad baja".
- **Guías y consensos** (OMS 2020, ACSM 2009/2011/2024/2026) no se valoran con AMSTAR 2. Se consideran consenso de expertos informado por evidencia.
- **GRADE orientativo.** La certeza (alta, moderada, baja o muy baja) se asigna por conclusión y no por estudio. Se parte de alta para metaanálisis de ECA y se baja por inconsistencia, imprecisión, evidencia indirecta (por población o porque la regla de FitSense no coincide con lo estudiado) y por acceso solo al resumen. **No es una evaluación GRADE formal**: no hay perfiles de evidencia ni dos evaluadores.
- **Penalización por acceso.** Ninguna conclusión con acceso RESUMEN se presenta como verificada en el texto completo. Si una cifra proviene solo de una nota de prensa o de un resumen secundario, la certeza baja un nivel.

## 8. Síntesis

- **Método:** síntesis narrativa por objetivo y por dimensión. Sin metaanálisis propio.
- **Etiquetas por afirmación:**
  - **[EVIDENCIA DIRECTA]**: la fuente lo dice, según el resumen o la ficha.
  - **[INFERENCIA]**: deducción razonada.
  - **[DECISIÓN DE INGENIERÍA]**: valor de producto que la literatura no fija.
- **Escala de veredictos por regla:**
  - **A**: respaldada directamente.
  - **B**: compatible y defendible.
  - **C**: decisión de ingeniería justificable.
  - **D**: evidencia insuficiente.
  - **E**: potencialmente problemática.
- **Principio de "conservación fundamentada":** si la evidencia admite el valor, la regla se clasifica B aunque no sea óptima. Solo se clasifica E si hay contradicción con la evidencia o un riesgo demostrable.

## 9. Limitaciones del protocolo

1. Ningún texto completo leído. Ninguna conclusión puede considerarse verificada a ese nivel.
2. Búsqueda no reproducible con un buscador general, sin bases indexadas.
3. Un solo revisor.
4. Sesgo hacia autores conocidos (§3).
5. Posible sesgo de publicación no evaluado.
6. Algunas cifras clave proceden solo de notas de prensa o de resúmenes secundarios. El caso principal es ACSM 2026: ~80 % 1RM, 2–3 series y ≥10 series/semana. Siempre se marcan.
7. Las fuentes no aportan datos específicos de población peruana o latinoamericana. La aplicabilidad a Lima Metropolitana es indirecta (adultos sanos en general).

## 10. Pasos para completar una revisión formal (recomendado)

1. Habilitar en la red de la sesión journals.lww.com, link.springer.com, www.frontiersin.org, bjsm.bmj.com, pmc.ncbi.nlm.nih.gov, pubmed.ncbi.nlm.nih.gov, doi.org, api.crossref.org y sportrxiv.org.
2. Registrar el protocolo en OSF.
3. Lanzar cadenas booleanas en PubMed, SPORTDiscus y Scopus, exportarlas y deduplicarlas.
4. Hacer el cribado por duplicado.
5. Extraer datos del texto completo, empezando por ACSM 2026 (tablas de recomendaciones) y Pelland 2026 (curvas dosis-respuesta).
6. Aplicar AMSTAR 2 completo y GRADE formal.
7. Preparar un diagrama PRISMA 2020.
