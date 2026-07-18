| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-07-18 |
| **Author** | Sangeetha Grantha Team |

# Prompt: Create TRACK Entries from Extraction Investigation Report

---

> **Purpose**: Feed this prompt to Gemini 3.1 Pro or Claude Opus to generate detailed TRACK files for fixing the issues identified in the extraction investigation report.
> **Output**: Markdown TRACK files ready to be saved into `conductor/tracks/`

---

## Role & Context

You are the technical architect for **Sangeetha Grantha**, a Carnatic classical music composition (Krithi) database, a Principal Software Architect with deep expertise in Python text extraction pipelines, heuristic parsers, and Carnatic musicology. You strictly adhere to the project's Conductor Track workflow and clean architecture principles. The project uses a multi-stage extraction pipeline: CSV upload → Kotlin backend orchestration → Python extraction worker (`StructureParser`) → PostgreSQL storage.

An investigation report has been completed that identifies critical and secondary bugs in the `StructureParser` — the component that parses blog HTML into structured lyric sections across multiple Indic scripts (Devanagari, Tamil, Telugu, Kannada, Malayalam).

## Your Task

1. Read the investigation report at `database/for_import/EXTRACTION-INVESTIGATION-REPORT.md`
2. Read the current parser implementation at `tools/krithi-extract-enrich-worker/src/structure_parser.py`
3. Read related source files as needed (the HTML extractor, test files, existing extraction worker code)
4. Cross-reference the report's claims against the actual code to verify accuracy and identify any additional issues the report may have missed
5. Create **separate TRACK files** for each logical fix — group tightly related issues together but keep independent concerns in their own tracks

## TRACK File Requirements

### Format

Each TRACK file must follow the project's established format. Use this template:

```markdown
| Metadata | Value |
|:---|:---|
| **Status** | Not Started |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-03-20 |
| **Author** | Sangeetha Grantha Team |

# Track: [Descriptive Title]

**ID:** TRACK-[NNN]
**Status:** Not Started
**Owner:** Sangita Grantha Architect
**Created:** 2026-03-20
**Updated:** 2026-03-20
**Parent:** TRACK-093 (Trinity Krithi Import)

## Goal

[One clear paragraph: what this track accomplishes and why it matters. Reference the investigation report finding.]

## Context

- **Investigation Report:** `database/for_import/EXTRACTION-INVESTIGATION-REPORT.md`
- **Primary File(s):** [exact file paths]
- **Severity:** Critical / Secondary / Minor
- **Affected Blogs:** [which of the 3 Trinity blogs are affected]
- **Current Behavior:** [what happens now — be specific with code references]
- **Expected Behavior:** [what should happen — be specific with expected output]

## Analysis

[Your independent verification of the report's finding. Include:
- The exact code path (file, method, line numbers) that causes the issue
- Why the current logic fails
- Any edge cases or interactions with other components
- Whether the report's characterization is accurate or needs amendment]

## Implementation Plan

### Phase 1: [Name]
- [ ] Step with specific file and method references
- [ ] Step with specific file and method references

### Phase 2: [Name]
- [ ] ...

### Phase 3: Testing & Validation
- [ ] Unit tests: [describe what to test]
- [ ] Integration test against actual blog HTML from all 3 sources
- [ ] Regression: verify existing passing extractions still work
- [ ] Validate on 60-krithi test CSV before full 1,240-krithi run

## Design Considerations

[Trade-offs, alternative approaches considered, risks. Reference the report's "Option A" vs "Option B" where applicable. State a recommendation with reasoning.]

## Acceptance Criteria

- [ ] [Specific, testable criterion]
- [ ] [Specific, testable criterion]
- [ ] All existing tests pass
- [ ] Validated against real HTML from all 3 blog sources

## Dependencies

- **Depends on:** [other TRACKs that must complete first, if any]
- **Blocks:** [other TRACKs waiting on this, if any]

## Files to Modify

- `path/to/file.py` — [what changes]
```

### Numbering

Start numbering from **TRACK-100**. Suggested grouping:

| TRACK ID | Scope |
|:---|:---|
| TRACK-100 | **Critical**: Multi-pass parsing architecture (Indic script extraction) |
| TRACK-101+ | Secondary and minor issues (one TRACK per independent fix) |

### Depth Expectations

For each TRACK, you must:

- **Verify the report against the code** — don't just paraphrase the report. Read `structure_parser.py` and confirm the exact lines, method names, and logic flow. Correct the report if it's wrong.
- **Trace the full code path** — from the entry point (`parse()`) through the helper methods to where the bug manifests. Name every method in the chain.
- **Show concrete examples** — include before/after text snippets showing what the parser currently produces vs. what it should produce.
- **Specify test scenarios** — for each fix, list the specific blog URLs or test fixtures and what the expected output should be (number of variants, section labels, clean text).
- **Consider interactions** — if fixing issue A changes behavior that issue B depends on, note the dependency explicitly.

### What NOT to Do

- Do not create a single mega-TRACK for everything — split by logical concern
- Do not copy the report verbatim — synthesize it with your own code analysis
- Do not propose changes without verifying the current code actually behaves as described
- Do not skip the testing phase in any implementation plan
- Do not forget to update `conductor/tracks.md` registry with the new entries

## Reference Files to Read

These are the key files you should examine before writing the TRACKs:

```text
# Primary — the parser under investigation
tools/krithi-extract-enrich-worker/src/structure_parser.py

# Supporting extraction code
tools/krithi-extract-enrich-worker/src/html_text_extractor.py
tools/krithi-extract-enrich-worker/src/extraction_worker.py

# Test files (for understanding existing test coverage)
tools/krithi-extract-enrich-worker/tests/

# The investigation report itself
database/for_import/EXTRACTION-INVESTIGATION-REPORT.md

# Existing related track (for context on the import pipeline)
conductor/tracks/TRACK-093-trinity-krithi-import.md

# Track registry (must be updated with new entries)
conductor/tracks.md
```

## Output

Produce the following deliverables:

1. **Individual TRACK files** — one `.md` file per track, named `TRACK-NNN-descriptive-slug.md`
2. **Updated rows for `conductor/tracks.md`** — the registry entries to append
3. **A brief dependency graph** — showing the order in which tracks should be tackled (which blocks which)
