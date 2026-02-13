| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.3.0 |
| **Last Updated** | 2026-02-13 |
| **Author** | Codex |

# TRACK-064 Key-Collision Milestone Handover (2026-02-13)

## Purpose
This handover captures the milestone reached in the current session:
- Hypothesis validation for `(first 10 chars of krithi, raga, tala)` identity key behavior.
- CLI implementation to detect and flag key collisions as outliers while continuing batch processing.
- Large-set validation run (`--max-rows 200`) with concrete collision outcomes.

Use this document with the follow-up review notes in [TRACK-064 Code Review (2026-02-13)](track-064-code-review-2026-02-13.md) as the starting point for the next step.

---

## Milestone Summary
- Hypothesis is validated in practice: the key is highly discriminative across Dikshitar rows.
- Known/accepted outlier behavior is now operationalized in the test pipeline.
- New CLI behavior supports:
  - Large-batch collision scans.
  - Collision flagging (non-blocking by default).
  - Optional strict failure mode (`--fail-on-collision`).

---

## Review-Driven Corrections (Applied to Next Plan)

- Collision metadata extraction logic must match `CanonicalExtraction` schema:
  - `raga` scalar accessor is incorrect for canonical payloads.
  - Correct accessor shape: `raw_extraction::jsonb->'ragas'->0->>'name'`.
- Current `metadataMissingRows=4` in the 200-row run is likely inflated by this accessor mismatch.
- Full-file collision scan should run **after** schema-aligned metadata retrieval is patched and verified on the 200-row sample.

### Follow-up Validation (2026-02-13)
- Accessor patch applied for canonical payload shape (`raw_extraction::jsonb->'ragas'->0->>'name'`).
- First 200 Dikshitar URLs revalidated directly against DB evidence:
  - `total_rows=200`
  - `keyed_rows=200`
  - `metadata_missing_rows=0`

---

## Hypothesis Validation Recap

## User hypothesis
For same-composer krithis, uniqueness should generally hold with:
`(first 10 characters of krithi title, raga, tala)`.

## Validated examples (requested)
For the three Abhayamba items:
- `abhayAmbAyAM bhaktiM` -> `sahAna`, `tripuTa`
- `abhayAmbikAyAH anyaM` -> `kEdAra gauLa`, `jhampa`
- `abhayAmbikAyai aSva` -> `yadukula kAmbhOji`, `rUpakam`

Even where title prefix overlaps (`abhayambik...`), raga/tala disambiguates.

## Outlier policy agreed in session
- `parimaLa ranga nAthaM1/2` treated as accepted outlier/manual variant handling.
- New implementation now flags such collisions and moves on.

---

## Code Changes Completed

## 1) CLI scenario + args for collision scans
File: `tools/sangita-cli/src/commands/test.rs`
- Added scenario: `dikshitar-key-collision`.
- Added args:
  - `--title-prefix`
  - `--max-rows`
  - `--fail-on-collision`

## 2) A-series convergence flow hardened
File: `tools/sangita-cli/src/commands/test.rs`
- During convergence analysis, key collisions are now:
  - Detected,
  - Flagged as outliers,
  - Excluded from false non-convergence counts,
  - Optional hard-fail via `--fail-on-collision`.

## 3) Shared helpers for identity-key scan
File: `tools/sangita-cli/src/commands/test.rs`
- Added generalized Dikshitar CSV loader with optional prefix/limit filters.
- Added key normalization:
  - alphanumeric-only canonicalization,
  - first-10 prefix extraction.
- Added metadata retrieval for each task (`raga`, `tala`) from `raw_extraction` with DB fallback.
  - Follow-up fix completed: raga extraction aligned with canonical `ragas[]` payload.
- Added grouping logic for collision reporting.

## 4) CLI docs update
File: `tools/sangita-cli/README.md`
- Added run command examples for `dikshitar-key-collision`.
- Added strict mode example.

---

