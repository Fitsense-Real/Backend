# Revisión científica actualizada: verificación de fuentes (FitSense)

**Fecha:** 2026-11-01.
**Alcance:** verificar las afirmaciones en las que se apoya la decisión de conservar las reglas. No se rediseña nada.
**Documento de partida:** `REVISION_CIENTIFICA_EXHAUSTIVA.md` (2026-10-31), en adelante "RCE". Sus archivos no se han modificado.
**Revisión humana:** ninguna. Este documento lo ha generado un agente.

---

## 1. Resultado principal en una línea

**No se pudo leer el texto completo de ninguna fuente.** El acceso máximo obtenido es RESUMEN (fragmentos del resumen vistos a través del buscador) o FUENTE_SECUNDARIA. Ninguna clase de regla cambia. Varias afirmaciones de la RCE deben matizarse (§6).

---

## 2. Método

### 2.1 Herramientas

- **WebFetch:** 8 intentos. Los 8 fallaron por DNS (`getaddrinfo ENOTFOUND`). El entorno indica que el host está fuera de los dominios permitidos de la sesión.
- **WebSearch:** 27 consultas (modo estándar o extendido). Funcionó.
- **No se usó** curl ni Bash para red. No se intentó sortear el proxy ni la política de red.

### 2.2 Qué devuelve WebSearch y cómo se ha tratado

- WebSearch devuelve títulos, URL y un resumen redactado por el propio buscador, con frases citadas de las páginas.
- Una frase citada del resumen de PubMed, de la editorial o de un repositorio cuenta como **RESUMEN** (fragmento).
- Una frase de blog, nota de prensa, infografía o resumen de terceros cuenta como **FUENTE_SECUNDARIA**.
- Ningún dato obtenido así se considera **TEXTO_COMPLETO**: no se vieron métodos ni resultados completos.
- Cuando la consulta incluía un dato (p. ej. un DOI) y el buscador solo lo "confirmó", se marca como **confirmación débil** (posible eco de la consulta).

### 2.3 Tabla de bloqueos (WebFetch)

| # | URL intentada | Herramienta | Resultado |
|---|---|---|---|
| 1 | https://pmc.ncbi.nlm.nih.gov/articles/PMC12965823/ | WebFetch | ENOTFOUND (DNS; fuera de dominios permitidos) |
| 2 | https://www.who.int/publications/i/item/9789240015128 | WebFetch | ENOTFOUND |
| 3 | https://link.springer.com/article/10.1007/s40279-025-02344-w | WebFetch | ENOTFOUND |
| 4 | https://api.openalex.org/works/doi:10.1249/MSS.0000000000003897 | WebFetch | ENOTFOUND |
| 5 | https://api.semanticscholar.org/graph/v1/paper/DOI:10.1007/s40279-025-02344-w | WebFetch | ENOTFOUND |
| 6 | https://acsm.org/resistance-training-guidelines-update-2026/ | WebFetch | ENOTFOUND |
| 7 | https://exrx.net/WeightTraining/Guidelines | WebFetch | ENOTFOUND |
| 8 | https://eprints.whiterose.ac.uk/215080 (repositorio institucional, ACSM 2024) | WebFetch | ENOTFOUND |

**No se intentaron**, porque el fallo era sistemático y el entorno pidió no seguir probando hosts: Unpaywall, Europe PMC, frontiersin.org, iris.who.int, journals.lww.com, ovid.com, ResearchGate, SportRxiv, pureadmin.qub.ac.uk, PMC7719906 (OMS en BJSM).

**Copias de terceros localizadas y no usadas:** el buscador mostró PDF del ACSM 2026 y de Pelland 2026 alojados en fisiologiadelejercicio.com. Se desconoce su licencia de redistribución, y WebFetch no habría podido abrirlos. No se usaron.

**Para lograr TEXTO_COMPLETO** habría que permitir en la configuración de red de la sesión al menos: `pmc.ncbi.nlm.nih.gov`, `www.who.int` o `iris.who.int`, `link.springer.com` y `journals.lww.com`.

---

## 3. Fichas de las fuentes prioritarias

### 3.1 ACSM 2026: Resistance Training Prescription… An Overview of Reviews [S01]

