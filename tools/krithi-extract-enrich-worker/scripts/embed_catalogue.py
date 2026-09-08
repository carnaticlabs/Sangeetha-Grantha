#!/usr/bin/env python3
"""Backfill and catalog indexing script for Gemini Embedding 2.

Extracts krithis, sections, and lyrics from PostgreSQL, enriches them with
musicological context headers, generates 768-D embeddings using Gemini Embedding 2,
and stores them in pgvector.

Usage:
  uv run python scripts/embed_catalogue.py --limit 10
  uv run python scripts/embed_catalogue.py --krithi-id <UUID>
  uv run python scripts/embed_catalogue.py --all
"""

from __future__ import annotations

import argparse
import hashlib
import json
import logging
import os
import sys
from typing import Any

import psycopg
from psycopg.rows import dict_row

# Add src to sys.path if running from within the worker directory
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from src.embeddings.catalogue_index import (
    STORAGE_DIMENSIONS,
    activate_profile,
    ensure_profile,
    find_obsolete_documents,
    find_profile,
    retire_documents,
    validate_dimensions,
    write_audit,
)
from src.embeddings.context_formatter import (
    format_composition_overview,
    format_section_passage,
)
from src.embeddings.gemini_embedder import GeminiEmbedder

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger("embed_catalogue")

DEFAULT_DB_URL = os.environ.get(
    "DATABASE_URL",
    "postgresql://postgres:postgres@localhost:5432/sangita_grantha",
)
SCRIPT_NAME = "embed_catalogue.py"


def md5_hash(text: str) -> str:
    return hashlib.md5(text.encode("utf-8")).hexdigest()


def fetch_krithi_candidates(
    conn: psycopg.Connection,
    krithi_id: str | None = None,
    limit: int | None = None,
) -> list[dict[str, Any]]:
    """Fetches candidate krithis with their composer, raga, tala, and lyric details."""
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
        WHERE 1 = 1
    """
    params: list[Any] = []
    if krithi_id:
        query += " AND k.id = %s"
        params.append(krithi_id)

    query += " ORDER BY k.title ASC"

    if limit:
        query += f" LIMIT {int(limit)}"

    with conn.cursor(row_factory=dict_row) as cur:
        cur.execute(query, params)
        return cur.fetchall()


def fetch_sections_for_krithi(conn: psycopg.Connection, krithi_id: str) -> list[dict[str, Any]]:
    """Fetches parsed sections for a composition."""
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


def index_krithi(
    conn: psycopg.Connection,
    embedder: GeminiEmbedder,
    profile_id: str | None,
    krithi: dict[str, Any],
    dry_run: bool = False,
    force: bool = False,
) -> tuple[int, int]:
    """Creates search documents and embeddings for a single krithi and its sections.

    `profile_id` is None only in dry-run mode when no profile exists yet.
    Documents that no longer correspond to eligible source text are retired.

    Returns (embeddings generated/saved, documents retired). Commits on success;
    the caller owns rollback on failure.
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

    inserted_count = 0
    keep_doc_ids: list[str] = []

    # 1. Composition Overview Document
    # Document text must be byte-identical to batch_embed_catalogue.py so content hashes agree.
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
            primary_lyrics=primary_lyrics[:600],  # bounded macro-level overview
            musical_form=musical_form,
        )
        content_hash = md5_hash(overview_text)

        with conn.cursor() as cur:
            # Check existing
            cur.execute(
                """
                SELECT sd.id, de.id, de.content_hash 
                FROM search_documents sd
                LEFT JOIN document_embeddings de ON de.document_id = sd.id AND de.profile_id = %s
                WHERE sd.krithi_id = %s AND sd.section_id IS NULL AND sd.document_kind = 'COMPOSITION_OVERVIEW'
                """,
                (profile_id, krithi_id),
            )
            existing = cur.fetchone()
            if existing:
                keep_doc_ids.append(str(existing[0]))

            if not existing or force or (existing and not existing[1]) or (existing and existing[2] != content_hash):
                if dry_run:
                    logger.info("[DRY-RUN] Would embed Overview for: %s (%s)", title, raga)
                else:
                    logger.info("Embedding Overview for: %s (%s)", title, raga)
                    vector = embedder.embed_document(overview_text, title=title)

                    # Upsert search_document
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
                        raise RuntimeError(f"Failed to upsert search document for {title}")
                    doc_id = doc_row[0]
                    if not existing:
                        keep_doc_ids.append(str(doc_id))

                    # Upsert document_embedding
                    cur.execute(
                        """
                        INSERT INTO document_embeddings (document_id, profile_id, embedding, content_hash)
                        VALUES (%s, %s, %s, %s)
                        ON CONFLICT (document_id, profile_id)
                        DO UPDATE SET embedding = EXCLUDED.embedding, content_hash = EXCLUDED.content_hash
                        """,
                        (doc_id, profile_id, vector, content_hash),
                    )
                    inserted_count += 1

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
                SELECT sd.id, de.id, de.content_hash 
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

            if not existing or force or (existing and not existing[1]) or (existing and existing[2] != content_hash):
                script_label = sec.get("script", "unknown")
                if dry_run:
                    logger.info("[DRY-RUN] Would embed Section %s (%s) for: %s", sec_type, script_label, title)
                else:
                    logger.info("Embedding Section %s (%s) for: %s", sec_type, script_label, title)
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
                    inserted_count += 1

    # 3. Retire documents whose source text is gone or below the eligibility threshold
    obsolete_ids = find_obsolete_documents(conn, krithi_id, keep_doc_ids)
    retired_count = 0
    if obsolete_ids:
        if dry_run:
            logger.info("[DRY-RUN] Would retire %d obsolete search document(s) for: %s", len(obsolete_ids), title)
        else:
            retired_count = retire_documents(conn, obsolete_ids)
            logger.info("Retired %d obsolete search document(s) for: %s", retired_count, title)

    if (inserted_count > 0 or retired_count > 0) and not dry_run:
        write_audit(
            conn,
            entity_table="krithis",
            entity_id=krithi_id,
            action="EMBED_CATALOGUE",
            diff={
                "inserted_count": inserted_count,
                "retired_count": retired_count,
                "retired_document_ids": obsolete_ids if retired_count else [],
                "profile_id": profile_id,
            },
            metadata={"title": title, "musical_form": musical_form, "script": SCRIPT_NAME},
        )

    conn.commit()
    return inserted_count, retired_count


