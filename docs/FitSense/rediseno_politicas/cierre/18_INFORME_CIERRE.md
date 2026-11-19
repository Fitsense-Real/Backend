# 18 · Informe de cierre técnico de PLAN-REV-2.0

**Datos de partida:**
- **Rama:** `investigacion/rediseno-politicas`.
- **Antes:** `d4556b1`.
- **Código final:** `cc76bd1`.
- **Estado:** sin merge, sin despliegue, sin Replicate y sin Neon. El short paper, el PR #41 y la evidencia R/R′ no se tocan.
- **Política por defecto:** `PLAN-HIST-1`, idéntica (barrido byte a byte y huellas K20 congeladas). REV2 solo se activa con `fitsense.planning.policy=PLAN-REV-2.0` y `allow-candidates=true`.

**Estados:** COMPROBADO · CORREGIDO · NO CUMPLE · PENDIENTE DE VALIDACIÓN · NO EVALUADO.

## 1. Los 14 asuntos

| # | Asunto | Estado | Resumen y evidencia |
|---|---|---|---|
| 1 | Contrato de disponibilidad y frecuencia | **CORREGIDO / COMPROBADO** | Sesiones = `min(días pedidos, tope del nivel, días disponibles)`; topes 4/6/6 sin cambio. Fuente única `FrecuenciaSemanal` (antes estaba triplicada). Aviso `FREQUENCY/S_MAX` en la API. 216/216 casos con HIST y con REV2. Adherencia sobre sesiones programadas. `10` |
| 2 | L05 con cada oráculo | **COMPROBADO** | Histórico y REV2: FAIL (se conserva). REV2 + DIAS-1: PASS, como **reevaluación** con el contrato corregido. Su escenario de adaptación no llegó a ejercitarse. `10` §3 |
| 3 | Los 4 perfiles de fuerza de 30 min | **CORREGIDO** (comunicación) · **PENDIENTE** (decisión deportiva) | Limitación legítima: los 4 patrones con la dosis de fuerza suman 1.896 s y el máximo es 1.800 s. Se informa antes de generar (`FUERZA_SIN_TIEMPO`, mínimo de 32 min o más días). La incoherencia de V25 («4 si M ≥ 30») está demostrada y **no** se corrige sin aprobación (P12). `11` |
| 4 | Las 4 pruebas de progresión con REV2 | **COMPROBADO** | 3 son premisas históricas; 1 destapó un defecto real (asunto 5). Hay variantes REV2 para todas y las históricas quedan intactas. Los 10 puntos multisemana están cubiertos. `13` |
| 5 | Ejercicios desactivados | **CORREGIDO** (REV2) · **PENDIENTE** (HIST y sin sustituto) | REV2 sustituye solo la colocación afectada por un equivalente estricto (`SUST-CAT-1`, V8C lo vuelve a comprobar). Sin equivalente, no se publica nada inactivo y el motivo empieza por «CATALOGO». HIST no cambia. `13` |
| 6 | Calidad de las sesiones cortas | **COMPROBADO** (volumen) · **NO CUMPLE** (etiqueta) · **PENDIENTE** (IS principiante, J03, L14) | En el 96 % o más: 3–5 ejercicios y 6–12 series. La etiqueta `CORTA_RAZONABLE` no recoge todo lo que pide `02` §2 (P14). La dosis limitada ya se avisa al usuario. `12` §3 |
| 7 | Clasificación de DOSIS_LIMITADA | **CORREGIDO** (artefacto y visibilidad) · **COMPROBADO** (causas) | 2.385 → 1.898 sin el artefacto de brazos en FULL_BODY. Causas: catálogo, días, tiempo y reglas. Aviso `DOSE` en la API. `12` §1–2 |
| 8 | Ejercicios y catálogo | **NO CUMPLE** (varias etiquetas, traducciones e instrucciones) · **PENDIENTE DE VALIDACIÓN** | 775, 1261, 1308, 973, 1320, 1263, 853, 1093, 1262, 1115 y 580; curl femoral y extensión acreditan el patrón de rodilla (previo a REV2). **Nada modificado:** requiere revisión visual y profesional. `12` §4 |
| 9 | Las 9 adaptaciones antes bloqueadas | **COMPROBADO** | Se adaptan con REV2 (mantenimiento P-A1 o una sustitución P-A2), todas pertinentes; con HIST siguen bloqueadas. `14` |
| 10 | Transiciones entre políticas | **CORREGIDO** | Defecto real: un cambio de configuración dejaba sin plan a usuarios en curso. Ahora la semana nueva sigue la política del linaje del usuario y la configuración solo decide la de usuarios nuevos; no hay cambios automáticos. Ya no es posible MANTENIMIENTO → HIST sin migración explícita (L9 desaparece). `13` |
| 11 | Regresión histórica | **COMPROBADO** | Barrido HIST byte a byte; K20 HIST con las huellas congeladas; prompt histórico idéntico (18 huellas). `17` |
| 12 | Cohorte M | **COMPROBADO** con un hallazgo | 1.ª ejecución (`b6a4718`): REV2 + DIAS-1 da 29/30, D = D′. El fallo es M29 (sin plan, el resultado esperado), pero por la fachada llegaba el fallo técnico de V25. Corregido en `cc76bd1`. **M deja de ser no vista.** `17` §3 |
| 13 | Limitaciones pendientes | Ver §2 | — |
| 14 | Recomendación para la IA real | Ver §3–4 | — |

