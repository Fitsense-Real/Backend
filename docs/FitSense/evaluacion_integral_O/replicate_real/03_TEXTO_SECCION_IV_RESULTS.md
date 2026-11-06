# Texto propuesto: sección IV. RESULTS (versión con IA real)

**Notas de uso:**
- **No modifica el short paper original;** es una versión propuesta.
- Usa solo resultados obtenidos (`01_INFORME_EVALUACION_REAL.md` y tablas de `entregables/`).
- Las subsecciones separan:
  - A, la evaluación integral con IA real (nueva);
  - B, la evaluación integral anterior con IA simulada;
  - C, la evaluación anterior U/N con IA real;
  - D, las limitaciones del catálogo.
- Ninguna cifra se atribuye a participantes humanos.

---

**IV. RESULTS**

**A. Integral evaluation with a real generative model (cohort O, Replicate)**

*Setup.* We executed the complete FitSense pipeline on the 20 frozen synthetic profiles O01–O20, with the adaptation step answered by a real language model through Replicate (gpt-5-mini; model version recorded before and after the runs, unchanged). Each profile followed its frozen four-week adherence script, extended up to 26 weeks where reincorporation had to be observed. Adherence is scripted; only the model responses are real. A main run (R_O) and a reproducibility run (R′_O) were executed with an independent rule-based oracle frozen beforehand. The two runs used 20 predictions at a measured cost of USD 0.30, within the authorized limit.

*Generation and personalization.* In both runs:
- 19 profiles received a valid plan;
- the predicted infeasible profile (advanced strength, one 30-min session per week) was reported through the limitations endpoint ("about 32 minutes per session are needed");
- no exercise violated a declared or age-based restriction in any week.

A complementary scenario identical to that profile but with 35 minutes obtained a valid one-session plan covering the four strength patterns, with no validation relaxed. It is reported separately and not counted among the 20 profiles.

*Adherence and decisions.* The scripted weeks 1–4 produced 256 scheduled sessions: 239 complete, 6 partial, 10 skipped and 1 unrecorded. Adherence recomputed by the oracle matched the stored metrics in every week. The weekly decision recomputed independently matched the backend in all cycles: 124/124 in R_O and 120/120 in R′_O.

*Adaptation with the real model.* Each run had 10 cycles that required the model.
- **Proposals and validation:** every proposal was answered in a single call and none was rejected by the validators.
- **R_O:** 8 adaptations were published. In one cycle the model returned "no change" although a valid removal existed, and the system conserved the session. In another, the provider failed and the deterministic fallback was applied.
- **R′_O:** all 10 adaptations were published.
- **Pertinence:** no adaptation was inadequate. Over the sessions targeted by a model-requiring intervention, R_O had 10 pertinent and 2 justified conservations; R′_O had 10 pertinent, 1 valid alternative and 1 justified conservation.
- **Constraints:** published adaptations never increased workload or difficulty. Untargeted sessions remained unchanged, and exercises excluded for pain never returned.
- **Style compared with the simulated responses:** the real model tended to reduce workload more broadly (e.g., −21 % to −62 % of session work).

*Progression, recovery and reincorporation.* Each run produced 32 progression steps, all within the volume ceiling and only after two consecutive weeks of full compliance. Recoveries to the reference dose after fatigue: 3 in R_O and 2 in R′_O. One recovery was not possible because the minimum step exceeded the configured limit of 10 %.

Reincorporation depends on the model first removing an exercise. It occurred once, in R′_O, at week 23, with the initial dose and only after a later progression step. Where the model chose to reduce the dose instead of removing an exercise, there was nothing to reincorporate.

*Ten-aspect matrix.* With the oracle frozen before the paid runs:
- R_O: 140 of 140 applicable cells were approved (100 %);
- R′_O: 140 of 141 (99.3 %).

The single failure was traced to the evaluation instrument. For a session with unlocated pain, the model replaced all exercises with fewer, easier ones and less work. The oracle penalized this with a same-pattern rule that the approved system rule does not apply to pain. The corrected oracle changes only that cell (141/141) and still rejects more additions than removals, more work, harder substitutes and any remaining exercise of the painful session. Both figures are reported.

*Reproducibility.* Between R_O and R′_O:
- the decision and intervention were identical in 10/10 model cycles;
- the outcome classification and pertinence matched in 8/10;
- the exact change matched in only 1/10.

The deterministic core reproduces exactly, while the model reaches different valid solutions.

**B. Integral evaluation with a simulated model (previous)**

The same cohort was evaluated earlier with laboratory-written model responses: a deliberately invalid first proposal followed by a valid one. All 142 applicable cells were approved (100 %), after a documented correction of two oracle defects (first scoring 97.9 %). That evaluation measured the technical path, including rejection of invalid proposals, not the behaviour of a real model.

**C. Earlier evaluation with a real model (cohorts U and N)**

An earlier code version was evaluated with the real model on cohorts U and N (44 predictions, USD 0.56). It met all pre-registered criteria:
- acceptance: 16/16 and 27/28;
- pertinence: 17/17 and 14/15, with no non-pertinent change.

It used a different version and protocol and is not pooled with Section IV-A.

**D. Catalogue limitations**

Technical approval does not establish sports quality. In cohort O, 14 of 19 initial plans contain exercises with pending catalogue findings: wording or instructions, a Nordic curl under review, and doses supported only by undemonstrated exercises. These are reported as "requires review" and excluded from the approvals. One model adaptation that introduced new movement patterns after unlocated pain is also flagged for professional review.
