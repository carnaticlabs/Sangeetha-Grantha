| Metadata | Value |
|:---|:---|
| **Status** | Accepted |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-06-13 |
| **Author** | Sangeetha Grantha Team |
| **Deciders** | Sangeetha Grantha Team (Seshadri) |
| **Spike** | [TRACK-116](../../../conductor/tracks/TRACK-116-versioned-canon-spike.md) (this ADR is its deliverable) |
| **Implemented by** | [TRACK-117](../../../conductor/tracks/TRACK-117-versioned-canon-implementation.md) |

# ADR-014: Versioned Canon & Provenance Graph (N5)

## Context

North-star finding **N5**: the canonical krithi tables hold **only current state**. The structural and lyric tables —
`krithis`, `krithi_sections`, `krithi_lyric_variants`, `krithi_lyric_sections` — each carry only `created_at` / `updated_at`.
`AUDIT_LOG` records *that* a change happened (action, entity, actor, timestamp) but **not a re-materializable *what***: there is
no stored diff or payload, so prior state cannot be reconstructed faithfully.

A scholarly Carnatic corpus needs **bitemporal, attributable** answers:

> "What did the canonical text of *Vātāpi Gaṇapatim*'s anupallavi say on 2026-01-15, and which **source document /
> extraction run / curator decision** produced *each section*?"

The current schema cannot answer either half. It also already contains **partial, coarse** provenance that we must
rationalise rather than duplicate:

| Existing table | What it gives us | Gap for N5 |
|---|---|---|
| `import_sources` | the **registry** node (name, base_url) | fine as-is |
| `extraction_queue` | the **extraction run** (result_payload, extractor_version, confidence, source_checksum) | not linked to a *document* node or to individual sections |
| `krithi_source_evidence` | **krithi-level** provenance (source_url, method, checksum, `contributed_fields[]`) | whole-krithi grain — cannot say which source produced *this section* |
| `imported_krithis` | raw imported row → `mapped_krithi_id` | pre-canon staging, not canon history |

**Enabling decision D1** (north-star decision log): because the Trinity DB krithis can be re-imported from scratch, we
**pause the import, build versioned canon, and re-import fresh** — so history and provenance are captured **from row one
with no retrofit/backfill**. This ADR is authored during the D2 freeze, in parallel with the Flyway cutover
([TRACK-110](../../../conductor/tracks/TRACK-110-testcontainers-flyway-cutover.md)); it adds no critical-path time.

### Constraints / non-functional requirements

- **PostgreSQL 18**, UUID v7 PKs across the schema ([ADR-011](./ADR-011-postgresql-18-uuid-v7.md)); new tables must follow suit (`uuidv7()` defaults).
- **Flyway is the only migration engine** ([ADR-013](./ADR-013-db-migration-with-flyway.md)); the schema ships as a versioned `V44__…` migration authored by TRACK-117 (this ADR authors **no** SQL file).
- Write volume is **low** (single-curator curation + batch imports), so write-amplification from append-only history is acceptable; read paths must stay fast.
- **No PostgreSQL extension dependency** — the system targets managed Postgres where extension availability is not guaranteed.
- Existing public/admin read routes and DTOs should keep working with **minimal churn**.

## Decision

Adopt **Option A — append-only revision tables with a current-state projection**, plus a normalised **provenance graph**:

1. **Revision history (append-only).** Every accepted change to a krithi writes an immutable **revision envelope**
   (`krithi_revisions`) and one append-only **per-section** row (`krithi_section_revisions`) per section in that revision.
   Nothing is updated in place; "current" is the latest revision.

2. **Provenance graph (normalised, per-section).** Introduce a `source_documents` node (the physical artifact) between the
   existing `extraction_queue` and `import_sources`, and attribute provenance **at the section grain**:

   ```
   krithi_section_revisions.source_document_id ─┐
   krithi_section_revisions.extraction_id ──────┤
                                                 ▼
        extraction_queue.source_document_id ──► source_documents.import_source_id ──► import_sources
              (extraction run)                      (physical artifact)                  (registry)
   ```

