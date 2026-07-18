| Metadata | Value |
|:---|:---|
| **Status** | Open — remediation partially applied |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-07-18 |
| **Author** | Sangeetha Grantha Team |
| **Relates to** | [ADR-015](../../../02-architecture/decisions/ADR-015-govindan-extraction-adapter.md) |

# Section-Issues Cleanup

Working set for the Curator **Section Issues** queue — lyric variants whose section structure does
not match their krithi's English/Latin template. These files were previously loose in the repository
root; they are grouped here because they are one artefact set, still in use.

## Contents

| File | What it is |
|:---|:---|
| [section-issues-cleanup-findings.md](./section-issues-cleanup-findings.md) | The remediation log — per-pass root cause, what was fixed, what was deliberately deferred and why. Cited as evidence by ADR-015. |
| [flagged-krithis-for-review.md](./flagged-krithis-for-review.md) | The **open** work list: variants the automated split intentionally skipped because it could not be made template-exact without risking lyric corruption. Each needs a human decision or re-extraction. |
| [flagged_for_review.json](./flagged_for_review.json) | Same data, machine-readable (category counts + krithi ids). Consumed by the triage scripts. |

The record of what the automated pass already **fixed** is archived at
[archive/quality-reports/fixed-krithis-for-review.md](../../../archive/quality-reports/fixed-krithis-for-review.md) —
it documents applied work and is kept for history only.

## Status

The queue has moved 2945 → 484 → **112** across three passes. The remainder is not a data-entry
backlog: ADR-015 concludes it needs a Govindan-house-style extraction adapter (Part B, still open),
covering the transliteration-key preamble and script-aware `<sup>` footnote handling. Do not attempt
to clear the remaining 112 by hand-editing — the guiding rule throughout has been *never guess when a
split would not match the template exactly*.

## Regenerating

The triage scripts take the flagged list as an argument, so they follow this path:

```bash
cd tools/krithi-extract-enrich-worker
uv run python scripts/section_triage_batch.py \
  --flagged ../../application_documentation/07-quality/results/section-issues-cleanup/flagged-krithis-for-review.md
```

Scripts are dry-run by default; `--apply` writes. All writes go through the audit-logged
variant-sections / krithi-sections APIs — never raw SQL.
