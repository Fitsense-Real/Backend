# 01 · Auditoría de ramas y dependencias

**Fecha:** 03/11/2026. Todo se ha leído del repositorio remoto (`git fetch`) y de GitHub Actions; no se ha supuesto nada.

## 1. `generation`

| | |
|---|---|
| SHA actual | **`0974fb3eafc39e8b7397b4dbfe7c4405e4847440`** (Merge pull request #27) |
| ¿Coincide con la referencia histórica? | **Sí**: 0 commits entre la referencia y `origin/generation` |
| CI | Verde (run 41545633892, `push`) |

Por eso la referencia de regresión (960 perfiles y REPLAY) de `generation` y la histórica son la misma.

## 2. La cadena de PR

Todos los PR están **abiertos, en borrador y con el CI en verde** en su commit actual.

| PR | Rama | Base | Cabeza | CI (run) | Commits |
|---|---|---|---|---|---:|
| #28 | `lab/experimento-kora-20usuarios` | `generation` | `0f17ac7` | 41570491767 ✅ | 15 |
| #29 | `experiment/adaptation-prompt-v2` | #28 | `51c145b` | 41577492912 ✅ | 11 |
| #30 | `experiment/adaptation-v2-quality-validation` | #29 | `7580f08` | 41637134939 ✅ | 7 |
| #31 | `experiment/adaptation-functional-safety-h1-h12` | #30 | `77ca7dc` | 41662611426 ✅ | 6 |
| #32 | `docs/catalogo-revision-580-775-780-862` | #31 | `21fb5db` | 41676575572 ✅ | 2 |
| #33 | `experiment/adaptation-h12-policy` | #32 | `6ad20fe` | 41676557370 ✅ | 9 |
| #34 | `candidate/h1-multiconjunto` | #31 | `5a6b481` | 41677011040 ✅ | 2 |
| #35 | `feature/justificacion-adaptacion-verificable` | #34 | `58b259f` | 41678886620 ✅ | 3 |

## 3. Qué cambia cada PR (frente a su base)

| PR | `src/main` | `src/test` | `docs` | Migraciones | Clase |
|---|---|---|---:|---:|---|
| #28 | — | `ExperimentoKora20IT` (laboratorio *opt-in*) | 143 | 0 | Laboratorio y evidencia |
| #29 | **`FactibilidadAdaptacion` (nueva, 349 líneas), `SessionFixPromptBuilder` (+61), `PlanDraftValidator`, `ReplicateSessionFixGenerator`, `ReplicateTrainingPlanGenerator`, `SessionFixInputSnapshot`; propiedad `fitsense.ai.adaptation.prompt-version=v1`** | `SessionFixPromptV2Test`, `PlanInputSnapshotFixture`, laboratorio | 216 | 0 | **Experimental (prompt V2)** |
| #30 | — | `V8CDiagnosticoSustitucionesTest` (documenta V8C histórica), laboratorio | 255 | 0 | Evaluación y evidencia |
| #31 | **`ConservationValidator` (+82: `ReglaPatron`, `revisarMulticonjunto`); propiedad `pattern-rule` (por defecto `conjunto`)** | `V8CConservacionPatronH1Test`, `H1ImpactoHistoricoTest` (*opt-in*), laboratorio | 96 | 0 | **H1** (apagado) |
| #32 | — | — | 2 | 0 | Documentación del catálogo |
| #33 | **`ConservationValidator` (+122: H12-A/C), `FactibilidadAdaptacion`, `SessionFixPromptBuilder`; 2 propiedades** | `V8CPoliticaVarianteH12Test`, `PromptVarianteH12Test`, laboratorio | 266 | 0 | **Experimental (H12)** |
| #34 | **`ConservationValidator` (multiconjunto por defecto, 23 líneas); propiedad (por defecto `multiconjunto`)** | 3 IT nuevas del caso U04; 2 pruebas ajustadas a `CONJUNTO` explícito; laboratorio (comentario) | 9 | 0 | **H1** (candidata) |
| #35 | **`DescripcionAdaptacion` (nueva), `WeeklyTrainingPlanCommandServiceImpl` (+10)** | `DescripcionAdaptacionTest`, IT del texto publicado | 2 | 0 | **Textos** |

## 4. Dependencias de H1

| Pregunta | Respuesta | Cómo se comprobó |
|---|---|---|
| ¿`ConservationValidator` de #31 parte del de `generation`? | **Sí**: el archivo es **idéntico** en `generation` y en #30, la base de #31 | `git diff --quiet origin/generation origin/experiment/adaptation-v2-quality-validation -- ConservationValidator.java` |
| ¿H1 usa algo de #29 (V2)? | **No**. Solo usa `CandidateExercise`, `Diferencias`, el catálogo y `Map`/`TreeMap`. `FactibilidadAdaptacion` no aparece | Revisión del diff y compilación de la rama limpia sobre `generation` |
| ¿Las pruebas de H1 usan algo experimental? | **No**. `ConservacionPatronH1Base` usa los servicios reales de `generation` y simula la IA con `@MockitoBean ReplicateSessionFixGenerator` (`interpretar`, que ya existe en `generation`) | Compilación y ejecución en la rama limpia (`04`) |
| ¿Y `V8CDiagnosticoSustitucionesTest` (de #30)? | Es una prueba unitaria pura del comportamiento histórico. No depende del laboratorio | Igual |
| ¿`H1ImpactoHistoricoTest`? | Es un laboratorio *opt-in* que lee evidencia CSV de #28–#31 (`docs/...`) que no está en `generation`. **No se integra**: se ejecutó aparte para revalidar las 123 sustituciones (`04` §3) | — |
| ¿La propiedad de #34 arrastra la de V2? | En la rama #34 sí aparece `fitsense.ai.adaptation.prompt-version=v1` (viene de #29). **En la rama limpia no se incluye** | Diff de `application.properties` |

**Conclusión:** H1 puede integrarse sobre `generation` **sin V2, sin H12 y sin laboratorios**. No hay ninguna dependencia que obligue a detener la integración.

## 5. Matriz de cambios

| Componente | Necesario para el MVP | Estado | Rama limpia |
|---|---|---|---|
| Corrección H1 (`MULTICONJUNTO`) | Sí | Aprobado | **PR A** |
| Pruebas permanentes de H1 (unitarias e IT U04) | Sí | Aprobado | **PR A** |
| Compatibilidad con la regla histórica (`conjunto`) | Sí | Aprobado | **PR A** |
| Textos correctos de adaptación (`adjustment_reason`) | Sí | Aprobados con revisión (corregidos, `05`) | **PR B** |
| Mensaje de intención (`message_shown`, P-T2/P-T3) | Sí | Autorizado para preparar y probar | **PR B** |
| Corrección del prompt (P-T1) | Pendiente | Solo propuesta | — (`07`) |
| Prompt V2 (#29) | Todavía no | Experimental | — |
| H12-A/B/C/D (#33) | No | Pendiente de revisión profesional | — |
| Laboratorios experimentales (#28–#35) | No para producción | Se conservan como evidencia en sus PR | — |
| Catálogo modificado | No | Pendiente de profesional (#32) | — |