| Campo | Verificado | Detalle |
|---|---|---|
| Acceso real | — | **RESUMEN** (fragmentos del resumen vía buscador) + **FUENTE_SECUNDARIA** (infografía oficial del ACSM, notas de prensa del ACSM y McMaster, blogs) |
| Autores | Parcial | Primer autor Currier BS; presidente Phillips SM (nota oficial ACSM). Lista de 13 autores solo en ExRx (secundaria): Currier, D'Souza, Fiatarone Singh, Lowisz, Rawson, Schoenfeld, Smith-Ryan, Steen, Thomas, Triplett, Washington, Werner, Phillips. **No verificada en la ficha editorial.** |
| Revista, vol., págs. | Sí, con reserva | *Med Sci Sports Exerc*, 58(4), 851–872. El título del PDF indexado empieza por "851 SPECIAL COMMUNICATIONS". Una fuente secundaria da 58(3), 512–545: discrepancia aislada. |
| DOI | Sí | 10.1249/MSS.0000000000003897 (repetido en varias fichas; no resuelto en doi.org) |
| PMID / PMCID | PMID nuevo | PMID 41843416 (aparece en la URL de PubMed del buscador). PMC12965823 dado en el encargo; no abierto. |
| Tipo | Sí | Revisión de revisiones (umbrella review); posicionamiento ACSM; actualiza el de 2009. |
| Población | Sí | Adultos sanos. |
| Método | Parcial | 137 revisiones sistemáticas; más de 30.000 participantes (nota oficial y varias secundarias). El método de calificación de la evidencia **no se vio**. |
| Resultados (resumen) | Fragmentos | (a) "Evidencia insuficiente" de que el descanso entre series influya en las adaptaciones. (b) Fallo muscular momentáneo, tipo de equipamiento, complejidad del ejercicio, configuración de series, tiempo bajo tensión, restricción del flujo y periodización no afectaron de forma consistente a los resultados. (c) Recomendación general: entrenamiento de fuerza progresivo con prescripción variable. (d) Potencia: cargas moderadas y concéntrica rápida. (e) Pocos datos sobre función física. |
| Cifras concretas | **No en el texto** | ~10 series/semana por grupo (infografía oficial: "~10 sets per muscle group"); ~80 % 1RM y 2–3 series por ejercicio para fuerza; todos los grandes grupos ≥2 d/semana; no hace falta el fallo (~2–3 RIR). Todo procede de la infografía oficial, de notas de prensa o de blogs. |
| No encontrado | — | Ninguna fuente, ni secundaria, menciona un número de ejercicios por sesión ni una duración de sesión. |
| Limitaciones | — | Revisión de revisiones: hereda la calidad de las revisiones incluidas. Sin datos para Perú. |
| Aplicabilidad | — | Alta para adultos sanos; extrapolación a Lima Metropolitana sin datos locales. |

### 3.2 Pelland et al. 2026: The Resistance Training Dose Response [S02]

| Campo | Verificado | Detalle |
|---|---|---|
| Acceso real | — | **RESUMEN** (fragmentos) + FUENTE_SECUNDARIA |
| Autores | Parcial | Pelland JC, Remmert JF, Robinson ZP, Hinson SR, Zourdos MC (lista dada por el buscador). No se pudo confirmar si es completa. |
| Revista | Sí | *Sports Medicine*, 56(2), 481–505. PMID 41343037. |
| DOI | Sí | 10.1007/s40279-025-02344-w |
| Tipo | Sí | Metarregresiones bayesianas. |
| Método | Parcial | Series clasificadas como directas o indirectas; tres conteos: total (indirecta = 1), fraccional (0,5) y directo (0). |
| Resultados (resumen) | Fragmentos | Probabilidad posterior del 100 % de que la pendiente marginal del volumen sea > 0, en hipertrofia y en fuerza. El conteo fraccional ajustó mejor. Volumen y frecuencia tienen dosis-respuesta propias; la fuerza muestra rendimientos decrecientes más marcados. |
| Frecuencia | Secundaria | Positiva con rendimientos decrecientes para la fuerza; compatible con efecto despreciable en hipertrofia. Un preprint posterior que reanaliza los datos discute la interpretación. |
| No verificado | — | "67 estudios y 2.058 participantes" (solo un sitio de terceros). Las cifras de % de hipertrofia por número de series (sitio de terceros). Un supuesto punto de ~31 series fraccionales/semana (resumen de terceros). **No usar.** |
| Aplicabilidad | — | Adultos sanos, mayoría jóvenes. |