**Otros defectos corregidos en el cierre:**
- La factibilidad y el prompt ofrecían sustitutos que V8C rechaza: 7, 38, 45 y 30 en U, J, K y L, y ahora 0 (`14`).
- REV2 perdía la cadera en LOSE_WEIGHT: 448 → 117 planes (`12`).
- La palabra «cuerda» marcaba impacto por error (`12`).

## 2. Limitaciones y decisiones pendientes (no se han tocado)

| # | Tipo | Asunto |
|---|---|---|
| P2 | PRO | Todas las cifras de dosis, techos, mínimos, aeróbico e impacto ([HP][PRO]) |
| P3 | PRO / decisión | Ritmo de recuperación tras la fatiga: un paso cada 2 semanas, frente a los 2 pasos que describe la evidencia |
| P5 | Decisión | Reincorporación de varios retirados |
| P6, P15 | Catálogo + PRO | Reclasificar 775, 1261, 1308 y 973. Decidir si un aislado (curl femoral o extensión) puede cubrir el patrón de rodilla: hoy son 194 planes REV2 y 208 HIST. Revisar traducciones (1263, 853, 580) e instrucciones (1320). Tracción y hombro en casa sin material |
| P10 | PRO | Escala de impacto por nombre (se quitó «cuerda»; quedan 1093 y otros) |
| P11 | Diseño | `chooseDates` no separa al máximo los días; la recuperación la garantiza la alternancia de enfoques. Cambiarlo altera HIST |
| P12 | Deportiva | Alinear el cálculo de V25 con el V4 de REV2: haría válidos los 4 perfiles de fuerza de 30 min con 3 patrones |
| P13 | Dosis | LOSE_WEIGHT de 1 día: hoy un solo ejercicio de pierna y 63 planes sin series de pierna, todos avisados como DOSIS_LIMITADA |
| P14 | Clasificación | Ampliar o renombrar `CORTA_RAZONABLE` |
| P16 | Decisión | Migración explícita de un usuario de una política a otra (propuesta en `13`) |
| P17 | **Seguridad / decisión** | Si se vuelve a `allow-candidates=false`, los usuarios con linaje REV2 **siguen** con REV2 (es lo que evita dejarlos sin plan). ¿Bloqueo explícito o continuidad? |
| P18 | **Confirmación** | `PoliticaV4CCicloIT.planesHistoricosIntactos`: la expectativa se cambió para que un plan anterior a V44 siga en HIST, en lugar de pasar a la política configurada (que era un cambio automático). Necesita tu confirmación |
| P19 | App móvil | La app envía `daysPerWeek = días marcados` y no consume `/plan/limitations` (FREQUENCY, TIME, DOSE, SLOT). Cambios en `10` §4. Repositorio fuera de esta sesión |
| P20 | Protocolo | El oráculo K20 no valida por sí mismo la dosis de mantenimiento; se apoya en V24 y V8C |

