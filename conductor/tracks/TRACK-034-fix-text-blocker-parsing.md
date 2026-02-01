| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-02-01 |
| **Author** | Sangita Grantha Team |

# TRACK-034: Fix TextBlocker Section Parsing

## 1. Objective
Fix issue where `TextBlocker` incorrectly parses section headers that contain additional text or punctuation, such as `(madhyama kAla sAhityam)`. This causes the remainder of the header line (e.g. `sAhityam)`) to be treated as the first line of lyrics.

## 2. Context
- **Issue**: The regex for section headers in `TextBlocker.kt` was too strict and did not account for suffixes like `sahityam` or enclosing parentheses.
- **Impact**: Incorrect lyric lines appearing in imported Krithis (e.g. "sAhityam)" as the first line).
- **Example**: `(madhyama kAla sAhityam)` was split into Header: `MADHYAMAKALA` and Content: `sAhityam)`.

## 3. Implementation Plan
- [x] Analyze `TextBlocker.kt` and identify the regex flaw.
- [x] Reproduce the issue with a standalone script.
- [x] Update `detectSectionHeader` regex to support optional suffixes (e.g. `sahityam`) and handle surrounding punctuation.
- [x] Verify the fix.

## 4. Progress Log
- **2026-02-01**: Identified the issue in `detectSectionHeader`. Created reproduction script. Implemented fix in `TextBlocker.kt` to handle optional `sahityam` suffix and clean up trailing punctuation from the remainder. Verified fix with reproduction script.
- **2026-02-01**: Extended support for single-letter section headers (`P`, `A`, `C`) as used in some Thyagaraja Vaibhavam posts (e.g. `P cakkani...`). Validated against multiple URLs.