### 3.3 ACSM 2024: Physical Activity and Excess Body Weight and Adiposity for Adults [S04]

| Campo | Verificado | Detalle |
|---|---|---|
| Acceso real | — | **RESUMEN** (fragmentos) + FUENTE_SECUNDARIA |
| Autores | **Sí (nuevo)** | Jakicic JM, Apovian CM, Barr-Anderson DJ, Courcoulas AP, Donnelly JE, Ekkekakis P, Hopkins M, Lambert EV, Napolitano MA, Volpe SL (registro del repositorio de GWU; 10 autores). |
| Revista | Parcial | *Med Sci Sports Exerc*, 56(10), 2024; PMID 39277776; DOI 10.1249/MSS.0000000000003520. **Páginas no encontradas.** Publicado también en *Transl J Am Coll Sports Med*, 9(4), e000266. |
| Tipo | Sí | Declaración de consenso. |
| Resultados (resumen) | Fragmentos | (a) Los efectos de la actividad física sobre el peso son más evidentes al progresar hasta **al menos 150 min/semana de intensidad moderada**, con patrón **dosis-respuesta**. (b) El efecto de la actividad física **se suma** al de la reducción de la ingesta. (c) El HIIT no parece superior a la actividad moderada-vigorosa para regular el peso. (d) La actividad ligera puede ser una alternativa si el gasto energético es suficiente; la dosis necesaria se desconoce. (e) Ninguna modalidad es superior: se recomienda actividad **multimodal**. |
| No verificado | — | El objetivo de pérdida de ≥5–10 % del peso. Las recomendaciones específicas de fuerza. "~300 min/semana para mantener la pérdida" aparece en una revisión que lo cita, **no** en el resumen del consenso. |
| Aplicabilidad | — | Adultos con sobrepeso u obesidad; directamente pertinente para LOSE_WEIGHT. |

### 3.4 OMS 2020: WHO Guidelines on Physical Activity and Sedentary Behaviour [S05]

| Campo | Verificado | Detalle |
|---|---|---|
| Acceso real | — | **RESUMEN** (fragmentos de la guía y del artículo en BJSM vía buscador) + FUENTE_SECUNDARIA |
| Guía | Sí | OMS (2020). ISBN 978-92-4-001512-8 (versión electrónica). |
| Artículo | **Sí (nuevo)** | Bull FC y 29 coautores (30 nombres en el repositorio de Western Sydney; el propio buscador dijo "31": discrepancia de recuento). *Br J Sports Med*, 54(24), 1451–1462. DOI 10.1136/bjsports-2020-102955. PMID 33239350. |
| Recomendaciones (fragmentos) | Sí | (a) 150–300 min/semana moderada, o 75–150 vigorosa, o una combinación equivalente. (b) Más de 300 min aporta beneficios adicionales (recomendación condicional). (c) Fortalecimiento a intensidad moderada o mayor, de todos los grandes grupos musculares, 2 o más días/semana. (d) Algo de actividad es mejor que nada. (e) Se elimina el requisito de bloques de ≥10 min. (f) Limitar el sedentarismo; no se pudo cuantificar un umbral. |
| Fortalecimiento: dosis-respuesta | Fragmento | No se encontró evidencia de asociación dosis-respuesta con volúmenes mayores de fortalecimiento (resumen del artículo en PMC, vía buscador). |
| Equivalencia 1 min vigoroso ≈ 2 min moderado | **Inferencia** | Se deduce de los rangos (75 frente a 150; 150 frente a 300). La frase literal no se vio. |
| Aplicabilidad | — | Salud pública global; aplicable a adultos de Lima. Es una guía de salud, no de pérdida de peso. |

### 3.5 ACSM 2009: Progression Models in Resistance Training for Healthy Adults [S06]

