# Section-Issues Cleanup — Findings & Remediation Log

_Autonomous pass, 2026-07-16; template-repair pass, 2026-07-17. Companion to `flagged-krithis-for-review.md`._

This log records the category-by-category triage of the Curator **Section Issues**
queue, the fixes applied, and the reasoning for what was deliberately deferred. The
guiding rule throughout: **move variants toward publishable (every language variant
matching its English/Latin template's section structure) without ever corrupting or
dropping lyric.** When a fix could not be made template-exact and content-safe, the
krithi was left flagged with a recommendation rather than guessed at.

## Queue movement

| Checkpoint | Section-issue count |
|---|---|
| Start (per `flagged-krithis-for-review.md`) | **484** |
| After CAT-B truncation fix (15 variants / 3 krithis) | 469 |
| After CAT-C re-split (51 variants / 11 krithis) | 418 |
| After gated resplit sweep (79 variants) | **339** |
| After template-repair pass (28 krithis, 2026-07-17) | **231** |
| After Tamil trailer-strip pass (119 variants, 2026-07-17) | **112** |

**Pass 1 (2026-07-16): 469 → 339 (−130 variants).**
**Pass 2 — template repair (2026-07-17): 339 → 231 (−108 variants).**
**Pass 3 — Tamil trailer strip (2026-07-17): 231 → 112 (−119 variants).**
**Whole effort: 484 → 112 (−372).**
All variant writes went through the audit-logged `POST /v1/admin/variants/{id}/sections`
(`UPDATE_LYRIC_VARIANT_SECTIONS`); template changes in pass 2 through
`POST /v1/admin/krithis/{id}/sections` (`UPDATE_KRITHI_SECTIONS`). No raw SQL; verified via
the live Curator API and per-section text inspection, not just row counts.

## Code changes (extraction worker)

1. **Truncation removed** — `worker.py` `_truncate_utf8`/`_truncate_lyric_variants`
   silently byte-capped every lyric section at 1800 bytes. Indic scripts are ~3
   bytes/char, so multi-section blobs were cut mid-composition. Replaced with
   `_filter_empty_lyric_sections` (drops empty sections, preserves full text). The
   stored `text` column is not indexed, so there was never a byte ceiling to defend.
2. **Indic `svara sAhitya` headers** — `structure_parser.py` gained per-script patterns
   (Devanagari/Tamil/Telugu/Kannada/Malayalam) for the inline `svara sAhitya N` label;
   without them CAT-B/CAT-C Syama Sastri Indic variants collapsed into one blob.
3. **Indic P/A/C abbreviation headers** — gated `INLINE_INDIC_PAC_PATTERNS` for the
   thyagaraja-vaibhavam abbreviations `प./అ./చ1.` in all five scripts. Gated (like the
   Latin inline patterns) on a probe so full-word-header documents are unaffected. The
   trailing period is the disambiguator.

4. **Latin P/A/C marker + footnote digit** (pass 2) — `INLINE_PAC_PATTERNS` for `P`
   required a letter immediately after `"P "`, so `"P 1giripai"` (rendered from
   `<span>P</span><sup>1</sup>giripai` on Govindan blogs) was not recognized and the Latin
   pallavi was head-captured — the true root cause of the "template-defect" bucket. The `P`
   lookahead now tolerates the footnote digit (matching `A`/`C`), which is then stripped.
   Locked by `tests/test_latin_footnote_markers.py`. Full worker suite: **213 passed**.

5. **Tamil translation-trailer stripper** (pass 3, ADR-015) — `structure_parser.py` gained
   `strip_refrain_trailer`, which trims the inline word-by-word Tamil *meaning* Govindan
   blogs append after the lyric (marker splitting otherwise leaves it glued to the final
   charanam, 5–18× its siblings). The refrain cue is abbreviated inconsistently across
   sections (`(ப்3ரஹ்மானந்த3)` in the pallavi vs `(ஆ)` in the last charanam), so cue-matching
   is unreliable; instead it uses the **transliteration itself** — every lyric line carries an
   HK pronunciation digit glued to an Indic consonant (`விது4லு`, `த்யாக3ராஜ`), the Tamil prose
   meaning has none — and cuts after the last digit-bearing line, only when ≥2 prose lines
   (≥40 chars) follow. Never touches Latin (no Indic digits) or clean lyric (all-digit lines).
   A `_trailer_strip_enabled` toggle lets the repair tool parse twice (with/without) to
   isolate and verify the removed tail. Locked by `tests/test_tamil_trailer_strip.py`
   (+ `tv_venkatesa_tamil_trailer.html` fixture). Full worker suite: **219 passed**.

Tests: `tests/test_catb_truncation_regression.py` (fixtures for the 3 CAT-B krithis +
a direct no-truncation unit test), `tests/test_indic_pac_abbreviations.py` (per-script
splitting + gating false-positive guards), `tests/test_latin_footnote_markers.py`, and
`tests/test_tamil_trailer_strip.py`.

## Reusable scripts (`tools/krithi-extract-enrich-worker/scripts/`)

- **`section_triage.py`** — for a krithi, re-fetch source (cached), re-run the current
  pipeline, and classify the gap vs the template (`resplit-to-template`,
  `template-undercount`, `parser-undercount`, `mixed`, `no-source`…). Read-only.
- **`section_triage_batch.py`** — run triage over every ID in the flagged markdown;
  emits a per-krithi JSON report + category summary.
- **`section_repair.py`** — re-split siblings via the audit-logged API, but **only**
  through a stack of safety gates (below). Dry-run by default; `--apply` to write.
- **`template_repair.py`** (pass 2) — fix an under-counted canonical template, then re-split
  every variant. Gated to **clean prepends only** (corrected = template with 1–2
  PALLAVI/ANUPALLAVI sections prepended, remainder identical), requiring **all six script
  variants to independently agree** on the corrected structure, plus the per-variant trailer +
  content gates. `saveSections` upserts by `order_index`, so the template change relabels
  rows in place; the immediate per-variant re-split puts the right lyric under each new label.
  A trailer-laden Tamil variant is deferred (its blob stays under the relabeled section —
  still flagged, never corrupted). Dry-run by default.
- **`tamil_trailer_repair.py`** (pass 3) — re-split variants whose last section carried an
  inline translation trailer, the one case `section_repair`'s `new/old ≥ 97%` content gate
  cannot pass (the re-split is deliberately shorter). Parses the source **twice** (trailer
  stripped vs `_trailer_strip_enabled = False`) to isolate the removed tail, and writes only
  when that tail is prose — ≥40 chars, **no pronunciation digits** (so it can never cut lyric)
  — plus a ≥20-char shared-run sanity vs the stored blob. Only touches the target variant
  (default `ta`), one atomic API call each — no relabel/partial-corruption exposure. Dry-run by default.

All three repair scripts **throttle to ~57 req/min and retry on HTTP 429** — the backend enforces
a global 100 req/min limit (all endpoints, per host), and a bare run fires ~170 requests.

### Safety gates in `section_repair.py`

A variant is re-split only if **all** hold:
1. krithi classified `resplit-to-template` (parser reproduces the template for every variant);
2. parsed section **types and count** equal the template exactly;
3. the variant is actually broken (stored count ≠ template) — already-correct siblings are left alone;
4. **drop-detection**: the stored blob's own inline markers do not exceed the template
   (more markers ⇒ canonical mapping would silently discard a section, e.g. a pallavi the template omits);
5. **trailer guard**: the last section is ≤ 2× the en template's last section (a larger one is an inline translation trailer, not lyric);
6. **content preservation**: the re-split keeps ≥ 97% of the stored blob's characters.

Gates 4–6 were added after direct evidence: the count/type check alone would have
silently dropped the pallavi of `Emaanaticchevo` (kept only 78% of the blob) and would
have written the 18×-oversized Tamil translation trailer of `kanna talli`.

## Per-category findings

### CAT-B — swara-sahitya source TRUNCATED · **FIXED** (3 krithis, 15 variants)
Root cause: the 1800-byte truncation + no Indic `svara sAhitya` recognition. Re-extracted
and re-split all three (`kAmAkshi anudinamu`, `kAmAkshi nI pada`, `rAvE hima giri kumAri`);
source snapshots added as regression fixtures.

### CAT-C — Partially split · **MOSTLY FIXED** (11 krithis, 51 variants)
Root cause: Syama Sastri Indic variants partially split during import (missing the final
`svara sAhitya`/charanam split). The corrected parser reproduces the template exactly for
all variants; re-split via API. 1 variant deferred (Tamil trailer).

### Fewer inline markers than the template expects · **LARGELY FIXED** (~19 krithis, 74 variants)
Root cause: Tyagaraja Indic variants stored as one blob because the inline `प./అ.` P/A/C
abbreviation markers weren't recognized. With the new gated patterns they split to the
(correct) template. Remaining 18 deferred are **Tamil** variants (translation trailer).

### Template defect — blob has MORE sections than the template · **LARGELY FIXED (pass 2)** (28 krithis, 141 re-splits)
Root cause confirmed: the **Latin template under-counts** because the pallavi's `"P 1…"`
marker (P + footnote digit) was never recognized (see code change #4), so it was
head-captured and dropped at import — leaving no PALLAVI slot while the Indic blobs carry a
real pallavi. **Remedy applied:** the fixed parser now recovers the pallavi, and for each
krithi all six script variants independently agree on the corrected structure. `template_repair.py`
rewrote the canonical template (prepend the recovered PALLAVI/ANUPALLAVI) and re-split the
variants. **All 28 candidates were reviewed by the `carnatic-musicologist` agent: 28/28
APPROVE, 0 FLAG** — every recovered pallavi echoes its title; the five anupallavi-less
results (`Bhaja Ramam`, `Brocevaarevarae`, `Dharmaatma`, `Raamuni Maravakavae`,
`Sri Rama Sri Rama Sri Manoharamaa`) confirmed as correct divya-nāma / utsava-sampradāya
kīrtanas. 2 krithis (`Sri Narada Muni`, `Sri Rama Jaya Rama`) deferred by the en-content
gate (>3% loss on re-split — footnote-heavy). Non-blocking musicological note: `Mahita
Pravrddha` (Śākta Sanskrit kṛti) flagged for an attribution/deity check unrelated to the
structure fix.

> **Rate-limit incident (2026-07-17) — resolved, no data loss.** The first `--apply` ran the
> pre-throttle script and stalled at the 100 req/min limit after ~17 krithis, leaving 16 fully
> applied, 1 partial (`Oka Maata`: template changed, `te/kn/ml` not yet re-split), and 13
> untouched. Because `template_repair` does template-change-then-resplit, no variant ever lost
> lyric — unsplit variants held their full text as a blob under the relabeled section.
> Verified by per-section text inspection (incl. `Raamuni Maravakavae` Tamil, whose charanams
> stayed aligned with `en` — the relabel *corrected* its section-1 to PALLAVI). Added throttle
> + 429-retry to both repair scripts; a fresh triage (`triage_report4.json`) drove the completion
> run (`section_repair` finished `Oka Maata`; `template_repair` did the remaining 11). Final
> reconciliation: 28 `UPDATE_KRITHI_SECTIONS` + 141 `UPDATE_LYRIC_VARIANT_SECTIONS`, matching the
> dry-run plan exactly. See [ADR-015](application_documentation/02-architecture/decisions/ADR-015-govindan-extraction-adapter.md).

### Tamil — no detectable refrain cue / translation trailer · **LARGELY FIXED (pass 3)** (119 variants)
Root cause confirmed (`kanna talli`, `Venkatesa`): the thyagaraja-vaibhavam Tamil variants
append an inline **word-by-word Tamil translation** after the lyric. Splitting by markers kept
the trailer in the last section (5–18× the en last section). **Remedy applied:** the
`strip_refrain_trailer` parser step (code change #5) cuts the trailer using the
pronunciation-digit signal, and `tamil_trailer_repair.py` re-split 119 Tamil variants after
verifying — via a parse-twice diff — that the removed tail was prose, never lyric. Spot-checked
across the size range (`gati nIvani` last 255 ch + 1503 ch prose dropped; `kanna talli`,
`nIvu brOva valenamma`, `sundari ninnu`, `vidulaku mrokkeda`): every stripped last section
ends exactly at its refrain cue and carries lyric digits; every dropped tail is pure Tamil
prose. **Remaining Tamil deferrals**: variants with no pronunciation digits or no recurring
refrain (the stripper cannot locate a safe boundary) — correctly left flagged.

### Head-capture — pallavi has no inline marker · **DEFERRED** (~10 variants)
These overlap the Tamil-trailer and template-undercount cases; deferred for the same
reasons (trailer + missing-pallavi). **Remedy:** preamble/trailer stripper + template fix.

## Open questions / recommendations for the next pass

1. ~~**Template-undercount.**~~ **DONE (pass 2)** — `template_repair.py` built, musicologist-reviewed,
   applied to 28 krithis (339 → 231). 2 en-content-loss krithis (`Sri Narada Muni`,
   `Sri Rama Jaya Rama`) remain: investigate why their en re-split loses >3% (likely footnote
   stripping or a genuine structural quirk) before including them.
2. ~~**Tamil translation-trailer stripper.**~~ **DONE (pass 3)** — `strip_refrain_trailer` +
   `tamil_trailer_repair.py` built and applied to 119 Tamil variants (231 → 112). This is the
   ADR-015 trailer-stripper follow-up. Remaining Tamil deferrals have no pronunciation-digit /
   refrain signal to locate a boundary.
3. **Head-capture preamble (still open).** The transliteration-key chart (`க,ச,ட,த,ப - 2-…`)
   is captured into the Tamil pallavi (section 1). It doesn't affect section counts (so it's not
   a queue item), but a preamble-stripper would clean these sections — the remaining half of
   the ADR-015 adapter. `Raamuni Maravakavae` Tamil is a related case (one charanam short,
   10 vs 11, preamble in section 1).
4. **`Sri Narada Muni`, `Sri Rama Jaya Rama`** — template-undercount krithis deferred by the
   en-content gate (>3% loss); investigate the footnote-stripping / structural cause.
5. **Trailer-guard threshold (2×).** Chosen from a wide observed gap (clean ≈ 0.7–1.6×,
   polluted ≥ 5×). Revisit if a legitimate long final charanam ever trips it.
6. **Rate limiting.** The global 100 req/min backend limit bites bulk API-driven remediation.
   All three repair scripts now throttle + retry; any future bulk tool should reuse the same `_request` helper.
7. The `.triage-cache/` directory holds downloaded source HTML + `triage_report*.json`;
   it is disposable and should be gitignored (not committed).
