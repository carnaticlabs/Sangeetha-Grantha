# Handoff: Simplify and Ship — March 21 Deadline

| Metadata | Value |
|:---|:---|
| **Created** | 2026-03-07 |
| **Deadline** | 2026-03-21 |
| **Goal** | 500+ curated Krithis with source evidence, stack reduced from 5 to 3 languages |
| **Primary Tool** | Claude Code (orchestrator + Kotlin/frontend work) |
| **Secondary Tools** | Codex (Python extraction/normalization), Antigravity (Docker/infra/migrations) |

---

## Background

Critical analysis revealed the project has 77 conductor tracks, 5 programming languages, ~50K LOC, 219 docs — but the core extraction pipeline (PDF → DB) has never completed E2E validation. Key metrics are unmet: PRD targets 2,000 krithis and 80% entity mapping; actual state is unknown krithis and 2.9% mapping at last measurement.

### Root Causes (from Feb 12 retrospective)
1. Strategy-sized prompts produced strategy-sized output (94 files in 2 days)
2. Horizontal layer cake — all layers built before any integration tested
3. Normalization logic duplicated in Kotlin AND Python (drift caused TRACK-077)
4. Tracks marked "Completed" when code compiled, not when data flowed
5. LLM optimism bias — claims of "production-ready" without E2E proof

### Decisions Made
- **Normalization**: Consolidate ALL transliteration/normalization into Python (single source of truth)
- **Rust CLI**: Replace with Python migration runner + Docker Compose orchestration + Makefile
- **Frontend**: Frozen except one new curator review page
- **TRACK-077**: Superseded by manual curator-assist approach
- **Open tracks**: Close or descope within Phase 0

---

## Phase Plan

| Phase | Days | Dates | Owner | Description |
|:---|:---|:---|:---|:---|
| 0 | 1 | Mar 8 | Claude | Close/descope open tracks |
| 1 | 2 | Mar 9-10 | Codex | Consolidate normalization into Python |
| 2 | 2 | Mar 11-12 | Antigravity + Claude | Replace Rust CLI, Docker Compose rewrite |
| 3 | 3 | Mar 13-15 | Claude (orchestrator) | Prove pipeline E2E with real data |
| 4 | 2 | Mar 16-17 | Claude | Curator review UI (backend + frontend) |
| 5 | 3 | Mar 18-20 | Claude | Curate to 500+ krithis |
| 6 | 1 | Mar 21 | Claude | Wrap-up, docs, clean git |

---

## Tool Assignment: Who Does What

### Claude Code (Primary — Orchestrator)

Claude owns the overall plan, Kotlin backend, frontend, and E2E validation.

**Phase 0 tasks:**
- [ ] Fix `BulkImportRepository.kt` — atomically increment `total_tasks` (50% stall fix)
- [ ] Mark `KrithiStructureParser.kt` as deprecated (Python is canonical)
- [ ] Close TRACK-001 (descope Phase E), TRACK-064 (close), TRACK-065 (defer), TRACK-072 (verify+close), TRACK-077 (supersede)
- [ ] Update `conductor/tracks.md`

**Phase 3 tasks (E2E validation — CRITICAL):**
- [ ] Run `db reset`, seed reference data, import 484 Dikshitar krithis via TRACK-071
- [ ] Submit `mdeng.pdf` to extraction queue, verify Kotlin processes results
- [ ] Submit `mdskt.pdf`, verify variant matching
- [ ] Debug and fix any pipeline issues (coordinate with Codex for Python fixes)
- [ ] **Checkpoint:** ≥400 krithis with source evidence

**Phase 4 tasks (Curator Review UI):**
- [ ] Create `CuratorRoutes.kt` — GET pending matches, POST approve/reject/reassign
- [ ] Create `CuratorReviewPage.tsx` — simple table with approve/reject actions
- [ ] Add route to `App.tsx`, sidebar link
- [ ] All mutations write to AUDIT_LOG

**Phase 5 tasks:**
- [ ] Use curator review UI to approve matches
- [ ] Verify 500+ krithis in DB
- [ ] Run data quality queries

**Phase 6 tasks:**
- [ ] Update CLAUDE.md, getting-started.md, tech-stack.md
- [ ] Commit everything with proper `Ref:` lines
- [ ] Verify `git status` clean

---

### Codex (Python extraction/normalization)

Codex works independently on Python code. No Kotlin, no frontend. All work in `tools/krithi-extract-enrich-worker/`.

**Phase 1 tasks — Consolidate normalizer:**

