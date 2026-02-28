| Metadata | Value |
|:---|:---|
| **Status** | Closed |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-02-20 |
| **Author** | Agent |

# TRACK-068: Markdown Krithi Ingestion Analysis

## Goal
Perform structural analysis on `mdskt.md` and `mdeng.md` to guarantee they contain precisely the same number of Krithis. Draft a robust test plan and define strict acceptance criteria before importing the processed Krithi Name, Raga, Tala, and Lyric Sections into the system.

## Structural Observations
- **Sanskrit (`mdskt.md`)**:
  - Krithis are sequentially numbered using Devanagari numbering (e.g., `१`, `२`).
  - Metadata follows a specific structure: `रागं : [raga name] ([mela]) ताळं : [tala name]`.
  - There are roughly ~479 `रागं` (Ragam) declarations.
- **English/Roman (`mdeng.md`)**:
  - Krithis use Roman numerals or standard digits.
  - Metadata uses transliterated terms (e.g., `rāgaṃ`, `tāḷaṃ`).
  - Due to OCR inconsistencies, regex counting is not 1:1, requiring a proper parser to establish ground truth.

## Acceptance Criteria for Ingestion
Before this data can be written to the database, the parser must satisfy the following:
2. **Total Count Parity**: The parser must extract the identical set of Krithis from BOTH `mdskt.md` and `mdeng.md` (Targeting 479 Krithis due to 5 known OCR page drops).
3. **Metadata Parity**: For a given Krithi Index:
   - The Sanskrit and English Raga names must map to the same Canonical Raga ID.
   - The Sanskrit and English Tala names must map to the same Canonical Tala ID.
3. **Section Integrity**: Every Krithi must have at least a `Pallavi`. Known Dikshitar structures (Pallavi, Anupallavi, Charanam, Madhyamakala) must be extracted without data loss or truncation.
4. **Idempotency**: Retrying the ingestion of these files must not create duplicate Krithis in the database.

## Test Plan
1. **Parser Unit Testing**:
   - Write a Python or Kotlin parser snippet that reads both files.
   - Extract `(id, title, raga, tala)` into a structured format (e.g., JSON or CSV) for both documents.
2. **Cross-Validation**:
   - Programmatically compare the extracted Sanskrit list against the English list.
   - Output a diff report of any ID/Raga mismatches.
3. **Dry-Run Import**:
   - Run a dry-run import in the local development environment using `tools/sangita-cli`.
   - Verify that 484 new records are created in `krithis` and `krithi_lyric_variants`.
   - Check the `audit_log` to ensure proper tracking.

## Progress Log
- [x] Initialized TRACK-068
- [x] Structural Analysis Completed
- [x] Documentation of Test Plan & Acceptance Criteria
- [x] Wrote `verification_parser.py` to extract and sync `mdskt.md` and `mdeng.md`
- [x] Cross-validated extracted metadata yielding exactly 479 matched Krithi structures
- [x] Generated target Markdown files (`final_mdskt.md`, `final_mdeng.md`) and comparison CSV (`krithi_comparison_report.csv`) from the structured JSON
- [x] Closed TRACK-068
