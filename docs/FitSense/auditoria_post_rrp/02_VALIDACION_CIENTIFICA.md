# 02 · Validación científica de las políticas de prescripción

> **Estado:** contraste integrado en la auditoría post R/R′ (05/11/2026).
>
> **Límites de la verificación:**
> - Las referencias se comprobaron con búsqueda web contra PubMed, las editoriales y el ACSM: existencia, autores, revista, año, DOI o PMID, y hallazgos del resumen.
> - **No se leyó el texto completo de ninguna fuente**, porque los dominios de texto completo no son accesibles desde la red de la sesión. Cuando una cifra no se pudo contrastar, se indica.
> - **Nada de este documento es una validación profesional.** Distingue evidencia de salud pública, evidencia de entrenamiento, criterios de seguridad, heurísticas de producto y decisiones que debe tomar un profesional.
>
> **Corrección de ejecución:** la dosis de fuerza de principiante (STRENGTH) que se observa en el motor es **2×8 con 120 s**, ampliable a 3×8 si hace falta para llegar al piso. Lo confirman los borradores reales del diagnóstico J03 (`evidencia/j03/borrador_motor.csv`).


**Fecha:** 05/11/2026 · **Alcance:** solo lectura; no se modificó ningún repositorio.
**Contexto leído:** `docs/FitSense/ADENDA_4D_v3_1..v3_4.md`, `POLITICA_ADAPTACION_Y_PROGRESION.md`, `DISENO_PROGRESION_Y_REINCORPORACION.md`, `IMPLEMENTACION_V20.md`, `PASO4_5_GENERACION_POR_OBJETIVO.md`.

## 0. Cómo leer este documento

Cada afirmación lleva una de estas etiquetas:

| Etiqueta | Significado |
|---|---|
| **[SP]** | Recomendación de salud pública (OMS, guías nacionales). Pensada para la población, no para optimizar un objetivo. |
| **[ER]** | Evidencia de entrenamiento de resistencia (metaanálisis, ensayos, posicionamientos). |
| **[SEG]** | Criterio de seguridad (prudencia razonable; poca evidencia cuantitativa). |
| **[HP]** | Heurística de producto o decisión de ingeniería. Sin respaldo científico específico. |
| **[PRO]** | Decisión que debería tomar o validar un profesional (entrenador titulado, fisiólogo del ejercicio o clínico). |

**Limitaciones de la verificación (importante):**
- En esta sesión, WebFetch no pudo abrir `pmc.ncbi.nlm.nih.gov` ni `newswise.com` (los nombres no resolvían porque esos dominios no están permitidos en la red de la sesión). Por eso dejé de intentar abrir textos completos.
- Todas las referencias se comprobaron con búsqueda web (WebSearch) contra registros de PubMed, la editorial, repositorios institucionales o el propio ACSM. Comprobé la existencia, los autores, la revista, el año y el DOI o el PMID, y los hallazgos principales tal como aparecen en el resumen. **No leí el texto completo de ninguna fuente.**
- Cuando una cifra concreta no pude contrastarla, lo digo expresamente («no verificado en esta sesión»). Ninguna cita es inventada. Las que no encontré se señalan como no encontradas.

## 1. Fuentes verificadas

