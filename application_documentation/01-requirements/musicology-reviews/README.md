| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-07-18 |
| **Author** | Sangeetha Grantha Team |

# Musicology Reviews

Composition-level musicological reviews (*lakshana*) that inform how specific krithis are modelled in
the database — raga sequences, section structure, musical form, terminology.

These are **evidence for data decisions**, not rules. The shared correctness contract lives in
[Domain Model §6](../domain-model.md#6-musicological-correctness-rules-lakshana); a review here
records the reasoning behind one composition's treatment when it is non-obvious or contested, so a
later migration or re-extraction does not silently undo a deliberate call.

Reviews are typically produced by the `carnatic-musicologist` subagent (`.claude/agents/`) and then
read by a human before anything is applied.

## Contents

| Review | Subject |
|:---|:---|
| [visvanatham-bhajeham-ragamalika.md](./visvanatham-bhajeham-ragamalika.md) | *viSva nAthaM bhajEhaM* (Dikshitar) — why the ragamalika is a separate composition from the Natabhranam krithi it was merged into during import, and how the ragamalika should be modelled once re-extracted. Backs migration `V47__demerge_ragamalika_visvanatham_from_natabharanam.sql`. |