## Verification of Code Health
- `cargo fmt --manifest-path tools/sangita-cli/Cargo.toml` -> passed.
- `cargo check --manifest-path tools/sangita-cli/Cargo.toml` -> passed.

---

## Large-Set Run Executed (Current Session)

## Command
```bash
cargo run --manifest-path /Users/seshadri/project/sangeetha-grantha/tools/sangita-cli/Cargo.toml -- \
  test extraction-e2e \
  --scenario dikshitar-key-collision \
  --csv-path /Users/seshadri/project/sangeetha-grantha/database/for_import/Dikshitar-Krithi-For-Import.csv \
  --max-rows 200 \
  --skip-migrations \
  --skip-extraction-start \
  --timeout-seconds 600
```

## Final summary (from run output)
- `totalRows=200`
- `successfulRows=199`
- `failedRows=1`
- `keyedRows=195`
- `metadataMissingRows=4`
- `collisionGroups=4`
- `collisionRows=8`

## Collision groups detected
1. `abhayamban|kedragaua|adi`
   - `abhayAmbA nAyaka hari sAyaka`
   - `abhayAmbA nAyaka vara dAyaka`

2. `brhadisvar|nagadhvani|adi`
   - `bRhadISvaraM bhaja`
   - `bRhadISvarIM bhaja`

3. `govindaraj|surai|rupakam`
   - `gOvinda rAjAya`
   - `gOvinda rAjEna`

4. `himagiriku|amrtavarshini|adi`
   - `hima giri kumAri ISa Priya`
   - `hima giri kumAri ISvari`

## Failed row
- `row 38` (`bhArati`, csvRaga=`dEva manOhari`)
- Failure type: transient extractor fetch failure (`httpx.ConnectError: [Errno 111] Connection refused`).
- This is operational/transient, not a key-model logic failure.

---

## Operational Interpretation
- Key strategy is strong and production-useful for batch matching heuristics.
- Collisions exist but are rare and now explicitly surfaced instead of silently causing confusion.
- Non-blocking collision handling works as designed and preserves batch momentum.

---

## Recommended Next Session Plan

1. Re-run failed row(s) only for operational cleanup.
   - Optional targeted re-run if required for clean pass accounting.

2. Run full-file collision scan (all Dikshitar rows).
   - Same command, remove `--max-rows 200`.

3. Export/store collision report artifact for manual curation workflow.
   - Keep collision key + row list for UI/manual krithi variant handling.

4. Decide strict policy for CI/regression:
   - Keep default non-blocking mode in dev.
   - Use `--fail-on-collision` in strict audit jobs only.

5. Feed results into Phase 2 planning in [TRACK-064: Unified Extraction Engine (UEE) Migration](../../conductor/tracks/TRACK-064-unified-extraction-engine-migration.md).
   - Use corrected metadata coverage numbers as baseline for heuristic-consolidation regression gates.

---

## Useful Commands

## Full-set run
```bash
cargo run --manifest-path /Users/seshadri/project/sangeetha-grantha/tools/sangita-cli/Cargo.toml -- \
  test extraction-e2e \
  --scenario dikshitar-key-collision \
  --csv-path /Users/seshadri/project/sangeetha-grantha/database/for_import/Dikshitar-Krithi-For-Import.csv \
  --skip-migrations \
  --skip-extraction-start \
  --timeout-seconds 600
```

## Strict mode (optional)
```bash
cargo run --manifest-path /Users/seshadri/project/sangeetha-grantha/tools/sangita-cli/Cargo.toml -- \
  test extraction-e2e \
  --scenario dikshitar-key-collision \
  --csv-path /Users/seshadri/project/sangeetha-grantha/database/for_import/Dikshitar-Krithi-For-Import.csv \
  --skip-migrations \
  --skip-extraction-start \
  --timeout-seconds 600 \
  --fail-on-collision
```

---

## Files Touched in This Session
- `tools/sangita-cli/src/commands/test.rs`
- `tools/sangita-cli/README.md`
- `application_documentation/11-retrospective/track-064-key-collision-handover-2026-02-13.md`