| # | Referencia | Estado de la verificación |
|---|---|---|
| F1 | Currier BS, D'Souza AC, Fiatarone Singh MA, Lowisz CV, Rawson ES, Schoenfeld BJ, Smith-Ryan AE, Steen JP, Thomas GA, Triplett NT, Washington TA, Werner TJ, Phillips SM. *American College of Sports Medicine Position Stand. Resistance Training Prescription for Muscle Function, Hypertrophy, and Physical Performance in Healthy Adults: An Overview of Reviews.* **Med Sci Sports Exerc. 2026;58(4):851-872.** doi:10.1249/MSS.0000000000003897 (publicado en línea el 5/3/2026). | **Existe y es de 2026** (lo confirman Ovid/MSSE, ACSM y el autor que presidió el comité). Es un *overview* de 137 revisiones sistemáticas con más de 30.000 participantes, y sustituye al de 2009. Hallazgos del resumen: el entrenamiento de resistencia supera a no entrenar en fuerza, hipertrofia, potencia, resistencia y función; **la mayoría de las variables de prescripción influyen poco**; la hipertrofia mejora con más volumen (**≥10 series/semana**) y con sobrecarga excéntrica; la potencia, con cargas moderadas (30-70 % 1RM). ACSM lo resume como «el mejor programa es el que vas a hacer». Según resúmenes secundarios (no verificados en el texto): entrenar todos los grupos ≥2 días por semana, y no hace falta llegar al fallo (bastaría un esfuerzo suficiente, de unas 2-3 RIR). **No pude verificar qué dice sobre los descansos entre series.** |
| F2 | ACSM (Ratamess NA et al.). *Progression Models in Resistance Training for Healthy Adults.* **Med Sci Sports Exerc. 2009;41(3):687-708.** doi:10.1249/MSS.0b013e3181915670. PMID 19204579. | Verificado. Incluye la regla de subir la carga un **2-10 %** cuando se completan 1-2 repeticiones por encima del objetivo, y una frecuencia de 2-3 días por semana en novatos, 3-4 en intermedios y 4-5 en avanzados. Lo que recuerdo de 8-12 RM y 1-2 min de descanso para hipertrofia en novatos e intermedios **no lo verifiqué en esta sesión**. |
| F3 | Bull FC et al. *World Health Organization 2020 guidelines on physical activity and sedentary behaviour.* **Br J Sports Med. 2020;54(24):1451-1462.** doi:10.1136/bjsports-2020-102955. | Verificado. Adultos: 150-300 min/semana de actividad moderada, o 75-150 min de vigorosa, más fortalecimiento muscular de intensidad moderada o mayor de todos los grupos principales **≥2 días por semana**. No fija un umbral cuantitativo de sedentarismo. |
| F4 | Schoenfeld BJ, Ogborn D, Krieger JW. *Dose-response relationship between weekly resistance training volume and increases in muscle mass: a systematic review and meta-analysis.* **J Sports Sci** (en línea en 2016; número de 2017). doi:10.1080/02640414.2016.1210197. | DOI y hallazgos verificados: 15 estudios y 34 grupos; relación dosis-respuesta graduada; **los datos no bastaban para saber si más de 10 series por músculo y semana añaden beneficio**. El volumen y las páginas (35(11):1073-1082) no los pude confirmar en el índice. |
| F5 | Schoenfeld BJ, Ogborn D, Krieger JW. *Effects of Resistance Training Frequency on Measures of Muscle Hypertrophy: A Systematic Review and Meta-Analysis.* **Sports Med. 2016;46(11):1689-1697.** doi:10.1007/s40279-016-0543-8. PMID 27102172. | Verificado: frecuencia ≥2 días por semana > 1 día, sin igualar el volumen (TE 0,49 frente a 0,30). |
| F6 | Schoenfeld BJ, Grgic J, Krieger J. *How many times per week should a muscle be trained to maximize muscle hypertrophy?* **J Sports Sci. 2019** (citado como 37(11):1286-1295; doi:10.1080/02640414.2018.1555906). | El artículo y su conclusión están verificados: con el volumen igualado, la frecuencia no influye de forma relevante. **El DOI y las páginas solo aparecen en fuentes secundarias.** |
| F7 | Singer A, Wolf M, Generoso L, …, Schoenfeld BJ. *Give it a rest: a systematic review with Bayesian meta-analysis on the effect of inter-set rest interval duration on muscle hypertrophy.* **Front Sports Act Living. 2024;6:1429789.** doi:10.3389/fspor.2024.1429789. | Verificado: 9 estudios. Descansar **más de 60 s** da una pequeña ventaja de hipertrofia, y **más allá de 90 s** no se aprecian diferencias. La heterogeneidad es alta. |
| F8 | Pelland JC, Remmert JF, Robinson ZP, Hinson SR, Zourdos MC. *The Resistance Training Dose Response: Meta-Regressions Exploring the Effects of Weekly Volume and Frequency on Muscle Hypertrophy and Strength Gains.* **Sports Med. 2026;56(2):481-505.** doi:10.1007/s40279-025-02344-w. PMID 41343037 (en línea el 4/12/2025; preprint en SportRxiv de 2024). | Verificado. Unos 67 estudios. Más volumen da más ganancia, **con rendimientos decrecientes**, más marcados para la fuerza que para la hipertrofia. El método de series «fraccionales» (las indirectas cuentan 0,5) es el que mejor se ajusta. **Hay pocos datos con volúmenes muy altos.** Que la muestra fuera de unos 2.058 participantes, un 79 % hombres, viene de una fuente secundaria. |
| F9 | Remmert JF, Pelland JC, Robinson ZP, Hinson SR, Zourdos MC. *Is There Too Much of a Good Thing? Meta-Regressions of the Effect of Per-Session Volume on Hypertrophy and Strength.* SportRxiv, 2025 (**preprint sin revisión por pares**). | Verificado como preprint. El beneficio deja de detectarse a partir de unas **11 series fraccionales por músculo y sesión** para hipertrofia, y de unas 2 para fuerza. Advierte de que hay pocos datos con volúmenes muy altos. |
| F10 | Schoenfeld BJ et al. *Resistance Training Volume Enhances Muscle Hypertrophy but Not Strength in Trained Men.* **Med Sci Sports Exerc. 2019;51(1):94-103.** PMID 30153194. | Verificado. Hombres entrenados, 8 semanas: 1, 3 o 5 series por ejercicio, que suponen unas 6-9, 18-27 y **30-45 series por semana**. Más hipertrofia con más volumen y la misma fuerza. |
| F11 | Aube D et al. *Progressive Resistance Training Volume: Effects on Muscle Thickness, Mass, and Strength Adaptations in Resistance-Trained Individuals.* **J Strength Cond Res. 2022;36(3):600-607.** PMID 32058362. | Verificado. 12, 18 o 24 series por semana de cuádriceps: **ninguna diferencia de hipertrofia**. En fuerza, una tendencia (p = 0,052) a favor de 18 frente a 24. Muestra pequeña. |
| F12 | Enes A et al. *Effects of Different Weekly Set Progressions on Muscular Adaptations in Trained Males: Is There a Dose-Response Effect?* **Med Sci Sports Exerc. 2024.** PMID 37796222. | Verificado. Progresar en series mejora más la fuerza. **En hipertrofia, sin diferencias significativas**, con una posible meseta en volúmenes altos. |
| F13 | Baz-Valle E, Balsalobre-Fernández C, Alix-Fages C, Santos-Concejero J. *A Systematic Review of the Effects of Different Resistance Training Volumes on Muscle Hypertrophy.* **J Hum Kinet. 2022;81:199-210.** | Verificado. Sin diferencias entre 12-20 y más de 20 series por semana en cuádriceps y bíceps; más de 20 favorece al tríceps. Proponen **12-20 series/semana** para hombres jóvenes entrenados. Solo 7 estudios. |
| F14 | Barbalho M et al. 2020, IJSPP («Evidence of a ceiling effect…»). | **Retractado** (PMID 32804467). No se usa. |
| F15 | Robinson ZP, Pelland JC, Remmert JF, Refalo MC, Jukic I, Steele J, Zourdos MC. *Exploring the Dose-Response Relationship Between Estimated Resistance Training Proximity to Failure, Strength Gain, and Muscle Hypertrophy: A Series of Meta-Regressions.* **Sports Med. 2024;54(9):2209-2231.** doi:10.1007/s40279-024-02069-2. | Verificado. 55 estudios. La fuerza es parecida en un rango amplio de RIR; **la hipertrofia mejora al acercarse al fallo**. El RIR es estimado, no medido. |
| F16 | Zourdos MC et al. *Novel resistance training-specific RPE scale measuring repetitions in reserve.* **J Strength Cond Res. 2016;30(1):267-275.** · Helms ER et al. *Application of the RIR-based RPE scale for resistance training.* **Strength Cond J. 2016;38(4):42-49.** | Verificados. La escala RIR es práctica para autorregular la carga. Su precisión empeora lejos del fallo y en series largas (Zourdos et al., JSCR 2021;35(2S):S158-S165). |
| F17 | Hickmott LM et al. *The Effect of Load and Volume Autoregulation on Muscular Strength and Hypertrophy: A Systematic Review and Meta-Analysis.* **Sports Med Open. 2022.** doi:10.1186/s40798-021-00404-9. PMID 35038063. | Verificado. Autorregular la carga da una fuerza **similar** a la de una prescripción estandarizada. La autorregulación del volumen se estudió sobre todo con pérdida de velocidad. **No compara directamente RIR o RPE con una dosis fija en hipertrofia.** |
| F18 | Larsen S et al. *Autoregulated resistance training for maximal strength enhancement: a systematic review.* **PeerJ. 2021.** (peerj.com/articles/10663) | Verificado como revisión sistemática centrada en la fuerza máxima. |
| F19 | Bell L, Nolan D, Immonen V, Helms E, Dallamore J, Wolf M, Androulakis Korakakis P. *Integrating Deloading into Strength and Physique Sports Training Programmes: An International Delphi Consensus Approach.* **Sports Med Open. 2023.** doi:10.1186/s40798-023-00633-0. | Verificado. Es un consenso de entrenadores, no evidencia experimental: el *deload* es un periodo de menor estrés para manejar la fatiga, y suele hacerse **reduciendo repeticiones por serie, series por sesión o ambas**. La lista completa de autores viene de mi conocimiento; en el índice comprobé el primer autor, la revista y el DOI. |
| F20 | Coleman M et al. *Gaining more from doing less? The effects of a one-week deload period during supervised resistance training on muscular adaptations.* **PeerJ. 2024;12:e16777.** doi:10.7717/peerj.16777. PMID 38274324. | Verificado. **Una semana sin entrenar** a mitad de 9 semanas no cambió la hipertrofia, la potencia ni la resistencia local, y perjudicó algo la fuerza. Ojo: probó una interrupción total, no una reducción. |
| F21 | Bickel CS, Cross JM, Bamman MM. *Exercise dosing to retain resistance training adaptations in young and older adults.* **Med Sci Sports Exerc. 2011;43(7):1177-1187.** PMID 21131862. | Verificado. Tras 16 semanas, en jóvenes **1/3 y 1/9 del volumen** conservaron la hipertrofia durante 32 semanas (con 1/3 incluso aumentó). En mayores, no; necesitan más dosis. |
| F22 | Spiering BA, Mujika I, Sharp MA, Foulis SA. *Maintaining Physical Performance: The Minimal Dose of Exercise Needed to Preserve Endurance and Strength Over Time.* **J Strength Cond Res. 2021;35(5):1449-1458.** doi:10.1519/JSC.0000000000003964. PMID 33629972. | Verificado. Revisión narrativa. En jóvenes, la fuerza y el tamaño se mantienen hasta 32 semanas con 1 sesión por semana y 1 serie por ejercicio **si se conserva la intensidad**; en mayores harían falta unas 2 sesiones y 2-3 series. La intensidad es la variable clave. |
| F23 | Donnelly JE et al. *ACSM Position Stand. Appropriate physical activity intervention strategies for weight loss and prevention of weight regain for adults.* **Med Sci Sports Exerc. 2009;41(2):459-471.** doi:10.1249/MSS.0b013e3181949333. PMID 19127177. | Verificado. Menos de 150 min/semana: efecto mínimo sobre el peso. Más de 150: pérdida modesta (unos 2-3 kg). 225-420 min: 5-7,5 kg. 150-250 min previenen ganar más de un 3 %. **El entrenamiento de resistencia no aumenta la pérdida de peso**, pero puede aumentar la masa libre de grasa y reducir la masa grasa y el riesgo. |
| F24 | Iversen VM, Norum M, Schoenfeld BJ, Fimland MS. *No Time to Lift? Designing Time-Efficient Training Programs for Strength and Hypertrophy: A Narrative Review.* **Sports Med. 2021;51:2079-2095.** doi:10.1007/s40279-021-01490-1. PMID 34125411. | Verificado. Mínimo de **4 series por grupo y semana** (6-15 RM). El volumen semanal importa más que la frecuencia. Superseries, *drop sets* y *rest-pause* reducen el tiempo a la mitad con un volumen similar. Calentamiento específico. |
| F25 | Grgic J, Schoenfeld BJ, Skrepnik M, Davies TB, Mikulic P. *Effects of Rest Interval Duration in Resistance Training on Measures of Muscular Strength: A Systematic Review.* **Sports Med. 2018;48(1):137-151.** doi:10.1007/s40279-017-0788-x. | Verificado. Para fuerza: más de 2 min en entrenados; 60-120 s bastan en no entrenados. |
| F26 | Rhea MR, Alvar BA, Burkett LN, Ball SD. *A meta-analysis to determine the dose response for strength development.* **Med Sci Sports Exerc. 2003;35(3):456-464.** PMID 12618576. | Verificado. En no entrenados, máximo con un 60 % de 1RM y 3 días por grupo y semana; en entrenados, 80 % y 2 días; unas 4 series por grupo en ambos casos. Es fuerza, no hipertrofia. |
| F27 | Buist I et al. *No effect of a graded training program on the number of running-related injuries in novice runners: a randomized controlled trial.* **Am J Sports Med. 2008;36(1):33-39.** | Verificado. La «regla del 10 %» no redujo las lesiones en 532 corredores novatos (20,8 % frente a 20,3 %). |
| F28 | Piercy KL et al. *The Physical Activity Guidelines for Americans.* **JAMA. 2018.** doi:10.1001/jama.2018.14854. | Verificado. La 2.ª edición elimina la exigencia de bloques de 10 min: **cuenta la actividad de cualquier duración**. |
| F29 | Collins KA et al. (ensayos STRRIDE, 2022; PMID 35669034), y la revisión de 2017 en *Osteoporosis International* sobre adherencia. | Verificados por su resumen. En STRRIDE, el 67 % de los abandonos ocurrió antes o durante la rampa hacia la dosis prescrita, y el motivo más citado fue **la falta de tiempo (40 %)**. La cifra clásica de «50 % de abandono en 6 meses» viene de Dishman (1982) y se repite sin una definición uniforme. **De la revisión de 2017 no anoté la referencia completa.** |
| F30 | Wewege M et al. *The effects of high-intensity interval training vs. moderate-intensity continuous training on body composition in overweight and obese adults: a systematic review and meta-analysis.* **Obes Rev. 2017.** PMID 28401638. · Keating SE et al. **Obes Rev. 2017.** PMID 28513103. | Verificados. HIIT y continuo moderado reducen la masa grasa de forma parecida y **modesta**. El ahorro de un 40 % de tiempo del HIIT (Wewege) no lo confirma Keating. |
| F31 | Garber CE et al. *ACSM Position Stand. Quantity and quality of exercise…* **Med Sci Sports Exerc. 2011;43(7):1334-1359.** doi:10.1249/MSS.0b013e318213fefb. | Existe. Verificado: ≥150 min/semana de moderada. **No verifiqué** sus pasajes sobre personas desacondicionadas, la progresión gradual ni la dosis de flexibilidad (10-30 s por estiramiento, unos 60 s por ejercicio). |
| — | *ACSM's Guidelines for Exercise Testing and Prescription*, 12.ª ed. (Wolters Kluwer, 2025). | Existe, pero **no pude verificar sus tablas FITT** de resistencia para novatos. No se usa como respaldo de ninguna cifra. |
| — | «Bell/Nuzzo 2022» sobre autorregulación. | **No encontrado.** No se cita. |

