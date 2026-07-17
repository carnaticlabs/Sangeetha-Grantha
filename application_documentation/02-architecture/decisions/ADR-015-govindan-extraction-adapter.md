| Metadata | Value |
|:---|:---|
| **Status** | Proposed — partially implemented (template repair + Tamil trailer strip shipped; adapter Part B open) |
| **Version** | 0.2.0 |
| **Last Updated** | 2026-07-17 |
| **Author** | Sangeetha Grantha Team |
| **Deciders** | Sangeetha Grantha Team (Seshadri) |
| **Relates to** | [ADR-012 Unified Extraction Architecture](./ADR-012-unified-extraction-architecture.md) |
| **Evidence** | `section-issues-cleanup-findings.md`, `.triage-cache/triage_report3.json` |

# ADR-015: Structure-Aware Extraction Adapter for the Govindan Blog Family

## Context

The Curator **Section Issues** queue — variants whose section structure does not match their
English/Latin template — stood at **484**. A cleanup pass (see `section-issues-cleanup-findings.md`)
brought it to **339** by fixing the CAT-B truncation bug, adding per-script inline section-label
recognition, and re-splitting through the audit-logged variant-sections API under strict safety gates.

The residual **339** are dominated by two failure modes of the **generic text-regex parser**, both
traced to a single upstream source family:

1. **Template under-count (~30 krithis / ~175 variants).** The Latin template itself is missing a
   leading section. Root cause confirmed on `giripai nelakonna`: the pallavi marker renders as
   `"P 1giripai…"` (from `<span style="font-size:180%">P</span><sup>1</sup>giripai`), and the Latin
   inline-marker rule `^\s*P (?=[a-zA-Z])` rejected the footnote digit, so the pallavi was
   head-captured and dropped at import. Every sibling then mismatches the truncated template.
2. **Translation trailers (~60+ Tamil variants).** The `thyagaraja-vaibhavam` **Tamil** variants
   append an inline word-by-word Tamil meaning after the lyric. Marker-based splitting keeps the
   trailer in the last section (observed 5–18× the size of the English last section).

### The source is one house-style, not one blog

`thyagaraja-vaibhavam` (Tyagaraja), `guru-guha` (Dikshitar) and `syamakrishnavaibhavam`
(Syama Sastri) are all **V Govindan's** blogs and share an identical, years-stable template:

- a Modified **Harvard-Kyoto** transliteration preamble (present on 100% of sampled pages);
- section markers `P` / `A` / `C` (pallavi/anupallavi/charanam) — often as `font-size:180%` spans,
  **but not always** (absent on a substantial minority of pages, which mark sections in plainer text);
- `<sup>` footnote references — in the **Latin** these are always footnote refs; in **Indic** scripts
  superscript digits are *pronunciation* markers (க`3` = ga, ப`4` = bha) and must be preserved;
- terminal `Gist` / `Word-by-word Meaning` blocks (present on 100% of sampled pages) that bound the
  end of the lyric.

Because this one house-style backs essentially the **entire** flagged queue, source-coupled handling
here has unusually high leverage and unusually low fragility — the two conditions that normally make
source-specific parsing a bad trade are inverted.

## Decision

Adopt a **domain-gated, structure-aware adapter for the Govindan blog family**, layered over the
existing generic pipeline (ADR-012), **not** a forked parser. Split responsibilities by layer:

**A. Generic parser/extractor (benefits all sources):**
- **[shipped]** Accept a leading footnote digit on inline P/A/C markers (`^\s*[PAC] (?=[a-zA-Z\d])`),
  so `"P 1giripai"` is detected and the footnote stripped. Low-risk, gated by the existing inline-marker
  probe. Reclassifies ~30 krithis from *falsely* `resplit-to-template` to *honestly* `template-undercount`.

**B. Govindan adapter (domain-gated; falls back to the generic parser on any mismatch):**
- **Bounding:** strip the HK preamble (top) and cut the lyric at `Gist` / `Word-by-word Meaning`
  (bottom) using the universal structural/textual markers — deterministically excluding metadata and,
  critically, the **Tamil translation trailer**.
- **`<sup>` handling — script-aware:** strip superscript footnote references in the **Latin** variant
  only; preserve superscript pronunciation digits in Indic scripts.
- **Section markers:** prefer the `font-size:180%` span signal when present; fall back to the generic
  text markers when absent. Never rely on structure alone.
- **Section vocabulary as data:** P/A/C for Tyagaraja/Dikshitar, pallavi + `svara sāhitya` for
  Syama Sastri — a per-composer/section-label table, not branching code.

**C. Explicitly NOT decided here (follow-up, separate change):**
- **Template repair** — correcting an under-counted canonical template (e.g. adding the dropped
  pallavi) mutates the publish contract and re-maps the `en` variant's existing sections. It is the
  path that actually reclaims the ~175 template-undercount variants, but it needs its own design and
  (for non-obvious cases) `carnatic-musicologist` review. Tracked as follow-up, not authorised by this ADR.