3. **Current-state projection.** A view (`v_krithi_current_revision`) and an as-of function (`krithi_sections_asof`) serve
   point-in-time reads. The existing current-state tables (`krithi_sections`, `krithi_lyric_sections`) **remain the serving
   layer**, written transactionally from the latest revision on each change (they cannot be replaced by views because
   `krithi_lyric_sections.section_id` is an FK target). Revisions are the **source of truth**; the current tables are a
   maintained projection.

**Revision granularity: per-section content under a krithi-level envelope.** A curator action or an import = **one**
`krithi_revisions` row; the section bodies live in `krithi_section_revisions`, each independently attributable. This is the
grain N5 demands ("which source produced *each section*") while still giving a clean whole-krithi point-in-time snapshot.

## Options Considered

### Option A — Append-only `krithi_revisions` + per-section revisions + projection view *(chosen)*

| Dimension | Assessment |
|-----------|------------|
| Complexity | **Medium** — two new history tables + one document node + a view/function; explicit, no magic |
| Provenance fit | **Excellent** — provenance columns live on the same rows as the versioned content; per-section attribution is native |
| Extension dependency | **None** — plain tables, FKs, a view, one SQL function |
| Query model | "current" via projection view; "as-of" via `valid_from <= :t` + `DISTINCT ON`; provenance via a single 4-join |
| Re-import fit (D1) | **Native** — the import path simply inserts revision #1 + section rows with provenance from row one |

**Pros:** explicit and inspectable; provenance and content are co-located (one join answers N5); models transaction-time now
and leaves room for valid-time later; no extension; re-import populates it with zero backfill.
**Cons:** write amplification (history grows monotonically — bounded and acceptable at this corpus size); the import/edit
path must write revisions **and** project current state in one transaction (a deliberate, well-contained dual-write).

### Option B — PostgreSQL temporal / system-versioned tables

| Dimension | Assessment |
|-----------|------------|
| Complexity | **High** — Postgres has **no native** SQL:2011 system-versioning; requires the `temporal_tables` extension (trigger-based) or `pg_bitemporal`/`periods` |
| Provenance fit | **Poor** — history tables capture *system time* (when a row changed), not *who/what produced it*; provenance still has to be bolted on as extra columns |
| Extension dependency | **Yes** — disqualifying under the managed-Postgres constraint |
| Query model | as-of via period predicates, but provenance attribution is not modelled at all |
| Re-import fit | neutral |

**Pros:** automatic history capture via triggers; familiar `FOR SYSTEM_TIME AS OF` ergonomics where supported.
**Cons:** extension dependency we explicitly forbid; trigger "magic" hides the write path; captures transaction time only, not
the scholarly provenance graph — we'd end up building Option A's columns anyway, on top of triggers. **Rejected.**

### Option C — Audit-derived reconstruction (replay `AUDIT_LOG`)

| Dimension | Assessment |
|-----------|------------|
| Complexity | **Low to add, High to trust** — no schema change, but a fragile reconstruction engine |
| Provenance fit | **None** — `AUDIT_LOG` stores action + entity + actor, no materializable payload |
| Correctness | **Lossy** — past state cannot be faithfully rebuilt; this *is* the N5 failure |