## 2. Preguntas clave

### A. ¿El piso de V20 (duración ≥ 70 % de `sessionMinutes`) tiene base científica? ¿Debería el tiempo declarado ser un máximo?

**Qué dice la evidencia**
- **Ninguna fuente revisada fija una duración mínima de sesión** como condición de eficacia: ni la OMS, ni el ACSM de 2009, 2011 o 2026, ni los metaanálisis de volumen.
- Las dosis se expresan en **minutos por semana** de actividad aeróbica [SP] (F3, F23) y en **series por grupo y semana** para resistencia [ER] (F1, F4, F8, F24).
- Las guías estadounidenses de 2018 cuentan la actividad **de cualquier duración** [SP] (F28).
- La evidencia de entrenamiento eficiente muestra adaptaciones relevantes con poco tiempo [ER] (F24):
  - 4 series por grupo y semana como mínimo;
  - técnicas que reducen el tiempo a la mitad.
- Para la hipertrofia, la duración de la sesión no es la variable activa. Lo son el volumen efectivo, la proximidad al fallo y la frecuencia (F1, F8, F15).
- Sobre la adherencia, la falta de tiempo es la barrera más citada, y la mayoría de los abandonos ocurren en las primeras semanas (F29).
- El ACSM de 2026, según su propia síntesis, insiste en que **individualizar para aumentar la participación** pesa más que afinar las variables (F1, resumen; no verificado en el texto).

