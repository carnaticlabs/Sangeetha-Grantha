| Metadata | Value |
|:---|:---|
| **Status** | Archived |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-07-19 |
| **Author** | Sangeetha Grantha Team |

> **⚠️ RETIRED (2026-07-19):** These are completed one-shot data repairs, archived by
> [TRACK-127](../../../conductor/tracks/TRACK-127-worker-script-relocation.md). They are kept
> as a record of what was changed and why — **they are not runnable as-is** (see below).

# Krithi Worker Repair Scripts (Phase 4A)

Three single-use scripts that repaired lyric-section data in place. They lived in
`tools/krithi-extract-enrich-worker/src/` — the installable, Docker-shipped worker
package — which meant one-shot repair tooling was being built into the production
image. TRACK-127 moved them out.

| Script | What it repaired |
|:---|:---|
| `repair_sections.py` | Krithis whose lyrics parsed to zero sections, or to a single `OTHER` section, by re-running `StructureParser` over the stored lyric text. |
| `repair_section_text.py` | Section *text* that had been stored against the wrong section, re-splitting each lyric variant against the canonical section skeleton. |
| `repair_comprehensive.py` | The combined pass: rebuilt canonical sections, backfilled a `PALLAVI` for header-less (nOTTu-svara) compositions, and dropped stray top-level `MADHYAMA_KALA` sections. |

## When these ran

The repairs completed under
[TRACK-079](../../../conductor/tracks/TRACK-079-e2e-pipeline-section-fix.md)
(E2E Pipeline Validation & Lyric Section Consistency Fix). Section consistency across
all krithis has been 100% since that track closed, so there is no remaining work for
these scripts to do.

## Why they are not runnable

Two deliberate changes were made on archiving:

1. **Imports.** They use package-relative imports (`from .structure_parser import …`)
   that only resolved while they sat inside `src/`. They were *not* rewritten, because
   rewriting untested archived code is riskier than leaving it as an accurate record.
2. **Database URL.** `repair_sections.py` carried a hardcoded fallback DSN
   (`postgresql://sangita:sangita@…`) whose credentials match no environment in this
   repo — a hazard if anyone re-ran it expecting a sane default. It now reads
   `DATABASE_URL` and raises if unset.

If a similar repair is ever needed, write a fresh script under
`tools/krithi-extract-enrich-worker/scripts/` using `scripts/_common.py` for DSN
resolution and logging, and use these files as reference for the SQL and the
section-rebuild logic.

## Related

- [TRACK-127](../../../conductor/tracks/TRACK-127-worker-script-relocation.md) — the relocation
- [TRACK-079](../../../conductor/tracks/TRACK-079-e2e-pipeline-section-fix.md) — the repairs these performed
- [TRACK-126](../../../conductor/tracks/TRACK-126-python-worker-quality-gates.md) — the worker's quality gates
