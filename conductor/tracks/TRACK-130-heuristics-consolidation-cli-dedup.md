| Metadata | Value |
|:---|:---|
| **Status** | Not Started |
| **Version** | 1.0.0 |
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
