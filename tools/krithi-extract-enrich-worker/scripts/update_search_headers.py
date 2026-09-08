#!/usr/bin/env python3
"""In-place update script for search_documents.indexed_content.

Updates existing [Raga: ...] headers in search_documents to include ASCII aliases
(e.g., [Raga: Chārukesi / Charukesi]) so hybrid lexical queries (word_similarity / ILIKE)
immediately match ASCII search terms without needing full Gemini API re-embedding.

Usage:
  uv run python scripts/update_search_headers.py [--dry-run]
"""

from __future__ import annotations

import argparse
import logging
import os
import re
import sys
from pathlib import Path

import psycopg
from psycopg.rows import dict_row

SCRIPT_DIR = Path(__file__).resolve().parent
WORKER_ROOT = SCRIPT_DIR.parent
if str(WORKER_ROOT) not in sys.path:
    sys.path.insert(0, str(WORKER_ROOT))

from src.embeddings.catalogue_index import write_audit  # noqa: E402
from src.embeddings.context_formatter import format_raga_header, strip_diacritics  # noqa: E402

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%H:%M:%S",
)
logger = logging.getLogger("update_search_headers")

DEFAULT_DB_URL = os.environ.get(
    "DATABASE_URL",
    "postgresql://postgres:postgres@localhost:5432/sangita_grantha",
)

RAGA_HEADER_PATTERN = re.compile(r"\[Raga:\s*([^\]]+)\]")


def redact_db_url(url: str) -> str:
    """Masks credentials in database connection URLs."""
    return re.sub(r"://([^:]+):([^@]+)@", r"://\1:***@", url)


def update_headers(db_url: str, dry_run: bool = False) -> None:
    logger.info("Connecting to database: %s", redact_db_url(db_url))
    with psycopg.connect(db_url) as conn:
        with conn.cursor(row_factory=dict_row) as cur:
            cur.execute(
                """
                SELECT id, indexed_content 
                FROM search_documents 
                WHERE indexed_content LIKE '%[Raga:%'
                """
            )
            rows = cur.fetchall()
            logger.info("Found %d search_documents with [Raga: ...] header", len(rows))

            updated_count = 0
            for row in rows:
                doc_id = row["id"]
                content = row["indexed_content"]

                match = RAGA_HEADER_PATTERN.search(content)
                if not match:
                    continue

                original_raga = match.group(1).strip()
                # If already aliased, skip
                if " / " in original_raga:
                    continue

                ascii_raga = strip_diacritics(original_raga)
                if ascii_raga.lower() == original_raga.lower():
                    continue

                new_raga_tag = format_raga_header(original_raga)
                new_content = content[: match.start()] + new_raga_tag + content[match.end() :]

                if dry_run:
                    logger.info(
                        "[DRY-RUN] Doc %s: '%s' -> '%s'",
                        doc_id,
                        match.group(0),
                        new_raga_tag,
                    )
                else:
                    cur.execute(
                        """
                        UPDATE search_documents 
                        SET indexed_content = %s, updated_at = clock_timestamp() 
                        WHERE id = %s
                        """,
                        (new_content, doc_id),
                    )
                    write_audit(
                        conn,
                        entity_table="search_documents",
                        entity_id=str(doc_id),
                        action="UPDATE_SEARCH_HEADER",
                        diff={"raga_header": {"before": match.group(0), "after": new_raga_tag}},
                        metadata={"script": "update_search_headers.py", "field": "indexed_content"},
                    )
                updated_count += 1

            if not dry_run:
                conn.commit()
                logger.info("Successfully updated %d documents with aliased Raga headers.", updated_count)
            else:
                logger.info("[DRY-RUN] Would update %d documents with aliased Raga headers.", updated_count)


def main():
    parser = argparse.ArgumentParser(description="Update search document headers with ASCII aliases")
    parser.add_argument("--db-url", type=str, default=DEFAULT_DB_URL, help="PostgreSQL connection string")
    parser.add_argument("--dry-run", action="store_true", help="Simulate updates without modifying database")
    args = parser.parse_args()

    update_headers(args.db_url, dry_run=args.dry_run)


if __name__ == "__main__":
    main()
