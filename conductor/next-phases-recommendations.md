# Prioritized Recommendations for Next Phases

| Metadata | Value |
|:---|:---|
| **Date** | 2026-03-08 |
| **Baseline** | Phase 3 complete — 474 krithis, 172 pending, 3 known issues |
| **Skill** | `/Sangeetha-Krithi-Analyser` integrated into lyric section work |

---

## Priority Matrix

| Priority | Phase | Description | Effort | Impact | Dependency |
|:---|:---|:---|:---|:---|:---|
| **P0** | 4A | Fix lyric section inconsistency (Issue #1) | 2 days | Critical — blocks curation | Sangeetha-Krithi-Analyser |
| **P1** | 4B | Fix idempotency key scoping (Issue #2) | 0.5 day | Medium — unblocks 6 krithis | None |
| **P2** | 4C | Curator Review UI | 2 days | High — enables curation workflow | P0, P1 |
| **P3** | 5A | Curate to 500+ krithis | 2 days | High — success criteria | P2 |
| **P4** | 5B | Sanskrit PDF extraction (mdskt.pdf) | 1 day | Medium — more variants | Issue #3 fix |
| **P5** | 6 | Wrap-up, docs, clean git | 1 day | Hygiene | All above |

---

## Phase 4A: Fix Lyric Section Inconsistency (P0 — CRITICAL)

### Problem

435/473 krithis (92%) have inconsistent section counts across their 6 language variants. 40 krithis have variants with zero sections. Root cause: the `DeterministicWebScraper` parses section labels differently per script and mishandles dual-format (continuous + word-division) text.

### Approach

Use the `/Sangeetha-Krithi-Analyser` skill to diagnose specific krithis, then fix the Python structure parser using its domain rules.

---

## Phase 4B: Fix Idempotency Key Scoping (P1)

### Problem

`BulkImportTaskRepository.createTasks()` uses `batchId::sourceUrl` without scoping to job type. Entity resolution phase creates zero tasks because dedup filter sees all URLs as "already existing" from the scrape phase.

---

## Phase 4C: Curator Review UI (P2)

### Problem

172 unmatched PDF extractions sit in `imported_krithis` with no review UI. Curators need a page to approve/reject/reassign matches.

---

## Phase 5A: Curate to 500+ (P3)

### Problem

474 krithis exist but 172 pending imports need review. Approving ~30 of those reaches the 500 target.

---

## Phase 5B: Sanskrit PDF (P4)

### Problem

mdskt.pdf never submitted — blocked by HTTP file serving requirement.

---

## Detailed Session Prompts

---

### Prompt 1: Fix Lyric Section Parsing (Phase 4A)

**Copy-paste into a new Claude Code session:**

```
Read conductor/next-phases-recommendations.md and conductor/status-phase3-completion.md for context.

This is Phase 4A — fixing the critical lyric section inconsistency that affects 92% of krithis.

### Background

435 out of 473 krithis have inconsistent krithi_lyric_sections counts across their 6 language variants. 40 krithis have at least one variant with zero sections. The root cause is in the DeterministicWebScraper and the Python structure_parser.py — they parse section labels differently per script and mishandle dual-format text.

### Step 1: Diagnose with /Sangeetha-Krithi-Analyser

Use the /Sangeetha-Krithi-Analyser skill to analyze 3 representative krithis with known issues:

1. `abhayAmbA nAyaka vara dAyaka` — section counts vary from 2 (te, ml) to 4 (kn)
2. `kAyArOhaNESam` — ranges from 2 to 7 sections across variants
3. Pick one of the 40 krithis with a zero-section variant

For each, run these diagnostic queries via the MCP postgres tool:

```sql
-- Get section details for a specific krithi
SELECT k.title, klv.language, klv.script,
       ks.section_type, ks.order_index, ks.label,
       LEFT(kls.text, 100) AS text_preview
FROM krithis k
JOIN krithi_lyric_variants klv ON klv.krithi_id = k.id
JOIN krithi_sections ks ON ks.krithi_id = k.id
LEFT JOIN krithi_lyric_sections kls ON kls.lyric_variant_id = klv.id AND kls.section_id = ks.id
WHERE k.title ILIKE '%abhayamba%'
ORDER BY klv.language, ks.order_index;
```

Use the Sangeetha-Krithi-Analyser skill rules to determine:
- What the CORRECT section structure should be
- Which variants have parsing errors
- Whether Madhyama Kala Sahitya is incorrectly stored as top-level
- Whether dual-format text created duplicate sections

### Step 2: Fix the Python structure parser

Read and fix `tools/krithi-extract-enrich-worker/src/structure_parser.py` applying these rules from the Sangeetha-Krithi-Analyser:

1. **Madhyama Kala Sahitya is NEVER a top-level section** — attach it to the preceding parent section
2. **Cross-variant section consistency** — use the English/IAST variant as the canonical structure reference and force other variants to match
3. **Dual-format detection** — if two consecutive text blocks under the same script have >90% character overlap (after stripping whitespace), merge them and keep only the word-division format
4. **Section header detection** — use the multi-script detection patterns from the skill (Latin, Devanagari, Tamil, Telugu, Kannada, Malayalam label patterns)

### Step 3: Fix the Kotlin LyricVariantPersistenceService

Read `modules/backend/api/.../services/LyricVariantPersistenceService.kt`.

When persisting sections from a CanonicalExtraction:
- First, create `krithi_sections` from the English/IAST variant (canonical structure)
- Then, for each other variant, map its sections to the canonical sections by type and order
- If a variant has fewer sections than canonical, log a warning but still persist what exists
- If a variant has more sections than canonical (likely dual-format), merge extras

### Step 4: Write a repair migration

Create a new migration using `/new-migration fix_inconsistent_lyric_sections` that:
- Identifies krithis where section counts differ across variants
- For each, keeps the English variant's sections as canonical
- Removes duplicate sections caused by dual-format parsing
- Re-indexes order_index to be sequential with no gaps

### Step 5: Validate

Run the diagnostic queries from the Sangeetha-Krithi-Analyser skill:

```sql
-- After fix: should return zero rows
SELECT k.id, k.title, klv.language, COUNT(kls.id) AS section_count
FROM krithis k
JOIN krithi_lyric_variants klv ON klv.krithi_id = k.id
LEFT JOIN krithi_lyric_sections kls ON kls.lyric_variant_id = klv.id
GROUP BY k.id, k.title, klv.language
HAVING COUNT(kls.id) != (
    SELECT COUNT(ks.id) FROM krithi_sections ks WHERE ks.krithi_id = k.id
)
ORDER BY k.title, klv.language;
```

```sql
-- After fix: should return zero rows
SELECT k.id, k.title, klv.language, klv.script
FROM krithis k
JOIN krithi_lyric_variants klv ON klv.krithi_id = k.id
LEFT JOIN krithi_lyric_sections kls ON kls.lyric_variant_id = klv.id
WHERE kls.id IS NULL
ORDER BY k.title;
```

Run backend tests: ./gradlew :modules:backend:api:test — zero failures.

### Commit

"Fix lyric section inconsistency across language variants

Phase 4A: Applied Sangeetha-Krithi-Analyser rules to fix 92% section
inconsistency rate. MKS treated as sub-section, dual-format text merged,
English/IAST variant used as canonical structure reference.

Ref: application_documentation/04-database/schema.md
Ref: conductor/next-phases-recommendations.md"
```

---

### Prompt 2: Fix Idempotency Key Scoping (Phase 4B)

**Copy-paste into a new Claude Code session:**

```
Read conductor/status-phase3-completion.md Issue #2 for context.

### Problem

BulkImportTaskRepository.createTasks() uses an idempotency key of `batchId::sourceUrl` that is NOT scoped to job type. When the entity_resolution phase tries to create task runs with the same URLs that the scrape phase already used, the dedup filter removes all 482 as "already existing." This left 6 CSV imports stuck in PENDING.

### Fix

Find BulkImportTaskRepository.kt (search in modules/backend/).

Change the idempotency key to include the job type:
- Old: `"$batchId::$sourceUrl"`
- New: `"$batchId::$jobType::$sourceUrl"`

This ensures each job phase (MANIFEST_INGEST, SCRAPE, ENTITY_RESOLUTION) creates its own set of task runs.

### Verify

Run: ./gradlew :modules:backend:api:test — zero failures.

### Commit

"Fix idempotency key scoping in BulkImportTaskRepository

Include jobType in the dedup key so entity resolution tasks are not
filtered as duplicates of scrape tasks. Fixes 6 stuck PENDING imports.

Ref: application_documentation/04-database/schema.md"
```

---

### Prompt 3: Curator Review UI (Phase 4C)

**Copy-paste into a new Claude Code session:**

```
Read conductor/next-phases-recommendations.md and conductor/session-prompts.md (Phase 4 section) for full context.

Phase 4A (lyric section fix) and 4B (idempotency fix) are complete. Now build the curator review UI.

### Backend: Create CuratorRoutes.kt

Location: modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/CuratorRoutes.kt

Endpoints:
- GET /api/v1/curator/pending-matches
  - Returns all records from `imported_krithis` where status = 'PENDING' and import_source = 'PDF Extraction (Unmatched)'
  - Include: raw title, matched krithi title (if any), confidence score, source info
  - Support: ?page=1&size=50, ?min_score=0.5
  - Also include krithis with inconsistent sections (use /Sangeetha-Krithi-Analyser diagnostic queries)

- POST /api/v1/curator/matches/{id}/approve
  - Move from imported_krithis to krithis table, create lyric variants
  - Use /Sangeetha-Krithi-Analyser section detection rules when creating sections for the approved krithi
  - Write to AUDIT_LOG

- POST /api/v1/curator/matches/{id}/reject
  - Set status to REJECTED in imported_krithis
  - Write to AUDIT_LOG

- POST /api/v1/curator/matches/{id}/reassign
  - Body: { "krithiId": "uuid" }
  - Link the extraction to an existing krithi instead
  - Write to AUDIT_LOG

- GET /api/v1/curator/section-issues
  - Returns krithis with inconsistent section counts (the 435 from Phase 4A that may still need manual review)
  - Uses the Sangeetha-Krithi-Analyser diagnostic SQL

- GET /api/v1/curator/stats
  - Counts: total pending, approved today, rejected today, total matched, section issues count

Register routes in existing Ktor routing setup. Follow patterns from other route files.

### Frontend: Create CuratorReviewPage.tsx

Location: modules/frontend/sangita-admin-web/src/pages/CuratorReviewPage.tsx

Two tabs:
1. **Pending Matches** — table of unmatched PDF extractions awaiting review
   - Columns: Extracted Title | Best Match | Score | Source | Actions
   - Score as percentage, color coded (green ≥70, yellow 50-69, red <50)
   - Actions: Approve (green), Reject (red), Reassign (opens krithi search modal)
   - Keyboard shortcuts: A=approve, R=reject, arrow keys navigate

2. **Section Issues** — table of krithis with inconsistent lyric sections
   - Columns: Title | Language | Expected Sections | Actual Sections | Issue Type
   - Issue types: "missing sections", "extra sections (dual-format)", "MKS as top-level"
   - This tab uses the /Sangeetha-Krithi-Analyser validation rules to categorize issues

Filter bar, pagination (50/page), use TanStack Query, existing Tailwind patterns.

### Route Registration

In App.tsx: <Route path="/curator-review" element={<CuratorReviewPage />} />
In sidebar: "Curator Review" link with badge showing pending count.

### Verification

1. make dev
2. Navigate to http://localhost:5001/curator-review
3. Should see 172 pending matches and section issues
4. Approve 5 matches, reject 2 — verify state changes

### Commit

"Add curator review page with section issue tracking

Curator review UI at /curator-review with two tabs:
- Pending matches from PDF extraction (approve/reject/reassign)
- Section inconsistency tracker using Sangeetha-Krithi-Analyser rules
Keyboard shortcuts for fast curation workflow.

Ref: application_documentation/01-requirements/admin-web/prd.md
Ref: conductor/next-phases-recommendations.md"
```

---

### Prompt 4: Curate to 500+ Krithis (Phase 5A)

**Copy-paste into a new Claude Code session:**

```
Read conductor/next-phases-recommendations.md for context. Phase 4 is complete — curator review UI is functional.

### Goal

Get from 474 krithis to 500+ krithis with source evidence.

### Step 1: Review pending imports via curator UI

Start the dev stack: make dev

Navigate to http://localhost:5001/curator-review

The 172 pending imports from PDF extraction are candidates. Many are likely valid krithis that simply didn't fuzzy-match to CSV-imported ones (different transliteration, OCR artifacts).

### Step 2: Use /Sangeetha-Krithi-Analyser for quality checks

For each batch of approvals, use the /Sangeetha-Krithi-Analyser skill to validate:
- Does the krithi have a valid structure (at minimum PALLAVI)?
- Are the section types in the CHECK constraint enum?
- Is the composer/raga/tala metadata plausible for a Dikshitar composition?

Run validation queries:
```sql
-- Verify all approved krithis have at least PALLAVI
SELECT k.title FROM krithis k
WHERE NOT EXISTS (
    SELECT 1 FROM krithi_sections ks
    WHERE ks.krithi_id = k.id AND ks.section_type = 'PALLAVI'
);
```

### Step 3: Bulk approve high-confidence matches

Using the curator API directly for efficiency:
```sql
-- Find high-confidence pending imports (likely safe to approve)
SELECT id, raw_title, metadata->>'confidence' as confidence
FROM imported_krithis
WHERE status = 'PENDING'
  AND import_source = 'PDF Extraction (Unmatched)'
  AND (metadata->>'confidence')::float >= 0.7
ORDER BY (metadata->>'confidence')::float DESC;
```

### Step 4: Checkpoint

```sql
SELECT count(*) FROM krithis;
-- Target: >= 500

SELECT count(*) FROM krithis k
WHERE EXISTS (
    SELECT 1 FROM source_evidence se WHERE se.krithi_id = k.id
);
-- Target: >= 500 with evidence
```

### Step 5: Data quality report

Generate a final data quality summary:
```sql
-- Section consistency check
SELECT
    COUNT(DISTINCT k.id) as total_krithis,
    COUNT(DISTINCT CASE WHEN consistent THEN k.id END) as consistent_krithis
FROM krithis k
CROSS JOIN LATERAL (
    SELECT COUNT(DISTINCT cnt) = 1 as consistent
    FROM (
        SELECT klv.language, COUNT(kls.id) as cnt
        FROM krithi_lyric_variants klv
        LEFT JOIN krithi_lyric_sections kls ON kls.lyric_variant_id = klv.id
        WHERE klv.krithi_id = k.id
        GROUP BY klv.language
    ) sub
) consistency_check;
```

### Commit

"Curate to 500+ krithis via curator review workflow

Approved N pending PDF extractions after validation with
Sangeetha-Krithi-Analyser structural rules. All approved krithis
have valid section structure and source evidence.

Ref: conductor/next-phases-recommendations.md"
```

---

### Prompt 5: Sanskrit PDF Extraction (Phase 5B)

**Copy-paste into a new Claude Code session:**

```
Read conductor/status-phase3-completion.md Issue #3 and conductor/next-phases-recommendations.md.

### Problem

The extraction worker uses httpx and only supports HTTP URLs. mdskt.pdf needs to be served over HTTP for the Docker worker to access it.

### Fix

Add a file-path handler to the extraction worker that can read from a mounted volume:

1. In tools/krithi-extract-enrich-worker/src/worker.py:
   - Check if the source URL starts with "file://" or is a bare path
   - If so, read directly from the filesystem (volume-mounted in Docker)
   - If HTTP URL, use existing httpx download

2. In compose.yaml:
   - Add a volume mount for PDF files: `./data/pdfs:/app/pdfs:ro`
   - The extraction worker can then access PDFs at `/app/pdfs/mdskt.pdf`

3. Submit mdskt.pdf for extraction and verify variant matching.

### Step 1: Fix worker file handling

Edit tools/krithi-extract-enrich-worker/src/worker.py to support local file paths.

### Step 2: Update compose.yaml

Add the volume mount to the extraction service.

### Step 3: Place PDF and submit

```bash
mkdir -p data/pdfs
cp /path/to/mdskt.pdf data/pdfs/
```

Submit extraction via SQL or API with source path `/app/pdfs/mdskt.pdf`.

### Step 4: Validate with /Sangeetha-Krithi-Analyser

After extraction, use the skill to validate Sanskrit section structure:
- Sanskrit sections should match English section structure for matched krithis
- Use cross-variant consistency rules from the skill

### Checkpoint

```sql
SELECT count(*) FROM extraction_queue WHERE status = 'COMPLETED' AND source_name = 'mdskt.pdf';
SELECT count(*) FROM source_evidence; -- should increase
```

### Commit

"Add local file path support to extraction worker for PDF ingestion

Extraction worker now supports file:// URIs and bare paths via
volume-mounted directory. Enables mdskt.pdf Sanskrit extraction
without requiring HTTP file server.

Ref: application_documentation/02-architecture/decisions/README.md
Ref: conductor/next-phases-recommendations.md"
```

---

### Prompt 6: Wrap-up and Clean Git (Phase 6)

**Copy-paste into a new Claude Code session:**

```
Read conductor/next-phases-recommendations.md for context. All prior phases are complete.

### Step 1: Update documentation

Update these files to reflect current state:
- CLAUDE.md: Ensure all commands, paths, and patterns are current
- application_documentation/00-onboarding/getting-started.md: Update with make commands
- application_documentation/02-architecture/tech-stack.md: Confirm 3-language stack (Kotlin, Python, TypeScript)
- application_documentation/00-meta/current-versions.md: Verify all versions

### Step 2: Close conductor tracks

Update conductor/tracks.md — all tracks should be Completed, Deferred, or Superseded. No In Progress tracks.

### Step 3: Final data quality report

Run these queries and include results in a status document:

```sql
SELECT count(*) as total_krithis FROM krithis;
SELECT count(*) as krithis_with_evidence FROM krithis k
WHERE EXISTS (SELECT 1 FROM source_evidence se WHERE se.krithi_id = k.id);
SELECT count(*) as total_sections FROM krithi_sections;
SELECT count(*) as total_lyric_sections FROM krithi_lyric_sections;
SELECT count(*) as pending_imports FROM imported_krithis WHERE status = 'PENDING';
```

### Step 4: Clean git status

```bash
git status
```

All files should be tracked and committed. No uncommitted changes.

### Commit

"Phase 6: Final documentation update and clean git state

Updated getting-started, tech-stack, and current-versions docs.
All conductor tracks resolved. Final data quality: N krithis with
evidence, M sections, clean git state.

Ref: application_documentation/00-meta/current-versions.md
Ref: conductor/next-phases-recommendations.md"
```

---

## Summary Timeline

```
Mar 8-9:   Phase 4A — Fix lyric sections (use /Sangeetha-Krithi-Analyser)
Mar 9:     Phase 4B — Fix idempotency key (0.5 day)
Mar 10-11: Phase 4C — Curator Review UI (with section issue tab)
Mar 12-13: Phase 5A — Curate to 500+
Mar 14:    Phase 5B — Sanskrit PDF extraction
Mar 15:    Phase 6  — Wrap-up
```

**The Sangeetha-Krithi-Analyser skill is embedded in Phases 4A, 4C, 5A, and 5B** — it provides the domain rules for section parsing, the diagnostic SQL queries for validation, and the structural knowledge needed for curator review.
