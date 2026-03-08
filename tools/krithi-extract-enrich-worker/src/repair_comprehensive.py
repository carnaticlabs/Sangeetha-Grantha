"""Phase 4A comprehensive repair: Fix all remaining section issues.

Handles:
1. 34 krithis with ZERO krithi_sections — creates sections from lyrics
2. 3 krithis with inconsistent section counts — fixes canonical structure + fills gaps
3. Re-parses all lyric section text for accuracy

Usage:
    DATABASE_URL="postgresql://postgres:postgres@localhost:5432/sangita_grantha" \
    PYTHONPATH=. mise exec -- python -m src.repair_comprehensive
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

        # =====================================================
        # PHASE 1: Create sections for 34 zero-section krithis
        # =====================================================
        cur.execute("""
            SELECT k.id, k.title
            FROM krithis k
            WHERE NOT EXISTS (
                SELECT 1 FROM krithi_sections ks WHERE ks.krithi_id = k.id
            )
            ORDER BY k.title
        """)
        zero_section_krithis = cur.fetchall()
        logger.info("Phase 1: %d krithis with zero sections", len(zero_section_krithis))

        sections_created = 0
        lyric_sections_created = 0

        for krithi_id, title in zero_section_krithis:
            # Get all lyric variants
            cur.execute("""
                SELECT id, language, script, lyrics
                FROM krithi_lyric_variants
                WHERE krithi_id = %s
                ORDER BY language
            """, (krithi_id,))
            variants = cur.fetchall()

            # Try English variant first for canonical structure (Rule 2)
            canonical_sections = None
            en_lyrics = None
            for vid, lang, script, lyrics in variants:
                if lang == "en" and lyrics and lyrics.strip():
                    en_lyrics = lyrics
                    result = parser.parse(lyrics)
                    if result.sections and result.sections[0].section_type != SectionType.OTHER:
                        canonical_sections = result.sections
                        break

            # If English failed, try any variant
            if not canonical_sections:
                for vid, lang, script, lyrics in variants:
                    if lyrics and lyrics.strip():
                        result = parser.parse(lyrics)
                        if result.sections and result.sections[0].section_type != SectionType.OTHER:
                            canonical_sections = result.sections
                            break

            # If still no sections detected, create a single PALLAVI from the text
            # (nOTTu-svara compositions have no headers — per Sangeetha-Krithi-Analyser:
            # every krithi must have at minimum a PALLAVI)
            if not canonical_sections:
                best_lyrics = en_lyrics
                if not best_lyrics:
                    for vid, lang, script, lyrics in variants:
                        if lyrics and lyrics.strip():
                            best_lyrics = lyrics
                            break

                if best_lyrics:
                    canonical_sections = [
                        parser.parse("").__class__  # dummy — just construct manually
                    ]
                    # Create a single PALLAVI section
                    from .structure_parser import DetectedSection
                    canonical_sections = [
                        DetectedSection(
                            section_type=SectionType.PALLAVI,
                            order=1,
                            label="Pallavi",
                            text=best_lyrics.strip(),
                            start_pos=0,
                            end_pos=len(best_lyrics),
                        )
                    ]

            if not canonical_sections:
                logger.warning("  [%s] No lyrics found — skipping", title)
                continue

            # Filter out MKS from canonical (Rule 1)
            canonical_sections = [
                s for s in canonical_sections
                if s.section_type != SectionType.MADHYAMA_KALA
            ]

            if not canonical_sections:
                logger.warning("  [%s] Only MKS sections found — skipping", title)
                continue

            # Create krithi_sections
            section_id_map = {}  # order -> section_id
            for section in canonical_sections:
                cur.execute("""
                    INSERT INTO krithi_sections (krithi_id, section_type, order_index, label)
                    VALUES (%s, %s, %s, %s)
                    RETURNING id
                """, (krithi_id, section.section_type.name, section.order, section.label))
                section_id = cur.fetchone()[0]
                section_id_map[section.order] = (section_id, section.section_type.name)
                sections_created += 1

            # Create krithi_lyric_sections for each variant
            for variant_id, language, script, lyrics in variants:
                if not lyrics or not lyrics.strip():
                    continue

                result = parser.parse(lyrics)
                # Build type->text queue from parsed sections
                type_texts: dict[str, list[str]] = {}
                if result.sections:
                    for s in result.sections:
                        if s.section_type != SectionType.MADHYAMA_KALA:
                            type_texts.setdefault(s.section_type.name, []).append(s.text)

                for order, (section_id, section_type) in section_id_map.items():
                    texts = type_texts.get(section_type, [])
                    if texts:
                        text = texts.pop(0)
                    elif len(canonical_sections) == 1:
                        # Single-section krithi: use full lyrics
                        text = lyrics.strip()
                        # Strip metadata headers like raga/tala line at the top
                        lines = text.splitlines()
                        # Remove leading lines that look like metadata
                        clean_lines = []
                        past_metadata = False
                        for line in lines:
                            stripped = line.strip()
                            if not past_metadata and stripped and (
                                " - " in stripped and ("rāg" in stripped.lower() or "raga" in stripped.lower() or "tāḷ" in stripped.lower() or "tala" in stripped.lower())
                                or stripped.startswith("(") and stripped.endswith(")")
                            ):
                                continue
                            if stripped:
                                past_metadata = True
                            clean_lines.append(line)
                        text = "\n".join(clean_lines).strip()
                    else:
                        continue

                    if text.strip():
                        cur.execute("""
                            INSERT INTO krithi_lyric_sections
                                (lyric_variant_id, section_id, text, normalized_text)
                            VALUES (%s, %s, %s, %s)
                        """, (variant_id, section_id, text, text.lower()))
                        lyric_sections_created += 1

            logger.info("  [%s] Created %d sections", title, len(canonical_sections))

        logger.info("Phase 1 done: %d sections, %d lyric_sections created",
                     sections_created, lyric_sections_created)

        # =====================================================
        # PHASE 2: Fix 3 inconsistent krithis
        # =====================================================
        logger.info("Phase 2: Fixing inconsistent krithis")

        # 2a. bRhannAyaki vara dAyaki — canonical has ANUPALLAVI but should be SAMASHTI_CHARANAM
        cur.execute("""
            SELECT ks.id, ks.section_type, ks.order_index
            FROM krithi_sections ks
            JOIN krithis k ON k.id = ks.krithi_id
            WHERE k.title = 'bRhannAyaki vara dAyaki'
            ORDER BY ks.order_index
        """)
        brhannayaki_sections = cur.fetchall()
        for section_id, section_type, order_index in brhannayaki_sections:
            if section_type == "ANUPALLAVI":
                cur.execute("""
                    UPDATE krithi_sections
                    SET section_type = 'SAMASHTI_CHARANAM', label = 'Samashti Charanam'
                    WHERE id = %s
                """, (section_id,))
                logger.info("  [bRhannAyaki] Fixed canonical: ANUPALLAVI -> SAMASHTI_CHARANAM")

        # Now re-parse and fill lyric_sections for bRhannAyaki
        cur.execute("""
            SELECT k.id FROM krithis k WHERE k.title = 'bRhannAyaki vara dAyaki'
        """)
        row = cur.fetchone()
        if row:
            _reparse_and_fill(cur, parser, row[0], "bRhannAyaki vara dAyaki")

        # 2b. anata bAla kRshNa — sa missing SAMASHTI_CHARANAM (parser fix should help)
        cur.execute("""
            SELECT k.id FROM krithis k WHERE k.title = 'anata bAla kRshNa'
        """)
        row = cur.fetchone()
        if row:
            _reparse_and_fill(cur, parser, row[0], "anata bAla kRshNa")

        # 2c. vEnkaTAcala patE — sa missing CHARANAM (upstream data issue, try best-effort)
        cur.execute("""
            SELECT k.id FROM krithis k WHERE k.title = 'vEnkaTAcala patE'
        """)
        row = cur.fetchone()
        if row:
            _reparse_and_fill(cur, parser, row[0], "vEnkaTAcala patE")

        logger.info("Phase 2 done")

        # =====================================================
        # PHASE 3: Re-parse ALL lyric section texts
        # =====================================================
        logger.info("Phase 3: Re-parsing all lyric section texts")

        cur.execute("""
            SELECT DISTINCT k.id, k.title
            FROM krithis k
            JOIN krithi_sections ks ON ks.krithi_id = k.id
            ORDER BY k.title
        """)
        all_krithis = cur.fetchall()

        text_updates = 0
        for krithi_id, title in all_krithis:
            changed = _reparse_and_fill(cur, parser, krithi_id, title)
            if changed:
                text_updates += 1

        logger.info("Phase 3 done: %d krithis had text updates", text_updates)

        conn.commit()
        logger.info("All phases complete. Committed.")

        # =====================================================
        # Verification queries
        # =====================================================
        cur.execute("""
            SELECT COUNT(DISTINCT k.id)
            FROM krithis k
            JOIN krithi_lyric_variants klv ON klv.krithi_id = k.id
            LEFT JOIN krithi_lyric_sections kls ON kls.lyric_variant_id = klv.id
            GROUP BY k.id, klv.language
            HAVING COUNT(kls.id) != (
                SELECT COUNT(ks.id) FROM krithi_sections ks WHERE ks.krithi_id = k.id
            )
        """)
        inconsistent = len(cur.fetchall())
        logger.info("Verification: %d inconsistent variant-section pairs remaining", inconsistent)

        cur.execute("""
            SELECT COUNT(DISTINCT k.id)
            FROM krithis k
            WHERE NOT EXISTS (
                SELECT 1 FROM krithi_sections ks WHERE ks.krithi_id = k.id
            )
        """)
        zero = cur.fetchone()[0]
        logger.info("Verification: %d krithis with zero sections remaining", zero)

        cur.execute("""
            SELECT COUNT(*) FROM krithi_sections WHERE section_type = 'MADHYAMA_KALA'
        """)
        mks = cur.fetchone()[0]
        logger.info("Verification: %d MKS top-level sections remaining", mks)

    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


def _reparse_and_fill(cur, parser: StructureParser, krithi_id, title: str) -> bool:
    """Re-parse lyrics for a krithi and update/insert lyric_sections."""
    cur.execute("""
        SELECT id, section_type, order_index
        FROM krithi_sections
        WHERE krithi_id = %s
        ORDER BY order_index
    """, (krithi_id,))
    canonical_sections = cur.fetchall()
    if not canonical_sections:
        return False

    cur.execute("""
        SELECT id, language, script, lyrics
        FROM krithi_lyric_variants
        WHERE krithi_id = %s
    """, (krithi_id,))
    variants = cur.fetchall()

    changed = False
    for variant_id, language, script, lyrics in variants:
        if not lyrics or not lyrics.strip():
            continue

        result = parser.parse(lyrics)
        if not result.sections:
            continue

        # Build type->text queue
        type_texts: dict[str, list[str]] = {}
        for section in result.sections:
            if section.section_type != SectionType.MADHYAMA_KALA:
                type_texts.setdefault(section.section_type.name, []).append(section.text)

        for section_id, section_type, order_index in canonical_sections:
            texts = type_texts.get(section_type, [])
            if not texts:
                continue

            new_text = texts.pop(0)

            cur.execute("""
                SELECT id, text FROM krithi_lyric_sections
                WHERE lyric_variant_id = %s AND section_id = %s
            """, (variant_id, section_id))
            row = cur.fetchone()
            if not row:
                cur.execute("""
                    INSERT INTO krithi_lyric_sections
                        (lyric_variant_id, section_id, text, normalized_text)
                    VALUES (%s, %s, %s, %s)
                """, (variant_id, section_id, new_text, new_text.lower()))
                changed = True
            elif row[1] != new_text:
                cur.execute("""
                    UPDATE krithi_lyric_sections
                    SET text = %s, normalized_text = %s
                    WHERE id = %s
                """, (new_text, new_text.lower(), row[0]))
                changed = True

    return changed


if __name__ == "__main__":
    main()
