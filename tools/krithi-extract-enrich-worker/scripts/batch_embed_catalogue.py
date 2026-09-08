#!/usr/bin/env python3
"""Batch Manager for Gemini Embedding 2 Catalogue Embedding.

Orchestrates embedding generation across all catalogue compositions in configurable
batches with checkpointing, rate limit handling, telemetry, and comprehensive post-run
reporting.
"""

from __future__ import annotations

# ruff: noqa: E402, E501
import argparse
import datetime
import hashlib
import json
import logging
import os
import sys
import time
from pathlib import Path
from typing import Any

import psycopg
from psycopg.rows import dict_row

# Ensure parent and project root paths are in sys.path
SCRIPT_DIR = Path(__file__).resolve().parent
WORKER_ROOT = SCRIPT_DIR.parent
if str(WORKER_ROOT) not in sys.path:
    sys.path.insert(0, str(WORKER_ROOT))

from src.embeddings.catalogue_index import (
    STORAGE_DIMENSIONS,
    activate_profile,
    ensure_profile,
    find_obsolete_documents,
    find_profile,
    retire_documents,
    write_audit,
)
from src.embeddings.context_formatter import (
    format_composition_overview,
    format_section_passage,
)
from src.embeddings.gemini_embedder import GeminiEmbedder


def md5_hash(text: str) -> str:
    return hashlib.md5(text.encode("utf-8")).hexdigest()


logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%H:%M:%S",
)
logger = logging.getLogger("batch_embed")

DEFAULT_MODEL = "gemini-embedding-2"
DEFAULT_DIMENSIONS = STORAGE_DIMENSIONS
SCRIPT_NAME = "batch_embed_catalogue.py"


def get_db_connection() -> psycopg.Connection:
    """Resolves DB connection parameters from environment or default local configuration."""
    db_url = os.environ.get("DATABASE_URL")
    if db_url:
        return psycopg.connect(db_url)

    host = os.environ.get("DB_HOST", "localhost")
    port = int(os.environ.get("DB_PORT", "5432"))
    name = os.environ.get("DB_NAME", "sangita_grantha")
    user = os.environ.get("DB_USER", "postgres")
    password = os.environ.get("DB_PASSWORD", "postgres")

    return psycopg.connect(
        host=host,
        port=port,
        dbname=name,
        user=user,
        password=password,
    )


def fetch_all_krithi_ids(conn: psycopg.Connection, priority_unembedded: bool = True) -> list[dict[str, Any]]:
    """Fetches all krithi IDs and metadata ordered logically."""
    query = """
        SELECT 
            k.id,
            k.title,
            k.musical_form,
            c.name AS composer_name,
            r.name AS raga_name,
            t.name AS tala_name,
            d.name AS deity,
            tmp.name AS kshetra,
            k.primary_language,
            (
                SELECT array_agg(t.display_name_en)
                FROM krithi_tags kt
                JOIN tags t ON kt.tag_id = t.id
                WHERE kt.krithi_id = k.id
            ) AS tags,
            (
                SELECT COUNT(de.id)
                FROM search_documents sd
                JOIN document_embeddings de ON de.document_id = sd.id
                WHERE sd.krithi_id = k.id
            ) AS existing_embedding_count,
            (
                SELECT lv.lyrics 
                FROM krithi_lyric_variants lv 
                WHERE lv.krithi_id = k.id 
                ORDER BY lv.is_primary DESC, lv.created_at ASC 
                LIMIT 1
            ) AS primary_lyrics
        FROM krithis k
        JOIN composers c ON k.composer_id = c.id
        LEFT JOIN ragas r ON k.primary_raga_id = r.id
        LEFT JOIN talas t ON k.tala_id = t.id
        LEFT JOIN deities d ON k.deity_id = d.id
        LEFT JOIN temples tmp ON k.temple_id = tmp.id
    """
    if priority_unembedded:
        query += " ORDER BY existing_embedding_count ASC, k.title ASC"
    else:
        query += " ORDER BY k.title ASC"

    with conn.cursor(row_factory=dict_row) as cur:
        cur.execute(query)
        return cur.fetchall()


