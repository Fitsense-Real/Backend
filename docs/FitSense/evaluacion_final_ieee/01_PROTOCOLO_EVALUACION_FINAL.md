# Protocolo de la evaluación final de FitSense para el short paper IEEE

**Estado: PROPUESTA PARA REVISIÓN.** No se ha ejecutado nada ni se ha llamado a Replicate.

Al aprobarse, este archivo, `02_CASOS_INDEPENDIENTES_PROPUESTOS.csv` y `03_CRITERIOS_Y_METRICAS.md` se congelan en un commit **anterior** a cualquier ejecución. Los cambios posteriores irán en anexos fechados y con su motivo; nunca se edita lo congelado.

No se modifica el short paper. Las discrepancias D1–D13 del experimento anterior (`short_paper_ieee/experimento_final_20_usuarios/23`) se tratan en el §8.

## 1. Pregunta y alcance

**Pregunta:** ¿FitSense mide la adherencia, decide y adapta el plan semanal **correctamente y sin violar sus restricciones de seguridad**? ¿Qué parte de las adaptaciones aplicadas se debe a la IA y qué parte a los mecanismos deterministas (respaldo)?

**Alcance:**
- Afirmaciones **técnicas** sobre usuarios sintéticos:
  - validez de los planes;
  - corrección de métricas y decisiones;
  - corrección y origen de las adaptaciones;
  - seguridad;
  - coste y latencia de la IA.
- **No** se afirman efectos de adherencia, conductuales ni clínicos.

## 2. Versión candidata (decisión 1, pendiente)