| Campo | Verificado | Detalle |
|---|---|---|
| Acceso real | — | **RESUMEN** (fragmentos con versiones discrepantes) + FUENTE_SECUNDARIA |
| Cita | Sí | *Med Sci Sports Exerc*, 41(3), 687–708. DOI 10.1249/MSS.0b013e3181915670. PMID 19204579. Autoría institucional (ACSM). |
| Carga (fragmentos) | Sí | Principiantes: 8–12 RM. Intermedios y avanzados: rango 1–12 RM periodizado, con énfasis final en carga pesada (1–6 RM). |
| Descanso (fragmentos) | **Discrepante** | Versión A (resumen): carga pesada 1–6 RM "con descansos **de al menos 3 min** entre series" y velocidad moderada. Versión B (otra copia): "**3–5 min**". Versión C (texto sobre descansos): "**al menos 2–3 min** en ejercicios principales con cargas pesadas" para principiantes, intermedios y avanzados, y 1–2 min en otros ejercicios de intensidad moderada a moderadamente alta. |
| Frecuencia (fragmentos) | **Discrepante** | Versión del resumen indexado: 2–3 d/semana principiantes, 3–4 intermedios, 4–5 avanzados. Otra copia: 2–3 para principiantes e intermedios. |
| Selección | Sí | Énfasis en ejercicios multiarticulares, sobre todo de cuerpo completo. |
| Series | Fragmento | En principiantes, una serie y varias series dan ganancias similares. |
| Limitaciones | — | Consenso de 2009, superado en parte por el ACSM 2026. |

### 3.6 Otras fuentes verificadas en esta sesión (solo lo nuevo)

| ID | Qué se verificó | Acceso |
|---|---|---|
| S03 Remmert 2025 | Sigue como preprint (SportRxiv, versión 1). No se halló versión revisada por pares. Pelland 2026 lo cita como preprint. Resumen: probabilidad 100 % de pendiente > 0 por sesión; punto de superioridad no detectable (PUOS) ~2 series (fuerza) y ~11 (hipertrofia) por sesión; datos insuficientes con volúmenes muy altos por sesión. Los autores lo presentan como límite de detección, no como techo. | RESUMEN |
| S07 Singer 2024 | 11 autores (Singer, Wolf, Generoso, Arias, Delcastillo, Echevarria, Martinez, Androulakis-Korakakis, Refalo, Swinton, Schoenfeld). Vol. 6, art. 1429789. Conclusión: beneficio pequeño con >60 s; sin diferencias apreciables con >90 s; el fallo frente a no fallo no cambia esa interacción; heterogeneidad sustancial. | RESUMEN |
| S08 Grgic 2018 | *Sports Med* 48(1), 137–151. DOI 10.1007/s40279-017-0788-x (registro del repositorio de la Victoria University). Resumen: >2 min para maximizar fuerza en entrenados; 60–120 s bastan en no entrenados; ganancias robustas incluso con <60 s. | RESUMEN |
| S15 Schoenfeld 2017 | 15 estudios, 34 grupos. Dosis-respuesta graduada; tendencia por categorías p = 0,074. Datos solo hasta ~10 series/semana. DOI 10.1080/02640414.2016.1210197 (confirmación débil). | RESUMEN |
| S17 Androulakis-Korakakis | PMID 31797219; publicado en línea en 2019. Protocolo "1 serie de 6–12 repeticiones al 70–85 % 1RM, 2–3 veces/semana, 8–12 semanas" **solo en un blog**. | FUENTE_SECUNDARIA |
| S19 Iversen 2021 | Revisión **narrativa**. *Sports Med* 51(10), 2079–2095. PMID 34125411; PMC8449772. Sesiones muy cortas y frecuentes (p. ej. 15 min) pueden ser alternativa viable; importa más el volumen semanal que la frecuencia. | RESUMEN |
| S37 Helms 2018 | 10 autores (confirmación débil). Hombres entrenados de 19–35 años; 8 semanas. Sin diferencias significativas; pequeña ventaja probable del RPE en 1RM. | RESUMEN |
| S38 Halperin 2022 | Autores: Halperin, Malleron, Har-Nir, Androulakis-Korakakis, Wolf, Fisher, Steele. 12 estudios, 414 participantes. Infraestimación media de 0,95 repeticiones (IC 95 % 0,17–1,73), con heterogeneidad. Más precisión cerca del fallo, con cargas más pesadas y en series posteriores. | RESUMEN |
| S39 Nuzzo 2024 | Autores: Nuzzo, Pinto, Nosaka, Steele. *Sports Med* 54(2), 303–321. 269 estudios, 7.289 personas. Repeticiones y su DE bajan al subir el %1RM. Prensa de piernas permite más repeticiones que press de banca. Sexo, edad y entrenamiento influyen poco. **Los porcentajes de la tabla siguen sin verificar.** | RESUMEN |
| S48 Lopez 2021 | 10 autores (Lopez, Radaelli, Taaffe, Newton, Galvão, Trajano, Teodoro, Kraemer, Häkkinen, Pinto). DOI 10.1249/MSS.0000000000002585. PMID 33433148. Resultados **no** releídos. | SOLO_REFERENCIA (metadatos) en esta sesión |
| S50 Krieger 2010 | 8 estudios, 19 grupos, 55 tamaños del efecto. Varias series > una (diferencia 0,10; p = 0,016). 2–3 frente a 4–6 series: sin diferencia (p = 0,29). DOI 10.1519/JSC.0b013e3181d4d436 (línea de cita de PubMed). | RESUMEN |
| S51 Bok 2022 | *Sports Med* 52(9), 2085–2109. Resumen: el talk test y la Feeling Scale delimitan el umbral ventilatorio, pero su validez para el umbral de compensación respiratoria es **limitada**. **No se verificaron** valores de RPE por umbral. | RESUMEN |