**Task 1.1: Create `src/normalizer.py`** (standalone, verifiable)
```
Input: Create tools/krithi-extract-enrich-worker/src/normalizer.py

Requirements:
- Single function: normalize_for_matching(text: str, entity_type: str) -> str
- entity_type: "title", "composer", "raga", "tala", "deity", "temple"
- Port these Kotlin TransliterationCollapse rules:
  ksh→ks, chh→c, sh→s, th→t, dh→d, bh→b, ph→p, gh→g, jh→j, ch→c
- Merge existing diacritic_normalizer.py logic (NFD decomposition, combining mark removal)
- Add Devanagari-aware rules from TRACK-077:
  - Strip trailing anusvara 'm' from words (kamalambam → kamalamba)
  - Collapse epenthetic vowels in consonant clusters (laksimi → laksmi)
  - Normalize conjunct splits (ndar → ndr)
- Composer canonicalization: map Trinity variants to canonical names
  (Tyagaraja/Thyagaraja→tyagaraja, Muttuswami/Muthuswami Diksitar→muttusvami diksitar, etc.)
- Raga normalization: vowel reduction (aa→a), strip spaces
- Tala normalization: suffix removal (rupakam→rupaka), alias mapping

Reference files:
- tools/krithi-extract-enrich-worker/src/diacritic_normalizer.py (existing logic to merge)
- modules/shared/domain/.../TransliterationCollapse.kt (rules to port)
- modules/backend/api/.../NameNormalizationService.kt (composer/raga/tala logic to port)
```

**Task 1.2: Create `tests/test_normalizer.py`** (standalone, verifiable)
```
Input: Create tools/krithi-extract-enrich-worker/tests/test_normalizer.py

Requirements:
- Real data fixtures from actual PDFs (not synthetic):
  - English IAST: "Akhilandesvari", "Kamalamba Bhajare", "Sri Subrahmanyaya Namaste"
  - Sanskrit Devanagari-derived: "akhilandesvari", "kamalamba bajare", "sri subrahmanyaya namaste"
  - Both should normalize to the same key
- Test all entity types (title, composer, raga, tala)
- At least 20 real title pairs from mdeng.pdf vs mdskt.pdf
- Test edge cases: OCR noise prefixes, section labels, single-word entries

Fixtures can be derived from: database/analysis/missed_sanskrit_variants.csv
```

**Task 1.3: Update `worker.py` to use consolidated normalizer** (depends on 1.1)
```
Input: Modify tools/krithi-extract-enrich-worker/src/worker.py

Requirements:
- After extraction, call normalize_for_matching() on title, composer, raga, tala
- Add normalized fields to result_payload JSON:
  title_normalized, composer_normalized, raga_normalized, tala_normalized
- Update schema.py CanonicalExtractionDto with new optional fields
```

**Verification for Codex:** `cd tools/krithi-extract-enrich-worker && pytest tests/test_normalizer.py -v` — all tests pass.

---

### Antigravity (Infrastructure/Docker/Migrations)

Antigravity works independently on infrastructure. No application logic.

**Phase 2 tasks — Replace Rust CLI:**

**Task 2.1: Create Python migration runner** (standalone, verifiable)
```
Input: Create tools/db-migrate/ Python package

Requirements:
- tools/db-migrate/migrate.py:
  - Reads database/migrations/*.sql files in numeric order (01__, 02__, etc.)
  - Tracks applied migrations in a schema_migrations table (id, filename, applied_at)
  - Applies pending migrations via psycopg
  - Commands: migrate, reset (drop all + recreate + migrate), status, create <name>
- tools/db-migrate/cli.py:
  - CLI entry: python -m db_migrate migrate|reset|status|create
  - Reads DB connection from env vars: DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
  - Supports --dry-run flag
- tools/db-migrate/pyproject.toml:
  - Dependencies: psycopg[binary] only
  - Cross-platform (Linux, macOS, Windows)
- MUST work with existing 37 SQL migration files AS-IS (no conversion)
- MUST handle the existing schema (down migrations in some files)
- CI/CD compatible: exit code 0 on success, non-zero on failure

Test: Apply all 37 migrations to a fresh PostgreSQL 18 database
```

**Task 2.2: Rewrite `compose.yaml`** (standalone, verifiable)
```
Input: Rewrite compose.yaml with profiles

Requirements:
- Services:
  - db: PostgreSQL 18.3 (keep existing config, add healthcheck)
  - backend: Build from modules/backend/api/, expose port 8080
    - Dev profile: mount source, use gradle run
    - Prod profile: fat JAR
  - extraction: Build from tools/krithi-extract-enrich-worker/
    - Dev profile: volume-mount src/ for hot-reload
    - Prod profile: COPY src/
  - frontend: Build from modules/frontend/sangita-admin-web/
    - Dev profile: volume-mount, bun run dev on port 5001
    - Prod profile: bun run build + nginx
- Profiles: default (db only), dev (all with hot-reload), prod (all with builds)
- Environment variables from .env file
- Network: all services on same Docker network

Test: docker compose --profile dev up starts all services, frontend accessible on :5001
```

