"""Standalone import script for Dikshitar Krithis into PostgreSQL.

Reads eng_krithis.json + skt_krithis.json + krithi_comparison_matched.csv
and inserts into the production schema.

Usage:
    mise exec -- python database/for_import/import_krithis.py
"""

from __future__ import annotations

import csv
import json
import logging
import os
import re
import uuid
from pathlib import Path

import psycopg
from psycopg.rows import dict_row

logger = logging.getLogger(__name__)

BASE_DIR = Path(__file__).parent

# DB connection
DATABASE_URL = os.environ.get(
    "DATABASE_URL",
    "postgresql://postgres:postgres@localhost:5432/sangita_grantha",
)

# ---------------------------------------------------------------------------
# Canonical section ordering
# ---------------------------------------------------------------------------

CANONICAL_ORDER: list[str] = [
    "pallavi",
    "anupallavi",
    "charanam",
    "samashti_charanam",
    "madhyamakala",
    "chittaswaram",
    "swara_sahitya",
    "vilomam",
]

_SECTION_TYPE_MAP: dict[str, str] = {
    "pallavi": "PALLAVI",
    "anupallavi": "ANUPALLAVI",
    "charanam": "CHARANAM",
    "samashti_charanam": "SAMASHTI_CHARANAM",
    "madhyamakala": "MADHYAMA_KALA",
    "chittaswaram": "CHITTASWARAM",
    "swara_sahitya": "SWARA_SAHITYA",
    "vilomam": "OTHER",
}


def _normalise_section_key(key: str) -> str:
    return key.lower().strip().replace(" ", "_").replace("-", "_")


def _section_type(raw_key: str) -> str:
    norm = _normalise_section_key(raw_key)
    return _SECTION_TYPE_MAP.get(norm, "OTHER")


def ordered_sections(sections: dict) -> list[tuple[str, str]]:
    """Return (raw_key, text) pairs in canonical musical order."""
    normalised = {_normalise_section_key(k): (k, v) for k, v in sections.items()}
    result: list[tuple[str, str]] = []
    seen: set[str] = set()
    for canonical_key in CANONICAL_ORDER:
        if canonical_key in normalised:
            raw_key, text = normalised[canonical_key]
            result.append((raw_key, text))
            seen.add(canonical_key)
    for norm_key, (raw_key, text) in normalised.items():
        if norm_key not in seen:
            result.append((raw_key, text))
    return result


# ---------------------------------------------------------------------------
# Text cleaning
# ---------------------------------------------------------------------------

_RE_IMAGE = re.compile(r'!\[.*?\]\(data:[^)]+\)', re.DOTALL)
_RE_DEVANAGARI_LEADING = re.compile(r'^[०-९]+\s+', re.MULTILINE)
_RE_ARABIC_LINE = re.compile(r'^\s*\d{1,3}\s*$', re.MULTILINE)
_RE_MULTI_BLANK = re.compile(r'\n{3,}')
_RE_FOOTNOTE_NUM = re.compile(r'(?<=[a-zāīūṛḷṁṅñṇṭḍśṣḥ])\d{1,3}(?=\s|$)')


def clean_lyric_text(text: str) -> str:
    """Remove extraction artefacts from lyric text."""
    text = _RE_IMAGE.sub('', text)
    text = _RE_DEVANAGARI_LEADING.sub('', text)
    text = _RE_ARABIC_LINE.sub('', text)
    text = _RE_FOOTNOTE_NUM.sub('', text)
    text = _RE_MULTI_BLANK.sub('\n\n', text)
    return text.strip()


# ---------------------------------------------------------------------------
# Name normalisation
# ---------------------------------------------------------------------------

def normalise_raga_name(name: str) -> str:
    name = name.strip()
    name = re.sub(r'\s*\([^)]*\)\s*$', '', name).strip()
    name = re.sub(r'^(ā)a', r'\1', name, flags=re.IGNORECASE)
    return name


def normalise_tala_name(name: str) -> str:
    return name.strip().lower()


# ---------------------------------------------------------------------------
# Main import
# ---------------------------------------------------------------------------

