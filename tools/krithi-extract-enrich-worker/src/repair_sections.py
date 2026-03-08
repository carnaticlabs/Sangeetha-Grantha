"""Phase 4A repair script: Re-parse lyrics to fill missing krithi_lyric_sections.

For each krithi with inconsistent section counts across variants:
1. Read the lyrics text from each variant
2. Re-parse using the fixed StructureParser (MKS demoted, dual-format merged)
3. Fill in missing krithi_lyric_sections entries

Usage:
    PYTHONPATH=tools/krithi-extract-enrich-worker \
    python -m src.repair_sections
"""
from __future__ import annotations

import logging
import os
import sys

import psycopg

from .structure_parser import StructureParser, SectionType

logging.basicConfig(level=logging.INFO, format="%(levelname)s: %(message)s")
logger = logging.getLogger(__name__)

DATABASE_URL = os.environ.get(
    "DATABASE_URL",
    "postgresql://sangita:sangita@localhost:5432/sangita_grantha",
)


def main() -> None:
    parser = StructureParser()
    conn = psycopg.connect(DATABASE_URL)
    conn.autocommit = False

    try:
        cur = conn.cursor()

        # Find krithis with inconsistent section counts
        cur.execute("""
            SELECT DISTINCT k.id, k.title
            FROM krithis k
            JOIN krithi_sections ks ON ks.krithi_id = k.id
            JOIN krithi_lyric_variants klv ON klv.krithi_id = k.id
            LEFT JOIN krithi_lyric_sections kls
                ON kls.lyric_variant_id = klv.id AND kls.section_id = ks.id
            WHERE kls.id IS NULL
            ORDER BY k.title
        """)
        krithis_with_gaps = cur.fetchall()
        logger.info("Found %d krithis with missing lyric sections", len(krithis_with_gaps))

        fixed = 0
        sections_created = 0

        for krithi_id, title in krithis_with_gaps:
            # Get canonical sections for this krithi
            cur.execute("""
                SELECT id, section_type, order_index
                FROM krithi_sections
                WHERE krithi_id = %s
                ORDER BY order_index
            """, (krithi_id,))
            canonical_sections = cur.fetchall()
            if not canonical_sections:
                continue

            # Get variants with missing sections
            cur.execute("""
                SELECT klv.id, klv.language, klv.script, klv.lyrics
                FROM krithi_lyric_variants klv
                WHERE klv.krithi_id = %s
                AND EXISTS (
                    SELECT 1 FROM krithi_sections ks
                    WHERE ks.krithi_id = %s
                    AND NOT EXISTS (
                        SELECT 1 FROM krithi_lyric_sections kls
                        WHERE kls.lyric_variant_id = klv.id
                        AND kls.section_id = ks.id
                    )
                )
            """, (krithi_id, krithi_id))
            variants_with_gaps = cur.fetchall()

            for variant_id, language, script, lyrics in variants_with_gaps:
                if not lyrics or not lyrics.strip():
                    continue

                # Re-parse the lyrics text
                result = parser.parse(lyrics)
                if not result.sections:
                    continue

                # Build type->text mapping from parsed sections
                type_texts: dict[str, list[str]] = {}
                for section in result.sections:
                    section_type_name = section.section_type.name
                    type_texts.setdefault(section_type_name, []).append(section.text)

                # Fill missing krithi_lyric_sections
                for section_id, section_type, order_index in canonical_sections:
                    # Check if this variant already has text for this section
                    cur.execute("""
                        SELECT id FROM krithi_lyric_sections
                        WHERE lyric_variant_id = %s AND section_id = %s
                    """, (variant_id, section_id))
                    if cur.fetchone():
                        continue

                    # Try to match by section type
                    texts = type_texts.get(section_type, [])
                    if texts:
                        text = texts.pop(0)
                    elif not result.sections or (len(result.sections) == 1 and result.sections[0].section_type.name == "OTHER"):
                        # No section markers found — use full lyrics for the first section
                        if order_index == 1:
                            text = lyrics.strip()
                        else:
                            continue
                    else:
                        continue

                    if text.strip():
                        cur.execute("""
                            INSERT INTO krithi_lyric_sections
                                (lyric_variant_id, section_id, text, normalized_text)
                            VALUES (%s, %s, %s, %s)
                        """, (variant_id, section_id, text, text.lower()))
                        sections_created += 1

            fixed += 1

        conn.commit()
        logger.info(
            "Repair complete: %d krithis processed, %d lyric sections created",
            fixed, sections_created,
        )

        # Verify
        cur.execute("""
            SELECT COUNT(DISTINCT k.id)
            FROM krithis k
            JOIN krithi_sections ks ON ks.krithi_id = k.id
            JOIN krithi_lyric_variants klv ON klv.krithi_id = k.id
            LEFT JOIN krithi_lyric_sections kls
                ON kls.lyric_variant_id = klv.id AND kls.section_id = ks.id
            WHERE kls.id IS NULL
            AND EXISTS (
                SELECT 1 FROM krithi_lyric_variants klv2
                JOIN krithi_lyric_sections kls2
                    ON kls2.lyric_variant_id = klv2.id AND kls2.section_id = ks.id
                WHERE klv2.krithi_id = k.id
            )
        """)
        remaining = cur.fetchone()[0]
        logger.info("Remaining krithis with gaps: %d", remaining)

    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


if __name__ == "__main__":
    main()