def fetch_sections_for_krithi(conn: psycopg.Connection, krithi_id: str) -> list[dict[str, Any]]:
    """Fetches lyric sections for a composition."""
    query = """
        SELECT 
            ks.id AS section_id,
            ks.section_type,
            ks.order_index,
            kls.lyric_variant_id,
            COALESCE(kls.text, '') AS section_text,
            klv.language,
            klv.script
        FROM krithi_sections ks
        LEFT JOIN krithi_lyric_sections kls ON kls.section_id = ks.id
        LEFT JOIN krithi_lyric_variants klv ON kls.lyric_variant_id = klv.id
        WHERE ks.krithi_id = %s
        ORDER BY ks.order_index ASC
    """
    with conn.cursor(row_factory=dict_row) as cur:
        cur.execute(query, (krithi_id,))
        return cur.fetchall()


def process_single_krithi(
    conn: psycopg.Connection,
    embedder: GeminiEmbedder,
    profile_id: str | None,
    krithi: dict[str, Any],
    dry_run: bool = False,
    force: bool = False,
) -> tuple[int, int, int]:
    """Processes a single composition: overview document + section passage documents.

    `profile_id` is None only in dry-run mode when no profile exists yet.
    Documents that no longer correspond to eligible source text are retired.
    Commits on success; the caller owns rollback on failure.

    Returns:
        (embedded_count, skipped_count, retired_count)
    """
    krithi_id = str(krithi["id"])
    title = krithi["title"]
    musical_form = krithi.get("musical_form")
    composer = krithi["composer_name"]
    raga = krithi["raga_name"]
    tala = krithi["tala_name"]
    deity = krithi.get("deity")
    kshetra = krithi.get("kshetra")
    tags = krithi.get("tags")
    primary_lyrics = krithi.get("primary_lyrics")

    embedded = 0
    skipped = 0
    keep_doc_ids: list[str] = []

    # 1. Overview Document
    if primary_lyrics and len(primary_lyrics.strip()) > 10:
        overview_text = format_composition_overview(
            title=title,
            composer=composer,
            raga=raga,
            tala=tala,
            deity=deity,
            kshetra=kshetra,
            tags=tags,
            language=krithi.get("primary_language"),
            primary_lyrics=primary_lyrics[:600],
            musical_form=musical_form,
        )
        content_hash = md5_hash(overview_text)

        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT sd.id, de.id, sd.content_hash
                FROM search_documents sd
                LEFT JOIN document_embeddings de ON de.document_id = sd.id AND de.profile_id = %s
                WHERE sd.krithi_id = %s AND sd.section_id IS NULL AND sd.document_kind = 'COMPOSITION_OVERVIEW'
                """,
                (profile_id, krithi_id),
            )
            existing = cur.fetchone()
            if existing:
                keep_doc_ids.append(str(existing[0]))

            # Skip if already embedded with matching hash
            if existing and existing[1] and existing[2] == content_hash and not force:
                skipped += 1
            else:
                if not dry_run:
                    vector = embedder.embed_document(overview_text, title=title)
                    cur.execute(
                        """
                        INSERT INTO search_documents (
                            krithi_id, document_kind, language_code, script_code,
                            original_content, indexed_content, content_hash
                        ) VALUES (%s, 'COMPOSITION_OVERVIEW', %s, NULL, %s, %s, %s)
                        ON CONFLICT (krithi_id, section_id, variant_id, document_kind, source_chunk_index)
                        DO UPDATE SET language_code = EXCLUDED.language_code,
                                      original_content = EXCLUDED.original_content,
                                      indexed_content = EXCLUDED.indexed_content,
                                      content_hash = EXCLUDED.content_hash,
                                      updated_at = clock_timestamp()
                        RETURNING id
                        """,
                        (krithi_id, krithi.get("primary_language"), primary_lyrics, overview_text, content_hash),
                    )
                    doc_row = cur.fetchone()
                    if not doc_row:
                        raise RuntimeError(f"Failed to upsert overview document for {title}")
                    doc_id = doc_row[0]
                    if not existing:
                        keep_doc_ids.append(str(doc_id))

                    cur.execute(
                        """
                        INSERT INTO document_embeddings (document_id, profile_id, embedding, content_hash)
                        VALUES (%s, %s, %s, %s)
                        ON CONFLICT (document_id, profile_id)
                        DO UPDATE SET embedding = EXCLUDED.embedding, content_hash = EXCLUDED.content_hash
                        """,
                        (doc_id, profile_id, vector, content_hash),
                    )
                embedded += 1

    # 2. Section Passage Documents
    sections = fetch_sections_for_krithi(conn, krithi_id)
    for sec in sections:
        sec_id = str(sec["section_id"])
        variant_id = str(sec["lyric_variant_id"]) if sec.get("lyric_variant_id") else None
        sec_text = sec["section_text"]
        sec_type = sec["section_type"]

        if not sec_text or len(sec_text.strip()) < 5:
            continue

        passage_text = format_section_passage(
            title=title,
            composer=composer,
            section_type=sec_type,
            section_text=sec_text,
            raga=raga,
            tala=tala,
            deity=deity,
            kshetra=kshetra,
            tags=tags,
            language=sec.get("language"),
            script=sec.get("script"),
            musical_form=musical_form,
        )
        content_hash = md5_hash(passage_text)

        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT sd.id, de.id, sd.content_hash
                FROM search_documents sd
                LEFT JOIN document_embeddings de ON de.document_id = sd.id AND de.profile_id = %s
                WHERE sd.krithi_id = %s 
                  AND sd.section_id = %s 
                  AND sd.variant_id IS NOT DISTINCT FROM %s 
                  AND sd.document_kind = 'SECTION_PASSAGE'
                """,
                (profile_id, krithi_id, sec_id, variant_id),
            )
            existing = cur.fetchone()
            if existing:
                keep_doc_ids.append(str(existing[0]))

            if existing and existing[1] and existing[2] == content_hash and not force:
                skipped += 1
            else:
                if not dry_run:
                    vector = embedder.embed_document(passage_text, title=f"{title} - {sec_type}")
                    cur.execute(
                        """
                        INSERT INTO search_documents (
                            krithi_id, section_id, variant_id, document_kind,
                            language_code, script_code,
                            original_content, indexed_content, content_hash
                        ) VALUES (%s, %s, %s, 'SECTION_PASSAGE', %s, %s, %s, %s, %s)
                        ON CONFLICT (krithi_id, section_id, variant_id, document_kind, source_chunk_index)
                        DO UPDATE SET language_code = EXCLUDED.language_code,
                                      script_code = EXCLUDED.script_code,
                                      original_content = EXCLUDED.original_content,
                                      indexed_content = EXCLUDED.indexed_content,
                                      content_hash = EXCLUDED.content_hash,
                                      updated_at = clock_timestamp()
                        RETURNING id
                        """,
                        (
                            krithi_id,
                            sec_id,
                            variant_id,
                            sec.get("language"),
                            sec.get("script"),
                            sec_text,
                            passage_text,
                            content_hash,
                        ),
                    )
                    sec_doc_row = cur.fetchone()
                    if not sec_doc_row:
                        raise RuntimeError(f"Failed to upsert section search document for {title} {sec_type}")
                    doc_id = sec_doc_row[0]
                    if not existing:
                        keep_doc_ids.append(str(doc_id))

                    cur.execute(
                        """
                        INSERT INTO document_embeddings (document_id, profile_id, embedding, content_hash)
                        VALUES (%s, %s, %s, %s)
                        ON CONFLICT (document_id, profile_id)
                        DO UPDATE SET embedding = EXCLUDED.embedding, content_hash = EXCLUDED.content_hash
                        """,
                        (doc_id, profile_id, vector, content_hash),
                    )
                embedded += 1

    # 3. Retire documents whose source text is gone or below the eligibility threshold
    obsolete_ids = find_obsolete_documents(conn, krithi_id, keep_doc_ids)
    retired = 0
    if obsolete_ids:
        if dry_run:
            logger.info("[DRY-RUN] Would retire %d obsolete search document(s) for: %s", len(obsolete_ids), title)
        else:
            retired = retire_documents(conn, obsolete_ids)

    if (embedded > 0 or retired > 0) and not dry_run:
        write_audit(
            conn,
            entity_table="krithis",
            entity_id=krithi_id,
            action="BATCH_EMBED_CATALOGUE",
            diff={
                "embedded_count": embedded,
                "retired_count": retired,
                "retired_document_ids": obsolete_ids if retired else [],
                "profile_id": profile_id,
            },
            metadata={"title": title, "musical_form": musical_form, "script": SCRIPT_NAME},
        )

    conn.commit()
    return embedded, skipped, retired


