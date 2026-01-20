# 09 Ai

| Metadata | Value |
|:---|:---|
| **Status** | Draft |
| **Version** | 0.2.0 |
| **Last Updated** | 2026-01-20 |
| **Author** | System |

---

## Contents

- [gemini-knowledge-base.md](./gemini-knowledge-base.md)
- [gemini-selection-rationale.md](./gemini-selection-rationale.md)
- [integration-opportunities.md](./integration-opportunities.md)
- [integration-summary.md](./integration-summary.md)
- [vibe-coding-references.md](./vibe-coding-references.md)

---

## Conductor & AI Change Tracking

All changes to the codebase **must** be tracked via Conductor:

- **Register a Track**
  - Add a row to `conductor/tracks.md` with a new `Track ID`, name, and status.
  - Example: `TRACK-002 | Gemini Transliteration Hardening | Planned`.
- **Create a Track Detail File**
  - Create `conductor/tracks/TRACK-XXX-some-slug.md` modelled on `TRACK-001-bulk-import-krithis.md`.
  - Include at minimum: **Goal, Context, Architecture Overview, Implementation Plan (Phases), Progress Log, Technical Details**.
- **Keep Docs & Tracks in Sync**
  - When updating any document in `09-ai/` (this folder), reference the relevant `TRACK-XXX` in the doc’s change notes or progress sections.
  - When implementing AI features described here, update the corresponding TRACK file’s **Progress Log** as part of the change.

For overall Conductor guidance, see:

- `conductor/index.md`
- `conductor/product-guidelines.md`
- `conductor/workflow.md`