**Qué es en realidad el 70 %**
- **[HP] Es una heurística de producto:** «la sesión debe parecerse a lo que el usuario pidió».
- Las adendas muestran que este piso es el que **dirige el volumen**, no al revés:
  - obligó a subir el tope a 7, 8 y 9 ejercicios (4D v3.1 y v3.4);
  - obligó a añadir series «solo si sin ellas no se llega al mínimo» (4D v3.2 y v3.3);
  - y de ahí salen las 36 series por semana (pregunta C).

  Una variable fisiológica, el volumen, queda subordinada a una de presentación, la duración. Es la relación causal inversa a la que propone la evidencia.
- **V4 (≤ minutos + 15 %) permite superar el tiempo declarado.** Si el usuario declara «disponible», un 15 % por encima de lo declarado es incoherente con esa semántica [HP].

**Veredicto:** sin base científica específica. Es razonable como preferencia de producto («llenar el tiempo que me das»), pero **no como restricción dura** que invalide planes o fuerce volumen.

**Propuesta**
1. Interpretar `sessionMinutes` como **máximo disponible**. V4 sería ≤ 100 % de los minutos, o un margen pequeño solo si el usuario lo acepta.
2. Sustituir V20 por un **mínimo de dosis eficaz por objetivo**, medido en la semana y no en la sesión:
   - **Hipertrofia:** unas 10 series directas por grupo principal y semana como objetivo (F1, F4, F8), con un suelo absoluto de unas 4 (F24) para semanas con poco tiempo.
   - **Fuerza:** ≥2 días por grupo (F1, F3, F5).
   - **Pérdida de peso o salud:** progresar hacia 150 min/semana de actividad aeróbica moderada, más fuerza 2 días por semana (F3, F23).
3. Conservar el 70 % solo como **objetivo blando** (rellenar si conviene) o como opción del usuario («quiero usar todo mi tiempo»).
4. **[PRO]** Los umbrales concretos por objetivo y nivel debería fijarlos un profesional.

### B. ¿Tiene respaldo impedir reducir volumen en `GAIN_MUSCLE` ante fatiga declarada?

