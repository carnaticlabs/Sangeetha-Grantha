#!/usr/bin/env python3
"""Targeted embedding rebuild script for TRACK-144 tala repairs.

Finds all compositions whose search documents and embeddings were affected by
the TRACK-144 canonical tala repairs (where document_embeddings.content_hash was
invalidated to 'STALE_TRACK_144_NEEDS_REBUILD'), recomputes their Gemini embeddings,
and updates document_embeddings with the fresh vectors and matching content hashes.

Usage:
  uv run python scripts/rebuild_track144_embeddings.py --dry-run
  uv run python scripts/rebuild_track144_embeddings.py --limit 10
  uv run python scripts/rebuild_track144_embeddings.py
"""

from __future__ import annotations

import argparse
import logging
import os
import sys

import psycopg

# Add src and scripts to sys.path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))
sys.path.insert(0, os.path.abspath(os.path.dirname(__file__)))

from scripts.embed_catalogue import (
    DEFAULT_DB_URL,
    STORAGE_DIMENSIONS,
    fetch_krithi_candidates,
    find_profile,
    index_krithi,
    validate_dimensions,
)
from src.embeddings.catalogue_index import ensure_profile
from src.embeddings.gemini_embedder import GeminiEmbedder

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger("rebuild_track144_embeddings")
SCRIPT_NAME = "rebuild_track144_embeddings.py"


def _v65_cohort_ids(conn: psycopg.Connection) -> set[str]:
    """Compositions whose embedding hashes V65 captured and marked stale."""
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT DISTINCT sd.krithi_id::text
            FROM audit_log a
            JOIN document_embeddings de ON de.id = a.entity_id
            JOIN search_documents sd ON sd.id = de.document_id
            WHERE a.action = 'UPDATE'
              AND a.metadata->>'method' = 'track144_embedding_cohort'
            """
        )
        return {row[0] for row in cur.fetchall()}


def main():
    parser = argparse.ArgumentParser(description="Targeted forced rebuild of TRACK-144 embeddings")
    parser.add_argument("--dry-run", action="store_true", help="Inspect planned rebuilds without API calls or saving")
    parser.add_argument("--limit", type=int, help="Limit number of compositions to rebuild")
    parser.add_argument("--krithi-id", type=str, help="Target a specific composition UUID")
    parser.add_argument(
        "--v65-cohort",
        action="store_true",
        help="Only compositions whose embeddings V65 marked stale (taxonomy merges and V61 title repairs)",
    )
    parser.add_argument("--db-url", type=str, default=DEFAULT_DB_URL, help="PostgreSQL connection string")
    parser.add_argument("--model", type=str, default="gemini-embedding-2", help="Embedding model name")
    parser.add_argument(
        "--dims",
        type=int,
        default=STORAGE_DIMENSIONS,
        help=f"Output dimensions; fixed at {STORAGE_DIMENSIONS}",
    )
    parser.add_argument("--vertexai", action="store_true", help="Use Vertex AI with ADC instead of API key")
    parser.add_argument("--project-id", type=str, help="GCP project ID if using Vertex AI")

    args = parser.parse_args()

    try:
        validate_dimensions(args.dims)
    except ValueError as exc:
        logger.error("%s", exc)
        sys.exit(2)

    logger.info("Connecting to database: %s", args.db_url.split("@")[-1])
    with psycopg.connect(args.db_url) as conn:
        if args.dry_run:
            profile = find_profile(conn, args.model, args.dims)
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

        candidates = fetch_krithi_candidates(
            conn,
            krithi_id=args.krithi_id,
            limit=args.limit,
            stale_only=True,
        )
        if args.v65_cohort:
            cohort_ids = _v65_cohort_ids(conn)
            candidates = [krithi for krithi in candidates if str(krithi["id"]) in cohort_ids]

        logger.info("Found %d TRACK-144 candidate krithi(s) with stale embeddings", len(candidates))
        if not candidates:
            logger.info("No stale embeddings found. All vectors are current.")
            return

        total_embedded = 0
        total_retired = 0
        failures: list[dict[str, str]] = []

        for idx, krithi in enumerate(candidates, start=1):
            logger.info("[%d/%d] Rebuilding %s by %s", idx, len(candidates), krithi["title"], krithi["composer_name"])
            try:
                count, retired = index_krithi(
                    conn=conn,
                    embedder=embedder,
                    profile_id=profile_id,
                    krithi=krithi,
                    dry_run=args.dry_run,
                    force=False,  # document_needs_refresh triggers naturally due to STALE hash
                )
            except Exception as exc:  # noqa: BLE001
                conn.rollback()
                failures.append({"krithi_id": str(krithi["id"]), "title": krithi["title"], "error": str(exc)})
                logger.error("Failed rebuilding embeddings for '%s': %s", krithi["title"], exc)
                continue

            total_embedded += count
            total_retired += retired

        logger.info(
            "Rebuild complete. Generated %d embeddings, retired %d obsolete docs, %d failure(s).",
            total_embedded,
            total_retired,
            len(failures),
        )

        if failures:
            sys.exit(1)


if __name__ == "__main__":
    main()
