| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-02-01 |
| **Author** | Sangita Grantha Team |

# TextBlocker Parsing Fix Implementation

## 1. Purpose
Fix issues in `TextBlocker` regex logic where section headers with suffixes (e.g., `(madhyama kAla sAhityam)`) or inline headers (e.g., `P cakkani...`) were not being correctly identified, causing headers to leak into lyric content or sections to be missed entirely.

## 2. Changes
- **Refactored `TextBlocker.kt`**:
    - Updated `detectSectionHeader` to support:
        - Optional suffixes like `sahityam` (e.g., `Madhyama Kala Sahityam`).
        - `caranam` spelling (starting with 'c').
        - Surrounding punctuation removal (parentheses, brackets).
        - Single-letter abbreviations (`P`, `A`, `C`) when at the start of a line.
        - Devanagari headers (पल्लवि, अनुपल्लवि, etc.).
    - **Robustness**: Added automatic unescaping of literal `\n` strings before line splitting.
- **Enhanced `ImportService.kt`**: Added sanitization logic to unescape `\n` literals during the review and section-mapping flow, preventing data corruption from reaching the structural tables.
- **Verification**: Validated against multiple blog formats (Thyagaraja Vaibhavam, Syama Krishna Vaibhavam) covering inline headers and complex transliteration. Corrected existing data for krithi `c64243b2` using a focused fix script.

## 3. Commit Reference
This document serves as the `Ref` for the commit.

## 4. Progress
- [x] Identify regex flaws.
- [x] Implement robust header detection.
- [x] Verify against "curve ball" URLs.
