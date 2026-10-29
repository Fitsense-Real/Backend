# Corrección del respaldo para 5 y 6 días

Rama `respaldo-5-6-dias`, a partir de `generation`. Es la recomendación 1 de `EVALUACION_GENERACION_4_6_DIAS.md`, con la opción A: corregir el generador, no limitar las sesiones.

Se hizo en dos fases:
1. **Fase 1 (§1–§3):** corrección del generador.
2. **Fase 2 (§4):** V15 cuenta como disponibles solo los grupos prescribibles. La autorizó el responsable (opción **a**) después de ver la fase 1.

**Lo que no se tocó en la fase 1:**
- V15, V20 y V22;
- el catálogo, la política de frecuencia y las migraciones;
- la IA, la adaptación y la progresión;
- Neon. Tampoco hubo llamadas a Replicate.

## Qué fallaba y qué se cambió

En `CapabilityWeekBuilder`, el generador por capacidades de INCREASE_STRENGTH, LOSE_WEIGHT y GENERAL_FITNESS, había dos defectos que solo aparecen en las divisiones de 5–6 días (empuje / tirón / piernas):

1. **Sesiones con un solo grupo** (V15 exige 2 cuando el conjunto elegible los tiene). La selección por patrón de movimiento llenaba las 2–3 colocaciones con el mismo grupo:
   - tirón = remo + remo + bisagra, todo `back`;
   - piernas = sentadilla + bisagra, todo `upper legs`.

   **Corrección:** un paso nuevo, `asegurarGrupos`, que **solo actúa si la sesión incumple** ese mínimo. Cambia una colocación de un grupo repetido por un ejercicio del grupo que falta. Para entrar, ese ejercicio tiene que:
   - ser trabajo resistido **prescribible para el objetivo y el nivel**, con dosis de sus capacidades. Si no hay ninguno, no se fuerza nada;
   - pertenecer al enfoque y respetar V22 (bíceps en tirón);
   - no repetir el día anterior ni lo que la sesión ya tiene;
   - dejar la sesión bajo el techo de duración, con la regla de la mitad, y sin bajar del mínimo de V20.

   Se cambian antes las colocaciones que no cubren un patrón de fuerza.

2. **Un estiramiento de tríceps (1322, 1119) en una sesión de tirón** (V22). Los complementos de movilidad se elegían sin el filtro de V22.

   **Corrección:** si la elección viola V22, se vuelve a elegir entre los admitidos. Solo en ese caso, para que una sesión que ya era válida salga idéntica.

Diff: `CapabilityWeekBuilder` (~120 líneas, la mayor parte comentario y el método nuevo). Nada más de producción.

## 1. Los 12 perfiles que se quedaban sin plan

`RespaldoCincoSeisDiasIT` prueba el generador con el validador de producción. Los casos de GENERAL_FITNESS se repiten con **4 semillas** (la semilla depende de usuario y semana).

| Perfil | Antes | Después |
|---|---|---|
| GENERAL_FITNESS INTERMEDIATE 5d casa | sin plan (tirón solo `back`) | **válido** |
| GENERAL_FITNESS INTERMEDIATE 6d casa | sin plan | **válido** |
| GENERAL_FITNESS ADVANCED 5d casa | sin plan | **válido** |
| GENERAL_FITNESS ADVANCED 6d casa | sin plan (además V22) | **válido** |
| LOSE_WEIGHT INTERMEDIATE/ADVANCED, 5/6d, gimnasio/casa (8) | sin plan (tirón y piernas) | **sigue sin plan**: el tirón ya sale bien; queda solo piernas, **demostrado imposible** con las reglas vigentes |

Con el código anterior, la misma prueba falla en 9 de sus 14 casos.

### Por qué los 8 de LOSE_WEIGHT no tienen solución sin cambiar una regla

La prueba fija las dos mitades de la contradicción:
- **V15** cuenta `lower legs` como grupo disponible (hay de 15 a 38 ejercicios de `lower legs` elegibles según nivel y lugar) y exige dos grupos en piernas.
- **4A v6 / 4D:** en LOSE_WEIGHT la única capacidad resistida es la fuerza, cuya dosis solo existe para ejercicios estructurales. Todo `lower legs` es RESISTIDO (flexión plantar) y su lista de capacidades admisibles para LOSE_WEIGHT está **vacía**, en los 4 niveles y lugares. Un gemelo en LOSE_WEIGHT no tiene dosis válida, y V24/V25 lo rechazarían.

