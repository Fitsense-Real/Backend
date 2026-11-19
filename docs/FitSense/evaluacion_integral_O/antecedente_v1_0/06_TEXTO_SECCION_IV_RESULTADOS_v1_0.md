# Texto propuesto: sección IV. RESULTADOS

**Notas de uso:**
- Basado exclusivamente en la corrida S_O de la cohorte O y en su oráculo oficial v1.0.
- **No modifica el short paper original.** Es una propuesta para que la revises.
- Si apruebas la corrección del oráculo (v1.1) y se repite la evaluación, habrá que actualizar las cifras marcadas con [*].

---

**IV. RESULTS**

**A. Integral synthetic evaluation (cohort O)**

We evaluated the deterministic core and the adaptation pipeline of FitSense on a new cohort of 20 synthetic profiles (O01–O20). The cohort was frozen before execution together with its expected criteria and an independent oracle.

**Cohort.** The cohort covers:
- the four goals (5 profiles each);
- three fitness levels (7 beginner, 7 intermediate, 6 advanced);
- three equipment conditions (9 gym, 6 home without equipment, 5 home with dumbbells and bands);
- 1–7 requested days and 30–75 min sessions;
- declared restrictions (impact, shoulder, knee) and age-based filters (62 and 67 years).

**Protocol.** Each profile followed a scripted four-week adherence sequence (complete, partial, skipped, fatigue, difficulty, demotivation and pain), followed by the weekly adaptation cycle. Two exceptions:
- Progression was enabled only for the six profiles whose scenario required it.
- Two reincorporation scenarios were extended until the condition was met.

The AI adaptation step was simulated: the full backend path was executed without calling the external model, and no cost was incurred. Adherence is simulated and does not represent real behaviour change.

**Generation.** 19 of 20 profiles received a valid plan. The remaining profile was the predicted infeasible case: advanced strength, one 30-min session per week. It was reported as such through the limitations endpoint ("about 32 minutes per session are needed").

In all 19 plans:
- the number of sessions matched min(requested days, level cap, available days), with the cap applied in 3 profiles;
- no session exceeded the profile's minutes;
- no exercise violated the declared or age-based restrictions in any week.

**Adherence and decisions.** Across weeks 1–4 the profiles had 256 scheduled sessions: 239 complete, 6 partial, 10 skipped and 1 unrecorded. Weekly observed adherence, registration coverage and weighted adherence recomputed by the oracle matched the stored metrics in every week.

The adaptation decision recomputed independently matched the backend in 103/103 weekly cycles. In weeks 1–4 the decisions were 63 no-change, 11 localized and 2 expanded adaptations.

**Adaptation.** Of the 15 sessions that received an adaptation order:
- 11 were modified pertinently: load reduction for fatigue, an easier variant for difficulty, a substitute for pain, removal of a filler exercise for lack of time;
- 4 were deliberately conserved because the cause did not authorize a change;
- no adaptation increased the workload or introduced a harder substitute;
- the deliberately invalid first proposal of the simulated AI was rejected in every cycle in which the AI was invoked.

**Progression and reincorporation.** The system produced:
- 35 progression steps, 3 recoveries and 2 reincorporations, all within the 1.10 volume ceiling and never more than one per cycle;
- no progression before two consecutive weeks of full compliance.

A previously withdrawn exercise returned only after a later progression step, at the end of its session and with its initial dose (weeks 10 and 23). An exercise excluded for pain never returned.

**Ten-aspect matrix.** Of the 200 profile–aspect cells:
- 139 were approved and 3 failed [*];
- 43 were not applicable;
- 15 required review because the content includes exercises whose sports validation is pending. They are reported separately and not counted as approvals.

The technical approval rate over applicable cells was 97.9 % [*]. The three failures were traced to the evaluation oracle, not to the system:
- it did not admit the recovery step allowed by the policy when progression is disabled;
- it located a withdrawn exercise at week level instead of within its session lineage.

With the corrected oracle the same outputs give 142/142. This figure is reported as diagnostic only, pending a new full run.

**Reproducibility and regression.**
- A second execution produced an identical normalized fingerprint.
- The extended test harness reproduced the reference fingerprints of the earlier cohorts and of the historical policy.

These results characterize technical compliance on synthetic data. They are reported separately from the earlier evaluation with the real language model (cohorts U and N) and do not establish the sports quality of the catalogue, which remains under professional review.