### Why an adapter and not more generic heuristics

The generic parser fights a text-vs-structure battle it cannot fully win: `font-180` spans, `<sup>`
semantics, and the HK preamble are *markup* facts that text normalization discards. Encoding them once,
behind a domain gate, is more robust and more testable than piling source-shaped regexes into the shared
parser where they risk regressing other sources.

## Fallback strategy (non-negotiable)

The adapter is **advisory**. It runs only when the source domain matches the Govindan family, and its
output is accepted only if it is self-consistent (e.g. all variants agree on one section structure —
already true for 30/30 template-undercount krithis, a strong cross-validation signal). On any mismatch,
low confidence, or an unrecognised page shape, the pipeline **falls back to the current generic parser**.
A template-change is never emitted by the extractor; it is a separate, reviewed action (C above). The
generic path remains the default for every non-Govindan source.

## Testing strategy

- **Golden fixtures (20–30 pages)** snapshotting the format variants: with and without `font-180`
  markers; Tamil-with-trailer; multi-charanam; Syama Sastri `svara sāhitya`; a page with heavy `<sup>`
  footnotes. Each asserts the full ordered section set per variant and that the trailer/preamble are
  excluded. Reuse the CAT-B fixture pattern (`tests/test_catb_truncation_regression.py`).
- **Fallback tests:** a non-Govindan page and a deliberately malformed Govindan page must both route to
  the generic parser and not raise.
- **Regression guard:** the existing 211-test worker suite must stay green; the adapter adds to it.
- **Whole-corpus dry-run:** `scripts/section_triage_batch.py` before/after, expecting
  `template-undercount` → resolvable and Tamil `mixed`/trailer counts to fall, with **zero** content-loss
  or trailer writes (enforced by `scripts/section_repair.py`'s drop/trailer/content gates).

## Consequences

**Positive**
- Targets the exact residual failure modes; ~175 variants become *safely* template-repairable and
  ~89 Tamil variants become splittable once bounded — together the bulk of the remaining 339.
- Makes future Govindan-source imports correct by construction (the dominant ingestion source).
- Keeps source-coupling contained: one adapter, one shared core, hard fallback.

**Negative / risks**
- Adds a second extraction path (maintenance surface). Mitigated by the fallback and golden fixtures.
- Zero benefit to non-Govindan sources; the learnings do not generalise. Accepted, because this
  house-style is a disproportionate share of the corpus — hence the domain gate.
- Source drift breaks the adapter. Mitigated by loud-failing fixtures + fallback to generic.
- The `font-180` signal is **not** universal; a naive "parse the 180% spans" design would regress the
  plainer pages. The decision explicitly requires structural-with-textual-fallback.

## Measured impact of the shipped generic fix (Part A)

| Metric | Before marker fix | After marker fix |
|---|---:|---:|
| `resplit-to-template` (auto-fixable) | 138 | 117 |
| `template-undercount` (needs template repair) | 9 | 30 |
| Direct new auto-fixes from the marker fix alone | — | **0** |
| Template-undercount variants now cleanly reclaimable (all 6 scripts agree) | — | **175** |

The marker fix reclaims **nothing on its own** — deliberately. Its value is diagnostic honesty: it stops
30 krithis from *masquerading* as safe re-splits (the safety gates were already refusing them because
re-splitting would have dropped the pallavi) and reclassifies them as `template-undercount`, where all
six script variants independently agree on the corrected structure. That agreement is what will make the
follow-up template repair safe.

## Follow-up

1. ~~**Template-repair tool**~~ **DONE (2026-07-17)** — `scripts/template_repair.py` built with the
   clean-prepend + cross-validation gates; 28 krithis reviewed by `carnatic-musicologist` (28/28 APPROVE)
   and applied (queue 339 → 231). See `section-issues-cleanup-findings.md` (Pass 2).
2. ~~**Translation-trailer strip**~~ **DONE (2026-07-17)** — the marker/`<sup>` layering (Part A) is not
   needed for this: `strip_refrain_trailer` in `structure_parser.py` locates the lyric→trailer boundary from
   the **pronunciation-digit** signal (lyric lines carry HK digits; the appended Tamil prose does not),
   which is more robust than the `Gist`/`Word-by-word` structural bound this ADR first proposed and needs no
   domain gate. `scripts/tamil_trailer_repair.py` verified the removed tail was prose (parse-twice diff) and
   re-split 119 Tamil variants (queue 231 → 112). See `section-issues-cleanup-findings.md` (Pass 3).
3. **Still open — head-capture preamble stripper.** The Modified-HK transliteration-key chart is captured
   into the Tamil pallavi (section 1). It doesn't affect section counts, so it's not a queue item, but it is
   the remaining cleanup the Govindan adapter (Part B) should own, alongside script-aware `<sup>` footnote
   stripping. Build behind the domain gate with the golden-fixture suite.
4. Update `adr-index.md` and re-run the batch triage to record the queue movement (done through Pass 3).
