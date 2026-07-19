| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-07-19 |
| **Author** | Sangeetha Grantha Team |

# TRACK-130: Consolidate Matching Heuristics & De-duplicate the CLI Extraction Pipeline

**Goal:** The transliteration-collapse table — matching-critical, and claimed to mirror Kotlin's `TransliterationCollapse` — exists twice in Python with **different contents**: `src/normalizer.py` (`_TRANSLITERATION_COLLAPSE_RULES`, includes `chh`, adds `kh` inline later) and `src/identity_candidates.py` (`normalize_identity_text`, includes `kh` and vowel collapses). Separately, `src/cli.py`'s `extract` command re-implements the PDF pipeline that now lives in `PdfExtractionStrategy` and has already drifted (no ragamalika handling, no diacritic normalization). Consolidate both.

**Origin:** Python best-practices evaluation, 2026-07-19 (item 5). Divergent copies of a matching table across two languages is how identity resolution quietly breaks.

## Definition of Done

* One authoritative transliteration-collapse table lives in `src/heuristics/` (e.g. `transliteration_collapse.py`); both `normalizer.normalize_for_matching` and `identity_candidates.normalize_identity_text` consume it.
* Any intentional per-consumer differences (vowel collapses for identity, `kh` handling) are explicit named rule-sets in that module, not accidental drift — with a comment mapping each set to its Kotlin counterpart.
* A test asserts the two consumers' shared prefix of rules is identical, and pins current normalization outputs for a fixture list of composer/raga/title names so any deliberate change is visible in review.
* `cli.py extract` delegates to `PdfExtractionStrategy` (no-op finalize) instead of re-implementing segmentation/parsing — CLI output gains the worker path's ragamalika + diacritic handling.
* Page-range parsing (`"3-7"` → 0-based tuple) exists exactly once, as a shared helper used by both the CLI and the PDF strategy.
* `Transliterator.detect_script` first-char bias is documented as a known limitation in the module docstring (fix is optional stretch — counting-based majority detection — only if fixture tests prove no regression).

## Inputs

* `@tools/krithi-extract-enrich-worker/src/normalizer.py`
* `@tools/krithi-extract-enrich-worker/src/identity_candidates.py`
* `@tools/krithi-extract-enrich-worker/src/cli.py`
* `@tools/krithi-extract-enrich-worker/src/extraction_strategies.py`
* `@tools/krithi-extract-enrich-worker/src/heuristics/`
* Kotlin `TransliterationCollapse` (backend) — the cross-language mirror to annotate against

## Out of Scope

* Changing any normalization *behaviour* — existing keys must normalize identically (the pinned-fixture test is the proof). If consolidation reveals the two tables' differences produce contradictory keys for real data, stop and record findings; reconciling semantics is a follow-up decision with the Kotlin side.
* Kotlin-side changes.
* CLI UX changes beyond the internal delegation.

## Steps

1. Write the pinning test first: run a fixture list of ~50 real names through both normalizers, snapshot outputs.
2. Extract the shared table + named variant rule-sets into `heuristics/`; point both consumers at it; pinning test must stay green.
3. Add the cross-consumer consistency test.
4. Refactor `cli.py` to instantiate `PdfExtractionStrategy` with `finalize=lambda e, *_: e`; extract the shared page-range helper.
5. Document the `detect_script` limitation; implement counting-based detection only if all regression fixtures pass unchanged.

## Deliverables

* New `src/heuristics/transliteration_collapse.py` (or similar); slimmed `normalizer.py` / `identity_candidates.py`.
* Refactored `cli.py`; shared page-range helper.
* New pinning + consistency tests.

## Verification

* `uv run pytest` fully green — especially `test_normalizer.py`, `test_identity_candidates.py`, CAT-B fixtures.
* CLI smoke: `uv run python -m src.cli extract -i <fixture.pdf> -o /tmp/out.json` produces output including ragamalika/diacritic handling (diff against pre-refactor output; differences must be exactly the known CLI-drift fixes).
* `ruff` / `mypy` clean on touched files.

## Outcome (2026-07-19)

### Pinned first, then consolidated

`tests/fixtures/normalization/pinned_outputs.json` was generated from the
**pre-consolidation** code: 61 real composer/raga/title/tala/deity/temple names
× 8 normalization paths = 488 pinned outputs, enforced by
`tests/test_normalization_pins.py`. Every pin stayed green through the
consolidation, so no matching key moved.

### What the two copies actually differed by

| | `normalizer.py` | `identity_candidates.py` |
|:---|:---|:---|
| `chh` → `c` | yes (before `sh`/`ch`) | **no** |
| `kh` → `k` | after the loop | mid-sequence |
| `gh`/`ph` order | `ph`, `gh` | `gh`, `ph` |
| long vowels (`aa`/`ee`/`oo`/`uu`) | raga branch only | always |

Consequence of the `chh` gap: `"chh"` normalised to `"c"` for matching keys but
to `"ch"` for identity keys — the two answered differently for the same input.
That divergence is now explicit rather than accidental, and each consumer keeps
its existing sequence exactly.

The `gh`/`ph` swap and the `kh` position are *provably* immaterial — the patterns
share no characters, so neither can create or destroy the other. Verified beyond
the pins by brute-forcing 400,000 random strings per variant through the old and
new orderings: **0 mismatches**.

### Kotlin counterpart — finding

There is no live Kotlin collapse table to mirror. `NameNormalizationService`
delegated consonant collapse to Python in its Phase 3 "Simplify and Ship" pass
("transliteration collapse is now handled by Python normalizer"). Kotlin retains
only the long-vowel collapses for ragas, which is the counterpart of
`LONG_VOWEL_COLLAPSE_RULES`. The module docstring records this, so the next
reader does not go hunting for a mirror that no longer exists. **Python is
authoritative for the consonant table.**

### CLI de-duplication — the drift, measured

`cli.py extract` now builds an `ExtractionTask` and delegates to
`PdfExtractionStrategy` with a no-op finalize. Diffed against pre-refactor output
on the real `ragamalika_multi_variant` fixture rendered to PDF:

| | ragas emitted |
|:---|:---|
| Before | `[{"name": "Ragamalika  Talam: Adi", "order": 1}]` |
| After | `SrI`/`Arabhi` (pallavi), `gauri`/`nATa` (anupallavi), `gauLa` (charanam) |

That is the drift the track predicted: the CLI had no ragamalika sub-raga
detection and no `cleanup_raga_tala_name`, so an unparsed metadata blob was being
stored as the raga name. On two simpler synthetic fixtures the before/after
output is byte-identical apart from `extractionTimestamp` — i.e. delegation
changes nothing except where the CLI was actually broken.

Page-range parsing (`"3-7"` → `(2, 6)`) now exists once as
`extraction_strategies.parse_page_range`, used by both callers and covered by a
parametrised test.

### `detect_script` — documented, not changed

The first-character bias is real and easy to trigger in this corpus, because
section headers are romanised:

    detect_script("pallavi श्री विश्व नाथं भजेहम्")  # -> "latin"

Documented in the module and method docstrings. The counting-based fix was **not**
implemented: it would change the `script` label on emitted lyric variants and so
change extraction output, which conflicts with this track's pinned-output remit.
The docstring records what a future fix must re-pin first.

### Verification

`ruff check` 0, `ruff format --check` clean (55 files), `mypy` 0,
`pytest` 292 unit + 18 integration passing (up from 220 unit — 65 normalization
pin/consistency tests and 7 CLI delegation tests added).