No se fuerza ni se oculta. La salida requiere una decisión, y **ninguna está dentro de lo autorizado**:

| Opción | Qué cambia | Efecto |
|---|---|---|
| **a** | V15 cuenta como disponibles solo los grupos **prescribibles** para el objetivo | Piernas pasaría solo con muslo. Toca V15 |
| **b** | Admitir resistencia muscular o hipertrofia para ejercicios resistidos en LOSE_WEIGHT | Toca la política de capacidades (4A/4D) |
| **c** | Una división sin piernas aisladas para LOSE_WEIGHT de 5–6 días (p. ej. superior / inferior / cuerpo completo) | Toca `WeeklySplitPlanner`, que también recibe la IA |
| **d** | Tope provisional de 4 sesiones para LOSE_WEIGHT intermedio y avanzado | Es la opción B, que se descartó |

Recomendación: **a** es la más coherente con el objetivo («no forzar»): no tiene sentido exigir un grupo que el objetivo no puede prescribir.

## 2. Los 72 perfiles del barrido (4–6 días, 45 min)

Evidencia en `evidencia/frecuencia/correccion_respaldo/{antes,despues}/`.

| | Antes | Después |
|---|---|---|
| Planes válidos | 60 | **64** |
| Sin plan | 12 | **8** (solo LOSE_WEIGHT, solo piernas) |
| Sesiones | 270 | 292 (+22, las de los 4 planes recuperados) |
| Sesiones de planes antes válidos que cambian | — | **0** |
| Días programados según la política actual (BEGINNER ≤ 4) | sí | sí |
| Cobertura de capacidades de los válidos | 60 `COMPLETO` | 64 `COMPLETO` |
| Duración (% de lo configurado) | 67,8–82,3 | 67,8–82,3 |

**Robustez:** 288 generaciones, 4 semillas por perfil.

| | Antes | Después |
|---|---|---|
| Semanas sin plan | 52 | **32** (las 8 de LOSE_WEIGHT × 4, todas por piernas) |
| GENERAL_FITNESS sin plan | **20 de 24**, incluidos 4 de gimnasio que el barrido de una semilla no veía | **0** |

### Las 22 sesiones nuevas (GENERAL_FITNESS, casa)

- **Empuje:** pecho, hombro y tríceps.
- **Tirón:** 2 de espalda y 1 de bíceps.
- **Piernas:** 2–3 de muslo y 1 de gemelo.
- **Superior / inferior:** 3–4 grupos.
- Todas con 3–4 ejercicios de trabajo, 1 cardio y 1 movilidad, y dosis de las capacidades del objetivo.
- Duración del 68,6 % al 79,2 % de lo configurado.

**Señales que siguen abiertas, ajenas a esta corrección:**
- 11 sesiones quedan por debajo del 70 % exacto: es el defecto de V20, que se corregirá aparte.
- Stalder press (1267) y superman push-up (1296) entran en empuje: es el problema del catálogo.
- Algunos tirones abren con una bisagra catalogada como `back` (p. ej. 776), porque el selector elige por patrón de 4C. Funcionalmente es discutible. Corresponde a la revisión del catálogo y de V15, no a esta corrección.

## 3. Validadores y regresión

| Prueba | Resultado |
|---|---|
| `RespaldoCincoSeisDiasIT` | 14 de 14 (6 perfiles de GENERAL_FITNESS × 4 semillas, más los 8 de LOSE_WEIGHT con la imposibilidad demostrada) |
| `GeneracionReferenciaIT` | **72 de 72 idénticas**. Además, en el barrido, 0 de 216 sesiones de referencia cambian |
| Regresión completa (`mvn verify -Pintegration`, con la réplica P2-D local) | **BUILD SUCCESS**: 816 unitarias (809 más 7 de `V20UmbralRedondeoTest`) y 262 de integración, 0 fallos y 13 omitidas (las 10 opcionales de siempre más las 3 de `EvaluacionFrecuenciaIT`, que son opcionales) |

