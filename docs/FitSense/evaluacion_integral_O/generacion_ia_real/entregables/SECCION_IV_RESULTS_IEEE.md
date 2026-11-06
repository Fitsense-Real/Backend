# Propuesta: sección IV. RESULTS (generación inicial y adaptación con IA real)

**Notas de uso:**
- **No modifica el short paper original.**
- Usa solo resultados de GRA-R1 y GRA-R2 (`INFORME_INTEGRAL_GENERACION_ADAPTACION_IA_REAL.md`).
- Las evaluaciones anteriores (R_O / R′_O con generación determinista, IA simulada, U/N) quedan como antecedentes y no se mezclan.
- Ninguna cifra se atribuye a participantes humanos.

---

**IV. RESULTS**

**A. Setup**

We evaluated the complete FitSense pipeline with a real generative model (gpt-5-mini through Replicate) used at two stages:
- the initial weekly plan;
- the later adaptations.

The 20 frozen synthetic profiles (O01–O20; four goals, three levels, three equipment conditions, 1–7 days, 30–75 min, declared and age-based restrictions) followed scripted four-week adherence sequences, extended up to 26 weeks where reincorporation had to be observed. **Adherence is simulated; model responses are real.**

The model proposes; the backend decides:
- **At generation,** the model receives the profile, the constraints and a pre-filtered exercise catalogue, and proposes the week (sessions, exercises, sets, repetitions or durations, rests). The backend validates it with the same rules applied to its deterministic generator (27 checks, including dose-per-capacity and mandatory movement patterns). It returns rejection reasons for up to two retries and uses the deterministic generator only as a labelled fallback.
- **At adaptation,** the model modifies only the sessions flagged by the backend's weekly decision. The result is validated for conservation and safety.

Two independent runs (GRA-R1, GRA-R2) on clean databases used 76 predictions at a measured cost of USD 1.54. The model version was recorded before every block and after the last, and did not change. An independent rule-based oracle, frozen before the paid runs, evaluated ten aspects per profile.

**B. Initial plan generation with the real model**

Plans:
- In both runs, all 19 feasible profiles and a complementary scenario received a plan **proposed by the model and accepted by the backend**. The complementary scenario is identical to the infeasible profile but has 35 instead of 30 minutes, and is reported separately.
- No plan came from the deterministic fallback.
- The predicted infeasible profile (advanced strength, one 30-min session) was reported through the limitations endpoint before any model call.

Acceptance:
- 13 and 14 of the 20 proposals were valid at the first attempt; the rest were valid at the second or third.
- The backend rejected 10 and 6 proposals, respectively, mainly for exceeding the per-session or weekly volume of a muscle group, or for missing a mandatory pattern. Each rejection cited the exact rule, and no invalid proposal was published.

Plan properties:
- All published plans respected the declared and age-based restrictions.
- Compared with the deterministic generator on the same profiles, the model's plans were slightly shorter (median 30 vs 33 min) and more conservative in weekly dose: 16 of 19 below the orientative dose, versus 9 of 19.

Cost and latency: median latency was 67–76 s per call, and generation cost USD 0.70 and 0.57 per run.

**C. Execution, adherence and weekly decisions (deterministic core)**

The scripted weeks 1–4 produced 256 scheduled sessions: 239 complete, 6 partial, 10 skipped and 1 unrecorded. Adherence recomputed by the oracle matched the stored metrics in every week. The weekly decision computed by the backend on the model-generated plans matched the oracle in 112/112 and 124/124 cycles.

**D. Adaptation with the real model**

Each run had 10 cycles requiring the model.
- **Proposals:** none was rejected by the validators.
- **GRA-R1:** all 10 adaptations were published.
- **GRA-R2:** 9 were published. In one cycle the model returned "no change" although a valid removal of a filler exercise existed, and the session was conserved; we report it as an avoidable conservation.
- **Pertinence:** no adaptation was inadequate.
- **Constraints:** published adaptations reduced or preserved workload, never increased difficulty, and left untargeted sessions unchanged. Exercises excluded for pain never returned. Each adaptation started from the plan generated in the same run.

**E. Progression, recovery and reincorporation**

| | GRA-R1 | GRA-R2 |
|---|---|---|
| Progression steps | 26 | 30 |
| Recoveries to the reference dose after fatigue | 3 | 3 |

All steps stayed within the 1.10 volume ceiling, and no progression occurred without two weeks of full compliance. A withdrawn exercise was reincorporated once (GRA-R1, week 15), at the end of its session with its initial dose and after a progression step. In GRA-R2 the model did not withdraw any exercise, so reincorporation was not applicable.

**F. Ten-aspect evaluation and reproducibility**

| | GRA-R1 | GRA-R2 |
|---|---|---|
| Applicable cells approved | 150 of 150 (100 %) | 148 of 148 (100 %) |
| Failures | 0 | 0 |
| Not applicable | 44 | 45 |
| Requires review (catalogue) | 6 | 7 |

Reproducibility between the runs:
- The deterministic elements were reproduced exactly: number of sessions, weekly decisions and interventions.
- The model's outputs were not: no initial plan was identical (median Jaccard similarity of exercises 0.36), and no adaptation was the same change. All of them satisfied the same constraints.

**G. Limitations**

- Adherence is scripted.
- The samples are small (20 generations and 10 adaptations per run).
- Sports quality is not established:
  - several plans include exercises under professional review;
  - the model's plans tend to stay below the orientative weekly dose;
  - one adaptation introduced a new movement pattern after unlocated pain, which is flagged for professional review.