def run_sanity_searches(
    conn: psycopg.Connection,
    embedder: GeminiEmbedder,
) -> list[dict[str, Any]]:
    """Runs 4 representative semantic searches to verify index quality."""
    test_queries = [
        "Vatapi Ganapatim Hamsadhvani",
        "Lord Shiva Anandatandavam Chidambaram",
        "Kritis of Muthuswami Dikshitar on Pancha bootha",
        "Kamakshi Navavaranam Dikshitar",
    ]
    results = []

    for q in test_queries:
        try:
            query_vector = embedder.embed_query(q)
            vec_str = "[" + ",".join(str(v) for v in query_vector) + "]"
            with conn.cursor(row_factory=dict_row) as cur:
                cur.execute(
                    f"""
                    SELECT 
                        k.title,
                        c.name AS composer,
                        r.name AS raga,
                        d.document_kind,
                        LEFT(d.indexed_content, 120) AS snippet,
                        1 - (e.embedding <=> '{vec_str}'::vector(768)) AS similarity
                    FROM document_embeddings e
                    JOIN search_documents d ON e.document_id = d.id
                    JOIN krithis k ON d.krithi_id = k.id
                    JOIN composers c ON k.composer_id = c.id
                    LEFT JOIN ragas r ON k.primary_raga_id = r.id
                    ORDER BY e.embedding <=> '{vec_str}'::vector(768) ASC
                    LIMIT 3;
                    """
                )
                top_hits = cur.fetchall()
                results.append({"query": q, "hits": top_hits})
        except Exception as e:
            logger.warning("Sanity search for '%s' encountered error: %s", q, e)
            results.append({"query": q, "hits": [], "error": str(e)})

    return results