V15, V20, V22, V24 y V25 son los de producción, sin cambios: la corrección está en el generador.

## 4. Fase 2: V15 cuenta solo lo que el objetivo puede prescribir

### El cambio exacto

Está en `PlanDraftValidator.validateFocusCoverage`, en el cálculo de `disponibles`: cuántos grupos del enfoque tienen material.

| | Antes | Ahora |
|---|---|---|
| Un grupo está disponible si… | existe **algún elegible** de ese grupo (que no sea complemento de cardio o movilidad) | existe algún elegible de ese grupo que además pase `prescribibleParaV15` |

`prescribibleParaV15`, con las capacidades en **ENFORCE** (como en la generación de producción, con IA o motor de reglas), exige:
1. dificultad ≤ la máxima vigente del perfil;
2. que V22 lo admita en el enfoque (en tirón, bíceps; en empuje, tríceps);
3. al menos una capacidad que el objetivo admite y el ejercicio demuestra, **con una dosis válida** para el nivel. Es `CapabilityPrescriptionPolicy`, la misma fuente que V24 y V25.

Sigue igual, sin cambios:
- el mínimo (`min(2, disponibles, ejercicios de trabajo)`);
- la regla de grupos ajenos y la de la mitad;
- el conteo de grupos cubiertos;
- los mensajes.

**No es una excepción para LOSE_WEIGHT.** Si el objetivo puede prescribir los dos grupos, se siguen exigiendo los dos. Por ejemplo, GAIN_MUSCLE, GENERAL_FITNESS e INCREASE_STRENGTH sí pueden prescribir gemelos, y una sesión de piernas solo con muslo se sigue rechazando.

**Cuándo se cuenta como antes:**
- sin capacidades en ENFORCE: es el caso de la adaptación P2-D, que construye su validador sin ellas, y su V15 queda exactamente igual;
- sin matriz de capacidades para el objetivo.

En ambos casos V24/V25 no limitan lo que se puede poner, así que contar cualquier elegible es lo coherente.

**No cambian:** las capacidades y dosis de LOSE_WEIGHT, la frecuencia, la división semanal, V20, V22, V24, V25, el catálogo, la adaptación, la progresión, los prompts y Replicate.

### Pruebas

**`V15GruposPrescribiblesTest` (unitaria, 13 casos).** Con el código anterior fallan los 4 del defecto y pasan los 9 de protección.

| Caso | Resultado |
|---|---|
| LOSE_WEIGHT piernas solo con muslo (propuesta del motor de reglas y de la IA) | ya no se rechaza |
| LOSE_WEIGHT tren inferior solo con muslo (el abdomen RESISTIDO tampoco es prescribible) | ya no se rechaza |
| LOSE_WEIGHT tirón solo con espalda, si en brazos solo hay curl (RESISTIDO) | ya no se rechaza |
| GAIN_MUSCLE, GENERAL_FITNESS, INCREASE_STRENGTH: piernas solo con muslo | **se sigue rechazando** |
| LOSE_WEIGHT tirón solo con espalda, con un bíceps estructural disponible | **se sigue rechazando** |
| Propuesta de IA con un grupo ajeno, o con 4 de 6 en un grupo | **se sigue rechazando** |
| Validador sin capacidades (adaptación) | **igual que antes** |
| LOSE_WEIGHT sin cardio | **lo rechaza V25** |
| Un gemelo puesto a la fuerza en LOSE_WEIGHT | **lo rechaza V24** |

**`RespaldoCincoSeisDiasIT`:** los 8 de LOSE_WEIGHT, × 4 semillas, dan semana válida. Además comprueba:
- ningún gemelo en sesiones de trabajo y ningún ejercicio sin capacidad del objetivo;
- que la política de LOSE_WEIGHT no cambió: siguen existiendo gemelos elegibles y ninguno es prescribible.

### Resultados

Evidencia en `evidencia/frecuencia/correccion_respaldo/despues_v15/`.