**Qué dice la evidencia**
- **Reducir el volumen temporalmente no hace perder la hipertrofia a corto plazo** [ER]:
  - en jóvenes, 1/3 e incluso 1/9 del volumen la mantuvieron durante 32 semanas (F21);
  - con 1 sesión por semana y 1 serie por ejercicio, manteniendo la intensidad, se conservan la fuerza y el tamaño (F22);
  - una semana **sin entrenar** no alteró la hipertrofia (F20).
- **Matiz:** en mayores, mantener el tamaño exige más dosis (F21, F22).
- **El *deload* es práctica habitual**, aunque con evidencia experimental limitada: los entrenadores reducen series, repeticiones o ambas para manejar la fatiga (F19, consenso Delphi, no ensayo).
- **La autorregulación** da resultados al menos similares a una prescripción fija en fuerza (F17, F18). Para hipertrofia, la comparación directa RIR frente a dosis fija es escasa.
- La dosis-respuesta del volumen tiene **rendimientos decrecientes** (F8). Recortar un 20-35 % durante una semana cuesta poco en el margen.
- **[SEG]** La fatiga declarada puede deberse a sueño, enfermedad o estrés. Ignorarla mantiene una carga que el usuario ya ha señalado como excesiva, y eso es contrario al principio de prudencia y probablemente a la adherencia (F29).

**Qué es la regla actual:** **[HP]** una decisión de ingeniería. La dosis fija de 4D define lo que «acredita hipertrofia», y V24 rechaza cualquier dosis distinta. Por eso las sesiones `GAIN_MUSCLE` fatigadas «no se adaptan» (`POLITICA_ADAPTACION_Y_PROGRESION.md` §2). Ninguna fuente revisada la respalda.

**Veredicto:** **demasiado rígida** y sin respaldo. Es la que más contradice la evidencia de todas las revisadas.

**Propuesta**
1. Permitir una **reducción temporal acotada** ante `FATIGUE` en `GAIN_MUSCLE`:
   - por ejemplo, 3 → 2 series por ejercicio (cerca de un −33 %), o retirar el relleno;
   - durante 1 semana, manteniendo los ejercicios y el esfuerzo (RIR 2-3);
   - con la vuelta gradual que ya existe (recuperación ≤ 10 % por paso).
2. Que V24 reconozca una **«dosis de mantenimiento»** como válida durante ese periodo. No acreditaría progreso, pero tampoco invalidaría el plan.
3. Aplicar la misma lógica a `LACK_OF_MOTIVATION` si el usuario lo pide.
4. **[PRO]** El tamaño exacto de la reducción y cuántas semanas consecutivas se permiten: con fatiga repetida (2 o más semanas), recomendar una revisión, no solo reducir.

### C. ¿Son razonables 36 series/semana por grupo para un intermedio?

**Qué dice la evidencia**
- **Mínimo para optimizar:** unas 10 series por semana (F1, F4).
- **Por encima de 10**, la dosis-respuesta sigue, pero con rendimientos decrecientes y **muy pocos datos en volúmenes altos** (F8).
- **En entrenados:**
  - 12, 18 y 24 series por semana dieron la misma hipertrofia (F11);
  - progresar las series no dio más hipertrofia significativa (F12);
  - 12-20 series por semana como referencia razonable, y más de 20 solo ayudó al tríceps (F13).
- **El único ensayo con 30-45 series por semana** (F10) favoreció al grupo de más volumen. Pero fueron 8 semanas, con hombres jóvenes **entrenados**, supervisado y en gimnasio. No es extrapolable a un intermedio de app, que puede entrenar en casa y no tiene supervisión.
- **Por sesión:** 36 series en 3 días son 12 series directas por grupo y sesión, en el entorno o por encima del punto en que el preprint F9 deja de detectar beneficio (unas 11 series fraccionales). **Si se cuentan las indirectas** (por ejemplo, el press de pecho suma al tríceps), el volumen fraccional es aún mayor.
- Retirado: Barbalho 2020 (F14) sugería un techo, pero está **retractado**. No se usa.

**Qué es en realidad:** **[HP]** un **efecto colateral** del piso de V20, no una decisión de dosis. La propia adenda 4D v3.4 §4.2 lo reconoce como «adecuación profesional no demostrada».

**Veredicto**
- No hay evidencia de que sea peligroso.
- Pero está **fuera del rango en que se concentra la evidencia** para intermedios (unas 10-20 series).
- Su beneficio adicional esperado es pequeño y el coste en tiempo, fatiga y adherencia, real.
- En resumen: **demasiado laxa** (no hay techo) y conviene una revisión profesional.

**Propuesta**
1. Añadir un **techo semanal por grupo** (validador nuevo), por ejemplo:
   - unas 20 series directas por grupo y semana en principiante e intermedio;
   - unas 24-26 en avanzado.

   Las cifras son **[HP]**, inspiradas en F11 y F13, y debe fijarlas un profesional **[PRO]**.
2. Un **techo por sesión** de unas 10-12 series directas por grupo (F9, todavía preprint).
3. Si el piso de duración choca con el techo, **gana el techo**. Se acorta la sesión, o se usan los minutos para movilidad o cardio opcional si el usuario lo quiere (ver A).

### D. Principiante, `LOSE_WEIGHT`, en casa sin material, 70 min: ¿es razonable exigir 49 min de contenido?

**Cálculo con el estimador de FitSense** (5 min de calentamiento + trabajo + descansos + 60 s de transición; cardio de principiante 3×90 s con 60 s; fuerza de principiante 2×8 con 120 s; tope de 8 ejercicios):
- Piso: ⌈70 × 60 × 0,70⌉ = 2.940 s (49 min).
- Hacen falta como mínimo **4 bloques de cardio (3×90 s) y 4 ejercicios de fuerza**: unos 2.952 s, es decir, 49,2 min.
- Eso supone unos 18 min de cardio efectivo, unos 3 min de fuerza efectiva, unos 16 min de descanso y unos 12 min de calentamiento y transiciones.
- Con 4 días por semana son unos **200 min/semana**.