---

## 4. Respuesta a las 10 afirmaciones

### 4.1 ¿Son defendibles 3×10 con 90 s y 3×13 (GAIN_MUSCLE)?

- **Sí, dentro de rangos defendibles.**
- Descanso: más de 60 s da un beneficio pequeño, y más de 90 s no muestra diferencias apreciables [S07, RESUMEN]. **[EVIDENCIA DIRECTA]**
- El ACSM 2026 halla evidencia insuficiente de que el descanso influya en las adaptaciones [S01, RESUMEN]. **[EVIDENCIA DIRECTA]**
- Series: varias series superan a una; 2–3 frente a 4–6 no difieren [S50, RESUMEN]. **[EVIDENCIA DIRECTA]**
- Repeticiones: hipertrofia similar en un rango amplio de cargas si las series se acercan al fallo [S48; resultados no releídos hoy]. El "30–100 % 1RM" atribuido al ACSM 2026 es de un blog. **[EVIDENCIA DIRECTA de la RCE, no reverificada]**
- Condición: depende de la proximidad al fallo, que FitSense no controla. **[INFERENCIA]**
- Los valores exactos (10 y 13) son elecciones dentro del rango. **[DECISIÓN DE INGENIERÍA]**

### 4.2 ¿Son razonables 2×8/120 s (principiante) y 3×6/150 s (intermedio/avanzado) para fuerza?

- **Sí, como esquemas.** No como garantía de intensidad.
- ACSM 2009: 8–12 RM en principiantes; énfasis final en 1–6 RM en intermedios y avanzados [S06, RESUMEN]. El 3×6 cae en 1–6 RM. El 2×8 cae en el límite inferior del rango de principiantes. **[EVIDENCIA DIRECTA]** sobre los rangos.
- 60–120 s bastan en no entrenados [S08, RESUMEN]. Los 120 s del principiante están en el rango. **[EVIDENCIA DIRECTA]**
- Dosis bajas funcionan en entrenados [S17, FUENTE_SECUNDARIA para el protocolo]. **[EVIDENCIA, certeza baja]**
- ~80 % 1RM y 2–3 series por ejercicio: solo en infografía oficial, prensa y blogs [S01]. Las 2 series del principiante están en la parte baja. **[EVIDENCIA no verificada en el texto]**
- Que 6 u 8 repeticiones sean "carga alta" depende del peso elegido [S39]. **[INFERENCIA]**

### 4.3 ¿Tienen respaldo suficiente 150 s para fuerza pesada?