| | Inicio | Tras la fase 1 | Tras la fase 2 |
|---|---|---|---|
| Barrido de 72 perfiles: planes válidos | 60 | 64 | **72** |
| Robustez, 288 generaciones: semanas sin plan | 52 | 32 | **0** |
| Sesiones de planes que ya eran válidos que cambian | — | 0 | **0** (frente al inicio y frente a la fase 1) |
| 72 referencias | idénticas | idénticas | **idénticas** (`GeneracionReferenciaIT`, y 0 de 216 sesiones cambian en el barrido) |

### Los 8 planes de LOSE_WEIGHT recuperados (44 sesiones)

| Rasgo | Valor |
|---|---|
| División | empuje / tirón / piernas / superior / inferior (5 días) o empuje / tirón / piernas × 2 (6 días), la de la política actual |
| Cada sesión | 2 ejercicios de fuerza estructural + 2 bloques de cardio. Es la misma estructura que los planes de 4 días de LOSE_WEIGHT intermedio y avanzado que ya eran válidos |
| Cobertura de capacidades | `COMPLETO` (CARDIO principal, STRENGTH secundaria) |
| Duración | 79,6 %–80,9 % de lo configurado |
| Piernas | 2 ejercicios de muslo (sentadilla, bisagra o extensión de cadera), sin gemelo |
| Tirón | espalda + un ejercicio de `upper arms` con patrón estructural. **En la práctica suele ser un ejercicio combinado** (ver abajo) |

**Hallazgo importante, del catálogo.** En LOSE_WEIGHT, un ejercicio de `upper arms` solo es prescribible si su patrón es estructural. Los curls de verdad son `ELBOW_FLEXION` (RESISTIDO) y no lo son, así que el tirón cubre «bíceps» con:
- **legítimos:** 452 *cable standing pulldown (with rope)* (`VERTICAL_PULL`);
- **combinados:** 527 *bicep curl lunge*, 609 *lunge with bicep curl* (`KNEE_DOMINANT`), 725 *biceps curl to shoulder press*, 760 *hammer curl and press* (`VERTICAL_PUSH`).

Un curl con press en un día de tirón cumple V15 y V22 por catálogo, pero no es lo que un entrenador pondría. No lo introduce la fase 2: el generador anterior ya colocaba 760 en tirón. La fase 2 tampoco lo empeora, porque solo deja de exigir grupos imposibles. Es un caso más de las incoherencias patrón/grupo de `EVALUACION_GENERACION_4_6_DIAS.md` §3, y su corrección corresponde a la revisión del catálogo.

**Otras señales que siguen abiertas**, del catálogo o de V15 por `body_part`, no de este cambio:
- en el tren inferior de 5 días, un ejercicio de muslo y uno catalogado como `waist` (1102, 1103 o 1225), que funcionalmente son de pierna;
- Stalder press (1267) y superman push-up (1296) en empuje;
- bisagras catalogadas como `back` (142, 776) abriendo el tirón.

### Regresión y CI (fase 2)

| Prueba | Resultado |
|---|---|
| Regresión completa (`mvn verify -Pintegration`, con la réplica P2-D local) | **BUILD SUCCESS**: 829 unitarias (816 más las 13 de `V15GruposPrescribiblesTest`) y 262 de integración, 0 fallos y 13 omitidas (opcionales) |
| `GeneracionReferenciaIT` | 72 de 72 idénticas |
| CI en GitHub (`verify`) sobre `97b890e` | en verde |

### Las propuestas de la IA siguen sometidas a V15

- El pipeline valida cada propuesta de la IA con el mismo `PlanDraftValidator` de producción (`PlanGenerationPipeline`), y V15 no distingue por origen.
- Las pruebas unitarias incluyen propuestas sintéticas `GenerationSource.AI`: grupo ajeno, más de la mitad, un solo grupo con dos prescribibles. Todas se siguen rechazando.

**Observación, sin tocar por alcance:** el prompt (`PlanInputSnapshot.suggestedSession`) sigue sugiriendo a la IA 2 grupos mínimos por sesión, sin saber cuáles son prescribibles. En LOSE_WEIGHT/piernas la IA podría proponer un gemelo, que V24 rechazaría, y gastar un intento. No cambia el resultado: el respaldo ya produce plan válido. Si se quiere evitar ese intento, habría que ajustar el prompt, que es un cambio aparte.

