| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-02-20 |
| **Author** | Agent |

# TRACK-067: Sarvam API English PDF Extraction

## Goal
Extract Muthuswami Dikshitar krithis from `database/for_import/mdeng.pdf` using the Sarvam Document Intelligence API into a markdown format (`mdeng.md`), using `en-IN` as the target language.

## Implementation Plan
1. Adapt the existing `test_sarvam.py` to use `mdeng.pdf` as the input.
2. Change the language target to `en-IN`.
3. Submit the document job to Sarvam, polling for completion.
4. If necessary, reuse the image/math hallucination scrubber on the English markdown.

## Progress Log
- [x] Initialized TRACK-067
- [x] Modified `test_sarvam.py`
- [x] Ran Sarvam extraction
- [x] Scrubbed/Verified `mdeng.md`
