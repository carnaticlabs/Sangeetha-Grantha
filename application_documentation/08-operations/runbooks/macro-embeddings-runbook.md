| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-11-02 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Runbook |

# Macro embeddings runbook

---

The collection and kshetra cards in Rasika search come from `CYCLE_OVERVIEW` and `KSHETRA_OVERVIEW` documents ([search guide](../../03-api/search.md#collection-and-kshetra-cards-v2-track-152)). Their text is regenerated from tags, cycle members and temples, so a data change or migration can leave the stored text and vectors behind. Run this after `make migrate` in every environment that serves search, and after any change to cycle membership, temple fields or kriti-to-temple links.

Everything here runs from `tools/krithi-extract-enrich-worker` with `DATABASE_URL` set for the intended environment. Do not use `--include-macro` for a macro refresh: it also walks every krithi in the catalogue. Use `--macro-only`.

## 1. Migrate

```bash
make migrate
make migrate-status
```

## 2. Check what is stale (no API call, no writes)

```bash
cd tools/krithi-extract-enrich-worker
uv run python scripts/embed_catalogue.py --macro-only --check
```

The check regenerates every overview from current data and compares it with the stored document and vector for the model's profile (`--model` / `--dims`, default `gemini-embedding-2` / 768). It reads the database and hashes locally; it needs no embedding credentials. A document is stale when it has no row, no vector for the profile, a vector hash that differs, a document hash that differs from its indexed text, or a regenerated hash that differs from the stored one (source data changed).

Up to date: exit code 0 and

```text
Macro freshness | checked = <n> | stale = 0 (cycle 0, kshetra 0) | profile <id>
```

Stale: exit code 1 and one `STALE <kind> <name>: <reasons>` line per document. Go to step 3.

Without the worker, the first three conditions have this read-only form (zero rows means they hold; it cannot see source-data changes):

```sql
SELECT d.document_kind, COALESCE(d.tag_id, d.temple_id) AS anchor, e.content_hash AS vector_hash, d.content_hash
FROM search_documents d
LEFT JOIN document_embeddings e ON e.document_id = d.id
  AND e.profile_id = (SELECT id FROM embedding_profiles WHERE is_active)
WHERE d.document_kind IN ('CYCLE_OVERVIEW', 'KSHETRA_OVERVIEW') AND d.is_published
  AND (e.id IS NULL OR e.content_hash <> d.content_hash OR d.content_hash <> md5(d.indexed_content));
```

## 3. Re-embed the stale overviews

Preview first; this makes no API call and builds no embedding client:

```bash
uv run python scripts/embed_catalogue.py --macro-only --dry-run
```

`would embed` counts the overviews that will be sent. Each is one paid embedding call, so confirm the number matches step 2 (a handful, not the catalogue) before the real run, which needs the environment's Gemini credentials and whoever owns the quota to agree:

```bash
uv run python scripts/embed_catalogue.py --macro-only
```

`--force` re-embeds every overview (about 106 calls); use it only after a deliberate text or profile change.

## 4. Prove it

Run step 2 again. It must report `stale = 0` and exit 0. Record the line in the change or deployment notes. If the environment's backend caches nothing across restarts, no restart is needed; search reads the vectors on each request.

## Failure notes

- `No embedding profile for <model>/<dims>`: the catalogue indexer has never run for that model there. Create the profile with the regular indexer first; do not start a macro-only run to do it.
- A profile that is not active logs a warning: the vectors are fresh but search will not use them until the profile is activated.
- Cycles with empty, mixed-composer or unapproved membership are skipped on purpose and are not stale.

---

[Runbooks](./README.md) · [Search guide](../../03-api/search.md)