## 5. Fase 3: el mismo V15 en la generación y en la copia semanal

**Defecto.** Los 8 planes de LOSE_WEIGHT (intermedio y avanzado, 5–6 días, gimnasio y casa) que recuperó la fase 2 se generaban válidos. Pero en el ciclo semanal real la semana 2 terminaba en `NO_FACTIBLE`: «V15: … enfoque LEGS y solo trabaja [upper legs]».

**Causa.** La fase 2 contaba como disponibles solo los grupos prescribibles cuando el validador tenía las capacidades en ENFORCE. Así ocurre en la generación, pero no en la adaptación P2-D: `StrictValidationAdapter` construye su validador sin capacidades, salvo con progresión o con `fitsense.adaptation.capability-validation.enabled`. Por eso la copia volvía a exigir gemelos, que LOSE_WEIGHT no puede prescribir. Los mismos 8 fallaban con el catálogo V35 y con V36: no es un problema del catálogo.

**Cambio, autorizado (orden de corrección final, punto 2).** `PlanDraftValidator.prescribibleParaV15` deja de depender del modo de capacidades del validador. Basta con que haya perfil y matriz de capacidades del objetivo. Es la misma función para la generación y para la copia; no se duplica el criterio.

**Lo que no cambia:**
- `fitsense.adaptation.capability-validation.enabled` sigue apagado y no se toca `StrictValidationAdapter`;
- V24 por capacidades y V25 siguen con el modo de cada validador;
- la regla de grupos ajenos y la de la mitad;
- los umbrales de adaptación, D8, V8C, la recuperación y la progresión.

El cambio solo afecta al **mínimo** de grupos de V15. Solo deja de exigir un grupo que el objetivo no puede prescribir con dosis válida. Si hay dos grupos prescribibles, se siguen exigiendo los dos.

**Flujos que usan el validador de la adaptación**, todos cubiertos por la regresión:

| Flujo | Efecto |
|---|---|
| Copia semanal (NO_CHANGE, INSUFFICIENT_DATA) | Copia los 8 planes de LOSE_WEIGHT que antes no podía. El resto, igual |
| Corrección de la IA (ADAPT_LOCALIZED, ADAPT_EXPANDED) y su respaldo | Una propuesta de LOSE_WEIGHT con piernas solo de muslo ya no se rechaza por V15. Sigue sujeta a V24 y al resto de reglas |
| Reparación de seguridad de planes publicados (`PublishedPlanSafetyRepairService`) | Mismo criterio de V15 que la generación |
| Progresión (encendida solo en pruebas) | Ya usaba capacidades: sin cambio |
| Generación inicial | Sin cambio: ya usaba el criterio. Barrido, referencias y robustez **idénticos** a la fase 2 |

**Pruebas:**
- **`V15GruposPrescribiblesTest`** (17 escenarios, 36 casos):
  - la copia aplica el mismo criterio que la generación;
  - GAIN_MUSCLE, INCREASE_STRENGTH y GENERAL_FITNESS siguen exigiendo los dos grupos, tanto en la generación como en la copia;
  - en la copia se sigue rechazando un grupo ajeno, y un tirón con un solo grupo cuando el bíceps es prescribible.
- **`ContinuidadSemanalIT`** (nueva, ciclo real, sin IA):
  - **los 8 LOSE_WEIGHT:** semana 1 válida para el validador de la generación y con cobertura de capacidades COMPLETA; semana 2 publicada (SIN_ORDEN, CONSERVED), también válida y COMPLETA;
  - **96 perfiles** (4 objetivos × 3 niveles × 3–6 días × gimnasio/casa, la mitad cumpliendo todo y la otra mitad sin registros): **96/96 publican la semana 2**;
  - **sin el cambio, las dos pruebas fallan** con el NO_FACTIBLE descrito.

**Resultados:**
- Barrido 72/72.
- Robustez 288/288 VALIDA.
- 72 referencias idénticas.
- Regresión completa: **BUILD SUCCESS**, con 836 unitarias y 264 de integración, 0 fallos y 10 omitidas (opcionales).