**Pros:** no new tables.
**Cons:** N5 exists *precisely because* this approach fails — the audit log is not a re-materializable record. Kept only as
the rejected baseline. `AUDIT_LOG` remains for *who/when* mutation tracking (Critical Rule #3); it is **complementary to**, not
a substitute for, versioned canon. **Rejected.**

## Trade-off Analysis

The decisive axes are **provenance fit** and **extension dependency**. Option C cannot answer N5 at all. Option B can answer
"what was the value at time T" but not "which source/extraction produced it," and only by taking on an extension we've ruled
out — and even then we'd add Option A's provenance columns. Option A pays a bounded write-amplification cost and one
contained dual-write in the import/edit path, and in exchange gives **content + provenance co-located on append-only rows**,
answerable in a single query, with **no extension** and **native re-import population**. Given low write volume and the D1
re-import, Option A's costs are cheap and its benefits are exactly what N5 requires.

## Provenance Graph — Design Detail

**Nodes & edges (cardinality):**

- `import_sources` (**registry**) **1 — N** `source_documents` — a registry lists many physical artifacts.
- `source_documents` (**artifact**) **1 — N** `extraction_queue` — one document may be extracted many times (re-runs, method changes).
- `extraction_queue` (**extraction run**) **1 — N** `krithi_revisions` — one extraction can create/update many krithis.
- `krithis` **1 — N** `krithi_revisions` (history) **1 — N** `krithi_section_revisions` (per-section content).
- `krithi_section_revisions` **N — 1** `extraction_queue` and **N — 1** `source_documents` — **each section is independently attributable** (different sections of one krithi may come from different sources).

**Null-handling (the contract for what may be absent):**

| Change kind | `extraction_id` | `source_document_id` | `created_by_user_id` |
|---|---|---|---|
| `IMPORT` (auto-approved or curator-approved) | **set** | **set** | approver (set) |
| `CURATOR_EDIT` (manual) | NULL | NULL | **set** (required) |
| `MERGE` / `CORRECTION` | optional | optional | **set** |

A `CHECK` enforces the floor: a revision must carry **either** an `extraction_id` **or** a `created_by_user_id` (never
anonymous, never unattributed). `source_document_id` on a section may be NULL only when `extraction_id` is NULL (manual text).

**`krithi_source_evidence`** is retained as a **krithi-level rollup** ("which sources contributed to this krithi overall");
it becomes **derivable** from `krithi_section_revisions` and is no longer the finest grain. TRACK-117 may keep populating it
for compatibility or mark it a generated rollup — an implementation choice, not an ADR-blocking one.

## Migration Shape for TRACK-117 (DDL sketch — **no SQL file authored here**)

TRACK-117 authors this as a single Flyway migration **`V44__versioned_canon.sql`** (post-[TRACK-110](../../../conductor/tracks/TRACK-110-testcontainers-flyway-cutover.md) `VNN__` naming). Sketch only:

```text
-- 1. Physical source artifact (the "source_document" node)
CREATE TABLE source_documents (
    id                UUID PRIMARY KEY DEFAULT uuidv7(),
    import_source_id  UUID NOT NULL REFERENCES import_sources(id),     -- → registry
    source_url        TEXT NOT NULL,
    source_format     TEXT NOT NULL CHECK (source_format IN ('HTML','PDF','DOCX','API','MANUAL')),
    page_range        TEXT,
    checksum          TEXT,                                            -- SHA-256 of artifact bytes
    retrieved_at      TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
    CONSTRAINT source_documents_dedup_uq UNIQUE (import_source_id, source_url, checksum)
);

-- 2. Link the existing extraction run to the document it consumed
ALTER TABLE extraction_queue
    ADD COLUMN source_document_id UUID REFERENCES source_documents(id);

-- 3. Append-only revision envelope (one per accepted change-set to a krithi)
CREATE TABLE krithi_revisions (
    id                 UUID PRIMARY KEY DEFAULT uuidv7(),
    krithi_id          UUID NOT NULL REFERENCES krithis(id) ON DELETE CASCADE,
    revision_no        INT  NOT NULL,                                  -- 1..N per krithi
    change_kind        TEXT NOT NULL CHECK (change_kind IN ('IMPORT','CURATOR_EDIT','MERGE','CORRECTION')),
    change_reason      TEXT,
    extraction_id      UUID REFERENCES extraction_queue(id),           -- NULL for manual edits
    created_by_user_id UUID REFERENCES users(id),
    valid_from         TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
    recorded_at        TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
    CONSTRAINT krithi_revisions_no_uq    UNIQUE (krithi_id, revision_no),
    CONSTRAINT krithi_revisions_attrib_ck CHECK (extraction_id IS NOT NULL OR created_by_user_id IS NOT NULL)
);

-- 4. Append-only per-section content + per-section provenance (the materializable "what")
CREATE TABLE krithi_section_revisions (
    id                 UUID PRIMARY KEY DEFAULT uuidv7(),
    revision_id        UUID NOT NULL REFERENCES krithi_revisions(id) ON DELETE CASCADE,
    krithi_id          UUID NOT NULL REFERENCES krithis(id),           -- denormalized for fast as-of
    section_type       TEXT NOT NULL,                                  -- PALLAVI / ANUPALLAVI / CHARANAM / …
    order_index        INT  NOT NULL,
    label              TEXT,
    language           language_code_enum,
    script             script_code_enum,
    text               TEXT NOT NULL,
    normalized_text    TEXT,
    extraction_id      UUID REFERENCES extraction_queue(id),           -- per-section source attribution
    source_document_id UUID REFERENCES source_documents(id),
    valid_from         TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now())
);

-- 5. Indexes for point-in-time and provenance joins
CREATE INDEX krithi_revisions_asof_idx          ON krithi_revisions (krithi_id, valid_from DESC, revision_no DESC);
CREATE INDEX krithi_section_revisions_asof_idx  ON krithi_section_revisions (krithi_id, valid_from DESC);
CREATE INDEX krithi_section_revisions_rev_idx   ON krithi_section_revisions (revision_id);
CREATE INDEX source_documents_registry_idx      ON source_documents (import_source_id, checksum);

-- 6. Current-state projection (latest revision per krithi)
CREATE VIEW v_krithi_current_revision AS
    SELECT DISTINCT ON (krithi_id) *
    FROM krithi_revisions
    ORDER BY krithi_id, valid_from DESC, revision_no DESC;

-- 7. As-of read: sections of the latest revision with valid_from <= :as_of
CREATE FUNCTION krithi_sections_asof(p_krithi UUID, p_at TIMESTAMPTZ)
RETURNS SETOF krithi_section_revisions
LANGUAGE sql STABLE AS $$
    SELECT sr.*
    FROM krithi_section_revisions sr
    WHERE sr.revision_id = (
        SELECT id FROM krithi_revisions
        WHERE krithi_id = p_krithi AND valid_from <= p_at
        ORDER BY valid_from DESC, revision_no DESC
        LIMIT 1
    );
$$;
```

> **No backfill.** Per **D1**, the corpus is re-imported from scratch by TRACK-117; revision #1 + provenance are written by the
> import path at creation. There is therefore no migration-time data backfill — the migration is pure DDL.

### Point-in-time & one-query provenance semantics

- **Validity model:** transaction-time (`valid_from` = when the revision was recorded). Valid-time ("as the source dates it")
  is intentionally **out of scope** here; the schema leaves room to add a `valid_time` period later without breaking reads.
- **"Current"** = `v_krithi_current_revision` (or `krithi_sections_asof(id, now())`).
- **The N5 provenance answer in one query** ("which source produced each section as of date X"):

```sql
SELECT  sr.section_type, sr.order_index, sr.text,
        eq.extractor_version, eq.confidence,
        sd.source_url, sd.checksum,
        reg.name AS source_registry
FROM    krithi_sections_asof(:krithi_id, :as_of) sr
LEFT JOIN extraction_queue eq ON eq.id = sr.extraction_id
LEFT JOIN source_documents sd ON sd.id = sr.source_document_id
LEFT JOIN import_sources   reg ON reg.id = sd.import_source_id
ORDER BY sr.order_index;
```

## Revision-Write Contract

- **What creates a revision:** every *accepted* change — import auto-approval, curator approval, manual curator edit, merge,
  correction. Draft/in-review states do **not** write canon revisions (they live in `imported_krithis` staging).
- **Atomicity:** within one `DatabaseFactory.dbQuery { }` transaction, the path writes (a) the `krithi_revisions` envelope,
  (b) its `krithi_section_revisions` rows, (c) the projection into the current-state tables, and (d) the `AUDIT_LOG` entry
  (Critical Rule #3). All-or-nothing.
- **Import path (TRACK-117 / re-import):** on first creation, writes `revision_no = 1`, `change_kind = 'IMPORT'`,
  `extraction_id` + section-level `source_document_id` from the extraction that produced each section.
- **Curator edit:** writes `revision_no = N+1`, `change_kind = 'CURATOR_EDIT'`, `created_by_user_id` set, extraction/document
  NULL for hand-edited sections (carried forward for untouched sections).

## Impact Check (for TRACK-117 scoping)

- **DAL (`dal`):** new Exposed table objects for `source_documents`, `krithi_revisions`, `krithi_section_revisions`; a
  `RevisionRepository`; extend the ingestion/creation path to write revisions + project current state in one transaction.
- **Services (`api`):** `KrithiCreationFromExtractionService` and curator edit/approve paths become revision-writers.
- **Routes/DTOs:** **current** read routes are unaffected (they read the projection/current tables, unchanged shape). New
  *history/provenance* read endpoints (as-of, lineage) are **additive** and can land with TRACK-117 or later.
- **Public read surface:** the deferred public API gains a future "provenance/lineage" capability; **no change required now**.
- **Tests:** reuses the TRACK-110 Testcontainers substrate — a krithi edit creates a revision; provenance lineage resolves in
  one query (the SQL above).

## Consequences

**Easier:** faithful point-in-time reads; per-section "which source said this" in one query; scholarly citation/lineage;
re-import captures everything from row one; `AUDIT_LOG` stays lean (who/when) while canon carries the materializable what.
**Harder:** the import/edit path must write history + project current state atomically (one contained dual-write); history
tables grow monotonically (bounded, prunable far in the future if ever needed).
**To revisit:** adding **valid-time** (bitemporal) if "as the source dates it" becomes a requirement; whether
`krithi_source_evidence` is retired in favour of a generated rollup over `krithi_section_revisions`.

## Action Items (TRACK-117)

1. [ ] Author `V44__versioned_canon.sql` from the DDL sketch above (Flyway; `uuidv7()` defaults; no backfill).
2. [ ] DAL: Exposed tables + `RevisionRepository`; wire the import/ingestion path to write revisions + provenance at creation.
3. [ ] Make the curator edit/approve/merge paths revision-writers; project current state transactionally; keep `AUDIT_LOG`.
4. [ ] `make db-reset` → re-import Trinity fresh; verify revision + provenance rows populate from the import path.
5. [ ] Verify junction tables (`krithi_ragas`) and sections populate through the full stack (DB → API → UI) post-reimport.
6. [ ] Integration coverage on the TRACK-110 substrate: edit → new revision; provenance lineage resolves in one query.
7. [ ] Resume / close TRACK-093 and TRACK-096 against the new schema.

## References

- [North-Star Evaluation N5](../../north-star-evaluation.md) · [North-Star Decision Log (D1)](../../north-star-production-readiness-decision.md)
- [TRACK-116 (spike)](../../../conductor/tracks/TRACK-116-versioned-canon-spike.md) · [TRACK-117 (implementation)](../../../conductor/tracks/TRACK-117-versioned-canon-implementation.md)
- [ADR-011 — PostgreSQL 18 & UUID v7](./ADR-011-postgresql-18-uuid-v7.md) · [ADR-012 — Unified Extraction](./ADR-012-unified-extraction-architecture.md) · [ADR-013 — Flyway](./ADR-013-db-migration-with-flyway.md)
- [Integration Tests Approach](../../07-quality/integration-tests-approach.md) · [Implementation Plan](../../north-star-production-readiness-implementation-plan.md)