def main() -> None:
    logging.basicConfig(level=logging.INFO, format="%(levelname)s %(message)s")

    csv_path = BASE_DIR / "krithi_comparison_matched.csv"
    eng_json_path = BASE_DIR / "eng_krithis.json"
    skt_json_path = BASE_DIR / "skt_krithis.json"

    # Load data
    with open(csv_path, "r", encoding="utf-8") as f:
        rows = list(csv.DictReader(f))

    with open(eng_json_path, "r", encoding="utf-8") as f:
        eng_data = json.load(f)
        # Index by ID (krithi number) for reliable matching
        eng_by_id = {k["id"]: k for k in eng_data}
        eng_by_title = {k["title"].strip(): k for k in eng_data}

    with open(skt_json_path, "r", encoding="utf-8") as f:
        skt_data = json.load(f)
        skt_by_id = {k["id"]: k for k in skt_data}

    logger.info("Loaded %d CSV rows, %d eng krithis, %d skt krithis",
                len(rows), len(eng_data), len(skt_data))

    conn = psycopg.connect(DATABASE_URL, row_factory=dict_row)

    try:
        with conn.transaction():
            cur = conn.cursor()

            # Resolve composer
            cur.execute("SELECT id, name FROM composers WHERE name = 'Muthuswami Dikshitar'")
            composer_row = cur.fetchone()
            if not composer_row:
                raise Exception("Muthuswami Dikshitar not found in seed data. Run 'sangita-cli db seed' first.")
            composer_id = composer_row["id"]
            primary_language = "sa"  # Sanskrit for Dikshitar

            imported = 0
            skipped = 0

            for index, row in enumerate(rows):
                title_en = row.get("Title-EN", "").strip()
                raga_en_raw = row.get("Raga-EN", "").strip()
                tala_en_raw = row.get("Tala-EN", "").strip()

                if not title_en:
                    skipped += 1
                    continue

                # Look up eng krithi by title
                eng_krithi = eng_by_title.get(title_en)
                if not eng_krithi:
                    logger.warning("No eng krithi found for title: %s", title_en)
                    skipped += 1
                    continue

                sections_en = eng_krithi.get("sections", {})
                krithi_id_num = eng_krithi["id"]

                # Get Sanskrit sections by matching krithi number
                skt_krithi = skt_by_id.get(krithi_id_num, {})
                sections_sa = skt_krithi.get("sections", {}) if skt_krithi else {}

                raga_en = normalise_raga_name(raga_en_raw)
                tala_en = normalise_tala_name(tala_en_raw)

                if not raga_en:
                    # Try from eng_krithi
                    raga_en = normalise_raga_name(eng_krithi.get("raga", ""))
                if not tala_en:
                    tala_en = normalise_tala_name(eng_krithi.get("tala", ""))

                # Skip if no sections at all
                if not sections_en:
                    logger.warning("No sections for #%d: %s — skipping", krithi_id_num, title_en)
                    skipped += 1
                    continue

                # Upsert Raga
                raga_id = None
                if raga_en:
                    raga_norm = raga_en.lower()
                    cur.execute("SELECT id FROM ragas WHERE name_normalized = %s", (raga_norm,))
                    raga_row = cur.fetchone()
                    if raga_row:
                        raga_id = raga_row["id"]
                    else:
                        raga_id = str(uuid.uuid4())
                        cur.execute(
                            "INSERT INTO ragas (id, name, name_normalized, created_at, updated_at)"
                            " VALUES (%s, %s, %s, NOW(), NOW())",
                            (raga_id, raga_en, raga_norm),
                        )

                # Upsert Tala
                tala_id = None
                if tala_en:
                    cur.execute("SELECT id FROM talas WHERE name_normalized = %s", (tala_en,))
                    tala_row = cur.fetchone()
                    if tala_row:
                        tala_id = tala_row["id"]
                    else:
                        tala_id = str(uuid.uuid4())
                        cur.execute(
                            "INSERT INTO talas (id, name, name_normalized, created_at, updated_at)"
                            " VALUES (%s, %s, %s, NOW(), NOW())",
                            (tala_id, tala_en, tala_en),
                        )

                # Incipit from pallavi
                pallavi_text = sections_en.get("pallavi", "")
                incipit = clean_lyric_text(pallavi_text).split("\n")[0].strip()[:120] if pallavi_text else ""

                # Detect ragamalika
                is_ragamalika = "rāgamālikā" in raga_en.lower() or "ragamalika" in raga_en.lower()

                # INSERT krithi
                krithi_id = str(uuid.uuid4())
                title_normalized = title_en.lower()

                cur.execute(
                    """
                    INSERT INTO krithis (
                        id, title, title_normalized, composer_id, primary_raga_id, tala_id,
                        workflow_state, musical_form, is_ragamalika, primary_language,
                        incipit, created_at, updated_at
                    ) VALUES (
                        %s, %s, %s, %s, %s, %s,
                        'draft', 'KRITHI', %s, %s,
                        %s, NOW(), NOW()
                    )
                    """,
                    (
                        krithi_id, title_en, title_normalized,
                        composer_id, raga_id, tala_id,
                        is_ragamalika, primary_language, incipit,
                    ),
                )

                # krithi_ragas junction
                if raga_id:
                    cur.execute(
                        """INSERT INTO krithi_ragas (krithi_id, raga_id, order_index)
                        VALUES (%s, %s, 0) ON CONFLICT DO NOTHING""",
                        (krithi_id, raga_id),
                    )

                # Sections in canonical order
                sections_ordered = ordered_sections(sections_en)

                krithi_section_ids: dict[str, str] = {}
                for order_idx, (raw_key, _text) in enumerate(sections_ordered, start=1):
                    ks_id = str(uuid.uuid4())
                    section_type = _section_type(raw_key)
                    cur.execute(
                        """INSERT INTO krithi_sections
                            (id, krithi_id, section_type, order_index, created_at, updated_at)
                        VALUES (%s, %s, %s, %s, NOW(), NOW())""",
                        (ks_id, krithi_id, section_type, order_idx),
                    )
                    krithi_section_ids[raw_key] = ks_id

                # English Lyric Variant
                var_en_id = str(uuid.uuid4())
                en_lyrics_full = "\n\n".join(
                    clean_lyric_text(text) for _, text in sections_ordered if text.strip()
                )
                cur.execute(
                    """INSERT INTO krithi_lyric_variants (
                        id, krithi_id, language, script, is_primary,
                        source_reference, lyrics, created_at, updated_at
                    ) VALUES (%s, %s, 'en', 'latin', true, 'guruguha.org PDF Import', %s, NOW(), NOW())""",
                    (var_en_id, krithi_id, en_lyrics_full),
                )

                # English Lyric Sections
                for raw_key, ks_id in krithi_section_ids.items():
                    if raw_key in sections_en:
                        cleaned = clean_lyric_text(sections_en[raw_key])
                        if cleaned:
                            cur.execute(
                                """INSERT INTO krithi_lyric_sections
                                    (lyric_variant_id, section_id, text, created_at, updated_at)
                                VALUES (%s, %s, %s, NOW(), NOW())""",
                                (var_en_id, ks_id, cleaned),
                            )

                # Sanskrit Lyric Variant (if available)
                if sections_sa:
                    var_sa_id = str(uuid.uuid4())
                    sa_texts = []
                    for raw_key, _ in sections_ordered:
                        canon = _normalise_section_key(raw_key)
                        sa_text = ""
                        if canon in sections_sa:
                            sa_text = sections_sa[canon]
                        else:
                            for k, v in sections_sa.items():
                                if _normalise_section_key(k) == canon:
                                    sa_text = v
                                    break
                        if sa_text:
                            sa_texts.append(clean_lyric_text(sa_text))

                    sa_lyrics_full = "\n\n".join(t for t in sa_texts if t)
                    if sa_lyrics_full:
                        cur.execute(
                            """INSERT INTO krithi_lyric_variants (
                                id, krithi_id, language, script, is_primary,
                                source_reference, lyrics, created_at, updated_at
                            ) VALUES (%s, %s, 'sa', 'devanagari', false, 'guruguha.org PDF Import', %s, NOW(), NOW())""",
                            (var_sa_id, krithi_id, sa_lyrics_full),
                        )

                        # Sanskrit Lyric Sections
                        for raw_key, ks_id in krithi_section_ids.items():
                            canon = _normalise_section_key(raw_key)
                            sa_text = ""
                            if canon in sections_sa:
                                sa_text = sections_sa[canon]
                            else:
                                for k, v in sections_sa.items():
                                    if _normalise_section_key(k) == canon:
                                        sa_text = v
                                        break
                            if sa_text:
                                cleaned = clean_lyric_text(sa_text)
                                if cleaned:
                                    cur.execute(
                                        """INSERT INTO krithi_lyric_sections
                                            (lyric_variant_id, section_id, text, created_at, updated_at)
                                        VALUES (%s, %s, %s, NOW(), NOW())""",
                                        (var_sa_id, ks_id, cleaned),
                                    )

                # AUDIT_LOG
                cur.execute(
                    """INSERT INTO audit_log (
                        action, entity_table, entity_id, changed_at, metadata
                    ) VALUES ('IMPORT', 'krithis', %s, NOW(), %s)""",
                    (
                        krithi_id,
                        json.dumps({
                            "title": title_en,
                            "composer": "Muthuswami Dikshitar",
                            "raga": raga_en,
                            "tala": tala_en,
                            "source": "guruguha.org_pdf",
                            "krithi_number": krithi_id_num,
                        }),
                    ),
                )

                imported += 1
                if imported % 50 == 0:
                    logger.info("Imported %d/%d krithis...", imported, len(rows))

        logger.info("Import complete: %d imported, %d skipped", imported, skipped)

    except Exception as e:
        logger.error("Import failed: %s", e, exc_info=True)
        raise
    finally:
        conn.close()


if __name__ == "__main__":
    main()
