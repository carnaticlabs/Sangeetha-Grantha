"""Phase 4A repair step 2: Fix section text content for existing krithi_lyric_sections.

The first repair added missing sections, but existing sections may have wrong text
(e.g., Anupallavi containing leaked Charanam text). This script re-parses all lyrics
and updates the text for all krithi_lyric_sections.

Usage:
    DATABASE_URL="postgresql://postgres:postgres@localhost:5432/sangita_grantha" \
    PYTHONPATH=. mise exec -- python -m src.repair_section_text
"""
from __future__ import annotations

import logging
import os

import psycopg

from .structure_parser import StructureParser, SectionType

logging.basicConfig(level=logging.INFO, format="%(levelname)s: %(message)s")
logger = logging.getLogger(__name__)

DATABASE_URL = os.environ.get(
    "DATABASE_URL",
    "postgresql://postgres:postgres@localhost:5432/sangita_grantha",
)


def main() -> None:
    parser = StructureParser()
    conn = psycopg.connect(DATABASE_URL)
    conn.autocommit = False

    try:
        cur = conn.cursor()

        # Get all krithis with sections
        cur.execute("""
            SELECT DISTINCT k.id, k.title
            FROM krithis k
            JOIN krithi_sections ks ON ks.krithi_id = k.id
            ORDER BY k.title
        """)
        all_krithis = cur.fetchall()
        logger.info("Processing %d krithis", len(all_krithis))

        updated = 0
        sections_updated = 0

        for krithi_id, title in all_krithis:
            # Get canonical sections
            cur.execute("""
                SELECT id, section_type, order_index
                FROM krithi_sections
                WHERE krithi_id = %s
                ORDER BY order_index
            """, (krithi_id,))
            canonical_sections = cur.fetchall()
            if not canonical_sections:
                continue

            # Get all variants
            cur.execute("""
                SELECT klv.id, klv.language, klv.script, klv.lyrics
                FROM krithi_lyric_variants klv
                WHERE klv.krithi_id = %s
            """, (krithi_id,))
            variants = cur.fetchall()

            krithi_changed = False
            for variant_id, language, script, lyrics in variants:
                if not lyrics or not lyrics.strip():
                    continue

                # Re-parse lyrics
                result = parser.parse(lyrics)
                if not result.sections:
                    continue

                # Build type->text queue
                type_texts: dict[str, list[str]] = {}
                for section in result.sections:
                    type_texts.setdefault(section.section_type.name, []).append(section.text)

                # Update existing section text
                for section_id, section_type, order_index in canonical_sections:
                    texts = type_texts.get(section_type, [])
                    if not texts:
                        continue

                    new_text = texts.pop(0)

                    # Check current text
                    cur.execute("""
                        SELECT id, text FROM krithi_lyric_sections
                        WHERE lyric_variant_id = %s AND section_id = %s
                    """, (variant_id, section_id))
                    row = cur.fetchone()
                    if not row:
                        # Insert missing
                        cur.execute("""
                            INSERT INTO krithi_lyric_sections
                                (lyric_variant_id, section_id, text, normalized_text)
                            VALUES (%s, %s, %s, %s)
                        """, (variant_id, section_id, new_text, new_text.lower()))
                        sections_updated += 1
                        krithi_changed = True
                        continue

                    existing_text = row[1]
                    if existing_text != new_text:
                        cur.execute("""
                            UPDATE krithi_lyric_sections
                            SET text = %s, normalized_text = %s
                            WHERE id = %s
                        """, (new_text, new_text.lower(), row[0]))
                        sections_updated += 1
                        krithi_changed = True

            if krithi_changed:
                updated += 1

        conn.commit()
        logger.info("Done: %d krithis updated, %d section texts changed", updated, sections_updated)

    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


if __name__ == "__main__":
    main()
