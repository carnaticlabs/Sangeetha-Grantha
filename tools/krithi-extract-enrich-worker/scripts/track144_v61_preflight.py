#!/usr/bin/env python3
"""Deployment preflight for V61's unscoped Marugelaraa pallavi write.

Read-only against the database. A snapshot file is the captured pre-migration
image of non-Latin variants. Recovery of those rows is Flyway V65, which reads
the contaminated row and removes the known inserted Latin text.

Usage:
  uv run python scripts/track144_v61_preflight.py
  uv run python scripts/track144_v61_preflight.py --snapshot output/track-144/marugelara-non-latin.json
  uv run python scripts/track144_v61_preflight.py --check-snapshot output/track-144/marugelara-non-latin.json
"""

from __future__ import annotations

import argparse
import os
import sys
from pathlib import Path

import psycopg
from psycopg.rows import dict_row

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from src.track144_v61_preflight import (
    MARUGELARA_TITLE,
    REPAIRED_TITLES,
    KrithiIdentity,
    LyricVariantView,
    PreflightInput,
    assess,
    load_snapshot,
    snapshot_still_matches,
    write_snapshot,
)

DEFAULT_DB_URL = os.environ.get(
    "DATABASE_URL",
    "postgresql://postgres:postgres@localhost:5432/sangita_grantha",
)


def load_inventory(conn: psycopg.Connection, *, snapshot_acknowledged: bool) -> PreflightInput:
    with conn.cursor(row_factory=dict_row) as cur:
        cur.execute(
            """
            SELECT version
            FROM flyway_schema_history
            WHERE success AND version = ANY(%s)
            """,
            (["61", "65"],),
        )
        applied = {row["version"] for row in cur.fetchall()}
        cur.execute(
            """
            SELECT k.id AS krithi_id, k.title, c.name AS composer
            FROM krithis k
            JOIN composers c ON c.id = k.composer_id
            WHERE k.title = ANY(%s)
            ORDER BY k.title, c.name, k.id
            """,
            (list(REPAIRED_TITLES),),
        )
        krithis = [
            KrithiIdentity(krithi_id=row["krithi_id"], title=row["title"], composer=row["composer"])
            for row in cur.fetchall()
        ]
        cur.execute(
            """
            SELECT v.id AS variant_id,
                   v.krithi_id,
                   k.title,
                   c.name AS composer,
                   v.script::text AS script,
                   v.language::text AS language,
                   v.lyrics,
                   (
                       SELECT ls.text
                       FROM krithi_lyric_sections ls
                       JOIN krithi_sections s ON s.id = ls.section_id
                       WHERE ls.lyric_variant_id = v.id
                         AND s.section_type = 'PALLAVI'
                         AND s.krithi_id = v.krithi_id
                       ORDER BY s.order_index
                       LIMIT 1
                   ) AS pallavi_text
            FROM krithi_lyric_variants v
            JOIN krithis k ON k.id = v.krithi_id
            JOIN composers c ON c.id = k.composer_id
            WHERE k.title = %s
            ORDER BY v.script, v.id
            """,
            (MARUGELARA_TITLE,),
        )
        variants = [
            LyricVariantView(
                variant_id=row["variant_id"],
                krithi_id=row["krithi_id"],
                title=row["title"],
                composer=row["composer"],
                script=row["script"],
                language=row["language"],
                lyrics=row["lyrics"],
                pallavi_text=row["pallavi_text"],
            )
            for row in cur.fetchall()
        ]
    return PreflightInput(
        v61_applied="61" in applied,
        v65_applied="65" in applied,
        krithis=krithis,
        variants=variants,
        snapshot_acknowledged=snapshot_acknowledged,
    )


def main() -> None:
    parser = argparse.ArgumentParser(description="Preflight V61's unscoped Marugelaraa pallavi write")
    parser.add_argument("--db-url", default=DEFAULT_DB_URL, help="PostgreSQL connection string")
    parser.add_argument(
        "--snapshot",
        help="Write non-Latin Marugelaraa variants to this JSON file and treat that capture as acknowledged",
    )
    parser.add_argument(
        "--check-snapshot",
        help="Compare current non-Latin rows with a snapshot taken before V61",
    )
    args = parser.parse_args()

    with psycopg.connect(args.db_url) as conn:
        inventory = load_inventory(conn, snapshot_acknowledged=args.snapshot is not None)

    if args.snapshot:
        written = write_snapshot(Path(args.snapshot), inventory.variants)
        print(f"Wrote {len(written.variants)} non-Latin variant(s) to {args.snapshot}")

    if args.check_snapshot:
        snapshot = load_snapshot(Path(args.check_snapshot))
        problems = snapshot_still_matches(snapshot, inventory.variants)
        if problems:
            for problem in problems:
                print(problem)
            sys.exit(2)
        print(f"Snapshot matches {len(snapshot.variants)} non-Latin variant(s).")

    decision = assess(inventory)
    for reason in decision.reasons:
        print(reason)
    if decision.blocks_migration or decision.needs_recovery:
        sys.exit(2)


if __name__ == "__main__":
    main()