def main():
    parser = argparse.ArgumentParser(description="Backfill Gemini Embedding 2 vectors into PostgreSQL")
    parser.add_argument("--limit", type=int, help="Limit number of krithis to process")
    parser.add_argument("--krithi-id", type=str, help="Process a single krithi by UUID")
    parser.add_argument("--all", action="store_true", help="Process the entire catalogue")
    parser.add_argument("--dry-run", action="store_true", help="Print plan without calling embedding API or saving")
    parser.add_argument("--force", action="store_true", help="Force re-embed even if hash matches")
    parser.add_argument("--db-url", type=str, default=DEFAULT_DB_URL, help="PostgreSQL connection string")
    parser.add_argument("--model", type=str, default="gemini-embedding-2", help="Embedding model name")
    parser.add_argument(
        "--dims",
        type=int,
        default=STORAGE_DIMENSIONS,
        help=f"Output dimensions (MRL); storage is fixed at {STORAGE_DIMENSIONS}",
    )
    parser.add_argument("--vertexai", action="store_true", help="Use Vertex AI with ADC instead of API key")
    parser.add_argument("--project-id", type=str, help="GCP project ID if using Vertex AI")
    parser.add_argument(
        "--activate-profile",
        action="store_true",
        help="After a successful run, atomically make this model's profile the single active retrieval profile",
    )

    args = parser.parse_args()

    if not args.limit and not args.krithi_id and not args.all and not args.dry_run:
        parser.print_help()
        sys.exit(1)

    try:
        validate_dimensions(args.dims)
    except ValueError as exc:
        logger.error("%s", exc)
        sys.exit(2)

    logger.info("Connecting to database: %s", args.db_url.split("@")[-1])  # log without credentials
    with psycopg.connect(args.db_url) as conn:
        if args.dry_run:
            profile = find_profile(conn, args.model, args.dims)
            if profile is None:
                logger.info(
                    "[DRY-RUN] No embedding profile for %s/%d yet; a real run would create it", args.model, args.dims
                )
            profile_id = profile.id if profile else None
        else:
            profile = ensure_profile(conn, args.model, args.dims, actor=SCRIPT_NAME)
            profile_id = profile.id

        embedder = GeminiEmbedder(
            model=args.model,
            dimensions=args.dims,
            vertexai=args.vertexai,
            project_id=args.project_id,
        )

        candidates = fetch_krithi_candidates(conn, krithi_id=args.krithi_id, limit=args.limit)
        logger.info("Found %d candidate krithis to process", len(candidates))

        total_embedded = 0
        total_retired = 0
        failures: list[dict[str, str]] = []
        for idx, krithi in enumerate(candidates, start=1):
            logger.info("[%d/%d] Processing %s by %s", idx, len(candidates), krithi["title"], krithi["composer_name"])
            try:
                count, retired = index_krithi(
                    conn=conn,
                    embedder=embedder,
                    profile_id=profile_id,
                    krithi=krithi,
                    dry_run=args.dry_run,
                    force=args.force,
                )
            except Exception as exc:  # noqa: BLE001 - one composition must not poison the rest of the run
                conn.rollback()
                failures.append({"krithi_id": str(krithi["id"]), "title": krithi["title"], "error": str(exc)})
                logger.error("Failed indexing krithi '%s' (%s): %s", krithi["title"], krithi["id"], exc)
                continue
            total_embedded += count
            total_retired += retired

        logger.info(
            "Backfill complete. Generated %d document embeddings, retired %d obsolete documents, %d krithi(s) failed.",
            total_embedded,
            total_retired,
            len(failures),
        )

        if failures:
            logger.error("Failed krithi ids: %s", json.dumps(failures, ensure_ascii=False))
            sys.exit(1)

        if args.activate_profile and profile_id is not None:
            if args.dry_run:
                logger.info("[DRY-RUN] Would activate embedding profile %s", profile_id)
            elif profile is not None and profile.is_active:
                logger.info("Embedding profile %s is already active", profile_id)
            else:
                activate_profile(conn, profile_id, actor=SCRIPT_NAME)


if __name__ == "__main__":
    main()
