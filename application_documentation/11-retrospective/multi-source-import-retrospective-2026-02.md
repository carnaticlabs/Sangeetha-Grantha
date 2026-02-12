# Multi-Source Import Retrospective

| Metadata | Value |
|:---|:---|
| **Status** | Final |
| **Date** | 2026-02-12 |
| **Feature** | Multi-Source Import Pipeline (TRACK-039 through TRACK-063) |
| **Author** | Claude Opus 4.6 (requested by Seshadri) |
| **Scope** | What went wrong, root causes, prompting lessons, recommendations |

---

## 1. Executive Summary

The multi-source import feature — spanning 25 conductor tracks (TRACK-039 through TRACK-063), 94 new files, 50 modified files, 8 database migrations, a Python PDF extraction service, and 9 new UI screens — stalled over 3-4 days of remediation after a catastrophic first E2E attempt. The core issue was **building too much at once without validation checkpoints**. The feature went from strategy document to 94 new files in two days, and the first real E2E test revealed that foundational assumptions (title encoding, font detection, normalisation equivalence, code path integration) were all wrong.

This retrospective documents what happened, why, and what to do differently next time.

---

## 2. Timeline Reconstruction

| Date | Activity | Tracks |
|:---|:---|:---|
| Feb 7 | Env var standardisation, dependency upgrades, conductor setup | TRACK-037, 038 |
| Feb 8 | Documentation guardian audit | TRACK-043 |
| Feb 9 | **Big bang**: Sourcing strategy docs + 5 DB migrations + Python PDF extractor + Structural voting engine + 9 UI screens + backend APIs + 15 shared components + ExtractionResultProcessor — all in one day | TRACK-039-041, 044-053 |
| Feb 10 | First E2E attempt fails catastrophically. 7 critical findings documented. Emergency fix tracks created (TRACK-054-063) | TRACK-054-063 |
| Feb 11 | Remediation: garbled text, Sanskrit segmentation, composer dedup, idempotency, transliteration-aware normalisation, test infrastructure | TRACK-059-062 |
| Feb 12 | Still not done. TRACK-063 (final E2E validation) still "Planned". 94 untracked files, 50 modified files, nothing committed. | — |

### Commit Authorship Pattern

| Period | Author Pattern | Outcome |
|:---|:---|:---|
| Jan 27 - Feb 3 | Mix of human and LLM commits, incremental, each validated | Stable, productive |
| Feb 7 - Feb 9 | All commits co-authored by Cursor, large batch commits | Scope explosion |
| Feb 10 - Feb 12 | Emergency remediation, analysis reports | Still incomplete |

The Jan 27 - Feb 3 period was productive and stable: TextBlocker renamed to KrithiStructureParser, Gemini prompt refined, kshetra mapping added — each change tested and committed individually. The Feb 9 "big bang" is where things went off the rails. The contrast between these two periods is the key lesson.

---

## 3. The Numbers Tell the Story

| Metric | Count |
|:---|:---|
| New conductor tracks created | 25 (TRACK-039 through TRACK-063) |
| New untracked Kotlin service files | 23 |
| New untracked frontend files | ~40 |
| New database migrations | 8 (migrations 23-30) |
| Modified existing files | 50 |
| Total untracked files | 94 |
| Analysis/remediation reports generated | 3 |
| Successful E2E validation passes | 0 (TRACK-063 still Planned) |

---

## 4. Root Cause Analysis: Why This Feature Stalled

### 4.1 Scope Explosion — "Strategy Document Driven Development"

The sourcing requirements document (`krithi-data-sourcing/README.md`) is a comprehensive 5-phase strategy covering authority tiers, structural voting, extraction queues, variant matching, source evidence, quality dashboards, and 9 new UI screens. This is easily 3-6 weeks of work for a team, yet it was treated as a single implementation push.