- **Respaldo suficiente para B; no para A.**
- Más de 2 min en entrenados [S08, RESUMEN]. 150 s lo cumple. **[EVIDENCIA DIRECTA]**
- ACSM 2026: evidencia insuficiente de que el descanso influya [S01, RESUMEN]. **[EVIDENCIA DIRECTA]**
- ACSM 2009: hay tres redacciones. "Al menos 2–3 min" en ejercicios principales pesados admite 150 s. "Al menos 3 min" y "3–5 min" con 1–6 RM no lo admiten [S06, RESUMEN discrepante]. **[EVIDENCIA DIRECTA, contradictoria entre versiones]**
- El ACSM 2026 sustituye al de 2009 y no exige un descanso concreto. Pesa más. **[INFERENCIA]**
- Permitir 180 s en básicos pesados sigue siendo prudente. Ya cabe en el techo de T5. **[INFERENCIA]**

### 4.4 Frecuencia semanal y distribución por patrones

- Fortalecer todos los grandes grupos ≥2 d/semana, a intensidad moderada o mayor [S05, RESUMEN]. **[EVIDENCIA DIRECTA]**
- ACSM 2026: grandes grupos ≥2 veces/semana [S01, FUENTE_SECUNDARIA oficial: infografía y notas del ACSM]. **[EVIDENCIA no verificada en el texto]**
- Más frecuencia mejora la fuerza con rendimientos decrecientes; en hipertrofia, efecto compatible con despreciable [S02, RESUMEN/FUENTE_SECUNDARIA]. **[EVIDENCIA DIRECTA, interpretación discutida]**
- ACSM 2009: énfasis en multiarticulares, sobre todo de cuerpo completo [S06, RESUMEN]. **[EVIDENCIA DIRECTA]** (consenso)
- La taxonomía de 4 patrones no tiene evidencia directa. Es una forma razonable de cumplir "todos los grandes grupos". **[INFERENCIA]**

### 4.5 ¿Existe un máximo científico de ejercicios por sesión?

- **No se encontró ninguno.** Ni en el ACSM 2026 (ni siquiera en sus resúmenes secundarios), ni en el ACSM 2009, ni en el resto de fuentes. **[EVIDENCIA DIRECTA de ausencia en lo buscado]**
- Lo más próximo es el preprint S03. Habla de **series por músculo y sesión**, no de ejercicios. Sus autores lo presentan como límite de detección, no como techo. **[EVIDENCIA, certeza muy baja]**
- Los topes de 6/7/8 ejercicios (V24) son **[DECISIÓN DE INGENIERÍA]**.

### 4.6 Volumen mínimo por sesión y por semana

- **¿≥10 series/semana es cifra del texto del ACSM 2026?** No verificado. La **infografía oficial del ACSM** dice "~10 sets per muscle group" (aproximadamente 10), no "≥10". Varias notas de prensa y blogs dicen "≥10" o "10+". **[FUENTE_SECUNDARIA oficial]**
- Dosis-respuesta positiva del volumen semanal, con rendimientos decrecientes, más marcados para la fuerza [S02, RESUMEN]. **[EVIDENCIA DIRECTA]**
- Dosis-respuesta graduada; por categorías solo tendencia (p = 0,074); datos solo hasta ~10 series/semana [S15, RESUMEN]. **[EVIDENCIA DIRECTA]**
- ≥4 series/semana por grupo para quien tiene poco tiempo [S19; cifra vista en fuente secundaria]. **[EVIDENCIA, certeza baja]**
- OMS: no hay evidencia de dosis-respuesta con volúmenes mayores de fortalecimiento (en el marco de salud general) [S05, RESUMEN]. **[EVIDENCIA DIRECTA]**
- Por sesión: no hay mínimo establecido. Solo el preprint S03 da un techo orientativo. **[EVIDENCIA, certeza muy baja]**

### 4.7 ¿Qué aporta la actividad aeróbica al control del peso? (ACSM 2024)

- Los efectos son más evidentes al llegar a ≥150 min/semana moderados, con patrón dosis-respuesta [S04, RESUMEN]. **[EVIDENCIA DIRECTA]**
- Su efecto **se suma** al de reducir la ingesta. No lo sustituye [S04, RESUMEN]. **[EVIDENCIA DIRECTA]**
- El HIIT no es superior a la actividad moderada-vigorosa; la ligera puede servir con gasto suficiente; se recomienda actividad multimodal [S04, RESUMEN]. **[EVIDENCIA DIRECTA]**
- Papel "modesto" en la pérdida y el mantenimiento del peso: solo en fuente secundaria. **[FUENTE_SECUNDARIA]**

