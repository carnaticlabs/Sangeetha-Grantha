| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-07-18 |
| **Author** | Sangeetha Grantha Team |

# Thyagaraja Krithi Import — Post-Mortem Analysis

---

**Date:** 2026-07-14
**Scope:** Bulk import of 687 Tyagaraja krithis from thyagaraja-vaibhavam.blogspot.com
**Outcome:** 684 approved, 2 mapped, 0 failures — all with correct section labels

---

## 1. Summary

The Tyagaraja bulk import surfaced a chain of issues across the extraction parser,
the import pipeline's idempotency layer, and the extraction queue lifecycle. What began
as a parser gap (230 krithis with `OTHER` sections) required three incremental parser
fixes and multiple manual interventions to work around pipeline design assumptions.

**Root cause:** The thyagaraja-vaibhavam blog uses a compact inline section label format
(`P`, `A`, `C`, `C1`–`C12`) that the structure parser did not recognise. Fixing the parser
was straightforward; getting the fixed parser's output into the database was not.

---

## 2. Timeline

| Step | Action | Result |
|------|--------|--------|
| 1 | Initial bulk import (1374 CSV rows → 687 unique krithis) | 687 `in_review`, 230 with `OTHER` sections |
| 2 | Parser fix #1: add `C\d{1,2}` + context-dependent `P`/`A` patterns | Commit `f6ee394` |
| 3 | CSV re-upload of 230 inline-format krithis | Idempotency check blocked: old rows kept, no re-extraction |
| 4 | Delete 230 old rows, re-upload CSV | Scrape ran, but extraction queue entries still `INGESTED` (not `PENDING`) |
| 5 | Manual reset: `INGESTED → PENDING` on 229 queue entries | Python worker re-extracted; Kotlin worker failed to enrich — `importId` mismatch |
| 6 | Patch `request_payload.importId` + reset `DONE` | Kotlin worker enriched successfully; 133 still had `OTHER` |
| 7 | Parser fix #2: standalone `C ` (no digit) + `C 1` (space before digit) | Commit `4f190ce` — fixed 133→48 |
| 8 | Parser fix #3: allow uppercase HK consonants after `C` label | Commit `ea84319` — fixed 48→3→0 |
| 9 | Final state | 229/229 fixed, 0 `OTHER` sections |

---

## 3. Issues Discovered

### 3.1 Parser: Inline Section Label Format

The thyagaraja-vaibhavam blog uses Harvard-Kyoto (HK) transliteration with inline section
labels that differ from the full-word headers (`Pallavi`, `Anupallavi`, `Charanam`) used
by most sources:

```text
P ataDE dhanyuDurA O manasA
A satata yAna suta dhRtamaina sItA
C1 venuka tIka tana manasu ranjillaga
C2 tumburu vale tana tambura paTTi
```

**Three distinct sub-patterns required incremental fixes:**

| Pattern | Example | Fix Commit |
|---------|---------|------------|
| `C\d{1,2}` + context-dependent `P`/`A` | `C1 venuka`, `P ataDE`, `A satata` | `f6ee394` |
| Standalone `C ` (no digit) + `C 1` (space before digit) | `C caduvulanni`, `C 1 vara giri` | `4f190ce` |
| Uppercase HK consonants after `C` | `C Sruti SAstra` (Ś = `S` in HK) | `ea84319` |

**Design decision — context-dependent activation:** Inline `P`/`A` patterns only fire when
the document also contains a `C` or `C\d+` inline label. This prevents false positives like
`A jagadamba sadA brova rAvu` (a lyric continuation line starting with HK long vowel ā)
being split into a new ANUPALLAVI section in documents that use full-word headers.

### 3.2 Pipeline: CSV Re-Import Does Not Re-Extract

The import pipeline's idempotency check in `ImportService.shouldEnqueueHtmlExtraction()`
returns `false` when a `source_key` already exists:

```kotlin
// ImportService.kt:129-130
private fun shouldEnqueueHtmlExtraction(request, existingImport): Boolean {
    if (existingImport != null) return false  // ← blocks re-extraction
    ...
}
```