| | VC-A: `generation` tal cual | VC-B: `generation` + #38 + #39 + corrección P3 |
|---|---|---|
| Código | `76e15c5` (incluye #36 H1 y #37 textos) | Rama de evaluación `eval/final-ieee`:<br>• `generation` con #38 y #39 incorporados por merge;<br>• la corrección de una línea de P3 (`DescripcionAdaptacion`), ya probada en local y no publicada |
| H1 (V8C multiconjunto) | sí (por defecto) | sí |
| Límite de dificultad | no existe | `difficulty-guard=preferencia` (C) |
| Retirada localizada | no existe | `pain-localized-removal=on` (solo con el límite) |
| Prompt de adaptación | V1 (V2 no está en `generation`) | V1 |
| Progresión | apagada | apagada |
| Política | PLAN-HIST-1 | PLAN-HIST-1 |
| Lo que el artículo podría afirmar | estado actual integrado | ningún sustituto más difícil ante dolor o «demasiado difícil»; salida intermedia ante dolor; textos coherentes |
| Riesgos conocidos | SUST-SEG-1 puede subir la dificultad (N08; 2.469 exposiciones simuladas). El texto P3 dice «no aplicamos cambios» tras una sustitución de respaldo | Semanas sin plan en 5 de 21 casos sin sustituto del 135 (V20). Requiere tu aprobación de #38, #39 y P3 **solo para la rama de evaluación**; su fusión en `generation` sigue siendo otra decisión |

**Recomendación: VC-B.**
- Es la versión que mejor representa el sistema que se quiere describir: seguro ante dolor y ante «demasiado difícil», y con mensajes coherentes.
- Sus diferencias con VC-A ya están medidas: #38 y #39 son estables con 960 perfiles, REPLAY, 22 perfiles y regresión.
- Si prefieres no incluir código en borrador, **VC-A** es válida, siempre que el artículo declare sus dos limitaciones conocidas.

**Prompt V2** (`experiment/adaptation-prompt-v2`): **fuera** de la versión candidata. Su resultado fue PARCIAL, con n = 8 × 2. Solo entraría como condición comparativa si lo decides expresamente (decisión 5).

**Congelación de la versión** (al aprobar), en `04_CONFIGURACION_CONGELADA.txt`:
- SHA exacto;
- `application.properties` efectivas;
- lista de migraciones (46) y hash del catálogo (`exercises`);
- modelo de IA y parámetros.

**El arnés de evaluación** (`ExperimentoKora20IT`) es código **solo de prueba**, de `lab/experimento-kora-20usuarios`. Se trae a la rama de evaluación con una condición comprobable: `git diff --stat <SHA candidato>..<SHA evaluación> -- src/main` debe estar **vacío**.

## 3. Escenarios

| Bloque | Casos | Origen | Semanas | Qué cubre |
|---|---:|---|---:|---|
| **Núcleo (U)** | 20 | `experimento_final_20_usuarios/03_PERFILES_U01_U20.csv`, **sin cambios** | 1, más la siguiente publicada | 4 objetivos × 5 patrones de ejecución (E1 completo, E2 parcial P40, E3 fatiga, E4 dificultad + desmotivación, E5 sin registro) |
| **Independientes (I)** | 16 | `02_CASOS_INDEPENDIENTES_PROPUESTOS.csv`, generado por `herramientas/generar_casos_independientes.py` con una regla fija, **sin mirar resultados** | 2 | Las causas que el núcleo no tiene:<br>• dolor identificado en un ejercicio;<br>• dolor de sesión;<br>• falta de tiempo;<br>• cambio de horario.<br>Con perfiles variados:<br>• 4 objetivos;<br>• 3 niveles;<br>• gimnasio, casa con mancuernas y bandas, y casa con peso corporal;<br>• 3–6 días;<br>• 30–75 min |
| **Control de regresión** | 960 + REPLAY | `ComparacionPerfilesIT`, `CampanaP123Cohorte02IT` | — | La generación inicial no cambia (hashes) y la REPLAY solo cambia lo explicado |

**Diseño de los independientes:**
- 16 = 4 objetivos × 4 causas, una vez cada combinación.
- Objetivo y causa quedan **cruzados completamente** con:
  - equipamiento (12/12 combinaciones);
  - nivel (12/12);
  - días (16/16);
  - minutos (16/16).
- Equipamiento × nivel también es completo (9/9).
- Hay cruces menores incompletos, por ejemplo días × minutos (8/16). Con 16 casos no se puede cruzar todo con todo.
- Ningún perfil se descarta por ser no factible: se conserva como resultado, como R0548.

**Semillas:**
- semilla del motor de reglas: `userId × 1.000.003 + epochDay(semana)`;
- base vacía y usuarios creados en orden: U01–U20 en una corrida, I01–I16 en otra;
- semana medida: 2026-11-02; los `userId` reales se registran.

**Patrones de ejecución:** H, P40, O y N, como en el protocolo anterior. Para los independientes se añade:
- **D:** dolor identificado. El ejercicio en la **posición 1** de S2 se registra con `PAIN_OR_DISCOMFORT` y 0 hecho; el resto, completo.
- **O-PAIN:** S2 omitida con `PAIN_OR_DISCOMFORT`.
- **O-TIME:** S2 omitida con `LACK_OF_TIME`.
- **O-SCHED:** S2 omitida con `SCHEDULE_CHANGE`.

La semana 2 se ejecuta entera (continuidad).

**Trabajo preparatorio, solo de prueba:** el arnés actual solo admite 3 sesiones y los patrones H, P40, O y N. Hay que añadir:
- un número de sesiones variable (3–6);
- el patrón D;
- los motivos de omisión anteriores.

Se hará en la rama de evaluación, sin tocar `src/main`, con una prueba que verifique el guion sobre un caso conocido.

## 4. Condiciones

| Condición | IA | Qué aísla | Coste |
|---|---|---|---|
| **D** (determinista) | `fitsense.ai.enabled=false`, beans reales | Núcleo determinista: métricas, decisiones, respaldo, seguridad. Oráculo de referencia | 0 |
| **D′** (repetición de D) | igual | Reproducibilidad: debe ser **idéntica** byte a byte, con los mismos hashes | 0 |
| **R** (Replicate real) | `gpt-5-mini` sobre `openai/gpt-5-structured`, esfuerzo `medium`, prompt V1, 3 intentos, `prefer-wait=25` | Contribución de la IA, con los mismos casos, semillas y registros | ver §6 |
| (opcional) **R′** | igual que R | Variabilidad de la IA (decisión 4) | ver §6 |

**Reglas para R:**
- la adaptación parte del **mismo plan inicial** que en D, generado por reglas;
- así, las diferencias se deben a la IA de adaptación y no a otra generación inicial.

La generación inicial con IA (los «gemelos» del experimento anterior) es opcional (decisión 6). Se mide aparte y nunca se mezcla con la adaptación.

## 5. Procedimiento

1. **Congelar:**
   - commit con este protocolo, `02`, `03` y `04_CONFIGURACION_CONGELADA.txt`;
   - SHA de la rama de evaluación;
   - CI en verde sobre ese SHA.
2. **D:**
   - núcleo e independientes;
   - escribe en `evidencia/D`.
3. **D′:**
   - igual, en `evidencia/D_repeticion`;
   - se comparan los hashes.
4. **Oráculo independiente:**
   - script en Python que recalcula, **desde las filas de la base y sin funciones del backend**:
     - adherencia;
     - decisión esperada;
     - sesiones señaladas;
     - invariantes de seguridad;
     - clasificación de cada sesión (`03`).
5. **R:**
   - solo con tu aprobación del presupuesto;
   - comprobación previa: `GET /v1/account` = 200, base local y límites cargados.
6. **Informe:**
   - resultados por caso, por origen y por condición;
   - discrepancias con el artículo.

**Reglas:**
- ninguna repetición selectiva;
- una corrida que falle por causas técnicas se conserva y se repite **completa** una sola vez, registrada;
- ningún perfil, guion, prompt ni validador cambia tras ver resultados;
- toda predicción iniciada cuenta para el límite;
- al llegar al límite, no hay más llamadas;
- carpetas nuevas: el arnés se niega a escribir en una carpeta con contenido.

**Paro:** si D incumple algún criterio duro del `03` (A1–A5):
- se detiene la evaluación;
- se informa la causa raíz;
- no se pasa a R.

Una corrección exige una nueva congelación y repetir **todo** desde D.

## 6. Presupuesto de R (decisión 3)

**Operaciones elegibles para IA (previstas, sin ejecutar):**
- **núcleo:** 8 (E3 y E4);
- **independientes:**
  - en la semana 1, hasta 16 (las cuatro causas señalan S2);
  - en la semana 2, las que salgan de la semana 1 (sin omisiones, en principio 0);
- **total:** ~24 operaciones; con 3 intentos, **72 predicciones** en el peor caso.

Con el coste observado, entre US$ 0,02 y 0,05 por predicción, eso son US$ 1,5–3,6.

| Propuesta | Valor |
|---|---|
| Por corrida R | **80 predicciones / USD 4** (supuesto conservador de 0,05 por predicción) |
| Con R′ | 160 predicciones / USD 8 |
| Gemelos de generación inicial (opcional) | +22–40 predicciones |

## 7. Entregables de la ejecución

Se preparan **después** de aprobar y ejecutar:
1. configuración congelada;
2. resultados por caso (D, D′, R), en CSV;
3. oráculo e informe de coincidencias;
4. clasificación por sesión y por origen;
5. seguridad;
6. IA: propuestas, rechazos, reintentos, latencia y coste;
7. reproducibilidad;
8. discrepancias con el artículo;
9. informe final con las cifras que el artículo puede usar y sus límites.

## 8. Relación con el artículo

Cada afirmación que el artículo haga debe tener una métrica del `03` que la respalde. Las discrepancias D1–D13 del experimento anterior deben citarse o corregirse en el texto, por ejemplo:
- la decisión usa Aobs, no Aw;
- «adaptación decidida» no es «adaptación aplicada»;
- los respaldos no cuentan como adaptaciones de la IA.

**No se modifica el artículo;** se entregan las cifras y las frases sugeridas.

## 9. Decisiones que necesito antes de congelar

1. **Versión candidata:** VC-A o **VC-B (recomendada)**. Si eliges B: autorización para crear la rama de evaluación con #38, #39 y la corrección P3 (sin fusionar en `generation`).
2. **Casos independientes:** aprobar la lista I01–I16 tal cual o la regla de generación. No se ajustan después.
3. **Presupuesto de R:** 80 predicciones / USD 4, o el que indiques.
4. **¿R′ (segunda corrida con IA)?** Sí o no. Recomiendo **sí** si el artículo va a dar tasas de la IA: con una sola corrida no hay medida de variabilidad.
5. **¿Prompt V2 como condición comparativa?** Recomiendo **no**: está fuera de la versión candidata.
6. **¿Generación inicial con IA (gemelos)?** Recomiendo **sí, como bloque separado**, porque el artículo describe la generación con IA.
7. **Criterios de aceptación del `03`:** aprobarlos o ajustarlos **antes** de ejecutar.