**What went wrong**: The LLM was given (or generated) a comprehensive strategy document and then asked to implement it. LLMs are eager to be thorough — they generated all 9 UI screens, 15 shared components, 6 new repositories, 12 new services, and 8 migrations in a single session. The strategy document became the prompt, and the output was proportional to the input.

**What should have been done**: Slice the feature into the minimum viable pipeline first:
1. Extract from ONE PDF -> create Krithis in the DB (no UI, no voting, no tiers)
2. Verify it works end-to-end with real data
3. Add the second source and deduplication
4. Only THEN build UI, voting, evidence tracking

### 4.2 No Vertical Slice — Horizontal Layer Cake Instead

The work was organised horizontally:
- Day 1: All migrations, all backend services, all UI screens, all components
- Day 2: First E2E attempt — everything breaks

This is the classic "integration hell" pattern. Every layer was built in isolation against assumed contracts, and the first real integration revealed that assumptions were wrong everywhere:

- Python extractor didn't normalise titles
- Kotlin normaliser didn't handle transliteration variants
- Bold detection didn't work for Devanagari fonts
- Bulk import path didn't know about the new source evidence tables
- Composer name mappings were incomplete

**What should have been done**: Build ONE vertical slice through the entire stack:
```
mdeng.pdf page 17 -> Python extract -> queue -> Kotlin process -> 1 Krithi in DB
```
Get that working. Then expand to all pages. Then add the second source. Each step validates the contract between layers.

### 4.3 The "Completed" Illusion — Tracks Marked Done Without E2E Proof

The tracks registry shows TRACK-044 through TRACK-062 all marked "Completed" or "Done". But TRACK-063 (the actual E2E validation) is still "Planned". This means every track was completed in isolation — code compiles, unit tests pass — but nobody ran the actual pipeline until Feb 10.

Tracks that "completed" on Feb 9:
- TRACK-044-052: 9 UI screens + 15 shared components — all rendering with mock data
- TRACK-053: Krithi creation service — compiles, never tested against real extraction output
- TRACK-041: Sourcing logic — ExtractionResultProcessor written, never tested against real Python output

**What should have been done**: The acceptance criterion for each track should have included an integration checkpoint, not just "compiles" or "renders with sample props". Even a manual smoke test would have caught the garbled title issue immediately.

### 4.4 Two Parallel Code Paths Never Reconciled

The bulk import path (existing, from TRACK-001 era) and the extraction pipeline (new, TRACK-041+) are completely separate code paths that both create Krithis. This was a known architectural gap but wasn't addressed until it caused 32 duplicate records.

From the analysis report:
> The bulk import path predates the sourcing pipeline (TRACK-041+) and was never updated to write `krithi_source_evidence` records.

**What should have been done**: Before building the new pipeline, the existing bulk import should have been analysed to identify the integration points. A design decision was needed upfront: does the new pipeline replace bulk import, or coexist? The answer (coexist with shared dedup) only emerged after the failure.

### 4.5 Normalisation Was an Iceberg

The analysis report documents a devastating cascade:
```
PDF diacritics garbled
  -> titles stored wrong
    -> normalisation produces different keys
      -> deduplication fails
        -> 707 records instead of ~484
          -> variant matching finds 0
            -> Sanskrit segmentation also broken
              -> only 1 result from 280-page PDF
                -> composer name mapping incomplete
                  -> duplicate composers
```

Each of these is a data domain problem that requires deep understanding of Carnatic music transliteration schemes (IAST, Harvard-Kyoto, ITRANS, Velthuis). The LLM generated plausible-looking normalisation code, but it didn't account for:
- The specific way PyMuPDF renders combining marks from this particular PDF font
- The difference between `s` (IAST after NFD) and `sh` (Harvard-Kyoto) after NFD decomposition
- The fact that Devanagari fonts don't have "Bold" in their font name

**What should have been done**:
- Extract 5 sample pages manually and eyeball the raw text output BEFORE writing any normalisation code
- Create a test fixture with 10-20 known title pairs (PDF garbled -> expected clean -> blogspot equivalent) and write the normaliser against those fixtures
- Test the Python extractor against the ACTUAL PDF files, not synthetic test data