**Consequence:** Re-uploading a CSV for the same URLs does not trigger re-extraction.
The scrape batch succeeds (it's counted as processed) but no new extraction queue
entries are created and the old `parsed_payload` persists.

**Workaround:** Delete the `imported_krithis` rows first, then re-upload. This removes
the idempotency match so the pipeline treats them as new imports.

**Recommendation:** Add a `force` flag or a dedicated `/re-extract` flow that:
1. Clears `parsed_payload` on matching import rows
2. Resets extraction queue entries to `PENDING`
3. Patches `request_payload.importId` to point to the current import rows

### 3.3 Pipeline: Extraction Queue `INGESTED` vs `PENDING`

The extraction lifecycle has a non-obvious status flow:

```text
CSV Upload → Scrape → extraction_queue (PENDING)
                           ↓
                    Python worker processes
                           ↓
                    extraction_queue (DONE)
                           ↓
                    Kotlin ExtractionWorker ingests
                           ↓
                    extraction_queue (INGESTED) + imported_krithis.parsed_payload populated
```

When rows are deleted and re-imported, the **old** extraction queue entries remain
with status `INGESTED`. The re-import doesn't create new queue entries (idempotency),
and the existing `INGESTED` entries are never re-queued because the Python worker
only polls `PENDING`.

**Manual intervention required:**
```sql
UPDATE extraction_queue SET status = 'PENDING', attempts = 0 WHERE ...
```

### 3.4 Pipeline: `request_payload.importId` Stale After Row Deletion

The extraction queue's `request_payload` contains the `importId` of the `imported_krithis`
row that triggered the extraction. When the old rows are deleted and new ones created,
the queue entries still reference the deleted UUIDs.

The Kotlin `ExtractionResultProcessor` logs:
```text
WARN Import be9b66d9-... from request_payload not found, skipping enrichment
```

The extraction completes successfully, the queue moves `DONE → INGESTED`, but
`parsed_payload` is never written because the target row doesn't exist.

**Manual intervention required:**
```sql
UPDATE extraction_queue eq
SET request_payload = jsonb_set(eq.request_payload, '{importId}', to_jsonb(ik.id::text))
FROM imported_krithis ik
WHERE eq.source_url = ik.source_key AND ...
```

---

## 4. Key Metrics

| Metric | Value |
|--------|-------|
| Total Tyagaraja imports | 687 (from 1374 CSV rows, deduplicated) |
| Initially correct sections | 457 (66.5%) |
| Required re-extraction | 230 (33.5%) |
| Parser fix iterations | 3 |
| Manual DB interventions | 6 (deletes, queue resets, importId patches) |
| Final section distribution | 2,067 CHARANAM, 628 PALLAVI, 555 ANUPALLAVI, 7 SWARA_SAHITYA |
| Final OTHER count | 0 |

---

## 5. Lessons Learned

### 5.1 Parser Design

1. **HK transliteration is case-significant.** Uppercase letters represent retroflex
   consonants (T, D, N) and long vowels (A, I, U), sibilants (S, Sh). Section label
   patterns must account for uppercase-initial words in HK text, not just lowercase.

2. **Context-dependent patterns prevent false positives.** The inline `P`/`A` patterns
   are ambiguous on their own (`A` is also HK long-ā). Gating them on the presence of
   `C`/`C\d+` labels in the same document is a reliable discrimination strategy.

3. **Blog formats vary within the same source.** Even within thyagaraja-vaibhavam,
   three distinct patterns exist: `C1`/`C2` (numbered), `C ` (standalone), and
   `C 1` (space-separated digit). Test with representative samples from each variant.

### 5.2 Pipeline Architecture

4. **Idempotency blocks re-extraction by design.** The pipeline assumes extraction
   is one-shot. Re-extraction after parser fixes requires manual intervention at
   three points: import rows, extraction queue status, and importId references.

5. **The extraction queue lifecycle has hidden coupling.** The `request_payload.importId`
   creates a tight coupling between extraction queue entries and import rows. Deleting
   import rows breaks this coupling silently (no FK constraint, just a WARN log).

6. **Scrape ≠ Extract ≠ Ingest.** The batch status (`succeeded`) reflects scrape
   completion, not extraction or ingestion. "All succeeded" can mask zero actual
   parsing work if the queue entries aren't in the right state.

### 5.3 Operational

7. **Build a re-extraction script.** The manual cycle of reset → wait → patch → wait
   was error-prone and time-consuming. An automated script (see `tools/re-extract.sh`)
   eliminates this.

8. **Always verify extraction timestamps.** Comparing `extractionTimestamp` between
   fixed and unfixed krithis immediately reveals whether the new parser actually ran.

9. **Check the backend logs, not just DB state.** The `WARN Import ... not found`
   log was the key signal that importId patching was needed — DB queries alone
   showed `INGESTED` status which looked correct.

---

## 6. Recommendations

| Priority | Action | Effort |
|----------|--------|--------|
| P1 | Add `--force` flag to CSV import that clears + re-queues matching source_keys | Medium |
| P1 | Add FK or validation on `request_payload.importId` → `imported_krithis.id` | Low |
| P2 | Surface extraction/ingestion status separately from scrape status in Curator UI | Medium |
| P2 | Add re-extraction API endpoint that handles the full reset cycle | Medium |
| P3 | Add parser coverage for edge cases: multi-script preambles, footnote markers in section labels | Low |

---

## 7. Related Artifacts

- Parser commits: `f6ee394`, `99ab4c0`, `4f190ce`, `ea84319`
- Test file: `tools/krithi-extract-enrich-worker/tests/test_structure_parser.py` (38 tests)
- Re-extraction script: `tools/re-extract.sh`
- Import CSV: `storage/imports/Tyagaraja-Inline-Reextract.csv`
- TRACK-093: Trinity bulk import