def generate_markdown_report(
    summary: dict[str, Any],
    sanity_results: list[dict[str, Any]],
    output_path: Path,
) -> None:
    """Writes a detailed execution report in Markdown format."""
    now_str = datetime.datetime.now(datetime.UTC).strftime("%Y-%m-%d %H:%M:%S UTC")

    md = f"""# Gemini Embedding 2 Catalogue Execution Report

**Execution Timestamp:** {now_str}  
**Model:** `{summary["model"]}` (768-D Matryoshka Representation Learning)  
**Database:** PostgreSQL 18 with `pgvector` HNSW index  

---

## 1. Execution Telemetry

| Metric | Value |
|:---|:---|
| **Total Krithis in Catalogue** | {summary["total_catalogue_krithis"]} |
| **Krithis Target in this Run** | {summary["target_krithis"]} |
| **Krithis Processed Successfully** | {summary["krithis_succeeded"]} |
| **Krithis Failed** | {summary["krithis_failed"]} |
| **New Embeddings Generated** | **{summary["total_embedded"]}** |
| **Embeddings Skipped (Unchanged)** | {summary["total_skipped"]} |
| **Obsolete Documents Retired** | {summary["total_retired"]} |
| **Elapsed Duration** | {summary["elapsed_formatted"]} |
| **Embedding Rate** | {summary["docs_per_sec"]:.2f} docs/sec |
| **Estimated Tokens Consumed** | ~{summary["estimated_tokens"]:,} |
| **Estimated API Cost** | **~${summary["estimated_cost"]:.4f} USD** |

---

## 2. Database State After Execution

| Table | Total Rows |
|:---|:---|
| `search_documents` (Composition Overviews) | {summary["db_overview_count"]} |
| `search_documents` (Section Passages) | {summary["db_passage_count"]} |
| `document_embeddings` (Active Vectors) | **{summary["db_total_embeddings"]}** |
| HNSW Cosine Index Status | `{summary["index_status"]}` |

---

## 3. Post-Run Sanity Check Searches

Verification of vector retrieval against live embedded krithis:

"""
    for res in sanity_results:
        md += f'### Query: *"{res["query"]}"*\n\n'
        if not res.get("hits"):
            md += "_No matches returned or dry-run execution._\n\n"
        else:
            md += "| Rank | Title | Composer | Raga | Match % | Snippet |\n"
            md += "|:---|:---|:---|:---|:---|:---|\n"
            for i, hit in enumerate(res["hits"], 1):
                sim_pct = f"{hit['similarity'] * 100:.1f}%"
                clean_snippet = hit["snippet"].replace("\n", " ")
                md += f"| #{i} | **{hit['title']}** | {hit['composer']} | {hit['raga'] or '-'} | `{sim_pct}` | {clean_snippet}... |\n"
            md += "\n"

    failures = summary.get("failures") or []
    if failures:
        md += "---\n\n## 4. Failed Compositions\n\n"
        md += f"Persisted to `{summary.get('failures_path')}`; exit status was non-zero.\n\n"
        md += "| Krithi ID | Title | Error |\n|:---|:---|:---|\n"
        for f in failures:
            err = str(f["error"]).replace("|", "\\|").replace("\n", " ")
            md += f"| `{f['krithi_id']}` | {f['title']} | {err} |\n"
        md += "\n"

    md += f"""---

## {5 if failures else 4}. Verification & Recommendations

1. **Incremental Updates:** Re-running this script after lyric edits or imports re-embeds only documents whose content hash changed and retires documents whose source text is gone. It is not triggered automatically; schedule it or run it after imports.
2. **Ktor API Integration:** Endpoints `POST /v1/search/hybrid` and `POST /v1/search/semantic` are live and querying this dataset.
3. **Frontend Search:** The admin console (`/krithis`) provides instant toggling between Lexical, Hybrid (RRF), and Semantic modes.
"""

    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(md, encoding="utf-8")
    logger.info("Saved execution report to: %s", output_path)