### 4.8 ¿Qué dosis aeróbicas se asocian con beneficios?

- OMS: 150–300 min/semana moderada, 75–150 vigorosa o combinación equivalente; más de 300 min aporta beneficios adicionales (condicional) [S05, RESUMEN]. **[EVIDENCIA DIRECTA]**
- "Algo es mejor que nada" y no se exigen bloques de ≥10 min: toda la actividad cuenta [S05, RESUMEN]. **[EVIDENCIA DIRECTA]**
- Equivalencia 1 min vigoroso ≈ 2 min moderados: se deduce de los rangos; la frase literal no se vio. **[INFERENCIA]**
- Para el peso: ≥150 min/semana moderados, con dosis-respuesta [S04, RESUMEN]. **[EVIDENCIA DIRECTA]**
- Consecuencia para LW2: 18–36 min/semana de trabajo queda por debajo de esas dosis **si es la única actividad aeróbica**. Si la persona hace actividad fuera de la app, esa actividad también cuenta. **[INFERENCIA]**

### 4.9 ¿Qué significa prescribir intensidad sin conocer el RIR ni la carga relativa?

- La relación entre repeticiones y %1RM varía por ejercicio y entre personas; sexo, edad y entrenamiento influyen poco [S39, RESUMEN]. **[EVIDENCIA DIRECTA]**
- Las personas infraestiman las repeticiones restantes en ~1 repetición; aciertan más cerca del fallo y con cargas pesadas [S38, RESUMEN]. **[EVIDENCIA DIRECTA]**
- La carga por RPE rinde al menos como la carga por %1RM en hombres entrenados [S37, RESUMEN]. **[EVIDENCIA DIRECTA]**
- Sin RIR ni carga relativa, "3×6" describe la **dosis** (series × repeticiones), no la **intensidad**. **[INFERENCIA]**
- Cardio: el talk test sirve para el umbral ventilatorio; su validez para el umbral de compensación respiratoria es limitada [S51, RESUMEN]. Sirve como ancla de "moderado frente a intenso", con precisión limitada en la zona alta. **[EVIDENCIA DIRECTA + INFERENCIA]**

### 4.10 ¿Qué parte de V20 y V24 es decisión de ingeniería?

- **V20 (duración mínima del 70 %): entera.** No se encontró evidencia de una duración mínima de sesión ligada a resultados. **[DECISIÓN DE INGENIERÍA]**
- Sesiones muy cortas y frecuentes (p. ej. 15 min) pueden ser una alternativa viable; importa más el volumen semanal [S19, RESUMEN; revisión narrativa]. **[EVIDENCIA DIRECTA, certeza baja]**
- Un metaanálisis de 2025 sobre superseries (no incluido en el CSV; ficha no verificada) indica que reducen la duración sin comprometer el volumen. Es evidencia indirecta de que la duración no es la variable activa. **[FUENTE_SECUNDARIA, no catalogada]**
- **V24 (tope de 6/7/8 ejercicios): entero.** No hay máximo científico (§4.5). **[DECISIÓN DE INGENIERÍA]**
- La única parte con apoyo indirecto: cubrir todos los grandes grupos ≥2 d/semana [S05] exige un número mínimo de ejercicios por semana, no por sesión. **[INFERENCIA]**

---

## 5. Clases de las reglas

- **Ninguna clase cambia.** Ver `TRAZABILIDAD_CIENTIFICA_ACTUALIZADA.csv`.
- **Motivo:** no se obtuvo texto completo. La evidencia nueva son fragmentos del resumen y metadatos. Refuerzan o matizan las clases, pero no justifican subirlas ni bajarlas.
- Reglas **reforzadas** sin cambio de clase:
  - LW2 (E): el resumen del ACSM 2024 confirma ≥150 min/semana con dosis-respuesta.
  - IS5 y GF6 (B): el resumen de la OMS confirma "todos los grandes grupos ≥2 d/semana".
  - GM1 (B): conclusión de Singer 2024 verificada en el resumen.
  - IS7 (D): Halperin 2022 verificado (error ~1 repetición).
