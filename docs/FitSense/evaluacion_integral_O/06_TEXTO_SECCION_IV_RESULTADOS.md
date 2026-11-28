# Texto definitivo propuesto: sección IV. RESULTS

**Notas de uso:**
- Basado exclusivamente en resultados obtenidos:
  - la repetición S_O de la cohorte O con el oráculo v1.1 (definitiva), con la v1.0 como antecedente;
  - el informe `rediseno_politicas/pre_replicate/25_RESULTADOS_EVALUACION_REAL.md` (U/N con IA real).
- **No modifica el short paper original;** es el texto listo para incorporarlo.
- Las subsecciones A, B y C separan las tres cosas que pide la orden: la evaluación integral simulada, la evaluación previa con IA real y las limitaciones del catálogo.
- Las tablas citadas son `entregables/TABLAS_SHORT_PAPER.md`. Para el artículo, la Tabla 5b basta como tabla resumen y la Tabla 1 como descripción de la cohorte.

---

**IV. RESULTS**

**A. Integral evaluation of 20 synthetic profiles with simulated AI (cohort O)**

*Cohort and protocol.* We evaluated FitSense on 20 synthetic profiles (O01–O20). The cohort covers:
- the four goals (5 profiles each);
- three levels (7 beginner, 7 intermediate, 6 advanced);
- three equipment conditions (9 gym, 6 home without equipment, 5 home with dumbbells and bands);
- 1–7 requested days and 30–75 min sessions;
- declared restrictions and age-based filters (62 and 67 years).

Each profile followed a frozen four-week script with complete, partial and skipped sessions and with fatigue, difficulty, demotivation and pain reports. The weekly adaptation cycle ran after each week. Progression was enabled only for the six profiles whose scenario required it. Two reincorporation scenarios were extended until their condition was met.

The AI adaptation step was simulated: the full backend path ran without calling the language model, at no cost. **Adherence is scripted and does not describe the behaviour of human participants.**

The profiles, scripts, expected criteria and an independent rule-based oracle were frozen before execution.

*Generation and personalization.* 19 profiles received a valid plan. The remaining one (advanced strength, one 30-min session per week) was the predicted infeasible case; the limitations endpoint explained it ("about 32 minutes per session are needed").

In all 19 plans:
- the number of sessions equalled min(requested days, level cap, available days), with the cap applied in 3 profiles;
- no session exceeded the profile's minutes;
- no exercise violated a declared or age-based restriction in any week.

*Execution and adherence.* Weeks 1–4 contained 256 scheduled sessions: 239 complete, 6 partial, 10 skipped and 1 unrecorded. The weekly observed adherence, registration coverage and weighted adherence recomputed by the oracle matched the stored values in every week. The oracle's independently recomputed adaptation decision matched the backend in 103/103 weekly cycles. In weeks 1–4 the decisions were 63 no-change, 11 localized and 2 expanded adaptations.

*Adaptation and maintenance.* 15 sessions received an adaptation order. Eleven were modified pertinently:
- load reduction for fatigue;
- an easier variant for difficulty;
- a substitute for pain;
- removal of a filler exercise for lack of time.

Four were conserved because the cause did not authorize a change. No adaptation increased the workload or introduced a harder substitute. Sessions without an adaptation order or a registered progression step were copied unchanged. The deliberately invalid first proposal of the simulated AI was rejected in every cycle in which the AI was invoked.

*Progression, recovery and reincorporation.* The system produced 35 progression steps, 3 recoveries to the reference dose after a fatigue reduction, and 2 reincorporations. All stayed within the 1.10 volume ceiling and never exceeded one step per cycle. No progression occurred without two consecutive weeks of full compliance.

A previously withdrawn exercise returned only after a later progression in its session lineage, at the end of the session and with its initial dose (weeks 10 and 23). An exercise excluded for pain never returned.

*Ten-aspect matrix.* Of the 200 profile–aspect cells:
- 142 were approved and none failed;
- 43 were not applicable;
- 15 required review because the content includes exercises whose sports validation is pending. They are reported separately and not counted as approvals.

The technical approval rate over applicable cells was **100 % (142/142)**. A repeated execution produced an identical normalized fingerprint and an identical matrix.

*Correction of the evaluation instrument.* The first scoring of the same execution, with oracle v1.0, gave 139/142 (97.9 %). The three failures were traced to the oracle, not to the system:
- it rejected the recovery step that the adaptation policy allows when progression is disabled;
- it searched for a withdrawn exercise across the whole week instead of within its session lineage.

The corrected oracle (v1.1) was frozen before the repeated execution. It changes exactly those three cells. Mutation tests showed that it still rejects:
- recoveries above the reference dose;
- unauthorized progressions;
- reincorporations without a prior progression;
- improper reappearances.

This is a re-evaluation of the same cohort, not an independent sample.

**B. Earlier evaluation with a real language model (cohorts U and N)**

An earlier code version was evaluated with the real model (Replicate, two independent runs R and R′) on cohort U and on the unseen cohort N (20 profiles each). The run used 44 predictions at a measured cost of USD 0.56, with no provider failures or timeouts. All pre-registered criteria were met:
- Feasibility: 20/20 profiles in U and 19/20 in N, the remaining N profile being the predicted infeasible case. There were no safety violations.
- Acceptance of the AI proposals by the validators: 16/16 in U and 27/28 in N (Wilson lower bound 82.3 %). The non-accepted cycle was a "no change" answer, not a rejection.
- Pertinence over feasible evaluable adaptations, with no non-pertinent change: 17/17 in U and 14/15 in N (Wilson lower bound 70.2 %).

Between R and R′, the decision and its category were reproduced in 30/31 sessions. The exact change matched in only 13/31: the model reached different valid solutions.

These figures belong to a different version and protocol from Section IV-A and are not pooled with it.

**C. Catalogue limitations requiring professional validation**

Technical approval does not establish sports quality.

In cohort O, 14 of the 19 initial plans and one substitute contain exercises with pending catalogue findings:
- exercises whose name or instructions need correction;
- a Nordic curl whose difficulty is under review;
- back or shoulder doses supported only by exercises not yet demonstrated.

These are the 15 cells marked "requires review".

In the real-AI evaluation:
- 38.5 % (20/52) of feasible AI adaptations touched such exercises and were not counted as pertinent. In the worst case, pertinence would fall to 70.8 % (U) and 50.0 % (N).
- 67–84 % of published plans contained at least one flagged exercise.

The pending catalogue decisions were not incorporated into either evaluation and remain subject to professional review.