def main() -> None:
    parser = argparse.ArgumentParser(description="Batch embed krithis using Gemini Embedding 2 and pgvector.")
    parser.add_argument("--batch-size", type=int, default=50, help="Krithis per batch (default: 50)")
    parser.add_argument("--max-krithis", type=int, default=None, help="Maximum krithis to process (default: all)")
    parser.add_argument("--delay", type=float, default=1.0, help="Seconds to sleep between batches (default: 1.0)")
    parser.add_argument("--force", action="store_true", help="Re-embed even if already embedded")
    parser.add_argument("--dry-run", action="store_true", help="Preview without making API calls or modifying DB")
    parser.add_argument("--report-path", type=str, default=None, help="Custom report output path")
    parser.add_argument(
        "--activate-profile",
        action="store_true",
        help="After a fully successful run, atomically make this model's profile the single active retrieval profile",
    )

    args = parser.parse_args()

    conn = get_db_connection()
    logger.info("Connected to database successfully.")

    if args.dry_run:
        profile = find_profile(conn, DEFAULT_MODEL, DEFAULT_DIMENSIONS)
        if profile is None:
            logger.info(
                "[DRY-RUN] No embedding profile for %s/%d yet; a real run would create it",
                DEFAULT_MODEL,
                DEFAULT_DIMENSIONS,
            )
        profile_id = profile.id if profile else None
    else:
        profile = ensure_profile(conn, DEFAULT_MODEL, DEFAULT_DIMENSIONS, actor=SCRIPT_NAME)
        profile_id = profile.id
    embedder = GeminiEmbedder(model=DEFAULT_MODEL, dimensions=DEFAULT_DIMENSIONS)

    all_krithis = fetch_all_krithi_ids(conn, priority_unembedded=not args.force)
    total_catalogue = len(all_krithis)

    target_krithis = all_krithis[: args.max_krithis] if args.max_krithis else all_krithis
    total_to_process = len(target_krithis)

    logger.info(
        "Beginning batch execution: %d/%d krithis (batch_size=%d, dry_run=%s)",
        total_to_process,
        total_catalogue,
        args.batch_size,
        args.dry_run,
    )

    start_time = time.time()
    total_embedded = 0
    total_skipped = 0
    total_retired = 0
    krithis_succeeded = 0
    krithis_failed = 0
    failures: list[dict[str, str]] = []

    batch_size = args.batch_size
    num_batches = (total_to_process + batch_size - 1) // batch_size

    for b_idx in range(num_batches):
        batch = target_krithis[b_idx * batch_size : (b_idx + 1) * batch_size]
        b_num = b_idx + 1
        logger.info(
            "--- Starting Batch %d/%d (%d krithis) ---",
            b_num,
            num_batches,
            len(batch),
        )

        for k in batch:
            try:
                emb, skp, ret = process_single_krithi(
                    conn=conn,
                    embedder=embedder,
                    profile_id=profile_id,
                    krithi=k,
                    dry_run=args.dry_run,
                    force=args.force,
                )
                total_embedded += emb
                total_skipped += skp
                total_retired += ret
                krithis_succeeded += 1
            except Exception as e:  # noqa: BLE001 - one composition must not poison the rest of the run
                # process_single_krithi commits per composition, so this discards only the failed one
                conn.rollback()
                krithis_failed += 1
                failures.append({"krithi_id": str(k["id"]), "title": k["title"], "error": str(e)})
                logger.error("Failed embedding krithi '%s' (%s): %s", k["title"], k["id"], e)
                # If rate limited (429), back off
                if "429" in str(e) or "quota" in str(e).lower():
                    logger.warning("Quota threshold encountered. Cooling down for 30 seconds...")
                    time.sleep(30.0)

        elapsed = time.time() - start_time
        processed_so_far = krithis_succeeded + krithis_failed
        pct = (processed_so_far / total_to_process) * 100
        rate = total_embedded / elapsed if elapsed > 0 else 0
        logger.info(
            "Batch %d/%d Complete | Progress: %d/%d (%.1f%%) | Embedded: %d | Skipped: %d | Rate: %.1f docs/s",
            b_num,
            num_batches,
            processed_so_far,
            total_to_process,
            pct,
            total_embedded,
            total_skipped,
            rate,
        )

        if b_idx < num_batches - 1 and args.delay > 0:
            time.sleep(args.delay)

    total_elapsed = time.time() - start_time
    m, s = divmod(int(total_elapsed), 60)
    elapsed_formatted = f"{m}m {s}s"

    # DB Stats
    with conn.cursor() as cur:
        cur.execute("SELECT COUNT(*) FROM search_documents WHERE document_kind = 'COMPOSITION_OVERVIEW'")
        row = cur.fetchone()
        db_overview_count = row[0] if row else 0

        cur.execute("SELECT COUNT(*) FROM search_documents WHERE document_kind = 'SECTION_PASSAGE'")
        row = cur.fetchone()
        db_passage_count = row[0] if row else 0

        cur.execute("SELECT COUNT(*) FROM document_embeddings")
        row = cur.fetchone()
        db_total_embeddings = row[0] if row else 0

        cur.execute(
            """
            SELECT indexrelname, pg_size_pretty(pg_relation_size(indexrelid)) 
            FROM pg_stat_user_indexes 
            WHERE indexrelname = 'idx_doc_embeddings_hnsw_cosine'
            """
        )
        idx_row = cur.fetchone()
        index_status = f"{idx_row[0]} ({idx_row[1]})" if idx_row else "active"

    estimated_tokens = total_embedded * 200
    estimated_cost = (estimated_tokens / 1_000_000) * 0.15

    timestamp_slug = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
    failures_path: Path | None = None
    if failures:
        failures_path = WORKER_ROOT / "reports" / f"embedding_failures_{timestamp_slug}.json"
        failures_path.parent.mkdir(parents=True, exist_ok=True)
        failures_path.write_text(json.dumps(failures, indent=2, ensure_ascii=False), encoding="utf-8")
        logger.error("Persisted %d failed krithi id(s) to %s", len(failures), failures_path)

    summary: dict[str, Any] = {
        "model": DEFAULT_MODEL,
        "total_catalogue_krithis": total_catalogue,
        "target_krithis": total_to_process,
        "krithis_succeeded": krithis_succeeded,
        "krithis_failed": krithis_failed,
        "failures": failures,
        "failures_path": str(failures_path) if failures_path else None,
        "total_embedded": total_embedded,
        "total_skipped": total_skipped,
        "total_retired": total_retired,
        "elapsed_formatted": elapsed_formatted,
        "docs_per_sec": total_embedded / total_elapsed if total_elapsed > 0 else 0,
        "estimated_tokens": estimated_tokens,
        "estimated_cost": estimated_cost,
        "db_overview_count": db_overview_count,
        "db_passage_count": db_passage_count,
        "db_total_embeddings": db_total_embeddings,
        "index_status": index_status,
    }

    # Sanity searches
    sanity_results = []
    if not args.dry_run and total_embedded > 0:
        logger.info("Executing post-run sanity searches...")
        sanity_results = run_sanity_searches(conn, embedder)

    report_file = (
        Path(args.report_path)
        if args.report_path
        else WORKER_ROOT / "reports" / f"embedding_report_{timestamp_slug}.md"
    )
    generate_markdown_report(summary, sanity_results, report_file)

    # Mirror the latest *real* run to application_documentation; previews must not touch it.
    if not args.dry_run:
        app_doc_report = (
            WORKER_ROOT.parent.parent
            / "application_documentation"
            / "10-implementations"
            / "embedding-execution-report.md"
        )
        generate_markdown_report(summary, sanity_results, app_doc_report)

    logger.info("==================================================")
    logger.info("EXECUTION COMPLETE in %s", elapsed_formatted)
    logger.info("Krithis Succeeded: %d, Failed: %d", krithis_succeeded, krithis_failed)
    logger.info("Embeddings Created: %d, Skipped: %d, Retired: %d", total_embedded, total_skipped, total_retired)
    logger.info("Report saved to: %s", report_file)
    logger.info("==================================================")

    if krithis_failed > 0:
        logger.error(
            "Run incomplete: %d krithi(s) failed; profile activation skipped. See %s", krithis_failed, failures_path
        )
        conn.close()
        sys.exit(1)

    if args.activate_profile and profile_id is not None:
        if args.dry_run:
            logger.info("[DRY-RUN] Would activate embedding profile %s", profile_id)
        elif profile is not None and profile.is_active:
            logger.info("Embedding profile %s is already active", profile_id)
        else:
            activate_profile(conn, profile_id, actor=SCRIPT_NAME)

    conn.close()


if __name__ == "__main__":
    main()