**Task 2.3: Create Makefile** (standalone, verifiable)
```
Input: Create Makefile at project root

Targets:
- make dev          → docker compose --profile dev up
- make dev-down     → docker compose --profile dev down
- make db-reset     → python -m db_migrate reset (with correct env vars)
- make migrate      → python -m db_migrate migrate
- make test         → ./gradlew :modules:backend:api:test
- make test-frontend → cd modules/frontend/sangita-admin-web && bun test
- make extract PDF=mdeng.pdf → submit PDF to extraction queue
- make steel-thread → run E2E smoke test
- make clean        → docker compose down -v

Cross-platform: use portable shell commands
```

**Verification for Antigravity:**
1. `make db-reset` applies all 37 migrations to fresh PG18 DB
2. `make dev` starts all 4 services
3. `curl http://localhost:8080/health` returns 200
4. `curl http://localhost:5001` returns frontend HTML

---

## Orchestration Flow

```
Week 1 (Mar 8-12):
┌─────────────────────────────────────────────────┐
│ Claude: Phase 0 (close tracks)                  │ Day 1
├─────────────────────┬───────────────────────────┤
│ Codex: Phase 1      │ Antigravity: Phase 2      │ Days 2-5
│ (Python normalizer) │ (Docker + migrations)     │ (parallel)
├─────────────────────┴───────────────────────────┤
│ Claude: Integrate Phase 1 + 2 outputs           │ Day 5
└─────────────────────────────────────────────────┘

Week 2 (Mar 13-21):
┌─────────────────────────────────────────────────┐
│ Claude: Phase 3 (E2E validation) — CRITICAL     │ Days 6-8
│  → May delegate Python fixes back to Codex      │
├─────────────────────────────────────────────────┤
│ Claude: Phase 4 (Curator review UI)             │ Days 9-10
├─────────────────────────────────────────────────┤
│ Claude: Phase 5 (Curate 500+ krithis)           │ Days 11-13
├─────────────────────────────────────────────────┤
│ Claude: Phase 6 (Wrap-up)                       │ Day 14
└─────────────────────────────────────────────────┘
```

### Integration Points (Claude must coordinate)

1. **After Codex completes Phase 1**: Claude updates `VariantMatchingService.kt` to read pre-normalized keys from extraction results instead of re-normalizing in Kotlin
2. **After Antigravity completes Phase 2**: Claude updates `.claude/commands/`, `CLAUDE.md`, verifies `make dev` works
3. **During Phase 3**: If normalization mismatches found, Claude files specific fix requests to Codex with real data examples

---

## Key Files Reference

| File | Language | Current Role | Change |
|:---|:---|:---|:---|
| `tools/krithi-extract-enrich-worker/src/normalizer.py` | Python | NEW | Consolidated normalizer |
| `tools/krithi-extract-enrich-worker/src/diacritic_normalizer.py` | Python | Diacritics | Merged into normalizer.py |
| `tools/krithi-extract-enrich-worker/src/worker.py` | Python | Extraction orchestrator | Add normalized fields |
| `tools/krithi-extract-enrich-worker/src/schema.py` | Python | DTOs | Add normalized fields |
| `tools/db-migrate/migrate.py` | Python | NEW | Migration runner |
| `modules/shared/domain/.../TransliterationCollapse.kt` | Kotlin | Transliteration rules | Deprecate |
| `modules/backend/api/.../NameNormalizationService.kt` | Kotlin | Normalization | Simplify |
| `modules/backend/api/.../VariantMatchingService.kt` | Kotlin | Matching | Read pre-normalized keys |
| `modules/backend/api/.../routes/CuratorRoutes.kt` | Kotlin | NEW | Curator review API |
| `modules/frontend/.../pages/CuratorReviewPage.tsx` | TypeScript | NEW | Curator review UI |
| `compose.yaml` | YAML | Docker Compose | Rewrite with profiles |
| `Makefile` | Make | NEW | Dev workflow |
| `tools/sangita-cli/` | Rust | CLI tool | Archive |

---

## Success Criteria (March 21)

- [ ] ≥ 500 krithis in database with source evidence
- [ ] Extraction pipeline works E2E for English and Sanskrit PDFs
- [ ] Curator review page functional at `/curator-review`
- [ ] Stack: Kotlin (API) + Python (extraction/normalization/migrations) + TypeScript (frontend)
- [ ] Rust CLI archived, `make dev` starts full stack
- [ ] All conductor tracks closed or explicitly deferred
- [ ] `git status` clean — zero uncommitted files

---

## Anti-Patterns to Avoid (from retrospective)

1. **No "big bang" sessions** — each task above is independently verifiable
2. **No marking "Completed" without E2E proof** — Phase 3 is the gate
3. **Fix in ONE place** — normalization bugs go to Python only
4. **Real data in tests** — use actual PDF text, not synthetic data
5. **Commit after each phase** — never accumulate uncommitted files