**Qué dice la evidencia**
- Para perder peso con ejercicio solo, hacen falta más de 150 min/semana para un efecto modesto, y 225-420 para uno mayor (F23) [SP/ER]. **No hay ninguna cifra por sesión.**
- La actividad de cualquier duración cuenta (F28).
- HIIT y continuo moderado dan reducciones de grasa parecidas y modestas (F30).
- Para un **principiante**, lo prudente es **empezar con menos y progresar** [SEG]. Es el principio de progresión de F2 y F31 (sin verificar en esta sesión el pasaje concreto sobre personas desacondicionadas). La mayoría de los abandonos ocurren al empezar o durante la rampa de dosis (F29).

**Factores de seguridad sin cuantificar [SEG/PRO]**
- En casa y sin material, el cardio disponible suele ser **de impacto** (saltos, *jumping jacks*, *burpees*).
- Repetir 12 bloques de 90 s en un principiante, posiblemente con sobrepeso, plantea dudas de impacto articular y de tolerancia.
- No pude verificar qué ejercicios elige el catálogo.
- El documento `IMPLEMENTACION_V20.md` muestra que los perfiles **LW/principiante/4-5 días/casa** fallan precisamente V20 por catálogo. Es otra señal de que el piso no encaja con este perfil.

**Veredicto**
- 49 min de sesión no son en sí peligrosos para un principiante sano a intensidad moderada.
- Pero **exigirlos no tiene base científica**. Además:
  - contradice la progresión gradual;
  - convierte los 70 min disponibles en una obligación;
  - lleva de golpe a unos 200 min/semana a alguien que empieza.

  La regla es **demasiado rígida**.

**Propuesta**
1. Para principiantes:
   - una dosis inicial por debajo del tiempo disponible (por ejemplo, 20-30 min por sesión) [HP], con progresión semanal de la duración hacia la meta de 150 min/semana (F3);
   - **cardio de bajo impacto por defecto** en casa [SEG].
2. **[PRO]** Cribado previo (por ejemplo, un cuestionario de aptitud tipo PAR-Q+; no verificado en esta sesión) para usuarios con obesidad o patología, y revisión del catálogo de cardio en casa.

## 3. Evaluación política por política

### 3.1 `GAIN_MUSCLE` (4D)

| Política | Evidencia | Valoración | ¿Revisión profesional? | Propuesta |
|---|---|---|---|---|
| **Estructural 3×10, RIR 2-3** | 2-3 series por ejercicio y un rango amplio de cargas valen si el esfuerzo es suficiente; no hace falta el fallo (F1 resumen; F15). 8-12 RM era la zona del ACSM 2009 (F2; cifra de memoria, no verificada aquí). | **Coherente en promedio, rígida en la forma.** Un número fijo de 10 repeticiones no garantiza un RIR de 2-3, sobre todo con peso corporal (puede ser imposible o demasiado fácil). El RIR no se valida (`PASO4_5` §2). [ER] + [HP] | Sí, en la selección de variantes de peso corporal. | Prescribir un **rango** (8-15 o 6-15 repeticiones) hasta un RIR de 2-3, con doble progresión: repeticiones y luego una variante más difícil. |
| **Relleno 2×10** | Sin evidencia específica. Coherente con contar volumen semanal (F8). | Decisión de ingeniería razonable. [HP] | No es prioritaria. | Mantener. |
| **Zona pequeña +3 repeticiones (3×13 / 2×13)** | No encontré evidencia específica para gemelo y abdomen. | Heurística sin respaldo, inocua. [HP] | Opcional. | Usar el mismo criterio de rango y RIR. |
| **Descanso de 90 s** | Más de 60 s da una pequeña ventaja; más de 90 s, sin diferencias (F7). Para fuerza en no entrenados bastan 60-120 s (F25). | **Coherente.** [ER] | No. | Mantener. Permitir 60-120 s según el ejercicio. |
| **Serie adicional solo para llegar a V20** | No hay ninguna base: el volumen lo decide el reloj. | **Decisión de ingeniería con la causalidad invertida.** [HP] | Sí. | Que la serie adicional dependa del volumen semanal objetivo del grupo, no de V20. |
| **Tope de 6, 7, 8 o 9 ejercicios** | Ninguna fuente fija un número de ejercicios. El volumen por sesión tiene una meseta aproximada (F9, preprint). | Decisión de ingeniería derivada de V20. [HP] | Sí, junto con C. | Derivarlo del volumen semanal y por sesión, no del piso de duración. |
| **Presupuesto del 85 % (trabajo + descansos)** | Ninguna. | Decisión de ingeniería razonable (deja margen para el calentamiento y las transiciones). [HP] | No. | Mantener. |
| **No reducir la dosis ante fatiga o desmotivación** | Reducir el volumen temporalmente no hace perder la hipertrofia (F19-F22). | **Demasiado rígida; contraria a la evidencia.** [HP] | **Sí.** | Ver B. |
| **36 series/semana por grupo (61-75 min, 3 días)** | La evidencia se concentra en 10-20; meseta en entrenados (F8, F11-F13). | **Demasiado laxa** (sin techo). [HP] | **Sí.** | Ver C. |
| **Fuerza secundaria sin asignar** | Fuerza ≥2 días por semana (F3); la fuerza responde con poco volumen (F8, F9). | Pendiente de diseño. | Sí. | Opcional: 1-2 series pesadas por patrón principal. |

### 3.2 Objetivos con dosis por capacidad (`INCREASE_STRENGTH`, `LOSE_WEIGHT`, `GENERAL_FITNESS`)