## 3. Puerta para Replicate

| Condición de la orden | Estado |
|---|---|
| Sin fallos críticos de publicación | ✅ Nada inválido publicado; 0 ejercicios inactivos; intentos inválidos rechazados al 100 % |
| Sin contradicciones de validadores | ✅ Prompt, factibilidad y V8C con el mismo criterio (0 discrepancias); V25/V4 documentado como P12, sin contradicción en lo publicado |
| Sin problemas de linaje sin resolver | ✅ P-P2, H6 y H7 corregidos; transiciones por linaje; reparación de catálogo trazada |
| Sin regresiones de generación inicial | ✅ HIST idéntica; REV2: 3.776/3.780, mismas garantías y mejor composición |
| Sin inconsistencias importantes entre política y oráculo | ✅ Oráculo versionado con contrato de días; cada FAIL restante está explicado (M29 esperado) |

**Resultado:** la puerta **técnica** queda satisfecha para REV2 como candidata. Las limitaciones de §2 están declaradas y ninguna es un fallo crítico. Quedan pendientes de tu decisión antes de una evaluación real P17, P18 y, si se quiere medir con catálogo corregido, P15.

## 4. Protocolo propuesto (NO ejecutado: requiere tu autorización)

| Elemento | Propuesta |
|---|---|
| Política | `PLAN-REV-2.0` (candidata), con `allow-candidates=true` solo en el arnés |
| Commit | `cc76bd1` (código) más el commit de este informe; árbol `src` y hashes en el `00_configuracion.txt` de cada corrida |
| Modelo | Replicate `openai/gpt-5-structured`, versión `9f4cd9ec…` (la de R/R′), `gpt-5-mini`, razonamiento `medium` |
| Prompt | `FIX-PR-2.1-EXP` (V2.1) con los permisos de REV2 (`PermisosIntervencion`). Construcción verificada con IA simulada en M y U; coherencia 100 % |
| Cohortes | **U** (comparabilidad con R/R′; 8 ciclos con IA) y **N**, una cohorte **nueva** generada, publicada y con su hash **antes** de autorizar (como M; unos 20 ciclos con IA). J y K, opcionales como regresión |
| Corridas | R y R′, 2 repeticiones independientes, más D/D′ sin IA como control |
| Predicciones previstas | Como máximo 3 por ciclo: U 24 + N 60 = **84 por corrida, 168 en total** (opción A). Con J y K: +96 por corrida, **360 en total** (opción B) |
| Presupuesto máximo | Reserva de 0,05 USD por predicción: **A: USD 8,40** · **B: USD 18,00**. Gasto esperado según R/R′ (0,013 USD por predicción): unos USD 2–5 |
| Control de gasto | P-H1: reserva, bloqueo por timeout o estado desconocido, `cancel`, tope de predicciones. Tarifa 0,25 / 2,00 USD por millón de tokens |
| Criterios fijados de antemano | Oráculo REV2 + DIAS-1 (contrato vigente), más el histórico como referencia. Pertinencia por script: patrón, grupo, dificultad e impacto. Publicación de respaldo si el proveedor falla. Sin repeticiones selectivas |
| Incidencias | Fallo del proveedor → respaldo y anotación. Predicción sin métricas → reserva (P-H1). Timeout o estado desconocido → bloqueo |
| Comparación | R frente a R′: huellas y decisiones por usuario. REV2 frente a R/R′ históricos solo en U, sabiendo que la política cambió (V20, dosis) |
| Limitaciones metodológicas | U, J, K, L y M ya no son no vistas; solo N lo sería. La IA simulada no mide la calidad del modelo. Las cifras de dosis son [PRO]. Las respuestas reales pueden variar entre corridas |

**Me detengo aquí.** No se ha hecho ninguna llamada pagada, no hay ningún token de Replicate en uso y no se ha generado la cohorte N, que debe fijarse cuando decidas la autorización.