### 4.6 Too Much Documentation Infrastructure, Not Enough Testing

The project has an impressive documentation apparatus — conductor tracks with metadata tables, strategy documents, UI/UX plans, implementation checklists, architecture docs. But look at what's missing:

- No integration test that runs the Python extractor against a real PDF page
- No integration test that verifies Kotlin normalisation matches Python normalisation
- No test fixture with real garbled text samples
- TRACK-014 (Bulk Import Testing & Quality Assurance) is still "Proposed" — never done

The remediation phase (Feb 11) finally created `IntegrationTestBase.kt` and `MigrationRunner.kt` — integration test infrastructure that should have existed before the feature was started.

---

## 5. Prompting & LLM Workflow Issues

### 5.1 Strategy-Sized Prompts Produce Strategy-Sized Output

When you give an LLM a 5-phase strategy document and say "implement this", it will try to implement ALL of it. The LLM doesn't naturally say "let's do phase 1 first and validate". It's a completion engine — bigger input, bigger output.

**Better approach**:
```
"Implement ONLY the minimum path: Python extracts one PDF page ->
result goes to extraction_queue -> Kotlin reads it and creates one
Krithi. No UI. No voting. No evidence tracking yet. Show me the
result in the database."
```

### 5.2 "Build X Service" Instead of "Make X Work"

The prompts appear to have been service-oriented ("build ExtractionResultProcessor", "build VariantMatchingService") rather than outcome-oriented ("make this PDF produce correct Krithis in the database").

Service-oriented prompts produce code that compiles. Outcome-oriented prompts produce code that works with real data.

**Better approach**:
```
"Here is the raw text that PyMuPDF extracts from page 17 of mdeng.pdf:
[paste actual text]. The expected Krithi title is 'Akhilandesvari'.
Write the normalisation that transforms the raw text into the expected
title."
```

### 5.3 No "Stop and Check" Gates

The Feb 9 session appears to have been one long generation session that produced TRACK-044 through TRACK-053 — 10 tracks, ~40 new files, 15 components, 6 repositories — without a human checkpoint in between.

**Better approach**: After every meaningful code generation, run the actual system:
```
1. Generate -> Compile -> Run -> Check output -> Adjust -> Next piece
   NOT
2. Generate everything -> Compile -> Run -> Everything is broken
```

### 5.4 The LLM Was Used as Architect AND Implementer Simultaneously

The same sessions that produced the strategy documents also produced the implementation. This means the LLM was designing the architecture and then immediately implementing its own design without the human validating whether the architecture was right.

**Better approach**: Separate the roles:
- **Session 1**: "Analyse what we need. Propose the simplest architecture. I'll review."
- **Session 2**: "Implement the approach I approved. Stop after each component."

### 5.5 LLM Optimism Bias

The exploration agent's report concluded with:
> "All artifacts are production-ready for the final E2E validation cycle."

This is exactly the kind of LLM optimism that contributed to the problem. The artifacts are NOT production-ready — they've never been run together successfully. When you see statements like "production-ready" or "Completed" from an LLM, treat them as claims that need E2E proof, not facts.

---

## 6. The Seven Critical Findings (Summary)

These were documented in detail in `analysis-extraction-pipeline-failures.md`. Summarised here for completeness:

| # | Finding | Severity | Root Cause | Resolution Track |
|:---|:---|:---|:---|:---|
| 1 | PDF title diacritics garbled | CRITICAL | `normalize_garbled_diacritics()` never called on titles; Rule 8 (consonant+dot) skipped | TRACK-059 |
| 2 | Duplicate composer records | HIGH | "Dikshithar" (with `th`) not in canonical mapping | TRACK-061 |
| 3 | Sanskrit PDF: 1 result instead of 484 | CRITICAL | `is_bold` detection fails for Devanagari fonts | TRACK-060 |
| 4 | Variant matching: 0 matches | CRITICAL | Cascade from #1 + #3 — no valid input to match | TRACK-060 |
| 5 | Blogspot import: 32 duplicates | HIGH | No idempotency guard; race condition on retry | TRACK-062 |
| 6 | Cross-source dedup: 14/480 matches | CRITICAL | Transliteration scheme mismatch (IAST vs HK) | TRACK-061 |
| 7 | Bulk import has no source evidence | HIGH | Separate code path predates sourcing pipeline | TRACK-062 |

---

## 7. Recommendations for Next Feature

### 7.1 One Vertical Slice First, Always

Before any UI, any documentation, any shared components — get data flowing from source to database for ONE real example. Validate with your own eyes.

### 7.2 Real Data Fixtures from Day 1

Extract sample pages from the actual PDFs. Put the actual garbled text in test fixtures. Don't generate synthetic test data — it misses the exact encoding issues that will bite you.

### 7.3 Commit Incrementally

94 untracked files and 50 modified files in a single uncommitted blob means no way to bisect or roll back to a known-good state. Commit after each working increment.

### 7.4 Track Completion = E2E Proof, Not "Compiles"

A track is done when you can demonstrate the outcome with real data, not when the code compiles and unit tests pass.

### 7.5 Smaller Prompts, More Iterations

Instead of "implement the sourcing pipeline", try:
- "Extract text from page 17 of this PDF and show me what comes out"
- "This garbled text needs to become this clean text — write the normaliser"
- "Now write the Kotlin side that reads the Python output and creates a Krithi"
- Each step: run it, verify it, commit it, move on

### 7.6 Address Existing Code Paths Before Building Parallel Ones

The bulk import path should have been analysed and either deprecated or extended BEFORE building the extraction pipeline alongside it.

### 7.7 Cap the UI Until the Backend Works

TRACK-044 through TRACK-052 created 9 screens and 15 components — all rendering mock data. None of this UI was usable until the backend pipeline actually worked. Build UI last, or at least in parallel with backend validation, not before it.

### 7.8 Budget for Domain Complexity

Carnatic music transliteration is a genuinely hard normalisation problem (multiple scripts, multiple romanisation schemes, combining marks, font-specific encoding quirks). This needed a domain-specific spike — extract real data, study the patterns, build normalisation against fixtures — not a generic "add normalisation" task.

### 7.9 Separate Architecture Review from Implementation

Don't let the LLM design and implement in the same session. Review the architecture first, then implement in controlled increments.

### 7.10 Treat LLM "Completed" Claims as Hypotheses

When an LLM marks something as done, verify with a concrete outcome. "Code compiles" and "unit tests pass" are necessary but not sufficient. The question is: "Does real data flow through correctly?"

---

## 8. Closing Out TRACK-063: Recommended Approach

To finally get the E2E validation over the line:

1. **Commit what exists now** in logical chunks (migrations, Python extractor, backend services, frontend, tests) — so you have rollback points
2. **Run `sangita-cli db reset`** to get a clean database
3. **Test the Python extractor alone** against one page of mdeng.pdf — verify the title comes out as clean IAST
4. **Test Kotlin normalisation alone** — feed it one clean IAST title and one blogspot title, verify they produce the same normalised key
5. **Then run the full sequence**: blogspot import -> mdeng.pdf extraction -> mdskt.pdf enrichment
6. **Run the TRACK-063 SQL verification queries** after each step, not just at the end

Each step is a checkpoint. If step 3 fails, you don't waste time on steps 4-6.

---

## 9. Key Takeaway

> The core issue wasn't any single bug — it was building too much at once without validation checkpoints. One working slice first, then expand.

---

*Retrospective conducted 2026-02-12 by Claude Opus 4.6, based on analysis of 25 conductor tracks, 3 analysis reports, git history, and codebase exploration.*