| Política | Evidencia | Valoración | ¿Revisión profesional? | Propuesta |
|---|---|---|---|---|
| **STRENGTH 3×5 (en la práctica 3×6 por V16) con 150 s; principiante 2×8 con 120 s** | Cargas altas (unos 80 % 1RM, 2-3 series) para fuerza (F1 según resúmenes; F26). Descanso de más de 2 min en entrenados (F25). | **Coherente en series, repeticiones y descanso**, pero FitSense no prescribe la carga. Con peso corporal, 3×6 no es un estímulo de fuerza salvo con una variante exigente. [ER] + [HP] | Sí, para el entorno doméstico. | Exigir para STRENGTH una variante cuyo RM esté cerca de las repeticiones prescritas (RIR 1-3), o indicar la carga relativa o el RPE. |
| **MUSCULAR_ENDURANCE 3×18, 45 s; principiante 2×15** | La resistencia local tradicionalmente se trabaja con cargas ligeras-moderadas, muchas repeticiones y descansos cortos (F2; detalle numérico no verificado aquí). | **Coherente.** [ER] | No. | Mantener. |
| **CARDIO por duración: 3×90 s (principiante) o 3×120 s, con 60 s; nota «RPE 5-6»** | La salud pública se expresa en minutos semanales (F3). Intervalos y continuo dan efectos parecidos en la grasa (F30). | Formato razonable, pero **no hay un objetivo semanal de minutos aeróbicos** y la intensidad no se valida. [SP] + [HP] | Sí: impacto y catálogo en casa. | Medir y mostrar los minutos aeróbicos por semana y progresar hacia 150 min. Bajo impacto por defecto en principiantes. |
| **MOVILIDAD 2×30 s, 15 s** | Garber 2011 (F31) da recomendaciones de flexibilidad, pero no verifiqué la cifra en esta sesión. | Probablemente coherente; **sin verificar**. [HP] | No es prioritaria. | Mantener. |
| **Tope de 8 ejercicios por sesión** | Ninguna. | Decisión de ingeniería. [HP] | No. | Mantener. |
| **`LOSE_WEIGHT`: cardio en cada sesión factible** | La actividad aeróbica es la palanca del gasto; el entrenamiento de resistencia no aumenta la pérdida de peso, pero conserva la masa libre de grasa (F23). Fuerza ≥2 días por semana (F3). | **Coherente.** Conviene asegurar también fuerza 2 días por semana. Recordar que sin déficit dietético la pérdida con ejercicio solo es modesta (F23). [SP/ER] | No; sí en el mensaje al usuario. | Mantener. Comprobar que haya fuerza ≥2 días por semana. Evitar prometer pérdidas. |
| **Principiante ≤ 4 sesiones/semana; resto ≤ 6** | ACSM 2009: 2-3 días de fuerza en novatos (F2), pero eso es entrenamiento de resistencia, no actividad total. La OMS admite actividad diaria (F3). | Prudencia razonable. [SEG] + [HP] | Opcional. | Mantener. Documentar que es prudencia y no «una prohibición del ACSM» (ya lo señala `EVALUACION_GENERACION_4_6_DIAS.md`). |

### 3.3 Duración (V20 y V4) y estimador

| Política | Evidencia | Valoración | ¿Revisión profesional? | Propuesta |
|---|---|---|---|---|
| **V20: duración ≥ 70 % de `sessionMinutes`** | Ninguna (ver A). | **Heurística de producto; demasiado rígida** como restricción dura. [HP] | Sí. | Ver A: mínimo de dosis semanal por objetivo, y el 70 % como objetivo blando. |
| **V4: ≤ minutos + 15 %** | Ninguna. | Incoherente si los minutos son «disponibles». [HP] | No. | ≤ 100 % de los minutos (o un margen aceptado por el usuario). |
| **Estimador: 5 min + series×repeticiones×3 s + descansos + 60 s por transición** | La duración de la repetición varía mucho. Los 3 s por repetición son una aproximación razonable, no una prescripción. | Decisión de ingeniería aceptable. [HP] | No. | Calibrarlo con datos reales de uso. |
| **30-75 min, 1-7 días** | Ninguna. | Decisión de producto. [HP] | No. | — |

### 3.4 Adaptación

| Política | Evidencia | Valoración | ¿Revisión profesional? | Propuesta |
|---|---|---|---|---|
| **Fatiga → menos trabajo, sin sustituir** | Reducir el volumen conserva las adaptaciones a corto plazo (F19-F22). | **Coherente** en los objetivos con dosis por capacidad; **anulada en `GAIN_MUSCLE`** (ver B). [ER] | Sí, si la fatiga se repite. | Extenderla a `GAIN_MUSCLE`. Con fatiga en 2 o más semanas, mensaje de revisión. |
| **«Demasiado difícil» → variante más fácil del mismo músculo o menos dosis; nunca más difícil** | Principio de progresión: ajustar la exigencia a la capacidad (F2). Esfuerzo suficiente sin llegar al fallo (F1, F15). | **Coherente.** [ER] + [SEG] | No. | Mantener. |
| **Desmotivación → cambio mínimo (1 ejercicio)** | No encontré evidencia específica sobre esta intervención. La adherencia mejora con la individualización (F1, resumen) y se resiente con la falta de tiempo (F29). | Heurística sin respaldo, prudente. [HP] | Opcional (psicología del ejercicio). | Ofrecer opciones (cambiar un ejercicio, acortar, cambiar de día), que el usuario elija. |
| **Dolor → retirada localizada o sustitución segura; `NO_FACTIBLE` si no hay alternativa** | No verifiqué en esta sesión ninguna guía cuantitativa. Es práctica prudente estándar. | **Coherente** como criterio de seguridad. [SEG] | **Sí:** con dolor persistente o con signos de alarma hay que derivar al profesional sanitario. | Mantener. Añadir la recomendación explícita de consulta si el dolor persiste o se repite. |