- Reglas **matizadas** sin cambio de clase: IS2 y T5 (ACSM 2009 discrepante), LW1 (talk test con validez limitada en la zona alta), T15 (cifra "~10" en lugar de "≥10").

---

## 6. Afirmaciones de la RCE que deben corregirse o matizarse

| # | Dónde (RCE) | Afirmación anterior | Corrección o matiz |
|---|---|---|---|
| 1 | §1.4; §2.2 Descansos; §3.3; IS2; T5; §4.6-2 | "3–5 min del ACSM 2009 para cargas pesadas" | El ACSM 2009 tiene tres redacciones: "≥2–3 min" en ejercicios principales pesados, "≥3 min" y "3–5 min" con 1–6 RM. 150 s entra en la primera y no en las otras dos. Citar el rango como discrepante. |
| 2 | §2.1 Volumen; §3.1; T15 | "≥10 series/semana por grupo (ACSM 2026, fuente secundaria)" | La fuente oficial más cercana (infografía del ACSM) dice "~10" ("aproximadamente 10"). No equivale a un mínimo. Sigue sin verificarse en el texto. |
| 3 | §2.3 Intensidad; LW1 | Talk test "etapa negativa y RPE 13–15 para el umbral de compensación respiratoria" | El resumen de Bok 2022 dice que la validez del talk test para ese umbral es **limitada**. Los valores de RPE por umbral no se verificaron. Usar el talk test como ancla de "moderado frente a intenso", no como marcador preciso de HIIT. |
| 4 | §3.4 | ACSM 2009: "2–3 / 3–4 / 4–5 d/semana" | Hay otra versión: 2–3 d para principiantes **e** intermedios. Marcar como discrepante. |
| 5 | §2.3 Dosis aeróbica; LW2 | Equivalencia OMS "1 min vigoroso ≈ 2 min moderado" | Es una **inferencia** de los rangos; la frase literal no se vio. |
| 6 | §6; S02 | "67 estudios y 2.058 participantes" | Sigue sin verificar (un solo sitio de terceros). No citar. |
| 7 | §3.1; §4.6-6; T16 | Preprint S03 "techo por sesión" | Correcto que es preprint (sigue así a 2026-11-01). Añadir: los autores lo presentan como límite de detección y advierten de pocos datos con volúmenes muy altos. Refuerza "no es norma". |
| 8 | §2.2 Dosis mínima; S17 | Protocolo mínimo de Androulakis-Korakakis | El protocolo (1 serie, 6–12 rep., 70–85 % 1RM, 2–3 veces/semana) solo aparece en un blog. Publicado en línea en 2019 (PMID 31797219). |
| 9 | §2.3 Dosis aeróbica; LW2 | "Probablemente ineficaz como componente principal" | Mantener, pero precisar: la OMS cuenta **toda** la actividad. LW2 es E **si el cardio de la app es la única actividad aeróbica**. Con actividad registrada fuera de la app, la brecha puede ser menor. |
| 10 | §3.7; T1 | "Sesiones cortas eficaces [S19, S17]" | S19 es una revisión **narrativa**, no sistemática. Mantener la certeza en "baja". |
| 11 | §0 / encabezado | "51 RESUMEN, 2 SOLO_REFERENCIA" | Sigue siendo cierto. En esta sesión tampoco hubo TEXTO_COMPLETO. |

---

## 7. Límites

- **Ningún texto completo.** Todos los hosts de texto completo y de metadatos dieron ENOTFOUND con WebFetch.
- **Los fragmentos pasan por el buscador.** WebSearch resume las páginas con un modelo. Una frase citada puede estar recortada o mal atribuida. Por eso nada se eleva a TEXTO_COMPLETO.
- **Posible eco de la consulta.** Algunas confirmaciones (DOI de S15, autores de S37 y S39) pueden repetir datos que iban en la consulta. Están marcadas como "confirmación débil".
- **Sin métodos de calificación.** No se vio cómo gradúa la evidencia el ACSM 2026.
- **Sin datos de Perú.** La aplicabilidad a Lima Metropolitana es una extrapolación desde adultos sanos en general.
- **Sin revisión humana.**