### 3.5 Progresión (apagada en la evaluación)

**Nota de coherencia con el código documentado:** el encargo dice «2 semanas con adherencia ≥ 80 %, pasos del +10 % o el +5 %». `POLITICA_ADAPTACION_Y_PROGRESION.md` (28/10/2026) y `DISENO_PROGRESION_Y_REINCORPORACION.md` dicen otra cosa:
- la **progresión ordinaria** exige un **100 % real dos semanas** y pasos de **≤ 5 %**;
- la **recuperación** y la **reincorporación** mantienen el 80 % y pasos de **≤ 10 %**;
- techo de 1,10 × la base.

Lo evalúo así.

| Política | Evidencia | Valoración | ¿Revisión profesional? | Propuesta |
|---|---|---|---|---|
| **Pasos de ≤ 5 % (progresión) o ≤ 10 % (recuperación) del volumen** | El 2-10 % del ACSM 2009 se refiere a la **carga**, no al volumen (F2; el propio diseño lo reconoce). La regla del 10 % no redujo las lesiones en corredores (F27). Progresar las series mejora la fuerza, pero no claramente la hipertrofia (F11, F12). | **Heurística prudente sin respaldo específico** para el volumen. [HP] | Sí. | Progresar sobre todo por **repeticiones dentro de un rango y por la dificultad de la variante** (doble progresión). El volumen solo hasta un techo semanal (ver C). |
| **Techo del 110 % del volumen de referencia** | Ninguna. Rendimientos decrecientes del volumen (F8). | Decisión de ingeniería conservadora. Razonable si el punto de partida ya es alto, como en `GAIN_MUSCLE`. [HP] | Sí. | Que el techo sea un número absoluto de series por grupo y semana, no un porcentaje de una base que a su vez sale del piso de duración. |
| **Exigir 2 semanas al 100 %, sin fatiga ni dolor** | Ninguna para el umbral exacto. Es un criterio de disposición razonable. | Prudente, quizá demasiado estricto para la progresión ordinaria. [HP] + [SEG] | Opcional. | Considerar el RIR o el RPE comunicado («me sobraban repeticiones») como disparador (F16). |
| **Reincorporar solo ejercicios retirados** | Ninguna. | Decisión de ingeniería coherente con la seguridad. [HP] + [SEG] | No. | Mantener. |

## 4. Tabla resumen

| Política | Veredicto | Confianza | Fuente principal |
|---|---|---|---|
| GM 3×10, RIR 2-3 | Coherente en promedio; rígida (sin rango) | Media | F1, F15, F2 |
| GM relleno 2×10 | Ingeniería razonable | Media | F8 (indirecta) |
| GM zona pequeña +3 repeticiones | Ingeniería sin respaldo, inocua | Baja | — |
| GM descanso de 90 s | Coherente | Alta | F7, F25 |
| GM serie adicional por V20 | Ingeniería, causalidad invertida | Alta | — (ninguna) |
| GM tope de 6, 7, 8 o 9 ejercicios | Ingeniería derivada de V20 | Alta | — ; F9 (preprint) |
| GM presupuesto del 85 % | Ingeniería razonable | Media | — |
| **GM sin reducción ante fatiga** | **Demasiado rígida; contraria a la evidencia** | **Alta** | F19, F20, F21, F22, F17 |
| **GM hasta 36 series/semana** | **Demasiado laxa; revisión profesional** | Media-alta | F8, F11, F12, F13, F10 |
| STRENGTH 3×5 o 3×6, 150 s | Coherente sin carga; débil con peso corporal | Media | F1, F25, F26 |
| MUSCULAR_ENDURANCE 3×18, 45 s | Coherente | Media | F2 |
| CARDIO 3×90 o 3×120 s, 60 s | Formato razonable; falta un objetivo semanal | Media | F3, F23, F30 |
| MOVILIDAD 2×30 s | Probablemente coherente (no verificado) | Baja | F31 (no verificado) |
| Tope de 8 ejercicios (otros objetivos) | Ingeniería | Media | — |
| LW con cardio en cada sesión | Coherente; añadir fuerza 2 días/semana | Alta | F23, F3 |
| Principiante ≤ 4, resto ≤ 6 sesiones | Prudencia razonable | Media | F2, F3 |
| **V20 ≥ 70 %** | **Heurística de producto; demasiado rígida** | **Alta** | F3, F28, F24, F29 (ausencia de respaldo) |
| V4 ≤ +15 % | Incoherente con «disponible» | Media | — |
| Estimador de duración | Ingeniería aceptable | Media | — |
| Fatiga → reducir | Coherente (salvo en GM) | Alta | F19-F22 |
| Difícil → más fácil | Coherente | Alta | F2, F15 |
| Desmotivación → 1 cambio | Heurística prudente | Baja | — |
| Dolor → retirada o sustitución | Coherente (seguridad); falta derivación clínica | Media | [SEG] |
| Progresión ≤ 5 % / ≤ 10 % | Heurística prudente; el 2-10 % del ACSM es de carga | Media | F2, F27, F11, F12 |
| Techo del 110 % | Ingeniería conservadora | Media | F8 |
| **D: principiante LW en casa, 49 de 70 min** | **Sin base; demasiado rígida; revisión profesional** | Media-alta | F23, F3, F28, F29 |

## 5. Prioridades para la revisión profesional

1. **Volumen semanal por grupo:** techo y mínimo (pregunta C), y que el piso de duración deje de generar volumen (A).
2. **Reducción temporal de la dosis en `GAIN_MUSCLE`** ante fatiga (B).
3. **Principiantes con `LOSE_WEIGHT` en casa:** duración inicial, impacto del cardio y cribado (D).
4. **Dosis con peso corporal:** rango de repeticiones y RIR frente a 10 repeticiones fijas; fuerza sin carga externa.
5. **Mensajes de derivación** ante dolor persistente o fatiga repetida.
